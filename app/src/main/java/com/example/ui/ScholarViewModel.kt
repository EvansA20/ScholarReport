package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiCommentSuggestion
import com.example.data.firebase.AuthManager
import com.example.data.firebase.SyncState
import com.example.data.model.CourseGrade
import com.example.data.model.ProgramType
import com.example.data.model.Report
import com.example.data.model.ReportTemplate
import com.example.data.model.Student
import com.example.data.model.StudentAcademicProgress
import com.example.data.model.TemplateSection
import com.example.data.repository.ScholarRepository
import com.example.pdf.PdfDownloadResult
import com.example.pdf.PdfReportExporter
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ScholarNavDestination {
    REPORTS_LIST,
    STUDENTS_LIST,
    PROGRESS_ANALYTICS,
    TEMPLATES_MANAGER,
    CLOUD_SYNC,
    REPORT_EDITOR,
    REPORT_DETAIL
}

class ScholarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ScholarRepository(application)
    val authManager = AuthManager(application)

    val currentUser: StateFlow<FirebaseUser?> = authManager.currentUser
    val syncState: StateFlow<SyncState> = repository.syncManager.syncState
    val syncMessage: StateFlow<String?> = repository.syncManager.lastSyncMessage

    // Offline / Evaluator mode bypass
    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    fun continueOffline() {
        _isOfflineMode.value = true
    }

    fun resetOfflineMode() {
        _isOfflineMode.value = false
    }

    // Navigation State
    private val _currentDestination = MutableStateFlow(ScholarNavDestination.REPORTS_LIST)
    val currentDestination: StateFlow<ScholarNavDestination> = _currentDestination.asStateFlow()

    // Filter states
    private val _programFilter = MutableStateFlow<ProgramType?>(null)
    val programFilter: StateFlow<ProgramType?> = _programFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Data streams
    val allStudents: StateFlow<List<Student>> = repository.students
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReports: StateFlow<List<Report>> = repository.reports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTemplates: StateFlow<List<ReportTemplate>> = repository.templates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live reports stream from Firestore
    val liveFirestoreReports: StateFlow<List<Report>> = repository.syncManager.getRecentReportsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshingFromFirestore = MutableStateFlow(false)
    val isRefreshingFromFirestore: StateFlow<Boolean> = _isRefreshingFromFirestore.asStateFlow()

    fun refreshReportsFromFirestore() {
        viewModelScope.launch {
            _isRefreshingFromFirestore.value = true
            repository.syncManager.syncAllData()
            _isRefreshingFromFirestore.value = false
            _uiNotice.value = "Reports refreshed from Firestore cloud database."
        }
    }

    // Filtered lists
    val filteredReports = combine(allReports, _programFilter, _searchQuery) { reports, prog, query ->
        reports.filter { r ->
            (prog == null || r.programType == prog) &&
                (query.isBlank() || r.studentName.contains(query, ignoreCase = true) ||
                    r.studentNumber.contains(query, ignoreCase = true) ||
                    r.termName.contains(query, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredStudents = combine(allStudents, _programFilter, _searchQuery) { students, prog, query ->
        students.filter { s ->
            (prog == null || s.programType == prog) &&
                (query.isBlank() || s.fullName.contains(query, ignoreCase = true) ||
                    s.studentNumber.contains(query, ignoreCase = true) ||
                    s.department.contains(query, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Report for Detail / Editor
    private val _selectedReport = MutableStateFlow<Report?>(null)
    val selectedReport: StateFlow<Report?> = _selectedReport.asStateFlow()

    // Selected Student for Progress View
    private val _selectedStudent = MutableStateFlow<Student?>(null)
    val selectedStudent: StateFlow<Student?> = _selectedStudent.asStateFlow()

    private val _studentProgress = MutableStateFlow<StudentAcademicProgress?>(null)
    val studentProgress: StateFlow<StudentAcademicProgress?> = _studentProgress.asStateFlow()

    // AI Comments Suggestion State
    private val _isGeneratingAiComments = MutableStateFlow(false)
    val isGeneratingAiComments: StateFlow<Boolean> = _isGeneratingAiComments.asStateFlow()

    private val _aiSuggestion = MutableStateFlow<AiCommentSuggestion?>(null)
    val aiSuggestion: StateFlow<AiCommentSuggestion?> = _aiSuggestion.asStateFlow()

    // Toast/Snackbar notifications
    private val _uiNotice = MutableStateFlow<String?>(null)
    val uiNotice: StateFlow<String?> = _uiNotice.asStateFlow()

    // Downloaded PDF state
    private val _lastPdfDownload = MutableStateFlow<PdfDownloadResult?>(null)
    val lastPdfDownload: StateFlow<PdfDownloadResult?> = _lastPdfDownload.asStateFlow()

    private val _isDownloadingPdf = MutableStateFlow(false)
    val isDownloadingPdf: StateFlow<Boolean> = _isDownloadingPdf.asStateFlow()

    init {
        viewModelScope.launch {
            // Delete pre-data on launch
            repository.deletePreData()
            allStudents.collect { students ->
                if (_selectedStudent.value == null && students.isNotEmpty()) {
                    selectStudentForProgress(students.first())
                }
            }
        }
    }

    fun deletePreData() {
        viewModelScope.launch {
            repository.deletePreData()
            _selectedReport.value = null
            _selectedStudent.value = null
            _studentProgress.value = null
            _uiNotice.value = "Pre-data has been completely deleted."
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            repository.deleteAllData()
            _selectedReport.value = null
            _selectedStudent.value = null
            _studentProgress.value = null
            _uiNotice.value = "All reports and student records deleted."
        }
    }

    fun navigateTo(dest: ScholarNavDestination) {
        _currentDestination.value = dest
    }

    fun setProgramFilter(filter: ProgramType?) {
        _programFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearUiNotice() {
        _uiNotice.value = null
    }

    // --- Report Actions ---

    fun openReportDetail(report: Report) {
        _selectedReport.value = report
        _currentDestination.value = ScholarNavDestination.REPORT_DETAIL
    }

    fun startNewReport(forStudent: Student? = null) {
        val student = forStudent ?: allStudents.value.firstOrNull()
        val defaultTemplate = allTemplates.value.firstOrNull {
            if (student != null) it.programType == student.programType else true
        }

        val newReport = Report(
            id = UUID.randomUUID().toString(),
            studentId = student?.id ?: "",
            studentName = student?.fullName ?: "",
            studentNumber = student?.studentNumber ?: "",
            programType = student?.programType ?: ProgramType.UNDERGRADUATE,
            termName = "Semester 1 (2025/2026)",
            templateId = defaultTemplate?.id ?: "",
            date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()),
            courses = listOf(
                CourseGrade(
                    id = UUID.randomUUID().toString(),
                    courseCode = if (student?.programType == ProgramType.POSTGRADUATE) "RES-701" else "BIO-201",
                    courseTitle = if (student?.programType == ProgramType.POSTGRADUATE) "Advanced Research Methodology" else "Cellular Biology & Genetics",
                    creditUnits = 3,
                    courseworkScore = 24.0,
                    examScore = 52.0,
                    practicalScore = if (student?.programType == ProgramType.UNDERGRADUATE) 16.0 else 0.0,
                    thesisScore = if (student?.programType == ProgramType.POSTGRADUATE) 18.0 else 0.0
                )
            ),
            executiveSummary = "",
            strengths = "",
            areasForImprovement = "",
            boardRecommendation = "",
            deanSignOff = "Dr. Eleanor Vance, Dean of Academic Affairs",
            status = "Draft"
        )
        _selectedReport.value = newReport
        _aiSuggestion.value = null
        _currentDestination.value = ScholarNavDestination.REPORT_EDITOR
    }

    fun editReport(report: Report) {
        _selectedReport.value = report
        _aiSuggestion.value = null
        _currentDestination.value = ScholarNavDestination.REPORT_EDITOR
    }

    fun updateActiveReport(updated: Report) {
        _selectedReport.value = updated
    }

    fun applyTemplateToActiveReport(template: ReportTemplate) {
        val current = _selectedReport.value ?: return
        val updated = current.copy(
            templateId = template.id,
            advisorRemarks = template.defaultNotes
        )
        _selectedReport.value = updated
        _uiNotice.value = "Template '${template.title}' applied to report."
    }

    fun addCourseToActiveReport() {
        val current = _selectedReport.value ?: return
        val newCourse = CourseGrade(
            id = UUID.randomUUID().toString(),
            courseCode = "MOD-00" + (current.courses.size + 1),
            courseTitle = "Elective Course ${current.courses.size + 1}",
            creditUnits = 3,
            courseworkScore = 20.0,
            examScore = 45.0,
            practicalScore = if (current.programType == ProgramType.UNDERGRADUATE) 14.0 else 0.0,
            thesisScore = if (current.programType == ProgramType.POSTGRADUATE) 15.0 else 0.0
        )
        _selectedReport.value = current.copy(courses = current.courses + newCourse)
    }

    fun updateCourseInActiveReport(updatedCourse: CourseGrade) {
        val current = _selectedReport.value ?: return
        val updatedList = current.courses.map {
            if (it.id == updatedCourse.id) updatedCourse else it
        }
        _selectedReport.value = current.copy(courses = updatedList)
    }

    fun removeCourseFromActiveReport(courseId: String) {
        val current = _selectedReport.value ?: return
        val updatedList = current.courses.filter { it.id != courseId }
        _selectedReport.value = current.copy(courses = updatedList)
    }

    fun saveActiveReport() {
        val current = _selectedReport.value ?: return
        viewModelScope.launch {
            repository.saveReport(current)
            _uiNotice.value = "Report saved successfully."
            _currentDestination.value = ScholarNavDestination.REPORT_DETAIL
        }
    }

    fun deleteReport(report: Report) {
        viewModelScope.launch {
            repository.deleteReport(report)
            _uiNotice.value = "Report deleted."
            if (_selectedReport.value?.id == report.id) {
                _selectedReport.value = null
                _currentDestination.value = ScholarNavDestination.REPORTS_LIST
            }
        }
    }

    // --- AI Suggestion Actions ---

    fun generateAiCommentsForActiveReport(customNotes: String = "") {
        val current = _selectedReport.value ?: return
        val student = allStudents.value.find { it.id == current.studentId } ?: Student(
            id = current.studentId,
            studentNumber = current.studentNumber,
            fullName = current.studentName,
            email = "",
            programType = current.programType,
            faculty = "Faculty of Academic Affairs",
            department = "Department",
            degreeTitle = "Degree Candidate",
            currentLevel = "Level",
            advisor = "Faculty Advisor"
        )

        viewModelScope.launch {
            _isGeneratingAiComments.value = true
            val suggestion = repository.generateAiComments(student, current, customNotes)
            _aiSuggestion.value = suggestion
            _isGeneratingAiComments.value = false
            _uiNotice.value = "AI feedback generated! Review and accept into report."
        }
    }

    fun acceptAiSuggestion() {
        val current = _selectedReport.value ?: return
        val suggestion = _aiSuggestion.value ?: return

        val updated = current.copy(
            executiveSummary = suggestion.executiveSummary,
            strengths = suggestion.strengths,
            areasForImprovement = suggestion.areasForImprovement,
            boardRecommendation = suggestion.boardRecommendation,
            scienceLabAssessment = suggestion.scienceLabAssessment
        )
        _selectedReport.value = updated
        _aiSuggestion.value = null
        _uiNotice.value = "AI comments integrated into report sections."
    }

    fun dismissAiSuggestion() {
        _aiSuggestion.value = null
    }

    // --- PDF Export & Download ---

    fun downloadReportPdf(context: Context, report: Report) {
        viewModelScope.launch {
            _isDownloadingPdf.value = true
            val student = allStudents.value.find { it.id == report.studentId }
            val result = PdfReportExporter.downloadPdfToDevice(context, report, student)
            _isDownloadingPdf.value = false
            if (result != null) {
                _lastPdfDownload.value = result
                _uiNotice.value = "PDF downloaded: ${result.fileName}"
            } else {
                _uiNotice.value = "Failed to generate PDF document."
            }
        }
    }

    fun openDownloadedPdf(context: Context, file: File) {
        PdfReportExporter.openPdf(context, file)
    }

    fun shareDownloadedPdf(context: Context, file: File, report: Report) {
        PdfReportExporter.sharePdf(context, file, report)
    }

    fun dismissPdfDownloadDialog() {
        _lastPdfDownload.value = null
    }

    fun exportAndSharePdf(context: Context, report: Report) {
        val student = allStudents.value.find { it.id == report.studentId }
        val file: File? = PdfReportExporter.generateAndSharePdf(context, report, student)
        if (file != null) {
            _uiNotice.value = "PDF exported: ${file.name}"
        } else {
            _uiNotice.value = "Failed to create PDF document."
        }
    }

    // --- Student Progress Actions ---

    fun selectStudentForProgress(student: Student) {
        _selectedStudent.value = student
        val reports = allReports.value.filter { it.studentId == student.id }
        _studentProgress.value = repository.calculateProgress(student, reports)
    }

    fun saveStudent(student: Student) {
        viewModelScope.launch {
            repository.saveStudent(student)
            _uiNotice.value = "Student profile saved."
        }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch {
            repository.deleteStudent(student)
            _uiNotice.value = "Student deleted."
            if (_selectedStudent.value?.id == student.id) {
                _selectedStudent.value = null
                _studentProgress.value = null
            }
        }
    }

    // --- Template Actions ---

    fun saveTemplate(template: ReportTemplate) {
        viewModelScope.launch {
            repository.saveTemplate(template)
            _uiNotice.value = "Template '${template.title}' saved."
        }
    }

    fun deleteTemplate(template: ReportTemplate) {
        if (template.isSystemDefault) {
            _uiNotice.value = "Default system templates cannot be deleted."
            return
        }
        viewModelScope.launch {
            repository.deleteTemplate(template)
            _uiNotice.value = "Template removed."
        }
    }

    // --- Cloud Sync ---

    fun triggerCloudSync() {
        viewModelScope.launch {
            val success = repository.syncManager.syncAllData()
            if (success) {
                _uiNotice.value = "Cloud synchronization completed successfully."
            } else {
                _uiNotice.value = repository.syncManager.lastSyncMessage.value ?: "Sync failed."
            }
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            val success = authManager.signInWithGoogle()
            if (success) {
                _uiNotice.value = "Signed in as ${authManager.currentUser.value?.email}"
                repository.syncManager.syncAllData()
            } else {
                _uiNotice.value = authManager.errorMessage.value ?: "Sign-in cancelled or failed."
            }
        }
    }

    fun signOut() {
        authManager.signOut()
        _isOfflineMode.value = false
        _uiNotice.value = "Signed out."
    }
}
