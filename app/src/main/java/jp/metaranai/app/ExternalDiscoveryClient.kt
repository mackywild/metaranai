package jp.metaranai.app

import kotlinx.coroutines.ensureActive
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
    private val musicBrainz = MusicBrainzClient(requestTimeoutMs = 4_000)

    suspend fun discover(
        seeds: List<String>,
        genreLenses: List<String> = emptyList(),
        limitPerSeed: Int = 18,
        excludedArtistNames: Set<String> = emptySet(),
        fallbackGenres: List<String> = emptyList(),
        onCandidates: suspend (List<MetalArtist>, List<MetalArtist>) -> Unit = { _, _ -> }
    ): Result<ExternalDiscoveryResult> = withContext(Dispatchers.IO) {
        runCatching {
            val preference = store.overseasPreference()
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

            val known = MetalCatalog.artists.map { it.name.lowercase() }.toSet() +
                excludedArtistNames.map { it.trim().lowercase() }
            val budget = DiscoveryBudget()
            val shortlist = merged.values
                .filterNot { it.name.lowercase() in known }
                .filterNot { it.name.lowercase() in seeds.map { it.lowercase() } }
                .sortedByDescending { it.match }
                .take(48)

            val alreadyNames = store.loadExternalArtists().map { it.name.lowercase() }.toSet()
            val accepted = mutableListOf<MetalArtist>()
            shortlist.forEachIndexed { index, c ->
                if (!budget.hasTime()) return@forEachIndexed
                enrichCandidate(key, c, musicBrainzAllowed = index < 12, already = c.name.lowercase() in alreadyNames, budget = budget)
                    ?.takeIf { GenreLensCatalog.matches(it, genreLenses) && preference.allows(it) }?.let { artist ->
                        accepted += artist
                        if (accepted.size == 1 || accepted.size % 3 == 0) onCandidates(mergeCache(accepted), accepted.toList())
                    }
            }

            // Manual discovery must keep searching when similar artists are empty, known or non-metal.
            // Genre pages are bounded; automatic pool maintenance remains a separate operation.
            if (accepted.isEmpty() && fallbackGenres.isNotEmpty()) {
                var regionLookups = 0
                val attempted = shortlist.map { it.name.trim().lowercase() }.toMutableSet()
                for (page in 1..3) {
                    if (!budget.hasTime()) break
                    for (genre in fallbackGenres.distinct().take(4)) {
                        val actualPage = store.discoveryPage(genre, preference)
                        val candidates = getTopArtistsByTag(key, genre, 50, actualPage)
                        store.saveDiscoveryPage(genre, preference, if (candidates.isEmpty()) 1 else actualPage + 1)
                        candidates.forEach { mergeCandidate(merged, it) }
                        for (candidate in candidates) {
                            if (!budget.hasTime()) break
                            val name = candidate.name.trim().lowercase()
                            if (name in known || seeds.any { it.equals(candidate.name, true) } || !attempted.add(name)) continue
                            val enriched = enrichCandidate(key, candidate, musicBrainzAllowed = preference == OverseasPreference.NO && regionLookups++ < 12,
                                already = name in alreadyNames, forcedGenre = genre, budget = budget) ?: continue
                            if (!GenreLensCatalog.matches(enriched, genreLenses) || !preference.allows(enriched)) continue
                            accepted += enriched
                            if (accepted.size == 1 || accepted.size % 3 == 0) onCandidates(mergeCache(accepted), accepted.toList())
                            if (accepted.size >= 12) break
                        }
                        if (accepted.isNotEmpty()) break
                    }
                    if (accepted.isNotEmpty()) break
                }
            }
            val eligible = accepted.filter { GenreLensCatalog.matches(it, genreLenses) }
            val combined = mergeCache(eligible)
            ExternalDiscoveryResult(merged.size, eligible.size, combined.size, seeds.take(5), combined, eligible)
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
        excludedArtistNames: Set<String> = emptySet(),
        onCandidates: suspend (List<MetalArtist>) -> Unit = {}
    ): Result<GenrePoolResult> = withContext(Dispatchers.IO) {
        runCatching {
            val budget = DiscoveryBudget()
            val requested = genreLenses.distinct().filter { it in GenreLensCatalog.names() }.take(4)
            val genres = requested.ifEmpty { listOf("Metal") }
            val preference = store.overseasPreference()
            val key = store.lastFmApiKey()
            require(key.isNotBlank()) { "Last.fm API Keyを設定してください" }
            val excluded = excludedArtistNames.map { it.trim().lowercase() }.toSet()
            var archive = (store.loadInitialCatalog() + store.loadExternalArtists()).distinctBy { it.name.lowercase() }
            val fetchedByGenre = linkedMapOf<String, Int>()
            val accepted = mutableListOf<MetalArtist>()

            genres.forEach { genre ->
                if (!budget.hasTime()) return@forEach
                val filterGenres = if (genre == "Metal") emptyList() else listOf(genre)
                val before = RecommendationPool.unrated(archive, preference, filterGenres, excluded).size
                if (before >= minimumPerGenre) {
                    fetchedByGenre[genre] = 0
                    return@forEach
                }
                val need = minimumPerGenre - before
                val knownNames = archive.map { it.name.trim().lowercase() }.toMutableSet()
                val attempted = mutableSetOf<String>()
                var addedForGenre = 0
                var fetched = 0
                // NO searches a domestic tag first, while still enforcing the requested musical genre.
                val tags = if (preference == OverseasPreference.NO) listOf("Japanese Metal", genre).distinct() else listOf(genre)
                for (tag in tags) {
                    if (!budget.hasTime()) break
                    var mbLookups = 0
                    val batch = RefillPageWalker.collect(
                        startPage = store.discoveryPage(tag, preference), target = need - addedForGenre,
                        fetchPage = { page ->
                            kotlinx.coroutines.currentCoroutineContext().ensureActive()
                            mbLookups = 0
                            getTopArtistsByTag(key, tag, fetchPerGenre.coerceIn(12, 50), page)
                        },
                        accept = { candidate ->
                            kotlinx.coroutines.currentCoroutineContext().ensureActive()
                            val normalized = candidate.name.trim().lowercase()
                            if (normalized in knownNames || normalized in excluded || !attempted.add(normalized)) null
                            else {
                                val artist = enrichCandidate(key, candidate,
                                    musicBrainzAllowed = mbLookups++ < if (preference == OverseasPreference.NO) 12 else 4,
                                    already = false, forcedGenre = tag, budget = budget)
                                if (artist != null && preference.allows(artist) && GenreLensCatalog.matches(artist, filterGenres)) {
                                    knownNames += artist.name.trim().lowercase()
                                    artist
                                } else null
                            }
                        },
                        onPageCompleted = { next -> store.saveDiscoveryPage(tag, preference, next) },
                        shouldContinue = budget::hasTime,
                        onAccepted = { partial ->
                            if (partial.size == 1 || partial.size % 3 == 0) {
                                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                                onCandidates(mergeCache(accepted + partial))
                            }
                        }
                    )
                    fetched += batch.fetched
                    accepted += batch.artists
                    addedForGenre += batch.artists.size
                    archive = (archive + batch.artists).distinctBy { it.name.lowercase() }
                    if (addedForGenre >= need) break
                }
                fetchedByGenre[genre] = fetched
            }
            kotlinx.coroutines.currentCoroutineContext().ensureActive()

            val combined = mergeCache(accepted)
            val fullArchive = (store.loadInitialCatalog() + combined).distinctBy { it.name.lowercase() }
            val unratedArchive = preference.apply(fullArchive).filterNot { it.name.trim().lowercase() in excluded }
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
            val preference = store.overseasPreference()
            val key = store.lastFmApiKey()
            require(key.isNotBlank()) { "Last.fm API Keyを設定してください" }

            val raw = searchLastFm(key, q, limit.coerceIn(1, 15))
            val alreadyNames = store.loadExternalArtists().map { it.name.lowercase() }.toSet()
            val enriched = mutableListOf<MetalArtist>()
            raw.forEachIndexed { index, candidate ->
                enrichCandidate(
                    key, candidate.copy(seed = "Search:$q"), musicBrainzAllowed = index < if (preference == OverseasPreference.NO) 12 else 3,
                    already = candidate.name.lowercase() in alreadyNames
                )?.let(enriched::add)
            }

            val strict = enriched.filter { SearchQueryMatcher.matches(it, q) }
                .sortedWith(
                    compareByDescending<MetalArtist> { SearchQueryMatcher.exactName(it, q) }
                        .thenByDescending { it.metadataConfidence }
                )
            val suggestions = enriched.filterNot { SearchQueryMatcher.matches(it, q) }
                .take(5)

            // Last.fm artist.search is fuzzy. Only strict query matches enter the local archive;
            // fuzzy neighbors are returned separately as "もしかして…" candidates.
            val combined = mergeCache(strict)
            ArtistSearchResult(q, raw.size, strict.size, strict, suggestions, combined)
        }
    }

    private fun enrichCandidate(apiKey: String, c: Candidate, musicBrainzAllowed: Boolean, already: Boolean, forcedGenre: String? = null, budget: DiscoveryBudget? = null): MetalArtist? {
        if (budget != null && !budget.hasTime()) return null
        val preference = store.overseasPreference()
        val knownArtist = MetalCatalog.findByName(c.name) ?: store.loadExternalArtists().firstOrNull { it.name.equals(c.name, true) }
        val mb = if (musicBrainzAllowed && (knownArtist == null || knownArtist.country == "External"))
            runCatching { musicBrainz.searchArtist(c.name) }.getOrNull() else null
        val location = mb?.country ?: mb?.area ?: knownArtist?.country ?: "External"
        if (preference == OverseasPreference.NO && !OverseasPreference.isJapanese(location, mb?.area ?: knownArtist?.area)) return null
        if (budget != null && !budget.hasTime()) return null
        val tags = getTopTags(apiKey, c.name)
        val metalTags = tags.filter { DiscoveryTagMapper.isMetalTag(it) }.toMutableList()
        // tag.getTopArtists(genre) is itself a genre membership signal. Preserve it in the local archive
        // even when the artist's current top-tags list only contains a broader metal tag.
        if (forcedGenre != null && metalTags.none { it.equals(forcedGenre, true) }) {
            metalTags.add(0, forcedGenre.lowercase())
        }
        if (metalTags.isEmpty()) return null

        val vocalType = VocalAnalyzer.infer(tags)
        if (budget != null && !budget.hasTime()) return null
        val info = getArtistInfo(apiKey, c.name)
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
            area = mb?.area ?: knownArtist?.area,
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
            requestMethod = "GET"; connectTimeout = 4_000; readTimeout = 5_000
            setRequestProperty("User-Agent", "Metaranai-Android/0.6.4")
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

data class ExternalDiscoveryResult(val fetched: Int, val accepted: Int, val cached: Int, val seeds: List<String>, val artists: List<MetalArtist>, val discoveredArtists: List<MetalArtist> = emptyList())


data class GenrePoolResult(val genres: List<String>, val fetched: Int, val accepted: Int, val cached: Int, val counts: Map<String, Int>, val artists: List<MetalArtist>)
