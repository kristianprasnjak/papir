package hr.papir

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Random

/**
 * Testovi nasumičnim izmjenama (ARHITEKTURA.md): tisuće nasumičnih papira i izmjena,
 * a nakon svake provjera da se ništa ne gubi i da nedirano ostaje isto.
 */
class OblikNasumicnoTest {

    /** Znakovi običnog teksta, uključujući one koji se u bilješkama često pojavljuju. */
    private val obicniZnakovi = "ab čšž 12*/-()[]{}#=_.:".toCharArray()

    /** Znakovi formata, za nasumične „rukom pisane" datoteke. */
    private val znakoviFormata = "ab ▐▌‹›═─░\n".toCharArray()

    private fun nasumicniPapir(r: Random): Pair<String, List<Stil>> {
        val redovi = r.nextInt(6)
        val sb = StringBuilder()
        val st = ArrayList<Stil>()
        for (n in 0 until redovi) {
            if (n > 0) {
                sb.append('\n')
                st.add(Stil.OBICNO)
            }
            val velicina = if (r.nextInt(3) == 0) r.nextInt(5) - 2 else 0
            repeat(r.nextInt(12)) {
                sb.append(obicniZnakovi[r.nextInt(obicniZnakovi.size)])
                st.add(Stil(r.nextInt(4) == 0, r.nextInt(5) == 0, velicina))
            }
        }
        return sb.toString() to st
    }

    /**
     * Što čitač smije promijeniti: redak koji je u Notepadu prazan (samo razmaci, bez znakova
     * oblikovanja) ne može biti naslov; veličina retka je veličina prvog znaka.
     */
    private fun ocekivano(t: String, st: List<Stil>): List<Stil> {
        val out = st.toMutableList()
        var off = 0
        for (linija in t.split("\n")) {
            val v = if (linija.isEmpty()) 0 else st[off].velicina
            val bezOznaka = (off until off + linija.length).none { st[it].podebljano || st[it].kurziv }
            val ispravna = if (v > 0 && linija.isBlank() && bezOznaka) 0 else v
            for (k in off until off + linija.length) out[k] = out[k].copy(velicina = ispravna)
            off += linija.length + 1
        }
        return out
    }

    @Test
    fun nijednoSlovoNeNestaje() {
        val r = Random(1)
        repeat(5000) { n ->
            val (t, st) = nasumicniPapir(r)
            val z = Oblik.zapisi(t, st)
            val p = Oblik.citaj(z.sirovo)
            assertEquals("papir $n: tekst", t, p.tekst)
            assertEquals("papir $n: oblikovanje", ocekivano(t, st), p.stilovi)
            assertEquals("papir $n: zapis je stabilan", z.sirovo, Oblik.zapisi(p.tekst, p.stilovi).sirovo)
        }
    }

    @Test
    fun nasumicniTxtSeCitaIStabilanJe() {
        val r = Random(2)
        repeat(5000) { n ->
            val sb = StringBuilder()
            repeat(r.nextInt(40)) { sb.append(znakoviFormata[r.nextInt(znakoviFormata.size)]) }
            if (r.nextBoolean()) sb.append("\n\n").append(Oblik.KLJUC)
            val sirovo = sb.toString()

            val p = Oblik.citaj(sirovo)
            assertEquals("txt $n: duljina stilova", p.tekst.length, p.stilovi.size)

            // Bez izmjena, s izvorima: tijelo se zapisuje doslovno isto.
            val z = Oblik.zapisi(p.tekst, p.stilovi, p.linije).sirovo
            val ocekivanoTijelo = Oblik.tijelo(sirovo) ?: sirovo
            val tijelo = Oblik.tijelo(z) ?: z
            assertEquals("txt $n: „$sirovo“ nedirano mora ostati isto", ocekivanoTijelo, tijelo)

            // Najkraći zapis se nakon jednog koraka više ne mijenja.
            val p2 = Oblik.citaj(Oblik.zapisi(p.tekst, p.stilovi).sirovo)
            val p3 = Oblik.citaj(Oblik.zapisi(p2.tekst, p2.stilovi).sirovo)
            assertEquals("txt $n: „$sirovo“ tekst", p2.tekst, p3.tekst)
            assertEquals("txt $n: „$sirovo“ oblikovanje", p2.stilovi, p3.stilovi)
        }
    }

    @Test
    fun izmjenaJednogRetkaNeDiraOstale() {
        val r = Random(3)
        repeat(3000) { n ->
            val (t, st) = nasumicniPapir(r)
            val izvorno = Oblik.citaj(Oblik.zapisi(t, st).sirovo)
            val linije = izvorno.tekst.split("\n")
            val j = r.nextInt(linije.size)
            // dopiši slovo na kraj retka j
            val kraj = linije.take(j + 1).sumOf { it.length + 1 } - 1
            val novi = izvorno.tekst.substring(0, kraj) + "x" + izvorno.tekst.substring(kraj)
            val noviSt = Oblik.prilagodi(izvorno.tekst, izvorno.stilovi, novi)
            val poslije = Oblik.citaj(Oblik.zapisi(novi, noviSt, izvorno.linije).sirovo)

            assertEquals("papir $n: broj redaka", izvorno.linije.size, poslije.linije.size)
            for (k in izvorno.linije.indices) {
                if (k == j) continue
                assertEquals("papir $n, redak $k", izvorno.linije[k].izvor, poslije.linije[k].izvor)
            }
        }
    }
}
