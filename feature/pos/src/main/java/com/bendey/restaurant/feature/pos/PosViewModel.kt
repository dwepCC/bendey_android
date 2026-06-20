package com.bendey.restaurant.feature.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.data.feedback.CartFeedback
import com.bendey.restaurant.core.data.printer.DocumentPrintService
import com.bendey.restaurant.core.data.printer.KitchenPrintService
import com.bendey.restaurant.core.data.receipt.ReceiptPdfFormat
import com.bendey.restaurant.core.data.receipt.ReceiptPdfService
import com.bendey.restaurant.core.data.receipt.ReceiptShareHelper
import com.bendey.restaurant.core.data.repository.defaultPaymentMethodCode
import com.bendey.restaurant.core.data.repository.needsOpenCashSessionForPayments
import com.bendey.restaurant.core.data.repository.parseCheckoutPayments
import com.bendey.restaurant.core.data.repository.pickDefaultNotaVentaSeries
import com.bendey.restaurant.core.data.repository.pickVariosContactId
import com.bendey.restaurant.core.domain.billing.BillSessionInput
import com.bendey.restaurant.core.domain.billing.BillSessionResult
import com.bendey.restaurant.core.domain.billing.BillingRepository
import com.bendey.restaurant.core.domain.billing.CheckoutDiscountMode
import com.bendey.restaurant.core.domain.billing.CheckoutMeta
import com.bendey.restaurant.core.domain.billing.CheckoutPaymentDraft
import com.bendey.restaurant.core.domain.billing.calcCheckoutDiscountAmount
import com.bendey.restaurant.core.domain.billing.calcPayableTotal
import com.bendey.restaurant.core.domain.billing.paidCoversTotal
import com.bendey.restaurant.core.domain.billing.roundSunat
import com.bendey.restaurant.core.domain.pos.ComboConfigureState
import com.bendey.restaurant.core.domain.pos.ProductConfigureState
import com.bendey.restaurant.core.domain.catalog.ModifierGroup
import com.bendey.restaurant.core.domain.catalog.CombosRepository
import com.bendey.restaurant.core.domain.catalog.ModifiersRepository
import com.bendey.restaurant.core.domain.catalog.ProductPresentation
import com.bendey.restaurant.core.domain.pos.appendCartLine
import com.bendey.restaurant.core.domain.pos.buildComboConfigJson
import com.bendey.restaurant.core.domain.pos.buildComboConfigureKey
import com.bendey.restaurant.core.domain.pos.buildConfigureKey
import com.bendey.restaurant.core.domain.pos.calcUnitPriceWithModifiers
import com.bendey.restaurant.core.domain.pos.CartModifierEntry
import com.bendey.restaurant.core.domain.pos.ComboCartConfig
import com.bendey.restaurant.core.domain.pos.ComboSlotSelection
import com.bendey.restaurant.core.domain.pos.comboNeedsConfiguration
import com.bendey.restaurant.core.domain.pos.decrementCartLine
import com.bendey.restaurant.core.domain.pos.getProductExtraGroups
import com.bendey.restaurant.core.domain.pos.modifierSummaryText
import com.bendey.restaurant.core.domain.pos.modifiersToJson
import com.bendey.restaurant.core.domain.pos.PosCatalogTab
import com.bendey.restaurant.core.domain.pos.PosComboItem
import com.bendey.restaurant.core.domain.pos.productNeedsConfiguration
import com.bendey.restaurant.core.domain.pos.selectPresentation
import com.bendey.restaurant.core.domain.pos.toggleExtraSelection
import com.bendey.restaurant.core.domain.pos.toggleSlotOption
import com.bendey.restaurant.core.domain.pos.toOrderItemInput
import com.bendey.restaurant.core.domain.pos.validateModifierSelection
import com.bendey.restaurant.core.domain.pos.validateSlotSelections
import com.bendey.restaurant.core.domain.catalog.ProductImageRepository
import com.bendey.restaurant.core.domain.products.ProductsRepository
import com.bendey.restaurant.core.domain.products.toFormInput
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import com.bendey.restaurant.core.domain.restaurant.DeliveryDriverBrief
import com.bendey.restaurant.core.domain.restaurant.MesasRepository
import com.bendey.restaurant.core.domain.restaurant.OpenOrderSummary
import com.bendey.restaurant.core.domain.restaurant.OrderItemInput
import com.bendey.restaurant.core.domain.restaurant.PosCartLine
import com.bendey.restaurant.core.domain.restaurant.PosProduct
import com.bendey.restaurant.core.domain.restaurant.PosRepository
import com.bendey.restaurant.core.domain.restaurant.PosSessionInput
import com.bendey.restaurant.core.domain.restaurant.ProductCategory
import com.bendey.restaurant.core.domain.restaurant.SessionComandaSummary
import com.bendey.restaurant.core.domain.restaurant.SessionOrderSummary
import com.bendey.restaurant.core.domain.restaurant.toComandaLine
import com.bendey.restaurant.core.domain.session.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.round

/** Paridad web: quick_sale, takeaway, delivery */
enum class PosOrderType(val apiValue: String, val label: String) {
    QUICK_SALE("quick_sale", "Directa"),
    TAKEAWAY("takeaway", "Llevar"),
    DELIVERY("delivery", "Delivery"),
}

enum class PosOrderDetailsModal {
    TAKEAWAY,
    DELIVERY,
}

data class PosOrderDetails(
    val customerName: String = "",
    val customerPhone: String = "",
    val orderNotes: String = "",
    val deliveryAddress: String = "",
    val deliveryReference: String = "",
    val deliveryDriverId: Int? = null,
    val estimatedMinutes: String = "30",
)

sealed class PosVoidTarget {
    data class PendingOrder(val order: OpenOrderSummary) : PosVoidTarget()
    data class Comanda(val comanda: SessionComandaSummary) : PosVoidTarget()
}

data class PosUiState(
    val loading: Boolean = false,
    val sending: Boolean = false,
    val products: List<PosProduct> = emptyList(),
    val categories: List<ProductCategory> = emptyList(),
    val selectedCategoryId: Int? = null,
    val searchQuery: String = "",
    val catalogTab: PosCatalogTab = PosCatalogTab.PRODUCTS,
    val combos: List<PosComboItem> = emptyList(),
    val productConfigure: ProductConfigureState? = null,
    val comboConfigure: ComboConfigureState? = null,
    val cart: List<PosCartLine> = emptyList(),
    val orderType: PosOrderType = PosOrderType.QUICK_SALE,
    val orderDetails: PosOrderDetails = PosOrderDetails(),
    val orderDetailsModal: PosOrderDetailsModal? = null,
    val activeSessionId: Int? = null,
    val orderCode: String? = null,
    val sessionTotal: Double = 0.0,
    val sessionOrders: List<SessionOrderSummary> = emptyList(),
    val pendingOrders: List<OpenOrderSummary> = emptyList(),
    val pendingOrdersOpen: Boolean = false,
    val deliveryDrivers: List<DeliveryDriverBrief> = emptyList(),
    val deliveryDriversLoading: Boolean = false,
    val reprintingOrderId: Int? = null,
    val reprintingAll: Boolean = false,
    val voidTarget: PosVoidTarget? = null,
    val voidReason: String = "",
    val voidPin: String = "",
    val voidSubmitting: Boolean = false,
    val checkoutOpen: Boolean = false,
    val checkoutMetaLoading: Boolean = false,
    val checkoutSubmitting: Boolean = false,
    val checkoutMeta: CheckoutMeta? = null,
    val checkoutSeriesId: Int? = null,
    val checkoutDocType: String = "NOTA DE VENTA",
    val checkoutContactId: Int? = null,
    val checkoutPayments: List<CheckoutPaymentDraft> = emptyList(),
    val checkoutDiscountMode: CheckoutDiscountMode = CheckoutDiscountMode.PERCENT,
    val checkoutDiscountValue: String = "0",
    val allowCheckoutDiscount: Boolean = false,
    val checkoutSuccess: BillSessionResult? = null,
    val checkoutPrintNote: String? = null,
    val receiptHasPrinter: Boolean = false,
    val receiptBusy: String? = null,
    val error: String? = null,
    val snackMessage: String? = null,
) {
    val isDirectSale: Boolean get() = orderType == PosOrderType.QUICK_SALE
    val isRestaurantOrder: Boolean get() = !isDirectSale
    val cartTotal: Double get() = cart.sumOf { it.lineTotal }
    val cartCount: Int get() = cart.sumOf { it.quantity }
    val checkoutRawTotal: Double get() = roundMoney(sessionTotal + cartTotal)
    val canClearCart: Boolean get() = cart.isNotEmpty() && sessionOrders.isEmpty()
    val hasSentComandas: Boolean get() = sessionOrders.any { it.comandas.isNotEmpty() }
    val pendingOrdersCount: Int get() = pendingOrders.size

    private val discountNumeric: Double
        get() = checkoutDiscountValue.replace(',', '.').trim().toDoubleOrNull() ?: 0.0

    val checkoutDiscountAmount: Double
        get() = calcCheckoutDiscountAmount(checkoutRawTotal, checkoutDiscountMode, discountNumeric)

    val checkoutPayableTotal: Double
        get() = calcPayableTotal(checkoutRawTotal, checkoutDiscountMode, discountNumeric)

    val canCheckout: Boolean get() = checkoutRawTotal > 0 && !checkoutSubmitting && !sending
}

private fun roundMoney(value: Double): Double = round(value * 100.0) / 100.0

@HiltViewModel
class PosViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val mesasRepository: MesasRepository,
    private val billingRepository: BillingRepository,
    private val kitchenPrintService: KitchenPrintService,
    private val documentPrintService: DocumentPrintService,
    private val receiptPdfService: ReceiptPdfService,
    private val cartFeedback: CartFeedback,
    private val sessionStore: UserSessionStore,
    private val productImageRepository: ProductImageRepository,
    private val productsRepository: ProductsRepository,
    private val modifiersRepository: ModifiersRepository,
    private val combosRepository: CombosRepository,
) : ViewModel() {

    val assetsBaseUrl: String?
        get() = productImageRepository.tenantAssetsBaseUrl()

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    init {
        refreshCatalog()
        warmCheckoutMeta()
        loadPendingOrders()
    }

    private fun warmCheckoutMeta() {
        viewModelScope.launch {
            val branchId = sessionStore.userSessionFlow.first()?.activeBranch?.id ?: return@launch
            when (val result = billingRepository.loadCheckoutMeta(branchId)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        if (state.checkoutMeta != null) state else state.copy(checkoutMeta = result.data)
                    }
                    applyCheckoutDefaults(result.data)
                }
                else -> Unit
            }
        }
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val branchId = sessionStore.userSessionFlow.first()?.activeBranch?.id
            when (val categoriesResult = posRepository.loadCategories()) {
                is AppResult.Error -> _uiState.update {
                    it.copy(loading = false, error = categoriesResult.message)
                }
                is AppResult.Success -> {
                    _uiState.update { it.copy(categories = categoriesResult.data) }
                    loadProducts(branchId = branchId)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun loadPendingOrders() {
        viewModelScope.launch {
            when (val result = posRepository.listOpenOrders()) {
                is AppResult.Success -> _uiState.update { it.copy(pendingOrders = result.data) }
                else -> Unit
            }
        }
    }

    fun openPendingOrdersSheet() {
        loadPendingOrders()
        _uiState.update { it.copy(pendingOrdersOpen = true) }
    }

    fun dismissPendingOrdersSheet() {
        _uiState.update { it.copy(pendingOrdersOpen = false) }
    }

    fun resumePendingOrder(orderId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, pendingOrdersOpen = false) }
            loadSession(orderId)
            _uiState.update { it.copy(loading = false) }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            loadProducts(branchId = sessionStore.userSessionFlow.first()?.activeBranch?.id)
        }
    }

    fun selectCategory(categoryId: Int?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
        viewModelScope.launch {
            loadProducts(branchId = sessionStore.userSessionFlow.first()?.activeBranch?.id)
        }
    }

    fun setOrderType(type: PosOrderType) {
        val state = _uiState.value
        if (state.activeSessionId != null && state.orderType != type) {
            resetSessionState(
                newType = type,
                snack = "Se inició un pedido nuevo",
            )
            return
        }
        _uiState.update {
            it.copy(
                orderType = type,
                orderDetailsModal = if (type == PosOrderType.QUICK_SALE) null else it.orderDetailsModal,
            )
        }
        if (type == PosOrderType.DELIVERY) loadDeliveryDriversIfNeeded()
    }

    fun openOrderDetailsModal() {
        val modal = when (_uiState.value.orderType) {
            PosOrderType.TAKEAWAY -> PosOrderDetailsModal.TAKEAWAY
            PosOrderType.DELIVERY -> PosOrderDetailsModal.DELIVERY
            PosOrderType.QUICK_SALE -> return
        }
        if (modal == PosOrderDetailsModal.DELIVERY) loadDeliveryDriversIfNeeded()
        _uiState.update { it.copy(orderDetailsModal = modal) }
    }

    fun dismissOrderDetailsModal() {
        _uiState.update { it.copy(orderDetailsModal = null) }
    }

    fun setOrderDetails(details: PosOrderDetails) {
        _uiState.update { it.copy(orderDetails = details) }
    }

    fun confirmOrderDetails() {
        val state = _uiState.value
        if (state.orderType == PosOrderType.DELIVERY && state.orderDetails.deliveryAddress.isBlank()) {
            _uiState.update { it.copy(error = "Ingresa la dirección de delivery") }
            return
        }
        _uiState.update { it.copy(orderDetailsModal = null, error = null) }
        viewModelScope.launch {
            state.activeSessionId?.let { sessionId ->
                posRepository.updatePosSession(sessionId, buildSessionInput(state))
            }
        }
    }

    fun setCatalogTab(tab: PosCatalogTab) {
        _uiState.update { it.copy(catalogTab = tab) }
        if (tab == PosCatalogTab.COMBOS && _uiState.value.combos.isEmpty()) {
            loadCombos()
        }
    }

    private fun loadCombos() {
        viewModelScope.launch {
            val branchId = sessionStore.userSessionFlow.first()?.activeBranch?.id
            when (val result = combosRepository.listPosCombos(branchId)) {
                is AppResult.Success -> _uiState.update { it.copy(combos = result.data) }
                is AppResult.Error -> _uiState.update { it.copy(error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun onProductClick(product: PosProduct) {
        if (!product.availableForSale) {
            _uiState.update { it.copy(error = "${product.name} no está disponible") }
            return
        }
        if (productNeedsConfiguration(product)) {
            openProductConfigure(product)
        } else {
            addSimpleProduct(product)
        }
    }

    fun onComboClick(combo: PosComboItem) {
        if (comboNeedsConfiguration(combo)) {
            openComboConfigure(combo)
        } else {
            viewModelScope.launch { resolveAndAddCombo(combo, ComboCartConfig()) }
        }
    }

    private fun openProductConfigure(product: PosProduct) {
        _uiState.update {
            it.copy(productConfigure = ProductConfigureState(product = product), error = null)
        }
        viewModelScope.launch {
            val detail = productsRepository.getProductDetail(product.id)
            val groups = modifiersRepository.listModifierGroups()
            when {
                detail is AppResult.Error -> _uiState.update {
                    it.copy(productConfigure = it.productConfigure?.copy(loading = false, error = detail.message))
                }
                groups is AppResult.Error -> _uiState.update {
                    it.copy(productConfigure = it.productConfigure?.copy(loading = false, error = groups.message))
                }
                detail is AppResult.Success && groups is AppResult.Success -> {
                    val form = detail.data.toFormInput()
                    val extraGroups = getProductExtraGroups(form.modifierGroupIds, groups.data, product)
                    val initialSelected = form.presentations
                        .firstOrNull { it.active && it.name.isNotBlank() }
                        ?.let { selectPresentation(emptyList(), it) }
                        ?: emptyList()
                    _uiState.update {
                        it.copy(
                            productConfigure = it.productConfigure?.copy(
                                loading = false,
                                presentations = form.presentations,
                                extraGroups = extraGroups,
                                selected = initialSelected,
                            ),
                        )
                    }
                }
                else -> Unit
            }
        }
    }

    fun dismissProductConfigure() {
        _uiState.update { it.copy(productConfigure = null) }
    }

    fun updateProductConfigureNote(note: String) {
        _uiState.update { state ->
            state.copy(productConfigure = state.productConfigure?.copy(kitchenNote = note, error = null))
        }
    }

    fun selectProductPresentation(presentation: ProductPresentation) {
        _uiState.update { state ->
            val cfg = state.productConfigure ?: return@update state
            state.copy(
                productConfigure = cfg.copy(
                    selected = selectPresentation(cfg.selected, presentation),
                    error = null,
                ),
            )
        }
    }

    fun toggleProductExtra(group: ModifierGroup, optionId: Int) {
        _uiState.update { state ->
            val cfg = state.productConfigure ?: return@update state
            state.copy(
                productConfigure = cfg.copy(
                    selected = toggleExtraSelection(cfg.selected, group, optionId),
                    error = null,
                ),
            )
        }
    }

    fun confirmProductConfigure() {
        val cfg = _uiState.value.productConfigure ?: return
        val product = cfg.product
        val validation = validateModifierSelection(cfg.presentations, cfg.extraGroups, cfg.selected, product)
        if (validation != null) {
            _uiState.update { it.copy(productConfigure = cfg.copy(error = validation)) }
            return
        }
        val unitPrice = calcUnitPriceWithModifiers(product.salePrice, cfg.selected)
        val modifiersJson = modifiersToJson(cfg.selected).takeIf { it.isNotBlank() }
        val cartKey = buildConfigureKey(cfg.selected, cfg.kitchenNote)
        val line = PosCartLine(
            cartKey = cartKey,
            product = product,
            quantity = 1,
            notes = cfg.kitchenNote,
            unitPrice = unitPrice,
            modifiersJson = modifiersJson,
            subtitle = modifierSummaryText(cfg.selected).takeIf { it.isNotBlank() },
        )
        cartFeedback.playAddToCart()
        _uiState.update {
            it.copy(
                cart = appendCartLine(it.cart, line),
                productConfigure = null,
                snackMessage = "${product.name} agregado",
            )
        }
    }

    private fun openComboConfigure(combo: PosComboItem) {
        _uiState.update { it.copy(comboConfigure = ComboConfigureState(combo = combo), error = null) }
        viewModelScope.launch {
            when (val result = combosRepository.getCombo(combo.id)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(comboConfigure = it.comboConfigure?.copy(loading = false, form = result.data))
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(comboConfigure = it.comboConfigure?.copy(loading = false, error = result.message))
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun dismissComboConfigure() {
        _uiState.update { it.copy(comboConfigure = null) }
    }

    fun updateComboConfigureNote(note: String) {
        _uiState.update { state ->
            state.copy(comboConfigure = state.comboConfigure?.copy(kitchenNote = note, error = null))
        }
    }

    fun toggleComboSlot(slot: com.bendey.restaurant.core.domain.catalog.ComboSlot, optionId: Int) {
        _uiState.update { state ->
            val cfg = state.comboConfigure ?: return@update state
            val selections = toggleSlotOption(cfg.selections, slot, optionId)
            state.copy(comboConfigure = cfg.copy(selections = selections, error = null))
        }
        previewComboPrice()
    }

    private fun previewComboPrice() {
        val cfg = _uiState.value.comboConfigure ?: return
        val form = cfg.form ?: return
        viewModelScope.launch {
            val branchId = sessionStore.userSessionFlow.first()?.activeBranch?.id ?: return@launch
            val configJson = buildComboConfigJson(ComboCartConfig(cfg.selections))
            when (val result = combosRepository.resolveCombo(cfg.combo.id, branchId, configJson)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(comboConfigure = it.comboConfigure?.copy(resolvedPrice = result.data))
                }
                else -> Unit
            }
        }
    }

    fun confirmComboConfigure() {
        val cfg = _uiState.value.comboConfigure ?: return
        val form = cfg.form
        val validation = form?.slots?.let { validateSlotSelections(it, cfg.selections) }
        if (validation != null) {
            _uiState.update { it.copy(comboConfigure = cfg.copy(error = validation)) }
            return
        }
        viewModelScope.launch {
            resolveAndAddCombo(
                cfg.combo,
                ComboCartConfig(cfg.selections),
                cfg.kitchenNote,
                cfg.resolvedPrice,
            )
            _uiState.update { it.copy(comboConfigure = null) }
        }
    }

    private suspend fun resolveAndAddCombo(
        combo: PosComboItem,
        config: ComboCartConfig,
        notes: String = "",
        knownPrice: Double? = null,
    ) {
        val branchId = sessionStore.userSessionFlow.first()?.activeBranch?.id
        if (branchId == null) {
            _uiState.update { it.copy(error = "Selecciona una sucursal") }
            return
        }
        val configJson = buildComboConfigJson(config)
        val unitPrice = knownPrice ?: when (val resolved = combosRepository.resolveCombo(combo.id, branchId, configJson)) {
            is AppResult.Success -> resolved.data
            is AppResult.Error -> {
                _uiState.update { it.copy(error = resolved.message) }
                return
            }
            AppResult.Loading -> return
        }
        val cartKey = buildComboConfigureKey(combo.id, config, notes, unitPrice)
        val syntheticProduct = PosProduct(
            id = -combo.id,
            code = "COMBO-${combo.id}",
            name = combo.name,
            salePrice = unitPrice,
            categoryId = null,
            imageUrl = combo.imageUrl,
            igvAffectationType = null,
            priceIncludesIgv = true,
        )
        val line = PosCartLine(
            cartKey = cartKey,
            product = syntheticProduct,
            quantity = 1,
            notes = notes,
            itemKind = "combo",
            unitPrice = unitPrice,
            comboId = combo.id,
            comboConfigJson = configJson,
        )
        cartFeedback.playAddToCart()
        _uiState.update {
            it.copy(cart = appendCartLine(it.cart, line), snackMessage = "${combo.name} agregado")
        }
    }

    private fun addSimpleProduct(product: PosProduct) {
        cartFeedback.playAddToCart()
        _uiState.update { state ->
            val line = PosCartLine(cartKey = "p-${product.id}", product = product, quantity = 1)
            state.copy(cart = appendCartLine(state.cart, line), snackMessage = "${product.name} agregado", error = null)
        }
    }

    fun addToCart(product: PosProduct) = onProductClick(product)

    fun incrementCartLine(line: PosCartLine) {
        cartFeedback.playAddToCart()
        _uiState.update { state ->
            state.copy(cart = appendCartLine(state.cart, line.copy(quantity = 1)))
        }
    }

    fun clearCart() {
        if (!_uiState.value.canClearCart) return
        cartFeedback.playAddToCart()
        _uiState.update { it.copy(cart = emptyList(), snackMessage = "Carrito vaciado") }
    }

    fun decrementLine(cartKey: String) {
        _uiState.update { state -> state.copy(cart = decrementCartLine(state.cart, cartKey)) }
    }

    fun removeLine(cartKey: String) {
        _uiState.update { it.copy(cart = it.cart.filter { line -> line.key != cartKey }) }
    }

    fun addProductByBarcode(code: String) {
        val normalized = code.trim()
        if (normalized.isEmpty()) return
        val local = _uiState.value.products.find { productMatchesBarcode(it.code, normalized) }
        if (local != null) {
            if (!local.availableForSale) {
                _uiState.update { it.copy(error = "${local.name} no está disponible") }
                return
            }
            onProductClick(local)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val branchId = sessionStore.userSessionFlow.first()?.activeBranch?.id
            when (val result = posRepository.loadProducts(normalized, null, page = 1, branchId)) {
                is AppResult.Success -> {
                    val products = result.data.first
                    val match = products.find { productMatchesBarcode(it.code, normalized) }
                        ?: products.firstOrNull()
                    if (match != null && match.availableForSale) {
                        onProductClick(match)
                        _uiState.update { it.copy(loading = false) }
                    } else {
                        _uiState.update {
                            it.copy(loading = false, error = "No se encontró producto con código $normalized")
                        }
                    }
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(loading = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun consumeSnackMessage() {
        _uiState.update { it.copy(snackMessage = null) }
    }

    fun sendComanda() {
        val state = _uiState.value
        if (!state.isRestaurantOrder) return
        if (state.cart.isEmpty()) {
            _uiState.update { it.copy(error = "Agrega productos al carrito") }
            return
        }
        if (state.orderType == PosOrderType.DELIVERY && state.orderDetails.deliveryAddress.isBlank()) {
            _uiState.update {
                it.copy(error = "Completa la dirección en Delivery", orderDetailsModal = PosOrderDetailsModal.DELIVERY)
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(sending = true, error = null) }
            val sessionId = ensureSession(state) ?: run {
                _uiState.update { it.copy(sending = false) }
                return@launch
            }
            if (sendCartItems(sessionId, state)) {
                refreshFullSession(sessionId)
                _uiState.update {
                    it.copy(
                        sending = false,
                        cart = emptyList(),
                        snackMessage = "Comanda enviada",
                    )
                }
                loadPendingOrders()
            } else {
                _uiState.update { it.copy(sending = false) }
            }
        }
    }

    fun reprintComanda(order: SessionOrderSummary) {
        val state = _uiState.value
        if (order.comandas.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(reprintingOrderId = order.id) }
            val userName = sessionStore.userSessionFlow.first()?.user?.name
            val tableLabel = state.orderType.label
            val result = kitchenPrintService.reprintComandaRound(
                tableName = tableLabel,
                orderNumber = order.orderNumber,
                waiterName = userName,
                comandas = order.comandas.map { it.toComandaLine() },
            )
            _uiState.update {
                it.copy(
                    reprintingOrderId = null,
                    snackMessage = when (result) {
                        true -> "Comanda #${order.orderNumber} reimpresa"
                        false -> "No se pudo reimprimir"
                        null -> "Configura impresora de comandas"
                    },
                )
            }
        }
    }

    fun reprintAllComandas() {
        val state = _uiState.value
        val orders = state.sessionOrders.filter { it.comandas.isNotEmpty() }
        if (orders.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(reprintingAll = true) }
            val userName = sessionStore.userSessionFlow.first()?.user?.name
            val result = kitchenPrintService.reprintAllComandaRounds(
                tableName = state.orderType.label,
                orders = orders.map { order ->
                    order.orderNumber to order.comandas.map { it.toComandaLine() }
                },
                waiterName = userName,
            )
            _uiState.update {
                it.copy(
                    reprintingAll = false,
                    snackMessage = when (result) {
                        true -> "Comandas reimpresas"
                        false -> "No se pudo reimprimir"
                        null -> "Configura impresora de comandas"
                    },
                )
            }
        }
    }

    fun openVoidPendingOrder(order: OpenOrderSummary) {
        _uiState.update {
            it.copy(voidTarget = PosVoidTarget.PendingOrder(order), voidReason = "", voidPin = "", error = null)
        }
    }

    fun openVoidComanda(comanda: SessionComandaSummary) {
        _uiState.update {
            it.copy(voidTarget = PosVoidTarget.Comanda(comanda), voidReason = "", voidPin = "", error = null)
        }
    }

    fun dismissVoidDialog() {
        if (_uiState.value.voidSubmitting) return
        _uiState.update { it.copy(voidTarget = null, voidReason = "", voidPin = "") }
    }

    fun setVoidReason(reason: String) {
        _uiState.update { it.copy(voidReason = reason) }
    }

    fun setVoidPin(pin: String) {
        _uiState.update { it.copy(voidPin = pin.filter { it.isDigit() }.take(6)) }
    }

    fun confirmVoid() {
        val state = _uiState.value
        val reason = state.voidReason.trim()
        val pin = state.voidPin.trim()
        if (reason.isBlank() || pin.isBlank()) {
            _uiState.update { it.copy(error = "Indique motivo y PIN") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(voidSubmitting = true, error = null) }
            val result = when (val target = state.voidTarget) {
                is PosVoidTarget.PendingOrder -> posRepository.cancelSession(target.order.id, reason, pin)
                is PosVoidTarget.Comanda -> posRepository.cancelComanda(target.comanda.id, reason, pin)
                null -> AppResult.Error("Sin objetivo")
            }
            when (result) {
                is AppResult.Success -> {
                    val wasActive = (state.voidTarget as? PosVoidTarget.PendingOrder)?.order?.id == state.activeSessionId
                    _uiState.update {
                        it.copy(
                            voidSubmitting = false,
                            voidTarget = null,
                            voidReason = "",
                            voidPin = "",
                            pendingOrdersOpen = false,
                            snackMessage = "Anulado correctamente",
                        )
                    }
                    if (wasActive) resetSessionState(newType = state.orderType)
                    else state.activeSessionId?.let { refreshFullSession(it) }
                    loadPendingOrders()
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(voidSubmitting = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    // --- Checkout (sin cambios sustanciales) ---

    fun openCheckout() {
        val state = _uiState.value
        if (state.checkoutRawTotal <= 0) {
            _uiState.update { it.copy(error = "No hay monto por cobrar") }
            return
        }
        viewModelScope.launch {
            val session = sessionStore.userSessionFlow.first()
            val allowDiscount = RestaurantPermissions.canApplyCheckoutDiscount(
                session?.permissions.orEmpty(),
                session?.user?.employeeType,
            )
            val method = defaultPaymentMethodCode(state.checkoutMeta?.paymentMethods.orEmpty())
            _uiState.update {
                it.copy(
                    checkoutOpen = true,
                    allowCheckoutDiscount = allowDiscount,
                    checkoutDiscountMode = CheckoutDiscountMode.PERCENT,
                    checkoutDiscountValue = "0",
                    checkoutPayments = listOf(
                        CheckoutPaymentDraft(method = method, amount = formatAmount(it.checkoutPayableTotal)),
                    ),
                    error = null,
                )
            }
            if (state.checkoutMeta == null) loadCheckoutMeta() else applyCheckoutDefaults(state.checkoutMeta)
        }
    }

    fun dismissCheckout() {
        _uiState.update { it.copy(checkoutOpen = false, error = null) }
    }

    fun setCheckoutSeries(seriesId: Int, docType: String) {
        _uiState.update { it.copy(checkoutSeriesId = seriesId, checkoutDocType = docType) }
    }

    fun setCheckoutContact(contactId: Int) {
        _uiState.update { it.copy(checkoutContactId = contactId) }
    }

    fun setCheckoutDiscountMode(mode: CheckoutDiscountMode) {
        _uiState.update { syncSinglePaymentAmount(it.copy(checkoutDiscountMode = mode)) }
    }

    fun setCheckoutDiscountValue(value: String) {
        _uiState.update { syncSinglePaymentAmount(it.copy(checkoutDiscountValue = value)) }
    }

    fun setCheckoutPayments(payments: List<CheckoutPaymentDraft>) {
        _uiState.update { it.copy(checkoutPayments = payments) }
    }

    fun confirmCheckout() {
        val state = _uiState.value
        val seriesId = state.checkoutSeriesId
        val contactId = state.checkoutContactId
        val payable = state.checkoutPayableTotal
        val paymentLines = parseCheckoutPayments(state.checkoutPayments)
        if (seriesId == null) {
            _uiState.update { it.copy(error = "Selecciona una serie") }
            return
        }
        if (contactId == null) {
            _uiState.update { it.copy(error = "Selecciona un cliente") }
            return
        }
        if (paymentLines == null) {
            _uiState.update { it.copy(error = "Revisa los montos de pago") }
            return
        }
        if (!paidCoversTotal(paymentLines.sumOf { it.amount }, payable)) {
            _uiState.update { it.copy(error = "El monto pagado debe cubrir el total") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(checkoutSubmitting = true, error = null) }
            val sessionId = state.activeSessionId ?: ensureSession(state) ?: run {
                _uiState.update { it.copy(checkoutSubmitting = false) }
                return@launch
            }
            if (state.cart.isNotEmpty() && !sendCartItems(sessionId, state)) {
                _uiState.update { it.copy(checkoutSubmitting = false) }
                return@launch
            }
            val methods = state.checkoutMeta?.paymentMethods.orEmpty()
            val needsCashSession = needsOpenCashSessionForPayments(methods, paymentLines)
            val cashSessionId = if (needsCashSession) sessionStore.cashSessionFlow.first()?.sessionId else null
            if (needsCashSession && cashSessionId == null) {
                _uiState.update {
                    it.copy(checkoutSubmitting = false, error = "Abre tu caja para cobrar en efectivo")
                }
                return@launch
            }
            val discountAmount = if (state.allowCheckoutDiscount && state.checkoutDiscountAmount > 0) {
                roundSunat(state.checkoutDiscountAmount)
            } else {
                null
            }
            when (
                val result = billingRepository.billSession(
                    sessionId = sessionId,
                    input = BillSessionInput(
                        seriesId = seriesId,
                        docType = state.checkoutDocType,
                        contactId = contactId,
                        cashSessionId = cashSessionId,
                        closeSession = true,
                        discountAmount = discountAmount,
                        payments = paymentLines,
                    ),
                )
            ) {
                is AppResult.Success -> {
                    val printNote = documentPrintNote(documentPrintService.printSaleDocument(result.data.printData))
                    val hasPrinter = documentPrintService.hasConfiguredPrinter()
                    _uiState.update {
                        it.copy(
                            checkoutSubmitting = false,
                            checkoutOpen = false,
                            checkoutSuccess = result.data,
                            checkoutPrintNote = printNote,
                            receiptHasPrinter = hasPrinter,
                        )
                    }
                    resetSessionState(newType = state.orderType)
                    loadPendingOrders()
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(checkoutSubmitting = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun dismissCheckoutSuccess() {
        _uiState.update { it.copy(checkoutSuccess = null, checkoutPrintNote = null, receiptBusy = null) }
    }

    fun reprintReceipt() {
        val data = _uiState.value.checkoutSuccess?.printData ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(receiptBusy = "print") }
            val ok = documentPrintService.printSaleDocument(data, force = true)
            _uiState.update {
                it.copy(
                    receiptBusy = null,
                    snackMessage = when (ok) {
                        true -> "Comprobante enviado a la ticketera"
                        false -> "No se pudo imprimir"
                        null -> "Configura la impresora en Ajustes"
                    },
                )
            }
        }
    }

    fun shareReceiptViaWhatsApp(context: android.content.Context) {
        val data = _uiState.value.checkoutSuccess?.printData ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(receiptBusy = "share") }
            try {
                val file = receiptPdfService.generate(data, ReceiptPdfFormat.TICKET)
                val uri = receiptPdfService.uriFor(file)
                withContext(Dispatchers.Main) {
                    ReceiptShareHelper.sharePdfWhatsApp(context, uri, "Comprobante ${data.number}")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackMessage = e.message ?: "No se pudo compartir") }
            } finally {
                _uiState.update { it.copy(receiptBusy = null) }
            }
        }
    }

    fun openReceiptPdf(context: android.content.Context, format: ReceiptPdfFormat) {
        val data = _uiState.value.checkoutSuccess?.printData ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(receiptBusy = "pdf") }
            try {
                val file = receiptPdfService.generate(data, format)
                val uri = receiptPdfService.uriFor(file)
                withContext(Dispatchers.Main) {
                    ReceiptShareHelper.openPdfExternal(context, uri)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackMessage = e.message ?: "No se pudo abrir el PDF") }
            } finally {
                _uiState.update { it.copy(receiptBusy = null) }
            }
        }
    }

    // --- Internos ---

    private fun resetSessionState(newType: PosOrderType, snack: String? = null) {
        _uiState.update {
            PosUiState(
                loading = it.loading,
                products = it.products,
                categories = it.categories,
                selectedCategoryId = it.selectedCategoryId,
                searchQuery = it.searchQuery,
                orderType = newType,
                checkoutMeta = it.checkoutMeta,
                pendingOrders = it.pendingOrders,
                deliveryDrivers = it.deliveryDrivers,
                snackMessage = snack,
            )
        }
    }

    private suspend fun loadSession(sessionId: Int) {
        when (val result = mesasRepository.getSession(sessionId)) {
            is AppResult.Success -> applySessionDetail(result.data)
            is AppResult.Error -> _uiState.update { it.copy(error = result.message) }
            AppResult.Loading -> Unit
        }
    }

    private fun applySessionDetail(detail: com.bendey.restaurant.core.domain.restaurant.TableSessionDetail) {
        val orderType = when (detail.orderType?.trim()?.lowercase()) {
            "takeaway" -> PosOrderType.TAKEAWAY
            "delivery" -> PosOrderType.DELIVERY
            else -> PosOrderType.QUICK_SALE
        }
        _uiState.update {
            it.copy(
                activeSessionId = detail.id,
                orderCode = detail.orderCode,
                sessionTotal = detail.totalAmount,
                sessionOrders = detail.orders,
                cart = emptyList(),
                orderType = orderType,
                orderDetails = PosOrderDetails(
                    customerName = detail.customerName.orEmpty(),
                    customerPhone = detail.customerPhone.orEmpty(),
                    orderNotes = detail.notes.orEmpty(),
                    deliveryAddress = detail.deliveryAddress.orEmpty(),
                    deliveryReference = detail.deliveryReference.orEmpty(),
                    deliveryDriverId = detail.deliveryDriverId,
                    estimatedMinutes = (detail.estimatedMinutes ?: 30).toString(),
                ),
            )
        }
    }

    private suspend fun refreshFullSession(sessionId: Int) {
        when (val result = mesasRepository.getSession(sessionId)) {
            is AppResult.Success -> applySessionDetail(result.data)
            else -> Unit
        }
    }

    private fun buildSessionInput(state: PosUiState, saveAsDraft: Boolean = false): PosSessionInput {
        val minutes = state.orderDetails.estimatedMinutes.toIntOrNull()?.coerceAtLeast(5)
        return PosSessionInput(
            orderType = state.orderType.apiValue,
            customerName = state.orderDetails.customerName,
            customerPhone = state.orderDetails.customerPhone,
            deliveryDriverId = state.orderDetails.deliveryDriverId,
            deliveryAddress = state.orderDetails.deliveryAddress,
            deliveryReference = state.orderDetails.deliveryReference,
            estimatedMinutes = minutes,
            notes = state.orderDetails.orderNotes,
            saveAsDraft = saveAsDraft,
        )
    }

    private suspend fun ensureSession(state: PosUiState, saveAsDraft: Boolean = false): Int? {
        val input = buildSessionInput(state, saveAsDraft)
        state.activeSessionId?.let { id ->
            when (val update = posRepository.updatePosSession(id, input)) {
                is AppResult.Success -> return id
                is AppResult.Error -> {
                    _uiState.update { it.copy(error = update.message) }
                    return null
                }
                AppResult.Loading -> return null
            }
        }
        return when (val open = posRepository.openPosSession(input)) {
            is AppResult.Success -> {
                _uiState.update {
                    it.copy(activeSessionId = open.data.sessionId, orderCode = open.data.orderCode)
                }
                open.data.sessionId
            }
            is AppResult.Error -> {
                _uiState.update { it.copy(error = open.message) }
                null
            }
            AppResult.Loading -> null
        }
    }

    private suspend fun sendCartItems(sessionId: Int, state: PosUiState): Boolean {
        val items = state.cart.map { it.toOrderItemInput() }
        return when (val result = posRepository.addOrder(sessionId, items)) {
            is AppResult.Success -> {
                val order = result.data
                val userName = sessionStore.userSessionFlow.first()?.user?.name
                kitchenPrintService.printComandaRound(
                    tableName = state.orderType.label,
                    orderNumber = order.orderNumber,
                    waiterName = userName,
                    comandas = order.comandas,
                )
                true
            }
            is AppResult.Error -> {
                _uiState.update { it.copy(error = result.message) }
                false
            }
            AppResult.Loading -> false
        }
    }

    private fun loadDeliveryDriversIfNeeded() {
        if (_uiState.value.deliveryDrivers.isNotEmpty() || _uiState.value.deliveryDriversLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(deliveryDriversLoading = true) }
            when (val result = posRepository.listDeliveryDrivers()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(deliveryDriversLoading = false, deliveryDrivers = result.data)
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(deliveryDriversLoading = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    private fun loadCheckoutMeta() {
        viewModelScope.launch {
            _uiState.update { it.copy(checkoutMetaLoading = true) }
            val branchId = sessionStore.userSessionFlow.first()?.activeBranch?.id
            if (branchId == null) {
                _uiState.update { it.copy(checkoutMetaLoading = false, error = "Selecciona una sucursal") }
                return@launch
            }
            when (val result = billingRepository.loadCheckoutMeta(branchId)) {
                is AppResult.Success -> {
                    applyCheckoutDefaults(result.data)
                    _uiState.update { it.copy(checkoutMetaLoading = false, checkoutMeta = result.data) }
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(checkoutMetaLoading = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    private fun applyCheckoutDefaults(meta: CheckoutMeta?) {
        if (meta == null) return
        val defaultSeries = pickDefaultNotaVentaSeries(meta.series)
        val defaultContact = pickVariosContactId(meta.contacts)
        val defaultMethod = defaultPaymentMethodCode(meta.paymentMethods)
        _uiState.update { state ->
            state.copy(
                checkoutSeriesId = state.checkoutSeriesId ?: defaultSeries?.id,
                checkoutDocType = defaultSeries?.docType ?: state.checkoutDocType,
                checkoutContactId = state.checkoutContactId ?: defaultContact,
                checkoutPayments = if (state.checkoutPayments.isEmpty()) {
                    listOf(CheckoutPaymentDraft(method = defaultMethod, amount = formatAmount(state.checkoutPayableTotal)))
                } else {
                    state.checkoutPayments
                },
            )
        }
    }

    private fun syncSinglePaymentAmount(state: PosUiState): PosUiState {
        if (state.checkoutPayments.size != 1) return state
        return state.copy(
            checkoutPayments = listOf(
                state.checkoutPayments.first().copy(amount = formatAmount(state.checkoutPayableTotal)),
            ),
        )
    }

    private suspend fun loadProducts(branchId: Int?) {
        val query = _uiState.value.searchQuery.trim()
        val categoryId = _uiState.value.selectedCategoryId
        when (val result = posRepository.loadProducts(query, categoryId, page = 1, branchId)) {
            is AppResult.Success -> _uiState.update {
                it.copy(loading = false, products = result.data.first, error = null)
            }
            is AppResult.Error -> _uiState.update {
                it.copy(loading = false, error = result.message)
            }
            AppResult.Loading -> Unit
        }
    }
}

private fun formatAmount(value: Double): String {
    val rounded = roundMoney(value)
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}

private fun productMatchesBarcode(productCode: String, scanned: String): Boolean {
    val product = productCode.trim()
    val code = scanned.trim()
    if (product.equals(code, ignoreCase = true)) return true
    val stripProduct = product.trimStart('0')
    val stripCode = code.trimStart('0')
    return stripProduct.isNotEmpty() && stripProduct == stripCode
}

private fun documentPrintNote(result: Boolean?): String? = when (result) {
    true -> "Documento enviado a impresora"
    false -> "Documento no impreso · revisa impresora"
    null -> null
}
