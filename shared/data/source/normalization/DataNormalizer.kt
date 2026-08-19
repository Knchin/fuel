package fuel.station.shared.data.source.normalization

import fuel.station.shared.domain.model.Station
import fuel.station.shared.domain.model.FuelPrice
import fuel.station.shared.domain.model.DecimalPrice
import fuel.station.shared.domain.enum.FuelType
import fuel.station.shared.domain.enum.Availability
import fuel.station.shared.data.source.adapter.GovernmentFeedParser
import fuel.station.shared.data.source.validation.SourceValidator
import java.math.BigDecimal

// Normalization layer - converts government DTOs to normalized persistence model
// and then to domain models. Isolates all government-specific logic.

object DataNormalizer {

    // Government DTO -> Normalized Persistence Model
    fun normalizeToPersistence(govStation: GovernmentFeedParser.GovernmentStation): NormalizedStation {
        val prices = govStation.fuelPrices
            .filter { SourceValidator.isFuelPriceValid(it) }
            .map { normalizeFuelPrice(it) }

        return GovernmentFeedParser.parseStation(govStation).copy(
            stationPrices = prices,
            dataSynchronizedAt = System.currentTimeMillis()
        )
    }

    // Government FuelPrice -> Normalized FuelPrice
    private fun normalizeFuelPrice(price: GovernmentFeedParser.GovernmentFuelPrice): NormalizedFuelPrice {
        val fuelType = GovernmentFeedParser.governmentIdToFuelType[price.fuelId]
            .let { it }
            .orElse(FuelType.UNKNOWN)

        val decimalPrice = SourceValidator.tryParsePrice(price.price)
            .orElseGet { DecimalPrice(BigDecimal.ZERO, "0,000") }

        // Apply availability determination with exact precedence
        val availability = GovernmentFeedParser.determineAvailability(price)

        return NormalizedFuelPrice(
            fuelType = fuelType,
            pricePerLiter = decimalPrice,
            reportedAt = price.date,
            availability = availability,
            ruptureType = price.ruptureType,
            ruptureStartedAt = price.rupturedebut,
            sourceId = price.source
        )
    }

    // Normalized Persistence Model -> Domain Model
    fun normalizeToDomain(normalized: NormalizedStation): Station {
        val fuelPrices = normalized.stationPrices.map { domainFuelPrice(it) }

        return Station(
            sourceId = normalized.sourceId,
            address = normalized.address,
            postalCode = normalized.postalCode,
            city = normalized.city,
            latitude = normalized.latitude,
            longitude = normalized.longitude,
            geom = normalized.geom,
            presenceType = normalized.presenceType,
            openingHours = normalized.openingHours,
            services = normalized.services,
            source = normalized.source,
            firstSeenAt = normalized.firstSeenAt,
            lastSeenAt = normalized.lastSeenAt,
            dataSynchronizedAt = normalized.dataSynchronizedAt,
            active = normalized.active,
            fuelPrices = fuelPrices
        )
    }

    // Normalized FuelPrice -> Domain FuelPrice
    private fun domainFuelPrice(normalized: NormalizedFuelPrice): FuelPrice {
        return FuelPrice(
            fuelType = normalized.fuelType,
            pricePerLiter = normalized.pricePerLiter,
            reportedAt = normalized.reportedAt,
            availability = normalized.availability,
            ruptureType = normalized.ruptureType,
            ruptureStartedAt = normalized.ruptureStartedAt
        )
    }

    // Government DTO -> Domain Model (convenience)
    fun normalizeToDomain(govStation: GovernmentFeedParser.GovernmentStation): Station {
        val normalized = normalizeToPersistence(govStation)
        return normalizeToDomain(normalized)
    }

    // Create an empty/station-initialized domain model
    fun createEmptyStation(
        sourceId: String,
        latitude: Double,
        longitude: Double
    ): Station {
        return Station(
            sourceId = sourceId,
            address = null,
            postalCode = null,
            city = null,
            latitude = latitude,
            longitude = longitude,
            geom = GovernmentFeedParser.wktFromCoordinates(latitude, longitude),
            presenceType = null,
            openingHours = null,
            services = null,
            source = "gouvernement_francais",
            firstSeenAt = System.currentTimeMillis(),
            lastSeenAt = System.currentTimeMillis(),
            dataSynchronizedAt = System.currentTimeMillis(),
            active = true,
            fuelPrices = emptyList()
        )
    }
}