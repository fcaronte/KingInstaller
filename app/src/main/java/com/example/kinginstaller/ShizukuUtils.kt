package com.example.kinginstaller

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
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

    fun transferFileToShizuku(localFile: File, remotePath: String): Boolean {
        return try {
            val catProcess = Shizuku.newProcess(arrayOf("sh", "-c", "cat > $remotePath"), null, null)
            val os = catProcess.outputStream
            localFile.inputStream().use { input ->
                input.copyTo(os)
            }
            os.flush()
            os.close()
            catProcess.waitFor() == 0
        } catch (e: Exception) {
            Log.e("ShizukuUtils", "Error transferring file", e)
            false
        }
    }

    fun showAdvancedDialog(activity: Activity, selectedFilePath: String?, onStatusUpdate: (String) -> Unit) {
        if (!isShizukuAvailable() || !hasShizukuPermission()) {
            Toast.makeText(activity, R.string.shizuku_not_available, Toast.LENGTH_SHORT).show()
            return
        }

        val input = TextInputEditText(activity)
        input.setText(activity.getString(R.string.advanced_shizuku_default))
        input.hint = activity.getString(R.string.advanced_shizuku_hint)

        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.advanced_shizuku_title)
            .setView(input)
            .setPositiveButton(R.string.execute) { _, _ ->
                executeAdvancedCommand(activity, selectedFilePath, input.text.toString(), onStatusUpdate)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun executeAdvancedCommand(activity: Activity, filepath: String?, command: String, onStatusUpdate: (String) -> Unit) {
        if (filepath == null) {
            Toast.makeText(activity, R.string.select_a_file, Toast.LENGTH_SHORT).show()
            return
        }

        onStatusUpdate("Executing advanced command...")

        thread {
            try {
                val remotePath = "/data/local/tmp/king_advanced_temp.apk"
                if (!transferFileToShizuku(File(filepath), remotePath)) {
                    activity.runOnUiThread { onStatusUpdate("Failed to transfer APK") }
                    return@thread
                }

                val finalCmd = command.replace("\$APK", remotePath)
                val (exitCode, output) = runShizukuShell(finalCmd)
                
                runShizukuShell("rm $remotePath")

                activity.runOnUiThread {
                    onStatusUpdate("Exit Code: $exitCode\nOutput:\n$output")
                }
            } catch (e: Exception) {
                activity.runOnUiThread { 
                    onStatusUpdate(activity.getString(R.string.error_occurred, e.toString()))
                }
            }
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
                // Usiamo il FileProvider per generare un URI sicuro
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
                // Non specifichiamo il componente (PackageInstallerActivity) per lasciare che il sistema 
                // scelga la sua attività predefinita, evitando crash o errori di puntamento.
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
                        onStatusUpdate("Success! Dialog opened.\nSource: Play Store")
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
