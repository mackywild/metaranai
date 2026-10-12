package jp.metaranai.app

object SearchQueryMatcher {
    fun terms(query: String): List<String> =
        query.trim()
            .split(Regex("\\s+"))
            .map { normalize(it) }
            .filter { it.isNotBlank() }

    fun matches(artist: MetalArtist, query: String): Boolean {
        val terms = terms(query)
        if (terms.isEmpty()) return true

        val fields = buildList {
            ArtistAliases.names(artist).forEach { add(normalize(it)) }
            add(normalize(artist.country))
            artist.genres.forEach { add(normalize(it)) }
        }

        return terms.all { term -> fields.any { field -> field.contains(term) } }
    }

    fun exactName(artist: MetalArtist, query: String): Boolean =
        ArtistAliases.names(artist).any { normalize(it) == normalize(query) }

    private fun normalize(value: String): String =
        java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKC).trim()
            .lowercase()
            .replace(Regex("[\\s\\-_・/]+"), "")
}
