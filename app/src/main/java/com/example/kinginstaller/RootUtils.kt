package com.example.kinginstaller

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import java.io.*
import kotlin.concurrent.thread

object RootUtils {

    val isDeviceRooted: Boolean
        get() = checkRootMethod1() || checkRootMethod2() ||
                checkRootMethod3() || checkRootMethod4()

    private fun checkRootMethod1(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkRootMethod2(): Boolean {
        val paths = arrayOf<String?>(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/magisk/.core/bin/su",
            "/data/adb/magisk/su",
            "/data/adb/ksu/bin/su"
        )
        for (path in paths) {
            if (path != null && File(path).exists()) return true
        }
        return false
    }

    private fun checkRootMethod3(): Boolean {
        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val `in` = BufferedReader(InputStreamReader(process.inputStream))
            return `in`.readLine() != null
        } catch (t: Throwable) {
            return false
        } finally {
            process?.destroy()
        }
    }

    private fun checkRootMethod4(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor()
            true
        } catch (e: Exception) {
            false
        } finally {
            process?.destroy()
        }
    }

    fun runSuWithCmd(cmd: String?): StreamLogs {
        var outputStream: DataOutputStream? = null
        var inputStream: InputStream? = null
        var errorStream: InputStream? = null

        val streamLogs = StreamLogs()
        streamLogs.setOutputStreamLog(cmd)

        try {
            val su = Runtime.getRuntime().exec("su")
            outputStream = DataOutputStream(su.outputStream)
            inputStream = su.inputStream
            errorStream = su.errorStream

            outputStream.writeBytes(cmd + "\n")
            outputStream.flush()
            outputStream.writeBytes("exit\n")
            outputStream.flush()

            try {
                su.waitFor()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            streamLogs.setInputStreamLog(readStream(inputStream))
            streamLogs.setErrorStreamLog(readStream(errorStream))
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return streamLogs
    }

    @Throws(IOException::class)
    private fun readStream(`is`: InputStream): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var length: Int
        while ((`is`.read(buffer).also { length = it }) != -1) {
            byteArrayOutputStream.write(buffer, 0, length)
        }
        return byteArrayOutputStream.toString("UTF-8")
    }

    @Suppress("DEPRECATION")
    fun installApk(activity: Activity, filepath: String?, onStatusUpdate: (String) -> Unit, onSuccess: () -> Unit) {
        if (filepath == null) return

        onStatusUpdate(activity.getString(R.string.launching_root_install))

        thread {
            try {
                val context = activity.applicationContext
                val apkFile = File(filepath)

                // URI content:// via FileProvider (come il flusso Shizuku): più compatibile del file://.
                val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)

                // Risolviamo l'activity del package installer DI SISTEMA (escludendo KingInstaller):
                // senza componente esplicito, l'am start potrebbe risolvere sul nostro .ApkHandler e
                // aprire KingInstaller invece dell'installer, senza installare nulla.
                val probe = Intent(Intent.ACTION_INSTALL_PACKAGE)
                    .setDataAndType(fileUri, "application/vnd.android.package-archive")
                val myPkg = context.packageName
                val handlers = context.packageManager.queryIntentActivities(probe, 0).map { it.activityInfo }
                val target = handlers.firstOrNull { it.packageName != myPkg && it.packageName.contains("packageinstaller") }
                    ?: handlers.firstOrNull { it.packageName != myPkg }

                // Permesso di lettura dell'URI all'installer scelto e ai noti.
                val grantTargets = listOfNotNull(
                    target?.packageName,
                    "com.google.android.packageinstaller",
                    "com.android.packageinstaller",
                    "com.miui.packageinstaller"
                ).distinct()
                grantTargets.forEach {
                    try { context.grantUriPermission(it, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
                }

                val componentArg = if (target != null) "-n ${target.packageName}/${target.name} " else ""

                // Dialog di installazione via Root: "Requested by" resta il package installer (non shell);
                // "Installed by" viene poi forzato a Play Store da setInstallerViaRoot.
                val amCommand = "am start " + componentArg +
                        "-a android.intent.action.INSTALL_PACKAGE " +
                        "-d \"$fileUri\" " +
                        "-t \"application/vnd.android.package-archive\" " +
                        "-f 0x00000001 " + // FLAG_GRANT_READ_URI_PERMISSION
                        "--es android.intent.extra.INSTALLER_PACKAGE_NAME \"${InstallationUtils.VENDING_PKG}\" " +
                        "--es android.intent.extra.REFERRER_NAME \"android-app://${InstallationUtils.VENDING_PKG}\" " +
                        "--ei android.intent.extra.INSTALL_REASON 1 " + // 1 = STORE
                        "--ez android.intent.extra.NOT_UNKNOWN_SOURCE true"

                val logs = runSuWithCmd(amCommand)
                Log.d("KingInstaller", "Root install (target=${target?.packageName ?: "?"}) -> out='${logs.getInputStreamLog()}' err='${logs.getErrorStreamLog()}'")

                activity.runOnUiThread {
                    onStatusUpdate("")
                    onSuccess()
                }
            } catch (e: Exception) {
                activity.runOnUiThread {
                    onStatusUpdate(activity.getString(R.string.root_error, e.message ?: ""))
                }
            }
        }
    }

    fun setInstallerViaRoot(packageName: String) {
        thread {
            val target = InstallationUtils.VENDING_PKG
            // Più varianti del comando (cambiano tra versioni Android/ROM): ci fermiamo alla prima
            // che riesce (|| sul codice d'uscita). Come root (uid 0) il controllo di firma
            // "same cert" del package manager viene aggirato — cosa che lo shell non può fare.
            val cmd = "pm set-installer $packageName $target 2>&1 || " +
                    "cmd package set-installer $packageName $target 2>&1 || " +
                    "cmd package set-installer-package --user 0 $packageName $target 2>&1"
            val logs = runSuWithCmd(cmd)
            Log.d("KingInstaller", "setInstallerViaRoot($packageName) -> out='${logs.getInputStreamLog()}' err='${logs.getErrorStreamLog()}'")
        }
    }
}
