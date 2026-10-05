package jp.metaranai.app

import org.junit.Assert.*
import org.junit.Test

class DiscoverySearchResultsTest {
    private val profile = MetalVector(.9f, .9f, .6f, .6f, .5f, .1f, .9f, .9f)
    private fun artist(name: String, genre: String = "Power Metal", vector: MetalVector = profile) =
        MetalArtist(name, "Japan", listOf(genre), vector, .8f, "test")

    @Test fun currentRunResultsDoNotIncludeUnrelatedCacheEntries() {
        val result = DiscoverySearchResults.choose(listOf(artist("New")), listOf(artist("Old")), profile, emptyList(), emptySet())
        assertEquals(listOf("New"), result.artists.map { it.name })
        assertFalse(result.usedSavedCandidates)
    }

    @Test fun zeroExternalResultsStillShowsAtLeastOneEligibleSavedArtist() {
        val result = DiscoverySearchResults.choose(emptyList(), listOf(artist("Unheard")), profile, listOf("Power Metal"), emptySet())
        assertEquals(listOf("Unheard"), result.artists.map { it.name })
        assertTrue(result.usedSavedCandidates)
    }

    @Test fun fallbackKeepsGenreConstraintAndExcludesRatedNamesIgnoringCaseAndSpaces() {
        val saved = listOf(artist("Rated"), artist("Wrong genre", "Death Metal"), artist("Eligible"))
        val result = DiscoverySearchResults.choose(emptyList(), saved, profile, listOf("Power Metal"), setOf(" RATED "))
        assertEquals(listOf("Eligible"), result.artists.map { it.name })
    }

    @Test fun unusableCurrentResultsTriggerSavedFallback() {
        val result = DiscoverySearchResults.choose(listOf(artist("Rated")), listOf(artist("Eligible")), profile, emptyList(), setOf("rated"))
        assertTrue(result.usedSavedCandidates)
        assertEquals("Eligible", result.artists.single().name)
    }

    @Test fun emptyEligiblePoolDoesNotInventArtistsOrReturnRatedOnes() {
        val result = DiscoverySearchResults.choose(emptyList(), listOf(artist("Rated")), profile, emptyList(), setOf("rated"))
        assertTrue(result.artists.isEmpty())
    }

    @Test fun resultsAreDeduplicatedAndRankedByTaste() {
        val far = MetalVector(.1f, .1f, .1f, .1f, .1f, .9f, .1f, .1f)
        val result = DiscoverySearchResults.select(listOf(artist("Far", vector = far), artist("Near"), artist(" near ")), profile, emptyList(), emptySet())
        assertEquals(listOf("Near", "Far"), result.map { it.name })
    }
}
