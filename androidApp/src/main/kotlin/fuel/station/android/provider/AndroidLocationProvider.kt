package fuel.station.android.provider

import fuel.station.shared.domain.model.Coordinates
import fuel.station.shared.domain.repository.LocationProvider

class AndroidLocationProvider : LocationProvider {
    override suspend fun getCurrentLocation(): Result<Coordinates> {
        return Result.failure(Exception("Location not available in JVM mode"))
    }
}
