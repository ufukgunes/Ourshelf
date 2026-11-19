package com.ourshelf.app.data.remote

import com.ourshelf.app.core.model.MediaType
import com.ourshelf.app.data.remote.mock.MockCatalog
import com.ourshelf.app.data.remote.mock.MockMediaProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class MockMediaProviderTest {
    private val provider = MockMediaProvider(latencyMs = 0)

    @Test
    fun searchIsAccentAndCaseInsensitiveAndFiltersByType() = runTest {
        val books = provider.search("kurk mantolu", MediaType.BOOK).getOrThrow()
        assertEquals(listOf("Kürk Mantolu Madonna"), books.map { it.title })

        val movies = provider.search("kurk mantolu", MediaType.MOVIE).getOrThrow()
        assertTrue(movies.isEmpty())
    }

    @Test
    fun searchMatchesSubtitleAndGenre() = runTest {
        val nolan = provider.search("nolan", MediaType.MOVIE).getOrThrow()
        assertEquals(setOf("Inception", "Interstellar"), nolan.map { it.title }.toSet())

        val sciFi = provider.search("bilim kurgu", MediaType.TV_SHOW).getOrThrow()
        assertTrue(sciFi.isNotEmpty())
    }

    @Test
    fun blankQueryAndLaterPagesReturnNothing() = runTest {
        assertTrue(provider.search(" ", MediaType.MOVIE).getOrThrow().isEmpty())
        assertTrue(provider.search("dune", MediaType.MOVIE, page = 2).getOrThrow().isEmpty())
    }

    @Test
    fun catalogExternalIdsAreUniqueAndEveryTypeIsCovered() {
        val ids = MockCatalog.items.map { it.externalId }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(MediaType.entries.toSet(), MockCatalog.items.map { it.mediaType }.toSet())
    }
}
