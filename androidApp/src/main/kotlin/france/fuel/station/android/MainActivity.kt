package france.fuel.station.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.ui
import androidx.activity.compose.requestPermission
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.FontWeight
import androidx.compose.ui.text.Semantics
import androidx.compose.ui.text.compose
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.style
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.px
import androidx.compose.ui.viewinterceptor.onGloballyPositioned
import androidx.compose.material3.icons.filled.Navigate
import androidx.compose.material3.icons.filled.Directions
import androidx.compose.material3.icons.filled.Search
import androidx.compose.material3.icons.favorite
import com.google.accompanist.animation.core.animateFloatAsState
import com.google.accompanist.layoutaware.LayoutAwareColumn
import fr.fuel.station.shared.data.network.ApiHttpClient
import fr.fuel.station.shared.domain.enum.FuelType
import fr.fuel.station.shared.domain.model.Station
import fr.fuel.station.shared.domain.model.FreshnessState
import fr.fuel.station.shared.domain.model.Page
import fr.fuel.station.shared.domain.valueobject.SearchLocation
import fr.fuel.station.shared.presentation.model.*
import fr.fuel.station.shared.data.network.StationApiClient
import fr.fuel.station.shared.domain.repository.StationRepository
import fr.fuel.station.shared.domain.repository.LocationSearchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectAsStateWithContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FranceFuelApp()
        }
    }
}

// Permission handling
private val LOCATION_PERMISSION_REQUEST_CODE = 1001

@Composable
fun FranceFuelApp() {
    // Permission state
    var locationPermission by remember { mutableStateOf<Int?>(null) }
    
    // Initialize permission check on first load
    LaunchedEffect(Unit) {
        checkLocationPermission()
    }

    when (locationPermission) {
        is null -> {
            // Permission not checked yet
            LoadingScreen()
        }
        PackageManager.PERMISSION_GRANTED -> {
            // Permission granted - show main app
            MainAppScreen()
        }
        PackageManager.PERMISSION_DENIED -> {
            // Permission denied - show option to request or use manual search
            PermissionDeniedScreen(MainAppScreen::class.java) { 
                // User chose to deny permanently
                locationPermission = null 
            }
        }
        else -> {
            // Permission permanently denied
            PermanentlyDeniedScreen(MainAppScreen::class.java) {
                locationPermission = null
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    val homeState = remember { mutableStateOf<HomeUiState>(
        HomeUiState(
            location = null,
            selectedFuel = null,
            stations = emptyList(),
            isLoading = false,
            refreshTriggered = false,
            error = null,
            isOffline = false,
            showingCached = false,
            syncTimestamp = null
        )
    )}

    val locationProvider = LocalLocationProvider()
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // Toolbar/Top bar
        TopAppBar(
            title = { Text(text = "Station Essence France") },
            navigationIcon = {
                IconButton(onClick = { /* search action */ }) {
                    Icon(Icons.Default.Search, contentDescription = "Rechercher")
                }
            }
        )

        // Home content
        HomeScreen(
            location = homeState.value.location,
            onLocationSelected = { location ->
                homeState.value = homeState.value.copy(location = location)
            },
            selectedFuel = homeState.value.selectedFuel,
            onFuelSelected = { fuel ->
                homeState.value = homeState.value.copy(selectedFuel = fuel)
            },
            stations = homeState.value.stations,
            isLoading = homeState.value.isLoading,
            onRefresh = { refreshStations() },
            error = homeState.value.error,
            isOffline = homeState.value.isOffline,
            showingCached = homeState.value.showingCached,
            syncTimestamp = homeState.value.syncTimestamp
        )
    }
}

@Composable
fun HomeScreen(
    location: SearchLocation?,
    onLocationSelected: (SearchLocation) -> Unit,
    selectedFuel: FuelType?,
    onFuelSelected: (FuelType) -> Unit,
    stations: List<StationUi>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    error: String?,
    isOffline: Boolean,
    showingCached: Boolean,
    syncTimestamp: String?
) {
    if (isOffline && !showingCached) {
        OfflineBanner()
    }

    if (isLoading) {
        LoadingState()
    } else if (stations.isEmpty()) {
        EmptyState(
            error ?: if (location == null) "Choisissez une localisation" else "Aucune station trouvée"
        )
    } else {
        StationList(
            stations = stations,
            selectedFuel = selectedFuel,
            onNavigation = { station ->
                openNavigation(station)
            },
            onStationSelected = { station ->
                // Navigate to station details
            }
        )
    }
}

@Composable
fun LoadingState() {
    Center {
        CircularProgressIndicator(
            modifier = Modifier.size(50.dp)
        )
    }
}

@Composable
fun EmptyState(message: String) {
    Center {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = "Erreur",
                tint = Color.Gray,
                modifier = Modifier.size(80.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(vertical = 16.dp),
                textAlign = TextAlign.Center
            )
            Button(
                onClick = {},
                modifier = Modifier.padding(8.dp)
            ) {
                Text(text = "Réessayer")
            }
        }
    }
}

@Composable
fun OfflineBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardElevation.Default,
        colors = CardColors(
            defaultColor = Color.Orange
        )
    ) {
        Modifier.padding(16.dp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Offline,
                contentDescription = "Hors connexion",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vous êtes hors connexion",
                    style = MaterialTheme.typography.h6,
                    color = Color.White
                )
                Text(
                    text = "Affichage des données enregistrées",
                    style = MaterialTheme.typography.body2,
                    color = Color.White.copy(alpha = 0.9)
                )
            }
        }
    }
}

@Composable
fun PermissionDeniedScreen(
    onGranted: () -> Unit,
    onPermanentlyDenied: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.Center,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Permission de localisation refusée",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Impossible de rechercher des stations sans permission de localisation. Vous pouvez utiliser la recherche d'adresse manuelle.",
            style = MaterialTheme.typography.body1,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 32.dp),
            textAlign = TextAlign.Center
        )
        Button(onClick = onGranted) {
            Text(text = "Réessayer")
        }
        Button(onClick = onPermanentlyDenied, style = MaterialTheme.buttonStyle2) {
            Text(text = "Utiliser la recherche manuelle")
        }
    }
}

@Composable
fun PermanentlyDeniedScreen(
    onOpenSettings: () -> Unit,
    onUseManual: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.Center,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Permission de localisation définitivement refusée",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Vous devez activer la localisation dans les paramètres de l'application pour utiliser la recherche par position.",
            style = MaterialTheme.typography.body1,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 32.dp),
            textAlign = TextAlign.Center
        )
        Button(onClick = onOpenSettings) {
            Text(text = "Ouvrir les paramètres")
        }
        Button(onClick = onUseManual) {
            Text(text = "Utiliser la recherche d'adresse")
        }
    }
}

// Station card composable
@Composable
fun StationCard(station: StationUi, onNavigate: () -> Unit, selectedFuel: FuelType?) {
    var showDetails by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = 2.dp,
        colors = CardColors(
            defaultColor = Color.White
        )
    ) {
        Modifier.padding(16.dp)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            // Fuel type and price row
            FuelPriceRow(station.fuelPrices, selectedFuel)
            
            // Freshness
            FreshnessIndicator(station.freshness)
            
            // Distance
            if (station.distance != null) {
                Text(
                    text = station.distance.formatted,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Address
            Text(
                text = station.address ?: "Adresse inconnue",
                style = MaterialTheme.typography.body1,
                maxLines = 2,
                overflow = Overflow.Clip,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action button
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterEnd
            ) {
                Button(
                    onClick = { onNavigate(); showDetails = true }
                ) {
                    Text(text = "Itinéraire")
                }
            }
        }
    }
}

@Composable
fun FuelPriceRow(
    fuelPrices: List<FuelPriceUi>,
    selectedFuel: FuelType?
) {
    // Show only the selected fuel or all available fuels
    val fuelsToShow = if (selectedFuel != null) {
        fuelPrices.filter { it.fuelType == selectedFuel }
    } else {
        fuelPrices
    }
    
    if (fuelsToShow.isEmpty()) {
        return
    }
    
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Center
    ) {
        // Fuel type label
        Text(
            text = fuelsToShow[0].fuelType.frenchLabel,
            style = MaterialTheme.typography.body1,
            maxLines = 1,
            overflow = Overflow.Clip
        )
        
        // Price
        Text(
            text = fuelsToShow[0].priceFormatted + " €/L",
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold,
            color = Color.Orange
        )
        
        // Freshness
        FreshnessSmallIndicator(fuelsToShow[0])
    }
}

@Composable
fun FreshnessIndicator(freshness: FreshnessState?) {
    if (freshness == null) {
        return
    }
    
    val (color, label) = when (freshness) {
        is FreshnessState.Fresh -> (Color.Green, "Prix signalé il y a moins de 2 h")
        is FreshnessState.Aging -> (Color.Orange, "Prix signalé il y a 2-6 h")
        is FreshnessState.Stale -> (Color.Blue, "Prix signalé il y a 6-24 h")
        is FreshnessState.VeryStale -> (Color.Red, "Prix signalé il y a plus de 24 h")
    }
    
    HapticFeedback.light()
    Row(
        verticalAlignment = Alignment.Center
    ) {
        Icon(
            if (freshness is FreshnessState.Fresh) Icons.Default.CheckCircle
            else if (freshness is FreshnessState.Aging) Icons.Default.Warning
            else if (freshness is FreshnessState.Stale) Icons.Default.ErrorCircle
            else Icons.Default.CloseCircle,
            tint = color,
            contentDescription = label,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.body2,
            color = color,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun FreshnessSmallIndicator(priceUi: FuelPriceUi) {
    FreshnessIndicator(priceUi.availability.frenchLabel() == "Disponible" && priceUi.isFresh 
        ? FreshnessState.Fresh 
        : FreshnessState.Stale)
}

// Navigation action
@Composable
fun openNavigation(station: StationUi) {
    // Platform abstraction for navigation
    // This will be replaced with platform-specific implementation
    val context = LocalContext.current
    val label = "${station.city} ${station.address ?: ""}".trim()
    
    // Use platform navigation
    // Navigate to Google Maps / Apple Maps / Waze
    // For now, just show a snackbar
    SnackbarHostState(
        snackBar = Snackbar(
            message = {
                Text("Ouverture de $label dans Google Maps")
            },
            action = SnackbarAction("Annuler") {}
        )
    )
}