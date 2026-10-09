package jp.metaranai.app

object RecommendationPool {
    fun initial(preference: OverseasPreference, genres: List<String> = emptyList()): List<MetalArtist> =
        GenreLensCatalog.filter(preference.apply(MetalCatalog.artists), genres)

    fun unrated(artists: List<MetalArtist>, preference: OverseasPreference, genres: List<String>,
                ratedNames: Set<String>): List<MetalArtist> {
        val rated = ratedNames.map { it.trim().lowercase() }.toSet()
        return GenreLensCatalog.filter(preference.apply(artists), genres)
            .filterNot { it.name.trim().lowercase() in rated }
            .distinctBy { it.name.trim().lowercase() }
    }

    fun needsRefill(artists: List<MetalArtist>, preference: OverseasPreference, genres: List<String>,
                    ratedNames: Set<String>, minimum: Int = 10): Boolean {
        val available = unrated(artists, preference, genres, ratedNames)
        return if (genres.isEmpty()) available.size < minimum
            else GenreLensCatalog.countByGenre(available, genres).values.any { it < minimum }
    }
}
