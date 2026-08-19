package jp.metaranai.app

import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

class RecommendationEngine {
    fun recommend(
        profile: MetalVector,
        history: List<DiscoveryRecord>,
        searchHistory: List<SearchRecord> = emptyList(),
        candidates: List<MetalArtist> = MetalCatalog.artists,
        seed: Long = LocalDate.now().toEpochDay()
    ): Recommendation {
        val seen = history.map { it.artistName.lowercase() }.toSet()
        val searched = searchHistory.take(15).map { it.artistName.lowercase() }.toSet()
        val ranked = candidates.distinctBy { it.name.lowercase() }.map { artist ->
            val similarity = profile.similarity(artist.vector)
            val novelty = if (artist.name.lowercase() in seen) 0f else 1f
            val searchInterest = if (artist.name.lowercase() in searched) .3f else 0f
            val exploration = explorationScore(profile, artist.vector)
            val hidden = artist.hiddenScore.coerceIn(0, 100) / 100f
            val discovery = artist.discovery.coerceIn(0f, 1f)
            // V0.4: affinity remains king, but "hidden" is now a first-class ranking dimension.
            val score = similarity * .50f + hidden * .20f + novelty * .15f + exploration * .10f + discovery * .04f + searchInterest * .01f
            artist to score
        }.sortedByDescending { it.second }

        val pool = ranked.filter { it.first.name.lowercase() !in seen }.take(10).ifEmpty { ranked.take(10) }
        val selected = pool[Math.floorMod(seed, pool.size.toLong()).toInt()]
        val artist = selected.first
        val similarity = profile.similarity(artist.vector)
        val compatibility = (similarity * 100).roundToInt()
        val novelty = if (artist.name.lowercase() in seen) 0f else 1f
        val exploration = explorationScore(profile, artist.vector)
        val hidden = artist.hiddenScore.coerceIn(0,100) / 100f
        val breakdown = RecommendationBreakdown(
            affinity = (similarity * 50).roundToInt(),
            hidden = (hidden * 20).roundToInt(),
            novelty = (novelty * 15).roundToInt(),
            exploration = (exploration * 10).roundToInt(),
            discovery = (artist.discovery * 5).roundToInt(),
            total = (similarity * 50 + hidden * 20 + novelty * 15 + exploration * 10 + artist.discovery * 5).roundToInt()
        )
        val traits = strongestMatches(profile, artist.vector)
        return Recommendation(
            artist = artist,
            compatibility = compatibility,
            reason = "${artist.reason}。${traits.joinToString("・")}があなたの傾向と近い。Hidden Score ${artist.hiddenScore}。",
            breakdown = breakdown,
            matchedTraits = traits
        )
    }

    fun updatedProfile(current: MetalVector, artist: MetalArtist, reaction: Reaction): MetalVector = when (reaction) {
        Reaction.HIT -> current.blend(artist.vector, .18f)
        Reaction.MAYBE -> current.blend(artist.vector, .05f)
        Reaction.MISS -> moveAway(current, artist.vector, .10f)
    }

    fun profileFromInterest(current: MetalVector, artist: MetalArtist): MetalVector = current.blend(artist.vector, .025f)

    fun dnaType(v: MetalVector): String {
        val melodic = (v.melody + v.catchy + v.cleanVocal) / 3f
        return when {
            melodic > .88f && v.speed > .78f && v.symphonic > .72f -> "天空疾走型メロディックメタラー"
            v.symphonic > .86f && v.cleanVocal > .82f -> "劇場型シンフォニックメタラー"
            v.technical > .82f && v.heavy > .65f -> "技巧偏重型プログレッシブメタラー"
            v.growl > .62f && v.heavy > .78f -> "極重圧型エクストリームメタラー"
            v.speed > .82f -> "高速巡航型パワーメタラー"
            melodic > .82f -> "旋律至上型メロディックメタラー"
            else -> "探索型オールラウンドメタラー"
        }
    }

    private fun explorationScore(profile: MetalVector, artist: MetalVector): Float {
        val similarity = profile.similarity(artist)
        return (1f - abs(similarity - .80f) / .80f).coerceIn(0f, 1f)
    }

    private fun strongestMatches(a: MetalVector, b: MetalVector): List<String> = a.traits()
        .zip(b.traits())
        .map { (x, y) -> Triple(x.first, 1f - abs(x.second - y.second), (x.second + y.second) / 2f) }
        .filter { it.third > .55f }
        .sortedByDescending { it.second * .65f + it.third * .35f }
        .take(3)
        .map { it.first }

    private fun moveAway(current: MetalVector, disliked: MetalVector, weight: Float): MetalVector {
        fun away(c: Float, d: Float): Float = (c + (c - d) * weight).coerceIn(0f, 1f)
        return MetalVector(
            away(current.melody, disliked.melody), away(current.speed, disliked.speed),
            away(current.heavy, disliked.heavy), away(current.symphonic, disliked.symphonic),
            away(current.technical, disliked.technical), away(current.growl, disliked.growl),
            away(current.cleanVocal, disliked.cleanVocal), away(current.catchy, disliked.catchy)
        )
    }
}
