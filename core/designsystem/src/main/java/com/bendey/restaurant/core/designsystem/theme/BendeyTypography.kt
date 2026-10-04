package com.bendey.restaurant.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bendey.restaurant.core.designsystem.R

/**
 * DM Sans empaquetada en `res/font` (DEC-15; licencia OFL en `core/designsystem/OFL-DMSans-LICENSE.txt`).
 * Funciona sin red. Los pesos 800/900 (p. ej. la cantidad de la cocina, `FontWeight.Black`) se
 * resuelven explícitamente a Bold: no se empaqueta ExtraBold/Black.
 */
val BendeyFontFamily = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_regular, FontWeight.Light),
    Font(R.font.dm_sans_medium, FontWeight.Medium),
    Font(R.font.dm_sans_semibold, FontWeight.SemiBold),
    Font(R.font.dm_sans_bold, FontWeight.Bold),
    Font(R.font.dm_sans_bold, FontWeight.ExtraBold),
    Font(R.font.dm_sans_bold, FontWeight.Black),
)

/**
 * Escala tipográfica oficial de Bendey — los 15 niveles de Material3, todos definidos acá.
 *
 * Hasta la evolución del Design System (2026) solo 6 de los 15 niveles estaban sobreescritos
 * (displayLarge, headlineMedium, titleLarge, bodyLarge, labelLarge, labelSmall) — los 9
 * restantes (incluidos bodyMedium/titleMedium/labelMedium, usados por toda la app) caían al
 * default de Material3, fuera del control del sistema. Estos 9 quedan interpolados entre los
 * anclajes que ya existían, sin cambiar ninguno de los 6 valores previos.
 *
 * R1 (DESIGN-SYSTEM §4): familia DM Sans; piso absoluto 12 sp ("micro" 11 eliminado):
 * labelSmall 11→12 sp y bodySmall 12→13 sp (lineHeight 16→18).
 */
val BendeyTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = BendeyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = BendeyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = BendeyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = BendeyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = BendeyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = BendeyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = BendeyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = BendeyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = BendeyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = BendeyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BendeyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = BendeyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = BendeyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = BendeyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = BendeyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)
