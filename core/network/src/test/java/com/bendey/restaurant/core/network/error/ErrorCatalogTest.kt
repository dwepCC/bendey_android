package com.bendey.restaurant.core.network.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ErrorCatalogTest {

    /** Los codigos de backend_go/docs/ERROR_CODES.md. */
    private val backendCodes = listOf(
        "AUTH_TOKEN_INVALID", "AUTH_TOKEN_MISSING", "AUTH_USER_NOT_FOUND", "BRANCH_REQUIRED",
        "CANCEL_REASON_REQUIRED", "CART_EMPTY", "CASH_ALREADY_OPEN", "CASH_ARQUEO_LOCKED", "CASH_NOT_OWNER",
        "CASH_OPENING_INVALID", "CASH_OPENING_NEGATIVE", "CASH_OPENING_TOO_LARGE", "CASH_SESSION_CLOSED",
        "CASH_SESSION_FOREIGN_BRANCH", "CASH_SESSION_NOT_FOUND", "CASH_SESSION_REQUIRED",
        "CASH_WAITER_NOT_ALLOWED", "COMANDA_ALREADY_BILLED", "COMANDA_ALREADY_DELIVERED", "COMANDA_CANCELLED",
        "COMANDA_NOTHING_TO_BILL", "COMANDA_NOT_BILLABLE", "COMANDA_NOT_FOUND", "COMANDA_STATUS_BACKWARDS",
        "COMANDA_WRONG_SESSION", "CREDIT_REQUIRES_CUSTOMER", "CUSTOMER_RUC_INVALID",
        "ELECTRONIC_BILLING_DISABLED", "INVOICE_REQUIRES_RUC", "LOGIN_PIN_AMBIGUOUS", "LOGIN_PIN_FORMAT",
        "LOGIN_PIN_INCORRECT", "LOGIN_PIN_RATE_LIMITED", "LOGIN_STATION_INVALID", "MOVEMENT_ALREADY_VOIDED",
        "MOVEMENT_AMOUNT_INVALID", "MOVEMENT_NOT_FOUND", "MOVEMENT_NOT_MANUAL", "MOVEMENT_NO_CHANGES",
        "MOVEMENT_REASON_REQUIRED", "MOVEMENT_SESSION_CLOSED", "MOVEMENT_TYPE_INVALID", "ORDER_ALREADY_BILLED",
        "ORDER_EMPTY", "ORDER_HAS_ITEMS", "ORDER_NOT_FOUND", "ORDER_NOT_OPEN", "PAYMENT_BELOW_TOTAL",
        "PAYMENT_METHOD_NO_ACCOUNT", "PAYMENT_REQUIRED", "PIN_DUPLICATE", "PIN_FORMAT_INVALID", "PIN_INCORRECT",
        "PIN_LOCKED", "PIN_NOT_CONFIGURED", "PIN_REQUIRED", "PIN_UNAVAILABLE", "REFUND_ALREADY_DONE",
        "REFUND_CONFIRMATION_REQUIRED", "REFUND_FAILED", "REFUND_NO_PAYMENT", "REFUND_USER_REQUIRED",
        "SALE_ALREADY_CANCELLED", "SALE_ALREADY_ELECTRONIC", "SALE_EMPTY", "SALE_NOT_FOUND",
        "SALE_VOID_NEEDS_CREDIT_NOTE", "SERIES_DUPLICATE", "SERIES_INACTIVE", "SERIES_INVALID",
        "SERIES_NOT_FOUND", "SERIES_REQUIRED", "SERIES_WRONG_BRANCH", "SESSION_CLOSED", "SESSION_MOVED",
        "SESSION_NOT_FOUND", "SESSION_NOT_OPEN", "TABLE_CLOSE_PENDING_BALANCE", "TABLE_CLOSE_PENDING_COMANDAS",
        "TABLE_NOT_AVAILABLE", "TABLE_NOT_FOUND", "TABLE_OCCUPIED",
    )

    /** Codigos que ya existian antes de R5 y que el cliente tambien maneja. */
    private val preexisting = listOf(
        "BRANCH_FORBIDDEN", "SESSION_UPDATED", "TOKEN_TENANT_INVALID", "TENANT_ISOLATION_VIOLATION",
        "SUBSCRIPTION_REQUIRED", "TENANT_BLOCKED", "PAYMENT_BLOCKED", "DOCUMENT_QUOTA_EXCEEDED",
        "SUMMARY_ALREADY_EXISTS", "menu_disabled", "takeaway_disabled", "delivery_disabled", "menu_digital_disabled",
    )

    private fun http(status: Int, code: String? = null, msg: String? = null, flow: ErrorFlow = ErrorFlow.GENERIC) =
        ErrorCatalog.resolve(Failure(FailureKind.HTTP, status, code, msg), flow)

    private fun allInfos(): List<Pair<String, ErrorInfo>> {
        val out = mutableListOf<Pair<String, ErrorInfo>>()
        for (code in ErrorCatalog.knownCodes) out += code to http(400, code)
        out += "SUMMARY_VOID_X" to http(400, "SUMMARY_VOID_NOT_ACCEPTED")
        for (flow in ErrorFlow.values()) {
            for (kind in listOf(FailureKind.NO_CONNECTION, FailureKind.TIMEOUT, FailureKind.TLS, FailureKind.UNREADABLE, FailureKind.OTHER)) {
                out += "$kind/$flow" to ErrorCatalog.resolve(Failure(kind), flow)
            }
            for (status in listOf(400, 401, 402, 403, 404, 408, 409, 418, 429, 500, 502, 503)) {
                out += "$status/$flow" to http(status, flow = flow)
            }
        }
        return out
    }

    @Test fun everyBackendCodeHasMessageAndAction() {
        for (code in backendCodes + preexisting) {
            assertTrue("falta $code en el catalogo", ErrorCatalog.has(code))
            val info = http(400, code)
            assertTrue("$code sin mensaje", info.message.isNotBlank())
            assertTrue("$code sin titulo", info.title.isNotBlank())
            assertTrue("$code sin accion", info.actions.isNotEmpty())
        }
    }

    @Test fun summaryVoidFamilyIsCovered() {
        assertTrue(ErrorCatalog.has("SUMMARY_VOID_ANYTHING"))
        assertTrue(http(400, "SUMMARY_VOID_NOT_ACCEPTED").message.contains("SUNAT"))
    }

    @Test fun fallbackPerFailureClass() {
        assertTrue(ErrorCatalog.resolve(Failure(FailureKind.NO_CONNECTION)).message.contains("conexión"))
        assertTrue(ErrorCatalog.resolve(Failure(FailureKind.TIMEOUT)).message.contains("tardó"))
        assertTrue(ErrorCatalog.resolve(Failure(FailureKind.UNREADABLE)).message.contains("No pudimos leer"))
        assertTrue(http(404).message.contains("No encontramos"))
        assertTrue(http(409).message.contains("Otra persona"))
        assertTrue(http(429).message.contains("Espera"))
        assertTrue(http(403).message.contains("permiso"))
        assertTrue(http(418).message.contains("No se pudo completar"))
    }

    @Test fun sessionExpiredIsNotWrongCredentials() {
        for (code in listOf("AUTH_TOKEN_INVALID", "AUTH_TOKEN_MISSING")) {
            val m = http(401, code, flow = ErrorFlow.GENERIC).message
            assertEquals("Tu sesión venció. Vuelve a iniciar sesión para continuar.", m)
        }
        assertTrue(http(401).message.startsWith("Tu sesión venció"))
        assertFalse(http(401).message.contains("redenciales"))
        assertTrue(http(401, flow = ErrorFlow.LOGIN_PIN).message.contains("PIN incorrecto"))
        assertTrue(http(401, flow = ErrorFlow.LOGIN_EMAIL).message.contains("Correo o contraseña"))
        // un token vencido NUNCA se disfraza de credenciales, ni en login
        assertTrue(http(401, "AUTH_TOKEN_INVALID", flow = ErrorFlow.LOGIN_EMAIL).message.startsWith("Tu sesión venció"))
    }

    @Test fun serverErrorNeverSaysConnection() {
        for (status in listOf(500, 502, 503, 504)) for (flow in ErrorFlow.values()) {
            val m = http(status, flow = flow).message
            assertFalse("5xx dice conexion ($flow): $m", m.lowercase().contains("conexión"))
            assertTrue(m.contains("de nuestro lado"))
        }
        assertTrue(http(500).message.contains("No se perdió nada de lo que ya cobraste"))
    }

    @Test fun moneyFlowsSayWhetherItHappened() {
        val noConn = ErrorCatalog.resolve(Failure(FailureKind.NO_CONNECTION), ErrorFlow.SEND_COMANDA).message
        assertTrue(noConn.contains("No salió a cocina"))
        val timeoutSend = ErrorCatalog.resolve(Failure(FailureKind.TIMEOUT), ErrorFlow.SEND_COMANDA).message
        assertTrue(timeoutSend.contains("Puede que sí haya llegado a cocina"))
        val timeoutCharge = ErrorCatalog.resolve(Failure(FailureKind.TIMEOUT), ErrorFlow.CHARGE).message
        assertTrue(timeoutCharge.contains("revisa en Ventas si ya se registró"))
        assertTrue(http(500, flow = ErrorFlow.CHARGE).message.contains("revisa en Ventas"))
        assertTrue(
            ErrorCatalog.resolve(Failure(FailureKind.NO_CONNECTION), ErrorFlow.CASH).message
                .contains("Tus ventas siguen guardadas"),
        )
        assertTrue(
            ErrorCatalog.resolve(Failure(FailureKind.NO_CONNECTION), ErrorFlow.VOID_REFUND).message
                .contains("No se anuló ni se devolvió nada"),
        )
    }

    @Test fun uncertainFailuresOfNonIdempotentFlowsOfferVerificationFirst() {
        val charge = ErrorCatalog.resolve(Failure(FailureKind.TIMEOUT), ErrorFlow.CHARGE)
        assertEquals(ErrorAction.GO_SALES, charge.actions.first())
        val send = ErrorCatalog.resolve(Failure(FailureKind.TIMEOUT), ErrorFlow.SEND_COMANDA)
        assertEquals(ErrorAction.REFRESH, send.actions.first())
    }

    @Test fun pinLockedUsesMinutesFromServer() {
        val m = http(429, "PIN_LOCKED", "demasiados intentos fallidos: espera 3 minuto(s) antes de volver a intentar").message
        assertTrue(m, m.contains("Espera 3 minutos"))
        val one = http(429, "PIN_LOCKED", "demasiados intentos fallidos: espera 1 minuto(s)").message
        assertTrue(one, one.contains("Espera 1 minuto "))
        assertTrue(http(429, "PIN_LOCKED").message.contains("Espera unos minutos"))
    }

    @Test fun unknownCodeFallsBackToReadableServerText() {
        assertEquals("El nombre ya existe", http(400, "ALGO_NUEVO", "El nombre ya existe").message)
    }

    @Test fun unreadableServerTextIsDiscarded() {
        assertNull(ErrorCatalog.readableServerMessage("<html><body>502 Bad Gateway</body></html>"))
        assertNull(ErrorCatalog.readableServerMessage("java.net.UnknownHostException: x"))
        assertNull(ErrorCatalog.readableServerMessage("{\"error\":\"x\"}"))
        assertNull(ErrorCatalog.readableServerMessage("a\nb"))
        assertNull(ErrorCatalog.readableServerMessage("x".repeat(300)))
        assertNotNull(ErrorCatalog.readableServerMessage("La mesa no existe"))
        assertFalse(http(400, null, "<html>oops</html>").message.contains("<"))
    }

    @Test fun noMessageHasJargonUstedOrStackTrace() {
        val banned = listOf(
            "usted", "indique", "aperture", "exception", "stacktrace", "java.", "kotlin.", "http ", "err_",
            "<html", "null", "token", "timeout", "socket", "ssl", "backend",
        )
        val infos = allInfos()
        assertTrue(infos.size > 200)
        for ((key, info) in infos) {
            val text = (info.title + " " + info.message).lowercase()
            for (b in banned) assertFalse("$key contiene '$b': ${info.message}", text.contains(b))
            assertFalse("$key es Error a secas", info.message.trim().equals("error", ignoreCase = true))
            assertTrue("$key sin accion", info.actions.isNotEmpty())
            assertTrue("$key sin punto final: ${info.message}", info.message.trimEnd().endsWith("."))
        }
    }

    @Test fun supportCodeKeepsTechnicalDetailOutOfTheMessage() {
        val info = http(502)
        assertEquals("HTTP 502", info.supportCode)
        assertFalse(info.message.contains("502"))
        assertEquals("PIN_INCORRECT · HTTP 400", http(400, "PIN_INCORRECT").supportCode)
    }

    /** D1: los errores de Delivery (D1 + tarifa D2.0), con el mismo texto que `errorCatalog.ts` de Tauri y la acción correcta. */
    @Test fun erroresDeDeliveryTienenTextoYAccion() {
        val esperado = mapOf(
            "DELIVERY_CANCEL_REASON_REQUIRED" to Pair("Escribe el motivo de la cancelación (mínimo 3 letras) y vuelve a intentar.", ErrorAction.BACK_TO_TRY),
            "DRIVER_INACTIVE" to Pair("Ese repartidor ya no está activo. Elige a otro.", ErrorAction.BACK_TO_TRY),
            "DRIVER_UNAVAILABLE" to Pair("Ese repartidor está marcado como no disponible. Elige a otro o espera a que se conecte.", ErrorAction.BACK_TO_TRY),
            "SESSION_ALREADY_CLOSED" to Pair("Este pedido ya fue cobrado o cerrado y no se puede cancelar. Actualiza para ver el estado.", ErrorAction.REFRESH),
            "SESSION_NOT_ASSIGNABLE" to Pair("Este pedido ya no se puede asignar: está cerrado, entregado o cancelado. Actualiza para ver el estado.", ErrorAction.REFRESH),
            "SESSION_NOT_DELIVERY" to Pair("Este pedido no es de delivery, así que no se gestiona desde Delivery. Actualiza para ver el estado.", ErrorAction.REFRESH),
            "USE_DELIVERY_ASSIGNMENT" to Pair("Este pedido ya tiene un repartidor. Cambia su estado desde Delivery.", ErrorAction.REFRESH),
            // D2.0: tarifa de delivery, al final del bloque (textos exactos de D2_COMMON §8).
            "DELIVERY_FEE_FORBIDDEN" to Pair("No tienes permiso para cambiar la tarifa de delivery.", ErrorAction.CONTACT_ADMIN),
            "DELIVERY_FEE_INVALID" to Pair("Revisa la tarifa: usa un monto entre S/ 0 y S/ 999.99.", ErrorAction.BACK_TO_TRY),
            "DELIVERY_FEE_NOT_EDITABLE" to Pair("Este pedido ya no admite cambios en la tarifa de delivery.", ErrorAction.REFRESH),
        )
        for ((code, want) in esperado) {
            val info = http(409, code)
            assertEquals(code, want.first, info.message)
            assertEquals(code, listOf(want.second), info.actions)
            assertTrue(code, code in ErrorCatalog.knownCodes)
        }
        // Mismo orden que el bloque final de Tauri: DELIVERY_CANCEL.., DRIVER_*, SESSION_*, USE_* y, al final, DELIVERY_FEE_*.
        val tail = ErrorCatalog.knownCodes.toList().takeLast(esperado.size)
        assertEquals(esperado.keys.toList(), tail)
    }

    /** Las cadenas del catalogo y las de docs/ERROR_CATALOG_COPY.md (que copia Tauri) no pueden divergir. */
    @Test fun copyDocumentMatchesCatalog() {
        val doc = listOf("../../docs/ERROR_CATALOG_COPY.md", "docs/ERROR_CATALOG_COPY.md")
            .map { File(it) }.firstOrNull { it.exists() }
        assertNotNull("no se encontro docs/ERROR_CATALOG_COPY.md", doc)
        val text = doc!!.readText()
        for (code in ErrorCatalog.knownCodes) {
            val info = http(400, code)
            assertTrue("el documento no trae el codigo $code", text.contains("`$code`"))
            assertTrue("el documento no trae el texto de $code", text.contains(info.message))
        }
        for (flow in ErrorFlow.values()) {
            for (kind in listOf(FailureKind.NO_CONNECTION, FailureKind.TIMEOUT)) {
                val m = ErrorCatalog.resolve(Failure(kind), flow).message
                assertTrue("el documento no trae el fallback $kind/$flow", text.contains(m))
            }
            assertTrue(text.contains(http(500, flow = flow).message))
        }
    }
}
