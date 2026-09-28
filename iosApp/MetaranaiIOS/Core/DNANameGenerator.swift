import Foundation

enum DNANamePolicy {
    static let regenerationInterval = 5

    static func shouldRegenerate(nextChangeCount: Int) -> Bool {
        nextChangeCount >= regenerationInterval
    }
}

enum DNANameGenerator {
    static let version = 2

    private struct Axis {
        let key: String
        let value: Double
        let accent: String
    }

    static func generate(profile: MetalVector, vocal: VocalProfile, history: [DiscoveryRecord]) -> String {
        let axes = [
            Axis(key: "melody", value: profile.melody, accent: "叙情"),
            Axis(key: "speed", value: profile.speed, accent: "加速"),
            Axis(key: "heavy", value: profile.heavy, accent: "重厚"),
            Axis(key: "symphonic", value: profile.symphonic, accent: "荘厳"),
            Axis(key: "technical", value: profile.technical, accent: "精密"),
            Axis(key: "growl", value: profile.growl, accent: "獰猛"),
            Axis(key: "clean", value: profile.cleanVocal, accent: "清麗"),
            Axis(key: "catchy", value: profile.catchy, accent: "歌心")
        ]

        let ranked = axes.sorted {
            if $0.value == $1.value { return $0.key < $1.key }
            return $0.value > $1.value
        }
        let top = ranked[0]
        let second = ranked[1]
        let third = ranked[2]
        let spread = top.value - ranked[ranked.count - 1].value
        let pairMean = (top.value + second.value) / 2

        let core: String
        if spread <= 0.08 && top.value <= 0.66 {
            core = "均衡探索型・オールラウンドメタラー"
        } else {
            let shape: String
            if top.value >= 0.86 && top.value - second.value >= 0.12 {
                shape = "一点突破型"
            } else if pairMean >= 0.78 {
                shape = "双極覚醒型"
            } else if third.value >= 0.70 && top.value - third.value <= 0.14 {
                shape = "多軸融合型"
            } else if spread <= 0.16 {
                shape = "全方位融合型"
            } else {
                shape = "偏愛融合型"
            }

            let thirdAccent = third.value >= 0.66 && top.value - third.value <= 0.20
                ? "・\(third.accent)アクセント"
                : ""
            core = "\(shape)・\(pairPhrase(top.key, second.key))\(thirdAccent)・\(archetype(profile))"
        }

        var qualifiers: [String] = []
        if let listening = listeningQualifier(history) { qualifiers.append(listening) }
        if let vocal = vocalQualifier(vocal) { qualifiers.append(vocal) }
        qualifiers.append(core)
        return qualifiers.joined(separator: "・")
    }

    private static let pairPhrases: [Set<String>: String] = [
        ["melody", "speed"]: "旋律疾走",
        ["melody", "heavy"]: "叙情重圧",
        ["melody", "symphonic"]: "幻想旋律",
        ["melody", "technical"]: "構築旋律",
        ["melody", "growl"]: "哀愁咆哮",
        ["melody", "clean"]: "清麗旋律",
        ["melody", "catchy"]: "歌心旋律",
        ["speed", "heavy"]: "疾走重装",
        ["speed", "symphonic"]: "荘厳疾走",
        ["speed", "technical"]: "高速技巧",
        ["speed", "growl"]: "暴走咆哮",
        ["speed", "clean"]: "清唱疾走",
        ["speed", "catchy"]: "疾走昂揚",
        ["heavy", "symphonic"]: "荘厳重圧",
        ["heavy", "technical"]: "重装技巧",
        ["heavy", "growl"]: "極重咆哮",
        ["heavy", "clean"]: "重厚清唱",
        ["heavy", "catchy"]: "重圧昂揚",
        ["symphonic", "technical"]: "構築幻想",
        ["symphonic", "growl"]: "深淵荘厳",
        ["symphonic", "clean"]: "劇場清唱",
        ["symphonic", "catchy"]: "荘厳歌心",
        ["technical", "growl"]: "精密咆哮",
        ["technical", "clean"]: "技巧清麗",
        ["technical", "catchy"]: "構築昂揚",
        ["growl", "clean"]: "双声二面",
        ["growl", "catchy"]: "獰猛昂揚",
        ["clean", "catchy"]: "清唱歌心"
    ]

    private static func pairPhrase(_ a: String, _ b: String) -> String {
        pairPhrases[Set([a, b])] ?? "\(fallbackToken(a))\(fallbackToken(b))"
    }

    private static func fallbackToken(_ key: String) -> String {
        switch key {
        case "melody": return "旋律"
        case "speed": return "疾走"
        case "heavy": return "重圧"
        case "symphonic": return "荘厳"
        case "technical": return "技巧"
        case "growl": return "咆哮"
        case "clean": return "清唱"
        case "catchy": return "歌心"
        default: return "探索"
        }
    }

    private static func archetype(_ v: MetalVector) -> String {
        let scores: [(String, Double)] = [
            ("メロディックメタラー", average(v.melody, v.catchy, v.cleanVocal)),
            ("パワーメタラー", average(v.speed, v.melody)),
            ("シンフォニックメタラー", average(v.symphonic, v.cleanVocal, v.melody)),
            ("プログレッシブメタラー", average(v.technical, v.symphonic, v.heavy)),
            ("エクストリームメタラー", average(v.heavy, v.growl, v.speed)),
            ("ヘヴィメタラー", average(v.heavy, v.technical))
        ]
        return scores.max(by: { $0.1 < $1.1 })?.0 ?? "オールラウンドメタラー"
    }

    private static func listeningQualifier(_ history: [DiscoveryRecord]) -> String? {
        let judged = history.filter { $0.reaction != .notFound }
        guard judged.count >= 8 else { return nil }

        let total = Double(judged.count)
        let love = Double(judged.filter { $0.reaction == .loveAll }.count) / total
        let positive = Double(judged.filter { $0.reaction == .loveAll || $0.reaction == .hit }.count) / total
        let selective = Double(judged.filter { $0.reaction == .some }.count) / total
        let reject = Double(judged.filter { $0.reaction == .meh || $0.reaction == .noInterest }.count) / total

        if love >= 0.30 { return "全曲没入型" }
        if positive >= 0.72 { return "高打率共鳴型" }
        if selective >= 0.38 { return "選曲発掘型" }
        if reject >= 0.45 { return "厳選審美型" }
        if judged.count >= 20 { return "長期探索型" }
        return nil
    }

    private static func vocalQualifier(_ vocal: VocalProfile) -> String? {
        guard vocal.observations >= 3 else { return nil }
        let total = max(0.001, vocal.male + vocal.female + vocal.mixed)
        let candidates = [
            ("男性Vo偏愛", vocal.male / total),
            ("女性Vo偏愛", vocal.female / total),
            ("混成Vo偏愛", vocal.mixed / total)
        ]
        guard let best = candidates.max(by: { $0.1 < $1.1 }), best.1 >= 0.46 else { return nil }
        return best.0
    }

    private static func average(_ values: Double...) -> Double {
        values.reduce(0, +) / Double(max(1, values.count))
    }
}
