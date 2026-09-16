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
        genreLens: List<String> = emptyList(),
        seed: Long = LocalDate.now().toEpochDay()
    ): Recommendation {
        require(candidates.isNotEmpty()) { "推薦候補がありません" }
        val seen = history.map { it.artistName.lowercase() }.toSet()
        val searched = searchHistory.take(30).map { it.artistName.lowercase() }.toSet()
        val lensActive = genreLens.isNotEmpty()
        val unique = candidates.distinctBy { it.name.lowercase() }
        // V0.5.2: Genre Lens is a WHERE clause, not a score bonus.
        val eligible = if (lensActive) GenreLensCatalog.filter(unique, genreLens) else unique
        require(eligible.isNotEmpty()) { "Genre Lens候補がありません: ${genreLens.joinToString(" / ")}" }
        val ranked = eligible.map { artist ->
            val similarity = profile.similarity(artist.vector)
            val lensScore = GenreLensCatalog.score(artist, genreLens)
            val novelty = if (artist.name.lowercase() in seen) 0f else 1f
            val searchInterest = if (artist.name.lowercase() in searched) .3f else 0f
            val exploration = explorationScore(profile, artist.vector)
            val hidden = artist.hiddenScore.coerceIn(0, 100) / 100f
            val discovery = artist.discovery.coerceIn(0f, 1f)
            val score = if (lensActive) {
                // All candidates already satisfy the requested genre. Personal DNA stays the strongest sorter.
                similarity * .52f + lensScore * .08f + hidden * .16f + novelty * .11f + exploration * .07f + discovery * .04f + searchInterest * .02f
            } else {
                similarity * .50f + hidden * .20f + novelty * .15f + exploration * .10f + discovery * .04f + searchInterest * .01f
            }
            artist to score
        }.sortedByDescending { it.second }

        val pool = ranked.filter { it.first.name.lowercase() !in seen }.take(12).ifEmpty { ranked.take(12) }
        val selected = pool[Math.floorMod(seed, pool.size.toLong()).toInt()]
        val artist = selected.first
        val similarity = profile.similarity(artist.vector)
        val compatibility = (similarity * 100).roundToInt()
        val novelty = if (artist.name.lowercase() in seen) 0f else 1f
        val exploration = explorationScore(profile, artist.vector)
        val hidden = artist.hiddenScore.coerceIn(0,100) / 100f
        val lensScore = GenreLensCatalog.score(artist, genreLens)
        val breakdown = if (lensActive) {
            RecommendationBreakdown(
                affinity = (similarity * 52).roundToInt(),
                genreLens = (lensScore * 8).roundToInt(),
                hidden = (hidden * 16).roundToInt(),
                novelty = (novelty * 11).roundToInt(),
                exploration = (exploration * 7).roundToInt(),
                discovery = (artist.discovery * 4).roundToInt(),
                total = (similarity * 52 + lensScore * 8 + hidden * 16 + novelty * 11 + exploration * 7 + artist.discovery * 4).roundToInt()
            )
        } else {
            RecommendationBreakdown(
                affinity = (similarity * 50).roundToInt(),
                genreLens = 0,
                hidden = (hidden * 20).roundToInt(),
                novelty = (novelty * 15).roundToInt(),
                exploration = (exploration * 10).roundToInt(),
                discovery = (artist.discovery * 5).roundToInt(),
                total = (similarity * 50 + hidden * 20 + novelty * 15 + exploration * 10 + artist.discovery * 5).roundToInt()
            )
        }
        val traits = strongestMatches(profile, artist.vector)
        val lensText = if (genreLens.isNotEmpty()) "。Genre Lens必須条件: ${genreLens.joinToString(" / ")}" else ""
        return Recommendation(
            artist = artist,
            compatibility = compatibility,
            reason = "${artist.reason}。${traits.joinToString("・")}があなたの傾向と近い。Hidden Score ${artist.hiddenScore}$lensText。",
            breakdown = breakdown,
            matchedTraits = traits,
            activeGenres = genreLens
        )
    }

    /** V0.5.1: five levels with asymmetric learning weights. */
    fun updatedProfile(current: MetalVector, artist: MetalArtist, reaction: Reaction): MetalVector = when (reaction) {
        Reaction.LOVE_ALL -> current.blend(artist.vector, .28f)
        Reaction.HIT -> current.blend(artist.vector, .18f)
        Reaction.SOME -> current.blend(artist.vector, .07f)
        Reaction.MEH -> moveAway(current, artist.vector, .06f)
        Reaction.NO_INTEREST -> moveAway(current, artist.vector, .18f)
    }

    fun profileFromInterest(current: MetalVector, artist: MetalArtist): MetalVector = current.blend(artist.vector, .025f)

    fun dnaType(v: MetalVector, vocal: VocalProfile = VocalProfile(), history: List<DiscoveryRecord> = emptyList()): String {
        val melodic = (v.melody + v.catchy + v.cleanVocal) / 3f
        val base = when {
            melodic > .88f && v.speed > .78f && v.symphonic > .72f -> "天空疾走型メロディックメタラー"
            v.symphonic > .86f && v.cleanVocal > .82f -> "劇場型シンフォニックメタラー"
            v.technical > .82f && v.heavy > .65f -> "技巧偏重型プログレッシブメタラー"
            v.growl > .62f && v.heavy > .78f -> "極重圧型エクストリームメタラー"
            v.speed > .82f -> "高速巡航型パワーメタラー"
            melodic > .82f -> "旋律至上型メロディックメタラー"
            else -> "探索型オールラウンドメタラー"
        }
        val qualifiers = buildList {
            listeningQualifier(history)?.let(::add)
            VocalAnalyzer.qualifier(vocal)?.let(::add)
        }
        return if (qualifiers.isEmpty()) base else "${qualifiers.joinToString("・")}・$base"
    }

    private fun listeningQualifier(history: List<DiscoveryRecord>): String? {
        if (history.size < 8) return null
        val total = history.size.toFloat()
        val love = history.count { it.reaction == Reaction.LOVE_ALL } / total
        val selective = history.count { it.reaction == Reaction.SOME } / total
        val reject = history.count { it.reaction == Reaction.MEH || it.reaction == Reaction.NO_INTEREST } / total
        return when {
            love >= .25f -> "全曲没入型"
            selective >= .40f -> "選曲発掘型"
            reject >= .45f -> "厳選審美型"
            else -> null
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
