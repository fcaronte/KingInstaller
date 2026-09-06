package com.example.kinginstaller

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

    private val processedPackages = mutableMapOf<String, Long>()

    private val packageAddedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
                val packageName = intent.data?.schemeSpecificPart ?: return
                val lastProcessed = processedPackages[packageName] ?: 0L
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastProcessed < 5000) {
                    return
                }
                processedPackages[packageName] = currentTime

                Log.d("KingInstaller", "Package added: $packageName")

                val tvError = findViewById<TextView>(R.id.textViewError)
                val installButton = findViewById<Button>(R.id.installButton)

                // 1. Feedback Visivo di Attesa Immediato (Anti-Freeze UI)
                runOnUiThread {
                    tvError.text = "Verifica e ottimizzazione compatibilità in corso..."
                    tvError.setTextColor(Color.parseColor("#FFA000")) // Arancione/giallo
                    installButton.isEnabled = false
                }
                
                // Fix post-installazione per Root
                if (rootTrickEnabled || RootUtils.isDeviceRooted) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        RootUtils.setInstallerViaRoot(packageName)
                    }, 200)
                }

                // 3. Reactive Polling (Zero Timer Fissi): ogni 150 ms per massimo 3 secondi
                val handler = Handler(Looper.getMainLooper())
                val startTime = System.currentTimeMillis()
                val timeoutMillis = 3000L
                val intervalMillis = 150L

                val pollingRunnable = object : Runnable {
                    override fun run() {
                        val isCompatible = InstallationUtils.isAACompatible(context, packageName)
                        val elapsed = System.currentTimeMillis() - startTime

                        if (isCompatible) {
                            runOnUiThread {
                                tvError.text = getString(R.string.install_success_aa)
                                tvError.setTextColor(getColor(R.color.aa_green_text))
                                installButton.isEnabled = true
                                updateComponentStates(installing = false)
                            }
                        } else if (elapsed >= timeoutMillis) {
                            runOnUiThread {
                                tvError.text = getString(R.string.install_success_no_aa)
                                tvError.setTextColor(getColor(R.color.aa_red_text))
                                installButton.isEnabled = true
                                updateComponentStates(installing = false)
                            }
                        } else {
                            handler.postDelayed(this, intervalMillis)
                        }
                    }
                }
                handler.post(pollingRunnable)
            }
        }
    }

    private val shizukuListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == shizukuRequestCode) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                shizukuTrickEnabled = true
                Shizuku.addBinderReceivedListenerSticky(binderListener)
                oppoTrickEnabled = false
                rootTrickEnabled = false
                saveMethodSelection()
                syncSwitches()
                updateComponentStates()
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

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        // Eseguiamo i controlli solo se l'utente stava effettivamente usando Shizuku
        if (shizukuTrickEnabled) {
            runOnUiThread {
                shizukuTrickEnabled = false
                saveMethodSelection()
                syncSwitches()
                updateComponentStates()
                findViewById<TextView>(R.id.textViewError).text = getString(R.string.shizuku_not_available)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val version = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (e: Exception) { "" }
        title = "${getString(R.string.app_name)} v$version"

        Shizuku.addRequestPermissionResultListener(shizukuListener)
        Shizuku.addBinderDeadListener(binderDeadListener)

        val filter = IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply {
            addDataScheme("package")
        }
        registerReceiver(packageAddedReceiver, filter)

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

        if (shizukuTrickEnabled && !ShizukuUtils.isShizukuAvailable()) {
            shizukuTrickEnabled = false
            saveMethodSelection()
        }

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
                updateComponentStates()
            } else if (oppoTrickEnabled) {
                oppoTrickEnabled = false
                saveMethodSelection()
                updateComponentStates()
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
                    updateComponentStates()
                } else {
                    view.isChecked = false
                    Toast.makeText(this, R.string.device_not_rooted, Toast.LENGTH_SHORT).show()
                }
            } else if (rootTrickEnabled) {
                rootTrickEnabled = false
                saveMethodSelection()
                updateComponentStates()
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
                        updateComponentStates()
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
                updateComponentStates()
            }
        }

        findViewById<Button>(R.id.installButton).setOnClickListener {
            try {
                startInstallation()
            } catch (e: Exception) {
                findViewById<TextView>(R.id.textViewError).text = getString(R.string.error_occurred, e.toString())
                updateComponentStates(installing = false)
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

        findViewById<Button>(R.id.donateButton).setOnClickListener {
            val url = "https://www.paypal.com/paypalme/FCaronte/2"
            try {
                startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            } catch (e: Exception) {
                Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Imposta lo stato del focus in modo pulito
        ShizukuUtils.InstallationState.isFocusLost = !hasFocus
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

    override fun onResume() {
        super.onResume()
        updateComponentStates(installing = false)
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
                startInstallation()
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

    fun updateComponentStates(installing: Boolean = false) {
        val pm = applicationContext.packageManager
        val apkHandler = ComponentName(packageName, "$packageName.ApkHandler")
        val oppoTrickFlagged = ComponentName(packageName, "$packageName.OppoTrick")
        try {
            val apkState = if (installing) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            val oppoState = if (installing && oppoTrickEnabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED 
                            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            
            pm.setComponentEnabledSetting(apkHandler, apkState, PackageManager.DONT_KILL_APP)
            pm.setComponentEnabledSetting(oppoTrickFlagged, oppoState, PackageManager.DONT_KILL_APP)
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
        RootUtils.installApk(this, selectedFilePath, { status ->
            findViewById<TextView>(R.id.textViewError).text = status
        }, {
            // Rimosso updateSelectedFile(null) per mantenere l'APK selezionato
        })
    }

    private fun startInstallation() {
        val filepath = selectedFilePath 
        if (filepath == null) {
            Toast.makeText(this, R.string.select_a_file, Toast.LENGTH_SHORT).show()
            return
        }

        val myFile = File(filepath)
        if (!myFile.exists()) {
            Toast.makeText(this, R.string.file_error, Toast.LENGTH_SHORT).show()
            return
        }

        updateComponentStates(installing = true)
        findViewById<TextView>(R.id.textViewError).text = ""

        if (shizukuTrickEnabled) {
            // FLUSSO SHIZUKU PURO (Nessun fallback al classico se fallisce)
            Log.d("MainActivity", "Avvio installazione tramite Metodo Shizuku (scelta utente).")
            
            if (!ShizukuUtils.isShizukuAvailable() || !ShizukuUtils.hasShizukuPermission()) {
                findViewById<TextView>(R.id.textViewError).text = "Errore: Shizuku non attivo o permessi negati."
                updateComponentStates(installing = false)
                return
            }

            ShizukuUtils.installApk(this, filepath, { status ->
                findViewById<TextView>(R.id.textViewError).text = status
            }, {
                // Successo Shizuku
            })
            
        } else if (rootTrickEnabled) {
            // FLUSSO ROOT PURO
            Log.d("MainActivity", "Avvio installazione tramite Metodo Root (scelta utente).")
            installAsRoot()
        } else {
            // FLUSSO CLASSICO / NATIVO PURO (Nessun tentativo con Shizuku)
            Log.d("MainActivity", "Avvio installazione tramite Metodo Classico (scelta utente).")
            
            if (!packageManager.canRequestPackageInstalls()) {
                startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply { 
                    data = "package:$packageName".toUri() 
                })
                updateComponentStates(installing = false)
                return
            }

            try {
                val intent = InstallationUtils.createInstallIntent(this, myFile)
                startActivityForResult(intent, 100)
            } catch (e: Exception) {
                findViewById<TextView>(R.id.textViewError).text = getString(R.string.error_occurred, e.toString())
                updateComponentStates(installing = false)
            }
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
        } else if (requestCode == 100) {
            updateComponentStates(installing = false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(packageAddedReceiver)
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
                        data = "package:$packageName".toUri()
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
