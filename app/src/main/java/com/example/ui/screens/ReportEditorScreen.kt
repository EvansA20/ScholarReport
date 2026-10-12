package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CourseGrade
import com.example.data.model.ProgramType
import com.example.data.model.Report
import com.example.data.model.ReportTemplate
import com.example.ui.ScholarNavDestination
import com.example.ui.ScholarViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportEditorScreen(
    viewModel: ScholarViewModel,
    modifier: Modifier = Modifier
) {
    val report = viewModel.selectedReport.collectAsState().value
    val allStudents by viewModel.allStudents.collectAsState()
    val allTemplates by viewModel.allTemplates.collectAsState()
    val isGeneratingAi by viewModel.isGeneratingAiComments.collectAsState()
    val aiSuggestion by viewModel.aiSuggestion.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showAiCustomPromptDialog by remember { mutableStateOf(false) }
    var customAiPromptText by remember { mutableStateOf("") }

    BackHandler {
        viewModel.navigateTo(ScholarNavDestination.REPORTS_LIST)
    }

    if (report == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active report selected.")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Write Academic Report",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "${report.programType.displayName} • ${report.termName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(ScholarNavDestination.REPORTS_LIST) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showTemplateDialog = true },
                        modifier = Modifier.testTag("apply_template_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Select Template",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.saveActiveReport() },
                        modifier = Modifier.testTag("save_report_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Report",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Term GPA: ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "%.2f", report.termGpa),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = report.classification,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Row {
                        Button(
                            onClick = { viewModel.saveActiveReport() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("save_bottom_bar_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Report")
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Editor Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("1. Student & Modules") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("2. Comments & AI") }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (selectedTabIndex == 0) {
                    // TAB 1: Student Metadata & Modular Grade Input
                    StudentAndMetaSection(
                        report = report,
                        allStudents = allStudents,
                        allTemplates = allTemplates,
                        onUpdate = { viewModel.updateActiveReport(it) },
                        onApplyTemplate = { viewModel.applyTemplateToActiveReport(it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ModularGradesSection(
                        report = report,
                        onAddCourse = { viewModel.addCourseToActiveReport() },
                        onUpdateCourse = { viewModel.updateCourseInActiveReport(it) },
                        onRemoveCourse = { viewModel.removeCourseFromActiveReport(it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { selectedTabIndex = 1 },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Proceed to Comments & AI Synthesis →")
                    }
                } else {
                    // TAB 2: AI Comments Generator & Report Sections
                    AiSuggestionCard(
                        isGenerating = isGeneratingAi,
                        suggestion = aiSuggestion,
                        onGenerate = { viewModel.generateAiCommentsForActiveReport() },
                        onCustomGenerate = { showAiCustomPromptDialog = true },
                        onAccept = { viewModel.acceptAiSuggestion() },
                        onDismiss = { viewModel.dismissAiSuggestion() }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ReportSectionsEditor(
                        report = report,
                        onUpdate = { viewModel.updateActiveReport(it) }
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Template selection dialog
    if (showTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text("Select Report Template") },
            text = {
                Column {
                    Text(
                        text = "Applying a template configures section structure and default academic guidance:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    for (tmpl in allTemplates) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.applyTemplateToActiveReport(tmpl)
                                    showTemplateDialog = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (report.templateId == tmpl.id)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = tmpl.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tmpl.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplateDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Custom AI Prompt Dialog
    if (showAiCustomPromptDialog) {
        AlertDialog(
            onDismissRequest = { showAiCustomPromptDialog = false },
            title = { Text("AI Assessor Prompt Guidance") },
            text = {
                Column {
                    Text(
                        text = "Add specific assessor observations or focus areas (e.g., 'Emphasize strong biochemistry lab pipetting and need for statistical power calculation'):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customAiPromptText,
                        onValueChange = { customAiPromptText = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        placeholder = { Text("Enter faculty observations...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAiCustomPromptDialog = false
                        viewModel.generateAiCommentsForActiveReport(customAiPromptText)
                    }
                ) {
                    Text("Generate AI Feedback")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiCustomPromptDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentAndMetaSection(
    report: Report,
    allStudents: List<com.example.data.model.Student>,
    allTemplates: List<ReportTemplate>,
    onUpdate: (Report) -> Unit,
    onApplyTemplate: (ReportTemplate) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "1. Student Identification & Program",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Student selection dropdown
            var expandedStudentDropdown by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedStudentDropdown,
                onExpandedChange = { expandedStudentDropdown = it }
            ) {
                OutlinedTextField(
                    value = if (report.studentName.isNotBlank()) "${report.studentName} (${report.studentNumber})" else "Select Student",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Enrolled Student") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStudentDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )

                ExposedDropdownMenu(
                    expanded = expandedStudentDropdown,
                    onDismissRequest = { expandedStudentDropdown = false }
                ) {
                    for (student in allStudents) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(student.fullName, fontWeight = FontWeight.Bold)
                                    Text("${student.studentNumber} • ${student.programType.displayName} • ${student.degreeTitle}", style = MaterialTheme.typography.bodySmall)
                                }
                            },
                            onClick = {
                                onUpdate(
                                    report.copy(
                                        studentId = student.id,
                                        studentName = student.fullName,
                                        studentNumber = student.studentNumber,
                                        programType = student.programType
                                    )
                                )
                                expandedStudentDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Manual Student Name and ID override
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = report.studentName,
                    onValueChange = { onUpdate(report.copy(studentName = it)) },
                    label = { Text("Student Full Name") },
                    modifier = Modifier.weight(1.2f)
                )
                OutlinedTextField(
                    value = report.studentNumber,
                    onValueChange = { onUpdate(report.copy(studentNumber = it)) },
                    label = { Text("Student ID") },
                    modifier = Modifier.weight(0.8f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Program Type Selection
            Text(
                text = "Academic Level Standard:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = report.programType == ProgramType.UNDERGRADUATE,
                    onClick = { onUpdate(report.copy(programType = ProgramType.UNDERGRADUATE)) },
                    label = { Text("Undergraduate (UG)") },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = report.programType == ProgramType.POSTGRADUATE,
                    onClick = { onUpdate(report.copy(programType = ProgramType.POSTGRADUATE)) },
                    label = { Text("Postgraduate (PG)") },
                    leadingIcon = { Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Academic Term
            OutlinedTextField(
                value = report.termName,
                onValueChange = { onUpdate(report.copy(termName = it)) },
                label = { Text("Academic Term / Session") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ModularGradesSection(
    report: Report,
    onAddCourse: () -> Unit,
    onUpdateCourse: (CourseGrade) -> Unit,
    onRemoveCourse: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "2. Modular Grades & Components",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Coursework (CW), Exam, Lab/Practical, and Thesis breakdown",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onAddCourse,
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.testTag("add_course_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Module", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            for (course in report.courses) {
                CourseGradeEditItem(
                    course = course,
                    isUndergrad = report.programType == ProgramType.UNDERGRADUATE,
                    onUpdate = onUpdateCourse,
                    onRemove = { onRemoveCourse(course.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun CourseGradeEditItem(
    course: CourseGrade,
    isUndergrad: Boolean,
    onUpdate: (CourseGrade) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = course.courseCode,
                        onValueChange = { onUpdate(course.copy(courseCode = it)) },
                        label = { Text("Code") },
                        modifier = Modifier.weight(0.9f)
                    )
                    OutlinedTextField(
                        value = course.courseTitle,
                        onValueChange = { onUpdate(course.copy(courseTitle = it)) },
                        label = { Text("Module Title") },
                        modifier = Modifier.weight(1.5f)
                    )
                    OutlinedTextField(
                        value = "${course.creditUnits}",
                        onValueChange = { onUpdate(course.copy(creditUnits = it.toIntOrNull() ?: 3)) },
                        label = { Text("Cr") },
                        modifier = Modifier.weight(0.6f)
                    )
                }

                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove Course", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Score inputs row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = "${course.courseworkScore}",
                    onValueChange = { onUpdate(course.copy(courseworkScore = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text("CW Score") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = "${course.examScore}",
                    onValueChange = { onUpdate(course.copy(examScore = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text("Exam Score") },
                    modifier = Modifier.weight(1f)
                )

                if (isUndergrad) {
                    OutlinedTextField(
                        value = "${course.practicalScore}",
                        onValueChange = { onUpdate(course.copy(practicalScore = it.toDoubleOrNull() ?: 0.0)) },
                        label = { Text("Lab/Prac") },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    OutlinedTextField(
                        value = "${course.thesisScore}",
                        onValueChange = { onUpdate(course.copy(thesisScore = it.toDoubleOrNull() ?: 0.0)) },
                        label = { Text("Thesis/Res") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Total & Letter Grade display
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${course.totalScore}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = course.letterGrade,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AiSuggestionCard(
    isGenerating: Boolean,
    suggestion: com.example.data.ai.AiCommentSuggestion?,
    onGenerate: () -> Unit,
    onCustomGenerate: () -> Unit,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth().testTag("ai_suggestion_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Assessor",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Academic Feedback Engine",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Analyzes modular grades, STEM practicals, credits, and historical comments to draft constructive, balanced academic evaluations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onGenerate,
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("ai_generate_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Suggest Comments")
                }

                OutlinedButton(
                    onClick = onCustomGenerate,
                    enabled = !isGenerating
                ) {
                    Text("Custom Prompt")
                }
            }

            // Generated AI Output Preview
            if (suggestion != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Draft Executive Summary:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(text = suggestion.executiveSummary, style = MaterialTheme.typography.bodySmall)

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Draft Strengths & Commendations:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(text = suggestion.strengths, style = MaterialTheme.typography.bodySmall)

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Draft Areas for Growth:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(text = suggestion.areasForImprovement, style = MaterialTheme.typography.bodySmall)

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Formal Board Recommendation:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(text = suggestion.boardRecommendation, style = MaterialTheme.typography.bodySmall)

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("Discard")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onAccept,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                modifier = Modifier.testTag("ai_accept_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Accept into Report")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportSectionsEditor(
    report: Report,
    onUpdate: (Report) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Official Evaluation Sections",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Review and edit the standard narrative sections for academic records and student transcript:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = report.executiveSummary,
                onValueChange = { onUpdate(report.copy(executiveSummary = it)) },
                label = { Text("Executive Academic Summary") },
                modifier = Modifier.fillMaxWidth().testTag("executive_summary_input"),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = report.strengths,
                onValueChange = { onUpdate(report.copy(strengths = it)) },
                label = { Text("Academic Strengths & Commendations") },
                modifier = Modifier.fillMaxWidth().testTag("strengths_input"),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = report.areasForImprovement,
                onValueChange = { onUpdate(report.copy(areasForImprovement = it)) },
                label = { Text("Areas for Improvement & Academic Guidance") },
                modifier = Modifier.fillMaxWidth().testTag("improvement_input"),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = report.scienceLabAssessment,
                onValueChange = { onUpdate(report.copy(scienceLabAssessment = it)) },
                label = { Text("Science Practical / Research Methodology Appraisal") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = report.boardRecommendation,
                onValueChange = { onUpdate(report.copy(boardRecommendation = it)) },
                label = { Text("Examination Board Recommendation") },
                modifier = Modifier.fillMaxWidth().testTag("recommendation_input"),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = report.deanSignOff,
                onValueChange = { onUpdate(report.copy(deanSignOff = it)) },
                label = { Text("Dean / Academic Registry Sign-Off") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
