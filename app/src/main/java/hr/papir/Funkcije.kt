package hr.papir

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Funkcije koje korisnik izričito umeće iz izbornika (SPEC.md, 3).
 *
 * Ovdje je samo čista logika, bez Androida, da se može testirati.
 */
object Funkcije {

    /** Fiksna širina (vodeće nule), da se u fontu fiksne širine poravnava u stupce. */
    private val DATUM_I_VRIJEME = DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy.")

    /** Funkcija unosa: rezultat je običan tekst, aplikacija ga nakon umetanja zaboravlja. */
    fun datumIVrijeme(trenutak: LocalDateTime): String = trenutak.format(DATUM_I_VRIJEME)

    /**
     * Zamjenjuje odabrani dio teksta umetkom (bez odabira: umeće na kursor).
     * Vraća novi tekst i položaj kursora odmah iza umetka.
     */
    fun umetni(tekst: String, start: Int, end: Int, umetak: String): Pair<String, Int> {
        val a = minOf(start, end).coerceIn(0, tekst.length)
        val b = maxOf(start, end).coerceIn(0, tekst.length)
        return (tekst.substring(0, a) + umetak + tekst.substring(b)) to (a + umetak.length)
    }
}
