package com.nexuswavetech.nexusplus.features.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexuswavetech.nexusplus.core.HapticHelper
import com.nexuswavetech.nexusplus.core.SettingsRepository
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.*

enum class CalcTab(val label: String) { STANDARD("Standard"), AGE("Age Calculator") }

data class CalcUiState(
    val tab: CalcTab = CalcTab.STANDARD,
    val display: String = "0",
    val expression: String = "",
    val birthDateText: String = "",
    val ageResult: String = ""
)

class CalculatorCenterViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CalcUiState())
    val uiState: StateFlow<CalcUiState> = _uiState.asStateFlow()

    fun setTab(tab: CalcTab) = _uiState.update { it.copy(tab = tab) }
    fun setBirthDate(v: String) = _uiState.update { it.copy(birthDateText = v) }

    fun onButton(symbol: String) {
        val s = _uiState.value
        when (symbol) {
            "C"  -> _uiState.update { it.copy(display = "0", expression = "") }
            "⌫"  -> {
                val e = s.expression.dropLast(1)
                _uiState.update { it.copy(expression = e, display = e.ifBlank { "0" }) }
            }
            "="  -> {
                runCatching {
                    val result = evalExpression(s.expression)
                    val fmt = if (result == result.toLong().toDouble()) result.toLong().toString() else "%.8g".format(result).trimEnd('0').trimEnd('.')
                    _uiState.update { it.copy(display = fmt, expression = fmt) }
                }.onFailure { _uiState.update { it.copy(display = "Error") } }
            }
            else -> {
                val newExpr = if (s.expression == "0" || s.expression == "Error") {
                    if (symbol in listOf("+", "-", "×", "÷", ".")) "0$symbol" else symbol
                } else {
                    s.expression + symbol
                }
                _uiState.update { it.copy(expression = newExpr, display = newExpr) }
            }
        }
    }

    fun calculateAge() {
        val s = _uiState.value
        runCatching {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dob = sdf.parse(s.birthDateText) ?: error("Invalid date")
            val today = Calendar.getInstance()
            val birth = Calendar.getInstance().apply { time = dob }
            var years = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
            var months = today.get(Calendar.MONTH) - birth.get(Calendar.MONTH)
            var days = today.get(Calendar.DAY_OF_MONTH) - birth.get(Calendar.DAY_OF_MONTH)
            if (days < 0) { months--; days += birth.getActualMaximum(Calendar.DAY_OF_MONTH) }
            if (months < 0) { years--; months += 12 }
            _uiState.update { it.copy(ageResult = "$years years, $months months, $days days") }
        }.onFailure { _uiState.update { it.copy(ageResult = "Invalid date. Use DD/MM/YYYY") } }
    }

    private fun evalExpression(expr: String): Double {
        val e = expr.replace("×", "*").replace("÷", "/")
        return object : Any() {
            var pos = -1; var ch = ' '
            fun nextChar() { ch = if (++pos < e.length) e[pos] else (-1).toChar() }
            fun eat(c: Char): Boolean { while (ch == ' ') nextChar(); return if (ch == c) { nextChar(); true } else false }
            fun parse(): Double { nextChar(); val v = parseExpr(); if (pos < e.length) error("Unexpected: $ch"); return v }
            fun parseExpr(): Double {
                var v = parseTerm()
                while (true) { v = when { eat('+') -> v + parseTerm(); eat('-') -> v - parseTerm(); else -> return v } }
            }
            fun parseTerm(): Double {
                var v = parseFactor()
                while (true) { v = when { eat('*') -> v * parseFactor(); eat('/') -> v / parseFactor(); else -> return v } }
            }
            fun parseFactor(): Double {
                if (eat('+')) return parseFactor()
                if (eat('-')) return -parseFactor()
                var v: Double
                val start = pos
                if (eat('(')) { v = parseExpr(); eat(')') } else {
                    while (ch in '0'..'9' || ch == '.') nextChar()
                    v = e.substring(start, pos).toDouble()
                }
                return v
            }
        }.parse()
    }
}

@Composable
fun CalculatorCenterScreen(
    onBack: () -> Unit,
    viewModel: CalculatorCenterViewModel = koinViewModel()
) {
    val uiState  by viewModel.uiState.collectAsState()
    val view     = LocalView.current
    val haptic   = koinInject<HapticHelper>()
    val settings = koinInject<SettingsRepository>()
    val touchVib by settings.touchVibration.collectAsState(initial = true)

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Calculator Center", onBack = onBack)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CalcTab.entries.forEach { tab ->
                FilterChip(
                    selected = uiState.tab == tab,
                    onClick = { viewModel.setTab(tab) },
                    label = { Text(tab.label) }
                )
            }
        }

        when (uiState.tab) {
            CalcTab.STANDARD -> StandardCalc(uiState, viewModel, haptic, touchVib)
            CalcTab.AGE      -> AgeCalc(uiState, viewModel, haptic, touchVib)
        }
    }
}

@Composable
private fun StandardCalc(
    uiState: CalcUiState,
    viewModel: CalculatorCenterViewModel,
    haptic: HapticHelper,
    touchVib: Boolean,
) {
    val view = LocalView.current
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        // Display
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = uiState.display,
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Light),
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .semantics { contentDescription = "Display: ${uiState.display}" },
                maxLines = 2
            )
        }

        val buttons = listOf(
            listOf("C", "⌫", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf(".", "0", "=", "=")
        )
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val rowSet = row.toMutableList()
                if (rowSet.count { it == "=" } > 1) {
                    rowSet.removeAt(rowSet.lastIndexOf("="))
                    rowSet.forEachIndexed { idx, sym ->
                        CalcButton(sym, modifier = Modifier.weight(1f)) {
                            haptic.click(view, touchVib)
                            viewModel.onButton(sym)
                            view.announceForAccessibility(sym)
                        }
                    }
                } else {
                    row.forEach { sym ->
                        CalcButton(sym, modifier = Modifier.weight(1f)) {
                            haptic.click(view, touchVib)
                            viewModel.onButton(sym)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalcButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val isOp = label in listOf("+", "-", "×", "÷", "=", "C", "⌫", "%")
    Button(
        onClick = onClick,
        modifier = modifier
            .height(60.dp)
            .semantics { contentDescription = label },
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                label == "=" -> MaterialTheme.colorScheme.primary
                label == "C" -> MaterialTheme.colorScheme.error
                isOp -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = when {
                label == "=" -> MaterialTheme.colorScheme.onPrimary
                label == "C" -> MaterialTheme.colorScheme.onError
                isOp -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    ) {
        Text(label, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AgeCalc(
    uiState: CalcUiState,
    viewModel: CalculatorCenterViewModel,
    haptic: HapticHelper,
    touchVib: Boolean,
) {
    val view = LocalView.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Cake, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Age Calculator", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        OutlinedTextField(
            value = uiState.birthDateText,
            onValueChange = viewModel::setBirthDate,
            label = { Text("Date of Birth") },
            placeholder = { Text("DD/MM/YYYY") },
            leadingIcon = { Icon(Icons.Filled.CalendarToday, null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Date of birth input in DD/MM/YYYY format" }
        )
        Button(
            onClick = {
                haptic.confirm(view, touchVib)
                viewModel.calculateAge()
                view.announceForAccessibility("Calculating age")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Calculate, null)
            Spacer(Modifier.width(8.dp))
            Text("Calculate Age")
        }
        if (uiState.ageResult.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Age result: ${uiState.ageResult}" }
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Your Age", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        uiState.ageResult,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
