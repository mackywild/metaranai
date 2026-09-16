package jp.metaranai.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.text.Normalizer

class MusicBrainzClient {
    private var lastRequestAt = 0L

    fun searchArtist(name: String): MusicBrainzArtist? {
        val arr = fetchArtists(name, 3) ?: return null
        var best: JSONObject? = null
        var bestScore = -1
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val score = o.optInt("score", 0) + if (o.optString("name").equals(name, true)) 20 else 0
            if (score > bestScore) { best = o; bestScore = score }
        }
        val o = best ?: return null
        if (bestScore < 70) return null
        return parseArtist(o, name, bestScore.coerceAtMost(100))
    }

    /**
     * V0.6.2 identity resolver for Spotify navigation.
     *
     * MusicBrainz search score or artist name alone is NOT enough. The result must have an
     * exact canonical artist name and at least one real discriminator from the Local Metal DB
     * (country / area / begin year). This lets us recover a trustworthy MBID for built-in
     * artists while still refusing ambiguous same-name artists.
     */
    fun resolveIdentityForSpotify(artist: MetalArtist): MusicBrainzIdentityMatch? {
        val arr = fetchArtists(artist.name, 10) ?: return null
        val targetName = canonicalName(artist.name)
        val exact = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                if (canonicalName(o.optString("name")) == targetName) add(o)
            }
        }
        if (exact.isEmpty()) return null

        val localCountries = countryCodes(artist.country)
        val localArea = canonicalGeo(artist.area)
        val localBeginYear = yearOf(artist.beginDate)
        val hasDiscriminator = localCountries.isNotEmpty() || localArea.isNotBlank() || localBeginYear != null
        if (!hasDiscriminator) return null

        data class Candidate(
            val json: JSONObject,
            val score: Int,
            val countryMatch: Boolean,
            val areaMatch: Boolean,
            val beginExact: Boolean,
            val reason: List<String>
        )

        val candidates = exact.map { o ->
            var score = 55 + (o.optInt("score", 0).coerceIn(0, 100) / 20) // max +5; name score is weak evidence only
            val reasons = mutableListOf("名前完全一致")

            val remoteCountry = o.optString("country").uppercase().trim()
            val countryMatch = remoteCountry.isNotBlank() && remoteCountry in localCountries
            if (countryMatch) {
                score += 25
                reasons += "国一致"
            }

            val remoteArea = canonicalGeo(o.optJSONObject("area")?.optString("name"))
            val areaMatch = localArea.isNotBlank() && remoteArea.isNotBlank() &&
                (localArea == remoteArea || localArea.contains(remoteArea) || remoteArea.contains(localArea))
            if (areaMatch) {
                score += 15
                reasons += "Area一致"
            }

            val remoteBeginYear = yearOf(o.optJSONObject("life-span")?.optString("begin"))
            val beginExact = localBeginYear != null && remoteBeginYear != null && localBeginYear == remoteBeginYear
            if (localBeginYear != null && remoteBeginYear != null) {
                val diff = kotlin.math.abs(localBeginYear - remoteBeginYear)
                when {
                    diff == 0 -> { score += 20; reasons += "開始年一致" }
                    diff <= 1 -> { score += 12; reasons += "開始年±1年" }
                    diff <= 3 -> { score += 6; reasons += "開始年近似" }
                }
            }

            Candidate(o, score.coerceAtMost(100), countryMatch, areaMatch, beginExact, reasons)
        }.sortedByDescending { it.score }

        val best = candidates.first()
        val second = candidates.getOrNull(1)
        val hasStrongDiscriminator = best.countryMatch || best.areaMatch || best.beginExact
        if (!hasStrongDiscriminator || best.score < 80) return null
        if (second != null && best.score - second.score < 15) return null

        val parsed = parseArtist(best.json, artist.name, best.score)
        return MusicBrainzIdentityMatch(parsed, best.score, best.reason.joinToString(" + "))
    }

    /** Resolve a MusicBrainz artist MBID to an explicit Spotify artist URL relation. */
    fun spotifyArtistRelation(mbid: String): Pair<String, String?>? {
        val safe = mbid.trim()
        if (safe.isBlank()) return null
        throttle()
        val url = URL("https://musicbrainz.org/ws/2/artist/${URLEncoder.encode(safe, "UTF-8")}?inc=url-rels&fmt=json")
        val c = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 10_000; readTimeout = 10_000
            setRequestProperty("User-Agent", "Metaranai-Android/0.6.2 (Spotify identity resolver)")
            setRequestProperty("Accept", "application/json")
        }
        val code = runCatching { c.responseCode }.getOrElse { return null }
        val body = runCatching {
            (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        }.getOrDefault("")
        if (code !in 200..299 || body.isBlank()) return null
        val relations = runCatching { JSONObject(body).optJSONArray("relations") }.getOrNull() ?: return null
        val pattern = Regex("https?://(?:www\\.)?open\\.spotify\\.com/artist/([A-Za-z0-9]+)")
        for (i in 0 until relations.length()) {
            val resource = relations.optJSONObject(i)?.optJSONObject("url")?.optString("resource").orEmpty()
            val id = pattern.find(resource)?.groupValues?.getOrNull(1) ?: continue
            return "https://open.spotify.com/artist/$id" to id
        }
        return null
    }

    private fun fetchArtists(name: String, limit: Int): JSONArray? {
        throttle()
        val q = URLEncoder.encode("artist:\"$name\"", "UTF-8")
        val url = URL("https://musicbrainz.org/ws/2/artist/?query=$q&fmt=json&limit=$limit")
        val c = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 10_000; readTimeout = 10_000
            setRequestProperty("User-Agent", "Metaranai-Android/0.6.2 (music discovery prototype)")
            setRequestProperty("Accept", "application/json")
        }
        val code = runCatching { c.responseCode }.getOrElse { return null }
        val body = runCatching {
            (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        }.getOrDefault("")
        if (code !in 200..299 || body.isBlank()) return null
        return runCatching { JSONObject(body).optJSONArray("artists") }.getOrNull()
    }

    private fun parseArtist(o: JSONObject, fallbackName: String, score: Int): MusicBrainzArtist {
        val life = o.optJSONObject("life-span")
        val area = o.optJSONObject("area")?.optString("name")?.takeIf { it.isNotBlank() }
        return MusicBrainzArtist(
            mbid = o.optString("id").takeIf { it.isNotBlank() },
            name = o.optString("name", fallbackName),
            country = o.optString("country").takeIf { it.isNotBlank() },
            area = area,
            beginDate = life?.optString("begin")?.takeIf { it.isNotBlank() },
            endDate = life?.optString("end")?.takeIf { it.isNotBlank() },
            ended = if (life?.has("ended") == true) life.optBoolean("ended") else null,
            matchScore = score.coerceAtMost(100)
        )
    }

    private fun canonicalName(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFKC)
        .trim()
        .lowercase()
        .replace(Regex("\\s+"), " ")

    private fun canonicalGeo(value: String?): String = Normalizer
        .normalize(value.orEmpty(), Normalizer.Form.NFKC)
        .trim()
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "")

    private fun yearOf(value: String?): Int? = Regex("(19|20)\\d{2}")
        .find(value.orEmpty())?.value?.toIntOrNull()

    private fun countryCodes(value: String?): Set<String> {
        val aliases = mapOf(
            "japan" to "JP", "jp" to "JP",
            "usa" to "US", "us" to "US", "unitedstates" to "US", "unitedstatesofamerica" to "US",
            "unitedkingdom" to "GB", "uk" to "GB", "greatbritain" to "GB", "england" to "GB",
            "sweden" to "SE", "finland" to "FI", "norway" to "NO", "denmark" to "DK",
            "germany" to "DE", "france" to "FR", "italy" to "IT", "austria" to "AT",
            "poland" to "PL", "canada" to "CA", "spain" to "ES", "portugal" to "PT",
            "netherlands" to "NL", "belgium" to "BE", "switzerland" to "CH", "greece" to "GR",
            "czechia" to "CZ", "czechrepublic" to "CZ", "hungary" to "HU", "brazil" to "BR",
            "australia" to "AU", "newzealand" to "NZ", "mexico" to "MX", "argentina" to "AR"
        )
        return value.orEmpty()
            .split('/', ',', ';', '|')
            .map { canonicalGeo(it) }
            .mapNotNull { token -> aliases[token] ?: token.uppercase().takeIf { it.length == 2 } }
            .toSet()
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

data class MusicBrainzIdentityMatch(
    val artist: MusicBrainzArtist,
    val confidence: Int,
    val reason: String
)
