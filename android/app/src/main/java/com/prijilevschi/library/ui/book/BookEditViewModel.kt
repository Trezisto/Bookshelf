package com.prijilevschi.library.ui.book

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prijilevschi.library.data.ApiException
import com.prijilevschi.library.data.Book
import com.prijilevschi.library.data.BookRequest
import com.prijilevschi.library.data.LibraryRepository
import com.prijilevschi.library.data.SettingsRepository
import com.prijilevschi.library.data.Shelf
import com.prijilevschi.library.data.SummaryRequest
import com.prijilevschi.library.util.Isbn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookForm(
    val name: String = "",
    val authorName: String = "",
    val isbn: String = "",
    val genre: String = "",
    val language: String = "",
    val year: String = "",
    val pages: String = "",
    val read: Boolean = false,
    val dateRead: String? = null,
    val shelfId: Long? = null,
    val depthRow: Int = 1,
    val position: String = "",
    val description: String = "",
)

data class BookEditState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val form: BookForm = BookForm(),
    val shelves: List<Shelf> = emptyList(),
    val authors: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val hasLlmKey: Boolean = false,
    /** Current cover on the server. */
    val coverUrl: String? = null,
    /** Newly taken/picked cover, shown as preview and uploaded on save. */
    val newCoverUri: Uri? = null,
    val newCoverJpeg: ByteArray? = null,
    val removeCover: Boolean = false,
    val generating: Boolean = false,
    val saving: Boolean = false,
    val nameError: String? = null,
    val authorError: String? = null,
    val isbnError: String? = null,
    val positionError: String? = null,
    val message: String? = null,
    val done: Boolean = false,
) {
    val selectedShelf: Shelf? get() = shelves.firstOrNull { it.id == form.shelfId }
}

class BookEditViewModel(
    private val bookId: Long?,
    private val repository: LibraryRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(BookEditState(isNew = bookId == null))
    val state: StateFlow<BookEditState> = _state.asStateFlow()

    private var original: Book? = null

    init {
        viewModelScope.launch {
            try {
                val shelves = repository.shelves()
                val authors = repository.authors().map { it.name }
                val languages = repository.languages()
                val book = bookId?.let { repository.book(it) }
                original = book
                _state.update {
                    it.copy(
                        loading = false,
                        shelves = shelves,
                        authors = authors,
                        languages = languages,
                        hasLlmKey = settings.current().hasLlmKey,
                        form = book?.toForm() ?: it.form,
                        coverUrl = book?.let { b -> repository.coverUrl(b) },
                    )
                }
            } catch (e: ApiException) {
                _state.update { it.copy(loading = false, message = e.message) }
            }
        }
    }

    fun refreshSettings() {
        viewModelScope.launch { _state.update { it.copy(hasLlmKey = settings.current().hasLlmKey) } }
    }

    fun update(transform: (BookForm) -> BookForm) {
        _state.update { state ->
            val form = transform(state.form)
            state.copy(
                form = form,
                nameError = if (form.name != state.form.name) null else state.nameError,
                authorError = if (form.authorName != state.form.authorName) null else state.authorError,
                isbnError = if (form.isbn != state.form.isbn) null else state.isbnError,
                positionError = if (form.position != state.form.position || form.shelfId != state.form.shelfId ||
                    form.depthRow != state.form.depthRow
                ) null else state.positionError,
            )
        }
    }

    /** Choosing a shelf or depth row suggests the first free slot (keeps the book's own slot when editing). */
    fun selectSlot(shelfId: Long?, depthRow: Int) {
        update { it.copy(shelfId = shelfId, depthRow = depthRow.coerceAtLeast(1)) }
        if (shelfId == null) {
            update { it.copy(position = "") }
            return
        }
        val book = original
        if (book != null && book.shelf?.id == shelfId && book.depthRow == depthRow && book.positionNumber != null) {
            update { it.copy(position = book.positionNumber.toString()) }
            return
        }
        viewModelScope.launch {
            try {
                val next = repository.nextPosition(shelfId, depthRow.coerceAtLeast(1))
                update { it.copy(position = next.toString()) }
            } catch (e: ApiException) {
                _state.update { it.copy(message = e.message) }
            }
        }
    }

    fun onCoverPicked(uri: Uri, jpeg: ByteArray) {
        _state.update { it.copy(newCoverUri = uri, newCoverJpeg = jpeg, removeCover = false) }
    }

    fun removeCover() {
        _state.update { it.copy(newCoverUri = null, newCoverJpeg = null, removeCover = it.coverUrl != null) }
    }

    fun generateSummary() {
        val form = _state.value.form
        if (form.name.isBlank()) {
            _state.update { it.copy(nameError = "Enter the title first") }
            return
        }
        _state.update { it.copy(generating = true) }
        viewModelScope.launch {
            try {
                val summary = repository.summary(
                    SummaryRequest(
                        title = form.name.trim(),
                        author = form.authorName.trim().ifBlank { null },
                        isbn = form.isbn.trim().ifBlank { null },
                        language = form.language.trim().ifBlank { null },
                    ),
                )
                _state.update { it.copy(generating = false, form = it.form.copy(description = summary)) }
            } catch (e: ApiException) {
                _state.update { it.copy(generating = false, message = e.message) }
            }
        }
    }

    fun save() {
        val s = _state.value
        val form = s.form
        var valid = true
        if (form.name.isBlank()) {
            _state.update { it.copy(nameError = "Title is required") }; valid = false
        }
        if (form.authorName.isBlank()) {
            _state.update { it.copy(authorError = "Author is required") }; valid = false
        }
        if (form.isbn.isNotBlank() && !Isbn.isValid(form.isbn)) {
            _state.update { it.copy(isbnError = "Not a valid ISBN-10 or ISBN-13") }; valid = false
        }
        val position = form.position.trim().toIntOrNull()
        if (form.shelfId != null && (position == null || position < 1)) {
            _state.update { it.copy(positionError = "Enter the position on the shelf (1 = leftmost)") }; valid = false
        }
        if (!valid) return

        val request = BookRequest(
            name = form.name.trim(),
            authorName = form.authorName.trim(),
            isbn = form.isbn.trim().ifBlank { null },
            description = form.description.trim().ifBlank { null },
            genre = form.genre.trim().ifBlank { null },
            language = form.language.trim().ifBlank { null },
            year = form.year.trim().toIntOrNull(),
            pages = form.pages.trim().toIntOrNull()?.takeIf { it > 0 },
            read = form.read,
            dateRead = if (form.read) form.dateRead else null,
            shelfId = form.shelfId,
            positionNumber = if (form.shelfId != null) position else null,
            depthRow = form.depthRow,
        )
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val saved = repository.saveBook(bookId, request)
                val jpeg = s.newCoverJpeg
                when {
                    jpeg != null -> repository.uploadCover(saved.id, jpeg)
                    s.removeCover -> repository.deleteCover(saved.id)
                }
                _state.update { it.copy(saving = false, done = true) }
            } catch (e: ApiException) {
                val message = e.message.orEmpty()
                _state.update {
                    when {
                        e.status == 409 && message.contains("Position", ignoreCase = true) ->
                            it.copy(saving = false, positionError = message)
                        message.contains("ISBN", ignoreCase = true) && (e.status == 409 || e.status == 400) ->
                            it.copy(saving = false, isbnError = message)
                        else -> it.copy(saving = false, message = message)
                    }
                }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }

    private fun Book.toForm() = BookForm(
        name = name,
        authorName = author.name,
        isbn = isbn.orEmpty(),
        genre = genre.orEmpty(),
        language = language.orEmpty(),
        year = year?.toString().orEmpty(),
        pages = pages?.toString().orEmpty(),
        read = read,
        dateRead = dateRead,
        shelfId = shelf?.id,
        depthRow = depthRow,
        position = positionNumber?.toString().orEmpty(),
        description = description.orEmpty(),
    )
}
