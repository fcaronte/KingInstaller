package com.example.kinginstaller

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

object AppLanguageSettings {
    const val MIN_SDK = Build.VERSION_CODES.TIRAMISU

    fun isSupported(sdkInt: Int, settingsActivityResolvable: Boolean): Boolean =
        sdkInt >= MIN_SDK && settingsActivityResolvable

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createSettingsIntent(packageName: String): Intent =
        Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
}
