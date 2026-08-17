package france.fuel.station.android.provider

import france.fuel.station.shared.domain.model.Coordinates
import france.fuel.station.shared.domain.repository.LocationProvider
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.icons.favorite
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.accompanist.location.location
import com.google.accompanist.location.locationRadio
import com.google.accompanist.location.locationResult
import com.google.accompanist.location.locationServices
import com.google.accompanist.location.primitives LocationResult
import com.google.accompanist.location.theme.LightTheme
import com.google.accompanist.location.theme.LocaleT
import kotlin.coroutines.resumeWithException
import permissions.dispatcher.*
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import kotlin.coroutines.*
import kotlinx.coroutines.*

// Android Location Provider implementation using Accompanist Location
// Handles: permission requests, location result, denied/permanently denied

androidx.compose.ui.platform.composeLocalScreenContext()

class AndroidLocationProvider(
    private val context: Context
) : LocationProvider {

    private val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    )

    suspend fun getCurrentLocation(): Result<Coordinates> {
        // Check and request permissions
        val locationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (locationPermission != PackageManager.PERMISSION_GRANTED &&
            coarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
            // Request permissions
            locationPermissionLauncher(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return Result.failure(
                Exception("Location permission denied")
            )
        }

        // Use Accompanist Location to get current location
        return suspendCoroutine { continuation ->
            try {
                val locationResult = location(
                    locationRadio = locationRadio.Default,
                    theme = LightTheme
                ).collect { locationResult ->
                    when (locationResult) {
                        is locationResult.LocationResult.Success -> {
                            val coords = Coordinates(
                                latitude = locationResult.latitude,
                                longitude = locationResult.longitude
                            )
                            continuation.resume(Result.success(coords))
                        }
                        is locationResult.LocationResult.PermissionDenied -> {
                            continuation.resume(
                                Result.failure(
                                    Exception("Location permission denied by user")
                                )
                            )
                        }
                        is locationResult.LocationResult.PermissionPermanentlyDenied -> {
                            continuation.resume(
                                Result.failure(
                                    Exception("Location permission permanently denied")
                                )
                            )
                        }
                        is locationResult.LocationResult.LocationUnavailable -> {
                            continuation.resume(
                                Result.failure(
                                    Exception("Location unavailable")
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }
    }
}