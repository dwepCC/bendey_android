package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.domain.products.CatalogSection

@Composable
fun CatalogSectionNav(
    current: CatalogSection,
    onOpenProductos: () -> Unit,
    onOpenModificadores: () -> Unit,
    onOpenAreasPreparacion: () -> Unit = {},
    onOpenCombos: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = current == CatalogSection.PRODUCTOS,
            onClick = onOpenProductos,
            enabled = current != CatalogSection.PRODUCTOS,
            label = { Text(CatalogSection.PRODUCTOS.label) },
        )
        FilterChip(
            selected = current == CatalogSection.MODIFICADORES,
            onClick = onOpenModificadores,
            enabled = current != CatalogSection.MODIFICADORES,
            label = { Text(CatalogSection.MODIFICADORES.label) },
        )
        FilterChip(
            selected = current == CatalogSection.AREAS_PREPARACION,
            onClick = onOpenAreasPreparacion,
            enabled = current != CatalogSection.AREAS_PREPARACION,
            label = { Text(CatalogSection.AREAS_PREPARACION.label) },
        )
        FilterChip(
            selected = current == CatalogSection.COMBOS,
            onClick = onOpenCombos,
            enabled = current != CatalogSection.COMBOS,
            label = { Text(CatalogSection.COMBOS.label) },
        )
    }
}
