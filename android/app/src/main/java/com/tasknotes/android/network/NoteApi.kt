package com.tasknotes.android.network

import com.tasknotes.android.model.Note
import com.tasknotes.android.model.NoteRequest
import retrofit2.http.*

interface NoteApi {

    @GET("api/notes/{id}")
    suspend fun getById(@Path("id") id: Long): Note

    @GET("api/categories/{categoryId}/notes")
    suspend fun getByCategory(@Path("categoryId") categoryId: Long): List<Note>

    @POST("api/categories/{categoryId}/notes")
    suspend fun create(@Path("categoryId") categoryId: Long, @Body request: NoteRequest): Note

    @PUT("api/notes/{id}")
    suspend fun update(@Path("id") id: Long, @Body request: NoteRequest): Note

    @DELETE("api/notes/{id}")
    suspend fun delete(@Path("id") id: Long)
}
