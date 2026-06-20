# Alineación con Capacitor (`bendey_tenant_restaurante`)

Documento de paridad entre el panel web/móvil Capacitor y la app Android nativa.

## Navegación

| Capacitor | Android Kotlin |
|-----------|----------------|
| Bottom nav: Dashboard, POS, Mesas, Comandas (4 ítems) | Igual: `TopLevelDestination.bottomBarDestinations` |
| Ventas, Caja, Productos, Clientes en menú lateral / gestión | Dashboard → sección **Gestión** → Ventas, Caja (+ placeholders Productos/Clientes) |
| Rail/tablet: más rutas visibles | Navigation rail con todas las rutas |

Referencia Capacitor: `src/config/restaurantNav.ts`, `MobileBottomNav.tsx`.

## Facturación SUNAT (async)

| Comportamiento | Detalle |
|----------------|---------|
| Cobro / checkout | `billSession` registra la venta y responde de inmediato; **no** espera SUNAT |
| Cola backend | Facturador procesa; webhook actualiza estado del comprobante |
| UI post-cobro | Mensaje: registrado; SUNAT en segundo plano |
| Ventas (boletas/facturas) | SSE `GET /api/billing/events` — `BillingEventsClient` (paridad `useBillingEvents.ts`) |
| Sin polling | Cocina/mesas **no** auto-refrescan cada N segundos |
| WebSocket | **No implementado** (igual que Capacitor por ahora) |

## Ventas — pestañas separadas

Como `VentasPage.tsx`:

- **Notas de venta** — solo NV
- **Facturación** — boletas y facturas electrónicas (+ SSE estados)
- **Notas de crédito** — anulaciones FE (+ SSE)

No existe pestaña "Todas" que mezcle tipos.

## POS móvil

- Categorías con scroll horizontal
- Una sola barra inferior: total + **Cobrar** (si hay sesión) + **Comanda**
- `PosSessionBar` solo en layout expandido (tablet)

## Pendiente (roadmap gestión)

Módulos presentes en Capacitor aún no portados:

- Productos (CRUD, categorías, precios) — **Fase 10**
- Clientes — **Fase 11**
- Repartidores
- Configuración / impresoras avanzada desde gestión
- Reportes

Estos aparecen en Dashboard como "Próximamente" hasta implementar cada fase.

## Archivos clave Android

```
core/navigation/TopLevelDestination.kt
core/navigation/BendeyNavigationSuite.kt
core/realtime/billing/BillingEventsClient.kt
feature/dashboard/DashboardScreen.kt
feature/ventas/VentasViewModel.kt
feature/pos/PosScreen.kt
```
