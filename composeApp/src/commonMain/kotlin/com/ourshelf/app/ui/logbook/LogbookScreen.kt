package com.ourshelf.app.ui.logbook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ourshelf.app.core.Platform
import com.ourshelf.app.core.model.MediaType
import com.ourshelf.app.resources.Res
import com.ourshelf.app.resources.logbook_all
import com.ourshelf.app.resources.logbook_avg_rating
import com.ourshelf.app.resources.logbook_empty_body
import com.ourshelf.app.resources.logbook_empty_title
import com.ourshelf.app.resources.logbook_episodes
import com.ourshelf.app.resources.logbook_finished
import com.ourshelf.app.resources.logbook_hours_played
import com.ourshelf.app.resources.logbook_hours_watched
import com.ourshelf.app.resources.logbook_month
import com.ourshelf.app.resources.logbook_pages_read
import com.ourshelf.app.resources.logbook_share
import com.ourshelf.app.resources.logbook_share_card
import com.ourshelf.app.resources.logbook_title
import com.ourshelf.app.resources.logbook_year
import com.ourshelf.app.resources.share_card_all
import com.ourshelf.app.resources.share_card_month
import com.ourshelf.app.resources.share_card_year
import com.ourshelf.app.ui.components.Poster
import com.ourshelf.app.ui.components.icon
import com.ourshelf.app.ui.share.rememberImageSharer
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@Composable
fun LogbookRoute(
    onOpenItem: (itemId: String, sharedKey: String) -> Unit,
    viewModel: LogbookViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LogbookScreen(state = state, onPeriodChange = viewModel::setPeriod, onOpenItem = onOpenItem)
}

/** İstatistik kutucuğu: büyük sayı + kısa etiket. Sıfır olanlar gösterilmez. */
private data class StatTile(val value: String, val label: String)

@Composable
private fun statTiles(stats: LogbookStats): List<StatTile> = buildList {
    if (stats.minutesWatched > 0) add(StatTile(stats.hoursWatched.toString(), stringResource(Res.string.logbook_hours_watched)))
    if (stats.episodesWatched > 0) add(StatTile(stats.episodesWatched.toString(), stringResource(Res.string.logbook_episodes)))
    if (stats.pagesRead > 0) add(StatTile(stats.pagesRead.toString(), stringResource(Res.string.logbook_pages_read)))
    if (stats.hoursPlayed > 0) add(StatTile(stats.hoursPlayed.toString(), stringResource(Res.string.logbook_hours_played)))
    stats.averageRating?.let {
        add(StatTile("★ " + ((it * 10).roundToInt() / 10f).toString(), stringResource(Res.string.logbook_avg_rating)))
    }
}

@Composable
private fun periodLabel(state: LogbookUiState): String = when (state.period) {
    LogbookPeriod.MONTH -> Platform.formatMonthYear(state.now)
    LogbookPeriod.YEAR -> state.now.toLocalDateTime(TimeZone.currentSystemDefault()).year.toString()
    LogbookPeriod.ALL_TIME -> stringResource(Res.string.logbook_all)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogbookScreen(
    state: LogbookUiState,
    onPeriodChange: (LogbookPeriod) -> Unit,
    onOpenItem: (String, String) -> Unit,
) {
    var showShare by rememberSaveable { mutableStateOf(false) }
    val stats = state.stats
    val tiles = statTiles(stats)
    val label = periodLabel(state)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.logbook_title), style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    if (!stats.isEmpty) {
                        IconButton(onClick = { showShare = true }) {
                            Icon(Icons.Filled.IosShare, contentDescription = stringResource(Res.string.logbook_share))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = padding.calculateTopPadding(), bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "periods") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        LogbookPeriod.MONTH to Res.string.logbook_month,
                        LogbookPeriod.YEAR to Res.string.logbook_year,
                        LogbookPeriod.ALL_TIME to Res.string.logbook_all,
                    ).forEach { (period, res) ->
                        FilterChip(
                            selected = state.period == period,
                            onClick = { onPeriodChange(period) },
                            label = { Text(stringResource(res)) },
                        )
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "hero") {
                Column(Modifier.padding(top = 8.dp)) {
                    Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        pluralStringResource(Res.plurals.logbook_finished, stats.completed.size, stats.completed.size),
                        style = MaterialTheme.typography.displaySmall,
                    )
                    if (stats.countByType.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            MediaType.entries.forEach { type ->
                                val count = stats.countByType[type] ?: return@forEach
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(type.icon, contentDescription = null, modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(4.dp))
                                    Text(count.toString(), style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
            if (tiles.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "tiles") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        tiles.forEach { tile ->
                            Column(
                                Modifier
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                Text(tile.value, style = MaterialTheme.typography.headlineSmall)
                                Text(tile.label, style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            if (stats.isEmpty) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "empty") {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("📖", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(Res.string.logbook_empty_title), style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(Res.string.logbook_empty_body), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            }
            items(stats.completed, key = { it.id }) { item ->
                val key = "logbook-${item.id}"
                Poster(item, Modifier.fillMaxWidth().clickable { onOpenItem(item.id, key) }, cornerRadius = 10.dp, sharedKey = key)
            }
        }
    }

    if (showShare) {
        ShareSheet(
            periodLabel = label,
            subtitle = stringResource(
                when (state.period) {
                    LogbookPeriod.MONTH -> Res.string.share_card_month
                    LogbookPeriod.YEAR -> Res.string.share_card_year
                    LogbookPeriod.ALL_TIME -> Res.string.share_card_all
                },
            ),
            stats = stats,
            statLines = listOf(pluralStringResource(Res.plurals.logbook_finished, stats.completed.size, stats.completed.size)) +
                tiles.map { "${it.value} ${it.label}" },
            onDismiss = { showShare = false },
        )
    }
}

/** Kart önizlemesi + paylaş. Görsel, önizlemenin çizim katmanından üretilir (ekranda görülenle birebir aynı). */
@Composable
private fun ShareSheet(
    periodLabel: String,
    subtitle: String,
    stats: LogbookStats,
    statLines: List<String>,
    onDismiss: () -> Unit,
) {
    val sharer = rememberImageSharer()
    val layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(Res.string.logbook_share_card), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            ShareCard(
                periodLabel = periodLabel,
                subtitle = subtitle,
                items = stats.completed,
                statLines = statLines,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .drawWithContent {
                        layer.record { this@drawWithContent.drawContent() }
                        drawLayer(layer)
                    },
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch {
                        val bitmap = layer.toImageBitmap()
                        // Dosya adı ASCII tutulur; bazı uygulamalar Türkçe karakterli adlarda sorun çıkarır.
                        sharer.share(bitmap, "ourshelf-${periodLabel.lowercase().filter { it in 'a'..'z' || it.isDigit() }}.png")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.IosShare, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.logbook_share))
            }
        }
    }
}
