package com.nexuswavetech.nexusplus.features.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra("label") ?: "Alarm"
        android.widget.Toast.makeText(context, "⏰ $label", android.widget.Toast.LENGTH_LONG).show()
    }
}

private data class AlarmItem(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val label: String,
    val enabled: Boolean,
    val repeatDays: Set<Int> = emptySet(),
)

private val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
private val clockFmt = DateTimeFormatter.ofPattern("hh:mm:ss a")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmClockScreen(onBack: () -> Unit) {
    val context      = LocalContext.current
    val alarmManager = remember { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }

    var alarms      by remember { mutableStateOf<List<AlarmItem>>(emptyList()) }
    var showAddSheet by remember { mutableStateOf(false) }
    var currentTime  by remember { mutableStateOf(LocalDateTime.now()) }

    var newHour     by remember { mutableIntStateOf(7) }
    var newMinute   by remember { mutableIntStateOf(0) }
    var newLabel    by remember { mutableStateOf("") }
    var newRepeat   by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var nextAlarmId  by remember { mutableIntStateOf(1000) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalDateTime.now()
            delay(1_000L)
        }
    }

    fun scheduleAlarm(alarm: AlarmItem) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val intent  = Intent(context, AlarmReceiver::class.java).apply { putExtra("label", alarm.label.ifBlank { "Alarm" }) }
        val pending = PendingIntent.getBroadcast(context, alarm.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        runCatching {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending)
        }
    }

    fun cancelAlarm(alarm: AlarmItem) {
        val intent  = Intent(context, AlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(context, alarm.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pending)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Alarm Clock", onBack = onBack, actions = {
            IconButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add alarm")
            }
        })

        // Live clock
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = currentTime.format(clockFmt),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                ),
                color    = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { contentDescription = "Current time: ${currentTime.format(clockFmt)}" },
            )
        }

        HorizontalDivider()

        if (alarms.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxSize().semantics { contentDescription = "No alarms set. Tap + to add one." },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Alarm, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Text("No alarms", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilledTonalButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Filled.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Alarm")
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(alarms, key = { _, a -> a.id }) { index, alarm ->
                    val timeStr = "%02d:%02d %s".format(
                        if (alarm.hour % 12 == 0) 12 else alarm.hour % 12,
                        alarm.minute,
                        if (alarm.hour < 12) "AM" else "PM",
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentDescription = "${alarm.label.ifBlank { "Alarm" }} at $timeStr. ${if (alarm.enabled) "Enabled" else "Disabled"}."
                        },
                        colors   = CardDefaults.cardColors(
                            containerColor = if (alarm.enabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Row(
                            modifier          = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    timeStr,
                                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                                    color = if (alarm.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                )
                                Text(
                                    alarm.label.ifBlank { "Alarm" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (alarm.repeatDays.isNotEmpty()) {
                                    Text(
                                        alarm.repeatDays.sorted().joinToString(" ") { dayNames[it] },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            Switch(
                                checked         = alarm.enabled,
                                onCheckedChange = { enabled ->
                                    val updated = alarm.copy(enabled = enabled)
                                    alarms = alarms.toMutableList().also { it[index] = updated }
                                    if (enabled) scheduleAlarm(updated) else cancelAlarm(updated)
                                },
                            )
                            IconButton(onClick = {
                                cancelAlarm(alarm)
                                alarms = alarms.toMutableList().also { it.removeAt(index) }
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete alarm", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("New Alarm", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

                // Time picker
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hour:", style = MaterialTheme.typography.bodyMedium)
                    Slider(value = newHour.toFloat(), onValueChange = { newHour = it.toInt() }, valueRange = 0f..23f, steps = 22, modifier = Modifier.weight(1f))
                    Text("%02d".format(newHour), style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Min:  ", style = MaterialTheme.typography.bodyMedium)
                    Slider(value = newMinute.toFloat(), onValueChange = { newMinute = it.toInt() }, valueRange = 0f..59f, steps = 58, modifier = Modifier.weight(1f))
                    Text("%02d".format(newMinute), style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                }
                Text("Set: %02d:%02d %s".format(if (newHour % 12 == 0) 12 else newHour % 12, newMinute, if (newHour < 12) "AM" else "PM"), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(value = newLabel, onValueChange = { newLabel = it }, label = { Text("Label (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                // Repeat days
                Text("Repeat on:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    dayNames.forEachIndexed { i, day ->
                        FilterChip(selected = i in newRepeat, onClick = { newRepeat = if (i in newRepeat) newRepeat - i else newRepeat + i }, label = { Text(day, style = MaterialTheme.typography.labelSmall) })
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { showAddSheet = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = {
                            val alarm = AlarmItem(id = nextAlarmId++, hour = newHour, minute = newMinute, label = newLabel.trim(), enabled = true, repeatDays = newRepeat)
                            scheduleAlarm(alarm)
                            alarms = alarms + alarm
                            showAddSheet = false
                            newLabel = ""; newRepeat = emptySet()
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Set Alarm") }
                }
            }
        }
    }
}
