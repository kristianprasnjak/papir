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

enum class Pending { NEW, OPEN }

/**
 * Ustav, pravilo „Ništa se ne gubi":
 * - svaki papir je prava datoteka u mapi koju je korisnik izabrao (nacrt unutar aplikacije
 *   postoji samo dok mapa nije izabrana);
 * - prije svakog zapisa provjerava se je li datoteka promijenjena izvana; ako jest, ništa
 *   se ne prepisuje dok čovjek ne odluči, a obje verzije ostaju sačuvane;
 * - poništavanje, i povijest verzija kao sigurnosna mreža.
 */
class PapirViewModel(app: Application) : AndroidViewModel(app) {

    private val resolver = app.contentResolver
    private val prefs = app.getSharedPreferences("papir", Context.MODE_PRIVATE)
    private val draft = AtomicFile(File(app.filesDir, "papir.txt"))
    private val povijest = Povijest(File(app.filesDir, "povijest"))
    private val povratak = Povratak()

    /** Tekst kakav se uređuje (krajevi redaka su uvijek \n), zajedno s kursorom i odabirom. */
    var content by mutableStateOf(TextFieldValue(""))
        private set

    val text: String get() = content.text

    /** Ime datoteke; null znači papir koji još nije datoteka. */
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

    /** Otvoren popis prethodnih verzija (null: zatvoren). */
    var versions by mutableStateOf<List<Povijest.Verzija>?>(null)

    var message by mutableStateOf<String?>(null)

    /** Radnja koja čeka da korisnik odluči što s nespremljenim nacrtom. */
    var pending by mutableStateOf<Pending?>(null)

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

    /** Je li izvorna verzija otvorene datoteke već spremljena u povijest. */
    private var originalKept = false

    /** Papir koji živi samo u aplikaciji jer mapa još nije izabrana. */
    val isUnsavedDraft: Boolean get() = target == null && folder == null && text.isNotEmpty()

    init {
        folder = prefs.getString(KEY_FOLDER, null)?.let(Uri::parse)?.takeIf(::hasPersistedWrite)
        hasFolder = folder != null
        op {
            val last = prefs.getString(KEY_TARGET, null)?.let(Uri::parse)
            if (last == null || !load(last)) loadDraft()
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
     * Ono što je zamijenjeno ostaje u poništavanju i u povijesti.
     */
    fun checkExternal() = op {
        val uri = target ?: return@op
        val known = diskBytes ?: return@op
        if (dirty || conflict || readOnly) return@op
        val onDisk = io { read(uri) } ?: return@op
        if (onDisk.contentEquals(known)) return@op
        val key = keyOf(uri)
        val replaced = text
        io { povijest.spremi(key, replaced) }
        povratak.prijeIzmjene(stanje(), SystemClock.uptimeMillis(), false)
        applyBytes(onDisk, keepCursor = true)
        readOnly = notText || !io { canWrite(uri) }
        message = "Datoteka je promijenjena izvan Papira. Prikazano je novo stanje."
    }

    // --- Sukob ---

    /** Moja verzija ostaje; ona s diska ide u povijest. */
    fun keepMine() = op {
        val t = theirs ?: return@op
        val uri = target ?: return@op
        io { povijest.spremi(keyOf(uri), PapirFile.decode(t)?.text ?: String(t, Charsets.UTF_8)) }
        diskBytes = t
        theirs = null
        conflict = false
        dirty = true
        saveLocked()
    }

    /** Verzija s diska ostaje; moja ide u povijest i u poništavanje. */
    fun loadTheirs() = op {
        val t = theirs ?: return@op
        val uri = target ?: return@op
        val mine = text
        io { povijest.spremi(keyOf(uri), mine) }
        povratak.prijeIzmjene(stanje(), SystemClock.uptimeMillis(), false)
        theirs = null
        conflict = false
        applyBytes(t, keepCursor = true)
        readOnly = notText || !io { canWrite(uri) }
    }

    // --- Izbornik ---

    /** Mapa u kojoj nastaju papiri. Bira se jednom; nacrt koji postoji odmah postaje datoteka u njoj. */
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
        if (target == null && text.isNotEmpty()) {
            dirty = true
            saveLocked()
        }
    }

    fun newPaper() = op {
        if (!saveBeforeLeaving()) return@op
        if (folder == null) {
            // Bez mape novi papir je nacrt. Stari nacrt se nikad ne briše bez traga.
            io { clearDraft(keptAs = null) }
        }
        resetTo(TextFieldValue(""))
        target = null
        title = null
        canRename = false
        prefs.edit().remove(KEY_TARGET).apply()
    }

    fun open(uri: Uri, fromPicker: Boolean) = op {
        if (!saveBeforeLeaving()) return@op
        val leavingDraft = target == null && folder == null
        if (fromPicker) persist(uri)
        if (!load(uri)) {
            message = "Datoteku nije moguće otvoriti."
            return@op
        }
        // Napušteni nacrt ne nestaje: ide u Prethodne verzije nacrta.
        if (leavingDraft) io { clearDraft(keptAs = null) }
    }

    /** Trenutni tekst postaje nova datoteka na mjestu i s imenom koje je korisnik izabrao. */
    fun saveAs(uri: Uri, then: () -> Unit = {}) = op {
        persist(uri)
        val wasDraft = target == null && folder == null
        target = uri
        diskBytes = null // nova datoteka: nema starije verzije s kojom bi bila u sukobu
        theirs = null
        conflict = false
        originalKept = true
        readOnly = false
        notText = false
        dirty = true
        if (saveLocked()) {
            title = io { displayName(uri) }
            canRename = io { supportsRename(uri) }
            prefs.edit().putString(KEY_TARGET, uri.toString()).apply()
            if (wasDraft) io { clearDraft(keptAs = text) }
            then()
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
        if (renamed != uri) {
            io { povijest.preseli(keyOf(uri), keyOf(renamed)) }
            persist(renamed)
        }
        target = renamed
        val actual = io { displayName(renamed) }
        title = actual
        prefs.edit().putString(KEY_TARGET, renamed.toString()).apply()
        if (actual != newName) message = "Sustav je datoteku nazvao „$actual“."
    }

    fun showVersions() = op {
        val key = currentKey()
        versions = io { povijest.popis(key) }
    }

    /** Vraća stariju verziju kao običnu izmjenu: može se poništiti, a trenutni tekst ide u povijest. */
    fun restore(v: Povijest.Verzija) = op {
        versions = null
        if (readOnly) {
            message = "Otvoreno samo za čitanje. Za izmjene: Spremi kao…"
            return@op
        }
        val key = currentKey()
        val current = text
        val old = io {
            povijest.spremi(key, current)
            povijest.procitaj(v)
        }
        povratak.prijeIzmjene(stanje(), SystemClock.uptimeMillis(), false)
        content = TextFieldValue(old)
        refreshUndo()
        dirty = true
        saveLocked()
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
        originalKept = false
        povratak.ocisti()
        refreshUndo()
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
        val dir = folder

        val ishod = when {
            uri == null && dir == null -> io { attempt { writeDraft(bytes) } }

            uri == null -> {
                // Prvo slovo novog papira: nastaje prava datoteka u mapi.
                if (snapshot.isEmpty()) return true
                val created = io {
                    try {
                        createIn(dir!!, PapirFile.newPaperName(LocalDateTime.now()))
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
                        originalKept = true
                        title = io { displayName(created) }
                        canRename = io { supportsRename(created) }
                        prefs.edit().putString(KEY_TARGET, created.toString()).apply()
                        io { clearDraft(keptAs = snapshot) }
                    }
                    r
                }
            }

            else -> io {
                val known = diskBytes
                val onDisk = if (known != null) read(uri) else null
                if (known != null && onDisk != null && !onDisk.contentEquals(known)) {
                    theirs = onDisk
                    Ishod.SUKOB
                } else {
                    if (!originalKept && known != null) {
                        PapirFile.decode(known)?.text?.let { povijest.spremi(keyOf(uri), it) }
                    }
                    attempt { writeUri(uri, bytes) }
                }
            }
        }

        when (ishod) {
            Ishod.OK -> {
                if (uri != null) originalKept = true
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

    private fun writeDraft(bytes: ByteArray) {
        val out = draft.startWrite()
        try {
            out.write(bytes)
            draft.finishWrite(out)
        } catch (e: Exception) {
            draft.failWrite(out)
            throw e
        }
    }

    /** Prazni nacrt; ako je u njemu bilo nešto drugo od [keptAs], to prvo ide u povijest. */
    private fun clearDraft(keptAs: String?) {
        val old = try {
            PapirFile.decode(draft.readFully())?.text.orEmpty()
        } catch (e: Exception) {
            ""
        }
        if (old.isEmpty()) return
        if (old != keptAs) povijest.spremi(Povijest.NACRT, old)
        try {
            writeDraft(ByteArray(0))
        } catch (_: Exception) {
        }
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
        // application/octet-stream: sustav ne dodaje svoj nastavak.
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
        val cursor = if (keepCursor) content.selection else TextRange.Zero
        val decoded = PapirFile.decode(bytes)
        if (decoded == null) {
            content = TextFieldValue(String(bytes, Charsets.UTF_8))
            notText = true
            message = "Ovo nije UTF-8 tekst. Otvoreno samo za čitanje."
        } else {
            // kao Notepad: otvorena datoteka počinje s kursorom na početku
            content = TextFieldValue(decoded.text, cursor)
            lineEnding = decoded.lineEnding
            hasBom = decoded.hasBom
            notText = false
        }
        diskBytes = bytes
        dirty = false
        refreshUndo()
    }

    private suspend fun loadDraft() {
        val restored = io {
            try {
                PapirFile.decode(draft.readFully())?.text.orEmpty()
            } catch (e: Exception) {
                ""
            }
        }
        // Nacrt: kursor na kraju, da se popis nastavlja tamo gdje je stao.
        resetTo(TextFieldValue(restored, TextRange(restored.length)))
        target = null
        title = null
        canRename = false
        prefs.edit().remove(KEY_TARGET).apply()
        // Nacrt iz vremena prije mape postaje prava datoteka čim je mapa izabrana.
        if (folder != null && restored.isNotEmpty()) {
            dirty = true
            saveLocked()
        }
    }

    private fun currentKey(): String = target?.let(::keyOf) ?: Povijest.NACRT

    private fun keyOf(uri: Uri): String = Povijest.kljuc(uri.toString())

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
