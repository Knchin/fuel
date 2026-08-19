package fuel.station.ios

import fuel.station.shared.domain.repository.LocationProvider
import fuel.station.shared.domain.model.Coordinates

class IOSLocationProvider : LocationProvider {
    override suspend fun getCurrentLocation(): Result<Coordinates> {
        TODO("iOS location not available on JVM")
    }
}
