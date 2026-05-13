package com.tasknotes.android.repository

import com.tasknotes.android.model.Category
import com.tasknotes.android.model.CategoryRequest
import com.tasknotes.android.network.ApiClient
import com.tasknotes.android.network.CategoryApi

class CategoryRepository {
    private val api = ApiClient.create<CategoryApi>()

    suspend fun getAll(): Result<List<Category>> =
        runCatching { api.getAll() }

    suspend fun create(name: String): Result<Category> =
        runCatching { api.create(CategoryRequest(name.trim())) }

    suspend fun update(id: Long, name: String): Result<Category> =
        runCatching { api.update(id, CategoryRequest(name.trim())) }

    suspend fun delete(id: Long): Result<Unit> =
        runCatching { api.delete(id) }
}
