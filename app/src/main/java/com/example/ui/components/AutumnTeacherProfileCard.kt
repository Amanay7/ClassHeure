package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingsEntity

/**
 * Sélectionne un symbole dynamique adapté à la matière d'enseignement saisie au lycée.
 */
fun getSubjectIconVector(subject: String): androidx.compose.ui.graphics.vector.ImageVector {
    val s = subject.lowercase().trim()
    return when {
        s.contains("math") || s.contains("رياض") || s.contains("calcul") || s.contains("alg") || s.contains("géom") -> Icons.Default.Calculate
        s.contains("phys") || s.contains("chim") || s.contains("فيزياء") || s.contains("كيمياء") || s.contains("sc. phys") -> Icons.Default.Science
        s.contains("svt") || s.contains("bio") || s.contains("طبيع") || s.contains("علوم الطبيعة") || s.contains("géol") -> Icons.Default.Biotech
        s.contains("franç") || s.contains("arab") || s.contains("angl") || s.contains("anglais") || s.contains("espagn") || s.contains("allemand") || s.contains("italien") || s.contains("lang") || s.contains("لغة") || s.contains("عربية") || s.contains("فرنسية") || s.contains("إنجليزية") || s.contains("أدب") || s.contains("philo") || s.contains("فلسفة") -> Icons.Default.MenuBook
        s.contains("hist") || s.contains("géo") || s.contains("تاريخ") || s.contains("جغرافيا") || s.contains("اجتماع") -> Icons.Default.HistoryEdu
        s.contains("info") || s.contains("informatique") || s.contains("tic") || s.contains("تكنولوج") || s.contains("إعلام") || s.contains("حاسوب") || s.contains("program") -> Icons.Default.Computer
        s.contains("art") || s.contains("dessin") || s.contains("رسم") || s.contains("تشكيل") || s.contains("فنون") -> Icons.Default.Palette
        s.contains("sport") || s.contains("eps") || s.contains("رياضة") || s.contains("بدني") -> Icons.Default.SportsBasketball
        s.contains("musi") || s.contains("موسيق") -> Icons.Default.MusicNote
        s.contains("islam") || s.contains("دين") || s.contains("إسلام") || s.contains("قرآن") || s.contains("شريع") -> Icons.Default.AutoStories
        else -> Icons.Default.AutoStories
    }
}

/**
 * Lignes colorées de séparation artistique style "fait à la main".
 */
@Composable
fun HandDrawnArtisticDivider(
    color: Color,
    modifier: Modifier = Modifier,
    variant: Int = 0
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .height(3.5.dp)
    ) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        val (curve1, curve2) = when (variant % 3) {
            0 -> Pair(1.2f, -1.4f)
            1 -> Pair(-1.3f, 1.2f)
            else -> Pair(1.1f, 1.0f)
        }

        val mainPath = Path().apply {
            moveTo(0f, midY)
            cubicTo(
                w * 0.28f, midY + curve1,
                w * 0.72f, midY + curve2,
                w, midY + (curve1 * 0.25f)
            )
        }

        val sketchPath = Path().apply {
            moveTo(w * 0.08f, midY + 0.8f)
            cubicTo(
                w * 0.38f, midY - 0.7f,
                w * 0.68f, midY + 0.9f,
                w * 0.94f, midY - 0.3f
            )
        }

        val brush = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                color.copy(alpha = 0.30f),
                color.copy(alpha = 0.85f),
                color.copy(alpha = 0.40f),
                Color.Transparent
            )
        )

        drawPath(
            path = mainPath,
            brush = brush,
            style = Stroke(
                width = 1.4.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        drawPath(
            path = sketchPath,
            brush = brush,
            style = Stroke(
                width = 0.75.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

/**
 * Encadré Profil de l'Enseignant de Lycée (Design Spatial iOS 27) :
 * - Animation poétique d'arbre d'automne et chute de feuilles (activable/désactivable).
 */
@Composable
fun AutumnTeacherProfileCard(
    currentSettings: SettingsEntity,
    isDark: Boolean,
    isAr: Boolean,
    teacherFontFamily: FontFamily,
    modifier: Modifier = Modifier
) {
    val teacherNameDisplay = remember(currentSettings.teacherName, isAr) {
        currentSettings.teacherName.ifBlank { if (isAr) "الأستاذ(ة)" else "Enseignant(e)" }
    }

    val isAnimated = currentSettings.profileAnimationEffect.ifBlank { "autumn_tree" } != "none"
    val infiniteTransition = rememberInfiniteTransition(label = "profile_anim")

    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "anim_progress"
    )

    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )

    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_progress"
    )

    val cardBackgroundBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0F172A),
                Color(0xFF1E1B4B),
                Color(0xFF261838),
                Color(0xFF1F2937),
                Color(0xFF18181B)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFF0F9FF),
                Color(0xFFFFF7ED),
                Color(0xFFFDF2F8),
                Color(0xFFFEF3C7),
                Color(0xFFF8FAFC)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    val cardBorderBrush = if (isDark) {
        Brush.sweepGradient(
            listOf(
                Color(0xFF38BDF8),
                Color(0xFF818CF8),
                Color(0xFFFBBF24),
                Color(0xFF60A5FA),
                Color(0xFF38BDF8)
            )
        )
    } else {
        Brush.sweepGradient(
            listOf(
                Color(0xFF2563EB),
                Color(0xFF38BDF8),
                Color(0xFFF59E0B),
                Color(0xFF60A5FA),
                Color(0xFF2563EB)
            )
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 6.dp else 4.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = if (isDark) Color(0xFF38BDF8).copy(alpha = 0.35f) else Color(0xFF2563EB).copy(alpha = 0.20f),
                spotColor = if (isDark) Color(0xFF818CF8).copy(alpha = 0.50f) else Color(0xFF60A5FA).copy(alpha = 0.30f)
            )
            .border(
                width = 1.4.dp,
                brush = cardBorderBrush,
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBackgroundBrush)
                .clip(RoundedCornerShape(18.dp))
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height

                if (isAnimated) {
                    drawAutumnTreeShadow(w, h, isDark, auraPulse)
                    drawFallingAutumnLeaves(w, h, isDark, animProgress)
                    val glintX = w * shimmerProgress
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                (if (isDark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.14f)),
                                Color.Transparent
                            ),
                            start = Offset(glintX - 50f, 0f),
                            end = Offset(glintX + 50f, h)
                        )
                    )
                } else {
                    drawAutumnTreeShadow(w, h, isDark, 1f, alphaScale = 0.8f)
                }

                // Reflet spéculaire supérieur iOS 27
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            if (isDark) Color.White.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.85f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(w * 0.08f, 1.2f),
                    end = Offset(w * 0.92f, 1.2f),
                    strokeWidth = 1.2f
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val nameTextColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F265C)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = teacherNameDisplay,
                        fontFamily = teacherFontFamily,
                        color = nameTextColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 32.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.4.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HandDrawnArtisticDivider(
                    color = if (isDark) Color(0xFF60A5FA).copy(alpha = 0.55f) else Color(0xFF2563EB).copy(alpha = 0.45f),
                    variant = 0
                )

                val subjectTextColor = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                val dynamicSubjectIcon = remember(currentSettings.taughtSubject) { getSubjectIconVector(currentSettings.taughtSubject) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = dynamicSubjectIcon,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    val subjectDisplay = if (currentSettings.taughtSubject.isNotBlank()) {
                        "${if (isAr) "المادة : " else "Matière : "}${currentSettings.taughtSubject}"
                    } else {
                        if (isAr) "المادة" else "Matière"
                    }
                    Text(
                        text = subjectDisplay,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = subjectTextColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                HandDrawnArtisticDivider(
                    color = if (isDark) Color(0xFFF59E0B).copy(alpha = 0.55f) else Color(0xFFD97706).copy(alpha = 0.45f),
                    variant = 1
                )

                val schoolTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = currentSettings.schoolName.ifBlank { if (isAr) "الثانوية" else "Lycée" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = schoolTextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        lineHeight = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                HandDrawnArtisticDivider(
                    color = if (isDark) Color(0xFF34D399).copy(alpha = 0.55f) else Color(0xFF059669).copy(alpha = 0.45f),
                    variant = 2
                )

                val yearTextColor = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "${if (isAr) "السنة الدراسية : " else "Année scolaire : "}${currentSettings.currentSchoolYear.ifBlank { if (isAr) "غير محددة" else "Non définie" }}",
                        color = yearTextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawAutumnTreeShadow(
    w: Float,
    h: Float,
    isDark: Boolean,
    pulse: Float,
    alphaScale: Float = 1.0f
) {
    val treeColor = if (isDark) {
        Color(0xFF0F263D).copy(alpha = 0.40f * alphaScale)
    } else {
        Color(0xFF78350F).copy(alpha = 0.12f * alphaScale)
    }

    val twigColor = if (isDark) {
        Color(0xFFB45309).copy(alpha = 0.22f * alphaScale)
    } else {
        Color(0xFFD97706).copy(alpha = 0.10f * alphaScale)
    }

    val sway = (pulse - 1f) * 6f

    val trunk = Path().apply {
        moveTo(w * 0.08f, h)
        cubicTo(
            w * 0.12f, h * 0.75f,
            w * 0.16f + sway * 0.5f, h * 0.55f,
            w * 0.20f + sway, h * 0.38f
        )
        lineTo(w * 0.23f + sway, h * 0.38f)
        cubicTo(
            w * 0.19f + sway * 0.5f, h * 0.58f,
            w * 0.16f, h * 0.78f,
            w * 0.15f, h
        )
        close()
    }
    drawPath(trunk, color = treeColor)

    val b1 = Path().apply {
        moveTo(w * 0.21f + sway, h * 0.38f)
        cubicTo(
            w * 0.34f + sway * 0.8f, h * 0.26f,
            w * 0.55f + sway * 1.2f, h * 0.15f,
            w * 0.84f + sway * 1.5f, h * 0.10f
        )
    }
    drawPath(b1, color = treeColor, style = Stroke(width = 3.2f, cap = StrokeCap.Round))

    val b1Sub1 = Path().apply {
        moveTo(w * 0.45f + sway, h * 0.21f)
        cubicTo(
            w * 0.54f + sway, h * 0.12f,
            w * 0.65f + sway, h * 0.08f,
            w * 0.74f + sway, h * 0.06f
        )
    }
    drawPath(b1Sub1, color = twigColor, style = Stroke(width = 2.0f, cap = StrokeCap.Round))

    val b1Sub2 = Path().apply {
        moveTo(w * 0.68f + sway * 1.3f, h * 0.13f)
        cubicTo(
            w * 0.76f + sway, h * 0.18f,
            w * 0.86f + sway, h * 0.20f,
            w * 0.94f + sway, h * 0.18f
        )
    }
    drawPath(b1Sub2, color = twigColor, style = Stroke(width = 1.8f, cap = StrokeCap.Round))

    val b2 = Path().apply {
        moveTo(w * 0.21f + sway, h * 0.42f)
        cubicTo(
            w * 0.35f + sway, h * 0.44f,
            w * 0.52f + sway * 1.1f, h * 0.38f,
            w * 0.72f + sway * 1.2f, h * 0.32f
        )
    }
    drawPath(b2, color = treeColor, style = Stroke(width = 2.6f, cap = StrokeCap.Round))

    val b3 = Path().apply {
        moveTo(w * 0.19f + sway, h * 0.45f)
        cubicTo(
            w * 0.14f + sway * 0.7f, h * 0.36f,
            w * 0.08f + sway * 0.5f, h * 0.26f,
            w * 0.02f + sway * 0.3f, h * 0.20f
        )
    }
    drawPath(b3, color = twigColor, style = Stroke(width = 2.2f, cap = StrokeCap.Round))

    val canopyClusters = listOf(
        Pair(Offset(w * 0.78f + sway, h * 0.09f), 24f),
        Pair(Offset(w * 0.62f + sway, h * 0.08f), 20f),
        Pair(Offset(w * 0.88f + sway, h * 0.14f), 22f),
        Pair(Offset(w * 0.48f + sway, h * 0.16f), 18f),
        Pair(Offset(w * 0.70f + sway, h * 0.30f), 20f),
        Pair(Offset(w * 0.06f + sway, h * 0.22f), 16f)
    )

    for ((center, radius) in canopyClusters) {
        drawCircle(
            color = twigColor.copy(alpha = (if (isDark) 0.18f else 0.09f) * alphaScale),
            radius = radius * (pulse * 0.2f + 0.8f),
            center = center
        )
    }
}

private fun DrawScope.drawFallingAutumnLeaves(
    w: Float,
    h: Float,
    isDark: Boolean,
    progress: Float
) {
    val leafParams = listOf(
        Triple(0.15f, 1.0f, 20f) to (Triple(2.5f, 0.10f, 14f) to (Color(0xFFF59E0B) to Color(0xFFB45309))),
        Triple(0.32f, 1.25f, 16f) to (Triple(3.0f, 0.45f, 12f) to (Color(0xFFD97706) to Color(0xFF92400E))),
        Triple(0.50f, 0.85f, 22f) to (Triple(2.0f, 0.75f, 15f) to (Color(0xFFEA580C) to Color(0xFF9A3412))),
        Triple(0.68f, 1.15f, 18f) to (Triple(2.8f, 0.25f, 13f) to (Color(0xFFFBBF24) to Color(0xFFD97706))),
        Triple(0.85f, 0.95f, 20f) to (Triple(2.2f, 0.85f, 14f) to (Color(0xFFE11D48) to Color(0xFF9F1239))),
        Triple(0.95f, 1.30f, 14f) to (Triple(3.2f, 0.55f, 11f) to (Color(0xFFCA8A04) to Color(0xFF854D0E))),
        Triple(0.40f, 0.75f, 24f) to (Triple(1.8f, 0.00f, 16f) to (Color(0xFFF97316) to Color(0xFFC2410C)))
    )

    for (leaf in leafParams) {
        val p1 = leaf.first
        val p2 = leaf.second.first
        val colors = leaf.second.second
        val leafColor = colors.first
        val veinColor = colors.second
        val baseX = p1.first
        val speed = p1.second
        val swayAmp = p1.third
        val swayFreq = p2.first
        val phase = p2.second
        val lSize = p2.third

        val p = (progress * speed + phase) % 1f
        val leafY = -25f + p * (h + 50f)
        val swayOffset = kotlin.math.sin((p * swayFreq + phase) * 2.0 * Math.PI).toFloat() * swayAmp
        val leafX = w * baseX + swayOffset
        val angle = (p * 360f * speed + phase * 180f + swayOffset * 1.5f)

        val leafPath = Path().apply {
            moveTo(0f, -lSize)
            cubicTo(lSize * 0.75f, -lSize * 0.45f, lSize * 0.75f, lSize * 0.45f, 0f, lSize)
            cubicTo(-lSize * 0.75f, lSize * 0.45f, -lSize * 0.75f, -lSize * 0.45f, 0f, -lSize)
            close()
        }

        rotate(degrees = angle, pivot = Offset(leafX, leafY)) {
            translate(left = leafX, top = leafY) {
                drawPath(
                    path = leafPath,
                    color = leafColor.copy(alpha = if (isDark) 0.38f else 0.30f)
                )
                drawLine(
                    color = veinColor.copy(alpha = if (isDark) 0.55f else 0.45f),
                    start = Offset(0f, -lSize * 0.80f),
                    end = Offset(0f, lSize * 0.90f),
                    strokeWidth = 1.1f
                )
            }
        }
    }
}
