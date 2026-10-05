package jp.metaranai.app

/** Current-run candidates only. Never treat the entire external cache as a search result. */
object DiscoverySearchResults {
    data class Selection(val artists: List<MetalArtist>, val usedSavedCandidates: Boolean)

    fun choose(
        current: List<MetalArtist>,
        saved: List<MetalArtist>,
        profile: MetalVector,
        genres: List<String>,
        ratedNames: Set<String>
    ): Selection {
        val fresh = select(current, profile, genres, ratedNames)
        if (fresh.isNotEmpty()) return Selection(fresh, false)
        return Selection(select(saved, profile, genres, ratedNames), true)
    }

    fun select(
        artists: List<MetalArtist>,
        profile: MetalVector,
        genres: List<String>,
        ratedNames: Set<String>,
        limit: Int = 24
    ): List<MetalArtist> {
        val rated = ratedNames.map { it.trim().lowercase() }.toSet()
        return GenreLensCatalog.filter(artists, genres)
            .filterNot { it.name.trim().lowercase() in rated }
            .distinctBy { it.name.trim().lowercase() }
            .sortedByDescending { profile.similarity(it.vector) }
            .take(limit)
    }
}
