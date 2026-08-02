package com.bendey.restaurant.feature.compras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.billing.BillingRepository
import com.bendey.restaurant.core.domain.billing.DEFAULT_TAX_RATE_PERCENT
import com.bendey.restaurant.core.domain.billing.ItemTaxBreakdown
import com.bendey.restaurant.core.domain.billing.TaxConfig
import com.bendey.restaurant.core.domain.billing.calcItem
import com.bendey.restaurant.core.domain.billing.resolveTaxRatePercent
import com.bendey.restaurant.core.domain.contacts.ContactsRepository
import com.bendey.restaurant.core.domain.contacts.CustomerContact
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.products.ProductItem
import com.bendey.restaurant.core.domain.products.ProductType
import com.bendey.restaurant.core.domain.products.ProductsRepository
import com.bendey.restaurant.core.domain.purchases.CreatePurchaseInput
import com.bendey.restaurant.core.domain.purchases.PURCHASE_DOC_TYPES
import com.bendey.restaurant.core.domain.purchases.PURCHASE_PAYMENT_METHODS
import com.bendey.restaurant.core.domain.purchases.PURCHASE_STATUS_FILTERS
import com.bendey.restaurant.core.domain.purchases.Purchase
import com.bendey.restaurant.core.domain.purchases.PurchaseDetail
import com.bendey.restaurant.core.domain.purchases.PurchaseItem
import com.bendey.restaurant.core.domain.purchases.PurchaseListParams
import com.bendey.restaurant.core.domain.purchases.PurchasesRepository
import com.bendey.restaurant.core.domain.session.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SUPPLIER_TYPE = "supplier"
private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private fun todayPeru(): String = LocalDate.now().format(dateFormatter)

data class ComprasFormState(
    val contactId: Int? = null,
    val docType: String = "FACTURA",
    val series: String = "",
    val number: String = "",
    val issueDate: String = todayPeru(),
    val paymentMethod: String = "efectivo",
    val items: List<PurchaseItem> = emptyList(),
    /** Default para ítems nuevos — independiente del price_includes_igv de VENTA del producto
     * (ese es el criterio del precio al cliente, no el de la factura del proveedor). */
    val defaultPriceIncludesIgv: Boolean = true,
) {
    private fun itemBreakdown(item: PurchaseItem, taxRate: Double, taxConfig: TaxConfig): ItemTaxBreakdown =
        calcItem(item.unitCost, item.quantity, 0.0, item.igvAffectationType, item.priceIncludesIgv, taxRate, taxConfig)

    fun itemTotal(item: PurchaseItem, taxRate: Double, taxConfig: TaxConfig): Double =
        itemBreakdown(item, taxRate, taxConfig).total

    fun subtotal(taxRate: Double, taxConfig: TaxConfig): Double =
        items.sumOf { itemBreakdown(it, taxRate, taxConfig).subtotal }

    fun igv(taxRate: Double, taxConfig: TaxConfig): Double =
        items.sumOf { itemBreakdown(it, taxRate, taxConfig).taxAmount }

    fun total(taxRate: Double, taxConfig: TaxConfig): Double =
        subtotal(taxRate, taxConfig) + igv(taxRate, taxConfig)
}

data class ComprasUiState(
    val loading: Boolean = false,
    /** true = sesión de PIN (turno): el backend rechaza /api/purchases; ver ComprasScreen. */
    val accessDenied: Boolean = false,
    val purchases: List<Purchase> = emptyList(),
    val searchQuery: String = "",
    val dateFrom: String = "",
    val dateTo: String = "",
    val statusFilter: String = "",
    val formOpen: Boolean = false,
    val saving: Boolean = false,
    val suppliers: List<CustomerContact> = emptyList(),
    val form: ComprasFormState = ComprasFormState(),
    val taxRate: Double = DEFAULT_TAX_RATE_PERCENT,
    val taxConfig: TaxConfig = TaxConfig(),
    val productPickerOpen: Boolean = false,
    val productSearchQuery: String = "",
    val productSearching: Boolean = false,
    val productResults: List<ProductItem> = emptyList(),
    val detail: PurchaseDetail? = null,
    val detailLoading: Boolean = false,
    val voidTargetId: Int? = null,
    val voiding: Boolean = false,
    val error: String? = null,
    val snackMessage: String? = null,
) {
    val docTypeOptions: List<String> get() = PURCHASE_DOC_TYPES
    val paymentMethodOptions: List<String> get() = PURCHASE_PAYMENT_METHODS
    val statusFilterOptions: List<Pair<String, String>> get() = PURCHASE_STATUS_FILTERS
}

@OptIn(FlowPreview::class)
@HiltViewModel
class ComprasViewModel @Inject constructor(
    private val purchasesRepository: PurchasesRepository,
    private val contactsRepository: ContactsRepository,
    private val productsRepository: ProductsRepository,
    private val billingRepository: BillingRepository,
    private val sessionStore: UserSessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComprasUiState())
    val uiState: StateFlow<ComprasUiState> = _uiState.asStateFlow()

    private val productSearchFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            // Compras exige permisos de rol que el backend SOLO emite en login completo
            // (usuario/contraseña); un token de PIN de turno nunca los recibe. Si detectamos PIN,
            // no llamamos a /api/purchases (evita 403 sordos) y mostramos un aviso claro.
            // staffId NO sirve para esto: se llena también en login completo cuando el usuario
            // tiene un registro de staff activo para poder usar PIN. authMethod == "pin" es la
            // única señal confiable (el backend solo la emite en el login por PIN).
            val session = sessionStore.userSessionFlow.first()
            val isPinSession = session?.user?.isPinSession == true
            _uiState.update { it.copy(accessDenied = isPinSession) }
            if (!isPinSession) {
                refresh()
                loadTaxConfig()
            }
        }
        viewModelScope.launch {
            productSearchFlow
                .debounce(400)
                .distinctUntilChanged()
                .collect { query -> searchProducts(query) }
        }
    }

    /** Tasa real del tenant (18%, 10.5% zona selva, exonerado) — mismo dato que usa el checkout. */
    private fun loadTaxConfig() {
        viewModelScope.launch {
            val branchId = sessionStore.userSessionFlow.first()?.activeBranch?.id ?: return@launch
            when (val result = billingRepository.loadCheckoutMeta(branchId)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        taxRate = resolveTaxRatePercent(result.data.taxRate),
                        taxConfig = TaxConfig(
                            taxRate = resolveTaxRatePercent(result.data.taxRate),
                            igvRegime = result.data.igvRegime,
                            taxBenefitZone = result.data.taxBenefitZone,
                        ),
                    )
                }
                is AppResult.Error -> Unit
                AppResult.Loading -> Unit
            }
        }
    }

    fun refresh() {
        if (_uiState.value.accessDenied) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val state = _uiState.value
            val params = PurchaseListParams(
                query = state.searchQuery,
                dateFrom = state.dateFrom.takeIf { it.isNotBlank() },
                dateTo = state.dateTo.takeIf { it.isNotBlank() },
                status = state.statusFilter.takeIf { it.isNotBlank() },
            )
            when (val result = purchasesRepository.listPurchases(params)) {
                is AppResult.Success -> _uiState.update { it.copy(loading = false, purchases = result.data) }
                is AppResult.Error -> _uiState.update { it.copy(loading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun setSearchQuery(value: String) {
        _uiState.update { it.copy(searchQuery = value) }
        refresh()
    }

    fun setDateRange(from: String, to: String) {
        _uiState.update { it.copy(dateFrom = from, dateTo = to) }
        refresh()
    }

    fun setStatusFilter(value: String) {
        _uiState.update { it.copy(statusFilter = value) }
        refresh()
    }

    fun openForm() {
        _uiState.update {
            it.copy(formOpen = true, form = ComprasFormState(issueDate = todayPeru()), error = null)
        }
        viewModelScope.launch {
            when (val result = contactsRepository.listCustomers(type = SUPPLIER_TYPE)) {
                is AppResult.Success -> _uiState.update { it.copy(suppliers = result.data) }
                is AppResult.Error -> _uiState.update { it.copy(error = result.message, suppliers = emptyList()) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun dismissForm() {
        _uiState.update { it.copy(formOpen = false) }
    }

    fun updateForm(transform: (ComprasFormState) -> ComprasFormState) {
        _uiState.update { it.copy(form = transform(it.form)) }
    }

    fun openProductPicker() {
        _uiState.update { it.copy(productPickerOpen = true, productSearchQuery = "", productResults = emptyList()) }
    }

    fun dismissProductPicker() {
        _uiState.update { it.copy(productPickerOpen = false) }
    }

    fun setProductSearchQuery(value: String) {
        _uiState.update { it.copy(productSearchQuery = value) }
        productSearchFlow.value = value
    }

    private fun searchProducts(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(productSearching = true) }
            when (val result = productsRepository.searchForComboEditor(query)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        productSearching = false,
                        // Un "elaborado" nunca tiene stock propio (se descuenta vía receta al
                        // vender): no tiene sentido comprarlo directamente.
                        productResults = result.data.first.filter { p -> p.productType != ProductType.ELABORADO },
                    )
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(productSearching = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun addProductToItems(product: ProductItem) {
        _uiState.update { state ->
            val existing = state.form.items.indexOfFirst { it.productId == product.id }
            val items = if (existing >= 0) {
                state.form.items.mapIndexed { idx, item ->
                    if (idx == existing) item.copy(quantity = item.quantity + 1) else item
                }
            } else {
                state.form.items + PurchaseItem(
                    productId = product.id,
                    code = product.code,
                    description = product.name,
                    unit = product.unit.ifBlank { "NIU" },
                    quantity = 1.0,
                    unitCost = product.purchasePrice ?: 0.0,
                    igvAffectationType = product.igvAffectationType,
                    priceIncludesIgv = state.form.defaultPriceIncludesIgv,
                )
            }
            state.copy(form = state.form.copy(items = items), snackMessage = "«${product.name}» agregado")
        }
    }

    fun updateItem(index: Int, transform: (PurchaseItem) -> PurchaseItem) {
        _uiState.update { state ->
            val items = state.form.items.mapIndexed { idx, item -> if (idx == index) transform(item) else item }
            state.copy(form = state.form.copy(items = items))
        }
    }

    fun removeItem(index: Int) {
        _uiState.update { state ->
            state.copy(form = state.form.copy(items = state.form.items.filterIndexed { idx, _ -> idx != index }))
        }
    }

    /** Default de "incluye IGV" para ítems nuevos — no toca los que ya están en el carrito. */
    fun setDefaultPriceIncludesIgv(value: Boolean) {
        _uiState.update { it.copy(form = it.form.copy(defaultPriceIncludesIgv = value)) }
    }

    fun save() {
        val form = _uiState.value.form
        val contactId = form.contactId
        if (contactId == null || form.items.isEmpty()) {
            _uiState.update { it.copy(error = "Proveedor e ítems son requeridos") }
            return
        }
        if (form.number.isBlank()) {
            _uiState.update { it.copy(error = "El número del comprobante es requerido") }
            return
        }
        if (form.paymentMethod.isBlank()) {
            _uiState.update { it.copy(error = "El método de pago es requerido") }
            return
        }
        val state = _uiState.value
        if (form.total(state.taxRate, state.taxConfig) <= 0.0) {
            _uiState.update { it.copy(error = "El total de la compra debe ser mayor a S/ 0.00") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null) }
            val input = CreatePurchaseInput(
                contactId = contactId,
                docType = form.docType,
                series = form.series.trim(),
                number = form.number.trim(),
                issueDate = form.issueDate,
                paymentMethod = form.paymentMethod,
                items = form.items,
            )
            when (val result = purchasesRepository.createPurchase(input)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(saving = false, formOpen = false, snackMessage = "Compra registrada") }
                    refresh()
                }
                is AppResult.Error -> _uiState.update { it.copy(saving = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun openDetail(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(detailLoading = true, error = null) }
            when (val result = purchasesRepository.getPurchase(id)) {
                is AppResult.Success -> _uiState.update { it.copy(detailLoading = false, detail = result.data) }
                is AppResult.Error -> _uiState.update { it.copy(detailLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun dismissDetail() {
        _uiState.update { it.copy(detail = null) }
    }

    fun requestVoid(id: Int) {
        _uiState.update { it.copy(voidTargetId = id) }
    }

    fun dismissVoidConfirm() {
        _uiState.update { it.copy(voidTargetId = null) }
    }

    fun confirmVoid() {
        val id = _uiState.value.voidTargetId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(voiding = true) }
            when (val result = purchasesRepository.voidPurchase(id)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(voiding = false, voidTargetId = null, detail = null, snackMessage = result.data)
                    }
                    refresh()
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(voiding = false, voidTargetId = null, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun consumeSnackMessage() {
        _uiState.update { it.copy(snackMessage = null) }
    }
}
