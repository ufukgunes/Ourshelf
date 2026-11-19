package com.ourshelf.app.ui.lounge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ourshelf.app.core.model.MediaItem
import com.ourshelf.app.core.model.MediaStatus
import com.ourshelf.app.core.model.Shelf
import com.ourshelf.app.data.repository.CollectionRepository
import com.ourshelf.app.data.repository.MediaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LoungeUiState(
    val isLoading: Boolean = true,
    val currentlyUnwinding: List<MediaItem> = emptyList(),
    val upNext: List<MediaItem> = emptyList(),
    val recentlyCompleted: List<MediaItem> = emptyList(),
    val shelves: List<Shelf> = emptyList(),
) {
    val isEmpty: Boolean
        get() = !isLoading && currentlyUnwinding.isEmpty() && upNext.isEmpty() &&
            recentlyCompleted.isEmpty() && shelves.isEmpty()
}

class LoungeViewModel(
    mediaRepository: MediaRepository,
    private val collectionRepository: CollectionRepository,
) : ViewModel() {

    val uiState: StateFlow<LoungeUiState> = combine(
        mediaRepository.observeCurrentlyUnwinding(),
        mediaRepository.observeByStatus(MediaStatus.BACKLOG),
        mediaRepository.observeByStatus(MediaStatus.COMPLETED),
        collectionRepository.observeShelves(),
    ) { current, backlog, completed, shelves ->
        val currentIds = current.mapTo(HashSet()) { it.id }
        LoungeUiState(
            isLoading = false,
            currentlyUnwinding = current,
            // Sabitlenmiş backlog öğeleri zaten üst rafta; tekrar gösterme.
            upNext = backlog.filterNot { it.id in currentIds },
            recentlyCompleted = completed.sortedByDescending { it.completedAt }.take(RECENT_LIMIT),
            shelves = shelves,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoungeUiState())

    fun createShelf(name: String, emoji: String) {
        if (name.isBlank()) return
        viewModelScope.launch { collectionRepository.create(name, emoji) }
    }

    private companion object {
        const val RECENT_LIMIT = 20
    }
}
