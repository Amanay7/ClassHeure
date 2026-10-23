package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.R
import com.example.ui.components.HandDrawnArtisticDivider
import com.example.util.Tr

@Composable
fun AboutDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val currentLayoutDir = LocalLayoutDirection.current
    val isArabic = Tr.isArabic() || currentLayoutDir == LayoutDirection.Rtl
    val isDark = isSystemInDarkTheme()

    // Configuration plein écran (Match Parent) et effet de flou natif sur l'arrière-plan
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.parent as? DialogWindowProvider)?.window
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window?.attributes?.let { attr ->
                attr.blurBehindRadius = 50
                window.attributes = attr
            }
        }
        onDispose {}
    }

    // Palette harmonieuse de couleurs douces et zen (mode nuit apaisant et moins sombre)
    val autumnAmber = if (isDark) Color(0xFFE2B774) else Color(0xFFD97706)
    val autumnTerracotta = if (isDark) Color(0xFFE89467) else Color(0xFFC2410C)
    val autumnCopper = if (isDark) Color(0xFFE0C28A) else Color(0xFFB45309)
    val autumnGold = if (isDark) Color(0xFFF3D58C) else Color(0xFFEAB308)
    val autumnPeach = if (isDark) Color(0xFFE6A374) else Color(0xFFF97316)

    // Dégradé de fond plein écran : zen, doux et moins sombre en mode nuit
    val fullscreenBgBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF222836),
                Color(0xFF1D2230),
                Color(0xFF191E2A),
                Color(0xFF151924)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFFFFBEB),
                Color(0xFFFEF3C7),
                Color(0xFFFFEDD5),
                Color(0xFFFFF7ED)
            )
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(fullscreenBgBrush)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismissRequest() }
        ) {
            // Toile artistique avec halos lumineux et motifs de feuilles ondulées
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height

                // Halo zen ambré doux en haut à droite
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            (if (isDark) Color(0xFFE2B774) else Color(0xFFFBBF24)).copy(alpha = if (isDark) 0.16f else 0.16f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.88f, h * 0.12f),
                        radius = w * 0.70f
                    )
                )

                // Halo apaisant en bas à gauche
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            (if (isDark) Color(0xFF818CF8) else Color(0xFFFB923C)).copy(alpha = if (isDark) 0.12f else 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.12f, h * 0.88f),
                        radius = w * 0.65f
                    )
                )

                // Motifs de feuilles douces ondulées en filigrane artistique
                val leafPathTop = Path().apply {
                    moveTo(w * 0.65f, 0f)
                    cubicTo(w * 0.80f, h * 0.08f, w * 0.90f, h * 0.04f, w, h * 0.15f)
                }
                drawPath(
                    path = leafPathTop,
                    color = autumnAmber.copy(alpha = if (isDark) 0.20f else 0.12f),
                    style = Stroke(width = 1.6f)
                )

                val leafPathBottom = Path().apply {
                    moveTo(0f, h * 0.86f)
                    cubicTo(w * 0.18f, h * 0.92f, w * 0.32f, h * 0.83f, w * 0.45f, h)
                }
                drawPath(
                    path = leafPathBottom,
                    color = autumnTerracotta.copy(alpha = if (isDark) 0.18f else 0.12f),
                    style = Stroke(width = 1.6f)
                )
            }

            // Conteneur principal plein écran : Titre, logo et cadre du texte positionnés en haut
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(start = 10.dp, top = 24.dp, end = 10.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // 1. Barre supérieure : Titre centralisé avec ClassHeure en italique
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val titleAnnotated = buildAnnotatedString {
                        if (isArabic) {
                            append("حول تطبيق ")
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                append("ClassHeure")
                            }
                        } else {
                            append("À propos de ")
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                append("ClassHeure")
                            }
                        }
                    }
                    Text(
                        text = titleAnnotated,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isArabic) "دفتر الأستاذ الرقمي للتعليم" else "Cahier Numérique de l'Enseignant",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.5.sp,
                        textAlign = TextAlign.Center,
                        color = if (isDark) Color(0xFF94A3B8) else autumnCopper,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Section Logo centrale : Arrière-plan 100% transparent identique à la fenêtre
                val logoPainter = runCatching { painterResource(id = R.drawable.ic_home_logo) }.getOrNull()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    if (logoPainter != null) {
                        Image(
                            painter = logoPainter,
                            contentDescription = "Logo ClassHeure",
                            modifier = Modifier.fillMaxHeight(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Logo ClassHeure",
                            tint = autumnAmber,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Encadré du texte (Card) : Parfaitement centralisé, plus large, couleurs zen
                val cardBgBrush = if (isDark) {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF282F40).copy(alpha = 0.95f),
                            Color(0xFF222838).copy(alpha = 0.95f),
                            Color(0xFF1E2432).copy(alpha = 0.95f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFFFFDF8),
                            Color(0xFFFEF7EA)
                        )
                    )
                }

                val cardBorderBrush = Brush.linearGradient(
                    listOf(
                        autumnAmber.copy(alpha = if (isDark) 0.50f else 0.55f),
                        autumnPeach.copy(alpha = if (isDark) 0.40f else 0.45f),
                        autumnGold.copy(alpha = if (isDark) 0.50f else 0.55f)
                    )
                )

                val textColorPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1C1917)
                val textColorSecondary = if (isDark) Color(0xFFCBD5E1) else Color(0xFF44403C)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Consomme le clic pour ne pas fermer en cliquant sur le texte */ }
                        .shadow(
                            elevation = if (isDark) 4.dp else 4.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = autumnAmber.copy(alpha = if (isDark) 0.20f else 0.15f),
                            spotColor = autumnTerracotta.copy(alpha = if (isDark) 0.25f else 0.20f)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.2.dp, cardBorderBrush)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardBgBrush)
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            // Badge "Mot de l'auteur"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF333E56) else Color(0xFFFEF3C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = autumnAmber,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = if (isArabic) "كلمة المصمم" else "Mot de l'auteur",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp,
                                    color = autumnTerracotta
                                )
                            }

                            // Séparateur ondulé artistique style parchemin
                            HandDrawnArtisticDivider(
                                color = autumnAmber.copy(alpha = 0.55f),
                                variant = 1
                            )

                            // Paragraphe 1 : Concepteur & Établissement
                            Text(
                                text = if (isArabic) "تم تصميم هذا التطبيق بواسطة الأستاذ فاتح\u00A0يايا، أستاذ اللغة الأمازيغية بثانوية غربي محند شريف - سيدي عيش، ولاية بجاية."
                                       else "Cette application a été conçue et pensée par Fatah\u00A0YAYA, enseignant de langue amazighe au lycée GHERBI Mohand Chérif - Sidi Aich, Béjaia.",
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = if (isArabic) 22.sp else 18.sp,
                                color = textColorPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Start
                            )

                            // Paragraphe 2 : Visée pédagogique
                            Text(
                                text = if (isArabic) "يهدف هذا التطبيق إلى مساعدة الأساتذة الكرام في أداء مهامهم البيداغوجية والتربوية، من خلال تزويدهم بأداة رقمية عصرية، شاملة وسلسة لإدارة أنشطتهم التعليمية بكل فعالية."
                                       else "Son objectif est de faciliter la mission pédagogique de ses collègues enseignants en leur offrant un outil moderne, complet et intuitif pour gérer facilement et avec une grande efficacité leurs différentes activités éducatives.",
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = if (isArabic) 22.sp else 18.sp,
                                color = textColorPrimary,
                                fontSize = 12.5.sp,
                                textAlign = TextAlign.Start
                            )

                            // Paragraphe 3 : Contact & retours
                            Text(
                                text = if (isArabic) "لأي ملاحظات، اقتراحات أو استفسارات، يمكنكم التواصل مباشرة مع المصمم :"
                                       else "Pour toute remarque, retour d'expérience ou suggestion d'amélioration, vous pouvez contacter directement l'auteur :",
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = if (isArabic) 19.sp else 16.sp,
                                color = textColorSecondary,
                                fontSize = 11.5.sp
                            )

                            // Bouton Email vers Gmail avec icône et copie rapide
                            val recipient = "fatah06@gmail.com"
                            val subject = "ClassHeure - Remarque / Suggestion"

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF1E2536) else Color(0xFF1E293B),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDark) autumnAmber.copy(alpha = 0.50f) else autumnAmber.copy(alpha = 0.45f)
                                ),
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            val gmailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("mailto:$recipient")
                                                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
                                                putExtra(Intent.EXTRA_SUBJECT, subject)
                                                setPackage("com.google.android.gm")
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(gmailIntent)
                                        } catch (e: Exception) {
                                            try {
                                                val fallbackIntent = Intent(Intent.ACTION_SENDTO).apply {
                                                    data = Uri.parse("mailto:$recipient")
                                                    putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
                                                    putExtra(Intent.EXTRA_SUBJECT, subject)
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(fallbackIntent)
                                            } catch (e2: Exception) {
                                                clipboardManager.setText(AnnotatedString(recipient))
                                                Toast.makeText(context, if (isArabic) "تم نسخ البريد الإلكتروني!" else "Adresse e-mail copiée !", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = Color(0xFFFDE68A),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Text(
                                            text = recipient,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copier",
                                        tint = Color.White.copy(alpha = 0.75f),
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable {
                                                clipboardManager.setText(AnnotatedString(recipient))
                                                Toast.makeText(context, if (isArabic) "تم نسخ البريد الإلكتروني!" else "Adresse e-mail copiée !", Toast.LENGTH_SHORT).show()
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
