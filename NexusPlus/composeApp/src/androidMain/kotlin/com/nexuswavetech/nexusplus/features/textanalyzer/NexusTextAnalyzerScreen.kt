package com.nexuswavetech.nexusplus.features.textanalyzer

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlin.math.roundToInt

private data class TextStats(
    val words        : Int,
    val chars        : Int,
    val charsNoSpace : Int,
    val sentences    : Int,
    val paragraphs   : Int,
    val readingTimeSec: Int,
    val topWords     : List<Pair<String, Int>>,
)

private fun analyze(text: String): TextStats {
    val trimmed    = text.trim()
    val words      = if (trimmed.isEmpty()) 0 else trimmed.split(Regex("\\s+")).size
    val chars      = text.length
    val charsNoSp  = text.count { !it.isWhitespace() }
    val sentences  = if (trimmed.isEmpty()) 0
                     else trimmed.split(Regex("[.!?]+")).count { it.isNotBlank() }
    val paragraphs = if (trimmed.isEmpty()) 0
                     else trimmed.split(Regex("\n\\s*\n")).count { it.isNotBlank() }
    val readSec    = if (words == 0) 0 else (words / 200.0 * 60).roundToInt()

    val stopWords  = setOf("the","a","an","and","or","but","in","on","at","to","for","of","is","it","be","as","was","are","with","by")
    val topWords   = trimmed
        .lowercase()
        .split(Regex("[^a-zA-Z0-9']+"))
        .filter { it.length > 2 && it !in stopWords }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(5)
        .map { it.key to it.value }

    return TextStats(words, chars, charsNoSp, sentences, paragraphs, readSec, topWords)
}

private fun formatReadingTime(seconds: Int): String = when {
    seconds == 0 -> "—"
    seconds < 60 -> "$seconds sec"
    else         -> "${seconds / 60} min ${seconds % 60} sec"
}

@Composable
fun NexusTextAnalyzerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    val stats     = remember(inputText) { analyze(inputText) }
    val scroll    = rememberScrollState()

    Scaffold(
        topBar = {
            NexusTopBar(
                title = "Text Analyzer",
                onBack = onBack,
                actions = {
                    if (inputText.isNotEmpty()) {
                        IconButton(
                            onClick = { inputText = "" },
                            modifier = Modifier.semantics { contentDescription = "Clear text" }
                        ) {
                            Icon(Icons.Filled.Clear, contentDescription = null)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Input area
            OutlinedTextField(
                value         = inputText,
                onValueChange = { inputText = it },
                modifier      = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                placeholder   = { Text("Paste or type your text here…") },
                shape         = RoundedCornerShape(12.dp),
            )

            // Paste / Clear buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = clipboard.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            inputText = clip.getItemAt(0).coerceToText(context).toString()
                        }
                    },
                    modifier = Modifier.semantics { contentDescription = "Paste from clipboard" }
                ) {
                    Icon(Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Paste")
                }
                if (inputText.isNotEmpty()) {
                    OutlinedButton(onClick = { inputText = "" }) {
                        Text("Clear")
                    }
                }
            }

            AnimatedVisibility(visible = inputText.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Primary stats grid
                    Text("Statistics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard("Words",      "${stats.words}",      Icons.Filled.TextFields, Modifier.weight(1f))
                        StatCard("Characters", "${stats.chars}",      Icons.Filled.Notes, Modifier.weight(1f))
                        StatCard("No Spaces",  "${stats.charsNoSpace}", Icons.Filled.SpaceBar, Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard("Sentences",  "${stats.sentences}",  Icons.Filled.FormatQuote, Modifier.weight(1f))
                        StatCard("Paragraphs", "${stats.paragraphs}", Icons.Filled.ViewHeadline, Modifier.weight(1f))
                        StatCard("Read Time",  formatReadingTime(stats.readingTimeSec), Icons.Filled.Schedule, Modifier.weight(1f))
                    }

                    // Top words
                    if (stats.topWords.isNotEmpty()) {
                        Text("Top Words", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                stats.topWords.forEachIndexed { i, (word, count) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        "${i + 1}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                            Text(word, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Text(
                                            "$count×",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Surface(
        shape         = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        modifier      = modifier.semantics { contentDescription = "$label: $value" }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
