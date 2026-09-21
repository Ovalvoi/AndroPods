package com.ovalvoi.andropods.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.ovalvoi.andropods.data.ColorTheme
import com.ovalvoi.andropods.data.DarkMode

/**
 * App theming.
 *
 * [ColorTheme.SYSTEM] is Material You: the app picks up the wallpaper palette
 * and looks native rather than branded. That is still the default. The named
 * palettes in [Palettes] are opt-in from Settings. The static schemes are a
 * fallback for API < 31, which minSdk makes unreachable today but costs
 * nothing to keep correct.
 *
 * Battery colours (green / orange / red) are semantic: the same in every
 * palette and in both brightness modes, because they report a regulated
 * state rather than decorate a surface.
 */
@Composable
fun AndroPodsTheme(
    colorTheme: ColorTheme = ColorTheme.DEFAULT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        colorTheme != ColorTheme.SYSTEM -> Palettes.scheme(colorTheme, darkTheme)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    CompositionLocalProvider(
        // One set for both modes: battery state is regulated information,
        // so the same level must be the same colour everywhere.
        LocalBatteryColors provides BatteryColors.Shared,
    ) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}

/** Whether the app should draw dark right now, honouring the user's override. */
@Composable
fun DarkMode.resolve(): Boolean = when (this) {
    DarkMode.SYSTEM -> isSystemInDarkTheme()
    DarkMode.LIGHT -> false
    DarkMode.DARK -> true
}

/**
 * The same resolution as [resolve], outside composition.
 *
 * The overlay window builds its theme from a Service, where there is no
 * composition to read `isSystemInDarkTheme()` from, so [DarkMode.SYSTEM] is
 * answered from the configuration's UI mode instead.
 */
fun DarkMode.resolveForOverlay(context: android.content.Context): Boolean = when (this) {
    DarkMode.LIGHT -> false
    DarkMode.DARK -> true
    DarkMode.SYSTEM ->
        context.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
}
