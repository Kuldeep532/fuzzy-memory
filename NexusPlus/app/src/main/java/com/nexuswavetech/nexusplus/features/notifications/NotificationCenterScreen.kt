package com.nexuswavetech.nexusplus.features.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.core.SessionManager
import com.nexuswavetech.nexusplus.core.UserSession
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

private fun categoryIcon(cat: String): ImageVector = when (cat) {
    "security" -> Icons.Filled.Security
    "update"   -> Icons.Filled.SystemUpdate
    "alert"    -> Icons.Filled.Warning
    else       -> Icons.Filled.Notifications
}

private fun categoryColor(cat: String) = when (cat) {
    "security" -> Color(0xFFF44336)
    "update"   -> Color(0xFF4CAF50)
    "alert"    -> Color(0xFFFFC107)
    else       -> Color(0xFF2196F3)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(onBack: () -> Unit) {
    val repository: NotificationRepository = koinInject()
    val sessionManager: SessionManager     = koinInject()
    val scope                              = rememberCoroutineScope()
    val notifications by repository.notifications.collectAsState(initial = emptyList())
    val session       by sessionManager.session.collectAsState()

    val isAdmin     = (session as? UserSession.Authenticated)?.isAdmin == true
    val unreadCount = notifications.count { !it.isRead }

    var showComposeDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (isAdmin) {
                ExtendedFloatingActionButton(
                    text     = { Text("Send Notification") },
                    icon     = { Icon(Icons.Filled.Edit, contentDescription = "Compose admin notification") },
                    onClick  = { showComposeDialog = true },
                )
            }
        },
    ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        NexusTopBar(
            title   = "Notifications",
            onBack  = onBack,
            actions = {
                if (isAdmin) {
                    IconButton(onClick = {}) {
                        Badge(containerColor = MaterialTheme.colorScheme.tertiary) {
                            Text("Admin", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                if (unreadCount > 0) {
                    TextButton(
                        onClick  = { scope.launch { repository.markAllRead() } },
                        modifier = Modifier.semantics { contentDescription = "Mark all $unreadCount notifications as read" },
                    ) {
                        Text("Mark all read", style = MaterialTheme.typography.labelMedium)
                    }
                }
            },
        )

        // Summary chip
        if (unreadCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Badge { Text("$unreadCount") }
                Text("unread notifications", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (notifications.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxSize().semantics { contentDescription = "No notifications" },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.NotificationsOff, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Text("No notifications", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("App alerts and updates appear here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(notifications, key = { it.id }) { notif ->
                    val icon  = categoryIcon(notif.category)
                    val color = categoryColor(notif.category)

                    val dismissState = rememberDismissState(
                        confirmValueChange = { value ->
                            if (value == DismissValue.DismissedToStart) {
                                scope.launch { repository.deleteNotification(notif.id) }
                                true
                            } else false
                        }
                    )

                    SwipeToDismiss(
                        state      = dismissState,
                        directions = setOf(DismissDirection.EndToStart),
                        background = {
                            Box(
                                modifier         = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete notification", tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissContent = {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics {
                                        contentDescription = "${notif.title}. ${notif.body}. ${if (notif.isRead) "Read." else "Unread."} ${sdf.format(Date(notif.timestampMs))}."
                                    },
                                colors   = CardDefaults.cardColors(
                                    containerColor = if (notif.isRead) MaterialTheme.colorScheme.surfaceVariant
                                                     else MaterialTheme.colorScheme.secondaryContainer,
                                ),
                            ) {
                                Row(
                                    modifier          = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .padding(top = 2.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                notif.title,
                                                style    = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (!notif.isRead) FontWeight.Bold else FontWeight.SemiBold),
                                                modifier = Modifier.weight(1f),
                                            )
                                            if (!notif.isRead) {
                                                Badge(containerColor = MaterialTheme.colorScheme.primary) {}
                                            }
                                        }
                                        Text(notif.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(sdf.format(Date(notif.timestampMs)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                    }
                                    IconButton(
                                        onClick  = { scope.launch { repository.markAsRead(notif.id) } },
                                        enabled  = !notif.isRead,
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            if (notif.isRead) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                            contentDescription = if (notif.isRead) "Already read" else "Mark as read",
                                            tint               = if (notif.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        },
                    )
                }

                item {
                    TextButton(
                        onClick  = { scope.launch { notifications.forEach { repository.deleteNotification(it.id) } } },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Icon(Icons.Filled.DeleteSweep, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Clear all notifications")
                    }
                }
            }
        }
    } // end Scaffold

    // ── Admin: Compose notification dialog ─────────────────────────────────
    if (showComposeDialog) {
        AdminNotifDialog(
            onSend = { title, body, category ->
                scope.launch {
                    val sent = repository.sendAdminNotification(title, body, category, session)
                    if (!sent) { /* session expired / not admin — dialog handles state */ }
                }
                showComposeDialog = false
            },
            onDismiss = { showComposeDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminNotifDialog(
    onSend:   (String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title    by remember { mutableStateOf("") }
    var body     by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("system") }
    val categories = listOf("system", "security", "update", "alert")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send Notification (Admin)", style = MaterialTheme.typography.titleMedium) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Message body") }, maxLines = 4, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (title.isNotBlank() && body.isNotBlank()) onSend(title.trim(), body.trim(), category) }, enabled = title.isNotBlank() && body.isNotBlank()) {
                Text("Send")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
