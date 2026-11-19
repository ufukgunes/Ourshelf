package com.ourshelf.app.ui.logbook

import com.ourshelf.app.core.model.MediaItem
import com.ourshelf.app.core.model.MediaStatus
import com.ourshelf.app.core.model.MediaType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

enum class LogbookPeriod { MONTH, YEAR, ALL_TIME }

/** Dönemin başlangıcı (dahil). Tüm zamanlar için null. */
fun periodStart(period: LogbookPeriod, now: Instant, zone: TimeZone): Instant? {
    val today = now.toLocalDateTime(zone).date
    return when (period) {
        LogbookPeriod.MONTH -> LocalDate(today.year, today.month, 1).atStartOfDayIn(zone)
        LogbookPeriod.YEAR -> LocalDate(today.year, 1, 1).atStartOfDayIn(zone)
        LogbookPeriod.ALL_TIME -> null
    }
}

data class LogbookStats(
    /** Dönemde bitirilenler, en yeni önce. */
    val completed: List<MediaItem>,
    val countByType: Map<MediaType, Int>,
    val minutesWatched: Int,
    val episodesWatched: Int,
    val pagesRead: Int,
    val hoursPlayed: Int,
    val averageRating: Float?,
) {
    val hoursWatched: Int get() = (minutesWatched + 30) / 60
    val isEmpty: Boolean get() = completed.isEmpty()

    companion object {
        /**
         * Yalnızca dönem içinde **tamamlanan** eserler sayılır. Uzunluklar eserin toplam uzunluğudur
         * (film dakikası, kitap sayfası, oyunun tahmini ana hikâye süresi, dizinin bölüm sayısı).
         */
        fun compute(items: List<MediaItem>, from: Instant?): LogbookStats {
            val completed = items
                .filter { it.status == MediaStatus.COMPLETED }
                .filter { item -> val at = item.completedAt; at != null && (from == null || at >= from) }
                .sortedByDescending { it.completedAt }

            fun sumOf(type: MediaType) = completed.filter { it.mediaType == type }.sumOf { it.lengthValue ?: 0 }
            val ratings = completed.mapNotNull { it.userRating }

            return LogbookStats(
                completed = completed,
                countByType = completed.groupingBy { it.mediaType }.eachCount(),
                minutesWatched = sumOf(MediaType.MOVIE),
                episodesWatched = sumOf(MediaType.TV_SHOW),
                pagesRead = sumOf(MediaType.BOOK),
                hoursPlayed = sumOf(MediaType.GAME),
                averageRating = if (ratings.isEmpty()) null else ratings.average().toFloat(),
            )
        }
    }
}
