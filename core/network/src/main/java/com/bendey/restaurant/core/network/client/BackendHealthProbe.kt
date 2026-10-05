package com.bendey.restaurant.core.network.client

import com.bendey.restaurant.core.network.api.HealthApi
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/** Sonda de alcance: `GET /api/health/live`. Cualquier respuesta < 500 significa "el servidor esta ahi". */
@Singleton
class BackendHealthProbe @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) {
    suspend fun isReachable(): Boolean = try {
        tenantRetrofitProvider.create<HealthApi>().live().close()
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        e.code() < 500
    } catch (_: Exception) {
        false
    }
}
