import Foundation

struct MetalVector: Equatable, Sendable {
    var melody: Double
    var speed: Double
    var heavy: Double
    var symphonic: Double
    var technical: Double
    var growl: Double
    var cleanVocal: Double
    var catchy: Double

    static let metaranaiDefault = MetalVector(
        melody: 0.50, speed: 0.50, heavy: 0.50, symphonic: 0.50,
        technical: 0.50, growl: 0.50, cleanVocal: 0.50, catchy: 0.50
    )

    func similarity(to other: MetalVector) -> Double {
        let pairs = [
            (melody, other.melody), (speed, other.speed), (heavy, other.heavy),
            (symphonic, other.symphonic), (technical, other.technical),
            (growl, other.growl), (cleanVocal, other.cleanVocal), (catchy, other.catchy)
        ]
        let distance = pairs.map { abs($0.0 - $0.1) }.reduce(0, +) / Double(pairs.count)
        return min(1, max(0, 1 - distance))
    }

    func blended(with other: MetalVector, weight: Double) -> MetalVector {
        func mix(_ a: Double, _ b: Double) -> Double { min(1, max(0, a * (1 - weight) + b * weight)) }
        return MetalVector(
            melody: mix(melody, other.melody), speed: mix(speed, other.speed),
            heavy: mix(heavy, other.heavy), symphonic: mix(symphonic, other.symphonic),
            technical: mix(technical, other.technical), growl: mix(growl, other.growl),
            cleanVocal: mix(cleanVocal, other.cleanVocal), catchy: mix(catchy, other.catchy)
        )
    }

    func movedAway(from other: MetalVector, weight: Double) -> MetalVector {
        func away(_ a: Double, _ b: Double) -> Double { min(1, max(0, a + (a - b) * weight)) }
        return MetalVector(
            melody: away(melody, other.melody), speed: away(speed, other.speed),
            heavy: away(heavy, other.heavy), symphonic: away(symphonic, other.symphonic),
            technical: away(technical, other.technical), growl: away(growl, other.growl),
            cleanVocal: away(cleanVocal, other.cleanVocal), catchy: away(catchy, other.catchy)
        )
    }

    var traits: [(String, Double)] {
        [
            ("メロディ", melody), ("疾走", speed), ("ヘヴィ", heavy),
            ("シンフォニック", symphonic), ("技巧", technical), ("グロウル", growl),
            ("クリーンVo", cleanVocal), ("キャッチー", catchy)
        ]
    }
}

enum Reaction: String, CaseIterable, Sendable {
    case loveAll = "LOVE_ALL"
    case hit = "HIT"
    case some = "SOME"
    case meh = "MEH"
    case noInterest = "NO_INTEREST"
    case notFound = "NOT_FOUND"

    var label: String {
        switch self {
        case .loveAll: return "💘 全部好き"
        case .hit: return "🔥 普通に刺さる"
        case .some: return "🎵 何曲か刺さる"
        case .meh: return "😐 イマイチ"
        case .noInterest: return "💀 興味なし"
        case .notFound: return "🔍 見つからなかった"
        }
    }

    var affinityScore: Int {
        switch self {
        case .loveAll: return 100
        case .hit: return 80
        case .some: return 60
        case .meh: return 30
        case .noInterest: return 0
        case .notFound: return 0
        }
    }

    static func compatible(raw: String) -> Reaction? {
        switch raw.uppercased() {
        case "LOVE_ALL": return .loveAll
        case "HIT": return .hit
        case "SOME", "MAYBE": return .some
        case "MEH", "MISS": return .meh
        case "NO_INTEREST": return .noInterest
        case "NOT_FOUND": return .notFound
        default: return nil
        }
    }
}

struct DiscoveryRecord: Identifiable, Equatable, Sendable {
    let id: UUID
    var artistName: String
    var date: String
    var reaction: Reaction
    var score: Int

    init(id: UUID = UUID(), artistName: String, date: String, reaction: Reaction, score: Int) {
        self.id = id
        self.artistName = artistName
        self.date = date
        self.reaction = reaction
        self.score = score
    }
}

struct SearchRecord: Equatable, Sendable {
    var query: String
    var artistName: String
    var dateTime: String
}

enum VocalType: String, CaseIterable, Sendable {
    case male = "MALE"
    case female = "FEMALE"
    case mixed = "MIXED"
    case unknown = "UNKNOWN"

    var label: String {
        switch self {
        case .male: return "男性Vo"
        case .female: return "女性Vo"
        case .mixed: return "混成Vo"
        case .unknown: return "Vo未判定"
        }
    }
}

struct VocalProfile: Equatable, Sendable {
    var male: Double = 0.333
    var female: Double = 0.333
    var mixed: Double = 0.334
    var observations: Int = 0
}

struct MetalArtist: Identifiable, Equatable, Sendable {
    var id: String {
        if let mbid, !mbid.isEmpty { return "mbid:\(mbid.lowercased())" }
        return "name:\(Self.normalizeName(name))|\(country.lowercased())"
    }

    var name: String
    var country: String
    var genres: [String]
    var vector: MetalVector
    var discovery: Double
    var reason: String
    var source: String
    var sourceSeed: String?
    var externalScore: Int?
    var lastFmListeners: Int64?
    var lastFmPlaycount: Int64?
    var mbid: String?
    var area: String?
    var beginDate: String?
    var endDate: String?
    var ended: Bool?
    var hiddenScore: Int
    var metadataConfidence: Int
    var vocalType: VocalType

    static func normalizeName(_ value: String) -> String {
        value.precomposedStringWithCompatibilityMapping
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()
            .split(whereSeparator: { $0.isWhitespace })
            .joined(separator: " ")
    }
}

enum GenreLensMode: String, Sendable {
    case off = "OFF"
    case weekday = "WEEKDAY"
    case manual = "MANUAL"
}

struct GenreLensConfig: Equatable, Sendable {
    var mode: GenreLensMode = .off
    var manualGenres: Set<String> = []
    var weekdayGenres: [String: Set<String>] = [:]
}

struct RecommendationBreakdown: Equatable, Sendable {
    var affinity: Int
    var genreLens: Int
    var hidden: Int
    var novelty: Int
    var exploration: Int
    var discovery: Int
    var total: Int
}

struct Recommendation: Equatable, Sendable {
    var artist: MetalArtist
    var compatibility: Int
    var reason: String
    var breakdown: RecommendationBreakdown
    var matchedTraits: [String]
    var activeGenres: [String]
}

struct SpotifyDestination: Equatable, Sendable {
    var url: URL
    var direct: Bool
    var artistID: String?
    var verification: String
}
