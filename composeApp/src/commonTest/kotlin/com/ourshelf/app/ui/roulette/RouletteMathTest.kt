package com.ourshelf.app.ui.roulette

import com.ourshelf.app.core.model.MediaItem
import com.ourshelf.app.core.model.MediaMetadata
import com.ourshelf.app.core.model.MediaStatus
import com.ourshelf.app.core.model.MediaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class RouletteMathTest {

    @Test
    fun targetLandsOnWinnerAfterMinimumLoops() {
        for (size in 1..7) {
            for (winner in 0 until size) {
                for (current in listOf(0f, 3f, 11f, -4f, 2.6f)) {
                    val target = rouletteTarget(current, size, winner, minLoops = 3)
                    assertEquals(winner, centeredIndex(target, size), "size=$size winner=$winner current=$current")
                    assertTrue(target >= current + 3 * size, "should spin at least 3 loops")
                    assertTrue(target < current + 4 * size + 1, "should not spin more than needed")
                }
            }
        }
    }

    @Test
    fun centeredIndexWrapsNegativePositions() {
        assertEquals(4, centeredIndex(-1f, 5))
        assertEquals(0, centeredIndex(5.4f, 5))
    }

    private fun item(type: MediaType, length: Int?) = MediaItem(
        id = "x", externalId = "x", mediaType = type, title = "x", subtitle = null, posterUrl = null,
        localPosterFile = null, backdropUrl = null, dominantColor = null, accentColor = null, releaseYear = null,
        lengthValue = length, genres = emptyList(), overview = null, metadata = MediaMetadata(),
        status = MediaStatus.BACKLOG, progressValue = null, userRating = null, userNote = null, isPinned = false,
        addedAt = Instant.fromEpochMilliseconds(0), startedAt = null, completedAt = null,
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    @Test
    fun shortFilterUsesPerTypeThresholds() {
        assertTrue(isShortPick(item(MediaType.MOVIE, 118)))
        assertFalse(isShortPick(item(MediaType.MOVIE, 169)))
        assertTrue(isShortPick(item(MediaType.BOOK, 160)))
        assertFalse(isShortPick(item(MediaType.BOOK, 612)))
        assertTrue(isShortPick(item(MediaType.GAME, 12)))
        assertFalse(isShortPick(item(MediaType.TV_SHOW, 62)))
        assertTrue(isShortPick(item(MediaType.ALBUM, null)))
        assertFalse(isShortPick(item(MediaType.MOVIE, null)))
    }
}
