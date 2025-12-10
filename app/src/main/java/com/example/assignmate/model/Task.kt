package com.example.assignmate.model

data class Task(
    val id: Long,
    val name: String,
    val description: String,
    val groupId: Long,
    val groupName: String? = null,
    val dueDate: Long,
    val status: String,
    val assignedTo: List<Int>? = null
)
