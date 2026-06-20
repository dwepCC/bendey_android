# Referencia visual React → Compose

Documento de alineación entre `bendey_tenant_restaurante` (React) y `restaurante_kotlin` (Compose).  
**La referencia principal es la app React validada con clientes**, no patrones genéricos de Material Design.

---

## Shell y safe area

| React | Compose | Notas |
|-------|---------|-------|
| `RestaurantLayout` — marco `rest-900` (#721F21) | `BendeyRestaurantShell` — `BendeyColors.Rest900` | Fondo tomate en status bar y bordes |
| Tarjeta blanca redondeada para contenido | `Surface` con `RoundedCornerShape(16.dp)` | Header + contenido dentro de la tarjeta |
| `--safe-top` / `--safe-bottom` CSS | `bendeyStatusBarsPadding()` + `navigationBarsPadding()` | Status bar tomate con iconos claros (`SystemBarStyle.dark`) |

---

## Header

| React | Compose |
|-------|---------|
| Logo + nombre negocio + sucursal | `BendeyAppHeader` — restaurante, sucursal, usuario |
| Estado caja / saldo | Chip «Caja S/ …» |
| Menú hamburguesa móvil → drawer | `IconButton(Menu)` → `ModalNavigationDrawer` |
| Usuario / avatar | Iniciales en círculo tomate |

**Jerarquía:** nombre del negocio (bold) → metadatos secundarios (sucursal · usuario) → chips de estado (online, caja).

---

## Navegación

### Barra inferior (solo operación diaria)

| React (`MobileBottomNav` / `NAV_GROUPS.operations`) | Compose (`TopLevelDestination.bottomBarDestinations`) |
|-----------------------------------------------------|-----------------------------------------------------|
| Dashboard / Inicio | `DASHBOARD` — «Inicio» |
| POS | `POS` |
| Mesas (`/salas`) | `MESAS` |
| Comandas | `COCINA` — «Comandas» |

### Drawer lateral (gestión)

| React (menú Gestión) | Compose (`BendeyDrawerDestination`) |
|----------------------|-------------------------------------|
| Caja | `CAJA` |
| Ventas | `VENTAS` |
| Productos | `PRODUCTOS` |
| Clientes | `CLIENTES` |
| Configuración | `CONFIGURACION` |
| Repartidores | `REPARTIDORES` |
| Impresoras | `IMPRESORAS` |

**Tablet:** rail lateral con los 4 ítems operativos; gestión solo en drawer.

---

## Dashboard

| React (`DashboardPage`) | Compose (`DashboardScreen`) |
|-------------------------|----------------------------|
| KPIs: ingresos, pedidos, ticket, comensales | `BendeyKpiCard` × 4 con % vs período anterior |
| Presets: Hoy / 7 días / 30 días | `DashboardRange` chips |
| Gráfico tendencia 30 días (Recharts) | `DailyTrendChart` (Canvas) |
| Top productos (barras) | `TopProductsChart` |
| Métodos de pago | `PaymentBars` |
| Mesas + estado caja | `TableSummaryCard` |
| **Sin grid de accesos directos** | Gestión movida al drawer |

**Densidad:** KPIs en filas de 2 (móvil) / 4 (tablet); gráficos en cards compactas con padding 10–12 dp.

---

## POS

| React (`POSPage`) | Compose |
|-------------------|---------|
| ~62% catálogo / ~38% carrito (tablet) | `weight(0.62f)` / `weight(0.38f)` |
| Cards producto ~172px | `BendeyProductCard` — altura fija 172 dp |
| Buscador compacto | `PosCompactSearchField` — 44 dp |
| Categorías sidebar (tablet) / chips (móvil) | `BendeyPosCatalogPane` |
| Imágenes vía API central | `ProductImageRepositoryImpl.tenantAssetsBaseUrl()` → `CENTRAL_API_URL` |
| Carrito visible (panel / bottom sheet) | `CartPane` / `CompactCartBar` |

**Imágenes:** igual que React `getPublicAssetsBaseUrl()` — base = API central, no URL del tenant SPA.

---

## Mesas

| React | Compose |
|-------|---------|
| Grid `minmax(220px, 1fr)` | `GridCells.Adaptive(min = 220.dp)` |
| Barra lateral color por estado | `BendeyTableCard` — emerald / orange / blue / amber |
| Filtro por piso | Chips horizontales |
| Stats libres / ocupadas / etc. | `BendeyTableStatsRow` |

---

## Comandas / Cocina

| React (`ComandasItemsView`) | Compose (`CocinaScreen`) |
|-----------------------------|--------------------------|
| Filtros horizontales snap por estado | `FilterChip` scroll horizontal |
| **Un estado activo a la vez** | `activeStatus` — no kanban 4 columnas |
| Grid 1 col móvil → 2–4 cols tablet | `GridCells.Fixed(columns)` según ancho |
| Cards legibles con qty grande | `KitchenCard` — nombre, meta, cantidad, avanzar |

**Principio operativo:** el usuario se enfoca en un estado (p. ej. Pendiente) antes de cambiar de tab.

---

## Jerarquía visual global

1. **Marco tomate** — identidad Bendey, status bar legible  
2. **Superficie blanca** — contenido operativo  
3. **Primario tomate (#C9393B)** — acciones, precios, selección  
4. **Acentos semánticos** — mesas (verde/naranja/azul), cocina (ámbar/azul/verde)  
5. **Densidad alta** — menos padding que Material demo; orientado a tablet 10" en restaurante  

---

## Mapeo de archivos

| Área | React | Kotlin |
|------|-------|--------|
| Shell | `RestaurantLayout.tsx` | `BendeyRestaurantShell.kt` |
| Header | `AppHeader.tsx` | `BendeyAppHeader.kt` |
| Bottom nav | `MobileBottomNav.tsx` | `BendeyBottomNavigationBar.kt` |
| Drawer | `ResponsiveMenu.tsx` | `BendeyNavigationDrawerContent.kt` |
| Dashboard | `DashboardPage.tsx` | `DashboardScreen.kt` |
| POS | `POSPage.tsx` | `PosScreen.kt` + `BendeyPosCatalogPane` |
| Mesas | `SalasPage.tsx` | `MesasScreen.kt` |
| Comandas | `ComandasItemsView.tsx` | `CocinaScreen.kt` |
| Colores | `tailwind rest-*` | `BendeyColors.kt` |
| Assets | `api.ts` → `getPublicAssetsBaseUrl` | `ProductImageRepositoryImpl` |

---

## Criterio de aceptación

Cada pantalla Compose debe:

1. Justificar su layout respecto al equivalente React (tabla anterior).  
2. Mantener identidad tomate + densidad operativa.  
3. No introducir patrones Android genéricos como referencia principal (FAB, grids de botones en dashboard, kanban comprimido en móvil).
