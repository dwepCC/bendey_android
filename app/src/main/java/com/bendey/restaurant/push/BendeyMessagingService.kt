package com.bendey.restaurant.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BendeyMessagingService : FirebaseMessagingService() {

    @Inject lateinit var registrar: PushRegistrar

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** FCM roto el token: se vuelve a registrar (sin sesion falla en silencio y se registra al entrar). */
    override fun onNewToken(token: String) {
        scope.launch { registrar.register(token) }
    }

    /** Con la app abierta FCM no muestra nada solo: la mostramos nosotros. Con la app cerrada la muestra el sistema. */
    override fun onMessageReceived(message: RemoteMessage) {
        val n = message.notification ?: return
        Push.show(applicationContext, n.title.orEmpty(), n.body.orEmpty(), (message.data["session_id"]?.toIntOrNull() ?: 0) + 3000)
    }
}
