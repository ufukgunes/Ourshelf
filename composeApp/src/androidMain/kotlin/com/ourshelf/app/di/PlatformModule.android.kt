package com.ourshelf.app.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ourshelf.app.data.local.OurshelfDatabase
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Cache
import okio.Path.Companion.toOkioPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

actual val platformModule: Module = module {
    single<RoomDatabase.Builder<OurshelfDatabase>> {
        val context = androidContext()
        Room.databaseBuilder<OurshelfDatabase>(
            context = context,
            name = context.getDatabasePath(OurshelfDatabase.NAME).absolutePath,
        )
    }
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath {
            File(androidContext().filesDir, "datastore/settings.preferences_pb").toOkioPath()
        }
    }
    single<HttpClientEngine> {
        val cacheDir = File(androidContext().cacheDir, "http_cache")
        OkHttp.create {
            config { cache(Cache(cacheDir, 20L * 1024 * 1024)) }
        }
    }
    // Context (= coil3.PlatformContext) Koin'e `startKoin { androidContext(...) }` ile zaten kaydedilir.
    // Burada yeniden tanımlamak, androidContext() kendini çözmeye çalıştığı için sonsuz döngü oluşturur.
}
