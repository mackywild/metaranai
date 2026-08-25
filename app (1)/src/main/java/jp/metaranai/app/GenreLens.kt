package jp.metaranai.app

import java.time.DayOfWeek
import java.time.LocalDate

object GenreLensCatalog {
    data class Lens(val name: String, val vector: MetalVector, val aliases: List<String>)

    val lenses = listOf(
        Lens("Melodic Metal", MetalVector(.95f,.66f,.56f,.52f,.48f,.18f,.90f,.88f), listOf("melodic metal", "melodic")),
        Lens("Power Metal", MetalVector(.92f,.88f,.58f,.62f,.58f,.08f,.95f,.90f), listOf("power metal")),
        Lens("Symphonic Metal", MetalVector(.88f,.64f,.60f,.96f,.58f,.18f,.88f,.82f), listOf("symphonic metal", "symphonic")),
        Lens("Gothic Metal", MetalVector(.73f,.44f,.70f,.78f,.44f,.40f,.75f,.60f), listOf("gothic metal", "gothic")),
        Lens("Metalcore", MetalVector(.64f,.73f,.86f,.25f,.62f,.70f,.55f,.64f), listOf("metalcore")),
        Lens("Melodic Death Metal", MetalVector(.83f,.77f,.83f,.46f,.66f,.80f,.40f,.67f), listOf("melodic death metal", "melodeath")),
        Lens("Progressive Metal", MetalVector(.72f,.58f,.72f,.43f,.94f,.34f,.75f,.53f), listOf("progressive metal", "prog metal")),
        Lens("Folk Metal", MetalVector(.82f,.68f,.67f,.55f,.48f,.35f,.73f,.77f), listOf("folk metal")),
        Lens("Doom Metal", MetalVector(.62f,.22f,.91f,.35f,.40f,.48f,.58f,.34f), listOf("doom metal", "doom")),
        Lens("Thrash Metal", MetalVector(.48f,.86f,.88f,.15f,.68f,.46f,.55f,.45f), listOf("thrash metal", "thrash")),
        Lens("Black Metal", MetalVector(.55f,.72f,.86f,.48f,.52f,.90f,.22f,.38f), listOf("black metal")),
        Lens("Death Metal", MetalVector(.48f,.72f,.92f,.22f,.62f,.94f,.12f,.33f), listOf("death metal")),
        Lens("Neoclassical Metal", MetalVector(.93f,.82f,.58f,.70f,.90f,.08f,.91f,.84f), listOf("neoclassical metal", "neoclassical")),
        Lens("Heavy Metal", MetalVector(.66f,.60f,.78f,.25f,.58f,.28f,.78f,.62f), listOf("heavy metal")),
        Lens("Alternative Metal", MetalVector(.65f,.52f,.78f,.28f,.60f,.38f,.72f,.70f), listOf("alternative metal", "alt metal"))
    )

    fun names(): List<String> = lenses.map { it.name }

    fun activeGenres(config: GenreLensConfig, date: LocalDate = LocalDate.now()): List<String> = when (config.mode) {
        GenreLensMode.OFF -> emptyList()
        GenreLensMode.MANUAL -> config.manualGenres.toList().sorted()
        GenreLensMode.WEEKDAY -> config.weekdayGenres[date.dayOfWeek.name].orEmpty().toList().sorted()
    }

    fun vectorFor(names: Collection<String>): MetalVector? {
        val selected = lenses.filter { it.name in names }
        if (selected.isEmpty()) return null
        fun avg(f: (MetalVector) -> Float) = selected.map { f(it.vector) }.average().toFloat()
        return MetalVector(avg { it.melody }, avg { it.speed }, avg { it.heavy }, avg { it.symphonic }, avg { it.technical }, avg { it.growl }, avg { it.cleanVocal }, avg { it.catchy })
    }

    fun score(artist: MetalArtist, names: Collection<String>): Float {
        if (names.isEmpty()) return 0f
        val selected = lenses.filter { it.name in names }
        if (selected.isEmpty()) return 0f
        val tagText = artist.genres.joinToString(" ").lowercase()
        return selected.maxOf { lens ->
            val vectorScore = artist.vector.similarity(lens.vector)
            val tagBonus = if (lens.aliases.any { tagText.contains(it) }) .12f else 0f
            (vectorScore * .88f + tagBonus).coerceIn(0f, 1f)
        }
    }

    fun dayLabel(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "月"
        DayOfWeek.TUESDAY -> "火"
        DayOfWeek.WEDNESDAY -> "水"
        DayOfWeek.THURSDAY -> "木"
        DayOfWeek.FRIDAY -> "金"
        DayOfWeek.SATURDAY -> "土"
        DayOfWeek.SUNDAY -> "日"
    }
}
