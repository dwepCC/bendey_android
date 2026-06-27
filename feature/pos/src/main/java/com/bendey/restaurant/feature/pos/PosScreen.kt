package com.bendey.restaurant.feature.pos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Surface
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import com.bendey.restaurant.core.ui.layout.BendeyAdaptiveSplitLayout
import com.bendey.restaurant.core.ui.layout.BendeyCatalogOverlayLayout
import com.bendey.restaurant.core.ui.layout.BendeyCompactCartBarHeight
import com.bendey.restaurant.core.ui.layout.BendeyFlexibleContentSlot
import com.bendey.restaurant.core.ui.layout.BendeyTabletTokens
import com.bendey.restaurant.core.ui.layout.rememberUseAdaptiveTwoPane
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChipVariant
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.restaurant.PosCartLine
import com.bendey.restaurant.core.domain.restaurant.PosProduct
import com.bendey.restaurant.core.domain.restaurant.SessionComandaSummary
import com.bendey.restaurant.core.domain.restaurant.SessionOrderSummary
import com.bendey.restaurant.core.ui.checkout.CheckoutDialog
import com.bendey.restaurant.core.ui.checkout.ReceiptPdfFormatUi
import com.bendey.restaurant.core.ui.checkout.ReceiptPrintModal
import com.bendey.restaurant.core.data.receipt.ReceiptPdfFormat
import com.bendey.restaurant.core.ui.components.BendeyLazyVerticalGrid
import com.bendey.restaurant.core.ui.components.BendeyPosCatalogPane
import com.bendey.restaurant.core.domain.pos.PosCatalogTab
import com.bendey.restaurant.core.domain.pos.PosComboItem
import com.bendey.restaurant.core.ui.components.BendeyPosProductCard
import com.bendey.restaurant.core.ui.pos.ComboConfigureDialog
import com.bendey.restaurant.core.ui.pos.PosCatalogTabRow
import com.bendey.restaurant.core.ui.pos.ProductConfigureDialog
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.components.BendeyHorizontalScrollRow
import com.bendey.restaurant.core.ui.components.BendeyPosCartPane
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.pos.ManualProductDialog
import com.bendey.restaurant.core.navigation.CashCheckoutGate
import com.bendey.restaurant.core.navigation.CashCheckoutGateNoOp
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.components.BendeySnackMessage
import com.bendey.restaurant.core.ui.components.VoidPinDialog
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    modifier: Modifier = Modifier,
    viewModel: PosViewModel = hiltViewModel(),
    cashCheckoutGate: CashCheckoutGate = CashCheckoutGateNoOp,
    onShowMessage: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currency = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }
    val useTwoPane = rememberUseAdaptiveTwoPane()
    val isExpanded = useTwoPane
    var showCartSheet by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var barcodeScanOn by remember { mutableStateOf(false) }
    var showManualProduct by remember { mutableStateOf(false) }

    LaunchedEffect(showScanner) {
        if (!showScanner) barcodeScanOn = false
    }

    val onCheckout: () -> Unit = {
        if (cashCheckoutGate.ensureForCheckout()) {
            viewModel.openCheckout()
        }
    }

    BendeySnackMessage(
        message = state.snackMessage,
        onShow = onShowMessage,
        onConsume = viewModel::consumeSnackMessage,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BendeyColors.Background),
    ) {
        OrderTypeRow(
            selected = state.orderType,
            onSelect = { type ->
                viewModel.setOrderType(type)
                if (type != PosOrderType.QUICK_SALE) viewModel.openOrderDetailsModal()
            },
            compact = isExpanded,
        )
        PosPendingOrdersBar(
            count = state.pendingOrdersCount,
            orderCode = state.orderCode,
            showEditDetails = state.isRestaurantOrder,
            onOpenPending = viewModel::openPendingOrdersSheet,
            onEditDetails = viewModel::openOrderDetailsModal,
        )
        if (isExpanded && (state.checkoutRawTotal > 0 || state.activeSessionId != null)) {
            PosSessionBar(
                orderCode = state.orderCode,
                sessionTotal = state.sessionTotal,
                cartTotal = state.cartTotal,
                currency = currency,
                checkoutLoading = state.checkoutSubmitting,
                onCheckout = onCheckout,
                canCheckout = state.canCheckout,
            )
        }
        if (isExpanded) {
            BendeyFlexibleContentSlot { inner ->
                BendeyAdaptiveSplitLayout(
                    modifier = inner,
                    primaryWeight = 0.62f,
                    secondaryWeight = 0.38f,
                    primary = { catalogModifier ->
                        CatalogPane(
                            state = state,
                            currency = currency,
                            assetsBaseUrl = viewModel.assetsBaseUrl,
                            sidebarCategories = true,
                            onSearch = viewModel::setSearchQuery,
                            onCategory = viewModel::selectCategory,
                            onPreparationArea = viewModel::setPreparationAreaFilter,
                            onAdd = viewModel::onProductClick,
                            onComboClick = viewModel::onComboClick,
                            onCatalogTab = viewModel::setCatalogTab,
                            barcodeScanEnabled = barcodeScanOn,
                            onBarcodeScanChange = { active ->
                                barcodeScanOn = active
                                showScanner = active
                            },
                            posStyle = true,
                            catalogBottomPadding = 12.dp,
                            modifier = catalogModifier,
                        )
                    },
                    secondary = { cartModifier ->
                        CartPane(
                            state = state,
                            currency = currency,
                            onIncrement = viewModel::incrementCartLine,
                            onDecrement = viewModel::decrementLine,
                            onRemove = viewModel::removeLine,
                            onClearCart = viewModel::clearCart,
                            onSend = viewModel::sendComanda,
                            onCheckout = onCheckout,
                            onManualProduct = { showManualProduct = true },
                            onSaveDraft = viewModel::saveDraftOrder,
                            onReprint = viewModel::reprintComanda,
                            onReprintAll = viewModel::reprintAllComandas,
                            onVoidComanda = viewModel::openVoidComanda,
                            onEditComandaNotes = viewModel::openComandaNoteEditor,
                            onPrintPrecuenta = viewModel::printPrecuenta,
                            onCartLineNotesChange = { line, notes -> viewModel.updateCartLineNotes(line.key, notes) },
                            onCartLineUnitPriceChange = { line, price -> viewModel.updateCartLineUnitPrice(line.key, price) },
                            canCheckout = state.canCheckout,
                            canAnularComanda = state.canAnularComanda,
                            modifier = cartModifier.background(BendeyColors.SurfaceVariant),
                        )
                    },
                )
            }
        } else {
            BendeyFlexibleContentSlot { inner ->
                BendeyCatalogOverlayLayout(
                    modifier = inner,
                    includeBottomBar = true,
                    compactBarHeight = BendeyCompactCartBarHeight,
                    catalog = { catalogModifier, gridBottomPadding ->
                        CatalogPane(
                            state = state,
                            currency = currency,
                            assetsBaseUrl = viewModel.assetsBaseUrl,
                            onSearch = viewModel::setSearchQuery,
                            onCategory = viewModel::selectCategory,
                            onPreparationArea = viewModel::setPreparationAreaFilter,
                            onAdd = viewModel::onProductClick,
                            onComboClick = viewModel::onComboClick,
                            onCatalogTab = viewModel::setCatalogTab,
                            barcodeScanEnabled = barcodeScanOn,
                            onBarcodeScanChange = { active ->
                                barcodeScanOn = active
                                showScanner = active
                            },
                            posStyle = true,
                            catalogBottomPadding = gridBottomPadding,
                            modifier = catalogModifier,
                        )
                    },
                    compactBar = { barModifier ->
                        CompactCartBar(
                            payableTotal = state.checkoutRawTotal,
                            cartTotal = state.cartTotal,
                            count = state.cartCount,
                            currency = currency,
                            sending = state.sending,
                            checkoutLoading = state.checkoutSubmitting,
                            canCheckout = state.canCheckout,
                            onOpenCart = { showCartSheet = true },
                            onSend = viewModel::sendComanda,
                            onCheckout = onCheckout,
                            modifier = barModifier,
                        )
                    },
                )
            }
        }
        state.error?.let { error ->
            Text(
                text = error,
                color = BendeyColors.Error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    if (!isExpanded && showCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            CartPane(
                state = state,
                currency = currency,
                onIncrement = viewModel::incrementCartLine,
                onDecrement = viewModel::decrementLine,
                onRemove = viewModel::removeLine,
                onClearCart = viewModel::clearCart,
                onSend = {
                    viewModel.sendComanda()
                    showCartSheet = false
                },
                onCheckout = {
                    onCheckout()
                    showCartSheet = false
                },
                onManualProduct = { showManualProduct = true },
                onSaveDraft = viewModel::saveDraftOrder,
                onReprint = viewModel::reprintComanda,
                onReprintAll = viewModel::reprintAllComandas,
                onVoidComanda = viewModel::openVoidComanda,
                onEditComandaNotes = viewModel::openComandaNoteEditor,
                onPrintPrecuenta = viewModel::printPrecuenta,
                onCartLineNotesChange = { line, notes -> viewModel.updateCartLineNotes(line.key, notes) },
                onCartLineUnitPriceChange = { line, price -> viewModel.updateCartLineUnitPrice(line.key, price) },
                canCheckout = state.canCheckout,
                canAnularComanda = state.canAnularComanda,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            )
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
            onSetExtraQuantity = viewModel::setProductExtraQuantity,
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
            onSetSlotQuantity = viewModel::setComboSlotQuantity,
            onKitchenNoteChange = viewModel::updateComboConfigureNote,
            onConfirm = viewModel::confirmComboConfigure,
            onDismiss = viewModel::dismissComboConfigure,
        )
    }

    PosBarcodeScannerSheet(
        open = showScanner,
        onDismiss = {
            showScanner = false
            barcodeScanOn = false
        },
        onBarcodeDetected = viewModel::addProductByBarcode,
    )

    CheckoutDialog(
        open = state.checkoutOpen,
        title = "Cobrar venta",
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
            onDismiss = viewModel::dismissCheckoutSuccess,
        )
    }

    OrderDetailsDialog(
        modal = state.orderDetailsModal,
        details = state.orderDetails,
        drivers = state.deliveryDrivers,
        driversLoading = state.deliveryDriversLoading,
        onDetailsChange = viewModel::setOrderDetails,
        onDismiss = viewModel::dismissOrderDetailsModal,
        onConfirm = viewModel::confirmOrderDetails,
    )

    if (state.pendingOrdersOpen) {
        PendingOrdersSheet(
            orders = state.pendingOrders,
            currency = currency,
            onDismiss = viewModel::dismissPendingOrdersSheet,
            onOpen = viewModel::resumePendingOrder,
            onVoid = viewModel::openVoidPendingOrder,
        )
    }

    val voidTarget = state.voidTarget
    VoidPinDialog(
        open = voidTarget != null,
        title = when (voidTarget) {
            is PosVoidTarget.PendingOrder -> "Anular pedido"
            is PosVoidTarget.Comanda -> "Anular comanda"
            null -> "Anular"
        },
        itemLabel = when (voidTarget) {
            is PosVoidTarget.PendingOrder -> voidTarget.order.orderCode ?: "#${voidTarget.order.id}"
            is PosVoidTarget.Comanda -> "${voidTarget.comanda.productName} ×${voidTarget.comanda.quantity.toInt()}"
            null -> null
        },
        reason = state.voidReason,
        pin = state.voidPin,
        loading = state.voidSubmitting,
        error = if (voidTarget != null) state.error else null,
        onReasonChange = viewModel::setVoidReason,
        onPinChange = viewModel::setVoidPin,
        onDismiss = viewModel::dismissVoidDialog,
        onConfirm = viewModel::confirmVoid,
    )

    ManualProductDialog(
        open = showManualProduct,
        onDismiss = { showManualProduct = false },
        onAdd = viewModel::addManualProduct,
    )

    state.comandaNoteTarget?.let {
        BendeyAlertDialog(
            onDismissRequest = viewModel::dismissComandaNoteEditor,
            title = { Text("Notas de comanda") },
            text = {
                BendeyTextField(
                    value = state.comandaNoteText,
                    onValueChange = viewModel::setComandaNoteText,
                    label = "Notas para cocina",
                    singleLine = false,
                )
            },
            confirmButton = {
                BendeyPrimaryButton(
                    text = if (state.comandaNoteSubmitting) "Guardando…" else "Guardar",
                    onClick = viewModel::confirmComandaNote,
                    enabled = !state.comandaNoteSubmitting,
                    fillWidth = false,
                )
            },
            dismissButton = {
                BendeyTextButton(text = "Cancelar", onClick = viewModel::dismissComandaNoteEditor)
            },
        )
    }
}

@Composable
private fun PosSessionBar(
    orderCode: String?,
    sessionTotal: Double,
    cartTotal: Double,
    currency: NumberFormat,
    checkoutLoading: Boolean,
    onCheckout: () -> Unit,
    canCheckout: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BendeyColors.PrimaryContainer)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(orderCode ?: "Venta activa", style = MaterialTheme.typography.labelMedium)
            Text(
                currency.format(sessionTotal + cartTotal),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = BendeyColors.Primary,
            )
        }
        BendeyPrimaryButton(
            text = if (checkoutLoading) "Cobrando…" else "Cobrar",
            onClick = onCheckout,
            enabled = canCheckout && !checkoutLoading,
        )
    }
}

@Composable
private fun OrderTypeRow(
    selected: PosOrderType,
    onSelect: (PosOrderType) -> Unit,
    compact: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.xxs)
            .clip(BendeyShapeTokens.lg)
            .background(BendeyColors.SurfaceVariant.copy(alpha = 0.65f))
            .padding(BendeySpacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
    ) {
        PosOrderType.entries.forEach { type ->
            val isSelected = selected == type
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(BendeyShapeTokens.sm)
                    .background(if (isSelected) BendeyColors.Primary else Color.Transparent)
                    .clickable { onSelect(type) }
                    .padding(vertical = if (compact) BendeySpacing.xs else 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = type.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) BendeyColors.OnPrimary else BendeyColors.OnSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CatalogPane(
    state: PosUiState,
    currency: NumberFormat,
    assetsBaseUrl: String?,
    onSearch: (String) -> Unit,
    onCategory: (Int?) -> Unit,
    onPreparationArea: (Int?) -> Unit,
    onAdd: (PosProduct) -> Unit,
    onComboClick: (PosComboItem) -> Unit,
    onCatalogTab: (PosCatalogTab) -> Unit,
    modifier: Modifier = Modifier,
    sidebarCategories: Boolean = false,
    posStyle: Boolean = false,
    barcodeScanEnabled: Boolean = false,
    onBarcodeScanChange: ((Boolean) -> Unit)? = null,
    catalogBottomPadding: Dp = 12.dp,
) {
    Column(modifier = modifier.fillMaxSize()) {
        PosCatalogTabRow(selected = state.catalogTab, onSelect = onCatalogTab)
        if (state.catalogTab == PosCatalogTab.PRODUCTS && state.preparationAreas.isNotEmpty()) {
            BendeyHorizontalScrollRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BendeyFilterChip(
                    selected = state.preparationAreaFilter == null,
                    onClick = { onPreparationArea(null) },
                    text = "Todas",
                    variant = BendeyFilterChipVariant.Pos,
                )
                state.preparationAreas.forEach { area ->
                    BendeyFilterChip(
                        selected = state.preparationAreaFilter == area.id,
                        onClick = { onPreparationArea(area.id) },
                        text = area.name,
                        variant = BendeyFilterChipVariant.Pos,
                    )
                }
            }
        }
        BendeyFlexibleContentSlot {
            when (state.catalogTab) {
                PosCatalogTab.PRODUCTS -> BendeyPosCatalogPane(
                    searchQuery = state.searchQuery,
                    onSearchChange = onSearch,
                    categories = state.categories,
                    selectedCategoryId = state.selectedCategoryId,
                    onCategorySelect = onCategory,
                    products = state.products,
                    currency = currency,
                    assetsBaseUrl = assetsBaseUrl,
                    onProductClick = onAdd,
                    modifier = it,
                    sidebarCategories = sidebarCategories,
                    compactCards = !posStyle,
                    posCatalogStyle = posStyle,
                    barcodeScanEnabled = barcodeScanEnabled,
                    onBarcodeScanChange = onBarcodeScanChange,
                    gridBottomPadding = catalogBottomPadding,
                )
                PosCatalogTab.COMBOS -> {
                    val term = state.searchQuery.trim().lowercase()
                    val filtered = if (term.isBlank()) state.combos else {
                        state.combos.filter {
                            it.name.lowercase().contains(term) ||
                                it.description.orEmpty().lowercase().contains(term)
                        }
                    }
                    BoxWithConstraints(modifier = it) {
                        val columns = BendeyTabletTokens.posProductGridColumns(maxWidth)
                        BendeyLazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            modifier = Modifier.fillMaxSize(),
                            state = rememberLazyGridState(),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 2.dp, bottom = catalogBottomPadding),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(filtered, key = { it.id }) { combo ->
                                BendeyPosProductCard(
                                    name = combo.name,
                                    price = combo.basePrice,
                                    currency = currency,
                                    imageUrl = combo.imageUrl,
                                    assetsBaseUrl = assetsBaseUrl,
                                    onClick = { onComboClick(combo) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartPane(
    state: PosUiState,
    currency: NumberFormat,
    onIncrement: (PosCartLine) -> Unit,
    onDecrement: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearCart: () -> Unit,
    onSend: () -> Unit,
    onCheckout: () -> Unit,
    onManualProduct: () -> Unit,
    onSaveDraft: () -> Unit,
    onReprint: (SessionOrderSummary) -> Unit,
    onReprintAll: () -> Unit,
    onVoidComanda: (SessionComandaSummary) -> Unit,
    onEditComandaNotes: (SessionComandaSummary) -> Unit = {},
    onPrintPrecuenta: () -> Unit = {},
    onCartLineNotesChange: (PosCartLine, String) -> Unit = { _, _ -> },
    onCartLineUnitPriceChange: (PosCartLine, String) -> Unit = { _, _ -> },
    canCheckout: Boolean,
    canAnularComanda: Boolean,
    modifier: Modifier = Modifier,
) {
    var notesLine by remember { mutableStateOf<PosCartLine?>(null) }
    var notesDraft by remember { mutableStateOf("") }

    notesLine?.let { line ->
        BendeyFormDialog(
            onDismissRequest = { notesLine = null },
            title = "Notas para comanda",
            confirmText = "Guardar",
            onConfirm = {
                onCartLineNotesChange(line, notesDraft.trim())
                notesLine = null
            },
            onDismiss = { notesLine = null },
        ) {
            Text(
                text = line.product.name,
                style = MaterialTheme.typography.bodySmall,
                color = BendeyColors.OnSurfaceVariant,
            )
            BendeyTextField(
                value = notesDraft,
                onValueChange = { notesDraft = it },
                label = "Notas de cocina",
                singleLine = false,
            )
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (state.hasSentComandas) {
            PosSentOrdersSection(
                orders = state.sessionOrders.filter { it.comandas.isNotEmpty() },
                reprintingOrderId = state.reprintingOrderId,
                reprintingAll = state.reprintingAll,
                onReprint = onReprint,
                onReprintAll = onReprintAll,
                onVoidComanda = onVoidComanda,
                onEditComandaNotes = onEditComandaNotes,
                canAnularComanda = canAnularComanda,
            )
        }
        BendeyPosCartPane(
            title = buildString {
                append("Carrito (${state.cartCount})")
                state.orderCode?.let { append(" · $it") }
            },
            lines = state.cart,
            total = if (state.sessionTotal > 0) state.sessionTotal + state.cartTotal else state.cartTotal,
            currency = currency,
            sending = state.sending,
            onIncrement = onIncrement,
            onDecrement = onDecrement,
            onClearCart = onClearCart,
            canClearCart = state.canClearCart,
            editablePrice = state.cart.isNotEmpty(),
            showLineNotes = state.isRestaurantOrder && state.cart.isNotEmpty(),
            onLineNotesClick = { line ->
                notesLine = line
                notesDraft = line.notes
            },
            onLineUnitPriceChange = onCartLineUnitPriceChange,
            modifier = Modifier.weight(1f),
            primaryAction = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BendeyPrimaryButton(
                        text = "Producto manual",
                        onClick = onManualProduct,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (state.isRestaurantOrder) {
                        BendeyPrimaryButton(
                            text = if (state.sending) "Enviando…" else "Enviar comanda",
                            onClick = onSend,
                            enabled = state.cart.isNotEmpty() && !state.sending && !state.savingDraft,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        BendeyPrimaryButton(
                            text = if (state.savingDraft) "Guardando…" else "Guardar borrador",
                            onClick = onSaveDraft,
                            enabled = !state.sending && !state.savingDraft,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        BendeyPrimaryButton(
                            text = if (state.printingPrecuenta) "Precuenta…" else "Precuenta",
                            onClick = onPrintPrecuenta,
                            enabled = !state.sending && !state.savingDraft && !state.printingPrecuenta,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            },
            secondaryAction = if (canCheckout) {
                {
                    BendeyPrimaryButton(
                        text = if (state.checkoutSubmitting) "Cobrando…" else if (state.isDirectSale) "Cobrar venta" else "Cobrar",
                        onClick = onCheckout,
                        enabled = !state.sending && !state.checkoutSubmitting && !state.savingDraft,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                null
            },
        )
    }
}

@Composable
private fun CompactCartBar(
    payableTotal: Double,
    cartTotal: Double,
    count: Int,
    currency: NumberFormat,
    sending: Boolean,
    checkoutLoading: Boolean,
    canCheckout: Boolean,
    onOpenCart: () -> Unit,
    onSend: () -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = if (payableTotal > 0) payableTotal else cartTotal
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = BendeyShapeTokens.lg,
        colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BendeyColors.Outline.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenCart)
                .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(BendeyColors.PrimaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                BadgedBox(
                    badge = {
                        if (count > 0) {
                            Badge { Text(count.coerceAtMost(99).toString()) }
                        }
                    },
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = "Carrito",
                        tint = BendeyColors.Primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ver carrito",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (count == 1) "1 producto" else "$count productos",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelSmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
                Text(
                    text = currency.format(total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BendeyColors.Primary,
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BendeyColors.Primary)
                    .clickable(onClick = onOpenCart),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Abrir carrito",
                    tint = BendeyColors.OnPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
