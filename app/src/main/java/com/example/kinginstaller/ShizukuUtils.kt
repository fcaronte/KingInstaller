package com.example.kinginstaller

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

    fun isShizukuAvailable(): Boolean = try { Shizuku.pingBinder() } catch (_: Exception) { false }

    fun hasShizukuPermission(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) { false }

    object InstallationState {
        @Volatile
        var isFocusLost = false
    }

    /**
     * Lancia l'intent di installazione spacciandosi per com.android.shell (UID 2000)
     * tramite il binder di sistema wrappato da Shizuku.
     * Funziona sia su Shizuku Standard che su Shizuku Plus.
     */
    @SuppressLint("DiscouragedPrivateApi", "PrivateApi")
    @Suppress("UNUSED_PARAMETER")
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

            val method = atmInstance.javaClass.methods.firstOrNull { it.name == "startActivityAsUser" }
                ?: atmInstance.javaClass.declaredMethods.firstOrNull { it.name == "startActivityAsUser" }
                ?: throw NoSuchMethodException("startActivityAsUser non trovato")

            method.isAccessible = true

            val args = arrayOfNulls<Any>(method.parameterTypes.size)
            for (i in method.parameterTypes.indices) {
                val paramType = method.parameterTypes[i]
                args[i] = when {
                    paramType.isAssignableFrom(Intent::class.java) -> intent
                    paramType == String::class.java && (i == 1 || i == 2) -> "com.android.shell"
                    paramType == Int::class.javaPrimitiveType -> 0
                    else -> null
                }
            }

            method.invoke(atmInstance, *args)
            Log.d("ShizukuUtils", "startActivityAsUser invocato come com.android.shell con successo")
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

                // 1. Condivisione URI con FileProvider
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

                // 2. Costruzione Intent nativo con gli extra necessari per Play Store
                val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    setDataAndType(fileUri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, InstallationUtils.VENDING_PKG)
                    putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    putExtra("android.intent.extra.REFERRER_NAME", "android-app://${InstallationUtils.VENDING_PKG}")
                    putExtra("android.content.pm.extra.INSTALL_REASON", 1)
                }

                InstallationState.isFocusLost = false

                // 3. Esecuzione con identità Shell (UID 2000)
                val success = startInstallActivityAsShell(context, installIntent)

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

    private fun showError(activity: Activity, onStatusUpdate: (String) -> Unit, message: String) {
        onStatusUpdate(message)
        if (activity is MainActivity) {
            activity.findViewById<TextView>(R.id.textViewError)?.setTextColor(
                activity.getColor(R.color.aa_red_text)
            )
        }
    }
}
