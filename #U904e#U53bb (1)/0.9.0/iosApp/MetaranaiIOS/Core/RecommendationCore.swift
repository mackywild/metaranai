import Foundation

enum RecommendationCore {
    static func recommend(
        profile: MetalVector,
        history: [DiscoveryRecord],
        searchHistory: [SearchRecord] = [],
        candidates: [MetalArtist],
        genreLens: [String] = [],
        seed: Int? = nil
    ) -> Recommendation? {
        guard !candidates.isEmpty else { return nil }
        let seen = Set(history.map { MetalArtist.normalizeName($0.artistName) })
        let searched = Set(searchHistory.prefix(30).map { MetalArtist.normalizeName($0.artistName) })
        let lensActive = !genreLens.isEmpty
        let unique = Dictionary(grouping: candidates, by: { MetalArtist.normalizeName($0.name) }).compactMap { $0.value.first }
        let eligible = lensActive ? unique.filter { GenreLensCore.matches($0, names: genreLens) } : unique
        guard !eligible.isEmpty else { return nil }

        let ranked: [(MetalArtist, Double)] = eligible.map { artist in
            let similarity = profile.similarity(to: artist.vector)
            let lensScore = GenreLensCore.score(artist, names: genreLens)
            let novelty = seen.contains(MetalArtist.normalizeName(artist.name)) ? 0.0 : 1.0
            let interest = searched.contains(MetalArtist.normalizeName(artist.name)) ? 0.3 : 0.0
            let exploration = explorationScore(profile: profile, artist: artist.vector)
            let hidden = Double(max(0, min(100, artist.hiddenScore))) / 100
            let discovery = min(1, max(0, artist.discovery))
            let score = lensActive
                ? similarity * 0.52 + lensScore * 0.08 + hidden * 0.16 + novelty * 0.11 + exploration * 0.07 + discovery * 0.04 + interest * 0.02
                : similarity * 0.50 + hidden * 0.20 + novelty * 0.15 + exploration * 0.10 + discovery * 0.04 + interest * 0.01
            return (artist, score)
        }.sorted { $0.1 > $1.1 }

        let unseen = ranked.filter { !seen.contains(MetalArtist.normalizeName($0.0.name)) }
        let pool = Array((unseen.isEmpty ? ranked : unseen).prefix(12))
        guard !pool.isEmpty else { return nil }
        let indexSeed = seed ?? Calendar.current.ordinality(of: .day, in: .era, for: Date()) ?? 0
        let selected = pool[abs(indexSeed) % pool.count].0
        let similarity = profile.similarity(to: selected.vector)
        let novelty = seen.contains(MetalArtist.normalizeName(selected.name)) ? 0.0 : 1.0
        let exploration = explorationScore(profile: profile, artist: selected.vector)
        let hidden = Double(max(0, min(100, selected.hiddenScore))) / 100
        let lensScore = GenreLensCore.score(selected, names: genreLens)
        let breakdown = lensActive
            ? RecommendationBreakdown(
                affinity: Int((similarity * 52).rounded()), genreLens: Int((lensScore * 8).rounded()),
                hidden: Int((hidden * 16).rounded()), novelty: Int((novelty * 11).rounded()),
                exploration: Int((exploration * 7).rounded()), discovery: Int((selected.discovery * 4).rounded()),
                total: Int((similarity * 52 + lensScore * 8 + hidden * 16 + novelty * 11 + exploration * 7 + selected.discovery * 4).rounded())
            )
            : RecommendationBreakdown(
                affinity: Int((similarity * 50).rounded()), genreLens: 0,
                hidden: Int((hidden * 20).rounded()), novelty: Int((novelty * 15).rounded()),
                exploration: Int((exploration * 10).rounded()), discovery: Int((selected.discovery * 5).rounded()),
                total: Int((similarity * 50 + hidden * 20 + novelty * 15 + exploration * 10 + selected.discovery * 5).rounded())
            )
        let traits = strongestMatches(profile, selected.vector)
        let lensText = genreLens.isEmpty ? "" : "。Genre Lens必須条件: \(genreLens.joined(separator: " / "))"
        return Recommendation(
            artist: selected,
            compatibility: Int((similarity * 100).rounded()),
            reason: "\(selected.reason)。\(traits.joined(separator: "・"))があなたの傾向と近い。Hidden Score \(selected.hiddenScore)\(lensText)。",
            breakdown: breakdown,
            matchedTraits: traits,
            activeGenres: genreLens
        )
    }

    static func updatedProfile(current: MetalVector, artist: MetalArtist, reaction: Reaction) -> MetalVector {
        switch reaction {
        case .loveAll: return current.blended(with: artist.vector, weight: 0.28)
        case .hit: return current.blended(with: artist.vector, weight: 0.18)
        case .some: return current.blended(with: artist.vector, weight: 0.07)
        case .meh: return current.movedAway(from: artist.vector, weight: 0.06)
        case .noInterest: return current.movedAway(from: artist.vector, weight: 0.18)
        }
    }

    static func dnaType(profile v: MetalVector, vocal: VocalProfile, history: [DiscoveryRecord]) -> String {
        let melodic = (v.melody + v.catchy + v.cleanVocal) / 3
        let base: String
        if melodic > 0.88 && v.speed > 0.78 && v.symphonic > 0.72 { base = "天空疾走型メロディックメタラー" }
        else if v.symphonic > 0.86 && v.cleanVocal > 0.82 { base = "劇場型シンフォニックメタラー" }
        else if v.technical > 0.82 && v.heavy > 0.65 { base = "技巧偏重型プログレッシブメタラー" }
        else if v.growl > 0.62 && v.heavy > 0.78 { base = "極重圧型エクストリームメタラー" }
        else if v.speed > 0.82 { base = "高速巡航型パワーメタラー" }
        else if melodic > 0.82 { base = "旋律至上型メロディックメタラー" }
        else { base = "探索型オールラウンドメタラー" }

        var qualifiers: [String] = []
        if history.count >= 8 {
            let count = Double(history.count)
            let love = Double(history.filter { $0.reaction == .loveAll }.count) / count
            let selective = Double(history.filter { $0.reaction == .some }.count) / count
            let reject = Double(history.filter { $0.reaction == .meh || $0.reaction == .noInterest }.count) / count
            if love >= 0.25 { qualifiers.append("全曲没入型") }
            else if selective >= 0.40 { qualifiers.append("選曲発掘型") }
            else if reject >= 0.45 { qualifiers.append("厳選審美型") }
        }
        if vocal.observations >= 3 {
            let values = [("男性Vo偏愛", vocal.male), ("女性Vo偏愛", vocal.female), ("混成Vo偏愛", vocal.mixed)]
            if let best = values.max(by: { $0.1 < $1.1 }), best.1 >= 0.46 { qualifiers.append(best.0) }
        }
        return qualifiers.isEmpty ? base : "\(qualifiers.joined(separator: "・"))・\(base)"
    }

    private static func explorationScore(profile: MetalVector, artist: MetalVector) -> Double {
        let similarity = profile.similarity(to: artist)
        return min(1, max(0, 1 - abs(similarity - 0.80) / 0.80))
    }

    private static func strongestMatches(_ a: MetalVector, _ b: MetalVector) -> [String] {
        struct ScoredTrait { let name: String; let closeness: Double; let intensity: Double }
        let left = a.traits
        let right = b.traits
        var scored: [ScoredTrait] = []
        for index in 0..<min(left.count, right.count) {
            let l = left[index]
            let r = right[index]
            let item = ScoredTrait(name: l.0, closeness: 1 - abs(l.1 - r.1), intensity: (l.1 + r.1) / 2)
            if item.intensity > 0.55 { scored.append(item) }
        }
        scored.sort {
            ($0.closeness * 0.65 + $0.intensity * 0.35) > ($1.closeness * 0.65 + $1.intensity * 0.35)
        }
        return Array(scored.prefix(3).map(\.name))
    }
}
