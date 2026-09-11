package jp.metaranai.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

class SpotifyClient(private val context: Context, private val store: LocalStore) {
    private val musicBrainz = MusicBrainzClient()
    private val redirectUri = "http://127.0.0.1:8888/callback"
    private var verifier: String = ""
    private var state: String = ""

    suspend fun loginAndSync(onStatus: (String) -> Unit): Result<SpotifySyncResult> = withContext(Dispatchers.IO) {
        runCatching {
            val clientId = store.clientId()
            require(clientId.isNotBlank()) { "Spotify Client IDを設定してください" }
            val token = if (store.token().isNotBlank() && store.tokenExpiry() > System.currentTimeMillis() + 60_000) {
                store.token()
            } else if (store.refreshToken().isNotBlank()) {
                onStatus("Spotifyセッションを更新中")
                refreshAccessToken(clientId)
            } else {
                authorize(clientId, onStatus)
            }
            syncSignals(token, onStatus)
        }
    }


    /**
     * V0.6.4 Spotify Identity Resolver.
     *
     * Key fixes after the real Yutaro Abe's ASTRAL WIND regression:
     * - search Spotify the same way a user does (plain artist name) in addition to field-filter search,
     * - do not depend on Spotify genres (deprecated / frequently empty),
     * - fingerprint the Spotify candidate catalog against TWO independent catalogs:
     *   Last.fm top tracks and Apple's public iTunes Search catalog,
     * - compare both track names and album names,
     * - same-name artists still require a unique evidence winner.
     */
    suspend fun resolveArtistDestination(artist: MetalArtist): SpotifyArtistDestination = withContext(Dispatchers.IO) {
        store.spotifyArtistLinkV064(artist)?.let { return@withContext it }

        val fallback = "https://open.spotify.com/search/${enc(artist.name)}"
        var strongIdentityReason: String? = null

        // Route A: trusted MBID -> explicit Spotify relation.
        artist.mbid?.takeIf { it.isNotBlank() && artist.metadataConfidence >= 90 }?.let { mbid ->
            resolveSpotifyFromMusicBrainz(mbid)?.let { verified ->
                val out = verified.copy(verification = "MusicBrainz MBIDで本人確認")
                store.saveSpotifyArtistLinkV064(artist, out.url, out.artistId, out.verification)
                return@withContext out
            }
            strongIdentityReason = "高信頼MBID"
        }

        // Route B: recover a trustworthy MBID from Local DB metadata.
        if (strongIdentityReason == null) {
            val identity = runCatching { musicBrainz.resolveIdentityForSpotify(artist) }.getOrNull()
            if (identity != null && identity.confidence >= 80) {
                val mbid = identity.artist.mbid
                if (!mbid.isNullOrBlank()) {
                    resolveSpotifyFromMusicBrainz(mbid)?.let { verified ->
                        val out = verified.copy(verification = "MusicBrainzメタデータ照合: ${identity.reason}")
                        store.saveSpotifyArtistLinkV064(artist, out.url, out.artistId, out.verification)
                        return@withContext out
                    }
                    strongIdentityReason = "MusicBrainzメタデータ照合: ${identity.reason}"
                }
            }
        }

        val clientId = store.clientId()
        if (clientId.isBlank()) {
            return@withContext SpotifyArtistDestination(fallback, false, verification = "Spotify Client ID未設定: 検索へ")
        }
        val token = runCatching {
            when {
                store.token().isNotBlank() && store.tokenExpiry() > System.currentTimeMillis() + 60_000 -> store.token()
                store.refreshToken().isNotBlank() -> refreshAccessToken(clientId)
                else -> ""
            }
        }.getOrDefault("")
        if (token.isBlank()) {
            return@withContext SpotifyArtistDestination(fallback, false, verification = "Spotify未認証: 検索へ")
        }

        val exactMatches = runCatching { spotifyExactArtistMatches(artist.name, token) }.getOrDefault(emptyList())
        if (exactMatches.isEmpty()) {
            return@withContext SpotifyArtistDestination(fallback, false, verification = "Spotify完全一致なし: 検索へ")
        }

        // Independent catalog evidence. Last.fm can be sparse for underground artists, so Apple is
        // a fallback/second opinion rather than silently treating missing Last.fm data as a mismatch.
        val lastFmTracks = runCatching { lastFmTopTrackNames(artist, 40) }.getOrDefault(emptyList())
        val appleGroups = runCatching { appleCatalogGroups(artist.name) }.getOrDefault(emptyList())

        val fingerprints = exactMatches.map { candidate ->
            val catalog = runCatching { spotifyCatalogForCandidate(candidate, token) }
                .getOrDefault(SpotifyCatalog(emptyList(), emptyList()))

            val evidences = mutableListOf<CatalogEvidence>()
            if (lastFmTracks.isNotEmpty()) {
                evidences += CatalogEvidence(
                    source = "Last.fm",
                    trackMatches = matchingNames(lastFmTracks, catalog.tracks, ::canonicalTrackName),
                    albumMatches = emptyList()
                )
            }
            appleGroups.forEach { group ->
                evidences += CatalogEvidence(
                    source = "Apple Catalog",
                    trackMatches = matchingNames(group.tracks, catalog.tracks, ::canonicalTrackName),
                    albumMatches = matchingNames(group.albums, catalog.albums, ::canonicalAlbumName)
                )
            }
            val bestEvidence = evidences.maxByOrNull { it.score } ?: CatalogEvidence("なし", emptyList(), emptyList())
            CandidateFingerprint(
                candidate = candidate,
                genreScore = genreEvidenceScore(artist.genres, jsonArrayStrings(candidate.optJSONArray("genres"))),
                catalog = catalog,
                evidence = bestEvidence
            )
        }

        fun isStrong(fp: CandidateFingerprint): Boolean =
            fp.evidence.trackMatches.size >= 2 ||
                fp.evidence.albumMatches.size >= 2 ||
                (fp.evidence.trackMatches.isNotEmpty() && fp.evidence.albumMatches.isNotEmpty())

        fun reasonFor(fp: CandidateFingerprint): String = buildString {
            append("Spotify名前完全一致 + ${fp.evidence.source}照合")
            if (fp.evidence.trackMatches.isNotEmpty()) append(" 曲${fp.evidence.trackMatches.size}件")
            if (fp.evidence.albumMatches.isNotEmpty()) append(" Album${fp.evidence.albumMatches.size}件")
        }

        val chosen: Pair<JSONObject, String> = when {
            exactMatches.size == 1 && isStrong(fingerprints.single()) -> {
                val fp = fingerprints.single()
                fp.candidate to reasonFor(fp)
            }

            exactMatches.size == 1 && fingerprints.single().evidence.trackMatches.size == 1 &&
                (strongIdentityReason != null || fingerprints.single().genreScore >= 1) -> {
                val fp = fingerprints.single()
                val support = strongIdentityReason ?: "Genre照合"
                fp.candidate to "${reasonFor(fp)} + $support"
            }

            // Metadata evidence remains a safe fallback when external catalog services have no data.
            exactMatches.size == 1 && strongIdentityReason != null ->
                exactMatches.single() to "$strongIdentityReason + Spotify名前完全一致"
            exactMatches.size == 1 && fingerprints.single().genreScore >= 2 ->
                exactMatches.single() to "Spotify名前完全一致 + Genre照合"

            // Same-name artists require ONE clearly stronger catalog fingerprint.
            exactMatches.size > 1 -> {
                val sorted = fingerprints.sortedByDescending { it.evidence.score }
                val winner = sorted.first()
                val runnerUp = sorted.getOrNull(1)
                if (isStrong(winner) && winner.evidence.score >= (runnerUp?.evidence?.score ?: 0) + 3) {
                    winner.candidate to "同名候補から${winner.evidence.source}曲/Album指紋で一意確認"
                } else {
                    val strongGenre = fingerprints.filter { it.genreScore >= 2 }
                    if (strongGenre.size == 1 && sorted.all { it.evidence.score == 0 }) {
                        strongGenre.single().candidate to "同名候補からGenreで一意確認"
                    } else {
                        return@withContext SpotifyArtistDestination(
                            fallback, false,
                            verification = "同名Artistを独立カタログで一意に特定できない: 検索へ"
                        )
                    }
                }
            }

            else -> return@withContext SpotifyArtistDestination(
                fallback, false,
                verification = buildString {
                    append("名前は完全一致だが本人確認材料不足")
                    if (lastFmTracks.isEmpty()) append(" / Last.fm曲0")
                    if (appleGroups.isEmpty()) append(" / Apple候補0")
                    append(": 検索へ")
                }
            )
        }

        val url = chosen.first.optJSONObject("external_urls")?.optString("spotify").orEmpty()
        if (url.isBlank()) {
            return@withContext SpotifyArtistDestination(fallback, false, verification = "Spotify URLなし: 検索へ")
        }
        val id = chosen.first.optString("id").takeIf { it.isNotBlank() }
        val verified = SpotifyArtistDestination(url, true, id, chosen.second)
        store.saveSpotifyArtistLinkV064(artist, url, id, verified.verification)
        verified
    }

    private data class CandidateFingerprint(
        val candidate: JSONObject,
        val genreScore: Int,
        val catalog: SpotifyCatalog,
        val evidence: CatalogEvidence
    )

    private data class SpotifyCatalog(
        val tracks: List<String>,
        val albums: List<String>
    )

    private data class ExternalCatalogGroup(
        val id: String,
        val tracks: List<String>,
        val albums: List<String>
    )

    private data class CatalogEvidence(
        val source: String,
        val trackMatches: List<String>,
        val albumMatches: List<String>
    ) {
        // Album matches are highly discriminating for same-name artists.
        val score: Int get() = trackMatches.size * 3 + albumMatches.size * 4
    }

    /**
     * Search like the Spotify UI first (plain name), then add the field-filter query as a second path.
     * Only canonical exact-name candidates survive either search, and IDs are deduplicated.
     */
    private fun spotifyExactArtistMatches(artistName: String, token: String): List<JSONObject> {
        val target = canonicalExactName(artistName)
        val found = linkedMapOf<String, JSONObject>()
        val queries = listOf(artistName, "artist:\"$artistName\"")
        for (queryText in queries.distinct()) {
            val q = URLEncoder.encode(queryText, Charsets.UTF_8.name())
            for (offset in listOf(0, 10, 20)) {
                val page = runCatching {
                    getJson("https://api.spotify.com/v1/search?q=$q&type=artist&limit=10&offset=$offset", token)
                }.getOrNull() ?: break
                val items = page.optJSONObject("artists")?.optJSONArray("items") ?: break
                for (i in 0 until items.length()) {
                    val candidate = items.optJSONObject(i) ?: continue
                    if (canonicalExactName(candidate.optString("name")) != target) continue
                    val id = candidate.optString("id").trim()
                    if (id.isNotBlank()) found.putIfAbsent(id, candidate)
                }
                if (items.length() < 10) break
            }
        }
        return found.values.toList()
    }

    /** Last.fm catalog fingerprint. */
    private fun lastFmTopTrackNames(artist: MetalArtist, limit: Int): List<String> {
        val apiKey = store.lastFmApiKey().trim()
        if (apiKey.isBlank()) return emptyList()
        val params = linkedMapOf(
            "method" to "artist.getTopTracks",
            "limit" to limit.coerceIn(5, 50).toString(),
            "autocorrect" to "1",
            "api_key" to apiKey,
            "format" to "json"
        )
        val trustedMbid = artist.mbid?.takeIf { it.isNotBlank() && artist.metadataConfidence >= 90 }
        if (trustedMbid != null) params["mbid"] = trustedMbid else params["artist"] = artist.name
        val query = params.entries.joinToString("&") { (k, v) -> "${enc(k)}=${enc(v)}" }
        val json = getPublicJson("https://ws.audioscrobbler.com/2.0/?$query", "Metaranai-Android/0.6.4")
        if (json.has("error")) return emptyList()
        val tracks = json.optJSONObject("toptracks")?.optJSONArray("track") ?: return emptyList()
        return buildList {
            for (i in 0 until tracks.length()) {
                tracks.optJSONObject(i)?.optString("name")?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
            }
        }.distinctBy(::canonicalTrackName)
    }

    /**
     * Independent catalog fallback for underground artists whose Last.fm top-track data is absent.
     * Apple's iTunes Search API is public/no-key. Results are grouped by Apple artistId so same-name
     * artists are not merged into one artificial catalog.
     */
    private fun appleCatalogGroups(artistName: String): List<ExternalCatalogGroup> {
        val target = canonicalExactName(artistName)
        val groups = linkedMapOf<String, Pair<LinkedHashSet<String>, LinkedHashSet<String>>>()

        for (country in listOf("JP", "US")) {
            // First resolve exact Apple artist identities, then lookup their songs by artistId.
            val artistSearchUrl = "https://itunes.apple.com/search?term=${enc(artistName)}" +
                "&country=$country&media=music&entity=musicArtist&attribute=artistTerm&limit=50"
            val artistSearch = runCatching { getPublicJson(artistSearchUrl, "Metaranai-Android/0.6.4") }.getOrNull()
            val artists = artistSearch?.optJSONArray("results")
            val exactAppleIds = linkedSetOf<String>()
            if (artists != null) {
                for (i in 0 until artists.length()) {
                    val item = artists.optJSONObject(i) ?: continue
                    if (canonicalExactName(item.optString("artistName")) != target) continue
                    item.opt("artistId")?.toString()?.takeIf { it.isNotBlank() }?.let(exactAppleIds::add)
                }
            }

            for (artistId in exactAppleIds) {
                val lookupUrl = "https://itunes.apple.com/lookup?id=$artistId&entity=song&limit=200&sort=recent&country=$country"
                val json = runCatching { getPublicJson(lookupUrl, "Metaranai-Android/0.6.4") }.getOrNull() ?: continue
                val results = json.optJSONArray("results") ?: continue
                val pair = groups.getOrPut(artistId) { linkedSetOf<String>() to linkedSetOf() }
                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    // lookup response starts with the artist object; only song rows have trackName.
                    if (item.optString("wrapperType") != "track") continue
                    if (canonicalExactName(item.optString("artistName")) != target) continue
                    item.optString("trackName").trim().takeIf { it.isNotBlank() }?.let(pair.first::add)
                    item.optString("collectionName").trim().takeIf { it.isNotBlank() }?.let(pair.second::add)
                }
            }

            // Some storefronts fail to return a musicArtist row for small artists. Fall back to an
            // artist-term song search, still grouping by Apple artistId so duplicates stay separated.
            if (groups.isEmpty()) {
                val songSearchUrl = "https://itunes.apple.com/search?term=${enc(artistName)}" +
                    "&country=$country&media=music&entity=song&attribute=artistTerm&limit=200"
                val json = runCatching { getPublicJson(songSearchUrl, "Metaranai-Android/0.6.4") }.getOrNull()
                val results = json?.optJSONArray("results")
                if (results != null) {
                    for (i in 0 until results.length()) {
                        val item = results.optJSONObject(i) ?: continue
                        if (canonicalExactName(item.optString("artistName")) != target) continue
                        val artistId = item.opt("artistId")?.toString()?.takeIf { it.isNotBlank() } ?: "name:$target"
                        val pair = groups.getOrPut(artistId) { linkedSetOf<String>() to linkedSetOf() }
                        item.optString("trackName").trim().takeIf { it.isNotBlank() }?.let(pair.first::add)
                        item.optString("collectionName").trim().takeIf { it.isNotBlank() }?.let(pair.second::add)
                    }
                }
            }
            if (groups.isNotEmpty()) break
        }
        return groups.map { (id, pair) ->
            ExternalCatalogGroup(id, pair.first.toList(), pair.second.toList())
        }
    }

    /** Candidate-ID-specific Spotify catalog. */
    private fun spotifyCatalogForCandidate(candidate: JSONObject, token: String): SpotifyCatalog {
        val artistId = candidate.optString("id").trim()
        val artistName = candidate.optString("name").trim()
        if (artistId.isBlank() || artistName.isBlank()) return SpotifyCatalog(emptyList(), emptyList())
        val tracks = linkedSetOf<String>()
        val albums = linkedSetOf<String>()
        val tq = URLEncoder.encode("artist:\"$artistName\"", Charsets.UTF_8.name())

        // Search can find recent/popular tracks quickly; filter by candidate Spotify artist ID.
        for (offset in listOf(0, 10, 20)) {
            val json = runCatching {
                getJson("https://api.spotify.com/v1/search?q=$tq&type=track&limit=10&offset=$offset", token)
            }.getOrNull() ?: break
            val items = json.optJSONObject("tracks")?.optJSONArray("items") ?: break
            for (i in 0 until items.length()) {
                val track = items.optJSONObject(i) ?: continue
                if (!trackHasArtistId(track, artistId)) continue
                track.optString("name").trim().takeIf { it.isNotBlank() }?.let(tracks::add)
                track.optJSONObject("album")?.optString("name")?.trim()?.takeIf { it.isNotBlank() }?.let(albums::add)
            }
            if (items.length() < 10 || tracks.size >= 20) break
        }

        // Artist Albums is ID-specific, so it is the reliable catalog backbone for same-name cases.
        val albumJson = runCatching {
            getJson("https://api.spotify.com/v1/artists/$artistId/albums?include_groups=album,single&limit=10", token)
        }.getOrNull()
        val albumItems = albumJson?.optJSONArray("items")
        val albumIds = linkedSetOf<String>()
        if (albumItems != null) {
            for (i in 0 until albumItems.length()) {
                val album = albumItems.optJSONObject(i) ?: continue
                album.optString("name").trim().takeIf { it.isNotBlank() }?.let(albums::add)
                album.optString("id").trim().takeIf { it.isNotBlank() }?.let(albumIds::add)
            }
        }

        // Read a few album tracklists even if Search already found tracks. Album-track lookup by ID
        // is much less sensitive to Search ranking and is important for underground artists.
        for (albumId in albumIds.take(5)) {
            val items = runCatching {
                getJson("https://api.spotify.com/v1/albums/$albumId/tracks?limit=50", token)
            }.getOrNull()?.optJSONArray("items") ?: continue
            for (i in 0 until items.length()) {
                val track = items.optJSONObject(i) ?: continue
                if (trackHasArtistId(track, artistId)) {
                    track.optString("name").trim().takeIf { it.isNotBlank() }?.let(tracks::add)
                }
            }
        }
        return SpotifyCatalog(tracks.toList(), albums.toList())
    }

    private fun trackHasArtistId(track: JSONObject, artistId: String): Boolean {
        val artists = track.optJSONArray("artists") ?: return false
        for (i in 0 until artists.length()) {
            if (artists.optJSONObject(i)?.optString("id") == artistId) return true
        }
        return false
    }

    private fun matchingNames(
        left: List<String>,
        right: List<String>,
        canon: (String) -> String
    ): List<String> {
        if (left.isEmpty() || right.isEmpty()) return emptyList()
        val remote = right.map(canon).filter(::isUsefulFingerprint).toSet()
        return left.map(canon).filter(::isUsefulFingerprint).filter { it in remote }.distinct()
    }

    private fun canonicalTrackName(value: String): String {
        var s = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKC).lowercase()
        // Strip track-number prefixes ("06-UNCHAINED", "09 BWV1041") before comparison.
        s = s.replace(Regex("^\\s*\\d{1,2}[\\s._#:-]+"), "")
        s = s.replace(Regex("[\\(\\[][^\\)\\]]*(remaster(?:ed)?|live|version|edit|mix|demo|acoustic|instrumental)[^\\)\\]]*[\\)\\]]"), " ")
        s = s.replace("&", " and ")
        s = s.replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        return s.trim().replace(Regex("\\s+"), " ")
    }

    private fun canonicalAlbumName(value: String): String {
        var s = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKC).lowercase()
        s = s.replace(Regex("[\\(\\[][^\\)\\]]*(remaster(?:ed)?|deluxe|expanded|edition|version)[^\\)\\]]*[\\)\\]]"), " ")
        s = s.replace("&", " and ")
        s = s.replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        return s.trim().replace(Regex("\\s+"), " ")
    }

    private fun isUsefulFingerprint(value: String): Boolean {
        if (value.length < 4) return false
        val generic = setOf(
            "intro", "outro", "interlude", "overture", "reprise", "instrumental",
            "untitled", "bonus track", "prologue", "epilogue"
        )
        return value !in generic
    }

    private fun canonicalExactName(value: String): String = java.text.Normalizer
        .normalize(value, java.text.Normalizer.Form.NFKC)
        .trim()
        .lowercase()
        .replace(Regex("\\s+"), " ")

    private fun jsonArrayStrings(array: org.json.JSONArray?): List<String> = buildList {
        if (array != null) for (i in 0 until array.length()) {
            array.optString(i).trim().takeIf { it.isNotBlank() }?.let(::add)
        }
    }

    /** Spotify genre is deprecated and is only tertiary evidence now. */
    private fun genreEvidenceScore(localGenres: List<String>, spotifyGenres: List<String>): Int {
        if (localGenres.isEmpty() || spotifyGenres.isEmpty()) return 0
        fun norm(v: String) = v.lowercase().replace('ü', 'u').trim()
        val locals = localGenres.map(::norm)
        val remotes = spotifyGenres.map(::norm)
        var score = 0
        for (local in locals) for (remote in remotes) {
            if (local == remote && local != "metal") score = maxOf(score, 4)
            else if (local.length >= 5 && remote.length >= 5 && (local.contains(remote) || remote.contains(local))) score = maxOf(score, 3)
        }
        GenreLensCatalog.lenses.forEach { lens ->
            val localMatches = lens.aliases.any { alias -> locals.any { it.contains(norm(alias)) } }
            val remoteMatches = lens.aliases.any { alias -> remotes.any { it.contains(norm(alias)) } }
            if (localMatches && remoteMatches) score = maxOf(score, 3)
        }
        if (score == 0 && locals.any { it.contains("metal") } && remotes.any { it.contains("metal") }) score = 1
        return score
    }

    private fun resolveSpotifyFromMusicBrainz(mbid: String): SpotifyArtistDestination? {
        val parsed = musicBrainz.spotifyArtistRelation(mbid) ?: return null
        return SpotifyArtistDestination(parsed.first, true, parsed.second, "MusicBrainz MBIDで本人確認")
    }

    private fun getPublicJson(url: String, userAgent: String): JSONObject {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "GET"
        c.connectTimeout = 12_000
        c.readTimeout = 15_000
        c.setRequestProperty("User-Agent", userAgent)
        val code = c.responseCode
        val stream = if (code in 200..299) c.inputStream else c.errorStream
        val text = stream.bufferedReader().use { it.readText() }
        if (code !in 200..299) error("Public catalog API error $code: $text")
        return JSONObject(text)
    }

    private suspend fun authorize(clientId: String, onStatus: (String) -> Unit): String {
        verifier = randomString(64)
        state = randomString(24)
        val challenge = Base64.encodeToString(
            MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )
        val scopes = "user-read-recently-played user-top-read"
        val auth = "https://accounts.spotify.com/authorize?" + mapOf(
            "client_id" to clientId,
            "response_type" to "code",
            "redirect_uri" to redirectUri,
            "scope" to scopes,
            "code_challenge_method" to "S256",
            "code_challenge" to challenge,
            "state" to state
        ).entries.joinToString("&") { "${enc(it.key)}=${enc(it.value)}" }

        val server = ServerSocket(8888, 1, InetAddress.getByName("127.0.0.1"))
        server.soTimeout = 180_000
        onStatus("Spotify認証画面を開きました")
        withContext(Dispatchers.Main) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(auth)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        try {
            val socket = server.accept()
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val firstLine = reader.readLine() ?: error("認証応答を受信できませんでした")
            val path = firstLine.split(" ").getOrNull(1) ?: error("認証応答が不正です")
            val uri = Uri.parse("http://127.0.0.1$path")
            val code = uri.getQueryParameter("code") ?: error(uri.getQueryParameter("error") ?: "認証コードがありません")
            require(uri.getQueryParameter("state") == state) { "state検証に失敗しました" }
            val body = "<html><body style='font-family:sans-serif;background:#111;color:#fff;padding:40px'><h2>メタらない？</h2><p>Spotify認証が完了しました。アプリに戻ってください。</p></body></html>"
            socket.getOutputStream().use { out ->
                out.write("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${body.toByteArray().size}\r\nConnection: close\r\n\r\n$body".toByteArray())
                out.flush()
            }
            socket.close()
            onStatus("トークンを取得中")
            val tokenJson = postForm("https://accounts.spotify.com/api/token", mapOf(
                "client_id" to clientId,
                "grant_type" to "authorization_code",
                "code" to code,
                "redirect_uri" to redirectUri,
                "code_verifier" to verifier
            ))
            saveTokens(tokenJson)
            return tokenJson.getString("access_token")
        } finally {
            runCatching { server.close() }
        }
    }

    private fun refreshAccessToken(clientId: String): String {
        val tokenJson = postForm("https://accounts.spotify.com/api/token", mapOf(
            "client_id" to clientId,
            "grant_type" to "refresh_token",
            "refresh_token" to store.refreshToken()
        ))
        saveTokens(tokenJson)
        return tokenJson.getString("access_token")
    }

    private fun saveTokens(json: JSONObject) {
        store.saveToken(json.getString("access_token"))
        json.optString("refresh_token").takeIf { it.isNotBlank() }?.let(store::saveRefreshToken)
        val expires = json.optLong("expires_in", 3600L)
        store.saveTokenExpiry(System.currentTimeMillis() + expires * 1000L)
    }

    private fun syncSignals(token: String, onStatus: (String) -> Unit): SpotifySyncResult {
        onStatus("Top Artistsを解析中")
        val top = getJson("https://api.spotify.com/v1/me/top/artists?limit=20&time_range=medium_term", token)
        onStatus("Recently Playedを解析中")
        val recent = getJson("https://api.spotify.com/v1/me/player/recently-played?limit=50", token)

        val topItems = top.optJSONArray("items")
        val topNames = mutableListOf<String>()
        val genres = mutableListOf<String>()
        val vectors = mutableListOf<Pair<MetalVector, Float>>()
        if (topItems != null) for (i in 0 until topItems.length()) {
            val artist = topItems.getJSONObject(i)
            val name = artist.optString("name")
            if (name.isNotBlank()) topNames += name
            val genreArray = artist.optJSONArray("genres")
            if (genreArray != null) for (g in 0 until genreArray.length()) genres += genreArray.optString(g)
            MetalCatalog.findByName(name)?.let { vectors += it.vector to (1f - i.coerceAtMost(19) / 28f) }
        }

        val recentItems = recent.optJSONArray("items")
        val recentArtistCounts = linkedMapOf<String, Int>()
        if (recentItems != null) for (i in 0 until recentItems.length()) {
            val track = recentItems.getJSONObject(i).optJSONObject("track") ?: continue
            val artists = track.optJSONArray("artists") ?: continue
            for (a in 0 until artists.length()) {
                val name = artists.getJSONObject(a).optString("name")
                if (name.isNotBlank()) recentArtistCounts[name] = (recentArtistCounts[name] ?: 0) + 1
            }
        }
        recentArtistCounts.forEach { (name, count) ->
            MetalCatalog.findByName(name)?.let { vectors += it.vector to (0.25f + count.coerceAtMost(8) * .06f) }
        }

        val genreVector = GenreMapper.fromGenres(genres)
        if (genreVector != null) vectors += genreVector to .65f
        val inferred = weightedAverage(vectors)
        val matched = (topNames + recentArtistCounts.keys).distinct().mapNotNull { n -> MetalCatalog.findByName(n)?.name }.distinct()
        val genreSignals = genres.map { it.lowercase() }.distinct().take(8)
        val recentCount = recentItems?.length() ?: 0
        val summary = buildString {
            append("Top: ")
            append(topNames.take(4).ifEmpty { listOf("取得なし") }.joinToString(" / "))
            append(" ・ 最近${recentCount}件")
            if (matched.isNotEmpty()) append(" ・ DNA一致${matched.size}組")
        }
        store.saveSpotifySummary(summary)
        return SpotifySyncResult(summary, inferred, matched, genreSignals)
    }

    private fun weightedAverage(values: List<Pair<MetalVector, Float>>): MetalVector? {
        if (values.isEmpty()) return null
        val total = values.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(.001f)
        fun avg(f: (MetalVector) -> Float) = values.sumOf { (v, w) -> (f(v) * w).toDouble() }.toFloat() / total
        return MetalVector(avg { it.melody }, avg { it.speed }, avg { it.heavy }, avg { it.symphonic }, avg { it.technical }, avg { it.growl }, avg { it.cleanVocal }, avg { it.catchy })
    }

    private fun getJson(url: String, token: String): JSONObject {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("Authorization", "Bearer $token")
        c.connectTimeout = 15000; c.readTimeout = 15000
        val code = c.responseCode
        val stream = if (code in 200..299) c.inputStream else c.errorStream
        val text = stream.bufferedReader().use { it.readText() }
        if (code !in 200..299) error("Spotify API error $code: $text")
        return JSONObject(text)
    }

    private fun postForm(url: String, fields: Map<String,String>): JSONObject {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.doOutput = true
        c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        val payload = fields.entries.joinToString("&") { "${enc(it.key)}=${enc(it.value)}" }
        c.outputStream.use { it.write(payload.toByteArray()) }
        val code = c.responseCode
        val stream = if (code in 200..299) c.inputStream else c.errorStream
        val text = stream.bufferedReader().use { it.readText() }
        if (code !in 200..299) error("Spotify token error $code: $text")
        return JSONObject(text)
    }

    private fun enc(v: String) = URLEncoder.encode(v, Charsets.UTF_8.name())
    private fun randomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
        val r = SecureRandom()
        return (1..length).map { chars[r.nextInt(chars.length)] }.joinToString("")
    }
}

object GenreMapper {
    fun fromGenres(genres: List<String>): MetalVector? {
        val g = genres.map { it.lowercase() }
        if (g.isEmpty()) return null
        var v = MetalVector(.55f,.50f,.55f,.30f,.45f,.25f,.65f,.55f)
        var hits = 0
        fun apply(keyword: String, target: MetalVector, weight: Float = .22f) {
            if (g.any { keyword in it }) { v = v.blend(target, weight); hits++ }
        }
        apply("power metal", MetalVector(.93f,.88f,.58f,.65f,.65f,.05f,.95f,.91f), .32f)
        apply("symphonic", MetalVector(.90f,.66f,.61f,.98f,.66f,.12f,.91f,.86f), .30f)
        apply("melodic", MetalVector(.94f,.72f,.62f,.55f,.60f,.14f,.87f,.91f), .26f)
        apply("progressive", MetalVector(.76f,.58f,.69f,.45f,.95f,.22f,.76f,.62f), .27f)
        apply("death", MetalVector(.58f,.72f,.92f,.34f,.70f,.92f,.18f,.43f), .28f)
        apply("black", MetalVector(.55f,.77f,.87f,.54f,.62f,.88f,.22f,.37f), .25f)
        apply("folk", MetalVector(.82f,.62f,.60f,.63f,.55f,.20f,.78f,.77f), .22f)
        apply("heavy metal", MetalVector(.78f,.62f,.75f,.35f,.64f,.12f,.88f,.77f), .20f)
        apply("metalcore", MetalVector(.65f,.69f,.87f,.26f,.66f,.69f,.50f,.67f), .22f)
        apply("avant", MetalVector(.69f,.48f,.68f,.58f,.88f,.26f,.72f,.60f), .24f)
        return if (hits == 0) null else v
    }
}
