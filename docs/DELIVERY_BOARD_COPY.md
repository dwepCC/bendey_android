# Delivery: textos y umbrales de la vista Delivery (D1)

Fuente: `src/utils/deliveryBoardLogic.ts` (Tauri). Android (`core/domain/.../delivery/DeliveryBoardLogic.kt`) replica ESTAS
claves, textos y valores tal cual. `deliveryBoardLogic.test.ts` falla si este documento y el código se desalinean
(regenerar: `BENDEY_WRITE_DELIVERY_COPY=1 npx vitest run src/utils/deliveryBoardLogic.test.ts`).

## Formato

- Dos tablas de dos columnas: `| \`clave\` | valor |`. Una fila por línea; el texto va literal (sin escapar).
- `{n}` en un texto es un marcador que se reemplaza por el número.
- Minutos = minutos enteros transcurridos (suelo). Formato de tiempo: `7 min` / `1 h 05 min` (igual que el KDS).

## Reglas del semáforo

- Con `estimated_minutes` > 0: verde si transcurrido/estimado < `estimatedAmberFrom`; ámbar de ahí hasta `estimatedRedAbove` (incluido); rojo por encima.
- Sin estimado: verde < `fallbackAmberMin`; ámbar de `fallbackAmberMin` a `fallbackRedMin` (incluido); rojo por encima.
- "Asignados" (asignación en estado `assigned`): alerta ámbar desde `acceptAmberMin` min sin aceptar, roja desde `acceptRedMin`. El color de la tarjeta es el peor entre el reloj y esta alerta.
- "Entregados hoy": sin semáforo. El reloj cuenta desde `created_at`.
- Orden: más antiguo primero (`created_at`); "Entregados hoy": el más reciente primero (`delivered_at`).
- Refetch por evento en tiempo real: coalescido `refetchDebounceMs`; respaldo cada `backupPollMs` SOLO si el tiempo real está caído.
- Sonido: solo cuando aparece en "Por asignar" un pedido que no estaba; nunca en la primera carga.

## Umbrales

| Clave | Valor |
|---|---|
| `estimatedAmberFrom` | 0.6 |
| `estimatedRedAbove` | 1 |
| `fallbackAmberMin` | 20 |
| `fallbackRedMin` | 35 |
| `acceptAmberMin` | 5 |
| `acceptRedMin` | 10 |
| `backupPollMs` | 60000 |
| `refetchDebounceMs` | 400 |
| `reasonMin` | 3 |
| `reasonMax` | 255 |

## Textos

| Clave | Texto |
|---|---|
| `title` | Entregas |
| `subtitle` | Pedidos para llevar a domicilio: quién los lleva y cómo van. |
| `section.unassigned` | Por asignar |
| `section.assigned` | Asignados |
| `section.in_transit` | En camino |
| `section.incidents` | Incidencias |
| `section.delivered_today` | Entregados hoy |
| `empty.unassigned` | No hay pedidos por asignar. |
| `empty.assigned` | Ningún pedido espera respuesta de un repartidor. |
| `empty.in_transit` | No hay pedidos en camino. |
| `empty.incidents` | Sin incidencias. Todo en orden. |
| `empty.delivered_today` | Todavía no se entregó ningún pedido hoy. |
| `source.staff` | POS |
| `source.digital_menu` | Menú digital |
| `source.marketplace` | Marketplace |
| `kitchen.draft` | Sin enviar |
| `kitchen.pending` | Por confirmar |
| `kitchen.sent_to_kitchen` | En cocina |
| `kitchen.preparing` | Preparando |
| `kitchen.ready` | Listo |
| `kitchen.on_the_way` | En camino |
| `kitchen.delivered` | Entregado |
| `kitchen.cancelled` | Cancelado |
| `assignment.assigned` | Esperando que acepte |
| `assignment.accepted` | Aceptado |
| `assignment.picked_up` | Recogido |
| `assignment.on_the_way` | En camino |
| `assignment.delivered` | Entregado |
| `assignment.failed` | Incidencia |
| `assignment.rejected` | Rechazado |
| `assignment.cancelled` | Cancelado |
| `chip.paid` | Pagado |
| `chip.no_phone` | Sin teléfono |
| `incident.failed` | No se pudo entregar |
| `incident.rejected` | El repartidor lo rechazó |
| `alert.accept_amber` | Sin aceptar |
| `alert.accept_red` | Sin aceptar hace rato |
| `action.assign` | Asignar |
| `action.reassign` | Reasignar |
| `action.call` | Llamar |
| `action.cancel` | Cancelar pedido |
| `action.delivered` | Marcar entregado |
| `action.failed` | Marcar fallido |
| `action.refresh` | Actualizar |
| `assign.title` | Elegir repartidor |
| `assign.none` | No hay repartidores registrados. Créalos en Mi negocio → Repartidores. |
| `driver.available` | Disponible |
| `driver.unavailable` | No disponible |
| `driver.unavailable_reason` | Marcado como no disponible |
| `driver.active_zero` | Sin pedidos activos |
| `driver.active_one` | 1 pedido activo |
| `driver.active_many` | {n} pedidos activos |
| `cancel.title` | Cancelar pedido |
| `cancel.reason_required` | Escribe el motivo de la cancelación (mínimo 3 letras). |
| `cancel.reason_other` | Otro |
| `cancel.reason_1` | El cliente canceló |
| `cancel.reason_2` | No hay stock |
| `cancel.reason_3` | Dirección fuera de zona |
| `cancel.reason_4` | No contesta |
| `failed.title` | Marcar como fallido |
| `failed.reason_required` | Escribe el motivo por el que no se pudo entregar (mínimo 3 letras). |
| `failed.reason_1` | El cliente no contesta |
| `failed.reason_2` | No encontramos la dirección |
| `failed.reason_3` | El cliente rechazó el pedido |
| `delivered.confirm` | Se marcará como entregado. El cliente ya recibió su pedido. |
| `ok.assigned` | Repartidor asignado. |
| `ok.cancelled` | Pedido cancelado. |
| `ok.delivered` | Pedido marcado como entregado. |
| `ok.failed` | Pedido marcado como fallido. |
| `drivers.title` | Repartidores |
| `drivers.empty` | No hay repartidores activos. |
| `readonly.hint` | Solo puedes ver los pedidos. Para asignar o cambiar su estado pide permiso al administrador. |
| `load_error` | No se pudo cargar Delivery. Revisa tu conexión e intenta de nuevo. |
| `badge.aria_one` | 1 pedido de delivery por asignar |
| `badge.aria_many` | {n} pedidos de delivery por asignar |
| `toast.new_unassigned` | Nuevo pedido de delivery por asignar |
