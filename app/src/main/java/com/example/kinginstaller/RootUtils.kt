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

    fun installApk(activity: Activity, filepath: String?, onStatusUpdate: (String) -> Unit, onSuccess: () -> Unit) {
        if (filepath == null) return
        
        onStatusUpdate("Launching Root Hybrid Install...")

        thread {
            try {
                val apkFile = File(filepath)
                val context = activity.applicationContext
                val fileUri = FileProvider.getUriForFile(
                    context, "${context.packageName}.provider", apkFile
                )

                // 1. Concediamo i permessi (Root può farlo per chiunque)
                val targetPackages = listOf("com.android.shell", "com.google.android.packageinstaller", "com.android.packageinstaller")
                targetPackages.forEach { pkg ->
                    try {
                        context.grantUriPermission(pkg, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (ignored: Exception) {}
                }

                // 2. Comando am start via Root (su)
                val amCommand = "am start " +
                        "-a android.intent.action.INSTALL_PACKAGE " +
                        "-d \"$fileUri\" " +
                        "-t \"application/vnd.android.package-archive\" " +
                        "-f 0x00000001 " + // FLAG_GRANT_READ_URI_PERMISSION
                        "--es android.intent.extra.INSTALLER_PACKAGE_NAME \"${InstallationUtils.VENDING_PKG}\" " +
                        "--es android.intent.extra.REFERRER_NAME \"android-app://${InstallationUtils.VENDING_PKG}\" " +
                        "--ei android.intent.extra.INSTALL_REASON 1 " + // 1 = STORE
                        "--ez android.intent.extra.NOT_UNKNOWN_SOURCE true"

                runSuWithCmd(amCommand)

                activity.runOnUiThread {
                    onStatusUpdate("")
                    onSuccess()
                }
            } catch (e: Exception) {
                activity.runOnUiThread {
                    onStatusUpdate("Root Error: ${e.message}")
                }
            }
        }
    }

    fun setInstallerViaRoot(packageName: String) {
        thread {
            val cmd = "cmd package set-installer $packageName ${InstallationUtils.VENDING_PKG}"
            runSuWithCmd(cmd)
            Log.d("KingInstaller", "Forced installer to Play Store for $packageName via Root")
        }
    }
}
