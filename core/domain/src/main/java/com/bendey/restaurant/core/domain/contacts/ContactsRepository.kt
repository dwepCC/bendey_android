package com.bendey.restaurant.core.domain.contacts

import com.bendey.restaurant.core.domain.model.AppResult

interface ContactsRepository {
    suspend fun listCustomers(query: String = "", includeInactive: Boolean = false): AppResult<List<CustomerContact>>
    suspend fun getCustomer(id: Int): AppResult<CustomerContact>
    suspend fun createCustomer(input: ContactFormInput): AppResult<CustomerContact>
    suspend fun updateCustomer(id: Int, input: ContactFormInput): AppResult<CustomerContact>
    suspend fun deleteCustomer(id: Int): AppResult<Unit>
    suspend fun toggleCustomer(id: Int): AppResult<Unit>
    suspend fun consultDni(tenantRuc: String, dni: String): AppResult<ConsultaDniResult>
    suspend fun consultRuc(tenantRuc: String, ruc: String): AppResult<ConsultaRucResult>
}
