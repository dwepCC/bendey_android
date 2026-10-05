package com.bendey.restaurant.navigation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.bendey.restaurant.core.domain.connectivity.ReachabilityLevel
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
 * Funcion pura: combina red del dispositivo + alcance REAL del backend (sonda) + WebSocket. NUNCA devuelve
 * ONLINE (verde) si no esta comprobado:
 *
 * - Sin red en el dispositivo => OFFLINE (manda el SO: el WS tarda hasta ~90 s en darse cuenta).
 * - Backend inalcanzable (3 sondas seguidas fallidas) => OFFLINE.
 * - Backend sin comprobar todavia o degradado (1-2 fallos) => CONNECTING.
 * - Backend alcanzable y WS conectando/reconectando => CONNECTING.
 * - Backend alcanzable y WS autenticado => ONLINE.
 * - Backend alcanzable y WS apagado a proposito (sin sesion, sin permiso de realtime, segundo plano) =>
 *   ONLINE: el WS no es la senal en ese caso y la sonda ya confirmo el servidor.
 */
internal fun resolveConnectionStatus(
    realtime: ConnectionState,
    hasNetwork: Boolean,
    reachability: ReachabilityLevel,
): BendeyConnectionStatus = when {
    !hasNetwork -> BendeyConnectionStatus.OFFLINE
    reachability == ReachabilityLevel.UNREACHABLE -> BendeyConnectionStatus.OFFLINE
    reachability == ReachabilityLevel.UNKNOWN || reachability == ReachabilityLevel.DEGRADED -> BendeyConnectionStatus.CONNECTING
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
