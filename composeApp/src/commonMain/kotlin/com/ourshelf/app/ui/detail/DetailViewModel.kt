package com.ourshelf.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ourshelf.app.core.model.MediaCollection
import com.ourshelf.app.core.model.MediaItem
import com.ourshelf.app.core.model.MediaStatus
import com.ourshelf.app.data.repository.CollectionRepository
import com.ourshelf.app.data.repository.MediaRepository
import com.ourshelf.app.ui.navigation.DetailRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data object NotFound : DetailUiState
    data class Ready(
        val item: MediaItem,
        val collections: List<MediaCollection>,
        val itemCollectionIds: Set<String>,
    ) : DetailUiState
}

@OptIn(FlowPreview::class)
class DetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository,
    private val collectionRepository: CollectionRepository,
    private val appScope: CoroutineScope,
) : ViewModel() {

    val itemId: String = savedStateHandle.toRoute<DetailRoute>().itemId

    private var hasLoaded = false

    val uiState: StateFlow<DetailUiState> = combine(
        mediaRepository.observeItem(itemId),
        collectionRepository.observeCollections(),
        collectionRepository.observeCollectionIdsForItem(itemId),
    ) { item, collections, ids ->
        if (item == null) DetailUiState.NotFound else DetailUiState.Ready(item, collections, ids)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState.Loading)

    /** Not yazılırken her tuşta DB'ye yazmamak için taslak + debounce. */
    private val noteDraft = MutableStateFlow<String?>(null)

    init {
        noteDraft.filterNotNull()
            .debounce(NOTE_DEBOUNCE_MS)
            .onEach { mediaRepository.setNote(itemId, it) }
            .launchIn(viewModelScope)
    }

    fun setStatus(status: MediaStatus) = launch { mediaRepository.setStatus(itemId, status) }

    fun setRating(rating: Float?) = launch { mediaRepository.setRating(itemId, rating) }

    fun setProgress(value: Int) = launch { mediaRepository.setProgress(itemId, value) }

    fun setPinned(pinned: Boolean) = launch { mediaRepository.setPinned(itemId, pinned) }

    /** Finisher Stamp: "Geri al" için tamamlama öncesi durum saklanır. */
    private var beforeCompletion: MediaItem? = null

    fun complete() {
        val current = (uiState.value as? DetailUiState.Ready)?.item ?: return
        if (current.status == MediaStatus.COMPLETED) return
        beforeCompletion = current
        launch { mediaRepository.setStatus(itemId, MediaStatus.COMPLETED) }
    }

    /** Snackbar ekran kapandıktan sonra da tetiklenebilir; bu yüzden uygulama kapsamında çalışır. */
    fun undoCompletion() {
        val previous = beforeCompletion ?: return
        beforeCompletion = null
        appScope.launch { mediaRepository.revertCompletion(itemId, previous) }
    }

    fun onNoteChange(text: String) {
        noteDraft.value = text
    }

    fun toggleCollection(collectionId: String, add: Boolean) = launch {
        if (add) {
            collectionRepository.addItem(collectionId, itemId)
        } else {
            collectionRepository.removeItem(collectionId, itemId)
        }
    }

    /** Silme ekran kapanırken de tamamlanmalı; bu yüzden uygulama kapsamında çalışır. */
    fun delete() {
        appScope.launch { mediaRepository.delete(itemId) }
    }

    override fun onCleared() {
        // Debounce süresi dolmadan ekrandan çıkılırsa son not taslağını kaybetme.
        noteDraft.value?.let { pending -> appScope.launch { mediaRepository.setNote(itemId, pending) } }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        const val NOTE_DEBOUNCE_MS = 600L
    }
}
