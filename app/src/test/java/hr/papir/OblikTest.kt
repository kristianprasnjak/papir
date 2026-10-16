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
        assertEquals("„$rijec“ nije jednoliko oblikovan: $stilovi", 1, stilovi.size)
        return stilovi.single()
    }

    private val B = Stil(podebljano = true)
    private val I = Stil(kurziv = true)
    private fun obicno(t: String) = List(t.length) { Stil.OBICNO }

    @Test
    fun primjerOblikovanje() {
        val p = Oblik.citaj(primjer("oblikovanje.txt"))
        assertEquals(
            "Palačinke\n\nSastojci\n200 g brašna, glatkog\nIvica je rekao 2+2=5\nNe zaboraviti kruh\n" +
                "može i s malo mineralne vode\njoš sitnije",
            p.tekst,
        )
        assertEquals(Stil(velicina = 2), stilOd(p, "Palačinke"))
        assertEquals(Stil(velicina = 1), stilOd(p, "Sastojci"))
        assertEquals(Stil.OBICNO, stilOd(p, "200 g brašna, "))
        assertEquals(B, stilOd(p, "glatkog"))
        assertEquals(I, stilOd(p, "2+2=5"))
        assertEquals(Stil(true, true), stilOd(p, "Ne zaboraviti kruh"))
        assertEquals(Stil(velicina = -1), stilOd(p, "može i s malo mineralne vode"))
        assertEquals(Stil(velicina = -2), stilOd(p, "još sitnije"))
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
        assertEquals(
            "Kupiti ▐ODLIČNO mlijeko\nKrižanje: a‹bc›\nPrazno: ▐▌ i ‹›\nNaslov\n────\n░░░ tri sjene\nDva puta: ▐ab",
            p.tekst,
        )
        assertEquals(Stil.OBICNO, stilOd(p, "Kupiti ▐ODLIČNO mlijeko"))
        assertEquals(B, stilOd(p, "a‹b"))
        assertEquals(Stil.OBICNO, stilOd(p, "c›"))
        assertEquals(Stil.OBICNO, stilOd(p, "Naslov"))
        assertEquals(Stil.OBICNO, stilOd(p, "░░░ tri sjene"))
        assertEquals(Stil.OBICNO, stilOd(p, "Dva puta: ▐a"))
        assertEquals(B, p.stilovi[p.tekst.length - 1])
    }

    @Test
    fun novijiKljucSeCitaIZadrzava() {
        val sirovo = primjer("noviji-kljuc.txt").replace("\r\n", "\n")
        val p = Oblik.citaj(sirovo)
        assertEquals("ODLIČNO", p.tekst)
        assertEquals(2, p.format)
        assertEquals(B, p.stilovi[0])
        // nedirano: zapisuje se doslovno, s istim brojem
        assertEquals(sirovo, Oblik.zapisi(p.tekst, p.stilovi, p.linije, p.kljuc, p.format).sirovo)
        // ni bez ijednog oblikovanja ključ novijeg formata ne nestaje: u papiru mogu biti nepoznate oznake
        val goli = Oblik.zapisi("nepoznato", List(9) { Stil.OBICNO }, kljuc = true, format = 2).sirovo
        assertEquals("nepoznato\n\n─── papir 2 ───", goli)
        // pisač nikad ne smanjuje broj
        assertEquals("▐a▌\n\n─── papir 7 ───", Oblik.zapisi("a", listOf(B), format = 7).sirovo)
        assertEquals("▐a▌\n\n─── papir 1 ───", Oblik.zapisi("a", listOf(B), format = 0).sirovo)
    }

    @Test
    fun nediraniRetciOstajuDoslovnoIsti() {
        val sirovo = primjer("rucno-pisano.txt")
        val p = Oblik.citaj(sirovo)
        assertEquals("Prvi redak\nDrugi redak\nsitno", p.tekst)
        val novi = p.tekst.replace("Drugi redak", "Drugi redak!")
        val st = Oblik.prilagodi(p.tekst, p.stilovi, novi)
        val z = Oblik.zapisi(novi, st, p.linije)
        assertEquals("▐Prvi▌▐ redak▌\nDrugi redak!\n░ ▐sitno▌\n\n─── papir 1 ───", z.sirovo)
        // bez izvora pisač bi prvi redak napisao u najkraćem obliku
        assertTrue(Oblik.zapisi(novi, st).sirovo.startsWith("▐Prvi redak▌\n"))
    }

    @Test
    fun kljuc() {
        val k = Oblik.KLJUC
        assertEquals("a", Oblik.tijelo("a\n\n$k"))
        assertEquals("a", Oblik.tijelo("a\n$k"))
        assertEquals("a", Oblik.tijelo("a\n\n$k\n"))
        assertEquals("a\n", Oblik.tijelo("a\n\n\n$k"))
        assertEquals("", Oblik.tijelo(k))
        assertEquals("a", Oblik.tijelo("a\n\n─── papir 2 ───"))
        assertEquals(1, Oblik.format("a\n\n$k"))
        assertEquals(12, Oblik.format("a\n\n─── papir 12 ───\n"))
        assertEquals(0, Oblik.format("a"))
        assertNull(Oblik.tijelo("a\n\n─── papir 0 ───"))
        assertNull(Oblik.tijelo("a\n\n─── papir 01 ───"))
        assertNull(Oblik.tijelo("a\n\n─── papir -1 ───"))
        assertNull(Oblik.tijelo("a\n\n─── papir  1 ───"))
        assertNull(Oblik.tijelo("a\n\n─── papir 1a ───"))
        assertNull(Oblik.tijelo("a\n\n─── Papir 1 ───"))
        assertNull(Oblik.tijelo("a\n\n--- papir 1 ---"))
        assertNull(Oblik.tijelo("a $k"))
        assertNull(Oblik.tijelo("$k\nb"))
        assertNull(Oblik.tijelo("a\n\n{papir 1}"))
    }

    @Test
    fun naslovTrebaCrtuTocneDuljine() {
        val k = Oblik.KLJUC
        assertEquals(Stil(velicina = 2), stilOd(Oblik.citaj("Čaj\n═══\n\n$k"), "Čaj"))
        assertEquals(Stil(velicina = 1), stilOd(Oblik.citaj("Čaj\n───\n\n$k"), "Čaj"))
        assertEquals("Čaj\n════", Oblik.citaj("Čaj\n════\n\n$k").tekst)
        assertEquals("Čaj\n──", Oblik.citaj("Čaj\n──\n\n$k").tekst)
        // crta broji i znakove oblikovanja, jer ih i Notepad pokazuje
        val p = Oblik.citaj("▐Čaj▌\n═════\n\n$k")
        assertEquals("Čaj", p.tekst)
        assertEquals(Stil(podebljano = true, velicina = 2), stilOd(p, "Čaj"))
        // prazan redak ne može biti naslov
        assertEquals("\n───", Oblik.citaj("\n───\n\n$k").tekst.let { "\n" + it.substringAfter("\n") })
    }

    @Test
    fun obicniZnakoviOstajuTekst() {
        val t = "* jabuke\n3*4=12\n**VAŽNO**\n16/10/2026 km/h\n-----\n(vidi gore)\n((x+1))\n{*}\n{papir 1}"
        val st = Oblik.primijeni(t, obicno(t), 0, 1) { it.copy(podebljano = false) } // i dalje sve obično
        assertEquals(t, Oblik.zapisi(t, st).sirovo) // bez oblikovanja: goli tekst, bez ključa
        // i u papiru s oblikovanjem ti znakovi ostaju tekst
        val st2 = Oblik.primijeni(t, obicno(t), 2, 8) { it.copy(podebljano = true) }
        val p = Oblik.citaj(Oblik.zapisi(t, st2).sirovo)
        assertEquals(t, p.tekst)
        assertEquals(st2, p.stilovi)
    }

    @Test
    fun pisacGnijezdiIneKriza() {
        val t = "abcd"
        // kurziv a–c, podebljano b–d: križaju se, pa pisač zatvara i ponovno otvara
        val st = listOf(I, Stil(true, true), Stil(true, true), B)
        val z = Oblik.zapisi(t, st).sirovo
        assertEquals("‹a▐bc▌›▐d▌\n\n${Oblik.KLJUC}", z)
        val p = Oblik.citaj(z)
        assertEquals(t, p.tekst)
        assertEquals(st, p.stilovi)
    }

    @Test
    fun velicinaJeSvojstvoRetka() {
        val t = "Naslov\ntekst"
        val st = Oblik.primijeni(t, obicno(t), 0, 6) { it.copy(velicina = 2) }
        assertEquals("Naslov\n══════\ntekst\n\n${Oblik.KLJUC}", Oblik.zapisi(t, st).sirovo)
        val st2 = Oblik.primijeni(t, obicno(t), 7, 12) { it.copy(velicina = -1) }
        assertEquals("Naslov\n░ tekst\n\n${Oblik.KLJUC}", Oblik.zapisi(t, st2).sirovo)
    }

    @Test
    fun goliTekstOstajeGoli() {
        val t = "Ivica: 2+2=5\nwww.nesto.hr ▐ ‹ ░ ═"
        assertEquals(t, Oblik.zapisi(t, obicno(t)).sirovo)
    }

    @Test
    fun tipkanjeNastavljaStil() {
        assertEquals(listOf(B, B, B), Oblik.prilagodi("ab", listOf(B, B), "abc"))
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
    fun pozicijeSePreslikavaju() {
        val k = Oblik.KLJUC
        val sirovo = "a ▐bc▌ d\n\n$k"
        val p = Oblik.citaj(sirovo)
        assertEquals("a bc d", p.tekst)
        assertEquals(2, p.izSirovog[2]) // ispred oznake
        assertEquals(2, p.izSirovog[3]) // iza oznake
        assertEquals(4, p.izSirovog[6]) // iza zatvaranja
        val z = Oblik.zapisi(p.tekst, p.stilovi)
        assertEquals(sirovo, z.sirovo)
        assertEquals(3, z.uSirovo[2])
        assertEquals(7, z.uSirovo[5])
    }

    @Test
    fun dijeloviZaPrikaz() {
        val d = Oblik.dijelovi(listOf(Stil.OBICNO, B, B, I, Stil.OBICNO))
        assertEquals(listOf(Triple(1, 3, B), Triple(3, 4, I)), d)
    }
}
