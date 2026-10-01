package com.example.kinginstaller

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageSettingsTest {
    @Test
    fun supportedOnAndroid13WhenSystemCanOpenSettings() {
        assertTrue(AppLanguageSettings.isSupported(33, true))
        assertTrue(AppLanguageSettings.isSupported(36, true))
    }

    @Test
    fun hiddenWhenNativePerAppLanguageIsUnavailable() {
        assertFalse(AppLanguageSettings.isSupported(32, true))
        assertFalse(AppLanguageSettings.isSupported(33, false))
        assertFalse(AppLanguageSettings.isSupported(26, false))
    }
}
