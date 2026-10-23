package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.AdaptiveScreenContainer
import com.example.util.adaptiveDialogMaxHeight
import com.example.R
import com.example.data.ClassEntity
import com.example.data.StudentEntity
import com.example.ui.ClassColorHelper
import com.example.ui.ClassBadge
import com.example.ui.ClassColorDot
import com.example.util.GradeCalculator
import com.example.util.Tr
import com.example.util.tr
import com.example.ui.AppViewModel
import com.example.ui.components.FineSearchBar
import com.example.ui.components.GlassmorphicCard
import com.example.ui.theme.OffWhite
import java.util.Calendar
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri

private fun formatRepeatedYearsDisplay(years: String, isAr: Boolean): String {
    if (!isAr || years.isBlank()) return years
    return years
        .replace("1AS", "السنة الأولى")
        .replace("2AS", "السنة الثانية")
        .replace("3AS", "السنة الثالثة")
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val classes by viewModel.allClasses.collectAsState()
    val selectedClassId by viewModel.selectedClassId.collectAsState()
    val students by viewModel.currentStudents.collectAsState()
    val currentSettings by viewModel.settings.collectAsState()
    val grades by viewModel.currentGrades.collectAsState()

    // Dialog state controllers
    var showAddClassDialog by remember { mutableStateOf(false) }
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showDivideGroupsDialog by remember { mutableStateOf(false) }
    var studentToEdit by remember { mutableStateOf<StudentEntity?>(null) }
    var classToEdit by remember { mutableStateOf<ClassEntity?>(null) }
    var activeClassForStudentsList by remember { mutableStateOf<ClassEntity?>(null) }
    var showDeleteClassConfirm by remember { mutableStateOf(false) }
    var studentToDelete by remember { mutableStateOf<StudentEntity?>(null) }
    var showDeleteStudentFromEditConfirm by remember { mutableStateOf(false) }

    val activeClass = classes.find { it.id == selectedClassId }
    val isAr = Tr.isArabic()

    var studentsForClassPdfExport by remember { mutableStateOf<List<StudentEntity>>(emptyList()) }
    var groupSubtitleForClassPdfExport by remember { mutableStateOf("") }

    val currentTargetClass by rememberUpdatedState(activeClassForStudentsList ?: activeClass)
    val currentStudentsList by rememberUpdatedState(students)
    val currentGradesList by rememberUpdatedState(grades)
    val currentExportStudents by rememberUpdatedState(studentsForClassPdfExport)
    val currentExportGroupSubtitle by rememberUpdatedState(groupSubtitleForClassPdfExport)
    val currentSettingsSnapshot by rememberUpdatedState(currentSettings)

    val planPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let {
            val targetCls = currentTargetClass
            if (targetCls != null) {
                viewModel.exportSingleSeatingPlanPdf(
                    context = context,
                    targetUri = it,
                    className = targetCls.name,
                    studentsList = currentStudentsList
                )
            }
        }
    }
    val classListPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let {
            val targetCls = currentTargetClass
            if (targetCls != null) {
                val listToExport = if (currentExportStudents.isNotEmpty()) currentExportStudents else currentStudentsList
                viewModel.exportSingleClassListPdf(
                    context = context,
                    targetUri = it,
                    className = targetCls.name,
                    studentsList = listToExport,
                    groupSubtitle = currentExportGroupSubtitle
                )
            }
        }
    }
    val statsPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let {
            val targetCls = currentTargetClass
            if (targetCls != null) {
                val classCoeff = targetCls.subjectCoefficient ?: currentSettingsSnapshot?.subjectCoefficient ?: 3
                viewModel.exportClassStatisticsPdf(
                    context = context,
                    targetUri = it,
                    className = targetCls.name,
                    studentsList = currentStudentsList,
                    gradesList = currentGradesList,
                    subjectCoefficient = classCoeff,
                    isPrimary = false
                )
            }
        }
    }

    AdaptiveScreenContainer(maxWidth = 900.dp) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
        // --- 1. SELECTION DE LA CLASSE ---
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.class_list),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    androidx.compose.material3.FilledIconButton(
                        onClick = { showAddClassDialog = true },
                        modifier = Modifier.size(36.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                        colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Add,
                            contentDescription = "Ajouter une classe",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (classes.isEmpty()) {
                    Text(
                        text = "Aucune classe créée. Veuillez cliquer sur le bouton '+' pour ajouter votre première classe.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    // Vertical list of classes
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isDarkScreen = MaterialTheme.colorScheme.background != OffWhite
                        classes.forEach { clazz ->
                            val isSelected = clazz.id == selectedClassId
                            val palette = ClassColorHelper.getPaletteForClass(clazz.id, clazz.name)
                            val bg = if (isSelected) palette.primary else (if (isDarkScreen) palette.primary.copy(alpha = 0.22f).compositeOver(MaterialTheme.colorScheme.surface) else palette.container.copy(alpha = 0.65f))
                            val tc = if (isSelected) Color.White else (if (isDarkScreen) palette.primary else palette.onContainer)
                            val borderColor = if (isSelected) palette.primary else (if (isDarkScreen) palette.primary.copy(alpha = 0.7f) else palette.primary.copy(alpha = 0.4f))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bg)
                                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                    .combinedClickable(
                                        onClick = {
                                            viewModel.selectClass(clazz.id)
                                            activeClassForStudentsList = clazz
                                        },
                                        onLongClick = { classToEdit = clazz }
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color.White else palette.primary)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = clazz.name,
                                            color = tc,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(
                                            text = if (Tr.isArabic()) "(اضغط مطولاً للتعديل)" else "(Appui long pour modifier)",
                                            fontSize = 10.sp,
                                            color = tc.copy(alpha = 0.7f)
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

    if (activeClassForStudentsList != null) {
        val activeClass = activeClassForStudentsList!!
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { activeClassForStudentsList = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                var currentSubView by remember(activeClass) { mutableStateOf("liste") }

                // Search, filter, and sorting states for student lists
                var searchQuery by remember { mutableStateOf("") }
                var selectedGenderFilter by remember { mutableStateOf("Tous") } // "Tous", "M", "F"
                var selectedAgeFilter by remember { mutableStateOf("Tous") } // "Tous" or specific age string like "11"
                var selectedAverageFilter by remember { mutableStateOf("Tous") } // "Tous", "Admis", "Non-admis", "Excellent"
                var selectedGradeFilter by remember { mutableStateOf("Tous") } // "Tous", "NoGrades", "MissingDevoir", "MissingFinal"
                var selectedGroupFilter by remember { mutableStateOf("ALL") } // "ALL", "G1", "G2", "G3", "NONE"
                var selectedSortOption by remember { mutableStateOf("Ordre personnalisé (N°)") }
                var showFiltersPanel by remember { mutableStateOf(false) }
                var studentToChangePosition by remember { mutableStateOf<Pair<StudentEntity, Int>?>(null) }
                var showExportClassListPdfDialog by remember { mutableStateOf(false) }

                val presentAges = remember(students) {
                    val computed = students.mapNotNull { calculateAge(it.birthDate) }
                    val extraAges = listOf(18, 19, 20, 21, 22)
                    (computed + extraAges).distinct().sorted()
                }

                // Safe helper to compute the student's weighted average
                val coeff = currentSettings?.subjectCoefficient ?: 3
                val filteredStudents = remember(students, grades, searchQuery, selectedGenderFilter, selectedAgeFilter, selectedAverageFilter, selectedGradeFilter, selectedGroupFilter, selectedSortOption, coeff) {
                    val listWithInfo = students.map { student ->
                        val studentGrade = grades.find { it.studentId == student.id }
                        val average = studentGrade?.let { sg ->
                            GradeCalculator.computeStudentAverage(sg, coeff)
                        }
                        Triple(student, studentGrade, average)
                    }

                    // Apply Search Query
                    var result = if (searchQuery.isBlank()) {
                        listWithInfo
                    } else {
                        val query = searchQuery.lowercase().trim()
                        listWithInfo.filter { (student, _, _) ->
                            student.firstName.lowercase().contains(query) ||
                            student.lastName.lowercase().contains(query) ||
                            student.observations.lowercase().contains(query) ||
                            student.birthPlace.lowercase().contains(query)
                        }
                    }

                    // Apply Gender Filter
                    if (selectedGenderFilter != "Tous") {
                        result = result.filter { (student, _, _) ->
                            student.gender.equals(selectedGenderFilter, ignoreCase = true)
                        }
                    }

                    // Apply Age Filter (only matches exact age if specified)
                    if (selectedAgeFilter != "Tous") {
                        val targetAge = selectedAgeFilter.toIntOrNull()
                        if (targetAge != null) {
                            result = result.filter { (student, _, _) ->
                                calculateAge(student.birthDate) == targetAge
                            }
                        }
                    }

                    // Apply Average Filter
                    if (selectedAverageFilter != "Tous") {
                        result = result.filter { (_, _, average) ->
                            when (selectedAverageFilter) {
                                "Admis" -> average != null && average >= 10f
                                "Non-admis" -> average != null && average < 10f
                                "Excellent" -> average != null && average >= 16f
                                "Sans Moyenne" -> average == null
                                else -> true
                            }
                        }
                    }

                    // Apply Grades Filter
                    if (selectedGradeFilter != "Tous") {
                        result = result.filter { (_, studentGrade, _) ->
                            when (selectedGradeFilter) {
                                "NoGrades" -> studentGrade == null || (
                                    studentGrade.participation == null && studentGrade.discipline == null &&
                                    studentGrade.assiduity == null && studentGrade.notebook == null &&
                                    studentGrade.exam1 == null && studentGrade.exam2 == null &&
                                    studentGrade.exam3 == null && studentGrade.finalExam == null
                                )
                                "MissingDevoir" -> studentGrade == null || studentGrade.exam1 == null || studentGrade.exam2 == null || studentGrade.exam3 == null
                                "MissingFinal" -> studentGrade == null || studentGrade.finalExam == null
                                else -> true
                            }
                        }
                    }

                    // Apply Group Filter
                    if (selectedGroupFilter != "ALL") {
                        result = if (selectedGroupFilter == "NONE") {
                            result.filter { it.first.groupName.isBlank() }
                        } else {
                            result.filter { it.first.groupName == selectedGroupFilter }
                        }
                    }

                    // Apply Sorting
                    val sortedList = when (selectedSortOption) {
                        "Ordre personnalisé (N°)", "Ordre personnalisé (Glissement)" -> result.sortedBy { it.first.orderIndex }
                        "Nom (A-Z)" -> result.sortedBy { it.first.lastName.lowercase() + " " + it.first.firstName.lowercase() }
                        "Nom (Z-A)" -> result.sortedByDescending { it.first.lastName.lowercase() + " " + it.first.firstName.lowercase() }
                        "Moyenne (+)" -> result.sortedWith { a, b ->
                            val avgA = a.third ?: -1f
                            val avgB = b.third ?: -1f
                            avgB.compareTo(avgA) // Descending
                        }
                        "Moyenne (-)" -> result.sortedWith { a, b ->
                            val avgA = a.third ?: 99f
                            val avgB = b.third ?: 99f
                            avgA.compareTo(avgB) // Ascending
                        }
                        "Âge (Le plus jeune)" -> result.sortedByDescending { parseBirthDateToMillis(it.first.birthDate) } // Later date = younger
                        "Âge (Le plus âgé)" -> result.sortedBy { parseBirthDateToMillis(it.first.birthDate) } // Earlier date = older
                        else -> result.sortedBy { it.first.orderIndex }
                    }

                    sortedList
                }

                val classGroups = remember(students) {
                    students.map { it.groupName.trim() }.filter { it.isNotBlank() }.distinct().sorted()
                }

                if (currentSubView == "liste") {
                    AdaptiveScreenContainer(maxWidth = 960.dp) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding(),
                            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                        // Header of the full-screen dialog
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { activeClassForStudentsList = null },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    ClassColorDot(activeClass.id, activeClass.name, size = 14)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = activeClass.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (Tr.isArabic()) "(${students.size} تلميذ)" else "(${students.size} élèves)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(onClick = {
                                        showExportClassListPdfDialog = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = if (Tr.isArabic()) "تصدير القائمة PDF" else "Exporter Liste PDF",
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                    IconButton(onClick = { showAddStudentDialog = true }) {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = if (Tr.isArabic()) "إضافة تلميذ" else "Ajouter un élève",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                SegmentedButton(
                                    selected = currentSubView == "liste",
                                    onClick = { currentSubView = "liste" },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(Tr.t("Liste"), fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                                SegmentedButton(
                                    selected = currentSubView == "plan",
                                    onClick = { currentSubView = "plan" },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(Tr.t("Plan"), fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                                SegmentedButton(
                                    selected = currentSubView == "stats",
                                    onClick = { currentSubView = "stats" },
                                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(Tr.t("Stats"), fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }
                        }

                        // --- BARRE DE RECHERCHE ET BOUTON DE FILTRE ---
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FineSearchBar(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = if (Tr.isArabic()) "بحث عن تلميذ بالاسم أو اللقب..." else "Rechercher un élève...",
                                    testTag = "search_student_field",
                                    modifier = Modifier.weight(1f)
                                )

                                // Button to toggle advanced filter panel
                                FilledIconButton(
                                    onClick = { showFiltersPanel = !showFiltersPanel },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = if (selectedGenderFilter != "Tous" || selectedAgeFilter != "Tous" || selectedAverageFilter != "Tous" || selectedGradeFilter != "Tous" || selectedSortOption != "Nom (A-Z)") {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        },
                                        contentColor = if (selectedGenderFilter != "Tous" || selectedAgeFilter != "Tous" || selectedAverageFilter != "Tous" || selectedGradeFilter != "Tous" || selectedSortOption != "Nom (A-Z)") {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        }
                                    ),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = if (Tr.isArabic()) "عوامل التصفية" else "Filtres",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // --- PANNEAU DE FILTRAGE EXPANDABLE ---
                        item {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = showFiltersPanel,
                                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
                            ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Filter Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (Tr.isArabic()) "تصفية وترتيب متقدم" else "Filtres avancés",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        TextButton(
                                            onClick = {
                                                selectedGenderFilter = "Tous"
                                                selectedAgeFilter = "Tous"
                                                selectedAverageFilter = "Tous"
                                                selectedGradeFilter = "Tous"
                                                selectedGroupFilter = "ALL"
                                                selectedSortOption = "Nom (A-Z)"
                                            }
                                        ) {
                                            Text(Tr.t("Réinitialiser"))
                                        }
                                    }

                                    // 1. Filter by Gender
                                    Column {
                                        Text(Tr.t("Sexe"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf("Tous", "M", "F").forEach { genderVal ->
                                                val label = when (genderVal) {
                                                    "M" -> Tr.t("Garçons ♂")
                                                    "F" -> Tr.t("Filles ♀")
                                                    else -> Tr.t("Tous")
                                                }
                                                FilterChip(
                                                    selected = selectedGenderFilter == genderVal,
                                                    onClick = { selectedGenderFilter = genderVal },
                                                    label = { Text(label) }
                                                )
                                            }
                                        }
                                    }

                                    // 2. Filter by Age (uniquement les âges renseignés dans la liste)
                                    if (presentAges.isNotEmpty()) {
                                        Column {
                                            Text(Tr.t("Âge"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                FilterChip(
                                                    selected = selectedAgeFilter == "Tous",
                                                    onClick = { selectedAgeFilter = "Tous" },
                                                    label = { Text(Tr.t("Tous")) }
                                                )
                                                presentAges.forEach { age ->
                                                    val ageStr = age.toString()
                                                    val label = if (Tr.isArabic()) "$age سنة" else "$age ans"
                                                    FilterChip(
                                                        selected = selectedAgeFilter == ageStr,
                                                        onClick = { selectedAgeFilter = ageStr },
                                                        label = { Text(label) }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // 3. Filter by Average
                                    Column {
                                        Text(Tr.t("Performance (Moyennes)"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf("Tous", "Admis", "Non-admis", "Excellent", "Sans Moyenne").forEach { avgVal ->
                                                val label = when (avgVal) {
                                                    "Admis" -> Tr.t("Admis (≥ 10)")
                                                    "Non-admis" -> Tr.t("Non-admis (< 10)")
                                                    "Excellent" -> Tr.t("Excellent (≥ 16)")
                                                    "Sans Moyenne" -> Tr.t("Non évalué")
                                                    else -> Tr.t("Tous")
                                                }
                                                FilterChip(
                                                    selected = selectedAverageFilter == avgVal,
                                                    onClick = { selectedAverageFilter = avgVal },
                                                    label = { Text(label) }
                                                )
                                            }
                                        }
                                    }

                                    // 4. Filter by Missing/Specific Grades
                                    Column {
                                        Text(Tr.t("Évaluations et Notes"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf("Tous", "MissingDevoir", "MissingFinal", "NoGrades").forEach { grdVal ->
                                                val label = when (grdVal) {
                                                    "MissingDevoir" -> Tr.t("Devoir manquant")
                                                    "MissingFinal" -> Tr.t("Composition manquante")
                                                    "NoGrades" -> Tr.t("Aucune note")
                                                    else -> Tr.t("Tous")
                                                }
                                                FilterChip(
                                                    selected = selectedGradeFilter == grdVal,
                                                    onClick = { selectedGradeFilter = grdVal },
                                                    label = { Text(label) }
                                                )
                                            }
                                        }
                                    }

                                    // 5. Sorting
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(Tr.t("Trier par"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf("Ordre personnalisé (N°)", "Nom (A-Z)", "Nom (Z-A)", "Moyenne (+)", "Moyenne (-)", "Âge (Le plus jeune)", "Âge (Le plus âgé)").forEach { sortVal ->
                                                FilterChip(
                                                    selected = selectedSortOption == sortVal,
                                                    onClick = { selectedSortOption = sortVal },
                                                    label = { Text(Tr.t(sortVal)) }
                                                )
                                            }
                                        }

                                        if (selectedSortOption != "Ordre personnalisé (N°)" && filteredStudents.isNotEmpty()) {
                                            FilledTonalButton(
                                                onClick = {
                                                    viewModel.reorderStudents(activeClass.id, filteredStudents.map { it.first })
                                                    selectedSortOption = "Ordre personnalisé (N°)"
                                                    Toast.makeText(context, if (isAr) "تم حفظ الترتيب بنجاح!" else "Classement sauvegardé avec succès !", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text(Tr.t("Enregistrer ce classement comme ordre officiel"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Group Quick Filter Row (if class has groups defined)
                    if (classGroups.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isAr) "الفوج :" else "Groupe :",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                FilterChip(
                                    selected = selectedGroupFilter == "ALL",
                                    onClick = { selectedGroupFilter = "ALL" },
                                    label = { Text("${if (isAr) "الكل" else "Tous"} (${students.size})") }
                                )
                                classGroups.forEach { grp ->
                                    val grpCount = students.count { it.groupName.trim() == grp }
                                    FilterChip(
                                        selected = selectedGroupFilter == grp,
                                        onClick = { selectedGroupFilter = grp },
                                        label = { Text("${if (isAr) grp.replace("G", "ف").replace("g", "ف") else grp} ($grpCount)") }
                                    )
                                }
                                val noGrpCount = students.count { it.groupName.trim().isBlank() }
                                if (noGrpCount > 0) {
                                    FilterChip(
                                        selected = selectedGroupFilter == "NONE",
                                        onClick = { selectedGroupFilter = "NONE" },
                                        label = { Text("${if (isAr) "بدون فوج" else "Sans groupe"} ($noGrpCount)") }
                                    )
                                }
                            }
                        }
                    }

                    if (students.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.PeopleOutline,
                                            contentDescription = "No students",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (Tr.isArabic()) "لا يوجد أي تلميذ في هذا الفوج.\nاضغط على '+' لإضافة تلميذ." else "Aucun élève dans cette classe.\nCliquez sur '+' pour en ajouter.",
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        } else if (filteredStudents.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "No results",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (Tr.isArabic()) "لا يوجد أي تلميذ يطابق معايير البحث." else "Aucun élève ne correspond à vos critères de recherche.",
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(filteredStudents, key = { _, (student, _, _) -> student.id }) { index, (student, _, average) ->
                                val studentOrderNum = if (student.orderIndex > 0) student.orderIndex else (index + 1)
                                StudentNumberedItemCard(
                                    number = studentOrderNum,
                                    student = student,
                                    average = null,
                                    onClick = { studentToEdit = student },
                                    onDelete = { studentToDelete = student },
                                    onChangePosition = {
                                        studentToChangePosition = Pair(student, studentOrderNum)
                                    }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(36.dp))
                            }
                        }
                    }
                    }
                } else if (currentSubView == "plan") {
                    // Conteneur dédié pour le Plan de classe horizontal (vue globale & édition fluide)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // En-tête de dialogue avec identification couleur de la classe
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { activeClassForStudentsList = null },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                ClassColorDot(activeClass.id, activeClass.name, size = 14)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = activeClass.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (Tr.isArabic()) "(${students.size} تلميذ)" else "(${students.size} élèves)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    planPdfLauncher.launch("Plan_de_classe_${activeClass.name.replace(" ", "_")}.pdf")
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = if (Tr.isArabic()) "تصدير مخطط الجلوس PDF" else "Exporter Plan PDF",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            SegmentedButton(
                                selected = currentSubView == "liste",
                                onClick = { currentSubView = "liste" },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(Tr.t("Liste"), fontSize = 11.sp, maxLines = 1)
                                }
                            }
                            SegmentedButton(
                                selected = currentSubView == "plan",
                                onClick = { currentSubView = "plan" },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(Tr.t("Plan"), fontSize = 11.sp, maxLines = 1)
                                }
                            }
                            SegmentedButton(
                                selected = currentSubView == "stats",
                                onClick = { currentSubView = "stats" },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(Tr.t("Stats"), fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }

                        HorizontalSeatingChartView(
                            students = students,
                            classId = activeClass.id,
                            onAssignSeat = { studentId, seatIdx ->
                                viewModel.assignStudentSeat(activeClass.id, studentId, seatIdx)
                            },
                            onFreeSeat = { seatIdx ->
                                viewModel.freeSeat(activeClass.id, seatIdx)
                            },
                            onSwapSeats = { studentId1, seatIdx1, studentId2, seatIdx2 ->
                                viewModel.swapStudentSeats(activeClass.id, studentId1, seatIdx1, studentId2, seatIdx2)
                            },
                            onAutoAssign = {
                                viewModel.autoAssignSeatsAlphabetical(activeClass.id)
                            },
                            onClearAll = {
                                viewModel.clearAllClassSeats(activeClass.id)
                            },
                            onExportPdf = {
                                planPdfLauncher.launch("Plan_de_classe_${activeClass.name.replace(" ", "_")}.pdf")
                            }
                        )
                    }
                } else {
                    AdaptiveScreenContainer(maxWidth = 960.dp) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                        // Header of the full-screen dialog for stats
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { activeClassForStudentsList = null },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                ClassColorDot(activeClass.id, activeClass.name, size = 14)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = activeClass.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (Tr.isArabic()) "(${students.size} تلميذ)" else "(${students.size} élèves)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    statsPdfLauncher.launch("Statistiques_${activeClass.name.replace(" ", "_")}.pdf")
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = if (Tr.isArabic()) "تصدير الإحصائيات PDF" else "Exporter Statistiques PDF",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            SegmentedButton(
                                selected = currentSubView == "liste",
                                onClick = { currentSubView = "liste" },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(Tr.t("Liste"), fontSize = 11.sp, maxLines = 1)
                                }
                            }
                            SegmentedButton(
                                selected = currentSubView == "plan",
                                onClick = { currentSubView = "plan" },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(Tr.t("Plan"), fontSize = 11.sp, maxLines = 1)
                                }
                            }
                            SegmentedButton(
                                selected = currentSubView == "stats",
                                onClick = { currentSubView = "stats" },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(Tr.t("Stats"), fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }

                        ClassStatisticsView(
                            students = students,
                            grades = grades,
                            activeClass = activeClass,
                            currentSettings = currentSettings,
                            onExportPdf = {
                                statsPdfLauncher.launch("Statistiques_${activeClass.name.replace(" ", "_")}.pdf")
                            }
                        )
                    }
                }

                // Dialogue pour changer le numéro / la position de l'élève
                if (studentToChangePosition != null && activeClass != null) {
                    val (targetStudent, currentPos) = studentToChangePosition!!
                    var targetPosNum by remember(targetStudent.id) { mutableIntStateOf(currentPos) }
                    var targetPosText by remember(targetStudent.id) { mutableStateOf(currentPos.toString()) }

                    AlertDialog(
                        onDismissRequest = { studentToChangePosition = null },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.FormatListNumbered,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        title = {
                            Text(
                                text = if (Tr.isArabic()) "تغيير الترتيب (الرقم)" else "Changer la position (N°)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "#$currentPos",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "${targetStudent.lastName.uppercase()} ${targetStudent.firstName}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (Tr.isArabic()) "الترتيب الحالي: رقم $currentPos من أصل ${students.size}" else "Position actuelle : n° $currentPos sur ${students.size}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = if (Tr.isArabic()) "أدخل رقم الترتيب الجديد (1 إلى ${students.size}) :" else "Saisissez le nouveau numéro d'ordre (1 à ${students.size}) :",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledTonalIconButton(
                                        onClick = {
                                            val next = (targetPosNum - 1).coerceAtLeast(1)
                                            targetPosNum = next
                                            targetPosText = next.toString()
                                        },
                                        enabled = targetPosNum > 1
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = if (Tr.isArabic()) "إنقاص" else "Diminuer")
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    OutlinedTextField(
                                        value = targetPosText,
                                        onValueChange = { input ->
                                            val clean = input.filter { it.isDigit() }
                                            targetPosText = clean
                                            val num = clean.toIntOrNull()
                                            if (num != null) {
                                                targetPosNum = num.coerceIn(1, students.size)
                                            }
                                        },
                                        singleLine = true,
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                        ),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            textAlign = TextAlign.Center
                                        ),
                                        modifier = Modifier.width(90.dp)
                                    )

                                    Spacer(Modifier.width(12.dp))

                                    FilledTonalIconButton(
                                        onClick = {
                                            val next = (targetPosNum + 1).coerceAtMost(students.size)
                                            targetPosNum = next
                                            targetPosText = next.toString()
                                        },
                                        enabled = targetPosNum < students.size
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = if (Tr.isArabic()) "زيادة" else "Augmenter")
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val finalTarget = targetPosNum.coerceIn(1, students.size)
                                    val currentOrdered = students.toMutableList()
                                    val fromIdx = currentOrdered.indexOfFirst { it.id == targetStudent.id }
                                    val toIdx = finalTarget - 1

                                    if (fromIdx != -1 && fromIdx != toIdx) {
                                        val item = currentOrdered.removeAt(fromIdx)
                                        currentOrdered.add(toIdx, item)
                                        viewModel.reorderStudents(activeClass.id, currentOrdered)
                                    }
                                    studentToChangePosition = null
                                }
                            ) {
                                Text(Tr.t("Appliquer"))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { studentToChangePosition = null }) {
                                Text(Tr.t("Annuler"))
                            }
                        }
                    )
                }

                // Dialogue d'options d'export PDF de la liste des élèves
                if (showExportClassListPdfDialog && activeClass != null) {
                    var exportScope by remember { mutableStateOf("ALL") } // "ALL" or "GROUPS"
                    val availableClassGroups = remember(students) {
                        val grps = students.map { it.groupName.trim() }.filter { it.isNotBlank() }.distinct().toMutableList()
                        if (grps.isEmpty()) {
                            listOf("G1", "G2", "G3", "G4")
                        } else grps.sorted()
                    }
                    val selectedGroupsForExport = remember { mutableStateListOf<String>().apply { addAll(availableClassGroups) } }
                    var includeUnassigned by remember { mutableStateOf(true) }

                    AlertDialog(
                        onDismissRequest = { showExportClassListPdfDialog = false },
                        icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        title = { Text(if (isAr) "تصدير قائمة التلاميذ PDF" else "Exporter la liste des élèves en PDF", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = if (isAr) "حدد نطاق التصدير :" else "Choisissez les élèves à inclure :",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                // Option 1: Toute la classe
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (exportScope == "ALL") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth().clickable { exportScope = "ALL" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(selected = exportScope == "ALL", onClick = { exportScope = "ALL" })
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = if (isAr) "كامل القسم (${students.size} تلميذ)" else "Toute la classe (${students.size} élèves)",
                                            fontWeight = if (exportScope == "ALL") FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }

                                // Option 2: Par groupe(s)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (exportScope == "GROUPS") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth().clickable { exportScope = "GROUPS" }
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(selected = exportScope == "GROUPS", onClick = { exportScope = "GROUPS" })
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = if (isAr) "أفواج محددة" else "Par groupe(s) sélectionné(s)",
                                                fontWeight = if (exportScope == "GROUPS") FontWeight.Bold else FontWeight.Normal
                                            )
                                        }

                                        if (exportScope == "GROUPS") {
                                            Spacer(Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                availableClassGroups.forEach { grp ->
                                                    val isSelected = selectedGroupsForExport.contains(grp)
                                                    val count = students.count { it.groupName.trim() == grp }
                                                    val grpLabel = if (isAr) grp.replace("G", "ف").replace("g", "ف") else grp
                                                    FilterChip(
                                                        selected = isSelected,
                                                        onClick = {
                                                            if (isSelected) selectedGroupsForExport.remove(grp) else selectedGroupsForExport.add(grp)
                                                        },
                                                        label = { Text("$grpLabel ($count)") },
                                                        leadingIcon = if (isSelected) {
                                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                                        } else null
                                                    )
                                                }
                                            }
                                            val unassignedCount = students.count { it.groupName.trim().isBlank() }
                                            if (unassignedCount > 0) {
                                                Spacer(Modifier.height(4.dp))
                                                FilterChip(
                                                    selected = includeUnassigned,
                                                    onClick = { includeUnassigned = !includeUnassigned },
                                                    label = { Text("${if (isAr) "بدون فوج" else "Sans groupe"} ($unassignedCount)") },
                                                    leadingIcon = if (includeUnassigned) {
                                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                                    } else null
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val list = if (exportScope == "ALL") {
                                        students
                                    } else {
                                        students.filter { s ->
                                            val g = s.groupName.trim()
                                            (g.isNotBlank() && selectedGroupsForExport.contains(g)) || (g.isBlank() && includeUnassigned)
                                        }
                                    }
                                    if (list.isEmpty()) {
                                        Toast.makeText(context, if (isAr) "لم يتم تحديد أي تلميذ للتصدير" else "Aucun élève sélectionné pour l'export", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val groupSubtitle = if (exportScope == "GROUPS") {
                                            val grps = selectedGroupsForExport.toList().sorted()
                                            if (grps.isNotEmpty()) {
                                                if (isAr) {
                                                    val arGrps = grps.map { it.replace("G", "ف").replace("g", "ف") }
                                                    if (arGrps.size == 1) "الفوج ${arGrps.first()}" else "الأفواج : ${arGrps.joinToString("، ")}"
                                                } else (if (grps.size == 1) "Groupe ${grps.first()}" else "Groupes: ${grps.joinToString(", ")}")
                                            } else if (includeUnassigned) {
                                                if (isAr) "بدون فوج" else "Sans groupe"
                                            } else ""
                                        } else ""

                                        groupSubtitleForClassPdfExport = groupSubtitle
                                        studentsForClassPdfExport = list
                                        showExportClassListPdfDialog = false
                                        val groupSuffix = if (exportScope == "GROUPS") {
                                            val grps = selectedGroupsForExport.toList().sorted()
                                            if (grps.isNotEmpty()) "_${grps.joinToString("_")}" else "_Groupes"
                                        } else ""
                                        classListPdfLauncher.launch("Liste_${activeClass.name.replace(" ", "_")}$groupSuffix.pdf")
                                    }
                                }
                            ) {
                                Text(if (isAr) "توليد PDF" else "Générer PDF")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showExportClassListPdfDialog = false }) {
                                Text(if (isAr) "إلغاء" else "Annuler")
                            }
                        }
                    )
                }
            }
        }
        }
    }

    // --- DIALOGS ---

    // 1. Ajouter une classe
    if (showAddClassDialog) {
        var className by remember { mutableStateOf("") }
        var classCoeff by remember { mutableStateOf((currentSettings?.subjectCoefficient ?: 3).toString()) }
        AlertDialog(
            onDismissRequest = { showAddClassDialog = false },
            title = { Text(text = Tr.t("Ajouter une classe")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = className,
                        onValueChange = { className = it },
                        label = { Text(Tr.t("Nom de la classe")) },
                        placeholder = { Text(if (Tr.isArabic()) "مثال: 1 ج م ع ت 1" else stringResource(id = R.string.class_name_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = classCoeff,
                        onValueChange = { classCoeff = it.filter { char -> char.isDigit() } },
                        label = { Text(Tr.t("Coefficient de la matière")) },
                        placeholder = { Text(if (Tr.isArabic()) "افتراضي : 3" else "Par défaut : 3") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (className.isNotBlank()) {
                            val coeffVal = classCoeff.toIntOrNull() ?: 3
                            viewModel.addClass(className, coeffVal)
                            showAddClassDialog = false
                        }
                    }
                ) {
                    Text(Tr.t("Ajouter"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddClassDialog = false }) {
                    Text(Tr.t("Annuler"))
                }
            }
        )
    }

    // 2. Modifier / Supprimer une classe (Long Click)
    if (classToEdit != null) {
        val editingClass = classToEdit!!
        var newClassName by remember { mutableStateOf(editingClass.name) }
        var newClassCoeff by remember { mutableStateOf(editingClass.subjectCoefficient.toString()) }
        AlertDialog(
            onDismissRequest = { classToEdit = null },
            title = { Text(text = Tr.t("Modifier la classe")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newClassName,
                        onValueChange = { newClassName = it },
                        label = { Text(Tr.t("Nom de la classe")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newClassCoeff,
                        onValueChange = { newClassCoeff = it.filter { char -> char.isDigit() } },
                        label = { Text(Tr.t("Coefficient de la matière")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            showDeleteClassConfirm = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Tr.t("Supprimer la classe et ses données"))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newClassName.isNotBlank()) {
                            viewModel.updateClass(
                                editingClass.copy(
                                    name = newClassName,
                                    subjectCoefficient = newClassCoeff.toIntOrNull() ?: 3
                                )
                            )
                            classToEdit = null
                        }
                    }
                ) {
                    Text(Tr.t("Enregistrer"))
                }
            },
            dismissButton = {
                TextButton(onClick = { classToEdit = null }) {
                    Text(Tr.t("Annuler"))
                }
            }
        )
    }

    if (showDeleteClassConfirm && classToEdit != null) {
        AlertDialog(
            onDismissRequest = { showDeleteClassConfirm = false },
            title = { Text(Tr.t("Confirmer la suppression")) },
            text = { Text(if (Tr.isArabic()) "هل تريد بالتأكيد حذف الفوج \"${classToEdit?.name}\" وجميع بياناته (التلاميذ، النقاط، إلخ)؟ هذا الإجراء لا يمكن التراجع عنه." else "Voulez-vous vraiment supprimer la classe \"${classToEdit?.name}\" ainsi que toutes ses données (élèves, notes, etc.) ? Cette action est irréversible.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        classToEdit?.let { viewModel.deleteClass(it) }
                        showDeleteClassConfirm = false
                        classToEdit = null
                    }
                ) {
                    Text(Tr.t("Supprimer"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteClassConfirm = false }) {
                    Text(Tr.t("Annuler"))
                }
            }
        )
    }

    // 3. Ajouter un élève
    if (showAddStudentDialog) {
        var lastName by remember { mutableStateOf("") }
        var firstName by remember { mutableStateOf("") }
        var birthDate by remember { mutableStateOf("") }
        var gender by remember { mutableStateOf("M") } // "M" or "F"
        var birthPlace by remember { mutableStateOf("") }
        var goodToKnow by remember { mutableStateOf("") }
        var repeatedYearsInput by remember { mutableStateOf("") }
        var groupNameInput by remember { mutableStateOf("") }

        val isArAddDialog = Tr.isArabic()
        val suggestedLevels = if (isArAddDialog) listOf("السنة الأولى", "السنة الثانية", "السنة الثالثة") else listOf("1AS", "2AS", "3AS")

        val computedAge = calculateAge(birthDate)
        val computedAgeDisplay = if (computedAge != null) " ($computedAge ans)" else ""

        AlertDialog(
            onDismissRequest = { showAddStudentDialog = false },
            title = { Text(text = Tr.t("Ajouter un élève")) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = adaptiveDialogMaxHeight(fraction = 0.65f, maxHeight = 520.dp)),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text(Tr.t("Nom de famille")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text(Tr.t("Prénom")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        BirthDateBoxesInput(
                            value = birthDate,
                            onValueChange = { birthDate = it },
                            ageDisplay = computedAgeDisplay
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = birthPlace,
                            onValueChange = { birthPlace = it },
                            label = { Text(Tr.t("Lieu de naissance")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (Tr.isArabic()) "السنوات أو المستويات المعادة :" else "Niveaux / Années refaites :",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            // Quick suggestion chips
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                            ) {
                                items(suggestedLevels) { level ->
                                    val currentTokens = repeatedYearsInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                    val containsLevel = currentTokens.any { 
                                        it.equals(level, ignoreCase = true) || formatRepeatedYearsDisplay(it, true).equals(level, ignoreCase = true)
                                    }
                                    FilterChip(
                                        selected = containsLevel,
                                        onClick = {
                                            val tokens = currentTokens.toMutableList()
                                            if (tokens.any { it.equals(level, ignoreCase = true) || formatRepeatedYearsDisplay(it, true).equals(level, ignoreCase = true) }) {
                                                tokens.removeAll { it.equals(level, ignoreCase = true) || formatRepeatedYearsDisplay(it, true).equals(level, ignoreCase = true) }
                                            } else {
                                                tokens.add(level)
                                            }
                                            repeatedYearsInput = tokens.joinToString(", ")
                                        },
                                        label = { Text(level, fontSize = 11.5.sp, fontWeight = if (containsLevel) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = if (containsLevel) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (Tr.isArabic()) "الفوج المسند إليه (اختياري) :" else "Groupe assigné (Optionnel) :",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("G1", "G2", "G3", "G4").forEach { grp ->
                                    val isSelected = groupNameInput == grp
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            groupNameInput = if (isSelected) "" else grp
                                        },
                                        label = { Text(if (Tr.isArabic()) grp.replace("G", "ف") else grp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = if (Tr.isArabic()) "الجنس :" else "Sexe :", fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = gender == "M", onClick = { gender = "M" })
                                Text(Tr.t("Garçon"))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = gender == "F", onClick = { gender = "F" })
                                Text(Tr.t("Fille"))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (lastName.isNotBlank() && firstName.isNotBlank()) {
                            viewModel.addStudent(
                                lastName = lastName,
                                firstName = firstName,
                                birthDate = if (birthDate.isNotBlank()) formatBirthDateDisplay(birthDate) else "01-01-2015",
                                gender = gender,
                                birthPlace = birthPlace,
                                goodToKnow = goodToKnow,
                                repeatedYears = repeatedYearsInput.trim(),
                                groupName = groupNameInput.trim()
                            )
                            showAddStudentDialog = false
                        }
                    }
                ) {
                    Text(Tr.t("Ajouter"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStudentDialog = false }) {
                    Text(Tr.t("Annuler"))
                }
            }
        )
    }

    // 4. Modifier / Supprimer un élève
    if (studentToEdit != null) {
        val editingStudent = studentToEdit!!
        var lastName by remember(editingStudent.id) { mutableStateOf(editingStudent.lastName) }
        var firstName by remember(editingStudent.id) { mutableStateOf(editingStudent.firstName) }
        var birthDate by remember(editingStudent.id) { mutableStateOf(formatBirthDateDisplay(editingStudent.birthDate)) }
        var gender by remember(editingStudent.id) { mutableStateOf(editingStudent.gender) }
        var observations by remember(editingStudent.id) { mutableStateOf(editingStudent.observations) }
        var birthPlace by remember(editingStudent.id) { mutableStateOf(editingStudent.birthPlace) }
        var goodToKnow by remember(editingStudent.id) { mutableStateOf(editingStudent.goodToKnow) }
        var repeatedYearsInput by remember(editingStudent.id) {
            mutableStateOf(editingStudent.repeatedYears)
        }
        var groupNameInput by remember(editingStudent.id) {
            mutableStateOf(editingStudent.groupName)
        }

        val isArEditDialog = Tr.isArabic()
        val suggestedLevels = if (isArEditDialog) listOf("السنة الأولى", "السنة الثانية", "السنة الثالثة") else listOf("1AS", "2AS", "3AS")

        val currentStudentPos = students.indexOfFirst { it.id == editingStudent.id }.let { if (it >= 0) it + 1 else if (editingStudent.orderIndex > 0) editingStudent.orderIndex else 1 }
        var rankNumberInput by remember(editingStudent.id) { mutableStateOf(currentStudentPos.toString()) }

        AlertDialog(
            onDismissRequest = { studentToEdit = null },
            title = {
                Text(
                    text = if (Tr.isArabic()) "ملف التلميذ : ${editingStudent.lastName.uppercase()} ${editingStudent.firstName}" else "Fiche Élève : ${editingStudent.lastName.uppercase()} ${editingStudent.firstName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = adaptiveDialogMaxHeight(fraction = 0.65f, maxHeight = 520.dp)),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // SECTION 1 : COORDONNÉES
                    item {
                        Text(
                            text = if (Tr.isArabic()) "المعلومات الشخصية" else "COORDONNÉES",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text(Tr.t("Nom de famille")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text(Tr.t("Prénom")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = rankNumberInput,
                            onValueChange = { rankNumberInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text(if (Tr.isArabic()) "الرقم في القائمة (رقم)" else "Numéro sur la liste (N°)") },
                            placeholder = { Text(if (Tr.isArabic()) "مثال: 1، 2، 5..." else "Ex: 1, 2, 5...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        val ageValue = calculateAge(birthDate)
                        val ageDisplay = if (ageValue != null) (if (Tr.isArabic()) " ($ageValue سنة)" else " ($ageValue ans)") else ""
                        BirthDateBoxesInput(
                            value = birthDate,
                            onValueChange = { birthDate = it },
                            ageDisplay = ageDisplay
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = birthPlace,
                            onValueChange = { birthPlace = it },
                            label = { Text(Tr.t("Lieu de naissance")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = if (Tr.isArabic()) "الجنس :" else "Sexe :", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = gender == "M", onClick = { gender = "M" })
                                Text(Tr.t("Garçon"), style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = gender == "F", onClick = { gender = "F" })
                                Text(Tr.t("Fille"), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    // SECTION 2 : OBSERVATIONS
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = if (Tr.isArabic()) "الملاحظات" else "OBSERVATIONS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = observations,
                            onValueChange = { observations = it },
                            label = { Text(if (Tr.isArabic()) "ملاحظات وتوجيهات حول التلميذ" else "Observations / Remarques sur l'élève") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (Tr.isArabic()) "السنوات أو المستويات المعادة :" else "Niveaux / Années refaites :",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            // Quick suggestion chips
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                            ) {
                                items(suggestedLevels) { level ->
                                    val currentTokens = repeatedYearsInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                    val containsLevel = currentTokens.any { 
                                        it.equals(level, ignoreCase = true) || formatRepeatedYearsDisplay(it, true).equals(level, ignoreCase = true)
                                    }
                                    FilterChip(
                                        selected = containsLevel,
                                        onClick = {
                                            val tokens = currentTokens.toMutableList()
                                            if (tokens.any { it.equals(level, ignoreCase = true) || formatRepeatedYearsDisplay(it, true).equals(level, ignoreCase = true) }) {
                                                tokens.removeAll { it.equals(level, ignoreCase = true) || formatRepeatedYearsDisplay(it, true).equals(level, ignoreCase = true) }
                                            } else {
                                                tokens.add(level)
                                            }
                                            repeatedYearsInput = tokens.joinToString(", ")
                                        },
                                        label = { Text(level, fontSize = 11.5.sp, fontWeight = if (containsLevel) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = if (containsLevel) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (Tr.isArabic()) "الفوج المسند إليه :" else "Groupe assigné :",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("G1", "G2", "G3", "G4").forEach { grp ->
                                    val isSelected = groupNameInput == grp
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            groupNameInput = if (isSelected) "" else grp
                                        },
                                        label = { Text(if (Tr.isArabic()) grp.replace("G", "ف") else grp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            onClick = {
                                showDeleteStudentFromEditConfirm = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (Tr.isArabic()) "حذف التلميذ نهائياً" else "Supprimer l'élève définitivement")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (lastName.isNotBlank() && firstName.isNotBlank()) {
                            // Update Coordinates and Observations
                            val updatedStudent = editingStudent.copy(
                                lastName = lastName,
                                firstName = firstName,
                                birthDate = formatBirthDateDisplay(birthDate),
                                gender = gender,
                                observations = observations,
                                birthPlace = birthPlace,
                                goodToKnow = goodToKnow,
                                repeatedYears = repeatedYearsInput.trim(),
                                groupName = groupNameInput.trim()
                            )
                            viewModel.updateStudent(updatedStudent)

                            val targetPos = rankNumberInput.toIntOrNull()
                            if (activeClass != null && targetPos != null && targetPos > 0 && targetPos != currentStudentPos) {
                                val currentOrdered = students.toMutableList()
                                val fromIdx = currentOrdered.indexOfFirst { it.id == editingStudent.id }
                                if (fromIdx != -1) {
                                    val toIdx = (targetPos - 1).coerceIn(0, currentOrdered.size - 1)
                                    if (fromIdx != toIdx) {
                                        val item = currentOrdered.removeAt(fromIdx)
                                        currentOrdered.add(toIdx, item)
                                        viewModel.reorderStudents(activeClass.id, currentOrdered)
                                    }
                                }
                            }
                            studentToEdit = null
                        }
                    }
                ) {
                    Text(Tr.t("Enregistrer"))
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToEdit = null }) {
                    Text(Tr.t("Annuler"))
                }
            }
        )
    }

    if (showDeleteStudentFromEditConfirm && studentToEdit != null) {
        val student = studentToEdit!!
        AlertDialog(
            onDismissRequest = { showDeleteStudentFromEditConfirm = false },
            title = { Text(Tr.t("Confirmer la suppression")) },
            text = { Text(if (Tr.isArabic()) "هل تريد بالتأكيد حذف التلميذ \"${student.firstName} ${student.lastName}\" وجميع نقاطه؟ هذا الإجراء لا يمكن التراجع عنه." else "Voulez-vous vraiment supprimer l'élève \"${student.firstName} ${student.lastName}\" ainsi que toutes ses notes ? Cette action est irréversible.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteStudent(student)
                        showDeleteStudentFromEditConfirm = false
                        studentToEdit = null
                    }
                ) {
                    Text(Tr.t("Supprimer"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteStudentFromEditConfirm = false }) {
                    Text(Tr.t("Annuler"))
                }
            }
        )
    }

    if (studentToDelete != null) {
        val student = studentToDelete!!
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text(Tr.t("Confirmer la suppression")) },
            text = { Text(if (Tr.isArabic()) "هل تريد بالتأكيد حذف التلميذ \"${student.firstName} ${student.lastName}\" وجميع نقاطه؟ هذا الإجراء لا يمكن التراجع عنه." else "Voulez-vous vraiment supprimer l'élève \"${student.firstName} ${student.lastName}\" ainsi que toutes ses notes ? Cette action est irréversible.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteStudent(student)
                        studentToDelete = null
                    }
                ) {
                    Text(Tr.t("Supprimer"))
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete = null }) {
                    Text(Tr.t("Annuler"))
                }
            }
        )
    }

    // 7. Divide Class Into Groups Dialog
    if (showDivideGroupsDialog && activeClassForStudentsList != null) {
        val activeClass = activeClassForStudentsList!!
        DivideClassIntoGroupsDialog(
            className = activeClass.name,
            students = students,
            onDismiss = { showDivideGroupsDialog = false },
            onSaveGroups = { assignments ->
                viewModel.saveCustomClassGroups(activeClass.id, assignments)
                showDivideGroupsDialog = false
            },
            onReset = {
                viewModel.resetClassGroups(activeClass.id)
                showDivideGroupsDialog = false
            }
        )
    }
}

@Composable
fun DivideClassIntoGroupsDialog(
    className: String,
    students: List<StudentEntity>,
    onDismiss: () -> Unit,
    onSaveGroups: (Map<Int, String>) -> Unit,
    onReset: () -> Unit
) {
    val isAr = Tr.isArabic()
    var selectedGroupCount by remember { mutableStateOf(2) }
    val groupAssignments = remember(students) {
        mutableStateMapOf<Int, String>().apply {
            students.forEach { s ->
                put(s.id, s.groupName.trim())
            }
        }
    }
    var studentSearchQuery by remember { mutableStateOf("") }
    var selectedTabFilter by remember { mutableStateOf("ALL") } // "ALL", "G1", "G2"..., "NONE"
    var showAutoFillMenu by remember { mutableStateOf(false) }

    fun applyAutoDistribution(method: String) {
        val sorted = when (method) {
            "GENDER" -> {
                val boys = students.filter { it.gender.equals("M", ignoreCase = true) }
                    .sortedWith(compareBy({ it.lastName.lowercase() }, { it.firstName.lowercase() }))
                val girls = students.filter { !it.gender.equals("M", ignoreCase = true) }
                    .sortedWith(compareBy({ it.lastName.lowercase() }, { it.firstName.lowercase() }))
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

        when (method) {
            "CLEAR" -> {
                students.forEach { groupAssignments[it.id] = "" }
            }
            "ALPHABETICAL" -> {
                val chunkSize = (sorted.size + selectedGroupCount - 1) / selectedGroupCount
                sorted.forEachIndexed { index, student ->
                    val groupIndex = (index / chunkSize).coerceAtMost(selectedGroupCount - 1) + 1
                    groupAssignments[student.id] = "G$groupIndex"
                }
            }
            else -> { // "ALTERNATE" and "GENDER"
                sorted.forEachIndexed { index, student ->
                    val groupIndex = (index % selectedGroupCount) + 1
                    groupAssignments[student.id] = "G$groupIndex"
                }
            }
        }
    }

    val availableGroups = (1..selectedGroupCount).map { "G$it" }

    val filteredStudents = remember(students, studentSearchQuery, selectedTabFilter, groupAssignments.toMap()) {
        students.filter { s ->
            val assigned = groupAssignments[s.id] ?: ""
            val matchesTab = when (selectedTabFilter) {
                "ALL" -> true
                "NONE" -> assigned.isBlank()
                else -> assigned == selectedTabFilter
            }
            val matchesSearch = if (studentSearchQuery.isBlank()) true else {
                val q = studentSearchQuery.trim().lowercase()
                s.lastName.lowercase().contains(q) || s.firstName.lowercase().contains(q)
            }
            matchesTab && matchesSearch
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 760.dp)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = if (isAr) "توزيع التلاميذ على الأفواج" else "Répartition des élèves par groupe",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$className • ${students.size} ${if (isAr) (if (students.size <= 1) "تلميذ" else "تلاميذ") else (if (students.size <= 1) "élève" else "élèves")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Number of Groups Selector + Quick Auto-Fill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isAr) "عدد الأفواج :" else "Nb groupes :",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        listOf(2, 3, 4).forEach { count ->
                            val isSel = selectedGroupCount == count
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedGroupCount = count },
                                label = { Text("$count", fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }

                    // Auto-fill menu
                    Box {
                        FilledTonalButton(
                            onClick = { showAutoFillMenu = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isAr) "توزيع مقترح" else "Pré-remplir", fontSize = 11.5.sp)
                        }
                        DropdownMenu(
                            expanded = showAutoFillMenu,
                            onDismissRequest = { showAutoFillMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isAr) "ترتيب أبجدي (A-Z)" else "Par ordre alphabétique") },
                                leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null) },
                                onClick = {
                                    applyAutoDistribution("ALPHABETICAL")
                                    showAutoFillMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isAr) "تناوبي متوازن (1، 2، 1، 2...)" else "Alterné (1, 2, 1, 2...)") },
                                leadingIcon = { Icon(Icons.Default.SyncAlt, contentDescription = null) },
                                onClick = {
                                    applyAutoDistribution("ALTERNATE")
                                    showAutoFillMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isAr) "مناصفة وتوازن الذكور والإناث" else "Parité Mixte (Filles / Garçons)") },
                                leadingIcon = { Icon(Icons.Default.Wc, contentDescription = null) },
                                onClick = {
                                    applyAutoDistribution("GENDER")
                                    showAutoFillMenu = false
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(if (isAr) "تفريغ كل الأفواج" else "Effacer les affectations") },
                                leadingIcon = { Icon(Icons.Default.ClearAll, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    applyAutoDistribution("CLEAR")
                                    showAutoFillMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Group summary cards (Horizontal scroll)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val allSelected = selectedTabFilter == "ALL"
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (allSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (allSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.clickable { selectedTabFilter = "ALL" }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isAr) "الكل" else "Tous",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (allSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${students.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${students.count { it.gender.equals("M", ignoreCase = true) }}G / ${students.count { !it.gender.equals("M", ignoreCase = true) }}F",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(availableGroups) { grp ->
                        val inGrp = students.filter { (groupAssignments[it.id] ?: "") == grp }
                        val boys = inGrp.count { it.gender.equals("M", ignoreCase = true) }
                        val girls = inGrp.count { !it.gender.equals("M", ignoreCase = true) }
                        val isSelectedTab = selectedTabFilter == grp

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelectedTab) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelectedTab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.clickable {
                                selectedTabFilter = if (selectedTabFilter == grp) "ALL" else grp
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isAr) grp.replace("G", "ف").replace("g", "ف") else grp,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isSelectedTab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${inGrp.size} ${if (isAr) "تلميذ" else "élèves"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isAr) "${boys}ذ / ${girls}ث" else "${boys}G / ${girls}F",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        val unassigned = students.filter { (groupAssignments[it.id] ?: "").isBlank() }
                        val isSelectedTab = selectedTabFilter == "NONE"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelectedTab) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelectedTab) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.clickable {
                                selectedTabFilter = if (selectedTabFilter == "NONE") "ALL" else "NONE"
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isAr) "بدون فوج" else "Non assignés",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = if (unassigned.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${unassigned.size}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (unassigned.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar
                OutlinedTextField(
                    value = studentSearchQuery,
                    onValueChange = { studentSearchQuery = it },
                    placeholder = { Text(if (isAr) "بحث عن تلميذ لتعديل فوجه..." else "Rechercher un élève...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = if (studentSearchQuery.isNotBlank()) {
                        {
                            IconButton(onClick = { studentSearchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Student List with 1-tap group chooser
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (filteredStudents.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isAr) "لا توجد نتائج مطابقة" else "Aucun élève trouvé",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        itemsIndexed(filteredStudents, key = { _, student -> student.id }) { _, student ->
                            val currentGroup = groupAssignments[student.id] ?: ""
                            val isBoy = student.gender.equals("M", ignoreCase = true)
                            val avatarBg = if (isBoy) Color(0xFFE0F2FE) else Color(0xFFFCE7F3)
                            val avatarTc = if (isBoy) Color(0xFF0284C7) else Color(0xFFDB2777)

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Student Info
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(avatarBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isBoy) "G" else "F",
                                                color = avatarTc,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "${student.lastName.uppercase()} ${student.firstName}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (isBoy) (if (isAr) "ذكر" else "Garçon") else (if (isAr) "أنثى" else "Fille"),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Group Choice Buttons: G1, G2, G3...
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        availableGroups.forEach { grp ->
                                            val isAssigned = currentGroup == grp
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isAssigned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (isAssigned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                                ),
                                                modifier = Modifier.clickable {
                                                    groupAssignments[student.id] = if (isAssigned) "" else grp
                                                }
                                            ) {
                                                Text(
                                                    text = if (isAr) grp.replace("G", "ف").replace("g", "ف") else grp,
                                                    color = if (isAssigned) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = if (isAssigned) FontWeight.ExtraBold else FontWeight.Medium,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                                )
                                            }
                                        }

                                        if (currentGroup.isNotBlank()) {
                                            IconButton(
                                                onClick = { groupAssignments[student.id] = "" },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Retirer du groupe",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onReset,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isAr) "إلغاء التقسيم" else "Réinitialiser", fontSize = 11.5.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) {
                            Text(Tr.t("Annuler"))
                        }
                        Button(
                            onClick = { onSaveGroups(groupAssignments.toMap()) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isAr) "حفظ التوزيع" else "Enregistrer les groupes")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentItemCard(
    student: StudentEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isBoy = student.gender.equals("M", ignoreCase = true)
    val nameBg = if (isBoy) Color(0xFFDBEAFE) else Color(0xFFFCE7F3)
    val nameBorder = if (isBoy) Color(0xFF93C5FD) else Color(0xFFF472B6)
    val nameTextColor = if (isBoy) Color(0xFF1E40AF) else Color(0xFF9D174D)
    val iconTint = if (isBoy) Color(0xFF2563EB) else Color(0xFFDB2777)

    val isDark = MaterialTheme.colorScheme.background != OffWhite
    val genderTint = if (isBoy) Color(0xFF2563EB) else Color(0xFFDB2777)
    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        tintColor = genderTint,
        tintAlpha = if (isDark) 0.08f else 0.05f,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Colored background for student's full name (blue for boys, pink for girls)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = nameBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, nameBorder.copy(alpha = 0.8f))
                ) {
                    Text(
                        text = "${student.lastName.uppercase()} ${student.firstName}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = nameTextColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                val age = calculateAge(student.birthDate)
                val ageStr = if (age != null) " ($age ans)" else ""
                Text(
                    text = "Né(e) le : ${formatBirthDateDisplay(student.birthDate)}$ageStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp)
                )
                if (student.birthPlace.isNotBlank()) {
                    Text(
                        text = "Lieu : ${student.birthPlace}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StudentNumberedItemCard(
    number: Int,
    student: StudentEntity,
    average: Float? = null,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onChangePosition: () -> Unit
) {
    val isMale = student.gender == "M"
    val isDark = MaterialTheme.colorScheme.background != OffWhite

    // Rich color palette per gender with harmonious pastel accents
    val primaryAccent = if (isDark) (if (isMale) Color(0xFF38BDF8) else Color(0xFFF472B6)) else (if (isMale) Color(0xFF0284C7) else Color(0xFFDB2777))
    val containerBg = if (isDark) {
        if (isMale) Color(0xFF0C243C).copy(alpha = 0.6f) else Color(0xFF3B122D).copy(alpha = 0.6f)
    } else {
        if (isMale) Color(0xFFF0F9FF) else Color(0xFFFDF2F8)
    }
    val genderBadgeBg = if (isDark) primaryAccent.copy(alpha = 0.2f) else (if (isMale) Color(0xFFE0F2FE) else Color(0xFFFCE7F3))

    val initials = listOfNotNull(student.firstName.firstOrNull(), student.lastName.firstOrNull())
        .joinToString("").uppercase()

    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        tintColor = primaryAccent,
        tintAlpha = if (isDark) 0.08f else 0.05f,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            // Ligne du haut : Badge Numéro, Nom & Prénom dans un rectangle coloré selon le sexe, Badge Groupe, Symbole Sexe
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Badge Numéro / Rang (Cliquable pour réordonner)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = primaryAccent,
                        modifier = Modifier.clickable(onClick = onChangePosition)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$number",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = if (Tr.isArabic()) "تغيير الترتيب" else "Changer la position",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(9.dp))

                    // Nom & Prénom complets dans un rectangle coloré (Rose pour filles, Bleu pour garçons)
                    val nameBgColor = if (isDark) {
                        if (isMale) Color(0xFF0C2E4E).copy(alpha = 0.9f) else Color(0xFF4C1D3A).copy(alpha = 0.9f)
                    } else {
                        if (isMale) Color(0xFFE0F2FE) else Color(0xFFFCE7F3)
                    }
                    val nameBorderColor = if (isDark) {
                        if (isMale) Color(0xFF38BDF8).copy(alpha = 0.8f) else Color(0xFFF472B6).copy(alpha = 0.8f)
                    } else {
                        if (isMale) Color(0xFF7DD3FC) else Color(0xFFF9A8D4)
                    }
                    val nameTextColor = if (isDark) {
                        if (isMale) Color(0xFFBAE6FD) else Color(0xFFFBCFE8)
                    } else {
                        if (isMale) Color(0xFF0369A1) else Color(0xFFBE185D)
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = nameBgColor,
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, nameBorderColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${student.lastName.uppercase()} ${student.firstName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = nameTextColor,
                            fontSize = 14.5.sp,
                            lineHeight = 18.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Badge Groupe si défini (abréviation 'ف' en arabe)
                if (student.groupName.isNotBlank()) {
                    val groupDisplay = if (Tr.isArabic()) student.groupName.replace("G", "ف").replace("g", "ف") else student.groupName
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = groupDisplay,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = primaryAccent.copy(alpha = 0.15f), thickness = 0.8.dp)

            // Informations d'état civil : Date de naissance (+ âge), et Lieu de naissance SOUS la date
            val age = calculateAge(student.birthDate)
            val ageStr = if (age != null) (if (Tr.isArabic()) " ($age سنة)" else " ($age ans)") else ""
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Date de naissance
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cake,
                            contentDescription = if (Tr.isArabic()) "تاريخ الميلاد" else "Date de naissance",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = (if (Tr.isArabic()) "تاريخ الميلاد : " else "Né(e) le : ") + "${formatBirthDateDisplay(student.birthDate).ifBlank { if (Tr.isArabic()) "غير محدد" else "N/R" }}$ageStr",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 2. Lieu de naissance placé sous la date
                if (student.birthPlace.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = if (Tr.isArabic()) "مكان الميلاد" else "Lieu de naissance",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = (if (Tr.isArabic()) "مكان الميلاد : " else "Lieu de naissance : ") + student.birthPlace,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Case pour spécifier le niveau refait (1AS / 2AS / 3AS) uniquement si redoublant
            if (student.repeatedYears.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = if (Tr.isArabic()) "معيد" else "Redoublant",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = (if (Tr.isArabic()) "المستوى المعاد : " else "Niveau refait : ") + formatRepeatedYearsDisplay(student.repeatedYears, Tr.isArabic()),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Place assignée dans le plan de classe
            if (student.seatIndex != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chair,
                            contentDescription = if (Tr.isArabic()) "المقعد" else "Place",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (Tr.isArabic()) "المقعد المعين : طاولة رقم ${student.seatIndex + 1}" else "Place assignée : Table n° ${student.seatIndex + 1}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Observations (si renseignées)
            if (student.observations.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = if (Tr.isArabic()) "الملاحظات" else "Observations",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp).padding(top = 1.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (Tr.isArabic()) "الملاحظات : ${student.observations}" else "Observations : ${student.observations}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BirthDateBoxesInput(
    value: String,
    onValueChange: (String) -> Unit,
    ageDisplay: String = "",
    modifier: Modifier = Modifier
) {
    val rawDigits = remember(value) {
        value.filter { it.isDigit() }.take(8)
    }

    val focusRequesters = remember { List(8) { FocusRequester() } }
    var focusedIndex by remember { mutableStateOf(-1) }

    fun emitFromDigits(d: String) {
        val sanitized = d.filter { it.isDigit() }.take(8)
        val formatted = when {
            sanitized.isEmpty() -> ""
            sanitized.length <= 2 -> sanitized
            sanitized.length <= 4 -> "${sanitized.substring(0, 2)}-${sanitized.substring(2)}"
            else -> "${sanitized.substring(0, 2)}-${sanitized.substring(2, 4)}-${sanitized.substring(4)}"
        }
        onValueChange(formatted)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (Tr.isArabic()) "تاريخ الميلاد " else "Date de naissance ",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (ageDisplay.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = ageDisplay.trim(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Cases de saisie : 2 pour le Jour, 2 pour le Mois, 4 pour l'Année (Toujours de Gauche à Droite : Jour - Mois - Année)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // JOUR : 2 cases
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (i in 0..1) {
                            SingleDigitBox(
                                index = i,
                                rawDigits = rawDigits,
                                isFocused = focusedIndex == i,
                                focusRequester = focusRequesters[i],
                                onFocusChanged = { if (it) focusedIndex = i else if (focusedIndex == i) focusedIndex = -1 },
                                onDigitEntered = { char ->
                                    val sb = StringBuilder(rawDigits.padEnd(8, ' '))
                                    sb[i] = char
                                    val updated = sb.toString().replace(" ", "").take(8)
                                    emitFromDigits(updated)
                                    if (i < 7) {
                                        focusRequesters[i + 1].requestFocus()
                                    }
                                },
                                onBackspace = {
                                    if (i < rawDigits.length) {
                                        val updated = rawDigits.removeRange(i, i + 1)
                                        emitFromDigits(updated)
                                    } else if (i > 0) {
                                        focusRequesters[i - 1].requestFocus()
                                    }
                                },
                                onPasted = { pastedText ->
                                    val digits = pastedText.filter { it.isDigit() }.take(8)
                                    if (digits.isNotEmpty()) {
                                        emitFromDigits(digits)
                                        val nextFocus = minOf(7, digits.length)
                                        focusRequesters[nextFocus].requestFocus()
                                    }
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(if (Tr.isArabic()) "اليوم" else "Jour", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(Modifier.width(5.dp))
                Text(
                    text = "-",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(5.dp))

                // MOIS : 2 cases
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (i in 2..3) {
                            SingleDigitBox(
                                index = i,
                                rawDigits = rawDigits,
                                isFocused = focusedIndex == i,
                                focusRequester = focusRequesters[i],
                                onFocusChanged = { if (it) focusedIndex = i else if (focusedIndex == i) focusedIndex = -1 },
                                onDigitEntered = { char ->
                                    val sb = StringBuilder(rawDigits.padEnd(8, ' '))
                                    sb[i] = char
                                    val updated = sb.toString().replace(" ", "").take(8)
                                    emitFromDigits(updated)
                                    if (i < 7) {
                                        focusRequesters[i + 1].requestFocus()
                                    }
                                },
                                onBackspace = {
                                    if (i < rawDigits.length) {
                                        val updated = rawDigits.removeRange(i, i + 1)
                                        emitFromDigits(updated)
                                    } else if (i > 0) {
                                        focusRequesters[i - 1].requestFocus()
                                    }
                                },
                                onPasted = { pastedText ->
                                    val digits = pastedText.filter { it.isDigit() }.take(8)
                                    if (digits.isNotEmpty()) {
                                        emitFromDigits(digits)
                                        val nextFocus = minOf(7, digits.length)
                                        focusRequesters[nextFocus].requestFocus()
                                    }
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(if (Tr.isArabic()) "الشهر" else "Mois", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(Modifier.width(5.dp))
                Text(
                    text = "-",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(5.dp))

                // ANNÉE : 4 cases
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (i in 4..7) {
                            SingleDigitBox(
                                index = i,
                                rawDigits = rawDigits,
                                isFocused = focusedIndex == i,
                                focusRequester = focusRequesters[i],
                                onFocusChanged = { if (it) focusedIndex = i else if (focusedIndex == i) focusedIndex = -1 },
                                onDigitEntered = { char ->
                                    val sb = StringBuilder(rawDigits.padEnd(8, ' '))
                                    sb[i] = char
                                    val updated = sb.toString().replace(" ", "").take(8)
                                    emitFromDigits(updated)
                                    if (i < 7) {
                                        focusRequesters[i + 1].requestFocus()
                                    }
                                },
                                onBackspace = {
                                    if (i < rawDigits.length) {
                                        val updated = rawDigits.removeRange(i, i + 1)
                                        emitFromDigits(updated)
                                    } else if (i > 0) {
                                        focusRequesters[i - 1].requestFocus()
                                    }
                                },
                                onPasted = { pastedText ->
                                    val digits = pastedText.filter { it.isDigit() }.take(8)
                                    if (digits.isNotEmpty()) {
                                        emitFromDigits(digits)
                                        val nextFocus = minOf(7, digits.length)
                                        focusRequesters[nextFocus].requestFocus()
                                    }
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(if (Tr.isArabic()) "السنة" else "Année", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun SingleDigitBox(
    index: Int,
    rawDigits: String,
    isFocused: Boolean,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onDigitEntered: (Char) -> Unit,
    onBackspace: () -> Unit,
    onPasted: (String) -> Unit
) {
    val char = if (index < rawDigits.length) rawDigits[index].toString() else ""

    val boxBg = if (isFocused) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f)
    }
    val boxBorderColor = if (isFocused) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
    }

    Box(
        modifier = Modifier
            .size(width = 26.dp, height = 42.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(boxBg)
            .border(
                width = if (isFocused) 2.dp else 1.5.dp,
                color = boxBorderColor,
                shape = RoundedCornerShape(6.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = char,
            onValueChange = { input ->
                val digitsOnly = input.filter { it.isDigit() }
                if (digitsOnly.length > 2) {
                    onPasted(input)
                } else if (digitsOnly.isNotEmpty()) {
                    val newChar = if (digitsOnly.length > 1 && digitsOnly.startsWith(char)) digitsOnly.last() else digitsOnly.first()
                    onDigitEntered(newChar)
                } else {
                    onBackspace()
                }
            },
            textStyle = LocalTextStyle.current.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = if (index == 7) ImeAction.Done else ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxSize()
                .wrapContentHeight(Alignment.CenterVertically)
                .focusRequester(focusRequester)
                .onFocusChanged { onFocusChanged(it.isFocused) }
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Backspace &&
                        keyEvent.type == KeyEventType.KeyDown
                    ) {
                        if (char.isEmpty()) {
                            onBackspace()
                            true
                        } else false
                    } else false
                }
        )
    }
}

private fun formatBirthDateInput(raw: String, previousText: String = ""): String {
    // 1. Détection de l'effacement arrière (Backspace)
    val isDeleting = raw.length < previousText.length
    if (isDeleting) {
        val sanitized = if (previousText.endsWith("-") && !raw.endsWith("-") && raw.isNotEmpty() && raw.last().isDigit()) {
            raw.dropLast(1)
        } else {
            raw
        }
        val digits = sanitized.filter { it.isDigit() }.take(8)
        return when {
            digits.isEmpty() -> ""
            digits.length <= 2 -> digits
            digits.length <= 4 -> "${digits.substring(0, 2)}-${digits.substring(2)}"
            else -> "${digits.substring(0, 2)}-${digits.substring(2, 4)}-${digits.substring(4)}"
        }
    }

    if (raw.isBlank()) return ""

    val normalized = raw.replace("/", "-")
    val rawParts = normalized.split("-")

    // Partie 0 : Jour (2 chiffres max) + débordement (overflow) vers le mois si curseur avant tiret
    val p0 = rawParts.getOrNull(0)?.filter { it.isDigit() } ?: ""
    val day = p0.take(2)
    val overflow0 = p0.drop(2)

    // Partie 1 : Mois (2 chiffres max) + débordement vers l'année
    val p1Raw = rawParts.getOrNull(1)?.filter { it.isDigit() } ?: ""
    val p1 = overflow0 + p1Raw
    var month = p1.take(2)
    val overflow1 = p1.drop(2)

    // Partie 2 : Année (4 chiffres max)
    val p2Raw = rawParts.getOrNull(2)?.filter { it.isDigit() } ?: ""
    val p2 = overflow1 + p2Raw
    val year = p2.take(4)

    val dashCount = normalized.count { it == '-' }

    // Si l'utilisateur tape explicitement un tiret après un chiffre unique pour le jour (ex: "5-" -> "05-")
    var formattedDay = day
    if (dashCount >= 1 && formattedDay.length == 1 && overflow0.isEmpty()) {
        formattedDay = "0$formattedDay"
    }

    // Si l'utilisateur tape explicitement un tiret après un chiffre unique pour le mois (ex: "10-6-" -> "10-06-")
    if (dashCount >= 2 && month.length == 1 && overflow1.isEmpty()) {
        month = "0$month"
    }

    val sb = StringBuilder()
    if (formattedDay.isNotEmpty()) {
        sb.append(formattedDay)
        if (formattedDay.length == 2 || dashCount >= 1) {
            sb.append("-")
        }
    }

    if (month.isNotEmpty()) {
        sb.append(month)
        if (month.length == 2 || dashCount >= 2) {
            sb.append("-")
        }
    }

    if (year.isNotEmpty()) {
        sb.append(year)
    }

    return sb.toString()
}

private fun formatBirthDateDisplay(dateStr: String): String {
    if (dateStr.isBlank()) return ""
    val parts = dateStr.trim().split("-", "/")
    if (parts.size == 3) {
        if (parts[0].length == 4) {
            // YYYY-MM-DD -> JJ-MM-AAAA
            val year = parts[0]
            val month = parts[1].padStart(2, '0')
            val day = parts[2].padStart(2, '0')
            return "$day-$month-$year"
        } else if (parts[2].length == 4) {
            // JJ-MM-AAAA
            val day = parts[0].padStart(2, '0')
            val month = parts[1].padStart(2, '0')
            val year = parts[2]
            return "$day-$month-$year"
        }
    }
    return dateStr
}

private fun parseBirthDateToMillis(dateStr: String): Long {
    return try {
        val parts = dateStr.trim().split("-", "/")
        if (parts.size == 3) {
            val (day, month, year) = if (parts[0].length == 4) {
                Triple(parts[2].toIntOrNull(), parts[1].toIntOrNull(), parts[0].toIntOrNull())
            } else {
                Triple(parts[0].toIntOrNull(), parts[1].toIntOrNull(), parts[2].toIntOrNull())
            }
            if (year != null && month != null && day != null) {
                val cal = Calendar.getInstance().apply {
                    set(year, month - 1, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                cal.timeInMillis
            } else 0L
        } else 0L
    } catch (e: Exception) {
        0L
    }
}

private fun calculateAge(birthDateStr: String): Int? {
    return try {
        val clean = birthDateStr.trim()
        if (clean.isBlank()) return null
        val parts = clean.split(Regex("[\\-/\\.\\s]+"))
        if (parts.size == 3) {
            val (day, month, year) = if (parts[0].length == 4) {
                // AAAA-MM-JJ (YYYY-MM-DD)
                Triple(parts[2].toIntOrNull(), parts[1].toIntOrNull(), parts[0].toIntOrNull())
            } else {
                // JJ-MM-AAAA (DD-MM-YYYY)
                Triple(parts[0].toIntOrNull(), parts[1].toIntOrNull(), parts[2].toIntOrNull())
            }
            if (year != null && month != null && day != null && month in 1..12 && day in 1..31 && year in 1900..2100) {
                val birthCal = Calendar.getInstance().apply {
                    set(year, month - 1, day)
                }
                val today = Calendar.getInstance()
                var age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
                if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                    age--
                }
                if (age in 0..120) age else null
            } else null
        } else null
    } catch (e: Exception) {
        null
    }
}

@Composable
fun SeatingChartView(
    students: List<StudentEntity>,
    onAssignSeat: (Int, Int) -> Unit,
    onFreeSeat: (Int) -> Unit
) {
    HorizontalSeatingChartView(
        students = students,
        classId = students.firstOrNull()?.classId ?: 0,
        onAssignSeat = onAssignSeat,
        onFreeSeat = onFreeSeat,
        onSwapSeats = { _, _, _, _ -> },
        onAutoAssign = {},
        onClearAll = {},
        onExportPdf = {}
    )
}

@Composable
fun ClassStatisticsView(
    students: List<StudentEntity>,
    grades: List<com.example.data.StudentWithGrade>,
    activeClass: ClassEntity?,
    currentSettings: com.example.data.SettingsEntity?,
    onExportPdf: (() -> Unit)? = null
) {
    if (grades.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (Tr.isArabic()) "لا توجد بيانات متاحة" else "Aucune donnée disponible",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (Tr.isArabic()) "أضف التلاميذ وسجل نقاطهم لعرض التحليلات الإحصائية." else "Ajoutez des élèves et saisissez leurs notes pour afficher les analyses statistiques.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
        return
    }

    val classCoeff = activeClass?.subjectCoefficient ?: currentSettings?.subjectCoefficient ?: 3
    val isPrimaryApp = false
    val isAr = Tr.isArabic()

    // 1. Calculations
    val totalCount = grades.size
    val boysCount = grades.count { it.gender.equals("M", ignoreCase = true) }
    val girlsCount = grades.count { it.gender.equals("F", ignoreCase = true) }
    val boysPercentage = if (totalCount > 0) (boysCount * 100f / totalCount) else 0f
    val girlsPercentage = if (totalCount > 0) (girlsCount * 100f / totalCount) else 0f

    // Ages
    val ages = grades.mapNotNull { calculateAge(it.birthDate) }
    val under12 = ages.count { it < 12 }
    val between12and15 = ages.count { it in 12..15 }
    val above15 = ages.count { it > 15 }

    // Pyramide des âges calculation
    val studentsWithAge = grades.mapNotNull { g ->
        val age = calculateAge(g.birthDate)
        if (age != null && age in 2..100) Pair(age, g.gender) else null
    }
    val distinctAges = if (studentsWithAge.isNotEmpty()) {
        val minAge = studentsWithAge.minOf { it.first }
        val maxAge = studentsWithAge.maxOf { it.first }
        (minAge..maxAge).toList().sortedDescending()
    } else emptyList()

    val maxStudentsInAnyAgeRow = if (distinctAges.isNotEmpty()) {
        distinctAges.maxOf { age ->
            val b = studentsWithAge.count { it.first == age && it.second.equals("M", ignoreCase = true) }
            val g = studentsWithAge.count { it.first == age && it.second.equals("F", ignoreCase = true) }
            maxOf(b, g, 1)
        }
    } else 1

    // Grades using GradeCalculator to support Project, Devoirs, and Composition with subjectCoefficient & isPrimary
    val averages = grades.mapNotNull { g ->
        GradeCalculator.computeStudentAverage(g, classCoeff, isPrimary = isPrimaryApp)
    }

    val maxScale = if (isPrimaryApp) 10f else 20f
    val passingThreshold = if (isPrimaryApp) 5f else 10f
    val classAverage = if (averages.isNotEmpty()) averages.average().toFloat() else null
    val maxAverage = if (averages.isNotEmpty()) averages.maxOrNull() else null
    val minAverage = if (averages.isNotEmpty()) averages.minOrNull() else null
    val passedCount = averages.count { it >= passingThreshold }
    val failedCount = averages.count { it < passingThreshold }
    val successRate = if (averages.isNotEmpty()) (passedCount * 100f / averages.size) else 0f

    // Ranges for Bar Chart (Primary /10 vs CEM/Lycée /20)
    val (r1, r2, r3, r4, r5) = if (isPrimaryApp) {
        listOf(
            averages.count { it < 2.5f },
            averages.count { it in 2.5f..4.99f },
            averages.count { it in 5.0f..6.99f },
            averages.count { it in 7.0f..8.49f },
            averages.count { it >= 8.5f }
        )
    } else {
        listOf(
            averages.count { it < 5f },
            averages.count { it in 5.0f..9.99f },
            averages.count { it in 10.0f..13.99f },
            averages.count { it in 14.0f..15.99f },
            averages.count { it >= 16.0f }
        )
    }
    val maxRangeCount = maxOf(1, r1, r2, r3, r4, r5)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // CARD 1: KPI Summary
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAr) "ملخص الفوج" else "Résumé de la Classe",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = if (isAr) "العدد" else "Effectif",
                        value = totalCount.toString(),
                        subtitle = if (isAr) (if (totalCount <= 1) "تلميذ" else "تلاميذ") else (if (totalCount <= 1) "élève" else "élèves"),
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = if (isAr) "المعدل" else "Moyenne",
                        value = if (classAverage != null) "%.2f".format(classAverage) else "-",
                        subtitle = if (isPrimaryApp) "/ 10" else "/ 20",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = if (isAr) "أعلى معدل" else "Note Max",
                        value = if (maxAverage != null) "%.2f".format(maxAverage) else "-",
                        subtitle = if (isAr) "الأفضل" else "Meilleur résultat",
                        modifier = Modifier.weight(1f),
                        valueColor = Color(0xFF10B981)
                    )
                    KpiCard(
                        title = if (isAr) "أدنى معدل" else "Note Min",
                        value = if (minAverage != null) "%.2f".format(minAverage) else "-",
                        subtitle = if (isAr) "الأدنى" else "Moins bon résultat",
                        modifier = Modifier.weight(1f),
                        valueColor = Color(0xFFEF4444)
                    )
                }
            }
        }

        // CARD 2: Success Rate Donut Chart
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isAr) "نسبة النجاح" else "Statistiques de Réussite",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (averages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isAr) "لا توجد نقاط مسجلة لهذا الفصل." else "Aucune note saisie pour ce trimestre.",
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // Donut representation
                        Box(
                            modifier = Modifier.size(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val successColor = Color(0xFF10B981)
                            val failColor = Color(0xFFEF4444)
                            androidx.compose.foundation.Canvas(modifier = Modifier.size(90.dp)) {
                                val strokeWidth = 12.dp.toPx()
                                drawArc(
                                    color = failColor.copy(alpha = 0.15f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                                )
                                drawArc(
                                    color = successColor,
                                    startAngle = -90f,
                                    sweepAngle = successRate * 3.6f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${successRate.toInt()}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isAr) "ناجح" else "Admis",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Labels / Legend
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val passLabel = if (isPrimaryApp) (if (isAr) "ناجح (≥ 5)" else "Admis (≥ 5)") else (if (isAr) "ناجح (≥ 10)" else "Admis (≥ 10)")
                            val failLabel = if (isPrimaryApp) (if (isAr) "غير ناجح (< 5)" else "Non-admis (< 5)") else (if (isAr) "غير ناجح (< 10)" else "Non-admis (< 10)")
                            LegendRow(color = Color(0xFF10B981), label = passLabel, count = passedCount, percentage = successRate)
                            LegendRow(color = Color(0xFFEF4444), label = failLabel, count = failedCount, percentage = 100f - successRate)
                        }
                    }
                }
            }
        }

        // CARD 3: Grades Distribution Bar Chart
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isAr) "توزيع المعدلات" else "Répartition des Moyennes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (averages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (Tr.isArabic()) "لا توجد معدلات متاحة." else "Aucune moyenne disponible.",
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        if (isPrimaryApp) {
                            BarItem(label = "< 2.5", count = r1, maxCount = maxRangeCount, color = Color(0xFFEF4444))
                            BarItem(label = "2.5-5", count = r2, maxCount = maxRangeCount, color = Color(0xFFF97316))
                            BarItem(label = "5-7", count = r3, maxCount = maxRangeCount, color = Color(0xFF0EA5E9))
                            BarItem(label = "7-8.5", count = r4, maxCount = maxRangeCount, color = Color(0xFF6366F1))
                            BarItem(label = "8.5-10", count = r5, maxCount = maxRangeCount, color = Color(0xFF10B981))
                        } else {
                            BarItem(label = "< 5", count = r1, maxCount = maxRangeCount, color = Color(0xFFEF4444))
                            BarItem(label = "5-10", count = r2, maxCount = maxRangeCount, color = Color(0xFFF97316))
                            BarItem(label = "10-14", count = r3, maxCount = maxRangeCount, color = Color(0xFF0EA5E9))
                            BarItem(label = "14-16", count = r4, maxCount = maxRangeCount, color = Color(0xFF6366F1))
                            BarItem(label = "16-20", count = r5, maxCount = maxRangeCount, color = Color(0xFF10B981))
                        }
                    }
                }
            }
        }

        // CARD 4: Pyramide des Âges (Age Pyramid)
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAr) "هرم الأعمار" else "Pyramide des Âges",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val avgAge = if (studentsWithAge.isNotEmpty()) studentsWithAge.map { it.first }.average() else null
                    if (avgAge != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isAr) "معدل العمر" else "Âge moyen",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isAr) "%.1f سنة".format(avgAge) else "%.1f ans".format(avgAge),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Legend: Boys (Left) vs Girls (Right)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF0284C7)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAr) "ذكور ♂" else "Garçons ♂",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isAr) "إناث ♀" else "Filles ♀",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEC4899)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFEC4899)))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (distinctAges.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isAr) "يرجى تسجيل تواريخ ميلاد التلاميذ لعرض هرم الأعمار." else "Veuillez renseigner les dates de naissance des élèves pour afficher la pyramide des âges.",
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            distinctAges.forEach { age ->
                                val bCount = studentsWithAge.count { it.first == age && it.second.equals("M", ignoreCase = true) }
                                val gCount = studentsWithAge.count { it.first == age && it.second.equals("F", ignoreCase = true) }
                                val bRatio = if (maxStudentsInAnyAgeRow > 0) bCount.toFloat() / maxStudentsInAnyAgeRow else 0f
                                val gRatio = if (maxStudentsInAnyAgeRow > 0) gCount.toFloat() / maxStudentsInAnyAgeRow else 0f

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left side: Boys bar extending towards center
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (bCount > 0) {
                                            Text(
                                                text = bCount.toString(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0284C7),
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f, fill = false),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(bRatio.coerceIn(0.12f, 1f))
                                                        .height(20.dp)
                                                        .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                                                        .background(Color(0xFF0284C7))
                                                )
                                            }
                                        } else {
                                            Box(modifier = Modifier.height(20.dp))
                                        }
                                    }

                                    // Center: Age badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isAr) "$age سنة" else "$age ans",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Right side: Girls bar extending from center rightwards
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (gCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f, fill = false),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(gRatio.coerceIn(0.12f, 1f))
                                                        .height(20.dp)
                                                        .clip(RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                                                        .background(Color(0xFFEC4899))
                                                )
                                            }
                                            Text(
                                                text = gCount.toString(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFEC4899),
                                                modifier = Modifier.padding(start = 4.dp)
                                            )
                                        } else {
                                            Box(modifier = Modifier.height(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // CARD 5: Demographic slices & Gender Progress
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = Tr.t("Profil Démographique"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Gender progress bar
                Text(
                    text = Tr.t("Répartition par Genre"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (boysCount > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(if (boysPercentage > 0) boysPercentage else 0.01f)
                                .background(Color(0xFF0284C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "♂ ${boysPercentage.toInt()}%",
                                color = Color.White,
                                fontSize = if (Tr.isArabic()) 13.5.sp else 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (girlsCount > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(if (girlsPercentage > 0) girlsPercentage else 0.01f)
                                .background(Color(0xFFEC4899)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "♀ ${girlsPercentage.toInt()}%",
                                color = Color.White,
                                fontSize = if (Tr.isArabic()) 13.5.sp else 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Age Distribution
                Text(
                    text = Tr.t("Tranches d'Âge"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AgeKpi(label = if (Tr.isArabic()) "< 12 سنة" else "< 12 ans", count = under12, modifier = Modifier.weight(1f))
                    AgeKpi(label = if (Tr.isArabic()) "12 - 15 سنة" else "12 - 15 ans", count = between12and15, modifier = Modifier.weight(1f))
                    AgeKpi(label = if (Tr.isArabic()) "> 15 سنة" else "> 15 ans", count = above15, modifier = Modifier.weight(1f))
                }
            }
        }

        // CARD 6: Component averages details (Omit evaluation details in Primary if desired or show available components)
        val evalContinuousGrades = grades.mapNotNull { g ->
            if (g.participation != null || g.discipline != null || g.assiduity != null || g.notebook != null) {
                (g.participation ?: 0f) + (g.discipline ?: 0f) + (g.assiduity ?: 0f) + (g.notebook ?: 0f)
            } else null
        }
        val avgEvalContinuous = if (evalContinuousGrades.isNotEmpty()) evalContinuousGrades.average().toFloat() else 0f

        val participations = grades.mapNotNull { it.participation }
        val avgParticipation = if (participations.isNotEmpty()) participations.average().toFloat() else 0f

        val disciplines = grades.mapNotNull { it.discipline }
        val avgDiscipline = if (disciplines.isNotEmpty()) disciplines.average().toFloat() else 0f

        val assiduitis = grades.mapNotNull { it.assiduity }
        val avgAssiduity = if (assiduitis.isNotEmpty()) assiduitis.average().toFloat() else 0f

        val notebooks = grades.mapNotNull { it.notebook }
        val avgNotebook = if (notebooks.isNotEmpty()) notebooks.average().toFloat() else 0f

        val exams = grades.flatMap { listOfNotNull(it.exam1, it.exam2, it.exam3) }
        val avgExams = if (exams.isNotEmpty()) exams.average().toFloat() else 0f

        val finalExams = grades.mapNotNull { it.finalExam }
        val avgFinalExam = if (finalExams.isNotEmpty()) finalExams.average().toFloat() else 0f

        val projects = grades.mapNotNull { it.project }
        val avgProject = if (projects.isNotEmpty()) projects.average().toFloat() else 0f

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = Tr.t("Moyennes par Composante d'Évaluation"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!isPrimaryApp) {
                        ComponentProgressRow(
                            label = if (isAr) "التقويم المستمر (المجموع على 20)" else "Évaluation Continue (Total sur 20)",
                            averageValue = avgEvalContinuous,
                            color = Color(0xFF6366F1),
                            maxScale = 20f
                        )
                        ComponentProgressRow(
                            label = if (isAr) "المشاركة الشفوية (على 5)" else "Participation (sur 5)",
                            averageValue = avgParticipation,
                            color = Color(0xFF8B5CF6),
                            maxScale = 5f
                        )
                        ComponentProgressRow(
                            label = if (isAr) "الانضباط والسلوك (على 5)" else "Discipline & Comportement (sur 5)",
                            averageValue = avgDiscipline,
                            color = Color(0xFFEF4444),
                            maxScale = 5f
                        )
                        ComponentProgressRow(
                            label = if (isAr) "المواظبة والحضور (على 5)" else "Assiduité & Présence (sur 5)",
                            averageValue = avgAssiduity,
                            color = Color(0xFFF59E0B),
                            maxScale = 5f
                        )
                        ComponentProgressRow(
                            label = if (isAr) "الكراس والواجبات (على 5)" else "Cahier & Devoirs (sur 5)",
                            averageValue = avgNotebook,
                            color = Color(0xFF10B981),
                            maxScale = 5f
                        )
                    }
                    ComponentProgressRow(label = Tr.t("Projet Pédagogique"), averageValue = avgProject, color = Color(0xFF14B8A6), maxScale = maxScale)
                    ComponentProgressRow(label = if (isPrimaryApp) (if (isAr) "الاختبارات والفروض" else "Devoirs / Tests") else Tr.t("Devoirs / Évaluations"), averageValue = avgExams, color = Color(0xFF3B82F6), maxScale = maxScale)
                    ComponentProgressRow(label = if (isPrimaryApp) (if (isAr) "الاختبار الفصلي (Composition)" else "Composition") else Tr.t("Composition"), averageValue = avgFinalExam, color = Color(0xFFEC4899), maxScale = maxScale)
                }
            }
        }

        // CARD 7: Success & Performance comparison by Gender
        val boysGrades = grades.filter { it.gender.equals("M", ignoreCase = true) }
        val girlsGrades = grades.filter { it.gender.equals("F", ignoreCase = true) }

        val boysAverages = boysGrades.mapNotNull { GradeCalculator.computeStudentAverage(it, classCoeff, isPrimary = isPrimaryApp) }
        val girlsAverages = girlsGrades.mapNotNull { GradeCalculator.computeStudentAverage(it, classCoeff, isPrimary = isPrimaryApp) }

        val boysAvgScore = if (boysAverages.isNotEmpty()) boysAverages.average().toFloat() else null
        val girlsAvgScore = if (girlsAverages.isNotEmpty()) girlsAverages.average().toFloat() else null

        val boysPassed = boysAverages.count { it >= passingThreshold }
        val boysSR = if (boysAverages.isNotEmpty()) (boysPassed * 100f / boysAverages.size) else 0f

        val girlsPassed = girlsAverages.count { it >= passingThreshold }
        val girlsSR = if (girlsAverages.isNotEmpty()) (girlsPassed * 100f / girlsAverages.size) else 0f

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = Tr.t("Performance comparative par Genre"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Boys Performance
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.2.dp, Color(0xFF0284C7).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .background(Color(0xFF0284C7).copy(alpha = 0.06f))
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = Tr.t("Garçons ♂"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (boysAvgScore != null) "%.2f".format(boysAvgScore) else "-",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0284C7)
                            )
                            if (boysAvgScore != null) {
                                Text(
                                    text = if (isPrimaryApp) " /10" else " /20",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0284C7).copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = Tr.t("Moyenne générale"),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$boysPassed / ${boysAverages.size} " + (if (Tr.isArabic()) "ناجح" else "admis"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0369A1),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { boysSR / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = Color(0xFF0284C7),
                            trackColor = Color(0xFF0284C7).copy(alpha = 0.15f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = (if (Tr.isArabic()) "نسبة النجاح : " else "Réussite: ") + "${boysSR.toInt()}%",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7),
                            maxLines = 1
                        )
                    }

                    // Girls Performance
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.2.dp, Color(0xFFEC4899).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .background(Color(0xFFEC4899).copy(alpha = 0.06f))
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = Tr.t("Filles ♀"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEC4899),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (girlsAvgScore != null) "%.2f".format(girlsAvgScore) else "-",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFEC4899)
                            )
                            if (girlsAvgScore != null) {
                                Text(
                                    text = if (isPrimaryApp) " /10" else " /20",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFEC4899).copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = Tr.t("Moyenne générale"),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$girlsPassed / ${girlsAverages.size} " + (if (Tr.isArabic()) "ناجحة" else "admises"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFBE185D),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { girlsSR / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = Color(0xFFEC4899),
                            trackColor = Color(0xFFEC4899).copy(alpha = 0.15f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = (if (Tr.isArabic()) "نسبة النجاح : " else "Réussite: ") + "${girlsSR.toInt()}%",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEC4899),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Espace au bas pour garantir un affichage complet sur tous les appareils
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun ComponentProgressRow(label: String, averageValue: Float, color: Color, maxScale: Float = 20f) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(
                text = "%.2f / %.0f".format(averageValue, maxScale),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (averageValue / maxScale).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}

@Composable
fun LegendRow(color: Color, label: String, count: Int, percentage: Float) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            val unitStr = if (Tr.isArabic()) (if (count <= 1) "تلميذ" else "تلاميذ") else (if (count <= 1) "élève" else "élèves")
            Text(
                text = "$count $unitStr (${percentage.toInt()}%)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun BarItem(label: String, count: Int, maxCount: Int, color: Color) {
    val barHeightRatio = count.toFloat() / maxCount
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.fillMaxHeight()
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(maxOf(6.dp, 120.dp * barHeightRatio))
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.primary
) {
    GlassmorphicCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.3.sp,
                color = valueColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AgeKpi(label: String, count: Int, modifier: Modifier = Modifier) {
    GlassmorphicCard(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = 1.5.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 9.5.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (Tr.isArabic()) "$count ${if (count <= 1) "تلميذ" else "تلاميذ"}" else "$count élève${if (count > 1) "s" else ""}",
                fontSize = if (Tr.isArabic()) 12.5.sp else 11.5.sp,
                maxLines = 1,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

