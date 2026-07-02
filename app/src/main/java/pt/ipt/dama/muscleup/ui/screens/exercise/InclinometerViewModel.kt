package pt.ipt.dama.muscleup.ui.screens.exercise

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.asin
import kotlin.math.sqrt

/** Calcula o ângulo de inclinação (pitch, em graus) a partir do acelerómetro. */
class InclinometerViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val isSensorAvailable = accelerometer != null

    private val _angleDegrees = MutableStateFlow(0f)
    val angleDegrees: StateFlow<Float> = _angleDegrees.asStateFlow()

    private var lastUpdateAt = 0L
    private val updateIntervalMillis = 100L

    fun start() {
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    fun stop() = sensorManager.unregisterListener(this)

    override fun onSensorChanged(event: SensorEvent) {
        val now = System.currentTimeMillis()
        if (now - lastUpdateAt < updateIntervalMillis) return
        lastUpdateAt = now

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val norm = sqrt((x * x + y * y + z * z).toDouble())
        if (norm == 0.0) return
        // Ângulo entre o eixo Y do telemóvel e a horizontal (vetor gravidade).
        _angleDegrees.value = Math.toDegrees(asin((y / norm).coerceIn(-1.0, 1.0))).toFloat()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onCleared() = stop()
}


