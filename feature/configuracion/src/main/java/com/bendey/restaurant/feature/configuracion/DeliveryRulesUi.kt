package com.bendey.restaurant.feature.configuracion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeySectionTitle
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextField

/**
 * «Pedido mínimo y tiempo de entrega» (D3): dos reglas opcionales del delivery del marketplace, apagadas por
 * defecto (0). Solo `s.m` las cambia; el resto las ve en solo lectura.
 */
@Composable
fun DeliveryRulesCard(viewModel: ConfiguracionViewModel) {
    val st by viewModel.rules.collectAsState()
    BendeyManagementCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xxs),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
            BendeySectionTitle(text = "Pedido mínimo y tiempo de entrega")
            Text(st.summary, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
            if (st.canEdit) {
                BendeyTextField(
                    value = st.minOrderText,
                    onValueChange = viewModel::setRulesMinOrder,
                    label = "Pedido mínimo del delivery (S/, sin contar el envío; 0 = sin mínimo)",
                    enabled = !st.saving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                BendeyTextField(
                    value = st.etaText,
                    onValueChange = viewModel::setRulesEta,
                    label = "Tiempo prometido de entrega (minutos; 0 = no prometer hora)",
                    enabled = !st.saving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                BendeyPrimaryButton(
                    text = if (st.saving) "Guardando…" else "Guardar",
                    onClick = viewModel::saveRules,
                    enabled = !st.saving && st.dirty,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (st.savedNotice) {
                    Text("Guardado.", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                }
            } else {
                Text(
                    "Solo el administrador puede cambiar estas reglas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            st.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = BendeyColors.ErrorText) }
        }
    }
}
