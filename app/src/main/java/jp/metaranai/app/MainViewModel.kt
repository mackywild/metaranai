package jp.metaranai.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LocalStore(app)
    private val engine = RecommendationEngine()
    private val spotify = SpotifyClient(app, store)
    private val externalDiscovery = ExternalDiscoveryClient(store)

    private val _profile = MutableStateFlow(store.loadProfile())
    val profile: StateFlow<MetalVector> = _profile
    private val _history = MutableStateFlow(store.loadHistory())
    val history: StateFlow<List<DiscoveryRecord>> = _history
    private val _searchHistory = MutableStateFlow(store.loadSearchHistory())
    val searchHistory: StateFlow<List<SearchRecord>> = _searchHistory
    private val _externalArtists = MutableStateFlow(store.loadExternalArtists())
    val externalArtists: StateFlow<List<MetalArtist>> = _externalArtists
    private val _recommendation = MutableStateFlow(engine.recommend(_profile.value, _history.value, _searchHistory.value, allArtists()))
    val recommendation: StateFlow<Recommendation> = _recommendation
    private val _spotifyStatus = MutableStateFlow(store.spotifySummary())
    val spotifyStatus: StateFlow<String> = _spotifyStatus
    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing
    private val _spotifySignals = MutableStateFlow<List<String>>(emptyList())
    val spotifySignals: StateFlow<List<String>> = _spotifySignals
    private val _discoveryStatus = MutableStateFlow(store.discoverySummary())
    val discoveryStatus: StateFlow<String> = _discoveryStatus
    private val _discovering = MutableStateFlow(false)
    val discovering: StateFlow<Boolean> = _discovering

    fun react(reaction: Reaction) {
        val rec = _recommendation.value
        if (_history.value.any { it.artistName == rec.artist.name && it.date == LocalDate.now().toString() }) return
        val record = DiscoveryRecord(rec.artist.name, LocalDate.now().toString(), reaction, rec.compatibility)
        _history.value = listOf(record) + _history.value
        _profile.value = engine.updatedProfile(_profile.value, rec.artist, reaction)
        persistAndRefresh()
    }

    fun shuffle() {
        _recommendation.value = engine.recommend(_profile.value, _history.value, _searchHistory.value, allArtists(), System.currentTimeMillis())
    }

    fun search(query: String): List<MetalArtist> {
        val q = query.trim()
        val catalog = allArtists()
        if (q.isBlank()) return catalog.sortedByDescending { it.discovery }.take(18)
        return catalog.filter {
            it.name.contains(q, true) || it.country.contains(q, true) || it.genres.any { g -> g.contains(q, true) }
        }.take(20)
    }

    fun recordSearch(query: String, artist: MetalArtist) {
        val r = SearchRecord(query.trim(), artist.name, LocalDateTime.now().withNano(0).toString())
        _searchHistory.value = listOf(r) + _searchHistory.value.filterNot { it.artistName == artist.name }.take(49)
        _profile.value = engine.profileFromInterest(_profile.value, artist)
        store.saveSearchHistory(_searchHistory.value)
        store.saveProfile(_profile.value)
        _recommendation.value = engine.recommend(_profile.value, _history.value, _searchHistory.value, allArtists())
    }

    fun clientId() = store.clientId()
    fun saveClientId(value: String) = store.saveClientId(value)
    fun lastFmApiKey() = store.lastFmApiKey()
    fun saveLastFmApiKey(value: String) = store.saveLastFmApiKey(value)
    fun externalCount() = _externalArtists.value.size
    fun dnaType() = engine.dnaType(_profile.value)

    fun stats(): DiscoveryStats {
        val h = _history.value
        val hits = h.count { it.reaction == Reaction.HIT }
        val dates = h.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet()
        var streak = 0
        var d = LocalDate.now()
        while (d in dates) { streak++; d = d.minusDays(1) }
        return DiscoveryStats(h.size, hits, if (h.isEmpty()) 0 else hits * 100 / h.size, streak)
    }

    fun syncExternalDiscovery() {
        if (_discovering.value) return
        val seeds = discoverySeeds()
        _discovering.value = true
        _discoveryStatus.value = "外部発掘中: ${seeds.joinToString(" / ")}"
        viewModelScope.launch {
            externalDiscovery.discover(seeds).onSuccess { result ->
                _externalArtists.value = result.artists
                val summary = "候補${result.fetched}件 → Metal+Hidden判定${result.accepted}件 / Cache ${result.cached}組"
                _discoveryStatus.value = summary
                store.saveDiscoverySummary(summary)
                _recommendation.value = engine.recommend(_profile.value, _history.value, _searchHistory.value, allArtists())
            }.onFailure {
                _discoveryStatus.value = "外部発掘失敗: ${it.message}"
            }
            _discovering.value = false
        }
    }

    private fun discoverySeeds(): List<String> {
        val hits = _history.value.filter { it.reaction == Reaction.HIT }.map { it.artistName }
        val searched = _searchHistory.value.map { it.artistName }
        val profileSeeds = MetalCatalog.artists.sortedByDescending { _profile.value.similarity(it.vector) }.map { it.name }
        return (hits + searched + profileSeeds).distinctBy { it.lowercase() }.take(5)
    }

    private fun allArtists(): List<MetalArtist> = (MetalCatalog.artists + _externalArtists.value)
        .distinctBy { it.name.lowercase() }

    fun syncSpotify() {
        if (_syncing.value) return
        _syncing.value = true
        viewModelScope.launch {
            val result = spotify.loginAndSync { _spotifyStatus.value = it }
            result.onSuccess { synced ->
                synced.inferredProfile?.let {
                    _profile.value = _profile.value.blend(it, .22f)
                    store.saveProfile(_profile.value)
                }
                _spotifySignals.value = buildList {
                    if (synced.matchedArtists.isNotEmpty()) add("一致: ${synced.matchedArtists.take(5).joinToString(" / ")}")
                    if (synced.genreSignals.isNotEmpty()) add("Genre: ${synced.genreSignals.take(5).joinToString(" / ")}")
                }
                _spotifyStatus.value = "同期完了: ${synced.summary}"
                _recommendation.value = engine.recommend(_profile.value, _history.value, _searchHistory.value, allArtists())
            }.onFailure {
                _spotifyStatus.value = "同期失敗: ${it.message}"
            }
            _syncing.value = false
        }
    }

    private fun persistAndRefresh() {
        store.saveHistory(_history.value)
        store.saveProfile(_profile.value)
        _recommendation.value = engine.recommend(_profile.value, _history.value, _searchHistory.value, allArtists())
    }
}
