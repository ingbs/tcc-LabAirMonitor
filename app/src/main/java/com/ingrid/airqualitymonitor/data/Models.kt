package com.ingrid.airqualitymonitor.data

import androidx.compose.ui.graphics.Color

// ─── Enum de qualidade do ar ───────────────────────────────────────────────

enum class AirQuality(
    val label: String,
    val chipColor: Color,
    val chipText: Color,
    val dotColor: Color,
    val cardStart: Color,
    val accentText: Color
) {
    BOA(
        label = "Conforme NBR 17037:2023",
        chipColor = Color(0xFFD1FAE5),
        chipText = Color(0xFF065F46),
        dotColor = Color(0xFF10B981),
        cardStart = Color(0xFFECFDF5),
        accentText = Color(0xFF059669)
    ),
    RUIM(
        label = "Não conforme NBR 17037:2023",
        chipColor = Color(0xFFFFE4E6),
        chipText = Color(0xFF9F1239),
        dotColor = Color(0xFFF43F5E),
        cardStart = Color(0xFFFFF1F2),
        accentText = Color(0xFFE11D48)
    )
}

// ─── Status da ESP32 ──────────────────────────────────────────────────────

enum class Esp32Status {
    OFFLINE,     // Heartbeat não foi recebido há mais de 45 segundos (3 batidas perdidas)
    STANDBY,     // Online, mas não está coletando nenhuma sala
    COLLECTING   // Online e coletando ativamente
}

// ─── Modelo do laboratório ─────────────────────────────────────────────────

data class Laboratory(
    val id: String,
    val name: String,
    val shortName: String,
    val quality: AirQuality,
    val co2Ppm: Int,
    val tempC: Double,
    val humidityPct: Int,
    val pm25: Double,
    val pm10: Double,
    val isCollecting: Boolean,
    val lastUpdate: String,
    val recommendation: String,
    val analysisItems: List<String>
)

// ─── Metadados estáticos dos laboratórios ─────────────────────────────────
// Métricas iniciam em 0 e exibem "—" até o Firebase entregar a primeira leitura real.

val sampleLaboratories = listOf(
    Laboratory(
        id = "lift1", name = "Laboratório de Informática 1", shortName = "LIFT1",
        quality = AirQuality.BOA, co2Ppm = 0, tempC = 0.0, humidityPct = 0,
        pm25 = 0.0, pm10 = 0.0, isCollecting = false, lastUpdate = "--/-- --:--",
        recommendation = "—", analysisItems = emptyList()
    ),
    Laboratory(
        id = "lab1", name = "Laboratório de Eletrônica 1", shortName = "LAB1",
        quality = AirQuality.BOA, co2Ppm = 0, tempC = 0.0, humidityPct = 0,
        pm25 = 0.0, pm10 = 0.0, isCollecting = false, lastUpdate = "--/-- --:--",
        recommendation = "—", analysisItems = emptyList()
    ),
    Laboratory(
        id = "lab2", name = "Laboratório de Eletrônica 2", shortName = "LAB2",
        quality = AirQuality.BOA, co2Ppm = 0, tempC = 0.0, humidityPct = 0,
        pm25 = 0.0, pm10 = 0.0, isCollecting = false, lastUpdate = "--/-- --:--",
        recommendation = "—", analysisItems = emptyList()
    ),
    Laboratory(
        id = "lab3", name = "Laboratório de Eletrônica 3", shortName = "LAB3",
        quality = AirQuality.BOA, co2Ppm = 0, tempC = 0.0, humidityPct = 0,
        pm25 = 0.0, pm10 = 0.0, isCollecting = false, lastUpdate = "--/-- --:--",
        recommendation = "—", analysisItems = emptyList()
    ),
    Laboratory(
        id = "lma", name = "Laboratório de Metodologias Ativas", shortName = "LMA",
        quality = AirQuality.BOA, co2Ppm = 0, tempC = 0.0, humidityPct = 0,
        pm25 = 0.0, pm10 = 0.0, isCollecting = false, lastUpdate = "--/-- --:--",
        recommendation = "—", analysisItems = emptyList()
    ),
    Laboratory(
        id = "labria", name = "Laboratório de Robótica e IA", shortName = "LABRIA",
        quality = AirQuality.BOA, co2Ppm = 0, tempC = 0.0, humidityPct = 0,
        pm25 = 0.0, pm10 = 0.0, isCollecting = false, lastUpdate = "--/-- --:--",
        recommendation = "—", analysisItems = emptyList()
    ),
    Laboratory(
        id = "tutoria", name = "Sala de Tutoria", shortName = "Tutoria",
        quality = AirQuality.BOA, co2Ppm = 0, tempC = 0.0, humidityPct = 0,
        pm25 = 0.0, pm10 = 0.0, isCollecting = false, lastUpdate = "--/-- --:--",
        recommendation = "—", analysisItems = emptyList()
    )
)