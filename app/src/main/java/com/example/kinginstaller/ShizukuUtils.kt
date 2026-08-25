package com.example.kinginstaller

import android.app.Activity
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

        onStatusUpdate("Transferring APK via Shizuku...")

        thread {
            try {
                val remotePath = "/data/local/tmp/king_install_temp.apk"
                if (!transferFileToShizuku(File(filepath), remotePath)) {
                    activity.runOnUiThread { onStatusUpdate("Failed to transfer APK") }
                    return@thread
                }
                
                activity.runOnUiThread { onStatusUpdate("File transferred. Installing...") }
                val (exitCode, output) = runShizukuShell("pm install -r -t -i ${InstallationUtils.VENDING_PKG} $remotePath")
                runShizukuShell("rm $remotePath")

                activity.runOnUiThread {
                    if (exitCode == 0 && output.contains("Success")) {
                        onSuccess()
                        onStatusUpdate("Installation Successful!\n(Installer: ${InstallationUtils.VENDING_PKG})")
                        Toast.makeText(activity, "App installed successfully", Toast.LENGTH_LONG).show()
                    } else {
                        onStatusUpdate("Install Failed (Exit $exitCode):\n$output")
                    }
                }
            } catch (e: Exception) {
                activity.runOnUiThread { 
                    onStatusUpdate(activity.getString(R.string.error_occurred, e.toString()))
                }
            }
        }
    }
}
