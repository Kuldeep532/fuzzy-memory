package com.nexuswavetech.nexusplus.features.units

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
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
import kotlin.math.pow

private data class UnitDef(val name: String, val toBase: Double, val suffix: String)
private data class Category(val name: String, val baseName: String, val units: List<UnitDef>)

private val categories = listOf(
    Category("Length", "Metres", listOf(
        UnitDef("Millimetre", 0.001, "mm"),
        UnitDef("Centimetre", 0.01, "cm"),
        UnitDef("Metre", 1.0, "m"),
        UnitDef("Kilometre", 1000.0, "km"),
        UnitDef("Inch", 0.0254, "in"),
        UnitDef("Foot", 0.3048, "ft"),
        UnitDef("Yard", 0.9144, "yd"),
        UnitDef("Mile", 1609.344, "mi"),
        UnitDef("Nautical Mile", 1852.0, "nmi"),
    )),
    Category("Weight / Mass", "Kilograms", listOf(
        UnitDef("Milligram", 0.000001, "mg"),
        UnitDef("Gram", 0.001, "g"),
        UnitDef("Kilogram", 1.0, "kg"),
        UnitDef("Metric Ton", 1000.0, "t"),
        UnitDef("Ounce", 0.028349523, "oz"),
        UnitDef("Pound", 0.45359237, "lb"),
        UnitDef("Stone", 6.35029318, "st"),
    )),
    Category("Temperature", "°C", listOf(
        UnitDef("Celsius", 1.0, "°C"),
        UnitDef("Fahrenheit", 1.0, "°F"),
        UnitDef("Kelvin", 1.0, "K"),
    )),
    Category("Area", "m²", listOf(
        UnitDef("Square Millimetre", 1e-6, "mm²"),
        UnitDef("Square Centimetre", 0.0001, "cm²"),
        UnitDef("Square Metre", 1.0, "m²"),
        UnitDef("Hectare", 10000.0, "ha"),
        UnitDef("Square Kilometre", 1e6, "km²"),
        UnitDef("Square Inch", 0.00064516, "in²"),
        UnitDef("Square Foot", 0.09290304, "ft²"),
        UnitDef("Acre", 4046.8564224, "ac"),
        UnitDef("Square Mile", 2589988.110336, "mi²"),
    )),
    Category("Volume", "Litres", listOf(
        UnitDef("Millilitre", 0.001, "mL"),
        UnitDef("Litre", 1.0, "L"),
        UnitDef("Cubic Metre", 1000.0, "m³"),
        UnitDef("Fluid Ounce (US)", 0.02957353, "fl oz"),
        UnitDef("Cup (US)", 0.2365882, "cup"),
        UnitDef("Pint (US)", 0.4731765, "pt"),
        UnitDef("Gallon (US)", 3.7854118, "gal"),
        UnitDef("Tablespoon", 0.01478676, "tbsp"),
        UnitDef("Teaspoon", 0.004928922, "tsp"),
    )),
    Category("Speed", "m/s", listOf(
        UnitDef("m/s", 1.0, "m/s"),
        UnitDef("km/h", 1.0 / 3.6, "km/h"),
        UnitDef("mph", 0.44704, "mph"),
        UnitDef("Knot", 0.514444, "kn"),
        UnitDef("Mach", 343.0, "Ma"),
    )),
    Category("Data Storage", "Bytes", listOf(
        UnitDef("Byte", 1.0, "B"),
        UnitDef("Kilobyte", 1024.0, "KB"),
        UnitDef("Megabyte", 1024.0.pow(2), "MB"),
        UnitDef("Gigabyte", 1024.0.pow(3), "GB"),
        UnitDef("Terabyte", 1024.0.pow(4), "TB"),
        UnitDef("Petabyte", 1024.0.pow(5), "PB"),
        UnitDef("Bit", 0.125, "bit"),
        UnitDef("Kilobit", 125.0, "Kbit"),
        UnitDef("Megabit", 125_000.0, "Mbit"),
    )),
    Category("Pressure", "Pascal", listOf(
        UnitDef("Pascal", 1.0, "Pa"),
        UnitDef("Kilopascal", 1000.0, "kPa"),
        UnitDef("Bar", 100_000.0, "bar"),
        UnitDef("PSI", 6894.757, "psi"),
        UnitDef("Atmosphere", 101_325.0, "atm"),
        UnitDef("mmHg (Torr)", 133.3224, "mmHg"),
    )),
)

private fun convert(value: Double, from: UnitDef, to: UnitDef, catName: String): Double {
    if (catName == "Temperature") {
        val celsius = when (from.name) {
            "Fahrenheit" -> (value - 32) * 5.0 / 9.0
            "Kelvin"     -> value - 273.15
            else         -> value
        }
        return when (to.name) {
            "Fahrenheit" -> celsius * 9.0 / 5.0 + 32
            "Kelvin"     -> celsius + 273.15
            else         -> celsius
        }
    }
    return value * from.toBase / to.toBase
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterScreen(onBack: () -> Unit) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var fromIndex by remember { mutableIntStateOf(0) }
    var toIndex   by remember { mutableIntStateOf(1) }
    var inputText by remember { mutableStateOf("") }

    val category = categories[selectedCategoryIndex]

    LaunchedEffect(selectedCategoryIndex) {
        fromIndex = 0
        toIndex   = if (category.units.size > 1) 1 else 0
        inputText = ""
    }

    val result: String = remember(inputText, fromIndex, toIndex, selectedCategoryIndex) {
        val v = inputText.toDoubleOrNull()
        if (v == null) ""
        else {
            val r = convert(v, category.units[fromIndex], category.units[toIndex], category.name)
            if (r == r.toLong().toDouble()) r.toLong().toString()
            else "%.6g".format(r).trimEnd('0').trimEnd('.')
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Unit Converter", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Category selector
            Text("Category", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value            = category.name,
                    onValueChange    = {},
                    readOnly         = true,
                    label            = { Text("Category") },
                    trailingIcon     = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier         = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categories.forEachIndexed { i, cat ->
                        DropdownMenuItem(
                            text    = { Text(cat.name) },
                            onClick = { selectedCategoryIndex = i; expanded = false },
                        )
                    }
                }
            }

            // From / To rows
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // From unit
                UnitDropdown(
                    label   = "From",
                    units   = category.units,
                    selected = fromIndex,
                    onSelect = { fromIndex = it },
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    val tmp = fromIndex; fromIndex = toIndex; toIndex = tmp
                }) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = "Swap units")
                }
                // To unit
                UnitDropdown(
                    label    = "To",
                    units    = category.units,
                    selected = toIndex,
                    onSelect = { toIndex = it },
                    modifier = Modifier.weight(1f),
                )
            }

            // Input
            OutlinedTextField(
                value         = inputText,
                onValueChange = { inputText = it },
                label         = { Text("Enter value") },
                suffix        = { Text(category.units.getOrNull(fromIndex)?.suffix ?: "") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine    = true,
                modifier      = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Input value for unit conversion" },
            )

            // Result
            Card(
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = if (result.isBlank()) "Result will appear here"
                        else "Result: $result ${category.units.getOrNull(toIndex)?.suffix ?: ""}"
                    },
            ) {
                Row(
                    modifier          = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text  = if (result.isBlank()) "—" else result,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        if (result.isNotBlank()) {
                            Text(
                                text  = category.units.getOrNull(toIndex)?.name ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            )
                        }
                    }
                    if (result.isNotBlank()) {
                        Text(
                            text  = category.units.getOrNull(toIndex)?.suffix ?: "",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            // Formula hint
            if (result.isNotBlank() && inputText.isNotBlank()) {
                Text(
                    text  = "${inputText.trim()} ${category.units.getOrNull(fromIndex)?.suffix} = $result ${category.units.getOrNull(toIndex)?.suffix}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdown(
    label: String,
    units: List<UnitDef>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value         = units.getOrNull(selected)?.name ?: "",
            onValueChange = {},
            readOnly      = true,
            label         = { Text(label) },
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier      = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            units.forEachIndexed { i, unit ->
                DropdownMenuItem(
                    text    = { Text("${unit.name} (${unit.suffix})") },
                    onClick = { onSelect(i); expanded = false },
                )
            }
        }
    }
}
