# Fase 2 — POS · Mesas · Cocina

> **Estado:** Implementada  
> **Fecha:** 2025-06-19  
> **Prerequisito:** Fase 1 + Fase 1.5 (impresión validada físicamente)

---

## Alcance entregado

| Módulo | Pantalla | Funcionalidad V1 |
|--------|----------|------------------|
| `feature:pos` | POS | Catálogo, categorías, búsqueda, carrito, mostrador/para llevar, enviar comanda |
| `feature:mesas` | Mesas | Pisos, grid con estados backend, stats, abrir mesa |
| `feature:cocina` | Cocina | KDS Kanban 4 columnas, avance de estado, pull-to-refresh |
| `core:data` | — | Repositorios POS/Mesas/Cocina + prefs impresora + auto-print comanda |

**Pendiente (Fase 3):** Caja, checkout/facturación SUNAT, modificadores, escáner, delivery completo.

Ver también: [`FASE2_1_MESA.md`](FASE2_1_MESA.md) — detalle mesa + precuenta.

---

## Módulos Gradle añadidos

```
feature/pos/
feature/mesas/
feature/cocina/
```

`settings.gradle.kts` incluye los tres módulos. Nav principal habilita POS, Mesas y Cocina (`enabledInV1 = true`). Caja sigue deshabilitada.

---

## APIs consumidas (sin cambios de contrato)

| Endpoint | Uso |
|----------|-----|
| `GET /api/products` | Catálogo POS (`catalog_only`, paginado) |
| `GET /api/categories` | Filtros POS |
| `POST /api/restaurant/sessions` | Abrir mostrador / mesa |
| `POST /api/restaurant/sessions/{id}/orders` | Enviar comanda |
| `GET /api/restaurant/floors` | Pisos |
| `GET /api/restaurant/tables` | Grid mesas |
| `GET /api/restaurant/staff` | Mozos (abrir mesa) |
| `GET /api/restaurant/kitchen` | KDS |
| `PUT /api/restaurant/comandas/{id}/status` | Avanzar estado cocina |

---

## Impresión integrada

1. Configura impresora en **Dashboard → Prueba de impresión** (Fase 1.5).
2. Al imprimir exitosamente, se guarda en DataStore (`PrinterPreferencesStore`).
3. Al **Enviar comanda** desde POS, si hay impresora configurada y `autoPrintComandas = true`, se imprime ticket ESC/POS vía `KitchenPrintService`.

---

## UI / Design system

- **Mesas:** `BendeyTableCard`, `BendeyTableStatsRow`, colores `TableStatus` (backend exacto).
- **Cocina:** columnas Kanban con headers coloreados (`ComandaStatus`).
- **POS:** two-pane en tablet (≥ medium width), bottom sheet carrito en phone.

Ver `docs/UI_UX_ANDROID_V1.md` v1.1.

---

## Cómo probar

1. Login (RUC + PIN) como en Fase 1.
2. **POS:** agregar productos → Enviar comanda → verificar en cocina web/backend.
3. **Mesas:** filtrar piso → tocar mesa libre → Abrir mesa.
4. **Cocina:** pull refresh → Avanzar ítems entre columnas.
5. **Impresión:** configurar BT/TCP en prueba → enviar comanda POS → ticket físico.

---

## Archivos clave

| Área | Ruta |
|------|------|
| DTOs/API | `core/network/dto/RestaurantDtos.kt`, `ProductsDtos.kt`, `api/Apis.kt` |
| Repositorios | `core/data/repository/RestaurantRepositories.kt` |
| Print prefs | `core/data/printer/PrinterPreferencesStore.kt` |
| Auto-print | `core/data/printer/KitchenPrintService.kt` |
| POS | `feature/pos/PosScreen.kt`, `PosViewModel.kt` |
| Mesas | `feature/mesas/MesasScreen.kt`, `MesasViewModel.kt` |
| Cocina | `feature/cocina/CocinaScreen.kt`, `CocinaViewModel.kt` |
| Nav | `TopLevelDestination.kt`, `BendeyAppNavHost.kt` |

---

## Siguiente fase sugerida

1. **Fase 2.1 — Mesa operativa:** navegar a detalle sesión, POS anclado a mesa, precuenta BT/TCP.
2. **Fase 3 — Caja:** apertura obligatoria, movimientos, cierre.
3. **Realtime:** WebSocket cocina/mesas (polling manual por ahora).
