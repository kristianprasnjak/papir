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
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.geometry.Rect
import android.os.SystemClock
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.ui.text.font.Font
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.drawscope.Stroke
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
        vm.onActivityCreated(recreated = savedInstanceState != null)
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

/** Tanka crta: jedina razdjelnica u Papiru. */
@Composable
private fun Crta() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

/** Alat u traci: ikona i kratki natpis ispod, dodirna površina od prsta. */
@Composable
private fun Alat(ikona: ImageVector, natpis: String, enabled: Boolean = true, aktivan: Boolean = false, onClick: () -> Unit) {
    val boja = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        aktivan -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        Modifier
            .widthIn(min = 64.dp)
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(ikona, contentDescription = null, tint = boja, modifier = Modifier.size(22.dp))
        Text(natpis, fontSize = 11.sp, color = boja, maxLines = 1, modifier = Modifier.padding(top = 3.dp))
    }
}

/** Tekstna radnja: tiha, ili naglašena tintom. */
@Composable
private fun Radnja(tekst: String, enabled: Boolean = true, naglaseno: Boolean = false, onClick: () -> Unit) {
    val boja = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        naglaseno -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        tekst,
        fontSize = 15.sp,
        fontWeight = if (naglaseno) FontWeight.SemiBold else FontWeight.Normal,
        color = boja,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    )
}

/**
 * Traka iznad tipkovnice, na dohvat palca: lijevo poništi i ponovi, desno funkcije.
 * Jedan dodir otvara popis funkcija, drugi umeće funkciju (PAPIR.md, ustav 4).
 * Iznad nje, kad treba: radnje s izabranom slikom ili kutijom, ili s poveznicom pod kursorom.
 */
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
    val traka = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp)
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        val nesto = (izabrani != null && enabled) || (open && enabled) || (enabled && vm.linkAtCursor != null)
        if (nesto) Crta()
        when {
            izabrani != null && ghost != null && enabled -> {
                // Pomicanje ili veličina: strelice, pa potvrda. Ništa se ne mijenja dok se ne potvrdi.
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IkonaGumb(Ikone.lijevo, "Lijevo") { vm.nudge(-1, 0) }
                    IkonaGumb(Ikone.gore, "Gore") { vm.nudge(0, -1) }
                    IkonaGumb(Ikone.dolje, "Dolje") { vm.nudge(0, 1) }
                    IkonaGumb(Ikone.desno, "Desno") { vm.nudge(1, 0) }
                    Spacer(Modifier.weight(1f))
                    Radnja("Odustani", onClick = vm::cancelGhost)
                    Radnja("Potvrdi", enabled = vm.ghostFree, naglaseno = true, onClick = vm::confirmGhost)
                }
            }
            izabrani != null && enabled -> {
                Row(traka) {
                    Alat(Ikone.pomakni, "Pomakni") { vm.startGhost(PapirViewModel.GhostMode.MOVE) }
                    if (izabrani.vrsta == Objekt.Vrsta.SLIKA) {
                        Alat(Ikone.velicina, "Veličina") { vm.startGhost(PapirViewModel.GhostMode.SIZE) }
                        Alat(Ikone.preimenuj, "Ime") { onRenameImage() }
                    }
                    Alat(Ikone.obrisi, "Obriši") { vm.deleteSelected() }
                    Alat(Ikone.gotovo, "Gotovo") { vm.select(null) }
                }
            }
            open && enabled && !textFunctions -> {
                val (retci, stupci) = tableShape ?: (0 to 0)
                Row(traka, verticalAlignment = Alignment.CenterVertically) {
                    Radnja("Redak ispod", tableShape != null) { onTableAction(TableAction.RowBelow) }
                    Radnja("Stupac desno", tableShape != null) { onTableAction(TableAction.ColumnRight) }
                    Radnja("Obriši redak", retci > 1) { onTableAction(TableAction.DeleteRow) }
                    Radnja("Obriši stupac", stupci > 1) { onTableAction(TableAction.DeleteColumn) }
                    Radnja("Obriši tablicu", tableShape != null) { onTableAction(TableAction.DeleteTable) }
                }
            }
            open && enabled -> {
                val current = vm.currentStyle
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FormatButton("B", "Podebljano", current.podebljano, vm::toggleBold, bold = true)
                    FormatButton("I", "Kurziv", current.kurziv, vm::toggleItalic, italic = true)
                    Spacer(Modifier.width(12.dp))
                    listOf(0 to "M", 1 to "L", 2 to "XL").forEach { (v, label) ->
                        FormatButton(label, "Veličina $label", current.velicina == v, { vm.setSize(v) })
                    }
                }
                Row(traka) {
                    Alat(Ikone.kvacica, "Kvačica") { onClose(); vm.insertCheckbox() }
                    Alat(Ikone.mapa, "Mapa") { onClose(); vm.insertFolder() }
                    Alat(Ikone.poveznica, "Poveznica") { onClose(); onLink() }
                    Alat(Ikone.tablica, "Tablica") { onClose(); vm.insertTable() }
                    Alat(Ikone.slika, "Slika") { onClose(); onImage() }
                    Alat(Ikone.kutija, "Kutija") { onClose(); vm.insertBox() }
                    Alat(Ikone.sat, "Vrijeme") { onClose(); vm.insertDateTime() }
                }
            }
            enabled && vm.linkAtCursor != null -> {
                val cilj = vm.linkAtCursor.orEmpty()
                Row(
                    Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenLink(cilj) }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Ikone.otvori, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text(
                            cilj,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    }
                    Radnja("Ukloni", onClick = vm::removeLink)
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IkonaGumb(Ikone.ponisti, "Poništi", enabled && vm.canUndo, onClick = vm::undo)
            IkonaGumb(Ikone.ponovi, "Ponovi", enabled && vm.canRedo, onClick = vm::redo)
            Spacer(Modifier.weight(1f))
            IkonaGumb(if (open) Ikone.zatvori else Ikone.plus, if (open) "Zatvori funkcije" else "Funkcije", enabled, onClick = onToggle)
        }
    }
}

@Composable
private fun IkonaGumb(ikona: ImageVector, opis: String, enabled: Boolean = true, boja: Color = Color.Unspecified, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.semantics { contentDescription = opis }) {
        val tint = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
            boja != Color.Unspecified -> boja
            else -> MaterialTheme.colorScheme.onSurface
        }
        Icon(ikona, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
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
            .clip(CircleShape)
            .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 16.sp,
            fontFamily = PapirMono,
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
    /** Znakovi □ ■ ► ▼ koje aplikacija crta sama: u tekstu su nevidljivi. */
    private val zivi: Set<Int> = emptySet(),
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
        for (i in zivi) if (i < n) b.addStyle(SpanStyle(color = Color.Transparent), znak(i), znak(i) + 1)
        // naslov: jedan odlomak, visok koliko su mu velika slova
        var od = 0
        while (od <= n) {
            val kraj = text.indexOf('\n', od).let { if (it < 0) n else it }
            val v = stilovi.getOrNull(od)?.velicina ?: 0
            if (v > 0 && kraj > od) {
                val doKraja = if (kraj < n) znak(kraj) + 1 else (n + prije[n])
                // naslov je na ekranu jedan redak svoje veličine; crta ispod njega postoji samo u datoteci
                b.addStyle(ParagraphStyle(lineHeight = (RED * Stil.mjera(v)).sp, lineHeightStyle = sredinaRetka), od + prije[od], doKraja)
            }
            od = kraj + 1
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
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
        Text(
            "Datoteka je promijenjena izvan Papira.",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error,
        )
        Text(
            "Ništa nije prepisano. Koju verziju zadržati? Druga ostaje kao zasebna datoteka u mapi.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Radnja("Onu izvana", onClick = onLoadTheirs)
            Radnja("Ovu ovdje", naglaseno = true, onClick = onKeepMine)
        }
        Crta()
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
        Text("Papir", fontFamily = PapirMono, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(
            "Svaki papir je obična datoteka u mapi koju izabereš. Biraš samo jednom.",
            fontSize = 16.sp,
            lineHeight = 23.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 14.dp, bottom = 18.dp),
        )
        Text(
            "Odaberi mapu",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onChoose)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

/** Jednoredno polje bez okvira, s tankom crtom ispod. */
@Composable
private fun Polje(
    vrijednost: String,
    onChange: (String) -> Unit,
    natpis: String,
    modifier: Modifier = Modifier,
    fokus: FocusRequester? = null,
) {
    BasicTextField(
        value = vrijednost,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
        modifier = modifier.let { if (fokus != null) it.focusRequester(fokus) else it },
        decorationBox = { unutra ->
            Box(Modifier.padding(vertical = 10.dp)) {
                if (vrijednost.isEmpty()) Text(natpis, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                unutra()
            }
        },
    )
}

/** Traži i zamijeni, kao u Notepadu (PAPIR.md, 5). */
@Composable
private fun SearchBar(vm: PapirViewModel, onClose: () -> Unit) {
    var zamjena by remember { mutableStateOf("") }
    val fokus = remember { FocusRequester() }
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Ikone.trazi, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Polje(vm.searchQuery, { vm.search(it) }, "Traži", Modifier.weight(1f).padding(start = 10.dp), fokus)
            Text(
                if (vm.matches.isEmpty()) "0" else "${vm.currentMatch + 1}/${vm.matches.size}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            IkonaGumb(Ikone.gore, "Prethodno", vm.matches.isNotEmpty()) { vm.nextMatch(-1) }
            IkonaGumb(Ikone.dolje, "Sljedeće", vm.matches.isNotEmpty()) { vm.nextMatch(1) }
            IkonaGumb(Ikone.zatvori, "Zatvori traženje") { vm.endSearch(); onClose() }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(30.dp))
            Polje(zamjena, { zamjena = it }, "Zamijeni s", Modifier.weight(1f))
            FormatButton("Aa", "Velika i mala slova", vm.matchCase, { vm.search(vm.searchQuery, !vm.matchCase) })
            Radnja("Zamijeni", !vm.readOnly && vm.matches.isNotEmpty()) { vm.replaceCurrent(zamjena) }
            Radnja("Sve", !vm.readOnly && vm.matches.isNotEmpty()) { vm.replaceAll(zamjena) }
        }
        Crta()
    }
    LaunchedEffect(Unit) { fokus.requestFocus() }
}

/** Font papira: JetBrains Mono NL, ugrađen u aplikaciju (fonts/README.md). */
private val PapirMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.jetbrains_mono_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.jetbrains_mono_bold_italic, FontWeight.Bold, FontStyle.Italic),
)

/** Jedan papir, jedna tinta: plavo-crna je jedina boja Papira. */
@Composable
private fun PapirTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = Color(0xFFA9BEDF), onPrimary = Color(0xFF121315),
            background = Color(0xFF121315), surface = Color(0xFF121315),
            onBackground = Color(0xFFE4E6EA), onSurface = Color(0xFFE4E6EA),
            onSurfaceVariant = Color(0xFF8E949E), surfaceVariant = Color(0xFF1C1E22),
            outline = Color(0xFF3A3F47), outlineVariant = Color(0xFF2A2D33),
            error = Color(0xFFF2B8B5), surfaceContainer = Color(0xFF1C1E22),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF1F2A3C), onPrimary = Color.White,
            background = Color.White, surface = Color.White,
            onBackground = Color(0xFF1B1E23), onSurface = Color(0xFF1B1E23),
            onSurfaceVariant = Color(0xFF6B717C), surfaceVariant = Color(0xFFF3F4F6),
            outline = Color(0xFFC9CDD4), outlineVariant = Color(0xFFE6E8EC),
            error = Color(0xFFB3261E), surfaceContainer = Color(0xFFF7F8FA),
        )
    }
    MaterialTheme(colorScheme = colors) {
        val odabir = TextSelectionColors(handleColor = colors.primary, backgroundColor = colors.primary.copy(alpha = 0.22f))
        CompositionLocalProvider(LocalTextSelectionColors provides odabir, content = content)
    }
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
    BackHandler(enabled = vm.browser != null) { vm.closeBrowser() }

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
            } else if (showPaper && vm.browser != null) {
                BrowserScreen(vm)
            } else if (showPaper) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IkonaGumb(Ikone.papiri, "Papiri") { vm.openBrowser() }
                    Text(
                        text = vm.title ?: "novi papir",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                    )
                    Box {
                        IkonaGumb(Ikone.vise, "Izbornik", boja = MaterialTheme.colorScheme.onSurfaceVariant) { menuOpen = true }
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

/**
 * Popis papira u mapi (PAPIR.md, 9): najnoviji gore, ime, prvi redak i vrijeme. Samo otvara;
 * brisanje i premještanje radi upravitelj datoteka.
 */
@Composable
private fun BrowserScreen(vm: PapirViewModel) {
    var upit by remember { mutableStateOf("") }
    val svi = vm.browser.orEmpty()
    val vidljivi = remember(svi, upit) {
        val q = upit.trim()
        if (q.isEmpty()) svi else svi.filter { it.ime.contains(q, ignoreCase = true) || it.tekst.contains(q, ignoreCase = true) }
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IkonaGumb(Ikone.natrag, "Natrag") { vm.closeBrowser() }
            Text("Papiri", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f).padding(start = 4.dp))
            IkonaGumb(Ikone.plus, "Novi papir") { vm.newFromBrowser() }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Ikone.trazi, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Polje(upit, { upit = it }, "Traži po imenu ili tekstu", Modifier.weight(1f).padding(start = 10.dp))
        }
        Crta()
        when {
            vm.browserLoading -> Text("Čitam mapu…", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp))
            vidljivi.isEmpty() -> Text(
                if (upit.isBlank()) "U mapi još nema papira. Novi papir postaje datoteka čim dobije prvo slovo." else "Ništa ne odgovara traženju.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp),
            )
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(vidljivi, key = { it.uri.toString() }) { u ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { vm.openFromBrowser(u.uri) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                u.ime,
                                fontSize = 16.sp,
                                color = if (u.jePapir) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val prvi = u.prviRed
                            if (!prvi.isNullOrEmpty()) {
                                Text(prvi, fontSize = 13.sp, fontFamily = PapirMono, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                        Text(vrijeme(u.izmijenjeno), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        }
    }
}

/** Kad je datoteka mijenjana: danas sat, ove godine dan i mjesec, inače i godina. */
private fun vrijeme(ms: Long): String {
    if (ms <= 0L) return ""
    val t = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault())
    val sada = java.time.ZonedDateTime.now()
    val f = when {
        t.toLocalDate() == sada.toLocalDate() -> "HH:mm"
        t.year == sada.year -> "d. M."
        else -> "d. M. yyyy."
    }
    return t.format(java.time.format.DateTimeFormatter.ofPattern(f))
}

/** Greška ostaje dok je ne dodirneš; obična obavijest je siva i sama nestane. */
private fun jeGreska(t: String): Boolean =
    listOf("nije uspjel", "nije moguće", "ne može", "Ne može", "ne smije", "nije UTF-8", "nije papir", "nije pronađen",
        "Odaberi mapu", "Tu je tekst", "Nijedna aplikacija", "ne mogu").any { it in t }

@Composable
private fun MessageLine(text: String, onDismiss: () -> Unit) {
    val greska = jeGreska(text)
    if (!greska) {
        LaunchedEffect(text) {
            kotlinx.coroutines.delay(4000)
            onDismiss()
        }
    }
    Text(
        text = text,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        color = if (greska) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDismiss)
            .padding(horizontal = 20.dp, vertical = 6.dp),
    )
}

/** Visina jednog retka mreže (sp): jedan redak datoteke je jedan redak ekrana. */
private const val RED = 26

private val sredinaRetka = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

@Composable
private fun papirStil(): TextStyle = TextStyle(
    fontFamily = PapirMono,
    fontSize = 16.sp,
    // obični retci iste visine, kao u Notepadu; naslov je jedan redak svoje veličine
    lineHeight = RED.sp,
    lineHeightStyle = sredinaRetka,
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

    // Izbornik Zalijepi / Odaberi sve samo kad je tekst odabran ili nakon dugog pritiska, a ne na svaki dodir.
    val dugiPritisak = remember { longArrayOf(0L) }
    val osnovniIzbornik = LocalTextToolbar.current
    val izbornik = remember(osnovniIzbornik) { PapirToolbar(osnovniIzbornik) { dugiPritisak[0] } }

    CompositionLocalProvider(LocalTextToolbar provides izbornik) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val ishod: Any? = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        waitForUpOrCancellation(pass = PointerEventPass.Initial) ?: "otkazano"
                    }
                    if (ishod == null) dugiPritisak[0] = SystemClock.uptimeMillis()
                }
            },
    ) {
        val sirinaEkrana = maxWidth
        val sirinaPx = with(density) { maxWidth.toPx() }
        val visinaPx = with(density) { maxHeight.toPx() }
        // obris koji se pomiče ili širi ostaje na ekranu
        LaunchedEffect(vm.ghost) {
            val g = vm.ghost ?: return@LaunchedEffect
            val padding = with(density) { 20.dp.toPx() }
            val desno = padding + (g.zadnjiStupac + 2) * znak
            val lijevo = padding + (g.stupac - 1) * znak
            if (desno > hScroll.value + sirinaPx) hScroll.animateScrollTo((desno - sirinaPx).roundToInt())
            else if (lijevo < hScroll.value) hScroll.animateScrollTo(lijevo.roundToInt().coerceAtLeast(0))
            val linija = vm.lineOfRawRow(g.zadnjiRedak)
            polozaji[linija]?.let { (_, dno) ->
                if (dno + padding > vScroll.value + visinaPx) vScroll.animateScrollTo((dno + padding - visinaPx).roundToInt())
            }
        }
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
                Markers(vm, polozaji, znak, density)
                Objects(vm, polozaji, znak, density)
            }
        }
    }
    }
}

/** Izbornik teksta koji se pokazuje kad je tekst odabran ili nakon dugog pritiska. */
private class PapirToolbar(private val base: TextToolbar, private val dugiPritisak: () -> Long) : TextToolbar by base {
    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        val odabrano = onCopyRequested != null || onCutRequested != null
        if (odabrano || SystemClock.uptimeMillis() - dugiPritisak() < 1500) {
            base.showMenu(rect, onCopyRequested, onPasteRequested, onCutRequested, onSelectAllRequested)
        } else {
            base.hide()
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
                    .border(if (izabran) 1.5.dp else 1.dp, if (izabran) izabranaBoja else linija),
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
        val slobodno = vm.ghostFree
        val boja = if (slobodno) izabranaBoja else MaterialTheme.colorScheme.error
        // novi položaj ili veličina, sa slikom unutra, da se vidi kako će izgledati
        Box(
            Modifier
                .offset { IntOffset((g.stupac * znak).roundToInt(), y.roundToInt()) }
                .size(with(density) { (g.sirina * znak).toDp() }, with(density) { h.toDp() })
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
                .border(1.5.dp, boja),
        ) {
            if (g.vrsta == Objekt.Vrsta.SLIKA) ImageObject(vm, g, odabir = false)
            Text(
                "${g.sirina} × ${g.visina}" + when {
                    !slobodno -> " · tu je tekst"
                    vm.ghostMode == PapirViewModel.GhostMode.SIZE && g.vrsta == Objekt.Vrsta.SLIKA &&
                        g.sirina <= g.sadrzaj.codePointCount(0, g.sadrzaj.length) + 4 -> " · uže ne može dok ime ne stane (Preimenuj)"
                    else -> ""
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.align(Alignment.TopStart).background(boja).padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun ImageObject(vm: PapirViewModel, o: Objekt, odabir: Boolean = true) {
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
            .let { if (odabir) it.clickable { vm.select(o) } else it },
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
    val zivi = remember(dio, vm.isKeyed) { if (vm.isKeyed) markerPositions(dio).map { it.first }.toSet() else emptySet() }
    val prikaz = remember(dijelStilova, praznine, oznake, trenutna, boje, zivi) {
        StilPrikaz(dijelStilova, praznine, oznake, trenutna, boje.primary, boje.primary.copy(alpha = 0.18f), boje.primary.copy(alpha = 0.4f), zivi)
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

/** Gdje su u tekstu živi znakovi: (pozicija znaka, redak u tekstu). */
private fun markerPositions(tekst: String): List<Pair<Int, Int>> {
    val out = ArrayList<Pair<Int, Int>>()
    var od = 0
    var redak = 0
    while (od <= tekst.length) {
        val kraj = tekst.indexOf('\n', od).let { if (it < 0) tekst.length else it }
        val l = tekst.substring(od, kraj)
        if (Mape.jeKvacica(l) || Mape.zaglavlje(l) != null) out.add(od + Mape.uvlaka(l) to redak)
        od = kraj + 1
        redak++
    }
    return out
}

/**
 * Kvačice i strelice mapa: element sučelja, ne slovo (PAPIR.md, 6). Dodirna površina je od
 * prsta; dodir mijenja jedan znak u datoteci.
 */
@Composable
private fun Markers(vm: PapirViewModel, polozaji: Polozaji, znak: Float, density: androidx.compose.ui.unit.Density) {
    if (!vm.isKeyed || vm.readOnly) return
    val tekst = vm.content.text
    val boja = MaterialTheme.colorScheme.primary
    val podloga = MaterialTheme.colorScheme.background
    val vidljivi = markerPositions(tekst).mapNotNull { (pozicija, linija) -> polozaji[linija]?.let { Triple(pozicija, it.first, it.second) } }
    for ((pozicija, vrh, dno) in vidljivi) {
        val c = tekst[pozicija]
        val stupac = pozicija - (tekst.lastIndexOf('\n', pozicija - 1) + 1)
        val visina = minOf(dno - vrh, with(density) { (RED * 2).sp.toPx() }).let { if (it <= 0f) 1f else it }
        val vrhRetka = vrh + ((dno - vrh) - visina) / 2
        val dodir = with(density) { 44.dp.toPx() }
        val x = stupac * znak + znak / 2 - dodir / 2
        val y = vrhRetka + visina / 2 - dodir / 2
        Box(
            Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .size(44.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { vm.toggleAt(pozicija) }
                .semantics {
                    contentDescription = when (c) {
                        Mape.PRAZNO -> "Označi"
                        Mape.OZNACENO -> "Odznači"
                        Mape.ZATVORENO -> "Otvori mapu"
                        else -> "Zatvori mapu"
                    }
                }
                .drawBehind {
                    val a = minOf(znak * 1.1f, size.width * 0.42f)
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val l = cx - a / 2
                    val t = cy - a / 2
                    when (c) {
                        Mape.PRAZNO, Mape.OZNACENO -> {
                            val w = 2.dp.toPx()
                            drawRoundRect(boja, Offset(l, t), androidx.compose.ui.geometry.Size(a, a), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()), style = Stroke(w))
                            if (c == Mape.OZNACENO) {
                                drawRoundRect(boja, Offset(l, t), androidx.compose.ui.geometry.Size(a, a), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                                val kvaka = Path().apply {
                                    moveTo(l + a * 0.22f, t + a * 0.52f)
                                    lineTo(l + a * 0.43f, t + a * 0.72f)
                                    lineTo(l + a * 0.78f, t + a * 0.3f)
                                }
                                drawPath(kvaka, podloga, style = Stroke(w))
                            }
                        }
                        else -> {
                            val trokut = Path().apply {
                                if (c == Mape.OTVORENO) {
                                    moveTo(l, t + a * 0.2f); lineTo(l + a, t + a * 0.2f); lineTo(cx, t + a * 0.85f)
                                } else {
                                    moveTo(l + a * 0.2f, t); lineTo(l + a * 0.85f, cy); lineTo(l + a * 0.2f, t + a)
                                }
                                close()
                            }
                            drawPath(trokut, boja)
                        }
                    }
                },
        )
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
    val red = with(density) { RED.sp.toDp() }
    // Na mreži Notepada: svaki zid je jedan stupac, svaka crta jedan redak; ćelija je razmak, tekst, razmak.
    val sirine = t.sirine.map { znak * (maxOf(it, 1) + 2) }
    val ukupno = sirine.fold(znak) { a, b -> a + b + znak }
    val visine = t.retci.map { row -> row.maxOf { it.trimEnd('\n').split("\n").size }.coerceAtLeast(1) }
    val linija = MaterialTheme.colorScheme.outline
    Column(
        modifier.drawBehind {
            val r = red.toPx()
            val z = znak.toPx()
            val debljina = 1.dp.toPx()
            val vrh = r / 2
            val crte = ArrayList<Float>()
            var y = r
            for (v in visine) {
                y += v * r
                crte.add(y + r / 2)
                y += r
            }
            val dno = crte.last()
            val sirina = ukupno.toPx()
            drawLine(linija, Offset(z / 2, vrh), Offset(sirina - z / 2, vrh), strokeWidth = debljina)
            crte.forEachIndexed { k, cy ->
                if (k == 0 && t.zaglavlje && crte.size > 1) {
                    // ╞═╪╡: dvostruka crta ispod zaglavlja
                    drawLine(linija, Offset(z / 2, cy - 2.dp.toPx()), Offset(sirina - z / 2, cy - 2.dp.toPx()), strokeWidth = debljina)
                    drawLine(linija, Offset(z / 2, cy + 2.dp.toPx()), Offset(sirina - z / 2, cy + 2.dp.toPx()), strokeWidth = debljina)
                } else {
                    drawLine(linija, Offset(z / 2, cy), Offset(sirina - z / 2, cy), strokeWidth = debljina)
                }
            }
            var x = z / 2
            drawLine(linija, Offset(x, vrh), Offset(x, dno), strokeWidth = debljina)
            for (w in sirine) {
                x += w.toPx() + z
                drawLine(linija, Offset(x, vrh), Offset(x, dno), strokeWidth = debljina)
            }
        },
    ) {
        Spacer(Modifier.width(ukupno).height(red))
        t.retci.forEachIndexed { r, row ->
            Row(Modifier.height(red * visine[r])) {
                Spacer(Modifier.width(znak))
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
                    Spacer(Modifier.width(znak))
                }
            }
            Spacer(Modifier.height(red))
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
            .padding(horizontal = padding)
            .onFocusChanged { onFocus(it.isFocused) },
    )
}
