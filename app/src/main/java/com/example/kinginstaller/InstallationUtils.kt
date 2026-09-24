package com.example.kinginstaller

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
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

    @SuppressLint("UnsanitizedFilenameFromContentProvider")
    fun copyFileToInternalStorage(context: Context, uri: Uri, newDirName: String): String? {
        return try {
            val returnCursor = context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )
            val nameIndex = returnCursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME) ?: -1
            returnCursor?.moveToFirst()
            var name = if (nameIndex != -1) returnCursor?.getString(nameIndex) else "temp.apk"
            returnCursor?.close()

            if (name.isNullOrEmpty()) {
                name = "temp.apk"
            }
            if (!name.lowercase().endsWith(".apk")) {
                name = "$name.apk"
            }

            val dir = if (newDirName.isNotEmpty()) {
                File(context.filesDir, newDirName).apply { if (!exists()) mkdir() }
            } else {
                context.filesDir
            }
            
            val output = File(dir, name)
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

    @Suppress("DEPRECATION")
    @SuppressLint("RequestInstallPackagesPolicy")
    fun createInstallIntent(context: Context, apkFile: File): Intent {
        val fileUri = FileProvider.getUriForFile(
            context.applicationContext,
            "${context.packageName}.provider",
            apkFile
        )
        
        return Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setDataAndType(fileUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, VENDING_PKG)
            
            putExtra("installerPackageName", VENDING_PKG)
            putExtra("android.content.pm.extra.VERIFICATION_INSTALLER_PACKAGE", VENDING_PKG)
            putExtra("android.content.pm.extra.VERIFICATION_INSTALLER_UID", 0)
            
            putExtra("android.intent.extra.INSTALL_REASON", 1)
            
            putExtra("android.intent.extra.REFERRER_NAME", "android-app://$VENDING_PKG")
            putExtra(Intent.EXTRA_REFERRER, "android-app://$VENDING_PKG".toUri())
            
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val info = pm.getInstallSourceInfo(packageName)
                val initiating = info.initiatingPackageName
                val installing = info.installingPackageName

                // Se i metadati non sono pronti, non considerare compatibile
                if (initiating == null || installing == null) {
                    return false
                }

                // 1. L'iniziatore non deve MAI essere la shell
                if (initiating == "com.android.shell") {
                    return false
                }

                // 2. L'installante (installingPackageName) DEVE ESSERE ESCLUSIVAMENTE Play Store (com.android.vending)
                val isInstallingVending = installing == VENDING_PKG

                // 3. Il richiedente / iniziatore può essere package installer o play store (purché non shell)
                val isValidInitiating = initiating == VENDING_PKG || 
                                       initiating == GOOGLE_INSTALLER_PKG || 
                                       initiating == "com.android.packageinstaller" ||
                                       initiating.contains("packageinstaller")

                isInstallingVending && isValidInitiating
            } else {
                @Suppress("DEPRECATION")
                val installer = pm.getInstallerPackageName(packageName)
                installer == VENDING_PKG
            }
        } catch (e: Exception) {
            false
        }
    }

    fun getPackageNameFromApk(context: Context, apkFile: File): String? {
        return try {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
            }
            info?.packageName
        } catch (e: Exception) {
            null
        }
    }
}
