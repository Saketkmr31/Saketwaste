package com.surwi.compass360.domain.profile

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Birth inputs required for a reproducible Vedic chart.
 * Coordinates and IANA timezone must come from a user-selected place, not IP guessing.
 */
data class BirthProfile(
    val displayName: String,
    val dateOfBirth: LocalDate,
    val localTimeOfBirth: LocalTime?,
    val placeName: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
    val timeIsApproximate: Boolean = false
) {
    fun validate(): List<String> = buildList {
        if (displayName.isBlank()) add("Enter a profile name.")
        if (dateOfBirth.isAfter(LocalDate.now())) add("Date of birth cannot be in the future.")
        if (placeName.isBlank()) add("Select a birth place.")
        if (latitude !in -90.0..90.0) add("Latitude must be between -90 and 90.")
        if (longitude !in -180.0..180.0) add("Longitude must be between -180 and 180.")
        try { ZoneId.of(timeZoneId) } catch (_: Exception) { add("Select a valid time zone.") }
    }

    val canCalculateAscendant: Boolean
        get() = validate().isEmpty() && localTimeOfBirth != null && !timeIsApproximate
}
