package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: String,
    val studentNumber: String,
    val fullName: String,
    val email: String,
    val programType: String,
    val faculty: String,
    val department: String,
    val degreeTitle: String,
    val currentLevel: String,
    val advisor: String,
    val overallGpa: Double,
    val notes: String,
    val updatedAt: Long
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val studentId: String,
    val studentName: String,
    val studentNumber: String,
    val programType: String,
    val termName: String,
    val templateId: String,
    val date: String,
    val coursesJson: String,
    val executiveSummary: String,
    val strengths: String,
    val areasForImprovement: String,
    val boardRecommendation: String,
    val advisorRemarks: String,
    val scienceLabAssessment: String,
    val deanSignOff: String,
    val status: String,
    val syncStatus: String,
    val updatedAt: Long
)

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val title: String,
    val programType: String,
    val description: String,
    val sectionsJson: String,
    val defaultNotes: String,
    val isSystemDefault: Boolean,
    val updatedAt: Long
)
