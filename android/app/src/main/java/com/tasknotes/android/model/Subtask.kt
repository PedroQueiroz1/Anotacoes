package com.tasknotes.android.model

data class Subtask(
    val id: Long,
    val taskId: Long,
    val text: String,
    val done: Boolean,
    val createdAt: String
)

data class SubtaskRequest(val text: String)
