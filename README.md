# Lab Air Monitor

Aplicativo Android para monitoramento em tempo real da **qualidade do ar de laboratórios** de uma instituição de ensino. Os dados são coletados por um dispositivo **ESP32** com sensores ambientais e enviados ao Firebase, onde o app os exibe e classifica conforme a norma **ABNT NBR 17037:2023**.

---

## Funcionalidades

- Lista todos os laboratórios monitorados com status de qualidade do ar
- Exibe leituras em tempo real: **CO₂, temperatura, umidade, PM2.5 e PM10**
- Classifica a qualidade do ar como **Boa**, **Moderada** ou **Ruim**
- Gera recomendações automáticas baseadas na norma ABNT NBR 17037:2023
- Permite iniciar e parar a coleta de dados diretamente pelo app
- Exibe o status do dispositivo ESP32 (online/offline)

---

## Laboratórios Monitorados

| ID | Nome | Sigla |
|---|---|---|
| lift1 | Laboratório de Informática 1 | LIFT1 |
| lab1 | Laboratório de Eletrônica 1 | LAB1 |
| lab2 | Laboratório de Eletrônica 2 | LAB2 |
| lab3 | Laboratório de Eletrônica 3 | LAB3 |
| lma | Laboratório de Metodologias Ativas | LMA |
| labria | Laboratório de Robótica e IA | LABRIA |
| tutoria | Sala de Tutoria | Tutoria |

---

## Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Kotlin | 2.2.10 | Linguagem principal |
| Jetpack Compose | BOM 2026.02.01 | UI declarativa |
| Navigation Compose | 2.8.5 | Navegação entre telas |
| ViewModel + StateFlow | 2.8.7 | Gerenciamento de estado |
| Firebase Realtime Database | 21.0.0 | Dados em tempo real |
| Material 3 | via BOM | Componentes visuais |
| Min SDK | 24 (Android 7.0) | — |
| Target SDK | 36 (Android 15) | — |

---

## Arquitetura

O app segue o padrão **MVVM** recomendado pelo Google:

```
VIEW (HomeScreen / DetailsScreen)
  │ observa StateFlow
VIEWMODEL (LabViewModel)
  │ lê/escreve dados
MODEL (Models.kt + Firebase Realtime DB)
```

---

## Estrutura do Projeto

```
LabAirMonitor/
├── app/src/main/java/com/ingrid/airqualitymonitor/
│   ├── MainActivity.kt           # Ponto de entrada + navegação
│   ├── data/
│   │   └── Models.kt             # Modelos de dados
│   └── ui/
│       ├── screens/
│       │   ├── HomeScreen.kt     # Lista de laboratórios
│       │   └── DetailsScreen.kt  # Detalhes do laboratório
│       ├── theme/                # Cores, tema e tipografia
│       └── viewmodel/
│           └── LabViewModel.kt   # Lógica e estado do app
├── firmware_tcc/                 # Código do dispositivo ESP32
└── DOCUMENTACAO.md               # Documentação técnica completa
```

---

## Como Rodar

1. Clone o repositório
2. Adicione o arquivo `google-services.json` (Firebase) em `app/`
3. Abra no **Android Studio** e sincronize o Gradle
4. Execute em um dispositivo ou emulador com Android 7.0+

> O `google-services.json` não está versionado por conter credenciais do Firebase. Solicite ao responsável pelo projeto.

---

## Norma Técnica

A classificação da qualidade do ar segue a **ABNT NBR 17037:2023**, que define os limites aceitáveis de CO₂, material particulado e outros parâmetros para ambientes internos.