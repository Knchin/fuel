package fuel.station.shared.data.source.validation

import fuel.station.shared.domain.model.Station
import fuel.station.shared.domain.model.DecimalPrice
import fuel.station.shared.domain.enum.FuelType
import fuel.station.shared.domain.enum.Availability
import java.math.BigDecimal

// Validation rules for government source data
// Ensures data integrity before persistence and client delivery

object SourceValidator {

    // Validate that coordinates are within valid ranges
    fun validateCoordinates(lat: Double, lng: Double): Boolean {
        return lat in -90.0..90.0 && lng in -180.0..180.0
    }

    // Validate that a price string can be parsed as a valid decimal
    fun validatePrice(price: String?): Boolean {
        return price != null && price.isNotEmpty() && isValidDecimal(price)
    }

    private fun isValidDecimal(s: String): Boolean {
        // Allow format like "1,689" or "1.689" (government/currency format)
        val cleaned = s.replace(" ", "").replace("€", "").replace("FRF", "").replace("F", "")
        return cleaned.remove(',').remove('.').all { it.isDigit() } && cleaned.count { _ == ',' || _ == '.' } <= 1
    }

    // Validate that a fuel type ID is within expected range
    fun validateFuelId(id: Int): Boolean {
        return id in 1..6
    }

    // Validate that a timestamp is reasonable (not in the future, not too old)
    fun validateTimestamp(epochMs: Long): Boolean {
        val now = System.currentTimeMillis()
        val oneWeekAgo = now - 7L * 24 * 60 * 60 * 1000L
        return epochMs >= oneWeekAgo && epochMs <= now + 24 * 60 * 60 * 1000L
    }

    // Validate that a station has at least basic required fields
    fun isStationValid(station: GovernmentStation): Boolean {
        val hasValidCoords = validateCoordinates(station.lat, station.lng)
        val hasCityOrAddress = station.ville?.isNotEmpty() == true || station.addr?.isNotEmpty() == true
        return hasValidCoords && hasCityOrAddress
    }

    // Validate that fuel price has required fields
    fun isFuelPriceValid(price: GovernmentFuelPrice): Boolean {
        val validId = validateFuelId(price.fuelId)
        val validCoords = validateCoordinates(price.lat, price.lng) // if coords exist
        val validTimestamp = validateTimestamp(price.date)
        val validPrice = validatePrice(price.price)
        return validId && validTimestamp && validPrice
    }

    // Convert raw price string to DecimalPrice, returning null if invalid
    fun tryParsePrice(price: String?): DecimalPrice? {
        if (price == null || price.isEmpty()) return null
        
        try {
            val cleaned = price.replace(" ", "")
            val bigDec = BigDecimal(cleaned.replace(",", "."))
            val formatted = bigDec.setScale(3, BigDecimal.ROUND_HALF_UP)
                .toString()
                .replace(".", ",")
            
            return DecimalPrice(bigDec, formatted)
        } catch (e: Exception) {
            return null
        }
    }

    // Check if price is within reasonable government fuel price range (EUR per liter)
    fun isPriceInReasonableRange(price: BigDecimal): Boolean {
        return price in BigDecimal("0.0") .. BigDecimal("20.0")
    }
}