# Bendey Resto — Android Nativo

Cliente Android independiente (`com.bendey.restaurant`) que consume la misma API que `bendey_tenant_restaurante`.

## Fase 1 ✅

- Arquitectura multi-módulo Clean + MVVM
- Design System light-only + paleta colorida
- Navegación adaptativa
- Network + Session layer
- Auth: RUC → Home → PIN → Dashboard

## Fase 1.5 ✅

- `platform/printing` — ESC/POS comanda + precuenta (port desde TypeScript)
- Bluetooth SPP + TCP/IP :9100
- Pantalla prueba: Dashboard → "Prueba de impresión"
- Docs: [`docs/FASE1_5_IMPRESION.md`](docs/FASE1_5_IMPRESION.md)

## Fase 2 ✅ — POS · Mesas · Cocina

- `feature:pos` — catálogo, carrito, enviar comanda (mostrador / para llevar)
- `feature:mesas` — grid por piso, estados backend, abrir mesa
- `feature:cocina` — KDS Kanban, avance de estado
- Auto-impresión comanda tras enviar desde POS (si impresora configurada)
- Docs: [`docs/FASE2_OPERACION.md`](docs/FASE2_OPERACION.md)

## Design System v1.1 + UI operativo ✅

- **Solo tema claro** — sin dark mode
- Primario: `#C62828` Tomate Bendey
- Acentos: teal, morado, verde, amarillo en KPIs y estados
- Shell: header global + bottom nav en rutas principales
- Toolbars secundarias: `BendeyScreenToolbar` (gestión y detalle mesa)
- Catálogo POS/Mesa: `BendeyPosCatalogPane` + imágenes Coil
- Docs: [`docs/FASE_UI_UX.md`](docs/FASE_UI_UX.md), [`docs/FASE_UX_REFINEMENT.md`](docs/FASE_UX_REFINEMENT.md) (capturas), spec [`docs/UI_UX_ANDROID_V1.md`](docs/UI_UX_ANDROID_V1.md)
- Componentes: `BendeyKpiCard`, `BendeyStatusChip`, `BendeyTableCard`, `BendeyProductCard`

## Módulos

```
app/
core/designsystem, ui, navigation, network, domain, data, realtime
platform/printing
feature/auth, dashboard, printing, pos, mesas, cocina, caja, ventas, productos, clientes, modificadores, combos, configuracion, repartidores
```

## Build

```bash
cd restaurante_kotlin
./gradlew :app:assembleDebug
./gradlew :platform:printing:testDebugUnitTest
```

## Flujo de prueba

1. RUC → vincular tenant
2. PIN o login admin
3. Dashboard colorido con KPIs
4. Prueba de impresión → comanda / precuenta (BT o TCP)
5. POS → agregar productos → Enviar comanda
6. Mesas → abrir mesa libre → detalle mesa → comanda + precuenta
7. Cocina → avanzar estados KDS
8. Caja → abrir turno → ingreso/egreso → cerrar
9. Mesa ocupada → **Cobrar** → venta generada y mesa cerrada
10. Ventas → listado del mes
11. POS → **Cobrar venta** → ticket impreso (si configurado)
12. Impresoras → configurar slots comandas / precuenta / documentos
13. Ventas → detalle → **Reimprimir ticket**
14. Boleta/Factura electrónica → ticket con **QR SUNAT**
15. Cobrar → **pagos mixtos** (efectivo + tarjeta) y **descuento** (si tienes permiso)
16. Ventas → pestañas NV / Facturación / NC; estados SUNAT en vivo (SSE) en facturación
17. Ventas → **Anular con nota de crédito** (boleta/factura aceptada) o **Anular NV**
18. Dashboard → **Gestión** → Ventas, Caja, Productos, Clientes, Configuración, Repartidores

## Fase 2.1 ✅ — Mesa operativa

- Detalle mesa `mesa/{sessionId}` — POS anclado a sesión
- Enviar comanda desde mesa + auto-impresión
- Precuenta BT/TCP
- Docs: [`docs/FASE2_1_MESA.md`](docs/FASE2_1_MESA.md)

## Fase 3 ✅ — Caja

- Apertura / cierre de sesión de caja por sucursal
- Ingresos y egresos manuales
- Saldo estimado + movimientos del turno
- Persistencia local `CashSessionSnapshot`
- Docs: [`docs/FASE3_CAJA.md`](docs/FASE3_CAJA.md)

## Fase 4 ✅ — Ventas / Facturación (MVP)

- Checkout **Cobrar** en detalle mesa (`billSession`)
- Series, clientes y métodos de pago desde API tenant
- Validación caja abierta para pagos en efectivo
- Tab **Ventas** con listado reciente
- Docs: [`docs/FASE4_VENTAS.md`](docs/FASE4_VENTAS.md)

## Fase 5 ✅ — POS checkout + documentos

- **Cobrar** en POS mostrador / para llevar
- Checkout compartido (`core:ui/checkout`)
- Auto-impresión ticket ESC/POS tras venta (`print_data`)
- Docs: [`docs/FASE5_POS_DOCUMENTOS.md`](docs/FASE5_POS_DOCUMENTOS.md)

## Fase 6 ✅ — Impresoras + detalle ventas

- 3 slots impresora: comandas, precuenta, documentos
- Detalle venta + reimprimir ticket
- Docs: [`docs/FASE6_IMPRESORAS_VENTAS.md`](docs/FASE6_IMPRESORAS_VENTAS.md)

## Fase 7 ✅ — QR SUNAT

- QR en boleta/factura electrónica (ESC/POS)
- Docs: [`docs/FASE7_QR_POLLING.md`](docs/FASE7_QR_POLLING.md) *(sin polling; ver alineación)*

## Fase 8 ✅ — Pagos mixtos + descuentos

- Checkout con varios métodos de pago y referencia
- Descuento % o monto (según permiso)
- Docs: [`docs/FASE8_CHECKOUT_AVANZADO.md`](docs/FASE8_CHECKOUT_AVANZADO.md)

## Fase 9 ✅ — Notas de crédito

- Pestañas ventas: NV, boletas/facturas, notas crédito
- Anular FE con nota de crédito (motivo)
- Anular nota de venta
- Docs: [`docs/FASE9_NOTAS_CREDITO.md`](docs/FASE9_NOTAS_CREDITO.md)

## Alineación Capacitor ✅ (parcial)

- Barra inferior: 4 ítems operativos (Dashboard, POS, Mesas, Comandas)
- Gestión desde Dashboard: Ventas, Caja
- Ventas: pestañas NV / Facturación / NC (sin mezclar)
- SSE billing en Ventas (estados SUNAT en vivo, sin esperar en checkout)
- Sin auto-refresh polling en cocina/mesas
- POS móvil: layout compacto mejorado
- Docs: [`docs/ALINEACION_CAPACITOR.md`](docs/ALINEACION_CAPACITOR.md)

## Fase 10 ✅ — Productos (gestión)

- Listado con búsqueda, filtros y paginación
- CRUD productos (nombre, precio, categoría, área, IGV, carta, stock)
- CRUD categorías (pestaña dedicada)
- Acceso: Dashboard → Gestión → Productos
- Docs: [`docs/FASE10_PRODUCTOS.md`](docs/FASE10_PRODUCTOS.md)

## Fase 11 ✅ — Clientes (gestión)

- Listado con búsqueda debounced
- CRUD clientes + activar/desactivar
- Consulta DNI/RUC SUNAT
- Acceso: Dashboard → Gestión → Clientes
- Docs: [`docs/FASE11_CLIENTES.md`](docs/FASE11_CLIENTES.md)

## Fase 10.1 ✅ — Catálogo extendido

- Productos: imagen, import Excel (+ plantilla descargable), presentaciones, grupos modificadores
- Modificadores y Combos (pantallas dedicadas con navegación cruzada)
- **Editor combos completo:** tipos (fijo, configurable, promo, familiar, arma tu combo), productos fijos con búsqueda, slots con min/max y upgrade, sucursales, fechas promo
- Configuración: empresa, **SUNAT editable** (IGV, régimen, zona beneficio), sucursales, PIN anulación, impresoras
- Repartidores: motorizados y empresas delivery
- Docs: [`docs/FASE10_1_CATALOGO.md`](docs/FASE10_1_CATALOGO.md)

## Próximo

- WebSocket tiempo real (cuando se implemente en Capacitor)
