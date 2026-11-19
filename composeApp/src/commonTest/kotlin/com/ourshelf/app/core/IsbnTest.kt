package com.ourshelf.app.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IsbnTest {

    @Test
    fun validBookBarcodeIsAccepted() {
        assertEquals("9780441172719", Isbn.fromBarcode("9780441172719")) // Dune
        assertEquals("9789750719387", Isbn.fromBarcode("978-975-07-1938-7"))
    }

    @Test
    fun wrongChecksumOrNonBookBarcodeIsRejected() {
        assertNull(Isbn.fromBarcode("9780441172718")) // sağlama hatası
        assertNull(Isbn.fromBarcode("5901234123457")) // geçerli EAN-13 ama kitap değil (978/979 değil)
        assertNull(Isbn.fromBarcode("12345"))
    }

    @Test
    fun typedIsbnIsRecognisedInBothFormats() {
        assertTrue(Isbn.looksLikeIsbn("978-0-441-17271-9"))
        assertTrue(Isbn.looksLikeIsbn("0441172717"))
        assertTrue(Isbn.looksLikeIsbn("080442957X"))
        assertFalse(Isbn.looksLikeIsbn("dune"))
        assertFalse(Isbn.looksLikeIsbn("2049"))
    }
}
