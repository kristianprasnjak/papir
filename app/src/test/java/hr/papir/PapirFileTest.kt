package hr.papir

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PapirFileTest {

    private fun roundTrip(bytes: ByteArray): ByteArray {
        val d = PapirFile.decode(bytes)!!
        return PapirFile.encode(d.text, d.lineEnding, d.hasBom)
    }

    @Test
    fun crlfOstajeCrlf() {
        val b = "mlijeko\r\nkruh\r\n".toByteArray()
        assertArrayEquals(b, roundTrip(b))
    }

    @Test
    fun lfOstajeLf() {
        val b = "mlijeko\nkruh\n".toByteArray()
        assertArrayEquals(b, roundTrip(b))
    }

    @Test
    fun crOstajeCr() {
        val b = "mlijeko\rkruh".toByteArray()
        assertArrayEquals(b, roundTrip(b))
    }

    @Test
    fun bomSeCuva() {
        val b = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "čćžšđ".toByteArray()
        val d = PapirFile.decode(b)!!
        assertTrue(d.hasBom)
        assertEquals("čćžšđ", d.text)
        assertArrayEquals(b, roundTrip(b))
    }

    @Test
    fun novaDatotekaJeCrlfBezBoma() {
        val out = PapirFile.encode("a\nb", LineEnding.CRLF, false)
        assertArrayEquals("a\r\nb".toByteArray(), out)
    }

    @Test
    fun bezKrajaRetkaZnaciCrlf() {
        assertEquals(LineEnding.CRLF, PapirFile.decode("samo jedan redak".toByteArray())!!.lineEnding)
    }

    @Test
    fun neispravanUtf8NijeTekst() {
        assertNull(PapirFile.decode(byteArrayOf(0x41, 0xC3.toByte(), 0x28, 0x42)))
    }

    @Test
    fun tekstSeNeDira() {
        val s = "Ivica je rekao učiteljici 2+2=5 ♠ www.nesto.hr"
        assertEquals(s, PapirFile.decode(s.toByteArray())!!.text)
    }

    @Test
    fun imeZaSukob() {
        assertEquals("Note (sukob).txt", PapirFile.conflictName("Note.txt"))
        assertEquals("Note (sukob).pap", PapirFile.conflictName(PapirFile.NEW_NAME))
        assertEquals("index (sukob).html", PapirFile.conflictName("index.html"))
        assertEquals("popis (sukob)", PapirFile.conflictName("popis"))
        assertEquals(".bashrc (sukob)", PapirFile.conflictName(".bashrc"))
    }

    @Test
    fun prijedlogImena() {
        assertEquals("Namirnice.pap", PapirFile.suggestName("\n  Namirnice  \nmlijeko"))
        assertEquals("Note.pap", PapirFile.suggestName(""))
        assertEquals("ab.pap", PapirFile.suggestName("a/b"))
    }
}
