import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

struct SpotifyIdentityResolver {
    private let defaults: UserDefaults
    private let lastFM: LastFMService

    init(defaults: UserDefaults = .standard, lastFM: LastFMService = LastFMService()) {
        self.defaults = defaults
        self.lastFM = lastFM
    }

    func resolve(artist: MetalArtist, token: String?, lastFmKey: String) async -> SpotifyDestination {
        if let cached = cachedVerified(artist: artist) { return cached }
        let fallback = spotifySearchURL(artist.name)
        guard let token, !token.isEmpty else {
            return SpotifyDestination(url: fallback, direct: false, artistID: nil, verification: "Spotify未認証: 検索へ")
        }
        do {
            let matches = try await exactArtistMatches(name: artist.name, token: token)
            guard !matches.isEmpty else {
                return SpotifyDestination(url: fallback, direct: false, artistID: nil, verification: "Spotify完全一致なし: 検索へ")
            }
            let lastFmTracks = (try? await lastFM.topTrackNames(artist: artist, apiKey: lastFmKey, limit: 40)) ?? []
            let appleGroups = (try? await appleCatalogGroups(name: artist.name)) ?? []
            var scored: [(SpotifyCandidate, Int, String)] = []
            for candidate in matches {
                let catalog = (try? await spotifyCatalog(candidate: candidate, token: token)) ?? .init(tracks: [], albums: [])
                var bestScore = 0
                var bestReason = ""
                if !lastFmTracks.isEmpty {
                    let tm = CatalogFingerprint.trackMatches(lastFmTracks, catalog.tracks)
                    let score = CatalogFingerprint.evidenceScore(trackMatches: tm.count, albumMatches: 0)
                    if score > bestScore { bestScore = score; bestReason = "Last.fm曲\(tm.count)件" }
                }
                for group in appleGroups {
                    let tm = CatalogFingerprint.trackMatches(group.tracks, catalog.tracks)
                    let am = CatalogFingerprint.albumMatches(group.albums, catalog.albums)
                    let score = CatalogFingerprint.evidenceScore(trackMatches: tm.count, albumMatches: am.count)
                    if score > bestScore { bestScore = score; bestReason = "Apple曲\(tm.count)件/Album\(am.count)件" }
                }
                scored.append((candidate, bestScore, bestReason))
            }

            let sorted = scored.sorted { $0.1 > $1.1 }
            if matches.count == 1, let winner = sorted.first, winner.1 >= 6 {
                let result = SpotifyDestination(url: winner.0.url, direct: true, artistID: winner.0.id, verification: "Spotify名前完全一致 + \(winner.2)")
                saveVerified(artist: artist, result: result)
                return result
            }
            if matches.count > 1, let winner = sorted.first {
                let runner = sorted.dropFirst().first?.1 ?? 0
                if winner.1 >= 6 && winner.1 >= runner + 3 {
                    let result = SpotifyDestination(url: winner.0.url, direct: true, artistID: winner.0.id, verification: "同名候補から独立カタログ指紋で一意確認: \(winner.2)")
                    saveVerified(artist: artist, result: result)
                    return result
                }
            }
            return SpotifyDestination(url: fallback, direct: false, artistID: nil, verification: "本人確認材料不足: 検索へ")
        } catch {
            return SpotifyDestination(url: fallback, direct: false, artistID: nil, verification: "Spotify照合失敗: 検索へ")
        }
    }

    private struct SpotifyCandidate: Sendable { let id: String; let name: String; let url: URL }
    private struct Catalog: Sendable { let tracks: [String]; let albums: [String] }
    private struct AppleGroup: Sendable { let id: String; let tracks: [String]; let albums: [String] }

    private func exactArtistMatches(name: String, token: String) async throws -> [SpotifyCandidate] {
        var output: [SpotifyCandidate] = []
        let target = CatalogFingerprint.canonicalName(name)
        for offset in [0, 10, 20] {
            var components = URLComponents(string: "https://api.spotify.com/v1/search")!
            components.queryItems = [
                .init(name: "q", value: name), .init(name: "type", value: "artist"),
                .init(name: "limit", value: "10"), .init(name: "offset", value: "\(offset)")
            ]
            let root = try await spotifyJSON(url: components.url!, token: token)
            let artists = ((root["artists"] as? [String: Any])?["items"] as? [[String: Any]]) ?? []
            for row in artists {
                guard let rowName = row["name"] as? String, CatalogFingerprint.canonicalName(rowName) == target,
                      let id = row["id"] as? String,
                      let urls = row["external_urls"] as? [String: Any], let urlRaw = urls["spotify"] as? String,
                      let url = URL(string: urlRaw) else { continue }
                output.append(.init(id: id, name: rowName, url: url))
            }
            if artists.count < 10 { break }
        }
        var seen = Set<String>()
        return output.filter { seen.insert($0.id).inserted }
    }

    private func spotifyCatalog(candidate: SpotifyCandidate, token: String) async throws -> Catalog {
        var tracks = Set<String>()
        var albums = Set<String>()
        var components = URLComponents(string: "https://api.spotify.com/v1/artists/\(candidate.id)/albums")!
        components.queryItems = [
            .init(name: "include_groups", value: "album,single"), .init(name: "limit", value: "10")
        ]
        let root = try await spotifyJSON(url: components.url!, token: token)
        let items = root["items"] as? [[String: Any]] ?? []
        var albumIDs: [String] = []
        for album in items {
            if let name = album["name"] as? String, !name.isEmpty { albums.insert(name) }
            if let id = album["id"] as? String, !id.isEmpty { albumIDs.append(id) }
        }
        for id in albumIDs.prefix(5) {
            var trackComponents = URLComponents(string: "https://api.spotify.com/v1/albums/\(id)/tracks")!
            trackComponents.queryItems = [.init(name: "limit", value: "50")]
            let row = try await spotifyJSON(url: trackComponents.url!, token: token)
            for track in row["items"] as? [[String: Any]] ?? [] {
                let artists = track["artists"] as? [[String: Any]] ?? []
                guard artists.contains(where: { ($0["id"] as? String) == candidate.id }) else { continue }
                if let name = track["name"] as? String, !name.isEmpty { tracks.insert(name) }
            }
        }
        return Catalog(tracks: Array(tracks), albums: Array(albums))
    }

    private func appleCatalogGroups(name: String) async throws -> [AppleGroup] {
        var components = URLComponents(string: "https://itunes.apple.com/search")!
        components.queryItems = [
            .init(name: "term", value: name), .init(name: "country", value: "JP"),
            .init(name: "media", value: "music"), .init(name: "entity", value: "song"),
            .init(name: "attribute", value: "artistTerm"), .init(name: "limit", value: "200")
        ]
        let (data, _) = try await URLSession.shared.data(from: components.url!)
        let root = try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
        let rows = root["results"] as? [[String: Any]] ?? []
        let target = CatalogFingerprint.canonicalName(name)
        var groups: [String: (Set<String>, Set<String>)] = [:]
        for row in rows {
            guard CatalogFingerprint.canonicalName(row["artistName"] as? String ?? "") == target else { continue }
            let id = String(describing: row["artistId"] ?? "name:\(target)")
            var pair = groups[id] ?? ([], [])
            if let track = row["trackName"] as? String { pair.0.insert(track) }
            if let album = row["collectionName"] as? String { pair.1.insert(album) }
            groups[id] = pair
        }
        return groups.map { AppleGroup(id: $0.key, tracks: Array($0.value.0), albums: Array($0.value.1)) }
    }

    private func spotifyJSON(url: URL, token: String) async throws -> [String: Any] {
        var request = URLRequest(url: url)
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else { throw URLError(.badServerResponse) }
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    private func cachedVerified(artist: MetalArtist) -> SpotifyDestination? {
        for key in ["spotify_artist_links_v080", "spotify_artist_links_v064"] {
            guard let raw = defaults.string(forKey: key), let data = raw.data(using: .utf8),
                  let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let row = root[identityKey(artist)] as? [String: Any],
                  let urlRaw = row["url"] as? String, let url = URL(string: urlRaw) else { continue }
            return SpotifyDestination(url: url, direct: true, artistID: row["artistId"] as? String, verification: row["verification"] as? String ?? "Android verified cache")
        }
        return nil
    }

    private func saveVerified(artist: MetalArtist, result: SpotifyDestination) {
        let raw = defaults.string(forKey: "spotify_artist_links_v080") ?? "{}"
        var root: [String: Any] = [:]
        if let data = raw.data(using: .utf8), let parsed = try? JSONSerialization.jsonObject(with: data) as? [String: Any] { root = parsed }
        root[identityKey(artist)] = [
            "url": result.url.absoluteString, "artistId": result.artistID ?? "",
            "verification": result.verification, "artistName": artist.name,
            "mbid": artist.mbid ?? "", "verifiedBy": "v0.8.0-ios-multi-catalog"
        ]
        defaults.set(MetalDataParser.jsonString(root), forKey: "spotify_artist_links_v080")
    }

    private func identityKey(_ artist: MetalArtist) -> String {
        if let mbid = artist.mbid?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(), !mbid.isEmpty, artist.metadataConfidence >= 90 { return "mbid:\(mbid)" }
        let norm: (String?) -> String = { ($0 ?? "").trimmingCharacters(in: .whitespacesAndNewlines).lowercased().split(whereSeparator: { $0.isWhitespace }).joined(separator: " ") }
        return "name:\(norm(artist.name))|country:\(norm(artist.country))|area:\(norm(artist.area))|begin:\(norm(artist.beginDate))"
    }

    private func spotifySearchURL(_ name: String) -> URL {
        var components = URLComponents(string: "https://open.spotify.com/search/\(name.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? name)")!
        components.query = nil
        return components.url!
    }

}
