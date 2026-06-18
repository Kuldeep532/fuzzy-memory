package com.nexuswavetech.nexusplus.features.expense

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// ── Domain models ─────────────────────────────────────────────────────────────

enum class ExpenseCategory(
    val label: String,
    val icon:  ImageVector,
    val color: Color,
) {
    FOOD         ("Food & Drinks",  Icons.Filled.Restaurant,      Color(0xFFEF5350)),
    TRANSPORT    ("Transport",      Icons.Filled.DirectionsCar,   Color(0xFF42A5F5)),
    SHOPPING     ("Shopping",       Icons.Filled.ShoppingCart,    Color(0xFFAB47BC)),
    BILLS        ("Bills",          Icons.Filled.Receipt,         Color(0xFFFFA726)),
    HEALTH       ("Health",         Icons.Filled.HealthAndSafety, Color(0xFF66BB6A)),
    ENTERTAINMENT("Entertainment",  Icons.Filled.SportsEsports,  Color(0xFF26C6DA)),
    SALARY       ("Salary / Income",Icons.Filled.AccountBalance,  Color(0xFF4CAF50)),
    OTHER        ("Other",          Icons.Filled.Category,        Color(0xFF78909C)),
}

@Serializable
data class Transaction(
    val id:         String  = UUID.randomUUID().toString(),
    val amount:     Double,
    val description:String,
    val category:   String,
    val isExpense:  Boolean,
    val dateMillis: Long    = System.currentTimeMillis(),
)

// ── ViewModel (AndroidViewModel — no Koin needed, standard Android pattern) ───

private val Context.expenseDataStore by preferencesDataStore("nexus_expense_tracker")
private val TRANSACTIONS_KEY         = stringPreferencesKey("transactions_json")
private val DATE_FMT                 = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
private val JSON                     = Json { ignoreUnknownKeys = true; isLenient = true }

class ExpenseTrackerViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx: Context = app.applicationContext

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    val totalBalance: StateFlow<Double> = transactions.map { list ->
        list.sumOf { if (it.isExpense) -it.amount else it.amount }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val totalIncome: StateFlow<Double> = transactions.map { list ->
        list.filter { !it.isExpense }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val totalExpenses: StateFlow<Double> = transactions.map { list ->
        list.filter { it.isExpense }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    init {
        viewModelScope.launch {
            ctx.expenseDataStore.data.collect { prefs ->
                runCatching {
                    _transactions.value =
                        JSON.decodeFromString<List<Transaction>>(prefs[TRANSACTIONS_KEY] ?: "[]")
                }
            }
        }
    }

    fun addTransaction(amount: Double, description: String, category: String, isExpense: Boolean) {
        val tx = Transaction(amount = amount, description = description, category = category, isExpense = isExpense)
        viewModelScope.launch {
            ctx.expenseDataStore.edit { prefs ->
                prefs[TRANSACTIONS_KEY] = JSON.encodeToString(_transactions.value + tx)
            }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            ctx.expenseDataStore.edit { prefs ->
                prefs[TRANSACTIONS_KEY] = JSON.encodeToString(_transactions.value.filterNot { it.id == id })
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusExpenseTrackerScreen(onBack: () -> Unit) {
    // AndroidViewModel is automatically constructed by the default factory — no Koin needed.
    val vm: ExpenseTrackerViewModel = viewModel()

    val transactions by vm.transactions.collectAsState()
    val balance      by vm.totalBalance.collectAsState()
    val income       by vm.totalIncome.collectAsState()
    val expenses     by vm.totalExpenses.collectAsState()

    var showAddSheet  by remember { mutableStateOf(false) }
    var filterExpense by remember { mutableStateOf<Boolean?>(null) }

    val filtered = remember(transactions, filterExpense) {
        when (filterExpense) {
            true  -> transactions.filter { it.isExpense }
            false -> transactions.filter { !it.isExpense }
            null  -> transactions
        }.sortedByDescending { it.dateMillis }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Expense Tracker", onBack = onBack)

        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Summary cards ──────────────────────────────────────────────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier            = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                "Net Balance",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            )
                            Text(
                                "₹ %,.2f".format(balance),
                                style    = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                color    = if (balance >= 0) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.error,
                                modifier = Modifier.semantics {
                                    contentDescription =
                                        "Net balance ₹%.2f".format(balance)
                                },
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryChip(label = "Income",   amount = income,   color = Color(0xFF4CAF50), icon = Icons.Filled.ArrowUpward,   modifier = Modifier.weight(1f))
                        SummaryChip(label = "Expenses", amount = expenses, color = MaterialTheme.colorScheme.error, icon = Icons.Filled.ArrowDownward, modifier = Modifier.weight(1f))
                    }
                }
            }

            // ── Filter chips ───────────────────────────────────────────────
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = filterExpense == null,  onClick = { filterExpense = null  }, label = { Text("All (${transactions.size})") })
                    FilterChip(
                        selected    = filterExpense == false,
                        onClick     = { filterExpense = false },
                        label       = { Text("Income") },
                        leadingIcon = { Icon(Icons.Filled.ArrowUpward, null, modifier = Modifier.size(16.dp), tint = Color(0xFF4CAF50)) },
                    )
                    FilterChip(
                        selected    = filterExpense == true,
                        onClick     = { filterExpense = true },
                        label       = { Text("Expenses") },
                        leadingIcon = { Icon(Icons.Filled.ArrowDownward, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) },
                    )
                }
            }

            // ── Add button ─────────────────────────────────────────────────
            item {
                Button(
                    onClick  = { showAddSheet = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Transaction")
                }
            }

            // ── Empty state ────────────────────────────────────────────────
            if (filtered.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.AccountBalanceWallet, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            Text("No transactions yet", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Tap 'Add Transaction' to record income or expenses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { tx ->
                    TransactionCard(transaction = tx, onDelete = { vm.deleteTransaction(tx.id) })
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showAddSheet) {
        AddTransactionSheet(
            onDismiss = { showAddSheet = false },
            onSave    = { amount, desc, cat, isExpense ->
                vm.addTransaction(amount, desc, cat, isExpense)
                showAddSheet = false
            },
        )
    }
}

// ── Summary chip ──────────────────────────────────────────────────────────────

@Composable
private fun SummaryChip(label: String, amount: Double, color: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("₹ %,.2f".format(amount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = color)
            }
        }
    }
}

// ── Transaction card ──────────────────────────────────────────────────────────

@Composable
private fun TransactionCard(transaction: Transaction, onDelete: () -> Unit) {
    val cat = ExpenseCategory.entries.firstOrNull { it.name == transaction.category } ?: ExpenseCategory.OTHER
    Card(
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = "${if (transaction.isExpense) "Expense" else "Income"}: ${transaction.description} ₹${transaction.amount}"
        },
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = MaterialTheme.shapes.small, color = cat.color.copy(alpha = 0.15f), modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(cat.icon, null, tint = cat.color, modifier = Modifier.size(22.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(transaction.description, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                Text(cat.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(DATE_FMT.format(Date(transaction.dateMillis)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${if (transaction.isExpense) "−" else "+"} ₹%,.2f".format(transaction.amount),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (transaction.isExpense) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete ${transaction.description}", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Add transaction bottom sheet ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionSheet(
    onDismiss: () -> Unit,
    onSave:    (amount: Double, desc: String, cat: String, isExpense: Boolean) -> Unit,
) {
    var isExpense   by remember { mutableStateOf(true) }
    var amountText  by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category    by remember { mutableStateOf(ExpenseCategory.FOOD) }
    var catExpanded by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var descError   by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Add Transaction", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(true to "Expense", false to "Income").forEach { (exp, label) ->
                    FilterChip(
                        selected    = isExpense == exp,
                        onClick     = {
                            isExpense = exp
                            category  = if (exp) ExpenseCategory.FOOD else ExpenseCategory.SALARY
                        },
                        label       = { Text(label) },
                        leadingIcon = {
                            Icon(
                                if (exp) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint     = if (exp) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            OutlinedTextField(
                value           = amountText,
                onValueChange   = { amountText = it.filter { c -> c.isDigit() || c == '.' }; amountError = false },
                label           = { Text("Amount (₹)") },
                leadingIcon     = { Icon(Icons.Filled.CurrencyRupee, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine      = true,
                isError         = amountError,
                supportingText  = if (amountError) ({ Text("Enter a valid amount greater than 0") }) else null,
                modifier        = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value         = description,
                onValueChange = { description = it; descError = false },
                label         = { Text("Description") },
                singleLine    = true,
                isError       = descError,
                supportingText = if (descError) ({ Text("Description cannot be empty") }) else null,
                modifier      = Modifier.fillMaxWidth(),
            )

            ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                OutlinedTextField(
                    value         = category.label,
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text("Category") },
                    leadingIcon   = { Icon(category.icon, null, tint = category.color) },
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
                    modifier      = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                )
                ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                    val choices = if (isExpense)
                        ExpenseCategory.entries.filter { it != ExpenseCategory.SALARY }
                    else
                        listOf(ExpenseCategory.SALARY, ExpenseCategory.OTHER)
                    choices.forEach { cat ->
                        DropdownMenuItem(
                            text        = { Text(cat.label) },
                            leadingIcon = { Icon(cat.icon, null, tint = cat.color) },
                            onClick     = { category = cat; catExpanded = false },
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    amountError = amt == null || amt <= 0.0
                    descError   = description.isBlank()
                    if (!amountError && !descError && amt != null) {
                        onSave(amt, description.trim(), category.name, isExpense)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Save Transaction") }
        }
    }
}
