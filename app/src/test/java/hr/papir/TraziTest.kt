package hr.papir

import org.junit.Assert.assertEquals
import org.junit.Test

class TraziTest {

    @Test
    fun nalaziKaoNotepad() {
        val t = "Kruh, kruh i KRUH"
        assertEquals(listOf(0..3, 6..9, 13..16), Trazi.nadji(t, "kruh", velikaMala = false))
        assertEquals(listOf(6..9), Trazi.nadji(t, "kruh", velikaMala = true))
        assertEquals(emptyList<IntRange>(), Trazi.nadji(t, "", velikaMala = false))
        assertEquals(listOf(0..1, 2..3), Trazi.nadji("aaaa", "aa", velikaMala = true))
    }

    @Test
    fun preskaceIzuzeto() {
        val t = "ab\nab\nab"
        assertEquals(listOf(0..1, 6..7), Trazi.nadji(t, "ab", true, listOf(3..4)))
    }

    @Test
    fun zamijeniSve() {
        assertEquals("pivo, pivo" to 2, Trazi.zamijeniSve("kruh, Kruh", "kruh", "pivo", velikaMala = false))
        assertEquals("XY\nab\nab" to 1, Trazi.zamijeniSve("XY\nab\nXY", "XY", "ab", true, listOf(0..1)))
        assertEquals("x" to 0, Trazi.zamijeniSve("x", "y", "z", true))
    }
}
