package pinak.sppunotify.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import pinak.sppunotify.util.safeStartActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

data class SppuLink(val title: String, val url: String, val category: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinksScreen(
    onBackClick: () -> Unit,
    isTopLevel: Boolean = false,
    scrollState: LazyListState,
    viewModel: LinksViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val examTimeTableLinks by viewModel.examTimeTableLinks.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()
    val discoveryMessage by viewModel.discoveryMessage.collectAsState()

    // Static links (base set)
    val staticLinks = remember {
        listOf(
            SppuLink("SPPU Main Website", "http://unipune.ac.in/", "Main"),
            SppuLink("SPPU Results Link 1", "http://onlineresults.unipune.ac.in/SPPU", "Results"),
            SppuLink("SPPU Results Link 2", "http://onlineresults.unipune.ac.in/Result/Dashboard/Default", "Results"),
            SppuLink("Revaluation Results", "https://unipune.ac.in/university_files/Reval_Online_Results_online.htm", "Results"),
            SppuLink("Exam Form Dates", "https://examform.unipune.ac.in/Support/StuExDates.aspx", "Exam"),
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
    }

    // Merge static + dynamic exam timetable links
    val allLinks = remember(staticLinks, examTimeTableLinks) {
        staticLinks + examTimeTableLinks
    }

    val groupedLinks = remember(allLinks) {
        allLinks.groupBy { it.category }
    }

    // Category display order: Exam Time Tables at the top, then static categories
    val categoryOrder = remember(groupedLinks) {
        val desiredOrder = listOf(
            "Exam Time Tables",
            "Main",
            "Results",
            "Exam",
            "Syllabus",
            "PhD Syllabus",
            "Circulars",
            "Other"
        )
        desiredOrder.filter { it in groupedLinks.keys } +
        (groupedLinks.keys - desiredOrder.toSet()).sorted()
    }

    Scaffold(
        topBar = {
            pinak.sppunotify.ui.components.AppTopBar(
                title = "Important Links",
                navIcon = Icons.Default.Menu,
                onNavClick = onBackClick,
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.discoverExamLinks()
                        },
                        enabled = !isDiscovering,
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Discover exam timetables",
                            tint = if (isDiscovering)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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

                // Discovery status indicator
                if (isDiscovering || discoveryMessage.isNotEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDiscovering)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    if (isDiscovering) Icons.Default.Search else Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isDiscovering) "Scanning for exam timetables..."
                                           else discoveryMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                categoryOrder.forEach { category ->
                    val categoryLinks = groupedLinks[category] ?: return@forEach
                    item {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (category == "Exam Time Tables")
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(top = if (category == "Exam Time Tables") 4.dp else 16.dp, bottom = 8.dp)
                        )
                    }
                    items(categoryLinks, key = { it.url }) { link ->
                        LinkCard(
                            link = link,
                            modifier = Modifier.animateItem(),
                        ) {
                            val intent = Intent(Intent.ACTION_VIEW, link.url.toUri())
                            context.safeStartActivity(intent)
                        }
                    }
                }
            }

            LazyScrollbar(
                listState = scrollState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
fun LinkCard(
    link: SppuLink,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        animationSpec = tween(200),
        label = "bg"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = {
                    isPressed = true
                    onClick()
                    isPressed = false
                }
            ),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
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
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open link",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
