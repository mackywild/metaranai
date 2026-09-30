package jp.metaranai.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchQueryMatcherTest {
    private val skywings = MetalArtist(
        name = "SKYWINGS",
        country = "Japan",
        genres = listOf("Melodic Power Metal", "Symphonic Metal"),
        vector = MetalVector(.8f, .8f, .4f, .8f, .4f, .2f, .8f, .7f),
        discovery = .5f
    )

    private val skylines = MetalArtist(
        name = "Skylines",
        country = "External",
        genres = listOf("Metalcore", "Math Metal"),
        vector = MetalVector(.4f, .6f, .7f, .3f, .7f, .6f, .3f, .4f),
        discovery = .7f
    )

    @Test
    fun exactBandQueryDoesNotAcceptFuzzyNeighbor() {
        assertTrue(SearchQueryMatcher.matches(skywings, "SKYWINGS"))
        assertFalse(SearchQueryMatcher.matches(skylines, "SKYWINGS"))
    }

    @Test
    fun halfWidthSpacesAreAndAcrossBandCountryAndGenre() {
        assertTrue(SearchQueryMatcher.matches(skywings, "Japan Power"))
        assertTrue(SearchQueryMatcher.matches(skywings, "SKYWINGS Japan Symphonic"))
        assertFalse(SearchQueryMatcher.matches(skywings, "Japan Death"))
    }

    @Test
    fun spacingAndCaseAreNormalized() {
        assertTrue(SearchQueryMatcher.matches(skywings, "  japan   power   METAL  "))
        assertTrue(SearchQueryMatcher.exactName(skywings, "sky wings"))
    }
}
