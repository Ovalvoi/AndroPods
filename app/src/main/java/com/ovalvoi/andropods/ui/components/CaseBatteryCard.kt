package com.ovalvoi.andropods.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ovalvoi.andropods.R
import com.ovalvoi.andropods.ui.AppIcons
import com.ovalvoi.andropods.ui.theme.Dimens
import com.ovalvoi.andropods.ui.theme.caseColor

/**
 * The charging case: ring on the left, label and level on the right.
 *
 * Visibly subordinate to the buds -- a wide card rather than a tall gauge --
 * because the case level matters less moment to moment and is often not live.
 *
 * @param remembered how long ago this reading was taken, when it is not live.
 *   The case has no radio of its own, so with both buds out of it there is
 *   nothing current to show and the honest thing is the last reading, dated.
 */
@Composable
fun CaseBatteryCard(
    level: Int?,
    isCharging: Boolean,
    isLidOpen: Boolean,
    modifier: Modifier = Modifier,
    remembered: String? = null,
) {
    val color = caseColor(level)
    val label = stringResource(R.string.pod_case)
    val description = buildString {
        append(label)
        append(if (level != null) ", $level percent" else ", level unknown")
        if (isLidOpen) append(", lid open")
        if (isCharging) append(", charging")
        if (remembered != null) append(", $remembered")
    }

    Surface(
        modifier = modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
        shape = RoundedCornerShape(Dimens.CornerCard),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(Dimens.BorderHairline, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(Dimens.CardPadding),
        ) {
            BatteryRing(
                level = level,
                color = color,
                diameter = Dimens.RingDiameterCase,
                stroke = Dimens.RingStrokeCase,
            ) {
                Icon(
                    if (isLidOpen) AppIcons.CaseOpen else AppIcons.CaseClosed,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.IconCase),
                    tint = color,
                )
            }
            Spacer(Modifier.width(Dimens.CardPadding))
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = Dimens.LabelTracking,
                    )
                    if (isLidOpen) StateTag(stringResource(R.string.lid_open))
                    if (isCharging) StateTag(stringResource(R.string.charging), AppIcons.Bolt)
                }
                Spacer(Modifier.height(2.dp))
                LevelText(
                    level = level,
                    color = color,
                    numberSize = CASE_NUMBER_SIZE,
                    prefix = if (remembered != null) "~" else "",
                )
                if (remembered != null) {
                    Text(
                        text = remembered,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** A small tonal pill for a state word: "Lid open", "Charging". */
@Composable
private fun StateTag(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = TAG_ALPHA),
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(11.dp))
            Text(text = text, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private val CASE_NUMBER_SIZE = 30.sp
private const val TAG_ALPHA = 0.9f
