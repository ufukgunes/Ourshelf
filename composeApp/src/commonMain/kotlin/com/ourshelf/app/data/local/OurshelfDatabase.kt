package com.ourshelf.app.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.ourshelf.app.data.local.dao.CollectionDao
import com.ourshelf.app.data.local.dao.MediaItemDao
import com.ourshelf.app.data.local.entity.CollectionEntity
import com.ourshelf.app.data.local.entity.CollectionItemEntity
import com.ourshelf.app.data.local.entity.MediaItemEntity
import com.ourshelf.app.data.local.entity.MediaItemFtsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [
        MediaItemEntity::class,
        MediaItemFtsEntity::class,
        CollectionEntity::class,
        CollectionItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(OurshelfDatabaseConstructor::class)
abstract class OurshelfDatabase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun collectionDao(): CollectionDao

    companion object {
        const val NAME = "ourshelf.db"
    }
}

/** Room KSP her platform için `actual` uygulamasını üretir. */
@Suppress("KotlinNoActualForExpect")
expect object OurshelfDatabaseConstructor : RoomDatabaseConstructor<OurshelfDatabase> {
    override fun initialize(): OurshelfDatabase
}

/** Platformdan gelen builder'ı ortak ayarlarla tamamlar. */
fun RoomDatabase.Builder<OurshelfDatabase>.buildOurshelf(): OurshelfDatabase =
    setQueryCoroutineContext(Dispatchers.IO).build()
