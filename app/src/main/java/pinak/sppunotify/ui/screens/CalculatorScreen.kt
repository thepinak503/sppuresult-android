package pinak.sppunotify.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLocale
import java.util.Locale

enum class CalculationPattern(val label: String, val reference: String) {
    CBCS_2019("2019 Pattern (Circular 332/2020)", "Official piecewise formula from Circular No. 332/2020."),
    NEP_2024("2024 NEP Pattern (Revised 2025)", "Based on 2024 Credit Framework and 2025 Revised Handbook."),
    ENGINEERING_LINEAR("Linear (CGPA - 0.75) * 10", "Common simplified linear formula used for quick reference."),
    LEGACY_8_8("Legacy Engineering (CGPA * 8.8)", "Older engineering conversion scheme."),
    LEGACY_8_9("Legacy / Circular 322 (CGPA * 8.9)", "Unified multiplier from Circular 322/2020.")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    onMenuClick: () -> Unit,
    onReferenceClick: () -> Unit
) {
    var gpaText by remember { mutableStateOf("") }
    var selectedPattern by remember { mutableStateOf(CalculationPattern.CBCS_2019) }
    var expanded by remember { mutableStateOf(value = false) }

    val gpa = gpaText.toDoubleOrNull() ?: 0.0

    var formulaDisplay = ""
    val percentage = when (selectedPattern) {
        CalculationPattern.CBCS_2019 -> when {
            gpa >= 9.5 -> { formulaDisplay = "(20 * $gpa) - 100"; (20 * gpa) - 100 }
            gpa >= 8.25 -> { formulaDisplay = "(12 * $gpa) - 24"; (12 * gpa) - 24 }
            gpa >= 6.75 -> { formulaDisplay = "(10 * $gpa) - 7.5"; (10 * gpa) - 7.5 }
            gpa >= 5.75 -> { formulaDisplay = "(5 * $gpa) + 26.25"; (5 * gpa) + 26.25 }
            gpa >= 5.25 -> { formulaDisplay = "(10 * $gpa) - 2.5"; (10 * gpa) - 2.5 }
            gpa >= 4.75 -> { formulaDisplay = "(10 * $gpa) - 2.5"; (10 * gpa) - 2.5 }
            gpa >= 4.0 -> { formulaDisplay = "(6.6 * $gpa) + 13.6"; (6.6 * gpa) + 13.6 }
            else -> 0.0
        }
        CalculationPattern.NEP_2024 -> when {
            gpa >= 9.5 -> { formulaDisplay = "(20 * $gpa) - 100"; (20 * gpa) - 100 }
            gpa >= 8.25 -> { formulaDisplay = "(12 * $gpa) - 24"; (12 * gpa) - 24 }
            gpa >= 6.75 -> { formulaDisplay = "(10 * $gpa) - 5"; (10 * gpa) - 5 }
            gpa >= 5.75 -> { formulaDisplay = "(12 * $gpa) - 20"; (12 * gpa) - 20 }
            gpa >= 5.25 -> { formulaDisplay = "(5 * $gpa) + 23.75"; (5 * gpa) + 23.75 }
            gpa >= 4.75 -> { formulaDisplay = "(10 * $gpa) - 2.5"; (10 * gpa) - 2.5 }
            gpa >= 4.0 -> { formulaDisplay = "(6.6 * $gpa) + 13.6"; (6.6 * gpa) + 13.6 }
            else -> 0.0
        }
        CalculationPattern.ENGINEERING_LINEAR -> {
            formulaDisplay = if (gpa > 0) "($gpa - 0.75) * 10" else ""
            if (gpa > 0) (gpa - 0.75) * 10 else 0.0
        }
        CalculationPattern.LEGACY_8_8 -> {
            formulaDisplay = "$gpa * 8.8"
            gpa * 8.8
        }
        CalculationPattern.LEGACY_8_9 -> {
            formulaDisplay = "$gpa * 8.9"
            gpa * 8.9
        }
    }

    val gradeLetter = when {
        gpa >= 9.5 -> "O"
        gpa >= 8.25 -> "A+"
        gpa >= 6.75 -> "A"
        gpa >= 5.75 -> "B+"
        gpa >= 5.25 -> "B"
        gpa >= 4.75 -> "C"
        gpa >= 4.0 -> "D"
        else -> "F"
    }

    val classOfDegree = when {
        gpa >= 9.5 -> "Outstanding"
        gpa >= 8.25 -> "Excellent"
        gpa >= 6.75 -> "Very Good"
        gpa >= 5.75 -> "Good"
        gpa >= 5.25 -> "Above Average"
        gpa >= 4.75 -> "Average"
        gpa >= 4.0 -> "Pass"
        else -> "Fail"
    }

    val legacyClass = when {
        gpa >= 7.75 -> "First Class with Distinction"
        gpa >= 6.75 -> "First Class"
        gpa >= 6.25 -> "Higher Second Class"
        gpa >= 5.5 -> "Second Class"
        gpa >= 4.0 -> "Pass Class"
        else -> "N/A"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calculator", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = onReferenceClick) {
                        Icon(Icons.Default.Info, contentDescription = "Formula Reference")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                Icons.Default.Calculate,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                "SGPA / CGPA Converter",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedPattern.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Conversion Pattern") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    CalculationPattern.entries.forEach { pattern ->
                        DropdownMenuItem(
                            text = { Text(pattern.label) },
                            onClick = {
                                selectedPattern = pattern
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = gpaText,
                onValueChange = { newVal ->
                    // Allow valid decimal numbers up to 4 chars + decimal point
                    if ((newVal.length <= 5) && (newVal.isEmpty() || newVal.matches(Regex("^\\d*\\.?\\d{0,2}$")))) {
                        gpaText = newVal
                    }
                },
                label = { Text("Enter Pointer (SGPA/CGPA)") },
                placeholder = { Text("e.g. 8.54") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Calculate, contentDescription = null) }
            )

            if (gpa > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = String.format(LocalLocale.current.platformLocale, "%.2f%%", percentage),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        if (formulaDisplay.isNotEmpty()) {
                            Text(
                                text = "Formula: $formulaDisplay",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ResultInfoItem("Grade", gradeLetter)
                            ResultInfoItem("Degree Class", classOfDegree)
                        }
                        
                        Text(
                            text = "Internal: $legacyClass",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = selectedPattern.reference,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                Text(
                    "Note: Formulas are verified from SPPU Circular 332/2020 and 2025 NEP Handbook.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = onReferenceClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("View Full Formula Reference & Circulars", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ResultInfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}
