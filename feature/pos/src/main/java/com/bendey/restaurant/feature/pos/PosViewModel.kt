package com.bendey.restaurant.feature.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.data.printer.DocumentPrintService
import com.bendey.restaurant.core.data.printer.KitchenPrintService
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
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import com.bendey.restaurant.core.domain.restaurant.MesasRepository
import com.bendey.restaurant.core.domain.restaurant.OrderItemInput
import com.bendey.restaurant.core.domain.restaurant.PosCartLine
import com.bendey.restaurant.core.domain.restaurant.PosProduct
import com.bendey.restaurant.core.domain.catalog.ProductImageRepository
import com.bendey.restaurant.core.domain.restaurant.PosRepository
import com.bendey.restaurant.core.domain.restaurant.ProductCategory
import com.bendey.restaurant.core.domain.session.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.round

enum class PosOrderType(val apiValue: String, val label: String) {
    COUNTER("mostrador", "Mostrador"),
    TAKEAWAY("para_llevar", "Para llevar"),
}

data class PosUiState(
    val loading: Boolean = false,
    val sending: Boolean = false,
    val products: List<PosProduct> = emptyList(),
    val categories: List<ProductCategory> = emptyList(),
    val selectedCategoryId: Int? = null,
    val searchQuery: String = "",
    val cart: List<PosCartLine> = emptyList(),
    val orderType: PosOrderType = PosOrderType.COUNTER,
    val activeSessionId: Int? = null,
    val orderCode: String? = null,
    val sessionTotal: Double = 0.0,
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
    val error: String? = null,
    val snackMessage: String? = null,
) {
    val cartTotal: Double get() = cart.sumOf { it.lineTotal }
    val cartCount: Int get() = cart.sumOf { it.quantity }
    val checkoutRawTotal: Double get() = roundMoney(sessionTotal + cartTotal)

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
    private val sessionStore: UserSessionStore,
    private val productImageRepository: ProductImageRepository,
) : ViewModel() {

    val assetsBaseUrl: String?
        get() = productImageRepository.tenantAssetsBaseUrl()

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    init {
        refreshCatalog()
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
        _uiState.update {
            it.copy(
                orderType = type,
                activeSessionId = null,
                orderCode = null,
                sessionTotal = 0.0,
                cart = emptyList(),
            )
        }
    }

    fun addToCart(product: PosProduct) {
        _uiState.update { state ->
            val existing = state.cart.indexOfFirst { it.product.id == product.id }
            val newCart = if (existing >= 0) {
                state.cart.toMutableList().apply {
                    val line = this[existing]
                    this[existing] = line.copy(quantity = line.quantity + 1)
                }
            } else {
                state.cart + PosCartLine(product = product, quantity = 1)
            }
            state.copy(cart = newCart, snackMessage = "${product.name} agregado")
        }
    }

    fun decrementLine(productId: Int) {
        _uiState.update { state ->
            state.copy(
                cart = state.cart.mapNotNull { line ->
                    when {
                        line.product.id != productId -> line
                        line.quantity <= 1 -> null
                        else -> line.copy(quantity = line.quantity - 1)
                    }
                },
            )
        }
    }

    fun removeLine(productId: Int) {
        _uiState.update { it.copy(cart = it.cart.filter { line -> line.product.id != productId }) }
    }

    fun consumeSnackMessage() {
        _uiState.update { it.copy(snackMessage = null) }
    }

    fun sendComanda() {
        val state = _uiState.value
        if (state.cart.isEmpty()) {
            _uiState.update { it.copy(error = "Agrega productos al carrito") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(sending = true, error = null) }
            val sessionId = ensureSession(state) ?: run {
                _uiState.update { it.copy(sending = false) }
                return@launch
            }
            if (sendCartItems(sessionId, state)) {
                refreshSessionTotal(sessionId)
                _uiState.update {
                    it.copy(
                        sending = false,
                        cart = emptyList(),
                        snackMessage = "Comanda enviada",
                    )
                }
            } else {
                _uiState.update { it.copy(sending = false) }
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
            val cashSessionId = if (needsCashSession) {
                sessionStore.cashSessionFlow.first()?.sessionId
            } else {
                null
            }
            if (needsCashSession && cashSessionId == null) {
                _uiState.update {
                    it.copy(
                        checkoutSubmitting = false,
                        error = "Abre tu caja para cobrar en efectivo (menú Caja)",
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
                    _uiState.update {
                        it.copy(
                            checkoutSubmitting = false,
                            checkoutOpen = false,
                            checkoutSuccess = result.data,
                            checkoutPrintNote = printNote,
                            activeSessionId = null,
                            orderCode = null,
                            sessionTotal = 0.0,
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
        _uiState.update { it.copy(checkoutSuccess = null, checkoutPrintNote = null) }
    }

    private suspend fun ensureSession(state: PosUiState): Int? {
        state.activeSessionId?.let { return it }
        return when (val open = posRepository.openCounterSession(state.orderType.apiValue)) {
            is AppResult.Success -> {
                _uiState.update {
                    it.copy(
                        activeSessionId = open.data.sessionId,
                        orderCode = open.data.orderCode,
                    )
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
        val items = state.cart.map { line ->
            OrderItemInput(
                productId = line.product.id,
                productCode = line.product.code,
                productName = line.product.name,
                quantity = line.quantity.toDouble(),
                unitPrice = line.product.salePrice,
                igvAffectationType = line.product.igvAffectationType,
                priceIncludesIgv = line.product.priceIncludesIgv,
            )
        }
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

    private suspend fun refreshSessionTotal(sessionId: Int) {
        when (val result = mesasRepository.getSession(sessionId)) {
            is AppResult.Success -> _uiState.update { it.copy(sessionTotal = result.data.totalAmount) }
            is AppResult.Error -> Unit
            AppResult.Loading -> Unit
        }
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

    private fun syncSinglePaymentAmount(state: PosUiState): PosUiState {
        if (state.checkoutPayments.size != 1) return state
        return state.copy(
            checkoutPayments = listOf(
                state.checkoutPayments.first().copy(
                    amount = formatAmount(state.checkoutPayableTotal),
                ),
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
