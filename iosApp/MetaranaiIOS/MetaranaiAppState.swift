import Foundation
import Combine

@MainActor
final class MetaranaiAppState: ObservableObject {
    @Published var profile: MetalVector = .metaranaiDefault
    @Published var history: [DiscoveryRecord] = []
    @Published var searchHistory: [SearchRecord] = []
    @Published var artists: [MetalArtist] = []
    @Published var genreLens = GenreLensConfig()
    @Published var vocalProfile = VocalProfile()
    @Published var recommendation: Recommendation?
    @Published var statusMessage = ""
    @Published var isBusy = false
    @Published var clientID = ""
    @Published var lastFmAPIKey = ""
    @Published var searchResults: [MetalArtist] = []
    @Published var deepDiveResults: [MetalArtist] = []

    let spotifyAuth = SpotifyAuthManager()
    private let defaults: UserDefaults
    private let lastFM = LastFMService()
    private lazy var spotifyResolver = SpotifyIdentityResolver(defaults: defaults, lastFM: lastFM)

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        reload()
        spotifyAuth.importLegacyTokens(from: defaults)
    }

    var activeGenres: [String] { GenreLensCore.activeGenres(genreLens) }
    var dnaType: String { RecommendationCore.dnaType(profile: profile, vocal: vocalProfile, history: history) }
    var ratedArtistNames: Set<String> { Set(history.map { MetalArtist.normalizeName($0.artistName) }) }
    var unratedArtists: [MetalArtist] { artists.filter { !ratedArtistNames.contains(MetalArtist.normalizeName($0.name)) } }
    var positiveRate: Int {
        guard !history.isEmpty else { return 0 }
        let positive = history.filter { $0.reaction == .loveAll || $0.reaction == .hit || $0.reaction == .some }.count
        return Int((Double(positive) / Double(history.count) * 100).rounded())
    }
    var averageAffinity: Int {
        guard !history.isEmpty else { return 0 }
        return Int((Double(history.map { $0.reaction.affinityScore }.reduce(0, +)) / Double(history.count)).rounded())
    }

    func reload() {
        profile = MetalDataParser.profile(from: defaults.string(forKey: "profile"))
        history = MetalDataParser.history(from: defaults.string(forKey: "history"))
        searchHistory = MetalDataParser.searchHistory(from: defaults.string(forKey: "search_history"))
        let external = MetalDataParser.externalArtists(from: defaults.string(forKey: "external_artists"))
        var combined = Dictionary(uniqueKeysWithValues: SeedCatalog.artists.map { ($0.id, $0) })
        for artist in external { combined[artist.id] = artist }
        artists = Array(combined.values)
        genreLens = MetalDataParser.genreLens(from: defaults.string(forKey: "genre_lens_v05"))
        vocalProfile = MetalDataParser.vocalProfile(from: defaults.string(forKey: "vocal_profile_v05"))
        clientID = defaults.string(forKey: "spotify_client_id") ?? ""
        lastFmAPIKey = defaults.string(forKey: "lastfm_api_key") ?? ""
        recomputeRecommendation()
    }

    func recomputeRecommendation(seed: Int? = nil) {
        recommendation = RecommendationCore.recommend(
            profile: profile, history: history, searchHistory: searchHistory,
            candidates: artists, genreLens: activeGenres, seed: seed
        )
    }

    func rate(_ artist: MetalArtist, reaction: Reaction) {
        let today = Self.dayString(Date())
        if history.contains(where: { MetalArtist.normalizeName($0.artistName) == MetalArtist.normalizeName(artist.name) && $0.date == today }) {
            statusMessage = "\(artist.name)は今日すでに評価済み。未評価候補を探します"
            recomputeRecommendation(seed: Int.random(in: 1...999_999))
            return
        }
        profile = RecommendationCore.updatedProfile(current: profile, artist: artist, reaction: reaction)
        history.insert(DiscoveryRecord(artistName: artist.name, date: today, reaction: reaction, score: recommendation?.compatibility ?? 0), at: 0)
        defaults.set(MetalDataParser.profileJSON(profile), forKey: "profile")
        defaults.set(MetalDataParser.historyJSON(history), forKey: "history")
        statusMessage = "\(reaction.label) を記録しました"
        recomputeRecommendation(seed: Int.random(in: 1...999_999))
    }

    func setLensMode(_ mode: GenreLensMode) {
        genreLens.mode = mode
        persistGenreLens()
    }

    func toggleManualGenre(_ genre: String) {
        if genreLens.manualGenres.contains(genre) { genreLens.manualGenres.remove(genre) }
        else { genreLens.manualGenres.insert(genre) }
        genreLens.mode = genreLens.manualGenres.isEmpty ? .off : .manual
        persistGenreLens()
    }

    func setWeekdayGenres(day: String, genres: Set<String>) {
        genreLens.weekdayGenres[day] = genres
        genreLens.mode = .weekday
        persistGenreLens()
    }

    func saveCredentials() {
        defaults.set(clientID.trimmingCharacters(in: .whitespacesAndNewlines), forKey: "spotify_client_id")
        defaults.set(lastFmAPIKey.trimmingCharacters(in: .whitespacesAndNewlines), forKey: "lastfm_api_key")
        statusMessage = "API設定を保存しました"
    }

    func ensureGenrePool(minimumUnrated: Int = 10, target: Int = 20) async {
        let genres = activeGenres
        guard !genres.isEmpty else { return }
        let current = unratedArtists.filter { GenreLensCore.matches($0, names: genres) }
        guard current.count < minimumUnrated else { return }
        guard !lastFmAPIKey.isEmpty else {
            statusMessage = "\(genres.joined(separator: " / ")) の未評価候補が\(current.count)組。Last.fm API Key設定後に補充できます"
            return
        }
        isBusy = true
        statusMessage = "\(genres.joined(separator: " / ")) の地下を探索中…"
        defer { isBusy = false }
        do {
            let fetched = try await lastFM.genreArtists(genres: genres, apiKey: lastFmAPIKey, perGenre: max(target, 20))
            mergeArtists(fetched)
            let after = unratedArtists.filter { GenreLensCore.matches($0, names: genres) }.count
            statusMessage = "Genre Lens候補を補充：未評価\(after)組"
            recomputeRecommendation(seed: Int.random(in: 1...999_999))
        } catch {
            statusMessage = "地下探索失敗: \(error.localizedDescription)"
        }
    }

    func search(_ query: String) async {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else { searchResults = []; return }
        let local = artists.filter {
            $0.name.localizedCaseInsensitiveContains(q) || $0.country.localizedCaseInsensitiveContains(q) || $0.genres.contains(where: { $0.localizedCaseInsensitiveContains(q) })
        }
        searchResults = Array(local.prefix(20))
        if local.count >= 5 || lastFmAPIKey.isEmpty { return }
        isBusy = true
        statusMessage = "世界検索中…"
        defer { isBusy = false }
        do {
            let remote = try await lastFM.searchMetalArtists(query: q, apiKey: lastFmAPIKey, limit: 10)
            mergeArtists(remote)
            let merged = (local + remote).reduce(into: [String: MetalArtist]()) { $0[$1.id] = $1 }
            searchResults = Array(merged.values).sorted { $0.hiddenScore > $1.hiddenScore }
            statusMessage = "検索で\(remote.count)組をLocal Metal DBへ追加"
        } catch {
            statusMessage = "外部検索失敗: \(error.localizedDescription)"
        }
    }

    func deepDive(from artist: MetalArtist) async {
        guard !lastFmAPIKey.isEmpty else { statusMessage = "Deep DiveにはLast.fm API Keyが必要です"; return }
        isBusy = true
        statusMessage = "\(artist.name) から地下を掘っています…"
        defer { isBusy = false }
        do {
            let remote = try await lastFM.similarArtists(to: artist, apiKey: lastFmAPIKey, limit: 16)
            mergeArtists(remote)
            deepDiveResults = remote.sorted { profile.similarity(to: $0.vector) > profile.similarity(to: $1.vector) }
            statusMessage = "Deep Diveで\(remote.count)組を発見"
        } catch {
            statusMessage = "Deep Dive失敗: \(error.localizedDescription)"
        }
    }

    func spotifyDestination(for artist: MetalArtist) async -> SpotifyDestination {
        var token: String? = nil
        if spotifyAuth.isAuthenticated, !clientID.isEmpty {
            token = try? await spotifyAuth.accessToken(clientID: clientID)
        }
        let destination = await spotifyResolver.resolve(artist: artist, token: token, lastFmKey: lastFmAPIKey)
        statusMessage = destination.verification
        return destination
    }

    func youtubeURL(for artist: MetalArtist, suffix: String = "") -> URL? {
        var components = URLComponents(string: "https://www.youtube.com/results")!
        components.queryItems = [.init(name: "search_query", value: [artist.name, suffix].filter { !$0.isEmpty }.joined(separator: " "))]
        return components.url
    }

    func importBackup(_ data: Data) throws -> LegacyBackupSummary {
        let summary = try PortableBackupCodec.inspect(data: data)
        _ = try PortableBackupCodec.restore(data: data, to: defaults)
        spotifyAuth.importLegacyTokens(from: defaults)
        reload()
        statusMessage = "Backup v\(summary.version)を復元：評価\(summary.historyCount)件 / External \(summary.externalArtistCount)組"
        return summary
    }

    func exportBackup() throws -> Data {
        try PortableBackupCodec.exportData(from: defaults, overrides: spotifyAuth.portableTokenOverrides(), version: 90)
    }

    func reaction(for artist: MetalArtist) -> Reaction? {
        history.first(where: { MetalArtist.normalizeName($0.artistName) == MetalArtist.normalizeName(artist.name) })?.reaction
    }

    func filteredArchive(query: String, genre: String?, filterReaction: Reaction?, onlyUnrated: Bool, vocal: VocalType?, sort: ArchiveSort) -> [MetalArtist] {
        var result = artists
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        if !q.isEmpty { result = result.filter { $0.name.localizedCaseInsensitiveContains(q) || $0.country.localizedCaseInsensitiveContains(q) } }
        if let genre { result = result.filter { GenreLensCore.matches($0, names: [genre]) } }
        if let vocal { result = result.filter { $0.vocalType == vocal } }
        if onlyUnrated { result = result.filter { reaction(for: $0) == nil } }
        if let reaction { result = result.filter { self.reaction(for: $0) == reaction } }
        switch sort {
        case .recommended: result.sort { profile.similarity(to: $0.vector) > profile.similarity(to: $1.vector) }
        case .hidden: result.sort { $0.hiddenScore > $1.hiddenScore }
        case .name: result.sort { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
        }
        return result
    }

    enum ArchiveSort: String, CaseIterable, Identifiable {
        case recommended = "おすすめ順", hidden = "HIDDEN", name = "名前順"
        var id: String { rawValue }
    }

    private func persistGenreLens() {
        defaults.set(MetalDataParser.genreLensJSON(genreLens), forKey: "genre_lens_v05")
        recomputeRecommendation(seed: Int.random(in: 1...999_999))
    }

    private func mergeArtists(_ incoming: [MetalArtist]) {
        var map = Dictionary(uniqueKeysWithValues: artists.map { ($0.id, $0) })
        for artist in incoming { map[artist.id] = artist }
        artists = Array(map.values)
        let portableExternal = artists.filter { $0.source != "BUILTIN" }
        defaults.set(MetalDataParser.externalArtistsJSON(portableExternal), forKey: "external_artists")
    }

    private static func dayString(_ date: Date) -> String {
        let f = DateFormatter(); f.locale = Locale(identifier: "en_US_POSIX"); f.dateFormat = "yyyy-MM-dd"; return f.string(from: date)
    }
}
