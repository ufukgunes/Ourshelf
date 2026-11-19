package com.ourshelf.app.di

import androidx.room.RoomDatabase
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.ourshelf.app.Secrets
import com.ourshelf.app.core.model.MediaType
import com.ourshelf.app.data.image.CoilPosterProcessor
import com.ourshelf.app.data.image.PosterProcessor
import com.ourshelf.app.data.local.OurshelfDatabase
import com.ourshelf.app.data.local.buildOurshelf
import com.ourshelf.app.data.prefs.UserPreferencesRepository
import com.ourshelf.app.data.remote.MediaSearchProvider
import com.ourshelf.app.data.remote.igdb.IgdbApi
import com.ourshelf.app.data.remote.igdb.IgdbProvider
import com.ourshelf.app.data.remote.itunes.ItunesApi
import com.ourshelf.app.data.remote.itunes.ItunesProvider
import com.ourshelf.app.data.remote.mock.MockMediaProvider
import com.ourshelf.app.data.remote.openlibrary.OpenLibraryApi
import com.ourshelf.app.data.remote.openlibrary.OpenLibraryProvider
import com.ourshelf.app.data.remote.tmdb.TmdbApi
import com.ourshelf.app.data.remote.tmdb.TmdbProvider
import com.ourshelf.app.data.remote.tmdb.isTmdbBearerToken
import com.ourshelf.app.data.repository.CollectionRepository
import com.ourshelf.app.data.repository.MediaProviderRegistry
import com.ourshelf.app.data.repository.MediaRepository
import com.ourshelf.app.data.repository.SearchRepository
import com.ourshelf.app.ui.detail.DetailViewModel
import com.ourshelf.app.ui.lounge.LoungeViewModel
import com.ourshelf.app.ui.navigation.AppViewModel
import com.ourshelf.app.ui.search.SearchViewModel
import com.ourshelf.app.ui.shelf.ManageShelvesViewModel
import com.ourshelf.app.ui.shelf.ShelfViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Clock

/** Uygulama ömrü boyunca yaşayan coroutine kapsamı (ekran kapansa da bitmesi gereken işler için). */
val AppScope = named("appScope")

/** Platform modülü: veritabanı builder'ı, DataStore, HTTP motoru ve Coil PlatformContext sağlar. */
expect val platformModule: Module

val networkJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}

fun createHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    expectSuccess = true
    install(ContentNegotiation) { json(networkJson) }
    install(HttpTimeout) {
        connectTimeoutMillis = 15_000
        requestTimeoutMillis = 20_000
    }
    defaultRequest {
        // OpenLibrary ve MusicBrainz istemcinin kendini tanıtmasını ister. Kişisel bilgi içermez.
        header(HttpHeaders.UserAgent, "Ourshelf/${Secrets.VERSION_NAME}")
    }
}

/** Anahtarı olmayan türler Mock sağlayıcıyla çalışır. */
fun createProviderRegistry(client: HttpClient): MediaProviderRegistry {
    val mock = MockMediaProvider()

    val tmdbKey = Secrets.TMDB_API_KEY
    val movies: MediaSearchProvider = if (tmdbKey.isNotBlank()) {
        TmdbProvider(
            TmdbApi(
                client.config {
                    defaultRequest {
                        url(TmdbApi.BASE_URL)
                        if (isTmdbBearerToken(tmdbKey)) {
                            header(HttpHeaders.Authorization, "Bearer $tmdbKey")
                        } else {
                            url.parameters.append("api_key", tmdbKey)
                        }
                    }
                },
            ),
        )
    } else {
        mock
    }

    val books = OpenLibraryProvider(OpenLibraryApi(client.config { defaultRequest { url(OpenLibraryApi.BASE_URL) } }))
    val audio = ItunesProvider(ItunesApi(client.config { defaultRequest { url(ItunesApi.BASE_URL) } }))

    val igdbUrl = Secrets.IGDB_PROXY_URL
    val games: MediaSearchProvider = if (igdbUrl.isNotBlank()) {
        val proxyKey = Secrets.IGDB_PROXY_KEY
        IgdbProvider(
            IgdbApi(
                client.config {
                    defaultRequest {
                        url(igdbUrl.trimEnd('/') + "/")
                        if (proxyKey.isNotBlank()) header("X-Ourshelf-Key", proxyKey)
                    }
                },
            ),
        )
    } else {
        mock
    }

    return MediaProviderRegistry(
        mapOf(
            MediaType.MOVIE to movies,
            MediaType.TV_SHOW to movies,
            MediaType.BOOK to books,
            MediaType.GAME to games,
            MediaType.PODCAST to audio,
            MediaType.ALBUM to audio,
        ),
    )
}

val appModule = module {
    single<Clock> { Clock.System }
    single(AppScope) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single { get<RoomDatabase.Builder<OurshelfDatabase>>().buildOurshelf() }
    single { UserPreferencesRepository(get()) }

    single { createHttpClient(get()) }
    single { createProviderRegistry(get()) }
    single {
        ImageLoader.Builder(get<PlatformContext>())
            .components { add(KtorNetworkFetcherFactory(httpClient = { get<HttpClient>() })) }
            .crossfade(true)
            .build()
    }
    single<PosterProcessor> { CoilPosterProcessor(get(), get()) }

    single { MediaRepository(get(), get(), get(), get(), get(AppScope)) }
    single { CollectionRepository(get(), get()) }
    single { SearchRepository(get()) }

    viewModelOf(::AppViewModel)
    viewModelOf(::LoungeViewModel)
    viewModelOf(::SearchViewModel)
    viewModel { DetailViewModel(get(), get(), get(), get(AppScope)) }
    viewModelOf(::ShelfViewModel)
    viewModelOf(::ManageShelvesViewModel)
}

fun ourshelfModules(): List<Module> = listOf(platformModule, appModule)
