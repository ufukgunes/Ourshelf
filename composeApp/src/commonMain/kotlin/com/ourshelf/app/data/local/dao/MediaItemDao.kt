package com.ourshelf.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ourshelf.app.core.model.MediaStatus
import com.ourshelf.app.data.local.entity.MediaItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {

    @Query("SELECT * FROM media_items WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id AND deletedAt IS NULL")
    fun observeById(id: String): Flow<MediaItemEntity?>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getById(id: String): MediaItemEntity?

    /** Silinmiş (tombstone) kayıtları da döndürür; aynı eser tekrar eklenirse kayıt diriltilir. */
    @Query("SELECT * FROM media_items WHERE externalId = :externalId")
    suspend fun getByExternalId(externalId: String): MediaItemEntity?

    /** Arama ekranında "zaten ekli" işaretlemesi için: externalId -> id. */
    @Query("SELECT externalId, id FROM media_items WHERE deletedAt IS NULL")
    fun observeSavedIds(): Flow<List<SavedIdRow>>

    @Query(
        """
        SELECT * FROM media_items
        WHERE deletedAt IS NULL AND (status = 'IN_PROGRESS' OR isPinned = 1)
        ORDER BY isPinned DESC, updatedAt DESC
        """,
    )
    fun observeCurrentlyUnwinding(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE deletedAt IS NULL AND status = :status ORDER BY updatedAt DESC")
    fun observeByStatus(status: MediaStatus): Flow<List<MediaItemEntity>>

    @Query(
        """
        SELECT media_items.* FROM media_items
        JOIN media_items_fts ON media_items.rowid = media_items_fts.rowid
        WHERE media_items_fts MATCH :ftsQuery AND media_items.deletedAt IS NULL
        ORDER BY media_items.updatedAt DESC
        LIMIT :limit
        """,
    )
    suspend fun search(ftsQuery: String, limit: Int = 50): List<MediaItemEntity>

    @Upsert
    suspend fun upsert(item: MediaItemEntity)

    @Query("UPDATE media_items SET deletedAt = :now, updatedAt = :now, isPinned = 0 WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

data class SavedIdRow(val externalId: String, val id: String)
