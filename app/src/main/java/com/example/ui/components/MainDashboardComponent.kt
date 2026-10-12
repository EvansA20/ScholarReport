package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.firebase.SyncState
import com.example.data.model.ProgramType
import com.example.data.model.Report
import com.example.ui.ScholarViewModel
import java.util.Locale

/**
 * Main dashboard UI component using Jetpack Compose that displays a list of recent
 * student reports fetched from Firestore, including a search bar to filter by student name.
 */
@Composable
fun MainDashboardComponent(
    viewModel: ScholarViewModel,
    onOpenReport: (Report) -> Unit,
    onEditReport: (Report) -> Unit,
    onCreateNewReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allReports by viewModel.allReports.collectAsState()
    val liveFirestoreReports by viewModel.liveFirestoreReports.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val isRefreshing by viewModel.isRefreshingFromFirestore.collectAsState()
    val lastPdfDownload by viewModel.lastPdfDownload.collectAsState()
    val isDownloadingPdf by viewModel.isDownloadingPdf.collectAsState()

    // Determine the active dataset (prefer live Firestore reports when available, or synced local reports)
    val baseReports = if (liveFirestoreReports.isNotEmpty()) liveFirestoreReports else allReports

    // Search bar state specifically for filtering by student name
    var studentNameQuery by remember { mutableStateOf("") }
    var selectedProgramFilter by remember { mutableStateOf<ProgramType?>(null) }

    // Filter recent reports by student name and program level
    val filteredReports = baseReports.filter { report ->
        val matchesName = studentNameQuery.isBlank() ||
            report.studentName.contains(studentNameQuery.trim(), ignoreCase = true)
        val matchesProgram = selectedProgramFilter == null || report.programType == selectedProgramFilter
        matchesName && matchesProgram
    }

    val ugCount = baseReports.count { it.programType == ProgramType.UNDERGRADUATE }
    val pgCount = baseReports.count { it.programType == ProgramType.POSTGRADUATE }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Firestore Cloud Status Banner
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (currentUser != null) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.secondary
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentUser != null) "Live Firestore Database Feed" else "Local Department Cache (Offline)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        IconButton(
                            onClick = { viewModel.refreshReportsFromFirestore() },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("refresh_firestore_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh from Firestore",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Stats Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(label = "Recent Reports", value = "${baseReports.size}", color = MaterialTheme.colorScheme.primary)
                    MetricItem(label = "Undergraduate", value = "$ugCount", color = MaterialTheme.colorScheme.secondary)
                    MetricItem(label = "Postgraduate", value = "$pgCount", color = MaterialTheme.colorScheme.tertiary)
                    val meanGpa = if (baseReports.isNotEmpty()) {
                        String.format(Locale.US, "%.2f", baseReports.map { it.termGpa }.average())
                    } else "0.00"
                    MetricItem(label = "Cohort GPA", value = meanGpa, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // 2. Search Bar to filter specifically by Student Name
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            OutlinedTextField(
                value = studentNameQuery,
                onValueChange = { studentNameQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_name_search_bar"),
                placeholder = { Text("Search by student name (e.g. Clara, Liam)...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (studentNameQuery.isNotEmpty()) {
                        IconButton(onClick = { studentNameQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search query"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Program Level Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedProgramFilter == null,
                    onClick = { selectedProgramFilter = null },
                    label = { Text("All Reports (${baseReports.size})") }
                )
                FilterChip(
                    selected = selectedProgramFilter == ProgramType.UNDERGRADUATE,
                    onClick = { selectedProgramFilter = ProgramType.UNDERGRADUATE },
                    label = { Text("Undergraduate ($ugCount)") }
                )
                FilterChip(
                    selected = selectedProgramFilter == ProgramType.POSTGRADUATE,
                    onClick = { selectedProgramFilter = ProgramType.POSTGRADUATE },
                    label = { Text("Postgraduate ($pgCount)") }
                )
            }
        }

        // 3. Reports List or Empty State
        if (filteredReports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (studentNameQuery.isNotBlank())
                            "No student reports matching '$studentNameQuery'"
                        else "No recent reports in Firestore database",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (studentNameQuery.isNotBlank())
                            "Check spelling or clear the search query to view all students."
                        else "Create a new undergraduate or postgraduate report to populate Firestore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (studentNameQuery.isNotBlank()) {
                        OutlinedButton(onClick = { studentNameQuery = "" }) {
                            Text("Clear Student Name Search")
                        }
                    } else {
                        Button(
                            onClick = onCreateNewReport,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Write First Academic Report")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("recent_reports_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredReports, key = { it.id }) { report ->
                    RecentReportCard(
                        report = report,
                        onOpen = { onOpenReport(report) },
                        onEdit = { onEditReport(report) },
                        onDownloadPdf = { viewModel.downloadReportPdf(context, report) },
                        onExportPdf = { viewModel.exportAndSharePdf(context, report) }
                    )
                }
            }
        }

        // PDF Download Feedback Dialogs
        PdfDownloadProgressDialog(isDownloading = isDownloadingPdf)

        PdfDownloadSuccessDialog(
            downloadResult = lastPdfDownload,
            onDismiss = { viewModel.dismissPdfDownloadDialog() },
            onOpen = { viewModel.openDownloadedPdf(context, it) },
            onShare = { file ->
                val activeReport = filteredReports.find { r -> lastPdfDownload?.fileName?.contains(r.studentNumber) == true }
                    ?: baseReports.firstOrNull()
                if (activeReport != null) {
                    viewModel.shareDownloadedPdf(context, file, activeReport)
                }
            }
        )
    }
}

@Composable
private fun MetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RecentReportCard(
    report: Report,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDownloadPdf: () -> Unit,
    onExportPdf: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("recent_report_item_${report.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Student Name and Program Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.studentName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "ID: ${report.studentNumber} • ${report.termName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = if (report.programType == ProgramType.UNDERGRADUATE)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = report.programType.code,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (report.programType == ProgramType.UNDERGRADUATE)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Performance Strip: GPA, Classification, and Credits
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "GPA: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f", report.termGpa),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = report.classification,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )

                Text(
                    text = "${report.totalCredits} Cr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (report.executiveSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = report.executiveSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDownloadPdf,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("download_pdf_${report.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Download PDF",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download PDF", style = MaterialTheme.typography.labelMedium)
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onExportPdf,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share PDF",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = onOpen,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("View Transcript", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
