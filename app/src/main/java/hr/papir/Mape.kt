package hr.papir

/**
 * Kvačice i mape (STANDARD.md, 5.1 i 5.2): živi znakovi na početku retka. Dodir mijenja znak
 * u datoteci, pa je stanje uvijek zapisano i vidljivo i u Notepadu.
 *
 * Čista logika bez Androida.
 */
object Mape {

    const val PRAZNO = '□' // U+25A1
    const val OZNACENO = '■' // U+25A0, Alt+254
    const val ZATVORENO = '►' // U+25BA, Alt+16
    const val OTVORENO = '▼' // U+25BC, Alt+31

    /** Uvlaka: broj razmaka na početku retka. */
    fun uvlaka(redak: String): Int = redak.length - redak.trimStart(' ').length

    /** Iza znaka je razmak ili kraj retka (prazna kvačica ili mapa bez imena). */
    private fun razmakIliKraj(redak: String, i: Int): Boolean = i == redak.length || redak[i] == ' '

    /** Redak je kvačica: uvlaka, □ ili ■, pa razmak ili kraj retka. */
    fun jeKvacica(redak: String): Boolean {
        val u = uvlaka(redak)
        return redak.length > u && (redak[u] == PRAZNO || redak[u] == OZNACENO) && razmakIliKraj(redak, u + 1)
    }

    /** Zaglavlje mape: (uvlaka, otvorena) ili null. Uvlaka, ► ili ▼, pa razmak ili kraj retka. */
    fun zaglavlje(redak: String): Pair<Int, Boolean>? {
        val u = uvlaka(redak)
        if (redak.length <= u || !razmakIliKraj(redak, u + 1)) return null
        return when (redak[u]) {
            ZATVORENO -> u to false
            OTVORENO -> u to true
            else -> null
        }
    }

    /**
     * Sadržaj mape kojoj je zaglavlje u retku [i]: svi sljedeći retci uvučeniji od zaglavlja,
     * i prazni retci između njih. Prazni retci na kraju ne pripadaju mapi. Prazan raspon ako je nema.
     */
    fun sadrzaj(retci: List<String>, i: Int): IntRange {
        val (u, _) = zaglavlje(retci[i]) ?: return IntRange.EMPTY
        var zadnji = i
        var k = i + 1
        while (k < retci.size) {
            val r = retci[k]
            if (r.isBlank()) {
                k++
                continue
            }
            if (uvlaka(r) <= u) break
            zadnji = k
            k++
        }
        return if (zadnji == i) IntRange.EMPTY else (i + 1)..zadnji
    }

    /** Retci skriveni u zatvorenim mapama (vanjski rasponi, poredani). */
    fun skriveni(retci: List<String>): List<IntRange> {
        val out = ArrayList<IntRange>()
        var i = 0
        while (i < retci.size) {
            val z = zaglavlje(retci[i])
            if (z != null && !z.second) {
                val s = sadrzaj(retci, i)
                if (!s.isEmpty()) {
                    out.add(s)
                    i = s.last + 1
                    continue
                }
            }
            i++
        }
        return out
    }

    /**
     * Ako je na [pozicija] u tekstu znak kvačice ili mape na svom mjestu, vraća tekst s promijenjenim
     * znakom (□ ⇄ ■, ► ⇄ ▼); inače null. Mijenja se točno jedan znak.
     */
    fun prebaci(tekst: String, pozicija: Int): String? {
        if (pozicija !in tekst.indices) return null
        val pocetak = tekst.lastIndexOf('\n', pozicija - 1) + 1
        val kraj = tekst.indexOf('\n', pozicija).let { if (it < 0) tekst.length else it }
        val redak = tekst.substring(pocetak, kraj)
        val u = uvlaka(redak)
        if (pozicija != pocetak + u) return null
        val novi = when {
            jeKvacica(redak) -> if (redak[u] == PRAZNO) OZNACENO else PRAZNO
            zaglavlje(redak) != null -> if (redak[u] == ZATVORENO) OTVORENO else ZATVORENO
            else -> return null
        }
        return tekst.substring(0, pozicija) + novi + tekst.substring(pozicija + 1)
    }
}
