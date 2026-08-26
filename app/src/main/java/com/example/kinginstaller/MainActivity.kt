package com.example.kinginstaller

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import rikka.shizuku.Shizuku
import java.io.File

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
                Shizuku.addBinderReceivedListenerSticky(binderListener)
                oppoTrickEnabled = false
                rootTrickEnabled = false
                saveMethodSelection()
                syncSwitches()
                oppoTrick()
                Toast.makeText(this, "Shizuku authorized", Toast.LENGTH_SHORT).show()
            } else {
                shizukuTrickEnabled = false
                Shizuku.removeBinderReceivedListener(binderListener)
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
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val version = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (e: Exception) { "" }
        title = "${getString(R.string.app_name)} v$version"

        Shizuku.addRequestPermissionResultListener(shizukuListener)

        InstallationUtils.clearTempFiles(this)
        if (savedInstanceState == null) {
            UpdateChecker.checkForUpdates(this)
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

        findViewById<Button>(R.id.selectButton).setOnClickListener {
            try {
                showFileChooser()
            } catch (e: Exception) {
                findViewById<TextView>(R.id.textViewError).text = getString(R.string.error_occurred, e.toString())
            }
        }

        // Initialize selection states
        oppoTrickEnabled = getSharedPreferences("oppo_trick_value", MODE_PRIVATE).getBoolean("oppo_trick_value", false)
        rootTrickEnabled = getSharedPreferences("root_trick_value", MODE_PRIVATE).getBoolean("root_trick_value", false)
        shizukuTrickEnabled = getSharedPreferences("shizuku_trick_value", MODE_PRIVATE).getBoolean("shizuku_trick_value", false)

        syncSwitches()
        if (shizukuTrickEnabled) {
            Shizuku.addBinderReceivedListenerSticky(binderListener)
        }

        findViewById<MaterialSwitch>(R.id.switchOppo).setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                oppoTrickEnabled = true
                rootTrickEnabled = false
                shizukuTrickEnabled = false
                Shizuku.removeBinderReceivedListener(binderListener)
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
                if (RootUtils.isDeviceRooted) {
                    rootTrickEnabled = true
                    oppoTrickEnabled = false
                    shizukuTrickEnabled = false
                    Shizuku.removeBinderReceivedListener(binderListener)
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

        val switchShizuku = findViewById<MaterialSwitch>(R.id.switchShizuku)
        switchShizuku.setOnCheckedChangeListener { view, isChecked ->
            if (isChecked) {
                Shizuku.addBinderReceivedListenerSticky(binderListener)
                if (ShizukuUtils.isShizukuAvailable()) {
                    if (ShizukuUtils.hasShizukuPermission()) {
                        shizukuTrickEnabled = true
                        oppoTrickEnabled = false
                        rootTrickEnabled = false
                        saveMethodSelection()
                        syncSwitches()
                        oppoTrick()
                    } else {
                        view.isChecked = false
                        Shizuku.requestPermission(shizukuRequestCode)
                    }
                } else {
                    view.isChecked = false
                    Shizuku.removeBinderReceivedListener(binderListener)
                    Toast.makeText(this, R.string.shizuku_not_available, Toast.LENGTH_SHORT).show()
                }
            } else if (shizukuTrickEnabled) {
                shizukuTrickEnabled = false
                Shizuku.removeBinderReceivedListener(binderListener)
                saveMethodSelection()
                oppoTrick()
            }
        }

        switchShizuku.setOnLongClickListener {
            ShizukuUtils.showAdvancedDialog(this, selectedFilePath) { status ->
                findViewById<TextView>(R.id.textViewError).text = status
            }
            true
        }
        findViewById<View>(R.id.shizukuDesc).setOnLongClickListener {
            ShizukuUtils.showAdvancedDialog(this, selectedFilePath) { status ->
                findViewById<TextView>(R.id.textViewError).text = status
            }
            true
        }

        findViewById<Button>(R.id.installButton).setOnClickListener {
            try {
                if (shizukuTrickEnabled) {
                    ShizukuUtils.installApk(this, selectedFilePath, { status ->
                        findViewById<TextView>(R.id.textViewError).text = status
                    }, {
                        updateSelectedFile(null)
                    })
                } else if (rootTrickEnabled) {
                    installAsRoot()
                } else installAsKing()
            } catch (e: Exception) {
                findViewById<TextView>(R.id.textViewError).text = getString(R.string.error_occurred, e.toString())
            }
        }

        findViewById<Button>(R.id.resetButton).setOnClickListener {
            if (isGooglePackageExist) {
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.setData(("package:" + InstallationUtils.GOOGLE_INSTALLER_PKG).toUri())
                    startActivity(intent)
                } catch (e: Exception) {
                    findViewById<TextView>(R.id.textViewError).text = getString(R.string.error_occurred, e.toString())
                }
            } else {
                findViewById<TextView>(R.id.textViewError).setText(R.string.missing_google_package_installer)
            }
        }

        findViewById<Button>(R.id.reinstallGoogleButton).setOnClickListener {
            reinstallGoogleInstaller()
        }

        findViewById<Button>(R.id.checkInstallerButton).setOnClickListener {
            startActivity(Intent(this, AppManagerActivity::class.java))
        }

        findViewById<Button>(R.id.openAndroidAutoButton).setOnClickListener {
            openAndroidAutoSettings()
        }
    }

    private fun checkShizukuPermission() {
        if (ShizukuUtils.hasShizukuPermission()) {
            val savedValue = getSharedPreferences("shizuku_trick_value", MODE_PRIVATE).getBoolean("shizuku_trick_value", false)
            if (savedValue && !shizukuTrickEnabled) {
                shizukuTrickEnabled = true
                runOnUiThread { syncSwitches() }
            }
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
                val intent = Intent()
                intent.setClassName("com.google.android.projection.gearhead", "com.google.android.projection.gearhead.companion.settings.DefaultSettingsActivity")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (e2: Exception) {
                findViewById<TextView>(R.id.textViewError).text = getString(R.string.error_occurred, "Android Auto not found")
            }
        }
    }

    private fun reinstallGoogleInstaller() {
        try {
            val sourceDir = InstallationUtils.getGoogleInstallerSourceDir(this)
            if (sourceDir != null) {
                val dir = File(filesDir, "apk")
                if (!dir.exists()) dir.mkdir()
                val tempApk = File(dir, "google_installer.apk")
                File(sourceDir).inputStream().use { input -> tempApk.outputStream().use { output -> input.copyTo(output) } }
                updateSelectedFile(tempApk.absolutePath)
                installAsKing()
            } else {
                findViewById<TextView>(R.id.textViewError).setText(R.string.error_google_installer_not_found)
            }
        } catch (e: Exception) {
            findViewById<TextView>(R.id.textViewError).text = getString(R.string.error_occurred, e.message ?: e.toString())
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        val action = intent.action
        var type = intent.type

        if (action == Intent.ACTION_VIEW || action == Intent.ACTION_INSTALL_PACKAGE) {
            if (data.toString().contains("king_install")) return
            
            if (type == null) type = contentResolver.getType(data)
            if (type == null && data.toString().lowercase().endsWith(".apk")) type = "application/vnd.android.package-archive"

            if (type == "application/vnd.android.package-archive" || type == "application/octet-stream" || type == null) {
                try {
                    val path = if (data.scheme == "file") data.path else InstallationUtils.copyFileToInternalStorage(this, data, "apk")
                    if (path != null) updateSelectedFile(path)
                } catch (e: Exception) {
                    findViewById<TextView>(R.id.textViewError).text = getString(R.string.error_loading_apk, e.message ?: "")
                }
            }
        }
    }

    val isGooglePackageExist: Boolean
        get() = try { packageManager.getPackageInfo(InstallationUtils.GOOGLE_INSTALLER_PKG, 0); true } catch (e: Exception) { false }

    fun oppoTrick() {
        val pm = applicationContext.packageManager
        val oppoTrickFlagged = ComponentName(packageName, "$packageName.OppoTrick")
        try {
            pm.setComponentEnabledSetting(oppoTrickFlagged, 
                if (oppoTrickEnabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP)
        } catch (e: Exception) { Log.e("KingInstaller", "Error setting component state", e) }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.user_info_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val url = when (item.itemId) {
            R.id.action_search -> "https://gitlab.com/annexhack/king-installer"
            R.id.action_search2 -> "https://github.com/fcaronte/KingInstaller"
            R.id.action_search3 -> "https://github.com/Rikj000/KingInstaller"
            R.id.action_site -> "https://inceptive.ru"
            R.id.action_check_update -> { UpdateChecker.checkForUpdates(this, manual = true); return true }
            R.id.action_about -> { showAboutDialog(); return true }
            else -> return super.onOptionsItemSelected(item)
        }
        startActivity(Intent(Intent.ACTION_VIEW).apply { data = url.toUri() })
        return true
    }

    private fun showAboutDialog() {
        val version = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (e: Exception) { "N/A" }
        val message = getString(R.string.app_version, version) + "\n\n" + 
                     getString(R.string.jen94) + "\n\n" + 
                     getString(R.string.testing_info)
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.action_about)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()

        val messageView = dialog.findViewById<TextView>(android.R.id.message)
        messageView?.let {
            android.text.util.Linkify.addLinks(it, android.text.util.Linkify.WEB_URLS)
            it.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        }
    }

    private fun installAsRoot() {
        try {
            val filepath = selectedFilePath ?: return
            RootUtils.runSuWithCmd("pm install -t -i \"${InstallationUtils.VENDING_PKG}\" -r $filepath")
            updateSelectedFile(null)
            findViewById<TextView>(R.id.textViewError).text = ""
        } catch (e: Exception) {
            findViewById<TextView>(R.id.textViewError).text = getString(R.string.error_occurred, e.toString())
        }
    }

    private fun installAsKing() {
        try {
            val filepath = selectedFilePath ?: return Toast.makeText(this, R.string.select_a_file, Toast.LENGTH_SHORT).show()
            if (!packageManager.canRequestPackageInstalls()) {
                startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply { data = Uri.parse("package:$packageName") })
                return
            }
            val myFile = File(filepath)
            if (!myFile.exists()) return Toast.makeText(this, R.string.file_error, Toast.LENGTH_SHORT).show()
            
            startActivity(InstallationUtils.createInstallIntent(this, myFile))
            updateSelectedFile(null)
            findViewById<TextView>(R.id.textViewError).text = ""
        } catch (e: Exception) {
            findViewById<TextView>(R.id.textViewError).text = getString(R.string.error_occurred, e.toString())
        }
    }

    private fun showFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*"; addCategory(Intent.CATEGORY_OPENABLE) }
        requestPermissions()
        try {
            startActivityForResult(Intent.createChooser(intent, getString(R.string.select_apk)), 1)
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(this, R.string.install_file_manager, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            val path = InstallationUtils.copyFileToInternalStorage(this, data?.data ?: return, "apk")
            if (path != null) updateSelectedFile(path)
        } else if (requestCode == 2 && resultCode == RESULT_OK) {
            Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show()
        } else if (requestCode == 2) {
            Toast.makeText(this, R.string.permission_not_granted, Toast.LENGTH_SHORT).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) requestPermissions()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuListener)
        Shizuku.removeBinderReceivedListener(binderListener)
        InstallationUtils.clearTempFiles(this)
    }

    private fun checkManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= 30) { // Build.VERSION_CODES.R
            if (!android.os.Environment.isExternalStorageManager()) {
                try {
                    @Suppress("InlinedApi")
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply { 
                        data = Uri.parse("package:$packageName") 
                    }
                    startActivityForResult(intent, 2)
                } catch (e: Exception) {
                    @Suppress("InlinedApi")
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivityForResult(intent, 2)
                }
            }
        } else {
            requestPermissions()
        }
    }

    private fun requestPermissions() {
        val perms = if (Build.VERSION.SDK_INT >= 30) {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.MANAGE_EXTERNAL_STORAGE)
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        ActivityCompat.requestPermissions(this, perms, 2)
    }
}
