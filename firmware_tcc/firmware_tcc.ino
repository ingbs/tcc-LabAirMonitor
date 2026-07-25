// =============================================================================
// firmware_tcc.ino — Monitoramento de Qualidade do Ar em Laboratórios (TCC UFAM)
// =============================================================================
// Sensores:
//   • AHT10  — temperatura e umidade via I2C
//              Biblioteca: Adafruit AHTX0 by Adafruit
//   • MQ-135 — CO₂ estimado via ADC (módulo FC-22 com divisor de tensão)
//              Curva log-log: Davide Gironi (2014) + Winsen datasheet MQ-135
//   • PMS5003 — PM2.5 e PM10 via UART (protocolo Plantower, sem biblioteca externa)
//
// Firebase:  FirebaseESP32 by Mobizt v4.x  — OBRIGATÓRIO usar esta lib,
//            NÃO usar Firebase_ESP_Client.h (causa crash de heap SSL no core 3.x)
// JSON:      ArduinoJson by Benoit Blanchon v6.x
// =============================================================================

// --- Definição da sala (ÚNICO ponto a alterar para cada laboratório) ----------
#define SALA_ID "LAB1"
// Valores válidos: LAB1, LAB2, LAB3, LABRIA, LIFT1, LMA, TUTORIA

// --- Credenciais (preencha antes de gravar no ESP32) -------------------------
#define WIFI_SSID      "SUA_REDE_WIFI"
#define WIFI_PASSWORD  "SUA_SENHA_WIFI"
#define FIREBASE_HOST  "SEU_PROJETO-default-rtdb.firebaseio.com"
#define FIREBASE_AUTH  "SUA_CHAVE_SECRETA_FIREBASE"

// --- Bibliotecas -------------------------------------------------------------
#include <WiFi.h>            // Wi-Fi nativo do ESP32
#include <time.h>            // Funções NTP e Unix timestamp
#include <Wire.h>            // Barramento I2C
#include <Adafruit_AHTX0.h> // Sensor AHT10/AHT20 (Adafruit AHTX0)
#include <FirebaseESP32.h>  // Firebase Realtime Database (Mobizt v4.x)
#include <ArduinoJson.h>    // Serialização JSON (Benoit Blanchon v6.x)

// --- Pinagem -----------------------------------------------------------------
#define PIN_SDA    21   // I2C SDA → AHT10
#define PIN_SCL    22   // I2C SCL → AHT10
#define PIN_MQ135  34   // ADC input-only → nó do divisor de tensão (MQ-135)
#define UART2_RX   16   // RX2 do ESP32 → TX do PMS5003
#define UART2_TX   17   // TX2 do ESP32 → RX do PMS5003

// --- Configuração NTP --------------------------------------------------------
#define NTP_SERVER    "pool.ntp.org"
#define GMT_OFFSET_S  (-4 * 3600)  // Manaus: UTC-4, sem horário de verão
#define DST_OFFSET_S  0

// --- Intervalos de tempo (ms) ------------------------------------------------
#define INTERVALO_LEITURA    300000UL  // 5 minutos entre coletas
#define INTERVALO_HEARTBEAT   15000UL  // 15 segundos entre heartbeats

// --- Calibração MQ-135 — valores definitivos, NÃO alterar -------------------
// Obtidos em sessão estável: 185 leituras, desvio padrão 0,11 kΩ
#define MQ135_RO         3.27f          // Resistência em ar limpo (kΩ), mediana
#define MQ135_RL         0.038f         // Resistência de carga do módulo FC-22 (kΩ)
#define MQ135_VC         5.0f           // Tensão de circuito do MQ-135 (V)
#define ADC_REF          3.3f           // Tensão de referência do ADC do ESP32 (V)
#define ADC_BITS         4095.0f        // Resolução ADC: 12 bits
#define DIVISOR_FATOR    (28.0f/18.0f)  // R1=10kΩ, R2=18kΩ → fator de correção

// Curva CO₂ (Davide Gironi 2014 + Winsen datasheet): Rs/Ro = a × ppm^b
#define MQ135_A    116.6020682f
#define MQ135_B    (-2.769034857f)
#define RSRO_MIN   0.35f    // Clamp mínimo de Rs/Ro para evitar leituras absurdas
#define CO2_MIN    400.0f   // Clamp mínimo de CO₂ (ppm)
#define CO2_MAX    5000.0f  // Clamp máximo de CO₂ (ppm)

// --- Limites de sanidade do AHT10 -------------------------------------------
#define TEMP_MIN  (-10.0f)
#define TEMP_MAX   60.0f
#define UMID_MIN    0.0f
#define UMID_MAX  100.0f

// --- Warm-up do MQ-135 -------------------------------------------------------
#define MQ135_WARMUP_S  60  // 60 s de estabilização antes de declarar pronto

// --- Objetos globais ---------------------------------------------------------
Adafruit_AHTX0 aht;        // Sensor AHT10
FirebaseData   fbData;     // Objeto de requisições Firebase
FirebaseAuth   fbAuth;     // Autenticação Firebase
FirebaseConfig fbConfig;   // Configuração Firebase (host + token)

// --- Variáveis de controle de tempo ------------------------------------------
unsigned long ultimaLeitura   = 0;  // millis() da última coleta enviada
unsigned long ultimoHeartbeat = 0;  // millis() do último heartbeat enviado

// --- Estrutura de retorno do PMS5003 -----------------------------------------
struct DadosPMS {
  bool     valido;  // true se frame recebido e checksum válido
  uint16_t pm2_5;   // PM2.5 atmosférico (µg/m³)
  uint16_t pm10;    // PM10  atmosférico (µg/m³)
};

// --- Protótipos --------------------------------------------------------------
bool     conectarWiFi();
void     configurarFirebase();
void     sincronizarNTP();
long     obterTimestamp();
float    lerCO2_MQ135();
bool     lerAHT10(float &temp, float &umidade);
DadosPMS lerPMS5003();
bool     verificarColetando();
bool     enviarDados(long ts, int co2, float temp, float umid, int pm25, int pm10);
void     enviarHeartbeat();


// =============================================================================
// SETUP
// =============================================================================
void setup() {
  Serial.begin(115200);
  Serial.println("\n=== Firmware TCC — Qualidade do Ar UFAM ===");
  Serial.printf("[SETUP] Sala configurada: %s\n", SALA_ID);

  // Inicializa I2C nos pinos definidos
  Wire.begin(PIN_SDA, PIN_SCL);

  // Inicializa UART2 para o PMS5003 (9600 baud, 8 bits, sem paridade, 1 stop bit)
  Serial2.begin(9600, SERIAL_8N1, UART2_RX, UART2_TX);

  // Configura ADC: atenuação 11 dB permite medir 0–3,3 V no GPIO 34 (input-only)
  analogSetAttenuation(ADC_11db);
  analogReadResolution(12);  // 12 bits → 0 a 4095

  // Inicializa o AHT10 via I2C; firmware continua mesmo se falhar (envia zeros)
  if (!aht.begin()) {
    Serial.println("[ERRO] AHT10 não encontrado. Verifique a fiação I2C.");
  } else {
    Serial.println("[OK] AHT10 inicializado.");
  }

  // Conecta ao Wi-Fi; reinicia o ESP32 em caso de falha
  if (!conectarWiFi()) {
    Serial.println("[ERRO] Wi-Fi falhou. Reiniciando em 5 s...");
    delay(5000);
    ESP.restart();
  }

  // Sincroniza o relógio via NTP (necessário para o timestamp das leituras)
  sincronizarNTP();

  // Configura as credenciais e inicializa a conexão com o Firebase
  configurarFirebase();

  // Warm-up do MQ-135: sensor precisa de 60 s para estabilizar a resistência interna
  Serial.println("[MQ-135] Iniciando warm-up de 60 segundos...");
  for (int i = MQ135_WARMUP_S; i > 0; i--) {
    Serial.printf("[MQ-135] Warm-up: %2d s restantes...\n", i);
    delay(1000);  // delay() permitido no setup/warm-up
  }
  Serial.println("[MQ-135] Warm-up concluído. Sensor pronto.");

  Serial.println("[SETUP] Inicialização completa. Entrando no loop principal.\n");
}


// =============================================================================
// LOOP PRINCIPAL — controle de tempo exclusivamente via millis()
// =============================================================================
void loop() {
  unsigned long agora = millis();

  // Heartbeat a cada 15 s, independente do estado de coleta
  if (agora - ultimoHeartbeat >= INTERVALO_HEARTBEAT) {
    ultimoHeartbeat = agora;
    enviarHeartbeat();
  }

  // Coleta a cada 5 minutos
  if (agora - ultimaLeitura >= INTERVALO_LEITURA) {
    ultimaLeitura = agora;

    // Verifica condições de controle antes de ler sensores
    if (!verificarColetando()) {
      return;
    }

    // --- Leitura dos sensores ---
    float temp = 0.0f, umid = 0.0f;
    bool ahtOk = lerAHT10(temp, umid);
    if (!ahtOk) {
      Serial.println("[AVISO] AHT10 falhou; enviando 0 para temperatura e umidade.");
    }

    int co2 = (int)lerCO2_MQ135();

    DadosPMS pms = lerPMS5003();
    if (!pms.valido) {
      Serial.println("[AVISO] PMS5003 falhou; enviando 0 para PM2.5 e PM10.");
    }

    long ts = obterTimestamp();

    // --- Envia todos os campos em uma única chamada HTTP ---
    bool ok = enviarDados(ts, co2, temp, umid,
                          pms.valido ? (int)pms.pm2_5 : 0,
                          pms.valido ? (int)pms.pm10  : 0);
    if (ok) {
      Serial.printf("[OK] Dados enviados — ts=%ld | CO2=%d ppm | T=%.2f°C | "
                    "H=%.2f%% | PM2.5=%d | PM10=%d\n",
                    ts, co2, temp, umid, pms.pm2_5, pms.pm10);
    }
  }
}


// =============================================================================
// FUNÇÕES
// =============================================================================

// --- Conecta ao Wi-Fi; retorna true se bem-sucedido --------------------------
bool conectarWiFi() {
  Serial.printf("[Wi-Fi] Conectando a '%s'", WIFI_SSID);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  int tentativas = 0;
  while (WiFi.status() != WL_CONNECTED && tentativas < 20) {
    delay(500);
    Serial.print(".");
    tentativas++;
  }
  Serial.println();

  if (WiFi.status() == WL_CONNECTED) {
    Serial.printf("[Wi-Fi] Conectado! IP: %s\n", WiFi.localIP().toString().c_str());
    return true;
  }
  Serial.println("[Wi-Fi] Falha na conexão.");
  return false;
}

// --- Configura host, token legado e inicializa o Firebase --------------------
void configurarFirebase() {
  fbConfig.host = FIREBASE_HOST;

  // Token legado (Database Secret) — modo compatível com FirebaseESP32 v4.x
  fbConfig.signer.tokens.legacy_token = FIREBASE_AUTH;

  Firebase.begin(&fbConfig, &fbAuth);
  Firebase.reconnectWiFi(true);  // Reconecta automaticamente se o Wi-Fi cair
  Serial.println("[Firebase] Configurado.");
}

// --- Sincroniza RTC interno do ESP32 via NTP (até 10 tentativas de 1 s) ------
void sincronizarNTP() {
  configTime(GMT_OFFSET_S, DST_OFFSET_S, NTP_SERVER);
  Serial.print("[NTP] Sincronizando");

  struct tm timeinfo;
  int tentativas = 0;
  while (!getLocalTime(&timeinfo) && tentativas < 10) {
    Serial.print(".");
    delay(1000);
    tentativas++;
  }
  Serial.println();

  if (tentativas < 10) {
    Serial.printf("[NTP] Sincronizado: %02d/%02d/%04d %02d:%02d:%02d (UTC-4)\n",
                  timeinfo.tm_mday, timeinfo.tm_mon + 1, timeinfo.tm_year + 1900,
                  timeinfo.tm_hour, timeinfo.tm_min, timeinfo.tm_sec);
  } else {
    Serial.println("[NTP] Falha na sincronização. Timestamps podem ser inválidos.");
  }
}

// --- Retorna o Unix timestamp atual (segundos desde 01/01/1970) --------------
long obterTimestamp() {
  time_t agora;
  time(&agora);  // Preenche 'agora' via RTC sincronizado pelo NTP
  return (long)agora;
}

// --- Lê CO₂ estimado do MQ-135 e retorna o valor em ppm ---------------------
float lerCO2_MQ135() {
  // Etapa 1: média de 10 leituras ADC para reduzir ruído elétrico
  long somaADC = 0;
  for (int i = 0; i < 10; i++) {
    somaADC += analogRead(PIN_MQ135);
    delay(10);  // Pausa curta entre amostras (aceitável dentro da função)
  }
  float adcMedio = somaADC / 10.0f;

  // Etapa 2: converte leitura ADC para tensão no pino GPIO (após divisor)
  float vout = (adcMedio / ADC_BITS) * ADC_REF;

  // Etapa 3: recupera a tensão original antes do divisor resistivo
  // Divisor: R1=10kΩ (série), R2=18kΩ (paralelo ao GND) → fator = 28/18
  float vrl = vout * DIVISOR_FATOR;

  // Etapa 4: calcula resistência do sensor Rs pela lei de Ohm no circuito do MQ-135
  // Rs = RL × (Vc − Vrl) / Vrl
  float rs = 0.0f;
  if (vrl > 0.001f) {
    rs = MQ135_RL * (MQ135_VC - vrl) / vrl;
  } else {
    Serial.println("[MQ-135] Vrl ≈ 0 V — verifique o circuito (divisor ou sensor).");
    return CO2_MIN;
  }

  // Etapa 5: calcula razão Rs/Ro e aplica clamp mínimo
  float rsRo = rs / MQ135_RO;
  if (rsRo < RSRO_MIN) {
    rsRo = RSRO_MIN;
  }

  // Etapa 6: aplica a curva log-log de Davide Gironi para estimar CO₂
  // Modelo: Rs/Ro = a × ppm^b  →  ppm = (Rs/Ro / a)^(1/b)
  float ppm = powf(rsRo / MQ135_A, 1.0f / MQ135_B);

  // Etapa 7: clamp final entre os limites operacionais do sensor
  if (ppm < CO2_MIN) ppm = CO2_MIN;
  if (ppm > CO2_MAX) ppm = CO2_MAX;

  // Debug detalhado para calibração e verificação em campo
  Serial.printf("[MQ-135] ADC=%.1f | Vout=%.4fV | Vrl=%.4fV | "
                "Rs=%.4fkΩ | Rs/Ro=%.4f | CO2=%.1f ppm\n",
                adcMedio, vout, vrl, rs, rsRo, ppm);

  return ppm;
}

// --- Lê temperatura e umidade do AHT10 via I2C -------------------------------
// Retorna true se a leitura for válida e estiver dentro dos limites de sanidade
bool lerAHT10(float &temp, float &umidade) {
  sensors_event_t hEvent, tEvent;

  // Solicita evento de temperatura e umidade ao driver Adafruit
  if (!aht.getEvent(&hEvent, &tEvent)) {
    Serial.println("[AHT10] Falha ao ler sensor.");
    temp    = 0.0f;
    umidade = 0.0f;
    return false;
  }

  float t = tEvent.temperature;
  float h = hEvent.relative_humidity;

  // Verificação de sanidade: descarta leituras fisicamente impossíveis
  if (t < TEMP_MIN || t > TEMP_MAX || h < UMID_MIN || h > UMID_MAX) {
    Serial.printf("[AHT10] Fora dos limites de sanidade: T=%.2f°C, H=%.2f%%\n", t, h);
    temp    = 0.0f;
    umidade = 0.0f;
    return false;
  }

  temp    = t;
  umidade = h;
  Serial.printf("[AHT10] Temperatura=%.2f°C | Umidade=%.2f%%\n", temp, umidade);
  return true;
}

// --- Lê PM2.5 e PM10 do PMS5003 via UART2 (protocolo Plantower) --------------
// Frame de 32 bytes: start=0x42 0x4D, dados big-endian, checksum nos bytes 30-31
DadosPMS lerPMS5003() {
  DadosPMS resultado = {false, 0, 0};

  // Descarta bytes residuais no buffer para garantir leitura de um frame novo
  while (Serial2.available()) {
    Serial2.read();
  }

  uint8_t       frame[32];
  int           idx     = 0;
  unsigned long tInicio = millis();

  // Aguarda e monta o frame byte a byte com timeout de 3 segundos
  while (millis() - tInicio < 3000UL) {
    if (!Serial2.available()) continue;

    uint8_t b = Serial2.read();

    if (idx == 0) {
      // Sincroniza com o primeiro byte de início: 0x42
      if (b != 0x42) continue;
      frame[0] = b;
      idx = 1;
    } else if (idx == 1) {
      // Valida segundo byte de início: deve ser 0x4D
      if (b != 0x4D) {
        idx = 0;  // Frame inválido, recomeça a sincronização
        continue;
      }
      frame[1] = b;
      idx = 2;
    } else {
      // Preenche os bytes restantes do frame (posições 2 a 31)
      frame[idx++] = b;
      if (idx == 32) break;  // Frame completo
    }
  }

  // Verifica se o frame chegou completo dentro do timeout
  if (idx < 32) {
    Serial.println("[PMS5003] Timeout: frame incompleto.");
    return resultado;
  }

  // Valida checksum: soma dos bytes 0–29 deve igualar os bytes 30–31 (big-endian)
  uint16_t checksumCalc  = 0;
  for (int i = 0; i < 30; i++) {
    checksumCalc += frame[i];
  }
  uint16_t checksumFrame = ((uint16_t)frame[30] << 8) | frame[31];

  if (checksumCalc != checksumFrame) {
    Serial.printf("[PMS5003] Checksum inválido: calculado=0x%04X, frame=0x%04X\n",
                  checksumCalc, checksumFrame);
    return resultado;
  }

  // Extrai valores atmosféricos em big-endian
  // PM1.0 (bytes 10-11) é calculado mas NÃO enviado: sem VMA na NBR 17037:2023
  uint16_t pm1_0_atm = ((uint16_t)frame[10] << 8) | frame[11];  // referência interna
  resultado.pm2_5    = ((uint16_t)frame[12] << 8) | frame[13];
  resultado.pm10     = ((uint16_t)frame[14] << 8) | frame[15];
  resultado.valido   = true;

  Serial.printf("[PMS5003] PM1.0=%d | PM2.5=%d | PM10=%d µg/m³ (PM1.0 não enviado)\n",
                pm1_0_atm, resultado.pm2_5, resultado.pm10);
  return resultado;
}

// --- Verifica se este dispositivo deve coletar dados agora -------------------
// Condições: sala_selecionada == SALA_ID  E  coletando == true
bool verificarColetando() {
  // Lê a sala selecionada pelo app Android
  if (!Firebase.getString(fbData, "/dispositivo/sala_selecionada")) {
    Serial.printf("[Firebase] Erro ao ler sala_selecionada: %s\n",
                  fbData.errorReason().c_str());
    return false;
  }
  String salaSelecionada = fbData.stringData();

  // Este dispositivo só coleta quando estiver selecionado
  if (salaSelecionada != SALA_ID) {
    Serial.printf("[Controle] Sala selecionada='%s', este dispositivo='%s'. Ignorando.\n",
                  salaSelecionada.c_str(), SALA_ID);
    return false;
  }

  // Lê a flag de início/parada de coleta
  if (!Firebase.getBool(fbData, "/dispositivo/coletando")) {
    Serial.printf("[Firebase] Erro ao ler coletando: %s\n",
                  fbData.errorReason().c_str());
    return false;
  }
  if (!fbData.boolData()) {
    Serial.println("[Controle] Coleta pausada pelo app. Aguardando comando...");
    return false;
  }

  return true;  // Todas as condições atendidas; pode coletar
}

// --- Envia todos os campos de ultima_leitura em uma única chamada HTTP -------
// Usa Firebase.setJSON() para economizar quota do plano Spark (1 req. em vez de 6)
bool enviarDados(long ts, int co2, float temp, float umid, int pm25, int pm10) {
  // Serializa com ArduinoJson para ter controle preciso do JSON gerado
  StaticJsonDocument<256> doc;
  doc["timestamp"]   = (int)ts;
  doc["co2"]         = co2;
  doc["temperatura"] = roundf(temp * 100.0f) / 100.0f;  // 2 casas decimais
  doc["umidade"]     = roundf(umid * 100.0f) / 100.0f;  // 2 casas decimais
  doc["pm2_5"]       = pm25;
  doc["pm10"]        = pm10;

  String jsonStr;
  serializeJson(doc, jsonStr);

  // Converte para FirebaseJson (tipo exigido por Firebase.setJSON)
  FirebaseJson fbJson;
  fbJson.setJsonData(jsonStr);

  // Caminho: /salas/{SALA_ID}/ultima_leitura
  String caminho = String("/salas/") + SALA_ID + "/ultima_leitura";

  if (!Firebase.setJSON(fbData, caminho, fbJson)) {
    Serial.printf("[Firebase] Erro ao enviar dados: %s\n",
                  fbData.errorReason().c_str());
    return false;
  }
  return true;
}

// --- Envia heartbeat: timestamp atual e status "online" ----------------------
// Chamado a cada 15 s para que o app Android detecte offline após 30 s de silêncio
void enviarHeartbeat() {
  // Tenta reconectar o Wi-Fi se necessário antes de enviar
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("[Wi-Fi] Desconectado durante heartbeat. Reconectando...");
    conectarWiFi();
  }

  long ts = obterTimestamp();

  if (!Firebase.setInt(fbData, "/dispositivo/ultimo_heartbeat", (int)ts)) {
    Serial.printf("[Heartbeat] Erro ao escrever timestamp: %s\n",
                  fbData.errorReason().c_str());
  }
  if (!Firebase.setString(fbData, "/dispositivo/status_esp32", "online")) {
    Serial.printf("[Heartbeat] Erro ao escrever status: %s\n",
                  fbData.errorReason().c_str());
  }

  Serial.printf("[Heartbeat] Enviado: ts=%ld\n", ts);
}
