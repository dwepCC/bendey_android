package com.bendey.restaurant.core.data.locale

import com.bendey.restaurant.core.domain.locale.TenantLocale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Lectura tolerante del bloque `locale` de `GET /api/session/capabilities`.
 *
 * - Sin bloque (servidor anterior a I3), JSON roto o `locale` que no es objeto → null: quien llama
 *   se queda con lo que tenía (lo guardado o Perú).
 * - Campo ausente o de otro tipo → el valor peruano de ese campo (nunca se cae).
 * - `identity_types`, `doc_kinds`, `geo_levels` y el resto de la respuesta se ignoran.
 */
object TenantLocaleJson {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Respuesta completa de capabilities → locale, o null si no trae un `locale` usable. */
    fun fromCapabilities(body: String): TenantLocale? {
        val root = parseObject(body) ?: return null
        val locale = root["locale"] as? JsonObject ?: return null
        return fromLocaleObject(locale)
    }

    /** Solo el objeto `locale` (lo que se guarda en DataStore) → locale, o null si no se puede leer. */
    fun fromLocaleJson(raw: String?): TenantLocale? {
        if (raw.isNullOrBlank()) return null
        return parseObject(raw)?.let { fromLocaleObject(it) }
    }

    /** Extrae el objeto `locale` crudo de la respuesta, para guardarlo tal cual. */
    fun localeObjectOf(body: String): String? =
        (parseObject(body)?.get("locale") as? JsonObject)?.toString()

    private fun parseObject(raw: String): JsonObject? =
        runCatching { json.parseToJsonElement(raw) as? JsonObject }.getOrNull()

    private fun fromLocaleObject(o: JsonObject): TenantLocale {
        val pe = TenantLocale.PE
        val labels = (o["labels"] as? JsonObject)
            ?.mapNotNull { (k, v) -> v.str()?.let { k to it } }
            ?.toMap()
            .orEmpty()
        return TenantLocale(
            country = o["country"].str() ?: pe.country,
            currency = o["currency"].str() ?: pe.currency,
            currencySymbol = o["currency_symbol"].str() ?: pe.currencySymbol,
            symbolSpacing = (o["symbol_spacing"] as? JsonPrimitive)?.booleanOrNull ?: pe.symbolSpacing,
            decimals = (o["decimals"] as? JsonPrimitive)?.intOrNull ?: pe.decimals,
            numberLocale = o["number_locale"].str() ?: pe.numberLocale,
            timeZone = o["time_zone"].str() ?: pe.timeZone,
            dateFormat = o["date_format"].str() ?: pe.dateFormat,
            phonePrefix = o["phone_prefix"].str() ?: pe.phonePrefix,
            taxName = o["tax_name"].str() ?: pe.taxName,
            taxAuthority = o["tax_authority"].str() ?: pe.taxAuthority,
            // Las etiquetas que falten siguen con el texto peruano.
            labels = TenantLocale.PE_LABELS + labels,
        ).sanitized()
    }

    private fun JsonElement?.str(): String? =
        (this as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }
}
