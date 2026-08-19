package fuel.station.web

import kotlinx.coroutines.await

private const val SUPABASE_URL = "https://uyeipadtsvbtrvtimotz.supabase.co"
private const val SUPABASE_ANON_KEY = "sb_publishable_9JfGTlFSJf4gdS7Bg5Bl5w_KsUtHWn4"

private fun supabaseHeaders(): dynamic {
    val h = js("({})")
    js("h.apikey = 'sb_publishable_9JfGTlFSJf4gdS7Bg5Bl5w_KsUtHWn4'")
    js("h['Authorization'] = 'Bearer sb_publishable_9JfGTlFSJf4gdS7Bg5Bl5w_KsUtHWn4'")
    js("h['Content-Type'] = 'application/json'")
    js("h['Prefer'] = 'return=representation'")
    return h
}

suspend fun fetchStations(lat: Double, lng: Double, radiusKm: Double): Array<Station> {
    val rpcUrl = "$SUPABASE_URL/rest/v1/rpc/get_nearby_stations_full"
    val body = js("JSON.stringify({p_latitude: lat, p_longitude: lng, p_radius_km: radiusKm})")
    val h = supabaseHeaders()
    val opts = js("({method: 'POST', headers: h, body: body})")
    val response = js("window.fetch(rpcUrl, opts)").await()
    val ok = js("response.ok") as Boolean
    if (!ok) throw Exception("Erreur serveur: ${js("response.status")}")
    val jsonText = js("response.text()").await() as String
    val arr = js("JSON.parse(jsonText)") as Array<dynamic>
    return arr.map { parseStation(it) }.toTypedArray()
}

suspend fun fetchAllStations(limit: Int = 5000): Array<Station> {
    val url = "$SUPABASE_URL/rest/v1/stations?select=*,fuel_prices(*)&active=eq.true&limit=$limit&order=id"
    val h = supabaseHeaders()
    val opts = js("({method: 'GET', headers: h})")
    val response = js("window.fetch(url, opts)").await()
    val ok = js("response.ok") as Boolean
    if (!ok) throw Exception("Erreur serveur: ${js("response.status")}")
    val jsonText = js("response.text()").await() as String
    val arr = js("JSON.parse(jsonText)") as Array<dynamic>
    return arr.map { parseStation(it) }.toTypedArray()
}

suspend fun searchStations(query: String): Array<Station> {
    val url = "$SUPABASE_URL/rest/v1/stations?select=*&active=eq.true&or=(city.ilike.*$query*,address.ilike.*$query*)&limit=50"
    val h = supabaseHeaders()
    val opts = js("({method: 'GET', headers: h})")
    val response = js("window.fetch(url, opts)").await()
    val ok = js("response.ok") as Boolean
    if (!ok) throw Exception("Erreur serveur: ${js("response.status")}")
    val jsonText = js("response.text()").await() as String
    val arr = js("JSON.parse(jsonText)") as Array<dynamic>
    return arr.map { parseStationFromSearch(it) }.toTypedArray()
}

private fun parseStation(raw: dynamic): Station {
    val pricesArr = js("raw.fuel_prices") as? Array<dynamic>
    val prices: Array<FuelPrice>? = if (pricesArr != null) {
        pricesArr.map { fp ->
            FuelPrice(
                fuelType = js("fp.fuel_type || ''") as String,
                pricePerLiter = (js("fp.price_per_liter") as? Number)?.toDouble() ?: 0.0,
                reportedAt = js("fp.reported_at || ''") as String,
                availability = js("fp.availability || 'disponible'") as String,
                ruptureType = js("fp.rupture_type || null") as String?
            )
        }.toTypedArray()
    } else null

    return Station(
        id = js("raw.id != null ? String(raw.id) : ''") as String,
        sourceId = js("raw.source_id != null ? String(raw.source_id) : ''") as String,
        address = js("raw.address || null") as String?,
        postalCode = js("raw.postal_code || null") as String?,
        city = js("raw.city || null") as String?,
        latitude = (js("raw.latitude") as? Number)?.toDouble() ?: 0.0,
        longitude = (js("raw.longitude") as? Number)?.toDouble() ?: 0.0,
        presenceType = js("raw.presence_type || null") as String?,
        openingHours = js("raw.opening_hours || null") as String?,
        services = js("raw.services || null") as String?,
        source = js("raw.source || ''") as String,
        active = js("raw.active === true || raw.active === 'true'") as Boolean,
        fuelPrices = prices
    )
}

private fun parseStationFromSearch(raw: dynamic): Station {
    return Station(
        id = js("raw.id != null ? String(raw.id) : ''") as String,
        sourceId = js("raw.source_id != null ? String(raw.source_id) : ''") as String,
        address = js("raw.address || null") as String?,
        postalCode = js("raw.postal_code || null") as String?,
        city = js("raw.city || null") as String?,
        latitude = (js("raw.latitude") as? Number)?.toDouble() ?: 0.0,
        longitude = (js("raw.longitude") as? Number)?.toDouble() ?: 0.0,
        presenceType = js("raw.presence_type || null") as String?,
        openingHours = js("raw.opening_hours || null") as String?,
        services = js("raw.services || null") as String?,
        source = js("raw.source || ''") as String,
        active = js("raw.active === true || raw.active === 'true'") as Boolean,
        fuelPrices = null
    )
}
