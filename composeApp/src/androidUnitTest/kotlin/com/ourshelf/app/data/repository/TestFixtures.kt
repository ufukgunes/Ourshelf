package com.ourshelf.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ourshelf.app.core.model.MediaType
import com.ourshelf.app.data.image.PosterProcessor
import com.ourshelf.app.data.image.ProcessedPoster
import com.ourshelf.app.data.local.OurshelfDatabase
import com.ourshelf.app.data.remote.mock.MockMediaProvider
import java.util.concurrent.Executor
import kotlin.time.Clock
import kotlin.time.Instant

class MutableClock(var millis: Long = 1_700_000_000_000L) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(millis)
    fun advance(ms: Long = 1_000) {
        millis += ms
    }
}

class FakePosterProcessor : PosterProcessor {
    val processed = mutableListOf<String>()
    override suspend fun process(externalId: String, url: String): ProcessedPoster {
        processed += url
        return ProcessedPoster("${externalId.replace(':', '_')}.webp", dominantColor = 0xFF112233.toInt(), accentColor = null)
    }
}

/** Room'un sorgularını test thread'inde senkron çalıştıran in-memory veritabanı (Android sistem SQLite'ı). */
fun inMemoryDatabase(): OurshelfDatabase {
    val direct = Executor { it.run() }
    return Room.inMemoryDatabaseBuilder<OurshelfDatabase>(ApplicationProvider.getApplicationContext<Context>())
        .allowMainThreadQueries()
        .setQueryExecutor(direct)
        .setTransactionExecutor(direct)
        .build()
}

fun mockRegistry() = MediaProviderRegistry(MediaType.entries.associateWith { MockMediaProvider(latencyMs = 0) })
