package jp.metaranai.app

import kotlin.math.ln

/** V0.4: popularity is not taste. This score only measures how hidden a candidate appears. */
object HiddenScoreEngine {
    fun score(listeners: Long?, playcount: Long?, discovery: Float, metadataConfidence: Int): Int {
        val rarity = when {
            listeners == null -> 42f
            listeners <= 0 -> 55f
            else -> {
                // ~100 listeners => near 100, ~10M+ => near 0 (log scale)
                val log = ln(listeners.toDouble() + 1.0) / ln(10.0)
                (100.0 - ((log - 2.0) / 5.0 * 100.0)).coerceIn(0.0, 100.0).toFloat()
            }
        }
        val devotion = if (listeners != null && listeners > 0 && playcount != null) {
            val playsPerListener = playcount.toDouble() / listeners.toDouble()
            ((playsPerListener - 2.0) / 28.0 * 100.0).toFloat().coerceIn(0f, 100f)
        } else 35f
        val discoveryPart = discovery.coerceIn(0f, 1f) * 100f
        val confidence = metadataConfidence.coerceIn(0, 100).toFloat()
        return (rarity * .58f + devotion * .17f + discoveryPart * .15f + confidence * .10f)
            .toInt().coerceIn(0, 100)
    }
}
