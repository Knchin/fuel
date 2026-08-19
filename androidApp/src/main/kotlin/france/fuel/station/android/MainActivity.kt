package france.fuel.station.android

import france.fuel.station.shared.domain.enum.FuelType
import france.fuel.station.shared.domain.model.FreshnessState
import france.fuel.station.shared.domain.valueobject.SearchLocation
import france.fuel.station.shared.domain.valueobject.Distance
import france.fuel.station.shared.presentation.model.*

class MainActivity {
    fun onCreate() {
        FranceFuelApp()
    }
}

fun FranceFuelApp() {
    println("France Fuel Station App")
}

fun MainAppScreen() {
    val homeState = HomeUiState(
        location = null,
        selectedFuel = null,
        stations = emptyList(),
        isLoading = false,
        refreshTriggered = false,
        error = null,
        isOffline = false,
        showingCached = false,
        syncTimestamp = null
    )
    println("MainAppScreen: loading=${homeState.isLoading}")
}

fun StationCard(
    station: StationUi,
    onNavigate: () -> Unit,
    selectedFuel: FuelType?
) {
    println("Station: ${station.city} - ${station.address}")
}
