package jp.metaranai.app

import android.os.Bundle
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
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
    var pendingLinkedSearch by remember { mutableStateOf<String?>(null) }
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
                    1 -> SearchScreen(vm, pendingLinkedSearch) { pendingLinkedSearch = null }
                    2 -> ArchiveScreen(vm)
                    3 -> DnaScreen(vm)
                    else -> SettingsScreen(vm) { source -> pendingLinkedSearch = source; tab = 1 }
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
                    Spacer(Modifier.height(22.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpotifyButton(vm, rec.artist, Modifier.weight(1f))
                        Button(
                            onClick = { vm.openYouTube(rec.artist) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF0000),
                                contentColor = Color.White
                            )
                        ) {
                            Text("YouTube", color = Color.White)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { vm.deepDive(rec.artist) },
                        enabled = !deepDiving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (deepDiving) "探索中" else "⛏ 深掘り")
                    }
                    if (spotifyOpen.isNotBlank() && spotifyOpen != "Spotify本人確認済み") {
                        Text(spotifyOpen, color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
                    }
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
private fun SearchScreen(vm: MainViewModel, pendingSource: String? = null, onSearchConsumed: () -> Unit = {}) {
    LaunchedEffect(pendingSource) {
        val source = pendingSource ?: return@LaunchedEffect
        onSearchConsumed()
        when (source) {
            "spotify" -> vm.syncSpotify()
            "lastfm" -> vm.syncLastFmProfile(vm.lastFmUsername())
            "discovery" -> vm.syncExternalDiscovery()
        }
    }
    val history by vm.searchHistory.collectAsState()
    val remote by vm.remoteSearchResults.collectAsState()
    val suggestions by vm.remoteSearchSuggestions.collectAsState()
    val remoteSearching by vm.remoteSearching.collectAsState()
    val remoteStatus by vm.remoteSearchStatus.collectAsState()
    val external by vm.externalArtists.collectAsState()
    val deepDiveResults by vm.deepDiveResults.collectAsState()
    val deepDiveStatus by vm.deepDiveStatus.collectAsState()
    val deepDiving by vm.deepDiving.collectAsState()
    var query by remember { mutableStateOf("") }

    val localResults = remember(query, external) { vm.search(query) }
    val merged = (localResults + remote).distinctBy { it.name.lowercase() }
    val suggestionResults = suggestions
        .filterNot { suggestion -> merged.any { it.name.equals(suggestion.name, true) } }
        .distinctBy { it.name.lowercase() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Header() }
        item { LinkedDiscoveryControls(vm) }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; vm.clearRemoteSearch() },
                singleLine = true,
                label = { Text("バンド名 / 国 / ジャンル") },
                supportingText = { Text("半角スペース区切りはAND検索") },
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.searchExternal(query) },
                enabled = query.trim().length >= 2 && !remoteSearching,
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
            ) {
                Text(if (remoteSearching) "グローバル検索中…" else "グローバル検索")
            }

            if (remoteStatus.isNotBlank()) {
                Text(
                    remoteStatus,
                    color = Muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            if (query.isNotBlank()) {
                Text(
                    "検索結果 ${merged.size}件",
                    color = Muted,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }

        items(merged) { artist ->
            SearchArtistCard(
                vm = vm,
                artist = artist,
                query = query,
                deepDiving = deepDiving
            )
        }

        if (suggestionResults.isNotEmpty()) {
            item {
                Text(
                    "もしかして…",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(20.dp, 14.dp, 20.dp, 4.dp)
                )
            }
            items(suggestionResults) { artist ->
                SearchArtistCard(
                    vm = vm,
                    artist = artist,
                    query = query,
                    deepDiving = deepDiving
                )
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
private fun SearchArtistCard(
    vm: MainViewModel,
    artist: MetalArtist,
    query: String,
    deepDiving: Boolean
) {
    Column(
        Modifier
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .fillMaxWidth()
            .background(Card, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Text(artist.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("${artist.country} • ${artist.genres.joinToString(" / ")}", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpotifyButton(
                vm,
                artist,
                Modifier.weight(1.15f),
                fontSize = 11,
                onBeforeOpen = { vm.recordSearch(query.ifBlank { "discover" }, artist) }
            )
            Button(
                onClick = {
                    vm.recordSearch(query.ifBlank { "discover" }, artist)
                    vm.openYouTube(artist)
                },
                modifier = Modifier.weight(1.15f),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF0000),
                    contentColor = Color.White
                )
            ) {
                Text("YouTube", color = Color.White, fontSize = 11.sp, maxLines = 1)
            }
            OutlinedButton(
                onClick = {
                    vm.recordSearch(query.ifBlank { "discover" }, artist)
                    vm.deepDive(artist)
                },
                enabled = !deepDiving,
                modifier = Modifier.weight(.70f),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Text("⛏", maxLines = 1)
            }
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
        val queryOk = query.isBlank() || SearchQueryMatcher.matches(artist, query)
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
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text("図鑑検索: バンド名 / 国 / ジャンル") },
                supportingText = { Text("半角スペース区切りはAND検索") },
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
                Text(
                    "発掘度 ${archiveStarRating(artist.hiddenScore)}  •  新規性 ${archiveStarRating((artist.discovery * 100).toInt())}",
                    color = Acid,
                    fontSize = 10.sp
                )
                if (record != null) {
                    Text(
                        "評価日時 ${formatEvaluationDateTime(record.date)}",
                        color = Muted,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpotifyButton(vm, artist, Modifier.weight(1f), fontSize = 11)
                    Button(
                        onClick = { vm.openYouTube(artist) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF0000),
                            contentColor = Color.White
                        )
                    ) {
                        Text("YouTube", color = Color.White, fontSize = 11.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { vm.deepDive(artist) },
                        enabled = !deepDiving,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("⛏", fontSize = 12.sp)
                    }
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


private fun archiveStarRating(score: Int): String {
    val filled = ((score.coerceIn(0, 100) + 19) / 20).coerceIn(0, 5)
    return "★".repeat(filled) + "☆".repeat(5 - filled)
}

private fun formatEvaluationDateTime(raw: String): String =
    raw.replace('T', ' ').let { if (it.length > 16) it.take(16) else it }


private val SavedClientIdMask = VisualTransformation { text ->
    TransformedText(AnnotatedString(maskClientId(text.text)), OffsetMapping.Identity)
}

@Composable
private fun LinkedDiscoveryControls(vm: MainViewModel) {
    val connecting by vm.connecting.collectAsState()
    val syncing by vm.syncing.collectAsState()
    val lastFmSyncing by vm.lastFmProfileSyncing.collectAsState()
    val discovering by vm.discovering.collectAsState()
    val spotifyStatus by vm.spotifyStatus.collectAsState()
    val lastFmStatus by vm.lastFmProfileStatus.collectAsState()
    val discoveryStatus by vm.discoveryStatus.collectAsState()
    Column(Modifier.padding(20.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("連携した好みから探す", color = Acid, fontWeight = FontWeight.Bold)
        Text("連携情報は設定で保存。ここから履歴を解析してDNAを更新し、未知のMetalを発掘できます。", color = Muted, fontSize = 11.sp)
        Button(onClick = vm::syncSpotify, enabled = vm.clientId().isNotBlank() && !syncing && !lastFmSyncing && !discovering && !connecting, modifier = Modifier.fillMaxWidth()) {
            Text(if (syncing) "Spotifyを解析中…" else "Spotifyにログインして好みを解析")
        }
        if (spotifyStatus.isNotBlank()) Text(spotifyStatus.substringBefore("Top:"), color = Muted, fontSize = 11.sp)
        Button(onClick = { vm.syncLastFmProfile(vm.lastFmUsername()) }, enabled = vm.lastFmUsername().isNotBlank() && vm.lastFmConfigured() && !lastFmSyncing && !syncing && !discovering && !connecting, modifier = Modifier.fillMaxWidth()) {
            Text(if (lastFmSyncing) "Last.fmを解析中…" else "Last.fm履歴から解析・発掘")
        }
        Text(lastFmStatus, color = Muted, fontSize = 11.sp)
        Button(onClick = vm::syncExternalDiscovery, enabled = vm.lastFmConfigured() && !discovering && !syncing && !lastFmSyncing, modifier = Modifier.fillMaxWidth()) {
            Text(if (discovering) "発掘中…" else "好みから未知のMetalを発掘")
        }
        if (discoveryStatus.isNotBlank()) Text(discoveryStatus, color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun DnaScreen(vm: MainViewModel) {
    val topArtists by vm.spotifyTopArtists.collectAsState()
    val spotifyStatus by vm.spotifyStatus.collectAsState()
    val signals by vm.spotifySignals.collectAsState()
    val p by vm.profile.collectAsState()
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
        item {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Spotify Topアーティスト", color = Acid, fontWeight = FontWeight.Bold)
                Text("過去約6か月の上位アーティスト（最終同期時点）", color = Muted, fontSize = 11.sp)
                if (topArtists.isEmpty()) Text("検索タブからSpotifyを解析すると表示されます。", color = Muted, fontSize = 12.sp)
                topArtists.forEachIndexed { index, name -> Text("${index + 1}. $name", color = Color.White, fontSize = 12.sp) }
                Text(spotifyStatus, color = Muted, fontSize = 11.sp)
                signals.forEach { Text(it, color = Muted, fontSize = 11.sp) }
            }
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
    }
}

private enum class SettingsPanel {
    ACCOUNT, GENRE, ARCHIVE, DISCOVERY, SPOTIFY, LASTFM, BACKUP
}

private const val PRIVACY_POLICY_URL = "https://mackywild.github.io/metaranai/privacy-policy.html"

@Composable
private fun SettingsScreen(vm: MainViewModel, onLinkedSearch: (String) -> Unit = {}) {
    val connectionStatus by vm.connectionStatus.collectAsState()
    val connecting by vm.connecting.collectAsState()
    val context = LocalContext.current
    val account by vm.account.collectAsState()
    val status by vm.spotifyStatus.collectAsState()
    val syncing by vm.syncing.collectAsState()
    val signals by vm.spotifySignals.collectAsState()
    val lastFmProfileStatus by vm.lastFmProfileStatus.collectAsState()
    val lastFmProfileSyncing by vm.lastFmProfileSyncing.collectAsState()
    val discoveryStatus by vm.discoveryStatus.collectAsState()
    val discovering by vm.discovering.collectAsState()
    val lens by vm.genreLens.collectAsState()
    val backupStatus by vm.backupStatus.collectAsState()

    var openPanel by remember { mutableStateOf<SettingsPanel?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var clientId by remember { mutableStateOf(vm.clientId()) }
    var clientIdSaved by remember { mutableStateOf(vm.clientId().isNotBlank()) }
    var lastFmUsername by remember { mutableStateOf(vm.lastFmUsername()) }
    var lastFmKey by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(vm.exportBackupJson()) }
            }.onSuccess {
                vm.setBackupStatus("バックアップを書き出しました")
            }.onFailure {
                vm.setBackupStatus("書き出し失敗: ${it.message}")
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("読込失敗")
            }.onSuccess(vm::importBackupJson).onFailure {
                vm.setBackupStatus("読込失敗: ${it.message}")
            }
        }
    }

    val accountSummary = when {
        account == null -> "未ログイン"
        account!!.isGuest -> "ゲスト利用中"
        else -> "${accountProviderLabel(account!!.provider)}でログイン中"
    }
    val activeGenreSummary = GenreLensCatalog.displayNames(vm.activeGenres()).ifBlank { "指定なし" }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Header() }

        item {
            Text(
                "設定",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        item {
            SettingsMenuItem(
                title = "アカウント・同期",
                summary = accountSummary,
                onClick = { openPanel = SettingsPanel.ACCOUNT }
            )
        }
        item {
            SettingsMenuItem(
                title = "ジャンル固定",
                summary = "本日: $activeGenreSummary",
                onClick = { openPanel = SettingsPanel.GENRE }
            )
        }
        item {
            SettingsMenuItem(
                title = "Spotify連携",
                summary = if (vm.clientId().isBlank()) "未設定" else "Spotifyの視聴傾向をDNAへ反映",
                onClick = { openPanel = SettingsPanel.SPOTIFY }
            )
        }
        item {
            SettingsMenuItem(
                title = "Last.fm連携（任意）",
                summary = if (vm.lastFmUsername().isBlank()) {
                    "長期の視聴履歴からDNAと発掘Seedを強化"
                } else {
                    "@${vm.lastFmUsername()} の公開履歴を反映"
                },
                onClick = { openPanel = SettingsPanel.LASTFM }
            )
        }
        item {
            SettingsMenuItem(
                title = "外部検索・発掘",
                summary = if (vm.lastFmConfigured()) "Last.fm / MusicBrainz 発掘基盤: 利用可能" else "Last.fm API未設定",
                onClick = { openPanel = SettingsPanel.DISCOVERY }
            )
        }
        item {
            SettingsMenuItem(
                title = "図鑑データ",
                summary = "保存したバンド情報と図鑑の状態",
                onClick = { openPanel = SettingsPanel.ARCHIVE }
            )
        }
        item {
            SettingsMenuItem(
                title = "バックアップ",
                summary = "JSON形式で書き出し・復元",
                onClick = { openPanel = SettingsPanel.BACKUP }
            )
        }

        item {
            Spacer(Modifier.height(6.dp))
            Text(
                "その他",
                color = Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
        item {
            SettingsMenuItem(
                title = "プライバシーポリシー",
                summary = "データの取得・利用・削除について確認",
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
                    }
                }
            )
        }
        if (account != null) {
            item {
                SettingsMenuItem(
                    title = "アカウント削除",
                    summary = "クラウド上のアカウントと同期データを削除",
                    destructive = true,
                    onClick = { showDeleteConfirm = true }
                )
            }
        }
    }

    when (openPanel) {
        SettingsPanel.ACCOUNT -> SettingsDialogShell(
            title = "アカウント・同期",
            onDismiss = { openPanel = null }
        ) {
            AccountSettingsContent(vm)
        }

        SettingsPanel.GENRE -> SettingsDialogShell(
            title = "ジャンル固定",
            onDismiss = { openPanel = null }
        ) {
            Text(
                "おすすめ対象を指定ジャンルに固定します。候補が不足した場合は自動で補充します。",
                color = Muted,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GenreLensMode.entries.forEach { mode ->
                    val label = when (mode) {
                        GenreLensMode.OFF -> "指定なし"
                        GenreLensMode.WEEKDAY -> "曜日固定"
                        GenreLensMode.MANUAL -> "手動固定"
                    }
                    FilterChip(
                        selected = lens.mode == mode,
                        onClick = { vm.setGenreLensMode(mode) },
                        label = { Text(label, fontSize = 10.sp) }
                    )
                }
            }
            if (lens.mode == GenreLensMode.MANUAL) {
                Text("固定するジャンル（複数可）", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                GenreSelector(selected = lens.manualGenres, onToggle = vm::toggleManualGenre)
            }
            if (lens.mode == GenreLensMode.WEEKDAY) {
                Text("曜日ごとに固定するジャンル", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                DayOfWeek.values().forEach { day ->
                    Text(
                        "${GenreLensCatalog.dayLabel(day)}曜日",
                        color = Acid,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    GenreSelector(
                        selected = lens.weekdayGenres[day.name].orEmpty(),
                        onToggle = { vm.toggleWeekdayGenre(day, it) }
                    )
                }
            }
            Text(
                "本日の固定ジャンル: ${GenreLensCatalog.displayNames(vm.activeGenres()).ifBlank { "指定なし" }}",
                color = Muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        SettingsPanel.SPOTIFY -> SettingsDialogShell(
            title = "Spotify連携", onDismiss = { openPanel = null }
        ) {
            Text("ここではログイン・連携確認だけを行います。履歴解析と発掘は「探す」、Topアーティストは「DNA」に表示します。", color = Muted, fontSize = 12.sp)
            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://accounts.spotify.com/"))) }) { Text("Spotifyにログイン / 新規登録") }
            if (BuildConfig.SPOTIFY_CLIENT_ID.isBlank()) {
                Text("Client ID取得手順\n1. 開発者Dashboardにログイン\n2. アプリを作成／既存アプリを選択\n3. Redirect URIに http://127.0.0.1:8888/callback を登録して保存\n4. SettingsのClient IDをコピーして下に貼り付け", color = Muted, fontSize = 11.sp)
                Text("Client IDはSpotifyのユーザー名とは別の、アプリ用IDです。Client Secretは入力しません。", color = Muted, fontSize = 11.sp)
                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://developer.spotify.com/dashboard"))) }) { Text("Client IDの取得ページを開く") }
            } else {
                Text("アプリ標準のClient IDを利用できます。取得・コピーは不要です。", color = Muted, fontSize = 11.sp)
            }
            OutlinedTextField(
                value = clientId, onValueChange = { clientId = it },
                readOnly = clientIdSaved,
                visualTransformation = if (clientIdSaved) SavedClientIdMask else VisualTransformation.None,
                label = { Text("Spotify Client ID") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), enabled = !connecting && !syncing
            )
            Button(onClick = { vm.connectSpotify(clientId); clientIdSaved = clientId.trim().matches(Regex("[a-fA-F0-9]{32}")) }, enabled = clientId.isNotBlank() && !connecting && !syncing && !lastFmProfileSyncing && !discovering, modifier = Modifier.fillMaxWidth()) {
                Text(if (connecting) "連携を確認中…" else if (vm.spotifyConnected()) "Spotify連携を確認" else "Spotifyにログインして連携")
            }
            TextButton(onClick = { clientIdSaved = false; clientId = "" }, enabled = !connecting && !syncing) { Text("Client IDを変更") }
            if (connectionStatus.startsWith("Spotify")) Text(connectionStatus, color = Muted, fontSize = 11.sp)
            Button(onClick = { openPanel = null; onLinkedSearch("spotify") }, enabled = vm.spotifyConnected() && clientId.trim() == vm.clientId() && !connecting && !syncing && !lastFmProfileSyncing && !discovering, modifier = Modifier.fillMaxWidth()) { Text("「探す」へ移動して好みを解析") }
        }

        SettingsPanel.LASTFM -> SettingsDialogShell(
            title = "Last.fm連携（任意）", onDismiss = { openPanel = null }
        ) {
            Text("ここではユーザー名の存在を確認して保存します。Last.fm本人認証や履歴解析は行いません。履歴解析と発掘は「探す」で実行します。", color = Muted, fontSize = 12.sp)
            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.last.fm/login"))) }) { Text("Last.fmにログイン") }
            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.last.fm/join"))) }) { Text("Last.fmに新規登録") }
            Text("ユーザー名の取得手順\n1. Last.fmにログイン／登録\n2. 自分のプロフィールを開く\n3. URLの /user/ の後ろのユーザー名を入力\n例: last.fm/user/metal_fan → metal_fan\nパスワード・API Keyの入力は不要です。登録直後は視聴履歴がまだ少ない場合があります。", color = Muted, fontSize = 11.sp)
            OutlinedTextField(value = lastFmUsername, onValueChange = { lastFmUsername = it }, label = { Text("Last.fmユーザー名") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !connecting && !lastFmProfileSyncing)
            Button(onClick = { vm.verifyLastFmUsername(lastFmUsername) }, enabled = lastFmUsername.isNotBlank() && vm.lastFmConfigured() && !connecting && !syncing && !lastFmProfileSyncing && !discovering, modifier = Modifier.fillMaxWidth()) { Text(if (connecting) "ユーザー名を確認中…" else "ユーザー名を確認して連携") }
            if (!vm.lastFmConfigured()) Text("Last.fm接続基盤が未設定です。外部検索・発掘の設定を確認してください。", color = Muted, fontSize = 11.sp)
            if (connectionStatus.startsWith("Last.fm")) Text(connectionStatus, color = Muted, fontSize = 11.sp)
            Button(onClick = { openPanel = null; onLinkedSearch("lastfm") }, enabled = vm.lastFmUsernameVerified() && lastFmUsername.trim().equals(vm.lastFmUsername(), true) && !connecting && !syncing && !lastFmProfileSyncing && !discovering, modifier = Modifier.fillMaxWidth()) { Text("「探す」へ移動して解析・発掘") }
            if (vm.lastFmUsername().isNotBlank()) TextButton(onClick = { vm.clearLastFmProfile(); lastFmUsername = "" }, enabled = !connecting && !lastFmProfileSyncing) { Text("Last.fm連携を解除") }
        }

        SettingsPanel.DISCOVERY -> SettingsDialogShell(
            title = "外部検索・発掘",
            onDismiss = { openPanel = null }
        ) {
            Text(
                "Last.fm + MusicBrainzから未知のメタルバンドを探し、図鑑へ保存します。Last.fmアカウント登録は不要です。",
                color = Muted,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))
            if (!vm.lastFmConfigured()) {
                Text(
                    "このビルドにはLast.fm API Keyが設定されていません。開発・テスト用Keyを入力してください。",
                    color = Muted,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = lastFmKey,
                    onValueChange = { lastFmKey = it },
                    label = { Text("Last.fm API Key（開発用）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
            } else {
                Text("Last.fm発掘API: 接続設定済み", color = Acid, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
            }
            Button(
                onClick = {
                    if (lastFmKey.isNotBlank()) vm.saveLastFmApiKey(lastFmKey)

                },
                enabled = !discovering && (vm.lastFmConfigured() || lastFmKey.isNotBlank()),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("接続設定を保存")
            }
            TextButton(onClick = { openPanel = null; onLinkedSearch("discovery") }, enabled = vm.lastFmConfigured() && !connecting && !discovering) { Text("「探す」へ移動して発掘") }
            if (discoveryStatus.isNotBlank()) {
                Text(discoveryStatus, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }

        SettingsPanel.ARCHIVE -> SettingsDialogShell(
            title = "図鑑データ",
            onDismiss = { openPanel = null }
        ) {
            Text(
                "発掘・検索したバンド情報をローカル図鑑として保持しています。",
                color = Muted,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))
            Text("登録バンド: ${vm.archiveArtists().size}組", color = Color.White, fontSize = 12.sp)
            Text("ジャンル: ${vm.archiveGenreCounts().size}系統", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
            Text("閲覧・評価は下部の「図鑑」タブから行えます。", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
        }

        SettingsPanel.BACKUP -> SettingsDialogShell(
            title = "バックアップ",
            onDismiss = { openPanel = null }
        ) {
            Text(
                "従来のmetaranai-backup JSON形式を維持し、古いバックアップからも復元できます。",
                color = Muted,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { exportLauncher.launch("metaranai-backup-v0.11.0.json") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("分析データをバックアップ")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("バックアップを復元")
            }
            if (backupStatus.isNotBlank()) {
                Text(backupStatus, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }

        null -> Unit
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("アカウントを削除しますか？") },
            text = {
                Text(
                    "Firebase上のアカウントとクラウド同期データを削除します。端末内のLocal DBやJSONデータは残ります。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        vm.deleteAccount()
                    }
                ) {
                    Text("削除する", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
private fun SettingsMenuItem(
    title: String,
    summary: String,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        color = Card,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = if (destructive) MaterialTheme.colorScheme.error else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(summary, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
            }
            Text("›", color = if (destructive) MaterialTheme.colorScheme.error else Acid, fontSize = 24.sp)
        }
    }
}

@Composable
private fun SettingsDialogShell(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                content()
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}

private fun accountProviderLabel(provider: String): String = when (provider) {
    "password" -> "メール"
    "google.com" -> "Google"
    "apple.com" -> "Apple"
    "facebook.com" -> "Facebook"
    "twitter.com" -> "X"
    "guest" -> "ゲスト"
    else -> provider
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
    val lastFmStatus by vm.lastFmProfileStatus.collectAsState()
    val lastFmSyncing by vm.lastFmProfileSyncing.collectAsState()
    var selected by remember { mutableStateOf(setOf<String>()) }
    var lastFmUsername by remember { mutableStateOf("") }
    var showEmail by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var createEmail by remember { mutableStateOf(true) }
    var showOtherProviders by remember { mutableStateOf(false) }

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
                    Text("おすすめ", color = Muted, fontSize = 10.sp)
                    Button(onClick = { activity?.let { vm.signInGoogle(it) } }, modifier = Modifier.fillMaxWidth()) {
                        Text("Googleで続ける")
                    }

                    TextButton(onClick = { showEmail = !showEmail }, modifier = Modifier.fillMaxWidth()) {
                        Text("メールアドレスで続ける")
                    }
                    if (showEmail) {
                        OutlinedTextField(email, { email = it }, label = { Text("メールアドレス") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(password, { password = it }, label = { Text("パスワード（6文字以上）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(createEmail, { createEmail = it }); Text(if (createEmail) "新規作成" else "ログイン", color = Color.White)
                        }
                        Button(onClick = { vm.signInEmail(email, password, createEmail) }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (createEmail) "確認メールを送信して登録" else "ログイン")
                        }
                        if (createEmail) Text("登録後、届いた確認メールのリンクを開いてからログインします。", color = Muted, fontSize = 10.sp)
                    }

                    TextButton(onClick = { showOtherProviders = !showOtherProviders }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (showOtherProviders) "その他のログインを閉じる" else "その他のログイン")
                    }
                    if (showOtherProviders) {
                        OutlinedButton(onClick = { activity?.let { vm.signInProvider(it, "apple.com") } }, modifier = Modifier.fillMaxWidth()) { Text("Appleで続ける") }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { activity?.let { vm.signInFacebook(it) } }, modifier = Modifier.weight(1f)) { Text("Facebook") }
                            OutlinedButton(onClick = { activity?.let { vm.signInProvider(it, "twitter.com") } }, modifier = Modifier.weight(1f)) { Text("X") }
                        }
                    }

                    OutlinedButton(onClick = vm::signInGuest, modifier = Modifier.fillMaxWidth()) { Text("アカウントなしで試す") }

                    when {
                        !vm.authConfigured -> Text("※ Firebase Authentication未設定。Google/メール等を使うにはFirebase設定が必要です。ゲストは利用できます。", color = Muted, fontSize = 10.sp)
                        !vm.googleConfigured -> Text("※ メール認証は利用可能です。GoogleログインにはGOOGLE_WEB_CLIENT_IDの設定が必要です。", color = Muted, fontSize = 10.sp)
                        !vm.cloudConfigured -> Text("※ ログインは利用可能です。クラウド同期のみFirebase Storage未設定のため無効です。", color = Muted, fontSize = 10.sp)
                    }
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
                Spacer(Modifier.height(8.dp))
                Text("Last.fmを使っているなら、長期の視聴履歴から開始できます（任意）", color = Muted, fontSize = 10.sp)
                OutlinedTextField(
                    value = lastFmUsername,
                    onValueChange = { lastFmUsername = it },
                    label = { Text("Last.fmユーザー名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { vm.startWithLastFmOnboarding(lastFmUsername) },
                    enabled = lastFmUsername.isNotBlank() && !lastFmSyncing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (lastFmSyncing) "Last.fm履歴を解析中…" else "⏱ Last.fmの視聴履歴から始める")
                }
                if (lastFmStatus.isNotBlank() && lastFmStatus != "未連携") {
                    Text(lastFmStatus, color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Text("またはジャンルから初期DNAを作成", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
                GenreSelector(selected) { g -> selected = if (g in selected) selected - g else selected + g }
                Spacer(Modifier.height(12.dp))
                Button(onClick = { vm.completeOnboardingWithGenres(selected) }, enabled = selected.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("このジャンルから始める") }
                Text("初回DNAはSpotify / Last.fm（任意）/ ジャンル選択のどれからでも作成できます。ゲストでも利用できます。", color = Muted, fontSize = 12.sp)
                Text("Last.fm未登録でも発掘機能は利用できます。Last.fm連携は履歴による精度ブーストです。", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun AccountSettingsContent(vm: MainViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity
    val account by vm.account.collectAsState()
    val status by vm.accountStatus.collectAsState()
    val pending by vm.legacyMigrationPending.collectAsState()
    val conflict by vm.cloudConflictPending.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Text(
        "Google / Apple / Facebook / X / メールで記録を引き継げます。",
        color = Muted,
        fontSize = 12.sp
    )
    Spacer(Modifier.height(10.dp))

    if (account == null) {
        Text("おすすめ", color = Muted, fontSize = 10.sp)
        Button(
            onClick = { activity?.let { vm.signInGoogle(it) } },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Googleで続ける")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            email,
            { email = it },
            label = { Text("メールアドレス") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            password,
            { password = it },
            label = { Text("パスワード（6文字以上）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { vm.signInEmail(email, password, true) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("確認メールを送信して登録")
        }
        TextButton(
            onClick = { vm.signInEmail(email, password, false) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("既存メールでログイン")
        }
        Text("その他のログイン", color = Muted, fontSize = 10.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
                onClick = { activity?.let { vm.signInProvider(it, "apple.com") } },
                modifier = Modifier.weight(1f)
            ) { Text("Apple") }
            OutlinedButton(
                onClick = { activity?.let { vm.signInFacebook(it) } },
                modifier = Modifier.weight(1f)
            ) { Text("Facebook") }
            OutlinedButton(
                onClick = { activity?.let { vm.signInProvider(it, "twitter.com") } },
                modifier = Modifier.weight(1f)
            ) { Text("X") }
        }
    } else {
        Text(
            "ログイン中  ${accountProviderLabel(account!!.provider)}",
            color = Acid,
            fontWeight = FontWeight.Bold
        )
        Text(
            account!!.email.ifBlank { account!!.displayName.ifBlank { account!!.uid } },
            color = Color.White,
            fontSize = 11.sp
        )

        if (conflict) {
            Spacer(Modifier.height(8.dp))
            Text(
                "⚠ 端末とクラウドの両方に記録があります。自動上書きしません。",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = vm::resolveCloudConflictUseCloud,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("クラウド記録をこの端末へ復元")
            }
            OutlinedButton(
                onClick = vm::resolveCloudConflictUseLocal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("この端末の記録でクラウドを更新")
            }
        } else if (pending) {
            Spacer(Modifier.height(8.dp))
            Text(
                "この端末にV0.8以前の記録があります。クラウドへ引き継ぐまで端末データは変更しません。",
                color = Color.White,
                fontSize = 11.sp
            )
            Button(
                onClick = vm::migrateLocalDataToAccount,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("この端末の記録をアカウントへ引き継ぐ")
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = vm::syncAccountNow,
            enabled = vm.cloudConfigured,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("今すぐクラウド同期")
        }
        if (!vm.cloudConfigured) {
            Text(
                "クラウド同期はFirebase Storage設定後に利用できます。ログイン自体は有効です。",
                color = Muted,
                fontSize = 10.sp
            )
        }
        OutlinedButton(
            onClick = vm::signOutAccount,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ログアウト")
        }
    }

    when {
        !vm.authConfigured -> Text(
            "Firebase Authentication未設定。ゲスト/既存Localデータは利用できます。",
            color = Muted,
            fontSize = 10.sp
        )
        !vm.googleConfigured -> Text(
            "メール認証は利用可能です。Googleログイン設定を確認してください。",
            color = Muted,
            fontSize = 10.sp
        )
        !vm.cloudConfigured -> Text(
            "ログインは利用できますが、クラウド同期は現在無効です。",
            color = Muted,
            fontSize = 10.sp
        )
    }

    if (status.isNotBlank()) {
        Text(status, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

private fun formatCompact(n: Long): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
    n >= 1_000 -> String.format("%.1fK", n / 1_000.0)
    else -> n.toString()
}
