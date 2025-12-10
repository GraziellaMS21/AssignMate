package com.example.assignmate.model

data class Group(
    val groupId: String = "",
    val name: String = "",
    val description: String = "",
    val createdBy: String = "",
    val members: HashMap<String, Any> = HashMap(),
    val anouncements: String = ""
)