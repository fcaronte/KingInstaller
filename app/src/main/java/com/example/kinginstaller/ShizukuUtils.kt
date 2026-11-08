package com.example.kinginstaller

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

object ShizukuUtils {

    private val isInstalling = AtomicBoolean(false)

    // UserService Shizuku tenuto "caldo": lo avviamo presto (all'inizio dell'installazione)
    // così è già connesso quando serve il fix post-installazione. Il bind tardivo (subito dopo
    // l'installazione) su One UI/HyperOS può non connettersi in tempo — vedi log S24.
    @Volatile private var cachedService: IMyUserService? = null
    @Volatile private var warmConnection: ServiceConnection? = null

    fun isShizukuAvailable(): Boolean = try {
        Shizuku.pingBinder() || Shizuku.getVersion() > 0
    } catch (_: Exception) {
        false
    }

    fun hasShizukuPermission(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) { false }

    object InstallationState {
        @Volatile
        var isFocusLost = false
    }

    @SuppressLint("BlockedPrivateApi", "DiscouragedPrivateApi", "PrivateApi")
    private fun startInstallActivityAsShell(context: Context, intent: Intent): Boolean {
        return try {
            val binder = SystemServiceHelper.getSystemService("activity_task")
                ?: SystemServiceHelper.getSystemService("activity")
                ?: return false

            val wrappedBinder = ShizukuBinderWrapper(binder)
            val atmStubClass = Class.forName("android.app.IActivityTaskManager\$Stub")
            val asInterfaceMethod = atmStubClass.getDeclaredMethod("asInterface", IBinder::class.java)
            asInterfaceMethod.isAccessible = true
            val atmInstance = asInterfaceMethod.invoke(null, wrappedBinder) ?: return false

            val methods = atmInstance.javaClass.methods + atmInstance.javaClass.declaredMethods
            val method = methods.firstOrNull { m ->
                m.name == "startActivityAsUser" &&
                m.parameterTypes.any { it.isAssignableFrom(Intent::class.java) }
            } ?: throw NoSuchMethodException("startActivityAsUser non trovato")

            method.isAccessible = true

            val args = arrayOfNulls<Any>(method.parameterTypes.size)
            for (i in method.parameterTypes.indices) {
                val pType = method.parameterTypes[i]
                args[i] = when {
                    pType.isAssignableFrom(Intent::class.java) -> intent
                    pType == String::class.java -> if (i == 1) "com.android.shell" else null
                    pType == Int::class.javaPrimitiveType || pType == Integer::class.java -> 0
                    else -> null
                }
            }

            method.invoke(atmInstance, *args)
            Log.d("ShizukuUtils", "startActivityAsUser invocato come com.android.shell con successo su API ${Build.VERSION.SDK_INT}")
            true
        } catch (e: Exception) {
            Log.e("ShizukuUtils", "Errore startInstallActivityAsShell", e)
            false
        }
    }

    fun installApk(activity: Activity, filepath: String?, onStatusUpdate: (String) -> Unit, onSuccess: () -> Unit) {
        if (filepath == null) {
            Toast.makeText(activity, R.string.select_a_file, Toast.LENGTH_SHORT).show()
            return
        }

        if (!isInstalling.compareAndSet(false, true)) {
            Log.w("ShizukuUtils", "Installazione già in corso, richiesta ignorata.")
            return
        }

        onStatusUpdate(activity.getString(R.string.shizuku_install_start))

        thread {
            try {
                val apkFile = File(filepath)
                val context = activity.applicationContext
                if (!apkFile.exists()) {
                    activity.runOnUiThread {
                        showError(activity, onStatusUpdate, activity.getString(R.string.file_apk_not_found))
                        isInstalling.set(false)
                    }
                    return@thread
                }

                ensureUserServiceConnected(context)

                val fileUri = FileProvider.getUriForFile(
                    context, "${context.packageName}.provider", apkFile
                )

                val targetPackages = listOf(
                    "com.android.shell",
                    "com.google.android.packageinstaller",
                    "com.android.packageinstaller"
                )
                targetPackages.forEach { pkg ->
                    try {
                        context.grantUriPermission(pkg, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (_: Exception) {}
                }

                activity.runOnUiThread { onStatusUpdate(activity.getString(R.string.opening_package_installer)) }

                val vending = InstallationUtils.VENDING_PKG
                val am = "am start" +
                        " -a android.intent.action.INSTALL_PACKAGE" +
                        " -d $fileUri" +
                        " -t application/vnd.android.package-archive" +
                        " --es android.intent.extra.INSTALLER_PACKAGE_NAME $vending" +
                        " --ez android.intent.extra.NOT_UNKNOWN_SOURCE true" +
                        " --ei android.content.pm.extra.INSTALL_REASON 1"

                val warm = cachedService
                var success = false
                if (warm?.asBinder()?.pingBinder() == true) {
                    try {
                        warm.execCommand(am)
                        success = true
                        Log.d("ShizukuUtils", "am start eseguito tramite UserService")
                    } catch (e: Exception) {
                        Log.e("ShizukuUtils", "execCommand tramite UserService fallito", e)
                    }
                }

                if (!success) {
                    @Suppress("DEPRECATION")
                    val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                        setDataAndType(fileUri, "application/vnd.android.package-archive")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, vending)
                        putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                        putExtra("android.content.pm.extra.INSTALL_REASON", 1)
                    }
                    InstallationState.isFocusLost = false
                    success = startInstallActivityAsShell(context, installIntent)
                    if (!success) {
                        try {
                            activity.startActivity(installIntent)
                            success = true
                            Log.d("ShizukuUtils", "startInstallActivityAsShell fallito -> fallback su startActivity riuscito")
                        } catch (e: Exception) {
                            Log.e("ShizukuUtils", "Fallback startActivity fallito", e)
                        }
                    }
                }

                if (success) {
                    val startTime = System.currentTimeMillis()
                    while (System.currentTimeMillis() - startTime < 1500) {
                        if (InstallationState.isFocusLost) break
                        Thread.sleep(40)
                    }
                }

                activity.runOnUiThread {
                    if (success) {
                        onStatusUpdate("")
                        onSuccess()
                    } else {
                        showError(
                            activity,
                            onStatusUpdate,
                            activity.getString(R.string.shizuku_shell_error)
                        )
                    }
                    isInstalling.set(false)
                }

            } catch (e: Exception) {
                Log.e("ShizukuUtils", "Errore procedura Shizuku", e)
                activity.runOnUiThread {
                    showError(activity, onStatusUpdate, activity.getString(R.string.error_occurred, e.localizedMessage ?: e.message))
                    isInstalling.set(false)
                }
            }
        }
    }

    fun installSplitApks(
        activity: Activity,
        packageName: String?,
        apkFiles: List<File>,
        onStatusUpdate: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        if (!isInstalling.compareAndSet(false, true)) {
            Log.w("ShizukuUtils", "Installazione già in corso, richiesta ignorata.")
            return
        }

        onStatusUpdate(activity.getString(R.string.shizuku_install_start))

        thread {
            try {
                val appContext = activity.applicationContext

                activity.runOnUiThread {
                    InstallationUtils.installSplitApksClassico(appContext, packageName, apkFiles, errorCallback = { err ->
                        showError(activity, onStatusUpdate, activity.getString(R.string.error_occurred, err))
                    })
                    onStatusUpdate("")
                    onSuccess()
                    isInstalling.set(false)
                }

            } catch (e: Exception) {
                Log.e("ShizukuUtils", "Errore installSplitApks Shizuku", e)
                activity.runOnUiThread {
                    val appContext = activity.applicationContext
                    InstallationUtils.installSplitApksClassico(appContext, packageName, apkFiles, errorCallback = { err ->
                        showError(activity, onStatusUpdate, activity.getString(R.string.error_occurred, err))
                    })
                    onStatusUpdate("")
                    onSuccess()
                    isInstalling.set(false)
                }
            }
        }
    }

    private fun showError(activity: Activity, onStatusUpdate: (String) -> Unit, message: String) {
        onStatusUpdate(message)
        if (activity is MainActivity) {
            activity.findViewById<TextView>(R.id.textViewError)?.setTextColor(
                activity.getColor(R.color.aa_red_text)
            )
        }
    }

    private fun buildUserServiceArgs(context: Context): Shizuku.UserServiceArgs =
        Shizuku.UserServiceArgs(ComponentName(context.applicationContext, MyUserService::class.java))
            .tag("king_installer_service")
            .processNameSuffix("service")
            .daemon(false)
            .version(1)

    fun ensureUserServiceConnected(context: Context) {
        if (!isShizukuAvailable() || !hasShizukuPermission()) return
        if (cachedService?.asBinder()?.pingBinder() == true) return

        val latch = java.util.concurrent.CountDownLatch(1)
        try {
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    if (binder != null && binder.pingBinder()) {
                        cachedService = IMyUserService.Stub.asInterface(binder)
                        Log.d("ShizukuUtils", "UserService connesso con successo")
                    }
                    latch.countDown()
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    cachedService = null
                    latch.countDown()
                }
            }
            Shizuku.bindUserService(buildUserServiceArgs(context), connection)
            latch.await(3500, java.util.concurrent.TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            Log.e("ShizukuUtils", "ensureUserServiceConnected fallito", e)
        }
    }

}
