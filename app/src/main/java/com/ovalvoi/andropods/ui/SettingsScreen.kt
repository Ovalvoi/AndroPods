package com.ovalvoi.andropods.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ovalvoi.andropods.R
import com.ovalvoi.andropods.data.AppSettings

/**
 * Options. Same typographic register as the readout: eyebrow labels, no
 * cards, the switch is the only chrome. Three sections because there are
 * three concerns, not because a settings screen "should" have sections.
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
                )
            }

            Spacer(Modifier.height(24.dp))

            SectionLabel(stringResource(R.string.settings_section_popup))
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

            SectionLabel(stringResource(R.string.settings_section_audio))
            SwitchRow(
                title = stringResource(R.string.settings_resume_music),
                summary = stringResource(R.string.settings_resume_music_summary),
                checked = settings.resumeMusicOnConnect,
                onCheckedChange = { on -> onChange { it.copy(resumeMusicOnConnect = on) } },
            )

            Spacer(Modifier.height(32.dp))

            SectionLabel(stringResource(R.string.settings_section_gestures))
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
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    )
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
