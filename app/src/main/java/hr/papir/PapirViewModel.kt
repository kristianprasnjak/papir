package hr.papir

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.os.SystemClock
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.AtomicFile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime

/**
 * Operacije nad datotekama žive izvan životnog vijeka ekrana, da se spremanje
 * dovrši i kad korisnik zatvori aplikaciju usred pisanja.
 */
private object AppScope {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val lock = Mutex()
}

/**
 * Ustav, pravilo „Ništa se ne gubi":
 * - svaki papir je prava datoteka u mapi koju je korisnik izabrao; aplikacija ne čuva
 *   nikakve skrivene kopije teksta;
 * - prije svakog zapisa provjerava se je li datoteka promijenjena izvana; ako jest, ništa
 *   se ne prepisuje dok čovjek ne odluči, a druga verzija postaje vidljiva datoteka u mapi;
 * - poništavanje dok je papir otvoren.
 */
class PapirViewModel(app: Application) : AndroidViewModel(app) {

    private val resolver = app.contentResolver
    private val prefs = app.getSharedPreferences("papir", Context.MODE_PRIVATE)

    /** Nacrt iz verzija 0.1 i 0.2. Čita se samo da bi se preselio u mapu, zatim se briše. */
    private val legacyDraft = AtomicFile(File(app.filesDir, "papir.txt"))

    private val povratak = Povratak()

    /**
     * Čisti tekst kakav se vidi i uređuje (bez oznaka i ključa; krajevi redaka \n),
     * zajedno s kursorom i odabirom.
     */
    var content by mutableStateOf(TextFieldValue(""))
        private set

    /** Oblikovanje svakog znaka u [content]. */
    var styles by mutableStateOf<List<Stil>>(emptyList())
        private set

    /** Stil za sljedeći upisani znak, izabran dok ništa nije označeno. */
    private var pendingStyle by mutableStateOf<Stil?>(null)

    /** Retci kako su zadnji put pročitani iz txt-a, da pisač nedirane zapiše doslovno (ARHITEKTURA.md). */
    private var lines: List<Oblik.Linija> = emptyList()

    /** Retci čistog teksta u kojima su tablice (STANDARD.md, 4). */
    var tables by mutableStateOf<List<IntRange>>(emptyList())
        private set

    /** Ima li papir ključ: tek tada se tablice prepoznaju i u tekstu koji se piše. */
    private var keyed = false

    /** Broj formata iz ključa (0 bez ključa); noviji od poznatog se čuva (STANDARD.md, 8). */
    private var format = 0

    /** Slike i kutije na mreži (STANDARD.md, 6). */
    var objects by mutableStateOf<List<Objekt>>(emptyList())
        private set

    /** Retci s objektima kako su zadnji put pročitani, da se nedirani zapišu doslovno. */
    private var mreza: Oblik.Mreza? = null

    /** Retci čistog teksta skriveni u zatvorenim mapama. */
    var hidden by mutableStateOf<List<IntRange>>(emptyList())
        private set

    /** Razmaci koje prikaz umeće u čisti tekst da tekst teče desno od objekta: pozicija → širina. */
    var gaps by mutableStateOf<Map<Int, Int>>(emptyMap())
        private set

    /** Papir je paket (.pzp): zip s tekstom i slikama (STANDARD.md, 2.3). */
    var isPackage by mutableStateOf(false)
        private set

    /** Prilozi u paketu (slike i ostalo), ime → bajtovi. */
    var attachments by mutableStateOf<Map<String, ByteArray>>(emptyMap())
        private set

    /** Obrisane slike ostaju ovdje dok je papir otvoren, da ih poništavanje može vratiti. */
    private val removedAttachments = HashMap<String, ByteArray>()
    private var packageTextName: String? = null
    private var convertToPackage = false

    /** Izabrana slika ili kutija; [ghost] je njezin novi položaj ili veličina dok se ne potvrdi. */
    var selected by mutableStateOf<Objekt?>(null)
        private set
    var ghost by mutableStateOf<Objekt?>(null)
        private set
    var ghostMode by mutableStateOf(GhostMode.MOVE)
        private set

    enum class GhostMode { MOVE, SIZE }

    var searchQuery by mutableStateOf("")
        private set
    var matchCase by mutableStateOf(false)
        private set
    var matches by mutableStateOf<List<IntRange>>(emptyList())
        private set
    var currentMatch by mutableStateOf(-1)
        private set


    /** Tekst kakav ide u datoteku: s oznakama i ključem (STANDARD.md, 3); bez funkcija goli tekst. */
    val text: String get() = Oblik.zapisi(content.text, styles, lines, keyed, format, objects, mreza).sirovo

    /** Stil na mjestu kursora (ili prvog odabranog znaka), za prikaz gumba. */
    val currentStyle: Stil
        get() {
            pendingStyle?.let { return it }
            val t = content.text
            val sel = content.selection
            if (!sel.collapsed) return styles.getOrElse(sel.min) { Stil.OBICNO }
            val c = sel.min
            return when {
                c > 0 && t.getOrNull(c - 1) != '\n' -> styles.getOrElse(c - 1) { Stil.OBICNO }
                c < t.length && t[c] != '\n' -> styles.getOrElse(c) { Stil.OBICNO }
                else -> Stil.OBICNO
            }
        }

    /** Ime datoteke; null znači papir koji još nije datoteka (nema prvog slova). */
    var title by mutableStateOf<String?>(null)
        private set

    var readOnly by mutableStateOf(false)
        private set

    /** Datoteka nije ispravan UTF-8: ne smije se ni prepisati ni spremiti kao. */
    var notText by mutableStateOf(false)
        private set

    var loaded by mutableStateOf(false)
        private set

    var hasFolder by mutableStateOf(false)
        private set

    var canUndo by mutableStateOf(false)
        private set

    var canRedo by mutableStateOf(false)
        private set

    var canRename by mutableStateOf(false)
        private set

    /** Datoteka je promijenjena izvana, a ovdje ima nespremljenih izmjena. Čeka odluku. */
    var conflict by mutableStateOf(false)
        private set

    var message by mutableStateOf<String?>(null)

    private var target: Uri? = null
    private var folder: Uri? = null
    private var lineEnding = LineEnding.CRLF
    private var hasBom = false
    private var dirty = false
    private var saveJob: Job? = null

    /** Bajtovi kakvi su na disku zadnji put pročitani ili zapisani (null: nema s čim usporediti). */
    private var diskBytes: ByteArray? = null

    /** Verzija nađena na disku u trenutku sukoba. */
    private var theirs: ByteArray? = null

    /** Mapa još nije izabrana, a nijedna datoteka nije otvorena: treba izabrati mapu. */
    val needsFolder: Boolean get() = !hasFolder && title == null

    init {
        folder = prefs.getString(KEY_FOLDER, null)?.let(Uri::parse)?.takeIf(::hasPersistedWrite)
        hasFolder = folder != null
        op {
            val last = prefs.getString(KEY_TARGET, null)?.let(Uri::parse)
            if (last == null || !load(last)) startEmpty()
            moveLegacyDraft()
            loaded = true
        }
    }

    // --- Pisanje ---

    fun onFieldChange(new: TextFieldValue) {
        val changed = new.text != content.text
        if (changed && readOnly) return
        if (!changed) {
            // samo pomak kursora ili odabira: ništa se ne sprema; izabrani stil vrijedi samo na mjestu gdje je izabran
            if (new.selection != content.selection) pendingStyle = null
            content = new
            return
        }
        povratak.prijeIzmjene(stanje(), SystemClock.uptimeMillis(), Povratak.jeMalaIzmjena(content.text, new.text))
        val newStyles = Oblik.prilagodi(content.text, styles, new.text, pendingStyle)
        pendingStyle = null
        setModel(new, newStyles)
        refreshUndo()
        markDirty()
    }

    // --- Oblikovanje (STANDARD.md, 3) ---

    /** Podebljano: na odabranom tekstu, ili za ono što slijedi ako ništa nije odabrano. */
    fun toggleBold() = restyle(wholeLine = false) { odabrani ->
        val sve = odabrani.all { it.podebljano }
        val promjena: (Stil) -> Stil = { s -> s.copy(podebljano = !sve) }
        promjena
    }

    fun toggleItalic() = restyle(wholeLine = false) { odabrani ->
        val sve = odabrani.all { it.kurziv }
        val promjena: (Stil) -> Stil = { s -> s.copy(kurziv = !sve) }
        promjena
    }

    /** Veličina je svojstvo retka: djeluje na sve retke koje odabir dodiruje (STANDARD.md, 3). */
    fun setSize(velicina: Int) = restyle(wholeLine = true) { _ ->
        val promjena: (Stil) -> Stil = { s -> s.copy(velicina = velicina) }
        promjena
    }

    private fun restyle(wholeLine: Boolean, makeChange: (List<Stil>) -> (Stil) -> Stil) {
        if (readOnly) return
        val t = content.text
        var a = content.selection.min
        var b = content.selection.max
        if (wholeLine) {
            a = t.lastIndexOf('\n', a - 1) + 1
            b = t.indexOf('\n', b).let { if (it < 0) t.length else it }
        }
        val odabrani = (a until b).filter { t[it] != '\n' }.map { styles.getOrElse(it) { Stil.OBICNO } }
        if (odabrani.isEmpty()) {
            // ništa za oblikovati: stil vrijedi za ono što se upravo počinje pisati
            pendingStyle = makeChange(listOf(currentStyle))(currentStyle)
            return
        }
        val change = makeChange(odabrani)
        povratak.prijeIzmjene(stanje(), SystemClock.uptimeMillis(), false)
        setModel(content, Oblik.primijeni(t, styles, a, b, change))
        refreshUndo()
        markDirty()
    }

    /**
     * Postavlja tekst i stilove tako da su uvijek ono što bi se pročitalo iz datoteke
     * (datoteka je istina): ako je u tekstu s oblikovanjem nastala točna oznaka, ona postaje oznaka.
     */
    private fun setModel(value: TextFieldValue, stilovi: List<Stil>, noviObjekti: List<Objekt> = objects) {
        val zapisano = Oblik.zapisi(value.text, stilovi, lines, keyed, format, noviObjekti, mreza)
        val procitano = Oblik.citaj(zapisano.sirovo)
        if (procitano.tekst == value.text) {
            content = value
        } else {
            fun map(i: Int) = procitano.izSirovog[zapisano.uSirovo[i.coerceIn(0, value.text.length)]]
            content = TextFieldValue(procitano.tekst, TextRange(map(value.selection.start), map(value.selection.end)))
        }
        takeRead(zapisano.sirovo, procitano)
        refreshSearch()
    }

    /** Sve što se izvodi iz pročitanog txt-a: datoteka je istina. */
    private fun takeRead(sirovo: String, p: Oblik.Procitano) {
        lines = p.linije
        tables = p.tablice
        keyed = p.kljuc
        format = p.format
        styles = p.stilovi
        objects = p.objekti
        mreza = p.mreza
        hidden = if (p.kljuc) Mape.skriveni(p.tekst.split("\n")) else emptyList()
        gaps = computeGaps(sirovo, p)
        selected = selected?.let { s -> p.objekti.firstOrNull { it == s } }
        if (selected == null) ghost = null
    }

    /**
     * Gdje tekst teče desno od objekta, prikaz umeće razmake da nastavak stoji desno od njega,
     * kao u Notepadu. Čisti tekst se ne mijenja.
     */
    private fun computeGaps(sirovo: String, p: Oblik.Procitano): Map<Int, Int> {
        if (p.objekti.isEmpty()) return emptyMap()
        val tijelo = Oblik.tijelo(sirovo) ?: return emptyMap()
        val retci = tijelo.split("\n")
        val pocetci = IntArray(retci.size)
        var o = 0
        for (k in retci.indices) {
            pocetci[k] = o
            o += retci[k].length + 1
        }
        val out = HashMap<Int, Int>()
        val umetnuto = HashMap<Int, Int>() // po retku čistog teksta
        for (r in retci.indices) {
            val red = retci[r].codePoints().toArray()
            val uRetku = p.objekti.filter { r in it.redak..it.zadnjiRedak }.sortedBy { it.stupac }
            for (ob in uRetku) {
                val nastavak = (ob.zadnjiStupac + 1 until red.size).any { x ->
                    red[x] != ' '.code && uRetku.none { x in it.stupac..it.zadnjiStupac }
                }
                if (!nastavak) continue
                val utf16 = retci[r].offsetByCodePoints(0, ob.stupac)
                val cisto = p.izSirovog[pocetci[r] + utf16]
                val pocetakLinije = p.tekst.lastIndexOf('\n', cisto - 1) + 1
                val vec = umetnuto[pocetakLinije] ?: 0
                val sirina = ob.zadnjiStupac + 1 - (cisto - pocetakLinije + vec)
                if (sirina <= 0) continue
                out[cisto] = (out[cisto] ?: 0) + sirina
                umetnuto[pocetakLinije] = vec + sirina
            }
        }
        return out
    }

    fun undo() {
        if (readOnly) return
        povratak.ponisti(stanje())?.let(::applyState)
    }

    fun redo() {
        if (readOnly) return
        povratak.ponovi(stanje())?.let(::applyState)
    }

    // --- Odsječci i tablice (STANDARD.md, 4) ---

    /** Izmjena u tekstnom odsječku (retci [od, doo)) prevodi se na cijeli papir. */
    fun onSegmentChange(od: Int, doo: Int, value: TextFieldValue) {
        val tekst = content.text
        val stari = tekst.split("\n").subList(od, doo).joinToString("\n")
        val noviTekst = if (value.text == stari) tekst else Odsjecci.zamijeni(tekst, od, doo, value.text)
        val pomak = Odsjecci.pocetak(noviTekst, od)
        val sel = TextRange(value.selection.start + pomak, value.selection.end + pomak)
        val comp = value.composition?.let { TextRange(it.start + pomak, it.end + pomak) }
        onFieldChange(TextFieldValue(noviTekst, sel, comp))
    }

    /** Tekst jedne ćelije: tablica se iznova nacrta i prepiše u papir (stupac se po potrebi proširi). */
    fun editCell(tablica: Int, red: Int, stupac: Int, prije: String, celija: String) {
        val t = table(tablica) ?: return
        val stara = t.retci.getOrNull(red)?.getOrNull(stupac) ?: return
        // ništa ako se txt ne bi promijenio (npr. razmak na kraju reda koji ne širi stupac)
        val novi = Tablice.zapisi(t.saUpisanim(red, stupac, prije, celija))
        if (novi == Tablice.zapisi(t)) return
        replaceTable(tablica, novi.joinToString("\n"), Povratak.jeMalaIzmjena(stara, celija))
    }

    /** Radnja nad cijelom tablicom (redak, stupac); svaka je jedan korak za poništavanje. */
    fun editTable(tablica: Int, radnja: (Tablica) -> Tablica?) {
        val t = table(tablica) ?: return
        val nova = radnja(t) ?: return
        replaceTable(tablica, Tablice.zapisi(nova).joinToString("\n"), false)
    }

    /** Briše cijelu tablicu; na njenom mjestu ostaje prazan redak s kursorom. Može se poništiti. */
    fun deleteTable(tablica: Int) {
        if (table(tablica) == null) return
        replaceTable(tablica, "", false)
    }

    private fun table(tablica: Int): Tablica? {
        if (readOnly) return null
        val raspon = tables.getOrNull(tablica) ?: return null
        return Tablice.citaj(content.text.split("\n").subList(raspon.first, raspon.last + 1))
    }

    private fun replaceTable(tablica: Int, novi: String, malaIzmjena: Boolean) {
        val raspon = tables.getOrNull(tablica) ?: return
        val noviTekst = Odsjecci.zamijeni(content.text, raspon.first, raspon.last + 1, novi)
        povratak.prijeIzmjene(stanje(), SystemClock.uptimeMillis(), malaIzmjena)
        val od = Odsjecci.pocetak(noviTekst, raspon.first)
        val st = Oblik.prilagodi(content.text, styles, noviTekst).let { s ->
            s.mapIndexed { i, x -> if (i in od until od + novi.length) Stil.OBICNO else x }
        }
        setModel(TextFieldValue(noviTekst, TextRange(od)), st)
        refreshUndo()
        markDirty()
    }

    /** Nova prazna tablica ispod retka u kojem je kursor. Papir time dobiva ključ. */
    fun insertTable() {
        if (readOnly) return
        val tekst = content.text
        val redak = tekst.substring(0, content.selection.start.coerceIn(0, tekst.length)).count { it == '\n' }
        val linije = tekst.split("\n")
        val tablica = Tablice.zapisi(Tablica.nova()).joinToString("\n")
        // prazan redak se zamjenjuje tablicom; inače tablica dolazi ispod njega
        val prazan = linije.getOrNull(redak)?.isEmpty() == true
        val od = if (prazan) redak else redak + 1
        val noviTekst = Odsjecci.zamijeni(tekst, od, if (prazan) redak + 1 else od, tablica)
        povratak.prijeIzmjene(stanje(), SystemClock.uptimeMillis(), false)
        val st = Oblik.prilagodi(tekst, styles, noviTekst).let { s ->
            val a = Odsjecci.pocetak(noviTekst, od)
            s.mapIndexed { i, x -> if (i in a until a + tablica.length) Stil.OBICNO else x }
        }
        keyed = true
        setModel(TextFieldValue(noviTekst, TextRange(Odsjecci.pocetak(noviTekst, od))), st)
        refreshUndo()
        markDirty()
    }

    // --- Funkcije (PAPIR.md, ustav 4) ---

    /** Umeće trenutni datum i vrijeme na mjesto kursora (ili umjesto odabranog teksta). */
    fun insertDateTime() {
        if (readOnly) return
        val (novi, kursor) = Funkcije.umetni(
            content.text, content.selection.start, content.selection.end,
            Funkcije.datumIVrijeme(LocalDateTime.now()),
        )
        onFieldChange(TextFieldValue(novi, TextRange(kursor)))
    }

    // --- Kvačice i mape (STANDARD.md, 5.1 i 5.2) ---

    /**
     * Dodir na □ ■ ► ▼ na početku retka mijenja taj znak u datoteci. Vraća true ako je promijenjen.
     * Bez ključa su to obična slova.
     */
    fun toggleAt(pozicija: Int): Boolean {
        if (readOnly || !keyed) return false
        val tekst = content.text
        val p = listOf(pozicija, pozicija - 1).firstOrNull { Mape.prebaci(tekst, it) != null } ?: return false
        val novi = Mape.prebaci(tekst, p)!!
        povratak.prijeIzmjene(stanje(), SystemClock.uptimeMillis(), false)
        // zatvorena mapa: kursor iz skrivenog sadržaja ide na kraj zaglavlja
        var kursor = content.selection
        val retci = novi.split("\n")
        val redak = novi.substring(0, p).count { it == '\n' }
        val sadrzaj = Mape.sadrzaj(retci, redak)
        if (novi[p] == Mape.ZATVORENO && !sadrzaj.isEmpty()) {
            val od = Odsjecci.pocetak(novi, sadrzaj.first)
            val doo = Odsjecci.pocetak(novi, sadrzaj.last + 1)
            if (kursor.min in od..doo) {
                val kraj = od - 1
                kursor = TextRange(kraj)
            }
        }
        setModel(TextFieldValue(novi, kursor), styles)
        refreshUndo()
        markDirty()
        return true
    }

    /** Kvačica □ na početku retka s kursorom (iza uvlake). Papir time dobiva ključ. */
    fun insertCheckbox() = insertLineMarker(Mape.PRAZNO)

    /** Mapa ▼ na početku retka s kursorom: redak postaje zaglavlje, uvučeni retci ispod sadržaj. */
    fun insertFolder() = insertLineMarker(Mape.OTVORENO)

    private fun insertLineMarker(znak: Char) {
        if (readOnly) return
        val tekst = content.text
        val c = content.selection.min.coerceIn(0, tekst.length)
        val pocetak = tekst.lastIndexOf('\n', c - 1) + 1
        val kraj = tekst.indexOf('\n', c).let { if (it < 0) tekst.length else it }
        val redak = tekst.substring(pocetak, kraj)
        if (Mape.jeKvacica(redak) || Mape.zaglavlje(redak) != null) return
        val gdje = pocetak + Mape.uvlaka(redak)
        val novi = tekst.substring(0, gdje) + znak + " " + tekst.substring(gdje)
        povratak.prijeIzmjene(stanje(), SystemClock.uptimeMillis(), false)
        keyed = true
        val st = Oblik.prilagodi(tekst, styles, novi, Stil.OBICNO)
        setModel(TextFieldValue(novi, TextRange(c + 2)), st)
        refreshUndo()
        markDirty()
    }

    // --- Poveznica (STANDARD.md, 5.3) ---

    /** Cilj poveznice na mjestu kursora (u njoj, na početku ili na kraju), ili null. */
    val linkAtCursor: String?
        get() {
            val c = content.selection.min
            return styles.getOrNull(c)?.poveznica ?: styles.getOrNull(c - 1)?.poveznica
        }

    /** Odabrani tekst, za prijedlog teksta poveznice. */
    val selectedText: String get() = content.text.substring(content.selection.min, content.selection.max)

    /** Umeće poveznicu umjesto odabranog teksta (ili na kursor). */
    fun insertLink(tekst: String, cilj: String) {
        if (readOnly) return
        val t = tekst.replace('\n', ' ')
        val c = cilj.trim()
        if (t.isEmpty() || !Oblik.ispravanCilj(c)) {
            message = "Poveznica treba tekst i cilj (bez znakova → ← │)."
            return
        }
        val stari = content.text
        val a = content.selection.min
        val b = content.selection.max
        val novi = stari.substring(0, a) + t + stari.substring(b)
        povratak.prijeIzmjene(stanje(), SystemClock.uptimeMillis(), false)
        val baza = styles.getOrElse(a) { Stil.OBICNO }.copy(poveznica = null)
        val st = Oblik.prilagodi(stari, styles, novi).toMutableList()
        for (i in a until a + t.length) st[i] = baza.copy(poveznica = c, velicina = st[i].velicina)
        setModel(TextFieldValue(novi, TextRange(a + t.length)), st)
        refreshUndo()
        markDirty()
    }

    /** Uklanja poveznicu na mjestu kursora; tekst ostaje. */
    fun removeLink() {
        if (readOnly) return
        val cilj = linkAtCursor ?: return
        var a = content.selection.min
        var b = a
        while (a > 0 && styles.getOrNull(a - 1)?.poveznica == cilj) a--
        while (b < styles.size && styles[b].poveznica == cilj) b++
        povratak.prijeIzmjene(stanje(), SystemClock.uptimeMillis(), false)
        setModel(content, Oblik.primijeni(content.text, styles, a, b) { it.copy(poveznica = null) })
        refreshUndo()
        markDirty()
    }

    /** Papir iz mape za papire s točno tim imenom; null ako ga nema. */
    fun openPaperByName(ime: String) = op {
        val dir = folder
        val uri = if (dir == null) null else io { findInFolder(dir, ime) }
        if (uri == null) {
            message = "Papir „$ime“ nije pronađen u mapi za papire."
            return@op
        }
        if (!saveBeforeLeaving()) return@op
        if (!load(uri)) message = "Datoteku nije moguće otvoriti."
    }

    private fun findInFolder(tree: Uri, ime: String): Uri? = try {
        val djeca = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        resolver.query(
            djeca,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null,
        )?.use { c ->
            var nadjeno: Uri? = null
            while (c.moveToNext()) {
                if (c.getString(1) == ime) {
                    nadjeno = DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0))
                    break
                }
            }
            nadjeno
        }
    } catch (e: Exception) {
        null
    }

    // --- Slike i kutije (STANDARD.md, 6) ---

    /** Retci tijela bez objekata, kako ih vidi pravilo slobodnog mjesta. */
    private fun flowRows(): List<String> {
        val z = Oblik.zapisi(content.text, styles, lines, keyed, format).sirovo
        return (Oblik.tijelo(z) ?: z).split("\n")
    }

    /** Redak mreže (tijelo datoteke) u kojem počinje redak čistog teksta. */
    fun rawRowOfLine(linija: Int): Int = lines.take(linija).sumOf { it.izvor.count { c -> c == '\n' } + 1 }

    /** Redak čistog teksta kojem pripada redak mreže. */
    fun lineOfRawRow(redak: Int): Int {
        var r = 0
        for ((i, l) in lines.withIndex()) {
            r += l.izvor.count { it == '\n' } + 1
            if (redak < r) return i
        }
        return maxOf(0, lines.size - 1 + (redak - r + 1))
    }

    /** Prvo slobodno mjesto za objekt, od retka ispod kursora nadolje, uz lijevi rub. */
    private fun placeFree(o: Objekt): Objekt {
        val retci = flowRows()
        val kursor = content.text.substring(0, content.selection.min.coerceIn(0, content.text.length)).count { it == '\n' }
        val od = rawRowOfLine(kursor) + 1
        for (r in od..retci.size + 1) {
            val kandidat = o.na(r, 0)
            if (Objekti.slobodno(kandidat, retci, objects)) return kandidat
        }
        return o.na(retci.size + 1, 0)
    }

    private fun applyObjects(novi: List<Objekt>, izabrani: Objekt?) {
        povratak.prijeIzmjene(stanje(), SystemClock.uptimeMillis(), false)
        keyed = true
        selected = izabrani
        setModel(content, styles, novi)
        selected = izabrani?.let { s -> objects.firstOrNull { it == s } }
        refreshUndo()
        markDirty()
    }

    /** Nova slika iz galerije: ulazi u paket, a okvir na prvo slobodno mjesto ispod kursora. */
    fun insertImage(uri: Uri) = op {
        if (readOnly) return@op
        val bajtovi = io { read(uri) } ?: run {
            message = "Sliku nije moguće pročitati."
            return@op
        }
        val ime = Paket.slobodnoIme(io { displayName(uri) }, attachments.keys + removedAttachments.keys)
        val (w, h) = io { imageSize(bajtovi) }
        val sirina = 24
        // znak je otprilike dvaput viši nego širok
        val visina = if (w > 0 && h > 0) (sirina * h / (2.0 * w)).toInt().coerceIn(3, 30) else 8
        attachments = LinkedHashMap(attachments).also { it[ime] = bajtovi }
        if (!isPackage) {
            isPackage = true
            convertToPackage = target != null
        }
        val o = placeFree(Objekt.slika(0, 0, sirina, visina, ime))
        applyObjects(objects + o, o)
    }

    private fun imageSize(b: ByteArray): Pair<Int, Int> {
        val o = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeByteArray(b, 0, b.size, o)
        return o.outWidth to o.outHeight
    }

    /** Nova prazna kutija teksta na prvo slobodno mjesto ispod kursora. */
    fun insertBox() {
        if (readOnly) return
        val o = placeFree(Objekt.kutija(0, 0, ""))
        applyObjects(objects + o, o)
    }

    fun select(o: Objekt?) {
        selected = o
        ghost = null
    }

    /** Počinje pomicanje ili mijenjanje veličine; ništa se ne mijenja dok se ne potvrdi. */
    fun startGhost(mode: GhostMode) {
        val s = selected ?: return
        ghostMode = mode
        ghost = s
    }

    /** Strelica: pomak za jedan znak ili redak (ili promjena širine i visine). */
    fun nudge(dx: Int, dy: Int) {
        val g = ghost ?: return
        ghost = when (ghostMode) {
            GhostMode.MOVE -> g.na(maxOf(0, g.redak + dy), maxOf(0, g.stupac + dx))
            GhostMode.SIZE -> Objekt.slika(g.redak, g.stupac, g.sirina + dx, g.visina + dy, g.sadrzaj)
        }
    }

    /** Je li novi položaj dopušten: slika ne smije stati na tekst ni na drugi objekt. */
    val ghostFree: Boolean
        get() {
            val g = ghost ?: return false
            val s = selected ?: return false
            return Objekti.slobodno(g, flowRows(), objects - s)
        }

    fun confirmGhost() {
        val g = ghost ?: return
        val s = selected ?: return
        if (g == s) {
            ghost = null
            return
        }
        if (!ghostFree) {
            message = "Tu je tekst ili drugi objekt. Pomakni dalje."
            return
        }
        ghost = null
        applyObjects(objects.map { if (it == s) g else it }, g)
    }

    fun cancelGhost() {
        ghost = null
    }

    fun deleteSelected() {
        val s = selected ?: return
        if (s.vrsta == Objekt.Vrsta.SLIKA && objects.count { it.sadrzaj == s.sadrzaj && it.vrsta == s.vrsta } == 1) {
            attachments[s.sadrzaj]?.let { removedAttachments[s.sadrzaj] = it }
            attachments = LinkedHashMap(attachments).also { it.remove(s.sadrzaj) }
        }
        ghost = null
        applyObjects(objects - s, null)
    }

    /** Novo ime slike: mijenja se i u okviru i u paketu. */
    fun renameImage(novo: String) {
        val s = selected ?: return
        if (s.vrsta != Objekt.Vrsta.SLIKA) return
        val ime = novo.trim()
        if (ime.isEmpty() || ime == s.sadrzaj) return
        if (ime.any { it == '/' || it == '\\' || it == '\n' } || ime.endsWith(".pap", true) || ime.endsWith(".txt", true)) {
            message = "To ime ne može biti ime slike."
            return
        }
        if (ime in attachments.keys) {
            message = "Slika s tim imenom već postoji u papiru."
            return
        }
        val bajtovi = attachments[s.sadrzaj]
        attachments = LinkedHashMap<String, ByteArray>().also { m ->
            for ((k, v) in attachments) m[if (k == s.sadrzaj) ime else k] = v
            if (bajtovi == null) removedAttachments[s.sadrzaj]?.let { m[ime] = it }
        }
        val o = Objekt.slika(s.redak, s.stupac, s.sirina, s.visina, ime)
        applyObjects(objects.map { if (it == s) o else it }, o)
    }

    /** Tekst u kutiji: kutija je široka koliko njezin najdulji red, kao ćelija. */
    fun editBox(kutija: Objekt, tekst: String) {
        if (readOnly) return
        val o = Objekt.kutija(kutija.redak, kutija.stupac, tekst)
        if (o == kutija) return
        val bio = objects.firstOrNull { it == kutija } ?: return
        povratak.prijeIzmjene(stanje(), SystemClock.uptimeMillis(), Povratak.jeMalaIzmjena(bio.sadrzaj, tekst))
        selected = o
        setModel(content, styles, objects.map { if (it == bio) o else it })
        selected = objects.firstOrNull { it == o }
        refreshUndo()
        markDirty()
    }

    private fun restoreAttachments() {
        val potrebno = objects.filter { it.vrsta == Objekt.Vrsta.SLIKA }.map { it.sadrzaj }
        val nedostaje = potrebno.filter { it !in attachments && it in removedAttachments }
        if (nedostaje.isNotEmpty()) {
            attachments = LinkedHashMap(attachments).also { m -> for (n in nedostaje) m[n] = removedAttachments.getValue(n) }
        }
    }

    // --- Traži i zamijeni (PAPIR.md, 5) ---

    /** Dijelovi teksta koji se ne pretražuju: tablice i sadržaj zatvorenih mapa. */
    private fun excludedRanges(): List<IntRange> {
        val t = content.text
        return (tables + hidden).map { r -> Odsjecci.pocetak(t, r.first) until Odsjecci.pocetak(t, r.last + 1) }
    }

    fun search(upit: String, velikaMala: Boolean = matchCase) {
        searchQuery = upit
        matchCase = velikaMala
        refreshSearch()
        currentMatch = matches.indexOfFirst { it.first >= content.selection.min }.let { if (it < 0 && matches.isNotEmpty()) 0 else it }
        showMatch()
    }

    private fun refreshSearch() {
        if (searchQuery.isEmpty()) {
            matches = emptyList()
            currentMatch = -1
            return
        }
        matches = Trazi.nadji(content.text, searchQuery, matchCase, excludedRanges())
        if (currentMatch >= matches.size) currentMatch = matches.size - 1
    }

    fun nextMatch(korak: Int) {
        if (matches.isEmpty()) return
        currentMatch = ((currentMatch + korak) % matches.size + matches.size) % matches.size
        showMatch()
    }

    private fun showMatch() {
        val m = matches.getOrNull(currentMatch) ?: return
        content = content.copy(selection = TextRange(m.first, m.last + 1))
    }

    fun replaceCurrent(novo: String) {
        if (readOnly) return
        val m = matches.getOrNull(currentMatch) ?: return
        val stari = content.text
        val novi = stari.substring(0, m.first) + novo + stari.substring(m.last + 1)
        povratak.prijeIzmjene(stanje(), SystemClock.uptimeMillis(), false)
        val st = Oblik.prilagodi(stari, styles, novi, styles.getOrNull(m.first))
        setModel(TextFieldValue(novi, TextRange(m.first + novo.length)), st)
        currentMatch = matches.indexOfFirst { it.first >= m.first + novo.length }.let { if (it < 0 && matches.isNotEmpty()) 0 else it }
        showMatch()
        refreshUndo()
        markDirty()
    }

    fun replaceAll(novo: String) {
        if (readOnly) return
        val stari = content.text
        val nadjeno = matches
        if (nadjeno.isEmpty()) return
        val sb = StringBuilder()
        val st = ArrayList<Stil>()
        var od = 0
        for (m in nadjeno) {
            sb.append(stari, od, m.first)
            st.addAll(styles.subList(od, m.first))
            val stil = styles.getOrElse(m.first) { Stil.OBICNO }
            for (c in novo) st.add(if (c == '\n') Stil.OBICNO else stil)
            sb.append(novo)
            od = m.last + 1
        }
        sb.append(stari, od, stari.length)
        st.addAll(styles.subList(od, stari.length))
        povratak.prijeIzmjene(stanje(), SystemClock.uptimeMillis(), false)
        setModel(TextFieldValue(sb.toString(), TextRange(0)), st)
        message = "Zamijenjeno: ${nadjeno.size}."
        refreshUndo()
        markDirty()
    }

    fun endSearch() {
        searchQuery = ""
        refreshSearch()
    }

    /** Pozvati kad aplikacija ide u pozadinu. */
    fun saveNow() {
        saveJob?.cancel()
        op { saveLocked() }
    }

    /**
     * Pozvati kad se aplikacija vrati u prvi plan. Ako je datoteka promijenjena izvana,
     * a ovdje nema ničeg nespremljenog, prikazuje se njeno stanje: datoteka je istina.
     * Prethodni prikaz ostaje u poništavanju.
     */
    fun checkExternal() = op {
        val uri = target ?: return@op
        val known = diskBytes ?: return@op
        if (dirty || conflict || readOnly) return@op
        val onDisk = io { read(uri) } ?: return@op
        if (onDisk.contentEquals(known)) return@op
        povratak.prijeIzmjene(stanje(), SystemClock.uptimeMillis(), false)
        applyBytes(onDisk, keepCursor = true)
        readOnly = notText || !io { canWrite(uri) }
        message = "Datoteka je promijenjena izvan Papira. Prikazano je novo stanje."
    }

    // --- Sukob: odlučuje čovjek, druga verzija postaje vidljiva datoteka ---

    /** Ova verzija ostaje u datoteci; ona izvana sprema se pored nje. */
    fun keepMine() = op {
        val t = theirs ?: return@op
        val savedAs = saveConflictCopy(t) ?: return@op
        diskBytes = t
        theirs = null
        conflict = false
        dirty = true
        saveLocked()
        message = "Verzija izvana spremljena je kao „$savedAs“."
    }

    /** Verzija izvana ostaje u datoteci; ova se sprema pored nje. */
    fun loadTheirs() = op {
        val t = theirs ?: return@op
        val uri = target ?: return@op
        val savedAs = saveConflictCopy(currentBytes(text)) ?: return@op
        povratak.prijeIzmjene(stanje(), SystemClock.uptimeMillis(), false)
        theirs = null
        conflict = false
        applyBytes(t, keepCursor = true)
        readOnly = notText || !io { canWrite(uri) }
        message = "Tvoja verzija spremljena je kao „$savedAs“."
    }

    // --- Izbornik ---

    /** Mapa u kojoj nastaju papiri. Bira se jednom. */
    fun setFolder(tree: Uri) = op {
        val rw = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            resolver.takePersistableUriPermission(tree, rw)
        } catch (e: Exception) {
            message = "Papir ne smije pisati u tu mapu. Odaberi drugu."
            return@op
        }
        folder = tree
        hasFolder = true
        prefs.edit().putString(KEY_FOLDER, tree.toString()).apply()
        moveLegacyDraft()
    }

    fun newPaper() = op {
        if (!saveBeforeLeaving()) return@op
        startEmpty()
    }

    fun open(uri: Uri, fromPicker: Boolean) = op {
        if (!saveBeforeLeaving()) return@op
        if (fromPicker) persist(uri)
        if (!load(uri)) message = "Datoteku nije moguće otvoriti."
    }

    /** Trenutni tekst postaje nova datoteka na mjestu i s imenom koje je korisnik izabrao. */
    fun saveAs(uri: Uri) = op {
        persist(uri)
        target = uri
        diskBytes = null // nova datoteka: nema starije verzije s kojom bi bila u sukobu
        theirs = null
        conflict = false
        readOnly = false
        notText = false
        dirty = true
        if (saveLocked()) {
            title = io { displayName(uri) }
            canRename = io { supportsRename(uri) }
            prefs.edit().putString(KEY_TARGET, uri.toString()).apply()
        }
    }

    /** Novo ime je točno ono što je korisnik upisao, slovo za slovo (ustav 8). */
    fun rename(newName: String) = op {
        val uri = target ?: return@op
        if (newName.isEmpty() || newName == title) return@op
        saveLocked()
        val renamed = io {
            try {
                DocumentsContract.renameDocument(resolver, uri, newName)
            } catch (e: Exception) {
                null
            }
        }
        if (renamed == null) {
            message = "Preimenovanje nije uspjelo."
            return@op
        }
        if (renamed != uri) persist(renamed)
        target = renamed
        val actual = io { displayName(renamed) }
        title = actual
        prefs.edit().putString(KEY_TARGET, renamed.toString()).apply()
        if (actual != newName) message = "Sustav je datoteku nazvao „$actual“."
    }

    fun suggestedName(): String = PapirFile.suggestName(content.text).let {
        if (isPackage) it.substringBeforeLast('.') + Paket.NASTAVAK else it
    }

    // --- Unutarnje ---

    private fun op(block: suspend () -> Unit) {
        AppScope.scope.launch { AppScope.lock.withLock { block() } }
    }

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    /** Za poništavanje: tekst kakav ide u datoteku (s oznakama) i kursor u čistom tekstu. */
    private fun stanje() = Stanje(text, content.selection.start, content.selection.end)

    private fun applyState(s: Stanje) {
        val p = Oblik.citaj(s.text)
        content = TextFieldValue(p.tekst, TextRange(s.start, s.end))
        takeRead(s.text, p)
        restoreAttachments()
        refreshSearch()
        pendingStyle = null
        refreshUndo()
        markDirty()
    }

    private fun refreshUndo() {
        canUndo = povratak.moguPonistiti
        canRedo = povratak.moguPonoviti
    }

    private fun markDirty() {
        dirty = true
        saveJob?.cancel()
        saveJob = AppScope.scope.launch {
            delay(600)
            AppScope.lock.withLock { saveLocked() }
        }
    }

    private fun resetTo(value: TextFieldValue) {
        content = value
        styles = List(value.text.length) { Stil.OBICNO }
        lines = emptyList()
        tables = emptyList()
        keyed = false
        format = 0
        objects = emptyList()
        mreza = null
        hidden = emptyList()
        gaps = emptyMap()
        isPackage = false
        attachments = emptyMap()
        removedAttachments.clear()
        packageTextName = null
        convertToPackage = false
        selected = null
        ghost = null
        pendingStyle = null
        lineEnding = LineEnding.CRLF
        hasBom = false
        readOnly = false
        notText = false
        dirty = false
        conflict = false
        theirs = null
        diskBytes = null
        povratak.ocisti()
        refreshUndo()
    }

    /** Prazan papir. Datoteka nastaje tek s prvim slovom, da se ne gomilaju prazne datoteke. */
    private fun startEmpty() {
        resetTo(TextFieldValue(""))
        target = null
        title = null
        canRename = false
        prefs.edit().remove(KEY_TARGET).apply()
    }

    /**
     * Nacrt iz 0.1/0.2 postaje prava datoteka u mapi, i tek nakon uspješnog zapisa
     * briše se iz aplikacije. Ako je otvoren prazan papir, nacrt se odmah prikazuje.
     */
    private suspend fun moveLegacyDraft() {
        val dir = folder ?: return
        val old = io {
            try {
                if (legacyDraft.baseFile.exists()) PapirFile.decode(legacyDraft.readFully())?.text.orEmpty() else ""
            } catch (e: Exception) {
                ""
            }
        }
        if (old.isEmpty()) {
            io { legacyDraft.delete() }
            return
        }
        val bytes = PapirFile.encode(old, LineEnding.CRLF, false)
        val created = io {
            try {
                createIn(dir, PapirFile.NEW_NAME)?.also { writeUri(it, bytes) }
            } catch (e: Exception) {
                null
            }
        } ?: return // nacrt ostaje gdje je; pokušat će se ponovno
        io { legacyDraft.delete() }
        if (target == null && text.isEmpty()) {
            if (load(created)) message = "Tvoj nacrt sada je datoteka „$title“ u mapi za papire."
        } else {
            message = "Tvoj stari nacrt spremljen je kao „${io { displayName(created) }}“ u mapi za papire."
        }
    }

    /** Druga verzija kod sukoba, kao vidljiva datoteka u mapi za papire. Vraća njeno ime. */
    private suspend fun saveConflictCopy(bytes: ByteArray): String? {
        val dir = folder
        val name = PapirFile.conflictName(title ?: PapirFile.NEW_NAME)
        val created = if (dir == null) null else io {
            try {
                createIn(dir, name)?.also { writeUri(it, bytes) }
            } catch (e: Exception) {
                null
            }
        }
        if (created == null) {
            message = "Drugu verziju nije moguće spremiti kao zasebnu datoteku. Ništa nije prepisano."
            return null
        }
        return io { displayName(created) }
    }

    /** Papir se napušta samo ako je sve spremljeno; inače ostaje na ekranu. */
    private suspend fun saveBeforeLeaving(): Boolean {
        if (saveLocked()) return true
        if (conflict) message = "Datoteka je promijenjena izvana. Prvo odaberi koju verziju zadržati."
        return false
    }

    private enum class Ishod { OK, SUKOB, GRESKA }

    /** Vraća true ako je sve spremljeno. Nikad ne piše ako tekst nije mijenjan. */
    private suspend fun saveLocked(): Boolean {
        if (!dirty) return true
        if (readOnly || conflict) return false
        val snapshot = text
        val bytes = currentBytes(snapshot)
        val uri = target

        val ishod = if (uri == null) {
            // Prvo slovo novog papira: nastaje prava datoteka u mapi.
            if (snapshot.isEmpty()) return true
            val dir = folder ?: run {
                message = "Odaberi mapu za papire da bi se tekst mogao spremiti."
                return false
            }
            val created = io {
                try {
                    createIn(dir, if (isPackage) PapirFile.NEW_NAME.substringBeforeLast('.') + Paket.NASTAVAK else PapirFile.NEW_NAME)
                } catch (e: Exception) {
                    null
                }
            }
            if (created == null) {
                Ishod.GRESKA
            } else {
                val r = io { attempt { writeUri(created, bytes) } }
                if (r == Ishod.OK) {
                    target = created
                    title = io { displayName(created) }
                    canRename = io { supportsRename(created) }
                    prefs.edit().putString(KEY_TARGET, created.toString()).apply()
                }
                r
            }
        } else {
            io {
                val known = diskBytes
                val onDisk = if (known != null) read(uri) else null
                if (known != null && onDisk != null && !onDisk.contentEquals(known)) {
                    theirs = onDisk
                    Ishod.SUKOB
                } else {
                    attempt { writeUri(uri, bytes) }
                }
            }
        }

        when (ishod) {
            Ishod.OK -> {
                diskBytes = bytes
                if (text == snapshot) dirty = false
                if (convertToPackage) renameToPackage()
            }
            Ishod.SUKOB -> conflict = true
            Ishod.GRESKA -> message = "Spremanje nije uspjelo. Tekst je i dalje ovdje."
        }
        return ishod == Ishod.OK
    }

    /** Bajtovi za datoteku: goli tekst, ili paket s tekstom i slikama. */
    private fun currentBytes(snapshot: String): ByteArray {
        val tekst = PapirFile.encode(snapshot, lineEnding, hasBom)
        if (!isPackage) return tekst
        val ime = packageTextName ?: ((title?.substringBeforeLast('.') ?: "Note") + ".pap").also { packageTextName = it }
        return Paket.zapisi(Paket.Sadrzaj(ime, tekst, LinkedHashMap(attachments)))
    }

    /** Papir koji je dobio prvu sliku postaje .pzp: zip je zip (STANDARD.md, 2.3). */
    private suspend fun renameToPackage() {
        convertToPackage = false
        val uri = target ?: return
        val ime = title ?: return
        if (ime.endsWith(Paket.NASTAVAK, ignoreCase = true)) return
        val novo = (if (ime.contains('.')) ime.substringBeforeLast('.') else ime) + Paket.NASTAVAK
        val renamed = io {
            try {
                DocumentsContract.renameDocument(resolver, uri, novo)
            } catch (e: Exception) {
                null
            }
        }
        if (renamed == null) {
            message = "Papir sa slikama je zip, ali preimenovanje u „$novo“ nije uspjelo."
            return
        }
        if (renamed != uri) persist(renamed)
        target = renamed
        title = io { displayName(renamed) }
        prefs.edit().putString(KEY_TARGET, renamed.toString()).apply()
        message = "Papir sa slikom sada je paket „$title“."
    }

    private fun attempt(write: () -> Unit): Ishod = try {
        write()
        Ishod.OK
    } catch (e: Exception) {
        Ishod.GRESKA
    }

    private fun writeUri(uri: Uri, bytes: ByteArray) {
        // "wt" skraćuje datoteku; bez toga bi kraći tekst ostavio ostatke starog.
        val stream = resolver.openOutputStream(uri, "wt")
            ?: throw IllegalStateException("Nema izlaznog toka")
        stream.use { it.write(bytes) }
    }

    private fun read(uri: Uri): ByteArray? = try {
        resolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (e: Exception) {
        null
    }

    private fun createIn(tree: Uri, name: String): Uri? {
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        // application/octet-stream: sustav ne dodaje svoj nastavak; za postojeće ime sam dodaje broj.
        return DocumentsContract.createDocument(resolver, parent, "application/octet-stream", name)
    }

    private suspend fun load(uri: Uri): Boolean {
        val bytes = io { read(uri) } ?: return false
        resetTo(TextFieldValue(""))
        applyBytes(bytes, keepCursor = false)
        readOnly = notText || !io { canWrite(uri) }
        if (readOnly && !notText) message = "Otvoreno samo za čitanje. Za izmjene: Spremi kao…"
        target = uri
        title = io { displayName(uri) }
        canRename = !readOnly && io { supportsRename(uri) }
        if (readOnly) prefs.edit().remove(KEY_TARGET).apply()
        else prefs.edit().putString(KEY_TARGET, uri.toString()).apply()
        return true
    }

    private fun applyBytes(bytes: ByteArray, keepCursor: Boolean) {
        // kao Notepad: otvorena datoteka počinje s kursorom na početku
        val cursor = if (keepCursor) content.selection else TextRange.Zero
        val paket = if (Paket.jePaket(bytes)) Paket.citaj(bytes) else null
        isPackage = paket != null
        attachments = paket?.prilozi ?: emptyMap()
        packageTextName = paket?.imeTeksta
        val decoded = if (Paket.jePaket(bytes) && paket == null) null else PapirFile.decode(paket?.tekst ?: bytes)
        if (decoded == null) {
            val raw = String(bytes, Charsets.UTF_8)
            content = TextFieldValue(raw)
            styles = List(raw.length) { Stil.OBICNO }
            lines = emptyList()
            tables = emptyList()
            keyed = false
            format = 0
            objects = emptyList()
            mreza = null
            hidden = emptyList()
            gaps = emptyMap()
            notText = true
            message = if (Paket.jePaket(bytes)) "Ovaj zip nije papir. Otvoreno samo za čitanje."
            else "Ovo nije UTF-8 tekst. Otvoreno samo za čitanje."
        } else {
            // Oznake i ključ se ne prikazuju; prikazuje se ono što znače (STANDARD.md, 3).
            val p = Oblik.citaj(decoded.text)
            content = TextFieldValue(p.tekst, cursor)
            takeRead(decoded.text, p)
            lineEnding = decoded.lineEnding
            hasBom = decoded.hasBom
            notText = false
        }
        diskBytes = bytes
        dirty = false
        pendingStyle = null
        refreshUndo()
    }

    private fun persist(uri: Uri) {
        val rw = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            resolver.takePersistableUriPermission(uri, rw)
        } catch (e: Exception) {
            try {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
        }
    }

    private fun hasPersistedWrite(uri: Uri): Boolean =
        resolver.persistedUriPermissions.any { it.uri == uri && it.isWritePermission }

    private fun documentFlags(uri: Uri): Int? = try {
        resolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_FLAGS), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getInt(0) else null
        }
    } catch (e: Exception) {
        null
    }

    private fun canWrite(uri: Uri): Boolean {
        val granted = getApplication<Application>().checkUriPermission(
            uri, Process.myPid(), Process.myUid(), Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return false
        val flags = documentFlags(uri) ?: return true
        return flags and DocumentsContract.Document.FLAG_SUPPORTS_WRITE != 0
    }

    private fun supportsRename(uri: Uri): Boolean {
        val flags = documentFlags(uri) ?: return false
        return flags and DocumentsContract.Document.FLAG_SUPPORTS_RENAME != 0
    }

    private fun displayName(uri: Uri): String {
        try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val name = c.getString(0)
                    if (!name.isNullOrEmpty()) return name
                }
            }
        } catch (_: Exception) {
        }
        return uri.lastPathSegment ?: "datoteka"
    }

    private companion object {
        const val KEY_TARGET = "target"
        const val KEY_FOLDER = "folder"
    }
}
