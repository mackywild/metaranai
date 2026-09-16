package jp.metaranai.app

import kotlin.math.abs

data class MetalVector(
    val melody: Float,
    val speed: Float,
    val heavy: Float,
    val symphonic: Float,
    val technical: Float,
    val growl: Float,
    val cleanVocal: Float,
    val catchy: Float
) {
    fun similarity(other: MetalVector): Float {
        val values = listOf(
            melody to other.melody, speed to other.speed, heavy to other.heavy,
            symphonic to other.symphonic, technical to other.technical,
            growl to other.growl, cleanVocal to other.cleanVocal, catchy to other.catchy
        )
        return (1f - values.map { abs(it.first - it.second) }.average().toFloat()).coerceIn(0f, 1f)
    }

    fun blend(other: MetalVector, weight: Float): MetalVector {
        fun mix(a: Float, b: Float) = (a * (1f - weight) + b * weight).coerceIn(0f, 1f)
        return MetalVector(
            mix(melody, other.melody), mix(speed, other.speed), mix(heavy, other.heavy),
            mix(symphonic, other.symphonic), mix(technical, other.technical), mix(growl, other.growl),
            mix(cleanVocal, other.cleanVocal), mix(catchy, other.catchy)
        )
    }

    fun traits(): List<Pair<String, Float>> = listOf(
        "メロディ" to melody, "疾走" to speed, "ヘヴィ" to heavy, "シンフォニック" to symphonic,
        "技巧" to technical, "グロウル" to growl, "クリーンVo" to cleanVocal, "キャッチー" to catchy
    )
}

enum class ArtistSource { BUILTIN, LASTFM, LASTFM_MUSICBRAINZ }

enum class VocalType(val label: String) {
    MALE("男性Vo"), FEMALE("女性Vo"), MIXED("混成Vo"), UNKNOWN("Vo未判定")
}

data class VocalProfile(
    val male: Float = .333f,
    val female: Float = .333f,
    val mixed: Float = .334f,
    val observations: Int = 0
) {
    fun normalized(): VocalProfile {
        val total = (male + female + mixed).coerceAtLeast(.001f)
        return copy(male = male / total, female = female / total, mixed = mixed / total)
    }

    fun dominantType(): VocalType? {
        if (observations < 3) return null
        val n = normalized()
        val best = listOf(VocalType.MALE to n.male, VocalType.FEMALE to n.female, VocalType.MIXED to n.mixed).maxBy { it.second }
        return best.first.takeIf { best.second >= .46f }
    }
}

data class MetalArtist(
    val name: String,
    val country: String,
    val genres: List<String>,
    val vector: MetalVector,
    val discovery: Float,
    val reason: String,
    val source: ArtistSource = ArtistSource.BUILTIN,
    val sourceSeed: String? = null,
    val externalScore: Int? = null,
    val lastFmListeners: Long? = null,
    val lastFmPlaycount: Long? = null,
    val mbid: String? = null,
    val area: String? = null,
    val beginDate: String? = null,
    val endDate: String? = null,
    val ended: Boolean? = null,
    val hiddenScore: Int = 50,
    val metadataConfidence: Int = 0,
    val vocalType: VocalType = VocalType.UNKNOWN
)

/**
 * V0.5.1 five-level feedback.
 * Legacy V0.5 records are migrated by LocalStore:
 * HIT -> HIT, MAYBE -> SOME, MISS -> MEH.
 */
enum class Reaction(
    val label: String,
    val description: String,
    val affinityScore: Int,
    val genreWeight: Float
) {
    LOVE_ALL("💘 全部好き", "アルバム単位でかなり刺さる", 100, 1.40f),
    HIT("🔥 普通に刺さる", "明確に好み", 80, 1.00f),
    SOME("🎵 何曲か刺さる", "一部の曲が刺さる", 60, .45f),
    MEH("😐 イマイチ", "あまり合わない", 30, -.30f),
    NO_INTEREST("💀 興味なし", "フルで聴く気にならない", 0, -1.00f);

    val isPositive: Boolean get() = this == LOVE_ALL || this == HIT || this == SOME
    val isStrongPositive: Boolean get() = this == LOVE_ALL || this == HIT
}

data class DiscoveryRecord(
    val artistName: String,
    val date: String,
    val reaction: Reaction,
    val score: Int
)

data class SearchRecord(val query: String, val artistName: String, val dateTime: String)

enum class GenreLensMode(val label: String) {
    OFF("OFF"), WEEKDAY("曜日"), MANUAL("手動")
}

data class GenreLensConfig(
    val mode: GenreLensMode = GenreLensMode.OFF,
    val manualGenres: Set<String> = emptySet(),
    val weekdayGenres: Map<String, Set<String>> = emptyMap()
)

data class RecommendationBreakdown(
    val affinity: Int,
    val genreLens: Int,
    val hidden: Int,
    val novelty: Int,
    val exploration: Int,
    val discovery: Int,
    val total: Int
)

data class Recommendation(
    val artist: MetalArtist,
    val compatibility: Int,
    val reason: String,
    val breakdown: RecommendationBreakdown,
    val matchedTraits: List<String>,
    val activeGenres: List<String> = emptyList()
)

data class DiscoveryStats(
    val total: Int,
    val favorites: Int,
    val positives: Int,
    val positiveRate: Int,
    val averageAffinity: Int,
    val streakDays: Int
)

data class SpotifySyncResult(
    val summary: String,
    val inferredProfile: MetalVector?,
    val matchedArtists: List<String>,
    val genreSignals: List<String>
)

data class SpotifyArtistDestination(
    val url: String,
    val direct: Boolean,
    val artistId: String? = null
)

data class ArtistSearchResult(
    val query: String,
    val fetched: Int,
    val accepted: Int,
    val results: List<MetalArtist>,
    val cachedArtists: List<MetalArtist>
)
