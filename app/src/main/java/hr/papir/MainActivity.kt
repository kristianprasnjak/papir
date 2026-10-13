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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.open(uri, fromPicker = true)
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
                Text(
                    text = vm.title.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    IconButton(onClick = { menuOpen = true }) {
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
                            text = { Text("Spremi kao…") },
                            enabled = !vm.notText,
                            onClick = { menuOpen = false; createDoc.launch(vm.suggestedName()) },
                        )
                    }
                }
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
                value = vm.text,
                onValueChange = vm::onTextChange,
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
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .focusRequester(focus),
            )
        }
    }

    if (vm.pending != null) {
        AlertDialog(
            onDismissRequest = { vm.pending = null },
            title = { Text("Spremiti ovaj papir?") },
            text = { Text("Papir još nije spremljen kao datoteka.") },
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
}
