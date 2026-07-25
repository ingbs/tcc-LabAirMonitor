# Firmware — Lab Air Monitor (ESP32)

Firmware do dispositivo ESP32 responsável pela coleta e envio dos dados de qualidade do ar ao Firebase Realtime Database.

---

## Hardware necessário

| Componente | Função | Interface |
|---|---|---|
| ESP32 (qualquer variante com 38 pinos) | Microcontrolador principal | — |
| AHT10 | Temperatura e umidade | I2C (SDA=GPIO21, SCL=GPIO22) |
| MQ-135 (módulo FC-22) | CO₂ estimado | ADC (GPIO34) |
| PMS5003 | PM2.5 e PM10 | UART2 (RX=GPIO16, TX=GPIO17) |

### Circuito do MQ-135

O módulo FC-22 opera com VCC de 5 V, mas o ESP32 tolera apenas 3,3 V no ADC. É obrigatório usar um **divisor de tensão** antes do GPIO34:

```
Saída analógica do FC-22
        │
       R1 = 10 kΩ
        │
        ├──── GPIO34 (ADC input-only)
        │
       R2 = 18 kΩ
        │
       GND
```

Fator do divisor: `(R1 + R2) / R2 = 28 / 18 ≈ 1,556` — já embutido na constante `DIVISOR_FATOR`.

---

## Bibliotecas (Arduino IDE)

Instale via **Sketch → Include Library → Manage Libraries**:

| Biblioteca | Autor | Versão testada |
|---|---|---|
| FirebaseESP32 | Mobizt | 4.x |
| ArduinoJson | Benoit Blanchon | 6.x |
| Adafruit AHTX0 | Adafruit | qualquer |

> **Atenção:** use `FirebaseESP32` (Mobizt), **não** `Firebase_ESP_Client`. A versão nova causa crash de heap SSL com o core ESP32 3.x.

---

## Configuração antes de gravar

Abra `firmware_tcc.ino` e edite as três seções abaixo:

```cpp
// 1. Sala onde o ESP32 será instalado
#define SALA_ID "LAB1"
// Valores válidos: LAB1, LAB2, LAB3, LABRIA, LIFT1, LMA, TUTORIA

// 2. Credenciais de rede
#define WIFI_SSID      "SUA_REDE_WIFI"
#define WIFI_PASSWORD  "SUA_SENHA_WIFI"

// 3. Credenciais do Firebase (copie do console do projeto)
#define FIREBASE_HOST  "SEU_PROJETO-default-rtdb.firebaseio.com"
#define FIREBASE_AUTH  "SUA_CHAVE_SECRETA_FIREBASE"
```

A `FIREBASE_AUTH` é o **Database Secret** do projeto, disponível em:  
**Firebase Console → Configurações do Projeto → Contas de serviço → Secrets do banco de dados**.

---

## Comportamento do firmware

### Inicialização (`setup`)

1. Inicializa I2C, UART2 e ADC
2. Conecta ao Wi-Fi (reinicia o ESP32 se falhar)
3. Sincroniza o relógio interno via NTP (`pool.ntp.org`, fuso UTC-4 / Manaus)
4. Configura a conexão com o Firebase
5. Aguarda **60 segundos** de warm-up do MQ-135 para estabilizar a resistência interna

### Loop principal

| Ação | Intervalo |
|---|---|
| **Heartbeat** — grava timestamp e `"online"` em `/dispositivo/` | A cada 15 s |
| **Coleta e envio** — lê os três sensores e envia ao Firebase | A cada 5 min |

A coleta só ocorre quando o app Android sinalizar `coletando = true` **e** a `sala_selecionada` coincidir com o `SALA_ID` gravado no firmware.

---

## Lógica de cada sensor

### AHT10 — Temperatura e Umidade
Leitura via driver Adafruit. Inclui verificação de sanidade (−10 °C a 60 °C; 0 % a 100 % UR). Envia zeros se o sensor falhar, sem travar o envio das demais leituras.

### MQ-135 — CO₂ estimado (ppm)

Conversão ADC → CO₂ em 7 etapas:

```
ADC (12 bits) → Vout (V) → Vrl (V, após divisor) → Rs (kΩ) → Rs/Ro → ppm
```

- **Curva usada:** Davide Gironi (2014) + Winsen datasheet — `ppm = (Rs/Ro / A)^(1/B)`
- **Ro calibrado:** 3,27 kΩ (mediana de 185 leituras em ar limpo)
- **Clamp:** mínimo 400 ppm (ar limpo ambiente), máximo 5 000 ppm

### PMS5003 — PM2.5 e PM10 (µg/m³)
Protocolo serial Plantower de 32 bytes. O firmware sincroniza com os bytes de início (`0x42 0x4D`), valida o checksum e extrai os valores atmosféricos. PM1.0 é calculado internamente mas **não enviado** (sem VMA definido na ABNT NBR 17037:2023).

---

## Estrutura de dados no Firebase

```
/
├── dispositivo/
│   ├── coletando           → bool   (app controla)
│   ├── sala_selecionada    → string (app controla)
│   ├── status_esp32        → "online" (escrito pelo firmware)
│   └── ultimo_heartbeat    → int    (Unix timestamp)
│
└── salas/
    └── {SALA_ID}/
        └── ultima_leitura/
            ├── timestamp   → int    (Unix timestamp)
            ├── co2         → int    (ppm)
            ├── temperatura → float  (°C, 2 casas)
            ├── umidade     → float  (%, 2 casas)
            ├── pm2_5       → int    (µg/m³)
            └── pm10        → int    (µg/m³)
```

---

## Norma aplicada

Os limites de classificação da qualidade do ar seguem a **ABNT NBR 17037:2023**, que define Valores Máximos Admissíveis (VMA) para ambientes internos. A classificação em **Boa / Moderada / Ruim** é feita pelo app Android, não pelo firmware.
