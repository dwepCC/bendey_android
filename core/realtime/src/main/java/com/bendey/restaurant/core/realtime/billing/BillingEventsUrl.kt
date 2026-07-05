package com.bendey.restaurant.core.realtime.billing

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** URL SSE billing — paridad con useBillingEvents.ts (Tauri). */
internal fun buildBillingEventsUrl(
    baseUrl: String,
    token: String,
    tenantSlug: String?,
): String? {
    val base = baseUrl.trim().trimEnd('/')
    val tok = token.trim()
    if (base.isBlank() || tok.isBlank()) return null

    val encodedToken = URLEncoder.encode(tok, StandardCharsets.UTF_8)
    val slug = tenantSlug?.trim().orEmpty()
    return buildString {
        append(base)
        append("/api/billing/events?access_token=")
        append(encodedToken)
        if (slug.isNotBlank()) {
            append("&tenant_slug=")
            append(URLEncoder.encode(slug, StandardCharsets.UTF_8))
        }
    }
}
