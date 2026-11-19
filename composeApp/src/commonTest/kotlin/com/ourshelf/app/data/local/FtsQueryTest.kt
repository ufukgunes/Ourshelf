package com.ourshelf.app.data.local

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.Test

class FtsQueryTest {
    @Test
    fun eachTokenIsQuotedAndPrefixMatched() {
        assertEquals("\"incep*\" \"nol*\"", FtsQuery.build("incep nol"))
    }

    @Test
    fun ftsOperatorsAndQuotesAreNeutralised() {
        assertEquals("\"dune*\" \"OR*\" \"x*\"", FtsQuery.build("dune\" OR x*"))
    }

    @Test
    fun unicodeLettersAreKept() {
        assertEquals("\"kürk*\" \"şair*\"", FtsQuery.build("  kürk-şair "))
    }

    @Test
    fun blankOrSymbolOnlyInputReturnsNull() {
        assertNull(FtsQuery.build("   "))
        assertNull(FtsQuery.build("*\"-()"))
    }
}
