package com.bendey.restaurant.feature.productos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.products.ProductType
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeySearchableSelect
import com.bendey.restaurant.core.ui.components.BendeySelectOption
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import java.text.NumberFormat
import java.util.Locale

/** Editor de receta de un producto elaborado. No persiste en el backend: arma un RecipeDraft
 * local que el formulario de producto guarda junto con internal/production recién al confirmar
 * el resto del formulario (ver RecipeEditorViewModel y ProductosViewModel.setRecipeDraft). */
@Composable
fun RecipeEditorSheet(
    productId: Int,
    productName: String,
    initialDraft: RecipeDraft?,
    onDismiss: () -> Unit,
    onSave: (RecipeDraft) -> Unit,
    viewModel: RecipeEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(productId) {
        viewModel.open(productId, productName, initialDraft)
    }

    val ingredientOptions = state.ingredientOptions.map {
        val tag = if (it.productType == ProductType.INSUMO) " · insumo" else ""
        BendeySelectOption(it.id, "${it.name}$tag (${it.unit})")
    }

    BendeyFormDialog(
        onDismissRequest = onDismiss,
        title = "Receta",
        subtitle = productName,
        confirmText = "Usar esta receta",
        confirmEnabled = !state.loading,
        loading = state.loading,
        enableContentScroll = true,
        validationError = state.error,
        footerSummary = state.cost?.let { "Costo guardado: ${formatSoles(it)}" },
        onConfirm = { viewModel.confirm(onConfirmed = { draft -> onSave(draft); onDismiss() }) },
        onDismiss = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
            BendeyTextField(
                value = state.notes,
                onValueChange = viewModel::setNotes,
                label = "Notas (opcional)",
            )
            Text("Ingredientes", style = MaterialTheme.typography.titleSmall)
            if (state.items.isEmpty()) {
                Text(
                    "Sin ingredientes todavía — agrega al menos uno.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            state.items.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                ) {
                    BendeySearchableSelect(
                        options = ingredientOptions,
                        selectedId = row.productId,
                        onSelect = { id -> viewModel.setIngredientProduct(row.key, id) },
                        label = "Insumo",
                        placeholder = "Buscar producto…",
                        modifier = Modifier.weight(1f),
                    )
                    BendeyTextField(
                        value = row.quantity,
                        onValueChange = { value -> viewModel.setIngredientQuantity(row.key, value) },
                        label = "Cant.",
                        modifier = Modifier.weight(0.4f),
                    )
                    IconButton(onClick = { viewModel.removeIngredient(row.key) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Quitar ingrediente")
                    }
                }
            }
            BendeyTextButton(text = "Agregar ingrediente", onClick = viewModel::addIngredient)
            Text(
                "La cantidad se expresa en la misma unidad de stock del insumo (ej. si la Papa se " +
                    "controla en kg, escribe 0.3 para 300 g).",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "\"Usar esta receta\" no la guarda todavía — se registra recién cuando confirmes el " +
                    "formulario del producto. Si lo cancelas, la receta no se modifica.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (state.cost == 0.0) {
                Text(
                    "El costo guardado es S/ 0.00 porque " +
                        (if (state.hasSavedRecipe) "estos insumos" else "los insumos") +
                        " aún no tienen compras registradas — el costo se calcula del costo promedio " +
                        "de sus compras, no del precio de venta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.Warning,
                )
            }
        }
    }
}

private fun formatSoles(value: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
    return format.format(value)
}
