import Foundation

struct LegacyBackupPayload {
    let version: Int
    let preferences: [String: Any]
}

struct LegacyBackupSummary {
    let version: Int
    let historyCount: Int
    let externalArtistCount: Int
}

enum LegacyBackupError: LocalizedError {
    case invalidFormat
    case missingPreferences
    case invalidRoot

    var errorDescription: String? {
        switch self {
        case .invalidFormat: return "メタらない？のバックアップではありません"
        case .missingPreferences: return "preferencesが見つかりません"
        case .invalidRoot: return "JSONのルート形式が不正です"
        }
    }
}

/// Portable backup contract shared with Android V0.5+.
/// V0.8.0 deliberately does NOT hard-gate the version so older backups remain importable.
enum PortableBackupCodec {
    static let format = "metaranai-backup"
    static let currentVersion = 90

    static let knownPreferenceKeys: Set<String> = [
        "profile", "history", "search_history", "external_artists",
        "genre_lens_v05", "vocal_profile_v05", "spotify_client_id", "lastfm_api_key",
        "spotify_access_token", "spotify_refresh_token", "spotify_token_expiry", "spotify_summary",
        "discovery_summary", "spotify_artist_links_v05", "spotify_artist_links_v061",
        "spotify_artist_links_v062", "spotify_artist_links_v063", "spotify_artist_links_v064",
        "spotify_artist_links_v080"
    ]

    static func decode(data: Data) throws -> LegacyBackupPayload {
        let object = try JSONSerialization.jsonObject(with: data)
        guard let root = object as? [String: Any] else { throw LegacyBackupError.invalidRoot }
        guard root["format"] as? String == format else { throw LegacyBackupError.invalidFormat }
        guard let preferences = root["preferences"] as? [String: Any] else { throw LegacyBackupError.missingPreferences }
        let version = (root["version"] as? NSNumber)?.intValue ?? 0
        return LegacyBackupPayload(version: version, preferences: preferences)
    }

    static func inspect(data: Data) throws -> LegacyBackupSummary {
        let payload = try decode(data: data)
        return LegacyBackupSummary(
            version: payload.version,
            historyCount: countJSONArrayString(payload.preferences["history"] as? String),
            externalArtistCount: countJSONArrayString(payload.preferences["external_artists"] as? String)
        )
    }

    @discardableResult
    static func restore(data: Data, to defaults: UserDefaults = .standard) throws -> LegacyBackupPayload {
        let payload = try decode(data: data)
        var importedKeys = Set(defaults.stringArray(forKey: "metaranai_imported_keys_v080") ?? [])
        for (key, value) in payload.preferences {
            importedKeys.insert(key)
            switch value {
            case let v as String: defaults.set(v, forKey: key)
            case let v as Bool: defaults.set(v, forKey: key)
            case let v as Int: defaults.set(v, forKey: key)
            case let v as Int64: defaults.set(v, forKey: key)
            case let v as Double: defaults.set(v, forKey: key)
            case let v as NSNumber: defaults.set(v, forKey: key)
            case let v as [String]: defaults.set(v, forKey: key)
            default: break
            }
        }
        defaults.set(Array(importedKeys).sorted(), forKey: "metaranai_imported_keys_v080")
        return payload
    }

    static func exportData(
        from defaults: UserDefaults = .standard,
        overrides: [String: Any] = [:],
        version: Int = currentVersion
    ) throws -> Data {
        let imported = Set(defaults.stringArray(forKey: "metaranai_imported_keys_v080") ?? [])
        let keys = knownPreferenceKeys.union(imported).union(overrides.keys)
        var preferences: [String: Any] = [:]
        for key in keys {
            if let override = overrides[key] {
                preferences[key] = override
            } else if let value = defaults.object(forKey: key), isJSONCompatible(value) {
                preferences[key] = value
            }
        }
        let root: [String: Any] = [
            "format": format,
            "version": version,
            "preferences": preferences
        ]
        return try JSONSerialization.data(withJSONObject: root, options: [.prettyPrinted, .sortedKeys])
    }

    private static func isJSONCompatible(_ value: Any) -> Bool {
        value is String || value is Bool || value is NSNumber || value is [String]
    }

    private static func countJSONArrayString(_ raw: String?) -> Int {
        guard let raw, let data = raw.data(using: .utf8),
              let array = try? JSONSerialization.jsonObject(with: data) as? [Any] else { return 0 }
        return array.count
    }
}

/// Compatibility name kept because V0.7.0 starter code and docs referenced it.
typealias LegacyBackupImporter = PortableBackupCodec
