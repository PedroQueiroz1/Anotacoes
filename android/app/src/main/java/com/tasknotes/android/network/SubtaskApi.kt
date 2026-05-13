package com.tasknotes.android.network

import com.tasknotes.android.model.Subtask
import com.tasknotes.android.model.SubtaskRequest
import retrofit2.http.*

interface SubtaskApi {

    @GET("api/tasks/{taskId}/subtasks")
    suspend fun getByTask(@Path("taskId") taskId: Long): List<Subtask>

    @POST("api/tasks/{taskId}/subtasks")
    suspend fun create(@Path("taskId") taskId: Long, @Body request: SubtaskRequest): Subtask

    @PATCH("api/subtasks/{id}/toggle")
    suspend fun toggle(@Path("id") id: Long): Subtask

    @DELETE("api/subtasks/{id}")
    suspend fun delete(@Path("id") id: Long)
}
