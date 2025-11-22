package com.example.assignmate.model

data class Group(
    val id: String,
    val name: String,
    val description: String,
    val leader: String,
    val members: List<String>,
    val lastUpdated: Long,
    val progress: Int
)
