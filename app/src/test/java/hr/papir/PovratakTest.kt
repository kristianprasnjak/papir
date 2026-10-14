package hr.papir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PovratakTest {

    private fun s(t: String) = Stanje(t, t.length, t.length)

    /** Upiše tekst slovo po slovo, s danim razmakom u milisekundama. */
    private fun tipkaj(p: Povratak, od: String, upis: String, pocetak: Long, korak: Long): String {
        var tekst = od
        var t = pocetak
        for (c in upis) {
            val novi = tekst + c
            p.prijeIzmjene(s(tekst), t, Povratak.jeMalaIzmjena(tekst, novi))
            tekst = novi
            t += korak
        }
        return tekst
    }

    @Test
    fun brzoTipkanjeSePonistavaOdjednom() {
        val p = Povratak()
        val tekst = tipkaj(p, "", "mlijeko", 0, 100)
        assertEquals("", p.ponisti(s(tekst))!!.text)
        assertFalse(p.moguPonistiti)
    }

    @Test
    fun stankaOdvajaKorake() {
        val p = Povratak()
        var tekst = tipkaj(p, "", "kruh", 0, 100)
        tekst = tipkaj(p, tekst, " i mlijeko", 5000, 100)
        assertEquals("kruh", p.ponisti(s(tekst))!!.text)
        assertEquals("", p.ponisti(s("kruh"))!!.text)
    }

    @Test
    fun noviRedakJeZasebanKorak() {
        val p = Povratak()
        val tekst = tipkaj(p, "", "kruh\nmlijeko", 0, 50)
        assertEquals("kruh\n", p.ponisti(s(tekst))!!.text)
        assertEquals("kruh", p.ponisti(s("kruh\n"))!!.text)
        assertEquals("", p.ponisti(s("kruh"))!!.text)
    }

    @Test
    fun velikaIzmjenaJeZasebanKorak() {
        val p = Povratak()
        val tekst = tipkaj(p, "", "ab", 0, 10)
        p.prijeIzmjene(s(tekst), 20, Povratak.jeMalaIzmjena(tekst, "ab 09:05 14.10.2026."))
        assertEquals("ab", p.ponisti(s("ab 09:05 14.10.2026."))!!.text)
    }

    @Test
    fun ponoviVracaPonisteno() {
        val p = Povratak()
        val tekst = tipkaj(p, "", "abc", 0, 10)
        val natrag = p.ponisti(s(tekst))!!
        assertTrue(p.moguPonoviti)
        assertEquals("abc", p.ponovi(natrag)!!.text)
        assertFalse(p.moguPonoviti)
    }

    @Test
    fun novaIzmjenaBriseMoguceVracanje() {
        val p = Povratak()
        val tekst = tipkaj(p, "", "abc", 0, 10)
        p.ponisti(s(tekst))
        tipkaj(p, "", "x", 9000, 10)
        assertFalse(p.moguPonoviti)
        assertNull(p.ponovi(s("x")))
    }

    @Test
    fun ograniceniBrojKoraka() {
        val p = Povratak(limit = 3)
        var tekst = ""
        for (i in 0 until 10) tekst = tipkaj(p, tekst, "$i", i * 10_000L, 10)
        var koraka = 0
        var t = s(tekst)
        while (true) {
            t = p.ponisti(t) ?: break
            koraka++
        }
        assertEquals(3, koraka)
    }

    @Test
    fun malaIzmjena() {
        assertTrue(Povratak.jeMalaIzmjena("ab", "abc"))
        assertTrue(Povratak.jeMalaIzmjena("abc", "ab"))
        assertTrue(Povratak.jeMalaIzmjena("a\nb", "ab"))
        assertFalse(Povratak.jeMalaIzmjena("ab", "ab\n"))
        assertFalse(Povratak.jeMalaIzmjena("ab", "abcd"))
        assertFalse(Povratak.jeMalaIzmjena("abcd", ""))
    }
}
