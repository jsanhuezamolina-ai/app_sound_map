package com.jonathan.soundmap.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.jonathan.soundmap.domain.model.NoiseLevel

val QuietColor = Color(0xFF2FB36B)
val ModerateColor = Color(0xFFF2B33D)
val LoudColor = Color(0xFFE5484D)

fun NoiseLevel.color(): Color = when (this) {
    NoiseLevel.QUIET -> QuietColor
    NoiseLevel.MODERATE -> ModerateColor
    NoiseLevel.LOUD -> LoudColor
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F6F8B),
    secondary = Color(0xFF4A6572),
    tertiary = Color(0xFF2FB36B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FC8E0),
    secondary = Color(0xFFB0C7D1),
    tertiary = Color(0xFF6FE0A0),
)

/** Tema Material 3. En Android 12+ usa los colores del fondo de pantalla (Material You). */
@Composable
fun SoundMapTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
