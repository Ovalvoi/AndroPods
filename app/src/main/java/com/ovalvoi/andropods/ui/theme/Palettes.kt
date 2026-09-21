package com.ovalvoi.andropods.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.ovalvoi.andropods.data.ColorTheme

/**
 * One design, five colourways.
 *
 * Every palette is built from the same recipe -- the surface ladder, the
 * borders and the text tones are computed identically, and only the accent
 * hue changes. That is what keeps Ocean and Grape looking like the same app
 * rather than two apps that happen to share a layout, and it means a change
 * to the design lands in all five without editing forty constants.
 *
 * [ColorTheme.SYSTEM] ("Default") is deliberately the neutral one: a near-black
 * and a near-white with a barely-there cool cast, for people who want the
 * readout to look like nothing in particular.
 *
 * Dark surfaces sit in the #08090F-#101020 range -- deep enough to be
 * AMOLED-friendly, with just enough blue that it reads as considered rather
 * than as a flat grey.
 */
object Palettes {

    fun scheme(theme: ColorTheme, dark: Boolean): ColorScheme {
        val accent = accentFor(theme, dark)
        return if (dark) darkScheme(accent) else lightScheme(accent)
    }

    /**
     * The picker swatches.
     *
     * Two stops per palette, taken from the same source as the scheme itself
     * so a swatch cannot drift from the theme it stands for. Default shows its
     * neutral pair rather than a rainbow: it is now a real colourway, not
     * "whatever the wallpaper says".
     */
    fun swatch(theme: ColorTheme): List<Color> =
        listOf(accentFor(theme, dark = false), accentFor(theme, dark = true))

    /**
     * Each palette's accent, in its light and dark forms.
     *
     * Dark accents are lighter and less saturated than their light-mode twins:
     * a colour that reads as rich on white turns muddy on near-black, and one
     * that glows on black is unreadable on white.
     */
    private fun accentFor(theme: ColorTheme, dark: Boolean): Color = when (theme) {
        // Neutral by design: a desaturated slate that reads as "no colour"
        // without being literally grey, which would kill every accent on the
        // screen.
        ColorTheme.SYSTEM -> if (dark) Color(0xFFB9C0DA) else Color(0xFF3C4468)
        ColorTheme.OCEAN -> if (dark) Color(0xFF6FD0F5) else Color(0xFF0A6C94)
        ColorTheme.SUNSET -> if (dark) Color(0xFFFFB088) else Color(0xFFBF4A1C)
        ColorTheme.FOREST -> if (dark) Color(0xFF8BD98B) else Color(0xFF2F6B33)
        ColorTheme.GRAPE -> if (dark) Color(0xFFB79CFF) else Color(0xFF5B34C4)
    }

    /**
     * The dark scheme: near-black surfaces with a faint cast of the accent.
     *
     * The surface ladder is tinted rather than pure neutral, so a card sitting
     * on the background belongs to the same world as the accent drawn on it.
     */
    private fun darkScheme(accent: Color): ColorScheme {
        val base = Color(0xFF08090F)
        val surface = base.mix(accent, 0.035f)
        return darkColorScheme(
            primary = accent,
            onPrimary = Color(0xFF0B0C14),
            primaryContainer = accent.mix(base, 0.72f),
            onPrimaryContainer = accent.mix(Color.White, 0.55f),
            secondary = accent.mix(Color.White, 0.25f),
            onSecondary = Color(0xFF0B0C14),
            secondaryContainer = base.mix(accent, 0.12f),
            onSecondaryContainer = Color(0xFFE6E7F2),
            tertiary = accent,
            onTertiary = Color(0xFF0B0C14),
            tertiaryContainer = base.mix(accent, 0.14f),
            onTertiaryContainer = Color(0xFFE6E7F2),
            background = base,
            onBackground = Color(0xFFF2F3F9),
            surface = surface,
            onSurface = Color(0xFFF2F3F9),
            // The ladder the cards and tiles sit on, each a touch lighter
            // than the last.
            surfaceContainerLowest = base,
            surfaceContainerLow = surface.mix(Color.White, 0.02f),
            surfaceContainer = surface.mix(Color.White, 0.04f),
            surfaceContainerHigh = surface.mix(Color.White, 0.06f),
            surfaceContainerHighest = surface.mix(Color.White, 0.09f),
            surfaceVariant = surface.mix(Color.White, 0.06f),
            // Muted lavender-grey rather than a flat grey: secondary text
            // should feel related to the accent, not stamped on top of it.
            onSurfaceVariant = accent.mix(Color(0xFF9AA0B8), 0.78f),
            outline = surface.mix(Color.White, 0.16f),
            outlineVariant = surface.mix(Color.White, 0.10f),
        )
    }

    /** The light scheme: the same structure inverted onto near-white. */
    private fun lightScheme(accent: Color): ColorScheme {
        val base = Color(0xFFFBFBFE)
        val surface = base.mix(accent, 0.03f)
        return lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accent.mix(Color.White, 0.86f),
            onPrimaryContainer = accent.mix(Color.Black, 0.45f),
            secondary = accent.mix(Color.Black, 0.12f),
            onSecondary = Color.White,
            secondaryContainer = accent.mix(Color.White, 0.90f),
            onSecondaryContainer = accent.mix(Color.Black, 0.5f),
            tertiary = accent,
            onTertiary = Color.White,
            tertiaryContainer = accent.mix(Color.White, 0.88f),
            onTertiaryContainer = accent.mix(Color.Black, 0.5f),
            background = base,
            onBackground = Color(0xFF15161D),
            surface = surface,
            onSurface = Color(0xFF15161D),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color.White,
            surfaceContainer = surface.mix(accent, 0.03f),
            surfaceContainerHigh = surface.mix(accent, 0.05f),
            surfaceContainerHighest = surface.mix(accent, 0.08f),
            surfaceVariant = surface.mix(accent, 0.05f),
            onSurfaceVariant = accent.mix(Color(0xFF5A5F73), 0.72f),
            outline = accent.mix(Color(0xFFC9CCDA), 0.82f),
            outlineVariant = accent.mix(Color(0xFFE2E4EE), 0.88f),
        )
    }

    /** Blend toward [other] by [amount]; 0 keeps this colour, 1 returns [other]. */
    private fun Color.mix(other: Color, amount: Float) = Color(
        red = red + (other.red - red) * amount,
        green = green + (other.green - green) * amount,
        blue = blue + (other.blue - blue) * amount,
        alpha = 1f,
    )
}
