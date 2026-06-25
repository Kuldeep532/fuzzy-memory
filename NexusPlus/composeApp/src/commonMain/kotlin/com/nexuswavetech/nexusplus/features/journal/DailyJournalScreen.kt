package com.nexuswavetech.nexusplus.features.journal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.platform.SettingsStore
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

// ── Storage keys & serialisation ─────────────────────────────────────────────

private const val JOURNAL_KEY = "journal_entries_v1"
private const val ENTRY_SEP   = "\u0001"        // SOH  — between entries
private const val FIELD_SEP   = "\u0000"        // NUL  — between fields within one entry

private data class JournalEntry(
    val id        : Long,
    val dateLabel : String,
    val mood      : String,
    val content   : String,
)

private fun List<JournalEntry>.encode(): String =
    joinToString(ENTRY_SEP) { "${it.id}$FIELD_SEP${it.dateLabel}$FIELD_SEP${it.mood}$FIELD_SEP${it.content}" }

private fun String.decodeEntries(): List<JournalEntry> {
    if (isBlank()) return emptyList()
    return split(ENTRY_SEP).mapNotNull { line ->
        val p = line.split(FIELD_SEP, limit = 4)
        if (p.size < 4) null
        else JournalEntry(
            id        = p[0].toLongOrNull() ?: return@mapNotNull null,
            dateLabel = p[1],
            mood      = p[2],
            content   = p[3],
        )
    }
}

// ── Static data ───────────────────────────────────────────────────────────────

private val MOODS = listOf(
    "Happy"      to "Joyful day",
    "Neutral"    to "Regular day",
    "Reflective" to "Deep thoughts",
    "Grateful"   to "Full of gratitude",
    "Tired"      to "Needs rest",
)

private fun todayLabel(): String = try {
    val sdf = java.text.SimpleDateFormat("EEEE, MMMM d yyyy", java.util.Locale.getDefault())
    sdf.format(java.util.Date())
} catch (_: Exception) { "Today" }

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyJournalScreen(onBack: () -> Unit) {

    val store: SettingsStore = koinInject()
    val scope                = rememberCoroutineScope()

    // Persist via DataStore — survives app restarts
    val rawJson  by store.stringFlow(JOURNAL_KEY, "").collectAsState(initial = "")
    val entries   = remember(rawJson) { rawJson.decodeEntries() }

    var showEditor      by remember { mutableStateOf(false) }
    var editingEntry    by remember { mutableStateOf<JournalEntry?>(null) }
    var draftText       by remember { mutableStateOf("") }
    var selectedMoodIdx by remember { mutableStateOf(0) }
    var deleteTarget    by remember { mutableStateOf<JournalEntry?>(null) }

    fun openNewEntry() {
        editingEntry    = null
        draftText       = ""
        selectedMoodIdx = 0
        showEditor      = true
    }

    fun saveEntry() {
        if (draftText.isBlank()) { showEditor = false; return }
        val mood    = MOODS[selectedMoodIdx].first
        val edited  = editingEntry
        val updated = if (edited != null) {
            entries.map { if (it.id == edited.id) it.copy(content = draftText, mood = mood) else it }
        } else {
            listOf(JournalEntry(System.currentTimeMillis(), todayLabel(), mood, draftText)) + entries
        }
        scope.launch { store.setString(JOURNAL_KEY, updated.encode()) }
        showEditor = false
    }

    fun deleteEntry(target: JournalEntry) {
        val updated = entries.filter { it.id != target.id }
        scope.launch { store.setString(JOURNAL_KEY, updated.encode()) }
        deleteTarget = null
    }

    Scaffold(
        topBar = { NexusTopBar(title = "Daily Journal", onBack = onBack) },
        floatingActionButton = {
            if (!showEditor) {
                FloatingActionButton(
                    onClick  = ::openNewEntry,
                    modifier = Modifier.semantics { contentDescription = "Write new journal entry" },
                ) { Icon(Icons.Filled.Edit, contentDescription = null) }
            }
        },
    ) { padding ->

        if (showEditor) {
            // ── Entry editor ──────────────────────────────────────────────
            Column(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text  = todayLabel(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )

                Text(
                    "How are you feeling?",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MOODS.forEachIndexed { i, (label, _) ->
                        FilterChip(
                            selected = selectedMoodIdx == i,
                            onClick  = { selectedMoodIdx = i },
                            label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.semantics {
                                contentDescription = "$label mood${if (selectedMoodIdx == i) ". Selected." else ""}"
                            },
                        )
                    }
                }

                OutlinedTextField(
                    value         = draftText,
                    onValueChange = { draftText = it },
                    label         = { Text("Write your thoughts…") },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .semantics { contentDescription = "Journal entry text area. ${draftText.length} characters." },
                    maxLines      = Int.MAX_VALUE,
                )

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick  = { showEditor = false },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "Cancel writing journal entry" },
                    ) { Text("Cancel") }
                    Button(
                        onClick  = ::saveEntry,
                        enabled  = draftText.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "Save journal entry" },
                    ) {
                        Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save")
                    }
                }
            }

        } else {
            // ── Entry list ────────────────────────────────────────────────
            if (entries.isEmpty()) {
                Box(
                    modifier         = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .semantics {
                            contentDescription =
                                "No journal entries yet. Tap the write button to add your first entry."
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Filled.MenuBook,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier           = Modifier.size(56.dp),
                        )
                        Text(
                            "Your journal is empty",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "Tap the pen button below to write your first entry",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier            = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(entries, key = { it.id }) { entry ->
                        Card(
                            onClick  = {
                                editingEntry    = entry
                                draftText       = entry.content
                                selectedMoodIdx = MOODS.indexOfFirst { it.first == entry.mood }.coerceAtLeast(0)
                                showEditor      = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Journal entry from ${entry.dateLabel}. Mood: ${entry.mood}." },
                            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape    = MaterialTheme.shapes.medium,
                        ) {
                            Column(
                                modifier            = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text  = entry.dateLabel,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Row(
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Surface(
                                            shape = MaterialTheme.shapes.extraSmall,
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                        ) {
                                            Text(
                                                text     = entry.mood,
                                                style    = MaterialTheme.typography.labelSmall,
                                                color    = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                        IconButton(
                                            onClick  = { deleteTarget = entry },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .semantics { contentDescription = "Delete entry from ${entry.dateLabel}" },
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                null,
                                                tint     = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text     = entry.content,
                                    style    = MaterialTheme.typography.bodyMedium,
                                    color    = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Delete confirmation dialog ─────────────────────────────────────────
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title            = { Text("Delete Entry?") },
            text             = { Text("This journal entry will be permanently deleted.") },
            confirmButton    = {
                TextButton(
                    onClick  = { deleteEntry(target) },
                    modifier = Modifier.semantics { contentDescription = "Confirm delete entry" },
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton    = {
                TextButton(
                    onClick  = { deleteTarget = null },
                    modifier = Modifier.semantics { contentDescription = "Cancel delete entry" },
                ) { Text("Cancel") }
            },
        )
    }
}
