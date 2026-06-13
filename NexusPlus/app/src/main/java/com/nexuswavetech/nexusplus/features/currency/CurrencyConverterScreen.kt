package com.nexuswavetech.nexusplus.features.currency

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar

private data class Currency(val code: String, val name: String, val symbol: String, val rateToUSD: Double)

// Offline rates (approximate, based on common reference rates)
private val currencies = listOf(
    Currency("USD", "US Dollar",            "$",   1.0),
    Currency("EUR", "Euro",                 "€",   0.92),
    Currency("GBP", "British Pound",        "£",   0.79),
    Currency("INR", "Indian Rupee",         "₹",   83.12),
    Currency("JPY", "Japanese Yen",         "¥",   149.50),
    Currency("CNY", "Chinese Yuan",         "¥",   7.24),
    Currency("AED", "UAE Dirham",           "د.إ", 3.67),
    Currency("SGD", "Singapore Dollar",     "S$",  1.34),
    Currency("CAD", "Canadian Dollar",      "C$",  1.36),
    Currency("AUD", "Australian Dollar",    "A$",  1.53),
    Currency("CHF", "Swiss Franc",          "Fr",  0.90),
    Currency("SAR", "Saudi Riyal",          "﷼",   3.75),
    Currency("MYR", "Malaysian Ringgit",    "RM",  4.71),
    Currency("BRL", "Brazilian Real",       "R$",  4.97),
    Currency("MXN", "Mexican Peso",         "$",   17.15),
    Currency("KRW", "South Korean Won",     "₩",   1325.0),
    Currency("ZAR", "South African Rand",   "R",   18.63),
    Currency("TRY", "Turkish Lira",         "₺",   32.40),
    Currency("THB", "Thai Baht",            "฿",   35.12),
    Currency("IDR", "Indonesian Rupiah",    "Rp",  15750.0),
    Currency("PKR", "Pakistani Rupee",      "₨",   278.50),
    Currency("BDT", "Bangladeshi Taka",     "৳",   110.0),
    Currency("NZD", "New Zealand Dollar",   "NZ$", 1.63),
    Currency("HKD", "Hong Kong Dollar",     "HK$", 7.83),
    Currency("SEK", "Swedish Krona",        "kr",  10.42),
    Currency("NOK", "Norwegian Krone",      "kr",  10.55),
    Currency("DKK", "Danish Krone",         "kr",  6.88),
    Currency("RUB", "Russian Ruble",        "₽",   89.50),
    Currency("EGP", "Egyptian Pound",       "£",   30.90),
    Currency("NGN", "Nigerian Naira",       "₦",   1560.0),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterScreen(onBack: () -> Unit) {
    var fromIndex  by remember { mutableIntStateOf(0) }
    var toIndex    by remember { mutableIntStateOf(3) }
    var inputText  by remember { mutableStateOf("") }

    val fromCur = currencies[fromIndex]
    val toCur   = currencies[toIndex]

    val result: String = remember(inputText, fromIndex, toIndex) {
        val v = inputText.toDoubleOrNull() ?: return@remember ""
        val usd = v / fromCur.rateToUSD
        val out = usd * toCur.rateToUSD
        if (out == out.toLong().toDouble()) out.toLong().toString()
        else "%.4f".format(out).trimEnd('0').trimEnd('.')
    }

    val allRates: List<Pair<Currency, String>> = remember(inputText, fromIndex) {
        val v = inputText.toDoubleOrNull() ?: 1.0
        currencies.map { cur ->
            val usd = v / fromCur.rateToUSD
            val out = usd * cur.rateToUSD
            val str = if (out >= 1.0) "%.2f".format(out) else "%.4f".format(out).trimEnd('0').trimEnd('.')
            cur to str
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Currency Converter", onBack = onBack)

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Disclaimer
            Surface(
                color  = MaterialTheme.colorScheme.tertiaryContainer,
                shape  = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "📋 Offline rates for reference only. Rates may not reflect live market values.",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }

            // Input
            OutlinedTextField(
                value           = inputText,
                onValueChange   = { inputText = it },
                label           = { Text("Amount") },
                prefix          = { Text(fromCur.symbol) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine      = true,
                modifier        = Modifier.fillMaxWidth().semantics { contentDescription = "Amount to convert, in ${fromCur.name}" },
            )

            // From / To selectors
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CurrencyDropdown(
                    label    = "From",
                    selected = fromIndex,
                    onSelect = { fromIndex = it },
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { val t = fromIndex; fromIndex = toIndex; toIndex = t }) {
                    Icon(Icons.Filled.SwapVert, contentDescription = "Swap currencies")
                }
                CurrencyDropdown(
                    label    = "To",
                    selected = toIndex,
                    onSelect = { toIndex = it },
                    modifier = Modifier.weight(1f),
                )
            }

            // Result card
            if (result.isNotBlank()) {
                Card(
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().semantics {
                        contentDescription = "$inputText ${fromCur.code} equals $result ${toCur.code}"
                    },
                ) {
                    Row(
                        modifier          = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Filled.CurrencyExchange, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
                        Column {
                            Text(
                                "${toCur.symbol} $result",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                "${inputText.trim()} ${fromCur.code} = $result ${toCur.code}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        // All rates list
        LazyColumn(
            modifier       = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item {
                Text(
                    "All Rates (from ${fromCur.code})",
                    style    = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color    = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            items(allRates) { (cur, converted) ->
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .semantics { contentDescription = "${cur.name}: ${cur.symbol} $converted" },
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(cur.code, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text(cur.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        "${cur.symbol} $converted",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (cur.code == toCur.code) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdown(
    label: String,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val cur = currencies[selected]
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value         = "${cur.code} – ${cur.name}",
            onValueChange = {},
            readOnly      = true,
            label         = { Text(label) },
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier      = Modifier.menuAnchor().fillMaxWidth(),
            singleLine    = true,
        )
        ExposedDropdownMenu(
            expanded          = expanded,
            onDismissRequest  = { expanded = false },
            modifier          = Modifier.heightIn(max = 320.dp),
        ) {
            currencies.forEachIndexed { i, c ->
                DropdownMenuItem(
                    text    = { Text("${c.code} – ${c.name}") },
                    onClick = { onSelect(i); expanded = false },
                )
            }
        }
    }
}
