package jp.metaranai.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class LocalStore(context: Context) {
    // Keep the same file and legacy keys. V0.4/V0.5 data must survive an in-place update.
    private val prefs = context.getSharedPreferences("metaranai", Context.MODE_PRIVATE)

    val defaultProfile = MetalVector(.93f,.80f,.55f,.84f,.57f,.12f,.94f,.92f)

    fun loadProfile(): MetalVector {
        val raw = prefs.getString("profile", null) ?: return defaultProfile
        return runCatching {
            val o = JSONObject(raw)
            MetalVector(
                o.getDouble("melody").toFloat(), o.getDouble("speed").toFloat(),
                o.getDouble("heavy").toFloat(), o.getDouble("symphonic").toFloat(),
                o.getDouble("technical").toFloat(), o.getDouble("growl").toFloat(),
                o.getDouble("cleanVocal").toFloat(), o.getDouble("catchy").toFloat()
            )
        }.getOrDefault(defaultProfile)
    }

    fun saveProfile(v: MetalVector) {
        prefs.edit().putString("profile", JSONObject().apply {
            put("melody",v.melody); put("speed",v.speed); put("heavy",v.heavy); put("symphonic",v.symphonic)
            put("technical",v.technical); put("growl",v.growl); put("cleanVocal",v.cleanVocal); put("catchy",v.catchy)
        }.toString()).apply()
    }

    fun loadHistory(): List<DiscoveryRecord> {
        val raw = prefs.getString("history", "[]") ?: "[]"
        return runCatching {
            val a = JSONArray(raw)
            (0 until a.length()).mapNotNull { i ->
                val o = a.optJSONObject(i) ?: return@mapNotNull null
                val reaction = parseReaction(o.optString("reaction")) ?: return@mapNotNull null
                DiscoveryRecord(
                    artistName = o.optString("artist"),
                    date = o.optString("date"),
                    reaction = reaction,
                    score = o.optInt("score", 0)
                )
            }
        }.getOrDefault(emptyList())
    }

    /**
     * V0.5.2 compatibility:
     * old HIT remains HIT, old MAYBE becomes SOME, old MISS becomes MEH.
     * We never upgrade a legacy HIT to LOVE_ALL or downgrade a legacy MISS to NO_INTEREST.
     */
    private fun parseReaction(raw: String): Reaction? = when (raw.uppercase()) {
        "LOVE_ALL" -> Reaction.LOVE_ALL
        "HIT" -> Reaction.HIT
        "SOME", "MAYBE" -> Reaction.SOME
        "MEH", "MISS" -> Reaction.MEH
        "NO_INTEREST" -> Reaction.NO_INTEREST
        else -> null
    }

    fun saveHistory(records: List<DiscoveryRecord>) {
        val a = JSONArray()
        records.forEach { r -> a.put(JSONObject().apply {
            put("artist", r.artistName); put("date", r.date); put("reaction", r.reaction.name); put("score",r.score)
        }) }
        prefs.edit().putString("history", a.toString()).apply()
    }

    fun loadSearchHistory(): List<SearchRecord> {
        val raw = prefs.getString("search_history", "[]") ?: "[]"
        return runCatching {
            val a = JSONArray(raw)
            (0 until a.length()).map { i -> a.getJSONObject(i) }.map {
                SearchRecord(it.getString("query"), it.getString("artist"), it.getString("dateTime"))
            }
        }.getOrDefault(emptyList())
    }

    fun saveSearchHistory(records: List<SearchRecord>) {
        val a = JSONArray()
        // V0.5.2: search history is no longer truncated. The local archive is an asset.
        records.forEach { r -> a.put(JSONObject().apply {
            put("query", r.query); put("artist", r.artistName); put("dateTime", r.dateTime)
        }) }
        prefs.edit().putString("search_history", a.toString()).apply()
    }

    fun clientId(): String = prefs.getString("spotify_client_id", "") ?: ""
    fun saveClientId(value: String) = prefs.edit().putString("spotify_client_id", value.trim()).apply()
    fun token(): String = prefs.getString("spotify_access_token", "") ?: ""
    fun saveToken(value: String) = prefs.edit().putString("spotify_access_token", value).apply()
    fun refreshToken(): String = prefs.getString("spotify_refresh_token", "") ?: ""
    fun saveRefreshToken(value: String) = prefs.edit().putString("spotify_refresh_token", value).apply()
    fun tokenExpiry(): Long = prefs.getLong("spotify_token_expiry", 0L)
    fun saveTokenExpiry(value: Long) = prefs.edit().putLong("spotify_token_expiry", value).apply()
    fun saveSpotifySummary(value: String) = prefs.edit().putString("spotify_summary", value).apply()
    fun spotifySummary(): String = prefs.getString("spotify_summary", "未同期") ?: "未同期"

    fun lastFmApiKey(): String = prefs.getString("lastfm_api_key", "") ?: ""
    fun saveLastFmApiKey(value: String) = prefs.edit().putString("lastfm_api_key", value.trim()).apply()
    fun saveDiscoverySummary(value: String) = prefs.edit().putString("discovery_summary", value).apply()
    fun discoverySummary(): String = prefs.getString("discovery_summary", "未同期") ?: "未同期"

    fun loadExternalArtists(): List<MetalArtist> {
        val raw = prefs.getString("external_artists", "[]") ?: "[]"
        return runCatching {
            val a = JSONArray(raw)
            (0 until a.length()).mapNotNull { i ->
                val o = a.optJSONObject(i) ?: return@mapNotNull null
                val v = o.optJSONObject("vector") ?: return@mapNotNull null
                MetalArtist(
                    name = o.getString("name"),
                    country = o.optString("country", "External"),
                    genres = buildList { val g=o.optJSONArray("genres"); if(g!=null) for(x in 0 until g.length()) add(g.optString(x)) },
                    vector = MetalVector(
                        v.optDouble("melody",.5).toFloat(), v.optDouble("speed",.5).toFloat(),
                        v.optDouble("heavy",.5).toFloat(), v.optDouble("symphonic",.5).toFloat(),
                        v.optDouble("technical",.5).toFloat(), v.optDouble("growl",.5).toFloat(),
                        v.optDouble("cleanVocal",.5).toFloat(), v.optDouble("catchy",.5).toFloat()
                    ),
                    discovery = o.optDouble("discovery", .8).toFloat(),
                    reason = o.optString("reason"),
                    source = runCatching { ArtistSource.valueOf(o.optString("source", "LASTFM")) }.getOrDefault(ArtistSource.LASTFM),
                    sourceSeed = o.optString("sourceSeed").takeIf { it.isNotBlank() },
                    externalScore = if (o.has("externalScore")) o.optInt("externalScore") else null,
                    lastFmListeners = if (o.has("lastFmListeners")) o.optLong("lastFmListeners") else null,
                    lastFmPlaycount = if (o.has("lastFmPlaycount")) o.optLong("lastFmPlaycount") else null,
                    mbid = o.optString("mbid").takeIf { it.isNotBlank() },
                    area = o.optString("area").takeIf { it.isNotBlank() },
                    beginDate = o.optString("beginDate").takeIf { it.isNotBlank() },
                    endDate = o.optString("endDate").takeIf { it.isNotBlank() },
                    ended = if (o.has("ended")) o.optBoolean("ended") else null,
                    hiddenScore = o.optInt("hiddenScore", 50),
                    metadataConfidence = o.optInt("metadataConfidence", 0),
                    vocalType = runCatching { VocalType.valueOf(o.optString("vocalType", "UNKNOWN")) }.getOrDefault(VocalType.UNKNOWN)
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveExternalArtists(artists: List<MetalArtist>) {
        val a = JSONArray()
        // V0.5.2: no artificial 500-artist ceiling. Every discovered/queried metal artist is retained.
        artists.forEach { artist ->
            a.put(JSONObject().apply {
                put("name", artist.name); put("country", artist.country); put("discovery", artist.discovery); put("reason", artist.reason)
                put("source", artist.source.name); put("sourceSeed", artist.sourceSeed ?: ""); artist.externalScore?.let { put("externalScore", it) }
                artist.lastFmListeners?.let { put("lastFmListeners", it) }; artist.lastFmPlaycount?.let { put("lastFmPlaycount", it) }
                put("mbid", artist.mbid ?: ""); put("area", artist.area ?: ""); put("beginDate", artist.beginDate ?: ""); put("endDate", artist.endDate ?: "")
                artist.ended?.let { put("ended", it) }; put("hiddenScore", artist.hiddenScore); put("metadataConfidence", artist.metadataConfidence)
                put("vocalType", artist.vocalType.name)
                put("genres", JSONArray().apply { artist.genres.forEach { put(it) } })
                put("vector", JSONObject().apply {
                    put("melody",artist.vector.melody); put("speed",artist.vector.speed); put("heavy",artist.vector.heavy)
                    put("symphonic",artist.vector.symphonic); put("technical",artist.vector.technical); put("growl",artist.vector.growl)
                    put("cleanVocal",artist.vector.cleanVocal); put("catchy",artist.vector.catchy)
                })
            })
        }
        prefs.edit().putString("external_artists", a.toString()).apply()
    }

    // ----- V0.5+ additive keys. Legacy keys above are never renamed/cleared. -----

    fun loadVocalProfile(): VocalProfile {
        val raw = prefs.getString("vocal_profile_v05", null) ?: return VocalProfile()
        return runCatching {
            val o = JSONObject(raw)
            VocalProfile(
                o.optDouble("male", .333).toFloat(),
                o.optDouble("female", .333).toFloat(),
                o.optDouble("mixed", .334).toFloat(),
                o.optInt("observations", 0)
            ).normalized().copy(observations = o.optInt("observations", 0))
        }.getOrDefault(VocalProfile())
    }

    fun saveVocalProfile(v: VocalProfile) {
        prefs.edit().putString("vocal_profile_v05", JSONObject().apply {
            put("male", v.male); put("female", v.female); put("mixed", v.mixed); put("observations", v.observations)
        }.toString()).apply()
    }

    fun loadGenreLens(): GenreLensConfig {
        val raw = prefs.getString("genre_lens_v05", null) ?: return GenreLensConfig()
        return runCatching {
            val o = JSONObject(raw)
            val manual = mutableSetOf<String>()
            o.optJSONArray("manual")?.let { a -> for (i in 0 until a.length()) a.optString(i).takeIf { it.isNotBlank() }?.let(manual::add) }
            val weekdays = mutableMapOf<String, Set<String>>()
            o.optJSONObject("weekdays")?.let { w ->
                w.keys().forEach { day ->
                    val set = mutableSetOf<String>()
                    w.optJSONArray(day)?.let { a -> for (i in 0 until a.length()) a.optString(i).takeIf { it.isNotBlank() }?.let(set::add) }
                    weekdays[day] = set
                }
            }
            GenreLensConfig(
                mode = runCatching { GenreLensMode.valueOf(o.optString("mode", "OFF")) }.getOrDefault(GenreLensMode.OFF),
                manualGenres = manual,
                weekdayGenres = weekdays
            )
        }.getOrDefault(GenreLensConfig())
    }

    fun saveGenreLens(config: GenreLensConfig) {
        val o = JSONObject().apply {
            put("mode", config.mode.name)
            put("manual", JSONArray().apply { config.manualGenres.sorted().forEach { put(it) } })
            put("weekdays", JSONObject().apply {
                config.weekdayGenres.forEach { (day, genres) -> put(day, JSONArray().apply { genres.sorted().forEach { put(it) } }) }
            })
        }
        prefs.edit().putString("genre_lens_v05", o.toString()).apply()
    }

    fun spotifyArtistLink(name: String): String? {
        val raw = prefs.getString("spotify_artist_links_v05", "{}") ?: "{}"
        return runCatching { JSONObject(raw).optString(name.lowercase()).takeIf { it.isNotBlank() } }.getOrNull()
    }

    fun saveSpotifyArtistLink(name: String, url: String) {
        val raw = prefs.getString("spotify_artist_links_v05", "{}") ?: "{}"
        val o = runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
        o.put(name.lowercase(), url)
        prefs.edit().putString("spotify_artist_links_v05", o.toString()).apply()
    }

    fun exportBackupJson(): String {
        val out = JSONObject()
        out.put("format", "metaranai-backup")
        out.put("version", 52)
        out.put("preferences", JSONObject().apply {
            prefs.all.forEach { (key, value) ->
                when (value) {
                    is String, is Boolean, is Int, is Long, is Float -> put(key, value)
                    is Set<*> -> put(key, JSONArray(value.toList()))
                }
            }
        })
        return out.toString(2)
    }

    fun importBackupJson(raw: String): Result<Unit> = runCatching {
        val root = JSONObject(raw)
        require(root.optString("format") == "metaranai-backup") { "メタらない？のバックアップではありません" }
        val o = root.getJSONObject("preferences")
        val e = prefs.edit()
        o.keys().forEach { key ->
            when (val value = o.get(key)) {
                is String -> e.putString(key, value)
                is Boolean -> e.putBoolean(key, value)
                is Int -> e.putInt(key, value)
                is Long -> e.putLong(key, value)
                is Double -> e.putFloat(key, value.toFloat())
                is JSONArray -> e.putStringSet(key, (0 until value.length()).map { value.optString(it) }.toSet())
            }
        }
        e.apply()
    }
}
