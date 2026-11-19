package com.ourshelf.app.data.remote.itunes

import com.ourshelf.app.core.model.MediaMetadata
import com.ourshelf.app.core.model.MediaSearchResult
import com.ourshelf.app.core.model.MediaType
import com.ourshelf.app.data.remote.MediaSearchProvider
import com.ourshelf.app.data.remote.subtitleOf
import com.ourshelf.app.data.remote.suspendRunCatching
import com.ourshelf.app.data.remote.yearOrNull
import kotlinx.serialization.Serializable
import com.ourshelf.app.core.Platform
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

/** iTunes Search API — anahtar gerektirmez; ~20 istek/dakika limiti var (aramada debounce şart). */
class ItunesApi(private val client: HttpClient) {
    /** iTunes yanıtı `text/javascript` döndürür; bu yüzden gövde metin olarak okunup ayrıştırılır. */
    suspend fun search(term: String, media: String, entity: String, country: String, limit: Int = 25): ItunesResponse {
        val text = client.get("search") {
            parameter("term", term); parameter("media", media); parameter("entity", entity)
            parameter("country", country); parameter("limit", limit)
        }.bodyAsText()
        return itunesJson.decodeFromString(ItunesResponse.serializer(), text)
    }

    companion object {
        const val BASE_URL = "https://itunes.apple.com/"
        private val itunesJson = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }
    }
}

@Serializable
data class ItunesResponse(
    val resultCount: Int = 0,
    val results: List<ItunesItem> = emptyList(),
)

@Serializable
data class ItunesItem(
    val collectionId: Long? = null,
    val collectionName: String? = null,
    val artistName: String? = null,
    val artworkUrl100: String? = null,
    val artworkUrl600: String? = null,
    val releaseDate: String? = null,
    val primaryGenreName: String? = null,
    val trackCount: Int? = null,
    val collectionViewUrl: String? = null,
    val genres: List<String> = emptyList(),
)

class ItunesProvider(
    private val api: ItunesApi,
    private val regionCode: () -> String = { Platform.region },
) : MediaSearchProvider {
    override val name = "iTunes"
    override val supportedTypes = setOf(MediaType.PODCAST, MediaType.ALBUM)

    override suspend fun search(query: String, type: MediaType, page: Int) = suspendRunCatching {
        // iTunes API sayfalama desteklemez; sadece ilk sayfa döner.
        if (page > 1) return@suspendRunCatching emptyList()
        val country = regionCode().ifBlank { "US" }
        val response = when (type) {
            MediaType.PODCAST -> api.search(query, media = "podcast", entity = "podcast", country = country)
            MediaType.ALBUM -> api.search(query, media = "music", entity = "album", country = country)
            else -> return@suspendRunCatching emptyList()
        }
        response.results.mapNotNull { it.toResult(type) }
    }
}

internal fun ItunesItem.toResult(type: MediaType): MediaSearchResult? {
    val id = collectionId ?: return null
    val title = collectionName ?: return null
    val year = releaseDate.yearOrNull()
    return MediaSearchResult(
        externalId = "itunes:$id",
        mediaType = type,
        title = title,
        subtitle = subtitleOf(artistName, year),
        posterUrl = highResArtwork(),
        releaseYear = year,
        lengthValue = trackCount?.takeIf { it > 0 },
        genres = (listOfNotNull(primaryGenreName) + genres)
            .filter { it != "Podcasts" && it != "Music" }
            .distinct()
            .take(3),
        metadata = MediaMetadata(
            creators = listOfNotNull(artistName),
            sourceUrl = collectionViewUrl,
        ),
    )
}

/** iTunes küçük görsel URL'sindeki boyutu büyütür: `.../100x100bb.jpg` -> `.../600x600bb.jpg`. */
internal fun ItunesItem.highResArtwork(): String? =
    artworkUrl600 ?: artworkUrl100?.replace(Regex("/\\d+x\\d+bb\\."), "/600x600bb.")
