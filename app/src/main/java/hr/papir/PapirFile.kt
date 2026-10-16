package hr.papir

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

/** Kraj retka kakav datoteka koristi. Nove datoteke koriste CRLF (Windows). */
enum class LineEnding(val sequence: String) {
    CRLF("\r\n"),
    LF("\n"),
    CR("\r"),
}

/** Tekst za uređivanje (uvijek s \n) i sve što treba da se zapiše natrag točno kakav je bio. */
data class Decoded(
    val text: String,
    val lineEnding: LineEnding,
    val hasBom: Boolean,
)

/**
 * Jedino mjesto koje pretvara bajtove u tekst i natrag (STANDARD.md, 2.1).
 *
 * - UTF-8, strogo: neispravan UTF-8 nije tekst koji smijemo prepisati.
 * - Postojeća datoteka zadržava svoj kraj retka i BOM.
 * - Datoteka bez ijednog kraja retka dobiva CRLF ako joj se doda redak.
 */
object PapirFile {

    private val BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

    /** Vraća null ako bajtovi nisu ispravan UTF-8. */
    fun decode(bytes: ByteArray): Decoded? {
        val hasBom = bytes.size >= 3 &&
            bytes[0] == BOM[0] && bytes[1] == BOM[1] && bytes[2] == BOM[2]
        val body = if (hasBom) bytes.copyOfRange(3, bytes.size) else bytes

        val raw = try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(body))
                .toString()
        } catch (e: CharacterCodingException) {
            return null
        }

        val text = raw.replace("\r\n", "\n").replace('\r', '\n')
        return Decoded(text, detectLineEnding(raw), hasBom)
    }

    fun encode(text: String, lineEnding: LineEnding, hasBom: Boolean): ByteArray {
        val body = text.replace("\n", lineEnding.sequence).toByteArray(Charsets.UTF_8)
        return if (hasBom) BOM + body else body
    }

    /** Prvi kraj retka u datoteci određuje stil. Bez ijednog: CRLF. */
    fun detectLineEnding(raw: String): LineEnding {
        for (i in raw.indices) {
            when (raw[i]) {
                '\r' -> return if (i + 1 < raw.length && raw[i + 1] == '\n') LineEnding.CRLF else LineEnding.CR
                '\n' -> return LineEnding.LF
            }
        }
        return LineEnding.CRLF
    }

    /**
     * Ime novog papira u mapi. Ako već postoji, sustav sam dodaje broj (Note (1).pap…).
     * Korisnik ga može preimenovati u bilo što, bilo kad.
     */
    const val NEW_NAME = "Note.pap"

    /** Ime za drugu verziju kod sukoba, pored originala: „Note (sukob).pap". */
    fun conflictName(name: String): String {
        val dot = name.lastIndexOf('.')
        return if (dot > 0) name.substring(0, dot) + " (sukob)" + name.substring(dot) else "$name (sukob)"
    }

    /** Prijedlog imena za „Spremi kao": prvi redak, bez znakova koje sustavi ne dopuštaju. */
    fun suggestName(text: String): String {
        val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        val clean = firstLine.filterNot { it in "/\\:*?\"<>|" || it.isISOControl() }.take(40).trim()
        return (clean.ifEmpty { "Note" }) + ".pap"
    }
}
