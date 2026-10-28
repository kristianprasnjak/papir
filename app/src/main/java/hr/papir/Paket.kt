package hr.papir

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Papir sa slikama (STANDARD.md, 2.3): obična zip datoteka s nastavkom `.pzp`. Unutra je jedan
 * tekstni papir (`.pap` ili `.txt`) i slike, sve u korijenu. Zip je zip: otvara ga svaki program
 * za arhive. Čista logika bez Androida.
 */
object Paket {

    const val NASTAVAK = ".pzp"

    /** Vrijeme upisano uz svaku stavku, uvijek isto: isti sadržaj daje iste bajtove. */
    private const val VRIJEME = 1767225600000L // 1.1.2026.

    class Sadrzaj(
        /** Ime tekstne stavke u zipu. */
        val imeTeksta: String,
        val tekst: ByteArray,
        /** Sve ostale stavke (slike i što god je još u zipu), redom kako su bile. */
        val prilozi: LinkedHashMap<String, ByteArray>,
    )

    fun jePaket(bajtovi: ByteArray): Boolean =
        bajtovi.size >= 4 && bajtovi[0] == 'P'.code.toByte() && bajtovi[1] == 'K'.code.toByte() &&
            bajtovi[2] == 3.toByte() && bajtovi[3] == 4.toByte()

    /** Sadržaj paketa, ili null ako to nije ispravan paket (nema točno jednog tekstnog papira). */
    fun citaj(bajtovi: ByteArray): Sadrzaj? = try {
        val stavke = LinkedHashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bajtovi)).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                if (!e.isDirectory && !e.name.contains('/')) stavke[e.name] = zip.readBytes()
            }
        }
        val pap = stavke.keys.filter { it.endsWith(".pap", ignoreCase = true) }
        val txt = stavke.keys.filter { it.endsWith(".txt", ignoreCase = true) }
        val ime = when {
            pap.size == 1 -> pap[0]
            pap.isEmpty() && txt.size == 1 -> txt[0]
            else -> null
        }
        if (ime == null) {
            null
        } else {
            val tekst = stavke.remove(ime)!!
            Sadrzaj(ime, tekst, stavke)
        }
    } catch (e: Exception) {
        null
    }

    fun zapisi(s: Sadrzaj): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun stavka(ime: String, b: ByteArray) {
                val e = ZipEntry(ime)
                e.time = VRIJEME
                zip.putNextEntry(e)
                zip.write(b)
                zip.closeEntry()
            }
            stavka(s.imeTeksta, s.tekst)
            for ((ime, b) in s.prilozi) stavka(ime, b)
        }
        return out.toByteArray()
    }

    /** Ime koje još nije zauzeto: „slika.jpg", pa „slika (1).jpg"… */
    fun slobodnoIme(zeljeno: String, zauzeta: Set<String>): String {
        val cisto = zeljeno.filterNot { it == '/' || it == '\\' || it.isISOControl() }.trim().ifEmpty { "slika" }
        if (cisto !in zauzeta) return cisto
        val tocka = cisto.lastIndexOf('.')
        val baza = if (tocka > 0) cisto.substring(0, tocka) else cisto
        val nastavak = if (tocka > 0) cisto.substring(tocka) else ""
        var n = 1
        while ("$baza ($n)$nastavak" in zauzeta) n++
        return "$baza ($n)$nastavak"
    }
}
