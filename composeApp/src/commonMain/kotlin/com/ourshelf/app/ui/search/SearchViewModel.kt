package com.ourshelf.app.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ourshelf.app.core.model.MediaItem
import com.ourshelf.app.core.model.MediaSearchResult
import com.ourshelf.app.core.model.MediaStatus
import com.ourshelf.app.core.model.MediaType
import com.ourshelf.app.data.repository.MediaRepository
import com.ourshelf.app.data.repository.SearchRepository
import com.ourshelf.app.data.repository.TypedSearchOutcome
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Arama filtresi: null = tüm türler. */
typealias SearchFilter = MediaType?

sealed interface RemoteResults {
    data object Idle : RemoteResults
    data object Loading : RemoteResults
    data class Loaded(val outcomes: List<TypedSearchOutcome>) : RemoteResults
}

data class SearchUiState(
    val query: String = "",
    val filter: SearchFilter = null,
    val localResults: List<MediaItem> = emptyList(),
    val remote: RemoteResults = RemoteResults.Idle,
    /** externalId -> yerel id (zaten kütüphanede olanlar). */
    val savedIds: Map<String, String> = emptyMap(),
    val mockTypes: Set<MediaType> = emptySet(),
)

sealed interface SearchEvent {
    data class Added(val itemId: String, val title: String) : SearchEvent
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val searchRepository: SearchRepository,
    private val mediaRepository: MediaRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val query = MutableStateFlow(savedStateHandle.get<String>(KEY_QUERY).orEmpty())
    private val filter = MutableStateFlow<SearchFilter>(
        savedStateHandle.get<String>(KEY_FILTER)?.let { runCatching { MediaType.valueOf(it) }.getOrNull() },
    )
    private val retryTick = MutableStateFlow(0)

    private val events = Channel<SearchEvent>(Channel.BUFFERED)
    val eventFlow: Flow<SearchEvent> = events.receiveAsFlow()

    private val localResults: Flow<List<MediaItem>> = combine(query.debounce(150), filter) { q, f -> q to f }
        .mapLatest { (q, f) ->
            if (q.isBlank()) emptyList()
            else mediaRepository.searchLocal(q).filter { f == null || it.mediaType == f }
        }

    private val remoteResults: Flow<RemoteResults> =
        combine(query.debounce(DEBOUNCE_MS).distinctUntilChanged(), filter, retryTick) { q, f, _ -> q.trim() to f }
            .flatMapLatest { (q, f) ->
                flow {
                    if (q.length < MIN_QUERY_LENGTH) {
                        emit(RemoteResults.Idle)
                    } else {
                        emit(RemoteResults.Loading)
                        val types = f?.let(::listOf) ?: MediaType.entries
                        emit(RemoteResults.Loaded(searchRepository.search(q, types)))
                    }
                }
            }

    val uiState: StateFlow<SearchUiState> = combine(
        query,
        filter,
        localResults,
        remoteResults,
        mediaRepository.observeSavedIds(),
    ) { q, f, local, remote, saved ->
        SearchUiState(
            query = q,
            filter = f,
            localResults = local,
            remote = remote,
            savedIds = saved,
            mockTypes = searchRepository.mockTypes,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState(query = query.value))

    fun onQueryChange(value: String) {
        query.value = value
        savedStateHandle[KEY_QUERY] = value
    }

    fun onFilterChange(value: SearchFilter) {
        filter.value = value
        savedStateHandle[KEY_FILTER] = value?.name
    }

    fun retry() {
        retryTick.value++
    }

    fun add(result: MediaSearchResult, status: MediaStatus = MediaStatus.BACKLOG) {
        viewModelScope.launch {
            val id = mediaRepository.addFromSearch(result, status)
            events.send(SearchEvent.Added(id, result.title))
        }
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val KEY_FILTER = "filter"
        const val DEBOUNCE_MS = 350L
        const val MIN_QUERY_LENGTH = 2
    }
}
