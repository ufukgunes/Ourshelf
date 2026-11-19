package com.ourshelf.app.ui.roulette

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ourshelf.app.core.model.MediaItem
import com.ourshelf.app.resources.Res
import com.ourshelf.app.resources.action_back
import com.ourshelf.app.resources.roulette_count
import com.ourshelf.app.resources.roulette_empty_body
import com.ourshelf.app.resources.roulette_empty_filtered
import com.ourshelf.app.resources.roulette_empty_title
import com.ourshelf.app.resources.roulette_from_backlog
import com.ourshelf.app.resources.roulette_from_shelf
import com.ourshelf.app.resources.roulette_open
import com.ourshelf.app.resources.roulette_result
import com.ourshelf.app.resources.roulette_short
import com.ourshelf.app.resources.roulette_spin
import com.ourshelf.app.resources.roulette_spin_again
import com.ourshelf.app.resources.roulette_start_now
import com.ourshelf.app.resources.roulette_title
import com.ourshelf.app.resources.type_all
import com.ourshelf.app.ui.components.AmbientMeshBackground
import com.ourshelf.app.ui.components.ConfettiBurst
import com.ourshelf.app.ui.components.Poster
import com.ourshelf.app.ui.components.ambientTertiary
import com.ourshelf.app.ui.components.fallbackColors
import com.ourshelf.app.ui.components.formatLength
import com.ourshelf.app.ui.components.labelRes
import com.ourshelf.app.ui.motion.LocalMotionSettings
import com.ourshelf.app.ui.motion.rememberHaptics
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.round

/** Hızlı başlayıp uzun süre yavaşlayan çark eğrisi. */
private val SpinEasing = CubicBezierEasing(0.12f, 0.75f, 0.18f, 1f)
private const val SPIN_MILLIS = 4200

@Composable
fun RouletteRoute(
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    viewModel: RouletteViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RouletteScreen(
        state = state,
        onBack = onBack,
        onTypeChange = viewModel::setType,
        onShortChange = viewModel::setShortOnly,
        pickWinner = viewModel::pickWinner,
        onStartNow = { item ->
            viewModel.startNow(item)
            onOpenItem(item.id)
        },
        onOpenItem = { onOpenItem(it.id) },
    )
}

@Composable
fun RouletteScreen(
    state: RouletteUiState,
    onBack: () -> Unit,
    onTypeChange: (com.ourshelf.app.core.model.MediaType?) -> Unit,
    onShortChange: (Boolean) -> Unit,
    pickWinner: (Int) -> Int,
    onStartNow: (MediaItem) -> Unit,
    onOpenItem: (MediaItem) -> Unit,
) {
    val motion = LocalMotionSettings.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val position = remember { Animatable(0f) }

    // Çevirme sırasında liste filtrelerden etkilenmesin diye dondurulur.
    var spinItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var spinning by remember { mutableStateOf(false) }
    var winner by remember { mutableStateOf<MediaItem?>(null) }
    var confetti by remember { mutableIntStateOf(0) }

    val items = if (spinning || winner != null) spinItems else state.candidates
    LaunchedEffect(state.candidates) {
        if (!spinning) {
            spinItems = state.candidates
            winner = null
        }
    }

    // Her öğe geçişinde mekanik tıkırtı.
    LaunchedEffect(position, items.size) {
        snapshotFlow { floor(position.value).toInt() }
            .distinctUntilChanged()
            .drop(1)
            .collect { if (spinning) haptics.tick() }
    }

    fun spin() {
        if (items.isEmpty() || spinning) return
        val frozen = items
        val winnerIndex = pickWinner(frozen.size)
        spinItems = frozen
        winner = null
        spinning = true
        scope.launch {
            val target = rouletteTarget(position.value, frozen.size, winnerIndex, minLoops = if (frozen.size > 1) 3 else 0)
            if (motion.animationsEnabled && frozen.size > 1) {
                position.animateTo(target, tween(SPIN_MILLIS, easing = SpinEasing))
            } else {
                position.snapTo(target)
            }
            spinning = false
            winner = frozen[winnerIndex]
            haptics.confirm()
            confetti++
        }
    }

    // Ekranda ortaya en yakın poster (yuvarlama); yalnızca poster değişince yeniden hesaplanır, her karede değil.
    val nearestIndex by remember(items) { derivedStateOf { centeredIndex(round(position.value), items.size) } }
    val highlight = winner ?: items.getOrNull(nearestIndex)
    val (fallbackTop, fallbackBottom) = fallbackColors(highlight?.title ?: "Ourshelf")
    val primary = highlight?.dominantColor?.let(::Color) ?: fallbackTop
    val secondary = highlight?.accentColor?.let(::Color) ?: fallbackBottom

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AmbientMeshBackground(primary, secondary, ambientTertiary(primary, secondary), Modifier.fillMaxSize())
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(Res.string.roulette_title))
                            Text(
                                state.shelfName?.let { stringResource(Res.string.roulette_from_shelf, it) }
                                    ?: stringResource(Res.string.roulette_from_backlog),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FilterRow(state, enabled = !spinning, onTypeChange = onTypeChange, onShortChange = onShortChange)

                if (!state.isLoading && items.isEmpty()) {
                    EmptyRoulette(filtered = state.pool.isNotEmpty(), modifier = Modifier.weight(1f))
                    return@Column
                }

                Text(
                    pluralStringResource(Res.plurals.roulette_count, items.size, items.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    RouletteCarousel(items = items, position = { position.value })
                    ConfettiBurst(trigger = confetti, modifier = Modifier.fillMaxSize())
                }

                ResultBlock(
                    winner = winner,
                    centered = highlight,
                    onStartNow = onStartNow,
                    onOpen = onOpenItem,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = ::spin,
                    enabled = !spinning && items.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp),
                ) {
                    Icon(Icons.Filled.Casino, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(if (winner == null) Res.string.roulette_spin else Res.string.roulette_spin_again),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Sonsuz 3B karusel ("cover flow"). Sürekli [position] değerinin tam kısmı ortadaki öğedir; her slotun dönüşü,
 * ölçeği ve saydamlığı yalnızca çizim aşamasında hesaplanır (dönüş sırasında yeniden kompozisyon olmaz).
 */
@Composable
private fun RouletteCarousel(items: List<MediaItem>, position: () -> Float) {
    if (items.isEmpty()) return
    val baseIndex by remember { derivedStateOf { floor(position()).toInt() } }
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val posterWidth = min(170f, maxWidth.value * 0.42f).dp
        val spacingPx = with(LocalDensity.current) { (posterWidth * 0.78f).toPx() }
        for (k in -3..3) {
            val logical = baseIndex + k
            val item = items[centeredIndex(logical.toFloat(), items.size)]
            key(logical) {
                Poster(
                    item = item,
                    cornerRadius = 14.dp,
                    modifier = Modifier
                        .width(posterWidth)
                        .zIndex(-abs(k).toFloat())
                        .graphicsLayer {
                            // Durgun hâlde position tam sayıdır ve `logical == position` olan öğe tam ortadadır.
                            val d = logical - position()
                            translationX = d * spacingPx
                            rotationY = (-d * 38f).coerceIn(-70f, 70f)
                            val s = 1f - min(abs(d), 3f) * 0.13f
                            scaleX = s
                            scaleY = s
                            alpha = (1f - abs(d) / 3.6f).coerceIn(0f, 1f)
                            cameraDistance = 12f * density
                            shadowElevation = if (abs(d) < 0.6f) 18.dp.toPx() else 4.dp.toPx()
                        },
                )
            }
        }
    }
}

@Composable
private fun FilterRow(
    state: RouletteUiState,
    enabled: Boolean,
    onTypeChange: (com.ourshelf.app.core.model.MediaType?) -> Unit,
    onShortChange: (Boolean) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = state.filters.shortOnly,
                onClick = { onShortChange(!state.filters.shortOnly) },
                enabled = enabled,
                label = { Text(stringResource(Res.string.roulette_short)) },
            )
        }
        if (state.availableTypes.size > 1) {
            item {
                FilterChip(
                    selected = state.filters.type == null,
                    onClick = { onTypeChange(null) },
                    enabled = enabled,
                    label = { Text(stringResource(Res.string.type_all)) },
                )
            }
            items(state.availableTypes) { type ->
                FilterChip(
                    selected = state.filters.type == type,
                    onClick = { onTypeChange(type) },
                    enabled = enabled,
                    label = { Text(stringResource(type.labelRes)) },
                )
            }
        }
    }
}

@Composable
private fun ResultBlock(
    winner: MediaItem?,
    centered: MediaItem?,
    onStartNow: (MediaItem) -> Unit,
    onOpen: (MediaItem) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(150.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        AnimatedVisibility(
            visible = winner != null,
            enter = fadeIn() + slideInVertically { it / 3 },
            exit = fadeOut(),
        ) {
            Text(
                stringResource(Res.string.roulette_result),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        val item = winner ?: centered
        if (item != null) {
            Text(
                item.title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = listOfNotNull(
                stringResource(item.mediaType.labelRes),
                item.lengthValue?.let { formatLength(item.mediaType, it) },
            ).joinToString(" · ")
            Text(meta, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(visible = winner != null, enter = fadeIn(), exit = fadeOut()) {
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                winner?.let { w ->
                    OutlinedButton(onClick = { onOpen(w) }) { Text(stringResource(Res.string.roulette_open)) }
                    Button(onClick = { onStartNow(w) }) { Text(stringResource(Res.string.roulette_start_now)) }
                }
            }
        }
    }
}

@Composable
private fun EmptyRoulette(filtered: Boolean, modifier: Modifier) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🎲", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(if (filtered) Res.string.roulette_empty_filtered else Res.string.roulette_empty_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        if (!filtered) {
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(Res.string.roulette_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
