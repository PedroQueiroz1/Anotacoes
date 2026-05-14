package com.tasknotes.android.repository

import com.tasknotes.android.model.Note
import com.tasknotes.android.model.NoteRequest
import com.tasknotes.android.network.ApiClient
import com.tasknotes.android.network.NoteApi

class NoteRepository {
    private val api = ApiClient.create<NoteApi>()

    suspend fun getByCategory(categoryId: Long): Result<List<Note>> =
        runCatching { api.getByCategory(categoryId) }

    suspend fun create(categoryId: Long, title: String, content: String?): Result<Note> =
        runCatching { api.create(categoryId, NoteRequest(title, content)) }

    suspend fun update(id: Long, title: String, content: String?): Result<Note> =
        runCatching { api.update(id, NoteRequest(title, content)) }

    suspend fun reorder(categoryId: Long, ids: List<Long>): Result<Unit> =
        runCatching { api.reorder(categoryId, ids) }

    suspend fun delete(id: Long): Result<Unit> =
        runCatching { api.delete(id) }
}
