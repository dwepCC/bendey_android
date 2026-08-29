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
        series = series,
        correlative = correlative,
        number = numeroDeComprobante(series, correlative),
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
                discount = it.discount,
                modifiersJson = it.modifiersJson,
            )
        },
        subtotal = subtotal,
        taxAmount = taxAmount,
        total = total,
        currency = currency,
        payments = payments.map { SalePrintPayment(method = it.method, amount = it.amount) },
        amountPaid = amountPaid,
        change = change,
        legendText = legendText,
        qrData = qrData.takeIf { it.isNotBlank() },
        sunatHash = sunatHash?.takeIf { it.isNotBlank() },
        showsBendeyBranding = showsBendeyBranding != false,
    )
}

/** Cuantos digitos lleva el correlativo impreso: el largo que exige SUNAT. */
private const val DIGITOS_DEL_CORRELATIVO = 8

/**
 * El numero de un comprobante, compuesto SIEMPRE igual: serie y correlativo.
 *
 * Aqui vivia `formatPrintNumber`, que comparaba si `number` ya empezaba por la serie para decidir si
 * anteponerla. Esa duda venia de que `number` la incluye en `tenant_sales` y no en otras tablas con
 * los mismos nombres de campo, y de ella salian los «NV001-NV001-00000133».
 */
private fun numeroDeComprobante(series: String, correlative: Int): String =
    "$series-${correlative.toString().padStart(DIGITOS_DEL_CORRELATIVO, '0')}"
