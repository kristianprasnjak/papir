package hr.papir

/**
 * Traži i zamijeni, kao u Notepadu (PAPIR.md, 5). Radi nad tekstom kakav se vidi; dijelovi u
 * [izuzeto] (tablice, zatvorene mape) se preskaču. Čista logika bez Androida.
 */
object Trazi {

    /** Sva nepreklapajuća pojavljivanja [upit] u [tekst], slijeva nadesno. */
    fun nadji(tekst: String, upit: String, velikaMala: Boolean, izuzeto: List<IntRange> = emptyList()): List<IntRange> {
        if (upit.isEmpty()) return emptyList()
        val out = ArrayList<IntRange>()
        var od = 0
        while (true) {
            val i = tekst.indexOf(upit, od, ignoreCase = !velikaMala)
            if (i < 0) break
            val r = i until i + upit.length
            if (izuzeto.none { it.first <= r.last && r.first <= it.last }) {
                out.add(r)
                od = r.last + 1
            } else {
                od = i + 1
            }
        }
        return out
    }

    /** Tekst u kojem su sva pojavljivanja zamijenjena; vraća i broj zamjena. */
    fun zamijeniSve(
        tekst: String,
        upit: String,
        novo: String,
        velikaMala: Boolean,
        izuzeto: List<IntRange> = emptyList(),
    ): Pair<String, Int> {
        val nadjeno = nadji(tekst, upit, velikaMala, izuzeto)
        if (nadjeno.isEmpty()) return tekst to 0
        val sb = StringBuilder()
        var od = 0
        for (r in nadjeno) {
            sb.append(tekst, od, r.first).append(novo)
            od = r.last + 1
        }
        sb.append(tekst, od, tekst.length)
        return sb.toString() to nadjeno.size
    }
}
