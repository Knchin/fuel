package france.fuel.station.web

import france.fuel.station.shared.presentation.model.HomeUiState
import fr.fuel.station.shared.presentation.model.StationUi
import fr.fuel.station.shared.data.network.ApiHttpClient
import fr.fuel.station.shared.domain.enum.FuelType
import kotlinx.coroutines.*
import kotlinx.serialization.*
import java.math.BigDecimal

// Web entry point for the KMP Wasm build
// This bootstraps the application when running in the browser

fun main() {
    // This will be called when the Wasm module is loaded
    // In a real application, this would connect to the Supabase backend
    // and start the UI
    
    // For now, just log that the app loaded
    println("France Fuel Station Web Application loaded")
}