package com.ingrid.airqualitymonitor.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ingrid.airqualitymonitor.data.AirQuality
import com.ingrid.airqualitymonitor.data.AirQualityClassifier
import com.ingrid.airqualitymonitor.data.Esp32Status
import com.ingrid.airqualitymonitor.data.Laboratory
import com.ingrid.airqualitymonitor.ui.theme.AppColors
import com.ingrid.airqualitymonitor.ui.viewmodel.LabViewModel

// ─── Tela de detalhes ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(labId: String, labViewModel: LabViewModel, onBack: () -> Unit) {
    val labs by labViewModel.labs.collectAsStateWithLifecycle()
    val esp32Status by labViewModel.esp32Status.collectAsStateWithLifecycle()
    val lab = labs.find { it.id == labId } ?: return

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
            // ── Navegação ──
            FilledTonalButton(
                onClick = onBack,
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = AppColors.White),
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

            Spacer(modifier = Modifier.height(16.dp))

            // ── Card de cabeçalho ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(colors = listOf(AppColors.Slate50, AppColors.White)))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "Última leitura: ${lab.lastUpdate}",
                            fontSize = 12.sp,
                            color = AppColors.Slate500
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = lab.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Slate900
                        )
                        Text(
                            text = lab.shortName,
                            fontSize = 13.sp,
                            color = AppColors.Slate500,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Controle de coleta ──
            CollectionControlCard(
                lab = lab,
                esp32Status = esp32Status,
                onToggle = { labViewModel.toggleCollection(labId) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SensorCard(
                    icon = Icons.Outlined.Thermostat,
                    label = "Temperatura",
                    value = if (lab.tempC <= 0) "—" else "%.1f".format(lab.tempC),
                    unit = "°C",
                    helper = helperText("temp", lab.tempC),
                    helperColor = helperColor("temp", lab.tempC),
                    modifier = Modifier.weight(1f)
                )
                SensorCard(
                    icon = Icons.Outlined.WaterDrop,
                    label = "Umidade",
                    value = if (lab.humidityPct <= 0) "—" else "${lab.humidityPct}",
                    unit = "%",
                    helper = helperText("humidity", lab.humidityPct.toDouble()),
                    helperColor = helperColor("humidity", lab.humidityPct.toDouble()),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            SensorCard(
                icon = Icons.Outlined.Air,
                label = "CO₂",
                value = if (lab.co2Ppm <= 0) "—" else "${lab.co2Ppm}",
                unit = "ppm",
                helper = helperText("co2", lab.co2Ppm.toDouble()),
                helperColor = helperColor("co2", lab.co2Ppm.toDouble()),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            PmUnificadoCard(pm25 = lab.pm25, pm10 = lab.pm10)

            Spacer(modifier = Modifier.height(20.dp))

            // ── Análise do ambiente ──
            SectionCard(title = "Análise do ambiente") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    lab.analysisItems.forEach { item ->
                        Text(
                            text = "– $item",
                            fontSize = 13.sp,
                            color = AppColors.Slate700,
                            lineHeight = 20.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.Slate50)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Recomendação (card escuro) ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.Slate900)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recomendação",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        lab.recommendation.split("\n").forEach { item ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = "•",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            IqarEfeitosCard(pm25 = lab.pm25, pm10 = lab.pm10)

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.Slate50)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = AppColors.Slate400,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(top = 1.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Indicador principal: ABNT NBR 17037:2023. Índice de material particulado: CONAMA 506/24, adaptado (indicador secundário/visual).",
                    fontSize = 11.sp,
                    color = AppColors.Slate400,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// ─── Card unificado de Material Particulado ───────────────────────────────

@Composable
fun PmUnificadoCard(pm25: Double, pm10: Double) {
    val indicePm25 = AirQualityClassifier.calcularIndiceConama(pm25, AirQualityClassifier.faixasPm25Conama)
    val indicePm10 = AirQualityClassifier.calcularIndiceConama(pm10, AirQualityClassifier.faixasPm10Conama)

    // Pior índice entre os dois (maior valor = mais grave)
    val piorIndice: Pair<Int, String>? = when {
        indicePm25 == null && indicePm10 == null -> null
        indicePm25 == null -> indicePm10
        indicePm10 == null -> indicePm25
        indicePm25.first >= indicePm10.first -> indicePm25
        else -> indicePm10
    }
    val nivelAtivo = piorIndice?.let { AirQualityClassifier.nivelDoIndiceConama(it.first) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // PM2.5 e PM10 lado a lado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.Slate100)
                            .padding(6.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Grain,
                            contentDescription = null,
                            tint = AppColors.Slate700,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("PM2.5", fontSize = 12.sp, color = AppColors.Slate500)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (pm25 <= 0) "—" else "%.1f".format(pm25),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Slate900
                        )
                        if (pm25 > 0) {
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "µg/m³",
                                fontSize = 12.sp,
                                color = AppColors.Slate500,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    PmConformanceBadge(valor = pm25, limite = AirQualityClassifier.PM25_RUIM)
                    if (indicePm25 != null) {
                        Spacer(Modifier.height(4.dp))
                        val n = AirQualityClassifier.nivelDoIndiceConama(indicePm25.first)
                        Text(
                            text = "N$n · ${indicePm25.second}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = AirQualityClassifier.corNivelConama(n)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.Slate100)
                            .padding(6.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Grain,
                            contentDescription = null,
                            tint = AppColors.Slate700,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("PM10", fontSize = 12.sp, color = AppColors.Slate500)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (pm10 <= 0) "—" else "%.1f".format(pm10),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Slate900
                        )
                        if (pm10 > 0) {
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "µg/m³",
                                fontSize = 12.sp,
                                color = AppColors.Slate500,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    PmConformanceBadge(valor = pm10, limite = AirQualityClassifier.PM10_RUIM)
                    if (indicePm10 != null) {
                        Spacer(Modifier.height(4.dp))
                        val n = AirQualityClassifier.nivelDoIndiceConama(indicePm10.first)
                        Text(
                            text = "N$n · ${indicePm10.second}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = AirQualityClassifier.corNivelConama(n)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = AppColors.Slate100, thickness = 1.dp)
            Spacer(Modifier.height(12.dp))

            // Índice CONAMA adaptado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("IQAr adaptado", fontSize = 12.sp, color = AppColors.Slate500)
                if (piorIndice != null && nivelAtivo != null) {
                    Text(
                        text = "N$nivelAtivo · ${piorIndice.second}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = corTextoNivelConama(nivelAtivo)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Barra de cor N1–N5
            ConamaColorBar(nivelAtivo)
        }
    }
}

@Composable
private fun PmConformanceBadge(valor: Double, limite: Double) {
    val (bg, text, label) = when {
        valor <= 0   -> Triple(AppColors.Slate100, AppColors.Slate400, "—")
        valor <= limite -> Triple(Color(0xFFD1FAE5), Color(0xFF065F46), "Conforme NBR")
        else         -> Triple(Color(0xFFFFE4E6), Color(0xFF9F1239), "Não conforme NBR")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = label, fontSize = 11.sp, color = text)
    }
}

@Composable
private fun ConamaColorBar(nivelAtivo: Int?) {
    val segmentos = listOf(
        1 to Color(0xFF4CAF50),
        2 to Color(0xFFFFEB3B),
        3 to Color(0xFFFF9800),
        4 to Color(0xFFF44336),
        5 to Color(0xFF9C27B0)
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            segmentos.forEach { (nivel, cor) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(cor)
                        .then(
                            if (nivel == nivelAtivo)
                                Modifier.border(2.dp, AppColors.Slate900, RoundedCornerShape(3.dp))
                            else Modifier
                        )
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("N1", "N2", "N3", "N4", "N5").forEach { r ->
                Text(r, fontSize = 9.sp, color = AppColors.Slate400)
            }
        }
    }
}

private fun corTextoNivelConama(nivel: Int): Color = when (nivel) {
    1    -> Color(0xFF2E7D32)
    2    -> Color(0xFFF57F17)
    3    -> Color(0xFFE65100)
    4    -> Color(0xFFC62828)
    5    -> Color(0xFF6A1B9A)
    else -> AppColors.Slate700
}

// ─── Card de controle de coleta ───────────────────────────────────────────

@Composable
fun CollectionControlCard(lab: Laboratory, esp32Status: Esp32Status, onToggle: () -> Unit) {
    val isCollecting = lab.isCollecting

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val dispositivoOnline = esp32Status != Esp32Status.OFFLINE
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (dispositivoOnline) Color(0xFF10B981) else Color(0xFFF43F5E))
                )
                Text(
                    text = "ESP32 " + if (dispositivoOnline) "online" else "offline",
                    fontSize = 13.sp,
                    color = AppColors.Slate500
                )
            }
            Button(
                onClick = onToggle,
                enabled = dispositivoOnline,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCollecting) Color(0xFFF43F5E) else Color(0xFF10B981),
                    disabledContainerColor = AppColors.Slate400,
                    disabledContentColor = Color.White.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    if (isCollecting) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isCollecting) "Parar coleta" else "Iniciar coleta",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ─── Card de seção genérico ───────────────────────────────────────────────

@Composable
fun SectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = AppColors.Slate900
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = AppColors.Slate500,
                    modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }
            content()
        }
    }
}

// ─── Card de sensor individual ────────────────────────────────────────────

@Composable
fun SensorCard(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    helper: String,
    helperColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppColors.Slate100)
                        .padding(8.dp)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = AppColors.Slate700,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(text = "Agora", fontSize = 10.sp, color = AppColors.Slate400)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = label, fontSize = 12.sp, color = AppColors.Slate500)
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Slate900
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = unit,
                    fontSize = 12.sp,
                    color = AppColors.Slate500,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = helper, fontSize = 11.sp, color = helperColor, lineHeight = 15.sp)
        }
    }
}

// ─── Card de efeitos à saúde (IQAr · Quadro 2 CONAMA 506/24) ─────────────

@Composable
fun IqarEfeitosCard(pm25: Double, pm10: Double) {
    val indicePm25 = AirQualityClassifier.calcularIndiceConama(pm25, AirQualityClassifier.faixasPm25Conama)
    val indicePm10 = AirQualityClassifier.calcularIndiceConama(pm10, AirQualityClassifier.faixasPm10Conama)
    val piorIndice: Pair<Int, String>? = when {
        indicePm25 == null && indicePm10 == null -> null
        indicePm25 == null -> indicePm10
        indicePm10 == null -> indicePm25
        indicePm25.first >= indicePm10.first -> indicePm25
        else -> indicePm10
    }
    val nivelAtivo = piorIndice?.let { AirQualityClassifier.nivelDoIndiceConama(it.first) }

    val niveis = listOf(
        Triple(1, "0 – 40 · N1 Boa",        "Nenhum efeito à saúde esperado."),
        Triple(2, "41 – 80 · N2 Moderada",   "Grupos sensíveis (crianças, idosos e pessoas com doenças respiratórias e cardíacas) podem apresentar tosse seca e cansaço. A população em geral não é afetada."),
        Triple(3, "81 – 120 · N3 Ruim",      "Toda a população pode apresentar tosse seca, cansaço e ardor nos olhos, nariz e garganta. Efeitos mais sérios em grupos sensíveis."),
        Triple(4, "121 – 200 · N4 Muito Ruim","Toda a população pode apresentar agravamento dos sintomas, falta de ar e respiração ofegante. Efeitos ainda mais graves em grupos sensíveis."),
        Triple(5, "> 200 · N5 Péssima",      "Toda a população pode apresentar sérios riscos de doenças respiratórias e cardiovasculares. Aumento de mortes prematuras em grupos sensíveis.")
    )

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
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppColors.Slate100)
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Favorite,
                        contentDescription = null,
                        tint = AppColors.Slate700,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Impactos à Saúde",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = AppColors.Slate900
                    )
                    Text(
                        text = "base IQAr · CONAMA 506/24",
                        fontSize = 11.sp,
                        color = AppColors.Slate400
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            niveis.forEachIndexed { idx, (nivel, faixa, efeito) ->
                val isAtivo = nivel == nivelAtivo
                val cor = AirQualityClassifier.corNivelConama(nivel)

                if (idx > 0) Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isAtivo) cor.copy(alpha = 0.1f) else AppColors.Slate50)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(10.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(if (isAtivo) cor else cor.copy(alpha = 0.35f))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = faixa,
                            fontSize = 11.sp,
                            fontWeight = if (isAtivo) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isAtivo) corTextoNivelConama(nivel) else AppColors.Slate500
                        )
                        if (efeito.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = efeito,
                                fontSize = 12.sp,
                                color = if (isAtivo) AppColors.Slate800 else AppColors.Slate400,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Fonte: Cetesb, 2019",
                fontSize = 10.sp,
                color = AppColors.Slate400
            )
        }
    }
}

// ─── Helpers de texto e cor por parâmetro ────────────────────────────────

fun helperText(param: String, value: Double): String {
    if (value <= 0) return AirQualityClassifier.LABEL_SEM_DADOS
    return when (param) {
        "temp" -> if (value >= AirQualityClassifier.TEMP_BOA_MIN && value <= AirQualityClassifier.TEMP_BOA_MAX)
            "Adequada" else "Não adequada"
        "humidity" -> if (value >= AirQualityClassifier.UMIDADE_BOA_MIN && value <= AirQualityClassifier.UMIDADE_BOA_MAX)
            "Adequada" else "Não adequada"
        "co2" -> if (value <= AirQualityClassifier.CO2_RUIM) "Adequado" else "Não adequado"
        "pm25" -> if (value <= AirQualityClassifier.PM25_RUIM) "Conforme NBR" else "Não conforme NBR"
        "pm10" -> if (value <= AirQualityClassifier.PM10_RUIM) "Conforme NBR" else "Não conforme NBR"
        else -> ""
    }
}

fun helperColor(param: String, value: Double): Color {
    if (value <= 0) return AppColors.Slate400
    return when (param) {
        "temp" -> if (value >= AirQualityClassifier.TEMP_BOA_MIN && value <= AirQualityClassifier.TEMP_BOA_MAX)
            Color(0xFF059669) else Color(0xFFE11D48)
        "humidity" -> if (value >= AirQualityClassifier.UMIDADE_BOA_MIN && value <= AirQualityClassifier.UMIDADE_BOA_MAX)
            Color(0xFF059669) else Color(0xFFE11D48)
        "co2" -> if (value <= AirQualityClassifier.CO2_RUIM) Color(0xFF059669) else Color(0xFFE11D48)
        "pm25" -> if (value <= AirQualityClassifier.PM25_RUIM) Color(0xFF059669) else Color(0xFFE11D48)
        "pm10" -> if (value <= AirQualityClassifier.PM10_RUIM) Color(0xFF059669) else Color(0xFFE11D48)
        else -> AppColors.Slate500
    }
}
