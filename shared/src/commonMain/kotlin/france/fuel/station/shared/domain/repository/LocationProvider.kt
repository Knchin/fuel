package france.fuel.station.shared.domain.repository

interface LocationProvider {
    suspend fun getCurrentLocation(): Result<france.fuel.station.shared.domain.model.Coordinates>
}
