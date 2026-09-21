package com.ovalvoi.andropods.data

/**
 * The colour palette the app is drawn in.
 *
 * [SYSTEM] is Material You: the phone's wallpaper palette, which is what the
 * app has always used and stays the default. The four named palettes are for
 * people who want the readout to look like *something* rather than like their
 * wallpaper. Each has a light and a dark variant; see `ui/theme/Palettes.kt`.
 *
 * [key] is what gets persisted. It is deliberately not the enum name so a
 * constant can be renamed without resetting anyone's choice.
 */
enum class ColorTheme(val key: String) {
    SYSTEM("system"),
    OCEAN("ocean"),
    SUNSET("sunset"),
    FOREST("forest"),
    GRAPE("grape");

    companion object {
        val DEFAULT = SYSTEM

        /** Unknown or missing keys fall back to [DEFAULT] rather than crashing on load. */
        fun fromKey(key: String?): ColorTheme = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

/** Light/dark override. [SYSTEM] follows the phone's setting, as before. */
enum class DarkMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        val DEFAULT = SYSTEM

        fun fromKey(key: String?): DarkMode = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

/**
 * Which screen edge the connect popup slides in from.
 *
 * [BOTTOM] is the default: it is inside thumb reach and cannot collide with
 * the status bar or a notch, which the top edge can on a Pixel.
 */
enum class PopupPosition(val key: String) {
    TOP("top"),
    BOTTOM("bottom");

    companion object {
        val DEFAULT = BOTTOM

        fun fromKey(key: String?): PopupPosition = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}
