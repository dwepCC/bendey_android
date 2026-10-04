package com.bendey.restaurant.core.domain.onboarding

/** Nivel de un paso del checklist (ONBOARDING §5.1). Solo REQUIRED y RECOMMENDED cuentan para el avance. */
enum class OnboardingTier(val apiValue: String) {
    REQUIRED("required"),
    RECOMMENDED("recommended"),
    ADVANCED("advanced"),
    ;

    companion object {
        fun fromApi(value: String?): OnboardingTier? = entries.firstOrNull { it.apiValue == value }
    }
}

/** Claves de paso que esta app sabe pintar. Una clave desconocida del servidor se ignora (compatibilidad hacia adelante). */
enum class OnboardingStepKey(val apiKey: String, val defaultTier: OnboardingTier) {
    MENU("menu", OnboardingTier.REQUIRED),
    FIRST_SALE("first_sale", OnboardingTier.REQUIRED),
    TABLES("tables", OnboardingTier.RECOMMENDED),
    PRINTER("printer", OnboardingTier.RECOMMENDED),
    AUTH_PIN("auth_pin", OnboardingTier.RECOMMENDED),
    TEAM("team", OnboardingTier.RECOMMENDED),
    QR_MENU("qr_menu", OnboardingTier.RECOMMENDED),
    SUNAT("sunat", OnboardingTier.ADVANCED),
    RECIPES("recipes", OnboardingTier.ADVANCED),
    DELIVERY("delivery", OnboardingTier.ADVANCED),
    BRANCHES("branches", OnboardingTier.ADVANCED),
    ;

    companion object {
        fun fromApi(value: String?): OnboardingStepKey? = entries.firstOrNull { it.apiKey == value }
    }
}

/** Paso tal como lo devuelve `GET /api/restaurant/onboarding`. */
data class OnboardingStep(
    val key: String,
    val tier: OnboardingTier?,
    val done: Boolean,
    val skipped: Boolean,
    /** El servidor no puede saberlo (p. ej. la impresora es de ESTE equipo): decide el cliente. */
    val localOnly: Boolean,
    val count: Int?,
)

data class OnboardingServerProgress(
    val sellReady: Boolean = false,
    val operateDone: Int = 0,
    val operateTotal: Int = 0,
    val percent: Int = 0,
)

data class OnboardingState(
    val dismissed: Boolean = false,
    val businessSubtype: String = "",
    val sampleDataLoaded: Boolean = false,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val firstSaleSeen: Boolean = false,
    val progress: OnboardingServerProgress = OnboardingServerProgress(),
    val steps: List<OnboardingStep> = emptyList(),
)

/** Cambios de preferencia (`PATCH /api/restaurant/onboarding`). Solo se envía lo que no es null. */
data class OnboardingPreferencesUpdate(
    val dismissed: Boolean? = null,
    val skip: List<String>? = null,
    val unskip: List<String>? = null,
    val businessSubtype: String? = null,
)

data class SampleMenuResult(val created: Int)

data class SampleDataDeleteResult(val deleted: Int, val deactivated: Int) {
    /** Lo que se le dice al administrador tras "Borrar ejemplos" (tuteo, sin jerga). */
    fun userMessage(): String = when {
        deleted > 0 && deactivated > 0 -> "Se borraron $deleted y se desactivaron $deactivated porque ya tienen ventas."
        deleted > 0 -> if (deleted == 1) "Se borró 1 producto de ejemplo." else "Se borraron $deleted productos de ejemplo."
        deactivated > 0 -> "Se desactivaron $deactivated porque ya tienen ventas."
        else -> "No había ejemplos que borrar."
    }
}

enum class SunatRequestState(val apiValue: String) {
    NONE("none"),
    REQUESTED("requested"),
    IN_REVIEW("in_review"),
    ACTIVE("active"),
    ;

    companion object {
        /** Un valor desconocido se trata como "sin solicitar": lo más seguro es volver a ofrecer el botón. */
        fun fromApi(value: String?): SunatRequestState = entries.firstOrNull { it.apiValue == value } ?: NONE
    }
}

data class SunatRequestStatus(
    val state: SunatRequestState,
    /** ISO-8601 tal como lo manda el servidor; null mientras no se haya solicitado. */
    val requestedAt: String? = null,
)

/**
 * El servidor no ofrece (todavía) el checklist para esta sesión: 403 (no es administrador) o 404
 * (backend sin la ruta). La UI lo trata como "no mostrar nada", no como un error que reintentar.
 */
class OnboardingUnavailableException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
