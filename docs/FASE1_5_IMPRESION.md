# Fase 1.5 — Impresión ESC/POS

> **Estado:** Implementado — pendiente validación en dispositivo físico  
> **Módulo:** `platform/printing` + `feature:printing`

---

## Alcance implementado

| Componente | Archivo | Paridad web |
|------------|---------|-------------|
| Comandos ESC/POS | `EscPosBuilder.kt` | `escposInit`, align, bold, size, cut |
| Normalización texto | `EscPosTextUtils.kt` | `normalizeTextForTicketPrint.ts` |
| Layout comanda | `ComandaLayoutBuilder.kt` | `buildComandaEscPos()` |
| Layout precuenta | `PrecuentaLayoutBuilder.kt` | `buildPrecuentaEscPos()` |
| Transporte Bluetooth SPP | `AndroidPrinterTransport.kt` | `TukichefPrinterPlugin.kt` |
| Transporte TCP :9100 | `AndroidPrinterTransport.kt` | `printTcp()` |
| UI prueba | `PrinterTestScreen.kt` | — |

---

## Acceso en app

**Dashboard → "Prueba de impresión (Fase 1.5)"**

---

## Checklist validación física

Completar en tablet/dispositivo con impresora real:

### Bluetooth ESC/POS

- [ ] Conceder permisos BT (Android 12+)
- [ ] Emparejar impresora en Ajustes Android
- [ ] Seleccionar dispositivo en lista
- [ ] Conectar Bluetooth
- [ ] Imprimir **comanda de prueba**
- [ ] Imprimir **precuenta de prueba**
- [ ] Verificar corte parcial de papel
- [ ] Verificar acentos normalizados (ej. "Pollo" sin caracteres corruptos)

### TCP/IP ESC/POS

- [ ] Configurar IP impresora de red
- [ ] Puerto 9100
- [ ] Imprimir comanda de prueba
- [ ] Imprimir precuenta de prueba

### Resultados

| Prueba | BT | TCP | Notas |
|--------|----|----|-------|
| Comanda | ☐ | ☐ | |
| Precuenta | ☐ | ☐ | |
| Ancho 80mm | ☐ | ☐ | |
| Ancho 58mm | ☐ | ☐ | Opcional |

---

## Tests unitarios

```bash
./gradlew :platform:printing:testDebugUnitTest
```

Verifica:
- Bytes comanda contienen `COMANDA`, mesa, ítems
- Bytes precuenta contienen `PRECUENTA`, `TOTAL`
- Normalización de tildes/ñ
- Columnas 32/48 para 58/80 mm

---

## Arquitectura

```
platform/printing/
├── escpos/          ← layouts puros (port TS)
├── transport/       ← BT + TCP
└── di/              ← Hilt

feature/printing/    ← pantalla de prueba Fase 1.5
```

**Interfaces desacopladas** (`PrinterTransport`, `PrinterRepository`) preparadas para Fase 2 integración en POS/Mesas.

---

## Próximo paso

Tras marcar checklist en dispositivo físico → **Fase 2**: POS, Mesas, Cocina.
