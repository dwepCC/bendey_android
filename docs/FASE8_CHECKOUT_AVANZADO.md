# Fase 8 — Checkout avanzado (pagos mixtos + descuentos)

Paridad con POS/Mesa web: varios métodos de pago y descuento en el cobro.

## Alcance entregado

| Área | Detalle |
|------|---------|
| **Pagos mixtos** | Varios métodos por venta (efectivo + tarjeta + Yape, etc.) |
| **Referencias** | Campo opcional por línea de pago |
| **Descuento** | % o monto fijo (S/) |
| **Permisos** | Descuento solo admin/supervisor o permiso `o.ch` |
| **Validación** | Suma de pagos ≥ total; caja abierta si hay efectivo |
| **API** | `discount_amount` + `payments[]` en `billSession` |

## UI checkout (Mesa + POS)

1. Serie, cliente
2. Descuento (si permitido): toggle **%** / **S/**
3. Una o más líneas de pago → **+ Agregar método de pago**
4. Restante por asignar, vuelto (si hay efectivo), total a pagar

## Probar

1. Mesa o POS con monto > 0 → **Cobrar**
2. Agregar 2 métodos (ej. 50 efectivo + resto tarjeta)
3. Con usuario cajero/admin: descuento 10% → total recalculado
4. Confirmar → venta con pagos múltiples en detalle Ventas

## Archivos clave

| Capa | Rutas |
|------|-------|
| Dominio | `CheckoutDiscount.kt`, `BillSessionInput.discountAmount` |
| UI | `CheckoutDialog.kt` |
| Soporte | `CheckoutSupport.kt` |
| VM | `MesaViewModel`, `PosViewModel` |

## Pendiente

- Notas de crédito
- SSE polling estado SUNAT post-emisión
- WebSocket tiempo real
