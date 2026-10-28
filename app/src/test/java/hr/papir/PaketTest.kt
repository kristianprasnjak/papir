package hr.papir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaketTest {

    @Test
    fun zapisICitanje() {
        val slike = linkedMapOf("torta.jpg" to byteArrayOf(1, 2, 3), "macka.png" to ByteArray(1000) { it.toByte() })
        val s = Paket.Sadrzaj("Recept.pap", "Torta\r\n".toByteArray(), slike)
        val b = Paket.zapisi(s)
        assertTrue(Paket.jePaket(b))
        val p = Paket.citaj(b)!!
        assertEquals("Recept.pap", p.imeTeksta)
        assertEquals("Torta\r\n", String(p.tekst))
        assertEquals(listOf("torta.jpg", "macka.png"), p.prilozi.keys.toList())
        assertTrue(p.prilozi["macka.png"]!!.contentEquals(slike["macka.png"]!!))
        // isti sadržaj, isti bajtovi: nema lažnih promjena ni sukoba
        assertTrue(b.contentEquals(Paket.zapisi(p)))
    }

    @Test
    fun obicanTekstNijePaket() {
        assertTrue(!Paket.jePaket("PK".toByteArray()))
        assertNull(Paket.citaj("nije zip".toByteArray()))
        // zip bez papira ili s dva papira nije paket
        assertNull(Paket.citaj(Paket.zapisi(Paket.Sadrzaj("a.jpg", byteArrayOf(1), linkedMapOf()))))
        assertNull(Paket.citaj(Paket.zapisi(Paket.Sadrzaj("a.pap", byteArrayOf(1), linkedMapOf("b.pap" to byteArrayOf(2))))))
    }

    @Test
    fun slobodnoIme() {
        assertEquals("torta.jpg", Paket.slobodnoIme("torta.jpg", setOf()))
        assertEquals("torta (1).jpg", Paket.slobodnoIme("torta.jpg", setOf("torta.jpg")))
        assertEquals("torta (2).jpg", Paket.slobodnoIme("torta.jpg", setOf("torta.jpg", "torta (1).jpg")))
        assertEquals("slika", Paket.slobodnoIme("  ", setOf()))
        assertEquals("ab.jpg", Paket.slobodnoIme("a/b.jpg", setOf()))
    }
}
