package fuel.station.shared.presentation.model

import fuel.station.shared.domain.enum.FuelType
import fuel.station.shared.domain.enum.Availability
import fuel.station.shared.domain.model.FreshnessState
import fuel.station.shared.domain.valueobject.SearchLocation
import fuel.station.shared.domain.valueobject.Distance

data class FuelPriceUi(
    val fuelType: FuelType,
    val price: String,
    val priceFormatted: String,
    val availability: Availability,
    val availabilityLabel: String,
    val isFresh: Boolean,
    val isAging: Boolean,
    val isStale: Boolean,
    val isVeryStale: Boolean
)

data class StationUi(
    val sourceId: String,
    val address: String?,
    val postalCode: String?,
    val city: String?,
    val coordinates: CoordinatesUi,
    val fuelPrices: List<FuelPriceUi>,
    val distance: Distance?,
    val freshness: FreshnessState?,
    val services: String?,
    val openingHours: String?,
    val navigationLabel: String?
)

data class CoordinatesUi(
    val latitude: Double,
    val longitude: Double
)

data class HomeUiState(
    val location: SearchLocation?,
    val selectedFuel: FuelType?,
    val stations: List<StationUi>,
    val isLoading: Boolean,
    val refreshTriggered: Boolean,
    val error: String?,
    val isOffline: Boolean,
    val showingCached: Boolean,
    val syncTimestamp: String?
)
