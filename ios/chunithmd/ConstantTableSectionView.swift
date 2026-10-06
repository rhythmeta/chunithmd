import Shared
import SwiftUI

struct ConstantTableSectionView: View {
    let section: ConstantTableSection
    let index: Int
    let includesScores: Bool
    let songs: [String: CatalogSongViewData]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text(section.constantLabel).font(.headline.weight(.black))
                    .foregroundStyle(constantTableLevelColor(section.constantLabel, index: index))
                Spacer()
                Text(tr("{0} 张谱面", section.entries.count)).font(.footnote).foregroundStyle(.secondary)
            }
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 5), spacing: 8) {
                ForEach(section.entries, id: \.sheetKey) { entry in
                    ConstantTableTile(entry: entry, includesScores: includesScores, song: songs[entry.songId])
                }
            }
            .padding(10)
            .background(Color.primary.opacity(index.isMultiple(of: 2) ? 0.045 : 0.025), in: .rect(cornerRadius: 14))
        }.accessibilityIdentifier("constant-section-" + section.constantLabel)
    }
}

func constantTableLevelColor(_ label: String, index: Int) -> Color {
    let tenths = Int(((Double(label) ?? 0) * 10).rounded()) % 10
    switch tenths {
    case 0, 5: return argbColor(0xFFD34A63)
    case 1, 6: return argbColor(0xFF4D78FF)
    case 2, 7: return argbColor(0xFF3F9B74)
    case 3, 8: return argbColor(0xFFB45BFF)
    default: return argbColor(index.isMultiple(of: 2) ? 0xFFC84A7B : 0xFF5489FF)
    }
}
