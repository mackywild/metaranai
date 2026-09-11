import SwiftUI
import UniformTypeIdentifiers

private enum MetalTheme {
    static let background = Color(red: 0.035, green: 0.035, blue: 0.045)
    static let card = Color(red: 0.085, green: 0.085, blue: 0.105)
    static let raised = Color(red: 0.12, green: 0.12, blue: 0.145)
    static let accent = Color(red: 0.92, green: 0.12, blue: 0.16)
    static let acid = Color(red: 0.35, green: 0.95, blue: 0.58)
    static let muted = Color.white.opacity(0.62)
}

struct ContentView: View {
    @EnvironmentObject private var state: MetaranaiAppState

    var body: some View {
        TabView {
            TodayView()
                .tabItem { Label("今日", systemImage: "bolt.fill") }
            SearchView()
                .tabItem { Label("探す", systemImage: "magnifyingglass") }
            ArchiveView()
                .tabItem { Label("図鑑", systemImage: "books.vertical.fill") }
            DNAView()
                .tabItem { Label("DNA", systemImage: "waveform.path.ecg") }
            SettingsView()
                .tabItem { Label("設定", systemImage: "gearshape.fill") }
        }
        .tint(MetalTheme.accent)
        .preferredColorScheme(.dark)
    }
}

private struct TodayView: View {
    @EnvironmentObject private var state: MetaranaiAppState
    @State private var showWhy = false
    @State private var showDeepDive = false

    var body: some View {
        NavigationStack {
            ZStack {
                MetalTheme.background.ignoresSafeArea()
                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        BrandHeader(subtitle: "v0.9.0 · ACCOUNT & PERSONALIZATION")
                        if !state.statusMessage.isEmpty { StatusBanner(text: state.statusMessage, busy: state.isBusy) }
                        LensStrip()
                        if let rec = state.recommendation {
                            RecommendationCard(rec: rec, showWhy: $showWhy)
                            ListenActions(artist: rec.artist)
                            RatingGrid(artist: rec.artist)
                        } else {
                            EmptyRecommendationCard()
                        }
                        if state.activeGenres.isEmpty == false {
                            let count = state.unratedArtists.filter { GenreLensCore.matches($0, names: state.activeGenres) }.count
                            Text("\(state.activeGenres.joined(separator: " / ")) · 未評価 \(count)組")
                                .font(.caption).foregroundStyle(MetalTheme.muted)
                        }
                    }
                    .padding(18)
                }
                .refreshable {
                    await state.ensureGenrePool()
                    state.recomputeRecommendation(seed: Int.random(in: 1...999_999))
                }
            }
            .toolbarBackground(MetalTheme.background, for: .navigationBar)
            .task(id: state.activeGenres.joined(separator: "|")) { await state.ensureGenrePool() }
        }
    }
}

private struct BrandHeader: View {
    let subtitle: String
    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            Image("AppIconPreview")
                .resizable().scaledToFit().frame(width: 52, height: 52)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(.white.opacity(0.08)))
            VStack(alignment: .leading, spacing: 2) {
                Text("メタらない？").font(.title.bold()).foregroundStyle(.white)
                Text(subtitle).font(.caption.bold()).foregroundStyle(MetalTheme.acid)
            }
            Spacer()
        }
    }
}

private struct LensStrip: View {
    @EnvironmentObject private var state: MetaranaiAppState
    var body: some View {
        VStack(alignment: .leading, spacing: 9) {
            HStack {
                Text("GENRE LENS").font(.caption.bold()).foregroundStyle(MetalTheme.muted)
                Spacer()
                if !state.activeGenres.isEmpty {
                    Button("OFF") { state.setLensMode(.off) }
                        .font(.caption.bold()).foregroundStyle(MetalTheme.accent)
                }
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(GenreLensCore.names, id: \.self) { genre in
                        let selected = state.genreLens.mode == .manual && state.genreLens.manualGenres.contains(genre)
                        Button {
                            state.toggleManualGenre(genre)
                        } label: {
                            Text(genre)
                                .font(.caption.bold())
                                .padding(.horizontal, 11).padding(.vertical, 8)
                                .background(selected ? MetalTheme.accent : MetalTheme.raised)
                                .foregroundStyle(selected ? .white : MetalTheme.muted)
                                .clipShape(Capsule())
                        }
                    }
                }
            }
        }
    }
}

private struct RecommendationCard: View {
    let rec: Recommendation
    @Binding var showWhy: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("TODAY'S METAL").font(.caption.bold()).foregroundStyle(MetalTheme.acid)
                Spacer()
                Text("DNA \(rec.compatibility)%")
                    .font(.headline.bold()).foregroundStyle(MetalTheme.acid)
            }
            Text(rec.artist.name).font(.title2.bold()).foregroundStyle(.white)
            Text([rec.artist.country, rec.artist.genres.prefix(3).joined(separator: " / ")].filter { !$0.isEmpty }.joined(separator: " · "))
                .font(.subheadline).foregroundStyle(MetalTheme.muted)
            HStack(spacing: 12) {
                ScorePill(title: "HIDDEN", value: rec.artist.hiddenScore)
                ScorePill(title: "TOTAL", value: rec.breakdown.total)
                Text(rec.artist.vocalType.label).font(.caption.bold()).foregroundStyle(MetalTheme.muted)
            }
            DisclosureGroup(isExpanded: $showWhy) {
                VStack(alignment: .leading, spacing: 8) {
                    Text(rec.reason).font(.subheadline).foregroundStyle(MetalTheme.muted)
                    Text("Affinity \(rec.breakdown.affinity) · Lens \(rec.breakdown.genreLens) · Hidden \(rec.breakdown.hidden) · Novelty \(rec.breakdown.novelty)")
                        .font(.caption.monospacedDigit()).foregroundStyle(MetalTheme.muted)
                    Text("MATCH: \(rec.matchedTraits.joined(separator: " / "))")
                        .font(.caption.bold()).foregroundStyle(MetalTheme.acid)
                }.padding(.top, 8)
            } label: {
                Text("なぜこのArtist？ / SCORE BREAKDOWN").font(.subheadline.bold())
            }
            .tint(.white)
        }
        .padding(18)
        .background(
            LinearGradient(colors: [MetalTheme.card, Color(red: 0.15, green: 0.035, blue: 0.055)], startPoint: .topLeading, endPoint: .bottomTrailing)
        )
        .clipShape(RoundedRectangle(cornerRadius: 22))
        .overlay(RoundedRectangle(cornerRadius: 22).stroke(MetalTheme.accent.opacity(0.35)))
    }
}

private struct EmptyRecommendationCard: View {
    @EnvironmentObject private var state: MetaranaiAppState
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("TODAY'S METAL").font(.caption.bold()).foregroundStyle(MetalTheme.acid)
            Text(state.activeGenres.isEmpty ? "Local Metal DBに候補がありません" : "指定Genreの未評価候補を探索します")
                .font(.headline).foregroundStyle(.white)
            Button("地下を探索") { Task { await state.ensureGenrePool(minimumUnrated: 10, target: 20) } }
                .buttonStyle(.borderedProminent).tint(MetalTheme.accent)
        }
        .padding(18).frame(maxWidth: .infinity, alignment: .leading)
        .background(MetalTheme.card).clipShape(RoundedRectangle(cornerRadius: 22))
    }
}

private struct ScorePill: View {
    let title: String
    let value: Int
    var body: some View {
        HStack(spacing: 4) {
            Text(title).font(.caption2.bold())
            Text("\(value)").font(.caption.bold().monospacedDigit())
        }
        .padding(.horizontal, 9).padding(.vertical, 6)
        .background(MetalTheme.raised).clipShape(Capsule()).foregroundStyle(.white)
    }
}

private struct ListenActions: View {
    @EnvironmentObject private var state: MetaranaiAppState
    let artist: MetalArtist
    @State private var showDeepDive = false

    var body: some View {
        VStack(spacing: 9) {
            HStack(spacing: 9) {
                ActionButton(title: "Spotify", systemImage: "play.circle.fill") {
                    Task {
                        let destination = await state.spotifyDestination(for: artist)
                        await MainActor.run { UIApplication.shared.open(destination.url) }
                    }
                }
                ActionButton(title: "YouTube", systemImage: "video.fill") {
                    if let url = state.youtubeURL(for: artist) { UIApplication.shared.open(url) }
                }
            }
            HStack(spacing: 9) {
                ActionButton(title: "MV", systemImage: "music.note.tv") {
                    if let url = state.youtubeURL(for: artist, suffix: "official music video") { UIApplication.shared.open(url) }
                }
                ActionButton(title: "Deep Dive", systemImage: "pickaxe") {
                    showDeepDive = true
                    Task { await state.deepDive(from: artist) }
                }
            }
        }
        .sheet(isPresented: $showDeepDive) {
            NavigationStack { DeepDiveView(seed: artist) }
                .presentationDetents([.medium, .large])
        }
    }
}

private struct ActionButton: View {
    let title: String
    let systemImage: String
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Label(title, systemImage: systemImage)
                .font(.subheadline.bold()).frame(maxWidth: .infinity).padding(.vertical, 11)
        }
        .buttonStyle(.plain).background(MetalTheme.raised).clipShape(RoundedRectangle(cornerRadius: 13)).foregroundStyle(.white)
    }
}

private struct RatingGrid: View {
    @EnvironmentObject private var state: MetaranaiAppState
    let artist: MetalArtist
    private let columns = [GridItem(.flexible()), GridItem(.flexible())]
    var body: some View {
        VStack(alignment: .leading, spacing: 9) {
            Text("このArtistどうだった？").font(.caption.bold()).foregroundStyle(MetalTheme.muted)
            LazyVGrid(columns: columns, spacing: 9) {
                ForEach(Reaction.allCases, id: \.rawValue) { reaction in
                    Button(reaction.label) { state.rate(artist, reaction: reaction) }
                        .font(.subheadline.bold()).frame(maxWidth: .infinity).padding(.vertical, 12)
                        .background(MetalTheme.card).clipShape(RoundedRectangle(cornerRadius: 13)).foregroundStyle(.white)
                }
            }
        }
    }
}

private struct SearchView: View {
    @EnvironmentObject private var state: MetaranaiAppState
    @State private var query = ""

    var body: some View {
        NavigationStack {
            ZStack {
                MetalTheme.background.ignoresSafeArea()
                VStack(spacing: 12) {
                    HStack {
                        TextField("Artist / Genre / Country", text: $query)
                            .textInputAutocapitalization(.never).autocorrectionDisabled()
                            .padding(12).background(MetalTheme.card).clipShape(RoundedRectangle(cornerRadius: 13))
                            .onSubmit { Task { await state.search(query) } }
                        Button { Task { await state.search(query) } } label: { Image(systemName: "magnifyingglass").font(.title3.bold()) }
                            .buttonStyle(.borderedProminent).tint(MetalTheme.accent)
                    }.padding(.horizontal, 16).padding(.top, 12)
                    if state.isBusy { ProgressView().tint(MetalTheme.acid) }
                    List(state.searchResults) { artist in
                        NavigationLink { ArtistDetailView(artist: artist) } label: { ArtistRow(artist: artist) }
                            .listRowBackground(MetalTheme.card)
                    }
                    .scrollContentBackground(.hidden)
                }
            }
            .navigationTitle("世界検索")
        }
    }
}

private struct ArchiveView: View {
    @EnvironmentObject private var state: MetaranaiAppState
    @State private var query = ""
    @State private var genre: String? = nil
    @State private var reaction: Reaction? = nil
    @State private var onlyUnrated = false
    @State private var vocal: VocalType? = nil
    @State private var sort: MetaranaiAppState.ArchiveSort = .recommended

    var body: some View {
        NavigationStack {
            ZStack {
                MetalTheme.background.ignoresSafeArea()
                VStack(spacing: 8) {
                    HStack {
                        Text("\(state.artists.count) ARTISTS").font(.caption.bold()).foregroundStyle(MetalTheme.acid)
                        Spacer()
                        Picker("Sort", selection: $sort) {
                            ForEach(MetaranaiAppState.ArchiveSort.allCases) { Text($0.rawValue).tag($0) }
                        }.pickerStyle(.menu)
                    }.padding(.horizontal, 16)
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            FilterChip(label: "未評価", active: onlyUnrated) { onlyUnrated.toggle(); if onlyUnrated { reaction = nil } }
                            Menu {
                                Button("すべて") { genre = nil }
                                ForEach(GenreLensCore.names, id: \.self) { item in Button(item) { genre = item } }
                            } label: { FilterChipLabel(label: genre ?? "Genre", active: genre != nil) }
                            Menu {
                                Button("すべて") { reaction = nil }
                                ForEach(Reaction.allCases, id: \.rawValue) { item in Button(item.label) { reaction = item; onlyUnrated = false } }
                            } label: { FilterChipLabel(label: reaction?.label ?? "評価", active: reaction != nil) }
                            Menu {
                                Button("すべて") { vocal = nil }
                                ForEach(VocalType.allCases, id: \.rawValue) { item in Button(item.label) { vocal = item } }
                            } label: { FilterChipLabel(label: vocal?.label ?? "Vo", active: vocal != nil) }
                        }.padding(.horizontal, 16)
                    }
                    let rows = state.filteredArchive(query: query, genre: genre, filterReaction: reaction, onlyUnrated: onlyUnrated, vocal: vocal, sort: sort)
                    List(rows) { artist in
                        NavigationLink { ArtistDetailView(artist: artist) } label: { ArtistRow(artist: artist) }
                            .listRowBackground(MetalTheme.card)
                    }.scrollContentBackground(.hidden)
                }
            }
            .navigationTitle("METAL ARCHIVE")
            .searchable(text: $query, prompt: "Artist / Country")
        }
    }
}

private struct FilterChip: View {
    let label: String; let active: Bool; let action: () -> Void
    var body: some View { Button(action: action) { FilterChipLabel(label: label, active: active) } }
}
private struct FilterChipLabel: View {
    let label: String; let active: Bool
    var body: some View {
        Text(label).font(.caption.bold()).padding(.horizontal, 11).padding(.vertical, 8)
            .background(active ? MetalTheme.accent : MetalTheme.raised).foregroundStyle(.white).clipShape(Capsule())
    }
}

private struct ArtistRow: View {
    @EnvironmentObject private var state: MetaranaiAppState
    let artist: MetalArtist
    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            HStack {
                Text(artist.name).font(.headline).foregroundStyle(.white).lineLimit(1)
                Spacer()
                if let reaction = state.reaction(for: artist) { Text(reaction.label.prefix(2)).font(.caption) }
                else { Text("未評価").font(.caption2.bold()).foregroundStyle(MetalTheme.acid) }
            }
            Text("\(artist.genres.prefix(2).joined(separator: " / ")) · \(artist.country)")
                .font(.caption).foregroundStyle(MetalTheme.muted).lineLimit(1)
            HStack {
                Text("DNA \(Int((state.profile.similarity(to: artist.vector) * 100).rounded()))")
                Text("HIDDEN \(artist.hiddenScore)")
                Text(artist.vocalType.label)
            }.font(.caption2.monospacedDigit()).foregroundStyle(MetalTheme.muted)
        }.padding(.vertical, 5)
    }
}

private struct ArtistDetailView: View {
    @EnvironmentObject private var state: MetaranaiAppState
    let artist: MetalArtist
    var body: some View {
        ZStack {
            MetalTheme.background.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(artist.name).font(.largeTitle.bold())
                    Text("\(artist.country) · \(artist.genres.joined(separator: " / "))").foregroundStyle(MetalTheme.muted)
                    HStack { ScorePill(title: "DNA", value: Int((state.profile.similarity(to: artist.vector) * 100).rounded())); ScorePill(title: "HIDDEN", value: artist.hiddenScore) }
                    Text(artist.reason).foregroundStyle(MetalTheme.muted)
                    ListenActions(artist: artist)
                    RatingGrid(artist: artist)
                }.padding(18)
            }
        }
    }
}

private struct DeepDiveView: View {
    @EnvironmentObject private var state: MetaranaiAppState
    let seed: MetalArtist
    var body: some View {
        ZStack {
            MetalTheme.background.ignoresSafeArea()
            List(state.deepDiveResults) { artist in
                NavigationLink { ArtistDetailView(artist: artist) } label: { ArtistRow(artist: artist) }
                    .listRowBackground(MetalTheme.card)
            }.scrollContentBackground(.hidden)
        }
        .navigationTitle("⛏ \(seed.name)")
        .overlay { if state.isBusy && state.deepDiveResults.isEmpty { ProgressView("地下探索中…").tint(MetalTheme.acid) } }
    }
}

private struct DNAView: View {
    @EnvironmentObject private var state: MetaranaiAppState
    var body: some View {
        NavigationStack {
            ZStack {
                MetalTheme.background.ignoresSafeArea()
                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        Text(state.dnaType).font(.title2.bold()).foregroundStyle(.white)
                        HStack(spacing: 10) {
                            StatCard(title: "評価", value: "\(state.history.count)")
                            StatCard(title: "好評価率", value: "\(state.positiveRate)%")
                            StatCard(title: "平均刺さり", value: "\(state.averageAffinity)")
                        }
                        VStack(spacing: 12) {
                            ForEach(Array(state.profile.traits.enumerated()), id: \.offset) { _, item in
                                DNAProgress(label: item.0, value: item.1)
                            }
                        }.padding(16).background(MetalTheme.card).clipShape(RoundedRectangle(cornerRadius: 18))
                        Text("VOCAL DNA").font(.caption.bold()).foregroundStyle(MetalTheme.acid)
                        HStack { StatCard(title: "男性", value: "\(Int(state.vocalProfile.male * 100))%"); StatCard(title: "女性", value: "\(Int(state.vocalProfile.female * 100))%"); StatCard(title: "混成", value: "\(Int(state.vocalProfile.mixed * 100))%") }
                    }.padding(18)
                }
            }.navigationTitle("METAL DNA")
        }
    }
}

private struct DNAProgress: View {
    let label: String; let value: Double
    var body: some View {
        VStack(spacing: 5) {
            HStack { Text(label).font(.subheadline.bold()); Spacer(); Text("\(Int(value * 100))").font(.caption.monospacedDigit()).foregroundStyle(MetalTheme.muted) }
            ProgressView(value: value).tint(MetalTheme.accent)
        }
    }
}

private struct StatCard: View {
    let title: String; let value: String
    var body: some View {
        VStack(spacing: 4) { Text(value).font(.headline.bold()).foregroundStyle(.white); Text(title).font(.caption2).foregroundStyle(MetalTheme.muted) }
            .frame(maxWidth: .infinity).padding(.vertical, 12).background(MetalTheme.card).clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

private struct SettingsView: View {
    @State private var importing = false
    @State private var exporting = false
    @State private var exportDocument = BackupDocument()
    @State private var alertText: String? = nil

    var body: some View {
        SettingsBridge(importing: $importing, exporting: $exporting, exportDocument: $exportDocument, alertText: $alertText)
    }
}

private struct SettingsBridge: View {
    @EnvironmentObject private var state: MetaranaiAppState
    @Binding var importing: Bool
    @Binding var exporting: Bool
    @Binding var exportDocument: BackupDocument
    @Binding var alertText: String?

    var body: some View {
        NavigationStack {
            ZStack {
                MetalTheme.background.ignoresSafeArea()
                Form {
                    Section("DATA SAFETY") {
                        LabeledContent("Local Metal DB", value: "\(state.artists.count) Artists")
                        LabeledContent("評価履歴", value: "\(state.history.count)件")
                        Text("Android V0.5〜V0.7の metaranai-backup JSONをそのまま復元できます。iOSから書き出したJSONもAndroid互換です。").font(.caption)
                        Button("JSONバックアップを復元") { importing = true }
                        Button("JSONバックアップを書き出す") {
                            do { exportDocument = BackupDocument(data: try state.exportBackup()); exporting = true }
                            catch { alertText = error.localizedDescription }
                        }
                    }
                    SpotifySettingsSection(auth: state.spotifyAuth, alertText: $alertText)
                    Section("Last.fm") {
                        SecureField("API Key", text: $state.lastFmAPIKey).textInputAutocapitalization(.never).autocorrectionDisabled()
                        Button("API設定を保存") { state.saveCredentials() }
                    }
                    Section("iOS BETA") {
                        LabeledContent("Version", value: "0.9.0 (17)")
                        LabeledContent("Bundle ID", value: "jp.metaranai.ios")
                        Text("TestFlightに上げる前にdocs/14_TESTFLIGHT_BETA.mdの手順を実行してください。").font(.caption)
                    }
                }
                .scrollContentBackground(.hidden)
            }
            .navigationTitle("設定")
            .fileImporter(isPresented: $importing, allowedContentTypes: [.json, .plainText]) { result in
                do {
                    let url = try result.get()
                    let accessing = url.startAccessingSecurityScopedResource()
                    defer { if accessing { url.stopAccessingSecurityScopedResource() } }
                    let data = try Data(contentsOf: url)
                    let summary = try state.importBackup(data)
                    alertText = "復元完了：Backup v\(summary.version) / 評価\(summary.historyCount)件 / External \(summary.externalArtistCount)組"
                } catch { alertText = error.localizedDescription }
            }
            .fileExporter(isPresented: $exporting, document: exportDocument, contentType: .json, defaultFilename: "metaranai-backup-v0.9.0") { result in
                if case .failure(let error) = result { alertText = error.localizedDescription }
            }
            .alert("メタらない？", isPresented: Binding(get: { alertText != nil }, set: { if !$0 { alertText = nil } })) {
                Button("OK", role: .cancel) { alertText = nil }
            } message: { Text(alertText ?? "") }
        }
    }
}


private struct SpotifySettingsSection: View {
    @EnvironmentObject private var state: MetaranaiAppState
    @ObservedObject var auth: SpotifyAuthManager
    @Binding var alertText: String?

    var body: some View {
        Section("Spotify") {
            TextField("Client ID", text: $state.clientID).textInputAutocapitalization(.never).autocorrectionDisabled()
            LabeledContent("状態", value: auth.status)
            if auth.isAuthenticated {
                Button("Spotify接続を解除", role: .destructive) { auth.disconnect() }
            } else {
                Button("Spotifyへ接続 (PKCE)") {
                    state.saveCredentials()
                    Task {
                        do { try await auth.login(clientID: state.clientID); state.statusMessage = "Spotify接続完了" }
                        catch { alertText = error.localizedDescription }
                    }
                }
            }
            Text("Redirect URI: \(SpotifyAuthManager.redirectURI)").font(.caption2).textSelection(.enabled)
        }
    }
}

private struct StatusBanner: View {
    let text: String; let busy: Bool
    var body: some View {
        HStack(spacing: 9) {
            if busy { ProgressView().controlSize(.small).tint(MetalTheme.acid) }
            Text(text).font(.caption).foregroundStyle(MetalTheme.muted)
            Spacer()
        }.padding(11).background(MetalTheme.card).clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
