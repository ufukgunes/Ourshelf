package com.ourshelf.app.ui.lounge

import com.ourshelf.app.resources.Res
import com.ourshelf.app.resources.roulette_title
import com.ourshelf.app.resources.app_name
import com.ourshelf.app.resources.lounge_add
import com.ourshelf.app.resources.lounge_backlog
import com.ourshelf.app.resources.lounge_completed
import com.ourshelf.app.resources.lounge_currently_unwinding
import com.ourshelf.app.resources.lounge_empty_action
import com.ourshelf.app.resources.lounge_empty_body
import com.ourshelf.app.resources.lounge_empty_title
import com.ourshelf.app.resources.lounge_new_shelf
import com.ourshelf.app.resources.lounge_no_shelves
import com.ourshelf.app.resources.lounge_shelf_empty
import com.ourshelf.app.resources.lounge_shelves
import com.ourshelf.app.resources.settings_title
import com.ourshelf.app.resources.shelf_item_count
import com.ourshelf.app.resources.shelf_manage
import com.ourshelf.app.resources.shelf_open
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ourshelf.app.core.model.MediaItem
import com.ourshelf.app.core.model.Shelf
import com.ourshelf.app.ui.components.Poster
import com.ourshelf.app.ui.motion.LocalMotionSettings
import kotlin.math.abs

@Composable
fun LoungeRoute(
    onOpenItem: (itemId: String, sharedKey: String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenShelf: (String) -> Unit,
    onManageShelves: () -> Unit,
    onOpenSettings: () -> Unit,
    onPickForMe: () -> Unit,
    viewModel: LoungeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LoungeScreen(
        state = state,
        onOpenItem = onOpenItem,
        onOpenSearch = onOpenSearch,
        onOpenShelf = onOpenShelf,
        onManageShelves = onManageShelves,
        onOpenSettings = onOpenSettings,
        onCreateShelf = viewModel::createShelf,
        onPickForMe = onPickForMe,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoungeScreen(
    state: LoungeUiState,
    onOpenItem: (itemId: String, sharedKey: String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenShelf: (String) -> Unit,
    onManageShelves: () -> Unit,
    onOpenSettings: () -> Unit,
    onCreateShelf: (String, String) -> Unit,
    onPickForMe: () -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showNewShelf by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    if (state.shelves.size > 1) {
                        IconButton(onClick = onManageShelves) {
                            Icon(Icons.Filled.SwapVert, contentDescription = stringResource(Res.string.shelf_manage))
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(Res.string.settings_title))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (!state.isEmpty) {
                ExtendedFloatingActionButton(
                    onClick = onOpenSearch,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(Res.string.lounge_add)) },
                )
            }
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            state.isEmpty -> EmptyLounge(
                modifier = Modifier.padding(padding),
                onOpenSearch = onOpenSearch,
                onCreateShelf = { showNewShelf = true },
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                if (state.currentlyUnwinding.isNotEmpty()) {
                    item(key = "current") {
                        Section(title = stringResource(Res.string.lounge_currently_unwinding)) {
                            CoverFlowRow(state.currentlyUnwinding, onOpenItem)
                        }
                    }
                }
                if (state.upNext.isNotEmpty()) {
                    item(key = "upnext") {
                        Column {
                            SectionHeader(
                                title = stringResource(Res.string.lounge_backlog),
                                action = {
                                    TextButton(onClick = onPickForMe) {
                                        Icon(Icons.Filled.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(Res.string.roulette_title))
                                    }
                                },
                            )
                            TitledPosterRow(state.upNext, "upnext", posterWidth = 112.dp, onOpenItem)
                        }
                    }
                }
                item(key = "shelves-header") {
                    SectionHeader(
                        title = stringResource(Res.string.lounge_shelves),
                        action = {
                            TextButton(onClick = { showNewShelf = true }) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(Res.string.lounge_new_shelf))
                            }
                        },
                    )
                }
                if (state.shelves.isEmpty()) {
                    item(key = "shelves-empty") {
                        Text(
                            stringResource(Res.string.lounge_no_shelves),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
                items(state.shelves, key = { "shelf-${it.collection.id}" }) { shelf ->
                    ShelfRow(shelf, onOpenShelf = { onOpenShelf(shelf.collection.id) }, onOpenItem = onOpenItem)
                }
                if (state.recentlyCompleted.isNotEmpty()) {
                    item(key = "completed") {
                        Section(title = stringResource(Res.string.lounge_completed)) {
                            TitledPosterRow(state.recentlyCompleted, "done", posterWidth = 96.dp, onOpenItem)
                        }
                    }
                }
            }
        }
    }

    if (showNewShelf) {
        ShelfDialog(
            collection = null,
            onDismiss = { showNewShelf = false },
            onConfirm = { name, emoji ->
                onCreateShelf(name, emoji)
                showNewShelf = false
            },
            onDelete = null,
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        SectionHeader(title)
        content()
    }
}

@Composable
private fun SectionHeader(title: String, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, bottom = 10.dp)
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

/**
 * "Şu An Tükettiklerim": merkeze olan uzaklığa göre hafifçe dönen ve küçülen 3B kartlar (cover-flow).
 * Dönüş, kaydırma sırasında yalnızca çizim aşamasında hesaplanır (yeniden kompozisyon yok).
 */
@Composable
private fun CoverFlowRow(items: List<MediaItem>, onOpenItem: (String, String) -> Unit) {
    val listState = rememberLazyListState()
    val perspective = LocalMotionSettings.current.animationsEnabled
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(items, key = { it.id }) { item ->
            val key = "lounge-current-${item.id}"
            Column(
                modifier = Modifier
                    .width(150.dp)
                    .then(if (perspective) Modifier.coverFlow(listState, item.id) else Modifier)
                    .clickable { onOpenItem(item.id, key) },
            ) {
                Box {
                    Poster(item, Modifier.fillMaxWidth(), cornerRadius = 14.dp, sharedKey = key)
                    if (item.isPinned) PinBadge(Modifier.align(Alignment.TopEnd))
                }
                Spacer(Modifier.height(8.dp))
                Text(item.title, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val length = item.lengthValue
                val progress = item.progressValue
                if (length != null && length > 0 && progress != null) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (progress.toFloat() / length).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private fun Modifier.coverFlow(listState: LazyListState, key: Any): Modifier = graphicsLayer {
    val layout = listState.layoutInfo
    val info = layout.visibleItemsInfo.firstOrNull { it.key == key } ?: return@graphicsLayer
    val viewportCenter = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
    val itemCenter = info.offset + info.size / 2f
    val fraction = ((itemCenter - viewportCenter) / (layout.viewportEndOffset - layout.viewportStartOffset))
        .coerceIn(-1f, 1f)
    rotationY = -fraction * 22f
    val scale = 1f - abs(fraction) * 0.1f
    scaleX = scale
    scaleY = scale
    cameraDistance = 16f * density
}

@Composable
private fun TitledPosterRow(
    items: List<MediaItem>,
    section: String,
    posterWidth: Dp,
    onOpenItem: (String, String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(items, key = { it.id }) { item ->
            val key = "lounge-$section-${item.id}"
            Column(Modifier.width(posterWidth).clickable { onOpenItem(item.id, key) }) {
                Box {
                    Poster(item, Modifier.fillMaxWidth(), sharedKey = key)
                    if (item.isPinned) PinBadge(Modifier.align(Alignment.TopEnd))
                }
                Spacer(Modifier.height(8.dp))
                Text(item.title, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * Kullanıcı rafı: kapaklar fiziksel bir rafın üstünde yan yana durur (başlıklar okuyucu için semantikte).
 */
@Composable
private fun ShelfRow(shelf: Shelf, onOpenShelf: () -> Unit, onOpenItem: (String, String) -> Unit) {
    val openLabel = stringResource(Res.string.shelf_open, shelf.collection.name)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = openLabel, onClick = onOpenShelf)
                .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${shelf.collection.iconEmoji}  ${shelf.collection.name}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                pluralStringResource(Res.plurals.shelf_item_count, shelf.items.size, shelf.items.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (shelf.items.isEmpty()) {
            Text(
                stringResource(Res.string.lounge_shelf_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                items(shelf.items, key = { it.id }) { item ->
                    val key = "lounge-shelf-${shelf.collection.id}-${item.id}"
                    Poster(
                        item,
                        modifier = Modifier
                            .width(92.dp)
                            .semantics { contentDescription = item.title }
                            .clickable { onOpenItem(item.id, key) },
                        cornerRadius = 6.dp,
                        sharedKey = key,
                    )
                }
            }
            ShelfPlank()
        }
    }
}

/** Kapakların üstünde durduğu raf tahtası: üst kenar parlak, alt kenarda yumuşak gölge. */
@Composable
private fun ShelfPlank() {
    val top = MaterialTheme.colorScheme.surfaceContainerHighest
    val bottom = MaterialTheme.colorScheme.surfaceContainerLow
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(Brush.verticalGradient(listOf(top, bottom)), RoundedCornerShape(3.dp)),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.scrim.copy(alpha = 0.25f), MaterialTheme.colorScheme.scrim.copy(alpha = 0f)),
                    ),
                ),
        )
    }
}

@Composable
private fun PinBadge(modifier: Modifier) {
    Icon(
        Icons.Filled.PushPin,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(6.dp).size(16.dp),
    )
}

@Composable
private fun EmptyLounge(modifier: Modifier, onOpenSearch: () -> Unit, onCreateShelf: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🛋️", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(Res.string.lounge_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(Res.string.lounge_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onOpenSearch) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.lounge_empty_action), fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onCreateShelf) {
            Text(stringResource(Res.string.lounge_new_shelf))
        }
    }
}
