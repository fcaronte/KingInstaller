package com.example.kinginstaller

import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.DynamicColors
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import kotlin.concurrent.thread

class AppManagerActivity : AppCompatActivity() {

    private lateinit var adapter: AppAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var searchEditText: TextInputEditText
    private lateinit var systemSwitch: MaterialSwitch
    
    private var allApps: List<ApplicationInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        DynamicColors.applyToActivitiesIfAvailable(application)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_manager)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.app_manager_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        progressBar = findViewById(R.id.progressBar)
        searchEditText = findViewById(R.id.searchEditText)
        systemSwitch = findViewById(R.id.showSystemAppsSwitch)

        val recyclerView = findViewById<RecyclerView>(R.id.appsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AppAdapter { appInfo ->
            showInstallerInfo(appInfo)
        }
        recyclerView.adapter = adapter

        searchEditText.addTextChangedListener { filterApps() }
        systemSwitch.setOnCheckedChangeListener { _, _ -> filterApps() }

        loadApps()
    }

    private fun loadApps() {
        progressBar.visibility = View.VISIBLE
        thread {
            try {
                // Usiamo 0 invece di GET_META_DATA per risparmiare memoria ed evitare crash su dispositivi con troppe app
                val packages = packageManager.getInstalledApplications(0)
                allApps = packages
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    filterApps()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Error loading apps: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun filterApps() {
        val query = searchEditText.text.toString().lowercase()
        val showSystem = systemSwitch.isChecked
        val pm = packageManager

        val filtered = allApps.filter { app ->
            try {
                val label = app.loadLabel(pm).toString().lowercase()
                val pkg = app.packageName.lowercase()
                val matchesQuery = label.contains(query) || pkg.contains(query)
                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                
                matchesQuery && (showSystem || !isSystem)
            } catch (e: Exception) {
                false
            }
        }.sortedBy { it.loadLabel(pm).toString().lowercase() }

        adapter.submitList(filtered)
    }

    private fun showInstallerInfo(app: ApplicationInfo) {
        val packageName = app.packageName
        val appLabel = app.loadLabel(packageManager).toString()
        
        try {
            val pm = packageManager
            val packageInfo = pm.getPackageInfo(packageName, 0)
            val appInfo = packageInfo.applicationInfo
            val isSystem = if (appInfo != null) (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 else false
            
            val message = StringBuilder()
            message.append(getString(R.string.app_type, if (isSystem) getString(R.string.system_app) else getString(R.string.user_app))).append("\n\n")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val info = pm.getInstallSourceInfo(packageName)
                val initiating = info.initiatingPackageName
                val installing = info.installingPackageName
                val originating = info.originatingPackageName

                message.append(getString(R.string.initiating_installer, formatPackageInfo(initiating))).append("\n\n")
                message.append(getString(R.string.installing_installer, formatPackageInfo(installing))).append("\n\n")
                message.append(getString(R.string.originating_installer, formatPackageInfo(originating)))

                if (Build.VERSION.SDK_INT >= 34) {
                    val updateOwner = info.updateOwnerPackageName
                    message.append("\n\n").append(getString(R.string.update_owner, formatPackageInfo(updateOwner)))
                }
            } else {
                @Suppress("DEPRECATION")
                val installing = pm.getInstallerPackageName(packageName)
                message.append(getString(R.string.installing_installer, formatPackageInfo(installing)))
            }

            AlertDialog.Builder(this)
                .setTitle(appLabel)
                .setMessage(message.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_occurred, e.message ?: e.toString()), Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatPackageInfo(packageName: String?): String {
        if (packageName == null) return getString(R.string.no_info)
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val label = pm.getApplicationLabel(appInfo).toString()
            "$label ($packageName)"
        } catch (e: Exception) {
            packageName
        }
    }

    inner class AppAdapter(private val onClick: (ApplicationInfo) -> Unit) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {
        private var list: List<ApplicationInfo> = emptyList()

        fun submitList(newList: List<ApplicationInfo>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = list[position]
            val pm = packageManager
            holder.name.text = app.loadLabel(pm)
            holder.pkg.text = app.packageName
            holder.icon.setImageDrawable(app.loadIcon(pm))
            
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            holder.systemTag.visibility = if (isSystem) View.VISIBLE else View.GONE
            
            holder.itemView.setOnClickListener { onClick(app) }
        }

        override fun getItemCount() = list.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.appIcon)
            val name: TextView = view.findViewById(R.id.appName)
            val pkg: TextView = view.findViewById(R.id.packageName)
            val systemTag: TextView = view.findViewById(R.id.systemTag)
        }
    }
}