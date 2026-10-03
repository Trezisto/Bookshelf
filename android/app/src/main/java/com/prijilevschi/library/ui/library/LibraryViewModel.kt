package com.prijilevschi.library.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prijilevschi.library.data.ApiException
import com.prijilevschi.library.data.Book
import com.prijilevschi.library.data.LibraryRepository
import com.prijilevschi.library.data.Shelf
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ReadFilter(val label: String, val value: Boolean?) {
    ALL("All", null), UNREAD("Unread", false), READ("Read", true)
}

data class LibraryState(
    val loading: Boolean = true,
    val error: String? = null,
    val shelves: List<Shelf> = emptyList(),
    val books: List<Book> = emptyList(),
    val languages: List<String> = emptyList(),
    val query: String = "",
    val readFilter: ReadFilter = ReadFilter.ALL,
    val language: String? = null,
    /** Server-side search result; null when no search or filter is active. */
    val matches: List<Book>? = null,
) {
    val filtering: Boolean get() = query.isNotBlank() || readFilter != ReadFilter.ALL || language != null
    val matchIds: Set<Long>? get() = matches?.mapTo(HashSet()) { it.id }
}

class LibraryViewModel(private val repository: LibraryRepository) : ViewModel() {
    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun refresh() {
        viewModelScope.launch {
            try {
                val shelves = repository.shelves()
                val books = repository.books()
                val languages = repository.languages()
                _state.update { it.copy(loading = false, error = null, shelves = shelves, books = books, languages = languages) }
                search(debounce = false)
            } catch (e: ApiException) {
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        search(debounce = true)
    }

    fun onReadFilter(filter: ReadFilter) {
        _state.update { it.copy(readFilter = filter) }
        search(debounce = false)
    }

    fun onLanguage(language: String?) {
        _state.update { it.copy(language = language) }
        search(debounce = false)
    }

    private fun search(debounce: Boolean) {
        searchJob?.cancel()
        val s = _state.value
        if (!s.filtering) {
            _state.update { it.copy(matches = null) }
            return
        }
        searchJob = viewModelScope.launch {
            if (debounce) delay(300)
            try {
                val matches = repository.books(s.query.trim(), s.readFilter.value, s.language)
                _state.update { it.copy(matches = matches, error = null) }
            } catch (e: ApiException) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }
}
