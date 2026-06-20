# Fase 2.1 — Mesa operativa

> **Estado:** Implementada  
> **Fecha:** 2025-06-19  
> **Prerequisito:** Fase 2 (POS · Mesas · Cocina)

---

## Alcance entregado

| Funcionalidad | Descripción |
|---------------|-------------|
| **Navegación a mesa** | Tocar mesa ocupada/en consumo → `mesa/{sessionId}` |
| **Abrir mesa → detalle** | Tras abrir mesa libre, navega automáticamente al detalle |
| **POS en mesa** | Catálogo + carrito anclado a sesión existente |
| **Enviar comanda** | `POST /sessions/{id}/orders` + auto-impresión ESC/POS |
| **Historial** | Pedidos/comandas de la sesión con estados |
| **Precuenta** | `GET /precuenta` + impresión BT/TCP |

**Pendiente (Fase 3):** Caja, facturación SUNAT (`billSession`), anulaciones, modificadores.

---

## Rutas

| Ruta | Pantalla |
|------|----------|
| `mesas` | Grid de mesas |
| `mesa/{sessionId}` | Detalle operativo (POS en mesa) |

---

## APIs nuevas

| Endpoint | Uso |
|----------|-----|
| `GET /api/restaurant/sessions/{id}` | Detalle sesión + pedidos |
| `GET /api/restaurant/sessions/{id}/precuenta` | Líneas y total para ticket |

---

## Flujo de prueba

1. **Mesas** → abrir mesa libre → entra al detalle automáticamente.
2. Agregar productos → **Enviar comanda** → verificar cocina + ticket.
3. **Precuenta** → imprimir (requiere impresora configurada en Fase 1.5).
4. Volver → tocar mesa ocupada → reabre la misma sesión.

---

## Archivos clave

| Área | Ruta |
|------|------|
| DTOs | `SessionDetailDto`, `PrecuentaDto` en `RestaurantDtos.kt` |
| Repo | `MesasRepositoryImpl.getSession`, `getPrecuenta` |
| Print | `KitchenPrintService.printPrecuenta` |
| UI | `MesaScreen.kt`, `MesaViewModel.kt` |
| Nav | `BendeyRoutes.MESA`, `MesasNavigation.kt` |

---

## Siguiente fase

**Fase 3 — Caja:** apertura de sesión de caja, movimientos, cierre, integración con `billSession`.
