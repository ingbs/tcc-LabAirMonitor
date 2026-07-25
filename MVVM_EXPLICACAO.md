# Arquitetura MVVM — LabAirMonitor

## O que é MVVM?

MVVM significa **Model · View · ViewModel**. É um padrão de arquitetura que divide o código em três camadas com responsabilidades bem definidas, para que cada parte do app faça apenas uma coisa.

```
┌──────────────────────────────────────────────────────────────┐
│  FIREBASE (fonte de dados externa)                           │
│  Realtime Database  ·  Authentication                        │
└───────────────────────────┬──────────────────────────────────┘
                            │ lê dados em tempo real
                            ▼
┌──────────────────────────────────────────────────────────────┐
│  MODEL  (camada de dados)                                    │
│  Models.kt · AirQualityClassifier.kt · LegendaStrings.kt    │
└───────────────────────────┬──────────────────────────────────┘
                            │ fornece estruturas e regras
                            ▼
┌──────────────────────────────────────────────────────────────┐
│  VIEWMODEL  (camada de lógica)                               │
│  LabViewModel.kt                                             │
└───────────────────────────┬──────────────────────────────────┘
                            │ expõe StateFlow observável
                            ▼
┌──────────────────────────────────────────────────────────────┐
│  VIEW  (camada visual)                                       │
│  MainActivity · HomeScreen · DetailsScreen · LegendaScreen   │
└──────────────────────────────────────────────────────────────┘
```

A seta sempre aponta para baixo: a **View** conhece o **ViewModel**, e o **ViewModel** conhece o **Model** — mas nunca o contrário. Isso mantém o código organizado e facilita testar cada parte separadamente.

---

## Camada 1 — MODEL

> **Pergunta que essa camada responde:** *"Como os dados são estruturados e quais são as regras do negócio?"*

O Model não sabe nada sobre telas ou botões. Ele apenas define o que são os dados e como classificá-los.

---

### `data/Models.kt`

Define os tipos de dados que circulam pelo app inteiro.

| Tipo | O que representa |
|------|-----------------|
| `enum AirQuality` | Os dois estados possíveis de um laboratório: **BOA** (Conforme NBR 17037:2023) ou **RUIM** (Não conforme). Cada estado já traz as cores do chip e do card embutidas. |
| `enum Esp32Status` | Os três estados do hardware: `OFFLINE`, `STANDBY` (ligado mas parado) e `COLLECTING` (coletando dados). |
| `data class Laboratory` | O objeto principal do app. Representa um laboratório com todos os seus dados: nome, métricas (CO₂, temperatura, umidade, PM2.5, PM10), status de coleta, horário da última leitura e a recomendação gerada. |
| `val sampleLaboratories` | Lista com os 7 laboratórios cadastrados (LIFT1, LAB1, LAB2, LAB3, LMA, LABRIA, Tutoria), com todas as métricas zeradas como ponto de partida — os zeros são substituídos pelos dados reais do Firebase assim que chegam. |

---

### `data/AirQualityClassifier.kt`

É o **cérebro das regras da NBR 17037:2023**. Não exibe nada na tela — apenas recebe números e devolve uma classificação.

**Constantes definidas (limites normativos):**

| Parâmetro | Limite | Norma |
|-----------|--------|-------|
| CO₂ | > 1120 ppm → RUIM | NBR 17037:2023 §5.2.1 |
| Temperatura | < 21°C ou > 26°C → RUIM | NBR 17037:2023 §5.2.3 |
| Umidade | < 35% ou > 65% → RUIM | NBR 17037:2023 §5.2.3 |
| PM2.5 | > 25 µg/m³ → RUIM | NBR 17037:2023 §5.2.2 |
| PM10 | > 50 µg/m³ → RUIM | NBR 17037:2023 §5.2.2 |

**Funções principais:**

- `classify(co2, temp, humidity, pm25, pm10)` — recebe as cinco medições, ignora valores ≤ 0 (sem leitura válida), classifica cada parâmetro individualmente e retorna o pior resultado, a recomendação textual e os itens de análise.
- `calcularIndiceConama(c, faixas)` — calcula o índice de qualidade do ar adaptado da CONAMA 506/24 para material particulado (indicador secundário/visual, complementar à NBR).
- `corNivelConama(nivel)` e `nivelDoIndiceConama(indice)` — retornam a cor e o nível (N1 a N5) correspondentes ao índice CONAMA calculado.

---

### `data/LegendaStrings.kt`

Armazena os **textos explicativos** da tela de Legenda: nome, unidade, descrição e referência normativa de cada variável monitorada (CO₂, Temperatura, Umidade Relativa, PM2.5 e PM10). Manter esses textos no Model, e não embutidos na View, facilita tradução ou atualização futura.

---

## Camada 2 — VIEWMODEL

> **Pergunta que essa camada responde:** *"O que a tela precisa ver e o que acontece quando o usuário age?"*

O ViewModel fica entre o Model e a View. Ele busca dados do Firebase, aplica as regras do `AirQualityClassifier` e expõe o resultado como `StateFlow` — um fluxo reativo que a View observa automaticamente.

---

### `ui/viewmodel/LabViewModel.kt`

É o **único ViewModel do app**. Ele é criado uma vez e compartilhado entre `HomeScreen` e `DetailsScreen`.

**Dados expostos para a View:**

| Propriedade | Tipo | O que contém |
|-------------|------|-------------|
| `labs` | `StateFlow<List<Laboratory>>` | Lista atualizada de todos os laboratórios com os dados mais recentes do Firebase |
| `esp32Status` | `StateFlow<Esp32Status>` | Estado atual do hardware (OFFLINE / STANDBY / COLLECTING) |

**O que acontece internamente:**

1. **`signInAndStart()`** — faz login anônimo no Firebase Authentication (necessário para ter permissão de leitura no banco) e inicia os listeners.

2. **`listenToSalas()`** — escuta o nó `salas/` no Firebase Realtime Database em tempo real. Quando um novo dado chega, lê as métricas brutas e chama `AirQualityClassifier.classify()` para classificar a qualidade do ar. Atualiza o `StateFlow<List<Laboratory>>` com o laboratório recalculado.

3. **`listenToDispositivo()`** — escuta o nó `dispositivo/` para saber qual sala está sendo coletada no momento e atualiza `isCollecting` de cada lab.

4. **`listenToHeartbeat()`** + **`startHeartbeatChecker()`** — monitoram o `ultimo_heartbeat` da ESP32. Se o heartbeat parar de chegar por mais de 45 segundos (3 batidas de 15 s perdidas), o status vira `OFFLINE`.

5. **`toggleCollection(labId)`** — única função chamada pela View. Escreve no Firebase para iniciar ou parar a coleta naquele laboratório. O feedback visual chega automaticamente pelo listener do nó `dispositivo/`.

---

## Camada 3 — VIEW

> **Pergunta que essa camada responde:** *"Como isso aparece na tela?"*

A View não tem lógica de negócio. Ela apenas observa o ViewModel com `collectAsStateWithLifecycle()` e redesenha os elementos que mudaram (recomposição do Jetpack Compose).

---

### `MainActivity.kt`

É o **ponto de entrada** do app Android. Faz duas coisas:

1. Ativa o modo edge-to-edge (conteúdo atrás da barra de status).
2. Chama `AirQualityNavHost()`, que configura toda a navegação do app.

**`AirQualityNavHost()`** define as três rotas disponíveis e as animações de transição entre elas:

| Rota | Tela |
|------|------|
| `home` | `HomeScreen` — lista de laboratórios |
| `legenda` | `LegendaScreen` — explicação das variáveis |
| `details/{labId}` | `DetailsScreen` — detalhes de um laboratório específico |

O `LabViewModel` é criado aqui e passado para `HomeScreen` e `DetailsScreen` — garantindo que **ambas as telas compartilhem o mesmo estado** sem duplicar conexões com o Firebase.

---

### `ui/screens/HomeScreen.kt`

**Tela principal** — exibe a lista de todos os laboratórios.

**Componentes:**

| Componente | Função |
|-----------|--------|
| Cabeçalho | Título "Qualidade do Ar" + botão de ajuda (navega para a Legenda) |
| Card ESP32 | Indica Online/Offline com um ponto colorido (verde/vermelho) |
| `AnimatedLabCard` | Card de cada laboratório com animação em cascata (cada card aparece 80 ms após o anterior). Exibe nome, sigla, temperatura, umidade, CO₂ e horário da última leitura. Toque navega para `DetailsScreen`. |
| `MetricMini` | Componente reutilizável de mini card de métrica (usado dentro de cada `AnimatedLabCard`). |

---

### `ui/screens/DetailsScreen.kt`

**Tela de detalhes** — exibida quando o usuário toca em um laboratório.

**Componentes principais:**

| Componente | Função |
|-----------|--------|
| Card de cabeçalho | Nome completo, sigla e horário da última leitura |
| `CollectionControlCard` | Botão "Iniciar/Parar coleta" — chama `labViewModel.toggleCollection()`. Fica desabilitado se a ESP32 está offline. |
| `SensorCard` | Card individual para Temperatura, Umidade e CO₂, com o valor atual e um texto de status ("Adequada" / "Não adequada") colorido. |
| `PmUnificadoCard` | Exibe PM2.5 e PM10 lado a lado. Calcula o índice CONAMA adaptado e exibe a barra visual N1–N5. |
| Análise do ambiente | Lista os itens gerados pelo `AirQualityClassifier` com os valores medidos e os limites normativos. |
| Recomendação | Card escuro com o texto de recomendação gerado automaticamente pela classificação. |
| `IqarEfeitosCard` | Exibe os cinco níveis de impacto à saúde da CONAMA 506/24, destacando o nível atual do laboratório. |

---

### `ui/screens/LegendaScreen.kt`

**Tela informativa** — explica o que cada indicador significa para o usuário final.

Lê os dados de `LegendaStrings.variaveis` (Model) e renderiza um card por variável com `LegendaItem`. Para PM, exibe sub-cards (`LegendaSubItem`) diferenciando PM10 de PM2.5. Cada card mostra ícone, nome, unidade, descrição e referência normativa.

---

## Resumo visual do fluxo de dados

```
Firebase Realtime Database
        │
        │  (ValueEventListener — tempo real)
        ▼
  LabViewModel.kt
  ├── applyFirebaseMeasurements()
  │       └── AirQualityClassifier.classify()   ← regras da NBR
  ├── _labs (MutableStateFlow)  →  labs (StateFlow)
  └── _esp32Status              →  esp32Status (StateFlow)
              │
              │  collectAsStateWithLifecycle()
              ▼
   HomeScreen          DetailsScreen
   (lista de labs)     (detalhes + controle)
```

**Regra de ouro:** a View nunca modifica dados diretamente. Se o usuário aperta "Iniciar coleta", a View chama `viewModel.toggleCollection()` → o ViewModel escreve no Firebase → o Firebase notifica o listener → o ViewModel atualiza o StateFlow → a View redesenha automaticamente.