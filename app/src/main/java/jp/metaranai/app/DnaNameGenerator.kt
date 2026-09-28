package jp.metaranai.app

object DnaNamePolicy {
    const val REGENERATION_INTERVAL = 5
    fun shouldRegenerate(nextChangeCount: Int): Boolean = nextChangeCount >= REGENERATION_INTERVAL
}

object DnaNameGenerator {
    const val VERSION = 2

    private data class Axis(val key: String, val value: Float, val accent: String)

    fun generate(
        profile: MetalVector,
        vocal: VocalProfile = VocalProfile(),
        history: List<DiscoveryRecord> = emptyList()
    ): String {
        val axes = listOf(
            Axis("melody", profile.melody, "叙情"),
            Axis("speed", profile.speed, "加速"),
            Axis("heavy", profile.heavy, "重厚"),
            Axis("symphonic", profile.symphonic, "荘厳"),
            Axis("technical", profile.technical, "精密"),
            Axis("growl", profile.growl, "獰猛"),
            Axis("clean", profile.cleanVocal, "清麗"),
            Axis("catchy", profile.catchy, "歌心")
        )
        val ranked = axes.sortedWith(compareByDescending<Axis> { it.value }.thenBy { it.key })
        val top = ranked[0]
        val second = ranked[1]
        val third = ranked[2]
        val spread = top.value - ranked.last().value
        val pairMean = (top.value + second.value) / 2f

        val core = if (spread <= .08f && top.value <= .66f) {
            "均衡探索型・オールラウンドメタラー"
        } else {
            val shape = when {
                top.value >= .86f && top.value - second.value >= .12f -> "一点突破型"
                pairMean >= .78f -> "双極覚醒型"
                third.value >= .70f && top.value - third.value <= .14f -> "多軸融合型"
                spread <= .16f -> "全方位融合型"
                else -> "偏愛融合型"
            }
            val pair = pairPhrase(top.key, second.key)
            val thirdAccent = if (third.value >= .66f && top.value - third.value <= .20f) {
                "・${third.accent}アクセント"
            } else {
                ""
            }
            "$shape・$pair$thirdAccent・${archetype(profile)}"
        }

        val qualifiers = buildList {
            listeningQualifier(history)?.let(::add)
            VocalAnalyzer.qualifier(vocal)?.let(::add)
        }
        return (qualifiers + core).joinToString("・")
    }

    private fun pairPhrase(a: String, b: String): String {
        val key = setOf(a, b)
        return pairPhrases[key] ?: "${fallbackToken(a)}${fallbackToken(b)}"
    }

    private val pairPhrases: Map<Set<String>, String> = mapOf(
        setOf("melody", "speed") to "旋律疾走",
        setOf("melody", "heavy") to "叙情重圧",
        setOf("melody", "symphonic") to "幻想旋律",
        setOf("melody", "technical") to "構築旋律",
        setOf("melody", "growl") to "哀愁咆哮",
        setOf("melody", "clean") to "清麗旋律",
        setOf("melody", "catchy") to "歌心旋律",
        setOf("speed", "heavy") to "疾走重装",
        setOf("speed", "symphonic") to "荘厳疾走",
        setOf("speed", "technical") to "高速技巧",
        setOf("speed", "growl") to "暴走咆哮",
        setOf("speed", "clean") to "清唱疾走",
        setOf("speed", "catchy") to "疾走昂揚",
        setOf("heavy", "symphonic") to "荘厳重圧",
        setOf("heavy", "technical") to "重装技巧",
        setOf("heavy", "growl") to "極重咆哮",
        setOf("heavy", "clean") to "重厚清唱",
        setOf("heavy", "catchy") to "重圧昂揚",
        setOf("symphonic", "technical") to "構築幻想",
        setOf("symphonic", "growl") to "深淵荘厳",
        setOf("symphonic", "clean") to "劇場清唱",
        setOf("symphonic", "catchy") to "荘厳歌心",
        setOf("technical", "growl") to "精密咆哮",
        setOf("technical", "clean") to "技巧清麗",
        setOf("technical", "catchy") to "構築昂揚",
        setOf("growl", "clean") to "双声二面",
        setOf("growl", "catchy") to "獰猛昂揚",
        setOf("clean", "catchy") to "清唱歌心"
    )

    private fun fallbackToken(key: String): String = when (key) {
        "melody" -> "旋律"
        "speed" -> "疾走"
        "heavy" -> "重圧"
        "symphonic" -> "荘厳"
        "technical" -> "技巧"
        "growl" -> "咆哮"
        "clean" -> "清唱"
        "catchy" -> "歌心"
        else -> "探索"
    }

    private fun archetype(v: MetalVector): String {
        val scores = listOf(
            "メロディックメタラー" to average(v.melody, v.catchy, v.cleanVocal),
            "パワーメタラー" to average(v.speed, v.melody),
            "シンフォニックメタラー" to average(v.symphonic, v.cleanVocal, v.melody),
            "プログレッシブメタラー" to average(v.technical, v.symphonic, v.heavy),
            "エクストリームメタラー" to average(v.heavy, v.growl, v.speed),
            "ヘヴィメタラー" to average(v.heavy, v.technical)
        )
        return scores.maxBy { it.second }.first
    }

    private fun listeningQualifier(history: List<DiscoveryRecord>): String? {
        val judged = history.filter { it.reaction != Reaction.NOT_FOUND }
        if (judged.size < 8) return null
        val total = judged.size.toFloat()
        val love = judged.count { it.reaction == Reaction.LOVE_ALL } / total
        val positive = judged.count { it.reaction == Reaction.LOVE_ALL || it.reaction == Reaction.HIT } / total
        val selective = judged.count { it.reaction == Reaction.SOME } / total
        val reject = judged.count { it.reaction == Reaction.MEH || it.reaction == Reaction.NO_INTEREST } / total
        return when {
            love >= .30f -> "全曲没入型"
            positive >= .72f -> "高打率共鳴型"
            selective >= .38f -> "選曲発掘型"
            reject >= .45f -> "厳選審美型"
            judged.size >= 20 -> "長期探索型"
            else -> null
        }
    }

    private fun average(vararg values: Float): Float = values.sum() / values.size.coerceAtLeast(1).toFloat()
}
