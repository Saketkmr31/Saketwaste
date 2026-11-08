package com.surwi.compass360.domain

import com.surwi.compass360.domain.numerology.NumerologyCalculator
import com.surwi.compass360.domain.vastu.VastuDirectionCatalog
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DomainFoundationTest {
    @Test fun numerologyCalculatesMulankAndBhagyank() {
        val result = NumerologyCalculator.calculate(LocalDate.of(1998, 7, 31))
        assertEquals(4, result.mulank)
        assertEquals(2, result.bhagyank)
    }

    @Test fun chaldeanNameNumberIsCalculatedOnlyWhenNameIsProvided() {
        val withoutName = NumerologyCalculator.calculate(LocalDate.of(1998, 7, 31))
        val withName = NumerologyCalculator.calculate(LocalDate.of(1998, 7, 31), "SURESH")
        assertEquals(null, withoutName.nameNumber)
        assertEquals(3, withName.nameNumber)
    }

    @Test fun vastuBearingWrapsCorrectlyAcrossNorth() {
        assertEquals("N", VastuDirectionCatalog.atBearing(359.0).code)
        assertEquals("N", VastuDirectionCatalog.atBearing(0.0).code)
        assertEquals("NNE", VastuDirectionCatalog.atBearing(20.0).code)
        assertEquals("NNW", VastuDirectionCatalog.atBearing(340.0).code)
    }

    @Test fun allSixteenVastuSectorsArePresent() {
        assertEquals(16, VastuDirectionCatalog.directions.size)
        assertEquals(16, VastuDirectionCatalog.directions.map { it.code }.toSet().size)
    }
}
