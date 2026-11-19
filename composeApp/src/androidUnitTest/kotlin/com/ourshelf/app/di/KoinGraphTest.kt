package com.ourshelf.app.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ImageLoader
import com.ourshelf.app.core.Platform
import com.ourshelf.app.data.image.PosterProcessor
import com.ourshelf.app.data.prefs.UserPreferencesRepository
import com.ourshelf.app.data.repository.CollectionRepository
import com.ourshelf.app.data.repository.MediaRepository
import com.ourshelf.app.data.repository.SearchRepository
import io.ktor.client.HttpClient
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

/**
 * Uygulamanın gerçek Koin modüllerini başlatıp ana bileşenleri çözer.
 * Tanım döngüleri, eksik bağımlılıklar veya yanlış tipler cihazda çökmeden önce burada yakalanır.
 */
@RunWith(AndroidJUnit4::class)
class KoinGraphTest {

    @After
    fun tearDown() = stopKoin()

    @Test
    fun appGraphResolves() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Platform.init(context)
        startKoin {
            androidContext(context)
            modules(ourshelfModules())
        }
        val koin = GlobalContext.get()

        assertNotNull(koin.get<HttpClient>())
        assertNotNull(koin.get<ImageLoader>())
        assertNotNull(koin.get<PosterProcessor>())
        assertNotNull(koin.get<UserPreferencesRepository>())
        assertNotNull(koin.get<MediaRepository>())
        assertNotNull(koin.get<CollectionRepository>())
        assertNotNull(koin.get<SearchRepository>())
    }
}
