package fuel.station.web

import fuel.station.web.external.L
import kotlinx.coroutines.*

private val FUEL_TYPES = listOf("Gazole", "SP95", "E10", "SP98", "E85", "GPLc", "GNV")
private const val DEFAULT_LAT = 48.8566
private const val DEFAULT_LNG = 2.3522
private const val DEFAULT_ZOOM = 12
private const val VIEWPORT_ZOOM_THRESHOLD = 11
private const val DEBOUNCE_MS = 300

fun escapeHtml(s: String): String {
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")
}

fun formatPrice(price: Double): String {
    val s = price.toString()
    val parts = s.split(".")
    if (parts.size == 1) return parts[0] + ",000"
    var dec = parts[1]
    while (dec.length < 3) dec += "0"
    return parts[0] + "," + dec.take(3)
}

fun formatDate(dateStr: String): String {
    if (dateStr.isEmpty()) return ""
    return try {
        val d = js("new Date(dateStr)")
        val day = js("String(d.getUTCDate()).padStart(2, '0')") as String
        val month = js("String(d.getUTCMonth() + 1).padStart(2, '0')") as String
        val year = js("d.getUTCFullYear()") as Int
        val hours = js("String(d.getUTCHours()).padStart(2, '0')") as String
        val mins = js("String(d.getUTCMinutes()).padStart(2, '0')") as String
        "$day/$month/$year $hours:$mins"
    } catch (e: dynamic) {
        dateStr.take(16)
    }
}

fun freshnessClass(dateStr: String): String {
    if (dateStr.isEmpty()) return ""
    return try {
        val reported = js("new Date(dateStr).getTime()") as Double
        val now = js("Date.now()") as Double
        val diffHours = (now - reported) / (1000.0 * 60.0 * 60.0)
        when {
            diffHours < 24 -> "fresh"
            diffHours < 72 -> "aging"
            else -> "stale"
        }
    } catch (e: dynamic) {
        ""
    }
}

class MapApp {
    private var map: dynamic = null
    private var allStations: Array<Station> = emptyArray()
    private var currentMarkers: Array<dynamic> = emptyArray()
    private var selectedFilters: MutableSet<String> = mutableSetOf()
    private var userMarker: dynamic = null
    private var searchTimeout: Int? = null
    private var moveendTimeout: Int? = null
    private var isViewportLoading: Boolean = false
    private var totalLoaded: Int = 0

    fun init() {
        setupMap()
        setupFilters()
        setupSearch()
        setupLocateButton()
        setupMoveendListener()
        loadAllStations()
    }

    private fun setupMap() {
        map = L.map("map", js("({})")).setView(arrayOf(DEFAULT_LAT, DEFAULT_LNG), DEFAULT_ZOOM)
        L.tileLayer(
            "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
            js("({attribution: '\\u00a9 OpenStreetMap contributors', maxZoom: 19})")
        ).addTo(map)
        L.control.scale(js("({imperial: false})")).addTo(map)
    }

    private fun setupMoveendListener() {
        map.on("moveend", { onMapMoveEnd() })
    }

    fun onMapMoveEnd() {
        moveendTimeout?.let { js("window.clearTimeout(it)") }
        moveendTimeout = js("window.setTimeout(function() { window._fuelApp.onMoveEndDebounced() }, $DEBOUNCE_MS)") as Int
    }

    fun onMoveEndDebounced() {
        val zoom = map.getZoom() as Int
        if (zoom >= VIEWPORT_ZOOM_THRESHOLD) {
            fetchAndRenderViewport()
        } else {
            updateMarkersFromSource(allStations)
        }
    }

    private fun fetchAndRenderViewport() {
        if (isViewportLoading) return
        isViewportLoading = true
        showViewportLoading()

        val bounds = map.getBounds()
        val ne = bounds.getNorthEast()
        val sw = bounds.getSouthWest()
        val minLat = sw.lat as Double
        val minLng = sw.lng as Double
        val maxLat = ne.lat as Double
        val maxLng = ne.lng as Double

        val fuelType = if (selectedFilters.size == 1) selectedFilters.first() else null

        runPromise {
            try {
                val stations = fetchStationsInViewport(minLat, minLng, maxLat, maxLng, fuelType)
                isViewportLoading = false
                hideViewportLoading()
                updateMarkersFromSource(stations)
            } catch (e: Exception) {
                isViewportLoading = false
                hideViewportLoading()
                showError("Erreur de chargement: ${e.message}")
            }
        }
    }

    private fun showViewportLoading() {
        setElementText("station-count", "Chargement...")
    }

    private fun hideViewportLoading() {
        // count is updated by updateMarkersFromSource
    }

    private fun setupFilters() {
        val container = getElementById("filter-bar") ?: return
        val allBtn = js("document.createElement('button')")
        js("allBtn.className = 'filter-btn active'")
        js("allBtn.textContent = 'Tous'")
        js("allBtn.onclick = function() { window._fuelApp.clearFilters() }")
        js("container.appendChild(allBtn)")

        for (fuel in FUEL_TYPES) {
            val btn = js("document.createElement('button')")
            js("btn.className = 'filter-btn'")
            js("btn.textContent = fuel")
            js("btn.setAttribute('data-fuel', fuel)")
            js("btn.onclick = function(f) { return function() { window._fuelApp.toggleFilter(f) } }(fuel)")
            js("container.appendChild(btn)")
        }
    }

    fun clearFilters() {
        selectedFilters.clear()
        updateFilterButtons()
        refreshView()
    }

    fun toggleFilter(fuel: String) {
        if (selectedFilters.contains(fuel)) {
            selectedFilters.remove(fuel)
        } else {
            selectedFilters.add(fuel)
        }
        updateFilterButtons()
        refreshView()
    }

    private fun refreshView() {
        val zoom = map.getZoom() as Int
        if (zoom >= VIEWPORT_ZOOM_THRESHOLD) {
            fetchAndRenderViewport()
        } else {
            updateMarkersFromSource(allStations)
        }
    }

    private fun updateFilterButtons() {
        val buttons = js("document.querySelectorAll('.filter-btn')")
        val allActive = selectedFilters.isEmpty()
        val len = js("buttons.length") as Int
        for (i in 0 until len) {
            val btn = js("buttons[i]")
            val fuel = js("btn.getAttribute('data-fuel')") as String?
            if (fuel == null) {
                js("btn.className = allActive ? 'filter-btn active' : 'filter-btn'")
            } else {
                val isActive = selectedFilters.contains(fuel)
                js("btn.className = isActive ? 'filter-btn active' : 'filter-btn'")
            }
        }
    }

    private fun setupSearch() {
        val input = getElementById("search-input") ?: return
        js("input.addEventListener('input', function() { window._fuelApp.onSearchInput() })")
        js("input.addEventListener('blur', function() { window.setTimeout(function() { window._fuelApp.onSearchBlur() }, 200) })")
    }

    fun onSearchInput() {
        val input = getElementById("search-input") ?: return
        val query = js("input.value.trim()") as String
        val resultsDiv = getElementById("search-results") ?: return

        if (query.length < 2) {
            js("resultsDiv.innerHTML = ''")
            js("resultsDiv.style.display = 'none'")
            return
        }

        searchTimeout?.let { js("window.clearTimeout(it)") }
        val q = query
        searchTimeout = js("window.setTimeout(function() { window._fuelApp.doSearch(q) }, 300)") as Int
    }

    fun onSearchBlur() {
        val resultsDiv = getElementById("search-results") ?: return
        js("resultsDiv.innerHTML = ''")
        js("resultsDiv.style.display = 'none'")
    }

    fun doSearch(query: String) {
        runPromise {
            try {
                val results = searchStations(query)
                val resultsDiv = getElementById("search-results") ?: return@runPromise
                js("resultsDiv.innerHTML = ''")
                if (results.isEmpty()) {
                    js("resultsDiv.style.display = 'none'")
                    return@runPromise
                }
                js("resultsDiv.style.display = 'block'")
                for (station in results) {
                    val item = js("document.createElement('div')")
                    js("item.className = 'search-result-item'")
                    val cityPart = station.city?.let { ", $it" } ?: ""
                    val label = "${escapeHtml(station.displayName())}$cityPart"
                    js("item.textContent = label")
                    js("(function(s) { item.addEventListener('mousedown', function(e) { e.preventDefault(); window._flyToStation(s) }) })(station)")
                    js("resultsDiv.appendChild(item)")
                }
            } catch (e: dynamic) {
                // ignore
            }
        }
    }

    fun flyToStation(station: Station) {
        val lat = station.latitude
        val lng = station.longitude
        map.flyTo(arrayOf(lat, lng), 15, js("({duration: 1.5})"))
        showStationDetail(station)
    }

    private fun setupLocateButton() {
        val btn = getElementById("locate-btn") ?: return
        js("btn.addEventListener('click', function() { window._fuelApp.onLocateClick() })")
    }

    fun onLocateClick() {
        val hasGeo = js("typeof navigator.geolocation !== 'undefined'") as Boolean
        if (!hasGeo) return
        showLoading("Localisation...")
        js("navigator.geolocation.getCurrentPosition(function(pos) { window._fuelApp.onGeoSuccess(pos) }, function(err) { window._fuelApp.onGeoError(err) })")
    }

    fun onGeoSuccess(pos: dynamic) {
        hideLoading()
        val lat = js("pos.coords.latitude") as Double
        val lng = js("pos.coords.longitude") as Double
        map.flyTo(arrayOf(lat, lng), 13, js("({duration: 1.5})"))
        if (userMarker != null) {
            map.removeLayer(userMarker)
        }
        userMarker = L.circleMarker(arrayOf(lat, lng), js("({radius: 8, fillColor: '#3b82f6', color: '#ffffff', weight: 3, fillOpacity: 1.0})")).addTo(map)
    }

    fun onGeoError(err: dynamic) {
        hideLoading()
        showError("Impossible d'obtenir votre position")
    }

    private fun loadAllStations() {
        showLoading("Chargement des stations...")
        runPromise {
            try {
                val stations = fetchAllStations()
                allStations = stations
                totalLoaded = stations.size
                hideLoading()
                updateMarkersFromSource(allStations)
            } catch (e: Exception) {
                hideLoading()
                showError("Erreur de chargement: ${e.message}")
            }
        }
    }

    private fun updateMarkersFromSource(stations: Array<Station>) {
        for (m in currentMarkers) {
            map.removeLayer(m)
        }
        currentMarkers = emptyArray()

        val markers = mutableListOf<dynamic>()
        var count = 0

        for (station in stations) {
            val prices = station.fuelPrices
            if (prices == null || prices.isEmpty()) continue

            var bestPrice: FuelPrice? = null
            if (selectedFilters.isNotEmpty()) {
                for (filter in selectedFilters) {
                    val p = station.priceForType(filter)
                    if (p != null && (bestPrice == null || p.pricePerLiter < bestPrice.pricePerLiter)) {
                        bestPrice = p
                    }
                }
            } else {
                bestPrice = station.cheapestPrice()
            }

            if (bestPrice == null) continue

            val color = priceColor(bestPrice.pricePerLiter, station)
            val lat = station.latitude
            val lng = station.longitude
            val marker = L.circleMarker(arrayOf(lat, lng), js("({radius: 7, fillColor: color, color: '#ffffff', weight: 2, fillOpacity: 0.85})"))

            val tooltipText = "${bestPrice.fuelType}: ${formatPrice(bestPrice.pricePerLiter)} \u20ac/L"
            marker.bindTooltip(tooltipText, js("({permanent: false, direction: 'top'})"))

            val s = station
            marker.on("click", js("function() { window._fuelApp.showStationDetail(s) }"))

            marker.addTo(map)
            markers.add(marker)
            count++

            if (count >= 5000) break
        }

        currentMarkers = markers.toTypedArray()

        val zoom = map.getZoom() as Int
        if (zoom >= VIEWPORT_ZOOM_THRESHOLD) {
            setElementText("station-count", "$count stations (zone)")
        } else {
            setElementText("station-count", "$count stations affich\u00e9es / $totalLoaded")
        }

        if (count == 0) {
            showEmptyState(stations.isEmpty())
        } else {
            hideEmptyState()
        }
    }

    private fun showEmptyState(isSourceEmpty: Boolean) {
        var msg = "Aucune station avec prix disponible dans cette zone"
        if (isSourceEmpty) {
            msg = "Aucune station trouv\u00e9e"
        }
        val countEl = getElementById("station-count") ?: return
        js("countEl.textContent = msg")
    }

    private fun hideEmptyState() {
        // handled by updateMarkersFromSource setting count text
    }

    private fun updateMarkers() {
        updateMarkersFromSource(allStations)
    }

    private fun priceColor(price: Double, station: Station): String {
        val allPrices = mutableListOf<Double>()
        station.fuelPrices?.forEach {
            if (it.pricePerLiter > 0 && it.availability != "indisponible") {
                allPrices.add(it.pricePerLiter)
            }
        }
        if (allPrices.isEmpty()) return "#94a3b8"
        val minP = allPrices.minOrNull() ?: return "#94a3b8"
        val maxP = allPrices.maxOrNull() ?: minP
        if (maxP == minP) return "#22c55e"
        val ratio = (price - minP) / (maxP - minP)
        return when {
            ratio < 0.33 -> "#22c55e"
            ratio < 0.66 -> "#eab308"
            else -> "#ef4444"
        }
    }

    fun showStationDetail(station: Station) {
        val content = getElementById("detail-content") ?: return

        val html = StringBuilder()
        html.append("<div class='detail-header'>")
        html.append("<h2>${escapeHtml(station.displayName())}</h2>")
        html.append("<button id='detail-close' class='detail-close-btn'>&times;</button>")
        html.append("</div>")

        val locationParts = mutableListOf<String>()
        station.address?.let { locationParts.add(it) }
        station.postalCode?.let { locationParts.add(it) }
        station.city?.let { locationParts.add(it) }
        if (locationParts.isNotEmpty()) {
            html.append("<div class='detail-location'>${escapeHtml(locationParts.joinToString(" "))}</div>")
        }

        if (station.fuelPrices != null && station.fuelPrices.isNotEmpty()) {
            html.append("<div class='detail-prices'>")
            html.append("<h3>Prix des carburants</h3>")
            val sorted = station.fuelPrices.sortedBy { it.fuelType }
            for (price in sorted) {
                val freshness = freshnessClass(price.reportedAt)
                val avail = if (price.availability == "indisponible") " <span class='unavailable'>Indisponible</span>" else ""
                val priceStr = if (price.pricePerLiter > 0) formatPrice(price.pricePerLiter) else "N/A"
                html.append("<div class='price-row'>")
                html.append("<span class='price-fuel'>${escapeHtml(price.fuelType)}</span>")
                html.append("<span class='price-value $freshness'>$priceStr \u20ac/L$avail</span>")
                html.append("<span class='price-date'>${formatDate(price.reportedAt)}</span>")
                html.append("</div>")
            }
            html.append("</div>")
        }

        if (station.openingHours != null && station.openingHours!!.isNotEmpty()) {
            html.append("<div class='detail-info'>")
            html.append("<h3>Horaires</h3>")
            html.append("<p>${escapeHtml(station.openingHours!!)}</p>")
            html.append("</div>")
        }

        if (station.services != null && station.services!!.isNotEmpty()) {
            html.append("<div class='detail-info'>")
            html.append("<h3>Services</h3>")
            html.append("<p>${escapeHtml(station.services!!)}</p>")
            html.append("</div>")
        }

        val gmapsUrl = "https://www.google.com/maps/dir/?api=1&destination=${station.latitude},${station.longitude}"
        html.append("<a href='$gmapsUrl' target='_blank' rel='noopener' class='directions-btn'>Itin\u00e9raire Google Maps</a>")

        val htmlStr = html.toString()
        js("content.innerHTML = htmlStr")
        js("document.getElementById('detail-close').addEventListener('click', function() { window._fuelApp.closeDetail() })")

        val panel = getElementById("detail-panel") ?: return
        js("panel.classList.add('open')")
    }

    fun closeDetail() {
        val panel = getElementById("detail-panel") ?: return
        js("panel.classList.remove('open')")
    }
}

private fun showLoading(message: String) {
    setElementText("loading-text", message)
    showElement("loading-overlay")
}

private fun hideLoading() {
    hideElement("loading-overlay")
}

private fun showError(message: String) {
    val el = getElementById("error-message") ?: return
    js("el.textContent = message")
    js("el.style.display = 'block'")
    js("window.setTimeout(function() { el.style.display = 'none' }, 5000)")
}

private fun runPromise(block: suspend () -> Unit) {
    GlobalScope.launch {
        block()
    }
}
