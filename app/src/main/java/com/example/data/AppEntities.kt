package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val teacherName: String = "",
    val schoolName: String = "",
    val activeLanguage: String = "fr", // "fr" or "ar"
    val activeTheme: String = "system", // "light", "dark", "system"
    val activeFont: String = "Times New Roman", // "Times New Roman", "Lora", "Arial"
    val fontSizeScale: String = "small", // "small", "medium", "large"
    val pinCode: String? = null,
    val isBiometricEnabled: Boolean = false,
    val gradeCoefficientsJson: String = "{\"participation\":1,\"discipline\":1,\"assiduity\":1,\"notebook\":1,\"exams\":2,\"final_exam\":3}",
    val currentSchoolYear: String = "",
    val schoolYearsJson: String = "[]",
    val archivedSchoolYearsJson: String = "[]",
    val taughtSubject: String = "",
    val profilePictureUri: String? = null,
    val subjectCoefficient: Int = 3,
    val teacherFreeDay: Int? = null, // 1 (Sun) to 5 (Thu), null if none
    val isFontBold: Boolean = false,
    val themeColor: String = "default",
    val educationLevel: String = "SECONDAIRE",
    val hasCompletedOnboarding: Boolean = false,
    val teacherTitle: String = "",
    val profileAnimationEffect: String = "autumn_tree" // "autumn_tree" or "none"
)

@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val schoolYear: String = "",
    val subjectCoefficient: Int = 3
)

@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["classId"])]
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: Int,
    val lastName: String,
    val firstName: String,
    val birthDate: String, // YYYY-MM-DD
    val gender: String, // "M" or "F"
    val observations: String = "",
    val birthPlace: String = "",
    val goodToKnow: String = "",
    val seatIndex: Int? = null,
    val orderIndex: Int = 0,
    val repeatedYears: String = "", // "1AS", "2AS", "3AS", or combinations like "1AS, 2AS"
    val groupName: String = "" // "G1", "G2", etc.
)

@Entity(
    tableName = "grades",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["studentId", "term"], unique = true)]
)
data class GradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val term: Int, // 1, 2, or 3
    val participation: Float? = null,
    val discipline: Float? = null,
    val assiduity: Float? = null,
    val notebook: Float? = null,
    val project: Float? = null,
    val exam1: Float? = null,
    val exam2: Float? = null,
    val exam3: Float? = null,
    val finalExam: Float? = null
)

@Entity(
    tableName = "journal_entries",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["classId"])]
)
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // YYYY-MM-DD
    val timeSlot: String, // e.g., "08:00 - 09:00"
    val courseSubject: String,
    val classId: Int,
    val objectives: String,
    val observations: String,
    val photoPath: String? = null,
    val audioPath: String? = null,
    val activity: String = "",
    val lessonTitle: String = ""
)

@Entity(
    tableName = "timetable_slots",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["classId"])]
)
data class TimetableSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: Int?, // Nullable for general teacher-only consolidated slots
    val dayOfWeek: Int, // 1 (Mon) to 6 (Sat)
    val timeSlot: String, // e.g., "08:00 - 09:00"
    val subject: String,
    val classroom: String,
    val colorHex: String // e.g., "#1F3A60"
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:MM
    val isNotificationEnabled: Boolean = true,
    val isCompleted: Boolean = false
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "document_categories")
data class DocumentCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val parentCategoryId: Int? = null
)

@Entity(
    tableName = "documents",
    foreignKeys = [
        ForeignKey(
            entity = DocumentCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int,
    val title: String,
    val fileUri: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attendances",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["studentId"]),
        Index(value = ["classId"]),
        Index(value = ["date"]),
        Index(value = ["studentId", "date", "timeSlot"], unique = true)
    ]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val classId: Int,
    val date: String, // YYYY-MM-DD
    val timeSlot: String, // e.g., "08:00 - 09:00"
    val subject: String = "",
    val status: String = "ABSENT", // "ABSENT", "RETARD", "PRESENT", "EXCLUDED"
    val isJustified: Boolean = false,
    val delayMinutes: Int = 0,
    val reason: String = "", // Motif
    val recordedAt: Long = System.currentTimeMillis()
)
