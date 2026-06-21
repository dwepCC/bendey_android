# QA Checklist — Producción Bendey Android V1

Checklist manual para validar release en dispositivo real antes de despliegue en restaurante.

**Entorno:** tenant de prueba + sucursal activa + impresora BT o TCP disponible (opcional según caso).

Marcar ✅ / ❌ / N/A por ítem.

---

## LOGIN

- [ ] Vincular tenant por RUC → resuelve slug y URL API
- [ ] Login email/contraseña admin
- [ ] Login PIN staff (4–6 dígitos)
- [ ] Cambio de usuario desde header
- [ ] Logout limpia sesión y vuelve a auth
- [ ] Usuario sin permisos → mensaje y cierre sesión

---

## PIN

- [ ] PIN acceso: login exitoso con PIN válido
- [ ] PIN acceso: rechazo con PIN incorrecto
- [ ] PIN operaciones (Configuración): configurar 4–6 dígitos
- [ ] Anular comanda / borrador exige PIN operaciones configurado
- [ ] Quitar PIN acceso de usuario (editar staff → clear pin)

---

## DASHBOARD

- [ ] Carga KPIs con rango Hoy / Semana / Mes
- [ ] Restricción de fechas custom según rol
- [ ] Tab Operación: datos coherentes con ventas del periodo
- [ ] Tab Catálogo: top productos/combos
- [ ] Export CSV/PDF operación
- [ ] Export CSV/PDF catálogo
- [ ] Navegación a Ventas / Mesas desde gestión

---

## POS

- [ ] Catálogo carga con `catalog_only`
- [ ] Filtro por categoría y área preparación
- [ ] Agregar producto simple y con modificadores
- [ ] Agregar combo con extras por componente
- [ ] Producto manual
- [ ] Editar precio/notas en carrito antes de enviar
- [ ] Enviar comanda → backend registra + impresión local (si configurada)
- [ ] Guardar borrador y reanudar
- [ ] Anular borrador con PIN
- [ ] Precuenta imprime
- [ ] Checkout boleta / NV / factura (según series y SUNAT)
- [ ] Multipago + descuento (con permiso)
- [ ] Bloqueo cobro sin series válidas
- [ ] Bloqueo factura sin RUC cliente

---

## MESAS

- [ ] Listado mesas por piso con estados correctos
- [ ] Abrir mesa libre → crea sesión
- [ ] Detalle mesa: agregar productos y enviar comanda
- [ ] Precuenta desde mesa
- [ ] Cobrar mesa → venta + mesa libre
- [ ] Cerrar mesa vacía (permiso `o.ch`)
- [ ] Admin mesas: CRUD pisos/mesas (si `g.p`)

---

## COCINA

- [ ] KDS muestra comandas pendientes
- [ ] Avanzar estado individual
- [ ] Marcar ronda lista (batch)
- [ ] Filtro por área preparación
- [ ] Pull-to-refresh actualiza comandas

---

## CAJA

- [ ] Apertura con monto 0 o positivo
- [ ] Ingreso / egreso manual con categoría
- [ ] Arqueo por denominaciones
- [ ] Cierre con advertencia operaciones activas
- [ ] Reporte sesión + productos vendidos
- [ ] Reporte movimientos (fechas + export)
- [ ] CRUD métodos pago y cuentas (supervisor/admin)
- [ ] Cajero `c.v`: config caja solo lectura

---

## VENTAS

- [ ] Tab Notas de venta: listado y filtros
- [ ] Tab Facturación: solo si SUNAT habilitado
- [ ] Tab Notas de crédito
- [ ] Detalle venta: PDF, reimpresión
- [ ] Anular NV con motivo
- [ ] Emitir FE con serie
- [ ] Enviar / reenviar SUNAT
- [ ] Anular con NC
- [ ] Descargar XML enviado, generado, CDR
- [ ] Export listado PDF/CSV
- [ ] SSE actualiza estado billing en vivo

---

## CONFIGURACIÓN

- [ ] Admin `s.m`: tabs General, Operación, Sucursales, Series
- [ ] Sin `s.m`: solo acceso Impresoras (no Configuración admin)
- [ ] Editar datos contacto empresa
- [ ] Editar IGV/régimen/zona beneficio
- [ ] Crear/editar staff con rol, PIN, sucursales
- [ ] Crear/editar sucursal + código domicilio fiscal
- [ ] Crear/editar serie; bloqueo SUNAT 00 sin FE
- [ ] No eliminar serie en uso

---

## IMPRESIÓN

- [ ] Configurar impresora Bluetooth (emparejamiento + prueba)
- [ ] Configurar impresora TCP (IP:9100 + prueba)
- [ ] Slot comandas default
- [ ] Slot comandas por área preparación
- [ ] Slot precuenta
- [ ] Slot documentos (post-checkout)
- [ ] Auto-impresión comanda tras enviar desde POS/Mesa

---

## SUNAT

- [ ] Checkout respeta `sunat_enabled` del tenant
- [ ] Series filtradas (00 sin FE; 01/03 con FE)
- [ ] QR en ticket cuando aplica FE
- [ ] Ventas: pipeline accepted/rejected/retry visible
- [ ] Descarga documentos SUNAT desde detalle

---

## MULTIPAGO

- [ ] POS/Mesa: agregar 2+ métodos de pago
- [ ] Suma pagos = total con descuento
- [ ] Método vinculado a cuenta bancaria válida
- [ ] Efectivo sin permiso caja bloqueado para cajero restringido

---

## BLUETOOTH

- [ ] Permisos BT concedidos (Android 12+)
- [ ] Listar dispositivos emparejados
- [ ] Conectar e imprimir prueba
- [ ] Reconectar tras apagar/encender impresora
- [ ] Mensaje claro si BT desactivado

---

## TCP

- [ ] Impresora en red accesible
- [ ] Puerto 9100 responde
- [ ] Impresión prueba exitosa
- [ ] Timeout/error legible si host incorrecto

---

## ROTACIÓN

- [ ] Rotar dispositivo en POS → estado carrito preservado (ViewModel)
- [ ] Rotar en Mesa detalle → sesión activa
- [ ] Rotar en Cocina → sin crash
- [ ] Rotar en checkout dialog → datos formulario preservados

---

## BACKGROUND

- [ ] App en background durante checkout → no duplica cobro al volver
- [ ] SSE Ventas se desconecta/reconecta al cambiar tab (sin crash)
- [ ] Sesión token persiste tras minimizar app

---

## OFFLINE

- [ ] Sin red: operaciones muestran error de conexión (no crash)
- [ ] No se pierde login al recuperar red
- [ ] Impresión local funciona sin red (BT/TCP) si ya configurada
- [ ] **Esperado:** app no opera offline completo (requiere API)

---

## RECONEXIÓN

- [ ] Recuperar WiFi → siguiente acción API funciona sin reiniciar app
- [ ] Pull-to-refresh recarga datos tras corte breve
- [ ] Login PIN tras expiración token → flujo auth normal

---

## Registro de ejecución

| Campo | Valor |
|-------|-------|
| Versión app | |
| Build | |
| Dispositivo | |
| Android | |
| Tenant / sucursal | |
| Tester | |
| Fecha | |
| Resultado global | ✅ Release / ❌ Bloqueado |

**Notas / incidencias:**

---

## Criterio de aprobación

- Todos los ítems **críticos operativos** (LOGIN, PIN, POS cobro, Mesas cobro, Caja apertura/cierre, Ventas listado) en ✅.
- Cero crashes reproducibles en flujo principal.
- Incidencias menores documentadas en `KNOWN_LIMITATIONS.md` o ticket de bug.
