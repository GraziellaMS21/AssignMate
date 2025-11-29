package com.example.assignmate.model

data class Comment(
    val id: Long,
    val taskId: Long,
    val userId: Int,
    val username: String,
    val commentText: String,
    val timestamp: Long
)
