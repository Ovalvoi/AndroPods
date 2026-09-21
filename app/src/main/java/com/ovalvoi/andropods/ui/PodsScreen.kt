package com.ovalvoi.andropods.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ovalvoi.andropods.R
import com.ovalvoi.andropods.ble.PodsModel
import com.ovalvoi.andropods.ble.PodsState
import com.ovalvoi.andropods.data.LastCaseReading
import com.ovalvoi.andropods.data.PodsConnection
import com.ovalvoi.andropods.ui.components.CaseBatteryCard
import com.ovalvoi.andropods.ui.components.DeviceSelector
import com.ovalvoi.andropods.ui.components.EarbudBatteryCard
import com.ovalvoi.andropods.ui.components.StatusIndicator
import com.ovalvoi.andropods.ui.theme.Dimens
import com.ovalvoi.andropods.ui.theme.LocalBatteryColors
import kotlinx.coroutines.delay

/**
 * The readout.
 *
 * Hierarchy is carried by scale: the two buds are the primary read, each in a
 * large ring with its glyph, and the case sits below in a quieter card.
 *
 * Colour is split by role. The palette owns the chrome -- the ambient wash,
 * the gear, the device capsule -- while the battery colours are semantic and
 * identical in every colourway, because a red 10% has to read as red whichever
 * theme is on. Neither state is signalled by colour alone; the number and its
 * label always say the same thing in text.
 */
@Composable
fun PodsScreen(
    connection: PodsConnection,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    /** The user's own name for the pods; falls back to the model name. */
    deviceName: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier
            .fillMaxSize()
            .background(scheme.background)
            .ambientGlow(scheme.primary),
    ) {
        // Scrollable so the content survives a short screen, a large font
        // scale, or landscape, rather than being clipped.
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Header(connection, onOpenSettings)

            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = Dimens.ScreenGutter),
                contentAlignment = Alignment.Center,
            ) {
                // Sized from the space actually available rather than from a
                // device assumption, so a small phone shrinks the rings
                // instead of pushing the case card off-screen.
                val isCompact = maxHeight < Dimens.CompactHeightThreshold
                val ringDiameter =
                    if (isCompact) Dimens.RingDiameterMin else Dimens.RingDiameterMax

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

                    is PodsConnection.Live -> LiveReadout(
                        state = connection.state,
                        lastCase = connection.lastCase,
                        ringDiameter = ringDiameter,
                        isCompact = isCompact,
                        deviceName = deviceName,
                    )
                }
            }

            Spacer(Modifier.height(Dimens.SectionGap))
        }
    }
}

/**
 * A soft wash of the palette behind everything.
 *
 * Two offset radial pools rather than one linear gradient: a vertical ramp
 * reads as a banded background, while overlapping pools read as light in a
 * room. Kept faint enough that the battery colours stay the brightest thing
 * on the screen.
 */
private fun Modifier.ambientGlow(accent: Color) = drawBehind {
    val radius = size.width * GLOW_RADIUS_FRACTION
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = GLOW_ALPHA_TOP), Color.Transparent),
            center = Offset(size.width * 0.18f, 0f),
            radius = radius,
        ),
        radius = radius,
        center = Offset(size.width * 0.18f, 0f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = GLOW_ALPHA_SIDE), Color.Transparent),
            center = Offset(size.width, size.height * 0.30f),
            radius = radius,
        ),
        radius = radius,
        center = Offset(size.width, size.height * 0.30f),
    )
}

@Composable
private fun Header(connection: PodsConnection, onOpenSettings: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val (dotColor, statusRes, isPulsing) = when (connection) {
        is PodsConnection.Disconnected ->
            Triple(scheme.outline, R.string.status_disconnected, false)
        is PodsConnection.Searching ->
            Triple(scheme.primary, R.string.status_searching, true)
        is PodsConnection.Live ->
            Triple(LocalBatteryColors.current.good, R.string.status_connected, true)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = Dimens.ScreenGutter,
                end = Dimens.TightGap,
                top = Dimens.SectionGap,
                bottom = Dimens.SectionGap,
            ),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Dimens.TightGap))
            StatusIndicator(
                label = stringResource(statusRes),
                color = dotColor,
                isPulsing = isPulsing,
            )
        }
        IconButton(onClick = onOpenSettings, modifier = Modifier.size(Dimens.TouchTarget)) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.settings_open),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun LiveReadout(
    state: PodsState,
    lastCase: LastCaseReading?,
    ringDiameter: androidx.compose.ui.unit.Dp,
    isCompact: Boolean,
    deviceName: String?,
) {
    val numberSize = if (isCompact) COMPACT_NUMBER_SIZE else NUMBER_SIZE

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = CONTENT_MAX_WIDTH).fillMaxWidth(),
    ) {
        // The user's own name identifies *whose* pods these are, which the
        // model name cannot: every Gen 2 looks alike on the air. The model
        // stays as the subtitle, and as the fallback when no name is set.
        DeviceSelector(
            name = deviceName ?: PodsModel.displayName(state.model),
            subtitle = PodsModel.displayName(state.model).takeIf { deviceName != null },
        )

        Spacer(Modifier.height(if (isCompact) Dimens.SectionGap else SECTION_GAP_WIDE))

        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EarbudBatteryCard(
                label = stringResource(R.string.pod_left),
                icon = AppIcons.PodLeft,
                level = state.leftBattery,
                isCharging = state.isLeftCharging,
                ringDiameter = ringDiameter,
                numberSize = numberSize,
            )
            EarbudBatteryCard(
                label = stringResource(R.string.pod_right),
                icon = AppIcons.PodRight,
                level = state.rightBattery,
                isCharging = state.isRightCharging,
                ringDiameter = ringDiameter,
                numberSize = numberSize,
            )
        }

        Spacer(Modifier.height(if (isCompact) Dimens.SectionGap else SECTION_GAP_WIDE))

        // Out of the case the beacon cannot report its level, so fall back to
        // the last reading, marked and dated rather than shown as a dash.
        val isLive = state.caseBattery != null
        val remembered = lastCase.takeIf { !isLive }
        CaseBatteryCard(
            level = state.caseBattery ?: remembered?.level,
            isCharging = state.isCaseCharging && isLive,
            isLidOpen = state.isLidOpen && isLive,
            remembered = remembered?.let { stringResource(R.string.case_last_seen, relativeAge(it)) },
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = Dimens.SectionGap),
    ) {
        Box(
            Modifier
                .size(IDLE_GLYPH_BOX)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(IDLE_GLYPH).graphicsLayer { this.alpha = alpha },
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(Dimens.SectionGap))
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Dimens.ItemGap))
        Text(
            text = stringResource(hintRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = HINT_MAX_WIDTH),
        )
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

private const val AGE_REFRESH_MS = 60_000L
private val NUMBER_SIZE = 42.sp
private val COMPACT_NUMBER_SIZE = 32.sp
private val SECTION_GAP_WIDE = 40.dp
/** Keeps the readout from stretching on a tablet or an unfolded device. */
private val CONTENT_MAX_WIDTH = 420.dp
private val HINT_MAX_WIDTH = 300.dp
private val IDLE_GLYPH_BOX = 112.dp
private val IDLE_GLYPH = 52.dp
private const val GLOW_RADIUS_FRACTION = 0.95f
private const val GLOW_ALPHA_TOP = 0.16f
private const val GLOW_ALPHA_SIDE = 0.10f
