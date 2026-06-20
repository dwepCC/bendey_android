package com.bendey.restaurant.core.realtime.billing

data class BillingStatusEvent(
    val saleId: Int,
    val status: String,
    val pipelineStatus: String? = null,
    val sunatMessage: String? = null,
)

fun isBillingStatusTerminal(status: String): Boolean =
    status.equals("accepted", ignoreCase = true) ||
        status.equals("rejected", ignoreCase = true) ||
        status.equals("error", ignoreCase = true) ||
        status.equals("voided", ignoreCase = true)
