package com.ingrid.airqualitymonitor.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ingrid.airqualitymonitor.data.LegendaStrings
import com.ingrid.airqualitymonitor.ui.theme.AppColors

// ─── Tela de Legenda ──────────────────────────────────────────────────────

@Composable
fun LegendaScreen(onBack: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 5 })
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Slate100)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 56.dp, bottom = 32.dp)
        ) {
            FilledTonalButton(
                onClick = onBack,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = AppColors.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = AppColors.Slate700
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Voltar", color = AppColors.Slate700, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "O que cada indicador significa",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Slate900
            )

            Spacer(modifier = Modifier.height(20.dp))

            LegendaStrings.variaveis.forEach { variavel ->
                LegendaItem(variavel)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

// ─── Item de variável ─────────────────────────────────────────────────────

@Composable
fun LegendaItem(info: LegendaStrings.VariavelInfo) {
    val icon: ImageVector = when {
        info.nome.contains("CO")          -> Icons.Outlined.Air
        info.nome.contains("Temperatura") -> Icons.Outlined.Thermostat
        info.nome.contains("Umidade")     -> Icons.Outlined.WaterDrop
        else                              -> Icons.Outlined.Grain
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.Slate100)
                        .padding(10.dp)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = AppColors.Slate700,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = info.nome,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = AppColors.Slate900
                    )
                    Text(
                        text = info.unidade,
                        fontSize = 12.sp,
                        color = AppColors.Slate500
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = info.descricao,
                fontSize = 13.sp,
                color = AppColors.Slate700,
                lineHeight = 20.sp
            )

            if (info.subItens.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    info.subItens.forEach { subItem -> LegendaSubItem(subItem) }
                }
            }

            info.normRef?.let { ref ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Ref.: $ref",
                    fontSize = 11.sp,
                    color = AppColors.Slate400
                )
            }
        }
    }
}

// ─── Sub-item (PM10 / PM2.5) ─────────────────────────────────────────────

@Composable
fun LegendaSubItem(info: LegendaStrings.SubItemInfo) {
    val icon: ImageVector = when (info.nome) {
        "PM10"  -> Icons.Outlined.BlurOn
        "PM2.5" -> Icons.Outlined.Grain
        else    -> Icons.Outlined.Grain
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Slate100)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = AppColors.Slate500,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = info.nome,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = AppColors.Slate700
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = info.descricao,
                fontSize = 12.sp,
                color = AppColors.Slate700,
                lineHeight = 18.sp
            )
        }
    }
}