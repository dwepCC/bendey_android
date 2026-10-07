package com.bendey.restaurant.feature.repartidores

import com.bendey.restaurant.core.domain.catalog.DeliveryRepository
import com.bendey.restaurant.core.domain.delivery.DELIVERY_REASON_OTHER
import com.bendey.restaurant.core.domain.delivery.DeliveryCard
import com.bendey.restaurant.core.domain.delivery.DeliveryFeeCopy
import com.bendey.restaurant.core.domain.delivery.DeliverySettings
import com.bendey.restaurant.core.domain.delivery.assignFeeInitialText
import com.bendey.restaurant.core.domain.delivery.assignFeeToSend
import com.bendey.restaurant.core.domain.delivery.canCollectDelivery
import com.bendey.restaurant.core.domain.delivery.canEditDeliveryFee
import com.bendey.restaurant.core.domain.delivery.canForceDelivery
import com.bendey.restaurant.core.domain.delivery.forceReasonError
import com.bendey.restaurant.core.domain.delivery.isCodPending
import com.bendey.restaurant.core.domain.delivery.isValidAssignFeeText
import com.bendey.restaurant.core.domain.delivery.showsAssignFeeField
import com.bendey.restaurant.core.domain.delivery.DeliveryCopy
import com.bendey.restaurant.core.domain.delivery.DeliveryReasonKind
import com.bendey.restaurant.core.domain.delivery.DeliverySection
import com.bendey.restaurant.core.domain.delivery.DeliveryThresholds
import com.bendey.restaurant.core.domain.delivery.canOperateDeliveryBoard
import com.bendey.restaurant.core.domain.delivery.isDeliveryDriver
import com.bendey.restaurant.core.domain.delivery.deliveryReasonError
import com.bendey.restaurant.core.domain.delivery.deliveryResolveReason
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.realtime.delivery.DeliveryBoardStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/** Qué diálogo está abierto sobre una tarjeta (a lo sumo uno). */
sealed interface DeliveryDialog {
    val card: DeliveryCard

    /** Elegir repartidor (asignar o reasignar). */
    data class Assign(override val card: DeliveryCard) : DeliveryDialog

    /** Cancelar el pedido (motivo obligatorio). */
    data class Cancel(override val card: DeliveryCard) : DeliveryDialog

    /** Marcar la entrega como fallida (motivo obligatorio). */
    data class Failed(override val card: DeliveryCard) : DeliveryDialog

    /** Confirmar "Marcar entregado". */
    data class Delivered(override val card: DeliveryCard) : DeliveryDialog

    /** D2b: confirmar "Marcar cobrado" (efectivo contra entrega ya recibido). */
    data class Collect(override val card: DeliveryCard) : DeliveryDialog

    /** D2b: "Aún no está cobrado": entregar igual con motivo obligatorio (solo `o.ch` / `s.m`). */
    data class ForceDelivered(override val card: DeliveryCard) : DeliveryDialog
}

private const val COLLECTION_REQUIRED = "COLLECTION_REQUIRED"

/** Estado de la pantalla Delivery que NO es del tablero (el tablero vive en [DeliveryBoardStore]). */
data class DeliveryUiState(
    /** Sección elegida en teléfono (pestañas con contador). */
    val selected: DeliverySection = DeliverySection.UNASSIGNED,
    /** En teléfono, la pestaña "Repartidores" (panel de disponibilidad, solo lectura) en lugar de una sección. */
    val showDrivers: Boolean = false,
    /** `d.u`: sin esto la vista es de solo lectura (solo "Llamar"). */
    val canAssign: Boolean = false,
    val dialog: DeliveryDialog? = null,
    /** Motivo rápido elegido, [DELIVERY_REASON_OTHER] o null. */
    val reasonChoice: String? = null,
    val reasonText: String = "",
    val reasonShowError: Boolean = false,
    /** Hay una acción en curso: todos los botones quedan deshabilitados (no se doble-envía). */
    val busy: Boolean = false,
    /** Error de la última acción, visible dentro del diálogo abierto. */
    val dialogError: String? = null,
    /** `o.ch` o `s.m` (D2.0): sin esto el campo de tarifa de la hoja de asignar sale deshabilitado. */
    val canEditFee: Boolean = false,
    /** `o.ch` o `s.m` (D2b): puede entregar un pedido contra entrega sin cobro marcado, con motivo. */
    val canForce: Boolean = false,
    /** Ajustes de delivery conocidos (caché compartida); null hasta que se lean. */
    val settings: DeliverySettings? = null,
    /** Texto del campo "Tarifa de delivery (S/)" de la hoja de asignar y el valor con el que se abrió. */
    val feeText: String = "",
    val feeInitialText: String = "",
) {
    /** La hoja de asignar muestra el campo si la tarifa está encendida o el pedido ya tiene una. */
    val showFeeField: Boolean
        get() = (dialog as? DeliveryDialog.Assign)?.let { showsAssignFeeField(settings, it.card.deliveryFee) } ?: false
}

/**
 * Reductor de las acciones de la vista Delivery (asignar, cancelar, entregado, fallido). Sin Android: lo usa
 * [DeliveryViewModel] con `viewModelScope` y los tests con un scope propio.
 *
 * Reglas: solo con `d.u` se puede operar; una acción en curso bloquea las demás; tras CADA acción (con éxito o
 * con error) se recarga el tablero; los errores llegan ya traducidos por el catálogo (`AppResult.Error.message`)
 * y salen por [messages] (Snackbar del DS) y, si hay diálogo abierto, también dentro de él.
 */
class DeliveryPresenter(
    private val scope: CoroutineScope,
    private val store: DeliveryBoardStore,
    private val repository: DeliveryRepository,
) {
    private val _state = MutableStateFlow(DeliveryUiState())
    val state: StateFlow<DeliveryUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    /** Avisos para el Snackbar (éxito y error). */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun setPermissions(permissions: List<String>, employeeType: String? = null) {
        _state.update {
            it.copy(
                canAssign = canOperateDeliveryBoard(permissions, employeeType),
                canEditFee = canEditDeliveryFee(permissions) && !isDeliveryDriver(employeeType),
                canForce = canForceDelivery(permissions) && !isDeliveryDriver(employeeType),
            )
        }
    }

    fun selectSection(section: DeliverySection) = _state.update { it.copy(selected = section, showDrivers = false) }

    fun selectDrivers() = _state.update { it.copy(showDrivers = true) }

    fun refresh() {
        scope.launch { store.refresh() }
    }

    // --- Diálogos -------------------------------------------------------------------------------

    private fun open(dialog: DeliveryDialog) {
        if (!_state.value.canAssign || _state.value.busy) return
        _state.update {
            it.copy(dialog = dialog, reasonChoice = null, reasonText = "", reasonShowError = false, dialogError = null)
        }
    }

    fun openAssign(card: DeliveryCard) {
        open(DeliveryDialog.Assign(card))
        if (_state.value.dialog !is DeliveryDialog.Assign) return
        // Prellenado: la tarifa del pedido o, si es null, la de ajustes (caché compartida).
        val cached = repository.peekDeliverySettings()
        val initial = assignFeeInitialText(card.deliveryFee, cached)
        _state.update { it.copy(settings = cached, feeText = initial, feeInitialText = initial) }
        if (cached == null) {
            // Ajustes aún sin leer: se piden (sin bloquear la hoja) y, si el cajero no tocó el campo, se prellena.
            scope.launch {
                val r = repository.getDeliverySettings()
                if (r !is AppResult.Success) return@launch
                _state.update { st ->
                    val open = st.dialog as? DeliveryDialog.Assign
                    if (open?.card?.sessionId != card.sessionId) return@update st.copy(settings = r.data)
                    val untouched = st.feeText == st.feeInitialText
                    val newInitial = assignFeeInitialText(card.deliveryFee, r.data)
                    st.copy(
                        settings = r.data,
                        feeInitialText = newInitial,
                        feeText = if (untouched) newInitial else st.feeText,
                    )
                }
            }
        }
    }

    /** Edita el campo de tarifa de la hoja de asignar; solo con `o.ch` o `s.m`. Solo dígitos y un decimal (2). */
    fun setFeeText(text: String) {
        if (!_state.value.canEditFee || _state.value.busy) return
        val clean = text.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
        val parts = clean.split('.')
        val limited = if (parts.size <= 1) clean.take(3) else parts[0].take(3) + "." + parts.drop(1).joinToString("").take(2)
        _state.update { it.copy(feeText = limited, dialogError = null) }
    }

    fun openCancel(card: DeliveryCard) = open(DeliveryDialog.Cancel(card))

    fun openFailed(card: DeliveryCard) {
        // Sin asignación no hay a qué marcarle el fallo.
        if (card.assignmentId != null) open(DeliveryDialog.Failed(card))
    }

    fun openDelivered(card: DeliveryCard) {
        if (card.assignmentId == null) return
        // D2b: si ya se sabe que el efectivo no está cobrado y se puede forzar, se va directo al motivo.
        if (card.payment.isCodPending() && _state.value.canForce) {
            open(DeliveryDialog.ForceDelivered(card))
        } else {
            open(DeliveryDialog.Delivered(card))
        }
    }

    /** D2b: "Marcar cobrado" solo con efectivo contra entrega pendiente y la asignación recogida o en camino. */
    fun openCollect(card: DeliveryCard) {
        if (canCollectDelivery(card.payment, card.assignmentId, card.assignmentStatus)) open(DeliveryDialog.Collect(card))
    }

    fun dismissDialog() {
        if (_state.value.busy) return
        _state.update { it.copy(dialog = null, dialogError = null, reasonShowError = false) }
    }

    fun chooseReason(reason: String?) =
        _state.update { it.copy(reasonChoice = reason, reasonShowError = false, dialogError = null) }

    fun setReasonText(text: String) =
        _state.update { it.copy(reasonText = text.take(DeliveryThresholds.REASON_MAX), reasonShowError = false) }

    // --- Acciones -------------------------------------------------------------------------------

    fun assign(driverId: Int) {
        val dialog = _state.value.dialog as? DeliveryDialog.Assign ?: return
        val st = _state.value
        // La tarifa solo viaja si el campo se ve, se puede editar y el valor CAMBIÓ: así quien no la toca no la pisa.
        var fee: Double? = null
        if (st.showFeeField && st.canEditFee) {
            if (!isValidAssignFeeText(st.feeText)) {
                _state.update { it.copy(dialogError = DeliveryFeeCopy.INVALID_AMOUNT) }
                return
            }
            fee = assignFeeToSend(st.feeInitialText, st.feeText)
        }
        run(okMessage = DeliveryCopy.text("ok.assigned")) {
            repository.assignDriver(dialog.card.sessionId, driverId, fee)
        }
    }

    fun confirmCancel() {
        val dialog = _state.value.dialog as? DeliveryDialog.Cancel ?: return
        val reason = currentReason()
        if (deliveryReasonError(reason, DeliveryReasonKind.CANCEL) != null) {
            _state.update { it.copy(reasonShowError = true) }
            return
        }
        run(okMessage = DeliveryCopy.text("ok.cancelled")) {
            repository.cancelDeliveryOrder(dialog.card.sessionId, reason)
        }
    }

    fun confirmFailed() {
        val dialog = _state.value.dialog as? DeliveryDialog.Failed ?: return
        val assignmentId = dialog.card.assignmentId ?: return
        val reason = currentReason()
        if (deliveryReasonError(reason, DeliveryReasonKind.FAILED) != null) {
            _state.update { it.copy(reasonShowError = true) }
            return
        }
        run(okMessage = DeliveryCopy.text("ok.failed")) {
            repository.updateAssignmentStatus(assignmentId, "failed", reason)
        }
    }

    fun confirmDelivered() {
        val dialog = _state.value.dialog as? DeliveryDialog.Delivered ?: return
        val assignmentId = dialog.card.assignmentId ?: return
        run(
            okMessage = DeliveryCopy.text("ok.delivered"),
            // D2b: el servidor dice que falta cobrar. Con o.ch / s.m se pasa al diálogo del motivo; sin ellos
            // solo se muestra el mensaje del catálogo (comportamiento normal).
            onError = { e ->
                if (e.code == COLLECTION_REQUIRED && _state.value.canForce) {
                    _state.update {
                        it.copy(dialog = DeliveryDialog.ForceDelivered(dialog.card), reasonChoice = null, reasonText = "", reasonShowError = false, dialogError = null)
                    }
                    true
                } else {
                    false
                }
            },
        ) {
            repository.updateAssignmentStatus(assignmentId, "delivered")
        }
    }

    fun confirmForceDelivered() {
        val dialog = _state.value.dialog as? DeliveryDialog.ForceDelivered ?: return
        val assignmentId = dialog.card.assignmentId ?: return
        if (!_state.value.canForce) return
        val reason = _state.value.reasonText.trim()
        if (forceReasonError(reason) != null) {
            _state.update { it.copy(reasonShowError = true) }
            return
        }
        run(okMessage = DeliveryCopy.text("ok.delivered")) {
            repository.updateAssignmentStatus(assignmentId, "delivered", forceReason = reason)
        }
    }

    fun confirmCollect() {
        val dialog = _state.value.dialog as? DeliveryDialog.Collect ?: return
        val assignmentId = dialog.card.assignmentId ?: return
        run(okMessage = DeliveryCopy.text("ok.collected")) {
            when (val r = repository.collectAssignment(assignmentId)) {
                is AppResult.Success -> AppResult.Success(Unit)
                is AppResult.Error -> r
                AppResult.Loading -> AppResult.Loading
            }
        }
    }

    private fun currentReason(): String =
        _state.value.let { deliveryResolveReason(it.reasonChoice, it.reasonText) }

    private fun run(
        okMessage: String,
        onError: ((AppResult.Error) -> Boolean)? = null,
        block: suspend () -> AppResult<Unit>,
    ) {
        val s = _state.value
        if (!s.canAssign || s.busy) return
        _state.update { it.copy(busy = true, dialogError = null) }
        scope.launch {
            val result = try {
                block()
            } catch (e: CancellationException) {
                _state.update { it.copy(busy = false) }
                throw e
            }
            when (result) {
                is AppResult.Success -> {
                    _state.update { it.copy(busy = false, dialog = null) }
                    _messages.tryEmit(okMessage)
                }
                is AppResult.Error -> {
                    if (onError?.invoke(result) == true) {
                        _state.update { it.copy(busy = false) }
                    } else {
                        _state.update { it.copy(busy = false, dialogError = result.message) }
                        _messages.tryEmit(result.message)
                    }
                }
                AppResult.Loading -> _state.update { it.copy(busy = false) }
            }
            // Recarga SIEMPRE: tras un error (p. ej. "ya no se puede asignar") el tablero cambió y hay que verlo.
            store.refresh()
        }
    }
}
