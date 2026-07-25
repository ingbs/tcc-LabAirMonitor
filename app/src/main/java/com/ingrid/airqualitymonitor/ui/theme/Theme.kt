package com.ingrid.airqualitymonitor.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Esquema de cores padrão para modo escuro (usado quando dynamic color não está disponível)
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

// Esquema de cores padrão para modo claro
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// Função que envolve todo o conteúdo do app aplicando o tema correto
@Composable
fun AirQualityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),  // Detecta automaticamente o modo escuro do sistema
    dynamicColor: Boolean = true,                 // Ativa o Material You (cores do wallpaper)
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Android 12+: extrai as cores do wallpaper do usuário (Material You / Dynamic Color)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme  // Android < 12 no modo escuro
        else -> LightColorScheme       // Android < 12 no modo claro
    }

    // Aplica o colorScheme e a tipografia para todos os composables filhos
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
