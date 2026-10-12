package com.example.data.firebase

import com.example.data.local.AppDatabase
import com.example.data.local.JsonConverters
import com.example.data.local.ReportEntity
import com.example.data.local.StudentEntity
import com.example.data.model.CourseGrade
import com.example.data.model.ProgramType
import com.example.data.model.Report
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class SyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}

class FirestoreSyncManager(
    private val database: AppDatabase
) {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState

    private val _lastSyncMessage = MutableStateFlow<String?>("Ready for cloud synchronization")
    val lastSyncMessage: StateFlow<String?> = _lastSyncMessage

    suspend fun syncAllData(): Boolean = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) {
            _syncState.value = SyncState.ERROR
            _lastSyncMessage.value = "Sign in with Google to enable cloud team synchronization."
            return@withContext false
        }

        _syncState.value = SyncState.SYNCING
        _lastSyncMessage.value = "Synchronizing students and academic reports with department cloud..."

        try {
            // 1. Push local students to Firestore
            val studentDao = database.studentDao()
            val reportDao = database.reportDao()

            val localStudents = mutableListOf<StudentEntity>()
            // Collect current snapshot of students
            // Room Flow first emission:
            val studentsSnapshot = firestore.collection("students").get().await()
            for (doc in studentsSnapshot.documents) {
                val id = doc.getString("id") ?: doc.id
                val entity = StudentEntity(
                    id = id,
                    studentNumber = doc.getString("studentNumber") ?: "",
                    fullName = doc.getString("fullName") ?: "",
                    email = doc.getString("email") ?: "",
                    programType = doc.getString("programType") ?: "UNDERGRADUATE",
                    faculty = doc.getString("faculty") ?: "",
                    department = doc.getString("department") ?: "",
                    degreeTitle = doc.getString("degreeTitle") ?: "",
                    currentLevel = doc.getString("currentLevel") ?: "",
                    advisor = doc.getString("advisor") ?: "",
                    overallGpa = doc.getDouble("overallGpa") ?: 0.0,
                    notes = doc.getString("notes") ?: "",
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
                studentDao.insertStudent(entity)
            }

            // Push reports from cloud
            val reportsSnapshot = firestore.collection("reports").get().await()
            for (doc in reportsSnapshot.documents) {
                val id = doc.getString("id") ?: doc.id
                val entity = ReportEntity(
                    id = id,
                    studentId = doc.getString("studentId") ?: "",
                    studentName = doc.getString("studentName") ?: "",
                    studentNumber = doc.getString("studentNumber") ?: "",
                    programType = doc.getString("programType") ?: "UNDERGRADUATE",
                    termName = doc.getString("termName") ?: "",
                    templateId = doc.getString("templateId") ?: "",
                    date = doc.getString("date") ?: "",
                    coursesJson = doc.getString("coursesJson") ?: "[]",
                    executiveSummary = doc.getString("executiveSummary") ?: "",
                    strengths = doc.getString("strengths") ?: "",
                    areasForImprovement = doc.getString("areasForImprovement") ?: "",
                    boardRecommendation = doc.getString("boardRecommendation") ?: "",
                    advisorRemarks = doc.getString("advisorRemarks") ?: "",
                    scienceLabAssessment = doc.getString("scienceLabAssessment") ?: "",
                    deanSignOff = doc.getString("deanSignOff") ?: "",
                    status = doc.getString("status") ?: "Draft",
                    syncStatus = "Synced",
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
                reportDao.insertReport(entity)
            }

            _syncState.value = SyncState.SUCCESS
            _lastSyncMessage.value = "Cloud synchronization complete. All records up to date."
            true
        } catch (e: Exception) {
            _syncState.value = SyncState.ERROR
            _lastSyncMessage.value = "Cloud sync error: ${e.message}"
            false
        }
    }

    suspend fun uploadReport(report: ReportEntity): Boolean = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext false
        try {
            val map = hashMapOf(
                "id" to report.id,
                "userId" to user.uid,
                "authorEmail" to (user.email ?: ""),
                "studentId" to report.studentId,
                "studentName" to report.studentName,
                "studentNumber" to report.studentNumber,
                "programType" to report.programType,
                "termName" to report.termName,
                "templateId" to report.templateId,
                "date" to report.date,
                "coursesJson" to report.coursesJson,
                "executiveSummary" to report.executiveSummary,
                "strengths" to report.strengths,
                "areasForImprovement" to report.areasForImprovement,
                "boardRecommendation" to report.boardRecommendation,
                "advisorRemarks" to report.advisorRemarks,
                "scienceLabAssessment" to report.scienceLabAssessment,
                "deanSignOff" to report.deanSignOff,
                "status" to report.status,
                "updatedAt" to report.updatedAt
            )
            firestore.collection("reports").document(report.id).set(map, SetOptions.merge()).await()
            database.reportDao().updateReport(report.copy(syncStatus = "Synced"))
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun uploadStudent(student: StudentEntity): Boolean = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext false
        try {
            val map = hashMapOf(
                "id" to student.id,
                "userId" to user.uid,
                "studentNumber" to student.studentNumber,
                "fullName" to student.fullName,
                "email" to student.email,
                "programType" to student.programType,
                "faculty" to student.faculty,
                "department" to student.department,
                "degreeTitle" to student.degreeTitle,
                "currentLevel" to student.currentLevel,
                "advisor" to student.advisor,
                "overallGpa" to student.overallGpa,
                "notes" to student.notes,
                "updatedAt" to student.updatedAt
            )
            firestore.collection("students").document(student.id).set(map, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getRecentReportsFlow(): Flow<List<Report>> = callbackFlow {
        val user = auth.currentUser
        if (user == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("reports")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val reports = snapshot.documents.mapNotNull { doc ->
                        try {
                            val coursesJson = doc.getString("coursesJson") ?: "[]"
                            Report(
                                id = doc.getString("id") ?: doc.id,
                                studentId = doc.getString("studentId") ?: "",
                                studentName = doc.getString("studentName") ?: "",
                                studentNumber = doc.getString("studentNumber") ?: "",
                                programType = try {
                                    ProgramType.valueOf(doc.getString("programType") ?: "UNDERGRADUATE")
                                } catch (_: Exception) {
                                    ProgramType.UNDERGRADUATE
                                },
                                termName = doc.getString("termName") ?: "",
                                templateId = doc.getString("templateId") ?: "",
                                date = doc.getString("date") ?: "",
                                courses = JsonConverters.jsonToCourseGrades(coursesJson),
                                executiveSummary = doc.getString("executiveSummary") ?: "",
                                strengths = doc.getString("strengths") ?: "",
                                areasForImprovement = doc.getString("areasForImprovement") ?: "",
                                boardRecommendation = doc.getString("boardRecommendation") ?: "",
                                advisorRemarks = doc.getString("advisorRemarks") ?: "",
                                scienceLabAssessment = doc.getString("scienceLabAssessment") ?: "",
                                deanSignOff = doc.getString("deanSignOff") ?: "",
                                status = doc.getString("status") ?: "Draft",
                                syncStatus = "Synced",
                                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }
                    trySend(reports)
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun fetchRecentReports(): List<Report> = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext emptyList()
        try {
            val snapshot = firestore.collection("reports")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    val coursesJson = doc.getString("coursesJson") ?: "[]"
                    Report(
                        id = doc.getString("id") ?: doc.id,
                        studentId = doc.getString("studentId") ?: "",
                        studentName = doc.getString("studentName") ?: "",
                        studentNumber = doc.getString("studentNumber") ?: "",
                        programType = try {
                            ProgramType.valueOf(doc.getString("programType") ?: "UNDERGRADUATE")
                        } catch (_: Exception) {
                            ProgramType.UNDERGRADUATE
                        },
                        termName = doc.getString("termName") ?: "",
                        templateId = doc.getString("templateId") ?: "",
                        date = doc.getString("date") ?: "",
                        courses = JsonConverters.jsonToCourseGrades(coursesJson),
                        executiveSummary = doc.getString("executiveSummary") ?: "",
                        strengths = doc.getString("strengths") ?: "",
                        areasForImprovement = doc.getString("areasForImprovement") ?: "",
                        boardRecommendation = doc.getString("boardRecommendation") ?: "",
                        advisorRemarks = doc.getString("advisorRemarks") ?: "",
                        scienceLabAssessment = doc.getString("scienceLabAssessment") ?: "",
                        deanSignOff = doc.getString("deanSignOff") ?: "",
                        status = doc.getString("status") ?: "Draft",
                        syncStatus = "Synced",
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    )
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
