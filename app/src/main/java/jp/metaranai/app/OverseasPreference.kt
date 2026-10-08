package jp.metaranai.app

/** Artist origin, not lyric language. Unknown origins are excluded in domestic-only mode. */
enum class OverseasPreference(val answer: String, val detail: String) {
    YES("はい", "国内・海外とも今までどおり表示"),
    SOMETIMES("たまに", "海外の候補も表示しつつ、国内を少し優先"),
    NO("いいえ", "日本のアーティストと確認できた候補だけ表示");

    fun allows(artist: MetalArtist): Boolean = this != NO || isJapanese(artist)
    fun weight(artist: MetalArtist): Float = if (this == SOMETIMES && !isJapanese(artist)) .85f else 1f
    fun apply(artists: Collection<MetalArtist>): List<MetalArtist> = artists.filter(::allows)
        .sortedByDescending { weight(it) } // Stable: preserve relevance order within each group.

    companion object {
        fun fromStored(value: String?): OverseasPreference = entries.firstOrNull { it.name == value } ?: YES
        fun isJapanese(artist: MetalArtist): Boolean = listOf(artist.country, artist.area.orEmpty()).any {
            it.trim().lowercase().split(Regex("[\\s/,|()]+"))
                .any { word -> word in setOf("jp", "japan", "日本", "日本国") }
        }
    }
}
