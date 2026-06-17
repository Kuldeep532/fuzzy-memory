package com.nexuswavetech.nexusplus.science

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

// ── ViewModel ─────────────────────────────────────────────────────────────────

class ScienceViewModel(
    private val service: ScienceService = ScienceService(),
) : ViewModel() {

    private val _apod = MutableStateFlow<ApodResult?>(null)
    val apod: StateFlow<ApodResult?> = _apod.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val facts: List<ScienceFact> = service.scienceFacts()
    val quiz: List<ScienceQuizQuestion> = service.quizQuestions()

    fun loadApod() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            val result = service.fetchApod()
            _isLoading.value = false
            if (result.isSuccess) {
                _apod.value = result.getOrNull()
            } else {
                _error.value = result.exceptionOrNull()?.message
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun ScienceScreen(
    onBack: () -> Unit,
    viewModel: ScienceViewModel = koinInject(),
) {
    val apod by viewModel.apod.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Space", "Facts", "Quiz")

    Scaffold(
        topBar = {
            com.nexuswavetech.nexusplus.ui.components.NexusTopBar(
                title = "Nexus Science",
                onBack = onBack,
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }

            when (selectedTab) {
                0 -> SpaceTab(viewModel, apod, isLoading, error)
                1 -> FactsTab(viewModel.facts)
                2 -> QuizTab(viewModel.quiz)
            }
        }
    }
}

@Composable
private fun SpaceTab(viewModel: ScienceViewModel, apod: ApodResult?, isLoading: Boolean, error: String?) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (apod == null && !isLoading) {
            Button(onClick = { viewModel.loadApod() }) {
                Icon(Icons.Filled.RocketLaunch, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Load NASA APOD")
            }
        }

        if (isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        apod?.let { result ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(result.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(result.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(8.dp))
                    Text(result.explanation, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun FactsTab(facts: List<ScienceFact>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(facts) { fact ->
            FactCard(fact)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FactCard(fact: ScienceFact) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(fact.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(fact.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(fact.fact, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun QuizTab(questions: List<ScienceQuizQuestion>) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableIntStateOf(-1) }
    var showResult by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }

    if (currentIndex >= questions.size) {
        QuizComplete(score, questions.size)
        return
    }

    val question = questions[currentIndex]

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Question ${currentIndex + 1} / ${questions.size}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(8.dp))
        Text(question.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        question.options.forEachIndexed { index, option ->
            val color = when {
                !showResult -> MaterialTheme.colorScheme.surface
                index == question.correctIndex -> MaterialTheme.colorScheme.primaryContainer
                index == selectedAnswer && index != question.correctIndex -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surface
            }
            Card(
                onClick = {
                    if (!showResult) {
                        selectedAnswer = index
                        showResult = true
                        if (index == question.correctIndex) score++
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = color),
            ) {
                Text(option, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }

        if (showResult) {
            Spacer(Modifier.height(8.dp))
            Text(
                question.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    currentIndex++
                    selectedAnswer = -1
                    showResult = false
                },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Next")
            }
        }
    }
}

@Composable
private fun QuizComplete(score: Int, total: Int) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Quiz Complete!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("You scored $score / $total", style = MaterialTheme.typography.titleLarge)
    }
}
