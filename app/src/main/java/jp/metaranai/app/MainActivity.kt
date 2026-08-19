package jp.metaranai.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.net.URLEncoder

private val Bg = Color(0xFF090909)
private val Card = Color(0xFF151515)
private val Acid = Color(0xFFD6FF36)
private val Muted = Color(0xFFA4A4A4)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MetaranaiApp() }
    }
}

@Composable
fun MetaranaiApp(vm: MainViewModel = viewModel()) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("今日", "探す", "発掘", "DNA", "設定")
    val icons = listOf("⚡", "🔎", "🔥", "🧬", "⚙")
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
                    2 -> HistoryScreen(vm)
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
            Text("v0.4", color = Acid, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Text(subtitle, color = Muted, fontSize = 13.sp)
    }
}

@Composable
private fun HomeScreen(vm: MainViewModel) {
    val rec by vm.recommendation.collectAsState()
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Header("まだ知らない、今日の1バンド。") }
        item {
            Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().background(Card, RoundedCornerShape(28.dp)).padding(24.dp)) {
                Text("TODAY'S おすすメタル", color = Acid, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(18.dp))
                Text(rec.artist.name, color = Color.White, fontSize = 33.sp, fontWeight = FontWeight.Black)
                Text("${rec.artist.country}  •  ${rec.artist.genres.joinToString(" / ")}", color = Muted)
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
                Spacer(Modifier.height(18.dp))
                ScoreBreakdown(rec.breakdown)
                if (rec.artist.source != ArtistSource.BUILTIN) {
                    Spacer(Modifier.height(12.dp))
                    ExternalMeta(rec.artist)
                }
                Spacer(Modifier.height(22.dp))
                Button(onClick = { openSpotify(context, rec.artist.name) }, modifier = Modifier.fillMaxWidth()) { Text("Spotifyで聴く") }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = vm::shuffle, modifier = Modifier.fillMaxWidth()) { Text("別の沼も見る") }
            }
        }
        item {
            Text("聴いた結果を教えろ", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp, 18.dp, 20.dp, 8.dp))
            Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Reaction.entries.forEach { r ->
                    AssistChip(onClick = { vm.react(r) }, label = { Text(r.label, fontSize = 11.sp) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ScoreBreakdown(b: RecommendationBreakdown) {
    Column(Modifier.fillMaxWidth().background(Bg, RoundedCornerShape(16.dp)).padding(14.dp)) {
        Text("WHY THIS BAND?  SCORE ${b.total}", color = Acid, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        listOf("相性" to b.affinity, "HIDDEN" to b.hidden, "未知" to b.novelty, "探索" to b.exploration, "発掘度" to b.discovery).forEach { (name, value) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, color = Muted, fontSize = 12.sp)
                Text("+$value", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        }
        if (stats.isNotEmpty()) Text(stats.joinToString("  •  "), color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        a.mbid?.let { Text("MBID ${it.take(8)}…", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp)) }
    }
}

@Composable
private fun SearchScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val history by vm.searchHistory.collectAsState()
    var query by remember { mutableStateOf("") }
    val results = remember(query) { vm.search(query) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Header("検索も、お前の好みとして少しだけ覚える。") }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text("バンド / 国 / ジャンル") },
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Text(if (query.isBlank()) "発掘度の高い候補" else "検索結果 ${results.size}件", color = Muted, modifier = Modifier.padding(horizontal = 20.dp))
        }
        items(results) { artist ->
            Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth().background(Card, RoundedCornerShape(18.dp)).padding(16.dp)) {
                Text(artist.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${artist.country} • ${artist.genres.joinToString(" / ")}", color = Muted, fontSize = 12.sp)
                Text("発掘度 ${(artist.discovery * 100).toInt()}%${if (artist.source != ArtistSource.BUILTIN) "  •  🌐 HIDDEN ${artist.hiddenScore}" else ""}", color = Acid, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    vm.recordSearch(query.ifBlank { "discover" }, artist)
                    openSpotify(context, artist.name)
                }, modifier = Modifier.fillMaxWidth()) { Text("Spotifyで探す") }
            }
        }
        if (history.isNotEmpty()) item {
            Spacer(Modifier.height(10.dp))
            Text("最近の探索", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp, 8.dp))
            Text(history.take(5).joinToString("  •  ") { it.artistName }, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp))
        }
    }
}

@Composable
private fun HistoryScreen(vm: MainViewModel) {
    val history by vm.history.collectAsState()
    val stats = vm.stats()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Header("刺さったバンドは、お前の財産。") }
        item {
            Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("発掘", stats.total.toString(), Modifier.weight(1f))
                StatCard("HIT", stats.hits.toString(), Modifier.weight(1f))
                StatCard("刺さり率", "${stats.hitRate}%", Modifier.weight(1f))
                StatCard("連続", "${stats.streakDays}日", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
        if (history.isEmpty()) item {
            Text("まだ発掘記録がない。今日の1バンドを評価するとここに残る。", color = Muted, modifier = Modifier.padding(20.dp))
        }
        items(history) { r ->
            Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth().background(Card, RoundedCornerShape(18.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(r.artistName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("${r.date}  •  相性 ${r.score}%", color = Muted, fontSize = 12.sp)
                }
                Text(r.reaction.label, color = Acid)
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Card, RoundedCornerShape(14.dp)).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Acid, fontWeight = FontWeight.Black, fontSize = 17.sp)
        Text(label, color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun DnaScreen(vm: MainViewModel) {
    val p by vm.profile.collectAsState()
    val values = listOf("MELODY" to p.melody,"SPEED" to p.speed,"HEAVINESS" to p.heavy,"SYMPHONIC" to p.symphonic,"TECHNICAL" to p.technical,"GROWL" to p.growl,"CLEAN VOCAL" to p.cleanVocal,"CATCHINESS" to p.catchy)
    val strongest = p.traits().sortedByDescending { it.second }.take(3)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Header("YOUR METAL DNA") }
        item {
            Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().background(Card, RoundedCornerShape(22.dp)).padding(18.dp)) {
                Text("TYPE", color = Muted, fontSize = 11.sp)
                Text(vm.dnaType(), color = Acid, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                Text("支配的特性: ${strongest.joinToString(" / ") { "${it.first} ${(it.second*100).toInt()}" }}", color = Color.White, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
        }
        items(values) { (name, value) ->
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("${(value*100).toInt()}", color = Acid)
                }
                LinearProgressIndicator(progress = { value }, modifier = Modifier.fillMaxWidth().height(8.dp))
            }
        }
    }
}

@Composable
private fun SettingsScreen(vm: MainViewModel) {
    val status by vm.spotifyStatus.collectAsState()
    val syncing by vm.syncing.collectAsState()
    val signals by vm.spotifySignals.collectAsState()
    val discoveryStatus by vm.discoveryStatus.collectAsState()
    val discovering by vm.discovering.collectAsState()
    val external by vm.externalArtists.collectAsState()
    var clientId by remember { mutableStateOf(vm.clientId()) }
    var lastFmKey by remember { mutableStateOf(vm.lastFmApiKey()) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Header("Spotify + Hidden Discovery / V0.4") }
        item {
            Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().background(Card, RoundedCornerShape(22.dp)).padding(18.dp)) {
                Text("HIDDEN DISCOVERY ENGINE", color = Acid, fontWeight = FontWeight.Bold)
                Text("Last.fmの類似・listeners/playcountとMusicBrainzのMBID/国/活動期間を統合し、本当に隠れたMetalを探索する。", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = lastFmKey, onValueChange = { lastFmKey = it }, label = { Text("Last.fm API Key") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Button(onClick = { vm.saveLastFmApiKey(lastFmKey); vm.syncExternalDiscovery() }, enabled = !discovering, modifier = Modifier.fillMaxWidth()) {
                    Text(if (discovering) "外部を掘削中…" else "未知のMetalを発掘")
                }
                Spacer(Modifier.height(8.dp))
                Text(discoveryStatus, color = Muted, fontSize = 12.sp)
                Text("External cache: ${external.size} artists / max 500", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().background(Card, RoundedCornerShape(22.dp)).padding(18.dp)) {
                Text("SPOTIFY DNA SYNC", color = Acid, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = clientId, onValueChange = { clientId = it }, label = { Text("Spotify Client ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Button(onClick = { vm.saveClientId(clientId); vm.syncSpotify() }, enabled = !syncing, modifier = Modifier.fillMaxWidth()) {
                    Text(if (syncing) "解析中…" else "Spotifyと接続してDNA更新")
                }
                Spacer(Modifier.height(10.dp))
                Text(status, color = Muted, fontSize = 12.sp)
                signals.forEach { Text(it, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp)) }
            }
        }
    }
}

private fun openSpotify(context: android.content.Context, artistName: String) {
    val q = URLEncoder.encode(artistName, "UTF-8")
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/search/$q")))
}


private fun formatCompact(n: Long): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
    n >= 1_000 -> String.format("%.1fK", n / 1_000.0)
    else -> n.toString()
}
