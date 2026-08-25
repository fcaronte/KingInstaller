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
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import rikka.shizuku.Shizuku
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
    var shizukuTrickEnabled: Boolean = false
    var forceRootEnabled: Boolean = false
    private var selectedFilePath: String? = null

    private val shizukuRequestCode = 1001

    private val shizukuListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == shizukuRequestCode) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                shizukuTrickEnabled = true
                oppoTrickEnabled = false
                rootTrickEnabled = false
                saveMethodSelection()
                syncSwitches()
                oppoTrick()
                Toast.makeText(this, "Shizuku authorized", Toast.LENGTH_SHORT).show()
            } else {
                shizukuTrickEnabled = false
                saveMethodSelection()
                syncSwitches()
                Toast.makeText(this, R.string.permission_not_granted, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val binderListener = Shizuku.OnBinderReceivedListener {
        checkShizukuPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        DynamicColors.applyToActivitiesIfAvailable(this.application)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Shizuku.addRequestPermissionResultListener(shizukuListener)
        Shizuku.addBinderReceivedListenerSticky(binderListener)

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

        // Initialize selection states
        oppoTrickEnabled = getSharedPreferences("oppo_trick_value", MODE_PRIVATE).getBoolean("oppo_trick_value", false)
        rootTrickEnabled = getSharedPreferences("root_trick_value", MODE_PRIVATE).getBoolean("root_trick_value", false)
        shizukuTrickEnabled = getSharedPreferences("shizuku_trick_value", MODE_PRIVATE).getBoolean("shizuku_trick_value", false)

        val switchShizuku = findViewById<MaterialSwitch>(R.id.switchShizuku)
        syncSwitches()

        findViewById<MaterialSwitch>(R.id.switchOppo).setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                oppoTrickEnabled = true
                rootTrickEnabled = false
                shizukuTrickEnabled = false
                syncSwitches()
                saveMethodSelection()
                oppoTrick()
            } else if (oppoTrickEnabled) {
                oppoTrickEnabled = false
                saveMethodSelection()
                oppoTrick()
            }
        }

        findViewById<MaterialSwitch>(R.id.switchRoot).setOnCheckedChangeListener { view, isChecked ->
            if (isChecked) {
                if (isDeviceRooted) {
                    rootTrickEnabled = true
                    oppoTrickEnabled = false
                    shizukuTrickEnabled = false
                    if (isGooglePackageExist && !forceRootEnabled) {
                        findViewById<TextView>(R.id.textViewError).setText(R.string.root_method_warning)
                        forceRootEnabled = true
                    }
                    syncSwitches()
                    saveMethodSelection()
                    oppoTrick()
                } else {
                    view.isChecked = false
                    Toast.makeText(this, R.string.device_not_rooted, Toast.LENGTH_SHORT).show()
                }
            } else if (rootTrickEnabled) {
                rootTrickEnabled = false
                saveMethodSelection()
                oppoTrick()
            }
        }

        switchShizuku.setOnCheckedChangeListener { view, isChecked ->
            if (isChecked) {
                if (isShizukuAvailable()) {
                    if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                        shizukuTrickEnabled = true
                        oppoTrickEnabled = false
                        rootTrickEnabled = false
                        syncSwitches()
                        saveMethodSelection()
                        oppoTrick()
                    } else {
                        view.isChecked = false
                        Shizuku.requestPermission(shizukuRequestCode)
                    }
                } else {
                    view.isChecked = false
                    Toast.makeText(this, R.string.shizuku_not_available, Toast.LENGTH_SHORT).show()
                }
            } else if (shizukuTrickEnabled) {
                shizukuTrickEnabled = false
                saveMethodSelection()
                oppoTrick()
            }
        }

        switchShizuku.setOnLongClickListener {
            showAdvancedShizukuDialog()
            true
        }
        findViewById<View>(R.id.shizukuDesc).setOnLongClickListener {
            showAdvancedShizukuDialog()
            true
        }

        val btnInstall = findViewById<Button>(R.id.installButton)
        btnInstall.setOnClickListener {
            try {
                if (shizukuTrickEnabled) {
                    installWithShizuku()
                } else if (rootTrickEnabled) {
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

    private fun showAdvancedShizukuDialog() {
        if (!isShizukuAvailable() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.shizuku_not_available, Toast.LENGTH_SHORT).show()
            return
        }

        val input = TextInputEditText(this)
        input.setText(getString(R.string.advanced_shizuku_default))
        input.hint = getString(R.string.advanced_shizuku_hint)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.advanced_shizuku_title)
            .setView(input)
            .setPositiveButton(R.string.execute) { _, _ ->
                val command = input.text.toString()
                executeAdvancedShizukuCommand(command)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun executeAdvancedShizukuCommand(command: String) {
        val filepath = selectedFilePath
        if (filepath == null) {
            Toast.makeText(this, R.string.select_a_file, Toast.LENGTH_SHORT).show()
            return
        }

        val tvError = findViewById<TextView>(R.id.textViewError)
        tvError.text = "Executing advanced command..."

        kotlin.concurrent.thread {
            try {
                // Prepariamo l'APK in /data/local/tmp come zona sicura e leggibile per shizuku
                val remotePath = "/data/local/tmp/king_advanced_temp.apk"
                val catProcess = Shizuku.newProcess(arrayOf("sh", "-c", "cat > $remotePath"), null, null)
                File(filepath).inputStream().use { input -> input.copyTo(catProcess.outputStream) }
                catProcess.outputStream.close()
                catProcess.waitFor()

                // Sostituiamo $APK nel comando con il percorso remoto
                val finalCmd = command.replace("\$APK", remotePath)
                val process = Shizuku.newProcess(arrayOf("sh", "-c", finalCmd), null, null)
                
                val output = StringBuilder()
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) output.append(line).append("\n")
                while (errorReader.readLine().also { line = it } != null) output.append(line).append("\n")
                
                val exitCode = process.waitFor()
                Shizuku.newProcess(arrayOf("sh", "-c", "rm $remotePath"), null, null).waitFor()

                runOnUiThread {
                    tvError.text = "Exit Code: $exitCode\nOutput:\n$output"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvError.text = getString(R.string.error_occurred, e.toString())
                }
            }
        }
    }

    private fun checkShizukuPermission() {
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            shizukuTrickEnabled = getSharedPreferences("shizuku_trick_value", MODE_PRIVATE).getBoolean("shizuku_trick_value", false)
            runOnUiThread { syncSwitches() }
        }
    }

    private fun syncSwitches() {
        findViewById<MaterialSwitch>(R.id.switchOppo).isChecked = oppoTrickEnabled
        findViewById<MaterialSwitch>(R.id.switchRoot).isChecked = rootTrickEnabled
        findViewById<MaterialSwitch>(R.id.switchShizuku).isChecked = shizukuTrickEnabled
    }

    private fun saveMethodSelection() {
        getSharedPreferences("oppo_trick_value", MODE_PRIVATE).edit { putBoolean("oppo_trick_value", oppoTrickEnabled) }
        getSharedPreferences("root_trick_value", MODE_PRIVATE).edit { putBoolean("root_trick_value", rootTrickEnabled) }
        getSharedPreferences("shizuku_trick_value", MODE_PRIVATE).edit { putBoolean("shizuku_trick_value", shizukuTrickEnabled) }
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
            // Ignoriamo l'intent se proviene dal nostro file temporaneo Shizuku per evitare loop
            if (data.toString().contains("king_installer_shizuku.apk")) {
                Log.d("KingInstaller", "Ignoring intent from our own Shizuku temp file")
                return
            }

            // Se il type è nullo, proviamo a ricavarlo dal ContentResolver
            if (type == null) {
                type = contentResolver.getType(data)
            }
            
            // Se è ancora nullo, controlliamo l'estensione del file
            if (type == null && data.toString().lowercase().endsWith(".apk")) {
                type = "application/vnd.android.package-archive"
            }

            if ("application/vnd.android.package-archive" == type || "application/octet-stream" == type || type == null) {
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
        val pm = applicationContext.packageManager
        val oppoTrickFlagged = ComponentName(packageName, "$packageName.OppoTrick")
        
        try {
            if (oppoTrickEnabled) {
                pm.setComponentEnabledSetting(
                    oppoTrickFlagged,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            } else {
                pm.setComponentEnabledSetting(
                    oppoTrickFlagged,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        } catch (e: Exception) {
            Log.e("KingInstaller", "Error setting component state", e)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.user_info_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == R.id.action_search) {
            val url = "https://gitlab.com/annexhack/king-installer"
            val i = Intent(Intent.ACTION_VIEW)
            i.setData(url.toUri())
            startActivity(i)
        }
        if (item.getItemId() == R.id.action_search2) {
            val url = "https://github.com/fcaronte/KingInstaller"
            val i = Intent(Intent.ACTION_VIEW)
            i.setData(url.toUri())
            startActivity(i)
        }
        if (item.getItemId() == R.id.action_search3) {
            val url = "https://github.com/Rikj000/KingInstaller"
            val i = Intent(Intent.ACTION_VIEW)
            i.setData(url.toUri())
            startActivity(i)
        }
        if (item.getItemId() == R.id.action_site) {
            val url = "https://inceptive.ru"
            val i = Intent(Intent.ACTION_VIEW)
            i.setData(url.toUri())
            startActivity(i)
        }
        if (item.getItemId() == R.id.action_about) {
            showAboutDialog()
        }
        return true
    }

    private fun showAboutDialog() {
        val version = try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName
        } catch (e: Exception) {
            "N/A"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.action_about)
            .setMessage(getString(R.string.app_version, version) + "\n\n" + getString(R.string.jen94))
            .setPositiveButton(android.R.string.ok, null)
            .show()
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
        Shizuku.removeRequestPermissionResultListener(shizukuListener)
        Shizuku.removeBinderReceivedListener(binderListener)
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

    private fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    private fun installWithShizuku() {
        val filepath = selectedFilePath
        if (filepath == null) {
            Toast.makeText(this, R.string.select_a_file, Toast.LENGTH_SHORT).show()
            return
        }

        val apkFile = File(filepath)
        if (!apkFile.exists()) {
            Toast.makeText(this, R.string.file_error, Toast.LENGTH_SHORT).show()
            return
        }

        val tvError = findViewById<TextView>(R.id.textViewError)
        tvError.text = "Transferring APK via Shizuku..."

        kotlin.concurrent.thread {
            try {
                val remotePath = "/data/local/tmp/king_install_temp.apk"
                
                // 1. Trasferiamo il file in /data/local/tmp usando cat (bypassiamo ogni problema di permessi file)
                val catProcess = Shizuku.newProcess(arrayOf("sh", "-c", "cat > $remotePath"), null, null)
                val os = catProcess.outputStream
                apkFile.inputStream().use { input ->
                    input.copyTo(os)
                }
                os.flush()
                os.close()
                val catExit = catProcess.waitFor()
                
                if (catExit != 0) {
                    runOnUiThread { tvError.text = "Failed to transfer APK (Exit $catExit)" }
                    return@thread
                }
                
                runOnUiThread { tvError.text = "File transferred. Installing..." }

                // 2. Lanciamo l'installazione dal percorso sicuro forzando l'installer Play Store
                val installCmd = "pm install -r -t -i com.android.vending $remotePath"
                val installProcess = Shizuku.newProcess(arrayOf("sh", "-c", installCmd), null, null)
                
                val output = StringBuilder()
                val reader = BufferedReader(InputStreamReader(installProcess.inputStream))
                val errorReader = BufferedReader(InputStreamReader(installProcess.errorStream))
                
                var line: String?
                while (reader.readLine().also { line = it } != null) output.append(line).append("\n")
                while (errorReader.readLine().also { line = it } != null) output.append(line).append("\n")
                
                val exitCode = installProcess.waitFor()
                
                // 3. Pulizia del file temporaneo in /data/local/tmp
                Shizuku.newProcess(arrayOf("sh", "-c", "rm $remotePath"), null, null).waitFor()

                runOnUiThread {
                    if (exitCode == 0 && output.contains("Success")) {
                        updateSelectedFile(null)
                        tvError.text = "Installation Successful!\n(Installer: com.android.vending)"
                        Toast.makeText(this, "App installed successfully", Toast.LENGTH_LONG).show()
                    } else {
                        tvError.text = "Install Failed (Exit $exitCode):\n$output"
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvError.text = getString(R.string.error_occurred, e.toString())
                }
            }
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
