package com.bendey.restaurant.core.domain.contacts

import com.bendey.restaurant.core.domain.model.AppResult

interface ContactsRepository {
    /**
     * type: "customer" (default) | "supplier". El mismo Contact de backend sirve para ambos —
     * Compras usa type="supplier" para el catálogo de proveedores (ver feature/proveedores).
     */
    suspend fun listCustomers(
        query: String = "",
        includeInactive: Boolean = false,
        type: String = "customer",
    ): AppResult<List<CustomerContact>>
    suspend fun getCustomer(id: Int): AppResult<CustomerContact>
    suspend fun createCustomer(input: ContactFormInput, type: String = "customer"): AppResult<CustomerContact>
    suspend fun updateCustomer(id: Int, input: ContactFormInput, type: String = "customer"): AppResult<CustomerContact>
    suspend fun deleteCustomer(id: Int): AppResult<Unit>
    suspend fun toggleCustomer(id: Int): AppResult<Unit>
    suspend fun consultDni(tenantRuc: String, dni: String): AppResult<ConsultaDniResult>
    suspend fun consultRuc(tenantRuc: String, ruc: String): AppResult<ConsultaRucResult>
}
