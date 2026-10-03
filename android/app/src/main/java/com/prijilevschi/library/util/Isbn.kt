package com.prijilevschi.library.util

/** Same rules as the backend: ISBN-10 or ISBN-13 with a valid checksum; hyphens and spaces are ignored. */
object Isbn {
    fun normalize(raw: String): String = raw.replace(Regex("[\\s-]"), "").uppercase()

    fun isValid(raw: String): Boolean {
        val isbn = normalize(raw)
        return when {
            isbn.matches(Regex("\\d{9}[\\dX]")) ->
                isbn.withIndex().sumOf { (i, c) -> (if (c == 'X') 10 else c - '0') * (10 - i) } % 11 == 0
            isbn.matches(Regex("\\d{13}")) ->
                isbn.withIndex().sumOf { (i, c) -> (c - '0') * (if (i % 2 == 0) 1 else 3) } % 10 == 0
            else -> false
        }
    }
}
