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
    // V0.6.0: keep V0.5.4 unrated refill; add Personal Metal Archive / Deep Dive / media routes.
    private val minimumUnratedLensPoolPerGenre = 10
    private val refillTargetUnratedLensPoolPerGenre = 20
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

    private val _reactionStatus = MutableStateFlow("")
    val reactionStatus: StateFlow<String> = _reactionStatus

    private val _deepDiveResults = MutableStateFlow<List<MetalArtist>>(emptyList())
    val deepDiveResults: StateFlow<List<MetalArtist>> = _deepDiveResults
    private val _deepDiveStatus = MutableStateFlow("")
    val deepDiveStatus: StateFlow<String> = _deepDiveStatus
    private val _deepDiving = MutableStateFlow(false)
    val deepDiving: StateFlow<Boolean> = _deepDiving
    private val _mediaOpenStatus = MutableStateFlow("")
    val mediaOpenStatus: StateFlow<String> = _mediaOpenStatus

    private val _genreLensPreparing = MutableStateFlow(false)
    val genreLensPreparing: StateFlow<Boolean> = _genreLensPreparing
    private val _genreLensReady = MutableStateFlow(initialLensReady())
    val genreLensReady: StateFlow<Boolean> = _genreLensReady
    private val _genreLensStatus = MutableStateFlow(initialLensStatus())
    val genreLensStatus: StateFlow<String> = _genreLensStatus

    private var lensRefreshJob: Job? = null
    private var reactionFeedbackJob: Job? = null

    init {
        val genres = activeGenres()
        if (genres.isNotEmpty() && !currentLensPoolSatisfied()) {
            _genreLensReady.value = false
            _genreLensPreparing.value = true
            _genreLensStatus.value = currentLensStatus()
            if (store.lastFmApiKey().isNotBlank()) {
                viewModelScope.launch {
                    delay(250)
                    ensureGenreLensPool()
                }
            } else {
                _genreLensPreparing.value = false
                _genreLensReady.value = lensUnratedCandidates(genres).isNotEmpty()
                _genreLensStatus.value += " / Last.fm API Keyを設定すると未評価候補を自動補充できます"
            }
        }
    }

    fun react(reaction: Reaction) {
        val rec = _recommendation.value
        val today = LocalDate.now().toString()
        if (_history.value.any { it.artistName.equals(rec.artist.name, true) && it.date == today }) {
            showReactionStatus("${rec.artist.name} は今日すでに評価済み。未評価候補を探します")
            refreshAfterReaction()
            return
        }
        val record = DiscoveryRecord(rec.artist.name, today, reaction, rec.compatibility)
        _history.value = listOf(record) + _history.value
        _profile.value = engine.updatedProfile(_profile.value, rec.artist, reaction)
        _vocalProfile.value = VocalAnalyzer.update(_vocalProfile.value, rec.artist.vocalType, reaction)
        showReactionStatus("${reaction.label} を記録しました")
        persistAndRefresh()
        refreshAfterReaction()
    }

    fun shuffle() {
        if (activeGenres().isNotEmpty() && !_genreLensReady.value) return
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

    fun openYouTube(artist: MetalArtist, mode: String = "artist") {
        val suffix = when (mode) {
            "mv" -> " official music video"
            "live" -> " live"
            else -> " official"
        }
        val query = Uri.encode(artist.name + suffix)
        val url = "https://www.youtube.com/results?search_query=$query"
        _mediaOpenStatus.value = when (mode) {
            "mv" -> "${artist.name} のMVをYouTubeで探索"
            "live" -> "${artist.name} のLiveをYouTubeで探索"
            else -> "${artist.name} をYouTubeで探索"
        }
        openExternalUrl(url)
    }

    fun openLastFm(artist: MetalArtist) {
        _mediaOpenStatus.value = "${artist.name} のLast.fmへ移動"
        openExternalUrl("https://www.last.fm/music/${Uri.encode(artist.name)}")
    }

    private fun openExternalUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    /** V0.6.0: seed one artist and dig its Last.fm similarity neighborhood. */
    fun deepDive(artist: MetalArtist) {
        if (_deepDiving.value) return
        if (store.lastFmApiKey().isBlank()) {
            _deepDiveStatus.value = "Deep DiveにはLast.fm API Keyが必要です"
            return
        }
        _deepDiving.value = true
        _deepDiveResults.value = emptyList()
        _deepDiveStatus.value = "${artist.name} から地下を掘削中…"
        val rated = ratedArtistNames()
        viewModelScope.launch {
            externalDiscovery.discover(listOf(artist.name), limitPerSeed = 18).onSuccess { result ->
                _externalArtists.value = result.artists
                val direct = result.artists.filter { candidate ->
                    !candidate.name.equals(artist.name, true) &&
                        candidate.name.trim().lowercase() !in rated &&
                        candidate.sourceSeed?.equals(artist.name, true) == true
                }
                val candidates = direct.ifEmpty {
                    result.artists.filter { candidate ->
                        !candidate.name.equals(artist.name, true) && candidate.name.trim().lowercase() !in rated
                    }
                }.sortedByDescending { candidate ->
                    _profile.value.similarity(candidate.vector) * .68f +
                        candidate.hiddenScore.coerceIn(0, 100) / 100f * .22f +
                        candidate.discovery * .10f
                }.take(12)
                _deepDiveResults.value = candidates
                _deepDiveStatus.value = if (candidates.isEmpty()) {
                    "${artist.name} から新しいMetal候補を取得できませんでした"
                } else {
                    "${artist.name} から ${candidates.size}組を発掘 / Local DB ${result.cached}組"
                }
                _recommendation.value = recommendNow()
            }.onFailure {
                _deepDiveStatus.value = "Deep Dive失敗: ${it.message}"
            }
            _deepDiving.value = false
        }
    }

    fun clearDeepDive() {
        _deepDiveResults.value = emptyList()
        _deepDiveStatus.value = ""
    }

    fun clientId() = store.clientId()
    fun saveClientId(value: String) = store.saveClientId(value)
    fun lastFmApiKey() = store.lastFmApiKey()
    fun saveLastFmApiKey(value: String) = store.saveLastFmApiKey(value)
    fun externalCount() = _externalArtists.value.size
    fun dnaType() = engine.dnaType(_profile.value, _vocalProfile.value, _history.value)
    fun activeGenres(): List<String> = GenreLensCatalog.activeGenres(_genreLens.value)

    /** V0.6.0 Personal Metal Archive. Built-ins and discovered artists share one deduplicated view. */
    fun archiveArtists(): List<MetalArtist> = allArtists().sortedWith(
        compareByDescending<MetalArtist> { reactionFor(it.name)?.reaction?.affinityScore ?: -1 }
            .thenByDescending { it.hiddenScore }
            .thenByDescending { it.discovery }
            .thenBy { it.name.lowercase() }
    )

    fun reactionFor(artistName: String): DiscoveryRecord? =
        _history.value.firstOrNull { it.artistName.equals(artistName, true) }

    fun spotifyLinkCached(artistName: String): Boolean = store.spotifyArtistLink(artistName) != null

    fun archiveGenreCounts(): List<Pair<String, Int>> = GenreLensCatalog.names()
        .map { it to GenreLensCatalog.filter(allArtists(), listOf(it)).size }
        .filter { it.second > 0 }
        .sortedByDescending { it.second }

    fun whyThisArtist(rec: Recommendation = _recommendation.value): List<String> = buildList {
        if (rec.activeGenres.isNotEmpty()) {
            add("Genre Lens ${rec.activeGenres.joinToString(" / ")} の必須条件を通過")
        }
        if (rec.matchedTraits.isNotEmpty()) {
            add("あなたの強いDNAと一致: ${rec.matchedTraits.joinToString(" / ")}")
        }
        val archiveByName = allArtists().associateBy { it.name.trim().lowercase() }
        val reference = _history.value.asSequence()
            .filter { it.reaction == Reaction.LOVE_ALL || it.reaction == Reaction.HIT }
            .mapNotNull { record -> archiveByName[record.artistName.trim().lowercase()]?.let { record to it } }
            .filterNot { (_, a) -> a.name.equals(rec.artist.name, true) }
            .map { (record, a) -> Triple(record, a, rec.artist.vector.similarity(a.vector)) }
            .maxByOrNull { it.third }
        if (reference != null && reference.third >= .60f) {
            add("${reference.first.reaction.label} の ${reference.second.name} とDNA類似 ${(reference.third * 100).toInt()}%")
        }
        val dominantVocal = _vocalProfile.value.dominantType()
        if (dominantVocal != null && rec.artist.vocalType == dominantVocal) {
            add("VOCAL DNAの主傾向 ${dominantVocal.label} と一致")
        }
        if (rec.artist.hiddenScore >= 70) {
            add("HIDDEN ${rec.artist.hiddenScore}: メジャー候補より地下発掘価値を優先")
        }
        if (rec.artist.sourceSeed != null) {
            add("発掘ルート: ${rec.artist.sourceSeed}")
        }
    }

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
        val genres = activeGenres()
        if (genres.isNotEmpty()) {
            ensureGenreLensPool(force = true)
            return
        }
        val seeds = discoverySeeds()
        _discovering.value = true
        _discoveryStatus.value = "外部発掘中: ${seeds.joinToString(" / ")}"
        viewModelScope.launch {
            externalDiscovery.discover(seeds).onSuccess { result ->
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
        _genreLensReady.value = currentLensPoolSatisfied() ||
            (store.lastFmApiKey().isBlank() && lensUnratedCandidates().isNotEmpty())
        _genreLensStatus.value = currentLensStatus()
        _recommendation.value = recommendNow()
    }

    /**
     * V0.5.4: Genre Lens remains a hard condition, but refill readiness is based on UNRATED candidates.
     * If the local genre pool is insufficient, do not show an off-genre recommendation;
     * first expand the requested genre pool, then recalculate TODAY.
     */
    private fun saveLensAndRefresh() {
        store.saveGenreLens(_genreLens.value)
        lensRefreshJob?.cancel()
        val genres = activeGenres()
        if (genres.isEmpty()) {
            _genreLensPreparing.value = false
            _genreLensReady.value = true
            _genreLensStatus.value = ""
            _recommendation.value = recommendNow()
            return
        }

        val counts = lensUnratedCounts(genres)
        val ready = counts.values.all { it >= minimumUnratedLensPoolPerGenre }
        val hasUnrated = lensUnratedCandidates(genres).isNotEmpty()
        _genreLensReady.value = ready || (hasUnrated && store.lastFmApiKey().isBlank())
        _genreLensPreparing.value = !ready && store.lastFmApiKey().isNotBlank()
        _genreLensStatus.value = lensStatusText(genres, counts, ready)
        if (hasUnrated && !_genreLensPreparing.value) {
            _recommendation.value = recommendNow()
        }

        if (!ready && store.lastFmApiKey().isNotBlank()) {
            lensRefreshJob = viewModelScope.launch {
                delay(350)
                ensureGenreLensPool()
            }
        } else if (!ready) {
            _genreLensStatus.value += " / Last.fm API Keyを設定すると未評価候補を自動補充できます"
        }
    }

    private fun ensureGenreLensPool(force: Boolean = false) {
        val genres = activeGenres()
        if (genres.isEmpty() || _discovering.value) return
        if (!force && currentLensPoolSatisfied()) {
            _genreLensReady.value = true
            _genreLensPreparing.value = false
            _genreLensStatus.value = currentLensStatus()
            _recommendation.value = recommendNow()
            return
        }
        val hasUnratedBefore = lensUnratedCandidates(genres).isNotEmpty()
        if (store.lastFmApiKey().isBlank()) {
            _genreLensReady.value = hasUnratedBefore
            _genreLensPreparing.value = false
            _genreLensStatus.value = "${genres.joinToString(" / ")} の未評価候補不足 / Last.fm API Key未設定"
            if (hasUnratedBefore) _recommendation.value = recommendNow()
            return
        }

        _discovering.value = true
        _genreLensPreparing.value = true
        _genreLensReady.value = false
        _genreLensStatus.value = "${genres.joinToString(" / ")} の未評価地下候補を補充中…"
        _discoveryStatus.value = _genreLensStatus.value
        viewModelScope.launch {
            externalDiscovery.ensureGenrePool(
                genreLenses = genres,
                minimumPerGenre = refillTargetUnratedLensPoolPerGenre,
                fetchPerGenre = 50,
                excludedArtistNames = ratedArtistNames()
            ).onSuccess { result ->
                _externalArtists.value = result.artists
                val counts = lensUnratedCounts(genres)
                val totals = lensCounts(genres)
                val hasUnrated = lensUnratedCandidates(genres).isNotEmpty()
                _genreLensReady.value = hasUnrated
                _genreLensPreparing.value = false
                _genreLensStatus.value = if (hasUnrated) {
                    "未評価候補 ${counts.entries.joinToString(" / ") { "${it.key} ${it.value}組" }} / 総候補 ${totals.entries.joinToString(" / ") { "${it.key} ${it.value}組" }} / Local DB ${result.cached}組"
                } else {
                    "${genres.joinToString(" / ")} の新しい未評価候補を取得できませんでした"
                }
                val summary = "Genre Lens未評価補充: 候補${result.fetched}件 → 新規${result.accepted}件 / ${_genreLensStatus.value}"
                _discoveryStatus.value = summary
                store.saveDiscoverySummary(summary)
                if (hasUnrated) _recommendation.value = recommendNow(System.currentTimeMillis())
            }.onFailure {
                val hasUnrated = lensUnratedCandidates(genres).isNotEmpty()
                _genreLensPreparing.value = false
                _genreLensReady.value = hasUnrated
                _genreLensStatus.value = "Genre Lens未評価候補の探索失敗: ${it.message}"
                _discoveryStatus.value = _genreLensStatus.value
                if (hasUnrated) _recommendation.value = recommendNow()
            }
            _discovering.value = false
        }
    }

    private fun recommendationSeed(): Long {
        val lensHash = activeGenres().sorted().joinToString("|").hashCode().toLong()
        return LocalDate.now().toEpochDay() * 31L + lensHash
    }

    private fun ratedArtistNames(): Set<String> =
        _history.value.map { it.artistName.trim().lowercase() }.toSet()

    private fun lensCandidates(genres: List<String> = activeGenres()): List<MetalArtist> =
        GenreLensCatalog.filter(allArtists(), genres)

    private fun lensUnratedCandidates(genres: List<String> = activeGenres()): List<MetalArtist> {
        val rated = ratedArtistNames()
        return lensCandidates(genres).filterNot { it.name.trim().lowercase() in rated }
    }

    private fun lensCounts(genres: List<String> = activeGenres()): Map<String, Int> =
        GenreLensCatalog.countByGenre(allArtists(), genres)

    private fun lensUnratedCounts(genres: List<String> = activeGenres()): Map<String, Int> {
        val rated = ratedArtistNames()
        val unratedArchive = allArtists().filterNot { it.name.trim().lowercase() in rated }
        return GenreLensCatalog.countByGenre(unratedArchive, genres)
    }

    private fun currentLensPoolSatisfied(): Boolean {
        val genres = activeGenres()
        if (genres.isEmpty()) return true
        val counts = lensUnratedCounts(genres)
        return counts.isNotEmpty() && counts.values.all { it >= minimumUnratedLensPoolPerGenre }
    }

    private fun initialLensReady(): Boolean {
        val genres = GenreLensCatalog.activeGenres(_genreLens.value)
        if (genres.isEmpty()) return true
        val rated = _history.value.map { it.artistName.trim().lowercase() }.toSet()
        val unratedArchive = allArtists().filterNot { it.name.trim().lowercase() in rated }
        val counts = GenreLensCatalog.countByGenre(unratedArchive, genres)
        return counts.isNotEmpty() && counts.values.all { it >= minimumUnratedLensPoolPerGenre }
    }

    private fun initialLensStatus(): String = currentLensStatus()

    private fun currentLensStatus(): String {
        val genres = activeGenres()
        if (genres.isEmpty()) return ""
        val counts = lensUnratedCounts(genres)
        return lensStatusText(genres, counts, currentLensPoolSatisfied())
    }

    private fun lensStatusText(genres: List<String>, counts: Map<String, Int>, ready: Boolean): String {
        val totals = lensCounts(genres)
        val detail = genres.joinToString(" / ") { "$it 未評価${counts[it] ?: 0}組・総数${totals[it] ?: 0}組" }
        return if (ready) {
            "Genre Lens候補: $detail"
        } else {
            "Genre Lens未評価候補不足: $detail（${minimumUnratedLensPoolPerGenre}組未満で自動補充 → 目標${refillTargetUnratedLensPoolPerGenre}組）"
        }
    }

    private fun recommendNow(seed: Long = recommendationSeed()): Recommendation {
        val genres = activeGenres()
        if (genres.isEmpty()) {
            return engine.recommend(
                profile = _profile.value, history = _history.value, searchHistory = _searchHistory.value,
                candidates = allArtists(), genreLens = emptyList(), seed = seed
            )
        }
        val strict = lensUnratedCandidates(genres)
        // Keep a safe hidden fallback object while the UI shows the refill state.
        // Rated or off-genre artists are never presented as the active Genre Lens recommendation.
        if (strict.isEmpty()) {
            return engine.recommend(
                profile = _profile.value, history = _history.value, searchHistory = _searchHistory.value,
                candidates = allArtists(), genreLens = emptyList(), seed = seed
            )
        }
        return engine.recommend(
            profile = _profile.value, history = _history.value, searchHistory = _searchHistory.value,
            candidates = strict, genreLens = genres, seed = seed
        )
    }

    private fun persistAndRefresh() {
        store.saveHistory(_history.value)
        store.saveProfile(_profile.value)
        store.saveVocalProfile(_vocalProfile.value)
        // Genre Lens refresh is handled after the new rating updates the unrated pool.
        // This avoids briefly surfacing the hidden off-genre fallback when the pool hits zero.
        if (activeGenres().isEmpty()) {
            _recommendation.value = recommendNow(System.currentTimeMillis())
        }
    }

    private fun refreshAfterReaction() {
        val genres = activeGenres()
        if (genres.isEmpty()) {
            _recommendation.value = recommendNow(System.currentTimeMillis())
            return
        }
        val hasUnrated = lensUnratedCandidates(genres).isNotEmpty()
        val enough = currentLensPoolSatisfied()
        if (enough) {
            _genreLensReady.value = true
            _genreLensPreparing.value = false
            _genreLensStatus.value = currentLensStatus()
            _recommendation.value = recommendNow(System.currentTimeMillis())
            return
        }
        _genreLensStatus.value = currentLensStatus()
        if (store.lastFmApiKey().isNotBlank()) {
            _genreLensReady.value = false
            _genreLensPreparing.value = true
            lensRefreshJob?.cancel()
            lensRefreshJob = viewModelScope.launch {
                delay(120)
                ensureGenreLensPool(force = true)
            }
        } else {
            _genreLensReady.value = hasUnrated
            _genreLensPreparing.value = false
            if (hasUnrated) _recommendation.value = recommendNow(System.currentTimeMillis())
        }
    }

    private fun showReactionStatus(message: String) {
        _reactionStatus.value = message
        reactionFeedbackJob?.cancel()
        reactionFeedbackJob = viewModelScope.launch {
            delay(2200)
            if (_reactionStatus.value == message) _reactionStatus.value = ""
        }
    }
}
