package com.bendey.restaurant.core.domain.delivery

import com.bendey.restaurant.core.domain.model.AppResult
import java.util.Locale

/** Zona de reparto: un distrito (código ubigeo de 6 dígitos) con su tarifa, mínimo y hora prometida. */
data class DeliveryZone(
    val id: Int,
    val ubigeoCode: String,
    val name: String,
    /** 0 = envío gratis. */
    val fee: Double = 0.0,
    /** 0 = usa el pedido mínimo general. */
    val minOrder: Double = 0.0,
    /** 0 = usa el tiempo prometido general. */
    val etaMinutes: Int = 0,
    val active: Boolean = true,
)

data class DeliveryZoneInput(
    val ubigeoCode: String,
    val name: String,
    val fee: Double,
    val minOrder: Double,
    val etaMinutes: Int,
    val active: Boolean,
)

/** Lista de zonas + el interruptor «Repartir solo en estas zonas». */
data class DeliveryZonesData(val zones: List<DeliveryZone>, val restrictToZones: Boolean)

interface DeliveryZonesRepository {
    suspend fun zones(): AppResult<DeliveryZonesData>

    /** Crea o actualiza la zona de ese distrito (el código es único). */
    suspend fun saveZone(input: DeliveryZoneInput): AppResult<DeliveryZone>
    suspend fun deleteZone(id: Int): AppResult<Unit>
}

/** Validación local de una zona (gemela de la del servidor) y textos de la tarjeta. */
object DeliveryZoneRules {
    const val MAX_AMOUNT = 999.99
    const val MAX_ETA = 240
    const val MAX_NAME = 150

    private val amountRegex = Regex("""\d{1,3}(\.\d{1,2})?""")

    fun isValidCode(code: String): Boolean = code.length == 6 && code.all { it.isDigit() }

    /** Monto válido (0 a 999.99, hasta 2 decimales; acepta coma; vacío = 0) o null. */
    fun parseAmount(text: String): Double? {
        val t = text.trim().replace(',', '.')
        if (t.isEmpty()) return 0.0
        if (!amountRegex.matches(t)) return null
        return t.toDoubleOrNull()?.takeIf { it in 0.0..MAX_AMOUNT }
    }

    /** Minutos válidos (entero 0 a 240; vacío = 0) o null. */
    fun parseEta(text: String): Int? {
        val t = text.trim()
        if (t.isEmpty()) return 0
        return t.toIntOrNull()?.takeIf { it in 0..MAX_ETA }
    }

    fun formatAmount(v: Double): String = if (v <= 0.0) "0" else String.format(Locale.US, "%.2f", v)

    /** Mensaje de la validación del formulario, o null si está bien. */
    fun validate(code: String, name: String, feeText: String, minOrderText: String, etaText: String): String? = when {
        !isValidCode(code) -> "Elige el distrito (región, provincia y distrito)."
        name.trim().isEmpty() -> "Escribe el nombre de la zona."
        parseAmount(feeText) == null -> "La tarifa debe estar entre S/ 0 y S/ 999.99, con hasta 2 decimales."
        parseAmount(minOrderText) == null -> "El pedido mínimo debe estar entre S/ 0 y S/ 999.99, con hasta 2 decimales."
        parseEta(etaText) == null -> "Los minutos deben ser un número entero entre 0 y $MAX_ETA."
        else -> null
    }

    /** Resumen de una zona para la lista. */
    fun describe(z: DeliveryZone): String {
        val fee = if (z.fee > 0) "Envío S/ ${formatAmount(z.fee)}" else "Envío gratis"
        val min = if (z.minOrder > 0) "mínimo S/ ${formatAmount(z.minOrder)}" else "mínimo general"
        val eta = if (z.etaMinutes > 0) "${z.etaMinutes} min" else "tiempo general"
        return "$fee · $min · $eta"
    }
}

object DeliveryZoneCopy {
    const val SECTION_TITLE = "Zonas de reparto"
    const val RESTRICT_LABEL = "Repartir solo en estas zonas"
    const val RESTRICT_HELP = "Fuera de estas zonas el cliente no podrá pedir delivery; sin distrito en su dirección se le pedirá elegirlo."
    const val RESTRICT_WARNING = "No tienes ninguna zona activa: si enciendes esto, nadie podrá pedir delivery."
    const val HELP = "La tarifa de la zona reemplaza a la tarifa única. Si no hay zona para el distrito del cliente se usa la tarifa única (salvo que repartas solo en tus zonas)."
    const val EMPTY = "Aún no tienes zonas de reparto."
    const val FEE_LABEL = "Tarifa de envío (S/; 0 = envío gratis)"
    const val MIN_LABEL = "Pedido mínimo (S/; 0 = usa el general)"
    const val ETA_LABEL = "Minutos prometidos (0 = usa el general)"
    const val READ_ONLY = "Solo el administrador puede cambiar las zonas de reparto."
}

/** ¿Encender «solo en estas zonas» deja sin delivery a todos? Sí si no hay ninguna zona activa. */
fun restrictWouldBlockAll(zones: List<DeliveryZone>): Boolean = zones.none { it.active }
