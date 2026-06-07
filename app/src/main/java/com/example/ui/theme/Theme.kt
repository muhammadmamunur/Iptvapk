package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = NeonGreen,
    secondary = ShadowMintGreen,
    tertiary = NeonGreen,
    background = DeepCharcoalGreen,
    surface = ShadowMintGreen,
    onPrimary = DeepCharcoalGreen,
    onSecondary = White,
    onTertiary = DeepCharcoalGreen,
    onBackground = White,
    onSurface = White
  )

private val LightColorScheme = DarkColorScheme // We enforce our premium dark sports theme everywhere

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force premium dark sports theme
  dynamicColor: Boolean = false, // Use our brand colors rather than system wallpapers
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme // Unified theme experience for sport fans
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
