package france.fuel.station.shared.presentation.model

import france.fuel.station.shared.domain.model.Station
import france.fuel.station.shared.domain.model.FreshnessState
import france.fuel.station.shared.domain.model.Distance
import france.fuel.station.shared.domain.enum.Availability
import france.fuel.station.shared.domain.valueobject.SearchLocation

// UI State models - what the Compose UI observes
// These are separate from domain models and adapted for UI consumption

data class FuelPriceUi(
    val fuelType: france.fuel.station.shared.domain.enum.FuelType,
    val price: String,
    val priceFormatted: String,
    val availability: france.fuel.station.shared.domain.enum.Availability,
    val availabilityFrench: String,
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

data class SearchUiState(
    val query: String,
    val location: SearchLocation?,
    val isSearching: Boolean,
    val searchResults: List<SearchLocation>,
    val showSearchResults: Boolean,
    val error: String?,
    val hasLocationPermission: Boolean
)

data class HomeUiState(
    val location: SearchLocation?,
    val selectedFuel: france.fuel.station.shared.domain.enum.FuelType?,
    val stations: List<StationUi>,
    val isLoading: Boolean,
    val refreshTriggered: Boolean,
    val error: String?,
    val isOffline: Boolean,
    val showingCached: Boolean,
    val syncTimestamp: String?
)

data class StationSearchState(
    val idle: StationSearchIdle,
    val loading: StationSearchLoading,
    val success: StationSearchSuccess,
    val empty: StationSearchEmpty,
    val offline: StationSearchOffline,
    val error: StationSearchError
)

data class StationSearchIdle(
    val preferredFuel: france.fuel.station.shared.domain.enum.FuelType?,
    val searchRadius: Double
)

data class StationSearchLoading()

data class StationSearchSuccess(
    val stations: List<StationUi>,
    val refreshTimestamp: String
)

data class StationSearchEmpty(
    val message: String
)

data class StationSearchOffline(
    val cachedTimestamp: String?
)

data class StationSearchError(
    val message: String,
    val retry: Boolean = true
)