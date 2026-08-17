package france.fuel.station.shared.domain.repository

import france.fuel.station.shared.domain.valueobject.SearchLocation
import kotlinx.coroutines.Result

interface LocationSearchRepository {
    suspend fun search(query: String): Result<List<SearchLocation>>

    suspend fun reverseGeocode(latitude: Double, longitude: Double): Result<SearchLocation>
}