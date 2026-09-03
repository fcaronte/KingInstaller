package com.example.kinginstaller

import android.app.Activity
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

        try {
            val installIntent = InstallationUtils.createInstallIntent(activity, File(filepath))
            onStatusUpdate("Opening system installation dialog...")
            activity.startActivity(installIntent)
            onStatusUpdate("")
            onSuccess()
        } catch (e: Exception) {
            onStatusUpdate(activity.getString(R.string.error_occurred, e.toString()))
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
