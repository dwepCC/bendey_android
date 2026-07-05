package com.bendey.restaurant.core.realtime.billing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BillingEventsUrlTest {

    @Test
    fun buildUrl_includesAccessTokenAndTenantSlug() {
        val url = buildBillingEventsUrl(
            baseUrl = "https://angel.bendey.cloud",
            token = "jwt.token.here",
            tenantSlug = "angel",
        )
        assertTrue(url!!.contains("/api/billing/events?access_token="))
        assertTrue(url.contains("tenant_slug=angel"))
    }

    @Test
    fun buildUrl_omitsTenantSlugWhenBlank() {
        val url = buildBillingEventsUrl(
            baseUrl = "https://angel.bendey.cloud",
            token = "jwt",
            tenantSlug = "  ",
        )
        assertEquals("https://angel.bendey.cloud/api/billing/events?access_token=jwt", url)
    }

    @Test
    fun buildUrl_returnsNullWhenMissingCredentials() {
        assertNull(buildBillingEventsUrl("", "jwt", "angel"))
        assertNull(buildBillingEventsUrl("https://x.test", "", "angel"))
    }
}
