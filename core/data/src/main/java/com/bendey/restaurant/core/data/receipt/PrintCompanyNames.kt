package com.bendey.restaurant.core.data.receipt

import com.bendey.restaurant.core.network.dto.PrintCompanyDto

/** Nombre comercial (negrita) y razón social (texto normal), alineado con web/Tauri. */
internal data class ResolvedPrintCompanyNames(
    val commercial: String,
    val legal: String?,
)

internal fun resolvePrintCompanyNames(company: PrintCompanyDto?): ResolvedPrintCompanyNames {
    val legal = company?.businessName.orEmpty().trim().ifBlank { "Empresa" }
    val trade = company?.tradeName.orEmpty().trim()
    if (trade.isEmpty()) return ResolvedPrintCompanyNames(commercial = legal, legal = null)
    if (trade.equals(legal, ignoreCase = true)) {
        return ResolvedPrintCompanyNames(commercial = trade, legal = null)
    }
    return ResolvedPrintCompanyNames(commercial = trade, legal = legal)
}
