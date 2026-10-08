package jp.metaranai.app

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RecommendationPoolTest {
    private val japan = MetalCatalog.artists.filter { OverseasPreference.isJapanese(it) }
    private val foreign = MetalCatalog.artists.first { !OverseasPreference.isJapanese(it) }

    @Test fun exhaustedDomesticPoolTriggersRefillEvenWithoutGenreFixing() {
        val rated = japan.map { it.name }.toSet()
        assertTrue(RecommendationPool.needsRefill(MetalCatalog.artists, OverseasPreference.NO, emptyList(), rated))
        assertTrue(RecommendationPool.unrated(MetalCatalog.artists, OverseasPreference.NO, emptyList(), rated).isEmpty())
    }

    @Test fun largeForeignPoolCannotSatisfyDomesticRefillThreshold() {
        val archive = japan.take(1) + (1..100).map { foreign.copy(name = "Foreign $it") }
        assertTrue(RecommendationPool.needsRefill(archive, OverseasPreference.NO, emptyList(), emptySet()))
        assertFalse(RecommendationPool.needsRefill(archive, OverseasPreference.YES, emptyList(), emptySet()))
    }

    @Test fun ratingsRemoveEachCandidateAndNewDomesticDiscoveryRestoresThePool() {
        val remaining = japan.map { it.name }.toMutableSet()
        val rated = mutableSetOf<String>()
        val engine = RecommendationEngine()
        while (remaining.isNotEmpty()) {
            val candidates = RecommendationPool.unrated(MetalCatalog.artists, OverseasPreference.NO, emptyList(), rated)
            val picked = engine.recommend(japan.first().vector, emptyList(), candidates = candidates).artist.name
            assertTrue(remaining.remove(picked))
            rated += picked
        }
        assertTrue(RecommendationPool.unrated(MetalCatalog.artists, OverseasPreference.NO, emptyList(), rated).isEmpty())
        val discovered = japan.first().copy(name = "New domestic fixture")
        assertEquals(listOf(discovered), RecommendationPool.unrated(MetalCatalog.artists + discovered,
            OverseasPreference.NO, emptyList(), rated))
    }

    @Test fun initialCatalogUsesBothRegionAndInitialGenre() {
        assertEquals(MetalCatalog.artists, RecommendationPool.initial(OverseasPreference.YES))
        assertEquals(japan, RecommendationPool.initial(OverseasPreference.NO))
        val restricted = RecommendationPool.initial(OverseasPreference.NO, listOf("Symphonic Metal"))
        assertTrue(restricted.isNotEmpty())
        assertTrue(restricted.all { OverseasPreference.isJapanese(it) && GenreLensCatalog.matches(it, listOf("Symphonic Metal")) })
        assertTrue(RecommendationPool.initial(OverseasPreference.NO, listOf("Death Metal")).isEmpty())
    }

    @Test fun paginationContinuesPastForeignUnknownAndRatedNames() = runBlocking {
        val requested = mutableListOf<Int>()
        var cursor = 1
        val discovered = japan.first().copy(name = "New fixture")
        val pages = mapOf(1 to List(50) { foreign.copy(name = "Foreign $it") },
            2 to listOf(japan.first(), foreign.copy(country = "External")), 3 to listOf(discovered))
        val batch = RefillPageWalker.collect(startPage = cursor, target = 1,
            fetchPage = { page -> requested += page; pages[page].orEmpty() },
            accept = { artist -> artist.takeIf { OverseasPreference.NO.allows(it) && it.name != japan.first().name } },
            onPageCompleted = { cursor = it })
        assertEquals(listOf(1, 2, 3), requested)
        assertEquals(listOf(discovered), batch.artists)
        assertEquals(4, cursor)
        // A subsequent refill starts after those already visited pages, not at the same top-50 list.
        requested.clear()
        val next = RefillPageWalker.collect(startPage = cursor, target = 1,
            fetchPage = { page -> requested += page; emptyList<MetalArtist>() },
            accept = { it }, onPageCompleted = { cursor = it })
        assertEquals(listOf(4), requested)
        assertTrue(next.artists.isEmpty())
        assertEquals(1, cursor)
    }

    @Test fun unsuccessfulSearchIsBoundedAndStillAdvancesForRetry() = runBlocking {
        val requested = mutableListOf<Int>()
        var cursor = 7
        val batch = RefillPageWalker.collect(startPage = cursor, target = 20,
            fetchPage = { page -> requested += page; listOf(foreign) },
            accept = { null }, onPageCompleted = { cursor = it })
        assertEquals(listOf(7, 8, 9), requested)
        assertEquals(10, cursor)
        assertTrue(batch.artists.isEmpty())
    }

    @Test fun acceptedGoalStopsExtraNetworkRequestsAndDuplicatesDoNotCount() = runBlocking {
        val requested = mutableListOf<Int>()
        val batch = RefillPageWalker.collect(startPage = 1, target = 2,
            fetchPage = { page -> requested += page; if (page == 1) List(50) { japan[0] } else listOf(japan[1]) },
            accept = { it }, onPageCompleted = {})
        assertEquals(listOf(1, 2), requested)
        assertEquals(listOf(japan[0], japan[1]), batch.artists)
    }
}
