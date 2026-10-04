package com.bendey.restaurant.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Rol semántico con cinco valores (DESIGN-SYSTEM §3.2).
 *
 * - [solid]: RELLENO (barra, icono, fondo de botón). Nunca como texto sobre blanco.
 * - [onSolid]: texto/icono sobre [solid].
 * - [text]: texto sobre superficie blanca.
 * - [tint]: fondo suave (chip, banner).
 * - [onTint]: texto sobre [tint].
 */
@Immutable
class BendeyTone(
    val solid: Color,
    val onSolid: Color,
    val text: Color,
    val tint: Color,
    val onTint: Color,
)

/**
 * Estado operativo (cocina y mesa) — DESIGN-SYSTEM §3.5.
 * [fill] = barra/borde/relleno; [tint] = fondo de tarjeta o chip; [onTint] = texto sobre [tint].
 */
@Immutable
class BendeyStateColors(
    val fill: Color,
    val tint: Color,
    val onTint: Color,
)

/**
 * Paleta Bendey Restaurant — tema claro exclusivo.
 * Valores de DESIGN-SYSTEM §3 (pares de contraste validados en `ContrastTest`).
 */
@Immutable
object BendeyColors {
    // ---- Marca: escala tomate completa (DESIGN-SYSTEM §3.1) ----
    val Brand50 = Color(0xFFFDF3F3)
    val Brand100 = Color(0xFFFCE8E8)
    val Brand200 = Color(0xFFF9D1D1)
    val Brand300 = Color(0xFFF2A8A9)
    val Brand400 = Color(0xFFE87375)
    /** Solo UI grande: como texto sobre blanco da 4,00:1. */
    val Brand500 = Color(0xFFDB4F51)
    val Brand600 = Color(0xFFC9393B)
    val Brand700 = Color(0xFFA82F31)
    val Brand800 = Color(0xFF8C2628)
    val Brand900 = Color(0xFF721F21)
    val Brand950 = Color(0xFF441012)

    // Primario — Tomate Bendey (= Brand600)
    val Primary = Brand600
    val OnPrimary = Color.White
    val PrimaryContainer = Brand100
    val OnPrimaryContainer = Brand900
    /** Marco exterior app (React `rest-900`) */
    val Rest900 = Brand900
    val Rest800 = Brand800

    // ---- Neutros (DESIGN-SYSTEM §3.3) ----
    val Background = Color(0xFFFAFAF9)
    val Surface = Color.White
    val SurfaceVariant = Color(0xFFF1F3F4)
    val Outline = Color(0xFFE0E0E0)
    val OnSurface = Color(0xFF1C1917)
    val OnSurfaceVariant = Color(0xFF5F6368)
    /** 4,61:1 SOLO sobre blanco (sobre #F5F5F5 baja a 4,23). */
    val NavInactive = Color(0xFF757575)

    // ---- Semánticos: roles completos (DESIGN-SYSTEM §3.2) ----
    val SuccessTone = BendeyTone(
        solid = Color(0xFF2E7D32),
        onSolid = Color.White,
        text = Color(0xFF2E7D32),
        tint = Color(0xFFE8F5E9),
        onTint = Color(0xFF1B5E20),
    )
    val WarningTone = BendeyTone(
        solid = Color(0xFFF9A825),
        onSolid = Color(0xFF1C1917),
        text = Color(0xFFB45309),
        tint = Color(0xFFFFF8E1),
        onTint = Color(0xFF92400E),
    )
    /** DEC-02: el rojo de peligro ya NO es el tomate de marca. Siempre con icono + texto. */
    val DangerTone = BendeyTone(
        solid = Color(0xFFB71C1C),
        onSolid = Color.White,
        text = Color(0xFFB3261E),
        tint = Color(0xFFFFEBEE),
        onTint = Color(0xFFB71C1C),
    )
    val InfoTone = BendeyTone(
        solid = Color(0xFF0277BD),
        onSolid = Color.White,
        text = Color(0xFF0277BD),
        tint = Color(0xFFE1F5FE),
        onTint = Color(0xFF01579B),
    )
    val NeutralTone = BendeyTone(
        solid = Color(0xFF57534E),
        onSolid = Color.White,
        text = Color(0xFF57534E),
        tint = Color(0xFFF5F5F4),
        onTint = Color(0xFF44403C),
    )

    // Alias planos conservados (los llamadores existentes siguen compilando).
    // Success/Warning/Info son RELLENO o icono; para texto usar *Text.
    val Success = SuccessTone.solid
    val SuccessContainer = SuccessTone.tint
    val OnSuccess = SuccessTone.onSolid
    val SuccessText = SuccessTone.text

    val Info = InfoTone.solid
    val InfoContainer = InfoTone.tint
    val OnInfo = InfoTone.onSolid
    val InfoText = InfoTone.text

    /** SOLO relleno/barras/iconos (1,97:1 sobre blanco). Para texto: [WarningText]. */
    val Warning = WarningTone.solid
    val WarningContainer = WarningTone.tint
    /** Texto/icono sobre [Warning] y sobre superficies cálidas (antes #5D4037). */
    val OnWarning = WarningTone.onSolid
    /** Texto de aviso sobre blanco (5,02:1). */
    val WarningText = WarningTone.text

    val Error = DangerTone.solid
    val ErrorContainer = DangerTone.tint
    /** Texto de error sobre blanco / [ErrorContainer] (6,54 / 5,72:1). */
    val ErrorText = DangerTone.text
    val OnErrorContainer = DangerTone.onTint

    // Acentos decorativos
    val AccentPurple = Color(0xFF7B1FA2)
    val AccentPurpleContainer = Color(0xFFF3E5F5)
    val AccentTeal = Color(0xFF00897B)
    val AccentTealContainer = Color(0xFFE0F2F1)

    // ---- Mapa ÚNICO de estados (DESIGN-SYSTEM §3.5): cocina y mesa ----
    val StateNuevo = BendeyStateColors(Color(0xFFF9A825), Color(0xFFFFF8E1), Color(0xFF92400E))
    val StatePreparando = BendeyStateColors(Color(0xFF0277BD), Color(0xFFE1F5FE), Color(0xFF01579B))
    val StateListo = BendeyStateColors(Color(0xFF2E7D32), Color(0xFFE8F5E9), Color(0xFF1B5E20))
    val StateEntregado = BendeyStateColors(Color(0xFF9E9E9E), Color(0xFFF5F5F5), Color(0xFF616161))
    val StateAtrasado = BendeyStateColors(Color(0xFFC62828), Color(0xFFFFEBEE), Color(0xFFB71C1C))
    val StateMesaLibre = BendeyStateColors(Color(0xFF2E7D32), Color(0xFFE8F5E9), Color(0xFF1B5E20))
    /** "Ocupada" es NARANJA en todo el producto (incluido el dashboard). */
    val StateMesaOcupada = BendeyStateColors(Color(0xFFF97316), Color(0xFFFFEDD5), Color(0xFF9A3412))
    val StateMesaReservada = BendeyStateColors(Color(0xFFF9A825), Color(0xFFFFF8E1), Color(0xFF92400E))
    val StateMesaEnConsumo = BendeyStateColors(Color(0xFF0277BD), Color(0xFFE1F5FE), Color(0xFF01579B))
    val StateMesaViendoCarta = BendeyStateColors(Color(0xFF6366F1), Color(0xFFEEF2FF), Color(0xFF3730A3))

    // Mesas (fill del mapa único)
    val TableLibre = StateMesaLibre.fill
    val TableOcupada = StateMesaOcupada.fill
    val TableReservada = StateMesaReservada.fill
    val TableEnConsumo = StateMesaEnConsumo.fill
    val TableBrowsing = StateMesaViendoCarta.fill

    // Cocina KDS (fill del mapa único)
    val KitchenPendiente = StateNuevo.fill
    val KitchenPreparando = StatePreparando.fill
    val KitchenListo = StateListo.fill
    val KitchenEntregado = StateEntregado.fill
    val KitchenAtrasado = StateAtrasado.fill

    // KPI Dashboard accents (iconos/relleno, no texto)
    val KpiSales = AccentTeal
    val KpiTicket = AccentPurple
    val KpiTables = Success
    val KpiComandas = Warning

    /**
     * Chips resumen mesas en Dashboard: el valor `...` es el color de TEXTO (onTint del mapa único)
     * y `...Container` el tinte. "Ocupada" pasa de rojo (#DC2626) a naranja.
     */
    val DashboardTableLibre = StateMesaLibre.onTint
    val DashboardTableLibreContainer = StateMesaLibre.tint
    val DashboardTableOcupada = StateMesaOcupada.onTint
    val DashboardTableOcupadaContainer = StateMesaOcupada.tint
    val DashboardTableReservada = StateMesaReservada.onTint
    val DashboardTableReservadaContainer = StateMesaReservada.tint
    val DashboardTableConsumo = StateMesaEnConsumo.onTint
    val DashboardTableConsumoContainer = StateMesaEnConsumo.tint

    /** Paleta gráficos Dashboard (top productos / tipos). */
    val DashboardChartOrange = Color(0xFFF97316)
    val DashboardChartLime = Color(0xFF84CC16)
    val DashboardChartPink = Color(0xFFEC4899)
    val DashboardChartAmber = Color(0xFFD97706)

    /**
     * Tono (tinte + texto accesible) para un color de acento conocido. Se usa en
     * `BendeyStatusChip` para pintar texto = onTint sobre tint sin cambiar los llamadores.
     * Devuelve null si el acento no pertenece a ningún rol conocido.
     */
    fun toneForAccent(accent: Color): BendeyStateColors? = when (accent) {
        SuccessTone.solid -> BendeyStateColors(SuccessTone.solid, SuccessTone.tint, SuccessTone.onTint)
        WarningTone.solid -> BendeyStateColors(WarningTone.solid, WarningTone.tint, WarningTone.onTint)
        DangerTone.solid -> BendeyStateColors(DangerTone.solid, DangerTone.tint, DangerTone.onTint)
        InfoTone.solid -> BendeyStateColors(InfoTone.solid, InfoTone.tint, InfoTone.onTint)
        StateMesaOcupada.fill -> StateMesaOcupada
        StateMesaViendoCarta.fill -> StateMesaViendoCarta
        StateEntregado.fill -> StateEntregado
        StateAtrasado.fill -> StateAtrasado
        Primary -> BendeyStateColors(Primary, Brand50, Color(0xFFA82F31))
        AccentTeal -> BendeyStateColors(AccentTeal, AccentTealContainer, Color(0xFF004D40))
        AccentPurple -> BendeyStateColors(AccentPurple, AccentPurpleContainer, Color(0xFF4A148C))
        OnSurfaceVariant -> BendeyStateColors(OnSurfaceVariant, NeutralTone.tint, NeutralTone.onTint)
        else -> null
    }

    /** Color de texto/icono legible sobre un relleno [fill]: el de mayor contraste entre blanco y [OnSurface]. */
    fun onFill(fill: Color): Color {
        fun lum(c: Color) = c.luminance()
        val lf = lum(fill)
        val white = 1.05f / (lf + 0.05f)
        val dark = (lf + 0.05f) / (lum(OnSurface) + 0.05f)
        return if (white >= dark) Color.White else OnSurface
    }
}
