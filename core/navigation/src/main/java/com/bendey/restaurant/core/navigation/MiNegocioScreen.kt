package com.bendey.restaurant.core.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.components.BendeyVerticalScrollColumn

/**
 * «Mi negocio» (R2b): índice de tarjetas agrupadas, una línea de descripción cada una. Reemplaza al drawer
 * plano. Solo recibe las tarjetas que el usuario puede abrir (ver [MiNegocioCard.visibleGrouped]).
 */
@Composable
fun MiNegocioScreen(
    groups: List<Pair<MiNegocioGroup, List<MiNegocioCard>>>,
    onOpen: (MiNegocioCard) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        BendeyScreenToolbar(
            title = "Mi negocio",
            subtitle = "Lo que configuras o revisas de vez en cuando",
            onBack = onBack,
        )
        BendeyVerticalScrollColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = BendeySpacing.md,
                vertical = BendeySpacing.sm,
            ),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
        ) {
            groups.forEach { (group, cards) ->
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = BendeyColors.OnSurfaceVariant,
                    modifier = Modifier.padding(top = BendeySpacing.sm, bottom = BendeySpacing.xxs),
                )
                cards.forEach { card -> MiNegocioCardRow(card = card, onClick = { onOpen(card) }) }
            }
        }
    }
}

@Composable
private fun MiNegocioCardRow(card: MiNegocioCard, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BendeyShapeTokens.md)
            .clickable(onClick = onClick),
        shape = BendeyShapeTokens.md,
        color = BendeyColors.Surface,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
        ) {
            Icon(card.icon, contentDescription = null, tint = BendeyColors.Primary, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(card.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    card.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = BendeyColors.OnSurfaceVariant,
            )
        }
    }
}
