package pinak.sppunotify.ui.screens

import android.content.Intent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    scrollState: ScrollState = rememberScrollState(),
) {
    val context = LocalContext.current

    var showExpandedLegal by remember { mutableStateOf(false) }
    var showExpandedData by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "About",
                        fontWeight = FontWeight.ExtraBold,
                    )
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Developer Photo
            AsyncImage(
                model = "https://avatars.githubusercontent.com/u/150576156?v=4",
                contentDescription = "Pinak Dhabu",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Pinak Dhabu",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = "Android Developer & FOSS Enthusiast",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/thepinak503/".toUri())
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.Code, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("GitHub")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // === ABOUT THE APP ===
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "About SPPU Result Watch",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = buildAnnotatedString {
                            append("SPPU Result Watch is a ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Free and Open Source (FOSS)")
                            }
                            append(" Android application built exclusively for students of ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Savitribai Phule Pune University (SPPU)")
                            }
                            append(".\n\n")
                            append("The app scrapes the official SPPU Online Results Portal (")
                            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                append("onlineresults.unipune.ac.in")
                            }
                            append(") to provide:")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.NewReleases, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(8.dp))
                            Text("Browse all published exam results", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.DataObject, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(8.dp))
                            Text("Filter by department (FE, SE, TE, BE, MBA, MCA, etc.)", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(8.dp))
                            Text("View individual results via seat number + CAPTCHA verification", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(8.dp))
                            Text("Download result PDFs to your device", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // === NON-AFFILIATION NOTICE ===
            OutlinedCard(
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f),
                ),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.5.dp),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotInterested, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "NOT AN OFFICIAL APP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("This application is NOT affiliated, associated, authorized, endorsed by, or in any way officially connected with ")
                            }
                            append("Savitribai Phule Pune University (SPPU), or any of its subsidiaries, departments, or affiliates.\n\n")
                            append("This is an independent, community-developed application. The official SPPU website is: ")
                            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)) {
                                append("www.unipune.ac.in")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // === DATA PRIVACY ===
            OutlinedCard(
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f),
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Data Privacy & Handling",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("What data enters this app:")
                            }
                            append("\n")
                            append("• Result listings fetched from SPPU's portal\n")
                            append("• Seat number and mother's name (entered by you to view results)\n")
                            append("• CAPTCHA images from SPPU's server\n\n")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("What is stored locally on YOUR device:")
                            }
                            append("\n")
                            append("• Basic result metadata (title, date, URL) in local SQLite database\n")
                            append("• Downloaded result files (in your Downloads folder)\n\n")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                                append("What is NEVER transmitted to third parties:")
                            }
                            append("\n")
                            append("• Your seat number\n")
                            append("• Your mother's name\n")
                            append("• Your downloaded results\n")
                            append("• Any personal information\n\n")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Medium)) {
                                append("All communication happens ONLY between your device and SPPU's official servers. No data is sent to any developer, third-party, or analytics server.")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp,
                    )
                    if (showExpandedData) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("Network Communication Details:\n")
                                }
                                append("• HTTPS connections only to: onlineresults.unipune.ac.in\n")
                                append("• User-Agent mimics a desktop browser for compatibility\n")
                                append("• Cookies and session data stored in memory only (per session)\n")
                                append("• No tracking cookies, no analytics, no telemetry\n\n")
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("Local Storage:\n")
                                }
                                append("• Room SQLite database: Contains result titles, URLs, dates for caching\n")
                                append("• MediaStore/Downloads: Result PDFs you choose to download\n")
                                append("• No persistent credentials stored\n")
                                append("• You can clear all app data via Android Settings → Apps")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { showExpandedData = !showExpandedData }) {
                        Text(if (showExpandedData) "Show less" else "Show technical details")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // === LEGAL DISCLAIMER (EXPANDABLE) ===
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f),
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Gavel, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Legal Disclaimer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("1. Ownership of Content\n")
                            }
                            append("All examination results, circulars, notifications, and related content displayed through this app are the exclusive intellectual property of ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Medium)) {
                                append("Savitribai Phule Pune University (SPPU)")
                            }
                            append(". The app merely acts as a browser/viewer for publicly accessible content.\n\n")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("2. Fair Use Purpose\n")
                            }
                            append("This application is provided for ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Medium)) {
                                append("educational, non-commercial purposes only")
                            }
                            append(". It is designed to assist students in conveniently accessing their exam results without having to navigate the official portal manually.\n\n")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("3. Accuracy of Information\n")
                            }
                            append("While we strive to display accurate and up-to-date information, this app ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)) {
                                append("does NOT guarantee the accuracy, completeness, or timeliness")
                            }
                            append(" of any data displayed. The official SPPU portal shall always be considered the sole authoritative source for result verification.\n\n")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Copyright Notice\n")
                            }
                            append("Copyright © 2026 Savitribai Phule Pune University. All rights reserved. All result content, logos, and university-related trademarks displayed through this app remain the sole property of SPPU.")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp,
                    )

                    if (showExpandedLegal) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("4. Limitation of Liability\n")
                                }
                                append("Under no circumstances shall the developer(s), contributor(s), or anyone associated with SPPU Result Watch be liable for:\n")
                                append("• Any direct, indirect, incidental, or consequential damages\n")
                                append("• Result discrepancies or errors in displayed information\n")
                                append("• Server downtime or network connectivity issues\n")
                                append("• Unauthorized access or data interception (while connections use HTTPS, no system is 100% secure)\n")
                                append("• Decisions made based on information from this app\n\n")
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("5. External Links\n")
                                }
                                append("This app may display or link to external SPPU portal URLs. The developer does not control and is not responsible for:\n")
                                append("• The content of external websites\n")
                                append("• Changes to external websites\n")
                                append("• Availability of external resources\n")
                                append("• Privacy practices of external websites\n\n")
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("6. No Warranty\n")
                                }
                                append("THIS SOFTWARE IS PROVIDED \"AS IS\" WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NONINFRINGEMENT. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU.\n\n")
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("7. User Responsibilities\n")
                                }
                                append("By using this app, you acknowledge and agree that:\n")
                                append("• You will use this app only for legitimate academic purposes\n")
                                append("• You will not use this app for any illegal or unauthorized purpose\n")
                                append("• You understand that result viewing requires entering your seat number and mother's name — this data is transmitted ONLY to SPPU's servers\n")
                                append("• You will verify critical information (results, dates, deadlines) through official SPPU channels\n")
                                append("• You will not misuse, redistribute, or sell any data obtained through this app\n\n")
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("8. Modifications to Disclaimer\n")
                                }
                                append("This legal disclaimer may be updated without prior notice. Continued use of the app constitutes acceptance of the current disclaimer.\n\n")
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("9. Governing Law\n")
                                }
                                append("This disclaimer and any disputes arising shall be governed by the laws of India. Any legal proceedings shall be subject to the exclusive jurisdiction of courts in Pune, Maharashtra.")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    TextButton(onClick = { showExpandedLegal = !showExpandedLegal }) {
                        Text(if (showExpandedLegal) "Show less" else "Show full legal disclaimer")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // === LICENSE / FOSS ===
            OutlinedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Open Source",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "SPPU Result Watch is Free and Open Source Software. You are free to view, modify, and distribute the source code under applicable open source license terms.",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
