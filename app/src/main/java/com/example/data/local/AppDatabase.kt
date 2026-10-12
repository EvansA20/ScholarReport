package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ProgramType
import com.example.data.model.TemplateSection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [StudentEntity::class, ReportEntity::class, TemplateEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun reportDao(): ReportDao
    abstract fun templateDao(): TemplateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scholar_report_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default templates only - no sample pre-data
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).seedInitialData()
                            }
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // Ensure any pre-data from previous builds is immediately purged
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).deletePreData()
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun deletePreData() {
        try {
            studentDao().deletePreDataStudents()
            reportDao().deletePreDataReports()
        } catch (_: Exception) {}
    }

    suspend fun seedInitialData() {
        val templateDao = templateDao()

        // Seed Only Standard Institutional Report Templates
        val ugStemTemplate = TemplateEntity(
            id = "template_ug_stem",
            title = "Undergraduate STEM & Science Lab Report",
            programType = ProgramType.UNDERGRADUATE.name,
            description = "Tailored for Science, Technology, and Engineering programs requiring laboratory practicals and experimental methodology evaluation.",
            sectionsJson = JsonConverters.templateSectionsToJson(
                listOf(
                    TemplateSection(
                        id = UUID.randomUUID().toString(),
                        title = "Executive Academic Summary",
                        promptCue = "Synthesize modular grade scores and overall academic momentum for this semester."
                    ),
                    TemplateSection(
                        id = UUID.randomUUID().toString(),
                        title = "Science Laboratory & Practical Competency",
                        promptCue = "Evaluate scientific rigor, adherence to lab safety, experimental data accuracy, and hypothesis formulation."
                    ),
                    TemplateSection(
                        id = UUID.randomUUID().toString(),
                        title = "Analytical Strengths & Commendations",
                        promptCue = "Highlight specific quantitative or conceptual mastery shown by the student."
                    ),
                    TemplateSection(
                        id = UUID.randomUUID().toString(),
                        title = "Areas for Growth & Academic Support",
                        promptCue = "Target specific mathematical, programming, or lab skills needing guided tutorial support."
                    ),
                    TemplateSection(
                        id = UUID.randomUUID().toString(),
                        title = "Faculty Examination Board Recommendation",
                        promptCue = "Official progression status (e.g. Unconditional progression to Level 400 Honours Project)."
                    )
                )
            ),
            defaultNotes = "All laboratory practicals must meet ISO 17025 academic safety benchmarks.",
            isSystemDefault = true,
            updatedAt = System.currentTimeMillis()
        )

        val ugStandardTemplate = TemplateEntity(
            id = "template_ug_standard",
            title = "Undergraduate Standard Honours Degree Report",
            programType = ProgramType.UNDERGRADUATE.name,
            description = "Standard institutional template for Bachelor's degree candidates across Humanities, Business, and Social Sciences.",
            sectionsJson = JsonConverters.templateSectionsToJson(
                listOf(
                    TemplateSection(
                        id = UUID.randomUUID().toString(),
                        title = "Executive Academic Summary",
                        promptCue = "Overall assessment of performance against degree benchmark statements."
                    ),
                    TemplateSection(
                        id = UUID.randomUUID().toString(),
                        title = "Modular Performance & Critical Analysis",
                        promptCue = "Analysis of coursework submissions, seminar participation, and examination results."
                    ),
                    TemplateSection(
                        id = UUID.randomUUID().toString(),
                        title = "Key Strengths & Merits",
                        promptCue = "Commendable academic achievements and high-scoring modules."
                    ),
                    TemplateSection(
                        id = UUID.randomUUID().toString(),
                        title = "Developmental Guidance",
                        promptCue = "Actionable suggestions for essay structuring, citation practices, and revision strategies."
                    ),
                    TemplateSection(
                        id = UUID.randomUUID().toString(),
                        title = "Degree Classification Trajectory & Recommendation",
                        promptCue = "Current honours band standing and formal board decision."
                    )
                )
            ),
            defaultNotes = "Approved by Senate Academic Quality Assurance Committee.",
            isSystemDefault = true,
            updatedAt = System.currentTimeMillis()
        )

        val pgMasterTemplate = TemplateEntity(
            id = "template_pg_master",
            title = "Postgraduate Master's Modular & Research Report",
            programType = ProgramType.POSTGRADUATE.name,
            description = "Comprehensive evaluation for MSc, MA, and MRes students covering taught modules, research methodology, and seminar contributions.",
            sectionsJson = JsonConverters.templateSectionsToJson(
                listOf(
                    TemplateSection(
                        id = UUID.randomUUID().toString(),
                        title = "Executive Summary & Postgraduate Standing",
                        promptCue = "Evaluation of master's level critical inquiry, theoretical depth, and grade averages."
                    ),
                    TemplateSection(
                        id = UUID.randomUUID().toString(),
                        title = "Advanced Research Competence & Methodology",
                        promptCue = "Appraisal of quantitative/qualitative methodologies, literature synthesis, and analytical independence."
                    ),
                    TemplateSection(
                        id = UUID.randomUUID().toString(),
                        title = "Scholarly Strengths",
                        promptCue = "Originality of arguments, seminar presentations, and publication-standard writing."
                    ),
                    TemplateSection(
                        id = UUID.randomUUID().toString(),
                        title = "Areas for Critical Improvement",
                        promptCue = "Nuances in methodology, statistical power, or conceptual frameworks."
                    ),
                    TemplateSection(
                        id = UUID.randomUUID().toString(),
                        title = "Dissertation Milestone & Committee Recommendation",
                        promptCue = "Readiness to proceed to final master's dissertation / thesis defense with Distinction/Merit status."
                    )
                )
            ),
            defaultNotes = "Postgraduate grading threshold: 50% Pass, 60% Merit, 70% Distinction.",
            isSystemDefault = true,
            updatedAt = System.currentTimeMillis()
        )

        templateDao.insertTemplates(listOf(ugStemTemplate, ugStandardTemplate, pgMasterTemplate))
    }
}
