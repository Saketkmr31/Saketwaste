package com.surwi.compass360.domain.panchang

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DrikPanchangServiceTest {
    @Test fun bhopalProducesSunriseTithiAndSolarTimes() {
        val result = DrikPanchangService.forDate(
            LocalDate.of(2026, 11, 8), "Bhopal", ZoneId.of("Asia/Kolkata")
        )
        assertTrue(result.tithiDisplay.isNotBlank())
        assertTrue(result.sunriseLocal.isNotBlank())
        assertTrue(result.sunsetLocal.isNotBlank())
        assertTrue(result.canonicalCity.equals("Bhopal", ignoreCase = true))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownCityIsRejected() {
        DrikPanchangService.forDate(
            LocalDate.of(2026, 11, 8), "UnknownPlaceXYZ", ZoneId.of("Asia/Kolkata")
        )
    }
}