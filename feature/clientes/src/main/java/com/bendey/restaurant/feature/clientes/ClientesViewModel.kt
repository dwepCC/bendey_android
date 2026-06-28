package com.bendey.restaurant.feature.clientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.contacts.ContactDocType
import com.bendey.restaurant.core.domain.contacts.ContactFormInput
import com.bendey.restaurant.core.domain.contacts.ContactsRepository
import com.bendey.restaurant.core.domain.contacts.CustomerContact
import com.bendey.restaurant.core.domain.contacts.sanitizeDocNumber
import com.bendey.restaurant.core.domain.contacts.toFormInput
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.session.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClientesUiState(
    val loading: Boolean = false,
    val actionLoading: Boolean = false,
    val consulting: Boolean = false,
    val contacts: List<CustomerContact> = emptyList(),
    val searchQuery: String = "",
    val showInactive: Boolean = false,
    val tenantRuc: String = "",
    val formOpen: Boolean = false,
    val editingContactId: Int? = null,
    val form: ContactFormInput = ContactFormInput(),
    val deleteContactId: Int? = null,
    val error: String? = null,
    val snackMessage: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class ClientesViewModel @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val sessionStore: UserSessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientesUiState())
    val uiState: StateFlow<ClientesUiState> = _uiState.asStateFlow()

    private val searchFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            sessionStore.tenantFlow.collect { tenant ->
                _uiState.update { it.copy(tenantRuc = tenant?.ruc.orEmpty()) }
            }
        }
        refresh()
        viewModelScope.launch {
            searchFlow
                .debounce(900)
                .distinctUntilChanged()
                .collect { query ->
                    val normalized = query.trim()
                    if (normalized.length >= 2 || normalized.isEmpty()) {
                        _uiState.update { it.copy(searchQuery = normalized) }
                        refresh()
                    }
                }
        }
    }

    fun setSearchQuery(value: String) {
        _uiState.update { it.copy(searchQuery = value) }
        searchFlow.value = value
    }

    fun setShowInactive(show: Boolean) {
        _uiState.update { it.copy(showInactive = show) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            when (val result = contactsRepository.listCustomers(
                query = _uiState.value.searchQuery,
                includeInactive = _uiState.value.showInactive,
            )) {
                is AppResult.Success -> _uiState.update {
                    it.copy(loading = false, contacts = result.data)
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(loading = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun openCreate() {
        _uiState.update {
            it.copy(
                formOpen = true,
                editingContactId = null,
                form = ContactFormInput(),
                error = null,
            )
        }
    }

    fun openEdit(contactId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            when (val result = contactsRepository.getCustomer(contactId)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        actionLoading = false,
                        formOpen = true,
                        editingContactId = contactId,
                        form = result.data.toFormInput(),
                    )
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(actionLoading = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun dismissForm() {
        _uiState.update { it.copy(formOpen = false, editingContactId = null) }
    }

    fun updateForm(transform: (ContactFormInput) -> ContactFormInput) {
        _uiState.update { it.copy(form = transform(it.form)) }
    }

    fun consultDocument() {
        val state = _uiState.value
        val form = state.form
        if (!ContactDocType.supportsConsulta(form.docType.code)) {
            _uiState.update { it.copy(error = "Consulta disponible solo para DNI o RUC") }
            return
        }
        val num = sanitizeDocNumber(form.docType, form.docNumber)
        val tenantRuc = state.tenantRuc.filter { it.isDigit() }
        if (tenantRuc.length != 11) {
            _uiState.update { it.copy(error = "No se pudo obtener el RUC de la empresa") }
            return
        }
        when (form.docType) {
            ContactDocType.RUC -> if (num.length != 11) {
                _uiState.update { it.copy(error = "Ingresa un RUC de 11 dígitos") }
                return
            }
            ContactDocType.DNI -> if (num.length != 8) {
                _uiState.update { it.copy(error = "Ingresa un DNI de 8 dígitos") }
                return
            }
            else -> return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(consulting = true, error = null) }
            when (form.docType) {
                ContactDocType.RUC -> when (
                    val result = contactsRepository.consultRuc(tenantRuc, num)
                ) {
                    is AppResult.Success -> {
                        val data = result.data
                        val razonSocial = data.razonSocial
                        if (!data.success || razonSocial.isNullOrBlank()) {
                            _uiState.update {
                                it.copy(consulting = false, error = "No se encontró el RUC")
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    consulting = false,
                                    form = it.form.copy(
                                        docNumber = num,
                                        businessName = razonSocial,
                                        address = data.direccion.orEmpty(),
                                        ubigeo = data.ubigeo?.takeIf { u -> u.length >= 6 }.orEmpty(),
                                    ),
                                    snackMessage = "Datos obtenidos correctamente",
                                )
                            }
                        }
                    }
                    is AppResult.Error -> _uiState.update {
                        it.copy(consulting = false, error = result.message)
                    }
                    AppResult.Loading -> Unit
                }
                ContactDocType.DNI -> when (
                    val result = contactsRepository.consultDni(tenantRuc, num)
                ) {
                    is AppResult.Success -> {
                        val data = result.data
                        val nombreCompleto = data.nombreCompleto
                        if (!data.success || nombreCompleto.isNullOrBlank()) {
                            _uiState.update {
                                it.copy(consulting = false, error = "No se encontró el DNI")
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    consulting = false,
                                    form = it.form.copy(
                                        docNumber = num,
                                        businessName = nombreCompleto,
                                    ),
                                    snackMessage = "Datos obtenidos correctamente",
                                )
                            }
                        }
                    }
                    is AppResult.Error -> _uiState.update {
                        it.copy(consulting = false, error = result.message)
                    }
                    AppResult.Loading -> Unit
                }
                else -> _uiState.update { it.copy(consulting = false) }
            }
        }
    }

    fun saveContact() {
        val state = _uiState.value
        val form = state.form
        if (form.businessName.trim().isEmpty() || form.docNumber.trim().isEmpty()) {
            _uiState.update { it.copy(error = "Nombre y documento son obligatorios") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, error = null) }
            val normalized = form.copy(
                docNumber = sanitizeDocNumber(form.docType, form.docNumber),
            )
            val result = if (state.editingContactId == null) {
                contactsRepository.createCustomer(normalized)
            } else {
                contactsRepository.updateCustomer(state.editingContactId, normalized)
            }
            when (result) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            actionLoading = false,
                            formOpen = false,
                            editingContactId = null,
                            snackMessage = if (state.editingContactId == null) {
                                "Cliente creado"
                            } else {
                                "Cliente actualizado"
                            },
                        )
                    }
                    refresh()
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(actionLoading = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun requestDelete(contactId: Int) {
        _uiState.update { it.copy(deleteContactId = contactId) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(deleteContactId = null) }
    }

    fun confirmDelete() {
        val contactId = _uiState.value.deleteContactId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(actionLoading = true, deleteContactId = null) }
            when (val result = contactsRepository.deleteCustomer(contactId)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(actionLoading = false, snackMessage = "Cliente eliminado")
                    }
                    refresh()
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(actionLoading = false, error = result.message)
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun toggleActive(contactId: Int) {
        viewModelScope.launch {
            when (val result = contactsRepository.toggleCustomer(contactId)) {
                is AppResult.Success -> refresh()
                is AppResult.Error -> _uiState.update { it.copy(error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun consumeSnackMessage() {
        _uiState.update { it.copy(snackMessage = null) }
    }
}
