# Limitaciones conocidas — Bendey Android V1

Limitaciones **documentadas** que **no bloquean** la operación diaria del restaurante en producción.

No incluye bugs abiertos. Ante comportamiento incorrecto vs reglas de negocio → reportar como bug funcional.

---

## Conectividad y offline

| Limitación | Impacto | Mitigación operativa |
|------------|---------|----------------------|
| **Sin modo offline** | Requiere internet para operar (mesas, POS, caja, ventas) | WiFi estable en local; reintentar tras reconexión |
| **Indicador "Online" fijo** | Header muestra Online siempre; no refleja estado real de red | Errores API muestran mensaje de conexión |
| **SSE billing sin auto-reconexión** | Tras corte largo de red, estados SUNAT en Ventas pueden requerir refresh manual | Pull-to-refresh o reentrar tab Facturación |

---

## Impresión

| Limitación | Impacto | Mitigación |
|------------|---------|------------|
| Solo **Bluetooth SPP** y **TCP :9100** | No Sunmi/iMin SDK nativo en V1 | Impresoras ESC/POS genéricas |
| Config impresoras **local por dispositivo** | No sincroniza entre tablets | Configurar cada equipo operativo |
| Conexión BT no persistente entre sesiones | Puede requerir reconectar tras reinicio app | Prueba impresión antes del servicio |

---

## Exportaciones y documentos

| Limitación | Impacto | Mitigación |
|------------|---------|------------|
| Algunos exports en **CSV** (no `.xlsx`) | Abre en Excel igualmente | Compartir/guardar CSV |
| PDF reportes generados localmente | Layout simple vs jsPDF web | Contenido fiscal/operativo presente |

---

## Configuración y administración

| Limitación | Impacto | Mitigación |
|------------|---------|------------|
| **Sin módulo Suscripción SaaS** | Admin no gestiona plan desde app Android | Panel web / tenant central |
| Empresa: sin editor logo/ubigeo/moneda en UI | Campos secundarios no editables desde Android | Panel central si aplica |
| Series: categorías fijadas en `venta` para UI simplificada | NC/ND/guías administrables desde panel web | Checkout usa series venta |
| Certificados SUNAT / ambiente FE | Solo lectura (`sunat_enabled` panel central) | Igual que cliente Windows |

---

## Cocina y mesas

| Limitación | Impacto | Mitigación |
|------------|---------|------------|
| **Sin WebSocket** cocina/mesas | Actualización manual o pull-to-refresh | Refrescar KDS periódicamente |
| Sin polling automático en cocina | No push en tiempo real sub-segundo | Operación estándar restaurante |

---

## Plataforma Android

| Limitación | Impacto | Mitigación |
|------------|---------|------------|
| **Min SDK 29** (Android 10+) | No dispositivos Android 9 o anteriores | Hardware POS moderno |
| Rotación: recreación Activity estándar | ViewModels preservan estado; diálogos pueden cerrarse en casos extremos | Bloquear rotación en kiosko si se desea (config dispositivo) |
| `runBlocking` en capa sesión para OkHttp | Patrón sync en interceptor; no en UI directa | Aceptado en V1 |

---

## Diferencias vs cliente Windows (no son bugs)

| Área | Android V1 | Nota |
|------|------------|------|
| Navegación | Bottom bar + drawer | Independiente |
| Paginación catálogo | Carga por página vs infinite scroll web | Mismo backend |
| Caja cajero | Exige sesión caja para cobro (roles restringidos) | Más estricto |
| WhatsApp comprobante | Integración nativa Android | Extra Android |

---

## Fuera de alcance V1 (por diseño)

- Modo offline con cola de sincronización
- Suscripción / billing SaaS in-app
- Impresoras USB directas (solo BT/TCP)
- WebSocket tiempo real universal
- Paridad visual con React/Tailwind

---

## Actualización de este documento

Agregar entrada solo cuando se confirme una limitación **conocida y aceptada** en producción.  
Remover entrada cuando se implemente la funcionalidad en una versión posterior.
