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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.Sort // Tetap import jika ikon sort masih relevan
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.DeleteOutline // Import ikon hapus
import androidx.compose.material.icons.filled.ArrowDownward // Import ikon panah bawah
import androidx.compose.material.icons.filled.ArrowUpward // Import ikon panah atas
import androidx.compose.material.icons.filled.CheckCircleOutline // Hapus jika tidak dipakai
import androidx.compose.material.icons.filled.Clear // Import ikon clear
import androidx.compose.material.icons.filled.DateRange // Import ikon tanggal
import androidx.compose.material.icons.filled.Sort // Import ikon sort (jika ingin dipakai di tombol utama)


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

// --- Helper Function untuk Format Tanggal (Tidak Berubah) ---
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
    var showCompletedTasks by rememberSaveable { mutableStateOf(true) }

    // Baca state dari ViewModel
    val pendingTasks by taskViewModel.pendingTasksState
    val completedTasks by taskViewModel.completedTasksState

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { taskViewModel.openAddTaskDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Filled.Add, // Gunakan ikon Add
                    contentDescription = "Add Task",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        )  {
            // 1. Header Card
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HeaderCard(
                    completedCount = taskViewModel.completedTasksCount,
                    totalCount = taskViewModel.totalTasks,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- Tombol Sortir ---
            item {
                SortControls(
                    currentSortType = taskViewModel.currentSortType,
                    isAscending = taskViewModel.sortDeadlineAscending, // Kirim state arah sortir
                    onToggleDeadlineSort = { taskViewModel.toggleDeadlineSort() }, // Panggil fungsi toggle
                    onClearSort = { taskViewModel.clearSort() } // Panggil fungsi clear
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 2. Section "Task" (Pending)
            item {
                TaskListSectionHeader(
                    title = "Task",
                    // Hapus showSeeAll dan onSeeAllClick
                    showSeeAll = false
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Daftar Task Pending
            if (pendingTasks.isEmpty()) {
                item {
                    Text(
                        "No pending tasks!", // Ubah pesan
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
                        onDelete = { taskViewModel.removeTask(task.id) }
                    )
                }
            }


            // 3. Section "Completed"
            item {
                Spacer(modifier = Modifier.height(16.dp))
                TaskListSectionHeader(
                    title = "Completed",
                    isExpanded = showCompletedTasks,
                    onToggleExpand = { showCompletedTasks = !showCompletedTasks },
                    showSeeAll = false // Pastikan tidak ada "See all" di Completed
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Daftar Task Completed (Muncul/Hilang berdasarkan state)
            item {
                AnimatedVisibility(visible = showCompletedTasks) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (completedTasks.isEmpty()) {
                            Text(
                                "No tasks completed yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp)
                            )
                        } else {
                            // Gunakan forEach pada list completedTasks
                            completedTasks.forEach { task ->
                                TaskListItem(
                                    task = task,
                                    onCheckedChange = { taskViewModel.toggleTaskStatus(task.id) },
                                    onDelete = { taskViewModel.removeTask(task.id) }
                                )
                            }
                        }
                    }
                }
            }
        } // Akhir LazyColumn
    }

    // AddTaskDialog (Tidak Berubah)
    if (taskViewModel.showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { taskViewModel.closeAddTaskDialog() },
            onAddTask = { title, deadline ->
                taskViewModel.addTask(title, deadline)
            }
        )
    }
}

// --- Composable untuk Tombol Sortir ---
@Composable
fun SortControls(
    currentSortType: SortType,
    isAscending: Boolean, // Menerima status arah sortir
    onToggleDeadlineSort: () -> Unit, // Callback untuk toggle deadline sort
    onClearSort: () -> Unit, // Callback untuk clear sort
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), // Rata kanan
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tombol Sortir Deadline (Satu-satunya tombol sortir utama)
        Button(
            onClick = onToggleDeadlineSort, // Panggil toggle saat diklik
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), // Sesuaikan padding
            // Tombol akan selalu aktif jika bisa diklik, beri visual berbeda jika sorting aktif
            colors = ButtonDefaults.buttonColors(
                containerColor = if (currentSortType == SortType.DEADLINE) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (currentSortType == SortType.DEADLINE) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(
                Icons.Default.DateRange, // Ikon kalender tetap
                contentDescription = "Sort by Deadline",
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("Deadline")
            // Tambahkan ikon panah untuk menunjukkan arah sortir HANYA jika sorting deadline aktif
            if (currentSortType == SortType.DEADLINE) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = if (isAscending) "Ascending" else "Descending",
                    modifier = Modifier.size(16.dp) // Buat ikon panah sedikit lebih kecil
                )
            }
        }

        // Tombol untuk menghapus sorting (Hanya muncul jika sorting aktif)
        if (currentSortType != SortType.NONE) {
            IconButton(onClick = onClearSort) { // Panggil clear sort
                Icon(Icons.Default.Clear, contentDescription = "Clear Sort")
            }
        }
    }
}



// --- Header Card (Tidak Berubah) ---
@Composable
fun HeaderCard(completedCount: Int, totalCount: Int, modifier: Modifier = Modifier) {
    // ... (kode sama seperti sebelumnya) ...
    val today = LocalDate.now()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }

    Card(
        modifier = modifier.height(IntrinsicSize.Min),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Filled.TaskAlt,
                        contentDescription = "Tasks Overview",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Today, ${today.format(dateFormatter)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = "$completedCount/$totalCount",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.Top)
                )
            }
        }
    }
}

// --- Header Section List (Parameter showSeeAll tidak lagi relevan untuk section Task) ---
@Composable
fun TaskListSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    isExpanded: Boolean? = null,
    onToggleExpand: (() -> Unit)? = null,
    showSeeAll: Boolean = false, // Pertahankan parameter ini untuk fleksibilitas, tapi panggilannya disesuaikan
    onSeeAllClick: (() -> Unit)? = null // Pertahankan parameter ini
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
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
        // Logika "See all" tetap ada di sini, tapi panggilannya di TaskAppScreen yang mengontrol
        if (showSeeAll && onSeeAllClick != null) {
            TextButton(onClick = onSeeAllClick) {
                Text("See all", fontWeight = FontWeight.Medium)
            }
        }
    }
}


// --- Modifikasi Composable TaskItem untuk menambahkan Tombol Hapus ---
@Composable
fun TaskListItem(
    task: Task,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit, // Tambahkan callback untuk hapus
    modifier: Modifier = Modifier
) {
    val cardContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)), // Hapus clickable di sini agar tombol hapus bisa diklik
        // .clickable { onCheckedChange(!task.isCompleted) }, // Pindahkan clickable ke Row atau Box jika perlu area klik lebih luas selain Checkbox
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp) // Sedikit elevasi
    ) {
        Row(
            modifier = Modifier
                //.clickable { onCheckedChange(!task.isCompleted) } // Klik Row untuk toggle status
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                // --- Logika Tampilan Deadline Diperbarui ---
                val relativeDateText = formatDeadlineRelative(task.deadline) // Dapat "Today", "Tomorrow", "Mar 23", dll.

                // Buat teks display final
                val deadlineDisplay = if (task.deadline != null && relativeDateText.isNotEmpty()) {
                    // Jika ada deadline dan teks tanggal relatif tidak kosong
                    // Format waktu menggunakan Localized Short format (mis: 10:30 AM / 14:00)
                    val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
                    val timeText = task.deadline.toLocalTime().format(timeFormatter) // Ambil hanya bagian waktu

                    // Gabungkan tanggal relatif dan waktu
                    "$relativeDateText at $timeText"
                } else {
                    // Jika tidak ada deadline atau teks tanggal kosong, tampilkan apa adanya
                    relativeDateText
                }
                // Hanya tampilkan baris deadline jika ada teks untuk ditampilkan
                if (deadlineDisplay.isNotEmpty()) {
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
                            text = deadlineDisplay, // Gunakan teks yang sudah digabung/relatif
                            style = MaterialTheme.typography.bodySmall, // Ukuran teks kecil
                            color = MaterialTheme.colorScheme.onSurfaceVariant // Warna teks deadline
                        )
                    }
                }
            }
            // Tambahkan Tombol Hapus (IconButton)
            Spacer(modifier = Modifier.width(8.dp)) // Jarak sebelum tombol hapus
            IconButton(
                onClick = onDelete, // Panggil callback onDelete saat diklik
                modifier = Modifier.size(40.dp) // Ukuran area klik tombol
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline, // Gunakan ikon hapus
                    contentDescription = "Delete Task",
                    tint = MaterialTheme.colorScheme.error // Warna merah untuk indikasi hapus
                )
            }
        }
    }
}


// --- AddTaskDialog (Tidak Berubah) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAddTask: (String, LocalDateTime?) -> Unit
) {
    // ... (kode sama seperti sebelumnya) ...
    var title by rememberSaveable { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Tambah Tugas Baru") }, text = {
        Column {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Judul Tugas") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Deadline (Opsional):")
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: "Pilih Tanggal")
                }
                OutlinedButton(onClick = { showTimePicker = true }, enabled = selectedDate != null) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedTime?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)) ?: "Pilih Waktu")
                }
            }
            if (selectedDate != null || selectedTime != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = {
                    selectedDate = null; selectedTime = null; datePickerState.selectedDateMillis = null
                }) { Text("Hapus Deadline") }
            }
        }
    }, confirmButton = {
        Button(onClick = {
            val deadline = if (selectedDate != null) LocalDateTime.of(selectedDate!!, selectedTime ?: LocalTime.MIDNIGHT) else null
            onAddTask(title, deadline); title = ""; selectedDate = null; selectedTime = null
        }, enabled = title.isNotBlank()) { Text("Tambah") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } })
    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
            TextButton(onClick = {
                showDatePicker = false; datePickerState.selectedDateMillis?.let { selectedDate = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
            }) { Text("OK") }
        }, dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Batal") } }) { DatePicker(state = datePickerState) }
    }
    if (showTimePicker) { TimePickerDialog(onDismissRequest = { showTimePicker = false }, confirmButton = {
        TextButton(onClick = { selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute); showTimePicker = false }) { Text("OK") }
    }, dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Batal") } }) { TimePicker(state = timePickerState) }
    }
}

// --- TimePickerDialog (Tidak Berubah) ---
@Composable
fun TimePickerDialog(title: String = "Pilih Waktu", onDismissRequest: () -> Unit, confirmButton: @Composable (() -> Unit), dismissButton: @Composable (() -> Unit)? = null, content: @Composable () -> Unit) {
    // ... (kode sama seperti sebelumnya) ...
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp, modifier = Modifier.width(IntrinsicSize.Min).height(IntrinsicSize.Min).background(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp), text = title, style = MaterialTheme.typography.labelMedium)
                content()
                Row(modifier = Modifier.height(40.dp).fillMaxWidth()) { Spacer(modifier = Modifier.weight(1f)); dismissButton?.invoke(); confirmButton() }
            }
        }
    }
}


// --- Preview (Disarankan menguji dengan data yang lebih beragam untuk sortir) ---
@Preview(showBackground = true, widthDp = 360, backgroundColor = 0xFFF0F0F0)
@Composable
fun TaskScreenPreview() {
    DailyTaskManagerTheme {
        val previewViewModel = remember { TaskViewModel() }
        LaunchedEffect(Unit) {
            if (previewViewModel.totalTasks == 0) { // Gunakan totalTasks untuk memeriksa kekosongan
                previewViewModel.addTask("Meeting Project", LocalDate.now().plusDays(1).atTime(14, 0)) // Tomorrow
                previewViewModel.addTask("Presentasi Final", LocalDate.now().plusDays(5).atTime(9, 30)) // Friday
                previewViewModel.addTask("Ngoding terus", LocalDate.now().atTime(10, 0)) // Today
                previewViewModel.addTask("Beli bahan makanan", null) // No deadline

            }
        }
        TaskAppScreen(taskViewModel = previewViewModel)
    }
}