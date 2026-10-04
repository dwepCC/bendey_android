package com.bendey.restaurant.core.network.error

import org.junit.Test
import java.io.File

/**
 * Regenera `docs/ERROR_CATALOG_COPY.md` desde [ErrorCatalog] (fuente unica de verdad).
 * Solo escribe si BENDEY_WRITE_ERROR_COPY=1; sin la variable no hace nada.
 * `ErrorCatalogTest.copyDocumentMatchesCatalog` falla si el documento se desalinea del catalogo.
 */
class ErrorCopyDocGenerator {

    private fun http(status: Int, code: String? = null, flow: ErrorFlow = ErrorFlow.GENERIC) =
        ErrorCatalog.resolve(Failure(FailureKind.HTTP, status, code, null), flow)

    private fun acts(i: ErrorInfo) = i.actions.joinToString(" · ") { "[${it.label}]" }
    private fun cell(s: String) = s.replace("|", "\\|")

    private val flowNames = mapOf(
        ErrorFlow.GENERIC to "Genérico (lecturas, listas, ajustes)",
        ErrorFlow.SEND_COMANDA to "Enviar comanda",
        ErrorFlow.CHARGE to "Cobrar",
        ErrorFlow.OPEN_TABLE to "Abrir mesa",
        ErrorFlow.VOID_REFUND to "Anular / devolver",
        ErrorFlow.CASH to "Caja (abrir / cerrar / mover)",
        ErrorFlow.LOGIN_PIN to "Login por PIN",
        ErrorFlow.LOGIN_EMAIL to "Login por correo",
    )

    @Test fun write() {
        if (System.getenv("BENDEY_WRITE_ERROR_COPY") != "1") return
        val sb = StringBuilder()
        sb.appendLine("# Catálogo de errores accionables: textos y acciones (R5, oleada 2)")
        sb.appendLine()
        sb.appendLine("Fuente: `core/network/src/main/java/com/bendey/restaurant/core/network/error/ErrorCatalog.kt` (Android).")
        sb.appendLine("Tauri debe copiar estas cadenas TAL CUAL a `src/utils/errorCatalog.ts` (mismas claves, mismo texto, mismo orden).")
        sb.appendLine("Este archivo se regenera desde el catálogo (`BENDEY_WRITE_ERROR_COPY=1 ./gradlew :core:network:testDebugUnitTest --tests '*ErrorCopyDocGenerator*'`) y `ErrorCatalogTest.copyDocumentMatchesCatalog` falla si se desalinea.")
        sb.appendLine()
        sb.appendLine("Reglas (UX-REDESIGN §15.2-§15.4): tuteo, sin jerga; todo error de comanda, cobro, caja y anulación dice SI LA OPERACIÓN SE REALIZÓ O NO; todo error lleva al menos una acción; el código de soporte (`CODE · HTTP n · clase`) va en un detalle plegable con Copiar, nunca en el mensaje. Nunca se reintenta solo una operación no idempotente (cobrar, enviar comanda).")
        sb.appendLine()
        sb.appendLine("## Orden de resolución")
        sb.appendLine()
        sb.appendLine("1. `code` del backend (tabla de abajo; `SUMMARY_VOID_*` comparte una entrada).")
        sb.appendLine("2. Tipo de fallo de red: sin conexión (la petición NO salió) / timeout o corte (pudo ejecutarse) / TLS / respuesta ilegible / otro.")
        sb.appendLine("3. HTTP: 5xx → \"problema de nuestro lado\" (nunca \"conexión\"); 401 → sesión vencida (en login: PIN/credenciales); 402 → servicio suspendido; 403/404/409/400 → texto legible del servidor si lo hay, si no la reserva; 429 → espera.")
        sb.appendLine("4. Texto `error` del servidor SOLO si es legible: una línea, ≤ 220 caracteres, sin HTML, llaves ni nombres de excepción.")
        sb.appendLine("5. Reserva por flujo (tablas de abajo). Nunca \"Error\" a secas.")
        sb.appendLine()
        sb.appendLine("## Acciones (etiqueta del botón)")
        sb.appendLine()
        sb.appendLine("| Clave | Etiqueta |")
        sb.appendLine("|---|---|")
        for (a in ErrorAction.values()) sb.appendLine("| `${a.name}` | ${a.label} |")
        sb.appendLine()
        sb.appendLine("## Códigos del backend (code → título → mensaje → acciones)")
        sb.appendLine()
        sb.appendLine("| code | título | mensaje | acciones |")
        sb.appendLine("|---|---|---|---|")
        for (code in ErrorCatalog.knownCodes) {
            val i = http(400, code)
            sb.appendLine("| `$code` | ${cell(i.title)} | ${cell(i.message)} | ${acts(i)} |")
        }
        val sv = http(400, "SUMMARY_VOID_ANY")
        sb.appendLine("| `SUMMARY_VOID_*` | ${cell(sv.title)} | ${cell(sv.message)} | ${acts(sv)} |")
        sb.appendLine()
        sb.appendLine("`PIN_LOCKED`: si el servidor trae `espera N minuto(s)`, el mensaje pasa a \"Demasiados intentos. Espera N minutos o pídele al administrador que restablezca el PIN.\" (\"minuto\" si N = 1).")
        sb.appendLine()
        sb.appendLine("## Reserva por clase de fallo y flujo (code desconocido o ausente)")
        sb.appendLine()
        val kinds = listOf(
            FailureKind.NO_CONNECTION to "Sin conexión (la petición NO salió)",
            FailureKind.TIMEOUT to "Timeout o corte (pudo ejecutarse)",
        )
        for ((kind, label) in kinds) {
            sb.appendLine("### $label")
            sb.appendLine()
            sb.appendLine("| flujo | mensaje | acciones |")
            sb.appendLine("|---|---|---|")
            for (flow in ErrorFlow.values()) {
                val i = ErrorCatalog.resolve(Failure(kind), flow)
                sb.appendLine("| ${flowNames[flow]} (`$flow`) | ${cell(i.message)} | ${acts(i)} |")
            }
            sb.appendLine()
        }
        sb.appendLine("### 5xx (el servidor falló; NO es falta de red)")
        sb.appendLine()
        sb.appendLine("| flujo | mensaje | acciones |")
        sb.appendLine("|---|---|---|")
        for (flow in ErrorFlow.values()) {
            val i = http(500, flow = flow)
            sb.appendLine("| ${flowNames[flow]} (`$flow`) | ${cell(i.message)} | ${acts(i)} |")
        }
        sb.appendLine()
        sb.appendLine("### Respuesta ilegible (HTML de proxy en un 2xx, JSON roto)")
        sb.appendLine()
        sb.appendLine("| flujo | mensaje | acciones |")
        sb.appendLine("|---|---|---|")
        for (flow in listOf(ErrorFlow.CHARGE, ErrorFlow.SEND_COMANDA, ErrorFlow.GENERIC)) {
            val i = ErrorCatalog.resolve(Failure(FailureKind.UNREADABLE), flow)
            sb.appendLine("| ${flowNames[flow]} (`$flow`) | ${cell(i.message)} | ${acts(i)} |")
        }
        sb.appendLine()
        sb.appendLine("### Otros fallos")
        sb.appendLine()
        sb.appendLine("| caso | mensaje | acciones |")
        sb.appendLine("|---|---|---|")
        val others = listOf(
            "TLS / certificado" to ErrorCatalog.resolve(Failure(FailureKind.TLS)),
            "Otro fallo (cobrar, `CHARGE`)" to ErrorCatalog.resolve(Failure(FailureKind.OTHER), ErrorFlow.CHARGE),
            "Otro fallo (enviar comanda, `SEND_COMANDA`)" to ErrorCatalog.resolve(Failure(FailureKind.OTHER), ErrorFlow.SEND_COMANDA),
            "Otro fallo (resto de flujos)" to ErrorCatalog.resolve(Failure(FailureKind.OTHER)),
            "HTTP 401 sin code, fuera de login (sesión)" to http(401),
            "HTTP 401 sin code, login por PIN" to http(401, flow = ErrorFlow.LOGIN_PIN),
            "HTTP 401 sin code, login por correo" to http(401, flow = ErrorFlow.LOGIN_EMAIL),
            "HTTP 402 (suscripción)" to http(402),
            "HTTP 403 sin texto legible" to http(403),
            "HTTP 404 sin texto legible" to http(404),
            "HTTP 409 sin texto legible" to http(409),
            "HTTP 429" to http(429),
            "HTTP 4xx desconocido sin texto legible" to http(418),
        )
        for ((label, i) in others) sb.appendLine("| $label | ${cell(i.message)} | ${acts(i)} |")
        sb.appendLine()
        sb.appendLine("Textos fijos de login que no vienen del catálogo (validación local): \"Ingresa al menos 4 dígitos\", \"Escribe tu correo y tu contraseña.\", \"Tu usuario no tiene permisos operativos\".")
        sb.appendLine()
        val out = File("../../docs/ERROR_CATALOG_COPY.md")
        out.parentFile.mkdirs()
        out.writeText(sb.toString().replace("\r\n", "\n"))
    }
}
