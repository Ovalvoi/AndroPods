package com.ovalvoi.andropods.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.ovalvoi.andropods.ui.AppIcons
import com.ovalvoi.andropods.ui.theme.Dimens
import com.ovalvoi.andropods.ui.theme.batteryColor

/**
 * One earbud: its ring, its glyph, the percentage, then the label.
 *
 * The primary read on the screen, so it is the largest thing on it. The
 * charging bolt sits as a badge on the ring rather than beside the number,
 * which keeps the numeric column aligned between the two buds.
 */
@Composable
fun EarbudBatteryCard(
    label: String,
    icon: ImageVector,
    level: Int?,
    isCharging: Boolean,
    ringDiameter: Dp,
    numberSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val color = batteryColor(level)
    val description = buildString {
        append(label)
        append(if (level != null) ", $level percent" else ", level unknown")
        if (isCharging) append(", charging")
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        BatteryRing(
            level = level,
            color = color,
            diameter = ringDiameter,
            stroke = Dimens.RingStroke,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(Dimens.IconEarbud),
                tint = color,
            )
            if (isCharging) ChargingBadge(Modifier.align(Alignment.BottomEnd))
        }
        Spacer(Modifier.height(Dimens.ItemGap))
        LevelText(level = level, color = color, numberSize = numberSize)
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = Dimens.LabelTracking,
        )
    }
}

/** The charging bolt, ringed in the background colour so it reads as raised. */
@Composable
private fun ChargingBadge(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(BADGE_SIZE)
            .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
            .padding(3.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        Alignment.Center,
    ) {
        Icon(
            AppIcons.Bolt,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

private val BADGE_SIZE = 30.dp
