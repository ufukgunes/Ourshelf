package com.ourshelf.app.data.remote

import com.ourshelf.app.core.model.MediaType
import com.ourshelf.app.data.remote.itunes.ItunesResponse
import com.ourshelf.app.data.remote.itunes.toResult
import com.ourshelf.app.data.remote.openlibrary.OpenLibrarySearchResponse
import com.ourshelf.app.data.remote.openlibrary.OpenLibraryWork
import com.ourshelf.app.data.remote.openlibrary.descriptionText
import com.ourshelf.app.data.remote.openlibrary.toResult
import com.ourshelf.app.data.remote.tmdb.TmdbMovieDetails
import com.ourshelf.app.data.remote.tmdb.TmdbPage
import com.ourshelf.app.data.remote.tmdb.TmdbTvSummary
import com.ourshelf.app.data.remote.tmdb.toResult
import kotlinx.serialization.json.Json
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.Test

/** Gerçek API yanıtlarından kısaltılmış örneklerle DTO ayrıştırma + domain eşleme testleri (ağ yok). */
class ProviderMappingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Test
    fun tmdbMovieDetailsMapDirectorRuntimeGenresAndRegionalProviders() {
        val body = """
            {"id":550,"title":"Fight Club","overview":"An insomniac...","poster_path":"/p.jpg",
             "backdrop_path":"/b.jpg","release_date":"1999-10-15","runtime":139,
             "genres":[{"id":18,"name":"Drama"}],
             "credits":{"crew":[{"name":"David Fincher","job":"Director"},{"name":"Jim Uhls","job":"Screenplay"}]},
             "watch/providers":{"results":{"TR":{"flatrate":[{"provider_name":"Netflix"}]},
                                            "US":{"flatrate":[{"provider_name":"Hulu"}]}}},
             "unknown_field":true}
        """.trimIndent()
        val result = json.decodeFromString<TmdbMovieDetails>(body).toResult(region = "TR")

        assertEquals("tmdb:movie:550", result.externalId)
        assertEquals("David Fincher • 1999", result.subtitle)
        assertEquals(139, result.lengthValue)
        assertEquals(listOf("Drama"), result.genres)
        assertEquals(listOf("Netflix"), result.metadata.streamingProviders)
        assertEquals("https://image.tmdb.org/t/p/w780/p.jpg", result.posterUrl)
    }

    @Test
    fun tmdbTvSearchHandlesNullPoster() {
        val body = """{"page":1,"results":[{"id":1399,"name":"Game of Thrones","poster_path":null,"first_air_date":"2011-04-17"}]}"""
        val page = json.decodeFromString<TmdbPage<TmdbTvSummary>>(body)
        val result = page.results.single().toResult()

        assertEquals("tmdb:tv:1399", result.externalId)
        assertEquals(MediaType.TV_SHOW, result.mediaType)
        assertEquals(2011, result.releaseYear)
        assertNull(result.posterUrl)
    }

    @Test
    fun openlibraryDocMapsCoverAuthorPagesAndIsbn13() {
        val body = """
            {"numFound":1,"docs":[{"key":"/works/OL893415W","title":"Dune","author_name":["Frank Herbert"],
             "first_publish_year":1965,"cover_i":11481354,"number_of_pages_median":604,
             "isbn":["0441172717","9780441172719"],"subject":["New York Times reviewed","Science fiction","Fiction, science fiction, general","Dune (Imaginary place)","Accessible book"]}]}
        """.trimIndent()
        val result = json.decodeFromString<OpenLibrarySearchResponse>(body).docs.single().toResult()

        assertEquals("ol:OL893415W", result.externalId)
        assertEquals("Frank Herbert • 1965", result.subtitle)
        assertEquals("https://covers.openlibrary.org/b/id/11481354-L.jpg", result.posterUrl)
        assertEquals(604, result.lengthValue)
        assertEquals("9780441172719", result.metadata.isbn)
        // Virgüllü/iki noktalı uzun konular elenir.
        assertEquals(listOf("Science fiction"), result.genres)
    }

    @Test
    fun openlibraryWorkDescriptionCanBeStringOrObject() {
        val plain = json.decodeFromString<OpenLibraryWork>("""{"description":"A desert planet."}""")
        val obj = json.decodeFromString<OpenLibraryWork>("""{"description":{"type":"/type/text","value":"Spice."}}""")
        val none = json.decodeFromString<OpenLibraryWork>("""{}""")

        assertEquals("A desert planet.", plain.descriptionText())
        assertEquals("Spice.", obj.descriptionText())
        assertNull(none.descriptionText())
    }

    @Test
    fun itunesAlbumMapsHighResArtworkAndSkipsItemsWithoutId() {
        val body = """
            {"resultCount":2,"results":[
              {"collectionId":617154241,"collectionName":"Random Access Memories","artistName":"Daft Punk",
               "artworkUrl100":"https://is1-ssl.mzstatic.com/image/thumb/Music/x/100x100bb.jpg",
               "releaseDate":"2013-05-17T07:00:00Z","primaryGenreName":"Electronic","trackCount":13},
              {"collectionName":"No id"}
            ]}
        """.trimIndent()
        val results = json.decodeFromString<ItunesResponse>(body).results.mapNotNull { it.toResult(MediaType.ALBUM) }

        val album = results.single()
        assertEquals("itunes:617154241", album.externalId)
        assertEquals("https://is1-ssl.mzstatic.com/image/thumb/Music/x/600x600bb.jpg", album.posterUrl)
        assertEquals("Daft Punk • 2013", album.subtitle)
        assertEquals(13, album.lengthValue)
        assertEquals(listOf("Electronic"), album.genres)
    }
}
