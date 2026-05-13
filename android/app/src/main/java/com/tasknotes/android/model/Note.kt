package com.tasknotes.android.model

data class Note(
    val id: Long,
    val categoryId: Long,
    val title: String,
    val content: String?,
    val createdAt: String,
    val updatedAt: String
)

data class NoteRequest(
    val title: String,
    val content: String?
)
