package jp.metaranai.app

import org.junit.Assert.*
import org.junit.Test

class MetalTasteQuizTest {
    @Test fun everyAnswerCombinationProducesThreeDistinctMatchingArtists() {
        val genres = mutableSetOf<String>()
        for (mask in 0 until 64) {
            val result = MetalTasteQuiz.result((0 until 6).map { (mask shr it) and 1 })
            genres += result.genre
            assertEquals(3, result.artists.size)
            assertEquals(3, result.artists.map { it.name.lowercase() }.distinct().size)
            assertTrue(result.artists.all { GenreLensCatalog.matches(it, listOf(result.genre)) })
            assertTrue(result.profile.traits().all { it.second in 0f..1f })
        }
        assertTrue("Results must cover different sounds, not just melody/power", genres.size >= 8)
    }
    @Test fun clearMelodicOrchestralAnswersLeadToSymphonicMetal() {
        val result = MetalTasteQuiz.result(listOf(0, 0, 0, 0, 1, 0))
        assertEquals("Symphonic Metal", result.genre)
        assertEquals("SMCO", result.code)
        assertEquals(listOf("Nightwish", "Epica", "Within Temptation"), result.artists.map { it.name })
    }
    @Test fun heavyHarshAnswersAreNotForcedIntoPowerMetal() {
        assertEquals("Death Metal", MetalTasteQuiz.result(listOf(0, 1, 1, 1, 1, 1)).genre)
    }
    @Test fun complexityCanLeadToProgressiveMetal() {
        assertEquals("Progressive Metal", MetalTasteQuiz.result(listOf(0, 0, 0, 1, 0, 1)).genre)
    }
    @Test fun everySupportedStarterGenreHasThreeCandidates() {
        StarterBandCatalog.genres.forEach { genre ->
            val artists = StarterBandCatalog.forGenre(genre)
            assertEquals(3, artists.size)
            assertTrue(artists.all { GenreLensCatalog.matches(it, listOf(genre)) })
        }
    }
    @Test fun incompleteOrInvalidAnswersCannotProduceAResult() {
        assertThrows(IllegalArgumentException::class.java) { MetalTasteQuiz.result(listOf(0, 1)) }
        assertThrows(IllegalArgumentException::class.java) { MetalTasteQuiz.result(listOf(0, 1, -1, 0, 0, 0)) }
    }
}
