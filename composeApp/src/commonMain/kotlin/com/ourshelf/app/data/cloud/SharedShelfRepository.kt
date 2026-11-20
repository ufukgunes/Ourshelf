package com.ourshelf.app.data.cloud

import com.ourshelf.app.core.model.MediaSearchResult
import com.ourshelf.app.core.model.MediaType
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class SharedMember(val uid: String, val name: String)

data class SharedShelf(
    val id: String,
    val name: String,
    val emoji: String,
    val ownerUid: String,
    val members: List<SharedMember>,
    val updatedAt: Instant?,
)

data class SharedItem(
    val id: String,
    val externalId: String,
    val mediaType: MediaType,
    val title: String,
    val subtitle: String?,
    val posterUrl: String?,
    val lengthValue: Int?,
    val releaseYear: Int?,
    val addedBy: String,
    val addedByName: String,
    /** "Ben de varım" diyen üyeler. */
    val votes: Set<String>,
    /** Bitiren üyeler ve zamanları. */
    val completedBy: Map<String, Instant?>,
    val addedAt: Instant?,
)

enum class ActivityType { CREATED, JOINED, ADDED, REMOVED, COMPLETED, LEFT }

data class ActivityEvent(
    val id: String,
    val type: ActivityType,
    val actorUid: String,
    val actorName: String,
    val itemTitle: String?,
    val at: Instant?,
)

/**
 * Paylaşılan raflar ("Bizim raf"). Kişisel kütüphaneden tamamen ayrıdır; yalnızca kullanıcı paylaşımı
 * kullandığında devreye girer. Her değişiklik raf belgesinin `updatedAt` alanını da günceller; açık olan ekranlar
 * yalnızca bu tek belgeyi yoklayarak değişikliği fark eder (ucuz "canlı" senkronizasyon).
 */
class SharedShelfRepository(
    private val config: CloudConfig,
    private val auth: AuthClient,
    private val firestore: FirestoreClient,
    private val clock: Clock = Clock.System,
    private val random: Random = Random.Default,
) {
    val isAvailable: Boolean get() = config.isConfigured

    suspend fun myUid(): String = auth.session().uid

    // ---- Profil ---------------------------------------------------------------------------------

    suspend fun profileName(): String? = firestore.get("users/${myUid()}")?.string("displayName")

    /** Görünen adı kaydeder (ilk kez ise profili oluşturur). */
    suspend fun saveProfileName(name: String) {
        val uid = myUid()
        val clean = name.trim().take(40)
        try {
            firestore.commit(
                listOf(
                    FsWrite.Set(
                        path = "users/$uid",
                        fields = mapOf("displayName" to clean),
                        mustNotExist = true,
                        serverTimestamps = listOf("createdAt"),
                    ),
                ),
            )
        } catch (e: CloudException.AlreadyExists) {
            firestore.commit(listOf(FsWrite.Set("users/$uid", mapOf("displayName" to clean), mask = listOf("displayName"))))
        }
    }

    suspend fun isPlus(): Boolean = firestore.get("users/${myUid()}")?.fields?.get("plus") == true

    // ---- Raflar ---------------------------------------------------------------------------------

    suspend fun myShelves(): List<SharedShelf> =
        firestore.queryArrayContains("shelves", "memberUids", myUid())
            .map(::toShelf)
            .sortedByDescending { it.updatedAt }

    suspend fun shelf(id: String): SharedShelf? = firestore.get("shelves/$id")?.let(::toShelf)

    /** Yalnızca Ourshelf+ abonelerine açıktır; değilse [CloudException.PermissionDenied]. */
    suspend fun createShelf(name: String, emoji: String, myName: String): String {
        val uid = myUid()
        val id = Uuid.random().toString()
        firestore.commit(
            listOf(
                FsWrite.Set(
                    path = "shelves/$id",
                    fields = mapOf(
                        "name" to name.trim().take(40),
                        "emoji" to emoji.ifBlank { "📚" },
                        "ownerUid" to uid,
                        "memberUids" to listOf(uid),
                        "memberNames" to mapOf(uid to myName),
                    ),
                    mustNotExist = true,
                    serverTimestamps = listOf("createdAt", "updatedAt"),
                ),
            ),
        )
        logActivity(id, ActivityType.CREATED, myName, null)
        return id
    }

    /** Davet kodu üretir (en az 128 bit rastgelelik; 14 gün geçerli). */
    suspend fun createInvite(shelfId: String): String {
        val token = newToken()
        firestore.commit(
            listOf(
                FsWrite.Set(
                    path = "invites/$token",
                    fields = mapOf(
                        "shelfId" to shelfId,
                        "createdBy" to myUid(),
                        "expiresAt" to clock.now() + INVITE_TTL,
                    ),
                    mustNotExist = true,
                ),
            ),
        )
        return token
    }

    /** Davetle katılır ve rafın kimliğini döndürür. Geçersiz/süresi dolmuş davette [CloudException.NotFound]. */
    suspend fun join(token: String, myName: String): String {
        val invite = firestore.get("invites/$token") ?: throw CloudException.NotFound("invite")
        val expires = invite.instant("expiresAt")
        if (expires != null && expires < clock.now()) throw CloudException.NotFound("invite expired")
        val shelfId = invite.string("shelfId") ?: throw CloudException.NotFound("invite")
        val uid = myUid()
        // Zaten üyeyse (aynı bağlantıya ikinci kez dokunmak) kurallar katılmayı reddeder; doğrudan rafa dön.
        if (runCatching { firestore.get("shelves/$shelfId") }.getOrNull() != null) return shelfId
        firestore.commit(
            listOf(
                FsWrite.Set(
                    path = "shelves/$shelfId",
                    fields = mapOf("memberNames.$uid" to myName, "joinToken" to token),
                    mask = listOf("memberNames.$uid", "joinToken"),
                    mustExist = true,
                    arrayUnion = mapOf("memberUids" to listOf(uid)),
                    serverTimestamps = listOf("updatedAt"),
                ),
            ),
        )
        logActivity(shelfId, ActivityType.JOINED, myName, null)
        return shelfId
    }

    suspend fun leave(shelfId: String, myName: String) {
        val uid = myUid()
        logActivity(shelfId, ActivityType.LEFT, myName, null)
        firestore.commit(
            listOf(
                FsWrite.Set(
                    path = "shelves/$shelfId",
                    mask = listOf("memberNames.$uid"),
                    arrayRemove = mapOf("memberUids" to listOf(uid)),
                    serverTimestamps = listOf("updatedAt"),
                ),
            ),
        )
    }

    suspend fun deleteShelf(shelfId: String) {
        firestore.commit(listOf(FsWrite.Delete("shelves/$shelfId")))
    }

    // ---- Eserler --------------------------------------------------------------------------------

    suspend fun items(shelfId: String): List<SharedItem> =
        firestore.list("shelves/$shelfId/items").map(::toItem).sortedByDescending { it.addedAt }

    /** Eseri rafa ekler; ekleyen otomatik olarak "ben de varım" der. Zaten varsa sessizce geçer. */
    suspend fun addItem(shelfId: String, result: MediaSearchResult, myName: String) {
        val uid = myUid()
        try {
            firestore.commit(
                listOf(
                    FsWrite.Set(
                        path = "shelves/$shelfId/items/${itemId(result.externalId)}",
                        fields = mapOf(
                            "externalId" to result.externalId,
                            "mediaType" to result.mediaType.name,
                            "title" to result.title.take(300),
                            "subtitle" to result.subtitle,
                            "posterUrl" to result.posterUrl,
                            "lengthValue" to result.lengthValue,
                            "releaseYear" to result.releaseYear,
                            "addedBy" to uid,
                            "addedByName" to myName,
                            "votes" to mapOf(uid to true),
                            "completedBy" to emptyMap<String, Any?>(),
                        ),
                        mustNotExist = true,
                        serverTimestamps = listOf("addedAt"),
                    ),
                    touch(shelfId),
                ),
            )
        } catch (e: CloudException.AlreadyExists) {
            return
        }
        logActivity(shelfId, ActivityType.ADDED, myName, result.title)
    }

    suspend fun setVote(shelfId: String, itemId: String, wantIt: Boolean) {
        val uid = myUid()
        firestore.commit(
            listOf(
                FsWrite.Set(
                    path = "shelves/$shelfId/items/$itemId",
                    fields = if (wantIt) mapOf("votes.$uid" to true) else emptyMap(),
                    mask = listOf("votes.$uid"),
                    mustExist = true,
                ),
                touch(shelfId),
            ),
        )
    }

    suspend fun markCompleted(shelfId: String, item: SharedItem, myName: String) {
        val uid = myUid()
        firestore.commit(
            listOf(
                FsWrite.Set(
                    path = "shelves/$shelfId/items/${item.id}",
                    mask = emptyList(),
                    mustExist = true,
                    serverTimestamps = listOf("completedBy.$uid"),
                ),
                touch(shelfId),
            ),
        )
        logActivity(shelfId, ActivityType.COMPLETED, myName, item.title)
    }

    suspend fun removeItem(shelfId: String, item: SharedItem, myName: String) {
        firestore.commit(listOf(FsWrite.Delete("shelves/$shelfId/items/${item.id}"), touch(shelfId)))
        logActivity(shelfId, ActivityType.REMOVED, myName, item.title)
    }

    // ---- Aktivite -------------------------------------------------------------------------------

    suspend fun activity(shelfId: String, limit: Int = 30): List<ActivityEvent> =
        firestore.list("shelves/$shelfId/activity")
            .map(::toActivity)
            .sortedByDescending { it.at }
            .take(limit)

    private suspend fun logActivity(shelfId: String, type: ActivityType, myName: String, itemTitle: String?) {
        // Aktivite süs niteliğindedir; yazılamazsa asıl işlemi bozmaz.
        runCatching {
            firestore.commit(
                listOf(
                    FsWrite.Set(
                        path = "shelves/$shelfId/activity/${Uuid.random()}",
                        fields = mapOf(
                            "type" to type.name,
                            "actorUid" to myUid(),
                            "actorName" to myName,
                            "itemTitle" to itemTitle,
                        ),
                        mustNotExist = true,
                        serverTimestamps = listOf("at"),
                    ),
                ),
            )
        }
    }

    // ---- Yardımcılar ----------------------------------------------------------------------------

    private fun touch(shelfId: String) = FsWrite.Set(
        path = "shelves/$shelfId",
        mask = emptyList(),
        mustExist = true,
        serverTimestamps = listOf("updatedAt"),
    )

    private fun newToken(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
        return (1..24).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
    }

    private fun toShelf(doc: FsDocument): SharedShelf {
        val names = doc.map("memberNames")
        return SharedShelf(
            id = doc.id,
            name = doc.string("name").orEmpty(),
            emoji = doc.string("emoji") ?: "📚",
            ownerUid = doc.string("ownerUid").orEmpty(),
            members = doc.stringList("memberUids").map { SharedMember(it, names[it] as? String ?: "?") },
            updatedAt = doc.instant("updatedAt"),
        )
    }

    private fun toItem(doc: FsDocument) = SharedItem(
        id = doc.id,
        externalId = doc.string("externalId").orEmpty(),
        mediaType = doc.string("mediaType")?.let { runCatching { MediaType.valueOf(it) }.getOrNull() } ?: MediaType.MOVIE,
        title = doc.string("title").orEmpty(),
        subtitle = doc.string("subtitle"),
        posterUrl = doc.string("posterUrl"),
        lengthValue = doc.long("lengthValue")?.toInt(),
        releaseYear = doc.long("releaseYear")?.toInt(),
        addedBy = doc.string("addedBy").orEmpty(),
        addedByName = doc.string("addedByName").orEmpty(),
        votes = doc.map("votes").filterValues { it == true }.keys,
        completedBy = doc.map("completedBy").mapValues { it.value as? Instant },
        addedAt = doc.instant("addedAt"),
    )

    private fun toActivity(doc: FsDocument) = ActivityEvent(
        id = doc.id,
        type = doc.string("type")?.let { runCatching { ActivityType.valueOf(it) }.getOrNull() } ?: ActivityType.ADDED,
        actorUid = doc.string("actorUid").orEmpty(),
        actorName = doc.string("actorName").orEmpty(),
        itemTitle = doc.string("itemTitle"),
        at = doc.instant("at"),
    )

    companion object {
        val INVITE_TTL = 14.days

        /** Eser kimliği belge kimliğine çevrilir ("tmdb:movie:550" → "tmdb_movie_550"); aynı eser bir kez eklenir. */
        fun itemId(externalId: String) = externalId.replace(Regex("[^A-Za-z0-9_-]"), "_")

        /** Davet bağlantısından ya da yapıştırılan metinden kodu çıkarır. */
        fun parseInviteToken(text: String): String? {
            val trimmed = text.trim()
            val fromLink = Regex("/j/([A-Za-z0-9]{16,40})").find(trimmed)?.groupValues?.get(1)
            if (fromLink != null) return fromLink
            return trimmed.takeIf { it.length in 16..40 && it.all { c -> c.isLetterOrDigit() } }
        }

        fun inviteLink(token: String, projectId: String) = "https://$projectId.web.app/j/$token"
    }
}
