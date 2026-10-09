package com.bendey.restaurant.push

import android.content.Context
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.network.api.PushApi
import com.bendey.restaurant.core.network.api.RegisterPushRequestDto
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/** Registra el dispositivo en el backend para recibir los avisos de pedidos con la app cerrada. */
@Singleton
class PushRegistrar @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionStore: UserSessionStore,
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) {
    private val api: PushApi get() = tenantRetrofitProvider.create()

    /** Se llama con el token nuevo (o el actual). Sin sesion o sin red falla en silencio: se reintenta al entrar. */
    suspend fun register(token: String) {
        runCatching { api.registerPush(RegisterPushRequestDto(token)) }
    }

    /** Escucha el inicio de sesion y registra el token vigente. Es un bucle: llamarlo desde un scope de la Activity. */
    suspend fun registerWhileLoggedIn() {
        sessionStore.userSessionFlow
            .map { it != null }
            .distinctUntilChanged()
            .filter { it }
            .collect { Push.currentToken(context)?.let { register(it) } }
    }
}
