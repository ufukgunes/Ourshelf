package com.ourshelf.app.ui.intake

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ourshelf.app.core.model.MediaSearchResult
import com.ourshelf.app.core.model.MediaStatus
import com.ourshelf.app.core.model.MediaType
import com.ourshelf.app.data.remote.openlibrary.normalizedKey
import com.ourshelf.app.data.remote.tmdb.TmdbProvider
import com.ourshelf.app.data.repository.MediaProviderRegistry
import com.ourshelf.app.data.repository.MediaRepository
import com.ourshelf.app.data.repository.SearchRepository
import com.ourshelf.app.data.share.SharedTarget
import com.ourshelf.app.data.share.SharedTextParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface IntakePhase {
    data object Loading : IntakePhase
    data object NotRecognised : IntakePhase
    data class Results(val results: List<MediaSearchResult>) : IntakePhase
}

data class ShareIntakeUiState(
    val sharedText: String,
    val phase: IntakePhase = IntakePhase.Loading,
    /** externalId -> yerel id (zaten kütüphanede olanlar). */
    val savedIds: Map<String, String> = emptyMap(),
)

/**
 * Paylaşım menüsünden gelen metni eserlere çevirir:
 * IMDb → TMDB find, TMDB bağlantısı → detay, ISBN → kitap, serbest metin → tüm türlerde arama.
 */
class ShareIntakeViewModel(
    private val sharedText: String,
    private val registry: MediaProviderRegistry,
    private val searchRepository: SearchRepository,
    private val mediaRepository: MediaRepository,
) : ViewModel() {

    private val phase = MutableStateFlow<IntakePhase>(IntakePhase.Loading)

    val uiState: StateFlow<ShareIntakeUiState> = combine(phase, mediaRepository.observeSavedIds()) { p, saved ->
        ShareIntakeUiState(sharedText, p, saved)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShareIntakeUiState(sharedText))

    init {
        viewModelScope.launch { phase.value = resolve() }
    }

    private suspend fun resolve(): IntakePhase {
        val results = when (val target = SharedTextParser.parse(sharedText)) {
            null -> return IntakePhase.NotRecognised
            is SharedTarget.Imdb ->
                (registry.providerFor(MediaType.MOVIE) as? TmdbProvider)?.findByImdbId(target.id)?.getOrNull().orEmpty()
            is SharedTarget.Tmdb -> {
                val stub = MediaSearchResult(
                    externalId = TmdbProvider.externalId(target.type, target.id),
                    mediaType = target.type,
                    title = "",
                )
                listOfNotNull(registry.providerFor(target.type).details(stub).getOrNull()?.takeIf { it.title.isNotBlank() })
            }
            is SharedTarget.Book -> searchRepository.search(target.isbn, listOf(MediaType.BOOK)).flatMap { it.results }
            is SharedTarget.Text -> topResults(target.query)
        }
        return if (results.isEmpty()) IntakePhase.NotRecognised else IntakePhase.Results(results.take(MAX_RESULTS))
    }

    /**
     * Serbest metinde her türün en iyi iki sonucu, türler sırasıyla. Film/dizi aramasının (TMDB) sıralaması
     * güvenilirdir; diğer türlerde başlığında sorgudaki anlamlı bir kelime geçmeyen sonuçlar elenir.
     */
    private suspend fun topResults(query: String): List<MediaSearchResult> =
        searchRepository.search(query, MediaType.entries).flatMap { outcome ->
            val trusted = outcome.type == MediaType.MOVIE || outcome.type == MediaType.TV_SHOW
            outcome.results.filter { trusted || isRelevant(it.title, query) }.take(2)
        }

    fun add(result: MediaSearchResult) {
        viewModelScope.launch { mediaRepository.addFromSearch(result, MediaStatus.BACKLOG) }
    }

    private companion object {
        const val MAX_RESULTS = 6
    }
}

/** Başlık, sorgudaki en az 4 harfli kelimelerden birini (aksan/büyük harf duyarsız) içeriyor mu? */
internal fun isRelevant(title: String, query: String): Boolean {
    val normalizedTitle = normalizedKey(title)
    return query.split(' ', ':', '-', '|', ',')
        .map(::normalizedKey)
        .filter { it.length >= 4 }
        .any { it in normalizedTitle }
}
