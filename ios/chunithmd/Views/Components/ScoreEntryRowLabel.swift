import SwiftUI

struct ScoreEntryRowLabel<Badges: View, Value: View>: View {
    let title: String
    let jacket: URL?
    let tint: AnyShapeStyle
    let rank: String?
    let score: Int32?
    @ViewBuilder let badges: Badges
    @ViewBuilder let value: Value

    var body: some View {
        HStack(spacing: 14) {
            HStack(spacing: 10) {
                RoundedRectangle(cornerRadius: 2).fill(tint).frame(width: 4).padding(.vertical, 8)
                JacketImage(url: jacket)
                    .frame(width: 56, height: 56).clipShape(.rect(cornerRadius: 10))
            }
            VStack(alignment: .leading, spacing: 4) {
                SongDetailText(text: title, font: .system(size: 15, weight: .bold), lineHeight: 20, alignment: .leading)
                if let rank, let score {
                    HStack(spacing: 6) {
                        Text(rank).font(.system(size: 13, weight: .black, design: .rounded)).foregroundStyle(scoreRankColor(rank))
                        Text(Int(score).formatted()).font(.system(size: 12, design: .monospaced)).foregroundStyle(.secondary)
                    }.fixedSize(horizontal: true, vertical: false)
                }
                badges
            }.frame(maxWidth: .infinity, minHeight: 56, alignment: .leading)
            VStack(alignment: .trailing, spacing: 2) { value }.fixedSize()
            Image(systemName: "chevron.right").font(.caption.weight(.semibold)).foregroundStyle(.tertiary)
        }.padding(.vertical, 6).contentShape(.rect)
    }
}
