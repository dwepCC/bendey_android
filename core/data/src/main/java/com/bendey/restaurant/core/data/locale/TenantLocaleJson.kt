package com.bendey.restaurant.core.data.locale

import com.bendey.restaurant.core.domain.locale.IdentityTypeOption
import com.bendey.restaurant.core.domain.locale.TaxRateOption
import com.bendey.restaurant.core.domain.locale.TenantLocale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Lectura tolerante del bloque `locale` de `GET /api/session/capabilities`.
 *
 * - Sin bloque (servidor anterior a I3), JSON roto o `locale` que no es objeto → null: quien llama
 *   se queda con lo que tenía (lo guardado o Perú).
 * - Campo ausente o de otro tipo → el valor peruano de ese campo (nunca se cae).
 * - `labels` (I5): solo se toman los valores de texto no vacíos; si falta o no es un objeto, quedan
 *   las etiquetas peruanas. Quien decide qué se muestra es `TenantLocale.label()` (Perú siempre PE).
 * - `identity_types`, `doc_kinds` (solo el tope a consumidor final de la factura) y `tax_rates`
 *   (solo Ecuador) se leen tolerantes: filas rotas se descartan, y si faltan quedan vacíos.
 * - `geo_levels` y el resto de la respuesta se ignoran.
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
        val country = o["country"].str() ?: pe.country
        // Perú usa sus catálogos SUNAT fijos: los tres bloques por país quedan vacíos («Perú idéntico»).
        val isPeru = country.trim().ifEmpty { "PE" }.equals("PE", ignoreCase = true)
        return TenantLocale(
            country = country,
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
            identityTypes = if (isPeru) emptyList() else identityTypesOf(o["identity_types"]),
            invoiceFinalConsumerMax = if (isPeru) 0.0 else finalConsumerMaxOf(o["doc_kinds"]),
            taxRates = if (isPeru) emptyList() else taxRatesOf(o["tax_rates"]),
        ).sanitized()
    }

    /** `identity_types`: se descartan las filas sin código; si no es un arreglo, lista vacía. */
    private fun identityTypesOf(el: JsonElement?): List<IdentityTypeOption> =
        (el as? JsonArray).orEmpty().mapNotNull { item ->
            val o = item as? JsonObject ?: return@mapNotNull null
            val code = o["code"].str() ?: return@mapNotNull null
            IdentityTypeOption(
                code = code,
                key = o["key"].str().orEmpty(),
                label = o["label"].str() ?: code,
                minLength = (o["min"] as? JsonPrimitive)?.intOrNull ?: 0,
                maxLength = (o["max"] as? JsonPrimitive)?.intOrNull ?: 0,
                numeric = (o["numeric"] as? JsonPrimitive)?.booleanOrNull ?: false,
            )
        }

    /** `doc_kinds[kind=invoice].final_consumer_max` (EC 50); 0 si no viene. */
    private fun finalConsumerMaxOf(el: JsonElement?): Double =
        (el as? JsonArray).orEmpty()
            .mapNotNull { it as? JsonObject }
            .firstOrNull { it["kind"].str() == "invoice" }
            ?.let { (it["final_consumer_max"] as? JsonPrimitive)?.doubleOrNull }
            ?.takeIf { it.isFinite() && it > 0.0 }
            ?: 0.0

    /** `tax_rates` (solo Ecuador): se descartan las filas sin código o sin tarifa numérica. */
    private fun taxRatesOf(el: JsonElement?): List<TaxRateOption> =
        (el as? JsonArray).orEmpty().mapNotNull { item ->
            val o = item as? JsonObject ?: return@mapNotNull null
            val code = o["code"].str() ?: return@mapNotNull null
            val rate = (o["rate"] as? JsonPrimitive)?.doubleOrNull?.takeIf { it.isFinite() } ?: return@mapNotNull null
            TaxRateOption(
                code = code,
                label = o["label"].str() ?: code,
                rate = rate,
                isDefault = (o["default"] as? JsonPrimitive)?.booleanOrNull ?: false,
            )
        }

    private fun JsonElement?.str(): String? =
        (this as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }
}
