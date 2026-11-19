package com.ourshelf.app.data.repository

import com.ourshelf.app.core.model.MediaSearchResult
import com.ourshelf.app.core.model.MediaType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

data class TypedSearchOutcome(
    val type: MediaType,
    val results: List<MediaSearchResult>,
    val error: Throwable? = null,
)

/** Omni-Search: birden fazla türü paralel arar, her tür için sonucu ayrı döndürür. */
class SearchRepository(
    private val registry: MediaProviderRegistry,
) {
    val mockTypes: Set<MediaType> get() = registry.mockTypes

    suspend fun search(query: String, types: Collection<MediaType>, page: Int = 1): List<TypedSearchOutcome> =
        coroutineScope {
            types.map { type ->
                async {
                    registry.providerFor(type).search(query, type, page).fold(
                        onSuccess = { TypedSearchOutcome(type, it) },
                        onFailure = { TypedSearchOutcome(type, emptyList(), it) },
                    )
                }
            }.awaitAll()
        }
}
