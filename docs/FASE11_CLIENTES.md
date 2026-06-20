# Fase 11 — Gestión de clientes

Paridad con `ClientesPage.tsx` del panel Capacitor.

## Alcance

| Funcionalidad | Estado |
|---------------|--------|
| Listado con búsqueda debounced | ✅ |
| Crear / editar / eliminar cliente | ✅ |
| Activar / desactivar (toggle) | ✅ |
| Consulta DNI/RUC (apiperu vía backend) | ✅ |
| Campos: doc, nombre, comercial, dirección, ubigeo, tel, email | ✅ |

## APIs

```
GET    /api/contacts?q=&type=customer
GET    /api/contacts/{id}
POST   /api/contacts
PUT    /api/contacts/{id}
DELETE /api/contacts/{id}
PATCH  /api/contacts/{id}/toggle
POST   /api/consulta/dni   { dni, tenant_ruc }
POST   /api/consulta/ruc   { ruc, tenant_ruc }
```

Tipo fijo: `customer`. Por defecto la API lista solo contactos activos.

## Módulos

```
core/domain/contacts/     — CustomerContact, ContactFormInput, ContactsRepository
core/data/repository/     — ContactsRepositoryImpl
core/network/api/         — ContactsApi, ConsultaApi
feature/clientes/         — ClientesScreen, ClientesViewModel
```

## Navegación

- Dashboard → Gestión → **Clientes**
- Ruta: `clientes` (`TopLevelDestination.CLIENTES`)

## Permiso backend

`o.ch` (OrdersCharge) — validación en backend.

## Integración checkout

Los clientes creados aquí aparecen en el selector de **Cobrar** (POS/mesa) vía `BillingRepository.loadCheckoutMeta`.

## Flujo de prueba

1. Dashboard → Clientes
2. Buscar por nombre o documento
3. **+** → crear cliente (RUC + Consultar SUNAT)
4. Editar teléfono/email
5. Toggle activo/inactivo
6. POS → Cobrar → verificar que el cliente aparece en la lista
