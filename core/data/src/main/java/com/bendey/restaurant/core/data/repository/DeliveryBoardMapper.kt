package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.delivery.DeliveryBoardCounts
import com.bendey.restaurant.core.domain.delivery.DeliveryBoardData
import com.bendey.restaurant.core.domain.delivery.DeliveryBoardDriver
import com.bendey.restaurant.core.domain.delivery.DeliveryCard
import com.bendey.restaurant.core.domain.delivery.DeliveryIncident
import com.bendey.restaurant.core.domain.delivery.DeliveryPerson
import com.bendey.restaurant.core.domain.delivery.deliveryBoardCounts
import com.bendey.restaurant.core.network.dto.DeliveryBoardDriverDto
import com.bendey.restaurant.core.network.dto.DeliveryBoardDto
import com.bendey.restaurant.core.network.dto.DeliveryCardDto

/**
 * DTO del tablero de Delivery -> dominio. TOLERANTE: arrays ausentes o nulos son listas vacías, textos nulos
 * son "", `counts` ausente se calcula con las longitudes (mismo criterio que `normalizeBoard` de Tauri) y un
 * envoltorio `{data}` se desenvuelve. Una tarjeta sin `session_id` válido se descarta (no se puede operar).
 */
fun DeliveryBoardDto.toDomain(): DeliveryBoardData {
    val r = data ?: this
    val base = DeliveryBoardData(
        generatedAt = r.generatedAt.orEmpty(),
        counts = DeliveryBoardCounts(
            unassigned = r.counts?.unassigned ?: 0,
            assigned = r.counts?.assigned ?: 0,
            inTransit = r.counts?.inTransit ?: 0,
            incidents = r.counts?.incidents ?: 0,
            deliveredToday = r.counts?.deliveredToday ?: 0,
        ),
        unassigned = r.unassigned.cards(),
        assigned = r.assigned.cards(),
        inTransit = r.inTransit.cards(),
        incidents = r.incidents.cards(),
        deliveredToday = r.deliveredToday.cards(),
        drivers = r.drivers.orEmpty().filter { it.id > 0 }.map { it.toDomain() },
    )
    return base.copy(counts = deliveryBoardCounts(base))
}

private fun List<DeliveryCardDto>?.cards(): List<DeliveryCard> =
    orEmpty().filter { it.sessionId > 0 }.map { it.toDomain() }

private fun DeliveryCardDto.toDomain() = DeliveryCard(
    sessionId = sessionId,
    assignmentId = assignmentId?.takeIf { it > 0 },
    source = source.orEmpty(),
    customerName = customerName.orEmpty(),
    customerPhone = customerPhone.orEmpty(),
    address = address.orEmpty(),
    reference = reference.orEmpty(),
    itemsCount = itemsCount ?: 0,
    totalAmount = totalAmount ?: 0.0,
    orderStatus = orderStatus.orEmpty(),
    createdAt = createdAt,
    sentToKitchenAt = sentToKitchenAt,
    readyAt = readyAt,
    estimatedMinutes = estimatedMinutes,
    driver = driver?.takeIf { it.id > 0 }?.let { DeliveryPerson(it.id, it.name.orEmpty(), it.phone.orEmpty()) },
    assignmentStatus = assignmentStatus,
    assignedAt = assignedAt,
    acceptedAt = acceptedAt,
    pickedUpAt = pickedUpAt,
    onTheWayAt = onTheWayAt,
    deliveredAt = deliveredAt,
    incident = incident?.let { DeliveryIncident(it.kind.orEmpty(), it.reason.orEmpty(), it.at) },
    paid = paid ?: false,
)

private fun DeliveryBoardDriverDto.toDomain() = DeliveryBoardDriver(
    id = id,
    name = name.orEmpty(),
    phone = phone.orEmpty(),
    vehicleType = vehicleType.orEmpty(),
    // Si el servidor no dice nada, se asume disponible: el backend igual valida al asignar.
    isAvailable = isAvailable ?: true,
    activeCount = activeCount ?: 0,
)
