package com.bendey.restaurant.core.ui.pos.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.layout.adaptive.AdaptivePos
import com.bendey.restaurant.core.ui.layout.adaptive.BendeyAdaptiveProfile
import com.bendey.restaurant.core.ui.layout.adaptive.CartSummaryAmountStyle
import java.text.NumberFormat

@Composable
fun BendeyPosCartPanel(
    title: String,
    total: Double,
    currency: NumberFormat,
    profile: BendeyAdaptiveProfile,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    headerActions: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
    footerActions: @Composable () -> Unit = {},
    checkoutAction: (@Composable () -> Unit)? = null,
) {
    val padding = AdaptivePos.cartPanelPadding(profile)

    Surface(
        modifier = modifier.fillMaxSize(),
        shape = BendeyShapeTokens.lg,
        color = BendeyColors.Surface,
        shadowElevation = AdaptivePos.cartPanelElevation(profile),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            BendeyPosCartPanelHeader(
                title = title,
                subtitle = subtitle,
                profile = profile,
                headerActions = headerActions,
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = AdaptivePos.cartHeaderDividerPadding(profile)),
                color = BendeyColors.Outline.copy(alpha = 0.25f),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                content()
            }
            BendeyPosCartPanelFooter(
                total = total,
                currency = currency,
                profile = profile,
                footerActions = footerActions,
                checkoutAction = checkoutAction,
            )
        }
    }
}

@Composable
private fun BendeyPosCartPanelHeader(
    title: String,
    subtitle: String?,
    profile: BendeyAdaptiveProfile,
    headerActions: @Composable () -> Unit,
) {
    val iconSize = AdaptivePos.cartHeaderIconSize(profile)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .background(BendeyColors.PrimaryContainer, BendeyShapeTokens.md),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = BendeyColors.Primary,
                modifier = Modifier.size(iconSize * 0.5f),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        headerActions()
    }
}

@Composable
private fun BendeyPosCartPanelFooter(
    total: Double,
    currency: NumberFormat,
    profile: BendeyAdaptiveProfile,
    footerActions: @Composable () -> Unit,
    checkoutAction: (@Composable () -> Unit)?,
) {
    val amountStyle = when (AdaptivePos.cartSummaryAmountStyle(profile)) {
        CartSummaryAmountStyle.HeadlineLarge -> MaterialTheme.typography.headlineLarge
        CartSummaryAmountStyle.HeadlineMedium -> MaterialTheme.typography.headlineMedium
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AdaptivePos.cartFooterTopPadding(profile)),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
    ) {
        HorizontalDivider(color = BendeyColors.Outline.copy(alpha = 0.25f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AdaptivePos.cartSummaryPadding(profile)),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "Total",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = BendeyColors.OnSurfaceVariant,
            )
            Text(
                text = currency.format(total),
                style = amountStyle,
                fontWeight = FontWeight.Bold,
                color = BendeyColors.Primary,
            )
        }
        checkoutAction?.invoke()
        footerActions()
    }
}

@Composable
fun BendeyPosCartPeekBar(
    itemCount: Int,
    total: Double,
    currency: NumberFormat,
    profile: BendeyAdaptiveProfile,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onCheckout: () -> Unit,
    canCheckout: Boolean,
    checkoutLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val targetHeight = AdaptivePos.peekBarHeight(profile)
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = tween(220),
        label = "peek_bar_height",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = animatedHeight)
            .navigationBarsPadding(),
        shape = BendeyShapeTokens.sheet,
        color = BendeyColors.Surface,
        shadowElevation = 4.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AdaptivePos.catalogHorizontalPadding(profile),
                    vertical = BendeySpacing.xs,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onExpandToggle),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
            ) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Contraer carrito" else "Expandir carrito",
                    tint = BendeyColors.OnSurfaceVariant,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (itemCount == 0) "Carrito vacío" else "$itemCount productos",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = currency.format(total),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BendeyColors.Primary,
                    )
                }
            }
            BendeyPrimaryButton(
                text = if (checkoutLoading) "Cobrando…" else "Cobrar",
                onClick = onCheckout,
                enabled = canCheckout && !checkoutLoading,
                fillWidth = false,
                modifier = Modifier
                    .heightIn(min = BendeySpacing.touchTarget)
                    .defaultMinSize(minWidth = 112.dp),
            )
        }
    }
}

@Composable
fun BendeyPosCartExpandedOverlay(
    visible: Boolean,
    profile: BendeyAdaptiveProfile,
    peekHeight: androidx.compose.ui.unit.Dp,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + slideInVertically(tween(280)) { it / 2 },
        exit = fadeOut(tween(180)) + slideOutVertically(tween(240)) { it / 2 },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BendeyColors.OnSurface.copy(alpha = 0.28f))
                .clickable(onClick = onDismiss),
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxSize(if (profile == BendeyAdaptiveProfile.MediumPortrait) 0.68f else 0.72f)
                    .padding(bottom = peekHeight)
                    .clickable(enabled = false) {},
                shape = BendeyShapeTokens.sheet,
                color = BendeyColors.Surface,
                shadowElevation = 8.dp,
            ) {
                content()
            }
        }
    }
}
