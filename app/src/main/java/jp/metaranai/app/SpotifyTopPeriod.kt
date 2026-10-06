package jp.metaranai.app

/** Spotify computes affinity over approximate, provider-defined periods. */
enum class SpotifyTopPeriod(val apiValue: String, val label: String) {
    MONTH("short_term", "約1か月"),
    HALF_YEAR("medium_term", "約6か月"),
    YEAR("long_term", "約1年");

    companion object {
        fun fromApiValue(value: String?): SpotifyTopPeriod =
            entries.firstOrNull { it.apiValue == value } ?: HALF_YEAR
    }
}

data class SpotifyTopSnapshot(
    val period: SpotifyTopPeriod,
    val artists: List<String>,
    val fetchedAt: String = ""
)
