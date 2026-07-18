package com.bendey.restaurant.core.realtime.dispatcher

import com.bendey.restaurant.core.realtime.DomainEvent
import kotlinx.serialization.json.JsonObject
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

/** Puerto de `validate.test.ts` (Tauri) — cubre wrong_tenant/wrong_branch. */
class RealtimeValidateTest {

    private fun buildEvent(tenantId: Int = 1, branchId: Int? = 1, type: String = "restaurant.order.created"): DomainEvent =
        DomainEvent(
            v = 1,
            id = "evt-1",
            type = type,
            tenantId = tenantId,
            branchId = branchId,
            occurredAt = Instant.now().toString(),
            scope = JsonObject(emptyMap()),
        )

    @Test
    fun rejectsEventFromAnotherTenantWhenActiveTenantIdIsKnown() {
        val result = RealtimeValidate.validateEvent(buildEvent(tenantId = 2), ValidateContext(activeTenantId = 1))
        assertEquals(ValidateResult.Fail("wrong_tenant"), result)
    }

    @Test
    fun acceptsEventFromActiveTenant() {
        val result = RealtimeValidate.validateEvent(buildEvent(tenantId = 1), ValidateContext(activeTenantId = 1))
        assertEquals(ValidateResult.Ok, result)
    }

    @Test
    fun doesNotRejectOnTenantWhenActiveTenantIdIsUnset() {
        val result = RealtimeValidate.validateEvent(buildEvent(tenantId = 2), ValidateContext())
        assertEquals(ValidateResult.Ok, result)
    }

    @Test
    fun rejectsEventFromAnotherBranchWhenActiveBranchIdIsKnown() {
        val result = RealtimeValidate.validateEvent(buildEvent(branchId = 2), ValidateContext(activeBranchId = 1))
        assertEquals(ValidateResult.Fail("wrong_branch"), result)
    }
}
