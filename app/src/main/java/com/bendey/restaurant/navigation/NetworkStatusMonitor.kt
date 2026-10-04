package com.bendey.restaurant.navigation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.bendey.restaurant.core.realtime.dispatcher.ConnectionState
import com.bendey.restaurant.core.ui.components.BendeyConnectionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Función pura: combina lo que dice el WebSocket con lo que dice el sistema operativo.
 *
 * - Sin red en el dispositivo => OFFLINE (manda el SO: el WS tarda hasta ~90 s en darse cuenta).
 * - Con red y WS autenticado => ONLINE.
 * - Con red y WS conectando/reconectando => CONNECTING (verificando, no afirmamos nada).
 * - Con red y WS apagado a propósito (sin sesión, sin permiso de realtime) => ONLINE: no hay otra señal
 *   y mostrar "Sin conexión" a quien nunca abre el WS sería mentir en el otro sentido.
 */
internal fun resolveConnectionStatus(realtime: ConnectionState, hasNetwork: Boolean): BendeyConnectionStatus = when {
    !hasNetwork -> BendeyConnectionStatus.OFFLINE
    realtime == ConnectionState.CONNECTING || realtime == ConnectionState.RECONNECTING -> BendeyConnectionStatus.CONNECTING
    else -> BendeyConnectionStatus.ONLINE
}

/** Conectividad del dispositivo (red con salida a Internet), vía NetworkCallback. Sin dependencias nuevas. */
@Singleton
class NetworkStatusMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val hasNetwork: Flow<Boolean> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
            trySend(true)
            awaitClose { }
            return@callbackFlow
        }
        fun current(): Boolean {
            val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(current()) }
            override fun onLost(network: Network) { trySend(current()) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { trySend(current()) }
        }
        trySend(current())
        cm.registerNetworkCallback(
            NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
            callback,
        )
        awaitClose { runCatching { cm.unregisterNetworkCallback(callback) } }
    }.distinctUntilChanged().conflate()
}
