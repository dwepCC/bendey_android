package com.bendey.restaurant.feature.clientes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyIconButton
import com.bendey.restaurant.core.ui.components.BendeyListRow
import com.bendey.restaurant.core.ui.components.BendeyListRowAction
import com.bendey.restaurant.core.ui.components.BendeyListRowSubtitle
import com.bendey.restaurant.core.ui.components.BendeyListRowTitle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.contacts.ContactDocType
import com.bendey.restaurant.core.domain.contacts.ContactFormInput
import com.bendey.restaurant.core.domain.contacts.CustomerContact
import com.bendey.restaurant.core.ui.components.BendeySnackMessage
import com.bendey.restaurant.core.ui.components.BendeyEmptyState
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyOption
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.components.BendeySimpleSelect
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.components.BendeyLazyColumn
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.components.BendeyVerticalScrollColumn
import com.bendey.restaurant.core.ui.layout.BendeyFlexibleContentSlot
import com.bendey.restaurant.core.ui.layout.rememberBendeyBottomBarScrollPadding
import com.bendey.restaurant.core.ui.layout.BendeyListScreenLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientesScreen(
    modifier: Modifier = Modifier,
    onShowMessage: (String) -> Unit = {},
    viewModel: ClientesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BendeySnackMessage(
        message = state.snackMessage,
        onShow = onShowMessage,
        onConsume = viewModel::consumeSnackMessage,
    )

    BendeyListScreenLayout(
            modifier = modifier.fillMaxSize(),
            isRefreshing = state.loading && state.contacts.isEmpty(),
            onRefresh = viewModel::refresh,
            header = {
                BendeyScreenToolbar(
                    title = "Clientes",
                    subtitle = clientesSubtitle(state),
                    actions = {
                        BendeyIconButton(
                            onClick = viewModel::refresh,
                            icon = Icons.Default.Refresh,
                            contentDescription = "Actualizar",
                        )
                        BendeyIconButton(
                            onClick = viewModel::openCreate,
                            icon = Icons.Default.Add,
                            contentDescription = "Nuevo cliente",
                        )
                    },
                )
            },
        ) { contentModifier ->
            ClientesListPane(
                state = state,
                onEdit = viewModel::openEdit,
                onDelete = viewModel::requestDelete,
                onToggle = viewModel::toggleActive,
                onSearchChange = viewModel::setSearchQuery,
                onShowInactiveChange = viewModel::setShowInactive,
                modifier = contentModifier,
            )
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
        BendeyAlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = "Eliminar cliente",
            message = "¿Eliminar este cliente del catálogo?",
            onConfirm = viewModel::confirmDelete,
            confirmText = "Eliminar",
            destructive = true,
        )
    }
}

private fun clientesSubtitle(state: ClientesUiState): String {
    val count = state.contacts.size
    return when {
        state.showInactive -> if (count == 1) "1 cliente" else "$count clientes"
        count == 1 -> "1 cliente activo"
        else -> "$count clientes activos"
    }
}

@Composable
private fun ClientesListPane(
    state: ClientesUiState,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onToggle: (Int) -> Unit,
    onSearchChange: (String) -> Unit,
    onShowInactiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val bottomScrollPadding = rememberBendeyBottomBarScrollPadding()
    Column(modifier = modifier.fillMaxSize()) {
        BendeyTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            label = "Buscar por nombre o documento",
            modifier = Modifier.padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xs),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
        ) {
            BendeyFilterChip(
                selected = !state.showInactive,
                onClick = { onShowInactiveChange(false) },
                text = "Activos",
            )
            BendeyFilterChip(
                selected = state.showInactive,
                onClick = { onShowInactiveChange(true) },
                text = "Incluir inactivos",
            )
        }
        state.error?.takeIf { !state.formOpen }?.let { error ->
            Text(
                error,
                color = BendeyColors.Error,
                modifier = Modifier.padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xxs),
            )
        }
        BendeyFlexibleContentSlot {
            if (state.contacts.isEmpty() && !state.loading) {
                BendeyEmptyState(
                    title = "Sin clientes",
                    inline = true,
                    modifier = Modifier.align(Alignment.TopStart),
                )
            } else {
                BendeyLazyColumn(
                    modifier = it,
                    state = listState,
                    contentPadding = PaddingValues(
                        start = BendeySpacing.md,
                        end = BendeySpacing.md,
                        top = BendeySpacing.md,
                        bottom = BendeySpacing.md + bottomScrollPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                ) {
                    items(state.contacts, key = { it.id }) { contact ->
                        ContactRow(
                            contact = contact,
                            selected = contact.id == state.editingContactId && state.formOpen,
                            onEdit = { onEdit(contact.id) },
                            onDelete = { onDelete(contact.id) },
                            onToggle = { onToggle(contact.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactFormPane(
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
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            if (isEditing) "Editar cliente" else "Nuevo cliente",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = BendeySpacing.sm),
        )
        BendeyVerticalScrollColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
        ) {
            ContactFormFields(
                form = form,
                loading = loading,
                consulting = consulting,
                error = error,
                onFormChange = onFormChange,
                onConsult = onConsult,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = BendeySpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
        ) {
            BendeyTextButton(
                text = "Cancelar",
                onClick = onDismiss,
                enabled = !loading && !consulting,
                modifier = Modifier.weight(1f),
            )
            BendeyPrimaryButton(
                text = if (loading) "Guardando…" else "Guardar",
                onClick = onSave,
                enabled = !loading && !consulting,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ContactFormFields(
    form: ContactFormInput,
    loading: Boolean,
    consulting: Boolean,
    error: String?,
    onFormChange: ((ContactFormInput) -> ContactFormInput) -> Unit,
    onConsult: () -> Unit,
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
    BendeyTextField(
        value = form.docNumber,
        onValueChange = { value -> onFormChange { it.copy(docNumber = value) } },
        label = "N° documento *",
        modifier = Modifier.fillMaxWidth(),
    )
    if (ContactDocType.supportsConsulta(form.docType.code)) {
        BendeyPrimaryButton(
            text = when {
                consulting -> "Validando…"
                else -> "Validar"
            },
            onClick = onConsult,
            enabled = !consulting && !loading,
            loading = consulting,
            modifier = Modifier.fillMaxWidth(),
        )
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

// Patrón de referencia usado para diseñar BendeyListRow — ahora lo consume directamente.
@Composable
private fun ContactRow(
    contact: CustomerContact,
    selected: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit,
) {
    BendeyListRow(
        selected = selected,
        actions = listOf(
            BendeyListRowAction(
                icon = if (contact.active) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                contentDescription = "Cambiar estado",
                onClick = onToggle,
            ),
            BendeyListRowAction(Icons.Default.Edit, "Editar", onEdit),
            BendeyListRowAction(Icons.Default.Delete, "Eliminar", onDelete, destructive = true),
        ),
    ) {
        BendeyListRowTitle(contact.displayName, maxLines = 2)
        BendeyListRowSubtitle("${contact.docLabel}: ${contact.docNumber}")
        contact.tradeName?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = BendeyColors.OnSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
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
        enableContentScroll = true,
    ) {
        ContactFormFields(
            form = form,
            loading = loading,
            consulting = consulting,
            error = error,
            onFormChange = onFormChange,
            onConsult = onConsult,
        )
    }
}
