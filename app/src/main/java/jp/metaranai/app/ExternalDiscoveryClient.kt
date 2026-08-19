package jp.metaranai.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import kotlin.math.roundToInt

/** V0.4: Last.fm discovery + popularity metadata + MusicBrainz normalization. */
class ExternalDiscoveryClient(private val store: LocalStore) {
    private val musicBrainz = MusicBrainzClient()

    suspend fun discover(seeds: List<String>, limitPerSeed: Int = 18): Result<ExternalDiscoveryResult> = withContext(Dispatchers.IO) {
        runCatching {
            val key = store.lastFmApiKey()
            require(key.isNotBlank()) { "Last.fm API Keyを設定してください" }
            require(seeds.isNotEmpty()) { "発掘Seedがありません" }

            val merged = linkedMapOf<String, Candidate>()
            seeds.distinctBy { it.lowercase() }.take(5).forEach { seed ->
                getSimilar(key, seed, limitPerSeed).forEach { c ->
                    val id = c.name.lowercase()
                    val existing = merged[id]
                    if (existing == null || c.match > existing.match) merged[id] = c.copy(seed = seed)
                }
            }

            val known = MetalCatalog.artists.map { it.name.lowercase() }.toSet()
            val already = store.loadExternalArtists().map { it.name.lowercase() }.toSet()
            val shortlist = merged.values
                .filterNot { it.name.lowercase() in known }
                .filterNot { it.name.lowercase() in seeds.map { it.lowercase() } }
                .sortedByDescending { it.match }
                .take(36)

            val accepted = mutableListOf<MetalArtist>()
            shortlist.forEachIndexed { index, c ->
                val tags = getTopTags(key, c.name)
                val metalTags = tags.filter { DiscoveryTagMapper.isMetalTag(it) }
                if (metalTags.isEmpty()) return@forEachIndexed

                val info = getArtistInfo(key, c.name)
                // MusicBrainz enrichement is deliberately capped; public API asks clients to stay <= 1 req/sec.
                val mb = if (index < 12) runCatching { musicBrainz.searchArtist(c.name) }.getOrNull() else null
                val vector = DiscoveryTagMapper.vectorFromTags(tags)
                val discovery = (.72f + (1f - c.match.coerceIn(0f, 1f)) * .22f + if (c.name.lowercase() !in already) .06f else 0f)
                    .coerceIn(.55f, .99f)
                val confidence = when {
                    mb?.mbid != null && info.mbid != null && mb.mbid.equals(info.mbid, true) -> 100
                    mb?.mbid != null -> 82
                    info.mbid != null -> 68
                    else -> 45
                }
                val hidden = HiddenScoreEngine.score(info.listeners, info.playcount, discovery, confidence)
                val location = mb?.area ?: mb?.country ?: "External"
                accepted += MetalArtist(
                    name = mb?.name ?: c.name,
                    country = location,
                    genres = metalTags.take(4).map(DiscoveryTagMapper::displayTag),
                    vector = vector,
                    discovery = discovery,
                    reason = buildString {
                        append("Last.fmで${c.seed}から発掘")
                        if (info.listeners != null) append("。listeners ${formatCount(info.listeners)}")
                        if (mb?.beginDate != null) append("。活動開始 ${mb.beginDate}")
                    },
                    source = ArtistSource.LASTFM_MUSICBRAINZ,
                    sourceSeed = c.seed,
                    externalScore = (c.match * 100).roundToInt(),
                    lastFmListeners = info.listeners,
                    lastFmPlaycount = info.playcount,
                    mbid = mb?.mbid ?: info.mbid,
                    area = mb?.area,
                    beginDate = mb?.beginDate,
                    endDate = mb?.endDate,
                    ended = mb?.ended,
                    hiddenScore = hidden,
                    metadataConfidence = confidence
                )
            }

            val combined = (store.loadExternalArtists() + accepted)
                .groupBy { it.name.lowercase() }
                .map { (_, xs) -> xs.maxByOrNull { it.metadataConfidence + it.hiddenScore }!! }
                .sortedWith(compareByDescending<MetalArtist> { it.hiddenScore }.thenByDescending { it.discovery })
                .take(500)
            store.saveExternalArtists(combined)
            ExternalDiscoveryResult(merged.size, accepted.size, combined.size, seeds.take(5), combined)
        }
    }

    private fun getSimilar(apiKey: String, artist: String, limit: Int): List<Candidate> {
        val json = lastFmGet(mapOf("method" to "artist.getSimilar", "artist" to artist, "limit" to limit.toString(), "autocorrect" to "1", "api_key" to apiKey, "format" to "json"))
        val arr = json.optJSONObject("similarartists")?.optJSONArray("artist") ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val name = o.optString("name").trim()
                if (name.isNotBlank()) add(Candidate(name, o.optString("match").toFloatOrNull() ?: 0f, artist))
            }
        }
    }

    private fun getTopTags(apiKey: String, artist: String): List<String> {
        val json = lastFmGet(mapOf("method" to "artist.getTopTags", "artist" to artist, "autocorrect" to "1", "api_key" to apiKey, "format" to "json"))
        val arr = json.optJSONObject("toptags")?.optJSONArray("tag") ?: return emptyList()
        return buildList {
            for (i in 0 until minOf(arr.length(), 15)) {
                val name = arr.optJSONObject(i)?.optString("name")?.trim()?.lowercase().orEmpty()
                if (name.isNotBlank()) add(name)
            }
        }
    }

    private fun getArtistInfo(apiKey: String, artist: String): LastFmArtistInfo {
        val json = lastFmGet(mapOf("method" to "artist.getInfo", "artist" to artist, "autocorrect" to "1", "api_key" to apiKey, "format" to "json"))
        val a = json.optJSONObject("artist") ?: return LastFmArtistInfo(null, null, null)
        val stats = a.optJSONObject("stats")
        return LastFmArtistInfo(
            listeners = stats?.optString("listeners")?.toLongOrNull(),
            playcount = stats?.optString("playcount")?.toLongOrNull(),
            mbid = a.optString("mbid").takeIf { it.isNotBlank() }
        )
    }

    private fun lastFmGet(params: Map<String, String>): JSONObject {
        val query = params.entries.joinToString("&") { (k, v) -> "${URLEncoder.encode(k, "UTF-8") }=${URLEncoder.encode(v, "UTF-8")}" }
        val connection = (URL("https://ws.audioscrobbler.com/2.0/?$query").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 10_000; readTimeout = 10_000
            setRequestProperty("User-Agent", "Metaranai-Android/0.4")
        }
        val code = connection.responseCode
        val body = (if (code in 200..299) connection.inputStream else connection.errorStream).bufferedReader().use { it.readText() }
        if (code !in 200..299) error("Last.fm HTTP $code: ${body.take(180)}")
        val json = JSONObject(body)
        if (json.has("error")) error("Last.fm error ${json.optInt("error")}: ${json.optString("message")}")
        return json
    }

    private fun formatCount(n: Long): String = when {
        n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
        n >= 1_000 -> String.format("%.1fK", n / 1_000.0)
        else -> n.toString()
    }

    private data class Candidate(val name: String, val match: Float, val seed: String)
    private data class LastFmArtistInfo(val listeners: Long?, val playcount: Long?, val mbid: String?)
}

data class ExternalDiscoveryResult(val fetched: Int, val accepted: Int, val cached: Int, val seeds: List<String>, val artists: List<MetalArtist>)
