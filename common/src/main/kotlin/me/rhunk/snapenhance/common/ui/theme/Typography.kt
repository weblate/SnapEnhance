package me.rhunk.snapenhance.common.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import me.rhunk.snapenhance.common.R

val ProductSansFamily = FontFamily(
    Font(resId = R.font.product_sans, weight = FontWeight.Normal)
)

private val defaultTypography = Typography()

val ProductSansTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = ProductSansFamily),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = ProductSansFamily),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = ProductSansFamily),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = ProductSansFamily),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = ProductSansFamily),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = ProductSansFamily),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = ProductSansFamily),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = ProductSansFamily),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = ProductSansFamily),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = ProductSansFamily),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = ProductSansFamily),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = ProductSansFamily),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = ProductSansFamily),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = ProductSansFamily),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = ProductSansFamily)
)