package com.example.assignmate.model

data class Task(
    val taskId: String = "",
    val name: String = "",
    val description: String = "",
    val groupId: String = "",
    val groupName: String = "Group",
    val dueDate: String = "", // Firebase stores dates as Strings (dd/MM/yyyy)
    val status: String = "Not Started",
    val assignedToId: String = "",
    val assignedToName: String = "Unassigned"
)