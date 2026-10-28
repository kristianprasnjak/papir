package hr.papir

import android.content.ClipData
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

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

/** Radnje s tablicom iz popisa funkcija, dok je kursor u ćeliji. */
private enum class TableAction { RowBelow, ColumnRight, DeleteRow, DeleteColumn, DeleteTable }

/**
 * Traka iznad tipkovnice, na dohvat palca: lijevo poništi i ponovi, desno funkcije.
 * Jedan dodir otvara popis funkcija, drugi umeće funkciju (PAPIR.md, ustav 4).
 * Iznad nje, kad treba: radnje s izabranom slikom ili kutijom, ili s poveznicom pod kursorom.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BottomBar(
    vm: PapirViewModel,
    open: Boolean,
    onToggle: () -> Unit,
    onClose: () -> Unit,
    textFunctions: Boolean,
    tableShape: Pair<Int, Int>?,
    onTableAction: (TableAction) -> Unit,
    onLink: () -> Unit,
    onImage: () -> Unit,
    onOpenLink: (String) -> Unit,
    onRenameImage: () -> Unit,
) {
    val enabled = !vm.readOnly
    val izabrani = vm.selected
    val ghost = vm.ghost
    Column(Modifier.fillMaxWidth()) {
        when {
            izabrani != null && ghost != null && enabled -> {
                // Pomicanje ili veličina: strelice, pa potvrda. Ništa se ne mijenja dok se ne potvrdi.
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BarButton("←", "Lijevo", true) { vm.nudge(-1, 0) }
                    BarButton("↑", "Gore", true) { vm.nudge(0, -1) }
                    BarButton("↓", "Dolje", true) { vm.nudge(0, 1) }
                    BarButton("→", "Desno", true) { vm.nudge(1, 0) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = vm::cancelGhost) { Text("Odustani") }
                    TextButton(onClick = vm::confirmGhost, enabled = vm.ghostFree) { Text("Potvrdi") }
                }
            }
            izabrani != null && enabled -> {
                FlowRow(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { vm.startGhost(PapirViewModel.GhostMode.MOVE) }) { Text("Pomakni") }
                    if (izabrani.vrsta == Objekt.Vrsta.SLIKA) {
                        TextButton(onClick = { vm.startGhost(PapirViewModel.GhostMode.SIZE) }) { Text("Veličina") }
                        TextButton(onClick = onRenameImage) { Text("Preimenuj") }
                    }
                    TextButton(onClick = vm::deleteSelected) { Text("Obriši") }
                    TextButton(onClick = { vm.select(null) }) { Text("Gotovo") }
                }
            }
            open && enabled && !textFunctions -> {
                val (retci, stupci) = tableShape ?: (0 to 0)
                FlowRow(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onTableAction(TableAction.RowBelow) }, enabled = tableShape != null) { Text("Redak ispod") }
                    TextButton(onClick = { onTableAction(TableAction.ColumnRight) }, enabled = tableShape != null) { Text("Stupac desno") }
                    TextButton(onClick = { onTableAction(TableAction.DeleteRow) }, enabled = retci > 1) { Text("Obriši redak") }
                    TextButton(onClick = { onTableAction(TableAction.DeleteColumn) }, enabled = stupci > 1) { Text("Obriši stupac") }
                    TextButton(onClick = { onTableAction(TableAction.DeleteTable) }, enabled = tableShape != null) { Text("Obriši tablicu") }
                }
            }
            open && enabled -> {
                val current = vm.currentStyle
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FormatButton("B", "Podebljano", current.podebljano, vm::toggleBold, bold = true)
                    FormatButton("I", "Kurziv", current.kurziv, vm::toggleItalic, italic = true)
                    Spacer(Modifier.width(8.dp))
                    listOf(0 to "M", 1 to "L", 2 to "XL").forEach { (v, label) ->
                        FormatButton(label, "Veličina $label", current.velicina == v, { vm.setSize(v) })
                    }
                }
                FlowRow(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onClose(); vm.insertCheckbox() }) { Text("□ Kvačica") }
                    TextButton(onClick = { onClose(); vm.insertFolder() }) { Text("▼ Mapa") }
                    TextButton(onClick = { onClose(); onLink() }) { Text("Poveznica") }
                    TextButton(onClick = { onClose(); vm.insertTable() }) { Text("Tablica") }
                    TextButton(onClick = { onClose(); onImage() }) { Text("Slika") }
                    TextButton(onClick = { onClose(); vm.insertBox() }) { Text("Kutija") }
                    TextButton(onClick = { onClose(); vm.insertDateTime() }) { Text("Datum i vrijeme") }
                }
            }
            enabled && vm.linkAtCursor != null -> {
                val cilj = vm.linkAtCursor.orEmpty()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { onOpenLink(cilj) }, modifier = Modifier.weight(1f)) {
                        Text("Otvori: $cilj", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = vm::removeLink) { Text("Ukloni") }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarButton("↶", "Poništi", enabled && vm.canUndo, vm::undo)
            BarButton("↷", "Ponovi", enabled && vm.canRedo, vm::redo)
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
private fun FormatButton(
    label: String,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
    bold: Boolean = false,
    italic: Boolean = false,
) {
    Box(
        Modifier
            .size(44.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = if (bold || active) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Prikaz: oznake su već uklonjene iz teksta; ovdje se crta stil, poveznica i nađeni tekst, a gdje
 * tekst teče desno od slike ili kutije, umeću se razmaci (samo na ekranu, kao u Notepadu).
 */
private class StilPrikaz(
    private val stilovi: List<Stil>,
    private val praznine: Map<Int, Int>,
    private val oznake: List<IntRange>,
    private val trenutna: IntRange?,
    private val bojaPoveznice: Color,
    private val bojaOznake: Color,
    private val bojaTrenutne: Color,
) : VisualTransformation {

    var mapping: OffsetMapping = OffsetMapping.Identity
        private set

    override fun filter(text: AnnotatedString): TransformedText {
        val n = text.length
        val prije = IntArray(n + 1) // umetnuto prije pozicije i (ne uključujući prazninu na i)
        var zbroj = 0
        for (i in 0..n) {
            prije[i] = zbroj
            zbroj += praznine[i] ?: 0
        }
        fun znak(i: Int) = i + prije[i] + (praznine[i] ?: 0) // gdje je znak i u prikazu
        val b = AnnotatedString.Builder()
        for (i in 0 until n) {
            praznine[i]?.let { b.append(" ".repeat(it)) }
            b.append(text[i])
        }
        for ((od, doo, s) in Oblik.dijelovi(stilovi)) {
            if (doo <= n && doo > od) b.addStyle(spanOf(s), znak(od), znak(doo - 1) + 1)
        }
        for (r in oznake) {
            if (r.last < n) b.addStyle(SpanStyle(background = if (r == trenutna) bojaTrenutne else bojaOznake), znak(r.first), znak(r.last) + 1)
        }
        val m = b.length
        val uPrikaz = IntArray(n + 1) { it + prije[it] }
        // svaki znak prikaza pripada znaku teksta: razmaci praznine onome ispred kojeg stoje
        val izPrikaza = IntArray(m + 1)
        var t = 0
        for (i in 0 until n) {
            repeat(praznine[i] ?: 0) { izPrikaza[t++] = i }
            izPrikaza[t++] = i
        }
        izPrikaza[m] = n
        mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = uPrikaz[offset.coerceIn(0, n)]
            override fun transformedToOriginal(offset: Int): Int = izPrikaza[offset.coerceIn(0, m)]
        }
        return TransformedText(b.toAnnotatedString(), mapping)
    }

    private fun spanOf(s: Stil) = SpanStyle(
        fontWeight = if (s.podebljano) FontWeight.Bold else null,
        fontStyle = if (s.kurziv) FontStyle.Italic else null,
        fontSize = when (s.velicina) {
            2 -> 1.7.em
            1 -> 1.3.em
            else -> TextUnit.Unspecified
        },
        color = if (s.poveznica != null) bojaPoveznice else Color.Unspecified,
        textDecoration = if (s.poveznica != null) TextDecoration.Underline else null,
    )
}

/** Sukob: datoteka je promijenjena izvana dok su ovdje bile nespremljene izmjene. Odlučuje čovjek. */
@Composable
private fun ConflictBar(onKeepMine: () -> Unit, onLoadTheirs: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
        Text(
            "Datoteka je u međuvremenu promijenjena izvan Papira. Ništa nije prepisano. " +
                "Koju verziju zadržati u datoteci? Druga se sprema kao zasebna datoteka u mapi.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onLoadTheirs) { Text("Onu izvana") }
            TextButton(onClick = onKeepMine) { Text("Ovu ovdje") }
        }
    }
}

/** Jednom u životu: gdje žive papiri. */
@Composable
private fun FolderPrompt(onChoose: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Gdje će živjeti tvoji papiri?", style = MaterialTheme.typography.titleLarge)
        Text(
            "Svaki papir je obična datoteka u mapi koju izabereš. Biraš samo jednom.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
        )
        TextButton(onClick = onChoose) { Text("Odaberi mapu") }
    }
}

/** Traži i zamijeni, kao u Notepadu (PAPIR.md, 5). */
@Composable
private fun SearchBar(vm: PapirViewModel, onClose: () -> Unit) {
    var zamjena by remember { mutableStateOf("") }
    val fokus = remember { FocusRequester() }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = vm.searchQuery,
                onValueChange = { vm.search(it) },
                singleLine = true,
                placeholder = { Text("Traži") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
                modifier = Modifier.weight(1f).focusRequester(fokus),
            )
            Text(
                if (vm.matches.isEmpty()) "0" else "${vm.currentMatch + 1}/${vm.matches.size}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            BarButton("↑", "Prethodno", vm.matches.isNotEmpty()) { vm.nextMatch(-1) }
            BarButton("↓", "Sljedeće", vm.matches.isNotEmpty()) { vm.nextMatch(1) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = zamjena,
                onValueChange = { zamjena = it },
                singleLine = true,
                placeholder = { Text("Zamijeni s") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
                modifier = Modifier.weight(1f),
            )
            FormatButton("Aa", "Velika i mala slova", vm.matchCase, { vm.search(vm.searchQuery, !vm.matchCase) })
            TextButton(onClick = { vm.replaceCurrent(zamjena) }, enabled = !vm.readOnly && vm.matches.isNotEmpty()) { Text("Zamijeni") }
            TextButton(onClick = { vm.replaceAll(zamjena) }, enabled = !vm.readOnly && vm.matches.isNotEmpty()) { Text("Sve") }
            BarButton("×", "Zatvori traženje", true) { vm.endSearch(); onClose() }
        }
    }
    LaunchedEffect(Unit) { fokus.requestFocus() }
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

@Composable
private fun PapirScreen(vm: PapirViewModel) {
    var menuOpen by remember { mutableStateOf(false) }
    var functionsOpen by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<TextFieldValue?>(null) }
    var linkDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var imageName by remember { mutableStateOf<String?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

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
        if (uri != null) vm.saveAs(uri)
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.insertImage(uri)
    }

    fun openLink(cilj: String) {
        val web = cilj.startsWith("http://", true) || cilj.startsWith("https://", true) || cilj.startsWith("mailto:", true)
        if (web) {
            // Papir nema internet: poveznicu otvara preglednik koji korisnik ima.
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cilj)))
            } catch (e: Exception) {
                vm.message = "Nijedna aplikacija ne može otvoriti „$cilj“."
            }
        } else {
            vm.openPaperByName(cilj)
        }
    }

    val showPaper = vm.loaded && !vm.needsFolder

    val tekst = vm.content.text
    val linije = remember(tekst) { tekst.split("\n") }
    val odsjecci = remember(linije.size, vm.tables, vm.hidden) { Odsjecci.podijeli(linije.size, vm.tables, vm.hidden) }
    val requesters = remember(odsjecci.size) { List(odsjecci.size) { FocusRequester() } }
    var focusedCell by remember { mutableStateOf<Any?>(null) }
    var focusText by remember { mutableStateOf(0) }
    val cell = focusedCell as? Triple<*, *, *>
    val cellTable = cell?.first as? Int
    val tableShape = cellTable?.let { k ->
        vm.tables.getOrNull(k)?.let { raspon ->
            Tablice.citaj(linije.subList(raspon.first, raspon.last + 1))?.let { it.retci.size to it.sirine.size }
        }
    }

    fun onTableAction(a: TableAction) {
        val k = cellTable ?: return
        val r = cell?.second as? Int ?: return
        val c = cell?.third as? Int ?: return
        val (retci, stupci) = tableShape ?: return
        when (a) {
            TableAction.RowBelow -> vm.editTable(k) { it.sRetkomIspod(r) }
            TableAction.ColumnRight -> vm.editTable(k) { it.sStupcemDesno(c) }
            TableAction.DeleteRow -> vm.editTable(k) { it.bezRetka(r) }
            TableAction.DeleteColumn -> vm.editTable(k) { it.bezStupca(c) }
            TableAction.DeleteTable -> vm.deleteTable(k)
        }
        // ako ćelija s kursorom više ne postoji, kursor se vraća u tekst
        val nestala = a == TableAction.DeleteTable ||
            (a == TableAction.DeleteRow && r == retci - 1) ||
            (a == TableAction.DeleteColumn && c == stupci - 1)
        if (nestala) {
            focusedCell = null
            functionsOpen = false
            focusText++
        }
    }

    LaunchedEffect(showPaper, vm.title, focusText) {
        if (showPaper && !vm.readOnly) {
            // kursor u odsječku teksta u kojem je bio; papir bez tablica ima samo jedan
            val kursor = vm.content.selection.start
            val indeks = odsjecci.indices.lastOrNull { i ->
                odsjecci[i].tablica == null && !odsjecci[i].skriveno && Odsjecci.pocetak(tekst, odsjecci[i].od) <= kursor
            } ?: odsjecci.lastIndex
            try {
                requesters[indeks].requestFocus()
            } catch (_: IllegalStateException) {
            }
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
            if (vm.loaded && vm.needsFolder) {
                vm.message?.let { MessageLine(it) { vm.message = null } }
                FolderPrompt { chooseFolder.launch(null) }
            } else if (showPaper) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(start = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = vm.title.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
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
                                onClick = { menuOpen = false; vm.newPaper() },
                            )
                            DropdownMenuItem(
                                text = { Text("Otvori…") },
                                onClick = { menuOpen = false; openDoc.launch(arrayOf("*/*")) },
                            )
                            DropdownMenuItem(
                                text = { Text("Traži i zamijeni…") },
                                onClick = { menuOpen = false; searching = true },
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
                                text = { Text("Mapa za papire…") },
                                onClick = { menuOpen = false; chooseFolder.launch(null) },
                            )
                        }
                    }
                }

                if (searching) SearchBar(vm) { searching = false }

                if (vm.conflict) {
                    ConflictBar(onKeepMine = vm::keepMine, onLoadTheirs = vm::loadTheirs)
                }

                vm.message?.let { MessageLine(it) { vm.message = null } }

                Paper(
                    vm = vm,
                    linije = linije,
                    odsjecci = odsjecci,
                    requesters = requesters,
                    focusedCell = focusedCell,
                    onFocusCell = { focusedCell = it },
                    onTapBelow = {
                        try {
                            requesters.last().requestFocus()
                        } catch (_: IllegalStateException) {
                        }
                        keyboard?.show()
                    },
                    modifier = Modifier.weight(1f),
                )

                BottomBar(
                    vm = vm,
                    open = functionsOpen && !vm.readOnly,
                    onToggle = { functionsOpen = !functionsOpen },
                    onClose = { functionsOpen = false },
                    textFunctions = focusedCell == null,
                    tableShape = tableShape,
                    onTableAction = { onTableAction(it) },
                    onLink = { linkDialog = vm.selectedText.replace('\n', ' ') to "" },
                    onImage = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onOpenLink = { openLink(it) },
                    onRenameImage = { imageName = vm.selected?.sadrzaj },
                )
            }
        }
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

    imageName?.let { ime ->
        AlertDialog(
            onDismissRequest = { imageName = null },
            title = { Text("Ime slike") },
            text = {
                OutlinedTextField(
                    value = ime,
                    onValueChange = { imageName = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    imageName = null
                    vm.renameImage(ime)
                }) { Text("Preimenuj") }
            },
            dismissButton = {
                TextButton(onClick = { imageName = null }) { Text("Odustani") }
            },
        )
    }

    linkDialog?.let { (t, c) ->
        AlertDialog(
            onDismissRequest = { linkDialog = null },
            title = { Text("Poveznica") },
            text = {
                Column {
                    OutlinedTextField(
                        value = t,
                        onValueChange = { linkDialog = it to c },
                        singleLine = true,
                        label = { Text("Tekst") },
                    )
                    OutlinedTextField(
                        value = c,
                        onValueChange = { linkDialog = t to it },
                        singleLine = true,
                        label = { Text("Cilj: ime papira ili web adresa") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    linkDialog = null
                    vm.insertLink(t, c)
                }) { Text("Umetni") }
            },
            dismissButton = {
                TextButton(onClick = { linkDialog = null }) { Text("Odustani") }
            },
        )
    }
}

@Composable
private fun MessageLine(text: String, onDismiss: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDismiss)
            .padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun papirStil(): TextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 17.sp,
    // bez fiksne visine retka: veći naslovi (L, XL) dobivaju svoj prostor
    color = MaterialTheme.colorScheme.onSurface,
)

/** Gdje je na ekranu svaki redak čistog teksta: vrh i dno u pikselima, unutar papira. */
private typealias Polozaji = SnapshotStateMap<Int, Pair<Float, Float>>

/**
 * Papir kao niz odsječaka: tekst, tablice i skriveni sadržaj zatvorenih mapa (ARHITEKTURA.md).
 * Retci se ne prelamaju, kao u Notepadu: dugačak redak ide udesno i papir se pomiče i vodoravno.
 * Slike i kutije stoje iznad teksta na svom mjestu mreže.
 */
@Composable
private fun Paper(
    vm: PapirViewModel,
    linije: List<String>,
    odsjecci: List<Odsjecak>,
    requesters: List<FocusRequester>,
    focusedCell: Any?,
    onFocusCell: (Any?) -> Unit,
    onTapBelow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tekst = vm.content.text
    val polozaji: Polozaji = remember { mutableStateMapOf() }
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    val stil = papirStil()
    val measurer = rememberTextMeasurer()
    val znak = remember(stil) { measurer.measure("0", stil).size.width.toFloat() }
    val density = LocalDensity.current

    // skriveni i nestali retci nemaju mjesto na ekranu (objekti u njima se ne crtaju)
    LaunchedEffect(vm.hidden, linije.size) {
        polozaji.keys.filter { l -> l >= linije.size || vm.hidden.any { l in it } }.forEach { polozaji.remove(it) }
    }

    // traženje: nađeni tekst se dovodi na ekran
    LaunchedEffect(vm.currentMatch, vm.matches) {
        val m = vm.matches.getOrNull(vm.currentMatch) ?: return@LaunchedEffect
        val linija = tekst.substring(0, m.first.coerceAtMost(tekst.length)).count { it == '\n' }
        val stupac = m.first - (tekst.lastIndexOf('\n', m.first - 1) + 1)
        polozaji[linija]?.let { (vrh, _) -> vScroll.animateScrollTo((vrh - 100f).roundToInt().coerceAtLeast(0)) }
        hScroll.animateScrollTo(((stupac - 4) * znak).roundToInt().coerceAtLeast(0))
    }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val sirinaEkrana = maxWidth
        Box(
            Modifier
                .fillMaxSize()
                .verticalScroll(vScroll)
                .horizontalScroll(hScroll),
        ) {
            Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Column(Modifier.widthIn(min = sirinaEkrana - 40.dp)) {
                    odsjecci.forEachIndexed { i, o ->
                        val tablica = o.tablica
                        when {
                            o.skriveno -> {}
                            tablica != null -> key("tablica", tablica) {
                                val blok = linije.subList(o.od, o.doo)
                                val t = remember(blok) { Tablice.citaj(blok) }
                                if (t != null) {
                                    TableView(
                                        t = t,
                                        index = tablica,
                                        enabled = !vm.readOnly,
                                        focusedCell = focusedCell,
                                        onFocusCell = onFocusCell,
                                        onCell = { r, c, prije, celija -> vm.editCell(tablica, r, c, prije, celija) },
                                        modifier = Modifier.onGloballyPositioned { k ->
                                            val vrh = k.positionInParent().y
                                            val v = k.size.height.toFloat() / (o.doo - o.od)
                                            for (l in o.od until o.doo) polozaji[l] = (vrh + (l - o.od) * v) to (vrh + (l - o.od + 1) * v)
                                        },
                                    )
                                }
                            }
                            else -> key("tekst", i) {
                                TextSegment(
                                    vm = vm,
                                    tekst = tekst,
                                    linije = linije,
                                    odsjecak = o,
                                    requester = requesters.getOrNull(i),
                                    sirina = sirinaEkrana - 40.dp,
                                    polozaji = polozaji,
                                    onFocused = { onFocusCell(null); vm.select(null) },
                                )
                            }
                        }
                    }
                    // Dodir ispod teksta vodi na kraj papira.
                    Box(
                        Modifier
                            .width(sirinaEkrana - 40.dp)
                            .height(240.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onTapBelow,
                            ),
                    )
                }
                Objects(vm, polozaji, znak, density)
            }
        }
    }
}

/** Vrh i dno retka mreže na ekranu. Crta naslova je donja polovica njegova retka. */
private fun rowSpan(vm: PapirViewModel, polozaji: Polozaji, redak: Int): Pair<Float, Float>? {
    val linija = vm.lineOfRawRow(redak)
    val (vrh, dno) = polozaji[linija] ?: return null
    val prvi = vm.rawRowOfLine(linija)
    val redova = vm.rawRowOfLine(linija + 1) - prvi
    if (redova <= 1) return vrh to dno
    val v = (dno - vrh) / redova
    val k = (redak - prvi).coerceIn(0, redova - 1)
    return (vrh + k * v) to (vrh + (k + 1) * v)
}

/** Slike i kutije iznad teksta, i obris novog položaja dok se pomiču. */
@Composable
private fun Objects(vm: PapirViewModel, polozaji: Polozaji, znak: Float, density: androidx.compose.ui.unit.Density) {
    val linija = MaterialTheme.colorScheme.outline
    val izabranaBoja = MaterialTheme.colorScheme.primary
    // objekt u skrivenom sadržaju mape se ne crta
    val vidljivi = vm.objects.mapNotNull { o ->
        val gore = rowSpan(vm, polozaji, o.redak)
        val dolje = rowSpan(vm, polozaji, o.zadnjiRedak)
        if (gore == null || dolje == null) null else Triple(o, gore, dolje)
    }
    for ((o, gore, dolje) in vidljivi) {
        val x = o.stupac * znak
        val w = o.sirina * znak
        val h = dolje.second - gore.first
        val izabran = vm.selected == o
        key(o.vrsta, o.redak, o.stupac) {
            Box(
                Modifier
                    .offset { IntOffset(x.roundToInt(), gore.first.roundToInt()) }
                    .size(with(density) { w.toDp() }, with(density) { h.toDp() })
                    .border(if (izabran) 3.dp else 2.dp, if (izabran) izabranaBoja else linija),
            ) {
                when (o.vrsta) {
                    Objekt.Vrsta.SLIKA -> ImageObject(vm, o)
                    Objekt.Vrsta.KUTIJA -> BoxObject(vm, o, polozaji, znak, gore.first, density)
                }
            }
        }
    }
    val g = vm.ghost
    if (g != null) {
        val gore = rowSpan(vm, polozaji, g.redak)
        val dolje = rowSpan(vm, polozaji, g.zadnjiRedak)
        val visinaRetka = gore?.let { it.second - it.first } ?: 40f
        // ispod kraja teksta: nastavlja se mreža redaka iste visine
        val y = gore?.first ?: ((polozaji.values.maxOfOrNull { it.second } ?: 0f) + (g.redak - vm.rawRowOfLine(Int.MAX_VALUE)) * visinaRetka)
        val h = if (gore != null && dolje != null) dolje.second - gore.first else g.visina * visinaRetka
        Box(
            Modifier
                .offset { IntOffset((g.stupac * znak).roundToInt(), y.roundToInt()) }
                .size(with(density) { (g.sirina * znak).toDp() }, with(density) { h.toDp() })
                .border(3.dp, if (vm.ghostFree) izabranaBoja else MaterialTheme.colorScheme.error),
        )
    }
}

@Composable
private fun ImageObject(vm: PapirViewModel, o: Objekt) {
    val bajtovi = vm.attachments[o.sadrzaj]
    val slika = remember(o.sadrzaj, bajtovi) {
        bajtovi?.let { b ->
            try {
                val granice = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(b, 0, b.size, granice)
                var uzorak = 1
                while (granice.outWidth / uzorak > 1600 || granice.outHeight / uzorak > 1600) uzorak *= 2
                BitmapFactory.decodeByteArray(b, 0, b.size, BitmapFactory.Options().apply { inSampleSize = uzorak })?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .clickable { vm.select(o) },
        contentAlignment = Alignment.Center,
    ) {
        if (slika != null) {
            Image(slika, contentDescription = o.sadrzaj, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(3.dp))
        } else {
            Text(o.sadrzaj, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Kutija teksta: piše se kao u ćeliji, Enter je novi red, kutija prati širinu teksta. */
@Composable
private fun BoxObject(
    vm: PapirViewModel,
    o: Objekt,
    polozaji: Polozaji,
    znak: Float,
    vrh: Float,
    density: androidx.compose.ui.unit.Density,
) {
    var lokalno by remember(o.redak, o.stupac) { mutableStateOf(TextFieldValue(o.sadrzaj, TextRange(o.sadrzaj.length))) }
    val trenutni by rememberUpdatedState(o)
    LaunchedEffect(o.sadrzaj) {
        if (Tablice.normaliziraj(lokalno.text) != Tablice.normaliziraj(o.sadrzaj)) {
            lokalno = TextFieldValue(o.sadrzaj, TextRange(o.sadrzaj.length))
        }
    }
    val prviRed = rowSpan(vm, polozaji, o.redak + 1)?.first ?: vrh
    BasicTextField(
        value = lokalno,
        onValueChange = { v ->
            val promijenjeno = v.text != lokalno.text
            lokalno = v
            if (promijenjeno) vm.editBox(trenutni, v.text)
        },
        readOnly = vm.readOnly,
        textStyle = papirStil(),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
        modifier = Modifier
            .offset { IntOffset((2 * znak).roundToInt(), (prviRed - vrh).roundToInt()) }
            .width(with(density) { ((o.sirina - 4).coerceAtLeast(1) * znak).toDp() })
            .onFocusChanged { if (it.isFocused) vm.select(trenutni) },
    )
}

@Composable
private fun TextSegment(
    vm: PapirViewModel,
    tekst: String,
    linije: List<String>,
    odsjecak: Odsjecak,
    requester: FocusRequester?,
    sirina: Dp,
    polozaji: Polozaji,
    onFocused: () -> Unit,
) {
    val start = Odsjecci.pocetak(tekst, odsjecak.od)
    val dio = if (odsjecak.od == odsjecak.doo) "" else linije.subList(odsjecak.od, odsjecak.doo).joinToString("\n")
    val end = start + dio.length
    val sel = vm.content.selection
    val unutra = sel.min >= start && sel.max <= end && !(odsjecak.od == odsjecak.doo && sel.min != start)
    val lokalno = if (unutra) TextRange(sel.start - start, sel.end - start) else TextRange(dio.length)
    val comp = vm.content.composition?.takeIf { unutra && it.min >= start && it.max <= end }
        ?.let { TextRange(it.start - start, it.end - start) }
    val styles = vm.styles
    val dijelStilova = remember(styles, start, end) {
        if (end <= styles.size) styles.subList(start, end).toList() else List(dio.length) { Stil.OBICNO }
    }
    val praznine = remember(vm.gaps, start, end) {
        vm.gaps.filterKeys { it in start until end }.mapKeys { it.key - start }
    }
    val oznake = remember(vm.matches, start, end) {
        vm.matches.filter { it.first >= start && it.last < end }.map { (it.first - start)..(it.last - start) }
    }
    val trenutna = vm.matches.getOrNull(vm.currentMatch)?.let { (it.first - start)..(it.last - start) }
    val boje = MaterialTheme.colorScheme
    val prikaz = remember(dijelStilova, praznine, oznake, trenutna, boje) {
        StilPrikaz(dijelStilova, praznine, oznake, trenutna, boje.primary, boje.primary.copy(alpha = 0.18f), boje.primary.copy(alpha = 0.4f))
    }
    var raspored by remember { mutableStateOf<TextLayoutResult?>(null) }
    var vrhOdsjecka by remember { mutableStateOf(0f) }

    // Kopiranje šalje i goli tekst i HTML s pravim oblikovanjem (PAPIR.md, 4).
    val trenutno by rememberUpdatedState(Triple(dio, lokalno, dijelStilova))
    val base = LocalClipboardManager.current
    val context = LocalContext.current
    val clipboard = remember(base, context) {
        PapirClipboard(base, context.getSystemService(android.content.ClipboardManager::class.java)) {
            val (d, s, st) = trenutno
            if (s.collapsed || s.max > d.length || st.size != d.length) null
            else d.substring(s.min, s.max) to st.subList(s.min, s.max)
        }
    }
    val pocetak by rememberUpdatedState(start)
    val trenutniPrikaz by rememberUpdatedState(prikaz)
    CompositionLocalProvider(LocalClipboardManager provides clipboard) {
        BasicTextField(
            value = TextFieldValue(dio, lokalno, comp),
            onValueChange = { vm.onSegmentChange(odsjecak.od, odsjecak.doo, it) },
            readOnly = vm.readOnly,
            visualTransformation = prikaz,
            textStyle = papirStil(),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            // Ustav: papir ne ispravlja i ne mijenja ono što pišeš.
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
            onTextLayout = { l ->
                raspored = l
                for (k in 0 until minOf(l.lineCount, odsjecak.doo - odsjecak.od)) {
                    polozaji[odsjecak.od + k] = (vrhOdsjecka + l.getLineTop(k)) to (vrhOdsjecka + l.getLineBottom(k))
                }
            },
            modifier = Modifier
                .widthIn(min = sirina)
                .onGloballyPositioned { k ->
                    val y = k.positionInParent().y
                    if (y != vrhOdsjecka) {
                        vrhOdsjecka = y
                        raspored?.let { l ->
                            for (r in 0 until minOf(l.lineCount, odsjecak.doo - odsjecak.od)) {
                                polozaji[odsjecak.od + r] = (y + l.getLineTop(r)) to (y + l.getLineBottom(r))
                            }
                        }
                    }
                }
                // Dodir na □ ■ ► ▼ mijenja znak (STANDARD.md, 5.1 i 5.2); tekstno polje dobije dodir normalno.
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val dolje = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val gore = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                        if (gore != null && (gore.position - dolje.position).getDistance() < viewConfiguration.touchSlop) {
                            raspored?.let { l ->
                                val t = l.getOffsetForPosition(dolje.position)
                                vm.toggleAt(pocetak + trenutniPrikaz.mapping.transformedToOriginal(t))
                            }
                        }
                    }
                }
                .let { if (requester != null) it.focusRequester(requester) else it }
                .onFocusChanged { if (it.isFocused) onFocused() },
        )
    }
}

/**
 * Međuspremnik koji uz goli tekst šalje i HTML, kad odabrani tekst ima oblikovanje.
 * Program u koji se lijepi sam bira verziju koju zna. Znakovi formata se nikad ne kopiraju.
 */
private class PapirClipboard(
    private val base: ClipboardManager,
    private val native: android.content.ClipboardManager?,
    private val odabrano: () -> Pair<String, List<Stil>>?,
) : ClipboardManager by base {
    override fun setText(annotatedString: AnnotatedString) {
        val goli = annotatedString.text
        val o = odabrano()
        if (native == null || o == null || o.first != goli || o.second.all { it.obicno }) {
            base.setText(annotatedString)
            return
        }
        native.setPrimaryClip(ClipData.newHtmlText("Papir", goli, Html.iz(goli, o.second)))
    }
}

/**
 * Tablica kako je aplikacija crta: prave linije i ćelije. Širine stupaca su iste kao u
 * datoteci (znakova u fontu jednake širine), pa izgleda isto kao u Notepadu.
 */
@Composable
private fun TableView(
    t: Tablica,
    index: Int,
    enabled: Boolean,
    focusedCell: Any?,
    onFocusCell: (Any?) -> Unit,
    onCell: (Int, Int, String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stil = papirStil()
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val znak = remember(stil) { with(density) { measurer.measure("0", stil).size.width.toDp() } }
    // Stupac na ekranu je točno širok kao u datoteci: koliko znakova i razmaka, najmanje jedan.
    val sirine = t.sirine.map { znak * (maxOf(it, 1) + 2) }
    val ukupno = sirine.fold(0.dp) { a, b -> a + b }
    val linija = MaterialTheme.colorScheme.outline
    Box(modifier.padding(vertical = 6.dp)) {
        Column(Modifier.border(1.dp, linija)) {
            t.retci.forEachIndexed { r, row ->
                if (r > 0) {
                    Box(
                        Modifier
                            .width(ukupno)
                            .height(if (r == 1 && t.zaglavlje) 3.dp else 1.dp)
                            .background(linija),
                    )
                }
                Row(
                    Modifier.drawBehind {
                        var x = 0f
                        sirine.dropLast(1).forEach { w ->
                            x += w.toPx()
                            drawLine(linija, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                        }
                    },
                ) {
                    row.forEachIndexed { c, celija ->
                        val kljuc = Triple(index, r, c)
                        CellEditor(
                            text = celija,
                            key = kljuc,
                            width = sirine[c],
                            padding = znak,
                            bold = t.zaglavlje && r == 0,
                            enabled = enabled,
                            style = stil,
                            onChange = { prije, novo -> onCell(r, c, prije, novo) },
                            onFocus = { fokus ->
                                if (fokus) onFocusCell(kljuc) else if (focusedCell == kljuc) onFocusCell(null)
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Jedna ćelija. Drži svoj kursor; tekst dolazi iz txt-a. Razmaci na kraju i prazni redovi na
 * kraju ćelije se ne čuvaju u datoteci (STANDARD.md, 4), pa se uspoređuje normalizirano.
 */
@Composable
private fun CellEditor(
    text: String,
    key: Any,
    width: Dp,
    padding: Dp,
    bold: Boolean,
    enabled: Boolean,
    style: TextStyle,
    onChange: (String, String) -> Unit,
    onFocus: (Boolean) -> Unit,
) {
    var lokalno by remember(key) { mutableStateOf(TextFieldValue(text)) }
    LaunchedEffect(text) {
        if (Tablice.normaliziraj(lokalno.text) != Tablice.normaliziraj(text)) {
            lokalno = TextFieldValue(text, TextRange(text.length))
        }
    }
    BasicTextField(
        value = lokalno,
        onValueChange = { v ->
            val prije = lokalno.text
            lokalno = v
            if (v.text != prije) onChange(prije, v.text)
        },
        readOnly = !enabled,
        textStyle = if (bold) style.copy(fontWeight = FontWeight.Bold) else style,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
        ),
        modifier = Modifier
            .width(width)
            .padding(horizontal = padding, vertical = 4.dp)
            .onFocusChanged { onFocus(it.isFocused) },
    )
}
