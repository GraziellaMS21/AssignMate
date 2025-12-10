package com.example.assignmate.model

data class Comment(
    val id: String = "",          // Changed from Long to String
    val userId: String = "",      // Changed from Int to String
    val username: String = "",
    val commentText: String = "", // Renamed to clearly identify the content
    val timestamp: Long = 0L
)