package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

data class ClassColorPalette(
    val primary: Color,
    val container: Color,
    val onContainer: Color,
    val label: String
)

object ClassColorHelper {
    val palettes = listOf(
        ClassColorPalette(
            primary = Color(0xFF2563EB), // Bleu Royal
            container = Color(0xFFDBEAFE),
            onContainer = Color(0xFF1E40AF),
            label = "Bleu"
        ),
        ClassColorPalette(
            primary = Color(0xFF059669), // Vert Émeraude
            container = Color(0xFFD1FAE5),
            onContainer = Color(0xFF065F46),
            label = "Vert"
        ),
        ClassColorPalette(
            primary = Color(0xFFD97706), // Ambre Doré
            container = Color(0xFFFEF3C7),
            onContainer = Color(0xFF92400E),
            label = "Ambre"
        ),
        ClassColorPalette(
            primary = Color(0xFF7C3AED), // Violet
            container = Color(0xFFEDE9FE),
            onContainer = Color(0xFF5B21B6),
            label = "Violet"
        ),
        ClassColorPalette(
            primary = Color(0xFFDB2777), // Rose / Framboise
            container = Color(0xFFFCE7F3),
            onContainer = Color(0xFF9D174D),
            label = "Rose"
        ),
        ClassColorPalette(
            primary = Color(0xFF0891B2), // Cyan Océan
            container = Color(0xFFCFFAFE),
            onContainer = Color(0xFF155E75),
            label = "Cyan"
        ),
        ClassColorPalette(
            primary = Color(0xFFEA580C), // Orange vif
            container = Color(0xFFFFEDD5),
            onContainer = Color(0xFF9A3412),
            label = "Orange"
        ),
        ClassColorPalette(
            primary = Color(0xFF4F46E5), // Indigo
            container = Color(0xFFE0E7FF),
            onContainer = Color(0xFF3730A3),
            label = "Indigo"
        ),
        ClassColorPalette(
            primary = Color(0xFF65A30D), // Lime / Vert clair
            container = Color(0xFFECFCCB),
            onContainer = Color(0xFF3F6212),
            label = "Lime"
        ),
        ClassColorPalette(
            primary = Color(0xFFB45309), // Cuir / Fauve
            container = Color(0xFFFEEBC8),
            onContainer = Color(0xFF7B341E),
            label = "Caramel"
        ),
        ClassColorPalette(
            primary = Color(0xFF475569), // Ardoise
            container = Color(0xFFF1F5F9),
            onContainer = Color(0xFF334155),
            label = "Ardoise"
        ),
        ClassColorPalette(
            primary = Color(0xFF9333EA), // Pourpre
            container = Color(0xFFF3E8FF),
            onContainer = Color(0xFF6B21A8),
            label = "Pourpre"
        )
    )

    // Palette dédiée et distinctive pour la classe 2SE
    val Palette2SE = ClassColorPalette(
        primary = Color(0xFF6366F1), // Indigo céleste éclatant
        container = Color(0xFFEEF2FF),
        onContainer = Color(0xFF312E81),
        label = "2SE"
    )

    fun getPaletteForClass(classId: Int, className: String = ""): ClassColorPalette {
        val clean = className.trim()
        val norm = clean.replace(" ", "").replace(".", "").replace("-", "").uppercase()
        if (clean.equals("2SE", ignoreCase = true) || clean.contains("2SE", ignoreCase = true) || norm == "2SE" || norm.contains("2SE") || norm.startsWith("2SE")) {
            return Palette2SE
        }
        val seed = if (classId > 0) classId else abs(className.hashCode())
        val index = abs(seed) % palettes.size
        return palettes[index]
    }

    fun getIntColorForClass(classId: Int, className: String = ""): Int {
        val palette = getPaletteForClass(classId, className)
        val c = palette.primary
        return android.graphics.Color.rgb((c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
    }

    fun getContainerIntColorForClass(classId: Int, className: String = ""): Int {
        val palette = getPaletteForClass(classId, className)
        val c = palette.container
        return android.graphics.Color.rgb((c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
    }

    fun getOnContainerIntColorForClass(classId: Int, className: String = ""): Int {
        val palette = getPaletteForClass(classId, className)
        val c = palette.onContainer
        return android.graphics.Color.rgb((c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
    }
}

@Composable
fun ClassColorDot(
    classId: Int,
    className: String = "",
    size: Int = 10,
    modifier: Modifier = Modifier
) {
    val palette = ClassColorHelper.getPaletteForClass(classId, className)
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(palette.primary)
    )
}

@Composable
fun ClassBadge(
    classId: Int,
    className: String,
    modifier: Modifier = Modifier
) {
    val palette = ClassColorHelper.getPaletteForClass(classId, className)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(palette.container)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(palette.primary)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = className,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = palette.onContainer
        )
    }
}
