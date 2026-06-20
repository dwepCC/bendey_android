package com.bendey.restaurant.core.domain.restaurant

import com.bendey.restaurant.core.domain.products.PreparationArea

fun normalizePreparationAreaKey(area: String?): String {
    val raw = area?.trim().orEmpty()
    if (raw.isBlank()) return PreparationArea.COCINA.apiValue
    return PreparationArea.fromApi(raw).apiValue
}

fun collectPreparationAreas(items: List<KitchenItem>): List<String> =
    items.mapNotNull { it.preparationArea?.let(::normalizePreparationAreaKey) }
        .distinct()
        .sorted()

fun collectTableNames(items: List<KitchenItem>): List<String> =
    items.mapNotNull { it.tableName?.trim()?.takeIf { name -> name.isNotBlank() } }
        .distinct()
        .sorted()
