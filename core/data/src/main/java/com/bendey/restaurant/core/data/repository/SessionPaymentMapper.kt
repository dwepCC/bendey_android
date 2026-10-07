package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.delivery.PaymentCollector
import com.bendey.restaurant.core.domain.delivery.SessionPayment
import com.bendey.restaurant.core.network.dto.SessionPaymentDto
import com.bendey.restaurant.core.network.dto.SessionPaymentResponseDto

/**
 * Objeto `payment` (D2B_COMMON) -> dominio. TOLERANTE: montos y fechas ausentes o nulos quedan en null, los
 * textos nulos en "" y un `collected_by` sin id válido se descarta. Nunca lanza.
 */
fun SessionPaymentDto.toDomain(): SessionPayment = SessionPayment(
    mode = mode.orEmpty(),
    status = status.orEmpty(),
    expectedAmount = expectedAmount,
    tenderedAmount = tenderedAmount,
    changeAmount = changeAmount,
    tenderedInsufficient = tenderedInsufficient ?: false,
    collectedAt = collectedAt,
    collectedBy = collectedBy?.takeIf { it.id > 0 }?.let { PaymentCollector(it.id, it.name.orEmpty(), it.kind.orEmpty()) },
)

/** Respuesta de PUT /payment o POST /collect: `payment` (null = sin pago) y un envoltorio `{data}` se desenvuelve. */
fun SessionPaymentResponseDto.toPayment(): SessionPayment? = (data ?: this).payment?.toDomain()
