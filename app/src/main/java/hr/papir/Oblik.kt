package hr.papir

/**
 * Oblikovanje jednog znaka (SPEC.md, 3.1).
 * Veličina: -2 XS, -1 S, 0 M (zadano), 1 L, 2 XL.
 */
data class Stil(
    val podebljano: Boolean = false,
    val kurziv: Boolean = false,
    val velicina: Int = 0,
) {
    val obicno: Boolean get() = !podebljano && !kurziv && velicina == 0

    /** Oznaka koja opisuje cijelo ovo stanje (npr. podebljano i kurziv); za obično `{=}`. */
    fun oznaka(): String {
        if (obicno) return "{=}"
        val sb = StringBuilder("{")
        if (podebljano) sb.append('*')
        if (kurziv) sb.append('/')
        sb.append(
            when (velicina) {
                2 -> "++"
                1 -> "+"
                -1 -> "-"
                -2 -> "--"
                else -> ""
            },
        )
        return sb.append('}').toString()
    }

    companion object {
        val OBICNO = Stil()

        /** Unutrašnjost oznake (bez zagrada) u stil; null ako nije točan uzorak. */
        fun izOznake(unutra: String): Stil? {
            if (unutra == "=") return OBICNO
            var r = unutra
            val b = r.startsWith("*")
            if (b) r = r.substring(1)
            val k = r.startsWith("/")
            if (k) r = r.substring(1)
            val v = when (r) {
                "" -> 0
                "++" -> 2
                "+" -> 1
                "-" -> -1
                "--" -> -2
                else -> return null
            }
            val s = Stil(b, k, v)
            return if (s.obicno) null else s // „{}" nije oznaka
        }
    }
}

/**
 * Čitanje i pisanje formata 1: čisti tekst + stil svakog znaka ⇄ tekst s oznakama i ključem.
 *
 * Sve je čista logika bez Androida. Pozicije se preslikavaju u oba smjera
 * da kursor ostane na istom mjestu u tekstu.
 */
object Oblik {

    const val KLJUC = "{papir 1}"

    /** Duljina najduže unutrašnjosti oznake: `*` + `/` + `++`. */
    private const val NAJDUZA_OZNAKA = 4

    class Procitano(
        val tekst: String,
        val stilovi: List<Stil>,
        /** Za svaku poziciju u sirovom tekstu (0..duljina) pozicija u čistom tekstu. */
        val izSirovog: IntArray,
    )

    class Zapisano(
        val sirovo: String,
        /** Za svaku poziciju u čistom tekstu (0..duljina) pozicija u sirovom tekstu. */
        val uSirovo: IntArray,
    )

    /** Tijelo datoteke bez ključa i praznog retka ispred njega; null ako ispravnog ključa nema. */
    fun tijelo(sirovo: String): String? {
        val s = if (sirovo.endsWith("\n")) sirovo.dropLast(1) else sirovo
        if (s == KLJUC) return ""
        if (!s.endsWith("\n$KLJUC")) return null
        val body = s.dropLast(KLJUC.length + 1)
        return if (body.endsWith("\n")) body.dropLast(1) else body
    }

    fun citaj(sirovo: String): Procitano {
        val body = tijelo(sirovo)
            ?: return Procitano(sirovo, List(sirovo.length) { Stil.OBICNO }, IntArray(sirovo.length + 1) { it })

        val sb = StringBuilder(body.length)
        val stilovi = ArrayList<Stil>(body.length)
        val mapa = IntArray(sirovo.length + 1)
        var stanje = Stil.OBICNO
        var i = 0
        while (i < body.length) {
            val c = body[i]
            if (c == '{') {
                val kraj = body.indexOf('}', i + 1)
                if (kraj >= i + 2 && kraj <= i + 1 + NAJDUZA_OZNAKA) {
                    val s = Stil.izOznake(body.substring(i + 1, kraj))
                    if (s != null) {
                        for (j in i..kraj) mapa[j] = sb.length
                        stanje = s
                        i = kraj + 1
                        continue
                    }
                }
            }
            mapa[i] = sb.length
            sb.append(c)
            if (c == '\n') {
                stilovi.add(Stil.OBICNO)
                stanje = Stil.OBICNO // svaki redak počinje obično
            } else {
                stilovi.add(stanje)
            }
            i++
        }
        for (j in body.length..sirovo.length) mapa[j] = sb.length
        return Procitano(sb.toString(), stilovi, mapa)
    }

    fun zapisi(tekst: String, stilovi: List<Stil>): Zapisano {
        val mapa = IntArray(tekst.length + 1)
        if (stilovi.size != tekst.length || stilovi.all { it.obicno }) {
            for (i in mapa.indices) mapa[i] = i
            return Zapisano(tekst, mapa) // bez funkcija: goli tekst, bez ključa
        }
        val sb = StringBuilder(tekst.length + 32)
        var trenutni = Stil.OBICNO
        for (i in tekst.indices) {
            val c = tekst[i]
            if (c == '\n') {
                mapa[i] = sb.length
                sb.append(c)
                trenutni = Stil.OBICNO
                continue
            }
            val s = stilovi[i]
            if (s != trenutni) {
                sb.append(s.oznaka())
                trenutni = s
            }
            mapa[i] = sb.length
            sb.append(c)
        }
        mapa[tekst.length] = sb.length
        sb.append("\n\n").append(KLJUC)
        return Zapisano(sb.toString(), mapa)
    }

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
