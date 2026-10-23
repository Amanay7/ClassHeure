package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StudentEntity
import com.example.ui.components.FineSearchBar
import com.example.util.Tr
import com.example.util.tr
import com.example.util.adaptiveDialogMaxHeight

data class SeatingGridConfig(
    val rowsCount: Int, // Nombre de rangées / colonnes horizontales (ex: 3 ou 4)
    val tablesPerRow: Int, // Nombre de tables par rangée (ex: 4 ou 5)
    val label: String
) {
    val totalSeats: Int get() = rowsCount * tablesPerRow * 2
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorizontalSeatingChartView(
    students: List<StudentEntity>,
    classId: Int,
    onAssignSeat: (studentId: Int, seatIdx: Int) -> Unit,
    onFreeSeat: (seatIdx: Int) -> Unit,
    onSwapSeats: (studentId1: Int, seatIdx1: Int, studentId2: Int, seatIdx2: Int) -> Unit,
    onAutoAssign: () -> Unit,
    onClearAll: () -> Unit,
    onExportPdf: () -> Unit
) {
    // Configurations de classe disponibles (3 rangées standards)
    val isAr = Tr.isArabic()
    val defaultConfigs = remember(isAr) {
        listOf(
            SeatingGridConfig(3, 4, if (isAr) "3 صفوف × 4 طاولات (24 مقعد)" else "3 rangées × 4 tables (24 places)"),
            SeatingGridConfig(3, 5, if (isAr) "3 صفوف × 5 طاولات (30 مقعد)" else "3 rangées × 5 tables (30 places)")
        )
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val seatingPrefs = remember(context) { context.getSharedPreferences("seating_chart_prefs", android.content.Context.MODE_PRIVATE) }
    val initialDeskPos = remember(classId) {
        seatingPrefs.getString("teacher_desk_pos_$classId", null)
            ?: seatingPrefs.getString("teacher_desk_pos_global", "centre")
            ?: "centre"
    }

    // Détermine la configuration par défaut selon les préférences enregistrées ou le nombre d'élèves
    val maxSeatAssigned = students.mapNotNull { it.seatIndex }.maxOrNull() ?: -1
    val initialConfig = remember(classId, students.size, maxSeatAssigned) {
        val savedRows = seatingPrefs.getInt("seating_rows_$classId", -1)
        val savedTables = seatingPrefs.getInt("seating_tables_$classId", -1)
        if (savedRows > 0 && savedTables > 0) {
            val total = savedRows * savedTables * 2
            val isPreset = defaultConfigs.firstOrNull { it.rowsCount == savedRows && it.tablesPerRow == savedTables }
            isPreset ?: SeatingGridConfig(
                savedRows,
                savedTables,
                if (isAr) "$savedRows صفوف × $savedTables طاولات ($total مقعد)" else "$savedRows rangées × $savedTables tables ($total places)"
            )
        } else {
            when {
                maxSeatAssigned >= 24 || students.size > 24 -> defaultConfigs[1]
                else -> defaultConfigs[0]
            }
        }
    }

    var selectedConfig by remember(classId) { mutableStateOf(initialConfig) }
    val onConfigChange: (SeatingGridConfig) -> Unit = { newConfig ->
        selectedConfig = newConfig
        seatingPrefs.edit()
            .putInt("seating_rows_$classId", newConfig.rowsCount)
            .putInt("seating_tables_$classId", newConfig.tablesPerRow)
            .apply()
    }

    var isGlobalOverviewMode by remember { mutableStateOf(true) } // true: vue d'ensemble compacte, false: vue agrandie
    var showConfigMenu by remember { mutableStateOf(false) }
    var showCustomConfigDialog by remember { mutableStateOf(false) }
    var teacherDeskPlacement by remember(classId) { mutableStateOf(initialDeskPos) } // "gauche", "centre", "droite"
    var showDeskPlacementMenu by remember { mutableStateOf(false) }

    // Dialog state
    var selectedSeatForAssign by remember { mutableStateOf<Int?>(null) }
    var selectedSeatForOptions by remember { mutableStateOf<Int?>(null) }
    var seatToSwapFirst by remember { mutableStateOf<Int?>(null) }
    var showConfirmAutoAssignDialog by remember { mutableStateOf(false) }
    var showConfirmClearDialog by remember { mutableStateOf(false) }

    val seatedCount = students.count { it.seatIndex != null }
    val totalStudents = students.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("horizontal_seating_chart")
    ) {
        // --- BARRE DE COMMANDE DU PLAN DE CLASSE ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Compteur & Bureau & Disposition (Boutons abrégés)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val badgeBg = if (seatedCount == totalStudents && totalStudents > 0) {
                        Color(0xFFD1FAE5)
                    } else if (seatedCount > 0) {
                        Color(0xFFFEF3C7)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                    val badgeColor = if (seatedCount == totalStudents && totalStudents > 0) {
                        Color(0xFF065F46)
                    } else if (seatedCount > 0) {
                        Color(0xFF92400E)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeBg)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "$seatedCount/$totalStudents",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }

                    // Bouton abrégé placement du bureau de l'enseignant
                    Box {
                        FilledTonalButton(
                            onClick = { showDeskPlacementMenu = true },
                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = if (isAr) "موضع مكتب الأستاذ" else "Position du bureau",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = when (teacherDeskPlacement) {
                                    "gauche" -> if (isAr) "مكتب ي" else "Bur. G"
                                    "droite" -> if (isAr) "مكتب يـم" else "Bur. D"
                                    else -> if (isAr) "مكتب و" else "Bur. C"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showDeskPlacementMenu,
                            onDismissRequest = { showDeskPlacementMenu = false }
                        ) {
                            listOf(
                                Triple("gauche", if (isAr) "المكتب على اليسار (ي)" else "Bureau à Gauche (G)", if (isAr) "أمام الصف الأيسر" else "Devant la rangée gauche"),
                                Triple("centre", if (isAr) "المكتب في الوسط (و)" else "Bureau au Centre (C)", if (isAr) "أمام الصف الأوسط" else "Devant la rangée centrale"),
                                Triple("droite", if (isAr) "المكتب على اليمين (يم)" else "Bureau à Droite (D)", if (isAr) "أمام الصف الأيمن" else "Devant la rangée droite")
                            ).forEach { (pos, label, desc) ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(label, fontWeight = if (teacherDeskPlacement == pos) FontWeight.Bold else FontWeight.Normal)
                                            Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    },
                                    leadingIcon = {
                                        if (teacherDeskPlacement == pos) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        } else {
                                            Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                        }
                                    },
                                    onClick = {
                                        teacherDeskPlacement = pos
                                        showDeskPlacementMenu = false
                                        seatingPrefs.edit()
                                            .putString("teacher_desk_pos_$classId", pos)
                                            .putString("teacher_desk_pos_global", pos)
                                            .apply()
                                    }
                                )
                            }
                        }
                    }

                    // Bouton abrégé sélecteur de grille / configuration
                    Box {
                        FilledTonalButton(
                            onClick = { showConfigMenu = true },
                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isAr) "${selectedConfig.rowsCount}ص×${selectedConfig.tablesPerRow}ط" else "${selectedConfig.rowsCount}R×${selectedConfig.tablesPerRow}T",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showConfigMenu,
                            onDismissRequest = { showConfigMenu = false }
                        ) {
                            defaultConfigs.forEach { cfg ->
                                val isSelected = (cfg.rowsCount == selectedConfig.rowsCount && cfg.tablesPerRow == selectedConfig.tablesPerRow)
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = cfg.label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        onConfigChange(cfg)
                                        showConfigMenu = false
                                    },
                                    leadingIcon = {
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                )
                            }

                            val isCurrentPreset = defaultConfigs.any { it.rowsCount == selectedConfig.rowsCount && it.tablesPerRow == selectedConfig.tablesPerRow }
                            if (!isCurrentPreset) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = selectedConfig.label,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (isAr) "تخصيص حالي" else "Configuration personnalisée",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    onClick = {
                                        showConfigMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isAr) "تخصيص الصفوف والطاولات..." else "Personnaliser (rangées / tables)...",
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showConfigMenu = false
                                    showCustomConfigDialog = true
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Actions rapides : Loupe (Zoom), A-Z, Réinitialiser, PDF
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    // Icône de la loupe (Zoom) de même taille et style que les autres icônes
                    IconButton(
                        onClick = { isGlobalOverviewMode = !isGlobalOverviewMode },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = if (isGlobalOverviewMode) Icons.Default.ZoomIn else Icons.Default.ZoomOut,
                            contentDescription = if (isGlobalOverviewMode) (if (isAr) "تكبير" else "Agrandir") else (if (isAr) "نظرة عامة" else "Vue d'ensemble"),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { showConfirmAutoAssignDialog = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = if (isAr) "توزيع تلقائي أ-ي" else "Placer automatiquement A-Z",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { showConfirmClearDialog = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = if (isAr) "إعادة تعيين المخطط" else "Réinitialiser le plan",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onExportPdf,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = if (isAr) "تصدير مخطط الجلوس PDF" else "Exporter Plan PDF",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // --- CONTENEUR HORIZONTAL GLOBAL DU PLAN DE CLASSE AVEC BOUTON DE ZOOM FLOTTANT ---
        val horizontalScrollState = rememberScrollState()
        val verticalScrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(horizontalScrollState)
                    .verticalScroll(verticalScrollState)
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (isGlobalOverviewMode) 10.dp else 16.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    for (rowIndex in 0 until selectedConfig.rowsCount) {
                        val rowTitle = when (selectedConfig.rowsCount) {
                            2 -> if (isAr) (if (rowIndex == 0) "الصف الأيسر" else "الصف الأيمن") else (if (rowIndex == 0) "Rangée Gauche" else "Rangée Droite")
                            3 -> if (isAr) listOf("الصف الأيسر", "الصف الأوسط", "الصف الأيمن")[rowIndex] else listOf("Rangée Gauche", "Rangée Centre", "Rangée Droite")[rowIndex]
                            else -> if (isAr) "الصف ${rowIndex + 1}" else "Rangée ${rowIndex + 1}"
                        }
                        val columnWidth = if (isGlobalOverviewMode) 112.dp else 156.dp

                    Column(
                        modifier = Modifier
                            .width(columnWidth)
                            .shadow(elevation = 2.5.dp, shape = RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
                                    )
                                )
                            )
                            .border(
                                width = 1.2.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.65f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                        Color.White.copy(alpha = 0.20f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // En-tête de rangée avec alignement uniforme
                        val isDeskInThisRow = when (teacherDeskPlacement) {
                            "gauche" -> rowIndex == 0
                            "centre" -> rowIndex == (selectedConfig.rowsCount / 2)
                            "droite" -> rowIndex == (selectedConfig.rowsCount - 1)
                            else -> rowIndex == 0
                        }

                        Box(
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDeskInThisRow) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp)
                                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(10.dp))
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFFEEF2FF), Color(0xFFE0E7FF), Color(0xFFC7D2FE))
                                            )
                                        )
                                        .border(
                                            width = 1.2.dp,
                                            brush = Brush.linearGradient(
                                                listOf(Color.White, Color(0xFF818CF8), Color(0xFF4F46E5))
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.School,
                                            contentDescription = if (isAr) "المكتب" else "Bureau",
                                            tint = Color(0xFF3730A3),
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = if (isAr) "المكتب" else "Bureau",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.3.sp,
                                            color = Color(0xFF312E81)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = rowTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        // Liste des tables de cette rangée
                        Column(
                            verticalArrangement = Arrangement.spacedBy(if (isGlobalOverviewMode) 8.dp else 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            for (tableIndex in 0 until selectedConfig.tablesPerRow) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(elevation = 1.5.dp, shape = RoundedCornerShape(12.dp))
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(
                                            width = 1.dp,
                                            brush = Brush.linearGradient(
                                                listOf(
                                                    Color.White.copy(alpha = 0.5f),
                                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isAr) "طاولة ${tableIndex + 1}" else "Table ${tableIndex + 1}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        for (placeIndex in 0 until 2) {
                                            val seatIdx = (rowIndex * selectedConfig.tablesPerRow + tableIndex) * 2 + placeIndex
                                            val occupant = students.find { it.seatIndex == seatIdx }

                                            Box(modifier = Modifier.weight(1f)) {
                                                InteractiveSeatCard(
                                                    seatIndex = seatIdx,
                                                    student = occupant,
                                                    isOverviewMode = isGlobalOverviewMode,
                                                    isSwapSource = (seatToSwapFirst == seatIdx),
                                                    onClick = {
                                                        if (seatToSwapFirst != null) {
                                                            // On effectue l'échange avec cette place
                                                            val sourceSeat = seatToSwapFirst!!
                                                            if (sourceSeat != seatIdx) {
                                                                val firstStudent = students.find { it.seatIndex == sourceSeat }
                                                                val secondStudent = occupant
                                                                if (firstStudent != null && secondStudent != null) {
                                                                    onSwapSeats(firstStudent.id, sourceSeat, secondStudent.id, seatIdx)
                                                                } else if (firstStudent != null && secondStudent == null) {
                                                                    // Déplacement simple
                                                                    onAssignSeat(firstStudent.id, seatIdx)
                                                                }
                                                            }
                                                            seatToSwapFirst = null
                                                        } else {
                                                            if (occupant == null) {
                                                                selectedSeatForAssign = seatIdx
                                                            } else {
                                                                selectedSeatForOptions = seatIdx
                                                            }
                                                        }
                                                    }
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

    // --- DIALOGUE 1 : ASSIGNER UN ÉLÈVE À UNE PLACE ---
    if (selectedSeatForAssign != null) {
        val targetSeat = selectedSeatForAssign!!
        var studentSearchQuery by remember { mutableStateOf("") }

        val unseated = students.filter { it.seatIndex == null }
            .filter {
                studentSearchQuery.isBlank() ||
                it.lastName.contains(studentSearchQuery, ignoreCase = true) ||
                it.firstName.contains(studentSearchQuery, ignoreCase = true)
            }

        val seated = students.filter { it.seatIndex != null }
            .filter {
                studentSearchQuery.isBlank() ||
                it.lastName.contains(studentSearchQuery, ignoreCase = true) ||
                it.firstName.contains(studentSearchQuery, ignoreCase = true)
            }

        AlertDialog(
            onDismissRequest = { selectedSeatForAssign = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AirlineSeatReclineNormal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isAr) "المقعد ${targetSeat + 1}" else "Place ${targetSeat + 1}")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Barre de recherche fine et contrastée
                    FineSearchBar(
                        value = studentSearchQuery,
                        onValueChange = { studentSearchQuery = it },
                        placeholder = if (isAr) "البحث عن تلميذ..." else "Rechercher un élève...",
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .adaptiveDialogMaxHeight(0.55f, maxHeight = 340.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (unseated.isEmpty() && seated.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (studentSearchQuery.isNotBlank()) (if (isAr) "لا يوجد أي تلميذ مطابق" else "Aucun élève correspondant") else (if (isAr) "تم توزيع جميع التلاميذ في المقاعد !" else "Tous les élèves sont déjà placés !"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        } else {
                            if (unseated.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp, bottom = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF059669))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isAr) "غير المعينين (${unseated.size})" else "NON PLACÉS (${unseated.size})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF059669)
                                        )
                                    }
                                }

                                items(unseated, key = { it.id }) { st ->
                                    val isBoy = st.gender == "M"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                onAssignSeat(st.id, targetSeat)
                                                selectedSeatForAssign = null
                                            }
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(if (isBoy) Color(0xFFE0F2FE) else Color(0xFFFCE7F3)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = st.firstName.take(1).uppercase(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isBoy) Color(0xFF0284C7) else Color(0xFFDB2777)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${st.lastName.uppercase()} ${st.firstName}",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.AddCircleOutline,
                                            contentDescription = if (isAr) "تعيين" else "Assigner",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                                }
                            }

                            if (seated.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.secondary)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isAr) "نقل من مقعد آخر (${seated.size})" else "DÉPLACER DEPUIS UNE AUTRE PLACE (${seated.size})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                items(seated, key = { it.id }) { st ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                onAssignSeat(st.id, targetSeat)
                                                selectedSeatForAssign = null
                                            }
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${st.lastName.uppercase()} ${st.firstName}",
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = if (isAr) "حالياً : المقعد ${(st.seatIndex ?: 0) + 1}" else "Actuellement : Place ${(st.seatIndex ?: 0) + 1}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedSeatForAssign = null }) {
                    Text(if (isAr) "إغلاق" else "Fermer")
                }
            }
        )
    }

    // --- DIALOGUE 2 : OPTIONS DE PLACE OCCUPÉE ---
    if (selectedSeatForOptions != null) {
        val seatIdx = selectedSeatForOptions!!
        val occupant = students.find { it.seatIndex == seatIdx }

        if (occupant != null) {
            val isBoy = occupant.gender == "M"
            AlertDialog(
                onDismissRequest = { selectedSeatForOptions = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isBoy) Color(0xFFE0F2FE) else Color(0xFFFCE7F3)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = occupant.firstName.take(1).uppercase(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBoy) Color(0xFF0284C7) else Color(0xFFDB2777)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(if (isAr) "المقعد ${seatIdx + 1}" else "Place ${seatIdx + 1}")
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "${occupant.lastName.uppercase()} ${occupant.firstName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (occupant.gender == "M") (if (isAr) "ذكر" else "Garçon") else (if (isAr) "أنثى" else "Fille"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        // Bouton 1 : Changer d'élève
                        Button(
                            onClick = {
                                selectedSeatForOptions = null
                                selectedSeatForAssign = seatIdx
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PersonSearch, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isAr) "تغيير التلميذ" else "Changer d'élève")
                        }

                        // Bouton 2 : Échanger de place
                        FilledTonalButton(
                            onClick = {
                                seatToSwapFirst = seatIdx
                                selectedSeatForOptions = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.SwapCalls, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isAr) "تبديل مع مقعد آخر..." else "Échanger avec une autre place...")
                        }

                        // Bouton 3 : Libérer la place
                        OutlinedButton(
                            onClick = {
                                onFreeSeat(seatIdx)
                                selectedSeatForOptions = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isAr) "إفراغ المقعد" else "Libérer la place")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { selectedSeatForOptions = null }) {
                        Text(if (isAr) "إلغاء" else "Annuler")
                    }
                }
            )
        }
    }

    // --- BANNIÈRE D'ÉCHANGE DE PLACE EN COURS ---
    if (seatToSwapFirst != null) {
        val sourceSeat = seatToSwapFirst!!
        val sourceOccupant = students.find { it.seatIndex == sourceSeat }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isAr) "التبديل قيد التنفيذ..." else "Échange en cours...",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E),
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (isAr) "المس المقعد المستهدف للتبديل مع ${sourceOccupant?.lastName?.uppercase() ?: "المقعد ${sourceSeat + 1}"}" else "Touchez la place cible pour échanger avec ${sourceOccupant?.lastName?.uppercase() ?: "la place ${sourceSeat + 1}"}",
                        fontSize = 11.sp,
                        color = Color(0xFF78350F)
                    )
                }
                TextButton(onClick = { seatToSwapFirst = null }) {
                    Text(if (isAr) "إلغاء" else "Annuler", color = Color(0xFF92400E), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // --- CONFIRMATION AUTO ASSIGN ---
    if (showConfirmAutoAssignDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmAutoAssignDialog = false },
            title = { Text(if (isAr) "توزيع تلقائي (أ-ي)" else "Attribution automatique (A-Z)") },
            text = {
                Text(if (isAr) "هل تريد توزيع جميع تلاميذ القسم تلقائياً بالترتيب الأبجدي من المقعد 1 إلى المقعد ${students.size} ؟" else "Voulez-vous placer automatiquement tous les élèves de la classe par ordre alphabétique de la place 1 à la place ${students.size} ?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAutoAssign()
                        showConfirmAutoAssignDialog = false
                    }
                ) {
                    Text(if (isAr) "تأكيد" else "Confirmer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmAutoAssignDialog = false }) {
                    Text(if (isAr) "إلغاء" else "Annuler")
                }
            }
        )
    }

    // --- CONFIRMATION RÉINITIALISER ---
    if (showConfirmClearDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            title = { Text(if (isAr) "إفراغ مخطط الجلوس" else "Vider le plan de classe") },
            text = {
                Text(if (isAr) "هل تريد إفراغ جميع المقاعد المشغولة في هذا القسم ؟" else "Voulez-vous libérer toutes les places occupées dans cette classe ?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        showConfirmClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isAr) "إفراغ الكل" else "Vider tout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearDialog = false }) {
                    Text(if (isAr) "إلغاء" else "Annuler")
                }
            }
        )
    }

    // --- DIALOGUE DE PERSONNALISATION DES RANGÉES ET TABLES ---
    if (showCustomConfigDialog) {
        var tempRows by remember { mutableIntStateOf(selectedConfig.rowsCount) }
        var tempTables by remember { mutableIntStateOf(selectedConfig.tablesPerRow) }

        val totalCapacity = tempRows * tempTables * 2
        val maxAssignedIdx = students.mapNotNull { it.seatIndex }.maxOrNull() ?: -1
        val hasOverflowAssigned = maxAssignedIdx >= totalCapacity
        val isUnderStudentCount = totalCapacity < totalStudents

        AlertDialog(
            onDismissRequest = { showCustomConfigDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = if (isAr) "تخصيص أبعاد القاعة" else "Personnaliser la disposition",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (isAr) "حدد عدد الصفوف وعدد الطاولات المزدوجة في كل صف لمطابقة قاعة التدريس الخاصة بك :" else "Définissez le nombre de rangées et de tables doubles par rangée pour correspondre à votre salle de classe :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 1. SÉLECTEUR DE RANGÉES
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isAr) "عدد الصفوف" else "Nombre de rangées",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (isAr) "أعمدة القاعة (1 إلى 6)" else "Colonnes de la salle (1 à 6)",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilledIconButton(
                                        onClick = { if (tempRows > 1) tempRows-- },
                                        enabled = tempRows > 1,
                                        modifier = Modifier.size(32.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Moins", modifier = Modifier.size(16.dp))
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.widthIn(min = 36.dp).height(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                            Text(
                                                text = "$tempRows",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }

                                    FilledIconButton(
                                        onClick = { if (tempRows < 6) tempRows++ },
                                        enabled = tempRows < 6,
                                        modifier = Modifier.size(32.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Plus", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // Boutons d'accès rapide aux rangées
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                (1..5).forEach { r ->
                                    val isCurrent = tempRows == r
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { tempRows = r }
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 4.dp)) {
                                            Text(
                                                text = if (isAr) "$r ص" else "$r R",
                                                fontSize = 11.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. SÉLECTEUR DE TABLES PAR RANGÉE
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isAr) "طاولات في كل صف" else "Tables par rangée",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (isAr) "طاولات مزدوجة (1 إلى 8)" else "Tables doubles (1 à 8)",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilledIconButton(
                                        onClick = { if (tempTables > 1) tempTables-- },
                                        enabled = tempTables > 1,
                                        modifier = Modifier.size(32.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Moins", modifier = Modifier.size(16.dp))
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.widthIn(min = 36.dp).height(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                            Text(
                                                text = "$tempTables",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }

                                    FilledIconButton(
                                        onClick = { if (tempTables < 8) tempTables++ },
                                        enabled = tempTables < 8,
                                        modifier = Modifier.size(32.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Plus", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // Boutons d'accès rapide aux tables
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                (3..7).forEach { t ->
                                    val isCurrent = tempTables == t
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { tempTables = t }
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 4.dp)) {
                                            Text(
                                                text = if (isAr) "$t ط" else "$t T",
                                                fontSize = 11.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. RÉCAPITULATIF DE LA CAPACITÉ
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isAr) "السعة الإجمالية للقاعة :" else "Capacité totale de la salle :",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isAr) "$totalCapacity مقعد ($tempRows × $tempTables = ${tempRows * tempTables} طاولة)" else "$totalCapacity places ($tempRows × $tempTables = ${tempRows * tempTables} tables)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isAr) "تلاميذ هذا القسم :" else "Effectif de la classe :",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = if (isAr) "$totalStudents تلميذ ($seatedCount معينين)" else "$totalStudents élèves ($seatedCount placés)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            if (isUnderStudentCount) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isAr) "تنبيه : السعة ($totalCapacity) أقل من عدد التلاميذ ($totalStudents)" else "Attention : capacité ($totalCapacity) inférieure à l'effectif ($totalStudents)",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFFB45309),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (hasOverflowAssigned) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isAr) "بعض التلاميذ سيتم تجاوز مقاعدهم في هذا المخطط" else "Certains élèves placés dépasseront la nouvelle grille",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold
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
                        val total = tempRows * tempTables * 2
                        val matchingPreset = defaultConfigs.firstOrNull { it.rowsCount == tempRows && it.tablesPerRow == tempTables }
                        val newConfig = matchingPreset ?: SeatingGridConfig(
                            rowsCount = tempRows,
                            tablesPerRow = tempTables,
                            label = if (isAr) "$tempRows صفوف × $tempTables طاولات ($total مقعد)" else "$tempRows rangées × $tempTables tables ($total places)"
                        )
                        onConfigChange(newConfig)
                        showCustomConfigDialog = false
                    }
                ) {
                    Text(if (isAr) "تطبيق" else "Appliquer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomConfigDialog = false }) {
                    Text(if (isAr) "إلغاء" else "Annuler")
                }
            }
        )
    }
}

@Composable
fun InteractiveSeatCard(
    seatIndex: Int,
    student: StudentEntity?,
    isOverviewMode: Boolean,
    isSwapSource: Boolean,
    onClick: () -> Unit
) {
    val cardHeight = if (isOverviewMode) 52.dp else 64.dp
    val isAr = Tr.isArabic()
    val label = if (isAr) "م${seatIndex + 1}" else "P${seatIndex + 1}"

    if (student == null) {
        // Place libre
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .shadow(elevation = 1.dp, shape = RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isSwapSource) Brush.verticalGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A)))
                    else Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = if (isSwapSource) Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
                            else Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.55f),
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                                )
                            ),
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable(onClick = onClick)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isOverviewMode) 18.dp else 22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Assigner",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        modifier = Modifier.size(if (isOverviewMode) 13.dp else 16.dp)
                    )
                }
                Spacer(Modifier.height(1.dp))
                Text(
                    text = label,
                    fontSize = if (isOverviewMode) 8.sp else 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.70f)
                )
            }
        }
    } else {
        // Place occupée
        val isBoy = student.gender == "M"
        val bgBrush = if (isSwapSource) {
            Brush.verticalGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A)))
        } else if (isBoy) {
            Brush.verticalGradient(listOf(Color(0xFFF0F9FF), Color(0xFFE0F2FE), Color(0xFFBAE6FD)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFFFF1F2), Color(0xFFFCE7F3), Color(0xFFFBCFE8)))
        }

        val borderBrush = if (isSwapSource) {
            Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
        } else if (isBoy) {
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.90f), Color(0xFF38BDF8), Color(0xFF0284C7).copy(alpha = 0.70f)))
        } else {
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.90f), Color(0xFFF472B6), Color(0xFFDB2777).copy(alpha = 0.70f)))
        }

        val textC = if (isSwapSource) Color(0xFF92400E)
                    else if (isBoy) Color(0xFF034A75)
                    else Color(0xFF9D174D)

        val accentC = if (isSwapSource) Color(0xFFD97706)
                      else if (isBoy) Color(0xFF0284C7)
                      else Color(0xFFDB2777)

        val shadowColor = if (isSwapSource) Color(0xFFD97706).copy(alpha = 0.35f)
                          else if (isBoy) Color(0xFF0284C7).copy(alpha = 0.22f)
                          else Color(0xFFDB2777).copy(alpha = 0.22f)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .shadow(
                    elevation = if (isOverviewMode) 1.5.dp else 2.5.dp,
                    shape = RoundedCornerShape(10.dp),
                    ambientColor = shadowColor,
                    spotColor = shadowColor
                )
                .clip(RoundedCornerShape(10.dp))
                .background(bgBrush)
                .border(width = 1.2.dp, brush = borderBrush, shape = RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 3.5.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ligne supérieure discrète en filigrane : Badge numéro de place à gauche, pastille genre à droite
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = textC.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.6.dp, accentC.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = label,
                        fontSize = if (isOverviewMode) 7.sp else 8.sp,
                        lineHeight = if (isOverviewMode) 8.sp else 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = textC,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(accentC.copy(alpha = 0.75f))
                )
            }

            // Noms de l'élève (parfaitement centralisés horizontalement et verticalement, sans chevauchement)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Nom de famille
                Text(
                    text = if (isAr) student.lastName else student.lastName.uppercase(),
                    fontSize = if (isOverviewMode) 8.5.sp else 10.sp,
                    lineHeight = if (isOverviewMode) 9.5.sp else 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.2.sp,
                    color = textC,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Prénom
                Text(
                    text = if (isOverviewMode) "${student.firstName.take(1)}." else student.firstName,
                    fontSize = if (isOverviewMode) 7.5.sp else 9.sp,
                    lineHeight = if (isOverviewMode) 8.5.sp else 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textC.copy(alpha = 0.88f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
