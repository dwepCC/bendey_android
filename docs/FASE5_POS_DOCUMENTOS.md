# Fase 5 — POS checkout + impresión documento

Cierra el ciclo operativo de mostrador: cobro desde POS e impresión automática del ticket de venta.

## Alcance entregado

| Área | Detalle |
|------|---------|
| **Checkout POS** | Botón **Cobrar** en barra de sesión y carrito |
| **Flujo** | Abre sesión si no existe → envía carrito pendiente → `billSession` → resetea venta |
| **UI compartida** | `CheckoutDialog` / `CheckoutSuccessDialog` en `core:ui` (mesa + POS) |
| **print_data** | Respuesta `billSession` parseada a `SalePrintData` |
| **Impresión ticket** | `DocumentLayoutBuilder` ESC/POS + `DocumentPrintService` |
| **Auto-print** | Pref `auto_print_documents` (usa misma impresora configurada en Ajustes) |

## Flujo POS

1. Agregar productos → **Enviar comanda** (opcional, acumula total de sesión).
2. **Cobrar** cuando hay monto (sesión + carrito pendiente).
3. Modal checkout: serie, cliente, pago.
4. Venta generada → diálogo éxito → auto-impresión si hay impresora.
5. POS queda listo para nueva venta.

## Flujo mesa (mejora Fase 5)

- Tras cobrar, intenta imprimir documento con `print_data` del backend.
- Mensaje en diálogo de éxito si la impresión falla o no hay impresora.

## Archivos clave

| Capa | Rutas |
|------|-------|
| UI compartida | `core/ui/checkout/CheckoutDialog.kt` |
| Impresión | `platform/printing/escpos/DocumentLayoutBuilder.kt` |
| Servicio | `core/data/printer/DocumentPrintService.kt` |
| API | `PrintDataDto`, `BillSessionResponseDto.print_data` |
| POS | `PosViewModel`, `PosScreen` |
| Mesa | `MesaViewModel` (auto-print doc) |

## Prueba manual

1. Configurar impresora BT/TCP en Dashboard → Prueba impresión.
2. **POS** → productos → comanda → **Cobrar** → confirmar.
3. Verificar ticket impreso y venta en tab Ventas.
4. **Mesa** → cobrar → verificar ticket y cierre de mesa.

## Pendiente

- QR SUNAT en ticket electrónico
- Pagos mixtos, descuentos checkout
- FE SUNAT / notas de crédito
