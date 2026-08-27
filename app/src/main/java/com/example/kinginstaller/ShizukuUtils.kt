package com.example.kinginstaller

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.concurrent.thread

object ShizukuUtils {

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

    fun runShizukuShell(command: String): Pair<Int, String> {
        return try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            val output = StringBuilder()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            var line: String?
            while (reader.readLine().also { line = it } != null) output.append(line).append("\n")
            while (errorReader.readLine().also { line = it } != null) output.append(line).append("\n")
            
            val exitCode = process.waitFor()
            Pair(exitCode, output.toString())
        } catch (e: Exception) {
            Log.e("ShizukuUtils", "Error running shell command", e)
            Pair(-1, e.message ?: "Unknown error")
        }
    }

    fun installApk(activity: Activity, filepath: String?, onStatusUpdate: (String) -> Unit, onSuccess: () -> Unit) {
        if (filepath == null) {
            Toast.makeText(activity, R.string.select_a_file, Toast.LENGTH_SHORT).show()
            return
        }

        onStatusUpdate("Launching Shell Proxy Install...")

        thread {
            try {
                val apkFile = File(filepath)
                val context = activity.applicationContext
                val fileUri = androidx.core.content.FileProvider.getUriForFile(
                    context, "${context.packageName}.provider", apkFile
                )

                // 1. Concediamo i permessi alla Shell e all'installatore di sistema (senza loggare errori se fallisce)
                val targetPackages = listOf("com.android.shell", "com.google.android.packageinstaller", "com.android.packageinstaller")
                targetPackages.forEach { pkg ->
                    try {
                        context.grantUriPermission(pkg, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (ignored: Exception) {}
                }

                activity.runOnUiThread { onStatusUpdate("Opening system installation dialog...") }

                // 2. Usiamo l'azione INSTALL_PACKAGE via Shell: è la più potente per lo spoofing su Android 15/17
                val amCommand = "am start " +
                        "-a android.intent.action.INSTALL_PACKAGE " +
                        "-d \"$fileUri\" " +
                        "-t \"application/vnd.android.package-archive\" " +
                        "-f 0x00000001 " + // FLAG_GRANT_READ_URI_PERMISSION
                        "--es android.intent.extra.INSTALLER_PACKAGE_NAME \"${InstallationUtils.VENDING_PKG}\" " +
                        "--es android.intent.extra.REFERRER_NAME \"android-app://${InstallationUtils.VENDING_PKG}\" " +
                        "--ei android.intent.extra.INSTALL_REASON 1 " + // 1 = STORE
                        "--ez android.intent.extra.NOT_UNKNOWN_SOURCE true"

                val (exitCode, output) = runShizukuShell(amCommand)

                activity.runOnUiThread {
                    if (exitCode == 0) {
                        onStatusUpdate("")
                        onSuccess()
                    } else {
                        onStatusUpdate("Error: $output")
                    }
                }
            } catch (e: Exception) {
                activity.runOnUiThread { 
                    onStatusUpdate(activity.getString(R.string.error_occurred, e.toString()))
                }
            }
        }
    }

    /**
     * Forza l'impostazione dell'installer via Shizuku per un pacchetto già installato.
     * Utile dopo un'installazione via Intent (Metodo King).
     */
    fun setInstallerViaShizuku(packageName: String) {
        thread {
            val cmd = "cmd package set-installer $packageName ${InstallationUtils.VENDING_PKG}"
            runShizukuShell(cmd)
            Log.d("KingInstaller", "Forced installer to Play Store for $packageName via Shizuku")
        }
    }
}
