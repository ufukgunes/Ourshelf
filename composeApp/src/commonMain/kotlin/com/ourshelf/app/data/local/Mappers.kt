package com.ourshelf.app.data.local

import com.ourshelf.app.core.model.MediaCollection
import com.ourshelf.app.core.model.MediaItem
import com.ourshelf.app.core.model.MediaMetadata
import com.ourshelf.app.data.local.entity.CollectionEntity
import com.ourshelf.app.data.local.entity.MediaItemEntity
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.time.Instant

internal val storageJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

private val stringList = ListSerializer(String.serializer())

internal fun encodeGenres(genres: List<String>): String = storageJson.encodeToString(stringList, genres)

internal fun decodeGenres(raw: String): List<String> =
    runCatching { storageJson.decodeFromString(stringList, raw) }.getOrDefault(emptyList())

internal fun encodeMetadata(metadata: MediaMetadata): String =
    storageJson.encodeToString(MediaMetadata.serializer(), metadata)

internal fun decodeMetadata(raw: String): MediaMetadata =
    runCatching { storageJson.decodeFromString(MediaMetadata.serializer(), raw) }.getOrDefault(MediaMetadata())

/** ARGB int -> `#AARRGGBB`. */
internal fun Int.toHexColor(): String = "#" + toUInt().toString(16).uppercase().padStart(8, '0')

/** `#RRGGBB` / `#AARRGGBB` -> ARGB int. */
internal fun String.parseHexColor(): Int? {
    val hex = removePrefix("#")
    val value = hex.toLongOrNull(16) ?: return null
    return when (hex.length) {
        6 -> (0xFF000000L or value).toInt()
        8 -> value.toInt()
        else -> null
    }
}

fun MediaItemEntity.toDomain(): MediaItem = MediaItem(
    id = id,
    externalId = externalId,
    mediaType = mediaType,
    title = title,
    subtitle = subtitle,
    posterUrl = posterUrl,
    localPosterFile = localPosterFile,
    backdropUrl = backdropUrl,
    dominantColor = dominantColorHex?.parseHexColor(),
    accentColor = accentColorHex?.parseHexColor(),
    releaseYear = releaseYear,
    lengthValue = lengthValue,
    genres = decodeGenres(genres),
    overview = overview,
    metadata = decodeMetadata(metadataJson),
    status = status,
    progressValue = progressValue,
    userRating = userRating,
    userNote = userNote,
    isPinned = isPinned,
    addedAt = Instant.fromEpochMilliseconds(addedAt),
    startedAt = startedAt?.let(Instant::fromEpochMilliseconds),
    completedAt = completedAt?.let(Instant::fromEpochMilliseconds),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
)

fun CollectionEntity.toDomain(): MediaCollection = MediaCollection(
    id = id,
    name = name,
    iconEmoji = iconEmoji,
    themeColor = themeColorHex?.parseHexColor(),
    sortOrder = sortOrder,
)
