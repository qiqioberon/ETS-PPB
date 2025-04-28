package com.example.dailytaskmanager

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.time.LocalDateTime
import java.util.Comparator
import java.util.UUID

enum class SortType {
    NONE, DEADLINE
}

class TaskViewModel : ViewModel() {

    private val _tasks = mutableStateListOf<Task>()
    // Expose _tasks langsung jika UI perlu list mentah, atau tidak sama sekali
    // Jika UI hanya butuh pending/completed, _tasks bisa private total.

    // State sortir
    var currentSortType by mutableStateOf(SortType.NONE)
        private set
    var sortDeadlineAscending by mutableStateOf(true)
        private set

    // derivedStateOf yang melakukan filter DAN sort
    val pendingTasksState: State<List<Task>> = derivedStateOf {
        val filtered = _tasks.filter { !it.isCompleted }
        applySortingLogic(filtered) // Terapkan sorting ke hasil filter
    }
    val completedTasksState: State<List<Task>> = derivedStateOf {
        val filtered = _tasks.filter { it.isCompleted }
        applySortingLogic(filtered) // Terapkan sorting ke hasil filter
    }

    var showAddTaskDialog by mutableStateOf(false)
        private set

    // Properti terhitung tetap sama
    val totalTasks: Int by derivedStateOf { _tasks.size }
    val completedTasksCount: Int by derivedStateOf { _tasks.count { it.isCompleted } }

    // --- Fungsi Modifikasi State ---
    // Hanya memodifikasi state dasar (_tasks atau state sortir)

    fun addTask(title: String, deadline: LocalDateTime?) {
        if (title.isNotBlank()) {
            val newTask = Task(title = title, deadline = deadline)
            _tasks.add(newTask) // Cukup tambahkan, derivedStateOf akan handle sisanya
        }
        closeAddTaskDialog()
    }

    fun removeTask(taskId: UUID) {
        _tasks.removeAll { it.id == taskId } // derivedStateOf akan handle sisanya
    }

    fun toggleTaskStatus(taskId: UUID) {
        val taskIndex = _tasks.indexOfFirst { it.id == taskId }
        if (taskIndex != -1) {
            val task = _tasks[taskIndex]
            // Modifikasi langsung di SnapshotStateList akan memicu derivedStateOf
            _tasks[taskIndex] = task.copy(isCompleted = !task.isCompleted)
        }
    }

    fun toggleDeadlineSort() {
        if (currentSortType == SortType.DEADLINE) {
            sortDeadlineAscending = !sortDeadlineAscending
        } else {
            currentSortType = SortType.DEADLINE
            sortDeadlineAscending = true
        }
        // Perubahan state sort akan memicu derivedStateOf
    }

    fun clearSort() {
        currentSortType = SortType.NONE
        sortDeadlineAscending = true
        // Perubahan state sort akan memicu derivedStateOf
    }

    // --- Helper Function untuk Logika Sorting (hanya dipanggil oleh derivedStateOf) ---
    private fun applySortingLogic(listToSort: List<Task>): List<Task> {
        return when (currentSortType) {
            SortType.DEADLINE -> {
                val comparator = compareBy<Task, LocalDateTime?>(nullsLast(Comparator.naturalOrder())) { it.deadline }
                if (sortDeadlineAscending) {
                    listToSort.sortedWith(comparator)
                } else {
                    listToSort.sortedWith(comparator.reversed())
                }
            }
            SortType.NONE -> {
                // Ketika tidak ada sort, kita mungkin ingin urutan penambahan asli
                // Namun, karena kita memfilter, urutan asli hilang. ID bisa jadi proxy.
                listToSort.sortedBy { it.id.toString() }
            }
        }
    }

    fun openAddTaskDialog() { showAddTaskDialog = true }
    fun closeAddTaskDialog() { showAddTaskDialog = false }
}