package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

/**
 * Bottom sheet de "Más filtros" — segunda mitad del patrón Buscador → chips primarios →
 * [BendeyFilterSheet] → lista. Vive aquí (no como un botón grande y permanente) la acción
 * "Limpiar filtros": solo aparece cuando hay algo que limpiar (`onClear` no nulo), como texto
 * secundario junto al botón de aplicar — nunca como bloque propio.
 *
 * El contenido (`content`) queda libre: cada pantalla decide qué grupos de filtro mostrar
 * (rango de fechas, estado, categoría, repartidor…) — este componente solo estandariza el
 * contenedor, el encabezado y el pie.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BendeyFilterSheet(
    onDismissRequest: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Filtros",
    applyText: String = "Aplicar filtros",
    onClear: (() -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    BendeyBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(horizontal = BendeySpacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = BendeyColors.OnSurface,
                )
                onClear?.let {
                    BendeyTextButton(text = "Limpiar filtros", onClick = it)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = screenHeightDp * 0.55f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = BendeySpacing.sm, bottom = BendeySpacing.md),
                verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                content = content,
            )
            BendeyPrimaryButton(
                text = applyText,
                onClick = {
                    onApply()
                    onDismissRequest()
                },
                fillWidth = true,
                modifier = Modifier.padding(bottom = BendeySpacing.lg),
            )
        }
    }
}
