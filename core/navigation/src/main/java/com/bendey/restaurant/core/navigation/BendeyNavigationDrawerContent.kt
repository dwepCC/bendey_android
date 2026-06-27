package com.bendey.restaurant.core.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.components.BendeyVerticalScrollColumn

@Composable
fun BendeyNavigationDrawerContent(
    currentRoute: String?,
    appVersion: String,
    destinations: List<BendeyDrawerDestination> = BendeyDrawerDestination.entries,
    onNavigate: (BendeyDrawerDestination) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(
        modifier = modifier,
        drawerContainerColor = BendeyColors.Surface,
        drawerContentColor = BendeyColors.OnSurface,
    ) {
        BendeyVerticalScrollColumn(
            modifier = Modifier.padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bendey Resto",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BendeyColors.Primary,
                    )
                    if (appVersion.isNotBlank()) {
                        Text(
                            text = "v$appVersion",
                            style = MaterialTheme.typography.labelMedium,
                            color = BendeyColors.OnSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar menú", tint = BendeyColors.OnSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            destinations.forEach { item ->
                val selected = currentRoute == item.route
                NavigationDrawerItem(
                    label = { Text(item.label) },
                    selected = selected,
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    onClick = { onNavigate(item) },
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = BendeyColors.PrimaryContainer.copy(alpha = 0.55f),
                        unselectedContainerColor = BendeyColors.Surface,
                        selectedIconColor = BendeyColors.Primary,
                        selectedTextColor = BendeyColors.Primary,
                    ),
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text(
                text = "Usa la barra inferior para volver a Inicio, POS, Mesas o Comandas.",
                style = MaterialTheme.typography.labelSmall,
                color = BendeyColors.OnSurfaceVariant,
            )
        }
    }
}
