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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import androidx.work.*
import com.nexuswavetech.nexusplus.core.HapticHelper
import com.nexuswavetech.nexusplus.core.SettingsRepository
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// ── Background Architecture Execution Core Worker ────────────────────────────

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val title = inputData.getString("title") ?: "Nexus Reminder"
        val body = inputData.getString("body") ?: ""
        val channelId = "nexus_reminders"
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Nexus Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Channels execution for system user alerts and routine trackers"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Deep Link intent configuration to safely open application root context upon activation
        val launchIntent = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(applicationContext, 0, it, pendingIntentFlags)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        return Result.success()
    }
}

// ── Architectural State Entities & Shared ViewModel ─────────────────────────

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
    fun setDelay(v: String) = _uiState.update { it.copy(delayMinutes = v.filter { it.isDigit() }) }

    fun scheduleReminder(context: Context) {
        val currentState = _uiState.value
        val parsedDelay = currentState.delayMinutes.toLongOrNull()?.coerceAtLeast(1L) ?: 5L
        val uniqueId = UUID.randomUUID().toString()
        val runtimeTargetTitle = currentState.title.ifBlank { "Nexus Reminder" }

        val dataPackage = Data.Builder()
            .putString("title", runtimeTargetTitle)
            .putString("body", currentState.body)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(parsedDelay, TimeUnit.MINUTES)
            .setInputData(dataPackage)
            .addTag(uniqueId)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)

        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val computedCalendar = Calendar.getInstance().apply { add(Calendar.MINUTE, parsedDelay.toInt()) }
        
        val newReminder = ReminderItem(
            id = uniqueId,
            title = runtimeTargetTitle,
            body = currentState.body,
            delayMinutes = parsedDelay,
            scheduledAt = timeFormatter.format(computedCalendar.time)
        )

        _uiState.update { 
            it.copy(
                reminders = it.reminders + newReminder, 
                title = "", 
                body = "", 
                delayMinutes = "5"
            ) 
        }
    }

    fun cancelReminder(context: Context, item: ReminderItem) {
        WorkManager.getInstance(context).cancelAllWorkByTag(item.id)
        _uiState.update { state -> state.copy(reminders = state.reminders.filter { it.id != item.id }) }
    }
}

// ── User Interface Dynamic Presentation Screen Layer ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReminderScreen(
    onBack: () -> Unit,
    viewModel: MyReminderViewModel = koinViewModel()
) {
    val uiState  by viewModel.uiState.collectAsState()
    val context  = LocalContext.current
    val view     = LocalView.current
    val haptic   = koinInject<HapticHelper>()
    val settings = koinInject<SettingsRepository>()
    val touchVib by settings.touchVibration.collectAsState(initial = true)

    val presetIntervals = listOf("5", "15", "30", "60")

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "My Reminders Engine", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Configuration Deployment Card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Configure New Track Trigger",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        
                        OutlinedTextField(
                            value = uiState.title,
                            onValueChange = viewModel::setTitle,
                            label = { Text("Reminder Title") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Enter target reminder headline title text" }
                        )
                        
                        OutlinedTextField(
                            value = uiState.body,
                            onValueChange = viewModel::setBody,
                            label = { Text("Supplementary Message Content (Optional)") },
                            minLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Enter optional supplementary context descriptive parameters" }
                        )
                        
                        OutlinedTextField(
                            value = uiState.delayMinutes,
                            onValueChange = viewModel::setDelay,
                            label = { Text("Trigger Delay Allocation (Minutes)") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Timer, null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Set digital time delay spacing inside quantitative minute values" }
                        )

                        // Smart Prescaled Accessibility Chips Layer Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presetIntervals.forEach { minuteLabel ->
                                val isSelected = uiState.delayMinutes == minuteLabel
                                ElevatedFilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        haptic.click(view, touchVib)
                                        viewModel.setDelay(minuteLabel)
                                    },
                                    label = { Text("$minuteLabel Mins") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                haptic.confirm(view, touchVib)
                                val announceDelay = uiState.delayMinutes.ifBlank { "5" }
                                viewModel.scheduleReminder(context)
                                view.announceForAccessibility("Security alert routine sequence scheduled execution within $announceDelay minutes successfully.")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Commit execution pipeline to arm local system reminder trigger parameters" }
                        ) {
                            Icon(Icons.Filled.NotificationAdd, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Deploy Operational Trigger")
                        }
                    }
                }
            }

            // Reminders State Queue Router Control Block
            if (uiState.reminders.isNotEmpty()) {
                item {
                    Text(
                        text = "Active Queue Schedules",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                    )
                }
                
                items(uiState.reminders, key = { it.id }) { reminder ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Active reminder track structural package labeled: ${reminder.title}. Scheduled deployment timestamp at ${reminder.scheduledAt}." }
                    ) {
                        ListItem(
                            leadingContent = {
                                Icon(Icons.Filled.Alarm, null, tint = MaterialTheme.colorScheme.primary)
                            },
                            headlineContent = { Text(reminder.title, fontWeight = FontWeight.SemiBold) },
                            supportingContent = {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    if (reminder.body.isNotBlank()) {
                                        Text(reminder.body, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        text = "Fires at ~${reminder.scheduledAt} (${reminder.delayMinutes} min interval duration)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        haptic.click(view, touchVib)
                                        viewModel.cancelReminder(context, reminder)
                                        view.announceForAccessibility("Internal structural package track designated as ${reminder.title} has been scrubbed.")
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Cancel, 
                                        contentDescription = "Terminate verification process schedule parameters for ${reminder.title}", 
                                        tint = MaterialTheme.colorScheme.error
                                    )
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
                            .padding(vertical = 40.dp)
                            .semantics { contentDescription = "Active pipeline tracking parameters database state is empty" },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.NotificationsNone, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                            Text("No active execution tracking parameters", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
