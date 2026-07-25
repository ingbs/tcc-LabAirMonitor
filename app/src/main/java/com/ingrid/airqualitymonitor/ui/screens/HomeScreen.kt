package com.ingrid.airqualitymonitor.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ingrid.airqualitymonitor.data.AirQuality
import com.ingrid.airqualitymonitor.data.Esp32Status
import com.ingrid.airqualitymonitor.data.Laboratory
import com.ingrid.airqualitymonitor.ui.theme.AppColors
import com.ingrid.airqualitymonitor.ui.viewmodel.LabViewModel

// ─── Tela inicial ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    labViewModel: LabViewModel,
    onLabSelected: (String) -> Unit,
    onLegendaClick: () -> Unit
) {
    // collectAsStateWithLifecycle observa o StateFlow e recompõe a UI quando o valor muda
    // Também cancela a observação automaticamente quando a tela sai da pilha de navegação
    val labs by labViewModel.labs.collectAsStateWithLifecycle()
    val esp32Status by labViewModel.esp32Status.collectAsStateWithLifecycle()

    // LazyColumn renderiza apenas os itens visíveis na tela (eficiente para listas longas)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Slate100),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 56.dp,   // Espaço para não ficar atrás da barra de status
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── Cabeçalho ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monitoramento",
                        fontSize = 13.sp,
                        color = AppColors.Slate500,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Qualidade do Ar",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Slate900
                    )
                }
                IconButton(onClick = onLegendaClick) {
                    Icon(
                        Icons.Outlined.Help,
                        contentDescription = "Legenda",
                        tint = AppColors.Slate500
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ── Card status ESP32 ──
        item {
            // Simplifica a condição: qualquer status diferente de OFFLINE é considerado online
            val isOnline = esp32Status != Esp32Status.OFFLINE
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    // Gradiente de dois tons escuros para o fundo do card
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AppColors.Slate900, AppColors.Slate800)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Ponto colorido: verde = online, vermelho = offline
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isOnline) Color(0xFF10B981) else Color(0xFFF43F5E)
                            )
                    )
                    Text(
                        text = "ESP32",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (isOnline) "Online" else "Offline",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "·",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (isOnline) "Dispositivo conectado" else "Sem sinal",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Rótulo da lista com indicador de carregamento ──
        item {
            Text(
                text = "AMBIENTES MONITORADOS",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Slate500,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Cards dos laboratórios ──
        // itemsIndexed passa o índice junto com o item, necessário para a animação em cascata
        itemsIndexed(labs) { index, lab ->
            AnimatedLabCard(
                lab = lab,
                index = index,
                onClick = { onLabSelected(lab.id) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ─── Paleta de cores por laboratório ─────────────────────────────────────

private val labAccentColors = listOf(
    Color(0xFF6366F1), // Indigo
    Color(0xFF14B8A6), // Teal
    Color(0xFFF59E0B), // Âmbar
    Color(0xFFEC4899), // Rosa
    Color(0xFF8B5CF6), // Violeta
    Color(0xFF0EA5E9), // Azul céu
    Color(0xFF10B981), // Esmeralda
)

// ─── Card individual do laboratório ────────────────────────────────────────

@Composable
fun AnimatedLabCard(
    lab: Laboratory,
    index: Int,
    onClick: () -> Unit
) {
    val accent = labAccentColors[index % labAccentColors.size]

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 80L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.White)
            ) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .fillMaxHeight()
                        .background(accent)
                        .align(Alignment.CenterVertically)
                        .height(80.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lab.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = lab.shortName,
                                fontSize = 12.sp,
                                color = AppColors.Slate500,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        color = AppColors.Slate100,
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Três mini cards de métricas lado a lado com peso igual (1f = largura igual)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricMini(
                            label = "Temperatura",
                            value = if (lab.tempC <= 0) "—" else "${"%.1f".format(lab.tempC)} °C",
                            modifier = Modifier.weight(1f)
                        )
                        MetricMini(
                            label = "Umidade",
                            value = if (lab.humidityPct <= 0) "—" else "${lab.humidityPct}%",
                            modifier = Modifier.weight(1f)
                        )
                        MetricMini(
                            label = "CO₂",
                            value = if (lab.co2Ppm <= 0) "—" else "${lab.co2Ppm} ppm",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Indicador verde "Coletando" — só aparece se este lab está sendo coletado agora
                        if (lab.isCollecting) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(AppColors.Emerald500)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Coletando",
                                    fontSize = 12.sp,
                                    color = AppColors.Emerald500
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(0.dp))
                        }
                        Text(
                            text = "Atualizado às ${lab.lastUpdate}",
                            fontSize = 11.sp,
                            color = AppColors.Slate400
                        )
                    }
                }
            }
        }
    }
}

// ─── Componentes auxiliares ────────────────────────────────────────────────

// Mini card com rótulo e valor — usado para exibir temperatura, umidade e CO₂ no card da lista
@Composable
fun MetricMini(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.White.copy(alpha = 0.8f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = AppColors.Slate500,
            letterSpacing = 0.3.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.Slate900,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
