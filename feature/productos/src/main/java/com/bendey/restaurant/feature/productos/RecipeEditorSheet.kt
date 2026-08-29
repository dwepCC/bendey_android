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
        // `state` viene de un `collectAsState` delegado, así que el compilador no puede afinar el
        // tipo dentro del `when`: se copia a un local para poder leerlo sin repetir el `!!`.
        footerSummary = state.costeo.let { costeo ->
            when {
                costeo != null -> buildString {
                    append("Costo del plato: ")
                    append(formatSoles(costeo.total))
                    if (costeo.sinCostear > 0) append(" (${costeo.sinCostear} sin costear)")
                }
                state.costeando -> "Costo del plato: …"
                else -> null
            }
        },
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
                val linea = state.costeo?.items?.firstOrNull { it.productId == row.productId }
                Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs)) {
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
                            // LA UNIDAD EN LA ETIQUETA DEL CAMPO, no en un párrafo al pie: es donde
                            // se decide si se escribe 1 o 0.3, y ahí es donde hay que leerla.
                            label = row.productId
                                ?.let { id -> ingredientOptions.firstOrNull { it.id == id }?.label }
                                ?.let(::unidadDe)
                                ?.let { u -> "Cant. ($u)" }
                                ?: "Cant.",
                            modifier = Modifier.weight(0.4f),
                        )
                        IconButton(onClick = { viewModel.removeIngredient(row.key) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Quitar ingrediente")
                        }
                    }
                    // EL UNITARIO AL LADO DEL SUBTOTAL, y no solo el subtotal: es lo que deshace el
                    // malentendido de leer «aceite S/8» y esperar S/8 en el plato. Si la receta usa
                    // 0.05, acá se lee «S/8.00 × 0.05 = S/0.40».
                    if (linea != null) {
                        if (linea.sinCostear) {
                            Text(
                                "Sin costo — cárgale el precio de compra en su ficha",
                                style = MaterialTheme.typography.bodySmall,
                                color = BendeyColors.Warning,
                            )
                        } else {
                            Text(
                                "${formatSoles(linea.unitCost)} × ${formatQty(linea.quantity)} = " +
                                    formatSoles(linea.subtotal),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
            BendeyTextButton(text = "Agregar ingrediente", onClick = viewModel::addIngredient)
        }
    }
}

/** Saca la unidad del rótulo del insumo — viene como "PAPA · insumo (kg)". */
private fun unidadDe(label: String): String? =
    Regex("""\(([^)]+)\)\s*$""").find(label)?.groupValues?.get(1)

private fun formatQty(value: Double): String {
    val rounded = kotlin.math.round(value * 1000.0) / 1000.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}

private fun formatSoles(value: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
    return format.format(value)
}
