package com.ovalvoi.andropods.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ovalvoi.andropods.R
import com.ovalvoi.andropods.ble.PodsModel
import com.ovalvoi.andropods.ble.PodsState
import com.ovalvoi.andropods.data.LastCaseReading
import com.ovalvoi.andropods.data.PodsConnection
import kotlinx.coroutines.delay

/**
 * Single-screen readout.
 *
 * Hierarchy is carried by scale, not by boxes: the two buds are the primary
 * read at display size, the case is secondary, and the device name is a quiet
 * caption. Deliberately not a uniform three-card grid -- the case is not as
 * important as the pods and should not look it.
 */
@Composable
fun PodsScreen(
    connection: PodsConnection,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            // Quiet corner affordance: the readout stays the only thing on screen.
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp),
            ) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.settings_open),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box(Modifier.fillMaxSize().padding(horizontal = 24.dp), Alignment.Center) {
                when (connection) {
                    is PodsConnection.Disconnected -> StatusMessage(stringRes = R.string.status_disconnected)
                    is PodsConnection.Searching -> StatusMessage(stringRes = R.string.status_searching)
                    is PodsConnection.Live -> LiveReadout(connection.state, connection.lastCase)
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(stringRes: Int) {
    Text(
        text = stringResource(stringRes),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun LiveReadout(state: PodsState, lastCase: LastCaseReading?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = PodsModel.displayName(state.model),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(40.dp))

        // The pair, at the largest scale on screen.
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PodReading(
                label = stringResource(R.string.pod_left),
                level = state.leftBattery,
                isCharging = state.isLeftCharging,
            )
            PodReading(
                label = stringResource(R.string.pod_right),
                level = state.rightBattery,
                isCharging = state.isRightCharging,
            )
        }

        Spacer(Modifier.height(48.dp))

        // Case: same information, visibly subordinate. When the pods are out
        // of the case the beacon cannot know its level, so show the last one
        // we saw, dimmed and dated, rather than a dash.
        CaseReading(
            level = state.caseBattery,
            isCharging = state.isCaseCharging,
            isLidOpen = state.isLidOpen,
            remembered = lastCase.takeIf { state.caseBattery == null },
        )
    }
}

@Composable
private fun PodReading(label: String, level: Int?, isCharging: Boolean) {
    val description = readingDescription(label, level, isCharging)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.5.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = level?.let { "$it" } ?: stringResource(R.string.battery_unknown),
            fontSize = 64.sp,
            fontWeight = FontWeight.Light,
            color = levelColor(level),
        )
        if (level != null) {
            Text(
                text = "%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        LevelBar(level = level, isCharging = isCharging, width = 64.dp)
    }
}

@Composable
private fun CaseReading(
    level: Int?,
    isCharging: Boolean,
    isLidOpen: Boolean,
    remembered: LastCaseReading?,
) {
    val label = stringResource(R.string.pod_case)
    val shown = level ?: remembered?.level
    val isStale = level == null && remembered != null
    val ageText = remembered?.let { relativeAge(it) }
    val lastSeen = ageText?.let { stringResource(R.string.case_last_seen, it) }
    val description = readingDescription(label, shown, isCharging) +
        (if (isLidOpen && level != null) ", lid open" else "") +
        (if (isStale && lastSeen != null) ", $lastSeen" else "")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.5.sp,
            )
            Text(
                text = shown?.let { "$it%" } ?: stringResource(R.string.battery_unknown),
                style = MaterialTheme.typography.titleLarge,
                color = if (isStale) MaterialTheme.colorScheme.onSurfaceVariant else levelColor(shown),
            )
            LevelBar(
                level = shown,
                isCharging = isCharging && !isStale,
                width = 48.dp,
                dimmed = isStale,
            )
        }
        if (isStale && lastSeen != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = lastSeen,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * "just now" / "12 min ago" / "3 h ago" / "2 d ago", re-evaluated once a
 * minute while on screen so a stale reading keeps ageing without a beacon.
 */
@Composable
private fun relativeAge(reading: LastCaseReading): String {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(reading.seenAtMs) {
        while (true) {
            delay(AGE_REFRESH_MS)
            now = System.currentTimeMillis()
        }
    }
    val minutes = reading.ageMs(now) / 60_000
    return when {
        minutes < 1 -> stringResource(R.string.age_just_now)
        minutes < 60 -> stringResource(R.string.age_minutes, minutes.toInt())
        minutes < 60 * 24 -> stringResource(R.string.age_hours, (minutes / 60).toInt())
        else -> stringResource(R.string.age_days, (minutes / (60 * 24)).toInt())
    }
}

/**
 * A level bar that animates between readings.
 *
 * Beacons arrive in 10% steps and irregularly, so an unanimated bar would
 * visibly jump. Animating only width and colour keeps this on the compositor.
 */
@Composable
private fun LevelBar(
    level: Int?,
    isCharging: Boolean,
    width: androidx.compose.ui.unit.Dp,
    dimmed: Boolean = false,
) {
    val fraction by animateFloatAsState(
        targetValue = (level ?: 0) / 100f,
        animationSpec = tween(durationMillis = 600),
        label = "levelFraction",
    )
    val color by animateColorAsState(
        targetValue = when {
            dimmed -> MaterialTheme.colorScheme.outlineVariant
            isCharging -> MaterialTheme.colorScheme.tertiary
            else -> levelColor(level)
        },
        animationSpec = tween(durationMillis = 400),
        label = "levelColor",
    )

    Box(
        Modifier
            .width(width)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (level != null) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

/**
 * Colour carries meaning here, not decoration: low battery is the one state
 * worth interrupting the palette for.
 */
@Composable
private fun levelColor(level: Int?): Color = when {
    level == null -> MaterialTheme.colorScheme.onSurfaceVariant
    level <= LOW_BATTERY_THRESHOLD -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurface
}

private fun readingDescription(label: String, level: Int?, isCharging: Boolean): String =
    buildString {
        append(label)
        append(if (level != null) ", $level percent" else ", level unknown")
        if (isCharging) append(", charging")
    }

private const val LOW_BATTERY_THRESHOLD = 20
private const val AGE_REFRESH_MS = 60_000L
