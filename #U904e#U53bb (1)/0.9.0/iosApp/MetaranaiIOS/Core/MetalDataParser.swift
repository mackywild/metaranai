import Foundation

enum MetalDataParser {
    static func profile(from raw: String?) -> MetalVector {
        guard let dict = jsonObject(raw) else { return .metaranaiDefault }
        return MetalVector(
            melody: number(dict["melody"], 0.93), speed: number(dict["speed"], 0.80),
            heavy: number(dict["heavy"], 0.55), symphonic: number(dict["symphonic"], 0.84),
            technical: number(dict["technical"], 0.57), growl: number(dict["growl"], 0.12),
            cleanVocal: number(dict["cleanVocal"], 0.94), catchy: number(dict["catchy"], 0.92)
        )
    }

    static func profileJSON(_ value: MetalVector) -> String {
        jsonString([
            "melody": value.melody, "speed": value.speed, "heavy": value.heavy,
            "symphonic": value.symphonic, "technical": value.technical, "growl": value.growl,
            "cleanVocal": value.cleanVocal, "catchy": value.catchy
        ])
    }

    static func history(from raw: String?) -> [DiscoveryRecord] {
        guard let array = jsonArray(raw) else { return [] }
        return array.compactMap { value in
            guard let row = value as? [String: Any], let reaction = Reaction.compatible(raw: row["reaction"] as? String ?? "") else { return nil }
            return DiscoveryRecord(
                artistName: row["artist"] as? String ?? "",
                date: row["date"] as? String ?? "",
                reaction: reaction,
                score: (row["score"] as? NSNumber)?.intValue ?? 0
            )
        }.filter { !$0.artistName.isEmpty }
    }

    static func historyJSON(_ values: [DiscoveryRecord]) -> String {
        jsonString(values.map { ["artist": $0.artistName, "date": $0.date, "reaction": $0.reaction.rawValue, "score": $0.score] as [String: Any] })
    }

    static func searchHistory(from raw: String?) -> [SearchRecord] {
        guard let array = jsonArray(raw) else { return [] }
        return array.compactMap { value in
            guard let row = value as? [String: Any] else { return nil }
            let name = row["artist"] as? String ?? ""
            guard !name.isEmpty else { return nil }
            return SearchRecord(query: row["query"] as? String ?? "", artistName: name, dateTime: row["dateTime"] as? String ?? "")
        }
    }

    static func searchHistoryJSON(_ values: [SearchRecord]) -> String {
        jsonString(values.map { ["query": $0.query, "artist": $0.artistName, "dateTime": $0.dateTime] })
    }

    static func externalArtists(from raw: String?) -> [MetalArtist] {
        guard let array = jsonArray(raw) else { return [] }
        return array.compactMap { artist(from: $0 as? [String: Any]) }
    }

    static func externalArtistsJSON(_ values: [MetalArtist]) -> String {
        jsonString(values.map(artistDictionary))
    }

    static func artist(from row: [String: Any]?) -> MetalArtist? {
        guard let row, let name = row["name"] as? String, !name.isEmpty,
              let vectorRow = row["vector"] as? [String: Any] else { return nil }
        let genres = row["genres"] as? [String] ?? []
        return MetalArtist(
            name: name,
            country: row["country"] as? String ?? "External",
            genres: genres,
            vector: MetalVector(
                melody: number(vectorRow["melody"], 0.5), speed: number(vectorRow["speed"], 0.5),
                heavy: number(vectorRow["heavy"], 0.5), symphonic: number(vectorRow["symphonic"], 0.5),
                technical: number(vectorRow["technical"], 0.5), growl: number(vectorRow["growl"], 0.5),
                cleanVocal: number(vectorRow["cleanVocal"], 0.5), catchy: number(vectorRow["catchy"], 0.5)
            ),
            discovery: number(row["discovery"], 0.8),
            reason: row["reason"] as? String ?? "",
            source: row["source"] as? String ?? "LASTFM",
            sourceSeed: nonEmpty(row["sourceSeed"] as? String),
            externalScore: (row["externalScore"] as? NSNumber)?.intValue,
            lastFmListeners: (row["lastFmListeners"] as? NSNumber)?.int64Value,
            lastFmPlaycount: (row["lastFmPlaycount"] as? NSNumber)?.int64Value,
            mbid: nonEmpty(row["mbid"] as? String),
            area: nonEmpty(row["area"] as? String),
            beginDate: nonEmpty(row["beginDate"] as? String),
            endDate: nonEmpty(row["endDate"] as? String),
            ended: row["ended"] as? Bool,
            hiddenScore: (row["hiddenScore"] as? NSNumber)?.intValue ?? 50,
            metadataConfidence: (row["metadataConfidence"] as? NSNumber)?.intValue ?? 0,
            vocalType: VocalType(rawValue: row["vocalType"] as? String ?? "UNKNOWN") ?? .unknown
        )
    }

    static func artistDictionary(_ artist: MetalArtist) -> [String: Any] {
        var row: [String: Any] = [
            "name": artist.name, "country": artist.country, "genres": artist.genres,
            "vector": [
                "melody": artist.vector.melody, "speed": artist.vector.speed, "heavy": artist.vector.heavy,
                "symphonic": artist.vector.symphonic, "technical": artist.vector.technical,
                "growl": artist.vector.growl, "cleanVocal": artist.vector.cleanVocal, "catchy": artist.vector.catchy
            ],
            "discovery": artist.discovery, "reason": artist.reason, "source": artist.source,
            "sourceSeed": artist.sourceSeed ?? "", "mbid": artist.mbid ?? "", "area": artist.area ?? "",
            "beginDate": artist.beginDate ?? "", "endDate": artist.endDate ?? "",
            "hiddenScore": artist.hiddenScore, "metadataConfidence": artist.metadataConfidence,
            "vocalType": artist.vocalType.rawValue
        ]
        if let value = artist.externalScore { row["externalScore"] = value }
        if let value = artist.lastFmListeners { row["lastFmListeners"] = value }
        if let value = artist.lastFmPlaycount { row["lastFmPlaycount"] = value }
        if let value = artist.ended { row["ended"] = value }
        return row
    }

    static func genreLens(from raw: String?) -> GenreLensConfig {
        guard let root = jsonObject(raw) else { return GenreLensConfig() }
        let mode = GenreLensMode(rawValue: root["mode"] as? String ?? "OFF") ?? .off
        let manual = Set(root["manual"] as? [String] ?? [])
        var weekdays: [String: Set<String>] = [:]
        if let rows = root["weekdays"] as? [String: Any] {
            for (day, value) in rows { weekdays[day] = Set(value as? [String] ?? []) }
        }
        return GenreLensConfig(mode: mode, manualGenres: manual, weekdayGenres: weekdays)
    }

    static func genreLensJSON(_ config: GenreLensConfig) -> String {
        let weekdays = config.weekdayGenres.mapValues { Array($0).sorted() }
        return jsonString(["mode": config.mode.rawValue, "manual": Array(config.manualGenres).sorted(), "weekdays": weekdays])
    }

    static func vocalProfile(from raw: String?) -> VocalProfile {
        guard let root = jsonObject(raw) else { return VocalProfile() }
        return VocalProfile(
            male: number(root["male"], 0.333), female: number(root["female"], 0.333),
            mixed: number(root["mixed"], 0.334), observations: (root["observations"] as? NSNumber)?.intValue ?? 0
        )
    }

    static func jsonObject(_ raw: String?) -> [String: Any]? {
        guard let raw, let data = raw.data(using: .utf8) else { return nil }
        return try? JSONSerialization.jsonObject(with: data) as? [String: Any]
    }

    static func jsonArray(_ raw: String?) -> [Any]? {
        guard let raw, let data = raw.data(using: .utf8) else { return nil }
        return try? JSONSerialization.jsonObject(with: data) as? [Any]
    }

    static func jsonString(_ object: Any) -> String {
        guard JSONSerialization.isValidJSONObject(object),
              let data = try? JSONSerialization.data(withJSONObject: object, options: []) else { return "{}" }
        return String(data: data, encoding: .utf8) ?? "{}"
    }

    private static func number(_ value: Any?, _ fallback: Double) -> Double { (value as? NSNumber)?.doubleValue ?? fallback }
    private static func nonEmpty(_ value: String?) -> String? { value?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty == false ? value : nil }
}
