package hr.papir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OblikTest {

    /** Ogledna datoteka iz primjeri/, pročitana točno kao što je čita aplikacija. */
    private fun primjer(ime: String): String {
        val f = listOf(File("../primjeri/$ime"), File("primjeri/$ime")).first { it.exists() }
        return PapirFile.decode(f.readBytes())!!.text
    }

    private fun stilOd(p: Oblik.Procitano, rijec: String): Stil {
        val i = p.tekst.indexOf(rijec)
        assertTrue("nema „$rijec“ u „${p.tekst}“", i >= 0)
        val stilovi = p.stilovi.subList(i, i + rijec.length).toSet()
        assertEquals("„$rijec“ nije jednoliko oblikovan", 1, stilovi.size)
        return stilovi.single()
    }

    private val B = Stil(podebljano = true)
    private val I = Stil(kurziv = true)

    @Test
    fun primjerOblikovanje() {
        val p = Oblik.citaj(primjer("oblikovanje.txt"))
        assertEquals(
            "Namirnice\nKupiti ODLIČNO mlijeko\nIvica je rekao 2+2=5\nNe zaboraviti kruh\nsitna napomena",
            p.tekst,
        )
        assertEquals(Stil(velicina = 2), stilOd(p, "Namirnice"))
        assertEquals(Stil.OBICNO, stilOd(p, "Kupiti "))
        assertEquals(B, stilOd(p, "ODLIČNO"))
        assertEquals(Stil.OBICNO, stilOd(p, " mlijeko"))
        assertEquals(I, stilOd(p, "2+2=5"))
        assertEquals(Stil(true, true, 1), stilOd(p, "Ne zaboraviti kruh"))
        assertEquals(Stil(velicina = -1), stilOd(p, "sitna napomena"))
    }

    @Test
    fun zapisJeIstiKaoOglednaDatoteka() {
        val sirovo = primjer("oblikovanje.txt")
        val p = Oblik.citaj(sirovo)
        assertEquals(sirovo, Oblik.zapisi(p.tekst, p.stilovi).sirovo)
    }

    @Test
    fun bezKljucaJeObicanTekst() {
        val sirovo = primjer("bez-kljuca.txt")
        val p = Oblik.citaj(sirovo)
        assertEquals(sirovo, p.tekst)
        assertTrue(p.stilovi.all { it.obicno })
        assertEquals(sirovo, Oblik.zapisi(p.tekst, p.stilovi).sirovo)
    }

    @Test
    fun pokvareneOznakeSuTekst() {
        val p = Oblik.citaj(primjer("pokvarena-oznaka.txt"))
        assertEquals("Kupiti {*ODLIČNO mlijeko\nIvica je rekao {/*}2+2=5\nTemperatura -5°C", p.tekst)
        assertEquals(Stil.OBICNO, stilOd(p, "{/*}2+2=5"))
        assertEquals(Stil(velicina = -1), stilOd(p, "-5°C"))
    }

    @Test
    fun nepoznatKljucZnaciObicanTekst() {
        val sirovo = primjer("nepoznat-kljuc.txt")
        assertEquals(sirovo, Oblik.citaj(sirovo).tekst)
    }

    @Test
    fun kljuc() {
        assertEquals("a", Oblik.tijelo("a\n\n{papir 1}"))
        assertEquals("a", Oblik.tijelo("a\n{papir 1}"))
        assertEquals("a", Oblik.tijelo("a\n\n{papir 1}\n"))
        assertEquals("a\n", Oblik.tijelo("a\n\n\n{papir 1}"))
        assertEquals("", Oblik.tijelo("{papir 1}"))
        assertNull(Oblik.tijelo("a\n\n{papir1}"))
        assertNull(Oblik.tijelo("a\n\n{Papir 1}"))
        assertNull(Oblik.tijelo("a {papir 1}"))
        assertNull(Oblik.tijelo("{papir 1}\nb"))
    }

    @Test
    fun oznake() {
        assertEquals(Stil.OBICNO, Stil.izOznake("="))
        assertEquals(Stil(true, true, 2), Stil.izOznake("*/++"))
        assertEquals(Stil(velicina = -2), Stil.izOznake("--"))
        assertNull(Stil.izOznake(""))
        assertNull(Stil.izOznake("/*"))
        assertNull(Stil.izOznake("+-"))
        assertNull(Stil.izOznake("+++"))
        assertNull(Stil.izOznake(" * "))
        assertNull(Stil.izOznake("**"))
        listOf(B, I, Stil(true, true, 1), Stil(velicina = -2), Stil.OBICNO).forEach { s ->
            val o = s.oznaka()
            assertEquals(s, Stil.izOznake(o.substring(1, o.length - 1)))
        }
    }

    @Test
    fun praznaZagradaNijeOznaka() {
        val p = Oblik.citaj("if (x) {} {*}da\n\n{papir 1}")
        assertEquals("if (x) {} da", p.tekst)
        assertEquals(B, stilOd(p, "da"))
    }

    @Test
    fun goliTekstOstajeGoli() {
        val t = "Ivica: 2+2=5\nwww.nesto.hr {*} ♠"
        val z = Oblik.zapisi(t, List(t.length) { Stil.OBICNO })
        assertEquals(t, z.sirovo)
    }

    @Test
    fun pisacNePiseSuvisneOznake() {
        val t = "ab cd\nef"
        val st = Oblik.primijeni(t, List(t.length) { Stil.OBICNO }, 0, 2) { it.copy(podebljano = true) }
        assertEquals("{*}ab{=} cd\nef\n\n{papir 1}", Oblik.zapisi(t, st).sirovo)
        // Oblikovanje do kraja retka: bez {=} na kraju; novi redak ponovno s oznakom.
        val st2 = Oblik.primijeni(t, List(t.length) { Stil.OBICNO }, 3, 8) { it.copy(kurziv = true) }
        assertEquals("ab {/}cd\n{/}ef\n\n{papir 1}", Oblik.zapisi(t, st2).sirovo)
    }

    @Test
    fun tipkanjeNastavljaStil() {
        val t = "ab"
        val st = listOf(B, B)
        assertEquals(listOf(B, B, B), Oblik.prilagodi(t, st, "abc"))
    }

    @Test
    fun noviRedakPocinjeObicno() {
        val st = Oblik.prilagodi("ab", listOf(B, B), "ab\n")
        assertEquals(listOf(B, B, Stil.OBICNO), st)
        assertEquals(listOf(B, B, Stil.OBICNO, Stil.OBICNO), Oblik.prilagodi("ab\n", st, "ab\nc"))
    }

    @Test
    fun umetanjeNaPocetkuRetkaNasljedujeSljedeci() {
        val xl = Stil(velicina = 2)
        assertEquals(listOf(xl, xl, xl), Oblik.prilagodi("ab", listOf(xl, xl), "Xab"))
    }

    @Test
    fun zadaniStilZaUmetanje() {
        assertEquals(listOf(Stil.OBICNO, I), Oblik.prilagodi("a", listOf(Stil.OBICNO), "ab", I))
    }

    @Test
    fun brisanjeZadrzavaOstatak() {
        assertEquals(listOf(B, I), Oblik.prilagodi("axb", listOf(B, Stil.OBICNO, I), "ab"))
    }

    @Test
    fun rukomUpisanaOznakaUDatoteciSKljucem() {
        // Tekst s oblikovanjem u kojem je korisnik napisao „{/}" pretvara se u oznaku.
        val t = "a {/}b"
        val st = listOf(B) + List(t.length - 1) { Stil.OBICNO }
        val z = Oblik.zapisi(t, st)
        val p = Oblik.citaj(z.sirovo)
        assertEquals("a b", p.tekst)
        assertEquals(I, stilOd(p, "b"))
    }

    @Test
    fun pozicijeSePreslikavaju() {
        val sirovo = "a {*}bc\n\n{papir 1}"
        val p = Oblik.citaj(sirovo)
        assertEquals("a bc", p.tekst)
        assertEquals(2, p.izSirovog[2]) // ispred oznake
        assertEquals(2, p.izSirovog[5]) // iza oznake
        assertEquals(4, p.izSirovog[sirovo.length])
        val z = Oblik.zapisi(p.tekst, p.stilovi)
        assertEquals(sirovo, z.sirovo)
        assertEquals(5, z.uSirovo[2])
        assertEquals(7, z.uSirovo[4])
    }

    @Test
    fun dijeloviZaPrikaz() {
        val d = Oblik.dijelovi(listOf(Stil.OBICNO, B, B, I, Stil.OBICNO))
        assertEquals(listOf(Triple(1, 3, B), Triple(3, 4, I)), d)
    }
}
