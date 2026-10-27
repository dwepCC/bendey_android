package com.bendey.restaurant.core.data.locale

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bendey.restaurant.core.domain.locale.TenantLocale
import com.bendey.restaurant.core.domain.locale.TenantLocaleHolder
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.network.api.SessionCapabilitiesApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.localeDataStore: DataStore<Preferences> by preferencesDataStore(name = "bendey_locale_prefs")

/**
 * Locale del tenant (paquete de país, incremento I4c): lo pide a `GET /api/session/capabilities`
 * tras iniciar sesión, lo guarda en DataStore por restaurante (slug) y alimenta [TenantLocaleHolder],
 * que es lo que leen `Money` y `TenantDateTime`.
 *
 * Al arrancar carga lo guardado del restaurante vinculado. Nada de esto puede romper la app: un fallo
 * de red, un servidor viejo sin `locale` o un JSON raro dejan lo que había (lo guardado o Perú).
 */
@Singleton
class LocaleRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val sessionStore: UserSessionStore,
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) {
    private val dataStore = context.localeDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)

    /** Se llama una vez desde `Application.onCreate`; las siguientes llamadas no hacen nada. */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            sessionStore.tenantFlow
                .map { it?.slug.orEmpty() }
                .combine(sessionStore.isAuthenticatedFlow) { slug, authenticated -> slug to authenticated }
                .distinctUntilChanged()
                .collect { (slug, authenticated) ->
                    // Primero lo guardado (instantáneo, sin red); luego, con sesión, lo del servidor.
                    applySaved(slug)
                    if (authenticated && slug.isNotEmpty()) refresh(slug)
                }
        }
    }

    /** Vuelve a pedir el locale del restaurante vinculado (p. ej. tras un login). Nunca lanza. */
    suspend fun refresh() {
        val slug = runCatching { sessionStore.tenantFlow.first()?.slug }.getOrNull().orEmpty()
        if (slug.isNotEmpty()) refresh(slug)
    }

    private suspend fun applySaved(slug: String) {
        val saved = if (slug.isEmpty()) null else readSaved(slug)
        if (saved != null) TenantLocaleHolder.set(saved) else TenantLocaleHolder.reset()
    }

    private suspend fun readSaved(slug: String): TenantLocale? = try {
        TenantLocaleJson.fromLocaleJson(dataStore.data.first()[key(slug)])
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private suspend fun refresh(slug: String) {
        try {
            val body = tenantRetrofitProvider.create<SessionCapabilitiesApi>().capabilities().use { it.string() }
            val locale = TenantLocaleJson.fromCapabilities(body) ?: return // servidor viejo: queda lo que había
            val raw = TenantLocaleJson.localeObjectOf(body) ?: return
            // Si mientras tanto se cambió de restaurante, no pisar el locale del nuevo.
            if (sessionStore.tenantFlow.first()?.slug != slug) return
            dataStore.edit { it[key(slug)] = raw }
            TenantLocaleHolder.set(locale)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Sin red, 401, 5xx, tenant sin URL…: se queda con lo guardado o Perú.
        }
    }

    private fun key(slug: String) = stringPreferencesKey("locale_$slug")
}
