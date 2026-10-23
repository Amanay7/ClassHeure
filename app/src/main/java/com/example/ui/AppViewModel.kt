package com.example.ui

import android.app.AlarmManager

import android.app.PendingIntent

import android.content.ContentValues

import android.content.Context

import android.content.Intent

import android.net.Uri

import androidx.core.content.FileProvider

import android.os.Build

import android.os.Environment

import android.provider.MediaStore

import android.util.Log

import androidx.lifecycle.ViewModel

import androidx.lifecycle.ViewModelProvider

import androidx.lifecycle.viewModelScope

import com.example.ReminderNotificationReceiver

import com.example.data.*

import com.example.util.Tr

import com.example.util.formatTeacherName

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.flow.*

import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext

import java.io.BufferedReader

import java.io.File

import java.io.ByteArrayOutputStream
import java.io.FileOutputStream

import java.io.InputStreamReader

import java.io.OutputStream

import java.util.Calendar

class AppViewModel(private val repository: AppRepository) : ViewModel() {

    private val TAG = "AppViewModel"

    init {

        viewModelScope.launch(Dispatchers.IO) {

            try {

                repository.getSettings()

                repository.deleteWeekendTimetableSlots()

                val currentCats = repository.getAllCategoriesList()

                if (currentCats.isEmpty()) {

                    repository.insertCategory(DocumentCategoryEntity(name = "Progressions annuelles", parentCategoryId = null))

                    repository.insertCategory(DocumentCategoryEntity(name = "Fiches de cours", parentCategoryId = null))

                }

                val curSettings = repository.getSettings()

                if (curSettings != null) {

                    if (curSettings.activeLanguage == "fr" && curSettings.fontSizeScale == "medium") {

                        repository.saveSettings(curSettings.copy(fontSizeScale = "small"))

                    }

                    if (curSettings.activeLanguage == "ar" && (curSettings.activeFont in listOf("Cairo", "Amiri", "Tajawal") || curSettings.activeFont.isBlank())) {

                        repository.saveSettings(curSettings.copy(activeFont = "Almarai"))

                    }

                }

            } catch (e: Exception) {

                Log.e(TAG, "Failed to initialize AppViewModel defaults", e)

            }

        }

    }

    // --- Core States ---

    val settings = repository.settingsFlow.stateIn(

        scope = viewModelScope,

        started = SharingStarted.WhileSubscribed(5000),

        initialValue = null

    )

    private val _isUnlocked = MutableStateFlow(false)

    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    fun setUnlocked(unlocked: Boolean) {

        _isUnlocked.value = unlocked

    }

    val allClasses = repository.allClasses.stateIn(

        scope = viewModelScope,

        started = SharingStarted.WhileSubscribed(5000),

        initialValue = emptyList()

    )

    private val _selectedClassId = MutableStateFlow<Int?>(null)

    val selectedClassId: StateFlow<Int?> = _selectedClassId.asStateFlow()

    private val _selectedTerm = MutableStateFlow(1) // 1, 2, or 3

    val selectedTerm: StateFlow<Int> = _selectedTerm.asStateFlow()

    // Students of selected class

    val currentStudents = _selectedClassId.flatMapLatest { classId ->

        if (classId != null) {

            repository.getStudentsByClass(classId)

        } else {

            flowOf(emptyList())

        }

    }.stateIn(

        scope = viewModelScope,

        started = SharingStarted.WhileSubscribed(5000),

        initialValue = emptyList()

    )

    // Grades of selected class and term

    val currentGrades = combine(_selectedClassId, _selectedTerm) { classId, term ->

        Pair(classId, term)

    }.flatMapLatest { (classId, term) ->

        if (classId != null) {

            repository.getStudentsWithGradesForTerm(classId, term)

        } else {

            flowOf(emptyList())

        }

    }.stateIn(

        scope = viewModelScope,

        started = SharingStarted.WhileSubscribed(5000),

        initialValue = emptyList()

    )

    // Journal entries of selected class

    val currentJournalEntries = _selectedClassId.flatMapLatest { classId ->

        if (classId != null) {

            repository.getJournalEntriesByClass(classId)

        } else {

            repository.allJournalEntries

        }

    }.stateIn(

        scope = viewModelScope,

        started = SharingStarted.WhileSubscribed(5000),

        initialValue = emptyList()

    )

    // All timetable slots (consolidated across all classes for teacher and overall schedule)

    val allTimetableSlots = repository.allTimetableSlots.stateIn(

        scope = viewModelScope,

        started = SharingStarted.WhileSubscribed(5000),

        initialValue = emptyList()

    )

    // Teacher-only timetable slots (classId == null)

    val teacherTimetableSlots = repository.teacherTimetableSlots.stateIn(

        scope = viewModelScope,

        started = SharingStarted.WhileSubscribed(5000),

        initialValue = emptyList()

    )

    // Timetable slots of selected class (or teacher slots if classId is null)

    val currentTimetableSlots = _selectedClassId.flatMapLatest { classId ->

        if (classId != null) {

            repository.getTimetableSlotsByClass(classId)

        } else {

            repository.teacherTimetableSlots

        }

    }.stateIn(

        scope = viewModelScope,

        started = SharingStarted.WhileSubscribed(5000),

        initialValue = emptyList()

    )

    // Target timetable day navigation from Home screen ("À VENIR" card click)

    private val _targetTimetableDay = MutableStateFlow<Int?>(null)

    val targetTimetableDay: StateFlow<Int?> = _targetTimetableDay.asStateFlow()

    fun selectTimetableDay(day: Int?) {

        _targetTimetableDay.value = day

    }

    // Reminders state flow

    val allReminders = repository.allReminders.stateIn(

        scope = viewModelScope,

        started = SharingStarted.WhileSubscribed(5000),

        initialValue = emptyList()

    )

    // Notes state flow

    val allNotes = repository.allNotes.stateIn(

        scope = viewModelScope,

        started = SharingStarted.WhileSubscribed(5000),

        initialValue = emptyList()

    )

    // Documents State flows

    val allCategories = repository.allCategories.stateIn(

        scope = viewModelScope,

        started = SharingStarted.WhileSubscribed(5000),

        initialValue = emptyList()

    )

    val selectedCategoryId = MutableStateFlow<Int?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

    val currentDocuments = selectedCategoryId.flatMapLatest { catId ->

        if (catId != null) {

            repository.getDocumentsByCategory(catId)

        } else {

            flowOf(emptyList())

        }

    }.stateIn(

        scope = viewModelScope,

        started = SharingStarted.WhileSubscribed(5000),

        initialValue = emptyList()

    )

    val allDocuments = repository.allDocuments.stateIn(

        scope = viewModelScope,

        started = SharingStarted.WhileSubscribed(5000),

        initialValue = emptyList()

    )

    // Attendances (Carnet d'absences)

    val allAttendances = repository.allAttendances.stateIn(

        scope = viewModelScope,

        started = SharingStarted.WhileSubscribed(5000),

        initialValue = emptyList()

    )

    // Timetable time slot replacements & deletions & custom slots (persisted across sessions)

    private val _timeSlotReplacements = MutableStateFlow<Map<String, String>>(emptyMap())

    val timeSlotReplacements: StateFlow<Map<String, String>> = _timeSlotReplacements.asStateFlow()

    private val _deletedTimeSlots = MutableStateFlow<Set<String>>(emptySet())

    val deletedTimeSlots: StateFlow<Set<String>> = _deletedTimeSlots.asStateFlow()

    private val _customTimeSlots = MutableStateFlow<Set<String>>(emptySet())

    val customTimeSlots: StateFlow<Set<String>> = _customTimeSlots.asStateFlow()

    fun initTimetablePrefs(context: Context) {

        try {

            val prefs = context.getSharedPreferences("timetable_config", Context.MODE_PRIVATE)

            val replacementsJson = prefs.getString("time_slot_replacements", "{}") ?: "{}"

            val deletedJson = prefs.getString("deleted_time_slots", "[]") ?: "[]"

            val customJson = prefs.getString("custom_time_slots", "[]") ?: "[]"

            val map = mutableMapOf<String, String>()

            val jsonObj = org.json.JSONObject(replacementsJson)

            val keys = jsonObj.keys()

            while (keys.hasNext()) {

                val k = keys.next()

                map[k] = jsonObj.getString(k)

            }

            _timeSlotReplacements.value = map

            val set = mutableSetOf<String>()

            val jsonArr = org.json.JSONArray(deletedJson)

            for (i in 0 until jsonArr.length()) {

                set.add(jsonArr.getString(i))

            }

            _deletedTimeSlots.value = set

            val customSet = mutableSetOf<String>()

            val customArr = org.json.JSONArray(customJson)

            for (i in 0 until customArr.length()) {

                customSet.add(customArr.getString(i))

            }

            _customTimeSlots.value = customSet

        } catch (e: Exception) {

            Log.e(TAG, "Error loading timetable preferences", e)

        }

    }

    fun selectDocumentCategory(categoryId: Int?) {

        selectedCategoryId.value = categoryId

    }

    // Status message for notification toasts

    private val _statusMessage = MutableStateFlow<String?>(null)

    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Gemini states

    private val _geminiSummary = MutableStateFlow<String?>(null)

    val geminiSummary: StateFlow<String?> = _geminiSummary.asStateFlow()

    private val _isGeneratingSummary = MutableStateFlow(false)

    val isGeneratingSummary: StateFlow<Boolean> = _isGeneratingSummary.asStateFlow()

    fun selectClass(classId: Int?) {

        _selectedClassId.value = classId

    }

    fun selectTerm(term: Int) {

        if (term in 1..3) {

            _selectedTerm.value = term

        }

    }

    fun clearStatusMessage() {

        _statusMessage.value = null

    }

    fun clearGeminiSummary() {

        _geminiSummary.value = null

    }

    // --- Settings CRUD ---

    fun updateProfile(teacherName: String, schoolName: String, taughtSubject: String) {
        viewModelScope.launch {
            val current = settings.value ?: SettingsEntity()
            repository.saveSettings(current.copy(
                teacherName = formatTeacherName(teacherName.trim()),
                schoolName = schoolName,
                taughtSubject = taughtSubject
            ))
        }
    }

    fun updateTeacherName(name: String) {

        viewModelScope.launch {

            val current = settings.value ?: SettingsEntity()

            repository.saveSettings(current.copy(teacherName = formatTeacherName(name.trim())))

        }

    }

    fun updateSchoolName(name: String) {

        viewModelScope.launch {

            val current = settings.value ?: SettingsEntity()

            repository.saveSettings(current.copy(schoolName = name))

        }

    }

    fun updateLanguage(lang: String) {

        viewModelScope.launch {

            val current = settings.value ?: SettingsEntity()

            val newFont = if (lang == "ar") {

                if (current.activeFont !in listOf("Almarai", "Tajawal")) "Almarai" else current.activeFont

            } else {

                if (current.activeFont in listOf("Tajawal", "Almarai", "Cairo", "Amiri")) "Times New Roman" else current.activeFont

            }

            val newScale = if (lang == "fr" && current.fontSizeScale == "medium") "small" else current.fontSizeScale

            repository.saveSettings(current.copy(activeLanguage = lang, activeFont = newFont, fontSizeScale = newScale))

        }

    }

    fun updateTheme(themeStr: String) {

        viewModelScope.launch {

            val current = settings.value ?: SettingsEntity()

            repository.saveSettings(current.copy(activeTheme = themeStr))

        }

    }

    fun updateFontFamily(font: String) {

        viewModelScope.launch {

            val current = settings.value ?: SettingsEntity()

            repository.saveSettings(current.copy(activeFont = font))

        }

    }

    fun updateFontSizeScale(scale: String) {

        viewModelScope.launch {

            val current = settings.value ?: SettingsEntity()

            repository.saveSettings(current.copy(fontSizeScale = scale))

        }

    }

    fun updateFontBold(isBold: Boolean) {
        viewModelScope.launch {
            val current = settings.value ?: SettingsEntity()
            repository.saveSettings(current.copy(isFontBold = isBold))
        }
    }

    fun updateProfileAnimationEffect(effect: String) {

        viewModelScope.launch {

            val current = settings.value ?: SettingsEntity()

            repository.saveSettings(current.copy(profileAnimationEffect = effect))

        }

    }

    fun updateSchoolYear(newYear: String) {

        viewModelScope.launch {

            val current = settings.value ?: SettingsEntity()

            repository.saveSettings(current.copy(currentSchoolYear = newYear))

        }

    }

    fun addSchoolYear(year: String) {

        viewModelScope.launch {

            val current = settings.value ?: SettingsEntity()

            val list = try {

                val array = org.json.JSONArray(current.schoolYearsJson)

                val items = mutableListOf<String>()

                for (i in 0 until array.length()) {

                    items.add(array.getString(i))

                }

                items

            } catch (e: Exception) {

                mutableListOf("2023 / 2024")

            }

            if (!list.contains(year)) {

                list.add(year)

                val newJson = org.json.JSONArray(list).toString()

                repository.saveSettings(current.copy(

                    schoolYearsJson = newJson,

                    currentSchoolYear = year

                ))

            }

        }

    }

    fun archiveCurrentSchoolYear() {

        viewModelScope.launch {

            val current = settings.value ?: SettingsEntity()

            val activeYear = current.currentSchoolYear

            val archivedList = try {

                val array = org.json.JSONArray(current.archivedSchoolYearsJson)

                val items = mutableListOf<String>()

                for (i in 0 until array.length()) {

                    items.add(array.getString(i))

                }

                items

            } catch (e: Exception) {

                mutableListOf<String>()

            }

            if (!archivedList.contains(activeYear)) {

                archivedList.add(activeYear)

                val newArchivedJson = org.json.JSONArray(archivedList).toString()

                repository.saveSettings(current.copy(

                    archivedSchoolYearsJson = newArchivedJson

                ))

            }

        }

    }

    fun deleteSchoolYear(year: String) {

        viewModelScope.launch {

            val current = settings.value ?: SettingsEntity()

            val activeList = try {

                val array = org.json.JSONArray(current.schoolYearsJson)

                val items = mutableListOf<String>()

                for (i in 0 until array.length()) {

                    items.add(array.getString(i))

                }

                items

            } catch (e: Exception) {

                mutableListOf<String>()

            }

            activeList.remove(year)

            val newActiveJson = org.json.JSONArray(activeList).toString()

            val archivedList = try {

                val array = org.json.JSONArray(current.archivedSchoolYearsJson)

                val items = mutableListOf<String>()

                for (i in 0 until array.length()) {

                    items.add(array.getString(i))

                }

                items

            } catch (e: Exception) {

                mutableListOf<String>()

            }

            archivedList.remove(year)

            val newArchivedJson = org.json.JSONArray(archivedList).toString()

            val newCurrentYear = if (current.currentSchoolYear == year) {

                activeList.firstOrNull() ?: ""

            } else current.currentSchoolYear

            repository.saveSettings(current.copy(

                schoolYearsJson = newActiveJson,

                archivedSchoolYearsJson = newArchivedJson,

                currentSchoolYear = newCurrentYear

            ))

            _statusMessage.value = "Année scolaire '$year' supprimée"

        }

    }

    // --- Carnet d'absences (Attendance) CRUD ---

    fun saveAttendancesForSession(attendancesList: List<AttendanceEntity>) {

        viewModelScope.launch {

            if (attendancesList.isNotEmpty()) {

                val first = attendancesList.first()

                repository.replaceAttendancesForSlot(first.classId, first.date, first.timeSlot, attendancesList)

                _statusMessage.value = "Absences de la séance enregistrées !"

            }

        }

    }

    fun deleteAttendance(attendance: AttendanceEntity) {

        viewModelScope.launch {

            repository.deleteAttendance(attendance)

            _statusMessage.value = "Saisie d'absence supprimée"

        }

    }

    fun updateAttendance(attendance: AttendanceEntity) {

        viewModelScope.launch {

            repository.updateAttendance(attendance)

            _statusMessage.value = "Saisie d'absence mise à jour !"

        }

    }

    fun updateTaughtSubject(subject: String) {

        viewModelScope.launch {

            val current = settings.value ?: SettingsEntity()

            repository.saveSettings(current.copy(taughtSubject = subject))

        }

    }

    fun updateSubjectCoefficient(coeff: Int) {
        viewModelScope.launch {
            val current = settings.value ?: SettingsEntity()
            repository.saveSettings(current.copy(subjectCoefficient = coeff))
        }
    }

    fun updatePinCode(pin: String?) {

        viewModelScope.launch {

            val current = settings.value ?: SettingsEntity()

            repository.saveSettings(current.copy(pinCode = pin))

        }

    }

    fun updateBiometricEnabled(enabled: Boolean) {

        viewModelScope.launch {

            val current = settings.value ?: SettingsEntity()

            repository.saveSettings(current.copy(isBiometricEnabled = enabled))

        }

    }

    fun updateTeacherFreeDay(day: Int?) {

        viewModelScope.launch {

            val current = settings.value ?: SettingsEntity()

            repository.saveSettings(current.copy(teacherFreeDay = day))

            _statusMessage.value = if (day != null) "Journée libre configurée" else "Journée libre réinitialisée"

        }

    }

    fun updateEducationLevel(level: String) {

        viewModelScope.launch {

            val current = settings.value ?: SettingsEntity()

            repository.saveSettings(current.copy(educationLevel = level, hasCompletedOnboarding = true))

        }

    }

    fun completeOnboarding(level: String, language: String = "fr", teacherName: String = "", schoolName: String = "") {

        viewModelScope.launch {

            val current = settings.value ?: SettingsEntity()

            val defaultFont = if (language == "ar") "Almarai" else "Times New Roman"

            val updated = current.copy(

                educationLevel = level,

                activeLanguage = language,

                activeFont = defaultFont,

                hasCompletedOnboarding = true,

                teacherName = if (teacherName.isNotBlank()) formatTeacherName(teacherName.trim()) else current.teacherName,

                schoolName = if (schoolName.isNotBlank()) schoolName.trim() else current.schoolName

            )

            repository.saveSettings(updated)

        }

    }

    suspend fun getStudentsByClassList(classId: Int): List<StudentEntity> {

        return repository.getStudentsByClassList(classId)

    }

    // --- Classes CRUD ---

    fun addClass(name: String, coefficient: Int = 3) {

        viewModelScope.launch {

            if (name.isNotBlank()) {

                val currentYear = settings.value?.currentSchoolYear ?: ""

                repository.insertClass(ClassEntity(name = name, schoolYear = currentYear, subjectCoefficient = coefficient))

                _statusMessage.value = "Classe '$name' ajoutée avec succès !"

            }

        }

    }

    fun updateClass(classEntity: ClassEntity) {

        viewModelScope.launch {

            repository.updateClass(classEntity)

            _statusMessage.value = "Classe modifiée."

        }

    }

    fun deleteClass(classEntity: ClassEntity) {

        viewModelScope.launch {

            repository.deleteClass(classEntity)

            if (_selectedClassId.value == classEntity.id) {

                _selectedClassId.value = null

            }

            _statusMessage.value = "Classe supprimée."

        }

    }

    // --- Students CRUD ---

    fun addStudent(

        lastName: String,

        firstName: String,

        birthDate: String,

        gender: String,

        birthPlace: String = "",

        goodToKnow: String = "",

        repeatedYears: String = "",

        groupName: String = ""

    ) {

        val classId = _selectedClassId.value ?: return

        viewModelScope.launch {

            if (lastName.isNotBlank() && firstName.isNotBlank()) {

                val existing = repository.getStudentsByClassList(classId)

                val nextOrder = (existing.maxOfOrNull { it.orderIndex } ?: 0) + 1

                repository.insertStudent(

                    StudentEntity(

                        classId = classId,

                        lastName = lastName,

                        firstName = firstName,

                        birthDate = birthDate,

                        gender = gender,

                        birthPlace = birthPlace,

                        goodToKnow = goodToKnow,

                        repeatedYears = repeatedYears,

                        groupName = groupName,

                        orderIndex = nextOrder

                    )

                )

                _statusMessage.value = "Élève $lastName $firstName ajouté."

            }

        }

    }

    fun divideClassIntoGroups(classId: Int, groupCount: Int = 2, method: String = "ALPHABETICAL") {

        viewModelScope.launch {

            val students = repository.getStudentsByClassList(classId)

            if (students.isEmpty()) return@launch

            val sorted = when (method) {

                "GENDER" -> {

                    val boys = students.filter { it.gender.equals("M", ignoreCase = true) }.sortedWith(compareBy({ it.lastName.lowercase() }, { it.firstName.lowercase() }))

                    val girls = students.filter { !it.gender.equals("M", ignoreCase = true) }.sortedWith(compareBy({ it.lastName.lowercase() }, { it.firstName.lowercase() }))

                    val list = mutableListOf<StudentEntity>()

                    var bIdx = 0

                    var gIdx = 0

                    while (bIdx < boys.size || gIdx < girls.size) {

                        if (bIdx < boys.size) list.add(boys[bIdx++])

                        if (gIdx < girls.size) list.add(girls[gIdx++])

                    }

                    list

                }

                else -> students.sortedWith(compareBy({ it.lastName.lowercase() }, { it.firstName.lowercase() }))

            }

            val updated = when (method) {

                "ALPHABETICAL" -> {

                    val chunkSize = (sorted.size + groupCount - 1) / groupCount

                    sorted.mapIndexed { index, student ->

                        val groupIndex = (index / chunkSize).coerceAtMost(groupCount - 1) + 1

                        student.copy(groupName = "G$groupIndex")

                    }

                }

                else -> {

                    sorted.mapIndexed { index, student ->

                        val groupIndex = (index % groupCount) + 1

                        student.copy(groupName = "G$groupIndex")

                    }

                }

            }

            repository.updateStudents(updated)

            _statusMessage.value = if (Tr.isArabicLang()) "تم تقسيم القسم إلى $groupCount أفواج بنجاح" else "Classe divisée en $groupCount groupes avec succès"

        }

    }

    fun resetClassGroups(classId: Int) {

        viewModelScope.launch {

            val students = repository.getStudentsByClassList(classId)

            val updated = students.map { it.copy(groupName = "") }

            repository.updateStudents(updated)

            _statusMessage.value = if (Tr.isArabicLang()) "تم إلغاء تقسيم الأفواج" else "Groupes réinitialisés"

        }

    }

    fun saveCustomClassGroups(classId: Int, studentGroupAssignments: Map<Int, String>) {

        viewModelScope.launch {

            val currentStudents = repository.getStudentsByClassList(classId)

            val updated = currentStudents.map { student ->

                val assignedGroup = studentGroupAssignments[student.id] ?: ""

                student.copy(groupName = assignedGroup.trim())

            }

            repository.updateStudents(updated)

            _statusMessage.value = if (Tr.isArabicLang()) "تم حفظ توزيع الأفواج بنجاح" else "Répartition des groupes enregistrée avec succès"

        }

    }

    fun setStudentGroup(studentId: Int, groupName: String) {

        viewModelScope.launch {

            val student = repository.getStudentById(studentId)

            if (student != null) {

                repository.updateStudent(student.copy(groupName = groupName.trim()))

            }

        }

    }

    fun updateStudent(student: StudentEntity) {

        viewModelScope.launch {

            repository.updateStudent(student)

            _statusMessage.value = "Élève modifié."

        }

    }

    fun assignStudentSeat(classId: Int, studentId: Int, seatIndex: Int) {

        viewModelScope.launch {

            repository.clearSeatForClass(classId, seatIndex)

            val student = repository.getStudentById(studentId)

            if (student != null) {

                repository.updateStudent(student.copy(seatIndex = seatIndex))

                _statusMessage.value = "${student.lastName.uppercase()} assigné(e) à la place ${seatIndex + 1}."

            }

        }

    }

    fun swapStudentSeats(classId: Int, studentId1: Int, seatIndex1: Int, studentId2: Int, seatIndex2: Int) {

        viewModelScope.launch {

            val student1 = repository.getStudentById(studentId1)

            val student2 = repository.getStudentById(studentId2)

            if (student1 != null && student2 != null) {

                repository.updateStudent(student1.copy(seatIndex = seatIndex2))

                repository.updateStudent(student2.copy(seatIndex = seatIndex1))

                _statusMessage.value = "Places échangées."

            }

        }

    }

    fun freeSeat(classId: Int, seatIndex: Int) {

        viewModelScope.launch {

            repository.clearSeatForClass(classId, seatIndex)

            _statusMessage.value = "Place ${seatIndex + 1} libérée."

        }

    }

    fun autoAssignSeatsAlphabetical(classId: Int) {

        viewModelScope.launch {

            repository.clearAllSeatsForClass(classId)

            val list = repository.getStudentsByClassList(classId).sortedBy {

                it.lastName.lowercase().trim() + " " + it.firstName.lowercase().trim()

            }

            list.forEachIndexed { index, student ->

                repository.updateStudent(student.copy(seatIndex = index))

            }

            _statusMessage.value = "Attribution automatique effectuée (${list.size} élèves)."

        }

    }

    fun clearAllClassSeats(classId: Int) {

        viewModelScope.launch {

            repository.clearAllSeatsForClass(classId)

            _statusMessage.value = "Toutes les places ont été libérées."

        }

    }

    fun reorderStudents(classId: Int, orderedStudents: List<StudentEntity>) {

        viewModelScope.launch {

            val updatedList = orderedStudents.mapIndexed { index, student ->

                student.copy(orderIndex = index + 1)

            }

            repository.updateStudents(updatedList)

        }

    }

    fun deleteStudent(student: StudentEntity) {

        viewModelScope.launch {

            repository.deleteStudent(student)

            _statusMessage.value = "Élève supprimé."

        }

    }

    // --- Grades Save ---

    suspend fun getGradeForStudentAndTerm(studentId: Int, term: Int): GradeEntity? {

        return repository.getGradeForStudentAndTerm(studentId, term)

    }

    suspend fun getStudentsWithGradesForTermList(classId: Int, term: Int): List<StudentWithGrade> {

        return repository.getStudentsWithGradesForTermList(classId, term)

    }

    fun saveStudentGrade(

        studentId: Int,

        term: Int,

        participation: Float?,

        discipline: Float?,

        assiduity: Float?,

        notebook: Float?,

        project: Float? = null,

        exam1: Float?,

        exam2: Float?,

        exam3: Float?,

        finalExam: Float?

    ) {

        viewModelScope.launch {

            val grade = GradeEntity(

                studentId = studentId,

                term = term,

                participation = participation,

                discipline = discipline,

                assiduity = assiduity,

                notebook = notebook,

                project = project,

                exam1 = exam1,

                exam2 = exam2,

                exam3 = exam3,

                finalExam = finalExam

            )

            repository.saveGrade(grade)

        }

    }

    // --- Journal CRUD ---

    fun addJournalEntry(

        date: String,

        timeSlot: String,

        subject: String,

        objectives: String,

        observations: String,

        photoPath: String? = null,

        audioPath: String? = null,

        targetClassId: Int? = null,

        activity: String = "",

        lessonTitle: String = ""

    ) {

        val classId = targetClassId ?: _selectedClassId.value ?: allClasses.value.firstOrNull()?.id ?: return

        viewModelScope.launch {

            repository.insertJournalEntry(

                JournalEntryEntity(

                    classId = classId,

                    date = date,

                    timeSlot = timeSlot,

                    courseSubject = subject,

                    objectives = objectives,

                    observations = observations,

                    photoPath = photoPath,

                    audioPath = audioPath,

                    activity = activity,

                    lessonTitle = lessonTitle

                )

            )

            _statusMessage.value = "Entrée journal pédagogique ajoutée."

        }

    }

    fun updateJournalEntry(entry: JournalEntryEntity) {

        viewModelScope.launch {

            repository.updateJournalEntry(entry)

            _statusMessage.value = "Fiche journal modifiée."

        }

    }

    fun deleteJournalEntry(id: Int) {

        viewModelScope.launch {

            repository.deleteJournalEntry(id)

            _statusMessage.value = "Entrée journal supprimée."

        }

    }

    // --- Timetable CRUD ---

    fun addTimetableSlot(

        classId: Int?,

        dayOfWeek: Int,

        timeSlot: String,

        subject: String,

        classroom: String,

        colorHex: String

    ) {

        val sanitizedDay = dayOfWeek.coerceIn(1, 5)

        viewModelScope.launch {

            repository.insertTimetableSlot(

                TimetableSlotEntity(

                    classId = classId,

                    dayOfWeek = sanitizedDay,

                    timeSlot = timeSlot,

                    subject = subject,

                    classroom = classroom,

                    colorHex = colorHex

                )

            )

            _statusMessage.value = "Créneau ajouté à l'emploi du temps."

        }

    }

    fun updateTimetableSlot(slot: TimetableSlotEntity) {

        viewModelScope.launch {

            repository.insertTimetableSlot(slot)

            _statusMessage.value = "Créneau mis à jour."

        }

    }

    fun updateTimetableSlotTime(slotId: Int, newTimeSlot: String) {

        viewModelScope.launch {

            val currentSlots = currentTimetableSlots.value

            val target = currentSlots.find { it.id == slotId }

            if (target != null) {

                repository.insertTimetableSlot(target.copy(timeSlot = newTimeSlot))

                _statusMessage.value = "Horaire modifié ($newTimeSlot)."

            }

        }

    }

    fun updateTimeSlotGlobally(oldTimeSlot: String, newTimeSlot: String, context: Context? = null) {

        val cleanOld = oldTimeSlot.trim()

        val cleanNew = newTimeSlot.trim()

        viewModelScope.launch {

            repository.updateTimeSlotGlobally(cleanOld, cleanNew)

            val currentMap = _timeSlotReplacements.value.toMutableMap()

            // If any key was pointing to cleanOld, update its destination to cleanNew

            for ((k, v) in currentMap.entries.toList()) {

                if (v.trim() == cleanOld) {

                    currentMap[k] = cleanNew

                }

            }

            currentMap[cleanOld] = cleanNew

            _timeSlotReplacements.value = currentMap

            if (context != null) {

                try {

                    val prefs = context.getSharedPreferences("timetable_config", Context.MODE_PRIVATE)

                    val jsonObj = org.json.JSONObject(currentMap as Map<*, *>)

                    prefs.edit().putString("time_slot_replacements", jsonObj.toString()).apply()

                } catch (e: Exception) {

                    Log.e(TAG, "Error persisting timetable replacement", e)

                }

            }

            _statusMessage.value = "Horaire mis à jour : $cleanNew"

        }

    }

    fun addCustomTimeSlot(slotStr: String, context: Context? = null) {

        val clean = slotStr.trim()

        if (clean.isBlank()) return

        viewModelScope.launch {

            val current = _customTimeSlots.value.toMutableSet()

            current.add(clean)

            _customTimeSlots.value = current

            val deleted = _deletedTimeSlots.value.toMutableSet()

            deleted.remove(clean)

            _deletedTimeSlots.value = deleted

            if (context != null) {

                try {

                    val prefs = context.getSharedPreferences("timetable_config", Context.MODE_PRIVATE)

                    prefs.edit()

                        .putString("custom_time_slots", org.json.JSONArray(current).toString())

                        .putString("deleted_time_slots", org.json.JSONArray(deleted).toString())

                        .apply()

                } catch (e: Exception) {

                    Log.e(TAG, "Error persisting custom time slots", e)

                }

            }

            _statusMessage.value = "Créneau horaire ajouté : $clean"

        }

    }

    fun deleteTimeSlotGlobally(targetTimeSlot: String, context: Context? = null) {

        val clean = targetTimeSlot.trim()

        viewModelScope.launch {

            repository.deleteTimeSlotGlobally(clean)

            val currentSet = _deletedTimeSlots.value.toMutableSet()

            currentSet.add(clean)

            _deletedTimeSlots.value = currentSet

            val currentCustom = _customTimeSlots.value.toMutableSet()

            currentCustom.remove(clean)

            _customTimeSlots.value = currentCustom

            if (context != null) {

                try {

                    val prefs = context.getSharedPreferences("timetable_config", Context.MODE_PRIVATE)

                    val jsonArr = org.json.JSONArray(currentSet)

                    prefs.edit()

                        .putString("deleted_time_slots", jsonArr.toString())

                        .putString("custom_time_slots", org.json.JSONArray(currentCustom).toString())

                        .apply()

                } catch (e: Exception) {

                    Log.e(TAG, "Error persisting deleted slots", e)

                }

            }

            _statusMessage.value = "Créneau horaire supprimé ($clean)"

        }

    }

    fun deleteTimetableSlot(id: Int) {

        viewModelScope.launch {

            repository.deleteTimetableSlot(id)

            _statusMessage.value = "Créneau supprimé."

        }

    }

    // --- Reminders (Rappels) ---

    fun addReminder(context: Context, title: String, description: String, date: String, time: String, isNotificationEnabled: Boolean) {

        viewModelScope.launch {

            val reminder = ReminderEntity(

                title = title,

                description = description,

                date = date,

                time = time,

                isNotificationEnabled = isNotificationEnabled

            )

            val id = repository.insertReminder(reminder)

            if (isNotificationEnabled) {

                scheduleNotification(context, reminder.copy(id = id.toInt()))

            }

            _statusMessage.value = "Rappel ajouté."

        }

    }

    fun deleteReminder(context: Context, reminder: ReminderEntity) {

        viewModelScope.launch {

            repository.deleteReminder(reminder.id)

            cancelNotification(context, reminder.id)

            _statusMessage.value = "Rappel supprimé."

        }

    }

    fun toggleReminderCompleted(context: Context, reminder: ReminderEntity) {

        viewModelScope.launch {

            val updated = reminder.copy(isCompleted = !reminder.isCompleted)

            repository.insertReminder(updated)

            if (updated.isCompleted) {

                cancelNotification(context, updated.id)

            } else if (updated.isNotificationEnabled) {

                scheduleNotification(context, updated)

            }

        }

    }

    private fun scheduleNotification(context: Context, reminder: ReminderEntity) {

        ReminderNotificationReceiver.scheduleNotification(context, reminder)

    }

    private fun cancelNotification(context: Context, id: Int) {

        ReminderNotificationReceiver.cancelNotification(context, id)

    }

    // --- Notes (Prise de notes) ---

    fun addNote(title: String, content: String) {

        viewModelScope.launch {

            repository.insertNote(NoteEntity(title = title, content = content))

            _statusMessage.value = "Note ajoutée."

        }

    }

    fun updateNote(note: NoteEntity) {

        viewModelScope.launch {

            repository.insertNote(note)

            _statusMessage.value = "Note modifiée."

        }

    }

    fun deleteNote(id: Int) {

        viewModelScope.launch {

            repository.deleteNote(id)

            _statusMessage.value = "Note supprimée."

        }

    }

    // --- Documents ---

    fun addCategory(name: String, parentCategoryId: Int? = null) {

        viewModelScope.launch {

            repository.insertCategory(DocumentCategoryEntity(name = name, parentCategoryId = parentCategoryId))

            _statusMessage.value = "Catégorie ajoutée."

        }

    }

    suspend fun addCategoryAndGetId(name: String, parentCategoryId: Int? = null): Int {

        return repository.insertCategory(DocumentCategoryEntity(name = name, parentCategoryId = parentCategoryId)).toInt()

    }

    suspend fun addDocumentDirect(categoryId: Int, title: String, fileUri: String): Long {

        return repository.insertDocument(DocumentEntity(categoryId = categoryId, title = title, fileUri = fileUri))

    }

    fun deleteCategory(category: DocumentCategoryEntity) {

        viewModelScope.launch(Dispatchers.IO) {

            suspend fun deleteCategoryRecursively(cat: DocumentCategoryEntity) {

                val subCategories = repository.getSubCategoriesList(cat.id)

                for (sub in subCategories) {

                    deleteCategoryRecursively(sub)

                }

                val docs = repository.getDocumentsByCategoryList(cat.id)

                for (doc in docs) {

                    try {

                        val path = doc.fileUri.removePrefix("file://")

                        val file = File(path)

                        if (file.exists() && file.absolutePath.contains("copied_documents")) {

                            file.delete()

                        }

                    } catch (e: Exception) {

                        Log.e(TAG, "Failed to physically delete file for document: ${doc.title}", e)

                    }

                }

                repository.deleteCategory(cat)

            }

            try {

                deleteCategoryRecursively(category)

            } catch (e: Exception) {

                Log.e(TAG, "Failed to clean up files for category ${category.name}", e)

            }

            if (selectedCategoryId.value == category.id) {

                selectedCategoryId.value = null

            }

            _statusMessage.value = "Catégorie supprimée."

        }

    }

    fun renameCategory(category: DocumentCategoryEntity, newName: String) {

        val trimmed = newName.trim()

        if (trimmed.isBlank()) return

        viewModelScope.launch {

            repository.insertCategory(category.copy(name = trimmed))

            _statusMessage.value = "Dossier renommé avec succès."

        }

    }

    fun addDocument(categoryId: Int, title: String, fileUri: String) {

        viewModelScope.launch {

            repository.insertDocument(DocumentEntity(categoryId = categoryId, title = title, fileUri = fileUri))

            _statusMessage.value = "Document ajouté."

        }

    }

    fun deleteDocument(document: DocumentEntity) {

        viewModelScope.launch(Dispatchers.IO) {

            try {

                val path = document.fileUri.removePrefix("file://")

                val file = File(path)

                if (file.exists() && file.absolutePath.contains("copied_documents")) {

                    file.delete()

                }

            } catch (e: Exception) {

                Log.e("AppViewModel", "Failed to physically delete document file: ${document.title}", e)

            }

            repository.deleteDocument(document)

            _statusMessage.value = "Document supprimé."

        }

    }

    fun renameDocument(document: DocumentEntity, newTitle: String) {

        val trimmed = newTitle.trim()

        if (trimmed.isBlank()) return

        viewModelScope.launch {

            repository.insertDocument(document.copy(title = trimmed))

            _statusMessage.value = "Document renommé avec succès."

        }

    }

    fun seedRegulatoryDocs(context: Context) {

        viewModelScope.launch {

            com.example.util.RegulatoryDocumentsHelper.seedRegulatoryDocuments(context, repository)

        }

    }

    // --- Gemini Assistant Integration ---

    fun generateAIWeeklySummary() {

        val currentEntries = currentJournalEntries.value

        val lang = settings.value?.activeLanguage ?: "fr"

        if (currentEntries.isEmpty()) {

            _geminiSummary.value = if (lang == "ar") "لا توجد مدخلات لتلخيصها." else "Aucune entrée disponible à résumer."

            return

        }

        viewModelScope.launch {

            _isGeneratingSummary.value = true

            try {

                val summary = GeminiSummaryHelper.generateWeeklySummary(currentEntries, lang)

                _geminiSummary.value = summary

            } catch (e: Exception) {

                _geminiSummary.value = "Erreur : ${e.localizedMessage}"

            } finally {

                _isGeneratingSummary.value = false

            }

        }

    }

    private fun autoArchivePdfBytes(context: Context, defaultName: String, pdfBytes: ByteArray) {
        try {
            com.example.util.AppStorageHelper.saveFileToClassHeure(
                context = context,
                fileName = defaultName,
                mimeType = "application/pdf",
                data = pdfBytes
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur autoArchivePdfBytes : ${e.message}")
        }
    }

    private fun writeBytesToUri(context: Context, targetUri: Uri, data: ByteArray): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri, "w")?.use { os ->
                os.write(data)
                os.flush()
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Erreur écriture vers $targetUri : ${e.message}")
            false
        }
    }

    // --- Export PDF ---
    fun exportSingleTimetablePdf(
        context: Context,
        targetUri: Uri,
        className: String,
        slotsList: List<TimetableSlotEntity>,
        lunchBreakLabel: String = "Pause déjeuner",
        deletedTimeSlots: Set<String> = emptySet(),
        timeSlotReplacements: Map<String, String> = emptyMap(),
        schoolYear: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val tempFile = File(context.cacheDir, "temp_tt_${System.currentTimeMillis()}.pdf")
            try {
                val profile = settings.value ?: SettingsEntity()
                val classesList = allClasses.value
                val targetClass = classesList.find { it.name.equals(className, ignoreCase = true) }
                val resolvedSchoolYear = when {
                    targetClass != null && targetClass.schoolYear.isNotBlank() -> targetClass.schoolYear
                    !schoolYear.isNullOrBlank() -> schoolYear
                    profile.currentSchoolYear.isNotBlank() -> profile.currentSchoolYear
                    classesList.any { it.schoolYear.isNotBlank() } -> classesList.first { it.schoolYear.isNotBlank() }.schoolYear
                    else -> {
                        val cal = java.util.Calendar.getInstance()
                        val y = cal.get(java.util.Calendar.YEAR)
                        val m = cal.get(java.util.Calendar.MONTH) + 1
                        if (m >= 8) "$y/${y + 1}" else "${y - 1}/$y"
                    }
                }
                val effectiveReplacements = if (timeSlotReplacements.isNotEmpty()) timeSlotReplacements else _timeSlotReplacements.value
                val effectiveDeleted = if (deletedTimeSlots.isNotEmpty()) deletedTimeSlots else _deletedTimeSlots.value
                val effectiveCustomSlots = _customTimeSlots.value

                val success = FileOutputStream(tempFile).use { fos ->
                    PdfExporter.generateSingleTimetablePdf(
                        outputStream = fos,
                        schoolName = profile.schoolName,
                        teacherName = profile.teacherName,
                        taughtSubject = profile.taughtSubject,
                        className = className,
                        slotsList = slotsList,
                        classesList = classesList,
                        freeDayIndex = if (className.contains("Enseignant", ignoreCase = true)) profile.teacherFreeDay else null,
                        lunchBreakLabel = lunchBreakLabel,
                        deletedTimeSlots = effectiveDeleted,
                        timeSlotReplacements = effectiveReplacements,
                        customTimeSlots = effectiveCustomSlots,
                        schoolYear = resolvedSchoolYear,
                        fontName = profile.activeFont,
                        context = context
                    )
                }

                if (success && tempFile.exists() && tempFile.length() > 0) {
                    val pdfBytes = tempFile.readBytes()
                    writeBytesToUri(context, targetUri, pdfBytes)
                    autoArchivePdfBytes(context, "Emploi_du_temps_${className}.pdf", pdfBytes)
                    _statusMessage.value = "Emploi du temps exporté en PDF !"
                } else {
                    _statusMessage.value = "Erreur lors de la génération du PDF."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Erreur d'écriture : ${e.localizedMessage}"
            } finally {
                tempFile.delete()
            }
        }
    }

    fun exportAllTimetablesPdf(
        context: Context,
        targetUri: Uri,
        slotsList: List<TimetableSlotEntity>,
        lunchBreakLabel: String = "Pause déjeuner",
        deletedTimeSlots: Set<String> = emptySet(),
        timeSlotReplacements: Map<String, String> = emptyMap()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val tempFile = File(context.cacheDir, "temp_all_tt_${System.currentTimeMillis()}.pdf")
            try {
                val profile = settings.value ?: SettingsEntity()
                val classesList = allClasses.value
                val effectiveReplacements = if (timeSlotReplacements.isNotEmpty()) timeSlotReplacements else _timeSlotReplacements.value
                val effectiveDeleted = if (deletedTimeSlots.isNotEmpty()) deletedTimeSlots else _deletedTimeSlots.value
                val effectiveCustomSlots = _customTimeSlots.value

                val success = FileOutputStream(tempFile).use { fos ->
                    PdfExporter.generateAllTimetablesPdf(
                        outputStream = fos,
                        schoolName = profile.schoolName,
                        teacherName = profile.teacherName,
                        taughtSubject = profile.taughtSubject,
                        classesList = classesList,
                        slotsList = slotsList,
                        teacherFreeDay = profile.teacherFreeDay,
                        lunchBreakLabel = lunchBreakLabel,
                        deletedTimeSlots = effectiveDeleted,
                        timeSlotReplacements = effectiveReplacements,
                        customTimeSlots = effectiveCustomSlots,
                        fontName = profile.activeFont,
                        context = context
                    )
                }

                if (success && tempFile.exists() && tempFile.length() > 0) {
                    val pdfBytes = tempFile.readBytes()
                    writeBytesToUri(context, targetUri, pdfBytes)
                    autoArchivePdfBytes(context, "Tous_les_emplois_du_temps.pdf", pdfBytes)
                    _statusMessage.value = "Tous les emplois du temps exportés en PDF !"
                } else {
                    _statusMessage.value = "Erreur lors de la génération du PDF."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Erreur d'écriture : ${e.localizedMessage}"
            } finally {
                tempFile.delete()
            }
        }
    }

    fun exportAttendancePdf(
        context: Context,
        targetUri: Uri,
        className: String,
        attendancesList: List<AttendanceEntity>,
        studentsList: List<StudentEntity>,
        schoolYear: String? = null,
        filterSubtitle: String = ""
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val tempFile = File(context.cacheDir, "temp_att_${System.currentTimeMillis()}.pdf")
            try {
                val profile = settings.value ?: SettingsEntity()
                val targetSchoolYear = schoolYear?.ifBlank { profile.currentSchoolYear.ifBlank { "2026/2027" } } ?: profile.currentSchoolYear.ifBlank { "2026/2027" }
                val targetTeacher = profile.teacherName.ifBlank { "L'enseignant" }
                val targetSchool = profile.schoolName.ifBlank { "Établissement Scolaire" }
                val activeFont = profile.activeFont

                val success = FileOutputStream(tempFile).use { fos ->
                    PdfExporter.generateAttendanceReportPdf(
                        context = context,
                        outputStream = fos,
                        teacherName = targetTeacher,
                        schoolName = targetSchool,
                        className = className,
                        schoolYear = targetSchoolYear,
                        attendancesList = attendancesList,
                        studentsList = studentsList,
                        taughtSubject = profile.taughtSubject,
                        fontName = activeFont,
                        filterSubtitle = filterSubtitle
                    )
                }

                if (success && tempFile.exists() && tempFile.length() > 0) {
                    val pdfBytes = tempFile.readBytes()
                    writeBytesToUri(context, targetUri, pdfBytes)
                    autoArchivePdfBytes(context, "Registre_Absences_${className}.pdf", pdfBytes)
                    _statusMessage.value = "Registre des absences exporté en PDF !"
                } else {
                    _statusMessage.value = "Erreur lors de la génération du PDF."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Erreur d'écriture : ${e.localizedMessage}"
            } finally {
                tempFile.delete()
            }
        }
    }

    fun exportSingleGradebookPdf(
        context: Context,
        targetUri: Uri,
        className: String,
        term: Int,
        gradesList: List<StudentWithGrade>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val tempFile = File(context.cacheDir, "temp_grades_${System.currentTimeMillis()}.pdf")
            try {
                val profile = settings.value ?: SettingsEntity()
                val success = FileOutputStream(tempFile).use { fos ->
                    PdfExporter.generateSingleGradebookPdf(
                        outputStream = fos,
                        schoolName = profile.schoolName,
                        teacherName = profile.teacherName,
                        taughtSubject = profile.taughtSubject,
                        className = className,
                        term = term,
                        gradesList = gradesList,
                        subjectCoefficient = profile.subjectCoefficient,
                        fontName = profile.activeFont,
                        context = context,
                        isPrimary = false
                    )
                }

                if (success && tempFile.exists() && tempFile.length() > 0) {
                    val pdfBytes = tempFile.readBytes()
                    writeBytesToUri(context, targetUri, pdfBytes)
                    autoArchivePdfBytes(context, "Notes_${className}_Trimestre_${term}.pdf", pdfBytes)
                    _statusMessage.value = "Carnet de notes exporté en PDF !"
                } else {
                    _statusMessage.value = "Erreur lors de la génération du PDF."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Erreur d'écriture : ${e.localizedMessage}"
            } finally {
                tempFile.delete()
            }
        }
    }

    fun exportSingleSeatingPlanPdf(
        context: Context,
        targetUri: Uri,
        className: String,
        studentsList: List<com.example.data.StudentEntity>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val tempFile = File(context.cacheDir, "temp_seating_${System.currentTimeMillis()}.pdf")
            try {
                val profile = settings.value ?: SettingsEntity()
                val success = FileOutputStream(tempFile).use { fos ->
                    PdfExporter.generateSingleSeatingPlanPdf(
                        outputStream = fos,
                        schoolName = profile.schoolName,
                        teacherName = profile.teacherName,
                        taughtSubject = profile.taughtSubject,
                        className = className,
                        studentsList = studentsList,
                        fontName = profile.activeFont,
                        context = context
                    )
                }

                if (success && tempFile.exists() && tempFile.length() > 0) {
                    val pdfBytes = tempFile.readBytes()
                    writeBytesToUri(context, targetUri, pdfBytes)
                    autoArchivePdfBytes(context, "Plan_de_classe_${className}.pdf", pdfBytes)
                    _statusMessage.value = "Plan de classe exporté en PDF !"
                } else {
                    _statusMessage.value = "Erreur lors de la génération du PDF."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Erreur d'écriture : ${e.localizedMessage}"
            } finally {
                tempFile.delete()
            }
        }
    }

    fun exportSingleClassListPdf(
        context: Context,
        targetUri: Uri,
        className: String,
        studentsList: List<com.example.data.StudentEntity>,
        groupSubtitle: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val tempFile = File(context.cacheDir, "temp_classlist_${System.currentTimeMillis()}.pdf")
            try {
                val profile = settings.value ?: SettingsEntity()
                val success = FileOutputStream(tempFile).use { fos ->
                    PdfExporter.generateSingleClassListPdf(
                        outputStream = fos,
                        schoolName = profile.schoolName,
                        teacherName = profile.teacherName,
                        taughtSubject = profile.taughtSubject,
                        className = className,
                        studentsList = studentsList,
                        fontName = profile.activeFont,
                        context = context,
                        groupSubtitle = groupSubtitle
                    )
                }

                if (success && tempFile.exists() && tempFile.length() > 0) {
                    val pdfBytes = tempFile.readBytes()
                    writeBytesToUri(context, targetUri, pdfBytes)
                    autoArchivePdfBytes(context, "Liste_eleves_${className}.pdf", pdfBytes)
                    _statusMessage.value = "Liste de classe exportée en PDF !"
                } else {
                    _statusMessage.value = "Erreur lors de la génération du PDF."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Erreur d'écriture : ${e.localizedMessage}"
            } finally {
                tempFile.delete()
            }
        }
    }

    fun exportClassStatisticsPdf(
        context: Context,
        targetUri: Uri,
        className: String,
        studentsList: List<StudentEntity>,
        gradesList: List<com.example.data.StudentWithGrade>,
        subjectCoefficient: Int,
        isPrimary: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val tempFile = File(context.cacheDir, "temp_stats_${System.currentTimeMillis()}.pdf")
            try {
                val profile = settings.value ?: SettingsEntity()
                val classesList = allClasses.value
                val targetClass = classesList.find { it.name.equals(className, ignoreCase = true) }
                val resolvedSchoolYear = when {
                    targetClass != null && targetClass.schoolYear.isNotBlank() -> targetClass.schoolYear
                    profile.currentSchoolYear.isNotBlank() -> profile.currentSchoolYear
                    classesList.any { it.schoolYear.isNotBlank() } -> classesList.first { it.schoolYear.isNotBlank() }.schoolYear
                    else -> {
                        val cal = java.util.Calendar.getInstance()
                        val y = cal.get(java.util.Calendar.YEAR)
                        val m = cal.get(java.util.Calendar.MONTH) + 1
                        if (m >= 8) "$y/${y + 1}" else "${y - 1}/$y"
                    }
                }

                val success = FileOutputStream(tempFile).use { fos ->
                    PdfExporter.generateClassStatisticsPdf(
                        outputStream = fos,
                        schoolName = profile.schoolName,
                        teacherName = profile.teacherName,
                        taughtSubject = profile.taughtSubject,
                        className = className,
                        schoolYear = resolvedSchoolYear,
                        studentsList = studentsList,
                        gradesList = gradesList,
                        subjectCoefficient = subjectCoefficient,
                        isPrimary = isPrimary,
                        fontName = profile.activeFont,
                        context = context
                    )
                }

                if (success && tempFile.exists() && tempFile.length() > 0) {
                    val pdfBytes = tempFile.readBytes()
                    writeBytesToUri(context, targetUri, pdfBytes)
                    autoArchivePdfBytes(context, "Statistiques_${className}.pdf", pdfBytes)
                    _statusMessage.value = if (com.example.util.Tr.isArabicLang()) "تم تصدير الإحصائيات إلى PDF بنجاح !" else "Statistiques de la classe exportées en PDF !"
                } else {
                    _statusMessage.value = if (com.example.util.Tr.isArabicLang()) "خطأ أثناء إنشاء ملف PDF." else "Erreur lors de la génération du PDF."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Erreur : ${e.localizedMessage}"
            } finally {
                tempFile.delete()
            }
        }
    }

    fun exportSingleJournalPdf(
        context: Context,
        targetUri: Uri,
        className: String,
        term: Int,
        entriesList: List<JournalEntryEntity>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val tempFile = File(context.cacheDir, "temp_journal_${System.currentTimeMillis()}.pdf")
            try {
                val profile = settings.value ?: SettingsEntity()
                val classesList = allClasses.value
                val targetClass = classesList.find { it.name.equals(className, ignoreCase = true) }
                val resolvedSchoolYear = when {
                    targetClass != null && targetClass.schoolYear.isNotBlank() -> targetClass.schoolYear
                    profile.currentSchoolYear.isNotBlank() -> profile.currentSchoolYear
                    classesList.any { it.schoolYear.isNotBlank() } -> classesList.first { it.schoolYear.isNotBlank() }.schoolYear
                    else -> {
                        val cal = java.util.Calendar.getInstance()
                        val y = cal.get(java.util.Calendar.YEAR)
                        val m = cal.get(java.util.Calendar.MONTH) + 1
                        if (m >= 8) "$y/${y + 1}" else "${y - 1}/$y"
                    }
                }

                val success = FileOutputStream(tempFile).use { fos ->
                    PdfExporter.generateJournalPdf(
                        outputStream = fos,
                        schoolName = profile.schoolName,
                        teacherName = profile.teacherName,
                        taughtSubject = profile.taughtSubject,
                        className = className,
                        schoolYear = resolvedSchoolYear,
                        term = term,
                        entriesList = entriesList,
                        classesList = classesList,
                        fontName = profile.activeFont,
                        context = context
                    )
                }

                if (success && tempFile.exists() && tempFile.length() > 0) {
                    val pdfBytes = tempFile.readBytes()
                    writeBytesToUri(context, targetUri, pdfBytes)
                    autoArchivePdfBytes(context, "Cahier_Journal_${className}_Trimestre_${term}.pdf", pdfBytes)
                    _statusMessage.value = "Cahier journal exporté en PDF !"
                } else {
                    _statusMessage.value = "Erreur lors de la génération du PDF."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Erreur d'écriture : ${e.localizedMessage}"
            } finally {
                tempFile.delete()
            }
        }
    }

    fun exportReportToPdf(
        context: Context,
        targetUri: Uri,
        classesToInclude: List<Int>,
        termsToInclude: List<Int>,
        includeGrades: Boolean,
        includeTimetable: Boolean,
        includeJournal: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val tempFile = File(context.cacheDir, "temp_report_${System.currentTimeMillis()}.pdf")
            try {
                // Prepare database states synchronously for the exporter
                val classes = repository.allClasses.firstOrNull() ?: emptyList()
                val studentsMap = mutableMapOf<Int, List<StudentEntity>>()
                val gradesMap = mutableMapOf<Int, Map<Int, List<StudentWithGrade>>>()
                val timetableMap = mutableMapOf<Int, List<TimetableSlotEntity>>()
                val journalMap = mutableMapOf<Int, List<JournalEntryEntity>>()

                for (classId in classesToInclude) {
                    studentsMap[classId] = repository.getStudentsByClassList(classId)
                    if (includeTimetable) {
                        timetableMap[classId] = repository.getTimetableSlotsByClassList(classId)
                    }
                    if (includeJournal) {
                        journalMap[classId] = repository.allJournalEntries.firstOrNull()?.filter { it.classId == classId } ?: emptyList()
                    }
                    if (includeGrades) {
                        val termGrades = mutableMapOf<Int, List<StudentWithGrade>>()
                        for (term in termsToInclude) {
                            termGrades[term] = repository.getStudentsWithGradesForTermList(classId, term)
                        }
                        gradesMap[classId] = termGrades
                    }
                }

                val fileName = "Rapport_Export.pdf"
                val profile = settings.value ?: SettingsEntity()

                val success = FileOutputStream(tempFile).use { fos ->
                    PdfExporter.generateDossierPdf(
                        context = context,
                        outputStream = fos,
                        teacherName = profile.teacherName,
                        taughtSubject = profile.taughtSubject,
                        schoolName = profile.schoolName,
                        classesList = classes,
                        studentsMap = studentsMap,
                        gradesMap = gradesMap,
                        timetableMap = timetableMap,
                        journalMap = journalMap,
                        selectedClasses = classesToInclude,
                        selectedTerms = termsToInclude,
                        includeGrades = includeGrades,
                        includeTimetable = includeTimetable,
                        includeJournal = includeJournal,
                        subjectCoefficient = profile.subjectCoefficient,
                        fontName = profile.activeFont
                    )
                }

                if (success && tempFile.exists() && tempFile.length() > 0) {
                    val pdfBytes = tempFile.readBytes()
                    writeBytesToUri(context, targetUri, pdfBytes)
                    autoArchivePdfBytes(context, fileName, pdfBytes)
                    _statusMessage.value = "Rapport PDF exporté : $fileName (Documents/ClassHeure)"
                    triggerPdfShareOrView(context, targetUri)
                } else {
                    _statusMessage.value = "Échec de l'exportation du PDF."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception PDF", e)
                _statusMessage.value = "Erreur PDF : ${e.localizedMessage}"
            } finally {
                tempFile.delete()
            }
        }
    }

    private fun triggerPdfShareOrView(context: Context, fileUri: Uri) {
        try {
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to view/share PDF", e)
        }
    }

    // --- Backup & Restore ---

    fun backupDatabaseEncrypted(password: String, context: Context? = null, onComplete: (String?) -> Unit) {

        viewModelScope.launch(Dispatchers.IO) {

            val encrypted = try {

                BackupHelper.exportBackup(repository, password, context)

            } catch (e: Exception) {

                null

            }

            if (encrypted != null && context != null) {

                try {

                    val dateTag = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())

                    com.example.util.AppStorageHelper.autoSaveExportedFile(context, "Sauvegarde_ClassHeure_${dateTag}.txt") { stream ->

                        stream.write(encrypted.toByteArray(Charsets.UTF_8))

                    }

                } catch (e: Exception) {

                    // Ignore

                }

            }

            withContext(Dispatchers.Main) {

                onComplete(encrypted)

            }

        }

    }

    fun restoreDatabaseEncrypted(encryptedText: String, password: String, context: Context? = null, onComplete: (Boolean) -> Unit) {

        viewModelScope.launch(Dispatchers.IO) {

            val success = BackupHelper.importBackup(repository, encryptedText, password, context)

            if (success) {

                _statusMessage.value = "Base de données restaurée !"

            } else {

                _statusMessage.value = "Échec de restauration (Mot de passe incorrect ou données corrompues)."

            }

            withContext(Dispatchers.Main) {

                onComplete(success)

            }

        }

    }

}

class AppViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {

            @Suppress("UNCHECKED_CAST")

            return AppViewModel(repository) as T

        }

        throw IllegalArgumentException("Unknown ViewModel class")

    }

}

