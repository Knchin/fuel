package fuel.station.shared.domain.model

import fuel.station.shared.domain.enum.FuelType
import fuel.station.shared.domain.enum.Availability

data class FuelPrice(
    val fuelType: FuelType,
    val pricePerLiter: DecimalPrice,
    val reportedAt: Long,
    val availability: Availability,
    val ruptureType: String?,
    val ruptureStartedAt: Long?
)

data class DecimalPrice(
    val value: BigDecimal,
    val formatted: String
)

data class Station(
    val sourceId: String,
    val address: String?,
    val postalCode: String?,
    val city: String?,
    val latitude: Double,
    val longitude: Double,
    val geom: String?, // PostGIS WKT representation
    val presenceType: String?,
    val openingHours: String?,
    val services: String?,
    val source: String,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val dataSynchronizedAt: Long,
    val active: Boolean
)

data class StationId(
    val value: String
)

data class Coordinates(
    val latitude: Double,
    val longitude: Double
)