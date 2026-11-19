package com.ourshelf.app.ui.shelf

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ourshelf.app.core.model.Shelf
import com.ourshelf.app.data.repository.CollectionRepository
import com.ourshelf.app.ui.navigation.ShelfRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ShelfUiState {
    data object Loading : ShelfUiState
    data object NotFound : ShelfUiState
    data class Ready(val shelf: Shelf) : ShelfUiState
}

class ShelfViewModel(
    savedStateHandle: SavedStateHandle,
    private val collectionRepository: CollectionRepository,
) : ViewModel() {

    val collectionId: String = savedStateHandle.toRoute<ShelfRoute>().collectionId

    val uiState: StateFlow<ShelfUiState> = collectionRepository.observeShelf(collectionId)
        .map { shelf -> if (shelf == null) ShelfUiState.NotFound else ShelfUiState.Ready(shelf) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShelfUiState.Loading)

    fun commitOrder(itemIds: List<String>) {
        viewModelScope.launch { collectionRepository.reorderItems(collectionId, itemIds) }
    }

    fun removeItem(itemId: String) {
        viewModelScope.launch { collectionRepository.removeItem(collectionId, itemId) }
    }

    fun update(name: String, emoji: String) {
        if (name.isBlank()) return
        viewModelScope.launch { collectionRepository.rename(collectionId, name, emoji) }
    }

    fun delete() {
        viewModelScope.launch { collectionRepository.delete(collectionId) }
    }
}

class ManageShelvesViewModel(
    private val collectionRepository: CollectionRepository,
) : ViewModel() {

    val shelves: StateFlow<List<Shelf>?> = collectionRepository.observeShelves()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun commitOrder(collectionIds: List<String>) {
        viewModelScope.launch { collectionRepository.reorderCollections(collectionIds) }
    }

    fun create(name: String, emoji: String) {
        if (name.isBlank()) return
        viewModelScope.launch { collectionRepository.create(name, emoji) }
    }
}
