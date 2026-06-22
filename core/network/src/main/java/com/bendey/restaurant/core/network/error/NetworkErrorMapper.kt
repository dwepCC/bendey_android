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
            val parsed = runCatching {
                Json.decodeFromString<ApiErrorDto>(bodyMessage).error
            }.getOrNull()?.takeIf { it.isNotBlank() }
            return IllegalStateException(parsed ?: bodyMessage, error)
        }
        val message = when (code) {
            401 -> unauthorizedMessage
            403 -> "Acceso denegado"
            else -> "Error de conexión ($code)"
        }
        return IllegalStateException(message, error)
    }
}
