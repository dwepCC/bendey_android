package com.bendey.restaurant.core.ui.components

import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

private val SkeletonBase = Color(0xFFE7E5E4)

/** true si el usuario desactivó las animaciones del sistema ("Reducir animaciones"). */
@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}

/** Bloque gris pulsante: base de los demás skeletons. Estático si se reducen las animaciones. */
@Composable
fun BendeySkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = BendeyShapeTokens.xs,
) {
    val reduceMotion = rememberReduceMotion()
    val alpha = if (reduceMotion) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "skeleton")
        val value by transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 900), RepeatMode.Reverse),
            label = "skeletonAlpha",
        )
        value
    }
    Box(
        modifier = modifier
            .alpha(alpha)
            .background(SkeletonBase, shape),
    )
}

/** Fila de lista en carga: avatar + dos líneas. */
@Composable
fun BendeySkeletonListRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = BendeySpacing.s16, vertical = BendeySpacing.s12),
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BendeySkeletonBlock(modifier = Modifier.size(40.dp), shape = CircleShape)
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
            BendeySkeletonBlock(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp))
            BendeySkeletonBlock(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp))
        }
    }
}

/** Tarjeta en carga: cabecera, dos líneas y pie. */
@Composable
fun BendeySkeletonCard(
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(BendeyColors.Surface, BendeyShapeTokens.xl)
            .border(1.dp, BendeyColors.Outline.copy(alpha = 0.65f), BendeyShapeTokens.xl)
            .padding(BendeySpacing.s16),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.s12),
    ) {
        BendeySkeletonBlock(modifier = Modifier.fillMaxWidth(0.5f).height(16.dp))
        BendeySkeletonBlock(modifier = Modifier.fillMaxWidth().height(12.dp))
        BendeySkeletonBlock(modifier = Modifier.fillMaxWidth(0.75f).height(12.dp))
    }
}

/** Lista de [count] filas en carga. */
@Composable
fun BendeySkeletonList(
    modifier: Modifier = Modifier,
    count: Int = 5,
) {
    Column(modifier = modifier) {
        repeat(count) { BendeySkeletonListRow() }
    }
}
