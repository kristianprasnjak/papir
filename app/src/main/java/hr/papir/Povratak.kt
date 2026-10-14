package hr.papir

/** Tekst s kursorom ili odabirom, kakav se pamti za poništavanje. */
data class Stanje(val text: String, val start: Int, val end: Int)

/**
 * Poništavanje i ponavljanje.
 *
 * Tipkanje se grupira: niz malih izmjena bez stanke dulje od [stankaMs] poništava se
 * jednim dodirom. Novi redak, lijepljenje, brisanje odabira i funkcije su zasebni koraci.
 */
class Povratak(private val limit: Int = 300, private val stankaMs: Long = 1000) {

    private val natrag = ArrayDeque<Stanje>()
    private val naprijed = ArrayDeque<Stanje>()
    private var zadnjaIzmjena = 0L
    private var uSkupini = false

    val moguPonistiti: Boolean get() = natrag.isNotEmpty()
    val moguPonoviti: Boolean get() = naprijed.isNotEmpty()

    /** Pozvati prije svake izmjene teksta, sa stanjem kakvo je bilo prije nje. */
    fun prijeIzmjene(prije: Stanje, sada: Long, mala: Boolean) {
        naprijed.clear()
        val nastavakSkupine = mala && uSkupini && sada - zadnjaIzmjena <= stankaMs
        if (!nastavakSkupine) {
            natrag.addLast(prije)
            while (natrag.size > limit) natrag.removeFirst()
        }
        uSkupini = mala
        zadnjaIzmjena = sada
    }

    fun ponisti(trenutno: Stanje): Stanje? {
        val s = natrag.removeLastOrNull() ?: return null
        naprijed.addLast(trenutno)
        uSkupini = false
        return s
    }

    fun ponovi(trenutno: Stanje): Stanje? {
        val s = naprijed.removeLastOrNull() ?: return null
        natrag.addLast(trenutno)
        uSkupini = false
        return s
    }

    fun ocisti() {
        natrag.clear()
        naprijed.clear()
        uSkupini = false
    }

    companion object {
        /** Jedno upisano ili obrisano slovo koje nije novi redak. */
        fun jeMalaIzmjena(prije: String, poslije: String): Boolean {
            val razlika = poslije.length - prije.length
            if (razlika > 1 || razlika < -1) return false
            if (razlika == 1 && poslije.count { it == '\n' } > prije.count { it == '\n' }) return false
            return true
        }
    }
}
