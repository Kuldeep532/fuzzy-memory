package com.nexuswavetech.nexusplus.news

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

// ── ViewModel ─────────────────────────────────────────────────────────────────

class NewsViewModel(
    private val service: NewsService = NewsService(),
) : ViewModel() {

    private val _articles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val articles: StateFlow<List<NewsArticle>> = _articles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedCategory = MutableStateFlow("World")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val categories: List<String> = NewsFeedRegistry.categories

    init { loadCategory("World") }

    fun loadCategory(category: String) {
        _selectedCategory.value = category
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            val result = service.fetchCategory(category)
            _isLoading.value = false
            if (result.isSuccess) {
                _articles.value = result.getOrNull() ?: emptyList()
            } else {
                _error.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            loadCategory(_selectedCategory.value)
            return
        }
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            val result = service.search(query)
            _isLoading.value = false
            if (result.isSuccess) {
                _articles.value = result.getOrNull() ?: emptyList()
            } else {
                _error.value = result.exceptionOrNull()?.message
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun NewsScreen(
    onBack: () -> Unit,
    viewModel: NewsViewModel = koinInject(),
) {
    val articles by viewModel.articles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            com.nexuswavetech.nexusplus.ui.components.NexusTopBar(
                title = "Nexus News",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Search
            if (showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.search(it)
                    },
                    label = { Text("Search news") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; viewModel.search("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                )
            }

            // Category chips
            ScrollableCategoryChips(
                categories = viewModel.categories,
                selected = selectedCategory,
                onSelect = { viewModel.loadCategory(it) },
            )

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    error != null -> ErrorMessage(error!!, onRetry = { viewModel.loadCategory(selectedCategory) })
                    articles.isEmpty() -> EmptyNews()
                    else -> LazyColumn {
                        items(articles) { article ->
                            ArticleCard(article)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScrollableCategoryChips(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { cat ->
            FilterChip(
                selected = cat == selected,
                onClick = { onSelect(cat) },
                label = { Text(cat) },
            )
        }
    }
}

@Composable
private fun ArticleCard(article: NewsArticle) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = { /* open article URL */ },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    article.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    article.source,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                article.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (article.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    article.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                article.publishedAt,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun ErrorMessage(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        FilledTonalButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun EmptyNews() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Newspaper, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(8.dp))
        Text("No articles found", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
