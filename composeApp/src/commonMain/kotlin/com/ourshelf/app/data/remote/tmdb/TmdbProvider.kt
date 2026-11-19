package com.ourshelf.app.data.remote.tmdb

import com.ourshelf.app.core.model.MediaMetadata
import com.ourshelf.app.core.model.MediaSearchResult
import com.ourshelf.app.core.model.MediaType
import com.ourshelf.app.data.remote.MediaSearchProvider
import com.ourshelf.app.data.remote.subtitleOf
import com.ourshelf.app.data.remote.suspendRunCatching
import com.ourshelf.app.data.remote.yearOrNull
import com.ourshelf.app.core.Platform

/**
 * TMDB (film & dizi). Hem v4 "Read Access Token" (Bearer) hem de v3 API anahtarı desteklenir.
 * Not: TMDB atfı (logo + feragat metni) uygulamada zorunludur.
 */
class TmdbProvider(
    private val api: TmdbApi,
    private val languageTag: () -> String = { Platform.languageTag },
    private val regionCode: () -> String = { Platform.region },
) : MediaSearchProvider {

    override val name = "TMDB"
    override val supportedTypes = setOf(MediaType.MOVIE, MediaType.TV_SHOW)

    private val language get() = languageTag()
    private val region get() = regionCode().ifBlank { "US" }

    override suspend fun search(query: String, type: MediaType, page: Int) = suspendRunCatching {
        when (type) {
            MediaType.MOVIE -> api.searchMovies(query, page, language).results.map { it.toResult() }
            MediaType.TV_SHOW -> api.searchTv(query, page, language).results.map { it.toResult() }
            else -> emptyList()
        }
    }

    override suspend fun details(result: MediaSearchResult) = suspendRunCatching {
        val id = parseId(result.externalId) ?: return@suspendRunCatching result
        when (result.mediaType) {
            MediaType.MOVIE -> api.movieDetails(id, language).toResult(region)
            MediaType.TV_SHOW -> api.tvDetails(id, language).toResult(region)
            else -> result
        }
    }

    suspend fun findByImdbId(imdbId: String): Result<List<MediaSearchResult>> = suspendRunCatching {
        val found = api.findByImdbId(imdbId, language)
        found.movieResults.map { it.toResult() } + found.tvResults.map { it.toResult() }
    }

    companion object {
        fun externalId(type: MediaType, id: Long) =
            "tmdb:${if (type == MediaType.MOVIE) "movie" else "tv"}:$id"

        fun parseId(externalId: String): Long? = externalId.substringAfterLast(':').toLongOrNull()

        fun poster(path: String?) = path?.let { "${TmdbApi.IMAGE_BASE}w780$it" }
        fun backdrop(path: String?) = path?.let { "${TmdbApi.IMAGE_BASE}w1280$it" }
    }
}

/** v4 token'lar uzun JWT'lerdir; v3 anahtarları 32 karakterlik hex'tir. */
fun isTmdbBearerToken(key: String): Boolean = key.length > 40

internal fun TmdbMovieSummary.toResult() = MediaSearchResult(
    externalId = TmdbProvider.externalId(MediaType.MOVIE, id),
    mediaType = MediaType.MOVIE,
    title = title,
    subtitle = subtitleOf(releaseDate.yearOrNull()),
    posterUrl = TmdbProvider.poster(posterPath),
    backdropUrl = TmdbProvider.backdrop(backdropPath),
    releaseYear = releaseDate.yearOrNull(),
    overview = overview?.takeIf { it.isNotBlank() },
)

internal fun TmdbTvSummary.toResult() = MediaSearchResult(
    externalId = TmdbProvider.externalId(MediaType.TV_SHOW, id),
    mediaType = MediaType.TV_SHOW,
    title = name,
    subtitle = subtitleOf(firstAirDate.yearOrNull()),
    posterUrl = TmdbProvider.poster(posterPath),
    backdropUrl = TmdbProvider.backdrop(backdropPath),
    releaseYear = firstAirDate.yearOrNull(),
    overview = overview?.takeIf { it.isNotBlank() },
)

internal fun TmdbMovieDetails.toResult(region: String): MediaSearchResult {
    val directors = credits?.crew.orEmpty().filter { it.job == "Director" }.map { it.name }.distinct()
    val year = releaseDate.yearOrNull()
    return MediaSearchResult(
        externalId = TmdbProvider.externalId(MediaType.MOVIE, id),
        mediaType = MediaType.MOVIE,
        title = title,
        subtitle = subtitleOf(directors.firstOrNull(), year),
        posterUrl = TmdbProvider.poster(posterPath),
        backdropUrl = TmdbProvider.backdrop(backdropPath),
        releaseYear = year,
        lengthValue = runtime?.takeIf { it > 0 },
        genres = genres.map { it.name },
        overview = overview?.takeIf { it.isNotBlank() },
        metadata = MediaMetadata(
            creators = directors,
            streamingProviders = watchProviders.flatrateFor(region),
            sourceUrl = "https://www.themoviedb.org/movie/$id",
        ),
    )
}

internal fun TmdbTvDetails.toResult(region: String): MediaSearchResult {
    val creators = createdBy.map { it.name }
    val year = firstAirDate.yearOrNull()
    return MediaSearchResult(
        externalId = TmdbProvider.externalId(MediaType.TV_SHOW, id),
        mediaType = MediaType.TV_SHOW,
        title = name,
        subtitle = subtitleOf(creators.firstOrNull(), year),
        posterUrl = TmdbProvider.poster(posterPath),
        backdropUrl = TmdbProvider.backdrop(backdropPath),
        releaseYear = year,
        lengthValue = numberOfEpisodes?.takeIf { it > 0 },
        genres = genres.map { it.name },
        overview = overview?.takeIf { it.isNotBlank() },
        metadata = MediaMetadata(
            creators = creators,
            streamingProviders = watchProviders.flatrateFor(region),
            seasonCount = numberOfSeasons,
            sourceUrl = "https://www.themoviedb.org/tv/$id",
        ),
    )
}

private fun TmdbWatchProviders?.flatrateFor(region: String): List<String> =
    this?.results?.get(region)?.flatrate.orEmpty().map { it.providerName }.distinct()
