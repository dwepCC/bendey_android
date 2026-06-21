package com.bendey.restaurant.feature.mesas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.ui.layout.BendeyTabletTokens
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowWidthSizeClass
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.accentColor
import com.bendey.restaurant.core.domain.restaurant.PosProduct
import com.bendey.restaurant.core.domain.restaurant.SessionComandaSummary
import com.bendey.restaurant.core.domain.restaurant.SessionOrderSummary
import com.bendey.restaurant.core.ui.checkout.CheckoutDialog
import com.bendey.restaurant.core.ui.checkout.ReceiptPdfFormatUi
import com.bendey.restaurant.core.ui.checkout.ReceiptPrintModal
import com.bendey.restaurant.core.data.receipt.ReceiptPdfFormat
import com.bendey.restaurant.core.domain.pos.PosCatalogTab
import com.bendey.restaurant.core.domain.pos.PosComboItem
import com.bendey.restaurant.core.ui.components.BendeyPosCatalogPane
import com.bendey.restaurant.core.ui.components.BendeyPosProductCard
import com.bendey.restaurant.core.ui.pos.ComboConfigureDialog
import com.bendey.restaurant.core.ui.pos.PosCatalogTabRow
import com.bendey.restaurant.core.ui.pos.ProductConfigureDialog
import com.bendey.restaurant.core.ui.components.BendeyPosCartPane
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.components.VoidPinDialog
import com.bendey.restaurant.core.ui.pos.ManualProductDialog
import com.bendey.restaurant.core.navigation.CashCheckoutGate
import com.bendey.restaurant.core.navigation.CashCheckoutGateNoOp
import com.bendey.restaurant.core.ui.components.BindSnackMessage
import com.bendey.restaurant.core.ui.layout.bendeySafeDrawingPadding
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MesaScreen(
    onBack: () -> Unit,
    onCheckoutSuccess: () -> Unit = onBack,
    modifier: Modifier = Modifier,
    viewModel: MesaViewModel = hiltViewModel(),
    cashCheckoutGate: CashCheckoutGate = CashCheckoutGateNoOp,
    onShowMessage: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currency = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }
    val widthClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    val isExpanded = widthClass != WindowWidthSizeClass.COMPACT
    var showCartSheet by remember { mutableStateOf(false) }
    var showOrdersSheet by remember { mutableStateOf(false) }
    var showManualProduct by remember { mutableStateOf(false) }
    val onCheckout: () -> Unit = {
        if (cashCheckoutGate.ensureForCheckout()) {
            viewModel.openCheckout()
        }
    }
    val orders = state.session?.orders.orEmpty()

    BindSnackMessage(
        message = state.snackMessage,
        onShow = onShowMessage,
        onConsume = viewModel::consumeSnackMessage,
    )

    Column(modifier = modifier.fillMaxSize().bendeySafeDrawingPadding()) {
        BendeyScreenToolbar(
            title = state.session?.tableName ?: "Mesa",
            subtitle = buildSessionSubtitle(state.session?.floorName, state.session?.orderCode, state.session?.guests),
            onBack = onBack,
            actions = {
                if (orders.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showOrdersSheet = true },
                        modifier = Modifier.heightIn(min = 36.dp),
                    ) {
                        Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Pedidos (${orders.size})")
                    }
                }
                IconButton(onClick = viewModel::refreshSession) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                }
            },
        )
        SessionSummaryBar(
            total = state.sessionTotal,
            currency = currency,
            canCloseMesa = state.canCloseMesa,
            canChargeOrders = state.canChargeOrders,
            closingMesa = state.closingMesa,
            onPrintPrecuenta = viewModel::printPrecuenta,
            onCheckout = onCheckout,
            onCloseMesa = { viewModel.closeMesa(onBack) },
            printing = state.printingPrecuenta,
            checkoutLoading = state.checkoutSubmitting,
        )
        if (isExpanded) {
            Row(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.weight(0.58f)) {
                    CatalogSection(
                        state = state,
                        currency = currency,
                        viewModel = viewModel,
                        sidebarCategories = true,
                        gridBottomPadding = 12.dp,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .background(BendeyColors.SurfaceVariant),
                ) {
                    CartSection(
                        state = state,
                        currency = currency,
                        viewModel = viewModel,
                        onManualProduct = { showManualProduct = true },
                        modifier = Modifier.weight(1f),
                    )
                    OrdersSection(
                        orders = orders,
                        reprintingOrderId = state.reprintingOrderId,
                        reprintingAll = state.reprintingAll,
                        onReprint = viewModel::reprintComanda,
                        onReprintAll = viewModel::reprintAllComandas,
                        onVoidComanda = viewModel::openVoidComanda,
                        canAnularComanda = state.canAnularComanda,
                        modifier = Modifier.weight(1f),
                        expanded = true,
                    )
                }
            }
        } else {
            CatalogSection(
                state = state,
                currency = currency,
                viewModel = viewModel,
                modifier = Modifier.weight(1f),
                gridBottomPadding = 72.dp,
            )
            CompactMesaBar(
                cartTotal = state.cartTotal,
                sessionTotal = state.sessionTotal,
                cartCount = state.cartCount,
                currency = currency,
                sending = state.sending,
                onOpenCart = { showCartSheet = true },
                onSend = viewModel::sendComanda,
            )
        }
        state.error?.let {
            Text(it, color = BendeyColors.Error, modifier = Modifier.padding(16.dp))
        }
    }

    state.productConfigure?.let { cfg ->
        ProductConfigureDialog(
            product = cfg.product,
            loading = cfg.loading,
            presentations = cfg.presentations,
            extraGroups = cfg.extraGroups,
            selected = cfg.selected,
            kitchenNote = cfg.kitchenNote,
            validationError = cfg.error,
            currency = currency,
            onSelectPresentation = viewModel::selectProductPresentation,
            onToggleExtra = viewModel::toggleProductExtra,
            onKitchenNoteChange = viewModel::updateProductConfigureNote,
            onConfirm = viewModel::confirmProductConfigure,
            onDismiss = viewModel::dismissProductConfigure,
        )
    }
    state.comboConfigure?.let { cfg ->
        ComboConfigureDialog(
            combo = cfg.combo,
            loading = cfg.loading,
            form = cfg.form,
            selections = cfg.selections,
            kitchenNote = cfg.kitchenNote,
            validationError = cfg.error,
            resolvedPrice = cfg.resolvedPrice,
            currency = currency,
            componentModifierGroups = cfg.componentModifierGroups,
            componentModifiers = cfg.componentModifiers,
            componentProductNames = cfg.componentProductNames,
            componentPresentations = cfg.componentPresentations,
            onToggleComponentModifier = viewModel::toggleComboComponentModifier,
            onSelectComponentPresentation = viewModel::selectComboComponentPresentation,
            onToggleSlot = viewModel::toggleComboSlot,
            onKitchenNoteChange = viewModel::updateComboConfigureNote,
            onConfirm = viewModel::confirmComboConfigure,
            onDismiss = viewModel::dismissComboConfigure,
        )
    }

    CheckoutDialog(
        open = state.checkoutOpen,
        title = "Cobrar y cerrar mesa",
        loading = state.checkoutSubmitting,
        metaLoading = state.checkoutMetaLoading,
        meta = state.checkoutMeta,
        rawTotal = state.checkoutRawTotal,
        discountMode = state.checkoutDiscountMode,
        discountValue = state.checkoutDiscountValue,
        allowDiscount = state.allowCheckoutDiscount,
        payments = state.checkoutPayments,
        seriesId = state.checkoutSeriesId,
        docType = state.checkoutDocType,
        contactId = state.checkoutContactId,
        error = if (state.checkoutOpen) state.error else null,
        onDismiss = viewModel::dismissCheckout,
        onSeriesChange = viewModel::setCheckoutSeries,
        onContactChange = viewModel::setCheckoutContact,
        onDiscountModeChange = viewModel::setCheckoutDiscountMode,
        onDiscountValueChange = viewModel::setCheckoutDiscountValue,
        onPaymentsChange = viewModel::setCheckoutPayments,
        onConfirm = viewModel::confirmCheckout,
    )

    val context = LocalContext.current
    state.checkoutSuccess?.let { sale ->
        ReceiptPrintModal(
            open = true,
            printData = sale.printData,
            saleNumber = sale.number,
            total = sale.total,
            hasPrinter = state.receiptHasPrinter,
            busyAction = state.receiptBusy,
            onPrint = viewModel::reprintReceipt,
            onShareWhatsApp = { viewModel.shareReceiptViaWhatsApp(context) },
            onOpenPdf = { formatUi ->
                viewModel.openReceiptPdf(
                    context,
                    when (formatUi) {
                        ReceiptPdfFormatUi.TICKET -> ReceiptPdfFormat.TICKET
                        ReceiptPdfFormatUi.A4 -> ReceiptPdfFormat.A4
                    },
                )
            },
            onDismiss = {
                viewModel.dismissCheckoutSuccess()
                onCheckoutSuccess()
            },
        )
    }

    if (!isExpanded && showCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                CartSection(
                    state = state,
                    currency = currency,
                    viewModel = viewModel,
                    onManualProduct = { showManualProduct = true },
                    onSend = {
                        viewModel.sendComanda()
                        showCartSheet = false
                    },
                )
                OrdersSection(
                    orders = orders,
                    reprintingOrderId = state.reprintingOrderId,
                    reprintingAll = state.reprintingAll,
                    onReprint = viewModel::reprintComanda,
                    onReprintAll = viewModel::reprintAllComandas,
                    onVoidComanda = viewModel::openVoidComanda,
                    canAnularComanda = state.canAnularComanda,
                    modifier = Modifier.heightIn(max = 280.dp),
                )
            }
        }
    }

    if (!isExpanded && showOrdersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOrdersSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            OrdersSection(
                orders = orders,
                reprintingOrderId = state.reprintingOrderId,
                reprintingAll = state.reprintingAll,
                onReprint = viewModel::reprintComanda,
                onReprintAll = viewModel::reprintAllComandas,
                onVoidComanda = viewModel::openVoidComanda,
                canAnularComanda = state.canAnularComanda,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 320.dp, max = 520.dp)
                    .padding(bottom = 24.dp),
                expanded = true,
            )
        }
    }

    val voidComanda = state.voidComanda
    VoidPinDialog(
        open = voidComanda != null,
        title = "Anular comanda",
        description = "Se elimina la línea de la comanda en cocina.",
        itemLabel = voidComanda?.let { "${it.productName} ×${it.quantity.toInt()}" },
        reason = state.voidReason,
        pin = state.voidPin,
        loading = state.voidSubmitting,
        error = if (voidComanda != null) state.error else null,
        onReasonChange = viewModel::setVoidReason,
        onPinChange = viewModel::setVoidPin,
        onDismiss = viewModel::dismissVoidDialog,
        onConfirm = viewModel::confirmVoidComanda,
    )

    ManualProductDialog(
        open = showManualProduct,
        onDismiss = { showManualProduct = false },
        onAdd = viewModel::addManualProduct,
    )
}

private fun buildSessionSubtitle(floor: String?, orderCode: String?, guests: Int?): String {
    val parts = mutableListOf<String>()
    floor?.let { parts += it }
    orderCode?.let { parts += it }
    guests?.takeIf { it > 0 }?.let { parts += "$it comensales" }
    return parts.joinToString(" · ").ifBlank { "Sesión activa" }
}

@Composable
private fun SessionSummaryBar(
    total: Double,
    currency: NumberFormat,
    canCloseMesa: Boolean,
    canChargeOrders: Boolean,
    closingMesa: Boolean,
    onPrintPrecuenta: () -> Unit,
    onCheckout: () -> Unit,
    onCloseMesa: () -> Unit,
    printing: Boolean,
    checkoutLoading: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BendeyColors.PrimaryContainer)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Total mesa", style = MaterialTheme.typography.labelMedium)
            Text(
                currency.format(total),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = BendeyColors.Primary,
            )
        }
        if (canCloseMesa) {
            BendeyPrimaryButton(
                text = if (closingMesa) "Cerrando…" else "Cerrar mesa",
                onClick = onCloseMesa,
                enabled = !closingMesa,
                fillWidth = false,
                modifier = Modifier.heightIn(min = 32.dp),
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPrintPrecuenta,
                    enabled = !printing && total > 0,
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Text(if (printing) "Imprimiendo…" else "Precuenta")
                }
                if (canChargeOrders) {
                    BendeyPrimaryButton(
                        text = if (checkoutLoading) "Cobrando…" else "Cobrar",
                        onClick = onCheckout,
                        enabled = total > 0 && !checkoutLoading && !printing,
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogSection(
    state: MesaUiState,
    currency: NumberFormat,
    viewModel: MesaViewModel,
    modifier: Modifier = Modifier,
    sidebarCategories: Boolean = false,
    gridBottomPadding: Dp = 12.dp,
) {
    Column(modifier = modifier.fillMaxSize()) {
        PosCatalogTabRow(selected = state.catalogTab, onSelect = viewModel::setCatalogTab)
        when (state.catalogTab) {
            PosCatalogTab.PRODUCTS -> BendeyPosCatalogPane(
                searchQuery = state.searchQuery,
                onSearchChange = viewModel::setSearchQuery,
                categories = state.categories,
                selectedCategoryId = state.selectedCategoryId,
                onCategorySelect = viewModel::selectCategory,
                products = state.products,
                currency = currency,
                assetsBaseUrl = viewModel.assetsBaseUrl,
                onProductClick = viewModel::onProductClick,
                modifier = Modifier.weight(1f),
                sidebarCategories = sidebarCategories,
                posCatalogStyle = true,
                gridBottomPadding = gridBottomPadding,
                searchPlaceholder = "Buscar producto…",
            )
            PosCatalogTab.COMBOS -> {
                val term = state.searchQuery.trim().lowercase()
                val filtered = if (term.isBlank()) state.combos else {
                    state.combos.filter {
                        it.name.lowercase().contains(term) ||
                            it.description.orEmpty().lowercase().contains(term)
                    }
                }
                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val columns = BendeyTabletTokens.posProductGridColumns(maxWidth)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 2.dp, bottom = gridBottomPadding),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(filtered, key = { it.id }) { combo ->
                            BendeyPosProductCard(
                                name = combo.name,
                                price = combo.basePrice,
                                currency = currency,
                                imageUrl = combo.imageUrl,
                                assetsBaseUrl = viewModel.assetsBaseUrl,
                                onClick = { viewModel.onComboClick(combo) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartSection(
    state: MesaUiState,
    currency: NumberFormat,
    viewModel: MesaViewModel,
    modifier: Modifier = Modifier,
    onSend: (() -> Unit)? = null,
    onManualProduct: (() -> Unit)? = null,
) {
    BendeyPosCartPane(
        title = "Nuevo pedido (${state.cartCount})",
        lines = state.cart,
        total = state.cartTotal,
        currency = currency,
        sending = state.sending,
        onIncrement = viewModel::incrementCartLine,
        onDecrement = viewModel::decrementLine,
        onClearCart = viewModel::clearCart,
        canClearCart = state.canClearCart,
        modifier = modifier,
        primaryAction = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BendeyPrimaryButton(
                    text = "Producto manual",
                    onClick = { onManualProduct?.invoke() },
                    enabled = onManualProduct != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                BendeyPrimaryButton(
                    text = if (state.sending) "Enviando…" else "Enviar comanda",
                    onClick = { onSend?.invoke() ?: viewModel.sendComanda() },
                    enabled = state.cart.isNotEmpty() && !state.sending,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

@Composable
private fun OrdersSection(
    orders: List<SessionOrderSummary>,
    reprintingOrderId: Int?,
    reprintingAll: Boolean,
    onReprint: (SessionOrderSummary) -> Unit,
    onReprintAll: () -> Unit,
    onVoidComanda: (SessionComandaSummary) -> Unit,
    canAnularComanda: Boolean = false,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
) {
    Column(
        modifier = modifier
            .then(if (expanded) Modifier.fillMaxSize() else Modifier)
            .then(if (orders.isNotEmpty()) Modifier.background(BendeyColors.WarningContainer.copy(alpha = 0.35f)) else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Sin pedidos enviados", color = BendeyColors.OnSurfaceVariant)
            }
            return@Column
        }
        val itemCount = orders.sumOf { order -> order.comandas.sumOf { it.quantity.toInt() } }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Default.Restaurant,
                contentDescription = null,
                tint = BendeyColors.OnWarning,
                modifier = Modifier.padding(start = 2.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Ya en cocina", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${orders.size} comanda(s) · $itemCount plato(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            OutlinedButton(
                onClick = onReprintAll,
                enabled = !reprintingAll && reprintingOrderId == null,
                modifier = Modifier.heightIn(min = 36.dp),
            ) {
                if (reprintingAll) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                }
                Text(if (orders.size > 1) "Todas" else "Reimprimir", style = MaterialTheme.typography.labelMedium)
            }
        }
        LazyColumn(
            modifier = Modifier
                .padding(top = 8.dp)
                .then(
                    if (expanded) Modifier.weight(1f, fill = false).heightIn(min = 120.dp, max = 480.dp)
                    else Modifier.heightIn(max = 220.dp),
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(orders, key = { it.id }) { order ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BendeyColors.Outline, RoundedCornerShape(10.dp)),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Comanda #${order.orderNumber}", fontWeight = FontWeight.SemiBold)
                            OutlinedButton(
                                onClick = { onReprint(order) },
                                enabled = reprintingOrderId != order.id && !reprintingAll,
                                modifier = Modifier.heightIn(min = 32.dp),
                            ) {
                                if (reprintingOrderId == order.id) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier
                                            .padding(end = 4.dp)
                                            .size(16.dp),
                                    )
                                } else {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                }
                                Text("Reimprimir", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        order.comandas.forEach { comanda ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${comanda.quantity.toInt()}× ${comanda.productName}",
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                BendeyStatusChip(
                                    label = comanda.status.label,
                                    accentColor = comanda.status.accentColor(),
                                )
                                if (canAnularComanda) {
                                    IconButton(
                                        onClick = { onVoidComanda(comanda) },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Anular línea",
                                            tint = BendeyColors.Error,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactMesaBar(
    cartTotal: Double,
    sessionTotal: Double,
    cartCount: Int,
    currency: NumberFormat,
    sending: Boolean,
    onOpenCart: () -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BendeyColors.Surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).clickable(onClick = onOpenCart)) {
            Text("$cartCount en carrito · Mesa ${currency.format(sessionTotal)}")
            Text(currency.format(cartTotal), fontWeight = FontWeight.Bold, color = BendeyColors.Primary)
        }
        BendeyPrimaryButton(
            text = if (sending) "…" else "Comanda",
            onClick = onSend,
            enabled = cartCount > 0 && !sending,
            modifier = Modifier.fillMaxWidth(0.4f),
        )
    }
}
