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

let neutralDNA = MetalVector(
    melody: 0.5, speed: 0.5, heavy: 0.5, symphonic: 0.5,
    technical: 0.5, growl: 0.5, cleanVocal: 0.5, catchy: 0.5
)
expect(
    DNANameGenerator.generate(profile: neutralDNA, vocal: VocalProfile(), history: []) == "均衡探索型・オールラウンドメタラー",
    "neutral DNA should use balanced explorer name"
)
let dnaProfiles = [
    MetalVector(melody: 0.92, speed: 0.88, heavy: 0.35, symphonic: 0.55, technical: 0.40, growl: 0.20, cleanVocal: 0.72, catchy: 0.70),
    MetalVector(melody: 0.35, speed: 0.45, heavy: 0.94, symphonic: 0.40, technical: 0.55, growl: 0.91, cleanVocal: 0.20, catchy: 0.30),
    MetalVector(melody: 0.70, speed: 0.45, heavy: 0.40, symphonic: 0.93, technical: 0.62, growl: 0.20, cleanVocal: 0.89, catchy: 0.68),
    MetalVector(melody: 0.45, speed: 0.66, heavy: 0.82, symphonic: 0.70, technical: 0.95, growl: 0.32, cleanVocal: 0.38, catchy: 0.42),
    MetalVector(melody: 0.72, speed: 0.48, heavy: 0.30, symphonic: 0.44, technical: 0.35, growl: 0.16, cleanVocal: 0.91, catchy: 0.88),
    MetalVector(melody: 0.48, speed: 0.92, heavy: 0.48, symphonic: 0.55, technical: 0.90, growl: 0.32, cleanVocal: 0.40, catchy: 0.57),
    MetalVector(melody: 0.90, speed: 0.52, heavy: 0.35, symphonic: 0.89, technical: 0.58, growl: 0.18, cleanVocal: 0.73, catchy: 0.65),
    MetalVector(melody: 0.42, speed: 0.90, heavy: 0.84, symphonic: 0.36, technical: 0.48, growl: 0.78, cleanVocal: 0.25, catchy: 0.40)
]
let dnaNames = Set(dnaProfiles.map { DNANameGenerator.generate(profile: $0, vocal: VocalProfile(), history: []) })
expect(dnaNames.count >= 7, "DNA parameter inference should produce broad naming variation")
expect(DNANamePolicy.shouldRegenerate(nextChangeCount: 4) == false, "DNA name should not regenerate before fifth change")
expect(DNANamePolicy.shouldRegenerate(nextChangeCount: 5) == true, "DNA name should regenerate on fifth change")

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
