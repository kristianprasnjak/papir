package hr.papir

import java.io.File
import java.security.MessageDigest

/**
 * Prethodne verzije papira, unutar aplikacije, kao sigurnosna mreža (zadnjih [limit] po papiru).
 *
 * Sprema se: izvorna verzija datoteke prije prve izmjene, verzija koja gubi u sukobu,
 * verzija koju je zamijenila promjena izvana, nacrt prije brisanja i tekst prije vraćanja
 * starije verzije. Svaka verzija je obična .txt datoteka (UTF-8, \n).
 */
class Povijest(private val root: File, private val limit: Int = 30) {

    data class Verzija(val file: File, val vrijeme: Long, val pregled: String)

    fun spremi(kljuc: String, tekst: String, vrijeme: Long = System.currentTimeMillis()) {
        if (tekst.isEmpty()) return
        val dir = File(root, kljuc)
        dir.mkdirs()
        val zadnja = popis(kljuc).firstOrNull()
        if (zadnja != null && procitaj(zadnja) == tekst) return

        var t = vrijeme
        var f = File(dir, "$t.txt")
        while (f.exists()) f = File(dir, "${++t}.txt")
        f.writeText(tekst, Charsets.UTF_8)

        popis(kljuc).drop(limit).forEach { it.file.delete() }
    }

    /** Najnovije prvo. */
    fun popis(kljuc: String): List<Verzija> {
        val files = File(root, kljuc).listFiles() ?: return emptyList()
        return files.mapNotNull { f ->
            val t = f.name.removeSuffix(".txt").toLongOrNull()
            if (t == null || !f.name.endsWith(".txt")) null else Verzija(f, t, pregled(f))
        }.sortedByDescending { it.vrijeme }
    }

    fun procitaj(v: Verzija): String = v.file.readText(Charsets.UTF_8)

    /** Kad se datoteka preimenuje, povijest ide s njom. */
    fun preseli(od: String, na: String) {
        if (od == na) return
        val izvor = File(root, od)
        val cilj = File(root, na)
        if (!izvor.exists()) return
        if (!cilj.exists() && izvor.renameTo(cilj)) return
        cilj.mkdirs()
        izvor.listFiles()?.forEach { f ->
            val novo = File(cilj, f.name)
            if (!novo.exists()) f.copyTo(novo)
            f.delete()
        }
        izvor.delete()
        popis(na).drop(limit).forEach { it.file.delete() }
    }

    private fun pregled(f: File): String = try {
        f.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.firstOrNull { it.isNotBlank() }?.trim()?.take(60).orEmpty()
        }
    } catch (e: Exception) {
        ""
    }

    companion object {
        const val NACRT = "nacrt"

        /** Ključ papira iz njegove adrese (URI-ja), siguran kao ime mape. */
        fun kljuc(adresa: String): String =
            MessageDigest.getInstance("SHA-1").digest(adresa.toByteArray())
                .joinToString("") { "%02x".format(it) }
    }
}
