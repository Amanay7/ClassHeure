package com.example.ui.screens

import android.content.Context

import android.content.Intent

import android.net.Uri

import android.widget.Toast

import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.Canvas

import androidx.compose.foundation.background

import androidx.compose.foundation.clickable

import androidx.compose.foundation.horizontalScroll

import androidx.compose.foundation.isSystemInDarkTheme

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.lazy.itemsIndexed

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.*

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.draw.shadow

import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.geometry.Size

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.StrokeCap

import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.platform.LocalLayoutDirection

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.input.ImeAction

import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.ui.text.style.TextDirection

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.LayoutDirection

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import androidx.core.content.FileProvider

import com.example.util.AdaptiveScreenContainer

import com.example.data.AttendanceEntity

import com.example.data.ClassEntity

import com.example.data.StudentEntity

import com.example.data.TimetableSlotEntity

import com.example.data.PdfExporter

import com.example.ui.AppViewModel

import com.example.ui.components.FineSearchBar

import com.example.ui.components.GlassmorphicCard

import com.example.util.Tr

import com.example.util.tr

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext

import java.io.File

import java.io.FileOutputStream

import java.text.SimpleDateFormat

import java.util.*

// Helper date conversions

private fun formatDbDateToDisplay(dbDate: String): String {

    // dbDate: YYYY-MM-DD -> DD-MM-YYYY

    val parts = dbDate.split("-")

    if (parts.size == 3 && parts[0].length == 4) {

        return "${parts[2]}-${parts[1]}-${parts[0]}"

    }

    return dbDate

}

private fun formatDisplayDateToDb(displayDate: String): String {

    // displayDate: DD-MM-YYYY -> YYYY-MM-DD

    val parts = displayDate.split("-")

    if (parts.size == 3 && parts[2].length == 4) {

        return "${parts[2]}-${parts[1]}-${parts[0]}"

    }

    return displayDate

}

private data class StudentStatSummary(

    val student: StudentEntity,

    val totalCas: Int,

    val absCount: Int,

    val retCount: Int,

    val excCount: Int,

    val delayMinutesSum: Int,

    val justifiedCount: Int

)

@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun AttendanceScreen(viewModel: AppViewModel) {

    val context = LocalContext.current

    val settings by viewModel.settings.collectAsState()

    val isAr = (settings?.activeLanguage == "ar") || Tr.isArabic() || com.example.util.I18n.isRtl()

    val isSystemDark = isSystemInDarkTheme()

    val isDark = settings?.activeTheme == "dark" || (settings?.activeTheme == "system" && isSystemDark) || (settings?.activeTheme != "light" && isSystemDark) || MaterialTheme.colorScheme.background != com.example.ui.theme.OffWhite

    val coroutineScope = rememberCoroutineScope()

    val classes by viewModel.allClasses.collectAsState()

    val teacherSlots by viewModel.teacherTimetableSlots.collectAsState()

    val allTimetableSlots by viewModel.allTimetableSlots.collectAsState()

    val timeSlotReplacements by viewModel.timeSlotReplacements.collectAsState()

    val deletedTimeSlots by viewModel.deletedTimeSlots.collectAsState()

    val allAttendances by viewModel.allAttendances.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }

    // Today in DD-MM-YYYY display format

    val todayDisplayDateStr = remember {

        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

    }

    var selectedDisplayDateStr by remember { mutableStateOf(todayDisplayDateStr) }

    var selectedClassId by remember { mutableStateOf<Int?>(classes.firstOrNull()?.id) }

    var selectedTimeSlot by remember { mutableStateOf("08:00 - 09:00") }

    var selectedSubject by remember { mutableStateOf("") }

    var studentSearchQuery by remember { mutableStateOf("") }

    // Global student lookup map (studentId -> StudentEntity)

    var studentMap by remember { mutableStateOf<Map<Int, StudentEntity>>(emptyMap()) }

    LaunchedEffect(classes) {

        val map = mutableMapOf<Int, StudentEntity>()

        classes.forEach { c ->

            val list = viewModel.getStudentsByClassList(c.id)

            list.forEach { s -> map[s.id] = s }

        }

        studentMap = map

    }

    // Initialize Matière from teacher profile when settings load

    LaunchedEffect(settings?.taughtSubject) {

        val profileSubj = settings?.taughtSubject ?: ""

        if (profileSubj.isNotBlank()) {

            selectedSubject = profileSubj

        }

    }

    // Keep class selected when classes change

    LaunchedEffect(classes) {

        if (selectedClassId == null && classes.isNotEmpty()) {

            selectedClassId = classes.first().id

        }

    }

    // DB Date YYYY-MM-DD for calculations & DB queries

    val dbDateStr = remember(selectedDisplayDateStr) {

        formatDisplayDateToDb(selectedDisplayDateStr)

    }

    // Determine current day of week for date (Sunday=1, Monday=2... Thursday=5)

    val cal = remember(dbDateStr) {

        Calendar.getInstance().apply {

            try {

                val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dbDateStr)

                if (d != null) time = d

            } catch (e: Exception) {}

        }

    }

    val dayOfWeekAndroid = cal.get(Calendar.DAY_OF_WEEK)

    val dayOfWeekApp = when (dayOfWeekAndroid) {

        Calendar.SUNDAY -> 1

        Calendar.MONDAY -> 2

        Calendar.TUESDAY -> 3

        Calendar.WEDNESDAY -> 4

        Calendar.THURSDAY -> 5

        Calendar.FRIDAY -> 6

        Calendar.SATURDAY -> 7

        else -> 1

    }

    // Détection de réception des parents pour exclure ce type de créneau du carnet d'absences

    fun isParentReceptionSlot(slot: TimetableSlotEntity): Boolean {

        val s = slot.subject.lowercase().trim()

        val r = slot.classroom.lowercase().trim()

        return s.contains("réception") || s.contains("reception") || s.contains("استقبال") || s.contains("أولياء") ||

               r.contains("réception") || r.contains("reception") || r.contains("استقبال") || r.contains("أولياء")

    }

    // Timetable slots for this day of week from teacher schedule (or fallback to classes schedule)

    val slotsForSelectedDay = remember(teacherSlots, allTimetableSlots, dayOfWeekApp, deletedTimeSlots) {

        val base = if (teacherSlots.isNotEmpty()) {

            teacherSlots.filter { it.dayOfWeek == dayOfWeekApp && !deletedTimeSlots.contains(it.timeSlot.trim()) }

        } else {

            allTimetableSlots.filter { it.dayOfWeek == dayOfWeekApp && !deletedTimeSlots.contains(it.timeSlot.trim()) }

        }

        base.filter { !isParentReceptionSlot(it) }

    }

    val allTeacherTimeSlots = remember(teacherSlots, allTimetableSlots, deletedTimeSlots, timeSlotReplacements) {

        val source = (if (teacherSlots.isNotEmpty()) teacherSlots else allTimetableSlots).filter { !isParentReceptionSlot(it) }

        com.example.util.TimetableTimeSlotHelper.computeHarmonizedTimeSlots(

            allSlots = source,

            timeSlotReplacements = timeSlotReplacements,

            deletedTimeSlots = deletedTimeSlots

        ).ifEmpty {

            source

                .map { it.timeSlot.trim() }

                .filter { it.isNotBlank() && !deletedTimeSlots.contains(it) }

                .distinct()

        }

    }

    // Détection robuste de la classe concernée par un créneau

    fun findClassForSlot(slot: TimetableSlotEntity): ClassEntity? {

        if (isParentReceptionSlot(slot)) return null

        if (slot.classId != null) {

            val found = classes.find { it.id == slot.classId }

            if (found != null) return found

        }

        val cleanSubj = slot.subject.trim()

        val cleanRoom = slot.classroom.trim()

        // 1. Correspondance exacte nom de classe

        classes.find { cleanSubj.equals(it.name.trim(), ignoreCase = true) }?.let { return it }

        // 2. Nom de classe contenu ou préfixé

        classes.find {

            cleanSubj.contains(it.name.trim(), ignoreCase = true) ||

            it.name.trim().contains(cleanSubj, ignoreCase = true) ||

            cleanSubj.startsWith(it.name.trim(), ignoreCase = true) ||

            it.name.trim().startsWith(cleanSubj, ignoreCase = true)

        }?.let { return it }

        // 3. Correspondance avec la salle (si renseignée comme classe)

        if (cleanRoom.isNotBlank()) {

            classes.find {

                cleanRoom.equals(it.name.trim(), ignoreCase = true) ||

                cleanRoom.contains(it.name.trim(), ignoreCase = true) ||

                it.name.trim().contains(cleanRoom, ignoreCase = true)

            }?.let { return it }

        }

        // 4. Recherche dans les emplois du temps des classes (allTimetableSlots) pour ce jour & horaire

        val classSlot = allTimetableSlots.firstOrNull {

            it.dayOfWeek == slot.dayOfWeek &&

            it.timeSlot.trim().equals(slot.timeSlot.trim(), ignoreCase = true) &&

            it.classId != null

        }

        if (classSlot?.classId != null) {

            classes.find { it.id == classSlot.classId }?.let { return it }

        }

        // 5. Recherche par matière similaire dans les emplois du temps de classes

        if (cleanSubj.isNotBlank()) {

            val subjSlot = allTimetableSlots.firstOrNull {

                it.dayOfWeek == slot.dayOfWeek &&

                it.timeSlot.trim().equals(slot.timeSlot.trim(), ignoreCase = true) &&

                (it.subject.contains(cleanSubj, ignoreCase = true) || cleanSubj.contains(it.subject, ignoreCase = true)) &&

                it.classId != null

            }

            if (subjSlot?.classId != null) {

                classes.find { it.id == subjSlot.classId }?.let { return it }

            }

        }

        // 6. Recherche dans l'historique d'appel déjà enregistré pour cette date & heure

        val attCid = allAttendances.firstOrNull {

            it.date == dbDateStr && it.timeSlot.trim().equals(slot.timeSlot.trim(), ignoreCase = true)

        }?.classId

        if (attCid != null) {

            classes.find { it.id == attCid }?.let { return it }

        }

        if (classes.size == 1) return classes.first()

        return null

    }

    // Classes concernées par le jour sélectionné (selon l'emploi du temps de l'enseignant)

    val classesForSelectedDay = remember(slotsForSelectedDay, classes, teacherSlots, allTimetableSlots) {

        if (teacherSlots.isEmpty() && allTimetableSlots.isEmpty()) {

            classes

        } else {

            val found = slotsForSelectedDay.mapNotNull { findClassForSlot(it) }.distinctBy { it.id }

            found

        }

    }

    // Créneaux horaires disponibles selon la classe sélectionnée et le jour choisi

    val slotsForSelectedClassAndDay = remember(slotsForSelectedDay, selectedClassId) {

        if (selectedClassId == null) {

            slotsForSelectedDay

        } else {

            val matching = slotsForSelectedDay.filter { slot ->

                val c = findClassForSlot(slot)

                c?.id == selectedClassId

            }

            if (matching.isNotEmpty()) matching else slotsForSelectedDay

        }

    }

    // When a timetable slot is picked from dropdown or list

    var selectedTimetableSlotId by remember { mutableStateOf<Int?>(null) }

    // Sélection d'un créneau : bascule automatiquement sur la classe concernée par ce créneau

    fun selectTimeSlot(slotText: String, explicitSlot: TimetableSlotEntity? = null) {

        val cleanSlotText = slotText.trim()

        selectedTimeSlot = cleanSlotText

        val slot = explicitSlot

            ?: slotsForSelectedDay.find { it.timeSlot.trim().equals(cleanSlotText, ignoreCase = true) }

            ?: allTimetableSlots.find { it.dayOfWeek == dayOfWeekApp && it.timeSlot.trim().equals(cleanSlotText, ignoreCase = true) }

            ?: teacherSlots.find { it.dayOfWeek == dayOfWeekApp && it.timeSlot.trim().equals(cleanSlotText, ignoreCase = true) }

        if (slot != null) {

            selectedTimetableSlotId = slot.id

            val targetClass = findClassForSlot(slot)

            if (targetClass != null) {

                selectedClassId = targetClass.id

            }

            val profileSubj = settings?.taughtSubject ?: ""

            selectedSubject = if (profileSubj.isNotBlank()) profileSubj else slot.subject

        } else {

            val attCid = allAttendances.firstOrNull {

                it.date == dbDateStr && it.timeSlot.trim().equals(cleanSlotText, ignoreCase = true)

            }?.classId

            if (attCid != null) {

                selectedClassId = attCid

            }

        }

    }

    LaunchedEffect(classesForSelectedDay, slotsForSelectedDay, dayOfWeekApp, selectedDisplayDateStr) {

        if (classesForSelectedDay.isNotEmpty()) {

            if (selectedClassId == null || classesForSelectedDay.none { it.id == selectedClassId }) {

                selectedClassId = classesForSelectedDay.first().id

            }

        } else {

            selectedClassId = null

        }

        val availableSlots = if (selectedClassId != null) {

            val matching = slotsForSelectedDay.filter { slot ->

                findClassForSlot(slot)?.id == selectedClassId

            }

            if (matching.isNotEmpty()) matching else slotsForSelectedDay

        } else {

            slotsForSelectedDay

        }

        if (availableSlots.isNotEmpty()) {

            val currentValid = availableSlots.find { it.timeSlot.trim().equals(selectedTimeSlot.trim(), ignoreCase = true) }

            if (currentValid != null) {

                selectTimeSlot(currentValid.timeSlot.trim(), currentValid)

            } else {

                val firstSlot = availableSlots.first()

                selectTimeSlot(firstSlot.timeSlot.trim(), firstSlot)

            }

        } else {

            selectedTimeSlot = ""

            selectedTimetableSlotId = null

        }

    }

    LaunchedEffect(selectedClassId) {

        if (selectedClassId != null && slotsForSelectedDay.isNotEmpty()) {

            val classSlots = slotsForSelectedDay.filter { slot ->

                findClassForSlot(slot)?.id == selectedClassId

            }

            if (classSlots.isNotEmpty() && classSlots.none { it.timeSlot.trim().equals(selectedTimeSlot.trim(), ignoreCase = true) }) {

                val firstSlot = classSlots.first()

                selectTimeSlot(firstSlot.timeSlot.trim(), firstSlot)

            }

        }

    }

    LaunchedEffect(selectedTimetableSlotId) {

        val slot = slotsForSelectedDay.find { it.id == selectedTimetableSlotId }

        if (slot != null) {

            selectTimeSlot(slot.timeSlot.trim(), slot)

        }

    }

    // Students for current selected class or all classes — SORTED IDENTICALLY TO STUDENTS SCREEN

    var currentClassStudents by remember { mutableStateOf<List<StudentEntity>>(emptyList()) }

    LaunchedEffect(selectedClassId, studentMap) {

        if (selectedClassId != null) {

            val list = viewModel.getStudentsByClassList(selectedClassId!!)

            currentClassStudents = list.sortedWith(

                compareBy<StudentEntity> { it.orderIndex }

                    .thenBy { it.lastName.lowercase(Locale.getDefault()) }

                    .thenBy { it.firstName.lowercase(Locale.getDefault()) }

            )

        } else {

            currentClassStudents = studentMap.values.sortedWith(

                compareBy<StudentEntity> { it.orderIndex }

                    .thenBy { it.lastName.lowercase(Locale.getDefault()) }

                    .thenBy { it.firstName.lowercase(Locale.getDefault()) }

            )

        }

    }

    // Local state for student statuses during attendance session

    data class AttendanceDraft(

        val status: String = "PRESENT", // "PRESENT", "ABSENT", "RETARD", "EXCLUDED"

        val isJustified: Boolean = false,

        val delayMinutes: Int = 10,

        val reason: String = ""

    )

    var studentStatusMap by remember(selectedClassId, dbDateStr, selectedTimeSlot) {

        mutableStateOf(mapOf<Int, AttendanceDraft>())

    }

    // Auto-save helper: saves immediately on every status or reason change

    fun autoSaveAttendances(updatedMap: Map<Int, AttendanceDraft>) {

        if (currentClassStudents.isEmpty()) return

        val listToSave = currentClassStudents.map { student ->

            val draft = updatedMap[student.id] ?: AttendanceDraft(status = "PRESENT")

            AttendanceEntity(

                studentId = student.id,

                classId = student.classId,

                date = dbDateStr,

                timeSlot = selectedTimeSlot,

                subject = selectedSubject.ifBlank { settings?.taughtSubject ?: "" },

                status = draft.status,

                isJustified = draft.isJustified,

                delayMinutes = if (draft.status == "RETARD") draft.delayMinutes else 0,

                reason = draft.reason

            )

        }

        viewModel.saveAttendancesForSession(listToSave)

    }

    // Load existing records for this date/slot/class if any

    LaunchedEffect(selectedClassId, dbDateStr, selectedTimeSlot, allAttendances) {

        val existing = if (selectedClassId != null) {

            allAttendances.filter {

                it.classId == selectedClassId && it.date == dbDateStr && it.timeSlot == selectedTimeSlot

            }

        } else {

            allAttendances.filter {

                it.date == dbDateStr && it.timeSlot == selectedTimeSlot

            }

        }

        if (existing.isNotEmpty()) {

            val newMap = mutableMapOf<Int, AttendanceDraft>()

            existing.forEach { att ->

                newMap[att.studentId] = AttendanceDraft(

                    status = att.status,

                    isJustified = att.isJustified,

                    delayMinutes = att.delayMinutes,

                    reason = att.reason

                )

            }

            studentStatusMap = newMap

        } else {

            studentStatusMap = emptyMap()

        }

    }

    // Dialog state for Editing an existing attendance record

    var editingAttendance by remember { mutableStateOf<AttendanceEntity?>(null) }

    var attendanceToDelete by remember { mutableStateOf<AttendanceEntity?>(null) }

    var showCalendarPickerModal by remember { mutableStateOf(false) }

    if (showCalendarPickerModal) {

        val initialEpochMillis = remember(selectedDisplayDateStr) {

            try {

                val parts = selectedDisplayDateStr.split("-", "/")

                if (parts.size == 3) {

                    val d = parts[0].toIntOrNull()

                    val m = parts[1].toIntOrNull()

                    val y = parts[2].toIntOrNull()

                    if (d != null && m != null && y != null && y in 1900..2100) {

                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

                        cal.set(y, m - 1, d, 12, 0, 0)

                        cal.timeInMillis

                    } else null

                } else null

            } catch (e: Exception) {

                null

            } ?: System.currentTimeMillis()

        }

        val datePickerState = rememberDatePickerState(

            initialSelectedDateMillis = initialEpochMillis

        )

        DatePickerDialog(

            onDismissRequest = { showCalendarPickerModal = false },

            confirmButton = {

                TextButton(

                    onClick = {

                        val selectedMillis = datePickerState.selectedDateMillis

                        if (selectedMillis != null) {

                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

                            cal.timeInMillis = selectedMillis

                            val d = cal.get(Calendar.DAY_OF_MONTH)

                            val m = cal.get(Calendar.MONTH) + 1

                            val y = cal.get(Calendar.YEAR)

                            selectedDisplayDateStr = String.format(Locale.US, "%02d-%02d-%04d", d, m, y)

                        }

                        showCalendarPickerModal = false

                    }

                ) {

                    Text(if (isAr) "موافق" else "OK", fontWeight = FontWeight.Bold)

                }

            },

            dismissButton = {

                TextButton(onClick = { showCalendarPickerModal = false }) {

                    Text(if (isAr) "إلغاء" else "Annuler")

                }

            }

        ) {

            DatePicker(state = datePickerState)

        }

    }

    if (editingAttendance != null) {

        val att = editingAttendance!!

        val student = remember(att.studentId, studentMap) { studentMap[att.studentId] }

        var editStatus by remember { mutableStateOf(att.status) }

        var editIsJustified by remember { mutableStateOf(att.isJustified) }

        var editDelayMins by remember { mutableStateOf(if (att.delayMinutes > 0) att.delayMinutes else 10) }

        var editTimeSlot by remember { mutableStateOf(att.timeSlot) }

        var editDisplayDate by remember { mutableStateOf(formatDbDateToDisplay(att.date)) }

        var editSubject by remember { mutableStateOf(att.subject) }

        var editReason by remember { mutableStateOf(att.reason) }

        AlertDialog(

            onDismissRequest = { editingAttendance = null },

            title = {

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)

                    Text(if (isAr) "تعديل التسجيل" else "Modifier l'enregistrement", fontWeight = FontWeight.Bold)

                }

            },

            text = {

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    Text(

                        text = "${if (isAr) "التلميذ" else "Élève"} : ${student?.lastName ?: ""} ${student?.firstName ?: ""}",

                        fontWeight = FontWeight.Bold,

                        style = MaterialTheme.typography.bodyMedium

                    )

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {

                        OutlinedTextField(

                            value = editDisplayDate,

                            onValueChange = { editDisplayDate = it },

                            label = { Text(if (isAr) "التاريخ " else "Date ") },

                            singleLine = true,

                            modifier = Modifier.fillMaxWidth(),

                            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)

                        )

                    }

                    OutlinedTextField(

                        value = editTimeSlot,

                        onValueChange = { editTimeSlot = it },

                        label = { Text(if (isAr) "التوقيت / الحصة" else "Horaire") },

                        singleLine = true,

                        modifier = Modifier.fillMaxWidth()

                    )

                    OutlinedTextField(

                        value = editSubject,

                        onValueChange = { editSubject = it },

                        label = { Text(if (isAr) "المادة" else "Matière") },

                        singleLine = true,

                        modifier = Modifier.fillMaxWidth()

                    )

                    Text(if (isAr) "الحالة :" else "Statut :", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {

                        listOf(

                            "PRESENT" to (if (isAr) "حاضر" else "Présent"),

                            "ABSENT" to (if (isAr) "غائب" else "Absent"),

                            "RETARD" to (if (isAr) "تأخر" else "Retard"),

                            "EXCLUDED" to (if (isAr) "مطرود" else "Exclus")

                        ).forEach { (st, label) ->

                            FilterChip(

                                selected = editStatus == st,

                                onClick = { editStatus = st },

                                label = { Text(label, fontSize = 11.sp) }

                            )

                        }

                    }

                    if (editStatus == "RETARD") {

                        Text(if (isAr) "مدة التأخر :" else "Durée du retard :", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {

                            listOf(5, 10, 15, 20, 30).forEach { mins ->

                                FilterChip(

                                    selected = editDelayMins == mins,

                                    onClick = { editDelayMins = mins },

                                    label = { Text(if (isAr) "$mins د" else "$mins min", fontSize = 11.sp) }

                                )

                            }

                        }

                    }

                    if (editStatus != "PRESENT") {

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.SpaceBetween,

                            modifier = Modifier.fillMaxWidth()

                        ) {

                            Text(if (isAr) "غياب/تأخر مبرر ؟" else "Absence/Retard Justifié ?", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)

                            Switch(

                                checked = editIsJustified,

                                onCheckedChange = { editIsJustified = it }

                            )

                        }

                        OutlinedTextField(

                            value = editReason,

                            onValueChange = { editReason = it },

                            label = { Text(if (isAr) "السبب / ملاحظة" else "Motif / Observation") },

                            modifier = Modifier.fillMaxWidth()

                        )

                    }

                }

            },

            confirmButton = {

                Button(

                    onClick = {

                        viewModel.updateAttendance(

                            att.copy(

                                date = formatDisplayDateToDb(editDisplayDate),

                                timeSlot = editTimeSlot,

                                subject = editSubject,

                                status = editStatus,

                                isJustified = editIsJustified,

                                delayMinutes = if (editStatus == "RETARD") editDelayMins else 0,

                                reason = editReason

                            )

                        )

                        editingAttendance = null

                    }

                ) {

                    Text(if (isAr) "حفظ" else "Enregistrer")

                }

            },

            dismissButton = {

                TextButton(onClick = { editingAttendance = null }) {

                    Text(if (isAr) "إلغاء" else "Annuler")

                }

            }

        )

    }

    if (attendanceToDelete != null) {

        val att = attendanceToDelete!!

        val student = remember(att.studentId, studentMap) { studentMap[att.studentId] }

        val studentName = if (student != null) "${student.lastName} ${student.firstName}" else "Élève #${att.studentId}"

        val statusText = when (att.status) {

            "ABSENT" -> if (isAr) "الغياب" else "l'absence"

            "RETARD" -> if (isAr) "التأخر (${att.delayMinutes} د)" else "le retard (${att.delayMinutes} min)"

            "EXCLUDED" -> if (isAr) "طرد الحصة" else "l'exclusion de cours"

            else -> if (isAr) "التسجيل" else "l'enregistrement"

        }

        AlertDialog(

            onDismissRequest = { attendanceToDelete = null },

            title = {

                Row(

                    verticalAlignment = Alignment.CenterVertically,

                    horizontalArrangement = Arrangement.spacedBy(8.dp)

                ) {

                    Icon(

                        imageVector = Icons.Default.Warning,

                        contentDescription = null,

                        tint = MaterialTheme.colorScheme.error

                    )

                    Text(

                        text = if (isAr) "تأكيد الحذف" else "Confirmer la suppression",

                        fontWeight = FontWeight.Bold,

                        style = MaterialTheme.typography.titleMedium

                    )

                }

            },

            text = {

                Text(

                    text = if (isAr)

                        "هل أنت متأكد من حذف $statusText للتلميذ $studentName (حصة ${formatDbDateToDisplay(att.date)} - ${att.timeSlot})؟\n\nهذا الإجراء لا يمكن التراجع عنه."

                    else

                        "Voulez-vous vraiment supprimer $statusText pour l'élève $studentName (Séance du ${formatDbDateToDisplay(att.date)} - ${att.timeSlot}) ?\n\nCette action est irréversible.",

                    style = MaterialTheme.typography.bodyMedium

                )

            },

            confirmButton = {

                Button(

                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),

                    onClick = {

                        viewModel.deleteAttendance(att)

                        attendanceToDelete = null

                    }

                ) {

                    Text(if (isAr) "حذف" else "Supprimer")

                }

            },

            dismissButton = {

                TextButton(onClick = { attendanceToDelete = null }) {

                    Text(if (isAr) "إلغاء" else "Annuler")

                }

            }

        )

    }

    // PDF Export Helper with standard Android SAF Document Creation & Direct Share Fallback

    data class PreparedPdfExportPayload(

        val classNameTitle: String,

        val studentsList: List<StudentEntity>,

        val attendancesList: List<AttendanceEntity>,

        val schoolYear: String,

        val filterSubtitle: String,

        val fileName: String

    )

    var pendingPdfExportPayload by remember { mutableStateOf<PreparedPdfExportPayload?>(null) }

    var showPdfExportDialog by remember { mutableStateOf(false) }

    var pdfExportScope by remember { mutableStateOf("ALL") } // "ALL", "CLASS", "STUDENT"

    var pdfExportClassId by remember { mutableStateOf<Int?>(null) }

    var pdfExportStudentId by remember { mutableStateOf<Int?>(null) }

    var pdfExportPeriod by remember { mutableStateOf("ALL") } // "ALL", "DAY", "RANGE", "TERM"

    var pdfExportSingleDate by remember { mutableStateOf(todayDisplayDateStr) }

    var pdfExportStartDate by remember { mutableStateOf(todayDisplayDateStr) }

    var pdfExportEndDate by remember { mutableStateOf(todayDisplayDateStr) }

    var pdfExportTerm by remember { mutableStateOf(1) }

    val attendancePdfLauncher = rememberLauncherForActivityResult(

        contract = ActivityResultContracts.CreateDocument("application/pdf")

    ) { uri: Uri? ->

        uri?.let { targetUri ->

            val payload = pendingPdfExportPayload ?: return@rememberLauncherForActivityResult

            coroutineScope.launch(Dispatchers.IO) {

                viewModel.exportAttendancePdf(

                    context = context,

                    targetUri = targetUri,

                    className = payload.classNameTitle,

                    attendancesList = payload.attendancesList,

                    studentsList = payload.studentsList,

                    schoolYear = payload.schoolYear,

                    filterSubtitle = payload.filterSubtitle

                )

            }

        }

    }

    // Direct View / Share PDF fallback

    fun shareAttendancePdfDirectly(payload: PreparedPdfExportPayload) {

        coroutineScope.launch(Dispatchers.IO) {

            try {

                val safeName = payload.classNameTitle.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")

                val cacheFile = File(context.cacheDir, "Absences_${safeName}.pdf")

                val prof = settings ?: com.example.data.SettingsEntity()

                FileOutputStream(cacheFile).use { fos ->

                    PdfExporter.generateAttendanceReportPdf(

                        context = context,

                        outputStream = fos,

                        teacherName = prof.teacherName.ifBlank { "L'enseignant" },

                        schoolName = prof.schoolName.ifBlank { "Établissement Scolaire" },

                        className = payload.classNameTitle,

                        schoolYear = payload.schoolYear,

                        attendancesList = payload.attendancesList,

                        studentsList = payload.studentsList,

                        taughtSubject = prof.taughtSubject,

                        fontName = prof.activeFont,

                        filterSubtitle = payload.filterSubtitle

                    )

                }

                val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)

                val intent = Intent(Intent.ACTION_VIEW).apply {

                    setDataAndType(contentUri, "application/pdf")

                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                }

                withContext(Dispatchers.Main) {

                    context.startActivity(intent)

                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {

                    Toast.makeText(context, if (isAr) "خطأ في الفتح : ${e.localizedMessage}" else "Erreur d'ouverture : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()

                }

            }

        }

    }

    fun openExportPdfDialog(preselectedClassId: Int?) {

        pdfExportClassId = preselectedClassId

        pdfExportScope = if (preselectedClassId != null) "CLASS" else "ALL"

        pdfExportStudentId = null

        pdfExportPeriod = "ALL"

        pdfExportSingleDate = todayDisplayDateStr

        pdfExportStartDate = todayDisplayDateStr

        pdfExportEndDate = todayDisplayDateStr

        pdfExportTerm = 1

        showPdfExportDialog = true

    }

    fun exportAttendancePdf(classIdToExport: Int?) {

        openExportPdfDialog(classIdToExport)

    }

    // DIALOGUE COMPLET DE SÉLECTION POUR L'EXPORT PDF (Classe, Toutes les classes, Élève, Période, Jour, Tout)

    if (showPdfExportDialog) {

        val allStudentsList = remember(studentMap) {

            studentMap.values.toList().sortedWith(

                compareBy<StudentEntity> { it.orderIndex }

                    .thenBy { it.lastName.lowercase(Locale.getDefault()) }

                    .thenBy { it.firstName.lowercase(Locale.getDefault()) }

            )

        }

        var dialogScope by remember { mutableStateOf(pdfExportScope) }

        var dialogClassId by remember { mutableStateOf(pdfExportClassId ?: classes.firstOrNull()?.id) }

        var dialogStudentId by remember { mutableStateOf(pdfExportStudentId ?: allStudentsList.firstOrNull()?.id) }

        var dialogStudentFilterClassId by remember { mutableStateOf(pdfExportClassId ?: classes.firstOrNull()?.id) }

        var dialogPeriod by remember { mutableStateOf(pdfExportPeriod) }

        var dialogSingleDate by remember { mutableStateOf(pdfExportSingleDate) }

        var dialogStartDate by remember { mutableStateOf(pdfExportStartDate) }

        var dialogEndDate by remember { mutableStateOf(pdfExportEndDate) }

        // État pour le sous-sélecteur de date Jetpack Compose Material 3

        var exportDatePickerTarget by remember { mutableStateOf<String?>(null) } // "SINGLE", "START", "END", or null

        var exportDatePickerInitial by remember { mutableStateOf("") }

        if (exportDatePickerTarget != null) {

            val initialMillis = remember(exportDatePickerInitial) {

                try {

                    val parts = exportDatePickerInitial.split("-", "/")

                    if (parts.size == 3) {

                        val d = parts[0].toIntOrNull()

                        val m = parts[1].toIntOrNull()

                        val y = parts[2].toIntOrNull()

                        if (d != null && m != null && y != null && y in 1900..2100) {

                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

                            cal.set(y, m - 1, d, 12, 0, 0)

                            cal.timeInMillis

                        } else null

                    } else null

                } catch (e: Exception) {

                    null

                } ?: System.currentTimeMillis()

            }

            val exportDatePickerState = rememberDatePickerState(

                initialSelectedDateMillis = initialMillis

            )

            DatePickerDialog(

                onDismissRequest = { exportDatePickerTarget = null },

                confirmButton = {

                    TextButton(

                        onClick = {

                            val selectedMillis = exportDatePickerState.selectedDateMillis

                            if (selectedMillis != null) {

                                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

                                cal.timeInMillis = selectedMillis

                                val d = cal.get(Calendar.DAY_OF_MONTH)

                                val m = cal.get(Calendar.MONTH) + 1

                                val y = cal.get(Calendar.YEAR)

                                val formattedDate = String.format(Locale.US, "%02d-%02d-%04d", d, m, y)

                                when (exportDatePickerTarget) {

                                    "SINGLE" -> dialogSingleDate = formattedDate

                                    "START" -> dialogStartDate = formattedDate

                                    "END" -> dialogEndDate = formattedDate

                                }

                            }

                            exportDatePickerTarget = null

                        }

                    ) {

                        Text(if (isAr) "موافق" else "OK", fontWeight = FontWeight.Bold)

                    }

                },

                dismissButton = {

                    TextButton(onClick = { exportDatePickerTarget = null }) {

                        Text(if (isAr) "إلغاء" else "Annuler")

                    }

                }

            ) {

                DatePicker(state = exportDatePickerState)

            }

        }

        // Calcul des élèves ciblés

        val targetStudents = remember(dialogScope, dialogClassId, dialogStudentId, allStudentsList, studentMap) {

            when (dialogScope) {

                "ALL" -> allStudentsList

                "CLASS" -> {

                    val cid = dialogClassId ?: classes.firstOrNull()?.id

                    if (cid != null) {

                        studentMap.values.filter { it.classId == cid }.sortedWith(

                            compareBy<StudentEntity> { it.orderIndex }

                                .thenBy { it.lastName.lowercase(Locale.getDefault()) }

                                .thenBy { it.firstName.lowercase(Locale.getDefault()) }

                        )

                    } else allStudentsList

                }

                "STUDENT" -> {

                    val sid = dialogStudentId ?: allStudentsList.firstOrNull()?.id

                    val st = sid?.let { id -> allStudentsList.find { it.id == id } ?: studentMap[id] }

                    if (st != null) listOf(st) else emptyList()

                }

                else -> allStudentsList

            }

        }

        // Calcul des enregistrements d'assiduité filtrés (uniquement Absences, Retards et Exclusions)

        val filteredAttendances = remember(dialogScope, dialogClassId, dialogStudentId, dialogPeriod, dialogSingleDate, dialogStartDate, dialogEndDate, allAttendances) {

            var res = allAttendances.filter { it.status in listOf("ABSENT", "RETARD", "EXCLUDED") }

            when (dialogScope) {

                "CLASS" -> {

                    val cid = dialogClassId ?: classes.firstOrNull()?.id

                    if (cid != null) {

                        res = res.filter { it.classId == cid }

                    }

                }

                "STUDENT" -> {

                    val sid = dialogStudentId ?: allStudentsList.firstOrNull()?.id

                    if (sid != null) {

                        res = res.filter { it.studentId == sid }

                    }

                }

                else -> {}

            }

            when (dialogPeriod) {

                "DAY" -> {

                    val dayDb = formatDisplayDateToDb(dialogSingleDate)

                    res = res.filter { it.date == dayDb }

                }

                "RANGE" -> {

                    val startDb = formatDisplayDateToDb(dialogStartDate)

                    val endDb = formatDisplayDateToDb(dialogEndDate)

                    res = res.filter { it.date in startDb..endDb }

                }

                else -> {}

            }

            res

        }

        val classNameTitle = remember(dialogScope, dialogClassId, dialogStudentId, classes, isAr) {

            when (dialogScope) {

                "CLASS" -> classes.find { it.id == dialogClassId }?.name ?: (if (isAr) "الفوج" else "Classe")

                "STUDENT" -> {

                    val st = dialogStudentId?.let { id -> allStudentsList.find { it.id == id } ?: studentMap[id] }

                    if (st != null) "${st.lastName} ${st.firstName}" else (if (isAr) "التلميذ" else "Élève")

                }

                else -> if (isAr) "جميع الأفواج" else "Toutes les classes"

            }

        }

        val periodSubtitle = remember(dialogPeriod, dialogSingleDate, dialogStartDate, dialogEndDate, isAr) {

            when (dialogPeriod) {

                "DAY" -> if (isAr) "يوم $dialogSingleDate" else "Journée du $dialogSingleDate"

                "RANGE" -> if (isAr) "الفترة من $dialogStartDate إلى $dialogEndDate" else "Période du $dialogStartDate au $dialogEndDate"

                else -> if (isAr) "كامل السجل المدرج" else "Toutes périodes confondues"

            }

        }

        fun createPayload(): PreparedPdfExportPayload {

            val safeName = classNameTitle.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")

            val incidentStudentIds = filteredAttendances.map { it.studentId }.toSet()

            val incidentStudents = targetStudents.filter { incidentStudentIds.contains(it.id) }

            return PreparedPdfExportPayload(

                classNameTitle = classNameTitle,

                studentsList = incidentStudents,

                attendancesList = filteredAttendances,

                schoolYear = settings?.currentSchoolYear?.ifBlank { "2026/2027" } ?: "2026/2027",

                filterSubtitle = periodSubtitle,

                fileName = "Absences_${safeName}_${System.currentTimeMillis()}.pdf"

            )

        }

        AlertDialog(

            onDismissRequest = { showPdfExportDialog = false },

            title = {

                Row(

                    verticalAlignment = Alignment.CenterVertically,

                    horizontalArrangement = Arrangement.spacedBy(8.dp)

                ) {

                    Icon(

                        imageVector = Icons.Default.PictureAsPdf,

                        contentDescription = null,

                        tint = MaterialTheme.colorScheme.primary,

                        modifier = Modifier.size(24.dp)

                    )

                    Text(

                        text = if (isAr) "خيارات تصدير PDF" else "Options d'exportation PDF",

                        style = MaterialTheme.typography.titleMedium,

                        fontWeight = FontWeight.Bold

                    )

                }

            },

            text = {

                Column(

                    modifier = Modifier

                        .fillMaxWidth()

                        .verticalScroll(rememberScrollState()),

                    verticalArrangement = Arrangement.spacedBy(14.dp)

                ) {

                    // SECTION 1 : CIBLE (TOUTES LES CLASSES, UNE CLASSE, UN ÉLÈVE)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

                        Text(

                            text = if (isAr) "1. الهدف المراد تصديره :" else "1. Cible à exporter :",

                            fontSize = 12.sp,

                            fontWeight = FontWeight.Bold,

                            color = MaterialTheme.colorScheme.primary

                        )

                        Row(

                            modifier = Modifier

                                .fillMaxWidth()

                                .horizontalScroll(rememberScrollState()),

                            horizontalArrangement = Arrangement.spacedBy(6.dp)

                        ) {

                            FilterChip(

                                selected = dialogScope == "ALL",

                                onClick = { dialogScope = "ALL" },

                                label = { Text(if (isAr) "كل الأقسام" else "Toutes les classes", fontSize = 11.sp, fontWeight = if (dialogScope == "ALL") FontWeight.Bold else FontWeight.Normal) },

                                leadingIcon = { Icon(Icons.Default.Groups, null, modifier = Modifier.size(14.dp)) }

                            )

                            FilterChip(

                                selected = dialogScope == "CLASS",

                                onClick = { dialogScope = "CLASS" },

                                label = { Text(if (isAr) "قسم محدد" else "Une classe", fontSize = 11.sp, fontWeight = if (dialogScope == "CLASS") FontWeight.Bold else FontWeight.Normal) },

                                leadingIcon = { Icon(Icons.Default.Class, null, modifier = Modifier.size(14.dp)) }

                            )

                            FilterChip(

                                selected = dialogScope == "STUDENT",

                                onClick = { dialogScope = "STUDENT" },

                                label = { Text(if (isAr) "تلميذ محدد" else "Un élève", fontSize = 11.sp, fontWeight = if (dialogScope == "STUDENT") FontWeight.Bold else FontWeight.Normal) },

                                leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(14.dp)) }

                            )

                        }

                        // Choix de la classe si "Une classe"

                        if (dialogScope == "CLASS") {

                            Text(

                                text = if (isAr) "اختر القسم :" else "Choisir la classe :",

                                fontSize = 11.sp,

                                fontWeight = FontWeight.SemiBold,

                                color = MaterialTheme.colorScheme.onSurfaceVariant

                            )

                            Row(

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .horizontalScroll(rememberScrollState()),

                                horizontalArrangement = Arrangement.spacedBy(6.dp)

                            ) {

                                classes.forEach { c ->

                                    val isC = dialogClassId == c.id

                                    FilterChip(

                                        selected = isC,

                                        onClick = { dialogClassId = c.id },

                                        label = { Text(c.name, fontSize = 11.sp, fontWeight = if (isC) FontWeight.Bold else FontWeight.Normal) }

                                    )

                                }

                            }

                        }

                        // Choix de l'élève si "Un élève"

                        if (dialogScope == "STUDENT") {

                            Text(

                                text = if (isAr) "تصفية القائمة حسب القسم :" else "Filtrer la liste par classe :",

                                fontSize = 11.sp,

                                fontWeight = FontWeight.SemiBold,

                                color = MaterialTheme.colorScheme.onSurfaceVariant

                            )

                            Row(

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .horizontalScroll(rememberScrollState()),

                                horizontalArrangement = Arrangement.spacedBy(6.dp)

                            ) {

                                FilterChip(

                                    selected = dialogStudentFilterClassId == null,

                                    onClick = { dialogStudentFilterClassId = null },

                                    label = { Text(if (isAr) "الكل" else "Tous", fontSize = 10.5.sp) }

                                )

                                classes.forEach { c ->

                                    val isC = dialogStudentFilterClassId == c.id

                                    FilterChip(

                                        selected = isC,

                                        onClick = {

                                            dialogStudentFilterClassId = c.id

                                            val firstInClass = allStudentsList.firstOrNull { it.classId == c.id }

                                            if (firstInClass != null) {

                                                dialogStudentId = firstInClass.id

                                            }

                                        },

                                        label = { Text(c.name, fontSize = 10.5.sp) }

                                    )

                                }

                            }

                            val studentsForPicker = remember(dialogStudentFilterClassId, allStudentsList) {

                                if (dialogStudentFilterClassId != null) {

                                    allStudentsList.filter { it.classId == dialogStudentFilterClassId }

                                } else {

                                    allStudentsList

                                }

                            }

                            var studentMenuExpanded by remember { mutableStateOf(false) }

                            val currStudent = allStudentsList.find { it.id == dialogStudentId }

                            val studentLabel = if (currStudent != null) "${currStudent.lastName} ${currStudent.firstName}" else (if (isAr) "اختر تلميذاً" else "Sélectionner un élève")

                            Box(modifier = Modifier.fillMaxWidth()) {

                                OutlinedButton(

                                    onClick = { studentMenuExpanded = true },

                                    modifier = Modifier.fillMaxWidth(),

                                    shape = RoundedCornerShape(8.dp)

                                ) {

                                    Row(

                                        modifier = Modifier.fillMaxWidth(),

                                        horizontalArrangement = Arrangement.SpaceBetween,

                                        verticalAlignment = Alignment.CenterVertically

                                    ) {

                                        Text(studentLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)

                                        Icon(Icons.Default.ArrowDropDown, null)

                                    }

                                }

                                DropdownMenu(

                                    expanded = studentMenuExpanded,

                                    onDismissRequest = { studentMenuExpanded = false },

                                    modifier = Modifier.heightIn(max = 250.dp)

                                ) {

                                    studentsForPicker.forEach { st ->

                                        DropdownMenuItem(

                                            text = {

                                                Text(

                                                    text = "${st.lastName} ${st.firstName}",

                                                    fontSize = 12.sp,

                                                    fontWeight = if (dialogStudentId == st.id) FontWeight.Bold else FontWeight.Normal

                                                )

                                            },

                                            onClick = {

                                                dialogStudentId = st.id

                                                studentMenuExpanded = false

                                            }

                                        )

                                    }

                                }

                            }

                        }

                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // SECTION 2 : PÉRIODE (TOUT, UN JOUR, PÉRIODE DE TEMPS)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

                        Text(

                            text = if (isAr) "2. الفترة الزمنية :" else "2. Période temporelle :",

                            fontSize = 12.sp,

                            fontWeight = FontWeight.Bold,

                            color = MaterialTheme.colorScheme.primary

                        )

                        Row(

                            modifier = Modifier

                                .fillMaxWidth()

                                .horizontalScroll(rememberScrollState()),

                            horizontalArrangement = Arrangement.spacedBy(6.dp)

                        ) {

                            FilterChip(

                                selected = dialogPeriod == "ALL",

                                onClick = { dialogPeriod = "ALL" },

                                label = { Text(if (isAr) "كامل السجل" else "Tout l'historique", fontSize = 11.sp, fontWeight = if (dialogPeriod == "ALL") FontWeight.Bold else FontWeight.Normal) },

                                leadingIcon = { Icon(Icons.Default.AllInclusive, null, modifier = Modifier.size(14.dp)) }

                            )

                            FilterChip(

                                selected = dialogPeriod == "DAY",

                                onClick = { dialogPeriod = "DAY" },

                                label = { Text(if (isAr) "يوم واحد" else "Un jour", fontSize = 11.sp, fontWeight = if (dialogPeriod == "DAY") FontWeight.Bold else FontWeight.Normal) },

                                leadingIcon = { Icon(Icons.Default.Today, null, modifier = Modifier.size(14.dp)) }

                            )

                            FilterChip(

                                selected = dialogPeriod == "RANGE",

                                onClick = { dialogPeriod = "RANGE" },

                                label = { Text(if (isAr) "فترة مخصصة" else "Période de temps", fontSize = 11.sp, fontWeight = if (dialogPeriod == "RANGE") FontWeight.Bold else FontWeight.Normal) },

                                leadingIcon = { Icon(Icons.Default.DateRange, null, modifier = Modifier.size(14.dp)) }

                            )

                        }

                        // Date pour "Un jour"

                        if (dialogPeriod == "DAY") {

                            Row(

                                modifier = Modifier.fillMaxWidth(),

                                horizontalArrangement = Arrangement.spacedBy(8.dp),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                OutlinedButton(

                                    onClick = {

                                        exportDatePickerInitial = dialogSingleDate

                                        exportDatePickerTarget = "SINGLE"

                                    },

                                    modifier = Modifier.weight(1f),

                                    shape = RoundedCornerShape(8.dp)

                                ) {

                                    Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp))

                                    Spacer(Modifier.width(6.dp))

                                    Text(dialogSingleDate, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                                }

                                TextButton(

                                    onClick = { dialogSingleDate = todayDisplayDateStr }

                                ) {

                                    Text(if (isAr) "اليوم" else "Aujourd'hui", fontSize = 11.sp)

                                }

                            }

                        }

                        // Période pour "Période de temps"

                        if (dialogPeriod == "RANGE") {

                            Row(

                                modifier = Modifier.fillMaxWidth(),

                                horizontalArrangement = Arrangement.spacedBy(8.dp),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {

                                    Text(if (isAr) "تاريخ البداية :" else "Date de début :", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    OutlinedButton(

                                        onClick = {

                                            exportDatePickerInitial = dialogStartDate

                                            exportDatePickerTarget = "START"

                                        },

                                        modifier = Modifier.fillMaxWidth(),

                                        shape = RoundedCornerShape(8.dp),

                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)

                                    ) {

                                        Row(verticalAlignment = Alignment.CenterVertically) {

                                            Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(12.dp))

                                            Spacer(Modifier.width(4.dp))

                                            Text(dialogStartDate, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)

                                        }

                                    }

                                }

                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {

                                    Text(if (isAr) "تاريخ النهاية :" else "Date de fin :", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    OutlinedButton(

                                        onClick = {

                                            exportDatePickerInitial = dialogEndDate

                                            exportDatePickerTarget = "END"

                                        },

                                        modifier = Modifier.fillMaxWidth(),

                                        shape = RoundedCornerShape(8.dp),

                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)

                                    ) {

                                        Row(verticalAlignment = Alignment.CenterVertically) {

                                            Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(12.dp))

                                            Spacer(Modifier.width(4.dp))

                                            Text(dialogEndDate, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)

                                        }

                                    }

                                }

                            }

                        }

                    }

                    // SECTION 3 : APERÇU DU RAPPORT

                    Surface(

                        shape = RoundedCornerShape(10.dp),

                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),

                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),

                        modifier = Modifier.fillMaxWidth()

                    ) {

                        Column(

                            modifier = Modifier.padding(10.dp),

                            verticalArrangement = Arrangement.spacedBy(4.dp)

                        ) {

                            Text(

                                text = if (isAr) "معاينة الوثيقة :" else "Aperçu du document :",

                                fontSize = 11.sp,

                                fontWeight = FontWeight.Bold,

                                color = MaterialTheme.colorScheme.primary

                            )

                            Text(

                                text = "${if (isAr) "• الهدف :" else "• Cible :"} $classNameTitle",

                                fontSize = 11.5.sp,

                                fontWeight = FontWeight.SemiBold

                            )

                            Text(

                                text = "${if (isAr) "• الفترة :" else "• Période :"} $periodSubtitle",

                                fontSize = 11.5.sp,

                                fontWeight = FontWeight.SemiBold

                            )

                            Text(

                                text = if (isAr) "• التلاميذ المعنيون : ${targetStudents.size}" else "• Élèves concernés : ${targetStudents.size}",

                                fontSize = 11.5.sp,

                                color = MaterialTheme.colorScheme.onSurfaceVariant

                            )

                            Text(

                                text = if (isAr) "• عدد تسجيلات المواظبة : ${filteredAttendances.size}" else "• Lignes d'assiduité trouvées : ${filteredAttendances.size}",

                                fontSize = 11.5.sp,

                                color = MaterialTheme.colorScheme.onSurfaceVariant

                            )

                        }

                    }

                }

            },

            confirmButton = {

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {

                    OutlinedButton(

                        onClick = {

                            val payload = createPayload()

                            showPdfExportDialog = false

                            shareAttendancePdfDirectly(payload)

                        },

                        shape = RoundedCornerShape(8.dp),

                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)

                    ) {

                        Icon(Icons.Default.Share, null, modifier = Modifier.size(14.dp))

                        Spacer(Modifier.width(4.dp))

                        Text(if (isAr) "فتح / مشاركة" else "Ouvrir / Partager", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)

                    }

                    Button(

                        onClick = {

                            val payload = createPayload()

                            showPdfExportDialog = false

                            pendingPdfExportPayload = payload

                            attendancePdfLauncher.launch(payload.fileName)

                        },

                        shape = RoundedCornerShape(8.dp),

                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)

                    ) {

                        Icon(Icons.Default.Download, null, modifier = Modifier.size(14.dp))

                        Spacer(Modifier.width(4.dp))

                        Text(if (isAr) "حفظ PDF" else "Enregistrer", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)

                    }

                }

            },

            dismissButton = {

                TextButton(onClick = { showPdfExportDialog = false }) {

                    Text(if (isAr) "إلغاء" else "Annuler")

                }

            }

        )

    }

    // Sélecteur de classes structuré : "Toutes les classes" en haut (si activé), cases des classes sur la MÊME LIGNE en dessous

    // Contraste élevé pour les contours des cases (ardoise sombre #334155 / claire #CBD5E1, 1.8dp pour non sélectionnées, 2.2dp pour sélectionnée)

    // Seule la case sélectionnée est colorée

    @Composable

    fun StackedClassSelector(

        selectedId: Int?,

        onSelect: (Int?) -> Unit,

        modifier: Modifier = Modifier,

        classList: List<ClassEntity> = classes,

        showAllClassesOption: Boolean = true

    ) {

        val isAllSelected = selectedId == null

        val unselectedBorderColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)

        val unselectedTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

        Column(

            modifier = modifier.fillMaxWidth(),

            verticalArrangement = Arrangement.spacedBy(6.dp)

        ) {

            if (showAllClassesOption) {

                // Ligne 1 : "Toutes les classes"

                Row(

                    modifier = Modifier.fillMaxWidth(),

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    val allBg = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface

                    val allTextColor = if (isAllSelected) Color.White else unselectedTextColor

                    val allBorder = if (isAllSelected) MaterialTheme.colorScheme.primary else unselectedBorderColor

                    Surface(

                        shape = RoundedCornerShape(8.dp),

                        color = allBg,

                        border = BorderStroke(if (isAllSelected) 2.2.dp else 1.8.dp, allBorder),

                        modifier = Modifier.clickable { onSelect(null) }

                    ) {

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)

                        ) {

                            Icon(

                                imageVector = Icons.Default.Groups,

                                contentDescription = null,

                                tint = allTextColor,

                                modifier = Modifier.size(15.dp)

                            )

                            Spacer(Modifier.width(5.dp))

                            Text(

                                text = if (isAr) "جميع الأفواج" else "Toutes les classes",

                                fontSize = 11.5.sp,

                                fontWeight = if (isAllSelected) FontWeight.Black else FontWeight.Bold,

                                color = allTextColor

                            )

                        }

                    }

                }

            }

            if (classList.isEmpty()) {

                Surface(

                    shape = RoundedCornerShape(8.dp),

                    color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF1F5F9),

                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),

                    modifier = Modifier.fillMaxWidth()

                ) {

                    Row(

                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),

                        verticalAlignment = Alignment.CenterVertically,

                        horizontalArrangement = Arrangement.spacedBy(8.dp)

                    ) {

                        Icon(

                            imageVector = Icons.Default.EventBusy,

                            contentDescription = null,

                            tint = MaterialTheme.colorScheme.primary,

                            modifier = Modifier.size(18.dp)

                        )

                        Text(

                            text = if (isAr) "لا توجد أي حصص مبرمجة في هذا اليوم" else "Aucun cours programmé pour cette journée",

                            fontSize = 12.sp,

                            fontWeight = FontWeight.Medium,

                            color = MaterialTheme.colorScheme.onSurfaceVariant

                        )

                    }

                }

            } else {

                // Ligne 2 : Les cases des classes sur la MÊME LIGNE

                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .horizontalScroll(rememberScrollState()),

                    horizontalArrangement = Arrangement.spacedBy(6.dp),

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    classList.forEach { c ->

                        val isSel = selectedId == c.id

                        val palette = com.example.ui.ClassColorHelper.getPaletteForClass(c.id, c.name)

                        val chipBg = if (isSel) palette.primary else MaterialTheme.colorScheme.surface

                        val chipTextColor = if (isSel) Color.White else unselectedTextColor

                        val chipBorder = if (isSel) palette.primary else unselectedBorderColor

                        Surface(

                            shape = RoundedCornerShape(8.dp),

                            color = chipBg,

                            border = BorderStroke(if (isSel) 2.2.dp else 1.8.dp, chipBorder),

                            modifier = Modifier.clickable { onSelect(c.id) }

                        ) {

                            Text(

                                text = c.name,

                                fontSize = 12.sp,

                                fontWeight = if (isSel) FontWeight.Black else FontWeight.Bold,

                                color = chipTextColor,

                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)

                            )

                        }

                    }

                }

            }

        }

    }

    @Composable

    fun SingleLineClassSelector(

        selectedId: Int?,

        onSelect: (Int?) -> Unit,

        modifier: Modifier = Modifier

    ) {

        StackedClassSelector(selectedId, onSelect, modifier)

    }

    @Composable

    fun TwoLineClassSelector(

        selectedId: Int?,

        onSelect: (Int?) -> Unit,

        modifier: Modifier = Modifier

    ) {

        StackedClassSelector(selectedId, onSelect, modifier)

    }

    Scaffold(

        containerColor = Color.Transparent,

        topBar = {

            // SÉPARATEURS DE REGISTRE COLORÉS (COLOR BINDER TABS)

            AdaptiveScreenContainer(maxWidth = 960.dp) {

                Column(

                    modifier = Modifier

                        .fillMaxWidth()

                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))

                        .padding(top = 6.dp, start = 6.dp, end = 6.dp, bottom = 2.dp)

                ) {

                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.spacedBy(6.dp),

                    verticalAlignment = Alignment.Bottom

                ) {

                    // TAB 0 : FAIRE L'APPEL (Vert Émeraude Registre)

                    val tab0Selected = selectedTabIndex == 0

                    Surface(

                        modifier = Modifier

                            .weight(1f)

                            .clickable { selectedTabIndex = 0 }

                            .padding(bottom = if (tab0Selected) 0.dp else 2.dp),

                        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),

                        color = if (tab0Selected) Color(0xFF059669) else Color(0xFFD1FAE5),

                        border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.5f)),

                        shadowElevation = if (tab0Selected) 3.dp else 0.dp

                    ) {

                        Row(

                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.Center

                        ) {

                            Icon(

                                imageVector = Icons.Default.HowToReg,

                                contentDescription = null,

                                tint = if (tab0Selected) Color.White else Color(0xFF047857),

                                modifier = Modifier.size(16.dp)

                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(

                                text = if (isAr) "1. المناداة" else "1. Appel",

                                fontWeight = FontWeight.Bold,

                                fontSize = 12.sp,

                                maxLines = 1,

                                color = if (tab0Selected) Color.White else Color(0xFF065F46)

                            )

                        }

                    }

                    // TAB 1 : BILAN & REGISTRE (Bleu Roi Registre)

                    val tab1Selected = selectedTabIndex == 1

                    Surface(

                        modifier = Modifier

                            .weight(1f)

                            .clickable { selectedTabIndex = 1 }

                            .padding(bottom = if (tab1Selected) 0.dp else 2.dp),

                        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),

                        color = if (tab1Selected) Color(0xFF2563EB) else Color(0xFFDBEAFE),

                        border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.5f)),

                        shadowElevation = if (tab1Selected) 3.dp else 0.dp

                    ) {

                        Row(

                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.Center

                        ) {

                            Icon(

                                imageVector = Icons.Default.Assessment,

                                contentDescription = null,

                                tint = if (tab1Selected) Color.White else Color(0xFF1D4ED8),

                                modifier = Modifier.size(16.dp)

                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(

                                text = if (isAr) "2. السجل" else "2. Registre",

                                fontWeight = FontWeight.Bold,

                                fontSize = 12.sp,

                                maxLines = 1,

                                color = if (tab1Selected) Color.White else Color(0xFF1E40AF)

                            )

                        }

                    }

                    // TAB 2 : STATISTIQUES (Violet Registre)

                    val tab2Selected = selectedTabIndex == 2

                    Surface(

                        modifier = Modifier

                            .weight(1f)

                            .clickable { selectedTabIndex = 2 }

                            .padding(bottom = if (tab2Selected) 0.dp else 2.dp),

                        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),

                        color = if (tab2Selected) Color(0xFF7C3AED) else Color(0xFFF3E8FF),

                        border = BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.5f)),

                        shadowElevation = if (tab2Selected) 3.dp else 0.dp

                    ) {

                        Row(

                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.Center

                        ) {

                            Icon(

                                imageVector = Icons.Default.BarChart,

                                contentDescription = null,

                                tint = if (tab2Selected) Color.White else Color(0xFF6D28D9),

                                modifier = Modifier.size(16.dp)

                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(

                                text = if (isAr) "3. الإحصائيات" else "3. Stats",

                                fontWeight = FontWeight.Bold,

                                fontSize = 12.sp,

                                maxLines = 1,

                                color = if (tab2Selected) Color.White else Color(0xFF5B21B6)

                            )

                        }

                    }

                }

                HorizontalDivider(

                    color = when (selectedTabIndex) {

                        0 -> Color(0xFF059669)

                        1 -> Color(0xFF2563EB)

                        else -> Color(0xFF7C3AED)

                    },

                    thickness = 2.5.dp

                )

            }

            }

        }

    ) { innerPadding ->

        AdaptiveScreenContainer(maxWidth = 960.dp) {

            Box(

                modifier = Modifier

                    .fillMaxSize()

                    .padding(innerPadding)

            ) {

            val filteredClassStudents = remember(currentClassStudents, studentSearchQuery) {

                if (studentSearchQuery.isBlank()) {

                    currentClassStudents

                } else {

                    val q = studentSearchQuery.trim().lowercase(Locale.getDefault())

                    currentClassStudents.filter { s ->

                        s.lastName.lowercase(Locale.getDefault()).contains(q) ||

                        s.firstName.lowercase(Locale.getDefault()).contains(q) ||

                        "${s.lastName} ${s.firstName}".lowercase(Locale.getDefault()).contains(q)

                    }

                }

            }

            if (selectedTabIndex == 0) {

                val currentMonthPrefix = remember(dbDateStr) {

                    if (dbDateStr.length >= 7) dbDateStr.substring(0, 7) else ""

                }

                val monthlyAbsenceCounts = remember(allAttendances, currentMonthPrefix) {

                    if (currentMonthPrefix.isBlank()) emptyMap<Int, Int>()

                    else {

                        allAttendances

                            .filter { it.date.startsWith(currentMonthPrefix) && it.status == "ABSENT" }

                            .groupBy { it.studentId }

                            .mapValues { it.value.size }

                    }

                }

                // TAB 0: FAIRE L'APPEL

                LazyColumn(

                    modifier = Modifier.fillMaxSize(),

                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),

                    verticalArrangement = Arrangement.spacedBy(10.dp)

                ) {

                    // Item 1: Header selection card

                    item {

                        GlassmorphicCard(

                            modifier = Modifier.fillMaxWidth(),

                            shape = RoundedCornerShape(14.dp),

                            elevation = 2.dp

                        ) {

                            Column(

                                modifier = Modifier.padding(12.dp),

                                verticalArrangement = Arrangement.spacedBy(10.dp)

                            ) {

                                val selectedClass = classes.find { it.id == selectedClassId }

                                Row(

                                    modifier = Modifier.fillMaxWidth(),

                                    horizontalArrangement = Arrangement.spacedBy(8.dp),

                                    verticalAlignment = Alignment.CenterVertically

                                ) {

                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {

                                        Box(

                                            modifier = (if (slotsForSelectedDay.isNotEmpty()) Modifier.weight(1f) else Modifier.fillMaxWidth())

                                                .clip(RoundedCornerShape(4.dp))

                                                .clickable { showCalendarPickerModal = true }

                                        ) {

                                            OutlinedTextField(

                                                value = selectedDisplayDateStr,

                                                onValueChange = {},

                                                readOnly = true,

                                                label = { Text(if (isAr) "التاريخ " else "Date ", fontSize = 11.sp) },

                                                leadingIcon = {

                                                    Icon(

                                                        Icons.Default.CalendarToday,

                                                        contentDescription = if (isAr) "اختيار تاريخ من الرزنامة" else "Sélectionner une date sur le calendrier",

                                                        modifier = Modifier.size(18.dp),

                                                        tint = MaterialTheme.colorScheme.primary

                                                    )

                                                },

                                                modifier = Modifier.fillMaxWidth(),

                                                singleLine = true,

                                                textStyle = LocalTextStyle.current.copy(

                                                    fontSize = 12.sp,

                                                    textDirection = TextDirection.Ltr,

                                                    fontWeight = FontWeight.Bold

                                                )

                                            )

                                            Box(

                                                modifier = Modifier

                                                    .matchParentSize()

                                                    .clickable { showCalendarPickerModal = true }

                                            )

                                        }

                                    }

                                    // CRÉNEAU DROP-DOWN SELECTOR (AFFICHE UNIQUEMENT SI DES COURS SONT PROGRAMMÉS CE JOUR)

                                    if (slotsForSelectedDay.isNotEmpty()) {

                                        var slotDropdownExpanded by remember { mutableStateOf(false) }

                                        Box(modifier = Modifier.weight(1f)) {

                                            OutlinedTextField(

                                                value = selectedTimeSlot,

                                                onValueChange = {},

                                                readOnly = true,

                                                label = { Text(if (isAr) "التوقيت" else "Créneau") },

                                                leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) },

                                                trailingIcon = {

                                                    IconButton(onClick = { slotDropdownExpanded = true }) {

                                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)

                                                    }

                                                },

                                                modifier = Modifier.fillMaxWidth(),

                                                singleLine = true,

                                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)

                                            )

                                            // Clic sur l'ensemble de la case créneau pour déplier le menu

                                            Box(

                                                modifier = Modifier

                                                    .matchParentSize()

                                                    .clickable { slotDropdownExpanded = true }

                                            )

                                            DropdownMenu(

                                                expanded = slotDropdownExpanded,

                                                onDismissRequest = { slotDropdownExpanded = false }

                                            ) {

                                                if (slotsForSelectedClassAndDay.isNotEmpty()) {

                                                    Text(

                                                        text = "${if (isAr) "الحصص المبرمجة للفوج" else "Créneaux de la classe"} (${slotsForSelectedClassAndDay.size}) :",

                                                        fontSize = 11.sp,

                                                        fontWeight = FontWeight.Bold,

                                                        color = MaterialTheme.colorScheme.primary,

                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)

                                                    )

                                                    slotsForSelectedClassAndDay.forEach { slot ->

                                                        val slotText = slot.timeSlot.trim()

                                                        val isCurrent = selectedTimeSlot.trim() == slotText

                                                        DropdownMenuItem(

                                                            text = {

                                                                Row(

                                                                    modifier = Modifier.fillMaxWidth(),

                                                                    horizontalArrangement = Arrangement.SpaceBetween,

                                                                    verticalAlignment = Alignment.CenterVertically

                                                                ) {

                                                                    Text(

                                                                        text = slotText,

                                                                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,

                                                                        fontSize = 12.5.sp,

                                                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

                                                                    )

                                                                    if (slot.subject.isNotBlank()) {

                                                                        Text(

                                                                            text = slot.subject,

                                                                            fontSize = 10.sp,

                                                                            color = MaterialTheme.colorScheme.onSurfaceVariant

                                                                        )

                                                                    }

                                                                }

                                                            },

                                                            onClick = {

                                                                selectTimeSlot(slotText, slot)

                                                                slotDropdownExpanded = false

                                                            }

                                                        )

                                                    }

                                                } else {

                                                    DropdownMenuItem(

                                                        text = {

                                                            Text(

                                                                text = if (isAr) "لا توجد حصص في جدول التوقيت لهذا اليوم" else "Aucun créneau dans l'emploi du temps pour ce jour",

                                                                fontSize = 11.5.sp,

                                                                color = MaterialTheme.colorScheme.onSurfaceVariant

                                                            )

                                                        },

                                                        onClick = { slotDropdownExpanded = false }

                                                    )

                                                }

                                            }

                                        }

                                    }

                                }

                                // Indicateur clair de la classe concernée par ce créneau

                                val currentClassEntity = classes.find { it.id == selectedClassId }

                                if (currentClassEntity != null && classesForSelectedDay.isNotEmpty()) {

                                    Surface(

                                        shape = RoundedCornerShape(8.dp),

                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),

                                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),

                                        modifier = Modifier.fillMaxWidth()

                                    ) {

                                        Row(

                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),

                                            verticalAlignment = Alignment.CenterVertically,

                                            horizontalArrangement = Arrangement.SpaceBetween

                                        ) {

                                            Row(

                                                verticalAlignment = Alignment.CenterVertically,

                                                horizontalArrangement = Arrangement.spacedBy(6.dp)

                                            ) {

                                                Icon(

                                                    imageVector = Icons.Default.Groups,

                                                    contentDescription = null,

                                                    modifier = Modifier.size(16.dp),

                                                    tint = MaterialTheme.colorScheme.primary

                                                )

                                                Text(

                                                    text = "${if (isAr) "الفوج المعني :" else "Classe concernée :"} ${currentClassEntity.name}",

                                                    fontSize = 12.sp,

                                                    fontWeight = FontWeight.Black,

                                                    color = MaterialTheme.colorScheme.primary

                                                )

                                            }

                                            Text(

                                                text = "${currentClassStudents.size} ${if (isAr) (if (currentClassStudents.size <= 1) "تلميذ" else "تلاميذ") else (if (currentClassStudents.size <= 1) "élève" else "élèves")}",

                                                fontSize = 11.5.sp,

                                                fontWeight = FontWeight.Bold,

                                                color = MaterialTheme.colorScheme.onSurfaceVariant

                                            )

                                        }

                                    }

                                }

                                // Dans "Appel" les cases des classes du jour sans "toutes les classes"

                                // Seule la case de classe sélectionnée est colorée

                                StackedClassSelector(

                                    selectedId = selectedClassId,

                                    onSelect = { selectedClassId = it },

                                    classList = classesForSelectedDay,

                                    showAllClassesOption = false

                                )

                            }

                        }

                    }

                    // Item 2: Action bar (Tout Présent & Fine barre de recherche)

                    if (classesForSelectedDay.isNotEmpty() && currentClassStudents.isNotEmpty()) {

                        item {

                            Column(

                                modifier = Modifier.fillMaxWidth(),

                                verticalArrangement = Arrangement.spacedBy(8.dp)

                            ) {

                                Row(

                                    modifier = Modifier.fillMaxWidth(),

                                    horizontalArrangement = Arrangement.SpaceBetween,

                                    verticalAlignment = Alignment.CenterVertically

                                ) {

                                    Text(

                                        text = "${if (isAr) "قائمة التلاميذ" else "Liste des élèves"} (${currentClassStudents.size})",

                                        fontWeight = FontWeight.Bold,

                                        fontSize = 13.5.sp,

                                        color = MaterialTheme.colorScheme.primary

                                    )

                                    OutlinedButton(

                                        onClick = {

                                            val newMap = mutableMapOf<Int, AttendanceDraft>()

                                            currentClassStudents.forEach { s ->

                                                newMap[s.id] = AttendanceDraft(status = "PRESENT")

                                            }

                                            studentStatusMap = newMap

                                            autoSaveAttendances(newMap)

                                        },

                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),

                                        shape = RoundedCornerShape(8.dp),

                                        modifier = Modifier.height(32.dp)

                                    ) {

                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(15.dp))

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Text(if (isAr) "الكل حاضر" else "Tout présent", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)

                                    }

                                }

                                // Fine barre de recherche pour filtrer les élèves

                                FineSearchBar(

                                    value = studentSearchQuery,

                                    onValueChange = { studentSearchQuery = it },

                                    placeholder = if (isAr) "بحث عن تلميذ..." else "Rechercher un élève...",

                                    modifier = Modifier.fillMaxWidth()

                                )

                            }

                        }

                    }

                    // Item 3: Student List in exact class order

                    if (classesForSelectedDay.isEmpty()) {

                        item {

                            Box(

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .padding(vertical = 36.dp),

                                contentAlignment = Alignment.Center

                            ) {

                                Column(

                                    horizontalAlignment = Alignment.CenterHorizontally,

                                    verticalArrangement = Arrangement.spacedBy(8.dp)

                                ) {

                                    Icon(

                                        Icons.Default.EventBusy,

                                        contentDescription = null,

                                        modifier = Modifier.size(36.dp),

                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)

                                    )

                                    Text(

                                        text = if (isAr) "لا توجد أي حصص مبرمجة في جدول التوقيت لهذا اليوم ($selectedDisplayDateStr)." else "Aucun cours programmé dans l'emploi du temps pour cette journée ($selectedDisplayDateStr).",

                                        color = MaterialTheme.colorScheme.onSurfaceVariant,

                                        style = MaterialTheme.typography.bodyMedium,

                                        fontWeight = FontWeight.SemiBold

                                    )

                                }

                            }

                        }

                    } else if (currentClassStudents.isEmpty()) {

                        item {

                            Box(

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .padding(vertical = 30.dp),

                                contentAlignment = Alignment.Center

                            ) {

                                Text(

                                    text = if (selectedClassId == null) (if (isAr) "اختر الفوج" else "Sélectionnez une classe") else (if (isAr) "لا يوجد تلاميذ في هذا الفوج" else "Aucun élève dans cette classe"),

                                    color = MaterialTheme.colorScheme.onSurfaceVariant,

                                    style = MaterialTheme.typography.bodyMedium

                                )

                            }

                        }

                    } else if (filteredClassStudents.isEmpty()) {

                        item {

                            Box(

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .padding(vertical = 20.dp),

                                contentAlignment = Alignment.Center

                            ) {

                                Text(

                                    text = if (isAr) "لا يوجد أي تلميذ يطابق \"$studentSearchQuery\"" else "Aucun élève ne correspond à \"$studentSearchQuery\"",

                                    color = MaterialTheme.colorScheme.onSurfaceVariant,

                                    style = MaterialTheme.typography.bodySmall

                                )

                            }

                        }

                    } else {

                        itemsIndexed(filteredClassStudents, key = { _, s -> s.id }) { _, student ->

                            val monthlyAbsences = monthlyAbsenceCounts[student.id] ?: 0

                            val hasAbsenceAlert = monthlyAbsences > 3

                            val rollNumber = remember(currentClassStudents, student.id) {

                                currentClassStudents.indexOfFirst { it.id == student.id } + 1

                            }

                            val draft = studentStatusMap[student.id] ?: AttendanceDraft(status = "PRESENT")

                            val cardShadowColor = when (draft.status) {

                                "ABSENT" -> Color(0xFFDC2626).copy(alpha = if (isDark) 0.30f else 0.15f)

                                "RETARD" -> Color(0xFFD97706).copy(alpha = if (isDark) 0.30f else 0.15f)

                                "EXCLUDED" -> Color(0xFF7C3AED).copy(alpha = if (isDark) 0.30f else 0.15f)

                                else -> if (isDark) Color.Black.copy(alpha = 0.30f) else Color(0xFF64748B).copy(alpha = 0.10f)

                            }

                            Card(

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp), ambientColor = cardShadowColor, spotColor = cardShadowColor),

                                shape = RoundedCornerShape(14.dp),

                                colors = CardDefaults.cardColors(

                                    containerColor = when (draft.status) {

                                        "ABSENT" -> if (isDark) Color(0xFF3B1818) else Color(0xFFFEE2E2)

                                        "RETARD" -> if (isDark) Color(0xFF3B2609) else Color(0xFFFEF3C7)

                                        "EXCLUDED" -> if (isDark) Color(0xFF2D1645) else Color(0xFFF3E8FF)

                                        else -> if (isDark) Color(0xFF1E293B) else MaterialTheme.colorScheme.surface

                                    },

                                    contentColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

                                ),

                                border = BorderStroke(

                                    1.2.dp,

                                    when (draft.status) {

                                        "ABSENT" -> Color(0xFFDC2626)

                                        "RETARD" -> Color(0xFFD97706)

                                        "EXCLUDED" -> Color(0xFF7C3AED)

                                        else -> if (isDark) Color(0xFF475569) else Color(0xFF94A3B8)

                                    }

                                )

                            ) {

                                Column(

                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),

                                    verticalArrangement = Arrangement.spacedBy(6.dp)

                                ) {

                                    // 1. Nom complet de l'élève sur toute la largeur (sur une seule ligne)

                                    Row(

                                        modifier = Modifier.fillMaxWidth(),

                                        verticalAlignment = Alignment.CenterVertically,

                                        horizontalArrangement = Arrangement.spacedBy(8.dp)

                                    ) {

                                        Surface(

                                            shape = CircleShape,

                                            color = if (isDark) Color(0xFF334155) else MaterialTheme.colorScheme.primaryContainer,

                                            modifier = Modifier.size(24.dp)

                                        ) {

                                            Box(contentAlignment = Alignment.Center) {

                                                Text(

                                                    text = "$rollNumber",

                                                    fontWeight = FontWeight.Bold,

                                                    fontSize = 11.sp,

                                                    color = if (isDark) Color(0xFFF8FAFC) else MaterialTheme.colorScheme.onPrimaryContainer

                                                )

                                            }

                                        }

                                        Text(

                                            text = "${student.lastName} ${student.firstName}",

                                            fontWeight = FontWeight.Bold,

                                            fontSize = 13.5.sp,

                                            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),

                                            maxLines = 1,

                                            softWrap = false,

                                            overflow = TextOverflow.Ellipsis,

                                            modifier = Modifier.weight(1f)

                                        )

                                        if (hasAbsenceAlert) {

                                            Surface(

                                                shape = RoundedCornerShape(6.dp),

                                                color = Color(0xFFEF4444).copy(alpha = if (isDark) 0.25f else 0.12f),

                                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.55f))

                                            ) {

                                                Row(

                                                    verticalAlignment = Alignment.CenterVertically,

                                                    horizontalArrangement = Arrangement.spacedBy(3.dp),

                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)

                                                ) {

                                                    Icon(

                                                        imageVector = Icons.Default.Warning,

                                                        contentDescription = null,

                                                        tint = Color(0xFFEF4444),

                                                        modifier = Modifier.size(12.dp)

                                                    )

                                                    Text(

                                                        text = if (isAr) "تنبيه: $monthlyAbsences غيابات" else "Alerte: $monthlyAbsences abs.",

                                                        style = MaterialTheme.typography.labelSmall,

                                                        color = Color(0xFFEF4444),

                                                        fontWeight = FontWeight.ExtraBold,

                                                        fontSize = 10.sp

                                                    )

                                                }

                                            }

                                        }

                                    }

                                    // 2. Cases P, A, R, X sous le nom de l'élève

                                    Row(

                                        modifier = Modifier.fillMaxWidth(),

                                        horizontalArrangement = Arrangement.spacedBy(6.dp),

                                        verticalAlignment = Alignment.CenterVertically

                                    ) {

                                        FilterChip(

                                            selected = draft.status == "PRESENT",

                                            onClick = {

                                                val newMap = studentStatusMap.toMutableMap()

                                                newMap[student.id] = AttendanceDraft(status = "PRESENT")

                                                studentStatusMap = newMap

                                                autoSaveAttendances(newMap)

                                            },

                                            label = { Text(if (isAr) "ح" else "P", fontSize = 11.sp, fontWeight = FontWeight.Bold) },

                                            colors = FilterChipDefaults.filterChipColors(

                                                containerColor = if (isDark) Color(0xFF1E293B) else MaterialTheme.colorScheme.surface,

                                                labelColor = if (isDark) Color(0xFFE2E8F0) else MaterialTheme.colorScheme.onSurface,

                                                selectedContainerColor = Color(0xFF10B981),

                                                selectedLabelColor = Color.White

                                            ),

                                            border = FilterChipDefaults.filterChipBorder(

                                                borderColor = if (isDark) Color(0xFF475569) else MaterialTheme.colorScheme.outlineVariant,

                                                selectedBorderColor = Color.Transparent,

                                                enabled = true,

                                                selected = draft.status == "PRESENT"

                                            ),

                                            modifier = Modifier.height(28.dp)

                                        )

                                        FilterChip(

                                            selected = draft.status == "ABSENT",

                                            onClick = {

                                                val newMap = studentStatusMap.toMutableMap()

                                                newMap[student.id] = AttendanceDraft(status = "ABSENT", isJustified = draft.isJustified, reason = draft.reason)

                                                studentStatusMap = newMap

                                                autoSaveAttendances(newMap)

                                            },

                                            label = { Text(if (isAr) "غ" else "A", fontSize = 11.sp, fontWeight = FontWeight.Bold) },

                                            colors = FilterChipDefaults.filterChipColors(

                                                containerColor = if (isDark) Color(0xFF1E293B) else MaterialTheme.colorScheme.surface,

                                                labelColor = if (isDark) Color(0xFFE2E8F0) else MaterialTheme.colorScheme.onSurface,

                                                selectedContainerColor = Color(0xFFDC2626),

                                                selectedLabelColor = Color.White

                                            ),

                                            border = FilterChipDefaults.filterChipBorder(

                                                borderColor = if (isDark) Color(0xFF475569) else MaterialTheme.colorScheme.outlineVariant,

                                                selectedBorderColor = Color.Transparent,

                                                enabled = true,

                                                selected = draft.status == "ABSENT"

                                            ),

                                            modifier = Modifier.height(28.dp)

                                        )

                                        FilterChip(

                                            selected = draft.status == "RETARD",

                                            onClick = {

                                                val newMap = studentStatusMap.toMutableMap()

                                                newMap[student.id] = AttendanceDraft(

                                                    status = "RETARD",

                                                    isJustified = draft.isJustified,

                                                    delayMinutes = if (draft.delayMinutes > 0) draft.delayMinutes else 10,

                                                    reason = draft.reason

                                                )

                                                studentStatusMap = newMap

                                                autoSaveAttendances(newMap)

                                            },

                                            label = { Text(if (isAr) "ت" else "R", fontSize = 11.sp, fontWeight = FontWeight.Bold) },

                                            colors = FilterChipDefaults.filterChipColors(

                                                containerColor = if (isDark) Color(0xFF1E293B) else MaterialTheme.colorScheme.surface,

                                                labelColor = if (isDark) Color(0xFFE2E8F0) else MaterialTheme.colorScheme.onSurface,

                                                selectedContainerColor = Color(0xFFD97706),

                                                selectedLabelColor = Color.White

                                            ),

                                            border = FilterChipDefaults.filterChipBorder(

                                                borderColor = if (isDark) Color(0xFF475569) else MaterialTheme.colorScheme.outlineVariant,

                                                selectedBorderColor = Color.Transparent,

                                                enabled = true,

                                                selected = draft.status == "RETARD"

                                            ),

                                            modifier = Modifier.height(28.dp)

                                        )

                                        FilterChip(

                                            selected = draft.status == "EXCLUDED",

                                            onClick = {

                                                val newMap = studentStatusMap.toMutableMap()

                                                newMap[student.id] = AttendanceDraft(

                                                    status = "EXCLUDED",

                                                    isJustified = draft.isJustified,

                                                    delayMinutes = 0,

                                                    reason = draft.reason

                                                )

                                                studentStatusMap = newMap

                                                autoSaveAttendances(newMap)

                                            },

                                            label = { Text(if (isAr) "ط" else "X", fontSize = 11.sp, fontWeight = FontWeight.Bold) },

                                            colors = FilterChipDefaults.filterChipColors(

                                                containerColor = if (isDark) Color(0xFF1E293B) else MaterialTheme.colorScheme.surface,

                                                labelColor = if (isDark) Color(0xFFE2E8F0) else MaterialTheme.colorScheme.onSurface,

                                                selectedContainerColor = Color(0xFF7C3AED),

                                                selectedLabelColor = Color.White

                                            ),

                                            border = FilterChipDefaults.filterChipBorder(

                                                borderColor = if (isDark) Color(0xFF475569) else MaterialTheme.colorScheme.outlineVariant,

                                                selectedBorderColor = Color.Transparent,

                                                enabled = true,

                                                selected = draft.status == "EXCLUDED"

                                            ),

                                            modifier = Modifier.height(28.dp)

                                        )

                                    }

                                    // Sub-row for Retard duration & Justifié toggle cleanly aligned

                                    if (draft.status == "ABSENT" || draft.status == "RETARD" || draft.status == "EXCLUDED") {

                                        Column(

                                            modifier = Modifier.fillMaxWidth(),

                                            verticalArrangement = Arrangement.spacedBy(4.dp)

                                        ) {

                                            if (draft.status == "RETARD") {

                                                Row(

                                                    verticalAlignment = Alignment.CenterVertically,

                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)

                                                ) {

                                                    Text(if (isAr) "تأخر :" else "Retard :", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A))

                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {

                                                        listOf(5, 10, 15, 20).forEach { mins ->

                                                            AssistChip(

                                                                onClick = {

                                                                    val newMap = studentStatusMap.toMutableMap()

                                                                    newMap[student.id] = AttendanceDraft(status = "RETARD", delayMinutes = mins, isJustified = draft.isJustified, reason = draft.reason)

                                                                    studentStatusMap = newMap

                                                                    autoSaveAttendances(newMap)

                                                                },

                                                                label = { Text("${mins}${if (isAr) "د" else "m"}", fontSize = 10.sp, fontWeight = FontWeight.Bold) },

                                                                colors = AssistChipDefaults.assistChipColors(

                                                                    containerColor = if (draft.delayMinutes == mins) Color(0xFFD97706) else (if (isDark) Color(0xFF334155) else MaterialTheme.colorScheme.surface),

                                                                    labelColor = if (draft.delayMinutes == mins) Color.White else (if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A))

                                                                ),

                                                                border = BorderStroke(0.8.dp, if (draft.delayMinutes == mins) Color.Transparent else (if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1))),

                                                                modifier = Modifier.height(24.dp)

                                                             )

                                                        }

                                                    }

                                                }

                                            }

                                            Row(

                                                modifier = Modifier.fillMaxWidth(),

                                                verticalAlignment = Alignment.CenterVertically,

                                                horizontalArrangement = Arrangement.SpaceBetween

                                            ) {

                                                Text(

                                                    text = if (draft.isJustified) (if (isAr) "مبرر" else "Motif Justifié") else (if (isAr) "غير مبرر" else "Non justifié"),

                                                    fontSize = 11.sp,

                                                    fontWeight = FontWeight.Bold,

                                                    color = if (draft.isJustified) (if (isDark) Color(0xFF34D399) else Color(0xFF059669)) else (if (isDark) Color(0xFFF87171) else Color(0xFFDC2626))

                                                )

                                                Switch(

                                                    checked = draft.isJustified,

                                                    onCheckedChange = { chk ->

                                                        val newMap = studentStatusMap.toMutableMap()

                                                        newMap[student.id] = AttendanceDraft(

                                                            status = draft.status,

                                                            isJustified = chk,

                                                            delayMinutes = draft.delayMinutes,

                                                            reason = draft.reason

                                                        )

                                                        studentStatusMap = newMap

                                                        autoSaveAttendances(newMap)

                                                    },

                                                    modifier = Modifier.height(24.dp)

                                                )

                                            }

                                        }

                                    }

                                }

                            }

                        }

                    }

                }

            } else if (selectedTabIndex == 1) {

                // TAB 1: BILAN & REGISTRE DES ABSENCES

                var filterClassId by remember { mutableStateOf<Int?>(null) } // null = Toutes les classes

                var registreSearchQuery by remember { mutableStateOf("") }

                val filteredAttendances = remember(filterClassId, allAttendances) {

                    if (filterClassId != null) {

                        allAttendances.filter { it.classId == filterClassId }

                    } else allAttendances

                }

                // Group attendances by Date and Session Key (date + "_" + classId + "_" + timeSlot)

                data class SessionGroup(

                    val key: String,

                    val dbDate: String,

                    val classId: Int,

                    val timeSlot: String,

                    val subject: String,

                    val records: List<AttendanceEntity>

                )

                val rawSessionGroups = remember(filteredAttendances) {

                    val nonPresents = filteredAttendances.filter { it.status != "PRESENT" }

                    nonPresents.groupBy { "${it.date}__${it.classId}__${it.timeSlot}" }

                        .map { (key, list) ->

                            val first = list.first()

                            SessionGroup(

                                key = key,

                                dbDate = first.date,

                                classId = first.classId,

                                timeSlot = first.timeSlot,

                                subject = first.subject,

                                records = list

                            )

                        }

                        .sortedWith(

                            compareByDescending<SessionGroup> { it.dbDate }

                                .thenBy { it.timeSlot }

                        )

                }

                // Filtrage selon la barre de recherche fine du Registre

                val sessionGroups = remember(rawSessionGroups, registreSearchQuery, studentMap, classes) {

                    if (registreSearchQuery.isBlank()) {

                        rawSessionGroups

                    } else {

                        val q = registreSearchQuery.trim().lowercase(Locale.getDefault())

                        rawSessionGroups.mapNotNull { group ->

                            val cObj = classes.find { it.id == group.classId }

                            val cName = cObj?.name?.lowercase(Locale.getDefault()) ?: ""

                            val groupMatches = group.subject.lowercase(Locale.getDefault()).contains(q) ||

                                    group.timeSlot.lowercase(Locale.getDefault()).contains(q) ||

                                    formatDbDateToDisplay(group.dbDate).lowercase(Locale.getDefault()).contains(q) ||

                                    cName.contains(q)

                            val matchingRecords = group.records.filter { att ->

                                val s = studentMap[att.studentId]

                                val sName = "${s?.lastName ?: ""} ${s?.firstName ?: ""}".lowercase(Locale.getDefault())

                                groupMatches ||

                                sName.contains(q) ||

                                att.reason.lowercase(Locale.getDefault()).contains(q) ||

                                att.subject.lowercase(Locale.getDefault()).contains(q) ||

                                att.timeSlot.lowercase(Locale.getDefault()).contains(q) ||

                                formatDbDateToDisplay(att.date).lowercase(Locale.getDefault()).contains(q)

                            }

                            if (matchingRecords.isNotEmpty()) {

                                group.copy(records = matchingRecords)

                            } else null

                        }

                    }

                }

                // Expanded sessions tracker

                var expandedSessionKeys by remember { mutableStateOf<Set<String>>(emptySet()) }

                LaunchedEffect(sessionGroups) {

                    if (expandedSessionKeys.isEmpty() && sessionGroups.isNotEmpty()) {

                        expandedSessionKeys = sessionGroups.map { it.key }.toSet()

                    }

                }

                LazyColumn(

                    modifier = Modifier.fillMaxSize(),

                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),

                    verticalArrangement = Arrangement.spacedBy(10.dp)

                ) {

                    // Item 1: Class Filter Chips over Two Lines & Export PDF buttons

                    item {

                        GlassmorphicCard(

                            modifier = Modifier.fillMaxWidth(),

                            shape = RoundedCornerShape(12.dp),

                            elevation = 2.dp

                        ) {

                            Column(

                                modifier = Modifier.padding(10.dp),

                                verticalArrangement = Arrangement.spacedBy(8.dp)

                            ) {

                                Row(

                                    modifier = Modifier.fillMaxWidth(),

                                    horizontalArrangement = Arrangement.SpaceBetween,

                                    verticalAlignment = Alignment.CenterVertically

                                ) {

                                    Text(

                                        text = if (isAr) "تصفية حسب الفوج :" else "Filtrer par classe :",

                                        fontWeight = FontWeight.Bold,

                                        fontSize = 12.sp,

                                        color = MaterialTheme.colorScheme.primary

                                    )

                                    Button(

                                        onClick = { exportAttendancePdf(filterClassId) },

                                        shape = RoundedCornerShape(8.dp),

                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),

                                        modifier = Modifier.height(32.dp)

                                    ) {

                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Text(if (isAr) "تصدير PDF" else "Export PDF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)

                                    }

                                }

                                // Dans "Registre" les cases des classes sur la même ligne (seule la classe sélectionnée colorée)

                                SingleLineClassSelector(

                                    selectedId = filterClassId,

                                    onSelect = { filterClassId = it }

                                )

                                // Fold / Unfold Single merged Toggle action

                                if (sessionGroups.isNotEmpty()) {

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    Row(

                                        modifier = Modifier.fillMaxWidth(),

                                        horizontalArrangement = Arrangement.SpaceBetween,

                                        verticalAlignment = Alignment.CenterVertically

                                    ) {

                                        Text(

                                            text = "${sessionGroups.size} ${if (isAr) "حصة" else "séance(s)"}",

                                            fontSize = 11.5.sp,

                                            fontWeight = FontWeight.SemiBold,

                                            color = MaterialTheme.colorScheme.onSurfaceVariant

                                        )

                                        val areAllSessionsExpanded = sessionGroups.isNotEmpty() && sessionGroups.all { expandedSessionKeys.contains(it.key) }

                                        TextButton(

                                            onClick = {

                                                if (areAllSessionsExpanded) {

                                                    expandedSessionKeys = emptySet()

                                                } else {

                                                    expandedSessionKeys = sessionGroups.map { it.key }.toSet()

                                                }

                                            },

                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),

                                            shape = RoundedCornerShape(6.dp),

                                            modifier = Modifier.height(28.dp)

                                        ) {

                                            Icon(

                                                imageVector = if (areAllSessionsExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,

                                                contentDescription = null,

                                                modifier = Modifier.size(14.dp),

                                                tint = MaterialTheme.colorScheme.primary

                                            )

                                            Spacer(Modifier.width(4.dp))

                                            Text(

                                                text = if (areAllSessionsExpanded) (if (isAr) "طي الكل" else "Tout plier") else (if (isAr) "توسيع الكل" else "Tout déplier"),

                                                fontSize = 11.sp,

                                                fontWeight = FontWeight.Bold,

                                                color = MaterialTheme.colorScheme.primary

                                            )

                                        }

                                    }

                                }

                            }

                        }

                    }

                    // Item 1.5: Fine barre de recherche dans le Registre

                    if (rawSessionGroups.isNotEmpty() || registreSearchQuery.isNotBlank()) {

                        item {

                            FineSearchBar(

                                value = registreSearchQuery,

                                onValueChange = { registreSearchQuery = it },

                                placeholder = if (isAr) "بحث (تلميذ، سبب، تاريخ، مادة...)" else "Rechercher (élève, motif, date, matière...)",

                                modifier = Modifier.fillMaxWidth()

                            )

                        }

                    }

                    // Item 2: Counters Header Card - High Contrast in Light Mode

                    val totalAbsences = filteredAttendances.count { it.status == "ABSENT" }

                    val totalRetards = filteredAttendances.count { it.status == "RETARD" }

                    val totalExclusions = filteredAttendances.count { it.status == "EXCLUDED" }

                    val totalJustified = filteredAttendances.count { it.isJustified }

                    item {

                        GlassmorphicCard(

                            modifier = Modifier.fillMaxWidth(),

                            shape = RoundedCornerShape(12.dp),

                            elevation = 2.dp

                        ) {

                            Row(

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .padding(horizontal = 10.dp, vertical = 10.dp),

                                horizontalArrangement = Arrangement.SpaceAround

                            ) {

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                    Text("$totalAbsences", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFFDC2626))

                                    Text(if (isAr) "الغيابات" else "Absences", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B))

                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                    Text("$totalRetards", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFFD97706))

                                    Text(if (isAr) "التأخرات" else "Retards", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B))

                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                    Text("$totalExclusions", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF7C3AED))

                                    Text(if (isAr) "حالات الطرد" else "Exclusions", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B))

                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                    Text("$totalJustified", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF059669))

                                    Text(if (isAr) "المبررة" else "Justifiées", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B))

                                }

                            }

                        }

                    }

                    // Item 3: Sessions List with exact requested format: Date puis Heure, Classe en dessous

                    if (sessionGroups.isEmpty()) {

                        item {

                            Box(

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .padding(vertical = 30.dp),

                                contentAlignment = Alignment.Center

                            ) {

                                Text(if (isAr) "لا توجد أي حالة مواظبة مسجلة لهذا التحديد." else "Aucun incident d'assiduité enregistré pour cette sélection.", color = MaterialTheme.colorScheme.onSurfaceVariant)

                            }

                        }

                    } else {

                        sessionGroups.forEach { session ->

                            val classObj = classes.find { it.id == session.classId }

                            val isExpanded = expandedSessionKeys.contains(session.key)

                            val sessionAbs = session.records.count { it.status == "ABSENT" }

                            val sessionRet = session.records.count { it.status == "RETARD" }

                            val sessionExc = session.records.count { it.status == "EXCLUDED" }

                            item(key = session.key) {

                                GlassmorphicCard(

                                    modifier = Modifier.fillMaxWidth(),

                                    shape = RoundedCornerShape(12.dp),

                                    elevation = 2.dp

                                ) {

                                    Column(

                                        modifier = Modifier.padding(12.dp),

                                        verticalArrangement = Arrangement.spacedBy(8.dp)

                                    ) {

                                        // Header: Line 1 = Date puis Heure, Line 2 (en dessous) = Classe & Incidents

                                        Column(

                                            modifier = Modifier

                                                .fillMaxWidth()

                                                .clip(RoundedCornerShape(8.dp))

                                                .clickable {

                                                    expandedSessionKeys = if (isExpanded) {

                                                        expandedSessionKeys - session.key

                                                    } else {

                                                        expandedSessionKeys + session.key

                                                    }

                                                }

                                                .padding(vertical = 2.dp),

                                            verticalArrangement = Arrangement.spacedBy(6.dp)

                                        ) {

                                            // ROW 1 : DATE PUIS HEURE

                                            Row(

                                                modifier = Modifier.fillMaxWidth(),

                                                horizontalArrangement = Arrangement.SpaceBetween,

                                                verticalAlignment = Alignment.CenterVertically

                                            ) {

                                                Row(

                                                    verticalAlignment = Alignment.CenterVertically,

                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)

                                                ) {

                                                    Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))

                                                    Text(

                                                        text = formatDbDateToDisplay(session.dbDate),

                                                        fontWeight = FontWeight.ExtraBold,

                                                        fontSize = 13.5.sp,

                                                        color = MaterialTheme.colorScheme.primary

                                                    )

                                                    Text("•", color = Color.Gray, fontSize = 12.sp)

                                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(15.dp))

                                                    Text(

                                                        text = session.timeSlot,

                                                        fontWeight = FontWeight.Bold,

                                                        fontSize = 12.5.sp,

                                                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)

                                                    )

                                                }

                                                Icon(

                                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,

                                                    contentDescription = if (isExpanded) "Plier" else "Déplier",

                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,

                                                    modifier = Modifier.size(22.dp)

                                                )

                                            }

                                            // ROW 2 (EN DESSOUS) : CLASSE + BADGES INCIDENTS

                                            Row(

                                                modifier = Modifier.fillMaxWidth(),

                                                horizontalArrangement = Arrangement.SpaceBetween,

                                                verticalAlignment = Alignment.CenterVertically

                                            ) {

                                                Surface(

                                                    shape = RoundedCornerShape(6.dp),

                                                    color = MaterialTheme.colorScheme.primaryContainer

                                                ) {

                                                    Text(

                                                        text = "${if (isAr) "الفوج :" else "Classe :"} ${classObj?.name ?: (if (isAr) "فوج #${session.classId}" else "Classe #${session.classId}")}",

                                                        fontSize = 11.5.sp,

                                                        fontWeight = FontWeight.ExtraBold,

                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,

                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)

                                                    )

                                                }

                                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {

                                                    if (sessionAbs > 0) {

                                                        Surface(shape = RoundedCornerShape(4.dp), color = if (isDark) Color(0xFF3F1919) else Color(0xFFFEE2E2)) {

                                                            Text("$sessionAbs ${if (isAr) "غ" else "Abs"}", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))

                                                        }

                                                    }

                                                    if (sessionRet > 0) {

                                                        Surface(shape = RoundedCornerShape(4.dp), color = if (isDark) Color(0xFF3D270B) else Color(0xFFFEF3C7)) {

                                                            Text("$sessionRet ${if (isAr) "ت" else "Ret"}", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))

                                                        }

                                                    }

                                                    if (sessionExc > 0) {

                                                        Surface(shape = RoundedCornerShape(4.dp), color = if (isDark) Color(0xFF2E1A47) else Color(0xFFF3E8FF)) {

                                                            Text("$sessionExc ${if (isAr) "ط" else "Exc"}", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))

                                                        }

                                                    }

                                                }

                                            }

                                        }

                                        // Detailed Records when Unfolded (Déplié)

                                        if (isExpanded) {

                                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

                                                session.records.forEach { att ->

                                                    val studentObj = remember(att.studentId, studentMap) { studentMap[att.studentId] }

                                                    Surface(

                                                        shape = RoundedCornerShape(8.dp),

                                                        color = when (att.status) {

                                                            "ABSENT" -> if (isDark) Color(0xFF3B1818) else Color(0xFFFEE2E2)

                                                            "RETARD" -> if (isDark) Color(0xFF3B2609) else Color(0xFFFEF3C7)

                                                            "EXCLUDED" -> if (isDark) Color(0xFF2D1645) else Color(0xFFF3E8FF)

                                                            else -> MaterialTheme.colorScheme.surfaceVariant

                                                        },

                                                        border = BorderStroke(1.dp, if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)),

                                                        modifier = Modifier.fillMaxWidth()

                                                    ) {

                                                        Row(

                                                            modifier = Modifier

                                                                .fillMaxWidth()

                                                                .padding(horizontal = 10.dp, vertical = 8.dp),

                                                            horizontalArrangement = Arrangement.SpaceBetween,

                                                            verticalAlignment = Alignment.CenterVertically

                                                        ) {

                                                            Column(

                                                                modifier = Modifier.weight(1f),

                                                                verticalArrangement = Arrangement.spacedBy(3.dp)

                                                            ) {

                                                                Text(

                                                                    text = "${studentObj?.lastName ?: ""} ${studentObj?.firstName ?: (if (isAr) "تلميذ #" + att.studentId else "Élève #" + att.studentId)}",

                                                                    fontWeight = FontWeight.Bold,

                                                                    fontSize = 13.sp,

                                                                    color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

                                                                )

                                                                Row(

                                                                    verticalAlignment = Alignment.CenterVertically,

                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)

                                                                ) {

                                                                    Text(

                                                                        text = when (att.status) {

                                                                            "ABSENT" -> if (isAr) "غائب" else "Absent"

                                                                            "RETARD" -> "${if (isAr) "تأخر" else "Retard"} (${att.delayMinutes}${if (isAr) "د" else "m"})"

                                                                            "EXCLUDED" -> if (isAr) "مطرود من الحصة" else "Exclus de cours"

                                                                            else -> if (isAr) "حاضر" else "Présent"

                                                                        },

                                                                        fontWeight = FontWeight.Bold,

                                                                        fontSize = 11.5.sp,

                                                                        color = when (att.status) {

                                                                            "ABSENT" -> Color(0xFFDC2626)

                                                                            "RETARD" -> Color(0xFFD97706)

                                                                            "EXCLUDED" -> Color(0xFF7C3AED)

                                                                            else -> Color(0xFF059669)

                                                                        }

                                                                    )

                                                                    Text("•", fontSize = 11.sp, color = Color.Gray)

                                                                    Text(

                                                                        text = if (att.isJustified) (if (isAr) "مبرر" else "Justifié") else (if (isAr) "غير مبرر" else "Non justifié"),

                                                                        fontSize = 11.sp,

                                                                        fontWeight = FontWeight.SemiBold,

                                                                        color = if (att.isJustified) Color(0xFF059669) else Color(0xFFDC2626)

                                                                    )

                                                                    if (att.reason.isNotBlank()) {

                                                                        Text(

                                                                            text = "(${att.reason})",

                                                                            fontSize = 10.5.sp,

                                                                            color = MaterialTheme.colorScheme.onSurfaceVariant

                                                                        )

                                                                    }

                                                                }

                                                            }

                                                            // Actions: MODIFIER & SUPPRIMER avec confirmation

                                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {

                                                                IconButton(

                                                                    onClick = { editingAttendance = att },

                                                                    modifier = Modifier.size(30.dp)

                                                                ) {

                                                                    Icon(

                                                                        imageVector = Icons.Default.Edit,

                                                                        contentDescription = "Modifier",

                                                                        tint = MaterialTheme.colorScheme.primary,

                                                                        modifier = Modifier.size(16.dp)

                                                                    )

                                                                }

                                                                IconButton(

                                                                    onClick = { attendanceToDelete = att },

                                                                    modifier = Modifier.size(30.dp)

                                                                ) {

                                                                    Icon(

                                                                        imageVector = Icons.Default.Delete,

                                                                        contentDescription = "Supprimer",

                                                                        tint = MaterialTheme.colorScheme.error,

                                                                        modifier = Modifier.size(16.dp)

                                                                    )

                                                                }

                                                            }

                                                        }

                                                    }

                                                }

                                            }

                                        }

                                    }

                                }

                            }

                        }

                    }

                }

            } else {

                // TAB 2: STATISTIQUES & INFOGRAPHIES AVEC RICHESSE DE DONNÉES

                var statClassId by remember { mutableStateOf<Int?>(null) } // null = Toutes les classes

                val statAttendances = remember(statClassId, allAttendances) {

                    if (statClassId != null) {

                        allAttendances.filter { it.classId == statClassId }

                    } else allAttendances

                }

                val targetStudents = remember(statClassId, classes, studentMap) {

                    if (statClassId != null) {

                        studentMap.values.filter { it.classId == statClassId }

                    } else studentMap.values.toList()

                }

                val totalAbs = statAttendances.count { it.status == "ABSENT" }

                val totalRet = statAttendances.count { it.status == "RETARD" }

                val totalExc = statAttendances.count { it.status == "EXCLUDED" }

                val totalJust = statAttendances.count { it.isJustified }

                val totalCas = totalAbs + totalRet + totalExc

                val totalDelayMins = statAttendances.filter { it.status == "RETARD" }.sumOf { it.delayMinutes }

                // Unique sessions recorded

                val uniqueSessionsCount = statAttendances.map { "${it.date}_${it.classId}_${it.timeSlot}" }.distinct().size

                // Compute Presence Rate %

                val presenceRatePercent = if (targetStudents.isNotEmpty() && statAttendances.isNotEmpty()) {

                    val totalExpected = targetStudents.size * (uniqueSessionsCount.coerceAtLeast(1))

                    val currentNonPresent = totalAbs + totalExc

                    val rate = ((totalExpected - currentNonPresent).toFloat() / totalExpected) * 100f

                    rate.coerceIn(70f, 100f)

                } else 100f

                val justRatePercent = if (totalCas > 0) {

                    ((totalJust.toFloat() / totalCas) * 100f).coerceIn(0f, 100f)

                } else 100f

                val studentRanking = remember(targetStudents, statAttendances) {

                    targetStudents.map { student ->

                        val sAtts = statAttendances.filter { it.studentId == student.id }

                        val abs = sAtts.count { it.status == "ABSENT" }

                        val ret = sAtts.count { it.status == "RETARD" }

                        val exc = sAtts.count { it.status == "EXCLUDED" }

                        val just = sAtts.count { it.isJustified }

                        val dMins = sAtts.filter { it.status == "RETARD" }.sumOf { it.delayMinutes }

                        val total = abs + ret + exc

                        StudentStatSummary(student, total, abs, ret, exc, dMins, just)

                    }.sortedByDescending { it.totalCas }

                }

                LazyColumn(

                    modifier = Modifier.fillMaxSize(),

                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),

                    verticalArrangement = Arrangement.spacedBy(12.dp)

                ) {

                    // Item 1: Filter Chips over Two Lines & PDF Export Buttons

                    item {

                        GlassmorphicCard(

                            modifier = Modifier.fillMaxWidth(),

                            shape = RoundedCornerShape(12.dp),

                            elevation = 2.dp

                        ) {

                            Column(

                                modifier = Modifier.padding(10.dp),

                                verticalArrangement = Arrangement.spacedBy(8.dp)

                            ) {

                                Row(

                                    modifier = Modifier.fillMaxWidth(),

                                    horizontalArrangement = Arrangement.SpaceBetween,

                                    verticalAlignment = Alignment.CenterVertically

                                ) {

                                    Text(

                                        text = "${if (isAr) "إحصائيات الفوج :" else "Statistiques de la classe :"}",

                                        fontWeight = FontWeight.Bold,

                                        fontSize = 12.sp,

                                        color = MaterialTheme.colorScheme.primary

                                    )

                                    Button(

                                        onClick = { exportAttendancePdf(statClassId) },

                                        shape = RoundedCornerShape(8.dp),

                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),

                                        modifier = Modifier.height(32.dp)

                                    ) {

                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Text(if (isAr) "تصدير PDF" else "Export PDF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)

                                    }

                                }

                                SingleLineClassSelector(

                                    selectedId = statClassId,

                                    onSelect = { statClassId = it }

                                )

                            }

                        }

                    }

                    // Item 2: Infographic 1 - Donut Gauges & Key Stats Cards (Bilan d'assiduité spacieux et lisible)

                    item {

                        GlassmorphicCard(

                            modifier = Modifier.fillMaxWidth(),

                            shape = RoundedCornerShape(14.dp),

                            elevation = 2.dp

                        ) {

                            Column(

                                modifier = Modifier.padding(14.dp),

                                verticalArrangement = Arrangement.spacedBy(12.dp)

                            ) {

                                Text(

                                    text = "${if (isAr) "حصيلة المواظبة والغيابات" else "Bilan d'Assiduité"} — ${if (statClassId == null) (if (isAr) "جميع الأفواج" else "Toutes les classes") else classes.find { it.id == statClassId }?.name ?: ""}",

                                    fontWeight = FontWeight.Bold,

                                    style = MaterialTheme.typography.titleSmall,

                                    color = MaterialTheme.colorScheme.primary

                                )

                                Row(

                                    modifier = Modifier.fillMaxWidth(),

                                    verticalAlignment = Alignment.CenterVertically,

                                    horizontalArrangement = Arrangement.spacedBy(16.dp)

                                ) {

                                    // Circular Donut Gauge for Presence Rate

                                    Box(

                                        modifier = Modifier.size(90.dp),

                                        contentAlignment = Alignment.Center

                                    ) {

                                        Canvas(modifier = Modifier.fillMaxSize()) {

                                            val strokeWidth = 10.dp.toPx()

                                            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

                                            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                                            // Track

                                            drawArc(

                                                color = Color(0xFFE2E8F0),

                                                startAngle = 135f,

                                                sweepAngle = 270f,

                                                useCenter = false,

                                                topLeft = topLeft,

                                                size = arcSize,

                                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)

                                            )

                                            // Progress

                                            val sweep = (presenceRatePercent / 100f) * 270f

                                            drawArc(

                                                color = Color(0xFF059669),

                                                startAngle = 135f,

                                                sweepAngle = sweep,

                                                useCenter = false,

                                                topLeft = topLeft,

                                                size = arcSize,

                                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)

                                            )

                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                            Text(

                                                text = "${presenceRatePercent.toInt()}%",

                                                fontWeight = FontWeight.ExtraBold,

                                                fontSize = 16.sp,

                                                color = Color(0xFF059669)

                                            )

                                            Text(if (isAr) "الحضور" else "Présence", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))

                                        }

                                    }

                                    // Stat breakdown numbers on TWO LINES

                                    Column(

                                        modifier = Modifier.weight(1f),

                                        verticalArrangement = Arrangement.spacedBy(8.dp)

                                    ) {

                                        Row(

                                            modifier = Modifier.fillMaxWidth(),

                                            horizontalArrangement = Arrangement.SpaceAround

                                        ) {

                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                                Text("$totalAbs", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color(0xFFDC2626))

                                                Text(if (isAr) "الغيابات" else "Absences", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))

                                            }

                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                                Text("$totalRet", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color(0xFFD97706))

                                                Text(if (isAr) "التأخرات" else "Retards", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))

                                            }

                                        }

                                        Row(

                                            modifier = Modifier.fillMaxWidth(),

                                            horizontalArrangement = Arrangement.SpaceAround

                                        ) {

                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                                Text("$totalExc", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color(0xFF7C3AED))

                                                Text(if (isAr) "حالات الطرد" else "Exclusions", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))

                                            }

                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                                Text("$totalJust", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color(0xFF059669))

                                                Text(if (isAr) "المبررة" else "Justifiées", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))

                                            }

                                        }

                                    }

                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                                // Detailed Additional Indicators (Mise en page aérée et propre en 3 blocs)

                                Row(

                                    modifier = Modifier.fillMaxWidth(),

                                    horizontalArrangement = Arrangement.spacedBy(8.dp),

                                    verticalAlignment = Alignment.CenterVertically

                                ) {

                                    Surface(

                                        modifier = Modifier.weight(1f),

                                        shape = RoundedCornerShape(10.dp),

                                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),

                                        border = BorderStroke(0.8.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))

                                    ) {

                                        Column(

                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),

                                            horizontalAlignment = Alignment.CenterHorizontally

                                        ) {

                                            Text(if (isAr) "الحصص" else "Séances", fontSize = 10.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B), maxLines = 1)

                                            Text("$uniqueSessionsCount", fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = MaterialTheme.colorScheme.primary)

                                            Text(if (isAr) "المسجلة" else "contrôlées", fontSize = 8.5.sp, color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8), maxLines = 1)

                                        }

                                    }

                                    Surface(

                                        modifier = Modifier.weight(1f),

                                        shape = RoundedCornerShape(10.dp),

                                        color = if (isDark) Color(0xFF2E2211) else Color(0xFFFEF3C7).copy(alpha = 0.5f),

                                        border = BorderStroke(0.8.dp, Color(0xFFD97706).copy(alpha = 0.4f))

                                    ) {

                                        Column(

                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),

                                            horizontalAlignment = Alignment.CenterHorizontally

                                        ) {

                                            Text(if (isAr) "التأخرات" else "Retards", fontSize = 10.sp, color = if (isDark) Color(0xFFFCD34D) else Color(0xFF92400E), maxLines = 1)

                                            Text("$totalDelayMins min", fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = Color(0xFFD97706))

                                            Text(if (isAr) "المجموع الكلي" else "cumul total", fontSize = 8.5.sp, color = if (isDark) Color(0xFFFCD34D).copy(alpha = 0.7f) else Color(0xFFB45309), maxLines = 1)

                                        }

                                    }

                                    Surface(

                                        modifier = Modifier.weight(1f),

                                        shape = RoundedCornerShape(10.dp),

                                        color = if (isDark) Color(0xFF132E22) else Color(0xFFD1FAE5).copy(alpha = 0.5f),

                                        border = BorderStroke(0.8.dp, Color(0xFF059669).copy(alpha = 0.4f))

                                    ) {

                                        Column(

                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),

                                            horizontalAlignment = Alignment.CenterHorizontally

                                        ) {

                                            Text(if (isAr) "التبرير" else "Justification", fontSize = 10.sp, color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46), maxLines = 1)

                                            Text("${justRatePercent.toInt()}%", fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = Color(0xFF059669))

                                            Text(if (isAr) "نسبة الحالات" else "taux des cas", fontSize = 8.5.sp, color = if (isDark) Color(0xFF6EE7B7).copy(alpha = 0.7f) else Color(0xFF047857), maxLines = 1)

                                        }

                                    }

                                }

                            }

                        }

                    }

                    // Item 3: Student Ranking List with "abs, ret, exc" on the LEFT

                    item {

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.SpaceBetween,

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            Text(

                                text = "${if (isAr) "متابعة التلاميذ اسمياً" else "Suivi nominatif des élèves"} (${studentRanking.size}) :",

                                fontWeight = FontWeight.Bold,

                                style = MaterialTheme.typography.titleSmall,

                                color = if (isDark) Color(0xFFF1F5F9) else MaterialTheme.colorScheme.onSurface

                            )

                        }

                    }

                    if (targetStudents.isEmpty()) {

                        item {

                            Text(if (isAr) "لم يتم العثور على أي تلميذ." else "Aucun élève trouvé.", fontSize = 12.sp, color = if (isDark) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant)

                        }

                    } else {

                        items(studentRanking, key = { it.student.id }) { itemSummary ->

                            val student = itemSummary.student

                            val abs = itemSummary.absCount

                            val ret = itemSummary.retCount

                            val exc = itemSummary.excCount

                            val totalCasStudent = itemSummary.totalCas

                            val studentClass = classes.find { it.id == student.classId }

                            val summaryShadow = if (abs >= 3) Color(0xFFDC2626).copy(alpha = if (isDark) 0.25f else 0.12f) else (if (isDark) Color.Black.copy(alpha = 0.25f) else Color(0xFF64748B).copy(alpha = 0.08f))

                            Card(

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .shadow(elevation = 1.5.dp, shape = RoundedCornerShape(14.dp), ambientColor = summaryShadow, spotColor = summaryShadow),

                                shape = RoundedCornerShape(14.dp),

                                colors = CardDefaults.cardColors(

                                    containerColor = if (abs >= 3) {

                                        if (isDark) Color(0xFF3B1818) else Color(0xFFFEE2E2)

                                    } else {

                                        if (isDark) Color(0xFF1E293B) else MaterialTheme.colorScheme.surface

                                    },

                                    contentColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

                                ),

                                border = BorderStroke(

                                    1.dp,

                                    if (abs >= 3) {

                                        if (isDark) Color(0xFFDC2626) else Color(0xFFEF4444)

                                    } else {

                                        if (isDark) Color(0xFF334155) else MaterialTheme.colorScheme.outlineVariant

                                    }

                                )

                            ) {

                                Row(

                                    modifier = Modifier

                                        .fillMaxWidth()

                                        .padding(10.dp),

                                    verticalAlignment = Alignment.CenterVertically,

                                    horizontalArrangement = Arrangement.spacedBy(10.dp)

                                ) {

                                    // METTRE SUR LA GAUCHE : "ABS, RET, EXC"

                                    Column(

                                        horizontalAlignment = Alignment.CenterHorizontally,

                                        verticalArrangement = Arrangement.spacedBy(3.dp),

                                        modifier = Modifier.width(68.dp)

                                    ) {

                                        // ABS PILL

                                        Surface(

                                            shape = RoundedCornerShape(4.dp),

                                            color = if (abs > 0) Color(0xFFDC2626) else (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),

                                            modifier = Modifier.fillMaxWidth()

                                        ) {

                                            Text(

                                                text = "${if (isAr) "غياب" else "Abs"}: $abs",

                                                fontSize = 10.sp,

                                                fontWeight = FontWeight.ExtraBold,

                                                color = if (abs > 0) Color.White else (if (isDark) Color(0xFFF1F5F9) else Color(0xFF334155)),

                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,

                                                modifier = Modifier.padding(vertical = 1.5.dp)

                                            )

                                        }

                                        // RET PILL

                                        Surface(

                                            shape = RoundedCornerShape(4.dp),

                                            color = if (ret > 0) Color(0xFFD97706) else (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),

                                            modifier = Modifier.fillMaxWidth()

                                        ) {

                                            Text(

                                                text = "${if (isAr) "تأخر" else "Ret"}: $ret",

                                                fontSize = 10.sp,

                                                fontWeight = FontWeight.ExtraBold,

                                                color = if (ret > 0) Color.White else (if (isDark) Color(0xFFF1F5F9) else Color(0xFF334155)),

                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,

                                                modifier = Modifier.padding(vertical = 1.5.dp)

                                            )

                                        }

                                        // EXC PILL

                                        Surface(

                                            shape = RoundedCornerShape(4.dp),

                                            color = if (exc > 0) Color(0xFF7C3AED) else (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),

                                            modifier = Modifier.fillMaxWidth()

                                        ) {

                                            Text(

                                                text = "${if (isAr) "طرد" else "Exc"}: $exc",

                                                fontSize = 10.sp,

                                                fontWeight = FontWeight.ExtraBold,

                                                color = if (exc > 0) Color.White else (if (isDark) Color(0xFFF1F5F9) else Color(0xFF334155)),

                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,

                                                modifier = Modifier.padding(vertical = 1.5.dp)

                                            )

                                        }

                                    }

                                    // RIGHT DETAILS : Nom élève, classe, assiduité, total

                                    Column(

                                        modifier = Modifier.weight(1f),

                                        verticalArrangement = Arrangement.spacedBy(4.dp)

                                    ) {

                                        Row(

                                            modifier = Modifier.fillMaxWidth(),

                                            horizontalArrangement = Arrangement.SpaceBetween,

                                            verticalAlignment = Alignment.CenterVertically

                                        ) {

                                            Text(

                                                text = "${student.lastName} ${student.firstName}",

                                                fontWeight = FontWeight.Bold,

                                                fontSize = 13.5.sp,

                                                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

                                            )

                                            if (studentClass != null) {

                                                Surface(

                                                    shape = RoundedCornerShape(4.dp),

                                                    color = if (isDark) Color(0xFF1E3A5F) else MaterialTheme.colorScheme.primaryContainer

                                                ) {

                                                    Text(

                                                        text = studentClass.name,

                                                        fontSize = 9.5.sp,

                                                        fontWeight = FontWeight.Bold,

                                                        color = if (isDark) Color(0xFF93C5FD) else MaterialTheme.colorScheme.onPrimaryContainer,

                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)

                                                    )

                                                }

                                            }

                                        }

                                        Row(

                                            modifier = Modifier.fillMaxWidth(),

                                            horizontalArrangement = Arrangement.SpaceBetween,

                                            verticalAlignment = Alignment.CenterVertically

                                        ) {

                                            if (abs >= 3) {

                                                Surface(

                                                    shape = RoundedCornerShape(4.dp),

                                                    color = Color(0xFFDC2626)

                                                ) {

                                                    Text(

                                                        text = if (isAr) "تنبيه متابعة (≥3 غ)" else "Alerte Suivi (≥3 abs)",

                                                        fontSize = 9.5.sp,

                                                        fontWeight = FontWeight.Bold,

                                                        color = Color.White,

                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)

                                                    )

                                                }

                                            } else if (totalCasStudent == 0) {

                                                Surface(

                                                    shape = RoundedCornerShape(4.dp),

                                                    color = Color(0xFF059669)

                                                ) {

                                                    Text(

                                                        text = if (isAr) "مواظب 100%" else "100% Assidu",

                                                        fontSize = 9.5.sp,

                                                        fontWeight = FontWeight.Bold,

                                                        color = Color.White,

                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)

                                                    )

                                                }

                                            } else {

                                                Surface(

                                                    shape = RoundedCornerShape(4.dp),

                                                    color = Color(0xFF2563EB)

                                                ) {

                                                    Text(

                                                        text = if (isAr) "منتظم" else "Régulier",

                                                        fontSize = 9.5.sp,

                                                        fontWeight = FontWeight.Bold,

                                                        color = Color.White,

                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)

                                                    )

                                                }

                                            }

                                            if (itemSummary.delayMinutesSum > 0) {

                                                Text(

                                                    text = if (isAr) "${itemSummary.delayMinutesSum} د تأخر" else "${itemSummary.delayMinutesSum} min retard",

                                                    fontSize = 10.sp,

                                                    fontWeight = FontWeight.Bold,

                                                    color = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)

                                                )

                                            }

                                        }

                                    }

                                }

                            }

                        }

                    }

                }

            }

        }

    }

}

}

