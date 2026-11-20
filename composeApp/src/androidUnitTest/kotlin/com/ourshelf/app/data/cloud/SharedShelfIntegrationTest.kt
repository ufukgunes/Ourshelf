package com.ourshelf.app.data.cloud

import com.ourshelf.app.core.model.MediaSearchResult
import com.ourshelf.app.core.model.MediaType
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * Gerçek güvenlik kurallarıyla, Firebase Emulator'a karşı uçtan uca testler.
 * Emulator çalışmıyorsa atlanır. Çalıştırmak için (firebase/ klasöründe):
 *   npx firebase emulators:exec --only firestore,auth --project demo-ourshelf \
 *     "cd .. && ./gradlew :composeApp:testDebugUnitTest --tests *SharedShelfIntegrationTest*"
 */
class SharedShelfIntegrationTest {

    private val config = CloudConfig.emulator()
    private val http = HttpClient(OkHttp) { expectSuccess = false }

    private fun repo(): Pair<SharedShelfRepository, AuthClient> {
        val auth = AuthClient(http, config, InMemorySessionStore())
        return SharedShelfRepository(config, auth, FirestoreClient(http, config, auth)) to auth
    }

    @Before
    fun requireEmulator() {
        assumeTrue("Firestore emulator not running", System.getenv("FIRESTORE_EMULATOR_HOST") != null)
    }

    /** Emulator'da "owner" token'ı kuralları atlar: abonelik sunucu tarafından yazılmış gibi işaretlenir. */
    private suspend fun grantPlus(uid: String) {
        http.patch("${config.firestoreBase}/v1/${config.documentsRoot}/users/$uid?updateMask.fieldPaths=plus") {
            header("Authorization", "Bearer owner")
            contentType(ContentType.Application.Json)
            setBody("""{"fields":{"plus":{"booleanValue":true}}}""")
        }
    }

    private val dune = MediaSearchResult(
        externalId = "tmdb:movie:693134", mediaType = MediaType.MOVIE, title = "Dune: Part Two",
        subtitle = "Denis Villeneuve • 2024", lengthValue = 166, releaseYear = 2024,
    )

    @Test
    fun fullSharedShelfFlow() = runBlocking {
        val (alice, aliceAuth) = repo()
        val (bob, bobAuth) = repo()

        alice.saveProfileName("Alice")
        bob.saveProfileName("Bob")
        assertEquals("Alice", alice.profileName())

        // Abone olmayan raf kuramaz.
        val denied = runCatching { alice.createShelf("Film gecesi", "🍿", "Alice") }
        assertTrue(denied.exceptionOrNull() is CloudException.PermissionDenied)

        grantPlus(aliceAuth.currentUid()!!)
        assertTrue(alice.isPlus())
        val shelfId = alice.createShelf("Film gecesi", "🍿", "Alice")

        // Bob davetle ücretsiz katılır; ikinci kez katılmak hata vermez.
        val token = alice.createInvite(shelfId)
        assertEquals(shelfId, bob.join(token, "Bob"))
        assertEquals(shelfId, bob.join(token, "Bob"))
        assertEquals(listOf("Alice", "Bob"), bob.shelf(shelfId)!!.members.map { it.name })
        assertEquals(listOf(shelfId), bob.myShelves().map { it.id })

        // Bob eser ekler (otomatik oy), Alice de oy verir ve bitirir.
        bob.addItem(shelfId, dune, "Bob")
        bob.addItem(shelfId, dune, "Bob") // ikinci ekleme sessizce geçer
        val item = alice.items(shelfId).single()
        assertEquals(setOf(bobAuth.currentUid()), item.votes)
        alice.setVote(shelfId, item.id, true)
        alice.markCompleted(shelfId, item, "Alice")
        val updated = bob.items(shelfId).single()
        assertEquals(2, updated.votes.size)
        assertTrue(aliceAuth.currentUid() in updated.completedBy)

        // Bob oyunu geri çeker.
        bob.setVote(shelfId, item.id, false)
        assertEquals(setOf(aliceAuth.currentUid()), alice.items(shelfId).single().votes)

        // Aktivite akışı.
        val types = alice.activity(shelfId).map { it.type }.toSet()
        assertTrue(types.containsAll(setOf(ActivityType.CREATED, ActivityType.JOINED, ActivityType.ADDED, ActivityType.COMPLETED)))

        // Davet olmadan üçüncü kişi rafı göremez.
        val (eve, _) = repo()
        eve.saveProfileName("Eve")
        assertTrue(eve.myShelves().isEmpty())
        assertTrue(runCatching { eve.items(shelfId) }.exceptionOrNull() is CloudException.PermissionDenied)

        // Bob ayrılır; artık rafı göremez.
        bob.leave(shelfId, "Bob")
        assertTrue(bob.myShelves().isEmpty())
        assertFalse(bobAuth.currentUid() in alice.shelf(shelfId)!!.members.map { it.uid })
    }

    @Test
    fun invalidInviteIsRejected() = runBlocking {
        val (bob, _) = repo()
        bob.saveProfileName("Bob")
        assertTrue(runCatching { bob.join("doesNotExist1234567890", "Bob") }.exceptionOrNull() is CloudException.NotFound)
    }
}
