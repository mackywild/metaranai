package jp.metaranai.app

/** Keep prior appraisals for history; repeated identical taps must not learn twice. */
object ArtistRatingHistory {
    fun record(history: List<DiscoveryRecord>, next: DiscoveryRecord): List<DiscoveryRecord> {
        val previous = history.firstOrNull { it.artistName.trim().equals(next.artistName.trim(), true) }
        if (previous?.reaction == next.reaction) return history
        return listOf(next) + history
    }
}
