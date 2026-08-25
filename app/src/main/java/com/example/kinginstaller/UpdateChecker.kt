package com.example.kinginstaller

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject
import java.net.URL
import kotlin.concurrent.thread

object UpdateChecker {

    private const val GITHUB_API_URL = "https://api.github.com/repos/fcaronte/KingInstaller/releases/latest"

    fun checkForUpdates(activity: Activity, manual: Boolean = false) {
        thread {
            try {
                val response = URL(GITHUB_API_URL).readText()
                val json = JSONObject(response)
                val latestVersion = json.getString("tag_name").replace("v", "")
                val currentVersion = activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: "0.0"

                if (isNewerVersion(latestVersion, currentVersion)) {
                    val downloadUrl = json.getString("html_url")
                    activity.runOnUiThread {
                        showUpdateDialog(activity, latestVersion, downloadUrl)
                    }
                } else if (manual) {
                    activity.runOnUiThread {
                        Toast.makeText(activity, R.string.no_update_available, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("UpdateChecker", "Failed to check for updates", e)
                if (manual) {
                    activity.runOnUiThread {
                        Toast.makeText(activity, activity.getString(R.string.error_occurred, e.message ?: e.toString()), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        
        val size = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until size) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    private fun showUpdateDialog(activity: Activity, version: String, url: String) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.update_available_title)
            .setMessage(activity.getString(R.string.update_available_msg, version))
            .setPositiveButton(R.string.download) { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                activity.startActivity(intent)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
