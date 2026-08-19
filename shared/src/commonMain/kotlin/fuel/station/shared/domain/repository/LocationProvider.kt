package fuel.station.shared.domain.repository

interface LocationProvider {
    suspend fun getCurrentLocation(): Result<fuel.station.shared.domain.model.Coordinates>
}
