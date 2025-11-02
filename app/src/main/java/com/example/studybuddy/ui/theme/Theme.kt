package com.example.studybuddy.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color
import com.example.studybuddy.ui.theme.AzulPrincipal
import com.example.studybuddy.ui.theme.AzulElementos
import com.example.studybuddy.ui.theme.AmarilloDestacar
import com.example.studybuddy.ui.theme.GrisClaro
import com.example.studybuddy.ui.theme.GrisOscuro
import com.example.studybuddy.ui.theme.Blanco

private val LightColorScheme = lightColorScheme(
    primary = AzulPrincipal,
    onPrimary = Blanco,
    secondary = AzulElementos,
    onSecondary = Blanco,
    background = GrisClaro,
    onBackground = GrisOscuro,
    surface = Blanco,
    onSurface = GrisOscuro

)

private val DarkColorScheme = darkColorScheme(
    primary = AzulPrincipal,
    onPrimary = Blanco,
    secondary = AzulElementos,
    onSecondary = Blanco,
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF2B292F),
    onSurface = Color(0xFFE6E1E5),
    tertiary = AmarilloDestacar,
    error = Color(0xFFCF6679),
    onError = Color.Black
)

@Composable
fun StudyBuddyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}