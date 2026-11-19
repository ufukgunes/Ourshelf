package com.ourshelf.app.ui.roulette

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ourshelf.app.core.model.MediaItem
import com.ourshelf.app.core.model.MediaStatus
import com.ourshelf.app.core.model.MediaType
import com.ourshelf.app.data.repository.CollectionRepository
import com.ourshelf.app.data.repository.MediaRepository
import com.ourshelf.app.ui.navigation.RouletteRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class RouletteFilters(
    val type: MediaType? = null,
    val shortOnly: Boolean = false,
)

data class RouletteUiState(
    val isLoading: Boolean = true,
    /** Raftan açıldıysa rafın adı; tüm "Sıradakiler"den açıldıysa null. */
    val shelfName: String? = null,
    val pool: List<MediaItem> = emptyList(),
    val filters: RouletteFilters = RouletteFilters(),
) {
    val candidates: List<MediaItem>
        get() = pool.filter { item ->
            (filters.type == null || item.mediaType == filters.type) && (!filters.shortOnly || isShortPick(item))
        }

    val availableTypes: List<MediaType>
        get() = pool.map { it.mediaType }.distinct().sortedBy { it.ordinal }
}

class RouletteViewModel(
    savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository,
    collectionRepository: CollectionRepository,
    private val random: Random = Random.Default,
) : ViewModel() {

    private val collectionId: String? = savedStateHandle.toRoute<RouletteRoute>().collectionId
    private val filters = MutableStateFlow(RouletteFilters())

    /** Raf seçildiyse rafın bitmemiş eserleri; değilse tüm "Sıradakiler". */
    private val pool: Flow<Pair<String?, List<MediaItem>>> =
        if (collectionId == null) {
            mediaRepository.observeByStatus(MediaStatus.BACKLOG).map { null to it }
        } else {
            collectionRepository.observeShelf(collectionId).map { shelf ->
                shelf?.collection?.name to shelf?.items.orEmpty().filter {
                    it.status == MediaStatus.BACKLOG || it.status == MediaStatus.IN_PROGRESS
                }
            }
        }

    val uiState: StateFlow<RouletteUiState> = combine(pool, filters) { (name, items), f ->
        RouletteUiState(isLoading = false, shelfName = name, pool = items, filters = f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RouletteUiState())

    fun setType(type: MediaType?) = filters.update { it.copy(type = type) }

    fun setShortOnly(value: Boolean) = filters.update { it.copy(shortOnly = value) }

    /** Kazanan, animasyon başlamadan önce seçilir. */
    fun pickWinner(size: Int): Int = random.nextInt(size)

    fun startNow(item: MediaItem) {
        viewModelScope.launch { mediaRepository.setStatus(item.id, MediaStatus.IN_PROGRESS) }
    }
}
