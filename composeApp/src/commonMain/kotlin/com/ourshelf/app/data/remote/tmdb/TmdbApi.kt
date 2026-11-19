package com.ourshelf.app.data.remote.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/** TMDB v3 uç noktaları. Kimlik doğrulama ve taban URL, DI'da yapılandırılan istemcide ayarlanır. */
class TmdbApi(private val client: HttpClient) {
    suspend fun searchMovies(query: String, page: Int, language: String): TmdbPage<TmdbMovieSummary> =
        client.get("search/movie") {
            parameter("query", query); parameter("page", page); parameter("language", language)
            parameter("include_adult", false)
        }.body()

    suspend fun searchTv(query: String, page: Int, language: String): TmdbPage<TmdbTvSummary> =
        client.get("search/tv") {
            parameter("query", query); parameter("page", page); parameter("language", language)
            parameter("include_adult", false)
        }.body()

    suspend fun movieDetails(id: Long, language: String): TmdbMovieDetails =
        client.get("movie/$id") {
            parameter("language", language); parameter("append_to_response", "credits,watch/providers")
        }.body()

    suspend fun tvDetails(id: Long, language: String): TmdbTvDetails =
        client.get("tv/$id") {
            parameter("language", language); parameter("append_to_response", "watch/providers")
        }.body()

    /** IMDb kimliğinden (tt…) TMDB kaydını bulur. */
    suspend fun findByImdbId(imdbId: String, language: String): TmdbFindResponse =
        client.get("find/$imdbId") {
            parameter("external_source", "imdb_id"); parameter("language", language)
        }.body()

    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val IMAGE_BASE = "https://image.tmdb.org/t/p/"
    }
}

@Serializable
data class TmdbPage<T>(
    val page: Int = 1,
    val results: List<T> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 0,
)

@Serializable
data class TmdbMovieSummary(
    val id: Long,
    val title: String = "",
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
)

@Serializable
data class TmdbTvSummary(
    val id: Long,
    val name: String = "",
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
)

@Serializable
data class TmdbFindResponse(
    @SerialName("movie_results") val movieResults: List<TmdbMovieSummary> = emptyList(),
    @SerialName("tv_results") val tvResults: List<TmdbTvSummary> = emptyList(),
)

@Serializable
data class TmdbGenre(val id: Int, val name: String)

@Serializable
data class TmdbCrew(val name: String, val job: String? = null)

@Serializable
data class TmdbCredits(val crew: List<TmdbCrew> = emptyList())

@Serializable
data class TmdbPerson(val name: String)

@Serializable
data class TmdbProviderEntry(@SerialName("provider_name") val providerName: String)

@Serializable
data class TmdbCountryProviders(
    val flatrate: List<TmdbProviderEntry> = emptyList(),
)

@Serializable
data class TmdbWatchProviders(val results: Map<String, TmdbCountryProviders> = emptyMap())

@Serializable
data class TmdbMovieDetails(
    val id: Long,
    val title: String = "",
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val runtime: Int? = null,
    val genres: List<TmdbGenre> = emptyList(),
    val credits: TmdbCredits? = null,
    @SerialName("watch/providers") val watchProviders: TmdbWatchProviders? = null,
)

@Serializable
data class TmdbTvDetails(
    val id: Long,
    val name: String = "",
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int? = null,
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    val genres: List<TmdbGenre> = emptyList(),
    @SerialName("created_by") val createdBy: List<TmdbPerson> = emptyList(),
    @SerialName("watch/providers") val watchProviders: TmdbWatchProviders? = null,
)
