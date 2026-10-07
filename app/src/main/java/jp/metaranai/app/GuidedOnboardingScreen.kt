package jp.metaranai.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SetupAcid = Color(0xFFD6FF36)
private val SetupMuted = Color(0xFFA4A4A4)

/** One decision at a time: account -> Spotify -> Last.fm -> first Metal DNA. */
@Composable
fun GuidedOnboardingScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity
    val account by vm.account.collectAsState()
    val accountBusy by vm.accountBusy.collectAsState()
    val accountStatus by vm.accountStatus.collectAsState()
    val cloudChecking by vm.cloudChecking.collectAsState()
    val conflict by vm.cloudConflictPending.collectAsState()
    val pending by vm.legacyMigrationPending.collectAsState()
    val cloudSyncing by vm.cloudSyncing.collectAsState()
    val connecting by vm.connecting.collectAsState()
    val connectionStatus by vm.connectionStatus.collectAsState()
    val spotifySyncing by vm.syncing.collectAsState()
    val spotifyStatus by vm.spotifyStatus.collectAsState()
    val spotifyPrepared by vm.onboardingSpotifyPrepared.collectAsState()
    val remoteResults by vm.remoteSearchResults.collectAsState()
    val remoteSearching by vm.remoteSearching.collectAsState()
    val remoteStatus by vm.remoteSearchStatus.collectAsState()
    val savedArtists by vm.externalArtists.collectAsState()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var accountForm by rememberSaveable { mutableStateOf(false) }
    var guestRequested by rememberSaveable { mutableStateOf(false) }
    var emailForm by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var createEmail by rememberSaveable { mutableStateOf(false) }
    var spotifyForm by rememberSaveable { mutableStateOf(false) }
    var clientId by rememberSaveable { mutableStateOf(vm.clientId()) }
    var lastFmForm by rememberSaveable { mutableStateOf(false) }
    var lastFmUsername by rememberSaveable { mutableStateOf(vm.lastFmUsername()) }
    var tasteMode by rememberSaveable { mutableIntStateOf(0) } // 0 choose, 1 favorite, 2 quiz, 3 result
    var query by rememberSaveable { mutableStateOf("") }
    var selectedName by rememberSaveable { mutableStateOf("") }
    var questionIndex by rememberSaveable { mutableIntStateOf(0) }
    var answerCodes by rememberSaveable { mutableStateOf("------") }
    val answers = answerCodes.map { it.digitToIntOrNull() ?: -1 }
    val busy = accountBusy || connecting || spotifySyncing || cloudChecking || cloudSyncing
    val selectedArtist = remember(selectedName, savedArtists) { vm.onboardingArtistByName(selectedName) }

    LaunchedEffect(account?.uid, accountBusy, guestRequested) {
        if (step == 0 && guestRequested && account?.isGuest == true && !accountBusy) {
            guestRequested = false
            step = 1
        }
    }
    LaunchedEffect(step, tasteMode) { vm.clearRemoteSearch() }
    val scrollState = rememberScrollState()
    LaunchedEffect(step, tasteMode, questionIndex) { scrollState.scrollTo(0) }

    Column(Modifier.fillMaxSize().background(Color(0xFF090909)).verticalScroll(scrollState)
        .imePadding().padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("メタらない？", color = SetupAcid, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("初期設定 ${step + 1} / 4", color = SetupMuted, fontSize = 12.sp)
        LinearProgressIndicator(progress = { (step + 1) / 4f }, modifier = Modifier.fillMaxWidth())
        when (step) {
            0 -> {
                SetupHeading("アカウントを使いますか？", "ログインすると、クラウドに保存した図鑑・評価・Metal DNAを引き継げます。アカウントなしでも音楽を発掘できます。")
                if (account != null && (!account!!.isGuest || !accountForm)) {
                    Text(if (account!!.isGuest) "アカウントなしで利用中" else "ログイン中: ${account!!.email.ifBlank { account!!.displayName }}", color = Color.White)
                    if (cloudChecking) Text("クラウドの記録を確認しています…", color = SetupMuted)
                    if (conflict) {
                        Text("端末とクラウドの両方に記録があります。引き継ぐ記録を選んでください。", color = SetupMuted)
                        SetupButton("クラウドの記録を復元", !busy, vm::resolveCloudConflictUseCloud)
                        SetupSecondary("端末の記録を引き継ぐ", !busy, vm::resolveCloudConflictUseLocal)
                    } else if (pending) {
                        SetupButton("この端末の記録をアカウントへ引き継ぐ", !busy, vm::migrateLocalDataToAccount)
                    }
                    SetupButton("次へ", !busy && !conflict && !pending) { step = 1 }
                    if (account!!.isGuest) SetupSecondary("Googleまたはメールのアカウントを使う", !busy) { accountForm = true }
                } else if (!accountForm) {
                    SetupButton("アカウントで始める") { accountForm = true }
                    SetupSecondary("アカウントなしで始める", !busy) { guestRequested = true; vm.signInGuest() }
                } else {
                    SetupButton("Googleで続ける", !busy && vm.googleConfigured && activity != null) { activity?.let(vm::signInGoogle) }
                    SetupSecondary("メールで登録・ログイン", !busy) { emailForm = !emailForm }
                    if (emailForm) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !createEmail, onClick = { createEmail = false }, label = { Text("ログイン") })
                            FilterChip(selected = createEmail, onClick = { createEmail = true }, label = { Text("新規登録") })
                        }
                        OutlinedTextField(email, { email = it }, label = { Text("メールアドレス") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(password, { password = it }, label = { Text("パスワード（6文字以上）") }, singleLine = true,
                            visualTransformation = PasswordVisualTransformation(), enabled = !busy, modifier = Modifier.fillMaxWidth())
                        SetupButton(if (createEmail) "確認メールを送信して登録" else "既存メールでログイン", !busy && email.isNotBlank() && password.length >= 6) {
                            vm.signInEmail(email, password, createEmail)
                        }
                        if (createEmail) Text("確認メールのリンクを開いたあと、ログインを選んでください。", color = SetupMuted, fontSize = 12.sp)
                    }
                    SetupSecondary("アカウントなしで始める", !busy) { guestRequested = true; vm.signInGuest() }
                }
                if (accountStatus.isNotBlank()) Text(accountStatus, color = SetupMuted, fontSize = 12.sp)
            }
            1 -> {
                SetupHeading("Spotifyを連携しますか？", "よく聴く音楽からMetal DNAを作れます。連携は任意で、あとから設定することもできます。")
                ConnectionStoryboard(lastFm = false)
                if (!spotifyForm) {
                    SetupButton("はい、Spotifyを連携する", !busy) { spotifyForm = true }
                } else {
                    Text("Spotify連携", color = SetupAcid, fontWeight = FontWeight.Bold)
                    if (BuildConfig.SPOTIFY_CLIENT_ID.isBlank()) {
                        Text("1. Spotify開発者Dashboardでアプリを作成\n2. Redirect URIに下のURLを登録して保存\n3. Client IDをコピーして貼り付け", color = SetupMuted, fontSize = 13.sp)
                        Text(SpotifySetup.REDIRECT_URI, color = Color.White, fontSize = 13.sp)
                        SpotifyRedirectCopyButton()
                        SetupSecondary("Spotify開発者Dashboardを開く") {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SpotifySetup.DASHBOARD_URL)))
                        }
                        Text("開発用アプリの所有者にはSpotify Premiumが必要です。Client Secretの入力は不要です。", color = SetupMuted, fontSize = 12.sp)
                    } else Text("アプリ標準のClient IDを利用できます。", color = SetupMuted)
                    OutlinedTextField(clientId, { clientId = it }, label = { Text("Spotify Client ID") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
                    SetupButton(if (connecting) "連携を確認中…" else "Spotifyにログインして連携", !busy && clientId.trim().matches(Regex("[a-fA-F0-9]{32}"))) { vm.connectSpotify(clientId) }
                    if (connectionStatus.startsWith("Spotify")) Text(connectionStatus, color = SetupMuted, fontSize = 12.sp)
                    if (vm.spotifyConnected() && clientId.trim() == vm.clientId()) {
                        SetupButton(if (spotifySyncing) "好みを解析中…" else "視聴傾向からMetal DNAを準備", !busy) { vm.startWithSpotifyOnboarding() }
                        if (spotifyStatus.isNotBlank()) Text(spotifyStatus, color = SetupMuted, fontSize = 12.sp)
                        SetupButton("次へ", !busy) { step = 2 }
                    }
                }
                SetupSecondary("スキップ", !busy) { vm.skipSpotifyOnboarding(); step = 2 }
            }
            2 -> {
                SetupHeading("Last.fmを連携しますか？", "記録済みの再生履歴を使って、長年の好みからMetalを発掘できます。使っていなければスキップでOKです。")
                ConnectionStoryboard(lastFm = true)
                if (!lastFmForm) {
                    SetupButton("はい、Last.fmを連携する", !busy && vm.lastFmConfigured()) { lastFmForm = true }
                    if (!vm.lastFmConfigured()) Text("このバージョンではLast.fm連携を利用できません。", color = SetupMuted, fontSize = 12.sp)
                } else {
                    SetupSecondary("Last.fmにログイン / 新規登録") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.last.fm/login"))) }
                    Text("プロフィールURLの /user/ の後ろの名前を入力します。パスワードは入力しません。", color = SetupMuted, fontSize = 12.sp)
                    OutlinedTextField(lastFmUsername, { lastFmUsername = it }, label = { Text("Last.fmユーザー名") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
                    SetupButton(if (connecting) "ユーザー名を確認中…" else "Last.fmを連携する", !busy && lastFmUsername.isNotBlank()) { vm.verifyLastFmUsername(lastFmUsername) }
                    if (connectionStatus.startsWith("Last.fm")) Text(connectionStatus, color = SetupMuted, fontSize = 12.sp)
                    if (vm.lastFmUsernameVerified() && lastFmUsername.trim().equals(vm.lastFmUsername(), true)) {
                        Text("履歴の解析・発掘は初期設定後に「探す」で実行できます。", color = SetupMuted, fontSize = 12.sp)
                        SetupButton("次へ", !busy) { step = 3 }
                    }
                }
                Text("Last.fm未登録でも発掘機能は利用できます。", color = SetupMuted, fontSize = 12.sp)
                SetupSecondary("スキップ", !busy) { step = 3 }
            }
            3 -> {
                if (spotifyPrepared && tasteMode == 0) {
                    SetupHeading("Metal DNAの準備ができました", "Spotifyの視聴傾向をもとに、あなたの好みから発掘を始められます。")
                    Text(spotifyStatus, color = SetupMuted, fontSize = 13.sp)
                    SetupButton("この好みで始める", !busy, vm::completeOnboardingFromSpotify)
                    SetupSecondary("バンド選択や診断で始める", !busy) { vm.skipSpotifyOnboarding() }
                } else when (tasteMode) {
                    0 -> {
                        SetupHeading("最初の好みを教えてください", "好きなバンドがあれば1組選択。まだメタルを知らない方は、音の好みを6問答えるだけで最初のジャンルが見つかります。")
                        SetupButton("好きなメタルアーティストを検索") { tasteMode = 1 }
                        SetupSecondary("メタルを知らない・診断してみる") { tasteMode = 2 }
                    }
                    1 -> {
                        SetupHeading("好きなアーティストを1組選ぶ", "このアーティストの音を出発点にして、Metal DNAを育てます。")
                        OutlinedTextField(query, { query = it; selectedName = ""; vm.clearRemoteSearch() }, label = { Text("アーティスト名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        SetupSecondary(if (remoteSearching) "外部検索中…" else "Last.fm / MusicBrainzでも検索", !remoteSearching && query.trim().length >= 2 && vm.lastFmConfigured()) { vm.searchExternal(query) }
                        val local = remember(query, savedArtists) { vm.searchOnboardingArtists(query) }
                        val results = (remoteResults + local).distinctBy { it.name.trim().lowercase() }.take(20)
                        results.forEach { artist ->
                            OutlinedButton(onClick = { selectedName = artist.name }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(if (selectedName == artist.name) "✓ ${artist.name}" else artist.name, color = if (selectedName == artist.name) SetupAcid else Color.White)
                                    Text("${artist.country} · ${artist.genres.joinToString(" / ")}", color = SetupMuted, fontSize = 11.sp)
                                }
                            }
                        }
                        if (query.isNotBlank() && results.isEmpty() && !remoteSearching) Text("端末内に候補がありません。外部検索か診断を試してください。", color = SetupMuted, fontSize = 12.sp)
                        if (remoteStatus.isNotBlank()) Text(remoteStatus, color = SetupMuted, fontSize = 12.sp)
                        SetupButton("この1組で始める", selectedArtist != null && !remoteSearching) { selectedArtist?.let(vm::completeOnboardingWithArtist) }
                        SetupSecondary("診断から選ぶ", !remoteSearching) { tasteMode = 2 }
                    }
                    2 -> {
                        val question = MetalTasteQuiz.questions[questionIndex]
                        SetupHeading("音楽の好み診断", "質問 ${questionIndex + 1} / ${MetalTasteQuiz.questions.size} · 直感で選んでください")
                        Text(question.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        question.choices.forEachIndexed { index, choice ->
                            OutlinedButton(onClick = { answerCodes = answerCodes.replaceRange(questionIndex, questionIndex + 1, index.toString()) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 84.dp)) {
                                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(if (answers[questionIndex] == index) "✓ ${choice.title}" else choice.title, color = if (answers[questionIndex] == index) SetupAcid else Color.White, fontSize = 16.sp)
                                    Text(choice.detail, color = SetupMuted, fontSize = 12.sp)
                                }
                            }
                        }
                        SetupButton(if (questionIndex == MetalTasteQuiz.questions.lastIndex) "診断結果を見る" else "次の質問へ", answers[questionIndex] >= 0) {
                            if (questionIndex == MetalTasteQuiz.questions.lastIndex) tasteMode = 3 else ++questionIndex
                        }
                        if (questionIndex > 0) SetupSecondary("前の質問へ") { --questionIndex }
                        SetupSecondary("バンド検索に切り替える") { tasteMode = 1 }
                    }
                    3 -> {
                        val result = remember(answerCodes) { MetalTasteQuiz.result(answers) }
                        SetupHeading("あなたのファーストジャンル", GenreLensCatalog.displayName(result.genre))
                        Text("音の好みタイプ: ${result.code}", color = SetupAcid, fontWeight = FontWeight.Bold)
                        Text(result.description, color = SetupMuted, fontSize = 14.sp)
                        Text("まず聴いてみたい3組", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        result.artists.forEachIndexed { index, artist ->
                            Surface(color = Color(0xFF181818), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${index + 1}. ${artist.name}", color = SetupAcid, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    Text(artist.reason, color = Color.White, fontSize = 13.sp)
                                    Text("${artist.country} · ${GenreLensCatalog.displayName(result.genre)}", color = SetupMuted, fontSize = 11.sp)
                                }
                            }
                        }
                        Text("開始するとこの3組を図鑑に追加し、最初のおすすめ対象をこのジャンルに設定します。ジャンルはあとから変更できます。", color = SetupMuted, fontSize = 12.sp)
                        SetupButton("このジャンルと3組で始める", !busy) { vm.completeOnboardingWithDiagnosis(result) }
                        SetupSecondary("診断をやり直す") { questionIndex = 0; tasteMode = 2 }
                    }
                }
            }
        }
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (step > 0) TextButton(onClick = { --step }, enabled = !busy && !remoteSearching, modifier = Modifier.fillMaxWidth()) { Text("前の設定へ戻る") }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SetupHeading(title: String, detail: String) {
    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
    Text(detail, color = SetupMuted, fontSize = 14.sp)
}

@Composable
private fun SetupButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(label, fontSize = 15.sp) }
}

@Composable
private fun SetupSecondary(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(label, fontSize = 14.sp) }
}
