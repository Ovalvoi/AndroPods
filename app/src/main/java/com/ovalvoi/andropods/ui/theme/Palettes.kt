package com.ovalvoi.andropods.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.ovalvoi.andropods.data.ColorTheme

/**
 * The four hand-picked palettes, each in a light and a dark variant.
 *
 * Only the roles that visibly matter are set; Material fills in the rest from
 * them. [swatch] is the pair of colours the Settings picker shows for each
 * theme, kept next to the schemes so the picker and the theme cannot drift.
 */
object Palettes {

    fun scheme(theme: ColorTheme, dark: Boolean): ColorScheme = when (theme) {
        ColorTheme.SYSTEM -> error("SYSTEM is Material You; resolved dynamically in AndroPodsTheme")
        ColorTheme.OCEAN -> if (dark) OceanDark else OceanLight
        ColorTheme.SUNSET -> if (dark) SunsetDark else SunsetLight
        ColorTheme.FOREST -> if (dark) ForestDark else ForestLight
        ColorTheme.GRAPE -> if (dark) GrapeDark else GrapeLight
    }

    /**
     * Representative colours per theme, for the picker swatches: the two
     * headline colours of a palette, or a four-colour wheel for Material You,
     * which has no fixed colours of its own.
     */
    fun swatch(theme: ColorTheme): List<Color> = when (theme) {
        ColorTheme.SYSTEM -> listOf(
            Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC04), Color(0xFF34A853), Color(0xFF4285F4),
        )
        ColorTheme.OCEAN -> listOf(Color(0xFF006494), Color(0xFF4DD9E4))
        ColorTheme.SUNSET -> listOf(Color(0xFFC2410C), Color(0xFFFFB0CB))
        ColorTheme.FOREST -> listOf(Color(0xFF386A20), Color(0xFFA0CFD0))
        ColorTheme.GRAPE -> listOf(Color(0xFF6D3FB5), Color(0xFFEFB8C8))
    }

    private val OceanLight = lightColorScheme(
        primary = Color(0xFF006494),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFCAE6FF),
        onPrimaryContainer = Color(0xFF001E30),
        secondary = Color(0xFF00696F),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFF9EF0F6),
        onSecondaryContainer = Color(0xFF002022),
        tertiary = Color(0xFF5B5B9E),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE2DFFF),
        onTertiaryContainer = Color(0xFF16134B),
        background = Color(0xFFF5FAFF),
        onBackground = Color(0xFF191C1E),
        surface = Color(0xFFF5FAFF),
        onSurface = Color(0xFF191C1E),
        surfaceVariant = Color(0xFFDDE3EA),
        onSurfaceVariant = Color(0xFF41484D),
        outline = Color(0xFF72787E),
        outlineVariant = Color(0xFFC1C7CE),
    )

    private val OceanDark = darkColorScheme(
        primary = Color(0xFF8FCDFF),
        onPrimary = Color(0xFF00344F),
        primaryContainer = Color(0xFF004B70),
        onPrimaryContainer = Color(0xFFCAE6FF),
        secondary = Color(0xFF4DD9E4),
        onSecondary = Color(0xFF00363A),
        secondaryContainer = Color(0xFF004F54),
        onSecondaryContainer = Color(0xFF9EF0F6),
        tertiary = Color(0xFFC4C2FF),
        onTertiary = Color(0xFF2C2A6D),
        tertiaryContainer = Color(0xFF434285),
        onTertiaryContainer = Color(0xFFE2DFFF),
        background = Color(0xFF0B1219),
        onBackground = Color(0xFFE1E2E5),
        surface = Color(0xFF0B1219),
        onSurface = Color(0xFFE1E2E5),
        surfaceVariant = Color(0xFF2A3238),
        onSurfaceVariant = Color(0xFFC1C7CE),
        outline = Color(0xFF8B9198),
        outlineVariant = Color(0xFF41484D),
    )

    private val SunsetLight = lightColorScheme(
        primary = Color(0xFFC2410C),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDBCF),
        onPrimaryContainer = Color(0xFF3A0B00),
        secondary = Color(0xFF9C3262),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFD8E4),
        onSecondaryContainer = Color(0xFF3E001D),
        tertiary = Color(0xFF6D5E00),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFBE38A),
        onTertiaryContainer = Color(0xFF211B00),
        background = Color(0xFFFFF8F5),
        onBackground = Color(0xFF221A17),
        surface = Color(0xFFFFF8F5),
        onSurface = Color(0xFF221A17),
        surfaceVariant = Color(0xFFF5DED6),
        onSurfaceVariant = Color(0xFF53433E),
        outline = Color(0xFF85736D),
        outlineVariant = Color(0xFFD8C2BB),
    )

    private val SunsetDark = darkColorScheme(
        primary = Color(0xFFFFB59B),
        onPrimary = Color(0xFF5C1900),
        primaryContainer = Color(0xFF7E2C0A),
        onPrimaryContainer = Color(0xFFFFDBCF),
        secondary = Color(0xFFFFB0CB),
        onSecondary = Color(0xFF5E1133),
        secondaryContainer = Color(0xFF7D2A4E),
        onSecondaryContainer = Color(0xFFFFD8E4),
        tertiary = Color(0xFFE0C64A),
        onTertiary = Color(0xFF383000),
        tertiaryContainer = Color(0xFF524600),
        onTertiaryContainer = Color(0xFFFBE38A),
        background = Color(0xFF1C1210),
        onBackground = Color(0xFFF0DFDA),
        surface = Color(0xFF1C1210),
        onSurface = Color(0xFFF0DFDA),
        surfaceVariant = Color(0xFF53433E),
        onSurfaceVariant = Color(0xFFD8C2BB),
        outline = Color(0xFFA08C86),
        outlineVariant = Color(0xFF53433E),
    )

    private val ForestLight = lightColorScheme(
        primary = Color(0xFF386A20),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFB7F397),
        onPrimaryContainer = Color(0xFF042100),
        secondary = Color(0xFF55624C),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD9E7CB),
        onSecondaryContainer = Color(0xFF131F0D),
        tertiary = Color(0xFF386667),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFBBEBEC),
        onTertiaryContainer = Color(0xFF002021),
        background = Color(0xFFF7FBF2),
        onBackground = Color(0xFF1A1C18),
        surface = Color(0xFFF7FBF2),
        onSurface = Color(0xFF1A1C18),
        surfaceVariant = Color(0xFFE0E4D6),
        onSurfaceVariant = Color(0xFF43483E),
        outline = Color(0xFF74796D),
        outlineVariant = Color(0xFFC3C8BB),
    )

    private val ForestDark = darkColorScheme(
        primary = Color(0xFF9CD67D),
        onPrimary = Color(0xFF0A3900),
        primaryContainer = Color(0xFF205107),
        onPrimaryContainer = Color(0xFFB7F397),
        secondary = Color(0xFFBDCBB0),
        onSecondary = Color(0xFF283420),
        secondaryContainer = Color(0xFF3E4A36),
        onSecondaryContainer = Color(0xFFD9E7CB),
        tertiary = Color(0xFFA0CFD0),
        onTertiary = Color(0xFF003738),
        tertiaryContainer = Color(0xFF1E4E4F),
        onTertiaryContainer = Color(0xFFBBEBEC),
        background = Color(0xFF111A0E),
        onBackground = Color(0xFFE2E3DC),
        surface = Color(0xFF111A0E),
        onSurface = Color(0xFFE2E3DC),
        surfaceVariant = Color(0xFF43483E),
        onSurfaceVariant = Color(0xFFC3C8BB),
        outline = Color(0xFF8D9286),
        outlineVariant = Color(0xFF43483E),
    )

    private val GrapeLight = lightColorScheme(
        primary = Color(0xFF6D3FB5),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFEADDFF),
        onPrimaryContainer = Color(0xFF25005A),
        secondary = Color(0xFF635B70),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE9DEF8),
        onSecondaryContainer = Color(0xFF1F182B),
        tertiary = Color(0xFF7E525F),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD8E3),
        onTertiaryContainer = Color(0xFF31101D),
        background = Color(0xFFFCF7FF),
        onBackground = Color(0xFF1D1B20),
        surface = Color(0xFFFCF7FF),
        onSurface = Color(0xFF1D1B20),
        surfaceVariant = Color(0xFFE7E0EC),
        onSurfaceVariant = Color(0xFF49454F),
        outline = Color(0xFF7A757F),
        outlineVariant = Color(0xFFCBC4D0),
    )

    private val GrapeDark = darkColorScheme(
        primary = Color(0xFFD0BCFF),
        onPrimary = Color(0xFF3C1A80),
        primaryContainer = Color(0xFF54339B),
        onPrimaryContainer = Color(0xFFEADDFF),
        secondary = Color(0xFFCDC2DB),
        onSecondary = Color(0xFF342D40),
        secondaryContainer = Color(0xFF4B4358),
        onSecondaryContainer = Color(0xFFE9DEF8),
        tertiary = Color(0xFFEFB8C8),
        onTertiary = Color(0xFF4A2532),
        tertiaryContainer = Color(0xFF643B48),
        onTertiaryContainer = Color(0xFFFFD8E3),
        background = Color(0xFF16111F),
        onBackground = Color(0xFFE6E0E9),
        surface = Color(0xFF16111F),
        onSurface = Color(0xFFE6E0E9),
        surfaceVariant = Color(0xFF49454F),
        onSurfaceVariant = Color(0xFFCBC4D0),
        outline = Color(0xFF948F99),
        outlineVariant = Color(0xFF49454F),
    )
}
