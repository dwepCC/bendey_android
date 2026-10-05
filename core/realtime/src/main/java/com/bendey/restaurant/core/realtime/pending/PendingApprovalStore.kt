package com.bendey.restaurant.core.realtime.pending

import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.pendingapproval.PendingApprovalOrder
import com.bendey.restaurant.core.domain.pendingapproval.PendingApprovalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

data class PendingApprovalState(
    val orders: List<PendingApprovalOrder> = emptyList(),
    /** Primera consulta en curso (nada cargado todavia). */
    val loading: Boolean = false,
    val loaded: Boolean = false,
    /** La ultima consulta fallo: puede haber pedidos esperando que no vemos. */
    val error: Boolean = false,
    val errorMessage: String? = null,
) {
    val count: Int get() = orders.size
}

/**
 * R10.1/R10.2: un solo lugar para la cola de pedidos del cliente (QR) por revisar. La leen el badge de
 * la barra y la hoja de la cola, asi no hay dos consultas ni dos numeros. Se refresca por el evento
 * `restaurant.order.pending_approval`, al volver a primer plano, al reconectar el WebSocket y con un
 * respaldo lento (ver `StaffOrderAlertsCoordinator`). Sin polling de 20 s.
 */
@Singleton
class PendingApprovalStore @Inject constructor(
    private val repository: PendingApprovalRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val _state = MutableStateFlow(PendingApprovalState())
    val state: StateFlow<PendingApprovalState> = _state.asStateFlow()

    private val _arrivals = MutableSharedFlow<String?>(extraBufferCapacity = 8)

    /** Un pedido nuevo acaba de llegar; el valor es el nombre de mesa (o null). Para avisar en pantalla. */
    val arrivals: SharedFlow<String?> = _arrivals.asSharedFlow()

    @Volatile
    var enabled: Boolean = false
        private set

    /** Solo quien puede revisar pedidos consulta la cola. Al cambiar se limpia el conteo anterior. */
    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        enabled = value
        reset()
    }

    fun reset() {
        _state.value = PendingApprovalState()
    }

    fun notifyArrival(tableName: String?) {
        _arrivals.tryEmit(tableName)
    }

    fun refreshAsync() {
        if (!enabled) return
        scope.launch { refresh() }
    }

    /** Consulta la lista. Varias llamadas seguidas comparten una sola peticion (las demas se descartan). */
    suspend fun refresh() {
        if (!enabled) return
        if (!mutex.tryLock()) return
        try {
            if (!_state.value.loaded) _state.update { it.copy(loading = true) }
            when (val r = repository.listPending()) {
                is AppResult.Success -> _state.value = PendingApprovalState(
                    orders = r.data, loading = false, loaded = true, error = false,
                )
                is AppResult.Error -> _state.update {
                    it.copy(loading = false, loaded = true, error = true, errorMessage = r.message)
                }
                AppResult.Loading -> Unit
            }
        } finally {
            mutex.unlock()
        }
    }

    suspend fun approve(orderId: Int): AppResult<Unit> {
        val r = repository.approve(orderId)
        if (r is AppResult.Success) {
            removeLocal(orderId)
            refresh()
        }
        return r
    }

    suspend fun reject(orderId: Int, reason: String): AppResult<Unit> {
        val r = repository.reject(orderId, reason)
        if (r is AppResult.Success) {
            removeLocal(orderId)
            refresh()
        }
        return r
    }

    private fun removeLocal(orderId: Int) {
        _state.update { s -> s.copy(orders = s.orders.filterNot { it.orderId == orderId }) }
    }
}
