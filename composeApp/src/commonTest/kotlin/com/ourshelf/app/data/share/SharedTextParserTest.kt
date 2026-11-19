package com.ourshelf.app.data.share

import com.ourshelf.app.core.model.MediaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SharedTextParserTest {

    @Test
    fun imdbLinksAreRecognised() {
        assertEquals(SharedTarget.Imdb("tt1375666"), SharedTextParser.parse("https://www.imdb.com/title/tt1375666/?ref_=nv_sr_srsg_0"))
        assertEquals(SharedTarget.Imdb("tt0903747"), SharedTextParser.parse("Check this out https://m.imdb.com/tr/title/tt0903747/"))
    }

    @Test
    fun tmdbLinksCarryTypeAndId() {
        assertEquals(SharedTarget.Tmdb(MediaType.MOVIE, 550), SharedTextParser.parse("https://www.themoviedb.org/movie/550-fight-club"))
        assertEquals(SharedTarget.Tmdb(MediaType.TV_SHOW, 1399), SharedTextParser.parse("https://www.themoviedb.org/tv/1399?language=tr"))
    }

    @Test
    fun isbnInTextIsRecognised() {
        assertEquals(SharedTarget.Book("9780441172719"), SharedTextParser.parse("Dune ISBN 978-0441172719 çok güzel"))
    }

    @Test
    fun youtubeStyleTitlesAreCleaned() {
        assertEquals(
            SharedTarget.Text("Dune: Part Two"),
            SharedTextParser.parse("Dune: Part Two | Official Trailer - YouTube https://youtu.be/abc123"),
        )
        assertEquals(SharedTarget.Text("Kış Uykusu"), SharedTextParser.parse("Kış Uykusu - Resmi Fragman\nhttps://youtu.be/x"))
        assertEquals(SharedTarget.Text("Hades II"), SharedTextParser.parse("\"Hades II\""))
    }

    @Test
    fun bareLinksWithoutTitleAreRejected() {
        assertNull(SharedTextParser.parse("https://example.com/some/page"))
        assertNull(SharedTextParser.parse("   "))
    }
}

class IntakeRelevanceTest {
    @Test
    fun unrelatedTitlesAreDropped() {
        val query = "Dune: Part Two"
        kotlin.test.assertTrue(com.ourshelf.app.ui.intake.isRelevant("Art and Soul of Dune", query))
        kotlin.test.assertTrue(com.ourshelf.app.ui.intake.isRelevant("Dune: Part Two (Original Motion Picture Soundtrack)", query))
        kotlin.test.assertFalse(com.ourshelf.app.ui.intake.isRelevant("Faust", query))
        kotlin.test.assertFalse(com.ourshelf.app.ui.intake.isRelevant("Silence of the Cinema", query))
    }
}
