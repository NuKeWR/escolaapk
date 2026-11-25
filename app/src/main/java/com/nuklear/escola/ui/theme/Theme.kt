package com.nuklear.escola.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ☢ NUKLEAR — Premium Dark Interface
//
//  Preto profundo  -> fundo principal
//  Grafite         -> superfícies e cartões
//  Roxo violeta    -> identidade e ações principais
//  Branco suave    -> textos e informações
//  Verde discreto  -> pagamentos confirmados
//  Laranja suave   -> pendências e alertas
val NukBlack = Color(0xFF0B0B10)         // Fundo principal (preto profundo)
val NukBar = Color(0xFF101017)           // Barra inferior
val NukDarkGray = Color(0xFF16161E)      // Cartões e superfícies (grafite)
val NukMedGray = Color(0xFF2B2B38)       // Elementos secundários, bordas de campos
val NukBorder = Color(0xFF262633)        // Contorno sutil dos cartões

val NukPurple = Color(0xFF7C4DFF)        // Roxo violeta principal
val NukPurpleDark = Color(0xFF2E1F66)    // Roxo escuro (fundos tingidos)
val NukPurpleLight = Color(0xFFB39DFF)   // Roxo claro (ícones, destaques)

val NukCream = Color(0xFFF2F1F8)         // Branco suave
val NukWhite = Color(0xFFF2F1F8)         // Texto principal
val NukLightGray = Color(0xFF9794A8)     // Texto secundário

val NukGreen = Color(0xFF34B27A)         // Pago / em dia
val NukOrange = Color(0xFFF5A64A)        // Pendência / alerta
val NukYellow = Color(0xFFE9C46A)        // Bolsista
val NukBlue = Color(0xFF5B9DFF)          // Informação / isento
val NukRed = Color(0xFFE5484D)           // Erro
val NukRedDark = Color(0xFF8F2D36)       // Ações perigosas
val NukRedLight = Color(0xFFFF7B80)

private val DarkColorScheme = darkColorScheme(
    primary = NukPurple,
    onPrimary = Color.White,
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
    outline = NukMedGray,
    error = NukRedLight,
    onError = NukBlack,
)

@Composable
fun EscolaNuklearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
