package com.ingrid.airqualitymonitor.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ingrid.airqualitymonitor.data.AirQualityClassifier
import com.ingrid.airqualitymonitor.data.Esp32Status
import com.ingrid.airqualitymonitor.data.Laboratory
import com.ingrid.airqualitymonitor.data.sampleLaboratories
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "LabViewModel"

class LabViewModel : ViewModel() {

    private val db   = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private val _labs = MutableStateFlow<List<Laboratory>>(sampleLaboratories)
    val labs: StateFlow<List<Laboratory>> = _labs

    private val _esp32Status = MutableStateFlow(Esp32Status.OFFLINE)
    val esp32Status: StateFlow<Esp32Status> = _esp32Status

    private var coletando: Boolean = false
    private var salaSelecionada: String = ""
    private var statusEsp32Str: String = ""
    private var lastHeartbeatValue: Long = 0L

    init {
        signInAndStart()
    }

    private fun signInAndStart() {
        if (auth.currentUser != null) {
            Log.d(TAG, "Auth: usuário já logado (${auth.currentUser!!.uid})")
            startListeners()
            return
        }
        auth.signInAnonymously().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e(TAG, "Auth: falha no login anônimo", task.exception)
                return@addOnCompleteListener
            }
            Log.d(TAG, "Auth: login anônimo OK (${auth.currentUser?.uid})")
            startListeners()
        }
    }

    private fun startListeners() {
        listenToDispositivo()
        listenToSalas()
        listenToHeartbeat()
        startHeartbeatChecker()
    }

    private fun listenToDispositivo() {
        db.child("dispositivo").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "dispositivo: ${snapshot.value}")
                coletando       = snapshot.child("coletando").getValue(Boolean::class.java) ?: false
                salaSelecionada = snapshot.child("sala_selecionada").getValue(String::class.java) ?: ""
                statusEsp32Str  = snapshot.child("status_esp32").getValue(String::class.java) ?: ""
                _labs.value = _labs.value.map { lab ->
                    lab.copy(isCollecting = coletando && salaSelecionada.equals(lab.id, ignoreCase = true))
                }
                updateEsp32Status()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "dispositivo cancelado: ${error.message} (código ${error.code})")
            }
        })
    }

    private fun listenToHeartbeat() {
        db.child("dispositivo").child("ultimo_heartbeat")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val newValue = snapshot.getValue(Long::class.java)
                        ?: snapshot.getValue(Int::class.java)?.toLong()
                        ?: 0L
                    Log.d(TAG, "heartbeat recebido: $newValue")
                    if (newValue > 0 && newValue != lastHeartbeatValue) {
                        lastHeartbeatValue = newValue
                    }
                    updateEsp32Status()
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "heartbeat cancelado: ${error.message} (código ${error.code})")
                }
            })
    }

    private fun updateEsp32Status() {
        val nowSeconds = System.currentTimeMillis() / 1000L
        val secondsSinceHeartbeat = if (lastHeartbeatValue > 0) nowSeconds - lastHeartbeatValue else Long.MAX_VALUE

        // status_esp32 escrito pelo firmware é o sinal primário;
        // heartbeat (>45 s = 3 batidas perdidas de 15 s) é o fallback.
        val online = when (statusEsp32Str.lowercase().trim()) {
            "online", "on", "conectado", "connected" -> true
            "offline", "off", "desconectado", "disconnected" -> false
            else -> secondsSinceHeartbeat <= 45
        }

        _esp32Status.value = when {
            !online   -> Esp32Status.OFFLINE
            coletando -> Esp32Status.COLLECTING
            else      -> Esp32Status.STANDBY
        }
    }

    private fun startHeartbeatChecker() {
        viewModelScope.launch {
            while (true) {
                delay(5_000L)
                updateEsp32Status()
            }
        }
    }

    private fun listenToSalas() {
        db.child("salas").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "salas recebidas: ${snapshot.children.map { it.key }}")
                val firebaseData = snapshot.children.associateBy { it.key?.uppercase() ?: "" }

                _labs.value = _labs.value.map { lab ->
                    val salaSnap = firebaseData[lab.id.uppercase()]
                    if (salaSnap != null) {
                        applyFirebaseMeasurements(lab, salaSnap)
                    } else {
                        // Sala sem dados no Firebase: mantém métricas em 0 (exibidas como "—")
                        lab.copy(
                            isCollecting = coletando && salaSelecionada.equals(lab.id, ignoreCase = true)
                        )
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "salas cancelado: ${error.message} (código ${error.code})")
            }
        })
    }

    private fun applyFirebaseMeasurements(lab: Laboratory, snap: DataSnapshot): Laboratory {
        val l = snap.child("ultima_leitura")

        val co2 = l.child("co2").getValue(Int::class.java)
            ?: l.child("co2").getValue(Long::class.java)?.toInt()
            ?: 0

        val temp = l.child("temperatura").getValue(Double::class.java)
            ?: l.child("temperatura").getValue(Float::class.java)?.toDouble()
            ?: 0.0

        val umidade = l.child("umidade").getValue(Double::class.java)?.toInt()
            ?: l.child("umidade").getValue(Int::class.java)
            ?: 0

        val pm25 = l.child("pm2_5").getValue(Double::class.java)
            ?: l.child("pm2_5").getValue(Int::class.java)?.toDouble()
            ?: 0.0

        val pm10 = l.child("pm10").getValue(Double::class.java)
            ?: l.child("pm10").getValue(Int::class.java)?.toDouble()
            ?: 0.0

        val ts = l.child("timestamp").getValue(Long::class.java)
            ?: l.child("timestamp").getValue(Int::class.java)?.toLong()
            ?: 0L

        // classify() ignora automaticamente parâmetros com valor <= 0
        val result = AirQualityClassifier.classify(co2, temp, umidade, pm25, pm10)

        return lab.copy(
            co2Ppm         = co2,
            tempC          = temp,
            humidityPct    = umidade,
            pm25           = pm25,
            pm10           = pm10,
            lastUpdate     = formatTimestamp(ts),
            quality        = result.quality,
            recommendation = result.recommendation,
            analysisItems  = result.analysisItems,
            isCollecting   = coletando && salaSelecionada.equals(lab.id, ignoreCase = true)
        )
    }

    fun toggleCollection(labId: String) {
        val jaColetandoEsse = coletando && salaSelecionada.equals(labId, ignoreCase = true)
        if (jaColetandoEsse) {
            db.child("dispositivo").child("coletando").setValue(false)
        } else {
            db.child("dispositivo").child("sala_selecionada").setValue(labId.uppercase())
            db.child("dispositivo").child("coletando").setValue(true)
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp == 0L) return "--/-- --:--"
        return SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault()).format(Date(timestamp * 1000L))
    }
}