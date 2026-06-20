# Bendey Resto — Auditoría funcional y plan de migración Android nativo

> **Versión:** 1.1  
> **Fecha:** 2025-06-19  
> **Origen analizado:** `bendey_tenant_restaurante` v1.0.2  
> **Destino planificado:** `restaurante_kotlin`  
> **Estado:** Aprobado — Fase 1 pendiente de aprobación UI/UX  
> **Package:** `com.bendey.restaurant` · **Min SDK:** 29

---

## 0. Modelo de coexistencia (aprobado)

`restaurante_kotlin` **NO reemplaza** ni modifica `bendey_tenant_restaurante`. Es un **nuevo cliente Android nativo** que convive con el frontend existente.

```
Backend Go (API sin cambios)
│
├── bendey_tenant_restaurante     ← NO TOCAR
│   ├── React (Web)
│   ├── Capacitor (Android híbrido legacy)
│   └── Tauri (Windows)
│
└── restaurante_kotlin            ← NUEVO CLIENTE
    ├── Kotlin 2.x
    ├── Jetpack Compose
    └── Android nativo puro
```

**Restricciones:**
- No modificar código en `bendey_tenant_restaurante`
- No renombrar endpoints ni cambiar contratos API
- Compatibilidad total con la API actual
- Suscripción SaaS fuera de V1
- Hardware POS V1: Bluetooth + TCP/IP ESC/POS únicamente
- Estados de mesa: exactamente los del backend (`libre`, `ocupada`, `reservada`, `en_consumo`)

---

## 1. Resumen ejecutivo

**Bendey Resto** es una SPA React 19 + TypeScript + Vite 7 empaquetada hoy como aplicación híbrida:

| Plataforma | Tecnología | UI |
|------------|------------|-----|
| Android | Capacitor 7 (`bendey.resto.cloud`) | WebView 100 % React |
| Windows | Tauri 2 | WebView + impresión Rust |
| Web | Vite dev/prod | React puro |

La migración a Android nativo implica **reescribir ~27 pantallas**, **8 capas de estado global**, **~100 endpoints REST**, lógica ESC/POS, escáner, flujos POS/mesa/cocina/caja, y rediseñar la UX para tablets y terminales POS — sin copiar literalmente la UI web.

**Activos reutilizables identificados:**
- Contratos API REST existentes (backend Go sin cambios iniciales)
- Plugin Kotlin `TukichefPrinterPlugin.kt` (~388 líneas) — Bluetooth SPP + TCP 9100
- Lógica de negocio documentada en `utils/` (checkout, permisos, estados)
- Paleta de marca `#C9393B` (rojo tomate)

---

## 2. Inventario de la aplicación actual

### 2.1 Estructura de carpetas

```
bendey_tenant_restaurante/
├── src/
│   ├── main.tsx              # Entry: providers + HashRouter
│   ├── App.tsx               # Rutas + guards
│   ├── pages/                # 27 pantallas
│   ├── components/           # ~109 componentes
│   ├── layouts/              # RestaurantLayout
│   ├── contexts/             # 8 React Contexts
│   ├── hooks/                # 5 hooks custom
│   ├── services/             # 25 servicios API + printers/
│   ├── utils/                # Lógica de negocio
│   ├── lib/                  # Platform, tenant, connectivity
│   ├── config/               # Nav, branding, theme
│   ├── plugins/              # TukichefPrinter (TS)
│   └── providers/            # NativeShellProvider
├── android/                  # Capacitor + TukichefPrinterPlugin.kt
├── src-tauri/                # Desktop Windows
└── capacitor.config.ts
```

### 2.2 Stack tecnológico actual

| Capa | Tecnología |
|------|------------|
| UI | React 19, TailwindCSS 3.4, Lucide icons |
| Routing | react-router-dom 7 (HashRouter) |
| HTTP | Axios 1.13 |
| Estado | React Context + localStorage / Capacitor Preferences |
| Virtualización | @tanstack/react-virtual (catálogo POS) |
| Gráficos | Recharts 3.8 |
| PDF | jsPDF 4.2 |
| QR | qrcode 1.5 |
| Escáner | html5-qrcode 2.3.8 |
| Excel import | hucre 0.5.1 |
| Nativo Android | Capacitor 7 (camera, preferences, status-bar, etc.) |

**No hay:** Zustand, Redux, MobX, WebSocket, SignalR, Room/SQLite, módulos Compose.

---

## 3. Módulos funcionales

### 3.1 Mapa de módulos

| Módulo Android | Pantallas actuales | Servicios API | Prioridad |
|----------------|-------------------|---------------|-----------|
| **auth** | RucPage, HomePage, LoginPage, PinLoginPage, NoAccessPage | public, auth, restaurantAuth, session | P0 |
| **dashboard** | DashboardPage | restaurant, catalogAnalytics | P1 |
| **pos** | POSPage (~2000 LOC) | restaurant, products, contacts, cashbank, billing | P0 |
| **mesas** | SalasPage, MesaPage, MesasPage | restaurant | P0 |
| **cocina** | ComandasPage, ComandasCocinaPage (legacy redirect) | restaurant | P0 |
| **ventas** | VentasPage | sales, billing | P1 |
| **productos** | ProductosPage, CategoriasPage, ModificadoresPage, CombosPage | products, combos | P1 |
| **clientes** | ClientesPage | contacts, consulta | P2 |
| **inventario** | *(sin pantalla dedicada)* — stock en ProductosPage | products (`/inventory/stock-summary`) | P2 |
| **caja** | CajaPage | cashbank | P1 |
| **configuracion** | AjustesPage (+ sub-tabs) | company, restaurant, printers local | P1 |
| **repartidores** | RepartidoresPage | restaurant (delivery) | P2 |
| **suscripcion** | SubscriptionPage | subscription | P3 |

### 3.2 Flujos críticos de negocio

#### Bootstrap / Auth
1. **Vincular RUC** → `GET /api/public/tenant-by-ruc` → persistir slug + apiUrl
2. **Home** → selección de estación (mozo, cajero, cocina, delivery, admin)
3. **Login email** → `POST /api/login` o **PIN estación** → `POST /api/restaurant/auth/pin`
4. **Permisos** → `GET /api/restaurant/session/permissions`
5. **Redirect por rol** → admin/cajero → dashboard; mozo → pos/salas; cocinero → comandas

#### POS (mostrador / llevar / delivery)
1. Abrir sesión → `POST /api/restaurant/sessions`
2. Agregar ítems (catálogo infinito, combos, modificadores, escáner)
3. Enviar comanda → `POST /api/restaurant/sessions/{id}/orders`
4. Impresión auto cocina → ESC/POS local
5. Cobro → `POST /api/restaurant/sessions/{id}/bill` (series SUNAT, cliente, pagos múltiples, descuento)
6. Cierre → `POST /api/restaurant/sessions/{id}/close`

#### Mesa (salón)
1. SalasPage → grid mesas por piso
2. Mesa libre → abrir sesión; ocupada → navegar a MesaPage
3. MesaPage → flujo similar a POS pero anclado a sesión de mesa
4. Precuenta → `GET /api/restaurant/sessions/{id}/precuenta` + impresión
5. Cierre mesa → bill + close

#### Cocina / Comandas
1. `GET /api/restaurant/kitchen` — **sin auto-refresh** (manual)
2. Vistas: por ítem, por pedido (board Kanban)
3. Estados comanda: `pendiente → preparacion → lista → entregada`
4. Acciones: notas, anular (PIN), reimprimir
5. Impresión automática al enviar ronda

#### Caja
1. Modal auto-apertura si cajero sin sesión abierta
2. CRUD sesiones, movimientos, arqueo, reportes
3. Cuentas bancarias y métodos de pago

#### Ventas / Facturación SUNAT
1. Listado con filtros y paginación
2. Emisión electrónica → polling + **SSE** (`/api/billing/events`)
3. Anulación con nota de crédito
4. Reimpresión PDF/ESC/POS

---

## 4. Pantallas y navegación actual

### 4.1 Rutas completas

| Ruta | Pantalla | Guard permiso |
|------|----------|---------------|
| `/ruc` | Vincular tenant | — |
| `/home` | Hub estaciones | tenant |
| `/login` | Login email | tenant |
| `/pin/:station` | Login PIN | tenant |
| `/dashboard` | Dashboard | `c.v` |
| `/pos` | POS | `p.u` |
| `/salas` | Mapa mesas operativo | `t.v` |
| `/mesa/:sessionId` | Pedido en mesa | `t.o` |
| `/comandas` | Cocina/comandas | `k.v` |
| `/mesas` | Config pisos/mesas | `g.p` |
| `/productos` | Catálogo | `g.p` |
| `/categorias` | Categorías | `g.p` |
| `/modificadores` | Modificadores | `g.p` |
| `/combos` | Combos | `g.p` |
| `/ventas` | Ventas | `o.ch` o `c.v` |
| `/caja` | Caja | `c.v` |
| `/clientes` | Clientes | `o.ch` |
| `/repartidores` | Delivery | `d.v` |
| `/ajustes` | Configuración | `s.m` o impresoras |
| `/suscripcion` | Billing SaaS | `s.m` |

### 4.2 Permisos backend (códigos cortos)

| Código | Feature UI |
|--------|------------|
| `g.p` | productos, modificadores, mesas (config) |
| `p.u` | POS |
| `t.v` | salas (ver mesas) |
| `t.o` | mesa (operar mesa) |
| `k.v` | comandas/cocina |
| `o.ch` | ventas, clientes, cerrar mesa, descuentos |
| `c.v` | caja, dashboard |
| `d.v` | repartidores |
| `s.m` | settings admin, suscripción |

### 4.3 Navegación UI actual

- **Desktop/tablet ancho:** TopNavigation + ManagementNavDropdown
- **Móvil:** Bottom nav fija — Dashboard, POS, Mesas, Comandas
- **Grupos:** Operaciones (4) + Gestión (6)

---

## 5. Gestión de estado actual

| Context | Responsabilidad | Persistencia |
|---------|-----------------|--------------|
| TenantBindingProvider | RUC → slug + API URL | Capacitor Preferences |
| AuthProvider | token, user, permisos, login/logout | localStorage |
| FeatureProvider | flags tenant (multi_branch, recipes, inventory) | API |
| BranchProvider | sucursal activa, switch | localStorage + evento `branch-changed` |
| BranchCheckoutSeriesProvider | series fiscales SUNAT por sucursal | cache memoria |
| CashSessionProvider | sesión caja abierta, modal apertura | API + memoria |
| BackendConnectivityProvider | online/offline/degraded | probe HTTP |
| SubscriptionStatusProvider | hub suscripción | API |

### Hooks custom
- `usePosInfiniteProducts` — scroll infinito catálogo
- `useDebouncedApiSearch` / `useDebouncedSearch`
- `useDeviceFormFactor` — tablet/phone/desktop
- `useBillingEvents` — SSE facturación

---

## 6. Catálogo de APIs

### 6.1 Configuración HTTP

| Header | Valor |
|--------|-------|
| `Authorization` | `Bearer {token}` |
| `X-Tenant-Slug` | slug del tenant |
| `Content-Type` | `application/json` |

| URL | Uso |
|-----|-----|
| `https://api.bendey.cloud` | API central (bootstrap RUC) |
| `{tenant.apiUrl}` | API del tenant (post-vinculación) |
| `VITE_ASSETS_ORIGIN` | `/uploads`, `/storage` |

### 6.2 Endpoints por dominio (~100 total)

#### Público / Bootstrap
- `GET /api/public/tenant-by-ruc`

#### Auth / Sesión
- `POST /api/login`
- `GET /api/session/context`
- `POST /api/session/switch-branch`
- `GET /api/restaurant/auth/config`
- `POST /api/restaurant/auth/pin`
- `GET /api/restaurant/session/permissions`

#### Restaurante (42 endpoints en restaurant.service.ts)
- Settings/staff: `/api/restaurant/settings`, `/staff`, `/staff/management`, `/staff/users`, `/users/:id/staff`
- Pisos/mesas: `/floors`, `/tables`, `/tables/:id/session`
- Sesiones: `/sessions`, `/sessions/:id`, `/orders`, `/bill`, `/close`, `/cancel`, `/precuenta`, `/order-status`
- Comandas: `/comandas/:id/status|notes|print`, DELETE con PIN
- Cocina: `/kitchen`
- Pedidos abiertos: `/orders`
- Delivery: `/delivery-drivers`, `/delivery-companies`
- Dashboard: `/dashboard`, `/payments-report`, `/operational-status`

#### Productos / Catálogo
- `/api/products` (+ bulk-import, image)
- `/api/categories`, `/api/modifier-groups`
- `/api/combos`, `/api/combos/:id/resolve`
- `/api/inventory/stock-summary`

#### Ventas / Facturación
- `/api/sales`, `/api/sales/:id`, issue-electronic, cancel
- `/api/billing/invoice/:id`, `/status/:id`, `/send/:id`, `/resend/:id`
- `/api/billing/void-with-credit-note/:id`
- **`GET /api/billing/events`** (SSE)

#### Caja (23 endpoints)
- Sessions: open/close/arqueo/report/movements
- Bank accounts, payment methods, reports

#### Empresa
- `/api/company/config`, `/sunat`, `/branches`, `/series`

#### Contactos / Consulta
- `/api/contacts`, `/api/consulta/dni`, `/api/consulta/ruc`

#### Suscripción
- `/api/subscription/summary`, `/plans`, `/payments`, `/plan-change`, document-packages

#### Usuarios
- `GET /api/users`

#### Analytics
- `GET /api/restaurant/catalog-analytics`

---

## 7. Impresión

### 7.1 Arquitectura actual

```
TypeScript ESC/POS builder (~650 LOC)
    ├── comandaEscposLayout.ts
    ├── receiptTicketLayout.ts
    ├── escposRasterImage.ts (logo)
    └── printers.service.ts (orquestación)
         ├── Bluetooth → TukichefPrinterPlugin.kt (SPP UUID 00001101-...)
         ├── Network TCP → puerto 9100
         └── Windows → Tauri Rust (no aplica Android)
```

### 7.2 Tipos de impresora
| Kind | Uso |
|------|-----|
| `comandas` | Por área de preparación |
| `precuenta` | Precuenta mesa |
| `documentos` | Boleta/factura/NV + QR SUNAT |

### 7.3 Flujos
- Auto al enviar comanda → `kitchenPrint.ts`
- Post-cobro → `print_data` en respuesta de `bill`
- Reimpresión → modal PDF o ESC/POS directo

**No hay:** Sunmi SDK, iMin SDK — solo ESC/POS genérico.

---

## 8. Escáner

| Modo | Implementación |
|------|----------------|
| Cámara | html5-qrcode + @capacitor/camera |
| USB HID | Enter en campo búsqueda ( pistola ) |
| Formatos | EAN-13/8, CODE-128/39, UPC, ITF, QR |

Búsqueda producto: `GET /api/products?q={code}` (catálogo POS).

**No hay:** Zebra DataWedge, Sunmi scanner nativo.

---

## 9. Tiempo real

| Mecanismo | Estado | Uso |
|-----------|--------|-----|
| SSE | ✅ Implementado | Facturación SUNAT (`billing.status.updated`) |
| HTTP polling | ✅ Implementado | Espera estado billing post-emisión |
| Connectivity probe | ✅ Implementado | Overlay offline |
| WebSocket | ❌ No existe | — |
| SignalR | ❌ No existe | — |
| Comandas live | ❌ Manual refresh | Cocina requiere pull-to-refresh |

---

## 10. Estados de mesa y comanda

### Mesas (backend actual)
| Estado | Label UI | Color |
|--------|----------|-------|
| `libre` | Libre | Verde |
| `ocupada` | Ocupada | Naranja |
| `reservada` | Reservada | Azul |
| `en_consumo` | Por cerrar | Rojo |

> **Nota migración:** El requerimiento menciona estados `Preparando`, `Lista`, `Facturada` — estos mapean mejor a **comandas** y **sesión de mesa**, no al status de mesa en backend. En Android se puede enriquecer la UI combinando `table.status` + `session.order_status` + agregado de comandas.

### Comandas
`pendiente → preparacion → lista → entregada`

---

## 11. Componentes clave a rediseñar (no copiar)

| Área | Componentes actuales | Rediseño Android |
|------|---------------------|------------------|
| POS | VirtualProductCatalogGrid, POSCheckoutModal | Two-pane adaptive: catálogo + carrito persistente |
| Mesa | FloatingCartButton, MobileCartDrawer | Bottom sheet / side panel según WindowSizeClass |
| Cocina | ComandasKitchenBoardView | KDS fullscreen, columnas por estado, gestos swipe |
| Checkout | TouchDecimalKeypad, CheckoutCartBillingFields | Material 3 number pad, payment chips |
| Mesas | TableStatusIndicator, TABLE_GRID | LazyVerticalGrid con cards táctiles grandes |
| Settings | PrintersSettingsTab | Wizard nativo BT pairing |

---

## 12. Arquitectura Android propuesta

### 12.1 Stack obligatorio

| Capa | Tecnología |
|------|------------|
| Lenguaje | Kotlin 2.x |
| UI | Jetpack Compose, Material 3, Material 3 Expressive |
| Navegación | Navigation Compose + Adaptive Layouts |
| Arquitectura | Clean Architecture + MVVM |
| Async | Coroutines + StateFlow |
| DI | Hilt |
| Network | Retrofit + OkHttp + Kotlin Serialization |
| Imágenes | Coil |
| Local (prep) | Room (schema only, sin sync) |
| Realtime (prep) | OkHttp SSE + abstracción WebSocket/SignalR |

### 12.2 Capas

```
Presentation (Compose + ViewModel)
    ↓ StateFlow / UiEvent
Domain (Use Cases + Models + Repository interfaces)
    ↓
Data (Repository impl + Retrofit APIs + Room DAOs + DataStore)
    ↓
Core (network, designsystem, navigation, common)
```

### 12.3 Módulos Gradle

```
restaurante_kotlin/
├── app/                          # Application, MainActivity, NavHost root
├── core/
│   ├── network/                  # Retrofit, interceptors, SSE client
│   ├── ui/                       # WindowSizeClass, insets, adaptive scaffolds
│   ├── designsystem/             # Theme, tokens, components base
│   ├── navigation/               # Routes, NavGraph, deep links
│   ├── domain/                   # Modelos compartidos, Result wrappers
│   └── data/                     # DataStore, Room base, mappers comunes
├── feature/
│   ├── auth/
│   ├── dashboard/
│   ├── pos/
│   ├── mesas/
│   ├── cocina/
│   ├── ventas/
│   ├── productos/
│   ├── clientes/
│   ├── inventario/
│   ├── caja/
│   └── configuracion/
└── platform/
    ├── printing/                 # Interfaces + ESC/POS + BT/TCP/USB/Sunmi/iMin
    └── scanning/                 # CameraX/ML Kit + HID + Zebra/Sunmi
```

### 12.4 Patrones por feature

```kotlin
// Ejemplo estructura interna feature:pos
feature/pos/
├── presentation/
│   ├── PosScreen.kt
│   ├── PosViewModel.kt
│   ├── PosUiState.kt
│   └── components/
├── domain/
│   ├── usecase/OpenPosSessionUseCase.kt
│   ├── usecase/AddOrderItemsUseCase.kt
│   └── repository/PosRepository.kt
└── data/
    ├── PosRepositoryImpl.kt
    ├── PosApi.kt
    └── dto/
```

---

## 13. Design System propuesto

### 13.1 Paleta — Rojo tomate profesional

Basado en marca actual `#C9393B` y referencias `#D84315`, `#E53935`, `#C62828`:

| Token | Light | Dark | Uso |
|-------|-------|------|-----|
| primary | `#C62828` | `#EF5350` | Acciones principales, brand |
| onPrimary | `#FFFFFF` | `#1A0A0A` | Texto sobre primary |
| primaryContainer | `#FFEBEE` | `#4A1515` | Chips, badges |
| secondary | `#D84315` | `#FF7043` | Acentos operativos |
| tertiary | `#5D4037` | `#BCAAA4` | Gestión/admin |
| surface | `#FFFBFE` | `#1C1B1F` | Fondos |
| error | `#B3261E` | `#F2B8B5` | Errores, anulaciones |

**Dynamic Color:** habilitado en Android 12+ con fallback a paleta fija tomate.

### 13.2 Tipografía (Material 3 Expressive)

- **Display:** operaciones POS — números grandes, peso bold
- **Headline:** títulos de pantalla
- **Title:** cards de mesa, productos
- **Body:** descripciones, notas
- **Label:** badges de estado, timestamps cocina

### 13.3 Componentes base (`core:designsystem`)

| Componente | Uso |
|------------|-----|
| `BendeyTopAppBar` | Header con sucursal, caja, usuario |
| `BendeyNavigationSuiteScaffold` | Rail / Drawer / BottomBar adaptativo |
| `BendeyPrimaryButton` | CTAs táctiles grandes (min 48dp) |
| `BendeyProductCard` | Catálogo POS |
| `BendeyTableCard` | Grid mesas |
| `BendeyStatusChip` | Estados mesa/comanda |
| `BendeyMoneyText` | Montos con formato PEN |
| `BendeyDecimalKeypad` | Cobro |
| `BendeyEmptyState` | Listas vacías |
| `BendeyLoadingOverlay` | Operaciones async |
| `BendeyOfflineBanner` | Conectividad |
| `BendeyConfirmDialog` | Anulaciones, PIN |

### 13.4 Safe Areas

```kotlin
Modifier
    .fillMaxSize()
    .safeDrawingPadding()
    // o granular:
    .statusBarsPadding()
    .navigationBarsPadding()
```

Sin márgenes manuales para notch, gesture nav, o barras del sistema.

### 13.5 WindowSizeClass

| Clase | Ancho | Layout |
|-------|-------|--------|
| Compact | < 600dp | Bottom nav, single pane, sheets |
| Medium | 600–840dp | Nav rail opcional, master-detail parcial |
| Expanded | > 840dp | Nav rail fijo, two-pane, POS catálogo+carrito side-by-side |

---

## 14. Navegación Android propuesta

### 14.1 NavGraph principal

```
NavHost (root)
├── auth_graph
│   ├── ruc
│   ├── home (estaciones)
│   ├── login
│   └── pin/{station}
└── main_graph (authenticated)
    ├── dashboard
    ├── pos
    ├── salas
    ├── mesa/{sessionId}
    ├── cocina
    ├── ventas
    ├── productos_graph (productos, categorias, modificadores, combos)
    ├── clientes
    ├── inventario
    ├── caja
    ├── repartidores
    ├── configuracion
    └── suscripcion
```

### 14.2 Adaptive navigation

| WindowSizeClass | Patrón |
|-----------------|--------|
| Compact | `NavigationBar` — Dashboard, POS, Mesas, Cocina |
| Medium | `NavigationRail` colapsable + contenido |
| Expanded | `NavigationRail` permanente + `PermanentNavigationDrawer` para gestión |

### 14.3 Deep links

```
bendey://resto/mesa/{sessionId}
bendey://resto/pos
bendey://resto/cocina
```

### 14.4 Guards

- `TenantRequired` — DataStore tenant binding
- `AuthRequired` — token válido
- `PermissionRequired(feature)` — mapa permisos igual que web
- `CashSessionRequired` — modal apertura caja (cajero)

---

## 15. Plataforma: Impresión (interfaces)

```kotlin
interface PrinterTransport {
    suspend fun connect(config: PrinterConfig): Result<Unit>
    suspend fun print(payload: EscPosPayload): Result<Unit>
    suspend fun disconnect()
}

interface PrinterRepository {
    suspend fun getConfiguredPrinters(): List<PrinterConfig>
    suspend fun printComanda(data: ComandaPrintData, area: String?)
    suspend fun printPrecuenta(data: PrecuentaPrintData)
    suspend fun printDocument(data: DocumentPrintData)
}

// Implementaciones
class BluetoothEscPosTransport : PrinterTransport
class TcpEscPosTransport : PrinterTransport      // Ethernet/WiFi :9100
class UsbEscPosTransport : PrinterTransport      // POS USB
class SunmiBuiltInPrinter : PrinterTransport       // SDK Sunmi
class IMinBuiltInPrinter : PrinterTransport        // SDK iMin
```

**Migración ESC/POS:** portar layouts TS → Kotlin (prioridad: comandas, precuenta, ticket).

---

## 16. Plataforma: Escáner (interfaces)

```kotlin
interface BarcodeScanner {
    fun startScanning(onResult: (BarcodeResult) -> Unit)
    fun stopScanning()
}

// Implementaciones
class CameraMlKitScanner : BarcodeScanner       // CameraX + ML Kit
class UsbHidScannerHandler                      // Keyboard wedge
class SunmiScannerBridge : BarcodeScanner
class ZebraDataWedgeScanner : BarcodeScanner
```

---

## 17. Offline-first (preparación)

### Room entities (schema inicial, sin sync)

- `CachedProduct`, `CachedCategory`, `CachedModifierGroup`
- `CachedContact` (clientes frecuentes)
- `PendingOrder` (cola de envío)
- `PrinterConfig` (config local)

### Sync (fase futura)
- WorkManager + conflict resolution
- Outbox pattern para órdenes

---

## 18. Tiempo real (preparación)

```kotlin
interface RealtimeClient {
    fun connect(endpoint: RealtimeEndpoint)
    fun disconnect()
    val events: Flow<RealtimeEvent>
}

sealed class RealtimeEndpoint {
    data class Sse(val url: String, val token: String) : RealtimeEndpoint()
    data class WebSocket(val url: String) : RealtimeEndpoint()
    data class SignalR(val hubUrl: String) : RealtimeEndpoint()
}
```

**Fase 1:** OkHttp SSE para billing (paridad con web).  
**Fase 2:** Polling cocina/mesas cada N segundos.  
**Fase 3:** WebSocket backend para KDS y mesas live.

---

## 19. Plan de migración por fases

### Fase 0 — Aprobación (actual)
- [x] Auditoría funcional
- [x] Arquitectura y design system
- [ ] **Aprobación del cliente**

### Fase 1 — Foundation (2–3 semanas)
- Proyecto Gradle multi-módulo
- `core:designsystem` — theme tomate light/dark/dynamic
- `core:network` — Retrofit, interceptors, auth headers
- `core:navigation` — NavHost adaptativo
- `feature:auth` — RUC, home, login, PIN
- DataStore tenant + token
- Splash + edge-to-edge + WindowInsets

### Fase 2 — Operaciones core (4–5 semanas)
- `feature:pos` — catálogo, carrito, checkout, escáner cámara
- `feature:mesas` — salas, mesa, estados
- `feature:cocina` — KDS, board, estados comanda
- `platform:printing` — port ESC/POS + Bluetooth/TCP (reusar lógica Kotlin existente)
- Integración caja (modal apertura)

### Fase 3 — Gestión (3–4 semanas)
- `feature:dashboard` — métricas, gráficos (Vico/Compose charts)
- `feature:ventas` — listado, facturación, SSE billing
- `feature:caja` — sesiones, arqueo, reportes
- `feature:productos` — CRUD, categorías, modificadores, combos

### Fase 4 — Complementos (2–3 semanas)
- `feature:clientes` — CRUD + consulta DNI/RUC
- `feature:inventario` — stock summary (extensible)
- `feature:configuracion` — empresa, SUNAT, series, staff, impresoras
- `feature:repartidores`
- Room schema (sin sync)

### Fase 5 — POS hardware (2 semanas)
- Sunmi / iMin SDK
- Zebra DataWedge
- USB printing
- Optimización tablets 10"/12" y terminales POS

### Fase 6 — Polish (ongoing)
- Performance (recomposition profiling)
- Pull-to-refresh universal
- Paginación en todas las listas
- Tests unitarios use cases + UI tests críticos
- Play Store / distribución enterprise

---

## 20. Estimación de esfuerzo

| Área | Complejidad | LOC referencia actual |
|------|-------------|----------------------|
| POS + Checkout | Muy alta | ~2000 (POSPage) + utils |
| Mesa | Alta | ~1500 (MesaPage) |
| Cocina/KDS | Media-alta | ComandasPage + board |
| Caja | Alta | CajaPage + cashbank.service |
| Ventas + Billing | Alta | VentasPage + SSE |
| Productos | Media | 4 páginas |
| Auth + Bootstrap | Media | 5 páginas |
| Impresión ESC/POS | Alta | ~650 TS + plugin Kotlin |
| Design System | Media | Nuevo |
| **Total estimado** | **~4–5 meses** (1 dev senior) | ~274 archivos TS/TSX |

---

## 21. Riesgos y decisiones pendientes

| Riesgo | Mitigación |
|--------|------------|
| Lógica ESC/POS en TypeScript | Port incremental a Kotlin; tests byte-a-byte |
| Sin WebSocket cocina | Polling + pull-to-refresh; negociar endpoint WS con backend |
| POSPage monolítica | Dividir en use cases desde el inicio |
| Permisos granulares | Replicar mapa exacto de `restaurantPermissions.ts` |
| Multi-sucursal | Branch switch invalida cache; event bus interno |
| SUNAT/billing | SSE + polling; critical path bien testeado |
| Tablets vs phones | Adaptive layouts desde día 1, no retrofit |

### Decisiones para aprobar
1. ¿Incluir módulo `suscripcion` en v1 o postergar?
2. ¿Soporte Sunmi/iMin en v1 o solo BT/TCP genérico?
3. ¿App ID/package: `bendey.resto.cloud` (mantener) o nuevo?
4. ¿Min SDK 29 (Android 10) confirmado?
5. ¿Backend WebSocket para cocina/mesas — scope backend separado?

---

## 22. Referencias de código existente

| Archivo | Relevancia |
|---------|------------|
| `src/App.tsx` | Rutas y guards |
| `src/utils/restaurantPermissions.ts` | Permisos |
| `src/services/api.ts` | HTTP client pattern |
| `src/services/restaurant.service.ts` | API operativa |
| `src/services/printers.service.ts` | ESC/POS |
| `android/.../TukichefPrinterPlugin.kt` | BT + TCP nativo |
| `src/config/restaurantTheme.ts` | Paleta marca |
| `src/hooks/useBillingEvents.ts` | SSE pattern |

---

*Documento generado como entregable de Fase 0. No se ha generado código de aplicación.*
