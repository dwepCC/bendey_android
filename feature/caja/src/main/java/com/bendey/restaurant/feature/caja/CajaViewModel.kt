package com.bendey.restaurant.feature.caja

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.data.export.BendeyFileShareService
import com.bendey.restaurant.core.data.export.ExportShareResult
import com.bendey.restaurant.core.domain.cash.AddCashMovementInput
import com.bendey.restaurant.core.domain.cash.CashBankAccount
import com.bendey.restaurant.core.domain.cash.CashBankMovement
import com.bendey.restaurant.core.domain.cash.CashMovement
import com.bendey.restaurant.core.domain.cash.CashMovementType
import com.bendey.restaurant.core.domain.cash.CashPaymentMethod
import com.bendey.restaurant.core.domain.restaurant.BranchOperationalStatus
import com.bendey.restaurant.core.domain.restaurant.MesasRepository
import com.bendey.restaurant.core.domain.cash.CashRepository
import com.bendey.restaurant.core.domain.cash.CashSession
import com.bendey.restaurant.core.domain.cash.CashSessionBrief
import com.bendey.restaurant.core.domain.cash.CashFilterUser
import com.bendey.restaurant.core.domain.cash.CashMovementReportRow
import com.bendey.restaurant.core.domain.cash.CashMovementsReportQuery
import com.bendey.restaurant.core.domain.cash.CashMovementsReportSummary
import com.bendey.restaurant.core.domain.cash.CashPaymentsReport
import com.bendey.restaurant.core.domain.cash.CashSessionProductSold
import com.bendey.restaurant.core.domain.cash.CashSessionReport
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.session.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class CajaTab(val label: String) {
    SESSION("Sesión"),
    MOVEMENTS("Movimientos"),
    REPORT("Reporte"),
    HISTORY("Historial"),
    CONFIG("Config"),
}

data class OpenCashForm(
    val openingBalance: String = "0",
    val notes: String = "",
)

data class MovementForm(
    val type: CashMovementType = CashMovementType.INCOME,
    val category: String = "ingreso_manual",
    val amount: String = "",
    val reference: String = "",
    val notes: String = "",
    val paymentMethod: String = "cash",
)

data class PaymentMethodForm(
    val id: Int? = null,
    val name: String = "",
    val code: String = "",
    val destinationType: String = "cash",
    val bankAccountId: Int? = null,
    val active: Boolean = true,
)

data class BankAccountForm(
    val id: Int? = null,
    val name: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val currency: String = "PEN",
    val type: String = "bank",
    val paymentMethod: String = "",
    val initialBalance: String = "0",
    val active: Boolean = true,
)

data class BankMovementForm(
    val type: String = "credit",
    val description: String = "",
    val reference: String = "",
    val amount: String = "",
    val date: String = java.time.LocalDate.now().toString(),
)

data class MovementsReportFilter(
    val dateFrom: String = currentMonthStart(),
    val dateTo: String = todayDate(),
    val userId: Int? = null,
    val sessionId: Int? = null,
    val type: String = "",
    val paymentMethod: String = "",
)

data class CloseCashForm(
    val closingBalance: String = "",
    val notes: String = "",
    val useArqueo: Boolean = true,
    val arqueo: Map<String, Int> = emptyArqueo(),
)

data class CajaUiState(
    val loading: Boolean = false,
    val actionLoading: Boolean = false,
    val tab: CajaTab = CajaTab.SESSION,
    val session: CashSession? = null,
    val movements: List<CashMovement> = emptyList(),
    val historySessions: List<CashSessionBrief> = emptyList(),
    val report: CashSessionReport? = null,
    val reportSessionId: Int? = null,
    val reportLoading: Boolean = false,
    val paymentMethods: List<CashPaymentMethod> = emptyList(),
    val bankAccounts: List<CashBankAccount> = emptyList(),
    val configLoading: Boolean = false,
    val showPaymentMethodDialog: Boolean = false,
    val showBankAccountDialog: Boolean = false,
    val paymentMethodForm: PaymentMethodForm = PaymentMethodForm(),
    val bankAccountForm: BankAccountForm = BankAccountForm(),
    val operationalStatus: BranchOperationalStatus? = null,
    val showBankMovementsDialog: Boolean = false,
    val bankMovementsAccountId: Int? = null,
    val bankMovementsAccountName: String? = null,
    val bankMovements: List<CashBankMovement> = emptyList(),
    val bankMovementsLoading: Boolean = false,
    val bankMovementForm: BankMovementForm = BankMovementForm(),
    val branchName: String? = null,
    val canViewCashSettings: Boolean = true,
    val canManageCashSettings: Boolean = false,
    val movementsFilter: MovementsReportFilter = MovementsReportFilter(),
    val movementsReportLoading: Boolean = false,
    val movementsReportRows: List<CashMovementReportRow> = emptyList(),
    val movementsReportSummary: CashMovementsReportSummary = CashMovementsReportSummary(),
    val paymentsReport: CashPaymentsReport? = null,
    val filterUsers: List<CashFilterUser> = emptyList(),
    val reportProducts: List<CashSessionProductSold> = emptyList(),
    val movementsExportBusy: Boolean = false,
    val sessionReportExportBusy: Boolean = false,
    val showOpenDialog: Boolean = false,
    val showMovementDialog: Boolean = false,
    val showCloseDialog: Boolean = false,
    val showCloseForceConfirm: Boolean = false,
    val showArqueoDialog: Boolean = false,
    val openForm: OpenCashForm = OpenCashForm(),
    val movementForm: MovementForm = MovementForm(),
    val closeForm: CloseCashForm = CloseCashForm(),
    val arqueoDraft: Map<String, Int> = emptyArqueo(),
    val error: String? = null,
    val snackMessage: String? = null,
) {
    val currentBalance: Double get() = session?.expectedBalance ?: 0.0
}

@HiltViewModel
class CajaViewModel @Inject constructor(
    private val cashRepository: CashRepository,
    private val mesasRepository: MesasRepository,
    private val sessionStore: UserSessionStore,
    private val fileShareService: BendeyFileShareService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CajaUiState())
    val uiState: StateFlow<CajaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionStore.userSessionFlow.collect { user ->
                val perms = user?.restaurantPermissions.orEmpty()
                val employeeType = user?.user?.employeeType
                _uiState.update {
                    it.copy(
                        branchName = user?.activeBranch?.name,
                        canViewCashSettings = RestaurantPermissions.canViewCashSettings(perms, employeeType),
                        canManageCashSettings = RestaurantPermissions.canManageCashSettings(perms, employeeType),
                    )
                }
            }
        }
        refresh()
    }

    fun setTab(tab: CajaTab) {
        _uiState.update { it.copy(tab = tab, error = null) }
        when (tab) {
            CajaTab.REPORT -> {
                val sessionId = _uiState.value.reportSessionId
                    ?: _uiState.value.session?.id
                    ?: _uiState.value.historySessions.firstOrNull()?.id
                sessionId?.let { loadReport(it) }
            }
            CajaTab.HISTORY -> loadHistory()
            CajaTab.CONFIG -> if (_uiState.value.canViewCashSettings) loadConfig()
            CajaTab.MOVEMENTS -> loadMovementsReportData()
            else -> Unit
        }
    }

    private fun loadConfig() {
        viewModelScope.launch {
            _uiState.update { it.copy(configLoading = true, error = null) }
            val methodsResult = cashRepository.listPaymentMethods()
            val accountsResult = cashRepository.listBankAccounts()
            _uiState.update { state ->
                state.copy(
                    configLoading = false,
                    paymentMethods = (methodsResult as? AppResult.Success)?.data.orEmpty(),
                    bankAccounts = (accountsResult as? AppResult.Success)?.data.orEmpty(),
                    error = (methodsResult as? AppResult.Error)?.message
                        ?: (accountsResult as? AppResult.Error)?.message,
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val branchId = sessionStore.userSessionFlow.first()?.activeBranch?.id
            when (val result = cashRepository.getOpenSession(branchId)) {
                is AppResult.Success -> {
                    val session = result.data
                    _uiState.update {
                        it.copy(
                            loading = false,
                            session = session,
                            showOpenDialog = session == null,
                            arqueoDraft = parseArqueoJson(session?.arqueoJson),
                            closeForm = it.closeForm.copy(
                                closingBalance = session?.expectedBalance?.toString().orEmpty(),
                                arqueo = parseArqueoJson(session?.arqueoJson),
                            ),
                        )
                    }
                    session?.let { loadMovements(it.id) }
                        ?: _uiState.update { it.copy(movements = emptyList()) }
                    loadHistory(silent = true)
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(loading = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun showOpenDialog() {
        _uiState.update { it.copy(showOpenDialog = true, openForm = OpenCashForm()) }
    }

    fun dismissOpenDialog() {
        if (_uiState.value.session == null) return
        _uiState.update { it.copy(showOpenDialog = false) }
    }

    fun updateOpenForm(transform: (OpenCashForm) -> OpenCashForm) {
        _uiState.update { it.copy(openForm = transform(it.openForm)) }
    }

    fun confirmOpenSession() {
        val form = _uiState.value.openForm
        val balance = form.openingBalance.replace(",", ".").toDoubleOrNull() ?: 0.0
        if (balance < 0) {
            _uiState.update { it.copy(error = "Ingresa un monto inicial válido") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            val branchId = sessionStore.userSessionFlow.first()?.activeBranch?.id
                ?: run {
                    _uiState.update { it.copy(actionLoading = false, error = "Sin sucursal activa") }
                    return@launch
                }
            when (val result = cashRepository.openSession(branchId, balance, form.notes)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            actionLoading = false,
                            session = result.data,
                            showOpenDialog = false,
                            snackMessage = "Caja abierta",
                            closeForm = CloseCashForm(
                                closingBalance = result.data.expectedBalance.toString(),
                            ),
                        )
                    }
                    loadMovements(result.data.id)
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(actionLoading = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun showMovementDialog(type: CashMovementType) {
        val category = when (type) {
            CashMovementType.INCOME -> "ingreso_manual"
            CashMovementType.EXPENSE -> "egreso_manual"
        }
        viewModelScope.launch {
            if (_uiState.value.paymentMethods.isEmpty()) {
                when (val result = cashRepository.listPaymentMethods()) {
                    is AppResult.Success -> _uiState.update { it.copy(paymentMethods = result.data) }
                    else -> Unit
                }
            }
            val method = _uiState.value.paymentMethods.firstOrNull { it.active }?.code ?: "cash"
            _uiState.update {
                it.copy(
                    showMovementDialog = true,
                    movementForm = MovementForm(type = type, category = category, paymentMethod = method),
                )
            }
        }
    }

    fun dismissMovementDialog() {
        _uiState.update { it.copy(showMovementDialog = false) }
    }

    fun updateMovementForm(transform: (MovementForm) -> MovementForm) {
        _uiState.update { it.copy(movementForm = transform(it.movementForm)) }
    }

    fun confirmMovement() {
        val session = _uiState.value.session ?: return
        val form = _uiState.value.movementForm
        val amount = form.amount.replace(",", ".").toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(error = "Ingresa un monto válido") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            when (
                val result = cashRepository.addMovement(
                    session.id,
                    AddCashMovementInput(
                        type = form.type,
                        category = form.category,
                        amount = amount,
                        reference = form.reference,
                        notes = form.notes,
                        paymentMethod = form.paymentMethod,
                    ),
                )
            ) {
                is AppResult.Success -> {
                    refresh()
                    _uiState.update {
                        it.copy(
                            actionLoading = false,
                            showMovementDialog = false,
                            snackMessage = "Movimiento registrado",
                        )
                    }
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(actionLoading = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun showArqueoDialog() {
        val session = _uiState.value.session ?: return
        _uiState.update {
            it.copy(
                showArqueoDialog = true,
                arqueoDraft = parseArqueoJson(session.arqueoJson),
                error = null,
            )
        }
    }

    fun dismissArqueoDialog() {
        _uiState.update { it.copy(showArqueoDialog = false) }
    }

    fun setArqueoQty(denomination: String, qty: Int) {
        _uiState.update { state ->
            state.copy(
                arqueoDraft = state.arqueoDraft.toMutableMap().apply {
                    this[denomination] = qty.coerceAtLeast(0)
                },
            )
        }
    }

    fun confirmSaveArqueo() {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            when (val result = cashRepository.saveArqueo(session.id, _uiState.value.arqueoDraft)) {
                is AppResult.Success -> {
                    refresh()
                    _uiState.update {
                        it.copy(
                            actionLoading = false,
                            showArqueoDialog = false,
                            snackMessage = "Arqueo guardado · ${String.format("%.2f", result.data)}",
                        )
                    }
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(actionLoading = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun showCloseDialog() {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            val operational = when (val result = mesasRepository.getOperationalStatus()) {
                is AppResult.Success -> result.data
                else -> null
            }
            _uiState.update {
                it.copy(
                    showCloseDialog = true,
                    operationalStatus = operational,
                    closeForm = CloseCashForm(
                        closingBalance = session.expectedBalance.toString(),
                        useArqueo = true,
                        arqueo = parseArqueoJson(session.arqueoJson),
                    ),
                )
            }
        }
    }

    fun dismissCloseDialog() {
        _uiState.update { it.copy(showCloseDialog = false, showCloseForceConfirm = false) }
    }

    fun updateCloseForm(transform: (CloseCashForm) -> CloseCashForm) {
        _uiState.update { it.copy(closeForm = transform(it.closeForm)) }
    }

    fun setCloseArqueoQty(denomination: String, qty: Int) {
        _uiState.update { state ->
            val arqueo = state.closeForm.arqueo.toMutableMap().apply {
                this[denomination] = qty.coerceAtLeast(0)
            }
            state.copy(
                closeForm = state.closeForm.copy(
                    arqueo = arqueo,
                    closingBalance = sumArqueo(arqueo).toString(),
                ),
            )
        }
    }

    fun requestCloseSession() {
        val op = _uiState.value.operationalStatus
        if (op?.hasActiveOperations == true) {
            _uiState.update { it.copy(showCloseForceConfirm = true) }
            return
        }
        confirmCloseSession()
    }

    fun dismissCloseForceConfirm() {
        _uiState.update { it.copy(showCloseForceConfirm = false) }
    }

    fun confirmCloseSessionForced() {
        _uiState.update { it.copy(showCloseForceConfirm = false) }
        confirmCloseSession()
    }

    fun confirmCloseSession() {
        val session = _uiState.value.session ?: return
        val form = _uiState.value.closeForm
        val closing = if (form.useArqueo) {
            sumArqueo(form.arqueo)
        } else {
            form.closingBalance.replace(",", ".").toDoubleOrNull()
        }
        if (closing == null) {
            _uiState.update { it.copy(error = "Indica el efectivo contado") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            val arqueo = if (form.useArqueo && form.arqueo.values.any { it > 0 }) form.arqueo else null
            when (val result = cashRepository.closeSession(session.id, closing, form.notes, arqueo)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            actionLoading = false,
                            session = null,
                            movements = emptyList(),
                            showCloseDialog = false,
                            showOpenDialog = true,
                            snackMessage = "Caja cerrada",
                            reportSessionId = result.data.id,
                        )
                    }
                    loadHistory(silent = true)
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(actionLoading = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun loadReport(sessionId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(reportLoading = true, reportSessionId = sessionId, error = null) }
            when (val result = cashRepository.getSessionReport(sessionId)) {
                is AppResult.Success -> {
                    val products = when (val productsResult = cashRepository.getSessionProductsReport(sessionId)) {
                        is AppResult.Success -> productsResult.data
                        else -> emptyList()
                    }
                    _uiState.update {
                        it.copy(
                            reportLoading = false,
                            report = result.data,
                            reportProducts = products,
                            tab = CajaTab.REPORT,
                        )
                    }
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(reportLoading = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun updateMovementsFilter(transform: (MovementsReportFilter) -> MovementsReportFilter) {
        _uiState.update { it.copy(movementsFilter = transform(it.movementsFilter)) }
    }

    fun searchMovementsReport() {
        loadMovementsReport(page = 1)
    }

    private fun loadMovementsReportData() {
        viewModelScope.launch {
            if (_uiState.value.filterUsers.isEmpty()) {
                when (val users = cashRepository.listCashFilterUsers()) {
                    is AppResult.Success -> _uiState.update { it.copy(filterUsers = users.data) }
                    else -> Unit
                }
            }
            if (_uiState.value.paymentMethods.isEmpty()) {
                when (val methods = cashRepository.listPaymentMethods()) {
                    is AppResult.Success -> _uiState.update { it.copy(paymentMethods = methods.data) }
                    else -> Unit
                }
            }
            loadMovementsReport(page = 1)
        }
    }

    private fun loadMovementsReport(page: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            val branchId = sessionStore.userSessionFlow.first()?.activeBranch?.id
            _uiState.update { it.copy(movementsReportLoading = true, error = null) }
            val filter = state.movementsFilter
            val query = CashMovementsReportQuery(
                branchId = branchId,
                userId = filter.userId,
                dateFrom = filter.dateFrom,
                dateTo = filter.dateTo,
                sessionId = filter.sessionId,
                type = filter.type.takeIf { it.isNotBlank() },
                paymentMethod = filter.paymentMethod.takeIf { it.isNotBlank() },
                page = page,
                perPage = 25,
            )
            val cashResult = cashRepository.listMovementsReport(query)
            val paymentsResult = cashRepository.getPaymentsReport(
                from = filter.dateFrom,
                to = filter.dateTo,
                method = filter.paymentMethod.takeIf { it.isNotBlank() },
                userId = filter.userId,
                sessionId = filter.sessionId,
            )
            _uiState.update { current ->
                current.copy(
                    movementsReportLoading = false,
                    movementsReportRows = (cashResult as? AppResult.Success)?.data?.rows.orEmpty(),
                    movementsReportSummary = (cashResult as? AppResult.Success)?.data?.summary
                        ?: CashMovementsReportSummary(),
                    paymentsReport = (paymentsResult as? AppResult.Success)?.data,
                    error = (cashResult as? AppResult.Error)?.message
                        ?: (paymentsResult as? AppResult.Error)?.message,
                )
            }
        }
    }

    fun exportMovementsReport(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(movementsExportBusy = true) }
            val state = _uiState.value
            val branchId = sessionStore.userSessionFlow.first()?.activeBranch?.id
            val filter = state.movementsFilter
            val query = CashMovementsReportQuery(
                branchId = branchId,
                userId = filter.userId,
                dateFrom = filter.dateFrom,
                dateTo = filter.dateTo,
                sessionId = filter.sessionId,
                type = filter.type.takeIf { it.isNotBlank() },
                paymentMethod = filter.paymentMethod.takeIf { it.isNotBlank() },
                page = 1,
                perPage = 0,
            )
            val paymentsResult = cashRepository.getPaymentsReport(
                from = filter.dateFrom,
                to = filter.dateTo,
                method = filter.paymentMethod.takeIf { it.isNotBlank() },
                userId = filter.userId,
                sessionId = filter.sessionId,
            )
            when (val cashResult = cashRepository.listMovementsReportAll(query)) {
                is AppResult.Success -> {
                    val electronic = filterNonCashPayments(
                        (paymentsResult as? AppResult.Success)?.data,
                    )
                    val shareResult = withContext(Dispatchers.Main) {
                        exportCashMovementsCsv(
                            context = context,
                            fileShareService = fileShareService,
                            cashRows = cashResult.data.rows,
                            electronicRows = electronic,
                            from = filter.dateFrom,
                            to = filter.dateTo,
                        )
                    }
                    _uiState.update {
                        it.copy(
                            movementsExportBusy = false,
                            snackMessage = when (shareResult) {
                                ExportShareResult.Success -> "Movimientos exportados"
                                is ExportShareResult.Failure -> shareResult.userMessage
                            },
                        )
                    }
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(movementsExportBusy = false, snackMessage = cashResult.message ?: "No se pudo exportar")
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun exportSessionReportPdf(context: Context) {
        val report = _uiState.value.report ?: return
        if (_uiState.value.sessionReportExportBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(sessionReportExportBusy = true) }
            val lines = formatSessionReportLines(report, _uiState.value.reportProducts)
            val shareResult = withContext(Dispatchers.Main) {
                shareSessionReportPdf(
                    context = context,
                    fileShareService = fileShareService,
                    title = "Reporte caja #${report.session.id}",
                    lines = lines,
                )
            }
            _uiState.update {
                it.copy(
                    sessionReportExportBusy = false,
                    snackMessage = when (shareResult) {
                        ExportShareResult.Success -> "Reporte PDF exportado"
                        is ExportShareResult.Failure -> shareResult.userMessage
                    },
                )
            }
        }
    }

    private fun filterNonCashPayments(report: CashPaymentsReport?): List<com.bendey.restaurant.core.domain.cash.CashPaymentDetailRow> {
        if (report == null) return emptyList()
        val cashCodes = setOf("efectivo", "cash", "contado")
        return report.detail.filter { row -> row.method.trim().lowercase() !in cashCodes }
    }

    fun requireManageCashSettings(): Boolean {
        if (_uiState.value.canManageCashSettings) return true
        _uiState.update { it.copy(error = "No tiene permiso para modificar la configuración de caja") }
        return false
    }

    fun consumeSnackMessage() {
        _uiState.update { it.copy(snackMessage = null) }
    }

    fun showCreatePaymentMethod() {
        if (!requireManageCashSettings()) return
        _uiState.update { it.copy(showPaymentMethodDialog = true, paymentMethodForm = PaymentMethodForm()) }
    }

    fun showEditPaymentMethod(pm: CashPaymentMethod) {
        if (!requireManageCashSettings()) return
        _uiState.update {
            it.copy(
                showPaymentMethodDialog = true,
                paymentMethodForm = PaymentMethodForm(
                    id = pm.id,
                    name = pm.name,
                    code = pm.code,
                    destinationType = pm.destinationType,
                    bankAccountId = pm.bankAccountId,
                    active = pm.active,
                ),
            )
        }
    }

    fun dismissPaymentMethodDialog() {
        _uiState.update { it.copy(showPaymentMethodDialog = false) }
    }

    fun updatePaymentMethodForm(transform: (PaymentMethodForm) -> PaymentMethodForm) {
        _uiState.update { it.copy(paymentMethodForm = transform(it.paymentMethodForm)) }
    }

    fun confirmPaymentMethod() {
        val form = _uiState.value.paymentMethodForm
        if (form.name.isBlank() || form.code.isBlank()) {
            _uiState.update { it.copy(error = "Nombre y código son obligatorios") }
            return
        }
        if (form.destinationType == "bank_account" && form.bankAccountId == null) {
            _uiState.update { it.copy(error = "Selecciona una cuenta bancaria") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            val result = if (form.id != null) {
                cashRepository.updatePaymentMethod(
                    form.id,
                    form.name.trim(),
                    form.code.trim(),
                    form.destinationType,
                    form.bankAccountId,
                    form.active,
                )
            } else {
                cashRepository.createPaymentMethod(
                    form.name.trim(),
                    form.code.trim(),
                    form.destinationType,
                    form.bankAccountId,
                )
            }
            when (result) {
                is AppResult.Success -> {
                    loadConfig()
                    _uiState.update {
                        it.copy(actionLoading = false, showPaymentMethodDialog = false, snackMessage = "Método guardado")
                    }
                }
                is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun deletePaymentMethod(id: Int) {
        if (!requireManageCashSettings()) return
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            when (val result = cashRepository.deletePaymentMethod(id)) {
                is AppResult.Success -> {
                    loadConfig()
                    _uiState.update { it.copy(actionLoading = false, snackMessage = "Método eliminado") }
                }
                is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun showCreateBankAccount() {
        if (!requireManageCashSettings()) return
        _uiState.update { it.copy(showBankAccountDialog = true, bankAccountForm = BankAccountForm()) }
    }

    fun showEditBankAccount(acc: CashBankAccount) {
        if (!requireManageCashSettings()) return
        _uiState.update {
            it.copy(
                showBankAccountDialog = true,
                bankAccountForm = BankAccountForm(
                    id = acc.id,
                    name = acc.name,
                    bankName = acc.bankName,
                    accountNumber = acc.accountNumber,
                    currency = acc.currency,
                    type = acc.type,
                    paymentMethod = acc.paymentMethod,
                    active = acc.active,
                ),
            )
        }
    }

    fun dismissBankAccountDialog() {
        _uiState.update { it.copy(showBankAccountDialog = false) }
    }

    fun updateBankAccountForm(transform: (BankAccountForm) -> BankAccountForm) {
        _uiState.update { it.copy(bankAccountForm = transform(it.bankAccountForm)) }
    }

    fun confirmBankAccount() {
        val form = _uiState.value.bankAccountForm
        if (form.name.isBlank()) {
            _uiState.update { it.copy(error = "Ingresa un nombre para la cuenta") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            val result = if (form.id != null) {
                cashRepository.updateBankAccount(
                    form.id,
                    form.name.trim(),
                    form.bankName.trim(),
                    form.accountNumber.trim(),
                    form.type,
                    form.paymentMethod,
                    form.active,
                )
            } else {
                val balance = form.initialBalance.replace(",", ".").toDoubleOrNull() ?: 0.0
                cashRepository.createBankAccount(
                    form.name.trim(),
                    form.bankName.trim(),
                    form.accountNumber.trim(),
                    form.currency,
                    form.type,
                    form.paymentMethod,
                    balance,
                )
            }
            when (result) {
                is AppResult.Success -> {
                    loadConfig()
                    _uiState.update {
                        it.copy(actionLoading = false, showBankAccountDialog = false, snackMessage = "Cuenta guardada")
                    }
                }
                is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun showBankMovements(acc: CashBankAccount) {
        if (!requireManageCashSettings()) return
        _uiState.update {
            it.copy(
                showBankMovementsDialog = true,
                bankMovementsAccountId = acc.id,
                bankMovementsAccountName = acc.name,
                bankMovementForm = BankMovementForm(),
            )
        }
        loadBankMovements(acc.id)
    }

    fun dismissBankMovementsDialog() {
        _uiState.update { it.copy(showBankMovementsDialog = false) }
    }

    fun updateBankMovementForm(transform: (BankMovementForm) -> BankMovementForm) {
        _uiState.update { it.copy(bankMovementForm = transform(it.bankMovementForm)) }
    }

    private fun loadBankMovements(accountId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(bankMovementsLoading = true) }
            when (val result = cashRepository.listBankMovements(accountId)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(bankMovementsLoading = false, bankMovements = result.data)
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(bankMovementsLoading = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun confirmBankMovement() {
        val accountId = _uiState.value.bankMovementsAccountId ?: return
        val form = _uiState.value.bankMovementForm
        val amount = form.amount.replace(",", ".").toDoubleOrNull()
        if (form.description.isBlank() || amount == null || amount <= 0) {
            _uiState.update { it.copy(error = "Completa descripción y monto") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            when (
                val result = cashRepository.addBankMovement(
                    accountId,
                    form.type,
                    form.description.trim(),
                    form.reference,
                    amount,
                    form.date,
                )
            ) {
                is AppResult.Success -> {
                    loadConfig()
                    loadBankMovements(accountId)
                    _uiState.update {
                        it.copy(actionLoading = false, bankMovementForm = BankMovementForm(date = form.date), snackMessage = "Movimiento registrado")
                    }
                }
                is AppResult.Error -> _uiState.update { it.copy(actionLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    private fun loadHistory(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _uiState.update { it.copy(loading = true) }
            val branchId = sessionStore.userSessionFlow.first()?.activeBranch?.id
            when (val result = cashRepository.listSessions(branchId)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        loading = if (silent) it.loading else false,
                        historySessions = result.data.sortedByDescending { s -> s.openedAt.orEmpty() },
                    )
                }
                is AppResult.Error -> if (!silent) {
                    _uiState.update { it.copy(loading = false, error = result.message) }
                }
                AppResult.Loading -> Unit
            }
        }
    }

    private suspend fun loadMovements(sessionId: Int) {
        when (val result = cashRepository.listMovements(sessionId)) {
            is AppResult.Success -> _uiState.update { it.copy(movements = result.data) }
            is AppResult.Error -> _uiState.update { it.copy(error = result.message) }
            AppResult.Loading -> Unit
        }
    }
}

private fun todayDate(): String = java.time.LocalDate.now().toString()

private fun currentMonthStart(): String =
    java.time.LocalDate.now().withDayOfMonth(1).toString()
