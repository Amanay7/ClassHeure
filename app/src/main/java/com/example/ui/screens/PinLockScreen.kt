package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
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
import com.example.ui.components.GlassmorphicBackground
import com.example.ui.components.GlassmorphicCard
import com.example.ui.theme.OffWhite
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.repeatOnLifecycle
import com.example.util.BiometricHelper
import com.example.util.Tr
import com.example.util.tr

@Composable
fun PinLockScreen(
    correctPin: String,
    isBiometricEnabled: Boolean = false,
    activity: FragmentActivity? = null,
    onUnlockSuccess: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPinFallback by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activityContext = com.example.LocalActivityContext.current
    val resolvedActivity = remember(activity, context, activityContext) {
        activity ?: BiometricHelper.findActivity(activityContext) ?: BiometricHelper.findActivity(context)
    }

    val biometricStatus = remember(resolvedActivity, context) {
        BiometricHelper.checkBiometricStatus(resolvedActivity ?: context)
    }

    // Le mode biométrique n'est actif QUE si l'utilisateur a activé l'option ET a une empreinte enregistrée
    val canUseBiometric = isBiometricEnabled && biometricStatus == BiometricHelper.BiometricStatus.AVAILABLE

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val isAr = Tr.isArabic()

    fun triggerBiometric() {
        if (resolvedActivity != null && canUseBiometric) {
            BiometricHelper.showBiometricPrompt(
                activity = resolvedActivity,
                title = if (isAr) "إلغاء قفل ClassHeure" else "Déverrouillage ClassHeure",
                subtitle = if (isAr) "المصادقة البيومترية" else "Authentification biométrique",
                negativeButtonText = if (isAr) "إدخال رمز PIN" else "Saisir un code PIN",
                onSuccess = onUnlockSuccess,
                onError = { err ->
                    errorMessage = err
                }
            )
        }
    }

    // Déclenchement automatique de l'invite d'empreinte uniquement si l'empreinte est disponible et enregistrée
    LaunchedEffect(canUseBiometric, resolvedActivity) {
        if (canUseBiometric && resolvedActivity != null && !showPinFallback) {
            lifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
                kotlinx.coroutines.delay(350)
                triggerBiometric()
            }
        }
    }

    val isLight = MaterialTheme.colorScheme.background == OffWhite || MaterialTheme.colorScheme.background == Color(0xFFF8FAFC)
    GlassmorphicBackground(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (canUseBiometric && !showPinFallback) {
                // =========================================================================
                // 1. ÉCRAN DE DÉVERROUILLAGE BIOMÉTRIQUE PUR (SANS PAVÉ NUMÉRIQUE SOUS L'EMPREINTE)
                // =========================================================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .size(105.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .clickable {
                                errorMessage = null
                                triggerBiometric()
                            }
                            .testTag("biometric_scan_button")
                    ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = if (isAr) "بصمة الإصبع" else "Empreinte digitale",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "ClassHeure",
                    fontFamily = com.example.ui.theme.TimesNewRomanFamily,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isAr) "إلغاء القفل ببصمة الإصبع" else "Déverrouillage par empreinte digitale",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isAr) "ضع إصبعك على مستشعر البصمة بهاتفك لإلغاء قفل التطبيق." else "Placez votre doigt sur le capteur de votre smartphone pour déverrouiller l'application.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            errorMessage = null
                            showPinFallback = true
                        }
                        .testTag("pin_fallback_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Password,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isAr) "إدخال رمز PIN" else "Saisir un code PIN",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        } else {
            // =========================================================================
            // 2. ÉCRAN DE SAISIE DU CODE PIN (UTILISÉ SI PAS D'EMPREINTE OU DEMANDE EXPLICITE)
            // =========================================================================
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.size(76.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = if (isAr) "قفل برمز PIN" else "Verrouillage par code PIN",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ClassHeure",
                    fontFamily = com.example.ui.theme.TimesNewRomanFamily,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isAr) "يرجى إدخال رمز PIN لإلغاء قفل التطبيق." else "Veuillez entrer votre code PIN pour déverrouiller l'application.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Password,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAr) "إدخال رمز PIN" else "Saisir un code PIN",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Indicateurs visuels des 4 chiffres du code PIN
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val filled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .shadow(if (filled) 3.dp else 0.dp, CircleShape)
                                .clip(CircleShape)
                                .background(
                                    if (filled) MaterialTheme.colorScheme.primary
                                    else if (isLight) MaterialTheme.colorScheme.surfaceVariant
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .border(
                                    1.2.dp,
                                    if (filled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                    CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    Spacer(modifier = Modifier.height(26.dp))
                }

                // Pavé numérique 0-9
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf(if (canUseBiometric) "fingerprint" else "", "0", "back")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    keys.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            modifier = Modifier.fillMaxWidth(0.82f)
                        ) {
                            row.forEach { key ->
                                val isFingerprint = key == "fingerprint"
                                val isBack = key == "back"
                                val isEnabled = key.isNotEmpty()

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .shadow(if (isEnabled) 2.dp else 0.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isFingerprint -> MaterialTheme.colorScheme.primaryContainer
                                                isEnabled -> if (isLight) Color.White else MaterialTheme.colorScheme.surfaceVariant
                                                else -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = if (isEnabled) 1.dp else 0.dp,
                                            color = when {
                                                isFingerprint -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                                isEnabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isLight) 0.55f else 0.35f)
                                                else -> Color.Transparent
                                            },
                                            shape = CircleShape
                                        )
                                        .clickable(enabled = isEnabled) {
                                            errorMessage = null
                                            if (isFingerprint) {
                                                showPinFallback = false
                                                triggerBiometric()
                                            } else if (isBack) {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                }
                                            } else {
                                                if (enteredPin.length < 4) {
                                                    enteredPin += key
                                                }
                                                if (enteredPin.length == 4) {
                                                    if (enteredPin == correctPin) {
                                                        onUnlockSuccess()
                                                    } else {
                                                        errorMessage = if (isAr) "رمز PIN غير صحيح. يرجى المحاولة مرة أخرى." else "Code PIN incorrect. Veuillez réessayer."
                                                        enteredPin = ""
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isFingerprint) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = if (isAr) "بصمة الإصبع" else "Empreinte digitale",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    } else if (isBack) {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = if (isAr) "مسح" else "Effacer",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else if (key.isNotEmpty()) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (canUseBiometric) {
                    Spacer(modifier = Modifier.height(14.dp))
                    TextButton(
                        onClick = {
                            errorMessage = null
                            showPinFallback = false
                            triggerBiometric()
                        }
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isAr) "إلغاء القفل بالبصمة" else "Déverrouiller par empreinte", fontSize = 12.5.sp)
                    }
                }
            }
        }
    }
}
}
