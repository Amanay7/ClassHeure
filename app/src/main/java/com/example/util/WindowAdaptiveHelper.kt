package com.example.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Catégories de taille d'écran adaptatives selon les standards Material 3 :
 * - COMPACT : smartphones en portrait (< 600 dp)
 * - MEDIUM : smartphones en paysage, pliables et petites tablettes (600 - 839 dp)
 * - EXPANDED : tablettes standards et grands écrans (>= 840 dp)
 */
enum class WindowWidthSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

@Composable
fun rememberWindowWidthSizeClass(): WindowWidthSizeClass {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp
    return when {
        widthDp < 600 -> WindowWidthSizeClass.COMPACT
        widthDp < 840 -> WindowWidthSizeClass.MEDIUM
        else -> WindowWidthSizeClass.EXPANDED
    }
}

/**
 * Retourne vrai si l'écran est une tablette ou un écran large (>= 600dp de largeur).
 */
@Composable
fun isTablet(): Boolean {
    return LocalConfiguration.current.screenWidthDp >= 600
}

/**
 * Retourne vrai si l'écran est une grande tablette ou écran étendu (>= 840dp).
 */
@Composable
fun isExpandedTablet(): Boolean {
    return LocalConfiguration.current.screenWidthDp >= 840
}

/**
 * Retourne vrai si l'appareil est actuellement orienté en mode paysage.
 */
@Composable
fun isLandscape(): Boolean {
    val config = LocalConfiguration.current
    return config.screenWidthDp > config.screenHeightDp
}

/**
 * Calcule dynamiquement la hauteur maximale sécurisée pour le corps des dialogues.
 * Empêche le rognage ou débordement sur les téléphones de faible hauteur ou en paysage,
 * tout en exploitant judicieusement le grand écran des tablettes.
 */
@Composable
fun adaptiveDialogMaxHeight(
    fraction: Float = 0.65f,
    minHeight: Dp = 220.dp,
    maxHeight: Dp = 640.dp
): Dp {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    return (screenHeight * fraction).coerceIn(minHeight, maxHeight)
}

/**
 * Extension Modifier appliquant la hauteur maximale adaptative sécurisée.
 */
@Composable
fun Modifier.adaptiveDialogMaxHeight(
    fraction: Float = 0.65f,
    minHeight: Dp = 220.dp,
    maxHeight: Dp = 640.dp
): Modifier {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val h = (screenHeight * fraction).coerceIn(minHeight, maxHeight)
    return this.heightIn(max = h)
}

/**
 * Calcule une largeur maximale pour les boîtes de dialogue et fenêtres modales.
 * Pleine largeur adaptative (95%) sur smartphones, centrée et bornée sur tablettes.
 */
@Composable
fun adaptiveDialogMaxWidth(
    tabletMaxWidth: Dp = 680.dp
): Dp {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    return if (screenWidth < 600.dp) screenWidth * 0.95f else tabletMaxWidth
}

/**
 * Conteneur adaptatif principal :
 * - Sur smartphone (< 600dp) : occupe toute la largeur naturelle.
 * - Sur tablette (>= 600dp) : centre élégamment la rubrique avec une largeur maximale idéale
 *   (880dp par défaut) évitant l'étirement disproportionné des cartes et champs de saisie.
 */
@Composable
fun AdaptiveScreenContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 880.dp,
    contentAlignment: Alignment = Alignment.TopCenter,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = contentAlignment
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = maxWidth),
            content = content
        )
    }
}
