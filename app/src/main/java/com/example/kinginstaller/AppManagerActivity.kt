package com.example.kinginstaller

import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale
import kotlin.concurrent.thread

data class AppItem(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val isSystem: Boolean,
    val searchKey: String
)

class AppManagerActivity : AppCompatActivity() {

    private lateinit var adapter: AppAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var searchEditText: TextInputEditText
    private lateinit var systemSwitch: MaterialSwitch
    
    private var allAppsItems: List<AppItem> = emptyList()

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
        adapter = AppAdapter { appItem ->
            showInstallerInfo(appItem)
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
                val pm = packageManager
                val packages = pm.getInstalledApplications(0)
                
                val items = packages.map { app ->
                    val label = app.loadLabel(pm).toString()
                    AppItem(
                        packageName = app.packageName,
                        label = label,
                        icon = app.loadIcon(pm),
                        isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                        searchKey = (label + app.packageName).lowercase(Locale.getDefault())
                    )
                }.sortedBy { it.label.lowercase(Locale.getDefault()) }

                runOnUiThread {
                    allAppsItems = items
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
        val query = searchEditText.text.toString().lowercase(Locale.getDefault())
        val showSystem = systemSwitch.isChecked

        val filtered = allAppsItems.filter { item ->
            val matchesQuery = query.isEmpty() || item.searchKey.contains(query)
            matchesQuery && (showSystem || !item.isSystem)
        }

        adapter.submitList(filtered)
    }

    private fun showInstallerInfo(app: AppItem) {
        val packageName = app.packageName
        
        try {
            val pm = packageManager
            var initiating: String? = null
            var installing: String? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val info = pm.getInstallSourceInfo(packageName)
                initiating = info.initiatingPackageName
                installing = info.installingPackageName
            } else {
                @Suppress("DEPRECATION")
                installing = pm.getInstallerPackageName(packageName)
            }

            // Logica compatibilità Android Auto
            val isPlayStoreInstalling = installing == "com.android.vending"
            val isValidInitiating = initiating == "com.android.vending" || 
                                   initiating == "com.google.android.packageinstaller" ||
                                   (initiating?.contains("packageinstaller") == true)

            val isCompatible = isPlayStoreInstalling && isValidInitiating

            // Create custom dialog view
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_installer_info, null)
            val iconImg = dialogView.findViewById<ImageView>(R.id.dialogAppIcon)
            val nameTv = dialogView.findViewById<TextView>(R.id.dialogAppName)
            val pkgTv = dialogView.findViewById<TextView>(R.id.dialogPackageName)
            val initiatingTv = dialogView.findViewById<TextView>(R.id.initiatingText)
            val installingTv = dialogView.findViewById<TextView>(R.id.installingText)
            val compCard = dialogView.findViewById<MaterialCardView>(R.id.compatibilityCard)
            val compIcon = dialogView.findViewById<ImageView>(R.id.compatibilityIcon)
            val compTv = dialogView.findViewById<TextView>(R.id.compatibilityText)

            iconImg.setImageDrawable(app.icon)
            nameTv.text = app.label
            pkgTv.text = app.packageName
            initiatingTv.text = formatPackageInfo(initiating)
            installingTv.text = formatPackageInfo(installing)

            if (isCompatible) {
                compCard.setCardBackgroundColor(getColor(R.color.aa_green_container))
                compTv.text = getString(R.string.aa_compatibility_ok)
                compTv.setTextColor(getColor(R.color.aa_green_text))
                compIcon.setImageResource(android.R.drawable.checkbox_on_background)
                compIcon.setColorFilter(getColor(R.color.aa_green_text))
            } else {
                compCard.setCardBackgroundColor(getColor(R.color.aa_red_container))
                compTv.text = getString(R.string.aa_compatibility_error)
                compTv.setTextColor(getColor(R.color.aa_red_text))
                compIcon.setImageResource(android.R.drawable.ic_delete)
                compIcon.setColorFilter(getColor(R.color.aa_red_text))
            }

            MaterialAlertDialogBuilder(this)
                .setView(dialogView)
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

    class AppAdapter(private val onClick: (AppItem) -> Unit) : ListAdapter<AppItem, AppAdapter.ViewHolder>(DiffCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position)
            holder.name.text = item.label
            holder.pkg.text = item.packageName
            holder.icon.setImageDrawable(item.icon)
            holder.systemTag.visibility = if (item.isSystem) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener { onClick(item) }
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.appIcon)
            val name: TextView = view.findViewById(R.id.appName)
            val pkg: TextView = view.findViewById(R.id.packageName)
            val systemTag: View = view.findViewById(R.id.systemTag)
        }

        class DiffCallback : DiffUtil.ItemCallback<AppItem>() {
            override fun areItemsTheSame(oldItem: AppItem, newItem: AppItem) = oldItem.packageName == newItem.packageName
            override fun areContentsTheSame(oldItem: AppItem, newItem: AppItem) = oldItem == newItem
        }
    }
}