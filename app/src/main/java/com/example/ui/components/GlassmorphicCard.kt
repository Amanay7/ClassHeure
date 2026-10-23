package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OffWhite

import androidx.compose.ui.draw.drawWithCache

/**
 * Splendid Material 3 Expressive Ambient Background.
 * Renders an ultra-clean, serene canvas with a subtle, dynamic tonal aura
 * that harmonizes gracefully with the active Material You color palette.
 */
@Composable
fun MaterialExpressiveBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background == OffWhite || MaterialTheme.colorScheme.background == Color(0xFFF8FAFC)
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val width = size.width
                val height = size.height
                val baseBrush = if (isLight) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8FAFC),
                            Color(0xFFF1F5F9),
                            Color(0xFFEFF3F8)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0B1120),
                            Color(0xFF0F172A),
                            Color(0xFF090D17)
                        )
                    )
                }
                val topAuraCenter = Offset(width * 0.88f, height * 0.08f)
                val topAuraRadius = (width * 0.78f).coerceAtLeast(1f)
                val topAuraBrush = Brush.radialGradient(
                    colors = if (isLight) {
                        listOf(
                            primaryColor.copy(alpha = 0.06f),
                            primaryColor.copy(alpha = 0.02f),
                            Color.Transparent
                        )
                    } else {
                        listOf(
                            primaryColor.copy(alpha = 0.11f),
                            primaryColor.copy(alpha = 0.03f),
                            Color.Transparent
                        )
                    },
                    center = topAuraCenter,
                    radius = topAuraRadius
                )
                val midAuraCenter = Offset(width * 0.08f, height * 0.48f)
                val midAuraRadius = (width * 0.68f).coerceAtLeast(1f)
                val midAuraBrush = Brush.radialGradient(
                    colors = if (isLight) {
                        listOf(
                            tertiaryColor.copy(alpha = 0.045f),
                            Color.Transparent
                        )
                    } else {
                        listOf(
                            tertiaryColor.copy(alpha = 0.07f),
                            Color.Transparent
                        )
                    },
                    center = midAuraCenter,
                    radius = midAuraRadius
                )
                onDrawBehind {
                    drawRect(brush = baseBrush)
                    drawCircle(brush = topAuraBrush, center = topAuraCenter, radius = topAuraRadius)
                    drawCircle(brush = midAuraBrush, center = midAuraCenter, radius = midAuraRadius)
                }
            }
    ) {
        content()
    }
}

/**
 * Backward compatibility alias for the Material 3 Expressive Background.
 */
@Composable
fun GlassmorphicBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    MaterialExpressiveBackground(modifier = modifier, content = content)
}

/**
 * Spatial Glass Card with sculpted geometry, refined elevation,
 * cached specular hairline reflections, and smooth scrolling performance.
 */
@Composable
fun MaterialExpressiveCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 2.5.dp,
    onClick: (() -> Unit)? = null,
    borderWidth: Dp = 1.dp,
    glassAlpha: Float = 1.0f,
    tintColor: Color? = null,
    tintAlpha: Float? = null,
    borderColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background == OffWhite || MaterialTheme.colorScheme.background == Color(0xFFF8FAFC)
    val baseSurface = if (isLight) Color.White else Color(0xFF131C31)

    val containerColor = remember(isLight, tintColor, tintAlpha, baseSurface) {
        if (tintColor != null) {
            val effectiveAlpha = tintAlpha ?: if (isLight) 0.08f else 0.16f
            tintColor.copy(alpha = effectiveAlpha).compositeOver(baseSurface)
        } else {
            baseSurface
        }
    }

    val borderBrush = remember(isLight, tintColor, borderColor) {
        if (borderColor != null) {
            Brush.linearGradient(listOf(borderColor, borderColor))
        } else if (tintColor != null) {
            if (isLight) {
                Brush.linearGradient(
                    listOf(
                        tintColor.copy(alpha = 0.38f),
                        tintColor.copy(alpha = 0.22f)
                    )
                )
            } else {
                Brush.linearGradient(
                    listOf(
                        tintColor.copy(alpha = 0.55f),
                        tintColor.copy(alpha = 0.28f)
                    )
                )
            }
        } else {
            if (isLight) {
                Brush.linearGradient(
                    listOf(
                        Color(0xFFE2E8F0),
                        Color(0xFFCBD5E1).copy(alpha = 0.75f)
                    )
                )
            } else {
                Brush.linearGradient(
                    listOf(
                        Color(0xFF334155).copy(alpha = 0.85f),
                        Color(0xFF1E293B).copy(alpha = 0.75f)
                    )
                )
            }
        }
    }

    val ambientShadow = remember(isLight, tintColor) {
        if (tintColor != null) tintColor.copy(alpha = if (isLight) 0.12f else 0.28f)
        else (if (isLight) Color(0x14000000) else Color(0x45000000))
    }
    val spotShadow = remember(isLight, tintColor) {
        if (tintColor != null) tintColor.copy(alpha = if (isLight) 0.18f else 0.36f)
        else (if (isLight) Color(0x1C000000) else Color(0x60000000))
    }

    Surface(
        modifier = modifier
            .then(
                if (elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = elevation,
                        shape = shape,
                        ambientColor = ambientShadow,
                        spotColor = spotShadow,
                        clip = false
                    )
                } else Modifier
            )
            .border(borderWidth, borderBrush, shape),
        shape = shape,
        color = containerColor,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = tintColor ?: MaterialTheme.colorScheme.primary),
                            onClick = onClick
                        )
                    } else Modifier
                )
                .drawWithCache {
                    val w = size.width
                    val highlightBrush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            if (isLight) Color.White.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                    onDrawWithContent {
                        drawContent()
                        drawLine(
                            brush = highlightBrush,
                            start = Offset(w * 0.12f, 1f),
                            end = Offset(w * 0.88f, 1f),
                            strokeWidth = 1f
                        )
                    }
                }
        ) {
            content()
        }
    }
}

/**
 * Backward compatibility alias for MaterialExpressiveCard.
 */
@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    elevation: Dp = 3.5.dp,
    onClick: (() -> Unit)? = null,
    borderWidth: Dp = 1.2.dp,
    glassAlpha: Float = 1.0f,
    tintColor: Color? = null,
    tintAlpha: Float? = null,
    borderColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    MaterialExpressiveCard(
        modifier = modifier,
        shape = shape,
        elevation = elevation,
        onClick = onClick,
        borderWidth = borderWidth,
        glassAlpha = glassAlpha,
        tintColor = tintColor,
        tintAlpha = tintAlpha,
        borderColor = borderColor,
        content = content
    )
}

/**
 * Splendid Material 3 Expressive Surface for inner sections, list items, search bars, and dialogs.
 */
@Composable
fun MaterialSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    elevation: Dp = 0.dp,
    glassAlpha: Float = 1.0f,
    tintColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    MaterialExpressiveCard(
        modifier = modifier,
        shape = shape,
        elevation = elevation,
        glassAlpha = glassAlpha,
        tintColor = tintColor,
        borderColor = borderColor,
        borderWidth = borderWidth,
        onClick = onClick,
        content = content
    )
}

@Composable
fun GlassmorphicSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    elevation: Dp = 0.dp,
    glassAlpha: Float = 1.0f,
    tintColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    MaterialSurface(
        modifier = modifier,
        shape = shape,
        elevation = elevation,
        glassAlpha = glassAlpha,
        tintColor = tintColor,
        borderColor = borderColor,
        borderWidth = borderWidth,
        onClick = onClick,
        content = content
    )
}

/**
 * Splendid Material 3 Expressive Tonal Pill Badge.
 * High-legibility status chips, tags, classes, and time slots in true Material You pill styling.
 */
@Composable
fun MaterialBadge(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = RoundedCornerShape(50), // Signature Material 3 Pill / Capsule
    borderWidth: Dp = 1.dp,
    content: @Composable RowScope.() -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background == OffWhite || MaterialTheme.colorScheme.background == Color(0xFFF8FAFC)
    val bg = if (isLight) tint.copy(alpha = 0.12f) else tint.copy(alpha = 0.22f)
    val borderCol = if (isLight) tint.copy(alpha = 0.35f) else tint.copy(alpha = 0.45f)

    Box(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(borderWidth, borderCol, shape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun GlassmorphicBadge(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = RoundedCornerShape(50),
    borderWidth: Dp = 1.dp,
    content: @Composable RowScope.() -> Unit
) {
    MaterialBadge(
        modifier = modifier,
        tint = tint,
        shape = shape,
        borderWidth = borderWidth,
        content = content
    )
}
