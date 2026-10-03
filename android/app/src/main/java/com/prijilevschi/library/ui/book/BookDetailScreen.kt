package com.prijilevschi.library.ui.book

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.prijilevschi.library.R
import com.prijilevschi.library.data.Book
import com.prijilevschi.library.ui.appViewModel
import com.prijilevschi.library.ui.components.AppIcon
import com.prijilevschi.library.ui.components.BackButton
import com.prijilevschi.library.ui.components.ErrorMessage
import com.prijilevschi.library.ui.components.IsoDatePickerDialog
import com.prijilevschi.library.ui.components.Loading
import com.prijilevschi.library.ui.components.formatDate
import com.prijilevschi.library.ui.components.formatInstant
import com.prijilevschi.library.ui.theme.Wood

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(bookId: Long, onBack: () -> Unit, onEdit: () -> Unit) {
    val vm = appViewModel { BookDetailViewModel(bookId, it.libraryRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        vm.load()
        onPauseOrDispose { }
    }
    val snackbar = remember { SnackbarHostState() }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    LaunchedEffect(state.error, state.book) {
        val error = state.error
        if (error != null && state.book != null) {
            snackbar.showSnackbar(error)
            vm.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    if (state.book != null) {
                        IconButton(onClick = onEdit) { AppIcon(R.drawable.ic_edit, "Edit") }
                        IconButton(onClick = { confirmDelete = true }) { AppIcon(R.drawable.ic_delete, "Delete") }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val book = state.book
        Box(Modifier.padding(padding)) {
            when {
                state.loading -> Loading()
                book == null -> ErrorMessage(state.error ?: "Book not found", onRetry = vm::load)
                else -> BookDetails(book, state.coverUrl, onRead = vm::setRead)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete book?") },
            text = { Text("\"${state.book?.name}\" will be removed from your library.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookDetails(book: Book, coverUrl: String?, onRead: (Boolean, String?) -> Unit) {
    var pickDate by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .width(130.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Wood.Mid),
                contentAlignment = Alignment.Center,
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = "Cover of ${book.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(book.name, color = Color.White, modifier = Modifier.padding(8.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(book.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(book.author.name, style = MaterialTheme.typography.titleMedium)
                if (book.coAuthors.isNotEmpty()) {
                    Text("with ${book.coAuthors.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
                }
                val published = listOfNotNull(book.publisher, book.year?.toString()).joinToString(", ")
                if (published.isNotEmpty()) Text(published, style = MaterialTheme.typography.bodyMedium)
                book.rating?.let { rating ->
                    val stars = Math.round(rating).toInt().coerceIn(0, 5)
                    Text("★".repeat(stars) + "☆".repeat(5 - stars), color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Where it is", style = MaterialTheme.typography.labelMedium)
                Text("📍 ${book.locationLabel}", style = MaterialTheme.typography.titleMedium)
            }
        }

        Card {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (book.read) "Read" else "Not read yet", style = MaterialTheme.typography.titleMedium)
                    if (book.read && book.dateRead != null) {
                        TextButton(onClick = { pickDate = true }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                            Text("on ${formatDate(book.dateRead)} · change")
                        }
                    }
                }
                Switch(checked = book.read, onCheckedChange = { onRead(it, null) })
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            book.genre?.let { AssistChip(onClick = {}, label = { Text(it) }) }
            book.language?.let { AssistChip(onClick = {}, label = { Text(it) }) }
            book.pages?.let { AssistChip(onClick = {}, label = { Text("$it pages") }) }
            book.isbn?.let { AssistChip(onClick = {}, label = { Text("ISBN $it") }) }
            book.url?.let { link -> AssistChip(onClick = { uriHandler.openUri(link) }, label = { Text("Open on the web") }) }
        }

        Text("Description", style = MaterialTheme.typography.titleMedium)
        Text(
            book.description ?: "No description yet. Edit the book to add one or generate it with AI.",
            style = MaterialTheme.typography.bodyLarge,
            color = if (book.description == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            "Added ${formatInstant(book.createdAt)} · Modified ${formatInstant(book.modifiedAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.padding(bottom = 24.dp))
    }

    if (pickDate) {
        IsoDatePickerDialog(initial = book.dateRead, onDismiss = { pickDate = false }, onPicked = { onRead(true, it) })
    }
}
