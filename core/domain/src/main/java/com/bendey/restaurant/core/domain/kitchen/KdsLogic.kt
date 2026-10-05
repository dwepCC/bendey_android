package com.bendey.restaurant.core.domain.kitchen

import com.bendey.restaurant.core.domain.restaurant.ComandaStatus
import com.bendey.restaurant.core.domain.restaurant.KitchenItem
import com.bendey.restaurant.core.domain.restaurant.normalizePreparationAreaKey
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Logica pura del KDS (R7). Mismas reglas y misma tabla de casos que `kdsLogic.ts` de Tauri.
 *
 * - El tiempo corre desde `created_at` de la COMANDA (nunca `session_opened_at`).
 * - Una tarjeta = una RONDA (un pedido/`orderId`); su columna es la del item MENOS avanzado.
 * - Orden FIFO: la mas antigua primero (ENTREGADO: la mas reciente primero).
 */
enum class KdsColumn(val status: ComandaStatus, val label: String) {
    NUEVO(ComandaStatus.PENDIENTE, "NUEVO"),
    PREPARANDO(ComandaStatus.PREPARACION, "PREPARANDO"),
    LISTO(ComandaStatus.LISTA, "LISTO"),
    ENTREGADO(ComandaStatus.ENTREGADA, "ENTREGADO"),
    ;

    companion object {
        fun of(status: ComandaStatus): KdsColumn = entries.first { it.status == status }
    }
}

enum class KdsUrgency { VERDE, AMBAR, ROJO }

/** Umbrales de respaldo FIJOS (no configurables por tenant en R7). */
object KdsThresholds {
    const val FALLBACK_AMBER_MIN = 8
    const val FALLBACK_RED_MIN = 15
    const val AMBER_FACTOR = 1.0
    const val RED_FACTOR = 1.5
    const val LISTO_AMBER_MIN = 2
    const val LISTO_RED_MIN = 5
    const val DELIVERED_MAX_ROUNDS = 10
    const val DELIVERED_MAX_AGE_MIN = 60
    const val UNDO_WINDOW_MS = 5_000L
    const val ESCALATION_REPEAT_MS = 120_000L
    const val ESCALATION_SNOOZE_MS = 5 * 60_000L
}

fun kdsParseInstant(iso: String?): Instant? {
    if (iso.isNullOrBlank()) return null
    return runCatching { Instant.parse(iso) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(iso).toInstant() }.getOrNull()
        ?: runCatching {
            LocalDateTime.parse(iso.trim().replace(" ", "T").substringBefore("+").substringBefore("Z"))
                .atZone(ZoneId.systemDefault())
                .toInstant()
        }.getOrNull()
}

/** Minutos enteros (hacia abajo) desde [createdAt]; nunca negativos. `null` si no hay fecha valida. */
fun kdsElapsedMinutes(createdAt: Instant?, now: Instant): Int? {
    createdAt ?: return null
    return ChronoUnit.MINUTES.between(createdAt, now).coerceAtLeast(0).toInt()
}

fun kdsElapsedLabel(minutes: Int?): String {
    minutes ?: return ""
    if (minutes < 60) return "$minutes min"
    return "${minutes / 60} h ${(minutes % 60).toString().padStart(2, '0')} min"
}

/**
 * Urgencia por tiempo. NUEVO/PREPARANDO: verde < ambar <= minutos <= rojo(limite) < rojo, con
 * ambar = 100 % y rojo = por encima del 150 % del estimado del area (`areaEstimatedMinutes` > 0),
 * o 8 / 15 min si no hay estimado. LISTO: ambar desde 2 min, rojo desde 5. ENTREGADO: siempre verde.
 */
fun kdsUrgency(column: KdsColumn, minutes: Int?, areaEstimatedMinutes: Int = 0): KdsUrgency {
    minutes ?: return KdsUrgency.VERDE
    return when (column) {
        KdsColumn.ENTREGADO -> KdsUrgency.VERDE
        KdsColumn.LISTO -> when {
            minutes >= KdsThresholds.LISTO_RED_MIN -> KdsUrgency.ROJO
            minutes >= KdsThresholds.LISTO_AMBER_MIN -> KdsUrgency.AMBAR
            else -> KdsUrgency.VERDE
        }
        KdsColumn.NUEVO, KdsColumn.PREPARANDO -> {
            val amber: Double
            val red: Double
            if (areaEstimatedMinutes > 0) {
                amber = areaEstimatedMinutes * KdsThresholds.AMBER_FACTOR
                red = areaEstimatedMinutes * KdsThresholds.RED_FACTOR
            } else {
                amber = KdsThresholds.FALLBACK_AMBER_MIN.toDouble()
                red = KdsThresholds.FALLBACK_RED_MIN.toDouble()
            }
            when {
                minutes > red -> KdsUrgency.ROJO
                minutes >= amber -> KdsUrgency.AMBAR
                else -> KdsUrgency.VERDE
            }
        }
    }
}

/** Columna de una ronda = el estado menos avanzado de sus items. */
fun kdsColumnOf(items: List<KitchenItem>): KdsColumn =
    KdsColumn.of(items.minByOrNull { it.status.ordinal }?.status ?: ComandaStatus.PENDIENTE)

data class KdsRound(
    val key: String,
    val orderId: Int?,
    val sessionId: Int?,
    val orderCode: String?,
    val orderNumber: Int?,
    val tableName: String?,
    val floorName: String?,
    val waiterName: String?,
    val customerName: String?,
    val orderType: String?,
    val items: List<KitchenItem>,
    val column: KdsColumn,
    /** La comanda mas antigua de la ronda (`created_at`). */
    val createdAt: Instant?,
    /** Actividad mas reciente (`updated_at`, o `created_at`): acota la columna ENTREGADO. */
    val lastActivity: Instant?,
    /** Mayor estimado de area > 0 entre los items aun NO entregados; 0 = usar umbral de respaldo. */
    val areaEstimatedMinutes: Int,
) {
    /** Ids de comanda distintos (un combo se expande a varias lineas con el mismo id). */
    val comandaIds: List<Int> get() = items.map { it.id }.distinct()

    fun minutes(now: Instant): Int? = kdsElapsedMinutes(createdAt, now)
    fun urgency(now: Instant): KdsUrgency = kdsUrgency(column, minutes(now), areaEstimatedMinutes)
}

fun kdsRoundKey(item: KitchenItem): String =
    item.orderId?.let { "o$it" }
        ?: "c${item.orderCode.orEmpty()}#${item.orderNumber ?: 0}#${item.tableName.orEmpty()}"

/** Agrupa items (ya filtrados por area/tipo) en rondas. No ordena. */
fun buildKdsRounds(items: List<KitchenItem>): List<KdsRound> =
    items.groupBy { kdsRoundKey(it) }.map { (key, group) ->
        val first = group.first()
        KdsRound(
            key = key,
            orderId = first.orderId,
            sessionId = first.sessionId,
            orderCode = first.orderCode,
            orderNumber = first.orderNumber,
            tableName = first.tableName,
            floorName = first.floorName,
            waiterName = first.waiterName,
            customerName = first.customerName,
            orderType = first.orderType,
            items = group,
            column = kdsColumnOf(group),
            createdAt = group.mapNotNull { kdsParseInstant(it.createdAt) }.minOrNull(),
            lastActivity = group.mapNotNull { kdsParseInstant(it.updatedAt) ?: kdsParseInstant(it.createdAt) }.maxOrNull(),
            areaEstimatedMinutes = group.filter { it.status != ComandaStatus.ENTREGADA }
                .maxOfOrNull { it.areaEstimatedMinutes } ?: 0,
        )
    }

/**
 * Tablero de 4 columnas. FIFO (la mas antigua arriba) salvo ENTREGADO: solo lectura, las
 * ultimas [KdsThresholds.DELIVERED_MAX_ROUNDS] con menos de [KdsThresholds.DELIVERED_MAX_AGE_MIN]
 * min, la mas reciente primero. Rondas sin fecha valida al final.
 */
fun buildKdsBoard(rounds: List<KdsRound>, now: Instant): Map<KdsColumn, List<KdsRound>> {
    val oldestFirst = compareBy<KdsRound, Instant?>(nullsLast(naturalOrder())) { it.createdAt }
        .thenBy { it.orderNumber ?: 0 }
        .thenBy { it.orderId ?: 0 }
        .thenBy { it.key }
    return KdsColumn.entries.associateWith { col ->
        val inCol = rounds.filter { it.column == col }
        if (col == KdsColumn.ENTREGADO) {
            // Sin fecha de actividad no se puede acotar el tiempo: no se muestra (igual que Tauri).
            inCol.filter { r ->
                r.lastActivity != null &&
                    ChronoUnit.MILLIS.between(r.lastActivity, now) <= KdsThresholds.DELIVERED_MAX_AGE_MIN * 60_000L
            }
                .sortedWith(compareByDescending<KdsRound> { it.lastActivity ?: Instant.MIN }.thenBy { it.key })
                .take(KdsThresholds.DELIVERED_MAX_ROUNDS)
        } else {
            inCol.sortedWith(oldestFirst)
        }
    }
}

/** Accion de tarjeta (un toque por ronda). */
data class KdsAction(val label: String, val target: ComandaStatus)

/** Acciones disponibles por columna: la primaria primero. ENTREGADO = solo lectura. */
fun kdsActions(column: KdsColumn): List<KdsAction> = when (column) {
    KdsColumn.NUEVO -> listOf(
        KdsAction(KdsCopy.ACTION_START, ComandaStatus.PREPARACION),
        KdsAction(KdsCopy.ACTION_ALL_READY, ComandaStatus.LISTA),
    )
    KdsColumn.PREPARANDO -> listOf(KdsAction(KdsCopy.ACTION_ALL_READY, ComandaStatus.LISTA))
    KdsColumn.LISTO -> listOf(KdsAction(KdsCopy.ACTION_DELIVERED, ComandaStatus.ENTREGADA))
    KdsColumn.ENTREGADO -> emptyList()
}

/**
 * Ids que de verdad hay que mandar: SOLO los que aun no alcanzaron [target]. El backend es estricto
 * (si una comanda ya esta en un estado posterior y se pide uno anterior, falla todo el lote).
 */
fun kdsIdsToAdvance(items: List<KitchenItem>, target: ComandaStatus): List<Int> =
    items.filter { it.status.ordinal < target.ordinal }.map { it.id }.distinct()

/** Igual que [kdsIdsToAdvance] pero contra el estado ACTUAL (por id) justo antes de enviar. */
fun kdsIdsStillBehind(ids: List<Int>, currentStatusById: Map<Int, ComandaStatus>, target: ComandaStatus): List<Int> =
    ids.distinct().filter { id ->
        val cur = currentStatusById[id] ?: return@filter false // ya no esta (cobrada/anulada)
        cur.ordinal < target.ordinal
    }

/** Capa optimista: el estado mostrado = el del servidor salvo que el override vaya mas adelante. */
fun applyKdsOverrides(items: List<KitchenItem>, overrides: Map<Int, ComandaStatus>): List<KitchenItem> {
    if (overrides.isEmpty()) return items
    return items.map { item ->
        val o = overrides[item.id]
        if (o != null && o.ordinal > item.status.ordinal) item.copy(status = o) else item
    }
}

/** Encabezado de la tarjeta, igual que Tauri: `DELIVERY #45` / `LLEVAR #123` / `MESA 5` (mayusculas). */
fun kdsTicketTitle(round: KdsRound): String {
    val code = round.orderCode?.trim().orEmpty()
    if (round.orderType == "delivery") return "DELIVERY" + if (code.isNotEmpty()) " $code" else ""
    if (round.orderType == "takeaway") return "LLEVAR" + if (code.isNotEmpty()) " $code" else ""
    val table = round.tableName?.trim().orEmpty()
    if (table.isNotEmpty()) return table.uppercase()
    return if (code.isNotEmpty()) code.uppercase() else "PEDIDO #${round.sessionId ?: 0}"
}

fun kdsFilterItems(items: List<KitchenItem>, areaFilter: String, orderType: String?): List<KitchenItem> =
    items.filter { item ->
        (areaFilter == "all" || normalizePreparationAreaKey(item.preparationArea) == areaFilter) &&
            (orderType == null || item.orderType == orderType)
    }

/**
 * Escalada sonora de atrasos (solo NUEVO/PREPARANDO, con la pantalla de cocina abierta). Suena cuando
 * una ronda cruza a rojo y se repite cada [KdsThresholds.ESCALATION_REPEAT_MS] mientras siga habiendo
 * rojas. Silenciable [KdsThresholds.ESCALATION_SNOOZE_MS] (5 min): nunca se desactiva del todo.
 * Los pedidos nuevos NO dependen de esta regla (siguen sonando).
 */
fun kdsShouldPlayEscalation(
    hasNewRed: Boolean,
    hasAnyRed: Boolean,
    lastPlayedMs: Long?,
    snoozedUntilMs: Long,
    nowMs: Long,
): Boolean {
    if (!hasAnyRed || nowMs < snoozedUntilMs) return false
    if (hasNewRed) return true
    return lastPlayedMs == null || nowMs - lastPlayedMs >= KdsThresholds.ESCALATION_REPEAT_MS
}

/**
 * Politica del sonido de pedido nuevo (igual que Tauri). Suenan `restaurant.order.created` y
 * `menu.order.accepted` (pedido QR aprobado). `menu.order.created` NO suena: mientras el pedido del
 * cliente esta por_aprobar no llega al KDS; el sonido va al aprobarlo.
 */
fun shouldPlayNewOrderSound(eventType: String): Boolean =
    eventType == "restaurant.order.created" || eventType == "menu.order.accepted"

/** Textos del KDS con las MISMAS claves y valores que `content/kdsCopy.ts` (Tauri). */
object KdsCopy {
    const val ACTION_START = "EMPEZAR"
    const val ACTION_ALL_READY = "TODO LISTO"
    const val ACTION_DELIVERED = "ENTREGADO"
    const val LATE = "ATRASADO"
    const val UNDO_BUTTON = "Deshacer"
    const val EMPTY_TITLE = "Nada por aquí"
    const val EMPTY_PENDIENTE = "Cocina al día"
    const val EMPTY_OTHER = "Sin rondas"
    const val ITEM_DONE = "Listo"
    const val TICKET_MENU = "Más acciones"
    const val BELL_ON = "Sonido activado. Toca para silenciar la alerta de atraso 5 min."
    const val BELL_SNOOZED = "Alerta de atraso silenciada. Toca para reactivarla. Los pedidos nuevos siguen sonando."
    const val FILTER_ALL = "Todas"
    const val FILTER_LABEL = "Esta pantalla"
    fun round(n: Int) = "Comanda #$n"
    fun waiter(name: String) = "Mozo: $name"
    fun voidItem(item: String) = "Anular: $item"
    fun undoMoved(headline: String, n: Int, to: String) = "$headline · Comanda #$n → $to"
    fun deliveredNote(n: Int, minutes: Int) = "Últimos $n entregados · $minutes min"
}
