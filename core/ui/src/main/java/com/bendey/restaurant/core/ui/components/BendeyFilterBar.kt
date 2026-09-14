package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bendey.restaurant.core.designsystem.components.BendeyBadge
import com.bendey.restaurant.core.designsystem.components.BendeyBadgeVariant
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

/** Un filtro activo mostrado como chip removible — ej. "Estado: Anulado ×". */
data class BendeyActiveFilter(
    val key: String,
    val label: String,
    val onRemove: () -> Unit,
)

/**
 * Patrón estándar de filtros: Buscador → filtros primarios (2-3 chips, lo que quepa en una
 * fila) → "Más filtros" → lista. La auditoría de 2026 midió header+filtros consumiendo hasta
 * ~51% de la pantalla en algunos módulos (Caja·Movimientos) porque cada uno improvisaba su propio
 * bloque de filtros fijo; este componente reemplaza esas implementaciones ad-hoc.
 *
 * Deliberadamente NO incluye un botón grande y permanente de "Limpiar filtros" — según los
 * filtros activos, esa acción vive dentro de [BendeyFilterSheet] o como una acción secundaria
 * puntual (ver `onClearAll`, opcional y discreto, solo visible cuando hay algo que limpiar).
 *
 * No se fuerza que la barra quede fija fuera del `weight(1f)` de la lista: cada pantalla decide
 * si esta barra vive dentro del mismo flujo de scroll que el contenido (recomendado, salvo razón
 * de UX válida para fijarla — ej. una búsqueda que el usuario usa mientras lee resultados largos).
 */
@Composable
fun BendeyFilterBar(
    modifier: Modifier = Modifier,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchPlaceholder: String = "Buscar…",
    primaryFilters: (@Composable () -> Unit)? = null,
    onMoreFiltersClick: (() -> Unit)? = null,
    moreFiltersActiveCount: Int = 0,
    activeFilters: List<BendeyActiveFilter> = emptyList(),
    onClearAll: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
        BendeySearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = searchPlaceholder,
        )
        if (primaryFilters != null || onMoreFiltersClick != null) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                primaryFilters?.let {
                    item { Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) { it() } }
                }
                onMoreFiltersClick?.let { onClick ->
                    item {
                        BendeyFilterChip(
                            selected = moreFiltersActiveCount > 0,
                            onClick = onClick,
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier)
                                    Text("Más filtros", style = MaterialTheme.typography.labelLarge)
                                    if (moreFiltersActiveCount > 0) {
                                        BendeyBadge(
                                            text = moreFiltersActiveCount.toString(),
                                            color = BendeyColors.OnPrimary,
                                            containerColor = BendeyColors.Primary,
                                            variant = BendeyBadgeVariant.Filled,
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
        if (activeFilters.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                items(activeFilters, key = { it.key }) { filter ->
                    BendeyFilterChip(
                        selected = true,
                        onClick = filter.onRemove,
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
                            ) {
                                Text(filter.label, style = MaterialTheme.typography.labelLarge)
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Quitar filtro ${filter.label}",
                                    modifier = Modifier,
                                )
                            }
                        },
                    )
                }
                if (activeFilters.size > 1 && onClearAll != null) {
                    item {
                        BendeyTextButton(text = "Limpiar todo", onClick = onClearAll)
                    }
                }
            }
        }
    }
}
