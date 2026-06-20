package com.bendey.restaurant.core.ui.layout

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier

/** Respeta status bar, notch y navigation bar sin ocultar hora/batería/señal. */
fun Modifier.bendeySafeDrawingPadding(): Modifier = safeDrawingPadding()

fun Modifier.bendeyStatusBarsPadding(): Modifier = statusBarsPadding()

fun Modifier.bendeyNavigationBarsPadding(): Modifier = navigationBarsPadding()
