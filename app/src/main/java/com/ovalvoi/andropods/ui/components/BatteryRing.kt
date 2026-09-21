package com.ovalvoi.andropods.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.ovalvoi.andropods.ui.theme.Dimens

/**
 * A circular battery gauge that animates between readings.
 *
 * Beacons arrive in 10% steps and irregularly, so an unanimated ring visibly
 * jumps. Only the sweep and the colour animate, and it is drawn on a Canvas
 * rather than composed from shapes, so it stays cheap enough to sit two of
 * them side by side on the main screen.
 *
 * The lit arc is a sweep gradient between [color] and a lightened form of it
 * rather than one flat colour, which is what keeps a large ring from looking
 * like a piece of clip art.
 *
 * @param level 0-100, or null for "no reading", which draws the track alone.
 * @param content drawn centred inside the ring, usually the component's glyph.
 */
@Composable
fun BatteryRing(
    level: Int?,
    color: Color,
    diameter: Dp,
    stroke: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val sweep by animateFloatAsState(
        targetValue = (level ?: 0) / 100f * FULL_SWEEP_DEGREES,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "ringSweep",
    )
    val ringColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(durationMillis = 400),
        label = "ringColor",
    )
    val track = MaterialTheme.colorScheme.surfaceContainerHighest

    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val inset = strokePx / 2
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val style = Stroke(width = strokePx, cap = StrokeCap.Round)

            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = FULL_SWEEP_DEGREES,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = style,
            )

            if (level != null && sweep > 0f) {
                drawArc(
                    // Rotated so the gradient's seam sits at the ring's start
                    // rather than cutting across the lit arc.
                    brush = Brush.sweepGradient(
                        0f to ringColor,
                        0.5f to lighten(ringColor, GRADIENT_LIFT),
                        1f to ringColor,
                    ),
                    startAngle = RING_START_DEGREES,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = style,
                )
            }
        }
        content()
    }
}

private fun lighten(color: Color, amount: Float) = Color(
    red = color.red + (1f - color.red) * amount,
    green = color.green + (1f - color.green) * amount,
    blue = color.blue + (1f - color.blue) * amount,
    alpha = color.alpha,
)

private const val FULL_SWEEP_DEGREES = 360f
/** Twelve o'clock; Canvas angles start at three o'clock. */
private const val RING_START_DEGREES = -90f
/** How much lighter the arc gets at its midpoint. */
private const val GRADIENT_LIFT = 0.28f
