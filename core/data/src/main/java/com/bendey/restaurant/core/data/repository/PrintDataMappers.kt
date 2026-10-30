package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.data.receipt.resolvePrintCompanyNames
import com.bendey.restaurant.core.domain.billing.AffectationTotal
import com.bendey.restaurant.core.domain.billing.SalePrintData
import com.bendey.restaurant.core.domain.billing.SalePrintEc
import com.bendey.restaurant.core.domain.billing.SalePrintEcPayment
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
                code = it.code,
                subtotal = it.subtotal,
                modifiersJson = it.modifiersJson,
            )
        },
        subtotal = subtotal,
        taxAmount = taxAmount,
        total = total,
        serviceChargeAmount = serviceChargeAmount,
        serviceChargeRate = serviceChargeRate,
        currency = currency,
        payments = payments.map { SalePrintPayment(method = it.method, amount = it.amount) },
        amountPaid = amountPaid,
        change = change,
        legendText = legendText,
        qrData = qrData.takeIf { it.isNotBlank() },
        sunatHash = sunatHash?.takeIf { it.isNotBlank() },
        showsBendeyBranding = showsBendeyBranding != false,
        affectationTotals = totalsByAffectation.mapValues { (code, v) ->
            AffectationTotal(subtotal = v.subtotal, taxAmount = v.taxAmount)
        }.mapKeys { (code, v) -> totalsByAffectation[code]?.code?.takeIf { it.isNotBlank() } ?: code },
        accessKey = accessKey?.takeIf { it.isNotBlank() },
        fiscalEnvironment = fiscalEnvironment?.takeIf { it.isNotBlank() },
        ec = ec?.let { e ->
            SalePrintEc(
                authorizedAt = e.authorizedAt?.takeIf { it.isNotBlank() },
                matrizAddress = e.matrizAddress?.takeIf { it.isNotBlank() },
                accountingRequired = e.accountingRequired,
                specialContributor = e.specialContributor?.takeIf { it.isNotBlank() },
                withholdingAgent = e.withholdingAgent?.takeIf { it.isNotBlank() },
                rimpeLegend = e.rimpeLegend?.takeIf { it.isNotBlank() },
                clientEmail = e.clientEmail?.takeIf { it.isNotBlank() },
                payments = e.payments.map { SalePrintEcPayment(it.code, it.label, it.amount) },
            )
        },
        authorizationNumber = authorizationNumber?.takeIf { it.isNotBlank() },
        affectedDocNumber = affectedDocNumber?.takeIf { it.isNotBlank() },
        affectedIssueDate = affectedIssueDate?.takeIf { it.isNotBlank() },
        noteReason = noteReason?.takeIf { it.isNotBlank() },
        notes = notes?.takeIf { it.isNotBlank() },
        clientPhone = client?.phone?.takeIf { it.isNotBlank() },
        clientAddress = client?.address?.takeIf { it.isNotBlank() },
        clientDocType = client?.docType?.takeIf { it.isNotBlank() },
        companyAdditionalNotes = company?.additionalNotes?.takeIf { it.isNotBlank() },
        branchAddress = branch?.address?.takeIf { it.isNotBlank() },
    )
}

/** Cuantos digitos lleva el correlativo impreso: el largo que exige SUNAT. */
private const val DIGITOS_DEL_CORRELATIVO = 8

/** Ecuador (SRI): la serie es establecimiento-punto de emision «001-001» y el secuencial lleva 9 digitos. */
private const val DIGITOS_DEL_SECUENCIAL_SRI = 9
private val SERIE_SRI = Regex("[0-9]{3}-[0-9]{3}")

/**
 * El numero de un comprobante, compuesto SIEMPRE igual: serie y correlativo.
 *
 * Aqui vivia `formatPrintNumber`, que comparaba si `number` ya empezaba por la serie para decidir si
 * anteponerla. Esa duda venia de que `number` la incluye en `tenant_sales` y no en otras tablas con
 * los mismos nombres de campo, y de ella salian los «NV001-NV001-00000133».
 */
private fun numeroDeComprobante(series: String, correlative: Int): String =
    "$series-${correlative.toString().padStart(if (SERIE_SRI.matches(series)) DIGITOS_DEL_SECUENCIAL_SRI else DIGITOS_DEL_CORRELATIVO, '0')}"
