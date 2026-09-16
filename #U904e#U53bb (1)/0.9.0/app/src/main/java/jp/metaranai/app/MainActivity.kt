package jp.metaranai.app

import android.os.Bundle
import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.DayOfWeek

private val Bg = Color(0xFF090909)
private val Card = Color(0xFF151515)
private val Acid = Color(0xFFD6FF36)
private val Muted = Color(0xFFA4A4A4)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MetaranaiApp() }
    }

    @Deprecated("Facebook SDK still delivers its login result through onActivityResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val handled = ViewModelProvider(this)[MainViewModel::class.java]
            .handleFacebookActivityResult(requestCode, resultCode, data)
        if (!handled) super.onActivityResult(requestCode, resultCode, data)
    }
}

@Composable
fun MetaranaiApp(vm: MainViewModel = viewModel()) {
    val onboardingComplete by vm.onboardingComplete.collectAsState()
    if (!onboardingComplete) {
        MaterialTheme(colorScheme = darkColorScheme(primary = Acid, background = Bg, surface = Card)) {
            OnboardingScreen(vm)
        }
        return
    }
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("今日", "探す", "図鑑", "DNA", "設定")
    val icons = listOf("⚡", "🔎", "📚", "🧬", "⚙")
    MaterialTheme(colorScheme = darkColorScheme(primary = Acid, background = Bg, surface = Card)) {
        Scaffold(
            containerColor = Bg,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF101010)) {
                    tabs.forEachIndexed { i, label ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Text(icons[i]) },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when(tab) {
                    0 -> HomeScreen(vm)
                    1 -> SearchScreen(vm)
                    2 -> ArchiveScreen(vm)
                    3 -> DnaScreen(vm)
                    else -> SettingsScreen(vm)
                }
            }
        }
    }
}

@Composable
private fun Header(subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("メタらない？", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("v0.9.0 · ACCOUNT & PERSONALIZATION", color = Acid, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Text(subtitle, color = Muted, fontSize = 13.sp)
    }
}

@Composable
private fun HomeScreen(vm: MainViewModel) {
    val rec by vm.recommendation.collectAsState()
    val spotifyOpen by vm.spotifyOpenStatus.collectAsState()
    val lens by vm.genreLens.collectAsState()
    val lensPreparing by vm.genreLensPreparing.collectAsState()
    val lensReady by vm.genreLensReady.collectAsState()
    val lensStatus by vm.genreLensStatus.collectAsState()
    val reactionStatus by vm.reactionStatus.collectAsState()
    val deepDiveResults by vm.deepDiveResults.collectAsState()
    val deepDiveStatus by vm.deepDiveStatus.collectAsState()
    val deepDiving by vm.deepDiving.collectAsState()
    val mediaStatus by vm.mediaOpenStatus.collectAsState()
    val activeGenres = GenreLensCatalog.activeGenres(lens)
    val lensBlocked = activeGenres.isNotEmpty() && (!lensReady || lensPreparing)
    var showAnalysis by remember(rec.artist.name) { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Header("ジャンルは必須条件。DNAでその地下を選び抜く。") }
        if (activeGenres.isNotEmpty()) item {
            Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().background(Color(0xFF101010), RoundedCornerShape(16.dp)).padding(12.dp)) {
                Text("TODAY'S GENRE LENS  ${activeGenres.joinToString(" / ")}", color = Acid, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                if (lensStatus.isNotBlank()) Text(lensStatus, color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(10.dp))
        }
        if (lensBlocked) {
            item {
                Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().background(Card, RoundedCornerShape(28.dp)).padding(24.dp)) {
                    Text("GENRE LENS DIGGING", color = Acid, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(if (lensPreparing) "${activeGenres.joinToString(" / ")} の地下を探索中…" else "${activeGenres.joinToString(" / ")} の候補が不足しています", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(10.dp))
                    Text("指定ジャンル以外は出さない。評価済みArtistを除外し、新しい未評価候補をLocal Metal DBへ補充してから、METAL DNAで今日の1組を選びます。", color = Muted, lineHeight = 20.sp)
                    if (reactionStatus.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(reactionStatus, color = Acid, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    if (lensPreparing) {
                        Spacer(Modifier.height(14.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        } else {
            item {
                Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().background(Card, RoundedCornerShape(28.dp)).padding(24.dp)) {
                    Text("TODAY'S おすすメタル", color = Acid, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.height(18.dp))
                    Text(rec.artist.name, color = Color.White, fontSize = 33.sp, fontWeight = FontWeight.Black)
                    Text("${rec.artist.country}  •  ${rec.artist.genres.joinToString(" / ")}", color = Muted)
                    if (rec.artist.vocalType != VocalType.UNKNOWN) Text(rec.artist.vocalType.label, color = Muted, fontSize = 11.sp)
                    if (rec.artist.source != ArtistSource.BUILTIN) {
                        Spacer(Modifier.height(6.dp))
                        Text("🌐 EXTERNAL DISCOVERY  •  Seed: ${rec.artist.sourceSeed ?: "unknown"}  •  HIDDEN ${rec.artist.hiddenScore}", color = Acid, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(22.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${rec.compatibility}%", color = Acid, fontSize = 34.sp, fontWeight = FontWeight.Black)
                        Text("  DNA MATCH", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Text(rec.reason, color = Color.White, lineHeight = 22.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rec.matchedTraits.forEach { trait -> SuggestionChip(onClick = {}, label = { Text(trait, fontSize = 11.sp) }) }
                    }
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(onClick = { showAnalysis = !showAnalysis }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (showAnalysis) "解析を閉じる" else "なぜこのArtist？ / スコアを見る")
                    }
                    if (showAnalysis) {
                        Spacer(Modifier.height(10.dp))
                        ScoreBreakdown(rec.breakdown)
                        Spacer(Modifier.height(10.dp))
                        WhyThisArtist(vm.whyThisArtist(rec))
                        if (rec.artist.source != ArtistSource.BUILTIN) {
                            Spacer(Modifier.height(10.dp))
                            ExternalMeta(rec.artist)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.openSpotifyArtist(rec.artist) }, modifier = Modifier.weight(1f)) { Text("Spotify") }
                        OutlinedButton(onClick = { vm.openYouTube(rec.artist) }, modifier = Modifier.weight(1f)) { Text("YouTube") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.openYouTube(rec.artist, "mv") }, modifier = Modifier.weight(1f)) { Text("MV") }
                        OutlinedButton(onClick = { vm.openYouTube(rec.artist, "live") }, modifier = Modifier.weight(1f)) { Text("LIVE") }
                        OutlinedButton(onClick = { vm.deepDive(rec.artist) }, enabled = !deepDiving, modifier = Modifier.weight(1.35f)) { Text(if (deepDiving) "掘削中" else "⛏ 深掘り") }
                    }
                    if (spotifyOpen.isNotBlank()) Text(spotifyOpen, color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
                    if (mediaStatus.isNotBlank()) Text(mediaStatus, color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = vm::shuffle, modifier = Modifier.fillMaxWidth()) { Text("別の沼も見る") }
                }
            }
            if (deepDiveStatus.isNotBlank() || deepDiveResults.isNotEmpty()) item {
                DeepDivePanel(vm, deepDiveStatus, deepDiveResults, deepDiving)
            }
            item {
                Text("聴いた結果を5段階で教えろ", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp, 18.dp, 20.dp, 8.dp))
                ReactionSelector(onReaction = vm::react)
                if (reactionStatus.isNotBlank()) {
                    Text(reactionStatus, color = Acid, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun ReactionSelector(onReaction: (Reaction) -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Reaction.entries.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                pair.forEach { r ->
                    OutlinedButton(onClick = { onReaction(r) }, modifier = Modifier.weight(1f).heightIn(min = 58.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(r.label, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(r.description, color = Muted, fontSize = 8.sp, maxLines = 1)
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ScoreBreakdown(b: RecommendationBreakdown) {
    Column(Modifier.fillMaxWidth().background(Bg, RoundedCornerShape(16.dp)).padding(14.dp)) {
        Text("SCORE BREAKDOWN  ${b.total}", color = Acid, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        buildList {
            add("相性" to b.affinity)
            if (b.genreLens > 0) add("GENRE LENS" to b.genreLens)
            add("HIDDEN" to b.hidden); add("未知" to b.novelty); add("探索" to b.exploration); add("発掘度" to b.discovery)
        }.forEach { (name, value) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, color = Muted, fontSize = 12.sp)
                Text("+$value", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WhyThisArtist(reasons: List<String>) {
    Column(Modifier.fillMaxWidth().background(Color(0xFF101010), RoundedCornerShape(16.dp)).padding(14.dp)) {
        Text("WHY THIS ARTIST?", color = Acid, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))
        if (reasons.isEmpty()) {
            Text("METAL DNAと未探索度の総合スコアから選出", color = Muted, fontSize = 11.sp)
        } else {
            reasons.take(5).forEach { reason ->
                Text("• $reason", color = Color.White, fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun DeepDivePanel(vm: MainViewModel, status: String, results: List<MetalArtist>, loading: Boolean) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp).fillMaxWidth().background(Card, RoundedCornerShape(22.dp)).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("⛏ DEEP DIVE", color = Acid, fontWeight = FontWeight.Bold)
                if (status.isNotBlank()) Text(status, color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
            }
            TextButton(onClick = vm::clearDeepDive) { Text("閉じる") }
        }
        if (loading) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        results.take(8).forEach { artist ->
            Spacer(Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth().background(Bg, RoundedCornerShape(14.dp)).padding(12.dp)) {
                Text(artist.name, color = Color.White, fontWeight = FontWeight.Bold)
                Text("${artist.country} • ${artist.genres.take(3).joinToString(" / ")} • HIDDEN ${artist.hiddenScore}", color = Muted, fontSize = 10.sp)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { vm.openSpotifyArtist(artist) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Spotify", fontSize = 10.sp) }
                    OutlinedButton(onClick = { vm.openYouTube(artist) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp)) { Text("YouTube", fontSize = 10.sp) }
                    OutlinedButton(onClick = { vm.deepDive(artist) }, enabled = !loading, modifier = Modifier.weight(.72f), contentPadding = PaddingValues(horizontal = 4.dp)) { Text("⛏", fontSize = 11.sp) }
                }
            }
        }
    }
}

@Composable
private fun ExternalMeta(a: MetalArtist) {
    Column(Modifier.fillMaxWidth().background(Color(0xFF101010), RoundedCornerShape(16.dp)).padding(14.dp)) {
        Text("HIDDEN PROFILE", color = Acid, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Hidden Score ${a.hiddenScore} / Metadata ${a.metadataConfidence}%", color = Color.White, fontSize = 12.sp)
        val stats = buildList {
            a.lastFmListeners?.let { add("Listeners ${formatCompact(it)}") }
            a.lastFmPlaycount?.let { add("Plays ${formatCompact(it)}") }
            a.beginDate?.let { add("Since $it") }
            a.area?.let { add(it) }
            if (a.vocalType != VocalType.UNKNOWN) add(a.vocalType.label)
        }
        if (stats.isNotEmpty()) Text(stats.joinToString("  •  "), color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        a.mbid?.let { Text("MBID ${it.take(8)}…", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp)) }
    }
}

@Composable
private fun SearchScreen(vm: MainViewModel) {
    val history by vm.searchHistory.collectAsState()
    val remote by vm.remoteSearchResults.collectAsState()
    val remoteSearching by vm.remoteSearching.collectAsState()
    val remoteStatus by vm.remoteSearchStatus.collectAsState()
    val external by vm.externalArtists.collectAsState()
    val deepDiveResults by vm.deepDiveResults.collectAsState()
    val deepDiveStatus by vm.deepDiveStatus.collectAsState()
    val deepDiving by vm.deepDiving.collectAsState()
    var query by remember { mutableStateOf("") }
    val localResults = remember(query, external) { vm.search(query) }
    val merged = (localResults + remote).distinctBy { it.name.lowercase() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Header("端末DBに無ければ、世界から掘って覚える。") }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; vm.clearRemoteSearch() },
                singleLine = true,
                label = { Text("バンド / 国 / ジャンル") },
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.searchExternal(query) },
                enabled = query.trim().length >= 2 && !remoteSearching,
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
            ) { Text(if (remoteSearching) "世界のMetal DBを探索中…" else "ローカルに無ければ世界から検索") }
            if (remoteStatus.isNotBlank()) Text(remoteStatus, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
            Text(
                if (query.isBlank()) "Local Metal DBから発掘度の高い候補" else "検索結果 ${merged.size}件",
                color = Muted, modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
        items(merged) { artist ->
            Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth().background(Card, RoundedCornerShape(18.dp)).padding(16.dp)) {
                Text(artist.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${artist.country} • ${artist.genres.joinToString(" / ")}", color = Muted, fontSize = 12.sp)
                Text(
                    "発掘度 ${(artist.discovery * 100).toInt()}%${if (artist.source != ArtistSource.BUILTIN) "  •  🌐 HIDDEN ${artist.hiddenScore}" else ""}",
                    color = Acid, fontSize = 12.sp
                )
                if (artist.sourceSeed?.startsWith("Search:") == true) Text("🌐 外部検索からLocal DBへ保存済み", color = Muted, fontSize = 10.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        vm.recordSearch(query.ifBlank { "discover" }, artist)
                        vm.openSpotifyArtist(artist)
                    }, modifier = Modifier.weight(1f)) { Text("Spotify") }
                    OutlinedButton(onClick = {
                        vm.recordSearch(query.ifBlank { "discover" }, artist)
                        vm.openYouTube(artist)
                    }, modifier = Modifier.weight(1f)) { Text("YouTube") }
                    OutlinedButton(onClick = {
                        vm.recordSearch(query.ifBlank { "discover" }, artist)
                        vm.deepDive(artist)
                    }, enabled = !deepDiving, modifier = Modifier.weight(1f)) { Text("⛏") }
                }
            }
        }
        if (deepDiveStatus.isNotBlank() || deepDiveResults.isNotEmpty()) item {
            DeepDivePanel(vm, deepDiveStatus, deepDiveResults, deepDiving)
        }
        if (history.isNotEmpty()) item {
            Spacer(Modifier.height(10.dp))
            Text("最近の探索", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp, 8.dp))
            Text(history.take(8).joinToString("  •  ") { it.artistName }, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp))
        }
    }
}

@Composable
private fun ArchiveScreen(vm: MainViewModel) {
    val external by vm.externalArtists.collectAsState()
    val history by vm.history.collectAsState()
    val profile by vm.profile.collectAsState()
    val deepDiveResults by vm.deepDiveResults.collectAsState()
    val deepDiveStatus by vm.deepDiveStatus.collectAsState()
    val deepDiving by vm.deepDiving.collectAsState()
    var query by remember { mutableStateOf("") }
    var reactionFilter by remember { mutableStateOf("ALL") }
    var genreFilter by remember { mutableStateOf<String?>(null) }
    var vocalFilter by remember { mutableStateOf<VocalType?>(null) }
    var sortMode by remember { mutableStateOf("DNA") }

    val archive = remember(external, history) { vm.archiveArtists() }
    val latestReaction = remember(history) {
        history.groupBy { it.artistName.trim().lowercase() }.mapValues { (_, rows) -> rows.first() }
    }
    val ratedCount = latestReaction.keys.count { key -> archive.any { it.name.trim().lowercase() == key } }
    val favorites = latestReaction.values.count { it.reaction == Reaction.LOVE_ALL }
    val filtered = archive.filter { artist ->
        val record = latestReaction[artist.name.trim().lowercase()]
        val queryOk = query.isBlank() || artist.name.contains(query, true) || artist.country.contains(query, true) || artist.genres.any { it.contains(query, true) }
        val reactionOk = when (reactionFilter) {
            "UNRATED" -> record == null
            "ALL" -> true
            else -> record?.reaction?.name == reactionFilter
        }
        val genreOk = genreFilter == null || GenreLensCatalog.matches(artist, listOf(genreFilter!!))
        val vocalOk = vocalFilter == null || artist.vocalType == vocalFilter
        queryOk && reactionOk && genreOk && vocalOk
    }
    val visible = when (sortMode) {
        "HIDDEN" -> filtered.sortedByDescending { it.hiddenScore }
        "NAME" -> filtered.sortedBy { it.name.lowercase() }
        else -> filtered.sortedByDescending { profile.similarity(it.vector) }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Header("PERSONAL METAL ARCHIVE — 聴くほど自分専用のMetal図鑑が育つ。") }
        item {
            Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("ARCHIVE", archive.size.toString(), Modifier.weight(1f))
                StatCard("外部DB", external.size.toString(), Modifier.weight(1f))
                StatCard("評価済", ratedCount.toString(), Modifier.weight(1f))
                StatCard("💘", favorites.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                label = { Text("Archive検索: バンド / 国 / ジャンル") },
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = reactionFilter == "ALL", onClick = { reactionFilter = "ALL" }, label = { Text("全部") })
                FilterChip(selected = reactionFilter == "UNRATED", onClick = { reactionFilter = "UNRATED" }, label = { Text("未評価") })
                Reaction.entries.forEach { reaction ->
                    FilterChip(selected = reactionFilter == reaction.name, onClick = { reactionFilter = reaction.name }, label = { Text(reaction.label, fontSize = 10.sp) })
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = genreFilter == null, onClick = { genreFilter = null }, label = { Text("全Genre") })
                GenreLensCatalog.names().forEach { genre ->
                    FilterChip(selected = genreFilter == genre, onClick = { genreFilter = if (genreFilter == genre) null else genre }, label = { Text(genre, fontSize = 10.sp) })
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = vocalFilter == null, onClick = { vocalFilter = null }, label = { Text("全Vo") })
                listOf(VocalType.MALE, VocalType.FEMALE, VocalType.MIXED, VocalType.UNKNOWN).forEach { vocal ->
                    FilterChip(selected = vocalFilter == vocal, onClick = { vocalFilter = if (vocalFilter == vocal) null else vocal }, label = { Text(vocal.label, fontSize = 10.sp) })
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("DNA" to "おすすめ順", "HIDDEN" to "HIDDEN", "NAME" to "名前順").forEach { (key, label) ->
                    FilterChip(selected = sortMode == key, onClick = { sortMode = key }, label = { Text(label, fontSize = 10.sp) })
                }
            }
            Text("表示 ${visible.size}組 / 未評価 ${(archive.size - ratedCount).coerceAtLeast(0)}組", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        }
        if (deepDiveStatus.isNotBlank() || deepDiveResults.isNotEmpty()) item {
            DeepDivePanel(vm, deepDiveStatus, deepDiveResults, deepDiving)
        }
        if (visible.isEmpty()) item {
            Text("条件に一致するArtistがいない。フィルターを緩めるか『探す』から地下を追加しよう。", color = Muted, modifier = Modifier.padding(20.dp))
        }
        items(visible, key = { it.name.lowercase() }) { artist ->
            val record = latestReaction[artist.name.trim().lowercase()]
            Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth().background(Card, RoundedCornerShape(18.dp)).padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(artist.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("${artist.country} • ${artist.genres.joinToString(" / ")}", color = Muted, fontSize = 11.sp)
                    }
                    Text(record?.reaction?.label ?: "未評価", color = if (record == null) Muted else Acid, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(5.dp))
                Text("HIDDEN ${artist.hiddenScore} • 発掘度 ${(artist.discovery * 100).toInt()}% • ${artist.vocalType.label}", color = Acid, fontSize = 10.sp)
                if (record != null) Text("最終評価 ${record.date} • 当時DNA MATCH ${record.score}%", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
                if (vm.spotifyLinkCached(artist)) Text("Spotify本人確認済みリンク取得済み", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.openSpotifyArtist(artist) }, modifier = Modifier.weight(1f)) { Text("Spotify", fontSize = 11.sp) }
                    OutlinedButton(onClick = { vm.openYouTube(artist) }, modifier = Modifier.weight(1f)) { Text("YouTube", fontSize = 11.sp) }
                    OutlinedButton(onClick = { vm.deepDive(artist) }, enabled = !deepDiving, modifier = Modifier.weight(1f)) { Text("⛏", fontSize = 12.sp) }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Card, RoundedCornerShape(14.dp)).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Acid, fontWeight = FontWeight.Black, fontSize = 17.sp); Text(label, color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun DnaScreen(vm: MainViewModel) {
    val p by vm.profile.collectAsState()
    val vocal by vm.vocalProfile.collectAsState()
    val values = listOf("MELODY" to p.melody,"SPEED" to p.speed,"HEAVINESS" to p.heavy,"SYMPHONIC" to p.symphonic,"TECHNICAL" to p.technical,"GROWL" to p.growl,"CLEAN VOCAL" to p.cleanVocal,"CATCHINESS" to p.catchy)
    val strongest = p.traits().sortedByDescending { it.second }.take(3)
    val topGenres = vm.topGenres()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Header("YOUR METAL DNA / VOCAL DNA") }
        item {
            Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().background(Card, RoundedCornerShape(22.dp)).padding(18.dp)) {
                Text("TYPE", color = Muted, fontSize = 11.sp)
                Text(vm.dnaType(), color = Acid, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                Text("支配的特性: ${strongest.joinToString(" / ") { "${it.first} ${(it.second*100).toInt()}" }}", color = Color.White, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
        }
        items(values) { (name, value) -> DnaBar(name, value) }
        item {
            Spacer(Modifier.height(8.dp))
            Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().background(Card, RoundedCornerShape(22.dp)).padding(18.dp)) {
                Text("VOCAL DNA", color = Acid, fontWeight = FontWeight.Bold)
                Text(if (vocal.observations < 3) "V0.5から学習開始 (${vocal.observations}/3+)" else "${vocal.observations}件のVoシグナル", color = Muted, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                VocalBar("男性Vo", vocal.male); VocalBar("女性Vo", vocal.female); VocalBar("混成Vo", vocal.mixed)
            }
            Spacer(Modifier.height(12.dp))
        }
        if (topGenres.isNotEmpty()) item {
            Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().background(Card, RoundedCornerShape(22.dp)).padding(18.dp)) {
                Text("LISTENING MAP", color = Acid, fontWeight = FontWeight.Bold)
                Text("既存の発掘履歴から再分析", color = Muted, fontSize = 11.sp)
                topGenres.forEach { (name, score) -> Text("$name  $score", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp)) }
            }
        }
    }
}

@Composable
private fun DnaBar(name: String, value: Float) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(name, color = Color.White, fontWeight = FontWeight.Bold); Text("${(value*100).toInt()}", color = Acid) }
        LinearProgressIndicator(progress = { value }, modifier = Modifier.fillMaxWidth().height(8.dp))
    }
}

@Composable
private fun VocalBar(name: String, value: Float) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, color = Color.White, modifier = Modifier.width(70.dp), fontSize = 11.sp)
        LinearProgressIndicator(progress = { value }, modifier = Modifier.weight(1f).height(7.dp))
        Text(" ${(value*100).toInt()}%", color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun SettingsScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val status by vm.spotifyStatus.collectAsState(); val syncing by vm.syncing.collectAsState(); val signals by vm.spotifySignals.collectAsState()
    val discoveryStatus by vm.discoveryStatus.collectAsState(); val discovering by vm.discovering.collectAsState(); val external by vm.externalArtists.collectAsState()
    val lens by vm.genreLens.collectAsState(); val backupStatus by vm.backupStatus.collectAsState()
    var clientId by remember { mutableStateOf(vm.clientId()) }; var lastFmKey by remember { mutableStateOf(vm.lastFmApiKey()) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(vm.exportBackupJson()) } }
            .onSuccess { vm.setBackupStatus("バックアップを書き出しました") }.onFailure { vm.setBackupStatus("書き出し失敗: ${it.message}") }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("読込失敗") }
            .onSuccess(vm::importBackupJson).onFailure { vm.setBackupStatus("読込失敗: ${it.message}") }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Header("ACCOUNT & PERSONALIZATION — YOUR METAL, YOUR DATA") }
        item { AccountSettingsCard(vm) }
        item {
            SettingsCard("GENRE LENS", "指定ジャンルを必須条件にし、そのジャンル内でDNAに合うArtistを選ぶ。候補不足時は先に地下を自動補充する。") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GenreLensMode.entries.forEach { mode -> FilterChip(selected = lens.mode == mode, onClick = { vm.setGenreLensMode(mode) }, label = { Text(mode.label) }) }
                }
                if (lens.mode == GenreLensMode.MANUAL) {
                    Text("手動ジャンル（複数可）", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    GenreSelector(selected = lens.manualGenres, onToggle = vm::toggleManualGenre)
                }
                if (lens.mode == GenreLensMode.WEEKDAY) {
                    Text("曜日ごとに複数ジャンル登録", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    DayOfWeek.values().forEach { day ->
                        Text("${GenreLensCatalog.dayLabel(day)}曜日", color = Acid, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                        GenreSelector(selected = lens.weekdayGenres[day.name].orEmpty(), onToggle = { vm.toggleWeekdayGenre(day, it) })
                    }
                }
                Text("今日: ${vm.activeGenres().ifEmpty { listOf("通常DNA推薦") }.joinToString(" / ")}", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            SettingsCard("PERSONAL METAL ARCHIVE", "Local Metal DBを図鑑として可視化。評価・Genre・Voで絞り込み、Spotify / YouTube / Deep Diveへ直行できる。") {
                Text("Archive ${vm.archiveArtists().size}組 / External ${external.size}組 / Genre ${vm.archiveGenreCounts().size}系統", color = Color.White, fontSize = 11.sp)
                Text("下部の『図鑑』タブから開く", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            SettingsCard("HIDDEN DISCOVERY ENGINE", "Last.fm + MusicBrainzで地下を掘り、取得ArtistをLocal DBへ蓄積し続ける。") {
                OutlinedTextField(value = lastFmKey, onValueChange = { lastFmKey = it }, label = { Text("Last.fm API Key") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Button(onClick = { vm.saveLastFmApiKey(lastFmKey); vm.syncExternalDiscovery() }, enabled = !discovering, modifier = Modifier.fillMaxWidth()) { Text(if (discovering) "外部を掘削中…" else "未知のMetalを発掘") }
                Text(discoveryStatus, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)); Text("Local Metal DB: ${external.size} external artists / 上限なし", color = Color.White, fontSize = 11.sp)
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            SettingsCard("SPOTIFY DNA SYNC", "Artist検索で完全一致を照合し、存在する場合はSpotify Artistページへ直接飛ぶ。") {
                OutlinedTextField(value = clientId, onValueChange = { clientId = it }, label = { Text("Spotify Client ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Button(onClick = { vm.saveClientId(clientId); vm.syncSpotify() }, enabled = !syncing, modifier = Modifier.fillMaxWidth()) { Text(if (syncing) "解析中…" else "Spotifyと接続してDNA更新") }
                Text(status, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)); signals.forEach { Text(it, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp)) }
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            SettingsCard("DATA SAFETY", "従来JSONバックアップ形式を維持。V0.4〜V0.6.xのJSONから復元するとSQLite Archiveを自動再構築する。") {
                Text("SQLite Mirror: ${vm.archiveDatabaseCount()} external artists", color = Color.White, fontSize = 11.sp)
                Text("Portable backup: metaranai-backup JSON", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
                Spacer(Modifier.height(10.dp))
                Button(onClick = { exportLauncher.launch("metaranai-backup-v0.9.0.json") }, modifier = Modifier.fillMaxWidth()) { Text("分析データをバックアップ") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }, modifier = Modifier.fillMaxWidth()) { Text("バックアップを復元") }
                if (backupStatus.isNotBlank()) Text(backupStatus, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, description: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().background(Card, RoundedCornerShape(22.dp)).padding(18.dp)) {
        Text(title, color = Acid, fontWeight = FontWeight.Bold); Text(description, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp)); Spacer(Modifier.height(10.dp)); content()
    }
}

@Composable
private fun GenreSelector(selected: Set<String>, onToggle: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        GenreLensCatalog.names().forEach { name -> FilterChip(selected = name in selected, onClick = { onToggle(name) }, label = { Text(name, fontSize = 10.sp) }) }
    }
}


@Composable
private fun OnboardingScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity
    val account by vm.account.collectAsState()
    val status by vm.accountStatus.collectAsState()
    var selected by remember { mutableStateOf(setOf<String>()) }
    var showEmail by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var createEmail by remember { mutableStateOf(true) }

    LazyColumn(Modifier.fillMaxSize().background(Bg), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("WELCOME TO", color = Muted, fontWeight = FontWeight.Bold)
            Text("メタらない？", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
            Text("YOUR METAL. YOUR DISCOVERY.", color = Acid, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("最初から誰かの好みに寄せません。ログインして記録を引き継ぐか、あなたのMetal DNAをここから作ります。", color = Muted)
        }
        item {
            Column(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(22.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ACCOUNT", color = Acid, fontWeight = FontWeight.Bold)
                if (account == null) {
                    Button(onClick = { activity?.let { vm.signInGoogle(it) } }, modifier = Modifier.fillMaxWidth()) { Text("Googleで続ける") }
                    OutlinedButton(onClick = { activity?.let { vm.signInProvider(it, "apple.com") } }, modifier = Modifier.fillMaxWidth()) { Text("Appleで続ける") }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { activity?.let { vm.signInFacebook(it) } }, modifier = Modifier.weight(1f)) { Text("Facebook") }
                        OutlinedButton(onClick = { activity?.let { vm.signInProvider(it, "twitter.com") } }, modifier = Modifier.weight(1f)) { Text("X") }
                    }
                    TextButton(onClick = { showEmail = !showEmail }, modifier = Modifier.fillMaxWidth()) { Text("メールアドレスで続ける") }
                    if (showEmail) {
                        OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(password, { password = it }, label = { Text("Password (6文字以上)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(createEmail, { createEmail = it }); Text(if (createEmail) "新規作成" else "ログイン", color = Color.White)
                        }
                        Button(onClick = { vm.signInEmail(email, password, createEmail) }, modifier = Modifier.fillMaxWidth()) { Text(if (createEmail) "アカウント作成" else "ログイン") }
                    }
                    OutlinedButton(onClick = vm::signInGuest, modifier = Modifier.fillMaxWidth()) { Text("アカウントなしで試す") }
                    if (!vm.cloudConfigured) Text("※ Google / Apple / Facebook / X / EmailはFirebase設定後に有効。ゲストはオフラインでも利用可能。", color = Muted, fontSize = 10.sp)
                } else {
                    Text("${account!!.displayName.ifBlank { account!!.email.ifBlank { "Guest" } }} で開始", color = Color.White)
                }
                if (status.isNotBlank()) Text(status, color = Muted, fontSize = 11.sp)
            }
        }
        item {
            Column(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(22.dp)).padding(18.dp)) {
                Text("BUILD YOUR METAL DNA", color = Acid, fontWeight = FontWeight.Bold)
                Text("好きなGenreを選択（複数可）。選んだGenreの平均から初期DNAを作り、以後の評価であなた専用に学習します。", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
                Button(onClick = vm::startWithSpotifyOnboarding, modifier = Modifier.fillMaxWidth()) { Text("🎧 Spotifyの視聴傾向から始める") }
                Text("またはGenreから初期DNAを作成", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
                GenreSelector(selected) { g -> selected = if (g in selected) selected - g else selected + g }
                Spacer(Modifier.height(12.dp))
                Button(onClick = { vm.completeOnboardingWithGenres(selected) }, enabled = selected.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("このGenreから始める") }
                Text("初回DNAは Spotify または Genre 選択で作成します。アカウントはゲストでも利用できます。", color = Muted, fontSize = 12.sp)
                Text("Spotify Client IDは公開ビルドではBuild Secretから設定可能。未設定時はGenre/探索で開始できます。", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun AccountSettingsCard(vm: MainViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity
    val account by vm.account.collectAsState()
    val status by vm.accountStatus.collectAsState()
    val pending by vm.legacyMigrationPending.collectAsState()
    val conflict by vm.cloudConflictPending.collectAsState()
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    SettingsCard("ACCOUNT & CLOUD SYNC", "Google / Apple / Facebook / X / Emailで記録をアカウントに紐付け。別端末では同じアカウントから復元。JSON Backupは非常用として維持。") {
        if (account == null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { activity?.let { vm.signInGoogle(it) } }, modifier = Modifier.weight(1f)) { Text("Google") }
                OutlinedButton(onClick = { activity?.let { vm.signInProvider(it, "apple.com") } }, modifier = Modifier.weight(1f)) { Text("Apple") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { activity?.let { vm.signInFacebook(it) } }, modifier = Modifier.weight(1f)) { Text("Facebook") }
                OutlinedButton(onClick = { activity?.let { vm.signInProvider(it, "twitter.com") } }, modifier = Modifier.weight(1f)) { Text("X") }
            }
            OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = { vm.signInEmail(email, password, true) }, modifier = Modifier.fillMaxWidth()) { Text("Emailで新規作成") }
            TextButton(onClick = { vm.signInEmail(email, password, false) }, modifier = Modifier.fillMaxWidth()) { Text("既存Emailでログイン") }
        } else {
            Text("SIGNED IN  ${account!!.provider}", color = Acid, fontWeight = FontWeight.Bold)
            Text(account!!.email.ifBlank { account!!.displayName.ifBlank { account!!.uid } }, color = Color.White, fontSize = 11.sp)
            if (conflict) {
                Spacer(Modifier.height(8.dp))
                Text("⚠ 端末とクラウドの両方に記録があります。自動上書きしません。", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Button(onClick = vm::resolveCloudConflictUseCloud, modifier = Modifier.fillMaxWidth()) { Text("クラウド記録をこの端末へ復元") }
                OutlinedButton(onClick = vm::resolveCloudConflictUseLocal, modifier = Modifier.fillMaxWidth()) { Text("この端末の記録でクラウドを更新") }
            } else if (pending) {
                Spacer(Modifier.height(8.dp))
                Text("この端末にV0.8以前の記録があります。クラウドへ引き継ぐまで端末データは変更しません。", color = Color.White, fontSize = 11.sp)
                Button(onClick = vm::migrateLocalDataToAccount, modifier = Modifier.fillMaxWidth()) { Text("この端末の記録をアカウントへ引き継ぐ") }
            }
            Button(onClick = vm::syncAccountNow, modifier = Modifier.fillMaxWidth()) { Text("今すぐクラウド同期") }
            OutlinedButton(onClick = vm::signOutAccount, modifier = Modifier.fillMaxWidth()) { Text("ログアウト") }
            TextButton(onClick = vm::deleteAccount, modifier = Modifier.fillMaxWidth()) { Text("アカウントを削除") }
        }
        if (!vm.cloudConfigured) Text("Firebase未設定：docs/17_ACCOUNT_AND_FIREBASE_SETUP.md を参照。ゲスト/既存Localデータはそのまま利用できます。", color = Muted, fontSize = 10.sp)
        if (status.isNotBlank()) Text(status, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
    }
    Spacer(Modifier.height(12.dp))
}

private fun formatCompact(n: Long): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
    n >= 1_000 -> String.format("%.1fK", n / 1_000.0)
    else -> n.toString()
}
