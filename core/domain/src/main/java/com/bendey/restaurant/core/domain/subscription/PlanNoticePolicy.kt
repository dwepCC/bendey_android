package com.bendey.restaurant.core.domain.subscription

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Cadencia del aviso de vencimiento del plan (R10.6). Copia de `SubscriptionExpiryModal.tsx` (Tauri):
 * cuando aparecer lo decide el backend (`showExpiryModal`); cerrar SIEMPRE cierra; el cierre se
 * recuerda por tenant + dia de Lima, salvo en suspendido/bloqueado (ahi reaparece al reabrir); y el
 * aviso / "Mi plan" son solo para el administrador (s.m).
 */
object PlanNoticePolicy {
    /** El hub se refresca cada media hora y al volver a primer plano. */
    const val REFRESH_MS = 30 * 60 * 1000L

    val REAPPEARS_ON_REOPEN = setOf("suspended", "blocked")

    private val LIMA: ZoneId = ZoneId.of("America/Lima")

    /** Dia calendario de Lima (yyyy-MM-dd); nunca la fecha del dispositivo. */
    fun limaDay(nowMs: Long): String = LocalDate.ofInstant(Instant.ofEpochMilli(nowMs), LIMA).toString()

    fun dismissKey(tenantSlug: String?): String = "subscription_expiry_modal_dismissed:${tenantSlug.orEmpty()}"

    fun persistsDismissal(urgencyTier: String): Boolean = urgencyTier !in REAPPEARS_ON_REOPEN

    fun shouldShow(
        showExpiryModal: Boolean,
        isAdmin: Boolean,
        urgencyTier: String,
        closedThisSession: Boolean,
        persistedDismissDay: String?,
        today: String,
    ): Boolean {
        if (!isAdmin || !showExpiryModal) return false
        if (closedThisSession) return false
        if (persistsDismissal(urgencyTier) && persistedDismissDay == today) return false
        return true
    }
}
