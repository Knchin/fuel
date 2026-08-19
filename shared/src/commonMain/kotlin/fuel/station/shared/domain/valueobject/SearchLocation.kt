package fuel.station.shared.domain.valueobject

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
