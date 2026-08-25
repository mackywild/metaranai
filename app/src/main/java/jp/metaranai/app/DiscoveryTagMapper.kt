package jp.metaranai.app

object DiscoveryTagMapper {
    private val metalWords = setOf(
        "metal", "heavy metal", "power metal", "symphonic metal", "progressive metal", "melodic metal",
        "speed metal", "neoclassical metal", "folk metal", "gothic metal", "melodic death metal",
        "death metal", "thrash metal", "doom metal", "black metal", "metalcore", "j-metal", "japanese metal"
    )

    fun isMetalTag(tag: String): Boolean {
        val t = tag.lowercase()
        return metalWords.any { t == it || t.contains(it) } || t.endsWith(" metal")
    }

    fun displayTag(tag: String): String = tag.split(' ', '-').joinToString(" ") { w ->
        w.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    fun vectorFromTags(tags: List<String>): MetalVector {
        var v = MetalVector(.68f, .58f, .62f, .35f, .42f, .30f, .68f, .56f)
        fun blend(target: MetalVector, w: Float) { v = v.blend(target, w) }
        tags.take(12).forEachIndexed { i, raw ->
            val t = raw.lowercase()
            val w = (0.28f - i * 0.012f).coerceAtLeast(.08f)
            when {
                "power metal" in t -> blend(MetalVector(.91f,.88f,.56f,.62f,.55f,.08f,.94f,.89f), w)
                "symphonic" in t -> blend(MetalVector(.88f,.64f,.58f,.96f,.58f,.16f,.89f,.82f), w)
                "melodic death" in t -> blend(MetalVector(.82f,.76f,.82f,.46f,.65f,.80f,.40f,.66f), w)
                "progressive" in t -> blend(MetalVector(.72f,.58f,.72f,.43f,.94f,.34f,.75f,.53f), w)
                "neoclassical" in t -> blend(MetalVector(.93f,.82f,.58f,.70f,.90f,.08f,.91f,.84f), w)
                "speed metal" in t -> blend(MetalVector(.76f,.96f,.70f,.30f,.61f,.22f,.78f,.69f), w)
                "folk metal" in t -> blend(MetalVector(.82f,.68f,.67f,.55f,.48f,.35f,.73f,.77f), w)
                "gothic" in t -> blend(MetalVector(.72f,.45f,.68f,.76f,.42f,.38f,.76f,.61f), w)
                "death metal" in t -> blend(MetalVector(.48f,.72f,.92f,.22f,.62f,.94f,.12f,.33f), w)
                "thrash" in t -> blend(MetalVector(.48f,.86f,.88f,.15f,.68f,.46f,.55f,.45f), w)
                "doom" in t -> blend(MetalVector(.62f,.22f,.91f,.35f,.40f,.48f,.58f,.34f), w)
                "black metal" in t -> blend(MetalVector(.55f,.72f,.86f,.48f,.52f,.90f,.22f,.38f), w)
                "metalcore" in t -> blend(MetalVector(.62f,.72f,.86f,.24f,.61f,.70f,.56f,.63f), w)
                "female vocal" in t || "female-fronted" in t -> blend(MetalVector(.78f,.55f,.54f,.60f,.44f,.16f,.96f,.76f), w)
                "melodic" in t -> blend(MetalVector(.95f,.66f,.52f,.52f,.45f,.16f,.91f,.89f), w)
                "japanese" in t || "j-metal" in t -> blend(MetalVector(.85f,.74f,.60f,.56f,.56f,.18f,.89f,.83f), w)
            }
        }
        return v
    }
}
