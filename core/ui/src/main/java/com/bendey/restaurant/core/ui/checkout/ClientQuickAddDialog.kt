package com.bendey.restaurant.core.ui.checkout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.contacts.ContactDocType
import com.bendey.restaurant.core.domain.contacts.ContactFormInput
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyOption
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeySimpleSelect
import com.bendey.restaurant.core.ui.components.BendeyTextField

/**
 * Alta rápida de cliente SIN salir de la venta — igual que `ClientQuickAddModal.tsx` en Bendey
 * Resto Tauri. A propósito trae solo 4 campos (documento, razón social, dirección), el mismo
 * subconjunto reducido que Tauri usa acá; el formulario completo (nombre comercial, ubigeo,
 * teléfono, email) sigue viviendo en Clientes, para cuando el usuario quiera completarlo después.
 */
@Composable
fun ClientQuickAddDialog(
    open: Boolean,
    form: ContactFormInput,
    saving: Boolean,
    consulting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onFormChange: ((ContactFormInput) -> ContactFormInput) -> Unit,
    onConsult: () -> Unit,
    onSave: () -> Unit,
) {
    if (!open) return
    BendeyFormDialog(
        onDismissRequest = onDismiss,
        title = "Nuevo cliente",
        confirmText = if (saving) "Guardando…" else "Guardar",
        dismissText = "Cancelar",
        confirmEnabled = !saving && !consulting,
        loading = saving,
        enableContentScroll = true,
        validationError = error,
        onConfirm = onSave,
        onDismiss = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
            BendeySimpleSelect(
                options = ContactDocType.entries.map { BendeyOption(it.name, it.label) },
                selectedValue = form.docType.name,
                onSelect = { value ->
                    val docType = ContactDocType.entries.firstOrNull { it.name == value } ?: ContactDocType.RUC
                    onFormChange { it.copy(docType = docType) }
                },
                label = "Tipo de documento",
            )
            BendeyTextField(
                value = form.docNumber,
                onValueChange = { value -> onFormChange { it.copy(docNumber = value) } },
                label = "N° documento *",
                modifier = Modifier.fillMaxWidth(),
            )
            if (ContactDocType.supportsConsulta(form.docType.code)) {
                BendeyPrimaryButton(
                    text = if (consulting) "Consultando…" else "Consultar",
                    onClick = onConsult,
                    enabled = !consulting && !saving,
                    loading = consulting,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            BendeyTextField(
                value = form.businessName,
                onValueChange = { value -> onFormChange { it.copy(businessName = value) } },
                label = "Nombre / Razón social *",
                modifier = Modifier.fillMaxWidth(),
            )
            BendeyTextField(
                value = form.address,
                onValueChange = { value -> onFormChange { it.copy(address = value) } },
                label = "Dirección",
                singleLine = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
