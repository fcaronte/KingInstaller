package com.example.kinginstaller

import android.app.Application
import com.google.android.material.color.DynamicColors

class KingInstallerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Applica i colori dinamici a tutte le Activity dell'app
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
