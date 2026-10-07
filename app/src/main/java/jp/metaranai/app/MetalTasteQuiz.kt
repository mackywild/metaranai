package jp.metaranai.app

/** A music preference quiz: answers describe sound, not personality or a clinical trait. */
object MetalTasteQuiz {
    data class Choice(val title: String, val detail: String, val values: Map<Int, Float>)
    data class Question(val title: String, val choices: List<Choice>)
    data class Result(val code: String, val genre: String, val profile: MetalVector,
                      val description: String, val artists: List<MetalArtist>)

    val questions = listOf(
        Question("どんなテンポで気分を上げたい？", listOf(
            Choice("駆け抜けたい", "速いビートでテンションを上げたい", mapOf(1 to .88f)),
            Choice("じっくり浸りたい", "ゆっくりした音の重さを味わいたい", mapOf(1 to .22f)))),
        Question("音楽でいちばん惹かれるのは？", listOf(
            Choice("口ずさめるメロディ", "歌やギターの旋律が心に残る曲", mapOf(0 to .94f, 2 to .56f)),
            Choice("体に響く迫力", "重いギターやリズムに圧倒される曲", mapOf(0 to .36f, 2 to .94f)))),
        Question("ボーカルの好みは？", listOf(
            Choice("はっきりした歌声", "歌詞やメロディを聴き取りやすい声", mapOf(5 to .05f, 6 to .96f)),
            Choice("叫び声も楽しみたい", "荒々しい声や低いうなり声も試したい", mapOf(5 to .90f, 6 to .20f)))),
        Question("どんな世界観に惹かれる？", listOf(
            Choice("映画や物語のような壮大さ", "オーケストラや幻想的な雰囲気", mapOf(3 to .96f)),
            Choice("バンドの音で勝負", "ギター・ベース・ドラムの生々しさ", mapOf(3 to .14f)))),
        Question("曲の展開はどちらが好き？", listOf(
            Choice("変化や意外性を楽しむ", "複雑なリズムや演奏をじっくり聴きたい", mapOf(4 to .95f)),
            Choice("まっすぐ気持ちよく", "分かりやすい流れに身を任せたい", mapOf(4 to .35f)))),
        Question("何度も聴きたくなるのは？", listOf(
            Choice("覚えやすいサビ", "一緒に歌ったり、すぐにノれる曲", mapOf(7 to .96f)),
            Choice("深く入り込める雰囲気", "すぐには覚えられなくても余韻が残る曲", mapOf(7 to .35f))))
    )

    fun result(answers: List<Int>): Result {
        require(answers.size == questions.size && answers.all { it in 0..1 }) { "すべての質問に回答してください" }
        val values = MutableList(8) { .5f }
        questions.forEachIndexed { index, q -> q.choices[answers[index]].values.forEach { (axis, value) -> values[axis] = value } }
        val profile = MetalVector(values[0], values[1], values[2], values[3], values[4], values[5], values[6], values[7])
        val genre = GenreLensCatalog.lenses.filter { it.name in StarterBandCatalog.genres }
            .minWith(compareBy<GenreLensCatalog.Lens> { distance(profile, it.vector) }.thenBy { it.name }).name
        val symbols = listOf("SD", "MH", "CG", "OR")
        val code = symbols.mapIndexed { i, pair -> pair[answers[i]] }.joinToString("")
        val traits = profile.traits().filter { it.second >= .80f }.take(3).joinToString("・") { it.first }
        return Result(code, genre, profile,
            "${traits.ifBlank { "落ち着いた重さ" }}を楽しめる音からスタート。評価を重ねると、Metal DNAがあなたの好みに育ちます。",
            StarterBandCatalog.forGenre(genre))
    }

    private fun distance(a: MetalVector, b: MetalVector): Float {
        val weights = listOf(1f, .9f, 1f, .8f, .9f, 1f, 1f, .6f)
        return a.traits().zip(b.traits()).mapIndexed { i, (left, right) ->
            val difference = left.second - right.second
            difference * difference * weights[i]
        }.sum()
    }
}

/** Introductory artists by genre. Profiles are genre prototypes used only to bootstrap discovery. */
object StarterBandCatalog {
    private data class Band(val name: String, val country: String, val description: String)
    private val bands = linkedMapOf(
        "Melodic Metal" to listOf(Band("Dynazty", "Sweden", "力強い歌声と耳に残るサビ"), Band("Amaranthe", "Sweden", "歌声と電子音を組み合わせた華やかさ"), Band("Beast in Black", "Finland", "キャッチーな旋律とシンセサイザー")),
        "Power Metal" to listOf(Band("Helloween", "Germany", "疾走するギターと高揚感のある歌メロ"), Band("Blind Guardian", "Germany", "物語を感じる重厚なコーラス"), Band("Fellowship", "United Kingdom", "明るいメロディとファンタジーの世界")),
        "Symphonic Metal" to listOf(Band("Nightwish", "Finland", "オーケストラと歌声が作る壮大な世界"), Band("Epica", "Netherlands", "荘厳な合唱と重いバンドサウンド"), Band("Within Temptation", "Netherlands", "ドラマチックな歌声と親しみやすい旋律")),
        "Gothic Metal" to listOf(Band("Paradise Lost", "United Kingdom", "暗い雰囲気と重厚なメロディ"), Band("Lacuna Coil", "Italy", "男女ボーカルが描く陰影"), Band("Moonspell", "Portugal", "妖しい世界観と力強いリズム")),
        "Metalcore" to listOf(Band("Killswitch Engage", "USA", "叫び声と歌メロのコントラスト"), Band("Architects", "United Kingdom", "緊張感のあるリズムと感情的な歌声"), Band("Trivium", "USA", "鋭いギターとメロディを組み合わせた音")),
        "Melodic Death Metal" to listOf(Band("Dark Tranquillity", "Sweden", "荒々しい歌声の奥にある美しい旋律"), Band("Insomnium", "Finland", "哀愁のあるメロディと深い響き"), Band("Amon Amarth", "Sweden", "重いリズムと勇壮な世界観")),
        "Progressive Metal" to listOf(Band("Dream Theater", "USA", "技巧的な演奏と変化に富む曲展開"), Band("Haken", "United Kingdom", "意外性のあるリズムと多彩な音"), Band("Symphony X", "USA", "技巧と劇的なメロディの組み合わせ")),
        "Glam Metal" to listOf(Band("Mötley Crüe", "USA", "華やかでノリのよいハードな音"), Band("Poison", "USA", "一緒に歌いたくなる明るいサビ"), Band("Ratt", "USA", "印象的なギターと軽快なリズム")),
        "Nu Metal" to listOf(Band("Korn", "USA", "低くうねるギターと独特のグルーヴ"), Band("Slipknot", "USA", "激しい打楽器と圧倒的な勢い"), Band("Limp Bizkit", "USA", "ラップと重いリズムの組み合わせ")),
        "Folk Metal" to listOf(Band("Eluveitie", "Switzerland", "民族楽器と激しいバンドサウンド"), Band("Korpiklaani", "Finland", "踊りたくなる民族音楽のリズム"), Band("Ensiferum", "Finland", "勇壮な旋律と冒険を感じる世界")),
        "Doom Metal" to listOf(Band("Candlemass", "Sweden", "ゆっくり迫る重さと劇的な歌声"), Band("Saint Vitus", "USA", "重いギターの響きにじっくり浸る"), Band("Electric Wizard", "United Kingdom", "濃密でゆっくりした音のうねり")),
        "Thrash Metal" to listOf(Band("Metallica", "USA", "鋭いギターのリフと力強い曲展開"), Band("Megadeth", "USA", "細かなギターと切れ味のあるリズム"), Band("Slayer", "USA", "荒々しい疾走感と緊張感")),
        "Black Metal" to listOf(Band("Emperor", "Norway", "激しさと壮大な雰囲気の融合"), Band("Immortal", "Norway", "冷たい世界観と高速の演奏"), Band("Darkthrone", "Norway", "ざらついた音と独特の空気感")),
        "Death Metal" to listOf(Band("Death", "USA", "重い歌声と変化に富むギター"), Band("Obituary", "USA", "うねるリズムと低い歌声"), Band("Cannibal Corpse", "USA", "強烈な重さと密度の高い演奏")),
        "Heavy Metal" to listOf(Band("Iron Maiden", "United Kingdom", "躍動するリズムと物語性のある歌"), Band("Judas Priest", "United Kingdom", "力強い高音ボーカルと鋼のギター"), Band("Saxon", "United Kingdom", "まっすぐなリフと骨太な歌声")),
        "Alternative Metal" to listOf(Band("System of a Down", "USA", "意外な展開と個性的な歌声"), Band("Deftones", "USA", "浮遊感のある音と重いギター"), Band("Disturbed", "USA", "強いリズムと覚えやすい歌メロ"))
    )
    val genres: Set<String> get() = bands.keys

    fun forGenre(genre: String): List<MetalArtist> {
        val vector = requireNotNull(GenreLensCatalog.vectorFor(listOf(genre)))
        return bands[genre].orEmpty().map { band ->
            MetalCatalog.findByName(band.name) ?: MetalArtist(band.name, band.country, listOf(genre), vector,
                .5f, band.description, sourceSeed = "genre:$genre")
        }
    }
}
