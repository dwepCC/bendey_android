package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.sales.CancelNotaResult
import com.bendey.restaurant.core.domain.sales.IssueElectronicResult
import com.bendey.restaurant.core.domain.sales.SaleContactBrief
import com.bendey.restaurant.core.domain.sales.SaleDetail
import com.bendey.restaurant.core.domain.sales.SaleDetailLine
import com.bendey.restaurant.core.domain.sales.SaleDetailPayment
import com.bendey.restaurant.core.domain.sales.SaleListSummary
import com.bendey.restaurant.core.domain.sales.SalePaymentTotal
import com.bendey.restaurant.core.domain.sales.SalesListPage
import com.bendey.restaurant.core.domain.sales.SaleSummary
import com.bendey.restaurant.core.domain.sales.SalesRepository
import com.bendey.restaurant.core.domain.sales.VentasTab
import com.bendey.restaurant.core.network.api.SalesApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.dto.CancelSaleRequestDto
import com.bendey.restaurant.core.network.dto.IssueElectronicRequestDto
import com.bendey.restaurant.core.network.dto.SaleContactDto
import com.bendey.restaurant.core.network.dto.SaleDetailResponseDto
import com.bendey.restaurant.core.network.dto.SaleDto
import com.bendey.restaurant.core.network.dto.SaleListSummaryDto
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
        query: String?,
        paymentMethod: String?,
        billingStatus: String?,
    ): AppResult<SalesListPage> = apiCall {
        val filters = tab.toListFilters()
        val response = tenantRetrofitProvider.create<SalesApi>().listSales(
            query = query?.trim()?.takeIf { it.isNotEmpty() },
            from = from,
            to = to,
            page = page,
            perPage = perPage,
            sunatCode = filters.sunatCode,
            docType = filters.docType,
            billingStatus = billingStatus?.trim()?.takeIf { it.isNotEmpty() && tab == VentasTab.FACTURACION },
            paymentMethod = paymentMethod?.trim()?.takeIf { it.isNotEmpty() },
        )
        SalesListPage(
            sales = response.data.map { it.toDomain() },
            total = response.total,
            summary = response.summary?.toDomain() ?: SaleListSummary(),
        )
    }

    override suspend fun listAllSalesForExport(
        from: String?,
        to: String?,
        tab: VentasTab,
        query: String?,
        paymentMethod: String?,
        billingStatus: String?,
    ): AppResult<List<SaleSummary>> = apiCall {
        val filters = tab.toListFilters()
        tenantRetrofitProvider.create<SalesApi>().listSales(
            query = query?.trim()?.takeIf { it.isNotEmpty() },
            from = from,
            to = to,
            exportAll = 1,
            sunatCode = filters.sunatCode,
            docType = filters.docType,
            billingStatus = billingStatus?.trim()?.takeIf { it.isNotEmpty() && tab == VentasTab.FACTURACION },
            paymentMethod = paymentMethod?.trim()?.takeIf { it.isNotEmpty() },
        ).data.map { it.toDomain() }
    }

    override suspend fun getSaleDetail(saleId: Int): AppResult<SaleDetail> = apiCall {
        tenantRetrofitProvider.create<SalesApi>().getSale(saleId).toDomain()
    }

    override suspend fun cancelNotaVenta(saleId: Int, reason: String): AppResult<CancelNotaResult> = apiCall {
        val response = tenantRetrofitProvider.create<SalesApi>()
            .cancelNota(saleId, CancelSaleRequestDto(reason = reason.trim()))
        CancelNotaResult(message = response.message ?: "Nota de venta anulada")
    }

    override suspend fun issueElectronicFromNota(
        saleId: Int,
        seriesId: Int,
        issueDate: String?,
        contactId: Int?,
    ): AppResult<IssueElectronicResult> = apiCall {
        val response = tenantRetrofitProvider.create<SalesApi>().issueElectronicFromNota(
            saleId = saleId,
            body = IssueElectronicRequestDto(
                seriesId = seriesId,
                issueDate = issueDate?.trim()?.takeIf { it.isNotEmpty() },
                contactId = contactId?.takeIf { it > 0 },
            ),
        )
        val sale = response.sale ?: error("Comprobante no generado")
        IssueElectronicResult(
            saleId = sale.id,
            docType = sale.docType,
            number = formatSaleNumber(sale.series, sale.number),
            message = "Comprobante generado: ${sale.docType} ${formatSaleNumber(sale.series, sale.number)}",
        )
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

private fun SaleListSummaryDto.toDomain() = SaleListSummary(
    sumTotal = sumTotal,
    sumActive = sumActive,
    countActive = countActive,
    paymentTotals = paymentTotals.map {
        SalePaymentTotal(method = it.method, total = it.total, count = it.count)
    },
)

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
    branchId = branchId,
    contactId = contactId,
)

private fun formatSaleNumber(series: String, number: String): String {
    val s = series.trim()
    val n = number.trim()
    if (n.isBlank()) return s
    if (n.contains("-")) return n
    if (s.isBlank()) return n
    return "$s-$n"
}

private fun SaleContactDto.toDomain() = SaleContactBrief(
    id = id,
    docType = docType,
    docNumber = docNumber,
    businessName = businessName,
)

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
        branchId = saleDto.branchId,
        contactId = saleDto.contactId,
        contact = contact?.toDomain(),
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
