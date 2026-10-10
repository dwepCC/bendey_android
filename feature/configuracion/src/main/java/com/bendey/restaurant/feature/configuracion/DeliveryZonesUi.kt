package com.bendey.restaurant.feature.configuracion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeySectionTitle
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.delivery.DeliveryZone
import com.bendey.restaurant.core.domain.delivery.DeliveryZoneCopy
import com.bendey.restaurant.core.domain.delivery.DeliveryZoneRules
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyOption
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeySimpleSelect
import com.bendey.restaurant.core.ui.components.BendeySwitchRow
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.components.BendeyTextField

/**
 * «Zonas de reparto» (§26): tarifa, mínimo y hora prometida por distrito, y el interruptor «Repartir solo en estas
 * zonas». Solo `s.m` lo cambia; el resto lo ve en solo lectura.
 */
@Composable
fun DeliveryZonesCard(viewModel: ConfiguracionViewModel) {
    val st by viewModel.zones.collectAsState()
    BendeyManagementCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xxs),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
            BendeySectionTitle(text = DeliveryZoneCopy.SECTION_TITLE)
            Text(DeliveryZoneCopy.HELP, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
            BendeySwitchRow(
                label = DeliveryZoneCopy.RESTRICT_LABEL,
                checked = st.restrictToZones,
                onCheckedChange = viewModel::toggleZonesRestrict,
                enabled = st.canEdit && !st.saving && st.loaded,
            )
            Text(DeliveryZoneCopy.RESTRICT_HELP, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
            if (st.restrictWarning) {
                Text(DeliveryZoneCopy.RESTRICT_WARNING, style = MaterialTheme.typography.bodySmall, color = BendeyColors.WarningText)
            }
            if (st.zones.isEmpty()) {
                Text(
                    if (st.loading) "Cargando…" else DeliveryZoneCopy.EMPTY,
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            st.zones.forEach { z -> ZoneRow(z, st.canEdit && !st.saving, viewModel) }
            if (st.canEdit) {
                BendeyPrimaryButton(text = "+ Agregar zona", onClick = viewModel::openZoneCreate, enabled = !st.saving, modifier = Modifier.fillMaxWidth())
            } else {
                Text(DeliveryZoneCopy.READ_ONLY, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
            }
            st.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = BendeyColors.ErrorText) }
        }
    }
    ZoneFormDialog(viewModel)
    st.deleteTarget?.let { t ->
        BendeyAlertDialog(
            onDismissRequest = viewModel::dismissZoneDelete,
            title = "Quitar «${t.name}»",
            message = "Los pedidos de ese distrito volverán a usar la tarifa única" +
                (if (st.restrictToZones) " o, como repartes solo en tus zonas, ya no se podrá pedir delivery allí" else "") + ". ¿Quitar la zona?",
            confirmText = "Quitar",
            onConfirm = viewModel::confirmZoneDelete,
            onDismiss = viewModel::dismissZoneDelete,
            destructive = true,
        )
    }
}

@Composable
private fun ZoneRow(z: DeliveryZone, canEdit: Boolean, viewModel: ConfiguracionViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(z.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "Distrito ${z.ubigeoCode} · ${DeliveryZoneRules.describe(z)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            BendeySwitchRow(
                label = if (z.active) "Activa" else "Apagada",
                checked = z.active,
                onCheckedChange = { viewModel.toggleZoneActive(z, it) },
                enabled = canEdit,
                modifier = Modifier.weight(1f),
            )
        }
        if (canEdit) {
            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                BendeyTextButton(text = "Editar", onClick = { viewModel.openZoneEdit(z) })
                BendeyTextButton(text = "Quitar", onClick = { viewModel.askZoneDelete(z) }, textColor = BendeyColors.ErrorText)
            }
        }
    }
}

@Composable
private fun ZoneFormDialog(viewModel: ConfiguracionViewModel) {
    val st by viewModel.zones.collectAsState()
    val f = st.form ?: return
    BendeyFormDialog(
        onDismissRequest = viewModel::dismissZoneForm,
        title = if (f.editingId == null) "Nueva zona de reparto" else "Editar zona de reparto",
        confirmText = if (f.saving) "Guardando…" else "Guardar",
        onConfirm = viewModel::saveZoneForm,
        onDismiss = viewModel::dismissZoneForm,
        confirmEnabled = !f.saving,
        loading = f.saving,
        enableContentScroll = true,
        validationError = f.error,
    ) {
        if (f.editingId == null) {
            BendeySimpleSelect(
                options = f.regiones.map { BendeyOption(it.id, it.nombre) },
                selectedValue = f.regionId,
                onSelect = viewModel::selectZoneRegion,
                label = "Región",
                enabled = !f.saving,
            )
            BendeySimpleSelect(
                options = f.provincias.map { BendeyOption(it.id, it.nombre) },
                selectedValue = f.provinciaId,
                onSelect = viewModel::selectZoneProvincia,
                label = "Provincia",
                enabled = !f.saving && f.regionId.isNotBlank(),
            )
            BendeySimpleSelect(
                options = f.distritos.map { BendeyOption(it.id, it.nombre) },
                selectedValue = f.code,
                onSelect = viewModel::selectZoneDistrito,
                label = "Distrito",
                enabled = !f.saving && f.provinciaId.isNotBlank(),
            )
        } else {
            Text("Distrito ${f.code}", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
        }
        BendeyTextField(f.name, viewModel::setZoneName, "Nombre de la zona", enabled = !f.saving)
        BendeyTextField(
            f.feeText, viewModel::setZoneFee, DeliveryZoneCopy.FEE_LABEL,
            enabled = !f.saving, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        BendeyTextField(
            f.minOrderText, viewModel::setZoneMinOrder, DeliveryZoneCopy.MIN_LABEL,
            enabled = !f.saving, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        BendeyTextField(
            f.etaText, viewModel::setZoneEta, DeliveryZoneCopy.ETA_LABEL,
            enabled = !f.saving, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        BendeySwitchRow(label = "Activa", checked = f.active, onCheckedChange = viewModel::setZoneActive, enabled = !f.saving)
    }
}
