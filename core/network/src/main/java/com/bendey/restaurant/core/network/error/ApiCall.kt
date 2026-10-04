package com.bendey.restaurant.core.network.error

import com.bendey.restaurant.core.domain.model.AppResult
import kotlin.coroutines.cancellation.CancellationException

/**
 * Unico `apiCall` de los repositorios (R5): ejecuta [block] y traduce cualquier fallo con el
 * [NetworkErrorMapper]. Reemplaza las 12 copias privadas que devolvian "Error de conexión" a secas.
 *
 * [flow] elige el texto de reserva cuando el fallo es de red o 5xx: en comanda y cobro dice si la
 * operacion se realizo o no. NUNCA reintenta solo: cobrar y enviar comanda no son idempotentes.
 */
inline fun <T> apiCall(flow: ErrorFlow = ErrorFlow.GENERIC, block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: CancellationException) {
    // Cancelar una corrutina no es un error de red: se propaga (antes se mostraba como fallo).
    throw e
} catch (e: Exception) {
    val mapped = NetworkErrorMapper.map(e, flow)
    AppResult.Error(mapped.message ?: ErrorCatalog.resolve(Failure(FailureKind.OTHER), flow).message, mapped)
}
