package com.bendey.restaurant.core.realtime

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object RealtimeProtocol {
    const val VERSION = 1

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun authFrame(token: String, app: String, version: String, platform: String = "android"): String =
        json.encodeToString(
            AuthFrame.serializer(),
            AuthFrame(
                op = "auth",
                v = VERSION,
                token = token,
                client = ClientInfo(app = app, version = version, platform = platform),
            ),
        )

    fun pingFrame(): String =
        """{"op":"ping","v":$VERSION,"ts":${System.currentTimeMillis() / 1000}}"""

    fun parseWireMessage(raw: String): RealtimeWireMessage? {
        return try {
            val root = json.parseToJsonElement(raw).jsonObject
            when (root["op"]?.jsonPrimitive?.content) {
                "auth.ok" -> RealtimeWireMessage.Authenticated(json.decodeFromString<AuthOk>(raw))
                "auth.error" -> RealtimeWireMessage.Failed(json.decodeFromString<AuthError>(raw))
                "event" -> RealtimeWireMessage.Event(json.decodeFromString<DomainEventEnvelope>(raw))
                "pong" -> RealtimeWireMessage.Pong
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}

@Serializable
data class AuthFrame(
    val op: String,
    val v: Int,
    val token: String,
    val client: ClientInfo,
)

@Serializable
data class ClientInfo(
    val app: String,
    val version: String,
    val platform: String? = null,
    @SerialName("device_id") val deviceId: String? = null,
)

@Serializable
data class AuthOk(
    val op: String,
    val v: Int,
    @SerialName("tenant_id") val tenantId: Int,
    @SerialName("tenant_slug") val tenantSlug: String,
    @SerialName("branch_id") val branchId: Int = 0,
    @SerialName("auth_kind") val authKind: String = "staff",
    val rooms: List<String> = emptyList(),
    val protocol: Int = RealtimeProtocol.VERSION,
)

@Serializable
data class AuthError(
    val op: String,
    val code: String,
    val message: String,
    val retryable: Boolean = false,
)

@Serializable
data class DomainEventEnvelope(
    val op: String,
    val v: Int,
    val event: DomainEvent,
)

@Serializable
data class DomainEvent(
    val v: Int,
    val id: String,
    val type: String,
    @SerialName("tenant_id") val tenantId: Int,
    @SerialName("branch_id") val branchId: Int? = null,
    @SerialName("occurred_at") val occurredAt: String,
    val actor: JsonObject? = null,
    val scope: JsonObject? = null,
    val data: JsonObject? = null,
)

sealed class RealtimeWireMessage {
    data class Authenticated(val authOk: AuthOk) : RealtimeWireMessage()
    data class Failed(val authError: AuthError) : RealtimeWireMessage()
    data class Event(val envelope: DomainEventEnvelope) : RealtimeWireMessage()
    data object Pong : RealtimeWireMessage()
}
