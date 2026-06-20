package com.bendey.restaurant.feature.mesas

import androidx.lifecycle.SavedStateHandle
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
import com.bendey.restaurant.core.domain.catalog.ProductImageRepository
import com.bendey.restaurant.core.domain.restaurant.MesasRepository
import com.bendey.restaurant.core.domain.restaurant.OrderItemInput
import com.bendey.restaurant.core.domain.restaurant.PosCartLine
import com.bendey.restaurant.core.domain.restaurant.PosProduct
import com.bendey.restaurant.core.domain.restaurant.PosRepository
import com.bendey.restaurant.core.domain.restaurant.ProductCategory
import com.bendey.restaurant.core.domain.restaurant.TableSessionDetail
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

data class MesaUiState(
    val loading: Boolean = true,
    val sending: Boolean = false,
    val printingPrecuenta: Boolean = false,
    val session: TableSessionDetail? = null,
    val products: List<PosProduct> = emptyList(),
    val categories: List<ProductCategory> = emptyList(),
    val selectedCategoryId: Int? = null,
    val searchQuery: String = "",
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
    val error: String? = null,
    val snackMessage: String? = null,
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
    private val sessionStore: UserSessionStore,
    private val productImageRepository: ProductImageRepository,
) : ViewModel() {

    val assetsBaseUrl: String?
        get() = productImageRepository.tenantAssetsBaseUrl()

    private val sessionId: Int = checkNotNull(savedStateHandle.get<Int>("sessionId"))

    private val _uiState = MutableStateFlow(MesaUiState())
    val uiState: StateFlow<MesaUiState> = _uiState.asStateFlow()

    init {
        refreshAll()
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

    fun addToCart(product: PosProduct) {
        _uiState.update { state ->
            val index = state.cart.indexOfFirst { it.product.id == product.id }
            val newCart = if (index >= 0) {
                state.cart.toMutableList().apply {
                    val line = this[index]
                    this[index] = line.copy(quantity = line.quantity + 1)
                }
            } else {
                state.cart + PosCartLine(product = product, quantity = 1)
            }
            state.copy(cart = newCart)
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

    fun sendComanda() {
        val state = _uiState.value
        if (state.cart.isEmpty()) {
            _uiState.update { it.copy(error = "Agrega productos al carrito") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(sending = true, error = null) }
            if (sendCartItems(state)) {
                _uiState.update { it.copy(snackMessage = "Comanda enviada") }
            }
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
                session?.permissions.orEmpty(),
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
        when (val result = posRepository.loadProducts(query, categoryId, page = 1, branchId)) {
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
