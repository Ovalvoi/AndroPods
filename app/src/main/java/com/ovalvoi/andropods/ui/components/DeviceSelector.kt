package com.ovalvoi.andropods.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ovalvoi.andropods.ui.AppIcons
import com.ovalvoi.andropods.ui.theme.Dimens

/**
 * The capsule naming the connected device.
 *
 * A translucent surface with a hairline accent border rather than a filled
 * chip: this is a label, not a control, and it should read as quieter than
 * the battery rings below it.
 */
@Composable
fun DeviceSelector(
    name: String,
    modifier: Modifier = Modifier,
    /** The model, shown under a user-assigned name. Null to show the name alone. */
    subtitle: String? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.CornerPill),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            Dimens.BorderHairline,
            MaterialTheme.colorScheme.primary.copy(alpha = BORDER_ALPHA),
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Icon(
                AppIcons.Bluetooth,
                contentDescription = null,
                modifier = Modifier.size(Dimens.IconInline),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private const val BORDER_ALPHA = 0.35f
