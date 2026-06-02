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
    primary = MinimalPrimary,
    secondary = MinimalSecondaryText,
    background = MinimalOnBackground,
    surface = MinimalCardBg,
    onPrimary = MinimalPrimaryContainer,
    onBackground = MinimalBackground,
    onSurface = MinimalOnBackground
  )

private val LightColorScheme =
  lightColorScheme(
    primary = MinimalPrimary,
    secondary = MinimalSecondaryText,
    onPrimary = MinimalOnPrimaryContainer,
    primaryContainer = MinimalPrimaryContainer,
    onPrimaryContainer = MinimalOnPrimaryContainer,
    background = MinimalBackground,
    onBackground = MinimalOnBackground,
    surface = MinimalCardBg,
    onSurface = MinimalOnBackground,
    surfaceVariant = MinimalIconBg,
    outline = MinimalBorder
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is disabled by default to keep the clean minimalism branding
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
