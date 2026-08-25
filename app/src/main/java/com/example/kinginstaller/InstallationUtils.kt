package com.example.kinginstaller

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object InstallationUtils {

    const val GOOGLE_INSTALLER_PKG = "com.google.android.packageinstaller"
    const val VENDING_PKG = "com.android.vending"

    fun getGoogleInstallerSourceDir(context: Context): String? {
        return try {
            val info = context.packageManager.getPackageInfo(GOOGLE_INSTALLER_PKG, 0)
            info.applicationInfo?.sourceDir
        } catch (e: Exception) {
            null
        }
    }

    fun clearTempFiles(context: Context) {
        try {
            val dir = File(context.filesDir, "apk")
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { it.delete() }
            }
        } catch (ignored: Exception) {
        }
    }

    fun copyFileToInternalStorage(context: Context, uri: Uri, newDirName: String): String? {
        return try {
            val returnCursor = context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )
            val nameIndex = returnCursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME) ?: -1
            returnCursor?.moveToFirst()
            val name = if (nameIndex != -1) returnCursor?.getString(nameIndex) else "temp.apk"
            returnCursor?.close()

            val dir = if (newDirName.isNotEmpty()) {
                File(context.filesDir, newDirName).apply { if (!exists()) mkdir() }
            } else {
                context.filesDir
            }
            
            val output = File(dir, name ?: "temp.apk")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(output).use { out ->
                    input.copyTo(out)
                }
            }
            output.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun createInstallIntent(context: Context, apkFile: File): Intent {
        val fileUri = FileProvider.getUriForFile(
            context.applicationContext,
            "${context.packageName}.provider",
            apkFile
        )
        return Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setData(fileUri)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, VENDING_PKG)
        }
    }
}
