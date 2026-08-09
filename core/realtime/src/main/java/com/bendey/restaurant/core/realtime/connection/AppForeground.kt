package com.bendey.restaurant.core.realtime.connection

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Si la aplicación está en pantalla, para que el WebSocket no siga vivo cuando nadie lo mira.
 *
 * EL PROBLEMA QUE RESUELVE. El cliente hace ping cada 30 s con `delay()` en una corrutina y el servidor
 * corta a los 90 s sin leer nada. Cuando el teléfono se bloquea, Android difiere las corrutinas: los
 * pings dejan de salir y el servidor mata la conexión. El cliente reconecta, vuelve a caerse, y así todo
 * el día. Medido en producción el 2026-08-08 sobre un solo tenant: **637 conexiones y 623
 * re-hidrataciones del estado operativo**, una por reconexión — un tercio de todo su tráfico, gastado en
 * reconstruir lo que ya tenía. Para el mozo eso se siente como que el sistema está lento.
 *
 * Saber si la app está en primer plano permite cerrar la conexión a propósito en vez de dejar que se
 * muera sola. NO SE PIERDE NADA: en segundo plano la conexión ya duraba 90 segundos como máximo. Lo que
 * se deja de hacer es el churn.
 *
 * Los avisos de pedidos con la app minimizada necesitan un servicio en primer plano o notificaciones
 * push; hoy tampoco funcionaban pasados esos 90 segundos.
 */
@Singleton
class AppForeground @Inject constructor() {

    // Arranca en `true` porque quien lo observa lo hace al crearse la primera pantalla: ahí la app YA
    // está en primer plano, y partir de `false` provocaría una desconexión y reconexión inmediatas.
    private val _enPantalla = MutableStateFlow(true)
    val enPantalla: StateFlow<Boolean> = _enPantalla.asStateFlow()

    /** Empieza a observar el ciclo de vida del proceso. Hay que llamarlo desde el hilo principal. */
    fun observar() {
        if (observando) return
        observando = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    _enPantalla.value = true
                }

                override fun onStop(owner: LifecycleOwner) {
                    _enPantalla.value = false
                }
            },
        )
    }

    private var observando = false
}
