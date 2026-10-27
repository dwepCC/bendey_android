package com.bendey.restaurant.core.domain.locale

import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lo que la app necesita del país del tenant (paquete de país, BENDEY-PAQUETE-DE-PAIS-DISENO.md §6).
 * Llega en el bloque `locale` de `GET /api/session/capabilities`; mientras no llegue (app sin
 * sesión, servidor viejo, sin red) vale [PE], que reproduce exactamente lo que la app mostraba
 * antes del paquete de país («Perú idéntico»).
 *
 * Solo se modela lo que la app usa hoy: moneda, zona horaria y las etiquetas de [LabelKey] (I5). Los catálogos
 * (`identity_types`, `doc_kinds`, `geo_levels`) se ignoran hasta que haga falta.
 */
data class TenantLocale(
    val country: String = "PE",
    val currency: String = "PEN",
    val currencySymbol: String = "S/",
    val symbolSpacing: Boolean = true,
    val decimals: Int = 2,
    val numberLocale: String = "es-PE",
    val timeZone: String = "America/Lima",
    val dateFormat: String = "dd/MM/yyyy",
    val phonePrefix: String = "+51",
    val taxName: String = "IGV",
    val taxAuthority: String = "SUNAT",
    val labels: Map<String, String> = PE_LABELS,
) {
    /** Zona del tenant; si el servidor manda una zona que el dispositivo no conoce, Lima. */
    val zoneId: ZoneId
        get() = runCatching { ZoneId.of(timeZone) }.getOrDefault(LIMA)

    /**
     * Texto visible de [key] para este país (I5, BENDEY-PAQUETE-DE-PAIS-DISENO.md §6.3):
     *  - Perú → SIEMPRE el texto peruano del catálogo ([LabelKey.pe]), aunque el servidor mande otra
     *    cosa («Perú idéntico»).
     *  - Otro país → lo que mande el servidor en `labels`; si falta la clave, el texto peruano.
     */
    fun label(key: LabelKey): String {
        if (isPeru) return key.pe
        return labels[key.key]?.takeIf { it.isNotBlank() } ?: key.pe
    }

    /** Igual que [label] con la clave en texto («doc.receipt»); una clave desconocida → [fallback]. */
    fun label(key: String, fallback: String = PE_LABELS[key].orEmpty()): String {
        LabelKey.of(key)?.let { return label(it) }
        if (isPeru) return fallback
        return labels[key]?.takeIf { it.isNotBlank() } ?: fallback
    }

    /** País Perú (o sin país): manda el catálogo peruano. */
    val isPeru: Boolean
        get() = country.trim().ifEmpty { "PE" }.equals("PE", ignoreCase = true)

    /** Corrige valores imposibles para que un dato raro del servidor nunca rompa un formato. */
    fun sanitized(): TenantLocale = copy(
        currency = currency.trim().ifEmpty { PE.currency },
        currencySymbol = currencySymbol.trim().ifEmpty { currency.trim().ifEmpty { PE.currencySymbol } },
        decimals = decimals.coerceIn(0, 4),
        numberLocale = numberLocale.trim().ifEmpty { PE.numberLocale },
        timeZone = timeZone.trim().takeIf { tz -> runCatching { ZoneId.of(tz) }.isSuccess } ?: PE.timeZone,
    )

    companion object {
        val LIMA: ZoneId = ZoneId.of("America/Lima")

        /** Textos peruanos del catálogo completo ([LabelKey]); lo que trae `labels` por defecto. */
        val PE_LABELS: Map<String, String> = LabelKey.LABELS_PE

        /** Perú: el valor por defecto y el de los montos de suscripción de Bendey (siempre en soles). */
        val PE = TenantLocale()
    }
}

/**
 * Locale vigente del tenant de la sesión. Lo alimenta `LocaleRepository` (core/data): al arrancar
 * con lo guardado en DataStore y tras iniciar sesión con lo que responde el servidor. Sin nada de
 * eso queda [TenantLocale.PE].
 */
object TenantLocaleHolder {
    private val state = MutableStateFlow(TenantLocale.PE)

    val flow: StateFlow<TenantLocale> = state.asStateFlow()

    val current: TenantLocale
        get() = state.value

    fun set(locale: TenantLocale) {
        state.value = locale.sanitized()
    }

    fun reset() {
        state.value = TenantLocale.PE
    }
}
