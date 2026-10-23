package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.sales.SoldExport
import com.bendey.restaurant.core.domain.sales.SoldExportRow
import com.bendey.restaurant.core.domain.sales.SoldItem
import com.bendey.restaurant.core.domain.sales.SoldPayment
import com.bendey.restaurant.core.domain.sales.SoldReportFilter
import com.bendey.restaurant.core.domain.sales.SoldReportPage
import com.bendey.restaurant.core.domain.sales.SoldReportRepository
import com.bendey.restaurant.core.domain.sales.SoldReportSummary
import com.bendey.restaurant.core.domain.sales.SoldSale
import com.bendey.restaurant.core.network.api.SoldReportApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.error.apiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoldReportRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : SoldReportRepository {
    private val api: SoldReportApi
        get() = tenantRetrofitProvider.create()

    override suspend fun page(filter: SoldReportFilter, page: Int, perPage: Int): AppResult<SoldReportPage> = apiCall {
        val d = api.page(
            from = filter.from,
            to = filter.to,
            userId = filter.userId,
            paymentMethod = filter.paymentMethod,
            docKind = filter.docKind,
            onlyValid = if (filter.onlyValid) 1 else null,
            page = page,
            perPage = perPage,
        ).data
        SoldReportPage(
            total = d?.total ?: 0,
            summary = SoldReportSummary(
                validSales = d?.summary?.validSales ?: 0,
                validAmount = d?.summary?.validAmount ?: 0.0,
                cancelledSales = d?.summary?.cancelledSales ?: 0,
                cancelledAmount = d?.summary?.cancelledAmount ?: 0.0,
            ),
            sales = d?.sales.orEmpty().map { s ->
                SoldSale(
                    saleId = s.saleId ?: 0,
                    issueDate = s.issueDate.orEmpty(),
                    registeredAt = s.registeredAt.orEmpty(),
                    docKind = s.docKind.orEmpty(),
                    number = s.number.orEmpty(),
                    valid = s.valid ?: true,
                    cashierName = s.cashierName.orEmpty(),
                    waiterName = s.waiterName?.takeIf { it.isNotBlank() },
                    service = s.service.orEmpty(),
                    orderType = s.orderType.orEmpty(),
                    tableName = s.tableName,
                    total = s.total ?: 0.0,
                    payments = s.payments.orEmpty().map { SoldPayment(it.method.orEmpty(), it.amount ?: 0.0) },
                    items = s.items.orEmpty().map {
                        SoldItem(
                            description = it.description.orEmpty(),
                            components = it.components.orEmpty(),
                            quantity = it.quantity ?: 0.0,
                            unitPrice = it.unitPrice ?: 0.0,
                            total = it.total ?: 0.0,
                        )
                    },
                )
            },
        )
    }

    override suspend fun export(filter: SoldReportFilter): AppResult<SoldExport> = apiCall {
        val d = api.export(
            from = filter.from,
            to = filter.to,
            userId = filter.userId,
            paymentMethod = filter.paymentMethod,
            docKind = filter.docKind,
            onlyValid = if (filter.onlyValid) 1 else null,
        ).data
        SoldExport(
            rows = d?.rows.orEmpty().map {
                SoldExportRow(
                    issueDate = it.issueDate.orEmpty(),
                    registeredAt = it.registeredAt.orEmpty(),
                    docKind = it.docKind.orEmpty(),
                    number = it.number.orEmpty(),
                    valid = it.valid ?: true,
                    service = it.service.orEmpty(),
                    orderType = it.orderType.orEmpty(),
                    tableName = it.tableName,
                    cashierName = it.cashierName.orEmpty(),
                    waiterName = it.waiterName,
                    payments = it.payments.orEmpty(),
                    description = it.description.orEmpty(),
                    quantity = it.quantity ?: 0.0,
                    unitPrice = it.unitPrice ?: 0.0,
                    lineTotal = it.lineTotal ?: 0.0,
                    saleTotal = it.saleTotal ?: 0.0,
                )
            },
            truncated = d?.truncated == true,
        )
    }
}
