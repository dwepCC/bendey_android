package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.rcshare.RcDistribution
import com.bendey.restaurant.core.domain.rcshare.RcLine
import com.bendey.restaurant.core.domain.rcshare.RcShareInput
import com.bendey.restaurant.core.domain.rcshare.RcShareRepository
import com.bendey.restaurant.core.domain.rcshare.RcSummary
import com.bendey.restaurant.core.domain.tips.MyTips
import com.bendey.restaurant.core.domain.tips.PendingTipStaff
import com.bendey.restaurant.core.domain.tips.TipMethodRow
import com.bendey.restaurant.core.domain.tips.TipPayoutResult
import com.bendey.restaurant.core.domain.tips.TipRecent
import com.bendey.restaurant.core.domain.tips.TipStaffRow
import com.bendey.restaurant.core.domain.tips.TipsReport
import com.bendey.restaurant.core.domain.tips.TipsRepository
import com.bendey.restaurant.core.network.api.MyTipsDto
import com.bendey.restaurant.core.network.api.RcDistributeLineDto
import com.bendey.restaurant.core.network.api.RcDistributeRequestDto
import com.bendey.restaurant.core.network.api.RcShareApi
import com.bendey.restaurant.core.network.api.RcSummaryDto
import com.bendey.restaurant.core.network.api.TipPayoutRequestDto
import com.bendey.restaurant.core.network.api.TipsApi
import com.bendey.restaurant.core.network.api.TipsReportDto
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.error.apiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TipsRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : TipsRepository {
    private val api: TipsApi
        get() = tenantRetrofitProvider.create()

    override suspend fun pending(): AppResult<List<PendingTipStaff>> = apiCall {
        api.pending().data.orEmpty().map {
            PendingTipStaff(it.staffId ?: 0, it.name.orEmpty(), it.count ?: 0, it.total ?: 0.0, it.unbooked ?: 0)
        }
    }

    override suspend fun payout(staffId: Int): AppResult<TipPayoutResult> = apiCall {
        val d = api.payout(TipPayoutRequestDto(staffId)).data
        TipPayoutResult(d?.name.orEmpty(), d?.payout?.amount ?: 0.0, d?.payout?.tipsCount ?: 0)
    }

    override suspend fun report(from: String, to: String): AppResult<TipsReport> = apiCall { api.report(from, to).toDomain() }

    override suspend fun mine(): AppResult<MyTips> = apiCall { api.mine().toDomain() }
}

/** TOLERANTE: ausentes = 0 / vacío; el envoltorio `data` se desenvuelve. */
fun TipsReportDto.toDomain(): TipsReport {
    val r = data ?: this
    return TipsReport(
        total = r.total ?: 0.0,
        count = r.count ?: 0,
        paid = r.paid ?: 0.0,
        pending = r.pending ?: 0.0,
        staff = r.staff.orEmpty().map {
            TipStaffRow(it.staffId ?: 0, it.name.orEmpty(), it.count ?: 0, it.total ?: 0.0, it.paid ?: 0.0, it.pending ?: 0.0)
        },
        methods = r.methods.orEmpty().map { TipMethodRow(it.method.orEmpty(), it.count ?: 0, it.total ?: 0.0) },
    )
}

fun MyTipsDto.toDomain(): MyTips {
    val r = data ?: this
    return MyTips(
        pending = r.pending ?: 0.0,
        paid = r.paid ?: 0.0,
        recent = r.recent.orEmpty().map {
            TipRecent(it.amount ?: 0.0, it.methodCode.orEmpty(), it.createdAt.orEmpty(), (it.payoutId ?: 0) > 0)
        },
    )
}

@Singleton
class RcShareRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : RcShareRepository {
    private val api: RcShareApi
        get() = tenantRetrofitProvider.create()

    override suspend fun summary(from: String, to: String): AppResult<RcSummary> = apiCall { api.summary(from, to).toDomain() }

    override suspend fun distribute(input: RcShareInput): AppResult<Unit> = apiCall {
        api.distribute(
            RcDistributeRequestDto(
                from = input.from,
                to = input.to,
                lines = input.lines.map { RcDistributeLineDto(it.staffId, it.amount) },
                notes = input.notes.takeIf { it.isNotBlank() },
                payFromCash = input.payFromCash,
            ),
        )
        Unit
    }
}

fun RcSummaryDto.toDomain(): RcSummary {
    val r = data ?: this
    return RcSummary(
        from = r.from.orEmpty().take(10),
        to = r.to.orEmpty().take(10),
        collected = r.collected ?: 0.0,
        distributed = r.distributed ?: 0.0,
        pending = r.pending ?: 0.0,
        salesCount = r.salesCount ?: 0,
        avgRate = r.avgRate ?: 0.0,
        history = r.history.orEmpty().map { h ->
            RcDistribution(
                id = h.id ?: 0,
                periodFrom = h.periodFrom.orEmpty().take(10),
                periodTo = h.periodTo.orEmpty().take(10),
                amount = h.amount ?: 0.0,
                notes = h.notes.orEmpty(),
                paidFromCash = h.paidFromCash ?: false,
                booked = h.booked ?: false,
                createdAt = h.createdAt.orEmpty(),
                lines = h.lines.orEmpty().map { RcLine(it.staffId ?: 0, it.name.orEmpty().ifBlank { "Sin nombre" }, it.amount ?: 0.0) },
            )
        },
    )
}
