package com.bendey.restaurant.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/** Ultima vez que el servidor respondio algo (< 500): prueba de vida para el indicador de conexion. */
@Singleton
class ApiActivitySignal @Inject constructor() {
    @Volatile
    var lastSuccessMs: Long = 0L
        private set

    fun markSuccess(nowMs: Long = System.currentTimeMillis()) {
        lastSuccessMs = nowMs
    }
}

class ApiActivityInterceptor @Inject constructor(
    private val signal: ApiActivitySignal,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code < 500) signal.markSuccess()
        return response
    }
}
