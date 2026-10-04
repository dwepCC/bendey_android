package com.bendey.restaurant.core.network.error

import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class NetworkErrorMapperTest {

    private fun httpError(status: Int, body: String, type: String = "application/json"): HttpException =
        HttpException(Response.error<Any>(status, body.toResponseBody(type.toMediaType())))

    private fun msg(t: Throwable, flow: ErrorFlow = ErrorFlow.GENERIC) = NetworkErrorMapper.map(t, flow).message.orEmpty()

    @Test fun unknownHostIsNoConnectionNotJavaText() {
        val m = msg(UnknownHostException("Unable to resolve host \"api.bendey.cloud\""))
        assertFalse(m.contains("Unable"))
        assertFalse(m.contains("resolve"))
        assertTrue(m.contains("No hay conexión"))
    }

    @Test fun connectFailureOnSendComandaSaysItDidNotLeave() {
        val m = msg(ConnectException("Failed to connect"), ErrorFlow.SEND_COMANDA)
        assertTrue(m, m.contains("No salió a cocina"))
    }

    @Test fun timeoutOnChargeAsksToCheckSalesFirst() {
        val m = msg(SocketTimeoutException("timeout"), ErrorFlow.CHARGE)
        assertTrue(m, m.contains("revisa en Ventas si ya se registró"))
        assertFalse(m.contains("timeout"))
    }

    @Test fun genericIoExceptionIsUncertainNotRawMessage() {
        val m = msg(IOException("unexpected end of stream on https://x"))
        assertFalse(m.contains("unexpected"))
        assertTrue(m.contains("tardó"))
    }

    @Test fun tlsFailureIsExplained() {
        val m = msg(SSLHandshakeException("PKIX path building failed"))
        assertFalse(m.contains("PKIX"))
        assertTrue(m.contains("conexión segura"))
    }

    @Test fun html502FromProxyNeverReachesTheUser() {
        val m = msg(httpError(502, "<html><body><h1>502 Bad Gateway</h1>nginx</body></html>", "text/html"))
        assertFalse(m.contains("<"))
        assertFalse(m.contains("nginx"))
        assertFalse(m.lowercase().contains("conexión"))
        assertTrue(m.contains("de nuestro lado"))
    }

    @Test fun serverErrorWithoutBodyIsNotConnection() {
        val m = msg(httpError(500, ""))
        assertFalse(m.lowercase().contains("conexión"))
        assertFalse(m.contains("500"))
    }

    @Test fun sessionUnauthorizedIsNotWrongCredentials() {
        val noBody = msg(httpError(401, ""))
        assertTrue(noBody, noBody.startsWith("Tu sesión venció"))
        val withCode = msg(httpError(401, """{"error":"Token inválido o expirado","code":"AUTH_TOKEN_INVALID"}"""))
        assertTrue(withCode.startsWith("Tu sesión venció"))
        assertFalse(withCode.contains("redenciales"))
    }

    @Test fun loginKeepsCredentialsWording() {
        assertTrue(msg(httpError(401, ""), ErrorFlow.LOGIN_PIN).contains("PIN incorrecto"))
        assertTrue(msg(httpError(401, ""), ErrorFlow.LOGIN_EMAIL).contains("Correo o contraseña"))
        val code = msg(
            httpError(401, """{"error":"PIN incorrecto o sin acceso a esta estación","code":"LOGIN_PIN_INCORRECT"}"""),
            ErrorFlow.LOGIN_PIN,
        )
        assertTrue(code, code.contains("cambia de estación"))
    }

    @Test fun backendCodeWinsOverServerText() {
        val m = msg(httpError(400, """{"error":"la mesa 'A1' ya está ocupada","code":"TABLE_OCCUPIED"}"""), ErrorFlow.OPEN_TABLE)
        assertTrue(m.contains("otro mozo"))
    }

    @Test fun bodyWithExtraFieldsStillParses() {
        val m = msg(httpError(400, """{"error":"x","code":"PIN_INCORRECT","retryable":true,"details":{"n":2}}"""))
        assertTrue(m, m.startsWith("PIN incorrecto"))
    }

    @Test fun legibleServerTextWithoutCodeIsKept() {
        assertEquals("El nombre ya existe", msg(httpError(400, """{"error":"El nombre ya existe"}""")))
    }

    @Test fun serverTextWithJargonFallsBackToFlowCopy() {
        val m = msg(httpError(400, """{"error":"gorm: SQLSTATE 23000 duplicate"}"""))
        assertFalse(m.contains("SQLSTATE"))
    }

    @Test fun moduleLockedAndSubscriptionStillTyped() {
        val locked = NetworkErrorMapper.map(httpError(403, """{"error":"Módulo no incluido","module":"billing"}"""))
        assertTrue(locked is ModuleLockedException)
        assertEquals("billing", (locked as ModuleLockedException).moduleKey)
        val blocked = NetworkErrorMapper.map(httpError(402, """{"error":"x","code":"TENANT_BLOCKED"}"""))
        assertTrue(blocked is SubscriptionBlockedException)
        assertTrue(NetworkErrorMapper.map(httpError(402, "")) is SubscriptionBlockedException)
    }

    @Test fun conflictHelpersStillWork() {
        val e = httpError(409, """{"error":"x"}""")
        assertTrue(NetworkErrorMapper.esConflicto(e))
        assertEquals(409, NetworkErrorMapper.httpStatus(e))
        assertNull(NetworkErrorMapper.httpStatus(IOException()))
    }

    @Test fun appAuthoredMessagesPassThrough() {
        val own = IllegalStateException("Vincule el RUC antes de iniciar sesión")
        assertTrue(NetworkErrorMapper.map(own) === own)
        val arg = IllegalArgumentException("Ingresa un RUC válido")
        assertTrue(NetworkErrorMapper.map(arg) === arg)
    }

    @Test fun serializationAndUnknownExceptionsAreHumanised() {
        val m = msg(SerializationException("Unexpected JSON token at offset 0"))
        assertFalse(m.contains("JSON"))
        assertTrue(m.contains("No pudimos leer la respuesta"))
        val n = msg(NullPointerException("Attempt to invoke virtual method"))
        assertFalse(n.contains("Attempt"))
        assertTrue(n.contains("No se pudo completar"))
        assertTrue(msg(IllegalStateException()).contains("No se pudo completar"))
    }

    @Test fun mappingIsIdempotent() {
        val once = NetworkErrorMapper.map(UnknownHostException("x"), ErrorFlow.CHARGE)
        assertTrue(NetworkErrorMapper.map(once, ErrorFlow.GENERIC) === once)
    }

    @Test fun mapResponseHandlesRawOkHttpBodies() {
        assertFalse(NetworkErrorMapper.mapResponse(413, "<html>too large</html>").message.orEmpty().contains("<"))
        assertTrue(
            NetworkErrorMapper.mapResponse(401, """{"error":"x","code":"AUTH_TOKEN_MISSING"}""").message.orEmpty()
                .startsWith("Tu sesión venció"),
        )
    }

    @Test fun infoOfExposesActions() {
        val t = NetworkErrorMapper.map(httpError(400, """{"error":"x","code":"CASH_SESSION_REQUIRED"}"""))
        assertEquals(listOf(ErrorAction.OPEN_CASH), NetworkErrorMapper.infoOf(t)?.actions)
    }
}
