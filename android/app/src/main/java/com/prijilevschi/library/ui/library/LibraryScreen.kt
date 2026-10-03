package com.prijilevschi.library.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prijilevschi.library.R
import com.prijilevschi.library.data.Book
import com.prijilevschi.library.data.ShelfOrientation
import com.prijilevschi.library.ui.appViewModel
import com.prijilevschi.library.ui.components.AppIcon
import com.prijilevschi.library.ui.components.ErrorMessage
import com.prijilevschi.library.ui.components.Loading
import com.prijilevschi.library.ui.components.ReadBadge
import com.prijilevschi.library.ui.components.ShelfView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBookClick: (Long) -> Unit,
    onAddBook: () -> Unit,
    onShelves: () -> Unit,
    onSettings: () -> Unit,
) {
    val vm = appViewModel { LibraryViewModel(it.libraryRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Library") },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) { AppIcon(R.drawable.ic_more_vert, "Menu") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Manage shelves") }, onClick = { menuOpen = false; onShelves() })
                            DropdownMenuItem(text = { Text("Settings") }, onClick = { menuOpen = false; onSettings() })
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddBook) { AppIcon(R.drawable.ic_add, "Add book") }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SearchBar(state, vm)
            when {
                state.loading -> Loading()
                state.error != null && state.books.isEmpty() && state.shelves.isEmpty() ->
                    ErrorMessage(state.error!!, onRetry = vm::refresh, onOpenSettings = onSettings)
                else -> LibraryContent(state, onBookClick, onShelves)
            }
        }
    }
}

@Composable
private fun SearchBar(state: LibraryState, vm: LibraryViewModel) {
    var languageMenu by remember { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Title, author or ISBN") },
            leadingIcon = { AppIcon(R.drawable.ic_search, null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { vm.onQueryChange("") }) { AppIcon(R.drawable.ic_close, "Clear") }
                }
            },
            singleLine = true,
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReadFilter.entries.forEach { filter ->
                FilterChip(
                    selected = state.readFilter == filter,
                    onClick = { vm.onReadFilter(filter) },
                    label = { Text(filter.label) },
                )
            }
            if (state.languages.isNotEmpty()) {
                Box {
                    FilterChip(
                        selected = state.language != null,
                        onClick = { languageMenu = true },
                        label = { Text(state.language ?: "Language") },
                    )
                    DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
                        DropdownMenuItem(text = { Text("Any language") }, onClick = { languageMenu = false; vm.onLanguage(null) })
                        state.languages.forEach { language ->
                            DropdownMenuItem(text = { Text(language) }, onClick = { languageMenu = false; vm.onLanguage(language) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryContent(state: LibraryState, onBookClick: (Long) -> Unit, onShelves: () -> Unit) {
    val matchIds = state.matchIds
    val byShelf = state.books.groupBy { it.shelf?.id }
    val unshelved = byShelf[null].orEmpty()

    if (state.books.isEmpty() && state.shelves.isEmpty()) {
        ErrorMessage("Your library is empty. Add a shelf first, then add books with the + button.", onRetry = null, onOpenSettings = null)
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        state.error?.let { error ->
            item { Text(error, color = MaterialTheme.colorScheme.error) }
        }
        val matches = state.matches
        if (matches != null) {
            item {
                Text(
                    if (matches.isEmpty()) "No books found" else "${matches.size} found",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            items(matches, key = { "match-${it.id}" }) { book -> MatchRow(book) { onBookClick(book.id) } }
        }
        if (state.shelves.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth().clickable(onClick = onShelves)) {
                    Text("No shelves yet. Tap to add your first shelf.", Modifier.padding(16.dp))
                }
            }
        }
        items(state.shelves, key = { "shelf-${it.id}" }) { shelf ->
            val books = byShelf[shelf.id].orEmpty()
            val count = matchIds?.let { ids -> books.count { it.id in ids } }
            if (count == 0) return@items // hide shelves without matches while searching
            ShelfView(
                title = shelf.label,
                subtitle = count?.let { "$it found" } ?: "${books.size} books",
                orientation = shelf.orientation,
                books = books,
                highlighted = matchIds,
                onBookClick = { onBookClick(it.id) },
            )
        }
        if (unshelved.isNotEmpty() && (matchIds == null || unshelved.any { it.id in matchIds })) {
            item(key = "unshelved") {
                ShelfView(
                    title = "Unshelved",
                    subtitle = "${unshelved.size} books",
                    orientation = ShelfOrientation.VERTICAL,
                    books = unshelved,
                    highlighted = matchIds,
                    onBookClick = { onBookClick(it.id) },
                )
            }
        }
    }
}

@Composable
private fun MatchRow(book: Book, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(book.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(book.author.name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "📍 ${book.locationLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (book.read) ReadBadge()
        }
    }
}
