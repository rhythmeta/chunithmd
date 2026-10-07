import Shared
import SwiftUI

struct RandomSongView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var songs: [CatalogSongViewData] = []
    @State private var pending: [CatalogSongViewData] = []
    @State private var reels: [[CatalogSongViewData]] = [[], [], [], []]
    @State private var offsets = [0.0, 0, 0, 0]
    @State private var count: Int32 = 3
    @State private var settings = CatalogFilterState()
    @State private var showFilters = false
    @State private var spinID: UUID?
    @State private var emptyResult = false
    @State private var feedback = 0

    private var spinning: Bool { spinID != nil }
    private var slotHeight: CGFloat { count == 3 ? 120 : 84 }

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                VStack(spacing: 24) {
                    Picker(tr("曲目数"), selection: $count) {
                        Text(tr("一次 3 首")).tag(Int32(3))
                        Text(tr("一次 4 首")).tag(Int32(4))
                    }.pickerStyle(.segmented).padding(.horizontal, 40).padding(.top, 16)
                    HStack(spacing: 8) {
                        ForEach(0..<Int(count), id: \.self) { index in
                            RandomSlotView(songs: reels[index], offset: offsets[index], slotHeight: slotHeight,
                                           jacketSize: count == 3 ? 85 : 70, cornerRadius: count == 3 ? 20 : 14, spinning: spinning)
                        }
                    }
                    .allowsHitTesting(false)
                    .frame(height: slotHeight).padding(16)
                    .background(AppTheme.surface, in: .rect(cornerRadius: 24)).padding(.horizontal, 16)
                    Button(action: draw) {
                        Label(spinning ? tr("直接跳过") : tr("立刻随机抽取"), systemImage: spinning ? "forward.end.fill" : "dice.fill")
                            .font(.headline.bold()).frame(maxWidth: .infinity).padding(.vertical, 8)
                    }.buttonStyle(.borderedProminent).disabled(catalog.allSongs.isEmpty)
                        .padding(.horizontal, 40).accessibilityIdentifier("random-draw")
                }.padding(.bottom, 24).background(AppTheme.page)
                if !spinning && !songs.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(tr("抽选结果")).font(.system(size: 13, weight: .bold)).foregroundStyle(.secondary)
                            .padding(.horizontal, 24).padding(.bottom, 8)
                        // Each draw position has its own identity, including repeated songs.
                        ForEach(songs.indices, id: \.self) { index in
                            SongRow(song: songs[index], card: true, showsProgress: true, scrollsText: true)
                                .padding(.horizontal, 16)
                                .transition(.asymmetric(insertion: .move(edge: .bottom).combined(with: .opacity), removal: .opacity))
                        }
                    }.padding(.top, 8).padding(.bottom, 40)
                        .transition(.opacity)
                } else if emptyResult {
                    ContentUnavailableView(tr("没有符合条件的谱面"), systemImage: "line.3.horizontal.decrease.circle")
                }
            }
        }
        .accessibilityIdentifier("random-page")
        .background(AppTheme.page).navigationTitle(tr("随机歌曲"))
        .toolbar {
            Button(tr("筛选"), systemImage: "line.3.horizontal.decrease.circle") { showFilters = true }
                .tint(settings.isActive ? .blue : .primary).accessibilityIdentifier("random-filter")
        }
        .sheet(isPresented: $showFilters) {
            CatalogFilterSheet(settings: $settings, categories: catalog.categories, versions: catalog.versions,
                               difficulties: catalog.difficulties)
        }
        .sensoryFeedback(.impact(weight: .medium), trigger: feedback)
        .task(id: spinID) { await animateReels() }
        .onChange(of: count) { reset() }
        .onChange(of: settings) { reset() }
        .onChange(of: personal.revision) { reset() }
        .onChange(of: reduceMotion) { if reduceMotion && spinning { finish() } }
        .onDisappear { if spinning { finish(haptic: false) } }
    }

    private func draw() {
        if spinning { finish(); return }
        guard let bundle = catalog.bundle else { return }
        do {
            let filterJSON = String(decoding: try JSONEncoder().encode(settings), as: UTF8.self)
            let json = try NativeCatalogQuery.shared.randomSongs(bundle: bundle, filterJson: filterJSON,
                server: catalog.server, favorites: Array(catalog.favorites), count: count)
            let drawn = try JSONDecoder().decode([CatalogSongViewData].self, from: Data(json.utf8))
            reset()
            emptyResult = drawn.isEmpty
            guard !drawn.isEmpty else { return }
            pending = drawn
            withoutAnimation {
                for index in drawn.indices {
                    // Filler covers are presentation only; shared code selects the actual results.
                    reels[index] = (0..<20).compactMap { _ in catalog.allSongs.randomElement() } + [drawn[index]]
                }
            }
            if reduceMotion { finish() } else { spinID = UUID() }
        } catch { personal.error = error.localizedDescription }
    }

    private func animateReels() async {
        guard let id = spinID else { return }
        do {
            // Render the first frame before advancing each continuous reel.
            try await Task.sleep(for: .milliseconds(30))
            guard spinID == id else { return }
            for index in 0..<Int(count) {
                withAnimation(.timingCurve(0.4, 0, 0.2, 1, duration: 2 + Double(index) * 0.4)) {
                    offsets[index] = -Double(reels[index].count - 1) * slotHeight
                }
            }
            try await Task.sleep(for: .seconds(2 + Double(count - 1) * 0.4))
            guard spinID == id, !Task.isCancelled else { return }
            finish()
        } catch { /* A new draw, count change, or leaving the page cancels this animation. */ }
    }

    private func finish(haptic: Bool = true) {
        withAnimation(reduceMotion ? nil : .spring(response: 0.4, dampingFraction: 0.8)) {
            songs = pending
            spinID = nil
        }
        if haptic { feedback += 1 }
    }

    private func reset() {
        withoutAnimation {
            spinID = nil
            songs = []
            pending = []
            reels = [[], [], [], []]
            offsets = [0, 0, 0, 0]
            emptyResult = false
        }
    }

    private func withoutAnimation(_ changes: () -> Void) {
        var transaction = Transaction()
        transaction.disablesAnimations = true
        withTransaction(transaction, changes)
    }
}
