package com.bendey.restaurant.core.domain.delivery

import com.bendey.restaurant.core.domain.copy.ForbiddenTerms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant

/** Lógica pura de la vista Delivery (D1): semáforos, umbrales, orden, atención, acciones, permisos y paridad con Tauri. */
class DeliveryBoardLogicTest {
    private val now = Instant.parse("2026-10-05T15:00:00Z")

    private fun minsAgo(m: Int): String = now.minusSeconds(m * 60L).toString()

    private fun card(
        id: Int = 1,
        createdMinAgo: Int? = 10,
        estimated: Int? = null,
        assignmentId: Int? = 7,
        assignmentStatus: String? = null,
        assignedMinAgo: Int? = null,
        phone: String = "999000111",
        deliveredAt: String? = null,
        driver: DeliveryPerson? = null,
    ) = DeliveryCard(
        sessionId = id, assignmentId = assignmentId, source = "marketplace", customerName = "Ana", customerPhone = phone,
        address = "Av. Sol 123", reference = "frente al banco", itemsCount = 3, totalAmount = 54.5, orderStatus = "preparing",
        createdAt = createdMinAgo?.let { minsAgo(it) }, estimatedMinutes = estimated,
        assignmentStatus = assignmentStatus, assignedAt = assignedMinAgo?.let { minsAgo(it) }, deliveredAt = deliveredAt,
        driver = driver,
    )

    // ---- semáforo del reloj ----

    @Test fun conEstimadoVerdeMenosDel60PorCiento() {
        assertEquals(DeliveryTone.OK, deliveryElapsedTone(17, 30))
        assertEquals(DeliveryTone.OK, deliveryElapsedTone(0, 30))
    }

    @Test fun conEstimadoAmbarEntre60YCien() {
        assertEquals(DeliveryTone.WARN, deliveryElapsedTone(18, 30))
        assertEquals(DeliveryTone.WARN, deliveryElapsedTone(30, 30))
    }

    @Test fun conEstimadoRojoPorEncimaDelCien() {
        assertEquals(DeliveryTone.DANGER, deliveryElapsedTone(31, 30))
    }

    @Test fun sinEstimadoVerdeAmbarRojoA20Y35() {
        assertEquals(DeliveryTone.OK, deliveryElapsedTone(19, null))
        assertEquals(DeliveryTone.WARN, deliveryElapsedTone(20, null))
        assertEquals(DeliveryTone.WARN, deliveryElapsedTone(35, null))
        assertEquals(DeliveryTone.DANGER, deliveryElapsedTone(36, null))
        // Un estimado en cero no es un estimado: se usan 20/35.
        assertEquals(DeliveryTone.WARN, deliveryElapsedTone(25, 0))
    }

    @Test fun sinFechaNoHaySemaforo() {
        assertEquals(DeliveryTone.NONE, deliveryElapsedTone(null, 30))
        assertEquals(DeliveryTone.NONE, deliveryCardClock(card(createdMinAgo = null), DeliverySection.UNASSIGNED, now).tone)
    }

    // ---- alerta de aceptación (Asignados) ----

    @Test fun asignadosAmbarA5MinYRojoA10() {
        assertEquals(DeliveryTone.NONE, deliveryAcceptAlert(4))
        assertEquals(DeliveryTone.WARN, deliveryAcceptAlert(5))
        assertEquals(DeliveryTone.WARN, deliveryAcceptAlert(9))
        assertEquals(DeliveryTone.DANGER, deliveryAcceptAlert(10))
        assertEquals(DeliveryTone.NONE, deliveryAcceptAlert(null))
    }

    @Test fun laEsperaDeAceptacionSoloCuentaEnEstadoAssigned() {
        val sinAceptar = card(assignmentStatus = "assigned", assignedMinAgo = 12)
        assertEquals(12, deliveryAcceptWaitMinutes(sinAceptar, now))
        assertNull(deliveryAcceptWaitMinutes(card(assignmentStatus = "accepted", assignedMinAgo = 12), now))
    }

    @Test fun elBordeDeLaTarjetaEsElPeorEntreRelojYAceptacion() {
        // Reloj verde (3 de 30 min) pero 12 min sin aceptar: rojo.
        val c = card(createdMinAgo = 3, estimated = 30, assignmentStatus = "assigned", assignedMinAgo = 12)
        val clock = deliveryCardClock(c, DeliverySection.ASSIGNED, now)
        assertEquals(DeliveryTone.OK, clock.tone)
        assertEquals(DeliveryTone.DANGER, clock.accept)
        assertEquals(DeliveryTone.DANGER, clock.overall)
        assertEquals("3 min", clock.label)
        assertEquals("Sin aceptar hace rato", deliveryAcceptAlertLabel(clock.accept))
        assertEquals("Sin aceptar", deliveryAcceptAlertLabel(DeliveryTone.WARN))
        assertEquals("", deliveryAcceptAlertLabel(DeliveryTone.NONE))
    }

    @Test fun entregadosHoyNoTienenSemaforo() {
        val clock = deliveryCardClock(card(createdMinAgo = 300), DeliverySection.DELIVERED_TODAY, now)
        assertEquals(DeliveryTone.NONE, clock.overall)
    }

    @Test fun elTiempoSeFormateaComoElKds() {
        assertEquals("1 h 05 min", deliveryCardClock(card(createdMinAgo = 65), DeliverySection.IN_TRANSIT, now).label)
    }

    // ---- atención ----

    @Test fun porAsignarEIncidenciasSiemprePidenAtencion() {
        assertTrue(deliveryNeedsAttention(card(createdMinAgo = 0), DeliverySection.UNASSIGNED, now))
        assertTrue(deliveryNeedsAttention(card(createdMinAgo = 0), DeliverySection.INCIDENTS, now))
    }

    @Test fun entregadosNuncaPidenAtencion() {
        assertFalse(deliveryNeedsAttention(card(createdMinAgo = 500), DeliverySection.DELIVERED_TODAY, now))
    }

    @Test fun asignadosPidenAtencionSinAceptarOSiSePusoRoja() {
        assertFalse(deliveryNeedsAttention(card(createdMinAgo = 2, assignmentStatus = "assigned", assignedMinAgo = 2), DeliverySection.ASSIGNED, now))
        assertTrue(deliveryNeedsAttention(card(createdMinAgo = 2, assignmentStatus = "assigned", assignedMinAgo = 6), DeliverySection.ASSIGNED, now))
        assertTrue(deliveryNeedsAttention(card(createdMinAgo = 40, assignmentStatus = "assigned", assignedMinAgo = 1), DeliverySection.ASSIGNED, now))
    }

    @Test fun enCaminoSoloPideAtencionEnRojo() {
        assertFalse(deliveryNeedsAttention(card(createdMinAgo = 25, estimated = 30), DeliverySection.IN_TRANSIT, now))
        assertTrue(deliveryNeedsAttention(card(createdMinAgo = 31, estimated = 30), DeliverySection.IN_TRANSIT, now))
    }

    // ---- orden ----

    @Test fun masAntiguoPrimeroYEntregadosMasRecientePrimero() {
        val a = card(id = 1, createdMinAgo = 5)
        val b = card(id = 2, createdMinAgo = 30)
        val c = card(id = 3, createdMinAgo = 15)
        assertEquals(listOf(2, 3, 1), deliverySortSection(DeliverySection.UNASSIGNED, listOf(a, b, c)).map { it.sessionId })

        val d1 = card(id = 4, deliveredAt = minsAgo(50))
        val d2 = card(id = 5, deliveredAt = minsAgo(5))
        assertEquals(listOf(5, 4), deliverySortSection(DeliverySection.DELIVERED_TODAY, listOf(d1, d2)).map { it.sessionId })
    }

    @Test fun ordenarNoMutaLaListaOriginal() {
        val list = listOf(card(id = 1, createdMinAgo = 5), card(id = 2, createdMinAgo = 30))
        deliverySortSection(DeliverySection.UNASSIGNED, list)
        assertEquals(listOf(1, 2), list.map { it.sessionId })
    }

    // ---- conteos y badge ----

    @Test fun losConteosDelServidorGananYSinEllosSeUsanLasLongitudes() {
        val board = DeliveryBoardData(
            counts = DeliveryBoardCounts(unassigned = 9),
            unassigned = listOf(card(id = 1)),
            assigned = listOf(card(id = 2), card(id = 3)),
        )
        val c = deliveryBoardCounts(board)
        assertEquals(9, c.unassigned)
        assertEquals(2, c.assigned)
        assertEquals(0, c.incidents)
        assertEquals(DeliveryBoardCounts(), deliveryBoardCounts(null))
    }

    @Test fun badgeDeLaBarra() {
        assertEquals("", deliveryBadgeLabel(0))
        assertEquals("3", deliveryBadgeLabel(3))
        assertEquals("99", deliveryBadgeLabel(99))
        assertEquals("99+", deliveryBadgeLabel(100))
        assertEquals("1 pedido de delivery por asignar", deliveryBadgeDescription(1))
        assertEquals("4 pedidos de delivery por asignar", deliveryBadgeDescription(4))
        assertEquals("Entregas", deliveryBadgeDescription(0))
    }

    // ---- sonido: nunca en la carga inicial ----

    @Test fun laPrimeraCargaNoTieneNuevos() {
        val board = DeliveryBoardData(unassigned = listOf(card(id = 1), card(id = 2)))
        assertEquals(emptyList<Int>(), deliveryNewUnassignedIds(null, board))
    }

    @Test fun soloLosQueNoEstabanSonNuevos() {
        val board = DeliveryBoardData(unassigned = listOf(card(id = 1), card(id = 2), card(id = 3)))
        assertEquals(listOf(3), deliveryNewUnassignedIds(setOf(1, 2), board))
        assertEquals(emptyList<Int>(), deliveryNewUnassignedIds(setOf(1, 2, 3), board))
    }

    // ---- permisos ----

    @Test fun permisosDeVerYOperar() {
        assertTrue(canViewDelivery(listOf("d.v")))
        assertFalse(canViewDelivery(listOf("d.u")))
        assertFalse(canViewDelivery(null))
        assertTrue(canAssignDelivery(listOf("d.v", "d.u")))
        assertFalse(canAssignDelivery(listOf("d.v")))
        assertFalse(canAssignDelivery(emptyList()))
    }

    @Test fun elRepartidorNoOperaElTableroAunqueTengaDU() {
        val perms = listOf("d.v", "d.u")
        assertTrue(canOperateDeliveryBoard(perms, "cashier"))
        assertTrue(canOperateDeliveryBoard(perms, null))
        assertFalse(canOperateDeliveryBoard(perms, "driver"))
        assertFalse(canOperateDeliveryBoard(perms, "DRIVER"))
        assertFalse(canOperateDeliveryBoard(listOf("d.v"), "cashier"))
    }

    // ---- acciones por tarjeta ----

    @Test fun sinPermisoDeOperarSoloSePuedeLlamar() {
        for (s in DeliverySection.entries) {
            val acts = deliveryCardActions(card(), s, canAssign = false)
            assertTrue("$s: $acts", acts.all { it == DeliveryAction.CALL })
        }
        assertEquals(emptyList<DeliveryAction>(), deliveryCardActions(card(phone = ""), DeliverySection.UNASSIGNED, false))
    }

    @Test fun accionesPorSeccionConPermiso() {
        assertEquals(
            listOf(DeliveryAction.CALL, DeliveryAction.ASSIGN, DeliveryAction.CANCEL),
            deliveryCardActions(card(assignmentId = null), DeliverySection.UNASSIGNED, true),
        )
        assertEquals(
            listOf(DeliveryAction.CALL, DeliveryAction.REASSIGN, DeliveryAction.CANCEL),
            deliveryCardActions(card(), DeliverySection.ASSIGNED, true),
        )
        assertEquals(
            listOf(DeliveryAction.CALL, DeliveryAction.DELIVERED, DeliveryAction.FAILED, DeliveryAction.REASSIGN, DeliveryAction.CANCEL),
            deliveryCardActions(card(), DeliverySection.IN_TRANSIT, true),
        )
        assertEquals(
            listOf(DeliveryAction.CALL, DeliveryAction.ASSIGN, DeliveryAction.CANCEL),
            deliveryCardActions(card(), DeliverySection.INCIDENTS, true),
        )
        assertEquals(listOf(DeliveryAction.CALL), deliveryCardActions(card(), DeliverySection.DELIVERED_TODAY, true))
    }

    @Test fun enCaminoSinAsignacionNoOfreceEntregadoNiFallido() {
        val acts = deliveryCardActions(card(assignmentId = null), DeliverySection.IN_TRANSIT, true)
        assertFalse(DeliveryAction.DELIVERED in acts)
        assertFalse(DeliveryAction.FAILED in acts)
    }

    @Test fun telefonoParaMarcar() {
        assertEquals("tel:999000111", deliveryTelUri("999 000 111"))
        assertEquals("tel:+51999000111", deliveryTelUri("+51 999-000-111"))
        assertEquals("", deliveryTelUri("123"))
        assertEquals("", deliveryTelUri(null))
        assertEquals("", deliveryTelUri(""))
    }

    // ---- repartidores ----

    private fun driver(id: Int, name: String, available: Boolean, active: Int) =
        DeliveryBoardDriver(id, name, "999", "moto", available, active)

    @Test fun repartidoresDisponiblesPrimeroLuegoMenosCargaLuegoNombre() {
        val sorted = deliverySortDrivers(
            listOf(
                driver(1, "Zoe", true, 0),
                driver(2, "Beto", false, 0),
                driver(3, "Ana", true, 2),
                driver(4, "Carlos", true, 0),
            ),
        )
        assertEquals(listOf(4, 1, 3, 2), sorted.map { it.id })
    }

    @Test fun elNoDisponibleSeDeshabilitaConSuRazon() {
        val off = deliveryDriverChoice(driver(1, "Luis", false, 1))
        assertTrue(off.disabled)
        assertEquals("Marcado como no disponible", off.reason)
        assertEquals("1 pedido activo", off.activeLabel)
        val on = deliveryDriverChoice(driver(2, "Ana", true, 3))
        assertFalse(on.disabled)
        assertEquals("3 pedidos activos", on.activeLabel)
        assertEquals("Sin pedidos activos", deliveryActiveCountLabel(0))
    }

    // ---- motivos ----

    @Test fun elMotivoRapidoOElTextoEscritoSiEligioOtro() {
        assertEquals("No hay stock", deliveryResolveReason("No hay stock", "ignorado"))
        assertEquals("se cayó el sistema", deliveryResolveReason(DELIVERY_REASON_OTHER, "  se cayó el sistema  "))
        assertEquals("", deliveryResolveReason(null, "algo"))
    }

    @Test fun elMotivoLleva3A255Caracteres() {
        assertNotNull(deliveryReasonError("ab", DeliveryReasonKind.CANCEL))
        assertNull(deliveryReasonError("abc", DeliveryReasonKind.CANCEL))
        assertNull(deliveryReasonError("a".repeat(255), DeliveryReasonKind.FAILED))
        assertNotNull(deliveryReasonError("a".repeat(256), DeliveryReasonKind.FAILED))
        assertNotNull(deliveryReasonError("   ", DeliveryReasonKind.CANCEL))
        assertEquals("Escribe el motivo de la cancelación (mínimo 3 letras).", deliveryReasonError("", DeliveryReasonKind.CANCEL))
        assertEquals("Escribe el motivo por el que no se pudo entregar (mínimo 3 letras).", deliveryReasonError("", DeliveryReasonKind.FAILED))
    }

    @Test fun losMotivosRapidosSonLosDelSpec() {
        assertEquals(listOf("El cliente canceló", "No hay stock", "Dirección fuera de zona", "No contesta"), DeliveryCopy.cancelQuickReasons)
        assertEquals(3, DeliveryCopy.failedQuickReasons.size)
    }

    // ---- etiquetas ----

    @Test fun etiquetasDeOrigenYEstados() {
        assertEquals("POS", deliverySourceLabel("staff"))
        assertEquals("Menú digital", deliverySourceLabel("digital_menu"))
        assertEquals("Marketplace", deliverySourceLabel("marketplace"))
        assertEquals("POS", deliverySourceLabel("raro"))
        assertEquals("Preparando", deliveryKitchenLabel("preparing"))
        assertEquals("En cocina", deliveryKitchenLabel("raro"))
        assertEquals("Esperando que acepte", deliveryAssignmentLabel("assigned"))
        assertEquals("", deliveryAssignmentLabel(null))
        assertEquals(DeliveryChipTone.SUCCESS, deliveryKitchenTone("ready"))
        assertEquals(DeliveryChipTone.NEUTRAL, deliveryKitchenTone(null))
    }

    @Test fun lasSeccionesTienenSuEtiquetaYSuVacio() {
        assertEquals(
            listOf("Por asignar", "Asignados", "En camino", "Incidencias", "Entregados hoy"),
            DeliverySection.entries.map { deliverySectionLabel(it) },
        )
        DeliverySection.entries.forEach { assertTrue(deliverySectionEmpty(it).isNotBlank()) }
    }

    @Test fun textosSinUstedNiJerga() {
        for ((k, v) in DeliveryCopy.entries) assertTrue("$k: $v", ForbiddenTerms.find(v).isEmpty())
    }

    // ---- paridad con docs/DELIVERY_BOARD_COPY.md (la misma tabla que usa Tauri) ----

    private fun docRows(section: String): List<Pair<String, String>> {
        val doc = listOf("../../docs/DELIVERY_BOARD_COPY.md", "docs/DELIVERY_BOARD_COPY.md").map { File(it) }.firstOrNull { it.exists() }
        assertNotNull("no se encontró docs/DELIVERY_BOARD_COPY.md", doc)
        val block = doc!!.readText().replace("\r\n", "\n").split("## $section")[1].split("\n## ")[0]
        return Regex("^\\| `([^`]+)` \\| (.+) \\|$", RegexOption.MULTILINE).findAll(block).map { it.groupValues[1] to it.groupValues[2] }.toList()
    }

    @Test fun losTextosCoincidenConLaTablaDeParidad() {
        val rows = docRows("Textos")
        assertEquals(DeliveryCopy.entries.keys.toList(), rows.map { it.first })
        for ((k, v) in rows) assertEquals(k, v, DeliveryCopy.entries[k])
    }

    @Test fun losUmbralesCoincidenConLaTablaDeParidad() {
        val rows = docRows("Umbrales")
        assertEquals(DeliveryCopy.thresholds.keys.toList(), rows.map { it.first })
        for ((k, v) in rows) assertEquals(k, v, DeliveryCopy.thresholds[k])
    }
}
