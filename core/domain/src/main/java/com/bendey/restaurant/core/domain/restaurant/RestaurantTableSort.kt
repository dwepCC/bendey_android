package com.bendey.restaurant.core.domain.restaurant

/** Paridad con `compareTableName` / `sortRestaurantTables` en tableSort.ts */
fun compareTableName(a: String, b: String): Int {
    val regex = Regex("""(\d+|\D+)""")
    val partsA = regex.findAll(a).map { it.value }.toList()
    val partsB = regex.findAll(b).map { it.value }.toList()
    val max = maxOf(partsA.size, partsB.size)
    for (i in 0 until max) {
        val pa = partsA.getOrNull(i).orEmpty()
        val pb = partsB.getOrNull(i).orEmpty()
        if (pa == pb) continue
        val na = pa.toIntOrNull()
        val nb = pb.toIntOrNull()
        if (na != null && nb != null) {
            val cmp = na.compareTo(nb)
            if (cmp != 0) return cmp
            continue
        }
        val cmp = pa.compareTo(pb, ignoreCase = true)
        if (cmp != 0) return cmp
    }
    return a.compareTo(b, ignoreCase = true)
}

fun sortRestaurantTables(tables: List<RestaurantTable>, floors: List<Floor>): List<RestaurantTable> {
    if (tables.isEmpty()) return tables
    val floorOrder = floors.withIndex().associate { (index, floor) ->
        floor.id to (floor.sortOrder.takeIf { it != 0 } ?: index)
    }
    return tables.sortedWith { a, b ->
        val fa = floorOrder[a.floorId] ?: a.floorId ?: Int.MAX_VALUE
        val fb = floorOrder[b.floorId] ?: b.floorId ?: Int.MAX_VALUE
        if (fa != fb) return@sortedWith fa.compareTo(fb)
        val nameCmp = compareTableName(a.name, b.name)
        if (nameCmp != 0) return@sortedWith nameCmp
        a.id.compareTo(b.id)
    }
}
