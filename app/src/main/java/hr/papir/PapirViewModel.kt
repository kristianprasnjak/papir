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

    /** Tekst kakav se uređuje (krajevi redaka su uvijek \n), zajedno s kursorom i odabirom. */
    var content by mutableStateOf(TextFieldValue(""))
        private set

    val text: String get() = content.text

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
        if (changed) {
            povratak.prijeIzmjene(stanje(), SystemClock.uptimeMillis(), Povratak.jeMalaIzmjena(content.text, new.text))
        }
        content = new
        if (!changed) return // samo pomak kursora ili odabira: ništa se ne sprema
        refreshUndo()
        markDirty()
    }

    fun undo() {
        if (readOnly) return
        povratak.ponisti(stanje())?.let(::applyState)
    }

    fun redo() {
        if (readOnly) return
        povratak.ponovi(stanje())?.let(::applyState)
    }

    // --- Funkcije (SPEC.md, 3) ---

    /** Umeće trenutni datum i vrijeme na mjesto kursora (ili umjesto odabranog teksta). */
    fun insertDateTime() {
        if (readOnly) return
        val (novi, kursor) = Funkcije.umetni(
            content.text, content.selection.start, content.selection.end,
            Funkcije.datumIVrijeme(LocalDateTime.now()),
        )
        onFieldChange(TextFieldValue(novi, TextRange(kursor)))
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
        val savedAs = saveConflictCopy(PapirFile.encode(text, lineEnding, hasBom)) ?: return@op
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

    fun suggestedName(): String = PapirFile.suggestName(text)

    // --- Unutarnje ---

    private fun op(block: suspend () -> Unit) {
        AppScope.scope.launch { AppScope.lock.withLock { block() } }
    }

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    private fun stanje() = Stanje(content.text, content.selection.start, content.selection.end)

    private fun applyState(s: Stanje) {
        content = TextFieldValue(s.text, TextRange(s.start, s.end))
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
        val bytes = PapirFile.encode(snapshot, lineEnding, hasBom)
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
                    createIn(dir, PapirFile.NEW_NAME)
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
            }
            Ishod.SUKOB -> conflict = true
            Ishod.GRESKA -> message = "Spremanje nije uspjelo. Tekst je i dalje ovdje."
        }
        return ishod == Ishod.OK
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
        val decoded = PapirFile.decode(bytes)
        if (decoded == null) {
            content = TextFieldValue(String(bytes, Charsets.UTF_8))
            notText = true
            message = "Ovo nije UTF-8 tekst. Otvoreno samo za čitanje."
        } else {
            content = TextFieldValue(decoded.text, cursor)
            lineEnding = decoded.lineEnding
            hasBom = decoded.hasBom
            notText = false
        }
        diskBytes = bytes
        dirty = false
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
