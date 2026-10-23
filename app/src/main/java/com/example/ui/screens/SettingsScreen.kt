package com.example.ui.screens

import android.content.Context

import android.content.Intent

import android.net.Uri

import android.widget.Toast

import androidx.core.content.FileProvider

import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.animation.AnimatedVisibility

import androidx.compose.animation.animateContentSize

import androidx.compose.animation.core.animateFloatAsState

import androidx.compose.ui.draw.rotate

import androidx.compose.ui.draw.scale

import androidx.compose.ui.draw.shadow

import androidx.compose.ui.draw.blur

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.background

import androidx.compose.foundation.border

import androidx.compose.foundation.clickable

import androidx.compose.foundation.horizontalScroll

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.isSystemInDarkTheme

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.*

import androidx.compose.material.icons.automirrored.filled.*

import androidx.compose.material3.*

import com.example.util.AdaptiveScreenContainer

import com.example.util.adaptiveDialogMaxHeight

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.compositeOver

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.platform.LocalClipboardManager

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.platform.testTag

import androidx.compose.ui.text.AnnotatedString

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.input.PasswordVisualTransformation

import androidx.compose.ui.text.style.TextAlign

import com.example.util.formatTeacherName

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import coil.compose.AsyncImage

import com.example.R

import com.example.data.SettingsEntity

import com.example.ui.AppViewModel

import com.example.util.Tr

import com.example.util.tr

import com.example.ui.components.GlassmorphicCard

import com.example.ui.theme.TajawalFontFamily

import kotlinx.coroutines.launch

private fun sendBackupViaGmail(

    context: Context,

    backupData: String,

    targetEmail: String?

) {

    try {

        val nowFormatted = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())

        val displayDate = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

        val fileName = "classheure_backup_$nowFormatted.json"

        val cacheFile = java.io.File(context.cacheDir, fileName)

        cacheFile.writeText(backupData)

        val fileUri: Uri = FileProvider.getUriForFile(

            context,

            "${context.packageName}.fileprovider",

            cacheFile

        )

        val recipient = if (!targetEmail.isNullOrBlank()) targetEmail else "fatah06@gmail.com"

        val subject = "Sauvegarde ClassHeure - $displayDate"

        val bodyText = "Bonjour,\n\nVeuillez trouver ci-joint votre fichier de sauvegarde ClassHeure réalisé le $displayDate.\n\nCe fichier contient l'ensemble de vos données (classes, élèves, notes, cahier journal et emplois du temps).\n\nPour restaurer ces données sur cet appareil ou un autre smartphone :\n1. Téléchargez la pièce jointe sur votre téléphone.\n2. Lancez ClassHeure -> Paramètres -> Sauvegarde & Restauration -> Restaurer -> Ouvrir fichier."

        val gmailIntent = Intent(Intent.ACTION_SEND).apply {

            type = "application/json"

            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))

            putExtra(Intent.EXTRA_SUBJECT, subject)

            putExtra(Intent.EXTRA_TEXT, bodyText)

            putExtra(Intent.EXTRA_STREAM, fileUri)

            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            setPackage("com.google.android.gm")

        }

        context.startActivity(gmailIntent)

    } catch (e: Exception) {

        try {

            val nowFormatted = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())

            val displayDate = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

            val fileName = "classheure_backup_$nowFormatted.json"

            val cacheFile = java.io.File(context.cacheDir, fileName)

            cacheFile.writeText(backupData)

            val fileUri: Uri = FileProvider.getUriForFile(

                context,

                "${context.packageName}.fileprovider",

                cacheFile

            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {

                type = "application/json"

                putExtra(Intent.EXTRA_EMAIL, arrayOf(if (!targetEmail.isNullOrBlank()) targetEmail else "fatah06@gmail.com"))

                putExtra(Intent.EXTRA_SUBJECT, "Sauvegarde ClassHeure - $displayDate")

                putExtra(Intent.EXTRA_STREAM, fileUri)

                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            }

            val isAr = com.example.util.Tr.forcedLanguage == "ar" || com.example.util.I18n.isRtl()

            context.startActivity(Intent.createChooser(shareIntent, if (isAr) "إرسال النسخة الاحتياطية" else "Envoyer la sauvegarde"))

        } catch (e2: Exception) {

            val isAr = com.example.util.Tr.forcedLanguage == "ar" || com.example.util.I18n.isRtl()

            Toast.makeText(context, if (isAr) "خطأ أثناء إرسال النسخة الاحتياطية : ${e2.localizedMessage}" else "Erreur lors de l'envoi de la sauvegarde : ${e2.localizedMessage}", Toast.LENGTH_LONG).show()

        }

    }

}

@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun SettingsScreen(viewModel: AppViewModel) {

    val context = LocalContext.current

    val clipboardManager = LocalClipboardManager.current

    // Google Sync preferences and state

    val coroutineScope = rememberCoroutineScope()

    val syncPrefs = remember { context.getSharedPreferences("google_sync_prefs", Context.MODE_PRIVATE) }

    var googleEmail by remember { mutableStateOf(syncPrefs.getString("email", null)) }

    var lastSyncTime by remember { mutableStateOf(syncPrefs.getString("last_sync", null)) }

    var isAutoSyncEnabled by remember { mutableStateOf(syncPrefs.getBoolean("auto_sync", false)) }

    var showGoogleLoginDialog by remember { mutableStateOf(false) }

    var showGoogleBackupsDialog by remember { mutableStateOf(false) }

    var isSyncingProgress by remember { mutableStateOf(false) }

    var syncStatusText by remember { mutableStateOf("") }

    val settingsState by viewModel.settings.collectAsState()

    val classes by viewModel.allClasses.collectAsState()

    val currentSettings = settingsState ?: SettingsEntity()

    val isAr = (currentSettings.activeLanguage == "ar") || Tr.isArabic()

    val selectedClasses = remember { mutableStateListOf<Int>() }

    val selectedTerms = remember { mutableStateListOf<Int>(1) }

    var includeGrades by remember { mutableStateOf(true) }

    var includeTimetable by remember { mutableStateOf(true) }

    var includeJournal by remember { mutableStateOf(true) }

    var schoolYearToDelete by remember { mutableStateOf<String?>(null) }

    val pdfLauncher = rememberLauncherForActivityResult(

        contract = ActivityResultContracts.CreateDocument("application/pdf")

    ) { uri: Uri? ->

        uri?.let {

            viewModel.exportReportToPdf(

                context = context,

                targetUri = it,

                classesToInclude = selectedClasses.toList(),

                termsToInclude = selectedTerms.toList(),

                includeGrades = includeGrades,

                includeTimetable = includeTimetable,

                includeJournal = includeJournal

            )

        }

    }

    var backupPasswordForFile by remember { mutableStateOf("") }

    val exportBackupLauncher = rememberLauncherForActivityResult(

        contract = ActivityResultContracts.CreateDocument("application/json")

    ) { uri: Uri? ->

        uri?.let {

            viewModel.backupDatabaseEncrypted(backupPasswordForFile, context) { base64Str ->

                if (base64Str != null) {

                    try {

                        context.contentResolver.openOutputStream(it)?.use { stream ->

                            stream.write(base64Str.toByteArray(Charsets.UTF_8))

                        }

                        try {

                            com.example.util.AppStorageHelper.saveFileToClassHeure(context, "Backup_ClassHeure.json", "application/json", base64Str.toByteArray(Charsets.UTF_8))

                        } catch (e: Exception) {

                            // Ignorer

                        }

                        // Enregistrer également dans les préférences de synchronisation cloud pour que la sauvegarde réelle soit disponible immédiatement

                        val nowStr = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

                        if (googleEmail != null) {

                            val emailClean = googleEmail!!.replace("@", "_").replace(".", "_")

                            val backupFile = java.io.File(context.filesDir, "cloud_backup_$emailClean.txt")

                            try {

                                backupFile.writeText(base64Str)

                            } catch (e: Exception) {

                                e.printStackTrace()

                            }

                        }

                        syncPrefs.edit()

                            .putString("last_sync", nowStr)

                            .putString("cloud_backup_data", base64Str)

                            .putString("cloud_backup_time", nowStr)

                            .apply()

                        lastSyncTime = nowStr

                        Toast.makeText(context, if (isAr) "تم حفظ النسخة الاحتياطية في الملف بنجاح!" else "Sauvegarde enregistrée dans le fichier !", Toast.LENGTH_LONG).show()

                    } catch (e: Exception) {

                        Toast.makeText(context, if (isAr) "خطأ في كتابة الملف." else "Erreur d'écriture du fichier.", Toast.LENGTH_SHORT).show()

                    }

                } else {

                    Toast.makeText(context, if (isAr) "خطأ أثناء إنشاء النسخة الاحتياطية." else "Erreur lors de la génération de la sauvegarde.", Toast.LENGTH_SHORT).show()

                }

            }

        }

    }

    var restorePasswordForFile by remember { mutableStateOf("") }

    val importBackupLauncher = rememberLauncherForActivityResult(

        contract = ActivityResultContracts.OpenDocument()

    ) { uri: Uri? ->

        uri?.let {

            try {

                val base64Str = context.contentResolver.openInputStream(it)?.use { stream ->

                    stream.readBytes().toString(Charsets.UTF_8).trim()

                }

                if (!base64Str.isNullOrBlank()) {

                    val passToTry = if (restorePasswordForFile.isBlank()) "google_sync_pass" else restorePasswordForFile

                    viewModel.restoreDatabaseEncrypted(base64Str, passToTry, context) { success ->

                        if (success) {

                            Toast.makeText(context, if (isAr) "تمت استعادة البيانات بنجاح!" else "Données restaurées avec succès !", Toast.LENGTH_LONG).show()

                        } else if (restorePasswordForFile.isNotBlank()) {

                            // Try fallback if user provided a password but it was actually a Gmail backup

                            viewModel.restoreDatabaseEncrypted(base64Str, "google_sync_pass", context) { fallbackSuccess ->

                                if (fallbackSuccess) {

                                    Toast.makeText(context, if (isAr) "تمت استعادة نسخة Gmail الاحتياطية بنجاح!" else "Sauvegarde Gmail restaurée avec succès !", Toast.LENGTH_LONG).show()

                                } else {

                                    Toast.makeText(context, if (isAr) "فشلت الاستعادة: كلمة المرور غير صحيحة." else "Restauration échouée : mot de passe invalide.", Toast.LENGTH_LONG).show()

                                }

                            }

                        } else {

                            Toast.makeText(context, if (isAr) "فشلت الاستعادة: كلمة المرور غير صحيحة أو الملف تالف." else "Restauration échouée : mot de passe invalide ou fichier corrompu.", Toast.LENGTH_LONG).show()

                        }

                    }

                } else {

                    Toast.makeText(context, if (isAr) "الملف فارغ أو غير صالح." else "Fichier vide ou invalide.", Toast.LENGTH_SHORT).show()

                }

            } catch (e: Exception) {

                Toast.makeText(context, if (isAr) "خطأ في قراءة الملف." else "Erreur de lecture du fichier.", Toast.LENGTH_SHORT).show()

            }

        }

    }

    // Dialog flags

    var showAddYearDialog by remember { mutableStateOf(false) }

    var showPdfExportDialog by remember { mutableStateOf(false) }

    var showBackupDialog by remember { mutableStateOf(false) }

    var showRestoreDialog by remember { mutableStateOf(false) }

    var showSetPinDialog by remember { mutableStateOf(false) }

    var showChangePinDialog by remember { mutableStateOf(false) }

    var showDisablePinDialog by remember { mutableStateOf(false) }

    var showAboutDialog by remember { mutableStateOf(false) }

    AdaptiveScreenContainer(maxWidth = 860.dp) {

        LazyColumn(

            modifier = Modifier

                .fillMaxSize()

                .then(if (showAboutDialog) Modifier.blur(16.dp) else Modifier)

                .testTag("settings_screen_scroll"),

            contentPadding = PaddingValues(16.dp),

            verticalArrangement = Arrangement.spacedBy(16.dp)

        ) {

        // --- ACCÈS RAPIDE : LANGUE & MODE JOUR/NUIT (TRÈS ACCESSIBLE) ---

        item {

            val isDarkThemeActive = currentSettings.activeTheme == "dark" || (currentSettings.activeTheme == "system" && isSystemInDarkTheme())

            val cardBg = if (isDarkThemeActive) {

                Brush.verticalGradient(

                    listOf(

                        Color(0xFF1E2230).copy(alpha = 0.95f),

                        Color(0xFF131722).copy(alpha = 0.95f)

                    )

                )

            } else {

                Brush.verticalGradient(

                    listOf(

                        Color(0xFFFFFFFF),

                        Color(0xFFF1F5F9)

                    )

                )

            }

            val cardBorder = if (isDarkThemeActive) {

                Brush.linearGradient(

                    listOf(

                        Color(0xFF38BDF8).copy(alpha = 0.6f),

                        Color(0xFF6366F1).copy(alpha = 0.4f)

                    )

                )

            } else {

                Brush.linearGradient(

                    listOf(

                        Color(0xFF93C5FD),

                        Color(0xFFCBD5E1)

                    )

                )

            }

            Surface(

                modifier = Modifier

                    .fillMaxWidth()

                    .shadow(

                        elevation = 3.dp,

                        shape = RoundedCornerShape(16.dp),

                        ambientColor = if (isDarkThemeActive) Color(0x60000000) else Color(0x18000000),

                        spotColor = if (isDarkThemeActive) Color(0x80000000) else Color(0x20000000)

                    )

                    .border(1.2.dp, cardBorder, RoundedCornerShape(16.dp)),

                shape = RoundedCornerShape(16.dp),

                color = Color.Transparent

            ) {

                Box(

                    modifier = Modifier

                        .fillMaxWidth()

                        .background(cardBg)

                        .padding(horizontal = 12.dp, vertical = 10.dp)

                ) {

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                        // En-tête compact

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.spacedBy(8.dp)

                        ) {

                            Box(

                                modifier = Modifier

                                    .size(28.dp)

                                    .clip(RoundedCornerShape(8.dp))

                                    .background(

                                        Brush.linearGradient(

                                            listOf(Color(0xFF007AFF), Color(0xFF6366F1))

                                        )

                                    ),

                                contentAlignment = Alignment.Center

                            ) {

                                Icon(

                                    imageVector = Icons.Default.BrightnessMedium,

                                    contentDescription = null,

                                    tint = Color.White,

                                    modifier = Modifier.size(16.dp)

                                )

                            }

                            Text(

                                text = if (isAr) "الوصول السريع : اللغة والمظهر" else "Accès Rapide : Langue & Affichage",

                                style = MaterialTheme.typography.titleSmall,

                                fontWeight = FontWeight.ExtraBold,

                                color = MaterialTheme.colorScheme.onSurface,

                                fontSize = 12.5.sp,

                                maxLines = 1,

                                softWrap = false,

                                overflow = TextOverflow.Ellipsis

                            )

                        }

                        // 1. Choix immédiat de la Langue

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.spacedBy(8.dp)

                        ) {

                            Box(modifier = Modifier.weight(1f)) {

                                LanguageChip(

                                    label = "🇫🇷 Français",

                                    selected = !isAr,

                                    onClick = { viewModel.updateLanguage("fr") }

                                )

                            }

                            Box(modifier = Modifier.weight(1f)) {

                                LanguageChip(

                                    label = "🇩🇿 العربية",

                                    selected = isAr,

                                    onClick = { viewModel.updateLanguage("ar") }

                                )

                            }

                        }

                        // 2. Choix immédiat du Mode Jour / Nuit / Système avec couleurs distinctives

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.spacedBy(8.dp)

                        ) {

                            val modes = listOf(

                                Triple("light", if (isAr) "☀️ نهار" else "☀️ Clair", Icons.Default.LightMode),

                                Triple("dark", if (isAr) "🌙 ليل" else "🌙 Sombre", Icons.Default.DarkMode),

                                Triple("system", if (isAr) "⚙️ تلقائي" else "⚙️ Système", Icons.Default.BrightnessAuto)

                            )

                            modes.forEach { (mode, label, _) ->

                                val isSelected = currentSettings.activeTheme == mode

                                // Couleurs soignées et distinctives selon chaque mode

                                val (borderColor, bgColor, textColor) = when (mode) {

                                    "light" -> {

                                        if (isSelected) {

                                            Triple(

                                                Color(0xFFF59E0B),

                                                if (isDarkThemeActive) Color(0xFF78350F).copy(alpha = 0.50f) else Color(0xFFFEF3C7),

                                                if (isDarkThemeActive) Color(0xFFFDE68A) else Color(0xFFB45309)

                                            )

                                        } else {

                                            Triple(

                                                if (isDarkThemeActive) Color(0xFFD97706).copy(alpha = 0.35f) else Color(0xFFFDE68A).copy(alpha = 0.8f),

                                                if (isDarkThemeActive) Color(0xFF1E2230).copy(alpha = 0.5f) else Color(0xFFFFFBEB).copy(alpha = 0.5f),

                                                if (isDarkThemeActive) Color(0xFFE2E8F0) else Color(0xFF475569)

                                            )

                                        }

                                    }

                                    "dark" -> {

                                        if (isSelected) {

                                            Triple(

                                                Color(0xFF818CF8),

                                                if (isDarkThemeActive) Color(0xFF312E81).copy(alpha = 0.60f) else Color(0xFFEEF2FF),

                                                if (isDarkThemeActive) Color(0xFFC7D2FE) else Color(0xFF3730A3)

                                            )

                                        } else {

                                            Triple(

                                                if (isDarkThemeActive) Color(0xFF818CF8).copy(alpha = 0.35f) else Color(0xFFCBD5E1),

                                                if (isDarkThemeActive) Color(0xFF1E2230).copy(alpha = 0.5f) else Color(0xFFF8FAFC).copy(alpha = 0.5f),

                                                if (isDarkThemeActive) Color(0xFFE2E8F0) else Color(0xFF475569)

                                            )

                                        }

                                    }

                                    else -> { // system

                                        if (isSelected) {

                                            Triple(

                                                Color(0xFF0284C7),

                                                if (isDarkThemeActive) Color(0xFF0C4A6E).copy(alpha = 0.60f) else Color(0xFFE0F2FE),

                                                if (isDarkThemeActive) Color(0xFFBAE6FD) else Color(0xFF0369A1)

                                            )

                                        } else {

                                            Triple(

                                                if (isDarkThemeActive) Color(0xFF0284C7).copy(alpha = 0.35f) else Color(0xFFCBD5E1),

                                                if (isDarkThemeActive) Color(0xFF1E2230).copy(alpha = 0.5f) else Color(0xFFF8FAFC).copy(alpha = 0.5f),

                                                if (isDarkThemeActive) Color(0xFFE2E8F0) else Color(0xFF475569)

                                            )

                                        }

                                    }

                                }

                                Surface(

                                    modifier = Modifier

                                        .weight(1f)

                                        .clip(RoundedCornerShape(10.dp))

                                        .clickable { viewModel.updateTheme(mode) }

                                        .shadow(if (isSelected) 2.dp else 0.dp, RoundedCornerShape(10.dp)),

                                    shape = RoundedCornerShape(10.dp),

                                    border = BorderStroke(

                                        width = if (isSelected) 1.6.dp else 0.9.dp,

                                        color = borderColor

                                    ),

                                    color = bgColor

                                ) {

                                    Text(

                                        text = label,

                                        textAlign = TextAlign.Center,

                                        fontSize = 12.sp,

                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,

                                        color = textColor,

                                        modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp),

                                        maxLines = 1,

                                        overflow = TextOverflow.Ellipsis

                                    )

                                }

                            }

                        }

                        Spacer(Modifier.height(4.dp))

                        // Choix et activation de l'effet d'animation du profil

                        val isAnimActive = (currentSettings.profileAnimationEffect.ifBlank { "autumn_tree" }) != "none"

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

                            Row(

                                modifier = Modifier.fillMaxWidth(),

                                horizontalArrangement = Arrangement.SpaceBetween,

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Text(

                                    text = if (isAr) "تفعيل حركة بطاقة الأستاذ" else "Animation de la carte profil",

                                    style = MaterialTheme.typography.labelMedium,

                                    fontWeight = FontWeight.Bold,

                                    color = if (isDarkThemeActive) Color(0xFFE2E8F0) else Color(0xFF334155)

                                )

                                Switch(

                                    checked = isAnimActive,

                                    onCheckedChange = { checked ->

                                        viewModel.updateProfileAnimationEffect(if (checked) "autumn_tree" else "none")

                                    },

                                    modifier = Modifier.scale(0.85f)

                                )

                            }

                        }

                    }

                }

            }

        }

        // --- SECTION 1: PROFIL DE L'ENSEIGNANT ---

        item {

            var teacherNameInput by remember(currentSettings.teacherName) { mutableStateOf(currentSettings.teacherName) }

            var schoolNameInput by remember(currentSettings.schoolName) { mutableStateOf(currentSettings.schoolName) }

            var taughtSubjectInput by remember(currentSettings.taughtSubject) { mutableStateOf(currentSettings.taughtSubject) }

            SettingsCard(

                title = if (isAr) "ملف الأستاذ" else "Profil de l'Enseignant",

                icon = Icons.Default.Person

            ) {

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    Text(

                        text = if (isAr) "أدخل بياناتك المهنية المعروضة في الشاشة الرئيسية وجداول التوقيت والوثائق الرسمية." else "Renseignez vos coordonnées professionnelles affichées sur l'accueil, les emplois du temps et les documents officiels.",

                        style = MaterialTheme.typography.bodySmall,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )

                    OutlinedTextField(

                        value = teacherNameInput,

                        onValueChange = {

                            teacherNameInput = formatTeacherName(it)

                        },

                        label = { Text(if (isAr) "اللقب والاسم للأستاذ" else "Nom et prénom du professeur") },

                        supportingText = { Text(if (isAr) "تنسيق الاسم بصيغة مناسبة" else "Seule la 1ère lettre de chaque mot est en majuscule", fontSize = 11.sp) },

                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },

                        singleLine = true,

                        modifier = Modifier.fillMaxWidth().testTag("teacher_name_input")

                    )

                    OutlinedTextField(

                        value = schoolNameInput,

                        onValueChange = { schoolNameInput = it },

                        label = { Text(if (isAr) "اسم المؤسسة التعليمية" else "Nom de l'établissement") },

                        placeholder = { Text(if (isAr) "مثال: ثانوية غربي محند شريف" else "Ex: Lycée Gherbi Mohand Chérif") },

                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },

                        singleLine = true,

                        modifier = Modifier.fillMaxWidth().testTag("school_name_input")

                    )

                    OutlinedTextField(

                        value = taughtSubjectInput,

                        onValueChange = { taughtSubjectInput = it },

                        label = { Text(if (isAr) "المادة المدرسة" else "Matière enseignée") },

                        placeholder = { Text(if (isAr) "مثال: الرياضيات" else "Ex: Mathématiques") },

                        leadingIcon = { Icon(Icons.Default.Book, contentDescription = null) },

                        singleLine = true,

                        modifier = Modifier.fillMaxWidth().testTag("taught_subject_input")

                    )

                    Button(

                        onClick = {

                            val formattedName = formatTeacherName(teacherNameInput)

                            viewModel.updateProfile(
                                teacherName = formattedName,
                                schoolName = schoolNameInput.trim(),
                                taughtSubject = taughtSubjectInput.trim()
                            )

                            Toast.makeText(context, if (isAr) "تم حفظ ملف الأستاذ بنجاح!" else "Profil enseignant enregistré !", Toast.LENGTH_SHORT).show()

                        },

                        modifier = Modifier

                            .fillMaxWidth()

                            .testTag("save_profile_button"),

                        shape = RoundedCornerShape(12.dp)

                    ) {

                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))

                        Spacer(Modifier.width(8.dp))

                        Text(if (isAr) "حفظ التعديلات" else "Enregistrer les modifications", fontWeight = FontWeight.Bold)

                    }

                }

            }

        }

        // --- SECTION 2: SCHOOL YEAR ---

        item {

            SettingsCard(

                title = if (isAr) "إدارة السنة الدراسية" else "Gestion de l'Année Scolaire",

                icon = Icons.Default.CalendarToday

            ) {

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    Text(

                        text = if (isAr) {

                            if (currentSettings.currentSchoolYear.isNotBlank()) "السنة الدراسية الحالية : ${currentSettings.currentSchoolYear}" else "السنة الدراسية الحالية : (غير محددة)"

                        } else {

                            if (currentSettings.currentSchoolYear.isNotBlank()) "Année Scolaire Active : ${currentSettings.currentSchoolYear}" else "Année Scolaire Active : (Non définie)"

                        },

                        fontWeight = FontWeight.Bold,

                        style = MaterialTheme.typography.bodyMedium,

                        color = MaterialTheme.colorScheme.primary

                    )

                    val schoolYears = try {

                        val array = org.json.JSONArray(currentSettings.schoolYearsJson)

                        val items = mutableListOf<String>()

                        for (i in 0 until array.length()) {

                            val yr = array.getString(i).trim()

                            if (yr.isNotBlank()) items.add(yr)

                        }

                        items

                    } catch (e: Exception) {

                        emptyList<String>()

                    }

                    var yearExpanded by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxWidth()) {

                        OutlinedTextField(

                            value = currentSettings.currentSchoolYear,

                            onValueChange = {},

                            readOnly = true,

                            label = { Text(if (isAr) "السنة الدراسية الحالية" else "Année scolaire active") },

                            placeholder = { Text(if (isAr) "اختر السنة من القائمة" else "Choisir dans la liste") },

                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },

                            modifier = Modifier

                                .fillMaxWidth()

                                .testTag("school_year_dropdown"),

                            trailingIcon = {

                                if (schoolYears.isNotEmpty()) {

                                    IconButton(onClick = { yearExpanded = true }) {

                                        Icon(Icons.Default.ArrowDropDown, contentDescription = if (isAr) "اختيار السنة" else "Choisir l'année")

                                    }

                                }

                            }

                        )

                        if (schoolYears.isNotEmpty()) {

                            DropdownMenu(

                                expanded = yearExpanded,

                                onDismissRequest = { yearExpanded = false }

                            ) {

                                schoolYears.forEach { yr ->

                                    DropdownMenuItem(

                                        text = { Text(yr) },

                                        onClick = {

                                            viewModel.updateSchoolYear(yr)

                                            yearExpanded = false

                                        }

                                    )

                                }

                            }

                        }

                    }

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement = Arrangement.spacedBy(10.dp)

                    ) {

                        Button(

                            onClick = { showAddYearDialog = true },

                            modifier = Modifier

                                .weight(1f)

                                .testTag("create_school_year_button")

                        ) {

                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(if (isAr) "إنشاء" else "Créer", fontSize = 12.sp)

                        }

                        Button(

                            onClick = {

                                viewModel.archiveCurrentSchoolYear()

                                Toast.makeText(context, if (isAr) "تمت أرشفة السنة الدراسية!" else "Année scolaire archivée !", Toast.LENGTH_SHORT).show()

                            },

                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),

                            modifier = Modifier

                                .weight(1f)

                                .testTag("archive_school_year_button")

                        ) {

                            Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(if (isAr) "أرشفة" else "Archiver", fontSize = 12.sp)

                        }

                    }

                    if (schoolYears.isNotEmpty()) {

                        Text(

                            text = if (isAr) "قائمة السنوات الدراسية :" else "Liste des Années Scolaires :",

                            fontWeight = FontWeight.Bold,

                            style = MaterialTheme.typography.bodySmall,

                            color = MaterialTheme.colorScheme.onSurfaceVariant

                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

                            schoolYears.forEach { yr ->

                                val isActive = currentSettings.currentSchoolYear == yr

                                Row(

                                    modifier = Modifier

                                        .fillMaxWidth()

                                        .clip(RoundedCornerShape(8.dp))

                                        .background(if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))

                                        .padding(horizontal = 12.dp, vertical = 6.dp),

                                    horizontalArrangement = Arrangement.SpaceBetween,

                                    verticalAlignment = Alignment.CenterVertically

                                ) {

                                    Row(

                                        verticalAlignment = Alignment.CenterVertically,

                                        horizontalArrangement = Arrangement.spacedBy(8.dp)

                                    ) {

                                        Text(

                                            text = yr,

                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,

                                            style = MaterialTheme.typography.bodyMedium,

                                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

                                        )

                                        if (isActive) {

                                            Surface(

                                                shape = RoundedCornerShape(4.dp),

                                                color = MaterialTheme.colorScheme.primary

                                            ) {

                                                Text(

                                                    text = "Active",

                                                    fontSize = 9.sp,

                                                    fontWeight = FontWeight.Bold,

                                                    color = Color.White,

                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)

                                                )

                                            }

                                        }

                                    }

                                    IconButton(

                                        onClick = {

                                            schoolYearToDelete = yr

                                        },

                                        modifier = Modifier.size(28.dp)

                                    ) {

                                        Icon(

                                            imageVector = Icons.Default.Delete,

                                            contentDescription = "Supprimer l'année scolaire",

                                            tint = MaterialTheme.colorScheme.error,

                                            modifier = Modifier.size(18.dp)

                                        )

                                    }

                                }

                            }

                        }

                    }

                    // Archived school years list

                    val archivedYears = try {

                        val array = org.json.JSONArray(currentSettings.archivedSchoolYearsJson)

                        val items = mutableListOf<String>()

                        for (i in 0 until array.length()) {

                            items.add(array.getString(i))

                        }

                        items

                    } catch (e: Exception) {

                        emptyList()

                    }

                    if (archivedYears.isNotEmpty()) {

                        HorizontalDivider()

                        Text(

                            text = "Années Scolaires Archivées :",

                            fontWeight = FontWeight.Bold,

                            style = MaterialTheme.typography.bodySmall,

                            color = MaterialTheme.colorScheme.onSurfaceVariant

                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

                            archivedYears.forEach { yr ->

                                Row(

                                    modifier = Modifier

                                        .fillMaxWidth()

                                        .clip(RoundedCornerShape(8.dp))

                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))

                                        .padding(horizontal = 12.dp, vertical = 6.dp),

                                    horizontalArrangement = Arrangement.SpaceBetween,

                                    verticalAlignment = Alignment.CenterVertically

                                ) {

                                    Row(

                                        verticalAlignment = Alignment.CenterVertically,

                                        horizontalArrangement = Arrangement.spacedBy(8.dp)

                                    ) {

                                        Text(text = yr, style = MaterialTheme.typography.bodySmall)

                                        Box(

                                            modifier = Modifier

                                                .clip(RoundedCornerShape(4.dp))

                                                .background(MaterialTheme.colorScheme.secondaryContainer)

                                                .padding(horizontal = 6.dp, vertical = 2.dp)

                                        ) {

                                            Text(

                                                text = "Archivée",

                                                fontSize = 9.sp,

                                                fontWeight = FontWeight.Bold,

                                                color = MaterialTheme.colorScheme.onSecondaryContainer

                                            )

                                        }

                                    }

                                    IconButton(

                                        onClick = {

                                            schoolYearToDelete = yr

                                        },

                                        modifier = Modifier.size(28.dp)

                                    ) {

                                        Icon(

                                            imageVector = Icons.Default.Delete,

                                            contentDescription = "Supprimer l'année archivée",

                                            tint = MaterialTheme.colorScheme.error,

                                            modifier = Modifier.size(18.dp)

                                        )

                                    }

                                }

                            }

                        }

                    }

                }

            }

        }

        // --- SECTION 3: LANGUAGE & CUSTOMIZATION ---

        item {

            val isAr = currentSettings.activeLanguage == "ar"

            SettingsCard(

                title = if (isAr) "المظهر واللغة" else "Apparence & Langue",

                icon = Icons.Default.Palette

            ) {

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // --- 1. Sélection de la Langue de l'interface (FR ⇄ AR) ---

                    Column {

                        Text(

                            text = if (isAr) "لغة الواجهة" else "Langue de l'interface",

                            fontWeight = FontWeight.SemiBold,

                            style = MaterialTheme.typography.bodyMedium

                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.spacedBy(10.dp)

                        ) {

                            LanguageChip(

                                label = "🇫🇷 Français",

                                selected = !isAr,

                                onClick = { viewModel.updateLanguage("fr") }

                            )

                            LanguageChip(

                                label = "🇩🇿 العربية",

                                selected = isAr,

                                onClick = { viewModel.updateLanguage("ar") }

                            )

                        }

                    }

                    HorizontalDivider()

                    // Fonts selection

                    val availableFonts = if (isAr) {

                        listOf("Almarai", "Tajawal")

                    } else {

                        listOf("Times New Roman", "Lora")

                    }

                    Column {

                        Text(

                            text = if (isAr) "نوع الخط" else "Style de Police",

                            fontWeight = FontWeight.SemiBold,

                            style = MaterialTheme.typography.bodyMedium

                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(

                            modifier = Modifier

                                .fillMaxWidth()

                                .horizontalScroll(rememberScrollState()),

                            horizontalArrangement = Arrangement.spacedBy(8.dp)

                        ) {

                            availableFonts.forEach { font ->

                                val isSelected = if (isAr) {

                                    if (currentSettings.activeFont in availableFonts) currentSettings.activeFont == font else font == "Almarai"

                                } else {

                                    if (currentSettings.activeFont in availableFonts) currentSettings.activeFont == font else font == "Times New Roman"

                                }

                                SettingsChip(

                                    label = font,

                                    selected = isSelected,

                                    onClick = { viewModel.updateFontFamily(font) }

                                )

                            }

                        }

                    }

                    HorizontalDivider()

                    // Visual theme display mode selection (sans Améthyste)

                    Column {

                        Text(

                            text = if (isAr) "وضع العرض (فاتح / داكن)" else "Mode d'affichage (Clair / Sombre)",

                            fontWeight = FontWeight.SemiBold,

                            style = MaterialTheme.typography.bodyMedium

                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.spacedBy(8.dp)

                        ) {

                            listOf("light", "dark", "system").forEach { theme ->

                                SettingsChip(

                                    label = when (theme) {

                                        "light" -> if (isAr) "فاتح" else "Clair"

                                        "dark" -> if (isAr) "داكن" else "Sombre"

                                        else -> if (isAr) "حسب النظام" else "Système"

                                    },

                                    selected = currentSettings.activeTheme == theme,

                                    onClick = { viewModel.updateTheme(theme) }

                                )

                            }

                        }

                    }

                    HorizontalDivider()

                    // Animation de l'encadré Profil de l'enseignant

                    val isAnimActiveSection = (currentSettings.profileAnimationEffect.ifBlank { "autumn_tree" }) != "none"

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.SpaceBetween,

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            Column(modifier = Modifier.weight(1f)) {

                                Text(

                                    text = if (isAr) "حركة بطاقة الأستاذ في الشاشة الرئيسية" else "Animation de la carte profil (Accueil)",

                                    fontWeight = FontWeight.SemiBold,

                                    style = MaterialTheme.typography.bodyMedium

                                )

                                Text(

                                    text = if (isAr) "تفعيل ظل الشجرة وأوراق الخريف المتحركة في خلفية البطاقة" else "Active l'effet arbre d'automne et feuilles animées en arrière-plan",

                                    style = MaterialTheme.typography.bodySmall,

                                    color = MaterialTheme.colorScheme.onSurfaceVariant

                                )

                            }

                            Spacer(Modifier.width(8.dp))

                            Switch(

                                checked = isAnimActiveSection,

                                onCheckedChange = { checked ->

                                    viewModel.updateProfileAnimationEffect(if (checked) "autumn_tree" else "none")

                                }

                            )

                        }

                    }

                }

            }

        }

        // --- SECTION 4: SECURITY & LOCKING ---

        item {

            SettingsCard(

                title = if (isAr) "الأمان والقفل" else "Sécurité & Verrouillage",

                icon = Icons.Default.Security

            ) {

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                    Text(

                        text = if (isAr) "حماية الوصول إلى سجلات التلاميذ، النقاط ودفتر النصوص بواسطة رمز PIN وبصمة الإصبع." else "Protégez l'accès à vos registres d'élèves, notes et cahier journal à l'aide d'un code PIN et de votre empreinte digitale.",

                        style = MaterialTheme.typography.bodySmall,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )

                    if (currentSettings.pinCode == null) {

                        Surface(

                            shape = RoundedCornerShape(12.dp),

                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),

                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),

                            modifier = Modifier.fillMaxWidth()

                        ) {

                            Row(

                                modifier = Modifier.padding(14.dp),

                                verticalAlignment = Alignment.CenterVertically,

                                horizontalArrangement = Arrangement.spacedBy(12.dp)

                            ) {

                                Box(

                                    modifier = Modifier

                                        .size(40.dp)

                                        .clip(CircleShape)

                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),

                                    contentAlignment = Alignment.Center

                                ) {

                                    Icon(

                                        imageVector = Icons.Default.LockOpen,

                                        contentDescription = null,

                                        tint = MaterialTheme.colorScheme.outline,

                                        modifier = Modifier.size(22.dp)

                                    )

                                }

                                Column(modifier = Modifier.weight(1f)) {

                                    Text(

                                        text = if (isAr) "القفل غير مفعل" else "Verrouillage désactivé",

                                        fontWeight = FontWeight.Bold,

                                        style = MaterialTheme.typography.bodyMedium,

                                        color = MaterialTheme.colorScheme.onSurface

                                    )

                                    Text(

                                        text = if (isAr) "يفتح التطبيق مباشرة دون طلب رمز أو بصمة." else "L'application s'ouvre sans demande de code ni d'empreinte.",

                                        style = MaterialTheme.typography.labelSmall,

                                        color = MaterialTheme.colorScheme.onSurfaceVariant

                                    )

                                }

                            }

                        }

                        Button(

                            onClick = { showSetPinDialog = true },

                            modifier = Modifier.fillMaxWidth().testTag("setup_pin_button"),

                            shape = RoundedCornerShape(12.dp)

                        ) {

                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))

                            Spacer(Modifier.width(8.dp))

                            Text(if (isAr) "تعيين رمز PIN" else "Définir un code PIN")

                        }

                    } else {

                        // Statut code PIN actif

                        Surface(

                            shape = RoundedCornerShape(12.dp),

                            color = Color(0xFF10B981).copy(alpha = 0.12f),

                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),

                            modifier = Modifier.fillMaxWidth()

                        ) {

                            Row(

                                modifier = Modifier.padding(12.dp),

                                verticalAlignment = Alignment.CenterVertically,

                                horizontalArrangement = Arrangement.spacedBy(10.dp)

                            ) {

                                Icon(

                                    imageVector = Icons.Default.CheckCircle,

                                    contentDescription = null,

                                    tint = Color(0xFF10B981),

                                    modifier = Modifier.size(22.dp)

                                )

                                Column {

                                    Text(

                                        text = if (isAr) "رمز PIN نشط (4 أرقام)" else "Code PIN actif (4 chiffres)",

                                        fontWeight = FontWeight.Bold,

                                        style = MaterialTheme.typography.bodyMedium,

                                        color = Color(0xFF047857)

                                    )

                                    Text(

                                        text = if (isAr) "الوصول إلى بيانات التطبيق محمي." else "L'accès à l'application est protégé.",

                                        style = MaterialTheme.typography.labelSmall,

                                        color = MaterialTheme.colorScheme.onSurfaceVariant

                                    )

                                }

                            }

                        }

                        // Boutons d'action pour le code PIN : Changer le code PIN & Désactiver

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.spacedBy(10.dp)

                        ) {

                            Button(

                                onClick = { showChangePinDialog = true },

                                modifier = Modifier.weight(1f).testTag("change_pin_button"),

                                shape = RoundedCornerShape(12.dp)

                            ) {

                                Icon(imageVector = Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(18.dp))

                                Spacer(Modifier.width(6.dp))

                                Text(if (isAr) "تغيير الرمز" else "Changer le code", maxLines = 1, fontSize = 12.5.sp)

                            }

                            OutlinedButton(

                                onClick = { showDisablePinDialog = true },

                                modifier = Modifier.weight(1f).testTag("disable_pin_button"),

                                shape = RoundedCornerShape(12.dp),

                                colors = ButtonDefaults.outlinedButtonColors(

                                    contentColor = MaterialTheme.colorScheme.error

                                ),

                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))

                            ) {

                                Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))

                                Spacer(Modifier.width(6.dp))

                                Text(if (isAr) "إلغاء التفعيل" else "Désactiver", maxLines = 1, fontSize = 12.5.sp)

                            }

                        }

                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // VERROUILLAGE PAR EMPREINTE DIGITALE

                    Row(

                        modifier = Modifier

                            .fillMaxWidth()

                            .clip(RoundedCornerShape(12.dp))

                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))

                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))

                            .padding(horizontal = 14.dp, vertical = 10.dp),

                        verticalAlignment = Alignment.CenterVertically,

                        horizontalArrangement = Arrangement.SpaceBetween

                    ) {

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.spacedBy(10.dp),

                            modifier = Modifier.weight(1f)

                        ) {

                            Surface(

                                shape = CircleShape,

                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),

                                modifier = Modifier.size(36.dp)

                            ) {

                                Box(contentAlignment = Alignment.Center) {

                                    Icon(

                                        imageVector = Icons.Default.Fingerprint,

                                        contentDescription = null,

                                        tint = MaterialTheme.colorScheme.primary,

                                        modifier = Modifier.size(20.dp)

                                    )

                                }

                            }

                            Column {

                                Text(

                                    text = if (isAr) "البصمة الحيوية" else "Empreinte digitale",

                                    style = MaterialTheme.typography.bodyMedium,

                                    fontWeight = FontWeight.Bold,

                                    color = MaterialTheme.colorScheme.onSurface

                                )

                                Text(

                                    text = if (currentSettings.pinCode == null)

                                        (if (isAr) "يتطلب أولاً إعداد رمز PIN" else "Nécessite d'abord un code PIN configuré")

                                    else

                                        (if (isAr) "فتح التطبيق بالبصمة المسجلة في هاتفك" else "Déverrouillez avec l'empreinte enregistrée sur votre smartphone"),

                                    style = MaterialTheme.typography.labelSmall,

                                    color = MaterialTheme.colorScheme.onSurfaceVariant

                                )

                            }

                        }

                        Switch(

                            checked = currentSettings.isBiometricEnabled,

                            enabled = currentSettings.pinCode != null,

                            onCheckedChange = { enable ->

                                if (enable) {

                                    val status = com.example.util.BiometricHelper.checkBiometricStatus(context)

                                    when (status) {

                                        com.example.util.BiometricHelper.BiometricStatus.NO_HARDWARE -> {

                                            Toast.makeText(context, if (isAr) "هذا الجهاز لا يدعم مستشعر البصمة." else "Cet appareil ne dispose pas d'un capteur d'empreinte digitale.", Toast.LENGTH_LONG).show()

                                        }

                                        com.example.util.BiometricHelper.BiometricStatus.NOT_ENROLLED -> {

                                            viewModel.updateBiometricEnabled(true)

                                            Toast.makeText(context, if (isAr) "تم التفعيل. تأكد من تسجيل بصمة في إعدادات الهاتف." else "Verrouillage activé. N'oubliez pas d'enregistrer une empreinte dans les paramètres Android de votre smartphone.", Toast.LENGTH_LONG).show()

                                        }

                                        else -> {

                                            viewModel.updateBiometricEnabled(true)

                                            Toast.makeText(context, if (isAr) "تم تفعيل القفل بالبصمة بنجاح." else "Verrouillage par empreinte digitale activé.", Toast.LENGTH_SHORT).show()

                                        }

                                    }

                                } else {

                                    viewModel.updateBiometricEnabled(false)

                                    Toast.makeText(context, if (isAr) "تم إلغاء القفل بالبصمة." else "Verrouillage par empreinte désactivé.", Toast.LENGTH_SHORT).show()

                                }

                            }

                        )

                    }

                }

            }

        }

        // --- SECTION 5: EXPORTS & PDF ---

        item {

            SettingsCard(

                title = if (isAr) "التقارير والمخرجات (PDF)" else "Rapports & Bulletins PDF",

                icon = Icons.Default.PictureAsPdf

            ) {

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    Text(

                        text = if (isAr) "توليد تقرير PDF شامل يجمع كشوف النقاط، بطاقات دفتر النصوص وجداول التوقيت." else "Générez un rapport PDF complet regroupant vos bulletins de notes, vos fiches de journal pédagogique et vos emplois du temps.",

                        style = MaterialTheme.typography.bodySmall,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(

                        onClick = { showPdfExportDialog = true },

                        modifier = Modifier

                            .fillMaxWidth()

                            .testTag("pdf_config_button")

                    ) {

                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(if (isAr) "تخصيص وتصدير PDF" else "Configurer & Exporter PDF")

                    }

                }

            }

        }

        // --- SECTION 6: ENCRYPTED BACKUP & RESTORE ---

        item {

            SettingsCard(

                title = if (isAr) "النسخ الاحتياطي والاستعادة المشفرة" else "Sauvegarde & Restauration chiffrée",

                icon = Icons.Default.CloudUpload

            ) {

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    Text(

                        text = if (isAr) "تصدير أو استعادة ملف أرشيف JSON مشفر بكلمة سر (AES) لكامل قاعدة البيانات المحلية." else "Exportez ou restaurez une archive JSON cryptée par mot de passe (AES) de l'ensemble de votre base locale.",

                        style = MaterialTheme.typography.bodySmall,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement = Arrangement.spacedBy(10.dp)

                    ) {

                        Button(

                            onClick = { showBackupDialog = true },

                            modifier = Modifier

                                .weight(1f)

                                .testTag("backup_db_button"),

                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)

                        ) {

                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(if (isAr) "نسخ احتياطي" else "Sauvegarder", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)

                        }

                        Button(

                            onClick = { showRestoreDialog = true },

                            modifier = Modifier

                                .weight(1f)

                                .testTag("restore_db_button"),

                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)

                        ) {

                            Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(if (isAr) "استعادة" else "Restaurer", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)

                        }

                    }

                }

            }

        }

        // --- SECTION 8: ABOUT & DEVELOPER CREDITS ---

        item {

            Card(

                modifier = Modifier

                    .fillMaxWidth()

                    .clickable { showAboutDialog = true }

                    .testTag("about_app_button"),

                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),

                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)

            ) {

                Row(

                    modifier = Modifier.padding(16.dp),

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Icon(

                        imageVector = Icons.Default.Info,

                        contentDescription = null,

                        tint = MaterialTheme.colorScheme.primary,

                        modifier = Modifier.size(24.dp)

                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(

                        text = if (isAr) "حول تطبيق ClassHeure" else "À propos de ClassHeure",

                        style = MaterialTheme.typography.titleMedium,

                        fontWeight = FontWeight.Bold,

                        color = MaterialTheme.colorScheme.onSurface

                    )

                }

            }

        }

        item {

            Spacer(modifier = Modifier.height(24.dp))

        }

    }

    }

    if (schoolYearToDelete != null) {

        val yr = schoolYearToDelete!!

        AlertDialog(

            onDismissRequest = { schoolYearToDelete = null },

            title = { Text(if (isAr) "تأكيد الحذف" else "Confirmer la suppression") },

            text = { Text(if (isAr) "هل تريد حقاً حذف السنة الدراسية \"$yr\"؟ هذا الإجراء نهائي ولا يمكن التراجع عنه." else "Voulez-vous vraiment supprimer l'année scolaire \"$yr\" ? Cette action est irréversible.") },

            confirmButton = {

                Button(

                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),

                    onClick = {

                        viewModel.deleteSchoolYear(yr)

                        schoolYearToDelete = null

                    }

                ) {

                    Text(if (isAr) "حذف" else "Supprimer")

                }

            },

            dismissButton = {

                TextButton(onClick = { schoolYearToDelete = null }) {

                    Text(if (isAr) "إلغاء" else "Annuler")

                }

            }

        )

    }

    if (showAboutDialog) {

        AboutDialog(

            onDismissRequest = { showAboutDialog = false }

        )

    }

    // --- SYNCING LOADER ---

    if (isSyncingProgress) {

        AlertDialog(

            onDismissRequest = {},

            confirmButton = {},

            title = { Text(if (isAr) "جارٍ المزامنة..." else "Synchronisation en cours...", fontWeight = FontWeight.Bold) },

            text = {

                Column(

                    horizontalAlignment = Alignment.CenterHorizontally,

                    verticalArrangement = Arrangement.spacedBy(16.dp),

                    modifier = Modifier.fillMaxWidth().padding(16.dp)

                ) {

                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)

                    Text(

                        text = syncStatusText,

                        style = MaterialTheme.typography.bodyMedium,

                        textAlign = TextAlign.Center,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )

                }

            }

        )

    }

    // --- POPUPS & ALERTS ---

    // 1. ADD NEW SCHOOL YEAR DIALOG

    if (showAddYearDialog) {

        var newYearInput by remember { mutableStateOf("") }

        AlertDialog(

            onDismissRequest = { showAddYearDialog = false },

            title = { Text(if (isAr) "إضافة سنة دراسية جديدة" else "Ajouter une année scolaire") },

            text = {

                OutlinedTextField(

                    value = newYearInput,

                    onValueChange = { newYearInput = it },

                    label = { Text(if (isAr) "مثال: 2024 / 2025" else "Ex: 2024 / 2025") },

                    singleLine = true,

                    modifier = Modifier

                        .fillMaxWidth()

                        .testTag("new_school_year_field")

                )

            },

            confirmButton = {

                Button(

                    onClick = {

                        if (newYearInput.isNotBlank()) {

                            viewModel.addSchoolYear(newYearInput.trim())

                            Toast.makeText(context, if (isAr) "تمت إضافة السنة الدراسية!" else "Année scolaire ajoutée !", Toast.LENGTH_SHORT).show()

                            showAddYearDialog = false

                        }

                    },

                    modifier = Modifier.testTag("confirm_add_school_year")

                ) {

                    Text(if (isAr) "إضافة" else "Ajouter")

                }

            },

            dismissButton = {

                TextButton(onClick = { showAddYearDialog = false }) {

                    Text(if (isAr) "إلغاء" else "Annuler")

                }

            }

        )

    }

    // 2. PDF EXPORT DIALOG

    if (showPdfExportDialog) {

        AlertDialog(

            onDismissRequest = { showPdfExportDialog = false },

            title = { Text(if (isAr) "تخصيص تصدير PDF" else "Personnaliser l'export PDF") },

            text = {

                LazyColumn(

                    modifier = Modifier

                        .fillMaxWidth()

                        .heightIn(max = adaptiveDialogMaxHeight(fraction = 0.60f, maxHeight = 480.dp)),

                    verticalArrangement = Arrangement.spacedBy(12.dp)

                ) {

                    item {

                        Text(if (isAr) "الأفواج المعنية :" else "Classes à inclure :", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)

                    }

                    if (classes.isEmpty()) {

                        item {

                            Text(if (isAr) "لا توجد أفواج متاحة." else "Aucune classe disponible.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

                        }

                    } else {

                        items(classes) { c ->

                            val checked = selectedClasses.contains(c.id)

                            Row(

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .clickable {

                                        if (checked) selectedClasses.remove(c.id) else selectedClasses.add(c.id)

                                    }

                                    .padding(vertical = 4.dp),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Checkbox(

                                    checked = checked,

                                    onCheckedChange = {

                                        if (it == true) selectedClasses.add(c.id) else selectedClasses.remove(c.id)

                                    }

                                )

                                Text(c.name, style = MaterialTheme.typography.bodyMedium)

                            }

                        }

                    }

                    item {

                        HorizontalDivider()

                        Text(if (isAr) "الفصول المعنية (للنقاط) :" else "Trimestres (pour les notes) :", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)

                    }

                    items((1..3).toList()) { term ->

                        val checked = selectedTerms.contains(term)

                        Row(

                            modifier = Modifier

                                .fillMaxWidth()

                                .clickable {

                                    if (checked) selectedTerms.remove(term) else selectedTerms.add(term)

                                }

                                .padding(vertical = 4.dp),

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            Checkbox(

                                checked = checked,

                                onCheckedChange = {

                                    if (it == true) selectedTerms.add(term) else selectedTerms.remove(term)

                                }

                            )

                            Text(if (isAr) "الفصل $term" else "Trimestre $term", style = MaterialTheme.typography.bodyMedium)

                        }

                    }

                    item {

                        HorizontalDivider()

                        Text(if (isAr) "المحتويات المراد تضمينها :" else "Sections à inclure :", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)

                    }

                    item {

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            modifier = Modifier

                                .fillMaxWidth()

                                .clickable { includeGrades = !includeGrades }

                                .padding(vertical = 4.dp)

                        ) {

                            Checkbox(checked = includeGrades, onCheckedChange = { includeGrades = it == true })

                            Text(if (isAr) "كشوف النقاط ودفتر التنقيط" else "Bulletins & Carnet de notes", style = MaterialTheme.typography.bodyMedium)

                        }

                    }

                    item {

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            modifier = Modifier

                                .fillMaxWidth()

                                .clickable { includeTimetable = !includeTimetable }

                                .padding(vertical = 4.dp)

                        ) {

                            Checkbox(checked = includeTimetable, onCheckedChange = { includeTimetable = it == true })

                            Text(if (isAr) "جداول التوقيت الأسبوعية" else "Grilles d'emplois du temps", style = MaterialTheme.typography.bodyMedium)

                        }

                    }

                    item {

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            modifier = Modifier

                                .fillMaxWidth()

                                .clickable { includeJournal = !includeJournal }

                                .padding(vertical = 4.dp)

                        ) {

                            Checkbox(checked = includeJournal, onCheckedChange = { includeJournal = it == true })

                            Text(if (isAr) "بطاقات دفتر النصوص (الكراس اليومي)" else "Fiches du cahier journal", style = MaterialTheme.typography.bodyMedium)

                        }

                    }

                }

            },

            confirmButton = {

                Button(

                    onClick = {

                        if (selectedClasses.isEmpty()) {

                            Toast.makeText(context, if (isAr) "يرجى اختيار فوج واحد على الأقل!" else "Sélectionnez au moins une classe !", Toast.LENGTH_SHORT).show()

                            return@Button

                        }

                        pdfLauncher.launch("ClassHeure_rapport_${System.currentTimeMillis()}.pdf")

                        showPdfExportDialog = false

                    },

                    modifier = Modifier.testTag("confirm_pdf_generation")

                ) {

                    Text(if (isAr) "توليد" else "Générer")

                }

            },

            dismissButton = {

                TextButton(onClick = { showPdfExportDialog = false }) {

                    Text(if (isAr) "إلغاء" else "Annuler")

                }

            }

        )

    }

    // 3. BACKUP DIALOG

    if (showBackupDialog) {

        var password by remember { mutableStateOf("") }

        var generatedBase64 by remember { mutableStateOf<String?>(null) }

        AlertDialog(

            onDismissRequest = { showBackupDialog = false },

            title = { Text(if (isAr) "نسخ احتياطي مشفر (AES)" else "Sauvegarde cryptée (AES)") },

            text = {

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    if (generatedBase64 == null) {

                        Text(

                            text = if (isAr) "أدخل كلمة سر لتشفير ملف النسخ الاحتياطي. ستحتاجها حتماً عند استعادة البيانات!" else "Entrez un mot de passe pour chiffrer votre fichier de sauvegarde. Vous en aurez absolument besoin pour restaurer les données !",

                            style = MaterialTheme.typography.bodySmall

                        )

                        OutlinedTextField(

                            value = password,

                            onValueChange = { password = it },

                            label = { Text(if (isAr) "كلمة السر" else "Mot de passe") },

                            visualTransformation = PasswordVisualTransformation(),

                            singleLine = true,

                            modifier = Modifier

                                .fillMaxWidth()

                                .testTag("backup_password_field")

                        )

                    } else {

                        Text(

                            text = if (isAr) "تم إنشاء نسخة مشفرة بنجاح. انسخ هذا النص واحفظه في مكان آمن :" else "Votre archive de sauvegarde cryptée a été générée. Copiez ce code et enregistrez-le en toute sécurité :",

                            style = MaterialTheme.typography.bodySmall

                        )

                        OutlinedTextField(

                            value = generatedBase64 ?: "",

                            onValueChange = {},

                            readOnly = true,

                            modifier = Modifier

                                .fillMaxWidth()

                                .height(130.dp)

                                .testTag("generated_backup_field")

                        )

                    }

                }

            },

            confirmButton = {

                if (generatedBase64 == null) {

                    Column(

                        modifier = Modifier.fillMaxWidth(),

                        verticalArrangement = Arrangement.spacedBy(8.dp)

                    ) {

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.spacedBy(8.dp)

                        ) {

                            OutlinedButton(

                                onClick = {

                                    if (password.length >= 4) {

                                        backupPasswordForFile = password

                                        exportBackupLauncher.launch("classheure_backup_${System.currentTimeMillis()}.json")

                                        showBackupDialog = false

                                    } else {

                                        Toast.makeText(context, if (isAr) "كلمة السر يجب أن تحتوي 4 أحرف على الأقل." else "Mot de passe de 4 caractères minimum.", Toast.LENGTH_SHORT).show()

                                    }

                                },

                                modifier = Modifier.weight(1f).height(40.dp),

                                shape = RoundedCornerShape(12.dp)

                            ) {

                                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(if (isAr) "حفظ في ملف" else "Sauver fichier", fontSize = 11.sp, maxLines = 1)

                            }

                            OutlinedButton(

                                onClick = {

                                    if (password.length >= 4) {

                                        viewModel.backupDatabaseEncrypted(password, context) { result ->

                                            if (result != null) {

                                                generatedBase64 = result

                                                val nowStr = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

                                                syncPrefs.edit()

                                                    .putString("last_sync", nowStr)

                                                    .putString("cloud_backup_data", result)

                                                    .putString("cloud_backup_time", nowStr)

                                                    .apply()

                                                lastSyncTime = nowStr

                                            } else {

                                                Toast.makeText(context, if (isAr) "خطأ في إنشاء النسخة الاحتياطية." else "Erreur de sauvegarde.", Toast.LENGTH_SHORT).show()

                                            }

                                        }

                                    } else {

                                        Toast.makeText(context, if (isAr) "كلمة السر يجب أن تحتوي 4 أحرف على الأقل." else "Mot de passe de 4 caractères minimum.", Toast.LENGTH_SHORT).show()

                                    }

                                },

                                modifier = Modifier.weight(1f).height(40.dp).testTag("generate_backup_button"),

                                shape = RoundedCornerShape(12.dp)

                            ) {

                                Text(if (isAr) "توليد نص" else "Générer texte", fontSize = 11.sp, maxLines = 1)

                            }

                        }

                    }

                } else {

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement = Arrangement.spacedBy(8.dp)

                    ) {

                        Button(

                            onClick = {

                                sendBackupViaGmail(context, generatedBase64 ?: "", googleEmail ?: "fatah06@gmail.com")

                                showBackupDialog = false

                            },

                            modifier = Modifier.weight(1f)

                        ) {

                            Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(if (isAr) "عبر Gmail" else "Via Gmail", fontSize = 11.sp)

                        }

                        Button(

                            onClick = {

                                clipboardManager.setText(AnnotatedString(generatedBase64 ?: ""))

                                Toast.makeText(context, if (isAr) "تم نسخ الشفرة بنجاح!" else "Sauvegarde copiée !", Toast.LENGTH_SHORT).show()

                                showBackupDialog = false

                            },

                            modifier = Modifier.weight(1f).testTag("copy_backup_button")

                        ) {

                            Text(if (isAr) "نسخ" else "Copier", fontSize = 11.sp)

                        }

                    }

                }

            },

            dismissButton = {

                TextButton(onClick = { showBackupDialog = false }) {

                    Text(if (isAr) "إغلاق" else "Fermer")

                }

            }

        )

    }

    // 4. RESTORE DIALOG

    if (showRestoreDialog) {

        var password by remember { mutableStateOf("") }

        var base64Input by remember { mutableStateOf("") }

        AlertDialog(

            onDismissRequest = { showRestoreDialog = false },

            title = { Text(if (isAr) "استعادة نسخة احتياطية" else "Restaurer une sauvegarde") },

            text = {

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    Text(

                        text = if (isAr) "أدخل كلمة السر (اتركها فارغة إذا كانت نسخة مرسلة عبر Gmail)، ثم اضغط على فتح ملف." else "Entrez le mot de passe secret (laissez vide si c'est une sauvegarde envoyée par Gmail), puis cliquez sur Ouvrir fichier.",

                        style = MaterialTheme.typography.bodySmall

                    )

                    OutlinedTextField(

                        value = base64Input,

                        onValueChange = { base64Input = it },

                        label = { Text(if (isAr) "الشفرة (Base64)" else "Code crypté (Base64)") },

                        modifier = Modifier

                            .fillMaxWidth()

                            .height(100.dp)

                            .testTag("restore_base64_field")

                    )

                    OutlinedTextField(

                        value = password,

                        onValueChange = { password = it },

                        label = { Text(if (isAr) "كلمة السر" else "Mot de passe") },

                        visualTransformation = PasswordVisualTransformation(),

                        singleLine = true,

                        modifier = Modifier

                            .fillMaxWidth()

                            .testTag("restore_password_field")

                    )

                }

            },

            confirmButton = {

                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.spacedBy(8.dp),

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    OutlinedButton(

                        onClick = {

                            restorePasswordForFile = password

                            importBackupLauncher.launch(arrayOf("*/*"))

                            showRestoreDialog = false

                        },

                        modifier = Modifier.weight(1f).height(44.dp),

                        shape = RoundedCornerShape(12.dp)

                    ) {

                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(if (isAr) "فتح ملف" else "Ouvrir fichier", fontSize = 11.sp, maxLines = 1)

                    }

                    Button(

                        onClick = {

                            if (base64Input.isNotBlank() && password.isNotBlank()) {

                                viewModel.restoreDatabaseEncrypted(base64Input.trim(), password, context) { success ->

                                    if (success) {

                                        Toast.makeText(context, if (isAr) "تمت استعادة البيانات بنجاح!" else "Données restaurées !", Toast.LENGTH_SHORT).show()

                                        showRestoreDialog = false

                                    } else {

                                        Toast.makeText(context, if (isAr) "فشل فك التشفير (الرمز أو كلمة السر غير صحيحة)." else "Décryptage échoué (code ou mot de passe invalide).", Toast.LENGTH_LONG).show()

                                    }

                                }

                            } else {

                                Toast.makeText(context, if (isAr) "يرجى ملء جميع الحقول." else "Veuillez remplir tous les champs.", Toast.LENGTH_SHORT).show()

                            }

                        },

                        modifier = Modifier.weight(1f).height(44.dp).testTag("confirm_restore_button"),

                        shape = RoundedCornerShape(12.dp)

                    ) {

                        Text(if (isAr) "استعادة" else "Restaurer", fontSize = 11.sp, maxLines = 1)

                    }

                }

            },

            dismissButton = {

                TextButton(onClick = { showRestoreDialog = false }) {

                    Text(if (isAr) "إلغاء" else "Annuler")

                }

            }

        )

    }

    // 5. SET PIN DIALOG

    if (showSetPinDialog) {

        var newPin by remember { mutableStateOf("") }

        var confirmPin by remember { mutableStateOf("") }

        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(

            onDismissRequest = { showSetPinDialog = false },

            title = {

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)

                    Text(if (isAr) "تعيين رمز PIN" else "Définir un code PIN")

                }

            },

            text = {

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    Text(

                        text = if (isAr) "اختر رمز PIN مكوناً من 4 أرقام لتأمين الوصول إلى بياناتك." else "Choisissez un code PIN à 4 chiffres pour sécuriser l'accès à vos données.",

                        style = MaterialTheme.typography.bodySmall,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )

                    OutlinedTextField(

                        value = newPin,

                        onValueChange = {

                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {

                                newPin = it

                                errorMsg = null

                            }

                        },

                        label = { Text(if (isAr) "رمز PIN (4 أرقام)" else "Code PIN (4 chiffres)") },

                        placeholder = { Text("Ex: 1234") },

                        visualTransformation = PasswordVisualTransformation(),

                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(

                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword

                        ),

                        singleLine = true,

                        modifier = Modifier.fillMaxWidth().testTag("new_pin_input")

                    )

                    OutlinedTextField(

                        value = confirmPin,

                        onValueChange = {

                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {

                                confirmPin = it

                                errorMsg = null

                            }

                        },

                        label = { Text(if (isAr) "تأكيد رمز PIN" else "Confirmer le code PIN") },

                        placeholder = { Text(if (isAr) "أعد كتابة الرمز" else "Répétez le code") },

                        visualTransformation = PasswordVisualTransformation(),

                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(

                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword

                        ),

                        singleLine = true,

                        modifier = Modifier.fillMaxWidth().testTag("confirm_pin_input")

                    )

                    if (errorMsg != null) {

                        Text(

                            text = errorMsg ?: "",

                            color = MaterialTheme.colorScheme.error,

                            style = MaterialTheme.typography.labelMedium,

                            fontWeight = FontWeight.Bold

                        )

                    }

                }

            },

            confirmButton = {

                Button(

                    onClick = {

                        if (newPin.length != 4) {

                            errorMsg = if (isAr) "يجب أن يتكون رمز PIN من 4 أرقام بالضبط." else "Le code PIN doit comporter exactement 4 chiffres."

                        } else if (newPin != confirmPin) {

                            errorMsg = if (isAr) "الرمزان غير متطابقين." else "Les deux codes ne correspondent pas."

                        } else {

                            viewModel.updatePinCode(newPin)

                            showSetPinDialog = false

                            Toast.makeText(context, if (isAr) "تم تفعيل رمز PIN بنجاح!" else "Code PIN activé avec succès !", Toast.LENGTH_SHORT).show()

                        }

                    },

                    modifier = Modifier.testTag("confirm_set_pin")

                ) {

                    Text(if (isAr) "حفظ" else "Enregistrer")

                }

            },

            dismissButton = {

                TextButton(onClick = { showSetPinDialog = false }) {

                    Text(if (isAr) "إلغاء" else "Annuler")

                }

            }

        )

    }

    // 6. CHANGE PIN DIALOG

    if (showChangePinDialog) {

        var currentPinInput by remember { mutableStateOf("") }

        var newPinInput by remember { mutableStateOf("") }

        var confirmPinInput by remember { mutableStateOf("") }

        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(

            onDismissRequest = { showChangePinDialog = false },

            title = {

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    Icon(Icons.Default.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.primary)

                    Text(if (isAr) "تغيير رمز PIN" else "Changer le code de verrouillage")

                }

            },

            text = {

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    Text(

                        text = if (isAr) "أدخل رمز PIN الحالي، ثم اختر رمزك الجديد المكون من 4 أرقام." else "Saisissez votre code PIN actuel, puis choisissez votre nouveau code à 4 chiffres.",

                        style = MaterialTheme.typography.bodySmall,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )

                    OutlinedTextField(

                        value = currentPinInput,

                        onValueChange = {

                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {

                                currentPinInput = it

                                errorMsg = null

                            }

                        },

                        label = { Text(if (isAr) "رمز PIN الحالي" else "Code PIN actuel") },

                        visualTransformation = PasswordVisualTransformation(),

                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(

                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword

                        ),

                        singleLine = true,

                        modifier = Modifier.fillMaxWidth().testTag("current_pin_input")

                    )

                    OutlinedTextField(

                        value = newPinInput,

                        onValueChange = {

                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {

                                newPinInput = it

                                errorMsg = null

                            }

                        },

                        label = { Text(if (isAr) "رمز PIN الجديد (4 أرقام)" else "Nouveau code PIN (4 chiffres)") },

                        visualTransformation = PasswordVisualTransformation(),

                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(

                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword

                        ),

                        singleLine = true,

                        modifier = Modifier.fillMaxWidth().testTag("new_change_pin_input")

                    )

                    OutlinedTextField(

                        value = confirmPinInput,

                        onValueChange = {

                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {

                                confirmPinInput = it

                                errorMsg = null

                            }

                        },

                        label = { Text(if (isAr) "تأكيد رمز PIN الجديد" else "Confirmer le nouveau code PIN") },

                        visualTransformation = PasswordVisualTransformation(),

                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(

                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword

                        ),

                        singleLine = true,

                        modifier = Modifier.fillMaxWidth().testTag("confirm_change_pin_input")

                    )

                    if (errorMsg != null) {

                        Text(

                            text = errorMsg ?: "",

                            color = MaterialTheme.colorScheme.error,

                            style = MaterialTheme.typography.labelMedium,

                            fontWeight = FontWeight.Bold

                        )

                    }

                }

            },

            confirmButton = {

                Button(

                    onClick = {

                        if (currentPinInput != currentSettings.pinCode) {

                            errorMsg = if (isAr) "رمز PIN الحالي غير صحيح." else "Le code PIN actuel est incorrect."

                        } else if (newPinInput.length != 4) {

                            errorMsg = if (isAr) "الرمز الجديد يجب أن يتكون من 4 أرقام بالضبط." else "Le nouveau code doit comporter exactement 4 chiffres."

                        } else if (newPinInput != confirmPinInput) {

                            errorMsg = if (isAr) "الرمزان الجديدان غير متطابقين." else "Les deux nouveaux codes ne correspondent pas."

                        } else {

                            viewModel.updatePinCode(newPinInput)

                            showChangePinDialog = false

                            Toast.makeText(context, if (isAr) "تم تغيير رمز القفل بنجاح!" else "Code de verrouillage modifié avec succès !", Toast.LENGTH_SHORT).show()

                        }

                    },

                    modifier = Modifier.testTag("confirm_change_pin_button")

                ) {

                    Text(if (isAr) "تأكيد" else "Valider")

                }

            },

            dismissButton = {

                TextButton(onClick = { showChangePinDialog = false }) {

                    Text(if (isAr) "إلغاء" else "Annuler")

                }

            }

        )

    }

    // 7. DISABLE PIN CONFIRMATION DIALOG

    if (showDisablePinDialog) {

        var pinVerify by remember { mutableStateOf("") }

        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(

            onDismissRequest = { showDisablePinDialog = false },

            title = { Text(if (isAr) "إلغاء تفعيل القفل" else "Désactiver le verrouillage") },

            text = {

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    Text(

                        text = if (isAr) "يرجى إدخال رمز PIN الحالي لإلغاء القفل بالرمز والبصمة." else "Veuillez saisir votre code PIN actuel pour désactiver le verrouillage par PIN et empreinte.",

                        style = MaterialTheme.typography.bodySmall,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )

                    OutlinedTextField(

                        value = pinVerify,

                        onValueChange = {

                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {

                                pinVerify = it

                                errorMsg = null

                            }

                        },

                        label = { Text(if (isAr) "رمز PIN الحالي" else "Code PIN actuel") },

                        visualTransformation = PasswordVisualTransformation(),

                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(

                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword

                        ),

                        singleLine = true,

                        modifier = Modifier.fillMaxWidth().testTag("disable_pin_verify_input")

                    )

                    if (errorMsg != null) {

                        Text(text = errorMsg ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)

                    }

                }

            },

            confirmButton = {

                Button(

                    onClick = {

                        if (pinVerify == currentSettings.pinCode) {

                            viewModel.updatePinCode(null)

                            viewModel.updateBiometricEnabled(false)

                            showDisablePinDialog = false

                            Toast.makeText(context, if (isAr) "تم إلغاء القفل بنجاح." else "Verrouillage désactivé.", Toast.LENGTH_SHORT).show()

                        } else {

                            errorMsg = if (isAr) "رمز PIN غير صحيح." else "Code PIN incorrect."

                        }

                    },

                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)

                ) {

                    Text(if (isAr) "إلغاء التفعيل" else "Désactiver")

                }

            },

            dismissButton = {

                TextButton(onClick = { showDisablePinDialog = false }) {

                    Text(if (isAr) "إلغاء" else "Annuler")

                }

            }

        )

    }

}

@Composable

fun HeroSettingsHeader(

    teacherName: String,

    schoolName: String,

    schoolYear: String

) {

    val isAr = Tr.isArabic()

    Card(

        modifier = Modifier

            .fillMaxWidth()

            .clip(RoundedCornerShape(24.dp)),

        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),

        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)

    ) {

        Row(

            modifier = Modifier

                .fillMaxWidth()

                .padding(20.dp),

            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement = Arrangement.spacedBy(16.dp)

        ) {

            Box(

                modifier = Modifier

                    .size(46.dp)

                    .clip(CircleShape)

                    .background(Color.White.copy(alpha = 0.2f))

                    .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),

                contentAlignment = Alignment.Center

            ) {

                Icon(

                    imageVector = Icons.Default.Person,

                    contentDescription = null,

                    tint = Color.White,

                    modifier = Modifier.size(24.dp)

                )

            }

            Column(

                modifier = Modifier.weight(1f),

                verticalArrangement = Arrangement.Center

            ) {

                Text(

                    text = if (teacherName.isNotBlank()) teacherName else if (isAr) "الأستاذ" else "Enseignant",

                    fontFamily = if (isAr) com.example.ui.theme.AlmaraiFontFamily else null,

                    style = MaterialTheme.typography.titleLarge,

                    fontWeight = FontWeight.Bold,

                    fontSize = if (isAr) 22.sp else 20.sp,

                    lineHeight = if (isAr) 28.sp else 24.sp,

                    color = Color.White,

                    maxLines = 1,

                    overflow = TextOverflow.Ellipsis

                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(

                    text = if (schoolName.isNotBlank()) schoolName else if (isAr) "المؤسسة غير محددة" else "Établissement non configuré",

                    style = MaterialTheme.typography.bodyMedium,

                    color = Color.White.copy(alpha = 0.9f),

                    maxLines = 1,

                    overflow = TextOverflow.Ellipsis

                )

                if (schoolYear.isNotBlank()) {

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(

                        text = if (isAr) "السنة الدراسية: $schoolYear" else "Année scolaire : $schoolYear",

                        style = MaterialTheme.typography.bodySmall,

                        color = Color.White.copy(alpha = 0.8f),

                        maxLines = 1,

                        overflow = TextOverflow.Ellipsis

                    )

                }

            }

        }

    }

}

@Composable

fun SettingsCard(

    title: String,

    icon: androidx.compose.ui.graphics.vector.ImageVector,

    initialExpanded: Boolean = false,

    content: @Composable ColumnScope.() -> Unit

) {

    val isAr = Tr.isArabic()

    var expanded by remember { mutableStateOf(initialExpanded) }

    val isLight = MaterialTheme.colorScheme.background == com.example.ui.theme.OffWhite || MaterialTheme.colorScheme.background == Color(0xFFF8FAFC)

    // Palette d'icônes et d'accentuation iOS 27 dynamique selon la rubrique

    val iconGradient = remember(title) {

        when {

            title.contains("Profil", ignoreCase = true) || title.contains("الأستاذ", ignoreCase = true) ->

                listOf(Color(0xFF007AFF), Color(0xFF38BDF8))

            title.contains("Année", ignoreCase = true) || title.contains("السنة", ignoreCase = true) ->

                listOf(Color(0xFF10B981), Color(0xFF34D399))

            title.contains("Thème", ignoreCase = true) || title.contains("المظهر", ignoreCase = true) || title.contains("Affichage", ignoreCase = true) ->

                listOf(Color(0xFF8B5CF6), Color(0xFFC084FC))

            title.contains("Emploi", ignoreCase = true) || title.contains("التوقيت", ignoreCase = true) ->

                listOf(Color(0xFFF59E0B), Color(0xFFFBBF24))

            title.contains("Sécurité", ignoreCase = true) || title.contains("الأمان", ignoreCase = true) || title.contains("PIN", ignoreCase = true) ->

                listOf(Color(0xFFEF4444), Color(0xFFF87171))

            title.contains("Sauvegarde", ignoreCase = true) || title.contains("الاحتياطي", ignoreCase = true) ->

                listOf(Color(0xFF6366F1), Color(0xFF818CF8))

            title.contains("Rapport", ignoreCase = true) || title.contains("التقارير", ignoreCase = true) || title.contains("PDF", ignoreCase = true) ->

                listOf(Color(0xFF06B6D4), Color(0xFF22D3EE))

            title.contains("Google", ignoreCase = true) || title.contains("مزامنة", ignoreCase = true) || title.contains("Sync", ignoreCase = true) ->

                listOf(Color(0xFF0284C7), Color(0xFF38BDF8))

            else ->

                listOf(Color(0xFF007AFF), Color(0xFF60A5FA))

        }

    }

    val rubricAccent = iconGradient.first()

    val rotationAngle by androidx.compose.animation.core.animateFloatAsState(

        targetValue = if (expanded) 180f else 0f,

        label = "chevron_rotation"

    )

    val cardBg = if (isLight) {

        rubricAccent.copy(alpha = 0.06f).compositeOver(Color.White)

    } else {

        rubricAccent.copy(alpha = 0.14f).compositeOver(Color(0xFF161821))

    }

    val cardBorder = Brush.linearGradient(

        listOf(

            (if (isLight) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.35f)),

            rubricAccent.copy(alpha = if (isLight) 0.45f else 0.55f),

            rubricAccent.copy(alpha = if (isLight) 0.20f else 0.25f)

        )

    )

    Surface(

        modifier = Modifier

            .fillMaxWidth()

            .animateContentSize()

            .shadow(

                elevation = if (isLight) 3.5.dp else 4.5.dp,

                shape = RoundedCornerShape(22.dp),

                ambientColor = rubricAccent.copy(alpha = if (isLight) 0.16f else 0.35f),

                spotColor = rubricAccent.copy(alpha = if (isLight) 0.24f else 0.45f)

            )

            .border(

                width = 1.2.dp,

                brush = cardBorder,

                shape = RoundedCornerShape(22.dp)

            ),

        shape = RoundedCornerShape(22.dp),

        color = cardBg

    ) {

        Column(

            modifier = Modifier.padding(16.dp),

            verticalArrangement = Arrangement.spacedBy(14.dp)

        ) {

            Row(

                modifier = Modifier

                    .fillMaxWidth()

                    .clickable { expanded = !expanded },

                verticalAlignment = Alignment.CenterVertically,

                horizontalArrangement = Arrangement.SpaceBetween

            ) {

                Row(

                    verticalAlignment = Alignment.CenterVertically,

                    horizontalArrangement = Arrangement.spacedBy(12.dp),

                    modifier = Modifier.weight(1f)

                ) {

                    // Squircle d'icône emblématique iOS 27

                    Box(

                        modifier = Modifier

                            .size(38.dp)

                            .shadow(2.5.dp, RoundedCornerShape(11.dp))

                            .clip(RoundedCornerShape(11.dp))

                            .background(Brush.linearGradient(iconGradient))

                            .border(

                                1.dp,

                                Brush.linearGradient(

                                    listOf(

                                        Color.White.copy(alpha = 0.65f),

                                        Color.Transparent

                                    )

                                ),

                                RoundedCornerShape(11.dp)

                            ),

                        contentAlignment = Alignment.Center

                    ) {

                        Icon(

                            imageVector = icon,

                            contentDescription = null,

                            tint = Color.White,

                            modifier = Modifier.size(20.dp)

                        )

                    }

                    Text(

                        text = title,

                        style = MaterialTheme.typography.titleMedium,

                        fontWeight = FontWeight.Bold,

                        color = MaterialTheme.colorScheme.onSurface,

                        fontSize = 15.sp

                    )

                }

                // Bouton chevron circulaire iOS 27

                Surface(

                    shape = CircleShape,

                    color = rubricAccent.copy(alpha = if (isLight) 0.12f else 0.22f),

                    border = BorderStroke(1.dp, rubricAccent.copy(alpha = if (isLight) 0.35f else 0.45f)),

                    modifier = Modifier.size(30.dp)

                ) {

                    Box(contentAlignment = Alignment.Center) {

                        Icon(

                            imageVector = Icons.Default.ExpandMore,

                            contentDescription = if (expanded) "Réduire" else "Déployer",

                            tint = rubricAccent,

                            modifier = Modifier

                                .size(18.dp)

                                .rotate(rotationAngle)

                        )

                    }

                }

            }

            if (expanded) {

                HorizontalDivider(color = rubricAccent.copy(alpha = if (isLight) 0.25f else 0.35f))

                content()

            }

        }

    }

}

@Composable

fun LanguageChip(

    label: String,

    selected: Boolean,

    onClick: () -> Unit

) {

    val isLight = MaterialTheme.colorScheme.background == com.example.ui.theme.OffWhite || MaterialTheme.colorScheme.background == Color(0xFFF8FAFC)

    val bgBrush = if (selected) {

        Brush.linearGradient(listOf(Color(0xFF007AFF), Color(0xFF38BDF8)))

    } else {

        Brush.linearGradient(

            listOf(

                if (isLight) Color(0xFFF1F5F9) else Color(0xFF27272A),

                if (isLight) Color(0xFFE2E8F0) else Color(0xFF18181B)

            )

        )

    }

    Surface(

        modifier = Modifier

            .shadow(if (selected) 2.dp else 0.dp, RoundedCornerShape(12.dp))

            .clip(RoundedCornerShape(12.dp))

            .clickable(onClick = onClick)

            .testTag("lang_chip_${label.lowercase()}"),

        shape = RoundedCornerShape(12.dp),

        border = androidx.compose.foundation.BorderStroke(

            0.8.dp,

            if (selected) Color(0xFF007AFF) else (if (isLight) Color(0xFFCBD5E1) else Color(0xFF3F3F46))

        ),

        color = Color.Transparent

    ) {

        Box(

            modifier = Modifier

                .background(bgBrush)

                .padding(horizontal = 16.dp, vertical = 8.dp),

            contentAlignment = Alignment.Center

        ) {

            Text(

                text = label,

                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,

                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,

                fontSize = 13.sp

            )

        }

    }

}

@Composable

fun SettingsChip(

    label: String,

    selected: Boolean,

    onClick: () -> Unit

) {

    val isLight = MaterialTheme.colorScheme.background == com.example.ui.theme.OffWhite || MaterialTheme.colorScheme.background == Color(0xFFF8FAFC)

    val bgBrush = if (selected) {

        Brush.linearGradient(listOf(Color(0xFF007AFF), Color(0xFF38BDF8)))

    } else {

        Brush.linearGradient(

            listOf(

                if (isLight) Color(0xFFF1F5F9) else Color(0xFF27272A),

                if (isLight) Color(0xFFE2E8F0) else Color(0xFF18181B)

            )

        )

    }

    Surface(

        modifier = Modifier

            .shadow(if (selected) 2.dp else 0.dp, RoundedCornerShape(12.dp))

            .clip(RoundedCornerShape(12.dp))

            .clickable(onClick = onClick)

            .testTag("settings_chip_${label.lowercase()}"),

        shape = RoundedCornerShape(12.dp),

        border = androidx.compose.foundation.BorderStroke(

            0.8.dp,

            if (selected) Color(0xFF007AFF) else (if (isLight) Color(0xFFCBD5E1) else Color(0xFF3F3F46))

        ),

        color = Color.Transparent

    ) {

        Box(

            modifier = Modifier

                .background(bgBrush)

                .padding(horizontal = 14.dp, vertical = 7.dp),

            contentAlignment = Alignment.Center

        ) {

            Text(

                text = label,

                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,

                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,

                fontSize = 12.sp

            )

        }

    }

}

