package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val readingFont = FontFamily.SansSerif

val Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = readingFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = readingFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 27.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = readingFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = readingFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = readingFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = readingFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = readingFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = readingFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontFamily = readingFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = readingFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)
