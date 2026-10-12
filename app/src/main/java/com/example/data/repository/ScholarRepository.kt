package com.example.data.repository

import android.content.Context
import com.example.data.ai.AiCommentSuggestion
import com.example.data.ai.GeminiAiService
import com.example.data.firebase.FirestoreSyncManager
import com.example.data.local.AppDatabase
import com.example.data.local.JsonConverters
import com.example.data.local.ReportEntity
import com.example.data.local.StudentEntity
import com.example.data.local.TemplateEntity
import com.example.data.model.GpaTrendDirection
import com.example.data.model.ProgramType
import com.example.data.model.Report
import com.example.data.model.ReportTemplate
import com.example.data.model.Student
import com.example.data.model.StudentAcademicProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScholarRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getInstance(context),
    val syncManager: FirestoreSyncManager = FirestoreSyncManager(database),
    private val aiService: GeminiAiService = GeminiAiService()
) {
    val students: Flow<List<Student>> = database.studentDao().getAllStudents().map { entities ->
        entities.map { it.toModel() }
    }

    val reports: Flow<List<Report>> = database.reportDao().getAllReports().map { entities ->
        entities.map { it.toModel() }
    }

    val templates: Flow<List<ReportTemplate>> = database.templateDao().getAllTemplates().map { entities ->
        entities.map { it.toModel() }
    }

    fun getStudent(id: String): Flow<Student?> = database.studentDao().getStudentById(id).map { it?.toModel() }

    fun getReport(id: String): Flow<Report?> = database.reportDao().getReportById(id).map { it?.toModel() }

    fun getReportsForStudent(studentId: String): Flow<List<Report>> =
        database.reportDao().getReportsForStudent(studentId).map { list -> list.map { it.toModel() } }

    suspend fun saveStudent(student: Student) {
        val entity = student.toEntity()
        database.studentDao().insertStudent(entity)
        syncManager.uploadStudent(entity)
    }

    suspend fun deleteStudent(student: Student) {
        database.studentDao().deleteStudent(student.toEntity())
    }

    suspend fun deletePreData() {
        database.studentDao().deletePreDataStudents()
        database.reportDao().deletePreDataReports()
    }

    suspend fun deleteAllData() {
        database.studentDao().deleteAllStudents()
        database.reportDao().deleteAllReports()
    }

    suspend fun saveReport(report: Report) {
        val entity = report.toEntity()
        database.reportDao().insertReport(entity)
        syncManager.uploadReport(entity)

        // Automatically update the student's overall GPA
        val studentReports = database.reportDao().getReportsForStudent(report.studentId)
        // Update student cached GPA
        val studentEntity = database.studentDao().getStudentById(report.studentId)
        // We will compute progress in ViewModel
    }

    suspend fun deleteReport(report: Report) {
        database.reportDao().deleteReport(report.toEntity())
    }

    suspend fun saveTemplate(template: ReportTemplate) {
        database.templateDao().insertTemplate(template.toEntity())
    }

    suspend fun deleteTemplate(template: ReportTemplate) {
        database.templateDao().deleteTemplate(template.toEntity())
    }

    suspend fun generateAiComments(
        student: Student,
        report: Report,
        customNotes: String = ""
    ): AiCommentSuggestion {
        val pastComments = mutableListOf<String>()
        if (report.strengths.isNotBlank()) pastComments.add("Strengths: ${report.strengths}")
        if (report.areasForImprovement.isNotBlank()) pastComments.add("Areas for Growth: ${report.areasForImprovement}")

        return aiService.generateReportComments(
            studentName = student.fullName,
            programType = report.programType,
            termName = report.termName,
            courses = report.courses,
            gpa = report.termGpa,
            classification = report.classification,
            pastComments = pastComments,
            customNotes = customNotes
        )
    }

    fun calculateProgress(student: Student, reports: List<Report>): StudentAcademicProgress {
        if (reports.isEmpty()) {
            return StudentAcademicProgress(
                student = student,
                reports = emptyList(),
                averageGpa = student.overallGpa,
                latestGpa = student.overallGpa,
                gpaDelta = 0.0,
                totalCreditsAccumulated = 0,
                trend = GpaTrendDirection.INSUFFICIENT_DATA,
                gradeDistribution = emptyMap()
            )
        }

        val sortedReports = reports.sortedBy { it.updatedAt }
        val gpas = sortedReports.map { it.termGpa }
        val totalCredits = sortedReports.sumOf { it.totalCredits }
        val avgGpa = (gpas.average() * 100.0).toLong() / 100.0
        val latestGpa = gpas.last()

        val delta = if (gpas.size >= 2) {
            val raw = latestGpa - gpas[gpas.size - 2]
            (raw * 100.0).toLong() / 100.0
        } else {
            0.0
        }

        val trend = when {
            gpas.size < 2 -> GpaTrendDirection.INSUFFICIENT_DATA
            delta > 0.05 -> GpaTrendDirection.UPWARD
            delta < -0.05 -> GpaTrendDirection.DOWNWARD
            else -> GpaTrendDirection.STEADY
        }

        // Calculate grade distribution (A+, A, B, etc.)
        val gradeDist = mutableMapOf<String, Int>()
        for (rep in reports) {
            for (c in rep.courses) {
                val letter = c.letterGrade
                gradeDist[letter] = (gradeDist[letter] ?: 0) + 1
            }
        }

        return StudentAcademicProgress(
            student = student,
            reports = sortedReports,
            averageGpa = avgGpa,
            latestGpa = latestGpa,
            gpaDelta = delta,
            totalCreditsAccumulated = totalCredits,
            trend = trend,
            gradeDistribution = gradeDist
        )
    }

    // Entity mapping helpers
    private fun StudentEntity.toModel(): Student = Student(
        id = id,
        studentNumber = studentNumber,
        fullName = fullName,
        email = email,
        programType = try { ProgramType.valueOf(programType) } catch (_: Exception) { ProgramType.UNDERGRADUATE },
        faculty = faculty,
        department = department,
        degreeTitle = degreeTitle,
        currentLevel = currentLevel,
        advisor = advisor,
        overallGpa = overallGpa,
        notes = notes,
        updatedAt = updatedAt
    )

    private fun Student.toEntity(): StudentEntity = StudentEntity(
        id = id,
        studentNumber = studentNumber,
        fullName = fullName,
        email = email,
        programType = programType.name,
        faculty = faculty,
        department = department,
        degreeTitle = degreeTitle,
        currentLevel = currentLevel,
        advisor = advisor,
        overallGpa = overallGpa,
        notes = notes,
        updatedAt = updatedAt
    )

    private fun ReportEntity.toModel(): Report = Report(
        id = id,
        studentId = studentId,
        studentName = studentName,
        studentNumber = studentNumber,
        programType = try { ProgramType.valueOf(programType) } catch (_: Exception) { ProgramType.UNDERGRADUATE },
        termName = termName,
        templateId = templateId,
        date = date,
        courses = JsonConverters.jsonToCourseGrades(coursesJson),
        executiveSummary = executiveSummary,
        strengths = strengths,
        areasForImprovement = areasForImprovement,
        boardRecommendation = boardRecommendation,
        advisorRemarks = advisorRemarks,
        scienceLabAssessment = scienceLabAssessment,
        deanSignOff = deanSignOff,
        status = status,
        syncStatus = syncStatus,
        updatedAt = updatedAt
    )

    private fun Report.toEntity(): ReportEntity = ReportEntity(
        id = id,
        studentId = studentId,
        studentName = studentName,
        studentNumber = studentNumber,
        programType = programType.name,
        termName = termName,
        templateId = templateId,
        date = date,
        coursesJson = JsonConverters.courseGradesToJson(courses),
        executiveSummary = executiveSummary,
        strengths = strengths,
        areasForImprovement = areasForImprovement,
        boardRecommendation = boardRecommendation,
        advisorRemarks = advisorRemarks,
        scienceLabAssessment = scienceLabAssessment,
        deanSignOff = deanSignOff,
        status = status,
        syncStatus = syncStatus,
        updatedAt = updatedAt
    )

    private fun TemplateEntity.toModel(): ReportTemplate = ReportTemplate(
        id = id,
        title = title,
        programType = try { ProgramType.valueOf(programType) } catch (_: Exception) { ProgramType.UNDERGRADUATE },
        description = description,
        sections = JsonConverters.jsonToTemplateSections(sectionsJson),
        defaultNotes = defaultNotes,
        isSystemDefault = isSystemDefault,
        updatedAt = updatedAt
    )

    private fun ReportTemplate.toEntity(): TemplateEntity = TemplateEntity(
        id = id,
        title = title,
        programType = programType.name,
        description = description,
        sectionsJson = JsonConverters.templateSectionsToJson(sections),
        defaultNotes = defaultNotes,
        isSystemDefault = isSystemDefault,
        updatedAt = updatedAt
    )
}
