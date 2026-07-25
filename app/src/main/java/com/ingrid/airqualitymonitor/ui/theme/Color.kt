package com.ingrid.airqualitymonitor.ui.theme

import androidx.compose.ui.graphics.Color

// Cores padrão geradas pelo Android Studio — usadas como fallback no Theme.kt
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Paleta personalizada do app, baseada na escala Slate do Tailwind CSS
object AppColors {
    // Escala de cinzas azulados — usados em fundos, textos e bordas
    val Slate50    = Color(0xFFF8FAFC)  // Fundo de itens de lista (quase branco)
    val Slate100   = Color(0xFFF1F5F9)  // Fundo das telas
    val Slate400   = Color(0xFF94A3B8)  // Textos muito secundários e marcadores sem destaque
    val Slate500   = Color(0xFF64748B)  // Textos secundários (rótulos, subtítulos)
    val Slate700   = Color(0xFF334155)  // Textos de ênfase média
    val Slate800   = Color(0xFF1E293B)  // Gradiente do card do ESP32
    val Slate900   = Color(0xFF0F172A)  // Títulos principais e card de recomendação escuro
    val White      = Color(0xFFFFFFFF)
    val Emerald500 = Color(0xFF10B981)  // Verde — indicador "Coletando" e botão iniciar
    val Amber500   = Color(0xFFF59E0B)  // Amarelo — ícone de alerta moderado
}
