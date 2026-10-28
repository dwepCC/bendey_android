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
 * Solo se modela lo que la app usa hoy: moneda, zona horaria, las etiquetas de [LabelKey] (I5) y, para
 * los países que no siguen las reglas de SUNAT (Ecuador), los tipos de identificación, el tope de la
 * factura a consumidor final (`doc_kinds[invoice].final_consumer_max`) y las tarifas del impuesto por
 * producto (`tax_rates`). En Perú esos tres quedan vacíos y la app usa sus catálogos SUNAT de siempre.
 * `geo_levels` se ignora.
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
    /** Tipos de identificación del país (`identity_types`). Vacío = Perú (catálogo SUNAT 06 fijo de la app). */
    val identityTypes: List<IdentityTypeOption> = emptyList(),
    /** Tope de una factura a consumidor final sin identificar (EC: 50 USD); 0 = el país no lo fija. */
    val invoiceFinalConsumerMax: Double = 0.0,
    /** Tarifas del impuesto por producto (`tax_rates`, solo Ecuador). Vacío = catálogo SUNAT 07. */
    val taxRates: List<TaxRateOption> = emptyList(),
) {
    /**
     * El país usa las reglas de comprador de SUNAT: factura solo con RUC de 11 dígitos, boleta con
     * cliente sin documento hasta S/ 700. Solo Perú.
     */
    val usesSunatBuyerRules: Boolean
        get() = isPeru

    /** Tarifas del impuesto por producto, o null si el país usa el catálogo SUNAT 07 (Perú). */
    val countryTaxRates: List<TaxRateOption>?
        get() = taxRates.takeIf { !isPeru && it.isNotEmpty() }

    /**
     * Tarifa (%) del código de la línea en un país de tarifas; null en Perú o si el código no está en
     * el catálogo. OJO Ecuador: el código es el codigoPorcentaje del SRI («4» = 15 %); «10» es IVA 13 %.
     */
    fun taxRateOf(code: String?): Double? {
        val c = code?.trim().orEmpty()
        return countryTaxRates?.firstOrNull { it.code == c }?.rate
    }

    /** Código con el que nace una línea o producto nuevo en un país de tarifas; null en Perú. */
    val defaultTaxCode: String?
        get() = countryTaxRates?.let { rates -> (rates.firstOrNull { it.isDefault } ?: rates.first()).code }

    /** Tope de la factura a consumidor final, o null si el país no lo fija (Perú). */
    val finalConsumerMax: Double?
        get() = invoiceFinalConsumerMax.takeIf { !usesSunatBuyerRules && it > 0.0 }

    /** Tipo de identificación de consumidor final del país (clave «other», EC 07), o null. */
    val finalConsumerIdentity: IdentityTypeOption?
        get() = identityTypes.firstOrNull { it.key == "other" }

    /** Tipo de identificación por clave (`tax_id`, `national_id`…), o null si el país no lo define. */
    fun identityByKey(key: String): IdentityTypeOption? = identityTypes.firstOrNull { it.key == key }

    /** Tipo de identificación por código (`04`, `05`…), o null. */
    fun identityByCode(code: String?): IdentityTypeOption? {
        val c = code?.trim().orEmpty()
        if (c.isEmpty()) return null
        return identityTypes.firstOrNull { it.code == c }
    }

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

/** Un tipo de identificación del país (`identity_types` del locale). */
data class IdentityTypeOption(
    /** Código del país: EC «04» RUC, «05» Cédula, «06» Pasaporte, «07» Consumidor final, «08» Exterior. */
    val code: String,
    /** Clave estable: `tax_id`, `national_id`, `passport`, `foreign_id`, `other` (consumidor final). */
    val key: String,
    val label: String,
    val minLength: Int = 0,
    val maxLength: Int = 0,
    val numeric: Boolean = false,
)

/** Una tarifa del impuesto elegible por producto (`tax_rates` del locale, solo Ecuador). */
data class TaxRateOption(
    /** codigoPorcentaje del SRI: «4» = 15 %, «5» = 5 %, «0» = 0 %, «6» no objeto, «7» exento. */
    val code: String,
    val label: String,
    val rate: Double,
    val isDefault: Boolean = false,
)

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
