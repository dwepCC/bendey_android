package com.bendey.restaurant.feature.mesas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.data.feedback.CartFeedback
import com.bendey.restaurant.core.data.printer.DocumentPrintService
import com.bendey.restaurant.core.data.printer.KitchenPrintService
import com.bendey.restaurant.core.data.receipt.ReceiptPdfFormat
import com.bendey.restaurant.core.data.receipt.ReceiptPdfService
import com.bendey.restaurant.core.data.receipt.ReceiptShareHelper
import com.bendey.restaurant.core.data.repository.defaultPaymentMethodCode
import com.bendey.restaurant.core.data.repository.requiresOpenCashSessionForCheckout
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
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import com.bendey.restaurant.core.domain.catalog.ComboFormInput
import com.bendey.restaurant.core.domain.catalog.CombosRepository
import com.bendey.restaurant.core.domain.catalog.ModifierGroup
import com.bendey.restaurant.core.domain.catalog.ModifiersRepository
import com.bendey.restaurant.core.domain.catalog.ProductPresentation
import com.bendey.restaurant.core.domain.pos.allComboFormProductIds
import com.bendey.restaurant.core.domain.pos.appendCartLine
import com.bendey.restaurant.core.domain.pos.buildComboConfigJson
import com.bendey.restaurant.core.domain.pos.buildComboConfigureKey
import com.bendey.restaurant.core.domain.pos.buildConfigureKey
import com.bendey.restaurant.core.domain.pos.calcUnitPriceWithModifiers
import com.bendey.restaurant.core.domain.pos.ComboCartConfig
import com.bendey.restaurant.core.domain.pos.ComboConfigureState
import com.bendey.restaurant.core.domain.pos.comboComponentModifiersList
import com.bendey.restaurant.core.domain.pos.comboNeedsConfiguration
import com.bendey.restaurant.core.domain.pos.selectedComboProductIds
import com.bendey.restaurant.core.domain.pos.decrementCartLine
import com.bendey.restaurant.core.domain.pos.getProductExtraGroups
import com.bendey.restaurant.core.domain.pos.manualCartLine
import com.bendey.restaurant.core.domain.pos.ManualProductInput
import com.bendey.restaurant.core.domain.pos.modifiersToJson
import com.bendey.restaurant.core.domain.pos.modifierSummaryText
import com.bendey.restaurant.core.domain.pos.PosCatalogTab
import com.bendey.restaurant.core.domain.pos.PosComboItem
import com.bendey.restaurant.core.domain.pos.ProductConfigureState
import com.bendey.restaurant.core.domain.pos.productNamesFromCatalog
import com.bendey.restaurant.core.domain.pos.productNeedsConfiguration
import com.bendey.restaurant.core.domain.pos.selectPresentation
import com.bendey.restaurant.core.domain.pos.toggleExtraSelection
import com.bendey.restaurant.core.domain.pos.setSlotOptionQuantity
import com.bendey.restaurant.core.domain.pos.toggleSlotOption
import com.bendey.restaurant.core.domain.pos.validateModifierSelection
import com.bendey.restaurant.core.domain.pos.validateSlotSelections
import com.bendey.restaurant.core.domain.products.ProductsRepository
import com.bendey.restaurant.core.domain.products.toFormInput
import com.bendey.restaurant.core.domain.catalog.ProductImageRepository
import com.bendey.restaurant.core.domain.restaurant.MesasRepository
import com.bendey.restaurant.core.domain.pos.toOrderItemInput
import com.bendey.restaurant.core.domain.restaurant.PosCartLine
import com.bendey.restaurant.core.domain.restaurant.PosProduct
import com.bendey.restaurant.core.domain.restaurant.PosRepository
import com.bendey.restaurant.core.domain.restaurant.ProductCategory
import com.bendey.restaurant.core.domain.restaurant.SessionComandaSummary
import com.bendey.restaurant.core.domain.restaurant.SessionOrderSummary
import com.bendey.restaurant.core.domain.restaurant.TableSessionDetail
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
import javax.inject.Inject
import kotlin.math.round

data class MesaUiState(
    val loading: Boolean = true,
    val sending: Boolean = false,
    val printingPrecuenta: Boolean = false,
    val session: TableSessionDetail? = null,
    val products: List<PosProduct> = emptyList(),
    val categories: List<ProductCategory> = emptyList(),
    val selectedCategoryId: Int? = null,
    val searchQuery: String = "",
    val catalogTab: PosCatalogTab = PosCatalogTab.PRODUCTS,
    val combos: List<PosComboItem> = emptyList(),
    val productConfigure: ProductConfigureState? = null,
    val comboConfigure: ComboConfigureState? = null,
    val cart: List<PosCartLine> = emptyList(),
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
    val reprintingOrderId: Int? = null,
    val reprintingAll: Boolean = false,
    val closingMesa: Boolean = false,
    val voidComanda: SessionComandaSummary? = null,
    val voidReason: String = "",
    val voidPin: String = "",
    val voidSubmitting: Boolean = false,
    val error: String? = null,
    val snackMessage: String? = null,
    val canChargeOrders: Boolean = false,
    val canAnularComanda: Boolean = false,
    val canOperateCash: Boolean = false,
) {
    val cartTotal: Double get() = cart.sumOf { it.lineTotal }
    val cartCount: Int get() = cart.sumOf { it.quantity }
    val sessionTotal: Double get() = session?.totalAmount ?: 0.0
    val checkoutRawTotal: Double get() = roundMoney(sessionTotal + cartTotal)

    private val discountNumeric: Double
        get() = checkoutDiscountValue.replace(',', '.').trim().toDoubleOrNull() ?: 0.0

    val checkoutDiscountAmount: Double
        get() = calcCheckoutDiscountAmount(checkoutRawTotal, checkoutDiscountMode, discountNumeric)

    val checkoutPayableTotal: Double
        get() = calcPayableTotal(checkoutRawTotal, checkoutDiscountMode, discountNumeric)

    val sessionOrders: List<SessionOrderSummary> get() = session?.orders.orEmpty()
    val hasSentComandas: Boolean get() = sessionOrders.any { it.comandas.isNotEmpty() }
    val canClearCart: Boolean get() = cart.isNotEmpty() && !hasSentComandas
    val canCloseMesa: Boolean get() =
        canChargeOrders && sessionTotal <= 0 && cart.isEmpty() && !checkoutSubmitting && !sending
}

private fun roundMoney(value: Double): Double = round(value * 100.0) / 100.0

@HiltViewModel
class MesaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mesasRepository: MesasRepository,
    private val posRepository: PosRepository,
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

    private val sessionId: Int = checkNotNull(savedStateHandle.get<Int>("sessionId"))

    private val _uiState = MutableStateFlow(MesaUiState())
    val uiState: StateFlow<MesaUiState> = _uiState.asStateFlow()

    init {
        refreshAll()
        warmCheckoutMeta()
        viewModelScope.launch {
            sessionStore.userSessionFlow.collect { session ->
                val perms = session?.restaurantPermissions.orEmpty()
                val employeeType = session?.user?.employeeType
                _uiState.update {
                    it.copy(
                        canChargeOrders = RestaurantPermissions.canChargeOrders(perms),
                        canAnularComanda = RestaurantPermissions.canAnularComanda(perms),
                        canOperateCash = RestaurantPermissions.canChargeCashByRole(employeeType, perms),
                    )
                }
            }
        }
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

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            loadSession()
            loadCatalog()
            _uiState.update { it.copy(loading = false) }
        }
    }

    fun refreshSession() {
        viewModelScope.launch { loadSession() }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch { loadProducts() }
    }

    fun selectCategory(categoryId: Int?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
        viewModelScope.launch { loadProducts() }
    }

    fun setCatalogTab(tab: PosCatalogTab) {
        _uiState.update { it.copy(catalogTab = tab) }
        if (tab == PosCatalogTab.COMBOS && _uiState.value.combos.isEmpty()) loadCombos()
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

    fun addToCart(product: PosProduct) = onProductClick(product)

    fun onProductClick(product: PosProduct) {
        if (!product.availableForSale) {
            _uiState.update { it.copy(error = "${product.name} no está disponible") }
            return
        }
        if (productNeedsConfiguration(product)) openProductConfigure(product) else addSimpleProduct(product)
    }

    fun onComboClick(combo: PosComboItem) {
        if (comboNeedsConfiguration(combo)) openComboConfigure(combo)
        else viewModelScope.launch { resolveAndAddCombo(combo, ComboCartConfig()) }
    }

    private fun openProductConfigure(product: PosProduct) {
        _uiState.update { it.copy(productConfigure = ProductConfigureState(product = product), error = null) }
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
                        ?.let { selectPresentation(emptyList(), it) } ?: emptyList()
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

    fun dismissProductConfigure() { _uiState.update { it.copy(productConfigure = null) } }
    fun updateProductConfigureNote(note: String) {
        _uiState.update { it.copy(productConfigure = it.productConfigure?.copy(kitchenNote = note, error = null)) }
    }
    fun selectProductPresentation(presentation: ProductPresentation) {
        _uiState.update { state ->
            val cfg = state.productConfigure ?: return@update state
            state.copy(productConfigure = cfg.copy(selected = selectPresentation(cfg.selected, presentation), error = null))
        }
    }
    fun toggleProductExtra(group: ModifierGroup, optionId: Int) {
        _uiState.update { state ->
            val cfg = state.productConfigure ?: return@update state
            state.copy(productConfigure = cfg.copy(selected = toggleExtraSelection(cfg.selected, group, optionId), error = null))
        }
    }
    fun confirmProductConfigure() {
        val cfg = _uiState.value.productConfigure ?: return
        val validation = validateModifierSelection(cfg.presentations, cfg.extraGroups, cfg.selected, cfg.product)
        if (validation != null) {
            _uiState.update { it.copy(productConfigure = cfg.copy(error = validation)) }
            return
        }
        val unitPrice = calcUnitPriceWithModifiers(cfg.product.salePrice, cfg.selected)
        val line = PosCartLine(
            cartKey = buildConfigureKey(cfg.selected, cfg.kitchenNote),
            product = cfg.product,
            quantity = 1,
            notes = cfg.kitchenNote,
            unitPrice = unitPrice,
            modifiersJson = modifiersToJson(cfg.selected).takeIf { it.isNotBlank() },
            subtitle = modifierSummaryText(cfg.selected).takeIf { it.isNotBlank() },
        )
        cartFeedback.playAddToCart()
        _uiState.update {
            it.copy(cart = appendCartLine(it.cart, line), productConfigure = null)
        }
    }

    private fun openComboConfigure(combo: PosComboItem) {
        _uiState.update { it.copy(comboConfigure = ComboConfigureState(combo = combo), error = null) }
        viewModelScope.launch {
            when (val result = combosRepository.getCombo(combo.id)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(comboConfigure = it.comboConfigure?.copy(loading = false, form = result.data))
                    }
                    loadComboProductNames(result.data)
                    loadComboComponentModifierGroups()
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(comboConfigure = it.comboConfigure?.copy(loading = false, error = result.message))
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun dismissComboConfigure() { _uiState.update { it.copy(comboConfigure = null) } }
    fun updateComboConfigureNote(note: String) {
        _uiState.update { it.copy(comboConfigure = it.comboConfigure?.copy(kitchenNote = note, error = null)) }
    }
    fun toggleComboSlot(slot: com.bendey.restaurant.core.domain.catalog.ComboSlot, optionId: Int) {
        _uiState.update { state ->
            val cfg = state.comboConfigure ?: return@update state
            state.copy(comboConfigure = cfg.copy(selections = toggleSlotOption(cfg.selections, slot, optionId), error = null))
        }
        previewComboPrice()
        loadComboComponentModifierGroups()
    }
    fun setComboSlotQuantity(slot: com.bendey.restaurant.core.domain.catalog.ComboSlot, optionId: Int, quantity: Int) {
        _uiState.update { state ->
            val cfg = state.comboConfigure ?: return@update state
            state.copy(comboConfigure = cfg.copy(selections = setSlotOptionQuantity(cfg.selections, slot, optionId, quantity), error = null))
        }
        previewComboPrice()
        loadComboComponentModifierGroups()
    }
    fun toggleComboComponentModifier(productId: Int, group: ModifierGroup, optionId: Int) {
        _uiState.update { state ->
            val cfg = state.comboConfigure ?: return@update state
            val current = cfg.componentModifiers[productId].orEmpty()
            val updated = toggleExtraSelection(current, group, optionId)
            state.copy(
                comboConfigure = cfg.copy(
                    componentModifiers = cfg.componentModifiers + (productId to updated),
                    error = null,
                ),
            )
        }
        previewComboPrice()
    }
    fun selectComboComponentPresentation(productId: Int, presentation: ProductPresentation) {
        _uiState.update { state ->
            val cfg = state.comboConfigure ?: return@update state
            val current = cfg.componentModifiers[productId].orEmpty()
            state.copy(
                comboConfigure = cfg.copy(
                    componentModifiers = cfg.componentModifiers + (productId to selectPresentation(current, presentation)),
                    error = null,
                ),
            )
        }
        previewComboPrice()
    }
    private fun loadComboProductNames(form: ComboFormInput) {
        viewModelScope.launch {
            val allIds = allComboFormProductIds(form)
            if (allIds.isEmpty()) return@launch
            val namesMap = productNamesFromCatalog(allIds, _uiState.value.products).toMutableMap()
            for (productId in allIds) {
                if (namesMap.containsKey(productId)) continue
                when (val detail = productsRepository.getProductDetail(productId)) {
                    is AppResult.Success -> namesMap[productId] = detail.data.toFormInput().name
                    else -> Unit
                }
            }
            _uiState.update { state ->
                val cfg = state.comboConfigure ?: return@update state
                state.copy(
                    comboConfigure = cfg.copy(
                        componentProductNames = cfg.componentProductNames + namesMap,
                    ),
                )
            }
        }
    }

    private fun loadComboComponentModifierGroups() {
        viewModelScope.launch {
            val cfg = _uiState.value.comboConfigure ?: return@launch
            val form = cfg.form ?: return@launch
            val productIds = selectedComboProductIds(form, cfg.selections)
            if (productIds.isEmpty()) {
                _uiState.update {
                    it.copy(
                        comboConfigure = it.comboConfigure?.copy(
                            componentModifierGroups = emptyMap(),
                            componentPresentations = emptyMap(),
                        ),
                    )
                }
                return@launch
            }
            val groupsResult = modifiersRepository.listModifierGroups()
            if (groupsResult !is AppResult.Success) return@launch
            val groupsMap = mutableMapOf<Int, List<ModifierGroup>>()
            val namesMap = mutableMapOf<Int, String>()
            val presentationsMap = mutableMapOf<Int, List<ProductPresentation>>()
            for (productId in productIds) {
                when (val detail = productsRepository.getProductDetail(productId)) {
                    is AppResult.Success -> {
                        val productForm = detail.data.toFormInput()
                        namesMap[productId] = productForm.name
                        val presentations = detail.data.presentations.filter { it.active && it.name.isNotBlank() }
                        if (presentations.isNotEmpty()) {
                            presentationsMap[productId] = presentations
                        }
                        val stub = PosProduct(
                            id = productId,
                            code = "",
                            name = productForm.name,
                            salePrice = 0.0,
                            categoryId = null,
                            imageUrl = null,
                            igvAffectationType = null,
                            priceIncludesIgv = null,
                            hasModifiers = productForm.hasModifiers,
                            hasVariants = productForm.hasVariants,
                        )
                        if (stub.hasModifiers) {
                            groupsMap[productId] = getProductExtraGroups(
                                productForm.modifierGroupIds,
                                groupsResult.data,
                                stub,
                            )
                        }
                    }
                    else -> Unit
                }
            }
            _uiState.update { state ->
                val current = state.comboConfigure ?: return@update state
                state.copy(
                    comboConfigure = current.copy(
                        componentModifierGroups = groupsMap,
                        componentProductNames = current.componentProductNames + namesMap,
                        componentPresentations = presentationsMap,
                    ),
                )
            }
        }
    }
    private fun previewComboPrice() {
        val cfg = _uiState.value.comboConfigure ?: return
        if (cfg.form == null) return
        viewModelScope.launch {
            val branchId = sessionStore.userSessionFlow.first()?.activeBranch?.id ?: return@launch
            val configJson = buildComboConfigJson(
                ComboCartConfig(
                    selections = cfg.selections,
                    componentModifiers = comboComponentModifiersList(cfg.componentModifiers),
                ),
            )
            when (val result = combosRepository.resolveCombo(cfg.combo.id, branchId, configJson)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(comboConfigure = it.comboConfigure?.copy(resolvedPrice = result.data.unitPrice))
                }
                else -> Unit
            }
        }
    }
    fun confirmComboConfigure() {
        val cfg = _uiState.value.comboConfigure ?: return
        val validation = cfg.form?.slots?.let { validateSlotSelections(it, cfg.selections) }
        if (validation != null) {
            _uiState.update { it.copy(comboConfigure = cfg.copy(error = validation)) }
            return
        }
        viewModelScope.launch {
            resolveAndAddCombo(
                cfg.combo,
                ComboCartConfig(
                    selections = cfg.selections,
                    componentModifiers = comboComponentModifiersList(cfg.componentModifiers),
                ),
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
        val branchId = sessionStore.userSessionFlow.first()?.activeBranch?.id ?: run {
            _uiState.update { it.copy(error = "Selecciona una sucursal") }
            return
        }
        val configJson = buildComboConfigJson(config)
        val resolved = when (val result = combosRepository.resolveCombo(combo.id, branchId, configJson)) {
            is AppResult.Success -> result.data
            is AppResult.Error -> {
                _uiState.update { it.copy(error = result.message) }
                return
            }
            AppResult.Loading -> return
        }
        val unitPrice = knownPrice ?: resolved.unitPrice
        val line = PosCartLine(
            cartKey = buildComboConfigureKey(combo.id, config, notes, unitPrice),
            product = PosProduct(
                id = -combo.id,
                code = "COMBO-${combo.id}",
                name = combo.name,
                salePrice = unitPrice,
                categoryId = null,
                imageUrl = combo.imageUrl,
                igvAffectationType = null,
                priceIncludesIgv = true,
            ),
            quantity = 1,
            notes = notes,
            itemKind = "combo",
            unitPrice = unitPrice,
            comboId = combo.id,
            comboConfigJson = configJson,
            comboSummaryLines = resolved.summaryLines,
        )
        cartFeedback.playAddToCart()
        _uiState.update { it.copy(cart = appendCartLine(it.cart, line)) }
    }

    private fun addSimpleProduct(product: PosProduct) {
        cartFeedback.playAddToCart()
        _uiState.update { state ->
            state.copy(
                cart = appendCartLine(state.cart, PosCartLine(cartKey = "p-${product.id}", product = product, quantity = 1)),
                error = null,
            )
        }
    }

    fun incrementCartLine(line: PosCartLine) {
        cartFeedback.playAddToCart()
        _uiState.update { state ->
            state.copy(cart = appendCartLine(state.cart, line.copy(quantity = 1)))
        }
    }

    fun decrementLine(cartKey: String) {
        _uiState.update { state ->
            state.copy(cart = decrementCartLine(state.cart, cartKey))
        }
    }

    fun removeLine(cartKey: String) {
        _uiState.update { it.copy(cart = it.cart.filter { line -> line.key != cartKey }) }
    }

    fun clearCart() {
        if (!_uiState.value.canClearCart) return
        cartFeedback.playAddToCart()
        _uiState.update { it.copy(cart = emptyList(), snackMessage = "Carrito vaciado") }
    }

    fun openVoidComanda(comanda: SessionComandaSummary) {
        if (!_uiState.value.canAnularComanda) return
        _uiState.update {
            it.copy(voidComanda = comanda, voidReason = "", voidPin = "", error = null)
        }
    }

    fun dismissVoidDialog() {
        if (_uiState.value.voidSubmitting) return
        _uiState.update { it.copy(voidComanda = null, voidReason = "", voidPin = "") }
    }

    fun setVoidReason(reason: String) {
        _uiState.update { it.copy(voidReason = reason) }
    }

    fun setVoidPin(pin: String) {
        _uiState.update { it.copy(voidPin = pin.filter { it.isDigit() }.take(6)) }
    }

    fun confirmVoidComanda() {
        val state = _uiState.value
        val comanda = state.voidComanda ?: return
        val reason = state.voidReason.trim()
        val pin = state.voidPin.trim()
        if (reason.isBlank() || pin.isBlank()) {
            _uiState.update { it.copy(error = "Indique motivo y PIN") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(voidSubmitting = true, error = null) }
            when (val result = posRepository.cancelComanda(comanda.id, reason, pin)) {
                is AppResult.Success -> {
                    loadSession()
                    _uiState.update {
                        it.copy(
                            voidSubmitting = false,
                            voidComanda = null,
                            voidReason = "",
                            voidPin = "",
                            snackMessage = "Comanda anulada",
                        )
                    }
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(voidSubmitting = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun closeMesa(onSuccess: () -> Unit) {
        if (!_uiState.value.canCloseMesa) return
        viewModelScope.launch {
            _uiState.update { it.copy(closingMesa = true, error = null) }
            when (val result = mesasRepository.closeSession(sessionId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(closingMesa = false, snackMessage = "Mesa cerrada") }
                    onSuccess()
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(closingMesa = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun addManualProduct(input: ManualProductInput) {
        val price = input.unitPrice.replace(",", ".").trim().toDoubleOrNull()
        if (input.description.trim().isBlank() || price == null || price < 0) {
            _uiState.update { it.copy(error = "Completa descripción y precio válido") }
            return
        }
        val line = manualCartLine(input)
        _uiState.update { it.copy(cart = appendCartLine(it.cart, line), error = null) }
        cartFeedback.playAddToCart()
    }

    fun sendComanda() {
        val state = _uiState.value
        if (state.cart.isEmpty()) {
            _uiState.update { it.copy(error = "Agrega productos al carrito") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(sending = true, error = null) }
            sendCartItems(state)
        }
    }

    fun printPrecuenta() {
        viewModelScope.launch {
            _uiState.update { it.copy(printingPrecuenta = true, error = null) }
            when (val result = mesasRepository.getPrecuenta(sessionId)) {
                is AppResult.Success -> {
                    val printed = kitchenPrintService.printPrecuenta(result.data)
                    _uiState.update {
                        it.copy(
                            printingPrecuenta = false,
                            snackMessage = when (printed) {
                                true -> "Precuenta enviada a impresora"
                                false -> "Error al imprimir precuenta"
                                null -> "Configura impresora en Ajustes"
                            },
                        )
                    }
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(printingPrecuenta = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun openCheckout() {
        val state = _uiState.value
        if (state.checkoutRawTotal <= 0) {
            _uiState.update { it.copy(error = "No hay monto por cobrar") }
            return
        }
        viewModelScope.launch {
            val session = sessionStore.userSessionFlow.first()
            val allowDiscount = RestaurantPermissions.canApplyCheckoutDiscount(
                session?.restaurantPermissions.orEmpty(),
                session?.user?.employeeType,
            )
            val method = defaultPaymentMethodCode(state.checkoutMeta?.paymentMethods.orEmpty())
            _uiState.update {
                it.copy(
                    checkoutOpen = true,
                    allowCheckoutDiscount = allowDiscount,
                    checkoutDiscountMode = CheckoutDiscountMode.PERCENT,
                    checkoutDiscountValue = if (allowDiscount) "0" else "0",
                    checkoutPayments = listOf(
                        CheckoutPaymentDraft(
                            method = method,
                            amount = formatAmount(it.checkoutPayableTotal),
                        ),
                    ),
                    error = null,
                )
            }
            if (state.checkoutMeta == null) {
                loadCheckoutMeta()
            } else {
                applyCheckoutDefaults(state.checkoutMeta)
            }
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
        val paid = paymentLines.sumOf { it.amount }
        if (!paidCoversTotal(paid, payable)) {
            _uiState.update { it.copy(error = "El monto pagado debe cubrir el total") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(checkoutSubmitting = true, error = null) }
            if (state.cart.isNotEmpty()) {
                val sent = sendCartItems(state)
                if (!sent) {
                    _uiState.update { it.copy(checkoutSubmitting = false) }
                    return@launch
                }
            }
            val methods = state.checkoutMeta?.paymentMethods.orEmpty()
            val needsCashSession = requiresOpenCashSessionForCheckout(
                canOperateCash = state.canOperateCash,
                methods = methods,
                payments = paymentLines,
            )
            val cashSessionId = if (needsCashSession) {
                sessionStore.cashSessionFlow.first()?.sessionId
            } else {
                null
            }
            if (needsCashSession && cashSessionId == null) {
                _uiState.update {
                    it.copy(
                        checkoutSubmitting = false,
                        checkoutOpen = false,
                        error = "Abre tu caja para cobrar",
                    )
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
                            cart = emptyList(),
                        )
                    }
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(checkoutSubmitting = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun dismissCheckoutSuccess() {
        _uiState.update {
            it.copy(
                checkoutSuccess = null,
                checkoutPrintNote = null,
                receiptBusy = null,
            )
        }
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
                        false -> "No se pudo imprimir · revisa la impresora"
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
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    ReceiptShareHelper.sharePdfWhatsApp(
                        context,
                        uri,
                        "Comprobante ${data.number}",
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackMessage = e.message ?: "No se pudo compartir") }
            } finally {
                _uiState.update { it.copy(receiptBusy = null) }
            }
        }
    }

    fun loadReceiptPdf(format: ReceiptPdfFormat, onReady: (File?) -> Unit) {
        val data = _uiState.value.checkoutSuccess?.printData
        if (data == null) {
            onReady(null)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = receiptPdfService.generate(data, format)
                kotlinx.coroutines.withContext(Dispatchers.Main) { onReady(file) }
            } catch (_: Exception) {
                kotlinx.coroutines.withContext(Dispatchers.Main) { onReady(null) }
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
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    ReceiptShareHelper.openPdfExternal(context, uri)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackMessage = e.message ?: "No se pudo abrir el PDF") }
            } finally {
                _uiState.update { it.copy(receiptBusy = null) }
            }
        }
    }

    fun reprintComanda(order: SessionOrderSummary) {
        val session = _uiState.value.session ?: return
        if (order.comandas.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(reprintingOrderId = order.id) }
            val userName = sessionStore.userSessionFlow.first()?.user?.name
            val result = kitchenPrintService.reprintComandaRound(
                tableName = session.tableName,
                orderNumber = order.orderNumber,
                waiterName = session.waiterName ?: userName,
                comandas = order.comandas.map { it.toComandaLine() },
            )
            _uiState.update {
                it.copy(
                    reprintingOrderId = null,
                    snackMessage = when (result) {
                        true -> "Comanda #${order.orderNumber} reimpresa"
                        false -> "No se pudo reimprimir · revisa impresora de comandas"
                        null -> "Configura la impresora de comandas en Ajustes"
                    },
                )
            }
        }
    }

    fun reprintAllComandas() {
        val session = _uiState.value.session ?: return
        val orders = session.orders.filter { it.comandas.isNotEmpty() }
        if (orders.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(reprintingAll = true) }
            val userName = sessionStore.userSessionFlow.first()?.user?.name
            val result = kitchenPrintService.reprintAllComandaRounds(
                tableName = session.tableName,
                waiterName = session.waiterName ?: userName,
                orders = orders.map { it.orderNumber to it.comandas.map { c -> c.toComandaLine() } },
            )
            _uiState.update {
                it.copy(
                    reprintingAll = false,
                    snackMessage = when (result) {
                        true -> "${orders.size} comanda(s) reimpresa(s)"
                        false -> "Algunas comandas no se imprimieron · revisa impresora"
                        null -> "Configura la impresora de comandas en Ajustes"
                    },
                )
            }
        }
    }

    fun consumeSnackMessage() {
        _uiState.update { it.copy(snackMessage = null) }
    }

    private fun loadCheckoutMeta() {
        viewModelScope.launch {
            _uiState.update { it.copy(checkoutMetaLoading = true) }
            val branchId = sessionStore.userSessionFlow.first()?.activeBranch?.id
            if (branchId == null) {
                _uiState.update {
                    it.copy(checkoutMetaLoading = false, error = "Selecciona una sucursal")
                }
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
                    listOf(
                        CheckoutPaymentDraft(
                            method = defaultMethod,
                            amount = formatAmount(state.checkoutPayableTotal),
                        ),
                    )
                } else {
                    state.checkoutPayments
                },
            )
        }
    }

    private fun syncSinglePaymentAmount(state: MesaUiState): MesaUiState {
        if (state.checkoutPayments.size != 1) return state
        return state.copy(
            checkoutPayments = listOf(
                state.checkoutPayments.first().copy(
                    amount = formatAmount(state.checkoutPayableTotal),
                ),
            ),
        )
    }

    private suspend fun sendCartItems(state: MesaUiState): Boolean {
        val items = state.cart.map { it.toOrderItemInput() }
        return when (val result = posRepository.addOrder(sessionId, items)) {
            is AppResult.Success -> {
                val order = result.data
                val session = state.session
                val userName = sessionStore.userSessionFlow.first()?.user?.name
                kitchenPrintService.printComandaRound(
                    tableName = session?.tableName,
                    orderNumber = order.orderNumber,
                    waiterName = session?.waiterName ?: userName,
                    comandas = order.comandas,
                )
                loadSession()
                _uiState.update { it.copy(cart = emptyList()) }
                true
            }
            is AppResult.Error -> {
                _uiState.update { it.copy(error = result.message, sending = false) }
                false
            }
            AppResult.Loading -> false
        }.also {
            _uiState.update { state -> state.copy(sending = false) }
        }
    }

    private suspend fun loadSession() {
        when (val result = mesasRepository.getSession(sessionId)) {
            is AppResult.Success -> _uiState.update { it.copy(session = result.data) }
            is AppResult.Error -> _uiState.update { it.copy(error = result.message) }
            AppResult.Loading -> Unit
        }
    }

    private suspend fun loadCatalog() {
        when (val categoriesResult = posRepository.loadCategories()) {
            is AppResult.Success -> _uiState.update { it.copy(categories = categoriesResult.data) }
            is AppResult.Error -> _uiState.update { it.copy(error = categoriesResult.message) }
            AppResult.Loading -> Unit
        }
        loadProducts()
    }

    private suspend fun loadProducts() {
        val branchId = sessionStore.userSessionFlow.first()?.activeBranch?.id
        val query = _uiState.value.searchQuery.trim()
        val categoryId = _uiState.value.selectedCategoryId
        when (val result = posRepository.loadProducts(
            query = query,
            categoryId = categoryId,
            page = 1,
            branchId = branchId,
            catalogOnly = true,
        )) {
            is AppResult.Success -> _uiState.update { it.copy(products = result.data.first) }
            is AppResult.Error -> _uiState.update { it.copy(error = result.message) }
            AppResult.Loading -> Unit
        }
    }

    private fun formatAmount(value: Double): String {
        val rounded = roundMoney(value)
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    }
}

private fun documentPrintNote(result: Boolean?): String? = when (result) {
    true -> "Documento enviado a impresora"
    false -> "Documento no impreso · revisa impresora"
    null -> null
}
