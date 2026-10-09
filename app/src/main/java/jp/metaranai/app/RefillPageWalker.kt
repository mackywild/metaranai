package jp.metaranai.app

/** Bounded pagination driven by accepted candidates, not unverified API name counts. */
object RefillPageWalker {
    data class Batch(val artists: List<MetalArtist>, val fetched: Int)

    suspend fun <T> collect(startPage: Int, target: Int, maxPages: Int = 3,
                            fetchPage: suspend (Int) -> List<T>,
                            accept: suspend (T) -> MetalArtist?,
                            onPageCompleted: (Int) -> Unit,
                            shouldContinue: () -> Boolean = { true },
                            onAccepted: suspend (List<MetalArtist>) -> Unit = {}): Batch {
        require(startPage >= 1 && target > 0 && maxPages > 0)
        val accepted = mutableListOf<MetalArtist>()
        var fetched = 0
        for (offset in 0 until maxPages) {
            if (!shouldContinue()) break
            val page = startPage + offset
            val raw = fetchPage(page)
            fetched += raw.size
            if (raw.isEmpty()) {
                onPageCompleted(1) // End of remote data: allow a future pass to see updated entries.
                break
            }
            for (item in raw) {
                if (!shouldContinue()) {
                    onPageCompleted(page) // Resume this unfinished page, not the first page.
                    return Batch(accepted, fetched)
                }
                val artist = accept(item) ?: continue
                if (accepted.none { it.name.equals(artist.name, true) }) {
                    accepted += artist
                    onAccepted(accepted.toList())
                }
                if (accepted.size >= target) break
            }
            onPageCompleted(page + 1)
            if (accepted.size >= target) break
        }
        return Batch(accepted, fetched)
    }
}
