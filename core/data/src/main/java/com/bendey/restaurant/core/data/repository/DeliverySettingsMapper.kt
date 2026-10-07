package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.delivery.DeliveryFeeRules
import com.bendey.restaurant.core.domain.delivery.DeliverySettings
import com.bendey.restaurant.core.network.dto.DeliverySettingsDto

/**
 * DTO de ajustes de delivery -> dominio. TOLERANTE: campos ausentes o nulos toman el valor por defecto (tarifa
 * apagada, 0, afectación gravada), una afectación desconocida vuelve a gravado y un envoltorio `{data}` se
 * desenvuelve. Nunca lanza.
 */
fun DeliverySettingsDto.toDomain(): DeliverySettings {
    val r = data ?: this
    val affectation = r.feeIgvAffectation?.trim()
        ?.takeIf { code -> DeliveryFeeRules.affectations.any { it.first == code } }
        ?: DeliveryFeeRules.AFFECTATION_TAXED
    return DeliverySettings(
        feeEnabled = r.feeEnabled ?: false,
        deliveryFee = (r.deliveryFee ?: 0.0).coerceAtLeast(0.0),
        feeIgvAffectation = affectation,
        codEnabled = r.codEnabled ?: false,
        manualPaymentEnabled = r.manualPaymentEnabled ?: false,
        paymentReviewMinutes = r.paymentReviewMinutes ?: 15,
    )
}
