package com.tasknotes.android.repository

import com.tasknotes.android.model.Subtask
import com.tasknotes.android.model.SubtaskRequest
import com.tasknotes.android.network.ApiClient
import com.tasknotes.android.network.SubtaskApi

class SubtaskRepository {
    private val api = ApiClient.create<SubtaskApi>()

    suspend fun getByTask(taskId: Long): Result<List<Subtask>> =
        runCatching { api.getByTask(taskId) }

    suspend fun create(taskId: Long, text: String): Result<Subtask> =
        runCatching { api.create(taskId, SubtaskRequest(text)) }

    suspend fun toggle(id: Long): Result<Subtask> =
        runCatching { api.toggle(id) }

    suspend fun delete(id: Long): Result<Unit> =
        runCatching { api.delete(id) }
}
