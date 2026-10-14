package hr.papir

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
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

class PapirViewModel(app: Application) : AndroidViewModel(app) {

    private val resolver = app.contentResolver
    private val prefs = app.getSharedPreferences("papir", Context.MODE_PRIVATE)
    private val draft = AtomicFile(File(app.filesDir, "papir.txt"))

    /** Tekst kakav se uređuje (krajevi redaka su uvijek \n), zajedno s kursorom i odabirom. */
    var field by mutableStateOf(TextFieldValue(""))
        private set

    val text: String get() = field.text

    /** Ime datoteke; null znači nacrt koji još nije spremljen kao datoteka. */
    var title by mutableStateOf<String?>(null)
        private set

    var readOnly by mutableStateOf(false)
        private set

    /** Datoteka nije ispravan UTF-8: ne smije se ni prepisati ni spremiti kao. */
    var notText by mutableStateOf(false)
        private set

    var loaded by mutableStateOf(false)
        private set

    var message by mutableStateOf<String?>(null)

    /** Radnja koja čeka da korisnik odluči što s nespremljenim nacrtom. */
    var pending by mutableStateOf<Pending?>(null)

    private var target: Uri? = null
    private var lineEnding = LineEnding.CRLF
    private var hasBom = false
    private var dirty = false
    private var saveJob: Job? = null

    val isUnsavedDraft: Boolean get() = target == null && text.isNotEmpty()

    init {
        op {
            val last = prefs.getString(KEY_TARGET, null)?.let(Uri::parse)
            if (last == null || !load(last)) loadDraft()
            loaded = true
        }
    }

    // --- Pisanje ---

    fun onFieldChange(new: TextFieldValue) {
        val changed = new.text != field.text
        if (changed && readOnly) return
        field = new
        if (!changed) return // samo pomak kursora ili odabira: ništa se ne sprema
        dirty = true
        saveJob?.cancel()
        saveJob = AppScope.scope.launch {
            delay(400)
            AppScope.lock.withLock { saveLocked() }
        }
    }

    // --- Funkcije (SPEC.md, 3) ---

    /** Umeće trenutni datum i vrijeme na mjesto kursora (ili umjesto odabranog teksta). */
    fun insertDateTime() {
        if (readOnly) return
        val (novi, kursor) = Funkcije.umetni(
            field.text, field.selection.start, field.selection.end,
            Funkcije.datumIVrijeme(LocalDateTime.now()),
        )
        onFieldChange(TextFieldValue(novi, TextRange(kursor)))
    }

    /** Pozvati kad aplikacija ide u pozadinu. */
    fun saveNow() {
        saveJob?.cancel()
        op { saveLocked() }
    }

    // --- Izbornik ---

    fun newPaper() = op {
        saveLocked()
        target = null
        title = null
        field = TextFieldValue("")
        lineEnding = LineEnding.CRLF
        hasBom = false
        readOnly = false
        notText = false
        dirty = true
        saveLocked()
        prefs.edit().remove(KEY_TARGET).apply()
    }

    fun open(uri: Uri, fromPicker: Boolean) = op {
        saveLocked()
        if (fromPicker) persist(uri)
        if (!load(uri)) message = "Datoteku nije moguće otvoriti."
    }

    /** Trenutni tekst postaje nova datoteka na mjestu i s imenom koje je korisnik izabrao. */
    fun saveAs(uri: Uri, then: () -> Unit = {}) = op {
        persist(uri)
        target = uri
        readOnly = false
        dirty = true
        if (saveLocked()) {
            title = displayName(uri)
            prefs.edit().putString(KEY_TARGET, uri.toString()).apply()
            then()
        }
    }

    // --- Unutarnje ---

    private fun op(block: suspend () -> Unit) {
        AppScope.scope.launch { AppScope.lock.withLock { block() } }
    }

    /** Vraća true ako je sve spremljeno. Nikad ne piše ako tekst nije mijenjan. */
    private suspend fun saveLocked(): Boolean {
        if (!dirty || readOnly) return true
        val snapshot = text
        val bytes = PapirFile.encode(snapshot, lineEnding, hasBom)
        val uri = target
        val ok = withContext(Dispatchers.IO) {
            try {
                if (uri == null) writeDraft(bytes) else writeUri(uri, bytes)
                true
            } catch (e: Exception) {
                false
            }
        }
        if (ok) {
            if (text == snapshot) dirty = false
        } else {
            message = "Spremanje nije uspjelo. Tekst je i dalje ovdje."
        }
        return ok
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

    private fun writeUri(uri: Uri, bytes: ByteArray) {
        // "wt" skraćuje datoteku; bez toga bi kraći tekst ostavio ostatke starog.
        val stream = resolver.openOutputStream(uri, "wt")
            ?: throw IllegalStateException("Nema izlaznog toka")
        stream.use { it.write(bytes) }
    }

    private suspend fun load(uri: Uri): Boolean {
        val bytes = withContext(Dispatchers.IO) {
            try {
                resolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (e: Exception) {
                null
            }
        } ?: return false

        val decoded = PapirFile.decode(bytes)
        if (decoded == null) {
            field = TextFieldValue(String(bytes, Charsets.UTF_8))
            notText = true
            readOnly = true
            message = "Ovo nije UTF-8 tekst. Otvoreno samo za čitanje."
        } else {
            field = TextFieldValue(decoded.text) // kao Notepad: kursor na početku datoteke
            lineEnding = decoded.lineEnding
            hasBom = decoded.hasBom
            notText = false
            readOnly = !canWrite(uri)
            if (readOnly) message = "Otvoreno samo za čitanje. Za izmjene: Spremi kao…"
        }
        target = uri
        title = displayName(uri)
        dirty = false
        if (readOnly) prefs.edit().remove(KEY_TARGET).apply()
        else prefs.edit().putString(KEY_TARGET, uri.toString()).apply()
        return true
    }

    private suspend fun loadDraft() {
        val bytes = withContext(Dispatchers.IO) {
            try {
                draft.readFully()
            } catch (e: Exception) {
                ByteArray(0)
            }
        }
        val restored = PapirFile.decode(bytes)?.text ?: ""
        // Nacrt: kursor na kraju, da se popis nastavlja tamo gdje je stao.
        field = TextFieldValue(restored, TextRange(restored.length))
        lineEnding = LineEnding.CRLF
        hasBom = false
        target = null
        title = null
        readOnly = false
        notText = false
        dirty = false
        prefs.edit().remove(KEY_TARGET).apply()
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

    private fun canWrite(uri: Uri): Boolean =
        getApplication<Application>().checkUriPermission(
            uri, Process.myPid(), Process.myUid(), Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        ) == PackageManager.PERMISSION_GRANTED

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

    fun suggestedName(): String = PapirFile.suggestName(text)

    private companion object {
        const val KEY_TARGET = "target"
    }
}
