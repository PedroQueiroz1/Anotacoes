package com.tasknotes.android.network

import com.tasknotes.android.model.Category
import com.tasknotes.android.model.CategoryRequest
import retrofit2.http.*

interface CategoryApi {

    @GET("api/categories")
    suspend fun getAll(): List<Category>

    @POST("api/categories")
    suspend fun create(@Body request: CategoryRequest): Category

    @PUT("api/categories/{id}")
    suspend fun update(@Path("id") id: Long, @Body request: CategoryRequest): Category

    @PUT("api/categories/reorder")
    suspend fun reorder(@Body ids: List<Long>)

    @DELETE("api/categories/{id}")
    suspend fun delete(@Path("id") id: Long)
}
