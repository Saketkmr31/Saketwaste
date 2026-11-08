package com.surwi.compass360.domain.panchang

import com.misrilibrary.tithi.City
import com.misrilibrary.tithi.Panchang
import com.misrilibrary.tithi.model.MonthSystem
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DailyTithi(
    val canonicalCity: String,
    val tithiDisplay: String,
    val pakshaHindi: String,
    val sunriseLocal: String,
    val sunsetLocal: String
)

/** Sunrise-anchored Tithi and solar times; not a complete five-limb Panchang. */
object DrikPanchangService {
    private val engine = Panchang(MonthSystem.PURNIMANT)
    private val clock = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)

    fun forDate(date: LocalDate, cityName: String, zone: ZoneId): DailyTithi {
        val city = requireNotNull(City.tryOf(cityName)) {
            "Unsupported city. Select a city in the calculation engine's registry."
        }
        val info = engine.tithiOnDate(date, city)
        val sunrise = engine.sunrise(date, city).atZone(zone).format(clock)
        val sunset = engine.sunset(date, city).atZone(zone).format(clock)
        val paksha = if (info.paksha.name == "SHUKLA") {
            "शुक्ल पक्ष · Shukla Paksha"
        } else {
            "कृष्ण पक्ष · Krishna Paksha"
        }
        return DailyTithi(city.name, info.displayName, paksha, sunrise, sunset)
    }
}