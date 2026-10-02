package jp.metaranai.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

/**
 * V0.11 optional Last.fm taste profile.
 *
 * This does NOT authenticate as the Last.fm user. Public user.getTopArtists /
 * user.getRecentTracks data is read with the app's API key and a username.
 * Existing Deep Dive stays independent and continues to use ExternalDiscoveryClient.
 */
class LastFmProfileClient(private val store: LocalStore) {

    suspend fun sync(
        username: String,
        knownArtists: List<MetalArtist>
    ): Result<LastFmProfileSyncResult> = withContext(Dispatchers.IO) {
        runCatching {
            val user = username.trim()
            require(user.isNotBlank()) { "Last.fmユーザー名を入力してください" }
            val apiKey = store.lastFmApiKey().trim()
            require(apiKey.isNotBlank()) { "Last.fm APIが未設定です" }

            val scores = linkedMapOf<String, MutableSignal>()
            val periods = listOf(
                "overall" to .35f,
                "12month" to .28f,
                "6month" to .22f,
                "7day" to .15f
            )
            periods.forEach { (period, weight) ->
                val artists = getTopArtists(apiKey, user, period, 50)
                val denominator = artists.size.coerceAtLeast(1).toFloat()
                artists.forEachIndexed { index, row ->
                    val normalized = normalize(row.name)
                    if (normalized.isBlank()) return@forEachIndexed
                    val rankScore = ((artists.size - index) / denominator).coerceIn(.02f, 1f)
                    val signal = scores.getOrPut(normalized) { MutableSignal(row.name) }
                    signal.score += rankScore * weight
                    signal.playcount = maxOf(signal.playcount, row.playcount)
                }
            }

            val recent = getRecentArtists(apiKey, user, 200)
            val recentCounts = recent.groupingBy(::normalize).eachCount().filterKeys { it.isNotBlank() }
            val recentMax = recentCounts.values.maxOrNull()?.coerceAtLeast(1) ?: 1
            recentCounts.forEach { (normalized, count) ->
                val display = recent.firstOrNull { normalize(it) == normalized } ?: normalized
                val signal = scores.getOrPut(normalized) { MutableSignal(display) }
                signal.score += (count.toFloat() / recentMax) * .30f
                signal.recentCount = count
            }

            val knownByName = knownArtists.associateBy { normalize(it.name) }
            val accepted = mutableListOf<LastFmArtistSignal>()
            val candidates = scores.values.sortedByDescending { it.score }.take(20)

            for (signal in candidates) {
                if (accepted.size >= 10) break
                val normalized = normalize(signal.name)
                val known = knownByName[normalized]
                if (known != null) {
                    accepted += LastFmArtistSignal(
                        name = known.name,
                        score = signal.score,
                        playcount = signal.playcount,
                        recentCount = signal.recentCount,
                        tags = known.genres,
                        vector = known.vector
                    )
                    continue
                }

                val tags = runCatching { getTopTags(apiKey, signal.name, 12) }.getOrDefault(emptyList())
                if (tags.none(DiscoveryTagMapper::isMetalTag)) continue
                accepted += LastFmArtistSignal(
                    name = signal.name,
                    score = signal.score,
                    playcount = signal.playcount,
                    recentCount = signal.recentCount,
                    tags = tags,
                    vector = DiscoveryTagMapper.vectorFromTags(tags)
                )
            }

            require(accepted.isNotEmpty()) {
                "公開履歴からMetal系アーティストを判定できませんでした。Scrobbleが増えてから再同期するか、Spotify/ジャンルから開始してください"
            }

            val inferred = weightedAverage(accepted.map { it.vector to it.score.coerceAtLeast(.01f) })
            val seeds = accepted.sortedByDescending { it.score }.map { it.name }.distinct().take(8)
            val matched = accepted.mapNotNull { signal ->
                knownByName[normalize(signal.name)]?.name
            }.distinct()

            LastFmProfileSyncResult(
                username = user,
                seedArtists = seeds,
                inferredProfile = inferred,
                matchedArtists = matched,
                recentTrackCount = recent.size,
                summary = "Last.fm @$user / Metal Seed ${seeds.size}組 / Recent ${recent.size}曲"
            )
        }
    }

    private fun getTopArtists(
        apiKey: String,
        username: String,
        period: String,
        limit: Int
    ): List<TopArtistRow> {
        val json = lastFmGet(
            mapOf(
                "method" to "user.getTopArtists",
                "user" to username,
                "period" to period,
                "limit" to limit.toString(),
                "page" to "1",
                "api_key" to apiKey,
                "format" to "json"
            )
        )
        val arr = json.optJSONObject("topartists")?.optJSONArray("artist") ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val row = arr.optJSONObject(i) ?: continue
                val name = row.optString("name").trim()
                if (name.isBlank()) continue
                add(TopArtistRow(name, row.optString("playcount").toIntOrNull() ?: 0))
            }
        }
    }

    private fun getRecentArtists(apiKey: String, username: String, limit: Int): List<String> {
        val json = lastFmGet(
            mapOf(
                "method" to "user.getRecentTracks",
                "user" to username,
                "limit" to limit.coerceIn(1, 200).toString(),
                "page" to "1",
                "extended" to "0",
                "api_key" to apiKey,
                "format" to "json"
            )
        )
        val arr = json.optJSONObject("recenttracks")?.optJSONArray("track") ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val row = arr.optJSONObject(i) ?: continue
                val artistNode = row.opt("artist")
                val name = when (artistNode) {
                    is JSONObject -> artistNode.optString("#text")
                    is String -> artistNode
                    else -> ""
                }.trim()
                if (name.isNotBlank()) add(name)
            }
        }
    }

    private fun getTopTags(apiKey: String, artist: String, limit: Int): List<String> {
        val json = lastFmGet(
            mapOf(
                "method" to "artist.getTopTags",
                "artist" to artist,
                "autocorrect" to "1",
                "api_key" to apiKey,
                "format" to "json"
            )
        )
        val arr = json.optJSONObject("toptags")?.optJSONArray("tag") ?: return emptyList()
        return buildList {
            for (i in 0 until minOf(arr.length(), limit)) {
                val name = arr.optJSONObject(i)?.optString("name").orEmpty().trim()
                if (name.isNotBlank()) add(name)
            }
        }
    }

    private fun weightedAverage(values: List<Pair<MetalVector, Float>>): MetalVector? {
        if (values.isEmpty()) return null
        val total = values.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(.001f)
        fun avg(f: (MetalVector) -> Float): Float =
            values.sumOf { (vector, weight) -> (f(vector) * weight).toDouble() }.toFloat() / total
        return MetalVector(
            avg { it.melody },
            avg { it.speed },
            avg { it.heavy },
            avg { it.symphonic },
            avg { it.technical },
            avg { it.growl },
            avg { it.cleanVocal },
            avg { it.catchy }
        ).clamped()
    }

    private fun lastFmGet(params: Map<String, String>): JSONObject {
        val query = params.entries.joinToString("&") { (key, value) ->
            "${enc(key)}=${enc(value)}"
        }
        val connection = (URL("https://ws.audioscrobbler.com/2.0/?$query").openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 12_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "metaranai/0.11")
        }
        val code = connection.responseCode
        val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) error("Last.fm HTTP $code: ${body.take(180)}")
        val json = JSONObject(body)
        if (json.has("error")) error("Last.fm error ${json.optInt("error")}: ${json.optString("message")}")
        return json
    }

    private fun enc(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
    private fun normalize(value: String): String = value.trim().lowercase().replace(Regex("\\s+"), " ")

    private data class MutableSignal(
        val name: String,
        var score: Float = 0f,
        var playcount: Int = 0,
        var recentCount: Int = 0
    )

    private data class TopArtistRow(val name: String, val playcount: Int)
}

data class LastFmArtistSignal(
    val name: String,
    val score: Float,
    val playcount: Int,
    val recentCount: Int,
    val tags: List<String>,
    val vector: MetalVector
)

data class LastFmProfileSyncResult(
    val username: String,
    val seedArtists: List<String>,
    val inferredProfile: MetalVector?,
    val matchedArtists: List<String>,
    val recentTrackCount: Int,
    val summary: String
)
