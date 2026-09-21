package com.ovalvoi.andropods.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ovalvoi.andropods.R
import com.ovalvoi.andropods.ble.PodsModel
import com.ovalvoi.andropods.ble.PodsState
import com.ovalvoi.andropods.data.LastCaseReading
import com.ovalvoi.andropods.data.PopupPosition
import com.ovalvoi.andropods.ui.AppIcons
import com.ovalvoi.andropods.ui.components.LevelText
import com.ovalvoi.andropods.ui.theme.Dimens
import com.ovalvoi.andropods.ui.theme.batteryColor
import com.ovalvoi.andropods.ui.theme.caseColor

/**
 * The connection card: what the popup shows when the AirPods connect.
 *
 * A condensed cousin of the main readout rather than a copy of it. This is
 * glanced at over whatever app is already on screen, so it trades the large
 * ring gauges for three compact tiles that read in one pass, while drawing on
 * the same surfaces, radii and colours as the rest of the app.
 */
@Composable
fun ConnectPopupCard(
    state: PodsState,
    lastCase: LastCaseReading?,
    position: PopupPosition,
    visibleState: MutableTransitionState<Boolean>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    /** The user's own name for the pods; falls back to the model name. */
    deviceName: String? = null,
) {
    // Slide from the edge the card is anchored to, so it reads as arriving
    // from off-screen rather than growing out of the middle.
    val slideFrom = if (position == PopupPosition.TOP) -1 else 1

    AnimatedVisibility(
        // A MutableTransitionState rather than a plain Boolean: the host
        // creates this composition and shows the card in the same frame, so a
        // plain `visible = true` would compose already-visible and skip the
        // enter animation entirely.
        visibleState = visibleState,
        // A low-stiffness spring rather than a tween: a duration-based ease
        // arrives at full speed and stops dead, which reads as harsh.
        enter = slideInVertically(
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessVeryLow,
                visibilityThreshold = IntOffset.VisibilityThreshold,
            )
        ) { full -> full * slideFrom } +
            fadeIn(tween(FADE_IN_MILLIS, easing = LinearOutSlowInEasing)),
        exit = slideOutVertically(tween(EXIT_MILLIS)) { full ->
            full * slideFrom
        } + fadeOut(tween(EXIT_MILLIS)),
        modifier = modifier,
    ) {
        Surface(
            shape = RoundedCornerShape(Dimens.CornerCard),
            color = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(Dimens.BorderHairline, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 0.dp,
            // No shadowElevation. In an overlay window the shadow is drawn
            // into a translucent surface with nothing behind it to fall on,
            // so it rendered as a hard grey band along the card's edges
            // rather than as depth -- obvious in light mode, invisible in
            // dark. The hairline border carries the separation instead.
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier
                    .cardGlow(
                        color = MaterialTheme.colorScheme.primary,
                        fromTop = isDarkTheme(),
                    )
                    .padding(Dimens.CardPadding),
            ) {
                PopupHeader(
                    title = deviceName ?: stringResource(R.string.popup_connected_title),
                    subtitle = PodsModel.displayName(state.model),
                    onDismiss = onDismiss,
                )
                Spacer(Modifier.height(Dimens.ItemGap))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.TightGap),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    LevelTile(
                        label = stringResource(R.string.pod_left),
                        icon = AppIcons.PodLeft,
                        level = state.leftBattery,
                        isCharging = state.isLeftCharging,
                        modifier = Modifier.weight(1f),
                    )
                    LevelTile(
                        label = stringResource(R.string.pod_right),
                        icon = AppIcons.PodRight,
                        level = state.rightBattery,
                        isCharging = state.isRightCharging,
                        modifier = Modifier.weight(1f),
                    )
                    // Out of the case the beacon cannot report its level, so
                    // fall back to the remembered one, marked with a "~".
                    LevelTile(
                        label = stringResource(R.string.pod_case),
                        icon = if (state.isLidOpen && state.caseBattery != null) {
                            AppIcons.CaseOpen
                        } else {
                            AppIcons.CaseClosed
                        },
                        level = state.caseBattery ?: lastCase?.level,
                        isCharging = state.isCaseCharging && state.caseBattery != null,
                        isRemembered = state.caseBattery == null && lastCase != null,
                        isCase = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** True when the app is currently drawing dark. */
@Composable
private fun isDarkTheme(): Boolean =
    MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE_LUMINANCE

/**
 * A soft wash of the palette across the card.
 *
 * Static, not animated: the pulse on the chip is this card's motion, and a
 * second moving thing beside it competes rather than adds. This only has to
 * keep the card from reading as a flat slab.
 *
 * @param fromTop which edge the colour gathers at. On a dark card it sits at
 *   the top, behind the header, so the card looks lit from above. On a pale
 *   card that reads as a heavy band across the title, so the tint drops to the
 *   bottom instead and the light falls from the top -- the same lighting, read
 *   correctly for each background.
 */
@Composable
private fun Modifier.cardGlow(color: Color, fromTop: Boolean): Modifier = this.drawBehind {
    val tint = color.copy(alpha = CARD_GLOW_ALPHA)
    drawRect(
        if (fromTop) {
            Brush.verticalGradient(
                colors = listOf(tint, Color.Transparent),
                endY = size.height * CARD_GLOW_EXTENT,
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(Color.Transparent, tint),
                startY = size.height * (1f - CARD_GLOW_EXTENT),
            )
        }
    )
}

@Composable
private fun PopupHeader(title: String, subtitle: String, onDismiss: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ConnectingPulse()
        Spacer(Modifier.width(Dimens.ItemGap))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontSize = SUBTITLE_SIZE,
                // On a card this quiet, the subtitle is one of the few places
                // the palette actually shows.
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            onClick = onDismiss,
            modifier = Modifier.size(Dimens.TouchTarget),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.popup_dismiss),
                    modifier = Modifier.size(Dimens.IconChip),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The Bluetooth chip, with rings radiating out of it.
 *
 * The popup otherwise just appears, which does not by itself say *what*
 * happened. A pulse expanding from the Bluetooth mark reads as a link being
 * made, so the card announces a connection rather than only reporting numbers.
 *
 * Two rings offset by half the cycle, so one is always mid-flight and the
 * effect never visibly stops and restarts. Each fades as it grows, which is
 * what keeps it a pulse instead of a spinner.
 */
@Composable
private fun ConnectingPulse() {
    val transition = rememberInfiniteTransition(label = "connectingPulse")
    val accent = MaterialTheme.colorScheme.primary

    @Composable
    fun wave(offsetMillis: Int) = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = PULSE_PERIOD_MILLIS + offsetMillis
                0f at 0 using LinearOutSlowInEasing
                0f at offsetMillis
                1f at PULSE_PERIOD_MILLIS + offsetMillis
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave$offsetMillis",
    ).value

    val first = wave(0)
    val second = wave(PULSE_PERIOD_MILLIS / 2)

    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(CHIP_TOUCH_SIZE)) {
            listOf(first, second).forEach { progress ->
                if (progress <= 0f) return@forEach
                val radius = size.minDimension / 2 * (PULSE_START + progress * PULSE_GROWTH)
                drawCircle(
                    color = accent.copy(alpha = (1f - progress) * PULSE_ALPHA),
                    radius = radius,
                    style = Stroke(width = PULSE_STROKE.toPx()),
                )
            }
        }
        Surface(shape = CircleShape, color = accent, modifier = Modifier.size(Dimens.ChipButton)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    AppIcons.Bluetooth,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.IconChip),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

/**
 * One component's reading: glyph, number, label, stacked in a tile.
 *
 * @param isRemembered render as the last known value, prefixed with "~"
 *   rather than shown as a plain number, so a stale case level cannot be
 *   mistaken for a fresh reading.
 * @param isCase the case takes its own pink rather than a battery band.
 */
@Composable
private fun LevelTile(
    label: String,
    icon: ImageVector,
    level: Int?,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
    isRemembered: Boolean = false,
    isCase: Boolean = false,
) {
    val color = if (isCase) caseColor(level) else batteryColor(level)
    val description = buildString {
        append(label)
        append(if (level != null) ", $level percent" else ", level unknown")
        if (isRemembered) append(", last known")
        if (isCharging) append(", charging")
    }

    Surface(
        shape = RoundedCornerShape(Dimens.CornerTile),
        // A step down the surface ladder plus a hairline. Opaque on purpose:
        // a translucent tile lets the header glow through and washes out the
        // number, which is the one thing this card exists to show.
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(Dimens.BorderHairline, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(Dimens.TilePadding),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = color)
                if (isCharging) {
                    Icon(
                        AppIcons.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            LevelText(
                level = level,
                color = color,
                numberSize = TILE_NUMBER_SIZE,
                prefix = if (isRemembered) "~" else "",
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontSize = TILE_LABEL_SIZE,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp,
            )
        }
    }
}

private const val FADE_IN_MILLIS = 420
private const val EXIT_MILLIS = 220
private val SUBTITLE_SIZE = 10.sp
private val TILE_NUMBER_SIZE = 19.sp
private val TILE_LABEL_SIZE = 9.sp
private const val CARD_GLOW_ALPHA = 0.30f
/** How far across the card the glow reaches before it has faded out. */
private const val CARD_GLOW_EXTENT = 0.75f
/** Below this the card counts as dark. */
private const val DARK_SURFACE_LUMINANCE = 0.35f
/** One ring's flight time; the second is launched half a cycle behind it. */
private const val PULSE_PERIOD_MILLIS = 1_600
/** Ring radius as a fraction of the chip, from just outside it to well clear. */
private const val PULSE_START = 0.42f
private const val PULSE_GROWTH = 0.58f
private const val PULSE_ALPHA = 0.5f
private val PULSE_STROKE = 1.5.dp
private val CHIP_TOUCH_SIZE = 46.dp
