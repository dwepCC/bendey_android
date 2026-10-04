package com.bendey.restaurant.core.network.error

import com.bendey.restaurant.core.network.dto.ApiErrorDto
import com.bendey.restaurant.core.network.serialization.ApiJson
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.PortUnreachableException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Error ya traducido para el usuario. Sigue siendo un [IllegalStateException] (los llamadores que
 * esperaban ese tipo no cambian) pero lleva el [info] del catalogo: acciones y codigo de soporte.
 */
class ApiException(
    val info: ErrorInfo,
    val status: Int? = null,
    /** `code` del backend, si vino. */
    val apiCode: String? = null,
    cause: Throwable? = null,
) : IllegalStateException(info.message, cause)

/**
 * Traduce cualquier fallo de red/HTTP a un error con texto accionable (R5, UX-REDESIGN §15).
 * Nunca deja pasar excepciones de Java, HTML de proxy ni "Error de conexion" para un 5xx; un 401 de
 * SESION no se llama "credenciales incorrectas" (eso solo vale en los flujos de login).
 *
 * Los errores que la propia app lanza (`require`/`error`, IllegalStateException con texto propio) se
 * respetan tal cual.
 */
object NetworkErrorMapper {

    fun map(error: Throwable, flow: ErrorFlow = ErrorFlow.GENERIC): Throwable = when (error) {
        // Ya traducido (doble mapeo en cadenas runCatching/recoverCatching).
        is ApiException, is ModuleLockedException, is SubscriptionBlockedException -> error
        is HttpException -> mapHttp(error, flow)
        is SSLException -> fromFailure(Failure(FailureKind.TLS), flow, error)
        is IOException -> fromFailure(Failure(ioKind(error)), flow, error)
        is SerializationException -> fromFailure(Failure(FailureKind.UNREADABLE), flow, error)
        // Mensaje propio de la app (validaciones, require/error): se respeta.
        is IllegalStateException, is IllegalArgumentException ->
            if (error.message.isNullOrBlank()) fromFailure(Failure(FailureKind.OTHER), flow, error) else error
        else -> fromFailure(Failure(FailureKind.OTHER), flow, error)
    }

    /**
     * Para respuestas HTTP de error leidas a mano con OkHttp (multipart), que no pasan por Retrofit.
     * [body] es el cuerpo crudo (JSON del backend, HTML de un proxy o vacio).
     */
    fun mapResponse(status: Int, body: String?, flow: ErrorFlow = ErrorFlow.GENERIC): Throwable =
        mapStatusAndBody(status, body, null, flow)

    private fun mapHttp(error: HttpException, flow: ErrorFlow): Throwable {
        val body = runCatching { error.response()?.errorBody()?.string() }.getOrNull()
        return mapStatusAndBody(error.code(), body, error, flow)
    }

    private fun mapStatusAndBody(status: Int, body: String?, cause: Throwable?, flow: ErrorFlow): Throwable {
        val dto = body?.takeIf { it.isNotBlank() }
            ?.let { runCatching { ApiJson.decodeFromString(ApiErrorDto.serializer(), it) }.getOrNull() }
        val serverMessage = (dto?.error ?: dto?.message)?.takeIf { it.isNotBlank() }
        val moduleKey = dto?.module?.takeIf { it.isNotBlank() }

        if (status == 403 && moduleKey != null) {
            val text = ErrorCatalog.readableServerMessage(serverMessage)
                ?: "Esta función no está incluida en tu plan. Pídele al administrador que revise el plan."
            return ModuleLockedException(text, moduleKey, cause)
        }
        if (status == 402) {
            val blocked = dto?.code == "TENANT_BLOCKED"
            val support = dto?.supportMessage?.takeIf { it.isNotBlank() }
            return SubscriptionBlockedException(support ?: subscriptionBlockedMessage(blocked), blocked, cause)
        }
        val failure = Failure(
            kind = FailureKind.HTTP,
            status = status,
            code = dto?.code?.takeIf { it.isNotBlank() },
            serverMessage = serverMessage,
        )
        return fromFailure(failure, flow, cause)
    }

    private fun fromFailure(failure: Failure, flow: ErrorFlow, cause: Throwable?): ApiException {
        val info = ErrorCatalog.resolve(failure, flow)
        return ApiException(info, failure.status, failure.code, cause)
    }

    /** Clasifica un [IOException]: la peticion nunca salio (sin red) o pudo haberse ejecutado (timeout/corte). */
    internal fun ioKind(e: IOException): FailureKind = when (e) {
        is UnknownHostException, is ConnectException, is NoRouteToHostException, is PortUnreachableException ->
            FailureKind.NO_CONNECTION
        else -> FailureKind.TIMEOUT
    }

    /**
     * Si el servidor respondio 409 (conflicto).
     *
     * Vive aquí porque Retrofit no cruza a `core:data`: sin esto, distinguir un conflicto obligaría a
     * comparar el texto del mensaje, que cambia en cuanto alguien reescribe el error del backend.
     */
    fun esConflicto(error: Throwable): Boolean = error is HttpException && error.code() == 409

    /** Código HTTP de la respuesta del servidor, o null si el fallo no vino de una respuesta (red, parseo). */
    fun httpStatus(error: Throwable): Int? = (error as? HttpException)?.code()

    /** Informacion de catalogo (acciones, codigo de soporte) de un error ya mapeado, si la lleva. */
    fun infoOf(error: Throwable?): ErrorInfo? = (error as? ApiException)?.info

    private fun subscriptionBlockedMessage(blocked: Boolean): String = if (blocked) {
        "Tu cuenta está bloqueada. Comunícate con soporte para reactivar el servicio."
    } else {
        "Tu plan venció y el acceso quedó restringido. Registra tu pago para seguir trabajando."
    }
}
