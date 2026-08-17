package france.fuel.station.shared.domain.use-case

import france.fuel.station.shared.domain.model.Station
import france.fuel.station.shared.domain.enum.FuelType
import kotlinx.coroutines.Result

class GetNearbyStations(
    private val stationRepository: StationRepository
) {
    suspend fun execute(
        latitude: Double,
        longitude: Double,
        radiusKm: Double = 10.0,
        fuelType: FuelType? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): Result<Page<Station>> = stationRepository.getNearby(
        latitude = latitude,
        longitude = longitude,
        radiusKm = radiusKm,
        fuelType = fuelType,
        page = page,
        pageSize = pageSize
    )
}

class GetStationDetails(
    private val stationRepository: StationRepository
) {
    suspend fun execute(stationId: String): Result<Station> = stationRepository.getStationDetails(stationId)
}

class RefreshStations(
    private val stationRepository: StationRepository
) {
    suspend fun execute(): Result<Unit> = stationRepository.refreshStations()
}

class SortStationsByPrice(
    private val stations: List<Station>
) {
    fun execute(): List<Station> = stations.sortedBy {
        when (it.pricePerLiter.value) {
            is france.fuel.station.shared.domain.model.DecimalPrice -> it.value
            null -> Double.POSITIVE_INFINITY
        }
    }
}

class SortStationsByDistance(
    private val stations: List<StationDistance>
) {
    fun execute(): List<StationDistance> = stations.sortedBy { it.distance }
}

class EvaluatePriceFreshness(
    private val reportedAt: Long,
    private val dataSynchronizedAt: Long
) {
    private val freshnessThresholds = mapOf(
        "FRESH" to 2L * 60 * 60 * 1000L,
        "AGING" to 6L * 60 * 60 * 1000L,
        "STALE" to 24L * 60 * 60 * 1000L,
        "VERY_STALE" to Long.MAX_VALUE
    )

    fun evaluate(): FreshnessState {
        val ageHours = (System.currentTimeMillis() - reportedAt) / (60 * 60 * 1000L)

        return when {
            ageHours <= 2 -> FreshnessState.FRESH
            ageHours <= 6 -> FreshnessState.AGING
            ageHours <= 24 -> FreshnessState.STALE
            else -> FreshnessState.VERY_STALE
        }
    }
}

class EvaluateFuelAvailability(
    val availability: france.fuel.station.shared.domain.enum.Availability,
    val price: france.fuel.station.shared.domain.model.DecimalPrice?
): FuelAvailabilityResult {

    fun frenchLabel(): String = availability.frenchLabel()

    fun frenchLabelWithSymbol(): String = availability.frenchLabelWithSymbol()

    fun isDisplayedAsAvailable(): Boolean = when (this) {
        FuelAvailabilityResult.AVAILABLE -> true
        FuelAvailabilityResult.REPORTED_PRICE -> true
        else -> false
    }
}

data class FuelAvailabilityResult(
    val label: String,
    val frenchLabel: String,
    val isAvailable: Boolean,
    val isUnavailableDefinitively: Boolean,
    val isUnavailableTemporarily: Boolean,
    val isReportedPrice: Boolean,
    val isUnknown: Boolean
)