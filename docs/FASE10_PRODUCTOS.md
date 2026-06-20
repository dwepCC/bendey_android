# Fase 10 — Gestión de productos

Paridad con `ProductosPage.tsx` y `CategoriasPage.tsx` del panel Capacitor.

## Alcance

| Funcionalidad | Estado |
|---------------|--------|
| Listado paginado con búsqueda debounced | ✅ |
| Filtros por categoría y área de preparación | ✅ |
| Crear / editar / eliminar producto | ✅ |
| Campos: nombre, código, precio, categoría, área, IGV, carta, stock | ✅ |
| Pestaña Categorías (CRUD) | ✅ |
| Import Excel | ⏳ Fase 10.1 |
| Imagen de producto | ⏳ Fase 10.1 |
| Presentaciones / variantes | ⏳ Fase 10.1 |
| Modificadores / extras | ⏳ Fase 11 |
| Combos | ⏳ Fase 12 |

## APIs

```
GET    /api/products?q=&restaurant_only=true&active_only=true&page=&per_page=&category_id=&preparation_area=
GET    /api/products/{id}
POST   /api/products
PUT    /api/products/{id}
DELETE /api/products/{id}
GET    /api/categories
POST   /api/categories
PUT    /api/categories/{id}
DELETE /api/categories/{id}
```

En gestión **no** se envía `catalog_only` (incluye productos solo-combo, como Capacitor).

## Módulos

```
core/domain/products/     — ProductItem, ProductFormInput, ProductsRepository
core/data/repository/     — ProductsRepositoryImpl
core/network/dto/         — DTOs ampliados
feature/productos/        — ProductosScreen, ProductosViewModel
```

## Navegación

- Dashboard → Gestión → **Productos**
- Ruta: `productos` (`TopLevelDestination.PRODUCTOS`, fuera del bottom bar)
- En tablet: visible en navigation rail

## Permiso backend

`g.p` (gestión productos) — validación en backend; UI aún sin guard de permisos local.

## Flujo de prueba

1. Dashboard → Productos
2. Pestaña **Productos** → buscar, filtrar por categoría/área
3. **+** → crear producto con precio y categoría
4. Editar producto existente
5. Eliminar producto (confirmación)
6. Pestaña **Categorías** → crear, editar, eliminar
7. POS → verificar que el producto nuevo aparece en catálogo (si `available_for_sale=true`)
