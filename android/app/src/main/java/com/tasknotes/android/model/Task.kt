package com.tasknotes.android.model

data class Task(
    val id: Long,
    val categoryId: Long,
    val title: String,
    val description: String?,
    val dueDate: String?,
    val priority: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)

data class TaskRequest(
    val title: String,
    val description: String?,
    val dueDate: String?,
    val priority: String
)

data class StatusUpdateRequest(val status: String)
