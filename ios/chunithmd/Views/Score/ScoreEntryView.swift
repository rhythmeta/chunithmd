import Shared
import SwiftUI

struct ScoreEntryView: View {
    let song: CatalogSongViewData
    let sheet: CatalogSongViewData.Sheet
    private let initialScore: Int?
    private let initialClear: String
    private let profileID: String?
    private let region: String?
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var scoreText = ""
    @State private var clear = "clear"
    @State private var combo = ""
    @State private var chain = ""
    @State private var error: String?
    @State private var saved = false
    @State private var loaded = false
    @State private var ownerID: String?
    @State private var ownerRegion: String?
    @FocusState private var scoreFocused: Bool

    init(song: CatalogSongViewData, sheet: CatalogSongViewData.Sheet, initialScore: Int? = nil,
         initialClear: String = "clear", profileID: String? = nil, region: String? = nil) {
        self.song = song; self.sheet = sheet; self.initialScore = initialScore
        self.initialClear = initialClear; self.profileID = profileID; self.region = region
    }

    private var parsedScore: Int32? { ChunithmScoreRules.shared.parseEntryScore(raw: scoreText)?.int32Value }
    private var rank: String { parsedScore.map { ChunithmScoreRules.shared.rank(score: $0) } ?? "—" }
    private var validation: String? {
        scoreText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || parsedScore != nil
            ? nil : tr("请输入 0–1,010,000 之间的整数分数。")
    }
    private var history: [ScoreRecord] { personal.history(songID: song.id, sheetID: sheet.id, sort: .score) }
    private var best: BestScoreSummary? { personal.bridge.bestHistorySummary(records: history) }
    private var tint: AnyShapeStyle { difficultyStyle(sheet.difficulty, type: sheet.type) }
    private var buttonStyle: AnyShapeStyle {
        saved ? AnyShapeStyle(Color.green) : parsedScore != nil ? tint : AnyShapeStyle(Color.gray)
    }
    private var feedbackAnimation: Animation? { reduceMotion ? nil : .spring(response: 0.3, dampingFraction: 0.82) }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    ScoreEntryHeader(song: song, sheet: sheet)
                    ScoreEntryInputCard(scoreText: $scoreText, clear: $clear, combo: $combo, chain: $chain,
                        rank: rank, validation: validation, focus: $scoreFocused)
                    if let best {
                        ScoreEntryBestCard(best: best, date: history.first.map { Date(timeIntervalSince1970: Double($0.playedAt) / 1000) }, tint: tint)
                    }
                    if let error {
                        Label(error, systemImage: "exclamationmark.circle.fill")
                            .font(.footnote).foregroundStyle(.red).frame(maxWidth: .infinity, alignment: .leading)
                    }
                    if saved {
                        Label(tr("成绩已保存，可继续修改后再次保存。"), systemImage: "checkmark.circle.fill")
                            .font(.footnote).foregroundStyle(.green).frame(maxWidth: .infinity, alignment: .leading)
                    }
                    Button(action: saveOrFinish) {
                        Label(saved ? tr("完成") : tr("保存成绩"), systemImage: saved ? "checkmark.circle.fill" : "square.and.arrow.down")
                            .font(.headline.bold()).foregroundStyle(.white)
                            .frame(maxWidth: .infinity).frame(minHeight: 52)
                            .background(buttonStyle, in: .rect(cornerRadius: 16))
                    }
                    .disabled(!saved && parsedScore == nil)
                    .accessibilityIdentifier("score-entry-save")
                }.padding(20).padding(.bottom, 32)
            }
            .scrollDismissesKeyboard(.interactively).background(Color(.systemGroupedBackground))
            .navigationTitle(tr("记录成绩")).navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(tr("取消")) { dismiss() } }
                if saved {
                    ToolbarItem(placement: .confirmationAction) { Button(tr("完成")) { dismiss() } }
                }
                ToolbarItemGroup(placement: .keyboard) {
                    Spacer()
                    Button(tr("完成")) { scoreFocused = false }
                }
            }
        }
        .presentationBackground(Color(.systemGroupedBackground))
        .onAppear(perform: loadInitialValues)
        .onChange(of: scoreText) { resetSaveState() }
        .onChange(of: clear) { resetSaveState() }
        .onChange(of: combo) { resetSaveState() }
        .onChange(of: chain) { resetSaveState() }
        .animation(feedbackAnimation, value: saved)
        .animation(feedbackAnimation, value: clear)
        .animation(feedbackAnimation, value: combo)
        .animation(feedbackAnimation, value: chain)
        .sensoryFeedback(.success, trigger: saved) { _, newValue in newValue }
    }

    private func loadInitialValues() {
        guard !loaded else { return }; loaded = true
        ownerID = profileID ?? personal.snapshot.activeProfile?.id
        ownerRegion = region ?? personal.snapshot.activeProfile?.server
        let currentBest = best
        scoreText = initialScore.map(String.init) ?? currentBest.map { String($0.score) } ?? ""
        clear = initialScore == nil ? currentBest?.clear ?? initialClear : initialClear
        // Scanner statuses belong to this play; do not inherit an earlier FC/AJ/CHAIN badge.
        if initialScore == nil { combo = currentBest?.fullCombo ?? ""; chain = currentBest?.fullChain ?? "" }
    }
    private func resetSaveState() { saved = false; error = nil }
    private func saveOrFinish() {
        if saved { dismiss(); return }
        scoreFocused = false
        guard let score = parsedScore else { return }
        guard let bundle = catalog.bundle, let ownerID, let ownerRegion else {
            error = tr("请先加载曲库并选择玩家档案。"); return
        }
        do {
            try personal.bridge.saveScannedScore(bundle: bundle, profileId: ownerID, region: ownerRegion,
                chartKey: song.id + ":" + sheet.id, scoreText: String(score), clear: clear, combo: combo, chain: chain)
            personal.reload(catalog: catalog)
            error = nil; saved = true
        } catch { self.error = error.localizedDescription }
    }
}
