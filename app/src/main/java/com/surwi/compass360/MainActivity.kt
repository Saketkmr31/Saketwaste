package com.surwi.compass360

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.Surface
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.*
import com.surwi.compass360.domain.numerology.NumerologyCalculator
import com.surwi.compass360.domain.numerology.ReductionRule
import com.surwi.compass360.domain.vastu.VastuDirectionCatalog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.*

/** Native app shell for Vedic Compass 360. Unconfigured astronomy is never shown as live data. */
class MainActivity : Activity(), SensorEventListener {
    private val bg = Color.rgb(31, 12, 8)
    private val panel = Color.rgb(57, 25, 17)
    private val panel2 = Color.rgb(74, 31, 21)
    private val gold = Color.rgb(255, 205, 112)
    private val ivory = Color.rgb(255, 241, 212)
    private val muted = Color.rgb(222, 190, 151)
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var nav: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var sensorManager: SensorManager
    private var rotationVector: Sensor? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private val gravity = FloatArray(3)
    private val magnetic = FloatArray(3)
    private var hasGravity = false
    private var hasMagnetic = false
    private var accuracy = SensorManager.SENSOR_STATUS_UNRELIABLE
    private var filteredHeading: Double? = null
    private var lastUi = 0L
    private var bearing = 0.0
    private var headingLabel = "N"
    private var screen = "home"
    private var city = "Bhopal"
    private var headingView: TextView? = null
    private var liveView: TextView? = null
    private var accuracyView: TextView? = null
    private var dialView: CompassView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        city = getSharedPreferences("vedic_compass_settings", MODE_PRIVATE).getString("city", "Bhopal") ?: "Bhopal"
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        buildShell()
        render()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()
    private fun rounded(color: Int, radius: Int = 16) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), Color.argb(40, 255, 205, 112))
    }

    private fun buildShell() {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg); clipToPadding = false }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(14), dp(12), dp(14), dp(12)) }
        brand.addView(TextView(this).apply {
            text = "卐  VEDIC COMPASS 360"
            textSize = 22f
            setTextColor(gold)
            typeface = Typeface.create("serif", Typeface.BOLD)
            gravity = Gravity.CENTER
        })
        brand.addView(TextView(this).apply {
            text = "वास्तु  •  ज्योतिष  •  पंचांग     |     VASTU  •  JYOTISH  •  PANCHANG"
            textSize = 9f
            letterSpacing = .06f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, 0)
        })
        scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(5), dp(18), dp(28)) }
        scroll.addView(content)
        nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(43, 18, 12))
            setPadding(dp(4), dp(6), dp(4), dp(6))
        }
        root.addView(brand, LinearLayout.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(nav, LinearLayout.LayoutParams(-1, -2))
        root.setOnApplyWindowInsetsListener { view, insets ->
            val top = if (android.os.Build.VERSION.SDK_INT >= 30) insets.getInsets(WindowInsets.Type.statusBars()).top else insets.systemWindowInsetTop
            val bottom = if (android.os.Build.VERSION.SDK_INT >= 30) insets.getInsets(WindowInsets.Type.navigationBars()).bottom else insets.systemWindowInsetBottom
            view.setPadding(0, top, 0, 0)
            nav.setPadding(dp(4), dp(6), dp(4), dp(6) + bottom)
            insets
        }
        setContentView(root)
    }

    private fun label(value: String, size: Float = 14f, color: Int = ivory, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setLineSpacing(dp(3).toFloat(), 1.04f)
        }

    private fun header(value: String, subtitle: String = "") {
        content.addView(label(value, 23f, gold, true), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10); bottomMargin = dp(4) })
        if (subtitle.isNotBlank()) content.addView(label(subtitle, 12f, muted), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
    }

    private fun section(value: String) {
        content.addView(label(value, 15f, gold, true), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(15); bottomMargin = dp(8) })
    }

    private fun card(vararg items: View) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(panel)
            setPadding(dp(15), dp(13), dp(15), dp(13))
        }
        items.forEach { box.addView(it, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(4) }) }
        content.addView(box, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
    }

    private fun button(caption: String, primary: Boolean = true, action: () -> Unit): Button =
        Button(this).apply {
            text = caption
            isAllCaps = false
            textSize = 13f
            setTextColor(if (primary) bg else gold)
            background = rounded(if (primary) gold else panel2, 13)
            setOnClickListener { action() }
        }

    private fun field(hintText: String, value: String = "") = EditText(this).apply {
        hint = hintText
        setText(value)
        setSingleLine(true)
        textSize = 14f
        setTextColor(ivory)
        setHintTextColor(muted)
        setPadding(dp(12), dp(10), dp(12), dp(10))
        background = rounded(panel2, 12)
    }

    private fun feature(title: String, subtitle: String, icon: String, target: String, ready: Boolean = true) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(panel, 15)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            isClickable = true
            isFocusable = true
            setOnClickListener { go(target) }
        }
        box.addView(label(icon, 22f, gold, true))
        box.addView(label(title, 15f, ivory, true))
        box.addView(label(subtitle, 11f, muted))
        if (!ready) box.addView(label("Calculation engine not connected", 10f, gold))
        content.addView(box, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
    }

    private fun render() {
        content.removeAllViews()
        headingView = null
        liveView = null
        accuracyView = null
        dialView = null
        when (screen) {
            "home" -> home()
            "compass" -> compass()
            "vastu" -> vastu()
            "panchang" -> panchang()
            "profile" -> profile()
            "numerology" -> numerology()
            "kundli" -> kundli()
            "muhurat" -> muhurat()
            "festivals" -> festivals()
            "settings" -> settings()
        }
        renderNav()
        scroll.scrollTo(0, 0)
    }

    private fun home() {
        header("Namaste 🙏", "Your Vedic companion · आपका वैदिक साथी")
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.ENGLISH))
        card(label("TODAY  ·  " + today, 11f, gold, true),
            label("Selected city / चुना हुआ शहर", 11f, muted),
            label("📍 " + city, 20f, ivory, true),
            button("Change city / शहर बदलें", false) { chooseCity() })
        section("DAILY TOOLS  ·  दैनिक साधन")
        feature("Compass 360", "Live direction · calibration", "◉", "compass")
        feature("Vastu Shastra", "16 directions · room guidance", "⌖", "vastu")
        feature("Daily Panchang", "Tithi · Nakshatra · Yoga · Karana", "▦", "panchang", false)
        section("JYOTISH & PERSONAL TOOLS")
        feature("Janma Kundli", "Rashi · Lagna · Nakshatra · Dasha", "✧", "kundli", false)
        feature("Numerology", "Mulank · Bhagyank · name number", "№", "numerology")
        feature("Muhurat", "Traditional timing calculations", "◷", "muhurat", false)
        feature("Hindu festivals", "Regional calendar & observances", "❀", "festivals", false)
        card(label("OFFLINE-FIRST", 11f, gold, true),
            label("Compass, Vastu reference and numerology work on-device. Panchang and Kundli values stay unavailable until a validated astronomy engine is connected.", 12f, ivory))
    }

    private fun compass() {
        header("Compass 360", "Magnetic direction · चुंबकीय दिशा")
        val h = label(headingLabel + "  ·  " + bearing.roundToInt() + "°", 30f, ivory, true).apply { gravity = Gravity.CENTER }
        headingView = h
        card(label("LIVE BEARING", 11f, gold, true), h)
        val dial = CompassView(this).apply { setBackgroundColor(bg); setHeading(bearing.toFloat()) }
        dialView = dial
        content.addView(dial, LinearLayout.LayoutParams(-1, dp(320)).apply { bottomMargin = dp(10) })
        val live = label("Live heading: " + String.format(Locale.US, "%.1f°", bearing), 16f, ivory, true)
        liveView = live
        val acc = label(accuracyLabel(), 16f, gold, true)
        accuracyView = acc
        card(label("SENSOR ACCURACY", 10f, gold, true), live, acc)
        content.addView(label("Calibration: move the phone slowly in a figure-eight. Keep it away from magnetic covers, speakers, vehicles and steel furniture.", 12f, muted))
        content.addView(label("This shows magnetic north. True north requires location-based magnetic declination correction; it is not enabled in this version.", 12f, muted))
    }

    private fun vastu() {
        header("Vastu Shastra", "16-sector direction explorer · वास्तु दिशा चक्र")
        val d = VastuDirectionCatalog.atBearing(bearing)
        card(label("CURRENT PHONE BEARING", 10f, gold, true),
            label(d.code + " · " + d.english + " · " + d.hindi, 18f, ivory, true),
            label(d.generalNote, 12f, muted))
        section("16 DIRECTIONS")
        VastuDirectionCatalog.directions.forEach { direction ->
            card(label(direction.code + " · " + direction.english + " / " + direction.hindi, 14f, gold, true),
                label(direction.traditionalAssociation, 12f, ivory),
                label(direction.generalNote, 12f, muted))
        }
        content.addView(label("These are common Vastu associations. Schools and classical source texts differ; this is not an architectural or structural safety assessment.", 12f, muted))
    }

    private fun panchang() {
        header("Hindu Panchang", "Daily calendar · दैनिक पंचांग")
        card(label("SELECTED CITY", 10f, gold, true), label(city, 20f, ivory, true),
            label(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)), 12f, muted),
            label("Live Panchang calculations are not connected in this build.", 14f, gold, true),
            label("We will not guess today's Tithi, Nakshatra, Yoga, Karana or sunrise. These require validated astronomy and local time/location rules.", 12f, ivory))
        section("PANCHANG LIMBS")
        listOf("Vara / वार", "Tithi / तिथि", "Nakshatra / नक्षत्र", "Yoga / योग", "Karana / करण", "Paksha / पक्ष", "Sunrise & sunset / सूर्योदय-सूर्यास्त", "Moonrise & moonset / चन्द्रोदय-चन्द्रास्त").forEach {
            card(label(it, 14f, ivory, true), label("Awaiting verified city-specific calculation", 11f, muted))
        }
        content.addView(button("Open IMD Rashtriya Panchang / आधिकारिक पंचांग") {
            openUrl("https://mausam.imd.gov.in/responsive/rashtriyPanchang.php")
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        content.addView(label("IMD Rashtriya Panchang publishes national calendar and astronomical data. Local observance may vary by region and tradition.", 12f, muted))
    }

    private fun profile() {
        header("Birth Profile", "Private on-device profile · जन्म विवरण")
        content.addView(label("Kundli needs DOB, exact local birth time, birthplace, coordinates and timezone. Approximate birth time must be flagged.", 13f, ivory))
        val current = BirthProfileStore(this).load()
        val name = field("Profile name", current?.getOrNull(0) ?: "")
        val dob = field("DOB · YYYY-MM-DD", current?.getOrNull(1) ?: "")
        val time = field("Birth time · HH:MM (24-hour)", current?.getOrNull(2) ?: "")
        val place = field("Birth city / place", current?.getOrNull(3) ?: "")
        val latitude = field("Birth latitude · e.g. 23.2599", current?.getOrNull(4) ?: "")
        val longitude = field("Birth longitude · e.g. 77.4126", current?.getOrNull(5) ?: "")
        val zone = field("Timezone · e.g. Asia/Kolkata", current?.getOrNull(6) ?: "Asia/Kolkata")
        listOf(name, dob, time, place, latitude, longitude, zone).forEach {
            content.addView(it, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
        val consent = CheckBox(this).apply {
            text = "I agree to save this profile encrypted on this device only."
            setTextColor(ivory)
            buttonTintList = android.content.res.ColorStateList.valueOf(gold)
        }
        content.addView(consent)
        content.addView(button("Save private profile / सुरक्षित सहेजें") {
            if (!consent.isChecked) {
                Toast.makeText(this, "Confirm local storage consent first.", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    val parsed = LocalDate.parse(dob.text.toString().trim())
                    require(!parsed.isAfter(LocalDate.now())) { "DOB cannot be in the future." }
                    require(name.text.isNotBlank() && place.text.isNotBlank()) { "Enter profile name and birthplace." }
                    require(time.text.matches(Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$"))) { "Birth time must be HH:MM." }
                    val lat = latitude.text.toString().toDouble()
                    val lon = longitude.text.toString().toDouble()
                    require(lat in -90.0..90.0) { "Latitude must be between -90 and 90." }
                    require(lon in -180.0..180.0) { "Longitude must be between -180 and 180." }
                    java.time.ZoneId.of(zone.text.toString().ifBlank { "Asia/Kolkata" })
                    BirthProfileStore(this).save(name.text.toString(), parsed.toString(), time.text.toString(), place.text.toString(), lat.toString(), lon.toString(), zone.text.toString().ifBlank { "Asia/Kolkata" })
                    Toast.makeText(this, "Saved encrypted on this device.", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, e.message ?: "Check profile fields.", Toast.LENGTH_LONG).show()
                }
            }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        content.addView(button("Delete saved profile", false) {
            BirthProfileStore(this).delete()
            Toast.makeText(this, "Profile deleted.", Toast.LENGTH_SHORT).show()
            render()
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7) })
        content.addView(label("No account or server upload is used for this local profile. Cloud sync would require separate consent.", 12f, muted))
    }

    private fun numerology() {
        header("Numerology", "Mulank · Bhagyank · Name number")
        content.addView(label("Enter DOB as YYYY-MM-DD. Name number uses a Chaldean letter-value table. The arithmetic convention is shown with the result.", 13f, muted))
        val fullName = field("Full name (optional)")
        val dob = field("DOB · YYYY-MM-DD, e.g. 1998-07-31")
        content.addView(fullName, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        content.addView(dob, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        val result = label("Your result will appear here.", 14f, ivory)
        card(result)
        content.addView(button("Calculate / गणना करें") {
            try {
                val date = LocalDate.parse(dob.text.toString().trim())
                val name = fullName.text.toString().trim().ifBlank { null }
                val r = NumerologyCalculator.calculate(date, name, ReductionRule.ROOT_1_TO_9)
                result.text = "MULANK / मूलांक: " + r.mulank + "\nBHAGYANK / भाग्यांक: " + r.bhagyank +
                    (r.nameNumber?.let { "\nNAME NUMBER / नामांक (Chaldean): " + it } ?: "") +
                    "\n\nMethod: " + r.method + "\nTraditional symbolic arithmetic, not a scientific prediction."
            } catch (_: Exception) {
                Toast.makeText(this, "Enter a valid past DOB in YYYY-MM-DD format.", Toast.LENGTH_LONG).show()
            }
        }, LinearLayout.LayoutParams(-1, -2))
        section("TRADITIONAL SYMBOLIC ASSOCIATIONS")
        listOf("1 · Sun / सूर्य — initiative", "2 · Moon / चन्द्र — cooperation", "3 · Jupiter / गुरु — learning", "4 · Rahu — structure", "5 · Mercury / बुध — communication", "6 · Venus / शुक्र — harmony", "7 · Ketu — introspection", "8 · Saturn / शनि — discipline", "9 · Mars / मंगल — energy").forEach {
            content.addView(label(it, 13f, ivory))
        }
        content.addView(label("Numerology traditions differ. These associations are symbolic, not diagnoses or predictions.", 12f, muted))
    }

    private fun kundli() {
        header("Janma Kundli", "Vedic birth chart · जन्म कुंडली")
        card(label("ASTRONOMY ENGINE NOT CONNECTED", 11f, gold, true),
            label("No guessed Rashi, Lagna or Nakshatra. A validated ephemeris, timezone handling and declared ayanamsha are required.", 13f, ivory))
        section("TARGET CHART")
        listOf("Janma Rashi / Moon sign", "Lagna / Ascendant", "27 Nakshatras and Pada", "Navagraha longitudes", "12 Bhava / houses", "Navamsha and divisional charts", "Vimshottari Dasha and Antardasha", "Ashtakoota compatibility").forEach {
            content.addView(label("• " + it, 13f, ivory))
        }
        content.addView(label("Proposed default: sidereal zodiac with Lahiri ayanamsha. Every result must show engine, version, location, timezone and calculation time. Exact Lagna needs reliable birth time.", 12f, muted))
        content.addView(button("Set up birth profile") { go("profile") })
    }

    private fun muhurat() {
        header("Muhurat", "Traditional timing guidance · मुहूर्त")
        content.addView(label("Muhurta depends on event, time range, location, sunrise, Panchang elements and the selected tradition. No universal auspicious time is assumed.", 13f, muted))
        listOf("Griha Pravesh / गृह प्रवेश", "Vivah / विवाह", "Vehicle purchase / वाहन", "Property registration / संपत्ति", "Travel / यात्रा", "Business start / नया कार्य").forEach {
            card(label(it, 15f, ivory, true))
        }
        card(label("NOT YET LIVE", 11f, gold, true), label("The calculation engine and tradition-specific rule set are not configured. This screen will not fabricate Muhurat windows.", 13f, ivory))
        content.addView(button("Open official IMD Panchang") { openUrl("https://mausam.imd.gov.in/responsive/rashtriyPanchang.php") })
    }

    private fun festivals() {
        header("Hindu Festivals", "Regional calendar · हिन्दू त्योहार")
        content.addView(label("Festival dates and observance windows vary by region, sunrise convention and tradition. This build lists observance categories but does not invent dates.", 13f, muted))
        listOf("Ekadashi / एकादशी", "Purnima / पूर्णिमा", "Amavasya / अमावस्या", "Sankashti Chaturthi / संकष्टी", "Pradosh Vrat / प्रदोष", "Navratri / नवरात्रि", "Diwali / दीपावली", "Holi / होली", "Makar Sankranti / मकर संक्रांति", "Janmashtami / जन्माष्टमी", "Ganesh Chaturthi / गणेश चतुर्थी", "Regional temple festivals").forEach {
            card(label(it, 14f, ivory, true))
        }
        card(label("DATE ENGINE NOT CONNECTED", 11f, gold, true),
            label("Dates will appear only after local calculation and regional observance verification. Source and last-updated time will be visible.", 13f, ivory))
        content.addView(button("Open IMD Rashtriya Panchang") { openUrl("https://mausam.imd.gov.in/responsive/rashtriyPanchang.php") })
    }

    private fun settings() {
        header("Settings", "Preferences · सेटिंग")
        card(label("SELECTED CITY", 11f, gold, true), label(city, 18f, ivory, true), button("Change city") { chooseCity() })
        card(label("LANGUAGE", 11f, gold, true), label("Hindi + English resources are added. Full screen-by-screen translation and language switch are not complete yet.", 13f, ivory))
        card(label("PRIVACY", 11f, gold, true), label("Compass requires no GPS. City is manually selected. Birth profile is encrypted on-device and is not uploaded to a server in this build.", 13f, ivory))
        card(label("SOURCES", 11f, gold, true), button("IMD Rashtriya Panchang") { openUrl("https://mausam.imd.gov.in/responsive/rashtriyPanchang.php") })
    }

    private fun renderNav() {
        nav.removeAllViews()
        listOf(Triple("⌂", "Home", "home"), Triple("◎", "Compass", "compass"), Triple("⌖", "Vastu", "vastu"), Triple("▦", "Panchang", "panchang"), Triple("◉", "Profile", "profile")).forEach { item ->
            val column = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(2), dp(3), dp(2), dp(3))
                setOnClickListener { go(item.third) }
            }
            column.addView(label(item.first, 20f, if (screen == item.third) gold else muted, true).apply { gravity = Gravity.CENTER })
            column.addView(label(item.second, 10f, if (screen == item.third) gold else muted, screen == item.third).apply { gravity = Gravity.CENTER })
            nav.addView(column, LinearLayout.LayoutParams(0, -2, 1f))
        }
    }

    private fun chooseCity() {
        val edit = field("City name", city)
        android.app.AlertDialog.Builder(this)
            .setTitle("Choose city / शहर चुनें")
            .setView(edit)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val value = edit.text.toString().trim()
                if (value.isNotEmpty()) {
                    city = value
                    getSharedPreferences("vedic_compass_settings", MODE_PRIVATE).edit().putString("city", city).apply()
                    render()
                }
            }.show()
    }

    private fun accuracyLabel() = when (accuracy) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "High"
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Medium"
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Low"
        else -> "Calibrating"
    }

    private fun go(destination: String) { screen = destination; render() }

    private fun openUrl(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: Exception) { Toast.makeText(this, "No browser available.", Toast.LENGTH_SHORT).show() }
    }

    override fun onResume() {
        super.onResume()
        rotationVector?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        if (rotationVector == null) {
            accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        }
    }

    override fun onPause() { super.onPause(); sensorManager.unregisterListener(this) }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val matrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(matrix, event.values)
            publishHeading(matrix)
            return
        }
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                for (i in 0..2) gravity[i] = gravity[i] * .82f + event.values[i] * .18f
                hasGravity = true
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                for (i in 0..2) magnetic[i] = event.values[i]
                hasMagnetic = true
            }
        }
        if (hasGravity && hasMagnetic) {
            val matrix = FloatArray(9)
            if (SensorManager.getRotationMatrix(matrix, FloatArray(9), gravity, magnetic)) publishHeading(matrix)
        }
    }

    private fun publishHeading(matrix: FloatArray) {
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
            (previous + delta * .12 + 360.0) % 360.0
        }
        bearing = filteredHeading!!
        val whole = bearing.roundToInt() % 360
        val dirs = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        headingLabel = dirs[((whole + 11.25) / 22.5).toInt() % 16]
        val now = System.currentTimeMillis()
        if (now - lastUi >= 80L) {
            lastUi = now
            headingView?.text = headingLabel + "  ·  " + whole + "°"
            liveView?.text = "Live heading: " + String.format(Locale.US, "%.1f°", bearing)
            dialView?.setHeading(bearing.toFloat())
            accuracyView?.text = accuracyLabel()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, value: Int) {
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD || sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            accuracy = value
            accuracyView?.text = accuracyLabel()
        }
    }
}
