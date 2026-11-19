package com.ourshelf.app.data.remote.igdb

import com.ourshelf.app.core.model.MediaMetadata
import com.ourshelf.app.core.model.MediaSearchResult
import com.ourshelf.app.core.model.MediaType
import com.ourshelf.app.data.remote.MediaSearchProvider
import com.ourshelf.app.data.remote.subtitleOf
import com.ourshelf.app.data.remote.suspendRunCatching
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * IGDB v4 (Apicalypse sorguları). İstekler doğrudan api.igdb.com'a DEĞİL, Twitch Client Secret'ını
 * sunucu tarafında tutan Ourshelf proxy'sine (Cloudflare Worker, bkz. `proxy/igdb-worker.js`) gider.
 */
class IgdbApi(private val client: HttpClient) {
    suspend fun games(query: String): List<IgdbGame> = post("games", query)

    suspend fun timeToBeat(query: String): List<IgdbTimeToBeat> = post("game_time_to_beats", query)

    private suspend inline fun <reified T> post(endpoint: String, query: String): T =
        client.post(endpoint) {
            contentType(ContentType.Text.Plain)
            setBody(query)
        }.body()
}

@Serializable
data class IgdbGame(
    val id: Long,
    val name: String = "",
    val summary: String? = null,
    val cover: IgdbImage? = null,
    val artworks: List<IgdbImage> = emptyList(),
    @SerialName("first_release_date") val firstReleaseDate: Long? = null,
    val genres: List<IgdbNamed> = emptyList(),
    val platforms: List<IgdbPlatform> = emptyList(),
    @SerialName("involved_companies") val involvedCompanies: List<IgdbInvolvedCompany> = emptyList(),
    val url: String? = null,
    /** Kullanıcı + eleştirmen puan sayısı; popülerlik göstergesi olarak sıralamada kullanılır. */
    @SerialName("total_rating_count") val totalRatingCount: Int? = null,
)

@Serializable
data class IgdbImage(@SerialName("image_id") val imageId: String)

@Serializable
data class IgdbNamed(val name: String = "")

@Serializable
data class IgdbPlatform(val name: String? = null, val abbreviation: String? = null)

@Serializable
data class IgdbInvolvedCompany(val company: IgdbNamed? = null, val developer: Boolean = false)

/** Süreler saniye cinsindendir. */
@Serializable
data class IgdbTimeToBeat(
    @SerialName("game_id") val gameId: Long,
    val hastily: Long? = null,
    val normally: Long? = null,
    val completely: Long? = null,
)

class IgdbProvider(private val api: IgdbApi) : MediaSearchProvider {
    override val name = "IGDB"
    override val supportedTypes = setOf(MediaType.GAME)

    override suspend fun search(query: String, type: MediaType, page: Int) = suspendRunCatching {
        if (type != MediaType.GAME) return@suspendRunCatching emptyList()
        rankByPopularity(api.games(searchQuery(query, page)), query).map { it.toResult() }
    }

    /** Tahmini bitirme süresini (normal oynanış) ekler. HowLongToBeat kullanılmaz. */
    override suspend fun details(result: MediaSearchResult) = suspendRunCatching {
        val id = result.externalId.removePrefix(EXTERNAL_PREFIX).toLongOrNull() ?: return@suspendRunCatching result
        val ttb = api.timeToBeat("fields game_id,hastily,normally,completely; where game_id = $id;")
            .firstOrNull()
        result.copy(lengthValue = ttb?.hours() ?: result.lengthValue)
    }

    companion object {
        const val EXTERNAL_PREFIX = "igdb:"
        private const val PAGE_SIZE = 20

        private const val FIELDS = "name,summary,cover.image_id,artworks.image_id,first_release_date," +
            "genres.name,platforms.name,platforms.abbreviation," +
            "involved_companies.company.name,involved_companies.developer,url,total_rating_count"

        /** Kullanıcı girdisi Apicalypse dizesine güvenle gömülür (ters bölü ve tırnak kaçışlanır). */
        fun searchQuery(query: String, page: Int): String {
            val escaped = query.trim().replace("\\", "\\\\").replace("\"", "\\\"")
            val offset = (page - 1).coerceAtLeast(0) * PAGE_SIZE
            return "search \"$escaped\"; fields $FIELDS; where version_parent = null; limit $PAGE_SIZE; offset $offset;"
        }

        fun coverUrl(imageId: String) = "https://images.igdb.com/igdb/image/upload/t_cover_big_2x/$imageId.jpg"
        fun artworkUrl(imageId: String) = "https://images.igdb.com/igdb/image/upload/t_1080p/$imageId.jpg"
    }
}

/**
 * "Bitirme süresi" olarak ana hikâye süresi (IGDB `hastily`: yan görevlere çok vakit ayırmadan jeneriğe kadar)
 * gösterilir; yoksa normal, o da yoksa %100 tamamlama süresi.
 */
internal fun IgdbTimeToBeat.hours(): Int? =
    (hastily ?: normally ?: completely)?.takeIf { it > 0 }?.let { (it / 3600.0).roundToInt().coerceAtLeast(1) }

/**
 * IGDB `search` sorgusu `sort` ile birleştirilemez ve kendi sıralaması zayıftır ("hades" araması 1995'ten az bilinen
 * bir oyunu önce getirir). Sonuçlar popülerlik (puan sayısının logaritması) + başlık eşleşmesi ile yeniden sıralanır.
 */
internal fun rankByPopularity(games: List<IgdbGame>, query: String): List<IgdbGame> {
    val q = query.trim().lowercase()
    return games.sortedByDescending { game ->
        val name = game.name.lowercase()
        val titleBonus = when {
            name == q -> 2.0
            name.startsWith(q) -> 1.0
            else -> 0.0
        }
        ln(1.0 + (game.totalRatingCount ?: 0)) + titleBonus
    }
}

internal fun IgdbGame.toResult(): MediaSearchResult {
    val year = firstReleaseDate?.let { Instant.fromEpochSeconds(it).toLocalDateTime(TimeZone.UTC).year }
    val developers = involvedCompanies.filter { it.developer }.mapNotNull { it.company?.name }
        .ifEmpty { involvedCompanies.mapNotNull { it.company?.name } }
        .distinct()
    return MediaSearchResult(
        externalId = IgdbProvider.EXTERNAL_PREFIX + id,
        mediaType = MediaType.GAME,
        title = name,
        subtitle = subtitleOf(developers.firstOrNull(), year),
        posterUrl = cover?.imageId?.let(IgdbProvider::coverUrl),
        backdropUrl = artworks.firstOrNull()?.imageId?.let(IgdbProvider::artworkUrl),
        releaseYear = year,
        genres = genres.map { it.name }.filter(String::isNotBlank).take(3),
        overview = summary?.takeIf { it.isNotBlank() },
        metadata = MediaMetadata(
            creators = developers,
            platforms = platforms.mapNotNull { it.abbreviation ?: it.name }.distinct(),
            sourceUrl = url,
        ),
    )
}
