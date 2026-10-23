package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.util.AdaptiveScreenContainer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ReminderEntity
import com.example.ui.AppViewModel
import com.example.ui.components.FineSearchBar
import com.example.ui.components.GlassmorphicCard
import com.example.util.Tr
import com.example.util.tr
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val activityContext = com.example.LocalActivityContext.current ?: context
    val reminders by viewModel.allReminders.collectAsState()

    // Request notification permission if on Android 13+ (Tiramisu)
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            val isAr = com.example.util.Tr.forcedLanguage == "ar" || com.example.util.I18n.isRtl()
            val msg = if (isAr) "تم تعطيل الإشعارات. لن تتلقى التذكيرات المبرمجة." else "Les notifications sont désactivées. Vous ne recevrez pas les rappels programmés."
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var filterCompleted by remember { mutableStateOf(false) }
    var reminderToDelete by remember { mutableStateOf<com.example.data.ReminderEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val activeReminders = reminders.filter { !it.isCompleted }
    val completedReminders = reminders.filter { it.isCompleted }
    val displayList = if (filterCompleted) completedReminders else activeReminders
    val filteredDisplayList = remember(displayList, searchQuery) {
        if (searchQuery.isBlank()) displayList
        else displayList.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    val isAr = Tr.isArabic()

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_reminder_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = if (isAr) "إضافة تذكير" else "Ajouter un rappel")
            }
        }
    ) { innerPadding ->
        AdaptiveScreenContainer(maxWidth = 920.dp) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isAr) "التذكيرات والتنبيهات" else "Rappels & Alertes",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isAr) "خطط لفروضك، اجتماعاتك ومهامك اليومية" else "Planifiez vos devoirs, réunions et tâches",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(
                        onClick = { filterCompleted = !filterCompleted },
                        modifier = Modifier.background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                    ) {
                        Icon(
                            imageVector = if (filterCompleted) Icons.Default.FilterListOff else Icons.Default.FilterList,
                            contentDescription = if (isAr) "تصفية" else "Filtrer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Fine Search Bar
            item {
                FineSearchBar(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = if (isAr) "البحث في التذكيرات..." else "Rechercher un rappel...",
                    testTag = "search_reminders_field",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Quick Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassmorphicCard(
                        modifier = Modifier.weight(1f),
                        tintColor = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp),
                        elevation = 2.dp,
                        borderWidth = 1.1.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (isAr) "النشطة" else "Actifs",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${activeReminders.size}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    GlassmorphicCard(
                        modifier = Modifier.weight(1f),
                        tintColor = MaterialTheme.colorScheme.secondary,
                        shape = RoundedCornerShape(16.dp),
                        elevation = 2.dp,
                        borderWidth = 1.1.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (isAr) "المكتملة" else "Complétés",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${completedReminders.size}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (displayList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = if (isAr) "لا توجد تذكيرات" else "Aucun rappel",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = if (filterCompleted) {
                                    if (isAr) "لا توجد تذكيرات مكتملة." else "Aucun rappel complété."
                                } else {
                                    if (isAr) "لا توجد تذكيرات حالية." else "Aucun rappel en cours."
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (filterCompleted) "" else (if (isAr) "أنشئ تذكيراً بالضغط على زر +" else "Créez un rappel avec le bouton +"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else if (filteredDisplayList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = if (isAr) "لا توجد نتائج" else "No results",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isAr) "لا يوجد أي تذكير يطابق بحثك." else "Aucun rappel ne correspond à votre recherche.",
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(filteredDisplayList, key = { it.id }) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onToggleComplete = { viewModel.toggleReminderCompleted(context, reminder) },
                        onDelete = { reminderToDelete = reminder }
                    )
                }
            }
        }
        }
    }

    // --- Add Reminder Dialog ---
    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var date by remember { mutableStateOf("") }
        var time by remember { mutableStateOf("") }
        var isNotificationEnabled by remember { mutableStateOf(true) }

        var showDatePickerDialog by remember { mutableStateOf(false) }

        if (showDatePickerDialog) {
            val initialMillis = remember(date) {
                try {
                    if (date.isNotBlank()) {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        sdf.parse(date.trim())?.time ?: System.currentTimeMillis()
                    } else {
                        System.currentTimeMillis()
                    }
                } catch (_: Exception) {
                    System.currentTimeMillis()
                }
            }
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
            DatePickerDialog(
                onDismissRequest = { showDatePickerDialog = false },
                confirmButton = {
                    Button(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                                    timeInMillis = millis
                                }
                                date = String.format(
                                    java.util.Locale.US,
                                    "%04d-%02d-%02d",
                                    cal.get(java.util.Calendar.YEAR),
                                    cal.get(java.util.Calendar.MONTH) + 1,
                                    cal.get(java.util.Calendar.DAY_OF_MONTH)
                                )
                            }
                            showDatePickerDialog = false
                        }
                    ) {
                        Text(if (isAr) "تأكيد" else "Confirmer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePickerDialog = false }) {
                        Text(if (isAr) "إلغاء" else "Annuler")
                    }
                }
            ) {
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        headlineContentColor = MaterialTheme.colorScheme.onSurface,
                        weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        subheadContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        yearContentColor = MaterialTheme.colorScheme.onSurface,
                        currentYearContentColor = MaterialTheme.colorScheme.primary,
                        selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                        selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                        dayContentColor = MaterialTheme.colorScheme.onSurface,
                        disabledDayContentColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
                        selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                        selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                        todayContentColor = MaterialTheme.colorScheme.primary,
                        todayDateBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (isAr) "تذكير جديد" else "Nouveau Rappel", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(if (isAr) "العنوان" else "Titre") },
                        placeholder = { Text(if (isAr) "مثال: إعداد كشوف النقاط" else "Ex: Préparer les bulletins") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("reminder_title_input")
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(if (isAr) "الوصف" else "Description") },
                        placeholder = { Text(if (isAr) "مثال: حجز العلامات على الأرضية الرقمية" else "Ex: Saisir les notes sur la plateforme") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Date Selector Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = date,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(if (isAr) "التاريخ" else "Date") },
                                placeholder = { Text("AAAA-MM-JJ") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                trailingIcon = {
                                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = if (isAr) "اختيار التاريخ" else "Choisir la date", tint = MaterialTheme.colorScheme.primary)
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showDatePickerDialog = true }
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = time,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(if (isAr) "الوقت" else "Heure") },
                                placeholder = { Text("HH:MM") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                trailingIcon = {
                                    Icon(imageVector = Icons.Default.AccessTime, contentDescription = if (isAr) "اختيار الوقت" else "Choisir l'heure", tint = MaterialTheme.colorScheme.primary)
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable {
                                        val now = Calendar.getInstance()
                                        TimePickerDialog(
                                            activityContext,
                                            { _, hourOfDay, minute ->
                                                time = String.format(java.util.Locale.US, "%02d:%02d", hourOfDay, minute)
                                            },
                                            now.get(Calendar.HOUR_OF_DAY),
                                            now.get(Calendar.MINUTE),
                                            true
                                        ).show()
                                    }
                            )
                        }
                    }

                    // Notification toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isNotificationEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = if (isNotificationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(if (isAr) "إشعار التنبيه" else "Notification push", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(if (isAr) "التنبيه في الوقت المحدد" else "Alerter à l'heure programmée", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Switch(
                            checked = isNotificationEnabled,
                            onCheckedChange = { isNotificationEnabled = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() && date.isNotBlank() && time.isNotBlank()) {
                            viewModel.addReminder(context, title, description, date, time, isNotificationEnabled)
                            showAddDialog = false
                        }
                    },
                    enabled = title.isNotBlank() && date.isNotBlank() && time.isNotBlank()
                ) {
                    Text(if (isAr) "إنشاء" else "Créer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(if (isAr) "إلغاء" else "Annuler")
                }
            }
        )
    }

    if (reminderToDelete != null) {
        val reminder = reminderToDelete!!
        AlertDialog(
            onDismissRequest = { reminderToDelete = null },
            title = { Text(if (isAr) "تأكيد الحذف" else "Confirmer la suppression") },
            text = { Text(if (isAr) "هل تريد حقاً حذف هذا التذكير (${reminder.title}) ؟ هذا الإجراء لا يمكن التراجع عنه." else "Voulez-vous vraiment supprimer ce rappel (${reminder.title}) ? Cette action est irréversible.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteReminder(context, reminder)
                        reminderToDelete = null
                    }
                ) {
                    Text(if (isAr) "حذف" else "Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { reminderToDelete = null }) {
                    Text(if (isAr) "إلغاء" else "Annuler")
                }
            }
        )
    }
}

@Composable
fun ReminderCard(
    reminder: ReminderEntity,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background == com.example.ui.theme.OffWhite || MaterialTheme.colorScheme.background == Color(0xFFF8FAFC)
    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminder_card_${reminder.id}"),
        shape = RoundedCornerShape(16.dp),
        elevation = 2.dp,
        borderWidth = 1.1.dp,
        tintColor = if (reminder.isCompleted) null else MaterialTheme.colorScheme.primary,
        glassAlpha = if (reminder.isCompleted) 0.65f else (if (isLight) 0.88f else 0.82f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = reminder.isCompleted,
                onCheckedChange = { onToggleComplete() },
                modifier = Modifier.testTag("reminder_checkbox_${reminder.id}")
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (reminder.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (reminder.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = reminder.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date & Time badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${reminder.date} @ ${reminder.time}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Notification active badge
                    if (reminder.isNotificationEnabled && !reminder.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = if (Tr.isArabic()) "التنبيه مفعل" else "Notification activée",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = if (Tr.isArabic()) "حذف" else "Supprimer",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}
