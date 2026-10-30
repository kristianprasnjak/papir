package hr.papir

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Ikone Papira: sve na mreži 24 × 24, ista tanka crta, zaobljeni krajevi. Boju daje onaj tko ih
 * crta (tint), pa slijede temu.
 */
object Ikone {

    private fun ikona(ime: String, crtez: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(ime, 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = crtez,
            )
        }.build()

    private fun puna(ime: String, crtez: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(ime, 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black), pathBuilder = crtez)
        }.build()

    val ponisti = ikona("Poništi") {
        moveTo(9f, 7f); lineTo(5f, 11f); lineTo(9f, 15f)
        moveTo(5f, 11f); lineTo(14f, 11f)
        curveTo(17.3f, 11f, 19f, 12.8f, 19f, 15.5f)
        curveTo(19f, 18.2f, 17.3f, 20f, 14f, 20f); lineTo(11f, 20f)
    }

    val ponovi = ikona("Ponovi") {
        moveTo(15f, 7f); lineTo(19f, 11f); lineTo(15f, 15f)
        moveTo(19f, 11f); lineTo(10f, 11f)
        curveTo(6.7f, 11f, 5f, 12.8f, 5f, 15.5f)
        curveTo(5f, 18.2f, 6.7f, 20f, 10f, 20f); lineTo(13f, 20f)
    }

    val plus = ikona("Funkcije") {
        moveTo(12f, 5f); lineTo(12f, 19f)
        moveTo(5f, 12f); lineTo(19f, 12f)
    }

    val zatvori = ikona("Zatvori") {
        moveTo(6.5f, 6.5f); lineTo(17.5f, 17.5f)
        moveTo(17.5f, 6.5f); lineTo(6.5f, 17.5f)
    }

    val vise = puna("Izbornik") {
        for (x in listOf(5.5f, 12f, 18.5f)) {
            moveTo(x + 1.6f, 12f)
            arcTo(1.6f, 1.6f, 0f, true, true, x - 1.6f, 12f)
            arcTo(1.6f, 1.6f, 0f, true, true, x + 1.6f, 12f)
            close()
        }
    }

    /** Popis papira: tri retka teksta. */
    val papiri = ikona("Papiri") {
        moveTo(5f, 7f); lineTo(19f, 7f)
        moveTo(5f, 12f); lineTo(19f, 12f)
        moveTo(5f, 17f); lineTo(13f, 17f)
    }

    val natrag = ikona("Natrag") {
        moveTo(11f, 6f); lineTo(5f, 12f); lineTo(11f, 18f)
        moveTo(5f, 12f); lineTo(19f, 12f)
    }

    val trazi = ikona("Traži") {
        moveTo(16.5f, 10.5f)
        arcTo(6f, 6f, 0f, true, true, 4.5f, 10.5f)
        arcTo(6f, 6f, 0f, true, true, 16.5f, 10.5f)
        moveTo(15f, 15f); lineTo(20f, 20f)
    }

    val lijevo = ikona("Lijevo") { moveTo(14f, 6f); lineTo(8f, 12f); lineTo(14f, 18f) }
    val desno = ikona("Desno") { moveTo(10f, 6f); lineTo(16f, 12f); lineTo(10f, 18f) }
    val gore = ikona("Gore") { moveTo(6f, 14f); lineTo(12f, 8f); lineTo(18f, 14f) }
    val dolje = ikona("Dolje") { moveTo(6f, 10f); lineTo(12f, 16f); lineTo(18f, 10f) }

    val kvacica = ikona("Kvačica") {
        moveTo(6f, 5f); lineTo(18f, 5f)
        quadTo(19f, 5f, 19f, 6f); lineTo(19f, 18f)
        quadTo(19f, 19f, 18f, 19f); lineTo(6f, 19f)
        quadTo(5f, 19f, 5f, 18f); lineTo(5f, 6f)
        quadTo(5f, 5f, 6f, 5f); close()
        moveTo(8.5f, 12.5f); lineTo(11f, 15f); lineTo(15.5f, 9.5f)
    }

    val mapa = ikona("Mapa") {
        moveTo(5f, 8f); lineTo(9f, 8f); lineTo(6.5f, 11.5f); close()
        moveTo(11f, 9f); lineTo(19f, 9f)
        moveTo(9f, 14f); lineTo(19f, 14f)
        moveTo(9f, 18f); lineTo(16f, 18f)
    }

    val poveznica = ikona("Poveznica") {
        moveTo(10f, 14f); lineTo(14f, 10f)
        moveTo(8.5f, 11.5f); lineTo(7f, 13f)
        curveTo(5.3f, 14.7f, 5.3f, 17.3f, 7f, 19f)
        curveTo(8.7f, 20.7f, 11.3f, 20.7f, 13f, 19f); lineTo(14.5f, 17.5f)
        moveTo(15.5f, 12.5f); lineTo(17f, 11f)
        curveTo(18.7f, 9.3f, 18.7f, 6.7f, 17f, 5f)
        curveTo(15.3f, 3.3f, 12.7f, 3.3f, 11f, 5f); lineTo(9.5f, 6.5f)
    }

    val tablica = ikona("Tablica") {
        moveTo(4.5f, 5.5f); lineTo(19.5f, 5.5f); lineTo(19.5f, 18.5f); lineTo(4.5f, 18.5f); close()
        moveTo(4.5f, 10f); lineTo(19.5f, 10f)
        moveTo(4.5f, 14.25f); lineTo(19.5f, 14.25f)
        moveTo(12f, 5.5f); lineTo(12f, 18.5f)
    }

    val slika = ikona("Slika") {
        moveTo(4.5f, 5.5f); lineTo(19.5f, 5.5f); lineTo(19.5f, 18.5f); lineTo(4.5f, 18.5f); close()
        moveTo(4.5f, 16f); lineTo(9.5f, 11f); lineTo(13f, 14.5f); lineTo(15f, 12.5f); lineTo(19.5f, 17f)
        moveTo(16.8f, 8.7f)
        arcTo(1.3f, 1.3f, 0f, true, true, 14.2f, 8.7f)
        arcTo(1.3f, 1.3f, 0f, true, true, 16.8f, 8.7f)
    }

    val kutija = ikona("Kutija") {
        moveTo(4.5f, 5.5f); lineTo(19.5f, 5.5f); lineTo(19.5f, 18.5f); lineTo(4.5f, 18.5f); close()
        moveTo(8f, 10f); lineTo(16f, 10f)
        moveTo(8f, 14f); lineTo(13f, 14f)
    }

    val sat = ikona("Datum i vrijeme") {
        moveTo(19.5f, 12f)
        arcTo(7.5f, 7.5f, 0f, true, true, 4.5f, 12f)
        arcTo(7.5f, 7.5f, 0f, true, true, 19.5f, 12f)
        moveTo(12f, 7.5f); lineTo(12f, 12f); lineTo(15f, 14f)
    }

    val pomakni = ikona("Pomakni") {
        moveTo(12f, 4f); lineTo(12f, 20f); moveTo(4f, 12f); lineTo(20f, 12f)
        moveTo(9.5f, 6.5f); lineTo(12f, 4f); lineTo(14.5f, 6.5f)
        moveTo(9.5f, 17.5f); lineTo(12f, 20f); lineTo(14.5f, 17.5f)
        moveTo(6.5f, 9.5f); lineTo(4f, 12f); lineTo(6.5f, 14.5f)
        moveTo(17.5f, 9.5f); lineTo(20f, 12f); lineTo(17.5f, 14.5f)
    }

    val velicina = ikona("Veličina") {
        moveTo(14f, 4.5f); lineTo(19.5f, 4.5f); lineTo(19.5f, 10f)
        moveTo(19.5f, 4.5f); lineTo(13f, 11f)
        moveTo(10f, 19.5f); lineTo(4.5f, 19.5f); lineTo(4.5f, 14f)
        moveTo(4.5f, 19.5f); lineTo(11f, 13f)
    }

    val preimenuj = ikona("Preimenuj") {
        moveTo(14.5f, 5.5f); lineTo(18.5f, 9.5f); lineTo(9f, 19f); lineTo(5f, 19f); lineTo(5f, 15f); close()
    }

    val obrisi = ikona("Obriši") {
        moveTo(5f, 7f); lineTo(19f, 7f)
        moveTo(9.5f, 7f); lineTo(9.5f, 5f); lineTo(14.5f, 5f); lineTo(14.5f, 7f)
        moveTo(7f, 7f); lineTo(8f, 19f); lineTo(16f, 19f); lineTo(17f, 7f)
    }

    val gotovo = ikona("Gotovo") { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f) }

    val otvori = ikona("Otvori") {
        moveTo(13f, 5f); lineTo(19f, 5f); lineTo(19f, 11f)
        moveTo(19f, 5f); lineTo(11f, 13f)
        moveTo(17f, 14f); lineTo(17f, 19f); lineTo(5f, 19f); lineTo(5f, 7f); lineTo(10f, 7f)
    }
}
