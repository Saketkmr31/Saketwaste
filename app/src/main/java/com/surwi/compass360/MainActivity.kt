package com.surwi.compass360

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Surface
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.*

class MainActivity : Activity(), SensorEventListener {
    private lateinit var sm: SensorManager
    private var rotationVector: Sensor? = null
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
    private var filteredHeading: Double? = null
    private var lastUiUpdate = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = android.graphics.Color.rgb(39, 16, 11)
        window.navigationBarColor = android.graphics.Color.rgb(39, 16, 11)
        setContentView(R.layout.activity_main)

        // Android 15+ enforces edge-to-edge for targetSdk 35. Add system-bar insets
        // to the scroll container so the last card can scroll above the nav bar.
        val scroll = findViewById<View>(R.id.mainScroll)
        val baseLeft = scroll.paddingLeft
        val baseTop = scroll.paddingTop
        val baseRight = scroll.paddingRight
        val baseBottom = scroll.paddingBottom
        scroll.setOnApplyWindowInsetsListener { view, insets ->
            val top = if (android.os.Build.VERSION.SDK_INT >= 30) insets.getInsets(WindowInsets.Type.statusBars()).top else insets.systemWindowInsetTop
            val bottom = if (android.os.Build.VERSION.SDK_INT >= 30) insets.getInsets(WindowInsets.Type.navigationBars()).bottom else insets.systemWindowInsetBottom
            view.setPadding(baseLeft, baseTop + top, baseRight, baseBottom + bottom + dp(24))
            insets
        }

        directionText = findViewById(R.id.direction)
        degreesText = findViewById(R.id.degrees)
        headingText = findViewById(R.id.headingValue)
        accuracyText = findViewById(R.id.accuracyValue)
        statusText = findViewById(R.id.sensorStatus)
        compassView = findViewById(R.id.compassView)
        findViewById<Button>(R.id.calculateNumbers).setOnClickListener { calculateNumerology() }

        sm = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationVector = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (rotationVector == null && (accelerometer == null || magnetometer == null)) {
            statusText.text = "Compass sensor unavailable on this device"
            accuracyText.text = "Unavailable"
        } else {
            statusText.text = if (rotationVector != null) "Sensor fusion ready" else "Fallback sensors ready"
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private fun calculateNumerology() {
        val input = findViewById<EditText>(R.id.dobInput).text.toString().trim()
        val result = findViewById<TextView>(R.id.numerologyResult)
        try {
            val date = LocalDate.parse(input)
            if (date.isAfter(LocalDate.now())) {
                Toast.makeText(this, "Date of birth cannot be in the future.", Toast.LENGTH_SHORT).show()
                return
            }
            val mulank = digitRoot(date.dayOfMonth)
            val bhagyank = digitRoot(input.filter { it.isDigit() }.sumOf { it.digitToInt() })
            result.text = "MULANK (Birth number): $mulank\nBHAGYANK (Destiny number): $bhagyank\n\nMethod: day of month and all digits of YYYY-MM-DD reduced to 1–9. Master numbers are reduced in this basic calculator. Interpretations are traditional numerology, not scientific facts."
        } catch (_: DateTimeParseException) {
            Toast.makeText(this, "Please enter date as YYYY-MM-DD.", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Please check the date and try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun digitRoot(number: Int): Int {
        var n = number
        while (n > 9) n = n.toString().sumOf { it.digitToInt() }
        return n
    }

    override fun onResume() {
        super.onResume()
        rotationVector?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        if (rotationVector == null) {
            accelerometer?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            magnetometer?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        }
    }

    override fun onPause() {
        super.onPause()
        sm.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val matrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(matrix, event.values)
            publishMatrix(matrix)
            return
        }
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                for (i in 0..2) gravity[i] = gravity[i] * 0.82f + event.values[i] * 0.18f
                hasGravity = true
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                for (i in 0..2) magnetic[i] = event.values[i]
                hasMagnetic = true
            }
        }
        if (!hasGravity || !hasMagnetic) return
        val matrix = FloatArray(9)
        val inclination = FloatArray(9)
        if (SensorManager.getRotationMatrix(matrix, inclination, gravity, magnetic)) publishMatrix(matrix)
    }

    private fun publishMatrix(matrix: FloatArray) {
        val remapped = FloatArray(9)
        val axes = when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        if (!SensorManager.remapCoordinateSystem(matrix, axes.first, axes.second, remapped)) return
        val orientation = FloatArray(3)
        SensorManager.getOrientation(remapped, orientation)
        val raw = (Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0
        val previous = filteredHeading
        filteredHeading = if (previous == null) raw else {
            val delta = ((raw - previous + 540.0) % 360.0) - 180.0
            (previous + delta * 0.12 + 360.0) % 360.0
        }
        val now = System.currentTimeMillis()
        if (now - lastUiUpdate >= 50L) {
            lastUiUpdate = now
            renderHeading(filteredHeading!!)
        }
    }

    private fun renderHeading(heading: Double) {
        val rounded = Math.round(heading).toInt() % 360
        val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        directionText.text = dirs[((rounded + 22) / 45) % 8]
        degreesText.text = String.format(Locale.getDefault(), "%03d°", rounded)
        headingText.text = String.format(Locale.getDefault(), "%.1f°", heading)
        compassView.setHeading(heading.toFloat())
        accuracyText.text = when (magneticAccuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "High"
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Medium"
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Low"
            else -> "Calibrating"
        }
        statusText.text = if (magneticAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
            "Live · Keep away from magnets and metal" else "Live · Magnetic north"
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD || sensor?.type == Sensor.TYPE_ROTATION_VECTOR) magneticAccuracy = accuracy
    }
}
