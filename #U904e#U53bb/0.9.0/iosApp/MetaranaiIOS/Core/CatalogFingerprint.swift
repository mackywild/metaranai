import Foundation

enum CatalogFingerprint {
    static func canonicalName(_ value: String) -> String {
        value.precomposedStringWithCompatibilityMapping
            .lowercased()
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .split(whereSeparator: { $0.isWhitespace })
            .joined(separator: " ")
    }

    static func canonicalTrack(_ value: String) -> String {
        var text = value.precomposedStringWithCompatibilityMapping.lowercased()
        text = text.replacingOccurrences(of: #"^\s*\d{1,2}[\s._#:-]+"#, with: "", options: .regularExpression)
        text = text.replacingOccurrences(of: #"[\(\[][^\)\]]*(remaster(?:ed)?|live|version|edit|mix|demo|acoustic|instrumental)[^\)\]]*[\)\]]"#, with: " ", options: .regularExpression)
        text = text.replacingOccurrences(of: "&", with: " and ")
        text = text.replacingOccurrences(of: #"[^\p{L}\p{N}]+"#, with: " ", options: .regularExpression)
        return normalizeSpaces(text)
    }

    static func canonicalAlbum(_ value: String) -> String {
        var text = value.precomposedStringWithCompatibilityMapping.lowercased()
        text = text.replacingOccurrences(of: #"[\(\[][^\)\]]*(remaster(?:ed)?|deluxe|expanded|edition|version)[^\)\]]*[\)\]]"#, with: " ", options: .regularExpression)
        text = text.replacingOccurrences(of: "&", with: " and ")
        text = text.replacingOccurrences(of: #"[^\p{L}\p{N}]+"#, with: " ", options: .regularExpression)
        return normalizeSpaces(text)
    }

    static func trackMatches(_ left: [String], _ right: [String]) -> [String] {
        matching(left, right, canonical: canonicalTrack)
    }

    static func albumMatches(_ left: [String], _ right: [String]) -> [String] {
        matching(left, right, canonical: canonicalAlbum)
    }

    static func evidenceScore(trackMatches: Int, albumMatches: Int) -> Int {
        trackMatches * 3 + albumMatches * 2
    }

    static func isStrong(trackMatches: Int, albumMatches: Int) -> Bool {
        trackMatches >= 2 || albumMatches >= 2 || (trackMatches >= 1 && albumMatches >= 1)
    }

    private static func matching(_ left: [String], _ right: [String], canonical: (String) -> String) -> [String] {
        let remote = Set(right.map(canonical).filter(usefulFingerprint))
        return Array(Set(left.map(canonical).filter(usefulFingerprint).filter { remote.contains($0) }))
    }

    private static func usefulFingerprint(_ value: String) -> Bool {
        guard value.count >= 4 else { return false }
        let generic: Set<String> = ["intro", "outro", "interlude", "overture", "reprise", "instrumental", "untitled", "bonus track", "prologue", "epilogue"]
        return !generic.contains(value)
    }

    private static func normalizeSpaces(_ value: String) -> String {
        value.trimmingCharacters(in: .whitespacesAndNewlines)
            .split(whereSeparator: { $0.isWhitespace })
            .joined(separator: " ")
    }
}
