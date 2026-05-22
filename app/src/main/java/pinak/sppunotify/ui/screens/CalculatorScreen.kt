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
import pinak.sppunotify.ui.components.AppTopBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Locale

enum class CalculationPattern(val label: String) {
    STANDARD("Standard (Grade - 0.75) * 10"),
    CIRCULAR_322("Circular 322/2020 (GPA * 8.9)"),
    ENGINEERING_2022("Engineering 2022 (GPA * 8.8)"),
    RANGE_BASED("Detailed Range-Based (Circular 332)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(onMenuClick: () -> Unit) {
    var gpaText by remember { mutableStateOf("") }
    var selectedPattern by remember { mutableStateOf(CalculationPattern.STANDARD) }
    var expanded by remember { mutableStateOf(false) }

    val gpa = gpaText.toDoubleOrNull() ?: 0.0

    val percentage = when (selectedPattern) {
        CalculationPattern.STANDARD -> if (gpa > 0) (gpa - 0.75) * 10 else 0.0
        CalculationPattern.CIRCULAR_322 -> gpa * 8.9
        CalculationPattern.ENGINEERING_2022 -> gpa * 8.8
        CalculationPattern.RANGE_BASED -> when {
            gpa >= 9.5 -> (20 * gpa) - 100
            gpa >= 8.25 -> (12 * gpa) - 25
            gpa >= 6.75 -> (10 * gpa) - 7.5
            gpa >= 5.75 -> (5 * gpa) + 26.25
            gpa >= 5.25 -> (10 * gpa) - 2.5
            gpa >= 4.75 -> (10 * gpa) - 7.5
            gpa >= 4.0 -> (6.6 * gpa) + 13.6
            else -> 0.0
        }
    }

    val gradeClass = when {
        gpa >= 7.75 -> "First Class with Distinction"
        gpa >= 6.75 -> "First Class"
        gpa >= 6.25 -> "Higher Second Class"
        gpa >= 5.5 -> "Second Class"
        gpa >= 4.0 -> "Pass Class"
        else -> "N/A"
    }

    val gradeLetter = when {
        gpa >= 9.0 -> "O"
        gpa >= 8.0 -> "A+"
        gpa >= 7.0 -> "A"
        gpa >= 6.0 -> "B+"
        gpa >= 5.5 -> "B"
        gpa >= 5.0 -> "C"
        gpa >= 4.0 -> "P"
        else -> "F"
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Calculator",
                navIcon = Icons.Default.Menu,
                onNavClick = onMenuClick
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
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
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
                onValueChange = { if (it.length <= 5) gpaText = it },
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
                            text = String.format(Locale.getDefault(), "%.2f%%", percentage),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ResultInfoItem("Grade", gradeLetter)
                            ResultInfoItem("Class", gradeClass)
                        }
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
                            text = when(selectedPattern) {
                                CalculationPattern.STANDARD -> "This is the most common formula used for 2015 and 2019 patterns."
                                CalculationPattern.RANGE_BASED -> "This follows Circular 332-2020 which provides specific formulas for different GPA ranges."
                                else -> "Formula based on University Circular 322/2020."
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                Text(
                    "Note: Formulas are based on SPPU Circulars 322/2020 and 332-2020.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
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
