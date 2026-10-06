package jp.metaranai.app

import org.junit.Assert.*
import org.junit.Test

class ArtistRatingHistoryTest {
    private fun record(name: String = "Artist", reaction: Reaction = Reaction.HIT, date: String = "2026-10-06T10:00:00") =
        DiscoveryRecord(name, date, reaction, 80)

    @Test fun firstRatingIsSavedForTheSelectedArtist() {
        val result = ArtistRatingHistory.record(emptyList(), record("Selected"))
        assertEquals("Selected", result.single().artistName)
    }

    @Test fun sameDayReappraisalBecomesLatestAndKeepsPreviousHistory() {
        val old = record()
        val next = record(reaction = Reaction.LOVE_ALL, date = "2026-10-06T10:05:00")
        val result = ArtistRatingHistory.record(listOf(old), next)
        assertEquals(listOf(next, old), result)
    }

    @Test fun sameReactionDoesNotDuplicateHistoryOrRetriggerLearning() {
        val history = listOf(record())
        val result = ArtistRatingHistory.record(history, record(" artist "))
        assertSame(history, result)
    }

    @Test fun anotherArtistWithTheSameRatingCanStillBeRated() {
        val result = ArtistRatingHistory.record(listOf(record("Existing")), record("New"))
        assertEquals(listOf("New", "Existing"), result.map { it.artistName })
    }

    @Test fun latestAppraisalControlsIdempotenceNotAnOlderRating() {
        val history = listOf(record(reaction = Reaction.MEH), record(reaction = Reaction.HIT))
        val result = ArtistRatingHistory.record(history, record(reaction = Reaction.HIT))
        assertEquals(3, result.size)
        assertEquals(Reaction.HIT, result.first().reaction)
    }
}
