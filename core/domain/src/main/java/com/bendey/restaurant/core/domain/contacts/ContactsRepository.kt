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

    /**
     * Una página de clientes y el total. La pantalla Clientes la usa para no bajar cientos de
     * contactos de golpe (El Braserito tiene 609). Por defecto cae a [listCustomers] completo,
     * así las implementaciones de prueba no tienen que cambiar.
     */
    suspend fun listCustomersPage(
        query: String = "",
        includeInactive: Boolean = false,
        page: Int = 1,
        perPage: Int = 50,
        type: String = "customer",
    ): AppResult<Pair<List<CustomerContact>, Int>> =
        when (val all = listCustomers(query, includeInactive, type)) {
            is AppResult.Success -> AppResult.Success(all.data to all.data.size)
            is AppResult.Error -> all
            AppResult.Loading -> AppResult.Loading
        }

    suspend fun getCustomer(id: Int): AppResult<CustomerContact>
    suspend fun createCustomer(input: ContactFormInput, type: String = "customer"): AppResult<CustomerContact>
    suspend fun updateCustomer(id: Int, input: ContactFormInput, type: String = "customer"): AppResult<CustomerContact>
    suspend fun deleteCustomer(id: Int): AppResult<Unit>
    suspend fun toggleCustomer(id: Int): AppResult<Unit>
    suspend fun consultDni(tenantRuc: String, dni: String): AppResult<ConsultaDniResult>
    suspend fun purchaseHistory(id: Int, page: Int = 1, limit: Int = PurchaseHistoryRules.PAGE_SIZE): AppResult<PurchaseHistoryPage>
    suspend fun consultRuc(tenantRuc: String, ruc: String): AppResult<ConsultaRucResult>
}
