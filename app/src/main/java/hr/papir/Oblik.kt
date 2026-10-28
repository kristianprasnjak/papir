package hr.papir

/**
 * Oblikovanje jednog znaka (STANDARD.md, 3).
 * Podebljano, kurziv i poveznica vrijede za znak; veličina za cijeli redak
 * (0 M zadano, 1 L, 2 XL). [poveznica] je cilj poveznice (STANDARD.md, 5.4) ili null.
 */
data class Stil(
    val podebljano: Boolean = false,
    val kurziv: Boolean = false,
    val velicina: Int = 0,
    val poveznica: String? = null,
) {
    val obicno: Boolean get() = !podebljano && !kurziv && velicina == 0 && poveznica == null

    companion object {
        val OBICNO = Stil()
    }
}

/**
 * Čitač i pisač formata 1 (STANDARD.md, 2.4 i 3): čisti tekst + stil svakog znaka ⇄ txt.
 *
 * Čista logika bez Androida. Pozicije se preslikavaju u oba smjera
 * da kursor ostane na istom mjestu u tekstu.
 */
object Oblik {

    /** Broj formata koji ovaj čitač i pisač poznaje. */
    const val FORMAT = 1

    /** Ključ formata 1; općenito `─── papir N ───` (STANDARD.md, 2.4). */
    const val KLJUC = "─── papir 1 ───"

    fun kljuc(format: Int): String = "─── papir $format ───"

    private val UZORAK_KLJUCA = Regex("─── papir ([1-9][0-9]{0,8}) ───")

    const val PODEBLJANO_OD = '▐' // U+2590, Alt+222
    const val PODEBLJANO_DO = '▌' // U+258C, Alt+221
    const val KURZIV_OD = '‹' // U+2039, Alt+0139
    const val KURZIV_DO = '›' // U+203A, Alt+0155
    const val CRTA_XL = '═' // U+2550, Alt+205
    const val CRTA_L = '─' // U+2500, Alt+196
    const val POVEZNICA_OD = '→' // U+2192, Alt+26
    const val POVEZNICA_CILJ = '│' // U+2502, Alt+179
    const val POVEZNICA_DO = '←' // U+2190, Alt+27

    /** Prepoznaje li se poveznica: cilj ne smije biti prazan ni sadržavati znakove poveznice ni novi red. */
    fun ispravanCilj(cilj: String): Boolean =
        cilj.isNotEmpty() && cilj.none { it == POVEZNICA_OD || it == POVEZNICA_DO || it == POVEZNICA_CILJ || it == '\n' }

    /** Jedan pročitani redak: što se vidi, kako je oblikovano, i točan izvorni tekst (s crtom naslova). */
    class Linija(val tekst: String, val stilovi: List<Stil>, val izvor: String)

    class Procitano(
        val tekst: String,
        val stilovi: List<Stil>,
        /** Za svaku poziciju u txt-u (0..duljina) pozicija u čistom tekstu. */
        val izSirovog: IntArray,
        /** Pročitani retci s izvornim tekstom, da pisač nedirane retke zapiše doslovno. */
        val linije: List<Linija>,
        /** Retci čistog teksta (uključivo) u kojima su tablice; u čistom tekstu tablica stoji doslovno. */
        val tablice: List<IntRange> = emptyList(),
        /** Je li datoteka imala ključ. */
        val kljuc: Boolean = false,
        /** Broj formata iz ključa; 0 ako ključa nema. */
        val format: Int = 0,
        /** Slike i kutije na mreži (STANDARD.md, 6 i 7). */
        val objekti: List<Objekt> = emptyList(),
        /** Sirovi retci s objektima, da pisač nedirane zapiše doslovno. */
        val mreza: Mreza? = null,
    )

    /** Retci tijela kakvi su bili u datoteci: tekući (bez objekata) i sirovi (s njima), i objekti. */
    class Mreza(val tekuci: List<String>, val sirovi: List<String>, val objekti: List<Objekt>)

    class Zapisano(
        val sirovo: String,
        /** Za svaku poziciju u čistom tekstu (0..duljina) pozicija u txt-u. */
        val uSirovo: IntArray,
    )

    // --- Ključ ---

    /** Tijelo datoteke bez ključa i praznog retka ispred njega; null ako ispravnog ključa nema. */
    fun tijelo(sirovo: String): String? = razdvoji(sirovo)?.first

    /** Broj formata iz ključa (`─── papir N ───`, N ≥ 1 bez vodećih nula); 0 ako ključa nema. */
    fun format(sirovo: String): Int = razdvoji(sirovo)?.second ?: 0

    private fun razdvoji(sirovo: String): Pair<String, Int>? {
        val s = if (sirovo.endsWith("\n")) sirovo.dropLast(1) else sirovo
        val zadnji = s.substring(s.lastIndexOf('\n') + 1)
        val m = UZORAK_KLJUCA.matchEntire(zadnji) ?: return null
        val n = m.groupValues[1].toInt()
        if (zadnji.length == s.length) return "" to n
        val body = s.dropLast(zadnji.length + 1)
        return (if (body.endsWith("\n")) body.dropLast(1) else body) to n
    }

    // --- Čitač ---

    fun citaj(sirovo: String): Procitano {
        val (tijelo, format) = razdvoji(sirovo) ?: return goliTekst(sirovo)
        val retci = tijelo.split("\n")
        val objekti = Objekti.nadji(retci)
        if (objekti.isEmpty()) return citajTijelo(tijelo, sirovo.length, format)

        // Slike i kutije su na mreži; tekst teče oko njih. Čita se tekst bez njih.
        val izv = Objekti.izvuci(retci, objekti)
        val tekuce = izv.retci.joinToString("\n")
        val p = citajTijelo(tekuce, tekuce.length, format)
        val mapa = IntArray(sirovo.length + 1)
        var o = 0
        var t = 0
        for ((k, red) in retci.withIndex()) {
            for (i in 0..red.length) mapa[o + i] = p.izSirovog[t + izv.mape[k][i]]
            o += red.length + 1
            t += izv.retci[k].length + 1
        }
        for (k in tijelo.length..sirovo.length) mapa[k] = p.tekst.length
        return Procitano(
            p.tekst, p.stilovi, mapa, p.linije, p.tablice, kljuc = true, format = format,
            objekti = objekti, mreza = Mreza(izv.retci, retci, objekti),
        )
    }

    private fun citajTijelo(body: String, ukupno: Int, format: Int): Procitano {

        val redovi = body.split("\n")
        val pocetci = IntArray(redovi.size)
        var o = 0
        for (k in redovi.indices) {
            pocetci[k] = o
            o += redovi[k].length + 1
        }

        val sb = StringBuilder(body.length)
        val stilovi = ArrayList<Stil>(body.length)
        val mapa = IntArray(ukupno + 1)
        val linije = ArrayList<Linija>()
        val tablice = Tablice.nadji(redovi)
        val tabliceCisto = ArrayList<IntRange>()
        var i = 0
        while (i < redovi.size) {
            if (linije.isNotEmpty()) {
                mapa[pocetci[i] - 1] = sb.length
                sb.append('\n')
                stilovi.add(Stil.OBICNO)
            }
            val tablica = tablice.firstOrNull { it.first == i }
            if (tablica != null) {
                // Tablica stoji u čistom tekstu doslovno; crta je prikaz (STANDARD.md, 4).
                val prviRedak = linije.size
                for (t in tablica) {
                    if (t > i) {
                        mapa[pocetci[t] - 1] = sb.length
                        sb.append('\n')
                        stilovi.add(Stil.OBICNO)
                    }
                    val red = redovi[t]
                    for (k in red.indices) mapa[pocetci[t] + k] = sb.length + k
                    sb.append(red)
                    repeat(red.length) { stilovi.add(Stil.OBICNO) }
                    linije.add(Linija(red, List(red.length) { Stil.OBICNO }, red))
                }
                tabliceCisto.add(prviRedak until linije.size)
                i = tablica.last + 1
                continue
            }
            val red = redovi[i]
            val sljedeci = redovi.getOrNull(i + 1)
            var velicina = 0
            var redova = 1
            when {
                sljedeci != null && jeNaslov(red, sljedeci, CRTA_XL) -> {
                    velicina = 2
                    redova = 2
                }
                sljedeci != null && jeNaslov(red, sljedeci, CRTA_L) -> {
                    velicina = 1
                    redova = 2
                }
            }
            val pocetakLinije = sb.length
            citajRedak(red, pocetci[i], velicina, sb, stilovi, mapa)
            var izvor = red
            if (redova == 2) {
                // prijelom i crta naslova pripadaju naslovu: preslikavaju se na kraj njegova teksta
                val od = pocetci[i] + red.length
                val doo = pocetci[i + 1] + sljedeci!!.length
                for (k in od until doo) mapa[k] = sb.length
                izvor = red + "\n" + sljedeci
            }
            linije.add(Linija(sb.substring(pocetakLinije), stilovi.subList(pocetakLinije, sb.length).toList(), izvor))
            i += redova
        }
        for (k in body.length..ukupno) mapa[k] = sb.length
        return Procitano(sb.toString(), stilovi, mapa, linije, tabliceCisto, kljuc = true, format = format)
    }

    private fun goliTekst(sirovo: String): Procitano = Procitano(
        sirovo,
        List(sirovo.length) { Stil.OBICNO },
        IntArray(sirovo.length + 1) { it },
        sirovo.split("\n").map { Linija(it, List(it.length) { Stil.OBICNO }, it) },
    )

    /** Broj znakova kako ih broji čovjek u Notepadu (kodne točke). */
    private fun duljina(s: String): Int = s.codePointCount(0, s.length)

    private fun jeCrta(s: String, znak: Char): Boolean = s.isNotEmpty() && s.all { it == znak }

    private fun jeBiloKojaCrta(s: String): Boolean = jeCrta(s, CRTA_XL) || jeCrta(s, CRTA_L)

    private fun jeNaslov(red: String, crta: String, znak: Char): Boolean =
        red.isNotBlank() && !jeBiloKojaCrta(red) && jeCrta(crta, znak) && duljina(crta) == duljina(red)

    /** Parovi otvaranja i zatvaranja jedne vrste u retku; ponovljeno otvaranje poništava ranije. */
    private fun parovi(red: String, otvori: Char, zatvori: Char, uPoveznici: BooleanArray): List<IntRange> {
        val out = ArrayList<IntRange>()
        var otvoren = -1
        for (k in red.indices) {
            if (uPoveznici[k]) continue
            val c = red[k]
            if (c == otvori) {
                otvoren = k
            } else if (c == zatvori && otvoren >= 0) {
                if (k > otvoren + 1) out.add(otvoren..k)
                otvoren = -1
            }
        }
        return out
    }

    private fun krizaju(a: IntRange, b: IntRange): Boolean =
        (a.first < b.first && b.first < a.last && a.last < b.last) ||
            (b.first < a.first && a.first < b.last && b.last < a.last)

    /** Poveznice u retku: (cijeli raspon od → do ←, kraj prikazanog teksta, cilj). */
    private fun poveznice(red: String): List<Triple<IntRange, Int, String>> {
        val out = ArrayList<Triple<IntRange, Int, String>>()
        var k = 0
        while (k < red.length) {
            if (red[k] != POVEZNICA_OD) {
                k++
                continue
            }
            val crta = red.indexOf(POVEZNICA_CILJ, k + 1)
            val kraj = if (crta < 0) -1 else red.indexOf(POVEZNICA_DO, crta + 1)
            val tekst = if (crta < 0) "" else red.substring(k + 1, crta)
            val cilj = if (kraj < 0) "" else red.substring(crta + 1, kraj)
            if (kraj < 0 || tekst.isEmpty() || tekst.any { it == POVEZNICA_OD || it == POVEZNICA_DO } || !ispravanCilj(cilj)) {
                k++
                continue
            }
            out.add(Triple(k..kraj, crta, cilj))
            k = kraj + 1
        }
        return out
    }

    private fun citajRedak(
        red: String,
        pocetak: Int,
        velicina: Int,
        sb: StringBuilder,
        stilovi: MutableList<Stil>,
        mapa: IntArray,
    ) {
        val pov = poveznice(red)
        val uPoveznici = BooleanArray(red.length)
        val skriveno = BooleanArray(red.length)
        val cilj = arrayOfNulls<String>(red.length)
        for ((raspon, crta, c) in pov) {
            for (k in raspon) uPoveznici[k] = true
            skriveno[raspon.first] = true
            for (k in crta..raspon.last) skriveno[k] = true
            for (k in raspon.first + 1 until crta) cilj[k] = c
        }
        val podebljano = parovi(red, PODEBLJANO_OD, PODEBLJANO_DO, uPoveznici)
        val kurziv = parovi(red, KURZIV_OD, KURZIV_DO, uPoveznici).filter { k -> podebljano.none { krizaju(it, k) } }

        val oznaka = BooleanArray(red.length)
        val jePodebljano = BooleanArray(red.length)
        val jeKurziv = BooleanArray(red.length)
        for (p in podebljano) {
            oznaka[p.first] = true
            oznaka[p.last] = true
            for (k in p.first + 1 until p.last) jePodebljano[k] = true
        }
        for (p in kurziv) {
            oznaka[p.first] = true
            oznaka[p.last] = true
            for (k in p.first + 1 until p.last) jeKurziv[k] = true
        }
        for (k in red.indices) {
            mapa[pocetak + k] = sb.length
            if (oznaka[k] || skriveno[k]) continue
            sb.append(red[k])
            stilovi.add(Stil(jePodebljano[k], jeKurziv[k], velicina, cilj[k]))
        }
    }

    // --- Pisač ---

    /**
     * Txt iz čistog teksta i stilova. Bez ijednog oblikovanja: goli tekst, bez ključa.
     *
     * Retci koji su na početku i na kraju jednaki retcima u [prethodne] (isti tekst i oblikovanje)
     * zapisuju se doslovno kao njihov izvornik; ostali u najkraćem obliku. Tako izmjena jednog
     * retka ne dira nijedan drugi.
     *
     * Tablice u čistom tekstu prepoznaju se samo ako papir već ima ključ ([kljuc]) ili oblikovanje,
     * da tablica nacrtana u golom tekstu ne dobije ključ bez korisnikove odluke (STANDARD.md, 4).
     * Prepoznata tablica zapisuje se doslovno i papiru daje ključ.
     *
     * [format] je broj iz ključa pročitane datoteke. Pisač ga nikad ne smanjuje, a papir novijeg
     * formata uvijek zadržava ključ, jer u njemu mogu biti oznake koje ovaj pisač ne poznaje
     * (STANDARD.md, 7).
     */
    fun zapisi(
        tekst: String,
        stilovi: List<Stil>,
        prethodne: List<Linija> = emptyList(),
        kljuc: Boolean = false,
        format: Int = 0,
        objekti: List<Objekt> = emptyList(),
        mreza: Mreza? = null,
    ): Zapisano {
        val noviji = format > FORMAT
        val mapa = IntArray(tekst.length + 1)
        val st = if (stilovi.size == tekst.length) stilovi else List(tekst.length) { Stil.OBICNO }

        val linije = tekst.split("\n")
        val pocetci = IntArray(linije.size)
        var o = 0
        for (k in linije.indices) {
            pocetci[k] = o
            o += linije[k].length + 1
        }
        fun stiloviLinije(k: Int) = st.subList(pocetci[k], pocetci[k] + linije[k].length)
        fun jednaka(k: Int, stara: Linija) = stara.tekst == linije[k] && stara.stilovi == stiloviLinije(k)

        // Nedirano: jednaki retci na početku i na kraju.
        val izvor = arrayOfNulls<String>(linije.size)
        var pocetak = 0
        while (pocetak < linije.size && pocetak < prethodne.size && jednaka(pocetak, prethodne[pocetak])) {
            izvor[pocetak] = prethodne[pocetak].izvor
            pocetak++
        }
        var kraj = 0
        while (kraj < linije.size - pocetak && kraj < prethodne.size - pocetak &&
            jednaka(linije.size - 1 - kraj, prethodne[prethodne.size - 1 - kraj])
        ) {
            izvor[linije.size - 1 - kraj] = prethodne[prethodne.size - 1 - kraj].izvor
            kraj++
        }

        val imaOblikovanje = st.any { !it.obicno }
        val uTablici = BooleanArray(linije.size)
        if (kljuc || noviji || imaOblikovanje) for (t in Tablice.nadji(linije)) for (k in t) uTablici[k] = true
        val imaTablicu = uTablici.any { it }
        // kvačice i mape su živi znakovi: žive samo u papiru s ključem, i taj ključ čuvaju
        val imaZivih = (kljuc || imaOblikovanje) && linije.any { Mape.jeKvacica(it) || Mape.zaglavlje(it) != null }
        val imaObjekte = objekti.isNotEmpty()
        val oblikovano = noviji || imaOblikovanje || imaTablicu || imaZivih || imaObjekte ||
            linije.indices.any { izvor[it] != null && izvor[it] != linije[it] }
        if (!oblikovano) {
            for (k in mapa.indices) mapa[k] = k
            return Zapisano(tekst, mapa)
        }

        val sb = StringBuilder(tekst.length + 32)
        for (k in linije.indices) {
            val off = pocetci[k]
            if (k > 0) {
                mapa[off - 1] = sb.length
                sb.append('\n')
            }
            val doslovno = if (uTablici[k]) linije[k] else izvor[k]
            if (doslovno != null) {
                val start = sb.length
                sb.append(doslovno)
                // približno: kursor unutar doslovno zapisanog retka ostaje u istom retku
                for (i in 0 until linije[k].length) mapa[off + i] = start + minOf(i, doslovno.length)
            } else {
                zapisiLiniju(linije[k], stiloviLinije(k), off, sb, mapa)
            }
        }
        mapa[tekst.length] = sb.length
        // Ako nijedan redak nije dobio nijednu oznaku (npr. „naslov" od samih razmaka), papir je goli tekst.
        if (!noviji && !imaTablicu && !imaZivih && !imaObjekte && sb.length == tekst.length && sb.toString() == tekst) {
            for (k in mapa.indices) mapa[k] = k
            return Zapisano(tekst, mapa)
        }
        if (imaObjekte) return saObjektima(sb.toString(), mapa, objekti, mreza, format)
        sb.append("\n\n").append(kljuc(maxOf(format, FORMAT)))
        return Zapisano(sb.toString(), mapa)
    }

    /**
     * Postavlja slike i kutije na mrežu preko tekućeg tijela. Redak čiji se tekst i objekti nisu
     * promijenili zapisuje se doslovno kakav je bio u datoteci.
     */
    private fun saObjektima(tekuce: String, mapa: IntArray, objekti: List<Objekt>, mreza: Mreza?, format: Int): Zapisano {
        val retci = tekuce.split("\n")
        val um = Objekti.umetni(retci, objekti)
        val konacni = um.retci.toMutableList()
        if (mreza != null) {
            for (k in konacni.indices) {
                val isti = k < mreza.tekuci.size && k < mreza.sirovi.size &&
                    retci.getOrElse(k) { "" } == mreza.tekuci[k] &&
                    objekti.filter { k in it.redak..it.zadnjiRedak } == mreza.objekti.filter { k in it.redak..it.zadnjiRedak }
                if (isti) konacni[k] = mreza.sirovi[k]
            }
        }
        val sb = StringBuilder()
        val pocetakTekuceg = IntArray(retci.size)
        val pocetakKonacnog = IntArray(konacni.size)
        var o = 0
        for (k in retci.indices) {
            pocetakTekuceg[k] = o
            o += retci[k].length + 1
        }
        for (k in konacni.indices) {
            if (k > 0) sb.append('\n')
            pocetakKonacnog[k] = sb.length
            sb.append(konacni[k])
        }
        // pozicije: čisti tekst → tekuće tijelo → konačno tijelo (doslovni retci približno)
        val nova = IntArray(mapa.size)
        for (i in mapa.indices) {
            val t = mapa[i]
            var k = retci.size - 1
            while (k > 0 && pocetakTekuceg[k] > t) k--
            val u = (t - pocetakTekuceg[k]).coerceIn(0, retci[k].length)
            val m = if (konacni[k] == um.retci[k]) um.mape[k][u] else minOf(u, konacni[k].length)
            nova[i] = pocetakKonacnog[k] + m
        }
        sb.append("\n\n").append(kljuc(maxOf(format, FORMAT)))
        return Zapisano(sb.toString(), nova)
    }

    private fun zapisiLiniju(linija: String, st: List<Stil>, off: Int, sb: StringBuilder, mapa: IntArray) {
        val velicina = if (linija.isEmpty()) 0 else st[0].velicina
        val redakStart = sb.length
        zapisiRedak(linija, st, off, sb, mapa)
        if (velicina > 0) {
            val red = sb.substring(redakStart)
            if (red.isNotBlank() && !jeBiloKojaCrta(red)) {
                sb.append('\n').append((if (velicina == 2) CRTA_XL else CRTA_L).toString().repeat(duljina(red)))
            }
        }
    }

    /** Podebljano i kurziv kao parovi, pravilno ugniježđeni; što ne treba, ne piše se. */
    private fun zapisiRedak(linija: String, st: List<Stil>, off: Int, sb: StringBuilder, mapa: IntArray) {
        val otvoreni = ArrayList<Char>() // 'b' ili 'k', redom otvaranja
        fun otvori(x: Char) {
            sb.append(if (x == 'b') PODEBLJANO_OD else KURZIV_OD)
            otvoreni.add(x)
        }
        fun zatvoriDo(indeks: Int) {
            while (otvoreni.size > indeks) {
                val x = otvoreni.removeAt(otvoreni.size - 1)
                sb.append(if (x == 'b') PODEBLJANO_DO else KURZIV_DO)
            }
        }
        var poveznica: String? = null
        fun zatvoriPoveznicu() {
            poveznica?.let { sb.append(POVEZNICA_CILJ).append(it).append(POVEZNICA_DO) }
            poveznica = null
        }
        for (k in linija.indices) {
            val b = st[k].podebljano
            val i = st[k].kurziv
            val znak = linija[k]
            // znakovi poveznice ne mogu biti dio njezina teksta
            val cilj = st[k].poveznica?.takeIf {
                ispravanCilj(it) && znak != POVEZNICA_OD && znak != POVEZNICA_DO && znak != POVEZNICA_CILJ
            }
            fun zeli(x: Char) = if (x == 'b') b else i
            val prvi = otvoreni.indexOfFirst { !zeli(it) }
            val promjena = prvi >= 0 || (b && 'b' !in otvoreni) || (i && 'k' !in otvoreni)
            if (poveznica != null && (cilj != poveznica || promjena)) zatvoriPoveznicu()
            if (prvi >= 0) {
                val iznad = otvoreni.subList(prvi + 1, otvoreni.size).toList()
                zatvoriDo(prvi)
                for (x in iznad) if (zeli(x)) otvori(x)
            }
            if (b && 'b' !in otvoreni) otvori('b')
            if (i && 'k' !in otvoreni) otvori('k')
            if (cilj != null && poveznica == null) {
                sb.append(POVEZNICA_OD)
                poveznica = cilj
            }
            mapa[off + k] = sb.length
            sb.append(znak)
        }
        zatvoriPoveznicu()
        zatvoriDo(0)
    }

    // --- Uređivanje ---

    /**
     * Stilovi nakon izmjene teksta. Umetnuti znakovi nasljeđuju stil prethodnog znaka u retku
     * (na početku retka: sljedećeg), osim ako je zadan [umetnuti]. Novi redak je uvijek običan.
     */
    fun prilagodi(stari: String, stilovi: List<Stil>, novi: String, umetnuti: Stil? = null): List<Stil> {
        val st = if (stilovi.size == stari.length) stilovi else List(stari.length) { Stil.OBICNO }
        val min = minOf(stari.length, novi.length)
        var p = 0
        while (p < min && stari[p] == novi[p]) p++
        var s = 0
        while (s < min - p && stari[stari.length - 1 - s] == novi[novi.length - 1 - s]) s++
        val umetnuto = novi.length - p - s

        val naslijedjeni = umetnuti ?: when {
            p > 0 && stari[p - 1] != '\n' -> st[p - 1]
            p < stari.length && stari[p] != '\n' -> st[p]
            else -> Stil.OBICNO
        }
        val rezultat = ArrayList<Stil>(novi.length)
        rezultat.addAll(st.subList(0, p))
        for (k in 0 until umetnuto) rezultat.add(if (novi[p + k] == '\n') Stil.OBICNO else naslijedjeni)
        rezultat.addAll(st.subList(stari.length - s, stari.length))
        return rezultat
    }

    /** Mijenja stil znakova u rasponu [od, do); prijelomi redaka ostaju obični. */
    fun primijeni(tekst: String, stilovi: List<Stil>, od: Int, doo: Int, promjena: (Stil) -> Stil): List<Stil> {
        val a = od.coerceIn(0, tekst.length)
        val b = doo.coerceIn(a, tekst.length)
        return List(tekst.length) { i ->
            val s = stilovi.getOrElse(i) { Stil.OBICNO }
            if (i in a until b && tekst[i] != '\n') promjena(s) else s
        }
    }

    /** Uzastopni neobični dijelovi teksta, za prikaz: (početak, kraj, stil). */
    fun dijelovi(stilovi: List<Stil>): List<Triple<Int, Int, Stil>> {
        val out = ArrayList<Triple<Int, Int, Stil>>()
        var i = 0
        while (i < stilovi.size) {
            val s = stilovi[i]
            var j = i + 1
            while (j < stilovi.size && stilovi[j] == s) j++
            if (!s.obicno) out.add(Triple(i, j, s))
            i = j
        }
        return out
    }
}
