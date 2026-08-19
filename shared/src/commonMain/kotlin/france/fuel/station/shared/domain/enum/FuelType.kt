package france.fuel.station.shared.domain.enum

enum class FuelType(val frenchLabel: String) {
    GAZOLE("Gazole (B7)"),
    SP95("SP95 (E5)"),
    E85("E85"),
    GPLC("GPLc"),
    E10("SP95-E10 (E10)"),
    SP98("SP98 (E5)"),
    UNKNOWN("Inconnu")
}
