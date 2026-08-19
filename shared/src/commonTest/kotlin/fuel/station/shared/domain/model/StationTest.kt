package fuel.station.shared.domain.model

import fuel.station.shared.domain.enum.FuelType
import fuel.station.shared.domain.enum.Availability
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StationTest {
    @Test
    fun testFuelPriceCreation() {
        val price = FuelPrice(
            fuelType = FuelType.GAZOLE,
            priceFormatted = "1,759",
            reportedAt = 1705312200000L,
            availability = Availability.AVAILABLE,
            ruptureType = null,
            ruptureStartedAt = null
        )
        assertEquals("1,759", price.priceFormatted)
        assertEquals(FuelType.GAZOLE, price.fuelType)
        assertEquals(Availability.AVAILABLE, price.availability)
        assertNull(price.ruptureType)
        assertNull(price.ruptureStartedAt)
    }

    @Test
    fun testStationWithPrices() {
        val prices = listOf(
            FuelPrice(FuelType.GAZOLE, "1,759", 1705312200000L, Availability.AVAILABLE, null, null),
            FuelPrice(FuelType.E10, "1,829", 1705312200000L, Availability.AVAILABLE, null, null),
            FuelPrice(FuelType.SP98, "1,959", 1705312200000L, Availability.AVAILABLE, null, null)
        )
        val station = Station(
            sourceId = "FR-00001",
            address = "15 Rue de Rivoli",
            postalCode = "75001",
            city = "Paris",
            latitude = 48.8566,
            longitude = 2.3522,
            geom = null,
            presenceType = "station",
            openingHours = "Lun-Sam: 06:00-22:00, Dim: 08:00-20:00",
            services = "Station services, Toilettes publiques",
            source = "gouvernement_francais",
            firstSeenAt = 1704067200000L,
            lastSeenAt = 1705312200000L,
            dataSynchronizedAt = 1705312200000L,
            active = true,
            fuelPrices = prices
        )
        assertEquals(3, station.fuelPrices.size)
        assertEquals("Paris", station.city)
        assertEquals("FR-00001", station.sourceId)
        assertEquals("gouvernement_francais", station.source)
        assertTrue(station.active)
    }

    @Test
    fun testStationMinimalFields() {
        val station = Station(
            sourceId = "FR-00099",
            address = null,
            postalCode = null,
            city = null,
            latitude = 48.8566,
            longitude = 2.3522,
            geom = null,
            presenceType = null,
            openingHours = null,
            services = null,
            source = "gouvernement_francais",
            firstSeenAt = 1704067200000L,
            lastSeenAt = 1705312200000L,
            dataSynchronizedAt = 1705312200000L,
            active = true,
            fuelPrices = emptyList()
        )
        assertNull(station.address)
        assertNull(station.postalCode)
        assertNull(station.city)
        assertTrue(station.fuelPrices.isEmpty())
    }

    @Test
    fun testFuelPriceWithRupture() {
        val price = FuelPrice(
            fuelType = FuelType.SP95,
            priceFormatted = "1,899",
            reportedAt = 1705312200000L,
            availability = Availability.UNAVAILABLE_TEMPORARILY,
            ruptureType = "maintenance",
            ruptureStartedAt = 1705225800000L
        )
        assertEquals(Availability.UNAVAILABLE_TEMPORARILY, price.availability)
        assertEquals("maintenance", price.ruptureType)
        assertNotNull(price.ruptureStartedAt)
    }

    @Test
    fun testAllFuelTypesRepresented() {
        val fuelTypes = listOf(
            FuelType.GAZOLE,
            FuelType.SP95,
            FuelType.E10,
            FuelType.SP98,
            FuelType.E85,
            FuelType.GPLC
        )
        fuelTypes.forEach { type ->
            val price = FuelPrice(
                fuelType = type,
                priceFormatted = "1,000",
                reportedAt = 1705312200000L,
                availability = Availability.AVAILABLE,
                ruptureType = null,
                ruptureStartedAt = null
            )
            assertEquals(type, price.fuelType)
        }
    }
}
