package com.example.kinginstaller

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

data class SplitArchiveResult(
    val packageName: String?,
    val apkFiles: List<File>,
    val obbFiles: List<File>,
    val error: String? = null
) {
    val isSingleApk: Boolean get() = apkFiles.size == 1
}

object PackageArchiveUtils {

    private const val TAG = "PackageArchiveUtils"

    fun isSplitArchive(file: File): Boolean {
        val name = file.name.lowercase()
        return name.endsWith(".xapk") || name.endsWith(".apks") || name.endsWith(".apkm") || name.endsWith(".zip")
    }

    fun isSplitArchive(path: String?): Boolean {
        if (path == null) return false
        return isSplitArchive(File(path))
    }

    fun extractArchive(context: Context, archiveFile: File): SplitArchiveResult {
        if (!archiveFile.exists()) {
            return SplitArchiveResult(null, emptyList(), emptyList(), "File non trovato")
        }

        val outputDir = File(context.filesDir, "split_temp_${System.currentTimeMillis()}").apply { mkdirs() }
        val apkFiles = mutableListOf<File>()
        val obbFiles = mutableListOf<File>()
        var jsonPackageName: String? = null

        try {
            ZipFile(archiveFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.isDirectory) continue

                    val entryName = entry.name
                    val lowercaseName = entryName.lowercase()
                    val fileName = File(entryName).name

                    if (lowercaseName.endsWith(".apk")) {
                        val targetFile = File(outputDir, fileName)
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(targetFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        apkFiles.add(targetFile)
                    } else if (lowercaseName.endsWith(".obb")) {
                        val obbDir = File(outputDir, "obb").apply { if (!exists()) mkdirs() }
                        val targetFile = File(obbDir, fileName)
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(targetFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        obbFiles.add(targetFile)
                    } else if (fileName.equals("manifest.json", ignoreCase = true) ||
                        fileName.equals("info.json", ignoreCase = true)
                    ) {
                        val jsonText = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                        jsonPackageName = parsePackageNameFromJson(jsonText)
                    }
                }
            }

            if (apkFiles.isEmpty()) {
                outputDir.deleteRecursively()
                return SplitArchiveResult(null, emptyList(), emptyList(), "Nessun file APK trovato nell'archivio")
            }

            val sortedApks = sortApkFiles(context, apkFiles)
            val baseApk = sortedApks.first()

            val detectedPackageName = jsonPackageName
                ?: InstallationUtils.getPackageNameFromApk(context, baseApk)

            if (!detectedPackageName.isNullOrEmpty() && obbFiles.isNotEmpty()) {
                copyObbFiles(detectedPackageName, obbFiles)
            }

            Log.d(TAG, "Estratti ${sortedApks.size} APK (base: ${baseApk.name}) per il pacchetto '$detectedPackageName'")
            return SplitArchiveResult(detectedPackageName, sortedApks, obbFiles)

        } catch (e: Exception) {
            Log.e(TAG, "Errore durante l'estrazione dell'archivio", e)
            outputDir.deleteRecursively()
            return SplitArchiveResult(null, emptyList(), emptyList(), e.localizedMessage ?: e.message ?: e.toString())
        }
    }

    private fun parsePackageNameFromJson(json: String): String? {
        val regex = Regex(""""(?:package_name|pname|package)"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE)
        return regex.find(json)?.groupValues?.get(1)
    }

    private fun sortApkFiles(context: Context, apkFiles: List<File>): List<File> {
        return apkFiles.sortedWith { f1, f2 ->
            val isBase1 = isBaseApk(context, f1)
            val isBase2 = isBaseApk(context, f2)
            when {
                isBase1 && !isBase2 -> -1
                !isBase1 && isBase2 -> 1
                else -> f1.name.length.compareTo(f2.name.length)
            }
        }
    }

    fun isBaseApk(context: Context, apkFile: File): Boolean {
        val name = apkFile.name.lowercase()
        if (name == "base.apk" || name.contains("base-master") || name.contains("standalone")) {
            return true
        }

        return try {
            val pm = context.packageManager
            val info = pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
            val splitName = try {
                info?.let {
                    val field = it.javaClass.getField("splitName")
                    field.get(it) as? String
                }
            } catch (_: Exception) { null }

            splitName == null && (name.contains("base") || info?.packageName != null)
        } catch (_: Exception) {
            name.contains("base")
        }
    }

    fun isCompatibleAbiSplit(fileName: String): Boolean {
        val lower = fileName.lowercase().replace("-", "_")
        val isArm64 = lower.contains("arm64")
        val isArmv7 = lower.contains("armeabi") || lower.contains("armv7")
        val isX86_64 = lower.contains("x86_64")
        val isX86 = lower.contains("x86") && !isX86_64

        val deviceAbis = android.os.Build.SUPPORTED_ABIS.map { it.lowercase().replace("-", "_") }
        val deviceHasArm64 = deviceAbis.any { it.contains("arm64") }
        val deviceHasArmv7 = deviceAbis.any { it.contains("armeabi") || it.contains("v7") }
        val deviceHasX86_64 = deviceAbis.any { it.contains("x86_64") }
        val deviceHasX86 = deviceAbis.any { it.contains("x86") && !it.contains("x86_64") }

        if (isArm64 && !deviceHasArm64) return false
        if (isArmv7 && !deviceHasArmv7) return false
        if (isX86_64 && !deviceHasX86_64) return false
        if (isX86 && !deviceHasX86) return false

        return true
    }

    enum class SplitType {
        BASE,
        ABI_COMPATIBLE,
        ABI_INCOMPATIBLE,
        DENSITY_COMPATIBLE,
        DENSITY_INCOMPATIBLE,
        LANG_COMPATIBLE,
        LANG_INCOMPATIBLE,
        GENERIC
    }

    fun classifySplit(context: Context, apkFile: File): SplitType {
        if (isBaseApk(context, apkFile)) {
            return SplitType.BASE
        }

        val fileName = apkFile.name
        val lower = fileName.lowercase().replace("-", "_")

        // 1. ABI Check
        if (lower.contains("arm64") || lower.contains("armv7") || lower.contains("armeabi") || lower.contains("x86") || lower.contains("abi")) {
            return if (isCompatibleAbiSplit(fileName)) SplitType.ABI_COMPATIBLE else SplitType.ABI_INCOMPATIBLE
        }

        // 2. Density Check (Tutti gli split di densità vengono considerati compatibili/pre-selezionati per sicurezza di risorse)
        if (lower.contains("dpi") || lower.contains("hdpi") || lower.contains("xhdpi") || lower.contains("xxhdpi") || lower.contains("xxxhdpi") || lower.contains("mdpi")) {
            return SplitType.DENSITY_COMPATIBLE
        }

        // 3. Language Check
        val systemLang = java.util.Locale.getDefault().language.lowercase()
        val allLangs = listOf(
            "af", "am", "ar", "as", "az", "be", "bg", "bn", "bs", "ca", "cs", "da", "de", "el", "en", "es", "et", "eu", "fa", "fi", "fr", "gl", "gu", "hi", "hr", "hu", "hy", "in", "is", "it", "iw", "ja", "ka", "kk", "km", "kn", "ko", "ky", "lo", "lt", "lv", "mk", "ml", "mn", "mr", "ms", "my", "nb", "ne", "nl", "or", "pa", "pl", "pt", "ro", "ru", "si", "sk", "sl", "sq", "sr", "sv", "sw", "ta", "te", "th", "tl", "tr", "uk", "ur", "uz", "vi", "zh", "zu"
        )
        val foundLang = allLangs.find { lang ->
            lower.contains(".$lang.") || lower.contains("_$lang") || lower.endsWith("$lang.apk") || lower.contains("config.$lang") || lower.contains("split_$lang")
        }

        if (foundLang != null) {
            if (foundLang == systemLang || foundLang == "en") {
                return SplitType.LANG_COMPATIBLE
            } else {
                return SplitType.LANG_INCOMPATIBLE
            }
        }

        return SplitType.GENERIC
    }

    private fun copyObbFiles(packageName: String, obbFiles: List<File>) {
        try {
            val obbTargetDir = File(Environment.getExternalStorageDirectory(), "Android/obb/$packageName")
            if (!obbTargetDir.exists()) {
                obbTargetDir.mkdirs()
            }
            for (obb in obbFiles) {
                val dest = File(obbTargetDir, obb.name)
                obb.inputStream().use { input ->
                    FileOutputStream(dest).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "Copiato file OBB ${obb.name} in $dest")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Impossibile copiare i file OBB per $packageName: ${e.message}")
        }
    }
}
