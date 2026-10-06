package jp.metaranai.app

import org.junit.Assert.*
import org.junit.Test

class SpotifyTopPeriodTest {
    @Test fun periodsMatchSpotifyApiValues() {
        assertEquals("short_term", SpotifyTopPeriod.MONTH.apiValue)
        assertEquals("medium_term", SpotifyTopPeriod.HALF_YEAR.apiValue)
        assertEquals("long_term", SpotifyTopPeriod.YEAR.apiValue)
    }

    @Test fun legacyOrUnknownPreferenceDefaultsToSixMonths() {
        assertEquals(SpotifyTopPeriod.HALF_YEAR, SpotifyTopPeriod.fromApiValue(null))
        assertEquals(SpotifyTopPeriod.HALF_YEAR, SpotifyTopPeriod.fromApiValue("unsupported"))
    }

    @Test fun everySavedPeriodRoundTrips() {
        SpotifyTopPeriod.entries.forEach { assertEquals(it, SpotifyTopPeriod.fromApiValue(it.apiValue)) }
    }
}
