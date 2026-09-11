import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

enum LastFMServiceError: LocalizedError {
    case missingAPIKey
    case invalidResponse
    case api(String)

    var errorDescription: String? {
        switch self {
        case .missingAPIKey: return "Last.fm API Keyを設定してください"
        case .invalidResponse: return "Last.fmの応答を解析できませんでした"
        case .api(let message): return message
        }
    }
}

struct LastFMService: Sendable {
    private let session: URLSession
    init(session: URLSession = .shared) { self.session = session }

    func searchMetalArtists(query: String, apiKey: String, limit: Int = 8) async throws -> [MetalArtist] {
        guard !apiKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { throw LastFMServiceError.missingAPIKey }
        let root = try await call(method: "artist.search", apiKey: apiKey, params: ["artist": query, "limit": "\(max(1, min(20, limit * 2)))"])
        guard let results = ((root["results"] as? [String: Any])?["artistmatches"] as? [String: Any])?["artist"] as? [[String: Any]] else { return [] }
        var output: [MetalArtist] = []
        for row in results.prefix(limit * 2) {
            guard let name = row["name"] as? String, !name.isEmpty else { continue }
            if let artist = try? await artistInfo(name: name, mbid: row["mbid"] as? String, apiKey: apiKey), isMetal(artist) {
                output.append(artist)
                if output.count >= limit { break }
            }
        }
        return output
    }

    func similarArtists(to artist: MetalArtist, apiKey: String, limit: Int = 12) async throws -> [MetalArtist] {
        guard !apiKey.isEmpty else { throw LastFMServiceError.missingAPIKey }
        var params = ["artist": artist.name, "limit": "\(max(1, min(50, limit * 2)))"]
        if let mbid = artist.mbid, !mbid.isEmpty { params["mbid"] = mbid }
        let root = try await call(method: "artist.getSimilar", apiKey: apiKey, params: params)
        guard let rows = (root["similarartists"] as? [String: Any])?["artist"] as? [[String: Any]] else { return [] }
        var output: [MetalArtist] = []
        for row in rows.prefix(limit * 2) {
            guard let name = row["name"] as? String else { continue }
            if let info = try? await artistInfo(name: name, mbid: row["mbid"] as? String, apiKey: apiKey), isMetal(info) {
                var candidate = info
                candidate.sourceSeed = artist.name
                candidate.discovery = max(candidate.discovery, 0.92)
                output.append(candidate)
                if output.count >= limit { break }
            }
        }
        return output
    }

    func genreArtists(genres: [String], apiKey: String, perGenre: Int = 20) async throws -> [MetalArtist] {
        guard !apiKey.isEmpty else { throw LastFMServiceError.missingAPIKey }
        var byID: [String: MetalArtist] = [:]
        for genre in genres {
            let tag = GenreLensCore.lenses.first(where: { $0.name == genre })?.aliases.first ?? genre
            let root = try await call(method: "tag.getTopArtists", apiKey: apiKey, params: ["tag": tag, "limit": "\(min(50, perGenre * 2))"])
            guard let rows = (root["topartists"] as? [String: Any])?["artist"] as? [[String: Any]] else { continue }
            var accepted = 0
            for row in rows {
                guard let name = row["name"] as? String else { continue }
                if var info = try? await artistInfo(name: name, mbid: row["mbid"] as? String, apiKey: apiKey), GenreLensCore.matches(info, names: [genre]) || isMetal(info) {
                    info.sourceSeed = "genre:\(genre)"
                    byID[info.id] = info
                    accepted += 1
                    if accepted >= perGenre { break }
                }
            }
        }
        return Array(byID.values)
    }

    func topTrackNames(artist: MetalArtist, apiKey: String, limit: Int = 40) async throws -> [String] {
        guard !apiKey.isEmpty else { return [] }
        var params = ["artist": artist.name, "limit": "\(min(50, limit))"]
        if let mbid = artist.mbid, !mbid.isEmpty { params["mbid"] = mbid }
        let root = try await call(method: "artist.getTopTracks", apiKey: apiKey, params: params)
        let rows = (root["toptracks"] as? [String: Any])?["track"] as? [[String: Any]] ?? []
        return rows.compactMap { $0["name"] as? String }.filter { !$0.isEmpty }
    }

    private func artistInfo(name: String, mbid: String?, apiKey: String) async throws -> MetalArtist {
        var params = ["artist": name, "autocorrect": "1"]
        if let mbid, !mbid.isEmpty { params["mbid"] = mbid }
        let root = try await call(method: "artist.getInfo", apiKey: apiKey, params: params)
        guard let row = root["artist"] as? [String: Any] else { throw LastFMServiceError.invalidResponse }
        let tagsRows = ((row["tags"] as? [String: Any])?["tag"] as? [[String: Any]]) ?? []
        let tags = tagsRows.compactMap { $0["name"] as? String }.filter { !$0.isEmpty }
        let stats = row["stats"] as? [String: Any] ?? [:]
        let listeners = Int64((stats["listeners"] as? String) ?? "") ?? 0
        let playcount = Int64((stats["playcount"] as? String) ?? "") ?? 0
        let vector = inferredVector(tags: tags)
        let hidden = hiddenScore(listeners: listeners, playcount: playcount)
        let detectedVocal = inferredVocal(tags: tags)
        return MetalArtist(
            name: row["name"] as? String ?? name,
            country: "External",
            genres: tags,
            vector: vector,
            discovery: 0.86,
            reason: "Last.fmから発掘",
            source: "LASTFM",
            sourceSeed: nil,
            externalScore: nil,
            lastFmListeners: listeners > 0 ? listeners : nil,
            lastFmPlaycount: playcount > 0 ? playcount : nil,
            mbid: (row["mbid"] as? String).flatMap { $0.isEmpty ? nil : $0 },
            area: nil, beginDate: nil, endDate: nil, ended: nil,
            hiddenScore: hidden,
            metadataConfidence: ((row["mbid"] as? String)?.isEmpty == false) ? 85 : 55,
            vocalType: detectedVocal
        )
    }

    private func call(method: String, apiKey: String, params: [String: String]) async throws -> [String: Any] {
        var components = URLComponents(string: "https://ws.audioscrobbler.com/2.0/")!
        var items = [URLQueryItem(name: "method", value: method), URLQueryItem(name: "api_key", value: apiKey), URLQueryItem(name: "format", value: "json")]
        items.append(contentsOf: params.map { URLQueryItem(name: $0.key, value: $0.value) })
        components.queryItems = items
        let (data, response) = try await session.data(from: components.url!)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else { throw LastFMServiceError.invalidResponse }
        guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any] else { throw LastFMServiceError.invalidResponse }
        if let message = root["message"] as? String { throw LastFMServiceError.api(message) }
        return root
    }

    private func isMetal(_ artist: MetalArtist) -> Bool {
        let text = artist.genres.joined(separator: " ").lowercased()
        return text.contains("metal") || GenreLensCore.lenses.flatMap(\.aliases).contains { text.contains($0.lowercased()) }
    }

    private func inferredVector(tags: [String]) -> MetalVector {
        let text = tags.joined(separator: " ").lowercased()
        let matched = GenreLensCore.lenses.filter { lens in lens.aliases.contains { text.contains($0.lowercased()) } }
        guard !matched.isEmpty else { return MetalVector(melody: 0.6, speed: 0.6, heavy: 0.7, symphonic: 0.3, technical: 0.5, growl: 0.45, cleanVocal: 0.6, catchy: 0.55) }
        func avg(_ kp: KeyPath<MetalVector, Double>) -> Double { matched.map { $0.vector[keyPath: kp] }.reduce(0, +) / Double(matched.count) }
        return .init(melody: avg(\.melody), speed: avg(\.speed), heavy: avg(\.heavy), symphonic: avg(\.symphonic), technical: avg(\.technical), growl: avg(\.growl), cleanVocal: avg(\.cleanVocal), catchy: avg(\.catchy))
    }

    private func hiddenScore(listeners: Int64, playcount: Int64) -> Int {
        guard listeners > 0 else { return 82 }
        let rarity = max(0, min(1, 1 - log10(Double(listeners) + 10) / 7.0))
        let engagement = max(0, min(1, Double(playcount) / Double(max(1, listeners)) / 50.0))
        return Int((rarity * 0.75 + engagement * 0.25) * 100)
    }

    private func inferredVocal(tags: [String]) -> VocalType {
        let text = tags.joined(separator: " ").lowercased()
        if text.contains("female vocal") || text.contains("female fronted") { return .female }
        if text.contains("dual vocal") || text.contains("mixed vocal") { return .mixed }
        return .unknown
    }
}
