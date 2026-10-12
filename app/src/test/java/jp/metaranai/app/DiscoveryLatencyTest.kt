package jp.metaranai.app

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DiscoveryLatencyTest {
    private val artist = MetalCatalog.artists.first()

    @Test fun firstVerifiedCandidateIsPublishedBeforeTheNextPageIsRequested() = runBlocking {
        val events = mutableListOf<String>()
        val batch = RefillPageWalker.collect(startPage = 1, target = 2,
            fetchPage = { page -> events += "fetch:$page"; listOf(artist.copy(name = "Band $page")) },
            accept = { it }, onPageCompleted = {},
            onAccepted = { partial -> events += "publish:${partial.size}" })
        assertEquals(listOf("fetch:1", "publish:1", "fetch:2", "publish:2"), events)
        assertEquals(2, batch.artists.size)
    }

    @Test fun budgetStopsNewRequestsAndKeepsPartialCandidatesAndUnfinishedPage() = runBlocking {
        var clock = 0L
        val budget = DiscoveryBudget(durationMs = 20, nowMs = { clock })
        var cursor = 6
        val requested = mutableListOf<Int>()
        val published = mutableListOf<MetalArtist>()
        val batch = RefillPageWalker.collect(startPage = cursor, target = 20,
            fetchPage = { page -> requested += page; (1..50).map { artist.copy(name = "Band $it") } },
            accept = { candidate -> clock += 10; candidate },
            onPageCompleted = { cursor = it }, shouldContinue = budget::hasTime,
            onAccepted = { published.clear(); published.addAll(it) })
        assertEquals(listOf(6), requested)
        assertEquals(6, cursor)
        assertEquals(2, batch.artists.size)
        assertEquals(batch.artists, published)
    }

    @Test fun expiredRoundDoesNotStartNetworkRequestsOrResetPagePosition() = runBlocking {
        var clock = 100L
        val budget = DiscoveryBudget(durationMs = 20, nowMs = { clock })
        clock += 21
        var requested = false
        var cursor = 8
        val batch = RefillPageWalker.collect<MetalArtist>(startPage = cursor, target = 20,
            fetchPage = { requested = true; emptyList() }, accept = { it },
            onPageCompleted = { cursor = it }, shouldContinue = budget::hasTime)
        assertFalse(requested)
        assertEquals(8, cursor)
        assertTrue(batch.artists.isEmpty())
    }

    @Test fun progressDoesNotPublishDuplicateArtists() = runBlocking {
        val sizes = mutableListOf<Int>()
        RefillPageWalker.collect(startPage = 1, target = 2,
            fetchPage = { listOf(artist, artist.copy(name = artist.name.uppercase())) },
            accept = { it }, onPageCompleted = {}, onAccepted = { sizes += it.size })
        assertEquals(listOf(1), sizes)
    }
}
