package com.bendey.restaurant.feature.cocina

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bendey.restaurant.core.domain.restaurant.KitchenItem
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

fun kitchenElapsedLabel(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    val opened = runCatching { Instant.parse(iso) }.getOrElse {
        runCatching { OffsetDateTime.parse(iso).toInstant() }.getOrElse {
            runCatching {
                LocalDateTime.parse(iso.replace(" ", "T").substringBefore("+").substringBefore("Z"))
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
            }.getOrNull()
        }
    } ?: return ""
    val minutes = ChronoUnit.MINUTES.between(opened, Instant.now()).coerceAtLeast(0)
    if (minutes < 60) return "${minutes} min"
    val hours = minutes / 60
    return "${hours}h ${minutes % 60}m"
}

fun KitchenItem.kitchenOpenedAt(): String? =
    sessionOpenedAt?.takeIf { it.isNotBlank() } ?: createdAt?.takeIf { it.isNotBlank() }

@Composable
fun rememberKitchenElapsed(iso: String?): String {
    var label by remember(iso) { mutableStateOf(kitchenElapsedLabel(iso)) }
    LaunchedEffect(iso) {
        if (iso.isNullOrBlank()) return@LaunchedEffect
        while (true) {
            label = kitchenElapsedLabel(iso)
            delay(30_000)
        }
    }
    return label
}
