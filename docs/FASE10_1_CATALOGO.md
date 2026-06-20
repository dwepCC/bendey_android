# Fase 10.1 — Catálogo extendido

Extensión del módulo de productos y nuevas pantallas de gestión, alineadas con Capacitor.

## Productos (`feature:productos`)

- **Imagen:** selector de galería + upload multipart `POST /api/products/{id}/image` tras guardar.
- **Import Excel:** lectura `.xlsx` con `fastexcel-reader`, validación de columnas (nombre, precio_venta, etc.) e import por chunks de 25 vía `POST /api/products/bulk-import/restaurant`.
- **Presentaciones:** editor en formulario de producto (`has_variants`, array `presentations`).
- **Modificadores en producto:** chips de grupos asignados (`modifier_group_ids`).
- **Nav catálogo:** chips Productos | Modificadores | Combos en pantalla de productos.

## Modificadores (`feature:modificadores`)

CRUD de grupos `/api/modifier-groups` con opciones, modo single/multiple/quantity.

## Combos (`feature:combos`)

Listado y editor MVP (tipo fijo/configurable/promo, precio base, productos fijos por ID).

## Configuración (`feature:configuracion`)

- Datos empresa (`/api/company/config`)
- SUNAT lectura (`/api/company/sunat`)
- Sucursales (`/api/company/branches`)
- PIN anulación (`/api/restaurant/settings`)
- Enlace a impresoras locales (Fase 1.5)

## Repartidores (`feature:repartidores`)

CRUD motorizados y empresas de delivery (`/api/restaurant/delivery-drivers`, `delivery-companies`).

## Core

- DTOs: `CatalogDtos.kt`
- APIs: `CatalogApi.kt`, extensiones en `ProductsApi`
- Repos: `ModifiersRepository`, `CombosRepository`, `DeliveryRepository`, `SettingsRepository`, `ProductImportRepository`, `ProductImageRepository`

## Acceso UI

Dashboard → Gestión → Productos / Configuración / Repartidores  
Productos → chips Modificadores / Combos
