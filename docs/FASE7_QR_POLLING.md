# Fase 7 — QR SUNAT + polling operativo

Paridad con la web: QR en boletas/facturas electrónicas y actualización automática de cocina/mesas.

## Alcance entregado

| Área | Detalle |
|------|---------|
| **QR SUNAT** | `qr_data` + `sunat_hash` en `print_data` → ESC/POS QR en CPE 01/03/07/08 |
| **Etiquetas** | Título SUNAT (Boleta/Factura electrónica) en ticket |
| **Polling** | Auto-refresh cada 15 s en Cocina, Mesas y detalle Mesa |
| **Lifecycle** | Polling solo con pantalla visible (ON_START / ON_STOP) |

## QR en ticket

Se imprime cuando:

- `sunat_code` es `01`, `03`, `07` u `08`
- `qr_data` viene en la respuesta `print_data` del backend

Incluye pie: hash (si existe), “Representación impresa CPE”, “Consulte en sunat.gob.pe”.

**Probar:** Cobrar con serie **Boleta** o **Factura** electrónica (tenant con FE activa) → ticket con QR en impresora documentos.

## Polling

- **Cocina:** KDS se actualiza cada 15 s sin spinner (pull-to-refresh manual sigue disponible)
- **Mesas:** grid de mesas se refresca en background
- **Detalle mesa:** sesión/comandas se sincronizan mientras la mesa está abierta
- No interrumpe acciones en curso (envío comanda, checkout, avance KDS)

## Archivos clave

| Capa | Rutas |
|------|-------|
| QR ESC/POS | `EscPosQr.kt`, `SunatPrintUtils.kt`, `DocumentLayoutBuilder.kt` |
| API/dominio | `PrintDataDto.qr_data`, `SalePrintData` |
| Polling | `PollingConfig.kt`, `AutoRefreshHandle.kt` |
| UI | `CocinaViewModel/Screen`, `MesasViewModel/Screen`, `MesaViewModel/Screen` |

## Pendiente

- SSE billing post-emisión (`/api/billing/events`)
- Pagos mixtos y descuentos en checkout
- Notas de crédito
- WebSocket KDS/mesas (sustituir polling)
