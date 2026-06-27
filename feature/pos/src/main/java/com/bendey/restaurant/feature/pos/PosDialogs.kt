package com.bendey.restaurant.feature.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.restaurant.DeliveryDriverBrief
import com.bendey.restaurant.core.domain.restaurant.OpenOrderSummary
import com.bendey.restaurant.core.ui.components.BendeyLazyColumn
import com.bendey.restaurant.core.ui.components.BendeyVerticalScrollColumn
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyIconButton
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import java.text.NumberFormat

@Composable
fun PosPendingOrdersBar(
    count: Int,
    orderCode: String?,
    showEditDetails: Boolean,
    onOpenPending: () -> Unit,
    onEditDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BendeyColors.Surface)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (orderCode != null) {
                Text(orderCode, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text(
                if (count == 0) "0 pedidos pendientes" else if (count == 1) "1 pedido pendiente" else "$count pedidos pendientes",
                style = MaterialTheme.typography.bodySmall,
                color = BendeyColors.OnSurfaceVariant,
            )
        }
        if (showEditDetails) {
            BendeyIconButton(
                onClick = onEditDetails,
                icon = Icons.Default.Edit,
                contentDescription = "Datos del pedido",
                tint = BendeyColors.Primary,
                modifier = Modifier.size(40.dp),
            )
        }
        OutlinedButton(
            onClick = onOpenPending,
            modifier = Modifier.heightIn(min = 36.dp),
        ) {
            BadgedBox(
                badge = {
                    if (count > 0) {
                        Badge {
                            Text(if (count > 99) "99+" else count.toString())
                        }
                    }
                },
            ) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            }
            Text("Pedidos")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsDialog(
    modal: PosOrderDetailsModal?,
    details: PosOrderDetails,
    drivers: List<DeliveryDriverBrief>,
    driversLoading: Boolean,
    onDetailsChange: (PosOrderDetails) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (modal == null) return
    BendeyAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (modal) {
                    PosOrderDetailsModal.TAKEAWAY -> "Para llevar"
                    PosOrderDetailsModal.DELIVERY -> "Delivery"
                },
            )
        },
        text = {
            BendeyVerticalScrollColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (modal == PosOrderDetailsModal.TAKEAWAY) {
                    Text(
                        "Datos opcionales del pedido. El cliente del comprobante se elige al cobrar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                }
                BendeyTextField(
                    value = details.customerName,
                    onValueChange = { onDetailsChange(details.copy(customerName = it)) },
                    label = if (modal == PosOrderDetailsModal.DELIVERY) "Contacto entrega (nombre)" else "Nombre quien recoge",
                )
                BendeyTextField(
                    value = details.customerPhone,
                    onValueChange = { onDetailsChange(details.copy(customerPhone = it)) },
                    label = "Teléfono",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
                if (modal == PosOrderDetailsModal.DELIVERY) {
                    BendeyTextField(
                        value = details.deliveryAddress,
                        onValueChange = { onDetailsChange(details.copy(deliveryAddress = it)) },
                        label = "Dirección *",
                    )
                    BendeyTextField(
                        value = details.deliveryReference,
                        onValueChange = { onDetailsChange(details.copy(deliveryReference = it)) },
                        label = "Referencia",
                    )
                    DriverDropdown(
                        drivers = drivers,
                        loading = driversLoading,
                        selectedId = details.deliveryDriverId,
                        onSelect = { onDetailsChange(details.copy(deliveryDriverId = it)) },
                    )
                    BendeyTextField(
                        value = details.estimatedMinutes,
                        onValueChange = { onDetailsChange(details.copy(estimatedMinutes = it.filter { ch -> ch.isDigit() })) },
                        label = "Tiempo estimado (min)",
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                BendeyTextField(
                    value = details.orderNotes,
                    onValueChange = { onDetailsChange(details.copy(orderNotes = it)) },
                    label = "Notas del pedido",
                    singleLine = false,
                )
            }
        },
        confirmButton = {
            BendeyPrimaryButton(
                text = if (modal == PosOrderDetailsModal.TAKEAWAY) "Listo" else "Guardar",
                onClick = onConfirm,
            )
        },
        dismissButton = {
            BendeyTextButton(
                text = if (modal == PosOrderDetailsModal.TAKEAWAY) "Omitir" else "Cerrar",
                onClick = onDismiss,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DriverDropdown(
    drivers: List<DeliveryDriverBrief>,
    loading: Boolean,
    selectedId: Int?,
    onSelect: (Int?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = drivers.find { it.id == selectedId }?.name ?: "Sin asignar"
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (!loading) expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = if (loading) "Cargando…" else selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Repartidor") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = !loading),
            shape = MaterialTheme.shapes.large,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Sin asignar") },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            drivers.forEach { driver ->
                DropdownMenuItem(
                    text = { Text(driver.name) },
                    onClick = {
                        onSelect(driver.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingOrdersSheet(
    orders: List<OpenOrderSummary>,
    currency: NumberFormat,
    onDismiss: () -> Unit,
    onOpen: (Int) -> Unit,
    onVoid: (OpenOrderSummary) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Pedidos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (orders.isEmpty()) "Sin pedidos pendientes" else "${orders.size} pendiente${if (orders.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                }
                BendeyTextButton(text = "Cerrar", onClick = onDismiss)
            }
            HorizontalDivider()
            if (orders.isEmpty()) {
                Text(
                    "No hay pedidos abiertos",
                    modifier = Modifier.padding(32.dp).fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = BendeyColors.OnSurfaceVariant,
                )
            } else {
                BendeyLazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .padding(12.dp),
                    state = rememberLazyListState(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(orders, key = { it.id }) { order ->
                        PendingOrderCard(
                            order = order,
                            currency = currency,
                            onOpen = { onOpen(order.id) },
                            onVoid = { onVoid(order) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingOrderCard(
    order: OpenOrderSummary,
    currency: NumberFormat,
    onOpen: () -> Unit,
    onVoid: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BendeyShapeTokens.md)
            .border(1.dp, BendeyColors.Outline.copy(alpha = 0.65f), BendeyShapeTokens.md)
            .background(BendeyColors.Surface)
            .padding(BendeySpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpen)
                .padding(end = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    order.orderCode ?: "#${order.id}",
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    currency.format(order.totalAmount),
                    fontWeight = FontWeight.Medium,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                BendeyStatusChip(
                    label = posOrderTypeLabel(order.orderType),
                    accentColor = BendeyColors.Primary,
                )
                BendeyStatusChip(
                    label = posOrderStatusLabel(order.orderStatus),
                    accentColor = BendeyColors.OnSurfaceVariant,
                )
            }
            order.customerName?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            order.customerPhone?.takeIf { it.isNotBlank() }?.let {
                Text("Tel. $it", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
            }
            if (order.orderType == "delivery") {
                order.deliveryAddress?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        BendeyIconButton(
            onClick = onVoid,
            icon = Icons.Default.Delete,
            contentDescription = "Anular pedido",
            tint = BendeyColors.Error,
        )
    }
}

private fun posOrderTypeLabel(type: String): String = when (type) {
    "takeaway" -> "Llevar"
    "delivery" -> "Delivery"
    "quick_sale" -> "Directa"
    else -> type
}

private fun posOrderStatusLabel(status: String): String = when (status) {
    "open" -> "Abierto"
    "in_kitchen" -> "En cocina"
    "ready" -> "Listo"
    "delivered" -> "Entregado"
    "closed" -> "Cerrado"
    "cancelled" -> "Anulado"
    else -> status.replace('_', ' ')
}
