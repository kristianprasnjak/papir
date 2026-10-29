package hr.papir

/**
 * Slika ili kutija teksta na točnom mjestu Notepadove mreže (STANDARD.md, 6).
 *
 * [redak] je redak tijela datoteke u kojem je gornji rub, [stupac] stupac lijevog ruba
 * (kodne točke, kako broji Notepad), [sirina] i [visina] su vanjske mjere s okvirom.
 * Za sliku je [sadrzaj] ime datoteke slike u paketu; za kutiju tekst (redovi odvojeni s \n).
 */
data class Objekt(
    val vrsta: Vrsta,
    val redak: Int,
    val stupac: Int,
    val sirina: Int,
    val visina: Int,
    val sadrzaj: String,
) {
    enum class Vrsta { SLIKA, KUTIJA }

    val zadnjiRedak: Int get() = redak + visina - 1
    val zadnjiStupac: Int get() = stupac + sirina - 1

    fun preklapa(o: Objekt): Boolean =
        redak <= o.zadnjiRedak && o.redak <= zadnjiRedak && stupac <= o.zadnjiStupac && o.stupac <= zadnjiStupac

    fun na(redak: Int, stupac: Int): Objekt = copy(redak = redak, stupac = stupac)

    companion object {
        /** Slika: najmanje jedan redak unutra, i dovoljno široka da ime stane s razmakom sa svake strane. */
        fun slika(redak: Int, stupac: Int, sirina: Int, visina: Int, ime: String): Objekt =
            Objekt(Vrsta.SLIKA, redak, stupac, maxOf(sirina, duljina(ime) + 4, 3), maxOf(visina, 3), ime)

        /** Kutija je široka koliko njezin najdulji red, kao ćelija tablice (STANDARD.md, 4). */
        fun kutija(redak: Int, stupac: Int, tekst: String): Objekt {
            val redovi = tekst.trimEnd('\n').split("\n")
            val w = maxOf(1, redovi.maxOf { duljina(it) })
            return Objekt(Vrsta.KUTIJA, redak, stupac, w + 4, redovi.size + 2, tekst.trimEnd('\n'))
        }

        internal fun duljina(s: String): Int = s.codePointCount(0, s.length)
    }
}

object Objekti {

    const val SLIKA_GL = '╔' // Alt+201
    const val SLIKA_GD = '╗' // Alt+187
    const val SLIKA_DL = '╚' // Alt+200
    const val SLIKA_DD = '╝' // Alt+188
    const val SLIKA_ZID = '║' // Alt+186
    const val KUTIJA_GL = '╒' // Alt+213
    const val KUTIJA_GD = '╕' // Alt+184
    const val KUTIJA_DL = '╘' // Alt+212
    const val KUTIJA_DD = '╛' // Alt+190
    const val KUTIJA_ZID = '│' // Alt+179
    const val VODORAVNO = '═' // Alt+205

    private fun cp(s: String): IntArray = s.codePoints().toArray()
    private fun str(a: IntArray, od: Int, doo: Int): String = String(a, od, doo - od)

    /** Retci objekta kako stoje u datoteci; svaki je dug točno [Objekt.sirina] znakova. */
    fun nacrtaj(o: Objekt): List<String> {
        val unutra = o.sirina - 2
        return when (o.vrsta) {
            Objekt.Vrsta.SLIKA -> {
                val n = o.visina - 2
                val sredina = (n - 1) / 2
                val ime = o.sadrzaj
                val lijevo = (unutra - Objekt.duljina(ime)) / 2
                List(o.visina) { r ->
                    when (r) {
                        0 -> "$SLIKA_GL${VODORAVNO.toString().repeat(unutra)}$SLIKA_GD"
                        o.visina - 1 -> "$SLIKA_DL${VODORAVNO.toString().repeat(unutra)}$SLIKA_DD"
                        1 + sredina -> SLIKA_ZID + " ".repeat(lijevo) + ime +
                            " ".repeat(unutra - lijevo - Objekt.duljina(ime)) + SLIKA_ZID
                        else -> SLIKA_ZID + " ".repeat(unutra) + SLIKA_ZID
                    }
                }
            }
            Objekt.Vrsta.KUTIJA -> {
                val redovi = o.sadrzaj.split("\n")
                val w = o.sirina - 4
                List(o.visina) { r ->
                    when (r) {
                        0 -> "$KUTIJA_GL${VODORAVNO.toString().repeat(unutra)}$KUTIJA_GD"
                        o.visina - 1 -> "$KUTIJA_DL${VODORAVNO.toString().repeat(unutra)}$KUTIJA_DD"
                        else -> {
                            val t = redovi.getOrElse(r - 1) { "" }
                            "$KUTIJA_ZID $t${" ".repeat(w - Objekt.duljina(t))} $KUTIJA_ZID"
                        }
                    }
                }
            }
        }
    }

    /**
     * Slike i kutije u retcima tijela datoteke. Prepoznavanje je strogo: okvir mora biti točno
     * nacrtan. Objekt koji bi se preklopio s već nađenim nije objekt (njegovi znakovi su tekst).
     */
    fun nadji(retci: List<String>): List<Objekt> {
        val cps = retci.map { cp(it) }
        val out = ArrayList<Objekt>()
        for (r in cps.indices) {
            val red = cps[r]
            for (c in red.indices) {
                val o = when (red[c]) {
                    SLIKA_GL.code -> pokusaj(cps, r, c, true)
                    KUTIJA_GL.code -> pokusaj(cps, r, c, false)
                    else -> null
                } ?: continue
                if (out.none { it.preklapa(o) }) out.add(o)
            }
        }
        return out
    }

    private fun pokusaj(cps: List<IntArray>, r: Int, c: Int, slika: Boolean): Objekt? {
        val gd = (if (slika) SLIKA_GD else KUTIJA_GD).code
        val dl = (if (slika) SLIKA_DL else KUTIJA_DL).code
        val dd = (if (slika) SLIKA_DD else KUTIJA_DD).code
        val zid = (if (slika) SLIKA_ZID else KUTIJA_ZID).code
        val vrh = cps[r]
        var k = c + 1
        while (k < vrh.size && vrh[k] == VODORAVNO.code) k++
        if (k >= vrh.size || vrh[k] != gd || k == c + 1) return null
        val c2 = k
        val unutra = ArrayList<String>()
        var rr = r + 1
        while (rr < cps.size) {
            val red = cps[rr]
            if (red.size <= c2) return null
            if (red[c] == dl) {
                for (x in c + 1 until c2) if (red[x] != VODORAVNO.code) return null
                if (red[c2] != dd) return null
                break
            }
            if (red[c] != zid || red[c2] != zid) return null
            unutra.add(str(red, c + 1, c2))
            rr++
        }
        if (rr >= cps.size || unutra.isEmpty()) return null
        val sirina = c2 - c + 1
        val visina = rr - r + 1
        return if (slika) {
            val puni = unutra.filter { it.isNotBlank() }
            if (puni.size != 1) return null
            val ime = puni[0].trim()
            if (Objekt.duljina(ime) + 4 > sirina) return null
            Objekt(Objekt.Vrsta.SLIKA, r, c, sirina, visina, ime)
        } else {
            if (unutra.any { it.length < 2 || !it.startsWith(" ") || !it.endsWith(" ") }) return null
            val redovi = unutra.map { s -> s.substring(1, s.length - 1).trimEnd() }.toMutableList()
            // višak širine: razmaci na kraju prvog najduljeg reda, kao u tablici (STANDARD.md, 4)
            val w = sirina - 4
            val najdulji = redovi.maxOf { Objekt.duljina(it) }
            if (w > maxOf(najdulji, 1)) {
                val i = redovi.indexOfFirst { Objekt.duljina(it) == najdulji }
                redovi[i] = redovi[i] + " ".repeat(w - najdulji)
            }
            while (redovi.size > 1 && redovi.last().isEmpty()) redovi.removeAt(redovi.size - 1)
            val tekst = redovi.joinToString("\n")
            val o = Objekt.kutija(r, c, tekst)
            // prazni redovi na kraju: kutija je viša od teksta; to se ne čuva, kao kod ćelije
            if (o.sirina != sirina) return null
            o.copy(visina = visina)
        }
    }

    /** Retci bez objekata, s preslikavanjem pozicija (UTF-16) iz sirovog retka u tekući. */
    class Izvuceno(val retci: List<String>, val mape: List<IntArray>)

    /**
     * Tekst kakav teče oko objekata: iz svakog retka izbacuju se stupci koje zauzimaju objekti,
     * a ostatak se spaja. U retku s objektom razmaci na kraju su dopuna, pa se ne čuvaju.
     */
    fun izvuci(retci: List<String>, objekti: List<Objekt>): Izvuceno {
        val out = ArrayList<String>(retci.size)
        val mape = ArrayList<IntArray>(retci.size)
        for ((r, red) in retci.withIndex()) {
            val uRetku = objekti.filter { r in it.redak..it.zadnjiRedak }
            val mapa = IntArray(red.length + 1)
            if (uRetku.isEmpty()) {
                for (i in mapa.indices) mapa[i] = i
                out.add(red)
                mapa.let(mape::add)
                continue
            }
            val sb = StringBuilder()
            var i = 0
            var stupac = 0
            while (i < red.length) {
                val cpv = red.codePointAt(i)
                val n = Character.charCount(cpv)
                val pokriven = uRetku.any { stupac in it.stupac..it.zadnjiStupac }
                for (j in 0 until n) mapa[i + j] = sb.length + (if (pokriven) 0 else j)
                if (!pokriven) sb.appendCodePoint(cpv)
                i += n
                stupac++
            }
            var kraj = sb.length
            while (kraj > 0 && sb[kraj - 1] == ' ') kraj--
            sb.setLength(kraj)
            for (j in mapa.indices) if (mapa[j] > kraj) mapa[j] = kraj
            mapa[red.length] = kraj
            out.add(sb.toString())
            mape.add(mapa)
        }
        return Izvuceno(out, mape)
    }

    /** Retci s objektima, s preslikavanjem pozicija (UTF-16) iz tekućeg retka u sirovi. */
    class Umetnuto(val retci: List<String>, val mape: List<IntArray>)

    /**
     * Postavlja objekte na mrežu. Tekst retka puni slobodne stupce slijeva nadesno: kad naleti na
     * objekt, nastavlja desno od njega. Ako je tekst kraći, do objekta se dopunjuje razmacima.
     * Retci se po potrebi dodaju, da stane i objekt ispod kraja teksta.
     */
    fun umetni(retci: List<String>, objekti: List<Objekt>): Umetnuto {
        val broj = maxOf(retci.size, (objekti.maxOfOrNull { it.zadnjiRedak } ?: -1) + 1)
        val crtezi = objekti.associateWith { nacrtaj(it).map { cp(it) } }
        val out = ArrayList<String>(broj)
        val mape = ArrayList<IntArray>(broj)
        for (r in 0 until broj) {
            val red = retci.getOrElse(r) { "" }
            val uRetku = objekti.filter { r in it.redak..it.zadnjiRedak }.sortedBy { it.stupac }
            val mapa = IntArray(red.length + 1)
            if (uRetku.isEmpty()) {
                for (i in mapa.indices) mapa[i] = i
                out.add(red)
                mape.add(mapa)
                continue
            }
            val sb = StringBuilder()
            var i = 0 // pozicija u tekućem retku (UTF-16)
            var stupac = 0
            for (o in uRetku) {
                while (stupac < o.stupac) {
                    if (i < red.length) {
                        val cpv = red.codePointAt(i)
                        val n = Character.charCount(cpv)
                        for (j in 0 until n) mapa[i + j] = sb.length + j
                        sb.appendCodePoint(cpv)
                        i += n
                    } else {
                        sb.append(' ')
                    }
                    stupac++
                }
                val crtez = crtezi.getValue(o)[r - o.redak]
                for (x in crtez) sb.appendCodePoint(x)
                stupac += o.sirina
            }
            while (i < red.length) {
                mapa[i] = sb.length
                sb.append(red[i])
                i++
            }
            mapa[red.length] = sb.length
            out.add(sb.toString())
            mape.add(mapa)
        }
        return Umetnuto(out, mape)
    }

    /**
     * Smije li objekt stajati ovdje: unutar mreže, bez preklapanja s drugim objektima, i bez
     * ijednog slova teksta od njegova lijevog ruba nadesno u retcima koje zauzima.
     * [retci] su retci bez objekata (tekući tekst), [ostali] objekti bez ovoga.
     */
    fun slobodno(o: Objekt, retci: List<String>, ostali: List<Objekt>, naEkranu: Map<Int, Int> = emptyMap()): Boolean {
        if (o.redak < 0 || o.stupac < 0) return false
        if (ostali.any { it.preklapa(o) }) return false
        val mreza = umetni(retci, ostali).retci
        for (r in o.redak..o.zadnjiRedak) {
            // veća slova naslova na ekranu zauzimaju više stupaca nego u datoteci
            if ((naEkranu[r] ?: 0) > o.stupac) return false
            val red = cp(mreza.getOrElse(r) { "" })
            for (x in o.stupac until red.size) {
                val tudi = ostali.any { r in it.redak..it.zadnjiRedak && x in it.stupac..it.zadnjiStupac }
                if (!tudi && red[x] != ' '.code) return false
            }
        }
        return true
    }

    /**
     * Nakon izmjene teksta ili veličine redaka: objekt ispod izmijenjenog dijela pomiče se zajedno
     * s retcima (PAPIR.md, 7). [stari] i [novi] su retci čistog teksta, a [redoviStarih] i
     * [redoviNovih] koliko redaka mreže zauzima svaki od njih (naslov dva).
     * Objekt čiji je gornji rub u izmijenjenom retku ostaje u tom retku.
     */
    fun pomakni(
        objekti: List<Objekt>,
        stari: List<String>,
        novi: List<String>,
        redoviStarih: List<Int>,
        redoviNovih: List<Int>,
    ): List<Objekt> {
        if (objekti.isEmpty()) return objekti
        var p = 0
        while (p < stari.size && p < novi.size && stari[p] == novi[p]) p++
        var k = 0
        while (k < stari.size - p && k < novi.size - p && stari[stari.size - 1 - k] == novi[novi.size - 1 - k]) k++
        val pocetakStarih = IntArray(stari.size + 1).also { a -> for (i in stari.indices) a[i + 1] = a[i] + redoviStarih.getOrElse(i) { 1 } }
        val pocetakNovih = IntArray(novi.size + 1).also { a -> for (i in novi.indices) a[i + 1] = a[i] + redoviNovih.getOrElse(i) { 1 } }
        return objekti.map { o ->
            if (o.redak >= pocetakStarih[stari.size]) {
                // ispod kraja teksta: pomak koliko se promijenio broj redaka
                return@map o.copy(redak = maxOf(0, o.redak + pocetakNovih[novi.size] - pocetakStarih[stari.size]))
            }
            var linija = 0
            while (linija + 1 <= stari.size && pocetakStarih[linija + 1] <= o.redak) linija++
            val unutar = o.redak - pocetakStarih[linija]
            val nova = when {
                linija < p -> linija
                linija >= stari.size - k -> linija + novi.size - stari.size
                else -> minOf(linija, maxOf(novi.size - 1, 0))
            }
            val redova = redoviNovih.getOrElse(nova) { 1 }
            o.copy(redak = pocetakNovih[nova.coerceIn(0, novi.size)] + minOf(unutar, redova - 1))
        }
    }
}
