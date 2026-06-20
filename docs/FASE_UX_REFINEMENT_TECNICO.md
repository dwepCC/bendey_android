# Fase UX Refinement — Resumen técnico

Objetivo: pasar de prototipo funcional a producto comercial **sin nuevas funcionalidades**.

## Header profesional (`BendeyAppHeader`)

- Avatar + nombre de usuario a la izquierda
- Restaurante + sucursal en bloque central
- Pills de conexión y caja
- Placeholders: sincronización (`CloudSync`), indicador Live, notificaciones con badge

## Navegación inferior

- Activo: fondo tomate `#C62828`, icono/texto blanco, escala spring
- Inactivo: gris neutro `NavInactive`
- Altura reducida (60 dp) para más área operativa

## POS tablet-first

- Grid adaptativo: 92–118 dp según ancho (8"/10"/12")
- `BendeyProductCard` compacto: imagen más grande, precio en franja destacada
- Badges desde API existente: Mods, Var., Stock, No disp.
- Catálogo 65% / carrito 35% en tablet
- Categorías con animación de color

## Mesas

- Tarjetas con barra lateral de color (sin emojis)
- Estado en mayúsculas + monto visible si ocupada
- Grid adaptativo más denso

## Cocina KDS

- Columnas con header uppercase + contador
- Cantidad grande (headline) estilo Toast/Square
- Botón primario full-width «Avanzar»

## Dashboard / Caja

- KPIs 4 columnas en tablet, cards compactas
- Accesos rápidos 3 columnas en tablet

## Animaciones

- Spring en bottom nav y chips de categoría
- `animateColorAsState` en mesas y KDS
- `animateContentSize` en KPIs y mesas

## Archivos clave

- `core/ui/components/BendeyAppHeader.kt`
- `core/ui/components/BendeyBottomNavigationBar.kt`
- `core/ui/components/BendeyProductCard.kt`
- `core/ui/layout/BendeyTabletLayout.kt`
- `core/designsystem/components/BendeyTableCard.kt`
- `feature/cocina/CocinaScreen.kt`

Capturas: ver [`FASE_UX_REFINEMENT.md`](FASE_UX_REFINEMENT.md) (misma carpeta, checklist + adb).
