package com.ourshelf.app.core.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** Türe özel ek bilgiler. Veritabanında `metadataJson` sütununda JSON olarak tutulur. */
@Serializable
data class MediaMetadata(
    val creators: List<String> = emptyList(),
    val streamingProviders: List<String> = emptyList(),
    val isbn: String? = null,
    val platforms: List<String> = emptyList(),
    val seasonCount: Int? = null,
    val sourceUrl: String? = null,
)

/** Bir sağlayıcının arama sonucu. Henüz kütüphaneye kaydedilmemiştir. */
data class MediaSearchResult(
    val externalId: String,
    val mediaType: MediaType,
    val title: String,
    val subtitle: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val releaseYear: Int? = null,
    val lengthValue: Int? = null,
    val genres: List<String> = emptyList(),
    val overview: String? = null,
    val metadata: MediaMetadata = MediaMetadata(),
)

/** Kullanıcının kütüphanesindeki bir eser. */
data class MediaItem(
    val id: String,
    val externalId: String,
    val mediaType: MediaType,
    val title: String,
    val subtitle: String?,
    val posterUrl: String?,
    val localPosterFile: String?,
    val backdropUrl: String?,
    val dominantColor: Int?,
    val accentColor: Int?,
    val releaseYear: Int?,
    val lengthValue: Int?,
    val genres: List<String>,
    val overview: String?,
    val metadata: MediaMetadata,
    val status: MediaStatus,
    val progressValue: Int?,
    val userRating: Float?,
    val userNote: String?,
    val isPinned: Boolean,
    val addedAt: Instant,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val updatedAt: Instant,
)

data class MediaCollection(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val themeColor: Int?,
    val sortOrder: Int,
)

/** Lounge'da gösterilen bir raf: koleksiyon + içindeki eserler (sıralı). */
data class Shelf(
    val collection: MediaCollection,
    val items: List<MediaItem>,
)
