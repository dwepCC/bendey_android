# Certificación funcional Android — Bendey Restaurant

Documento interno de estado por módulo. Cada módulo se certifica de forma independiente: mismos endpoints, payloads, validaciones y resultado en backend que la referencia operativa (Capacitor).

**Criterio:** si el restaurante puede realizar la misma operación de negocio y obtener el mismo resultado en backend, el módulo está certificado.

**Última actualización:** 2025-06-20 — **CIERRE V1 PRODUCCIÓN**

> Documento maestro de cierre: [`ANDROID_V1_CERTIFICADO.md`](ANDROID_V1_CERTIFICADO.md)

---

## Estado por módulo

| Módulo | Estado |
|--------|--------|
| Dashboard | ✅ Certificado |
| Productos | ✅ Certificado |
| Cocina | ✅ Certificado |
| **Mesas** | **✅ Certificado** |
| **POS** | **✅ Certificado** |
| **Ventas** | **✅ Certificado** |
| **Caja** | **✅ Certificado** |
| **Configuración** | **✅ Certificado** |

---

## POS — Certificación funcional Android

### Veredicto

**POS = FUNCIONALMENTE CERTIFICADO PARA PRODUCCIÓN**

Todas las operaciones de negocio auditadas envían los mismos endpoints/payloads que la referencia y aplican las mismas reglas de validación. No quedan operaciones bloqueantes pendientes de implementación.

---

### 1. Funcionalidades certificadas

| Área | Operación | Endpoint / regla |
|------|-----------|------------------|
| Catálogo | Productos POS con `catalog_only=true` | `GET /products` |
| Catálogo | Filtro por área de preparación | `preparation_area` en query |
| Catálogo | Combos en pestaña combos | `GET /combos` |
| Catálogo | Scanner código de barras | `GET /products?catalog_only=true&q=` |
| Carrito | Agregar producto simple / con variantes | `modifiers_json`, `item_kind=product` |
| Carrito | Modificadores modo quantity (`quantity` en JSON) | Validación + payload |
| Carrito | Combos con `component_modifiers` en `combo_config_json` | `POST /combos/{id}/resolve` + `addOrder` |
| Carrito | Editar precio unitario y notas antes de enviar | Payload `unit_price`, `notes` en items |
| Carrito | Producto manual | `item_kind=manual`, nombre/precio/cantidad |
| Sesión | Abrir / actualizar sesión POS | `POST/PATCH /sessions` |
| Sesión | `contact_id` en POST/PATCH | Campo en payload sesión |
| Sesión | Notas default: `"Venta directa"` / `"POS"` | Regla quick_sale vs restaurante |
| Sesión | `save_as_draft` solo en POST (no en PATCH) | Alineado con backend |
| Sesión | Restaurar `contact_id` al reanudar pedido | `GET /sessions/{id}` |
| Comandas | Enviar comanda (`addOrder`) | `POST /sessions/{id}/orders` |
| Comandas | Impresión local por área | `KitchenPrintService` |
| Comandas | Marcar impresa | `POST /table-orders/{id}/printed` |
| Comandas | Editar notas enviadas | `PATCH /comandas/{id}/notes` |
| Comandas | Reimprimir / reimprimir todas | Impresión local |
| Borradores | Guardar borrador | `save_as_draft: true` en POST |
| Borradores | Listar y reanudar pendientes | `GET /open-orders` + sesión |
| Borradores | Anular borrador (solo PIN configurado) | PIN operaciones, no `s.m` |
| Precuenta | Obtener e imprimir | `GET /sessions/{id}/precuenta` + impresora |
| Checkout | Series filtradas por SUNAT habilitado | Validación dominio |
| Checkout | Bloqueo factura electrónica sin SUNAT | Validación |
| Checkout | Factura requiere cliente con RUC | Validación |
| Checkout | Método de pago vinculado a cuenta | Validación `bank_account_id` |
| Checkout | Series vacías bloquean cobro | Validación |
| Checkout | Efectivo sin permiso caja | Validación `c.v` / rol |
| Checkout | Multipago + descuento | `billSession` payload |
| Checkout | Cierre sesión al cobrar | `close_session: true` |
| Facturación | Boleta / factura / nota de venta | `POST /sessions/{id}/bill` |
| Facturación | Comprobante PDF / reimpresión / WhatsApp | Post-checkout Android |
| Permisos | Cobrar pedidos (`o.ch`) | Gate checkout |
| Permisos | Anular comanda enviada | PIN + permiso |
| Tipos pedido | Llevar / delivery / venta directa | `order_type` + detalles |

---

### 2. Funcionalidades implementadas en esta certificación POS

Implementación de brechas que **sí impedían** operaciones de negocio:

1. **Precuenta** — `getPrecuenta` + botón en carrito + impresión automática.
2. **Edición precio/notas en carrito** — dominio + UI editable antes de `addOrder`.
3. **Combos `component_modifiers`** — resolve, estado combo, UI extras por componente.
4. **Modificadores quantity** — `setOptionQuantity`, validación, UI +/-.
5. **`markTableOrderPrinted`** — tras enviar comanda e imprimir.
6. **Validaciones checkout** — SUNAT, RUC, cuentas bancarias, series, efectivo.
7. **`updateComandaNotes`** — PATCH + diálogo + acción en comandas enviadas.
8. **Anular borrador** — exige PIN de operaciones configurado.
9. **`save_as_draft` solo POST** — `toUpdateDto()` sin draft.
10. **`contact_id` sesión** — POST/PATCH + restauración al reanudar.
11. **Notas sesión** — defaults `"Venta directa"` / `"POS"`.
12. **`preparation_area`** — mapeo DTO → dominio → filtro chips.
13. **UI wiring** — diálogos producto/combo, carrito editable, precuenta, notas comanda.

---

### 3. Diferencias ignoradas (propias de plataforma o mejora Android)

No impiden realizar ninguna operación de negocio:

| Diferencia | Motivo |
|------------|--------|
| Paginación infinita vs `page=1` | Mismo catálogo funcional; no bloquea operación |
| Modal preview precuenta en pantalla | Capacitor muestra preview; Android imprime directo — mismo backend |
| Kanban / layout tablet distinto | Solo presentación |
| Snackbars / animaciones / loading UX | Solo presentación |
| Caja obligatoria para **todo** cobro de cajeros | Mejora Android (Capacitor solo exige caja en efectivo) |
| Anular comanda enviada desde POS | Extra Android; Capacitor POS no lo expone en POS |
| Reenvío SUNAT en estado `rejected` | Extra Android en ventas/post-checkout |
| WhatsApp comprobante | Extra Android nativo |

---

### 4. Operaciones de negocio pendientes

**Ninguna** en el módulo POS.

---

## Ventas — Certificación funcional Android

### Veredicto

**VENTAS = FUNCIONALMENTE CERTIFICADO PARA PRODUCCIÓN**

---

### Endpoints comparados (Android = Capacitor)

| Operación | Endpoint | Estado |
|-----------|----------|--------|
| Listado | `GET /api/sales` | ✅ |
| Export masivo | `GET /api/sales?export_all=1` | ✅ |
| Detalle | `GET /api/sales/{id}` | ✅ |
| Anular NV | `POST /api/sales/{id}/cancel` `{reason}` | ✅ |
| Emitir FE | `POST /api/sales/{id}/issue-electronic` `{series_id, issue_date?}` | ✅ |
| Enviar SUNAT | `POST /api/billing/send/{id}` | ✅ |
| Reenviar SUNAT | `POST /api/billing/resend/{id}` | ✅ |
| Anular con NC | `POST /api/billing/void-with-credit-note/{id}` `{reason}` | ✅ |
| PDF oficial | `GET /api/billing/invoice/{id}/document/pdf` | ✅ |
| XML enviado | `GET /api/billing/invoice/{id}/document/xml` | ✅ |
| XML generado | `GET /api/billing/invoice/{id}/document/xml-generated` | ✅ |
| CDR | `GET /api/billing/invoice/{id}/document/cdr` | ✅ |
| SSE estados | `GET /api/billing/events?access_token=` | ✅ |
| Series emitir | `GET /api/company/series?category=venta` | ✅ |
| Métodos pago filtro | `GET /api/cashbank/payment-methods?all=1` | ✅ |

No implementados (no usados en Capacitor VentasPage): `GET /api/billing/invoice/{id}`, `GET /api/billing/status/{id}`.

---

### Payloads comparados

**Listado query:** `q`, `from`, `to`, `page`, `per_page`, `sunat_code` (`00` / `01,03`), `doc_type` (`NOTA_CREDITO`), `billing_status`, `payment_method`, `export_all=1` — idénticos.

**Anular NV:** `{ "reason": "string" }` — idéntico.

**Emitir FE:** `{ "series_id": number, "issue_date?": "YYYY-MM-DD" }` — idéntico.

**Anular NC:** `{ "reason": "string" }` — idéntico.

**Send/Resend SUNAT:** body vacío — idéntico.

---

### Permisos comparados

| Acción | Capacitor | Android |
|--------|-----------|---------|
| Acceso módulo `/ventas` | `o.ch` OR `c.v` | `o.ch` OR `c.v` |
| Acciones internas (emitir, anular, SUNAT, export) | Sin sub-permisos | Sin sub-permisos |
| Tab Facturación/NC sin SUNAT | Bloqueo fetch + mensaje | Bloqueo fetch + mensaje |

---

### Implementado en esta certificación

1. Descarga/compartir **XML enviado**, **XML generado**, **CDR**.
2. Visor **XML** en diálogo (Facturación).
3. Export listado **PDF** y **Excel** (CSV vía `export_all=1` + generación local).
4. **`sunatEnabled`** desde config tenant (`CatalogApi`), no inferido por series.
5. Bloqueo listado Facturación/NC cuando SUNAT deshabilitado.

---

### Diferencias ignoradas (plataforma)

| Diferencia | Motivo |
|------------|--------|
| Infinite scroll vs paginación fija | Mismo backend, misma data |
| Export Excel `.xlsx` vs CSV compartible | Mismo dato exportable; CSV abre en Excel |
| Bottom sheet vs modal detalle | Solo presentación |
| Reenvío SUNAT en `rejected` | Mejora Android |
| WhatsApp PDF local | Mejora Android nativa |
| `GET /billing/invoice` y `/status` no usados en UI Capacitor | No bloquean operación |

---

### Confirmación backend

Android envía los mismos endpoints, query params y bodies que la referencia operativa para todas las operaciones de negocio del módulo Ventas. Los documentos SUNAT se descargan del mismo path `/document/{kind}`.

---

## Caja — Certificación funcional Android

### Veredicto

**CAJA = FUNCIONALMENTE CERTIFICADO PARA PRODUCCIÓN**

Todas las operaciones de negocio auditadas (apertura, cierre, arqueo, movimientos manuales, reportes, configuración de cuentas/métodos, permisos) utilizan los mismos endpoints y payloads que la referencia Capacitor. Las brechas bloqueantes identificadas en la auditoría fueron implementadas en esta certificación.

---

## Configuración — Certificación funcional Android

### Veredicto

**CONFIGURACIÓN = FUNCIONALMENTE CERTIFICADO PARA PRODUCCIÓN**

---

## Mesas — Certificación funcional Android

### Veredicto

**MESAS = FUNCIONALMENTE CERTIFICADO PARA PRODUCCIÓN**

Operaciones verificadas: grid por piso, abrir mesa, detalle sesión, comandas, precuenta, checkout, cierre mesa, permisos (`t.o`, `o.ch`), integración caja y series checkout.

---

## Cierre V1

Migración y certificación funcional **finalizadas**. Ver [`ANDROID_V1_CERTIFICADO.md`](ANDROID_V1_CERTIFICADO.md).

---

## Cómo usar este documento

Al agregar una función nueva (ej. reservas, promociones):

1. Identificar el módulo afectado.
2. Certificar **solo ese módulo** contra backend.
3. Actualizar la tabla de estado arriba.
4. No re-auditar toda la aplicación.
