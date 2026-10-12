package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ProgramType
import com.example.data.model.Student
import com.example.ui.ScholarNavDestination
import com.example.ui.ScholarViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsListScreen(
    viewModel: ScholarViewModel,
    modifier: Modifier = Modifier
) {
    val students by viewModel.filteredStudents.collectAsState()
    val allStudents by viewModel.allStudents.collectAsState()
    val programFilter by viewModel.programFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    BackHandler {
        viewModel.navigateTo(ScholarNavDestination.REPORTS_LIST)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Enrolled Students",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Undergraduate & Postgraduate Cohort Database",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Student") },
                text = { Text("Add Student") },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.testTag("add_student_fab")
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search and Filters
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search by student name, ID, department...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = programFilter == null,
                        onClick = { viewModel.setProgramFilter(null) },
                        label = { Text("All (${allStudents.size})") }
                    )
                    FilterChip(
                        selected = programFilter == ProgramType.UNDERGRADUATE,
                        onClick = { viewModel.setProgramFilter(ProgramType.UNDERGRADUATE) },
                        label = { Text("Undergraduate") }
                    )
                    FilterChip(
                        selected = programFilter == ProgramType.POSTGRADUATE,
                        onClick = { viewModel.setProgramFilter(ProgramType.POSTGRADUATE) },
                        label = { Text("Postgraduate") }
                    )
                }
            }

            if (students.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No students found matching current filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(students, key = { it.id }) { student ->
                        StudentCardItem(
                            student = student,
                            onViewProgress = {
                                viewModel.selectStudentForProgress(student)
                                viewModel.navigateTo(ScholarNavDestination.PROGRESS_ANALYTICS)
                            },
                            onCreateReport = {
                                viewModel.startNewReport(student)
                            },
                            onDelete = {
                                viewModel.deleteStudent(student)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddStudentDialog(
            onDismiss = { showAddDialog = false },
            onSave = { stu ->
                viewModel.saveStudent(stu)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun StudentCardItem(
    student: Student,
    onViewProgress: () -> Unit,
    onCreateReport: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("student_card_${student.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = student.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "ID: ${student.studentNumber} • ${student.currentLevel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = if (student.programType == ProgramType.UNDERGRADUATE)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = student.programType.code,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (student.programType == ProgramType.UNDERGRADUATE)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = student.degreeTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Text(text = "${student.faculty} • Advisor: ${student.advisor}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (student.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Notes: ${student.notes}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onViewProgress,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Track Progress", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = onCreateReport,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Write Report", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun AddStudentDialog(
    onDismiss: () -> Unit,
    onSave: (Student) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var studentNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var programType by remember { mutableStateOf(ProgramType.UNDERGRADUATE) }
    var faculty by remember { mutableStateOf("Faculty of Science & Computing") }
    var department by remember { mutableStateOf("Computer Science") }
    var degreeTitle by remember { mutableStateOf("BSc (Hons) Computer Science") }
    var currentLevel by remember { mutableStateOf("Year 2") }
    var advisor by remember { mutableStateOf("Dr. Eleanor Vance") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Student Record") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth().testTag("student_name_input")
                )

                OutlinedTextField(
                    value = studentNumber,
                    onValueChange = { studentNumber = it },
                    label = { Text("Student Registration Number") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = programType == ProgramType.UNDERGRADUATE,
                        onClick = {
                            programType = ProgramType.UNDERGRADUATE
                            degreeTitle = "BSc (Hons) Computer Science"
                            currentLevel = "Year 2"
                        },
                        label = { Text("Undergraduate") }
                    )
                    FilterChip(
                        selected = programType == ProgramType.POSTGRADUATE,
                        onClick = {
                            programType = ProgramType.POSTGRADUATE
                            degreeTitle = "MSc Artificial Intelligence"
                            currentLevel = "Master's Stage 1"
                        },
                        label = { Text("Postgraduate") }
                    )
                }

                OutlinedTextField(
                    value = degreeTitle,
                    onValueChange = { degreeTitle = it },
                    label = { Text("Degree Program Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = faculty,
                    onValueChange = { faculty = it },
                    label = { Text("Faculty / School") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Academic Department") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = currentLevel,
                    onValueChange = { currentLevel = it },
                    label = { Text("Current Academic Level / Stage") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = advisor,
                    onValueChange = { advisor = it },
                    label = { Text("Faculty Advisor / Supervisor") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Assessor Academic Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val stu = Student(
                        id = UUID.randomUUID().toString(),
                        studentNumber = if (studentNumber.isBlank()) "UG-${(1000..9999).random()}" else studentNumber,
                        fullName = fullName,
                        email = email,
                        programType = programType,
                        faculty = faculty,
                        department = department,
                        degreeTitle = degreeTitle,
                        currentLevel = currentLevel,
                        advisor = advisor,
                        notes = notes,
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(stu)
                },
                enabled = fullName.isNotBlank()
            ) {
                Text("Save Student")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
