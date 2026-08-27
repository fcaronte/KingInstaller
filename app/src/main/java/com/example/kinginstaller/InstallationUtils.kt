package com.example.kinginstaller

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.net.toUri
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
        
        // Torniamo a ACTION_INSTALL_PACKAGE. Anche se deprecato, 
        // è quello che il sistema processa con più "trucchi" legacy.
        return Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setData(fileUri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, VENDING_PKG)
            
            // Varianti Extra trovate in diverse versioni di PackageInstaller
            putExtra("installerPackageName", VENDING_PKG)
            putExtra("android.content.pm.extra.VERIFICATION_INSTALLER_PACKAGE", VENDING_PKG)
            putExtra("android.content.pm.extra.VERIFICATION_INSTALLER_UID", 0) // UID di sistema
            
            // Ragione installazione (1 = STORE)
            putExtra("android.intent.extra.INSTALL_REASON", 1)
            
            // Referrer completo
            putExtra("android.intent.extra.REFERRER_NAME", "android-app://$VENDING_PKG")
            putExtra(Intent.EXTRA_REFERRER, "android-app://$VENDING_PKG".toUri())
            
            // Originating Info
            putExtra("android.intent.extra.ORIGINATING_PACKAGE", VENDING_PKG)
            putExtra(Intent.EXTRA_ORIGINATING_URI, "https://play.google.com/store/apps/details?id=${context.packageName}".toUri())
            
            if (Build.VERSION.SDK_INT >= 34) {
                putExtra("android.content.pm.extra.REQUEST_UPDATE_OWNERSHIP", true)
            }
        }
    }

    fun isAACompatible(context: Context, packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            var initiating: String? = null
            var installing: String? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val info = pm.getInstallSourceInfo(packageName)
                initiating = info.initiatingPackageName
                installing = info.installingPackageName
            } else {
                @Suppress("DEPRECATION")
                installing = pm.getInstallerPackageName(packageName)
            }

            val isPlayStoreInstalling = installing == VENDING_PKG
            val isValidInitiating = initiating == VENDING_PKG || 
                                   initiating == "com.google.android.packageinstaller" ||
                                   (initiating?.contains("packageinstaller") == true)

            isPlayStoreInstalling && (initiating == null || isValidInitiating)
        } catch (e: Exception) {
            false
        }
    }
}
