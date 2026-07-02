package com.bendey.restaurant.core.data.printer.printserver

/**
 * Normaliza IP/host pegado desde navegador o QR.
 * Acepta: `192.168.1.20`, `192.168.1.20:19280`, `http://192.168.1.20:19280/v1/health`
 */
data class ParsedPrintServerEndpoint(
    val host: String,
    val port: Int? = null,
)

fun parsePrintServerEndpoint(raw: String): ParsedPrintServerEndpoint? {
    var s = raw.trim()
    if (s.isBlank()) return null

    s = s.removePrefix("http://").removePrefix("https://")
    s = s.substringBefore('/').substringBefore('?').trim()
    if (s.isBlank()) return null

    val lastColon = s.lastIndexOf(':')
    if (lastColon > 0 && s.lastIndexOf('.') < lastColon) {
        val host = s.substring(0, lastColon).trim()
        val port = s.substring(lastColon + 1).toIntOrNull()
        if (host.isNotBlank()) return ParsedPrintServerEndpoint(host, port)
    }

    return ParsedPrintServerEndpoint(s, null)
}

fun PrintServerSelection.withManualEndpoint(raw: String): PrintServerSelection? {
    val parsed = parsePrintServerEndpoint(raw) ?: return if (isReady()) this else null
    return copy(
        host = parsed.host,
        manualHost = parsed.host,
        port = parsed.port ?: port,
    )
}

fun manualPrintServerSelection(raw: String): PrintServerSelection? {
    val parsed = parsePrintServerEndpoint(raw) ?: return null
    return PrintServerSelection(
        serverId = "manual-${parsed.host}",
        displayName = "Manual (${parsed.host})",
        host = parsed.host,
        manualHost = parsed.host,
        port = parsed.port ?: DEFAULT_PRINT_SERVER_PORT,
    )
}
