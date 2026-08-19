package fuel.station.shared.data.source.adapter

import fuel.station.shared.domain.model.Station
import fuel.station.shared.domain.model.FuelPrice
import fuel.station.shared.domain.model.DecimalPrice
import fuel.station.shared.domain.enum.FuelType
import fuel.station.shared.domain.enum.Availability
import fuel.station.shared.domain.valueobject.SearchLocation
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date

// Government feed parser - isolates government-specific implementation
// Converts GovernmentStation -> NormalizedStation -> Domain Station

object GovernmentFeedParser {

    private val governmentIdToFuelType = mapOf(
        1 to FuelType.GAZOLE,
        2 to FuelType.SP95,
        3 to FuelType.E85,
        4 to FuelType.GPLC,
        5 to FuelType.E10,
        6 to FuelType.SP98
    )

    private val governmentIdToLabel = mapOf(
        1 to "Gazole (B7)",
        2 to "SP95 (E5)",
        3 to "E85",
        4 to "GPLc",
        5 to "SP95-E10 (E10)",
        6 to "SP98 (E5)"
    )

    fun parseStation(gov: GovernmentStation): NormalizedStation {
        val prices = gov.fuelPrices?.mapNotNull { parseFuelPrice(it) } ?: emptyList()
        
        return NormalizedStation(
            sourceId = gov.id,
            address = gov.addr,
            postalCode = gov.cp,
            city = gov.ville,
            latitude = gov.lat,
            longitude = gov.lng,
            geom = wktFromCoordinates(gov.lat, gov.lng),
            presenceType = gov.presence,
            openingHours = gov.horra,
            services = gov.typserv,
            source = gov.source,
            firstSeenAt = System.currentTimeMillis(),
            lastSeenAt = System.currentTimeMillis(),
            dataSynchronizedAt = System.currentTimeMillis(),
            active = true,
            stationPrices = prices
        )
    }

    private fun parseFuelPrice(price: GovernmentFuelPrice): NormalizedFuelPrice {
        val fuelType = governmentIdToFuelType[price.fuelId]
            .let { it }
            .orElse(FuelType.UNKNOWN) // Should not happen with valid data, but safety first

        val priceValue = when (price.price) {
            null, "" -> DecimalPrice(BigDecimal.ZERO, "0,000")
            else -> {
                val bigDec = BigDecimal(price.price)
                // Format to 3 decimal places for government fuel prices
                val formatted = bigDec.setScale(3, BigDecimal.ROUND_HALF_UP).toString()
                    .replace(".", ",")
                    .replace("-.", "-")
                DecimalPrice(bigDec, formatted)
            }
        }

        // Apply availability rules with exact precedence
        val availability = determineAvailability(price)

        val reportedAt = price.date

        val ruptureStartedAt = price.rupturedebut

        return NormalizedFuelPrice(
            fuelType = fuelType,
            pricePerLiter = priceValue,
            reportedAt = reportedAt,
            availability = availability,
            ruptureType = price.ruptureType,
            ruptureStartedAt = ruptureStartedAt,
            sourceId = price.source
        )
    }

    private fun determineAvailability(price: GovernmentFuelPrice): Availability {
        // Exact precedence rules from specification:
        // 1. definitive rupture -> UNAVAILABLE_DEFINITIVELY
        // 2. temporary rupture -> UNAVAILABLE_TEMPORARILY
        // 3. explicit unavailable -> UNAVAILABLE
        // 4. explicit available + valid price -> AVAILABLE
        // 5. valid price without explicit availability -> REPORTED_PRICE
        // 6. nothing useful -> UNKNOWN

        val ruptureType = price.ruptureType ?: ""
        val disponibilite = price.disponibilite ?: ""
        val priceVal = price.price

        // Rule 1: definitive rupture
        if ("definitive".equals(ruptureType, ignoreCase = true) || 
            "definitif".equals(ruptureType, ignoreCase = true)) {
            return Availability.UNAVAILABLE_DEFINITIVELY
        }

        // Rule 2: temporary rupture
        if ("temporary".equals(ruptureType, ignoreCase = true) || 
            "temporaire".equals(ruptureType, ignoreCase = true)) {
            return Availability.UNAVAILABLE_TEMPORARILY
        }

        // Rule 3: explicit unavailable
        if ("non disponible".equals(disponibilite, ignoreCase = true) || 
            "indisponible".equals(disponibilite, ignoreCase = true) ||
            "rupture".equals(ruptureType, ignoreCase = true)) {
            return Availability.UNAVAILABLE
        }

        // Rule 4: explicit available + valid price
        if ("disponible".equals(disponibilite, ignoreCase = true) && priceVal != null && priceVal != "") {
            return Availability.AVAILABLE
        }

        // Rule 5: valid price without explicit availability -> REPORTED_PRICE
        if (priceVal != null && priceVal != "") {
            return Availability.REPORTED_PRICE
        }

        // Rule 6: nothing useful
        return Availability.UNKNOWN
    }

    private fun wktFromCoordinates(lat: Double, lng: Double): String {
        // PostGIS geography WKT: SRID 4326
        return "SRID=4326;POINT($lng $lat)"
    }

    // Convert government coordinate format if needed
    // Government may provide coordinates in various formats - normalize to Double
    fun normalizeCoordinate(value: String?): Double {
        return when (value) {
            null -> 0.0
            else -> value.toDoubleOrNull() ?: 0.0
        }
    }

    // Parse raw government feed line (CSV/JSON) into GovernmentStation
    // This isolates the government-specific format parsing
    fun parseFromCsvLine(line: String): GovernmentStation? {
        // CSV format from government feed:
        // id,addr,cp,ville,lat,lng,horra,typserv,presence,fuel1Id,fuel1Price,fuel1Date,fuel1Dispo,fuel1RuptType,fuel1RuptDeb,fuel2Id,...
        val parts = line.split(",")
        if (parts.size < 9) return null

        try {
            val stationId = parts[0]
            val addr = if (parts.size > 1 && parts[1].isNotEmpty()) parts[1] else null
            val cp = if (parts.size > 2 && parts[2].isNotEmpty()) parts[2] else null
            val ville = if (parts.size > 3 && parts[3].isNotEmpty()) parts[3] else null
            val lat = parts[4].toDoubleOrNull() ?: 0.0
            val lng = parts[5].toDoubleOrNull() ?: 0.0
            val horra = if (parts.size > 6 && parts[6].isNotEmpty()) parts[6] else null
            val typserv = if (parts.size > 7 && parts[7].isNotEmpty()) parts[7] else null
            val presence = if (parts.size > 8 && parts[8].isNotEmpty()) parts[8] else null

            // Parse fuel prices - government feed has fuelId -> price/date/dispo/rupture structure
            // Starting from index 9, fuels are listed in groups of 6+ fields
            val fuelPrices = parseFuelPricesFromCsv(parts, 9)

            return GovernmentStation(
                id = stationId,
                addr = addr,
                cp = cp,
                ville = ville,
                lat = lat,
                lng = lng,
                horra = horra,
                typserv = typserv,
                presence = presence,
                fuelPrices = fuelPrices
            )
        } catch (e: Exception) {
            // Log and return null - malformed record
            return null
        }
    }

    private fun parseFuelPricesFromCsv(parts: List<String>, startIndex: Int): List<GovernmentFuelPrice> {
        val fuelPrices = mutableListOf<GovernmentFuelPrice>()
        var index = startIndex

        while (index + 5 < parts.size) {
            try {
                val fuelId = parts[index].toIntOrNull()
                val fuelPrice = parts[index + 1]
                val fuelDate = parts[index + 2].toLongOrNull() ?: System.currentTimeMillis()
                val fuelDisponibilite = if (index + 3 < parts.size) parts[index + 3] else ""
                val fuelRuptType = if (index + 4 < parts.size) parts[index + 4] else ""
                val fuelRuptDeb = if (index + 5 < parts.size) parts[index + 5].toLongOrNull() ?: 0L

                if (fuelId != null && fuelId in 1..6) {
                    fuelPrices.add(GovernmentFuelPrice(
                        id = "${fuelId}_${fuelDisponibilite}",
                        fuelId = fuelId,
                        price = fuelPrice,
                        date = fuelDate,
                        disponibilite = fuelDisponibilite,
                        ruptureType = fuelRuptType,
                        rupturedebut = fuelRuptDeb,
                        source = "gouvernement_francais"
                    ))
                }
                index += 6
            } catch (e: Exception) {
                // Skip malformed fuel price entry, continue with next
                index += 1
            }
        }

        return fuelPrices
    }
}