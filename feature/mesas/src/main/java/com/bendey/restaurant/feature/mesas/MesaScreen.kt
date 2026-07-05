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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.bendey.restaurant.core.ui.components.BendeyBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import com.bendey.restaurant.core.ui.layout.BendeyAdaptiveSplitLayout
import com.bendey.restaurant.core.ui.layout.BendeyCatalogOverlayLayout
import com.bendey.restaurant.core.ui.layout.rememberCompactMesaBarHeight
import com.bendey.restaurant.core.ui.layout.BendeyFlexibleContentSlot
import com.bendey.restaurant.core.ui.layout.BendeyTabletTokens
import com.bendey.restaurant.core.ui.layout.adaptive.AdaptivePos
import com.bendey.restaurant.core.ui.layout.adaptive.BendeyAdaptiveProfile
import com.bendey.restaurant.core.ui.layout.adaptive.BendeyPosWorkspaceMode
import com.bendey.restaurant.core.ui.layout.adaptive.rememberBendeyAdaptiveProfile
import com.bendey.restaurant.core.ui.layout.adaptive.rememberPosWorkspaceMode
import com.bendey.restaurant.core.ui.pos.PosPolishTokens
import com.bendey.restaurant.core.ui.pos.workspace.BendeyPosCatalogWorkspace
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.designsystem.theme.accentColor
import com.bendey.restaurant.core.domain.restaurant.PosCartLine
import com.bendey.restaurant.core.domain.restaurant.PosProduct
import com.bendey.restaurant.core.domain.restaurant.SessionComandaSummary
import com.bendey.restaurant.core.domain.restaurant.SessionOrderSummary
import com.bendey.restaurant.core.domain.billing.TaxConfig
import com.bendey.restaurant.core.domain.billing.isComandaBillable
import com.bendey.restaurant.core.domain.billing.resolveTaxRatePercent
import com.bendey.restaurant.core.ui.checkout.CheckoutSplitBillControl
import com.bendey.restaurant.core.ui.checkout.CheckoutDialog
import com.bendey.restaurant.core.ui.checkout.ReceiptPdfFormatUi
import com.bendey.restaurant.core.ui.checkout.ReceiptPrintModal
import com.bendey.restaurant.core.data.receipt.ReceiptPdfFormat
import com.bendey.restaurant.core.domain.pos.PosCatalogTab
import com.bendey.restaurant.core.domain.pos.PosComboItem
import com.bendey.restaurant.core.ui.components.BendeyIconButton
import com.bendey.restaurant.core.ui.components.BendeyLazyColumn
import com.bendey.restaurant.core.ui.components.BendeyLazyVerticalGrid
import com.bendey.restaurant.core.ui.components.BendeyPosCatalogPane
import com.bendey.restaurant.core.ui.components.BendeyPosProductCard
import com.bendey.restaurant.core.ui.pos.ComboConfigureDialog
import com.bendey.restaurant.core.ui.pos.PosCatalogTabRow
import com.bendey.restaurant.core.ui.pos.ProductConfigureDialog
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyCartAction
import com.bendey.restaurant.core.ui.components.BendeyCartActionGrid
import com.bendey.restaurant.core.ui.components.BendeyCartActionStyle
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyPosCartPane
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.components.BendeySessionOrderCard
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.components.VoidPinDialog
import com.bendey.restaurant.core.ui.pos.ManualProductDialog
import com.bendey.restaurant.core.navigation.CashCheckoutGate
import com.bendey.restaurant.core.navigation.CashCheckoutGateNoOp
import com.bendey.restaurant.core.ui.components.BendeyOverlayBanner
import com.bendey.restaurant.core.ui.layout.rememberBendeyBottomBarScrollPadding
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
    val profile = rememberBendeyAdaptiveProfile()
    val workspaceMode = rememberPosWorkspaceMode()
    val isLandscapeWorkspace = workspaceMode == BendeyPosWorkspaceMode.MediumLandscape ||
        workspaceMode == BendeyPosWorkspaceMode.Expanded
    val usesMobileCartSheet = workspaceMode == BendeyPosWorkspaceMode.Compact ||
        workspaceMode == BendeyPosWorkspaceMode.MediumPortrait
    var showCartSheet by remember { mutableStateOf(false) }
    var showOrdersSheet by remember { mutableStateOf(false) }
    var showManualProduct by remember { mutableStateOf(false) }
    val onCheckout: () -> Unit = {
        if (cashCheckoutGate.ensureForCheckout()) {
            viewModel.openCheckout()
        }
    }
    val orders = state.session?.orders.orEmpty()
    val overlayError = state.error?.takeIf {
        !state.checkoutOpen && state.voidComanda == null && state.comandaNoteTarget == null
    }
    val compactMesaBarHeight = rememberCompactMesaBarHeight(workspaceMode)

    LaunchedEffect(state.snackMessage) {
        if (state.snackMessage != null) {
            viewModel.consumeSnackMessage()
        }
    }

    val tabletBannerBottomPadding = rememberBendeyBottomBarScrollPadding(includeBottomBar = false)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BendeyColors.Background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                BendeyIconButton(
                    onClick = viewModel::refreshSession,
                    icon = Icons.Default.Refresh,
                    contentDescription = "Actualizar",
                )
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
            landscapeTablet = isLandscapeWorkspace,
        )
        if (isLandscapeWorkspace) {
            BendeyFlexibleContentSlot { inner ->
                BendeyAdaptiveSplitLayout(
                    modifier = inner,
                    primary = { catalogModifier ->
                        CatalogSection(
                            state = state,
                            currency = currency,
                            viewModel = viewModel,
                            workspaceMode = workspaceMode,
                            profile = profile,
                            gridBottomPadding = 12.dp,
                            modifier = catalogModifier,
                        )
                    },
                    secondary = { sideModifier ->
                        Column(
                            modifier = sideModifier.background(BendeyColors.SurfaceVariant),
                        ) {
                            if (orders.isNotEmpty()) {
                                SentOrdersQuickAccess(
                                    orderCount = orders.size,
                                    itemCount = orders.sumOf { order -> order.comandas.sumOf { it.quantity.toInt() } },
                                    onClick = { showOrdersSheet = true },
                                    modifier = Modifier.padding(
                                        horizontal = BendeySpacing.sm,
                                        vertical = BendeySpacing.xs,
                                    ),
                                )
                            }
                            CartSection(
                                state = state,
                                currency = currency,
                                viewModel = viewModel,
                                onManualProduct = { showManualProduct = true },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    },
                )
            }
        } else {
            BendeyFlexibleContentSlot { inner ->
                BendeyCatalogOverlayLayout(
                    modifier = inner,
                    includeBottomBar = false,
                    compactBarHeight = compactMesaBarHeight,
                    extraCatalogScrollPadding = BendeySpacing.sm,
                    bannerMessage = overlayError,
                    onBannerDismiss = viewModel::dismissError,
                    catalog = { catalogModifier, gridBottomPadding ->
                        CatalogSection(
                            state = state,
                            currency = currency,
                            viewModel = viewModel,
                            workspaceMode = workspaceMode,
                            profile = profile,
                            gridBottomPadding = gridBottomPadding,
                            modifier = catalogModifier,
                        )
                    },
                    compactBar = { barModifier ->
                        CompactMesaBar(
                            cartTotal = state.cartTotal,
                            sessionTotal = state.sessionTotal,
                            cartCount = state.cartCount,
                            currency = currency,
                            sending = state.sending,
                            onOpenCart = { showCartSheet = true },
                            onSend = viewModel::sendComanda,
                            workspaceMode = workspaceMode,
                            modifier = barModifier,
                        )
                    },
                )
            }
        }
        }
        if (isLandscapeWorkspace) {
            BendeyOverlayBanner(
                message = overlayError,
                onDismiss = viewModel::dismissError,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = BendeySpacing.md,
                        end = BendeySpacing.md,
                        bottom = tabletBannerBottomPadding,
                    ),
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

    CheckoutDialog(
        open = state.checkoutOpen,
        title = "Cobrar y cerrar mesa",
        loading = state.checkoutSubmitting,
        metaLoading = state.checkoutMetaLoading,
        meta = state.checkoutMeta,
        rawTotal = state.checkoutRawTotal,
        discountMode = state.checkoutDiscountMode,
        discountValue = state.checkoutDiscountValue,
        allowDiscount = state.allowDiscountInCheckout,
        payments = state.checkoutPayments,
        seriesId = state.checkoutSeriesId,
        docType = state.checkoutDocType,
        contactId = state.checkoutContactId,
        error = if (state.checkoutOpen) state.error else null,
        extraBeforePayments = {
            val taxRate = resolveTaxRatePercent(state.checkoutMeta?.taxRate)
            val taxConfig = TaxConfig(
                taxRate = taxRate,
                igvRegime = state.checkoutMeta?.igvRegime ?: "standard",
                taxBenefitZone = state.checkoutMeta?.taxBenefitZone ?: false,
            )
            CheckoutSplitBillControl(
                enabled = state.splitBillEnabled,
                onEnabledChange = viewModel::setSplitBillEnabled,
                showOption = state.showSplitBillOption,
                pending = state.pendingComandaRows,
                billed = state.billedComandaRows,
                selectedIds = state.selectedComandaIds,
                onSelectionChange = viewModel::setSelectedComandaIds,
                taxRatePercent = taxRate,
                taxConfig = taxConfig,
            )
        },
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
                val closesSession = state.checkoutSessionClosed
                viewModel.dismissCheckoutSuccess()
                if (closesSession) onCheckoutSuccess()
            },
        )
    }

    if (usesMobileCartSheet && showCartSheet) {
        BendeyBottomSheet(
            onDismissRequest = { showCartSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(
                        when (workspaceMode) {
                            BendeyPosWorkspaceMode.MediumPortrait ->
                                AdaptivePos.portraitCartSheetHeightFraction()
                            else -> 0.92f
                        },
                    )
                    .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.sm),
            ) {
                if (orders.isNotEmpty()) {
                    SentOrdersQuickAccess(
                        orderCount = orders.size,
                        itemCount = orders.sumOf { order -> order.comandas.sumOf { it.quantity.toInt() } },
                        onClick = {
                            showCartSheet = false
                            showOrdersSheet = true
                        },
                        modifier = Modifier.padding(bottom = BendeySpacing.sm),
                    )
                }
                CartSection(
                    state = state,
                    currency = currency,
                    viewModel = viewModel,
                    onManualProduct = { showManualProduct = true },
                    onSend = {
                        viewModel.sendComanda()
                        showCartSheet = false
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (showOrdersSheet) {
        BendeyBottomSheet(
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
                onEditComandaNotes = viewModel::openComandaNoteEditor,
                canAnularComanda = state.canAnularComanda,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f),
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
    landscapeTablet: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BendeyColors.PrimaryContainer)
            .padding(
                horizontal = BendeySpacing.md,
                vertical = 6.dp,
            ),
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
                modifier = Modifier.heightIn(min = if (landscapeTablet) 40.dp else 32.dp),
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onPrintPrecuenta,
                    enabled = !printing && total > 0,
                    modifier = if (landscapeTablet) Modifier.heightIn(min = 40.dp) else Modifier,
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Text(if (printing) "Imprimiendo…" else "Precuenta")
                }
                if (canChargeOrders) {
                    BendeyPrimaryButton(
                        text = if (checkoutLoading) "Cobrando…" else "Cobrar",
                        onClick = onCheckout,
                        enabled = total > 0 && !checkoutLoading && !printing,
                        fillWidth = false,
                        modifier = if (landscapeTablet) Modifier.heightIn(min = 40.dp) else Modifier,
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
    workspaceMode: BendeyPosWorkspaceMode? = null,
    profile: BendeyAdaptiveProfile = rememberBendeyAdaptiveProfile(),
    gridBottomPadding: Dp = 12.dp,
) {
    val isLandscapeWorkspace = workspaceMode == BendeyPosWorkspaceMode.MediumLandscape ||
        workspaceMode == BendeyPosWorkspaceMode.Expanded

    Column(modifier = modifier.fillMaxSize()) {
        if (!isLandscapeWorkspace) {
            PosCatalogTabRow(selected = state.catalogTab, onSelect = viewModel::setCatalogTab)
        }
        BendeyFlexibleContentSlot {
            when (state.catalogTab) {
                PosCatalogTab.PRODUCTS -> {
                    when (workspaceMode) {
                        BendeyPosWorkspaceMode.MediumLandscape,
                        BendeyPosWorkspaceMode.Expanded,
                        -> {
                            BendeyPosCatalogWorkspace(
                                searchQuery = state.searchQuery,
                                onSearchChange = viewModel::setSearchQuery,
                                categories = state.categories,
                                selectedCategoryId = state.selectedCategoryId,
                                onCategorySelect = viewModel::selectCategory,
                                products = state.products,
                                currency = currency,
                                assetsBaseUrl = viewModel.assetsBaseUrl,
                                onProductClick = viewModel::onProductClick,
                                profile = profile,
                                workspaceMode = workspaceMode,
                                modifier = it,
                                catalogTab = state.catalogTab,
                                onCatalogTab = viewModel::setCatalogTab,
                                hasMoreProducts = state.hasMoreProducts,
                                productsLoadingMore = state.productsLoadingMore,
                                onLoadMoreProducts = viewModel::loadMoreProducts,
                                gridBottomPadding = gridBottomPadding,
                            )
                        }
                        else -> {
                            BendeyPosCatalogPane(
                                searchQuery = state.searchQuery,
                                onSearchChange = viewModel::setSearchQuery,
                                categories = state.categories,
                                selectedCategoryId = state.selectedCategoryId,
                                onCategorySelect = viewModel::selectCategory,
                                products = state.products,
                                currency = currency,
                                assetsBaseUrl = viewModel.assetsBaseUrl,
                                onProductClick = viewModel::onProductClick,
                                modifier = it,
                                hasMoreProducts = state.hasMoreProducts,
                                productsLoadingMore = state.productsLoadingMore,
                                onLoadMoreProducts = viewModel::loadMoreProducts,
                                sidebarCategories = false,
                                posCatalogStyle = true,
                                gridBottomPadding = gridBottomPadding,
                                searchPlaceholder = "Buscar producto…",
                            )
                        }
                    }
                }
                PosCatalogTab.COMBOS -> {
                    val term = state.searchQuery.trim().lowercase()
                    val filtered = if (term.isBlank()) state.combos else {
                        state.combos.filter {
                            it.name.lowercase().contains(term) ||
                                it.description.orEmpty().lowercase().contains(term)
                        }
                    }
                    BoxWithConstraints(modifier = it) {
                        val isTabletMobileCatalog = workspaceMode == BendeyPosWorkspaceMode.MediumPortrait ||
                            (workspaceMode == null && PosPolishTokens.isTabletProfile(profile))
                        val columns = when {
                            isTabletMobileCatalog ->
                                AdaptivePos.portraitMobileProductGridColumns(maxWidth.value.toInt())
                            else ->
                                BendeyTabletTokens.posProductGridColumns(profile, maxWidth)
                        }
                        BendeyLazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            modifier = Modifier.fillMaxSize(),
                            state = rememberLazyGridState(),
                            showScrollHints = false,
                            contentPadding = PaddingValues(
                                start = if (isTabletMobileCatalog) {
                                    AdaptivePos.portraitMobileCatalogHorizontalPadding()
                                } else {
                                    12.dp
                                },
                                end = if (isTabletMobileCatalog) {
                                    AdaptivePos.portraitMobileCatalogHorizontalPadding()
                                } else {
                                    12.dp
                                },
                                top = 2.dp,
                                bottom = gridBottomPadding,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(
                                if (isTabletMobileCatalog) AdaptivePos.portraitMobileGridGap() else 6.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(
                                if (isTabletMobileCatalog) AdaptivePos.portraitMobileGridGap() else 6.dp,
                            ),
                        ) {
                            items(filtered, key = { it.id }) { combo ->
                                BendeyPosProductCard(
                                    name = combo.name,
                                    price = combo.basePrice,
                                    currency = currency,
                                    imageUrl = combo.imageUrl,
                                    assetsBaseUrl = viewModel.assetsBaseUrl,
                                    onClick = { viewModel.onComboClick(combo) },
                                    profile = profile,
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
private fun SentOrdersQuickAccess(
    orderCount: Int,
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = BendeyColors.WarningContainer.copy(alpha = 0.45f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
        ) {
            Icon(
                Icons.Default.Restaurant,
                contentDescription = null,
                tint = BendeyColors.OnWarning,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pedidos en cocina",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "$orderCount comanda(s) · $itemCount plato(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            Text(
                text = "Ver",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = BendeyColors.Primary,
            )
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
    var notesLine by remember { mutableStateOf<PosCartLine?>(null) }
    var notesDraft by remember { mutableStateOf("") }

    notesLine?.let { line ->
        BendeyFormDialog(
            onDismissRequest = { notesLine = null },
            title = "Notas para comanda",
            confirmText = "Guardar",
            onConfirm = {
                viewModel.updateCartLineNotes(line.key, notesDraft.trim())
                notesLine = null
            },
            onDismiss = { notesLine = null },
            enableContentScroll = true,
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
        showLineNotes = state.cart.isNotEmpty(),
        onLineNotesClick = { line ->
            notesLine = line
            notesDraft = line.notes
        },
        modifier = modifier,
        primaryAction = {
            BendeyCartActionGrid(
                actions = listOf(
                    BendeyCartAction(
                        text = "Producto manual",
                        onClick = { onManualProduct?.invoke() },
                        enabled = onManualProduct != null,
                        style = BendeyCartActionStyle.SecondaryFilled,
                    ),
                    BendeyCartAction(
                        text = if (state.sending) "Enviando…" else "Enviar comanda",
                        onClick = { onSend?.invoke() ?: viewModel.sendComanda() },
                        enabled = state.cart.isNotEmpty() && !state.sending,
                        style = BendeyCartActionStyle.FilledTonal,
                    ),
                ),
            )
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
    onEditComandaNotes: (SessionComandaSummary) -> Unit = {},
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
        BendeyLazyColumn(
            modifier = Modifier
                .padding(top = BendeySpacing.xs)
                .then(
                    if (expanded) Modifier.weight(1f)
                    else Modifier.heightIn(max = 220.dp),
                ),
            state = rememberLazyListState(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(orders, key = { it.id }) { order ->
                BendeySessionOrderCard(
                    order = order,
                    reprinting = reprintingOrderId == order.id,
                    reprintEnabled = reprintingOrderId != order.id && !reprintingAll,
                    onReprint = { onReprint(order) },
                    comandaActions = { comanda ->
                        BendeyIconButton(
                            onClick = { onEditComandaNotes(comanda) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Editar notas",
                                tint = BendeyColors.Primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        if (canAnularComanda && comanda.isComandaBillable()) {
                            BendeyIconButton(
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
                    },
                )
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
    modifier: Modifier = Modifier,
    workspaceMode: BendeyPosWorkspaceMode = BendeyPosWorkspaceMode.Compact,
) {
    val useTabletPortraitBar = workspaceMode == BendeyPosWorkspaceMode.MediumPortrait
    val barPadding = if (useTabletPortraitBar) BendeySpacing.sm else 12.dp
    val sendButtonWidth = if (useTabletPortraitBar) 0.36f else 0.4f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BendeyColors.Surface)
            .navigationBarsPadding()
            .padding(horizontal = barPadding, vertical = if (useTabletPortraitBar) BendeySpacing.sm else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).clickable(onClick = onOpenCart)) {
            Text(
                "$cartCount en carrito · Mesa ${currency.format(sessionTotal)}",
                style = if (useTabletPortraitBar) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            )
            Text(
                currency.format(cartTotal),
                fontWeight = FontWeight.Bold,
                color = BendeyColors.Primary,
                style = if (useTabletPortraitBar) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            )
        }
        BendeyPrimaryButton(
            text = if (sending) "…" else "Comanda",
            onClick = onSend,
            enabled = cartCount > 0 && !sending,
            modifier = Modifier
                .fillMaxWidth(sendButtonWidth)
                .heightIn(min = if (useTabletPortraitBar) 48.dp else 40.dp),
        )
    }
}
