package com.bendey.restaurant.core.realtime

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull

/** Puerto de src/lib/realtime/schema.ts (Tauri) — REALTIME_PROTOCOL.md §8.2/§9. */
object RealtimeSchema {

    private val SUPPORTED_SCHEMA_VERSIONS = setOf(1)

    /** Schema version canónico BRP — event.v. */
    fun getSchemaVersion(event: DomainEvent): Int {
        val v = event.v
        return if (v > 0) v else 1
    }

    fun isSupportedSchemaVersion(version: Int): Boolean = version in SUPPORTED_SCHEMA_VERSIONS

    /** Dominio desde type: restaurant.session.opened -> restaurant */
    fun resolveDomainFromEventType(type: String): String {
        val t = type.trim()
        if (t.isEmpty()) return ""
        val dot = t.indexOf('.')
        return if (dot == -1) t else t.substring(0, dot)
    }

    /** scope + data para IDs de UI. */
    fun readEventScopeId(event: DomainEvent, key: String): Int? {
        return numField(event.scope, key) ?: numField(event.data, key)
    }

    fun readEventScopeString(event: DomainEvent, key: String): String? {
        return strField(event.scope, key) ?: strField(event.data, key)
    }

    fun readEventScopeDouble(event: DomainEvent, key: String): Double? {
        return doubleField(event.scope, key) ?: doubleField(event.data, key)
    }

    fun readEventScopeBoolean(event: DomainEvent, key: String): Boolean? {
        return boolField(event.scope, key) ?: boolField(event.data, key)
    }

    /** Lee únicamente de `event.data` (sin fallback a `scope`) — paridad con `readNum`/`readStr` (Tauri handlers.ts). */
    fun readDataDouble(event: DomainEvent, key: String): Double? = doubleField(event.data, key)

    fun readDataString(event: DomainEvent, key: String): String? = strField(event.data, key)

    fun readDataBoolean(event: DomainEvent, key: String): Boolean? = boolField(event.data, key)

    private fun primitive(obj: JsonObject?, key: String): JsonPrimitive? {
        return obj?.get(key) as? JsonPrimitive
    }

    private fun numField(obj: JsonObject?, key: String): Int? =
        doubleField(obj, key)?.toInt()

    private fun doubleField(obj: JsonObject?, key: String): Double? {
        val p = primitive(obj, key) ?: return null
        return p.doubleOrNull
    }

    private fun strField(obj: JsonObject?, key: String): String? {
        val p = primitive(obj, key) ?: return null
        if (!p.isString) return null
        val content = p.content
        return content.ifBlank { null }
    }

    private fun boolField(obj: JsonObject?, key: String): Boolean? {
        val p = primitive(obj, key) ?: return null
        return p.booleanOrNull
    }
}
