package com.example.kinginstaller

import android.app.Application
import android.content.Context
import android.os.Build
import com.google.android.material.color.DynamicColors
import org.lsposed.hiddenapibypass.HiddenApiBypass

class KingInstallerApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("")
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Applica i colori dinamici a tutte le Activity dell'app
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
