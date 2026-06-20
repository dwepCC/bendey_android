# Fase UX Refinement — Capturas para revisión

Antes de avanzar a nuevas funcionalidades, generar capturas en **tablet 10"** (prioridad) de:

| Pantalla | Ruta / acceso |
|----------|----------------|
| Dashboard | Bottom nav → Inicio |
| POS | Bottom nav → POS |
| Mesas | Bottom nav → Mesas |
| Cocina | Bottom nav → Cocina |
| Caja | Dashboard → Caja |

## Dispositivos recomendados (Android Studio AVD)

| Perfil | Resolución | Uso |
|--------|------------|-----|
| Pixel Tablet | 2560×1600 | 10" referencia |
| Nexus 10 | 2560×1600 | 10" alternativo |
| Medium Tablet | 1280×800 | 8" mínimo |

## Comandos (PowerShell)

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
cd D:\bendey_saas\restaurante_kotlin

# Instalar debug en emulador/dispositivo conectado
.\gradlew :app:installDebug

# Capturas vía adb (requiere emulador encendido o tablet USB)
$out = "D:\bendey_saas\restaurante_kotlin\docs\capturas_ux"
New-Item -ItemType Directory -Force -Path $out | Out-Null

adb shell screencap -p /sdcard/bendey_dashboard.png
adb pull /sdcard/bendey_dashboard.png "$out\01_dashboard.png"
# Repetir navegando manualmente a POS, Mesas, Cocina, Caja
```

## Checklist visual (aprobar / rechazar)

- [ ] Header: avatar, usuario, restaurante, sucursal, caja, conexión visibles sin scroll
- [ ] Bottom nav: pestaña activa tomate con icono blanco; inactivas grises
- [ ] POS: ≥6 productos visibles en 10" sin scroll; precio legible; badges Mods/Stock
- [ ] Mesas: estado reconocible por color en <1 s; grid denso
- [ ] Cocina: columnas KDS legibles; botón Avanzar accesible
- [ ] Caja: KPIs compactos; sin espacios vacíos excesivos
- [ ] Animaciones suaves al cambiar categoría / tab / mesa (sin exceso)

## Cambios aplicados en esta fase

Ver [`FASE_UX_REFINEMENT.md`](FASE_UX_REFINEMENT.md).

**No avanzar a nuevas fases hasta aprobación visual explícita.**
