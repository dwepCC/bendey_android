# Fase UI/UX — Rediseño operativo (Toast / Square POS)

Referencia visual: [`UI_UX_ANDROID_V1.md`](UI_UX_ANDROID_V1.md)

## Fase 1 — Shell global ✅

- `BendeyAppHeader` — restaurante, sucursal, usuario, caja, conectividad
- `BendeyBottomNavigationBar` — Dashboard, POS, Mesas, Cocina (activo tomate `#C62828`)
- `BendeyNavigationSuite` + `MainShell` en `BendeyAppNavHost`
- `BendeyInsets` — safe areas (`bendeySafeDrawingPadding`, etc.)
- Dashboard con KPIs coloridos y accesos rápidos
- POS, Mesas (lista), Cocina rediseñados sin top bar duplicado

## Fase 2 — Toolbars y catálogo compartido ✅

- `BendeyScreenToolbar` — título, subtítulo, `onBack`, slot `actions`
- Migración de pantallas de gestión: Caja, Ventas, Productos, Clientes, Configuración, Modificadores, Combos, Repartidores
- `BendeyPosCatalogPane` — búsqueda, categorías (chips o sidebar en tablet), grid con `BendeyProductCard`
- POS tablet: categorías en columna lateral (`sidebarCategories = true`)
- `MesaScreen`: toolbar propia, catálogo con imágenes, layout tablet alineado con POS
- Reglas de navegación (`BendeyRoutes`):
  - `showsGlobalHeader()` — oculto en `mesa/*` e `printing_test`
  - `showsBottomBar()` — solo dashboard, pos, mesas, cocina

## Fase 3 — Auth y pantallas full-screen ✅

- Safe areas en RUC, Home, Login email, PIN, Impresoras, detalle de mesa
- Sin header global en rutas operativas secundarias (mesa, impresoras)

## Pendiente / mejoras futuras

- Header: notificaciones, menú avatar, indicador offline real (hoy placeholder)
- Densidad fina en carrito POS/mesa (animaciones, bottom sheet refinado)
- Capturas de aprobación visual en dispositivo/emulador
- WebSocket / tiempo real (fuera de alcance UI)

## Componentes clave

| Componente | Ubicación |
|------------|-----------|
| `BendeyAppHeader` | `core/ui/.../BendeyAppHeader.kt` |
| `BendeyScreenToolbar` | `core/ui/.../BendeyScreenToolbar.kt` |
| `BendeyPosCatalogPane` | `core/ui/.../BendeyScreenToolbar.kt` |
| `BendeyProductCard` | `core/ui/.../BendeyProductCard.kt` |
| `BendeyBottomNavigationBar` | `core/ui/.../BendeyBottomNavigationBar.kt` |
