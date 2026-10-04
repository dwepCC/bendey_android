package com.bendey.restaurant.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Tema claro fijo — sin dark mode ni dynamic color.
 *
 * PENDIENTE (no R1): `secondary`/`tertiary` siguen siendo AccentTeal/AccentPurple. Material los usa
 * por defecto en FilterChip seleccionado, indicadores, etc.; cambiarlos tiñe componentes en toda la
 * app, así que se deja para una fase con revisión visual (ver informe R1).
 */
private val BendeyLightColorScheme = lightColorScheme(
    primary = BendeyColors.Primary,
    onPrimary = BendeyColors.OnPrimary,
    primaryContainer = BendeyColors.PrimaryContainer,
    onPrimaryContainer = BendeyColors.OnPrimaryContainer,
    secondary = BendeyColors.AccentTeal,
    onSecondary = BendeyColors.OnPrimary,
    secondaryContainer = BendeyColors.AccentTealContainer,
    onSecondaryContainer = Color(0xFF004D40),
    tertiary = BendeyColors.AccentPurple,
    onTertiary = BendeyColors.OnPrimary,
    tertiaryContainer = BendeyColors.AccentPurpleContainer,
    onTertiaryContainer = Color(0xFF4A148C),
    background = BendeyColors.Background,
    onBackground = BendeyColors.OnSurface,
    surface = BendeyColors.Surface,
    onSurface = BendeyColors.OnSurface,
    surfaceVariant = BendeyColors.SurfaceVariant,
    onSurfaceVariant = BendeyColors.OnSurfaceVariant,
    outline = BendeyColors.Outline,
    error = BendeyColors.Error,
    onError = BendeyColors.DangerTone.onSolid,
    errorContainer = BendeyColors.ErrorContainer,
    onErrorContainer = BendeyColors.OnErrorContainer,
)

@Composable
fun BendeyTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = BendeyLightColorScheme,
        typography = BendeyTypography,
        shapes = BendeyShapes,
        content = content,
    )
}
