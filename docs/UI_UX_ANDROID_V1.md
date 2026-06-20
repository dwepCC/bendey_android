# Bendey Resto Android — UI/UX V1

> **Versión:** 1.1  
> **Fecha:** 2025-06-19  
> **Package:** `com.bendey.restaurant`  
> **Estado:** Aprobado — actualizado design system colorido + light-only  
> **Alcance V1:** Login · Dashboard · POS · Mesas · Cocina · Caja · Impresión (Fase 1.5)  
> **Referencia funcional:** `bendey_tenant_restaurante` (sin copia visual web)

---

## 0. Política de tema (v1.1)

**Solo tema claro.** No Dark Mode, no Dynamic Color, no alternancia light/dark.

Motivo: operación en restaurantes, POS táctiles y tablets de atención — máxima legibilidad bajo luz ambiental.

Implementación: `BendeyTheme()` fijo con `lightColorScheme` personalizado.

---

## 1. Principios de diseño

### 1.1 Filosofía

La app debe sentirse **nativa Android**, no un WebView. Inspiración en Toast POS, Square POS, Shopify POS, Stripe Dashboard y Material 3 Expressive.

| Principio | Implementación |
|-----------|----------------|
| **Interfaz viva, no monocromática** | Tomate Bendey como primario; acentos teal, morado, verde, amarillo en KPIs y estados |
| **Velocidad operativa** | Targets táctiles ≥ 48 dp; acciones frecuentes a 1 tap |
| **Claridad bajo presión** | Jerarquía tipográfica fuerte; montos y estados siempre visibles |
| **Tablet-first operativo** | Layouts optimizados 8"/10"/12"; phone como compact fallback |
| **Tema claro exclusivo** | Sin dark mode |
| **Sin estados inventados** | Mesas: estados exactos del backend |

### 1.2 Prioridad UX V1

```
1. POS          → flujo de venta más rápido posible
2. Mesas        → mapa operativo + apertura de sesión
3. Cocina       → KDS legible a distancia
4. Impresión    → BT/TCP transparente post-acción
5. Caja         → apertura obligatoria + movimientos esenciales
```

### 1.3 Breakpoints (WindowSizeClass)

| Clase | Ancho | Dispositivo típico | Layout |
|-------|-------|-------------------|--------|
| **Compact** | < 600 dp | Celular | Single pane, bottom sheet carrito, bottom nav |
| **Medium** | 600–840 dp | Tablet 8" | Nav rail colapsable, grids 3–4 cols |
| **Expanded** | > 840 dp | Tablet 10"/12", POS | Two-pane persistente, nav rail fijo |

### 1.4 Safe areas

Todo contenido interactivo respeta `WindowInsets` vía `safeDrawingPadding()`. Sin márgenes manuales para notch, status bar o gesture navigation.

---

## 2. Design tokens V1.1

### 2.1 Paleta — Light only, colorida

**NO monocromática.** El rojo tomate es primario de marca, no color de fondo universal.

| Token | Hex | Uso |
|-------|-----|-----|
| **Primary** | `#C62828` | Botones principales, nav activa, branding |
| **Success** | `#2E7D32` | Libre, listo, confirmaciones |
| **Info** | `#0288D1` | En consumo, información |
| **Warning** | `#F9A825` | Reservada, pendiente, alertas suaves |
| **Error** | `#D32F2F` | Errores, anulaciones |
| **Accent Purple** | `#7B1FA2` | KPI ticket, chips decorativos |
| **Accent Teal** | `#00897B` | KPI ventas, acentos secundarios |
| **Background** | `#F8F9FA` | Fondo app |
| **Surface** | `#FFFFFF` | Cards, paneles |
| **Surface Variant** | `#F1F3F4` | Áreas secundarias |

Material mapping:
- `primary` → Tomate Bendey
- `secondary` → Teal
- `tertiary` → Purple

### 2.2 Estados de mesa (backend exacto — colores visuales v1.1)

| Estado | Color | Label UI |
|--------|-------|----------|
| `libre` | `#2E7D32` Verde | Libre |
| `ocupada` | `#C62828` Tomate Bendey | Ocupada |
| `reservada` | `#F9A825` Amarillo | Reservada |
| `en_consumo` | `#0288D1` Azul | Por cerrar |

Representación: barra lateral 4dp + dot + `BendeyStatusChip` — nunca solo texto.

### 2.3 Estados comanda (cocina KDS)

| Estado | Color | Columna |
|--------|-------|---------|
| `pendiente` | `#F9A825` | Pendiente |
| `preparacion` | `#0288D1` | Preparando |
| `lista` | `#2E7D32` | Listo |
| `entregada` | `#9E9E9E` | Entregado |

Cada columna Kanban con header coloreado + cards con borde acento.

### 2.4 Animaciones (Compose nativas)

Permitido con moderación:
- `AnimatedContent`, `AnimatedVisibility`, `animateContentSize`
- Shared transitions en navegación (Fase 2)
- Duración 200–300 ms

Prioridad: velocidad operativa > efectos visuales.

### 2.5 Tipografía (Material 3 Expressive)

| Rol | Uso | Tamaño ref. |
|-----|-----|-------------|
| Display Large | Total cobro POS | 36 sp / bold |
| Headline Medium | Título pantalla | 24 sp |
| Title Large | Nombre producto, mesa | 18 sp |
| Body Large | Notas, descripciones | 16 sp |
| Label Large | Badges, chips estado | 14 sp / medium |
| Label Small | Timestamps cocina | 11 sp |

### 2.5 Espaciado y touch

| Token | Valor |
|-------|-------|
| Grid base | 4 dp |
| Padding pantalla | 16 dp (compact) · 24 dp (medium+) |
| Gap cards | 8 dp (compact) · 12 dp (expanded) |
| Min touch target | 48 dp |
| Botón primario altura | 56 dp (operación) |
| Corner radius card | 16 dp |
| Corner radius button | 12 dp (standard) · 28 dp (FAB/chips) |

---

## 3. Navegación global

### 3.1 Compact (< 600 dp)

```
┌─────────────────────────────────────┐
│  TopAppBar: sucursal · caja · user  │
├─────────────────────────────────────┤
│                                     │
│           CONTENIDO                 │
│                                     │
├─────────────────────────────────────┤
│ 🏠 Dash │ 🧾 POS │ ▦ Mesas │ 👨‍🍳 Cocina │
└─────────────────────────────────────┘
```

Gestión (Productos, Ventas, Caja, Ajustes) → menú overflow `⋮` o drawer swipe desde avatar.

### 3.2 Expanded (> 840 dp) — layout operativo principal

```
┌──────┬──────────────────────────────────────────────┐
│      │  TopAppBar                                    │
│  R   ├──────────────────────────────────────────────┤
│  A   │                                              │
│  I   │              CONTENIDO                       │
│  L   │         (two-pane en POS/Mesas)              │
│      │                                              │
│ 72dp │                                              │
└──────┴──────────────────────────────────────────────┘

Rail items (V1 operativos):
  ● Dashboard
  ● POS          ← emphasis (filled cuando activo)
  ● Mesas
  ● Cocina
  ─────────
  ○ Ventas
  ○ Caja
  ○ Productos
  ○ Ajustes
```

---

## 4. Flujo de autenticación (Login)

### 4.1 Journey

```
[RUC] → [Home estaciones] → [PIN / Login email] → [App principal]
                ↑
         (admin: login email)
```

### 4.2 Mockup — RUC (Compact)

```
┌─────────────────────────────────────┐
│ ░░░░░░░░░ Status bar ░░░░░░░░░░░░░ │
├─────────────────────────────────────┤
│                                     │
│         ┌─────────────┐             │
│         │  🍅 BENDEY  │             │
│         │    RESTO    │             │
│         └─────────────┘             │
│                                     │
│     Vincula tu restaurante          │
│                                     │
│  ┌─────────────────────────────┐    │
│  │ RUC                         │    │
│  │ 20XXXXXXXXX                 │    │
│  └─────────────────────────────┘    │
│                                     │
│  ┌─────────────────────────────┐    │
│  │      CONTINUAR              │    │  ← Primary 56dp
│  └─────────────────────────────┘    │
│                                     │
│  Restaurante Demo S.A.C.            │  ← preview tras validar
│  demo.bendey.cloud                  │
│                                     │
└─────────────────────────────────────┘
```

### 4.3 Mockup — Home estaciones (Expanded — tablet 10")

```
┌──────┬──────────────────────────────────────────────────────────┐
│      │                                                          │
│      │  ┌─ Brand panel (PrimaryContainer gradient) ──────────┐  │
│  R   │  │  🍅 Bendey Resto                                   │  │
│  A   │  │  El sistema inteligente para tu restaurante       │  │
│  I   │  │                                                    │  │
│  L   │  │  Tu equipo, en el punto correcto                    │  │
│      │  └────────────────────────────────────────────────────┘  │
│ logo │                                                          │
│ only │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐    │
│      │  │  👤      │ │  💰      │ │  👨‍🍳     │ │  🛵      │    │
│      │  │  Mozo    │ │  Cajero  │ │  Cocina  │ │ Delivery │    │
│      │  │ Mesas y  │ │ POS y    │ │ Comandas │ │ Repartos │    │
│      │  │ pedidos  │ │ cobros   │ │ en vivo  │ │          │    │
│      │  └──────────┘ └──────────┘ └──────────┘ └──────────┘    │
│      │                                                          │
│      │  ┌─────────────────────────────────────────────────┐    │
│      │  │  🛡️  Acceso administrativo (email)         →   │    │
│      │  └─────────────────────────────────────────────────┘    │
│      │                                                          │
│      │  Empresa: Restaurante Demo · Sucursal Principal           │
└──────┴──────────────────────────────────────────────────────────┘
```

Cards estación: 160×140 dp mínimo, ripple al tap → navega a PIN.

### 4.4 Mockup — PIN Login (Medium — tablet 8", split layout)

```
┌─────────────────────────────────────────────────────────────────┐
│ ← Volver                                          Bendey Resto  │
├────────────────────────┬────────────────────────────────────────┤
│                        │                                        │
│    ┌──────────────┐    │           Cajero                       │
│    │              │    │     Ingresa tu PIN de operación        │
│    │   [Ilust.    │    │                                        │
│    │    cajero]   │    │         ● ● ● ○ ○ ○                    │
│    │              │    │                                        │
│    └──────────────┘    │    ┌─────┬─────┬─────┐                  │
│                        │    │  1  │  2  │  3  │                  │
│   PrimaryContainer     │    ├─────┼─────┼─────┤                  │
│   con pattern sutil    │    │  4  │  5  │  6  │                  │
│                        │    ├─────┼─────┼─────┤                  │
│                        │    │  7  │  8  │  9  │                  │
│                        │    ├─────┼─────┼─────┤                  │
│                        │    │     │  0  │  ⌫  │                  │
│                        │    └─────┴─────┴─────┘                  │
│                        │                                        │
│                        │    ┌─────────────────────────┐          │
│                        │    │      INGRESAR           │          │
│                        │    └─────────────────────────┘          │
└────────────────────────┴────────────────────────────────────────┘
```

**Interacciones:**
- Auto-submit al 6º dígito (configurable según backend)
- Shake + haptic en PIN incorrecto
- Loading overlay en teclado durante auth
- Post-login: cajero → Dashboard o POS según permisos; cocina → Cocina directo

### 4.5 Mockup — Login email (Admin)

```
┌─────────────────────────────────────┐
│ ←                    Acceso admin   │
├─────────────────────────────────────┤
│                                     │
│  Email                              │
│  ┌─────────────────────────────┐    │
│  │ admin@restaurante.com       │    │
│  └─────────────────────────────┘    │
│                                     │
│  Contraseña                         │
│  ┌─────────────────────────────┐    │
│  │ ••••••••••            👁    │    │
│  └─────────────────────────────┘    │
│                                     │
│  ┌─────────────────────────────┐    │
│  │     INICIAR SESIÓN          │    │
│  └─────────────────────────────┘    │
│                                     │
└─────────────────────────────────────┘
```

---

## 5. Dashboard (operación restaurante — no contabilidad)

Tarjetas `BendeyKpiCard` con:
- Header coloreado suave (12% opacity del acento)
- Icono en círculo con color de acento
- Valor grande en color acento (teal, morado, verde, amarillo)
- Título operativo: "Operación hoy", no "Reporte financiero"

KPIs implementados Fase 1:
- Ventas hoy (teal)
- Ticket promedio (morado)
- Mesas activas (verde)
- Comandas pendientes (amarillo)

Acceso Fase 1.5: botón "Prueba de impresión" al pie del dashboard.

```
┌──────┬──────────────────────────────────────────────────────────────────┐
│  🏠  │  Dashboard                    📅 Hoy ▼    🔄    Sucursal Centro ▼│
│  ●   ├──────────────────────────────────────────────────────────────────┤
│  🧾  │                                                                  │
│  ▦   │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐│
│  👨‍🍳 │  │ Ventas hoy  │ │ Ticket prom.│ │ Mesas activ.│ │ Comandas    ││
│  ──  │  │ S/ 4,280.50 │ │ S/ 38.20    │ │ 12 / 24     │ │ 8 pendientes││
│  📄  │  │  ▲ 12%      │ │             │ │  ████░░░░   │ │  ⚠ 3 demora ││
│  💰  │  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘│
│  ⚙   │                                                                  │
│      │  ┌──────────────────────────────┐ ┌────────────────────────────┐│
│      │  │ Ventas por hora              │ │ Top productos              ││
│      │  │                              │ │                            ││
│      │  │    ▄                         │ │ 1. Lomo saltado    142 u  ││
│      │  │   ▄█▄    ▄                   │ │ 2. Pollo a la brasa 98 u ││
│      │  │  ▄███▄  ███▄                 │ │ 3. Chicha morada    87 u ││
│      │  │ ▄█████▄▄████▄                │ │ 4. ...                     ││
│      │  │ 10  12  14  16  18  20       │ │                            ││
│      │  └──────────────────────────────┘ └────────────────────────────┘│
│      │                                                                  │
│      │  Accesos rápidos                                                 │
│      │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐                    │
│      │  │  POS   │ │ Mesas  │ │ Cocina │ │  Caja  │                    │
│      │  └────────┘ └────────┘ └────────┘ └────────┘                    │
└──────┴──────────────────────────────────────────────────────────────────┘
```

### 5.3 Mockup — Dashboard (Compact)

```
┌─────────────────────────────────────┐
│ Dashboard              Hoy ▼   🔄   │
├─────────────────────────────────────┤
│ ┌─────────────────────────────────┐ │
│ │ Ventas hoy        S/ 4,280.50   │ │
│ │ ▲ 12% vs ayer                   │ │
│ └─────────────────────────────────┘ │
│ ┌───────────────┐ ┌───────────────┐ │
│ │ Mesas 12/24   │ │ Comandas 8    │ │
│ └───────────────┘ └───────────────┘ │
│                                     │
│ Ventas por hora                     │
│ ┌─────────────────────────────────┐ │
│ │     [chart scroll horizontal] │ │
│ └─────────────────────────────────┘ │
│                                     │
│ Top productos                       │
│ · Lomo saltado .............. 142   │
│ · Pollo a la brasa ..........  98   │
│                                     │
├─────────────────────────────────────┤
│ 🏠  │ 🧾 POS │ ▦ Mesas │ 👨‍🍳      │
└─────────────────────────────────────┘
```

**Comportamiento:** Pull-to-refresh · skeleton loading · export CSV/PDF en overflow (V1.1)

---

## 6. POS (Fase 2 — diseño visual)

### 6.0 Directrices coloridas

- Catálogo siempre visible; carrito nunca más de 1 tap de distancia
- Categorías como **FilterChip** horizontal scroll
- Búsqueda + escáner en barra superior
- Tipos pedido: **Mostrador** · **Para llevar** · **Delivery** (SegmentedButton)
- Checkout modal full-screen en compact; side panel en expanded

### 6.2 Mockup — POS (Expanded — tablet 10"/12" — TWO PANE)

```
┌──────┬────────────────────────────────────────────┬─────────────────────┐
│  🏠  │  POS          [Mostrador|Llevar|Delivery]  │  🛒 Carrito    (4) │
│  🧾● │  🔍 Buscar...              📷 Escanear    │─────────────────────│
│  ▦   ├────────────────────────────────────────────┤ Pollo a la brasa    │
│  👨‍🍳 │ [Todos][Pollos][Bebidas][Postres][→]     │  1 × S/ 38.00       │
│      │                                            │  + Papas extra      │
│      │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐│─────────────────────│
│      │  │ [img]  │ │ [img]  │ │ [img]  │ │ [img]  ││ Lomo saltado        │
│      │  │Pollo   │ │Lomo    │ │Chicha  │ │Combo   ││  2 × S/ 32.00       │
│      │  │S/38.00 │ │S/32.00 │ │S/ 8.00 │ │Familiar││─────────────────────│
│      │  └────────┘ └────────┘ └────────┘ └────────┘│                     │
│      │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐│ Cliente             │
│      │  │ [img]  │ │ [img]  │ │ [img]  │ │ [img]  ││ [+ Agregar cliente] │
│      │  │Arroz   │ │Ensalada│ │Gaseosa │ │ ...    ││─────────────────────│
│      │  │S/12.00 │ │S/15.00 │ │S/ 5.00 │ │        ││ Subtotal  S/ 102.00 │
│      │  └────────┘ └────────┘ └────────┘ └────────┘│ Descuento S/   0.00 │
│      │                                            │ IGV       S/  18.36 │
│      │  ◀ 1  2  3 ... 12 ▶   (paginación/grid)    │─────────────────────│
│      │                                            │ TOTAL     S/ 120.36 │
│      │                                            │                     │
│      │                                            │ ┌─────────────────┐ │
│      │                                            │ │  COBRAR  S/120  │ │
│      │                                            │ └─────────────────┘ │
│      │                                            │ [Enviar comanda]    │
└──────┴────────────────────────────────────────────┴─────────────────────┘
         │ 65% catálogo                              │ 35% carrito fijo   │
```

### 6.3 Mockup — POS (Compact — celular)

```
┌─────────────────────────────────────┐
│ POS              Mostrador ▼   🔍 📷│
├─────────────────────────────────────┤
│ [Todos][Pollos][Bebidas][Postres →] │
├─────────────────────────────────────┤
│ ┌────────┐ ┌────────┐               │
│ │ [img]  │ │ [img]  │               │
│ │Pollo   │ │ Lomo   │               │
│ │S/38.00 │ │S/32.00 │               │
│ └────────┘ └────────┘               │
│ ┌────────┐ ┌────────┐               │
│ │ ...    │ │ ...    │               │
│ └────────┘ └────────┘               │
│                                     │
│                    ┌──────────────┐ │
│                    │ 🛒 4  S/120  │ │  ← FAB extended
│                    └──────────────┘ │
├─────────────────────────────────────┤
│ 🏠  │ 🧾● POS │ ▦ Mesas │ 👨‍🍳      │
└─────────────────────────────────────┘

Tap FAB → Bottom Sheet carrito (90% height)
```

### 6.4 Mockup — Checkout / Cobro (Modal Expanded)

```
┌─────────────────────────────────────────────────────────────────┐
│ ✕  Cobrar                                              S/ 120.36│
├──────────────────────────────┬──────────────────────────────────┤
│  Tipo documento              │  Métodos de pago                 │
│  ○ Boleta  ● Factura  ○ NV   │                                  │
│                              │  ┌────────┐ ┌────────┐ ┌────────┐│
│  Cliente                     │  │ Efect. │ │ Yape   │ │ Tarjeta││
│  ┌────────────────────────┐  │  │  ●     │ │        │ │        ││
│  │ 🔍 DNI/RUC  Juan Pérez │  │  └────────┘ └────────┘ └────────┘│
│  └────────────────────────┘  │                                  │
│                              │  Monto recibido                  │
│  Serie                       │  ┌────────────────────────────┐  │
│  [B001 - Boleta      ▼]      │  │      S/ 150.00             │  │
│                              │  └────────────────────────────┘  │
│  Descuento (si permiso)      │  ┌───┬───┬───┐                   │
│  [ 0.00 ]                    │  │ 7 │ 8 │ 9 │                   │
│                              │  ├───┼───┼───┤                   │
│  Observaciones               │  │ 4 │ 5 │ 6 │  ← DecimalKeypad │
│  ┌────────────────────────┐  │  ├───┼───┼───┤                   │
│  │ Sin cebolla          │  │  │ 1 │ 2 │ 3 │                   │
│  └────────────────────────┘  │  ├───┼───┼───┤                   │
│                              │  │ C │ 0 │ ⌫ │                   │
│                              │  └───┴───┴───┘                   │
│                              │  Vuelto: S/ 29.64                │
├──────────────────────────────┴──────────────────────────────────┤
│              ┌─────────────────────────────────────┐            │
│              │     CONFIRMAR COBRO    S/ 120.36    │            │
│              └─────────────────────────────────────┘            │
└─────────────────────────────────────────────────────────────────┘
```

**Post-cobro:** Snackbar "Venta registrada" + acción "Imprimir" → BT/TCP según config · Nueva venta limpia carrito

### 6.5 Interacciones POS clave

| Acción | Gestos |
|--------|--------|
| Agregar producto | Tap card → si tiene modificadores → bottom sheet config |
| Combo | Tap → modal configuración paso a paso |
| Cantidad en carrito | Stepper +/- o tap cantidad → keypad |
| Escáner | Tap 📷 → CameraX fullscreen · beep + vibrate al leer |
| Enviar comanda | Botón secundario · auto-print cocina si configurado |
| Pedidos abiertos | Chip en top bar → lista bottom sheet |

---

## 7. Mesas (Salas)

### 7.1 Propósito

Mapa operativo de mesas por piso. Tap mesa libre → modal abrir · Tap ocupada → navegar a detalle mesa (V1.1) o mostrar resumen.

Estados **exactos del backend** — sin derivados.

### 7.2 Mockup — Mesas (Expanded)

```
┌──────┬──────────────────────────────────────────────────────────────────┐
│  🏠  │  Mesas                              🔄    🔍 Buscar mesa...      │
│  🧾  ├──────────────────────────────────────────────────────────────────┤
│  ▦ ● │  [Todos][Salón][Terraza][VIP][→]     ← FloorFilterChips        │
│  👨‍🍳 │                                                                  │
│      │  ┌─ Stats bar ────────────────────────────────────────────────┐ │
│      │  │ ● 8 Libre  ● 10 Ocupada  ● 2 Reservada  ● 4 Por cerrar    │ │
│      │  └────────────────────────────────────────────────────────────┘ │
│      │                                                                  │
│      │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐   │
│      │  │▌        │ │▌        │ │▌        │ │▌        │ │▌        │   │
│      │  │ Mesa 01 │ │ Mesa 02 │ │ Mesa 03 │ │ Mesa 04 │ │ Mesa 05 │   │
│      │  │ 4 pers. │ │ 2 pers. │ │ RESERV. │ │ S/ 85   │ │ Libre   │   │
│      │  │ S/ 120  │ │ 12:34   │ │ 20:00   │ │         │ │         │   │
│      │  │ 🟠 Ocup.│ │ 🟠 Ocup.│ │ 🔵 Res. │ │ 🔴 Cerrar│ │ 🟢 Libre│   │
│      │  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘   │
│      │  ┌─────────┐ ┌─────────┐ ...                                     │
│      │  │▌ Mesa 06│ │▌ Mesa 07│                                       │
│      │  └─────────┘ └─────────┘                                       │
└──────┴──────────────────────────────────────────────────────────────────┘

▌ = barra lateral color estado (4dp)
Card min: 180×140 dp · grid auto-fill min 180dp
```

### 7.3 Mockup — Abrir mesa (Modal)

```
┌─────────────────────────────────────┐
│         Abrir Mesa 05               │
├─────────────────────────────────────┤
│                                     │
│  Comensales                         │
│  ┌─────┐                            │
│  │  -  │    4    │  +  │            │
│  └─────┘                            │
│                                     │
│  Mozo (si admin)                    │
│  [Seleccionar mozo            ▼]    │
│                                     │
│  Notas                              │
│  ┌─────────────────────────────┐    │
│  │ Cumpleaños, mesa junto...   │    │
│  └─────────────────────────────┘    │
│                                     │
│  ┌──────────────┐ ┌──────────────┐  │
│  │   Cancelar   │ │ Abrir mesa   │  │
│  └──────────────┘ └──────────────┘  │
└─────────────────────────────────────┘
```

### 7.4 Mockup — Mesas (Compact)

```
┌─────────────────────────────────────┐
│ Mesas                          🔄   │
├─────────────────────────────────────┤
│ ●8 Libre ●10 Ocup ●2 Res ●4 Cerrar  │
│ [Salón][Terraza][VIP]               │
├─────────────────────────────────────┤
│ ┌───────────┐ ┌───────────┐         │
│ │▌ Mesa 01  │ │▌ Mesa 02  │         │
│ │ 4p S/120  │ │ 2p 12:34  │         │
│ └───────────┘ └───────────┘         │
│ ┌───────────┐ ┌───────────┐         │
│ │▌ Mesa 03  │ │▌ Mesa 04  │         │
│ └───────────┘ └───────────┘         │
├─────────────────────────────────────┤
│ 🏠  │ 🧾 POS │ ▦● Mesas │ 👨‍🍳      │
└─────────────────────────────────────┘
```

**Refresh:** Pull-to-refresh manual (sin WebSocket V1). Indicador "Actualizado hace 2 min".

---

## 8. Cocina (KDS)

### 8.1 Principios KDS

- Legible a 1–2 metros (tablet en pared o soporte)
- Vista por defecto: **Board Kanban** por estado comanda
- Alternativa: vista por ítem (toggle en top bar)
- Acción principal: avanzar estado con tap grande o swipe
- Fondo oscuro opcional (Dark theme forzado en Cocina — setting V1.1)

### 8.2 Mockup — Cocina Board (Expanded — landscape preferido)

```
┌──────┬──────────────────────────────────────────────────────────────────────────┐
│  🏠  │  Cocina          [Board ● | Lista]        🔄 14:32:05    🔊            │
│  🧾  ├─────────────────┬─────────────────┬─────────────────┬──────────────────┤
│  ▦   │ PENDIENTE (4)   │ PREPARACIÓN (3) │ LISTA (2)       │ ENTREGADA ▼ (5)  │
│  👨‍🍳● │                 │                 │                 │                  │
│      │ ┌─────────────┐ │ ┌─────────────┐ │ ┌─────────────┐ │                  │
│      │ │ #042 · M05  │ │ │ #039 · POS  │ │ │ #035 · M02  │ │  (colapsada)   │
│      │ │ hace 8 min  │ │ │ hace 15 min │ │ │ hace 2 min  │ │                  │
│      │ │─────────────│ │ │─────────────│ │ │─────────────│ │                  │
│      │ │ 2× Lomo     │ │ │ 1× Pollo    │ │ │ 3× Arroz    │ │                  │
│      │ │ 1× Chicha   │ │ │ 2× Ensalada │ │ │             │ │                  │
│      │ │ ⚠ sin picante│ │ │             │ │ │             │ │                  │
│      │ │             │ │ │             │ │ │             │ │                  │
│      │ │ [PREPARAR]  │ │ │ [→ LISTA]   │ │ │ [ENTREGAR]  │ │                  │
│      │ └─────────────┘ │ └─────────────┘ │ └─────────────┘ │                  │
│      │ ┌─────────────┐ │ ┌─────────────┐ │                 │                  │
│      │ │ #043 · M08  │ │ │ ...         │ │                 │                  │
│      │ └─────────────┘ │ └─────────────┘ │                 │                  │
└──────┴─────────────────┴─────────────────┴─────────────────┴──────────────────┘
```

### 8.3 Mockup — Tarjeta comanda (detalle)

```
┌─────────────────────────────┐
│ #042          Mesa 05       │  ← Title Large bold
│ ⏱ 08:24        Mostrador    │  ← Label Small
├─────────────────────────────┤
│  2×  Lomo saltado           │
│      · Sin cebolla          │  ← modificadores indent
│  1×  Chicha morada          │
│  1×  Papas fritas           │
├─────────────────────────────┤
│  📝 Nota: cliente alérgico  │  ← SurfaceVariant bg
├─────────────────────────────┤
│  ┌───────────────────────┐  │
│  │    → PREPARACIÓN      │  │  ← Primary 56dp, full width
│  └───────────────────────┘  │
│  [Nota] [Reimprimir] [Anular]│  ← Text buttons
└─────────────────────────────┘
```

### 8.4 Mockup — Cocina (Compact — vista lista)

```
┌─────────────────────────────────────┐
│ Cocina         Board | ●Lista   🔄  │
├─────────────────────────────────────┤
│ ┌─────────────────────────────────┐ │
│ │ 🟠 #042 · Mesa 05 · 8 min       │ │
│ │ 2× Lomo · 1× Chicha             │ │
│ │ [Preparar →]                    │ │
│ └─────────────────────────────────┘ │
│ ┌─────────────────────────────────┐ │
│ │ 🔵 #039 · POS · 15 min          │ │
│ │ 1× Pollo · 2× Ensalada          │ │
│ │ [→ Lista]                       │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ 🏠  │ 🧾 POS │ ▦ Mesas │ 👨‍🍳●     │
└─────────────────────────────────────┘
```

**Timer:** elapsed time desde creación, color warning > 15 min (configurable).

---

## 9. Caja

### 9.1 Propósito

Gestión sesión de caja. Modal bloqueante si cajero sin sesión abierta (paridad web).

### 9.2 Mockup — Modal apertura caja (obligatorio)

```
┌─────────────────────────────────────┐
│                                     │
│     💰  Abrir caja                  │
│                                     │
│  Debes abrir caja para continuar    │
│  operando como cajero.              │
│                                     │
│  Monto inicial                      │
│  ┌─────────────────────────────┐    │
│  │ S/ 200.00                   │    │
│  └─────────────────────────────┘    │
│  ┌───┬───┬───┐                      │
│  │ 7 │ 8 │ 9 │   ← DecimalKeypad     │
│  ...                                │
│                                     │
│  Cuenta / caja                      │
│  [Caja Principal            ▼]      │
│                                     │
│  ┌─────────────────────────────┐    │
│  │      ABRIR CAJA             │    │
│  └─────────────────────────────┘    │
│                                     │
└─────────────────────────────────────┘
         (no dismissible)
```

### 9.3 Mockup — Caja principal (Expanded)

```
┌──────┬──────────────────────────────────────────────────────────────────┐
│  💰● │  Caja                              Sesión #128 · Abierta 09:15   │
│      ├──────────────────────────────────────────────────────────────────┤
│      │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐│
│      │  │ Apertura    │ │ Ventas      │ │ Ingresos    │ │ Egresos     ││
│      │  │ S/ 200.00   │ │ S/ 4,280    │ │ S/ 50.00    │ │ S/ 120.00   ││
│      │  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘│
│      │                                                                  │
│      │  Saldo estimado en caja:  S/ 4,410.00                            │
│      │                                                                  │
│      │  [+ Ingreso]  [- Egreso]  [Arqueo]  [Cerrar caja]               │
│      │                                                                  │
│      │  Movimientos recientes                                           │
│      │  ┌────────────────────────────────────────────────────────────┐ │
│      │  │ 14:32  Venta #1842      Efectivo           +S/ 120.36     │ │
│      │  │ 14:15  Venta #1841      Yape               +S/  85.00     │ │
│      │  │ 13:40  Egreso           Compra insumos      -S/  50.00     │ │
│      │  │ 09:15  Apertura         Caja principal      S/ 200.00     │ │
│      │  └────────────────────────────────────────────────────────────┘ │
└──────┴──────────────────────────────────────────────────────────────────┘
```

### 9.4 Mockup — Cerrar caja / Arqueo

```
┌─────────────────────────────────────┐
│  Cerrar caja                        │
├─────────────────────────────────────┤
│  Saldo sistema:     S/ 4,410.00     │
│                                     │
│  Arqueo (conteo físico)             │
│  ┌─────────────────────────────┐    │
│  │ S/ 4,400.00                 │    │
│  └─────────────────────────────┘    │
│                                     │
│  Diferencia:        -S/ 10.00  ⚠    │
│                                     │
│  Observaciones                      │
│  ┌─────────────────────────────┐    │
│  │ Faltante monedas            │    │
│  └─────────────────────────────┘    │
│                                     │
│  ┌─────────────────────────────┐    │
│  │     CONFIRMAR CIERRE        │    │
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
```

---

## 10. Componentes compartidos V1

| Componente | Pantallas | Notas |
|------------|-----------|-------|
| `BendeyScaffold` | Todas autenticadas | Insets + nav adaptativa |
| `BendeyTopAppBar` | Todas | Sucursal, badge caja, avatar |
| `BendeySearchBar` | POS, Mesas, Clientes | Leading icon + trailing scan |
| `BendeyFilterChipRow` | POS, Mesas | Horizontal scroll |
| `BendeyProductGrid` | POS | LazyVerticalGrid, paginación |
| `BendeyCartPanel` | POS | Side panel / bottom sheet |
| `BendeyTableGrid` | Mesas | LazyVerticalGrid + status bar |
| `BendeyKanbanBoard` | Cocina | Horizontal scroll columns |
| `BendeyDecimalKeypad` | PIN, Checkout, Caja | 56dp keys |
| `BendeyMoneyDisplay` | POS, Caja, Checkout | Formato PEN |
| `BendeyStatusChip` | Mesas, Cocina | Color acento + label |
| `BendeyConfirmDialog` | Anular comanda | PIN requerido |
| `BendeyOfflineBanner` | Global | Degraded connectivity |
| `BendeyPrintSnackbar` | Post-acción | "Imprimiendo..." / error BT |

---

## 11. Motion y feedback

| Evento | Feedback |
|--------|----------|
| Tap producto | Ripple + scale 0.98 |
| Agregar al carrito | Item fly animation hacia carrito + haptic light |
| Cobro exitoso | Haptic success + confetti sutil (opcional setting) |
| Error PIN/API | Shake + haptic error |
| Pull refresh | Material 3 refresh indicator |
| Comanda nueva (manual refresh) | Sound + banner top (si habilitado) |

Duraciones: 200–300 ms para transiciones; 120 ms para micro-interacciones.

---

## 12. Accesibilidad

- Contraste WCAG AA mínimo en textos operativos
- ContentDescription en todos los iconos
- TalkBack: estados de mesa anunciados ("Mesa 5, ocupada, 4 personas, 120 soles")
- Tamaño texto respeta system font scale hasta 1.3× sin romper layout operativo

---

## 13. Fuera de alcance V1 (confirmado)

- Suscripción SaaS
- Sunmi / iMin / Zebra
- WebSocket mesas/cocina
- Estados de mesa derivados
- Modificaciones a `bendey_tenant_restaurante`
- Pantalla detalle Mesa completa (POS en mesa) → V1.1 tras POS estable

---

## 14. Checklist de aprobación visual

Marque cada ítem para aprobar diseño antes de Fase 1 código:

- [ ] **Tokens de color** — rojo tomate light/dark
- [ ] **Estados mesa** — 4 estados backend exactos
- [ ] **Navegación adaptativa** — rail / bottom nav
- [ ] **Login flow** — RUC → Home → PIN/Email
- [ ] **Dashboard** — KPI cards + charts + accesos rápidos
- [ ] **POS two-pane** — catálogo 65% + carrito 35%
- [ ] **POS checkout** — keypad + métodos pago + documento
- [ ] **Mesas grid** — cards con barra estado + stats bar
- [ ] **Cocina KDS** — Kanban 4 columnas + timer
- [ ] **Caja** — modal apertura + movimientos + cierre/arqueo

### Comentarios / cambios solicitados

```
(Espacio para feedback del cliente)




```

---

## 15. Próximo paso tras aprobación

Generación **Fase 1 código**:
1. Proyecto Gradle multi-módulo (`com.bendey.restaurant`)
2. `core:designsystem` implementando tokens de este documento
3. `core:network` + `feature:auth`
4. Navigation Compose adaptativa
5. Shell vacío de POS/Mesas/Cocina/Caja con placeholders según mockups

---

*Documento de diseño UI/UX V1 — cliente Android independiente. No modifica `bendey_tenant_restaurante`.*
