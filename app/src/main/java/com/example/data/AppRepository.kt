package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class AppRepository(private val appDao: AppDao) {

    // --- Settings ---
    val settingsFlow: Flow<SettingsEntity?> = appDao.getSettingsFlow()

    suspend fun getSettings(): SettingsEntity {
        return appDao.getSettings() ?: SettingsEntity().also {
            appDao.insertOrUpdateSettings(it)
        }
    }

    suspend fun saveSettings(settings: SettingsEntity) {
        appDao.insertOrUpdateSettings(settings)
    }

    // --- Classes ---
    val allClasses: Flow<List<ClassEntity>> = appDao.getAllClassesFlow()

    suspend fun getClassById(id: Int): ClassEntity? = appDao.getClassById(id)

    suspend fun insertClass(classEntity: ClassEntity): Long = appDao.insertClass(classEntity)

    suspend fun updateClass(classEntity: ClassEntity) = appDao.updateClass(classEntity)

    suspend fun deleteClass(classEntity: ClassEntity) = appDao.deleteClass(classEntity)

    // --- Students ---
    val allStudents: Flow<List<StudentEntity>> = appDao.getAllStudentsFlow()

    fun getStudentsByClass(classId: Int): Flow<List<StudentEntity>> = appDao.getStudentsByClassFlow(classId)

    suspend fun getStudentsByClassList(classId: Int): List<StudentEntity> = appDao.getStudentsByClass(classId)

    suspend fun getStudentById(id: Int): StudentEntity? = appDao.getStudentById(id)

    suspend fun insertStudent(student: StudentEntity): Long = appDao.insertStudent(student)

    suspend fun updateStudent(student: StudentEntity) = appDao.updateStudent(student)

    suspend fun updateStudents(students: List<StudentEntity>) = appDao.updateStudents(students)

    suspend fun deleteStudent(student: StudentEntity) = appDao.deleteStudent(student)

    suspend fun clearSeatForClass(classId: Int, seatIndex: Int) = appDao.clearSeatForClass(classId, seatIndex)

    suspend fun clearAllSeatsForClass(classId: Int) = appDao.clearAllSeatsForClass(classId)

    // --- Grades ---
    fun getStudentsWithGradesForTerm(classId: Int, term: Int): Flow<List<StudentWithGrade>> {
        return appDao.getStudentsWithGradesForTermFlow(classId, term)
    }

    suspend fun getStudentsWithGradesForTermList(classId: Int, term: Int): List<StudentWithGrade> {
        return appDao.getStudentsWithGradesForTerm(classId, term)
    }

    suspend fun saveGrade(grade: GradeEntity): Long = appDao.insertGrade(grade)

    suspend fun deleteGradesByStudent(studentId: Int) = appDao.deleteGradesByStudent(studentId)

    suspend fun getGradeForStudentAndTerm(studentId: Int, term: Int): GradeEntity? = appDao.getGradeForStudentAndTerm(studentId, term)

    // --- Journal (Cahier Journal) ---
    val allJournalEntries: Flow<List<JournalEntryEntity>> = appDao.getAllJournalEntriesFlow()

    fun getJournalEntriesByClass(classId: Int): Flow<List<JournalEntryEntity>> = appDao.getJournalEntriesByClassFlow(classId)

    fun getJournalEntriesBetweenDates(startDate: String, endDate: String): Flow<List<JournalEntryEntity>> {
        return appDao.getJournalEntriesBetweenDatesFlow(startDate, endDate)
    }

    suspend fun getJournalEntriesBetweenDatesList(startDate: String, endDate: String): List<JournalEntryEntity> {
        return appDao.getJournalEntriesBetweenDates(startDate, endDate)
    }

    suspend fun insertJournalEntry(entry: JournalEntryEntity): Long = appDao.insertJournalEntry(entry)

    suspend fun updateJournalEntry(entry: JournalEntryEntity) = appDao.updateJournalEntry(entry)

    suspend fun deleteJournalEntry(id: Int) = appDao.deleteJournalEntry(id)

    // --- Timetables ---
    val allTimetableSlots: Flow<List<TimetableSlotEntity>> = appDao.getAllTimetableSlotsFlow()

    val teacherTimetableSlots: Flow<List<TimetableSlotEntity>> = appDao.getTeacherTimetableSlotsFlow()

    suspend fun getTeacherTimetableSlots(): List<TimetableSlotEntity> = appDao.getTeacherTimetableSlots()

    fun getTimetableSlotsByClass(classId: Int): Flow<List<TimetableSlotEntity>> = appDao.getTimetableSlotsByClassFlow(classId)

    suspend fun getTimetableSlotsByClassList(classId: Int): List<TimetableSlotEntity> = appDao.getTimetableSlotsByClass(classId)

    suspend fun getAllTimetableSlots(): List<TimetableSlotEntity> = appDao.getAllTimetableSlots()

    suspend fun updateTimeSlotGlobally(oldTimeSlot: String, newTimeSlot: String) {
        appDao.updateTimeSlotEverywhere(oldTimeSlot, newTimeSlot)
    }

    suspend fun deleteTimeSlotGlobally(targetTimeSlot: String) = appDao.deleteTimeSlotGlobally(targetTimeSlot)

    suspend fun insertTimetableSlot(slot: TimetableSlotEntity): Long = appDao.insertTimetableSlot(slot)

    suspend fun deleteTimetableSlot(id: Int) = appDao.deleteTimetableSlot(id)

    suspend fun deleteWeekendTimetableSlots() = appDao.deleteWeekendTimetableSlots()

    // --- Reminders (Rappels) ---
    val allReminders: Flow<List<ReminderEntity>> = appDao.getAllRemindersFlow()

    suspend fun getAllRemindersList(): List<ReminderEntity> = appDao.getAllRemindersList()

    suspend fun insertReminder(reminder: ReminderEntity): Long = appDao.insertReminder(reminder)

    suspend fun deleteReminder(id: Int) = appDao.deleteReminder(id)

    suspend fun clearAllReminders() = appDao.clearAllReminders()

    // --- Notes (Prise de notes) ---
    val allNotes: Flow<List<NoteEntity>> = appDao.getAllNotesFlow()

    suspend fun getAllNotesList(): List<NoteEntity> = appDao.getAllNotesList()

    suspend fun insertNote(note: NoteEntity): Long = appDao.insertNote(note)

    suspend fun deleteNote(id: Int) = appDao.deleteNote(id)

    suspend fun clearAllNotes() = appDao.clearAllNotes()

    // --- Documents ---
    val allCategories: Flow<List<DocumentCategoryEntity>> = appDao.getAllCategoriesFlow()

    suspend fun getAllCategoriesList(): List<DocumentCategoryEntity> = appDao.getAllCategoriesList()
    suspend fun getSubCategoriesList(parentId: Int): List<DocumentCategoryEntity> = appDao.getSubCategoriesList(parentId)

    suspend fun insertCategory(category: DocumentCategoryEntity): Long = appDao.insertCategory(category)

    suspend fun deleteCategory(category: DocumentCategoryEntity) = appDao.deleteCategory(category)

    suspend fun clearAllCategories() = appDao.clearAllCategories()

    fun getDocumentsByCategory(categoryId: Int): Flow<List<DocumentEntity>> = appDao.getDocumentsByCategoryFlow(categoryId)

    val allDocuments: Flow<List<DocumentEntity>> = appDao.getAllDocumentsFlow()

    suspend fun getAllDocumentsList(): List<DocumentEntity> = appDao.getAllDocumentsList()
    suspend fun getDocumentsByCategoryList(categoryId: Int): List<DocumentEntity> = appDao.getDocumentsByCategoryList(categoryId)

    suspend fun insertDocument(document: DocumentEntity): Long = appDao.insertDocument(document)

    suspend fun deleteDocument(document: DocumentEntity) = appDao.deleteDocument(document)
    suspend fun clearAllDocuments() = appDao.clearAllDocuments()

    // --- Attendances (Carnet d'absences) ---
    val allAttendances: Flow<List<AttendanceEntity>> = appDao.getAllAttendancesFlow()

    fun getAttendancesByClass(classId: Int): Flow<List<AttendanceEntity>> = appDao.getAttendancesByClassFlow(classId)

    fun getAttendancesByClassAndDate(classId: Int, date: String): Flow<List<AttendanceEntity>> = appDao.getAttendancesByClassAndDateFlow(classId, date)

    fun getAttendancesByStudent(studentId: Int): Flow<List<AttendanceEntity>> = appDao.getAttendancesByStudentFlow(studentId)

    suspend fun getAttendancesForSlot(classId: Int, date: String, timeSlot: String): List<AttendanceEntity> = appDao.getAttendancesForSlot(classId, date, timeSlot)

    suspend fun insertAttendance(attendance: AttendanceEntity): Long = appDao.insertAttendance(attendance)

    suspend fun insertAttendances(attendances: List<AttendanceEntity>) = appDao.insertAttendances(attendances)

    suspend fun updateAttendance(attendance: AttendanceEntity) = appDao.updateAttendance(attendance)

    suspend fun deleteAttendance(attendance: AttendanceEntity) = appDao.deleteAttendance(attendance)

    suspend fun deleteAttendancesForSlot(classId: Int, date: String, timeSlot: String) = appDao.deleteAttendancesForSlot(classId, date, timeSlot)
    suspend fun replaceAttendancesForSlot(classId: Int, date: String, timeSlot: String, attendances: List<AttendanceEntity>) = appDao.replaceAttendancesForSlot(classId, date, timeSlot, attendances)
    suspend fun clearAllAttendances() = appDao.clearAllAttendances()
}
