package com.ingrid.airqualitymonitor.data

import androidx.compose.ui.graphics.Color

object AirQualityClassifier {

    // ── Labels ────────────────────────────────────────────────────────────────
    const val LABEL_SEM_DADOS = "—"

    // ── CO₂ — NBR 17037:2023 §5.2.1 ────────────────────────────────────────
    // VMA = concentração exterior de referência + diferencial máximo permitido
    private const val CO2_BASELINE_EXTERIOR = 420  // ppm — média atmosférica global (NOAA 2024)
    private const val CO2_DIFERENCIAL_NBR   = 700  // ppm — diferencial máximo (NBR 17037:2023 §5.2.1)
    const val CO2_RUIM = CO2_BASELINE_EXTERIOR + CO2_DIFERENCIAL_NBR  // 1120 ppm

    // ── Temperatura — NBR 17037:2023 §5.2.3 ─────────────────────────────────
    const val TEMP_BOA_MIN = 21.0  // °C — limite inferior da faixa de conforto
    const val TEMP_BOA_MAX = 26.0  // °C — limite superior da faixa de conforto

    // ── Umidade relativa — NBR 17037:2023 §5.2.3 ────────────────────────────
    const val UMIDADE_BOA_MIN = 35  // %
    const val UMIDADE_BOA_MAX = 65  // %

    // ── PM2.5 — NBR 17037:2023 §5.2.2 ──────────────────────────────────────
    const val PM25_RUIM = 25.0  // µg/m³ — VMA (NBR 17037:2023)

    // ── PM10 — NBR 17037:2023 §5.2.2 ────────────────────────────────────────
    const val PM10_RUIM = 50.0  // µg/m³ — VMA (NBR 17037:2023)

    data class ClassificationResult(
        val quality: AirQuality,
        val recommendation: String,
        val analysisItems: List<String>
    )

    /**
     * Classifica a qualidade do ar segundo os VMAs da NBR 17037:2023 (regra binária).
     * Parâmetros com valor <= 0 são considerados sem leitura válida e excluídos da análise.
     * Retorna resultado vazio ("—") quando nenhum parâmetro tem leitura válida.
     */
    fun classify(co2: Int, temp: Double, humidity: Int, pm25: Double, pm10: Double): ClassificationResult {
        val co2Level      = if (co2 > 0) classifyCo2(co2) else null
        val tempLevel     = if (temp > 0.0) classifyTemp(temp) else null
        val humidityLevel = if (humidity > 0) classifyHumidity(humidity) else null
        val pm25Level     = if (pm25 > 0.0) classifyPm25(pm25) else null
        val pm10Level     = if (pm10 > 0.0) classifyPm10(pm10) else null

        val validLevels = listOfNotNull(co2Level, tempLevel, humidityLevel, pm25Level, pm10Level)

        if (validLevels.isEmpty()) {
            return ClassificationResult(AirQuality.BOA, LABEL_SEM_DADOS, emptyList())
        }

        return ClassificationResult(
            quality        = validLevels.maxOrNull() ?: AirQuality.BOA,
            recommendation = buildRecommendation(co2Level, tempLevel, humidityLevel, pm25Level, pm10Level),
            analysisItems  = buildAnalysisItems(co2, co2Level, temp, tempLevel, humidity, humidityLevel, pm25, pm25Level, pm10, pm10Level)
        )
    }

    // ── Classificação binária por parâmetro (Conforme / Não conforme) ─────────

    private fun classifyCo2(co2: Int): AirQuality =
        if (co2 > CO2_RUIM) AirQuality.RUIM else AirQuality.BOA

    private fun classifyTemp(temp: Double): AirQuality =
        if (temp < TEMP_BOA_MIN || temp > TEMP_BOA_MAX) AirQuality.RUIM else AirQuality.BOA

    private fun classifyHumidity(humidity: Int): AirQuality =
        if (humidity < UMIDADE_BOA_MIN || humidity > UMIDADE_BOA_MAX) AirQuality.RUIM else AirQuality.BOA

    private fun classifyPm25(pm25: Double): AirQuality =
        if (pm25 > PM25_RUIM) AirQuality.RUIM else AirQuality.BOA

    private fun classifyPm10(pm10: Double): AirQuality =
        if (pm10 > PM10_RUIM) AirQuality.RUIM else AirQuality.BOA

    private fun buildRecommendation(
        co2: AirQuality?, temp: AirQuality?, humidity: AirQuality?,
        pm25: AirQuality?, pm10: AirQuality?
    ): String {
        val all = listOfNotNull(co2, temp, humidity, pm25, pm10)
        if (all.all { it == AirQuality.BOA }) {
            return "Todos os parâmetros estão dentro dos padrões recomendados (ABNT NBR 17037:2023)."
        }
        val parts = mutableListOf<String>()
        if (co2 == AirQuality.RUIM)
            parts.add("Nível de CO₂ acima do VMA. Recomenda-se ventilação do ambiente.")
        if (pm25 == AirQuality.RUIM || pm10 == AirQuality.RUIM)
            parts.add("Concentração de material particulado acima do VMA (NBR 17037). Recomenda-se higienização das superfícies e verificação da filtragem do ar-condicionado (NBR 16401-3).")
        if (temp == AirQuality.RUIM)
            parts.add("Temperatura fora da faixa de conforto. Ajuste a climatização.")
        if (humidity == AirQuality.RUIM)
            parts.add("Umidade fora da faixa recomendada. Verifique a climatização.")
        return parts.joinToString("\n")
    }

    private fun buildAnalysisItems(
        co2: Int, co2Level: AirQuality?,
        temp: Double, tempLevel: AirQuality?,
        humidity: Int, humidityLevel: AirQuality?,
        pm25: Double, pm25Level: AirQuality?,
        pm10: Double, pm10Level: AirQuality?
    ): List<String> {
        val items = mutableListOf<String>()
        if (co2 > 0)       items.add("CO₂: $co2 ppm  (VMA: $CO2_RUIM ppm)")
        if (temp > 0.0)    items.add("Temperatura: ${"%.1f".format(temp)}°C  (faixa: ${TEMP_BOA_MIN.toInt()}–${TEMP_BOA_MAX.toInt()}°C)")
        if (humidity > 0)  items.add("Umidade: $humidity%  (faixa: $UMIDADE_BOA_MIN–$UMIDADE_BOA_MAX%)")
        if (pm25 > 0.0)    items.add("PM2.5: ${"%.1f".format(pm25)} µg/m³  (VMA: ${PM25_RUIM.toInt()} µg/m³)")
        if (pm10 > 0.0)    items.add("PM10: ${"%.1f".format(pm10)} µg/m³  (VMA: ${PM10_RUIM.toInt()} µg/m³)")
        return items
    }

    // ── CONAMA 506/24 — índice ADAPTADO (indicador SECUNDÁRIO/visual) ─────────
    // Restrito a material particulado. Não substitui a classificação NBR 17037 acima.

    data class FaixaConama(
        val nivel: Int,
        val rotulo: String,
        val cIni: Double,
        val cFin: Double,
        val iIni: Int,
        val iFin: Int
    )

    // MP2,5 — canal Vout2 do DSM501A (proxy, fração estimada ≥ 1 µm)
    val faixasPm25Conama = listOf(
        FaixaConama(1, "Boa",         0.0,  15.0,   0,  40),
        FaixaConama(2, "Moderada",   15.0,  50.0,  41,  80),
        FaixaConama(3, "Ruim",       50.0,  75.0,  81, 120),
        FaixaConama(4, "Muito Ruim", 75.0, 125.0, 121, 200),
        FaixaConama(5, "Péssima",   125.0, 300.0, 201, 400)
    )

    // MP10 — canal Vout1 do DSM501A (proxy, fração grossa ≥ 2.5 µm;
    // não equivale ao PM10 normativo — limitação instrumental documentada no TCC)
    val faixasPm10Conama = listOf(
        FaixaConama(1, "Boa",          0.0,  40.0,   0,  40),
        FaixaConama(2, "Moderada",    40.0, 100.0,  41,  80),
        FaixaConama(3, "Ruim",       100.0, 150.0,  81, 120),
        FaixaConama(4, "Muito Ruim", 150.0, 250.0, 121, 200),
        FaixaConama(5, "Péssima",    250.0, 600.0, 201, 400)
    )

    /**
     * Índice ADAPTADO da CONAMA 506/24 (Equação 1, interpolação linear).
     * Retorna null quando c <= 0 — deve ser exibido como "—", nunca como "Boa".
     */
    fun calcularIndiceConama(c: Double, faixas: List<FaixaConama>): Pair<Int, String>? {
        if (c <= 0.0) return null
        val faixa = faixas.firstOrNull { c > it.cIni && c <= it.cFin }
            ?: faixas.last()
        val i = faixa.iIni +

                ((faixa.iFin - faixa.iIni).toDouble() / (faixa.cFin - faixa.cIni)) *
                (c - faixa.cIni)
        return i.toInt().coerceIn(0, 400) to faixa.rotulo
    }

    // Cores do Quadro 1 (CONAMA 506/24)
    fun corNivelConama(nivel: Int): Color = when (nivel) {
        1    -> Color(0xFF4CAF50)
        2    -> Color(0xFFFFEB3B)
        3    -> Color(0xFFFF9800)
        4    -> Color(0xFFF44336)
        5    -> Color(0xFF9C27B0)
        else -> Color.Gray
    }

    fun nivelDoIndiceConama(indice: Int): Int = when {
        indice <= 40  -> 1
        indice <= 80  -> 2
        indice <= 120 -> 3
        indice <= 200 -> 4
        else          -> 5
    }
}