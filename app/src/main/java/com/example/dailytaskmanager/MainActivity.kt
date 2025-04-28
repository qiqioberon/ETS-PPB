package com.example.dailytaskmanager // Ganti dengan package name Anda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dailytaskmanager.ui.theme.DailyTaskManagerTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape // Import untuk rounded shape
import androidx.compose.material.icons.automirrored.filled.Sort // Import Sort
import androidx.compose.animation.AnimatedVisibility // Import untuk collapse/expand
import androidx.compose.foundation.clickable // Import clickable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DailyTaskManagerTheme {
                TaskAppScreen()
            }
        }
    }
}

// --- Helper Function untuk Format Tanggal ---
fun formatDeadlineRelative(deadline: LocalDateTime?, today: LocalDate = LocalDate.now()): String {
    if (deadline == null) return "" // Atau "No deadline"

    val deadlineDate = deadline.toLocalDate()
    val period = Period.between(today, deadlineDate)

    return when {
        deadlineDate.isEqual(today) -> "Today"
        deadlineDate.isEqual(today.plusDays(1)) -> "Tomorrow"
        deadlineDate.isEqual(today.minusDays(1)) -> "Yesterday"
        period.years != 0 -> deadline.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) // "Mar 23, 2024"
        period.months != 0 -> deadline.format(DateTimeFormatter.ofPattern("MMM d")) // "Mar 23"
        period.days < 7 && period.days > 0 -> deadline.format(DateTimeFormatter.ofPattern("EEE")) // "Tue" (jika dalam minggu ini)
        else -> deadline.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) // "3/23/24"
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskAppScreen(taskViewModel: TaskViewModel = viewModel()) {
    // State untuk mengontrol visibilitas bagian 'Completed'
    var showCompletedTasks by rememberSaveable { mutableStateOf(true) }

    // Pisahkan tugas menjadi pending dan completed
    // Kita tidak butuh sortedTasks lagi karena pemisahan sudah dilakukan
    val allTasks = taskViewModel.tasks.toList() // Ambil snapshot list saat ini
    val pendingTasks = remember(allTasks) { allTasks.filter { !it.isCompleted } }
    val completedTasks = remember(allTasks) { allTasks.filter { it.isCompleted } }

    Scaffold(
        // Tidak ada TopAppBar di UI target
        floatingActionButton = {
            FloatingActionButton(
                onClick = { taskViewModel.openAddTaskDialog() },
                // Warna FAB sesuai gambar
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add Task",
                    tint = MaterialTheme.colorScheme.onPrimary // Warna ikon di FAB
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface // Warna background utama
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Terapkan padding dari Scaffold
                .padding(horizontal = 16.dp), // Padding horizontal utama
            verticalArrangement = Arrangement.spacedBy(12.dp), // Jarak antar elemen utama
            contentPadding = PaddingValues(bottom = 80.dp) // Padding bawah agar tidak tertutup FAB
        ) {
            // 1. Header Card
            item {
                Spacer(modifier = Modifier.height(8.dp)) // Jarak dari atas
                HeaderCard(
                    completedCount = taskViewModel.completedTasksCount,
                    totalCount = taskViewModel.totalTasks,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp)) // Jarak setelah header
            }

            // 2. Section "Task" (Pending)
            item {
                TaskListSectionHeader(
                    title = "Task",
                    showSeeAll = true, // Tampilkan "See all"
                    onSeeAllClick = { /* TODO: Implement See All logic */ }
                )
                Spacer(modifier = Modifier.height(8.dp)) // Jarak setelah header section
            }

            // Daftar Task Pending
            if (pendingTasks.isEmpty()) {
                item {
                    Text(
                        "No pending tasks for today!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp)
                    )
                }
            } else {
                items(items = pendingTasks, key = { task -> "pending-${task.id}" }) { task ->
                    TaskListItem(
                        task = task,
                        onCheckedChange = { taskViewModel.toggleTaskStatus(task.id) },
                        // onDelete = { taskViewModel.removeTask(task.id) } // Hapus jika tidak ingin ada aksi delete langsung
                    )
                }
            }


            // 3. Section "Completed"
            item {
                Spacer(modifier = Modifier.height(16.dp)) // Jarak sebelum Completed
                TaskListSectionHeader(
                    title = "Completed",
                    isExpanded = showCompletedTasks,
                    onToggleExpand = { showCompletedTasks = !showCompletedTasks }, // Toggle state
                    showSeeAll = false // Tidak ada "See all" di Completed
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Daftar Task Completed (Muncul/Hilang berdasarkan state)
            // Pakai AnimatedVisibility untuk animasi collapse/expand
            item { // Bungkus items dalam satu item LazyColumn agar animasi bekerja
                AnimatedVisibility(visible = showCompletedTasks) {
                    // Gunakan Column di dalam AnimatedVisibility jika items dipanggil di sini
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (completedTasks.isEmpty()) {
                            Text(
                                "No tasks completed yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp)
                            )
                        } else {
                            completedTasks.forEach { task ->
                                TaskListItem(
                                    task = task,
                                    onCheckedChange = { taskViewModel.toggleTaskStatus(task.id) },
                                    // onDelete = { taskViewModel.removeTask(task.id) }
                                )
                            }
                        }
                    }
                }
            }
        } // Akhir LazyColumn
    }

    // AddTaskDialog tetap sama
    if (taskViewModel.showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { taskViewModel.closeAddTaskDialog() },
            onAddTask = { title, deadline ->
                taskViewModel.addTask(title, deadline)
            }
        )
    }
}


// --- Composable Baru: Header Card ---
@Composable
fun HeaderCard(
    completedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy") } // Format: 23 Mar 2021

    Card(
        modifier = modifier.height(IntrinsicSize.Min), // Agar Card menyesuaikan tinggi konten
        shape = RoundedCornerShape(16.dp), // Sudut melengkung
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary), // Warna biru
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically // Pusatkan item secara vertikal
            ) {
                // Kolom untuk Ikon dan Tanggal
                Column(modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Filled.TaskAlt, // Ikon mirip di gambar
                        contentDescription = "Tasks Overview",
                        tint = MaterialTheme.colorScheme.onPrimary, // Warna putih
                        modifier = Modifier.size(48.dp) // Ukuran ikon
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Today, ${today.format(dateFormatter)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimary // Warna putih
                    )
                }
                // Teks Jumlah Tugas (di kanan atas) - Kita letakkan saja di sini
                Text(
                    text = "$completedCount/$totalCount",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), // Sedikit transparan
                    modifier = Modifier.align(Alignment.Top) // Align ke atas Row
                )
            }
        }
    }
}

// --- Composable Baru: Header Section List ---
@Composable
fun TaskListSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    isExpanded: Boolean? = null, // Nullable jika tidak ada expand/collapse
    onToggleExpand: (() -> Unit)? = null, // Callback untuk toggle
    showSeeAll: Boolean = false,
    onSeeAllClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween // Agar title dan tombol terpisah
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface // Warna teks judul section
            )
            // Tampilkan tombol expand/collapse jika ada state-nya
            if (isExpanded != null && onToggleExpand != null) {
                IconButton(onClick = onToggleExpand, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (showSeeAll && onSeeAllClick != null) {
            TextButton(onClick = onSeeAllClick) {
                Text("See all", fontWeight = FontWeight.Medium)
            }
        }
    }
}


// --- Modifikasi Composable TaskItem ---
@Composable
fun TaskListItem(
    task: Task,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
    // onDelete: () -> Unit, // Hapus jika tidak perlu
) {
    // Warna latar belakang Card item
    val cardContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh // Warna lebih terang dari background utama

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)) // Clip agar clickable mengikuti shape
            .clickable { onCheckedChange(!task.isCompleted) }, // Klik di mana saja pada Card untuk toggle
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Tanpa shadow atau sedikit
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp) // Padding dalam Card item
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox (tampilannya akan standar M3)
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = null, // Aksi sudah ditangani oleh Card.clickable
                modifier = Modifier.size(24.dp) // Sesuaikan ukuran jika perlu
                // colors = CheckboxDefaults.colors(...) // Bisa kustomisasi warna jika mau
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge, // Ukuran teks judul task
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface // Redup jika selesai
                )
                // Tampilkan deadline jika ada
                val deadlineText = formatDeadlineRelative(task.deadline)
                if (deadlineText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday, // Ikon kalender
                            contentDescription = "Deadline",
                            modifier = Modifier.size(14.dp), // Ukuran ikon kecil
                            tint = MaterialTheme.colorScheme.onSurfaceVariant // Warna ikon
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = deadlineText,
                            style = MaterialTheme.typography.bodySmall, // Ukuran teks kecil
                            color = MaterialTheme.colorScheme.onSurfaceVariant // Warna teks deadline
                        )
                    }
                }
            }
            // Hapus IconButton Delete dari sini jika tidak ada di UI target
            // Spacer(modifier = Modifier.width(8.dp))
            // IconButton(onClick = onDelete) {
            //     Icon(
            //         Icons.Filled.DeleteOutline,
            //         contentDescription = "Delete Task",
            //         tint = MaterialTheme.colorScheme.error
            //     )
            // }
        }
    }
}

// --- AddTaskDialog, TimePickerDialog, SortMenu (Tidak Berubah) ---
// (Salin kode AddTaskDialog, TimePickerDialog, dan SortMenu dari jawaban sebelumnya)
// Catatan: SortMenu tidak digunakan lagi karena tidak ada TopAppBar, Anda bisa menghapusnya
// atau mengintegrasikannya di tempat lain jika perlu sorting.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAddTask: (String, LocalDateTime?) -> Unit
) {
    // ... (Kode AddTaskDialog tetap sama dari jawaban sebelumnya) ...
    var title by rememberSaveable { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Tugas Baru") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Tugas") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Deadline (Opsional):")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(selectedDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: "Pilih Tanggal")
                    }
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        enabled = selectedDate != null
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(selectedTime?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)) ?: "Pilih Waktu")
                    }
                }
                if (selectedDate != null || selectedTime != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = {
                        selectedDate = null
                        selectedTime = null
                        datePickerState.selectedDateMillis = null
                    }) {
                        Text("Hapus Deadline")
                    }
                }

            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val deadline = if (selectedDate != null) {
                        LocalDateTime.of(selectedDate!!, selectedTime ?: LocalTime.MIDNIGHT)
                    } else {
                        null
                    }
                    onAddTask(title, deadline)
                    title = ""
                    selectedDate = null
                    selectedTime = null
                },
                enabled = title.isNotBlank()
            ) {
                Text("Tambah")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        }
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Batal") }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

@Composable
fun TimePickerDialog(
    title: String = "Pilih Waktu",
    onDismissRequest: () -> Unit,
    confirmButton: @Composable (() -> Unit),
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    // ... (Kode TimePickerDialog tetap sama dari jawaban sebelumnya) ...
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface
                ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = title,
                    style = MaterialTheme.typography.labelMedium
                )
                content()
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}

// --- Preview ---
@Preview(showBackground = true, widthDp = 360, backgroundColor = 0xFFF0F0F0) // Latar belakang preview abu-abu
@Composable
fun TaskScreenPreview() {
    DailyTaskManagerTheme {
        val previewViewModel = remember { TaskViewModel() }
        // Tambahkan data dummy yang lebih mirip UI target
        LaunchedEffect(Unit) {
            if (previewViewModel.tasks.isEmpty()) {
                previewViewModel.addTask("Ngoding terus", LocalDate.now().atTime(10, 0)) // Today
                previewViewModel.addTask("Meeting Project", LocalDate.now().plusDays(1).atTime(14, 0)) // Tomorrow
                previewViewModel.addTask("Beli bahan makanan", LocalDate.now().minusDays(2).atTime(18, 0)) // Yesterday (Example)
                previewViewModel.addTask("Presentasi Final", LocalDate.now().plusDays(5).atTime(9, 30)) // Friday (example)
                // Completed Tasks
                val completed1 = Task(title = "Ngoding", deadline = LocalDate.now().atTime(8, 0), isCompleted = true)
                val completed2 = Task(title = "Ngoding lagi", deadline = LocalDate.now().atTime(9, 0), isCompleted = true)
                val completed3 = Task(title = "Push ke Github", deadline = LocalDate.now().minusDays(1).atTime(17,0), isCompleted = true)
                previewViewModel.tasks.addAll(listOf(completed1, completed2, completed3))
            }
        }
        TaskAppScreen(taskViewModel = previewViewModel)
    }
}