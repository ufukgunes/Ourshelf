package com.ourshelf.app.ui.search

import com.ourshelf.app.resources.Res
import com.ourshelf.app.resources.scan_open
import com.ourshelf.app.resources.search_add
import com.ourshelf.app.resources.search_added
import com.ourshelf.app.resources.search_clear
import com.ourshelf.app.resources.search_demo_hint
import com.ourshelf.app.resources.search_error
import com.ourshelf.app.resources.search_hint
import com.ourshelf.app.resources.search_idle_body
import com.ourshelf.app.resources.search_idle_title
import com.ourshelf.app.resources.search_in_library
import com.ourshelf.app.resources.search_no_results
import com.ourshelf.app.resources.search_results_for
import com.ourshelf.app.resources.search_retry
import com.ourshelf.app.resources.search_sheet_backlog
import com.ourshelf.app.resources.search_sheet_completed
import com.ourshelf.app.resources.search_sheet_in_progress
import com.ourshelf.app.resources.type_all
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ourshelf.app.core.model.MediaItem
import com.ourshelf.app.core.model.MediaSearchResult
import com.ourshelf.app.core.model.MediaStatus
import com.ourshelf.app.core.model.MediaType
import com.ourshelf.app.ui.components.Poster
import com.ourshelf.app.ui.components.formatLength
import com.ourshelf.app.ui.components.labelRes

@Composable
fun SearchRoute(
    onOpenItem: (itemId: String, sharedKey: String?) -> Unit,
    onItemAdded: (itemId: String, title: String) -> Unit,
    onScanIsbn: () -> Unit = {},
    scannedIsbn: String? = null,
    onScannedIsbnConsumed: () -> Unit = {},
    viewModel: SearchViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(scannedIsbn) {
        if (scannedIsbn != null) {
            viewModel.searchIsbn(scannedIsbn)
            onScannedIsbnConsumed()
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is SearchEvent.Added -> onItemAdded(event.itemId, event.title)
            }
        }
    }
    SearchScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onRetry = viewModel::retry,
        onAdd = viewModel::add,
        onOpenItem = onOpenItem,
        onScanIsbn = onScanIsbn,
        onPendingIsbnHandled = viewModel::clearPendingIsbn,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (SearchFilter) -> Unit,
    onRetry: () -> Unit,
    onAdd: (MediaSearchResult, MediaStatus) -> Unit,
    onOpenItem: (itemId: String, sharedKey: String?) -> Unit,
    onScanIsbn: () -> Unit = {},
    onPendingIsbnHandled: () -> Unit = {},
) {
    var preview by remember { mutableStateOf<MediaSearchResult?>(null) }

    // Barkoddan okunan ISBN'in sonucu gelince ilk kitabın önizlemesini aç.
    LaunchedEffect(state.pendingIsbn, state.remote) {
        val isbn = state.pendingIsbn ?: return@LaunchedEffect
        val loaded = state.remote as? RemoteResults.Loaded ?: return@LaunchedEffect
        if (loaded.query != isbn) return@LaunchedEffect
        loaded.outcomes.firstOrNull { it.type == MediaType.BOOK }?.results?.firstOrNull()?.let { preview = it }
        onPendingIsbnHandled()
    }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        if (state.query.isEmpty()) focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            Column(Modifier.statusBarsPadding().padding(top = 8.dp)) {
                SearchField(
                    query = state.query,
                    onQueryChange = onQueryChange,
                    onSearch = { focusManager.clearFocus() },
                    onScanIsbn = onScanIsbn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .focusRequester(focusRequester),
                )
                FilterRow(state.filter, onFilterChange)
            }
        },
    ) { padding ->
        val remote = state.remote
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (remote is RemoteResults.Loading) {
                item(key = "loading") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }

            val visibleTypes = state.filter?.let(::setOf) ?: MediaType.entries.toSet()
            val mockVisible = state.mockTypes.intersect(visibleTypes)
            if (state.query.isNotBlank() && mockVisible.isNotEmpty()) {
                item(key = "demo") { DemoHint(mockVisible) }
            }

            if (state.localResults.isNotEmpty()) {
                item(key = "local-header") { SectionTitle(stringResource(Res.string.search_in_library)) }
                items(state.localResults, key = { "local-${it.id}" }) { item ->
                    LocalResultRow(item, onClick = { onOpenItem(item.id, "search-${item.id}") })
                }
            }

            when (remote) {
                RemoteResults.Idle -> if (state.query.isBlank()) {
                    item(key = "idle") { IdleState() }
                }
                RemoteResults.Loading -> Unit
                is RemoteResults.Loaded -> remoteSections(
                    remote = remote,
                    state = state,
                    showHeaders = state.filter == null,
                    onRetry = onRetry,
                    onAdd = { onAdd(it, MediaStatus.BACKLOG) },
                    onClick = { result ->
                        val savedId = state.savedIds[result.externalId]
                        if (savedId != null) onOpenItem(savedId, null) else preview = result
                    },
                )
            }
        }
    }

    preview?.let { result ->
        ModalBottomSheet(onDismissRequest = { preview = null }) {
            PreviewSheet(
                result = result,
                onAdd = { status ->
                    onAdd(result, status)
                    preview = null
                },
            )
        }
    }
}

private fun LazyListScope.remoteSections(
    remote: RemoteResults.Loaded,
    state: SearchUiState,
    showHeaders: Boolean,
    onRetry: () -> Unit,
    onAdd: (MediaSearchResult) -> Unit,
    onClick: (MediaSearchResult) -> Unit,
) {
    val outcomes = remote.outcomes
    val allEmpty = outcomes.all { it.results.isEmpty() && it.error == null }
    if (allEmpty && state.localResults.isEmpty()) {
        item(key = "no-results") {
            Text(
                stringResource(Res.string.search_no_results, state.query.trim()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(32.dp),
            )
        }
        return
    }
    outcomes.forEach { outcome ->
        if (outcome.results.isEmpty() && outcome.error == null) return@forEach
        if (showHeaders) {
            item(key = "header-${outcome.type}") {
                SectionTitle(stringResource(Res.string.search_results_for, stringResource(outcome.type.labelRes)))
            }
        }
        if (outcome.error != null) {
            item(key = "error-${outcome.type}") {
                ErrorRow(stringResource(outcome.type.labelRes), onRetry)
            }
        }
        items(outcome.results, key = { "remote-${it.externalId}" }) { result ->
            RemoteResultRow(
                result = result,
                isSaved = result.externalId in state.savedIds,
                onClick = { onClick(result) },
                onAdd = { onAdd(result) },
            )
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onScanIsbn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text(stringResource(Res.string.search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.search_clear))
                }
            } else {
                IconButton(onClick = onScanIsbn) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = stringResource(Res.string.scan_open))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
    )
}

@Composable
private fun FilterRow(selected: SearchFilter, onSelect: (SearchFilter) -> Unit) {
    val options: List<SearchFilter> = listOf(null) + MediaType.entries
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options, key = { it?.name ?: "ALL" }) { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(stringResource(option?.labelRes ?: Res.string.type_all)) },
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
private fun DemoHint(types: Set<MediaType>) {
    val labels = types.sortedBy { it.ordinal }.map { stringResource(it.labelRes) }.joinToString(", ")
    Text(
        stringResource(Res.string.search_demo_hint, labels),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    )
}

@Composable
private fun ErrorRow(source: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.search_error, source),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) { Text(stringResource(Res.string.search_retry)) }
    }
}

@Composable
private fun IdleState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🍿", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(Res.string.search_idle_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(Res.string.search_idle_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ResultRowLayout(
    title: String,
    subtitle: String?,
    meta: String,
    poster: @Composable () -> Unit,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(56.dp)) { poster() }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                meta,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing()
    }
}

@Composable
private fun LocalResultRow(item: MediaItem, onClick: () -> Unit) {
    ResultRowLayout(
        title = item.title,
        subtitle = item.subtitle,
        meta = "${stringResource(item.mediaType.labelRes)} · ${stringResource(item.status.labelRes)}",
        poster = { Poster(item, cornerRadius = 6.dp, sharedKey = "search-${item.id}") },
        onClick = onClick,
        trailing = {},
    )
}

@Composable
private fun RemoteResultRow(
    result: MediaSearchResult,
    isSaved: Boolean,
    onClick: () -> Unit,
    onAdd: () -> Unit,
) {
    val length = result.lengthValue?.let { formatLength(result.mediaType, it) }
    ResultRowLayout(
        title = result.title,
        subtitle = result.subtitle,
        meta = listOfNotNull(stringResource(result.mediaType.labelRes), length).joinToString(" · "),
        poster = {
            Poster(
                title = result.title,
                type = result.mediaType,
                model = result.posterUrl,
                cornerRadius = 6.dp,
                showFallbackTitle = false,
            )
        },
        onClick = onClick,
        trailing = {
            if (isSaved) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = stringResource(Res.string.search_added),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp),
                )
            } else {
                IconButton(onClick = onAdd) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(Res.string.search_add))
                }
            }
        },
    )
}

@Composable
private fun PreviewSheet(result: MediaSearchResult, onAdd: (MediaStatus) -> Unit) {
    Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
        Row {
            Poster(
                title = result.title,
                type = result.mediaType,
                model = result.posterUrl,
                modifier = Modifier.width(96.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(result.title, style = MaterialTheme.typography.titleLarge)
                result.subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                val length = result.lengthValue?.let { formatLength(result.mediaType, it) }
                Text(
                    listOfNotNull(stringResource(result.mediaType.labelRes), length).joinToString(" · "),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (result.genres.isNotEmpty()) {
                    Text(
                        result.genres.joinToString(", "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        result.overview?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 6, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(24.dp))
        FilledTonalButton(onClick = { onAdd(MediaStatus.BACKLOG) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.search_sheet_backlog))
        }
        Spacer(Modifier.height(8.dp))
        // Tam genişlik: Türkçe gibi uzun etiketli dillerde metin kesilmesin.
        OutlinedButton(onClick = { onAdd(MediaStatus.IN_PROGRESS) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.search_sheet_in_progress))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { onAdd(MediaStatus.COMPLETED) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.search_sheet_completed))
        }
    }
}
