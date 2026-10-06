import Shared
import SwiftUI

struct RecommendationRow: View {
    let entry: RecommendationResult
    @Environment(CatalogStore.self) private var catalog
    @Environment(SongNavigation.self) private var navigation
    @State private var identity = UUID()

    var body: some View {
        let song = catalog.allSongs.first { $0.id == entry.song.songId }
        let tint = difficultyStyle(entry.sheet.difficulty, type: entry.sheet.type)
        Button {
            if let song {
                navigation.open(song, sheetID: entry.sheet.type + ":" + entry.sheet.difficulty, sourceID: identity)
            }
        } label: {
            HStack(spacing: 14) {
                HStack(spacing: 10) {
                    RoundedRectangle(cornerRadius: 2).fill(tint).frame(width: 4).padding(.vertical, 8)
                    JacketImage(url: catalog.jacketURL(for: entry.song.imageName))
                        .frame(width: 56, height: 56).clipShape(.rect(cornerRadius: 10))
                }
                VStack(alignment: .leading, spacing: 4) {
                    SongDetailText(text: entry.song.title, font: .system(size: 15, weight: .bold), lineHeight: 18, alignment: .leading)
                    HStack(spacing: 6) {
                        if let score = entry.currentScore {
                            let rank = ChunithmScoreRules.shared.rank(score: score.int32Value)
                            Text(rank).font(.system(size: 11, weight: .black, design: .rounded))
                                .foregroundStyle(scoreRankColor(rank))
                            Text(score.intValue.formatted()).font(.system(size: 10, design: .monospaced))
                                .foregroundStyle(.secondary)
                        } else {
                            Text(tr("未游玩")).font(.system(size: 11)).foregroundStyle(.secondary)
                        }
                    }.fixedSize(horizontal: true, vertical: false)
                    ScoreTintBadge(text: entry.constant.formatted(.number.precision(.fractionLength(1))), tint: tint,
                                   font: .system(size: 10, weight: .bold))
                }.frame(maxWidth: .infinity, minHeight: 56, alignment: .leading)
                VStack(alignment: .trailing, spacing: 2) {
                    Text("+\(entry.potentialGain.formatted(.number.precision(.fractionLength(2))))")
                        .font(.system(size: 18, weight: .black, design: .rounded)).foregroundStyle(.orange)
                    Text(tr("目标 {0}", entry.targetRank))
                        .font(.system(size: 10, weight: .bold, design: .monospaced)).foregroundStyle(.secondary)
                }.fixedSize()
                Image(systemName: "chevron.right").font(.caption.weight(.semibold)).foregroundStyle(.tertiary)
            }.padding(.vertical, 4).contentShape(.rect)
        }
        .buttonStyle(.plain).disabled(song == nil)
        .accessibilityIdentifier("recommendation-row-" + entry.chartId)
        .modifier(SongTransitionSource(id: identity))
    }
}
