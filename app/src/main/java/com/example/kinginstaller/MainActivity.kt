package com.example.kinginstaller

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.color.DynamicColors
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    var oppoTrickEnabled: Boolean = false
    var rootTrickEnabled: Boolean = false
    var forceRootEnabled: Boolean = false
    private var selectedFilePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        DynamicColors.applyToActivitiesIfAvailable(this.application)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Pulisce i file temporanei all'avvio
        try {
            clearTempFile()
        } catch (ignored: Exception) {
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }

        handleIntent(intent)
        val tvStatus = findViewById<TextView>(R.id.textViewError)
        if (isGooglePackageExist) {
            tvStatus.setText(R.string.google_package_installer_is_installed)
        } else {
            tvStatus.setText(R.string.missing_google_package_installer)
        }

        try {
            checkManageExternalStoragePermission()
        } catch (e: Exception) {
            tvStatus.text = getString(R.string.error_occurred, e.toString())
        }

        val btnSelect = findViewById<Button>(R.id.selectButton)
        btnSelect.setOnClickListener {
            try {
                showFileChooser()
            } catch (e: Exception) {
                val tv = findViewById<TextView>(R.id.textViewError)
                tv.text = getString(R.string.error_occurred, e.toString())
            }
        }

        val siteAnnexhack = findViewById<TextView>(R.id.site_annexhack)
        siteAnnexhack.setOnClickListener {
            try {
                val url = "https://inceptive.ru"
                val i = Intent(Intent.ACTION_VIEW)
                i.setData(url.toUri())
                startActivity(i)
            } catch (e: Exception) {
                val tv = findViewById<TextView>(R.id.textViewError)
                tv.text = getString(R.string.error_occurred, e.toString())
            }
        }

        //MAKE OPPO TRICK DISABLED AS DEFAULT AND AVOID HAVE AN UNUSEFUL FAKE INSTALLER
        val oppoTrickStatus = getSharedPreferences("oppo_trick_value", MODE_PRIVATE)
        oppoTrickEnabled = oppoTrickStatus.getBoolean("oppo_trick_value", false)
        val oppoTrick = findViewById<CheckBox>(R.id.checkBox1)
        oppoTrick.isChecked = oppoTrickEnabled
        //MAKE ROOT TRICK DISABLED AS DEFAULT
        val rootTrickStatus = getSharedPreferences("root_trick_value", MODE_PRIVATE)
        rootTrickEnabled = rootTrickStatus.getBoolean("root_trick_value", false)
        val rootTrick = findViewById<CheckBox>(R.id.checkBox2)
        rootTrick.isChecked = rootTrickEnabled
        oppoTrick()

        oppoTrick.setOnClickListener {
            oppoTrickEnabled = !oppoTrickEnabled
            oppoTrickStatus.edit {
                putBoolean("oppo_trick_value", oppoTrickEnabled)
            }
            oppoTrick.isChecked = oppoTrickEnabled
            //Switch off root flags
            rootTrickStatus.edit {
                putBoolean("root_trick_value", false)
            }
            rootTrick.isChecked = false
            oppoTrick()

            Log.d(
                "oppo button",
                "oppo value is " + oppoTrickStatus.getBoolean("oppo_trick_value", false)
            )
            Log.d(
                "root button",
                "root value is " + rootTrickStatus.getBoolean("root_trick_value", false)
            )
        }

        rootTrick.setOnClickListener {
            val tv = findViewById<TextView>(R.id.textViewError)
            if (!isDeviceRooted) {
                Toast.makeText(baseContext, R.string.device_not_rooted, Toast.LENGTH_SHORT)
                    .show()
                //Switch off root flags
                rootTrickStatus.edit {
                    putBoolean("root_trick_value", false)
                }
                rootTrick.isChecked = false
            } else if (isGooglePackageExist && !forceRootEnabled) {
                tv.setText(R.string.root_method_warning)
                //Switch off root flags
                rootTrickStatus.edit {
                    putBoolean("root_trick_value", false)
                }
                rootTrick.isChecked = false
                forceRootEnabled = true
            } else {
                tv.text = ""
                forceRootEnabled = !rootTrickEnabled
                rootTrickEnabled = !rootTrickEnabled
                rootTrickStatus.edit {
                    putBoolean("root_trick_value", rootTrickEnabled)
                }
                rootTrick.isChecked = rootTrickEnabled
                //Switch off oppo flags
                oppoTrickStatus.edit {
                    putBoolean("oppo_trick_value", false)
                }
                oppoTrick.isChecked = false
                oppoTrick()
            }
            Log.d("root check", "is phone rooted $isDeviceRooted")
            Log.d(
                "oppo button",
                "oppo value is " + oppoTrickStatus.getBoolean("oppo_trick_value", false)
            )
            Log.d(
                "root button",
                "root value is " + rootTrickStatus.getBoolean("root_trick_value", false)
            )
        }

        val btnInstall = findViewById<Button>(R.id.installButton)
        btnInstall.setOnClickListener {
            try {
                if (rootTrickEnabled) {
                    installAsRoot()
                } else installAsKing()
            } catch (e: Exception) {
                val tv = findViewById<TextView>(R.id.textViewError)
                tv.text = getString(R.string.error_occurred, e.toString())
            }
        }

        //RESET BUTTON TO OPEN DEFAULT PACKAGE INSTALLER TO CAN CLEAR AS DEFAULT SETTING
        val resetButton = findViewById<Button>(R.id.resetButton)
        resetButton.setOnClickListener {
            if (isGooglePackageExist) {
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.setData(("package:" + "com.google.android.packageinstaller").toUri())
                    startActivity(intent)
                } catch (e: Exception) {
                    val tv = findViewById<TextView>(R.id.textViewError)
                    tv.text = getString(R.string.error_occurred, e.toString())
                }
            } else {
                val tv = findViewById<TextView>(R.id.textViewError)
                tv.setText(R.string.missing_google_package_installer)
            }
        }

        val reinstallButton = findViewById<Button>(R.id.reinstallGoogleButton)
        reinstallButton.setOnClickListener {
            reinstallGoogleInstaller()
        }

        val checkInstallerButton = findViewById<Button>(R.id.checkInstallerButton)
        checkInstallerButton.setOnClickListener {
            startActivity(Intent(this, AppManagerActivity::class.java))
        }

        val openAndroidAutoButton = findViewById<Button>(R.id.openAndroidAutoButton)
        openAndroidAutoButton.setOnClickListener {
            openAndroidAutoSettings()
        }
    }

    private fun updateSelectedFile(path: String?) {
        selectedFilePath = path
        val fileNameText = findViewById<TextView>(R.id.selectedFileText)
        if (path != null) {
            fileNameText.text = File(path).name
            fileNameText.visibility = View.VISIBLE
        } else {
            fileNameText.visibility = View.GONE
        }
    }

    private fun openAndroidAutoSettings() {
        try {
            val intent = Intent("com.google.android.projection.gearhead.SETTINGS")
            intent.setPackage("com.google.android.projection.gearhead")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            try {
                // Alternativa se la precedente fallisce (versioni più vecchie o diverse)
                val intent = Intent()
                intent.setClassName("com.google.android.projection.gearhead", "com.google.android.projection.gearhead.companion.settings.DefaultSettingsActivity")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (e2: Exception) {
                val tv = findViewById<TextView>(R.id.textViewError)
                tv.text = getString(R.string.error_occurred, "Android Auto not found")
            }
        }
    }

    private fun reinstallGoogleInstaller() {
        try {
            val pm = packageManager
            val info = pm.getPackageInfo("com.google.android.packageinstaller", 0)
            val sourceDir = info.applicationInfo?.sourceDir
            if (sourceDir != null) {
                val systemApk = File(sourceDir)
                if (systemApk.exists()) {
                    // Copia l'APK di sistema nella sottocartella "apk" per pulizia automatica
                    val dir = File(filesDir, "apk")
                    if (!dir.exists()) dir.mkdir()
                    val tempApk = File(dir, "google_installer.apk")
                    systemApk.inputStream().use { input ->
                        tempApk.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    updateSelectedFile(tempApk.absolutePath)
                    installAsKing()
                } else {
                    val tv = findViewById<TextView>(R.id.textViewError)
                    tv.setText(R.string.error_google_installer_not_found)
                }
            } else {
                val tv = findViewById<TextView>(R.id.textViewError)
                tv.setText(R.string.error_google_installer_not_found)
            }
        } catch (e: Exception) {
            val tv = findViewById<TextView>(R.id.textViewError)
            tv.text = getString(R.string.error_occurred, e.message ?: e.toString())
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.action
        var type = intent?.type
        val data = intent?.data

        Log.d("KingInstaller", "handleIntent: action=$action, type=$type, data=$data")

        if (data != null && (Intent.ACTION_VIEW == action || Intent.ACTION_INSTALL_PACKAGE == action)) {
            // Se il type è nullo, proviamo a ricavarlo dal ContentResolver
            if (type == null) {
                type = contentResolver.getType(data)
            }
            
            // Se è ancora nullo, controlliamo l'estensione del file
            if (type == null && data.toString().lowercase().endsWith(".apk")) {
                type = "application/vnd.android.package-archive"
            }

            if ("application/vnd.android.package-archive" == type || type == null) {
                try {
                    val path: String? = if ("file" == data.scheme) {
                        data.path
                    } else {
                        copyFileToInternalStorage(data, "apk")
                    }
                    if (path != null) {
                        updateSelectedFile(path)
                    }
                } catch (e: Exception) {
                    val tv = findViewById<TextView>(R.id.textViewError)
                    tv.text = getString(R.string.error_loading_apk, e.message ?: "")
                    Log.e("KingInstaller", "Error handling intent", e)
                }
            }
        }
    }

    val isGooglePackageExist: Boolean
        //CHECK IF GOOGLE PACKAGE INSTALLER EXIST ON YOUR DEVICE
        get() {
            val pm = packageManager
            try {
                pm.getPackageInfo(
                    "com.google.android.packageinstaller",
                    PackageManager.GET_META_DATA
                )
            } catch (e: PackageManager.NameNotFoundException) {
                return false
            }
            return true
        }

    fun oppoTrick() {
        //MAKE OPPO TRICK DISABLED AS DEFAULT AND AVOID HAVE AN UNUSEFUL FAKE INSTALLER
        val oppoTrickStatus = getSharedPreferences("oppo_trick_value", MODE_PRIVATE)
        oppoTrickEnabled = oppoTrickStatus.getBoolean("oppo_trick_value", false)
        //MAKE ROOT TRICK DISABLED AS DEFAULT
        val rootTrickStatus = getSharedPreferences("root_trick_value", MODE_PRIVATE)
        rootTrickEnabled = rootTrickStatus.getBoolean("root_trick_value", false)
        val pm = applicationContext.packageManager
        if (oppoTrickEnabled) {
            val oppoTrickFlagged =
                ComponentName(packageName, "$packageName.OppoTrick")
            pm.setComponentEnabledSetting(
                oppoTrickFlagged,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } else {
            val oppoTrickFlagged =
                ComponentName(packageName, "$packageName.OppoTrick")
            pm.setComponentEnabledSetting(
                oppoTrickFlagged,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
        oppoTrickStatus.edit {
            putBoolean("oppo_trick_value", oppoTrickEnabled)
        }
        rootTrickStatus.edit {
            putBoolean("root_trick_value", rootTrickEnabled)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.user_info_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_search) {
            val url = "https://gitlab.com/annexhack/king-installer"
            val i = Intent(Intent.ACTION_VIEW)
            i.setData(url.toUri())
            startActivity(i)
        }
        if (item.itemId == R.id.action_search2) {
            val url = "https://github.com/fcaronte/KingInstaller"
            val i = Intent(Intent.ACTION_VIEW)
            i.setData(url.toUri())
            startActivity(i)
        }
        if (item.itemId == R.id.action_search3) {
            val url = "https://github.com/Rikj000/KingInstaller"
            val i = Intent(Intent.ACTION_VIEW)
            i.setData(url.toUri())
            startActivity(i)
        }
        return true
    }

    private fun installAsRoot() {
        try {
            val filepath = selectedFilePath ?: ""
            runSuWithCmd("pm install -t -i \"com.android.vending\" -r $filepath")
            updateSelectedFile(null)
            val tv = findViewById<TextView>(R.id.textViewError)
            tv.text = ""
        } catch (e: Exception) {
            val tv = findViewById<TextView>(R.id.textViewError)
            tv.text = getString(R.string.error_occurred, e.toString())
        }
    }

    private fun installAsKing() {
        try {
            val filepath = selectedFilePath ?: ""
            if (filepath.isEmpty()) {
                Toast.makeText(this, R.string.select_a_file, Toast.LENGTH_SHORT).show()
                return
            }

            if (!packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
                return
            }

            val myFile = File(filepath)
            if (!myFile.exists()) {
                Toast.makeText(this, R.string.file_error, Toast.LENGTH_SHORT).show()
                return
            }
            val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
            val fileUri: Uri? = FileProvider.getUriForFile(
                applicationContext,
                "$packageName.provider",
                myFile
            )
            intent.setData(fileUri)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            intent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, "com.android.vending")
            updateSelectedFile(null)
            val tv = findViewById<TextView>(R.id.textViewError)
            tv.text = ""
            startActivity(intent)
        } catch (e: Exception) {
            val tv = findViewById<TextView>(R.id.textViewError)
            tv.text = getString(R.string.error_occurred, e.toString())
        }
    }

    private fun showFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setType("*/*")
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        requestPermissions()
        try {
            startActivityForResult(
                Intent.createChooser(intent, getString(R.string.select_apk)), FILE_SELECT_CODE
            )
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(this, R.string.install_file_manager, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            FILE_SELECT_CODE -> if (resultCode == RESULT_OK) {
                val uri = data?.data
                val path = copyFileToInternalStorage(uri!!, "apk")

                if (path != null) {
                    updateSelectedFile(path)
                }
            }

            PERMISSION_REQUEST_CODE -> if (resultCode == RESULT_OK) {
                Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.permission_not_granted, Toast.LENGTH_SHORT).show()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    requestPermissions()
                }
            }
        }
    }

    fun clearTempFile() {
        val file = File(applicationContext.filesDir.toString() + "/apk")
        val listFiles = file.listFiles()
        if (listFiles == null || !file.isDirectory) {
            return
        }
        for (file2 in listFiles) {
            file2.delete()
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        try {
            clearTempFile()
        } catch (ignored: Throwable) {
        }
    }

    private fun checkManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivityForResult(intent, PERMISSION_REQUEST_CODE)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivityForResult(intent, PERMISSION_REQUEST_CODE)
                }
            }
        } else {
            requestPermissions()
        }
    }

    private fun copyFileToInternalStorage(uri: Uri, newDirName: String): String? {
        val mContext = applicationContext
        val returnCursor = mContext.contentResolver.query(
            uri, arrayOf(
                android.provider.OpenableColumns.DISPLAY_NAME, android.provider.OpenableColumns.SIZE
            ), null, null, null
        )

        val nameIndex = returnCursor?.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME) ?: -1
        returnCursor?.moveToFirst()
        val name = if (nameIndex != -1) returnCursor?.getString(nameIndex) else "temp.apk"
        returnCursor?.close()

        val output: File
        if (newDirName != "") {
            val dir = File(mContext.filesDir.toString() + "/" + newDirName)
            if (!dir.exists()) {
                dir.mkdir()
            }
            output = File(mContext.filesDir.toString() + "/" + newDirName + "/" + name)
        } else {
            output = File(mContext.filesDir.toString() + "/" + name)
        }
        try {
            val inputStream = mContext.contentResolver.openInputStream(uri)
            val outputStream = java.io.FileOutputStream(output)
            var read = 0
            val bufferSize = 1024
            val buffers = ByteArray(bufferSize)
            while ((inputStream!!.read(buffers).also { read = it }) != -1) {
                outputStream.write(buffers, 0, read)
            }

            inputStream.close()
            outputStream.close()
        } catch (e: Exception) {
            return null
        }

        return output.path
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
                ),
                PERMISSION_REQUEST_CODE
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    companion object {
        private const val FILE_SELECT_CODE = 1
        private const val PERMISSION_REQUEST_CODE = 2

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
        fun readStream(`is`: InputStream): String {
            val byteArrayOutputStream = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var length: Int
            while ((`is`.read(buffer).also { length = it }) != -1) {
                byteArrayOutputStream.write(buffer, 0, length)
            }
            return byteArrayOutputStream.toString("UTF-8")
        }

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
                if (File(path).exists()) return true
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
    }
}
