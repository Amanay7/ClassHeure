package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.JournalEntryEntity
import com.example.ui.AppViewModel
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.FineSearchBar
import com.example.ui.theme.OffWhite
import com.example.ui.ClassColorHelper
import com.example.ui.ClassColorDot
import com.example.ui.ClassBadge
import com.example.util.Tr
import com.example.util.tr
import com.example.util.AdaptiveScreenContainer
import com.example.util.adaptiveDialogMaxHeight

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    initialDateStr: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isAr = Tr.isArabic()
    val initialMillis = remember(initialDateStr) {
        val ts = parseDateToTimestamp(initialDateStr)
        if (ts > 0L) ts else System.currentTimeMillis()
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = millis
                        }
                        val formatted = String.format(
                            java.util.Locale.US,
                            "%02d/%02d/%04d",
                            cal.get(java.util.Calendar.DAY_OF_MONTH),
                            cal.get(java.util.Calendar.MONTH) + 1,
                            cal.get(java.util.Calendar.YEAR)
                        )
                        onDateSelected(formatted)
                    }
                    onDismiss()
                }
            ) {
                Text(if (isAr) "تأكيد" else "Confirmer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isAr) "إلغاء" else "Annuler")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

private fun parseDateToTimestamp(dateStr: String): Long {
    val formats = listOf("dd/MM/yyyy", "yyyy-MM-dd", "d/M/yyyy", "dd-MM-yyyy")
    for (fmt in formats) {
        try {
            val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.getDefault())
            sdf.isLenient = false
            val parsed = sdf.parse(dateStr.trim())
            if (parsed != null) return parsed.time
        } catch (_: Exception) {}
    }
    return 0L
}

private fun getTermForDate(dateStr: String): Int {
    val formats = listOf("dd/MM/yyyy", "yyyy-MM-dd", "d/M/yyyy", "dd-MM-yyyy")
    for (fmt in formats) {
        try {
            val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.getDefault())
            sdf.isLenient = false
            val parsed = sdf.parse(dateStr.trim())
            if (parsed != null) {
                val cal = java.util.Calendar.getInstance()
                cal.time = parsed
                val month = cal.get(java.util.Calendar.MONTH) + 1 // 1..12
                return when (month) {
                    in 9..12 -> 1 // Sep..Dec -> Trimestre 1
                    in 1..3 -> 2  // Jan..Mar -> Trimestre 2
                    else -> 3     // Apr..Aug -> Trimestre 3
                }
            }
        } catch (_: Exception) {}
    }
    return 1
}

private fun getTermPeriodDescription(term: Int, isAr: Boolean = false): String {
    return when (term) {
        1 -> if (isAr) "سبتمبر – ديسمبر" else "Septembre – Décembre"
        2 -> if (isAr) "جانفي – مارس" else "Janvier – Mars"
        3 -> if (isAr) "أفريل – جوان" else "Avril – Juin"
        else -> ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(viewModel: AppViewModel) {
    val context = LocalContext.current

    val classes by viewModel.allClasses.collectAsState()
    val selectedClassId by viewModel.selectedClassId.collectAsState()
    val journalEntries by viewModel.currentJournalEntries.collectAsState()
    val geminiSummary by viewModel.geminiSummary.collectAsState()
    val isGeneratingSummary by viewModel.isGeneratingSummary.collectAsState()

    val activeClass = classes.find { it.id == selectedClassId }

    var showAddEntryDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<com.example.data.JournalEntryEntity?>(null) }
    var entryToDelete by remember { mutableStateOf<com.example.data.JournalEntryEntity?>(null) }
    var journalSearchQuery by remember { mutableStateOf("") }
    var selectedTermFilter by remember { mutableStateOf(0) } // 0 = Tous, 1 = T1, 2 = T2, 3 = T3
    var areAllExpanded by remember { mutableStateOf<Boolean?>(null) }
    var collapsedTerms by remember { mutableStateOf(setOf<Int>()) }

    // Tri de la plus récente à la plus ancienne (dernière saisie = première affichée)
    val sortedJournalEntries = remember(journalEntries) {
        journalEntries.sortedWith(
            compareByDescending<JournalEntryEntity> {
                val t = parseDateToTimestamp(it.date)
                if (t > 0L) t else (it.id.toLong() * 1000L)
            }.thenByDescending { it.id }
        )
    }

    val term1Count = remember(sortedJournalEntries) { sortedJournalEntries.count { getTermForDate(it.date) == 1 } }
    val term2Count = remember(sortedJournalEntries) { sortedJournalEntries.count { getTermForDate(it.date) == 2 } }
    val term3Count = remember(sortedJournalEntries) { sortedJournalEntries.count { getTermForDate(it.date) == 3 } }

    val filteredJournalEntries = remember(sortedJournalEntries, journalSearchQuery, selectedTermFilter) {
        var list = sortedJournalEntries
        if (selectedTermFilter in 1..3) {
            list = list.filter { getTermForDate(it.date) == selectedTermFilter }
        }
        if (journalSearchQuery.isNotBlank()) {
            val q = journalSearchQuery.trim().lowercase()
            list = list.filter {
                it.courseSubject.lowercase().contains(q) ||
                it.objectives.lowercase().contains(q) ||
                it.observations.lowercase().contains(q) ||
                it.date.lowercase().contains(q)
            }
        }
        list
    }

    // Launcher pour l'exportation du journal en PDF
    val journalPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let {
            val targetClassName = activeClass?.name ?: "Toutes_les_classes"
            viewModel.exportSingleJournalPdf(
                context = context,
                targetUri = it,
                className = targetClassName,
                term = selectedTermFilter,
                entriesList = filteredJournalEntries
            )
        }
    }

    AdaptiveScreenContainer(maxWidth = 920.dp) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        // --- 1. SÉLECTION DE LA CLASSE (DESIGN HARMONISÉ & ÉPURÉ) ---
        item {
            var classDropdownExpanded by remember { mutableStateOf(false) }

            val isLight = MaterialTheme.colorScheme.background == OffWhite || MaterialTheme.colorScheme.background == Color(0xFFF8FAFC)
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journal_class_card"),
                shape = RoundedCornerShape(18.dp),
                elevation = 2.dp,
                borderWidth = 1.1.dp,
                glassAlpha = if (isLight) 0.88f else 0.82f
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    // Bouton sélecteur de classe moderne avec "Sélection de la classe" directement dans la case
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            onClick = { classDropdownExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                                .testTag("journal_class_dropdown_selector"),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.2.dp,
                                color = if (classDropdownExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val activePalette = if (activeClass != null) ClassColorHelper.getPaletteForClass(activeClass.id, activeClass.name) else null
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                activePalette?.container?.copy(alpha = 0.5f)
                                                    ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (activeClass != null) {
                                            ClassColorDot(activeClass.id, activeClass.name, size = 18)
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.School,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = activeClass?.name ?: stringResource(id = R.string.select_class),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (activeClass != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                                            maxLines = 1
                                        )
                                        if (activeClass != null && activeClass.schoolYear.isNotBlank()) {
                                            Text(
                                                text = "${if (Tr.isArabic()) "السنة الدراسية :" else "Année scolaire :"} ${activeClass.schoolYear}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                ) {
                                    Icon(
                                        imageVector = if (classDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(4.dp).size(22.dp)
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = classDropdownExpanded,
                            onDismissRequest = { classDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.88f)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(id = R.string.all_classes),
                                        fontWeight = if (selectedClassId == null) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                onClick = {
                                    viewModel.selectClass(null)
                                    classDropdownExpanded = false
                                }
                            )
                            HorizontalDivider()
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
                                        classDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. BOUTONS NOUVELLE ENTRÉE ET EXPORT PDF ---
        if (classes.isNotEmpty()) {
            item {
                val isAr = Tr.isArabic()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showAddEntryDialog = true },
                        modifier = Modifier
                            .weight(1.15f)
                            .height(44.dp)
                            .testTag("journal_add_entry_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = if (isAr) "إضافة درس جديد" else "Nouvelle entrée",
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAr) "إضافة درس جديد" else "Nouvelle entrée",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            if (filteredJournalEntries.isEmpty()) {
                                Toast.makeText(context, if (isAr) "لا توجد بطاقات للتصدير في هذا التحديد." else "Aucune entrée à exporter pour cette sélection.", Toast.LENGTH_SHORT).show()
                            } else {
                                val classNameSafe = activeClass?.name?.replace(" ", "_") ?: "Toutes_Classes"
                                val termSuffix = if (selectedTermFilter in 1..3) "_T$selectedTermFilter" else ""
                                journalPdfLauncher.launch("Cahier_Journal_${classNameSafe}${termSuffix}.pdf")
                            }
                        },
                        modifier = Modifier
                            .weight(0.85f)
                            .height(44.dp)
                            .testTag("journal_export_pdf_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = if (isAr) "تصدير PDF" else "Exporter en PDF",
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAr) "تصدير PDF" else "Export PDF",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // --- 3. BARRE DE RECHERCHE DANS LE JOURNAL (NETTE & VISIBLE) ---
        if (journalEntries.isNotEmpty()) {
            item {
                val isAr = Tr.isArabic()
                FineSearchBar(
                    value = journalSearchQuery,
                    onValueChange = { journalSearchQuery = it },
                    placeholder = if (isAr) "بحث عن حصة، درس، تاريخ..." else "Rechercher une séance, un thème, une date...",
                    testTag = "journal_search_bar",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // --- 4. ORGANISATION PAR TRIMESTRE & OPTIMISATION DE L'ESPACE ---
        if (journalEntries.isNotEmpty()) {
            item {
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = 1.dp,
                    borderWidth = 1.dp,
                    glassAlpha = 0.80f
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
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (Tr.isArabic()) "تنظيم حسب الفصول :" else "Organisation par trimestre :",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (sortedJournalEntries.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        val newState = !(areAllExpanded ?: false)
                                        areAllExpanded = newState
                                        if (newState) {
                                            collapsedTerms = emptySet()
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (areAllExpanded == true) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (areAllExpanded == true) {
                                            if (Tr.isArabic()) "طي الكل" else "Tout replier"
                                        } else {
                                            if (Tr.isArabic()) "توسيع الكل" else "Tout déplier"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Filtres rapides Trimestres (Tous, T1, T2, T3 visibles à la fois)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isAr = Tr.isArabic()
                            val terms = listOf(
                                0 to "${if (isAr) "الكل" else "Tous"} (${sortedJournalEntries.size})",
                                1 to "${if (isAr) "ف1" else "T1"} ($term1Count)",
                                2 to "${if (isAr) "ف2" else "T2"} ($term2Count)",
                                3 to "${if (isAr) "ف3" else "T3"} ($term3Count)"
                            )
                            terms.forEach { (termIdx, label) ->
                                val isSelected = selectedTermFilter == termIdx
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedTermFilter = termIdx },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 5. LISTE DES FICHES JOURNAL (DÉPLIABLES & ORGANISÉES PAR TRIMESTRE) ---
        if (filteredJournalEntries.isEmpty()) {
            item {
                val isAr = Tr.isArabic()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (journalSearchQuery.isNotBlank()) {
                            if (isAr) "لا توجد أي بطاقة تطابق \"$journalSearchQuery\"." else "Aucune fiche ne correspond à \"$journalSearchQuery\"."
                        } else if (selectedTermFilter in 1..3) {
                            if (isAr) "لا توجد أي بطاقة مسجلة للفصل $selectedTermFilter." else "Aucune fiche enregistrée pour le Trimestre $selectedTermFilter."
                        } else if (activeClass == null) {
                            if (isAr) "يرجى اختيار فوج لإنشاء بطاقات دفتر نصوص جديدة." else "Sélectionnez une classe pour créer de nouvelles fiches journal."
                        } else {
                            if (isAr) "لا توجد أي بطاقة في دفتر النصوص لهذا الفوج." else "Aucune fiche journal enregistrée pour cette classe."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            if (selectedTermFilter == 0) {
                // Affichage groupé par Trimestre (du plus récent au plus ancien)
                val groupedByTerm = filteredJournalEntries.groupBy { getTermForDate(it.date) }
                // Trier les trimestres du plus récent au plus ancien (ex: T3 -> T2 -> T1)
                val sortedTerms = groupedByTerm.keys.sortedByDescending { termNum ->
                    groupedByTerm[termNum]?.maxOfOrNull {
                        val t = parseDateToTimestamp(it.date)
                        if (t > 0L) t else (it.id.toLong() * 1000L)
                    } ?: 0L
                }
                sortedTerms.forEach { termNum ->
                    val entriesInTerm = groupedByTerm[termNum]
                    if (!entriesInTerm.isNullOrEmpty()) {
                        val isTermCollapsed = collapsedTerms.contains(termNum)
                        item(key = "term_header_$termNum") {
                            TrimestreSectionHeader(
                                term = termNum,
                                count = entriesInTerm.size,
                                isCollapsed = isTermCollapsed,
                                onToggleCollapse = {
                                    collapsedTerms = if (isTermCollapsed) {
                                        collapsedTerms - termNum
                                    } else {
                                        collapsedTerms + termNum
                                    }
                                }
                            )
                        }
                        if (!isTermCollapsed) {
                            items(entriesInTerm, key = { it.id }) { entry ->
                                JournalItemCard(
                                    entry = entry,
                                    classesList = classes,
                                    forceExpanded = areAllExpanded,
                                    onEdit = { entryToEdit = entry },
                                    onDelete = { entryToDelete = entry }
                                )
                            }
                        }
                    }
                }
            } else {
                // Trimestre spécifique sélectionné
                items(filteredJournalEntries, key = { it.id }) { entry ->
                    JournalItemCard(
                        entry = entry,
                        classesList = classes,
                        forceExpanded = areAllExpanded,
                        onEdit = { entryToEdit = entry },
                        onDelete = { entryToDelete = entry }
                    )
                }
            }
        }
        }
    }

    // --- ADD JOURNAL DIALOG ---
    if (showAddEntryDialog && classes.isNotEmpty()) {
        var selectedEntryClassId by remember { mutableStateOf(activeClass?.id ?: classes.first().id) }
        var date by remember { mutableStateOf(java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())) }
        var timeSlot by remember { mutableStateOf("08:00 - 09:00") }
        var subject by remember { mutableStateOf("") }
        var activity by remember { mutableStateOf("") }
        var lessonTitle by remember { mutableStateOf("") }
        var objectives by remember { mutableStateOf("") }
        var observations by remember { mutableStateOf("") }

        var classSelectExpanded by remember { mutableStateOf(false) }
        var timeSlotDropdownExpanded by remember { mutableStateOf(false) }
        var showDatePickerDialog by remember { mutableStateOf(false) }

        val standardTimeSlots = listOf(
            "08:00 - 09:00",
            "09:00 - 10:00",
            "10:00 - 11:00",
            "11:00 - 12:00",
            "13:30 - 14:30",
            "14:30 - 15:30",
            "15:30 - 16:30",
            "16:30 - 17:30",
            "08:00 - 10:00",
            "10:00 - 12:00",
            "13:30 - 15:30",
            "15:30 - 17:30"
        )

        val targetClassName = classes.find { it.id == selectedEntryClassId }?.name ?: ""
        val isAr = Tr.isArabic()

        if (showDatePickerDialog) {
            AppDatePickerDialog(
                initialDateStr = date,
                onDateSelected = { date = it },
                onDismiss = { showDatePickerDialog = false }
            )
        }

        AlertDialog(
            onDismissRequest = { showAddEntryDialog = false },
            title = { Text(if (isAr) "بطاقة جديدة : $targetClassName" else "Nouvelle fiche : $targetClassName") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = adaptiveDialogMaxHeight(fraction = 0.65f, maxHeight = 520.dp)),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Class selector inside dialog
                    item {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                onClick = { classSelectExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ClassColorDot(selectedEntryClassId, targetClassName, size = 14)
                                        Text("${if (isAr) "القسم :" else "Classe :"} $targetClassName", fontWeight = FontWeight.SemiBold)
                                    }
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = classSelectExpanded,
                                onDismissRequest = { classSelectExpanded = false }
                            ) {
                                classes.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c.name, fontWeight = if (c.id == selectedEntryClassId) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = { ClassColorDot(c.id, c.name, size = 12) },
                                        onClick = {
                                             selectedEntryClassId = c.id
                                            classSelectExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = date,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(if (isAr) "التاريخ" else "Date") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = if (isAr) "اختيار تاريخ من الرزنامة" else "Sélectionner une date",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable {
                                        showDatePickerDialog = true
                                    }
                            )
                        }
                    }
                    item {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = timeSlot,
                                onValueChange = { timeSlot = it },
                                label = { Text(if (isAr) "التوقيت (اختر أو اكتب)" else "Créneau horaire (Sélectionner ou saisir)") },
                                placeholder = { Text(if (isAr) "مثال : 08:00 - 09:00" else "Ex: 08:00 - 09:00") },
                                trailingIcon = {
                                    IconButton(onClick = { timeSlotDropdownExpanded = true }) {
                                        Icon(
                                            imageVector = if (timeSlotDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                            contentDescription = "Choisir créneau",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("journal_input_timeslot")
                            )

                            DropdownMenu(
                                expanded = timeSlotDropdownExpanded,
                                onDismissRequest = { timeSlotDropdownExpanded = false }
                            ) {
                                standardTimeSlots.forEach { slot ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = slot,
                                                fontWeight = if (slot == timeSlot) FontWeight.Bold else FontWeight.Normal,
                                                color = if (slot == timeSlot) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        onClick = {
                                            timeSlot = slot
                                            timeSlotDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = activity,
                            onValueChange = { activity = it },
                            label = { Text(if (isAr) "النشاط" else "Activité") },
                            placeholder = { Text(if (isAr) "مثال : قراءة، تعبير كتابي، دراسة نص، أشغال تطبيقية..." else "Ex: Lecture, Expression écrite, TD, TP...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("journal_input_activity")
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = lessonTitle,
                            onValueChange = { lessonTitle = it },
                            label = { Text(if (isAr) "عنوان الدرس" else "Titre du cours") },
                            placeholder = { Text(if (isAr) "مثال : المفعول به، الجملة الفعلية، حل معادلات..." else "Ex: Les temps du passé, Equations du 1er degré...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("journal_input_lesson_title")
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = objectives,
                            onValueChange = { objectives = it },
                            label = { Text(if (isAr) "الأهداف التربوية" else "Objectifs pédagogiques") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = observations,
                            onValueChange = { observations = it },
                            label = { Text(if (isAr) "ملاحظات" else "Observations") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }

                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalSubject = when {
                            activity.isNotBlank() -> activity
                            lessonTitle.isNotBlank() -> lessonTitle
                            else -> if (isAr) "حصة تعليمية" else "Séance de cours"
                        }
                        viewModel.addJournalEntry(
                            date = date,
                            timeSlot = timeSlot,
                            subject = finalSubject,
                            objectives = objectives,
                            observations = observations,
                            photoPath = null,
                            audioPath = null,
                            targetClassId = selectedEntryClassId,
                            activity = activity,
                            lessonTitle = lessonTitle
                        )
                        showAddEntryDialog = false
                    }
                ) {
                    Text(if (isAr) "إضافة" else "Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEntryDialog = false }) {
                    Text(if (isAr) "إلغاء" else "Annuler")
                }
            }
        )
    }

    // --- EDIT JOURNAL DIALOG ---
    if (entryToEdit != null && classes.isNotEmpty()) {
        val currentEditEntry = entryToEdit!!
        var selectedEntryClassId by remember(currentEditEntry.id) { mutableStateOf(currentEditEntry.classId) }
        var date by remember(currentEditEntry.id) { mutableStateOf(currentEditEntry.date) }
        var timeSlot by remember(currentEditEntry.id) { mutableStateOf(currentEditEntry.timeSlot) }
        var subject by remember(currentEditEntry.id) { mutableStateOf(currentEditEntry.courseSubject) }
        var activity by remember(currentEditEntry.id) { mutableStateOf(currentEditEntry.activity) }
        var lessonTitle by remember(currentEditEntry.id) { mutableStateOf(currentEditEntry.lessonTitle) }
        var objectives by remember(currentEditEntry.id) { mutableStateOf(currentEditEntry.objectives) }
        var observations by remember(currentEditEntry.id) { mutableStateOf(currentEditEntry.observations) }
        var classSelectExpanded by remember { mutableStateOf(false) }
        var timeSlotDropdownExpanded by remember { mutableStateOf(false) }
        var showDatePickerDialog by remember { mutableStateOf(false) }

        val standardTimeSlots = listOf(
            "08:00 - 09:00",
            "09:00 - 10:00",
            "10:00 - 11:00",
            "11:00 - 12:00",
            "13:30 - 14:30",
            "14:30 - 15:30",
            "15:30 - 16:30",
            "16:30 - 17:30",
            "08:00 - 10:00",
            "10:00 - 12:00",
            "13:30 - 15:30",
            "15:30 - 17:30"
        )

        val targetClassName = classes.find { it.id == selectedEntryClassId }?.name ?: ""
        val isAr = Tr.isArabic()

        if (showDatePickerDialog) {
            AppDatePickerDialog(
                initialDateStr = date,
                onDateSelected = { date = it },
                onDismiss = { showDatePickerDialog = false }
            )
        }

        AlertDialog(
            onDismissRequest = { entryToEdit = null },
            title = { Text(if (isAr) "تعديل بطاقة دفتر النصوص" else "Modifier la fiche journal") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = adaptiveDialogMaxHeight(fraction = 0.65f, maxHeight = 520.dp)),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                onClick = { classSelectExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ClassColorDot(selectedEntryClassId, targetClassName, size = 14)
                                        Text("${if (isAr) "القسم :" else "Classe :"} $targetClassName", fontWeight = FontWeight.SemiBold)
                                    }
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = classSelectExpanded,
                                onDismissRequest = { classSelectExpanded = false }
                            ) {
                                classes.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c.name, fontWeight = if (c.id == selectedEntryClassId) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = { ClassColorDot(c.id, c.name, size = 12) },
                                        onClick = {
                                            selectedEntryClassId = c.id
                                            classSelectExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = date,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(if (isAr) "التاريخ" else "Date") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = if (isAr) "اختيار تاريخ من الرزنامة" else "Sélectionner une date",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable {
                                        showDatePickerDialog = true
                                    }
                            )
                        }
                    }
                    item {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = timeSlot,
                                onValueChange = { timeSlot = it },
                                label = { Text(if (isAr) "التوقيت (اختر أو اكتب)" else "Créneau horaire (Sélectionner ou saisir)") },
                                placeholder = { Text(if (isAr) "مثال : 08:00 - 09:00" else "Ex: 08:00 - 09:00") },
                                trailingIcon = {
                                    IconButton(onClick = { timeSlotDropdownExpanded = true }) {
                                        Icon(
                                            imageVector = if (timeSlotDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                            contentDescription = "Choisir créneau",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("journal_edit_input_timeslot")
                            )

                            DropdownMenu(
                                expanded = timeSlotDropdownExpanded,
                                onDismissRequest = { timeSlotDropdownExpanded = false }
                            ) {
                                standardTimeSlots.forEach { slot ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = slot,
                                                fontWeight = if (slot == timeSlot) FontWeight.Bold else FontWeight.Normal,
                                                color = if (slot == timeSlot) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        onClick = {
                                            timeSlot = slot
                                            timeSlotDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = activity,
                            onValueChange = { activity = it },
                            label = { Text(if (isAr) "النشاط" else "Activité") },
                            placeholder = { Text(if (isAr) "مثال : قراءة، تعبير كتابي، دراسة نص، أشغال تطبيقية..." else "Ex: Lecture, Expression écrite, TD, TP...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("journal_edit_input_activity")
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = lessonTitle,
                            onValueChange = { lessonTitle = it },
                            label = { Text(if (isAr) "عنوان الدرس" else "Titre du cours") },
                            placeholder = { Text(if (isAr) "مثال : المفعول به، الجملة الفعلية، حل معادلات..." else "Ex: Les temps du passé, Equations du 1er degré...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("journal_edit_input_lesson_title")
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = objectives,
                            onValueChange = { objectives = it },
                            label = { Text(if (isAr) "الأهداف التربوية" else "Objectifs pédagogiques") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = observations,
                            onValueChange = { observations = it },
                            label = { Text(if (isAr) "ملاحظات" else "Observations") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalSubject = when {
                            currentEditEntry.courseSubject.isNotBlank() -> currentEditEntry.courseSubject
                            activity.isNotBlank() -> activity
                            lessonTitle.isNotBlank() -> lessonTitle
                            else -> if (isAr) "حصة تعليمية" else "Séance de cours"
                        }
                        viewModel.updateJournalEntry(
                            currentEditEntry.copy(
                                classId = selectedEntryClassId,
                                date = date,
                                timeSlot = timeSlot,
                                courseSubject = finalSubject,
                                activity = activity,
                                lessonTitle = lessonTitle,
                                objectives = objectives,
                                observations = observations
                            )
                        )
                        entryToEdit = null
                    }
                ) {
                    Text(if (isAr) "حفظ" else "Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToEdit = null }) {
                    Text(if (isAr) "إلغاء" else "Annuler")
                }
            }
        )
    }

    if (entryToDelete != null) {
        val entry = entryToDelete!!
        val isAr = Tr.isArabic()
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text(if (isAr) "تأكيد الحذف" else "Confirmer la suppression") },
            text = { Text(if (isAr) "هل تريد حقاً حذف هذه البطاقة من دفتر النصوص (${entry.courseSubject}) ؟ لا يمكن التراجع عن هذا الإجراء." else "Voulez-vous vraiment supprimer cette entrée du cahier de texte (${entry.courseSubject}) ? Cette action est irréversible.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteJournalEntry(entry.id)
                        entryToDelete = null
                    }
                ) {
                    Text(if (isAr) "حذف" else "Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text(if (isAr) "إلغاء" else "Annuler")
                }
            }
        )
    }
}

@Composable
private fun TrimestreSectionHeader(
    term: Int,
    count: Int,
    isCollapsed: Boolean = false,
    onToggleCollapse: () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggleCollapse() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                val isAr = Tr.isArabic()
                Column {
                    Text(
                        text = if (isAr) "الفصل $term" else "Trimestre $term",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = getTermPeriodDescription(term, isAr),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isAr = Tr.isArabic()
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = if (isAr) "$count بطاقة" else "$count fiche${if (count > 1) "s" else ""}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Icon(
                    imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = if (isCollapsed) "Déplier trimestre" else "Replier trimestre",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Fiche journal dépliable pour optimiser l'espace à l'écran.
 * En mode replié, elle occupe un espace minimal (~60dp) tout en affichant
 * la matière, la classe, la date, l'horaire, le badge trimestre et un aperçu.
 * En mode déplié, elle révèle l'ensemble des objectifs pédagogiques et mémos.
 */
@Composable
fun JournalItemCard(
    entry: JournalEntryEntity,
    classesList: List<com.example.data.ClassEntity>,
    forceExpanded: Boolean? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val className = classesList.find { it.id == entry.classId }?.name ?: "Classe"
    val palette = ClassColorHelper.getPaletteForClass(entry.classId, className)
    val term = getTermForDate(entry.date)

    var isExpanded by remember(forceExpanded) { mutableStateOf(forceExpanded ?: false) }

    val isLight = MaterialTheme.colorScheme.background == OffWhite || MaterialTheme.colorScheme.background == Color(0xFFF8FAFC)
    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        elevation = if (isExpanded) 3.dp else 1.5.dp,
        shape = RoundedCornerShape(18.dp),
        borderWidth = if (isExpanded) 1.4.dp else 1.1.dp,
        borderColor = if (isExpanded) palette.primary else palette.primary.copy(alpha = 0.5f),
        tintColor = palette.primary,
        glassAlpha = if (isLight) 0.88f else 0.82f,
        onClick = { isExpanded = !isExpanded }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Bandeau latéral repère de couleur de la classe
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(palette.primary)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                val isAr = Tr.isArabic()
                val isArBadge = Tr.isArabic()
                val (termBg, termText, termBorder) = when (term) {
                    1 -> Triple(Color(0xFFDBEAFE), Color(0xFF1E40AF), Color(0xFF93C5FD)) // T1 Bleu
                    2 -> Triple(Color(0xFFFEF3C7), Color(0xFF92400E), Color(0xFFFCD34D)) // T2 Ambre
                    3 -> Triple(Color(0xFFD1FAE5), Color(0xFF065F46), Color(0xFF6EE7B7)) // T3 Émeraude
                    else -> Triple(Color(0xFFEDE9FE), Color(0xFF5B21B6), Color(0xFFC4B5FD)) // T Violet
                }

                // 1. Ligne supérieure : Titre du cours à gauche, Badges Classe & Trimestre dans le coin supérieur droit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        val mainTitle = when {
                            entry.lessonTitle.isNotBlank() -> entry.lessonTitle
                            else -> entry.courseSubject.ifBlank { if (isAr) "حصة تعليمية" else "Séance de cours" }
                        }
                        Text(
                            text = mainTitle,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (entry.activity.isNotBlank() || (entry.lessonTitle.isNotBlank() && entry.courseSubject.isNotBlank())) {
                            val subLabel = buildString {
                                if (entry.activity.isNotBlank()) append(entry.activity)
                                if (entry.courseSubject.isNotBlank() && entry.lessonTitle.isNotBlank()) {
                                    if (isNotEmpty()) append(" • ")
                                    append(entry.courseSubject)
                                }
                            }
                            Text(
                                text = subLabel,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Coin supérieur : Badges Classe et Trimestre (au-dessus de la date et de l'heure)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        ClassBadge(classId = entry.classId, className = className)

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = termBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, termBorder)
                        ) {
                            Text(
                                text = if (isArBadge) "ف$term" else "T$term",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = termText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 2. Ligne inférieure : Date et Heure sous les badges, et la flèche dans le coin inférieur
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = entry.date,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        val cleanTimeSlot = entry.timeSlot.replace("\n", " ").replace("–", "-").trim()
                        if (cleanTimeSlot.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = cleanTimeSlot,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // Flèche dans le coin inférieur
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Replier" else "Déplier",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Aperçu sur 1 ligne quand replié (pour optimiser l'espace d'écran)
                val isArPreview = Tr.isArabic()
                if (!isExpanded && (entry.objectives.isNotBlank() || entry.observations.isNotBlank())) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (entry.objectives.isNotBlank()) "${if (isArPreview) "الهدف :" else "Obj :"} ${entry.objectives}" else entry.observations,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // --- CONTENU DÉPLIÉ ---
                AnimatedVisibility(visible = isExpanded) {
                    val isArDetails = Tr.isArabic()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                            thickness = 0.8.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (entry.activity.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                                Text(
                                    text = if (isArDetails) "النشاط : " else "Activité : ",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = entry.activity,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.5.sp
                                )
                            }
                        }

                        if (entry.lessonTitle.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                                Text(
                                    text = if (isArDetails) "عنوان الدرس : " else "Titre du cours : ",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = entry.lessonTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.5.sp
                                )
                            }
                        }

                        if (entry.objectives.isNotBlank()) {
                            Text(
                                text = if (isArDetails) "الأهداف التربوية والتعلمية :" else "Objectifs pédagogiques :",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = entry.objectives,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.5.sp,
                                modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                            )
                        }

                        if (entry.observations.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isArDetails) "ملاحظات وتوجيهات :" else "Observations :",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = entry.observations,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.5.sp,
                                modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                            )
                        }

                        // Attachments (si présents)
                        val isAr = Tr.isArabic()
                        if (entry.photoPath != null || entry.audioPath != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (entry.photoPath != null) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(if (isAr) "صورة مرفقة" else "Photo attachée", fontSize = 10.sp) },
                                        icon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                    )
                                }
                                if (entry.audioPath != null) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(if (isAr) "تسجيل صوتي" else "Note vocale", fontSize = 10.sp) },
                                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { isExpanded = false },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.ExpandLess, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(2.dp))
                                Text(if (isAr) "طي" else "Replier", fontSize = 11.sp)
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = onEdit,
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text(if (isAr) "تعديل" else "Modifier", fontSize = 11.sp)
                                }

                                TextButton(
                                    onClick = onDelete,
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text(if (isAr) "حذف" else "Supprimer", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
