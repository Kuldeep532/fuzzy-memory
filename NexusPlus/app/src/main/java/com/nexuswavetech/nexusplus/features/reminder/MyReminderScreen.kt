package com.nexuswavetech.nexusplus.features.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// ── Worker ───────────────────────────────────────────────────────────────────

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val title = inputData.getString("title") ?: "Nexus Reminder"
        val body = inputData.getString("body") ?: ""
        val channelId = "nexus_reminders"
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(channelId, "Nexus Reminders", NotificationManager.IMPORTANCE_HIGH))
        }
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notification)
        return Result.success()
    }
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

data class ReminderItem(
    val id: String,
    val title: String,
    val body: String,
    val delayMinutes: Long,
    val scheduledAt: String
)

data class ReminderUiState(
    val reminders: List<ReminderItem> = emptyList(),
    val title: String = "",
    val body: String = "",
    val delayMinutes: String = "5"
)

class MyReminderViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ReminderUiState())
    val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()

    fun setTitle(v: String) = _uiState.update { it.copy(title = v) }
    fun setBody(v: String) = _uiState.update { it.copy(body = v) }
    fun setDelay(v: String) = _uiState.update { it.copy(delayMinutes = v) }

    fun scheduleReminder(context: Context) {
        val s = _uiState.value
        val delay = s.delayMinutes.toLongOrNull() ?: 5L
        val id = UUID.randomUUID().toString()
        val data = Data.Builder()
            .putString("title", s.title.ifBlank { "Nexus Reminder" })
            .putString("body", s.body)
            .build()
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .setInputData(data)
            .addTag(id)
            .build()
        WorkManager.getInstance(context).enqueue(request)
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val cal = Calendar.getInstance().apply { add(Calendar.MINUTE, delay.toInt()) }
        val item = ReminderItem(id, s.title.ifBlank { "Reminder" }, s.body, delay, sdf.format(cal.time))
        _uiState.update { it.copy(reminders = it.reminders + item, title = "", body = "", delayMinutes = "5") }
    }

    fun cancelReminder(context: Context, item: ReminderItem) {
        WorkManager.getInstance(context).cancelAllWorkByTag(item.id)
        _uiState.update { it.copy(reminders = it.reminders.filter { r -> r.id != item.id }) }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun MyReminderScreen(
    onBack: () -> Unit,
    viewModel: MyReminderViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "My Reminder", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "New Reminder",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        OutlinedTextField(
                            value = uiState.title,
                            onValueChange = viewModel::setTitle,
                            label = { Text("Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Reminder title" }
                        )
                        OutlinedTextField(
                            value = uiState.body,
                            onValueChange = viewModel::setBody,
                            label = { Text("Message (optional)") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.delayMinutes,
                                onValueChange = viewModel::setDelay,
                                label = { Text("Delay (minutes)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.scheduleReminder(context)
                                view.announceForAccessibility("Reminder scheduled in ${uiState.delayMinutes} minutes")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Schedule reminder" }
                        ) {
                            Icon(Icons.Filled.NotificationAdd, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Schedule Reminder")
                        }
                    }
                }
            }

            if (uiState.reminders.isNotEmpty()) {
                item {
                    Text(
                        "Scheduled Reminders",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(uiState.reminders, key = { it.id }) { reminder ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Reminder: ${reminder.title}. Fires at ${reminder.scheduledAt}." }
                    ) {
                        ListItem(
                            leadingContent = {
                                Icon(Icons.Filled.Alarm, null, tint = MaterialTheme.colorScheme.primary)
                            },
                            headlineContent = { Text(reminder.title, fontWeight = FontWeight.SemiBold) },
                            supportingContent = {
                                Column {
                                    if (reminder.body.isNotBlank()) Text(reminder.body, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "Fires at ~${reminder.scheduledAt} (${reminder.delayMinutes} min)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            trailingContent = {
                                IconButton(onClick = {
                                    viewModel.cancelReminder(context, reminder)
                                    view.announceForAccessibility("${reminder.title} cancelled")
                                }) {
                                    Icon(Icons.Filled.Cancel, contentDescription = "Cancel ${reminder.title}", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                            .semantics { contentDescription = "No reminders scheduled." },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No active reminders", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
