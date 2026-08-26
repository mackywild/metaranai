package jp.metaranai.app

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LocalStore(app)
    private val engine = RecommendationEngine()
    private val spotify = SpotifyClient(app, store)
    private val externalDiscovery = ExternalDiscoveryClient(store)

    private val _profile = MutableStateFlow(store.loadProfile())
    val profile: StateFlow<MetalVector> = _profile
    private val _vocalProfile = MutableStateFlow(store.loadVocalProfile())
    val vocalProfile: StateFlow<VocalProfile> = _vocalProfile
    private val _genreLens = MutableStateFlow(store.loadGenreLens())
    val genreLens: StateFlow<GenreLensConfig> = _genreLens
    private val _history = MutableStateFlow(store.loadHistory())
    val history: StateFlow<List<DiscoveryRecord>> = _history
    private val _searchHistory = MutableStateFlow(store.loadSearchHistory())
    val searchHistory: StateFlow<List<SearchRecord>> = _searchHistory
    private val _externalArtists = MutableStateFlow(store.loadExternalArtists())
    val externalArtists: StateFlow<List<MetalArtist>> = _externalArtists
    private val _recommendation = MutableStateFlow(recommendNow())
    val recommendation: StateFlow<Recommendation> = _recommendation

    private val _spotifyStatus = MutableStateFlow(store.spotifySummary())
    val spotifyStatus: StateFlow<String> = _spotifyStatus
    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing
    private val _spotifySignals = MutableStateFlow<List<String>>(emptyList())
    val spotifySignals: StateFlow<List<String>> = _spotifySignals
    private val _spotifyOpenStatus = MutableStateFlow("")
    val spotifyOpenStatus: StateFlow<String> = _spotifyOpenStatus

    private val _discoveryStatus = MutableStateFlow(store.discoverySummary())
    val discoveryStatus: StateFlow<String> = _discoveryStatus
    private val _discovering = MutableStateFlow(false)
    val discovering: StateFlow<Boolean> = _discovering

    private val _remoteSearchResults = MutableStateFlow<List<MetalArtist>>(emptyList())
    val remoteSearchResults: StateFlow<List<MetalArtist>> = _remoteSearchResults
    private val _remoteSearching = MutableStateFlow(false)
    val remoteSearching: StateFlow<Boolean> = _remoteSearching
    private val _remoteSearchStatus = MutableStateFlow("")
    val remoteSearchStatus: StateFlow<String> = _remoteSearchStatus

    private val _backupStatus = MutableStateFlow("")
    val backupStatus: StateFlow<String> = _backupStatus

    private var lensRefreshJob: Job? = null

    fun react(reaction: Reaction) {
        val rec = _recommendation.value
        if (_history.value.any { it.artistName == rec.artist.name && it.date == LocalDate.now().toString() }) return
        val record = DiscoveryRecord(rec.artist.name, LocalDate.now().toString(), reaction, rec.compatibility)
        _history.value = listOf(record) + _history.value
        _profile.value = engine.updatedProfile(_profile.value, rec.artist, reaction)
        _vocalProfile.value = VocalAnalyzer.update(_vocalProfile.value, rec.artist.vocalType, reaction)
        persistAndRefresh()
    }

    fun shuffle() {
        _recommendation.value = recommendNow(System.currentTimeMillis())
    }

    fun search(query: String): List<MetalArtist> {
        val q = query.trim()
        val catalog = allArtists()
        if (q.isBlank()) return catalog.sortedWith(compareByDescending<MetalArtist> { it.hiddenScore }.thenByDescending { it.discovery }).take(24)
        return catalog.filter {
            it.name.contains(q, true) || it.country.contains(q, true) || it.genres.any { g -> g.contains(q, true) }
        }.sortedWith(compareByDescending<MetalArtist> { it.name.startsWith(q, true) }.thenByDescending { it.hiddenScore }).take(40)
    }

    fun searchExternal(query: String) {
        val q = query.trim()
        if (_remoteSearching.value) return
        if (q.length < 2) {
            _remoteSearchStatus.value = "2文字以上入力してください"
            return
        }
        _remoteSearching.value = true
        _remoteSearchStatus.value = "Last.fm / MusicBrainzから「$q」を探索中…"
        viewModelScope.launch {
            externalDiscovery.searchArtists(q).onSuccess { result ->
                _externalArtists.value = result.cachedArtists
                _remoteSearchResults.value = result.results
                _remoteSearchStatus.value = "外部${result.fetched}件 → Metal判定${result.accepted}件 / Local DB ${result.cachedArtists.size}組"
                _recommendation.value = recommendNow()
            }.onFailure {
                _remoteSearchResults.value = emptyList()
                _remoteSearchStatus.value = "外部検索失敗: ${it.message}"
            }
            _remoteSearching.value = false
        }
    }

    fun clearRemoteSearch() {
        _remoteSearchResults.value = emptyList()
        _remoteSearchStatus.value = ""
    }

    fun recordSearch(query: String, artist: MetalArtist) {
        val r = SearchRecord(query.trim(), artist.name, LocalDateTime.now().withNano(0).toString())
        _searchHistory.value = listOf(r) + _searchHistory.value.filterNot { it.artistName == artist.name }
        _profile.value = engine.profileFromInterest(_profile.value, artist)
        store.saveSearchHistory(_searchHistory.value)
        store.saveProfile(_profile.value)
        _recommendation.value = recommendNow()
    }

    fun openSpotifyArtist(artist: MetalArtist) {
        _spotifyOpenStatus.value = "Spotify上の${artist.name}を照合中…"
        viewModelScope.launch {
            val destination = spotify.resolveArtistDestination(artist.name)
            _spotifyOpenStatus.value = if (destination.direct) "Spotifyアーティストページへ移動" else "完全一致なし: Spotify検索へ移動"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(destination.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            getApplication<Application>().startActivity(intent)
        }
    }

    fun clientId() = store.clientId()
    fun saveClientId(value: String) = store.saveClientId(value)
    fun lastFmApiKey() = store.lastFmApiKey()
    fun saveLastFmApiKey(value: String) = store.saveLastFmApiKey(value)
    fun externalCount() = _externalArtists.value.size
    fun dnaType() = engine.dnaType(_profile.value, _vocalProfile.value, _history.value)
    fun activeGenres(): List<String> = GenreLensCatalog.activeGenres(_genreLens.value)

    fun setGenreLensMode(mode: GenreLensMode) {
        _genreLens.value = _genreLens.value.copy(mode = mode)
        saveLensAndRefresh()
    }

    fun toggleManualGenre(name: String) {
        val current = _genreLens.value.manualGenres.toMutableSet()
        if (!current.add(name)) current.remove(name)
        _genreLens.value = _genreLens.value.copy(manualGenres = current)
        saveLensAndRefresh()
    }

    fun toggleWeekdayGenre(day: DayOfWeek, name: String) {
        val map = _genreLens.value.weekdayGenres.toMutableMap()
        val current = map[day.name].orEmpty().toMutableSet()
        if (!current.add(name)) current.remove(name)
        map[day.name] = current
        _genreLens.value = _genreLens.value.copy(weekdayGenres = map)
        saveLensAndRefresh()
    }

    fun topGenres(limit: Int = 8): List<Pair<String, Int>> {
        val byName = allArtists().associateBy { it.name.lowercase() }
        val weights = linkedMapOf<String, Float>()
        _history.value.forEach { r ->
            val a = byName[r.artistName.lowercase()] ?: return@forEach
            a.genres.forEach { g -> weights[g] = (weights[g] ?: 0f) + r.reaction.genreWeight }
        }
        val positive = weights.filterValues { it > 0f }
        val max = positive.values.maxOrNull()?.coerceAtLeast(.01f) ?: return emptyList()
        return positive.entries.sortedByDescending { it.value }.take(limit)
            .map { it.key to ((it.value / max) * 100).toInt().coerceIn(0,100) }
    }

    fun stats(): DiscoveryStats {
        val h = _history.value
        val favorites = h.count { it.reaction == Reaction.LOVE_ALL }
        val positives = h.count { it.reaction.isPositive }
        val average = if (h.isEmpty()) 0 else h.sumOf { it.reaction.affinityScore } / h.size
        val dates = h.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet()
        var streak = 0
        var d = LocalDate.now()
        while (d in dates) { streak++; d = d.minusDays(1) }
        return DiscoveryStats(
            total = h.size,
            favorites = favorites,
            positives = positives,
            positiveRate = if (h.isEmpty()) 0 else positives * 100 / h.size,
            averageAffinity = average,
            streakDays = streak
        )
    }

    fun syncExternalDiscovery() {
        if (_discovering.value) return
        val seeds = discoverySeeds()
        val genres = activeGenres()
        _discovering.value = true
        _discoveryStatus.value = "外部発掘中: ${(seeds + genres).joinToString(" / ")}"
        viewModelScope.launch {
            externalDiscovery.discover(seeds, genres).onSuccess { result ->
                _externalArtists.value = result.artists
                val summary = "候補${result.fetched}件 → Metal+Hidden判定${result.accepted}件 / Local DB ${result.cached}組"
                _discoveryStatus.value = summary
                store.saveDiscoverySummary(summary)
                _recommendation.value = recommendNow()
            }.onFailure {
                _discoveryStatus.value = "外部発掘失敗: ${it.message}"
            }
            _discovering.value = false
        }
    }

    private fun discoverySeeds(): List<String> {
        val strong = _history.value.filter { it.reaction.isStrongPositive }.map { it.artistName }
        val partial = _history.value.filter { it.reaction == Reaction.SOME }.map { it.artistName }
        val searched = _searchHistory.value.map { it.artistName }
        val profileSeeds = MetalCatalog.artists.sortedByDescending { _profile.value.similarity(it.vector) }.map { it.name }
        return (strong + partial + searched + profileSeeds).distinctBy { it.lowercase() }.take(6)
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
                _recommendation.value = recommendNow()
            }.onFailure {
                _spotifyStatus.value = "同期失敗: ${it.message}"
            }
            _syncing.value = false
        }
    }

    fun exportBackupJson(): String = store.exportBackupJson()

    fun importBackupJson(raw: String) {
        store.importBackupJson(raw).onSuccess {
            reloadFromStore()
            _backupStatus.value = "バックアップを復元しました"
        }.onFailure {
            _backupStatus.value = "復元失敗: ${it.message}"
        }
    }

    fun setBackupStatus(value: String) { _backupStatus.value = value }

    private fun reloadFromStore() {
        _profile.value = store.loadProfile()
        _vocalProfile.value = store.loadVocalProfile()
        _genreLens.value = store.loadGenreLens()
        _history.value = store.loadHistory()
        _searchHistory.value = store.loadSearchHistory()
        _externalArtists.value = store.loadExternalArtists()
        _spotifyStatus.value = store.spotifySummary()
        _discoveryStatus.value = store.discoverySummary()
        _recommendation.value = recommendNow()
    }

    /**
     * Genre Lens changes update TODAY immediately, then expand that genre's external pool after a short debounce.
     */
    private fun saveLensAndRefresh() {
        store.saveGenreLens(_genreLens.value)
        _recommendation.value = recommendAfterLensChange()

        lensRefreshJob?.cancel()
        val genres = activeGenres()
        if (genres.isEmpty() || store.lastFmApiKey().isBlank()) return
        lensRefreshJob = viewModelScope.launch {
            delay(550)
            // Start the normal discovery job only after the user's rapid toggle sequence has settled.
            // syncExternalDiscovery owns its lifecycle and always clears the discovering flag.
            syncExternalDiscovery()
        }
    }

    private fun recommendationSeed(): Long {
        val lensHash = activeGenres().sorted().joinToString("|").hashCode().toLong()
        return LocalDate.now().toEpochDay() * 31L + lensHash
    }

    private fun recommendAfterLensChange(): Recommendation {
        val current = runCatching { _recommendation.value.artist.name }.getOrNull()
        val all = allArtists()
        val candidates = if (current != null && all.size > 1) all.filterNot { it.name.equals(current, true) } else all
        return engine.recommend(
            profile = _profile.value,
            history = _history.value,
            searchHistory = _searchHistory.value,
            candidates = candidates,
            genreLens = activeGenres(),
            seed = recommendationSeed()
        )
    }

    private fun recommendNow(seed: Long = recommendationSeed()): Recommendation = engine.recommend(
        profile = _profile.value,
        history = _history.value,
        searchHistory = _searchHistory.value,
        candidates = allArtists(),
        genreLens = activeGenres(),
        seed = seed
    )

    private fun persistAndRefresh() {
        store.saveHistory(_history.value)
        store.saveProfile(_profile.value)
        store.saveVocalProfile(_vocalProfile.value)
        _recommendation.value = recommendNow(System.currentTimeMillis())
    }
}
