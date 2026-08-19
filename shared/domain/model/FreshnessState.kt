package fuel.station.shared.domain.model

import fuel.station.shared.domain.enum.Availability

sealed interface FreshnessState {
    data class Fresh: FreshnessState()
    data class Aging: FreshnessState()
    data class Stale: FreshnessState()
    data class VeryStale: FreshnessState()
}

data class Page<T>(
    val items: List<T>,
    val hasNext: Boolean,
    val page: Int,
    val pageSize: Int,
    val totalItems: Int
)