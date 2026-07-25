# Documentação Técnica — Lab Air Monitor

> **Finalidade deste documento:** explicar toda a lógica, arquitetura e implementação do aplicativo Android Lab Air Monitor, de forma detalhada, para estudo e apresentação do projeto.

---

## Sumário

1. [Visão Geral do Projeto](#1-visão-geral-do-projeto)
2. [Tecnologias Utilizadas](#2-tecnologias-utilizadas)
3. [Arquitetura do App](#3-arquitetura-do-app)
4. [Estrutura de Pastas](#4-estrutura-de-pastas)
5. [Configuração do Projeto (Gradle)](#5-configuração-do-projeto-gradle)
6. [Firebase — Banco de Dados em Tempo Real](#6-firebase--banco-de-dados-em-tempo-real)
7. [Modelos de Dados (Models.kt)](#7-modelos-de-dados-modelskt)
8. [ViewModel — Cérebro do App (LabViewModel.kt)](#8-viewmodel--cérebro-do-app-labviewmodelkt)
9. [Tela Principal — HomeScreen.kt](#9-tela-principal--homescreenkt)
10. [Tela de Detalhes — DetailsScreen.kt](#10-tela-de-detalhes--detailsscreenkt)
11. [Navegação entre Telas (MainActivity.kt)](#11-navegação-entre-telas-mainactivitykt)
12. [Identidade Visual — Tema e Cores](#12-identidade-visual--tema-e-cores)
13. [Norma Técnica ABNT NBR 17037:2023](#13-norma-técnica-abnt-nbr-170372023)
14. [Fluxo Completo de Dados](#14-fluxo-completo-de-dados)
15. [Diagrama de Componentes](#15-diagrama-de-componentes)

---

## 1. Visão Geral do Projeto

O **Lab Air Monitor** é um aplicativo Android desenvolvido para monitorar em tempo real a **qualidade do ar de laboratórios** de uma instituição de ensino. O app se conecta ao Firebase para receber dados coletados por um **dispositivo ESP32** com sensores ambientais instalados fisicamente nos laboratórios.

### O que o app faz:
- Exibe uma lista com todos os laboratórios monitorados
- Mostra as leituras atuais de cada ambiente: CO₂, temperatura, umidade, PM2.5 e PM10
- Classifica a qualidade do ar como **Boa**, **Moderada** ou **Ruim**
- Gera recomendações automáticas com base na norma brasileira **ABNT NBR 17037:2023**
- Permite iniciar e parar a coleta de dados diretamente pelo app
- Exibe o status do dispositivo ESP32 (online/offline)

### Laboratórios monitorados:
| ID | Nome Completo | Sigla |
|---|---|---|
| lift1 | Laboratório de Informática 1 | LIFT1 |
| lab1 | Laboratório de Eletrônica 1 | LAB1 |
| lab2 | Laboratório de Eletrônica 2 | LAB2 |
| lab3 | Laboratório de Eletrônica 3 | LAB3 |
| lma | Laboratório de Metodologias Ativas | LMA |
| labria | Laboratório de Robótica e IA | LABRIA |
| tutoria | Sala de Tutoria | Tutoria |

---

## 2. Tecnologias Utilizadas

| Tecnologia | Versão | Para que serve |
|---|---|---|
| **Kotlin** | 2.2.10 | Linguagem de programação |
| **Jetpack Compose** | BOM 2026.02.01 | Framework de UI declarativa |
| **Navigation Compose** | 2.8.5 | Navegação entre telas |
| **ViewModel + StateFlow** | 2.8.7 | Gerenciamento de estado |
| **Firebase Realtime Database** | 21.0.0 | Banco de dados em tempo real |
| **Material 3** | via BOM | Componentes visuais |
| **Material Icons Extended** | via BOM | Ícones dos sensores |
| **Min SDK** | 24 (Android 7.0) | Versão mínima do Android |
| **Target SDK** | 36 (Android 15) | Versão alvo |

---

## 3. Arquitetura do App

O app segue a arquitetura **MVVM (Model-View-ViewModel)**, que é o padrão recomendado pelo Google para apps Android modernos.

```
┌─────────────────────────────────────────────────┐
│                      VIEW                        │
│   HomeScreen.kt          DetailsScreen.kt        │
│   (Lista de labs)        (Detalhes do lab)       │
└────────────────────┬────────────────────────────┘
                     │ observa StateFlow
┌────────────────────▼────────────────────────────┐
│                  VIEWMODEL                       │
│              LabViewModel.kt                     │
│   - Gerencia estado (labs, isLoading, esp32)     │
│   - Escuta o Firebase em tempo real              │
│   - Gera recomendações automáticas               │
└────────────────────┬────────────────────────────┘
                     │ lê/escreve dados
┌────────────────────▼────────────────────────────┐
│                    MODEL                         │
│   Models.kt             Firebase Realtime DB     │
│   (Data classes,        (Dados dos sensores      │
│    Enums, Mock data)     e do dispositivo)       │
└─────────────────────────────────────────────────┘
```

### Por que MVVM?
- A **ViewModel** não é destruída quando a tela gira (rotação do celular), preservando os dados
- Separa a lógica de negócio da interface, tornando o código mais organizado
- O **StateFlow** notifica automaticamente a UI quando os dados mudam, sem precisar atualizar manualmente

---

## 4. Estrutura de Pastas

```
LabAirMonitor/
├── app/
│   ├── src/main/
│   │   ├── java/com/ingrid/airqualitymonitor/
│   │   │   ├── MainActivity.kt           ← Ponto de entrada + Navegação
│   │   │   ├── data/
│   │   │   │   └── Models.kt             ← Modelos de dados e dados mock
│   │   │   └── ui/
│   │   │       ├── screens/
│   │   │       │   ├── HomeScreen.kt     ← Tela da lista de laboratórios
│   │   │       │   └── DetailsScreen.kt  ← Tela de detalhes do laboratório
│   │   │       ├── theme/
│   │   │       │   ├── Color.kt          ← Paleta de cores do app
│   │   │       │   ├── Theme.kt          ← Tema Material 3
│   │   │       │   └── Type.kt           ← Tipografia
│   │   │       └── viewmodel/
│   │   │           └── LabViewModel.kt   ← Lógica e estado do app
│   │   ├── res/
│   │   │   ├── values/strings.xml        ← Nome do app
│   │   │   └── values/themes.xml         ← Tema base sem ActionBar
│   │   └── AndroidManifest.xml           ← Permissão de internet + Activity
│   ├── build.gradle.kts                  ← Dependências do app
│   └── google-services.json             ← Configuração do Firebase
├── build.gradle.kts                      ← Plugins globais
├── settings.gradle.kts                   ← Repositórios e nome do projeto
└── gradle/libs.versions.toml            ← Catálogo de versões das libs
```

---

## 5. Configuração do Projeto (Gradle)

O projeto usa o **Kotlin DSL** para o Gradle (arquivos `.kts` em vez de `.gradle`), que é a forma moderna e mais segura de configurar projetos Android.

### `gradle/libs.versions.toml` — Catálogo de versões
Este arquivo centraliza todas as versões das bibliotecas em um único lugar. Assim, se precisar atualizar uma biblioteca, muda em apenas um lugar.

```toml
[versions]
agp = "9.1.1"          # Android Gradle Plugin
kotlin = "2.2.10"      # Kotlin
composeBom = "2026.02.01"  # Compose Bill of Materials (agrupa versões compatíveis)
```

### `app/build.gradle.kts` — Dependências principais

```kotlin
// Firebase (plataforma + banco de dados)
implementation(platform("com.google.firebase:firebase-bom:34.12.0"))
implementation("com.google.firebase:firebase-database-ktx:21.0.0")

// Navegação entre telas
implementation("androidx.navigation:navigation-compose:2.8.5")

// ViewModel integrado ao Compose
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
```

### `AndroidManifest.xml` — Permissões
O app precisa apenas de **uma permissão**: acesso à internet, para se comunicar com o Firebase.

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

Não há permissões de Bluetooth, sensores ou localização porque o app **não acessa hardware diretamente** — quem faz isso é o ESP32.

---

## 6. Firebase — Banco de Dados em Tempo Real

### O que é o Firebase Realtime Database?
É um banco de dados **NoSQL em nuvem** da Google. Os dados são armazenados como um grande JSON e qualquer cliente conectado recebe atualizações automaticamente quando algo muda — é como um "broadcast" em tempo real.

### Estrutura dos dados no Firebase

```json
{
  "dispositivo": {
    "coletando": true,
    "sala_selecionada": "lab1",
    "ultimo_heartbeat": 1748456789
  },
  "salas": {
    "lab1": {
      "nome": "Laboratório de Eletrônica 1",
      "ultima_leitura": {
        "co2": 920,
        "temperatura": 29.0,
        "umidade": 58,
        "pm2_5": 14.0,
        "pm10": 22.5,
        "qualidade_ar": "moderada",
        "recomendacao": "Melhorar ventilação.",
        "timestamp": 1748456789
      }
    },
    "lift1": { ... },
    "lab2": { ... }
  }
}
```

### Como o app lê esses dados?

O app usa **ValueEventListener** — um "escutador" que é chamado automaticamente toda vez que o dado muda no banco:

```kotlin
db.child("salas").addValueEventListener(object : ValueEventListener {
    override fun onDataChange(snapshot: DataSnapshot) {
        // Chamado IMEDIATAMENTE quando qualquer sala é atualizada
        // snapshot contém todos os dados atualizados
    }
    override fun onCancelled(error: DatabaseError) { }
})
```

### Nó `/dispositivo` — controle do ESP32
Este nó guarda o estado do dispositivo coletor:
- `coletando` (Boolean): se o ESP32 está coletando dados agora
- `sala_selecionada` (String): qual laboratório está sendo coletado
- `ultimo_heartbeat` (Long): timestamp Unix da última vez que o ESP32 "bateu ponto"

O **heartbeat** é um mecanismo de verificação de vida: o ESP32 atualiza este campo periodicamente. Se o app perceber que o heartbeat não foi atualizado há mais de 30 segundos, considera o dispositivo **offline**.

---

## 7. Modelos de Dados (Models.kt)

Este arquivo define as estruturas de dados do app. É a "língua" que o app usa para representar as informações.

### Enum `AirQuality` — Qualidade do ar

```kotlin
enum class AirQuality(
    val label: String,
    val chipColor: Color,   // Cor do badge (ex: fundo verde)
    val chipText: Color,    // Cor do texto no badge
    val dotColor: Color,    // Cor do indicador circular
    val cardStart: Color,   // Cor do gradiente do card
    val accentText: Color   // Cor de destaque para textos
) {
    BOA      // Verde  — CO₂ baixo, temp e umidade adequados
    MODERADA // Amarelo — algum parâmetro fora do ideal
    RUIM     // Vermelho — parâmetro acima do VMA da NBR
}
```

**Por que o enum carrega as cores?** Porque assim, a interface só precisa perguntar `lab.quality.chipColor` para saber qual cor usar, sem precisar de `if/else` espalhados pela tela. Cada estado carrega sua própria identidade visual.

### Enum `Esp32Status` — Estado do dispositivo

```kotlin
enum class Esp32Status {
    OFFLINE,     // Heartbeat > 30 segundos atrás
    STANDBY,     // Online mas não coletando
    COLLECTING   // Online e coletando ativamente
}
```

### Data class `Laboratory` — Laboratório

```kotlin
data class Laboratory(
    val id: String,            // Chave única (ex: "lab1")
    val name: String,          // Nome completo
    val shortName: String,     // Sigla (ex: "LAB1")
    val quality: AirQuality,   // Estado geral da qualidade
    val co2Ppm: Int,           // CO₂ em partes por milhão
    val tempC: Double,         // Temperatura em °C
    val humidityPct: Int,      // Umidade em porcentagem
    val pm25: Double,          // Partículas finas (µg/m³)
    val pm10: Double,          // Partículas grossas (µg/m³)
    val isCollecting: Boolean, // Se este lab está sendo coletado agora
    val lastUpdate: String,    // Hora da última leitura (ex: "14:28")
    val recommendation: String,         // Recomendação resumida
    val analysisItems: List<String>,    // Lista de análises detalhadas
    val recentAlerts: List<String>,     // Alertas das últimas horas
    val historyValues: List<Float>      // Dados para o gráfico
)
```

### Por que existe `sampleLaboratories`?
São dados estáticos que servem como **estado inicial** da tela. Quando o app abre, a lista já tem dados para mostrar enquanto o Firebase carrega os dados reais. Isso melhora a experiência do usuário, que não vê uma tela vazia.

---

## 8. ViewModel — Cérebro do App (LabViewModel.kt)

A `LabViewModel` é o componente mais importante do app. Ela gerencia todo o estado, se comunica com o Firebase e aplica as regras de negócio.

### Estado exposto para a UI

```kotlin
// Lista de laboratórios (começa com dados mock, é substituída pelo Firebase)
private val _labs = MutableStateFlow<List<Laboratory>>(sampleLaboratories)
val labs: StateFlow<List<Laboratory>> = _labs

// Indicador de carregamento
private val _isLoading = MutableStateFlow(false)
val isLoading: StateFlow<Boolean> = _isLoading

// Status do ESP32
private val _esp32Status = MutableStateFlow(Esp32Status.OFFLINE)
val esp32Status: StateFlow<Esp32Status> = _esp32Status
```

**Por que `MutableStateFlow` vs `StateFlow`?**
- `MutableStateFlow` é privado: só a ViewModel pode alterar o valor
- `StateFlow` é o que a UI recebe: somente leitura
- Esse padrão garante que a UI nunca modifique o estado diretamente (princípio de fonte única de verdade)

### Inicialização — `init {}`

```kotlin
init {
    listenToDispositivo()  // Escuta mudanças no nó /dispositivo
    listenToSalas()        // Escuta mudanças em todas as salas
    listenToHeartbeat()    // Escuta o timestamp do heartbeat
    startHeartbeatChecker() // Verifica se o heartbeat ainda é recente
}
```

Tudo começa quando a ViewModel é criada. Os quatro métodos configuram os "escutadores" que mantêm o app atualizado.

### `listenToDispositivo()` — Escutando o estado do hardware

```kotlin
private fun listenToDispositivo() {
    db.child("dispositivo").addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            coletando = snapshot.child("coletando").getValue(Boolean::class.java) ?: false
            salaSelecionada = snapshot.child("sala_selecionada").getValue(String::class.java) ?: ""
            
            // Atualiza o isCollecting de cada laboratório
            _labs.value = _labs.value.map { lab ->
                lab.copy(isCollecting = coletando && salaSelecionada == lab.id)
            }
            updateEsp32Status()
        }
        override fun onCancelled(error: DatabaseError) {}
    })
}
```

Quando o ESP32 atualiza `/dispositivo/coletando` ou `/dispositivo/sala_selecionada`, este listener é chamado. Ele recalcula o campo `isCollecting` de cada laboratório — somente o laboratório com o mesmo `id` que `sala_selecionada` fica com `isCollecting = true`.

### `listenToHeartbeat()` + `startHeartbeatChecker()` — Detectando se o ESP32 está vivo

```kotlin
private fun listenToHeartbeat() {
    db.child("dispositivo").child("ultimo_heartbeat")
        .addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                lastHeartbeat = snapshot.getValue(Long::class.java) ?: 0L
                updateEsp32Status()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
}

private fun updateEsp32Status() {
    val nowSeconds = System.currentTimeMillis() / 1000L
    _esp32Status.value = when {
        nowSeconds - lastHeartbeat > 30 -> Esp32Status.OFFLINE    // Sem sinal há mais de 30s
        coletando                        -> Esp32Status.COLLECTING  // Online e coletando
        else                             -> Esp32Status.STANDBY     // Online mas parado
    }
}

private fun startHeartbeatChecker() {
    viewModelScope.launch {
        while (true) {
            delay(5_000L)      // Verifica a cada 5 segundos
            updateEsp32Status()
        }
    }
}
```

O heartbeat resolve um problema: e se o ESP32 travar e parar de enviar dados, mas o Firebase ainda tiver o último valor de `coletando = true`? O app ficaria mostrando "coletando" para sempre. Com o heartbeat, se o ESP32 não se manifestar em 30 segundos, o app sabe que ele está offline.

O `startHeartbeatChecker()` roda numa coroutine infinita, verificando a cada 5 segundos — mesmo sem novas atualizações do Firebase.

### `listenToSalas()` — Lendo dados dos sensores

```kotlin
private fun listenToSalas() {
    _isLoading.value = true
    db.child("salas").addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val updated = mutableListOf<Laboratory>()
            snapshot.children.forEach { salaSnap ->
                parseSala(salaSnap)?.let { updated.add(it) }
            }
            if (updated.isNotEmpty()) _labs.value = updated
            _isLoading.value = false
        }
        override fun onCancelled(error: DatabaseError) {
            _isLoading.value = false
        }
    })
}
```

Este listener fica escutando **toda a árvore `/salas`**. Quando qualquer sensor atualiza uma leitura, este método é chamado, converte os dados crus do Firebase em objetos `Laboratory` e atualiza o estado.

### `parseSala()` — Convertendo dados do Firebase em objetos Kotlin

```kotlin
private fun parseSala(snap: DataSnapshot): Laboratory? {
    return try {
        val id   = snap.key ?: return null
        val nome = snap.child("nome").getValue(String::class.java) ?: id
        val l    = snap.child("ultima_leitura")

        val co2     = l.child("co2").getValue(Int::class.java) ?: 0
        val temp    = l.child("temperatura").getValue(Double::class.java) ?: 0.0
        val umidade = l.child("umidade").getValue(Int::class.java) ?: 0
        val pm25    = l.child("pm2_5").getValue(Double::class.java) ?: 0.0
        val pm10    = l.child("pm10").getValue(Double::class.java) ?: 0.0
        val qualStr = l.child("qualidade_ar").getValue(String::class.java) ?: "boa"
        val recomFB = l.child("recomendacao").getValue(String::class.java) ?: ""
        val timestamp = l.child("timestamp").getValue(Long::class.java) ?: 0L

        val quality = parseQuality(qualStr)
        val result  = gerarRecomendacoes(co2, pm25, pm10, temp, umidade)

        Laboratory(
            id             = id,
            name           = nome,
            quality        = quality,
            co2Ppm         = co2,
            // ... demais campos
            recommendation = recomFB.ifBlank { summaryRecommendation(quality) },
            analysisItems  = result.recomendacoes,
            recentAlerts   = result.alertas.ifEmpty { listOf("Sem alertas recentes.") }
        )
    } catch (e: Exception) {
        null  // Se qualquer campo falhar, ignora este laboratório
    }
}
```

O `try/catch` aqui é importante: se um laboratório tiver dados malformados no Firebase, ele é ignorado sem quebrar a lista inteira.

### `toggleCollection()` — Controlando o ESP32 pelo app

```kotlin
fun toggleCollection(labId: String) {
    val jaColetandoEsse = coletando && salaSelecionada == labId
    if (jaColetandoEsse) {
        // Se já está coletando este lab, para a coleta
        db.child("dispositivo").child("coletando").setValue(false)
    } else {
        // Se não está coletando (ou está coletando outro), inicia neste lab
        db.child("dispositivo").child("sala_selecionada").setValue(labId)
        db.child("dispositivo").child("coletando").setValue(true)
    }
}
```

Este método **escreve no Firebase**, e o ESP32 fica escutando essa mesma chave. Quando o app muda `coletando` para `true`, o ESP32 lê isso e começa a coletar dados do laboratório indicado em `sala_selecionada`.

### `gerarRecomendacoes()` — Análise automática baseada na NBR 17037:2023

Este é o método que implementa a inteligência do app. Ele analisa cada parâmetro e gera recomendações textuais:

```kotlin
private fun gerarRecomendacoes(co2: Int, pm25: Double, pm10: Double, temp: Double, umidade: Int): RecomendacoesResult {
    val recomendacoes = mutableListOf<String>()
    val alertas = mutableListOf<String>()

    // CO₂ — VMA da NBR 17037:2023 é 1100 ppm
    when {
        co2 > 1100 -> {
            alertas.add("CO₂ acima do VMA de 1100 ppm (NBR 17037 §5.2.1).")
            recomendacoes.add("Aumentar imediatamente a renovação de ar...")
        }
        co2 > 800 -> {
            recomendacoes.add("CO₂ em elevação. Abrir janelas...")
        }
    }

    // PM2.5 — VMA é 25 µg/m³
    // PM10 — VMA é 50 µg/m³
    // Temperatura — faixa de conforto: 21°C a 26°C
    // Umidade — faixa ideal: 35% a 65%
    // ...

    return RecomendacoesResult(recomendacoes, alertas)
}
```

VMA = **Valor Máximo Admissível** — limite que, se ultrapassado, representa risco à saúde.

---

## 9. Tela Principal — HomeScreen.kt

A HomeScreen exibe a lista de todos os laboratórios. É construída com **LazyColumn**, que renderiza apenas os cards visíveis na tela (como um RecyclerView, mas declarativo).

### Estrutura da tela

```
┌────────────────────────────────────┐
│  Monitoramento                     │  ← Cabeçalho
│  Qualidade do Ar                   │
├────────────────────────────────────┤
│  ● ESP32  ON  · Dispositivo conect │  ← Status do hardware
├────────────────────────────────────┤
│  AMBIENTES MONITORADOS          ⟳  │  ← Label + loading
├────────────────────────────────────┤
│ ┌──────────────────────────────┐   │
│ │ Lab de Eletrônica 1    Ruim  │   │  ← Card de cada laboratório
│ │ LAB1                         │   │
│ │ Temp    Umidade    CO₂       │   │
│ │ 29°C    58%        920 ppm   │   │
│ │ ● Coletando   Atualizado 14h │   │
│ └──────────────────────────────┘   │
│  ... (outros labs)                 │
└────────────────────────────────────┘
```

### Animação dos cards — `AnimatedLabCard`

```kotlin
@Composable
fun AnimatedLabCard(lab: Laboratory, index: Int, onClick: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(index * 80L)  // O card 0 aparece imediatamente
        visible = true       // O card 1 aparece após 80ms
    }                        // O card 2 aparece após 160ms, etc.

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
    ) {
        // ... conteúdo do card
    }
}
```

Isso cria um efeito de entrada em cascata: os cards aparecem um por um com um pequeno atraso, tornando a transição mais suave e agradável.

### Gradiente horizontal do card

```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .background(
            Brush.horizontalGradient(
                colors = listOf(style.cardStart, AppColors.White)
            )
        )
)
```

Cada card tem um gradiente que vai da cor do status (verde, amarelo ou vermelho claro) até o branco, reforçando visualmente a qualidade do ar.

### Componente `QualityChip`

```kotlin
@Composable
fun QualityChip(quality: AirQuality) {
    Surface(
        shape = RoundedCornerShape(50.dp),  // Pill shape
        color = quality.chipColor,
        border = BorderStroke(1.dp, quality.chipColor.copy(alpha = 0.5f))
    ) {
        Text(
            text = when (quality) {
                AirQuality.BOA -> "Boa"
                AirQuality.MODERADA -> "Moderada"
                AirQuality.RUIM -> "Ruim"
            },
            color = quality.chipText
        )
    }
}
```

### Componente `MetricMini`

Exibe cada métrica (temperatura, umidade, CO₂) num pequeno quadrado dentro do card:

```kotlin
@Composable
fun MetricMini(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.White.copy(alpha = 0.8f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(text = label, fontSize = 10.sp, color = AppColors.Slate500)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
```

---

## 10. Tela de Detalhes — DetailsScreen.kt

A DetailsScreen exibe todas as informações de um único laboratório, com muitos mais detalhes que o card da HomeScreen.

### Estrutura da tela

```
┌────────────────────────────────────┐
│  ← Voltar                          │  ← Botão de navegação
├────────────────────────────────────┤
│  Boa            Última leitura 14h │  ← Header com status
│  Laboratório de Eletrônica 2       │
│  LAB2                              │
├────────────────────────────────────┤
│  [ Iniciar coleta  ▶ ]             │  ← Controle do ESP32
├────────────────────────────────────┤
│  LEITURAS ATUAIS                   │
│  ┌─────────┐ ┌─────────┐          │  ← Grid de sensores
│  │ 24.5°C  │ │  55%    │          │
│  │  Temp   │ │ Umidade │          │
│  └─────────┘ └─────────┘          │
│  ┌─────────┐ ┌─────────┐          │
│  │  9.2    │ │  15.1   │          │
│  │ PM2.5   │ │  PM10   │          │
│  └─────────┘ └─────────┘          │
│  ┌─────────────────────────────┐  │
│  │      610 ppm — CO₂          │  │
│  └─────────────────────────────┘  │
├────────────────────────────────────┤
│  Análise do ambiente               │  ← Recomendações da NBR
│  • CO₂ em faixa confortável        │
│  • Temperatura adequada            │
├────────────────────────────────────┤
│  Recomendação (card escuro)        │  ← Card preto
├────────────────────────────────────┤
│  Histórico      [Hoje]             │  ← Gráfico de linha
│  ┌──────────────────────────────┐  │
│  │         ╱╲  ╱               │  │
│  │        ╱  ╲╱                │  │
│  └──────────────────────────────┘  │
│  08:00  10:00  12:00  14:00  16:00 │
├────────────────────────────────────┤
│  Alertas recentes                  │  ← Lista de alertas
│  ○ Sem alertas recentes.           │
└────────────────────────────────────┘
```

### `CollectionControlCard` — Botão de controle do ESP32

```kotlin
Button(
    onClick = onToggle,
    enabled = esp32Status != Esp32Status.OFFLINE,  // Desabilitado se offline
    colors = ButtonDefaults.buttonColors(
        containerColor = if (isCollecting) Color(0xFFF43F5E)  // Vermelho = parar
                        else Color(0xFF10B981)                  // Verde = iniciar
    )
) {
    Icon(if (isCollecting) Icons.Outlined.Stop else Icons.Outlined.PlayArrow)
    Text(if (isCollecting) "Parar coleta" else "Iniciar coleta")
}
```

O botão fica desabilitado quando o ESP32 está offline, pois não adiantaria enviar comandos.

### `SensorCard` — Card de sensor individual

Cada sensor tem um card com:
- Ícone representativo
- Rótulo (ex: "Temperatura")
- Valor em destaque (ex: "24.5")
- Unidade (ex: "°C")
- Texto auxiliar colorido indicando se está dentro da norma

```kotlin
@Composable
fun SensorCard(icon, label, value, unit, helper, helperColor, modifier) {
    // Exibe: ícone | "Agora"
    //        rótulo
    //        valor grande + unidade
    //        texto auxiliar (verde/amarelo/vermelho)
}
```

### `helperText()` e `helperColor()` — Classificação por parâmetro

Estas duas funções retornam o texto e a cor de status para cada sensor, com base nos limites da NBR 17037:2023:

```kotlin
fun helperText(param: String, value: Double): String = when (param) {
    "temp" -> when {
        value < 21.0 -> "Abaixo do conforto (NBR 17037: 21–26°C)"
        value > 26.0 -> "Acima do conforto (NBR 17037: 21–26°C)"
        else         -> "Adequada (21–26°C)"
    }
    "co2" -> when {
        value <= 800  -> "Ótimo (≤ 800 ppm)"
        value <= 1100 -> "Atenção — VMA 1100 ppm (NBR 17037 §5.2.1)"
        else          -> "Acima do VMA — NBR 17037 §5.2.1"
    }
    // ... outros parâmetros
}

fun helperColor(param: String, value: Double): Color = when (param) {
    "co2" -> when {
        value <= 800  -> Color(0xFF059669)  // Verde
        value <= 1100 -> Color(0xFFD97706)  // Amarelo
        else          -> Color(0xFFE11D48)  // Vermelho
    }
    // ...
}
```

### `MiniLineChart` — Gráfico de linha com Canvas

O gráfico é desenhado manualmente com a API `Canvas` do Compose, sem nenhuma biblioteca externa:

```kotlin
@Composable
fun MiniLineChart(values: List<Float>, lineColor: Color) {
    Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
        val maxV = values.max()
        val minV = values.min()
        val range = (maxV - minV).coerceAtLeast(1f)  // Evita divisão por zero

        val path = Path()
        values.forEachIndexed { i, v ->
            // Normaliza X: distribui os pontos igualmente na largura
            val x = i.toFloat() / (values.size - 1) * size.width
            // Normaliza Y: 80% da altura, com 10% de margem em cima e embaixo
            val y = size.height - ((v - minV) / range) * size.height * 0.8f - size.height * 0.1f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path, color = lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    }
}
```

**Como funciona a normalização dos valores?**
- O eixo X divide a largura total pelo número de pontos
- O eixo Y converte cada valor para uma posição entre 10% e 90% da altura
- A cor da linha é a mesma do `dotColor` da qualidade do ar (verde, amarelo ou vermelho)

### `SectionCard` — Card de seção reutilizável

```kotlin
@Composable
fun SectionCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = AppColors.Slate500)
            content()  // Lambda — cada uso passa conteúdo diferente
        }
    }
}
```

Este componente usa um **Slot API** (lambda `content`): define a estrutura do card e deixa o conteúdo interno ser passado como parâmetro. É o mesmo padrão que o Compose usa internamente no `Scaffold`, `LazyColumn`, etc.

---

## 11. Navegação entre Telas (MainActivity.kt)

### Single Activity Architecture

O app tem **uma única Activity** (`MainActivity`). As "telas" são composables que o Navigation Compose troca dentro da mesma Activity.

### `AirQualityNavHost` — Definindo as rotas

```kotlin
@Composable
fun AirQualityNavHost() {
    val navController = rememberNavController()
    val labViewModel: LabViewModel = viewModel()  // ← ViewModel compartilhada entre telas!

    NavHost(navController = navController, startDestination = "home") {
        
        composable("home") {
            HomeScreen(
                labViewModel = labViewModel,
                onLabSelected = { labId ->
                    navController.navigate("details/$labId")  // Navega passando o ID
                }
            )
        }

        composable(
            route = "details/{labId}",
            arguments = listOf(navArgument("labId") { type = NavType.StringType })
        ) { backStackEntry ->
            val labId = backStackEntry.arguments?.getString("labId") ?: return@composable
            DetailsScreen(
                labId = labId,
                labViewModel = labViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
```

**Pontos importantes:**
- A `LabViewModel` é criada **uma vez** no `NavHost` e passada para ambas as telas. Isso significa que HomeScreen e DetailsScreen compartilham os mesmos dados — não há duplicação de listeners do Firebase.
- O `labId` é passado pela URL da rota (`"details/$labId"`), como uma URL de um site.

### Transições de animação

```kotlin
NavHost(
    enterTransition = {
        slideInHorizontally(initialOffsetX = { it }) + fadeIn()   // Slide da direita + fade in
    },
    exitTransition = {
        slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut()  // Sai levemente para esquerda
    },
    popEnterTransition = {
        slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn()   // Volta da esquerda
    },
    popExitTransition = {
        slideOutHorizontally(targetOffsetX = { it }) + fadeOut()        // Sai para a direita
    }
)
```

Este padrão de animação imita o comportamento nativo do Android: ao avançar, a nova tela desliza da direita; ao voltar, a tela atual sai para a direita.

### `enableEdgeToEdge()`

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()  // ← Isso faz o app ocupar toda a tela, incluindo atrás da barra de status
    setContent { ... }
}
```

---

## 12. Identidade Visual — Tema e Cores

### Paleta `AppColors` — Tailwind Slate

O app usa a paleta de cinzas **Slate** do Tailwind CSS como base, com verde esmeralda como cor de destaque:

```kotlin
object AppColors {
    val Slate50  = Color(0xFFF8FAFC)  // Fundo quase branco
    val Slate100 = Color(0xFFF1F5F9)  // Fundo das telas
    val Slate400 = Color(0xFF94A3B8)  // Textos secundários leves
    val Slate500 = Color(0xFF64748B)  // Textos secundários
    val Slate700 = Color(0xFF334155)  // Textos de ênfase
    val Slate800 = Color(0xFF1E293B)  // Card do ESP32 (gradiente)
    val Slate900 = Color(0xFF0F172A)  // Título principal + card escuro
    val White    = Color(0xFFFFFFFF)
    val Emerald500 = Color(0xFF10B981) // Indicador "Coletando" (verde)
    val Amber500   = Color(0xFFF59E0B) // Alertas moderados (amarelo)
}
```

### Cores por qualidade do ar

| Qualidade | Fundo chip | Texto chip | Ponto/linha | Gradiente card |
|---|---|---|---|---|
| **BOA** | Verde claro `#D1FAE5` | Verde escuro `#065F46` | `#10B981` | `#ECFDF5` |
| **MODERADA** | Amarelo claro `#FEF3C7` | Marrom `#92400E` | `#F59E0B` | `#FFFBEB` |
| **RUIM** | Rosa claro `#FFE4E6` | Vermelho escuro `#9F1239` | `#F43F5E` | `#FFF1F2` |

### `AirQualityTheme` — Suporte a tema dinâmico

```kotlin
@Composable
fun AirQualityTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = true) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // Android 12+: usa as cores do wallpaper do usuário
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme  // Tema escuro padrão
        else -> LightColorScheme      // Tema claro padrão
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
```

---

## 13. Norma Técnica ABNT NBR 17037:2023

O app é construído em conformidade com a norma brasileira de qualidade do ar em ambientes interiores. Todos os limites exibidos e usados nas análises são baseados nesta norma.

### Parâmetros e limites

| Parâmetro | Seção | Faixa Ideal | VMA (Máximo) | Ação se ultrapassar |
|---|---|---|---|---|
| **CO₂** | §5.2.1 | ≤ 800 ppm | 1.100 ppm | Ventilação imediata |
| **Temperatura** | §5.2.3 | 21°C a 26°C | — | Ajustar climatização |
| **Umidade relativa** | §5.2.3 | 35% a 65% | — | Umidificar/desumidificar |
| **PM2.5** | §5.2.2 | ≤ 15 µg/m³ | 25 µg/m³ | Identificar fontes, limpar filtros |
| **PM10** | §5.2.2 | ≤ 30 µg/m³ | 50 µg/m³ | Verificar poeira, não varrer a seco |

VMA = Valor Máximo Admissível

### Como o app classifica a qualidade geral?

O app recebe o campo `qualidade_ar` do Firebase (enviado pelo ESP32 ou calculado no backend), que pode ser `"boa"`, `"moderada"` ou `"ruim"`. A lógica de classificação final pode ser implementada no firmware do ESP32 ou em Cloud Functions do Firebase.

---

## 14. Fluxo Completo de Dados

Este é o caminho completo que um dado percorre, desde o sensor físico até aparecer na tela:

```
[SENSOR FÍSICO]
Sensor CO₂ / DHT / PMS5003
        │
        ▼
[HARDWARE ESP32]
Lê os sensores
Calcula qualidade_ar
Faz upload para o Firebase
Atualiza ultimo_heartbeat a cada ~10s
        │
        ▼
[FIREBASE REALTIME DATABASE]
/salas/lab1/ultima_leitura/co2 = 920
/salas/lab1/ultima_leitura/temperatura = 29.0
/dispositivo/ultimo_heartbeat = 1748456789
        │ (push automático para todos os clientes)
        ▼
[LABVIEWMODEL — listenToSalas()]
ValueEventListener.onDataChange() é chamado
parseSala() converte DataSnapshot → Laboratory
gerarRecomendacoes() analisa os valores
_labs.value = listaAtualizada
        │ (StateFlow emite novo valor)
        ▼
[UI — HomeScreen / DetailsScreen]
collectAsStateWithLifecycle() recebe o novo estado
Compose recompõe apenas os componentes que mudaram
Tela atualizada em tempo real
```

### Fluxo inverso — App controlando o ESP32:

```
[USUÁRIO]
Toca em "Iniciar coleta" na DetailsScreen
        │
        ▼
[DETAILSSCREEN]
onToggle() → labViewModel.toggleCollection(labId)
        │
        ▼
[LABVIEWMODEL]
db.child("dispositivo").child("coletando").setValue(true)
db.child("dispositivo").child("sala_selecionada").setValue("lab1")
        │
        ▼
[FIREBASE]
/dispositivo/coletando = true
/dispositivo/sala_selecionada = "lab1"
        │ (push para o ESP32)
        ▼
[ESP32]
Lê o novo valor de "coletando"
Começa a coletar dados do laboratório "lab1"
```

---

## 15. Diagrama de Componentes

```
MainActivity.kt
└── AirQualityNavHost()
    ├── NavController (gerencia histórico de navegação)
    ├── LabViewModel (compartilhada entre telas)
    │   ├── StateFlow<List<Laboratory>> labs
    │   ├── StateFlow<Boolean> isLoading
    │   ├── StateFlow<Esp32Status> esp32Status
    │   ├── Firebase Listener: /dispositivo
    │   ├── Firebase Listener: /salas
    │   ├── Firebase Listener: /dispositivo/ultimo_heartbeat
    │   └── Coroutine: heartbeat checker (loop a cada 5s)
    │
    ├── HomeScreen (rota: "home")
    │   ├── LazyColumn
    │   ├── ESP32 Status Card
    │   └── AnimatedLabCard[] (um por laboratório)
    │       ├── QualityChip
    │       └── MetricMini × 3 (temp, umidade, CO₂)
    │
    └── DetailsScreen (rota: "details/{labId}")
        ├── Header Card (nome + qualidade + hora)
        ├── CollectionControlCard (botão iniciar/parar)
        ├── SensorCard × 5 (temp, umidade, PM2.5, PM10, CO₂)
        │   └── helperText() + helperColor() por sensor
        ├── SectionCard "Análise do ambiente"
        │   └── analysisItems (gerado pela ViewModel)
        ├── Card escuro "Recomendação"
        ├── SectionCard "Histórico"
        │   └── MiniLineChart (Canvas manual)
        └── SectionCard "Alertas recentes"
```

---

## Resumo para Apresentação

Se precisar apresentar o projeto em poucas frases:

> "O Lab Air Monitor é um app Android que monitora em tempo real a qualidade do ar de laboratórios. Ele usa a arquitetura MVVM com Jetpack Compose para a interface e Firebase Realtime Database como backend. Um dispositivo ESP32 com sensores de CO₂, temperatura, umidade e material particulado coleta os dados e os envia ao Firebase. O app recebe essas atualizações automaticamente e analisa os valores com base na norma brasileira ABNT NBR 17037:2023, gerando recomendações para garantir um ambiente saudável. O app também permite controlar remotamente quando o ESP32 inicia ou para a coleta de dados."

---

*Documentação gerada em 28/05/2026 para o projeto LabAirMonitor.*
