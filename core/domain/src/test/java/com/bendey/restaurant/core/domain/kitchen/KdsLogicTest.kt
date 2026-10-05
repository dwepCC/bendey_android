package com.bendey.restaurant.core.domain.kitchen

import com.bendey.restaurant.core.domain.restaurant.ComandaStatus
import com.bendey.restaurant.core.domain.restaurant.KitchenItem
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KdsLogicTest {
    private val now = Instant.parse("2026-10-04T15:00:00Z")
    private fun ago(min: Int, sec: Int = 0) = now.minusSeconds(min * 60L + sec).toString()

    private fun item(
        id: Int,
        status: ComandaStatus = ComandaStatus.PENDIENTE,
        orderId: Int? = 1,
        createdAt: String? = ago(0),
        updatedAt: String? = null,
        sessionOpenedAt: String? = null,
        est: Int = 0,
        type: String? = "dine_in",
        table: String? = "Mesa 5",
        area: String? = null,
    ) = KitchenItem(
        id = id, productName = "P$id", quantity = 1.0, notes = null, modifiersJson = null, status = status,
        orderNumber = 1, orderCode = "TK-1", tableName = table, floorName = null, customerName = null,
        waiterName = null, orderType = type, preparationArea = area, createdAt = createdAt, updatedAt = updatedAt,
        sessionOpenedAt = sessionOpenedAt, orderId = orderId, areaEstimatedMinutes = est,
    )

    // ---- tiempo: desde la COMANDA, no desde la sesion ----
    @Test fun plato_recien_enviado_a_mesa_abierta_hace_1h_dice_0_min() {
        val round = buildKdsRounds(listOf(item(1, createdAt = ago(0, 10), sessionOpenedAt = ago(60)))).single()
        assertEquals(0, round.minutes(now))
        assertEquals("0 min", kdsElapsedLabel(round.minutes(now)))
    }

    @Test fun elapsed_baja_a_entero_y_no_es_negativo() {
        assertEquals(7, kdsElapsedMinutes(Instant.parse(ago(7, 59)), now))
        assertEquals(0, kdsElapsedMinutes(now.plusSeconds(120), now))
        assertNull(kdsElapsedMinutes(null, now))
    }

    @Test fun etiqueta_de_tiempo() {
        assertEquals("59 min", kdsElapsedLabel(59))
        assertEquals("1 h 00 min", kdsElapsedLabel(60))
        assertEquals("2 h 05 min", kdsElapsedLabel(125))
        assertEquals("", kdsElapsedLabel(null))
    }

    // ---- urgencia, umbral de respaldo 8 / 15 ----
    @Test fun urgencia_respaldo() {
        val c = KdsColumn.PREPARANDO
        assertEquals(KdsUrgency.VERDE, kdsUrgency(c, 7))
        assertEquals(KdsUrgency.AMBAR, kdsUrgency(c, 8))
        assertEquals(KdsUrgency.AMBAR, kdsUrgency(c, 15))
        assertEquals(KdsUrgency.ROJO, kdsUrgency(c, 16))
        assertEquals(KdsUrgency.VERDE, kdsUrgency(KdsColumn.NUEVO, null))
    }

    // ---- urgencia por estimado del area: 100 % / 150 % ----
    @Test fun urgencia_con_estimado_10() {
        val c = KdsColumn.NUEVO
        assertEquals(KdsUrgency.VERDE, kdsUrgency(c, 9, 10))
        assertEquals(KdsUrgency.AMBAR, kdsUrgency(c, 10, 10))
        assertEquals(KdsUrgency.AMBAR, kdsUrgency(c, 15, 10))
        assertEquals(KdsUrgency.ROJO, kdsUrgency(c, 16, 10))
    }

    @Test fun urgencia_con_estimado_20_ignora_el_respaldo() {
        assertEquals(KdsUrgency.VERDE, kdsUrgency(KdsColumn.PREPARANDO, 16, 20))
        assertEquals(KdsUrgency.ROJO, kdsUrgency(KdsColumn.PREPARANDO, 31, 20))
    }

    // ---- LISTO: ambar 2, rojo 5; ENTREGADO nunca urgente ----
    @Test fun urgencia_en_listo() {
        val c = KdsColumn.LISTO
        assertEquals(KdsUrgency.VERDE, kdsUrgency(c, 1))
        assertEquals(KdsUrgency.AMBAR, kdsUrgency(c, 2))
        assertEquals(KdsUrgency.AMBAR, kdsUrgency(c, 4))
        assertEquals(KdsUrgency.ROJO, kdsUrgency(c, 5))
        assertEquals(KdsUrgency.VERDE, kdsUrgency(KdsColumn.ENTREGADO, 500))
    }

    @Test fun estimado_de_la_ronda_es_el_del_plato_mas_lento_aun_no_entregado() {
        val r = buildKdsRounds(
            listOf(item(1, est = 3), item(2, est = 20), item(3, est = 60, status = ComandaStatus.ENTREGADA)),
        ).single()
        assertEquals(20, r.areaEstimatedMinutes)
    }

    @Test fun en_listo_el_estimado_no_cuenta() {
        assertEquals(KdsUrgency.AMBAR, kdsUrgency(KdsColumn.LISTO, 3, 30))
    }

    @Test fun estimado_cero_o_negativo_usa_respaldo() {
        assertEquals(KdsUrgency.AMBAR, kdsUrgency(KdsColumn.NUEVO, 9, 0))
        assertEquals(KdsUrgency.AMBAR, kdsUrgency(KdsColumn.NUEVO, 9, -3))
    }

    // ---- columna de la ronda = item menos avanzado ----
    @Test fun columna_es_el_item_menos_avanzado() {
        assertEquals(KdsColumn.NUEVO, kdsColumnOf(listOf(item(1, ComandaStatus.LISTA), item(2, ComandaStatus.PENDIENTE))))
        assertEquals(KdsColumn.PREPARANDO, kdsColumnOf(listOf(item(1, ComandaStatus.LISTA), item(2, ComandaStatus.PREPARACION))))
        assertEquals(KdsColumn.LISTO, kdsColumnOf(listOf(item(1, ComandaStatus.ENTREGADA), item(2, ComandaStatus.LISTA))))
        assertEquals(KdsColumn.ENTREGADO, kdsColumnOf(listOf(item(1, ComandaStatus.ENTREGADA))))
    }

    @Test fun una_tarjeta_por_ronda_y_combos_comparten_id() {
        val items = listOf(item(1, orderId = 10), item(1, orderId = 10), item(2, orderId = 10), item(3, orderId = 11))
        val rounds = buildKdsRounds(items)
        assertEquals(2, rounds.size)
        assertEquals(listOf(1, 2), rounds.first { it.orderId == 10 }.comandaIds)
    }

    // ---- FIFO ----
    @Test fun fifo_mas_antigua_primero_y_sin_fecha_al_final() {
        val rounds = buildKdsRounds(
            listOf(
                item(1, orderId = 1, createdAt = ago(2)),
                item(2, orderId = 2, createdAt = ago(30)),
                item(3, orderId = 3, createdAt = null),
                item(4, orderId = 4, createdAt = ago(10)),
            ),
        )
        val ids = buildKdsBoard(rounds, now).getValue(KdsColumn.NUEVO).map { it.orderId }
        assertEquals(listOf(2, 4, 1, 3), ids)
    }

    @Test fun entregado_ultimas_10_y_menos_de_60_min_la_mas_reciente_primero() {
        // la ventana se mide por ultima actividad (updated_at), no por created_at
        val items = (1..12).map { item(it, ComandaStatus.ENTREGADA, orderId = it, createdAt = ago(300), updatedAt = ago(it)) } +
            item(99, ComandaStatus.ENTREGADA, orderId = 99, createdAt = ago(300), updatedAt = ago(61)) +
            item(98, ComandaStatus.ENTREGADA, orderId = 98, createdAt = null)
        val col = buildKdsBoard(buildKdsRounds(items), now).getValue(KdsColumn.ENTREGADO)
        assertEquals(10, col.size)
        assertEquals(1, col.first().orderId)
        assertEquals(10, col.last().orderId)
        assertFalse(col.any { it.orderId == 99 || it.orderId == 98 })
    }

    // ---- acciones: 1 toque por ronda, 3 toques en total ----
    @Test fun acciones_por_columna() {
        assertEquals(listOf("EMPEZAR", "TODO LISTO"), kdsActions(KdsColumn.NUEVO).map { it.label })
        assertEquals(listOf(ComandaStatus.PREPARACION, ComandaStatus.LISTA), kdsActions(KdsColumn.NUEVO).map { it.target })
        assertEquals(listOf("TODO LISTO"), kdsActions(KdsColumn.PREPARANDO).map { it.label })
        assertEquals(listOf("ENTREGADO"), kdsActions(KdsColumn.LISTO).map { it.label })
        assertTrue(kdsActions(KdsColumn.ENTREGADO).isEmpty())
    }

    @Test fun solo_se_mandan_los_ids_que_no_han_avanzado() {
        val items = listOf(item(1, ComandaStatus.PENDIENTE), item(2, ComandaStatus.LISTA), item(3, ComandaStatus.PREPARACION))
        assertEquals(listOf(1, 3), kdsIdsToAdvance(items, ComandaStatus.LISTA).sorted())
        assertEquals(listOf(1), kdsIdsToAdvance(items, ComandaStatus.PREPARACION))
    }

    @Test fun antes_de_enviar_se_revalida_contra_el_estado_actual() {
        val current = mapOf(1 to ComandaStatus.PENDIENTE, 2 to ComandaStatus.LISTA)
        // 2 ya lo avanzo otra pantalla; 3 se cobro (ya no existe)
        assertEquals(listOf(1), kdsIdsStillBehind(listOf(1, 2, 3), current, ComandaStatus.LISTA))
    }

    // ---- optimista ----
    @Test fun override_solo_adelanta_nunca_retrocede() {
        val items = listOf(item(1, ComandaStatus.PENDIENTE), item(2, ComandaStatus.LISTA))
        val out = applyKdsOverrides(items, mapOf(1 to ComandaStatus.PREPARACION, 2 to ComandaStatus.PREPARACION))
        assertEquals(ComandaStatus.PREPARACION, out[0].status)
        assertEquals(ComandaStatus.LISTA, out[1].status)
    }

    @Test fun mesa_de_4_platos_se_despacha_en_3_toques_por_ronda() {
        var items = (1..4).map { item(it, ComandaStatus.PENDIENTE) }
        var taps = 0
        while (true) {
            val round = buildKdsRounds(items).single()
            val action = kdsActions(round.column).firstOrNull() ?: break
            items = applyKdsOverrides(items, kdsIdsToAdvance(round.items, action.target).associateWith { action.target })
            taps++
        }
        assertEquals(3, taps)
    }

    // ---- titulos (identicos a kdsHeadline de Tauri) ----
    @Test fun titulos() {
        fun title(type: String?, table: String?, code: String?) =
            kdsTicketTitle(buildKdsRounds(listOf(item(1, type = type, table = table).copy(orderCode = code))).single())
        assertEquals("MESA 5", title("dine_in", "Mesa 5", "TK-1"))
        assertEquals("LLEVAR #123", title("takeaway", null, "#123"))
        assertEquals("DELIVERY #45", title("delivery", null, "#45"))
        assertEquals("DELIVERY", title("delivery", null, null))
        assertEquals("TK-9", title("dine_in", null, "tk-9"))
    }

    @Test fun textos_identicos_a_tauri() {
        assertEquals("Comanda #2", KdsCopy.round(2))
        assertEquals("Mozo: Ana", KdsCopy.waiter("Ana"))
        assertEquals("MESA 5 · Comanda #2 → LISTO", KdsCopy.undoMoved("MESA 5", 2, "LISTO"))
        assertEquals("Anular: 2× Lomo", KdsCopy.voidItem("2× Lomo"))
        assertEquals("ATRASADO", KdsCopy.LATE)
        assertEquals("Deshacer", KdsCopy.UNDO_BUTTON)
    }

    // ---- sonido ----
    @Test fun politica_de_sonido_de_pedido() {
        assertTrue(shouldPlayNewOrderSound("restaurant.order.created"))
        assertTrue(shouldPlayNewOrderSound("menu.order.accepted"))
        assertFalse(shouldPlayNewOrderSound("menu.order.created"))
        assertFalse(shouldPlayNewOrderSound("restaurant.comanda.updated"))
    }

    @Test fun escalada_sonora_y_silencio_de_5_min() {
        val t = 1_000_000L
        assertFalse(kdsShouldPlayEscalation(false, false, null, 0, t)) // sin rojas
        assertTrue(kdsShouldPlayEscalation(true, true, t - 1000, 0, t)) // roja nueva
        assertFalse(kdsShouldPlayEscalation(false, true, t - 119_000, 0, t)) // no repite antes de 2 min
        assertTrue(kdsShouldPlayEscalation(false, true, t - 120_000, 0, t)) // repite a los 2 min
        val snooze = t + KdsThresholds.ESCALATION_SNOOZE_MS
        assertFalse(kdsShouldPlayEscalation(true, true, null, snooze, t + 1000)) // silenciada 5 min
        assertTrue(kdsShouldPlayEscalation(true, true, null, snooze, snooze)) // vuelve sola
    }
}
