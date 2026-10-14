package hr.papir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDateTime

class PovijestTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun najnovijeJePrvo() {
        val p = Povijest(tmp.root)
        p.spremi("k", "prva\nverzija", 1000)
        p.spremi("k", "druga", 2000)
        val popis = p.popis("k")
        assertEquals(listOf("druga", "prva"), popis.map { it.pregled })
        assertEquals("prva\nverzija", p.procitaj(popis[1]))
    }

    @Test
    fun istaVerzijaSeNeSpremaDvaput() {
        val p = Povijest(tmp.root)
        p.spremi("k", "isto", 1000)
        p.spremi("k", "isto", 2000)
        assertEquals(1, p.popis("k").size)
    }

    @Test
    fun prazanTekstSeNeSprema() {
        val p = Povijest(tmp.root)
        p.spremi("k", "", 1000)
        assertTrue(p.popis("k").isEmpty())
    }

    @Test
    fun cuvaSeSamoZadnjihNekoliko() {
        val p = Povijest(tmp.root, limit = 3)
        for (i in 1..5) p.spremi("k", "verzija $i", i * 1000L)
        assertEquals(listOf("verzija 5", "verzija 4", "verzija 3"), p.popis("k").map { it.pregled })
    }

    @Test
    fun istoVrijemeNePrepisuje() {
        val p = Povijest(tmp.root)
        p.spremi("k", "a", 1000)
        p.spremi("k", "b", 1000)
        assertEquals(2, p.popis("k").size)
    }

    @Test
    fun povijestIdeSaPreimenovanom() {
        val p = Povijest(tmp.root)
        p.spremi("staro", "tekst", 1000)
        p.preseli("staro", "novo")
        assertTrue(p.popis("staro").isEmpty())
        assertEquals("tekst", p.procitaj(p.popis("novo").single()))
    }

    @Test
    fun kljucJeStabilanISiguran() {
        val k = Povijest.kljuc("content://x/document/primary%3ADocuments%2Fa.txt")
        assertEquals(k, Povijest.kljuc("content://x/document/primary%3ADocuments%2Fa.txt"))
        assertNotEquals(k, Povijest.kljuc("content://x/document/primary%3ADocuments%2Fb.txt"))
        assertTrue(k.matches(Regex("[0-9a-f]{40}")))
    }

    @Test
    fun imeNovogPapira() {
        assertEquals("2026-10-14 21.04.txt", PapirFile.newPaperName(LocalDateTime.of(2026, 10, 14, 21, 4)))
    }
}
