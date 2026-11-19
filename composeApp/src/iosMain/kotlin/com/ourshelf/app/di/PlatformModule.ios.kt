package com.ourshelf.app.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import coil3.PlatformContext
import com.ourshelf.app.core.Platform
import com.ourshelf.app.data.local.OurshelfDatabase
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<RoomDatabase.Builder<OurshelfDatabase>> {
        // FTS4 (unicode61) desteği için paketlenmiş SQLite kullanılır.
        Room.databaseBuilder<OurshelfDatabase>(name = "${Platform.filesDir}/${OurshelfDatabase.NAME}")
            .setDriver(BundledSQLiteDriver())
    }
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath { "${Platform.filesDir}/settings.preferences_pb".toPath() }
    }
    single<HttpClientEngine> { Darwin.create() }
    single<PlatformContext> { PlatformContext.INSTANCE }
}
