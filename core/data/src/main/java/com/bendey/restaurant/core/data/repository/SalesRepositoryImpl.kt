package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.sales.CancelNotaResult
import com.bendey.restaurant.core.domain.sales.SaleDetail
import com.bendey.restaurant.core.domain.sales.SaleDetailLine
import com.bendey.restaurant.core.domain.sales.SaleDetailPayment
import com.bendey.restaurant.core.domain.sales.SaleSummary
import com.bendey.restaurant.core.domain.sales.SalesRepository
import com.bendey.restaurant.core.domain.sales.VentasTab
import com.bendey.restaurant.core.network.api.SalesApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.dto.CancelSaleRequestDto
import com.bendey.restaurant.core.network.dto.SaleDetailResponseDto
import com.bendey.restaurant.core.network.dto.SaleDto
import com.bendey.restaurant.core.network.error.NetworkErrorMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SalesRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : SalesRepository {

    override suspend fun listSales(
        from: String?,
        to: String?,
        tab: VentasTab,
        page: Int,
        perPage: Int,
    ): AppResult<Pair<List<SaleSummary>, Int>> = apiCall {
        val filters = tab.toListFilters()
        val response = tenantRetrofitProvider.create<SalesApi>().listSales(
            from = from,
            to = to,
            page = page,
            perPage = perPage,
            sunatCode = filters.sunatCode,
            docType = filters.docType,
        )
        response.data.map { it.toDomain() } to response.total
    }

    override suspend fun getSaleDetail(saleId: Int): AppResult<SaleDetail> = apiCall {
        tenantRetrofitProvider.create<SalesApi>().getSale(saleId).toDomain()
    }

    override suspend fun cancelNotaVenta(saleId: Int, reason: String): AppResult<CancelNotaResult> = apiCall {
        val response = tenantRetrofitProvider.create<SalesApi>()
            .cancelNota(saleId, CancelSaleRequestDto(reason = reason.trim()))
        CancelNotaResult(message = response.message ?: "Nota de venta anulada")
    }
}

private data class ListFilters(
    val sunatCode: String? = null,
    val docType: String? = null,
)

private fun VentasTab.toListFilters(): ListFilters = when (this) {
    VentasTab.NOTAS -> ListFilters(sunatCode = "00")
    VentasTab.FACTURACION -> ListFilters(sunatCode = "01,03")
    VentasTab.CREDITOS -> ListFilters(docType = "NOTA_CREDITO")
}

private inline fun <T> apiCall(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: Exception) {
    val mapped = NetworkErrorMapper.map(e)
    AppResult.Error(mapped.message ?: "Error de conexión", mapped)
}

private fun SaleDto.toDomain() = SaleSummary(
    id = id,
    docType = docType,
    number = formatSaleNumber(series, number),
    issueDate = issueDate,
    contactName = contactName,
    total = total,
    currency = currency,
    status = status,
    billingStatus = billingStatus,
    paymentMethod = paymentMethod,
    sunatCode = sunatCode,
    convertedTo = convertedTo,
    electronicIssueSaleId = electronicIssueSaleId,
)

private fun formatSaleNumber(series: String, number: String): String {
    val s = series.trim()
    val n = number.trim()
    if (n.isBlank()) return s
    if (n.contains("-")) return n
    if (s.isBlank()) return n
    return "$s-$n"
}

private fun SaleDetailResponseDto.toDomain(): SaleDetail {
    val saleDto = sale ?: error("Venta no encontrada")
    return SaleDetail(
        id = saleDto.id,
        number = formatSaleNumber(saleDto.series, saleDto.number),
        docType = saleDto.docType,
        issueDate = saleDto.issueDate,
        contactName = saleDto.contactName,
        subtotal = saleDto.subtotal.takeIf { it > 0 } ?: saleDto.total,
        taxAmount = saleDto.taxAmount,
        total = saleDto.total,
        currency = saleDto.currency,
        status = saleDto.status,
        billingStatus = saleDto.billingStatus,
        sunatCode = saleDto.sunatCode,
        convertedTo = saleDto.convertedTo,
        electronicIssueSaleId = saleDto.electronicIssueSaleId,
        items = items.map {
            SaleDetailLine(
                description = it.description.ifBlank { it.code },
                quantity = it.quantity,
                unitPrice = it.unitPrice,
                total = it.total,
            )
        },
        payments = payments.map {
            SaleDetailPayment(
                method = it.method,
                amount = it.amount,
                reference = it.reference,
            )
        },
        printData = printData?.toDomain(),
    )
}
