package com.bendey.restaurant.feature.clientes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.contacts.PurchaseHistoryPage
import com.bendey.restaurant.core.domain.contacts.PurchaseHistoryRules
import com.bendey.restaurant.core.domain.contacts.PurchaseSale
import com.bendey.restaurant.core.domain.time.PeruDateTime
import com.bendey.restaurant.core.ui.components.BendeyBottomSheet
import com.bendey.restaurant.core.ui.components.BendeyLoadError
import com.bendey.restaurant.core.ui.components.BendeyOutlinedButton
import com.bendey.restaurant.core.ui.components.BendeyVerticalScrollColumn

/** Estado del panel «Historial» de un cliente. */
data class HistorialUiState(
    val contactId: Int,
    val name: String,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val page: PurchaseHistoryPage? = null,
)

/**
 * Panel «Historial» de un cliente: visitas, total gastado, ticket promedio, última visita, lo que más pide
 * (top 5) y lista paginada de ventas. Solo ventas vigentes (sin anuladas, borradores ni notas).
 */
@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HistorialClienteSheet(
    state: HistorialUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    BendeyBottomSheet(onDismissRequest = onDismiss) {
        BendeyVerticalScrollColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .padding(horizontal = BendeySpacing.s16),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.s12),
        ) {
            Text("Historial", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BendeyColors.OnSurface)
            Text(state.name, style = MaterialTheme.typography.bodyMedium, color = BendeyColors.OnSurfaceVariant)
            val page = state.page
            when {
                state.loading && page == null -> CircularProgressIndicator()
                state.error != null && page == null -> BendeyLoadError(onRetry = onRetry, message = state.error)
                page != null -> {
                    state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = BendeyColors.Error) }
                    if (page.summary.orders == 0 && page.sales.isEmpty()) {
                        Text(
                            "Este cliente aún no tiene compras registradas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BendeyColors.OnSurfaceVariant,
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
                            verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
                        ) {
                            Stat("Visitas", page.summary.orders.toString())
                            Stat("Total gastado", PurchaseHistoryRules.money(page.summary.totalSpent))
                            Stat("Ticket promedio", PurchaseHistoryRules.money(page.summary.avgTicket))
                            Stat("Última visita", PeruDateTime.formatDate(page.summary.lastVisit) ?: "—")
                        }
                        if (page.topProducts.isNotEmpty()) {
                            Text("Lo que más pide", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = BendeyColors.OnSurface)
                            page.topProducts.forEachIndexed { i, p ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        "${i + 1}. ${p.description}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BendeyColors.OnSurface,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        "${PurchaseHistoryRules.quantity(p.quantity)} u. · ${PurchaseHistoryRules.money(p.total)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BendeyColors.OnSurfaceVariant,
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                        Text("Compras", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = BendeyColors.OnSurface)
                        page.sales.forEach { SaleRow(it) }
                        if (page.hasMore) {
                            BendeyOutlinedButton(
                                text = if (state.loadingMore) "Cargando…" else "Ver más compras",
                                onClick = onLoadMore,
                                enabled = !state.loadingMore,
                                fillWidth = true,
                            )
                        }
                    }
                }
            }
            Column(Modifier.padding(bottom = BendeySpacing.s16)) {}
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    BendeyManagementCard(modifier = Modifier.fillMaxWidth(0.48f)) {
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = BendeyColors.OnSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = BendeyColors.OnSurface)
        }
    }
}

@Composable
private fun SaleRow(s: PurchaseSale) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(
                "${PurchaseHistoryRules.docLabel(s.docType)} ${s.number}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = BendeyColors.OnSurface,
            )
            Text(
                PeruDateTime.formatDate(s.issueDate) ?: s.issueDate,
                style = MaterialTheme.typography.bodySmall,
                color = BendeyColors.OnSurfaceVariant,
            )
        }
        Text(
            PurchaseHistoryRules.money(s.total),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = BendeyColors.OnSurface,
        )
    }
}
