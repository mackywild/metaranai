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
private fun Header() {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("メタらない？", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text("メタルバンド探索アプリケーション", color = Muted, fontSize = 13.sp)
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

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Header() }
        if (activeGenres.isNotEmpty()) item {
            Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().background(Color(0xFF101010), RoundedCornerShape(16.dp)).padding(12.dp)) {
                Text("本日のジャンル: ${GenreLensCatalog.displayNames(activeGenres)}", color = Acid, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.height(10.dp))
        }
        if (lensBlocked) {
            item {
                Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().background(Card, RoundedCornerShape(28.dp)).padding(24.dp)) {
                    Text("ジャンル候補を探索中", color = Acid, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(if (lensPreparing) "${GenreLensCatalog.displayNames(activeGenres)} を探索中…" else "${GenreLensCatalog.displayNames(activeGenres)} の候補が不足しています", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(10.dp))
                    Text("指定ジャンルの未評価バンドを補充してから、あなたのDNAに合う今日の1組を選びます。", color = Muted, lineHeight = 20.sp)
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
                    Text("今日のメタル", color = Acid, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.height(18.dp))
                    Text(rec.artist.name, color = Color.White, fontSize = 33.sp, fontWeight = FontWeight.Black)
                    Text("${rec.artist.country}  •  ${rec.artist.genres.joinToString(" / ")}", color = Muted)
                    if (rec.artist.source != ArtistSource.BUILTIN) {
                        Spacer(Modifier.height(6.dp))
                        Text("🌐 外部発掘  •  発掘度 ${rec.artist.hiddenScore}", color = Acid, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(22.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${rec.compatibility}%", color = Acid, fontSize = 34.sp, fontWeight = FontWeight.Black)
                        Text("  DNA一致度", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpotifyButton(vm, rec.artist, Modifier.weight(1f))
                        OutlinedButton(onClick = { vm.openYouTube(rec.artist) }, modifier = Modifier.weight(1f)) { Text("YouTube") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.openYouTube(rec.artist, "mv") }, modifier = Modifier.weight(1f)) { Text("MV") }
                        OutlinedButton(onClick = { vm.openYouTube(rec.artist, "live") }, modifier = Modifier.weight(1f)) { Text("ライブ") }
                        OutlinedButton(onClick = { vm.deepDive(rec.artist) }, enabled = !deepDiving, modifier = Modifier.weight(1.35f)) { Text(if (deepDiving) "探索中" else "⛏ 深掘り") }
                    }
                    if (spotifyOpen.isNotBlank()) Text(spotifyOpen, color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
                    if (mediaStatus.isNotBlank()) Text(mediaStatus, color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = vm::shuffle, modifier = Modifier.fillMaxWidth()) { Text("別のバンドを見る") }
                }
            }
            if (deepDiveStatus.isNotBlank() || deepDiveResults.isNotEmpty()) item {
                DeepDivePanel(vm, deepDiveStatus, deepDiveResults, deepDiving)
            }
            item {
                Text("聴いた結果を教えてください", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp, 18.dp, 20.dp, 8.dp))
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
    val ratings = listOf(Reaction.LOVE_ALL, Reaction.HIT, Reaction.SOME, Reaction.MEH, Reaction.NO_INTEREST)
    Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        ratings.chunked(2).forEach { pair ->
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
        OutlinedButton(onClick = { onReaction(Reaction.NOT_FOUND) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Reaction.NOT_FOUND.label, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(Reaction.NOT_FOUND.description, color = Muted, fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun SpotifyButton(
    vm: MainViewModel,
    artist: MetalArtist,
    modifier: Modifier = Modifier,
    fontSize: Int = 12,
    onBeforeOpen: (() -> Unit)? = null
) {
    val availability by vm.spotifyAvailability.collectAsState()
    val key = artist.name.trim().lowercase()
    val available = availability[key]
    LaunchedEffect(key) { vm.checkSpotifyArtistAvailability(artist) }
    Button(
        onClick = {
            onBeforeOpen?.invoke()
            vm.openSpotifyArtist(artist)
        },
        enabled = available == true,
        modifier = modifier
    ) {
        Text(
            when (available) {
                true -> "Spotify"
                false -> "Spotify未対応"
                null -> "Spotify確認中"
            },
            fontSize = fontSize.sp
        )
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
private fun DeepDivePanel(vm: MainViewModel, status: String, results: List<MetalArtist>, loading: Boolean) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp).fillMaxWidth().background(Card, RoundedCornerShape(22.dp)).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("⛏ 深掘り", color = Acid, fontWeight = FontWeight.Bold)
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
                    SpotifyButton(vm, artist, Modifier.weight(1f), fontSize = 10)
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
        item { Header() }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; vm.clearRemoteSearch() },
                singleLine = true,
                label = { Text("バンド名 / 国 / ジャンル") },
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.searchExternal(query) },
                enabled = query.trim().length >= 2 && !remoteSearching,
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
            ) { Text(if (remoteSearching) "世界のメタルDBを探索中…" else "ローカルに無ければ世界から検索") }
            if (remoteStatus.isNotBlank()) Text(remoteStatus, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
            Text(
                if (query.isBlank()) "ローカル図鑑から発掘度の高い候補" else "検索結果 ${merged.size}件",
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
                if (artist.sourceSeed?.startsWith("Search:") == true) Text("🌐 外部検索からローカル図鑑へ保存済み", color = Muted, fontSize = 10.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpotifyButton(vm, artist, Modifier.weight(1f), onBeforeOpen = { vm.recordSearch(query.ifBlank { "discover" }, artist) })
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
        queryOk && reactionOk && genreOk
    }
    val visible = when (sortMode) {
        "HIDDEN" -> filtered.sortedByDescending { it.hiddenScore }
        "NAME" -> filtered.sortedBy { it.name.lowercase() }
        else -> filtered.sortedByDescending { profile.similarity(it.vector) }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Header() }
        item {
            Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("図鑑", archive.size.toString(), Modifier.weight(1f))
                StatCard("外部", external.size.toString(), Modifier.weight(1f))
                StatCard("評価済", ratedCount.toString(), Modifier.weight(1f))
                StatCard("💘", favorites.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                label = { Text("図鑑検索: バンド名 / 国 / ジャンル") },
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
                FilterChip(selected = genreFilter == null, onClick = { genreFilter = null }, label = { Text("全ジャンル") })
                GenreLensCatalog.names().forEach { genre ->
                    FilterChip(selected = genreFilter == genre, onClick = { genreFilter = if (genreFilter == genre) null else genre }, label = { Text(GenreLensCatalog.displayName(genre), fontSize = 10.sp) })
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("DNA" to "おすすめ順", "HIDDEN" to "発掘度順", "NAME" to "名前順").forEach { (key, label) ->
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
                Text("発掘度 ${artist.hiddenScore} • 新規性 ${(artist.discovery * 100).toInt()}%", color = Acid, fontSize = 10.sp)
                if (record != null) Text("最終評価 ${record.date} • 当時DNA一致度 ${record.score}%", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
                if (vm.spotifyLinkCached(artist)) Text("Spotify本人確認済みリンク取得済み", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpotifyButton(vm, artist, Modifier.weight(1f), fontSize = 11)
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
    val topGenres = vm.topGenres()
    val metrics = listOf(
        "メロディ重視" to p.melody,
        "疾走感" to p.speed,
        "重厚さ" to p.heavy,
        "シンフォニック" to p.symphonic,
        "技巧性" to p.technical,
        "グロウル" to p.growl,
        "クリーンボーカル" to p.cleanVocal,
        "キャッチーさ" to p.catchy
    )

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Header() }
        item {
            Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().background(Card, RoundedCornerShape(22.dp)).padding(18.dp)) {
                Text("あなたのメタルDNA", color = Acid, fontWeight = FontWeight.Bold)
                Text(vm.dnaType(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 5.dp))
            }
            Spacer(Modifier.height(10.dp))
        }
        items(metrics) { (label, value) ->
            Column(Modifier.padding(horizontal = 20.dp, vertical = 7.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("${(value * 100).toInt()}", color = Acid, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { value },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )
            }
        }
        if (topGenres.isNotEmpty()) item {
            Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().background(Card, RoundedCornerShape(22.dp)).padding(18.dp)) {
                Text("よく刺さっているジャンル", color = Acid, fontWeight = FontWeight.Bold)
                topGenres.forEach { (name, score) ->
                    Text("${GenreLensCatalog.displayName(name)}  $score", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
                }
            }
        }
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
        item { Header() }
        item { AccountSettingsCard(vm) }
        item {
            SettingsCard("ジャンルレンズ", "選択したジャンルの中から、DNAに合うバンドを探します。候補が不足した場合は自動で補充します。") {
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
                Text("本日のジャンル: ${GenreLensCatalog.displayNames(vm.activeGenres()).ifBlank { "指定なし" }}", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            SettingsCard("メタル図鑑", "発掘したバンドを保存し、評価やジャンルで絞り込めます。") {
                Text("図鑑 ${vm.archiveArtists().size}組 / 外部発掘 ${external.size}組 / ジャンル ${vm.archiveGenreCounts().size}系統", color = Color.White, fontSize = 11.sp)
                Text("下部の『図鑑』タブから開く", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            SettingsCard("外部発掘", "Last.fm + MusicBrainzから未知のメタルバンドを探し、図鑑へ保存します。") {
                OutlinedTextField(value = lastFmKey, onValueChange = { lastFmKey = it }, label = { Text("Last.fm API Key") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Button(onClick = { vm.saveLastFmApiKey(lastFmKey); vm.syncExternalDiscovery() }, enabled = !discovering, modifier = Modifier.fillMaxWidth()) { Text(if (discovering) "外部を掘削中…" else "未知のMetalを発掘") }
                Text(discoveryStatus, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)); Text("ローカル図鑑: ${external.size}組 / 保存上限なし", color = Color.White, fontSize = 11.sp)
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            SettingsCard("Spotify連携", "バンド名を完全一致で照合し、本人と確認できた場合だけSpotifyページを有効にします。") {
                OutlinedTextField(value = clientId, onValueChange = { clientId = it }, label = { Text("Spotify Client ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Button(onClick = { vm.saveClientId(clientId); vm.syncSpotify() }, enabled = !syncing, modifier = Modifier.fillMaxWidth()) { Text(if (syncing) "解析中…" else "Spotifyと接続してDNA更新") }
                Text(status, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)); signals.forEach { Text(it, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp)) }
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            SettingsCard("データ保護", "従来のJSONバックアップ形式を維持し、古いバックアップからも復元できます。") {
                Text("図鑑DB: ${vm.archiveDatabaseCount()}組", color = Color.White, fontSize = 11.sp)
                Text("互換バックアップ: metaranai-backup JSON", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
                Spacer(Modifier.height(10.dp))
                Button(onClick = { exportLauncher.launch("metaranai-backup-v0.9.2.json") }, modifier = Modifier.fillMaxWidth()) { Text("分析データをバックアップ") }
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
        GenreLensCatalog.names().forEach { name -> FilterChip(selected = name in selected, onClick = { onToggle(name) }, label = { Text(GenreLensCatalog.displayName(name), fontSize = 10.sp) }) }
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
            Text("ようこそ", color = Muted, fontWeight = FontWeight.Bold)
            Text("メタらない？", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
            Text("あなたのメタルを、あなたのために。", color = Acid, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("最初から誰かの好みに寄せません。ログインして記録を引き継ぐか、あなたのMetal DNAをここから作ります。", color = Muted)
        }
        item {
            Column(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(22.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("アカウント", color = Acid, fontWeight = FontWeight.Bold)
                if (account == null) {
                    Button(onClick = { activity?.let { vm.signInGoogle(it) } }, modifier = Modifier.fillMaxWidth()) { Text("Googleで続ける") }
                    OutlinedButton(onClick = { activity?.let { vm.signInProvider(it, "apple.com") } }, modifier = Modifier.fillMaxWidth()) { Text("Appleで続ける") }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { activity?.let { vm.signInFacebook(it) } }, modifier = Modifier.weight(1f)) { Text("Facebook") }
                        OutlinedButton(onClick = { activity?.let { vm.signInProvider(it, "twitter.com") } }, modifier = Modifier.weight(1f)) { Text("X") }
                    }
                    TextButton(onClick = { showEmail = !showEmail }, modifier = Modifier.fillMaxWidth()) { Text("メールアドレスで続ける") }
                    if (showEmail) {
                        OutlinedTextField(email, { email = it }, label = { Text("メールアドレス") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(password, { password = it }, label = { Text("パスワード（6文字以上）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
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
                Text("メタルDNAを作成", color = Acid, fontWeight = FontWeight.Bold)
                Text("好きなジャンルを選択（複数可）。選択したジャンルから初期DNAを作り、以後の評価であなた専用に学習します。", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
                Button(onClick = vm::startWithSpotifyOnboarding, modifier = Modifier.fillMaxWidth()) { Text("🎧 Spotifyの視聴傾向から始める") }
                Text("またはジャンルから初期DNAを作成", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
                GenreSelector(selected) { g -> selected = if (g in selected) selected - g else selected + g }
                Spacer(Modifier.height(12.dp))
                Button(onClick = { vm.completeOnboardingWithGenres(selected) }, enabled = selected.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("このジャンルから始める") }
                Text("初回DNAはSpotifyまたはジャンル選択で作成します。ゲストでも利用できます。", color = Muted, fontSize = 12.sp)
                Text("Spotify未連携でも、ジャンル選択から開始できます。", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
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
    SettingsCard("アカウントとクラウド同期", "Google / Apple / Facebook / X / メールで記録を引き継げます。JSONバックアップも非常用として維持します。") {
        if (account == null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { activity?.let { vm.signInGoogle(it) } }, modifier = Modifier.weight(1f)) { Text("Google") }
                OutlinedButton(onClick = { activity?.let { vm.signInProvider(it, "apple.com") } }, modifier = Modifier.weight(1f)) { Text("Apple") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { activity?.let { vm.signInFacebook(it) } }, modifier = Modifier.weight(1f)) { Text("Facebook") }
                OutlinedButton(onClick = { activity?.let { vm.signInProvider(it, "twitter.com") } }, modifier = Modifier.weight(1f)) { Text("X") }
            }
            OutlinedTextField(email, { email = it }, label = { Text("メールアドレス") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = { vm.signInEmail(email, password, true) }, modifier = Modifier.fillMaxWidth()) { Text("メールで新規作成") }
            TextButton(onClick = { vm.signInEmail(email, password, false) }, modifier = Modifier.fillMaxWidth()) { Text("既存メールでログイン") }
        } else {
            Text("ログイン中  ${account!!.provider}", color = Acid, fontWeight = FontWeight.Bold)
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
