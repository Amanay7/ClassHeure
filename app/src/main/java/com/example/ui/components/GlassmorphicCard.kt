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

/**
 * Splendid Material 3 Expressive Ambient Background.
 * Renders an ultra-clean, serene canvas with a subtle, dynamic tonal aura
 * that harmonizes gracefully with the active Material You color palette (Marine, Emerald, Amethyst, etc.).
 */
@Composable
fun MaterialExpressiveBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background == OffWhite || MaterialTheme.colorScheme.background == Color(0xFFF8FAFC)
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            if (isLight) {
                // Base background: Pristine, airy Material You canvas
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF9FAFC),
                            Color(0xFFF3F5F9),
                            Color(0xFFEDF0F6)
                        )
                    )
                )

                // Very subtle top-right tonal aura (Material You dynamic accent)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.06f),
                            primaryColor.copy(alpha = 0.02f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.90f, height * 0.08f),
                        radius = width * 0.75f
                    ),
                    center = Offset(width * 0.90f, height * 0.08f),
                    radius = width * 0.75f
                )

                // Gentle center-left tonal warmth
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tertiaryColor.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.06f, height * 0.45f),
                        radius = width * 0.65f
                    ),
                    center = Offset(width * 0.06f, height * 0.45f),
                    radius = width * 0.65f
                )
            } else {
                // Base background: Deep, luxurious OLED Material 3 dark surface
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0C101C),
                            Color(0xFF0F172A),
                            Color(0xFF090D17)
                        )
                    )
                )

                // Subtle top tonal glow for dark theme
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.12f),
                            primaryColor.copy(alpha = 0.03f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.85f, height * 0.10f),
                        radius = width * 0.80f
                    ),
                    center = Offset(width * 0.85f, height * 0.10f),
                    radius = width * 0.80f
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tertiaryColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.08f, height * 0.50f),
                        radius = width * 0.70f
                    ),
                    center = Offset(width * 0.08f, height * 0.50f),
                    radius = width * 0.70f
                )
            }
        }

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
 * Splendid iOS 27 Spatial Glass Card with sculpted squircle geometry, 3D multi-layered depth,
 * specular hairline reflections, and authentic tactile feedback.
 */
@Composable
fun MaterialExpressiveCard(
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
    val isLight = MaterialTheme.colorScheme.background == OffWhite || MaterialTheme.colorScheme.background == Color(0xFFF8FAFC)
    val baseSurface = if (isLight) Color.White else Color(0xFF1E222D)

    // iOS 27 Solid Spatial Container - compositeOver guarantees 100% opacity, pure contrast, and vibrant colors
    val containerColor = if (tintColor != null) {
        val effectiveAlpha = tintAlpha ?: if (isLight) 0.10f else 0.18f
        tintColor.copy(alpha = effectiveAlpha).compositeOver(baseSurface)
    } else {
        baseSurface
    }

    // iOS 27 Specular Prismatic Hairline Border Brush
    val borderBrush = if (borderColor != null) {
        Brush.linearGradient(listOf(borderColor, borderColor))
    } else if (tintColor != null) {
        if (isLight) {
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.95f),
                    tintColor.copy(alpha = 0.50f),
                    tintColor.copy(alpha = 0.25f)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.35f),
                    tintColor.copy(alpha = 0.65f),
                    Color(0xFF27272A).copy(alpha = 0.40f)
                )
            )
        }
    } else {
        if (isLight) {
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.95f),
                    Color(0xFFCBD5E1).copy(alpha = 0.70f),
                    Color(0xFFE2E8F0).copy(alpha = 0.50f)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.35f),
                    Color(0xFF52525B).copy(alpha = 0.40f),
                    Color(0xFF18181B).copy(alpha = 0.60f)
                )
            )
        }
    }

    // iOS 27 Multi-layer 3D elevation shadow + surface
    Surface(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = if (tintColor != null) tintColor.copy(alpha = if (isLight) 0.16f else 0.35f) else (if (isLight) Color(0x1A000000) else Color(0x60000000)),
                spotColor = if (tintColor != null) tintColor.copy(alpha = if (isLight) 0.24f else 0.45f) else (if (isLight) Color(0x24000000) else Color(0x80000000)),
                clip = false
            )
            .border(borderWidth, borderBrush, shape),
        shape = shape,
        color = containerColor,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = tintColor ?: MaterialTheme.colorScheme.primary),
                            onClick = onClick
                        )
                    } else Modifier
                )
        ) {
            // Reflet spéculaire supérieur 3D iOS 27
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            (if (isLight) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.25f)),
                            Color.Transparent
                        )
                    ),
                    start = Offset(w * 0.10f, 1.2f),
                    end = Offset(w * 0.90f, 1.2f),
                    strokeWidth = 1.2f
                )
            }

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
