import SwiftUI
import Shared

struct ChartCard: View {
    let song: CatalogSongViewData
    let sheet: CatalogSongViewData.Sheet
    @Environment(PersonalStore.self) private var personal
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var expanded: Bool
    @State private var recording = false
    @State private var collections = false

    init(song: CatalogSongViewData, sheet: CatalogSongViewData.Sheet, initiallyExpanded: Bool) {
        self.song = song
        self.sheet = sheet
        _expanded = State(initialValue: initiallyExpanded)
    }

    var body: some View {
        let color = difficultyStyle(sheet.difficulty, type: sheet.type)
        let best = personal.best.first { $0.chartId == song.id + ":" + sheet.id }
        VStack(spacing: 0) {
            Button {
                withAnimation(reduceMotion ? nil : .spring(duration: 0.35, bounce: 0.15)) { expanded.toggle() }
            } label: {
                HStack(spacing: 0) {
                    RoundedRectangle(cornerRadius: 2).fill(difficultyStyle(sheet.difficulty, type: sheet.type, vertical: true))
                        .frame(width: 4).padding(.vertical, 4)
                    HStack(spacing: 12) {
                        VStack(alignment: .leading, spacing: 3) {
                            Text(sheet.difficulty.uppercased()).font(.system(size: 13, weight: .bold, design: .rounded)).foregroundStyle(color)
                            if let designer = sheet.noteDesigner, !designer.isEmpty {
                                Text(designer).font(.system(size: 11)).foregroundStyle(.secondary).lineLimit(1)
                            }
                        }
                        Spacer(minLength: 0)
                        if !expanded, let best {
                            VStack(alignment: .trailing, spacing: 1) {
                                Text(Int(best.score).formatted()).font(.system(size: 12, weight: .bold, design: .monospaced))
                                    .lineLimit(1).minimumScaleFactor(0.75)
                                ScoreStatusBadges(clear: best.clear, combo: best.fullCombo, chain: best.fullChain, tint: color, showClear: false)
                            }
                        }
                        Text((sheet.internalLevelValue ?? sheet.levelValue).map { $0.formatted(.number.precision(.fractionLength(1))) } ?? sheet.level).font(.system(size: 28, weight: .black, design: .rounded)).foregroundStyle(color.opacity(0.85)).frame(minWidth: 44)
                        Image(systemName: "chevron.down").font(.system(size: 10, weight: .bold))
                            .foregroundStyle(.secondary.opacity(0.4)).rotationEffect(.degrees(expanded ? 180 : 0))
                    }.padding(.leading, 12).padding(.trailing, 16)
                }.padding(.vertical, 14).contentShape(.rect)
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("chart-card-" + sheet.id)
            .accessibilityValue(expanded ? tr("已展开") : tr("已收起"))
            if expanded {
                VStack(spacing: 16) {
                    Rectangle().fill(color.opacity(0.12)).frame(height: 1).padding(.horizontal, 16)
                    BestChartSummaryView(song: song, sheet: sheet)
                    ChartDetailSection(sheet: sheet, rating: best?.rating)
                    if !personal.history(songID: song.id, sheetID: sheet.id).isEmpty {
                        ScoreHistorySection(song: song, sheet: sheet)
                    }
                    HStack(spacing: 10) {
                        Button { recording = true } label: {
                            Label(tr("录入成绩"), systemImage: "pencil.line")
                                .frame(maxWidth: .infinity).padding(.vertical, 10)
                                .background(color.opacity(0.1), in: .rect(cornerRadius: 10))
                        }
                        Button { collections = true } label: {
                            Label(tr("加入收藏夹"), systemImage: "rectangle.stack.badge.plus")
                                .frame(maxWidth: .infinity).padding(.vertical, 10)
                                .background(color.opacity(0.1), in: .rect(cornerRadius: 10))
                        }
                    }.font(.system(size: 13, weight: .semibold)).foregroundStyle(color)
                        .buttonStyle(.plain).padding(.horizontal, 16)
                }.padding(.bottom, 14)
                    .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .background(.ultraThinMaterial, in: .rect(cornerRadius: 16))
        .overlay { RoundedRectangle(cornerRadius: 16).strokeBorder(color.opacity(0.15)) }
        .contextMenu { Button(tr("加入收藏夹"), systemImage: "folder.badge.plus") { collections = true } }
        .sheet(isPresented: $recording) { ScoreEntryView(song: song, sheet: sheet) }
        .sheet(isPresented: $collections) { CollectionPickerView(song: song, sheet: sheet) }
    }
}
