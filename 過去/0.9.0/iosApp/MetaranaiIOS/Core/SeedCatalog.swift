import Foundation

enum SeedCatalog {
    static let artists: [MetalArtist] = [
        MetalArtist(
            name: "SKYWINGS", country: "Japan", genres: ["Melodic Power Metal", "Symphonic"],
            vector: .init(melody: 0.98, speed: 0.86, heavy: 0.55, symphonic: 0.88, technical: 0.62, growl: 0.04, cleanVocal: 0.98, catchy: 0.96),
            discovery: 0.82, reason: "高密度のクサメロと疾走感", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .male
        ),
        MetalArtist(
            name: "MinstreliX", country: "Japan", genres: ["Melodic Power Metal"],
            vector: .init(melody: 0.96, speed: 0.90, heavy: 0.58, symphonic: 0.75, technical: 0.64, growl: 0.08, cleanVocal: 0.96, catchy: 0.91),
            discovery: 0.86, reason: "国産メロスピの美旋律をさらに掘る", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .male
        ),
        MetalArtist(
            name: "Syu", country: "Japan", genres: ["Power Metal", "Instrumental"],
            vector: .init(melody: 0.88, speed: 0.82, heavy: 0.62, symphonic: 0.55, technical: 0.91, growl: 0.02, cleanVocal: 0.90, catchy: 0.80),
            discovery: 0.72, reason: "技巧派だがメロディ中心", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .unknown
        ),
        MetalArtist(
            name: "Unlucky Morpheus", country: "Japan", genres: ["Power Metal", "Symphonic"],
            vector: .init(melody: 0.91, speed: 0.91, heavy: 0.72, symphonic: 0.83, technical: 0.88, growl: 0.18, cleanVocal: 0.88, catchy: 0.86),
            discovery: 0.64, reason: "疾走・技巧・シンフォニックの交点", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .unknown
        ),
        MetalArtist(
            name: "Versailles", country: "Japan", genres: ["Symphonic Power Metal"],
            vector: .init(melody: 0.91, speed: 0.78, heavy: 0.62, symphonic: 0.94, technical: 0.72, growl: 0.08, cleanVocal: 0.93, catchy: 0.88),
            discovery: 0.58, reason: "華麗さと劇的メロディを重視", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .male
        ),
        MetalArtist(
            name: "Lovebites", country: "Japan", genres: ["Power Metal", "Heavy Metal"],
            vector: .init(melody: 0.84, speed: 0.88, heavy: 0.72, symphonic: 0.54, technical: 0.78, growl: 0.04, cleanVocal: 0.97, catchy: 0.82),
            discovery: 0.45, reason: "クリーンVoと高速ツインギター", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .female
        ),
        MetalArtist(
            name: "Twilight Force", country: "Sweden", genres: ["Symphonic Power Metal"],
            vector: .init(melody: 0.94, speed: 0.90, heavy: 0.52, symphonic: 0.96, technical: 0.64, growl: 0.01, cleanVocal: 0.99, catchy: 0.94),
            discovery: 0.48, reason: "明るいクサメロと大仰なシンフォニー", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .male
        ),
        MetalArtist(
            name: "Fellowship", country: "United Kingdom", genres: ["Power Metal"],
            vector: .init(melody: 0.96, speed: 0.82, heavy: 0.48, symphonic: 0.72, technical: 0.58, growl: 0.00, cleanVocal: 0.99, catchy: 0.98),
            discovery: 0.67, reason: "多幸感のあるメロディとキャッチーさ", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .male
        ),
        MetalArtist(
            name: "Majestica", country: "Sweden", genres: ["Power Metal"],
            vector: .init(melody: 0.93, speed: 0.88, heavy: 0.53, symphonic: 0.68, technical: 0.67, growl: 0.01, cleanVocal: 0.99, catchy: 0.95),
            discovery: 0.62, reason: "明快なサビと疾走パワーメタル", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .male
        ),
        MetalArtist(
            name: "Dynazty", country: "Sweden", genres: ["Melodic Metal"],
            vector: .init(melody: 0.88, speed: 0.68, heavy: 0.66, symphonic: 0.45, technical: 0.60, growl: 0.02, cleanVocal: 0.99, catchy: 0.96),
            discovery: 0.50, reason: "モダンさを保ちつつサビが強い", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .unknown
        ),
        MetalArtist(
            name: "Keldian", country: "Norway", genres: ["Power Metal", "Melodic Metal"],
            vector: .init(melody: 0.92, speed: 0.75, heavy: 0.54, symphonic: 0.58, technical: 0.62, growl: 0.02, cleanVocal: 0.97, catchy: 0.91),
            discovery: 0.89, reason: "知名度控えめでメロディ偏重", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .male
        ),
        MetalArtist(
            name: "Pathfinder", country: "Poland", genres: ["Symphonic Power Metal"],
            vector: .init(melody: 0.92, speed: 0.94, heavy: 0.62, symphonic: 0.97, technical: 0.68, growl: 0.10, cleanVocal: 0.89, catchy: 0.85),
            discovery: 0.91, reason: "爆速と映画的シンフォニー", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .unknown
        ),
        MetalArtist(
            name: "Heavenly", country: "France", genres: ["Power Metal"],
            vector: .init(melody: 0.96, speed: 0.91, heavy: 0.56, symphonic: 0.60, technical: 0.63, growl: 0.02, cleanVocal: 0.98, catchy: 0.93),
            discovery: 0.80, reason: "クラシックな欧州メロスピ直球", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .unknown
        ),
        MetalArtist(
            name: "Dreamtale", country: "Finland", genres: ["Power Metal"],
            vector: .init(melody: 0.92, speed: 0.85, heavy: 0.55, symphonic: 0.58, technical: 0.58, growl: 0.04, cleanVocal: 0.94, catchy: 0.91),
            discovery: 0.76, reason: "フィンランド系の透明感あるメロディ", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .unknown
        ),
        MetalArtist(
            name: "Ancient Bards", country: "Italy", genres: ["Symphonic Power Metal"],
            vector: .init(melody: 0.91, speed: 0.84, heavy: 0.62, symphonic: 0.96, technical: 0.70, growl: 0.12, cleanVocal: 0.91, catchy: 0.86),
            discovery: 0.79, reason: "物語性とシンフォニック密度", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .unknown
        ),
        MetalArtist(
            name: "Temperance", country: "Italy", genres: ["Melodic Metal", "Symphonic"],
            vector: .init(melody: 0.89, speed: 0.71, heavy: 0.58, symphonic: 0.84, technical: 0.61, growl: 0.05, cleanVocal: 0.98, catchy: 0.94),
            discovery: 0.70, reason: "男女クリーンVoとキャッチーな旋律", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .mixed
        ),
        MetalArtist(
            name: "Frozen Crown", country: "Italy", genres: ["Power Metal"],
            vector: .init(melody: 0.86, speed: 0.89, heavy: 0.69, symphonic: 0.54, technical: 0.75, growl: 0.17, cleanVocal: 0.88, catchy: 0.85),
            discovery: 0.66, reason: "疾走とヘヴィさのバランス", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .unknown
        ),
        MetalArtist(
            name: "Seven Spires", country: "USA", genres: ["Symphonic Metal"],
            vector: .init(melody: 0.86, speed: 0.69, heavy: 0.78, symphonic: 0.92, technical: 0.79, growl: 0.48, cleanVocal: 0.72, catchy: 0.78),
            discovery: 0.77, reason: "少し冒険したい日に刺さる劇的サウンド", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .female
        ),
        MetalArtist(
            name: "Diablo Swing Orchestra", country: "Sweden", genres: ["Avant-garde Metal", "Swing"],
            vector: .init(melody: 0.81, speed: 0.52, heavy: 0.68, symphonic: 0.66, technical: 0.82, growl: 0.21, cleanVocal: 0.84, catchy: 0.91),
            discovery: 0.73, reason: "ジャズ／スウィング要素を持つ異色枠", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .unknown
        ),
        MetalArtist(
            name: "Amberian Dawn", country: "Finland", genres: ["Symphonic Metal"],
            vector: .init(melody: 0.88, speed: 0.68, heavy: 0.56, symphonic: 0.92, technical: 0.62, growl: 0.06, cleanVocal: 0.99, catchy: 0.89),
            discovery: 0.69, reason: "女性クリーンVoとキーボード主体の旋律", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .female
        ),
        MetalArtist(
            name: "Astralion", country: "Finland", genres: ["Power Metal"],
            vector: .init(melody: 0.92, speed: 0.89, heavy: 0.57, symphonic: 0.61, technical: 0.66, growl: 0.02, cleanVocal: 0.98, catchy: 0.91),
            discovery: 0.88, reason: "北欧らしい疾走と透明感の強いサビ", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .unknown
        ),
        MetalArtist(
            name: "Arion", country: "Finland", genres: ["Symphonic Metal", "Melodic Metal"],
            vector: .init(melody: 0.88, speed: 0.66, heavy: 0.64, symphonic: 0.86, technical: 0.67, growl: 0.06, cleanVocal: 0.96, catchy: 0.93),
            discovery: 0.72, reason: "モダン寄りでも旋律とクリーンVoが強い", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .female
        ),
        MetalArtist(
            name: "Induction", country: "Germany", genres: ["Power Metal"],
            vector: .init(melody: 0.91, speed: 0.84, heavy: 0.62, symphonic: 0.68, technical: 0.74, growl: 0.04, cleanVocal: 0.96, catchy: 0.90),
            discovery: 0.83, reason: "現代的な音像と王道パワーメタルの両立", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .unknown
        ),
        MetalArtist(
            name: "Veonity", country: "Sweden", genres: ["Power Metal"],
            vector: .init(melody: 0.91, speed: 0.90, heavy: 0.58, symphonic: 0.60, technical: 0.61, growl: 0.02, cleanVocal: 0.97, catchy: 0.92),
            discovery: 0.87, reason: "速さと明快なメロディを優先した北欧型", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .unknown
        ),
        MetalArtist(
            name: "Bloodbound", country: "Sweden", genres: ["Power Metal"],
            vector: .init(melody: 0.86, speed: 0.80, heavy: 0.69, symphonic: 0.54, technical: 0.58, growl: 0.05, cleanVocal: 0.96, catchy: 0.94),
            discovery: 0.61, reason: "骨太だが非常に覚えやすいコーラス", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .unknown
        ),
        MetalArtist(
            name: "Serenity", country: "Austria", genres: ["Symphonic Power Metal"],
            vector: .init(melody: 0.90, speed: 0.70, heavy: 0.61, symphonic: 0.91, technical: 0.67, growl: 0.05, cleanVocal: 0.97, catchy: 0.90),
            discovery: 0.74, reason: "歴史劇的なシンフォニーと美旋律", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .unknown
        ),
        MetalArtist(
            name: "Dragony", country: "Austria", genres: ["Symphonic Power Metal"],
            vector: .init(melody: 0.91, speed: 0.84, heavy: 0.58, symphonic: 0.90, technical: 0.61, growl: 0.03, cleanVocal: 0.98, catchy: 0.93),
            discovery: 0.82, reason: "大仰で明るいシンフォニック疾走", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .unknown
        ),
        MetalArtist(
            name: "ShadowStrike", country: "USA", genres: ["Power Metal", "Symphonic"],
            vector: .init(melody: 0.95, speed: 0.91, heavy: 0.52, symphonic: 0.89, technical: 0.68, growl: 0.01, cleanVocal: 0.99, catchy: 0.96),
            discovery: 0.93, reason: "超高密度のクサメロと明るい疾走感", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .unknown
        ),
        MetalArtist(
            name: "Eternity's End", country: "Germany", genres: ["Power Metal", "Progressive Metal"],
            vector: .init(melody: 0.89, speed: 0.82, heavy: 0.63, symphonic: 0.58, technical: 0.94, growl: 0.03, cleanVocal: 0.94, catchy: 0.77),
            discovery: 0.94, reason: "技巧派なのにメロディを捨てない深掘り枠", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .unknown
        ),
        MetalArtist(
            name: "Ravian", country: "Italy", genres: ["Power Metal"],
            vector: .init(melody: 0.90, speed: 0.86, heavy: 0.59, symphonic: 0.67, technical: 0.65, growl: 0.03, cleanVocal: 0.96, catchy: 0.89),
            discovery: 0.95, reason: "未知度を優先した欧州メロスピ探索枠", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .unknown
        ),
        MetalArtist(
            name: "Memories of Old", country: "United Kingdom", genres: ["Symphonic Power Metal"],
            vector: .init(melody: 0.94, speed: 0.82, heavy: 0.55, symphonic: 0.95, technical: 0.63, growl: 0.01, cleanVocal: 0.99, catchy: 0.94),
            discovery: 0.90, reason: "物語型シンフォニックと強烈な美旋律", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .unknown
        ),
        MetalArtist(
            name: "NorthTale", country: "Sweden/USA", genres: ["Power Metal"],
            vector: .init(melody: 0.94, speed: 0.90, heavy: 0.57, symphonic: 0.61, technical: 0.68, growl: 0.02, cleanVocal: 0.99, catchy: 0.93),
            discovery: 0.72, reason: "古典的北欧メロスピを現代的に鳴らす", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .unknown
        ),
        MetalArtist(
            name: "Saint Deamon", country: "Sweden/Norway", genres: ["Power Metal"],
            vector: .init(melody: 0.89, speed: 0.70, heavy: 0.67, symphonic: 0.65, technical: 0.63, growl: 0.03, cleanVocal: 0.98, catchy: 0.91),
            discovery: 0.79, reason: "ミドルテンポでもサビと旋律が非常に強い", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .unknown
        ),
        MetalArtist(
            name: "Operus", country: "Canada", genres: ["Symphonic Metal"],
            vector: .init(melody: 0.85, speed: 0.62, heavy: 0.72, symphonic: 0.95, technical: 0.75, growl: 0.18, cleanVocal: 0.84, catchy: 0.78),
            discovery: 0.91, reason: "オペラ／映画音楽方向へ少し外す探索枠", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .unknown
        ),
        MetalArtist(
            name: "Marius Danielsen's Legend of Valley Doom", country: "Norway", genres: ["Symphonic Power Metal"],
            vector: .init(melody: 0.95, speed: 0.83, heavy: 0.55, symphonic: 0.94, technical: 0.60, growl: 0.02, cleanVocal: 0.99, catchy: 0.95),
            discovery: 0.96, reason: "RPG級の大仰さとクサメロを極端に盛った発掘枠", source: "BUILTIN", sourceSeed: nil,
            externalScore: nil, lastFmListeners: nil, lastFmPlaycount: nil, mbid: nil, area: nil,
            beginDate: nil, endDate: nil, ended: nil, hiddenScore: 50, metadataConfidence: 0, vocalType: .unknown
        ),
    ]
}
