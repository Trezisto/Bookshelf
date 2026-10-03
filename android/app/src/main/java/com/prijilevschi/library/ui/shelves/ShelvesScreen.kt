package com.prijilevschi.library.ui.shelves

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prijilevschi.library.R
import com.prijilevschi.library.data.Shelf
import com.prijilevschi.library.data.ShelfOrientation
import com.prijilevschi.library.data.ShelfRequest
import com.prijilevschi.library.ui.appViewModel
import com.prijilevschi.library.ui.components.AppIcon
import com.prijilevschi.library.ui.components.BackButton
import com.prijilevschi.library.ui.components.Loading
import com.prijilevschi.library.ui.components.SuggestField

private sealed interface ShelfDialog {
    data object New : ShelfDialog
    data class Edit(val shelf: Shelf) : ShelfDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelvesScreen(onBack: () -> Unit) {
    val vm = appViewModel { ShelvesViewModel(it.libraryRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var dialog by remember { mutableStateOf<ShelfDialog?>(null) }
    var deleting by remember { mutableStateOf<Shelf?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); vm.messageShown() }
    }
    LaunchedEffect(state.dialogDone) { dialog = null }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Shelves") }, navigationIcon = { BackButton(onBack) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.clearDialogError(); dialog = ShelfDialog.New }) { AppIcon(R.drawable.ic_add, "Add shelf") }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (state.loading) {
            Loading(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.shelves.isEmpty()) {
                item {
                    Text(
                        "Add a shelf for every row of your bookcases, e.g. \"Living room\" row 1, 2, 3…",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            items(state.shelves, key = { it.id }) { shelf ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(shelf.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${if (shelf.orientation == ShelfOrientation.VERTICAL) "Standing" else "Lying flat"} · " +
                                    "${state.bookCounts[shelf.id] ?: 0} books",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        IconButton(onClick = { vm.clearDialogError(); dialog = ShelfDialog.Edit(shelf) }) {
                            AppIcon(R.drawable.ic_edit, "Edit")
                        }
                        IconButton(onClick = { deleting = shelf }) { AppIcon(R.drawable.ic_delete, "Delete") }
                    }
                }
            }
        }
    }

    dialog?.let { current ->
        ShelfEditDialog(
            shelf = (current as? ShelfDialog.Edit)?.shelf,
            locations = state.shelves.map { it.location }.distinct(),
            error = state.dialogError,
            onDismiss = { dialog = null },
            onSave = { request -> vm.save((current as? ShelfDialog.Edit)?.shelf?.id, request) },
        )
    }
    deleting?.let { shelf ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${shelf.label}?") },
            text = { Text("Its books stay in your library as \"Unshelved\".") },
            confirmButton = { TextButton(onClick = { deleting = null; vm.delete(shelf) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ShelfEditDialog(
    shelf: Shelf?,
    locations: List<String>,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (ShelfRequest) -> Unit,
) {
    var location by remember { mutableStateOf(shelf?.location.orEmpty()) }
    var row by remember { mutableStateOf(shelf?.rowNum?.toString() ?: "1") }
    var orientation by remember { mutableStateOf(shelf?.orientation ?: ShelfOrientation.VERTICAL) }
    val rowNum = row.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (shelf == null) "New shelf" else "Edit shelf") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SuggestField(value = location, onValueChange = { location = it }, label = "Location (room, bookcase…)", options = locations)
                OutlinedTextField(
                    value = row,
                    onValueChange = { row = it.filter(Char::isDigit).take(3) },
                    label = { Text("Row number (1 = top)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                Text("Books are", style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val options = listOf(ShelfOrientation.VERTICAL to "Standing", ShelfOrientation.HORIZONTAL to "Lying flat")
                    options.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = orientation == value,
                            onClick = { orientation = value },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        ) { Text(label) }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(ShelfRequest(location.trim(), rowNum ?: 1, orientation)) },
                enabled = location.isNotBlank() && rowNum != null && rowNum >= 1,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
