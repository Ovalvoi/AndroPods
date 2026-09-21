package com.ovalvoi.andropods.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ovalvoi.andropods.R
import com.ovalvoi.andropods.ble.PodsModel
import com.ovalvoi.andropods.ble.PodsState
import com.ovalvoi.andropods.data.LastCaseReading
import com.ovalvoi.andropods.data.PodsConnection
import com.ovalvoi.andropods.ui.theme.LocalBatteryColors
import com.ovalvoi.andropods.ui.theme.batteryColor
import kotlinx.coroutines.delay

/**
 * Single-screen readout.
 *
 * Hierarchy is carried by scale: the two buds are the primary read, each in a
 * ring gauge with its glyph; the case sits below in a quieter card. Colour is
 * semantic -- the ring, the glyph and the number all take the battery band's
 * green / orange / red -- while the palette only tints the background wash
 * and the chrome around it.
 */
@Composable
fun PodsScreen(
    connection: PodsConnection,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val wash = scheme.primaryContainer.copy(alpha = WASH_ALPHA)
    Box(
        modifier
            .fillMaxSize()
            .background(scheme.background)
            // A wash of the palette's container colour fading into the
            // background: enough to feel themed, not enough to fight the
            // battery colours.
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(wash, Color.Transparent), endY = size.height * WASH_EXTENT))
            },
    ) {
        Header(connection, onOpenSettings)

        Box(Modifier.fillMaxSize().padding(horizontal = 24.dp), Alignment.Center) {
            when (connection) {
                is PodsConnection.Disconnected -> IdleState(
                    icon = AppIcons.BluetoothOff,
                    messageRes = R.string.status_disconnected,
                    hintRes = R.string.status_disconnected_hint,
                )
                is PodsConnection.Searching -> IdleState(
                    icon = AppIcons.Bluetooth,
                    messageRes = R.string.status_searching,
                    hintRes = R.string.status_searching_hint,
                    isPulsing = true,
                )
                is PodsConnection.Live -> LiveReadout(connection.state, connection.lastCase)
            }
        }
    }
}

@Composable
private fun Header(connection: PodsConnection, onOpenSettings: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 24.dp, end = 8.dp, top = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            StatusPill(connection)
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.settings_open),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A dot and a word: the connection state at a glance, under the title. */
@Composable
private fun StatusPill(connection: PodsConnection) {
    val (dot, textRes) = when (connection) {
        is PodsConnection.Disconnected -> MaterialTheme.colorScheme.outline to R.string.status_disconnected
        is PodsConnection.Searching -> MaterialTheme.colorScheme.primary to R.string.status_searching
        is PodsConnection.Live -> LocalBatteryColors.current.good to R.string.status_connected
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Disconnected and searching: a large tinted glyph with the message and a hint. */
@Composable
private fun IdleState(icon: ImageVector, messageRes: Int, hintRes: Int, isPulsing: Boolean = false) {
    val alpha = if (isPulsing) {
        rememberInfiniteTransition(label = "searching").animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1_100), RepeatMode.Reverse),
            label = "searchingAlpha",
        ).value
    } else {
        1f
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(52.dp).graphicsLayer { this.alpha = alpha },
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(hintRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp),
        )
    }
}

@Composable
private fun LiveReadout(state: PodsState, lastCase: LastCaseReading?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DeviceChip(PodsModel.displayName(state.model))

        Spacer(Modifier.height(36.dp))

        // The pair, at the largest scale on screen.
        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PodGauge(
                label = stringResource(R.string.pod_left),
                icon = AppIcons.PodLeft,
                level = state.leftBattery,
                isCharging = state.isLeftCharging,
            )
            PodGauge(
                label = stringResource(R.string.pod_right),
                icon = AppIcons.PodRight,
                level = state.rightBattery,
                isCharging = state.isRightCharging,
            )
        }

        Spacer(Modifier.height(40.dp))

        // Case: same information, visibly subordinate. When the pods are out
        // of the case the beacon cannot know its level, so show the last one
        // we saw, dimmed and dated, rather than a dash.
        CaseCard(
            level = state.caseBattery,
            isCharging = state.isCaseCharging,
            isLidOpen = state.isLidOpen,
            remembered = lastCase.takeIf { state.caseBattery == null },
        )
    }
}

@Composable
private fun DeviceChip(name: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Icon(AppIcons.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text = name, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** One bud: ring gauge around its glyph, the number beneath, the label last. */
@Composable
private fun PodGauge(label: String, icon: ImageVector, level: Int?, isCharging: Boolean) {
    val color = batteryColor(level)
    val description = readingDescription(label, level, isCharging)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        BatteryRing(level = level, color = color, diameter = 128.dp, stroke = 9.dp) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(52.dp), tint = color)
            if (isCharging) ChargingBadge(Modifier.align(Alignment.BottomEnd))
        }
        Spacer(Modifier.height(14.dp))
        LevelText(level = level, color = color, numberSize = 40.sp)
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.5.sp,
        )
    }
}

@Composable
private fun CaseCard(
    level: Int?,
    isCharging: Boolean,
    isLidOpen: Boolean,
    remembered: LastCaseReading?,
) {
    val label = stringResource(R.string.pod_case)
    val isLive = level != null
    val shown = level ?: remembered?.level
    val isStale = !isLive && remembered != null
    val ageText = remembered?.let { relativeAge(it) }
    val lastSeen = ageText?.let { stringResource(R.string.case_last_seen, it) }
    val showLidOpen = isLidOpen && isLive
    val showCharging = isCharging && isLive
    val color = if (isStale) MaterialTheme.colorScheme.onSurfaceVariant else batteryColor(shown)
    val description = readingDescription(label, shown, showCharging) +
        (if (showLidOpen) ", lid open" else "") +
        (if (isStale && lastSeen != null) ", $lastSeen" else "")

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = CASE_CARD_ALPHA),
        modifier = Modifier
            .widthIn(max = 340.dp)
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            BatteryRing(level = shown, color = color, diameter = 64.dp, stroke = 6.dp) {
                Icon(
                    if (showLidOpen) AppIcons.CaseOpen else AppIcons.CaseClosed,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = color,
                )
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.5.sp,
                    )
                    if (showLidOpen) Tag(text = stringResource(R.string.lid_open))
                    if (showCharging) Tag(text = stringResource(R.string.charging), icon = AppIcons.Bolt)
                }
                Spacer(Modifier.height(2.dp))
                LevelText(level = shown, color = color, numberSize = 30.sp)
                if (isStale && lastSeen != null) {
                    Text(
                        text = lastSeen,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** "80" with a small "%" on the baseline, or a dash when unknown. */
@Composable
private fun LevelText(level: Int?, color: Color, numberSize: TextUnit) {
    Row {
        Text(
            text = level?.toString() ?: stringResource(R.string.battery_unknown),
            fontSize = numberSize,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.alignByBaseline(),
        )
        if (level != null) {
            Text(
                text = "%",
                style = MaterialTheme.typography.titleMedium,
                color = color,
                modifier = Modifier.alignByBaseline().padding(start = 2.dp),
            )
        }
    }
}

/** Small tonal pill for a state word: "Lid open", "Charging". */
@Composable
private fun Tag(text: String, icon: ImageVector? = null) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp))
            Text(text = text, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ChargingBadge(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
        Alignment.Center,
    ) {
        Icon(
            AppIcons.Bolt,
            contentDescription = stringResource(R.string.charging),
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/**
 * A ring gauge that animates between readings.
 *
 * Beacons arrive in 10% steps and irregularly, so an unanimated ring would
 * visibly jump. Only the sweep and the colour animate, and it is drawn on a
 * Canvas rather than composed from shapes, so it stays cheap.
 */
@Composable
private fun BatteryRing(
    level: Int?,
    color: Color,
    diameter: Dp,
    stroke: Dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val sweep by animateFloatAsState(
        targetValue = (level ?: 0) / 100f * FULL_SWEEP_DEGREES,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "ringSweep",
    )
    val ringColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(durationMillis = 400),
        label = "ringColor",
    )
    val track = MaterialTheme.colorScheme.surfaceVariant

    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
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
                    color = ringColor,
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

private fun readingDescription(label: String, level: Int?, isCharging: Boolean): String =
    buildString {
        append(label)
        append(if (level != null) ", $level percent" else ", level unknown")
        if (isCharging) append(", charging")
    }

private const val AGE_REFRESH_MS = 60_000L
private const val FULL_SWEEP_DEGREES = 360f
/** Twelve o'clock; Canvas angles start at three o'clock. */
private const val RING_START_DEGREES = -90f
private const val WASH_ALPHA = 0.55f
/** How far down the screen the palette wash reaches before it has faded out. */
private const val WASH_EXTENT = 0.55f
private const val CASE_CARD_ALPHA = 0.6f
