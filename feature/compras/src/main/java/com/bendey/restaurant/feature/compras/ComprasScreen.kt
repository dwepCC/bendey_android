package com.bendey.restaurant.feature.compras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyCard
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.products.ProductItem
import com.bendey.restaurant.core.domain.purchases.Purchase
import com.bendey.restaurant.core.domain.purchases.PurchaseItem
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyEmptyState
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyIconButton
import com.bendey.restaurant.core.ui.components.BendeyLazyColumn
import com.bendey.restaurant.core.ui.components.BendeyOption
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.components.BendeySimpleSelect
import com.bendey.restaurant.core.ui.components.BendeySnackMessage
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.layout.BendeyFlexibleContentSlot
import com.bendey.restaurant.core.ui.layout.BendeyListScreenLayout
import com.bendey.restaurant.core.ui.layout.rememberBendeyBottomBarScrollPadding
import java.text.NumberFormat
import java.util.Locale

private val currency: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "PE"))

/**
 * Compras a proveedores: registra el comprobante recibido, suma stock (comercial/insumo) y
 * actualiza el costo promedio. Solo funciona con sesión de login completo (no PIN) — el backend
 * exige permisos por rol que un token de PIN nunca recibe (ver ComprasViewModel).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprasScreen(
    modifier: Modifier = Modifier,
    onShowMessage: (String) -> Unit = {},
    viewModel: ComprasViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BendeySnackMessage(
        message = state.snackMessage,
        onShow = onShowMessage,
        onConsume = viewModel::consumeSnackMessage,
    )

    BendeyListScreenLayout(
        modifier = modifier.fillMaxSize(),
        isRefreshing = state.loading && state.purchases.isEmpty(),
        onRefresh = viewModel::refresh,
        header = {
            BendeyScreenToolbar(
                title = "Compras",
                subtitle = "Registro de compras a proveedores",
                actions = {
                    if (!state.accessDenied) {
                        BendeyIconButton(onClick = viewModel::refresh, icon = Icons.Default.Refresh, contentDescription = "Actualizar")
                        BendeyIconButton(onClick = viewModel::openForm, icon = Icons.Default.Add, contentDescription = "Nueva compra")
                    }
                },
            )
        },
    ) { contentModifier ->
        if (state.accessDenied) {
            BendeyEmptyState(
                title = "No disponible en esta sesión",
                description = "Compras requiere iniciar sesión con usuario y contraseña (no con el PIN del turno).",
                inline = true,
                modifier = contentModifier.padding(BendeySpacing.md),
            )
        } else {
            ComprasListPane(
                state = state,
                onSearchChange = viewModel::setSearchQuery,
                onOpenDetail = viewModel::openDetail,
                modifier = contentModifier,
            )
        }
    }

    if (state.formOpen) {
        ComprasFormDialog(state = state, viewModel = viewModel)
    }

    if (state.productPickerOpen) {
        ProductPickerDialog(state = state, viewModel = viewModel)
    }

    if (state.detail != null || state.detailLoading) {
        PurchaseDetailDialog(state = state, viewModel = viewModel)
    }

    state.voidTargetId?.let {
        BendeyAlertDialog(
            onDismissRequest = viewModel::dismissVoidConfirm,
            title = "Anular compra",
            message = "Se revertirá el stock y el costo promedio del producto. ¿Continuar?",
            onConfirm = viewModel::confirmVoid,
            confirmText = if (state.voiding) "Anulando…" else "Anular",
        )
    }
}

@Composable
private fun ComprasListPane(
    state: ComprasUiState,
    onSearchChange: (String) -> Unit,
    onOpenDetail: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val bottomScrollPadding = rememberBendeyBottomBarScrollPadding()
    Column(modifier = modifier.fillMaxSize()) {
        BendeyTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            label = "Buscar por comprobante o proveedor",
            modifier = Modifier.padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xs),
        )
        state.error?.takeIf { !state.formOpen }?.let { error ->
            Text(error, color = BendeyColors.Error, modifier = Modifier.padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xxs))
        }
        BendeyFlexibleContentSlot {
            if (state.purchases.isEmpty() && !state.loading) {
                BendeyEmptyState(title = "Sin compras registradas", inline = true, modifier = Modifier.align(Alignment.TopStart))
            } else {
                BendeyLazyColumn(
                    modifier = it,
                    state = listState,
                    contentPadding = PaddingValues(
                        start = BendeySpacing.md, end = BendeySpacing.md,
                        top = BendeySpacing.md, bottom = BendeySpacing.md + bottomScrollPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                ) {
                    items(state.purchases, key = { it.id }) { purchase ->
                        PurchaseRow(purchase = purchase, onOpenDetail = { onOpenDetail(purchase.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchaseRow(purchase: Purchase, onOpenDetail: () -> Unit) {
    BendeyCard(contentPadding = PaddingValues(BendeySpacing.cardPadding)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(purchase.supplierName ?: "Sin proveedor", fontWeight = FontWeight.SemiBold)
                Text(
                    "${purchase.docType} · ${purchase.documentLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
                Text(purchase.issueDate, style = MaterialTheme.typography.labelSmall, color = BendeyColors.OnSurfaceVariant)
                if (purchase.isCancelled) {
                    BendeyStatusChip(label = "Anulada", accentColor = BendeyColors.Error)
                }
            }
            Text(currency.format(purchase.total), fontWeight = FontWeight.Bold)
            BendeyIconButton(onClick = onOpenDetail, icon = Icons.Default.Visibility, contentDescription = "Ver detalle")
        }
    }
}

@Composable
private fun ComprasFormDialog(state: ComprasUiState, viewModel: ComprasViewModel) {
    val form = state.form
    BendeyFormDialog(
        onDismissRequest = viewModel::dismissForm,
        title = "Nueva compra",
        confirmText = if (state.saving) "Guardando…" else "Registrar compra",
        onConfirm = viewModel::save,
        onDismiss = viewModel::dismissForm,
        confirmEnabled = !state.saving,
        loading = state.saving,
        enableContentScroll = true,
    ) {
        BendeySimpleSelect(
            options = state.suppliers.map { BendeyOption(it.id.toString(), it.displayName) },
            selectedValue = form.contactId?.toString(),
            onSelect = { value -> viewModel.updateForm { f -> f.copy(contactId = value.toIntOrNull()) } },
            label = "Proveedor *",
        )
        BendeySimpleSelect(
            options = state.docTypeOptions.map { BendeyOption(it, it) },
            selectedValue = form.docType,
            onSelect = { value -> viewModel.updateForm { f -> f.copy(docType = value) } },
            label = "Tipo de documento",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
            BendeyTextField(
                value = form.series,
                onValueChange = { value -> viewModel.updateForm { f -> f.copy(series = value.trim()) } },
                label = "Serie",
                modifier = Modifier.width(120.dp),
            )
            BendeyTextField(
                value = form.number,
                onValueChange = { value -> viewModel.updateForm { f -> f.copy(number = value) } },
                label = "N° comprobante *",
                modifier = Modifier.weight(1f),
            )
        }
        BendeyTextField(
            value = form.issueDate,
            onValueChange = { value -> viewModel.updateForm { f -> f.copy(issueDate = value) } },
            label = "Fecha (AAAA-MM-DD)",
        )
        BendeySimpleSelect(
            options = listOf(BendeyOption("", "Sin asignar")) + state.paymentMethodOptions.map { BendeyOption(it, it) },
            selectedValue = form.paymentMethod ?: "",
            onSelect = { value -> viewModel.updateForm { f -> f.copy(paymentMethod = value.ifBlank { null }) } },
            label = "Método de pago",
        )
        Text(
            "Si eliges un método de pago, el monto se descuenta de esa cuenta (efectivo → caja abierta).",
            style = MaterialTheme.typography.labelSmall,
            color = BendeyColors.OnSurfaceVariant,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = BendeySpacing.xs))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Detalle de la compra", fontWeight = FontWeight.SemiBold)
            BendeyTextButton(text = "Agregar ítem", onClick = viewModel::openProductPicker)
        }
        if (form.items.isEmpty()) {
            Text("Sin ítems. Usa «Agregar ítem» para elegir productos.", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
        } else {
            form.items.forEachIndexed { index, item ->
                PurchaseItemRow(
                    item = item,
                    onQuantityChange = { qty -> viewModel.updateItem(index) { it.copy(quantity = qty) } },
                    onUnitCostChange = { cost -> viewModel.updateItem(index) { it.copy(unitCost = cost) } },
                    onRemove = { viewModel.removeItem(index) },
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = BendeySpacing.xs))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Column(horizontalAlignment = Alignment.End) {
                Text("Subtotal: ${currency.format(form.subtotal)}", style = MaterialTheme.typography.bodySmall)
                Text("IGV: ${currency.format(form.igv)}", style = MaterialTheme.typography.bodySmall)
                Text("Total: ${currency.format(form.total)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
        state.error?.let { Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun PurchaseItemRow(
    item: PurchaseItem,
    onQuantityChange: (Double) -> Unit,
    onUnitCostChange: (Double) -> Unit,
    onRemove: () -> Unit,
) {
    BendeyCard(contentPadding = PaddingValues(BendeySpacing.sm)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(item.description, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                BendeyIconButton(onClick = onRemove, icon = Icons.Default.Delete, contentDescription = "Quitar", tint = BendeyColors.Error)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
                BendeyTextField(
                    value = if (item.quantity == 0.0) "" else item.quantity.toString(),
                    onValueChange = { value -> onQuantityChange(value.toDoubleOrNull() ?: 0.0) },
                    label = "Cantidad",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                BendeyTextField(
                    value = if (item.unitCost == 0.0) "" else item.unitCost.toString(),
                    onValueChange = { value -> onUnitCostChange(value.toDoubleOrNull() ?: 0.0) },
                    label = "Costo unit.",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
            Text(currency.format(item.lineTotal), style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
        }
    }
}

@Composable
private fun ProductPickerDialog(state: ComprasUiState, viewModel: ComprasViewModel) {
    BendeyFormDialog(
        onDismissRequest = viewModel::dismissProductPicker,
        title = "Agregar producto",
        confirmText = "Cerrar",
        onConfirm = viewModel::dismissProductPicker,
        onDismiss = viewModel::dismissProductPicker,
        enableContentScroll = true,
    ) {
        BendeyTextField(
            value = state.productSearchQuery,
            onValueChange = viewModel::setProductSearchQuery,
            label = "Buscar producto por nombre o código",
        )
        if (state.productResults.isEmpty() && !state.productSearching) {
            Text(
                if (state.productSearchQuery.isBlank()) "Escribe para buscar un producto comercial o insumo." else "Sin resultados.",
                style = MaterialTheme.typography.bodySmall,
                color = BendeyColors.OnSurfaceVariant,
            )
        }
        state.productResults.forEach { product ->
            ProductPickerRow(product = product, onAdd = { viewModel.addProductToItems(product) })
        }
    }
}

@Composable
private fun ProductPickerRow(product: ProductItem, onAdd: () -> Unit) {
    BendeyCard(contentPadding = PaddingValues(BendeySpacing.sm)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Medium)
                Text(
                    "${product.code.ifBlank { "Sin código" }} · ${product.unit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            BendeyIconButton(onClick = onAdd, icon = Icons.Default.Add, contentDescription = "Agregar")
        }
    }
}

@Composable
private fun PurchaseDetailDialog(state: ComprasUiState, viewModel: ComprasViewModel) {
    BendeyFormDialog(
        onDismissRequest = viewModel::dismissDetail,
        title = "Detalle de compra",
        confirmText = "Cerrar",
        onConfirm = viewModel::dismissDetail,
        onDismiss = viewModel::dismissDetail,
        enableContentScroll = true,
    ) {
        val detail = state.detail
        if (state.detailLoading || detail == null) {
            Text("Cargando…", color = BendeyColors.OnSurfaceVariant)
        } else {
            Text(detail.purchase.supplierName ?: "Sin proveedor", fontWeight = FontWeight.SemiBold)
            Text("${detail.purchase.docType} ${detail.purchase.documentLabel}", style = MaterialTheme.typography.bodySmall)
            Text(detail.purchase.issueDate, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
            HorizontalDivider(modifier = Modifier.padding(vertical = BendeySpacing.xs))
            detail.items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${item.quantity} × ${item.description}", modifier = Modifier.weight(1f))
                    Text(currency.format(item.lineTotal))
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = BendeySpacing.xs))
            Text("Total: ${currency.format(detail.purchase.total)}", fontWeight = FontWeight.Bold)
            if (!detail.purchase.isCancelled) {
                BendeyPrimaryButton(
                    text = "Anular compra",
                    onClick = { viewModel.requestVoid(detail.purchase.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
