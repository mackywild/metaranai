import Foundation

struct GenreLensDefinition: Sendable {
    let name: String
    let vector: MetalVector
    let aliases: [String]
}

enum GenreLensCore {
    static let lenses: [GenreLensDefinition] = [
        .init(name: "Melodic Metal", vector: .init(melody: 0.95, speed: 0.66, heavy: 0.56, symphonic: 0.52, technical: 0.48, growl: 0.18, cleanVocal: 0.90, catchy: 0.88), aliases: ["melodic metal", "melodic"]),
        .init(name: "Power Metal", vector: .init(melody: 0.92, speed: 0.88, heavy: 0.58, symphonic: 0.62, technical: 0.58, growl: 0.08, cleanVocal: 0.95, catchy: 0.90), aliases: ["power metal"]),
        .init(name: "Symphonic Metal", vector: .init(melody: 0.88, speed: 0.64, heavy: 0.60, symphonic: 0.96, technical: 0.58, growl: 0.18, cleanVocal: 0.88, catchy: 0.82), aliases: ["symphonic metal", "symphonic"]),
        .init(name: "Gothic Metal", vector: .init(melody: 0.73, speed: 0.44, heavy: 0.70, symphonic: 0.78, technical: 0.44, growl: 0.40, cleanVocal: 0.75, catchy: 0.60), aliases: ["gothic metal", "gothic"]),
        .init(name: "Metalcore", vector: .init(melody: 0.64, speed: 0.73, heavy: 0.86, symphonic: 0.25, technical: 0.62, growl: 0.70, cleanVocal: 0.55, catchy: 0.64), aliases: ["metalcore"]),
        .init(name: "Melodic Death Metal", vector: .init(melody: 0.83, speed: 0.77, heavy: 0.83, symphonic: 0.46, technical: 0.66, growl: 0.80, cleanVocal: 0.40, catchy: 0.67), aliases: ["melodic death metal", "melodeath"]),
        .init(name: "Progressive Metal", vector: .init(melody: 0.72, speed: 0.58, heavy: 0.72, symphonic: 0.43, technical: 0.94, growl: 0.34, cleanVocal: 0.75, catchy: 0.53), aliases: ["progressive metal", "prog metal", "progressive death metal", "progressive power metal", "technical progressive metal"]),
        .init(name: "Glam Metal", vector: .init(melody: 0.84, speed: 0.66, heavy: 0.58, symphonic: 0.24, technical: 0.43, growl: 0.08, cleanVocal: 0.94, catchy: 0.94), aliases: ["glam metal", "hair metal", "sleaze metal", "sleaze rock", "glam rock"]),
        .init(name: "Japanese Metal", vector: .init(melody: 0.72, speed: 0.64, heavy: 0.66, symphonic: 0.44, technical: 0.55, growl: 0.24, cleanVocal: 0.76, catchy: 0.72), aliases: ["japanese metal", "j-metal", "japanese heavy metal"]),
        .init(name: "Nu Metal", vector: .init(melody: 0.50, speed: 0.45, heavy: 0.90, symphonic: 0.12, technical: 0.42, growl: 0.55, cleanVocal: 0.56, catchy: 0.78), aliases: ["nu metal", "nü metal", "rap metal"]),
        .init(name: "Folk Metal", vector: .init(melody: 0.82, speed: 0.68, heavy: 0.67, symphonic: 0.55, technical: 0.48, growl: 0.35, cleanVocal: 0.73, catchy: 0.77), aliases: ["folk metal"]),
        .init(name: "Doom Metal", vector: .init(melody: 0.62, speed: 0.22, heavy: 0.91, symphonic: 0.35, technical: 0.40, growl: 0.48, cleanVocal: 0.58, catchy: 0.34), aliases: ["doom metal", "doom"]),
        .init(name: "Thrash Metal", vector: .init(melody: 0.48, speed: 0.86, heavy: 0.88, symphonic: 0.15, technical: 0.68, growl: 0.46, cleanVocal: 0.55, catchy: 0.45), aliases: ["thrash metal", "thrash"]),
        .init(name: "Black Metal", vector: .init(melody: 0.55, speed: 0.72, heavy: 0.86, symphonic: 0.48, technical: 0.52, growl: 0.90, cleanVocal: 0.22, catchy: 0.38), aliases: ["black metal"]),
        .init(name: "Death Metal", vector: .init(melody: 0.48, speed: 0.72, heavy: 0.92, symphonic: 0.22, technical: 0.62, growl: 0.94, cleanVocal: 0.12, catchy: 0.33), aliases: ["death metal"]),
        .init(name: "Neoclassical Metal", vector: .init(melody: 0.93, speed: 0.82, heavy: 0.58, symphonic: 0.70, technical: 0.90, growl: 0.08, cleanVocal: 0.91, catchy: 0.84), aliases: ["neoclassical metal", "neoclassical"]),
        .init(name: "Heavy Metal", vector: .init(melody: 0.66, speed: 0.60, heavy: 0.78, symphonic: 0.25, technical: 0.58, growl: 0.28, cleanVocal: 0.78, catchy: 0.62), aliases: ["heavy metal"]),
        .init(name: "Alternative Metal", vector: .init(melody: 0.65, speed: 0.52, heavy: 0.78, symphonic: 0.28, technical: 0.60, growl: 0.38, cleanVocal: 0.72, catchy: 0.70), aliases: ["alternative metal", "alt metal"])
    ]

    static var names: [String] { lenses.map(\.name) }

    static func activeGenres(_ config: GenreLensConfig, date: Date = Date(), calendar: Calendar = .current) -> [String] {
        switch config.mode {
        case .off: return []
        case .manual: return config.manualGenres.sorted()
        case .weekday:
            let symbols = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"]
            let index = max(1, min(7, calendar.component(.weekday, from: date))) - 1
            return Array(config.weekdayGenres[symbols[index]] ?? []).sorted()
        }
    }

    static func matches<C: Collection>(_ artist: MetalArtist, names: C) -> Bool where C.Element == String {
        if names.isEmpty { return true }
        let selected = lenses.filter { names.contains($0.name) }
        if selected.isEmpty { return false }
        let genreText = artist.genres.joined(separator: " | ").lowercased()
        let seed = (artist.sourceSeed ?? "").lowercased()
        let location = "\(artist.country) | \(artist.area ?? "")".lowercased()
        return selected.contains { lens in
            let aliasMatch = lens.aliases.contains { genreText.contains($0.lowercased()) }
            let seedMatch = seed == "genre:\(lens.name)".lowercased()
            let japanese = lens.name == "Japanese Metal" && (location.contains("japan") || location.split(separator: "|").contains { $0.trimmingCharacters(in: .whitespaces) == "jp" })
            return aliasMatch || seedMatch || japanese
        }
    }

    static func score<C: Collection>(_ artist: MetalArtist, names: C) -> Double where C.Element == String {
        if names.isEmpty { return 0 }
        let selected = lenses.filter { names.contains($0.name) }
        guard !selected.isEmpty else { return 0 }
        let text = artist.genres.joined(separator: " ").lowercased()
        return selected.map { lens -> Double in
            if lens.name == "Japanese Metal" && matches(artist, names: [lens.name]) { return 1 }
            let tagBonus = lens.aliases.contains { text.contains($0.lowercased()) } ? 0.12 : 0
            return min(1, artist.vector.similarity(to: lens.vector) * 0.88 + tagBonus)
        }.max() ?? 0
    }

    static func vector<C: Collection>(for names: C) -> MetalVector? where C.Element == String {
        let selected = lenses.filter { names.contains($0.name) }
        guard !selected.isEmpty else { return nil }
        func avg(_ keyPath: KeyPath<MetalVector, Double>) -> Double {
            selected.map { $0.vector[keyPath: keyPath] }.reduce(0, +) / Double(selected.count)
        }
        return MetalVector(melody: avg(\.melody), speed: avg(\.speed), heavy: avg(\.heavy), symphonic: avg(\.symphonic), technical: avg(\.technical), growl: avg(\.growl), cleanVocal: avg(\.cleanVocal), catchy: avg(\.catchy))
    }
}
