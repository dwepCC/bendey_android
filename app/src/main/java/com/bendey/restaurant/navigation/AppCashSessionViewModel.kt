package com.bendey.restaurant.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.cash.CashChipState
import com.bendey.restaurant.core.domain.cash.CashRepository
import com.bendey.restaurant.core.domain.cash.CashSession
import com.bendey.restaurant.core.domain.cash.CashSessionReport
import com.bendey.restaurant.core.domain.cash.decideCashChip
import com.bendey.restaurant.core.domain.cash.lastCountedCash
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.ui.cash.OpenCashFormState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class AppCashSessionState(
    val loading: Boolean = true,
    val checkedForBranch: Boolean = false,
    val canOperateCash: Boolean = false,
    val hasOpenSession: Boolean = false,
    val showOpenModal: Boolean = false,
    /** Ya no se fuerza (R9): el diálogo de apertura siempre se puede cancelar. Se conserva por compatibilidad. */
    val mandatoryModal: Boolean = false,
    val openForm: OpenCashFormState = OpenCashFormState(),
    val opening: Boolean = false,
    val error: String? = null,
    /**
     * true cuando el ultimo intento de verificar la caja fallo por un error de red/servidor (NO
     * porque de verdad no haya sesion abierta). Distingue "no se si esta abierta" de "confirme que
     * esta cerrada": un timeout de wifi no debe tratarse como caja cerrada. Mismo criterio que
     * CashSessionContext.tsx en Bendey Resto Tauri (session=undefined distinto de session=null).
     */
    val checkFailed: Boolean = false,
    /** Última lectura de la caja abierta (apertura, esperado, ingresos/gastos, hora). */
    val session: CashSession? = null,
    /** Efectivo contado en el último cierre con conteo, para prellenar la apertura. */
    val openPrefill: Double? = null,
    val sheetOpen: Boolean = false,
    val report: CashSessionReport? = null,
    val reportLoading: Boolean = false,
    val reportError: Boolean = false,
) {
    /** Lo que dibuja el chip de la barra superior (mozo: oculto; lectura fallida: "Caja…"). */
    val chip: CashChipState
        get() = decideCashChip(
            canOperateCash = canOperateCash,
            hasOpenSession = hasOpenSession,
            openedAt = session?.openedAt,
            checked = checkedForBranch && !loading,
            checkFailed = checkFailed,
        )
}

/**
 * Fuente del chip de caja (R9) y del diálogo de apertura. Ya NO bloquea: la caja cerrada no impide armar
 * pedidos ni enviar comandas; solo se exige para cobrar en efectivo (ver `requiresOpenCashSessionForCheckout`).
 */
@HiltViewModel
class AppCashSessionViewModel @Inject constructor(
    private val sessionStore: UserSessionStore,
    private val cashRepository: CashRepository,
) : ViewModel() {

    private val _local = MutableStateFlow(AppCashSessionState())
    private var lastRefreshAt = 0L

    val state: StateFlow<AppCashSessionState> = combine(
        sessionStore.userSessionFlow,
        sessionStore.cashSessionFlow,
        _local,
    ) { userSession, cashSnapshot, local ->
        val permissions = userSession?.restaurantPermissions.orEmpty()
        val employeeType = userSession?.user?.employeeType
        val branchId = userSession?.activeBranch?.id
        val canOperate = RestaurantPermissions.canChargeCashByRole(employeeType, permissions)
        val hasSession = cashSnapshot != null
        local.copy(
            canOperateCash = canOperate,
            hasOpenSession = hasSession,
            session = if (hasSession) local.session else null,
            showOpenModal = local.showOpenModal && canOperate && branchId != null && !hasSession,
            sheetOpen = local.sheetOpen && canOperate,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppCashSessionState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val userSession = sessionStore.userSessionFlow.first() ?: run {
                _local.update { it.copy(loading = false, checkedForBranch = true, showOpenModal = false) }
                return@launch
            }
            val canOperate = RestaurantPermissions.canChargeCashByRole(
                userSession.user.employeeType,
                userSession.restaurantPermissions,
            )
            val branchId = userSession.activeBranch?.id
            if (!canOperate || branchId == null) {
                _local.update {
                    it.copy(loading = false, checkedForBranch = true, showOpenModal = false, canOperateCash = canOperate)
                }
                return@launch
            }
            _local.update { it.copy(loading = true, error = null) }
            when (val result = cashRepository.getOpenSession(branchId)) {
                is AppResult.Success -> {
                    lastRefreshAt = System.currentTimeMillis()
                    _local.update {
                        it.copy(
                            loading = false,
                            checkedForBranch = true,
                            session = result.data,
                            report = if (result.data == null || result.data?.id != it.report?.session?.id) null else it.report,
                            error = null,
                            checkFailed = false,
                        )
                    }
                    // El "Vendido" del chip sale del reporte de la sesión (neto, sin anuladas).
                    if (result.data != null) loadReport()
                }
                // Un timeout de red NO significa "caja cerrada": el chip dice "Caja…" y reintenta al tocar.
                is AppResult.Error -> _local.update {
                    it.copy(loading = false, checkedForBranch = true, error = result.message, checkFailed = true)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    /**
     * Al entrar a POS/Mesas/Mesa: solo refresca la lectura si no hay caja abierta conocida. NO abre ningún
     * diálogo ni bloquea nada (antes era un modal obligatorio que impedía hasta consultar).
     */
    fun requireOpenSessionForOperation() {
        val current = state.value
        if (!current.canOperateCash || current.opening) return
        // Con caja abierta también se refresca (esperado/vendido vivos), pero sin martillar al servidor.
        if (current.hasOpenSession && System.currentTimeMillis() - lastRefreshAt < REFRESH_MIN_INTERVAL_MS) return
        refresh()
    }

    /**
     * Compuerta del cobro: ya NO bloquea. La caja se exige solo para EFECTIVO y esa validación (con su
     * mensaje) la hace el precheck del cobro en cada ViewModel. Aquí solo se reintenta la lectura si falló.
     */
    fun ensureForCheckout(): Boolean {
        val current = state.value
        if (current.canOperateCash && !current.hasOpenSession && (current.checkFailed || current.loading)) refresh()
        return true
    }

    fun updateOpenForm(transform: (OpenCashFormState) -> OpenCashFormState) {
        _local.update { it.copy(openForm = transform(it.openForm)) }
    }

    fun setOpenForm(form: OpenCashFormState) {
        _local.update { it.copy(openForm = form) }
    }

    /** Chip / franja: abre el diálogo de apertura prellenado con el efectivo contado del último cierre. */
    fun requestOpen() {
        val current = state.value
        if (!current.canOperateCash || current.hasOpenSession) return
        _local.update {
            it.copy(
                showOpenModal = true,
                mandatoryModal = false,
                error = null,
                openPrefill = null,
                openForm = OpenCashFormState(),
            )
        }
        loadOpenPrefill()
    }

    private fun loadOpenPrefill() {
        viewModelScope.launch {
            val branchId = sessionStore.userSessionFlow.first()?.activeBranch?.id ?: return@launch
            val last = (cashRepository.listSessions(branchId) as? AppResult.Success)?.data?.let(::lastCountedCash)
            if (last != null && last > 0.0) {
                _local.update {
                    // No pisa lo que el cajero ya empezó a teclear.
                    if (!it.showOpenModal || it.openForm.openingBalance != "0") it
                    else it.copy(openPrefill = last, openForm = it.openForm.copy(openingBalance = formatMoneyInput(last)))
                }
            }
        }
    }

    /** Guía de la primera venta (R4) y "Abrir con S/ 0": abre la caja con S/ 0 de un toque. */
    fun openWithZero() {
        val current = state.value
        if (!current.canOperateCash || current.hasOpenSession || current.opening) return
        _local.update { it.copy(openForm = OpenCashFormState()) }
        confirmOpenSession()
    }

    fun dismissOpenModal() {
        _local.update { it.copy(showOpenModal = false, mandatoryModal = false, error = null) }
    }

    fun confirmOpenSession() {
        val form = _local.value.openForm
        val balance = form.openingBalance.replace(",", ".").toDoubleOrNull()
        if (balance == null || balance < 0) {
            _local.update { it.copy(error = "Ingresa un monto inicial válido") }
            return
        }
        viewModelScope.launch {
            val branchId = sessionStore.userSessionFlow.first()?.activeBranch?.id
                ?: run {
                    _local.update { it.copy(error = "Sin sucursal activa") }
                    return@launch
                }
            _local.update { it.copy(opening = true, error = null) }
            when (val result = cashRepository.openSession(branchId, balance, form.notes)) {
                is AppResult.Success -> _local.update {
                    it.copy(
                        opening = false,
                        showOpenModal = false,
                        mandatoryModal = false,
                        openForm = OpenCashFormState(),
                        session = result.data,
                        checkFailed = false,
                        error = null,
                    )
                }
                is AppResult.Error -> _local.update { it.copy(opening = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    // ── Chip: hoja con el resumen ──

    /** Toque en el chip: caja abierta -> hoja con resumen; cerrada -> apertura; "Caja…" -> reintento. */
    fun onChipClick() {
        val current = state.value
        when {
            !current.canOperateCash -> Unit
            current.hasOpenSession -> openSheet()
            current.checkFailed -> refresh()
            else -> requestOpen()
        }
    }

    fun openSheet() {
        _local.update { it.copy(sheetOpen = true) }
        refresh() // "Efectivo esperado" vivo, no el de hace un rato
        loadReport()
    }

    fun closeSheet() {
        _local.update { it.copy(sheetOpen = false) }
    }

    fun loadReport() {
        val sessionId = state.value.session?.id ?: return
        viewModelScope.launch {
            _local.update { it.copy(reportLoading = it.report == null, reportError = false) }
            when (val result = cashRepository.getSessionReport(sessionId)) {
                is AppResult.Success -> _local.update { it.copy(reportLoading = false, report = result.data) }
                else -> _local.update { it.copy(reportLoading = false, reportError = true) }
            }
        }
    }

    private companion object {
        const val REFRESH_MIN_INTERVAL_MS = 15_000L
    }

    private fun formatMoneyInput(value: Double): String =
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
}
