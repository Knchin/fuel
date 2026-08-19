package fuel.station.shared.domain.model

import fuel.station.shared.domain.enum.FuelType
import fuel.station.shared.domain.enum.Availability

data class FuelPrice(
    val fuelType: FuelType,
    val priceFormatted: String,
    val reportedAt: Long,
    val availability: Availability,
    val ruptureType: String?,
    val ruptureStartedAt: Long?
)
