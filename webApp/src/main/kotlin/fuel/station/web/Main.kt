package fuel.station.web

fun main() {
    val app = MapApp()
    js("window._fuelApp = app")
    js("window._flyToStation = function(s) { app.flyToStation(s) }")
    app.init()
}
