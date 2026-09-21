package com.ovalvoi.andropods.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.ovalvoi.andropods.ui.theme.Dimens

/**
 * A dot and a word: the connection state at a glance.
 *
 * The dot breathes a soft halo only while [isPulsing] -- live and searching
 * states are both "something is happening", and a still dot for a disconnected
 * state is the contrast that makes the pulse mean anything.
 *
 * The whole row carries one content description, so a screen reader announces
 * "Connected" rather than reading a decorative dot.
 */
@Composable
fun StatusIndicator(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    isPulsing: Boolean = false,
) {
    val halo by rememberInfiniteTransition(label = "statusPulse").animateFloat(
        initialValue = if (isPulsing) 0f else 0f,
        targetValue = if (isPulsing) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_800),
            repeatMode = RepeatMode.Restart,
        ),
        label = "statusHalo",
    )

    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.TightGap),
    ) {
        Canvas(Modifier.size(DOT_BOX)) {
            val centre = size.minDimension / 2
            if (isPulsing && halo > 0f) {
                drawCircle(
                    color = color.copy(alpha = (1f - halo) * HALO_ALPHA),
                    radius = centre * (DOT_FRACTION + halo * HALO_GROWTH),
                )
            }
            drawCircle(color = color, radius = centre * DOT_FRACTION)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val DOT_BOX = 16.dp
/** The solid dot, as a fraction of the box; the rest is room for the halo. */
private const val DOT_FRACTION = 0.45f
private const val HALO_GROWTH = 0.55f
private const val HALO_ALPHA = 0.55f
