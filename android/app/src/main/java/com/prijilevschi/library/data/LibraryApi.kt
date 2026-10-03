package com.prijilevschi.library.data

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

private const val KEY = "X-LLM-Api-Key"
private const val BASE_URL = "X-LLM-Base-Url"
private const val MODEL = "X-LLM-Model"

/** Retrofit mirror of the backend's /api endpoints. Null headers are not sent. */
interface LibraryApi {
    @GET("api/books")
    suspend fun books(
        @Query("q") query: String? = null,
        @Query("shelfId") shelfId: Long? = null,
        @Query("read") read: Boolean? = null,
        @Query("language") language: String? = null,
    ): List<Book>

    @GET("api/books/languages")
    suspend fun languages(): List<String>

    @GET("api/books/{id}")
    suspend fun book(@Path("id") id: Long): Book

    @POST("api/books")
    suspend fun createBook(
        @Body request: BookRequest,
        @Header(KEY) apiKey: String?,
        @Header(BASE_URL) baseUrl: String?,
        @Header(MODEL) model: String?,
    ): Book

    @PUT("api/books/{id}")
    suspend fun updateBook(
        @Path("id") id: Long,
        @Body request: BookRequest,
        @Header(KEY) apiKey: String?,
        @Header(BASE_URL) baseUrl: String?,
        @Header(MODEL) model: String?,
    ): Book

    @DELETE("api/books/{id}")
    suspend fun deleteBook(@Path("id") id: Long)

    @PATCH("api/books/{id}/read")
    suspend fun setRead(@Path("id") id: Long, @Body request: ReadRequest): Book

    @Multipart
    @PUT("api/books/{id}/cover")
    suspend fun uploadCover(@Path("id") id: Long, @Part file: MultipartBody.Part)

    @DELETE("api/books/{id}/cover")
    suspend fun deleteCover(@Path("id") id: Long)

    /** Either [isbn], or [title] with an optional [author]. 404 when nothing is found. */
    @GET("api/lookup")
    suspend fun lookup(
        @Query("isbn") isbn: String? = null,
        @Query("title") title: String? = null,
        @Query("author") author: String? = null,
    ): BookLookup

    @POST("api/ai/summary")
    suspend fun summary(
        @Body request: SummaryRequest,
        @Header(KEY) apiKey: String?,
        @Header(BASE_URL) baseUrl: String?,
        @Header(MODEL) model: String?,
    ): SummaryResponse

    @GET("api/shelves")
    suspend fun shelves(): List<Shelf>

    @POST("api/shelves")
    suspend fun createShelf(@Body request: ShelfRequest): Shelf

    @PUT("api/shelves/{id}")
    suspend fun updateShelf(@Path("id") id: Long, @Body request: ShelfRequest): Shelf

    @DELETE("api/shelves/{id}")
    suspend fun deleteShelf(@Path("id") id: Long)

    @GET("api/shelves/{id}/next-position")
    suspend fun nextPosition(@Path("id") id: Long, @Query("depthRow") depthRow: Int): Position

    @GET("api/authors")
    suspend fun authors(@Query("q") query: String? = null): List<Author>
}
