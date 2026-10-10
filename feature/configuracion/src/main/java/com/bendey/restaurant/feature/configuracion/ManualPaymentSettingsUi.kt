package com.bendey.restaurant.feature.configuracion

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeySectionTitle
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.delivery.ManualPaymentRules
import com.bendey.restaurant.core.domain.delivery.PayMethod
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyOption
import com.bendey.restaurant.core.ui.components.BendeyOutlinedButton
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeySimpleSelect
import com.bendey.restaurant.core.ui.components.BendeySwitchRow
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.components.BendeyTextField

/**
 * «Pago con captura» (D2a): interruptor, minutos para pagar y medios (Yape, Plin, transferencia...).
 * Solo `s.m` lo cambia; el resto lo ve en solo lectura. Apagado por defecto.
 */
@Composable
fun ManualPaymentCard(viewModel: ConfiguracionViewModel) {
    val st by viewModel.manualPay.collectAsState()
    BendeyManagementCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xxs),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
            BendeySectionTitle(text = "Pago con captura")
            BendeySwitchRow(
                label = "Cobrar por adelantado con Yape, Plin o transferencia",
                checked = st.enabled,
                onCheckedChange = viewModel::toggleManualPay,
                enabled = st.canEdit && !st.saving,
            )
            Text(
                "El cliente paga y sube la captura desde su app; tú la validas en Entregas > Pagos y recién entonces el pedido pasa a cocina. " +
                    "Necesitas al menos un medio activo.",
                style = MaterialTheme.typography.bodySmall,
                color = BendeyColors.OnSurfaceVariant,
            )
            Text(st.summary, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
            if (st.canEdit) {
                Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    BendeyTextField(
                        value = st.minutesText,
                        onValueChange = viewModel::setManualPayMinutes,
                        label = "Minutos para pagar (5 a 120)",
                        enabled = !st.saving,
                        fillWidth = false,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    BendeyOutlinedButton(text = "Guardar", onClick = viewModel::saveManualPayMinutes, enabled = !st.saving)
                }
            } else {
                Text(
                    "Solo el administrador puede cambiar el pago con captura.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            st.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = BendeyColors.ErrorText) }

            BendeySectionTitle(text = "Medios de pago")
            if (st.methods.isEmpty()) {
                Text("Aún no ofreces ningún medio.", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
            }
            st.methods.forEach { m ->
                PayMethodRow(m, st.canEdit && !st.saving, viewModel)
            }
            if (st.canEdit) {
                BendeyPrimaryButton(text = "+ Agregar medio", onClick = viewModel::openPayMethodCreate, enabled = !st.saving)
            }
        }
    }
    PayMethodFormDialog(viewModel)
    st.deleteTarget?.let { t ->
        BendeyAlertDialog(
            onDismissRequest = viewModel::dismissPayMethodDelete,
            title = "Quitar «${t.label}»",
            message = "Los pagos ya hechos con este medio se conservan. ¿Quitarlo de la lista?",
            confirmText = "Quitar",
            onConfirm = viewModel::confirmPayMethodDelete,
            onDismiss = viewModel::dismissPayMethodDelete,
            destructive = true,
        )
    }
}

@Composable
private fun PayMethodRow(m: PayMethod, canEdit: Boolean, viewModel: ConfiguracionViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(m.label, fontWeight = FontWeight.SemiBold)
                Text(
                    listOf(m.methodCode, m.holder, m.accountNumber).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
                if (m.qrUrl.isNotBlank()) {
                    Text("Con imagen QR", style = MaterialTheme.typography.labelSmall, color = BendeyColors.OnSurfaceVariant)
                }
            }
            BendeySwitchRow(
                label = if (m.active) "Activo" else "Apagado",
                checked = m.active,
                onCheckedChange = { viewModel.togglePayMethodActive(m, it) },
                enabled = canEdit,
                modifier = Modifier.weight(1f),
            )
        }
        if (canEdit) {
            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                BendeyTextButton(text = "Editar", onClick = { viewModel.openPayMethodEdit(m) })
                BendeyTextButton(text = "Quitar", onClick = { viewModel.askPayMethodDelete(m) }, textColor = BendeyColors.ErrorText)
            }
        }
    }
}

@Composable
private fun PayMethodFormDialog(viewModel: ConfiguracionViewModel) {
    val st by viewModel.manualPay.collectAsState()
    val f = st.form ?: return
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        if (bytes != null) viewModel.pickPayMethodQr(bytes, mime)
    }
    val options = st.codeOptionsFor(f.editingId).map { BendeyOption(it.code, it.name) }
    BendeyFormDialog(
        onDismissRequest = viewModel::dismissPayMethodForm,
        title = if (f.editingId == null) "Nuevo medio de pago" else "Editar medio de pago",
        confirmText = if (f.saving) "Guardando…" else "Guardar",
        onConfirm = viewModel::savePayMethodForm,
        onDismiss = viewModel::dismissPayMethodForm,
        confirmEnabled = !f.saving,
        loading = f.saving,
        enableContentScroll = true,
        validationError = f.error,
    ) {
        if (f.editingId == null) {
            if (options.isEmpty()) {
                Text(
                    "No hay medios de pago sin usar en Caja y Bancos. Registra primero Yape, Plin o transferencia en Caja y Bancos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            } else {
                BendeySimpleSelect(
                    options = options,
                    selectedValue = f.methodCode,
                    onSelect = { code ->
                        viewModel.updatePayMethodForm {
                            it.copy(methodCode = code, label = it.label.ifBlank { options.firstOrNull { o -> o.value == code }?.label.orEmpty() })
                        }
                    },
                    label = "Medio de Caja y Bancos (donde entra la plata)",
                    enabled = !f.saving,
                )
            }
        } else {
            Text("Medio de Caja y Bancos: ${f.methodCode}", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
        }
        BendeyTextField(f.label, { v -> viewModel.updatePayMethodForm { it.copy(label = v.take(100)) } }, "Nombre que ve el cliente", enabled = !f.saving)
        BendeyTextField(f.holder, { v -> viewModel.updatePayMethodForm { it.copy(holder = v.take(120)) } }, "Titular", enabled = !f.saving)
        BendeyTextField(
            f.accountNumber,
            { v -> viewModel.updatePayMethodForm { it.copy(accountNumber = v.take(60)) } },
            "Celular o número de cuenta",
            enabled = !f.saving,
        )
        BendeyTextField(f.cci, { v -> viewModel.updatePayMethodForm { it.copy(cci = v.take(40)) } }, "CCI (opcional)", enabled = !f.saving)
        BendeyTextField(
            f.instructions,
            { v -> viewModel.updatePayMethodForm { it.copy(instructions = v.take(255)) } },
            "Indicaciones (opcional)",
            enabled = !f.saving,
        )
        BendeySwitchRow(
            label = "Activo",
            checked = f.active,
            onCheckedChange = { v -> viewModel.updatePayMethodForm { it.copy(active = v) } },
            enabled = !f.saving,
        )
        Text("Imagen QR (opcional)", style = MaterialTheme.typography.labelMedium)
        val preview: Any? = f.pendingQr?.bytes
            ?: f.existingQrUrl.takeIf { it.isNotBlank() }?.let { ManualPaymentRules.resolveAssetUrl(viewModel.assetsBaseUrl, it) }
        if (preview != null) {
            Box(
                Modifier
                    .size(160.dp)
                    .clip(BendeyShapeTokens.xs)
                    .background(BendeyColors.SurfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = preview,
                    contentDescription = "Vista previa del QR",
                    modifier = Modifier.size(160.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
            BendeyOutlinedButton(
                text = if (preview == null) "Elegir imagen" else "Cambiar imagen",
                onClick = { picker.launch("image/*") },
                enabled = !f.saving,
            )
            if (f.pendingQr != null) {
                BendeyTextButton(text = "Quitar selección", onClick = viewModel::clearPayMethodQr, enabled = !f.saving)
            }
        }
    }
}
