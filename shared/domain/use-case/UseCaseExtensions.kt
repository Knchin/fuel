package fuel.station.shared.domain.use-case

import fuel.station.shared.domain.model.Station
import fuel.station.shared.domain.model.FreshnessState
import fuel.station.shared.domain.model.Page
import fuel.station.shared.domain.model.StationSearchState
import fuel.station.shared.domain.enum.FuelType
import fuel.station.shared.domain.valueobject.SearchLocation
import fuel.station.shared.presentation.model.UiState
import fuel.station.shared.presentation.model.StationUi
import kotlinx.coroutines.Result

// Convert domain Station to UI StationUi
fun domainStationToUi(station: Station, distance: fuel.station.shared.domain.model.Distance? = null, freshness: FreshnessState? = null): StationUi {
    val fuelPrices = station.fuelPrices.map { price ->
        FuelPriceUi(
            fuelType = price.fuelType,
            price = price.pricePerLiter.formatted,
            priceFormatted = formatPrice(price.pricePerLiter.value),
            availability = price.availability,
            availabilityLabel = price.availability.label(),
            isFresh = false, // computed later based on reportedAt vs now
            isAging = false,
            isStale = false,
            isVeryStale = false
        )
    }

    // Compute freshness for each price
    val computedFreshness = station.fuelPrices.joinToString { price ->
        evaluateFreshness(price.reportedAt)
    }

    return StationUi(
        sourceId = station.sourceId,
        address = station.address,
        postalCode = station.postalCode,
        city = station.city,
        coordinates = CoordinatesUi(
            latitude = station.latitude,
            longitude = station.longitude
        ),
        fuelPrices = fuelPrices,
        distance = distance,
        freshness = freshness,
        services = station.services,
        openingHours = station.openingHours,
        navigationLabel = "${station.city}, ${station.address}"
    )
}

fun evaluateFreshness(reportedAt: Long): FreshnessState {
    val now = System.currentTimeMillis()
    val ageMillis = now - reportedAt
    val ageHours = ageMillis / (60 * 60 * 1000L)

    return when {
        ageHours <= 2 -> FreshnessState.Fresh
        ageHours <= 6 -> FreshnessState.Aging
        ageHours <= 24 -> FreshnessState.Stale
        else -> FreshnessState.VeryStale
    }
}

fun formatPrice(price: BigDecimal): String {
    // Format in government style: 1,689
    val formatted = price.setScale(3, BigDecimal.ROUND_HALF_UP)
        .toString()
        .replace(".", ",")
    return formatted
}

// Convert UI State to SearchState
fun uiStateToSearchState(uiState: UiState.HomeUiState): StationSearchState = when {
    uiState.isOffline -> StationSearchState.offline(uiState.isOffline.toString())
    uiState.error != null -> StationSearchState.error(uiState.error)
    uiState.stations.isEmpty() -> StationSearchState.empty("Aucune station trouvée")
    else -> StationSearchState.success(
        uiState.stations,
        uiState.syncTimestamp ?: "—"
    )
}

// Use case: calculate distance between two points
class CalculateDistance {
    fun execute(
        userLat: Double,
        userLng: Double,
        stationLat: Double,
        stationLng: Double
    ): Distance {
        // Haversine formula
        val earthRadiusKm = 6371.0

        val lat1 = toRadians(userLat)
        val lat2 = toRadians(stationLat)
        val deltaLat = toRadians(stationLat - userLat)
        val deltaLng = toRadians(stationLng - userLng)

        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1) * cos(lat2) * sin(deltaLng / 2) * sin(deltaLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distanceKm = earthRadiusKm * c

        val formatted = "À ${"%.1f".format(distanceKm)} km"

        return Distance(value = distanceKm, formatted = formatted)
    }

    private fun toRadians(deg: Double): Double = deg * Math.PI / 180.0
}

// Use case: sort stations by price ascending
fun sortStationsByPrice(stations: List<StationUi>): List<StationUi> =
    stations.sortedBy { it.fuelPrices.isNotEmpty() && it.fuelPrices[0].priceFormatted != "0,000" }

// Use case: sort stations by distance ascending
fun sortStationsByDistance(stations: List<StationUi>): List<StationUi> =
    stations.sortedBy { it.distance?.value ?: Double.MAX_VALUE }

// Use case: filter stations by fuel type
fun filterStationsByFuel(
    stations: List<StationUi>,
    fuelType: FuelType?
): List<StationUi> {
    if (fuelType == null) return stations
    return stations.filter { station ->
        station.fuelPrices.any { it.fuelType == fuelType }
    }
}

// Use case: get primary fuel price (lowest available)
fun getPrimaryFuelPrice(station: StationUi): FuelPriceUi? {
    val priced = station.fuelPrices.filter { it.availability.label() == "Disponible" }
    return if (priced.isNotEmpty()) priced[0] else null
}