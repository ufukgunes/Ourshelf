package com.ourshelf.app.ui.onboarding

import com.ourshelf.app.resources.Res
import com.ourshelf.app.resources.onb_1_body
import com.ourshelf.app.resources.onb_1_title
import com.ourshelf.app.resources.onb_2_body
import com.ourshelf.app.resources.onb_2_title
import com.ourshelf.app.resources.onb_3_body
import com.ourshelf.app.resources.onb_3_title
import com.ourshelf.app.resources.onb_next
import com.ourshelf.app.resources.onb_skip
import com.ourshelf.app.resources.onb_start
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ourshelf.app.core.model.MediaItem
import com.ourshelf.app.core.model.MediaMetadata
import com.ourshelf.app.core.model.MediaStatus
import com.ourshelf.app.core.model.MediaType
import com.ourshelf.app.ui.components.AmbientMeshBackground
import com.ourshelf.app.ui.components.TactilePoster
import com.ourshelf.app.ui.components.FinStamp
import com.ourshelf.app.ui.components.ambientTertiary
import com.ourshelf.app.ui.components.icon
import kotlinx.coroutines.launch
import kotlin.time.Instant

private const val PAGE_COUNT = 3

/** İlk açılış: üç sayfada uygulamanın imza deneyimlerini canlı olarak gösterir. */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pager = rememberPagerState { PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val demo = remember { demoItem() }
    val amber = Color(0xFFE5A93C)
    val plum = Color(0xFF6E5BA8)

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AmbientMeshBackground(plum, amber, ambientTertiary(plum, amber), Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                if (pager.currentPage < PAGE_COUNT - 1) {
                    TextButton(onClick = onFinish) { Text(stringResource(Res.string.onb_skip)) }
                } else {
                    Spacer(Modifier.height(48.dp))
                }
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Görsel kalan alanı doldurur; küçük ekranlarda metinler ekrandan taşmasın diye küçülür.
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val posterWidth = minOf(180.dp, maxHeight * 0.85f * 2f / 3f)
                        when (page) {
                            0 -> TypeIcons(iconSize = minOf(76.dp, maxHeight / 2.8f))
                            1 -> TactilePoster(demo, Modifier.width(posterWidth))
                            else -> TactilePoster(demo.copy(status = MediaStatus.COMPLETED), Modifier.width(posterWidth)) {
                                if (pager.currentPage == 2) {
                                    FinStamp(animate = true, modifier = Modifier.align(Alignment.Center))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    val (title, body) = when (page) {
                        0 -> Res.string.onb_1_title to Res.string.onb_1_body
                        1 -> Res.string.onb_2_title to Res.string.onb_2_body
                        else -> Res.string.onb_3_title to Res.string.onb_3_body
                    }
                    Text(stringResource(title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(PAGE_COUNT) { i ->
                        val active = i == pager.currentPage
                        Box(
                            Modifier
                                .size(if (active) 10.dp else 8.dp)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    CircleShape,
                                ),
                        )
                    }
                }
                val last = pager.currentPage == PAGE_COUNT - 1
                Button(onClick = {
                    if (last) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                }) {
                    Text(stringResource(if (last) Res.string.onb_start else Res.string.onb_next))
                }
            }
        }
    }
}

@Composable
private fun TypeIcons(iconSize: Dp) {
    val types = MediaType.entries
    Column(verticalArrangement = Arrangement.spacedBy(iconSize / 4)) {
        types.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(iconSize / 4)) {
                row.forEach { type ->
                    Box(
                        Modifier.size(iconSize).background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(type.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(iconSize * 0.45f))
                    }
                }
            }
        }
    }
}

private fun demoItem() = MediaItem(
    id = "onboarding-demo",
    externalId = "demo",
    mediaType = MediaType.MOVIE,
    title = "Ourshelf",
    subtitle = null,
    posterUrl = null,
    localPosterFile = null,
    backdropUrl = null,
    dominantColor = null,
    accentColor = null,
    releaseYear = null,
    lengthValue = null,
    genres = emptyList(),
    overview = null,
    metadata = MediaMetadata(),
    status = MediaStatus.BACKLOG,
    progressValue = null,
    userRating = null,
    userNote = null,
    isPinned = false,
    addedAt = Instant.fromEpochMilliseconds(0),
    startedAt = null,
    completedAt = null,
    updatedAt = Instant.fromEpochMilliseconds(0),
)
