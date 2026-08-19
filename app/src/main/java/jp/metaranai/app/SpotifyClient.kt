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
