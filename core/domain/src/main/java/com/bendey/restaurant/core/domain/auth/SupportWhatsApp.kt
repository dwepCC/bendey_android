package com.bendey.restaurant.core.domain.auth

import java.net.URLEncoder

/**
 * Enlace wa.me al soporte de Bendey. Gemelo de `buildSupportWhatsAppHref` de Tauri: el número se guarda
 * sin prefijo (ej. 914561918) y se completa con el 51 de Perú; vacío o inválido = null (sin botón).
 */
fun supportWhatsAppUrl(phone: String?, message: String = ""): String? {
    var digits = phone.orEmpty().filter { it.isDigit() }
    if (digits.isEmpty()) return null
    if (digits.startsWith("00")) digits = digits.drop(2)
    val wa = when {
        digits.startsWith("51") && digits.length >= 11 -> digits
        digits.startsWith("0") -> "51" + digits.drop(1)
        digits.length == 9 -> "51$digits"
        digits.length >= 10 -> digits
        else -> return null
    }
    val text = message.trim()
    if (text.isEmpty()) return "https://wa.me/$wa"
    return "https://wa.me/$wa?text=" + URLEncoder.encode(text, "UTF-8").replace("+", "%20")
}
