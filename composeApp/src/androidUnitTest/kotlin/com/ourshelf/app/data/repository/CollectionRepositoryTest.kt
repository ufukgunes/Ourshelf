package com.ourshelf.app.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ourshelf.app.data.local.OurshelfDatabase
import com.ourshelf.app.data.remote.mock.MockCatalog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectionRepositoryTest {

    private lateinit var db: OurshelfDatabase
    private val clock = MutableClock()

    @Before
    fun setUp() {
        db = inMemoryDatabase()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `shelves keep insertion order and include empty collections`() = runTest {
        val media = MediaRepository(db, mockRegistry(), FakePosterProcessor(), clock, backgroundScope)
        val collections = CollectionRepository(db, clock)

        val rainy = collections.create("Yağmurlu Hafta Sonu", "🌧️")
        val empty = collections.create("PS5 Sıradakiler", "")
        val a = media.addFromSearch(MockCatalog.items.first { it.title == "Amélie" })
        val b = media.addFromSearch(MockCatalog.items.first { it.title == "Arrival" })
        collections.addItem(rainy, b)
        collections.addItem(rainy, a)
        collections.addItem(rainy, a) // tekrar eklemek çoğaltmaz

        val shelves = collections.observeShelves().first()
        assertEquals(listOf(rainy, empty), shelves.map { it.collection.id })
        assertEquals(listOf(b, a), shelves[0].items.map { it.id })
        assertTrue(shelves[1].items.isEmpty())
        assertEquals(CollectionRepository.DEFAULT_EMOJI, shelves[1].collection.iconEmoji)
        assertEquals(setOf(rainy), collections.observeCollectionIdsForItem(a).first())
    }

    @Test
    fun `removing links, deleting items and deleting collections hide them`() = runTest {
        val media = MediaRepository(db, mockRegistry(), FakePosterProcessor(), clock, backgroundScope)
        val collections = CollectionRepository(db, clock)

        val shelf = collections.create("Komediler", "😂")
        val a = media.addFromSearch(MockCatalog.items.first { it.title == "Amélie" })
        val b = media.addFromSearch(MockCatalog.items.first { it.title == "The Grand Budapest Hotel" })
        collections.addItem(shelf, a)
        collections.addItem(shelf, b)

        collections.removeItem(shelf, a)
        media.delete(b)
        assertTrue(collections.observeShelves().first().single().items.isEmpty())

        // Bağlantı geri eklenebilir (tombstone diriltilir).
        collections.addItem(shelf, a)
        assertEquals(listOf(a), collections.observeShelves().first().single().items.map { it.id })

        collections.delete(shelf)
        assertTrue(collections.observeShelves().first().isEmpty())
    }

    @Test
    fun `reordering items and collections persists new order`() = runTest {
        val media = MediaRepository(db, mockRegistry(), FakePosterProcessor(), clock, backgroundScope)
        val collections = CollectionRepository(db, clock)

        val first = collections.create("A", "🅰️")
        val second = collections.create("B", "🅱️")
        val ids = listOf("Inception", "Arrival", "Coco").map { title ->
            media.addFromSearch(MockCatalog.items.first { it.title == title }).also { collections.addItem(first, it) }
        }

        collections.reorderItems(first, listOf(ids[2], ids[0], ids[1]))
        collections.reorderCollections(listOf(second, first))

        val shelves = collections.observeShelves().first()
        assertEquals(listOf(second, first), shelves.map { it.collection.id })
        assertEquals(listOf(ids[2], ids[0], ids[1]), shelves[1].items.map { it.id })
        assertEquals(shelves[1], collections.observeShelf(first).first())
    }

    @Test
    fun `rename keeps emoji when blank`() = runTest {
        val collections = CollectionRepository(db, clock)
        val id = collections.create("Okuma", "📖")
        collections.rename(id, "  2026 Okuma Listem ", "")

        val c = collections.observeCollections().first().single()
        assertEquals("2026 Okuma Listem", c.name)
        assertEquals("📖", c.iconEmoji)
    }
}
