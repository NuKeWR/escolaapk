package com.nuklear.escola.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ☢ Escola Nuklear — Cores da Logo (Roxo + Creme + Preto)
val NukPurple = Color(0xFF7B2D8E)        // Roxo principal (da logo)
val NukPurpleDark = Color(0xFF4A1A5E)    // Roxo escuro
val NukPurpleLight = Color(0xFFAB5CC2)   // Roxo claro
val NukCream = Color(0xFFF5F0DC)         // Creme/bege (da logo)
val NukBlack = Color(0xFF1A1A1A)         // Preto fundo
val NukDarkGray = Color(0xFF2E2E2E)      // Card escuro
val NukMedGray = Color(0xFF424242)       // Cinza médio
val NukLightGray = Color(0xFF9E9E9E)     // Texto secundário
val NukWhite = Color(0xFFFFFFFF)         // Texto principal
val NukGreen = Color(0xFF4CAF50)         // Em dia / sucesso
val NukYellow = Color(0xFFFFC107)        // Aviso / bolsista
val NukBlue = Color(0xFF2196F3)          // Info / isento
val NukRed = Color(0xFFD32F2F)           // Erro / inadimplente
val NukRedDark = Color(0xFF9A0007)
val NukRedLight = Color(0xFFFF6659)

private val DarkColorScheme = darkColorScheme(
    primary = NukPurple,
    onPrimary = NukCream,
    primaryContainer = NukPurpleDark,
    onPrimaryContainer = NukCream,
    secondary = NukMedGray,
    onSecondary = NukWhite,
    background = NukBlack,
    onBackground = NukWhite,
    surface = NukDarkGray,
    onSurface = NukWhite,
    surfaceVariant = NukMedGray,
    onSurfaceVariant = NukLightGray,
    error = Color(0xFFCF6679),
    onError = NukBlack,
)

@Composable
fun EscolaNuklearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
