package com.example.kinginstaller

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
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
import android.widget.ImageButton
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
    private var lastInstalledPackage: String? = null

    // Output grezzo dell'ultimo tentativo di fix via Root (per diagnosi nel messaggio finale).
    @Volatile
    private var lastRootFixDetail: String? = null

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
                lastInstalledPackage = packageName
                lastRootFixDetail = null

                val tvError = findViewById<TextView>(R.id.textViewError)
                val installButton = findViewById<Button>(R.id.installButton)

                // 1. Feedback Visivo di Attesa Immediato (Anti-Freeze UI)
                runOnUiThread {
                    tvError.text = "Verifica e ottimizzazione compatibilità in corso..."
                    tvError.setTextColor(Color.parseColor("#FFA000")) // Arancione/giallo
                    installButton.isEnabled = false
                }
                
                // 3. Polling reattivo CON FEEDBACK VISIBILE: ogni 150 ms verifichiamo la
                //    compatibilità AA. Se dopo un breve margine i metadati NON risultano
                //    corretti (tipico di Xiaomi/MIUI/HyperOS che ignora gli extra dell'intent,
                //    ma possibile anche su One UI), forziamo UNA volta l'installer a Play Store
                //    con privilegi Root o Shizuku mostrando lo stato, poi controlliamo l'esito.
                //    In caso di fallimento mostriamo i valori reali di install-source per diagnosi.
                val handler = Handler(Looper.getMainLooper())
                val startTime = System.currentTimeMillis()
                val timeoutMillis = 6000L
                val intervalMillis = 150L
                val fixGraceMillis = 500L

                val pollingRunnable = object : Runnable {
                    var fixAttempted = false

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
                        } else if (!fixAttempted && elapsed >= fixGraceMillis) {
                            fixAttempted = true
                            when {
                                rootTrickEnabled -> {
                                    runOnUiThread {
                                        tvError.text = getString(R.string.fixing_installer_root)
                                        tvError.setTextColor(Color.parseColor("#FFA000"))
                                    }
                                    RootUtils.setInstallerViaRoot(packageName) { detail -> lastRootFixDetail = detail }
                                    handler.postDelayed(this, intervalMillis)
                                }
                                shizukuTrickEnabled -> {
                                    runOnUiThread {
                                        tvError.text = getString(R.string.fixing_installer_shizuku)
                                        tvError.setTextColor(Color.parseColor("#FFA000"))
                                    }
                                    ShizukuUtils.setInstallerViaShizuku(applicationContext, packageName) { ok ->
                                        if (!ok) Log.w("KingInstaller", "setInstallerViaShizuku fallito per $packageName")
                                    }
                                    handler.postDelayed(this, intervalMillis)
                                }
                                else -> {
                                    handler.postDelayed(this, intervalMillis)
                                }
                            }
                        } else if (elapsed >= timeoutMillis) {
                            val summary = InstallationUtils.getInstallSourceSummary(context, packageName)
                            val rootDetail = lastRootFixDetail
                            Log.w("KingInstaller", "AA non compatibile per $packageName -> $summary" +
                                    (if (!rootDetail.isNullOrEmpty()) " | [root] $rootDetail" else ""))
                            runOnUiThread {
                                installButton.isEnabled = true
                                updateComponentStates(installing = false)
                                tvError.setTextColor(getColor(R.color.aa_red_text))
                                tvError.text = getString(R.string.install_success_no_aa) + "\n" + summary
                                if (shizukuTrickEnabled) {
                                    promptNotCompatibleAfterShizuku(packageName)
                                } else if (!rootTrickEnabled && ShizukuUtils.isShizukuAvailable() && ShizukuUtils.hasShizukuPermission()) {
                                    promptNotCompatibleAfterClassic(packageName)
                                }
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
                Toast.makeText(this, R.string.shizuku_authorized, Toast.LENGTH_SHORT).show()
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
        updateComponentStates(installing = false)
        val tvStatus = findViewById<TextView>(R.id.textViewError)
        // Il suggerimento basato sul package installer rilevato ("hai il Google installer
        // -> usa il metodo no-root") ha senso solo fino ad Android 14 (API 34). Su Android 15+
        // quell'euristica non è più attendibile, quindi non mostriamo alcun suggerimento iniziale.
        if (Build.VERSION.SDK_INT <= 34) {
            if (isGooglePackageExist) {
                tvStatus.setText(R.string.google_package_installer_is_installed)
            } else {
                tvStatus.setText(R.string.missing_google_package_installer)
            }
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

        // La vecchia modalità Oppo/Realme/OnePlus non è più utile su Android 15+ (API 35):
        // nascondiamo lo switch e ci assicuriamo che non resti attivo da sessioni precedenti.
        if (Build.VERSION.SDK_INT >= 35) {
            findViewById<MaterialSwitch>(R.id.switchOppo).visibility = View.GONE
            if (oppoTrickEnabled) {
                oppoTrickEnabled = false
                saveMethodSelection()
            }
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
                    findViewById<TextView>(R.id.textViewError).text = ""
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

        findViewById<ImageButton>(R.id.donatePayPalButton).setOnClickListener {
            val url = "https://www.paypal.com/paypalme/FCaronte/4"
            try {
                startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            } catch (e: Exception) {
                Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<ImageButton>(R.id.donateCoffeeButton).setOnClickListener {
            val url = "http://buymeacoffee.com/fcaronte"
            try {
                startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            } catch (e: Exception) {
                Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show()
            }
        }

        // Icona di condivisione accanto al titolo: genera e condivide il report diagnostico,
        // utile per farsi mandare i log dagli utenti (Xiaomi, One UI, ecc.).
        findViewById<ImageButton>(R.id.shareLogButton).setOnClickListener {
            Toast.makeText(this, R.string.share_log_preparing, Toast.LENGTH_SHORT).show()
            DiagnosticsUtils.shareDiagnostics(this, lastInstalledPackage)
        }

        // Tocco sulla finestra del log: copia il testo negli appunti (comodo per incollare la
        // diagnostica [root]/[installer=]). Su Android 13+ il sistema mostra già il proprio avviso.
        findViewById<TextView>(R.id.textViewError).setOnClickListener { v ->
            val text = (v as TextView).text?.toString()?.trim().orEmpty()
            if (text.isEmpty()) return@setOnClickListener
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("KingInstaller log", text))
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(this, R.string.log_copied, Toast.LENGTH_SHORT).show()
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
        if (intent == null) return

        if (intent.action == "com.example.kinginstaller.INSTALL_COMPLETE") {
            val status = intent.getIntExtra(android.content.pm.PackageInstaller.EXTRA_STATUS, android.content.pm.PackageInstaller.STATUS_FAILURE)
            Log.d("MainActivity", "Received INSTALL_COMPLETE session result: status=$status")
            when (status) {
                android.content.pm.PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                    }
                    if (confirmIntent != null) {
                        try {
                            confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(confirmIntent)
                            Log.d("MainActivity", "Launched installation user confirmation dialog from MainActivity")
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Failed to start confirmation activity", e)
                        }
                    }
                }
                android.content.pm.PackageInstaller.STATUS_SUCCESS -> {
                    Log.d("MainActivity", "Split package installation succeeded via session")
                    val tvError = findViewById<TextView>(R.id.textViewError)
                    tvError.text = getString(R.string.install_success_aa)
                    tvError.setTextColor(getColor(R.color.aa_green_text))
                    findViewById<Button>(R.id.installButton).isEnabled = true
                    updateComponentStates(installing = false)
                }
                else -> {
                    val message = intent.getStringExtra(android.content.pm.PackageInstaller.EXTRA_STATUS_MESSAGE)
                    Log.e("MainActivity", "Split package installation failed: status=$status, message=$message")
                    val tvError = findViewById<TextView>(R.id.textViewError)
                    tvError.setTextColor(getColor(R.color.aa_red_text))
                    tvError.text = getString(R.string.error_occurred, message ?: "Status $status")
                    findViewById<Button>(R.id.installButton).isEnabled = true
                    updateComponentStates(installing = false)
                }
            }
            return
        }

        val extraPath = intent.getStringExtra("selected_apk_path")
        if (extraPath != null) {
            updateSelectedFile(extraPath)
            findViewById<TextView>(R.id.textViewError).text = ""
            return
        }
        val action = intent.action

        if (action == Intent.ACTION_VIEW || action == Intent.ACTION_INSTALL_PACKAGE) {
            val data = intent.data ?: return
            if (data.toString().contains("king_install")) return

            val tvError = findViewById<TextView>(R.id.textViewError)
            tvError.text = getString(R.string.processing_apk)

            Thread {
                try {
                    var type = intent.type
                    if (type == null) type = contentResolver.getType(data)
                    val dataString = data.toString().lowercase()
                    if (type == null && (dataString.endsWith(".apk") || dataString.endsWith(".xapk") || dataString.endsWith(".apks") || dataString.endsWith(".apkm"))) {
                        type = "application/vnd.android.package-archive"
                    }

                    if (type == "application/vnd.android.package-archive" || type == "application/octet-stream" || type == "application/zip" || type == "application/x-xapk" || type == "application/x-apks" || type == "application/x-apkm" || type == null) {
                        val path = if (data.scheme == "file") data.path else InstallationUtils.copyFileToInternalStorage(this, data, "apk")
                        runOnUiThread {
                            if (path != null) {
                                updateSelectedFile(path)
                                tvError.text = ""
                            } else {
                                tvError.text = getString(R.string.error_loading_apk, getString(R.string.error_copying_file))
                            }
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        tvError.text = getString(R.string.error_loading_apk, e.message ?: "")
                    }
                }
            }.start()
        } else if (action == Intent.ACTION_SEND) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            } ?: return

            val tvError = findViewById<TextView>(R.id.textViewError)
            tvError.text = getString(R.string.processing_shared_apk)

            Thread {
                try {
                    val path = if (uri.scheme == "file") uri.path else InstallationUtils.copyFileToInternalStorage(this, uri, "apk")
                    runOnUiThread {
                        if (path != null) {
                            val lower = path.lowercase()
                            if (lower.endsWith(".apk") || lower.endsWith(".xapk") || lower.endsWith(".apks") || lower.endsWith(".apkm") || PackageArchiveUtils.isSplitArchive(File(path)) || InstallationUtils.getPackageNameFromApk(this, File(path)) != null) {
                                updateSelectedFile(path)
                                tvError.text = ""
                            } else {
                                tvError.text = getString(R.string.error_loading_apk, getString(R.string.error_not_valid_apk))
                            }
                        } else {
                            tvError.text = getString(R.string.error_loading_apk, getString(R.string.error_copying_file))
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        tvError.text = getString(R.string.error_loading_apk, e.message ?: "")
                    }
                }
            }.start()
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
        menu?.findItem(R.id.action_change_language)?.isVisible = isAppLanguageSettingsSupported()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val url = when (item.itemId) {
            R.id.action_search -> "https://gitlab.com/annexhack/king-installer"
            R.id.action_search2 -> "https://github.com/fcaronte/KingInstaller"
            R.id.action_search3 -> "https://github.com/Rikj000/KingInstaller"
            R.id.action_site -> "https://inceptive.ru"
            R.id.action_download_shizuku_next -> "https://github.com/rushiranpise/Shizuku-Next/releases"
            R.id.action_readme -> "https://github.com/fcaronte/KingInstaller/blob/master/README.md"
            R.id.action_change_language -> {
                openAppLanguageSettings()
                return true
            }
            R.id.action_check_update -> { UpdateChecker.checkForUpdates(this, manual = true); return true }
            R.id.action_about -> { showAboutDialog(); return true }
            else -> return super.onOptionsItemSelected(item)
        }
        startActivity(Intent(Intent.ACTION_VIEW).apply { data = url.toUri() })
        return true
    }

    private fun isAppLanguageSettingsSupported(): Boolean {
        val intent = AppLanguageSettings.createSettingsIntent(packageName)
        return AppLanguageSettings.isSupported(
            Build.VERSION.SDK_INT,
            intent.resolveActivity(packageManager) != null
        )
    }

    private fun openAppLanguageSettings() {
        try {
            startActivity(AppLanguageSettings.createSettingsIntent(packageName))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.error_occurred, e.toString()), Toast.LENGTH_SHORT).show()
        }
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

    /**
     * Quando il metodo Shizuku non rende l'app compatibile su questo device (es. blocco "same cert"
     * dello shell), suggeriamo di provare PRIMA il metodo Classico (senza Shizuku); solo se il device
     * è rootato offriamo anche la correzione via Root (uid 0 aggira il controllo di firma).
     */
    private fun promptNotCompatibleAfterShizuku(packageName: String) {
        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.not_aa_title)
            .setMessage(R.string.not_aa_message)
            .setPositiveButton(R.string.try_classic) { _, _ -> retryWithClassic() }
            .setNegativeButton(android.R.string.cancel, null)
        if (RootUtils.isDeviceRooted) {
            builder.setNeutralButton(R.string.fix_with_root_confirm) { _, _ -> fixViaRootAndRecheck(packageName) }
        }
        builder.show()
    }

    /** Ripiega sul metodo Classico (senza Shizuku/Root) e riavvia l'installazione sullo stesso APK. */
    private fun retryWithClassic() {
        shizukuTrickEnabled = false
        rootTrickEnabled = false
        oppoTrickEnabled = false
        Shizuku.removeBinderReceivedListener(binderListener)
        saveMethodSelection()
        syncSwitches()
        startInstallation()
    }

    private fun promptNotCompatibleAfterClassic(packageName: String) {
        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.not_aa_title)
            .setMessage(R.string.not_aa_classic_message)
        if (ShizukuUtils.isShizukuAvailable() && ShizukuUtils.hasShizukuPermission()) {
            builder.setPositiveButton(R.string.shizuku_trick) { _, _ -> retryWithShizuku() }
        }
        if (RootUtils.isDeviceRooted) {
            builder.setNeutralButton(R.string.fix_with_root_confirm) { _, _ -> fixViaRootAndRecheck(packageName) }
        }
        builder.setNegativeButton(android.R.string.cancel, null)
        builder.show()
    }

    private fun retryWithShizuku() {
        shizukuTrickEnabled = true
        rootTrickEnabled = false
        oppoTrickEnabled = false
        Shizuku.addBinderReceivedListenerSticky(binderListener)
        saveMethodSelection()
        syncSwitches()
        Handler(Looper.getMainLooper()).postDelayed({
            startInstallation()
        }, 300L)
    }

    private fun fixViaRootAndRecheck(packageName: String) {
        val tv = findViewById<TextView>(R.id.textViewError)
        tv.text = getString(R.string.fixing_installer_root)
        tv.setTextColor(Color.parseColor("#FFA000"))
        RootUtils.setInstallerViaRoot(packageName)

        val handler = Handler(Looper.getMainLooper())
        val start = System.currentTimeMillis()
        handler.postDelayed(object : Runnable {
            override fun run() {
                when {
                    InstallationUtils.isAACompatible(this@MainActivity, packageName) -> {
                        tv.text = getString(R.string.install_success_aa)
                        tv.setTextColor(getColor(R.color.aa_green_text))
                    }
                    System.currentTimeMillis() - start >= 3000 -> {
                        tv.text = getString(R.string.install_success_no_aa) + "\n" +
                            InstallationUtils.getInstallSourceSummary(this@MainActivity, packageName)
                        tv.setTextColor(getColor(R.color.aa_red_text))
                    }
                    else -> handler.postDelayed(this, 150)
                }
            }
        }, 400)
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
        val tvError = findViewById<TextView>(R.id.textViewError)
        tvError.text = ""

        Handler(Looper.getMainLooper()).postDelayed({
            updateComponentStates(installing = false)
        }, 1000L)

        if (PackageArchiveUtils.isSplitArchive(myFile)) {
            tvError.text = getString(R.string.extracting_archive)
            Thread {
                val result = PackageArchiveUtils.extractArchive(this, myFile)
                runOnUiThread {
                    if (result.error != null || result.apkFiles.isEmpty()) {
                        tvError.text = getString(R.string.error_extracting_archive, result.error ?: getString(R.string.error_no_apks_in_archive))
                        updateComponentStates(installing = false)
                        return@runOnUiThread
                    }

                    if (result.isSingleApk) {
                        performSingleApkInstall(result.apkFiles[0].absolutePath)
                    } else {
                        tvError.text = ""
                        showSplitSelectionDialog(result.packageName, result.apkFiles)
                    }
                }
            }.start()
        } else {
            performSingleApkInstall(myFile.absolutePath)
        }
    }

    private fun performSingleApkInstall(filepath: String) {
        val myFile = File(filepath)
        if (shizukuTrickEnabled) {
            Log.d("MainActivity", "Avvio installazione tramite Metodo Shizuku (scelta utente).")
            
            if (!ShizukuUtils.isShizukuAvailable() || !ShizukuUtils.hasShizukuPermission()) {
                findViewById<TextView>(R.id.textViewError).text = getString(R.string.shizuku_error_inactive)
                updateComponentStates(installing = false)
                return
            }

            ShizukuUtils.installApk(this, filepath, { status ->
                findViewById<TextView>(R.id.textViewError).text = status
            }, {
                // Successo Shizuku
            })
            
        } else if (rootTrickEnabled) {
            Log.d("MainActivity", "Avvio installazione tramite Metodo Root (installer di sistema come shell).")
            installViaRootFlow(filepath)

        } else {
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

    private fun showSplitSelectionDialog(
        packageName: String?,
        apkFiles: List<File>
    ) {
        val context = this
        val items = apkFiles.map { file ->
            val name = file.name
            val isBase = PackageArchiveUtils.isBaseApk(context, file)
            if (isBase) {
                "$name (${getString(R.string.split_mandatory)})"
            } else {
                name
            }
        }.toTypedArray()

        val checkedItems = BooleanArray(apkFiles.size) { i ->
            val file = apkFiles[i]
            val splitType = PackageArchiveUtils.classifySplit(context, file)
            when (splitType) {
                PackageArchiveUtils.SplitType.BASE -> true
                PackageArchiveUtils.SplitType.ABI_COMPATIBLE -> true
                PackageArchiveUtils.SplitType.ABI_INCOMPATIBLE -> false
                PackageArchiveUtils.SplitType.DENSITY_COMPATIBLE -> true
                PackageArchiveUtils.SplitType.DENSITY_INCOMPATIBLE -> false
                PackageArchiveUtils.SplitType.LANG_COMPATIBLE -> true
                PackageArchiveUtils.SplitType.LANG_INCOMPATIBLE -> false
                PackageArchiveUtils.SplitType.GENERIC -> true
            }
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_split_selection, null)
        val selectAllCheckbox = dialogView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.selectAllCheckbox)
        val listView = dialogView.findViewById<android.widget.ListView>(R.id.splitListView)

        val adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_list_item_multiple_choice,
            items
        )
        listView.adapter = adapter

        for (i in checkedItems.indices) {
            listView.setItemChecked(i, checkedItems[i])
        }

        listView.setOnItemClickListener { _: android.widget.AdapterView<*>, _: View, position: Int, _: Long ->
            val file = apkFiles[position]
            if (PackageArchiveUtils.isBaseApk(context, file)) {
                listView.setItemChecked(position, true)
                checkedItems[position] = true
                Toast.makeText(this, R.string.base_apk_mandatory, Toast.LENGTH_SHORT).show()
            } else {
                checkedItems[position] = listView.isItemChecked(position)
            }
        }

        selectAllCheckbox.setOnCheckedChangeListener { _: android.widget.CompoundButton, isChecked: Boolean ->
            for (i in apkFiles.indices) {
                val file = apkFiles[i]
                if (!PackageArchiveUtils.isBaseApk(context, file)) {
                    checkedItems[i] = isChecked
                    listView.setItemChecked(i, isChecked)
                }
            }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_split_apks_title)
            .setView(dialogView)
            .setPositiveButton(R.string.install_as_king) { _: android.content.DialogInterface, _: Int ->
                val selectedFiles = apkFiles.filterIndexed { index, _ -> checkedItems[index] }
                if (selectedFiles.isEmpty()) {
                    Toast.makeText(this, R.string.no_splits_selected, Toast.LENGTH_SHORT).show()
                    updateComponentStates(installing = false)
                } else {
                    performSplitApkInstall(packageName, selectedFiles)
                }
            }
            .setNegativeButton(android.R.string.cancel) { _: android.content.DialogInterface, _: Int ->
                updateComponentStates(installing = false)
            }
            .setOnCancelListener {
                updateComponentStates(installing = false)
            }
            .create()

        dialog.show()
    }

    private fun performSplitApkInstall(packageName: String?, apkFiles: List<File>) {
        val tvError = findViewById<TextView>(R.id.textViewError)
        tvError.text = getString(R.string.installing_split_apks, apkFiles.size)

        if (shizukuTrickEnabled) {
            Log.d("MainActivity", "Avvio installazione split tramite Metodo Shizuku.")
            if (!ShizukuUtils.isShizukuAvailable() || !ShizukuUtils.hasShizukuPermission()) {
                tvError.text = getString(R.string.shizuku_error_inactive)
                updateComponentStates(installing = false)
                return
            }
            ShizukuUtils.installSplitApks(this, packageName, apkFiles, { status ->
                tvError.text = status
            }, {})
        } else if (rootTrickEnabled) {
            Log.d("MainActivity", "Avvio installazione split tramite Metodo Root.")
            installSplitApksRootFlow(packageName, apkFiles)
        } else {
            Log.d("MainActivity", "Avvio installazione split tramite Metodo Classico.")
            if (!packageManager.canRequestPackageInstalls()) {
                startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = "package:$packageName".toUri()
                })
                updateComponentStates(installing = false)
                return
            }
            try {
                InstallationUtils.installSplitApksClassico(this, packageName, apkFiles, errorCallback = { err ->
                    runOnUiThread {
                        tvError.setTextColor(getColor(R.color.aa_red_text))
                        tvError.text = getString(R.string.error_occurred, err)
                        updateComponentStates(installing = false)
                    }
                })
            } catch (e: Exception) {
                tvError.setTextColor(getColor(R.color.aa_red_text))
                tvError.text = getString(R.string.error_occurred, e.toString())
                updateComponentStates(installing = false)
            }
        }
    }

    private fun installSplitApksRootFlow(packageName: String?, apkFiles: List<File>) {
        val tv = findViewById<TextView>(R.id.textViewError)
        tv.setTextColor(Color.parseColor("#FFA000"))
        tv.text = getString(R.string.installing_root)

        RootUtils.installSplitApksAsShell(packageName, apkFiles) { ok, detail ->
            if (!ok) Log.w("KingInstaller", "installSplitApksAsShell fallito: $detail")
            runOnUiThread {
                if (!ok) {
                    updateComponentStates(installing = false)
                    tv.setTextColor(getColor(R.color.aa_red_text))
                    tv.text = getString(R.string.root_launch_failed)
                }
            }
        }
    }

    /**
     * Flusso d'installazione del Metodo Root: lancia il package installer di sistema COME SHELL con
     * l'extra INSTALLER_PACKAGE_NAME=vending, così "Installed by" = Play Store e "Requested by" =
     * packageinstaller vengono impostati AL VOLO. L'esito (verde/rosso) lo determina poi il
     * packageAddedReceiver verificando isAACompatible a installazione conclusa.
     */
    private fun installViaRootFlow(filepath: String) {
        val tv = findViewById<TextView>(R.id.textViewError)
        val uri = InstallationUtils.getApkContentUri(this, File(filepath))

        // Concediamo in anticipo la lettura dell'URI ai package installer: l'`am start` è lanciato
        // dalla shell, ma l'installer leggerà l'APK grazie a questo grant (indipendente dal caller).
        for (p in InstallationUtils.INSTALLER_PACKAGES) {
            try {
                grantUriPermission(p, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (ignored: Exception) {
            }
        }

        tv.setTextColor(Color.parseColor("#FFA000"))
        tv.text = getString(R.string.installing_root)

        RootUtils.launchInstallerAsShell(uri.toString()) { ok, detail ->
            if (!ok) Log.w("KingInstaller", "launchInstallerAsShell fallito: $detail")
            runOnUiThread {
                if (!ok) {
                    // Lancio dell'installer come shell fallito (es. su non concesso): messaggio
                    // chiaro a schermo, il dettaglio grezzo resta solo nel logcat/log condiviso.
                    // Se è andato, non tocchiamo la UI: l'installer di sistema è in primo piano e
                    // l'esito lo verifica il packageAddedReceiver.
                    updateComponentStates(installing = false)
                    tv.setTextColor(getColor(R.color.aa_red_text))
                    tv.text = getString(R.string.root_launch_failed)
                }
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
