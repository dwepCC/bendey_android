package com.bendey.restaurant.feature.caja.propinas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeySectionTitle
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.delivery.RangedReportState
import com.bendey.restaurant.core.domain.delivery.ReportPreset
import com.bendey.restaurant.core.domain.delivery.ReportRangeRules
import com.bendey.restaurant.core.ui.components.BendeyDateField
import com.bendey.restaurant.core.ui.components.BendeyOutlinedButton

/** Piezas visuales compartidas por Propinas y Reparto del recargo (mismo estilo que el reporte de delivery). */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> RangePicker(
    state: RangedReportState<T>,
    onPreset: (ReportPreset) -> Unit,
    onFrom: (String) -> Unit,
    onTo: (String) -> Unit,
    onApplyCustom: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.s4),
        ) {
            ReportPreset.entries.forEach { p ->
                BendeyFilterChip(selected = state.preset == p, onClick = { onPreset(p) }, text = p.label)
            }
        }
        if (state.preset == ReportPreset.CUSTOM) {
            BendeyDateField(value = state.fromText, onValueChange = onFrom, label = "Desde")
            BendeyDateField(value = state.toText, onValueChange = onTo, label = "Hasta")
            Muted("Máximo ${ReportRangeRules.MAX_DAYS} días.")
            BendeyOutlinedButton(text = "Consultar", onClick = onApplyCustom, enabled = !state.loading)
        }
        state.shownRange?.let { r ->
            Muted(
                if (r.days == 1) "Del ${ReportRangeRules.display(r.from)}"
                else "Del ${ReportRangeRules.display(r.from)} al ${ReportRangeRules.display(r.to)}",
            )
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    BendeyManagementCard(modifier = modifier) {
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = BendeyColors.OnSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = BendeyColors.OnSurface)
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    BendeyManagementCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
            BendeySectionTitle(text = title)
            content()
        }
    }
}

@Composable
fun LineRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = BendeyColors.OnSurface,
            modifier = Modifier.weight(0.45f),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, color = BendeyColors.OnSurface, modifier = Modifier.weight(0.55f))
    }
}

@Composable
fun Muted(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
}
