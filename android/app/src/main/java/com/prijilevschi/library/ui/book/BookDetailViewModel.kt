package com.prijilevschi.library.ui.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prijilevschi.library.data.ApiException
import com.prijilevschi.library.data.Book
import com.prijilevschi.library.data.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookDetailState(
    val loading: Boolean = true,
    val book: Book? = null,
    val coverUrl: String? = null,
    val error: String? = null,
    val deleted: Boolean = false,
)

class BookDetailViewModel(private val bookId: Long, private val repository: LibraryRepository) : ViewModel() {
    private val _state = MutableStateFlow(BookDetailState())
    val state: StateFlow<BookDetailState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            try {
                show(repository.book(bookId))
            } catch (e: ApiException) {
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    /** [date] is an ISO date; null = today (server default). */
    fun setRead(read: Boolean, date: String? = null) {
        viewModelScope.launch {
            try {
                show(repository.setRead(bookId, read, date))
            } catch (e: ApiException) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            try {
                repository.deleteBook(bookId)
                _state.update { it.copy(deleted = true) }
            } catch (e: ApiException) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    private suspend fun show(book: Book) {
        _state.update { it.copy(loading = false, book = book, coverUrl = repository.coverUrl(book), error = null) }
    }
}
