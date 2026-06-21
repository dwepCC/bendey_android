# Bendey Android V1 — Certificación funcional oficial

**Proyecto:** `bendey_android` (`com.bendey.restaurant`)  
**Fase:** PRODUCCIÓN (cierre de migración: 2025-06-20)  
**Cliente oficial Android:** Kotlin + Jetpack Compose  
**Cliente oficial Windows:** React + Tauri (`front_tenant_restaurant_tauri_capacitor`)

---

## Política de coexistencia

Ambos clientes son **independientes**. Comparten únicamente:

- Backend Go
- Endpoints REST
- Reglas de negocio
- Permisos (`s.m`, `c.v`, `o.ch`, etc.)
- Payloads y validaciones de API

No comparten componentes, navegación, diseño ni arquitectura de presentación.

**Criterio de certificación:** el restaurante puede realizar la misma operación de negocio y obtener el mismo resultado en backend que la referencia operativa histórica (Capacitor), sin exigir paridad visual.

---

## Estado oficial por módulo

| Módulo | Estado | Veredicto |
|--------|--------|-----------|
| Dashboard | ✅ | FUNCIONALMENTE CERTIFICADO |
| Productos | ✅ | FUNCIONALMENTE CERTIFICADO |
| POS | ✅ | FUNCIONALMENTE CERTIFICADO |
| Cocina | ✅ | FUNCIONALMENTE CERTIFICADO |
| Mesas | ✅ | FUNCIONALMENTE CERTIFICADO |
| Ventas | ✅ | FUNCIONALMENTE CERTIFICADO |
| Caja | ✅ | FUNCIONALMENTE CERTIFICADO |
| Configuración | ✅ | FUNCIONALMENTE CERTIFICADO |
| Impresión (BT/TCP) | ✅ | FUNCIONALMENTE CERTIFICADO |

**Módulos congelados:** Dashboard, Productos, Cocina, POS, Ventas, Caja, Configuración. Solo modificables por bug funcional, cambio de backend o nueva funcionalidad explícita.

---

## Resumen por módulo

### Dashboard
- KPIs operativos y catálogo con rango de fechas (timezone Lima, restricciones por rol).
- Export CSV/PDF operación y catálogo.
- Endpoints: `GET /api/restaurant/dashboard`, analytics catálogo.

### Productos
- CRUD productos, categorías, presentaciones, precios, imágenes.
- Modificadores y combos integrados.
- Endpoints: `/api/products`, `/api/categories`, `/api/modifier-groups`, `/api/combos`.

### POS
- Catálogo `catalog_only`, área preparación, scanner, combos, modificadores quantity.
- Borradores, precuenta, checkout multipago, descuento, series SUNAT, bill session.
- Impresión comanda local por área.

### Cocina
- KDS Kanban, avance de estados, mark round ready (batch a lista).
- Endpoint: `GET /api/restaurant/kitchen`, PATCH estados comanda.

### Mesas
- Grid por piso, estados backend, abrir/cerrar mesa, detalle sesión.
- Comandas, precuenta, checkout, permiso `o.ch` al cerrar mesa vacía.
- Endpoints: `/api/restaurant/tables`, `/api/restaurant/sessions`, comandas.

### Ventas
- Pestañas NV / Facturación / NC separadas.
- Anular NV, emitir FE, SUNAT send/resend, void con NC.
- Descarga PDF/XML/CDR, export listado, SSE estados billing.
- Endpoints: `/api/sales`, `/api/billing/*`.

### Caja
- Apertura/cierre/arqueo, movimientos manuales, cuentas, métodos pago.
- Reporte sesión + productos, reporte movimientos unificado, exportaciones.
- Permisos `canViewCashSettings` / `canManageCashSettings`.

### Configuración
- Empresa, SUNAT/IGV, staff, PIN operaciones, sucursales, series.
- Permiso `s.m` para admin; sin `s.m` → solo Impresoras.
- Series: bloqueo FE (SUNAT 00), locked/can_delete, correlativo.

### Impresión
- Bluetooth SPP + TCP :9100 ESC/POS.
- Slots: comandas (por área), precuenta, documentos.
- Config local DataStore (sin API backend).

---

## Permisos — mapa operativo

| Código / regla | Uso Android |
|----------------|-------------|
| `s.m` | Admin restaurante, configuración completa |
| `c.v` | Caja, dashboard, ver config caja (solo lectura) |
| `o.ch` | Ventas, cobrar, cerrar mesa, clientes |
| `p.u` | POS |
| `t.v` / `t.o` | Salas / operar mesa |
| `k.v` | Cocina |
| `g.p` | Productos, modificadores, mesas admin |
| `d.v` | Repartidores |
| `canConfigureDevicePrinters` | Impresoras (roles operativos + permisos) |
| `canManageRestaurantSettings` | Solo `s.m` |

---

## Backend — contratos verificados

Todos los módulos certificados utilizan los mismos paths HTTP, query params y bodies documentados en `docs/CERTIFICACION_FUNCIONAL_MODULOS.md` (detalle por módulo POS, Ventas, Caja, Configuración).

Headers obligatorios en todas las llamadas autenticadas:
- `Authorization: Bearer {token}`
- `X-Tenant-Slug: {slug}`

---

## Mejoras Android conservadas (no bloquean certificación)

- Caja obligatoria para cobro de cajeros (más estricto que Capacitor en efectivo).
- Reenvío SUNAT en estado `rejected`.
- Compartir comprobante vía WhatsApp/PDF nativo.
- Export CSV compartible en lugar de `.xlsx` en algunos reportes.

---

## Documentos relacionados

| Documento | Propósito |
|-----------|-----------|
| `CERTIFICACION_FUNCIONAL_MODULOS.md` | Detalle técnico por módulo (histórico certificación) |
| `QA_CHECKLIST_PRODUCCION.md` | Checklist QA pre-release |
| `KNOWN_LIMITATIONS.md` | Limitaciones conocidas no bloqueantes |
| `README.md` | Build y flujo de prueba manual |

---

## Veredicto final

```
======================================================
Bendey Android V1
ESTADO:
LISTO PARA PRODUCCIÓN

La aplicación cumple las operaciones funcionales requeridas
para operar un restaurante utilizando el backend oficial.

Las diferencias restantes corresponden únicamente a decisiones
de implementación propias de Android y no afectan las reglas
de negocio ni la operación diaria.
======================================================
```

---

## Política post-certificación

A partir del cierre oficial:

- **NO** auditar paridad visual con React/Capacitor.
- **NO** proponer cambios por diseño, UX, arquitectura, Material3, optimizaciones.
- **SÍ** cambiar solo ante: bug funcional, nueva funcionalidad, cambio backend, cambio legal/fiscal, solicitud de producto.
