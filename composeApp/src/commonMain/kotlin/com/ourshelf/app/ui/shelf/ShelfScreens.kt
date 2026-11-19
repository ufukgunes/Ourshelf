package com.ourshelf.app.ui.shelf

import com.ourshelf.app.resources.Res
import com.ourshelf.app.resources.roulette_title
import com.ourshelf.app.resources.action_back
import com.ourshelf.app.resources.lounge_new_shelf
import com.ourshelf.app.resources.lounge_shelf_empty
import com.ourshelf.app.resources.shelf_dialog_rename_title
import com.ourshelf.app.resources.shelf_drag_handle
import com.ourshelf.app.resources.shelf_item_count
import com.ourshelf.app.resources.shelf_manage
import com.ourshelf.app.resources.shelf_move_down
import com.ourshelf.app.resources.shelf_move_up
import com.ourshelf.app.resources.shelf_not_found
import com.ourshelf.app.resources.shelf_remove_item
import com.ourshelf.app.resources.shelf_reorder_hint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ourshelf.app.core.model.MediaItem
import com.ourshelf.app.core.model.Shelf
import com.ourshelf.app.ui.components.Poster
import com.ourshelf.app.ui.components.ReorderState
import com.ourshelf.app.ui.components.dragHandle
import com.ourshelf.app.ui.components.draggedItem
import com.ourshelf.app.ui.components.labelRes
import com.ourshelf.app.ui.components.rememberReorderState
import com.ourshelf.app.ui.lounge.ShelfDialog

/**
 * Sunucudan (DB) gelen listeyi yerel, sürüklenebilir bir kopyada tutar. Sürükleme sürerken DB güncellemesi
 * gelirse yerel sıra ezilmez; bırakınca [onCommit] ile kalıcı hale getirilir.
 */
@Composable
private fun <T> rememberReorderableList(
    source: List<T>,
    key: (T) -> String,
    onCommit: (List<String>) -> Unit,
): Pair<List<T>, ReorderState> {
    val local = remember { mutableStateListOf<T>().apply { addAll(source) } }
    val reorder = rememberReorderState(
        onMove = { from, to -> local.add(to, local.removeAt(from)) },
        onDragEnd = { onCommit(local.map(key)) },
    )
    LaunchedEffect(source) {
        if (reorder.draggingKey == null) {
            local.clear()
            local.addAll(source)
        }
    }
    return local to reorder
}

/** Erişilebilirlik: sürükleyemeyen kullanıcılar için "yukarı/aşağı taşı" eylemleri. */
@Composable
private fun <T> Modifier.moveActions(
    list: List<T>,
    index: Int,
    key: (T) -> String,
    onCommit: (List<String>) -> Unit,
): Modifier {
    val up = stringResource(Res.string.shelf_move_up)
    val down = stringResource(Res.string.shelf_move_down)
    return semantics {
        customActions = buildList {
            if (index > 0) {
                add(CustomAccessibilityAction(up) {
                    onCommit(list.toMutableList().apply { add(index - 1, removeAt(index)) }.map(key)); true
                })
            }
            if (index < list.lastIndex) {
                add(CustomAccessibilityAction(down) {
                    onCommit(list.toMutableList().apply { add(index + 1, removeAt(index)) }.map(key)); true
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfRoute(
    onBack: () -> Unit,
    onOpenItem: (itemId: String, sharedKey: String) -> Unit,
    onPickForMe: (collectionId: String) -> Unit,
    viewModel: ShelfViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }
    val shelf = (state as? ShelfUiState.Ready)?.shelf

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        shelf?.let { "${it.collection.iconEmoji}  ${it.collection.name}" }.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                    }
                },
                actions = {
                    if (shelf != null && shelf.items.isNotEmpty()) {
                        IconButton(onClick = { onPickForMe(shelf.collection.id) }) {
                            Icon(Icons.Filled.Casino, contentDescription = stringResource(Res.string.roulette_title))
                        }
                    }
                    if (shelf != null) {
                        IconButton(onClick = { editing = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(Res.string.shelf_dialog_rename_title))
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            ShelfUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            ShelfUiState.NotFound -> CenteredMessage(stringResource(Res.string.shelf_not_found), Modifier.padding(padding))
            is ShelfUiState.Ready -> ShelfItems(
                shelf = (state as ShelfUiState.Ready).shelf,
                contentPadding = padding,
                onOpenItem = onOpenItem,
                onCommit = viewModel::commitOrder,
                onRemove = viewModel::removeItem,
            )
        }
    }

    if (editing && shelf != null) {
        ShelfDialog(
            collection = shelf.collection,
            onDismiss = { editing = false },
            onConfirm = { name, emoji ->
                viewModel.update(name, emoji)
                editing = false
            },
            onDelete = {
                editing = false
                viewModel.delete()
                onBack()
            },
        )
    }
}

@Composable
private fun ShelfItems(
    shelf: Shelf,
    contentPadding: PaddingValues,
    onOpenItem: (String, String) -> Unit,
    onCommit: (List<String>) -> Unit,
    onRemove: (String) -> Unit,
) {
    if (shelf.items.isEmpty()) {
        CenteredMessage(stringResource(Res.string.lounge_shelf_empty), Modifier.padding(contentPadding))
        return
    }
    val (items, reorder) = rememberReorderableList(shelf.items, MediaItem::id, onCommit)
    Column(Modifier.padding(contentPadding)) {
        Hint(stringResource(Res.string.shelf_reorder_hint))
        LazyColumn(state = reorder.listState, contentPadding = PaddingValues(bottom = 32.dp)) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                val sharedKey = "shelf-${shelf.collection.id}-${item.id}"
                Row(
                    modifier = Modifier
                        .then(if (reorder.isDragging(item.id)) Modifier else Modifier.animateItem())
                        .draggedItem(reorder, item.id)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .clickable { onOpenItem(item.id, sharedKey) }
                        .moveActions(items, index, MediaItem::id, onCommit)
                        .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DragHandleIcon(Modifier.dragHandle(reorder, item.id))
                    Poster(item, Modifier.width(52.dp), cornerRadius = 6.dp, sharedKey = sharedKey)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOfNotNull(stringResource(item.mediaType.labelRes), item.subtitle).joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { onRemove(item.id) }) {
                        Icon(
                            Icons.Filled.RemoveCircleOutline,
                            contentDescription = stringResource(Res.string.shelf_remove_item),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageShelvesRoute(
    onBack: () -> Unit,
    onOpenShelf: (String) -> Unit,
    viewModel: ManageShelvesViewModel = koinViewModel(),
) {
    val shelves by viewModel.shelves.collectAsStateWithLifecycle()
    var creating by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.shelf_manage)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(Res.string.lounge_new_shelf)) },
            )
        },
    ) { padding ->
        val source = shelves
        if (source == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val (list, reorder) = rememberReorderableList(source, { it.collection.id }, viewModel::commitOrder)
        Column(Modifier.padding(padding)) {
            Hint(stringResource(Res.string.shelf_reorder_hint))
            LazyColumn(state = reorder.listState, contentPadding = PaddingValues(bottom = 96.dp)) {
                itemsIndexed(list, key = { _, s -> s.collection.id }) { index, shelf ->
                    val id = shelf.collection.id
                    Row(
                        modifier = Modifier
                            .then(if (reorder.isDragging(id)) Modifier else Modifier.animateItem())
                            .draggedItem(reorder, id)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .clickable { onOpenShelf(id) }
                            .moveActions(list, index, { it.collection.id }, viewModel::commitOrder)
                            .padding(horizontal = 4.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DragHandleIcon(Modifier.dragHandle(reorder, id))
                        Text(shelf.collection.iconEmoji, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            shelf.collection.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            pluralStringResource(Res.plurals.shelf_item_count, shelf.items.size, shelf.items.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                    }
                }
            }
        }
    }

    if (creating) {
        ShelfDialog(
            collection = null,
            onDismiss = { creating = false },
            onConfirm = { name, emoji ->
                viewModel.create(name, emoji)
                creating = false
            },
            onDelete = null,
        )
    }
}

@Composable
private fun DragHandleIcon(modifier: Modifier) {
    Box(modifier.padding(12.dp)) {
        Icon(
            Icons.Filled.DragHandle,
            contentDescription = stringResource(Res.string.shelf_drag_handle),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun CenteredMessage(text: String, modifier: Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
