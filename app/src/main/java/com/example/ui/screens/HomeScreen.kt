package com.example.ui.screens

import androidx.compose.animation.core.*

import androidx.compose.foundation.Canvas

import androidx.compose.foundation.background

import androidx.compose.foundation.border

import androidx.compose.foundation.clickable

import androidx.compose.foundation.horizontalScroll

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight

import androidx.compose.material.icons.filled.*

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.platform.LocalConfiguration

import com.example.util.AdaptiveScreenContainer

import com.example.util.isLandscape

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.draw.shadow

import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.Path

import androidx.compose.ui.graphics.compositeOver

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.res.stringResource

import androidx.compose.ui.text.font.Font

import androidx.compose.ui.text.font.FontFamily

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import com.example.R

import com.example.data.SettingsEntity

import com.example.data.TimetableSlotEntity

import com.example.ui.AppViewModel

import com.example.ui.ClassBadge

import com.example.ui.ClassColorDot

import com.example.ui.ClassColorHelper

import com.example.ui.theme.OffWhite

import com.example.ui.theme.TajawalFontFamily

import com.example.ui.theme.CairoFontFamily

import com.example.ui.components.GlassmorphicCard

import com.example.ui.components.GlassmorphicBadge

import com.example.ui.components.AutumnTeacherProfileCard

import java.text.SimpleDateFormat

import java.util.*

import kotlinx.coroutines.delay

import kotlin.math.PI

import kotlin.math.cos

import kotlin.math.sin

private val PristinaFontFamily = FontFamily(

    Font(R.font.great_vibes, FontWeight.Normal)

)

private val ThuluthFontFamily = FontFamily(

    Font(R.font.decotype_thuluth, FontWeight.Normal)

)

private val NotoNastaliqUrduFontFamily = FontFamily(

    Font(R.font.noto_nastaliq_urdu, FontWeight.Normal)

)

private data class LessonDisplayInfo(

    val isCurrentLesson: Boolean,

    val className: String,

    val classroom: String,

    val subject: String,

    val timeSlot: String,

    val dayLabel: String = "Aujourd'hui",

    val isToday: Boolean = true,

    val startHourDisplay: String = "",

    val endHourDisplay: String = "",

    val targetDayOfWeek: Int = 1

)

private fun parseSlotMinutes(timeSlotStr: String): Pair<Int, Int>? {

    return try {

        val cleanSlot = timeSlotStr.replace("–", "-").replace("—", "-")

        val parts = cleanSlot.split("-").map { it.trim() }

        if (parts.isEmpty()) return null

        fun parsePart(p: String): Int? {

            val clean = p.replace("h", ":", ignoreCase = true).replace("H", ":").replace(" ", "")

            val colonParts = clean.split(":").map { it.trim() }

            val h = colonParts.firstOrNull()?.filter { it.isDigit() }?.toIntOrNull() ?: return null

            val m = colonParts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0

            return h * 60 + m

        }

        val startM = parsePart(parts[0]) ?: return null

        val endM = if (parts.size > 1) parsePart(parts[1]) ?: (startM + 60) else (startM + 60)

        Pair(startM, endM)

    } catch (e: Exception) {

        null

    }

}

@Composable

fun HomeScreen(

    viewModel: AppViewModel,

    onNavigate: (String) -> Unit

) {

    val settingsState by viewModel.settings.collectAsState()

    val classes by viewModel.allClasses.collectAsState()

    val teacherSlots by viewModel.teacherTimetableSlots.collectAsState()

    val currentSettings = settingsState ?: SettingsEntity()

    val isAr = currentSettings.activeLanguage == "ar"

    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()

    val isDark = currentSettings.activeTheme == "dark" || (currentSettings.activeTheme == "system" && isSystemDark) || MaterialTheme.colorScheme.background != com.example.ui.theme.OffWhite

    // Live timer that ticks regularly to re-evaluate current and next lessons

    var tickerTime by remember {

        mutableStateOf(System.currentTimeMillis())

    }

    LaunchedEffect(Unit) {

        while (true) {

            tickerTime = System.currentTimeMillis()

            delay(10000)

        }

    }

    // Determine current day of week (Sunday=1, Monday=2... in App timetable)

    val nowCal = Calendar.getInstance().apply { timeInMillis = tickerTime }

    val dayOfWeekAndroid = nowCal.get(Calendar.DAY_OF_WEEK)

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

    val isTeacherFreeDayToday = currentSettings.teacherFreeDay == dayOfWeekApp

    // Emploi du temps de l'enseignant

    val targetSlots = teacherSlots

    // Calcul du cours actuel ou du prochain cours selon l'emploi du temps du prof

    val lessonInfo = remember(targetSlots, classes, tickerTime, currentSettings, dayOfWeekApp, isAr) {

        if (targetSlots.isEmpty()) null

        else {

            val currentMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)

            val dayNames = if (isAr) {

                listOf("", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")

            } else {

                listOf("", "Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi")

            }

            // 1. Check if there is an ongoing lesson right now today

            val todaySlots = targetSlots.filter { it.dayOfWeek == dayOfWeekApp }

                .sortedBy { slot -> parseSlotMinutes(slot.timeSlot)?.first ?: 0 }

            var ongoingSlot: TimetableSlotEntity? = null

            var isLast15MinutesOfOngoing = false

            for (slot in todaySlots) {

                val (sMin, eMin) = parseSlotMinutes(slot.timeSlot) ?: continue

                if (currentMinutes in sMin until eMin) {

                    ongoingSlot = slot

                    // Switch to displaying next course 15 minutes before the end of the current course

                    if (currentMinutes >= (eMin - 15)) {

                        isLast15MinutesOfOngoing = true

                    }

                    break

                }

            }

            // If currently in a lesson and NOT in the last 15 minutes -> display "Cours actuel :"

            if (ongoingSlot != null && !isLast15MinutesOfOngoing) {

                val cName = if (ongoingSlot.classId != null) {

                    classes.find { it.id == ongoingSlot.classId }?.name ?: ongoingSlot.subject.ifBlank { if (isAr) "الفوج" else "Classe" }

                } else {

                    ongoingSlot.subject.ifBlank { if (isAr) "الحصة" else "Cours" }

                }

                val room = ongoingSlot.classroom.trim()

                val (sMin, eMin) = parseSlotMinutes(ongoingSlot.timeSlot) ?: Pair(0, 0)

                val sHour = String.format(Locale.getDefault(), "%02d:%02d", sMin / 60, sMin % 60)

                val eHour = String.format(Locale.getDefault(), "%02d:%02d", eMin / 60, eMin % 60)

                LessonDisplayInfo(

                    isCurrentLesson = true,

                    className = cName,

                    classroom = room,

                    subject = ongoingSlot.subject,

                    timeSlot = ongoingSlot.timeSlot,

                    dayLabel = if (isAr) "اليوم" else "Aujourd'hui",

                    isToday = true,

                    startHourDisplay = sHour,

                    endHourDisplay = eHour,

                    targetDayOfWeek = ongoingSlot.dayOfWeek

                )

            } else {

                // Find NEXT lesson:

                var nextSlotFound: TimetableSlotEntity? = null

                var nextSlotDayLabel = ""

                var nextSlotIsToday = false

                for (offset in 0..6) {

                    val targetDay = ((dayOfWeekApp - 1 + offset) % 7) + 1

                    val slotsForDay = targetSlots.filter { it.dayOfWeek == targetDay }

                        .sortedBy { slot -> parseSlotMinutes(slot.timeSlot)?.first ?: 0 }

                    if (slotsForDay.isEmpty()) continue

                    if (offset == 0) {

                        val upcoming = slotsForDay.firstOrNull { slot ->

                            val (sMin, _) = parseSlotMinutes(slot.timeSlot) ?: return@firstOrNull false

                            if (ongoingSlot != null && isLast15MinutesOfOngoing) {

                                val ongoingEnd = parseSlotMinutes(ongoingSlot.timeSlot)?.second ?: currentMinutes

                                (sMin >= ongoingEnd || sMin > currentMinutes) && slot.id != ongoingSlot.id

                            } else {

                                sMin > currentMinutes

                            }

                        }

                        if (upcoming != null) {

                            nextSlotFound = upcoming

                            nextSlotDayLabel = if (isAr) "اليوم" else "Aujourd'hui"

                            nextSlotIsToday = true

                            break

                        }

                    } else {

                        val firstSlot = slotsForDay.firstOrNull()

                        if (firstSlot != null) {

                            nextSlotFound = firstSlot

                            nextSlotDayLabel = if (offset == 1) (if (isAr) "غداً" else "Demain") else (dayNames.getOrNull(targetDay) ?: "")

                            nextSlotIsToday = false

                            break

                        }

                    }

                }

                if (nextSlotFound != null) {

                    val cName = if (nextSlotFound.classId != null) {

                        classes.find { it.id == nextSlotFound.classId }?.name ?: nextSlotFound.subject.ifBlank { if (isAr) "الفوج" else "Classe" }

                    } else {

                        nextSlotFound.subject.ifBlank { if (isAr) "الحصة" else "Cours" }

                    }

                    val room = nextSlotFound.classroom.trim()

                    val (sMin, eMin) = parseSlotMinutes(nextSlotFound.timeSlot) ?: Pair(0, 0)

                    val sHour = String.format(Locale.getDefault(), "%02d:%02d", sMin / 60, sMin % 60)

                    val eHour = String.format(Locale.getDefault(), "%02d:%02d", eMin / 60, eMin % 60)

                    LessonDisplayInfo(

                        isCurrentLesson = false,

                        className = cName,

                        classroom = room,

                        subject = nextSlotFound.subject,

                        timeSlot = nextSlotFound.timeSlot,

                        dayLabel = nextSlotDayLabel,

                        isToday = nextSlotIsToday,

                        startHourDisplay = sHour,

                        endHourDisplay = eHour,

                        targetDayOfWeek = nextSlotFound.dayOfWeek

                    )

                } else if (ongoingSlot != null) {

                    val cName = if (ongoingSlot.classId != null) {

                        classes.find { it.id == ongoingSlot.classId }?.name ?: ongoingSlot.subject.ifBlank { if (isAr) "الفوج" else "Classe" }

                    } else {

                        ongoingSlot.subject.ifBlank { if (isAr) "الحصة" else "Cours" }

                    }

                    val room = ongoingSlot.classroom.trim()

                    val (sMin, eMin) = parseSlotMinutes(ongoingSlot.timeSlot) ?: Pair(0, 0)

                    val sHour = String.format(Locale.getDefault(), "%02d:%02d", sMin / 60, sMin % 60)

                    val eHour = String.format(Locale.getDefault(), "%02d:%02d", eMin / 60, eMin % 60)

                    LessonDisplayInfo(

                        isCurrentLesson = true,

                        className = cName,

                        classroom = room,

                        subject = ongoingSlot.subject,

                        timeSlot = ongoingSlot.timeSlot,

                        dayLabel = if (isAr) "اليوم" else "Aujourd'hui",

                        isToday = true,

                        startHourDisplay = sHour,

                        endHourDisplay = eHour,

                        targetDayOfWeek = ongoingSlot.dayOfWeek

                    )

                } else {

                    null

                }

            }

        }

    }

    val screenHeightDp = LocalConfiguration.current.screenHeightDp

    val isLandscapeMode = isLandscape()

    val needsScroll = isLandscapeMode || screenHeightDp < 520

    AdaptiveScreenContainer(maxWidth = 860.dp) {

        Column(

            modifier = Modifier

                .fillMaxSize()

                .then(if (needsScroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)

                .padding(horizontal = 10.dp, vertical = if (needsScroll) 8.dp else 4.dp),

            verticalArrangement = if (needsScroll) Arrangement.spacedBy(10.dp) else Arrangement.SpaceEvenly

        ) {

        // =========================================================

        // 1. HERO CARD : PROFIL ENSEIGNANT (DESIGN iOS 27 SPATIAL GLASS)

        // =========================================================

        AutumnTeacherProfileCard(

            currentSettings = currentSettings,

            isDark = isDark,

            isAr = isAr,

            teacherFontFamily = if (isAr) NotoNastaliqUrduFontFamily else PristinaFontFamily

        )

        // =========================================================

        // 2. ENCADRÉ : PROCHAIN COURS / COURS ACTUEL / RÉCEPTION (Emploi du temps)

        // Habillé d'un design iOS 27 Liquid Glass avec vagues de couleurs animées

        // =========================================================

        val isCurrent = lessonInfo?.isCurrentLesson == true

        val isParentReception = lessonInfo != null && (

            lessonInfo.subject.contains("réception", ignoreCase = true) ||

            lessonInfo.subject.contains("reception", ignoreCase = true) ||

            lessonInfo.className.contains("réception", ignoreCase = true) ||

            lessonInfo.className.contains("reception", ignoreCase = true)

        )

        val lessonPalette = if (lessonInfo != null) {

            ClassColorHelper.getPaletteForClass(0, lessonInfo.className)

        } else null

        // Palette vibrante de vagues chromatiques iOS 27 selon l'état

        val (wavePrimary, waveSecondary, waveAccent) = when {

            isCurrent && isParentReception -> Triple(

                Color(0xFFF59E0B), // Ambre radieux

                Color(0xFFEA580C), // Coucher de soleil orange

                Color(0xFFE11D48)  // Framboise / Rose

            )

            isCurrent -> Triple(

                Color(0xFF10B981), // Émeraude électrique

                Color(0xFF06B6D4), // Cyan brillant

                Color(0xFF3B82F6)  // Bleu cobalt

            )

            isParentReception -> Triple(

                Color(0xFFF59E0B), // Ambre

                Color(0xFFD97706), // Ocre chaud

                Color(0xFFFBBF24)  // Doré

            )

            isTeacherFreeDayToday -> Triple(

                Color(0xFF6366F1), // Indigo céleste

                Color(0xFF8B5CF6), // Violet fluide

                Color(0xFF06B6D4)  // Cyan

            )

            lessonPalette != null -> Triple(

                lessonPalette.primary,

                Color(0xFF8B5CF6),

                Color(0xFF06B6D4)

            )

            else -> Triple(

                Color(0xFF3B82F6), // Bleu royal iOS

                Color(0xFF8B5CF6), // Violet électrique

                Color(0xFFEC4899)  // Fuchsia dynamique

            )

        }

        val targetDayForTimetable = lessonInfo?.targetDayOfWeek ?: dayOfWeekApp

        val headerTitle = when {

            isCurrent && isParentReception -> if (isAr) "استقبال أولياء التلاميذ" else "RÉCEPTION EN COURS"

            isCurrent -> if (isAr) "الدرس الحالي" else "COURS ACTUEL"

            isTeacherFreeDayToday -> if (isAr) "يوم راحة" else "JOURNÉE LIBRE"

            else -> if (isAr) "الحصة المبرمجة" else "À VENIR"

        }

        val headerIcon = when {

            isCurrent && isParentReception -> Icons.Default.Diversity3

            isCurrent -> Icons.Default.PlayCircle

            isTeacherFreeDayToday -> Icons.Default.CheckCircle

            isParentReception -> Icons.Default.Diversity3

            else -> Icons.Default.Schedule

        }

        // Animation de pulsation du point "Live / En direct" ultra-fluide et organique

        val infiniteStatusTransition = rememberInfiniteTransition(label = "ios27_status_pulse")

        val statusDotAlpha by infiniteStatusTransition.animateFloat(

            initialValue = 0.70f,

            targetValue = 1.0f,

            animationSpec = infiniteRepeatable(

                animation = tween(2200, easing = FastOutSlowInEasing),

                repeatMode = RepeatMode.Reverse

            ),

            label = "status_dot_alpha"

        )

        val statusDotScale by infiniteStatusTransition.animateFloat(

            initialValue = 0.90f,

            targetValue = 1.15f,

            animationSpec = infiniteRepeatable(

                animation = tween(2200, easing = FastOutSlowInEasing),

                repeatMode = RepeatMode.Reverse

            ),

            label = "status_dot_scale"

        )

        val statusHaloScale by infiniteStatusTransition.animateFloat(

            initialValue = 1.0f,

            targetValue = 2.2f,

            animationSpec = infiniteRepeatable(

                animation = tween(2200, easing = LinearOutSlowInEasing),

                repeatMode = RepeatMode.Restart

            ),

            label = "status_halo_scale"

        )

        val statusHaloAlpha by infiniteStatusTransition.animateFloat(

            initialValue = 0.50f,

            targetValue = 0.0f,

            animationSpec = infiniteRepeatable(

                animation = tween(2200, easing = FastOutLinearInEasing),

                repeatMode = RepeatMode.Restart

            ),

            label = "status_halo_alpha"

        )

        Surface(

            modifier = Modifier

                .fillMaxWidth()

                .shadow(

                    elevation = if (isDark) 4.dp else 3.dp,

                    shape = RoundedCornerShape(20.dp),

                    ambientColor = wavePrimary.copy(alpha = 0.20f),

                    spotColor = waveSecondary.copy(alpha = 0.30f)

                )

                .border(

                    width = 1.2.dp,

                    brush = Brush.linearGradient(

                        listOf(

                            wavePrimary.copy(alpha = if (isDark) 0.65f else 0.50f),

                            waveSecondary.copy(alpha = if (isDark) 0.45f else 0.35f),

                            (if (isDark) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.70f))

                        )

                    ),

                    shape = RoundedCornerShape(20.dp)

                )

                .clip(RoundedCornerShape(20.dp))

                .clickable {

                    viewModel.selectTimetableDay(targetDayForTimetable)

                    onNavigate("timetables")

                },

            shape = RoundedCornerShape(20.dp),

            color = Color.Transparent

        ) {

            Box(

                modifier = Modifier

                    .fillMaxWidth()

                    .clip(RoundedCornerShape(20.dp))

            ) {

                // Fond animé de vagues de couleurs chromatiques iOS 27

                ChromaticWavesCanvas(

                    isDark = isDark,

                    primaryWaveColor = wavePrimary,

                    secondaryWaveColor = waveSecondary,

                    accentWaveColor = waveAccent,

                    modifier = Modifier.matchParentSize()

                )

                // Contenu premier plan de la carte

                Column(

                    modifier = Modifier

                        .fillMaxWidth()

                        .padding(horizontal = 12.dp, vertical = if (isAr) 6.dp else 9.dp),

                    verticalArrangement = Arrangement.spacedBy(if (isAr) 4.dp else 6.dp)

                ) {

                    // Ligne supérieure : En-tête Statut & Capsule Live

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement = Arrangement.SpaceBetween,

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.spacedBy(7.dp)

                        ) {

                            Box(

                                modifier = Modifier

                                    .size(if (isAr) 22.dp else 26.dp)

                                    .shadow(2.dp, RoundedCornerShape(8.dp))

                                    .clip(RoundedCornerShape(8.dp))

                                    .background(

                                        Brush.linearGradient(

                                            listOf(

                                                wavePrimary,

                                                waveSecondary

                                            )

                                        )

                                    ),

                                contentAlignment = Alignment.Center

                            ) {

                                Icon(

                                    imageVector = headerIcon,

                                    contentDescription = null,

                                    tint = Color.White,

                                    modifier = Modifier.size(if (isAr) 13.dp else 15.dp)

                                )

                            }

                            Text(

                                text = headerTitle,

                                fontSize = if (isAr) 13.sp else 12.5.sp,

                                fontWeight = FontWeight.Black,

                                color = if (isDark) Color.White else Color(0xFF0F172A),

                                letterSpacing = 0.4.sp

                            )

                        }

                        // Capsule d'état avec halo d'onde respiratoire réaliste

                        val dotColor = if (isCurrent) Color(0xFF10B981) else wavePrimary

                        Box(

                            contentAlignment = Alignment.Center,

                            modifier = Modifier.size(if (isAr) 22.dp else 24.dp)

                        ) {

                            // Halo fluide en expansion

                            Box(

                                modifier = Modifier

                                    .size(if (isAr) 9.dp else 10.dp)

                                    .graphicsLayer(

                                        scaleX = statusHaloScale,

                                        scaleY = statusHaloScale,

                                        alpha = statusHaloAlpha

                                    )

                                    .clip(CircleShape)

                                    .background(dotColor)

                            )

                            // Capsule vitrée protectrice

                            Surface(

                                shape = CircleShape,

                                color = dotColor.copy(alpha = if (isDark) 0.22f else 0.14f),

                                border = androidx.compose.foundation.BorderStroke(

                                    1.dp,

                                    dotColor.copy(alpha = if (isDark) 0.65f else 0.50f)

                                )

                            ) {

                                Box(

                                    modifier = Modifier.padding(if (isAr) 4.5.dp else 5.dp),

                                    contentAlignment = Alignment.Center

                                ) {

                                    // Point central lumineux avec reflet radial

                                    Box(

                                        modifier = Modifier

                                            .size(if (isAr) 6.5.dp else 7.5.dp)

                                            .graphicsLayer(

                                                scaleX = statusDotScale,

                                                scaleY = statusDotScale,

                                                alpha = statusDotAlpha

                                            )

                                            .clip(CircleShape)

                                            .background(

                                                Brush.radialGradient(

                                                    listOf(

                                                        Color.White,

                                                        dotColor

                                                    )

                                                )

                                            )

                                    )

                                }

                            }

                        }

                    }

                    // Contenu de la séance

                    if (lessonInfo != null) {

                        val startHour = if (lessonInfo.startHourDisplay.isNotBlank()) lessonInfo.startHourDisplay else (lessonInfo.timeSlot.split("-").firstOrNull()?.trim() ?: lessonInfo.timeSlot)

                        val dayPrefix = if (lessonInfo.isToday) (if (isAr) "اليوم" else "Aujourd'hui") else lessonInfo.dayLabel

                        val timeDisplay = "$dayPrefix $startHour"

                        val cleanRoom = if (lessonInfo.classroom.isNotBlank()) {

                            lessonInfo.classroom.replace(Regex("(?i)^salle\\s*"), "").trim()

                        } else ""

                        val displayRoom = if (cleanRoom.isNotBlank()) {

                            if (cleanRoom.startsWith("S", ignoreCase = true) && cleanRoom.length > 1 && (cleanRoom[1] == ' ' || cleanRoom[1] == '.')) {

                                cleanRoom

                            } else {

                                if (isAr) "قاعة $cleanRoom" else "Salle $cleanRoom"

                            }

                        } else ""

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.spacedBy(if (isAr) 4.dp else 6.dp),

                            modifier = Modifier.fillMaxWidth()

                        ) {

                            // Badge Heure (Frosted glass capsule)

                            Surface(

                                shape = RoundedCornerShape(8.dp),

                                color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.90f),

                                border = androidx.compose.foundation.BorderStroke(

                                    0.9.dp,

                                    if (isDark) Color(0xFF475569).copy(alpha = 0.5f) else Color(0xFFCBD5E1)

                                ),

                                modifier = Modifier.shadow(1.dp, RoundedCornerShape(8.dp))

                            ) {

                                Row(

                                    verticalAlignment = Alignment.CenterVertically,

                                    horizontalArrangement = Arrangement.spacedBy(4.dp),

                                    modifier = Modifier.padding(horizontal = if (isAr) 5.dp else 7.dp, vertical = if (isAr) 2.dp else 3.dp)

                                ) {

                                    Icon(

                                        imageVector = Icons.Default.AccessTime,

                                        contentDescription = null,

                                        tint = wavePrimary,

                                        modifier = Modifier.size(if (isAr) 12.dp else 13.dp)

                                    )

                                    Text(

                                        text = timeDisplay,

                                        fontWeight = FontWeight.ExtraBold,

                                        fontSize = if (isAr) 12.sp else 11.5.sp,

                                        color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)

                                    )

                                }

                            }

                            // Badge Classe ou Réception des parents

                            if (isParentReception) {

                                Surface(

                                    shape = RoundedCornerShape(8.dp),

                                    color = if (isDark) Color(0xFF78350F).copy(alpha = 0.35f) else Color(0xFFFEF3C7),

                                    border = androidx.compose.foundation.BorderStroke(

                                        0.9.dp,

                                        Color(0xFFF59E0B).copy(alpha = 0.75f)

                                    ),

                                    modifier = Modifier.shadow(1.dp, RoundedCornerShape(8.dp))

                                ) {

                                    Row(

                                        verticalAlignment = Alignment.CenterVertically,

                                        horizontalArrangement = Arrangement.spacedBy(4.dp),

                                        modifier = Modifier.padding(horizontal = if (isAr) 5.dp else 7.dp, vertical = if (isAr) 2.dp else 3.dp)

                                    ) {

                                        Icon(

                                            imageVector = Icons.Default.Diversity3,

                                            contentDescription = null,

                                            tint = Color(0xFFD97706),

                                            modifier = Modifier.size(if (isAr) 12.dp else 13.dp)

                                        )

                                        Text(

                                            text = if (isAr) "استقبال الأولياء" else "Réception parents",

                                            fontWeight = FontWeight.ExtraBold,

                                            fontSize = if (isAr) 12.sp else 11.sp,

                                            color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)

                                        )

                                    }

                                }

                            } else {

                                val classPal = ClassColorHelper.getPaletteForClass(0, lessonInfo.className.trim())

                                Surface(

                                    shape = RoundedCornerShape(8.dp),

                                    color = if (isDark) classPal.primary.copy(alpha = 0.22f) else classPal.container.copy(alpha = 0.92f),

                                    border = androidx.compose.foundation.BorderStroke(

                                        0.9.dp,

                                        classPal.primary.copy(alpha = 0.75f)

                                    ),

                                    modifier = Modifier.shadow(1.dp, RoundedCornerShape(8.dp))

                                ) {

                                    Row(

                                        verticalAlignment = Alignment.CenterVertically,

                                        horizontalArrangement = Arrangement.spacedBy(4.dp),

                                        modifier = Modifier.padding(horizontal = if (isAr) 5.dp else 7.dp, vertical = if (isAr) 2.dp else 3.dp)

                                    ) {

                                        Box(

                                            modifier = Modifier

                                                .size(if (isAr) 6.dp else 7.dp)

                                                .clip(CircleShape)

                                                .background(classPal.primary)

                                        )

                                        Text(

                                            text = lessonInfo.className.trim(),

                                            fontSize = if (isAr) 12.5.sp else 11.5.sp,

                                            fontWeight = FontWeight.Black,

                                            color = if (isDark) Color.White else classPal.onContainer

                                        )

                                    }

                                }

                            }

                            // Badge Salle spécifique

                            if (displayRoom.isNotBlank() && !isParentReception) {

                                Surface(

                                    shape = RoundedCornerShape(8.dp),

                                    color = if (isDark) Color(0xFF0F766E).copy(alpha = 0.25f) else Color(0xFFCCFBF1).copy(alpha = 0.92f),

                                    border = androidx.compose.foundation.BorderStroke(

                                        0.9.dp,

                                        Color(0xFF14B8A6).copy(alpha = 0.80f)

                                    ),

                                    modifier = Modifier.shadow(1.dp, RoundedCornerShape(8.dp))

                                ) {

                                    Row(

                                        verticalAlignment = Alignment.CenterVertically,

                                        horizontalArrangement = Arrangement.spacedBy(4.dp),

                                        modifier = Modifier.padding(horizontal = if (isAr) 5.dp else 7.dp, vertical = if (isAr) 2.dp else 3.dp)

                                    ) {

                                        Icon(

                                            imageVector = Icons.Default.MeetingRoom,

                                            contentDescription = null,

                                            tint = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0D9488),

                                            modifier = Modifier.size(if (isAr) 12.dp else 13.dp)

                                        )

                                        Text(

                                            text = displayRoom,

                                            fontWeight = FontWeight.Black,

                                            fontSize = if (isAr) 12.sp else 11.5.sp,

                                            color = if (isDark) Color(0xFF99F6E4) else Color(0xFF0F766E)

                                        )

                                    }

                                }

                            }

                        }

                    } else if (isTeacherFreeDayToday) {

                        Surface(

                            shape = RoundedCornerShape(10.dp),

                            color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.75f),

                            border = androidx.compose.foundation.BorderStroke(0.8.dp, wavePrimary.copy(alpha = 0.35f)),

                            modifier = Modifier.fillMaxWidth()

                        ) {

                            Row(

                                verticalAlignment = Alignment.CenterVertically,

                                horizontalArrangement = Arrangement.spacedBy(8.dp),

                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)

                            ) {

                                Icon(

                                    imageVector = Icons.Default.CheckCircle,

                                    contentDescription = null,

                                    tint = wavePrimary,

                                    modifier = Modifier.size(16.dp)

                                )

                                Text(

                                    text = if (isAr) "يوم راحة — لا توجد أي حصص مبرمجة اليوم." else "Journée libre (Repos) — Aucun cours programmé aujourd'hui.",

                                    fontSize = 11.5.sp,

                                    fontWeight = FontWeight.SemiBold,

                                    color = if (isDark) Color(0xFFE2E8F0) else MaterialTheme.colorScheme.onSurface

                                )

                            }

                        }

                    } else {

                        Surface(

                            shape = RoundedCornerShape(10.dp),

                            color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.75f),

                            border = androidx.compose.foundation.BorderStroke(0.8.dp, wavePrimary.copy(alpha = 0.35f)),

                            modifier = Modifier.fillMaxWidth()

                        ) {

                            Row(

                                verticalAlignment = Alignment.CenterVertically,

                                horizontalArrangement = Arrangement.spacedBy(8.dp),

                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)

                            ) {

                                Icon(

                                    imageVector = Icons.Default.Schedule,

                                    contentDescription = null,

                                    tint = wavePrimary,

                                    modifier = Modifier.size(16.dp)

                                )

                                Text(

                                    text = if (isAr) "لا توجد أي حصص مبرمجة في جدول التوقيت." else "Aucun cours programmé dans l'emploi du temps.",

                                    fontSize = 11.5.sp,

                                    fontWeight = FontWeight.SemiBold,

                                    color = if (isDark) Color(0xFFE2E8F0) else MaterialTheme.colorScheme.onSurface

                                )

                            }

                        }

                    }

                }

            }

        }

        // =========================================================

        // 3. OUTILS, DOCUMENTS & ACTIVITÉS (GRILLE ARTISTIQUE 2x2)

        // =========================================================

        Column(

            modifier = Modifier.fillMaxWidth(),

            verticalArrangement = Arrangement.spacedBy(if (isAr) 4.dp else 8.dp)

        ) {

            Row(

                verticalAlignment = Alignment.CenterVertically,

                horizontalArrangement = Arrangement.spacedBy(6.dp),

                modifier = Modifier.padding(start = 4.dp, top = if (isAr) 0.dp else 2.dp)

            ) {

                Box(

                    modifier = Modifier

                        .size(if (isAr) 5.dp else 6.dp)

                        .clip(CircleShape)

                        .background(MaterialTheme.colorScheme.primary)

                )

                Text(

                    text = if (isAr) "أدوات و وثائق بيداغوجية" else "OUTILS & ESPACES PÉDAGOGIQUES",

                    fontWeight = FontWeight.Black,

                    letterSpacing = 0.5.sp,

                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),

                    fontSize = if (isAr) 12.sp else 11.5.sp

                )

            }

            // Ligne 1 : Documents & Carnet d'absences

            Row(

                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement = Arrangement.spacedBy(if (isAr) 6.dp else 9.dp)

            ) {

                ArtisticToolGridCard(

                    title = if (isAr) "وثائقي" else "Mes documents",

                    subtitle = if (isAr) "نصوص تنظيمية ومطبوعات" else "Textes & fiches officielles",

                    badgeText = if (isAr) "وثائق" else "DOCS",

                    icon = Icons.Default.Folder,

                    accentColor = Color(0xFF2563EB),

                    gradientColors = listOf(Color(0xFF2563EB), Color(0xFF3B82F6)),

                    cardBgColor = Color(0xFFF0F7FF),

                    isAr = isAr,

                    modifier = Modifier.weight(1f),

                    onClick = { onNavigate("documents") }

                )

                ArtisticToolGridCard(

                    title = if (isAr) "دفتر الغيابات" else "Carnet d'absences",

                    subtitle = if (isAr) "متابعة الحضور والمواظبة" else "Appel & assiduité élèves",

                    badgeText = if (isAr) "مواظبة" else "APPEL",

                    icon = Icons.Default.HowToReg,

                    accentColor = Color(0xFF059669),

                    gradientColors = listOf(Color(0xFF059669), Color(0xFF10B981)),

                    cardBgColor = Color(0xFFF0FDF4),

                    isAr = isAr,

                    modifier = Modifier.weight(1f),

                    onClick = { onNavigate("attendances") }

                )

            }

            // Ligne 2 : Prises de notes & Tâches et rappels

            Row(

                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement = Arrangement.spacedBy(if (isAr) 6.dp else 9.dp)

            ) {

                ArtisticToolGridCard(

                    title = if (isAr) "الملاحظات" else "Notes & Synthèses",

                    subtitle = if (isAr) "أفكار وملخصات الدروس" else "Idées & comptes-rendus",

                    badgeText = if (isAr) "مذكرات" else "NOTES",

                    icon = Icons.Default.NoteAlt,

                    accentColor = Color(0xFFD97706),

                    gradientColors = listOf(Color(0xFFD97706), Color(0xFFF59E0B)),

                    cardBgColor = Color(0xFFFFFBEB),

                    isAr = isAr,

                    modifier = Modifier.weight(1f),

                    onClick = { onNavigate("notes") }

                )

                ArtisticToolGridCard(

                    title = if (isAr) "التذكيرات" else "Tâches & Alertes",

                    subtitle = if (isAr) "مواعيد وتنبيهات مبرمجة" else "Dates clés & rappels",

                    badgeText = if (isAr) "تنبيه" else "ALERTES",

                    icon = Icons.Default.Notifications,

                    accentColor = Color(0xFF7C3AED),

                    gradientColors = listOf(Color(0xFF7C3AED), Color(0xFF8B5CF6)),

                    cardBgColor = Color(0xFFFAF5FF),

                    isAr = isAr,

                    modifier = Modifier.weight(1f),

                    onClick = { onNavigate("reminders") }

                )

            }

        }

    }

    }

}

/**

 * Encadré d'outil et de document au design iOS 27 3D Spatial Glass :

 * - Icône 3D avec biseau volumétrique et reflet miroitant

 * - Capsule de badge en verre dépoli tactile

 * - Bordure hairline spéculaire et ombre portée teintée multi-couches

 */

@Composable

private fun ArtisticToolGridCard(

    title: String,

    subtitle: String,

    badgeText: String,

    icon: androidx.compose.ui.graphics.vector.ImageVector,

    accentColor: Color,

    gradientColors: List<Color>,

    cardBgColor: Color,

    isAr: Boolean = false,

    modifier: Modifier = Modifier,

    onClick: () -> Unit

) {

    val isLight = MaterialTheme.colorScheme.background == OffWhite || MaterialTheme.colorScheme.background == Color(0xFFF8FAFC)

    val effectiveBg = if (isLight) {

        accentColor.copy(alpha = 0.07f).compositeOver(Color.White)

    } else {

        accentColor.copy(alpha = 0.14f).compositeOver(Color(0xFF141722))

    }

    Surface(

        modifier = modifier

            .shadow(

                elevation = if (isAr) 2.dp else 4.dp,

                shape = RoundedCornerShape(if (isAr) 14.dp else 20.dp),

                ambientColor = accentColor.copy(alpha = if (isLight) 0.18f else 0.40f),

                spotColor = accentColor.copy(alpha = if (isLight) 0.28f else 0.55f)

            )

            .border(

                width = 1.2.dp,

                brush = Brush.linearGradient(

                    listOf(

                        (if (isLight) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.35f)),

                        accentColor.copy(alpha = if (isLight) 0.45f else 0.50f),

                        accentColor.copy(alpha = if (isLight) 0.15f else 0.20f)

                    )

                ),

                shape = RoundedCornerShape(if (isAr) 14.dp else 20.dp)

            )

            .clip(RoundedCornerShape(if (isAr) 14.dp else 20.dp))

            .clickable(onClick = onClick),

        shape = RoundedCornerShape(if (isAr) 14.dp else 20.dp),

        color = effectiveBg

    ) {

        Box(modifier = Modifier.fillMaxWidth()) {

            // Reflet spéculaire supérieur 3D iOS 27

            Canvas(modifier = Modifier.matchParentSize()) {

                val w = size.width

                drawLine(

                    brush = Brush.horizontalGradient(

                        listOf(

                            Color.Transparent,

                            (if (isLight) Color.White.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.30f)),

                            Color.Transparent

                        )

                    ),

                    start = Offset(w * 0.10f, 1.2f),

                    end = Offset(w * 0.90f, 1.2f),

                    strokeWidth = 1.2f

                )

            }

            Column(

                modifier = Modifier

                    .fillMaxWidth()

                    .padding(horizontal = if (isAr) 9.dp else 11.dp, vertical = if (isAr) 5.dp else 7.dp),

                verticalArrangement = Arrangement.spacedBy(if (isAr) 3.dp else 5.dp)

            ) {

                // Ligne du haut : Icône 3D avec biseau volumétrique + Badge catégorie

                Row(

                    modifier = Modifier.fillMaxWidth(),

                    verticalAlignment = Alignment.CenterVertically,

                    horizontalArrangement = Arrangement.SpaceBetween

                ) {

                    // Conteneur d'icône 3D avec reflet et relief

                    Box(

                        modifier = Modifier

                            .size(if (isAr) 26.dp else 30.dp)

                            .shadow(2.dp, RoundedCornerShape(if (isAr) 8.dp else 10.dp))

                            .clip(RoundedCornerShape(if (isAr) 8.dp else 10.dp))

                            .background(Brush.linearGradient(gradientColors))

                            .border(

                                1.dp,

                                Brush.linearGradient(

                                    listOf(

                                        Color.White.copy(alpha = 0.65f),

                                        Color.Transparent

                                    )

                                ),

                                RoundedCornerShape(if (isAr) 8.dp else 10.dp)

                            ),

                        contentAlignment = Alignment.Center

                    ) {

                        Icon(

                            imageVector = icon,

                            contentDescription = null,

                            tint = Color.White,

                            modifier = Modifier.size(if (isAr) 15.dp else 17.dp)

                        )

                    }

                    // Badge de catégorie tactile iOS 27

                    Surface(

                        shape = RoundedCornerShape(50),

                        color = accentColor.copy(alpha = if (isLight) 0.14f else 0.24f),

                        border = androidx.compose.foundation.BorderStroke(

                            0.9.dp,

                            accentColor.copy(alpha = if (isLight) 0.40f else 0.55f)

                        )

                    ) {

                        Text(

                            text = badgeText,

                            fontSize = if (isAr) 9.5.sp else 9.sp,

                            fontWeight = FontWeight.Black,

                            letterSpacing = 0.3.sp,

                            color = if (isLight) accentColor else Color.White,

                            modifier = Modifier.padding(horizontal = if (isAr) 5.dp else 6.dp, vertical = if (isAr) 1.5.dp else 2.dp)

                        )

                    }

                }

                // Textes : Titre net et sous-titre élégant

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {

                    Text(

                        text = title,

                        style = MaterialTheme.typography.titleSmall,

                        fontWeight = FontWeight.ExtraBold,

                        color = MaterialTheme.colorScheme.onSurface,

                        maxLines = 1,

                        overflow = TextOverflow.Ellipsis,

                        fontSize = if (isAr) 13.5.sp else 12.5.sp,

                        lineHeight = if (isAr) 16.sp else 16.sp

                    )

                    Text(

                        text = subtitle,

                        style = MaterialTheme.typography.bodySmall,

                        fontWeight = FontWeight.Medium,

                        color = MaterialTheme.colorScheme.onSurfaceVariant,

                        maxLines = 1,

                        overflow = TextOverflow.Ellipsis,

                        fontSize = if (isAr) 11.sp else 10.sp,

                        lineHeight = if (isAr) 13.5.sp else 13.sp

                    )

                }

            }

        }

    }

}

/**

 * Arrière-plan de vagues chromatiques animées en continu (iOS 27 Liquid Aurora Waves) :

 * - Ondes harmoniques douces et fluides inspirées de Siri et de l'interface spatiale iOS 27

 * - Dégradés vibrants et reflets spéculaires hairlines

 */

@Composable

private fun ChromaticWavesCanvas(

    modifier: Modifier = Modifier,

    isDark: Boolean,

    primaryWaveColor: Color,

    secondaryWaveColor: Color,

    accentWaveColor: Color

) {

    val infiniteTransition = rememberInfiniteTransition(label = "ios27_chromatic_waves")

    val phase by infiniteTransition.animateFloat(

        initialValue = 0f,

        targetValue = 2f * PI.toFloat(),

        animationSpec = infiniteRepeatable(

            animation = tween(durationMillis = 7500, easing = LinearEasing),

            repeatMode = RepeatMode.Restart

        ),

        label = "wave_phase"

    )

    Canvas(modifier = modifier) {

        val w = size.width

        val h = size.height

        // 1. Fond spatial translucide iOS 27

        val baseGradient = if (isDark) {

            Brush.verticalGradient(

                listOf(

                    Color(0xFF1C1C1E).copy(alpha = 0.95f),

                    Color(0xFF121214).copy(alpha = 0.97f)

                )

            )

        } else {

            Brush.verticalGradient(

                listOf(

                    Color(0xFFFFFFFF).copy(alpha = 0.92f),

                    Color(0xFFF8FAFC).copy(alpha = 0.95f)

                )

            )

        }

        drawRect(brush = baseGradient)

        // 2. Première onde fluide par courbes de Bézier (GPU accéléré, 60fps fluide)

        val p1 = Path().apply {

            moveTo(0f, h)

            val baseMid1 = h * 0.52f

            val waveAmp1 = h * 0.24f

            val y0 = baseMid1 + sin(phase) * waveAmp1 * 0.5f

            val y1 = baseMid1 + cos(phase * 0.8f) * waveAmp1 * 0.7f

            val y2 = baseMid1 - sin(phase * 1.1f) * waveAmp1 * 0.6f

            val y3 = baseMid1 + cos(phase * 0.9f + 1f) * waveAmp1 * 0.5f

            lineTo(0f, y0)

            cubicTo(

                w * 0.28f, y0 + sin(phase + 1f) * waveAmp1 * 0.5f,

                w * 0.42f, y1 - cos(phase * 0.7f) * waveAmp1 * 0.5f,

                w * 0.55f, y1

            )

            cubicTo(

                w * 0.72f, y1 + cos(phase + 2f) * waveAmp1 * 0.5f,

                w * 0.88f, y2 + sin(phase * 0.9f) * waveAmp1 * 0.5f,

                w, y3

            )

            lineTo(w, h)

            close()

        }

        val waveBrush1 = Brush.horizontalGradient(

            colors = listOf(

                primaryWaveColor.copy(alpha = if (isDark) 0.38f else 0.22f),

                secondaryWaveColor.copy(alpha = if (isDark) 0.32f else 0.18f),

                primaryWaveColor.copy(alpha = if (isDark) 0.25f else 0.14f)

            )

        )

        drawPath(path = p1, brush = waveBrush1)

        // 3. Deuxième onde fluide en opposition de phase (GPU accéléré)

        val p2 = Path().apply {

            moveTo(0f, h)

            val baseMid2 = h * 0.62f

            val waveAmp2 = h * 0.20f

            val y0 = baseMid2 + cos(phase * 0.85f + 1.2f) * waveAmp2 * 0.5f

            val y1 = baseMid2 - sin(phase * 0.95f) * waveAmp2 * 0.6f

            val y2 = baseMid2 + cos(phase * 0.75f + 2f) * waveAmp2 * 0.5f

            lineTo(0f, y0)

            cubicTo(

                w * 0.30f, y0 - sin(phase * 0.9f) * waveAmp2 * 0.6f,

                w * 0.45f, y1 + cos(phase * 1.1f) * waveAmp2 * 0.5f,

                w * 0.60f, y1

            )

            cubicTo(

                w * 0.75f, y1 - cos(phase * 0.8f) * waveAmp2 * 0.5f,

                w * 0.90f, y2 + sin(phase * 1.2f) * waveAmp2 * 0.6f,

                w, y2

            )

            lineTo(w, h)

            close()

        }

        val waveBrush2 = Brush.horizontalGradient(

            colors = listOf(

                secondaryWaveColor.copy(alpha = if (isDark) 0.30f else 0.18f),

                accentWaveColor.copy(alpha = if (isDark) 0.35f else 0.22f),

                secondaryWaveColor.copy(alpha = if (isDark) 0.22f else 0.12f)

            )

        )

        drawPath(path = p2, brush = waveBrush2)

        // 4. Troisième onde subtile pour effet liquide caustique (GPU accéléré)

        val p3 = Path().apply {

            moveTo(0f, h)

            val baseMid3 = h * 0.72f

            val waveAmp3 = h * 0.16f

            val y0 = baseMid3 + sin(phase * 0.75f + 2.4f) * waveAmp3 * 0.5f

            val y1 = baseMid3 - cos(phase * 0.85f) * waveAmp3 * 0.5f

            val y2 = baseMid3 + sin(phase * 0.65f + 1.5f) * waveAmp3 * 0.5f

            lineTo(0f, y0)

            cubicTo(

                w * 0.35f, y0 + cos(phase * 0.7f) * waveAmp3 * 0.5f,

                w * 0.65f, y1 - sin(phase * 0.8f) * waveAmp3 * 0.5f,

                w, y2

            )

            lineTo(w, h)

            close()

        }

        val waveBrush3 = Brush.horizontalGradient(

            colors = listOf(

                accentWaveColor.copy(alpha = if (isDark) 0.22f else 0.14f),

                primaryWaveColor.copy(alpha = if (isDark) 0.25f else 0.16f)

            )

        )

        drawPath(path = p3, brush = waveBrush3)

        // 5. Reflet spéculaire supérieur dynamique (Bordure luminescente de verre iOS 27)

        val shimmerOffset = sin(phase) * 0.15f

        drawLine(

            brush = Brush.horizontalGradient(

                listOf(

                    Color.Transparent,

                    (if (isDark) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.90f)),

                    Color.Transparent

                ),

                startX = w * (0.20f + shimmerOffset),

                endX = w * (0.80f + shimmerOffset)

            ),

            start = Offset(0f, 1f),

            end = Offset(w, 1f),

            strokeWidth = 1.5f

        )

    }

}

