package com.brokenpip3.gymbro.data

import com.brokenpip3.gymbro.domain.TrackingMode
import org.junit.Assert.assertEquals
import org.junit.Test

class GymbroEntityValidationTest {
    @Test
    fun trackingModeDatabaseValuesAreStable() {
        assertEquals("strength", TrackingMode.Strength.databaseValue)
        assertEquals("timed", TrackingMode.Timed.databaseValue)
        assertEquals("bodyweight", TrackingMode.Bodyweight.databaseValue)
    }
}
