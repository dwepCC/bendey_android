package com.bendey.restaurant.core.realtime.dispatcher

import com.bendey.restaurant.core.realtime.DomainEvent
import com.bendey.restaurant.core.realtime.RealtimeSchema

/** Puerto de `dispatcher/validate.ts` (Tauri). */

sealed class ValidateResult {
    data object Ok : ValidateResult()
    data class Fail(val reason: String) : ValidateResult()
}

data class ValidateContext(
    val activeBranchId: Int? = null,
    val activeTenantId: Int? = null,
)

object RealtimeValidate {
    fun validateEvent(event: DomainEvent, ctx: ValidateContext): ValidateResult {
        if (event.type.isBlank()) return ValidateResult.Fail("empty_type")
        if (!RealtimeSchema.isSupportedSchemaVersion(RealtimeSchema.getSchemaVersion(event))) {
            return ValidateResult.Fail("unsupported_schema")
        }
        if (ctx.activeTenantId != null && event.tenantId != ctx.activeTenantId) {
            return ValidateResult.Fail("wrong_tenant")
        }
        val branchId = event.branchId
        if (ctx.activeBranchId != null && ctx.activeBranchId > 0 &&
            branchId != null && branchId > 0 && branchId != ctx.activeBranchId
        ) {
            return ValidateResult.Fail("wrong_branch")
        }
        return ValidateResult.Ok
    }
}
