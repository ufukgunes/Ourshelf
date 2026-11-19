package com.ourshelf.app.ui.logbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ourshelf.app.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

data class LogbookUiState(
    val isLoading: Boolean = true,
    val period: LogbookPeriod = LogbookPeriod.MONTH,
    val now: Instant = Instant.fromEpochMilliseconds(0),
    val stats: LogbookStats = LogbookStats.compute(emptyList(), null),
)

class LogbookViewModel(
    mediaRepository: MediaRepository,
    private val clock: Clock,
) : ViewModel() {

    private val period = MutableStateFlow(LogbookPeriod.MONTH)

    val uiState: StateFlow<LogbookUiState> = combine(mediaRepository.observeLibrary(), period) { items, p ->
        val now = clock.now()
        LogbookUiState(
            isLoading = false,
            period = p,
            now = now,
            stats = LogbookStats.compute(items, periodStart(p, now, TimeZone.currentSystemDefault())),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LogbookUiState())

    fun setPeriod(value: LogbookPeriod) {
        period.value = value
    }
}
