package hr.papir

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {

    private val vm: PapirViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        setContent { PapirTheme { PapirScreen(vm) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        vm.checkExternal()
    }

    override fun onStop() {
        super.onStop()
        vm.saveNow()
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_VIEW || action == Intent.ACTION_EDIT) {
            intent.data?.let { vm.open(it, fromPicker = false) }
        }
    }
}

/**
 * Traka iznad tipkovnice, na dohvat palca: lijevo poništi i ponovi, desno funkcije.
 * Jedan dodir otvara popis funkcija, drugi umeće funkciju (SPEC.md, ustav 4).
 *
 * Popis je dio rasporeda, a ne skočni prozor, pa ga tipkovnica ne može prekriti
 * i ne oduzima fokus papiru (tipkovnica ostaje otvorena).
 */
@Composable
private fun BottomBar(
    open: Boolean,
    enabled: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggle: () -> Unit,
    onDateTime: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        if (open) {
            FunctionItem("Datum i vrijeme", onDateTime)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarButton("↶", "Poništi", enabled && canUndo, onUndo)
            BarButton("↷", "Ponovi", enabled && canRedo, onRedo)
            Spacer(Modifier.weight(1f))
            BarButton(if (open) "×" else "+", if (open) "Zatvori funkcije" else "Funkcije", enabled, onToggle)
        }
    }
}

@Composable
private fun BarButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Text(symbol, fontSize = 22.sp)
    }
}

@Composable
private fun FunctionItem(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Sukob: datoteka je promijenjena izvana dok su ovdje bile nespremljene izmjene. Odlučuje čovjek. */
@Composable
private fun ConflictBar(onKeepMine: () -> Unit, onLoadTheirs: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
        Text(
            "Datoteka je u međuvremenu promijenjena izvan Papira. Ništa nije prepisano. " +
                "Koju verziju zadržati? Druga ostaje u Prethodnim verzijama.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onLoadTheirs) { Text("Onu izvana") }
            TextButton(onClick = onKeepMine) { Text("Ovu ovdje") }
        }
    }
}

@Composable
private fun PapirTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(background = Color.Black, surface = Color.Black)
    } else {
        lightColorScheme(background = Color.White, surface = Color.White)
    }
    MaterialTheme(colorScheme = colors, content = content)
}

private val VERSION_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm")

private fun versionTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(VERSION_TIME)

@Composable
private fun PapirScreen(vm: PapirViewModel) {
    var menuOpen by remember { mutableStateOf(false) }
    var functionsOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<TextFieldValue?>(null) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.open(uri, fromPicker = true)
    }
    val chooseFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.setFolder(uri)
    }
    // application/octet-stream: sustav ne dodaje svoj nastavak, ime ostaje slovo za slovo.
    val createDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val next = vm.pending
        vm.pending = null
        if (uri != null) {
            vm.saveAs(uri) {
                when (next) {
                    Pending.NEW -> vm.newPaper()
                    Pending.OPEN -> openDoc.launch(arrayOf("*/*"))
                    null -> Unit
                }
            }
        }
    }

    fun request(action: Pending) {
        if (vm.isUnsavedDraft) {
            vm.pending = action
        } else when (action) {
            Pending.NEW -> vm.newPaper()
            Pending.OPEN -> openDoc.launch(arrayOf("*/*"))
        }
    }

    LaunchedEffect(vm.loaded, vm.title) {
        if (vm.loaded && !vm.readOnly) {
            focus.requestFocus()
            keyboard?.show()
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(start = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val title = vm.title
                if (title == null && !vm.hasFolder && vm.loaded) {
                    // Jednom u životu: gdje žive papiri. Do tada se piše u nacrt.
                    Text(
                        text = "Odaberi mapu za papire",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { chooseFolder.launch(null) },
                    )
                } else {
                    Text(
                        text = title.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.semantics { contentDescription = "Izbornik" },
                    ) {
                        Text("⋯", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Novi") },
                            onClick = { menuOpen = false; request(Pending.NEW) },
                        )
                        DropdownMenuItem(
                            text = { Text("Otvori…") },
                            onClick = { menuOpen = false; request(Pending.OPEN) },
                        )
                        DropdownMenuItem(
                            text = { Text("Preimenuj…") },
                            enabled = vm.canRename,
                            onClick = {
                                menuOpen = false
                                val name = vm.title.orEmpty()
                                val dot = name.lastIndexOf('.')
                                // Odabrano je ime bez nastavka, ali i nastavak se smije mijenjati.
                                renaming = TextFieldValue(name, TextRange(0, if (dot > 0) dot else name.length))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Spremi kao…") },
                            enabled = !vm.notText,
                            onClick = { menuOpen = false; createDoc.launch(vm.suggestedName()) },
                        )
                        DropdownMenuItem(
                            text = { Text("Prethodne verzije…") },
                            onClick = { menuOpen = false; vm.showVersions() },
                        )
                        DropdownMenuItem(
                            text = { Text(if (vm.hasFolder) "Mapa za papire…" else "Odaberi mapu za papire…") },
                            onClick = { menuOpen = false; chooseFolder.launch(null) },
                        )
                    }
                }
            }

            if (vm.conflict) {
                ConflictBar(onKeepMine = vm::keepMine, onLoadTheirs = vm::loadTheirs)
            }

            vm.message?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.message = null }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }

            BasicTextField(
                value = vm.content,
                onValueChange = vm::onFieldChange,
                readOnly = vm.readOnly,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                // Ustav: papir ne ispravlja i ne mijenja ono što pišeš.
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .focusRequester(focus),
            )

            BottomBar(
                open = functionsOpen && !vm.readOnly,
                enabled = !vm.readOnly,
                canUndo = vm.canUndo,
                canRedo = vm.canRedo,
                onUndo = vm::undo,
                onRedo = vm::redo,
                onToggle = { functionsOpen = !functionsOpen },
                onDateTime = { functionsOpen = false; vm.insertDateTime() },
            )
        }
    }

    if (vm.pending != null) {
        AlertDialog(
            onDismissRequest = { vm.pending = null },
            title = { Text("Spremiti ovaj papir?") },
            text = {
                Text("Papir još nije datoteka. Ako ga ne spremiš, ostaje u Prethodnim verzijama.")
            },
            confirmButton = {
                TextButton(onClick = { createDoc.launch(vm.suggestedName()) }) { Text("Spremi kao…") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { vm.pending = null }) { Text("Odustani") }
                    TextButton(onClick = {
                        val next = vm.pending
                        vm.pending = null
                        when (next) {
                            Pending.NEW -> vm.newPaper()
                            Pending.OPEN -> openDoc.launch(arrayOf("*/*"))
                            null -> Unit
                        }
                    }) { Text("Ne spremaj") }
                }
            },
        )
    }

    renaming?.let { value ->
        val nameFocus = remember { FocusRequester() }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Preimenuj") },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { renaming = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                    ),
                    modifier = Modifier.focusRequester(nameFocus),
                )
                LaunchedEffect(Unit) { nameFocus.requestFocus() }
            },
            confirmButton = {
                TextButton(onClick = {
                    renaming = null
                    vm.rename(value.text)
                }) { Text("Preimenuj") }
            },
            dismissButton = {
                TextButton(onClick = { renaming = null }) { Text("Odustani") }
            },
        )
    }

    vm.versions?.let { list ->
        AlertDialog(
            onDismissRequest = { vm.versions = null },
            title = { Text("Prethodne verzije") },
            text = {
                if (list.isEmpty()) {
                    Text("Još nema spremljenih verzija ovog papira.")
                } else {
                    Column(
                        Modifier
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            "Dodir vraća verziju. Trenutni tekst pritom ide u ovaj popis, a vraćanje se može poništiti.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        list.forEach { v ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable { vm.restore(v) }
                                    .padding(vertical = 8.dp),
                            ) {
                                Text(versionTime(v.vrijeme), style = MaterialTheme.typography.labelLarge)
                                Text(
                                    v.pregled.ifEmpty { "(prazni retci)" },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.versions = null }) { Text("Zatvori") }
            },
        )
    }
}
