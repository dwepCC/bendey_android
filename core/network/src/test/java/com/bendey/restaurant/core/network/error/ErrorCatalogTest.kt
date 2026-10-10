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

    /** D1: los errores de Delivery (D1 + tarifa D2.0 + contra entrega D2b), con el mismo texto que `errorCatalog.ts` de Tauri y la acción correcta. */
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
            // D2b: efectivo contra entrega (textos exactos de D2B_COMMON §8).
            "CASH_TENDERED_TOO_LOW" to Pair("El monto con el que pagas debe cubrir el total del pedido.", ErrorAction.BACK_TO_TRY),
            "COLLECTION_REQUIRED" to Pair("Primero marca «Cobrado» antes de marcar la entrega.", ErrorAction.REFRESH),
            "COLLECT_NOT_APPLICABLE" to Pair("Este pedido no es de pago contra entrega.", ErrorAction.REFRESH),
            "COLLECT_STATUS_INVALID" to Pair("Solo puedes cobrar cuando ya recogiste el pedido.", ErrorAction.REFRESH),
            "PAYMENT_MODE_NOT_AVAILABLE" to Pair("El pago contra entrega no está disponible en este restaurante.", ErrorAction.CONTACT_ADMIN),
            "PAYMENT_NOT_EDITABLE" to Pair("Este pedido ya no admite cambios en el pago.", ErrorAction.REFRESH),
            // Rendición D2c, pausa/cancelación del marketplace y vendedor (mismo orden que Tauri).
            "SELLER_INVALID" to Pair("El vendedor elegido ya no está disponible. Elige otro.", ErrorAction.BACK_TO_TRY),
            "SETTLEMENT_INVALID_PAYMENTS" to Pair("Algún pedido ya no se puede rendir: ya se rindió o cambió. Actualiza la lista.", ErrorAction.REFRESH),
            "SETTLEMENT_MIXED_COLLECTORS" to Pair("Una rendición es de un solo repartidor. Elige los pedidos de una sola persona.", ErrorAction.BACK_TO_TRY),
            "SETTLEMENT_NO_SERIES" to Pair("No hay una serie de Nota de Venta activa en esta sucursal. Pídele al administrador que la cree en Ajustes.", ErrorAction.CONTACT_ADMIN),
            "SETTLEMENT_NO_CASH_SESSION" to Pair("Abre tu caja para recibir el efectivo del repartidor.", ErrorAction.BACK_TO_TRY),
            "SETTLEMENT_REASON_REQUIRED" to Pair("Hay una diferencia con lo esperado: escribe el motivo (mínimo 3 letras) y vuelve a intentar.", ErrorAction.BACK_TO_TRY),
            "SETTLEMENT_AMOUNT_INVALID" to Pair("Revisa el monto que recibiste: debe ser un número igual o mayor a cero.", ErrorAction.BACK_TO_TRY),
            "SETTLEMENT_TOO_MANY" to Pair("Puedes rendir hasta 50 pedidos a la vez. Quita algunos y vuelve a intentar.", ErrorAction.BACK_TO_TRY),
            "SETTLEMENT_PARTIAL" to Pair("Algunos pedidos no pudieron facturarse y siguen por rendir. Los demás ya quedaron rendidos. Actualiza para ver el estado.", ErrorAction.REFRESH),
            "REFUND_NOT_PENDING" to Pair("Este cobro ya no está por devolver. Actualiza para ver el estado.", ErrorAction.REFRESH),
            "MARKETPLACE_PAUSED" to Pair("El restaurante no está recibiendo pedidos por ahora.", ErrorAction.BACK_TO_TRY),
            "MARKETPLACE_PAUSE_INVALID" to Pair("La pausa debe durar entre 1 minuto y 24 horas, o ser indefinida.", ErrorAction.BACK_TO_TRY),
            "MARKETPLACE_THRESHOLD_INVALID" to Pair("Los minutos deben estar entre 0 y 240.", ErrorAction.BACK_TO_TRY),
            "ORDER_NOT_CANCELLABLE" to Pair("Este pedido ya está en preparación y no se puede cancelar desde la app. Actualiza para ver el estado.", ErrorAction.REFRESH),
            "DOC_REQUEST_NOT_COLLECTED" to Pair("Primero marca «Cobrado»: el comprobante se pide cuando el cliente ya pagó.", ErrorAction.REFRESH),
            "DOC_REQUEST_INVALID" to Pair("Revisa los datos del comprobante: boleta con DNI de 8 dígitos o factura con RUC de 11 dígitos y razón social.", ErrorAction.BACK_TO_TRY),
            "DOC_REQUEST_EXISTS" to Pair("Este pedido ya tiene una solicitud de comprobante. Actualiza para ver el estado.", ErrorAction.REFRESH),
            "DOC_REQUEST_NOT_PENDING" to Pair("Esta solicitud ya fue emitida o cancelada. Actualiza la lista.", ErrorAction.REFRESH),
            "DOC_REQUEST_NO_SERIES" to Pair("No hay una serie activa para ese comprobante en esta sucursal. Pídele al administrador que la cree en Ajustes.", ErrorAction.CONTACT_ADMIN),
            "DOC_REQUEST_BOLETA_LIMIT" to Pair("Una boleta de S/ 700 o más necesita el DNI del cliente. Cancela la solicitud y pídela otra vez con el DNI.", ErrorAction.BACK_TO_TRY),
            "CASH_PENDING_SETTLEMENT" to Pair("Tienes efectivo de delivery ya facturado que el repartidor aún no te entrega. Recíbelo en la pestaña Rendiciones de Entregas antes de cerrar la caja.", ErrorAction.REFRESH),
            "PAYMENT_NOT_PENDING_REVIEW" to Pair("Este pago ya fue validado o rechazado. Actualiza la lista.", ErrorAction.REFRESH),
            "PAYMENT_REJECT_REASON_REQUIRED" to Pair("Indica por qué rechazas el pago (mínimo 3 caracteres).", ErrorAction.BACK_TO_TRY),
            "PAY_METHOD_NOT_FOUND" to Pair("Ese medio de pago no existe.", ErrorAction.REFRESH),
            "PAY_METHOD_CODE_INVALID" to Pair("Elige un medio de pago que no sea efectivo de los registrados en Caja y Bancos.", ErrorAction.BACK_TO_TRY),
            "PAY_METHOD_LABEL_REQUIRED" to Pair("Escribe el nombre del medio de pago.", ErrorAction.BACK_TO_TRY),
            "PAY_METHOD_EXISTS" to Pair("Ya ofreces ese medio de pago.", ErrorAction.BACK_TO_TRY),
            "MANUAL_PAYMENT_NOT_AVAILABLE" to Pair("El pago con captura no está disponible en este restaurante.", ErrorAction.CONTACT_ADMIN),
            "PAYMENT_REVIEW_MINUTES_INVALID" to Pair("El plazo para pagar debe estar entre 5 y 120 minutos.", ErrorAction.BACK_TO_TRY),
            "ORDER_PAYMENT_PENDING" to Pair("Este pedido espera la validación del pago. Valídalo o recházalo en Entregas.", ErrorAction.REFRESH),
            "MANUAL_PAYMENT_METHOD_INVALID" to Pair("Elige uno de los medios de pago que ofrece el restaurante.", ErrorAction.BACK_TO_TRY),
            "PAYMENT_PROOF_NOT_ALLOWED" to Pair("Este pedido ya no admite subir la captura del pago.", ErrorAction.REFRESH),
            "PAYMENT_EXPIRED" to Pair("El tiempo para pagar venció y el pedido se canceló.", ErrorAction.REFRESH),
            "PAYMENT_PROOF_TOO_MANY" to Pair("Subiste demasiadas capturas para este pedido. Llama al restaurante.", ErrorAction.CONTACT_ADMIN),
            "REPORT_RANGE_INVALID" to Pair("Revisa las fechas del reporte: usa un rango de hasta 92 días.", ErrorAction.BACK_TO_TRY),
            "DELIVERY_MIN_ORDER_NOT_REACHED" to Pair("El pedido no llega al mínimo de delivery del restaurante.", ErrorAction.BACK_TO_TRY),
            "DELIVERY_MIN_ORDER_INVALID" to Pair("El pedido mínimo debe estar entre S/ 0 y S/ 9999.99.", ErrorAction.BACK_TO_TRY),
            "DELIVERY_ETA_INVALID" to Pair("El tiempo de entrega debe estar entre 0 y 240 minutos.", ErrorAction.BACK_TO_TRY),
            "DELIVERY_RULES_UNAVAILABLE" to Pair("Las reglas de delivery aún no están disponibles para este restaurante; intenta de nuevo en unos minutos.", ErrorAction.REFRESH),
            "COMPANY_ADMIN_REQUIRED" to Pair("Solo un administrador puede cambiar los datos de la empresa, las sucursales y las series.", ErrorAction.CONTACT_ADMIN),
            "OUTSIDE_BUSINESS_HOURS" to Pair("El restaurante está cerrado por ahora. Intenta de nuevo cuando abra.", ErrorAction.REFRESH),
            "BUSINESS_HOURS_INVALID" to Pair("Revisa el horario: cada tramo necesita día, hora de apertura y de cierre distintas (formato HH:MM).", ErrorAction.BACK_TO_TRY),
            "BUSINESS_HOURS_UNAVAILABLE" to Pair("El horario de atención aún no está disponible para este restaurante; intenta de nuevo en unos minutos.", ErrorAction.REFRESH),
            "SESSION_HAS_SERVED_ITEMS" to Pair("Este pedido tiene platos ya servidos. Si lo anulas se borran igual: confirma para continuar.", ErrorAction.BACK_TO_TRY),
            "OVERPAYMENT_NOT_CASH" to Pair("Solo el efectivo puede dar vuelto: el pago con Yape, tarjeta o transferencia no puede ser mayor al total.", ErrorAction.BACK_TO_TRY),
            "OVERPAYMENT_TOO_HIGH" to Pair("El vuelto sería demasiado alto. Revisa el monto recibido.", ErrorAction.BACK_TO_TRY),
            "DISCOUNT_FORBIDDEN" to Pair("No tienes permiso para dar descuentos ni cambiar precios. Pídele a un administrador que lo haga o que te lo autorice.", ErrorAction.CONTACT_ADMIN),
            "OPS_CONTROLS_UNAVAILABLE" to Pair("Los controles de operación aún no están disponibles para este restaurante; intenta de nuevo en unos minutos.", ErrorAction.REFRESH),
            "REOPEN_SALE_NOT_CANCELLED" to Pair("Primero anula la venta; después podrás reabrir el pedido para cobrarlo de nuevo.", ErrorAction.REFRESH),
            "REOPEN_NOT_APPLICABLE" to Pair("Esta venta no viene de una mesa o pedido del restaurante, o ya fue reemplazada por otro comprobante.", ErrorAction.REFRESH),
            "REOPEN_TABLE_OCCUPIED" to Pair("La mesa ya tiene otro pedido abierto. Libérala antes de reabrir este.", ErrorAction.REFRESH),
            "REOPEN_TOO_OLD" to Pair("Pasaron más de 48 horas desde que se cerró este pedido y ya no se puede reabrir.", ErrorAction.REFRESH),
            "REOPEN_REASON_REQUIRED" to Pair("Indica el motivo para reabrir el pedido (mínimo 3 caracteres).", ErrorAction.BACK_TO_TRY),
        )
        for ((code, want) in esperado) {
            val info = http(409, code)
            assertEquals(code, want.first, info.message)
            assertEquals(code, listOf(want.second), info.actions)
            assertTrue(code, code in ErrorCatalog.knownCodes)
        }
        // Mismo orden que el bloque final de Tauri: DELIVERY_CANCEL.., DRIVER_*, SESSION_*, USE_* DELIVERY_FEE_* y, al final, los 6 de D2b (efectivo contra entrega).
        val tail = ErrorCatalog.knownCodes.toList().takeLast(esperado.size)
        assertEquals(esperado.keys.toList(), tail)
    }

    /** Platos servidos y fuera de horario traen datos del momento (cuantos platos, cuando abre): gana el texto del servidor. */
    @Test fun serverTextWinsForDynamicCodes() {
        val served = "Este pedido tiene 2 plato(s) ya servido(s). Si lo anulas se borran igual: confirma para continuar."
        assertEquals(served, http(409, "SESSION_HAS_SERVED_ITEMS", served).message)
        val closed = "El restaurante está cerrado por ahora. Abre mañana a las 11:00."
        assertEquals(closed, http(409, "OUTSIDE_BUSINESS_HOURS", closed).message)
        // Sin texto legible del servidor cae al texto fijo del catálogo.
        assertTrue(http(409, "OUTSIDE_BUSINESS_HOURS").message.startsWith("El restaurante está cerrado"))
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
