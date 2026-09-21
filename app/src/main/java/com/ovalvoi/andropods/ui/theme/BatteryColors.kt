package com.ovalvoi.andropods.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.ovalvoi.andropods.ui.BatteryBand

/**
 * Green / orange / red for battery levels.
 *
 * These are meaning, not decoration, so they do not follow the palette: a red
 * 10% must read as red in every theme. Light and dark variants exist only for
 * contrast against the background.
 */
@Immutable
data class BatteryColors(
    val good: Color,
    val low: Color,
    val critical: Color,
) {
    companion object {
        val Light = BatteryColors(
            good = Color(0xFF2E7D32),
            low = Color(0xFFEF6C00),
            critical = Color(0xFFD32F2F),
        )
        val Dark = BatteryColors(
            good = Color(0xFF66BB6A),
            low = Color(0xFFFFA726),
            critical = Color(0xFFEF5350),
        )
    }
}

val LocalBatteryColors = staticCompositionLocalOf { BatteryColors.Light }

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
