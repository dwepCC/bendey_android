package com.bendey.restaurant.core.network.error

import com.bendey.restaurant.core.network.dto.ApiErrorDto
import kotlinx.serialization.json.Json
import retrofit2.HttpException

object NetworkErrorMapper {

    fun map(error: Throwable, unauthorizedMessage: String = "Credenciales incorrectas"): Throwable {
        if (error !is HttpException) return error
        val code = error.code()
        val bodyMessage = runCatching {
            error.response()?.errorBody()?.string()?.takeIf { it.isNotBlank() }
        }.getOrNull()
        if (bodyMessage != null) {
            val parsedDto = runCatching { Json.decodeFromString<ApiErrorDto>(bodyMessage) }.getOrNull()
            val parsedMessage = parsedDto?.error?.takeIf { it.isNotBlank() }
            val moduleKey = parsedDto?.module?.takeIf { it.isNotBlank() }
            if (code == 403 && moduleKey != null) {
                return ModuleLockedException(parsedMessage ?: bodyMessage, moduleKey, error)
            }
            if (code == 402) {
                val blocked = parsedDto?.code == "TENANT_BLOCKED"
                val supportMessage = parsedDto?.supportMessage?.takeIf { it.isNotBlank() }
                return SubscriptionBlockedException(
                    supportMessage ?: subscriptionBlockedMessage(blocked),
                    blocked,
                    error,
                )
            }
            return IllegalStateException(parsedMessage ?: bodyMessage, error)
        }
        if (code == 402) {
            return SubscriptionBlockedException(subscriptionBlockedMessage(blocked = false), false, error)
        }
        val message = when (code) {
            401 -> unauthorizedMessage
            403 -> "Acceso denegado"
            else -> "Error de conexión ($code)"
        }
        return IllegalStateException(message, error)
    }

    private fun subscriptionBlockedMessage(blocked: Boolean): String = if (blocked) {
        "Tu cuenta está bloqueada. Comunícate con soporte para reactivar el servicio."
    } else {
        "Tu plan venció y el acceso quedó restringido. Registra tu pago para seguir trabajando."
    }
}
