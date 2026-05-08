package pinak.sppunotify.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

data class SppuLink(val title: String, val url: String, val category: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinksScreen(onBackClick: () -> Unit, isTopLevel: Boolean = false, scrollState: LazyListState) {
    val context = LocalContext.current
    val links = listOf(
        SppuLink("SPPU Main Website", "http://unipune.ac.in/", "Main"),
        SppuLink("SPPU Results Link 1", "http://onlineresults.unipune.ac.in/SPPU", "Results"),
        SppuLink("SPPU Results Link 2", "http://onlineresults.unipune.ac.in/Result/Dashboard/Default", "Results"),
        SppuLink("Exam Circulars", "http://collegecirculars.unipune.ac.in/sites/examdocs/Time%20Tables%20APRMAY%202026/Forms/AllItems.aspx", "Exam"),
        SppuLink("Syllabus 2026", "http://collegecirculars.unipune.ac.in/sites/documents/Syllabus%202026/Forms/AllItems.aspx", "Syllabus"),
        SppuLink("Syllabus 2025", "http://collegecirculars.unipune.ac.in/sites/documents/Syllabus%202025/Forms/AllItems.aspx", "Syllabus"),
        SppuLink("Syllabus 2024", "http://collegecirculars.unipune.ac.in/sites/documents/Syllabus%202024/Forms/AllItems.aspx", "Syllabus"),
        SppuLink("Syllabus 2023", "http://collegecirculars.unipune.ac.in/sites/documents/Syllabus%202023/Forms/AllItems.aspx", "Syllabus"),
        SppuLink("Syllabus 2022", "http://collegecirculars.unipune.ac.in/sites/documents/Syllabus%202022/Forms/AllItems.aspx", "Syllabus"),
        SppuLink("Syllabus 2021", "http://collegecirculars.unipune.ac.in/sites/documents/Syllabus2021/Forms/AllItems.aspx", "Syllabus"),
        SppuLink("Syllabus 2020", "http://collegecirculars.unipune.ac.in/sites/documents/Syllabus2020/Forms/AllItems.aspx", "Syllabus"),
        SppuLink("Syllabus 2019", "http://collegecirculars.unipune.ac.in/sites/documents/Syllabus%202019/Forms/AllItems.aspx", "Syllabus"),
        SppuLink("Syllabus 2018", "http://collegecirculars.unipune.ac.in/sites/documents/Syllabus%202018/Forms/AllItems.aspx", "Syllabus"),
        SppuLink("Syllabus 2017", "http://collegecirculars.unipune.ac.in/sites/documents/Syllabus%202017/Forms/AllItems.aspx", "Syllabus"),
        SppuLink("Syllabus 2016", "http://collegecirculars.unipune.ac.in/sites/documents/Syllabus%202016/Forms/AllItems.aspx", "Syllabus"),
        SppuLink("PhD PET Syllabus", "http://collegecirculars.unipune.ac.in/sites/documents/MPhilPhD%20Admission%20PET%20Exam%20Syllabus/Forms/AllItems.aspx", "PhD Syllabus"),
        SppuLink("PhD Syllabus 2025", "http://collegecirculars.unipune.ac.in/sites/documents/Revised%20PhD%20Syllabus%20from%20the%20Academic%20Year%202025%20N/Forms/AllItems.aspx", "PhD Syllabus"),
        SppuLink("PhD Syllabus 2024", "http://collegecirculars.unipune.ac.in/sites/documents/Revised%20PhD%20Syllabus%20from%20the%20Academic%20Year%202024%20N/Forms/AllItems.aspx", "PhD Syllabus"),
        SppuLink("Academic Calendar", "http://collegecirculars.unipune.ac.in/sites/documents/Academic%20Calender/Forms/AllItems.aspx", "Other"),
        SppuLink("Previous Question Papers", "http://exam.unipune.ac.in/Pages/PreviousQuestionPapers.html", "Other"),
        SppuLink("Main Circulars Site", "http://collegecirculars.unipune.ac.in/SitePages/Home.aspx", "Circulars"),
        SppuLink("University Documents", "http://collegecirculars.unipune.ac.in/sites/documents/SitePages/Home.aspx", "Circulars"),
        SppuLink("Important Circulars for Colleges", "http://unipune.ac.in/university_files/imp_cir_for_college.htm", "Circulars"),
        SppuLink("Circular Portal", "http://www.unipune.ac.in/university_files/Circular_search.htm", "Circulars")
    )
    
    val groupedLinks = links.groupBy { it.category }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Important Links", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (!isTopLevel) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "ADVISORY: Use Desktop Site mode on phone if a page doesn't load correctly. PC/Laptop recommended.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
            
            groupedLinks.forEach { (category, categoryLinks) ->
                item {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                items(categoryLinks) { link ->
                    LinkCard(link = link) {
                        val intent = Intent(Intent.ACTION_VIEW, link.url.toUri())
                        context.startActivity(intent)
                    }
                }
            }
        }
    }
}

@Composable
fun LinkCard(link: SppuLink, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = link.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
