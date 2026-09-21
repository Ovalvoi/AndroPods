package com.ovalvoi.andropods.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * User-facing options. Immutable; change one with [copy] and hand it to
 * [SettingsStore.update].
 */
data class AppSettings(
    /** Dismiss the case-opened popup by itself after [popupTimeoutSeconds]. */
    val autoDismissPopup: Boolean = true,
    val popupTimeoutSeconds: Int = DEFAULT_POPUP_TIMEOUT_SECONDS,
    /**
     * Slide a themed battery card in from the screen edge when the pods
     * connect. Needs the overlay permission; see [OverlayPermission].
     */
    val showConnectPopup: Boolean = true,
    val popupPosition: PopupPosition = PopupPosition.DEFAULT,
    /** Send a media PLAY to the last active player once the pods' audio link is up. */
    val resumeMusicOnConnect: Boolean = false,
    val colorTheme: ColorTheme = ColorTheme.DEFAULT,
    val darkMode: DarkMode = DarkMode.DEFAULT,
) {
    val popupTimeoutMs: Long get() = popupTimeoutSeconds * 1_000L

    companion object {
        /**
         * Long enough to actually read three battery levels at a glance,
         * which 8 s was not -- the card was gone before the numbers landed.
         */
        const val DEFAULT_POPUP_TIMEOUT_SECONDS = 15

        /** The choices offered in Settings; any persisted value outside them is clamped on load. */
        val POPUP_TIMEOUT_CHOICES_SECONDS = listOf(3, 8, 15)
    }
}

/**
 * SharedPreferences-backed store exposing [AppSettings] as a [StateFlow], so the
 * service and Compose observe the same object.
 *
 * Process-wide like [PodsRepository], and for the same reason: the service and
 * the Activity outlive each other, and the settings must outlive both.
 */
object SettingsStore {

    private lateinit var prefs: SharedPreferences
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    /** Call once from the Application. Safe to call again; later calls are no-ops. */
    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _settings.value = load(prefs)
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        prefs.edit {
            putBoolean(KEY_AUTO_DISMISS, next.autoDismissPopup)
            putInt(KEY_POPUP_TIMEOUT, next.popupTimeoutSeconds)
            putBoolean(KEY_SHOW_CONNECT_POPUP, next.showConnectPopup)
            putString(KEY_POPUP_POSITION, next.popupPosition.key)
            putBoolean(KEY_RESUME_MUSIC, next.resumeMusicOnConnect)
            putString(KEY_COLOR_THEME, next.colorTheme.key)
            putString(KEY_DARK_MODE, next.darkMode.key)
        }
    }

    internal fun load(prefs: SharedPreferences): AppSettings {
        val defaults = AppSettings()
        val timeout = prefs.getInt(KEY_POPUP_TIMEOUT, defaults.popupTimeoutSeconds)
        return AppSettings(
            autoDismissPopup = prefs.getBoolean(KEY_AUTO_DISMISS, defaults.autoDismissPopup),
            popupTimeoutSeconds = if (timeout in AppSettings.POPUP_TIMEOUT_CHOICES_SECONDS) {
                timeout
            } else {
                defaults.popupTimeoutSeconds
            },
            showConnectPopup = prefs.getBoolean(KEY_SHOW_CONNECT_POPUP, defaults.showConnectPopup),
            popupPosition = PopupPosition.fromKey(prefs.getString(KEY_POPUP_POSITION, null)),
            resumeMusicOnConnect = prefs.getBoolean(KEY_RESUME_MUSIC, defaults.resumeMusicOnConnect),
            colorTheme = ColorTheme.fromKey(prefs.getString(KEY_COLOR_THEME, null)),
            darkMode = DarkMode.fromKey(prefs.getString(KEY_DARK_MODE, null)),
        )
    }

    private const val PREFS_NAME = "settings"
    private const val KEY_AUTO_DISMISS = "auto_dismiss_popup"
    private const val KEY_POPUP_TIMEOUT = "popup_timeout_seconds"
    private const val KEY_SHOW_CONNECT_POPUP = "show_connect_popup"
    private const val KEY_POPUP_POSITION = "popup_position"
    private const val KEY_RESUME_MUSIC = "resume_music_on_connect"
    private const val KEY_COLOR_THEME = "color_theme"
    private const val KEY_DARK_MODE = "dark_mode"
}
