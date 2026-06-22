package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

private val BendeySwitchColors
    @Composable get() = SwitchDefaults.colors(
        checkedThumbColor = BendeyColors.OnPrimary,
        checkedTrackColor = BendeyColors.Primary,
        checkedBorderColor = BendeyColors.Primary,
        uncheckedThumbColor = BendeyColors.OnSurfaceVariant,
        uncheckedTrackColor = BendeyColors.Outline,
        uncheckedBorderColor = BendeyColors.OnSurfaceVariant.copy(alpha = 0.35f),
        disabledCheckedThumbColor = BendeyColors.OnPrimary.copy(alpha = 0.5f),
        disabledCheckedTrackColor = BendeyColors.Primary.copy(alpha = 0.35f),
        disabledUncheckedThumbColor = BendeyColors.OnSurfaceVariant.copy(alpha = 0.35f),
        disabledUncheckedTrackColor = BendeyColors.Outline.copy(alpha = 0.5f),
    )

@Composable
fun BendeySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = BendeySwitchColors,
    )
}

@Composable
fun BendeySwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(end = BendeySpacing.sm),
        )
        BendeySwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}
