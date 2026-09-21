package com.ovalvoi.andropods.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ovalvoi.andropods.BuildConfig
import com.ovalvoi.andropods.R
import com.ovalvoi.andropods.data.AppSettings
import com.ovalvoi.andropods.data.ColorTheme
import com.ovalvoi.andropods.data.DarkMode
import com.ovalvoi.andropods.data.PopupPosition
import com.ovalvoi.andropods.ui.overlay.ConnectPopupHost
import com.ovalvoi.andropods.ui.overlay.OverlayPermission
import com.ovalvoi.andropods.ble.PodsModel
import com.ovalvoi.andropods.ble.PodsState
import com.ovalvoi.andropods.data.LastCaseReading
import com.ovalvoi.andropods.ui.theme.Palettes

/**
 * Options. Same typographic register as the readout: eyebrow labels with a
 * small icon, no cards, the controls are the only chrome. Four sections
 * because there are four concerns.
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.settings_back),
                    )
                }
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(24.dp))

            SectionLabel(AppIcons.Palette, stringResource(R.string.settings_section_appearance))
            FieldLabel(stringResource(R.string.settings_theme))
            ThemePicker(
                selected = settings.colorTheme,
                onSelect = { theme -> onChange { it.copy(colorTheme = theme) } },
            )
            Spacer(Modifier.height(16.dp))
            FieldLabel(stringResource(R.string.settings_dark_mode))
            DarkModePicker(
                selected = settings.darkMode,
                onSelect = { mode -> onChange { it.copy(darkMode = mode) } },
            )

            Spacer(Modifier.height(32.dp))

            SectionLabel(Icons.Outlined.Notifications, stringResource(R.string.settings_section_popup))
            SwitchRow(
                title = stringResource(R.string.settings_connect_popup),
                summary = stringResource(R.string.settings_connect_popup_summary),
                checked = settings.showConnectPopup,
                onCheckedChange = { on -> onChange { it.copy(showConnectPopup = on) } },
            )
            // Only while the popup is actually wanted: the grant is useless
            // otherwise, and asking for it unprompted is the kind of thing
            // that makes people uninstall an app.
            if (settings.showConnectPopup) {
                OverlayPermissionRow()
                FieldLabel(stringResource(R.string.settings_popup_position))
                PopupPositionPicker(
                    selected = settings.popupPosition,
                    onSelect = { position -> onChange { it.copy(popupPosition = position) } },
                )
            }
            if (BuildConfig.DEBUG) PreviewPopupRow(settings)
            SwitchRow(
                title = stringResource(R.string.settings_auto_dismiss),
                summary = stringResource(R.string.settings_auto_dismiss_summary),
                checked = settings.autoDismissPopup,
                onCheckedChange = { on -> onChange { it.copy(autoDismissPopup = on) } },
            )
            TimeoutChoiceRow(
                enabled = settings.autoDismissPopup,
                selectedSeconds = settings.popupTimeoutSeconds,
                onSelect = { s -> onChange { it.copy(popupTimeoutSeconds = s) } },
            )

            Spacer(Modifier.height(32.dp))

            SectionLabel(AppIcons.MusicNote, stringResource(R.string.settings_section_audio))
            SwitchRow(
                title = stringResource(R.string.settings_resume_music),
                summary = stringResource(R.string.settings_resume_music_summary),
                checked = settings.resumeMusicOnConnect,
                onCheckedChange = { on -> onChange { it.copy(resumeMusicOnConnect = on) } },
            )

            Spacer(Modifier.height(32.dp))

            SectionLabel(AppIcons.TouchApp, stringResource(R.string.settings_section_gestures))
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = stringResource(R.string.settings_gestures_title),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_gestures_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.5.sp,
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/**
 * One swatch per palette, showing its light and dark accent.
 *
 * Every palette now has fixed colours of its own -- including Default, which
 * is the neutral colourway rather than "whatever the wallpaper says" -- so
 * they all render the same way.
 */
@Composable
private fun ThemePicker(selected: ColorTheme, onSelect: (ColorTheme) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        ColorTheme.entries.forEach { theme ->
            ThemeSwatch(
                theme = theme,
                isSelected = theme == selected,
                onClick = { onSelect(theme) },
            )
        }
    }
}

@Composable
private fun ThemeSwatch(theme: ColorTheme, isSelected: Boolean, onClick: () -> Unit) {
    val name = stringResource(theme.nameRes())
    val colors = Palettes.swatch(theme)
    val brush = Brush.linearGradient(colors)
    val ring = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
    ) {
        Box(
            Modifier
                .size(SWATCH_SIZE)
                .border(3.dp, ring, CircleShape)
                .padding(5.dp)
                .clip(CircleShape)
                .background(brush),
            Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = Color.White,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/**
 * Prompts for the overlay grant, and disappears once it is held.
 *
 * Re-checked on every resume rather than once: the grant is given on a
 * Settings page in another task, so the only reliable moment to learn it
 * changed is coming back to this screen.
 */
@Composable
private fun OverlayPermissionRow() {
    val context = LocalContext.current
    var isGranted by remember { mutableStateOf(OverlayPermission.isGranted(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) isGranted = OverlayPermission.isGranted(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (isGranted) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_overlay_permission),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.settings_overlay_permission_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        TextButton(onClick = { context.startActivity(OverlayPermission.requestIntent(context)) }) {
            Text(stringResource(R.string.settings_overlay_grant))
        }
    }
}

/**
 * Debug-only: renders the popup on demand with sample levels.
 *
 * Shown with the *live* settings object, so the preview is exactly what a real
 * connect would draw -- theme, position and dismiss timeout included. Tweaking
 * a colour and seeing it otherwise costs a disconnect/reconnect of the pods.
 */
@Composable
private fun PreviewPopupRow(settings: AppSettings) {
    val context = LocalContext.current
    val host = remember(context) { ConnectPopupHost(context.applicationContext) }
    // The window must come down with the screen that opened it; a preview left
    // attached would outlive Settings and float over everything.
    DisposableEffect(host) { onDispose { host.destroy() } }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_preview_popup),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.settings_preview_popup_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        TextButton(
            onClick = {
                if (OverlayPermission.isGranted(context)) {
                    host.show(PREVIEW_STATE, PREVIEW_CASE, settings)
                } else {
                    context.startActivity(OverlayPermission.requestIntent(context))
                }
            },
        ) {
            Text(stringResource(R.string.settings_preview_show))
        }
    }
}

/** Levels chosen to span all three battery colour bands at once. */
private val PREVIEW_STATE = PodsState(
    model = PodsModel.AIRPODS_GEN_2,
    leftBattery = 80,
    rightBattery = 35,
    caseBattery = null,
    isLeftCharging = false,
    isRightCharging = true,
    isCaseCharging = false,
    isLidOpen = false,
    lidOpenCounter = 0,
    rawLeftInEar = true,
    rawRightInEar = true,
)

/** Null case level above, so this also exercises the remembered path. */
private val PREVIEW_CASE = LastCaseReading(level = 15, isCharging = false, seenAtMs = 0L)

@Composable
private fun PopupPositionPicker(selected: PopupPosition, onSelect: (PopupPosition) -> Unit) {
    val positions = PopupPosition.entries
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        positions.forEachIndexed { index, position ->
            SegmentedButton(
                selected = position == selected,
                onClick = { onSelect(position) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = positions.size),
                label = { Text(stringResource(position.nameRes())) },
            )
        }
    }
}

private fun PopupPosition.nameRes(): Int = when (this) {
    PopupPosition.TOP -> R.string.popup_position_top
    PopupPosition.BOTTOM -> R.string.popup_position_bottom
}

@Composable
private fun DarkModePicker(selected: DarkMode, onSelect: (DarkMode) -> Unit) {
    val modes = DarkMode.entries
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        modes.forEachIndexed { index, mode ->
            val isSelected = mode == selected
            SegmentedButton(
                selected = isSelected,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                icon = {
                    SegmentedButtonDefaults.Icon(active = isSelected) {
                        Icon(
                            mode.icon(),
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                        )
                    }
                },
                label = { Text(stringResource(mode.nameRes())) },
            )
        }
    }
}

private fun ColorTheme.nameRes(): Int = when (this) {
    ColorTheme.SYSTEM -> R.string.theme_system
    ColorTheme.OCEAN -> R.string.theme_ocean
    ColorTheme.SUNSET -> R.string.theme_sunset
    ColorTheme.FOREST -> R.string.theme_forest
    ColorTheme.GRAPE -> R.string.theme_grape
}

private fun DarkMode.nameRes(): Int = when (this) {
    DarkMode.SYSTEM -> R.string.dark_mode_system
    DarkMode.LIGHT -> R.string.dark_mode_light
    DarkMode.DARK -> R.string.dark_mode_dark
}

private fun DarkMode.icon(): ImageVector = when (this) {
    DarkMode.SYSTEM -> AppIcons.BrightnessAuto
    DarkMode.LIGHT -> AppIcons.Sun
    DarkMode.DARK -> AppIcons.Moon
}

@Composable
private fun SwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(2.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        // The row is the toggle; the switch is its indicator.
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun TimeoutChoiceRow(enabled: Boolean, selectedSeconds: Int, onSelect: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_popup_timeout),
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        AppSettings.POPUP_TIMEOUT_CHOICES_SECONDS.forEach { seconds ->
            FilterChip(
                selected = seconds == selectedSeconds,
                enabled = enabled,
                onClick = { onSelect(seconds) },
                label = { Text(stringResource(R.string.settings_seconds, seconds)) },
            )
        }
    }
}

private val SWATCH_SIZE = 52.dp
