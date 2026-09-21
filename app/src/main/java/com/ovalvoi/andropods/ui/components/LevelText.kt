package com.ovalvoi.andropods.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.ovalvoi.andropods.R

/**
 * A battery percentage: the number large, the "%" small on the same baseline.
 *
 * The number counts rather than cutting, so a reading moving from 80 to 70
 * reads as a change instead of a flicker. An unknown level renders as a dash,
 * never as 0%, because the beacon reports "unknown" routinely and zero would
 * be a lie.
 *
 * @param prefix e.g. "~" for a remembered case reading.
 */
@Composable
fun LevelText(
    level: Int?,
    color: Color,
    numberSize: TextUnit,
    modifier: Modifier = Modifier,
    prefix: String = "",
) {
    val animated by animateIntAsState(
        targetValue = level ?: 0,
        animationSpec = tween(durationMillis = 650),
        label = "levelCount",
    )

    Row(modifier) {
        Text(
            text = if (level != null) "$prefix$animated" else stringResource(R.string.battery_unknown),
            fontSize = numberSize,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.alignByBaseline(),
        )
        if (level != null) {
            Text(
                text = "%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = color,
                modifier = Modifier.alignByBaseline().padding(start = 2.dp),
            )
        }
    }
}
