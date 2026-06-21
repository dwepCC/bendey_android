package com.bendey.restaurant.feature.clientes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.domain.contacts.ContactDocType
import com.bendey.restaurant.core.domain.contacts.ContactFormInput
import com.bendey.restaurant.core.domain.contacts.CustomerContact
import com.bendey.restaurant.core.ui.components.BindSnackMessage
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyOption
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeySimpleSelect
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientesScreen(
    modifier: Modifier = Modifier,
    onShowMessage: (String) -> Unit = {},
    viewModel: ClientesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BindSnackMessage(
        message = state.snackMessage,
        onShow = onShowMessage,
        onConsume = viewModel::consumeSnackMessage,
    )

    PullToRefreshBox(
        isRefreshing = state.loading && state.contacts.isEmpty(),
        onRefresh = viewModel::refresh,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BendeyScreenToolbar(
                title = "Clientes",
                subtitle = "${state.contacts.size} clientes activos",
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                    IconButton(onClick = viewModel::openCreate) {
                        Icon(Icons.Default.Add, contentDescription = "Nuevo cliente")
                    }
                },
            )
            BendeyTextField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                label = "Buscar por nombre o documento",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            state.error?.takeIf { !state.formOpen }?.let { error ->
                Text(
                    error,
                    color = BendeyColors.Error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            if (state.contacts.isEmpty() && !state.loading) {
                Text(
                    "Sin clientes",
                    color = BendeyColors.OnSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.contacts, key = { it.id }) { contact ->
                        ContactRow(
                            contact = contact,
                            onEdit = { viewModel.openEdit(contact.id) },
                            onDelete = { viewModel.requestDelete(contact.id) },
                            onToggle = { viewModel.toggleActive(contact.id) },
                        )
                    }
                }
            }
        }
    }

    if (state.formOpen) {
        ContactFormDialog(
            form = state.form,
            loading = state.actionLoading,
            consulting = state.consulting,
            error = state.error,
            isEditing = state.editingContactId != null,
            onDismiss = viewModel::dismissForm,
            onFormChange = viewModel::updateForm,
            onConsult = viewModel::consultDocument,
            onSave = viewModel::saveContact,
        )
    }

    state.deleteContactId?.let {
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("Eliminar cliente") },
            text = { Text("¿Eliminar este cliente del catálogo?") },
            confirmButton = {
                BendeyPrimaryButton(text = "Eliminar", onClick = viewModel::confirmDelete)
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun ContactRow(
    contact: CustomerContact,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit,
) {
    BendeyManagementCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.displayName, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${contact.docLabel}: ${contact.docNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
                contact.tradeName?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = BendeyColors.OnSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    contact.phone?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = BendeyColors.OnSurfaceVariant)
                    }
                    contact.email?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = BendeyColors.OnSurfaceVariant)
                    }
                }
                if (!contact.active) {
                    BendeyStatusChip(label = "Inactivo", accentColor = BendeyColors.Warning)
                }
            }
            IconButton(onClick = onToggle) {
                Icon(
                    if (contact.active) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                    contentDescription = "Cambiar estado",
                    tint = if (contact.active) BendeyColors.Success else BendeyColors.OnSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = BendeyColors.Error)
            }
        }
    }
}

@Composable
private fun ContactFormDialog(
    form: ContactFormInput,
    loading: Boolean,
    consulting: Boolean,
    error: String?,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onFormChange: ((ContactFormInput) -> ContactFormInput) -> Unit,
    onConsult: () -> Unit,
    onSave: () -> Unit,
) {
    BendeyFormDialog(
        onDismissRequest = onDismiss,
        title = if (isEditing) "Editar cliente" else "Nuevo cliente",
        confirmText = if (loading) "Guardando…" else "Guardar",
        onConfirm = onSave,
        onDismiss = onDismiss,
        confirmEnabled = !loading && !consulting,
        loading = loading,
    ) {
        BendeySimpleSelect(
            options = ContactDocType.entries.map { BendeyOption(it.name, it.label) },
            selectedValue = form.docType.name,
            onSelect = { value ->
                val docType = ContactDocType.entries.firstOrNull { it.name == value } ?: ContactDocType.RUC
                onFormChange { it.copy(docType = docType) }
            },
            label = "Tipo de documento",
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BendeyTextField(
                value = form.docNumber,
                onValueChange = { value -> onFormChange { it.copy(docNumber = value) } },
                label = "N° documento *",
                modifier = Modifier.weight(1f),
            )
            if (ContactDocType.supportsConsulta(form.docType.code)) {
                BendeyPrimaryButton(
                    text = if (consulting) "…" else "Consultar",
                    onClick = onConsult,
                    enabled = !consulting && !loading,
                    modifier = Modifier.weight(0.55f),
                )
            }
        }
        BendeyTextField(
            value = form.businessName,
            onValueChange = { value -> onFormChange { it.copy(businessName = value) } },
            label = "Nombre / Razón social *",
        )
        BendeyTextField(
            value = form.tradeName,
            onValueChange = { value -> onFormChange { it.copy(tradeName = value) } },
            label = "Nombre comercial",
        )
        BendeyTextField(
            value = form.address,
            onValueChange = { value -> onFormChange { it.copy(address = value) } },
            label = "Dirección",
            singleLine = false,
        )
        BendeyTextField(
            value = form.ubigeo,
            onValueChange = { value -> onFormChange { it.copy(ubigeo = value.filter { c -> c.isDigit() }.take(6)) } },
            label = "Ubigeo (6 dígitos)",
        )
        BendeyTextField(
            value = form.phone,
            onValueChange = { value -> onFormChange { it.copy(phone = value) } },
            label = "Teléfono",
        )
        BendeyTextField(
            value = form.email,
            onValueChange = { value -> onFormChange { it.copy(email = value) } },
            label = "Email",
        )
        error?.let {
            Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
