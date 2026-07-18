package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.data.receipt.resolvePrintCompanyNames
import com.bendey.restaurant.core.domain.billing.SalePrintData
import com.bendey.restaurant.core.domain.billing.SalePrintLine
import com.bendey.restaurant.core.domain.billing.SalePrintPayment
import com.bendey.restaurant.core.network.dto.PrintDataDto

internal fun PrintDataDto.toDomain(): SalePrintData {
    val names = resolvePrintCompanyNames(company)
    return SalePrintData(
        docType = docType.ifBlank { "NOTA DE VENTA" },
        sunatCode = sunatCode,
        number = formatPrintNumber(series, number),
        issueDate = issueDate,
        issueTime = issueTime?.takeIf { it.isNotBlank() },
        companyName = names.commercial,
        companyLegalName = names.legal,
        companyRuc = company?.ruc.orEmpty(),
        companyAddress = company?.address ?: branch?.address,
        companyPhone = company?.phone?.takeIf { it.isNotBlank() },
        companyEmail = company?.email?.takeIf { it.isNotBlank() },
        companyWebsite = company?.website?.takeIf { it.isNotBlank() },
        companyLogoUrl = company?.logoUrl?.takeIf { it.isNotBlank() },
        branchName = branch?.name,
        clientName = client?.businessName,
        clientDocNumber = client?.docNumber,
        items = items.map {
            SalePrintLine(
                description = it.description.ifBlank { it.code },
                quantity = it.quantity,
                unitPrice = it.unitPrice,
                total = it.total,
            )
        },
        subtotal = subtotal,
        taxAmount = taxAmount,
        total = total,
        currency = currency,
        payments = payments.map { SalePrintPayment(method = it.method, amount = it.amount) },
        legendText = legendText,
        qrData = qrData.takeIf { it.isNotBlank() },
        sunatHash = sunatHash?.takeIf { it.isNotBlank() },
        showsBendeyBranding = showsBendeyBranding != false,
    )
}

private fun formatPrintNumber(series: String, number: String): String {
    val n = number.trim()
    if (n.isBlank()) return series.trim()
    val s = series.trim()
    if (s.isNotBlank() && !n.startsWith(s)) return "$s-$n"
    return n
}
