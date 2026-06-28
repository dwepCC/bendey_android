package com.bendey.restaurant.core.data.export

sealed class ExportShareResult {
    data object Success : ExportShareResult()

    data class Failure(
        val userMessage: String,
        val cause: Throwable? = null,
    ) : ExportShareResult()
}
