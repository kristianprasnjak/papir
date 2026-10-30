package hr.papir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapeTest {

    private val kupovina = listOf(
        "▼ Kupovina",
        "  ► Pekara",
        "    kruh",
        "  ▼ Tržnica",
        "    rajčica",
        "",
        "    paprika",
        "",
        "► Posao",
        "  sastanak",
        "kraj",
    )

    @Test
    fun zaglavljaIKvacice() {
        assertEquals(0 to true, Mape.zaglavlje("▼ Kupovina"))
        assertEquals(2 to false, Mape.zaglavlje("  ► Pekara"))
        assertNull(Mape.zaglavlje("►Pekara"))
        // znak na kraju retka (npr. razmak iza njega pojeo je kraj retka uz sliku) je i dalje mapa
        assertEquals(0 to false, Mape.zaglavlje("►"))
        assertTrue(Mape.jeKvacica("  □"))
        assertTrue(Mape.jeKvacica("□ kruh"))
        assertTrue(Mape.jeKvacica("    ■ mlijeko"))
        assertTrue(!Mape.jeKvacica("□kruh"))
        assertTrue(!Mape.jeKvacica("a □ kruh"))
    }

    @Test
    fun sadrzajPoUvlaci() {
        assertEquals(1..6, Mape.sadrzaj(kupovina, 0))
        assertEquals(2..2, Mape.sadrzaj(kupovina, 1))
        assertEquals(4..6, Mape.sadrzaj(kupovina, 3))
        assertEquals(9..9, Mape.sadrzaj(kupovina, 8))
        assertTrue(Mape.sadrzaj(listOf("► prazna", "isto"), 0).isEmpty())
    }

    @Test
    fun skriveniSuSamoZatvoreni() {
        assertEquals(listOf(2..2, 9..9), Mape.skriveni(kupovina))
        // zatvorena vanjska mapa skriva i otvorene unutarnje
        val zatvoreno = kupovina.toMutableList().also { it[0] = "► Kupovina" }
        assertEquals(listOf(1..6, 9..9), Mape.skriveni(zatvoreno))
    }

    @Test
    fun dodirMijenjaJedanZnak() {
        val t = "  □ kruh\n▼ Posao\nobično"
        assertEquals("  ■ kruh\n▼ Posao\nobično", Mape.prebaci(t, 2))
        assertEquals("  □ kruh\n► Posao\nobično", Mape.prebaci(t, 9))
        assertNull(Mape.prebaci(t, 0))
        assertNull(Mape.prebaci(t, 4))
        assertNull(Mape.prebaci(t, 17))
        assertEquals(t, Mape.prebaci(Mape.prebaci(t, 2)!!, 2))
    }

    @Test
    fun skriveniOdsjecci() {
        val o = Odsjecci.podijeli(kupovina.size, emptyList(), Mape.skriveni(kupovina))
        assertEquals(
            listOf(
                Odsjecak(0, 2, null),
                Odsjecak(2, 3, null, skriveno = true),
                Odsjecak(3, 9, null),
                Odsjecak(9, 10, null, skriveno = true),
                Odsjecak(10, 11, null),
            ),
            o,
        )
    }
}
