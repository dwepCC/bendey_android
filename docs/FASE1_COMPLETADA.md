# Fase 1 — Completada

> Cliente Android independiente · `com.bendey.restaurant` · Min SDK 29

## Entregables

| Componente | Módulo | Estado |
|------------|--------|--------|
| Multi-módulo Gradle | raíz | ✅ |
| Design System (tomate light/dark/dynamic) | `core:designsystem` | ✅ |
| Componentes `Bendey*` | `core:ui` | ✅ |
| Navegación adaptativa | `core:navigation` | ✅ |
| Retrofit + interceptors | `core:network` | ✅ |
| TokenManager | `core:data` | ✅ |
| SessionManager (tenant, user, caja prep) | `core:data` | ✅ |
| Realtime stub (SSE/WS/SignalR) | `core:realtime` | ✅ |
| Auth RUC → Home → PIN/Email | `feature:auth` | ✅ |
| Dashboard KPIs | `feature:dashboard` | ✅ |

## Componentes Bendey* V1

- `BendeyTheme`
- `BendeyPrimaryButton`
- `BendeyTopAppBar`
- `BendeyTextField`
- `BendeyPinKeypad`
- `BendeyLoadingOverlay`
- `BendeyScaffold`
- `BendeyEmptyState`
- `BendeyNavigationSuite`

## Fuera de Fase 1 (placeholders Fase 2)

- POS, Mesas, Cocina, Caja → mensaje "Fase 2" en nav

## Validación en dispositivo

1. Abrir proyecto en Android Studio
2. Sync Gradle
3. `./gradlew :app:assembleDebug`
4. Instalar en tablet/dispositivo API 29+
5. Probar flujo RUC → PIN → Dashboard

## Próximo paso

Tras validación física → **Fase 2**: POS, Mesas, Cocina, Impresión BT/TCP.
