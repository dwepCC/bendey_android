package com.bendey.restaurant.core.domain.cash

import com.bendey.restaurant.core.domain.copy.CashCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CashRulesTest {

    private fun chip(
        can: Boolean = true,
        open: Boolean = false,
        failed: Boolean = false,
        checked: Boolean = true,
    ) = decideCashChip(can, open, "2026-10-04T10:02:00-05:00", checked, failed)

    @Test fun mozoNoVeElChip_aunqueHayaCajaAbierta() {
        assertEquals(CashChipState.Hidden, chip(can = false, open = true))
        assertEquals(CashChipState.Hidden, chip(can = false))
    }

    @Test fun cajaAbiertaMuestraDesde() {
        assertEquals(CashChipState.Open("2026-10-04T10:02:00-05:00"), chip(open = true))
    }

    @Test fun cerradaConfirmadaEsCerrada() {
        assertEquals(CashChipState.Closed, chip())
        assertTrue(shouldShowClosedBanner(chip()))
    }

    @Test fun errorDeLecturaNoEsCerrada() {
        assertEquals(CashChipState.Unknown, chip(failed = true))
        assertFalse(shouldShowClosedBanner(chip(failed = true)))
    }

    @Test fun aunSinLeerNoAfirmaNada() {
        assertEquals(CashChipState.Hidden, chip(checked = false))
        assertFalse(shouldShowClosedBanner(chip(checked = false)))
    }

    @Test fun conSesionLocalYFalloDeRedSigueAbierta() {
        assertTrue(chip(open = true, failed = true) is CashChipState.Open)
    }

    // ── Resultado de cierre ──
    @Test fun cierreSinConteoNoEsCuadro() {
        assertEquals(CashCloseOutcome.NotCounted, resolveCloseOutcome(true, false, null, null, 100.0))
        // difference null NUNCA se toma como 0
        assertEquals(CashCloseOutcome.NotCounted, resolveCloseOutcome(true, null, null, null, 100.0))
    }

    @Test fun flagFalseGanaAunqueVengaUnNumero() {
        assertEquals(CashCloseOutcome.NotCounted, resolveCloseOutcome(true, false, 100.0, 0.0, 100.0))
    }

    @Test fun cierreContadoUsaLaDiferenciaDelServidor() {
        assertEquals(CashCloseOutcome.Counted(90.0, -10.0), resolveCloseOutcome(true, true, 90.0, -10.0, 100.0))
    }

    @Test fun contadoCeroExplicitoSiCuenta() {
        assertEquals(CashCloseOutcome.Counted(0.0, -50.0), resolveCloseOutcome(true, true, 0.0, -50.0, 50.0))
    }

    @Test fun contadoSinDiferenciaSeCalculaContraElEsperado() {
        assertEquals(CashCloseOutcome.Counted(90.0, -10.0), resolveCloseOutcome(true, null, 90.0, null, 100.0))
        assertEquals(CashCloseOutcome.NotCounted, resolveCloseOutcome(true, null, 90.0, null, null))
    }

    @Test fun abiertaNoTieneResultado() {
        assertEquals(CashCloseOutcome.NotClosed, resolveCloseOutcome(false, null, null, null, 1.0))
    }

    // ── Prellenado de apertura ──
    private fun brief(id: Int, status: CashSessionStatus, closing: Double?, closedAt: String?) =
        CashSessionBrief(id, null, null, 0.0, closing, 0.0, status, "2026-10-0${id}T08:00:00-05:00", closedAt)

    @Test fun prellenaConElContadoDelUltimoCierre() {
        val list = listOf(
            brief(1, CashSessionStatus.CLOSED, 120.0, "2026-10-01T22:00:00-05:00"),
            brief(2, CashSessionStatus.CLOSED, 80.5, "2026-10-02T22:00:00-05:00"),
        )
        assertEquals(80.5, lastCountedCash(list)!!, 0.0001)
    }

    @Test fun ignoraCierresSinConteoYAbiertas() {
        val list = listOf(
            brief(1, CashSessionStatus.CLOSED, 120.0, "2026-10-01T22:00:00-05:00"),
            brief(2, CashSessionStatus.CLOSED, null, "2026-10-02T22:00:00-05:00"),
            brief(3, CashSessionStatus.OPEN, null, null),
        )
        assertEquals(120.0, lastCountedCash(list)!!, 0.0001)
        assertNull(lastCountedCash(emptyList()))
    }

    @Test fun cajaRequeridaSoloConEfectivo() {
        assertTrue(cashSessionRequiredForPayments(true))
        assertFalse(cashSessionRequiredForPayments(false))
    }

    @Test fun horaDeApertura() {
        assertEquals("10:02", formatCashSince("2026-10-04T10:02:00-05:00"))
        assertEquals("10:02", formatCashSince("2026-10-04T15:02:00Z"))
        assertNull(formatCashSince(null))
        assertNull(formatCashSince("basura"))
    }

    @Test fun textosIgualesATauri() {
        assertEquals("Efectivo esperado", CashCopy.POPOVER_EXPECTED)
        assertEquals("Caja…", CashCopy.CHIP_UNKNOWN)
        assertEquals("Tu usuario no cobra en efectivo. Usa otro método o pide a un cajero.", CashCopy.CHECKOUT_CASH_DISABLED_ROLE)
        assertEquals("Caja abierta · desde 10:02 · Vendido S/ 1,240.00", CashCopy.chipOpenFull("10:02", "S/ 1,240.00"))
    }
}
