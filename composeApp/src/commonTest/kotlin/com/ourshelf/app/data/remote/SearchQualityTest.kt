package com.ourshelf.app.data.remote

import com.ourshelf.app.core.model.MediaMetadata
import com.ourshelf.app.core.model.MediaSearchResult
import com.ourshelf.app.core.model.MediaType
import com.ourshelf.app.data.remote.igdb.IgdbGame
import com.ourshelf.app.data.remote.igdb.rankByPopularity
import com.ourshelf.app.data.remote.openlibrary.dedupeWorks
import com.ourshelf.app.data.remote.openlibrary.normalizedKey
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchQualityTest {

    @Test
    fun popularGameOutranksObscureOneWithSameTitle() {
        val obscure = IgdbGame(id = 1, name = "Hades", totalRatingCount = 2)
        val famous = IgdbGame(id = 2, name = "Hades", totalRatingCount = 1500)
        val sequel = IgdbGame(id = 3, name = "Hades II", totalRatingCount = 300)
        val unrelated = IgdbGame(id = 4, name = "Hade", totalRatingCount = null)

        val ranked = rankByPopularity(listOf(obscure, sequel, unrelated, famous), "hades")

        assertEquals(listOf(2L, 3L, 1L, 4L), ranked.map { it.id })
    }

    @Test
    fun exactTitleMatchHelpsLessPopularGame() {
        val exact = IgdbGame(id = 1, name = "Celeste", totalRatingCount = 40)
        val other = IgdbGame(id = 2, name = "Celestial Legends", totalRatingCount = 60)
        assertEquals(1L, rankByPopularity(listOf(other, exact), "celeste").first().id)
    }

    private fun book(id: String, title: String, author: String, poster: String? = null) = MediaSearchResult(
        externalId = id, mediaType = MediaType.BOOK, title = title, posterUrl = poster,
        metadata = MediaMetadata(creators = listOf(author)),
    )

    @Test
    fun duplicateWorksAreMergedPreferringOneWithCover() {
        val results = listOf(
            book("ol:1", "Kürk Mantolu Madonna", "Sabahattin Ali"),
            book("ol:2", "Madonna in a fur coat", "Sabahattin Ali", poster = "p2"),
            book("ol:3", "Kurk Mantolu Madonna", "Sabahattin Ali", poster = "p3"),
            book("ol:4", "kürk mantolu madonna", "Sabahattin Ali"),
        )

        val deduped = dedupeWorks(results)

        assertEquals(listOf("ol:3", "ol:2"), deduped.map { it.externalId })
    }

    @Test
    fun sameTitleByDifferentAuthorsIsKept() {
        val results = listOf(book("a", "Dune", "Frank Herbert"), book("b", "Dune", "Someone Else"))
        assertEquals(2, dedupeWorks(results).size)
    }

    @Test
    fun normalizedKeyIgnoresCasePunctuationAndDiacritics() {
        assertEquals(normalizedKey("Science fiction"), normalizedKey("Science-Fiction"))
        assertEquals(normalizedKey("Kürk"), normalizedKey("KURK"))
    }
}
