package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CourseGrade
import com.example.data.model.ProgramType
import com.example.data.model.Report
import com.example.data.model.Student
import com.example.pdf.PdfReportExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ScholarReport", appName)
  }

  @Test
  fun `test generate and download formatted academic report PDF`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val student = Student(
      id = "stu_01",
      fullName = "Tariq Al-Mansoor",
      studentNumber = "UG-2024-042",
      degreeTitle = "B.Sc. Biotechnology & Microbiology",
      faculty = "Faculty of Natural Sciences",
      department = "Biological Sciences",
      advisor = "Prof. Marcus Brody"
    )
    val report = Report(
      id = "rep_01",
      studentId = student.id,
      studentName = student.fullName,
      studentNumber = student.studentNumber,
      programType = ProgramType.UNDERGRADUATE,
      termName = "Autumn Semester 2024",
      courses = listOf(
        CourseGrade(
          courseCode = "BIO-301",
          courseTitle = "Molecular Genetics & Recombinant DNA",
          creditUnits = 4,
          courseworkScore = 27.0,
          examScore = 52.0,
          practicalScore = 14.0
        ),
        CourseGrade(
          courseCode = "CHM-305",
          courseTitle = "Advanced Biochemistry",
          creditUnits = 3,
          courseworkScore = 26.0,
          examScore = 48.0,
          practicalScore = 15.0
        )
      ),
      executiveSummary = "Candidate demonstrated exceptional laboratory proficiency and analytical discipline.",
      strengths = "Consistently accurate titration and gel electrophoresis methodology.",
      areasForImprovement = "Ensure preliminary literature citations are thoroughly integrated into reports.",
      scienceLabAssessment = "High adherence to GLP safety protocols and aseptic technique.",
      boardRecommendation = "Recommended for Advanced Research Honours Track."
    )

    val downloadResult = PdfReportExporter.downloadPdfToDevice(context, report, student)
    assertNotNull(downloadResult)
    assertNotNull(downloadResult?.file)
    assertTrue(downloadResult!!.file.exists())
    assertTrue(downloadResult.file.length() > 0)
    assertTrue(downloadResult.fileName.contains("Tariq_Al_Mansoor"))
    assertTrue(downloadResult.fileName.endsWith(".pdf"))
  }
}

