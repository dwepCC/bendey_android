package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.copy.EmptyCopy
import com.bendey.restaurant.core.domain.copy.EmptyStatesCopy
import com.bendey.restaurant.core.domain.copy.ListViewState
import com.bendey.restaurant.core.domain.copy.LoadErrorCopy

/**
 * Estado vacío: bloque centrado con [icon] opcional (decorativo, 48 dp), título, descripción y acción.
 * Responde las 4 preguntas de UX-REDESIGN §7: qué es, por qué está vacío, qué hacer y botón.
 */
@Composable
fun BendeyEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    action: @Composable (() -> Unit)? = null,
    icon: ImageVector? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(BendeySpacing.s24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.s12),
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = BendeyColors.OnSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = BendeyColors.OnSurface,
            textAlign = TextAlign.Center,
        )
        description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = BendeyColors.OnSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        action?.invoke()
    }
}

/**
 * Estado vacío con los textos centrales de [com.bendey.restaurant.core.domain.copy.EmptyStatesCopy].
 * El botón solo aparece si hay [onAction] y la copia define `action`; con [canAct] = false se usa
 * la variante «pídele al administrador» (si existe) y no se muestra botón.
 */
@Composable
fun BendeyEmptyState(
    copy: EmptyCopy,
    modifier: Modifier = Modifier,
    onAction: (() -> Unit)? = null,
    canAct: Boolean = true,
    icon: ImageVector? = null,
) {
    BendeyEmptyState(
        title = copy.title,
        description = copy.descriptionFor(canAct),
        modifier = modifier,
        icon = icon,
        action = if (canAct && onAction != null && copy.action != null) {
            { BendeyPrimaryButton(text = copy.action!!, onClick = onAction, fillWidth = false) }
        } else {
            null
        },
    )
}

/**
 * Fallo de carga: mensaje + Reintentar. Se usa EN LUGAR de «Sin X» cuando la carga falló
 * (un error de red nunca debe parecer una lista vacía).
 */
@Composable
fun BendeyLoadError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    BendeyEmptyState(
        title = LoadErrorCopy.TITLE,
        description = message?.takeIf { it.isNotBlank() } ?: LoadErrorCopy.DESCRIPTION,
        modifier = modifier,
        action = { BendeyPrimaryButton(text = LoadErrorCopy.RETRY, onClick = onRetry, fillWidth = false) },
    )
}

/**
 * Placeholder de una lista según [ListViewState]: error + Reintentar, vacío «no hay nada creado» o vacío
 * «nada con este filtro». Para Loading/Content no dibuja nada (la pantalla muestra la lista).
 */
@Composable
fun BendeyListPlaceholder(
    viewState: ListViewState,
    emptyCopy: EmptyCopy,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onCreate: (() -> Unit)? = null,
    onClearFilters: (() -> Unit)? = null,
    canAct: Boolean = true,
) {
    when (viewState) {
        is ListViewState.Error -> BendeyLoadError(onRetry = onRetry, message = viewState.message, modifier = modifier)
        ListViewState.EmptyCreated -> BendeyEmptyState(copy = emptyCopy, onAction = onCreate, canAct = canAct, modifier = modifier)
        ListViewState.EmptyFiltered -> BendeyEmptyState(
            copy = EmptyStatesCopy.filtrado,
            onAction = onClearFilters,
            modifier = modifier,
        )
        ListViewState.Loading, ListViewState.Content -> Unit
    }
}
