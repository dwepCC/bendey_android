package com.bendey.restaurant.core.domain.restaurant

import com.bendey.restaurant.core.domain.catalog.normalizePreparationAreaName

fun normalizePreparationAreaKey(area: String?): String? = normalizePreparationAreaName(area)

fun collectPreparationAreas(items: List<KitchenItem>): List<String> =
    items.mapNotNull { normalizePreparationAreaKey(it.preparationArea) }
        .distinct()
        .sorted()

fun collectTableNames(items: List<KitchenItem>): List<String> =
    items.mapNotNull { it.tableName?.trim()?.takeIf { name -> name.isNotBlank() } }
        .distinct()
        .sorted()
