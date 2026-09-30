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
            add(normalize(artist.name))
            add(normalize(artist.country))
            artist.genres.forEach { add(normalize(it)) }
        }

        return terms.all { term -> fields.any { field -> field.contains(term) } }
    }

    fun exactName(artist: MetalArtist, query: String): Boolean =
        normalize(artist.name) == normalize(query)

    private fun normalize(value: String): String =
        value.trim()
            .lowercase()
            .replace(Regex("[\\s\\-_・/]+"), "")
}
