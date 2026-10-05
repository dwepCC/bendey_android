package com.bendey.restaurant.core.data.cache

import com.bendey.restaurant.core.domain.cache.CheckoutCachePolicy
import com.bendey.restaurant.core.network.api.ProductsApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * R10.5. Equivalente Android de `CatalogVersionWatcher` (Tauri App.tsx): con la app en pantalla y
 * sesion iniciada pregunta la version del catalogo cada 2 min y al volver a primer plano; si cambia,
 * o si la app estuvo fuera mas de 10 min, descarta la cache de cobro (series, contactos, metodos de
 * pago) y de categorias para que lo creado en otro equipo aparezca sin reiniciar sesion.
 * Un chequeo fallido es silencioso: la cache sigue valida hasta el siguiente.
 */
@Singleton
class CatalogVersionWatcher @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
    private val cache: OperationalDataCache,
) {
    @Volatile private var lastKnownVersion: String? = null
    @Volatile private var backgroundedAtMs: Long? = null
    @Volatile private var started = false

    fun start(scope: CoroutineScope, foreground: Flow<Boolean>, authenticated: Flow<Boolean>) {
        if (started) return
        started = true
        scope.launch {
            // Cada vez que cambia (en pantalla && con sesion) se reinicia el ciclo.
            combine(foreground, authenticated) { fg, auth -> fg to auth }
                .distinctUntilChanged()
                .collectLatest { (fg, auth) ->
                    if (!auth) {
                        // Re-login (otro tenant) debe re-basear, no comparar con la sesion anterior.
                        lastKnownVersion = null
                        backgroundedAtMs = null
                        return@collectLatest
                    }
                    if (!fg) {
                        if (backgroundedAtMs == null) backgroundedAtMs = System.currentTimeMillis()
                        return@collectLatest
                    }
                    val since = backgroundedAtMs
                    backgroundedAtMs = null
                    if (CheckoutCachePolicy.shouldInvalidateOnForeground(since, System.currentTimeMillis())) {
                        invalidate()
                    }
                    while (true) {
                        check()
                        delay(CheckoutCachePolicy.CATALOG_VERSION_POLL_MS)
                    }
                }
        }
    }

    private fun invalidate() {
        cache.clearCheckoutMeta()
        cache.clearCategories()
    }

    private suspend fun check() {
        try {
            val version = tenantRetrofitProvider.create<ProductsApi>().getCatalogVersion().catalogUpdatedAt
            if (CheckoutCachePolicy.catalogVersionChanged(lastKnownVersion, version)) invalidate()
            lastKnownVersion = version
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Silencioso a proposito (red caida, etc.).
        }
    }
}
