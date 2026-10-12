package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ProgramType
import com.example.data.model.ReportTemplate
import com.example.data.model.TemplateSection
import com.example.ui.ScholarNavDestination
import com.example.ui.ScholarViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateManagerScreen(
    viewModel: ScholarViewModel,
    modifier: Modifier = Modifier
) {
    val templates by viewModel.allTemplates.collectAsState()
    var selectedFilter by remember { mutableStateOf<ProgramType?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<ReportTemplate?>(null) }

    BackHandler {
        viewModel.navigateTo(ScholarNavDestination.REPORTS_LIST)
    }

    val filtered = templates.filter {
        selectedFilter == null || it.programType == selectedFilter
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Report Templates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Institutional Section Blueprints & Grading Rubrics",
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
                onClick = {
                    editingTemplate = null
                    showCreateDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "New Template") },
                text = { Text("New Template") },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.testTag("create_template_fab")
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
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("All Templates (${templates.size})") }
                )
                FilterChip(
                    selected = selectedFilter == ProgramType.UNDERGRADUATE,
                    onClick = { selectedFilter = ProgramType.UNDERGRADUATE },
                    label = { Text("Undergraduate (UG)") },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                FilterChip(
                    selected = selectedFilter == ProgramType.POSTGRADUATE,
                    onClick = { selectedFilter = ProgramType.POSTGRADUATE },
                    label = { Text("Postgraduate (PG)") },
                    leadingIcon = { Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { template ->
                    TemplateItemCard(
                        template = template,
                        onUseTemplate = {
                            viewModel.startNewReport()
                            viewModel.applyTemplateToActiveReport(template)
                        },
                        onEdit = {
                            editingTemplate = template
                            showCreateDialog = true
                        },
                        onDelete = {
                            viewModel.deleteTemplate(template)
                        }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        TemplateEditorDialog(
            existing = editingTemplate,
            onDismiss = { showCreateDialog = false },
            onSave = { tmpl ->
                viewModel.saveTemplate(tmpl)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun TemplateItemCard(
    template: ReportTemplate,
    onUseTemplate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("template_card_${template.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = template.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (template.isSystemDefault) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Standard",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = template.programType.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${template.sections.size} Sections",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = template.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (template.defaultNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Policy: ${template.defaultNotes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expandable Sections Preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Hide Defined Sections" else "View Section Guidance (${template.sections.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    for ((index, sec) in template.sections.withIndex()) {
                        Text(
                            text = "${index + 1}. ${sec.title}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (sec.promptCue.isNotBlank()) {
                            Text(
                                text = "Guidance: ${sec.promptCue}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    if (!template.isSystemDefault) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Template", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Template", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Button(
                    onClick = onUseTemplate,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Apply to Report", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun TemplateEditorDialog(
    existing: ReportTemplate?,
    onDismiss: () -> Unit,
    onSave: (ReportTemplate) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var programType by remember { mutableStateOf(existing?.programType ?: ProgramType.UNDERGRADUATE) }
    var defaultNotes by remember { mutableStateOf(existing?.defaultNotes ?: "") }

    val sections = remember {
        mutableStateListOf<TemplateSection>().apply {
            if (existing != null) {
                addAll(existing.sections)
            } else {
                addAll(
                    listOf(
                        TemplateSection(title = "Executive Academic Summary", promptCue = "Summarize term momentum"),
                        TemplateSection(title = "Modular Performance Breakdown", promptCue = "Coursework and exam findings"),
                        TemplateSection(title = "Strengths & Commendations", promptCue = "Highlight academic distinction"),
                        TemplateSection(title = "Areas for Growth & Action Plan", promptCue = "Targeted revision strategy"),
                        TemplateSection(title = "Examination Board Recommendation", promptCue = "Formal progression standing")
                    )
                )
            }
        }
    }

    var newSectionTitle by remember { mutableStateOf("") }
    var newSectionCue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) "Create Report Template" else "Edit Template")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Template Title") },
                    modifier = Modifier.fillMaxWidth().testTag("template_title_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = programType == ProgramType.UNDERGRADUATE,
                        onClick = { programType = ProgramType.UNDERGRADUATE },
                        label = { Text("Undergraduate (UG)") }
                    )
                    FilterChip(
                        selected = programType == ProgramType.POSTGRADUATE,
                        onClick = { programType = ProgramType.POSTGRADUATE },
                        label = { Text("Postgraduate (PG)") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = defaultNotes,
                    onValueChange = { defaultNotes = it },
                    label = { Text("Default Academic Policy Guidance") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Report Sections (${sections.size}):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                for ((idx, sec) in sections.withIndex()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "${idx + 1}. ${sec.title}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                            if (sec.promptCue.isNotBlank()) {
                                Text(text = sec.promptCue, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        IconButton(onClick = { sections.removeAt(idx) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Section", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Add section input
                Text("Add Section to Template:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = newSectionTitle,
                    onValueChange = { newSectionTitle = it },
                    label = { Text("Section Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = newSectionCue,
                    onValueChange = { newSectionCue = it },
                    label = { Text("Assessor Guidance / Cue") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        if (newSectionTitle.isNotBlank()) {
                            sections.add(
                                TemplateSection(
                                    id = UUID.randomUUID().toString(),
                                    title = newSectionTitle,
                                    promptCue = newSectionCue
                                )
                            )
                            newSectionTitle = ""
                            newSectionCue = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    enabled = newSectionTitle.isNotBlank()
                ) {
                    Text("Add Section")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val template = ReportTemplate(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        title = if (title.isBlank()) "Custom Academic Template" else title,
                        programType = programType,
                        description = description,
                        sections = sections.toList(),
                        defaultNotes = defaultNotes,
                        isSystemDefault = false,
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(template)
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save Template")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
