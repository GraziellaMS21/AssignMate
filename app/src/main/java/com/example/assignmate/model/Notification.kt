package com.example.assignmate.model

data class Notification(
    val id: Long,
    val userId: Int,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false
)
