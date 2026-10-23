package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ClassEntity
import com.example.data.TimetableSlotEntity
import com.example.ui.AppViewModel
import com.example.ui.ClassColorDot
import com.example.ui.components.GlassmorphicCard
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.example.ui.ClassColorHelper
import com.example.data.PdfExporter
import com.example.util.Tr
import com.example.util.tr
import com.example.util.AdaptiveScreenContainer
import com.example.util.adaptiveDialogMaxHeight
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetablesScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val isAr = Tr.isArabic()

    val classes by viewModel.allClasses.collectAsState()
    val selectedClassId by viewModel.selectedClassId.collectAsState()
    val teacherSlots by viewModel.teacherTimetableSlots.collectAsState()
    val classSlots by viewModel.currentTimetableSlots.collectAsState()
    val allTimetableSlots by viewModel.allTimetableSlots.collectAsState()
    val settingsState by viewModel.settings.collectAsState()

    var activeTab by remember { mutableStateOf(0) }
    val allSlots = if (activeTab == 0) teacherSlots else classSlots

    val teacherFreeDay = settingsState?.teacherFreeDay
    val teacherName = settingsState?.teacherName ?: ""
    val activeClass = classes.find { it.id == selectedClassId }

    var showAddSlotDialog by remember { mutableStateOf(false) }
    var slotToEdit by remember { mutableStateOf<TimetableSlotEntity?>(null) }
    var timeSlotToEdit by remember { mutableStateOf<String?>(null) }
    var slotToDelete by remember { mutableStateOf<TimetableSlotEntity?>(null) }
    var isLandscapeFullscreen by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showIntegratedPdfViewer by remember { mutableStateOf(false) }
    var showEditLunchBreakDialog by remember { mutableStateOf(false) }
    var showAddCustomTimeSlotDialog by remember { mutableStateOf(false) }
    var addMenuExpanded by remember { mutableStateOf(false) }
    var lunchBreakLabel by remember { mutableStateOf("Pause déjeuner") }
    var timeSlotToDeleteConfirm by remember { mutableStateOf<String?>(null) }

    // Persistent time slot replacements, deleted time slots and custom time slots from ViewModel
    val timeSlotReplacements by viewModel.timeSlotReplacements.collectAsState()
    val deletedTimeSlots by viewModel.deletedTimeSlots.collectAsState()
    val customTimeSlots by viewModel.customTimeSlots.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initTimetablePrefs(context)
    }

    var zoomScale by remember { mutableStateOf(1.0f) }
    var exportMenuExpanded by remember { mutableStateOf(false) }
    var showOnlyFilledSlotsLandscape by remember { mutableStateOf(false) }
    var selectedDayIndex by remember { mutableStateOf(1) } // 1..5 for Dimanche..Jeudi, 0 for Tous les jours

    val targetTimetableDay by viewModel.targetTimetableDay.collectAsState()
    LaunchedEffect(targetTimetableDay) {
        targetTimetableDay?.let { day ->
            selectedDayIndex = if (day in 1..5) day else 1
            viewModel.selectTimetableDay(null)
        }
    }

    // Pre-fill parameters when adding slot from a specific grid cell
    var initialSlotDay by remember { mutableStateOf(1) }
    var initialSlotTime by remember { mutableStateOf("08:00 - 09:00") }
    var showParentReceptionDialog by remember { mutableStateOf(false) }

    val parentReceptionSlot = remember(teacherSlots) {
        teacherSlots.find { it.subject.contains("réception", ignoreCase = true) || it.subject.contains("reception", ignoreCase = true) }
    }

    LaunchedEffect(activeTab, classes, selectedClassId) {
        if (activeTab == 1 && selectedClassId == null && classes.isNotEmpty()) {
            viewModel.selectClass(classes.first().id)
        }
    }

    // Orientation handling for Fullscreen Landscape mode
    val activity = context as? Activity
    DisposableEffect(isLandscapeFullscreen) {
        if (isLandscapeFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            if (!isLandscapeFullscreen) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    val daysOfWeek = listOf(
        stringResource(id = R.string.day_sunday),
        stringResource(id = R.string.day_monday),
        stringResource(id = R.string.day_tuesday),
        stringResource(id = R.string.day_wednesday),
        stringResource(id = R.string.day_thursday)
    )

    // Standard school time slots + any custom minute-specific slots created by the user
    fun decomposeSlotToUnitHours(slotStr: String): List<String> {
        val clean = slotStr.trim()
        val parts = clean.split("-", "à", "au").map { it.trim() }
        if (parts.size >= 2) {
            val startH = parts[0].split(":", "h").firstOrNull()?.toIntOrNull()
            val startM = parts[0].split(":", "h").getOrNull(1)?.toIntOrNull() ?: 0
            val endH = parts[1].split(":", "h").firstOrNull()?.toIntOrNull()
            val endM = parts[1].split(":", "h").getOrNull(1)?.toIntOrNull() ?: 0
            if (startH != null && endH != null && (endH * 60 + endM - (startH * 60 + startM)) >= 100) {
                val list = mutableListOf<String>()
                val sH: Int = startH
                val eH: Int = endH
                var curH = sH
                while (curH < eH) {
                    val nextH = curH + 1
                    val sStr = String.format(java.util.Locale.US, "%02d:%02d", curH, startM)
                    val eStr = String.format(java.util.Locale.US, "%02d:%02d", nextH, startM)
                    list.add("$sStr - $eStr")
                    curH = nextH
                }
                return list
            }
        }
        return listOf(clean)
    }

    val uniqueTimeSlots = remember(allSlots, timeSlotReplacements, deletedTimeSlots, customTimeSlots) {
        val raw = com.example.util.TimetableTimeSlotHelper.computeHarmonizedTimeSlots(
            allSlots = allSlots,
            timeSlotReplacements = timeSlotReplacements,
            deletedTimeSlots = deletedTimeSlots,
            customTimeSlots = customTimeSlots
        )
        val hasActivityIn1630 = allSlots.any { slot ->
            val sc = slot.timeSlot.replace(" ", "").replace("h", ":")
            (sc.contains("16:30-17:30") || (sc.startsWith("16:30") && sc.endsWith("17:30"))) &&
            slot.subject.isNotBlank()
        }
        raw.filter { s ->
            val clean = s.replace(" ", "").replace("h", ":")
            val is1630 = clean.contains("16:30-17:30") || (clean.startsWith("16:30") && clean.endsWith("17:30"))
            if (is1630) hasActivityIn1630 else true
        }
    }

    // Créneaux horaires affichés en mode agrandi plein écran : tous les créneaux harmonisés et personnalisés
    val landscapeTimeSlots = remember(allSlots, activeTab, selectedClassId, uniqueTimeSlots, showOnlyFilledSlotsLandscape, deletedTimeSlots, timeSlotReplacements, customTimeSlots) {
        if (!showOnlyFilledSlotsLandscape) {
            uniqueTimeSlots
        } else {
            val relevantSlots = if (activeTab == 0) allSlots.filter { it.classId == null } else allSlots.filter { it.classId == selectedClassId }
            val filled = com.example.util.TimetableTimeSlotHelper.computeHarmonizedTimeSlots(
                allSlots = relevantSlots,
                timeSlotReplacements = timeSlotReplacements,
                deletedTimeSlots = deletedTimeSlots,
                customTimeSlots = customTimeSlots
            )
            if (filled.isEmpty()) {
                uniqueTimeSlots
            } else {
                filled
            }
        }
    }

    val timetablePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let {
            val effYear = if (activeTab == 0) (settingsState?.currentSchoolYear ?: "") else (activeClass?.schoolYear?.ifBlank { settingsState?.currentSchoolYear ?: "" } ?: "")
            viewModel.exportSingleTimetablePdf(
                context = context,
                targetUri = it,
                className = if (activeTab == 0) (if (isAr) "جدول التوقيت" else "Enseignant") else (activeClass?.name ?: (if (isAr) "القسم" else "Classe")),
                slotsList = allSlots,
                lunchBreakLabel = lunchBreakLabel,
                deletedTimeSlots = deletedTimeSlots,
                timeSlotReplacements = timeSlotReplacements,
                schoolYear = effYear
            )
        }
    }

    val allTimetablesPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportAllTimetablesPdf(
                context = context,
                targetUri = it,
                slotsList = if (allTimetableSlots.isNotEmpty()) allTimetableSlots else allSlots,
                lunchBreakLabel = lunchBreakLabel,
                deletedTimeSlots = deletedTimeSlots,
                timeSlotReplacements = timeSlotReplacements
            )
        }
    }

    // --- FULLSCREEN LANDSCAPE VIEW ---
    if (isLandscapeFullscreen) {
        BackHandler { isLandscapeFullscreen = false }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                // Header bar in landscape mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (Tr.isArabic()) "نظرة عامة : ${if (activeTab == 0) "جدول توقيت الأستاذ" else (activeClass?.name ?: "القسم")}" else "Vue générale : ${if (activeTab == 0) "Emploi du temps Enseignant" else (activeClass?.name ?: "Classe")}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (activeTab == 0 && teacherFreeDay != null && teacherFreeDay in 1..daysOfWeek.size) {
                            Spacer(Modifier.width(10.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = if (Tr.isArabic()) "☕ يوم الراحة : ${daysOfWeek[teacherFreeDay - 1]}" else "☕ Journée libre : ${daysOfWeek[teacherFreeDay - 1]}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Zoom controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = { zoomScale = (zoomScale - 0.15f).coerceIn(0.25f, 2.2f) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Zoom -", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "${(zoomScale * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { zoomScale = 1.0f }
                                    .padding(horizontal = 4.dp)
                            )
                            IconButton(
                                onClick = { zoomScale = (zoomScale + 0.15f).coerceIn(0.25f, 2.2f) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Zoom +", modifier = Modifier.size(16.dp))
                            }
                        }

                        // Toggle créneaux remplis uniquement vs tous les créneaux
                        FilterChip(
                            selected = showOnlyFilledSlotsLandscape,
                            onClick = { showOnlyFilledSlotsLandscape = !showOnlyFilledSlotsLandscape },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (showOnlyFilledSlotsLandscape) Icons.Default.FilterAlt else Icons.Default.GridView,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = if (showOnlyFilledSlotsLandscape) (if (Tr.isArabic()) "الخانات الممتلئة" else "Cases remplies") else (if (Tr.isArabic()) "جميع الحصص" else "Tous créneaux"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            modifier = Modifier.height(34.dp)
                        )

                        // Quick Add slot in landscape
                        OutlinedButton(
                            onClick = {
                                initialSlotDay = 1
                                initialSlotTime = "08:00 - 09:00"
                                showAddSlotDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (Tr.isArabic()) "حصة" else "Créneau", fontSize = 11.sp)
                        }

                        // PDF button
                        IconButton(
                            onClick = {
                                timetablePdfLauncher.launch("Emploi_du_temps_${if (activeTab == 0) "Enseignant" else (activeClass?.name ?: "Classe")}.pdf")
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = MaterialTheme.colorScheme.tertiary)
                        }

                        // Exit landscape button
                        FilledTonalButton(
                            onClick = { isLandscapeFullscreen = false },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.FullscreenExit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (Tr.isArabic()) "خروج" else "Quitter", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // The Timetable Week Table with pinned days and zoom
                TimetableWeekTable(
                    daysOfWeek = daysOfWeek,
                    uniqueTimeSlots = landscapeTimeSlots,
                    allSlots = allSlots,
                    classes = classes,
                    activeTab = activeTab,
                    teacherFreeDay = teacherFreeDay,
                    teacherName = teacherName,
                    lunchBreakLabel = lunchBreakLabel,
                    zoomScale = zoomScale,
                    onEditLunchBreak = { showEditLunchBreakDialog = true },
                    onDeleteSlot = { slotToDelete = it },
                    onEditSlot = { slotToEdit = it },
                    onEditTimeSlot = { timeSlotToEdit = it },
                    onDeleteTimeSlot = { timeSlotToDeleteConfirm = it },
                    onAddSlotAt = { d, t ->
                        initialSlotDay = d
                        initialSlotTime = t
                        showAddSlotDialog = true
                    },
                    fitOverview = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            }
        }
    } else {
        // --- STANDARD PORTRAIT VIEW (Emploi du temps direct sans vue jour) ---
        AdaptiveScreenContainer(maxWidth = 1000.dp) {
            val screenScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(screenScrollState)
                    .padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            // --- 1. SÉPARATEURS DE PARTIES DE REGISTRE COLORÉS : ENSEIGNANT OU CLASSE ---
            val isDarkTabs = MaterialTheme.colorScheme.background != com.example.ui.theme.OffWhite
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Intercalaire 1 : "Enseignant" (Bleu Roi / Saphir Académique)
                val tab0Selected = activeTab == 0
                val tab0Color = if (isDarkTabs) Color(0xFF3B82F6) else Color(0xFF1D4ED8)
                val tab0Bg = if (tab0Selected) {
                    if (isDarkTabs) Color(0xFF1E3A8A) else Color(0xFF2563EB)
                } else {
                    if (isDarkTabs) Color(0xFF1E293B) else Color(0xFFDBEAFE)
                }
                val tab0TextColor = if (tab0Selected) Color.White else (if (isDarkTabs) Color(0xFF93C5FD) else Color(0xFF1E40AF))

                Surface(
                    shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                    color = tab0Bg,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (tab0Selected) 1.5.dp else 1.dp,
                        color = if (tab0Selected) tab0Color else tab0Color.copy(alpha = 0.35f)
                    ),
                    shadowElevation = if (tab0Selected) 4.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(if (tab0Selected) 44.dp else 38.dp)
                        .clickable {
                            activeTab = 0
                            viewModel.selectClass(null)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = tab0TextColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (Tr.isArabic()) "الأستاذ" else "Enseignant",
                            fontWeight = if (tab0Selected) FontWeight.ExtraBold else FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = tab0TextColor
                        )
                    }
                }

                // Intercalaire 2 : "Classes" (Ambre / Terracotta Chaleureux)
                val tab1Selected = activeTab == 1
                val tab1Color = if (isDarkTabs) Color(0xFFF59E0B) else Color(0xFFD97706)
                val tab1Bg = if (tab1Selected) {
                    if (isDarkTabs) Color(0xFF78350F) else Color(0xFFD97706)
                } else {
                    if (isDarkTabs) Color(0xFF1E293B) else Color(0xFFFEF3C7)
                }
                val tab1TextColor = if (tab1Selected) Color.White else (if (isDarkTabs) Color(0xFFFDE68A) else Color(0xFF92400E))

                Surface(
                    shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                    color = tab1Bg,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (tab1Selected) 1.5.dp else 1.dp,
                        color = if (tab1Selected) tab1Color else tab1Color.copy(alpha = 0.35f)
                    ),
                    shadowElevation = if (tab1Selected) 4.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(if (tab1Selected) 44.dp else 38.dp)
                        .clickable {
                            activeTab = 1
                            if (classes.isNotEmpty() && selectedClassId == null) {
                                viewModel.selectClass(classes.first().id)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = tab1TextColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (Tr.isArabic()) "الأفواج" else "Classes",
                            fontWeight = if (tab1Selected) FontWeight.ExtraBold else FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = tab1TextColor
                        )
                    }
                }
            }

                // --- 1B. CLASS SELECTOR (ONLY FOR CLASS TAB) ---
                if (activeTab == 1) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = if (Tr.isArabic()) "القسم :" else "Classe :",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { expanded = true },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    if (activeClass != null) {
                                        ClassColorDot(activeClass.id, activeClass.name, size = 12)
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = activeClass?.name ?: (if (Tr.isArabic()) "اختر قسماً" else "Choisir une classe"),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    classes.forEach { c ->
                                        val isSelected = selectedClassId == c.id
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    c.name,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
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
                                            onClick = {
                                                viewModel.selectClass(c.id)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 2. ACTION BUTTONS ROW (Export PDF, Ajout Créneau, Agrandir) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bouton Ajouter une séance de cours direct (créneau horaire masqué)
                        Button(
                            onClick = {
                                initialSlotDay = if (selectedDayIndex in 1..5) selectedDayIndex else 1
                                initialSlotTime = "08:00 - 09:00"
                                showAddSlotDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("add_slot_header_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = if (Tr.isArabic()) "إضافة حصة دراسية" else "Ajouter une séance",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (Tr.isArabic()) "إضافة" else "Ajouter", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        // Bouton d'exportation en PDF (Grille complète exportée en PDF)
                        Box {
                            FilledTonalButton(
                                onClick = { exportMenuExpanded = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = if (Tr.isArabic()) "تصدير PDF" else "Exporter PDF",
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }

                            DropdownMenu(
                                expanded = exportMenuExpanded,
                                onDismissRequest = { exportMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (Tr.isArabic()) "تصدير جدول التوقيت هذا (PDF)" else "Exporter cet emploi du temps (PDF)") },
                                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        exportMenuExpanded = false
                                        timetablePdfLauncher.launch("Emploi_du_temps_${if (activeTab == 0) "Enseignant" else (activeClass?.name ?: "Classe")}.pdf")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (Tr.isArabic()) "تصدير جميع جداول التوقيت (PDF)" else "Exporter TOUS les emplois du temps (PDF)") },
                                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
                                    onClick = {
                                        exportMenuExpanded = false
                                        allTimetablesPdfLauncher.launch("Tous_les_Emplois_du_Temps.pdf")
                                    }
                                )
                            }
                        }

                        // Bouton Agrandir en paysage plein écran
                        IconButton(
                            onClick = { isLandscapeFullscreen = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Agrandir en mode paysage",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // --- 3. CONTENU PRINCIPAL : HORAIRES PAR JOURS (N'affiche QUE les heures remplies) ---
                TimetableDailyView(
                    viewModel = viewModel,
                    parentReceptionSlot = parentReceptionSlot,
                    onShowParentReceptionDialog = { showParentReceptionDialog = true },
                    selectedDayIndex = selectedDayIndex,
                    onSelectDay = { selectedDayIndex = it },
                    daysOfWeek = daysOfWeek,
                    allSlots = allSlots,
                    classes = classes,
                    activeTab = activeTab,
                    teacherFreeDay = teacherFreeDay,
                    teacherName = teacherName,
                    onAddSlotForDay = { dayIdx ->
                        initialSlotDay = dayIdx
                        initialSlotTime = "08:00 - 09:00"
                        showAddSlotDialog = true
                    },
                    onEditSlot = { slotToEdit = it },
                    onDeleteSlot = { slotToDelete = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // --- 4. ADD SLOT DIALOG (AVEC PERSONNALISATION DES HORAIRES À LA MINUTE) ---
    if (showAddSlotDialog) {
        var slotDay by remember(initialSlotDay) { mutableStateOf(initialSlotDay.coerceIn(1, 5)) }

        // Parse initial start and end times
        val initialParsed = remember(initialSlotTime) {
            val parts = initialSlotTime.split("-").map { it.trim() }
            val sParts = parts.getOrNull(0)?.split(":")?.mapNotNull { it.toIntOrNull() } ?: listOf(8, 0)
            val eParts = parts.getOrNull(1)?.split(":")?.mapNotNull { it.toIntOrNull() } ?: listOf(9, 0)
            val sH = sParts.getOrElse(0) { 8 }.coerceIn(0, 23)
            val sM = sParts.getOrElse(1) { 0 }.coerceIn(0, 59)
            val eH = eParts.getOrElse(0) { sH + 1 }.coerceIn(0, 23)
            val eM = eParts.getOrElse(1) { sM }.coerceIn(0, 59)
            Pair(Pair(sH, sM), Pair(eH, eM))
        }

        var startHour by remember { mutableStateOf(initialParsed.first.first) }
        var startMinute by remember { mutableStateOf(initialParsed.first.second) }
        var endHour by remember { mutableStateOf(initialParsed.second.first) }
        var endMinute by remember { mutableStateOf(initialParsed.second.second) }

        var subject by remember { mutableStateOf("") }
        var classroom by remember { mutableStateOf("1") }
        var selectedColorHex by remember { mutableStateOf("#1F3A60") } // Default navy
        var slotClassId by remember { mutableStateOf(selectedClassId ?: (classes.firstOrNull()?.id)) }

        val colors = listOf("#1F3A60", "#10B981", "#D4AF37", "#EF4444", "#8B5CF6", "#F59E0B")

        // Helper to format slot time string
        val formattedTimeSlot = String.format(Locale.getDefault(), "%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute)

        AlertDialog(
            onDismissRequest = { showAddSlotDialog = false },
            title = { Text(stringResource(id = R.string.add_slot)) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .adaptiveDialogMaxHeight(0.65f, maxHeight = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Day selection
                    item {
                        Text(if (Tr.isArabic()) "اليوم :" else "Jour :", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (dayIdx in 1..daysOfWeek.size) {
                                val isSel = slotDay == dayIdx
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .clickable { slotDay = dayIdx }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (Tr.isArabic()) daysOfWeek[dayIdx - 1] else daysOfWeek[dayIdx - 1].take(3),
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Class selection (in teacher view)
                    if (activeTab == 0) {
                        item {
                            Text(if (Tr.isArabic()) "القسم :" else "Classe :", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            var clsExpanded by remember { mutableStateOf(false) }
                            val currSelClass = classes.find { it.id == slotClassId }

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = { clsExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                    if (currSelClass != null) {
                                        ClassColorDot(currSelClass.id, currSelClass.name, size = 12)
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(currSelClass?.name ?: (if (Tr.isArabic()) "اختر قسماً" else "Choisir une classe"))
                                }
                                DropdownMenu(expanded = clsExpanded, onDismissRequest = { clsExpanded = false }) {
                                    classes.forEach { c ->
                                        DropdownMenuItem(
                                            text = { Text(c.name, fontWeight = if (c.id == slotClassId) FontWeight.Bold else FontWeight.Normal) },
                                            leadingIcon = { ClassColorDot(c.id, c.name, size = 12) },
                                            onClick = {
                                                slotClassId = c.id
                                                if (subject.isBlank() || classes.any { it.name == subject } || subject == "Réception des parents" || subject == "استقبال أولياء الأمور") {
                                                    subject = c.name
                                                }
                                                selectedColorHex = String.format("#%06X", (0xFFFFFF and ClassColorHelper.getIntColorForClass(c.id, c.name)))
                                                clsExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Quick class chips and Parent reception chip
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SuggestionChip(
                                    onClick = {
                                        subject = if (isAr) "استقبال أولياء الأمور" else "Réception des parents"
                                        classroom = ""
                                        selectedColorHex = "#F59E0B"
                                    },
                                    label = { Text(if (isAr) "استقبال أولياء الأمور" else "Réception des parents", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Diversity3,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = Color(0xFFD97706)
                                        )
                                    }
                                )

                                classes.forEach { c ->
                                    val isSelected = slotClassId == c.id && subject == c.name
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            slotClassId = c.id
                                            subject = c.name
                                            selectedColorHex = String.format("#%06X", (0xFFFFFF and ClassColorHelper.getIntColorForClass(c.id, c.name)))
                                        },
                                        label = { Text(c.name, fontSize = 11.5.sp) },
                                        leadingIcon = { ClassColorDot(c.id, c.name, size = 10) }
                                    )
                                }
                            }
                        }
                    }

                    // --- MINUTE-LEVEL TIME CUSTOMIZER ---
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (Tr.isArabic()) "التوقيت بالدقيقة :" else "Horaire à la minute :", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = formattedTimeSlot,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                // Start & End time selectors
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Start Time Selector
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(if (Tr.isArabic()) "البداية :" else "Début :", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.outline)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            TimeUnitPicker(
                                                value = startHour,
                                                range = 7..20,
                                                label = "h",
                                                onValueChange = { startHour = it }
                                            )
                                            Text(":", fontWeight = FontWeight.Bold)
                                            TimeUnitPicker(
                                                value = startMinute,
                                                range = 0..59,
                                                label = "m",
                                                step = 5,
                                                onValueChange = { startMinute = it }
                                            )
                                        }
                                    }

                                    // End Time Selector
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(if (Tr.isArabic()) "النهاية :" else "Fin :", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.outline)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            TimeUnitPicker(
                                                value = endHour,
                                                range = 7..21,
                                                label = "h",
                                                onValueChange = { endHour = it }
                                            )
                                            Text(":", fontWeight = FontWeight.Bold)
                                            TimeUnitPicker(
                                                value = endMinute,
                                                range = 0..59,
                                                label = "m",
                                                step = 5,
                                                onValueChange = { endMinute = it }
                                            )
                                        }
                                    }
                                }

                                // Quick presets for standard durations
                                val isArDialog = Tr.isArabic()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        45 to if (isArDialog) "+45 د" else "+45m",
                                        60 to if (isArDialog) "+1 سا" else "+1h",
                                        90 to if (isArDialog) "+1 سا 30 د" else "+1h30"
                                    ).forEach { (durationMinutes, label) ->
                                        OutlinedButton(
                                            onClick = {
                                                val totalStart = startHour * 60 + startMinute
                                                val totalEnd = totalStart + durationMinutes
                                                endHour = (totalEnd / 60) % 24
                                                endMinute = totalEnd % 60
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(30.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text(if (Tr.isArabic()) "القسم" else "Classe") },
                            placeholder = { Text(if (Tr.isArabic()) "مثال: 1ث1، 2ع1..." else "Ex: 1AS1, 2AS...") },
                            modifier = Modifier.fillMaxWidth().testTag("timetables_slot_class_input"),
                            singleLine = true
                        )
                    }

                    if (!subject.contains("réception", ignoreCase = true) && !subject.contains("reception", ignoreCase = true) && !subject.contains("استقبال", ignoreCase = true)) {
                        item {
                            if (activeTab == 1) {
                                OutlinedTextField(
                                    value = classroom,
                                    onValueChange = { classroom = it },
                                    label = { Text(if (Tr.isArabic()) "اسم الأستاذ" else "Nom de l'enseignant") },
                                    placeholder = if (teacherName.isNotBlank()) { { Text(teacherName) } } else null,
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            } else {
                                OutlinedTextField(
                                    value = classroom,
                                    onValueChange = { classroom = it },
                                    label = { Text(stringResource(id = R.string.classroom)) },
                                    placeholder = { Text(if (Tr.isArabic()) "مثال: 12" else "Ex: 12") },
                                    leadingIcon = {
                                        Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = if (Tr.isArabic()) "لون الحصة :" else "Couleur du créneau :",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            colors.forEach { hex ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = selectedColorHex == hex
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { selectedColorHex = hex }
                                        .then(
                                            if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Séparation stricte des données : Enseignant (classId = null) vs Classe (classId = selectedClassId)
                        val effectiveClassId: Int? = if (activeTab == 0) null else selectedClassId
                        val effectiveSubject = if (activeTab == 0) {
                            if (subject.isBlank()) {
                                classes.find { it.id == slotClassId }?.name ?: "Classe"
                            } else subject
                        } else {
                            if (subject.isBlank()) "Matière" else subject
                        }

                        viewModel.addTimetableSlot(
                            classId = effectiveClassId,
                            dayOfWeek = slotDay,
                            timeSlot = formattedTimeSlot,
                            subject = effectiveSubject,
                            classroom = classroom,
                            colorHex = selectedColorHex
                        )
                        showAddSlotDialog = false
                    }
                ) {
                    Text(stringResource(id = R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSlotDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    // --- 4B. EDIT TIME SLOT DIALOG (MODIFICATION DIRECTE DES HEURES À LA MINUTE D'UN COURS) ---
    slotToEdit?.let { targetSlot ->
        EditTimeSlotDialog(
            slot = targetSlot,
            classes = classes,
            isStudentTimetable = activeTab == 1,
            defaultTeacherName = teacherName,
            onDismiss = { slotToEdit = null },
            onConfirm = { updatedSlot ->
                val oldSlotTime = targetSlot.timeSlot.trim()
                val newSlotTime = updatedSlot.timeSlot.trim()
                if (oldSlotTime.isNotBlank() && newSlotTime.isNotBlank() && oldSlotTime != newSlotTime) {
                    val otherUsingOld = allSlots.count { it.id != targetSlot.id && it.timeSlot.trim() == oldSlotTime }
                    if (otherUsingOld == 0) {
                        viewModel.updateTimeSlotGlobally(oldSlotTime, newSlotTime, context)
                    }
                }
                viewModel.updateTimetableSlot(updatedSlot)
                slotToEdit = null
            },
            onDelete = {
                val s = targetSlot
                slotToEdit = null
                slotToDelete = s
            }
        )
    }

    // --- 4C. EDIT TIME COLUMN DIALOG (MODIFICATION DIRECTE DE L'HORAIRE EN CLIQUANT SUR LA CASE DE L'HEURE) ---
    timeSlotToEdit?.let { currentTimeSlot ->
        EditTimeColumnDialog(
            initialTimeSlot = currentTimeSlot,
            onDismiss = { timeSlotToEdit = null },
            onConfirm = { newTimeSlot ->
                viewModel.updateTimeSlotGlobally(currentTimeSlot, newTimeSlot, context)
                timeSlotToEdit = null
            },
            onDelete = {
                val t = currentTimeSlot
                timeSlotToEdit = null
                timeSlotToDeleteConfirm = t
            }
        )
    }

    // --- 4D. DELETE TIME SLOT COLUMN CONFIRMATION DIALOG ---
    timeSlotToDeleteConfirm?.let { slotToDeleteStr ->
        AlertDialog(
            onDismissRequest = { timeSlotToDeleteConfirm = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(if (Tr.isArabic()) "حذف خانة التوقيت" else "Supprimer la case d'heure")
                }
            },
            text = {
                Text(if (Tr.isArabic()) "هل تريد حذف خانة التوقيت \"$slotToDeleteStr\" نهائياً وجميع الحصص المبرمجة فيها ؟" else "Voulez-vous supprimer définitivement le créneau d'heure \"$slotToDeleteStr\" ainsi que tous les cours qui y sont programmés ?")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteTimeSlotGlobally(slotToDeleteStr, context)
                        timeSlotToDeleteConfirm = null
                    }
                ) {
                    Text(if (Tr.isArabic()) "حذف" else "Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { timeSlotToDeleteConfirm = null }) {
                    Text(if (Tr.isArabic()) "إلغاء" else "Annuler")
                }
            }
        )
    }

    // --- 4E. EDIT LUNCH BREAK DIALOG (MODIFICATION DU TEXTE DE LA PAUSE DÉJEUNER) ---
    if (showEditLunchBreakDialog) {
        EditLunchBreakDialog(
            initialLabel = lunchBreakLabel,
            onDismiss = { showEditLunchBreakDialog = false },
            onConfirm = { newLabel ->
                lunchBreakLabel = newLabel
                showEditLunchBreakDialog = false
            }
        )
    }

    // --- 4E2. ADD CUSTOM TIME SLOT DIALOG (AJOUT DE NOUVEAUX CRÉNEAUX HORAIRES) ---
    if (showAddCustomTimeSlotDialog) {
        AddCustomTimeSlotDialog(
            onDismiss = { showAddCustomTimeSlotDialog = false },
            onConfirm = { newTimeSlot ->
                viewModel.addCustomTimeSlot(newTimeSlot, context)
                showAddCustomTimeSlotDialog = false
            }
        )
    }

    // --- 4F. INTEGRATED LANDSCAPE PDF VIEWER (VUE GLOBALE DU TABLEAU EN PAYSAGE) ---
    if (showIntegratedPdfViewer) {
        val schoolName = settingsState?.schoolName ?: ""
        val teacherName = settingsState?.teacherName ?: ""
        val taughtSubject = settingsState?.taughtSubject ?: ""
        val className = if (activeTab == 0) (if (Tr.isArabic()) "جدول التوقيت" else "Enseignant") else (activeClass?.name ?: (if (Tr.isArabic()) "القسم" else "Classe"))

        IntegratedTimetablePdfDialog(
            schoolName = schoolName,
            teacherName = teacherName,
            taughtSubject = taughtSubject,
            className = className,
            slotsList = allSlots,
            classesList = classes,
            teacherFreeDay = if (activeTab == 0) teacherFreeDay else null,
            lunchBreakLabel = lunchBreakLabel,
            deletedTimeSlots = deletedTimeSlots,
            timeSlotReplacements = timeSlotReplacements,
            customTimeSlots = customTimeSlots,
            schoolYear = if (activeTab == 0) (settingsState?.currentSchoolYear ?: "") else (activeClass?.schoolYear?.ifBlank { settingsState?.currentSchoolYear ?: "" } ?: ""),
            onDismiss = { showIntegratedPdfViewer = false },
            onExportPdf = {
                showIntegratedPdfViewer = false
                timetablePdfLauncher.launch("Emploi_du_temps_${if (activeTab == 0) "Enseignant" else (activeClass?.name ?: "Classe")}.pdf")
            },
            onSwitchToLandscapeGrid = {
                showIntegratedPdfViewer = false
                isLandscapeFullscreen = true
            }
        )
    }

    // --- 4G. PARENT RECEPTION DIALOG (CONFIGURATION DE L'HEURE DE RÉCEPTION DES PARENTS) ---
    if (showParentReceptionDialog) {
        ParentReceptionDialog(
            existingSlot = parentReceptionSlot,
            onDismiss = { showParentReceptionDialog = false },
            onSave = { dayOfWeek, timeSlot, classroom ->
                if (parentReceptionSlot != null) {
                    viewModel.updateTimetableSlot(
                        parentReceptionSlot.copy(
                            dayOfWeek = dayOfWeek,
                            timeSlot = timeSlot,
                            classroom = classroom,
                            subject = if (isAr) "استقبال أولياء الأمور" else "Réception des parents",
                            classId = null,
                            colorHex = "#F59E0B"
                        )
                    )
                } else {
                    viewModel.addTimetableSlot(
                        classId = null,
                        dayOfWeek = dayOfWeek,
                        timeSlot = timeSlot,
                        subject = if (isAr) "استقبال أولياء الأمور" else "Réception des parents",
                        classroom = classroom,
                        colorHex = "#F59E0B"
                    )
                }
                showParentReceptionDialog = false
            },
            onDelete = if (parentReceptionSlot != null) {
                {
                    slotToDelete = parentReceptionSlot
                    showParentReceptionDialog = false
                }
            } else null
        )
    }

    // --- 5. DELETE SLOT CONFIRMATION DIALOG ---
    slotToDelete?.let { slot ->
        AlertDialog(
            onDismissRequest = { slotToDelete = null },
            title = { Text(if (Tr.isArabic()) "تأكيد الحذف" else "Confirmer la suppression") },
            text = { Text(if (Tr.isArabic()) "هل تريد حذف هذه الحصة (${slot.subject}) من جدول التوقيت ؟" else "Voulez-vous supprimer ce cours (${slot.subject}) de l'emploi du temps ?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteTimetableSlot(slot.id)
                        slotToDelete = null
                    }
                ) {
                    Text(if (Tr.isArabic()) "حذف" else "Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { slotToDelete = null }) {
                    Text(if (Tr.isArabic()) "إلغاء" else "Annuler")
                }
            }
        )
    }
}

/**
 * Dialogue dédié pour modifier directement les heures de cours à la minute,
 * ainsi que la classe, matière et salle, avec suppression accessible.
 */
@Composable
private fun EditTimeSlotDialog(
    slot: TimetableSlotEntity,
    classes: List<ClassEntity>,
    isStudentTimetable: Boolean = false,
    defaultTeacherName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (TimetableSlotEntity) -> Unit,
    onDelete: () -> Unit
) {
    val isAr = Tr.isArabic()
    // Parse existing start and end time
    val parsedTimes = remember(slot.timeSlot) {
        val parts = slot.timeSlot.split("-").map { it.trim() }
        val sParts = parts.getOrNull(0)?.split(":")?.mapNotNull { it.toIntOrNull() } ?: listOf(8, 0)
        val eParts = parts.getOrNull(1)?.split(":")?.mapNotNull { it.toIntOrNull() } ?: listOf(9, 0)
        val sH = sParts.getOrElse(0) { 8 }.coerceIn(0, 23)
        val sM = sParts.getOrElse(1) { 0 }.coerceIn(0, 59)
        val eH = eParts.getOrElse(0) { sH + 1 }.coerceIn(0, 23)
        val eM = eParts.getOrElse(1) { sM }.coerceIn(0, 59)
        listOf(sH, sM, eH, eM)
    }

    var startHour by remember { mutableStateOf(parsedTimes[0]) }
    var startMinute by remember { mutableStateOf(parsedTimes[1]) }
    var endHour by remember { mutableStateOf(parsedTimes[2]) }
    var endMinute by remember { mutableStateOf(parsedTimes[3]) }

    var subject by remember { mutableStateOf(slot.subject) }
    var classroom by remember { mutableStateOf(slot.classroom) }
    var selectedClassId by remember { mutableStateOf(slot.classId) }
    var selectedColorHex by remember { mutableStateOf(slot.colorHex) }

    val formattedTimeSlot = remember(startHour, startMinute, endHour, endMinute) {
        String.format(Locale.getDefault(), "%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isAr) "تعديل الحصة" else "Modifier le cours",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = if (isAr) "حذف هذه الحصة" else "Supprimer ce cours",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Section Modification Horaires à la minute
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isAr) "تعديل التوقيت (بالدقيقة)" else "Modifier l'horaire (à la minute)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Heure de début
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isAr) "البداية :" else "Début :", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TimeUnitPicker(
                                    value = startHour,
                                    range = 7..21,
                                    label = "h",
                                    onValueChange = {
                                        startHour = it
                                        if (endHour < startHour || (endHour == startHour && endMinute <= startMinute)) {
                                            endHour = (startHour + 1).coerceAtMost(22)
                                        }
                                    }
                                )
                                Text(":", fontWeight = FontWeight.Bold)
                                TimeUnitPicker(
                                    value = startMinute,
                                    range = 0..59,
                                    label = "m",
                                    step = 5,
                                    onValueChange = {
                                        startMinute = it
                                        if (endHour == startHour && endMinute <= startMinute) {
                                            endMinute = (startMinute + 15) % 60
                                            if (endMinute <= startMinute) endHour = (startHour + 1).coerceAtMost(22)
                                        }
                                    }
                                )
                            }
                        }

                        // Heure de fin
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isAr) "النهاية :" else "Fin :", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TimeUnitPicker(
                                    value = endHour,
                                    range = 7..22,
                                    label = "h",
                                    onValueChange = { endHour = it }
                                )
                                Text(":", fontWeight = FontWeight.Bold)
                                TimeUnitPicker(
                                    value = endMinute,
                                    range = 0..59,
                                    label = "m",
                                    step = 5,
                                    onValueChange = { endMinute = it }
                                )
                            }
                        }

                        // Aperçu du créneau calculé
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = if (isAr) "الحصة : $formattedTimeSlot" else "Créneau : $formattedTimeSlot",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Classe associée
                if (classes.isNotEmpty() || !isStudentTimetable) {
                    Text(
                        text = if (isAr) "القسم / الحصة" else "Classe / Séance",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!isStudentTimetable) {
                            SuggestionChip(
                                onClick = {
                                    subject = if (isAr) "استقبال أولياء الأمور" else "Réception des parents"
                                    classroom = ""
                                    selectedColorHex = "#F59E0B"
                                    selectedClassId = null
                                },
                                label = { Text(if (isAr) "استقبال أولياء الأمور" else "Réception des parents", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Diversity3,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = Color(0xFFD97706)
                                    )
                                }
                            )
                        }

                        FilterChip(
                            selected = selectedClassId == null && !subject.contains("réception", ignoreCase = true) && !subject.contains("reception", ignoreCase = true) && !subject.contains("استقبال", ignoreCase = true),
                            onClick = { selectedClassId = null },
                            label = { Text(if (isAr) "عام" else "Général", fontSize = 12.sp) }
                        )
                        classes.forEach { c ->
                            FilterChip(
                                selected = selectedClassId == c.id,
                                onClick = {
                                    selectedClassId = c.id
                                    if (subject.isBlank() || classes.any { it.name == subject } || subject.contains("réception", ignoreCase = true) || subject.contains("reception", ignoreCase = true) || subject.contains("استقبال", ignoreCase = true)) {
                                        subject = c.name
                                    }
                                    selectedColorHex = String.format("#%06X", (0xFFFFFF and ClassColorHelper.getIntColorForClass(c.id, c.name)))
                                },
                                label = { Text(c.name, fontSize = 12.sp) },
                                leadingIcon = { ClassColorDot(c.id, c.name, size = 10) }
                            )
                        }
                    }
                }

                // Classe (au lieu de Matière au-dessus de Salle)
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text(if (isAr) "القسم" else "Classe") },
                    placeholder = { Text(if (isAr) "مثال: 1ث1، 2ع1..." else "Ex: 1AS1, 2AS...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("timetables_edit_slot_class_input")
                )

                // Salle ou Nom de l'enseignant (masqué pour la réception des parents)
                if (!subject.contains("réception", ignoreCase = true) && !subject.contains("reception", ignoreCase = true) && !subject.contains("استقبال", ignoreCase = true)) {
                    if (isStudentTimetable) {
                        OutlinedTextField(
                            value = classroom,
                            onValueChange = { classroom = it },
                            label = { Text(if (isAr) "اسم الأستاذ" else "Nom de l'enseignant") },
                            placeholder = if (defaultTeacherName.isNotBlank()) { { Text(defaultTeacherName) } } else null,
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = classroom,
                            onValueChange = { classroom = it },
                            label = { Text(stringResource(id = R.string.classroom)) },
                            placeholder = { Text(if (isAr) "مثال: 12، قاعة 104..." else "Ex: B12, Salle 104...") },
                            leadingIcon = {
                                Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetClassId = if (isStudentTimetable) (selectedClassId ?: slot.classId) else null
                    val isRec = subject.contains("réception", ignoreCase = true) || subject.contains("reception", ignoreCase = true) || subject.contains("استقبال", ignoreCase = true)
                    val updated = slot.copy(
                        timeSlot = formattedTimeSlot,
                        subject = subject.ifBlank { if (isStudentTimetable) (if (isAr) "مادة" else "Matière") else (if (isAr) "قسم" else "Classe") },
                        classroom = if (isRec) "" else classroom,
                        classId = targetClassId,
                        colorHex = selectedColorHex
                    )
                    onConfirm(updated)
                }
            ) {
                Text(stringResource(id = R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

/**
 * Dialogue interactif pour modifier les heures à la minute
 * directement en cliquant sur la case affichant l'heure dans la grille,
 * avec option de suppression du créneau.
 */
@Composable
private fun EditTimeColumnDialog(
    initialTimeSlot: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onDelete: () -> Unit
) {
    val isAr = Tr.isArabic()
    val parsedTimes = remember(initialTimeSlot) {
        val parts = initialTimeSlot.split("-").map { it.trim() }
        val sParts = parts.getOrNull(0)?.split(":")?.mapNotNull { it.toIntOrNull() } ?: listOf(8, 0)
        val eParts = parts.getOrNull(1)?.split(":")?.mapNotNull { it.toIntOrNull() } ?: listOf(9, 0)
        val sH = sParts.getOrElse(0) { 8 }.coerceIn(0, 23)
        val sM = sParts.getOrElse(1) { 0 }.coerceIn(0, 59)
        val eH = eParts.getOrElse(0) { sH + 1 }.coerceIn(0, 23)
        val eM = eParts.getOrElse(1) { sM }.coerceIn(0, 59)
        listOf(sH, sM, eH, eM)
    }

    var startHour by remember { mutableStateOf(parsedTimes[0]) }
    var startMinute by remember { mutableStateOf(parsedTimes[1]) }
    var endHour by remember { mutableStateOf(parsedTimes[2]) }
    var endMinute by remember { mutableStateOf(parsedTimes[3]) }

    val formattedTimeSlot = remember(startHour, startMinute, endHour, endMinute) {
        String.format(Locale.getDefault(), "%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isAr) "تعديل التوقيت" else "Modifier l'horaire",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isAr) "اضبط بدقة وقت البداية والنهاية لعمود التوقيت هذا (بالدقيقة) :" else "Ajustez avec précision l'heure de début et de fin de cette colonne horaire (à la minute) :",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Heure de début
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isAr) "البداية :" else "Début :", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TimeUnitPicker(
                                    value = startHour,
                                    range = 7..21,
                                    label = "h",
                                    onValueChange = {
                                        startHour = it
                                        if (endHour < startHour || (endHour == startHour && endMinute <= startMinute)) {
                                            endHour = (startHour + 1).coerceAtMost(22)
                                        }
                                    }
                                )
                                Text(":", fontWeight = FontWeight.Bold)
                                TimeUnitPicker(
                                    value = startMinute,
                                    range = 0..59,
                                    label = "m",
                                    step = 5,
                                    onValueChange = {
                                        startMinute = it
                                        if (endHour == startHour && endMinute <= startMinute) {
                                            endMinute = (startMinute + 15) % 60
                                            if (endMinute <= startMinute) endHour = (startHour + 1).coerceAtMost(22)
                                        }
                                    }
                                )
                            }
                        }

                        // Heure de fin
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isAr) "النهاية :" else "Fin :", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TimeUnitPicker(
                                    value = endHour,
                                    range = 7..22,
                                    label = "h",
                                    onValueChange = { endHour = it }
                                )
                                Text(":", fontWeight = FontWeight.Bold)
                                TimeUnitPicker(
                                    value = endMinute,
                                    range = 0..59,
                                    label = "m",
                                    step = 5,
                                    onValueChange = { endMinute = it }
                                )
                            }
                        }

                        // Badge nouvel horaire
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = if (isAr) "التوقيت الجديد : $formattedTimeSlot" else "Nouvel horaire : $formattedTimeSlot",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Bouton de suppression de ce créneau
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isAr) "حذف خانة التوقيت هذه" else "Supprimer ce créneau d'heure")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(formattedTimeSlot) }
            ) {
                Text(stringResource(id = R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

/**
 * Dialogue d'édition du texte de la pause déjeuner
 */
@Composable
private fun EditLunchBreakDialog(
    initialLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var labelText by remember { mutableStateOf(initialLabel) }
    val suggestions = listOf("Pause déjeuner", "Pause de midi", "Déjeuner", "Repas & Repos", "استراحة الغداء")

    val isAr = Tr.isArabic()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(8.dp))
                Text(if (isAr) "استراحة الغداء (بعد الساعة 12)" else "Pause déjeuner (après 12H)")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isAr) "تخصيص النص المعروض في خانة استراحة الغداء في جدول التوقيت :" else "Personnalisez le libellé affiché dans la case de pause déjeuner de l'emploi du temps :",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = labelText,
                    onValueChange = { labelText = it },
                    label = { Text(if (isAr) "نص الاستراحة" else "Texte de la pause") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = if (isAr) "اقتراحات سريعة :" else "Suggestions rapides :",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestions.forEach { sug ->
                        SuggestionChip(
                            onClick = { labelText = sug },
                            label = { Text(sug, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(labelText.ifBlank { if (isAr) "استراحة الغداء" else "Pause déjeuner" }) }
            ) {
                Text(stringResource(id = R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

/**
 * Dialogue d'ajout d'un nouveau créneau horaire personnalisé
 * (ex: 07:30 - 08:30, 12:00 - 13:00, 12:30 - 13:30, 17:00 - 18:00, 17:30 - 18:30, etc.)
 */
@Composable
private fun AddCustomTimeSlotDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val isAr = Tr.isArabic()
    var startHour by remember { mutableStateOf(8) }
    var startMinute by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(9) }
    var endMinute by remember { mutableStateOf(0) }

    val quickSuggestions = listOf(
        "07:30 - 08:30",
        "08:00 - 09:00",
        "11:30 - 12:30",
        "12:00 - 13:00",
        "12:30 - 13:30",
        "13:00 - 14:00",
        "16:30 - 17:30",
        "17:00 - 18:00",
        "17:30 - 18:30",
        "18:00 - 19:00"
    )

    val currentFormatted = String.format(java.util.Locale.US, "%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute)

    val isValid = remember(currentFormatted) {
        val parsed = com.example.util.TimetableTimeSlotHelper.parseSlotInterval(currentFormatted)
        parsed != null && parsed.startMin < parsed.endMin
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(if (isAr) "إضافة حصة توقيت" else "Ajouter un créneau horaire")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isAr) "اضبط وقت البداية والنهاية أو اختر اقتراحاً سريعاً :" else "Configurez les heures de début et de fin ou choisissez une suggestion rapide :",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Sélecteurs Heure de début et Heure de fin
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Début
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isAr) "البداية :" else "Début :", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilledTonalButton(
                                    onClick = { startHour = if (startHour > 6) startHour - 1 else 22 },
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) { Text("-") }
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = String.format(java.util.Locale.US, "%02dH", startHour),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    modifier = Modifier.width(42.dp),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.width(4.dp))
                                FilledTonalButton(
                                    onClick = { startHour = if (startHour < 22) startHour + 1 else 6 },
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) { Text("+") }

                                Spacer(Modifier.width(8.dp))

                                FilterChip(
                                    selected = startMinute == 0,
                                    onClick = { startMinute = 0 },
                                    label = { Text("00") }
                                )
                                Spacer(Modifier.width(4.dp))
                                FilterChip(
                                    selected = startMinute == 30,
                                    onClick = { startMinute = 30 },
                                    label = { Text("30") }
                                )
                            }
                        }

                        // Fin
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isAr) "النهاية :" else "Fin :", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilledTonalButton(
                                    onClick = { endHour = if (endHour > 7) endHour - 1 else 23 },
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) { Text("-") }
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = String.format(java.util.Locale.US, "%02dH", endHour),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    modifier = Modifier.width(42.dp),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.width(4.dp))
                                FilledTonalButton(
                                    onClick = { endHour = if (endHour < 23) endHour + 1 else 7 },
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) { Text("+") }

                                Spacer(Modifier.width(8.dp))

                                FilterChip(
                                    selected = endMinute == 0,
                                    onClick = { endMinute = 0 },
                                    label = { Text("00") }
                                )
                                Spacer(Modifier.width(4.dp))
                                FilterChip(
                                    selected = endMinute == 30,
                                    onClick = { endMinute = 30 },
                                    label = { Text("30") }
                                )
                            }
                        }
                    }
                }

                // Aperçu du créneau
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isAr) "الحصة المحددة :" else "Créneau généré :", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = currentFormatted,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = if (isAr) "اقتراحات سريعة :" else "Suggestions rapides :",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickSuggestions.forEach { sug ->
                        SuggestionChip(
                            onClick = {
                                val parsed = com.example.util.TimetableTimeSlotHelper.parseSlotInterval(sug)
                                if (parsed != null) {
                                    startHour = parsed.startMin / 60
                                    startMinute = parsed.startMin % 60
                                    endHour = parsed.endMin / 60
                                    endMinute = parsed.endMin % 60
                                }
                            },
                            label = { Text(sug, fontSize = 11.5.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(currentFormatted)
                    }
                },
                enabled = isValid
            ) {
                Text(if (isAr) "إضافة" else "Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isAr) "إلغاء" else "Annuler")
            }
        }
    )
}

/**
 * Dialogue dédié pour configurer l'heure de réception des parents
 * dans l'emploi du temps de l'enseignant.
 */
@Composable
private fun ParentReceptionDialog(
    existingSlot: TimetableSlotEntity?,
    onDismiss: () -> Unit,
    onSave: (dayOfWeek: Int, timeSlot: String, classroom: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val isAr = Tr.isArabic()
    val daysOfWeek = if (isAr) listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس") else listOf("Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi")
    var selectedDay by remember(existingSlot) { mutableStateOf(existingSlot?.dayOfWeek ?: 1) }

    val initialParsed = remember(existingSlot?.timeSlot) {
        val raw = existingSlot?.timeSlot ?: "10:00 - 11:00"
        val parts = raw.split("-").map { it.trim() }
        val sParts = parts.getOrNull(0)?.split(":")?.mapNotNull { it.toIntOrNull() } ?: listOf(10, 0)
        val eParts = parts.getOrNull(1)?.split(":")?.mapNotNull { it.toIntOrNull() } ?: listOf(11, 0)
        val sH = sParts.getOrElse(0) { 10 }.coerceIn(0, 23)
        val sM = sParts.getOrElse(1) { 0 }.coerceIn(0, 59)
        val eH = eParts.getOrElse(0) { sH + 1 }.coerceIn(0, 23)
        val eM = eParts.getOrElse(1) { sM }.coerceIn(0, 59)
        Pair(Pair(sH, sM), Pair(eH, eM))
    }

    var startHour by remember { mutableStateOf(initialParsed.first.first) }
    var startMinute by remember { mutableStateOf(initialParsed.first.second) }
    var endHour by remember { mutableStateOf(initialParsed.second.first) }
    var endMinute by remember { mutableStateOf(initialParsed.second.second) }

    val formattedTimeSlot = String.format(Locale.getDefault(), "%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Diversity3,
                    contentDescription = null,
                    tint = Color(0xFFD97706)
                )
                Text(
                    text = if (isAr) "استقبال أولياء الأمور" else "Réception des parents",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF92400E)
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = if (isAr) "اختر اليوم والساعة الأسبوعية لاستقبال أولياء التلاميذ :" else "Choisissez le jour et l'heure hebdomadaire de réception des parents d'élèves :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Jour de réception
                item {
                    Text(if (isAr) "يوم الاستقبال :" else "Jour de réception :", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (dayIdx in 1..daysOfWeek.size) {
                            val isSel = selectedDay == dayIdx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0xFFD97706) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .clickable { selectedDay = dayIdx }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isAr) daysOfWeek[dayIdx - 1] else daysOfWeek[dayIdx - 1].take(3),
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Horaire à la minute
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7).copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (isAr) "توقيت الحصة :" else "Horaire du créneau :", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = Color(0xFF92400E))
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFD97706)
                                ) {
                                    Text(
                                        text = formattedTimeSlot,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // Start & End
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(if (isAr) "البداية :" else "Début :", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF92400E))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        TimeUnitPicker(
                                            value = startHour,
                                            range = 7..20,
                                            label = "h",
                                            onValueChange = {
                                                startHour = it
                                                if (endHour < startHour || (endHour == startHour && endMinute <= startMinute)) {
                                                    endHour = (startHour + 1).coerceAtMost(21)
                                                }
                                            }
                                        )
                                        Text(":", fontWeight = FontWeight.Bold)
                                        TimeUnitPicker(
                                            value = startMinute,
                                            range = 0..59,
                                            label = "m",
                                            step = 5,
                                            onValueChange = {
                                                startMinute = it
                                                if (endHour == startHour && endMinute <= startMinute) {
                                                    endMinute = (startMinute + 15) % 60
                                                }
                                            }
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(if (isAr) "النهاية :" else "Fin :", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF92400E))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        TimeUnitPicker(
                                            value = endHour,
                                            range = 7..21,
                                            label = "h",
                                            onValueChange = { endHour = it }
                                        )
                                        Text(":", fontWeight = FontWeight.Bold)
                                        TimeUnitPicker(
                                            value = endMinute,
                                            range = 0..59,
                                            label = "m",
                                            step = 5,
                                            onValueChange = { endMinute = it }
                                        )
                                    }
                                }
                            }

                            // Presets
                            val isArPresets = Tr.isArabic()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    30 to if (isArPresets) "+30 د" else "+30m",
                                    45 to if (isArPresets) "+45 د" else "+45m",
                                    60 to if (isArPresets) "+1 سا" else "+1h",
                                    90 to if (isArPresets) "+1 سا 30 د" else "+1h30"
                                ).forEach { (durationMinutes, label) ->
                                    OutlinedButton(
                                        onClick = {
                                            val totalStart = startHour * 60 + startMinute
                                            val totalEnd = totalStart + durationMinutes
                                            endHour = (totalEnd / 60) % 24
                                            endMinute = totalEnd % 60
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(30.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.5f))
                                    ) {
                                        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF92400E))
                                    }
                                }
                            }
                        }
                    }
                }

                if (onDelete != null) {
                    item {
                        OutlinedButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isAr) "حذف ساعة الاستقبال هذه" else "Supprimer cette heure de réception")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                onClick = {
                    onSave(selectedDay, formattedTimeSlot, "")
                }
            ) {
                Text(if (isAr) "حفظ" else "Enregistrer", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

/**
 * Dialogue plein écran avec visualiseur PDF intégré en mode paysage
 */
@Composable
fun IntegratedTimetablePdfDialog(
    schoolName: String,
    teacherName: String,
    taughtSubject: String,
    className: String,
    slotsList: List<TimetableSlotEntity>,
    classesList: List<ClassEntity>,
    teacherFreeDay: Int?,
    lunchBreakLabel: String,
    deletedTimeSlots: Set<String>,
    timeSlotReplacements: Map<String, String> = emptyMap(),
    customTimeSlots: Set<String> = emptySet(),
    schoolYear: String = "",
    onDismiss: () -> Unit,
    onExportPdf: () -> Unit,
    onSwitchToLandscapeGrid: () -> Unit
) {
    val context = LocalContext.current
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var zoomScale by remember { mutableStateOf(1f) }

    LaunchedEffect(slotsList, classesList, teacherFreeDay, lunchBreakLabel, deletedTimeSlots, timeSlotReplacements, customTimeSlots) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val tempFile = File(context.cacheDir, "temp_timetable_preview.pdf")
                tempFile.outputStream().use { fos ->
                    PdfExporter.generateSingleTimetablePdf(
                        outputStream = fos,
                        schoolName = schoolName,
                        teacherName = teacherName,
                        taughtSubject = taughtSubject,
                        className = className,
                        slotsList = slotsList,
                        classesList = classesList,
                        freeDayIndex = teacherFreeDay,
                        lunchBreakLabel = lunchBreakLabel,
                        deletedTimeSlots = deletedTimeSlots,
                        timeSlotReplacements = timeSlotReplacements,
                        customTimeSlots = customTimeSlots,
                        schoolYear = schoolYear
                    )
                }
                ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        if (renderer.pageCount > 0) {
                            renderer.openPage(0).use { page ->
                                // Render landscape A4 in high definition
                                val renderWidth = (842 * 2.2f).toInt()
                                val renderHeight = (595 * 2.2f).toInt()
                                val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                previewBitmap = bitmap
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            previewBitmap = null
            File(context.cacheDir, "temp_timetable_preview.pdf").delete()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Toolbar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            val isAr = Tr.isArabic()
                            Column {
                                Text(
                                    text = if (isAr) "معاينة شاملة أفقية (PDF)" else "Aperçu Global Paysage (PDF)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (isAr) "جدول التوقيت صيغة A4 أفقية — $className" else "Emploi du temps format A4 Paysage — $className",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isAr = Tr.isArabic()
                            // Zoom controls
                            IconButton(
                                onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.75f) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("${(zoomScale * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            IconButton(
                                onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(3.0f) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(Modifier.width(6.dp))

                            // Switch to interactive landscape grid
                            OutlinedButton(
                                onClick = onSwitchToLandscapeGrid,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (isAr) "شبكة أفقية" else "Grille Paysage", fontSize = 11.sp)
                            }

                            // Export PDF
                            Button(
                                onClick = onExportPdf,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (isAr) "تصدير PDF" else "Exporter PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Close
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = if (isAr) "إغلاق" else "Fermer")
                            }
                        }
                    }
                }

                // PDF Viewer Content Area
                val isAr = Tr.isArabic()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .background(Color(0xFF2C3E50))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(Modifier.height(12.dp))
                            Text(if (isAr) "جارٍ إنشاء معاينة PDF..." else "Génération de l'aperçu PDF...", color = Color.White, fontSize = 13.sp)
                        }
                    } else if (previewBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(6.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(
                                        width = (842.dp * zoomScale),
                                        height = (595.dp * zoomScale)
                                    )
                            ) {
                                Image(
                                    bitmap = previewBitmap!!.asImageBitmap(),
                                    contentDescription = if (isAr) "جدول التوقيت أفقياً PDF" else "Emploi du temps Paysage PDF",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } else {
                        Text(if (isAr) "تعذر تحميل معاينة PDF" else "Impossible de charger l'aperçu PDF", color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Interactive unit picker for hours or minutes
 */
@Composable
private fun TimeUnitPicker(
    value: Int,
    range: IntRange,
    label: String,
    step: Int = 1,
    onValueChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.clickable { expanded = true }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                val displayLabel = if (Tr.isArabic()) {
                    when (label.trim()) {
                        "h" -> "سا"
                        "m" -> "د"
                        else -> label
                    }
                } else label

                Text(
                    text = String.format(Locale.getDefault(), "%02d", value),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = displayLabel,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 200.dp)
        ) {
            val stepValues = range.filter { if (step > 1) it % step == 0 else true }
            stepValues.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = String.format(Locale.getDefault(), "%02d %s", item, label),
                            fontWeight = if (item == value) FontWeight.Bold else FontWeight.Normal,
                            color = if (item == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onValueChange(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Carte d'affichage d'un créneau individuel dans la vue quotidienne
 */
@Composable
private fun TimetableDailySlotCard(
    slot: TimetableSlotEntity,
    classes: List<ClassEntity>,
    activeTab: Int,
    teacherName: String,
    onEditSlot: (TimetableSlotEntity) -> Unit,
    onDeleteSlot: (TimetableSlotEntity) -> Unit
) {
    val cleanSubject = slot.subject.trim()
    val isParentReception = cleanSubject.contains("réception", ignoreCase = true) || cleanSubject.contains("reception", ignoreCase = true)
    val matchedClass = if (slot.classId != null) classes.find { it.id == slot.classId } else {
        classes.find {
            cleanSubject.equals(it.name, ignoreCase = true) ||
            cleanSubject.startsWith(it.name, ignoreCase = true) ||
            it.name.startsWith(cleanSubject, ignoreCase = true)
        }
    }
    val cName = matchedClass?.name ?: ""
    val displayClassName = if (isParentReception) "Réception des parents" else (if (cName.isNotBlank()) cName else slot.subject.ifBlank { if (Tr.isArabic()) "القسم" else "Classe" })
    val showSubject = !isParentReception && cleanSubject.isNotBlank() &&
        !cleanSubject.equals(displayClassName.trim(), ignoreCase = true) &&
        !cleanSubject.equals(cName.trim(), ignoreCase = true)

    val palette = if (isParentReception) {
        com.example.ui.ClassColorPalette(
            primary = Color(0xFFD97706),
            container = Color(0xFFFEF3C7),
            onContainer = Color(0xFF92400E),
            label = "Réception"
        )
    } else if (activeTab == 1) {
        com.example.ui.SubjectColorHelper.getPaletteForSubject(slot.subject)
    } else if (cleanSubject.contains("2SE", ignoreCase = true) || (matchedClass?.name?.contains("2SE", ignoreCase = true) == true) || slot.classroom.contains("2SE", ignoreCase = true)) {
        ClassColorHelper.Palette2SE
    } else if (matchedClass != null) {
        ClassColorHelper.getPaletteForClass(matchedClass.id, matchedClass.name)
    } else {
        ClassColorHelper.getPaletteForClass(slot.classId ?: 0, cleanSubject.ifBlank { if (Tr.isArabic()) "القسم" else "Classe" })
    }

    val isDarkSlot = MaterialTheme.colorScheme.background != com.example.ui.theme.OffWhite
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        tintColor = palette.primary,
        tintAlpha = if (isDarkSlot) 0.08f else 0.05f,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val (startH, endH) = com.example.util.getSlotHoursTwoLines(slot.timeSlot)
                val hoursTextColor = if (isDarkSlot) Color.White else palette.primary

                // Affichage vertical (sur deux lignes), agrandi et vivant des heures
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDarkSlot) palette.primary.copy(alpha = 0.55f).compositeOver(Color(0xFF1E293B)) else palette.container.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.2.dp,
                        color = if (isDarkSlot) Color.White.copy(alpha = 0.5f) else palette.primary.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = startH,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.5.sp,
                            lineHeight = 15.sp,
                            letterSpacing = 0.5.sp,
                            textAlign = TextAlign.Center,
                            color = hoursTextColor
                        )
                        Text(
                            text = endH,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.5.sp,
                            lineHeight = 15.sp,
                            letterSpacing = 0.5.sp,
                            textAlign = TextAlign.Center,
                            color = hoursTextColor
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (activeTab == 1) {
                        // --- EMPLOI DU TEMPS DES ÉLÈVES / CLASSE ---
                        val displaySubject = slot.subject.ifBlank { if (Tr.isArabic()) "المادة" else "Matière" }

                        // 1. Matière mise en valeur (Badge coloré de la matière)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDarkSlot) palette.primary.copy(alpha = 0.25f) else palette.container.copy(alpha = 0.95f),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, palette.primary.copy(alpha = 0.75f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(palette.primary)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = displaySubject,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.3.sp,
                                    color = if (isDarkSlot) Color.White else palette.onContainer
                                )
                            }
                        }

                        // 2. Nom de l'enseignant (sans préfixe "Salle" ou "قاعة", avec icône Person)
                        val rawTeacher = slot.classroom.trim()
                        val cleanTeacher = rawTeacher.replace(Regex("(?i)^salle\\s*"), "").replace(Regex("^(?i)قاعة\\s*"), "").trim()
                        val teacherDisplay = cleanTeacher.ifBlank { if (Tr.isArabic()) "الأستاذ" else "Enseignant" }

                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = if (isDarkSlot) Color(0xFF0F766E).copy(alpha = 0.30f) else Color(0xFFCCFBF1).copy(alpha = 0.90f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkSlot) Color(0xFF2DD4BF).copy(alpha = 0.65f) else Color(0xFF0D9488).copy(alpha = 0.55f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (isDarkSlot) Color(0xFF2DD4BF) else Color(0xFF0D9488),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = teacherDisplay,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkSlot) Color(0xFF99F6E4) else Color(0xFF0F766E)
                                )
                            }
                        }
                    } else {
                        // --- EMPLOI DU TEMPS DE L'ENSEIGNANT ---
                        if (isParentReception) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Diversity3,
                                    contentDescription = null,
                                    tint = palette.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Réception des parents",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = palette.primary
                                )
                            }
                        } else {
                            // 1. Nom de la classe dans un rectangle coloré aux couleurs de la classe
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDarkSlot) palette.primary.copy(alpha = 0.25f) else palette.container.copy(alpha = 0.95f),
                                border = androidx.compose.foundation.BorderStroke(1.2.dp, palette.primary.copy(alpha = 0.75f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    ClassColorDot(matchedClass?.id ?: 0, displayClassName, size = 11)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = displayClassName,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.5.sp,
                                        letterSpacing = 0.3.sp,
                                        color = if (isDarkSlot) Color.White else palette.onContainer
                                    )
                                }
                            }
                        }

                        // 2. Numéro de salle colorée SOUS le nom de la classe
                        if (slot.classroom.isNotBlank() && !isParentReception) {
                            val roomClean = slot.classroom.replace(Regex("(?i)^salle\\s*"), "").trim()
                            val displayRoom = if (roomClean.isNotBlank()) {
                                if (roomClean.matches(Regex("(?i)^(labo|amphi|bureau|atelier).*"))) roomClean 
                                else if (Tr.isArabic()) "قاعة $roomClean" else "Salle $roomClean"
                            } else slot.classroom

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isDarkSlot) Color(0xFF134E4A).copy(alpha = 0.55f) else Color(0xFFCCFBF1).copy(alpha = 0.95f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkSlot) Color(0xFF2DD4BF) else Color(0xFF14B8A6).copy(alpha = 0.85f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MeetingRoom,
                                        contentDescription = null,
                                        tint = if (isDarkSlot) Color(0xFF5EEAD4) else Color(0xFF0D9488),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = displayRoom,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkSlot) Color(0xFFCCFBF1) else Color(0xFF0F766E)
                                    )
                                }
                            }
                        }

                        // 3. Matière si distincte
                        if (showSubject) {
                            Text(
                                text = cleanSubject,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.onContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onEditSlot(slot) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Modifier",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { onDeleteSlot(slot) },
                    modifier = Modifier.size(32.dp)
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

/**
 * Vue quotidienne de l'emploi du temps affichant les cours JOUR PAR JOUR,
 * avec sélection du jour directement intégrée dans l'encadré des horaires pour un gain d'espace optimal.
 */
@Composable
private fun TimetableDailyView(
    viewModel: com.example.ui.AppViewModel,
    parentReceptionSlot: com.example.data.TimetableSlotEntity?,
    onShowParentReceptionDialog: () -> Unit,
    selectedDayIndex: Int,
    onSelectDay: (Int) -> Unit,
    daysOfWeek: List<String>,
    allSlots: List<TimetableSlotEntity>,
    classes: List<ClassEntity>,
    activeTab: Int,
    teacherFreeDay: Int?,
    teacherName: String = "",
    onAddSlotForDay: (Int) -> Unit,
    onEditSlot: (TimetableSlotEntity) -> Unit,
    onDeleteSlot: (TimetableSlotEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDayLabel = if (selectedDayIndex in 1..daysOfWeek.size) {
        val dName = daysOfWeek[selectedDayIndex - 1]
        val isFree = activeTab == 0 && teacherFreeDay == selectedDayIndex
        val dCount = allSlots.count { it.dayOfWeek == selectedDayIndex }
        if (isFree) {
            if (Tr.isArabic()) "$dName (راحة)" else "$dName (Repos)"
        } else {
            if (Tr.isArabic()) "$dName ($dCount حصص)" else "$dName ($dCount cours)"
        }
    } else {
        if (Tr.isArabic()) "كامل الأسبوع (${allSlots.size} حصة)" else "Semaine complète (${allSlots.size} cours)"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // =========================================================
        // ENCADRÉ HORAIRES AVEC SÉLECTION DU JOUR DIRECTEMENT INTÉGRÉE
        // =========================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("timetables_days_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                var isDaySelectionExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }
                // 1. En-tête : Titre horaires
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { isDaySelectionExpanded = !isDaySelectionExpanded }.padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentDayName = if (selectedDayIndex in 1..daysOfWeek.size) daysOfWeek[selectedDayIndex - 1] else null
                    val currentDayIsFree = currentDayName != null && activeTab == 0 && teacherFreeDay == selectedDayIndex
                    val currentDaySlotsCount = if (currentDayName != null) allSlots.count { it.dayOfWeek == selectedDayIndex } else allSlots.size

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (currentDayIsFree) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (currentDayIsFree) Icons.Default.Weekend else Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (currentDayIsFree) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (currentDayName != null) {
                                if (Tr.isArabic()) "جدول توقيت يوم $currentDayName" else "Horaires du $currentDayName"
                            } else {
                                if (Tr.isArabic()) "جدول توقيت الأسبوع" else "Horaires de la semaine"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (currentDayIsFree) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (currentDayIsFree) {
                                if (Tr.isArabic()) "يوم راحة (لا توجد حصص)" else "Journée de repos (aucun cours)"
                            } else if (currentDayName != null) {
                                if (Tr.isArabic()) "$currentDaySlotsCount حصة مبرمجة" else "$currentDaySlotsCount cours programmé(s)"
                            } else {
                                if (Tr.isArabic()) "$currentDaySlotsCount حصة في الأسبوع" else "$currentDaySlotsCount cours sur la semaine"
                            },
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Icon(
                        imageVector = if (isDaySelectionExpanded) androidx.compose.material.icons.Icons.Default.ExpandLess else androidx.compose.material.icons.Icons.Default.ExpandMore,
                        contentDescription = "Plier/Déplier",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                androidx.compose.animation.AnimatedVisibility(visible = isDaySelectionExpanded) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // 2. Sélection des jours disposée sur DEUX LIGNES (Semaine, Dim, Lun.. sur 2 lignes)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Ligne 1 : Semaine, Dim, Lun
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Semaine
                        val isAll = selectedDayIndex == 0
                        Surface(
                            onClick = { onSelectDay(0) },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(9.dp),
                            color = if (isAll) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isAll) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewAgenda,
                                    contentDescription = null,
                                    tint = if (isAll) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (Tr.isArabic()) "الأسبوع" else "Semaine",
                                    fontSize = 12.sp,
                                    fontWeight = if (isAll) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isAll) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }

                        // Jours 1 (Dim) et 2 (Lun)
                        val line1Count = minOf(2, daysOfWeek.size)
                        for (dayIdx in 1..line1Count) {
                            val dName = daysOfWeek[dayIdx - 1]
                            val shortLabel = if (Tr.isArabic()) dName else dName.take(3).replaceFirstChar { it.uppercase() }
                            val isSelected = selectedDayIndex == dayIdx
                            val isFree = activeTab == 0 && teacherFreeDay == dayIdx
                            val count = allSlots.count { it.dayOfWeek == dayIdx }

                            Surface(
                                onClick = { onSelectDay(dayIdx) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(9.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else if (isFree) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else if (isFree) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = shortLabel,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else if (isFree) MaterialTheme.colorScheme.tertiary
                                                else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    if (isFree) {
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            text = "•",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.tertiary
                                        )
                                    } else if (count > 0) {
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            text = "($count)",
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Ligne 2 : Mar, Mer, Jeu...
                    if (daysOfWeek.size >= 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (dayIdx in 3..daysOfWeek.size) {
                                val dName = daysOfWeek[dayIdx - 1]
                                val shortLabel = if (Tr.isArabic()) dName else dName.take(3).replaceFirstChar { it.uppercase() }
                                val isSelected = selectedDayIndex == dayIdx
                                val isFree = activeTab == 0 && teacherFreeDay == dayIdx
                                val count = allSlots.count { it.dayOfWeek == dayIdx }

                                Surface(
                                    onClick = { onSelectDay(dayIdx) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp),
                                    shape = RoundedCornerShape(9.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else if (isFree) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else if (isFree) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = shortLabel,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                    else if (isFree) MaterialTheme.colorScheme.tertiary
                                                    else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        if (isFree) {
                                            Spacer(Modifier.width(3.dp))
                                            Text(
                                                text = "•",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.tertiary
                                            )
                                        } else if (count > 0) {
                                            Spacer(Modifier.width(3.dp))
                                            Text(
                                                text = "($count)",
                                                fontSize = 10.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 1C. JOURNÉE LIBRE & RÉCEPTION DES PARENTS (FUSIONNÉS EN UN SEUL ENCADRÉ) ---
                if (activeTab == 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Ligne 1 : Journée libre
                            var freeDayMenuExpanded by remember { mutableStateOf(false) }
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Weekend,
                                        contentDescription = null,
                                        tint = if (teacherFreeDay != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (Tr.isArabic()) "يوم راحة" else "Journée libre",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (teacherFreeDay != null && teacherFreeDay in 1..daysOfWeek.size) {
                                                if (Tr.isArabic()) daysOfWeek[teacherFreeDay - 1] else daysOfWeek[teacherFreeDay - 1].take(3)
                                            } else {
                                                if (Tr.isArabic()) "غير محدد" else "Non définie"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (teacherFreeDay != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                Box {
                                    OutlinedButton(
                                        onClick = { freeDayMenuExpanded = true },
                                        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(
                                            text = if (teacherFreeDay != null && teacherFreeDay in 1..daysOfWeek.size) (if (Tr.isArabic()) daysOfWeek[teacherFreeDay - 1] else daysOfWeek[teacherFreeDay - 1].take(3)) else (if (Tr.isArabic()) "اختيار" else "Choisir"),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.5.sp
                                        )
                                        Spacer(Modifier.width(2.dp))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(13.dp))
                                    }

                                    DropdownMenu(
                                        expanded = freeDayMenuExpanded,
                                        onDismissRequest = { freeDayMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(if (Tr.isArabic()) "لا يوجد يوم حر" else "Aucune journée libre") },
                                            onClick = {
                                                viewModel.updateTeacherFreeDay(null)
                                                freeDayMenuExpanded = false
                                            },
                                            leadingIcon = {
                                                if (teacherFreeDay == null) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        )
                                        HorizontalDivider()
                                        for (dayIdx in 1..daysOfWeek.size) {
                                            val isDaySelected = teacherFreeDay == dayIdx
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        daysOfWeek[dayIdx - 1],
                                                        fontWeight = if (isDaySelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isDaySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                leadingIcon = {
                                                    if (isDaySelected) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                    }
                                                },
                                                onClick = {
                                                    viewModel.updateTeacherFreeDay(dayIdx)
                                                    freeDayMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(0.8.dp)
                                    .height(34.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            )

                            // Ligne 2 : Réception des parents (sans mention de salle)
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Diversity3,
                                        contentDescription = null,
                                        tint = if (parentReceptionSlot != null) Color(0xFFD97706) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (Tr.isArabic()) "استقبال الأولياء" else "Réc. parents",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (parentReceptionSlot != null) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (parentReceptionSlot != null && parentReceptionSlot.dayOfWeek in 1..daysOfWeek.size) {
                                                val dayShort = if (Tr.isArabic()) daysOfWeek[parentReceptionSlot.dayOfWeek - 1] else daysOfWeek[parentReceptionSlot.dayOfWeek - 1].take(3)
                                                "$dayShort ${parentReceptionSlot.timeSlot.take(5)}"
                                            } else {
                                                if (Tr.isArabic()) "غير محدد" else "Non défini"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (parentReceptionSlot != null) Color(0xFFB45309) else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { onShowParentReceptionDialog() },
                                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(28.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (parentReceptionSlot != null) Color(0xFFD97706) else MaterialTheme.colorScheme.outline
                                    )
                                ) {
                                    Text(
                                        text = if (parentReceptionSlot != null) (if (Tr.isArabic()) "تعديل" else "Modifier") else (if (Tr.isArabic()) "اختيار" else "Choisir"),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.5.sp,
                                        color = if (parentReceptionSlot != null) Color(0xFFD97706) else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.8.dp)

                // 3. Contenu des créneaux
                val daysToDisplay = if (selectedDayIndex in 1..daysOfWeek.size) {
                    listOf(selectedDayIndex)
                } else {
                    (1..daysOfWeek.size).toList()
                }

                for ((dayOrder, dayIdx) in daysToDisplay.withIndex()) {
                    val dayName = daysOfWeek[dayIdx - 1]
                    val daySlots = allSlots.filter { it.dayOfWeek == dayIdx }.sortedWith(compareBy { slot ->
                        val firstWord = slot.timeSlot.split("-", " ").firstOrNull { it.contains(":") }
                        val hourPart = firstWord?.split(":")?.firstOrNull()?.toIntOrNull() ?: 99
                        val minutePart = firstWord?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 99
                        hourPart * 60 + minutePart
                    })
                    val isFreeDay = activeTab == 0 && teacherFreeDay == dayIdx

                    if (selectedDayIndex == 0) {
                        // En mode semaine complète, séparateur et sous-titre de chaque jour
                        if (dayOrder > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                thickness = 0.8.dp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isFreeDay) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isFreeDay) Icons.Default.Weekend else Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = if (isFreeDay) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = dayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = if (isFreeDay) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isFreeDay) (if (Tr.isArabic()) "راحة" else "Repos") else "(${daySlots.size})",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            if (!isFreeDay) {
                                TextButton(
                                    onClick = { onAddSlotForDay(dayIdx) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text(if (Tr.isArabic()) "إضافة" else "Ajouter", fontSize = 11.5.sp)
                                }
                            }
                        }
                    }

                    if (isFreeDay) {
                        GlassmorphicCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            tintColor = MaterialTheme.colorScheme.tertiary,
                            tintAlpha = 0.08f,
                            elevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Weekend,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (Tr.isArabic()) "يوم حر — راحة الأستاذ" else "Journée libre — Repos de l'enseignant",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        fontSize = 12.5.sp
                                    )
                                    Text(
                                        text = if (Tr.isArabic()) "لا توجد أي حصص مبرمجة في جدول التوقيت لهذا اليوم." else "Aucun cours programmé dans l'emploi du temps pour ce jour.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    } else if (daySlots.isEmpty()) {
                        if (selectedDayIndex != 0) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (Tr.isArabic()) "لا توجد أي حصة مبرمجة يوم $dayName." else "Aucun cours programmé le $dayName.",
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                TextButton(
                                    onClick = { onAddSlotForDay(dayIdx) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (Tr.isArabic()) "إضافة حصة ليوم $dayName" else "Ajouter un cours pour $dayName", fontSize = 12.sp)
                                }
                            }
                        } else {
                            Text(
                                text = if (Tr.isArabic()) "لا توجد أي حصة مبرمجة في هذا اليوم." else "Aucun cours programmé ce jour-là.",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(start = 28.dp, bottom = 4.dp)
                            )
                        }
                    } else {
                        // Liste des cours de la journée
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (slot in daySlots) {
                                TimetableDailySlotCard(
                                    slot = slot,
                                    classes = classes,
                                    activeTab = activeTab,
                                    teacherName = teacherName,
                                    onEditSlot = onEditSlot,
                                    onDeleteSlot = onDeleteSlot
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Helper to get the start hour of a time slot (e.g. "08:00 - 09:00" -> 8)
 */
private fun getStartHour(slot: String): Int {
    val firstWord = slot.split("-", " ").firstOrNull { it.contains(":") }
    return firstWord?.split(":")?.firstOrNull()?.toIntOrNull() ?: 0
}

/**
 * Transposed Week Timetable with STICKY DAYS COLUMN,
 * Lunch break column after 12H, and merged free day row:
 * - Jours fixes à gauche (Rows: Dimanche à Jeudi) qui restent toujours visibles lors du défilement des heures
 * - Horaires matin (< 12H) puis Pause Déjeuner puis Horaires après-midi (>= 12H)
 * - Colonne Pause Déjeuner unifiée avec libellé personnalisable au clic
 * - Ligne Journée Libre fusionnée sur toute la largeur
 * - Possibilité de modifier et de supprimer chaque créneau d'heure
 */
@Composable
private fun TimetableSlotGridCell(
    slot: TimetableSlotEntity?,
    dayIdx: Int,
    timeSlot: String,
    slotColWidth: androidx.compose.ui.unit.Dp,
    classes: List<ClassEntity>,
    activeTab: Int,
    teacherName: String,
    onEditSlot: ((TimetableSlotEntity) -> Unit)?,
    onDeleteSlot: (TimetableSlotEntity) -> Unit,
    onAddSlotAt: ((dayIdx: Int, timeSlot: String) -> Unit)?
) {
    Box(
        modifier = Modifier
            .width(slotColWidth)
            .fillMaxHeight()
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        if (slot != null) {
            val cleanSubj = slot.subject.trim()
            val isParentReception = cleanSubj.contains("réception", ignoreCase = true) || cleanSubj.contains("reception", ignoreCase = true)
            val matchedClass = if (slot.classId != null) classes.find { it.id == slot.classId } else {
                classes.find {
                    cleanSubj.equals(it.name, ignoreCase = true) ||
                    cleanSubj.startsWith(it.name, ignoreCase = true) ||
                    it.name.startsWith(cleanSubj, ignoreCase = true)
                }
            }
            val cName = matchedClass?.name ?: ""
            val effectiveClassName = if (isParentReception) "Réception Parents" else (if (cName.isNotBlank()) cName else slot.subject.ifBlank { if (Tr.isArabic()) "القسم" else "Classe" })

            val palette = if (isParentReception) {
                com.example.ui.ClassColorPalette(
                    primary = Color(0xFFD97706),
                    container = Color(0xFFFEF3C7),
                    onContainer = Color(0xFF92400E),
                    label = "Réception"
                )
            } else if (activeTab == 1) {
                com.example.ui.SubjectColorHelper.getPaletteForSubject(slot.subject)
            } else if (cleanSubj.contains("2SE", ignoreCase = true) || (matchedClass?.name?.contains("2SE", ignoreCase = true) == true) || slot.classroom.contains("2SE", ignoreCase = true)) {
                ClassColorHelper.Palette2SE
            } else if (matchedClass != null) {
                ClassColorHelper.getPaletteForClass(matchedClass.id, matchedClass.name)
            } else {
                ClassColorHelper.getPaletteForClass(slot.classId ?: 0, cleanSubj.ifBlank { if (Tr.isArabic()) "القسم" else "Classe" })
            }

            val isDarkCell = MaterialTheme.colorScheme.background != com.example.ui.theme.OffWhite
            val primaryColor = palette.primary
            val containerColor = palette.container
            val onContainerColor = palette.onContainer
            val cellBg = if (isDarkCell) MaterialTheme.colorScheme.surface else Color.White
            val cellBorder = if (isDarkCell) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else Color(0xFFE2E8F0)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(elevation = 1.5.dp, shape = RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isDarkCell) Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                        else Brush.verticalGradient(listOf(Color.White, Color(0xFFF8FAFC)))
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = if (isDarkCell) 0.35f else 0.85f),
                                primaryColor.copy(alpha = 0.40f),
                                Color.White.copy(alpha = if (isDarkCell) 0.15f else 0.35f)
                            )
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable {
                        if (onEditSlot != null) {
                            onEditSlot(slot)
                        } else {
                            onDeleteSlot(slot)
                        }
                    }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (activeTab == 1) {
                        // --- MODE EMPLOI DU TEMPS DES ÉLÈVES / CLASSE ---
                        val displaySubject = slot.subject.ifBlank { if (Tr.isArabic()) "المادة" else "Matière" }
                        val splitSubject = com.example.ui.SubjectColorHelper.splitSubjectIntoTwoLines(displaySubject)
                        val formattedSubject = if (splitSubject != null) "${splitSubject.first}\n${splitSubject.second}" else displaySubject

                        // 1. Matière mise en valeur
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDarkCell) primaryColor.copy(alpha = 0.75f).compositeOver(Color(0xFF0F172A)) else primaryColor,
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, if (isDarkCell) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.35f)),
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Text(
                                text = formattedSubject,
                                fontWeight = FontWeight.Black,
                                fontSize = if (splitSubject != null) 11.sp else 12.5.sp,
                                lineHeight = if (splitSubject != null) 12.sp else 14.5.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(Modifier.height(2.dp))

                        // 2. Nom de l'enseignant (sans préfixe "Salle" ou "قاعة", avec icône Person)
                        val rawTeacher = slot.classroom.trim()
                        val cleanTeacher = rawTeacher.replace(Regex("(?i)^salle\\s*"), "").replace(Regex("^(?i)قاعة\\s*"), "").trim()
                        val teacherDisplay = cleanTeacher.ifBlank { if (Tr.isArabic()) "الأستاذ" else "Enseignant" }
                        val teacherFontSize = when {
                            teacherDisplay.length > 14 -> 7.5.sp
                            teacherDisplay.length > 10 -> 8.5.sp
                            else -> 9.5.sp
                        }
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = if (isDarkCell) Color(0xFF0F766E).copy(alpha = 0.30f) else Color(0xFFCCFBF1).copy(alpha = 0.95f),
                            border = androidx.compose.foundation.BorderStroke(0.9.dp, if (isDarkCell) Color(0xFF2DD4BF).copy(alpha = 0.65f) else Color(0xFF0D9488).copy(alpha = 0.60f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (isDarkCell) Color(0xFF2DD4BF) else Color(0xFF0D9488),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(Modifier.width(2.5.dp))
                                Text(
                                    text = teacherDisplay,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = teacherFontSize,
                                    color = if (isDarkCell) Color(0xFF99F6E4) else Color(0xFF0F766E),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // --- MODE EMPLOI DU TEMPS DE L'ENSEIGNANT ---
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = containerColor.copy(alpha = 0.9f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.6f)),
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                if (isParentReception) {
                                    Icon(
                                        imageVector = Icons.Default.Diversity3,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(Modifier.width(3.dp))
                                }
                                Text(
                                    text = effectiveClassName,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = primaryColor,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (!isParentReception && slot.subject.isNotBlank() && !slot.subject.equals(effectiveClassName, ignoreCase = true)) {
                            val subjPalette = com.example.ui.SubjectColorHelper.getPaletteForSubject(slot.subject)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isDarkCell) subjPalette.primary.copy(alpha = 0.25f) else subjPalette.container.copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, subjPalette.primary.copy(alpha = 0.6f)),
                                modifier = Modifier.padding(bottom = 2.dp)
                            ) {
                                Text(
                                    text = slot.subject,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (isDarkCell) Color.White else subjPalette.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }

                        if (slot.classroom.isNotBlank() && !isParentReception) {
                            Spacer(Modifier.height(3.dp))
                            val roomClean = slot.classroom.replace(Regex("(?i)^salle\\s*"), "").trim()
                            val displayRoom = if (roomClean.isNotBlank()) {
                                if (roomClean.matches(Regex("(?i)^(labo|amphi|bureau|atelier).*"))) roomClean 
                                else if (Tr.isArabic()) "قاعة $roomClean" else "Salle $roomClean"
                            } else slot.classroom

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isDarkCell) Color(0xFF134E4A).copy(alpha = 0.5f) else Color(0xFFCCFBF1).copy(alpha = 0.95f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkCell) Color(0xFF2DD4BF) else Color(0xFF14B8A6).copy(alpha = 0.85f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MeetingRoom,
                                        contentDescription = null,
                                        tint = if (isDarkCell) Color(0xFF5EEAD4) else Color(0xFF0D9488),
                                        modifier = Modifier.size(9.dp)
                                    )
                                    Spacer(Modifier.width(2.dp))
                                    Text(
                                        text = displayRoom,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkCell) Color(0xFFCCFBF1) else Color(0xFF0F766E),
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        onAddSlotAt?.invoke(dayIdx, timeSlot)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Ajouter créneau",
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun TimetableWeekTable(
    daysOfWeek: List<String>,
    uniqueTimeSlots: List<String>,
    allSlots: List<TimetableSlotEntity>,
    classes: List<ClassEntity>,
    activeTab: Int,
    teacherFreeDay: Int?,
    teacherName: String = "",
    lunchBreakLabel: String = "Pause déjeuner",
    zoomScale: Float = 1.0f,
    onEditLunchBreak: () -> Unit = {},
    onDeleteSlot: (TimetableSlotEntity) -> Unit,
    onEditSlot: ((TimetableSlotEntity) -> Unit)? = null,
    onEditTimeSlot: ((timeSlot: String) -> Unit)? = null,
    onDeleteTimeSlot: ((timeSlot: String) -> Unit)? = null,
    onAddSlotAt: ((dayIdx: Int, timeSlot: String) -> Unit)? = null,
    fitOverview: Boolean = false,
    modifier: Modifier = Modifier
) {
    val dayColWidth = ((if (fitOverview) 78 else 92) * zoomScale).dp
    val slotColWidth = (132 * zoomScale).dp
    val lunchColWidth = (84 * zoomScale).dp
    val rowHeight = ((if (fitOverview) 86 else 94) * zoomScale).dp
    val headerHeight = ((if (fitOverview) 36 else 42) * zoomScale).dp

    val morningSlots = remember(uniqueTimeSlots) {
        uniqueTimeSlots.filter { getStartHour(it) < 12 }
    }
    val afternoonSlots = remember(uniqueTimeSlots) {
        uniqueTimeSlots.filter { getStartHour(it) >= 12 }
    }

    // Shared vertical scroll state to ensure Days and Time cells scroll together vertically
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    GlassmorphicCard(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = 2.dp
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // ==========================================
            // 1. PINNED / STICKY DAYS COLUMN (Fixed horizontally, scrolls vertically with grid)
            // ==========================================
            Column(
                modifier = Modifier
                    .width(dayColWidth)
                    .fillMaxHeight()
            ) {
                // Top-left corner cell (vide / sans mention Jour)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(0.5.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                )

                // Days rows (vertically scrollable in sync with the grid)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(verticalScrollState)
                ) {
                    for (dayIdx in 1..daysOfWeek.size) {
                        val isTeacherFreeDay = (activeTab == 0 && teacherFreeDay == dayIdx)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(rowHeight)
                                .background(
                                    if (isTeacherFreeDay) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                )
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = daysOfWeek[dayIdx - 1],
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isTeacherFreeDay) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                if (isTeacherFreeDay) {
                                    Spacer(Modifier.height(3.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = if (Tr.isArabic()) "☕ راحة" else "☕ Libre",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Divider between fixed days and scrollable time content
            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // ==========================================
            // 2. SCROLLABLE TIME SLOTS & GRID (Horizontally scrollable)
            // ==========================================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(horizontalScrollState) // Horizontal scroll ONLY on time columns!
            ) {
                // Header row of time slots (Matin + Pause Déjeuner + Après-midi)
                val isDarkTable = MaterialTheme.colorScheme.background != com.example.ui.theme.OffWhite
                val tableHourColor = if (isDarkTable) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                Row(
                    modifier = Modifier
                        .height(headerHeight)
                        .background(if (isDarkTable) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primaryContainer),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Morning Slots Headers
                    for (timeSlot in morningSlots) {
                        val (startH, endH) = com.example.util.getSlotHoursWithMinutesTwoLines(timeSlot)

                        Box(
                            modifier = Modifier
                                .width(slotColWidth)
                                .fillMaxHeight()
                                .border(0.5.dp, if (isDarkTable) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                                .clickable {
                                    onEditTimeSlot?.invoke(timeSlot)
                                }
                                .padding(horizontal = 2.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = startH,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.5.sp,
                                    lineHeight = 12.5.sp,
                                    textAlign = TextAlign.Center,
                                    color = tableHourColor
                                )
                                Text(
                                    text = endH,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.5.sp,
                                    lineHeight = 12.5.sp,
                                    textAlign = TextAlign.Center,
                                    color = tableHourColor
                                )
                            }
                        }
                    }

                    // Lunch Break Header Column
                    Box(
                        modifier = Modifier
                            .width(lunchColWidth)
                            .fillMaxHeight()
                            .background(Color(0xFFFFF3E0))
                            .border(0.5.dp, Color(0xFFFFB74D))
                            .clickable { onEditLunchBreak() }
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = if (Tr.isArabic()) "12:00\nاستراحة" else "12:00\nPause",
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 11.sp,
                                color = Color(0xFFE65100)
                            )
                        }
                    }

                    // Afternoon Slots Headers
                    for (timeSlot in afternoonSlots) {
                        val (startH, endH) = com.example.util.getSlotHoursWithMinutesTwoLines(timeSlot)

                        Box(
                            modifier = Modifier
                                .width(slotColWidth)
                                .fillMaxHeight()
                                .border(0.5.dp, if (isDarkTable) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                                .clickable {
                                    onEditTimeSlot?.invoke(timeSlot)
                                }
                                .padding(horizontal = 2.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = startH,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.5.sp,
                                    lineHeight = 12.5.sp,
                                    textAlign = TextAlign.Center,
                                    color = tableHourColor
                                )
                                Text(
                                    text = endH,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.5.sp,
                                    lineHeight = 12.5.sp,
                                    textAlign = TextAlign.Center,
                                    color = tableHourColor
                                )
                            }
                        }
                    }
                }

                // Grid content rows (synced vertical scroll with days column)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(verticalScrollState)
                ) {
                    val totalSlotsWidth = (slotColWidth * (morningSlots.size + afternoonSlots.size)) + lunchColWidth

                    for (dayIdx in 1..daysOfWeek.size) {
                        val isTeacherFreeDay = (activeTab == 0 && teacherFreeDay == dayIdx)

                        if (isTeacherFreeDay) {
                            // --- FUSION DE TOUTES LES CASES POUR LA JOURNÉE LIBRE ---
                            Row(
                                modifier = Modifier
                                    .width(totalSlotsWidth)
                                    .height(rowHeight)
                                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.22f))
                                    .border(0.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Weekend,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = if (Tr.isArabic()) "☕ يوم راحة" else "☕ JOURNÉE LIBRE",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .height(rowHeight)
                                    .background(Color.Transparent),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Helper to match slot by exact string or by time overlap
                                fun findSlotForTimeSlot(slots: List<TimetableSlotEntity>, day: Int, targetTimeSlot: String): TimetableSlotEntity? {
                                    val direct = slots.find { it.dayOfWeek == day && it.timeSlot.trim().equals(targetTimeSlot.trim(), ignoreCase = true) }
                                    if (direct != null) return direct

                                    val targetInterval = com.example.util.TimetableTimeSlotHelper.parseSlotInterval(targetTimeSlot) ?: return null
                                    return slots.find { s ->
                                        if (s.dayOfWeek != day) return@find false
                                        val sInterval = com.example.util.TimetableTimeSlotHelper.parseSlotInterval(s.timeSlot) ?: return@find false
                                        sInterval.startMin == targetInterval.startMin || (targetInterval.startMin >= sInterval.startMin && targetInterval.endMin <= sInterval.endMin)
                                    }
                                }

                                // Morning Slot Cells
                                var mIdx = 0
                                while (mIdx < morningSlots.size) {
                                    val timeSlot = morningSlots[mIdx]
                                    val slot = findSlotForTimeSlot(allSlots, dayIdx, timeSlot)
                                    val nextTimeSlot = morningSlots.getOrNull(mIdx + 1)
                                    val nextSlot = if (nextTimeSlot != null) findSlotForTimeSlot(allSlots, dayIdx, nextTimeSlot) else null

                                    val is2HourSpan = slot != null && (
                                        (nextSlot != null && slot.id == nextSlot.id) ||
                                        (nextSlot != null && slot.subject.isNotBlank() &&
                                         slot.subject.trim().equals(nextSlot.subject.trim(), ignoreCase = true) &&
                                         slot.classroom.trim().equals(nextSlot.classroom.trim(), ignoreCase = true) &&
                                         slot.classId == nextSlot.classId)
                                    )

                                    val cellWidth = if (is2HourSpan) slotColWidth * 2 else slotColWidth

                                    Box(
                                        modifier = Modifier
                                            .width(cellWidth)
                                            .fillMaxHeight()
                                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                            .padding(3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (slot != null) {
                                            val cleanSubj = slot.subject.trim()
                                            val isParentReception = cleanSubj.contains("réception", ignoreCase = true) || cleanSubj.contains("reception", ignoreCase = true)
                                            val matchedClass = if (slot.classId != null) classes.find { it.id == slot.classId } else {
                                                classes.find {
                                                    cleanSubj.equals(it.name, ignoreCase = true) ||
                                                    cleanSubj.startsWith(it.name, ignoreCase = true) ||
                                                    it.name.startsWith(cleanSubj, ignoreCase = true)
                                                }
                                            }
                                            val cName = matchedClass?.name ?: ""
                                            val effectiveClassName = if (isParentReception) "Réception Parents" else (if (cName.isNotBlank()) cName else slot.subject.ifBlank { if (Tr.isArabic()) "القسم" else "Classe" })

                                            val palette = if (isParentReception) {
                                                com.example.ui.ClassColorPalette(
                                                    primary = Color(0xFFD97706),
                                                    container = Color(0xFFFEF3C7),
                                                    onContainer = Color(0xFF92400E),
                                                    label = "Réception"
                                                )
                                            } else if (activeTab == 1) {
                                                com.example.ui.SubjectColorHelper.getPaletteForSubject(slot.subject)
                                            } else if (cleanSubj.contains("2SE", ignoreCase = true) || (matchedClass?.name?.contains("2SE", ignoreCase = true) == true) || slot.classroom.contains("2SE", ignoreCase = true)) {
                                                ClassColorHelper.Palette2SE
                                            } else if (matchedClass != null) {
                                                ClassColorHelper.getPaletteForClass(matchedClass.id, matchedClass.name)
                                            } else {
                                                ClassColorHelper.getPaletteForClass(slot.classId ?: 0, cleanSubj.ifBlank { if (Tr.isArabic()) "القسم" else "Classe" })
                                            }

                                            val primaryColor = palette.primary
                                            val containerColor = palette.container
                                            val cellBg = if (isDarkTable) MaterialTheme.colorScheme.surface else Color.White
                                            val cellBorder = if (isDarkTable) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else Color(0xFFE2E8F0)

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .shadow(elevation = 1.5.dp, shape = RoundedCornerShape(10.dp))
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        if (isDarkTable) Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                                                        else Brush.verticalGradient(listOf(Color.White, Color(0xFFF8FAFC)))
                                                    )
                                                    .border(
                                                        width = 1.2.dp,
                                                        brush = Brush.linearGradient(
                                                            listOf(
                                                                Color.White.copy(alpha = if (isDarkTable) 0.35f else 0.85f),
                                                                primaryColor.copy(alpha = 0.40f),
                                                                Color.White.copy(alpha = if (isDarkTable) 0.15f else 0.35f)
                                                            )
                                                        ),
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                .clickable {
                                                    if (onEditSlot != null) {
                                                        onEditSlot(slot)
                                                    } else {
                                                        onDeleteSlot(slot)
                                                    }
                                                }
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    if (activeTab == 1) {
                                                        // --- MODE EMPLOI DU TEMPS DES ÉLÈVES / CLASSE ---
                                                        val isAr = Tr.isArabic()
                                                        // 1. Matière mise en valeur
                                                        val displaySubject = slot.subject.ifBlank { if (isAr) "المادة" else "Matière" }
                                                        Surface(
                                                            shape = RoundedCornerShape(5.dp),
                                                            color = if (isDarkTable) primaryColor.copy(alpha = 0.75f).compositeOver(Color(0xFF0F172A)) else primaryColor,
                                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTable) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.35f)),
                                                            modifier = Modifier.padding(bottom = 1.dp)
                                                        ) {
                                                            Text(
                                                                text = displaySubject,
                                                                fontWeight = FontWeight.Black,
                                                                fontSize = 13.5.sp,
                                                                lineHeight = 16.sp,
                                                                color = Color.White,
                                                                textAlign = TextAlign.Center,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                                            )
                                                        }

                                                        Spacer(Modifier.height(2.dp))

                                                        // 2. Nom de l'enseignant (sans préfixe "Salle" ou "قاعة", avec icône Person)
                                                        val rawTeacher = slot.classroom.trim()
                                                        val cleanTeacher = rawTeacher.replace(Regex("(?i)^salle\\s*"), "").replace(Regex("^(?i)قاعة\\s*"), "").trim()
                                                        val teacherDisplay = cleanTeacher.ifBlank { if (isAr) "الأستاذ" else "Enseignant" }
                                                        val teacherFontSize = when {
                                                            teacherDisplay.length > 14 -> 7.sp
                                                            teacherDisplay.length > 10 -> 8.sp
                                                            else -> 9.sp
                                                        }
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = if (isDarkTable) Color(0xFF0F766E).copy(alpha = 0.35f) else Color(0xFFCCFBF1).copy(alpha = 0.95f),
                                                            border = androidx.compose.foundation.BorderStroke(0.8.dp, if (isDarkTable) Color(0xFF2DD4BF).copy(alpha = 0.65f) else Color(0xFF0D9488).copy(alpha = 0.60f))
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.Center,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Person,
                                                                    contentDescription = null,
                                                                    tint = if (isDarkTable) Color(0xFF2DD4BF) else Color(0xFF0D9488),
                                                                    modifier = Modifier.size(9.dp)
                                                                )
                                                                Spacer(Modifier.width(2.dp))
                                                                Text(
                                                                    text = teacherDisplay,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    fontSize = teacherFontSize,
                                                                    color = if (isDarkTable) Color(0xFF99F6E4) else Color(0xFF0F766E),
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    textAlign = TextAlign.Center
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        // --- MODE EMPLOI DU TEMPS DE L'ENSEIGNANT ---
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = containerColor.copy(alpha = 0.9f),
                                                            border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.6f)),
                                                            modifier = Modifier.padding(bottom = 2.dp)
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                if (isParentReception) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Diversity3,
                                                                        contentDescription = null,
                                                                        tint = primaryColor,
                                                                        modifier = Modifier.size(11.dp)
                                                                    )
                                                                    Spacer(Modifier.width(3.dp))
                                                                }
                                                                Text(
                                                                    text = effectiveClassName,
                                                                    fontWeight = FontWeight.Black,
                                                                    fontSize = 15.sp,
                                                                    color = primaryColor,
                                                                    textAlign = TextAlign.Center,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }

                                                        val effectiveRoom = if (slot.classroom.isNotBlank()) {
                                                            slot.classroom
                                                        } else {
                                                            allSlots.firstOrNull { s ->
                                                                s.classroom.isNotBlank() && (
                                                                    (slot.classId != null && s.classId == slot.classId) ||
                                                                    s.subject.trim().equals(slot.subject.trim(), ignoreCase = true)
                                                                )
                                                            }?.classroom ?: ""
                                                        }

                                                        if (effectiveRoom.isNotBlank() && !isParentReception) {
                                                            val roomClean = effectiveRoom.replace(Regex("(?i)^salle\\s*"), "").trim()
                                                            val displayRoom = if (roomClean.isNotBlank()) {
                                                                if (roomClean.matches(Regex("(?i)^(labo|amphi|bureau|atelier).*"))) roomClean
                                                                else if (Tr.isArabic()) "قاعة $roomClean" else "Salle $roomClean"
                                                            } else effectiveRoom
                                                            Spacer(Modifier.height(2.dp))
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = if (isDarkTable) Color(0xFF134E4A).copy(alpha = 0.65f) else Color(0xFFCCFBF1).copy(alpha = 0.95f),
                                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTable) Color(0xFF2DD4BF) else Color(0xFF14B8A6).copy(alpha = 0.85f))
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.Center,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.MeetingRoom,
                                                                        contentDescription = null,
                                                                        tint = if (isDarkTable) Color(0xFF5EEAD4) else Color(0xFF0D9488),
                                                                        modifier = Modifier.size(9.dp)
                                                                    )
                                                                    Spacer(Modifier.width(2.5.dp))
                                                                    Text(
                                                                        text = displayRoom,
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = if (isDarkTable) Color(0xFFCCFBF1) else Color(0xFF0F766E),
                                                                        maxLines = 1,
                                                                        textAlign = TextAlign.Center
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .clickable {
                                                        onAddSlotAt?.invoke(dayIdx, timeSlot)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Ajouter créneau",
                                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (is2HourSpan) {
                                        mIdx += 2
                                    } else {
                                        mIdx += 1
                                    }
                                }

                                // Lunch Break Column Cell
                                Box(
                                    modifier = Modifier
                                        .width(lunchColWidth)
                                        .fillMaxHeight()
                                        .background(Color(0xFFFFF8E1))
                                        .border(0.5.dp, Color(0xFFFFE082))
                                        .clickable { onEditLunchBreak() }
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Restaurant,
                                            contentDescription = null,
                                            tint = Color(0xFFE65100),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        val effectiveLunchLabel = if (lunchBreakLabel.trim().equals("Pause déjeuner", ignoreCase = true) && Tr.isArabic()) {
                                            "استراحة الغداء"
                                        } else {
                                            Tr.translate(lunchBreakLabel, Tr.isArabic())
                                        }
                                        Text(
                                            text = effectiveLunchLabel,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE65100),
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            lineHeight = 11.sp
                                        )
                                    }
                                }

                                // Afternoon Slot Cells
                                var aIdx = 0
                                while (aIdx < afternoonSlots.size) {
                                    val timeSlot = afternoonSlots[aIdx]
                                    val slot = findSlotForTimeSlot(allSlots, dayIdx, timeSlot)
                                    val nextTimeSlot = afternoonSlots.getOrNull(aIdx + 1)
                                    val nextSlot = if (nextTimeSlot != null) findSlotForTimeSlot(allSlots, dayIdx, nextTimeSlot) else null

                                    val is2HourSpan = slot != null && (
                                        (nextSlot != null && slot.id == nextSlot.id) ||
                                        (nextSlot != null && slot.subject.isNotBlank() &&
                                         slot.subject.trim().equals(nextSlot.subject.trim(), ignoreCase = true) &&
                                         slot.classroom.trim().equals(nextSlot.classroom.trim(), ignoreCase = true) &&
                                         slot.classId == nextSlot.classId)
                                    )

                                    val cellWidth = if (is2HourSpan) slotColWidth * 2 else slotColWidth

                                    Box(
                                        modifier = Modifier
                                            .width(cellWidth)
                                            .fillMaxHeight()
                                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                            .padding(3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (slot != null) {
                                            val cleanSubj = slot.subject.trim()
                                            val isParentReception = cleanSubj.contains("réception", ignoreCase = true) || cleanSubj.contains("reception", ignoreCase = true)
                                            val matchedClass = if (slot.classId != null) classes.find { it.id == slot.classId } else {
                                                classes.find {
                                                    cleanSubj.equals(it.name, ignoreCase = true) ||
                                                    cleanSubj.startsWith(it.name, ignoreCase = true) ||
                                                    it.name.startsWith(cleanSubj, ignoreCase = true)
                                                }
                                            }
                                            val cName = matchedClass?.name ?: ""
                                            val effectiveClassName = if (isParentReception) "Réception Parents" else (if (cName.isNotBlank()) cName else slot.subject.ifBlank { if (Tr.isArabic()) "القسم" else "Classe" })

                                            val palette = if (isParentReception) {
                                                com.example.ui.ClassColorPalette(
                                                    primary = Color(0xFFD97706),
                                                    container = Color(0xFFFEF3C7),
                                                    onContainer = Color(0xFF92400E),
                                                    label = "Réception"
                                                )
                                            } else if (activeTab == 1) {
                                                com.example.ui.SubjectColorHelper.getPaletteForSubject(slot.subject)
                                            } else if (cleanSubj.contains("2SE", ignoreCase = true) || (matchedClass?.name?.contains("2SE", ignoreCase = true) == true) || slot.classroom.contains("2SE", ignoreCase = true)) {
                                                ClassColorHelper.Palette2SE
                                            } else if (matchedClass != null) {
                                                ClassColorHelper.getPaletteForClass(matchedClass.id, matchedClass.name)
                                            } else {
                                                ClassColorHelper.getPaletteForClass(slot.classId ?: 0, cleanSubj.ifBlank { if (Tr.isArabic()) "القسم" else "Classe" })
                                            }

                                            val primaryColor = palette.primary
                                            val containerColor = palette.container
                                            val cellBg = if (isDarkTable) MaterialTheme.colorScheme.surface else Color.White
                                            val cellBorder = if (isDarkTable) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else Color(0xFFE2E8F0)

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(cellBg)
                                                    .border(1.2.dp, cellBorder, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    if (onEditSlot != null) {
                                                        onEditSlot(slot)
                                                    } else {
                                                        onDeleteSlot(slot)
                                                    }
                                                }
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    if (activeTab == 1) {
                                                        // --- MODE EMPLOI DU TEMPS DES ÉLÈVES / CLASSE ---
                                                        val isAr = Tr.isArabic()
                                                        // 1. Matière mise en valeur
                                                        val displaySubject = slot.subject.ifBlank { if (isAr) "المادة" else "Matière" }
                                                        Surface(
                                                            shape = RoundedCornerShape(5.dp),
                                                            color = if (isDarkTable) primaryColor.copy(alpha = 0.75f).compositeOver(Color(0xFF0F172A)) else primaryColor,
                                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTable) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.35f)),
                                                            modifier = Modifier.padding(bottom = 1.dp)
                                                        ) {
                                                            Text(
                                                                text = displaySubject,
                                                                fontWeight = FontWeight.Black,
                                                                fontSize = 13.5.sp,
                                                                lineHeight = 16.sp,
                                                                color = Color.White,
                                                                textAlign = TextAlign.Center,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                                            )
                                                        }

                                                        Spacer(Modifier.height(2.dp))

                                                        // 2. Nom de l'enseignant (sans préfixe "Salle" ou "قاعة", avec icône Person)
                                                        val rawTeacher = slot.classroom.trim()
                                                        val cleanTeacher = rawTeacher.replace(Regex("(?i)^salle\\s*"), "").replace(Regex("^(?i)قاعة\\s*"), "").trim()
                                                        val teacherDisplay = cleanTeacher.ifBlank { if (isAr) "الأستاذ" else "Enseignant" }
                                                        val teacherFontSize = when {
                                                            teacherDisplay.length > 14 -> 7.sp
                                                            teacherDisplay.length > 10 -> 8.sp
                                                            else -> 9.sp
                                                        }
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = if (isDarkTable) Color(0xFF0F766E).copy(alpha = 0.35f) else Color(0xFFCCFBF1).copy(alpha = 0.95f),
                                                            border = androidx.compose.foundation.BorderStroke(0.8.dp, if (isDarkTable) Color(0xFF2DD4BF).copy(alpha = 0.65f) else Color(0xFF0D9488).copy(alpha = 0.60f))
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.Center,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Person,
                                                                    contentDescription = null,
                                                                    tint = if (isDarkTable) Color(0xFF2DD4BF) else Color(0xFF0D9488),
                                                                    modifier = Modifier.size(9.dp)
                                                                )
                                                                Spacer(Modifier.width(2.dp))
                                                                Text(
                                                                    text = teacherDisplay,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    fontSize = teacherFontSize,
                                                                    color = if (isDarkTable) Color(0xFF99F6E4) else Color(0xFF0F766E),
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    textAlign = TextAlign.Center
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        // --- MODE EMPLOI DU TEMPS DE L'ENSEIGNANT ---
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = containerColor.copy(alpha = 0.9f),
                                                            border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.6f)),
                                                            modifier = Modifier.padding(bottom = 2.dp)
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                if (isParentReception) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Diversity3,
                                                                        contentDescription = null,
                                                                        tint = primaryColor,
                                                                        modifier = Modifier.size(11.dp)
                                                                    )
                                                                    Spacer(Modifier.width(3.dp))
                                                                }
                                                                Text(
                                                                    text = effectiveClassName,
                                                                    fontWeight = FontWeight.Black,
                                                                    fontSize = 15.sp,
                                                                    color = primaryColor,
                                                                    textAlign = TextAlign.Center,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }

                                                        val effectiveRoom = if (slot.classroom.isNotBlank()) {
                                                            slot.classroom
                                                        } else {
                                                            allSlots.firstOrNull { s ->
                                                                s.classroom.isNotBlank() && (
                                                                    (slot.classId != null && s.classId == slot.classId) ||
                                                                    s.subject.trim().equals(slot.subject.trim(), ignoreCase = true)
                                                                )
                                                            }?.classroom ?: ""
                                                        }

                                                        if (effectiveRoom.isNotBlank() && !isParentReception) {
                                                            val roomClean = effectiveRoom.replace(Regex("(?i)^salle\\s*"), "").trim()
                                                            val displayRoom = if (roomClean.isNotBlank()) {
                                                                if (roomClean.matches(Regex("(?i)^(labo|amphi|bureau|atelier).*"))) roomClean
                                                                else if (Tr.isArabic()) "قاعة $roomClean" else "Salle $roomClean"
                                                            } else effectiveRoom
                                                            Spacer(Modifier.height(2.dp))
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = if (isDarkTable) Color(0xFF134E4A).copy(alpha = 0.65f) else Color(0xFFCCFBF1).copy(alpha = 0.95f),
                                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTable) Color(0xFF2DD4BF) else Color(0xFF14B8A6).copy(alpha = 0.85f))
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.Center,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.MeetingRoom,
                                                                        contentDescription = null,
                                                                        tint = if (isDarkTable) Color(0xFF5EEAD4) else Color(0xFF0D9488),
                                                                        modifier = Modifier.size(9.dp)
                                                                    )
                                                                    Spacer(Modifier.width(2.5.dp))
                                                                    Text(
                                                                        text = displayRoom,
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = if (isDarkTable) Color(0xFFCCFBF1) else Color(0xFF0F766E),
                                                                        maxLines = 1,
                                                                        textAlign = TextAlign.Center
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .clickable {
                                                        onAddSlotAt?.invoke(dayIdx, timeSlot)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Ajouter créneau",
                                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (is2HourSpan) {
                                        aIdx += 2
                                    } else {
                                        aIdx += 1
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
