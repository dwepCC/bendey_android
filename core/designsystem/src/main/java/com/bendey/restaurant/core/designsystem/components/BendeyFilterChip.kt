package com.bendey.restaurant.core.designsystem.components

import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bendey.restaurant.core.designsystem.previews.BendeyPhonePreview
import com.bendey.restaurant.core.designsystem.previews.BendeyPreviewSurface
import com.bendey.restaurant.core.designsystem.previews.BendeyTabletPreview
import com.bendey.restaurant.core.designsystem.theme.BendeyChipDefaults
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens

enum class BendeyFilterChipVariant {
    Admin,
    Pos,
}

@Composable
fun BendeyFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    variant: BendeyFilterChipVariant = BendeyFilterChipVariant.Admin,
) {
    val colors = when (variant) {
        BendeyFilterChipVariant.Admin -> BendeyChipDefaults.filterChipColors()
        BendeyFilterChipVariant.Pos -> BendeyChipDefaults.posFilterChipColors()
    }
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        colors = colors,
        shape = BendeyShapeTokens.chip,
        border = null,
    )
}

@Composable
fun BendeyFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: BendeyFilterChipVariant = BendeyFilterChipVariant.Admin,
) {
    BendeyFilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        variant = variant,
        label = {
            Text(text, style = MaterialTheme.typography.labelLarge)
        },
    )
}

@BendeyPhonePreview
@Composable
private fun BendeyFilterChipPhonePreview() {
    BendeyPreviewSurface {
        BendeyFilterChip(selected = true, onClick = {}, text = "Hoy")
    }
}

@BendeyTabletPreview
@Composable
private fun BendeyFilterChipTabletPreview() {
    BendeyPreviewSurface {
        BendeyFilterChip(selected = false, onClick = {}, text = "Semana")
    }
}
