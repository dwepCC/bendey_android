package com.bendey.restaurant.core.network.serialization

import kotlinx.serialization.json.Json

val ApiJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
    explicitNulls = false
}
