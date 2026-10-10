package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.inventory.CountLineInput
import com.bendey.restaurant.core.domain.inventory.CountResultLine
import com.bendey.restaurant.core.domain.inventory.InventoryCountRepository
import com.bendey.restaurant.core.domain.inventory.InventoryValuation
import com.bendey.restaurant.core.domain.inventory.PhysicalCountResult
import com.bendey.restaurant.core.domain.inventory.ValuationRow
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.network.api.CountLineRequestDto
import com.bendey.restaurant.core.network.api.InventoryCountApi
import com.bendey.restaurant.core.network.api.PhysicalCountRequestDto
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.error.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/** Valorización y conteo físico (sección 25 del contrato). */
@Singleton
class InventoryCountRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : InventoryCountRepository {
    private val api: InventoryCountApi
        get() = tenantRetrofitProvider.create()

    override suspend fun valuation(branchId: Int?, q: String): AppResult<InventoryValuation> = apiCall {
        val d = api.valuation(branchId?.takeIf { it > 0 }, q.trim().takeIf { it.isNotEmpty() }).data
        InventoryValuation(
            branchId = d?.branchId ?: 0,
            rows = d?.rows.orEmpty().map {
                ValuationRow(
                    productId = it.productId ?: 0,
                    presentationId = it.presentationId ?: 0,
                    code = it.code.orEmpty(),
                    name = it.name.orEmpty(),
                    quantity = it.quantity ?: 0.0,
                    averageCost = it.averageCost ?: 0.0,
                    value = it.value ?: 0.0,
                    negative = it.negative ?: ((it.quantity ?: 0.0) < 0.0),
                )
            },
            totalValue = d?.totalValue ?: 0.0,
            totalProducts = d?.totalProducts ?: 0,
            negativeCount = d?.negativeCount ?: 0,
        )
    }

    override suspend fun physicalCount(
        branchId: Int,
        lines: List<CountLineInput>,
        note: String,
        apply: Boolean,
    ): AppResult<PhysicalCountResult> = apiCall {
        val d = api.physicalCount(
            PhysicalCountRequestDto(
                branchId = branchId,
                lines = lines.map { CountLineRequestDto(it.productId, it.presentationId, it.counted) },
                note = note.trim().takeIf { it.isNotEmpty() },
                apply = apply,
            ),
        ).data
        PhysicalCountResult(
            applied = d?.applied ?: apply,
            lines = d?.lines.orEmpty().map {
                CountResultLine(
                    productId = it.productId ?: 0,
                    presentationId = it.presentationId ?: 0,
                    name = it.name.orEmpty(),
                    system = it.system ?: 0.0,
                    counted = it.counted ?: 0.0,
                    difference = it.difference ?: 0.0,
                    unitCost = it.unitCost ?: 0.0,
                    valueDiff = it.valueDiff ?: 0.0,
                    applied = it.applied ?: false,
                    error = it.error?.takeIf { e -> e.isNotBlank() },
                )
            },
            adjustedCount = d?.adjustedCount ?: 0,
            failedCount = d?.failedCount ?: 0,
            totalValueDiff = d?.totalValueDiff ?: 0.0,
        )
    }
}
