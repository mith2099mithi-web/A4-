package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle

/** App-wide font: bundled Inter, consistent with the default document font. */
val AppFontFamily = DocumentFonts.Inter.composeFamily

private val defaults = Typography()
private fun TextStyle.withAppFont(): TextStyle = copy(fontFamily = AppFontFamily)

val Typography = Typography(
    displayLarge = defaults.displayLarge.withAppFont(),
    displayMedium = defaults.displayMedium.withAppFont(),
    displaySmall = defaults.displaySmall.withAppFont(),
    headlineLarge = defaults.headlineLarge.withAppFont(),
    headlineMedium = defaults.headlineMedium.withAppFont(),
    headlineSmall = defaults.headlineSmall.withAppFont(),
    titleLarge = defaults.titleLarge.withAppFont(),
    titleMedium = defaults.titleMedium.withAppFont(),
    titleSmall = defaults.titleSmall.withAppFont(),
    bodyLarge = defaults.bodyLarge.withAppFont(),
    bodyMedium = defaults.bodyMedium.withAppFont(),
    bodySmall = defaults.bodySmall.withAppFont(),
    labelLarge = defaults.labelLarge.withAppFont(),
    labelMedium = defaults.labelMedium.withAppFont(),
    labelSmall = defaults.labelSmall.withAppFont()
)
