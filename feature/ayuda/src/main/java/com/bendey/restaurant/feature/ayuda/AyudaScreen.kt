package com.bendey.restaurant.feature.ayuda

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.help.HelpArticle
import com.bendey.restaurant.core.domain.help.HelpBlock
import com.bendey.restaurant.core.domain.help.HelpCalloutKind
import com.bendey.restaurant.core.domain.help.HelpCatalog
import com.bendey.restaurant.core.domain.help.HelpCopy
import com.bendey.restaurant.core.domain.help.HelpListState
import com.bendey.restaurant.core.domain.help.HelpRole
import com.bendey.restaurant.core.domain.help.HelpSpan
import com.bendey.restaurant.core.domain.help.parseHelpMarkdown
import com.bendey.restaurant.core.ui.components.BendeyEmptyState
import com.bendey.restaurant.core.ui.components.BendeyHorizontalScrollRow
import com.bendey.restaurant.core.ui.components.BendeyLazyColumn
import com.bendey.restaurant.core.ui.components.BendeyListRow
import com.bendey.restaurant.core.ui.components.BendeyListRowSubtitle
import com.bendey.restaurant.core.ui.components.BendeyListRowTitle
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.components.BendeySearchField
import com.bendey.restaurant.core.ui.components.BendeySpinner
import com.bendey.restaurant.core.ui.components.BendeySpinnerSize
import com.bendey.restaurant.core.ui.components.BendeyVerticalScrollColumn

/** Centro de ayuda (R10.7): mismo contenido que Tauri, buscable y filtrado por puesto. */
@Composable
fun AyudaScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AyudaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val catalog = state.catalog
    val open = state.openArticleId?.let { catalog?.article(it) }

    // Con un artículo abierto, "atrás" vuelve a la lista de ayuda, no sale de la pantalla.
    BackHandler(enabled = open != null) { viewModel.closeArticle() }

    Column(modifier.fillMaxSize()) {
        BendeyScreenToolbar(
            title = HelpCopy.TITLE,
            subtitle = open?.let { catalog?.category(it.categoryId)?.title },
            onBack = if (open != null) viewModel::closeArticle else onBack,
        )
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BendeySpinner(size = BendeySpinnerSize.Large)
            }
            state.loadFailed || catalog == null -> BendeyEmptyState(
                title = HelpCopy.LOAD_ERROR_TITLE,
                description = HelpCopy.LOAD_ERROR_DESCRIPTION,
                action = { BendeyPrimaryButton(text = "Reintentar", onClick = viewModel::load, fillWidth = false) },
            )
            open != null -> ArticleDetail(open, catalog)
            else -> HelpList(
                state = state,
                catalog = catalog,
                onQuery = viewModel::setQuery,
                onRole = viewModel::setRole,
                onClearSearch = viewModel::clearSearch,
                onShowAll = viewModel::showAllRoles,
                onOpen = viewModel::openArticle,
            )
        }
    }
}

@Composable
private fun HelpList(
    state: AyudaUiState,
    catalog: HelpCatalog,
    onQuery: (String) -> Unit,
    onRole: (HelpRole?) -> Unit,
    onClearSearch: () -> Unit,
    onShowAll: () -> Unit,
    onOpen: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        BendeySearchField(
            value = state.query,
            onValueChange = onQuery,
            placeholder = HelpCopy.SEARCH_PLACEHOLDER,
            modifier = Modifier.padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xs),
        )
        BendeyHorizontalScrollRow(
            contentPadding = PaddingValues(horizontal = BendeySpacing.md),
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
        ) {
            BendeyFilterChip(selected = state.role == null, onClick = { onRole(null) }, text = HelpCopy.ROLE_ALL)
            HelpRole.entries.forEach { r ->
                BendeyFilterChip(selected = state.role == r, onClick = { onRole(if (state.role == r) null else r) }, text = r.label)
            }
        }
        state.role?.let {
            Text(
                HelpCopy.forRole(it),
                style = MaterialTheme.typography.labelMedium,
                color = BendeyColors.OnSurfaceVariant,
                modifier = Modifier.padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xs),
            )
        }
        when (val list = state.list) {
            is HelpListState.Browse -> BendeyLazyColumn(
                state = rememberLazyListState(),
                contentPadding = PaddingValues(BendeySpacing.md),
                verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                modifier = Modifier.fillMaxSize(),
            ) {
                list.groups.forEach { (category, articles) ->
                    item(key = "cat-${category.id}") {
                        Column(Modifier.padding(top = BendeySpacing.sm)) {
                            Text(category.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            if (category.description.isNotBlank()) {
                                Text(
                                    category.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BendeyColors.OnSurfaceVariant,
                                )
                            }
                        }
                    }
                    items(articles, key = { it.id }) { article -> ArticleRow(article, snippet = null, onOpen = onOpen) }
                }
            }
            is HelpListState.Results -> BendeyLazyColumn(
                state = rememberLazyListState(),
                contentPadding = PaddingValues(BendeySpacing.md),
                verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "count") {
                    Text(
                        HelpCopy.resultsCount(list.hits.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                }
                items(list.hits, key = { it.article.id }) { hit -> ArticleRow(hit.article, snippet = hit.snippet, onOpen = onOpen) }
            }
            HelpListState.NoResults -> BendeyEmptyState(
                title = HelpCopy.NO_RESULTS_TITLE,
                description = HelpCopy.NO_RESULTS_DESCRIPTION,
                action = {
                    BendeyPrimaryButton(
                        text = if (state.role != null) HelpCopy.SHOW_ALL else HelpCopy.CLEAR_SEARCH,
                        onClick = { if (state.role != null) onShowAll() else onClearSearch() },
                        fillWidth = false,
                    )
                },
            )
            HelpListState.EmptyRole -> BendeyEmptyState(
                title = HelpCopy.EMPTY_ROLE_TITLE,
                description = HelpCopy.EMPTY_ROLE_DESCRIPTION,
                action = { BendeyPrimaryButton(text = HelpCopy.SHOW_ALL, onClick = onShowAll, fillWidth = false) },
            )
            null -> Unit
        }
    }
}

@Composable
private fun ArticleRow(article: HelpArticle, snippet: String?, onOpen: (String) -> Unit) {
    BendeyListRow(onClick = { onOpen(article.id) }) {
        BendeyListRowTitle(article.title)
        BendeyListRowSubtitle(article.summary)
        if (!snippet.isNullOrBlank()) {
            Text(
                snippet,
                style = MaterialTheme.typography.bodySmall,
                color = BendeyColors.OnSurfaceVariant,
                maxLines = 3,
            )
        }
    }
}

@Composable
private fun ArticleDetail(article: HelpArticle, catalog: HelpCatalog) {
    val blocks = androidx.compose.runtime.remember(article.id) { parseHelpMarkdown(article.markdown) }
    BendeyVerticalScrollColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = BendeySpacing.md),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
    ) {
        Text(article.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(article.summary, style = MaterialTheme.typography.bodyMedium, color = BendeyColors.OnSurfaceVariant)
        Text(
            HelpCopy.FOR_WHO + " " + HelpRole.entries.filter { it in article.roles }.joinToString(", ") { it.label },
            style = MaterialTheme.typography.labelMedium,
            color = BendeyColors.OnSurfaceVariant,
        )
        HorizontalDivider()
        blocks.forEach { HelpBlockView(it) }
        Box(Modifier.padding(bottom = BendeySpacing.lg))
    }
}

@Composable
private fun HelpBlockView(block: HelpBlock) {
    when (block) {
        is HelpBlock.Paragraph -> Text(spansToText(block.spans), style = MaterialTheme.typography.bodyMedium)
        is HelpBlock.Heading -> Text(
            block.text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = BendeySpacing.xs),
        )
        is HelpBlock.Steps -> Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
            block.items.forEachIndexed { i, spans ->
                Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                    Text("${i + 1}.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = BendeyColors.Primary)
                    Text(spansToText(spans), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
            }
        }
        is HelpBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
            block.items.forEach { spans ->
                Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                    Text("•", style = MaterialTheme.typography.bodyMedium, color = BendeyColors.Primary)
                    Text(spansToText(spans), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
            }
        }
        is HelpBlock.Callout -> Callout(block)
        is HelpBlock.Table -> HelpTable(block)
    }
}

@Composable
private fun Callout(block: HelpBlock.Callout) {
    val (container, accent) = when (block.kind) {
        HelpCalloutKind.NOTE -> BendeyColors.InfoContainer to BendeyColors.InfoText
        HelpCalloutKind.WARN -> BendeyColors.WarningContainer to BendeyColors.WarningText
        HelpCalloutKind.TIP -> BendeyColors.SuccessContainer to BendeyColors.SuccessText
        HelpCalloutKind.NOPE -> BendeyColors.ErrorContainer to BendeyColors.ErrorText
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(container, BendeyShapeTokens.xs)
            .padding(BendeySpacing.sm),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
    ) {
        Text(block.kind.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = accent)
        Text(spansToText(block.spans), style = MaterialTheme.typography.bodyMedium, color = BendeyColors.OnSurface)
    }
}

@Composable
private fun HelpTable(table: HelpBlock.Table) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(BendeyColors.SurfaceVariant, BendeyShapeTokens.xs)
            .padding(BendeySpacing.xs),
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = BendeySpacing.xxs), horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
            table.head.forEach {
                Text(it, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
        }
        table.rows.forEach { row ->
            HorizontalDivider(color = BendeyColors.Outline)
            Row(Modifier.fillMaxWidth().padding(vertical = BendeySpacing.xxs), horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                row.forEachIndexed { i, cell ->
                    Text(
                        spansToText(com.bendey.restaurant.core.domain.help.parseHelpSpans(cell)),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun spansToText(spans: List<HelpSpan>): AnnotatedString = buildAnnotatedString {
    spans.forEach { span ->
        when {
            span.bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(span.text) }
            span.code -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x14000000))) { append(span.text) }
            else -> append(span.text)
        }
    }
}
