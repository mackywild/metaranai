import Foundation

func expect(_ condition: @autoclosure () -> Bool, _ message: String) {
    if !condition() {
        fputs("FAIL: \(message)\n", stderr)
        exit(1)
    }
}

guard CommandLine.arguments.count >= 2 else {
    fputs("fixture path required\n", stderr)
    exit(2)
}
let data = try Data(contentsOf: URL(fileURLWithPath: CommandLine.arguments[1]))
let payload = try PortableBackupCodec.decode(data: data)
expect(payload.version == 64, "legacy backup version should be readable")
let summary = try PortableBackupCodec.inspect(data: data)
expect(summary.historyCount == 2, "history count")
expect(summary.externalArtistCount == 2, "external count")
let prefs = payload.preferences
let profile = MetalDataParser.profile(from: prefs["profile"] as? String)
let history = MetalDataParser.history(from: prefs["history"] as? String)
let artists = MetalDataParser.externalArtists(from: prefs["external_artists"] as? String)
let lens = MetalDataParser.genreLens(from: prefs["genre_lens_v05"] as? String)
expect(history.count == 2, "history parse")
expect(artists.count == 2, "artist parse")
expect(lens.manualGenres.contains("Thrash Metal"), "lens parse")
expect(Reaction.compatible(raw: "MAYBE") == .some, "legacy MAYBE -> SOME")
expect(Reaction.compatible(raw: "MISS") == .meh, "legacy MISS -> MEH")
let active = GenreLensCore.activeGenres(lens)
expect(active == ["Thrash Metal"], "active manual genre")
expect(GenreLensCore.matches(artists[1], names: ["Thrash Metal"]), "strict lens membership")
let rec = RecommendationCore.recommend(profile: profile, history: [], candidates: artists, genreLens: ["Thrash Metal"], seed: 0)
expect(rec?.artist.name == "Underground Thrash", "strict recommendation must stay in lens")
let updated = RecommendationCore.updatedProfile(current: profile, artist: artists[1], reaction: .hit)
expect(updated != profile, "rating updates DNA")

let suite = "MetaranaiV090CoreTests-\(UUID().uuidString)"
guard let defaults = UserDefaults(suiteName: suite) else { fatalError("defaults") }
defaults.removePersistentDomain(forName: suite)
_ = try PortableBackupCodec.restore(data: data, to: defaults)
let exported = try PortableBackupCodec.exportData(from: defaults, version: 90)
let round = try PortableBackupCodec.decode(data: exported)
expect(round.version == 90, "new backup version")
expect((round.preferences["history"] as? String) == (prefs["history"] as? String), "history survives roundtrip")
expect((round.preferences["external_artists"] as? String) == (prefs["external_artists"] as? String), "artist DB survives roundtrip")


// Spotify identity regression: Yutaro Abe's ASTRAL WIND
let astralTarget = "Yutaro Abe's ASTRAL WIND"
expect(CatalogFingerprint.canonicalName(astralTarget) == "yutaro abe's astral wind", "canonical artist name")
expect(CatalogFingerprint.canonicalName("Yutaro Abe") != CatalogFingerprint.canonicalName(astralTarget), "partial artist name must not equal target")

let astralReferenceTracks = [
    "Wings Of Heart",
    "Infernal Will",
    "Forever Gone",
    "Blazing Heart Of Sorrow",
    "UNCHAINED -Soul Of The Brave-",
    "In The World Of Saints",
    "BWV1041"
]
let astralSpotifyTracks = [
    "Wings Of Heart",
    "Infernal Will",
    "Forever Gone",
    "Blazing Heart Of Sorrow",
    "UNCHAINED-Soul Of The Brave-",
    "In The World Of Saints",
    "BWV1041"
]
let astralReferenceAlbums = ["Cycle Of Life", "UNCHAINED -Soul Of The Brave-"]
let astralSpotifyAlbums = ["Cycle Of Life", "UNCHAINED-Soul Of The Brave-"]
let astralTrackMatches = CatalogFingerprint.trackMatches(astralReferenceTracks, astralSpotifyTracks)
let astralAlbumMatches = CatalogFingerprint.albumMatches(astralReferenceAlbums, astralSpotifyAlbums)
let astralScore = CatalogFingerprint.evidenceScore(trackMatches: astralTrackMatches.count, albumMatches: astralAlbumMatches.count)
expect(astralTrackMatches.count >= 7, "ASTRAL WIND track fingerprint")
expect(astralAlbumMatches.count >= 2, "ASTRAL WIND album fingerprint")
expect(CatalogFingerprint.isStrong(trackMatches: astralTrackMatches.count, albumMatches: astralAlbumMatches.count), "ASTRAL WIND must be strong identity evidence")
expect(astralScore >= 25, "ASTRAL WIND identity score")

let wrongSameNameTracks = ["Sunrise", "Night Drive", "Blue Sky"]
let wrongSameNameAlbums = ["First Steps"]
let wrongTrackMatches = CatalogFingerprint.trackMatches(astralReferenceTracks, wrongSameNameTracks)
let wrongAlbumMatches = CatalogFingerprint.albumMatches(astralReferenceAlbums, wrongSameNameAlbums)
let wrongScore = CatalogFingerprint.evidenceScore(trackMatches: wrongTrackMatches.count, albumMatches: wrongAlbumMatches.count)
expect(wrongScore == 0, "same-name wrong artist must not fingerprint-match")
expect(astralScore >= wrongScore + 3, "ASTRAL WIND must uniquely beat same-name wrong artist")

defaults.removePersistentDomain(forName: suite)
print("V090_IOS_CORE_OK")
print("legacy JSON -> iOS parse -> strict Genre Lens -> export roundtrip OK")
