package fuel.station.shared.domain.repository

import fuel.station.shared.domain.model.Station
import fuel.station.shared.domain.valueobject.SearchLocation
import kotlinx.coroutines.Result

interface StationRepository {
    suspend fun getNearby(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
        fuelType: FuelType?,
        page: Int,
        pageSize: Int
    ): Result<Page<Station>>

    suspend fun getStationDetails(stationId: String): Result<Station>

    suspend fun refreshStations(): Result<Unit>
}