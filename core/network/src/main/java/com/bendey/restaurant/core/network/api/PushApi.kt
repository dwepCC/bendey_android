package com.bendey.restaurant.core.network.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class RegisterPushRequestDto(
    @SerialName("fcm_token") val fcmToken: String,
    val platform: String = "android",
)

/** Registro del dispositivo del personal para recibir avisos (pedido nuevo del marketplace) con la app cerrada. */
interface PushApi {
    @POST("/api/push/register")
    suspend fun registerPush(@Body body: RegisterPushRequestDto)
}
