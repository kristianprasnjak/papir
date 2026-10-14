package hr.papir

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class FunkcijeTest {

    @Test
    fun datumImaFiksnuSirinu() {
        assertEquals("09:05 04.03.2026.", Funkcije.datumIVrijeme(LocalDateTime.of(2026, 3, 4, 9, 5)))
        assertEquals("23:59 31.12.2026.", Funkcije.datumIVrijeme(LocalDateTime.of(2026, 12, 31, 23, 59)))
    }

    @Test
    fun umetanjeNaKursor() {
        assertEquals("ab-cd" to 3, Funkcije.umetni("abcd", 2, 2, "-"))
    }

    @Test
    fun umetanjeNaPocetakIKraj() {
        assertEquals("Xabc" to 1, Funkcije.umetni("abc", 0, 0, "X"))
        assertEquals("abcX" to 4, Funkcije.umetni("abc", 3, 3, "X"))
        assertEquals("X" to 1, Funkcije.umetni("", 0, 0, "X"))
    }

    @Test
    fun umetakZamjenjujeOdabir() {
        assertEquals("aXd" to 2, Funkcije.umetni("abcd", 1, 3, "X"))
        // Odabir povučen unatrag (kraj prije početka) radi isto.
        assertEquals("aXd" to 2, Funkcije.umetni("abcd", 3, 1, "X"))
    }

    @Test
    fun ostatakTekstaSeNeDira() {
        val tekst = "Ivica: 2+2=5\nwww.nesto.hr"
        val (novi, _) = Funkcije.umetni(tekst, 12, 12, " (14:00)")
        assertEquals("Ivica: 2+2=5 (14:00)\nwww.nesto.hr", novi)
    }
}
