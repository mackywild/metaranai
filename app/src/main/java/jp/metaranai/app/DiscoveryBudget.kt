package jp.metaranai.app

/** Soft round deadline: do not start more metadata requests after this budget is spent. */
class DiscoveryBudget(private val durationMs: Long = 20_000,
                      private val nowMs: () -> Long = { System.nanoTime() / 1_000_000 }) {
    private val startedAt = nowMs()
    fun hasTime(): Boolean = nowMs() - startedAt < durationMs
}
