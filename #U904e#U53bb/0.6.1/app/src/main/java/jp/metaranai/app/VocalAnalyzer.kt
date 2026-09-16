package jp.metaranai.app

object VocalAnalyzer {
    fun infer(tags: List<String>): VocalType {
        val t = tags.map { it.lowercase() }
        val female = t.any { "female vocal" in it || "female-fronted" in it || "female fronted" in it }
        val male = t.any { "male vocal" in it || "male-fronted" in it || "male fronted" in it }
        val mixed = t.any { "mixed vocal" in it || "dual vocal" in it || "male and female" in it || "female and male" in it }
        return when {
            mixed || (female && male) -> VocalType.MIXED
            female -> VocalType.FEMALE
            male -> VocalType.MALE
            else -> VocalType.UNKNOWN
        }
    }

    fun update(current: VocalProfile, type: VocalType, reaction: Reaction): VocalProfile {
        if (type == VocalType.UNKNOWN) return current
        val weight = when (reaction) {
            Reaction.LOVE_ALL -> .25f
            Reaction.HIT -> .18f
            Reaction.SOME -> .07f
            Reaction.MEH -> -.05f
            Reaction.NO_INTEREST -> -.14f
        }
        fun move(value: Float, target: Boolean): Float {
            return if (weight >= 0f) {
                (value * (1f - weight) + (if (target) 1f else 0f) * weight).coerceIn(.02f, .96f)
            } else {
                val w = -weight
                (value + (if (target) -w else w / 2f)).coerceIn(.02f, .96f)
            }
        }
        return VocalProfile(
            male = move(current.male, type == VocalType.MALE),
            female = move(current.female, type == VocalType.FEMALE),
            mixed = move(current.mixed, type == VocalType.MIXED),
            observations = current.observations + 1
        ).normalized().copy(observations = current.observations + 1)
    }

    fun qualifier(profile: VocalProfile): String? = when (profile.dominantType()) {
        VocalType.MALE -> "男性Vo偏愛"
        VocalType.FEMALE -> "女性Vo偏愛"
        VocalType.MIXED -> "混成Vo偏愛"
        else -> null
    }
}
