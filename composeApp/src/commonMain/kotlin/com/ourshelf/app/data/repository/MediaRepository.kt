package com.ourshelf.app.data.repository

import com.ourshelf.app.core.Platform
import com.ourshelf.app.core.model.MediaItem
import com.ourshelf.app.core.model.MediaSearchResult
import com.ourshelf.app.core.model.MediaStatus
import com.ourshelf.app.data.image.PosterProcessor
import com.ourshelf.app.data.local.FtsQuery
import com.ourshelf.app.data.local.OurshelfDatabase
import com.ourshelf.app.data.local.encodeGenres
import com.ourshelf.app.data.local.encodeMetadata
import com.ourshelf.app.data.local.entity.MediaItemEntity
import com.ourshelf.app.data.local.toDomain
import com.ourshelf.app.data.local.toHexColor
import com.ourshelf.app.data.local.transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.round
import kotlin.time.Clock
import kotlin.uuid.Uuid

class MediaRepository(
    private val database: OurshelfDatabase,
    private val registry: MediaProviderRegistry,
    private val posterProcessor: PosterProcessor,
    private val clock: Clock,
    private val appScope: CoroutineScope,
) {
    private val dao = database.mediaItemDao()

    fun observeLibrary(): Flow<List<MediaItem>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeItem(id: String): Flow<MediaItem?> = dao.observeById(id).map { it?.toDomain() }

    fun observeCurrentlyUnwinding(): Flow<List<MediaItem>> =
        dao.observeCurrentlyUnwinding().map { list -> list.map { it.toDomain() } }

    fun observeByStatus(status: MediaStatus): Flow<List<MediaItem>> =
        dao.observeByStatus(status).map { list -> list.map { it.toDomain() } }

    /** externalId -> yerel id. */
    fun observeSavedIds(): Flow<Map<String, String>> =
        dao.observeSavedIds().map { rows -> rows.associate { it.externalId to it.id } }

    suspend fun searchLocal(query: String): List<MediaItem> {
        val fts = FtsQuery.build(query) ?: return emptyList()
        return dao.search(fts).map { it.toDomain() }
    }

    /**
     * Arama sonucunu kütüphaneye ekler ve yerel id'yi döndürür. Kayıt hemen yazılır;
     * detay zenginleştirme ve poster/renk işleme arka planda yapılır.
     * Daha önce silinmiş bir eser tekrar eklenirse aynı kayıt diriltilir.
     */
    suspend fun addFromSearch(result: MediaSearchResult, status: MediaStatus = MediaStatus.BACKLOG): String {
        val now = now()
        val id = database.transaction {
            val existing = dao.getByExternalId(result.externalId)
            if (existing != null && existing.deletedAt == null) return@transaction existing.id

            val entity = MediaItemEntity(
                id = existing?.id ?: Uuid.random().toString(),
                externalId = result.externalId,
                mediaType = result.mediaType,
                title = result.title,
                subtitle = result.subtitle,
                posterUrl = result.posterUrl,
                localPosterFile = existing?.localPosterFile,
                backdropUrl = result.backdropUrl,
                dominantColorHex = existing?.dominantColorHex,
                accentColorHex = existing?.accentColorHex,
                releaseYear = result.releaseYear,
                lengthValue = result.lengthValue,
                genres = encodeGenres(result.genres),
                overview = result.overview,
                metadataJson = encodeMetadata(result.metadata),
                status = MediaStatus.BACKLOG,
                progressValue = null,
                userRating = null,
                userNote = null,
                isPinned = false,
                addedAt = now,
                startedAt = null,
                completedAt = null,
                updatedAt = now,
                deletedAt = null,
            ).withStatus(status, now)
            dao.upsert(entity)
            entity.id
        }
        appScope.launch { enrich(id, result) }
        return id
    }

    private suspend fun enrich(id: String, result: MediaSearchResult) {
        try {
            val detailed = registry.providerFor(result.mediaType).details(result).getOrNull()
            if (detailed != null) {
                mutate(id) { e ->
                    e.copy(
                        subtitle = detailed.subtitle ?: e.subtitle,
                        posterUrl = detailed.posterUrl ?: e.posterUrl,
                        backdropUrl = detailed.backdropUrl ?: e.backdropUrl,
                        releaseYear = detailed.releaseYear ?: e.releaseYear,
                        lengthValue = detailed.lengthValue ?: e.lengthValue,
                        genres = if (detailed.genres.isNotEmpty()) encodeGenres(detailed.genres) else e.genres,
                        overview = detailed.overview ?: e.overview,
                        metadataJson = encodeMetadata(detailed.metadata),
                    )
                }
            }
            val posterUrl = dao.getById(id)?.posterUrl ?: return
            val poster = posterProcessor.process(result.externalId, posterUrl) ?: return
            mutate(id) { e ->
                e.copy(
                    localPosterFile = poster.fileName,
                    dominantColorHex = poster.dominantColor?.toHexColor(),
                    accentColorHex = poster.accentColor?.toHexColor(),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Zenginleştirme başarısız olsa da eser arama sonucundaki bilgilerle kütüphanede kalır.
            Platform.logWarning(TAG, "Enrichment failed for ${result.externalId}", e)
        }
    }

    suspend fun setStatus(id: String, status: MediaStatus) = mutate(id) { it.withStatus(status, now()) }

    /** Yarım yıldız adımlarıyla 0.5–5.0; null puanı kaldırır. */
    suspend fun setRating(id: String, rating: Float?) = mutate(id) {
        it.copy(userRating = rating?.let { r -> (round(r * 2) / 2f).coerceIn(0.5f, 5f) })
    }

    suspend fun setNote(id: String, note: String?) = mutate(id) {
        it.copy(userNote = note?.trim()?.takeIf(String::isNotEmpty))
    }

    suspend fun setProgress(id: String, progress: Int?) = mutate(id) { e ->
        val clamped = progress?.coerceAtLeast(0)?.let { p -> e.lengthValue?.let { p.coerceAtMost(it) } ?: p }
        e.copy(progressValue = clamped)
    }

    suspend fun setPinned(id: String, pinned: Boolean) = mutate(id) { it.copy(isPinned = pinned) }

    /**
     * Finisher Stamp'ın "Geri al"ı: tamamlamanın değiştirdiği alanları önceki anlık görüntüye döndürür.
     * Bu arada girilen puan ve not korunur.
     */
    suspend fun revertCompletion(id: String, previous: MediaItem) = mutate(id) {
        it.copy(
            status = previous.status,
            startedAt = previous.startedAt?.toEpochMilliseconds(),
            completedAt = previous.completedAt?.toEpochMilliseconds(),
            progressValue = previous.progressValue,
            isPinned = previous.isPinned,
        )
    }

    suspend fun delete(id: String) = dao.softDelete(id, now())

    /** Silmeyi geri al (Snackbar "Geri Al"). */
    suspend fun restore(id: String) = database.transaction {
        val e = dao.getById(id) ?: return@transaction
        dao.upsert(e.copy(deletedAt = null, updatedAt = now()))
    }

    /** Oku-değiştir-yaz işlemini tek transaction'da yapar ve `updatedAt`'i günceller. */
    private suspend fun mutate(id: String, transform: (MediaItemEntity) -> MediaItemEntity) {
        database.transaction {
            val current = dao.getById(id) ?: return@transaction
            if (current.deletedAt != null) return@transaction
            val next = transform(current)
            if (next != current) dao.upsert(next.copy(updatedAt = now()))
        }
    }

    private fun now(): Long = clock.now().toEpochMilliseconds()

    private companion object {
        const val TAG = "MediaRepository"
    }
}

/** Durum geçişlerinin yan etkileri: başlama/bitiş zamanları ve ilerleme. */
internal fun MediaItemEntity.withStatus(status: MediaStatus, now: Long): MediaItemEntity = when (status) {
    MediaStatus.BACKLOG -> copy(status = status, completedAt = null)
    MediaStatus.IN_PROGRESS -> copy(status = status, startedAt = startedAt ?: now, completedAt = null)
    MediaStatus.COMPLETED -> copy(
        status = status,
        startedAt = startedAt ?: now,
        completedAt = now,
        progressValue = lengthValue ?: progressValue,
        isPinned = false,
    )
    MediaStatus.ABANDONED -> copy(status = status, completedAt = null, isPinned = false)
}
