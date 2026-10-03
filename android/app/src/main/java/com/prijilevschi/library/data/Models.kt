package com.prijilevschi.library.data

import kotlinx.serialization.Serializable

@Serializable
enum class ShelfOrientation { HORIZONTAL, VERTICAL }

@Serializable
data class Author(val id: Long, val name: String)

@Serializable
data class Shelf(
    val id: Long,
    val location: String,
    val rowNum: Int,
    val orientation: ShelfOrientation,
) {
    val label: String get() = "$location · Row $rowNum"
}

@Serializable
data class Book(
    val id: Long,
    val name: String,
    val isbn: String? = null,
    val description: String? = null,
    val genre: String? = null,
    val language: String? = null,
    val publisher: String? = null,
    val url: String? = null,
    /** Own rating, 0-5. */
    val rating: Double? = null,
    /** ISO date, e.g. 2005-08-01; the app only shows the year. */
    val publicationDate: String? = null,
    val pages: Int? = null,
    val read: Boolean = false,
    /** ISO date, e.g. 2026-10-03 */
    val dateRead: String? = null,
    val author: Author,
    val coAuthors: List<String> = emptyList(),
    val shelf: Shelf? = null,
    val positionNumber: Int? = null,
    val depthRow: Int = 1,
    val hasCover: Boolean = false,
    /** ISO instants, e.g. 2026-10-03T12:00:00Z */
    val createdAt: String? = null,
    val modifiedAt: String? = null,
) {
    val year: Int? get() = publicationDate?.take(4)?.toIntOrNull()

    /** "Living room · Row 3 · Position 7 · Back row (2)" */
    val locationLabel: String
        get() = if (shelf == null) "Not on a shelf" else buildString {
            append(shelf.label)
            positionNumber?.let { append(" · Position ").append(it) }
            append(if (depthRow == 1) " · Front row" else " · Back row ($depthRow)")
        }
}

@Serializable
data class BookRequest(
    val name: String,
    val authorName: String,
    val coAuthors: List<String> = emptyList(),
    val isbn: String? = null,
    val description: String? = null,
    val genre: String? = null,
    val language: String? = null,
    val publisher: String? = null,
    val url: String? = null,
    val rating: Double? = null,
    val publicationDate: String? = null,
    val pages: Int? = null,
    val read: Boolean = false,
    val dateRead: String? = null,
    val shelfId: Long? = null,
    val positionNumber: Int? = null,
    val depthRow: Int? = null,
)

/** What the web knows about a book; every field may be missing. */
@Serializable
data class BookLookup(
    val isbn: String? = null,
    val title: String? = null,
    val author: String? = null,
    val coAuthors: List<String> = emptyList(),
    val publisher: String? = null,
    val publicationDate: String? = null,
    val language: String? = null,
    val pages: Int? = null,
    val genre: String? = null,
    val description: String? = null,
    val coverUrl: String? = null,
    val url: String? = null,
)

@Serializable
data class ShelfRequest(val location: String, val rowNum: Int, val orientation: ShelfOrientation)

@Serializable
data class ReadRequest(val read: Boolean, val dateRead: String? = null)

@Serializable
data class SummaryRequest(
    val title: String,
    val author: String? = null,
    val isbn: String? = null,
    val language: String? = null,
)

@Serializable
data class SummaryResponse(val summary: String)

@Serializable
data class Position(val shelfId: Long, val depthRow: Int, val positionNumber: Int)

/** Spring's RFC 9457 error body. */
@Serializable
data class Problem(val status: Int? = null, val title: String? = null, val detail: String? = null)
