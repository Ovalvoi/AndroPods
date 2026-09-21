package com.ovalvoi.andropods.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AppearanceTest {

    @Test
    fun everyColorThemeRoundTripsThroughItsKey() {
        ColorTheme.entries.forEach { theme ->
            assertSame(theme, ColorTheme.fromKey(theme.key))
        }
    }

    @Test
    fun everyDarkModeRoundTripsThroughItsKey() {
        DarkMode.entries.forEach { mode ->
            assertSame(mode, DarkMode.fromKey(mode.key))
        }
    }

    @Test
    fun unknownOrMissingKeysFallBackToTheDefaults() {
        assertSame(ColorTheme.DEFAULT, ColorTheme.fromKey(null))
        assertSame(ColorTheme.DEFAULT, ColorTheme.fromKey("neon_zebra"))
        assertSame(DarkMode.DEFAULT, DarkMode.fromKey(null))
        assertSame(DarkMode.DEFAULT, DarkMode.fromKey("sepia"))
    }

    @Test
    fun theDefaultThemeIsMaterialYouAndTheDefaultModeFollowsTheSystem() {
        // Both are the pre-theming behaviour, so an upgrade changes nothing visually.
        assertSame(ColorTheme.SYSTEM, ColorTheme.DEFAULT)
        assertSame(DarkMode.SYSTEM, DarkMode.DEFAULT)
    }

    @Test
    fun keysAreStableIdentifiersNotEnumNames() {
        // Persisted in SharedPreferences: renaming an enum constant must not
        // silently reset a user's choice.
        assertEquals("system", ColorTheme.SYSTEM.key)
        assertEquals("ocean", ColorTheme.OCEAN.key)
        assertEquals("sunset", ColorTheme.SUNSET.key)
        assertEquals("forest", ColorTheme.FOREST.key)
        assertEquals("grape", ColorTheme.GRAPE.key)
        assertEquals("system", DarkMode.SYSTEM.key)
        assertEquals("light", DarkMode.LIGHT.key)
        assertEquals("dark", DarkMode.DARK.key)
    }

    @Test
    fun thereAreExactlyFourPalettesBesidesMaterialYou() {
        assertEquals(4, ColorTheme.entries.count { it != ColorTheme.SYSTEM })
    }
}
