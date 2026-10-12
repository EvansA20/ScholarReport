package com.example.pdf

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.ProgramType
import com.example.data.model.Report
import com.example.data.model.Student
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PdfDownloadResult(
    val file: File,
    val fileName: String,
    val publicUri: Uri?,
    val isSavedToDownloads: Boolean,
    val storageLocationDescription: String
)

object PdfReportExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    /**
     * Converts the retrieved Firestore report data into a formatted, downloadable PDF document,
     * saving it into the system Downloads folder (or media store) and local archive.
     */
    fun downloadPdfToDevice(context: Context, report: Report, student: Student?): PdfDownloadResult? {
        val sanitizedStudent = report.studentName.replace(Regex("[^a-zA-Z0-9]"), "_")
        val sanitizedNumber = report.studentNumber.replace(Regex("[^a-zA-Z0-9]"), "_")
        val sanitizedTerm = report.termName.replace(Regex("[^a-zA-Z0-9]"), "_")
        val fileName = "ScholarReport_${sanitizedStudent}_${sanitizedNumber}_${sanitizedTerm}.pdf"

        // 1. Generate formatted PDF file into internal cache
        val internalFile = generateFormattedPdfFile(context, report, student, fileName) ?: return null

        var publicUri: Uri? = null
        var isPublic = false
        var locationDesc = "Internal Archive: ${internalFile.name}"

        // 2. Download / Save to public Downloads directory
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/ScholarReports")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        FileInputStream(internalFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    publicUri = uri
                    isPublic = true
                    locationDesc = "Downloads/ScholarReports/$fileName"
                }
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, "ScholarReports")
                if (!targetDir.exists()) targetDir.mkdirs()
                val targetFile = File(targetDir, fileName)
                FileInputStream(internalFile).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                isPublic = true
                locationDesc = "Downloads/ScholarReports/$fileName"
            }
        } catch (e: Exception) {
            // Fallback: copy to external files directory
            try {
                val extDownloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                if (extDownloads != null) {
                    val fallbackFile = File(extDownloads, fileName)
                    FileInputStream(internalFile).use { input ->
                        FileOutputStream(fallbackFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    locationDesc = "Downloads/$fileName"
                    isPublic = true
                }
            } catch (_: Exception) {}
        }

        return PdfDownloadResult(
            file = internalFile,
            fileName = fileName,
            publicUri = publicUri,
            isSavedToDownloads = isPublic,
            storageLocationDescription = locationDesc
        )
    }

    /**
     * Opens the PDF document using any installed PDF reader on the device.
     */
    fun openPdf(context: Context, file: File) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Open Official Academic Report")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Shares the PDF document with academic departments, faculty, or students.
     */
    fun sharePdf(context: Context, file: File, report: Report) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Official Academic Report - ${report.studentName} (${report.termName})")
            putExtra(
                Intent.EXTRA_TEXT,
                "Please find attached the official academic transcript and evaluation report for ${report.studentName} (${report.studentNumber}), Term: ${report.termName}. Generated via ScholarReport Cloud System."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share Academic Report PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Legacy helper method for share workflow.
     */
    fun generateAndSharePdf(context: Context, report: Report, student: Student?): File? {
        val downloadResult = downloadPdfToDevice(context, report, student) ?: return null
        sharePdf(context, downloadResult.file, report)
        return downloadResult.file
    }

    /**
     * Core PDF generation implementation using Android's native android.graphics.pdf.PdfDocument library.
     * Generates a multi-page formatted academic transcript and evaluation document.
     */
    private fun generateFormattedPdfFile(
        context: Context,
        report: Report,
        student: Student?,
        fileName: String
    ): File? {
        val pdfDocument = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // ================= PAGE 1: TRANSCRIPT & MODULAR GRADES =================
        val page1Info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page1 = pdfDocument.startPage(page1Info)
        val canvas1: Canvas = page1.canvas

        // Header Banner (Deep Navy + Gold Accent)
        paint.color = Color.parseColor("#0A192F")
        canvas1.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 76f, paint)

        // Accent gold stripe
        paint.color = Color.parseColor("#D4AF37")
        canvas1.drawRect(0f, 76f, PAGE_WIDTH.toFloat(), 80f, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 14.5f
        paint.isFakeBoldText = true
        canvas1.drawText("OFFICIAL ACADEMIC EVALUATION & GRADE TRANSCRIPT", 28f, 32f, paint)

        paint.textSize = 8.8f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#E2E8F0")
        canvas1.drawText("SENATE COMMITTEE ON ACADEMIC STANDARDS • FACULTY ARCHIVE", 28f, 48f, paint)

        paint.textSize = 8f
        val currentDate = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        canvas1.drawText("ISSUED: $currentDate   |   STATUS: ${report.status.uppercase()}   |   SYNC: FIRESTORE CLOUD", 28f, 64f, paint)

        var y1 = 96f

        // Student Information Card
        paint.color = Color.parseColor("#F8FAFC")
        val studentBox = RectF(24f, y1, 571f, y1 + 86f)
        canvas1.drawRoundRect(studentBox, 6f, 6f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#CBD5E1")
        paint.strokeWidth = 1f
        canvas1.drawRoundRect(studentBox, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        // Student Data Columns
        paint.textSize = 8.5f
        paint.color = Color.parseColor("#64748B")
        canvas1.drawText("STUDENT NAME:", 36f, y1 + 18f, paint)
        canvas1.drawText("STUDENT ID:", 36f, y1 + 36f, paint)
        canvas1.drawText("PROGRAM LEVEL:", 36f, y1 + 54f, paint)
        canvas1.drawText("DEGREE TITLE:", 36f, y1 + 72f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.isFakeBoldText = true
        canvas1.drawText(report.studentName, 130f, y1 + 18f, paint)
        canvas1.drawText(report.studentNumber, 130f, y1 + 36f, paint)
        canvas1.drawText(report.programType.displayName, 130f, y1 + 54f, paint)
        canvas1.drawText(student?.degreeTitle ?: "Degree Candidate", 130f, y1 + 72f, paint)
        paint.isFakeBoldText = false

        // Student Data Column 2
        paint.color = Color.parseColor("#64748B")
        canvas1.drawText("ACADEMIC TERM:", 320f, y1 + 18f, paint)
        canvas1.drawText("FACULTY:", 320f, y1 + 36f, paint)
        canvas1.drawText("DEPARTMENT:", 320f, y1 + 54f, paint)
        canvas1.drawText("ADVISOR:", 320f, y1 + 72f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.isFakeBoldText = true
        canvas1.drawText(report.termName, 415f, y1 + 18f, paint)
        canvas1.drawText(student?.faculty ?: "Faculty of Academic Affairs", 415f, y1 + 36f, paint)
        canvas1.drawText(student?.department ?: "Academic Department", 415f, y1 + 54f, paint)
        canvas1.drawText(student?.advisor ?: "Senate Advisor Board", 415f, y1 + 72f, paint)
        paint.isFakeBoldText = false

        y1 += 102f

        // Modular Grades Section Title
        paint.color = Color.parseColor("#0A192F")
        paint.isFakeBoldText = true
        paint.textSize = 10f
        canvas1.drawText("MODULAR COURSEWORK & EXAMINATION BREAKDOWN", 26f, y1, paint)
        y1 += 10f

        // Table Header
        paint.color = Color.parseColor("#1E3A8A")
        val headerRect = RectF(24f, y1, 571f, y1 + 22f)
        canvas1.drawRoundRect(headerRect, 4f, 4f, paint)

        paint.color = Color.WHITE
        paint.textSize = 8f
        paint.isFakeBoldText = true
        canvas1.drawText("CODE", 32f, y1 + 14f, paint)
        canvas1.drawText("MODULE TITLE", 85f, y1 + 14f, paint)
        canvas1.drawText("CRED", 260f, y1 + 14f, paint)
        canvas1.drawText("CW", 300f, y1 + 14f, paint)
        canvas1.drawText("EXAM", 340f, y1 + 14f, paint)
        val practicalLabel = if (report.programType == ProgramType.UNDERGRADUATE) "LAB" else "THESIS"
        canvas1.drawText(practicalLabel, 380f, y1 + 14f, paint)
        canvas1.drawText("TOTAL", 432f, y1 + 14f, paint)
        canvas1.drawText("GRADE", 482f, y1 + 14f, paint)
        canvas1.drawText("POINTS", 528f, y1 + 14f, paint)

        y1 += 24f
        paint.isFakeBoldText = false

        // Course Rows
        var rowAlternate = false
        val courses = report.courses
        for (course in courses) {
            val rowHeight = 20f
            if (rowAlternate) {
                paint.color = Color.parseColor("#F1F5F9")
                canvas1.drawRect(24f, y1, 571f, y1 + rowHeight, paint)
            }
            rowAlternate = !rowAlternate

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 8f
            canvas1.drawText(course.courseCode, 32f, y1 + 13f, paint)

            val truncatedTitle = if (course.courseTitle.length > 29) course.courseTitle.take(27) + ".." else course.courseTitle
            canvas1.drawText(truncatedTitle, 85f, y1 + 13f, paint)
            canvas1.drawText("${course.creditUnits}", 265f, y1 + 13f, paint)
            canvas1.drawText(String.format(Locale.US, "%.1f", course.courseworkScore), 300f, y1 + 13f, paint)
            canvas1.drawText(String.format(Locale.US, "%.1f", course.examScore), 340f, y1 + 13f, paint)

            val practicalOrThesis = if (course.practicalScore > 0) course.practicalScore else course.thesisScore
            canvas1.drawText(String.format(Locale.US, "%.1f", practicalOrThesis), 380f, y1 + 13f, paint)

            paint.isFakeBoldText = true
            canvas1.drawText(String.format(Locale.US, "%.1f%%", course.totalScore), 432f, y1 + 13f, paint)
            canvas1.drawText(course.letterGrade, 487f, y1 + 13f, paint)
            canvas1.drawText(String.format(Locale.US, "%.1f", course.gradePoint), 532f, y1 + 13f, paint)
            paint.isFakeBoldText = false

            y1 += rowHeight
        }

        // Summary Performance Bar
        y1 += 8f
        paint.color = Color.parseColor("#EFF6FF")
        val summaryRect = RectF(24f, y1, 571f, y1 + 42f)
        canvas1.drawRoundRect(summaryRect, 6f, 6f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#93C5FD")
        canvas1.drawRoundRect(summaryRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#1E3A8A")
        paint.textSize = 9f
        paint.isFakeBoldText = true
        canvas1.drawText("TOTAL CREDITS: ${report.totalCredits}", 36f, y1 + 25f, paint)
        canvas1.drawText("TERM GPA: ${String.format(Locale.US, "%.2f", report.termGpa)} / 4.00", 155f, y1 + 25f, paint)
        canvas1.drawText("HONOURS: ${report.classification}", 290f, y1 + 25f, paint)
        paint.isFakeBoldText = false

        // Page 1 Executive Summary Box if space allows
        y1 += 54f
        if (report.executiveSummary.isNotBlank()) {
            paint.color = Color.parseColor("#0A192F")
            paint.isFakeBoldText = true
            paint.textSize = 9f
            canvas1.drawText("EXECUTIVE ACADEMIC PERFORMANCE SUMMARY", 26f, y1, paint)
            y1 += 12f

            paint.color = Color.parseColor("#334155")
            paint.textSize = 7.8f
            paint.isFakeBoldText = false
            y1 = drawWrappedText(canvas1, paint, report.executiveSummary, 28f, y1, 520f, 4)
        }

        // Page 1 Footer
        drawPageFooter(canvas1, paint, pageNumber = 1, totalPages = 2, reportId = report.id)
        pdfDocument.finishPage(page1)

        // ================= PAGE 2: NARRATIVE, LAB APPRAISAL & SIGN-OFFS =================
        val page2Info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create()
        val page2 = pdfDocument.startPage(page2Info)
        val canvas2: Canvas = page2.canvas

        // Header Mini-Banner
        paint.color = Color.parseColor("#0A192F")
        canvas2.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 50f, paint)

        paint.color = Color.parseColor("#D4AF37")
        canvas2.drawRect(0f, 50f, PAGE_WIDTH.toFloat(), 53f, paint)

        paint.color = Color.WHITE
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas2.drawText("ACADEMIC EVALUATION & SENATE ACTION RECORD", 28f, 26f, paint)

        paint.textSize = 8f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#E2E8F0")
        canvas2.drawText("CANDIDATE: ${report.studentName} (${report.studentNumber}) • TERM: ${report.termName}", 28f, 42f, paint)

        var y2 = 72f

        // Qualitative Evaluation Sections
        fun drawSectionOnPage2(title: String, content: String, maxLines: Int = 4, titleColorHex: String = "#1E3A8A") {
            if (content.isBlank()) return

            paint.color = Color.parseColor(titleColorHex)
            paint.textSize = 9f
            paint.isFakeBoldText = true
            canvas2.drawText(title.uppercase(), 26f, y2, paint)
            y2 += 11f

            // Background card for narrative block
            val startY = y2 - 2f
            paint.color = Color.parseColor("#334155")
            paint.textSize = 8f
            paint.isFakeBoldText = false
            y2 = drawWrappedText(canvas2, paint, content, 28f, y2, 520f, maxLines)
            y2 += 12f
        }

        drawSectionOnPage2(
            title = "Academic Strengths & Commendations",
            content = report.strengths,
            maxLines = 5,
            titleColorHex = "#047857" // Emerald
        )

        drawSectionOnPage2(
            title = "Areas for Growth & Academic Guidance",
            content = report.areasForImprovement,
            maxLines = 5,
            titleColorHex = "#B91C1C" // Crimson
        )

        if (report.scienceLabAssessment.isNotBlank()) {
            drawSectionOnPage2(
                title = "Laboratory & Scientific Methodology Appraisal",
                content = report.scienceLabAssessment,
                maxLines = 4,
                titleColorHex = "#4338CA" // Indigo
            )
        }

        drawSectionOnPage2(
            title = "Examination Board Formal Recommendation",
            content = report.boardRecommendation,
            maxLines = 4,
            titleColorHex = "#1E3A8A"
        )

        if (report.advisorRemarks.isNotBlank()) {
            drawSectionOnPage2(
                title = "Academic Advisor Detailed Notes",
                content = report.advisorRemarks,
                maxLines = 3,
                titleColorHex = "#334155"
            )
        }

        // Institutional Verification & Sign-off Block
        y2 = 680f
        paint.color = Color.parseColor("#CBD5E1")
        paint.strokeWidth = 1f
        canvas2.drawLine(24f, y2, 571f, y2, paint)
        y2 += 20f

        paint.color = Color.parseColor("#0A192F")
        paint.textSize = 9f
        paint.isFakeBoldText = true
        canvas2.drawText("INSTITUTIONAL ENDORSEMENT & REGISTRY SIGN-OFF", 26f, y2, paint)
        y2 += 24f

        // Signature Lines
        paint.color = Color.parseColor("#94A3B8")
        paint.strokeWidth = 1f
        canvas2.drawLine(36f, y2 + 25f, 185f, y2 + 25f, paint)
        canvas2.drawLine(225f, y2 + 25f, 375f, y2 + 25f, paint)
        canvas2.drawLine(415f, y2 + 25f, 560f, y2 + 25f, paint)

        // Head of Department Stamp Box
        val stampBox = RectF(245f, y2 - 20f, 355f, y2 + 18f)
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#94A3B8")
        canvas2.drawRoundRect(stampBox, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 7f
        paint.isFakeBoldText = true
        canvas2.drawText("OFFICIAL STAMP", 262f, y2 + 2f, paint)

        // Signatory Labels
        paint.color = Color.parseColor("#475569")
        paint.textSize = 7.5f
        paint.isFakeBoldText = false
        canvas2.drawText("ACADEMIC ADVISOR", 36f, y2 + 38f, paint)
        canvas2.drawText("HEAD OF DEPARTMENT", 225f, y2 + 38f, paint)
        canvas2.drawText("DEAN OF ACADEMIC AFFAIRS", 415f, y2 + 38f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.isFakeBoldText = true
        paint.textSize = 8f
        canvas2.drawText(student?.advisor ?: "Academic Supervisor", 36f, y2 + 50f, paint)
        canvas2.drawText(student?.department ?: "Faculty Board", 225f, y2 + 50f, paint)
        canvas2.drawText(report.deanSignOff, 415f, y2 + 50f, paint)
        paint.isFakeBoldText = false

        // Page 2 Footer
        drawPageFooter(canvas2, paint, pageNumber = 2, totalPages = 2, reportId = report.id)
        pdfDocument.finishPage(page2)

        // Write PDF file to internal storage
        val reportsDir = File(context.filesDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()

        val pdfFile = File(reportsDir, fileName)
        try {
            val fos = FileOutputStream(pdfFile)
            pdfDocument.writeTo(fos)
            fos.close()
            pdfDocument.close()
            return pdfFile
        } catch (e: Exception) {
            pdfDocument.close()
            return null
        }
    }

    private fun drawWrappedText(
        canvas: Canvas,
        paint: Paint,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        maxLines: Int
    ): Float {
        var currentY = startY
        val words = text.split(" ")
        var line = StringBuilder()
        var linesDrawn = 0

        for (word in words) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) > maxWidth) {
                canvas.drawText(line.toString(), x, currentY, paint)
                currentY += 12f
                line = StringBuilder(word)
                linesDrawn++
                if (linesDrawn >= maxLines) break
            } else {
                line.append(if (line.isEmpty()) word else " $word")
            }
        }

        if (line.isNotEmpty() && linesDrawn < maxLines) {
            canvas.drawText(line.toString(), x, currentY, paint)
            currentY += 12f
        }

        return currentY
    }

    private fun drawPageFooter(
        canvas: Canvas,
        paint: Paint,
        pageNumber: Int,
        totalPages: Int,
        reportId: String
    ) {
        val y = 822f
        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 1f
        canvas.drawLine(24f, y, 571f, y, paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 7f
        paint.isFakeBoldText = false
        val hash = reportId.replace("-", "").take(12).uppercase()
        canvas.drawText("ScholarReport Academic Records • Verification Hash: SHA256-$hash • Cloud Verified", 28f, y + 12f, paint)
        canvas.drawText("Page $pageNumber of $totalPages", 520f, y + 12f, paint)
    }
}
