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
            // Filtra le righe di stacktrace ("    at com.android…"): lasciano solo rumore nella
            // finestra del log. Teniamo la riga d'eccezione utile (es. "Unknown calling UID: 0").
            val noStack = "grep -vE '^[[:space:]]*at '"
            val cmd = "echo '[u1000]'; su 1000 -c '$set' 2>&1 | $noStack; " +
                    "echo '[u2000]'; su 2000 -c '$set' 2>&1 | $noStack; " +
                    "echo '[root]'; $set 2>&1 | $noStack; " +
                    "echo '[installer=]'; dumpsys package $packageName 2>/dev/null | grep -m1 -i installerPackageName | tr -d ' '"
            val logs = runSuWithCmd(cmd)
            val detail = (logs.getInputStreamLog() + " " + logs.getErrorStreamLog())
                .replace("\n", " ").trim()
            Log.d("KingInstaller", "setInstallerViaRoot($packageName) -> '$detail'")
            onDone?.invoke(detail)
        }
    }

    /**
     * Root-equivalente di `ShizukuUtils.startInstallActivityAsShell`: lancia lo **stesso** intent
     * d'installazione del metodo Shizuku ma con identità **shell (uid 2000)** via `su`, invece che
     * col binder dell'ActivityTaskManager wrappato da Shizuku. Stessi extra
     * (`INSTALLER_PACKAGE_NAME=vending`, `NOT_UNKNOWN_SOURCE`, `REFERRER_NAME`, `INSTALL_REASON`) e
     * stessi grant d'URI (fatti da MainActivity, come in `installApk`).
     *
     * Perché così e non `set-installer`/`pm install -i`:
     * - il `set-installer` POST-installazione è rifiutato per OGNI uid (controllo di firma);
     * - `pm install -i` imposterebbe "Installed by" = vending ma lascerebbe "Requested by" a
     *   shell/nullo → AA ≥14 lo rifiuta.
     * Da shell (privilegiata) l'extra `INSTALLER_PACKAGE_NAME` è **onorato** → "Installed by" =
     * vending; ed è il packageinstaller a installare → "Requested by" =
     * `com.google.android.packageinstaller`. L'esito reale (verde/rosso) lo verifica poi il
     * `packageAddedReceiver`.
     */
    fun launchInstallerAsShell(
        uriString: String,
        onLaunched: ((ok: Boolean, detail: String) -> Unit)? = null
    ) {
        thread {
            val vending = InstallationUtils.VENDING_PKG
            val am = "am start" +
                    " -a android.intent.action.INSTALL_PACKAGE" +
                    " -d $uriString" +
                    " -t application/vnd.android.package-archive" +
                    " --es android.intent.extra.INSTALLER_PACKAGE_NAME $vending" +
                    " --ez android.intent.extra.NOT_UNKNOWN_SOURCE true" +
                    " --es android.intent.extra.REFERRER_NAME android-app://$vending" +
                    " --ei android.content.pm.extra.INSTALL_REASON 1"
            val logs = runSuWithCmd("su 2000 -c \"$am\" 2>&1")
            val detail = (logs.getInputStreamLog() + " " + logs.getErrorStreamLog())
                .replace("\n", " ").trim()
            val ok = detail.contains("Starting", ignoreCase = true) &&
                    !detail.contains("Error", ignoreCase = true)
            Log.d("KingInstaller", "launchInstallerAsShell ok=$ok -> '$detail'")
            onLaunched?.invoke(ok, detail)
        }
    }

    /**
     * Installa un pacchetto composto da più APK split (XAPK / APKS / APKM) eseguendo le operazioni
     * di install-create, install-write e install-commit come UID 2000 (Shell) via Root (`su`).
     */
    fun installSplitApksAsShell(
        packageName: String?,
        apkFiles: List<File>,
        onLaunched: ((ok: Boolean, detail: String) -> Unit)? = null
    ) {
        thread {
            try {
                val vending = InstallationUtils.VENDING_PKG
                val createCmd = "su -c \"cmd package install-create -i $vending --user 0 -r\" 2>&1"
                val createLogs = runSuWithCmd(createCmd)
                val createDetail = (createLogs.getInputStreamLog() + " " + createLogs.getErrorStreamLog()).trim()

                val sessionIdRegex = Regex("""\[(\d+)]""")
                var match = sessionIdRegex.find(createDetail)
                var sessionId = match?.groupValues?.get(1)

                if (sessionId == null) {
                    val fallbackCmd = "su -c \"pm install-create -i $vending -r\" 2>&1"
                    val fbLogs = runSuWithCmd(fallbackCmd)
                    val fbDetail = (fbLogs.getInputStreamLog() + " " + fbLogs.getErrorStreamLog()).trim()
                    match = sessionIdRegex.find(fbDetail)
                    sessionId = match?.groupValues?.get(1)

                    if (sessionId == null) {
                        Log.e("KingInstaller", "Impossibile creare sessione install-create Root: $createDetail | $fbDetail")
                        onLaunched?.invoke(false, createDetail)
                        return@thread
                    }
                }

                var writeSuccess = true
                val writeDetails = StringBuilder()

                for (apk in apkFiles) {
                    val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "cmd package install-write $sessionId ${apk.name} -"))
                    val os = proc.outputStream
                    apk.inputStream().use { input ->
                        input.copyTo(os)
                    }
                    os.flush()
                    os.close()
                    val exitCode = proc.waitFor()
                    if (exitCode != 0) {
                        val procFb = Runtime.getRuntime().exec(arrayOf("su", "-c", "pm install-write $sessionId ${apk.name} -"))
                        val osFb = procFb.outputStream
                        apk.inputStream().use { input ->
                            input.copyTo(osFb)
                        }
                        osFb.flush()
                        osFb.close()
                        val exitCodeFb = procFb.waitFor()
                        if (exitCodeFb != 0) {
                            writeSuccess = false
                            val err = procFb.errorStream.bufferedReader().readText()
                            writeDetails.append("Write ${apk.name} failed ($exitCodeFb): $err ")
                        }
                    }
                }

                if (!writeSuccess) {
                    runSuWithCmd("su -c \"cmd package install-abandon $sessionId\"")
                    onLaunched?.invoke(false, writeDetails.toString())
                    return@thread
                }

                val commitLogs = runSuWithCmd("su -c \"cmd package install-commit $sessionId\" 2>&1")
                val commitDetail = (commitLogs.getInputStreamLog() + " " + commitLogs.getErrorStreamLog()).trim()
                val ok = commitDetail.contains("Success", ignoreCase = true)
                Log.d("KingInstaller", "installSplitApksAsShell ok=$ok -> '$commitDetail'")
                onLaunched?.invoke(ok, commitDetail)

            } catch (e: Exception) {
                Log.e("KingInstaller", "installSplitApksAsShell exception", e)
                onLaunched?.invoke(false, e.message ?: e.toString())
            }
        }
    }
}
