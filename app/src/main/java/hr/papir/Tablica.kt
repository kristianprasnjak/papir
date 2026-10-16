package hr.papir

/**
 * Tablica (STANDARD.md, 4): širine stupaca i tekst svake ćelije.
 *
 * [sirine] su širine sadržaja (bez razmaka uz zidove); [retci] su retci tablice, svaki s tekstom
 * po stupcu (redovi unutar ćelije odvojeni s \n).
 */
data class Tablica(
    val sirine: List<Int>,
    val retci: List<List<String>>,
    val zaglavlje: Boolean,
) {
    fun sa(red: Int, stupac: Int, tekst: String): Tablica = copy(
        retci = retci.mapIndexed { r, row ->
            if (r != red) row else row.mapIndexed { c, t -> if (c == stupac) tekst else t }
        },
    )

    companion object {
        /** Nova prazna tablica: zaglavlje i jedan redak. */
        fun nova(stupaca: Int = 2, sirina: Int = 8): Tablica =
            Tablica(List(stupaca) { sirina }, List(2) { List(stupaca) { "" } }, zaglavlje = true)
    }
}

/** Čitanje i crtanje tablica. Čista logika bez Androida. */
object Tablice {

    private const val GORE_LIJEVO = '┌'.code
    private const val GORE_SPOJ = '┬'.code
    private const val GORE_DESNO = '┐'.code
    private const val DOLJE_LIJEVO = '└'.code
    private const val DOLJE_SPOJ = '┴'.code
    private const val DOLJE_DESNO = '┘'.code
    private const val CRTA = '─'.code
    private const val ZID = '│'.code
    private const val RAZMAK = ' '.code

    private fun duljina(s: String): Int = s.codePointCount(0, s.length)

    /** Retci (indeksi, uključivo) u kojima stoje ispravne tablice. */
    fun nadji(linije: List<String>): List<IntRange> {
        val out = ArrayList<IntRange>()
        var i = 0
        while (i < linije.size) {
            if (linije[i].startsWith("┌")) {
                val kraj = (i + 1 until linije.size).firstOrNull { linije[it].startsWith("└") }
                if (kraj != null && citaj(linije.subList(i, kraj + 1)) != null) {
                    out.add(i..kraj)
                    i = kraj + 1
                    continue
                }
            }
            i++
        }
        return out
    }

    /** Tablica iz svojih redaka (od `┌` do `└`); null ako ijedan redak nije točno kako treba. */
    fun citaj(blok: List<String>): Tablica? {
        if (blok.size < 3) return null
        val cp = blok.map { it.codePoints().toArray() }
        val vrh = cp[0]
        val n = vrh.size
        if (n < 5 || vrh[0] != GORE_LIJEVO || vrh[n - 1] != GORE_DESNO) return null
        val zidovi = ArrayList<Int>()
        zidovi.add(0)
        for (k in 1 until n - 1) {
            when (vrh[k]) {
                CRTA -> {}
                GORE_SPOJ -> zidovi.add(k)
                else -> return null
            }
        }
        zidovi.add(n - 1)
        val jeZid = BooleanArray(n)
        for (z in zidovi) jeZid[z] = true
        for (c in 0 until zidovi.size - 1) if (zidovi[c + 1] - zidovi[c] - 1 < 3) return null

        fun jeCrta(red: IntArray, lijevo: Char, spoj: Char, desno: Char, crta: Char): Boolean =
            red.size == n && red[0] == lijevo.code && red[n - 1] == desno.code &&
                (1 until n - 1).all { k -> red[k] == (if (jeZid[k]) spoj.code else crta.code) }

        if (!jeCrta(cp.last(), '└', '┴', '┘', '─')) return null

        val retci = ArrayList<List<String>>()
        var trenutni: MutableList<MutableList<String>>? = null
        var zaglavlje = false
        for (li in 1 until cp.size - 1) {
            val red = cp[li]
            val obicnaCrta = jeCrta(red, '├', '┼', '┤', '─')
            val crtaZaglavlja = retci.isEmpty() && jeCrta(red, '╞', '╪', '╡', '═')
            if (obicnaCrta || crtaZaglavlja) {
                val gotov = trenutni ?: return null // crta bez retka iznad
                if (crtaZaglavlja) zaglavlje = true
                retci.add(gotov.map { zavrsi(it) })
                trenutni = null
                continue
            }
            if (red.size != n) return null
            for (z in zidovi) if (red[z] != ZID) return null
            val celije = ArrayList<String>(zidovi.size - 1)
            for (c in 0 until zidovi.size - 1) {
                val a = zidovi[c] + 1
                val b = zidovi[c + 1] // područje ćelije je [a, b)
                if (red[a] != RAZMAK || red[b - 1] != RAZMAK) return null
                celije.add(String(red, a + 1, b - a - 2).trimEnd())
            }
            val t = trenutni ?: MutableList(celije.size) { mutableListOf<String>() }.also { trenutni = it }
            celije.forEachIndexed { c, tekst -> t[c].add(tekst) }
        }
        val zadnji = trenutni ?: return null // dno odmah iza crte
        retci.add(zadnji.map { zavrsi(it) })
        val sirine = (0 until zidovi.size - 1).map { zidovi[it + 1] - zidovi[it] - 3 }
        return Tablica(sirine, retci, zaglavlje)
    }

    private fun zavrsi(redovi: List<String>): String = redovi.joinToString("\n").trimEnd('\n')

    /** Ćelija kakva se čuva u datoteci: bez razmaka na kraju redova i praznih redova na kraju. */
    fun normaliziraj(tekst: String): String = tekst.split("\n").joinToString("\n") { it.trimEnd() }.trimEnd('\n')

    /** Crta tablicu. Stupac je širok barem koliko je bio, i barem koliko najduži red u njemu. */
    fun zapisi(t: Tablica): List<String> {
        val celije = t.retci.map { row -> row.map { celija -> celija.split("\n").map { it.trimEnd() } } }
        val sirine = t.sirine.indices.map { c ->
            maxOf(t.sirine[c], 1, celije.maxOfOrNull { row -> row[c].maxOf { duljina(it) } } ?: 0)
        }
        fun crta(lijevo: Char, spoj: Char, desno: Char, znak: Char) =
            lijevo + sirine.joinToString(spoj.toString()) { znak.toString().repeat(it + 2) } + desno

        val out = ArrayList<String>()
        out.add(crta('┌', '┬', '┐', '─'))
        for ((r, row) in celije.withIndex()) {
            val visina = row.maxOf { it.size }
            for (k in 0 until visina) {
                out.add(
                    "│" + row.indices.joinToString("│") { c ->
                        val tekst = row[c].getOrElse(k) { "" }
                        " " + tekst + " ".repeat(sirine[c] - duljina(tekst)) + " "
                    } + "│",
                )
            }
            if (r < celije.size - 1) {
                out.add(if (r == 0 && t.zaglavlje) crta('╞', '╪', '╡', '═') else crta('├', '┼', '┤', '─'))
            }
        }
        out.add(crta('└', '┴', '┘', '─'))
        return out
    }
}

/**
 * Dio papira kakav se prikazuje: tekst ili tablica. [od] i [doo] su retci čistog teksta
 * ([od] uključivo, [doo] isključivo). Tekstni odsječak može biti prazan (od == doo):
 * mjesto ispred, između ili iza tablica gdje se može početi pisati.
 */
data class Odsjecak(val od: Int, val doo: Int, val tablica: Int?)

object Odsjecci {
    fun podijeli(brojRedaka: Int, tablice: List<IntRange>): List<Odsjecak> {
        val out = ArrayList<Odsjecak>()
        var redak = 0
        for ((k, t) in tablice.withIndex()) {
            out.add(Odsjecak(redak, t.first, null))
            out.add(Odsjecak(t.first, t.last + 1, k))
            redak = t.last + 1
        }
        out.add(Odsjecak(redak, brojRedaka, null))
        return out
    }

    /** Zamjenjuje retke [od, doo) novim tekstom; prazan tekst u praznom odsječku ne dodaje ništa. */
    fun zamijeni(tekst: String, od: Int, doo: Int, novi: String): String {
        val linije = tekst.split("\n")
        val noviRetci = if (od == doo && novi.isEmpty()) emptyList() else novi.split("\n")
        return (linije.subList(0, od) + noviRetci + linije.subList(doo, linije.size)).joinToString("\n")
    }

    /** Pozicija početka retka u tekstu. */
    fun pocetak(tekst: String, redak: Int): Int {
        var pos = 0
        repeat(redak) {
            val nl = tekst.indexOf('\n', pos)
            pos = if (nl < 0) tekst.length else nl + 1
        }
        return pos
    }
}
