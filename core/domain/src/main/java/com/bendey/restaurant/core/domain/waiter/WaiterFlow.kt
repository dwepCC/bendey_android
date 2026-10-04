package com.bendey.restaurant.core.domain.waiter

import com.bendey.restaurant.core.domain.model.PinStation
import com.bendey.restaurant.core.domain.restaurant.PosCartLine
import com.bendey.restaurant.core.domain.restaurant.TableSessionDetail
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reglas puras del flujo del mozo (R8). Sin Android: se prueban con JUnit.
 */

/**
 * Guard SINCRONO contra doble toque. Un `StateFlow` no alcanza: dos toques seguidos llegan antes de que
 * la recomposicion deshabilite el boton, y los dos verian `sending == false`. Este guard se toma ANTES de
 * lanzar la corrutina y de cualquier comprobacion asincrona, asi el segundo toque ve el candado ya puesto.
 */
class SyncGuard {
    private val held = AtomicBoolean(false)

    /** true si este llamador tomo el candado; false si ya lo tenia otro (hay que abortar). */
    fun tryAcquire(): Boolean = held.compareAndSet(false, true)

    fun release() {
        held.set(false)
    }

    val isHeld: Boolean get() = held.get()
}

/**
 * Header `Idempotency-Key` de "Enviar a cocina": UNA key por intento de carrito.
 * - Mismo carrito (misma firma) => misma key, tambien en reintentos tras un fallo o timeout.
 * - Carrito distinto => key nueva.
 * - Envio exitoso => [onSuccess] la descarta; el siguiente carrito (aunque sea identico) lleva key nueva.
 */
class IdempotencyKeyHolder(private val generator: () -> String = { UUID.randomUUID().toString() }) {
    private var signature: String? = null
    private var key: String? = null

    @Synchronized
    fun keyFor(cartSignature: String): String {
        val current = key
        if (current != null && signature == cartSignature) return current
        val fresh = generator()
        signature = cartSignature
        key = fresh
        return fresh
    }

    @Synchronized
    fun onSuccess() {
        signature = null
        key = null
    }
}

/** Firma estable del carrito: cambia si cambia cualquier linea, cantidad, precio, nota o modificador. */
fun cartSignature(cart: List<PosCartLine>): String = cart.joinToString("\n") { line ->
    listOf(
        line.key,
        line.itemKind,
        line.product.id.toString(),
        line.quantity.toString(),
        line.effectiveUnitPrice.toString(),
        line.notes,
        line.modifiersJson.orEmpty(),
        line.comboId?.toString().orEmpty(),
        line.comboConfigJson.orEmpty(),
    ).joinToString("|")
}

// ---------------------------------------------------------------------------------------------------
// Estacion recordada por dispositivo
// ---------------------------------------------------------------------------------------------------

object StationMemory {
    /** La estacion de administracion usa correo y contrasena: no se recuerda ni se salta. */
    fun isRememberable(station: PinStation): Boolean = station != PinStation.ADMIN

    /** Clave de preferencias POR RESTAURANTE: un dispositivo vinculado a otro restaurante no hereda estacion. */
    fun prefKey(slug: String): String = "station_$slug"

    /** Valor guardado o ilegible/ADMIN => null (no hay estacion que saltar). */
    fun decode(raw: String?): PinStation? =
        raw?.let { PinStation.fromRouteKey(it) }?.takeIf { isRememberable(it) }

    fun encode(station: PinStation): String? = station.routeKey.takeIf { isRememberable(station) }
}

// ---------------------------------------------------------------------------------------------------
// Mesa: cerrar la sesion si se sale sin pedir
// ---------------------------------------------------------------------------------------------------

/**
 * Hay que cerrar esta sesion al salir de la mesa? Solo si es de una mesa, no tiene NINGUNA linea vigente,
 * ni total, y no hay un carrito a medias ni un envio/cobro en curso. Una sesion con items NUNCA se cierra
 * desde aqui (el backend ademas lo rechaza).
 *
 * Existe porque abrir la mesa al tocarla crea la sesion antes de saber si habra pedido.
 */
fun shouldAutoCloseEmptySession(
    session: TableSessionDetail?,
    cartSize: Int,
    busy: Boolean = false,
): Boolean {
    if (session == null || busy) return false
    if (session.tableName == null) return false
    if (cartSize > 0) return false
    if (session.totalAmount > 0.0) return false
    val live = session.orders.sumOf { order -> order.comandas.count { it.cancelledAt == null } }
    return live == 0
}

// ---------------------------------------------------------------------------------------------------
// Comensales y nota de la mesa (PATCH /sessions/:id)
// ---------------------------------------------------------------------------------------------------

/**
 * Cuerpo del PATCH de la sesion. El backend REEMPLAZA nombre, telefono, direccion, referencia, minutos y
 * nota con lo que reciba (un campo omitido se guarda vacio), asi que se reenvian los valores actuales de lo
 * que NO se esta editando. `guests` va aparte: hoy el backend puede ignorarlo; el cliente relee la sesion.
 */
data class SessionPatch(
    val customerName: String,
    val customerPhone: String,
    val deliveryAddress: String,
    val deliveryReference: String,
    val estimatedMinutes: Int,
    val notes: String,
    val guests: Int?,
)

fun buildSessionPatch(current: TableSessionDetail, guests: Int? = null, notes: String? = null): SessionPatch =
    SessionPatch(
        customerName = current.customerName.orEmpty(),
        customerPhone = current.customerPhone.orEmpty(),
        deliveryAddress = current.deliveryAddress.orEmpty(),
        deliveryReference = current.deliveryReference.orEmpty(),
        estimatedMinutes = current.estimatedMinutes ?: 0,
        notes = notes?.trim() ?: current.notes.orEmpty(),
        guests = guests?.coerceAtLeast(1),
    )

const val GUESTS_NOT_SAVED_MESSAGE = "No se pudo actualizar el número de comensales en el servidor."

// ---------------------------------------------------------------------------------------------------
// Producto manual: trazabilidad de quien lo agrego
// ---------------------------------------------------------------------------------------------------

const val MANUAL_TAG_PREFIX = "Producto manual"

/** Texto que queda en la nota del item: "Producto manual · agregado por Ana". */
fun manualAddedByNote(userName: String?): String {
    val who = userName.orEmpty().trim()
    return if (who.isNotEmpty()) "$MANUAL_TAG_PREFIX · agregado por $who" else MANUAL_TAG_PREFIX
}

/**
 * Marca los productos manuales con quien los agrego. Se aplica justo antes de enviar para que el mozo no
 * pueda quitar la marca editando la nota. Va en `notes` (campo existente del item) y NO en la descripcion:
 * la descripcion termina en el comprobante del cliente. No muta el carrito original.
 */
fun stampManualLines(cart: List<PosCartLine>, userName: String?): List<PosCartLine> {
    val tag = manualAddedByNote(userName)
    return cart.map { line ->
        if (line.itemKind != "manual") return@map line
        val current = line.notes.trim()
        if (current.startsWith(MANUAL_TAG_PREFIX)) line
        else line.copy(notes = if (current.isEmpty()) tag else "$tag — $current")
    }
}

// ---------------------------------------------------------------------------------------------------
// Textos compartidos con Tauri (rama r8-waiter)
// ---------------------------------------------------------------------------------------------------

/** "Comanda #3 enviada · 4 ítems" (igual que `comandaSentMessage` de Tauri). */
fun comandaSentMessage(orderNumber: Int, itemCount: Int): String {
    val n = if (orderNumber > 0) " #$orderNumber" else ""
    if (itemCount <= 0) return "Comanda$n enviada"
    return "Comanda$n enviada · $itemCount ${if (itemCount == 1) "ítem" else "ítems"}"
}
