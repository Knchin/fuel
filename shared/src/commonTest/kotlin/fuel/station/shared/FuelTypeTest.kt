package fuel.station.shared

import fuel.station.shared.domain.enum.FuelType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FuelTypeTest {
    @Test
    fun testFuelTypeLabels() {
        assertEquals("Gazole (B7)", FuelType.GAZOLE.label)
        assertEquals("SP95 (E5)", FuelType.SP95.label)
        assertEquals("E85", FuelType.E85.label)
        assertEquals("GPLc", FuelType.GPLC.label)
        assertEquals("SP95-E10 (E10)", FuelType.E10.label)
        assertEquals("SP98 (E5)", FuelType.SP98.label)
        assertEquals("Inconnu", FuelType.UNKNOWN.label)
    }

    @Test
    fun testAllFuelTypesHaveLabels() {
        FuelType.values().forEach { type ->
            assertTrue(type.label.isNotEmpty())
        }
    }
}
