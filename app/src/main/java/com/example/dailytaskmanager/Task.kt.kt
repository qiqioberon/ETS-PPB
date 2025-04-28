package com.example.dailytaskmanager // Ganti dengan package name Anda

import java.time.LocalDateTime
import java.util.UUID // Untuk ID unik

data class Task(
    val id: UUID = UUID.randomUUID(), // ID unik untuk setiap task
    val title: String,
    val deadline: LocalDateTime?, // Bisa null jika tidak ada deadline
    var isCompleted: Boolean = false
)