package com.prijilevschi.library.ui.book

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.prijilevschi.library.R
import com.prijilevschi.library.ui.appViewModel
import com.prijilevschi.library.ui.components.AppIcon
import com.prijilevschi.library.ui.components.BackButton
import com.prijilevschi.library.ui.components.IsoDatePickerDialog
import com.prijilevschi.library.ui.components.Loading
import com.prijilevschi.library.ui.components.SuggestField
import com.prijilevschi.library.ui.components.formatDate
import com.prijilevschi.library.ui.theme.Wood
import com.prijilevschi.library.util.BarcodeScanner
import com.prijilevschi.library.util.Images
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookEditScreen(bookId: Long?, onDone: () -> Unit, onOpenSettings: () -> Unit) {
    val vm = appViewModel { BookEditViewModel(bookId, it.libraryRepository, it.settingsRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LifecycleResumeEffect(Unit) {
        vm.refreshSettings()
        onPauseOrDispose { }
    }
    LaunchedEffect(state.done) { if (state.done) onDone() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.messageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "Add book" else "Edit book") },
                navigationIcon = { BackButton(onDone) },
                actions = {
                    TextButton(onClick = vm::save, enabled = !state.saving && !state.loading) {
                        Text(if (state.saving) "Saving…" else "Save")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (state.loading) {
            Loading(Modifier.padding(padding))
        } else {
            BookForm(state, vm, onOpenSettings, Modifier.padding(padding))
        }
    }
}

@Composable
private fun BookForm(state: BookEditState, vm: BookEditViewModel, onOpenSettings: () -> Unit, modifier: Modifier) {
    val form = state.form
    var pickDate by remember { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CoverPicker(state, vm)

        OutlinedTextField(
            value = form.name,
            onValueChange = { v -> vm.update { it.copy(name = v) } },
            label = { Text("Title") },
            isError = state.nameError != null,
            supportingText = state.nameError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        SuggestField(
            value = form.authorName,
            onValueChange = { v -> vm.update { it.copy(authorName = v) } },
            label = "Author",
            options = state.authors,
            error = state.authorError,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.coAuthors,
            onValueChange = { v -> vm.update { it.copy(coAuthors = v) } },
            label = { Text("Co-authors (optional, comma-separated)") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        IsbnField(state, vm)
        OutlinedTextField(
            value = form.publisher,
            onValueChange = { v -> vm.update { it.copy(publisher = v) } },
            label = { Text("Publisher") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = form.genre,
                onValueChange = { v -> vm.update { it.copy(genre = v) } },
                label = { Text("Genre") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            SuggestField(
                value = form.language,
                onValueChange = { v -> vm.update { it.copy(language = v) } },
                label = "Language",
                options = state.languages,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = form.year,
                onValueChange = { v -> vm.update { it.copy(year = v.filter(Char::isDigit).take(4)) } },
                label = { Text("Year") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = form.pages,
                onValueChange = { v -> vm.update { it.copy(pages = v.filter(Char::isDigit).take(5)) } },
                label = { Text("Pages") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        OutlinedTextField(
            value = form.url,
            onValueChange = { v -> vm.update { it.copy(url = v.trim()) } },
            label = { Text("Link (Goodreads, found automatically when empty)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        RatingRow(form.rating) { rating -> vm.update { it.copy(rating = rating) } }

        Text("Location", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        ShelfPicker(state, vm)
        if (form.shelfId != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Depth row", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { vm.selectSlot(form.shelfId, form.depthRow - 1) }, enabled = form.depthRow > 1) {
                            AppIcon(R.drawable.ic_remove, "Closer to the front")
                        }
                        Text(if (form.depthRow == 1) "1 · front" else "${form.depthRow} · back", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { vm.selectSlot(form.shelfId, form.depthRow + 1) }) {
                            AppIcon(R.drawable.ic_add, "Further back")
                        }
                    }
                }
                OutlinedTextField(
                    value = form.position,
                    onValueChange = { v -> vm.update { it.copy(position = v.filter(Char::isDigit).take(4)) } },
                    label = { Text("Position from left") },
                    isError = state.positionError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            state.positionError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Checkbox(checked = form.read, onCheckedChange = { checked -> vm.update { it.copy(read = checked) } })
            Text("I've read it", Modifier.weight(1f))
            if (form.read) {
                TextButton(onClick = { pickDate = true }) {
                    Text(form.dateRead?.let { "on ${formatDate(it)}" } ?: "Set date (today)")
                }
            }
        }

        Text("Description", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = form.description,
            onValueChange = { v -> vm.update { it.copy(description = v) } },
            placeholder = {
                Text(
                    if (state.hasLlmKey) "Leave empty to have the AI write a short summary when saving"
                    else "Short summary of the book",
                )
            },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.hasLlmKey) {
            FilledTonalButton(onClick = vm::generateSummary, enabled = !state.generating && form.name.isNotBlank()) {
                if (state.generating) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Asking the AI…")
                } else {
                    Text("✨ Generate summary")
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Add an LLM API key in Settings to generate summaries automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onOpenSettings) { Text("Settings") }
            }
        }

        Button(
            onClick = vm::save,
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        ) { Text(if (state.saving) "Saving…" else "Save book") }
    }

    if (pickDate) {
        IsoDatePickerDialog(
            initial = form.dateRead,
            onDismiss = { pickDate = false },
            onPicked = { date -> vm.update { it.copy(dateRead = date) } },
        )
    }
}

/** ISBN input with a barcode-scan button, plus the lookup that fills the other fields from the web. */
@Composable
private fun IsbnField(state: BookEditState, vm: BookEditViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val form = state.form
    OutlinedTextField(
        value = form.isbn,
        onValueChange = { v -> vm.update { it.copy(isbn = v) } },
        label = { Text("ISBN (optional)") },
        isError = state.isbnError != null,
        supportingText = state.isbnError?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = {
                scope.launch {
                    try {
                        BarcodeScanner.scan(context)?.let(vm::onScanned)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        vm.scanFailed("The barcode scanner is not available (needs Google Play services): ${e.message}")
                    }
                }
            }) { AppIcon(R.drawable.ic_camera, "Scan the barcode") }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    FilledTonalButton(
        onClick = vm::lookup,
        enabled = !state.lookingUp && (form.isbn.isNotBlank() || form.name.isNotBlank()),
    ) {
        if (state.lookingUp) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Looking up…")
        } else {
            AppIcon(R.drawable.ic_search, null)
            Spacer(Modifier.width(8.dp))
            Text(if (form.isbn.isNotBlank()) "Look up by ISBN" else "Look up by title and author")
        }
    }
}

/** Five tappable stars; tapping the current rating clears it. */
@Composable
private fun RatingRow(rating: Int?, onRating: (Int?) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Rating", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(end = 12.dp))
        (1..5).forEach { star ->
            Text(
                if (rating != null && star <= rating) "★" else "☆",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(role = Role.Button, onClickLabel = "$star stars") { onRating(if (rating == star) null else star) }
                    .padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun ShelfPicker(state: BookEditState, vm: BookEditViewModel) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(state.selectedShelf?.label ?: if (state.shelves.isEmpty()) "No shelves yet – add one in Manage shelves" else "Not on a shelf")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Not on a shelf") }, onClick = { open = false; vm.selectSlot(null, 1) })
            state.shelves.forEach { shelf ->
                DropdownMenuItem(
                    text = { Text("${shelf.label} (${shelf.orientation.name.lowercase()})") },
                    onClick = { open = false; vm.selectSlot(shelf.id, state.form.depthRow) },
                )
            }
        }
    }
}

@Composable
private fun CoverPicker(state: BookEditState, vm: BookEditViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    fun accept(uri: Uri) {
        scope.launch {
            runCatching { Images.toCoverJpeg(context, uri) }
                .onSuccess { vm.onCoverPicked(uri, it) }
        }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = cameraUri
        if (saved && uri != null) accept(uri)
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) accept(uri)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(96.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(6.dp))
                .background(Wood.Mid),
            contentAlignment = Alignment.Center,
        ) {
            val model: Any? = state.newCoverUri ?: state.newCoverJpeg ?: state.coverUrl.takeUnless { state.removeCover }
            if (model != null) {
                AsyncImage(model = model, contentDescription = "Cover", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text("No cover", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = {
                val uri = Images.newCameraUri(context)
                cameraUri = uri
                try {
                    takePicture.launch(uri)
                } catch (_: ActivityNotFoundException) {
                    // no camera app; the gallery button still works
                }
            }) {
                AppIcon(R.drawable.ic_camera, null)
                Spacer(Modifier.width(8.dp))
                Text("Take photo")
            }
            OutlinedButton(onClick = {
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                AppIcon(R.drawable.ic_image, null)
                Spacer(Modifier.width(8.dp))
                Text("From gallery")
            }
            if (state.newCoverUri != null || state.newCoverJpeg != null || (state.coverUrl != null && !state.removeCover)) {
                TextButton(onClick = vm::removeCover) { Text("Remove cover") }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}
