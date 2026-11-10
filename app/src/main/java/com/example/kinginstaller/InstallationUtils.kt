package com.example.kinginstaller

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object InstallationUtils {

    const val GOOGLE_INSTALLER_PKG = "com.google.android.packageinstaller"
    const val VENDING_PKG = "com.android.vending"

    enum class AACompatibilityStatus {
        PENDING,
        COMPATIBLE,
        INCOMPATIBLE
    }

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
                dir.listFiles()?.forEach { it.deleteRecursively() }
            }
            val splitDirs = context.filesDir.listFiles { _, name -> name.startsWith("split_temp_") }
            splitDirs?.forEach { it.deleteRecursively() }
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
            // Sanitizza il nome per evitare percorsi o subpath (es. "Download/app.apk")
            name = File(name).name
            val lower = name.lowercase()
            if (!lower.endsWith(".apk") && !lower.endsWith(".xapk") && !lower.endsWith(".apks") && !lower.endsWith(".apkm")) {
                name = "$name.apk"
            }

            val dir = if (newDirName.isNotEmpty()) {
                File(context.filesDir, newDirName).apply { if (!exists()) mkdir() }
            } else {
                context.filesDir
            }
            
            val output = File(dir, name)

            // Evita di copiare un file su se stesso (loop di troncamento a 0 byte)
            if (output.exists() && (uri.scheme == "file" && uri.path == output.absolutePath || uri.toString().contains(context.packageName))) {
                android.util.Log.d("InstallationUtils", "File già presente nella memoria interna, riutilizzo: ${output.absolutePath}")
                return output.absolutePath
            }

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(output).use { out ->
                    input.copyTo(out)
                }
            }
            output.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("InstallationUtils", "Errore in copyFileToInternalStorage per URI: $uri", e)
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
            putExtra("android.intent.extra.INSTALL_REASON", 1)
        }
    }

    fun isAACompatible(context: Context, packageName: String): Boolean {
        return getAACompatibilityStatus(context, packageName) == AACompatibilityStatus.COMPATIBLE
    }

    fun getAACompatibilityStatus(context: Context, packageName: String): AACompatibilityStatus {
        return try {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val info = pm.getInstallSourceInfo(packageName)
                val initiating = info.initiatingPackageName
                val installing = info.installingPackageName
                if (initiating == null || installing == null) {
                    return AACompatibilityStatus.PENDING
                }

                val isValidInitiating = initiating != "com.android.shell" &&
                    (initiating == VENDING_PKG ||
                        initiating == GOOGLE_INSTALLER_PKG ||
                        initiating == "com.android.packageinstaller" ||
                        initiating.contains("packageinstaller"))
                if (installing == VENDING_PKG && isValidInitiating) {
                    AACompatibilityStatus.COMPATIBLE
                } else {
                    AACompatibilityStatus.INCOMPATIBLE
                }
            } else {
                @Suppress("DEPRECATION")
                val installer = pm.getInstallerPackageName(packageName)
                when (installer) {
                    null -> AACompatibilityStatus.PENDING
                    VENDING_PKG -> AACompatibilityStatus.COMPATIBLE
                    else -> AACompatibilityStatus.INCOMPATIBLE
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            AACompatibilityStatus.PENDING
        } catch (e: Exception) {
            AACompatibilityStatus.INCOMPATIBLE
        }
    }

    /** URI `content://` dell'APK via FileProvider, da passare al package installer di sistema. */
    fun getApkContentUri(context: Context, apkFile: File): Uri =
        FileProvider.getUriForFile(context.applicationContext, "${context.packageName}.provider", apkFile)

    /**
     * Package noti a cui concedere la lettura dell'URI dell'APK prima di lanciare l'installer come shell.
     * Include Google, AOSP e i principali installer OEM (Samsung, Xiaomi/MIUI, Oppo/OnePlus, Huawei)
     * oltre a com.android.shell.
     */
    val INSTALLER_PACKAGES = listOf(
        "com.android.shell",
        GOOGLE_INSTALLER_PKG,
        "com.android.packageinstaller",
        "com.samsung.android.packageinstaller",
        "com.sec.android.app.samsungapps",
        "com.miui.packageinstaller",
        "com.coloros.packageinstaller",
        "com.oplus.packageinstaller",
        "com.huawei.appmarket"
    )

    /**
     * Concede dinamicamente il permesso di lettura dell'URI FileProvider a tutti i potenziali
     * installer di sistema rilevati nel dispositivo e alla shell, garantendo che nessun installer
     * OEM incorra in SecurityException: Permission Denial.
     */
    fun grantUriPermissionToInstallers(context: Context, fileUri: Uri) {
        val targets = mutableSetOf<String>()
        targets.addAll(INSTALLER_PACKAGES)

        try {
            val pm = context.packageManager
            val testIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                setDataAndType(fileUri, "application/vnd.android.package-archive")
            }
            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(testIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(testIntent, 0)
            }
            for (info in resolveInfos) {
                val pkg = info.activityInfo?.packageName
                if (!pkg.isNullOrEmpty() && pkg != context.packageName) {
                    targets.add(pkg)
                }
            }
        } catch (_: Exception) {}

        for (pkg in targets) {
            try {
                context.grantUriPermission(pkg, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
        }
    }

    /**
     * Riepilogo leggibile dei campi di origine installazione, per diagnostica.
     * Utile su Xiaomi/One UI per capire perché un'app non risulta compatibile con AA
     * (es. "Requested by" = com.android.shell, oppure "Installed by" != com.android.vending).
     */
    fun getInstallSourceSummary(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val info = pm.getInstallSourceInfo(packageName)
                "Installed by: ${info.installingPackageName ?: "-"} | Requested by: ${info.initiatingPackageName ?: "-"}"
            } else {
                @Suppress("DEPRECATION")
                "Installed by: ${pm.getInstallerPackageName(packageName) ?: "-"}"
            }
        } catch (e: Exception) {
            "install source n/d (${e.message})"
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

    fun installSplitApksClassico(
        ctx: Context,
        packageName: String?,
        apkFiles: List<File>,
        errorCallback: ((String) -> Unit)? = null
    ) {
        val context = ctx
        kotlin.concurrent.thread {
            try {
                val pm = context.packageManager
                val installer = pm.packageInstaller
                val params = android.content.pm.PackageInstaller.SessionParams(
                    android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL
                )
                // Non impostiamo setAppPackageName o setInstallerPackageName per app non privilegiate su Android 14+ (API 34+),
                // in quanto solleverebbero SecurityException o IllegalArgumentException. Il sistema legge automaticamente
                // il nome pacchetto dall'APK base.

                val sessionId = installer.createSession(params)
                val session = installer.openSession(sessionId)

                for (apk in apkFiles) {
                    session.openWrite(apk.name, 0, apk.length()).use { out ->
                        apk.inputStream().use { input ->
                            input.copyTo(out)
                        }
                        session.fsync(out)
                    }
                }

                val intent = Intent(context, MainActivity::class.java).apply {
                    action = "com.example.kinginstaller.INSTALL_COMPLETE"
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = android.app.PendingIntent.getActivity(
                    context,
                    sessionId,
                    intent,
                    android.app.PendingIntent.FLAG_MUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
                session.commit(pendingIntent.intentSender)
                session.close()
                android.util.Log.d("InstallationUtils", "session.commit completato con successo per session $sessionId")
            } catch (e: Exception) {
                android.util.Log.e("InstallationUtils", "Errore in installSplitApksClassico", e)
                errorCallback?.invoke(e.localizedMessage ?: e.message ?: e.toString())
            }
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("RequestInstallPackagesPolicy")
    fun createSplitInstallIntent(context: Context, apkFiles: List<File>): Intent {
        val baseApk = apkFiles.first()
        val splitApks = apkFiles.drop(1)

        val baseUri = FileProvider.getUriForFile(
            context.applicationContext,
            "${context.packageName}.provider",
            baseApk
        )

        val splitUris = ArrayList<Uri>()
        for (split in splitApks) {
            splitUris.add(
                FileProvider.getUriForFile(
                    context.applicationContext,
                    "${context.packageName}.provider",
                    split
                )
            )
        }

        return Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setDataAndType(baseUri, "application/vnd.android.package-archive")
            if (splitUris.isNotEmpty()) {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, splitUris)
            }
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK

            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, VENDING_PKG)
            putExtra("installerPackageName", VENDING_PKG)
            putExtra("android.intent.extra.INSTALL_REASON", 1)
        }
    }
}
