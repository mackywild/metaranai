package jp.metaranai.app

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

class MusicBrainzClient {
    private var lastRequestAt = 0L

    fun searchArtist(name: String): MusicBrainzArtist? {
        throttle()
        val q = URLEncoder.encode("artist:\"$name\"", "UTF-8")
        val url = URL("https://musicbrainz.org/ws/2/artist/?query=$q&fmt=json&limit=3")
        val c = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 10_000; readTimeout = 10_000
            setRequestProperty("User-Agent", "Metaranai-Android/0.5.1 (music discovery prototype)")
            setRequestProperty("Accept", "application/json")
        }
        val code = c.responseCode
        val body = (if (code in 200..299) c.inputStream else c.errorStream).bufferedReader().use { it.readText() }
        if (code !in 200..299) return null
        val arr = JSONObject(body).optJSONArray("artists") ?: return null
        var best: JSONObject? = null
        var bestScore = -1
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val score = o.optInt("score", 0) + if (o.optString("name").equals(name, true)) 20 else 0
            if (score > bestScore) { best = o; bestScore = score }
        }
        val o = best ?: return null
        if (bestScore < 70) return null
        val life = o.optJSONObject("life-span")
        val area = o.optJSONObject("area")?.optString("name")?.takeIf { it.isNotBlank() }
        return MusicBrainzArtist(
            mbid = o.optString("id").takeIf { it.isNotBlank() },
            name = o.optString("name", name),
            country = o.optString("country").takeIf { it.isNotBlank() },
            area = area,
            beginDate = life?.optString("begin")?.takeIf { it.isNotBlank() },
            endDate = life?.optString("end")?.takeIf { it.isNotBlank() },
            ended = if (life?.has("ended") == true) life.optBoolean("ended") else null,
            matchScore = bestScore.coerceAtMost(100)
        )
    }

    private fun throttle() {
        val now = System.currentTimeMillis()
        val wait = 1100L - (now - lastRequestAt)
        if (wait > 0) Thread.sleep(wait)
        lastRequestAt = System.currentTimeMillis()
    }
}

data class MusicBrainzArtist(
    val mbid: String?, val name: String, val country: String?, val area: String?,
    val beginDate: String?, val endDate: String?, val ended: Boolean?, val matchScore: Int
)
