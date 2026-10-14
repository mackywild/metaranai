package jp.metaranai.app

import java.text.Normalizer

/** Explicit aliases only: spelling similarity/romanization never proves artist identity. */
object ArtistAliases {
    private data class Entry(val name: String, val country: String, val aliases: List<String>)
    // Label-confirmed names: wardrecords.com/page/campaign/sabaton/ and
    // columbia.jp/nuclearblast/bands/battlebeast/ . No Spotify IDs are inferred from these names.
    private val entries = listOf(
        Entry("Sabaton", "Sweden", listOf("サバトン")),
        Entry("Battle Beast", "Finland", listOf("バトル・ビースト", "バトルビースト"))
    )
    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .trim().lowercase().replace(Regex("[\\s\\-_・/]+"), "")
    private fun entry(name: String): Entry? = entries.firstOrNull { e ->
        (e.aliases + e.name).any { normalize(it) == normalize(name) }
    }
    fun canonicalName(name: String): String = entry(name)?.name ?: name.trim()
    fun knownCountry(name: String): String? = entry(name)?.country
    fun aliases(name: String): List<String> = entry(name)?.let { it.aliases + it.name }.orEmpty()
    fun names(artist: MetalArtist): List<String> =
        (listOf(artist.name, artist.displayName) + artist.aliases + aliases(artist.name) + aliases(artist.displayName))
            .filter { it.isNotBlank() }.distinctBy(::normalize)
    fun forLookup(artist: MetalArtist): MetalArtist = artist.copy(
        name = artist.displayName,
        country = if (artist.country in listOf("External", "", "Unknown")) knownCountry(artist.name) ?: artist.country else artist.country,
        aliases = names(artist)
    )
    fun availabilityKey(artist: MetalArtist): String = listOf(artist.name, artist.displayName, artist.country,
        artist.mbid.orEmpty(), artist.metadataConfidence.toString(), artist.aliases.sorted().joinToString("|"))
        .joinToString("\u001f")

    /** Preserve the first stored name so existing ratings/search history continue to point at it. */
    fun deduplicate(artists: List<MetalArtist>): List<MetalArtist> {
        val groups = mutableListOf<MutableList<MetalArtist>>()
        val index = mutableMapOf<String, MutableSet<Int>>()
        artists.forEach { artist ->
            val keys = names(artist).map(::normalize) + listOfNotNull(artist.mbid?.let { "mbid:$it" })
            val candidates = keys.flatMap { index[it].orEmpty() }.distinct()
            val existing = candidates.firstOrNull { i -> groups[i].any { sameIdentity(it, artist) } }
            val groupIndex = existing ?: groups.size.also { groups += mutableListOf<MetalArtist>() }
            groups[groupIndex] += artist
            keys.forEach { index.getOrPut(it) { mutableSetOf() } += groupIndex }
        }
        return groups.map { group ->
            val best = group.maxWith(compareBy<MetalArtist> { it.metadataConfidence }.thenBy { it.hiddenScore })
            best.copy(name = group.first().name, canonicalName = best.displayName,
                aliases = group.flatMap(::names).distinctBy(::normalize).filterNot { normalize(it) == normalize(group.first().name) })
        }
    }
    fun expandRatedNames(recordedNames: List<String>, artists: List<MetalArtist>): Set<String> {
        val known = (recordedNames + recordedNames.flatMap(::aliases)).map(::normalize).toSet()
        val matched = artists.filter { artist -> names(artist).any { normalize(it) in known } }
        return (recordedNames + recordedNames.flatMap(::aliases) + matched.flatMap(::names)).map { it.trim().lowercase() }.toSet()
    }
    private fun countryKey(artist: MetalArtist): String = when (val value =
        (if (artist.country == "External") knownCountry(artist.name) ?: artist.country else artist.country).lowercase()) {
        "sweden", "se" -> "SE"
        "finland", "fi" -> "FI"
        "japan", "jp", "日本" -> "JP"
        else -> value
    }
    private fun sameIdentity(a: MetalArtist, b: MetalArtist): Boolean {
        if (!a.mbid.isNullOrBlank() && !b.mbid.isNullOrBlank()) return a.mbid.equals(b.mbid, true)
        // A confirmed alias plus compatible countries; conflicting known countries stay separate.
        val countryA = countryKey(a)
        val countryB = countryKey(b)
        val countriesCompatible = countryA == countryB || countryA == "external" || countryB == "external"
        return countriesCompatible && names(a).any { x -> names(b).any { normalize(x) == normalize(it) } }
    }
}
