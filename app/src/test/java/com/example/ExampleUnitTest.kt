package com.example

import com.example.data.model.CourseGrade
import com.example.data.model.GpaTrendDirection
import com.example.data.model.ProgramType
import com.example.data.model.Report
import com.example.data.model.Student
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ExampleUnitTest {
    @Test
    fun testCourseGradeCalculation() {
        val course = CourseGrade(
            courseCode = "BIO-301",
            courseTitle = "Molecular Biology",
            creditUnits = 4,
            courseworkScore = 25.0,
            examScore = 50.0,
            practicalScore = 15.0
        )

        assertEquals(90.0, course.totalScore, 0.01)
        assertEquals("A+", course.letterGrade)
        assertEquals(4.0, course.gradePoint, 0.01)
    }

    @Test
    fun testUndergraduateHonoursClassification() {
        val report = Report(
            studentId = "stu_1",
            studentName = "Jane Doe",
            studentNumber = "UG-2024-001",
            programType = ProgramType.UNDERGRADUATE,
            termName = "Term 1",
            courses = listOf(
                CourseGrade(courseCode = "CS-101", creditUnits = 4, courseworkScore = 24.0, examScore = 52.0),
                CourseGrade(courseCode = "MATH-101", creditUnits = 4, courseworkScore = 26.0, examScore = 54.0)
            )
        )

        assertTrue(report.termGpa >= 3.60)
        assertEquals("First Class Honours (1st)", report.classification)
    }

    @Test
    fun testPostgraduateDistinctionClassification() {
        val pgReport = Report(
            studentId = "stu_pg",
            studentName = "Dr. Alan Turing",
            studentNumber = "PG-2024-999",
            programType = ProgramType.POSTGRADUATE,
            termName = "Master's Stage 1",
            courses = listOf(
                CourseGrade(courseCode = "CS-901", creditUnits = 4, courseworkScore = 35.0, examScore = 55.0, thesisScore = 0.0),
                CourseGrade(courseCode = "RES-900", creditUnits = 4, courseworkScore = 32.0, examScore = 52.0, thesisScore = 15.0)
            )
        )

        assertTrue(pgReport.termGpa >= 3.70)
        assertEquals("Master's Degree with Distinction", pgReport.classification)
    }
}
