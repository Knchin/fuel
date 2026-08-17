package france.fuel.station.shared.domain.valueobject

import france.fuel.station.shared.domain.enum.FuelType

data class FuelSelection(
    val selectedTypes: Set<FuelType>,
    val includeAll: Boolean
)

data class SearchLocation(
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val postalCode: String?,
    val city: String?
)

data class Distance(
    val value: Double,
    val formatted: String
)

data class StationDistance(
    val station: france.fuel.station.shared.domain.model.Station,
    val distance: Distance
)