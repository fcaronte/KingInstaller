package com.example.kinginstaller

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

object ShizukuUtils {

    // Guardia per evitare installazioni multiple o loop di esecuzione in parallelo
    private val isInstalling = AtomicBoolean(false)

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun hasShizukuPermission(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    object InstallationState {
        @Volatile
        var isFocusLost = false
    }

    fun runShizukuShell(command: String): Pair<Int, String> {
        return try {
            // Tentativo 1: Usiamo la nuova modalità via reflection sulle API correnti di Shizuku se presenti
            val process = callNewProcessViaReflection(arrayOf("sh", "-c", command), null, null)
                ?: throw NullPointerException("Process is null")

            val output = StringBuilder()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            while (errorReader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            val exitCode = process.waitFor()
            Pair(exitCode, output.toString())
        } catch (e: Exception) {
            Log.w("ShizukuUtils", "Standard Shizuku shell failed, trying alternative execution method: ${e.message}")

            // Tentativo 2 (Piano di riserva per ShizukuPlus):
            // Se newProcess fallisce, proviamo a invocare il comando tramite il package manager o un processo di runtime diretto
            try {
                val fallbackProcess = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                val reader = BufferedReader(InputStreamReader(fallbackProcess.inputStream))
                val output = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                val exitCode = fallbackProcess.waitFor()
                Pair(exitCode, output.toString())
            } catch (ex: Exception) {
                Pair(-1, ex.message ?: "Unknown error")
            }
        }
    }

    // Helper di reflection per invocare il processo Shizuku in modo sicuro sulle nuove versioni delle API
    private fun callNewProcessViaReflection(cmd: Array<String>, env: Array<String>?, dir: String?): Process {
        try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val result = method.invoke(null, cmd, env, dir)
            if (result != null) {
                return result as Process
            }
        } catch (ignored: Exception) {}

        // PIANO ALTERNATIVO PER SHIZUKUPLUS:
        // Se newProcess è assente o restituisce null, simuliamo un processo locale
        // delegando l'esecuzione al contesto o sollevando un'eccezione gestita
        // che forza l'uso dei comandi diretti di Package Manager.
        throw UnsupportedOperationException("Shizuku.newProcess is not supported by this Shizuku implementation.")
    }

    fun installApk(activity: Activity, filepath: String?, onStatusUpdate: (String) -> Unit, onSuccess: () -> Unit) {
        if (filepath == null) {
            Toast.makeText(activity, R.string.select_a_file, Toast.LENGTH_SHORT).show()
            return
        }

        // Blocca richieste multiple simultanee (evita i loop di installazione su S24 e altri dispositivi)
        if (!isInstalling.compareAndSet(false, true)) {
            Log.w("ShizukuUtils", "Installation already in progress, ignoring duplicate request.")
            return
        }

        onStatusUpdate("Launching Shizuku Install...")

        thread {
            try {
                val apkFile = File(filepath)
                val context = activity.applicationContext
                val fileUri = androidx.core.content.FileProvider.getUriForFile(
                    context, "${context.packageName}.provider", apkFile
                )

                val targetPackages = listOf("com.android.shell", "com.google.android.packageinstaller", "com.android.packageinstaller")
                targetPackages.forEach { pkg ->
                    try {
                        context.grantUriPermission(pkg, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (ignored: Exception) {}
                }

                activity.runOnUiThread { onStatusUpdate("Opening system installation dialog...") }

                val amCommand = "am start " +
                        "-a android.intent.action.INSTALL_PACKAGE " +
                        "-d \"$fileUri\" " +
                        "-t \"application/vnd.android.package-archive\" " +
                        "-f 0x00000001 " +
                        "--es android.intent.extra.INSTALLER_PACKAGE_NAME \"${InstallationUtils.VENDING_PKG}\" " +
                        "--es android.intent.extra.REFERRER_NAME \"android-app://${InstallationUtils.VENDING_PKG}\" " +
                        "--ei android.intent.extra.INSTALL_REASON 1 " +
                        "--ez android.intent.extra.NOT_UNKNOWN_SOURCE true"

                InstallationState.isFocusLost = false
                var commandSent = false

                // TENTATIVO 1: Proviamo prima con il metodo classico (runShizukuShell / newProcess)
                try {
                    val (exitCode, output) = runShizukuShell(amCommand)
                    val hasError = exitCode != 0 ||
                            output.contains("Error", ignoreCase = true) ||
                            output.contains("Exception", ignoreCase = true)

                    if (!hasError) {
                        commandSent = true
                    }
                } catch (e: Exception) {
                    Log.w("ShizukuUtils", "Classic Shizuku method failed, switching to UserService...", e)
                }

                // TENTATIVO 2: Se il metodo classico ha fallito (es. su ShizukuPlus), usiamo il UserService via AIDL
                if (!commandSent) {
                    val userServiceArgs = Shizuku.UserServiceArgs(
                        ComponentName(context, MyUserService::class.java)
                    ).tag("king_installer_service")
                        .processNameSuffix("service")
                        .daemon(false)
                        .version(1)

                    val serviceConnection = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                            val binder = service ?: return
                            val proxy = IMyUserService.Stub.asInterface(binder)
                            try {
                                proxy.execCommand(amCommand)
                                Log.d("KingInstaller", "Installation intent sent via Shizuku UserService fallback")
                            } catch (ex: Exception) {
                                Log.e("KingInstaller", "Error executing command via UserService fallback", ex)
                            } finally {
                                try {
                                    Shizuku.unbindUserService(userServiceArgs, this, true)
                                } catch (ignored: Exception) {}
                            }
                        }

                        override fun onServiceDisconnected(name: ComponentName?) {}
                    }

                    try {
                        Shizuku.bindUserService(userServiceArgs, serviceConnection)
                        Thread.sleep(600) // Attesa avvio servizio
                        commandSent = true
                    } catch (ex: Exception) {
                        Log.e("KingInstaller", "UserService fallback also failed", ex)
                    }
                }

                // CONTROLLO EFFETTIVO DEL FOCUS:
                // Se il dispositivo blocca silenziosamente l'intent (es. Xiaomi), isFocusLost resterà false e attiveremo il fallback.
                var actualSuccess = false
                if (commandSent) {
                    val startTime = System.currentTimeMillis()
                    while (System.currentTimeMillis() - startTime < 1500) {
                        if (InstallationState.isFocusLost) {
                            actualSuccess = true
                            break
                        }
                        Thread.sleep(40)
                    }
                }

                if (actualSuccess) {
                    activity.runOnUiThread {
                        onStatusUpdate("")
                        onSuccess()
                    }
                } else {
                    activity.runOnUiThread {
                        onStatusUpdate("Shizuku blocked, falling back to standard installer...")
                        if (activity is MainActivity) {
                            activity.triggerFallbackInstall(filepath)
                        }
                    }
                }
            } catch (e: Exception) {
                activity.runOnUiThread {
                    onStatusUpdate(activity.getString(R.string.error_occurred, e.toString()))
                }
            } finally {
                // Rilasciamo la guardia dopo un breve delay per consentire nuove installazioni future
                thread {
                    Thread.sleep(2000)
                    isInstalling.set(false)
                }
            }
        }
    }

    fun setInstallerViaShizuku(context: android.content.Context, packageName: String) {
        val userServiceArgs = Shizuku.UserServiceArgs(
            ComponentName(context, MyUserService::class.java)
        ).tag("king_installer_service")
            .processNameSuffix("service")
            .daemon(false)
            .version(1)

        val serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service ?: return
                val proxy = IMyUserService.Stub.asInterface(binder)
                try {
                    proxy.setInstaller(packageName, InstallationUtils.VENDING_PKG)
                    Log.d("KingInstaller", "Installer successfully forced via Shizuku UserService for $packageName")
                } catch (e: Exception) {
                    Log.e("KingInstaller", "Error calling UserService method", e)
                } finally {
                    try {
                        Shizuku.unbindUserService(userServiceArgs, this, true)
                    } catch (ignored: Exception) {}
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.d("KingInstaller", "UserService disconnected")
            }
        }

        try {
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        } catch (e: Exception) {
            Log.e("KingInstaller", "Failed to bind Shizuku UserService", e)
        }
    }
}