package com.ourshelf.app.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Upsert
import com.ourshelf.app.data.local.entity.CollectionEntity
import com.ourshelf.app.data.local.entity.CollectionItemEntity
import com.ourshelf.app.data.local.entity.MediaItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collections WHERE deletedAt IS NULL ORDER BY sortOrder, name")
    fun observeCollections(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getById(id: String): CollectionEntity?

    @Query("SELECT COUNT(*) FROM collections WHERE deletedAt IS NULL")
    suspend fun countActive(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM collections WHERE deletedAt IS NULL")
    suspend fun maxSortOrder(): Int

    @Upsert
    suspend fun upsert(collection: CollectionEntity)

    @Query("UPDATE collections SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    /** Tüm rafların eserleri tek sorguda; repository koleksiyona göre gruplar. */
    @Query(
        """
        SELECT ci.collectionId AS shelf_collectionId, m.* FROM collection_items ci
        JOIN media_items m ON m.id = ci.mediaItemId
        JOIN collections c ON c.id = ci.collectionId
        WHERE ci.deletedAt IS NULL AND m.deletedAt IS NULL AND c.deletedAt IS NULL
        ORDER BY ci.position, ci.addedAt
        """,
    )
    fun observeShelfItems(): Flow<List<ShelfItemRow>>

    @Query("SELECT collectionId FROM collection_items WHERE mediaItemId = :mediaItemId AND deletedAt IS NULL")
    fun observeCollectionIdsForItem(mediaItemId: String): Flow<List<String>>

    @Query("SELECT * FROM collection_items WHERE collectionId = :collectionId AND mediaItemId = :mediaItemId")
    suspend fun getLink(collectionId: String, mediaItemId: String): CollectionItemEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM collection_items WHERE collectionId = :collectionId AND deletedAt IS NULL")
    suspend fun maxPosition(collectionId: String): Int

    @Upsert
    suspend fun upsertLink(link: CollectionItemEntity)

    @Query(
        """
        UPDATE collection_items SET position = :position, updatedAt = :now
        WHERE collectionId = :collectionId AND mediaItemId = :mediaItemId AND position != :position
        """,
    )
    suspend fun updatePosition(collectionId: String, mediaItemId: String, position: Int, now: Long)

    @Query("UPDATE collections SET sortOrder = :sortOrder, updatedAt = :now WHERE id = :id AND sortOrder != :sortOrder")
    suspend fun updateSortOrder(id: String, sortOrder: Int, now: Long)

    @Query(
        """
        UPDATE collection_items SET deletedAt = :now, updatedAt = :now
        WHERE collectionId = :collectionId AND mediaItemId = :mediaItemId
        """,
    )
    suspend fun softDeleteLink(collectionId: String, mediaItemId: String, now: Long)
}

data class ShelfItemRow(
    val shelf_collectionId: String,
    @Embedded val item: MediaItemEntity,
)
