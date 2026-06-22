package com.bendey.restaurant.core.designsystem.theme

import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable

object BendeyChipDefaults {
    @Composable
    fun filterChipColors() = FilterChipDefaults.filterChipColors(
        selectedContainerColor = BendeyColors.PrimaryContainer,
        selectedLabelColor = BendeyColors.OnPrimaryContainer,
        containerColor = BendeyColors.SurfaceVariant,
        labelColor = BendeyColors.OnSurfaceVariant,
    )

    /** Chips operativos POS — selección con primario de marca. */
    @Composable
    fun posFilterChipColors() = FilterChipDefaults.filterChipColors(
        selectedContainerColor = BendeyColors.Primary,
        selectedLabelColor = BendeyColors.OnPrimary,
        containerColor = BendeyColors.SurfaceVariant,
        labelColor = BendeyColors.OnSurfaceVariant,
    )
}
