package fuel.station.shared.data.source

import fuel.station.shared.domain.model.Station
import fuel.station.shared.domain.model.FuelPrice
import fuel.station.shared.domain.model.DecimalPrice
import fuel.station.shared.domain.enum.FuelType
import fuel.station.shared.domain.enum.Availability
import java.math.BigDecimal

// Government source DTO - maps directly to the official government feed structure
// Never leak these DTOs into UI or domain code - they must be normalized

// Government fuel ID mapping: 1=Gazole, 2=SP95, 3=E85, 4=GPLc, 5=E10, 6=SP98
// Government IDs exposed as constants elsewhere in the codebase

data class GovernmentStation(
    val id: String, // Government source station ID
    val addr: String?,
    val cp: String?, // Postal code
    val ville: String?, // City
    val lat: Double,
    val lng: Double,
    val horra: String?, // Opening hours
    val typserv: String?, // Services
    val presence: String?, // Presence type
    // Fuel price fields - these vary by record
    // fuel prices structure: map of fuelId -> price/report time/availability
    val fuelPrices: List<GovernmentFuelPrice>
)

data class GovernmentFuelPrice(
    val id: String, // Government fuel price record ID
    val fuelId: Int, // Government fuel ID (1-6)
    val price: String, // Price as string (can be null/missing)
    val date: Long, // Report timestamp (epoch ms)
    val disponibilite: String?, // Availability status
    val ruptype: String?, // Rupture type
    val rupturedebut: Long?, // Rupture start timestamp
    val source: String // Source identifier
)

// Normalized/staging persistence model - between government DTO and domain model
data class NormalizedStation(
    val sourceId: String,
    val address: String?,
    val postalCode: String?,
    val city: String?,
    val latitude: Double,
    val longitude: Double,
    val geom: String, // PostGIS geography
    val presenceType: String?,
    val openingHours: String?,
    val services: String?,
    val source: String,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val dataSynchronizedAt: Long,
    val active: Boolean,
    val stationPrices: List<NormalizedFuelPrice>
)

data class NormalizedFuelPrice(
    val fuelType: FuelType,
    val pricePerLiter: DecimalPrice,
    val reportedAt: Long,
    val availability: Availability,
    val ruptureType: String?,
    val ruptureStartedAt: Long?,
    val sourceId: String
)

// View model for API response - what clients receive
data class StationViewModel(
    val sourceId: String,
    val address: String?,
    val postalCode: String?,
    val city: String?,
    val coordinates: CoordinatesViewModel,
    val fuelPrices: List<FuelPriceViewModel>,
    val freshness: FreshnessState,
    val dataSynchronizedAt: Long,
    val services: String?,
    val openingHours: String?,
    val distance: Distance?
)

data class CoordinatesViewModel(
    val latitude: Double,
    val longitude: Double
)

data class FuelPriceViewModel(
    val fuelType: FuelType,
    val pricePerLiter: DecimalPrice,
    val reportedAt: Long,
    val availability: Availability,
    val availabilityLabel: String,
    val ruptureType: String?,
    val freshness: FreshnessState
)

data class PageViewModel<T>(
    val items: List<T>,
    val hasNext: Boolean,
    val page: Int,
    val pageSize: Int,
    val totalItems: Int
)