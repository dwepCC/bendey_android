package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.purchases.CreatePurchaseInput
import com.bendey.restaurant.core.domain.purchases.Purchase
import com.bendey.restaurant.core.domain.purchases.PurchaseDetail
import com.bendey.restaurant.core.domain.purchases.PurchaseItem
import com.bendey.restaurant.core.domain.purchases.PurchaseListParams
import com.bendey.restaurant.core.domain.purchases.PurchasesRepository
import com.bendey.restaurant.core.network.api.PurchasesApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.dto.CreatePurchaseRequestDto
import com.bendey.restaurant.core.network.dto.PurchaseDetailDataDto
import com.bendey.restaurant.core.network.dto.PurchaseDto
import com.bendey.restaurant.core.network.dto.PurchaseItemDto
import com.bendey.restaurant.core.network.error.NetworkErrorMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PurchasesRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : PurchasesRepository {

    private val api: PurchasesApi
        get() = tenantRetrofitProvider.create()

    override suspend fun listPurchases(params: PurchaseListParams): AppResult<List<Purchase>> = apiCall {
        api.listPurchases(
            query = params.query,
            from = params.dateFrom,
            to = params.dateTo,
            status = params.status,
        ).data.map { it.toDomain() }
    }

    override suspend fun getPurchase(id: Int): AppResult<PurchaseDetail> = apiCall {
        api.getPurchase(id).data.toDetail()
    }

    override suspend fun createPurchase(input: CreatePurchaseInput): AppResult<Purchase> = apiCall {
        val response = api.createPurchase(input.toRequestDto())
        response.data?.toDomain() ?: throw IllegalStateException("Respuesta sin datos de compra")
    }

    override suspend fun voidPurchase(id: Int): AppResult<String> = apiCall {
        api.voidPurchase(id).message ?: "Compra anulada"
    }
}

private inline fun <T> apiCall(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: Exception) {
    val mapped = NetworkErrorMapper.map(e)
    AppResult.Error(mapped.message ?: "Error de conexión", mapped)
}

private fun PurchaseDto.toDomain() = Purchase(
    id = id,
    docType = docType,
    series = series,
    number = number,
    issueDate = issueDate,
    contactId = contactId,
    supplierName = supplierName,
    subtotal = subtotal,
    taxAmount = taxAmount,
    total = total,
    currency = currency,
    status = status,
    paymentMethod = paymentMethod,
    notes = notes,
)

private fun PurchaseDetailDataDto.toDetail() = PurchaseDetail(
    purchase = Purchase(
        id = id,
        docType = docType,
        series = series,
        number = number,
        issueDate = issueDate,
        contactId = contactId,
        supplierName = supplierName,
        subtotal = subtotal,
        taxAmount = taxAmount,
        total = total,
        currency = currency,
        status = status,
        paymentMethod = paymentMethod,
        notes = notes,
    ),
    items = items.map { it.toDomain() },
)

private fun PurchaseItemDto.toDomain() = PurchaseItem(
    productId = productId,
    code = code,
    description = description,
    unit = unit,
    quantity = quantity,
    unitCost = unitCost,
    igvAffectationType = igvAffectationType,
    priceIncludesIgv = priceIncludesIgv,
)

private fun PurchaseItem.toDto() = PurchaseItemDto(
    productId = productId,
    code = code,
    description = description,
    unit = unit,
    quantity = quantity,
    unitCost = unitCost,
    igvAffectationType = igvAffectationType,
    priceIncludesIgv = priceIncludesIgv,
    serials = emptyList(),
)

private fun CreatePurchaseInput.toRequestDto() = CreatePurchaseRequestDto(
    branchId = branchId,
    contactId = contactId,
    docType = docType,
    series = series.takeIf { it.isNotBlank() },
    number = number,
    issueDate = issueDate,
    currency = currency,
    paymentMethod = paymentMethod,
    notes = notes,
    items = items.map { it.toDto() },
)
