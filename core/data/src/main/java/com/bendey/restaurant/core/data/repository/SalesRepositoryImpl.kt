package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.sales.CancelNotaResult
import com.bendey.restaurant.core.domain.sales.IssueElectronicResult
import com.bendey.restaurant.core.domain.sales.RefundResult
import com.bendey.restaurant.core.domain.sales.SaleContactBrief
import com.bendey.restaurant.core.domain.sales.SaleDetail
import com.bendey.restaurant.core.domain.sales.SaleDetailLine
import com.bendey.restaurant.core.domain.sales.SaleDetailPayment
import com.bendey.restaurant.core.domain.sales.SaleListSummary
import com.bendey.restaurant.core.domain.sales.SalePaymentLine
import com.bendey.restaurant.core.domain.sales.SalePaymentTotal
import com.bendey.restaurant.core.domain.sales.SalesByProductPage
import com.bendey.restaurant.core.domain.sales.SalesByProductRow
import com.bendey.restaurant.core.domain.sales.SalesByProductSummary
import com.bendey.restaurant.core.domain.sales.SalesListPage
import com.bendey.restaurant.core.domain.sales.SaleSummary
import com.bendey.restaurant.core.domain.sales.SalesRepository
import com.bendey.restaurant.core.domain.sales.VentasTab
import com.bendey.restaurant.core.domain.sales.incluyeElectronicos
import com.bendey.restaurant.core.network.api.SalesApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.dto.CancelSaleRequestDto
import com.bendey.restaurant.core.network.dto.RefundSaleRequestDto
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
        orderType: String?,
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
            billingStatus = billingStatus?.trim()?.takeIf { it.isNotEmpty() && tab.incluyeElectronicos() },
            paymentMethod = paymentMethod?.trim()?.takeIf { it.isNotEmpty() },
            orderType = orderType?.trim()?.takeIf { it.isNotEmpty() },
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
        orderType: String?,
    ): AppResult<List<SaleSummary>> = apiCall {
        val filters = tab.toListFilters()
        tenantRetrofitProvider.create<SalesApi>().listSales(
            query = query?.trim()?.takeIf { it.isNotEmpty() },
            from = from,
            to = to,
            exportAll = 1,
            sunatCode = filters.sunatCode,
            docType = filters.docType,
            billingStatus = billingStatus?.trim()?.takeIf { it.isNotEmpty() && tab.incluyeElectronicos() },
            paymentMethod = paymentMethod?.trim()?.takeIf { it.isNotEmpty() },
            orderType = orderType?.trim()?.takeIf { it.isNotEmpty() },
        ).data.map { it.toDomain() }
    }

    override suspend fun getSaleDetail(saleId: Int): AppResult<SaleDetail> = apiCall {
        tenantRetrofitProvider.create<SalesApi>().getSale(saleId).toDomain()
    }

    override suspend fun cancelNotaVenta(saleId: Int, reason: String, pin: String): AppResult<CancelNotaResult> = apiCall {
        val response = tenantRetrofitProvider.create<SalesApi>()
            .cancelNota(saleId, CancelSaleRequestDto(reason = reason.trim(), pin = pin.trim()))
        CancelNotaResult(message = response.message ?: "Nota de venta anulada")
    }

    override suspend fun refundSale(saleId: Int, reason: String): AppResult<RefundResult> = apiCall {
        val response = tenantRetrofitProvider.create<SalesApi>()
            .refundSale(saleId, RefundSaleRequestDto(reason = reason.trim()))
        val data = response.data ?: error("El servidor no informo cuanto se devolvio")
        RefundResult(
            saleId = data.saleId,
            total = data.total,
            cashRefunded = data.cashRefunded,
            bankRefunded = data.bankRefunded,
            reference = data.reference,
        )
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
            number = numeroDeComprobante(sale.series, sale.correlative),
            message = "Comprobante generado: ${sale.docType} ${numeroDeComprobante(sale.series, sale.correlative)}",
        )
    }

    override suspend fun listSalesByProduct(
        from: String?,
        to: String?,
        branchId: Int?,
        categoryId: Int?,
    ): AppResult<SalesByProductPage> = apiCall {
        val response = tenantRetrofitProvider.create<SalesApi>().listSalesByProduct(
            from = from,
            to = to,
            branchId = branchId,
            categoryId = categoryId,
        )
        SalesByProductPage(
            rows = response.data.map {
                SalesByProductRow(
                    productId = it.productId,
                    productCode = it.productCode,
                    productName = it.productName,
                    categoryName = it.categoryName,
                    unit = it.unit,
                    quantitySold = it.quantitySold,
                    totalAmount = it.totalAmount,
                    linesCount = it.linesCount,
                    salesCount = it.salesCount,
                    avgLineAmount = it.avgLineAmount,
                )
            },
            summary = response.summary?.let {
                SalesByProductSummary(
                    totalAmount = it.totalAmount,
                    totalQuantity = it.totalQuantity,
                    lineItems = it.lineItems,
                    distinctSales = it.distinctSales,
                    productsCount = it.productsCount,
                )
            } ?: SalesByProductSummary(),
        )
    }
}

private data class ListFilters(
    val sunatCode: String? = null,
    val docType: String? = null,
)

// La nota de credito se pide por doc_type porque su serie no comparte los codigos de venta; el
// resto sale de `sunatCodes`, que ya viene en el propio filtro.
private fun VentasTab.toListFilters(): ListFilters =
    if (this == VentasTab.CREDITOS) ListFilters(docType = "NOTA_CREDITO")
    else ListFilters(sunatCode = sunatCodes)

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
    number = numeroDeComprobante(series, correlative),
    issueDate = issueDate,
    contactName = contactName,
    total = total,
    currency = currency,
    status = status,
    billingStatus = billingStatus,
    paymentMethod = paymentMethod,
    payments = payments.map { SalePaymentLine(method = it.method, amount = it.amount) },
    sunatCode = sunatCode,
    convertedTo = convertedTo,
    electronicIssueSaleId = electronicIssueSaleId,
    branchId = branchId,
    contactId = contactId,
    refundable = refundable,
    refundableAmount = refundableAmount,
    refundedAmount = refundedAmount,
    tableName = tableName,
    orderType = orderType,
)

/** Cuantos digitos lleva el correlativo impreso: el largo que exige SUNAT y el que usa el backend al
 *  componer el numero al emitir. */
private const val DIGITOS_DEL_CORRELATIVO = 8

/**
 * El numero de un comprobante, compuesto SIEMPRE igual: serie y correlativo.
 *
 * NO MIRA EL CONTENIDO PARA DECIDIR QUE HACER, y esa es toda la diferencia con lo que habia antes.
 *
 * Aqui vivia `formatSaleNumber`, que recibia `series` y `number` y tenia que averiguar si `number` ya
 * traia la serie dentro («si contiene un guion, ya viene completo»). Esa duda no era un capricho:
 * `number` guarda «NV001-00000133» en `tenant_sales` y «00000002» en `tenant_quotations`, con los
 * mismos nombres de campo, asi que quien lo recibia no podia saber cual convencion le tocaba. De esa
 * adivinanza salian los «NV001-NV001-00000133».
 *
 * `series` y `correlative` no admiten esa duda: uno es el codigo de la serie, el otro un entero que el
 * backend reserva de forma transaccional al emitir.
 */
private fun numeroDeComprobante(series: String, correlative: Int): String =
    "$series-${correlative.toString().padStart(DIGITOS_DEL_CORRELATIVO, '0')}"

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
        number = numeroDeComprobante(saleDto.series, saleDto.correlative),
        docType = saleDto.docType,
        issueDate = saleDto.issueDate,
        contactName = saleDto.contactName,
        subtotal = saleDto.subtotal.takeIf { it > 0 } ?: saleDto.total,
        taxAmount = saleDto.taxAmount,
        total = saleDto.total,
        serviceChargeAmount = saleDto.serviceChargeAmount,
        currency = saleDto.currency,
        status = saleDto.status,
        billingStatus = saleDto.billingStatus,
        sunatCode = saleDto.sunatCode,
        convertedTo = saleDto.convertedTo,
        electronicIssueSaleId = saleDto.electronicIssueSaleId,
        branchId = saleDto.branchId,
        contactId = saleDto.contactId,
        refundable = saleDto.refundable,
        refundableAmount = saleDto.refundableAmount,
        refundedAmount = saleDto.refundedAmount,
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
