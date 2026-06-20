package com.bendey.restaurant.feature.caja

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.cash.AddCashMovementInput
import com.bendey.restaurant.core.domain.cash.CashMovement
import com.bendey.restaurant.core.domain.cash.CashMovementType
import com.bendey.restaurant.core.domain.cash.CashRepository
import com.bendey.restaurant.core.domain.cash.CashSession
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.session.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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
)

data class CloseCashForm(
    val closingBalance: String = "",
    val notes: String = "",
)

data class CajaUiState(
    val loading: Boolean = false,
    val actionLoading: Boolean = false,
    val session: CashSession? = null,
    val movements: List<CashMovement> = emptyList(),
    val branchName: String? = null,
    val showOpenDialog: Boolean = false,
    val showMovementDialog: Boolean = false,
    val showCloseDialog: Boolean = false,
    val openForm: OpenCashForm = OpenCashForm(),
    val movementForm: MovementForm = MovementForm(),
    val closeForm: CloseCashForm = CloseCashForm(),
    val error: String? = null,
    val snackMessage: String? = null,
)

@HiltViewModel
class CajaViewModel @Inject constructor(
    private val cashRepository: CashRepository,
    private val sessionStore: UserSessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CajaUiState())
    val uiState: StateFlow<CajaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionStore.userSessionFlow.collect { user ->
                _uiState.update { it.copy(branchName = user?.activeBranch?.name) }
            }
        }
        refresh()
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
                        )
                    }
                    session?.let { loadMovements(it.id) }
                        ?: _uiState.update { it.copy(movements = emptyList()) }
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
        val balance = form.openingBalance.replace(",", ".").toDoubleOrNull()
        if (balance == null || balance < 0) {
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
            when (
                val result = cashRepository.openSession(branchId, balance, form.notes)
            ) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            actionLoading = false,
                            session = result.data,
                            showOpenDialog = false,
                            snackMessage = "Caja abierta",
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
        _uiState.update {
            it.copy(
                showMovementDialog = true,
                movementForm = MovementForm(type = type, category = category),
            )
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

    fun showCloseDialog() {
        val session = _uiState.value.session ?: return
        _uiState.update {
            it.copy(
                showCloseDialog = true,
                closeForm = CloseCashForm(
                    closingBalance = session.expectedBalance.toString(),
                ),
            )
        }
    }

    fun dismissCloseDialog() {
        _uiState.update { it.copy(showCloseDialog = false) }
    }

    fun updateCloseForm(transform: (CloseCashForm) -> CloseCashForm) {
        _uiState.update { it.copy(closeForm = transform(it.closeForm)) }
    }

    fun confirmCloseSession() {
        val session = _uiState.value.session ?: return
        val form = _uiState.value.closeForm
        val closing = form.closingBalance.replace(",", ".").toDoubleOrNull()
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            when (
                val result = cashRepository.closeSession(
                    sessionId = session.id,
                    closingBalance = closing,
                    notes = form.notes,
                )
            ) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            actionLoading = false,
                            session = null,
                            movements = emptyList(),
                            showCloseDialog = false,
                            showOpenDialog = true,
                            snackMessage = "Caja cerrada",
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

    fun consumeSnackMessage() {
        _uiState.update { it.copy(snackMessage = null) }
    }

    private suspend fun loadMovements(sessionId: Int) {
        when (val result = cashRepository.listMovements(sessionId)) {
            is AppResult.Success -> _uiState.update { it.copy(movements = result.data) }
            is AppResult.Error -> _uiState.update { it.copy(error = result.message) }
            AppResult.Loading -> Unit
        }
    }
}
