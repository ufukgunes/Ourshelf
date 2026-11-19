package com.ourshelf.app.data.repository

import com.ourshelf.app.core.model.MediaCollection
import com.ourshelf.app.core.model.Shelf
import com.ourshelf.app.data.local.OurshelfDatabase
import com.ourshelf.app.data.local.entity.CollectionEntity
import com.ourshelf.app.data.local.entity.CollectionItemEntity
import com.ourshelf.app.data.local.toDomain
import com.ourshelf.app.data.local.transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.uuid.Uuid

class CollectionRepository(
    private val database: OurshelfDatabase,
    private val clock: Clock,
) {
    private val dao = database.collectionDao()

    fun observeCollections(): Flow<List<MediaCollection>> =
        dao.observeCollections().map { list -> list.map { it.toDomain() } }

    /** Koleksiyonlar ve içerikleri; boş koleksiyonlar da raf olarak döner. */
    fun observeShelves(): Flow<List<Shelf>> =
        combine(dao.observeCollections(), dao.observeShelfItems()) { collections, rows ->
            val itemsByCollection = rows.groupBy({ it.shelf_collectionId }, { it.item.toDomain() })
            collections.map { c -> Shelf(c.toDomain(), itemsByCollection[c.id].orEmpty()) }
        }

    fun observeShelf(collectionId: String): Flow<Shelf?> =
        observeShelves().map { shelves -> shelves.firstOrNull { it.collection.id == collectionId } }

    /** Rafın içindeki sıralamayı verilen id sırasına göre kaydeder. */
    suspend fun reorderItems(collectionId: String, orderedItemIds: List<String>) = database.transaction {
        val now = now()
        orderedItemIds.forEachIndexed { index, itemId -> dao.updatePosition(collectionId, itemId, index, now) }
    }

    /** Rafların Lounge'daki sırasını kaydeder. */
    suspend fun reorderCollections(orderedIds: List<String>) = database.transaction {
        val now = now()
        orderedIds.forEachIndexed { index, id -> dao.updateSortOrder(id, index, now) }
    }

    fun observeCollectionIdsForItem(mediaItemId: String): Flow<Set<String>> =
        dao.observeCollectionIdsForItem(mediaItemId).map { it.toSet() }

    suspend fun create(name: String, iconEmoji: String): String = database.transaction {
        val now = now()
        val entity = CollectionEntity(
            id = Uuid.random().toString(),
            name = name.trim(),
            iconEmoji = iconEmoji.ifBlank { DEFAULT_EMOJI },
            themeColorHex = null,
            sortOrder = dao.maxSortOrder() + 1,
            smartRuleJson = null,
            updatedAt = now,
            deletedAt = null,
        )
        dao.upsert(entity)
        entity.id
    }

    suspend fun rename(id: String, name: String, iconEmoji: String) = database.transaction {
        val current = dao.getById(id) ?: return@transaction
        dao.upsert(
            current.copy(
                name = name.trim(),
                iconEmoji = iconEmoji.ifBlank { current.iconEmoji },
                updatedAt = now(),
            ),
        )
    }

    suspend fun delete(id: String) = dao.softDelete(id, now())

    suspend fun addItem(collectionId: String, mediaItemId: String) = database.transaction {
        val now = now()
        val existing = dao.getLink(collectionId, mediaItemId)
        if (existing != null && existing.deletedAt == null) return@transaction
        dao.upsertLink(
            CollectionItemEntity(
                collectionId = collectionId,
                mediaItemId = mediaItemId,
                position = dao.maxPosition(collectionId) + 1,
                addedAt = now,
                updatedAt = now,
                deletedAt = null,
            ),
        )
    }

    suspend fun removeItem(collectionId: String, mediaItemId: String) =
        dao.softDeleteLink(collectionId, mediaItemId, now())

    private fun now(): Long = clock.now().toEpochMilliseconds()

    companion object {
        const val DEFAULT_EMOJI = "📚"
    }
}
