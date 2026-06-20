# Fase 6 — Impresoras multi-slot + detalle ventas

Paridad con la web: tres impresoras independientes y consulta/reimpresión de ventas.

## Alcance entregado

| Área | Detalle |
|------|---------|
| **3 slots** | Comandas, Precuenta, Documentos (DataStore) |
| **Migración** | Config legacy se aplica a los 3 slots hasta reconfigurar |
| **Servicios** | Comanda → slot comandas; precuenta → precuenta (fallback comandas); documento → documentos (fallback comandas) |
| **Ajustes** | Pantalla Impresoras con selector de slot + prueba documento |
| **Ventas** | Tap en venta → detalle (ítems, pagos) |
| **Reimpresión** | Botón reimprimir ticket desde detalle (`print_data`, force) |

## Configuración impresoras

Dashboard → **Prueba de impresión** (ahora **Impresoras**):

1. Elegir slot: Comandas / Precuenta / Documentos
2. Configurar BT o TCP para cada una
3. Toggles auto-impresión comandas y documentos
4. Probar cada tipo con botones de prueba

## Ventas

1. Tab **Ventas** → tocar una fila
2. Sheet con detalle, totales y pagos
3. **Reimprimir ticket** si hay `print_data`

## Archivos clave

| Capa | Rutas |
|------|-------|
| Prefs | `PrinterSlotConfig.kt`, `PrinterPreferencesStore.kt` |
| Mapper | `PrintDataMappers.kt` |
| API | `SalesApi.getSale`, `SaleDetailResponseDto` |
| UI | `PrinterTestScreen`, `VentasScreen` |

## Pendiente

- Pagos mixtos / descuentos checkout
- FE SUNAT, notas de crédito
