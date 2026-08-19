package fuel.station.android

import fuel.station.shared.domain.enum.FuelType
import fuel.station.shared.domain.model.FreshnessState
import fuel.station.shared.domain.valueobject.SearchLocation
import fuel.station.shared.domain.valueobject.Distance
import fuel.station.shared.presentation.model.*

class MainActivity {
    fun onCreate() {
        FuelApp()
    }
}

fun FuelApp() {
    println("Fuel Station App")
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
