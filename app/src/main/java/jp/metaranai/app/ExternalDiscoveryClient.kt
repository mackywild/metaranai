package jp.metaranai.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import kotlin.math.roundToInt

/**
 * V0.6.1 external discovery:
 * - Last.fm similar/tag discovery
 * - Last.fm live artist.search
 * - Last.fm listeners/playcount
 * - MusicBrainz normalization
 * - unbounded local cache (deduplicated by normalized artist name)
 */
class ExternalDiscoveryClient(private val store: LocalStore) {
    private val musicBrainz = MusicBrainzClient()

    suspend fun discover(
        seeds: List<String>,
        genreLenses: List<String> = emptyList(),
        limitPerSeed: Int = 18
    ): Result<ExternalDiscoveryResult> = withContext(Dispatchers.IO) {
        runCatching {
            val key = store.lastFmApiKey()
            require(key.isNotBlank()) { "Last.fm API Keyを設定してください" }
            require(seeds.isNotEmpty() || genreLenses.isNotEmpty()) { "発掘SeedまたはGenre Lensがありません" }

            val merged = linkedMapOf<String, Candidate>()
            seeds.distinctBy { it.lowercase() }.take(5).forEach { seed ->
                getSimilar(key, seed, limitPerSeed).forEach { c -> mergeCandidate(merged, c.copy(seed = seed)) }
            }
            // Genre Lens adds a real genre-specific candidate pool; it is not only a ranking filter.
            genreLenses.distinct().take(4).forEach { genre ->
                getTopArtistsByTag(key, genre, 20).forEach { c -> mergeCandidate(merged, c) }
            }

            val known = MetalCatalog.artists.map { it.name.lowercase() }.toSet()
            val shortlist = merged.values
                .filterNot { it.name.lowercase() in known }
                .filterNot { it.name.lowercase() in seeds.map { it.lowercase() } }
                .sortedByDescending { it.match }
                .take(48)

            val alreadyNames = store.loadExternalArtists().map { it.name.lowercase() }.toSet()
            val accepted = mutableListOf<MetalArtist>()
            shortlist.forEachIndexed { index, c ->
                enrichCandidate(key, c, musicBrainzAllowed = index < 12, already = c.name.lowercase() in alreadyNames)?.let(accepted::add)
            }

            val combined = mergeCache(accepted)
            ExternalDiscoveryResult(merged.size, accepted.size, combined.size, seeds.take(5), combined)
        }
    }

    /**
     * V0.5.4 Genre Lens pool builder.
     * The selected genre remains a hard eligibility condition, but the target pool is counted
     * after excluding artists the user has already rated. This keeps discovery moving forward.
     */
    suspend fun ensureGenrePool(
        genreLenses: List<String>,
        minimumPerGenre: Int = 20,
        fetchPerGenre: Int = 50,
        excludedArtistNames: Set<String> = emptySet()
    ): Result<GenrePoolResult> = withContext(Dispatchers.IO) {
        runCatching {
            val genres = genreLenses.distinct().filter { it in GenreLensCatalog.names() }.take(4)
            require(genres.isNotEmpty()) { "Genre Lensがありません" }
            val key = store.lastFmApiKey()
            require(key.isNotBlank()) { "Last.fm API Keyを設定してください" }
            val excluded = excludedArtistNames.map { it.trim().lowercase() }.toSet()

            var archive = (MetalCatalog.artists + store.loadExternalArtists()).distinctBy { it.name.lowercase() }
            val fetchedByGenre = linkedMapOf<String, Int>()
            val accepted = mutableListOf<MetalArtist>()

            genres.forEach { genre ->
                val before = GenreLensCatalog.filter(
                    archive.filterNot { it.name.trim().lowercase() in excluded },
                    listOf(genre)
                ).size
                if (before >= minimumPerGenre) {
                    fetchedByGenre[genre] = 0
                    return@forEach
                }
                val need = minimumPerGenre - before
                val knownNames = archive.map { it.name.trim().lowercase() }.toMutableSet()
                // Do not get stuck on the same top-50 list after the user has rated it all.
                // Walk deeper Last.fm tag pages until we have enough genuinely new names (max 3 pages/run).
                val raw = mutableListOf<Candidate>()
                for (page in 1..3) {
                    raw += getTopArtistsByTag(key, genre, fetchPerGenre.coerceIn(12, 50), page)
                    val freshNames = raw.map { it.name.trim().lowercase() }
                        .distinct()
                        .count { it !in knownNames && it !in excluded }
                    if (freshNames >= need) break
                }
                fetchedByGenre[genre] = raw.size
                var addedForGenre = 0
                var mbLookups = 0
                for (candidate in raw) {
                    if (addedForGenre >= need) break
                    val normalized = candidate.name.trim().lowercase()
                    // Existing cached artists (rated or unrated) are never duplicated.
                    // Rated names are explicitly excluded from satisfying the refill target.
                    if (normalized in knownNames || normalized in excluded) continue
                    val artist = enrichCandidate(
                        key,
                        candidate,
                        musicBrainzAllowed = mbLookups < 4,
                        already = false,
                        forcedGenre = genre
                    ) ?: continue
                    if (mbLookups < 4) mbLookups++
                    accepted += artist
                    knownNames += artist.name.trim().lowercase()
                    addedForGenre++
                    archive = (archive + artist).distinctBy { it.name.lowercase() }
                }
            }

            val combined = mergeCache(accepted)
            val fullArchive = (MetalCatalog.artists + combined).distinctBy { it.name.lowercase() }
            val unratedArchive = fullArchive.filterNot { it.name.trim().lowercase() in excluded }
            GenrePoolResult(
                genres = genres,
                fetched = fetchedByGenre.values.sum(),
                accepted = accepted.size,
                cached = combined.size,
                counts = GenreLensCatalog.countByGenre(unratedArchive, genres),
                artists = combined
            )
        }
    }

    /**
     * Live global search. Local search is handled first in MainViewModel; this expands beyond the device DB.
     * Results that are confirmed as metal are immediately retained in the local archive.
     */
    suspend fun searchArtists(query: String, limit: Int = 10): Result<ArtistSearchResult> = withContext(Dispatchers.IO) {
        runCatching {
            val q = query.trim()
            require(q.length >= 2) { "2文字以上入力してください" }
            val key = store.lastFmApiKey()
            require(key.isNotBlank()) { "Last.fm API Keyを設定してください" }

            val raw = searchLastFm(key, q, limit.coerceIn(1, 15))
            val alreadyNames = store.loadExternalArtists().map { it.name.lowercase() }.toSet()
            val accepted = mutableListOf<MetalArtist>()
            raw.forEachIndexed { index, candidate ->
                enrichCandidate(
                    key, candidate.copy(seed = "Search:$q"), musicBrainzAllowed = index < 3,
                    already = candidate.name.lowercase() in alreadyNames
                )?.let(accepted::add)
            }
            val combined = mergeCache(accepted)
            ArtistSearchResult(q, raw.size, accepted.size, accepted, combined)
        }
    }

    private fun enrichCandidate(apiKey: String, c: Candidate, musicBrainzAllowed: Boolean, already: Boolean, forcedGenre: String? = null): MetalArtist? {
        val tags = getTopTags(apiKey, c.name)
        val metalTags = tags.filter { DiscoveryTagMapper.isMetalTag(it) }.toMutableList()
        // tag.getTopArtists(genre) is itself a genre membership signal. Preserve it in the local archive
        // even when the artist's current top-tags list only contains a broader metal tag.
        if (forcedGenre != null && metalTags.none { it.equals(forcedGenre, true) }) {
            metalTags.add(0, forcedGenre.lowercase())
        }
        if (metalTags.isEmpty()) return null

        val vocalType = VocalAnalyzer.infer(tags)
        val info = getArtistInfo(apiKey, c.name)
        val mb = if (musicBrainzAllowed) runCatching { musicBrainz.searchArtist(c.name) }.getOrNull() else null
        val vector = DiscoveryTagMapper.vectorFromTags(tags)
        val discovery = (.72f + (1f - c.match.coerceIn(0f, 1f)) * .22f + if (!already) .06f else 0f)
            .coerceIn(.55f, .99f)
        val confidence = when {
            mb?.mbid != null && info.mbid != null && mb.mbid.equals(info.mbid, true) -> 100
            info.mbid != null -> 90
            mb?.mbid != null -> 70
            else -> 45
        }
        val hidden = HiddenScoreEngine.score(info.listeners, info.playcount, discovery, confidence)
        val location = mb?.area ?: mb?.country ?: "External"
        return MetalArtist(
            name = mb?.name ?: c.name,
            country = location,
            genres = metalTags.take(5).map(DiscoveryTagMapper::displayTag),
            vector = vector,
            discovery = discovery,
            reason = buildString {
                append(if (c.seed.startsWith("Search:")) "Last.fmリアルタイム検索から発見" else "Last.fmで${c.seed}から発掘")
                if (info.listeners != null) append("。listeners ${formatCount(info.listeners)}")
                if (mb?.beginDate != null) append("。活動開始 ${mb.beginDate}")
            },
            source = ArtistSource.LASTFM_MUSICBRAINZ,
            sourceSeed = c.seed,
            externalScore = (c.match * 100).roundToInt(),
            lastFmListeners = info.listeners,
            lastFmPlaycount = info.playcount,
            mbid = info.mbid ?: mb?.mbid,
            area = mb?.area,
            beginDate = mb?.beginDate,
            endDate = mb?.endDate,
            ended = mb?.ended,
            hiddenScore = hidden,
            metadataConfidence = confidence,
            vocalType = vocalType
        )
    }

    private fun mergeCache(newArtists: List<MetalArtist>): List<MetalArtist> {
        val combined = (store.loadExternalArtists() + newArtists)
            .groupBy { it.name.trim().lowercase() }
            .map { (_, xs) ->
                xs.maxWithOrNull(
                    compareBy<MetalArtist> { it.metadataConfidence }
                        .thenBy { it.hiddenScore }
                        .thenBy { if (it.mbid != null) 1 else 0 }
                )!!
            }
            .sortedWith(compareByDescending<MetalArtist> { it.hiddenScore }.thenByDescending { it.discovery })
        store.saveExternalArtists(combined)
        return combined
    }

    private fun mergeCandidate(target: MutableMap<String, Candidate>, c: Candidate) {
        val id = c.name.trim().lowercase()
        if (id.isBlank()) return
        val existing = target[id]
        if (existing == null || c.match > existing.match) target[id] = c
    }

    private fun searchLastFm(apiKey: String, query: String, limit: Int): List<Candidate> {
        val json = lastFmGet(mapOf(
            "method" to "artist.search", "artist" to query, "limit" to limit.toString(),
            "api_key" to apiKey, "format" to "json"
        ))
        val arr = json.optJSONObject("results")?.optJSONObject("artistmatches")?.optJSONArray("artist") ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val name = o.optString("name").trim()
                if (name.isBlank()) continue
                // Search API has no similarity score; preserve rank as a soft relevance signal.
                val rank = (1f - i * .055f).coerceAtLeast(.48f)
                add(Candidate(name, rank, "Search:$query"))
            }
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

    private fun getTopArtistsByTag(apiKey: String, tag: String, limit: Int, page: Int = 1): List<Candidate> {
        val json = lastFmGet(mapOf(
            "method" to "tag.getTopArtists", "tag" to tag, "limit" to limit.toString(),
            "page" to page.coerceAtLeast(1).toString(), "api_key" to apiKey, "format" to "json"
        ))
        val arr = json.optJSONObject("topartists")?.optJSONArray("artist") ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val name = o.optString("name").trim()
                if (name.isBlank()) continue
                val globalRank = (page.coerceAtLeast(1) - 1) * limit + i
                val rankScore = (0.94f - globalRank * 0.008f).coerceAtLeast(.42f)
                add(Candidate(name, rankScore, "Genre:$tag"))
            }
        }
    }

    private fun getTopTags(apiKey: String, artist: String): List<String> {
        val json = lastFmGet(mapOf("method" to "artist.getTopTags", "artist" to artist, "autocorrect" to "1", "api_key" to apiKey, "format" to "json"))
        val arr = json.optJSONObject("toptags")?.optJSONArray("tag") ?: return emptyList()
        return buildList {
            for (i in 0 until minOf(arr.length(), 20)) {
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
            requestMethod = "GET"; connectTimeout = 10_000; readTimeout = 12_000
            setRequestProperty("User-Agent", "Metaranai-Android/0.6.1")
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


data class GenrePoolResult(val genres: List<String>, val fetched: Int, val accepted: Int, val cached: Int, val counts: Map<String, Int>, val artists: List<MetalArtist>)
