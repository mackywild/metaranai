package jp.metaranai.app

object MetalCatalog {
    val artists = listOf(
        MetalArtist("SKYWINGS", "Japan", listOf("Melodic Power Metal", "Symphonic"), MetalVector(.98f,.86f,.55f,.88f,.62f,.04f,.98f,.96f), .82f, "高密度のクサメロと疾走感"),
        MetalArtist("MinstreliX", "Japan", listOf("Melodic Power Metal"), MetalVector(.96f,.90f,.58f,.75f,.64f,.08f,.96f,.91f), .86f, "国産メロスピの美旋律をさらに掘る"),
        MetalArtist("Syu", "Japan", listOf("Power Metal", "Instrumental"), MetalVector(.88f,.82f,.62f,.55f,.91f,.02f,.90f,.80f), .72f, "技巧派だがメロディ中心"),
        MetalArtist("Unlucky Morpheus", "Japan", listOf("Power Metal", "Symphonic"), MetalVector(.91f,.91f,.72f,.83f,.88f,.18f,.88f,.86f), .64f, "疾走・技巧・シンフォニックの交点"),
        MetalArtist("Versailles", "Japan", listOf("Symphonic Power Metal"), MetalVector(.91f,.78f,.62f,.94f,.72f,.08f,.93f,.88f), .58f, "華麗さと劇的メロディを重視"),
        MetalArtist("Lovebites", "Japan", listOf("Power Metal", "Heavy Metal"), MetalVector(.84f,.88f,.72f,.54f,.78f,.04f,.97f,.82f), .45f, "クリーンVoと高速ツインギター"),
        MetalArtist("Twilight Force", "Sweden", listOf("Symphonic Power Metal"), MetalVector(.94f,.90f,.52f,.96f,.64f,.01f,.99f,.94f), .48f, "明るいクサメロと大仰なシンフォニー"),
        MetalArtist("Fellowship", "United Kingdom", listOf("Power Metal"), MetalVector(.96f,.82f,.48f,.72f,.58f,.00f,.99f,.98f), .67f, "多幸感のあるメロディとキャッチーさ"),
        MetalArtist("Majestica", "Sweden", listOf("Power Metal"), MetalVector(.93f,.88f,.53f,.68f,.67f,.01f,.99f,.95f), .62f, "明快なサビと疾走パワーメタル"),
        MetalArtist("Dynazty", "Sweden", listOf("Melodic Metal"), MetalVector(.88f,.68f,.66f,.45f,.60f,.02f,.99f,.96f), .50f, "モダンさを保ちつつサビが強い"),
        MetalArtist("Keldian", "Norway", listOf("Power Metal", "Melodic Metal"), MetalVector(.92f,.75f,.54f,.58f,.62f,.02f,.97f,.91f), .89f, "知名度控えめでメロディ偏重"),
        MetalArtist("Pathfinder", "Poland", listOf("Symphonic Power Metal"), MetalVector(.92f,.94f,.62f,.97f,.68f,.10f,.89f,.85f), .91f, "爆速と映画的シンフォニー"),
        MetalArtist("Heavenly", "France", listOf("Power Metal"), MetalVector(.96f,.91f,.56f,.60f,.63f,.02f,.98f,.93f), .80f, "クラシックな欧州メロスピ直球"),
        MetalArtist("Dreamtale", "Finland", listOf("Power Metal"), MetalVector(.92f,.85f,.55f,.58f,.58f,.04f,.94f,.91f), .76f, "フィンランド系の透明感あるメロディ"),
        MetalArtist("Ancient Bards", "Italy", listOf("Symphonic Power Metal"), MetalVector(.91f,.84f,.62f,.96f,.70f,.12f,.91f,.86f), .79f, "物語性とシンフォニック密度"),
        MetalArtist("Temperance", "Italy", listOf("Melodic Metal", "Symphonic"), MetalVector(.89f,.71f,.58f,.84f,.61f,.05f,.98f,.94f), .70f, "男女クリーンVoとキャッチーな旋律"),
        MetalArtist("Frozen Crown", "Italy", listOf("Power Metal"), MetalVector(.86f,.89f,.69f,.54f,.75f,.17f,.88f,.85f), .66f, "疾走とヘヴィさのバランス"),
        MetalArtist("Seven Spires", "USA", listOf("Symphonic Metal"), MetalVector(.86f,.69f,.78f,.92f,.79f,.48f,.72f,.78f), .77f, "少し冒険したい日に刺さる劇的サウンド"),
        MetalArtist("Diablo Swing Orchestra", "Sweden", listOf("Avant-garde Metal", "Swing"), MetalVector(.81f,.52f,.68f,.66f,.82f,.21f,.84f,.91f), .73f, "ジャズ／スウィング要素を持つ異色枠"),
        MetalArtist("Amberian Dawn", "Finland", listOf("Symphonic Metal"), MetalVector(.88f,.68f,.56f,.92f,.62f,.06f,.99f,.89f), .69f, "女性クリーンVoとキーボード主体の旋律"),
        MetalArtist("Astralion", "Finland", listOf("Power Metal"), MetalVector(.92f,.89f,.57f,.61f,.66f,.02f,.98f,.91f), .88f, "北欧らしい疾走と透明感の強いサビ"),
        MetalArtist("Arion", "Finland", listOf("Symphonic Metal", "Melodic Metal"), MetalVector(.88f,.66f,.64f,.86f,.67f,.06f,.96f,.93f), .72f, "モダン寄りでも旋律とクリーンVoが強い"),
        MetalArtist("Induction", "Germany", listOf("Power Metal"), MetalVector(.91f,.84f,.62f,.68f,.74f,.04f,.96f,.90f), .83f, "現代的な音像と王道パワーメタルの両立"),
        MetalArtist("Veonity", "Sweden", listOf("Power Metal"), MetalVector(.91f,.90f,.58f,.60f,.61f,.02f,.97f,.92f), .87f, "速さと明快なメロディを優先した北欧型"),
        MetalArtist("Bloodbound", "Sweden", listOf("Power Metal"), MetalVector(.86f,.80f,.69f,.54f,.58f,.05f,.96f,.94f), .61f, "骨太だが非常に覚えやすいコーラス"),
        MetalArtist("Serenity", "Austria", listOf("Symphonic Power Metal"), MetalVector(.90f,.70f,.61f,.91f,.67f,.05f,.97f,.90f), .74f, "歴史劇的なシンフォニーと美旋律"),
        MetalArtist("Dragony", "Austria", listOf("Symphonic Power Metal"), MetalVector(.91f,.84f,.58f,.90f,.61f,.03f,.98f,.93f), .82f, "大仰で明るいシンフォニック疾走"),
        MetalArtist("ShadowStrike", "USA", listOf("Power Metal", "Symphonic"), MetalVector(.95f,.91f,.52f,.89f,.68f,.01f,.99f,.96f), .93f, "超高密度のクサメロと明るい疾走感"),
        MetalArtist("Eternity's End", "Germany", listOf("Power Metal", "Progressive Metal"), MetalVector(.89f,.82f,.63f,.58f,.94f,.03f,.94f,.77f), .94f, "技巧派なのにメロディを捨てない深掘り枠"),
        MetalArtist("Ravian", "Italy", listOf("Power Metal"), MetalVector(.90f,.86f,.59f,.67f,.65f,.03f,.96f,.89f), .95f, "未知度を優先した欧州メロスピ探索枠"),
        MetalArtist("Memories of Old", "United Kingdom", listOf("Symphonic Power Metal"), MetalVector(.94f,.82f,.55f,.95f,.63f,.01f,.99f,.94f), .90f, "物語型シンフォニックと強烈な美旋律"),
        MetalArtist("NorthTale", "Sweden/USA", listOf("Power Metal"), MetalVector(.94f,.90f,.57f,.61f,.68f,.02f,.99f,.93f), .72f, "古典的北欧メロスピを現代的に鳴らす"),
        MetalArtist("Saint Deamon", "Sweden/Norway", listOf("Power Metal"), MetalVector(.89f,.70f,.67f,.65f,.63f,.03f,.98f,.91f), .79f, "ミドルテンポでもサビと旋律が非常に強い"),
        MetalArtist("Operus", "Canada", listOf("Symphonic Metal"), MetalVector(.85f,.62f,.72f,.95f,.75f,.18f,.84f,.78f), .91f, "オペラ／映画音楽方向へ少し外す探索枠"),
        MetalArtist("Marius Danielsen's Legend of Valley Doom", "Norway", listOf("Symphonic Power Metal"), MetalVector(.95f,.83f,.55f,.94f,.60f,.02f,.99f,.95f), .96f, "RPG級の大仰さとクサメロを極端に盛った発掘枠")
    )

    fun findByName(name: String): MetalArtist? = artists.firstOrNull { it.name.equals(name, ignoreCase = true) }
}
