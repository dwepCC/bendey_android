package com.bendey.restaurant.core.domain.catalog

data class PreparationAreaItem(
    val id: Int,
    val name: String,
    val description: String,
    val color: String,
    val estimatedMinutes: Int,
    val sortOrder: Int,
    val active: Boolean,
)

data class PreparationAreaFormInput(
    val name: String = "",
    val description: String = "",
    val color: String = DEFAULT_PREPARATION_AREA_COLORS.first(),
    val estimatedMinutes: String = "0",
    val sortOrder: String = "0",
    val active: Boolean = true,
)

val DEFAULT_PREPARATION_AREA_COLORS = listOf(
    "#EF4444",
    "#F97316",
    "#EAB308",
    "#22C55E",
    "#06B6D4",
    "#3B82F6",
    "#8B5CF6",
    "#EC4899",
)

fun normalizePreparationAreaName(name: String?): String? =
    name?.trim()?.lowercase()?.takeIf { it.isNotBlank() }

fun PreparationAreaItem.normalizedName(): String = normalizePreparationAreaName(name).orEmpty()

fun PreparationAreaItem.toFormInput() = PreparationAreaFormInput(
    name = name,
    description = description,
    color = color.ifBlank { DEFAULT_PREPARATION_AREA_COLORS.first() },
    estimatedMinutes = estimatedMinutes.toString(),
    sortOrder = sortOrder.toString(),
    active = active,
)

fun preparationAreaDisplayLabel(areaKey: String, areas: List<PreparationAreaItem> = emptyList()): String {
    val normalized = normalizePreparationAreaName(areaKey).orEmpty()
    if (normalized.isBlank()) return "Sin área"
    return areas.firstOrNull { it.normalizedName() == normalized }?.name
        ?: areaKey.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
