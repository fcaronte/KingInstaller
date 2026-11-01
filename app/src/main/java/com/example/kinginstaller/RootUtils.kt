package com.example.kinginstaller

import android.os.Build
import android.util.Log
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

    /**
     * Pre-attiva il permesso root: esegue un comando innocuo così il prompt del superuser compare
     * SUBITO (mentre l'utente è ancora nel flusso d'installazione) e, al momento del fix
     * post-installazione, su è già concesso e il set-installer parte senza latenza.
     */
    fun prewarm() {
        thread {
            try { runSuWithCmd("id") } catch (e: Exception) { /* no-op: su assente */ }
        }
    }

    /**
     * Imposta Play Store come installer del pacchetto appena installato.
     *
     * ⚠️ `cmd package set-installer` eseguito come **root (uid 0)** è rifiutato da Android recente
     * (14/15/17) con `SecurityException: Unknown calling UID: 0`: il PackageManager pretende un uid
     * "noto" (associato a un pacchetto) e uid 0 non lo è. Lo eseguiamo quindi come **system
     * (uid 1000)** — uid noto e con INSTALL_PACKAGES, che aggira anche il check "same cert" — e come
     * **shell (uid 2000)**, infine root. Eseguiamo tutte le varianti e in coda leggiamo l'installer
     * REALE con `dumpsys`, così [onDone] riporta con certezza quale uid ha funzionato (o perché
     * ognuno fallisce). L'esito finale lo conferma comunque il polling via `isAACompatible`.
     */
    fun setInstallerViaRoot(packageName: String, onDone: ((detail: String) -> Unit)? = null) {
        thread {
            val target = InstallationUtils.VENDING_PKG
            val set = "cmd package set-installer $packageName $target"
            val cmd = "echo '[u1000]'; su 1000 -c '$set' 2>&1; " +
                    "echo '[u2000]'; su 2000 -c '$set' 2>&1; " +
                    "echo '[root]'; $set 2>&1; " +
                    "echo '[installer=]'; dumpsys package $packageName 2>/dev/null | grep -m1 -i installerPackageName | tr -d ' '"
            val logs = runSuWithCmd(cmd)
            val detail = (logs.getInputStreamLog() + " " + logs.getErrorStreamLog())
                .replace("\n", " ").trim()
            Log.d("KingInstaller", "setInstallerViaRoot($packageName) -> '$detail'")
            onDone?.invoke(detail)
        }
    }
}
