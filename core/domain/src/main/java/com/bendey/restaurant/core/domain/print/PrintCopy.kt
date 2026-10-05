package com.bendey.restaurant.core.domain.print

/**
 * Textos y estados de la impresión (R10.4), con CLAVES ESTABLES `print.*` IDÉNTICAS a las de Tauri
 * (`front_tenant_restaurant_tauri/src/content/printCopy.ts`). Cambiar un texto allá = cambiarlo acá;
 * `PrintCopyParityTest` compara ambos archivos cuando el repo de Tauri está al lado.
 *
 * Nunca texto técnico (excepciones, IP:puerto, "failed to ..."). Una impresión fallida NUNCA se marca
 * como impresa: solo [PrintStatus.OK] cuenta.
 */
object PrintCopy {
    const val OK = "print.ok"
    const val FAILED = "print.failed"
    const val NOT_CONFIGURED = "print.not_configured"
    const val SERVER_UNREACHABLE = "print.server_unreachable"
    const val SERVER_DOWN = "print.server_down"
    const val COMANDA_SENT_NOT_PRINTED = "print.comanda_sent_not_printed"

    /** Misma tabla (claves y textos) que `PRINT_COPY` de Tauri. */
    val TEXTS: Map<String, String> = linkedMapOf(
        OK to "Impreso.",
        FAILED to "No se pudo imprimir: {motivo}. Revisa la impresora y vuelve a intentar.",
        NOT_CONFIGURED to "Esta impresora no está configurada. Ve a Impresoras para elegirla.",
        SERVER_UNREACHABLE to
            "No encontramos el servidor de impresión de tu red. Revisa que la PC de caja esté encendida y en el mismo Wi-Fi.",
        SERVER_DOWN to "El servidor de impresión no respondió. Reinicia la app de caja e intenta de nuevo.",
        COMANDA_SENT_NOT_PRINTED to "Comanda #{n} enviada, pero no se imprimió.",
    )

    fun text(key: String, params: Map<String, Any> = emptyMap()): String {
        var text = TEXTS.getValue(key)
        for ((k, v) in params) text = text.replace("{$k}", v.toString())
        return text
    }
}

enum class PrintStatus { OK, FAILED, NOT_CONFIGURED, SERVER_UNREACHABLE, SERVER_DOWN }

data class PrintOutcome(val status: PrintStatus, val reason: String? = null) {
    /** Solo OK cuenta como impreso. */
    val isPrinted: Boolean get() = status == PrintStatus.OK

    /** El texto que ve el usuario. */
    val message: String get() = printMessage(this)

    companion object {
        val Ok = PrintOutcome(PrintStatus.OK)
        val NotConfigured = PrintOutcome(PrintStatus.NOT_CONFIGURED)
        val ServerUnreachable = PrintOutcome(PrintStatus.SERVER_UNREACHABLE)
        val ServerDown = PrintOutcome(PrintStatus.SERVER_DOWN)
        fun failed(reason: String?) = PrintOutcome(PrintStatus.FAILED, reason)
    }
}

/** Motivo por defecto cuando no hay uno entendible. */
const val PRINT_REASON_FALLBACK = "la impresora no respondió"

fun printMessage(outcome: PrintOutcome): String = when (outcome.status) {
    PrintStatus.OK -> PrintCopy.text(PrintCopy.OK)
    PrintStatus.NOT_CONFIGURED -> PrintCopy.text(PrintCopy.NOT_CONFIGURED)
    PrintStatus.SERVER_UNREACHABLE -> PrintCopy.text(PrintCopy.SERVER_UNREACHABLE)
    PrintStatus.SERVER_DOWN -> PrintCopy.text(PrintCopy.SERVER_DOWN)
    PrintStatus.FAILED -> PrintCopy.text(PrintCopy.FAILED, mapOf("motivo" to sanitizePrintReason(outcome.reason)))
}

/** "Comanda #n enviada, pero no se imprimió." */
fun comandaSentNotPrinted(orderNumber: Int): String =
    PrintCopy.text(PrintCopy.COMANDA_SENT_NOT_PRINTED, mapOf("n" to orderNumber))

/**
 * Reglas de [sanitizePrintReason], en el MISMO orden y con los MISMOS patrones que Tauri
 * (los patrones se comparan textualmente en el test de paridad).
 */
internal val PRINT_REASON_RULES: List<Pair<String, String>> = listOf(
    "refused|rechaz|10061|actively refused" to "la impresora rechazó la conexión",
    "time.?out|timed out|10060|no respond" to PRINT_REASON_FALLBACK,
    "unreachable|no route|10051|10065|host is down|dns|resolve|network is" to "no encontramos la impresora en la red",
    "offline|fuera de línea|fuera de linea" to "la impresora está apagada o sin conexión",
    "paper|papel" to "a la impresora se le acabó el papel o la tapa está abierta",
    "access.?denied|acceso denegado|permission" to "no hay permiso para usar la impresora",
    "not found|no encontrad|no existe|cannot find|1801|invalid printer" to "no encontramos la impresora",
    "disconnect|desconect|broken pipe|connection reset|reset by peer|10054" to "se perdió la conexión con la impresora",
)

private val PRINT_REASON_REGEXES: List<Pair<Regex, String>> =
    PRINT_REASON_RULES.map { (p, r) -> Regex(p, RegexOption.IGNORE_CASE) to r }

/** Rastros típicos de un mensaje de programador (patrón 1: con `i`; patrón 2: sin `i`; patrón 3: símbolos). */
internal const val TECHNICAL_PATTERN_1 =
    """(os error|errno|exception|stack|traceback|panic|\brust\b|tauri|invoke|cargo|undefined|\bnull\b|object Object|\bat \w+\.|0x[0-9a-f]+|\bfailed to\b)"""
internal const val TECHNICAL_PATTERN_2 = """\b(ERR_[A-Z_]+|E[A-Z]{4,})\b"""
internal const val TECHNICAL_PATTERN_3 = """[{}[\]<>]"""

private val TECHNICAL_1 = Regex(TECHNICAL_PATTERN_1, RegexOption.IGNORE_CASE)
private val TECHNICAL_2 = Regex(TECHNICAL_PATTERN_2)

// En Java `[` dentro de una clase abre una clase anidada: se escapa (equivale al `[{}[\]<>]` de JS).
private val TECHNICAL_3 = Regex("""[{}\[\]<>]""")

fun looksTechnical(text: String): Boolean =
    TECHNICAL_1.containsMatchIn(text) || TECHNICAL_2.containsMatchIn(text) || TECHNICAL_3.containsMatchIn(text)

private val TRAILING = Regex("""[.\s]+$""")

/**
 * Convierte lo que devuelva el sistema (errores de Android, de la red, del servidor) en un motivo que
 * entiende quien está en el local. Lo técnico se traduce o se descarta; el texto ya claro en español se
 * conserva. Sale en minúscula y sin punto final, listo para `print.failed`.
 */
fun sanitizePrintReason(raw: String?): String {
    val text = (raw ?: "").trim()
    if (text.isEmpty()) return PRINT_REASON_FALLBACK
    val t = text.lowercase()
    for ((re, reason) in PRINT_REASON_REGEXES) if (re.containsMatchIn(t)) return reason
    if (looksTechnical(text)) return PRINT_REASON_FALLBACK
    return text.replace(TRAILING, "")
        .replaceFirstChar { it.lowercase() }
        .take(120)
}

private val NOT_CONFIGURED_TEXT = Regex(
    "no hay impresora|no configurada|sin configurar|not configured|completa la configuraci|configura la impresora|selecciona una impresora|indica la ip",
    RegexOption.IGNORE_CASE,
)

/** Mismo criterio que `isNotConfiguredText` de Tauri. */
fun isNotConfiguredText(text: String?): Boolean = NOT_CONFIGURED_TEXT.containsMatchIn(text ?: "")

private val IPV4 = Regex("""\d{1,3}(\.\d{1,3}){3}""")
private val HOST_PORT = Regex("""(\bport\s*\d+|:\d{2,5}\b)""", RegexOption.IGNORE_CASE)
private val PLATFORM_PREFIX = Regex(
    """^(error tcp|error al imprimir bt|no se pudo conectar|error de conexión con el servidor)\s*:?\s*""",
    RegexOption.IGNORE_CASE,
)

/**
 * Motivo para un error que viene del transporte local (Bluetooth/USB/red) de Android: quita los
 * prefijos propios y, si lo que queda trae una IP o un puerto crudos, lo reemplaza por algo entendible.
 */
fun platformPrintReason(raw: String?): String {
    val cleaned = (raw ?: "").trim().replace(PLATFORM_PREFIX, "")
    val reason = sanitizePrintReason(cleaned)
    return if (IPV4.containsMatchIn(reason) || HOST_PORT.containsMatchIn(reason)) {
        "no encontramos la impresora en la red"
    } else {
        reason
    }
}

/** Resultado a partir de un error del transporte local de impresión. */
fun printOutcomeFromPlatformError(message: String?): PrintOutcome = when {
    isNotConfiguredText(message) -> PrintOutcome.NotConfigured
    // El transporte de red avisa así que no hay dirección de impresora cargada.
    (message ?: "").trim().startsWith("Host TCP vacío", ignoreCase = true) -> PrintOutcome.NotConfigured
    else -> PrintOutcome.failed(platformPrintReason(message))
}

/** Códigos de error del servidor de impresión (ver `RemotePrintResult.Error.code`). */
object RemotePrintCode {
    const val CONNECTION = "connection_error"
    const val TIMEOUT = "timeout"
    const val SERVER_STOPPED = "server_stopped"
    const val QUEUE_UNAVAILABLE = "queue_unavailable"
    const val HTTP = "http_error"
    const val NOT_CONFIGURED = "not_configured"
}

/**
 * Resultado a partir de un error del servidor de impresión (la PC de caja):
 * - no se llegó al servidor -> inalcanzable; llegó pero no atiende (detenido, sin cola, sin respuesta) -> caído;
 * - si el servidor respondió con un motivo propio ("Impresora de comandas no configurada", "impresora
 *   sin conexión"...) se traduce: sin configurar es un estado propio, lo demás es FAILED con motivo limpio.
 */
fun printOutcomeFromRemoteError(code: String, message: String?): PrintOutcome = when (code) {
    RemotePrintCode.CONNECTION -> PrintOutcome.ServerUnreachable
    RemotePrintCode.TIMEOUT, RemotePrintCode.SERVER_STOPPED, RemotePrintCode.QUEUE_UNAVAILABLE -> PrintOutcome.ServerDown
    RemotePrintCode.NOT_CONFIGURED -> PrintOutcome.NotConfigured
    else -> if (isNotConfiguredText(message)) PrintOutcome.NotConfigured else PrintOutcome.failed(platformPrintReason(message))
}

/**
 * Junta los resultados de varios tickets de una misma comanda (uno por área de preparación, o varias
 * rondas): solo es OK si TODOS salieron; si no, se muestra el primer fallo. Nunca se declara impresa una
 * comanda a medias. Lista vacía = nada que imprimir = OK.
 */
fun combinePrintOutcomes(outcomes: List<PrintOutcome>): PrintOutcome =
    outcomes.firstOrNull { !it.isPrinted } ?: PrintOutcome.Ok
