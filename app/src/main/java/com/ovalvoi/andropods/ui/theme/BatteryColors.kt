package com.ovalvoi.andropods.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.ovalvoi.andropods.ui.BatteryBand

/**
 * Green / orange / red for battery levels, plus the case's own colour.
 *
 * These are meaning, not decoration, so they do not follow the palette: a red
 * 10% must read as red whichever colourway is selected. Light and dark
 * variants exist only so each stays legible against its background.
 *
 * Note this means battery state is never carried by hue alone -- the number
 * and its label say the same thing in text, which is what keeps the readout
 * usable for anyone who cannot separate these colours.
 */
@Immutable
data class BatteryColors(
    val good: Color,
    val low: Color,
    val critical: Color,
    /**
     * The charging case, which takes a pink/coral of its own.
     *
     * Deliberately outside the green/orange/red scale: the case is a different
     * *kind* of thing from a bud, and giving it a fourth colour lets the eye
     * find it without reading the label. Its level still drives the ring's
     * sweep, so a nearly-flat case is visible as geometry as well as colour.
     */
    val case: Color,
) {
    companion object {
        /**
         * One set of colours for both brightness modes.
         *
         * These are regulated values, not decoration: a 15% reading has to be
         * the same red whether the phone is light or dark, whichever palette
         * is selected. Two variants of "the warning colour" meant the same
         * state was shown in two different hues, which is exactly what a
         * status colour must not do.
         *
         * Each was picked to clear 3:1 contrast against *both* backgrounds
         * the app draws on -- a white popup tile and the near-black surface --
         * and measured rather than judged by eye:
         *
         * ```
         *            on white   on near-black
         *   good       3.39          5.87
         *   low        3.16          6.28
         *   critical   4.04          4.92
         *   case       3.71          5.36
         * ```
         *
         * That constraint is what keeps them mid-toned. A neon green or a
         * bright amber reads well on black and disappears on white.
         */
        val Shared = BatteryColors(
            good = Color(0xFF22A04A),
            low = Color(0xFFD97800),
            critical = Color(0xFFF0322C),
            case = Color(0xFFE0527A),
        )
    }
}

val LocalBatteryColors = staticCompositionLocalOf { BatteryColors.Shared }

/** The colour for a battery band; an unknown reading takes the quiet variant colour. */
@Composable
@ReadOnlyComposable
fun batteryColor(band: BatteryBand): Color {
    val colors = LocalBatteryColors.current
    return when (band) {
        BatteryBand.GOOD -> colors.good
        BatteryBand.LOW -> colors.low
        BatteryBand.CRITICAL -> colors.critical
        BatteryBand.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
@ReadOnlyComposable
fun batteryColor(level: Int?): Color = batteryColor(BatteryBand.of(level))

/** The case's colour, or the quiet variant colour when it has never been seen. */
@Composable
@ReadOnlyComposable
fun caseColor(level: Int?): Color =
    if (level == null) MaterialTheme.colorScheme.onSurfaceVariant else LocalBatteryColors.current.case
