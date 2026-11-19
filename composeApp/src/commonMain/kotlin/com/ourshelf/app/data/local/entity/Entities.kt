package com.ourshelf.app.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ourshelf.app.core.model.MediaStatus
import com.ourshelf.app.core.model.MediaType

/**
 * Tüm medya türleri tek tabloda tutulur. Zaman alanları UTC epoch millis'tir.
 * `genres` ve `metadataJson` JSON metni olarak saklanır (FTS indekslemesi için düz TEXT).
 */
@Entity(
    tableName = "media_items",
    indices = [
        Index(value = ["externalId"], unique = true),
        Index(value = ["status"]),
    ],
)
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val externalId: String,
    val mediaType: MediaType,
    val title: String,
    val subtitle: String?,
    val posterUrl: String?,
    val localPosterFile: String?,
    val backdropUrl: String?,
    val dominantColorHex: String?,
    val accentColorHex: String?,
    val releaseYear: Int?,
    val lengthValue: Int?,
    val genres: String,
    val overview: String?,
    val metadataJson: String,
    val status: MediaStatus,
    val progressValue: Int?,
    val userRating: Float?,
    val userNote: String?,
    val isPinned: Boolean,
    val addedAt: Long,
    val startedAt: Long?,
    val completedAt: Long?,
    val updatedAt: Long,
    val deletedAt: Long?,
)

/**
 * Çevrimdışı tam metin arama indeksi. Room, içerik tablosuyla senkron tutan trigger'ları üretir.
 * `unicode61`: Türkçe vb. ASCII dışı harflerde de büyük/küçük harf duyarsız eşleşme sağlar.
 */
@Fts4(contentEntity = MediaItemEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "media_items_fts")
data class MediaItemFtsEntity(
    val title: String,
    val subtitle: String?,
    val overview: String?,
    val genres: String,
)

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconEmoji: String,
    val themeColorHex: String?,
    val sortOrder: Int,
    /** Null ise manuel koleksiyon; doluysa Akıllı Liste kuralı (Faz 4). */
    val smartRuleJson: String?,
    val updatedAt: Long,
    val deletedAt: Long?,
)

/** Koleksiyon ↔ eser çoka-çok ilişkisi. Senkronizasyon için o da tombstone taşır. */
@Entity(
    tableName = "collection_items",
    primaryKeys = ["collectionId", "mediaItemId"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["mediaItemId"])],
)
data class CollectionItemEntity(
    val collectionId: String,
    val mediaItemId: String,
    val position: Int,
    val addedAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
