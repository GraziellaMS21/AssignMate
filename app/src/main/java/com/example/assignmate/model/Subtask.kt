package com.example.assignmate.model

data class Subtask(
    val id: Long,
    val taskId: Long,
    val name: String,
    var isCompleted: Boolean
)
