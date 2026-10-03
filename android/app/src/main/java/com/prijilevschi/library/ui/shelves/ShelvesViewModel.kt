package com.prijilevschi.library.ui.shelves

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prijilevschi.library.data.ApiException
import com.prijilevschi.library.data.LibraryRepository
import com.prijilevschi.library.data.Shelf
import com.prijilevschi.library.data.ShelfRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShelvesState(
    val loading: Boolean = true,
    val shelves: List<Shelf> = emptyList(),
    val bookCounts: Map<Long, Int> = emptyMap(),
    val message: String? = null,
    /** Error for the open add/edit dialog. */
    val dialogError: String? = null,
    val dialogDone: Int = 0,
)

class ShelvesViewModel(private val repository: LibraryRepository) : ViewModel() {
    private val _state = MutableStateFlow(ShelvesState())
    val state: StateFlow<ShelvesState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                val shelves = repository.shelves()
                val counts = repository.books().mapNotNull { it.shelf?.id }.groupingBy { it }.eachCount()
                _state.update { it.copy(loading = false, shelves = shelves, bookCounts = counts) }
            } catch (e: ApiException) {
                _state.update { it.copy(loading = false, message = e.message) }
            }
        }
    }

    fun save(id: Long?, request: ShelfRequest) {
        viewModelScope.launch {
            try {
                repository.saveShelf(id, request)
                _state.update { it.copy(dialogError = null, dialogDone = it.dialogDone + 1) }
                load()
            } catch (e: ApiException) {
                _state.update { it.copy(dialogError = e.message) }
            }
        }
    }

    fun delete(shelf: Shelf) {
        viewModelScope.launch {
            try {
                repository.deleteShelf(shelf.id)
                load()
            } catch (e: ApiException) {
                _state.update { it.copy(message = e.message) }
            }
        }
    }

    fun clearDialogError() = _state.update { it.copy(dialogError = null) }

    fun messageShown() = _state.update { it.copy(message = null) }
}
