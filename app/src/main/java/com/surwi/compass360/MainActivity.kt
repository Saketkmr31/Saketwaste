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
import com.surwi.compass360.domain.panchang.DrikPanchangService
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
    private var selectedPanchangDate: LocalDate = LocalDate.now()
    private var selectedVastuRoom: String = "Prayer / पूजा कक्ष"
    private var selectedMuhuratEvent: String = "Griha Pravesh / गृह प्रवेश"
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
        feature("Daily Panchang", "Sunrise Tithi · lunar month · sunrise/sunset", "▦", "panchang")
        section("JYOTISH & PERSONAL TOOLS")
        feature("Janma Kundli", "Rashi · Lagna · Nakshatra · Dasha", "✧", "kundli", false)
        feature("Numerology", "Mulank · Bhagyank · name number", "№", "numerology")
        feature("Muhurat", "Traditional timing calculations", "◷", "muhurat", false)
        feature("Hindu festivals", "Regional calendar & observances", "❀", "festivals", false)
        val savedProfile = BirthProfileStore(this).load()
        if (savedProfile != null) {
            card(label("YOUR SAVED BIRTH PROFILE", 10f, gold, true),
                label(savedProfile.getOrNull(0).orEmpty(), 17f, ivory, true),
                label("DOB: " + savedProfile.getOrNull(1).orEmpty() + " · " + savedProfile.getOrNull(3).orEmpty(), 12f, muted),
                button("Open my Kundli inputs", false) { go("kundli") })
        } else {
            card(label("PERSONALISE YOUR EXPERIENCE", 10f, gold, true),
                label("Save your birth details once. Your DOB and name can then prefill Numerology; birth-place details are shown in your Kundli workspace.", 12f, ivory),
                button("Create birth profile", false) { go("profile") })
        }
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
            label(d.generalNote, 12f, muted),
            label("Magnetic compass reading only. Calibrate the phone; a phone compass is not a building survey.", 11f, muted))
        section("ROOM PLACEMENT GUIDE · कक्ष दिशा मार्गदर्शिका")
        val rooms = listOf("Prayer / पूजा कक्ष", "Kitchen / रसोई", "Master bedroom / मुख्य शयनकक्ष", "Living room / बैठक", "Study / अध्ययन", "Guest room / अतिथि कक्ष", "Dining / भोजन कक्ष", "Storage / भंडारण", "Main entrance / मुख्य द्वार")
        val advice = mapOf(
            rooms[0] to ("NE · ईशान" to "Common modern guidance prefers north-east for prayer or meditation."),
            rooms[1] to ("SE · आग्नेय" to "Common modern guidance associates south-east with Agni and kitchen activity. Fire safety and ventilation come first."),
            rooms[2] to ("SW · नैऋत्य" to "Many contemporary schools associate south-west with stability and the main bedroom."),
            rooms[3] to ("N / E · उत्तर / पूर्व" to "North or east are commonly suggested for shared living spaces."),
            rooms[4] to ("E / NE · पूर्व / ईशान" to "East or north-east are common suggestions for study and focused work."),
            rooms[5] to ("NW · वायव्य" to "North-west is commonly associated with guest rooms in modern Vastu."),
            rooms[6] to ("W / NW · पश्चिम / वायव्य" to "West or north-west are used in some modern dining-room recommendations."),
            rooms[7] to ("S / W · दक्षिण / पश्चिम" to "South or west are sometimes suggested for heavier storage. Check floor and shelf load limits."),
            rooms[8] to ("Plot-specific" to "Entrance recommendations vary by school, plot geometry and road orientation; compass direction alone is insufficient.")
        )
        content.addView(button("Choose room · " + selectedVastuRoom, false) {
            android.app.AlertDialog.Builder(this).setTitle("Select room / कक्ष चुनें")
                .setItems(rooms.toTypedArray()) { _, which -> selectedVastuRoom = rooms[which]; render() }.show()
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        val selectedAdvice = advice[selectedVastuRoom]!!
        card(label("TRADITIONAL DIRECTION", 10f, gold, true),
            label(selectedAdvice.first, 21f, ivory, true),
            label(selectedAdvice.second, 13f, ivory),
            label("Practical safety, accessibility, ventilation, daylight and building codes override generic Vastu suggestions.", 12f, muted))
        section("16 DIRECTIONS")
        VastuDirectionCatalog.directions.forEach { direction ->
            card(label(direction.code + " · " + direction.english + " / " + direction.hindi, 14f, gold, true),
                label(direction.traditionalAssociation, 12f, ivory),
                label(direction.generalNote, 12f, muted))
        }
        content.addView(label("Common modern Vastu associations are shown for education. Schools and classical sources differ. This is not an architectural, structural, fire or electrical assessment. Never demolish or modify a building based only on this screen.", 12f, muted))
    }

    private fun panchang() {
        header("Hindu Panchang", "Daily sunrise calendar · दैनिक पंचांग")
        card(label("SELECTED CITY · चुना हुआ शहर", 10f, gold, true),
            label(city, 20f, ivory, true),
            label("Selected date: " + selectedPanchangDate.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale.ENGLISH)), 12f, muted),
            button("Change date / तारीख चुनें", false) {
                android.app.DatePickerDialog(this, { _, year, month, day ->
                    selectedPanchangDate = LocalDate.of(year, month + 1, day)
                    render()
                }, selectedPanchangDate.year, selectedPanchangDate.monthValue - 1, selectedPanchangDate.dayOfMonth).show()
            })
        val dateNav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        dateNav.addView(button("← Previous day", false) { selectedPanchangDate = selectedPanchangDate.minusDays(1); render() }, LinearLayout.LayoutParams(0, -2, 1f))
        dateNav.addView(button("Next day →", false) { selectedPanchangDate = selectedPanchangDate.plusDays(1); render() }, LinearLayout.LayoutParams(0, -2, 1f))
        content.addView(dateNav, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        try {
            val day = DrikPanchangService.forDate(selectedPanchangDate, city, java.time.ZoneId.of("Asia/Kolkata"))
            card(label("TITHI AT LOCAL SUNRISE · सूर्योदय की तिथि", 10f, gold, true),
                label(day.tithiDisplay, 21f, ivory, true),
                label(day.pakshaHindi, 13f, gold),
                label("Lunar month system: Purnimant · Sunrise convention: Upper limb", 11f, muted))
            card(label("SUNRISE & SUNSET · सूर्योदय और सूर्यास्त", 10f, gold, true),
                label("Sunrise / सूर्योदय     " + day.sunriseLocal, 16f, ivory, true),
                label("Sunset / सूर्यास्त       " + day.sunsetLocal, 16f, ivory, true))
            card(label("VARA · WEEKDAY", 10f, gold, true),
                label(selectedPanchangDate.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH), 17f, ivory, true),
                label(selectedPanchangDate.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.forLanguageTag("hi-IN")), 13f, muted))
            card(label("CALCULATION DETAILS", 10f, gold, true),
                label("Engine: tithi-engine 5.0.0 · City registry: " + day.canonicalCity, 11f, ivory),
                label("Timezone: Asia/Kolkata · Sunrise-anchored tithi for selected civil date.", 11f, muted),
                label("This is the limited local Tithi engine. Full five-limb Panchang, exact transition times and Rahu Kaal will switch to the commercial provider only after its licensed gateway is activated.", 12f, gold))
        } catch (e: Exception) {
            card(label("CITY NOT SUPPORTED BY THE VERIFIED ENGINE", 12f, gold, true),
                label("Selected city: " + city, 15f, ivory, true),
                label("Choose a supported city name. The app will not silently substitute another location.", 12f, muted))
            content.addView(button("Choose supported city") { chooseCity() })
        }
        content.addView(button("Open IMD Rashtriya Panchang / आधिकारिक पंचांग", false) {
            openUrl("https://packolkata.imd.gov.in/panchang/en")
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(10) })
    }

    private fun profile() {
        header("Birth Profile", "Private on-device profile · जन्म विवरण")
        content.addView(label("Enter your date of birth and birth city. Birth time and latitude/longitude are OPTIONAL. If unknown, leave them blank: a city-based approximate location can be used and the chart must be marked approximate. Exact time and coordinates improve accuracy.", 13f, ivory))
        val current = BirthProfileStore(this).load()
        val name = field("Profile name", current?.getOrNull(0) ?: "")
        val dob = field("DOB · YYYY-MM-DD", current?.getOrNull(1) ?: "")
        val time = field("Birth time · optional · HH:MM (24-hour)", current?.getOrNull(2) ?: "")
        val place = field("Birth city / place", current?.getOrNull(3) ?: "")
        val latitude = field("Birth latitude · optional · e.g. 23.2599", current?.getOrNull(4) ?: "")
        val longitude = field("Birth longitude · optional · e.g. 77.4126", current?.getOrNull(5) ?: "")
        val zone = field("Timezone · optional · default Asia/Kolkata", current?.getOrNull(6) ?: "Asia/Kolkata")
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
                    require(name.text.isNotBlank() && place.text.isNotBlank()) { "Enter profile name and birth city/place. Time and coordinates are optional." }
                    val birthTime = time.text.toString().trim()
                    require(birthTime.isBlank() || birthTime.matches(Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$"))) { "If known, birth time must be HH:MM." }
                    val latText = latitude.text.toString().trim()
                    val lonText = longitude.text.toString().trim()
                    require(latText.isBlank() == lonText.isBlank()) { "Enter both latitude and longitude, or leave both blank." }
                    if (latText.isNotBlank()) {
                        val lat = latText.toDouble()
                        val lon = lonText.toDouble()
                        require(lat in -90.0..90.0) { "Latitude must be between -90 and 90." }
                        require(lon in -180.0..180.0) { "Longitude must be between -180 and 180." }
                    }
                    val timezone = zone.text.toString().trim().ifBlank { "Asia/Kolkata" }
                    java.time.ZoneId.of(timezone)
                    BirthProfileStore(this).save(name.text.toString(), parsed.toString(), birthTime, place.text.toString(), latText, lonText, timezone)
                    val quality = if (birthTime.isBlank() || latText.isBlank()) "Approximate profile saved. Exact time/coordinates can be added later." else "Profile saved with supplied birth details."
                    Toast.makeText(this, quality, Toast.LENGTH_LONG).show()
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
        content.addView(label("Required: date of birth, profile name and birth city. Optional: birth time, latitude, longitude and timezone. Missing time/location reduces chart precision; any derived result must be labelled approximate. You can add details later.\nNo account or server upload is used for this local profile. Cloud sync would require separate consent.", 12f, muted))
    }

    private fun numerology() {
        header("Numerology", "Mulank · Bhagyank · Name number")
        content.addView(label("Enter DOB as YYYY-MM-DD. Name number uses a Chaldean letter-value table. The arithmetic convention is shown with the result.", 13f, muted))
        val savedProfile = BirthProfileStore(this).load()
        val fullName = field("Full name (optional)", savedProfile?.getOrNull(0) ?: "")
        val dob = field("DOB · YYYY-MM-DD, e.g. 1998-07-31", savedProfile?.getOrNull(1) ?: "")
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
        val saved = BirthProfileStore(this).load()
        if (saved == null) {
            card(label("BIRTH PROFILE NOT SET UP", 11f, gold, true),
                label("Create your profile once. Your saved DOB, birth city, optional birth time and optional coordinates will appear here automatically.", 13f, ivory),
                button("Set up birth profile") { go("profile") })
        } else {
            val time = saved.getOrNull(2).orEmpty()
            val lat = saved.getOrNull(4).orEmpty()
            val lon = saved.getOrNull(5).orEmpty()
            val zone = saved.getOrNull(6).orEmpty().ifBlank { "Asia/Kolkata" }
            val completeness = when {
                time.isNotBlank() && lat.isNotBlank() && lon.isNotBlank() -> "INPUTS COMPLETE · supplied birth time and coordinates"
                time.isBlank() && lat.isBlank() -> "APPROXIMATE INPUTS · birth time and coordinates not supplied"
                time.isBlank() -> "APPROXIMATE TIME · birth time not supplied"
                else -> "APPROXIMATE LOCATION · coordinates not supplied"
            }
            card(label("YOUR SAVED BIRTH DETAILS", 11f, gold, true),
                label(saved.getOrNull(0).orEmpty(), 18f, ivory, true),
                label("Date of birth: " + saved.getOrNull(1).orEmpty(), 13f, ivory),
                label("Birth place: " + saved.getOrNull(3).orEmpty(), 13f, ivory),
                label("Birth time: " + if (time.isBlank()) "Not provided" else time, 13f, ivory),
                label("Coordinates: " + if (lat.isNotBlank()) "$lat, $lon" else "Not provided", 13f, ivory),
                label("Timezone: " + zone, 13f, ivory),
                label(completeness, 11f, gold),
                button("Edit birth profile") { go("profile") })
        }
        card(label("COMMERCIAL ENGINE SELECTED · ACTIVATION PENDING", 11f, gold, true),
            label("TantraGurukul / Tantrakulam Commercial API is the selected provider. The paid production key, authenticated gateway and privacy consent are not active yet, so this beta does not calculate a Kundli. We will not invent Rashi, Lagna, Nakshatra, houses or Dasha.", 13f, ivory))
        section("CHART MODULES TO ENABLE")
        listOf("Janma Rashi / Moon sign", "Lagna / Ascendant", "27 Nakshatras and Pada", "Navagraha longitudes", "12 Bhava / houses", "Navamsha and divisional charts", "Vimshottari Dasha and Antardasha", "Ashtakoota compatibility").forEach {
            content.addView(label("• " + it, 13f, ivory))
        }
        content.addView(label("Planned default: sidereal zodiac with Lahiri ayanamsha. A saved profile does not itself calculate a horoscope; every result must be generated by a validated ephemeris and disclose precision, location, timezone and method.", 12f, muted))
        if (saved == null) content.addView(button("Set up birth profile") { go("profile") })
    }

    private fun muhurat() {
        header("Muhurat Planner", "Event planning workspace · मुहूर्त योजना")
        content.addView(label("Choose an event and prepare details for a tradition-specific Muhurta review. This version does not rank auspicious time windows; that requires all five Panchang limbs, local sunrise and a declared rule set.", 13f, muted))
        val events = listOf("Griha Pravesh / गृह प्रवेश", "Vivah / विवाह", "Vehicle purchase / वाहन", "Property registration / संपत्ति", "Travel / यात्रा", "Business start / नया कार्य")
        content.addView(button("Event: " + selectedMuhuratEvent, false) {
            android.app.AlertDialog.Builder(this).setTitle("Choose event")
                .setItems(events.toTypedArray()) { _, which -> selectedMuhuratEvent = events[which]; render() }.show()
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        val targetDate = field("Preferred date · YYYY-MM-DD", selectedPanchangDate.toString())
        val eventCity = field("City / शहर", city)
        val tradition = field("Tradition (e.g. Smarta / Vaishnava / regional)", "General")
        listOf(targetDate, eventCity, tradition).forEach { content.addView(it, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }) }
        card(label("REQUEST SUMMARY", 10f, gold, true),
            label(selectedMuhuratEvent, 16f, ivory, true),
            label("A full recommendation must validate tithi, nakshatra, yoga, karana, weekday, local sunrise/sunset, Rahu Kaal and event-specific tradition rules.", 12f, muted))
        content.addView(button("Save planning details") {
            try {
                val date = LocalDate.parse(targetDate.text.toString().trim())
                getSharedPreferences("muhurat_requests", MODE_PRIVATE).edit()
                    .putString("date", date.toString()).putString("city", eventCity.text.toString().trim())
                    .putString("event", selectedMuhuratEvent).putString("tradition", tradition.text.toString().trim()).apply()
                Toast.makeText(this, "Planning details saved on this device. No auspicious time has been calculated.", Toast.LENGTH_LONG).show()
            } catch (_: Exception) { Toast.makeText(this, "Enter date as YYYY-MM-DD.", Toast.LENGTH_SHORT).show() }
        })
        card(label("ENGINE STATUS", 10f, gold, true),
            label("Commercial provider selected; activation is pending. No auspicious or inauspicious time is fabricated until the paid engine, authenticated gateway and event-rule validation are live.", 13f, ivory))
        content.addView(button("Check official IMD Panchang") { openUrl("https://packolkata.imd.gov.in/panchang/en") })
    }

    private fun festivals() {
        header("Tithi & Vrat Calendar", "Tithi-based observance finder · तिथि व्रत कैलेंडर")
        content.addView(label("Find sunrise tithi matches for common observance categories. These are tithi matches, not final regional festival declarations. Smarta, Vaishnava and regional rules may assign different dates.", 13f, muted))
        try {
            val matches = mutableListOf<Pair<LocalDate, String>>()
            for (offset in 0..120) {
                val date = LocalDate.now().plusDays(offset.toLong())
                val info = DrikPanchangService.forDate(date, city, java.time.ZoneId.of("Asia/Kolkata"))
                val t = info.tithiDisplay.lowercase(Locale.ROOT)
                val category = when {
                    "ekadashi" in t -> "Ekadashi / एकादशी"
                    "purnima" in t -> "Purnima / पूर्णिमा"
                    "amavasya" in t -> "Amavasya / अमावस्या"
                    "chaturthi" in t -> "Chaturthi / चतुर्थी"
                    "trayodashi" in t -> "Trayodashi / त्रयोदशी"
                    "ashtami" in t -> "Ashtami / अष्टमी"
                    "navami" in t -> "Navami / नवमी"
                    else -> null
                }
                if (category != null) matches.add(date to (category + " · " + info.tithiDisplay))
            }
            matches.forEach { (date, description) ->
                card(label(date.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH)), 12f, gold, true),
                    label(description, 15f, ivory, true))
            }
            card(label("METHOD & LIMITATION", 10f, gold, true),
                label("Engine: tithi-engine 5.0.0 · sunrise tithi · Purnimant month · upper-limb sunrise convention · city: " + city, 11f, ivory),
                label("This is a tithi observance aid, not a verified festival-date engine. Verify festival dates, fasting rules and puja Muhurta with the relevant regional Panchang and sampradaya.", 12f, muted))
        } catch (e: Exception) {
            card(label("CITY NOT SUPPORTED", 12f, gold, true), label(e.message ?: "Choose a supported city.", 12f, ivory))
            content.addView(button("Choose city") { chooseCity() })
        }
        content.addView(button("Verify with IMD Rashtriya Panchang") { openUrl("https://packolkata.imd.gov.in/panchang/en") })
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
