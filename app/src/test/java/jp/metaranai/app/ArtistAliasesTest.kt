package jp.metaranai.app

import org.junit.Assert.*
import org.junit.Test

class ArtistAliasesTest {
    private fun artist(name: String, country: String = "External") = MetalCatalog.artists.first().copy(
        name = name, country = country, genres = listOf("Heavy Metal"), aliases = emptyList(), canonicalName = null)

    @Test fun reportedJapaneseNamesResolveToOfficialEnglishNames() {
        assertEquals("Sabaton", ArtistAliases.canonicalName("サバトン"))
        assertEquals("Battle Beast", ArtistAliases.canonicalName("バトル・ビースト"))
        assertEquals("Battle Beast", ArtistAliases.canonicalName("ﾊﾞﾄﾙ･ﾋﾞｰｽﾄ"))
        assertEquals("Battle Beast", ArtistAliases.canonicalName("バトル ビースト"))
    }

    @Test fun unknownNamesAndPartialNamesAreNeverGuessed() {
        assertEquals("謎のバンド", ArtistAliases.canonicalName("謎のバンド"))
        assertEquals("サバ", ArtistAliases.canonicalName("サバ"))
        assertEquals("Battle", ArtistAliases.canonicalName("Battle"))
        assertFalse(SearchQueryMatcher.exactName(artist("Battle"), "バトル・ビースト"))
    }

    @Test fun localSearchMatchesJapaneseAndEnglishNamesWithAndSemantics() {
        val english = artist("Battle Beast", "Finland")
        assertTrue(SearchQueryMatcher.matches(english, "バトル・ビースト"))
        assertTrue(SearchQueryMatcher.matches(english, "ﾊﾞﾄﾙ･ﾋﾞｰｽﾄ Finland"))
        assertTrue(SearchQueryMatcher.exactName(english, "バトル ビースト"))
        assertFalse(SearchQueryMatcher.matches(english, "バトル・ビースト Japan"))
        assertTrue(SearchQueryMatcher.matches(artist("サバトン"), "Sabaton"))
    }

    @Test fun registeredAliasesWorkBeyondTheBundledTwoBands() {
        val band = artist("Official fixture").copy(aliases = listOf("公式別名"), canonicalName = "Official fixture")
        assertTrue(SearchQueryMatcher.exactName(band, "公式別名"))
        assertTrue(SearchQueryMatcher.matches(band, "公式別名 Heavy"))
        assertFalse(SearchQueryMatcher.exactName(band, "公式別"))
    }

    @Test fun lookupAddsKnownCountryWithoutChangingStoredHistoryNameOrKnownCountry() {
        val original = artist("サバトン")
        val lookup = ArtistAliases.forLookup(original)
        assertEquals("Sabaton", lookup.name)
        assertEquals("Sweden", lookup.country)
        assertEquals("サバトン", original.name)
        assertEquals("Sabaton", original.displayName)
        assertEquals("Japan", ArtistAliases.forLookup(original.copy(country = "Japan")).country)
    }

    @Test fun mergingSpellingAliasesPreservesStoredNameAndAllSearchSpellings() {
        val legacy = artist("サバトン")
        val fresh = artist("Sabaton", "SE").copy(metadataConfidence = 100, mbid = "fixture-id")
        val merged = ArtistAliases.deduplicate(listOf(legacy, fresh)).single()
        assertEquals("サバトン", merged.name)
        assertEquals("Sabaton", merged.displayName)
        assertEquals("fixture-id", merged.mbid)
        assertTrue(SearchQueryMatcher.matches(merged, "Sabaton"))
        assertTrue(SearchQueryMatcher.matches(merged, "サバトン"))
    }

    @Test fun differentMbidsAndConflictingCountriesNeverMergeAsAliases() {
        val first = artist("サバトン", "SE").copy(mbid = "one")
        val second = artist("Sabaton", "SE").copy(mbid = "two")
        assertEquals(2, ArtistAliases.deduplicate(listOf(first, second)).size)
        assertEquals(2, ArtistAliases.deduplicate(listOf(artist("サバトン", "Japan"), artist("Sabaton", "SE"))).size)
        assertEquals(2, ArtistAliases.deduplicate(listOf(artist("Identical", "Japan"), artist("Identical", "Finland"))).size)
    }

    @Test fun alreadyRatedAliasesDoNotReturnToTodaysUnratedPool() {
        val english = artist("Sabaton", "Sweden")
        val rated = ArtistAliases.expandRatedNames(listOf("サバトン"), listOf(english))
        assertTrue(RecommendationPool.unrated(listOf(english), OverseasPreference.YES, emptyList(), rated).isEmpty())
        assertTrue(rated.contains("サバトン"))
        assertTrue(rated.contains("sabaton"))
    }

    @Test fun metadataChangesInvalidateUnconfirmedAvailabilityAndSameNameKeysDiffer() {
        val old = artist("サバトン")
        assertNotEquals(ArtistAliases.availabilityKey(old), ArtistAliases.availabilityKey(old.copy(mbid = "id", country = "SE")))
        assertNotEquals(ArtistAliases.availabilityKey(old), ArtistAliases.availabilityKey(old.copy(aliases = listOf("New alias"))))
    }
}
