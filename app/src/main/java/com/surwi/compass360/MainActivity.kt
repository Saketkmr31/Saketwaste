package com.surwi.compass360

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Surface
import android.view.WindowManager
import android.widget.TextView
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : Activity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private lateinit var directionText: TextView
    private lateinit var degreesText: TextView
    private lateinit var headingText: TextView
    private lateinit var accuracyText: TextView
    private lateinit var statusText: TextView
    private lateinit var compassView: CompassView
    private val gravity = FloatArray(3)
    private val magnetic = FloatArray(3)
    private var hasGravity = false
    private var hasMagnetic = false
    private var magneticAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE
    private var lastHeading = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        directionText = findViewById(R.id.direction)
        degreesText = findViewById(R.id.degrees)
        headingText = findViewById(R.id.headingValue)
        accuracyText = findViewById(R.id.accuracyValue)
        statusText = findViewById(R.id.sensorStatus)
        compassView = findViewById(R.id.compassView)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val missing = mutableListOf<String>()
        if (accelerometer == null) missing.add("accelerometer")
        if (magnetometer == null) missing.add("magnetometer")
        if (missing.isNotEmpty()) {
            statusText.text = "Required sensor unavailable: ${missing.joinToString()}"
            accuracyText.text = "Unavailable"
        } else {
            statusText.text = "Sensors detected · Waiting for readings"
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                for (i in 0..2) gravity[i] = gravity[i] * 0.8f + event.values[i] * 0.2f
                hasGravity = true
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                for (i in 0..2) magnetic[i] = event.values[i]
                hasMagnetic = true
            }
        }
        if (!hasGravity || !hasMagnetic) return
        val rotation = FloatArray(9)
        val inclination = FloatArray(9)
        if (!SensorManager.getRotationMatrix(rotation, inclination, gravity, magnetic)) {
            statusText.text = "Unable to calculate heading · Check magnetic interference"
            return
        }
        val remapped = FloatArray(9)
        val axes = when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        if (!SensorManager.remapCoordinateSystem(rotation, axes.first, axes.second, remapped)) return
        val orientation = FloatArray(3)
        SensorManager.getOrientation(remapped, orientation)
        var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
        azimuth = (azimuth + 360f) % 360f
        val delta = ((azimuth - lastHeading + 540f) % 360f) - 180f
        lastHeading = (lastHeading + delta * 0.22f + 360f) % 360f
        renderHeading(lastHeading)
    }

    private fun renderHeading(heading: Float) {
        val rounded = heading.roundToInt() % 360
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val label = directions[((rounded + 22) / 45) % 8]
        directionText.text = label
        degreesText.text = String.format(Locale.getDefault(), "%.1f°", heading)
        headingText.text = String.format(Locale.getDefault(), "%.1f°", heading)
        compassView.setHeading(heading)
        accuracyText.text = when (magneticAccuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "High"
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Medium"
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Low"
            else -> "Unreliable"
        }
        statusText.text = "Live · Accelerometer + magnetometer"
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticAccuracy = accuracy
            accuracyText.text = when (accuracy) {
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "High"
                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Medium"
                SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Low"
                else -> "Unreliable"
            }
        }
    }
}
