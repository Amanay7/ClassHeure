package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val DecotypeThuluthFamily = FontFamily(
    Font(R.font.decotype_thuluth, FontWeight.Normal),
    Font(R.font.decotype_thuluth, FontWeight.Bold)
)

val AmiriFamily = FontFamily(
    Font(R.font.amiri, FontWeight.Normal)
)

val CairoFamily = FontFamily(
    Font(R.font.cairo, FontWeight.Normal)
)

val TimesNewRomanFamily = FontFamily(
    Font(R.font.times_new_roman, FontWeight.Normal),
    Font(R.font.times_new_roman_bold, FontWeight.Bold)
)

// Set of Material typography styles to start with
val Typography =
  Typography(
    bodyLarge =
      TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
      )
  )

