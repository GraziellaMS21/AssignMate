package com.example.assignmate.model

data class Group(
    val id: Long,
    val name: String,
    val description: String,
    val code: String,
    val leader: String,
    val members: List<String>,
    val lastUpdated: Long,
    val progress: Int,
    val pendingTaskCount: Int,
    val assignedTasksCount: Int,
    val isFavourite: Boolean = false
)
