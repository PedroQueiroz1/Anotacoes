package com.tasknotes.android.model

data class Category(
    val id: Long,
    val name: String,
    val createdAt: String,
    val updatedAt: String,
    val pendingTaskCount: Int
)

data class CategoryRequest(val name: String)
