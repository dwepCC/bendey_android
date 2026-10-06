package com.bendey.restaurant.core.realtime.delivery

import com.bendey.restaurant.core.domain.catalog.DeliveryRepository
import com.bendey.restaurant.core.domain.delivery.DeliveryBoardData
import com.bendey.restaurant.core.domain.delivery.DeliveryThresholds
import com.bendey.restaurant.core.domain.delivery.deliveryNewUnassignedIds
import com.bendey.restaurant.core.domain.model.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class DeliveryBoardState(
    val board: DeliveryBoardData? = null,
    /** La última consulta falló: lo que se ve puede estar desactualizado (o no hay nada que ver). */
    val error: Boolean = false,
    val errorMessage: String? = null,
    val loaded: Boolean = false,
    /** Hay una consulta en curso (para el indicador de actualizar). */
    val loading: Boolean = false,
    /** Epoch ms de la última consulta correcta (para recalcular semáforos sin red). */
    val fetchedAtMs: Long? = null,
) {
    /** Pedidos por asignar (alimenta el badge de la barra). 0 si aún no hay datos. */
    val unassignedCount: Int get() = board?.counts?.unassigned ?: 0
}

/**
 * Tablero de Delivery (D1): UN solo lugar para los datos. Lo leen la pantalla Entregas y el badge "por
 * asignar" de la barra, así no hay dos consultas ni dos números. Mismo patrón que [com.bendey.restaurant.core.realtime.pending.PendingApprovalStore]
 * (R10) y gemelo de `deliveryBoardStore.ts` de Tauri:
 *
 *  - se refresca por eventos (`delivery.board.updated`, ver `DeliveryDomain`), con refetch COALESCIDO
 *    ([DeliveryThresholds.REFETCH_DEBOUNCE_MS]: una ráfaga de eventos produce UNA consulta);
 *  - al reconectar el tiempo real, y con un respaldo de 60 s SOLO si el tiempo real está caído (lo maneja el
 *    `StaffOrderAlertsCoordinator`);
 *  - sonido: cuando aparece en "Por asignar" un pedido que no estaba se emite en [arrivals]. La PRIMERA
 *    carga (y la de después de cambiar de sucursal) solo memoriza lo que ya hay: no suena por pedidos que
 *    ya estaban.
 */
@Singleton
class DeliveryBoardStore internal constructor(
    private val repository: DeliveryRepository,
    private val scope: CoroutineScope,
    private val debounceMs: Long,
) {
    @Inject constructor(repository: DeliveryRepository) : this(
        repository,
        CoroutineScope(SupervisorJob() + Dispatchers.Default),
        DeliveryThresholds.REFETCH_DEBOUNCE_MS,
    )

    private val lock = Any()
    private val _state = MutableStateFlow(DeliveryBoardState())
    val state: StateFlow<DeliveryBoardState> = _state.asStateFlow()

    private val _arrivals = MutableSharedFlow<List<Int>>(extraBufferCapacity = 8)

    /** Sesiones que acaban de aparecer en "Por asignar" (nunca en la carga inicial). Para sonar y avisar. */
    val arrivals: SharedFlow<List<Int>> = _arrivals.asSharedFlow()

    @Volatile
    var enabled: Boolean = false
        private set

    /**
     * Solo quien puede asignar y recibe alertas de pedido nuevo (d.u + sonido) oye/ve el aviso de "pedido nuevo
     * por asignar" (igual que Tauri). Aunque no avise, el tablero y el badge se siguen actualizando.
     */
    @Volatile
    var alertsEnabled: Boolean = false

    private var running = false
    private var rerun = false
    private var debounceJob: Job? = null
    private var known: Set<Int>? = null

    /** Se incrementa en cada reset: una respuesta de una "época" anterior se descarta. */
    @Volatile
    private var epoch = 0

    /** Solo quien tiene `d.v` (y sucursal activa) consulta. Al cambiar se limpia lo anterior. */
    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        enabled = value
        reset()
    }

    /** Al cerrar sesión o cambiar de sucursal: no arrastrar datos ni "ya conocidos" de antes. */
    fun reset() {
        synchronized(lock) {
            epoch++
            debounceJob?.cancel()
            debounceJob = null
            known = null
            rerun = false
            running = false
        }
        _state.value = DeliveryBoardState()
    }

    fun refreshAsync() {
        if (!enabled) return
        scope.launch { refresh() }
    }

    /**
     * Consulta el tablero. Varias llamadas seguidas comparten una sola petición; si llega una mientras hay otra
     * en curso, se repite UNA vez al terminar (no se pierde el cambio más reciente).
     */
    suspend fun refresh() {
        if (!enabled) return
        val myEpoch: Int
        synchronized(lock) {
            if (running) {
                rerun = true
                return
            }
            running = true
            myEpoch = epoch
        }
        _state.update { it.copy(loading = true) }
        try {
            do {
                synchronized(lock) { rerun = false }
                fetchOnce(myEpoch)
            } while (synchronized(lock) { rerun } && epoch == myEpoch && enabled)
        } finally {
            synchronized(lock) { if (epoch == myEpoch) running = false }
            if (epoch == myEpoch) _state.update { it.copy(loading = false) }
        }
    }

    private suspend fun fetchOnce(myEpoch: Int) {
        when (val r = repository.getDeliveryBoard()) {
            is AppResult.Success -> {
                if (epoch != myEpoch) return
                val board = r.data
                val previous: Set<Int>?
                val fresh: List<Int>
                synchronized(lock) {
                    previous = known
                    fresh = deliveryNewUnassignedIds(previous, board)
                    known = board.unassigned.map { it.sessionId }.toSet()
                }
                _state.value = DeliveryBoardState(
                    board = board, error = false, loaded = true, loading = true,
                    fetchedAtMs = System.currentTimeMillis(),
                )
                if (alertsEnabled && previous != null && fresh.isNotEmpty()) _arrivals.tryEmit(fresh)
            }
            is AppResult.Error -> {
                if (epoch != myEpoch) return
                _state.update { it.copy(error = true, errorMessage = r.message, loaded = true) }
            }
            AppResult.Loading -> Unit
        }
    }

    /**
     * Refresco por evento de tiempo real: coalescente. Una ráfaga de eventos dentro de la ventana produce UNA
     * sola consulta (el primero programa el temporizador; los demás se suman).
     */
    fun scheduleRefresh() {
        if (!enabled) return
        synchronized(lock) {
            if (debounceJob != null) return
            debounceJob = scope.launch {
                delay(debounceMs)
                synchronized(lock) { debounceJob = null }
                refresh()
            }
        }
    }
}
