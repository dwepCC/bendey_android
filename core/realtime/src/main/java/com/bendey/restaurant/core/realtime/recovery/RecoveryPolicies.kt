package com.bendey.restaurant.core.realtime.recovery

/** Políticas de recuperación HTTP — REALTIME_FRONTEND_ARCHITECTURE.md §4. Puerto de `recovery/policies.ts` (Tauri). */
enum class RecoveryPolicy { ENTITY, PARTIAL, DOMAIN, BRANCH, FULL }

data class RecoveryScope(
    val domain: String? = null,
    val slice: String? = null,
    val entity: String? = null,
    val entityId: Int? = null,
    val branchId: Int? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
)

data class RecoveryRequest(
    val policy: RecoveryPolicy,
    val scope: RecoveryScope? = null,
    val reason: String? = null,
)

/** Matriz documental — ejecución real desde Fase C+. */
fun selectRecoveryPolicy(trigger: String): RecoveryPolicy = when (trigger) {
    "patch_missing" -> RecoveryPolicy.ENTITY
    "payload_insufficient" -> RecoveryPolicy.PARTIAL
    "schema_unsupported", "domain_stale" -> RecoveryPolicy.DOMAIN
    "branch_changed" -> RecoveryPolicy.BRANCH
    "login", "reconnect_long_gap" -> RecoveryPolicy.FULL
    else -> RecoveryPolicy.PARTIAL
}
