package hr.papir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class ObjektiTest {

    private val k = Oblik.KLJUC
    private fun obicno(t: String) = List(t.length) { Stil.OBICNO }

    @Test
    fun imeStojiUSredini() {
        // širina 15: unutra 13 znakova, ime 9: lijevo 2, desno 2; visina 5: srednji redak je 2
        val crtez = Objekti.nacrtaj(Objekt.slika(0, 0, 15, 5, "torta.jpg"))
        assertEquals(
            listOf(
                "╔═════════════╗",
                "║             ║",
                "║  torta.jpg  ║",
                "║             ║",
                "╚═════════════╝",
            ),
            crtez,
        )
        // ime koje ne stane širi okvir udesno
        assertEquals(13, Objekt.slika(0, 0, 5, 3, "torta.jpg").sirina)
    }

    @Test
    fun tekstTeceDesnoOdSlike() {
        val tekst = "Recept za tortu\nza osam osoba\n\n200 g brašna, 4 jaja, šećer"
        val slika = Objekt.slika(0, 19, 15, 5, "torta.jpg")
        val z = Oblik.zapisi(tekst, obicno(tekst), kljuc = true, objekti = listOf(slika)).sirovo
        assertEquals(
            "Recept za tortu    ╔═════════════╗\n" +
                "za osam osoba      ║             ║\n" +
                "                   ║  torta.jpg  ║\n" +
                "200 g brašna, 4 jaj║             ║a, šećer\n" +
                "                   ╚═════════════╝\n" +
                "\n$k",
            z,
        )
        val p = Oblik.citaj(z)
        assertEquals(listOf(slika), p.objekti)
        // slika ispod kraja teksta dodaje prazan redak
        assertEquals("$tekst\n", p.tekst)
        assertEquals(z, Oblik.zapisi(p.tekst, p.stilovi, p.linije, p.kljuc, p.format, p.objekti, p.mreza).sirovo)
    }

    @Test
    fun kutijaTeksta() {
        val tekst = "Popis\nkruh\nmlijeko\njaja"
        val kutija = Objekt.kutija(0, 19, "ne zaboravi\nkartice!")
        val z = Oblik.zapisi(tekst, obicno(tekst), kljuc = true, objekti = listOf(kutija)).sirovo
        assertEquals(
            "Popis              ╒═════════════╕\n" +
                "kruh               │ ne zaboravi │\n" +
                "mlijeko            │ kartice!    │\n" +
                "jaja               ╘═════════════╛\n" +
                "\n$k",
            z,
        )
        val p = Oblik.citaj(z)
        assertEquals(tekst, p.tekst)
        assertEquals(listOf(kutija), p.objekti)
    }

    @Test
    fun bezKljucaNemaObjekata() {
        val t = "╔═════╗\n║ a.j ║\n╚═════╝"
        assertEquals(t, Oblik.citaj(t).tekst)
        assertTrue(Oblik.citaj(t).objekti.isEmpty())
    }

    @Test
    fun pokvarenOkvirJeTekst() {
        for (t in listOf(
            "╔═════╗\n║ a.j ║\n╚════╝", // dno kraće
            "╔═════╗\n║     ║\n╚═════╝", // bez imena
            "╔═════╗\n║ a b ║\n║ c d ║\n╚═════╝", // dva retka teksta
            "╒═══╕\n│abc│\n╘═══╛", // bez razmaka uz zid
        )) {
            val p = Oblik.citaj("$t\n\n$k")
            assertTrue(t, p.objekti.isEmpty())
            assertEquals(t, p.tekst)
        }
    }

    @Test
    fun nediraniRetciSObjektomOstajuDoslovno() {
        // rukom: razmaci iza okvira; dok se redak ne dira, ostaje kakav je bio
        val sirovo = "a ╒═══╕   \nb │ x │\nc ╘═══╛\nd\n\n$k"
        val p = Oblik.citaj(sirovo)
        assertEquals("a\nb\nc\nd", p.tekst)
        val novi = "a\nb\nc\nd!"
        val z = Oblik.zapisi(novi, obicno(novi), p.linije, p.kljuc, p.format, p.objekti, p.mreza).sirovo
        assertEquals("a ╒═══╕   \nb │ x │\nc ╘═══╛\nd!\n\n$k", z)
    }

    @Test
    fun slobodnoMjesto() {
        val retci = listOf("kruh", "mlijeko i još puno toga", "")
        assertTrue(Objekti.slobodno(Objekt.kutija(0, 8, "a"), listOf("kruh", "mlijeko", ""), emptyList()))
        assertTrue(!Objekti.slobodno(Objekt.kutija(0, 5, "a"), listOf("kruh", "mlijeko", ""), emptyList()))
        // u drugom retku tekst ide preko stupca 10
        assertTrue(!Objekti.slobodno(Objekt.kutija(0, 10, "a"), retci, emptyList()))
        assertTrue(!Objekti.slobodno(Objekt.kutija(0, 2, "a"), retci, emptyList()))
        assertTrue(Objekti.slobodno(Objekt.kutija(2, 0, "a"), retci, emptyList()))
        val druga = Objekt.kutija(2, 0, "b")
        assertTrue(!Objekti.slobodno(Objekt.kutija(2, 2, "a"), retci, listOf(druga)))
        assertTrue(Objekti.slobodno(Objekt.kutija(2, 6, "a"), retci, listOf(druga)))
    }

    @Test
    fun nasumicniPapiriSObjektima() {
        val r = Random(21)
        val znakovi = "ab čšž 12*-".toCharArray()
        repeat(2000) { n ->
            val redaka = 1 + r.nextInt(6)
            val tekst = List(redaka) {
                buildString { repeat(r.nextInt(30)) { append(znakovi[r.nextInt(znakovi.size)]) } }.trimEnd()
            }.joinToString("\n")
            val st = tekst.map { if (it != '\n' && r.nextInt(5) == 0) Stil(podebljano = true) else Stil.OBICNO }
            val objekti = ArrayList<Objekt>()
            repeat(1 + r.nextInt(3)) {
                val red = r.nextInt(redaka + 2)
                val stupac = r.nextInt(25)
                val o = if (r.nextBoolean()) {
                    Objekt.slika(red, stupac, 5 + r.nextInt(10), 3 + r.nextInt(3), "s$it.jpg")
                } else {
                    Objekt.kutija(red, stupac, listOf("x", "ab\ncd", "dugo ime").let { l -> l[r.nextInt(l.size)] })
                }
                if (objekti.none { it.preklapa(o) }) objekti.add(o)
            }
            val z = Oblik.zapisi(tekst, st, kljuc = true, objekti = objekti)
            val p = Oblik.citaj(z.sirovo)
            assertEquals("papir $n: objekti\n${z.sirovo}", objekti.sortedBy { it.redak * 1000 + it.stupac }, p.objekti.sortedBy { it.redak * 1000 + it.stupac })
            val dodano = p.tekst.length - tekst.length
            assertEquals("papir $n: tekst\n${z.sirovo}", tekst + "\n".repeat(maxOf(dodano, 0)), p.tekst)
            val z2 = Oblik.zapisi(p.tekst, p.stilovi, p.linije, p.kljuc, p.format, p.objekti, p.mreza)
            assertEquals("papir $n: stabilno", z.sirovo, z2.sirovo)
            val z3 = Oblik.zapisi(p.tekst, p.stilovi, kljuc = p.kljuc, objekti = p.objekti)
            assertEquals("papir $n: stabilno i bez izvora", z.sirovo, z3.sirovo)
        }
    }

    @Test
    fun primjerKupovina() {
        val f = listOf(java.io.File("../primjeri/kupovina.txt"), java.io.File("primjeri/kupovina.txt")).first { it.exists() }
        val sirovo = PapirFile.decode(f.readBytes())!!.text
        val p = Oblik.citaj(sirovo)
        assertEquals("Kupovina\n▼ Pekara\n  □ kruh\n  ■ kifle\n► Tržnica\n  □ rajčica\nRecept: torta", p.tekst)
        assertEquals(listOf(Objekt.kutija(0, 24, "ne zaboravi\nkartice!")), p.objekti)
        assertEquals(listOf(5..5), Mape.skriveni(p.tekst.split("\n")))
        assertEquals("Torta.pap", p.stilovi.last().poveznica)
        assertEquals(sirovo, Oblik.zapisi(p.tekst, p.stilovi, p.linije, p.kljuc, p.format, p.objekti, p.mreza).sirovo)
        assertEquals(sirovo, Oblik.zapisi(p.tekst, p.stilovi, kljuc = p.kljuc, objekti = p.objekti).sirovo)
    }
}
