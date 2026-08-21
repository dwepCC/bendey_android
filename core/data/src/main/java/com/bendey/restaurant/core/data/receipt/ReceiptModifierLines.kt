package com.bendey.restaurant.core.data.receipt

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * EL COMPROBANTE TIENE QUE DECIR QUÉ SE VENDIÓ.
 *
 * Una boleta describió dos líneas distintas —un «Duo 6 Nuggets + 6 Alitas + Papa» y unas «6 Alitas
 * Acevichadas con Papas»— como «6 Alitas» las dos, porque el nombre comercial vive en la
 * presentación elegida y no en la descripción del producto.
 *
 * La presentación va sin su precio: en el ticket ya está la columna de importe al lado y repetirlo
 * confunde. Los extras sí lo conservan, porque explican por qué el precio subió.
 *
 * Paridad con `formatReceiptModifierLines` del lado web: los dos imprimen los mismos tickets.
 */
object ReceiptModifierLines {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun of(modifiersJson: String?, money: (Double) -> String): List<String> {
        val raw = modifiersJson?.trim().orEmpty()
        if (raw.isEmpty()) return emptyList()
        val elementos = try {
            json.parseToJsonElement(raw) as? JsonArray ?: return emptyList()
        } catch (_: Exception) {
            // Un JSON ilegible no puede impedir imprimir: se pierde el detalle, no el ticket.
            return emptyList()
        }

        val out = mutableListOf<String>()
        for (el in elementos) {
            val obj = el as? JsonObject ?: continue
            // `name` es el alias legacy de retail; las ventas nuevas usan `option_name`.
            val nombre = (obj["option_name"] ?: obj["name"])?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            if (nombre.isEmpty()) continue
            val tipo = (obj["type"] ?: obj["group_type"])?.jsonPrimitive?.contentOrNull.orEmpty()
            val cantidad = obj["quantity"]?.jsonPrimitive?.doubleOrNull ?: 1.0
            val etiqueta = if (cantidad > 1) "$nombre x${formatQty(cantidad)}" else nombre
            if (tipo == "variant") {
                out.add(etiqueta)
                continue
            }
            val extra = obj["extra_price"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            out.add(if (extra > 0) "+ $etiqueta (+${money(extra * if (cantidad > 1) cantidad else 1.0)})" else "+ $etiqueta")
        }
        return out
    }

    private fun formatQty(q: Double): String =
        if (q == q.toLong().toDouble()) q.toLong().toString() else q.toString()
}
