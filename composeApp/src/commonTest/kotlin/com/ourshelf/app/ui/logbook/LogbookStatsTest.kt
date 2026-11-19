package com.ourshelf.app.ui.logbook

import com.ourshelf.app.core.model.MediaItem
import com.ourshelf.app.core.model.MediaMetadata
import com.ourshelf.app.core.model.MediaStatus
import com.ourshelf.app.core.model.MediaType
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class LogbookStatsTest {

    private val istanbul = TimeZone.of("Europe/Istanbul")
    private val now = Instant.parse("2026-11-19T12:00:00Z")

    private fun item(
        id: String,
        type: MediaType,
        length: Int?,
        completedAt: String?,
        rating: Float? = null,
        status: MediaStatus = MediaStatus.COMPLETED,
    ) = MediaItem(
        id = id, externalId = id, mediaType = type, title = id, subtitle = null, posterUrl = null,
        localPosterFile = null, backdropUrl = null, dominantColor = null, accentColor = null, releaseYear = null,
        lengthValue = length, genres = emptyList(), overview = null, metadata = MediaMetadata(),
        status = status, progressValue = null, userRating = rating, userNote = null, isPinned = false,
        addedAt = Instant.fromEpochMilliseconds(0), startedAt = null,
        completedAt = completedAt?.let(Instant::parse), updatedAt = Instant.fromEpochMilliseconds(0),
    )

    @Test
    fun periodStartsAreLocalMidnights() {
        assertEquals(Instant.parse("2026-10-31T21:00:00Z"), periodStart(LogbookPeriod.MONTH, now, istanbul))
        assertEquals(Instant.parse("2025-12-31T21:00:00Z"), periodStart(LogbookPeriod.YEAR, now, istanbul))
        assertNull(periodStart(LogbookPeriod.ALL_TIME, now, istanbul))
    }

    @Test
    fun statsCountOnlyItemsCompletedInPeriod() {
        val items = listOf(
            item("movieNov", MediaType.MOVIE, 148, "2026-11-05T20:00:00Z", rating = 4.5f),
            item("movieOct", MediaType.MOVIE, 120, "2026-10-10T20:00:00Z", rating = 3f),
            item("bookNov", MediaType.BOOK, 612, "2026-11-18T08:00:00Z", rating = 5f),
            item("gameNov", MediaType.GAME, 22, "2026-11-01T10:00:00Z"),
            item("showNov", MediaType.TV_SHOW, 10, "2026-11-02T10:00:00Z"),
            item("inProgress", MediaType.MOVIE, 100, null, status = MediaStatus.IN_PROGRESS),
        )

        val month = LogbookStats.compute(items, periodStart(LogbookPeriod.MONTH, now, istanbul))

        assertEquals(listOf("bookNov", "movieNov", "showNov", "gameNov"), month.completed.map { it.id })
        assertEquals(148, month.minutesWatched)
        assertEquals(2, month.hoursWatched) // 148 dk ≈ 2 saat
        assertEquals(612, month.pagesRead)
        assertEquals(22, month.hoursPlayed)
        assertEquals(10, month.episodesWatched)
        assertEquals(4.75f, month.averageRating)
        assertEquals(1, month.countByType[MediaType.MOVIE])

        val all = LogbookStats.compute(items, null)
        assertEquals(5, all.completed.size)
        assertEquals(268, all.minutesWatched)
    }

    @Test
    fun emptyPeriodHasNoAverage() {
        val stats = LogbookStats.compute(emptyList(), null)
        assertTrue(stats.isEmpty)
        assertNull(stats.averageRating)
    }
}
