package com.ourshelf.app.ui.detail

import com.ourshelf.app.resources.Res
import com.ourshelf.app.resources.action_back
import com.ourshelf.app.resources.action_cancel
import com.ourshelf.app.resources.action_delete
import com.ourshelf.app.resources.attribution_tmdb
import com.ourshelf.app.resources.detail_added_on
import com.ourshelf.app.resources.detail_completed_on
import com.ourshelf.app.resources.detail_delete
import com.ourshelf.app.resources.detail_delete_confirm_body
import com.ourshelf.app.resources.detail_delete_confirm_title
import com.ourshelf.app.resources.detail_hold_to_complete
import com.ourshelf.app.resources.detail_no_shelves
import com.ourshelf.app.resources.detail_not_found
import com.ourshelf.app.resources.detail_note
import com.ourshelf.app.resources.detail_note_hint
import com.ourshelf.app.resources.detail_open_source
import com.ourshelf.app.resources.detail_overview
import com.ourshelf.app.resources.detail_pin
import com.ourshelf.app.resources.detail_platforms
import com.ourshelf.app.resources.detail_progress
import com.ourshelf.app.resources.detail_progress_label
import com.ourshelf.app.resources.detail_progress_of
import com.ourshelf.app.resources.detail_rating
import com.ourshelf.app.resources.detail_shelves
import com.ourshelf.app.resources.detail_status
import com.ourshelf.app.resources.detail_streaming
import com.ourshelf.app.resources.detail_unpin
import com.ourshelf.app.resources.finisher_body
import com.ourshelf.app.resources.finisher_done
import com.ourshelf.app.resources.finisher_title
import com.ourshelf.app.resources.seasons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ourshelf.app.core.model.MediaCollection
import com.ourshelf.app.core.model.MediaItem
import com.ourshelf.app.core.model.MediaStatus
import com.ourshelf.app.ui.components.AmbientMeshBackground
import com.ourshelf.app.ui.components.HoldToCompleteButton
import com.ourshelf.app.ui.components.RatingBar
import com.ourshelf.app.ui.components.TactilePoster
import com.ourshelf.app.ui.components.FinStamp
import com.ourshelf.app.ui.components.ambientTertiary
import com.ourshelf.app.ui.components.fallbackColors
import com.ourshelf.app.ui.components.formatLength
import com.ourshelf.app.ui.components.labelRes
import com.ourshelf.app.ui.components.posterAspectRatio
import com.ourshelf.app.ui.components.progressStep
import kotlinx.coroutines.delay
import com.ourshelf.app.core.Platform

@Composable
fun DetailRoute(
    sharedKey: String?,
    onBack: () -> Unit,
    onDeleted: (itemId: String, title: String) -> Unit,
    onCompleted: (undo: () -> Unit) -> Unit,
    viewModel: DetailViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DetailScreen(
        state = state,
        sharedKey = sharedKey ?: "detail-${viewModel.itemId}",
        onBack = onBack,
        actions = DetailActions(
            onStatusChange = viewModel::setStatus,
            onRatingChange = viewModel::setRating,
            onProgressChange = viewModel::setProgress,
            onNoteChange = viewModel::onNoteChange,
            onPinnedChange = viewModel::setPinned,
            onToggleCollection = viewModel::toggleCollection,
            onComplete = viewModel::complete,
            onFinisherClosed = { onCompleted(viewModel::undoCompletion) },
            onDelete = { item ->
                viewModel.delete()
                onDeleted(item.id, item.title)
            },
        ),
    )
}

class DetailActions(
    val onStatusChange: (MediaStatus) -> Unit,
    val onRatingChange: (Float?) -> Unit,
    val onProgressChange: (Int) -> Unit,
    val onNoteChange: (String) -> Unit,
    val onPinnedChange: (Boolean) -> Unit,
    val onToggleCollection: (String, Boolean) -> Unit,
    val onComplete: () -> Unit,
    val onFinisherClosed: () -> Unit,
    val onDelete: (MediaItem) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    state: DetailUiState,
    sharedKey: String,
    onBack: () -> Unit,
    actions: DetailActions,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var stampAnimating by remember { mutableStateOf(false) }
    var showFinisher by rememberSaveable { mutableStateOf(false) }
    val item = (state as? DetailUiState.Ready)?.item

    // Not taslağı ekran düzeyinde tutulur; hem sayfadaki alan hem Finisher kartı aynı metni düzenler.
    var note by rememberSaveable(item?.id) { mutableStateOf(item?.userNote.orEmpty()) }
    val onNote: (String) -> Unit = { text ->
        if (text.length <= NOTE_MAX_LENGTH) {
            note = text
            actions.onNoteChange(text)
        }
    }

    LaunchedEffect(stampAnimating) {
        if (stampAnimating) {
            delay(650) // Mühür otursun, sonra puan kartı açılsın.
            showFinisher = true
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (item != null) {
            val (fallbackTop, fallbackBottom) = fallbackColors(item.title)
            val primary = item.dominantColor?.let(::Color) ?: fallbackTop
            val secondary = item.accentColor?.let(::Color) ?: fallbackBottom
            AmbientMeshBackground(
                primary = primary,
                secondary = secondary,
                tertiary = ambientTertiary(primary, secondary),
                modifier = Modifier.fillMaxSize(),
            )
        }
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                        }
                    },
                    actions = {
                        if (item != null) {
                            IconButton(onClick = { actions.onPinnedChange(!item.isPinned) }) {
                                Icon(
                                    if (item.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = stringResource(
                                        if (item.isPinned) Res.string.detail_unpin else Res.string.detail_pin,
                                    ),
                                )
                            }
                            IconButton(onClick = { confirmDelete = true }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(Res.string.detail_delete))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
        ) { padding ->
            when (state) {
                DetailUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                DetailUiState.NotFound -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(
                        stringResource(Res.string.detail_not_found),
                        modifier = Modifier.padding(32.dp),
                        textAlign = TextAlign.Center,
                    )
                }
                is DetailUiState.Ready -> DetailContent(
                    state = state,
                    sharedKey = sharedKey,
                    stampAnimating = stampAnimating,
                    note = note,
                    onNoteChange = onNote,
                    modifier = Modifier.padding(top = padding.calculateTopPadding()),
                    actions = actions,
                    onHoldComplete = {
                        stampAnimating = true
                        actions.onComplete()
                    },
                )
            }
        }
    }

    if (showFinisher && item != null) {
        val close = {
            showFinisher = false
            stampAnimating = false
            actions.onFinisherClosed()
        }
        ModalBottomSheet(onDismissRequest = close) {
            FinisherSheet(
                rating = item.userRating,
                note = note,
                onRatingChange = actions.onRatingChange,
                onNoteChange = onNote,
                onDone = close,
            )
        }
    }

    if (confirmDelete && item != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(Res.string.detail_delete_confirm_title, item.title)) },
            text = { Text(stringResource(Res.string.detail_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    actions.onDelete(item)
                }) {
                    Text(stringResource(Res.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    state: DetailUiState.Ready,
    sharedKey: String,
    stampAnimating: Boolean,
    note: String,
    onNoteChange: (String) -> Unit,
    modifier: Modifier,
    actions: DetailActions,
    onHoldComplete: () -> Unit,
) {
    val item = state.item

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        TactilePoster(
            item = item,
            sharedKey = sharedKey,
            modifier = Modifier.width(if (item.mediaType.posterAspectRatio == 1f) 220.dp else 200.dp),
        ) {
            if (item.status == MediaStatus.COMPLETED) {
                FinStamp(animate = stampAnimating, modifier = Modifier.align(Alignment.Center))
            }
        }

        Spacer(Modifier.height(24.dp))
        TitleBlock(item)

        if (item.status != MediaStatus.COMPLETED) {
            Spacer(Modifier.height(20.dp))
            HoldToCompleteButton(
                text = stringResource(Res.string.detail_hold_to_complete),
                onComplete = onHoldComplete,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionCard {
            Label(stringResource(Res.string.detail_status))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MediaStatus.entries.forEach { status ->
                    FilterChip(
                        selected = item.status == status,
                        onClick = { actions.onStatusChange(status) },
                        label = { Text(stringResource(status.labelRes)) },
                    )
                }
            }
            if (item.status == MediaStatus.IN_PROGRESS || (item.progressValue ?: 0) > 0) {
                Spacer(Modifier.height(16.dp))
                ProgressSection(item, actions.onProgressChange)
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionCard {
            Label(stringResource(Res.string.detail_rating))
            RatingBar(rating = item.userRating, onRatingChange = actions.onRatingChange)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                label = { Text(stringResource(Res.string.detail_note)) },
                placeholder = { Text(stringResource(Res.string.detail_note_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
            )
        }

        val meta = item.metadata
        if (item.overview != null || meta.streamingProviders.isNotEmpty() || meta.platforms.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            SectionCard {
                item.overview?.let {
                    Label(stringResource(Res.string.detail_overview))
                    Text(it, style = MaterialTheme.typography.bodyLarge)
                }
                if (meta.streamingProviders.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Label(stringResource(Res.string.detail_streaming))
                    Text(meta.streamingProviders.joinToString(" · "), style = MaterialTheme.typography.bodyLarge)
                }
                if (meta.platforms.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Label(stringResource(Res.string.detail_platforms))
                    Text(meta.platforms.joinToString(" · "), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionCard {
            Label(stringResource(Res.string.detail_shelves))
            CollectionChips(state.collections, state.itemCollectionIds, actions.onToggleCollection)
        }

        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth()) {
            val dates = buildList {
                add(stringResource(Res.string.detail_added_on, Platform.formatDate(item.addedAt)))
                item.completedAt?.let { add(stringResource(Res.string.detail_completed_on, Platform.formatDate(it))) }
            }
            Text(
                dates.joinToString("  ·  "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            meta.sourceUrl?.let { url ->
                val uriHandler = LocalUriHandler.current
                TextButton(onClick = { runCatching { uriHandler.openUri(url) } }) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.detail_open_source))
                }
            }
            if (item.externalId.startsWith("tmdb:")) {
                Text(
                    stringResource(Res.string.attribution_tmdb),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TitleBlock(item: MediaItem) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(item.mediaType.labelRes).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(item.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        item.subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        val facts = buildList {
            item.lengthValue?.let { add(formatLength(item.mediaType, it)) }
            item.metadata.seasonCount?.let { add(pluralStringResource(Res.plurals.seasons, it, it)) }
            addAll(item.genres)
        }
        if (facts.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                facts.joinToString(" · "),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun FinisherSheet(
    rating: Float?,
    note: String,
    onRatingChange: (Float?) -> Unit,
    onNoteChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(Res.string.finisher_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(Res.string.finisher_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        RatingBar(rating = rating, onRatingChange = onRatingChange, starSize = 48.dp)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            placeholder = { Text(stringResource(Res.string.detail_note_hint)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 3,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.finisher_done))
        }
    }
}

@Composable
private fun ProgressSection(item: MediaItem, onProgressChange: (Int) -> Unit) {
    val current = item.progressValue ?: 0
    val length = item.lengthValue
    val step = item.mediaType.progressStep
    Label(stringResource(Res.string.detail_progress))
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = { onProgressChange((current - step).coerceAtLeast(0)) }, enabled = current > 0) {
            Icon(Icons.Filled.Remove, contentDescription = null)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (length != null) stringResource(Res.string.detail_progress_of, current, length)
                else formatLength(item.mediaType, current),
                style = MaterialTheme.typography.titleMedium,
            )
            if (length != null && length > 0) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (current.toFloat() / length).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        FilledTonalIconButton(
            onClick = { onProgressChange(current + step) },
            enabled = length == null || current < length,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
        }
    }
    var exact by rememberSaveable(item.id, current) { mutableStateOf(current.toString()) }
    OutlinedTextField(
        value = exact,
        onValueChange = { text ->
            val digits = text.filter(Char::isDigit).take(6)
            exact = digits
            digits.toIntOrNull()?.let(onProgressChange)
        },
        label = { Text(stringResource(Res.string.detail_progress_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.padding(top = 8.dp).width(160.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CollectionChips(
    collections: List<MediaCollection>,
    selectedIds: Set<String>,
    onToggle: (String, Boolean) -> Unit,
) {
    if (collections.isEmpty()) {
        Text(
            stringResource(Res.string.detail_no_shelves),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        collections.forEach { c ->
            val selected = c.id in selectedIds
            FilterChip(
                selected = selected,
                onClick = { onToggle(c.id, !selected) },
                label = { Text("${c.iconEmoji} ${c.name}") },
            )
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

private const val NOTE_MAX_LENGTH = 280
