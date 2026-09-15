package com.bendey.restaurant.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.cash.CashRepository
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
import javax.inject.Inject

data class AppCashSessionState(
    val loading: Boolean = true,
    val checkedForBranch: Boolean = false,
    val canOperateCash: Boolean = false,
    val hasOpenSession: Boolean = false,
    val showOpenModal: Boolean = false,
    val mandatoryModal: Boolean = false,
    val openForm: OpenCashFormState = OpenCashFormState(),
    val opening: Boolean = false,
    val error: String? = null,
    /**
     * true cuando el ultimo intento de verificar la caja fallo por un error de red/servidor (NO
     * porque de verdad no haya sesion abierta). Distingue "no se si esta abierta" de "confirme que
     * esta cerrada" -- sin esto, un timeout de wifi (justo lo que un local con mala senal sufre a
     * cada rato) se trataba exactamente igual que una caja cerrada de verdad: se forzaba el modal
     * obligatorio de "abrir caja", el cajero terminaba cerrando y reabriendo una sesion que en
     * realidad seguia abierta en el servidor. Mismo criterio que ya usa correctamente
     * CashSessionContext.tsx en Bendey Resto Tauri (session=undefined distinto de session=null).
     */
    val checkFailed: Boolean = false,
)


/**
 * Paridad con `CashSessionProvider` (Capacitor): verifica caja abierta y modal obligatorio.
 */
@HiltViewModel
class AppCashSessionViewModel @Inject constructor(
    private val sessionStore: UserSessionStore,
    private val cashRepository: CashRepository,
) : ViewModel() {

    private val _local = MutableStateFlow(AppCashSessionState())

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
            showOpenModal = local.showOpenModal && canOperate && branchId != null && !hasSession,
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
            val permissions = userSession.restaurantPermissions
            val canOperate = RestaurantPermissions.canChargeCashByRole(
                userSession.user.employeeType,
                permissions,
            )
            val branchId = userSession.activeBranch?.id
            if (!canOperate || branchId == null) {
                _local.update {
                    it.copy(
                        loading = false,
                        checkedForBranch = true,
                        showOpenModal = false,
                        canOperateCash = canOperate,
                    )
                }
                return@launch
            }
            _local.update { it.copy(loading = true, error = null) }
            when (val result = cashRepository.getOpenSession(branchId)) {
                is AppResult.Success -> {
                    val open = result.data != null
                    _local.update {
                        it.copy(
                            loading = false,
                            checkedForBranch = true,
                            showOpenModal = !open,
                            mandatoryModal = !open,
                            error = null,
                            checkFailed = false,
                        )
                    }
                }
                is AppResult.Error -> {
                    // Un timeout de red NO significa "caja cerrada" -- antes esto forzaba el mismo
                    // modal obligatorio que una caja confirmada como cerrada, y el cajero terminaba
                    // cerrando/reabriendo una sesion que en realidad seguia abierta en el servidor.
                    // showOpenModal/mandatoryModal quedan tal cual estaban (no se tocan).
                    _local.update {
                        it.copy(
                            loading = false,
                            checkedForBranch = true,
                            error = result.message,
                            checkFailed = true,
                        )
                    }
                }
                AppResult.Loading -> Unit
            }
        }
    }

    /** Al entrar a POS/Mesa — muestra modal si falta caja. */
    fun requireOpenSessionForOperation() {
        val current = state.value
        if (!current.canOperateCash || current.hasOpenSession) return
        if (current.loading) {
            refresh()
            return
        }
        if (current.checkFailed) {
            // Todavia no sabemos si esta abierta o cerrada (el ultimo intento fallo por red) --
            // reintenta en vez de asumir que esta cerrada.
            refresh()
            return
        }
        _local.update { it.copy(showOpenModal = true, mandatoryModal = true) }
    }

    /**
     * Bloquea checkout hasta tener caja abierta (cajeros).
     * @return true si puede abrir checkout.
     */
    fun ensureForCheckout(): Boolean {
        val current = state.value
        if (!current.canOperateCash) return true
        if (current.hasOpenSession) return true
        if (current.loading) return false
        requireOpenSessionForOperation()
        return false
    }

    fun updateOpenForm(transform: (OpenCashFormState) -> OpenCashFormState) {
        _local.update { it.copy(openForm = transform(it.openForm)) }
    }

    fun setOpenForm(form: OpenCashFormState) {
        _local.update { it.copy(openForm = form) }
    }

    fun dismissOpenModal() {
        if (_local.value.mandatoryModal && !state.value.hasOpenSession) return
        _local.update { it.copy(showOpenModal = false, mandatoryModal = false) }
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
                is AppResult.Success -> {
                    _local.update {
                        it.copy(
                            opening = false,
                            showOpenModal = false,
                            mandatoryModal = false,
                            openForm = OpenCashFormState(),
                            error = null,
                        )
                    }
                }
                is AppResult.Error -> {
                    _local.update { it.copy(opening = false, error = result.message) }
                }
                AppResult.Loading -> Unit
            }
        }
    }
}
