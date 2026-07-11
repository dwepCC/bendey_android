package com.bendey.restaurant.feature.productos.reportes



import android.content.Context

import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope

import com.bendey.restaurant.core.data.export.BendeyFileShareService

import com.bendey.restaurant.core.data.export.ExportShareResult

import com.bendey.restaurant.core.data.export.exportSalesListCsv

import com.bendey.restaurant.core.data.export.exportSalesListPdf

import com.bendey.restaurant.core.domain.catalog.BranchItem

import com.bendey.restaurant.core.domain.catalog.SettingsRepository

import com.bendey.restaurant.core.domain.cash.CashPaymentMethod

import com.bendey.restaurant.core.domain.cash.CashRepository

import com.bendey.restaurant.core.domain.inventory.InventoryRepository

import com.bendey.restaurant.core.domain.inventory.StockMovementItem

import com.bendey.restaurant.core.domain.inventory.StockMovementQuery

import com.bendey.restaurant.core.domain.model.AppResult

import com.bendey.restaurant.core.domain.products.CategoryItem

import com.bendey.restaurant.core.domain.products.ProductReportItem

import com.bendey.restaurant.core.domain.products.ProductReportQuery

import com.bendey.restaurant.core.domain.products.ProductsRepository

import com.bendey.restaurant.core.domain.sales.SaleListSummary

import com.bendey.restaurant.core.domain.sales.SaleSummary

import com.bendey.restaurant.core.domain.sales.SalesRepository

import com.bendey.restaurant.core.domain.sales.VentasTab

import dagger.hilt.android.lifecycle.HiltViewModel

import java.time.LocalDate

import java.time.format.DateTimeFormatter

import javax.inject.Inject

import kotlinx.coroutines.Job

import kotlinx.coroutines.delay

import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.flow.update

import kotlinx.coroutines.launch



data class ReportesUiState(

    val tab: ReportesTab = ReportesTab.KARDEX,

    val salesSubTab: SalesReportSubTab = SalesReportSubTab.NOTAS,

    val loading: Boolean = false,

    val exportBusy: String? = null,

    val fromDate: String = currentMonthStart(),

    val toDate: String = todayDate(),

    val branchId: Int? = null,

    val categoryId: Int? = null,

    val productQ: String = "",

    val movementKind: String = "",

    val refNotesQ: String = "",

    val stockLessThan: String = "",

    val salesQuery: String = "",

    val paymentMethodFilter: String = "",

    val billingStatusFilter: String = "",

    val branches: List<BranchItem> = emptyList(),

    val categories: List<CategoryItem> = emptyList(),

    val paymentMethods: List<CashPaymentMethod> = emptyList(),

    val kardexRows: List<StockMovementItem> = emptyList(),

    val kardexTotal: Int = 0,

    val kardexPage: Int = 1,

    val productRows: List<ProductReportItem> = emptyList(),

    val productTotal: Int = 0,

    val productPage: Int = 1,

    val salesList: List<SaleSummary> = emptyList(),

    val salesTotal: Int = 0,

    val salesPage: Int = 1,

    val listSummary: SaleListSummary = SaleListSummary(),

    val error: String? = null,

    val snackMessage: String? = null,

) {

    val paymentMethodNames: Map<String, String>

        get() = paymentMethods.associate { it.code.lowercase() to it.name }



    val hasMoreSales: Boolean get() = salesList.size < salesTotal

}



private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE



private fun todayDate(): String = LocalDate.now().format(dateFormatter)



private fun currentMonthStart(): String = LocalDate.now().withDayOfMonth(1).format(dateFormatter)



private fun SalesReportSubTab.toVentasTab(): VentasTab = when (this) {

    SalesReportSubTab.NOTAS -> VentasTab.NOTAS

    SalesReportSubTab.DOCUMENTOS -> VentasTab.FACTURACION

}



@HiltViewModel

class ReportesViewModel @Inject constructor(

    private val inventoryRepository: InventoryRepository,

    private val productsRepository: ProductsRepository,

    private val salesRepository: SalesRepository,

    private val settingsRepository: SettingsRepository,

    private val cashRepository: CashRepository,

    private val fileShareService: BendeyFileShareService,

) : ViewModel() {



    private val _uiState = MutableStateFlow(ReportesUiState())

    val uiState: StateFlow<ReportesUiState> = _uiState.asStateFlow()



    private var salesSearchJob: Job? = null



    init {

        loadMeta()

        refresh()

    }



    fun selectTab(tab: ReportesTab) {

        _uiState.update { it.copy(tab = tab, error = null) }

        refresh()

    }



    fun selectSalesSubTab(subTab: SalesReportSubTab) {

        if (_uiState.value.salesSubTab == subTab) return

        _uiState.update {

            it.copy(

                salesSubTab = subTab,

                salesPage = 1,

                paymentMethodFilter = "",

                billingStatusFilter = "",

                error = null,

            )

        }

        refresh()

    }



    fun setFromDate(value: String) {

        _uiState.update { it.copy(fromDate = value, salesPage = 1) }

        refresh()

    }



    fun setToDate(value: String) {

        _uiState.update { it.copy(toDate = value, salesPage = 1) }

        refresh()

    }



    fun setBranchId(value: Int?) {

        _uiState.update { it.copy(branchId = value) }

        refresh()

    }



    fun setCategoryId(value: Int?) {

        _uiState.update { it.copy(categoryId = value) }

        refresh()

    }



    fun setProductQ(value: String) {

        _uiState.update { it.copy(productQ = value, kardexPage = 1, productPage = 1) }

        refresh()

    }



    fun setMovementKind(value: String) {

        _uiState.update { it.copy(movementKind = value, kardexPage = 1) }

        refresh()

    }



    fun setRefNotesQ(value: String) {

        _uiState.update { it.copy(refNotesQ = value, kardexPage = 1) }

        refresh()

    }



    fun setStockLessThan(value: String) {

        _uiState.update { it.copy(stockLessThan = value, productPage = 1) }

        refresh()

    }



    fun setSalesQuery(value: String) {

        _uiState.update { it.copy(salesQuery = value, salesPage = 1) }

        salesSearchJob?.cancel()

        salesSearchJob = viewModelScope.launch {

            delay(350)

            refresh()

        }

    }



    fun setPaymentMethodFilter(code: String) {

        _uiState.update { it.copy(paymentMethodFilter = code, salesPage = 1) }

        refresh()

    }



    fun setBillingStatusFilter(status: String) {

        _uiState.update { it.copy(billingStatusFilter = status, salesPage = 1) }

        refresh()

    }



    fun refresh() {

        when (_uiState.value.tab) {

            ReportesTab.KARDEX -> loadKardex()

            ReportesTab.PRODUCTOS -> loadProducts()

            ReportesTab.VENTAS -> loadSales(reset = true)

        }

    }



    fun loadMoreSales() {

        val state = _uiState.value

        if (state.tab != ReportesTab.VENTAS || state.loading || !state.hasMoreSales) return

        _uiState.update { it.copy(salesPage = it.salesPage + 1) }

        loadSales(reset = false)

    }



    fun consumeSnackMessage() {

        _uiState.update { it.copy(snackMessage = null) }

    }



    fun export(context: Context, format: String) {

        val state = _uiState.value

        if (state.exportBusy != null) return

        viewModelScope.launch {

            _uiState.update { it.copy(exportBusy = format, error = null) }

            val result = when (state.tab) {

                ReportesTab.KARDEX -> exportKardex(context, state, format)

                ReportesTab.PRODUCTOS -> exportProducts(context, state, format)

                ReportesTab.VENTAS -> exportSales(context, state, format)

            }

            _uiState.update {

                it.copy(

                    exportBusy = null,

                    snackMessage = when (result) {

                        ExportShareResult.Success -> "Exportación lista"

                        is ExportShareResult.Failure -> result.userMessage

                    },

                    error = if (result is ExportShareResult.Failure) result.userMessage else null,

                )

            }

        }

    }



    private fun loadMeta() {

        viewModelScope.launch {

            when (val branches = settingsRepository.listBranches()) {

                is AppResult.Success -> _uiState.update { it.copy(branches = branches.data.filter { b -> b.active }) }

                else -> Unit

            }

            when (val categories = productsRepository.listCategories()) {

                is AppResult.Success -> _uiState.update { it.copy(categories = categories.data) }

                else -> Unit

            }

            when (val methods = cashRepository.listPaymentMethods()) {

                is AppResult.Success -> _uiState.update { it.copy(paymentMethods = methods.data.filter { m -> m.active }) }

                else -> Unit

            }

        }

    }



    private fun loadKardex() {

        viewModelScope.launch {

            val state = _uiState.value

            _uiState.update { it.copy(loading = true, error = null) }

            when (

                val result = inventoryRepository.listMovements(

                    StockMovementQuery(

                        productQ = state.productQ,

                        branchId = state.branchId,

                        dateFrom = state.fromDate,

                        dateTo = state.toDate,

                        movementKind = state.movementKind.ifBlank { null },

                        refNotesQ = state.refNotesQ,

                        page = state.kardexPage,

                        perPage = 25,

                    ),

                )

            ) {

                is AppResult.Success -> _uiState.update {

                    it.copy(loading = false, kardexRows = result.data.first, kardexTotal = result.data.second)

                }

                is AppResult.Error -> _uiState.update { it.copy(loading = false, error = result.message) }

                AppResult.Loading -> Unit

            }

        }

    }



    private fun loadProducts() {

        viewModelScope.launch {

            val state = _uiState.value

            val stockLess = state.stockLessThan.replace(",", ".").toDoubleOrNull()

            _uiState.update { it.copy(loading = true, error = null) }

            when (

                val result = productsRepository.listProductReport(

                    ProductReportQuery(

                        query = state.productQ,

                        categoryId = state.categoryId,

                        branchId = state.branchId,

                        stockLessThan = stockLess,

                        page = state.productPage,

                        perPage = 25,

                    ),

                )

            ) {

                is AppResult.Success -> _uiState.update {

                    it.copy(loading = false, productRows = result.data.first, productTotal = result.data.second)

                }

                is AppResult.Error -> _uiState.update { it.copy(loading = false, error = result.message) }

                AppResult.Loading -> Unit

            }

        }

    }



    private fun loadSales(reset: Boolean) {

        viewModelScope.launch {

            val state = _uiState.value

            val page = if (reset) 1 else state.salesPage

            if (reset) {

                _uiState.update { it.copy(salesPage = 1, salesList = emptyList()) }

            }

            _uiState.update { it.copy(loading = true, error = null) }

            val ventasTab = state.salesSubTab.toVentasTab()

            when (

                val result = salesRepository.listSales(

                    from = state.fromDate,

                    to = state.toDate,

                    tab = ventasTab,

                    page = page,

                    perPage = 25,

                    query = state.salesQuery,

                    paymentMethod = state.paymentMethodFilter,

                    billingStatus = state.billingStatusFilter,

                )

            ) {

                is AppResult.Success -> {

                    val merged = if (reset) result.data.sales else state.salesList + result.data.sales

                    _uiState.update {

                        it.copy(

                            loading = false,

                            salesList = merged,

                            salesTotal = result.data.total,

                            listSummary = result.data.summary,

                        )

                    }

                }

                is AppResult.Error -> _uiState.update { it.copy(loading = false, error = result.message) }

                AppResult.Loading -> Unit

            }

        }

    }



    private suspend fun exportKardex(context: Context, state: ReportesUiState, format: String): ExportShareResult {

        val rows = when (

            val result = inventoryRepository.listAllMovementsForExport(

                StockMovementQuery(

                    productQ = state.productQ,

                    branchId = state.branchId,

                    dateFrom = state.fromDate,

                    dateTo = state.toDate,

                    movementKind = state.movementKind.ifBlank { null },

                    refNotesQ = state.refNotesQ,

                ),

            )

        ) {

            is AppResult.Success -> result.data

            is AppResult.Error -> return fileShareService.failureFrom(Exception(result.message))

            AppResult.Loading -> emptyList()

        }

        if (rows.isEmpty()) return fileShareService.failureFrom(Exception("No hay datos para exportar"))

        return if (format == "pdf") {

            exportKardexPdf(context, fileShareService, rows, state.fromDate, state.toDate)

        } else {

            exportKardexCsv(context, fileShareService, rows, state.fromDate, state.toDate)

        }

    }



    private suspend fun exportProducts(context: Context, state: ReportesUiState, format: String): ExportShareResult {

        val stockLess = state.stockLessThan.replace(",", ".").toDoubleOrNull()

        val rows = when (

            val result = productsRepository.listProductReport(

                ProductReportQuery(

                    query = state.productQ,

                    categoryId = state.categoryId,

                    branchId = state.branchId,

                    stockLessThan = stockLess,

                    page = 1,

                    perPage = 10_000,

                ),

            )

        ) {

            is AppResult.Success -> result.data.first

            is AppResult.Error -> return fileShareService.failureFrom(Exception(result.message))

            AppResult.Loading -> emptyList()

        }

        if (rows.isEmpty()) return fileShareService.failureFrom(Exception("No hay datos para exportar"))

        return if (format == "pdf") exportProductsReportPdf(context, fileShareService, rows)

        else exportProductsReportCsv(context, fileShareService, rows)

    }



    private suspend fun exportSales(context: Context, state: ReportesUiState, format: String): ExportShareResult {

        val ventasTab = state.salesSubTab.toVentasTab()

        val rows = when (

            val result = salesRepository.listAllSalesForExport(

                from = state.fromDate,

                to = state.toDate,

                tab = ventasTab,

                query = state.salesQuery,

                paymentMethod = state.paymentMethodFilter,

                billingStatus = state.billingStatusFilter,

            )

        ) {

            is AppResult.Success -> result.data

            is AppResult.Error -> return fileShareService.failureFrom(Exception(result.message))

            AppResult.Loading -> emptyList()

        }

        if (rows.isEmpty()) return fileShareService.failureFrom(Exception("No hay datos para exportar"))

        return if (format == "pdf") {

            exportSalesListPdf(context, fileShareService, ventasTab, rows, state.fromDate, state.toDate, reportStyle = true)

        } else {

            exportSalesListCsv(context, fileShareService, ventasTab, rows, state.fromDate, state.toDate, reportStyle = true)

        }

    }

}


