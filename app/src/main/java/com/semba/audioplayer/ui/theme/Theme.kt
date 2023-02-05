package com.semba.audioplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColorScheme(
        primary = Primary_Dark,
        secondary = Secondary_Dark,
    background = Color.Black,
    onBackground = Color.White,
    tertiary = Tertiary_Dark,
    onTertiary = On_Tertiary_Dark,
)

private val LightColorPalette = lightColorScheme(
        primary = Primary_Light,
        secondary = Secondary_Light,
    background = Color.White,
    onBackground = Color.Black,
    tertiary = Tertiary_Light,
    onTertiary = On_Tertiary_Light,
)

@Composable
fun AudioPlayerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
            colorScheme = colors,
            typography = Typography,
            content = content
    )
}