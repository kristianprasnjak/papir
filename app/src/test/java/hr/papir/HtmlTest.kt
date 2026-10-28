package hr.papir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlTest {

    private val B = Stil(podebljano = true)
    private val I = Stil(kurziv = true)
    private val BI = Stil(podebljano = true, kurziv = true)
    private val O = Stil.OBICNO

    private fun omot(s: String) = "<div ${Html.OZNAKA} style=\"white-space:pre-wrap\">$s</div>"

    @Test
    fun goliTekst() {
        assertEquals(omot("<div>kruh</div><div><br></div><div>mlijeko</div>"), Html.iz("kruh\n\nmlijeko", List(13) { O }))
    }

    @Test
    fun podebljanoIKurziv() {
        val t = "a bc d"
        val st = listOf(O, O, B, BI, I, I)
        assertEquals(omot("<div>a <b>b<i>c</i></b><i> d</i></div>"), Html.iz(t, st))
    }

    @Test
    fun velicine() {
        val t = "N\nP\nx"
        val st = listOf(Stil(velicina = 2), O, Stil(velicina = 1), O, O)
        assertEquals(omot("<h1>N</h1><h2>P</h2><div>x</div>"), Html.iz(t, st))
    }

    @Test
    fun posebniZnakoviSuTekst() {
        assertEquals(omot("<div>2&lt;3 &amp; &quot;x&quot; &gt;</div>"), Html.iz("2<3 & \"x\" >", List(11) { O }))
    }

    @Test
    fun nikadZnakoviFormata() {
        val p = Oblik.citaj("Naslov\n══════\n▐kruh▌ i ‹mlijeko›\nVidi →recept│Torta.pap←\n\n${Oblik.KLJUC}")
        val h = Html.iz(p.tekst, p.stilovi)
        for (c in "▐▌‹›═─→←│") assertTrue("„$c“ u $h", c !in h)
        assertEquals(
            omot("<h1>Naslov</h1><div><b>kruh</b> i <i>mlijeko</i></div><div>Vidi <a href=\"Torta.pap\">recept</a></div>"),
            h,
        )
    }
}
