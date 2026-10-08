package jp.metaranai.app

import org.junit.Assert.*
import org.junit.Test

class OverseasPreferenceTest {
    private val domestic = MetalCatalog.artists.first()
    private val foreign = domestic.copy(name = "Overseas fixture", country = "Sweden", area = null)
    private val unknown = foreign.copy(name = "Unknown fixture", country = "External")

    @Test fun oldAndInvalidSettingsKeepPreviousBehavior() {
        assertEquals(OverseasPreference.YES, OverseasPreference.fromStored(null))
        assertEquals(OverseasPreference.YES, OverseasPreference.fromStored("invalid"))
        OverseasPreference.entries.forEach { assertEquals(it, OverseasPreference.fromStored(it.name)) }
        assertEquals(listOf(foreign, unknown, domestic), OverseasPreference.YES.apply(listOf(foreign, unknown, domestic)))
    }

    @Test fun domesticOnlyRequiresOriginEvidenceRatherThanGenreOrName() {
        for (country in listOf("Japan", "JP", "日本", "Tokyo, Japan")) {
            assertTrue(OverseasPreference.NO.allows(domestic.copy(country = country)))
        }
        assertTrue(OverseasPreference.NO.allows(unknown.copy(area = "Japan")))
        assertFalse(OverseasPreference.NO.allows(unknown.copy(genres = listOf("Japanese Metal"))))
        assertFalse(OverseasPreference.NO.allows(unknown.copy(country = "NotJapan")))
        assertEquals(listOf(domestic), OverseasPreference.NO.apply(listOf(foreign, unknown, domestic)))
    }

    @Test fun sometimesKeepsForeignCandidatesWithReducedWeight() {
        assertTrue(OverseasPreference.SOMETIMES.allows(foreign))
        assertEquals(.85f, OverseasPreference.SOMETIMES.weight(foreign), .0001f)
        assertEquals(1f, OverseasPreference.SOMETIMES.weight(domestic), .0001f)
        assertEquals(listOf(domestic, foreign), OverseasPreference.SOMETIMES.apply(listOf(foreign, domestic)))
    }

    @Test fun foreignOnlyFreshDiscoveryFallsBackToSavedDomesticCandidates() {
        val selection = DiscoverySearchResults.choose(listOf(foreign), listOf(domestic, unknown),
            domestic.vector, emptyList(), emptySet(), OverseasPreference.NO)
        assertTrue(selection.usedSavedCandidates)
        assertEquals(listOf(domestic), selection.artists)
        assertTrue(DiscoverySearchResults.select(listOf(foreign, unknown), domestic.vector,
            emptyList(), emptySet(), overseasPreference = OverseasPreference.NO).isEmpty())
    }

    @Test fun domesticPreferenceDoesNotOverrideGenreOrRatedExclusions() {
        assertTrue(DiscoverySearchResults.select(listOf(domestic), domestic.vector,
            listOf("Death Metal"), emptySet(), overseasPreference = OverseasPreference.NO).isEmpty())
        assertTrue(DiscoverySearchResults.select(listOf(domestic), domestic.vector,
            emptyList(), setOf(domestic.name), overseasPreference = OverseasPreference.NO).isEmpty())
    }

    @Test fun sometimesLowersSearchRankWhileYesRetainsRelevance() {
        assertEquals(listOf(domestic, foreign), DiscoverySearchResults.select(listOf(foreign, domestic),
            domestic.vector, emptyList(), emptySet(), overseasPreference = OverseasPreference.SOMETIMES))
        assertEquals(listOf(foreign, domestic), DiscoverySearchResults.select(listOf(foreign, domestic),
            domestic.vector, emptyList(), emptySet()))
    }

    @Test fun recommendationNeverLeaksForeignOrUnknownOrigins() {
        val engine = RecommendationEngine()
        for (seed in 0L until 100L) {
            assertEquals(domestic.name, engine.recommend(domestic.vector, emptyList(),
                candidates = listOf(foreign, unknown, domestic), seed = seed,
                overseasPreference = OverseasPreference.NO).artist.name)
        }
    }

    @Test fun sometimesLowersForeignRecommendationFrequencyWithoutExcludingIt() {
        val engine = RecommendationEngine()
        fun count(preference: OverseasPreference) = (0L until 2000L).count { seed ->
            engine.recommend(domestic.vector, emptyList(), candidates = listOf(domestic, foreign),
                seed = seed, overseasPreference = preference).artist.name == foreign.name
        }
        val sometimes = count(OverseasPreference.SOMETIMES)
        assertTrue(sometimes > 0)
        assertTrue(sometimes < count(OverseasPreference.YES))
    }

    @Test fun allQuizAnswersRespectEveryPreferenceAndKeepThreeBands() {
        for (mask in 0 until 64) {
            val answers = (0 until 6).map { (mask shr it) and 1 }
            val original = MetalTasteQuiz.result(answers)
            OverseasPreference.entries.forEach { preference ->
                val result = MetalTasteQuiz.result(answers, preference)
                assertEquals(3, result.artists.size)
                assertEquals(3, result.artists.map { it.name }.distinct().size)
                assertEquals(original.profile, result.profile)
                assertTrue(result.artists.all { preference.allows(it) && GenreLensCatalog.matches(it, listOf(result.genre)) })
                if (preference == OverseasPreference.NO) assertEquals("Japanese Metal", result.genre)
            }
        }
    }
}
