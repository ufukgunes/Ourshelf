package com.ourshelf.app.data.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ColorExtractorTest {

    private fun argb(r: Int, g: Int, b: Int) = ColorExtractor.argb(r, g, b)

    @Test
    fun dominantIsMostPopulousColorAndAccentIsMostVibrant() {
        val darkNavy = argb(20, 24, 48)
        val vividOrange = argb(240, 120, 20)
        val pixels = IntArray(900) { darkNavy } + IntArray(100) { vividOrange }

        val colors = ColorExtractor.extract(pixels)

        assertEquals(darkNavy, colors.dominant)
        assertEquals(vividOrange, colors.accent)
    }

    @Test
    fun grayscaleImageHasNoAccent() {
        val pixels = IntArray(500) { argb(128, 128, 128) } + IntArray(500) { argb(30, 30, 30) }
        val colors = ColorExtractor.extract(pixels)
        assertNotNull(colors.dominant)
        assertNull(colors.accent)
    }

    @Test
    fun rareVividSpecksAreIgnoredForAccent() {
        // %0.5'lik canlı kırmızı benek, %1 eşiğinin altında kalır.
        val pixels = IntArray(995) { argb(40, 60, 40) } + IntArray(5) { argb(255, 0, 0) }
        assertNull(ColorExtractor.extract(pixels).accent)
    }

    @Test
    fun transparentPixelsAreIgnored() {
        val transparentRed = (0x00 shl 24) or (255 shl 16)
        val colors = ColorExtractor.extract(IntArray(100) { transparentRed })
        assertNull(colors.dominant)
        assertNull(colors.accent)
    }

    @Test
    fun saturationValueMatchesHsv() {
        val (s, v) = ColorExtractor.saturationValue(argb(255, 0, 0))
        assertEquals(1.0, s)
        assertEquals(1.0, v)
        val (gs, _) = ColorExtractor.saturationValue(argb(100, 100, 100))
        assertTrue(gs == 0.0)
    }
}
