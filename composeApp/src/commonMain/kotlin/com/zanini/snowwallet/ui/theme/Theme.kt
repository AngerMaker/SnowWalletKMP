// src/commonMain/kotlin/com/zanini/snowwallet/ui/theme/Theme.kt
package com.zanini.snowwallet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// NOVO: Paletas curadas de cores Material 3 (Seed Colors)
enum class AppThemePalette(
    val themeName: String, 
    val primaryLight: Color, 
    val secondaryLight: Color, 
    val surfaceVariantLight: Color,
    val primaryDark: Color,   
    val secondaryDark: Color,   
    val surfaceVariantDark: Color
) {
    PURPLE("Roxo", 
        Color(0xFF6750A4), Color(0xFF625B71), Color(0xFFEADDFF), // Light
        Color(0xFFD0BCFF), Color(0xFFCCC2DC), Color(0xFF4F378B)  // Dark
    ),
    BLUE("Azul", 
        Color(0xFF0061A4), Color(0xFF535F70), Color(0xFFD1E4FF), 
        Color(0xFF9ECAFF), Color(0xFFBBC7DB), Color(0xFF00497D)
    ),
    GREEN("Verde", 
        Color(0xFF386A20), Color(0xFF55624C), Color(0xFFB8F397), 
        Color(0xFF9CD67D), Color(0xFFBDCBAE), Color(0xFF205107)
    ),
    ORANGE("Laranja", 
        Color(0xFF984800), Color(0xFF765743), Color(0xFFFFDBC7), 
        Color(0xFFFFB588), Color(0xFFE5C1A9), Color(0xFF733400)
    ),
    PINK("Rosa", 
        Color(0xFF9E2A9B), Color(0xFF6E5676), Color(0xFFFFD6F8), 
        Color(0xFFFFA9FE), Color(0xFFD7BCE1), Color(0xFF7C007C)
    ),
    TEAL("Verde Água", 
        Color(0xFF006A60), Color(0xFF4A635F), Color(0xFF74F8E5), 
        Color(0xFF53DBC9), Color(0xFFB0CCC7), Color(0xFF005048)
    ),
    BROWN("Marrom", 
        Color(0xFF815512), Color(0xFF6F5B40), Color(0xFFFFDDB3), 
        Color(0xFFF6BC70), Color(0xFFDDC2A1), Color(0xFF643F00)
    )
}

@Composable
fun SnowWalletTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColor: String = "PURPLE", // Cor Semente Escolhida
    content: @Composable () -> Unit
) {
    // Busca a paleta escolhida, com fallback seguro para Roxo
    val palette = AppThemePalette.entries.find { it.name == themeColor } ?: AppThemePalette.PURPLE

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.primaryDark,
            secondary = palette.secondaryDark,
            surfaceVariant = palette.surfaceVariantDark, // Tinta suave para cards!
            tertiary = Pink80,
            onPrimary = Color.White
        )
    } else {
        lightColorScheme(
            primary = palette.primaryLight,
            secondary = palette.secondaryLight,
            surfaceVariant = palette.surfaceVariantLight, // Tinta suave para cards!
            tertiary = Pink40,
            onPrimary = Color.White
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}