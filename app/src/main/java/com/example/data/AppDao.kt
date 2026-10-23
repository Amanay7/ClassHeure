package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class StudentWithGrade(
    val studentId: Int,
    val classId: Int,
    val lastName: String,
    val firstName: String,
    val birthDate: String,
    val gender: String,
    val term: Int?,
    val gradeId: Int?,
    val participation: Float?,
    val discipline: Float?,
    val assiduity: Float?,
    val notebook: Float?,
    val project: Float?,
    val exam1: Float?,
    val exam2: Float?,
    val exam3: Float?,
    val finalExam: Float?,
    val orderIndex: Int = 0,
    val repeatedYears: String = "",
    val groupName: String = ""
)

@Dao
interface AppDao {

    // --- Settings ---
    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettingsFlow(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun getSettings(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: SettingsEntity)

    // --- Classes ---
    @Query("SELECT * FROM classes ORDER BY name ASC")
    fun getAllClassesFlow(): Flow<List<ClassEntity>>

    @Query("SELECT * FROM classes ORDER BY name ASC")
    suspend fun getAllClasses(): List<ClassEntity>

    @Query("SELECT * FROM classes WHERE id = :id")
    suspend fun getClassById(id: Int): ClassEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(classEntity: ClassEntity): Long

    @Update
    suspend fun updateClass(classEntity: ClassEntity)

    @Delete
    suspend fun deleteClass(classEntity: ClassEntity)

    // --- Students ---
    @Query("SELECT * FROM students ORDER BY (CASE WHEN orderIndex > 0 THEN orderIndex ELSE 999999 END) ASC, lastName ASC, firstName ASC")
    fun getAllStudentsFlow(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY (CASE WHEN orderIndex > 0 THEN orderIndex ELSE 999999 END) ASC, lastName ASC, firstName ASC")
    fun getStudentsByClassFlow(classId: Int): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY (CASE WHEN orderIndex > 0 THEN orderIndex ELSE 999999 END) ASC, lastName ASC, firstName ASC")
    suspend fun getStudentsByClass(classId: Int): List<StudentEntity>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudentById(id: Int): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Update
    suspend fun updateStudents(students: List<StudentEntity>)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)

    @Query("UPDATE students SET seatIndex = NULL WHERE classId = :classId AND seatIndex = :seatIndex")
    suspend fun clearSeatForClass(classId: Int, seatIndex: Int)

    @Query("UPDATE students SET seatIndex = NULL WHERE classId = :classId")
    suspend fun clearAllSeatsForClass(classId: Int)

    // --- Grades ---
    @Query("""
        SELECT s.id AS studentId, s.classId, s.lastName, s.firstName, s.birthDate, s.gender, 
               g.term, g.id AS gradeId, g.participation, g.discipline, g.assiduity, g.notebook, 
               g.project, g.exam1, g.exam2, g.exam3, g.finalExam,
               s.orderIndex, s.repeatedYears, s.groupName
        FROM students s 
        LEFT JOIN grades g ON s.id = g.studentId AND g.term = :term 
        WHERE s.classId = :classId
        ORDER BY (CASE WHEN s.orderIndex > 0 THEN s.orderIndex ELSE 999999 END) ASC, s.lastName ASC, s.firstName ASC
    """)
    fun getStudentsWithGradesForTermFlow(classId: Int, term: Int): Flow<List<StudentWithGrade>>

    @Query("""
        SELECT s.id AS studentId, s.classId, s.lastName, s.firstName, s.birthDate, s.gender, 
               g.term, g.id AS gradeId, g.participation, g.discipline, g.assiduity, g.notebook, 
               g.project, g.exam1, g.exam2, g.exam3, g.finalExam,
               s.orderIndex, s.repeatedYears, s.groupName
        FROM students s 
        LEFT JOIN grades g ON s.id = g.studentId AND g.term = :term 
        WHERE s.classId = :classId
        ORDER BY (CASE WHEN s.orderIndex > 0 THEN s.orderIndex ELSE 999999 END) ASC, s.lastName ASC, s.firstName ASC
    """)
    suspend fun getStudentsWithGradesForTerm(classId: Int, term: Int): List<StudentWithGrade>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: GradeEntity): Long

    @Query("DELETE FROM grades WHERE studentId = :studentId")
    suspend fun deleteGradesByStudent(studentId: Int)

    @Query("SELECT * FROM grades WHERE studentId = :studentId AND term = :term")
    suspend fun getGradeForStudentAndTerm(studentId: Int, term: Int): GradeEntity?

    // --- Journal Entries (Cahier Journal) ---
    @Query("SELECT * FROM journal_entries ORDER BY id DESC")
    fun getAllJournalEntriesFlow(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE classId = :classId ORDER BY id DESC")
    fun getJournalEntriesByClassFlow(classId: Int): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, timeSlot ASC")
    fun getJournalEntriesBetweenDatesFlow(startDate: String, endDate: String): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, timeSlot ASC")
    suspend fun getJournalEntriesBetweenDates(startDate: String, endDate: String): List<JournalEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntryEntity): Long

    @Update
    suspend fun updateJournalEntry(entry: JournalEntryEntity)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteJournalEntry(id: Int)

    // --- Timetable Slots (Emplois du temps) ---
    @Query("SELECT * FROM timetable_slots WHERE dayOfWeek BETWEEN 1 AND 5 ORDER BY dayOfWeek ASC, timeSlot ASC")
    fun getAllTimetableSlotsFlow(): Flow<List<TimetableSlotEntity>>

    @Query("SELECT * FROM timetable_slots WHERE classId IS NULL AND dayOfWeek BETWEEN 1 AND 5 ORDER BY dayOfWeek ASC, timeSlot ASC")
    fun getTeacherTimetableSlotsFlow(): Flow<List<TimetableSlotEntity>>

    @Query("SELECT * FROM timetable_slots WHERE classId IS NULL AND dayOfWeek BETWEEN 1 AND 5 ORDER BY dayOfWeek ASC, timeSlot ASC")
    suspend fun getTeacherTimetableSlots(): List<TimetableSlotEntity>

    @Query("SELECT * FROM timetable_slots WHERE classId = :classId AND dayOfWeek BETWEEN 1 AND 5 ORDER BY dayOfWeek ASC, timeSlot ASC")
    fun getTimetableSlotsByClassFlow(classId: Int): Flow<List<TimetableSlotEntity>>

    @Query("SELECT * FROM timetable_slots WHERE classId = :classId AND dayOfWeek BETWEEN 1 AND 5 ORDER BY dayOfWeek ASC, timeSlot ASC")
    suspend fun getTimetableSlotsByClass(classId: Int): List<TimetableSlotEntity>

    @Query("SELECT * FROM timetable_slots WHERE dayOfWeek BETWEEN 1 AND 5 ORDER BY dayOfWeek ASC, timeSlot ASC")
    suspend fun getAllTimetableSlots(): List<TimetableSlotEntity>

    @Query("UPDATE timetable_slots SET timeSlot = :newTimeSlot WHERE TRIM(timeSlot) = TRIM(:oldTimeSlot)")
    suspend fun updateTimeSlotGlobally(oldTimeSlot: String, newTimeSlot: String)

    @Query("DELETE FROM timetable_slots WHERE TRIM(timeSlot) = TRIM(:targetTimeSlot)")
    suspend fun deleteTimeSlotGlobally(targetTimeSlot: String)

    @Query("UPDATE journal_entries SET timeSlot = :newTimeSlot WHERE TRIM(timeSlot) = TRIM(:oldTimeSlot)")
    suspend fun updateJournalTimeSlotGlobally(oldTimeSlot: String, newTimeSlot: String)

    @Query("UPDATE attendances SET timeSlot = :newTimeSlot WHERE TRIM(timeSlot) = TRIM(:oldTimeSlot)")
    suspend fun updateAttendanceTimeSlotGlobally(oldTimeSlot: String, newTimeSlot: String)

    @Transaction
    suspend fun updateTimeSlotEverywhere(oldTimeSlot: String, newTimeSlot: String) {
        updateTimeSlotGlobally(oldTimeSlot, newTimeSlot)
        updateJournalTimeSlotGlobally(oldTimeSlot, newTimeSlot)
        updateAttendanceTimeSlotGlobally(oldTimeSlot, newTimeSlot)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableSlot(slot: TimetableSlotEntity): Long

    @Query("DELETE FROM timetable_slots WHERE id = :id")
    suspend fun deleteTimetableSlot(id: Int)

    @Query("DELETE FROM timetable_slots WHERE dayOfWeek NOT BETWEEN 1 AND 5")
    suspend fun deleteWeekendTimetableSlots()

    // --- Reminders (Rappels) ---
    @Query("SELECT * FROM reminders ORDER BY date ASC, time ASC")
    fun getAllRemindersFlow(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders")
    suspend fun getAllRemindersList(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: Int)

    @Query("DELETE FROM reminders")
    suspend fun clearAllReminders()

    // --- Notes (Prise de notes) ---
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotesFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesList(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Int)

    @Query("DELETE FROM notes")
    suspend fun clearAllNotes()

    // --- Documents ---
    @Query("SELECT * FROM document_categories ORDER BY name ASC")
    fun getAllCategoriesFlow(): Flow<List<DocumentCategoryEntity>>

    @Query("SELECT * FROM document_categories")
    suspend fun getAllCategoriesList(): List<DocumentCategoryEntity>

    @Query("SELECT * FROM document_categories WHERE parentCategoryId = :parentId")
    suspend fun getSubCategoriesList(parentId: Int): List<DocumentCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: DocumentCategoryEntity): Long

    @Delete
    suspend fun deleteCategory(category: DocumentCategoryEntity)

    @Query("DELETE FROM document_categories")
    suspend fun clearAllCategories()

    @Query("SELECT * FROM documents WHERE categoryId = :categoryId ORDER BY createdAt DESC")
    fun getDocumentsByCategoryFlow(categoryId: Int): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents")
    fun getAllDocumentsFlow(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents")
    suspend fun getAllDocumentsList(): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE categoryId = :categoryId")
    suspend fun getDocumentsByCategoryList(categoryId: Int): List<DocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("DELETE FROM documents")
    suspend fun clearAllDocuments()

    // --- Attendances (Carnet d'absences) ---
    @Query("SELECT * FROM attendances ORDER BY date DESC, recordedAt DESC")
    fun getAllAttendancesFlow(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendances WHERE classId = :classId ORDER BY date DESC, recordedAt DESC")
    fun getAttendancesByClassFlow(classId: Int): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendances WHERE classId = :classId AND date = :date ORDER BY recordedAt DESC")
    fun getAttendancesByClassAndDateFlow(classId: Int, date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendances WHERE studentId = :studentId ORDER BY date DESC")
    fun getAttendancesByStudentFlow(studentId: Int): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendances WHERE classId = :classId AND date = :date AND timeSlot = :timeSlot")
    suspend fun getAttendancesForSlot(classId: Int, date: String, timeSlot: String): List<AttendanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendances(attendances: List<AttendanceEntity>)

    @Update
    suspend fun updateAttendance(attendance: AttendanceEntity)

    @Delete
    suspend fun deleteAttendance(attendance: AttendanceEntity)

    @Query("DELETE FROM attendances WHERE classId = :classId AND date = :date AND timeSlot = :timeSlot")
    suspend fun deleteAttendancesForSlot(classId: Int, date: String, timeSlot: String)

    @Transaction
    suspend fun replaceAttendancesForSlot(classId: Int, date: String, timeSlot: String, attendances: List<AttendanceEntity>) {
        deleteAttendancesForSlot(classId, date, timeSlot)
        insertAttendances(attendances)
    }

    @Query("DELETE FROM attendances")
    suspend fun clearAllAttendances()
}
