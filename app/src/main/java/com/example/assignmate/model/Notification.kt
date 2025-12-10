package com.example.assignmate.model

data class Notification(
    val id: String = "",        // Changed from Long to String for Firebase
    val userId: String = "",    // Changed from Int to String for Firebase UIDs
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)