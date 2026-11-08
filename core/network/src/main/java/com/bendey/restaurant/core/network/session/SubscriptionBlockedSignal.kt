package com.bendey.restaurant.core.network.session

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Aviso de que el backend respondió 402 (plan suspendido o bloqueado).
 *
 * Windows y el ERP llevan a "Mi plan" en ese momento; Android solo mostraba el error de cada pantalla
 * y el cliente no sabía dónde pagar. La capa de red lo emite y la navegación decide a dónde ir. Una
 * pantalla dispara varias peticiones a la vez, así que se avisa como mucho una vez por minuto.
 */
object SubscriptionBlockedSignal {
    private const val INTERVALO_MS = 60_000L
    private val ultimo = AtomicLong(0)
    private val _eventos = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val eventos: SharedFlow<Unit> = _eventos.asSharedFlow()

    fun report(ahoraMs: Long = System.currentTimeMillis()) {
        val previo = ultimo.get()
        if (ahoraMs - previo < INTERVALO_MS) return
        if (ultimo.compareAndSet(previo, ahoraMs)) _eventos.tryEmit(Unit)
    }
}
