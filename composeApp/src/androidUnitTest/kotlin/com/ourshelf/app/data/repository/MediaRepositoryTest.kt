package com.ourshelf.app.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ourshelf.app.core.model.MediaSearchResult
import com.ourshelf.app.core.model.MediaStatus
import com.ourshelf.app.core.model.MediaType
import com.ourshelf.app.data.local.OurshelfDatabase
import com.ourshelf.app.data.remote.mock.MockCatalog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MediaRepositoryTest {

    private lateinit var db: OurshelfDatabase
    private val clock = MutableClock()
    private val posters = FakePosterProcessor()

    private val inception = MockCatalog.items.first { it.title == "Inception" }
    private val dune = MockCatalog.items.first { it.title == "Dune" && it.mediaType == MediaType.BOOK }

    @Before
    fun setUp() {
        db = inMemoryDatabase()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun TestScope.repository() = MediaRepository(db, mockRegistry(), posters, clock, backgroundScope)

    @Test
    fun `adding the same external id twice keeps a single item`() = runTest {
        val repo = repository()
        val first = repo.addFromSearch(inception)
        val second = repo.addFromSearch(inception)

        assertEquals(first, second)
        assertEquals(1, repo.observeLibrary().first().size)
    }

    @Test
    fun `enrichment stores poster file and palette colors`() = runTest {
        val repo = repository()
        val withPoster = inception.copy(posterUrl = "https://example.org/inception.jpg")
        val id = repo.addFromSearch(withPoster)
        advanceUntilIdle()

        val item = repo.observeItem(id).first()!!
        assertEquals(listOf("https://example.org/inception.jpg"), posters.processed)
        assertEquals("mock_movie_1.webp", item.localPosterFile)
        assertEquals(0xFF112233.toInt(), item.dominantColor)
        assertNull(item.accentColor)
    }

    @Test
    fun `items without poster url are not sent to the poster processor`() = runTest {
        val repo = repository()
        repo.addFromSearch(dune)
        advanceUntilIdle()
        assertTrue(posters.processed.isEmpty())
    }

    @Test
    fun `status transitions maintain started, completed and progress`() = runTest {
        val repo = repository()
        val id = repo.addFromSearch(inception)

        clock.advance()
        repo.setStatus(id, MediaStatus.IN_PROGRESS)
        val started = repo.observeItem(id).first()!!
        assertNotNull(started.startedAt)
        assertNull(started.completedAt)

        clock.advance()
        repo.setStatus(id, MediaStatus.COMPLETED)
        val completed = repo.observeItem(id).first()!!
        assertEquals(started.startedAt, completed.startedAt)
        assertNotNull(completed.completedAt)
        assertEquals(inception.lengthValue, completed.progressValue)

        repo.setStatus(id, MediaStatus.BACKLOG)
        assertNull(repo.observeItem(id).first()!!.completedAt)
    }

    @Test
    fun `reverting a completion restores previous state but keeps the new rating`() = runTest {
        val repo = repository()
        val id = repo.addFromSearch(dune, MediaStatus.IN_PROGRESS)
        repo.setProgress(id, 120)
        repo.setPinned(id, true)
        val before = repo.observeItem(id).first()!!

        clock.advance()
        repo.setStatus(id, MediaStatus.COMPLETED)
        repo.setRating(id, 4f)
        repo.revertCompletion(id, before)

        val after = repo.observeItem(id).first()!!
        assertEquals(MediaStatus.IN_PROGRESS, after.status)
        assertEquals(120, after.progressValue)
        assertTrue(after.isPinned)
        assertNull(after.completedAt)
        assertEquals(before.startedAt, after.startedAt)
        assertEquals(4f, after.userRating!!, 0f)
    }

    @Test
    fun `adding directly as completed sets completion timestamp`() = runTest {
        val repo = repository()
        val id = repo.addFromSearch(dune, MediaStatus.COMPLETED)
        val item = repo.observeItem(id).first()!!
        assertEquals(MediaStatus.COMPLETED, item.status)
        assertNotNull(item.completedAt)
    }

    @Test
    fun `rating is rounded to half stars and progress is clamped to length`() = runTest {
        val repo = repository()
        val id = repo.addFromSearch(dune) // 612 sayfa

        repo.setRating(id, 3.7f)
        repo.setProgress(id, 10_000)
        val item = repo.observeItem(id).first()!!
        assertEquals(3.5f, item.userRating!!, 0f)
        assertEquals(612, item.progressValue)

        repo.setRating(id, null)
        assertNull(repo.observeItem(id).first()!!.userRating)
    }

    @Test
    fun `currently unwinding contains in-progress and pinned items`() = runTest {
        val repo = repository()
        val a = repo.addFromSearch(inception, MediaStatus.IN_PROGRESS)
        val b = repo.addFromSearch(dune)
        repo.setPinned(b, true)
        repo.addFromSearch(MockCatalog.items.first { it.title == "Hades" })

        val ids = repo.observeCurrentlyUnwinding().first().map { it.id }
        assertEquals(listOf(b, a), ids) // sabitlenenler önce
    }

    @Test
    fun `full text search finds by title prefix, subtitle and genre`() = runTest {
        val repo = repository()
        repo.addFromSearch(inception)
        repo.addFromSearch(dune)

        assertEquals(listOf("Inception"), repo.searchLocal("incep").map { it.title })
        assertEquals(listOf("Dune"), repo.searchLocal("herbert").map { it.title })
        assertEquals(2, repo.searchLocal("bilim").size)
        assertTrue(repo.searchLocal("\" OR *").isEmpty())

        // ASCII dışı harflerde büyük/küçük harf duyarsızlığı (unicode61 tokenizer).
        repo.addFromSearch(MockCatalog.items.first { it.title == "Kürk Mantolu Madonna" })
        assertEquals(listOf("Kürk Mantolu Madonna"), repo.searchLocal("KÜRK").map { it.title })
    }

    @Test
    fun `soft delete hides the item and restore brings it back`() = runTest {
        val repo = repository()
        val id = repo.addFromSearch(inception)
        repo.setRating(id, 4f)

        repo.delete(id)
        assertNull(repo.observeItem(id).first())
        assertTrue(repo.observeLibrary().first().isEmpty())
        assertTrue(repo.searchLocal("inception").isEmpty())
        assertFalse(repo.observeSavedIds().first().containsKey(inception.externalId))

        repo.restore(id)
        assertEquals(4f, repo.observeItem(id).first()!!.userRating!!, 0f)
    }

    @Test
    fun `re-adding a deleted item resurrects the same row with fresh user data`() = runTest {
        val repo = repository()
        val id = repo.addFromSearch(inception)
        repo.setRating(id, 5f)
        repo.delete(id)

        val again = repo.addFromSearch(inception)
        assertEquals(id, again)
        val item = repo.observeItem(again).first()!!
        assertNull(item.userRating)
        assertEquals(MediaStatus.BACKLOG, item.status)
    }

    @Test
    fun `mutations bump updatedAt for sync`() = runTest {
        val repo = repository()
        val id = repo.addFromSearch(inception)
        val before = repo.observeItem(id).first()!!.updatedAt

        clock.advance(5_000)
        repo.setNote(id, "  Cuma akşamı  ")
        val after = repo.observeItem(id).first()!!
        assertEquals("Cuma akşamı", after.userNote)
        assertEquals(before + 5.seconds, after.updatedAt)
    }

    @Test
    fun `details enrichment keeps user changes made before it completes`() = runTest {
        val repo = repository()
        val partial = MediaSearchResult(
            externalId = inception.externalId,
            mediaType = MediaType.MOVIE,
            title = "Inception",
        )
        val id = repo.addFromSearch(partial)
        repo.setRating(id, 4.5f)
        advanceUntilIdle()

        val item = repo.observeItem(id).first()!!
        assertEquals(4.5f, item.userRating!!, 0f)
        assertEquals(148, item.lengthValue) // Mock detayından geldi
        assertEquals(listOf("Christopher Nolan"), item.metadata.creators)
    }
}
