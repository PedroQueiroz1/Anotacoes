package com.tasknotes.android.network

import com.tasknotes.android.model.StatusUpdateRequest
import com.tasknotes.android.model.Task
import com.tasknotes.android.model.TaskRequest
import retrofit2.http.*

interface TaskApi {

    @GET("api/categories/{categoryId}/tasks")
    suspend fun getByCategory(@Path("categoryId") categoryId: Long): List<Task>

    @POST("api/categories/{categoryId}/tasks")
    suspend fun create(@Path("categoryId") categoryId: Long, @Body request: TaskRequest): Task

    @PUT("api/tasks/{id}")
    suspend fun update(@Path("id") id: Long, @Body request: TaskRequest): Task

    @PATCH("api/tasks/{id}/status")
    suspend fun updateStatus(@Path("id") id: Long, @Body request: StatusUpdateRequest): Task

    @PUT("api/categories/{categoryId}/tasks/reorder")
    suspend fun reorder(@Path("categoryId") categoryId: Long, @Body ids: List<Long>)

    @DELETE("api/tasks/{id}")
    suspend fun delete(@Path("id") id: Long)
}
