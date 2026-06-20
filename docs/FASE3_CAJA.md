# Fase 3 — Caja

> **Estado:** Implementada (MVP operativo)  
> **Fecha:** 2025-06-19  
> **Prerequisito:** Fases 1–2.1

---

## Alcance entregado

| Funcionalidad | Descripción |
|---------------|-------------|
| **Sesión abierta** | Consulta `GET /cashbank/sessions/open` por sucursal |
| **Abrir caja** | Modal obligatorio si no hay sesión activa |
| **Movimientos** | Ingreso / egreso manual con categorías backend |
| **Cerrar caja** | Cierre con efectivo contado + notas |
| **Persistencia local** | `CashSessionSnapshot` en DataStore vía `SessionManager` |
| **Nav** | Tab **Caja** habilitada en barra inferior / rail |

**Pendiente (Fase 3.1+):** arqueo por denominaciones, reportes PDF, historial de sesiones, modal bloqueante global para cajeros en POS, `billSession` SUNAT.

---

## APIs consumidas

| Endpoint | Uso |
|----------|-----|
| `GET /api/cashbank/sessions/open` | Sesión activa sucursal |
| `POST /api/cashbank/sessions` | Apertura |
| `POST /api/cashbank/sessions/{id}/close` | Cierre |
| `GET /api/cashbank/sessions/{id}/movements` | Listado turno |
| `POST /api/cashbank/sessions/{id}/movements` | Ingreso / egreso |

---

## Módulo

```
feature/caja/
  CajaScreen.kt
  CajaViewModel.kt
  navigation/CajaNavigation.kt
```

Capa compartida: `CashbankApi`, `CashRepository`, `CashRepositoryImpl`.

---

## Flujo de prueba

1. Ir a **Caja** → si no hay sesión, modal **Abrir caja**.
2. Ingresar monto inicial → confirmar.
3. **+ Ingreso** / **- Egreso** → registrar movimientos.
4. Ver saldo estimado y listado actualizado (pull refresh).
5. **Cerrar caja** → efectivo contado → sesión cerrada.

---

## Siguiente fase sugerida

- **Fase 4:** Ventas / facturación (`billSession`), clientes, productos CRUD.
- **Fase 3.1:** Arqueo denominaciones + reporte sesión (paridad web).
