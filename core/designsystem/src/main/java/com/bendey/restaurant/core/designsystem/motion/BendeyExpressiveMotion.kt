package com.bendey.restaurant.core.designsystem.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.bendey.restaurant.core.designsystem.theme.BendeyMotion

/** Tokens de motion para la Experience Layer (sobre BendeyMotion estándar). */
object BendeyExpressiveMotion {
    const val StaggerStepMs = 45
    const val EntranceDurationMs = 220
}

@Composable
fun BendeyExpressiveFadeSlideIn(
    visible: Boolean,
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = BendeyExpressiveMotion.EntranceDurationMs,
                delayMillis = delayMillis,
            ),
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = BendeyExpressiveMotion.EntranceDurationMs,
                delayMillis = delayMillis,
            ),
            initialOffsetY = { offset -> offset / 12 },
        ),
        exit = fadeOut(animationSpec = BendeyMotion.ExpressiveEffectsTween) +
            slideOutVertically(
                animationSpec = tween(durationMillis = BendeyExpressiveMotion.EntranceDurationMs),
                targetOffsetY = { offset -> offset / 16 },
            ),
        content = content,
    )
}

@Composable
fun BendeyExpressiveReveal(
    index: Int = 0,
    modifier: Modifier = Modifier,
    resetKey: Any? = Unit,
    content: @Composable () -> Unit,
) {
    var visible by remember(resetKey) { mutableStateOf(false) }
    LaunchedEffect(resetKey) {
        visible = true
    }
    BendeyExpressiveFadeSlideIn(
        visible = visible,
        modifier = modifier,
        delayMillis = index * BendeyExpressiveMotion.StaggerStepMs,
    ) {
        content()
    }
}

@Composable
fun <T> BendeyExpressiveCrossfadeValue(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    Crossfade(
        targetState = targetState,
        modifier = modifier,
        animationSpec = BendeyMotion.ExpressiveEffectsTween,
        label = "bendey_expressive_crossfade",
        content = content,
    )
}

@Composable
fun <T> BendeyExpressiveAnimatedContent(
    targetState: T,
    modifier: Modifier = Modifier,
    label: String = "bendey_expressive_content",
    content: @Composable AnimatedContentScope.(targetState: T) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            fadeIn(animationSpec = BendeyMotion.ExpressiveEffectsTween) togetherWith
                fadeOut(animationSpec = BendeyMotion.ExpressiveEffectsTween) using
                SizeTransform(clip = false)
        },
        label = label,
        content = content,
    )
}
