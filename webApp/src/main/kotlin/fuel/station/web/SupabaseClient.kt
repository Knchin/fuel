package fuel.station.web

import kotlinx.coroutines.await
import kotlinx.coroutines.withTimeout

private fun getSupabaseUrl(): String = js("window.FUEL_CONFIG.SUPABASE_URL") as String
private fun getSupabaseKey(): String = js("window.FUEL_CONFIG.SUPABASE_ANON_KEY") as String

private fun supabaseHeaders(): dynamic {
    val key = getSupabaseKey()
    val h = js("({})")
    js("h.apikey = key")
    js("h['Authorization'] = 'Bearer ' + key")
    js("h['Content-Type'] = 'application/json'")
    js("h['Prefer'] = 'return=representation'")
    return h
}

private suspend fun fetchWithTimeout(url: String, opts: dynamic): dynamic {
    return withTimeout(10000) {
        js("window.fetch(url, opts)").await()
    }
}

suspend fun fetchStations(lat: Double, lng: Double, radiusKm: Double): Array<Station> {
    val rpcUrl = "${getSupabaseUrl()}/rest/v1/rpc/get_nearby_stations_full"
    val body = js("JSON.stringify({p_latitude: lat, p_longitude: lng, p_radius_km: radiusKm})")
    val h = supabaseHeaders()
    val opts = js("({method: 'POST', headers: h, body: body})")
    val response = fetchWithTimeout(rpcUrl, opts)
    val ok = js("response.ok") as Boolean
    if (!ok) throw Exception("Erreur serveur: ${js("response.status")}")
    val jsonText = js("response.text()").await() as String
    val arr = js("JSON.parse(jsonText)") as Array<dynamic>
    return arr.map { parseStation(it) }.toTypedArray()
}

suspend fun fetchAllStations(limit: Int = 5000): Array<Station> {
    val url = "${getSupabaseUrl()}/rest/v1/stations?select=*,fuel_prices(*)&active=eq.true&limit=$limit&order=id"
    val h = supabaseHeaders()
    val opts = js("({method: 'GET', headers: h})")
    val response = fetchWithTimeout(url, opts)
    val ok = js("response.ok") as Boolean
    if (!ok) throw Exception("Erreur serveur: ${js("response.status")}")
    val jsonText = js("response.text()").await() as String
    val arr = js("JSON.parse(jsonText)") as Array<dynamic>
    return arr.map { parseStation(it) }.toTypedArray()
}

suspend fun fetchStationsInViewport(
    minLat: Double,
    minLng: Double,
    maxLat: Double,
    maxLng: Double,
    fuelType: String? = null
): Array<Station> {
    val rpcUrl = "${getSupabaseUrl()}/rest/v1/rpc/viewport_stations"
    val params = if (fuelType != null) {
        js("JSON.stringify({p_min_lat: minLat, p_min_lng: minLng, p_max_lat: maxLat, p_max_lng: maxLng, p_fuel_type: fuelType})")
    } else {
        js("JSON.stringify({p_min_lat: minLat, p_min_lng: minLng, p_max_lat: maxLat, p_max_lng: maxLng, p_fuel_type: null})")
    }
    val h = supabaseHeaders()
    val opts = js("({method: 'POST', headers: h, body: params})")
    val response = fetchWithTimeout(rpcUrl, opts)
    val ok = js("response.ok") as Boolean
    if (!ok) throw Exception("Erreur serveur: ${js("response.status")}")
    val jsonText = js("response.text()").await() as String
    val arr = js("JSON.parse(jsonText)") as Array<dynamic>
    return arr.map { parseStation(it) }.toTypedArray()
}

suspend fun searchStations(query: String): Array<Station> {
    val url = "${getSupabaseUrl()}/rest/v1/stations?select=*&active=eq.true&or=(city.ilike.*$query*,address.ilike.*$query*)&limit=50"
    val h = supabaseHeaders()
    val opts = js("({method: 'GET', headers: h})")
    val response = fetchWithTimeout(url, opts)
    val ok = js("response.ok") as Boolean
    if (!ok) throw Exception("Erreur serveur: ${js("response.status")}")
    val jsonText = js("response.text()").await() as String
    val arr = js("JSON.parse(jsonText)") as Array<dynamic>
    return arr.map { parseStationFromSearch(it) }.toTypedArray()
}

private fun parseStation(raw: dynamic): Station {
    val pricesArr = raw.fuel_prices as? Array<dynamic>
    val prices: Array<FuelPrice>? = if (pricesArr != null && js("Array.isArray(pricesArr)") as Boolean) {
        pricesArr.map { fp ->
            FuelPrice(
                fuelType = (js("fp.fuel_type") as? String) ?: "",
                pricePerLiter = (js("fp.price_per_liter") as? Number)?.toDouble() ?: 0.0,
                reportedAt = (js("fp.reported_at") as? String) ?: "",
                availability = (js("fp.availability") as? String) ?: "disponible",
                ruptureType = js("fp.rupture_type") as? String?
            )
        }.toTypedArray()
    } else null

    return Station(
        id = (js("raw.id") as? String) ?: "",
        sourceId = (js("raw.source_id") as? String) ?: "",
        address = js("raw.address") as? String?,
        postalCode = js("raw.postal_code") as? String?,
        city = js("raw.city") as? String?,
        latitude = (js("raw.latitude") as? Number)?.toDouble() ?: 0.0,
        longitude = (js("raw.longitude") as? Number)?.toDouble() ?: 0.0,
        presenceType = js("raw.presence_type") as? String?,
        openingHours = js("raw.opening_hours") as? String?,
        services = js("raw.services") as? String?,
        source = (js("raw.source") as? String) ?: "",
        active = js("raw.active === true") as Boolean,
        fuelPrices = prices
    )
}

private fun parseStationFromSearch(raw: dynamic): Station {
    return Station(
        id = (js("raw.id") as? String) ?: "",
        sourceId = (js("raw.source_id") as? String) ?: "",
        address = js("raw.address") as? String?,
        postalCode = js("raw.postal_code") as? String?,
        city = js("raw.city") as? String?,
        latitude = (js("raw.latitude") as? Number)?.toDouble() ?: 0.0,
        longitude = (js("raw.longitude") as? Number)?.toDouble() ?: 0.0,
        presenceType = js("raw.presence_type") as? String?,
        openingHours = js("raw.opening_hours") as? String?,
        services = js("raw.services") as? String?,
        source = (js("raw.source") as? String) ?: "",
        active = js("raw.active === true") as Boolean,
        fuelPrices = null
    )
}