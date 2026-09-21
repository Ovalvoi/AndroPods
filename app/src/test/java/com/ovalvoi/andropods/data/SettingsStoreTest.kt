package com.ovalvoi.andropods.data

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsStoreTest {

    /** Just enough of SharedPreferences for [SettingsStore.load]; writes are not exercised. */
    private class FakePrefs(private val values: Map<String, Any?> = emptyMap()) : SharedPreferences {
        override fun getAll(): MutableMap<String, *> = values.toMutableMap()
        override fun getString(key: String?, defValue: String?) = values[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?) = defValues
        override fun getInt(key: String?, defValue: Int) = values[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long) = values[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float) = values[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean) = values[key] as? Boolean ?: defValue
        override fun contains(key: String?) = values.containsKey(key)
        override fun edit(): SharedPreferences.Editor = throw UnsupportedOperationException("read-only fake")
        override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    }

    @Test
    fun emptyPreferencesLoadTheDefaults() {
        assertEquals(AppSettings(), SettingsStore.load(FakePrefs()))
    }

    @Test
    fun loadsEveryPersistedField() {
        val loaded = SettingsStore.load(
            FakePrefs(
                mapOf(
                    "auto_dismiss_popup" to false,
                    "popup_timeout_seconds" to 15,
                    "resume_music_on_connect" to true,
                    "color_theme" to "sunset",
                    "dark_mode" to "dark",
                )
            )
        )

        assertFalse(loaded.autoDismissPopup)
        assertEquals(15, loaded.popupTimeoutSeconds)
        assertTrue(loaded.resumeMusicOnConnect)
        assertSame(ColorTheme.SUNSET, loaded.colorTheme)
        assertSame(DarkMode.DARK, loaded.darkMode)
    }

    @Test
    fun aTimeoutOutsideTheOfferedChoicesIsClampedToTheDefault() {
        val loaded = SettingsStore.load(FakePrefs(mapOf("popup_timeout_seconds" to 999)))
        assertEquals(AppSettings.DEFAULT_POPUP_TIMEOUT_SECONDS, loaded.popupTimeoutSeconds)
    }

    @Test
    fun anUnknownThemeOrModeKeyFallsBackToTheDefault() {
        // e.g. a theme removed in a later version, or a downgrade.
        val loaded = SettingsStore.load(FakePrefs(mapOf("color_theme" to "lava", "dark_mode" to "amoled")))
        assertSame(ColorTheme.DEFAULT, loaded.colorTheme)
        assertSame(DarkMode.DEFAULT, loaded.darkMode)
    }
}
