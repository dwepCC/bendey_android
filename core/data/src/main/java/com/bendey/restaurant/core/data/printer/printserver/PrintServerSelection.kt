package com.bendey.restaurant.core.data.printer.printserver

import kotlinx.serialization.Serializable

@Serializable
data class PrintServerSelection(
    val serverId: String = "",
    val displayName: String = "",
    val host: String = "",
    val port: Int = DEFAULT_PRINT_SERVER_PORT,
    val tenant: String = "",
    val branchName: String = "",
    val branchId: Int = 0,
    val hostname: String = "",
    val appVersion: String = "",
    /** IP/host manual (opción avanzada). */
    val manualHost: String = "",
) {
    fun resolvedHost(): String = manualHost.trim().ifBlank { host.trim() }

    fun isReady(): Boolean = resolvedHost().isNotBlank() && port in 1025..65535
}

const val DEFAULT_PRINT_SERVER_PORT = 19_280
const val PRINT_SERVER_SERVICE_TYPE = "_bendey-print._tcp."
