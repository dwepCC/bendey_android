package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.contacts.ConsultaDniResult
import com.bendey.restaurant.core.domain.contacts.ConsultaRucResult
import com.bendey.restaurant.core.domain.contacts.ContactFormInput
import com.bendey.restaurant.core.domain.contacts.ContactsRepository
import com.bendey.restaurant.core.domain.contacts.CustomerContact
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.network.api.ConsultaApi
import com.bendey.restaurant.core.network.api.ContactsApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.dto.ContactDto
import com.bendey.restaurant.core.network.dto.ConsultaDniRequestDto
import com.bendey.restaurant.core.network.dto.ConsultaRucRequestDto
import com.bendey.restaurant.core.network.dto.CreateContactRequestDto
import com.bendey.restaurant.core.network.dto.UpdateContactRequestDto
import com.bendey.restaurant.core.network.error.NetworkErrorMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : ContactsRepository {

    private val contactsApi: ContactsApi
        get() = tenantRetrofitProvider.create()

    private val consultaApi: ConsultaApi
        get() = tenantRetrofitProvider.create()

    override suspend fun listCustomers(query: String, includeInactive: Boolean): AppResult<List<CustomerContact>> = apiCall {
        contactsApi.listContacts(
            query = query,
            type = "customer",
            status = if (includeInactive) "all" else "active",
        ).data.map { it.toCustomer() }
    }

    override suspend fun getCustomer(id: Int): AppResult<CustomerContact> = apiCall {
        contactsApi.getContact(id).data.toCustomer()
    }

    override suspend fun createCustomer(input: ContactFormInput): AppResult<CustomerContact> = apiCall {
        contactsApi.createContact(input.toCreateDto()).data.toCustomer()
    }

    override suspend fun updateCustomer(id: Int, input: ContactFormInput): AppResult<CustomerContact> = apiCall {
        contactsApi.updateContact(id, input.toUpdateDto()).data.toCustomer()
    }

    override suspend fun deleteCustomer(id: Int): AppResult<Unit> = apiCall {
        contactsApi.deleteContact(id)
    }

    override suspend fun toggleCustomer(id: Int): AppResult<Unit> = apiCall {
        contactsApi.toggleContact(id)
    }

    override suspend fun consultDni(tenantRuc: String, dni: String): AppResult<ConsultaDniResult> = apiCall {
        val response = consultaApi.consultDni(
            ConsultaDniRequestDto(dni = dni.trim(), tenantRuc = tenantRuc.trim()),
        )
        ConsultaDniResult(
            success = response.success,
            nombreCompleto = response.nombreCompleto,
        )
    }

    override suspend fun consultRuc(tenantRuc: String, ruc: String): AppResult<ConsultaRucResult> = apiCall {
        val response = consultaApi.consultRuc(
            ConsultaRucRequestDto(ruc = ruc.trim(), tenantRuc = tenantRuc.trim()),
        )
        ConsultaRucResult(
            success = response.success,
            razonSocial = response.razonSocial,
            direccion = response.direccion,
            ubigeo = response.ubigeo,
        )
    }
}

private inline fun <T> apiCall(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: Exception) {
    val mapped = NetworkErrorMapper.map(e)
    AppResult.Error(mapped.message ?: "Error de conexión", mapped)
}

private fun ContactDto.toCustomer() = CustomerContact(
    id = id,
    docType = docType,
    docNumber = docNumber,
    businessName = businessName,
    tradeName = tradeName.takeIf { it.isNotBlank() },
    address = address?.takeIf { it.isNotBlank() },
    ubigeo = ubigeo?.takeIf { it.isNotBlank() },
    phone = phone?.takeIf { it.isNotBlank() },
    email = email?.takeIf { it.isNotBlank() },
    active = active,
)

private fun ContactFormInput.toCreateDto() = CreateContactRequestDto(
    type = "customer",
    docType = docType.code,
    docNumber = docNumber.trim(),
    businessName = businessName.trim(),
    tradeName = tradeName.trim(),
    address = address.trim(),
    ubigeo = ubigeo.trim().takeIf { it.length >= 6 },
    phone = phone.trim(),
    email = email.trim(),
)

private fun ContactFormInput.toUpdateDto() = UpdateContactRequestDto(
    type = "customer",
    docType = docType.code,
    docNumber = docNumber.trim(),
    businessName = businessName.trim(),
    tradeName = tradeName.trim(),
    address = address.trim(),
    ubigeo = ubigeo.trim().takeIf { it.isNotBlank() },
    phone = phone.trim(),
    email = email.trim(),
)
