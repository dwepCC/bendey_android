package com.bendey.restaurant.feature.productos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.domain.catalog.BranchItem
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyOption
import com.bendey.restaurant.core.ui.components.BendeySimpleSelect
import com.bendey.restaurant.core.ui.components.BendeyTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

data class StockAdjustmentForm(
    val productId: Int,
    val productName: String,
    val branchId: Int? = null,
    val isIncrease: Boolean = true,
    val quantity: String = "1",
    val notes: String = "",
    /**
     * Productos con stock por presentación: cada una lleva su propio stock, así que
     * el ajuste tiene que decir a cuál va. Vacío = el producto no las usa para stock.
     */
    val presentations: List<StockAdjustmentPresentation> = emptyList(),
    val presentationId: Int? = null,
)

data class StockAdjustmentPresentation(val id: Int, val name: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StockAdjustmentDialog(
    form: StockAdjustmentForm,
    branches: List<BranchItem>,
    loading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onFormChange: ((StockAdjustmentForm) -> StockAdjustmentForm) -> Unit,
    onConfirm: () -> Unit,
) {
    val branchOptions = branches.map { BendeyOption(it.id.toString(), it.name) }
    BendeyFormDialog(
        onDismissRequest = onDismiss,
        title = "Ajuste de stock",
        confirmText = "Guardar",
        loading = loading,
        // Sin presentación elegida el servidor rechaza el ajuste: se bloquea acá para
        // no hacer perder el formulario lleno.
        confirmEnabled = !loading &&
            form.notes.trim().isNotEmpty() &&
            (form.presentations.isEmpty() || form.presentationId != null),
        onConfirm = onConfirm,
        enableContentScroll = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = form.productName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (branchOptions.isNotEmpty()) {
                BendeySimpleSelect(
                    options = branchOptions,
                    selectedValue = form.branchId?.toString().orEmpty(),
                    onSelect = { value ->
                        onFormChange { it.copy(branchId = value.toIntOrNull()) }
                    },
                    label = "Sucursal",
                )
            }
            if (form.presentations.isNotEmpty()) {
                BendeySimpleSelect(
                    options = form.presentations.map { BendeyOption(it.id.toString(), it.name) },
                    selectedValue = form.presentationId?.toString().orEmpty(),
                    onSelect = { value ->
                        onFormChange { it.copy(presentationId = value.toIntOrNull()) }
                    },
                    label = "Presentación",
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tipo de ajuste",
                    style = MaterialTheme.typography.labelMedium,
                    color = BendeyColors.OnSurfaceVariant,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BendeyFilterChip(
                        selected = form.isIncrease,
                        onClick = { onFormChange { it.copy(isIncrease = true) } },
                        text = "Aumentar stock",
                    )
                    BendeyFilterChip(
                        selected = !form.isIncrease,
                        onClick = { onFormChange { it.copy(isIncrease = false) } },
                        text = "Disminuir stock",
                    )
                }
            }
            BendeyTextField(
                value = form.quantity,
                onValueChange = { value -> onFormChange { it.copy(quantity = value) } },
                label = "Cantidad",
            )
            BendeyTextField(
                value = form.notes,
                onValueChange = { value -> onFormChange { it.copy(notes = value) } },
                label = "Motivo del ajuste *",
                singleLine = false,
            )
            error?.let {
                Text(
                    text = it,
                    color = BendeyColors.Error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
