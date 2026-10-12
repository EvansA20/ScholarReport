package com.example.data.model

import java.util.UUID

enum class ProgramType(val displayName: String, val code: String) {
    UNDERGRADUATE("Undergraduate (UG)", "UG"),
    POSTGRADUATE("Postgraduate (PG)", "PG")
}

data class CourseGrade(
    val id: String = UUID.randomUUID().toString(),
    val courseCode: String = "",
    val courseTitle: String = "",
    val creditUnits: Int = 3,
    val courseworkScore: Double = 0.0,   // typically 0-30 or 0-40
    val examScore: Double = 0.0,         // typically 0-50 or 0-70
    val practicalScore: Double = 0.0,    // for Science / STEM lab practicals (0-20)
    val thesisScore: Double = 0.0        // for PG dissertation / thesis / seminar
) {
    val totalScore: Double
        get() {
            val sum = courseworkScore + examScore + practicalScore + thesisScore
            return (sum * 10.0).toLong() / 10.0
        }

    val letterGrade: String
        get() = when {
            totalScore >= 75.0 -> "A+"
            totalScore >= 70.0 -> "A"
            totalScore >= 65.0 -> "B+"
            totalScore >= 60.0 -> "B"
            totalScore >= 55.0 -> "C+"
            totalScore >= 50.0 -> "C"
            totalScore >= 45.0 -> "D"
            else -> "F"
        }

    val gradePoint: Double
        get() = when (letterGrade) {
            "A+" -> 4.0
            "A" -> 4.0
            "B+" -> 3.5
            "B" -> 3.0
            "C+" -> 2.5
            "C" -> 2.0
            "D" -> 1.0
            else -> 0.0
        }
}

data class TemplateSection(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val promptCue: String = "",
    val defaultText: String = "",
    val isMandatory: Boolean = true
)

data class ReportTemplate(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val programType: ProgramType,
    val description: String,
    val sections: List<TemplateSection>,
    val defaultNotes: String = "",
    val isSystemDefault: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

data class Report(
    val id: String = UUID.randomUUID().toString(),
    val studentId: String,
    val studentName: String,
    val studentNumber: String,
    val programType: ProgramType,
    val termName: String,
    val templateId: String = "",
    val date: String = "",
    val courses: List<CourseGrade> = emptyList(),
    val executiveSummary: String = "",
    val strengths: String = "",
    val areasForImprovement: String = "",
    val boardRecommendation: String = "",
    val advisorRemarks: String = "",
    val scienceLabAssessment: String = "", // for Science / STEM research & practicals
    val deanSignOff: String = "Dr. Eleanor Vance, Dean of Academic Affairs",
    val status: String = "Draft", // Draft, Approved, Official
    val syncStatus: String = "Local", // Local, Synced, Pending
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalCredits: Int
        get() = courses.sumOf { it.creditUnits }

    val termGpa: Double
        get() {
            val totalCreds = totalCredits
            if (totalCreds == 0) return 0.0
            val totalQualityPoints = courses.sumOf { it.gradePoint * it.creditUnits }
            val raw = totalQualityPoints / totalCreds
            return (raw * 100.0).toLong() / 100.0
        }

    val classification: String
        get() = if (programType == ProgramType.UNDERGRADUATE) {
            when {
                termGpa >= 3.60 -> "First Class Honours (1st)"
                termGpa >= 3.00 -> "Second Class Honours: Upper Division (2:1)"
                termGpa >= 2.50 -> "Second Class Honours: Lower Division (2:2)"
                termGpa >= 2.00 -> "Third Class Honours (3rd)"
                termGpa >= 1.50 -> "Pass Degree"
                else -> "Academic Probation / Fail"
            }
        } else {
            when {
                termGpa >= 3.70 -> "Master's Degree with Distinction"
                termGpa >= 3.30 -> "Master's Degree with Merit"
                termGpa >= 3.00 -> "Commended Master's Pass"
                termGpa >= 2.70 -> "Standard Pass"
                else -> "Unsatisfactory / Resubmission Required"
            }
        }

    val academicStanding: String
        get() = when {
            termGpa >= 3.70 -> "Dean's Honours List"
            termGpa >= 3.00 -> "Good Standing"
            termGpa >= 2.00 -> "Satisfactory Progress"
            else -> "Academic Warning"
        }
}

data class Student(
    val id: String = UUID.randomUUID().toString(),
    val studentNumber: String,
    val fullName: String,
    val email: String,
    val programType: ProgramType,
    val faculty: String,
    val department: String,
    val degreeTitle: String,
    val currentLevel: String,
    val advisor: String,
    val overallGpa: Double = 0.0,
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

data class StudentAcademicProgress(
    val student: Student,
    val reports: List<Report>,
    val averageGpa: Double,
    val latestGpa: Double,
    val gpaDelta: Double,
    val totalCreditsAccumulated: Int,
    val trend: GpaTrendDirection,
    val gradeDistribution: Map<String, Int>
)

enum class GpaTrendDirection(val label: String) {
    UPWARD("Improving (+"),
    STEADY("Consistent"),
    DOWNWARD("At Risk ("),
    INSUFFICIENT_DATA("Initial Term")
}
