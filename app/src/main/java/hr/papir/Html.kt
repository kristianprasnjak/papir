package hr.papir

/**
 * HTML verzija odabranog teksta za kopiranje (PAPIR.md, 4): pravo podebljano, kurziv i veličine,
 * da ih e-mail i uređivači teksta prime kakvi jesu. Uz HTML se uvijek šalje i goli tekst.
 *
 * Čista logika bez Androida. HTML nikad ne sadrži znakove formata: što se vidi, to se kopira.
 */
object Html {

    /** Oznaka po kojoj će Papir jednom prepoznati svoj HTML pri lijepljenju. */
    const val OZNAKA = "data-papir=\"1\""

    /** HTML za čisti [tekst] sa [stilovi] (jedan stil po znaku). Svaki redak je jedan blok. */
    fun iz(tekst: String, stilovi: List<Stil>): String {
        val st = if (stilovi.size == tekst.length) stilovi else List(tekst.length) { Stil.OBICNO }
        val sb = StringBuilder()
        sb.append("<div ").append(OZNAKA).append(" style=\"white-space:pre-wrap\">")
        var off = 0
        for (linija in tekst.split("\n")) {
            val ls = st.subList(off, off + linija.length)
            val velicina = ls.firstOrNull()?.velicina ?: 0
            val (od, doo) = when (velicina) {
                2 -> "<h1>" to "</h1>"
                1 -> "<h2>" to "</h2>"
                -1 -> "<div><small>" to "</small></div>"
                -2 -> "<div><small><small>" to "</small></small></div>"
                else -> "<div>" to "</div>"
            }
            sb.append(od)
            if (linija.isEmpty()) sb.append("<br>") else redak(linija, ls, sb)
            sb.append(doo)
            off += linija.length + 1
        }
        sb.append("</div>")
        return sb.toString()
    }

    /** Podebljano i kurziv kao pravilno ugniježđene oznake, najmanje koliko treba. */
    private fun redak(linija: String, st: List<Stil>, sb: StringBuilder) {
        val otvoreni = ArrayList<Char>() // 'b' ili 'i', redom otvaranja
        fun zatvoriDo(n: Int) {
            while (otvoreni.size > n) sb.append(if (otvoreni.removeAt(otvoreni.size - 1) == 'b') "</b>" else "</i>")
        }
        fun otvori(x: Char) {
            sb.append(if (x == 'b') "<b>" else "<i>")
            otvoreni.add(x)
        }
        for (k in linija.indices) {
            fun zeli(x: Char) = if (x == 'b') st[k].podebljano else st[k].kurziv
            val prvi = otvoreni.indexOfFirst { !zeli(it) }
            if (prvi >= 0) {
                val iznad = otvoreni.subList(prvi + 1, otvoreni.size).toList()
                zatvoriDo(prvi)
                for (x in iznad) if (zeli(x)) otvori(x)
            }
            if (st[k].podebljano && 'b' !in otvoreni) otvori('b')
            if (st[k].kurziv && 'i' !in otvoreni) otvori('i')
            when (val c = linija[k]) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                else -> sb.append(c)
            }
        }
        zatvoriDo(0)
    }
}
