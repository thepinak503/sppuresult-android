package pinak.sppunotify.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormulaReferenceScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Technical Reference", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            InfoCard(
                title = "Open Source Transparency",
                content = "This app uses strictly verified conversion rules based on official SPPU circulars. As an open-source project, all calculation logic is publicly auditable in the source code.",
                icon = Icons.Default.Code
            )

            SectionHeader("1. CBCS 2019 Pattern", "Circular No. 332/2020")
            FormulaTable(
                listOf(
                    TableEntry("O", "9.50 – 10.00", "(20 * CGPA) - 100"),
                    TableEntry("A+", "8.25 – 9.49", "(12 * CGPA) - 24"),
                    TableEntry("A", "6.75 – 8.24", "(10 * CGPA) - 7.5"),
                    TableEntry("B+", "5.75 – 6.74", "(5 * CGPA) + 26.25"),
                    TableEntry("B", "5.25 – 5.74", "(10 * CGPA) - 2.5"),
                    TableEntry("C", "4.75 – 5.24", "(10 * CGPA) - 2.5"),
                    TableEntry("D", "4.00 – 4.74", "(6.6 * CGPA) + 13.6")
                )
            )

            SectionHeader("2. NEP 2024 Pattern", "Revised 2025 Handbook")
            FormulaTable(
                listOf(
                    TableEntry("O", "9.50 – 10.00", "(20 * CGPA) - 100"),
                    TableEntry("A+", "8.25 – 9.49", "(12 * CGPA) - 24"),
                    TableEntry("A", "6.75 – 8.24", "(10 * CGPA) - 5"),
                    TableEntry("B+", "5.75 – 6.74", "(12 * CGPA) - 20"),
                    TableEntry("B", "5.25 – 5.74", "(5 * CGPA) + 23.75"),
                    TableEntry("C", "4.75 – 5.24", "(10 * CGPA) - 2.5"),
                    TableEntry("D", "4.00 – 4.74", "(6.6 * CGPA) + 13.6")
                )
            )

            SectionHeader("3. Legacy & Linear Patterns", "Unified Multipliers")
            LegacySection()

            TechnicalNote()

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 2.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    }
}

data class TableEntry(val grade: String, val range: String, val formula: String)

@Composable
private fun FormulaTable(entries: List<TableEntry>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .padding(vertical = 12.dp, horizontal = 12.dp)
            ) {
                Text("Grade", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Text("Range", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Text("Formula", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            }
            
            entries.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(entry.grade, modifier = Modifier.weight(0.5f), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Text(entry.range, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text(
                            entry.formula,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (entry != entries.last()) {
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun LegacySection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LegacyItem("Linear (2015/19)", "(CGPA - 0.75) * 10", "Equivalent to (CGPA * 10) - 7.5. Used for broad estimates.")
        LegacyItem("Circular 322", "CGPA * 8.9", "Official multiplier for professional courses like Engineering.")
        LegacyItem("Legacy Engg", "CGPA * 8.8", "Used in older conversion schemes.")
    }
}

@Composable
private fun LegacyItem(label: String, formula: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                formula,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun InfoCard(title: String, content: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                Text(content, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun TechnicalNote() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Verification Note: A+ Formula", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Many unofficial sources incorrectly list the A+ constant as -25. However, this app uses -24 as verified from official Circular 332/2020 examples where a 9.0 CGPA results in exactly 84%.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
