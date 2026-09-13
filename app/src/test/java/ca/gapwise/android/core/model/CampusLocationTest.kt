package ca.gapwise.android.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CampusLocationTest {
    @Test
    fun `location normalizes building and room text`() {
        val location = CampusLocation.of(
            campus = Campus.UTSG,
            buildingCode = " ba ",
            room = " 1170 ",
        )

        assertEquals("BA", location.buildingCode)
        assertEquals("1170", location.room)
        assertEquals("BA 1170", location.displayName)
    }

    @Test
    fun `same building code on different campuses remains distinct`() {
        val utm = CampusLocation.of(Campus.UTM, "MN", "1270")
        val utsg = CampusLocation.of(Campus.UTSG, "MN", "1270")

        assertNotEquals(utm, utsg)
        assertNotEquals(utm.stableKey, utsg.stableKey)
    }

    @Test
    fun `blank room is treated as absent`() {
        val location = CampusLocation.of(
            campus = Campus.UTM,
            buildingCode = "IB",
            room = "   ",
        )

        assertNull(location.room)
        assertEquals("IB", location.displayName)
    }
}
