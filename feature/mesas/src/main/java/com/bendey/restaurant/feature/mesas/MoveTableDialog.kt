package com.bendey.restaurant.feature.mesas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.restaurant.RestaurantTable
import com.bendey.restaurant.core.ui.components.BendeyFormDialog

@Composable
fun MoveTableDialog(
    sourceTable: RestaurantTable,
    freeTables: List<RestaurantTable>,
    loadingFree: Boolean,
    selectedTargetId: Int?,
    submitting: Boolean,
    onSelectTarget: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val floorSuffix = sourceTable.floorName?.let { " · $it" }.orEmpty()
    BendeyFormDialog(
        onDismissRequest = {
            if (!submitting) onDismiss()
        },
        title = "Mover mesa",
        subtitle = "Mesa actual: ${sourceTable.name}$floorSuffix",
        confirmText = if (submitting) "Moviendo…" else "Mover",
        confirmEnabled = selectedTargetId != null && !submitting && !loadingFree,
        loading = submitting,
        onConfirm = onConfirm,
        onDismiss = {
            if (!submitting) onDismiss()
        },
    ) {
        Text(
            text = "Elegir mesa destino",
            style = MaterialTheme.typography.labelMedium,
            color = BendeyColors.OnSurfaceVariant,
        )
        when {
            loadingFree -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = BendeySpacing.xl),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            freeTables.isEmpty() -> {
                Text(
                    text = "No hay mesas libres disponibles.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(BendeyShapeTokens.md)
                        .border(1.dp, BendeyColors.Outline.copy(alpha = 0.5f), BendeyShapeTokens.md)
                        .padding(BendeySpacing.md),
                    style = MaterialTheme.typography.bodyMedium,
                    color = BendeyColors.OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 224.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
                ) {
                    freeTables.forEach { table ->
                        val selected = selectedTargetId == table.id
                        val borderColor = if (selected) BendeyColors.Primary else BendeyColors.Outline.copy(alpha = 0.65f)
                        val background = if (selected) BendeyColors.PrimaryContainer else BendeyColors.Surface
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .clip(BendeyShapeTokens.md)
                                .border(1.dp, borderColor, BendeyShapeTokens.md)
                                .background(background)
                                .clickable(enabled = !submitting) { onSelectTarget(table.id) }
                                .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = table.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = table.floorName ?: "Sin sala",
                                style = MaterialTheme.typography.labelSmall,
                                color = BendeyColors.OnSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
