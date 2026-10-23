package com.example.ui.screens

import android.widget.Toast

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.background

import androidx.compose.foundation.border

import androidx.compose.foundation.clickable

import androidx.compose.foundation.horizontalScroll

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.LazyRow

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.lazy.itemsIndexed

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.*

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.draw.shadow

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.platform.testTag

import androidx.compose.ui.res.stringResource

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import com.example.ui.ClassColorHelper

import com.example.ui.ClassColorDot

import androidx.compose.ui.unit.sp

import com.example.R

import com.example.data.StudentWithGrade

import com.example.util.GradeCalculator

import com.example.ui.AppViewModel

import com.example.ui.components.GlassmorphicCard

import com.example.ui.components.FineSearchBar

import com.example.ui.theme.OffWhite

import com.example.util.Tr

import com.example.util.tr

import com.example.util.AdaptiveScreenContainer

import com.example.util.adaptiveDialogMaxHeight

import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.result.contract.ActivityResultContracts

import android.net.Uri

import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.ui.text.input.ImeAction

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight

@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun GradebookScreen(viewModel: AppViewModel) {

    val context = LocalContext.current.applicationContext

    val classes by viewModel.allClasses.collectAsState()

    val selectedClassId by viewModel.selectedClassId.collectAsState()

    val selectedTerm by viewModel.selectedTerm.collectAsState()

    val gradesList by viewModel.currentGrades.collectAsState()

    val currentSettings by viewModel.settings.collectAsState()

    val activeClass = classes.find { it.id == selectedClassId }

    val isAr = Tr.isArabic()

    val isPrimary = false

    // Dialog state for editing a student's grades

    var studentToGrade by remember { mutableStateOf<StudentWithGrade?>(null) }

    var showClassSummaryDialog by remember { mutableStateOf(false) }

    var showComparisonDialog by remember { mutableStateOf(false) }

    var term1Grades by remember { mutableStateOf<List<StudentWithGrade>>(emptyList()) }

    var term2Grades by remember { mutableStateOf<List<StudentWithGrade>>(emptyList()) }

    var term3Grades by remember { mutableStateOf<List<StudentWithGrade>>(emptyList()) }

    var isHistoryLoading by remember { mutableStateOf(false) }

    var classAverages by remember { mutableStateOf<Map<Int, Triple<Float?, Float?, Float?>>>(emptyMap()) }

    LaunchedEffect(showComparisonDialog, activeClass) {

        if (showComparisonDialog && activeClass != null) {

            isHistoryLoading = true

            val t1 = viewModel.getStudentsWithGradesForTermList(activeClass.id, 1)

            val t2 = viewModel.getStudentsWithGradesForTermList(activeClass.id, 2)

            val t3 = viewModel.getStudentsWithGradesForTermList(activeClass.id, 3)

            term1Grades = t1

            term2Grades = t2

            term3Grades = t3

            val avgsMap = mutableMapOf<Int, Triple<Float?, Float?, Float?>>()

            classes.forEach { cls ->

                val clsT1 = viewModel.getStudentsWithGradesForTermList(cls.id, 1)

                val clsT2 = viewModel.getStudentsWithGradesForTermList(cls.id, 2)

                val clsT3 = viewModel.getStudentsWithGradesForTermList(cls.id, 3)

                val c1Coeff = cls.subjectCoefficient

                val clsT1Avgs = clsT1.mapNotNull { computeStudentAverage(it, c1Coeff, isPrimary) }

                val clsT2Avgs = clsT2.mapNotNull { computeStudentAverage(it, c1Coeff, isPrimary) }

                val clsT3Avgs = clsT3.mapNotNull { computeStudentAverage(it, c1Coeff, isPrimary) }

                val avgT1 = if (clsT1Avgs.isNotEmpty()) clsT1Avgs.average().toFloat() else null

                val avgT2 = if (clsT2Avgs.isNotEmpty()) clsT2Avgs.average().toFloat() else null

                val avgT3 = if (clsT3Avgs.isNotEmpty()) clsT3Avgs.average().toFloat() else null

                avgsMap[cls.id] = Triple(avgT1, avgT2, avgT3)

            }

            classAverages = avgsMap

            isHistoryLoading = false

        }

    }

    LaunchedEffect(classes, selectedClassId) {

        if (selectedClassId == null && classes.isNotEmpty()) {

            viewModel.selectClass(classes.first().id)

        }

    }

    var studentSearchQuery by remember { mutableStateOf("") }

    val filteredGrades = remember(gradesList, studentSearchQuery) {

        val list = if (studentSearchQuery.isBlank()) gradesList

        else {

            val q = studentSearchQuery.trim().lowercase()

            gradesList.filter {

                it.firstName.lowercase().contains(q) || it.lastName.lowercase().contains(q)

            }

        }

        list.sortedWith(

            compareBy<StudentWithGrade> { if (it.orderIndex > 0) it.orderIndex else Int.MAX_VALUE }

                .thenBy { it.lastName.lowercase() }

                .thenBy { it.firstName.lowercase() }

        )

    }

    val currentActiveClass by rememberUpdatedState(activeClass)
    val currentSelectedTerm by rememberUpdatedState(selectedTerm)
    val currentGradesListSnapshot by rememberUpdatedState(gradesList)

    val gradebookPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let {
            val targetCls = currentActiveClass
            if (targetCls != null) {
                viewModel.exportSingleGradebookPdf(
                    context = context,
                    targetUri = it,
                    className = targetCls.name,
                    term = currentSelectedTerm,
                    gradesList = currentGradesListSnapshot.sortedWith(
                        compareBy<StudentWithGrade> { if (it.orderIndex > 0) it.orderIndex else Int.MAX_VALUE }
                            .thenBy { it.lastName.lowercase() }
                            .thenBy { it.firstName.lowercase() }
                    )
                )
            }
        }
    }

    AdaptiveScreenContainer(maxWidth = 960.dp) {

        LazyColumn(

            modifier = Modifier

                .fillMaxSize()

                .testTag("gradebook_screen_scroll"),

            contentPadding = PaddingValues(16.dp),

            verticalArrangement = Arrangement.spacedBy(12.dp)

        ) {

        // --- 1. SÉLECTION FUSIONNÉE : CLASSE & TRIMESTRE (GAIN D'ESPACE MAXIMAL) ---

        item {

            var classDropdownExpanded by remember { mutableStateOf(false) }

            val isLight = MaterialTheme.colorScheme.background == OffWhite || MaterialTheme.colorScheme.background == Color(0xFFF8FAFC)

            GlassmorphicCard(

                modifier = Modifier

                    .fillMaxWidth()

                    .testTag("gradebook_merged_header_card"),

                shape = RoundedCornerShape(18.dp),

                elevation = 2.dp,

                borderWidth = 1.1.dp,

                glassAlpha = if (isLight) 0.88f else 0.82f

            ) {

                Column(

                    modifier = Modifier

                        .fillMaxWidth()

                        .padding(12.dp),

                    verticalArrangement = Arrangement.spacedBy(10.dp)

                ) {

                    // Ligne 1 : Sélecteur de classe

                    Box(modifier = Modifier.fillMaxWidth()) {

                        Surface(

                            onClick = { classDropdownExpanded = true },

                            modifier = Modifier

                                .fillMaxWidth()

                                .heightIn(min = 46.dp)

                                .testTag("class_dropdown_selector"),

                            shape = RoundedCornerShape(12.dp),

                            color = if (isLight) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            } else {
                                Color(0xFF1E293B).copy(alpha = 0.90f)
                            },

                            border = BorderStroke(

                                width = 1.dp,

                                color = if (classDropdownExpanded) {
                                    if (isLight) MaterialTheme.colorScheme.primary else Color(0xFF60A5FA)
                                } else {
                                    if (isLight) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f) else Color(0xFF475569)
                                }

                            )

                        ) {

                            Row(

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .padding(horizontal = 12.dp, vertical = 8.dp),

                                verticalAlignment = Alignment.CenterVertically,

                                horizontalArrangement = Arrangement.SpaceBetween

                            ) {

                                Row(

                                    verticalAlignment = Alignment.CenterVertically,

                                    horizontalArrangement = Arrangement.spacedBy(10.dp),

                                    modifier = Modifier.weight(1f)

                                ) {

                                    val activePalette = if (activeClass != null) ClassColorHelper.getPaletteForClass(activeClass.id, activeClass.name) else null

                                    Box(

                                        modifier = Modifier

                                            .size(32.dp)

                                            .clip(RoundedCornerShape(8.dp))

                                            .background(

                                                if (activePalette != null) {
                                                    if (isLight) activePalette.container.copy(alpha = 0.7f)
                                                    else activePalette.primary.copy(alpha = 0.25f)
                                                } else {
                                                    if (isLight) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                                    else Color(0xFF334155).copy(alpha = 0.6f)
                                                }

                                            ),

                                        contentAlignment = Alignment.Center

                                    ) {

                                        if (activeClass != null) {

                                            ClassColorDot(activeClass.id, activeClass.name, size = 16)

                                        } else {

                                            Icon(

                                                imageVector = Icons.Default.School,

                                                contentDescription = null,

                                                tint = if (isLight) MaterialTheme.colorScheme.outline else Color(0xFF94A3B8),

                                                modifier = Modifier.size(16.dp)

                                            )

                                        }

                                    }

                                    Column {

                                        Text(

                                            text = activeClass?.name ?: "Sélectionner une classe...",

                                            style = MaterialTheme.typography.bodyMedium,

                                            fontWeight = if (activeClass != null) FontWeight.Bold else FontWeight.Normal,

                                            color = if (activeClass != null) {
                                                if (isLight) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                                            } else {
                                                if (isLight) MaterialTheme.colorScheme.outline else Color(0xFF94A3B8)
                                            },

                                            maxLines = 1,

                                            overflow = TextOverflow.Ellipsis

                                        )

                                        if (activeClass != null && activeClass.schoolYear.isNotBlank()) {

                                            Text(

                                                text = "${if (isAr) "السنة :" else "Année :"} ${activeClass.schoolYear} • ${if (isAr) "المعامل :" else "Coeff :"} ${activeClass.subjectCoefficient}",

                                                style = MaterialTheme.typography.labelSmall,

                                                color = if (isLight) Color(0xFF475569) else Color(0xFFCBD5E1)

                                            )

                                        }

                                    }

                                }

                                Surface(

                                    shape = RoundedCornerShape(6.dp),

                                    color = if (isLight) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color(0xFF3B82F6).copy(alpha = 0.22f)

                                ) {

                                    Icon(

                                        imageVector = if (classDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,

                                        contentDescription = "Ouvrir le menu des classes",

                                        tint = if (isLight) MaterialTheme.colorScheme.primary else Color(0xFF93C5FD),

                                        modifier = Modifier.padding(2.dp).size(20.dp)

                                    )

                                }

                            }

                        }

                        // Menu déroulant listant toutes les classes

                        DropdownMenu(

                            expanded = classDropdownExpanded,

                            onDismissRequest = { classDropdownExpanded = false },

                            modifier = Modifier

                                .fillMaxWidth(0.9f)

                                .testTag("class_dropdown_menu")

                        ) {

                            if (classes.isEmpty()) {

                                DropdownMenuItem(

                                    text = { Text(if (Tr.isArabic()) "لا توجد أقسام متاحة" else "Aucune classe disponible") },

                                    onClick = { classDropdownExpanded = false }

                                )

                            } else {

                                classes.forEach { c ->

                                    val isSelected = c.id == selectedClassId

                                    DropdownMenuItem(

                                        text = {

                                            Column {

                                                Text(

                                                    text = c.name,

                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,

                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

                                                )

                                                if (c.schoolYear.isNotBlank()) {

                                                    Text(

                                                        text = c.schoolYear,

                                                        style = MaterialTheme.typography.labelSmall,

                                                        color = MaterialTheme.colorScheme.onSurfaceVariant

                                                    )

                                                }

                                            }

                                        },

                                        leadingIcon = {

                                            Row(verticalAlignment = Alignment.CenterVertically) {

                                                ClassColorDot(c.id, c.name, size = 12)

                                                if (isSelected) {

                                                    Spacer(modifier = Modifier.width(4.dp))

                                                    Icon(

                                                        imageVector = Icons.Default.Check,

                                                        contentDescription = "Sélectionné",

                                                        tint = MaterialTheme.colorScheme.primary,

                                                        modifier = Modifier.size(14.dp)

                                                    )

                                                }

                                            }

                                        },

                                        trailingIcon = {

                                            Surface(

                                                shape = RoundedCornerShape(6.dp),

                                                color = MaterialTheme.colorScheme.surfaceVariant

                                            ) {

                                                Text(

                                                    text = "${if (Tr.isArabic()) "المعامل :" else "Coeff :"} ${c.subjectCoefficient}",

                                                    style = MaterialTheme.typography.labelSmall,

                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),

                                                    color = MaterialTheme.colorScheme.onSurfaceVariant

                                                )

                                            }

                                        },

                                        onClick = {

                                            viewModel.selectClass(c.id)

                                            classDropdownExpanded = false

                                        }

                                    )

                                }

                            }

                        }

                    }

                    // Ligne 2 : Onglets Trimestres T1, T2, T3 compacts

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement = Arrangement.spacedBy(8.dp),

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        for (term in 1..3) {

                            val isSelected = selectedTerm == term

                            val termLabel = if (isAr) "الفصل $term" else "Trimestre $term"

                            Surface(

                                onClick = { viewModel.selectTerm(term) },

                                modifier = Modifier

                                    .weight(1f)

                                    .height(38.dp)

                                    .testTag("term_tab_$term"),

                                shape = RoundedCornerShape(10.dp),

                                color = if (isSelected) {
                                    if (isLight) MaterialTheme.colorScheme.primary else Color(0xFF2563EB)
                                } else {
                                    if (isLight) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f) else Color(0xFF1E293B).copy(alpha = 0.85f)
                                },

                                border = BorderStroke(

                                    width = if (isSelected) 1.5.dp else 1.dp,

                                    color = if (isSelected) {
                                        if (isLight) MaterialTheme.colorScheme.primary else Color(0xFF60A5FA)
                                    } else {
                                        if (isLight) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f) else Color(0xFF475569)
                                    }

                                ),

                                shadowElevation = if (isSelected) 2.dp else 0.dp

                            ) {

                                Box(

                                    modifier = Modifier.fillMaxSize(),

                                    contentAlignment = Alignment.Center

                                ) {

                                    Row(

                                        verticalAlignment = Alignment.CenterVertically,

                                        horizontalArrangement = Arrangement.Center

                                    ) {

                                        if (isSelected) {

                                            Icon(

                                                imageVector = Icons.Default.Check,

                                                contentDescription = null,

                                                tint = Color.White,

                                                modifier = Modifier.size(14.dp)

                                            )

                                            Spacer(Modifier.width(4.dp))

                                        }

                                        Text(

                                            text = termLabel,

                                            style = MaterialTheme.typography.labelMedium,

                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,

                                            color = if (isSelected) Color.White else (if (isLight) Color(0xFF1E293B) else Color(0xFFE2E8F0))

                                        )

                                    }

                                }

                            }

                        }

                    }

                }

            }

        }

        // --- 3. GRADES MATRIX/LIST ---

        if (activeClass == null) {

            item {

                Box(

                    modifier = Modifier

                        .fillMaxWidth()

                        .padding(vertical = 40.dp),

                    contentAlignment = Alignment.Center

                ) {

                    Text(

                        text = "Veuillez sélectionner une classe pour gérer le carnet de notes.",

                        style = MaterialTheme.typography.bodyMedium,

                        color = MaterialTheme.colorScheme.outline,

                        textAlign = TextAlign.Center

                    )

                }

            }

        } else {

            item {
                val isLight = MaterialTheme.colorScheme.background == OffWhite || MaterialTheme.colorScheme.background == Color(0xFFF8FAFC)

                Column(

                    modifier = Modifier.fillMaxWidth(),

                    verticalArrangement = Arrangement.spacedBy(10.dp)

                ) {

                    // En-tête avec titre, badge de classe et indication tactile

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement = Arrangement.SpaceBetween,

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Column {

                            Row(

                                verticalAlignment = Alignment.CenterVertically,

                                horizontalArrangement = Arrangement.spacedBy(8.dp)

                            ) {

                                Text(

                                    text = if (Tr.isArabic()) "نقاط القسم" else "Notes de la classe",

                                    style = MaterialTheme.typography.titleMedium,

                                    fontWeight = FontWeight.Bold,

                                    color = if (isLight) Color(0xFF0F172A) else Color(0xFFF8FAFC)

                                )

                                Surface(

                                    color = if (isLight) Color(0xFFDBEAFE) else Color(0xFF1E3A8A).copy(alpha = 0.85f),

                                    border = BorderStroke(
                                        1.dp,
                                        if (isLight) Color(0xFF93C5FD) else Color(0xFF3B82F6).copy(alpha = 0.65f)
                                    ),

                                    shape = RoundedCornerShape(8.dp)

                                ) {

                                    Text(

                                        text = activeClass.name,

                                        style = MaterialTheme.typography.labelSmall,

                                        fontWeight = FontWeight.Bold,

                                        color = if (isLight) Color(0xFF1D4ED8) else Color(0xFF93C5FD),

                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)

                                    )

                                }

                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Row(

                                verticalAlignment = Alignment.CenterVertically,

                                horizontalArrangement = Arrangement.spacedBy(5.dp)

                            ) {

                                Icon(

                                    imageVector = Icons.Default.TouchApp,

                                    contentDescription = null,

                                    modifier = Modifier.size(15.dp),

                                    tint = if (isLight) Color(0xFF2563EB) else Color(0xFF60A5FA)

                                )

                                Text(

                                    text = if (Tr.isArabic()) "اضغط على تلميذ للتنقيط" else "Tapez sur un élève pour noter",

                                    style = MaterialTheme.typography.bodySmall,

                                    color = if (isLight) Color(0xFF475569) else Color(0xFFCBD5E1),

                                    fontWeight = FontWeight.Medium

                                )

                            }

                        }

                    }

                    // Barre de boutons d'action équilibrée et fluide

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement = Arrangement.spacedBy(8.dp),

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Button(

                            onClick = { showComparisonDialog = true },

                            modifier = Modifier.weight(1f).height(38.dp),

                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),

                            colors = ButtonDefaults.buttonColors(

                                containerColor = if (isLight) Color(0xFFEDE9FE) else Color(0xFF4C1D95).copy(alpha = 0.80f),

                                contentColor = if (isLight) Color(0xFF5B21B6) else Color(0xFFDDD6FE)

                            ),

                            border = BorderStroke(
                                1.dp,
                                if (isLight) Color(0xFFC4B5FD) else Color(0xFF8B5CF6).copy(alpha = 0.65f)
                            ),

                            shape = RoundedCornerShape(10.dp)

                        ) {

                            Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(if (Tr.isArabic()) "مقارنة" else "Comparer", fontSize = 11.sp, fontWeight = FontWeight.Bold)

                        }

                        Button(

                            onClick = { showClassSummaryDialog = true },

                            modifier = Modifier.weight(1f).height(38.dp),

                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),

                            colors = ButtonDefaults.buttonColors(

                                containerColor = if (isLight) Color(0xFFD1FAE5) else Color(0xFF065F46).copy(alpha = 0.80f),

                                contentColor = if (isLight) Color(0xFF065F46) else Color(0xFFA7F3D0)

                            ),

                            border = BorderStroke(
                                1.dp,
                                if (isLight) Color(0xFF6EE7B7) else Color(0xFF10B981).copy(alpha = 0.65f)
                            ),

                            shape = RoundedCornerShape(10.dp)

                        ) {

                            Icon(imageVector = Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(if (Tr.isArabic()) "ملخص" else "Résumé", fontSize = 11.sp, fontWeight = FontWeight.Bold)

                        }

                        Button(

                            onClick = {

                                if (activeClass != null) {

                                    gradebookPdfLauncher.launch("Notes_${activeClass.name}_Trimestre_$selectedTerm.pdf")

                                }

                            },

                            modifier = Modifier.weight(1f).height(38.dp),

                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),

                            colors = ButtonDefaults.buttonColors(

                                containerColor = if (isLight) Color(0xFFDBEAFE) else Color(0xFF1E3A8A).copy(alpha = 0.85f),

                                contentColor = if (isLight) Color(0xFF1E40AF) else Color(0xFFBFDBFE)

                            ),

                            border = BorderStroke(
                                1.dp,
                                if (isLight) Color(0xFF93C5FD) else Color(0xFF3B82F6).copy(alpha = 0.70f)
                            ),

                            shape = RoundedCornerShape(10.dp)

                        ) {

                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = if (Tr.isArabic()) "تصدير PDF" else "Exporter PDF", modifier = Modifier.size(16.dp))

                            Spacer(modifier = Modifier.width(4.dp))

                            Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)

                        }

                    }

                }

            }

            // Barre de recherche fine dans le carnet

            item {

                FineSearchBar(

                    value = studentSearchQuery,

                    onValueChange = { studentSearchQuery = it },

                    placeholder = if (Tr.isArabic()) "البحث عن تلميذ بالاسم..." else "Rechercher un élève par nom...",

                    testTag = "gradebook_student_search",

                    modifier = Modifier.fillMaxWidth()

                )

            }

            if (filteredGrades.isEmpty()) {

                item {

                    Box(

                        modifier = Modifier

                            .fillMaxWidth()

                            .padding(vertical = 32.dp),

                        contentAlignment = Alignment.Center

                    ) {

                        Text(

                            text = if (studentSearchQuery.isNotBlank()) {

                                if (Tr.isArabic()) "لا يوجد أي تلميذ يطابق \"$studentSearchQuery\"." else "Aucun élève ne correspond à \"$studentSearchQuery\"."

                            } else {

                                if (Tr.isArabic()) "لا يوجد تلاميذ مسجلون في هذا القسم حالياً." else "Aucun élève enregistré dans cette classe pour le moment."

                            },

                            style = MaterialTheme.typography.bodyMedium,

                            color = MaterialTheme.colorScheme.outline,

                            textAlign = TextAlign.Center

                        )

                    }

                }

            } else {

                val defaultCoeff = currentSettings?.subjectCoefficient?.toInt() ?: 1

                itemsIndexed(filteredGrades, key = { _, grade -> grade.studentId }) { index, grade ->

                    val studentNum = if (grade.orderIndex > 0) grade.orderIndex else (index + 1)

                    GradeItemCard(

                        number = studentNum,

                        grade = grade,

                        subjectCoefficient = activeClass?.subjectCoefficient ?: defaultCoeff,

                        isPrimary = isPrimary

                    ) {

                        studentToGrade = grade

                    }

                }

            }

        }

    }

    }

    // --- GRADING DIALOG ---

    if (studentToGrade != null) {

        val editingGrade = studentToGrade!!

        var participation by remember { mutableStateOf(editingGrade.participation?.toString() ?: "") }

        var discipline by remember { mutableStateOf(editingGrade.discipline?.toString() ?: "") }

        var assiduity by remember { mutableStateOf(editingGrade.assiduity?.toString() ?: "") }

        var notebook by remember { mutableStateOf(editingGrade.notebook?.toString() ?: "") }

        var project by remember { mutableStateOf(editingGrade.project?.toString() ?: "") }

        var exam1 by remember { mutableStateOf(editingGrade.exam1?.toString() ?: "") }

        var exam2 by remember { mutableStateOf(editingGrade.exam2?.toString() ?: "") }

        var exam3 by remember { mutableStateOf(editingGrade.exam3?.toString() ?: "") }

        var finalExam by remember { mutableStateOf(editingGrade.finalExam?.toString() ?: "") }

        val pVal = if (isPrimary) null else GradeCalculator.parseGradeInput(participation)

        val dVal = if (isPrimary) null else GradeCalculator.parseGradeInput(discipline)

        val aVal = if (isPrimary) null else GradeCalculator.parseGradeInput(assiduity)

        val nVal = if (isPrimary) null else GradeCalculator.parseGradeInput(notebook)

        val projVal = GradeCalculator.parseGradeInput(project)

        val ex1Val = GradeCalculator.parseGradeInput(exam1)

        val ex2Val = GradeCalculator.parseGradeInput(exam2)

        val ex3Val = GradeCalculator.parseGradeInput(exam3)

        val fVal = GradeCalculator.parseGradeInput(finalExam)

        val evalTotal = if (isPrimary) null else GradeCalculator.computeEvaluationSum(pVal, dVal, aVal, nVal)

        val activeCoeff = activeClass?.subjectCoefficient ?: currentSettings?.subjectCoefficient?.toInt() ?: 1

        val liveAvg = GradeCalculator.computeTrimestrialAverage(

            participation = pVal,

            discipline = dVal,

            assiduity = aVal,

            notebook = nVal,

            exam1 = ex1Val,

            exam2 = ex2Val,

            exam3 = ex3Val,

            project = projVal,

            finalExam = fVal,

            subjectCoefficient = activeCoeff,

            isPrimary = isPrimary

        )

        val maxScaleStr = if (isPrimary) "/ 10" else "/ 20"

        val passingThreshold = if (isPrimary) 5f else 10f

        AlertDialog(

            onDismissRequest = { studentToGrade = null },

            title = {

                Text(

                    text = "${if (Tr.isArabic()) "تنقيط :" else "Noter :"} ${editingGrade.lastName.uppercase()} ${editingGrade.firstName}",

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

                    // Pour le primaire : suppression des cases évaluation continue (Non applicable au primaire)

                    if (!isPrimary) {

                        item {

                            Text(

                                text = if (Tr.isArabic()) "1. التقويم المستمر (الانضباط، المشاركة، المواظبة، الكراس)" else "1. Évaluation Continue (Discipline, Participation, Assiduité, Cahier)",

                                fontWeight = FontWeight.Bold,

                                color = MaterialTheme.colorScheme.primary,

                                style = MaterialTheme.typography.bodySmall

                            )

                        }

                        item {

                            val clamp5: (String) -> String = { s ->

                                val n = com.example.util.GradeCalculator.parseGradeInput(s)

                                if (n != null && n > 5f) "5" else s

                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                                OutlinedTextField(

                                    value = participation,

                                    onValueChange = { participation = clamp5(it) },

                                    label = { Text(if (Tr.isArabic()) "مشاركة" else "Partic.") },

                                    singleLine = true,

                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),

                                    modifier = Modifier.weight(1f)

                                )

                                OutlinedTextField(

                                    value = discipline,

                                    onValueChange = { discipline = clamp5(it) },

                                    label = { Text(if (Tr.isArabic()) "انضباط" else "Discip.") },

                                    singleLine = true,

                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),

                                    modifier = Modifier.weight(1f)

                                )

                            }

                        }

                        item {

                            val clamp5: (String) -> String = { s ->

                                val n = com.example.util.GradeCalculator.parseGradeInput(s)

                                if (n != null && n > 5f) "5" else s

                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                                OutlinedTextField(

                                    value = assiduity,

                                    onValueChange = { assiduity = clamp5(it) },

                                    label = { Text(if (Tr.isArabic()) "مواظبة" else "Assid.") },

                                    singleLine = true,

                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),

                                    modifier = Modifier.weight(1f)

                                )

                                OutlinedTextField(

                                    value = notebook,

                                    onValueChange = { notebook = clamp5(it) },

                                    label = { Text(if (Tr.isArabic()) "كراس" else "Cahier") },

                                    singleLine = true,

                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),

                                    modifier = Modifier.weight(1f)

                                )

                            }

                        }

                        // Affichage dynamique du total des notes d'évaluation

                        item {

                            Surface(

                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),

                                shape = RoundedCornerShape(10.dp),

                                modifier = Modifier.fillMaxWidth()

                            ) {

                                Row(

                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),

                                    horizontalArrangement = Arrangement.SpaceBetween,

                                    verticalAlignment = Alignment.CenterVertically

                                ) {

                                    Row(verticalAlignment = Alignment.CenterVertically) {

                                        Icon(

                                            imageVector = Icons.Default.Calculate,

                                            contentDescription = null,

                                            modifier = Modifier.size(18.dp),

                                            tint = MaterialTheme.colorScheme.primary

                                        )

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Text(

                                            text = if (Tr.isArabic()) "مجموع التقويم :" else "Total de l'évaluation :",

                                            style = MaterialTheme.typography.bodySmall,

                                            fontWeight = FontWeight.SemiBold,

                                            color = MaterialTheme.colorScheme.onSurface

                                        )

                                    }

                                    Text(

                                        text = if (evalTotal != null) "%.2f / 20".format(evalTotal) else "- / 20",

                                        style = MaterialTheme.typography.titleSmall,

                                        fontWeight = FontWeight.Bold,

                                        color = MaterialTheme.colorScheme.primary

                                    )

                                }

                            }

                        }

                    }

                    item {

                        Text(

                            text = if (isPrimary) {

                                if (Tr.isArabic()) "1. المشروع (على 10)" else "1. Projet (sur 10)"

                            } else {

                                if (Tr.isArabic()) "2. المشروع (معامل 1)" else "2. Projet (Coeff. 1)"

                            },

                            fontWeight = FontWeight.Bold,

                            color = MaterialTheme.colorScheme.primary,

                            style = MaterialTheme.typography.bodySmall

                        )

                    }

                    item {

                        OutlinedTextField(

                            value = project,

                            onValueChange = { project = it },

                            label = { Text(if (Tr.isArabic()) "علامة المشروع ($maxScaleStr)" else "Note du projet ($maxScaleStr)") },

                            singleLine = true,

                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),

                            modifier = Modifier.fillMaxWidth()

                        )

                    }

                    item {

                        Text(

                            text = if (isPrimary) {

                                if (Tr.isArabic()) "2. الفروض والتقويمات (على 10)" else "2. Devoirs / Tests (sur 10)"

                            } else {

                                if (Tr.isArabic()) "3. الفروض / الاختبارات (معامل 1)" else "3. Devoirs / Examens (Coeff. 1)"

                            },

                            fontWeight = FontWeight.Bold,

                            color = MaterialTheme.colorScheme.primary,

                            style = MaterialTheme.typography.bodySmall

                        )

                    }

                    item {

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                            OutlinedTextField(

                                value = exam1,

                                onValueChange = { exam1 = it },

                                label = { Text(if (Tr.isArabic()) "فرض 1 ($maxScaleStr)" else "Devoir 1 ($maxScaleStr)", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, fontSize = 11.sp) },

                                singleLine = true,

                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),

                                modifier = Modifier.weight(1f)

                            )

                            OutlinedTextField(

                                value = exam2,

                                onValueChange = { exam2 = it },

                                label = { Text(if (Tr.isArabic()) "فرض 2 ($maxScaleStr)" else "Devoir 2 ($maxScaleStr)", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, fontSize = 11.sp) },

                                singleLine = true,

                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),

                                modifier = Modifier.weight(1f)

                            )

                            OutlinedTextField(

                                value = exam3,

                                onValueChange = { exam3 = it },

                                label = { Text(if (Tr.isArabic()) "فرض 3 ($maxScaleStr)" else "Devoir 3 ($maxScaleStr)", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, fontSize = 11.sp) },

                                singleLine = true,

                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),

                                modifier = Modifier.weight(1f)

                            )

                        }

                    }

                    item {

                        Text(

                            text = if (isPrimary) {

                                if (Tr.isArabic()) "3. الاختبار الفصلي (معامل $activeCoeff - على 10)" else "3. Épreuve Finale / Composition (Coeff. $activeCoeff - sur 10)"

                            } else {

                                if (Tr.isArabic()) "4. الاختبار الفصل / الاختبار النهائي (معامل $activeCoeff)" else "4. Épreuve Finale / Composition (Coeff. $activeCoeff)"

                            },

                            fontWeight = FontWeight.Bold,

                            color = MaterialTheme.colorScheme.primary,

                            style = MaterialTheme.typography.bodySmall

                        )

                    }

                    item {

                        OutlinedTextField(

                            value = finalExam,

                            onValueChange = { finalExam = it },

                            label = { Text(if (Tr.isArabic()) "الاختبار ($maxScaleStr)" else "Composition ($maxScaleStr)") },

                            singleLine = true,

                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),

                            modifier = Modifier.fillMaxWidth()

                        )

                    }

                    // Aperçu en temps réel de la moyenne calculée

                    item {

                        Surface(

                            color = if (liveAvg != null && liveAvg >= passingThreshold) Color(0xFF10B981).copy(alpha = 0.12f)

                                   else if (liveAvg != null) Color(0xFFEF4444).copy(alpha = 0.12f)

                                   else MaterialTheme.colorScheme.surfaceVariant,

                            shape = RoundedCornerShape(10.dp),

                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)

                        ) {

                            Column(modifier = Modifier.padding(10.dp)) {

                                Row(

                                    modifier = Modifier.fillMaxWidth(),

                                    horizontalArrangement = Arrangement.SpaceBetween,

                                    verticalAlignment = Alignment.CenterVertically

                                ) {

                                    Text(

                                        text = if (Tr.isArabic()) "المعدل المحسوب :" else "Moyenne calculée :",

                                        style = MaterialTheme.typography.bodyMedium,

                                        fontWeight = FontWeight.Bold

                                    )

                                    Text(

                                        text = if (liveAvg != null) "%.2f $maxScaleStr".format(liveAvg) else "- $maxScaleStr",

                                        style = MaterialTheme.typography.titleMedium,

                                        fontWeight = FontWeight.ExtraBold,

                                        color = if (liveAvg != null && liveAvg >= passingThreshold) Color(0xFF10B981)

                                               else if (liveAvg != null) Color(0xFFEF4444)

                                               else MaterialTheme.colorScheme.onSurface

                                    )

                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(

                                    text = if (isPrimary) {

                                        if (Tr.isArabic()) "الصيغة : (المشروع × 1 + معدل الفروض × 1 + الاختبار × $activeCoeff) / مجموع المعاملات" else "Formule : (Projet × 1 + Moyenne Devoirs × 1 + Compo × $activeCoeff) / Total Coeffs"

                                    } else {

                                        if (Tr.isArabic()) "الصيغة : (مجموع التقويم × 1 + المشروع × 1 + معدل الفروض × 1 + الاختبار × $activeCoeff) / مجموع المعاملات" else "Formule : (Total Éval × 1 + Projet × 1 + Moyenne Devoirs × 1 + Compo × $activeCoeff) / Total Coeffs"

                                    },

                                    style = MaterialTheme.typography.labelSmall,

                                    color = MaterialTheme.colorScheme.outline,

                                    fontSize = 10.sp

                                )

                            }

                        }

                    }

                }

            },

            confirmButton = {

                Button(

                    onClick = {

                        viewModel.saveStudentGrade(

                            studentId = editingGrade.studentId,

                            term = selectedTerm,

                            participation = pVal,

                            discipline = dVal,

                            assiduity = aVal,

                            notebook = nVal,

                            project = projVal,

                            exam1 = ex1Val,

                            exam2 = ex2Val,

                            exam3 = ex3Val,

                            finalExam = fVal

                        )

                        Toast.makeText(context, if (isAr) "تم حفظ النقاط بنجاح !" else "Notes enregistrées !", Toast.LENGTH_SHORT).show()

                        studentToGrade = null

                    }

                ) {

                    Text(if (Tr.isArabic()) "حفظ النقاط" else "Enregistrer les notes")

                }

            },

            dismissButton = {

                TextButton(onClick = { studentToGrade = null }) {

                    Text(if (Tr.isArabic()) "إلغاء" else "Annuler")

                }

            }

        )

    }

    if (showClassSummaryDialog && activeClass != null) {

        val coeff = activeClass.subjectCoefficient

        val maxScaleStr = if (isPrimary) "/ 10" else "/ 20"

        val passingThreshold = if (isPrimary) 5f else 10f

        val studentsWithAverage = gradesList.map { it to computeStudentAverage(it, coeff, isPrimary) }

        val gradedStudents = studentsWithAverage.filter { it.second != null }.map { it.first to it.second!! }

        val classAverage = if (gradedStudents.isNotEmpty()) gradedStudents.map { it.second }.average().toFloat() else null

        val bestStudent = gradedStudents.maxByOrNull { it.second }

        val worstStudent = gradedStudents.minByOrNull { it.second }

        val passedCount = gradedStudents.count { it.second >= passingThreshold }

        val failedCount = gradedStudents.count { it.second < passingThreshold }

        val successRate = if (gradedStudents.isNotEmpty()) (passedCount.toFloat() / gradedStudents.size * 100).toInt() else 0

        AlertDialog(

            onDismissRequest = { showClassSummaryDialog = false },

            title = {

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Icon(

                        imageVector = Icons.Default.Analytics,

                        contentDescription = null,

                        tint = MaterialTheme.colorScheme.primary,

                        modifier = Modifier.size(24.dp)

                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(

                        text = "${if (Tr.isArabic()) "ملخص القسم :" else "Résumé :"} ${activeClass.name}",

                        style = MaterialTheme.typography.titleMedium,

                        fontWeight = FontWeight.Bold

                    )

                }

            },

            text = {

                val isDark = MaterialTheme.colorScheme.background != OffWhite && MaterialTheme.colorScheme.background != Color(0xFFF8FAFC)

                val isAvgAdmitted = classAverage != null && classAverage >= passingThreshold

                val avgCardBg = when {

                    classAverage == null -> if (isDark) listOf(Color(0xFF1E293B), Color(0xFF0F172A)) else listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9))

                    isAvgAdmitted -> if (isDark) listOf(Color(0xFF064E3B).copy(alpha = 0.45f), Color(0xFF022C22).copy(alpha = 0.55f)) else listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7))

                    else -> if (isDark) listOf(Color(0xFF7F1D1D).copy(alpha = 0.45f), Color(0xFF450A0A).copy(alpha = 0.55f)) else listOf(Color(0xFFFEF2F2), Color(0xFFFEE2E2))

                }

                val avgCardBorder = when {

                    classAverage == null -> if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)

                    isAvgAdmitted -> if (isDark) Color(0xFF10B981).copy(alpha = 0.55f) else Color(0xFF22C55E).copy(alpha = 0.45f)

                    else -> if (isDark) Color(0xFFF43F5E).copy(alpha = 0.55f) else Color(0xFFEF4444).copy(alpha = 0.45f)

                }

                val avgLabelColor = when {

                    classAverage == null -> if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

                    isAvgAdmitted -> if (isDark) Color(0xFF6EE7B7) else Color(0xFF166534)

                    else -> if (isDark) Color(0xFFFDA4AF) else Color(0xFF991B1B)

                }

                val avgValueColor = when {

                    classAverage == null -> if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)

                    isAvgAdmitted -> if (isDark) Color(0xFFA7F3D0) else Color(0xFF15803D)

                    else -> if (isDark) Color(0xFFFECDD3) else Color(0xFFB91C1C)

                }

                val rateCardBg = if (isDark) listOf(Color(0xFF1E3A8A).copy(alpha = 0.45f), Color(0xFF172554).copy(alpha = 0.55f)) else listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE))

                val rateCardBorder = if (isDark) Color(0xFF3B82F6).copy(alpha = 0.55f) else Color(0xFF3B82F6).copy(alpha = 0.45f)

                val rateLabelColor = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E40AF)

                val rateValueColor = if (isDark) Color(0xFFBFDBFE) else Color(0xFF1D4ED8)

                val rateTrackColor = if (isDark) Color(0xFF1E3A8A) else Color(0xFFBFDBFE)

                val rateBarColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)

                val detailsCardBg = if (isDark) listOf(Color(0xFF1E293B).copy(alpha = 0.60f), Color(0xFF0F172A).copy(alpha = 0.70f)) else listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9))

                val detailsCardBorder = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

                val detailsLabelColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

                val detailsValueColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

                val detailsDividerColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

                val admisBg = if (isDark) listOf(Color(0xFF064E3B).copy(alpha = 0.45f), Color(0xFF022C22).copy(alpha = 0.55f)) else listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7))

                val admisBorder = if (isDark) Color(0xFF10B981).copy(alpha = 0.55f) else Color(0xFF22C55E).copy(alpha = 0.45f)

                val admisTitleColor = if (isDark) Color(0xFF6EE7B7) else Color(0xFF166534)

                val admisCountColor = if (isDark) Color(0xFFA7F3D0) else Color(0xFF15803D)

                val nonAdmisBg = if (isDark) listOf(Color(0xFF7F1D1D).copy(alpha = 0.45f), Color(0xFF450A0A).copy(alpha = 0.55f)) else listOf(Color(0xFFFEF2F2), Color(0xFFFEE2E2))

                val nonAdmisBorder = if (isDark) Color(0xFFF43F5E).copy(alpha = 0.55f) else Color(0xFFEF4444).copy(alpha = 0.45f)

                val nonAdmisTitleColor = if (isDark) Color(0xFFFDA4AF) else Color(0xFF991B1B)

                val nonAdmisCountColor = if (isDark) Color(0xFFFECDD3) else Color(0xFFBE123C)

                LazyColumn(

                    modifier = Modifier.fillMaxWidth().heightIn(max = adaptiveDialogMaxHeight(fraction = 0.65f, maxHeight = 520.dp)),

                    verticalArrangement = Arrangement.spacedBy(12.dp)

                ) {

                    // Statistiques globales (Modern Card Dashboard)

                    item {

                        Column(

                            modifier = Modifier.fillMaxWidth(),

                            verticalArrangement = Arrangement.spacedBy(8.dp)

                        ) {

                            Text(

                                text = "${if (Tr.isArabic()) "الإحصائيات العامة (الفصل" else "STATISTIQUES GLOBALES (TRIMESTRE"} $selectedTerm${if (Tr.isArabic()) ")" else ")"}".uppercase(),

                                style = MaterialTheme.typography.labelMedium,

                                fontWeight = FontWeight.Bold,

                                color = MaterialTheme.colorScheme.primary,

                                modifier = Modifier.padding(bottom = 4.dp)

                            )

                            // Main Stats Side-by-Side Cards

                            Row(

                                modifier = Modifier.fillMaxWidth(),

                                horizontalArrangement = Arrangement.spacedBy(8.dp)

                            ) {

                                // Card 1: Moyenne

                                Box(

                                    modifier = Modifier

                                        .weight(1f)

                                        .shadow(elevation = 1.5.dp, shape = RoundedCornerShape(14.dp))

                                        .clip(RoundedCornerShape(14.dp))

                                        .background(Brush.verticalGradient(avgCardBg))

                                        .border(width = 1.dp, color = avgCardBorder, shape = RoundedCornerShape(14.dp))

                                        .padding(12.dp),

                                    contentAlignment = Alignment.Center

                                ) {

                                    Column(

                                        horizontalAlignment = Alignment.CenterHorizontally

                                    ) {

                                        Text(

                                            text = if (Tr.isArabic()) "المعدل العام" else "Moyenne",

                                            style = MaterialTheme.typography.labelSmall,

                                            fontWeight = FontWeight.Bold,

                                            color = avgLabelColor

                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        val avgStr = if (classAverage != null) "%.2f".format(classAverage) else "N/A"

                                        Text(

                                            text = avgStr,

                                            style = MaterialTheme.typography.headlineMedium,

                                            fontWeight = FontWeight.Black,

                                            color = avgValueColor

                                        )

                                        Text(

                                            text = maxScaleStr,

                                            style = MaterialTheme.typography.labelSmall,

                                            fontWeight = FontWeight.SemiBold,

                                            color = avgLabelColor.copy(alpha = 0.85f)

                                        )

                                    }

                                }

                                // Card 2: Taux de Réussite

                                Box(

                                    modifier = Modifier

                                        .weight(1f)

                                        .shadow(elevation = 1.5.dp, shape = RoundedCornerShape(14.dp))

                                        .clip(RoundedCornerShape(14.dp))

                                        .background(Brush.verticalGradient(rateCardBg))

                                        .border(width = 1.dp, color = rateCardBorder, shape = RoundedCornerShape(14.dp))

                                        .padding(12.dp),

                                    contentAlignment = Alignment.Center

                                ) {

                                    Column(

                                        horizontalAlignment = Alignment.CenterHorizontally

                                    ) {

                                        Text(

                                            text = if (Tr.isArabic()) "نسبة النجاح" else "Taux Réussite",

                                            style = MaterialTheme.typography.labelSmall,

                                            fontWeight = FontWeight.Bold,

                                            color = rateLabelColor

                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(

                                            text = "$successRate%",

                                            style = MaterialTheme.typography.headlineMedium,

                                            fontWeight = FontWeight.Black,

                                            color = rateValueColor

                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        LinearProgressIndicator(

                                            progress = { successRate.toFloat() / 100f },

                                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),

                                            color = rateBarColor,

                                            trackColor = rateTrackColor

                                        )

                                    }

                                }

                            }

                            // Secondary Stats Details

                            Box(

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .shadow(elevation = 1.5.dp, shape = RoundedCornerShape(14.dp))

                                    .clip(RoundedCornerShape(14.dp))

                                    .background(Brush.verticalGradient(detailsCardBg))

                                    .border(width = 1.dp, color = detailsCardBorder, shape = RoundedCornerShape(14.dp))

                                    .padding(14.dp)

                            ) {

                                Column(

                                    verticalArrangement = Arrangement.spacedBy(8.dp)

                                ) {

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

                                        Text(if (Tr.isArabic()) "إجمالي التلاميذ :" else "Effectif Total :", style = MaterialTheme.typography.bodyMedium, color = detailsLabelColor)

                                        Text("${gradesList.size} ${if (Tr.isArabic()) (if (gradesList.size <= 1) "تلميذ" else "تلاميذ") else (if (gradesList.size <= 1) "élève" else "élèves")}", fontWeight = FontWeight.Bold, color = detailsValueColor, style = MaterialTheme.typography.bodyMedium)

                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

                                        Text(if (Tr.isArabic()) "التلاميذ المقيمون :" else "Élèves Évalués :", style = MaterialTheme.typography.bodyMedium, color = detailsLabelColor)

                                        Text("${gradedStudents.size} ${if (Tr.isArabic()) (if (gradedStudents.size <= 1) "تلميذ" else "تلاميذ") else (if (gradedStudents.size <= 1) "élève" else "élèves")}", fontWeight = FontWeight.Bold, color = detailsValueColor, style = MaterialTheme.typography.bodyMedium)

                                    }

                                    HorizontalDivider(color = detailsDividerColor)

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

                                        Text(if (Tr.isArabic()) "أعلى معدل :" else "Meilleure Note :", style = MaterialTheme.typography.bodyMedium, color = detailsLabelColor)

                                        val bestStr = if (bestStudent != null) "%.2f (${bestStudent.first.lastName.uppercase()})".format(bestStudent.second) else "N/A"

                                        Surface(

                                            color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.6f) else Color(0xFFDCFCE7),

                                            shape = RoundedCornerShape(6.dp),

                                            border = BorderStroke(1.dp, if (isDark) Color(0xFF059669) else Color(0xFF86EFAC))

                                        ) {

                                            Text(

                                                text = bestStr,

                                                fontWeight = FontWeight.Bold,

                                                color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF15803D),

                                                fontSize = 12.sp,

                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)

                                            )

                                        }

                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

                                        Text(if (Tr.isArabic()) "أدنى معدل :" else "Plus Basse Note :", style = MaterialTheme.typography.bodyMedium, color = detailsLabelColor)

                                        val worstStr = if (worstStudent != null) "%.2f (${worstStudent.first.lastName.uppercase()})".format(worstStudent.second) else "N/A"

                                        Surface(

                                            color = if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.6f) else Color(0xFFFFE4E6),

                                            shape = RoundedCornerShape(6.dp),

                                            border = BorderStroke(1.dp, if (isDark) Color(0xFFE11D48) else Color(0xFFFDA4AF))

                                        ) {

                                            Text(

                                                text = worstStr,

                                                fontWeight = FontWeight.Bold,

                                                color = if (isDark) Color(0xFFFECDD3) else Color(0xFFBE123C),

                                                fontSize = 12.sp,

                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)

                                            )

                                        }

                                    }

                                }

                            }

                        }

                    }

                    // Répartition admis/non admis

                    item {

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                            Box(

                                modifier = Modifier

                                    .weight(1f)

                                    .shadow(elevation = 1.5.dp, shape = RoundedCornerShape(14.dp))

                                    .clip(RoundedCornerShape(14.dp))

                                    .background(Brush.verticalGradient(admisBg))

                                    .border(width = 1.dp, color = admisBorder, shape = RoundedCornerShape(14.dp))

                                    .padding(10.dp)

                            ) {

                                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

                                    Text(

                                        text = if (Tr.isArabic()) "ناجحون (معدل >= ${passingThreshold.toInt()})" else "Admis (Moy. >= ${passingThreshold.toInt()})",

                                        fontSize = 10.5.sp,

                                        color = admisTitleColor,

                                        fontWeight = FontWeight.Bold

                                    )

                                    Spacer(Modifier.height(2.dp))

                                    Text(

                                        text = "$passedCount",

                                        style = MaterialTheme.typography.titleLarge,

                                        fontWeight = FontWeight.Black,

                                        color = admisCountColor

                                    )

                                }

                            }

                            Box(

                                modifier = Modifier

                                    .weight(1f)

                                    .shadow(elevation = 1.5.dp, shape = RoundedCornerShape(14.dp))

                                    .clip(RoundedCornerShape(14.dp))

                                    .background(Brush.verticalGradient(nonAdmisBg))

                                    .border(width = 1.dp, color = nonAdmisBorder, shape = RoundedCornerShape(14.dp))

                                    .padding(10.dp)

                            ) {

                                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

                                    Text(

                                        text = if (Tr.isArabic()) "غير ناجحين (< ${passingThreshold.toInt()})" else "Non admis (< ${passingThreshold.toInt()})",

                                        fontSize = 10.5.sp,

                                        color = nonAdmisTitleColor,

                                        fontWeight = FontWeight.Bold

                                    )

                                    Spacer(Modifier.height(2.dp))

                                    Text(

                                        text = "$failedCount",

                                        style = MaterialTheme.typography.titleLarge,

                                        fontWeight = FontWeight.Black,

                                        color = nonAdmisCountColor

                                    )

                                }

                            }

                        }

                    }

                    // Liste détaillée de tous les élèves

                    item {

                        Text(

                            text = if (Tr.isArabic()) "التفاصيل حسب التلميذ" else "DÉTAIL PAR ÉLÈVE",

                            style = MaterialTheme.typography.labelMedium,

                            fontWeight = FontWeight.Bold,

                            color = MaterialTheme.colorScheme.primary

                        )

                    }

                    items(studentsWithAverage, key = { it.first.studentId }) { (student, avg) ->

                        Row(

                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),

                            horizontalArrangement = Arrangement.SpaceBetween,

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            Text(

                                text = "${student.lastName.uppercase()} ${student.firstName}",

                                style = MaterialTheme.typography.bodyMedium,

                                fontWeight = FontWeight.SemiBold,

                                modifier = Modifier.weight(1f),

                                maxLines = 1

                            )

                            val badgeBg = when {

                                avg == null -> if (isDark) Color(0xFF334155) else Color(0xFFF1F3F4)

                                avg >= passingThreshold -> if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)

                                else -> if (isDark) Color(0xFF7F1D1D) else Color(0xFFFFE4E6)

                            }

                            val badgeTc = when {

                                avg == null -> if (isDark) Color(0xFF94A3B8) else Color(0xFF5F6368)

                                avg >= passingThreshold -> if (isDark) Color(0xFFA7F3D0) else Color(0xFF15803D)

                                else -> if (isDark) Color(0xFFFECDD3) else Color(0xFFBE123C)

                            }

                            val badgeBorder = when {

                                avg == null -> if (isDark) Color(0xFF475569) else Color(0xFFE2E8F0)

                                avg >= passingThreshold -> if (isDark) Color(0xFF059669) else Color(0xFF86EFAC)

                                else -> if (isDark) Color(0xFFE11D48) else Color(0xFFFDA4AF)

                            }

                            val badgeText = when {

                                avg == null -> "N/A"

                                else -> "%.2f".format(avg)

                            }

                            Box(

                                modifier = Modifier

                                    .clip(RoundedCornerShape(8.dp))

                                    .background(badgeBg)

                                    .border(1.dp, badgeBorder, RoundedCornerShape(8.dp))

                                    .padding(horizontal = 8.dp, vertical = 3.dp)

                            ) {

                                Text(

                                    text = badgeText,

                                    color = badgeTc,

                                    fontSize = 12.sp,

                                    fontWeight = FontWeight.Bold

                                )

                            }

                        }

                        HorizontalDivider(color = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    }

                }

            },

            confirmButton = {

                Button(onClick = { showClassSummaryDialog = false }) {

                    Text(if (Tr.isArabic()) "إغلاق" else "Fermer")

                }

            }

        )

    }

    if (showComparisonDialog && activeClass != null) {

        val coeff = activeClass.subjectCoefficient

        val passingThreshold = if (isPrimary) 5f else 10f

        var dialogTab by remember { mutableStateOf(0) }

        val allStudentsInClass: List<StudentWithGrade> = (term1Grades + term2Grades + term3Grades)

            .distinctBy { it.studentId }

            .sortedWith(compareBy({ it.orderIndex }, { it.lastName }, { it.firstName }))

        val t1Avgs = term1Grades.mapNotNull { computeStudentAverage(it, coeff, isPrimary) }

        val t2Avgs = term2Grades.mapNotNull { computeStudentAverage(it, coeff, isPrimary) }

        val t3Avgs = term3Grades.mapNotNull { computeStudentAverage(it, coeff, isPrimary) }

        val classAvgT1 = if (t1Avgs.isNotEmpty()) t1Avgs.average().toFloat() else null

        val classAvgT2 = if (t2Avgs.isNotEmpty()) t2Avgs.average().toFloat() else null

        val classAvgT3 = if (t3Avgs.isNotEmpty()) t3Avgs.average().toFloat() else null

        AlertDialog(

            onDismissRequest = { showComparisonDialog = false },

            title = {

                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.SpaceBetween,

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Icon(

                            imageVector = Icons.Default.TrendingUp,

                            contentDescription = null,

                            tint = MaterialTheme.colorScheme.primary,

                            modifier = Modifier.size(24.dp)

                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(

                            text = if (Tr.isArabic()) "المقارنة" else "Comparaison",

                            style = MaterialTheme.typography.titleMedium,

                            fontWeight = FontWeight.Bold

                        )

                    }

                    IconButton(onClick = { showComparisonDialog = false }) {

                        Icon(Icons.Default.Close, contentDescription = if (Tr.isArabic()) "إغلاق" else "Fermer")

                    }

                }

            },

            text = {

                if (isHistoryLoading) {

                    Box(

                        modifier = Modifier

                            .fillMaxWidth()

                            .height(200.dp),

                        contentAlignment = Alignment.Center

                    ) {

                        CircularProgressIndicator()

                    }

                } else {

                    Column(

                        modifier = Modifier

                            .fillMaxWidth()

                            .heightIn(max = adaptiveDialogMaxHeight(fraction = 0.65f, maxHeight = 520.dp)),

                        verticalArrangement = Arrangement.spacedBy(12.dp)

                    ) {

                        TabRow(

                            selectedTabIndex = dialogTab,

                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)

                        ) {

                            Tab(

                                selected = dialogTab == 0,

                                onClick = { dialogTab = 0 },

                                text = { Text(if (Tr.isArabic()) "حسب التلميذ" else "Par élève", fontSize = 11.sp, fontWeight = FontWeight.Bold) }

                            )

                            Tab(

                                selected = dialogTab == 1,

                                onClick = { dialogTab = 1 },

                                text = { Text(if (Tr.isArabic()) "بين الأقسام" else "Entre classes", fontSize = 11.sp, fontWeight = FontWeight.Bold) }

                            )

                        }

                        val isDarkComp = MaterialTheme.colorScheme.background != OffWhite && MaterialTheme.colorScheme.background != Color(0xFFF8FAFC)
                        val compPassColor = if (isDarkComp) Color(0xFF34D399) else Color(0xFF137333)
                        val compFailColor = if (isDarkComp) Color(0xFFF87171) else Color(0xFFC5221F)

                        if (dialogTab == 0) {

                            Card(

                                modifier = Modifier.fillMaxWidth(),

                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDarkComp) Color(0xFF1E3A8A).copy(alpha = 0.45f)
                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                )

                            ) {

                                Column(modifier = Modifier.padding(10.dp)) {

                                    Text(

                                        "${if (Tr.isArabic()) "المعدلات العامة" else "MOYENNES GÉNÉRALES"} : ${activeClass.name.uppercase()}",

                                        style = MaterialTheme.typography.labelMedium,

                                        fontWeight = FontWeight.Bold,

                                        color = if (isDarkComp) Color(0xFF93C5FD) else MaterialTheme.colorScheme.primary

                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(

                                        modifier = Modifier.fillMaxWidth(),

                                        horizontalArrangement = Arrangement.SpaceBetween

                                    ) {

                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {

                                            Text(if (Tr.isArabic()) "الفصل 1" else "Trimestre 1", style = MaterialTheme.typography.labelSmall, color = if (isDarkComp) Color(0xFFCBD5E1) else MaterialTheme.colorScheme.onPrimaryContainer)

                                            Text(text = classAvgT1?.let { "%.2f".format(it) } ?: "N/A", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {

                                            Text(if (Tr.isArabic()) "الفصل 2" else "Trimestre 2", style = MaterialTheme.typography.labelSmall, color = if (isDarkComp) Color(0xFFCBD5E1) else MaterialTheme.colorScheme.onPrimaryContainer)

                                            Row(verticalAlignment = Alignment.CenterVertically) {

                                                Text(text = classAvgT2?.let { "%.2f".format(it) } ?: "N/A", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                                                if (classAvgT1 != null && classAvgT2 != null) {

                                                    val diff = classAvgT2 - classAvgT1

                                                    val icon = if (diff >= 0) "↗" else "↘"

                                                    val color = if (diff >= 0) compPassColor else compFailColor

                                                    Text(" $icon", color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp)

                                                }

                                            }

                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {

                                            Text(if (Tr.isArabic()) "الفصل 3" else "Trimestre 3", style = MaterialTheme.typography.labelSmall, color = if (isDarkComp) Color(0xFFCBD5E1) else MaterialTheme.colorScheme.onPrimaryContainer)

                                            Row(verticalAlignment = Alignment.CenterVertically) {

                                                Text(text = classAvgT3?.let { "%.2f".format(it) } ?: "N/A", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                                                if (classAvgT2 != null && classAvgT3 != null) {

                                                    val diff = classAvgT3 - classAvgT2

                                                    val icon = if (diff >= 0) "↗" else "↘"

                                                    val color = if (diff >= 0) compPassColor else compFailColor

                                                    Text(" $icon", color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp)

                                                }

                                            }

                                        }

                                    }

                                }

                            }

                            Row(

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .background(
                                        if (isDarkComp) Color(0xFF1E293B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(6.dp)
                                    )

                                    .padding(horizontal = 6.dp, vertical = 6.dp),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Text(if (Tr.isArabic()) "التلميذ" else "Élève", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f))

                                Text(if (Tr.isArabic()) "فصل 1" else "Trim 1", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)

                                Text(if (Tr.isArabic()) "فصل 2" else "Trim 2", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)

                                Text(if (Tr.isArabic()) "فصل 3" else "Trim 3", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)

                            }

                            if (allStudentsInClass.isEmpty()) {

                                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {

                                    Text(if (Tr.isArabic()) "لا يوجد تلاميذ" else "Aucun élève", style = MaterialTheme.typography.bodyMedium)

                                }

                            } else {

                                LazyColumn(

                                    modifier = Modifier.weight(1f),

                                    verticalArrangement = Arrangement.spacedBy(6.dp)

                                ) {

                                    items(allStudentsInClass, key = { it.studentId }) { student ->

                                        val t1Grade = term1Grades.find { it.studentId == student.studentId }

                                        val t2Grade = term2Grades.find { it.studentId == student.studentId }

                                        val t3Grade = term3Grades.find { it.studentId == student.studentId }

                                        val t1Avg = t1Grade?.let { computeStudentAverage(it, coeff, isPrimary) }

                                        val t2Avg = t2Grade?.let { computeStudentAverage(it, coeff, isPrimary) }

                                        val t3Avg = t3Grade?.let { computeStudentAverage(it, coeff, isPrimary) }

                                        Row(

                                            modifier = Modifier

                                                .fillMaxWidth()

                                                .border(
                                                    1.dp,
                                                    if (isDarkComp) Color(0xFF334155) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                    RoundedCornerShape(6.dp)
                                                )

                                                .padding(horizontal = 6.dp, vertical = 8.dp),

                                            verticalAlignment = Alignment.CenterVertically

                                        ) {

                                            Text(

                                                text = "${student.lastName.uppercase()} ${student.firstName.take(1)}.",

                                                style = MaterialTheme.typography.bodySmall,

                                                fontWeight = FontWeight.SemiBold,

                                                modifier = Modifier.weight(1.3f),

                                                maxLines = 1,

                                                overflow = TextOverflow.Ellipsis

                                            )

                                            Text(

                                                text = t1Avg?.let { "%.2f".format(it) } ?: "-",

                                                style = MaterialTheme.typography.bodySmall,

                                                fontWeight = FontWeight.Medium,

                                                modifier = Modifier.weight(0.9f),

                                                textAlign = TextAlign.Center,

                                                color = if (t1Avg != null) {

                                                    if (t1Avg >= passingThreshold) compPassColor else compFailColor

                                                } else MaterialTheme.colorScheme.onSurfaceVariant

                                            )

                                            Row(

                                                modifier = Modifier.weight(0.9f),

                                                horizontalArrangement = Arrangement.Center,

                                                verticalAlignment = Alignment.CenterVertically

                                            ) {

                                                Text(

                                                    text = t2Avg?.let { "%.2f".format(it) } ?: "-",

                                                    style = MaterialTheme.typography.bodySmall,

                                                    fontWeight = FontWeight.Medium,

                                                    color = if (t2Avg != null) {

                                                        if (t2Avg >= passingThreshold) compPassColor else compFailColor

                                                    } else MaterialTheme.colorScheme.onSurfaceVariant

                                                )

                                                if (t1Avg != null && t2Avg != null) {

                                                    val diff = t2Avg - t1Avg

                                                    val icon = if (diff >= 0) "↗" else "↘"

                                                    val color = if (diff >= 0) compPassColor else compFailColor

                                                    Text(text = " $icon", color = color, fontWeight = FontWeight.Bold, fontSize = 9.sp)

                                                }

                                            }

                                            Row(

                                                modifier = Modifier.weight(0.9f),

                                                horizontalArrangement = Arrangement.Center,

                                                verticalAlignment = Alignment.CenterVertically

                                            ) {

                                                Text(

                                                    text = t3Avg?.let { "%.2f".format(it) } ?: "-",

                                                    style = MaterialTheme.typography.bodySmall,

                                                    fontWeight = FontWeight.Medium,

                                                    color = if (t3Avg != null) {

                                                        if (t3Avg >= passingThreshold) compPassColor else compFailColor

                                                    } else MaterialTheme.colorScheme.onSurfaceVariant

                                                )

                                                if (t2Avg != null && t3Avg != null) {

                                                    val diff = t3Avg - t2Avg

                                                    val icon = if (diff >= 0) "↗" else "↘"

                                                    val color = if (diff >= 0) compPassColor else compFailColor

                                                    Text(text = " $icon", color = color, fontWeight = FontWeight.Bold, fontSize = 9.sp)

                                                }

                                            }

                                        }

                                    }

                                }

                            }

                        } else {

                            Row(

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))

                                    .padding(horizontal = 6.dp, vertical = 6.dp),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Text(if (Tr.isArabic()) "القسم" else "Classe", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f))

                                Text(if (Tr.isArabic()) "فصل 1" else "Trim 1", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)

                                Text(if (Tr.isArabic()) "فصل 2" else "Trim 2", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)

                                Text(if (Tr.isArabic()) "فصل 3" else "Trim 3", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)

                            }

                            LazyColumn(

                                modifier = Modifier.weight(1f),

                                verticalArrangement = Arrangement.spacedBy(6.dp)

                            ) {

                                items(classes, key = { it.id }) { cls ->

                                    val avgs = classAverages[cls.id] ?: Triple(null, null, null)

                                    val t1Val = avgs.first

                                    val t2Val = avgs.second

                                    val t3Val = avgs.third

                                    val isCurrent = cls.id == activeClass.id

                                    Row(

                                        modifier = Modifier

                                            .fillMaxWidth()

                                            .background(

                                                if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)

                                                else Color.Transparent,

                                                RoundedCornerShape(6.dp)

                                            )

                                            .border(

                                                1.dp,

                                                if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

                                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),

                                                RoundedCornerShape(6.dp)

                                            )

                                            .padding(horizontal = 6.dp, vertical = 10.dp),

                                        verticalAlignment = Alignment.CenterVertically

                                    ) {

                                        Text(

                                            text = cls.name,

                                            style = MaterialTheme.typography.bodySmall,

                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,

                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,

                                            modifier = Modifier.weight(1.3f),

                                            maxLines = 1,

                                            overflow = TextOverflow.Ellipsis

                                        )

                                        Text(

                                            text = t1Val?.let { "%.2f".format(it) } ?: "-",

                                            style = MaterialTheme.typography.bodySmall,

                                            fontWeight = FontWeight.Medium,

                                            modifier = Modifier.weight(0.9f),

                                            textAlign = TextAlign.Center

                                        )

                                        Text(

                                            text = t2Val?.let { "%.2f".format(it) } ?: "-",

                                            style = MaterialTheme.typography.bodySmall,

                                            fontWeight = FontWeight.Medium,

                                            modifier = Modifier.weight(0.9f),

                                            textAlign = TextAlign.Center

                                        )

                                        Text(

                                            text = t3Val?.let { "%.2f".format(it) } ?: "-",

                                            style = MaterialTheme.typography.bodySmall,

                                            fontWeight = FontWeight.Medium,

                                            modifier = Modifier.weight(0.9f),

                                            textAlign = TextAlign.Center

                                        )

                                    }

                                }

                            }

                        }

                    }

                }

            },

            confirmButton = {

                Button(onClick = { showComparisonDialog = false }) {

                    Text(if (Tr.isArabic()) "حسناً" else "OK")

                }

            }

        )

    }

}

private fun computeStudentAverage(grade: StudentWithGrade, subjectCoefficient: Int, isPrimary: Boolean = false): Float? {

    return GradeCalculator.computeStudentAverage(grade, subjectCoefficient, isPrimary)

}

@Composable
fun GradeItemCard(
    number: Int,
    grade: StudentWithGrade,
    subjectCoefficient: Int,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    val isAr = Tr.isArabic()
    val passingThreshold = if (isPrimary) 5f else 10f
    val maxScale = if (isPrimary) 10f else 20f
    val maxScaleStr = if (isPrimary) "/ 10" else "/ 20"

    // Calcul de la moyenne générale en temps réel
    val average = GradeCalculator.computeStudentAverage(grade, subjectCoefficient, isPrimary)
    val averageStr = if (average != null) "%.2f".format(average) else "-"

    val isLight = MaterialTheme.colorScheme.background == OffWhite || MaterialTheme.colorScheme.background == Color(0xFFF8FAFC)

    val isPassing = average != null && average >= passingThreshold
    val averageColor = when {
        average == null -> if (isLight) Color(0xFF64748B) else Color(0xFFCBD5E1)
        isPassing -> if (isLight) Color(0xFF059669) else Color(0xFF34D399)
        else -> if (isLight) Color(0xFFDC2626) else Color(0xFFF87171)
    }
    val averageBadgeBg = when {
        average == null -> if (isLight) Color(0xFFF1F5F9) else Color(0xFF334155).copy(alpha = 0.55f)
        isPassing -> if (isLight) Color(0xFFD1FAE5).copy(alpha = 0.85f) else Color(0xFF065F46).copy(alpha = 0.55f)
        else -> if (isLight) Color(0xFFFEE2E2).copy(alpha = 0.85f) else Color(0xFF7F1D1D).copy(alpha = 0.55f)
    }
    val averageBadgeBorder = when {
        average == null -> if (isLight) Color(0xFFCBD5E1) else Color(0xFF64748B).copy(alpha = 0.65f)
        isPassing -> if (isLight) Color(0xFF10B981).copy(alpha = 0.55f) else Color(0xFF34D399).copy(alpha = 0.65f)
        else -> if (isLight) Color(0xFFEF4444).copy(alpha = 0.55f) else Color(0xFFF87171).copy(alpha = 0.65f)
    }

    // Calcul des composantes pour affichage élégant (Évaluation, Devoir, Compo)
    val evalScore: Float? = if (isPrimary) {
        grade.project
    } else {
        val p = grade.participation ?: 0f
        val d = grade.discipline ?: 0f
        val a = grade.assiduity ?: 0f
        val n = grade.notebook ?: 0f
        val proj = grade.project ?: 0f
        if (grade.participation != null || grade.discipline != null || grade.assiduity != null || grade.notebook != null || grade.project != null) {
            (p + d + a + n + proj).coerceIn(0f, 20f)
        } else null
    }

    val devoirs = listOfNotNull(grade.exam1, grade.exam2, grade.exam3)
    val devoirScore: Float? = if (devoirs.isNotEmpty()) devoirs.average().toFloat() else null
    val compoScore: Float? = grade.finalExam

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = 2.dp,
        borderWidth = 1.15.dp,
        borderColor = when {
            average != null && average >= passingThreshold -> if (isLight) Color(0xFF10B981).copy(alpha = 0.50f) else Color(0xFF34D399).copy(alpha = 0.55f)
            average != null -> if (isLight) Color(0xFFEF4444).copy(alpha = 0.50f) else Color(0xFFF87171).copy(alpha = 0.55f)
            else -> if (isLight) Color(0xFFCBD5E1).copy(alpha = 0.70f) else Color(0xFF475569).copy(alpha = 0.75f)
        },
        tintColor = if (average != null && average >= passingThreshold) Color(0xFF10B981)
                    else if (average != null) Color(0xFFEF4444) else null,
        glassAlpha = if (isLight) 0.92f else 0.88f,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            // Ligne 1 : Numéro + Nom complet de l'élève + Badge Moyenne
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge Numéro d'ordre (sans initiales)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isLight) MaterialTheme.colorScheme.primary else Color(0xFF2563EB),
                    border = if (!isLight) BorderStroke(1.dp, Color(0xFF60A5FA).copy(alpha = 0.55f)) else null
                ) {
                    Text(
                        text = "$number",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Nom complet de l'élève (haut contraste et parfaitement lisible)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${grade.lastName.uppercase()} ${grade.firstName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = if (isLight) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = if (isAr) "اضغط للتنقيط" else "Appuyer pour noter",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8),
                        fontSize = 10.5.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Badge Moyenne
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = averageBadgeBg,
                    border = BorderStroke(1.dp, averageBadgeBorder)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isAr) "المعدل: " else "Moy: ",
                            style = MaterialTheme.typography.labelSmall,
                            color = averageColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = if (average != null) "$averageStr $maxScaleStr" else (if (isAr) "غير منقط" else "Non noté"),
                            color = averageColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }

            // Ligne 2 : Pastilles compactes (Éval, Devoir, Compo) + micro-jauge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val evalText = if (evalScore != null) "%.1f".format(evalScore) else "-"
                    MiniGradePill(
                        label = if (isAr) "تقويم" else "Éval",
                        value = evalText,
                        isEntered = evalScore != null,
                        isLight = isLight,
                        accentLight = Color(0xFF0284C7),
                        accentDark = Color(0xFF38BDF8)
                    )

                    val devoirText = if (devoirScore != null) "%.1f".format(devoirScore) else "-"
                    MiniGradePill(
                        label = if (isAr) "فرض" else "Devoir",
                        value = devoirText,
                        isEntered = devoirScore != null,
                        isLight = isLight,
                        accentLight = Color(0xFFD97706),
                        accentDark = Color(0xFFFBBF24)
                    )

                    val compoText = if (compoScore != null) "%.1f".format(compoScore) else "-"
                    MiniGradePill(
                        label = if (isAr) "اختبار" else "Compo",
                        value = compoText,
                        isEntered = compoScore != null,
                        isLight = isLight,
                        accentLight = Color(0xFF7C3AED),
                        accentDark = Color(0xFFA78BFA)
                    )
                }

                if (average != null) {
                    val progressFraction = (average / maxScale).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isLight) Color(0xFFE2E8F0) else Color(0xFF334155))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressFraction)
                                .clip(RoundedCornerShape(3.dp))
                                .background(averageColor)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = if (isLight) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniGradePill(
    label: String,
    value: String,
    isEntered: Boolean,
    isLight: Boolean = true,
    accentLight: Color = Color(0xFF2563EB),
    accentDark: Color = Color(0xFF60A5FA)
) {
    val accent = if (isLight) accentLight else accentDark
    val bgColor = if (isEntered) {
        if (isLight) accent.copy(alpha = 0.12f) else accent.copy(alpha = 0.20f)
    } else {
        if (isLight) Color(0xFFF1F5F9) else Color(0xFF1E293B).copy(alpha = 0.80f)
    }
    val borderColor = if (isEntered) {
        if (isLight) accent.copy(alpha = 0.40f) else accent.copy(alpha = 0.55f)
    } else {
        if (isLight) Color(0xFFCBD5E1).copy(alpha = 0.75f) else Color(0xFF475569).copy(alpha = 0.65f)
    }
    val labelColor = if (isEntered) {
        if (isLight) accent else accent.copy(alpha = 0.95f)
    } else {
        if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8)
    }
    val valueColor = if (isEntered) {
        if (isLight) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    } else {
        if (isLight) Color(0xFF94A3B8) else Color(0xFFCBD5E1)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = BorderStroke(0.9.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "${label}:",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = labelColor
            )
            Text(
                text = value,
                fontSize = 10.5.sp,
                fontWeight = if (isEntered) FontWeight.ExtraBold else FontWeight.Medium,
                color = valueColor
            )
        }
    }
}
