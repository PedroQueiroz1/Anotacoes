package com.tasknotes.android.repository

import com.tasknotes.android.model.StatusUpdateRequest
import com.tasknotes.android.model.Task
import com.tasknotes.android.model.TaskRequest
import com.tasknotes.android.network.ApiClient
import com.tasknotes.android.network.TaskApi

class TaskRepository {
    private val api = ApiClient.create<TaskApi>()

    suspend fun getByCategory(categoryId: Long): Result<List<Task>> =
        runCatching { api.getByCategory(categoryId) }

    suspend fun create(categoryId: Long, request: TaskRequest): Result<Task> =
        runCatching { api.create(categoryId, request) }

    suspend fun update(id: Long, request: TaskRequest): Result<Task> =
        runCatching { api.update(id, request) }

    suspend fun updateStatus(id: Long, status: String): Result<Task> =
        runCatching { api.updateStatus(id, StatusUpdateRequest(status)) }

    suspend fun reorder(categoryId: Long, ids: List<Long>): Result<Unit> =
        runCatching { api.reorder(categoryId, ids) }

    suspend fun delete(id: Long): Result<Unit> =
        runCatching { api.delete(id) }
}
