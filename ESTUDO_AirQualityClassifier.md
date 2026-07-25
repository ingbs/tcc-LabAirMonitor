# Estudo: AirQualityClassifier.kt

> Arquivo fonte: `app/src/main/java/com/ingrid/airqualitymonitor/data/AirQualityClassifier.kt`

---

## O que esse arquivo faz (em uma frase)

É o **cérebro da análise de qualidade do ar** do aplicativo — ele recebe os dados brutos dos sensores (CO₂, temperatura, umidade, PM2.5, PM10) e decide: o ar está **conforme** ou **não conforme** com as normas técnicas brasileiras?

---

## Por que existe uma classe só para isso?

Separar a **lógica de classificação** do restante do app é uma boa prática de engenharia de software. Se amanhã a norma mudar (ex: novo limite de CO₂), você altera apenas este arquivo sem precisar caçar lógica espalhada pelo código.

---

## Estrutura geral do arquivo

```
AirQualityClassifier (object)
│
├── Constantes (limites das normas)
│   ├── CO₂ — NBR 17037:2023
│   ├── Temperatura — NBR 17037:2023
│   ├── Umidade — NBR 17037:2023
│   ├── PM2.5 — NBR 17037:2023
│   └── PM10 — NBR 17037:2023
│
├── ClassificationResult (data class) — resultado principal
│
├── classify() — função principal de classificação
│
├── Funções privadas por parâmetro
│   ├── classifyCo2()
│   ├── classifyTemp()
│   ├── classifyHumidity()
│   ├── classifyPm25()
│   └── classifyPm10()
│
├── buildRecommendation() — monta o texto de recomendação
├── buildAnalysisItems() — monta a lista de itens para exibição
│
└── Índice CONAMA 506/24 (indicador visual secundário)
    ├── FaixaConama (data class)
    ├── faixasPm25Conama / faixasPm10Conama (tabelas da norma)
    ├── calcularIndiceConama() — interpolação linear
    ├── corNivelConama() — cor por nível
    └── nivelDoIndiceConama() — converte índice numérico em nível (1–5)
```

---

## O que é `object` em Kotlin?

```kotlin
object AirQualityClassifier { ... }
```

`object` cria um **Singleton** — uma única instância que existe durante toda a vida do app. Faz sentido aqui porque a lógica de classificação não precisa de estado próprio (não guarda dados entre chamadas). Você acessa diretamente: `AirQualityClassifier.classify(...)`.

---

## Parte 1 — As Constantes (limites das normas)

As constantes definem os **Valores Máximos Aceitáveis (VMA)** com base na norma **ABNT NBR 17037:2023** — que trata de qualidade do ar interior em ambientes climatizados.

### CO₂

```kotlin
private const val CO2_BASELINE_EXTERIOR = 420  // ppm — média global (NOAA 2024)
private const val CO2_DIFERENCIAL_NBR   = 700  // ppm — diferencial máximo (NBR 17037 §5.2.1)
const val CO2_RUIM = 420 + 700  // = 1120 ppm
```

**Como entender:** A norma não diz "máximo X ppm". Ela diz: o ar interior não pode ter mais que 700 ppm **acima** do ar exterior. Como o ar exterior tem ~420 ppm (dado do NOAA 2024), o limite resulta em **1120 ppm**.

### Temperatura

```kotlin
const val TEMP_BOA_MIN = 21.0  // °C
const val TEMP_BOA_MAX = 26.0  // °C
```

**Faixa de conforto térmico** segundo a NBR 17037 §5.2.3. Abaixo de 21°C ou acima de 26°C = fora do padrão.

### Umidade Relativa

```kotlin
const val UMIDADE_BOA_MIN = 35  // %
const val UMIDADE_BOA_MAX = 65  // %
```

Abaixo de 35% o ar resseca as mucosas; acima de 65% favorece mofo e proliferação de fungos.

### Material Particulado (poeira)

```kotlin
const val PM25_RUIM = 25.0  // µg/m³
const val PM10_RUIM = 50.0  // µg/m³
```

- **PM2.5**: partículas com diâmetro ≤ 2,5 µm — penetram fundo nos pulmões
- **PM10**: partículas com diâmetro ≤ 10 µm — ficam nas vias aéreas superiores

---

## Parte 2 — O Resultado da Classificação

```kotlin
data class ClassificationResult(
    val quality: AirQuality,          // BOA ou RUIM
    val recommendation: String,       // texto explicando o que fazer
    val analysisItems: List<String>   // lista com os valores de cada sensor
)
```

Essa `data class` é o **envelope** que a função principal devolve. O app Android usa esses três campos para exibir na tela.

O enum `AirQuality` (definido em `Models.kt`) tem apenas dois valores:

| Valor | Significado | Cor |
|-------|-------------|-----|
| `BOA` | Conforme NBR 17037:2023 | Verde |
| `RUIM` | Não conforme NBR 17037:2023 | Vermelho |

---

## Parte 3 — A Função Principal `classify()`

```kotlin
fun classify(co2: Int, temp: Double, humidity: Int, pm25: Double, pm10: Double): ClassificationResult
```

### Como ela funciona (passo a passo)

**Passo 1:** Classifica cada parâmetro individualmente. Se o valor for ≤ 0, considera que o sensor não tem leitura válida (retorna `null`).

```kotlin
val co2Level      = if (co2 > 0) classifyCo2(co2) else null
val tempLevel     = if (temp > 0.0) classifyTemp(temp) else null
// ... e assim por diante
```

**Passo 2:** Filtra apenas os parâmetros com leitura válida.

```kotlin
val validLevels = listOfNotNull(co2Level, tempLevel, humidityLevel, pm25Level, pm10Level)
```

**Passo 3:** Se nenhum sensor tem dado válido, retorna resultado vazio ("—").

```kotlin
if (validLevels.isEmpty()) {
    return ClassificationResult(AirQuality.BOA, LABEL_SEM_DADOS, emptyList())
}
```

**Passo 4:** Aplica a **regra do pior caso** — se *qualquer* parâmetro for RUIM, o resultado geral é RUIM.

```kotlin
quality = validLevels.maxOrNull() ?: AirQuality.BOA
```

> **Por que `maxOrNull()` funciona aqui?**
> O enum `AirQuality` implementa `Comparable` por ordem de declaração. `BOA` vem antes de `RUIM`, então `RUIM > BOA`. Logo, `max()` retorna `RUIM` se houver pelo menos um parâmetro ruim.

**Passo 5:** Monta a recomendação e a lista de itens para exibição.

---

## Parte 4 — As Funções de Classificação por Parâmetro

Cada função aplica uma **regra binária** simples: conforme ou não conforme.

```kotlin
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
```

**Observação importante:** temperatura e umidade têm faixa (mínimo E máximo). CO₂ e material particulado têm apenas limite superior.

---

## Parte 5 — Recomendações e Itens de Análise

### `buildRecommendation()`

Monta um texto em linguagem natural para o usuário. Se tudo estiver bem:
> "Todos os parâmetros estão dentro dos padrões recomendados (ABNT NBR 17037:2023)."

Se algo estiver ruim, monta frases específicas:
- CO₂ alto → "Recomenda-se ventilação do ambiente."
- PM alto → "Recomenda-se higienização das superfícies e verificação da filtragem do ar-condicionado."
- Temperatura fora → "Ajuste a climatização."
- Umidade fora → "Verifique a climatização."

### `buildAnalysisItems()`

Cria uma lista formatada como:
```
CO₂: 950 ppm  (VMA: 1120 ppm)
Temperatura: 28.3°C  (faixa: 21–26°C)
Umidade: 70%  (faixa: 35–65%)
PM2.5: 18.2 µg/m³  (VMA: 25 µg/m³)
PM10: 55.0 µg/m³  (VMA: 50 µg/m³)
```

---

## Parte 6 — Índice CONAMA 506/24 (indicador visual secundário)

Esta segunda parte do arquivo implementa um **índice numérico** baseado na **Resolução CONAMA 506/2024** — padrão nacional de qualidade do ar exterior, adaptado aqui para uso visual.

> **Atenção:** O comentário no código deixa claro que este índice é **secundário e visual** — não substitui a classificação NBR 17037 acima.

### A `data class FaixaConama`

```kotlin
data class FaixaConama(
    val nivel: Int,      // 1 a 5
    val rotulo: String,  // "Boa", "Moderada", "Ruim", "Muito Ruim", "Péssima"
    val cIni: Double,    // concentração inicial da faixa (µg/m³)
    val cFin: Double,    // concentração final da faixa (µg/m³)
    val iIni: Int,       // índice inicial da faixa
    val iFin: Int        // índice final da faixa
)
```

### Tabela de faixas PM2.5

| Nível | Rótulo | Concentração (µg/m³) | Índice |
|-------|--------|----------------------|--------|
| 1 | Boa | 0 – 15 | 0 – 40 |
| 2 | Moderada | 15 – 50 | 41 – 80 |
| 3 | Ruim | 50 – 75 | 81 – 120 |
| 4 | Muito Ruim | 75 – 125 | 121 – 200 |
| 5 | Péssima | 125 – 300 | 201 – 400 |

### `calcularIndiceConama()` — A fórmula de interpolação linear

```kotlin
fun calcularIndiceConama(c: Double, faixas: List<FaixaConama>): Pair<Int, String>?
```

Aplica a **Equação 1 da CONAMA 506/24**:

```
I = I_ini + ((I_fin - I_ini) / (C_fin - C_ini)) × (C - C_ini)
```

**Em palavras:** Dentro da faixa, o índice cresce proporcionalmente à concentração. Se a concentração está exatamente no meio da faixa, o índice estará no meio do intervalo de índices.

**Exemplo:**
- PM2.5 = 30 µg/m³ → faixa "Moderada" (15–50 µg/m³, índice 41–80)
- I = 41 + ((80-41)/(50-15)) × (30-15)
- I = 41 + (39/35) × 15 = 41 + 16.7 ≈ **57**

Retorna `null` quando não há leitura válida (c ≤ 0).

### `corNivelConama()` — Cores do Quadro 1

```kotlin
fun corNivelConama(nivel: Int): Color = when (nivel) {
    1    -> Color(0xFF4CAF50)  // Verde
    2    -> Color(0xFFFFEB3B)  // Amarelo
    3    -> Color(0xFFFF9800)  // Laranja
    4    -> Color(0xFFF44336)  // Vermelho
    5    -> Color(0xFF9C27B0)  // Roxo
    else -> Color.Gray
}
```

As cores seguem exatamente o padrão visual do **Quadro 1 da CONAMA 506/24**.

### `nivelDoIndiceConama()` — Converte índice em nível

```kotlin
fun nivelDoIndiceConama(indice: Int): Int = when {
    indice <= 40  -> 1   // Boa
    indice <= 80  -> 2   // Moderada
    indice <= 120 -> 3   // Ruim
    indice <= 200 -> 4   // Muito Ruim
    else          -> 5   // Péssima
}
```

---

## Resumo: Dois sistemas de classificação no mesmo arquivo

| | NBR 17037:2023 | CONAMA 506/24 (adaptado) |
|---|---|---|
| **Função** | Classificação principal | Indicador visual secundário |
| **Parâmetros** | CO₂, Temp, Umidade, PM2.5, PM10 | Apenas PM2.5 e PM10 |
| **Resultado** | BOA / RUIM (binário) | Índice 0–400 + rótulo (5 níveis) |
| **Norma** | Qualidade do ar interior | Qualidade do ar exterior (adaptado) |
| **Uso no app** | Classificação dos laboratórios | Exibição visual colorida |

---

## Fluxo completo (do sensor à tela)

```
Sensor (ESP32)
     │
     ▼  valores brutos: co2=950, temp=28.3, humidity=70, pm25=18.2, pm10=55.0
AirQualityClassifier.classify()
     │
     ├── classifyCo2(950)      → BOA  (950 < 1120)
     ├── classifyTemp(28.3)    → RUIM (28.3 > 26.0)
     ├── classifyHumidity(70)  → RUIM (70 > 65)
     ├── classifyPm25(18.2)    → BOA  (18.2 < 25.0)
     └── classifyPm10(55.0)    → RUIM (55.0 > 50.0)
     │
     ▼  maxOrNull() → RUIM (pior caso vence)
ClassificationResult(
    quality = RUIM,
    recommendation = "Temperatura fora da faixa...\nUmidade fora...\nMaterial particulado acima...",
    analysisItems = ["CO₂: 950 ppm  (VMA: 1120 ppm)", "Temperatura: 28.3°C  ...", ...]
)
     │
     ▼
Tela do app: card vermelho com recomendações
```

---

## Pontos importantes para citar na apresentação

1. **Embasamento normativo real:** Os limites não são arbitrários — cada constante tem referência à norma (NBR 17037:2023 §5.2.1, §5.2.2, §5.2.3).

2. **Lógica do pior caso:** Se um único parâmetro estiver fora do padrão, o ambiente é classificado como não conforme. Isso é mais seguro do ponto de vista da saúde.

3. **Separação de responsabilidades:** O classificador não sabe como os dados chegaram (Bluetooth? Wi-Fi? Firebase?), apenas classifica. Isso facilita testes e manutenção.

4. **Dois indicadores com propósitos diferentes:** NBR 17037 é a classificação de conformidade; CONAMA 506/24 é um índice visual para dar noção de gradação dentro de cada parâmetro de material particulado.

5. **Tratamento de sensores sem leitura:** Valores ≤ 0 são ignorados, evitando que um sensor desconectado marque o ambiente como ruim indevidamente.
