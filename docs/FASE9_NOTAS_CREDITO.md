# Fase 9 — Notas de crédito y anulaciones

Paridad con Ventas web: filtros por tipo de comprobante y anulación con NC / NV.

## Alcance entregado

| Área | Detalle |
|------|---------|
| **Pestañas** | Todas · Notas venta · Boletas/Facturas · Notas crédito |
| **Filtros API** | `sunat_code=00`, `01,03`, `doc_type=NOTA_CREDITO` |
| **Anular FE** | `POST /api/billing/void-with-credit-note/{id}` + motivo |
| **Anular NV** | `POST /api/sales/{id}/cancel` + motivo |
| **Detalle** | Botones según elegibilidad (aceptada SUNAT / NV sin convertir) |

## Flujo nota de crédito

1. Ventas → pestaña **Boletas/Facturas** (o Todas)
2. Abrir venta con estado billing **accepted**
3. **Anular con nota de crédito** → motivo → confirmar
4. Backend encola NC; al aceptar SUNAT anula la venta original
5. Tras éxito, la app cambia a pestaña **Notas crédito**

## Flujo anular nota de venta

1. Ventas → **Notas venta**
2. Abrir NV no convertida ni anulada
3. **Anular nota de venta** → motivo → confirmar

## Archivos clave

| Capa | Rutas |
|------|-------|
| API | `BillingApi`, `SalesApi.cancelNota` |
| Dominio | `SaleVoidRules.kt`, `VentasTab` |
| UI | `VentasScreen`, `VentasViewModel` |

## Pendiente

- SSE estado SUNAT post-NC
- Convertir NV → boleta/factura desde app
- Envío/reenvío manual a SUNAT
