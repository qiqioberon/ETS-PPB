package com.example.dailytaskmanager // Ganti dengan package name Anda

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import java.time.LocalDateTime
import java.util.UUID
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf

// Enum untuk tipe penyortiran
enum class SortType {
    NONE, DEADLINE, STATUS
}

class TaskViewModel : ViewModel() {

    // Daftar tugas yang akan diobservasi oleh Compose
    // Menggunakan mutableStateListOf agar perubahan pada list (add/remove) memicu recomposition
    val tasks = mutableStateListOf<Task>()

    // State untuk mengontrol visibilitas dialog tambah tugas
    var showAddTaskDialog by mutableStateOf(false)
        private set // Hanya bisa diubah dari dalam ViewModel

    // State untuk tipe penyortiran saat ini
    var currentSortType by mutableStateOf(SortType.NONE)
        private set



    // Contoh data awal (opsional)
    init {
        // Tambahkan beberapa contoh tugas jika perlu untuk testing awal
        // tasks.addAll(listOf(
        //     Task(title = "Belajar Compose", deadline = LocalDateTime.now().plusDays(1)),
        //     Task(title = "Beli Susu", deadline = LocalDateTime.now().plusHours(2), isCompleted = true),
        //     Task(title = "Olahraga", deadline = null)
        // ))
    }

    // Properti terhitung untuk jumlah tugas
    val totalTasks: Int by derivedStateOf { tasks.size }
    val completedTasksCount: Int by derivedStateOf { tasks.count { it.isCompleted } }

    fun addTask(title: String, deadline: LocalDateTime?) {
        if (title.isNotBlank()) { // Hanya tambah jika judul tidak kosong
            val newTask = Task(title = title, deadline = deadline)
            tasks.add(newTask)
        }
        closeAddTaskDialog() // Tutup dialog setelah menambahkan
    }

    fun removeTask(taskId: UUID) {
        tasks.removeAll { it.id == taskId }
    }

    fun toggleTaskStatus(taskId: UUID) {
        val taskIndex = tasks.indexOfFirst { it.id == taskId }
        if (taskIndex != -1) {
            val task = tasks[taskIndex]
            // Buat salinan task dengan status terbalik dan ganti di list
            // Ini penting agar Compose mendeteksi perubahan state pada item
            tasks[taskIndex] = task.copy(isCompleted = !task.isCompleted)
        }
    }

    fun sortTasks(sortType: SortType) {
        currentSortType = sortType
        // Penyortiran dilakukan langsung pada mutableStateListOf
        // atau bisa juga menggunakan derived state di UI jika lebih kompleks
        when (sortType) {
            SortType.DEADLINE -> {
                // Sortir berdasarkan deadline (nulls last), lalu berdasarkan judul
                tasks.sortBy { it.deadline ?: LocalDateTime.MAX } // Taruh yang null di akhir
            }
            SortType.STATUS -> {
                // Sortir berdasarkan status (belum selesai dulu), lalu deadline
                tasks.sortWith(compareBy<Task> { it.isCompleted }.thenBy { it.deadline ?: LocalDateTime.MAX })
            }
            SortType.NONE -> {
                // Mungkin kembali ke urutan penambahan atau urutan default lain
                // Untuk kesederhanaan, kita bisa biarkan seperti terakhir disortir
                // atau implementasikan cara mengembalikan ke urutan asli jika perlu
            }
        }
    }

    fun openAddTaskDialog() {
        showAddTaskDialog = true
    }

    fun closeAddTaskDialog() {
        showAddTaskDialog = false
    }
}