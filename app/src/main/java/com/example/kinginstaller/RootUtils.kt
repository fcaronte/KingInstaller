package com.example.kinginstaller

import android.os.Build
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

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
}
