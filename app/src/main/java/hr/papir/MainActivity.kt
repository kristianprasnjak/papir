package hr.papir

import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.key
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

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
 * Jedan dodir otvara popis funkcija, drugi umeće funkciju (PAPIR.md, ustav 4).
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
    current: Stil,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onSize: (Int) -> Unit,
    onDateTime: () -> Unit,
    textFunctions: Boolean,
    onTable: () -> Unit,
    tableShape: Pair<Int, Int>?,
    onTableAction: (TableAction) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        if (open && !textFunctions) {
            // U ćeliji: radnje s tablicom. Svaka je jedan korak za ↶.
            val (retci, stupci) = tableShape ?: (0 to 0)
            FunctionItem("Novi redak ispod", { onTableAction(TableAction.RowBelow) }, tableShape != null)
            FunctionItem("Novi stupac desno", { onTableAction(TableAction.ColumnRight) }, tableShape != null)
            FunctionItem("Obriši redak", { onTableAction(TableAction.DeleteRow) }, retci > 1)
            FunctionItem("Obriši stupac", { onTableAction(TableAction.DeleteColumn) }, stupci > 1)
            FunctionItem("Obriši tablicu", { onTableAction(TableAction.DeleteTable) }, tableShape != null)
        }
        if (open && textFunctions) {
            // Oblikovanje: djeluje na odabrani tekst; veličina djeluje na cijele retke.
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FormatButton("B", "Podebljano", current.podebljano, onBold, bold = true)
                FormatButton("I", "Kurziv", current.kurziv, onItalic, italic = true)
                Spacer(Modifier.width(8.dp))
                listOf(-2 to "XS", -1 to "S", 0 to "M", 1 to "L", 2 to "XL").forEach { (v, label) ->
                    FormatButton(label, "Veličina $label", current.velicina == v, { onSize(v) })
                }
            }
            FunctionItem("Datum i vrijeme", onDateTime)
            FunctionItem("Tablica", onTable)
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

/** Prikaz oblikovanja: oznake su već uklonjene iz teksta, ovdje se samo crta stil (pozicije se ne mijenjaju). */
private class StilPrikaz(private val stilovi: List<Stil>) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val b = AnnotatedString.Builder(text)
        for ((od, doo, s) in Oblik.dijelovi(stilovi)) {
            if (doo <= text.length) b.addStyle(spanOf(s), od, doo)
        }
        return TransformedText(b.toAnnotatedString(), OffsetMapping.Identity)
    }

    private fun spanOf(s: Stil) = SpanStyle(
        fontWeight = if (s.podebljano) FontWeight.Bold else null,
        fontStyle = if (s.kurziv) FontStyle.Italic else null,
        fontSize = when (s.velicina) {
            2 -> 1.7.em
            1 -> 1.3.em
            -1 -> 0.85.em
            -2 -> 0.7.em
            else -> TextUnit.Unspecified
        },
    )
}

/** Radnje s tablicom iz popisa funkcija, dok je kursor u ćeliji. */
private enum class TableAction { RowBelow, ColumnRight, DeleteRow, DeleteColumn, DeleteTable }

@Composable
private fun FunctionItem(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f),
        )
    }
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
    var renaming by remember { mutableStateOf<TextFieldValue?>(null) }
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
        if (uri != null) vm.saveAs(uri)
    }

    val showPaper = vm.loaded && !vm.needsFolder

    val tekst = vm.content.text
    val linije = remember(tekst) { tekst.split("\n") }
    val odsjecci = remember(linije.size, vm.tables) { Odsjecci.podijeli(linije.size, vm.tables) }
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
                odsjecci[i].tablica == null && Odsjecci.pocetak(tekst, odsjecci[i].od) <= kursor
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
                    open = functionsOpen && !vm.readOnly,
                    enabled = !vm.readOnly,
                    canUndo = vm.canUndo,
                    canRedo = vm.canRedo,
                    onUndo = vm::undo,
                    onRedo = vm::redo,
                    onToggle = { functionsOpen = !functionsOpen },
                    current = vm.currentStyle,
                    onBold = vm::toggleBold,
                    onItalic = vm::toggleItalic,
                    onSize = vm::setSize,
                    onDateTime = { functionsOpen = false; vm.insertDateTime() },
                    textFunctions = focusedCell == null,
                    onTable = { functionsOpen = false; vm.insertTable() },
                    tableShape = tableShape,
                    onTableAction = { onTableAction(it) },
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

/**
 * Papir kao niz odsječaka: tekst i tablice (ARHITEKTURA.md). Papir bez tablica je jedan
 * tekstni odsječak, kao i prije. Sve izmjene idu kroz model, koji ih prepiše u txt.
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
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        odsjecci.forEachIndexed { i, o ->
            val tablica = o.tablica
            if (tablica != null) {
                key("tablica", tablica) {
                    val blok = linije.subList(o.od, o.doo)
                    val t = remember(blok) { Tablice.citaj(blok) }
                    if (t != null) {
                        TableView(
                            t = t,
                            index = tablica,
                            enabled = !vm.readOnly,
                            focusedCell = focusedCell,
                            onFocusCell = onFocusCell,
                            onCell = { r, c, celija -> vm.editCell(tablica, r, c, celija) },
                        )
                    }
                }
            } else {
                key("tekst", i) {
                    TextSegment(
                        vm = vm,
                        tekst = tekst,
                        linije = linije,
                        odsjecak = o,
                        requester = requesters.getOrNull(i),
                        onFocused = { onFocusCell(null) },
                    )
                }
            }
        }
        // Dodir ispod teksta vodi na kraj papira.
        Box(
            Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTapBelow,
                ),
        )
    }
}

@Composable
private fun TextSegment(
    vm: PapirViewModel,
    tekst: String,
    linije: List<String>,
    odsjecak: Odsjecak,
    requester: FocusRequester?,
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
    val prikaz = remember(dijelStilova) { StilPrikaz(dijelStilova) }

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
            modifier = Modifier
                .fillMaxWidth()
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
    onCell: (Int, Int, String) -> Unit,
) {
    val stil = papirStil()
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val znak = remember(stil) { with(density) { measurer.measure("0", stil).size.width.toDp() } }
    // Prikaz smije saviti: vrlo uski stupac crta se malo šire, da se ćelija može dodirnuti.
    val sirine = t.sirine.map { znak * (maxOf(it, 3) + 2) }
    val ukupno = sirine.fold(0.dp) { a, b -> a + b }
    val linija = MaterialTheme.colorScheme.outline
    Box(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 6.dp),
    ) {
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
                            onChange = { onCell(r, c, it) },
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
    onChange: (String) -> Unit,
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
            val promijenjeno = v.text != lokalno.text
            lokalno = v
            if (promijenjeno) onChange(v.text)
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
