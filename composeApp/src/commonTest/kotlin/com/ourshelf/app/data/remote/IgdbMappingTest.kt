package com.ourshelf.app.data.remote

import com.ourshelf.app.core.model.MediaType
import com.ourshelf.app.data.remote.igdb.IgdbGame
import com.ourshelf.app.data.remote.igdb.IgdbProvider
import com.ourshelf.app.data.remote.igdb.IgdbTimeToBeat
import com.ourshelf.app.data.remote.igdb.hours
import com.ourshelf.app.data.remote.igdb.toResult
import kotlinx.serialization.json.Json
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.Test

class IgdbMappingTest {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }

    @Test
    fun gameMapsDeveloperCoverYearAndPlatformAbbreviations() {
        val body = """
            [{"id":113112,"name":"Hades","summary":"Defy the god of the dead.",
              "cover":{"id":1,"image_id":"co39vc"},"artworks":[{"id":2,"image_id":"ar5l8"}],
              "first_release_date":1600300800,
              "genres":[{"id":12,"name":"Role-playing (RPG)"},{"id":31,"name":"Adventure"}],
              "platforms":[{"id":6,"name":"PC (Microsoft Windows)","abbreviation":"PC"},{"id":130,"name":"Nintendo Switch"}],
              "involved_companies":[{"id":9,"company":{"id":1,"name":"Private Division"},"developer":false},
                                    {"id":8,"company":{"id":2,"name":"Supergiant Games"},"developer":true}],
              "url":"https://www.igdb.com/games/hades--1"}]
        """.trimIndent()
        val result = json.decodeFromString<List<IgdbGame>>(body).single().toResult()

        assertEquals("igdb:113112", result.externalId)
        assertEquals(MediaType.GAME, result.mediaType)
        assertEquals("Supergiant Games • 2020", result.subtitle)
        assertEquals("https://images.igdb.com/igdb/image/upload/t_cover_big_2x/co39vc.jpg", result.posterUrl)
        assertEquals("https://images.igdb.com/igdb/image/upload/t_1080p/ar5l8.jpg", result.backdropUrl)
        assertEquals(listOf("PC", "Nintendo Switch"), result.metadata.platforms)
        assertEquals(listOf("Role-playing (RPG)", "Adventure"), result.genres)
    }

    @Test
    fun gameWithoutCoverOrCompaniesStillMaps() {
        val result = json.decodeFromString<List<IgdbGame>>("""[{"id":1,"name":"Obscure"}]""").single().toResult()
        assertNull(result.posterUrl)
        assertEquals("Obscure", result.title)
        assertNull(result.subtitle)
    }

    @Test
    fun timeToBeatPrefersNormalPlaythroughAndRoundsToHours() {
        assertEquals(22, IgdbTimeToBeat(1, hastily = 50_000, normally = 79_200).hours())
        assertEquals(14, IgdbTimeToBeat(1, hastily = 50_000).hours())
        assertEquals(1, IgdbTimeToBeat(1, normally = 600).hours())
        assertNull(IgdbTimeToBeat(1).hours())
    }

    @Test
    fun searchQueryEscapesQuotesAndBackslashesAndPaginates() {
        val q = IgdbProvider.searchQuery("  zelda \"tears\" \\ ", page = 3)
        assertEquals(true, q.startsWith("search \"zelda \\\"tears\\\" \\\\\";"))
        assertEquals(true, q.endsWith("limit 20; offset 40;"))
    }
}
