package com.example.kinginstaller

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Raccoglie un report diagnostico (device, ROM, stato app, origine installazione, logcat del
 * processo) e lo condivide come file di testo. Pensato per farsi inviare i log dagli utenti,
 * in particolare su Xiaomi/MIUI/HyperOS dove gli extra dell'intent vengono ignorati.
 */
object DiagnosticsUtils {

    private val ROM_PROPS = listOf(
        "ro.miui.ui.version.name",
        "ro.mi.os.version.name",
        "ro.mi.os.version.incremental",
        "ro.build.version.incremental",
        "ro.product.mod_device",
        "ro.miui.region",
        "persist.sys.miui_optimization"
    )

    private fun getProp(key: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("getprop", key))
            val value = process.inputStream.bufferedReader().use { it.readText().trim() }
            process.waitFor()
            value.ifEmpty { "-" }
        } catch (e: Exception) {
            "-"
        }
    }

    private fun readOwnLogcat(): String {
        // Senza il permesso READ_LOGS logcat restituisce solo i log del nostro processo:
        // è esattamente ciò che ci serve. Proviamo prima con i tag dell'app, poi senza filtro.
        val filtered = arrayOf(
            "logcat", "-d", "-v", "time", "-t", "2000",
            "KingInstaller:V", "ShizukuUtils:V", "MyUserService:V",
            "MainActivity:V", "InstallationResultReceiver:V", "AndroidRuntime:E", "*:S"
        )
        val unfiltered = arrayOf("logcat", "-d", "-v", "time", "-t", "1000")
        return try {
            var out = runLogcat(filtered)
            if (out.isBlank()) out = runLogcat(unfiltered)
            out.ifBlank { "(logcat vuoto: la ROM potrebbe bloccare la lettura dei log)" }
        } catch (e: Exception) {
            "(logcat non disponibile: ${e.message})"
        }
    }

    private fun runLogcat(cmd: Array<String>): String {
        val process = Runtime.getRuntime().exec(cmd)
        val out = process.inputStream.bufferedReader().use { it.readText().trim() }
        process.waitFor()
        return out
    }

    fun buildReport(context: Context, lastPackage: String?): String {
        val sb = StringBuilder()
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        val pm = context.packageManager
        val (versionName, versionCode) = try {
            val info = pm.getPackageInfo(context.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
            (info.versionName ?: "?") to code
        } catch (e: Exception) { "?" to -1L }

        sb.appendLine("=== KingInstaller Diagnostics ===")
        sb.appendLine("Time: $now")
        sb.appendLine("App: KingInstaller v$versionName ($versionCode)")
        sb.appendLine()

        sb.appendLine("-- Device --")
        sb.appendLine("Manufacturer: ${Build.MANUFACTURER}")
        sb.appendLine("Brand: ${Build.BRAND}")
        sb.appendLine("Model: ${Build.MODEL}")
        sb.appendLine("Device: ${Build.DEVICE}")
        sb.appendLine("Product: ${Build.PRODUCT}")
        sb.appendLine()

        sb.appendLine("-- Android --")
        sb.appendLine("Release: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("Security patch: ${Build.VERSION.SECURITY_PATCH}")
        sb.appendLine("Build display: ${Build.DISPLAY}")
        sb.appendLine()

        sb.appendLine("-- ROM props --")
        ROM_PROPS.forEach { sb.appendLine("$it: ${getProp(it)}") }
        sb.appendLine()

        sb.appendLine("-- KingInstaller state --")
        val oppo = context.getSharedPreferences("oppo_trick_value", Context.MODE_PRIVATE).getBoolean("oppo_trick_value", false)
        val root = context.getSharedPreferences("root_trick_value", Context.MODE_PRIVATE).getBoolean("root_trick_value", false)
        val shizuku = context.getSharedPreferences("shizuku_trick_value", Context.MODE_PRIVATE).getBoolean("shizuku_trick_value", false)
        val method = when { shizuku -> "Shizuku"; root -> "Root"; oppo -> "Oppo"; else -> "Classic" }
        sb.appendLine("Selected method: $method")
        sb.appendLine("Shizuku available: ${ShizukuUtils.isShizukuAvailable()}")
        sb.appendLine("Shizuku permission: ${ShizukuUtils.hasShizukuPermission()}")
        sb.appendLine("Device rooted: ${RootUtils.isDeviceRooted}")
        val googleInstaller = try { pm.getPackageInfo(InstallationUtils.GOOGLE_INSTALLER_PKG, 0); true } catch (e: Exception) { false }
        sb.appendLine("Google Package Installer present: $googleInstaller")
        val canInstall = try { pm.canRequestPackageInstalls() } catch (e: Exception) { false }
        sb.appendLine("Can request installs: $canInstall")
        sb.appendLine()

        sb.appendLine("-- Last installed package --")
        if (lastPackage.isNullOrEmpty()) {
            sb.appendLine("Package: -")
        } else {
            sb.appendLine("Package: $lastPackage")
            sb.appendLine(InstallationUtils.getInstallSourceSummary(context, lastPackage))
        }
        sb.appendLine()

        sb.appendLine("-- Logcat (own process) --")
        sb.appendLine(readOwnLogcat())

        return sb.toString()
    }

    fun shareDiagnostics(activity: Activity, lastPackage: String?) {
        try {
            val report = buildReport(activity, lastPackage)
            val file = File(activity.cacheDir, "kinginstaller-diagnostics.txt")
            file.writeText(report)

            val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.provider", file)
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "KingInstaller diagnostics")
                // Testo inline (troncato) per le app che non gestiscono l'allegato.
                putExtra(Intent.EXTRA_TEXT, report.take(3500))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(
                Intent.createChooser(share, activity.getString(R.string.share_log_chooser))
            )
        } catch (e: Exception) {
            Log.e("DiagnosticsUtils", "Errore condivisione log", e)
            Toast.makeText(
                activity,
                activity.getString(R.string.share_log_error, e.message ?: ""),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
