package com.pagewise.app.ui.timer

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pagewise.app.data.db.Book
import com.pagewise.app.service.ReadingTimer
import com.pagewise.app.ui.Routes
import com.pagewise.app.ui.components.LocalRepo
import com.pagewise.app.domain.toLocalDate
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId

private val limits = listOf(null, 10, 15, 20, 30, 45, 60)

/** Picks an optional time limit, then starts the timer and opens the timer screen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StartSessionDialog(book: Book, nav: NavController, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var limit by rememberSaveable { mutableStateOf<Int?>(null) }
    val start = {
        ReadingTimer.start(context, book.id, book.title, book.currentPage, limit)
        onDismiss()
        nav.navigate(Routes.TIMER)
    }
    // The timer lives in a notification, so ask for permission first on Android 13+.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { start() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start reading") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium)
                Text("Starting at page ${book.currentPage}. Set a time limit?", style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    limits.forEach { m ->
                        FilterChip(selected = limit == m, onClick = { limit = m }, label = { Text(m?.let { "$it min" } ?: "No limit") })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else start()
            }) { Text("Start") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Adds a session after the fact, without the timer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSessionDialog(book: Book, onDismiss: () -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    var minutes by rememberSaveable { mutableStateOf("") }
    var from by rememberSaveable { mutableStateOf(book.currentPage.toString()) }
    var to by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var pickDate by rememberSaveable { mutableStateOf(false) }
    val valid = (minutes.toIntOrNull() ?: 0) > 0 && to.toIntOrNull() != null && from.toIntOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log a session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(date.toLocalDate().format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")))
                }
                OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit) }, label = { Text("Minutes read") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(from, { from = it.filter(Char::isDigit) }, label = { Text("From page") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(to, { to = it.filter(Char::isDigit) }, label = { Text("To page") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                scope.launch {
                    repo.saveSession(book.id, date, minutes.toLong() * 60, from.toInt(), to.toInt(), timed = false)
                    onDismiss()
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (pickDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date, selectableDates = PastDates)
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    // Picker returns UTC midnight; keep the local time-of-day at noon to avoid day shifts.
                    state.selectedDateMillis?.let { utc ->
                        val d = Instant.ofEpochMilli(utc).atZone(ZoneId.of("UTC")).toLocalDate()
                        date = d.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }
                    pickDate = false
                }) { Text("OK") }
            },
        ) { DatePicker(state) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private object PastDates : androidx.compose.material3.SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= System.currentTimeMillis()
}
