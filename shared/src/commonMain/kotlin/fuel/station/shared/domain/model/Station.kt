package fuel.station.shared.domain.model

data class Coordinates(
    val latitude: Double,
    val longitude: Double
)

data class Station(
    val sourceId: String,
    val address: String?,
    val postalCode: String?,
    val city: String?,
    val latitude: Double,
    val longitude: Double,
    val geom: String?,
    val presenceType: String?,
    val openingHours: String?,
    val services: String?,
    val source: String,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val dataSynchronizedAt: Long,
    val active: Boolean,
    val fuelPrices: List<fuel.station.shared.domain.model.FuelPrice> = emptyList()
)

data class StationId(val value: String)

sealed interface FreshnessState {
    class Fresh : FreshnessState
    class Aging : FreshnessState
    class Stale : FreshnessState
    class VeryStale : FreshnessState
}

data class Page<T>(
    val items: List<T>,
    val hasNext: Boolean,
    val page: Int,
    val pageSize: Int,
    val totalItems: Int
)
