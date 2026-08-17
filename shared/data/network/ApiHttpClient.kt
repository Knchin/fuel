package france.fuel.station.shared.data.network

import france.fuel.station.shared.domain.model.Station
import france.fuel.station.shared.domain.valueobject.SearchLocation
import io.ktor.client.engine.curl.curlEngine
import io.ktor.client.request.*
import io.ktor.client.responses.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.JSON
import kotlinx.serialization.descriptors.ClassDescriptor
import kotlinx.serialization.encoder.Encoder
import kotlinx.serialization.decoder.Decoder
import java.util.concurrent.TimeUnit

// KMP-compatible HTTP client for API communication
// Wraps Ktor with proper error handling, timeouts, and cancellation

object ApiHttpClient {
    private val client = io.ktor.client.network.setEngine(curlEngine()).build()

    suspend fun <T> get(
        url: String,
        serializers: List<KSerializer<T>>,
        timeoutMs: Int = 15000
    ): Result<T> {
        return try {
            val request = client.get(url)
            request.timeout(timeoutMs, TimeUnit.MILLISECONDS)

            val response = request.perform()

            if (response.isErrorStatus()) {
                return Result.failure(
                    Exception("HTTP ${response.status}: ${response.statusMessage}")
                )
            }

            val body = response.body?.string() ?: return Result.failure(
                Exception("Empty response body")
            )

            val decoded = JSON.decodeFromString(
                kotlinx.serialization.encoding.DecodingInputBuffer(
                    body.toCharArray()
                ),
                serializers[0].descriptor
            ) as T

            Result.success(decoded)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun <T> post(
        url: String,
        body: String,
        serializers: List<KSerializer<T>>,
        timeoutMs: Int = 15000
    ): Result<T> {
        return try {
            val request = client.post(url)
            request.body(HttpContentSerializerFactory.contentSerializer(body))
            request.timeout(timeoutMs, TimeUnit.MILLISECONDS)

            val response = request.perform()

            if (response.isErrorStatus()) {
                return Result.failure(
                    Exception("HTTP ${response.status}: ${response.statusMessage}")
                )
            }

            val responseBody = response.body?.string() ?: return Result.failure(
                Exception("Empty response body")
            )

            val decoded = JSON.decodeFromString(
                kotlinx.serialization.encoding.DecodingInputBuffer(
                    responseBody.toCharArray()
                ),
                serializers[0].descriptor
            ) as T

            Result.success(decoded)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// API response wrapper for consistent error handling
sealed class ApiResult<T>(
    val data: T?,
    val error: String?
)

object Success : ApiResult(null, null)
object Failure : ApiResult(null, "Request failed")

// Location search API client
object LocationSearchClient {
    private const val BASE_URL = "https://api.geoplateforme.gouv.fr"

    suspend fun search(query: String): Result<List<SearchLocation>> {
        return ApiHttpClient.get(
            "$BASE_URL/geocode?q=${java.net.URLEncoder.encode(query, "UTF-8")}",
            listOf(SearchLocationSerializer.INSTANCE),
            15000
        ).map { it ?: emptyList() }
            .onError { exception ->
                // Handle specific error types
                if (exception is java.net.SocketTimeoutException) {
                    Result.failure(Exception("Search service timeout"))
                } else if (exception.message?.contains("404") == true) {
                    Result.failure(Exception("No results found for query: $query"))
                } else {
                    Result.failure(Exception("Search service error: ${exception.message}"))
                }
            }
    }

    suspend fun reverseGeocode(lat: Double, lng: Double): Result<SearchLocation> {
        return ApiHttpClient.get(
            "$BASE_URL/reverse?lat=$lat&lng=$lng",
            listOf(SearchLocationSerializer.INSTANCE),
            15000
        ).map { it ?: throw Exception("No reverse geocode result") }
    }
}

// Station API client
object StationApiClient {
    private const val BASE_URL = "/v1"

    suspend fun getNearby(
        latitude: Double,
        longitude: Double,
        radiusKm: Double = 10.0,
        fuelType: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): Result<Page<Station>> {
        val params = mutableMapOf<String, String>(
            "latitude" to latitude.toString(),
            "longitude" to longitude.toString(),
            "radius" to radiusKm.toString(),
            "page" to page.toString(),
            "pageSize" to pageSize.toString()
        )

        if (fuelType != null) {
            params["fuelType"] = fuelType
        }

        return ApiHttpClient.get(
            "$BASE_URL/stations/nearby${params.entries.joinToString("&")}",
            listOf(PageSerializer(INSTANCE)),
            8000
        )
    }

    suspend fun getStationDetails(stationId: String): Result<Station> {
        return ApiHttpClient.get(
            "$BASE_URL/stations/$stationId",
            listOf(StationSerializer.INSTANCE),
            8000
        )
    }

    suspend fun refreshStations(): Result<Unit> {
        return ApiHttpClient.get(
            "$BASE_URL/stations/refresh",
            listOf(),
            30000
        ).map { _ -> Unit }
    }
}