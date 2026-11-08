package com.surwi.compass360.domain.astronomy

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Provider contracts deliberately contain no placeholder astronomy.
 * A real implementation must declare its ephemeris, ayanamsha, timezone and provenance.
 */
data class CalculationMetadata(
    val engineName: String,
    val engineVersion: String,
    val ayanamsha: String?,
    val calculationTime: Instant,
    val sourceDescription: String,
    val accuracyNotes: String
)

data class PanchangDay(
    val date: LocalDate,
    val locationName: String,
    val zoneId: ZoneId,
    val sunrise: Instant?,
    val sunset: Instant?,
    val tithi: String?,
    val nakshatra: String?,
    val yoga: String?,
    val karana: String?,
    val vara: String,
    val metadata: CalculationMetadata
)

interface PanchangCalculator {
    suspend fun calculate(date: LocalDate, latitude: Double, longitude: Double, zoneId: ZoneId): PanchangDay
}

interface VedicChartCalculator {
    suspend fun calculate(
        birthInstant: Instant,
        latitude: Double,
        longitude: Double,
        ayanamsha: String
    ): VedicChart
}

data class VedicChart(
    val siderealAscendantDegrees: Double,
    val siderealMoonLongitudeDegrees: Double,
    val planetaryLongitudesDegrees: Map<String, Double>,
    val metadata: CalculationMetadata
)

class AstronomyEngineNotConfiguredException :
    IllegalStateException("A licensed, validated astronomy engine has not been configured.")
