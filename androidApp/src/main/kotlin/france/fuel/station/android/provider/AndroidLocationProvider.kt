package france.fuel.station.android.provider

import france.fuel.station.shared.domain.model.Coordinates
import france.fuel.station.shared.domain.repository.LocationProvider

class AndroidLocationProvider : LocationProvider {
    override suspend fun getCurrentLocation(): Result<Coordinates> {
        return Result.failure(Exception("Location not available in JVM mode"))
    }
}
