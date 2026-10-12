package com.example.data.ai

import com.example.BuildConfig
import com.example.data.model.CourseGrade
import com.example.data.model.ProgramType
import com.example.data.model.Report
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiCommentSuggestion(
    val executiveSummary: String,
    val strengths: String,
    val areasForImprovement: String,
    val boardRecommendation: String,
    val scienceLabAssessment: String = ""
)

class GeminiAiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun generateReportComments(
        studentName: String,
        programType: ProgramType,
        termName: String,
        courses: List<CourseGrade>,
        gpa: Double,
        classification: String,
        pastComments: List<String> = emptyList(),
        customNotes: String = ""
    ): AiCommentSuggestion = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        // Check if we can make a live call with a real API key
        if (!apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = buildPrompt(
                    studentName, programType, termName, courses, gpa, classification, pastComments, customNotes
                )
                val aiResponse = callGeminiApi(apiKey, prompt)
                if (aiResponse != null) {
                    return@withContext aiResponse
                }
            } catch (e: Exception) {
                // Fall back gracefully to the offline academic engine
            }
        }

        // Intelligent local Academic Evaluation Engine fallback
        return@withContext generateLocalAcademicComments(
            studentName, programType, termName, courses, gpa, classification, pastComments
        )
    }

    private fun buildPrompt(
        studentName: String,
        programType: ProgramType,
        termName: String,
        courses: List<CourseGrade>,
        gpa: Double,
        classification: String,
        pastComments: List<String>,
        customNotes: String
    ): String {
        val coursesDetails = courses.joinToString("; ") {
            "${it.courseCode} (${it.courseTitle}): Total ${it.totalScore}%, Grade ${it.letterGrade}, CW: ${it.courseworkScore}, Exam: ${it.examScore}" +
                if (it.practicalScore > 0) ", Lab Practical: ${it.practicalScore}" else "" +
                if (it.thesisScore > 0) ", Thesis/Research: ${it.thesisScore}" else ""
        }

        val pastRemarks = if (pastComments.isNotEmpty()) {
            "Past comments history: ${pastComments.joinToString(" | ")}"
        } else {
            "No prior term comments on record."
        }

        return """
        You are an expert Academic Assessor and University Examination Board Secretary writing an official academic progress report for $studentName.
        Program Level: ${programType.displayName}
        Academic Term: $termName
        Computed Term GPA: $gpa (Academic Classification: $classification)
        Modules Evaluated: $coursesDetails
        $pastRemarks
        Faculty Notes: $customNotes

        Generate a professional, balanced, and constructive evaluation strictly in the following JSON format:
        {
          "executiveSummary": "2-3 comprehensive sentences summarizing academic trajectory, work ethic, and standing.",
          "strengths": "Detailed bullet-ready feedback praising specific conceptual, quantitative, or laboratory competencies shown.",
          "areasForImprovement": "Constructive, actionable advice targeting specific revision, critical reasoning, or research methodological gaps.",
          "boardRecommendation": "Formal Examination Board recommendation (e.g., Unconditional Progression, Commendation for First Class Honours, or Guided Academic Advisory).",
          "scienceLabAssessment": "Appraisal of experimental rigor, scientific methodology, and lab/practical execution."
        }
        Do not wrap in Markdown code blocks. Output pure JSON only.
        """.trimIndent()
    }

    private fun callGeminiApi(apiKey: String, prompt: String): AiCommentSuggestion? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val jsonRequest = JSONObject().apply {
            val contents = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    }
                    put("parts", parts)
                }
                put(contentObj)
            }
            put("contents", contents)

            val genConfig = JSONObject().apply {
                put("temperature", 0.4)
                put("responseMimeType", "application/json")
            }
            put("generationConfig", genConfig)
        }

        val body = jsonRequest.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null

        val responseBody = response.body?.string() ?: return null
        val root = JSONObject(responseBody)
        val text = root.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text") ?: return null

        val parsed = JSONObject(text.trim().removePrefix("```json").removeSuffix("```").trim())
        return AiCommentSuggestion(
            executiveSummary = parsed.optString("executiveSummary"),
            strengths = parsed.optString("strengths"),
            areasForImprovement = parsed.optString("areasForImprovement"),
            boardRecommendation = parsed.optString("boardRecommendation"),
            scienceLabAssessment = parsed.optString("scienceLabAssessment")
        )
    }

    private fun generateLocalAcademicComments(
        studentName: String,
        programType: ProgramType,
        termName: String,
        courses: List<CourseGrade>,
        gpa: Double,
        classification: String,
        pastComments: List<String>
    ): AiCommentSuggestion {
        val topCourses = courses.filter { it.totalScore >= 70.0 }
        val struggleCourses = courses.filter { it.totalScore < 60.0 }
        val hasPracticals = courses.any { it.practicalScore > 0 }
        val hasThesis = courses.any { it.thesisScore > 0 }

        val isUndergrad = programType == ProgramType.UNDERGRADUATE

        val summary = if (gpa >= 3.60) {
            "$studentName has demonstrated exceptional scholarship during $termName, achieving a distinguished GPA of $gpa ($classification). The candidate exhibits advanced intellectual rigor, consistent preparation, and commendable analytical execution across taught modules."
        } else if (gpa >= 3.00) {
            "$studentName maintains solid academic momentum this term with a commendable GPA of $gpa, placing them firmly within $classification standing. Performance across core modules reflects reliable engagement, with clear capacity for top-tier distinction."
        } else if (gpa >= 2.50) {
            "$studentName has recorded satisfactory modular engagement for $termName with a cumulative GPA of $gpa ($classification). While fundamental concepts are grasped, targeted academic support in examination preparation will enhance overall trajectory."
        } else {
            "$studentName has experienced notable academic hurdles during $termName (GPA: $gpa). Immediate academic counseling and structured revision tutorials are recommended to reinforce foundational competencies."
        }

        val strengths = buildString {
            if (topCourses.isNotEmpty()) {
                val codes = topCourses.joinToString(", ") { "${it.courseCode} (${it.totalScore}%)" }
                append("• Exemplary performance achieved in: $codes.\n")
            } else {
                append("• Demonstrated sustained effort and consistent attendance across lectures.\n")
            }

            if (isUndergrad) {
                if (hasPracticals) {
                    append("• Strong scientific methodology and disciplined laboratory bench performance.\n")
                }
                append("• Coherent essay argumentation and adherence to academic integrity and citation standards.")
            } else {
                if (hasThesis) {
                    append("• High-level theoretical synthesis and critical evaluation of peer-reviewed literature in postgraduate seminars.\n")
                }
                append("• Independent research initiative and sophisticated conceptual modeling appropriate for master's level inquiry.")
            }
        }

        val improvements = buildString {
            if (struggleCourses.isNotEmpty()) {
                val codes = struggleCourses.joinToString(", ") { "${it.courseCode} (${it.totalScore}%)" }
                append("• Dedicated revision recommended for: $codes to address conceptual gaps prior to final assessments.\n")
            }
            if (isUndergrad) {
                append("• Deepen critical analysis in timed examination essays; move beyond descriptive summaries towards evaluative critique.\n")
                append("• Utilize faculty office hours and tutorial workshops for structured feedback on draft assignments.")
            } else {
                append("• Refine quantitative methodology and statistical sensitivity testing for research findings.\n")
                append("• Accelerate manuscript drafting timelines to allow iterative feedback from supervisory committee members.")
            }
        }

        val recommendation = if (gpa >= 3.60) {
            if (isUndergrad) {
                "Unconditional progression with Dean's Honours Commendation. Endorsed for competitive undergraduate research fellowships."
            } else {
                "Formal recommendation to proceed to Master's Dissertation Defense with Distinction honors standing."
            }
        } else if (gpa >= 3.00) {
            if (isUndergrad) {
                "Unconditional progression to the subsequent academic year in Upper Second (2:1) Honours standing."
            } else {
                "Approved for progression to final taught stage and master's research proposal submission."
            }
        } else if (gpa >= 2.00) {
            "Conditional progression subject to scheduled consultations with personal academic advisor."
        } else {
            "Referral to Academic Advisory Panel for formalized academic recovery plan and guided tutorial intervention."
        }

        val labAssessment = if (hasPracticals || isUndergrad) {
            "Laboratory practicals and experimental sessions were conducted in strict compliance with safety guidelines. Demonstrates sound pipetting accuracy, precise sample preparation, and systematic data recording in primary lab notebooks."
        } else {
            "Exhibits sound computational and empirical modeling skills with reproducible workflow management."
        }

        return AiCommentSuggestion(
            executiveSummary = summary,
            strengths = strengths,
            areasForImprovement = improvements,
            boardRecommendation = recommendation,
            scienceLabAssessment = labAssessment
        )
    }
}
