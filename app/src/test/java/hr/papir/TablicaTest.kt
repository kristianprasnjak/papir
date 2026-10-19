package hr.papir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Random

class TablicaTest {

    private fun primjer(ime: String): String {
        val f = listOf(File("../primjeri/$ime"), File("primjeri/$ime")).first { it.exists() }
        return PapirFile.decode(f.readBytes())!!.text
    }

    private val trening = listOf(
        "┌───────────────┬────────┬─────┐",
        "│ Vježba        │ Serije │ Kg  │",
        "╞═══════════════╪════════╪═════╡",
        "│ Čučanj        │ 4x8    │ 80  │",
        "├───────────────┼────────┼─────┤",
        "│ Mrtvo dizanje │ 3x5    │ 100 │",
        "│               │ 3x3    │ 110 │",
        "└───────────────┴────────┴─────┘",
    )

    @Test
    fun citaTablicu() {
        val t = Tablice.citaj(trening)!!
        assertEquals(listOf(13, 6, 3), t.sirine)
        assertTrue(t.zaglavlje)
        assertEquals(listOf("Vježba", "Serije", "Kg"), t.retci[0])
        assertEquals(listOf("Čučanj", "4x8", "80"), t.retci[1])
        assertEquals(listOf("Mrtvo dizanje", "3x5\n3x3", "100\n110"), t.retci[2])
    }

    @Test
    fun zapisJeIstiKaoIzvornik() {
        assertEquals(trening, Tablice.zapisi(Tablice.citaj(trening)!!))
    }

    @Test
    fun stupacSeSiriUCijelojTablici() {
        val t = Tablice.citaj(trening)!!.sa(1, 1, "4x8 do otkaza")
        val z = Tablice.zapisi(t)
        assertEquals("┌───────────────┬───────────────┬─────┐", z[0])
        assertEquals("│ Čučanj        │ 4x8 do otkaza │ 80  │", z[3])
        assertEquals("│               │ 3x3           │ 110 │", z[6])
        assertEquals(listOf(13, 13, 3), Tablice.citaj(z)!!.sirine)
    }

    @Test
    fun stupacJeTocnoNajduljiRed() {
        // kraći tekst: stupac se suzi na najdulji red
        val t = Tablice.citaj(trening)!!.sa(2, 0, "Mrtvo")
        assertEquals(listOf(6, 6, 3), Tablice.citaj(Tablice.zapisi(t))!!.sirine)
        // Enter je jedini način da dugačak red postane uži
        val z = Tablice.zapisi(Tablice.citaj(trening)!!.sa(2, 0, "Mrtvo\ndizanje"))
        assertEquals("┌─────────┬────────┬─────┐", z[0])
        assertEquals("│ Mrtvo   │ 3x5    │ 100 │", z[5])
        assertEquals("│ dizanje │ 3x3    │ 110 │", z[6])
    }

    @Test
    fun enterUCelijiDodajeRed() {
        val t = Tablice.citaj(trening)!!.sa(1, 0, "Čučanj\nprednji")
        val z = Tablice.zapisi(t)
        assertEquals("│ Čučanj        │ 4x8    │ 80  │", z[3])
        assertEquals("│ prednji       │        │     │", z[4])
        assertEquals("Čučanj\nprednji", Tablice.citaj(z)!!.retci[1][0])
    }

    @Test
    fun okvirJeTablicaSJednomCelijom() {
        val okvir = listOf(
            "┌─────────────────────┐",
            "│ Ne zaboraviti:      │",
            "│ ključevi, novčanik, │",
            "│ punjač              │",
            "└─────────────────────┘",
        )
        val t = Tablice.citaj(okvir)!!
        assertEquals(listOf("Ne zaboraviti:\nključevi, novčanik,\npunjač"), t.retci.single())
        assertEquals(okvir, Tablice.zapisi(t))
    }

    @Test
    fun pomaknutZidNijeTablica() {
        val krivo = trening.toMutableList().also { it[3] = "│ Čučanj        │ 4x8   │ 80  │" }
        assertNull(Tablice.citaj(krivo))
        val bezRazmaka = trening.toMutableList().also { it[3] = "│Čučanj         │ 4x8    │ 80  │" }
        assertNull(Tablice.citaj(bezRazmaka))
        val bezDna = trening.dropLast(1) + "│               │        │     │"
        assertNull(Tablice.citaj(bezDna))
        assertTrue(Tablice.nadji(krivo).isEmpty())
    }

    @Test
    fun crtaZaglavljaSamoNaPocetku() {
        val krivo = trening.toMutableList().also { it[4] = "╞═══════════════╪════════╪═════╡" }
        assertNull(Tablice.citaj(krivo))
    }

    @Test
    fun razmaciSireStupac() {
        // razmaci koji ne šire stupac nisu ništa drugo nego dopuna do zida
        val t = Tablica.nova().sa(1, 0, "Ana  \nMarko\n\n")
        assertEquals("Ana\nMarko", Tablice.citaj(Tablice.zapisi(t))!!.retci[1][0])
        assertEquals("Ana\nMarko", Tablice.normaliziraj("Ana  \nMarko\n\n"))
        assertEquals("  uvučeno", Tablice.citaj(Tablice.zapisi(Tablica.nova().sa(1, 1, "  uvučeno")))!!.retci[1][1])
        // razmaci koji šire stupac čuvaju se kao širina, i vraćaju se na isto mjesto
        val siri = Tablice.zapisi(Tablica.nova().sa(1, 0, "Ana     "))
        assertEquals("│ Ana      │   │", siri[3])
        assertEquals("Ana     ", Tablice.citaj(siri)!!.retci[1][0])
        assertEquals(siri, Tablice.zapisi(Tablice.citaj(siri)!!))
    }

    @Test
    fun visakSirinePripadaPrvomNajduljemRedu() {
        val rukom = listOf(
            "┌──────────┐",
            "│ ab       │",
            "├──────────┤",
            "│ x        │",
            "│ cd       │",
            "└──────────┘",
        )
        val t = Tablice.citaj(rukom)!!
        assertEquals(listOf(listOf("ab      "), listOf("x\ncd")), t.retci)
        assertEquals(rukom, Tablice.zapisi(t))
        // nova tablica: prazni stupci najmanje širine, bez viška
        val nova = Tablice.zapisi(Tablica.nova())
        assertEquals("┌───┬───┐", nova[0])
        assertEquals(listOf(listOf("", ""), listOf("", "")), Tablice.citaj(nova)!!.retci)
    }

    @Test
    fun tablicaUPapiruSKljucem() {
        val p = Oblik.citaj(primjer("tablica.txt"))
        assertEquals(1, p.tablice.size)
        val linije = p.tekst.split("\n")
        val t = Tablice.citaj(linije.subList(p.tablice[0].first, p.tablice[0].last + 1))!!
        assertEquals(listOf("Vježba", "Serije", "Kg"), t.retci[0])
        assertEquals(Stil(velicina = 2), p.stilovi[0]) // naslov iznad tablice
        assertEquals(primjer("tablica.txt"), Oblik.zapisi(p.tekst, p.stilovi, p.linije, p.kljuc).sirovo)
    }

    @Test
    fun tablicaUGolomTekstuOstajeTekst() {
        val sirovo = trening.joinToString("\n")
        val p = Oblik.citaj(sirovo)
        assertTrue(p.tablice.isEmpty())
        // ni pri zapisu ne dobiva ključ sama od sebe
        assertEquals(sirovo, Oblik.zapisi(p.tekst, p.stilovi, p.linije, p.kljuc).sirovo)
        // ali kad korisnik doda tablicu iz izbornika (kljuc = true), papir dobiva ključ
        val z = Oblik.zapisi(sirovo, p.stilovi, kljuc = true).sirovo
        assertEquals(sirovo + "\n\n" + Oblik.KLJUC, z)
        assertEquals(1, Oblik.citaj(z).tablice.size)
    }

    @Test
    fun tablicaBezIcegaDrugogZadrzavaKljuc() {
        val sirovo = trening.joinToString("\n") + "\n\n" + Oblik.KLJUC
        val p = Oblik.citaj(sirovo)
        assertEquals(sirovo, Oblik.zapisi(p.tekst, p.stilovi, kljuc = p.kljuc).sirovo)
    }

    @Test
    fun odsjecci() {
        assertEquals(listOf(Odsjecak(0, 1, null)), Odsjecci.podijeli(1, emptyList()))
        assertEquals(
            listOf(Odsjecak(0, 0, null), Odsjecak(0, 3, 0), Odsjecak(3, 5, null)),
            Odsjecci.podijeli(5, listOf(0..2)),
        )
        assertEquals("a\nX\nb", Odsjecci.zamijeni("a\nb", 1, 1, "X"))
        assertEquals("a\nb", Odsjecci.zamijeni("a\nb", 1, 1, ""))
        assertEquals("a\nb\nX", Odsjecci.zamijeni("a\nb", 2, 2, "X"))
        assertEquals("X\nY\nb", Odsjecci.zamijeni("a\nb", 0, 1, "X\nY"))
        assertEquals(2, Odsjecci.pocetak("a\nb", 1))
        assertEquals(3, Odsjecci.pocetak("a\nb", 2))
    }

    @Test
    fun nasumicneTabliceSeVjernoCitaju() {
        val r = Random(4)
        val znakovi = "ab čšž 12*/-│┼▐‹\n".toCharArray()
        repeat(3000) { n ->
            val stupaca = 1 + r.nextInt(4)
            val redaka = 1 + r.nextInt(4)
            var t = Tablica(List(stupaca) { 1 + r.nextInt(6) }, List(redaka) { List(stupaca) { "" } }, r.nextBoolean() && redaka > 1)
            repeat(r.nextInt(8)) {
                val tekst = buildString { repeat(r.nextInt(10)) { append(znakovi[r.nextInt(znakovi.size)]) } }
                t = Tablice.citaj(Tablice.zapisi(t.sa(r.nextInt(redaka), r.nextInt(stupaca), tekst)))!!
            }
            val z = Tablice.zapisi(t)
            val p = Tablice.citaj(z)!!
            fun norm(x: Tablica) = x.retci.map { row -> row.map { Tablice.normaliziraj(it) } }
            assertEquals("tablica $n", norm(t), norm(p))
            assertEquals("tablica $n: zapis je stabilan", z, Tablice.zapisi(p))
            for (c in p.sirine.indices) {
                val najdulji = p.retci.maxOf { row -> row[c].split("\n").maxOf { it.codePointCount(0, it.length) } }
                assertEquals("tablica $n: stupac $c je točno najdulji red", maxOf(najdulji, 1), p.sirine[c])
            }
        }
    }

    private val mala = Tablica(listOf(3, 4), listOf(listOf("Što", "Kg"), listOf("a", "80")), zaglavlje = true)

    @Test
    fun redakIspod() {
        val t = mala.sRetkomIspod(0)
        assertEquals(listOf(listOf("Što", "Kg"), listOf("", ""), listOf("a", "80")), t.retci)
        assertEquals(
            listOf(
                "┌─────┬────┐",
                "│ Što │ Kg │",
                "╞═════╪════╡",
                "│     │    │",
                "├─────┼────┤",
                "│ a   │ 80 │",
                "└─────┴────┘",
            ),
            Tablice.zapisi(t),
        )
        assertEquals(3, mala.sRetkomIspod(1).retci.size)
        assertEquals(listOf("", ""), mala.sRetkomIspod(1).retci.last())
    }

    @Test
    fun stupacDesno() {
        val t = mala.sStupcemDesno(0)
        assertEquals(listOf(listOf("Što", "", "Kg"), listOf("a", "", "80")), t.retci)
        val procitana = Tablice.citaj(Tablice.zapisi(t))!!
        assertEquals(t.retci, procitana.retci)
        assertEquals(listOf(3, 1, 2), procitana.sirine)
    }

    @Test
    fun brisanjeRetkaIStupca() {
        assertEquals(listOf(listOf("Što", "Kg")), mala.bezRetka(1)!!.retci)
        // brisanjem zaglavlja tablica ostaje bez zaglavlja
        val bez = mala.bezRetka(0)!!
        assertEquals(listOf(listOf("a", "80")), bez.retci)
        assertTrue(!bez.zaglavlje)
        assertNull(mala.bezRetka(0)!!.bezRetka(0))
        assertEquals(listOf(listOf("Kg"), listOf("80")), mala.bezStupca(0)!!.retci)
                assertNull(mala.bezStupca(0)!!.bezStupca(0))
        assertNull(mala.bezRetka(5))
    }

    @Test
    fun nasumicneRadnjeUvijekDajuIspravnuTablicu() {
        val r = Random(11)
        val tekstovi = listOf("", "x", "čaj", "a\nb", "dugačak tekst")
        var t = Tablica.nova()
        repeat(3000) { n ->
            val red = r.nextInt(t.retci.size)
            val stupac = r.nextInt(t.sirine.size)
            t = when (r.nextInt(5)) {
                0 -> t.sRetkomIspod(red)
                1 -> t.sStupcemDesno(stupac)
                2 -> t.bezRetka(red) ?: t
                3 -> t.bezStupca(stupac) ?: t
                else -> t.sa(red, stupac, tekstovi[r.nextInt(tekstovi.size)])
            }
            val linije = Tablice.zapisi(t)
            val procitana = Tablice.citaj(linije)
            assertEquals(
                "radnja $n",
                t.retci.map { row -> row.map { Tablice.normaliziraj(it) } },
                procitana?.retci?.map { row -> row.map { Tablice.normaliziraj(it) } },
            )
            assertEquals("radnja $n: nadji", listOf(0 until linije.size), Tablice.nadji(linije))
            t = procitana!!
        }
    }

    @Test
    fun brisanjeRazmakaUvijekSuzuje() {
        // „Kg" i „80" su jednako dugi; razmak upisan iza „80" datoteka pripisuje prvom, „Kg"
        val prazna = Tablica(listOf(1), listOf(listOf("Kg"), listOf("80")), zaglavlje = true)
        val s = Tablice.citaj(Tablice.zapisi(prazna.saUpisanim(1, 0, "80", "80 ")))!!
        assertEquals(listOf(3), s.sirine)
        assertEquals(listOf(listOf("Kg "), listOf("80")), s.retci)
        // ćelija na ekranu i dalje pokazuje „80 "; kad se razmak obriše, stupac se suzi
        val z = Tablice.zapisi(s.saUpisanim(1, 0, "80 ", "80"))
        assertEquals("┌────┐", z[0])
        // a brisanje u ćeliji koja nije imala razmak ne dira tuđu širinu
        assertEquals(listOf(3), Tablice.citaj(Tablice.zapisi(s.saUpisanim(1, 0, "80", "8")))!!.sirine)
        assertEquals("│ 8   │", Tablice.zapisi(s.saUpisanim(1, 0, "80", "8"))[3])
        // sve obrisano: stupac je širok jedan razmak
        val nista = Tablica(listOf(1), listOf(listOf(""), listOf("")), zaglavlje = false)
        assertEquals("│   │", Tablice.zapisi(nista.saUpisanim(0, 0, "a", ""))[1])
    }
}
