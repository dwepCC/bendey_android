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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bendey.restaurant.core.designsystem.components.BendeyCard
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.billing.TaxConfig
import com.bendey.restaurant.core.domain.billing.calcItem
import com.bendey.restaurant.core.domain.products.ProductItem
import com.bendey.restaurant.core.domain.purchases.Purchase
import com.bendey.restaurant.core.domain.purchases.PurchaseItem
import com.bendey.restaurant.core.domain.purchases.PURCHASE_PAYMENT_METHODS
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyCheckboxRow
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
                onSetDateRange = viewModel::setDateRange,
                onSetStatusFilter = viewModel::setStatusFilter,
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

private fun paymentMethodLabel(code: String?): String {
    if (code.isNullOrBlank()) return "Sin asignar"
    return PURCHASE_PAYMENT_METHODS.find { it == code }?.replaceFirstChar { c -> c.uppercase() } ?: code
}

@Composable
private fun ComprasListPane(
    state: ComprasUiState,
    onSearchChange: (String) -> Unit,
    onOpenDetail: (Int) -> Unit,
    onSetDateRange: (String, String) -> Unit,
    onSetStatusFilter: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val bottomScrollPadding = rememberBendeyBottomBarScrollPadding()
    var showDateRangeDialog by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxSize()) {
        BendeyTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            label = "Buscar por comprobante o proveedor",
            modifier = Modifier.padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xs),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BendeySpacing.md),
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BendeyTextButton(
                text = if (state.dateFrom.isBlank() && state.dateTo.isBlank()) {
                    "Filtrar por fecha"
                } else {
                    "${state.dateFrom.ifBlank { "…" }} → ${state.dateTo.ifBlank { "…" }}"
                },
                onClick = { showDateRangeDialog = true },
            )
            BendeySimpleSelect(
                options = state.statusFilterOptions.map { (value, label) -> BendeyOption(value, label) },
                selectedValue = state.statusFilter,
                onSelect = onSetStatusFilter,
                label = "Estado",
                modifier = Modifier.weight(1f),
            )
        }
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
    if (showDateRangeDialog) {
        CustomDateRangeDialog(
            from = state.dateFrom,
            to = state.dateTo,
            onDismiss = { showDateRangeDialog = false },
            onApply = { from, to ->
                showDateRangeDialog = false
                onSetDateRange(from, to)
            },
        )
    }
}

@Composable
private fun CustomDateRangeDialog(from: String, to: String, onDismiss: () -> Unit, onApply: (String, String) -> Unit) {
    var fromValue by remember(from) { mutableStateOf(from) }
    var toValue by remember(to) { mutableStateOf(to) }
    BendeyFormDialog(
        onDismissRequest = onDismiss,
        title = "Rango de fechas",
        confirmText = "Aplicar",
        onConfirm = { onApply(fromValue.trim(), toValue.trim()) },
        onDismiss = onDismiss,
    ) {
        BendeyTextField(value = fromValue, onValueChange = { fromValue = it }, label = "Desde (AAAA-MM-DD)")
        BendeyTextField(value = toValue, onValueChange = { toValue = it }, label = "Hasta (AAAA-MM-DD)")
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
                Text(
                    paymentMethodLabel(purchase.paymentMethod),
                    style = MaterialTheme.typography.labelSmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
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
            options = state.paymentMethodOptions.map { BendeyOption(it, it.replaceFirstChar { c -> c.uppercase() }) },
            selectedValue = form.paymentMethod,
            onSelect = { value -> viewModel.updateForm { f -> f.copy(paymentMethod = value) } },
            label = "Método de pago *",
        )
        Text(
            "El monto se descuenta de la cuenta del método elegido (efectivo → caja abierta del turno). " +
                "Si no hay caja abierta, o el método no tiene cuenta configurada, no se podrá registrar la compra.",
            style = MaterialTheme.typography.labelSmall,
            color = BendeyColors.OnSurfaceVariant,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = BendeySpacing.xs))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Detalle de la compra", fontWeight = FontWeight.SemiBold)
            BendeyTextButton(text = "Agregar ítem", onClick = viewModel::openProductPicker)
        }
        BendeyCheckboxRow(
            label = "Los precios que agregue ya incluyen IGV (ítems nuevos; cada fila se ajusta aparte)",
            checked = form.defaultPriceIncludesIgv,
            onCheckedChange = viewModel::setDefaultPriceIncludesIgv,
        )
        if (form.items.isEmpty()) {
            Text("Sin ítems. Usa «Agregar ítem» para elegir productos.", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
        } else {
            form.items.forEachIndexed { index, item ->
                PurchaseItemRow(
                    item = item,
                    taxRate = state.taxRate,
                    taxConfig = state.taxConfig,
                    onQuantityChange = { qty -> viewModel.updateItem(index) { it.copy(quantity = qty) } },
                    onUnitCostChange = { cost -> viewModel.updateItem(index) { it.copy(unitCost = cost) } },
                    onPriceIncludesIgvChange = { value -> viewModel.updateItem(index) { it.copy(priceIncludesIgv = value) } },
                    onRemove = { viewModel.removeItem(index) },
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = BendeySpacing.xs))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Column(horizontalAlignment = Alignment.End) {
                Text("Subtotal: ${currency.format(form.subtotal(state.taxRate, state.taxConfig))}", style = MaterialTheme.typography.bodySmall)
                Text("IGV: ${currency.format(form.igv(state.taxRate, state.taxConfig))}", style = MaterialTheme.typography.bodySmall)
                Text(
                    "Total: ${currency.format(form.total(state.taxRate, state.taxConfig))}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        state.error?.let { Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun PurchaseItemRow(
    item: PurchaseItem,
    taxRate: Double,
    taxConfig: TaxConfig,
    onQuantityChange: (Double) -> Unit,
    onUnitCostChange: (Double) -> Unit,
    onPriceIncludesIgvChange: (Boolean) -> Unit,
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
            BendeyCheckboxRow(
                label = "Incluye IGV",
                checked = item.priceIncludesIgv,
                onCheckedChange = onPriceIncludesIgvChange,
            )
            Text(
                currency.format(calcItem(item.unitCost, item.quantity, 0.0, item.igvAffectationType, item.priceIncludesIgv, taxRate, taxConfig).total),
                style = MaterialTheme.typography.bodySmall,
                color = BendeyColors.OnSurfaceVariant,
            )
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
