package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.products.ProductStockDialogState
import com.bendey.restaurant.core.domain.products.ProductStockRules
import com.bendey.restaurant.core.domain.products.StockLevel

/** «Stock de X» en la sucursal activa, abierto desde el ícono de la tarjeta del producto (POS y mesa). */
@Composable
fun BendeyProductStockDialog(state: ProductStockDialogState, onDismiss: () -> Unit) {
    BendeyAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stock de ${state.productName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
        text = {
            val stock = state.stock
            when {
                state.loading -> Box(Modifier.fillMaxWidth().padding(BendeySpacing.md), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> Text(
                    "No se pudo consultar el stock. Revisa tu conexión e intenta de nuevo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BendeyColors.ErrorText,
                )
                stock != null -> Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                    val level = ProductStockRules.level(stock.total, stock.minStock)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("En esta sucursal", style = MaterialTheme.typography.bodyMedium, color = BendeyColors.OnSurfaceVariant)
                        Text(
                            ProductStockRules.qty(stock.total),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (level == StockLevel.OK) BendeyColors.OnSurface else BendeyColors.Error,
                        )
                    }
                    // La palabra dice el estado: no se comunica solo con color.
                    when (level) {
                        StockLevel.OUT -> Text("Sin stock", fontWeight = FontWeight.SemiBold, color = BendeyColors.ErrorText)
                        StockLevel.LOW -> Text(
                            "Por debajo del mínimo (${ProductStockRules.qty(stock.minStock)})",
                            fontWeight = FontWeight.SemiBold,
                            color = BendeyColors.ErrorText,
                        )
                        StockLevel.OK -> Unit
                    }
                    stock.presentations.forEach { p ->
                        val pl = ProductStockRules.level(p.quantity, p.minStock)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(p.name, style = MaterialTheme.typography.bodyMedium, color = BendeyColors.OnSurface, modifier = Modifier.weight(1f))
                            Text(
                                ProductStockRules.qty(p.quantity) + when (pl) {
                                    StockLevel.OUT -> " · sin stock"
                                    StockLevel.LOW -> " · bajo mínimo"
                                    StockLevel.OK -> ""
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (pl == StockLevel.OK) BendeyColors.OnSurface else BendeyColors.Error,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}
