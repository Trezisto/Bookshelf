package com.prijilevschi.library.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/** An error to show the user; [status] is the HTTP status, or null when the server could not be reached. */
class ApiException(message: String, val status: Int? = null, cause: Throwable? = null) : Exception(message, cause)

class LibraryRepository(
    private val settingsRepository: SettingsRepository,
    val httpClient: OkHttpClient,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Volatile
    private var cached: Pair<String, LibraryApi>? = null

    /** Bumped after a cover changes so image URLs change and Coil reloads them. */
    private val _coverVersions = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val coverVersions: StateFlow<Map<Long, Long>> = _coverVersions.asStateFlow()

    private suspend fun api(): LibraryApi {
        val url = settingsRepository.current().serverUrl
        cached?.let { (cachedUrl, api) -> if (cachedUrl == url) return api }
        val api = Retrofit.Builder()
            .baseUrl(url)
            .client(httpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(LibraryApi::class.java)
        cached = url to api
        return api
    }

    suspend fun coverUrl(book: Book): String? {
        if (!book.hasCover) return null
        val version = coverVersions.value[book.id] ?: 0
        return "${settingsRepository.current().serverUrl}api/books/${book.id}/cover?v=$version"
    }

    suspend fun books(query: String? = null, read: Boolean? = null, language: String? = null) =
        call { api().books(query = query?.takeIf { it.isNotBlank() }, read = read, language = language) }

    suspend fun book(id: Long) = call { api().book(id) }

    suspend fun languages() = call { api().languages() }

    suspend fun authors() = call { api().authors() }

    suspend fun shelves() = call { api().shelves() }

    suspend fun saveBook(id: Long?, request: BookRequest): Book = call {
        val s = settingsRepository.current()
        val key = s.llmApiKey.ifBlank { null }
        val baseUrl = if (key == null) null else s.llmBaseUrl.ifBlank { null }
        val model = if (key == null) null else s.llmModel.ifBlank { null }
        if (id == null) api().createBook(request, key, baseUrl, model)
        else api().updateBook(id, request, key, baseUrl, model)
    }

    suspend fun deleteBook(id: Long) = call { api().deleteBook(id) }

    suspend fun setRead(id: Long, read: Boolean, dateRead: String?) =
        call { api().setRead(id, ReadRequest(read, dateRead)) }

    suspend fun uploadCover(id: Long, jpeg: ByteArray) {
        call {
            val part = MultipartBody.Part.createFormData(
                "file", "cover.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()),
            )
            api().uploadCover(id, part)
        }
        bumpCover(id)
    }

    suspend fun deleteCover(id: Long) {
        call { api().deleteCover(id) }
        bumpCover(id)
    }

    suspend fun summary(request: SummaryRequest, settings: AppSettings? = null): String = call {
        val s = settings ?: settingsRepository.current()
        api().summary(
            request,
            s.llmApiKey.ifBlank { null },
            s.llmBaseUrl.ifBlank { null },
            s.llmModel.ifBlank { null },
        ).summary
    }

    suspend fun lookup(isbn: String?, title: String?, author: String?): BookLookup =
        call { api().lookup(isbn = isbn, title = title, author = author) }

    /** Downloads a cover found by a lookup; null when it cannot be fetched. */
    suspend fun downloadImage(url: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            httpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (response.isSuccessful) response.body.bytes() else null
            }
        }.getOrNull()
    }

    suspend fun saveShelf(id: Long?, request: ShelfRequest): Shelf = call {
        if (id == null) api().createShelf(request) else api().updateShelf(id, request)
    }

    suspend fun deleteShelf(id: Long) = call { api().deleteShelf(id) }

    suspend fun nextPosition(shelfId: Long, depthRow: Int) = call { api().nextPosition(shelfId, depthRow).positionNumber }

    private fun bumpCover(id: Long) {
        _coverVersions.update { it + (id to System.currentTimeMillis()) }
    }

    private suspend fun <T> call(block: suspend () -> T): T = try {
        block()
    } catch (e: HttpException) {
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        val detail = body?.let { runCatching { json.decodeFromString<Problem>(it).detail }.getOrNull() }
        throw ApiException(detail ?: "Server error (${e.code()})", e.code(), e)
    } catch (e: IOException) {
        val url = settingsRepository.current().serverUrl
        throw ApiException("Can't reach the library server at $url. Check the address in Settings.", null, e)
    } catch (e: IllegalArgumentException) {
        throw ApiException("Invalid server address. Check it in Settings.", null, e)
    }

    companion object {
        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            // summaries can take a while on free LLM tiers
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
