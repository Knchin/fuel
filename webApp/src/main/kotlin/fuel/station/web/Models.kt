package fuel.station.web

data class FuelPrice(
    val fuelType: String,
    val pricePerLiter: Double,
    val reportedAt: String,
    val availability: String,
    val ruptureType: String?
)

data class Station(
    val id: String,
    val sourceId: String,
    val address: String?,
    val postalCode: String?,
    val city: String?,
    val latitude: Double,
    val longitude: Double,
    val presenceType: String?,
    val openingHours: String?,
    val services: String?,
    val source: String,
    val active: Boolean,
    val fuelPrices: Array<FuelPrice>?
) {
    fun cheapestPrice(): FuelPrice? {
        return fuelPrices
            ?.filter { it.availability != "indisponible" && it.pricePerLiter > 0 }
            ?.minByOrNull { it.pricePerLiter }
    }

    fun priceForType(fuelType: String): FuelPrice? {
        return fuelPrices?.firstOrNull { it.fuelType == fuelType && it.availability != "indisponible" && it.pricePerLiter > 0 }
    }

    fun displayName(): String {
        return address ?: city ?: sourceId
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as Station
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

data class MapMarker(
    val station: Station,
    val price: FuelPrice?,
    val distanceKm: Double?
)
