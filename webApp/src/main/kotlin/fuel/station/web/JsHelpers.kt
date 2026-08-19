package fuel.station.web

internal fun jsObj(): dynamic = js("({})")
internal fun getElementById(id: String): dynamic = js("document.getElementById(id)")

internal fun showElement(id: String) {
    val el = getElementById(id)
    if (el != null) js("el.style.display = 'flex'")
}

internal fun hideElement(id: String) {
    val el = getElementById(id)
    if (el != null) js("el.style.display = 'none'")
}

internal fun setElementText(id: String, text: String) {
    val el = getElementById(id)
    if (el != null) js("el.textContent = text")
}
