package france.fuel.station.shared.domain.enum

enum class Availability {
    AVAILABLE,
    UNAVAILABLE,
    UNAVAILABLE_TEMPORARILY,
    UNAVAILABLE_DEFINITIVELY,
    UNKNOWN,
    REPORTED_PRICE;

    fun frenchLabel(): String = when (this) {
        AVAILABLE -> "Disponible"
        UNAVAILABLE -> "Indisponible"
        UNAVAILABLE_TEMPORARILY -> "Indisponible temporairement"
        UNAVAILABLE_DEFINITIVELY -> "Indisponible définitivement"
        REPORTED_PRICE -> "Information indisponible"
        UNKNOWN -> "Information indisponible"
    }

    fun frenchLabelWithSymbol(): String = when (this) {
        AVAILABLE -> "✓ Disponible"
        UNAVAILABLE -> "× Indisponible"
        UNAVAILABLE_TEMPORARILY -> "⚠ Temporairement indisponible"
        UNAVAILABLE_DEFINITIVELY -> "× Indisponible définitivement"
        REPORTED_PRICE -> "? Information indisponible"
        UNKNOWN -> "? Information indisponible"
    }
}