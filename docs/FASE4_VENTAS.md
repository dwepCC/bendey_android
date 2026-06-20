# Fase 4 — Ventas / Facturación (MVP)

Checkout desde mesa y listado de ventas recientes. Misma API que `bendey_tenant_restaurante`.

## Alcance entregado

| Área | Detalle |
|------|---------|
| **API** | `POST /api/restaurant/sessions/{id}/bill`, series, contactos, métodos de pago, `GET /api/sales` |
| **Dominio** | `BillingRepository`, `SalesRepository` |
| **Mesa** | Botón **Cobrar** → modal serie/cliente/pago → cierra sesión |
| **Ventas** | Tab **Ventas** — listado del mes en curso |
| **Caja** | Si el pago es efectivo, exige caja abierta (`cash_session_id`) |

## Flujo checkout mesa

1. En detalle mesa, **Cobrar** (barra superior junto a Precuenta).
2. Se cargan series (`category=venta`), clientes y métodos de pago.
3. Defaults: NV / Nota de venta, cliente VARIOS, primer método configurado.
4. Si hay ítems en carrito, se envían como comanda antes de facturar (igual que web).
5. `billSession` con `close_session: true`.
6. Diálogo de éxito → vuelve al grid de mesas.

## Archivos clave

| Capa | Rutas |
|------|-------|
| DTOs | `core/network/dto/BillingDtos.kt` |
| APIs | `CompanyApi`, `ContactsApi`, `SalesApi`, `RestaurantApi.billSession`, `CashbankApi.listPaymentMethods` |
| Repos | `BillingRepositoryImpl`, `SalesRepositoryImpl` |
| UI mesa | `MesaCheckoutDialog.kt`, `MesaViewModel` (checkout), `MesaScreen` |
| UI ventas | `feature:ventas` — `VentasScreen`, `VentasViewModel` |
| Nav | `BendeyRoutes.VENTAS`, `TopLevelDestination.VENTAS` |

## Prueba manual

1. Abrir caja si vas a cobrar en efectivo.
2. Mesas → mesa ocupada → agregar consumo → **Cobrar**.
3. Confirmar → verificar documento y que la mesa quede libre.
4. Ventas → ver la venta en el listado del mes.

## Pendiente (fases posteriores)

- Impresora dedicada documentos (slot separado)
- QR SUNAT en ticket electrónico
- FE SUNAT, NC, filtros avanzados en ventas
- Descuentos en checkout, pagos mixtos
- Alta rápida de cliente
